package aksw.org.sdw.kg.datasets;

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

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.Logger;

//import com.google.common.cache.Cache;
//import com.google.common.cache.CacheBuilder;
//import com.google.common.cache.CacheLoader;
//import com.hp.hpl.jena.query.QueryExecution;
//import com.hp.hpl.jena.query.QueryExecutionFactory;
//import com.hp.hpl.jena.query.QuerySolution;
//import com.hp.hpl.jena.query.ResultSet;
//import com.hp.hpl.jena.rdf.model.Literal;
//import com.hp.hpl.jena.rdf.model.Resource;

import aksw.org.kg.KgException;
import aksw.org.kg.input.CsvReader;
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
	public GeoNamesMapper(final String fileName) throws KgException {

		URL url = GeoNamesMapper.class.getClassLoader().getResource(fileName);
		if (null == url) {
			throw new KgException("Was not able to find file: " + fileName);
		}
		
		Map<String, Map<String, List<String>>> synonymMap = new HashMap<>();
		synonymMap.put("Country", GeoNamesMapper.countryNameSynonyms);
		
		this.csvReader = new CsvReader("countryCodeInfo.tsv", "\t", Arrays.asList("Country","geonameid"), synonymMap);
		
		this.sparqlEndpoint = "http://localhost:9890/sparql";
		this.graphName = "http://geonames.org";
	}	
	
	public Map<String, List<String>> getGeoNamesSynonyms() {
		return GeoNamesMapper.countryNameSynonyms;
	}
	
	protected String getGeoNamesIdFromInfoFile(final String countryName) throws KgException {
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
	
	protected String getCountryName2GeoNamesCountryIdQuery(final String countryName, final String languageCode) {
		StringBuilder builder = new StringBuilder();
		
//		builder.append("SELECT DISTINCT * WHERE {" +
//					   "		{ ?id <http://www.geonames.org/ontology#featureClass>" +
//					   "              <http://www.geonames.org/ontology#A> . \n}" +
//					   "		{ ?id <http://www.geonames.org/ontology#officialName> \"" + countryName + "\"@" + languageCode + " } UNION\n" +
//					   "		{ ?id <http://www.geonames.org/ontology#officialName> \"" + countryName + "\". } UNION\n" +
//					   "		{ ?id <http://www.geonames.org/ontology#alternateName> \"" + countryName + "\"@" + languageCode + " } UNION\n" +
//					   "		{ ?id <http://www.geonames.org/ontology#alternateName> \"" + countryName + "\". } UNION\n" +
//					   "		{ ?id <http://www.geonames.org/ontology#name> \"" + countryName + "\". } UNION\n" +
//					   "		{ ?id <http://www.geonames.org/ontology#name> \"" + countryName + "\"@" + languageCode + " } . } LIMIT 10000");
		
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
		
//		builder.append("SELECT DISTINCT * WHERE {" +
//					   "		{ ?id <http://www.geonames.org/ontology#featureClass>" +
//					   "              <http://www.geonames.org/ontology#A> .} \n" +
//					   "		{ ?id <http://www.geonames.org/ontology#featureCode>" +
//					   "              <http://www.geonames.org/ontology#A.ADM1> .} \n");
//		if (null != geonamesCountryId) {
//			builder.append("		{ ?id <http://www.geonames.org/ontology#parentCountry>  <" + geonamesCountryId + "> .}\n");
//		}
//		
//		builder.append("		{ ?id <http://www.geonames.org/ontology#officialName> \"" + countyName + "\"@" + languageCode + ". } UNION\n" +
//					   "		{ ?id <http://www.geonames.org/ontology#officialName> \"" + countyName + "\". } UNION\n" +
//					   "		{ ?id <http://www.geonames.org/ontology#alternateName> \"" + countyName + "\"@" + languageCode + " } UNION\n" +
//					   "		{ ?id <http://www.geonames.org/ontology#alternateName> \"" + countyName + "\". } UNION\n" +
//					   "		{ ?id <http://www.geonames.org/ontology#name> \"" + countyName + "\". } UNION\n" +
//					   "		{ ?id <http://www.geonames.org/ontology#name> \"" + countyName + "\"@" + languageCode + " } . } LIMIT 1000");
		
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
		
//		builder.append("SELECT DISTINCT * WHERE {\n" +
//					   "{ <" + geonamesId + "> <http://www.geonames.org/ontology#officialName> ?officialCountryName . } UNION\n" +
//					   "{ <" + geonamesId + "> <http://www.geonames.org/ontology#alternateName> ?alternateCountryName . } UNION\n" +
//					   "{ <" + geonamesId + "> <http://www.geonames.org/ontology#name> ?countryName . }\n" +
//					   " } LIMIT 10000");
		
		builder.append("SELECT DISTINCT * WHERE {\n");
		builder.append("{ <").append(geonamesId).append("> <http://www.geonames.org/ontology#officialName> ?officialCountryName . } UNION\n");
		builder.append("{ <").append(geonamesId).append("> <http://www.geonames.org/ontology#alternateName> ?alternateCountryName . } UNION\n");
		builder.append("{ <").append(geonamesId).append("> <http://www.geonames.org/ontology#name> ?countryName . }\n");
		builder.append(" } LIMIT 10000");
		
		return builder.toString();
	}
	
	protected String getName2GeoNamesCityIdQuery(final String cityName, final String parentCountryId, final String languageCode) {
		StringBuilder builder = new StringBuilder();

//		builder.append("SELECT DISTINCT * WHERE {\n" +
//				   "		{ ?id <http://www.geonames.org/ontology#featureClass>\n" +
//				   "              <http://www.geonames.org/ontology#P> . }\n" +
//				   "		{ ?id <http://www.geonames.org/ontology#officialName> \"" + cityName + "\"@" + languageCode + " . } UNION \n" +
//				   "		{ ?id <http://www.geonames.org/ontology#alternateName> \"" + cityName + "\"@" + languageCode + " . } UNION \n" +
//				   "		{ ?id <http://www.geonames.org/ontology#name> \"" + cityName + "\"@" + languageCode + " . } UNION \n" +
//				   "		{ ?id <http://www.geonames.org/ontology#officialName> \"" + cityName + "\" . } UNION \n" +
//				   "		{ ?id <http://www.geonames.org/ontology#alternateName> \"" + cityName + "\" . } UNION \n" +
//				   "		{ ?id <http://www.geonames.org/ontology#name> \"" + cityName + "\" . } \n");
//		
//		if (null != parentCountryId) {
//			builder.append("		{ ?id <http://www.geonames.org/ontology#parentCountry> <" + parentCountryId + "> . } \n");
//		}
		
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
	 * @throws KgException
	 */
	@SuppressWarnings("unchecked")
	protected List<String> getGeoNamesCountryIdFromSparqlEndpoint(final String countryName, final String languageCode) throws KgException {
		if (null == countryName) {
			return null;
		}
		
		if (null == this.sparqlEndpoint || null == this.graphName) {
			return null;
		}
		
		final String queryString = this.getCountryName2GeoNamesCountryIdQuery(countryName, languageCode);
		
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
			throw new KgException("Was not able to load country data", e);
		}
	}
	
	/**
	 * Get geonames IDs for counties which are in a particular country
	 * 
	 * @param countyName
	 * @param geonamesCountryId
	 * @param languageCode
	 * @return
	 * @throws KgException
	 */
	@SuppressWarnings("unchecked")
	protected List<String> getGeoNamesCountyIdFromSparqlEndpoint(final String countyName, final String geonamesCountryId, final String languageCode) throws KgException {
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
			throw new KgException("Was not able to load country data", e);
		}
	}
	
	protected String getGeoNamesCityIdFromSparqlEndpoint(final String cityName, final String languageCode, final String countryId) throws KgException {
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
			throw new KgException("Was not able to load city data", e);
		}
	}
	
	protected String getCountryCodeFromId(final String geonamesUri) throws KgException {
		
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
			throw new KgException("Was not able to get country code", e);
		}
	}
	
	protected String getInstanceNameFromId(final String geonamesUri, final String langId) throws KgException {
		
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
			throw new KgException("Was not able to get country code for query: " + queryString, e);
		}
	}
	
	public String getCountryCode(final String geonamesUri) throws KgException {
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
	
	public String getNameFromId(final String geonameUri, final String languageCode) throws KgException {
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
	
	public String getGeoNamesCountryId(final String countryName, final String languageCode) throws KgException {
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
	
	public String getGeoNamesCountyId(final String countyName, final String geonamesCountryId, final String languageCode) throws KgException {
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
	
	public String getGeoNamesCityId(final String cityName, final String languageCode, final String countryId) throws KgException {
		String id = this.getGeoNamesCityIdFromSparqlEndpoint(cityName, languageCode, countryId);
		return id;
	}
	
	public String getGeoNamesCityId(final String cityName, final String languageCode) throws KgException {
		return this.getGeoNamesCityId(cityName, languageCode, (String) null);
	}

	public Pattern getPostCodeRegex(final String countryName) throws KgException {
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
