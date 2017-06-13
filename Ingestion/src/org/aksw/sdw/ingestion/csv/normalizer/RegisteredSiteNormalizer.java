package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;

/**
 * This class can be used to look after registered addresses
 * 
 * @author kay
 *
 */
public class RegisteredSiteNormalizer implements PropertyNormalizer {

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		
		boolean hasRegisteredInformation = true;
		for (String predicate : entity.getPredicates()) {
			if (predicate.toLowerCase().contains("registered")) {
				hasRegisteredInformation = true;
				break;
			}
		}
		
		if (false == hasRegisteredInformation) {
			return;
		}
		
		GeoNamesMapper geonamesMapper = PropertyNormalizerUtils.getInstance().getGeoNamesMapper();
		
		Entity addressEntity = new Entity(false);
		
		String addressCountryName = null;
		List<RDFNode> addressObjects = entity.getRdfObjects("http://ont.thomsonreuters.com/mdaas/RegisteredAddress");
		for (RDFNode addressObject : addressObjects) {
			String addressString = addressObject.asLiteral().getLexicalForm();
			if (null == addressString) {
				continue;
			}
			
			String[] addressComponents = addressString.split("\\\\n");
			if (null != addressComponents && 1 <= addressComponents.length) {
				addressCountryName = addressComponents[addressComponents.length - 1];
			}
			
			
			addressEntity.addTriple("http://corp.dbpedia.org/ontology#siteAddress", addressObject);
		}
	
		List<RDFNode> phoneNumbers = entity.getRdfObjects("http://permid.org/ontology/organization/hasRegisteredPhoneNumber");
		if (null != phoneNumbers) {
			for (RDFNode phoneNumber : phoneNumbers) {
				addressEntity.addTriple("http://corp.dbpedia.org/ontology#phoneNumber", phoneNumber);
			}
		}
	
		List<RDFNode> faxNumbers = entity.getRdfObjects("http://permid.org/ontology/organization/hasRegisteredFaxNumber");
		if (null != faxNumbers) {
			for (RDFNode faxNumber : faxNumbers) {
				addressEntity.addTriple("http://corp.dbpedia.org/ontology#faxNumber", faxNumber);
			}
		}
		
		boolean addedCountryInformation = false;
		if (null != addressCountryName) {
			try {
				int bracketsIndex = addressCountryName.indexOf("(");
				String cleanCountryName = ((0 < bracketsIndex) ? addressCountryName.substring(0, bracketsIndex).trim() : addressCountryName.trim());
				
				String geonamesIdCountry = geonamesMapper.getGeoNamesCountryId(cleanCountryName, "en");
				if (null != geonamesIdCountry && false == geonamesIdCountry.isEmpty()) {
					ResourceImpl genomanesIdObject = new ResourceImpl(geonamesIdCountry);
					addressEntity.addTriple("http://corp.dbpedia.org/ontology#" + "geonamesIdCountry", genomanesIdObject);
					
					String nameEnglish = geonamesMapper.getNameFromId(geonamesIdCountry, "en");
					if (null != nameEnglish) {
						addressEntity.addTripleWithLiteral("http://corp.dbpedia.org/ontology#" + "countryName", geonamesIdCountry, "en");
					}
					
					String nameGerman = geonamesMapper.getNameFromId(geonamesIdCountry, "de");
					if (null != nameGerman) {						
						addressEntity.addTripleWithLiteral("http://corp.dbpedia.org/ontology#" + "countryName", nameGerman, "de");
					}
					
					addedCountryInformation = true;
				}
			} catch (IngestionException e) {
				System.err.println("Exception when trying to get geonames entity: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	
		// add information about country
		List<RDFNode> domiciledIn = entity.getRdfObjects("http://www.omg.org/spec/EDMC-FIBO/BE/LegalEntities/CorporateBodies/isDomiciledIn");
		if (false == addedCountryInformation && null != domiciledIn) {
			for (RDFNode domiciledInTmp : domiciledIn) {
				String geonamesIdCountry = domiciledInTmp.asResource().getURI();
				
				ResourceImpl genomanesIdObject = new ResourceImpl(geonamesIdCountry);
				addressEntity.addTriple("http://corp.dbpedia.org/ontology#" + "geonamesIdCountry", genomanesIdObject);
				
				String nameEnglish = geonamesMapper.getNameFromId(geonamesIdCountry, "en");
				if (null != nameEnglish) {					
					addressEntity.addTripleWithLiteral("http://corp.dbpedia.org/ontology#" + "countryName", nameEnglish, "en");
				}
				
				String nameGerman = geonamesMapper.getNameFromId(geonamesIdCountry, "de");
				if (null != nameGerman) {					
					addressEntity.addTripleWithLiteral("http://corp.dbpedia.org/ontology#" + "countryName", nameGerman, "de");
				}
			}
		}
		
		if (false == addressEntity.isEmpty()) {
			String uri = entity.getSubjectUri() + "_RegisteredSite";
			
			addressEntity.setSubjectUri(uri);
			
			addressEntity.addTriple("http://corp.dbpedia.org/ontology#" + "siteOf", entity.getSubjectUriObject());
			
			entity.addSubEntity(addressEntity);
			
			entity.addTriple("http://corp.dbpedia.org/ontology#" + "hasRegisteredSite", addressEntity.getSubjectUriObject());			
		}
		
		entity.deleteProperty("http://ont.thomsonreuters.com/mdaas/RegisteredAddress");
		entity.deleteProperty("http://permid.org/ontology/organization/hasRegisteredPhoneNumber");
		entity.deleteProperty("http://permid.org/ontology/organization/hasRegisteredFaxNumber");
		
	}

}
