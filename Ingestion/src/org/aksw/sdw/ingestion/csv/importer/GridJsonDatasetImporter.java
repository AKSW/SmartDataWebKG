//package org.aksw.sdw.ingestion.csv.importer;
//
//import java.io.IOException;
//import java.util.Map.Entry;
//import java.util.Set;
//
//import aksw.org.kg.entity.Entity;
//import org.aksw.sdw.ingestion.csv.IngestionException;
//import aksw.org.kg.entity.RdfObject;
//import aksw.org.kg.entity.RdfObjectLiteral;
//import aksw.org.kg.entity.RdfObjectUri;
//import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
//import org.aksw.sdw.ingestion.csv.constants.GeoNamesConst;
//import org.aksw.sdw.ingestion.csv.constants.W3COrg;
//import org.aksw.sdw.ingestion.csv.constants.SKOS;
//import org.aksw.sdw.ingestion.csv.constants.VCARD;
//import org.aksw.sdw.ingestion.csv.normalizer.GeoCordinatesNormalizer;
//import org.aksw.sdw.ingestion.csv.normalizer.PropertyNormalizer;
//import org.aksw.sdw.ingestion.csv.normalizer.PropertyNormalizerUtils;
//import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper;
//import org.codehaus.jettison.json.JSONArray;
//
//import com.google.gson.JsonArray;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import org.apache.jena.datatypes.RDFDatatype;
//import org.apache.jena.datatypes.xsd.XSDDatatype;
//import org.apache.jena.graph.Triple;
//import org.apache.jena.sparql.vocabulary.FOAF;
//import org.apache.jena.vocabulary.RDF;
//import org.apache.jena.vocabulary.RDFS;
//
///**
// * This class can be used to convert grid.ac json dataset
// * into our common organization format
// * 
// * @author kay
// *
// */
//public class GridJsonDatasetImporter extends JsonDatasetImporter {
//	
//	/** current json element of input file */
//	protected JsonArray institutes;
//	
//	protected int instituteIndex = -1;
//	
//	final protected Entity currentEntity = new Entity(CorpDbpedia.prefix);
//
//	public GridJsonDatasetImporter(final String filePath) {
//		super(filePath);
//	}
//	
//	protected JsonArray getJsonInstitutes() throws IngestionException {
//		try {
//			JsonElement jsonElement = super.getJsonReader().getJson();
//			
//			JsonObject gridElement = jsonElement.getAsJsonObject();
//			JsonArray institutesArray = gridElement.getAsJsonArray("institutes");
//			return institutesArray;
//		} catch (IOException e) {
//			throw new IngestionException("Was not able to read from json instance", e);
//		}
//	}
//
//	@Override
//	public boolean hasNext() throws IngestionException {
////		try {
//			if (null == this.institutes) {
//				this.institutes = this.getJsonInstitutes();
//				this.instituteIndex = 0;
//			}
//			
//			if (this.instituteIndex >= this.institutes.size()) {
//				// we have read all the institutes
//				return false;
//			}
//			
//			while (true) {
//				JsonObject currentInstitute = this.institutes.get(this.instituteIndex).getAsJsonObject();
//				
//				this.currentEntity.reset();
//				
//				String id = currentInstitute.get("id").getAsString().toLowerCase();
//				this.currentEntity.addTriple(W3COrg.identifier, new RdfObjectLiteral(id, XSDDatatype.XSDstring.getURI()));
//	
//				String name;
//				try {
//					name = currentInstitute.get("name").getAsString();			
//				} catch (Exception e) {
//					//throw new IngestionException("Was not able to find id name: " + id, e);
//					/// TODO km : think about how to integrate redirects! (e.g. store ids and add them as sameAs??)
//					++this.instituteIndex; //--> we have hit a redirect
//					continue; //
//				}
//				RdfObjectLiteral nameObject = new RdfObjectLiteral(name, "@en");
//				this.currentEntity.addTriple(SKOS.prefLabel , nameObject);
//				
//				JsonArray acronyms = currentInstitute.get("acronyms").getAsJsonArray();
//				if (null != acronyms && 0 < acronyms.size()) {
//					for (int i = 0; i < acronyms.size(); ++i) {
//						String acronym = acronyms.get(i).getAsString();
//						RdfObjectLiteral acronymsObject = new RdfObjectLiteral(acronym, "@en");
//						this.currentEntity.addTriple(SKOS.altLabel, acronymsObject);
//					}
//				}
//				
//				JsonArray aliases = currentInstitute.get("aliases").getAsJsonArray();
//				if (null != aliases && 0 < aliases.size()) {
//					for (int i = 0; i < aliases.size(); ++i) {
//						String alias = aliases.get(i).getAsString();
//						RdfObjectLiteral aliaseObject = new RdfObjectLiteral(alias, "@en");
//						this.currentEntity.addTriple(SKOS.altLabel, aliaseObject);
//					}
//				}
//	
//	
//				String countryCode = null;
//				JsonArray addresses = currentInstitute.get("addresses").getAsJsonArray();
//				for (int i = 0; i < addresses.size(); ++i) {
//					JsonObject address = addresses.get(i).getAsJsonObject();
//					
//					if (null == countryCode) {
//						countryCode = address.get("country_code").getAsString().toLowerCase();
//					}
//					
//					Entity addressEntity = new Entity(true);
//					addressEntity.setSubjectUri("_:address");
//					
//					try {
//						String addressLine = address.get("line_1").getAsString();
//						if (null != addressLine && false == addressLine.isEmpty()) {
//							addressEntity.addTriple(VCARD.ADDR_STR, new RdfObjectLiteral(addressLine, "@en"));
//						}
//					} catch (Exception e) {
//						// ignore
//					}
//					
//					String country = address.get("country").getAsString();
//					if (null != country && false == country.isEmpty()) {
//						addressEntity.addTriple(VCARD.ADDR_COUNTRY_NAME, new RdfObjectLiteral(country, "@en"));
//					}
//					
//					String geoNamesCountry = PropertyNormalizerUtils.getInstance().
//							getGeoNamesMapper().getGeoNamesCountryId(country, "@en");
//					if (null != geoNamesCountry && false == geoNamesCountry.isEmpty()) {
//						addressEntity.addTriple(VCARD.ADDR_COUNTRY_NAME, new RdfObjectUri(geoNamesCountry));
//					}
//					
//					String city = address.get("city").getAsString();
//					if (null != city && false == city.isEmpty()) {
//						addressEntity.addTriple(VCARD.ADDR_LOCALITY, new RdfObjectLiteral(city, "@en"));
//					}
//					
//					try {
//						JsonObject cityObject = address.get("geonames_city").getAsJsonObject();
//						if (null != cityObject) {
//							String geonamesId = cityObject.get("id").getAsString();
//							if (null != geonamesId) {
//								String geonamesUri = GeoNamesConst.createGeonamesUri(geonamesId);
//								addressEntity.addTriple(VCARD.ADDR_LOCALITY, new RdfObjectUri(geonamesUri));
//							}
//						}
//					} catch (Exception e) {
//						String geonamesId = PropertyNormalizerUtils.getInstance().
//								getGeoNamesMapper().getGeoNamesCityId(city, "@en", geoNamesCountry);
//						if (null != geonamesId) {
//							String geonamesUri = GeoNamesConst.createGeonamesUri(id);
//							addressEntity.addTriple(VCARD.ADDR_LOCALITY, new RdfObjectUri(geonamesUri));
//						}
//					}
//					
//					try {
//						String postcode = address.get("postcode").getAsString();
//						if (null != postcode && false == postcode.isEmpty()) {
//							RdfObjectLiteral postcodeObject = new RdfObjectLiteral(postcode, XSDDatatype.XSDstring.getURI());
//							addressEntity.addTriple(VCARD.ADDR_POST_CODE, postcodeObject);
//						}
//					} catch (Exception e) {
//						// ignore
//					}
//					
//					try {
//						String lattitude = address.get("lat").getAsString();
//						String longitude = address.get("lng").getAsString();
//						if (null != lattitude && false == lattitude.isEmpty() &&
//							null != longitude && false == longitude.isEmpty()) {
//							GeoCordinatesNormalizer.addPoint(longitude, lattitude, addressEntity);
//						}
//					} catch (Exception e) {
//						// ignore
//						/// TODO km : think about using nominatim here
//					}
//					
//					if (false == addressEntity.isEmpty()) {
//						this.currentEntity.addSubEntity(addressEntity);
//						
//						RdfObjectUri addressUri = new RdfObjectUri(addressEntity.getSubjectUri());
//						this.currentEntity.addTriple(VCARD.ADDR_HAS_ADDRESS, addressUri);
//					}				
//				}
//				
//				String subjectUri = CorpDbpedia.prefixResource + countryCode + "/grid_" + name.toLowerCase() + "_" + id;			
//				this.currentEntity.setSubjectUri(subjectUri);
//				
//				JsonArray links = currentInstitute.get("links").getAsJsonArray();
//				if (null != links && 0 < links.size()) {
//					for (int i = 0; i < links.size(); ++i) {
//						String homepage = links.get(i).getAsString();
//						
//						RdfObjectLiteral homepageObject = new RdfObjectLiteral(homepage, XSDDatatype.XSDstring.getURI());
//						this.currentEntity.addTriple(FOAF.homepage.toString(), homepageObject);
//					}
//				}
//							
//				try {
//					String wikipediaUrl = currentInstitute.get("wikipedia_url").getAsString();
//					if (null != wikipediaUrl && false == wikipediaUrl.isEmpty()) {
//						RdfObjectUri wikipediaObject = new RdfObjectUri(wikipediaUrl);
//						this.currentEntity.addTriple(RDFS.getURI() + "sameAs", wikipediaObject);
//					}
//				} catch (Exception e) {
//					// ignore
//				}
//				
//				JsonArray relationships = currentInstitute.get("relationships").getAsJsonArray();
//				if (null != relationships && 0 < relationships.size()) {
//					for (int i = 0; i < relationships.size(); ++i) {
//						JsonObject relatedEntity = relationships.get(i).getAsJsonObject();
//						String type = relatedEntity.get("type").getAsString().toLowerCase();
//						String relatedId = relatedEntity.get("id").getAsString();
//						
//						String predicate = CorpDbpedia.prefixOntology + "relationship/" + type;
//						RdfObjectLiteral idLiteral = new RdfObjectLiteral(relatedId, "@id");
//						
//						this.currentEntity.addTriple(predicate, idLiteral);					
//					}
//				}
//				
//				try {
//					JsonObject externalIds = currentInstitute.get("external_ids").getAsJsonObject();
//					if (null != externalIds) {
//						Set<Entry<String, JsonElement>> idElementss = externalIds.entrySet();
//						for (Entry<String, JsonElement> idElement : idElementss) {
//							String idName = idElement.getKey();
//							JsonArray ids = idElement.getValue().getAsJsonArray();
//							
//							for (int i = 0; i < ids.size(); ++i) {
//								String specificId = ids.get(i).getAsString();
//								
//								RdfObjectLiteral idObject = new RdfObjectLiteral(specificId, XSDDatatype.XSDstring.getURI());
//								this.currentEntity.addTriple(CorpDbpedia.prefixOntology + "id/" + idName, idObject);
//							}
//						}
//					}
//				} catch (Exception e) {
//					// ignore
//				}
//				
//				break;
//			}
//			
//			return true;
////		} catch (IOException e) {
////			throw new IngestionException("Was not able to read from json instance", e);
////		}
//	}
//
//	@Override
//	public Triple next() throws IngestionException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	public Entity nextEntity() throws IngestionException {
//		++this.instituteIndex;
//		
//		return this.currentEntity;
//	}
//
//	@Override
//	public void close() throws IOException {
//		// TODO Auto-generated method stub
//		
//	}
//
//}
