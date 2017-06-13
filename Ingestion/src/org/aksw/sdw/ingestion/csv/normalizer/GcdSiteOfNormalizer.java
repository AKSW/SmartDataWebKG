package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;
import java.util.regex.Pattern;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.aksw.sdw.ingestion.csv.utils.CustomJenaType;
import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.impl.XSDBaseStringType;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;

/**
 * This class can be used to convert an Address Blanknode into a Site Instance with proper URI
 * 
 * @author kay
 *
 */
public class GcdSiteOfNormalizer implements PropertyNormalizer {
	
	protected enum LocationType {COUNTRY, COUNTY, CITY};

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		
		entity.deleteProperty("http://www.w3.org/2006/vcard/ns#hasAddress");
	
		Entity addressNode = entity.getSubEntityByCategory("address");
		if (null == addressNode) {
			return;
		}							  
		
		List<RDFNode> postCodes = addressNode.getRdfObjects(CorpDbpedia.postalCode);
		String postCode = postCodes.get(0).toString().replace("\"", "");
		if (null == postCode) {
			return;
		}
		
		// handle cases like "D-12345"
		postCode = postCode.replaceAll("[^0-9]", "");		
		if (5 < postCode.length()) {
			throw new IngestionException("Invalid postcode for entity: " + entity.getSubjectUri() +
					 " and postcode: " + postCode);
		}
		
		// handle cases like "04109"
		if (5 > postCode.length()) {
			int diff = 5 - postCode.length();
			
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < diff; ++i) {
				buffer.append("0");
			}
			
