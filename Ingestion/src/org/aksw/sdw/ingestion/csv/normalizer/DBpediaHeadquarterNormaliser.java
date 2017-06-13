package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.solr.common.util.Pair;

import aksw.org.kg.entity.Entity;
import aksw.org.sdw.kg.handler.sparql.SparqlHandler;

/**
 * This class can be used to add a headquarter location to this entity
 * 
 * @author kay
 *
 */
public class DBpediaHeadquarterNormaliser implements PropertyNormalizer {
	
	protected final SparqlHandler sparqlHandler;
	
	/** reference to current result */
	ResultSet currentResult;
	
	/** reference to currentQueryExecution Unit */
	QueryExecution currentQueryExecution;
	
	String currentUri;
	
	public DBpediaHeadquarterNormaliser() {
		/// TODO get config
		this.sparqlHandler = new SparqlHandler("http://localhost:9890/sparql", "http://dbpedia_new.org");
	}

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		
		List<RDFNode> headQuarterLocations = entity.getRdfObjects("http://dbpedia.org/ontology/headquarter");
		if (null == headQuarterLocations || headQuarterLocations.isEmpty()) {
			return;
		}
		
		String countryUri = null;
		String regionUri = null;
		String cityUri = null;
		Map<String, String> uriToGeonamesMap = new HashMap<>();
		
		Set<String> otherUris = new HashSet<>();
		Map<String, Pair<String, String>> coordinateMap = new HashMap<>();
		
		for (RDFNode headQuarterLocation : headQuarterLocations) {
			String headQuarterUri = headQuarterLocation.asResource().getURI();
			if (false == headQuarterUri.startsWith("http")) {
				continue; // filter out invalid URIs
			}

			AtomicInteger offset = new AtomicInteger(0);
			AtomicInteger limit = new AtomicInteger(10_000);
			if (null == currentUri || false == currentUri.equals(headQuarterUri)) {
				this.currentUri = headQuarterUri;
			}			

			while (this.hasNextResult(limit, offset)) {
				QuerySolution resultTriples = this.currentResult.next();
				
				RDFNode object = resultTriples.get("type");
				
				String objectString = object.toString();
				
				if (null == cityUri && objectString.equals("http://dbpedia.org/ontology/Settlement") || objectString.equals("http://dbpedia.org/ontology/City")) {
					System.out.println(headQuarterUri + " is city!");
					cityUri = headQuarterUri;
					
					RDFNode sameAsObject = resultTriples.get("sameAs");				
					if (null != sameAsObject && sameAsObject.toString().startsWith("http://sws.geonames.org")) {						
						uriToGeonamesMap.put(headQuarterUri, sameAsObject.toString());
					}
					
				} else if (null == regionUri && objectString.equals("http://dbpedia.org/ontology/Region")) {
					System.out.println(headQuarterUri + " is region!");
					regionUri = headQuarterUri;
					
					RDFNode sameAsObject = resultTriples.get("sameAs");				
					if (null != sameAsObject && sameAsObject.toString().startsWith("http://sws.geonames.org")) {						
						uriToGeonamesMap.put(headQuarterUri, sameAsObject.toString());
					}
				} else if (null == countryUri && objectString.equals("http://dbpedia.org/ontology/Country")) {
					System.out.println(headQuarterUri + " is country!");
					countryUri = headQuarterUri;
					
					RDFNode sameAsObject = resultTriples.get("sameAs");				
					if (null != sameAsObject && sameAsObject.toString().startsWith("http://sws.geonames.org")) {						
						uriToGeonamesMap.put(headQuarterUri, sameAsObject.toString());
					}
					
					RDFNode latitudeNode = resultTriples.get("lat");
					RDFNode longitudeNode = resultTriples.get("long");
					if (null != latitudeNode && null != longitudeNode) {
						String cityLat = latitudeNode.asLiteral().getLexicalForm();
						String cityLong = longitudeNode.asLiteral().getLexicalForm();
						coordinateMap.put(headQuarterUri, new Pair<String, String>(cityLat, cityLong));
						
					}
				} else {
					otherUris.add(headQuarterUri);
					
					RDFNode sameAsObject = resultTriples.get("sameAs");				
					if (null != sameAsObject && sameAsObject.toString().startsWith("http://sws.geonames.org")) {						
						uriToGeonamesMap.put(headQuarterUri, sameAsObject.toString());
					}
					
					RDFNode latitudeNode = resultTriples.get("lat");
					RDFNode longitudeNode = resultTriples.get("long");
					if (null != latitudeNode && null != longitudeNode) {						
						String latitude = latitudeNode.asLiteral().getLexicalForm();
						String longitude = longitudeNode.asLiteral().getLexicalForm();
						coordinateMap.put(headQuarterUri, new Pair<String, String>(latitude, longitude));
						
					}
				}				
			}
		}
		
