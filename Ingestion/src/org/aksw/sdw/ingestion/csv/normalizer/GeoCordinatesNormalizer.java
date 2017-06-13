package org.aksw.sdw.ingestion.csv.normalizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.VCARD;
import org.aksw.sdw.ingestion.csv.utils.CustomJenaType;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.datatypes.xsd.impl.XSDBaseStringType;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

//import com.google.common.cache.Cache;
//import com.google.common.cache.CacheBuilder;
import org.apache.jena.vocabulary.RDF;

import jersey.repackaged.com.google.common.cache.Cache;
import jersey.repackaged.com.google.common.cache.CacheBuilder;


/**
 * This class can be used to add geo location information
 * for this dataset
 * 
 * @author kay
 *
 */
public class GeoCordinatesNormalizer implements PropertyNormalizer {
	
	/** language which should be supported here */
	final static String countryCode = "en";
	
	/** nominatim query prefix */
	final static String queryPrefixEN = "?format=json&polygon_text=1&accept-language=en" +
										"&addressdetails=1&q=";
	
	final static String queryPrefixDE = "?format=json&polygon_text=1&accept-language=de" +
			"&addressdetails=1&q=";
	
	final static Cache<String, JSONArray> nominatimCache =
			CacheBuilder.newBuilder()
			.maximumSize(15000)
			.build();
	
	/** data type for virtuoso */
	final static String virtuosoGeoDataType = "http://www.openlinksw.com/schemas/virtrdf#Geometry";
	
	final static String virtuosoPredicateGeo = "http://www.opengis.net/ont/geosparql#asWKT";
	
	final static String virtuosoGeoType = "http://geovocab.org/geometry#Geometry";
	
	public void addPredicate(final StringBuilder builder, final Entity entity, final String ... predicates) {
		this.addPredicate(builder, entity, (ObjectNormaliser) null, predicates);
	}
	
	/**
	 * This method can be used to add a predicate to the address string builder
	 * 
	 * @param builder	- string builder which is used to store the address
	 * @param entityMap - entity map which contains all the known entity properties
	 * @param objectStringNormalizer - class which can be used to normalisee string
	 * @param predicates - array of predicates
	 */
	public void addPredicate(final StringBuilder builder, final Entity entity,
							 final ObjectNormaliser objectStringNormalizer, final String ... predicates) {
		for (String predicate : predicates) {
			List<RDFNode> rdfObject = entity.getRdfObjects(predicate);
			if (null != rdfObject && 1 == rdfObject.size()) {
				if (0 < builder.length()) {
					builder.append(",");
				}
			
				String objectString = null == objectStringNormalizer ?
						rdfObject.get(0).asLiteral().getLexicalForm() :
						objectStringNormalizer.normaliseObject(rdfObject.get(0).asLiteral().getLexicalForm());
						
				// remove any characters which are not part of standard words
				/// TODO km : support unicode!!!
				String cleanedObjectString = RegisteredAddressNormalizerCH.nonWordCharacter.
												matcher(objectString).replaceAll(" ").trim();						
				builder.append(cleanedObjectString);
				break;
			}
		}
	}
	
	/**
	 * This method can be used to obtain an address from an entity
	 * 
	 * @param entity
	 * @return address
	 */
	public String getAddress(final Entity entity) {
		if (null == entity) {
			return null;
		}
		
		StringBuilder builder = new StringBuilder();
		
		// add all the address information together
		this.addPredicate(builder, entity, VCARD.ADDR_STR);
		this.addPredicate(builder, entity, VCARD.ADDR_POST_CODE);
		this.addPredicate(builder, entity, VCARD.ADDR_LOCALITY);
		this.addPredicate(builder, entity, VCARD.ADDR_COUNTRY_NAME);	
		
		return builder.toString();
	}
	