			buffer.append(postCode);
			postCode = buffer.toString();
		}
		
		// create unique URI for each site
		String entityUriSite = entity.getSubjectUri() + "_" + postCode;
		
		// create entity for each site instead
		Entity siteEntity = new Entity(false);
		siteEntity.setSubjectUri(entityUriSite);
		
		// make it of type org:Site
		RDFNode siteTypeObject = new ResourceImpl(W3COrg.site);
		siteEntity.addTriple(RDF.type.toString(), siteTypeObject);
		
		// make link to parent company
		siteEntity.addTriple(W3COrg.siteOf, new ResourceImpl(entity.getSubjectUri()));
		entity.addTriple(W3COrg.hasSite, siteEntity.getSubjectUriObject());
		
		Pattern postcodePattern = PropertyNormalizerUtils.getInstance().getGeoNamesMapper().getPostCodeRegex("Germany");
		if (null != postcodePattern) {
			if (false == postcodePattern.matcher(postCode).matches()) {
				throw new IngestionException("Invalid postcode for entity: " + entity.getSubjectUri() +
											 " and postcode: " + postCode);
			}
		}
		siteEntity.addTripleWithLiteral(CorpDbpedia.postalCode, postCode, XSDBaseStringType.XSDstring);
		
		GeoNamesMapper geoNamesMapper = PropertyNormalizerUtils.getInstance().getGeoNamesMapper();
		
		String geonamesCountryUri = 
				this.addLocation(addressNode, siteEntity, geoNamesMapper, (String) null,
						CorpDbpedia.countryName, CorpDbpedia.countryGeoNamesId, LocationType.COUNTY);
		if (null == geonamesCountryUri) {
			throw new IngestionException("Was not able to find country");
		}
		
		this.addLocation(addressNode, siteEntity, geoNamesMapper, geonamesCountryUri,
						CorpDbpedia.countyName, CorpDbpedia.countyGeonamesId, LocationType.COUNTY);
		this.addLocation(addressNode, siteEntity, geoNamesMapper, geonamesCountryUri,
						CorpDbpedia.cityName, CorpDbpedia.cityGeonamesId, LocationType.CITY);
		
		List<RDFNode> streetAddress = addressNode.getRdfObjects(CorpDbpedia.prefixOntology + "siteAddress");
		if (null != streetAddress && false == streetAddress.isEmpty()) {
			Literal addressLiteral = streetAddress.get(0).asLiteral();
			String addressString = addressLiteral.getLexicalForm();			
			
			RDFDatatype dataType = ((Literal) streetAddress).asLiteral().getDatatype();
			if (null == dataType) {
				String language = ((Literal) streetAddress).asLiteral().getLanguage();
				if (null != language) {
					siteEntity.addTripleWithLiteral(W3COrg.siteAddress, addressString, language);
				} else {
					// not even a language tag --> mark as string
					dataType = XSDBaseStringType.XSDstring;
				}
			}
		
			// if we have a data type --> store new triple
			if (null != dataType) {
				siteEntity.addTripleWithLiteral(W3COrg.siteAddress, addressString, dataType);
			}
		}
		
		// get geo coordinates
		List<RDFNode> geoType = addressNode.getRdfObjects(RDF.type.getURI());
		List<RDFNode> geoCoordinates = addressNode.getRdfObjects("http://www.opengis.net/ont/geosparql#asWKT");
		
		// if geo-locations exist --> add them
		if (null != geoType && null != geoCoordinates) {
			siteEntity.addTriple(RDF.type.getURI(), geoType.get(0));
			
			String geoCoordinatesString = geoCoordinates.get(0).asLiteral().getLexicalForm();
			
			/// TODO handle other data types!!
			siteEntity.addTripleWithLiteral("http://www.opengis.net/ont/geosparql#asWKT",
					geoCoordinatesString, new CustomJenaType("http://www.openlinksw.com/schemas/virtrdf#Geometry"));
			
			stats.addNumberToStats(getClass(), "address.geoCoordinates", 1);
		} else {
			/// TODO add code to retrieve information??
		}
		
		// employee-count of GCD is per site and not per company
		List<RDFNode> employeeCounts = entity.getRdfObjects("http://dbpedia.org/ontology/numberOfEmployees");
		if (null != employeeCounts) {
			for (RDFNode employeeCount : employeeCounts) {
				siteEntity.addTriple("http://dbpedia.org/ontology/numberOfEmployees", employeeCount);
			}
			
			entity.deleteProperty("http://dbpedia.org/ontology/numberOfEmployees");
		}
		
		
		// make sure that this entity is not used anymore
		entity.deleteSubEntity(addressNode);
		
		// add this entity instead
		entity.addSubEntity(siteEntity);
	}
	
	
	/**
	 * This method can be used to add location inforamtion like country, county and city
	 * to the siteEntity using information from the addressNode
	 * 
	 * @param addressNode
	 * @param siteEntity
	 * @param geoNamesMapper
	 * @param geonamesCountryUri
	 * @param objectPredicateName
	 * @param objectPredicateId
	 * @param locationType
	 * @return geonames URI
	 * @throws IngestionException
	 */
	protected String addLocation(final Entity addressNode, final Entity siteEntity,
							   final GeoNamesMapper geoNamesMapper,
							   final String geonamesCountryUri,
							   final String objectPredicateName,
							   final String objectPredicateId,
							   final LocationType locationType) throws IngestionException {
		
		String geoNamesId = null;
		List<RDFNode> nameObjects = addressNode.getRdfObjects(objectPredicateName);
		if (null != nameObjects && false == nameObjects.isEmpty()) {
			
			for (RDFNode nameObject : nameObjects) {
				if (nameObject.toString().contains("http")) {
					siteEntity.addTriple(objectPredicateId, nameObject);
					
					geoNamesId = nameObject.asResource().getURI();
				} else {
					// ignore, since some names are incorrect
				}
			}
			
			if (null == geoNamesId) {
				for (RDFNode nameObject : nameObjects) {
					String languageCode = null;
					if (nameObject instanceof Literal) {
						languageCode = nameObject.asLiteral().getLanguage();
					} else {
						continue;
					}
					
					if (languageCode.startsWith("@")) {
						// cut off "@"
						languageCode = languageCode.substring(1);
					}
					
					String name = nameObject.asLiteral().getLexicalForm();
					switch (locationType) {
					case COUNTRY:
						geoNamesId = geoNamesMapper.getGeoNamesCountryId(name, languageCode);
						break;
					case COUNTY:
						geoNamesId = geoNamesMapper.getGeoNamesCountyId(name, geonamesCountryUri, languageCode);
						break;
					case CITY:
						geoNamesId = geoNamesMapper.getGeoNamesCityId(name, languageCode, geonamesCountryUri);
						break;
					}
					
					if (null != geoNamesId) {
						ResourceImpl geoNamesUriObject = new ResourceImpl(geoNamesId);
						siteEntity.addTriple(objectPredicateId, geoNamesUriObject);
						break; // break out of this loop
					}
				}
			}
			
			// now add names
			if (null != geoNamesId) {
				String nameEnglish = geoNamesMapper.getNameFromId(geoNamesId, "en");
				if (null != nameEnglish && false == nameEnglish.isEmpty()) {					
					siteEntity.addTripleWithLiteral(objectPredicateName, nameEnglish, "en");
				} else {
					for (RDFNode nameObject : nameObjects) {
						// in case we have the original one --> add old one
						String languageCode = null;
						if (nameObject instanceof Literal) {
							languageCode = nameObject.asLiteral().getLanguage();
						} else {
							continue;
						}
						
						if ("en".equals(languageCode)) {
							siteEntity.addTriple(objectPredicateName, nameObject);
						}
					}
				}
				
				String nameGerman = geoNamesMapper.getNameFromId(geoNamesId, "de");
				if (null != nameGerman && false == nameGerman.isEmpty()) {					
					siteEntity.addTripleWithLiteral(objectPredicateName, nameGerman, "de");
				} else {
					for (RDFNode nameObject : nameObjects) {
						// in case we have the original one --> add old one
						String languageCode = null;
						if (nameObject instanceof Literal) {
							languageCode = nameObject.asLiteral().getLanguage();
						} else {
							continue;
						}
						
						if ("de".equals(languageCode)) {
							siteEntity.addTriple(objectPredicateName, nameObject);
						}
					}
				}
			}
		}
		
		return geoNamesId;
	}

}
