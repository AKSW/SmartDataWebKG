package org.aksw.sdw.ingestion.csv.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.GeoNamesConst;
import org.aksw.sdw.ingestion.csv.normalizer.PropertyNormalizerUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.util.Pair;

import aksw.org.kg.KgException;

//import com.google.common.cache.Cache;
//import com.google.common.cache.CacheBuilder;
//import com.google.common.cache.CacheLoader;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import jersey.repackaged.com.google.common.cache.Cache;
import jersey.repackaged.com.google.common.cache.CacheBuilder;

/**
 * This class can be used to import a countryinfo.txt mapping from
 * http://download.geonames.org/export/dump/countryInfo.txt
 * 
 * @author kay
 *
 */
public class GeoNamesMapper {

	final static Logger logger = Logger.getLogger(GeoNamesMapper.class);
	
	/** uses csv input file with mapping information to retrieve data */
	final protected CsvReader csvReader;
	
	/** URL to the Sparql endpoint */
	final String sparqlEndpoint;
	
	/** name of the graph */
	final String graphName;
	
	/**
	 * This is a country object cache
	 */
	final static Cache<String, Object> countryCache =
			CacheBuilder.newBuilder()
			.concurrencyLevel(8)
			.maximumSize(15000)
			.build();
	
	/**
	 * This is a cache for city information
	 */
	final static Cache<List<String>, Object> cityCache =
			CacheBuilder.newBuilder()
			.concurrencyLevel(8)
			.maximumSize(15000)
			.build();	
	
	/** list of synonyms for country names */
	static final Map<String, List<String>> countryNameSynonyms = getCountryNameSynonyms();
	static Map<String, List<String>> getCountryNameSynonyms() {
		Map<String, List<String>> synonymMap = new HashMap<>();

		List<String> usa = Arrays.asList("usa", "u.s.a.", "united states of america", "us", "u.s.");
		synonymMap.put("united states", usa);

		List<String> gb = Arrays.asList("uk", "gb", "great britain");
		synonymMap.put("united kingdom", gb);
		
		List<String> virginIslandsUK = Arrays.asList("virgin islands");
		synonymMap.put("british virgin islands", virginIslandsUK);

		// get unmodifiable map
		return Collections.unmodifiableMap(synonymMap);
	}

	/**
	 * 
	 * @param fileName
	 *            - file name to geo names mapping file
	 * @throws KgException
	 */
	public GeoNamesMapper(final String fileName) throws IngestionException {
		this(fileName, "http://localhost:9890/sparql", "http://geonames.org");
	}
	
	public GeoNamesMapper() throws IngestionException {
		this(PropertyNormalizerUtils.geoNamesMappingFilePath, "http://localhost:9890/sparql", "http://geonames.org");
	}

	/**
	 * 
	 * @param fileName
	 *            			 - file name to geo names mapping file
	 * @param sparqlEndpoint - address of sparql endpoint
	 * @param graphName 	 - name of graph which should be searched
	 * @throws KgException
	 */
	public GeoNamesMapper(final String fileName, final String sparqlEndpoint, final String graphName) throws IngestionException {

		URL url = GeoNamesMapper.class.getClassLoader().getResource(fileName);
		if (null == url) {
			File file = new File(fileName);
			if (false == file.exists() && false == file.isFile()) {
				throw new IngestionException("Was not able to find file: " + fileName);
			}
			
			try {
				url = file.toURL();
			} catch (MalformedURLException e) {
				throw new IngestionException("Was not able to get url", e);
			}
		}
		
		Map<String, Map<String, List<String>>> synonymMap = new HashMap<>();
		synonymMap.put("Country", GeoNamesMapper.countryNameSynonyms);
		
		this.csvReader = new CsvReader(new File(url.getFile()), "\t", Arrays.asList("Country","geonameid"), synonymMap);
		
		this.sparqlEndpoint = "http://localhost:9890/sparql";
		this.graphName = "http://geonames.org";
	}	
	