	/**
	 * Obtains address data for a given address from a Nominatim server
	 * 
	 * @param client
	 * @param searchUrl
	 * @param searchParameters
	 * @return JSONArray with address data
	 * @throws ExecutionException
	 */
	protected JSONArray getAddressData(final CloseableHttpClient client,
									   final String searchUrl,
									   final String queryPrefix,
									   final String address) throws ExecutionException {
		if (null == client || null == searchUrl || null == queryPrefix || null == address) {
			return null;
		}
		
		String searchParameters = (queryPrefix + address).replaceAll(",\\s+", ",").replaceAll("\\s+","+");
		final String queryString = String.format("%s%s", searchUrl, searchParameters);
		
		JSONArray response = (JSONArray) GeoCordinatesNormalizer.nominatimCache.get(queryString, new Callable<JSONArray>() {

			@Override
			public JSONArray call() throws Exception {
				//System.out.println("Query: " + queryString);
				HttpGet get = new HttpGet(queryString);
				
				JSONArray response = null;
				int count = 5;
				while (0 <= --count) {
					try {
						response = client.execute(get, new ResponseHandler<JSONArray>() {
			
							@Override
							public JSONArray handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
								// if no response or empty response is returned --> null
								if (null == response || 2 == response.getEntity().getContentLength()) {
									return null;
								}
								
								InputStream contentStream = response.getEntity().getContent();
								if (null == contentStream) {
									return null;
								}
								InputStreamReader contentStreamReader = new InputStreamReader(contentStream);
								BufferedReader reader = new BufferedReader(contentStreamReader);
			
								// get the response json string
								String line = reader.readLine();
								try {
									JSONArray jsonArray = new JSONArray(line);
									if (1 <= jsonArray.length()) {
										return jsonArray;
									} else {
										return null;
									}
									
								} catch (JSONException e) {
									throw new ClientProtocolException(e);
								}
							}
						});
					} catch (Exception e) {
						continue;
					}
				
				break;
			}
	
			return (null == response) ? new JSONArray("[]") : response;
		}
		});
		