		GeoNamesMapper geonamesMapper = PropertyNormalizerUtils.getInstance().getGeoNamesMapper();
		
		Entity headerQuarterEntity = new Entity(false);
		String headQuarterUri = entity.getSubjectUri() + "_headerquarterSite";
		headerQuarterEntity.setSubjectUri(headQuarterUri);
		
		if (null != countryUri) {
			otherUris.remove(countryUri);

			String geonamesIdCountry = uriToGeonamesMap.get(countryUri);
			if (null != geonamesIdCountry) {
				ResourceImpl geonamesCountryObject = new ResourceImpl(geonamesIdCountry);
				headerQuarterEntity.addTriple(CorpDbpedia.countryGeoNamesId, geonamesCountryObject);
				
				String nameEnglish = geonamesMapper.getNameFromId(geonamesIdCountry, "en");
				if (null != nameEnglish) {
					headerQuarterEntity.addTripleWithLiteral(CorpDbpedia.countryName, nameEnglish, "en");
				}
				
				String nameGerman = geonamesMapper.getNameFromId(geonamesIdCountry, "de");
				if (null != nameGerman) {
					headerQuarterEntity.addTripleWithLiteral(CorpDbpedia.countryName, nameGerman, "de");
				}
			}
		}
		
		if (null != regionUri) {
			otherUris.remove(regionUri);

			String geonamesIdCounty = uriToGeonamesMap.get(regionUri);
			if (null != geonamesIdCounty) {
				ResourceImpl geonamesCountryObject = new ResourceImpl(geonamesIdCounty);
				headerQuarterEntity.addTriple(CorpDbpedia.countyGeonamesId, geonamesCountryObject);
				
				String nameEnglish = geonamesMapper.getNameFromId(geonamesIdCounty, "en");
				if (null != nameEnglish) {
					headerQuarterEntity.addTripleWithLiteral(CorpDbpedia.countyName, nameEnglish, "en");
				}
				
				String nameGerman = geonamesMapper.getNameFromId(geonamesIdCounty, "de");
				if (null != nameGerman) {
					headerQuarterEntity.addTripleWithLiteral(CorpDbpedia.countyName, nameGerman, "de");
				}
			}
			
			
		}
		
		if (null != cityUri) {
			otherUris.remove(cityUri);

			String geonamesIdCity = uriToGeonamesMap.get(cityUri);
			if (null != geonamesIdCity) {
				ResourceImpl geonamesCountryObject = new ResourceImpl(geonamesIdCity);
				headerQuarterEntity.addTriple(CorpDbpedia.cityGeonamesId, geonamesCountryObject);
				
				String nameEnglish = geonamesMapper.getNameFromId(geonamesIdCity, "en");
				if (null != nameEnglish) {
					headerQuarterEntity.addTripleWithLiteral(CorpDbpedia.cityName, nameEnglish, "en");
				}
				
				String nameGerman = geonamesMapper.getNameFromId(geonamesIdCity, "de");
				if (null != nameGerman) {
					headerQuarterEntity.addTripleWithLiteral(CorpDbpedia.cityName, nameGerman, "de");
				}
				
				Pair<String, String> coordinatePair = coordinateMap.get(cityUri);
				if (null != coordinatePair) {
					String cityLat = coordinatePair.getKey();
					String cityLong = coordinatePair.getValue();
					GeoCordinatesNormalizer.addPoint(cityLong, cityLat, headerQuarterEntity);
				}
			}
		}
		