	public Map<String, List<String>> getGeoNamesSynonyms() {
		return GeoNamesMapper.countryNameSynonyms;
	}
	
	protected String getGeoNamesIdFromInfoFile(final String countryName) throws IngestionException {
		if (null == countryName) {
			return null;
		}

		String id = this.csvReader.getEntityFieldValue("Country",
				countryName.trim().toLowerCase(), "geonameid");
		
		if (null == id || id.trim().isEmpty()) {
			return null;
		} else {
			return GeoNamesConst.createGeonamesUri(id);
		}
	}
	
	protected String getCoordinatesFromIdQuery(final String geonamesId) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("SELECT * WHERE {");
		builder.append("       <").append(geonamesId).append("> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat . \n");
		builder.append("       <").append(geonamesId).append("> <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?long . \n }");
		
		return builder.toString();
		
	}
	
	protected String getParentInstanceQuery(final String geonamesId) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("SELECT * WHERE {");
		builder.append("       <").append(geonamesId).append("> <http://www.geonames.org/ontology#parentFeature>* ?o . \n");
		builder.append("       ?o <http://www.geonames.org/ontology#featureClass> ?featureClass . \n");
		builder.append("       ?o <http://www.geonames.org/ontology#featureCode> ?featureCode . \n");
		builder.append("       ?o <http://www.geonames.org/ontology#parentCountry> ?parentCountry . \n");
		builder.append("       ?o <http://www.geonames.org/ontology#name> ?name . \n }");
		
		return builder.toString();
	}
	
	protected String getCountryName2GeoNamesCountryIdQuery(final String countryName, final String languageCode) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("SELECT DISTINCT * WHERE {\n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#featureClass>");
		builder.append("              <http://www.geonames.org/ontology#A> . \n}");
		builder.append("		{ ?id <http://www.geonames.org/ontology#officialName> \"").append(countryName).append("\"@").append(languageCode).append(" } UNION\n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#officialName> \"").append(countryName).append("\". } UNION\n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#alternateName> \"").append(countryName).append("\"@").append(languageCode).append(" } UNION\n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#alternateName> \"").append(countryName).append("\". } UNION\n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#name> \"").append(countryName).append("\". } UNION\n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#name> \"").append(countryName).append("\"@").append(languageCode).append(" } . } LIMIT 10000");
		
		return builder.toString();
	}
	
	protected String getCountyName2GeoNamesCountryIdQuery(final String countyName, final String geonamesCountryId, final String languageCode) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("SELECT DISTINCT * WHERE {");
		builder.append("		{ ?id <http://www.geonames.org/ontology#featureClass>");
		builder.append("              <http://www.geonames.org/ontology#A> .} \n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#featureCode>");
		builder.append("              <http://www.geonames.org/ontology#A.ADM1> .} \n");
		if (null != geonamesCountryId) {
			builder.append("		{ ?id <http://www.geonames.org/ontology#parentCountry>  <").append(geonamesCountryId).append("> .}\n");
		}
	
		builder.append("		{ ?id <http://www.geonames.org/ontology#officialName> \"").append(countyName).append("\"@").append(languageCode).append(". } UNION\n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#officialName> \"").append(countyName).append("\". } UNION\n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#alternateName> \"").append(countyName).append("\"@").append(languageCode).append(" } UNION\n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#alternateName> \"").append(countyName).append("\". } UNION\n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#name> \"").append(countyName).append("\". } UNION\n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#name> \"").append(countyName).append("\"@").append(languageCode).append(" } . } LIMIT 1000");
				
		return builder.toString();
	}
	
	protected String getName2GeoNamesCountryCodeQuery(final String geonamesUri) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("SELECT DISTINCT * WHERE {");
		builder.append("		{ <").append(geonamesUri).append("> <http://www.geonames.org/ontology#countryCode> ?countryCode } } LIMIT 10000");
		
		return builder.toString();
	}
	
	protected String getName2GeoNamesCountryNameQuery(final String geonamesId) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("SELECT DISTINCT * WHERE {\n");
		builder.append("{ <").append(geonamesId).append("> <http://www.geonames.org/ontology#officialName> ?officialCountryName . } UNION\n");
		builder.append("{ <").append(geonamesId).append("> <http://www.geonames.org/ontology#alternateName> ?alternateCountryName . } UNION\n");
		builder.append("{ <").append(geonamesId).append("> <http://www.geonames.org/ontology#name> ?countryName . }\n");
		builder.append(" } LIMIT 10000");
		
		return builder.toString();
	}
	
	protected String getName2GeoNamesCityIdQuery(final String cityName, final String parentCountryId, final String languageCode) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("SELECT DISTINCT * WHERE {\n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#featureClass>\n");
		builder.append("              <http://www.geonames.org/ontology#P> . }\n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#officialName> \"").append(cityName).append("\"@").append(languageCode).append(" . } UNION \n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#alternateName> \"").append(cityName).append("\"@").append(languageCode).append(" . } UNION \n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#name> \"").append(cityName).append("\"@").append(languageCode).append(" . } UNION \n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#officialName> \"").append(cityName).append("\" . } UNION \n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#alternateName> \"").append(cityName).append("\" . } UNION \n");
		builder.append("		{ ?id <http://www.geonames.org/ontology#name> \"").append(cityName).append("\" . } \n");

		if (null != parentCountryId) {
			builder.append("		{ ?id <http://www.geonames.org/ontology#parentCountry> <").append(parentCountryId).append("> . } \n");
		}
		
		builder.append("} LIMIT 10000");
		
		return builder.toString();
	
	}
	
	/**
	 * Get IDs for countries
	 * 
	 * @param countryName
	 * @param languageCode
	 * @return
	 * @throws IngestionException
	 */
	@SuppressWarnings("unchecked")
	protected List<String> getGeoNamesCountryIdFromSparqlEndpoint(final String countryName, final String languageCode) throws IngestionException {
		if (null == countryName) {
			return null;
		}
		
		if (null == this.sparqlEndpoint || null == this.graphName) {
			return null;
		}
		
		final String queryString = this.getCountryName2GeoNamesCountryIdQuery(countryName.trim(), languageCode);
		
		try {
			List<String> ids = (List<String>) GeoNamesMapper.countryCache.get(queryString, new Callable<Object>() {
	
				@Override
				public Object call() throws Exception {
					QueryExecution queryResult = QueryExecutionFactory.sparqlService(
							sparqlEndpoint, queryString, graphName);
					ResultSet selectResult = queryResult.execSelect();
	
					
					List<String> ids = new ArrayList<>();
					while (selectResult.hasNext()) {
						QuerySolution querySolution = selectResult.next();
						
						Resource uri = querySolution.getResource("id");
						String id = uri.toString();
						
						ids.add(id);
					}
	
					return ids;
				}
			});
			
			return ids;
		} catch (Exception e) {
			throw new IngestionException("Was not able to load country data for query: " + queryString, e);
		}
	}
	
	/**
	 * Get geonames IDs for counties which are in a particular country
	 * 
	 * @param countyName
	 * @param geonamesCountryId
	 * @param languageCode
	 * @return
	 * @throws IngestionException
	 */
	@SuppressWarnings("unchecked")
	protected List<String> getGeoNamesCountyIdFromSparqlEndpoint(final String countyName, final String geonamesCountryId, final String languageCode) throws IngestionException {
		if (null == countyName) {
			return null;
		}
		
		if (null == this.sparqlEndpoint || null == this.graphName) {
			return null;
		}
		
		final String queryString = this.getCountyName2GeoNamesCountryIdQuery(countyName, geonamesCountryId, languageCode);
		
		try {
			List<String> ids = (List<String>) GeoNamesMapper.countryCache.get(queryString, new Callable<Object>() {
	
				@Override
				public Object call() throws Exception {
					QueryExecution queryResult = QueryExecutionFactory.sparqlService(
							sparqlEndpoint, queryString, graphName);
					ResultSet selectResult = queryResult.execSelect();
	
					
					List<String> ids = new ArrayList<>();
					while (selectResult.hasNext()) {
						QuerySolution querySolution = selectResult.next();
						
						Resource uri = querySolution.getResource("id");
						String id = uri.toString();
						
						ids.add(id);
					}
	
					return ids;
				}
			});
			
			return ids;
		} catch (ExecutionException e) {
			throw new IngestionException("Was not able to load country data", e);
		}
	}
	
	protected String getGeoNamesCityIdFromSparqlEndpoint(final String cityName, final String languageCode, final String countryId) throws IngestionException {
		if (null == cityName) {
			return null;
		}
		
		if (null == this.sparqlEndpoint || null == this.graphName) {
			return null;
		}
		
		try {
			final String queryString = this.getName2GeoNamesCityIdQuery(cityName, countryId, languageCode);
			
			List<String> keys = Arrays.asList(queryString, countryId);
			String id = (String) GeoNamesMapper.cityCache.get(keys, new Callable<Object>() {
	
				@Override
				public Object call() throws Exception {
					QueryExecution queryResult = QueryExecutionFactory.sparqlService(
							sparqlEndpoint, queryString, graphName);
					ResultSet selectResult = queryResult.execSelect();

					String id = "";
					while (selectResult.hasNext()) {
						QuerySolution querySolution = selectResult.next();
						
						Resource uri = querySolution.getResource("id");
						id = uri.toString();
					}
					
					return id;
				}
			});
			
			return id.isEmpty() ? null : id;
		} catch (ExecutionException e) {
			throw new IngestionException("Was not able to load city data", e);
		}
	}
	
	protected String getCountryCodeFromId(final String geonamesUri) throws IngestionException {
		
		try {
			final String queryString = getName2GeoNamesCountryCodeQuery(geonamesUri);
			
			String countryCode = (String) GeoNamesMapper.countryCache.get(queryString, new Callable<Object>() {
	
				@Override
				public Object call() throws Exception {
					QueryExecution queryResult = QueryExecutionFactory.sparqlService(
							sparqlEndpoint, queryString, graphName);
					ResultSet selectResult = queryResult.execSelect();
	
					
					String countryCode = null;
					while (selectResult.hasNext()) {
						QuerySolution querySolution = selectResult.next();
						
						Literal literal = querySolution.getLiteral("countryCode");
						countryCode = literal.toString();
						if (null != countryCode) {
							break;
						}
					}
	
					return countryCode;
				}
			});
			
			return countryCode;
		} catch (Exception e) {
			throw new IngestionException("Was not able to get country code", e);
		}
	}
	
	protected String getInstanceNameFromId(final String geonamesUri, final String langId) throws IngestionException {
		
		final String queryString = this.getName2GeoNamesCountryNameQuery(geonamesUri);
		
		try {			
			
			@SuppressWarnings("unchecked")
			List<String> names = (List<String>) GeoNamesMapper.countryCache.get(queryString, new Callable<Object>() {
	
				@Override
				public Object call() throws Exception {
					QueryExecution queryResult = QueryExecutionFactory.sparqlService(
							sparqlEndpoint, queryString, graphName);
					ResultSet selectResult = queryResult.execSelect();
	
					final List<String> officialNames = new ArrayList<>();
					final List<String> alternateNames = new ArrayList<>();
					
					while (selectResult.hasNext()) {
						QuerySolution querySolution = selectResult.next();
						
						Literal literal = querySolution.getLiteral("officialCountryName");
						if (null != literal) {
							officialNames.add(literal.toString());
						}
						
						literal = querySolution.getLiteral("alternateCountryName");
						if (null != literal) {
							alternateNames.add(literal.toString());
						}
						literal = querySolution.getLiteral("countryName");
						if (null != literal) {
							alternateNames.add(literal.toString());
						}
					}
	
					// add alternate names after offical ones
					officialNames.addAll(alternateNames);
					return officialNames;
				}
			});
			
			String matchingCountryName = null;
			for (String countryName : names) {
				if (countryName.endsWith(langId)) {
					matchingCountryName = countryName;
					break;
				}
			}
					
			return matchingCountryName;
		} catch (Exception e) {
			throw new IngestionException("Was not able to get country code for query: " + queryString, e);
		}
	}
	
	static public class Coordinates {
		final public String latitude;
		final public String longitude;
		
		public Coordinates(final String latitude, final String longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
		}
	}
	
	/**
	 * This method can be used to get geo coordinates for a geonames ID.
	 * 
	 * @param geonamesId - geonames id of a location which is situated within a region/county and a country
	 * @return
	 * @throws IngestionException 
	 */
	public Coordinates getCoordinatesFromId(final String geonamesId) throws IngestionException {
		
		final String coordinateQuery = this.getCoordinatesFromIdQuery(geonamesId);
		
		try {			
			
			Coordinates parentsPair = (Coordinates) GeoNamesMapper.countryCache.get(coordinateQuery, new Callable<Object>() {
	
				@Override
				public Object call() throws Exception {
					
					QueryExecution queryResult = QueryExecutionFactory.sparqlService(
							sparqlEndpoint, coordinateQuery, graphName);
					
					ResultSet selectResult = queryResult.execSelect();
					
					Coordinates coordinates = null;	

					while (selectResult.hasNext()) {
						QuerySolution querySolution = selectResult.next();
						
						RDFNode latitudeNode = querySolution.get("lat");
						RDFNode longitudeNode = querySolution.get("long");
						
						String latitude = latitudeNode.asLiteral().getString();
						String longitude = longitudeNode.asLiteral().getString();
						
						coordinates = new Coordinates(latitude, longitude);
						break;
					}
					
					queryResult.close();
					
					return coordinates;
				}
			});
					
			return parentsPair;
		} catch (Exception e) {
			throw new IngestionException("Was not able to parent country information: " + coordinateQuery, e);
		}
		
	}
	
	/**
	 * This method can be used to get a parent county/region and country from a city/village/etc. geonames id.
	 * 
	 * @param geonamesId - geonames id of a location which is situated within a region/county and a country
	 * @return
	 * @throws IngestionException 
	 */
	protected Pair<String, String> getParentCountyAndCountryData(final String geonamesId) throws IngestionException {
		
		final String parentQuery = this.getParentInstanceQuery(geonamesId);
		
		try {			
			
			@SuppressWarnings("unchecked")
			Pair<String, String> parentsPair = (Pair<String, String>) GeoNamesMapper.countryCache.get(parentQuery, new Callable<Object>() {
	
				@Override
				public Object call() throws Exception {
					
					QueryExecution queryResult = QueryExecutionFactory.sparqlService(
							sparqlEndpoint, parentQuery, graphName);
					
					ResultSet selectResult = queryResult.execSelect();
	
					Pair<String, String> pair = null;
					String previousFeatureCode = "Z";
					while (selectResult.hasNext()) {
						QuerySolution querySolution = selectResult.next();
						
						RDFNode uri = querySolution.get("o");						
						RDFNode featureCodeUri = querySolution.get("featureCode");
						RDFNode parentCountry = querySolution.get("parentCountry");
						
						// find country/region
						String featureCode = featureCodeUri.toString().substring("http://www.geonames.org/ontology#".length());
						if (0 < previousFeatureCode.compareTo(featureCode)) {
							pair = new Pair<String, String>(uri.toString(), parentCountry.toString());
							
							previousFeatureCode = featureCode.toString();
							
							// if we found highest county --> happy days
							if (featureCode.toString().endsWith("A.ADM1")) {
								break;
							}
						}
					}
					
					queryResult.close();
					
					return pair;
				}
			});
					
			return parentsPair;
		} catch (Exception e) {
			throw new IngestionException("Was not able to parent country information: " + parentQuery, e);
		}
		
	}
	
	/**
	 * This method can be used to get a parent county/region and country from a city/village/etc. geonames id.
	 * 
	 * @param geonamesId - geonames id of a location which is situated within a region/county and a country
	 * @return
	 * @throws IngestionException 
	 */
	public Pair<String, String> getParentCountyAndCountry(final String geonamesId) throws IngestionException {
		if (null == geonamesId) {
			return null;
		}
		
		return this.getParentCountyAndCountryData(geonamesId);
	}
	
	public String getCountryCode(final String geonamesUri) throws IngestionException {
		if (null == geonamesUri) {
			return null;
		}
		
		return this.getCountryCodeFromId(geonamesUri); 
	}
	
	public List<String> getLanguageCodes(final String geonamesUri) {
		if (null == geonamesUri) {
			return null;
		}
		
		try {
			int startId = geonamesUri.lastIndexOf("/", geonamesUri.length() - 2) + 1;		
			String geonamesId = geonamesUri.substring(startId, geonamesUri.length() - 1);
	
			String languages = this.csvReader.getEntityFieldValue("geonameid",
					geonamesId, "Languages");
			if (null == languages || languages.isEmpty()) {
				return null;
			}
			
			String[] languagesCodes = languages.split(",");
			List<String> languageCodeList = new ArrayList<>();
			
			for (String languageCode : languagesCodes) {
				
				int end = languageCode.indexOf("-");			
				end = (0 > end) ?  languageCode.length() : end;
				String cleanLanguageCode = languageCode.substring(0, end);
				
				languageCodeList.add(cleanLanguageCode);
			}
			
			return languageCodeList;
			
		} catch (Exception e) {
			return null;
		}
	}
	
	public String getNameFromId(final String geonameUri, final String languageCode) throws IngestionException {
		if (null == geonameUri) {
			return null;
		}
		
		String name = this.getInstanceNameFromId(geonameUri, languageCode);
		if (null == name) {
			return null;
		} else if (name.contains("@")) {
			int index = name.indexOf("@");
			return name.substring(0, index);
		} else {
			return name;
		}
	}
	
	public String getGeoNamesCountryId(final String countryName, final String languageCode) throws IngestionException {
		if (null == countryName) {
			return null;
		}
		
		String geoNamesId = null;
		if (null == languageCode || "en".equals(languageCode.toLowerCase())) {
			geoNamesId = this.getGeoNamesIdFromInfoFile(countryName);
		}
		
		if (null == geoNamesId) {
			List<String> geoNamesIds = this.getGeoNamesCountryIdFromSparqlEndpoint(countryName, languageCode);
			/// TODO km : think about what to do with multiple results
			if (null != geoNamesIds && 1 == geoNamesIds.size()) {
				geoNamesId = geoNamesIds.get(0);
			}
		}
		
		return geoNamesId;
	}
	
	public String getGeoNamesCountyId(final String countyName, final String geonamesCountryId, final String languageCode) throws IngestionException {
		if (null == countyName) {
			return null;
		}
		
		String geoNamesId = null;
		List<String> geoNamesIds = this.getGeoNamesCountyIdFromSparqlEndpoint(countyName, geonamesCountryId, languageCode);
		if (null != geoNamesIds && 1 == geoNamesIds.size()) {
			geoNamesId = geoNamesIds.get(0);
		}
		
		return geoNamesId;
	}
	
	public String getGeoNamesCityId(final String cityName, final String languageCode, final String countryId) throws IngestionException {
		String id = this.getGeoNamesCityIdFromSparqlEndpoint(cityName, languageCode, countryId);
		return id;
	}
	
	public String getGeoNamesCityId(final String cityName, final String languageCode) throws IngestionException {
		return this.getGeoNamesCityId(cityName, languageCode, (String) null);
	}

	public Pattern getPostCodeRegex(final String countryName) throws IngestionException {
		if (null == countryName) {
			return null;
		}

		String regex = this.csvReader.getEntityFieldValue("Country",
				countryName.trim().toLowerCase(), "Postal Code Regex");
		if (null == regex || regex.isEmpty()) {
			return null;
		}

		try {
			return Pattern.compile(regex);
		} catch (Exception e) {
			return null;
		}
	}
}