		return response;
	}
	
	@Override
	public void normalize(final Entity entity, final ConversionStats stats) throws IngestionException {		
		try {
			final CloseableHttpClient client = HttpClients.createDefault();
							
			JSONArray addressArrayEN = null;	
			JSONArray addressArrayDE = null;
			
			String address = this.getAddress(entity);
			String[] parts = address.split(",");
			if (null == parts || 0 == parts.length) {
				throw new IngestionException("Did not get valid address: " + address);
			}			
			
			int maxIndex = parts.length - 1;
			int i = 0;
			for (; i < maxIndex; ++i) {					
				String searchUrl = "http://nominatim.linkedgeodata.org/";
				
				String queryAddress = parts[i];
				for (int ii = i + 1; ii <= maxIndex; ++ii) {
					if (parts[ii].isEmpty()) {
						continue;
					}
					
					queryAddress += "," + parts[ii];
					
				}
				
				queryAddress = queryAddress.trim();
				
				addressArrayEN = this.getAddressData(client, searchUrl, queryPrefixEN, queryAddress);
				if (null == addressArrayEN || 0 == addressArrayEN.length()) {
					continue;
				}
				
				addressArrayDE = this.getAddressData(client, searchUrl, queryPrefixDE, queryAddress);
				
				stats.incrementStats(GeoCordinatesNormalizer.class, "gotAddressCount");
				
				//if (response instanceof JSONArray) {
					
					boolean didNotFindPlace = true;
//					JSONArray array = (JSONArray) response;

/// TODO km : get a better handle on polygon
//					for (int ii = 0; ii < array.length(); ++ii) {
//
//						JSONObject placeObject = (JSONObject) array.get(ii);
//						
//						String geoText = (String) placeObject.get("geotext");
//						boolean isPolygon = geoText.startsWith("POLYGON");
//						boolean isLineString = geoText.startsWith("LINESTRING");
//						if (null == geoText || false == isPolygon && false == isLineString) {
//							continue;
//						}
//						
//						if (isLineString) {
//							// LINESTRING is a special case of POLYGON
//							geoText = geoText.replace("LINESTRING", "POLYGON");
//						}
//						this.addPolygon(geoText, addressEntity);
//						this.addAddressData(placeObject, GeoCordinatesNormalizer.countryCode, addressEntity);
//						
//						didNotFindPlace = false;
//						break;
//					}
					
					// in case no polygon place was found --> look for point
					if (didNotFindPlace) {
						JSONObject placeObjectEN  = null;
						JSONObject placeObjectDE = null;
						
						for (int ii = 0; ii < addressArrayEN.length(); ++ii) {
							placeObjectEN = (JSONObject) addressArrayEN.get(ii);
							String placeIdEN = placeObjectEN.getString("place_id");
							
							// look for matching place
							placeObjectDE = null;
							for (int iii = 0; iii < addressArrayDE.length(); ++iii) {
								JSONObject tmpPlaceObjectDE = (JSONObject) addressArrayDE.get(iii);
								String placeIdDE = tmpPlaceObjectDE.getString("place_id");
								if (placeIdDE.equals(placeIdEN)) {
									placeObjectDE = tmpPlaceObjectDE;
									break;
								}
							}
							
							if (0 < addressArrayDE.length() && null == placeObjectDE) {
								continue;
							} else {
								break;
							}
						}
						
						if (null == placeObjectDE) {
							// in case we not find German one --> use first English match
							placeObjectEN = (JSONObject) addressArrayEN.get(0);
						}
													
						this.addAddressData(placeObjectEN, placeObjectDE, GeoCordinatesNormalizer.countryCode, entity);
					}
//				} else {
//					throw new IngestionException("Got unexpected response from virtuoso: " +
//												response.getClass().getName());
//				}
				
				break;
			}
				
			if (null == addressArrayEN) {
				stats.incrementStats(GeoCordinatesNormalizer.class, "gotNoAddressCount");
				stats.setStatText(GeoCordinatesNormalizer.class, "gotNoAddressFor", address);
			}
		} catch (Exception e) {
			throw new IngestionException("Was not able to obtain geo coordinages", e);
		}
		
	}
	
	/**
	 * This method can be used to add point geo-coordinates for an entity
	 * 
	 * @param geoText		- polygon string
	 * @param entityMap		- entityMap which contains the entities properties and their values
	 */
	public static void addPoint(final String longitude, final String lattitude, final Entity entity) {	
		
		String geoText = "POINT(" + longitude + " " + lattitude + ")";
		
		/// TODO add data type
		entity.addTripleWithLiteral(GeoCordinatesNormalizer.virtuosoPredicateGeo,
				geoText, new CustomJenaType(GeoCordinatesNormalizer.virtuosoGeoDataType));

		ResourceImpl geometryObject = new ResourceImpl(GeoCordinatesNormalizer.virtuosoGeoType);
		entity.addTriple(RDF.type.toString(), geometryObject);
	}
	
	/**
	 * This method can be used to add polygon geo-coordinates for an entity
	 * 
	 * @param geoText		- polygon string
	 * @param entityMap		- entityMap which contains the entities properties and their values
	 */
	protected void addPolygon(final String geoText, final Entity entity) {
		
		entity.addTripleWithLiteral(GeoCordinatesNormalizer.virtuosoPredicateGeo,
				geoText, new CustomJenaType(GeoCordinatesNormalizer.virtuosoGeoDataType));

		ResourceImpl geometryObject = new ResourceImpl(GeoCordinatesNormalizer.virtuosoGeoType);
		entity.addTriple(RDF.type.toString(), geometryObject);
	}
	
	/**
	 * This method can be used to add potential missing address information
	 * 
	 * @param placeObjectEN
	 * @param placeObjectDE
	 * @param entityMap
	 * @throws IngestionException 
	 */
	protected void addAddressData(final JSONObject placeObjectEN,
								  final JSONObject placeObjectDE,
								  final String countryCode,
								  final Entity parentEntity) throws IngestionException {
		try {
			// get entity which will store the addresses
			Entity addressEntity = parentEntity.getSubEntityByCategory("address");
			
			// get coordinates
			String longitude = placeObjectEN.getString("lon");
			String lattitude = placeObjectEN.getString("lat");
				
			// get geo text annd store in address entity
			GeoCordinatesNormalizer.addPoint(longitude, lattitude, addressEntity);
			
			// get response from nominatim
			JSONObject addressJsonEN = (JSONObject) placeObjectEN.get("address");
			JSONObject addressJsonDE = null;
			if (null != placeObjectDE) {
				addressJsonDE = (JSONObject) placeObjectDE.get("address");
			}

			final String countryCodeAtEN = "en";
			final String countryCodeAtDE = "de";
			
			String countryNameEN = null;
			String countryNameDE = null;
			try {
				countryNameEN = (String) addressJsonEN.get("country");
				countryNameDE = null != addressJsonDE ? (String) addressJsonDE.get("country") : null;
			} catch (Exception e) {
				// ignore
			}
			
			List<RDFNode> countryNameObjects = parentEntity.getRdfObjects(VCARD.ADDR_COUNTRY_NAME);
			if (null != countryNameEN) {
				
				addressEntity.addTripleWithLiteral(VCARD.ADDR_COUNTRY_NAME, countryNameEN, countryCodeAtEN);

				if (null != countryNameDE) {				
					addressEntity.addTripleWithLiteral(VCARD.ADDR_COUNTRY_NAME, countryNameDE, countryCodeAtDE);
				}
			} else {
				for (RDFNode countryNameObject : countryNameObjects) {
					if (countryNameObject instanceof Literal) {
						String lanuageCode = countryNameObject.asLiteral().getLanguage();
						
						switch(lanuageCode) {
							case countryCodeAtEN:
								countryNameEN = countryNameObject.asLiteral().getLexicalForm();
								break;
							case countryCodeAtDE:
								countryNameDE = countryNameObject.asLiteral().getLexicalForm();
								break;
						}						
					}
					
					addressEntity.addTriple(VCARD.ADDR_COUNTRY_NAME, countryNameObject);
				}				
			}
			
			// delete from parent
			parentEntity.deleteProperty(VCARD.ADDR_COUNTRY_NAME);
			
			// add geonames entry, if possible
			String geoNamesCountryUri = null;
			if (null != countryNameDE || null != countryNameEN) {
				
				geoNamesCountryUri = PropertyNormalizerUtils.getInstance().
						getGeoNamesMapper().getGeoNamesCountryId(countryNameEN, countryCodeAtEN);
				if (null == geoNamesCountryUri && null != countryNameDE) {				
					geoNamesCountryUri = PropertyNormalizerUtils.getInstance().
							getGeoNamesMapper().getGeoNamesCountryId(countryNameDE, countryCodeAtDE);
				}
				
				ResourceImpl countryNameObject = new ResourceImpl(geoNamesCountryUri);					
				addressEntity.addTriple(VCARD.ADDR_COUNTRY_NAME, countryNameObject);
			}
			
			String cityNameEN = null;
			String cityNameDE = null;
			List<RDFNode> cityNameObjects = parentEntity.getRdfObjects(VCARD.ADDR_LOCALITY);
			if (null == cityNameObjects || cityNameObjects.isEmpty()) {
				try {
					cityNameDE = null != addressJsonDE ? (String) addressJsonDE.get("city") : null;
					cityNameEN = (String) addressJsonEN.get("city");
				} catch (Exception e) {
					try {
						// sometimes it is declared a town and not a city
						cityNameDE = null != addressJsonDE ? (String) addressJsonDE.get("town") : null;
						cityNameEN = (String) addressJsonEN.get("town");
					} catch (Exception e2) {
						// ignore
					}
				}
				
				if (null != cityNameEN) {								
					addressEntity.addTripleWithLiteral(VCARD.ADDR_LOCALITY, cityNameEN, countryCodeAtEN);
				}
				
				if (null != cityNameDE) {										
					addressEntity.addTripleWithLiteral(VCARD.ADDR_LOCALITY, cityNameDE, countryCodeAtEN);
				}
			}  else {
				
				for (RDFNode cityNameObject : cityNameObjects) {
					if (cityNameObject instanceof Literal) {
						String languageCode = cityNameObject.asLiteral().getLanguage();
						
						switch(languageCode) {
							case countryCodeAtEN:
								cityNameEN = cityNameObject.asLiteral().getLexicalForm();
								break;
							case countryCodeAtDE:
								cityNameDE = cityNameObject.asLiteral().getLexicalForm();
								break;
						}						
					}
					
					addressEntity.addTriple(VCARD.ADDR_LOCALITY, cityNameObject);
				}
				
				if (null == cityNameDE) {
					try {
						cityNameDE = null != addressJsonDE ? (String) addressJsonDE.get("city") : null;
					} catch (Exception e) {
						try {
							// sometimes it is declared a town and not a city
							cityNameDE = null != addressJsonDE ? (String) addressJsonDE.get("town") : null;
						} catch (Exception e2) {
							// ignore
						}
					}
					
					if (null != cityNameDE) {					
						addressEntity.addTripleWithLiteral(VCARD.ADDR_LOCALITY, cityNameDE, countryCodeAtDE);
					}
				}
				
				if (null == cityNameEN) {
					try {
						cityNameEN = (String) addressJsonEN.get("city");
					} catch (Exception e) {
						try {
							// sometimes it is declared a town and not a city
							cityNameEN = (String) addressJsonEN.get("town");
						} catch (Exception e2) {
							// ignore
						}
					}
					
					if (null != cityNameEN) {					
						addressEntity.addTripleWithLiteral(VCARD.ADDR_LOCALITY, cityNameEN, countryCodeAtEN);
					}
				}
				
			}
			
			parentEntity.deleteProperty(VCARD.ADDR_LOCALITY);
			
			if (null != cityNameEN || null != cityNameDE) {
				RDFNode cityNameObject = null;
				
				String geoNamesCityUri = null;
				if (null != cityNameEN) {
					geoNamesCityUri = PropertyNormalizerUtils.getInstance().
							getGeoNamesMapper().getGeoNamesCityId(cityNameEN, countryCodeAtEN, geoNamesCountryUri);
				}
				
				if (null == geoNamesCityUri && null != cityNameDE) {
					geoNamesCityUri = PropertyNormalizerUtils.getInstance().
							getGeoNamesMapper().getGeoNamesCityId(cityNameDE, countryCodeAtDE, geoNamesCountryUri);
				}
				
				if (null != geoNamesCityUri) {
					cityNameObject = new ResourceImpl(geoNamesCityUri);
					addressEntity.addTriple(VCARD.ADDR_LOCALITY, cityNameObject);
				}
			}
			
			List<RDFNode> stateNameObjects = parentEntity.getRdfObjects(VCARD.ADDR_REGION);
			if (null == stateNameObjects || stateNameObjects.isEmpty()) {
				String stateNameEN = null;
				String stateNameDE = null;
				try {
					stateNameEN = (String) addressJsonEN.get("state");
					stateNameDE = null != addressJsonDE ? (String) addressJsonDE.get("state") : null;
				} catch (JSONException e) {
					// ignore
				}
				if (null != stateNameEN) {				
					addressEntity.addTripleWithLiteral(VCARD.ADDR_REGION, stateNameEN, countryCodeAtEN);
				}
				
				if (null != stateNameDE) {					
					addressEntity.addTripleWithLiteral(VCARD.ADDR_REGION, stateNameDE, countryCodeAtDE);
				}
			} else {
				for (RDFNode stateNameObject : stateNameObjects) {
					addressEntity.addTriple(VCARD.ADDR_REGION, stateNameObject);
				}
			}
			
			parentEntity.deleteProperty(VCARD.ADDR_REGION);
			
			List<RDFNode> postCodeObjects = parentEntity.getRdfObjects(VCARD.ADDR_POST_CODE);
			if (null == postCodeObjects || postCodeObjects.isEmpty()) {
				String postcode = null;
				try {
					postcode = (String) addressJsonEN.get("postcode");
				} catch (JSONException e) {
					// ignore
				}
				
				if (null != postcode) {					
					addressEntity.addTripleWithLiteral(VCARD.ADDR_POST_CODE, postcode, XSDBaseStringType.XSDstring);
				}
			} else {
				addressEntity.addTriple(VCARD.ADDR_POST_CODE, postCodeObjects.get(0));
			}
			
			parentEntity.deleteProperty(VCARD.ADDR_POST_CODE);
			
			List<RDFNode> streetObjects = parentEntity.getRdfObjects(VCARD.ADDR_STR);
			if (null != streetObjects && false == streetObjects.isEmpty()) {
				addressEntity.addTriple(VCARD.ADDR_STR, streetObjects.get(0));

			}
			
			parentEntity.deleteProperty(VCARD.ADDR_STR);

		} catch (Exception e) {
			throw new IngestionException("Was not able to add address", e);
		}
	}
	
	/**
	 * This class can be used to normalise an object string where required
	 * @author kay
	 *
	 */
	abstract class ObjectNormaliser {
		abstract String normaliseObject(final String objectString);
	}
}
