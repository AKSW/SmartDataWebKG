package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;
import java.util.Map;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.GeoNamesConst;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper;

import org.apache.jena.datatypes.xsd.impl.XSDBaseStringType;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;

/**
 * This class can take a GRID address and convert it into a W3C organization site
 * 
 * @author kay
 *
 */
public class GridAddressNormalizer extends JsonPropertyNormalizer {
	
	public GridAddressNormalizer() {
		super("addresses.");
	}

	@Override
	public void normalize(Entity entity, ConversionStats stats, Map<String, List<MatchingPredicateStruct>> addressMap) throws IngestionException {
				
		GeoNamesMapper geonamesMapper = PropertyNormalizerUtils.getInstance().getGeoNamesMapper();
		
		for (String indexString : addressMap.keySet()) {
			
			String cleanedIndexString = indexString.replace(".", "");

			// create entity and set uri --> do not know what type of site this is (headquarter?)
			Entity addressEntity = new Entity(false);
			String uri = new StringBuffer().append(entity.getSubjectUri()).
					append("_site_").append(cleanedIndexString).toString();			
			addressEntity.setSubjectUri(uri);
			
			StringBuffer addressLine = null;
			String lattitude = null;
			String longitude = null;
			List<MatchingPredicateStruct> addressPredicates = addressMap.get(indexString);
			for (MatchingPredicateStruct addressPredicate : addressPredicates) {
				String shortenedPredicate = addressPredicate.shortenedPredicate;
				
				switch (shortenedPredicate) {
				case "country": {
					String matchingPredicate = addressPredicate.fullPredicate;
					List<RDFNode> countryObjects = entity.getRdfObjects(matchingPredicate);
					if (null == countryObjects || countryObjects.isEmpty()) {
						continue;
					}
					
					for (RDFNode countryObject : countryObjects) {
						String countryName = countryObject.asLiteral().getLexicalForm();						
						String geonamesIdCountry = geonamesMapper.getGeoNamesCountryId(countryName, "en");
						if (null == geonamesIdCountry || geonamesIdCountry.isEmpty()) {
							continue;
						}
						
						ResourceImpl genomanesIdObject = new ResourceImpl(geonamesIdCountry);
						addressEntity.addTriple(new PropertyImpl(CorpDbpedia.countryGeoNamesId), genomanesIdObject);
						
						String nameEnglish = geonamesMapper.getNameFromId(geonamesIdCountry, "en");
						if (null != nameEnglish) {
							addressEntity.addTripleWithLiteral(
									new PropertyImpl(CorpDbpedia.countryName), nameEnglish, "en");
						}
						
						String nameGerman = geonamesMapper.getNameFromId(geonamesIdCountry, "de");
						if (null != nameGerman) {
							addressEntity.addTripleWithLiteral(
									new PropertyImpl(CorpDbpedia.countryName), nameGerman, "de");
						}
					}
					} break;
				case "geonames_city.id": {
					String matchingPredicate = addressPredicate.fullPredicate;
					List<RDFNode> cityObjects = entity.getRdfObjects(matchingPredicate);
					if (null == cityObjects || cityObjects.isEmpty()) {
						continue;
					}
					
					for (RDFNode cityObject : cityObjects) {
						String cityId = cityObject.asLiteral().getLexicalForm();
						if (null == cityId || cityId.isEmpty()) {
							continue;
						}
						
						String geonamesUri = GeoNamesConst.createGeonamesUri(cityId);
						String nameEnglish = geonamesMapper.getNameFromId(geonamesUri, "en");
						if (null != nameEnglish) {
							addressEntity.addTripleWithLiteral(
									new PropertyImpl(CorpDbpedia.cityName), nameEnglish, "en");
						}
						
						String nameGerman = geonamesMapper.getNameFromId(geonamesUri, "de");
						if (null != nameGerman) {
							addressEntity.addTripleWithLiteral(
									new PropertyImpl(CorpDbpedia.cityName), nameGerman, "de");
						}
						
						ResourceImpl uriObject = new ResourceImpl(geonamesUri);
						addressEntity.addTriple(new PropertyImpl(CorpDbpedia.cityGeonamesId), uriObject);
					}
				} break;
				case "lat": {
					String matchingPredicate = addressPredicate.fullPredicate;

					List<RDFNode> lattitudeObjects = entity.getRdfObjects(matchingPredicate);
					if (null != lattitudeObjects && 1 == lattitudeObjects.size()) {					
							lattitude = lattitudeObjects.get(0).asLiteral().getLexicalForm();
					}
				} break;
				case "lng": {
					String matchingPredicate = addressPredicate.fullPredicate;

					List<RDFNode> lattitudeObjects = entity.getRdfObjects(matchingPredicate);
					if (null != lattitudeObjects && 1 == lattitudeObjects.size()) {					
						longitude = lattitudeObjects.get(0).asLiteral().getLexicalForm();
					}
				} break;
				case "postcode": {
					String matchingPredicate = addressPredicate.fullPredicate;

					List<RDFNode> postcodeObjects = entity.getRdfObjects(matchingPredicate);
					if (null != postcodeObjects && 1 == postcodeObjects.size()) {					
						String postCode = postcodeObjects.get(0).asLiteral().getLexicalForm();
						addressEntity.addTripleWithLiteral(
								new PropertyImpl(CorpDbpedia.postalCode),
								postCode, XSDBaseStringType.XSDstring);
						
						stats.addNumberToStats(getClass(), "address.postalCode", 1);
					}
				} break;
				case "state": {
					String matchingPredicate = addressPredicate.fullPredicate;

					List<RDFNode> stateObjects = entity.getRdfObjects(matchingPredicate);
					if (null != stateObjects && 1 == stateObjects.size()) {					
						String stateName = stateObjects.get(0).asLiteral().getLexicalForm();
						
						String countyIde = geonamesMapper.getGeoNamesCountryId(stateName, "en");
						if (null != countyIde && false == countyIde.isEmpty()) {
							
							String geonamesUri = GeoNamesConst.createGeonamesUri(countyIde);
							String nameEnglish = geonamesMapper.getNameFromId(geonamesUri, "en");
							if (null != nameEnglish) {
								addressEntity.addTripleWithLiteral(
										new PropertyImpl(CorpDbpedia.countyName), nameEnglish, "en");
							}
							
							String nameGerman = geonamesMapper.getNameFromId(geonamesUri, "de");
							if (null != nameGerman) {
								addressEntity.addTripleWithLiteral(
										new PropertyImpl(CorpDbpedia.countyName), nameGerman, "de");
							}
							
							ResourceImpl uriObject = new ResourceImpl(geonamesUri);
							addressEntity.addTriple(new PropertyImpl(CorpDbpedia.countyGeonamesId), uriObject);
							
						}
						
						stats.addNumberToStats(getClass(), "address.stateId", 1);
					}
				} break;
				case "line_1":
				case "line_2":
				case "line_3": {
					if (null == addressLine) {
						addressLine = new StringBuffer();
					}
					
					String matchingPredicate = addressPredicate.fullPredicate;

					List<RDFNode> addressLineObjects = entity.getRdfObjects(matchingPredicate);
					if (null != addressLineObjects && 1 == addressLineObjects.size()) {	
						addressLine.append(addressLineObjects.get(0).asLiteral().getLexicalForm());
						addressLine.append(", ");
					}
				} break;
					
				}
			}
			
			if (null != addressLine) {
				addressEntity.addTripleWithLiteral(
						new PropertyImpl(W3COrg.siteAddress), addressLine.toString(), "en");
				stats.addNumberToStats(getClass(), "address.siteAddress", 1);
			}
			
			if (null != longitude && null != lattitude) {
				// add geo - coordinates, if they exist
				GeoCordinatesNormalizer.addPoint(longitude, lattitude, addressEntity);
				stats.addNumberToStats(getClass(), "address.geoCoordinates", 1);
			}
			
			if (false == addressEntity.isEmpty()) {
				addressEntity.addTriple(new PropertyImpl(W3COrg.siteOf), entity.getSubjectUriObject());
				addressEntity.addTriple(new PropertyImpl(RDF.type.getURI()), new ResourceImpl(W3COrg.site));
				
				entity.addSubEntity(addressEntity);
				entity.addTriple(new PropertyImpl(W3COrg.hasSite), addressEntity.getSubjectUriObject());
			}
		}
		
		// make sure that all the address predicates are gone
		for (List<MatchingPredicateStruct> addressPredicates : addressMap.values()) {
			for (MatchingPredicateStruct addressPredicate : addressPredicates) {
				entity.deleteProperty(addressPredicate.fullPredicate);
			}
		}		
	}
}
