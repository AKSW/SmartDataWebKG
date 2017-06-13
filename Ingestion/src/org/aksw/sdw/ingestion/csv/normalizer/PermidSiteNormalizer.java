package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;
import java.util.regex.Pattern;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;

public class PermidSiteNormalizer implements PropertyNormalizer {
	
	protected final Pattern filterRegex;
	protected final String uriSuffix;
	protected final String targetEntityPredicate;
	protected final String addressPredicate;
	protected final String phoneNumberPredicate;
	protected final String faxNumberPredicate;
	protected final String isDomiciledInPredicate;
	
	static final Pattern SPLIT_PATTERN = Pattern.compile("\\\\n");
	
	public PermidSiteNormalizer(final String filterReg,
							  final String uriSuffix,
							  final String targetEntityPredicate,
							  final String addressPredicate,
							  final String phoneNumberPredicate,
							  final String faxNumberPredicate,
							  final String isDomiciledInPredicate) {
		this.filterRegex = Pattern.compile("(?i)(" + filterReg + ")");
		this.uriSuffix = uriSuffix;
		
		this.addressPredicate = addressPredicate;
		this.phoneNumberPredicate = phoneNumberPredicate;
		this.faxNumberPredicate = faxNumberPredicate;
		this.isDomiciledInPredicate = isDomiciledInPredicate;
		this.targetEntityPredicate = targetEntityPredicate;
	}

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		boolean hasRegisteredInformation = true;
		for (String predicate : entity.getPredicates()) {
			if (filterRegex.matcher(predicate).find()) {
				hasRegisteredInformation = true;
				break;
			}
		}
		
		if (false == hasRegisteredInformation) {
			return;
		}
		
		GeoNamesMapper geonamesMapper = PropertyNormalizerUtils.getInstance().getGeoNamesMapper();
		
		Entity addressEntity = new Entity(false);
		addressEntity.setSubjectUri(entity.getSubjectUri() + "_address");
		
		String addressCountryName = null;
		if (null != this.addressPredicate) {
			List<RDFNode> addressObjects = entity.getRdfObjects(this.addressPredicate);
			for (RDFNode addressObject : addressObjects) {
				String addressString = addressObject.asLiteral().getLexicalForm();
				if (null == addressString) {
					continue;
				}
				
				String[] addressComponents = SPLIT_PATTERN.split(addressString);
				if (null != addressComponents && 1 <= addressComponents.length) {
					addressCountryName = addressComponents[addressComponents.length - 1];
				}
				
				
				addressEntity.addTriple(W3COrg.siteAddress, addressObject);
			}
		}
	
		if (null != this.phoneNumberPredicate) {
			List<RDFNode> phoneNumbers = entity.getRdfObjects(this.phoneNumberPredicate);
			if (null != phoneNumbers) {
				for (RDFNode phoneNumber : phoneNumbers) {
					addressEntity.addTriple(CorpDbpedia.phoneNumber, phoneNumber);
				}
			}
		}
	
		if (null != this.faxNumberPredicate) {
			List<RDFNode> faxNumbers = entity.getRdfObjects(this.faxNumberPredicate);
			if (null != faxNumbers) {
				for (RDFNode faxNumber : faxNumbers) {
					addressEntity.addTriple(CorpDbpedia.faxNumber, faxNumber);
				}
			}
		}
		
		boolean addedCountryInformation = false;
		if (null != addressCountryName) {
			try {
				int bracketsIndex = addressCountryName.indexOf("(");
				String cleanCountryName = ((0 < bracketsIndex) ? addressCountryName.substring(0, bracketsIndex).trim() : addressCountryName.trim());
				
				String geonamesIdCountry = geonamesMapper.getGeoNamesCountryId(cleanCountryName.replace("\"", ""), "en");
				if (null != geonamesIdCountry && false == geonamesIdCountry.isEmpty()) {
					ResourceImpl genomanesIdObject = new ResourceImpl(geonamesIdCountry);
					addressEntity.addTriple(CorpDbpedia.countryGeoNamesId, genomanesIdObject);
					
					String nameEnglish = geonamesMapper.getNameFromId(geonamesIdCountry, "en");
					if (null != nameEnglish) {
						addressEntity.addTripleWithLiteral(CorpDbpedia.countryName, nameEnglish, "en");
					}
					
					String nameGerman = geonamesMapper.getNameFromId(geonamesIdCountry, "de");
					if (null != nameGerman) {
						addressEntity.addTripleWithLiteral(CorpDbpedia.countryName, nameGerman, "de");
					}
					
					addedCountryInformation = true;
				}
			} catch (IngestionException e) {
				System.err.println("Exception when trying to get geonames entity: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	
		// add information about country
		if (null != this.isDomiciledInPredicate) {
			List<RDFNode> domiciledIn = entity.getRdfObjects(this.isDomiciledInPredicate);
			if (false == addedCountryInformation && null != domiciledIn) {
				for (RDFNode domiciledInTmp : domiciledIn) {
					String geonamesIdCountry = domiciledInTmp.asResource().getURI();
					
					ResourceImpl genomanesIdObject = new ResourceImpl(geonamesIdCountry);
					addressEntity.addTriple(CorpDbpedia.countryGeoNamesId, genomanesIdObject);
					
					String nameEnglish = geonamesMapper.getNameFromId(geonamesIdCountry, "en");
					if (null != nameEnglish) {						
						addressEntity.addTripleWithLiteral(CorpDbpedia.countryName, nameEnglish, "en");
					}
					
					String nameGerman = geonamesMapper.getNameFromId(geonamesIdCountry, "de");
					if (null != nameGerman) {
						addressEntity.addTripleWithLiteral(CorpDbpedia.countryName, nameGerman, "de");
					}
				}
			}
		}
		
		if (false == addressEntity.isEmpty()) {
			String uri = new StringBuffer().append(entity.getSubjectUri()).append(this.uriSuffix).toString();
			
			addressEntity.setSubjectUri(uri);
			
			addressEntity.addTriple(W3COrg.siteOf, entity.getSubjectUriObject());			
			addressEntity.addTriple(RDF.type.getURI(), new ResourceImpl(W3COrg.site));
			
			entity.addTriple(this.targetEntityPredicate, addressEntity.getSubjectUriObject());			
	
			List<String> propertyClassNames = PropertyNormalizerUtils.getInstance().
					getOntologyHandler().getParentPropertyNames(this.targetEntityPredicate);
			
			for (String propertyClassName : propertyClassNames) {
				if (propertyClassName.startsWith(W3COrg.prefix) ||
					propertyClassName.startsWith(CorpDbpedia.prefixOntology)) {
					entity.addTriple(propertyClassName, addressEntity.getSubjectUriObject());
				}
			}
			
			entity.addSubEntity(addressEntity);
			
		}
		
		entity.deleteProperty(this.addressPredicate);
		entity.deleteProperty(this.phoneNumberPredicate);
		entity.deleteProperty(this.faxNumberPredicate);
		entity.deleteProperty(this.isDomiciledInPredicate);
	}

}