		// check whether it is possible to
		Pair<String, String> parentData = null;
		if (null == countryUri || null == uriToGeonamesMap.get(countryUri) ||
			null != regionUri || null == uriToGeonamesMap.get(regionUri)) {
			
			String geonamesIdCity = null == cityUri ? null : uriToGeonamesMap.get(cityUri);
			parentData = geonamesMapper.getParentCountyAndCountry(geonamesIdCity);
			if (null == parentData) {
				String geonamesIdRegion = null == regionUri ? null : uriToGeonamesMap.get(regionUri);
				parentData = geonamesMapper.getParentCountyAndCountry(geonamesIdRegion);
			}
			
			if (null != parentData) {
				String regionGeoNamesId = parentData.getKey();
				if (false == regionGeoNamesId.equals(regionUri)) {
	
					ResourceImpl geonamesCountryObject = new ResourceImpl(regionGeoNamesId);
					headerQuarterEntity.addTriple(CorpDbpedia.countyGeonamesId, geonamesCountryObject);
					
					String nameEnglish = geonamesMapper.getNameFromId(regionGeoNamesId, "en");
					if (null != nameEnglish) {
						headerQuarterEntity.addTripleWithLiteral(CorpDbpedia.countyName, nameEnglish, "en");
					}
					
					String nameGerman = geonamesMapper.getNameFromId(regionGeoNamesId, "de");
					if (null != nameGerman) {
						headerQuarterEntity.addTripleWithLiteral(CorpDbpedia.countyName, nameGerman, "de");
					}
				}
				
				String geonamesIdCountry = parentData.getValue();
				if (false == geonamesIdCountry.equals(countryUri)) {
					ResourceImpl geonamesCountryObject = new ResourceImpl(geonamesIdCountry);
					headerQuarterEntity.addTriple(CorpDbpedia.countryGeoNamesId, geonamesCountryObject);
						
					String nameEnglish = geonamesMapper.getNameFromId(geonamesIdCountry, "en");
					if (null != nameEnglish) {
						headerQuarterEntity.addTripleWithLiteral(CorpDbpedia.countryName, nameEnglish, "en");
					}
						
					String nameGerman = geonamesMapper.getNameFromId(geonamesIdCountry, "de");
					if (null != nameGerman) {
						headerQuarterEntity.addTripleWithLiteral(CorpDbpedia.countryName, nameGerman, "de");
					}
				}
			}
		}
		
		if (false == headerQuarterEntity.isEmpty()) {
			entity.addSubEntity(headerQuarterEntity);
			
			entity.addTriple(CorpDbpedia.hasHeadquarterSite, headerQuarterEntity.getSubjectUriObject());
			entity.addTriple(W3COrg.hasSite, headerQuarterEntity.getSubjectUriObject());
		}
		
		entity.deleteProperty("http://dbpedia.org/ontology/headquarter");
		
		this.currentQueryExecution.close();
		
	}
	
	protected String getHeadQuarterQueryString(final String headQuarterUri, final AtomicInteger limit, final AtomicInteger offset) {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("SELECT DISTINCT * WHERE { ");
		buffer.append("<").append(headQuarterUri).append("> <").append(RDF.type.toString()).append("> ?type . ");
		buffer.append("OPTIONAL {<").append(headQuarterUri).append("> <").append(OWL.sameAs.toString()).append("> ?sameAs . ");
		buffer.append(" FILTER ( CONTAINS(STR(?sameAs), \"sws.geonames.org\") ) } ");
		buffer.append("OPTIONAL {<").append(headQuarterUri).append("> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat . } ");
		buffer.append("OPTIONAL {<").append(headQuarterUri).append("> <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?long . } ");		
		buffer.append("} LIMIT ").append(limit).append(" OFFSET ").append(offset);
		
		return buffer.toString();
	}
	
	protected boolean hasNextResult(final AtomicInteger limit, final AtomicInteger offset) {
		if (null == this.currentResult) {	
			String query = this.getHeadQuarterQueryString(this.currentUri, limit, offset);
			this.currentQueryExecution = this.sparqlHandler.createQueryExecuter(query);
			
			this.currentResult = this.currentQueryExecution.execSelect();
			if (null == this.currentResult || false == this.currentResult.hasNext()) {
				this.currentQueryExecution.close();
				return false;
			}
			
			offset.addAndGet(limit.get());
		}
		
		if (this.currentQueryExecution.isClosed() || false == this.currentResult.hasNext()) {
			String query = this.getHeadQuarterQueryString(this.currentUri, limit, offset);
			this.currentQueryExecution = this.sparqlHandler.createQueryExecuter(query);
			
			this.currentResult = this.currentQueryExecution.execSelect();
			if (null == this.currentResult || false == this.currentResult.hasNext()) {
				this.currentQueryExecution.close();
				return false;
			}
			
			offset.addAndGet(limit.get());
		}
		
		return true;
	}
}
