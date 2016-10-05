package aksw.org.sdw.kg.datasets;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.solr.common.SolrDocument;

//import com.hp.hpl.jena.query.QueryExecution;
//import com.hp.hpl.jena.query.QueryExecutionFactory;
//import com.hp.hpl.jena.query.QuerySolution;
//import com.hp.hpl.jena.query.ResultSet;
//import com.hp.hpl.jena.rdf.model.RDFNode;

import aksw.org.sdw.kg.handler.solr.SolrUriInputDocument;

/**
 * This class can be used to load geonames ids
 * 
 * @author kay
 *
 */
public class GeonamesLoader implements Iterator<SolrUriInputDocument>, Closeable {
	
	protected int limit = 10000;
	
	protected int offset = 0;
	
	/** sparql endpoint url */
	final String sparqlEndpoint;
	
	final String graphName;
	
	ResultSet resultSetCountry;
	
	QueryExecution queryExecCountry;
	
	String previousCountryUri;
	
	/** this solr document has already all the country data */
	SolrDocument finishedSolrDocument;
	
	/** this solr document is used to collect the data for the current country */
	SolrDocument currentSolrDocument;
	
	GeonamesCountryIterator countryIterator;	
	
	public GeonamesLoader(final String sparqlEndpoint, final String graphName) {		
		this.sparqlEndpoint = sparqlEndpoint;
		this.graphName = graphName;
		
		this.countryIterator = new GeonamesCountryIterator(sparqlEndpoint, graphName, Arrays.asList("http://dbpedia.org/ontology/Country"));		
	}
	
	@Override
	public boolean hasNext() {
		
		if (false == this.countryIterator.hasNext()) {
			return false;
		}
		
		try {			
			SolrUriInputDocument countryDoc = this.countryIterator.next();			
			countryDoc.addFieldData("source", "http://sws.geonames.org");
	
			GeonamesCountyIterator countyIterator = new GeonamesCountyIterator(
					this.sparqlEndpoint, this.graphName,
					Arrays.asList("http://dbpedia.org/ontology/AdministrativeRegion"), countryDoc);
			
			while (null != countryDoc && countyIterator.hasNext()) {
				SolrUriInputDocument countyDoc = countyIterator.next();
				countyDoc.addFieldData("source", "http://sws.geonames.org");
				
				countryDoc.addChildDocument(countyDoc);
				
				GeonamesCityIterator cityIterator = new GeonamesCityIterator(
						this.sparqlEndpoint, this.graphName,
						Arrays.asList("http://dbpedia.org/ontology/City"), countyDoc);
				
				while (cityIterator.hasNext()) {
					SolrUriInputDocument cityDoc = cityIterator.next();
					cityDoc.addFieldData("source", "http://sws.geonames.org");
					
					countyDoc.addChildDocument(cityDoc);
				}
				
				cityIterator.close();				
			}
			
			countyIterator.close();
			
			return null != countryDoc;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public SolrUriInputDocument next() {
		//return this.finishedSolrDocument;
		return this.countryIterator.next();
	}

	@Override
	public void close() throws IOException {
		if (null != queryExecCountry) {
			this.queryExecCountry.close();
			this.queryExecCountry = null;
		}
	}
	
	static abstract class GeonamesEntityIterator implements Iterator<SolrUriInputDocument>, Closeable {
		
		protected int limit = 10000;
		
		protected int offset;
		
		/** sparql endpoint url */
		final String sparqlEndpoint;
		
		final String graphName;
		
		ResultSet resultSet;
		
		QueryExecution queryExec;
		
		String previousCountryUri;
		
		/** specifies whether geo information from an entity where stored */
		boolean storedGeoInformation = false;
		
		/** this solr document has already all the country data */
		SolrUriInputDocument finishedSolrDocument;
		
		/** this solr document is used to collect the data for the current country */
		SolrUriInputDocument currentSolrDocument;
		
		final List<String> types;
		
		final SolrUriInputDocument parentDoc;
		
		final String parentId;
		
		/** stores all the English label */
		final Set<String> labelsEn = new HashSet<>();
		
		/** stores all the German labels */
		final Set<String> labelsDe = new HashSet<>();
		
		
		public GeonamesEntityIterator(final String sparqlEndpoint, final String graphName,
									  final List<String> types, final SolrUriInputDocument parentDoc) {		
			this.sparqlEndpoint = sparqlEndpoint;
			this.graphName = graphName;
			this.types = types;
			this.parentDoc = parentDoc;
			this.parentId = (null == parentDoc) ? null : parentDoc.getUri();
		}
		
		protected boolean executeCountryQuery() {
			if (null == this.resultSet || (null != this.resultSet && false == this.resultSet.hasNext())) {
				String countryQuery = this.getQuery();
				System.out.println("Country Query: " + countryQuery);
				
				// close old results
				if (null != this.queryExec) {
					this.queryExec.close();
					this.queryExec = null;
				}
				
				this.queryExec = this.getQueryResult(countryQuery, this.graphName);
				if (null == this.queryExec) {
					return false;
				}
				
				this.resultSet = queryExec.execSelect();
				if (null == resultSet || false == resultSet.hasNext()) {
					return false;
				}
				
				// increment for the next round
				this.offset += this.limit;				
			}
			
			return true;
		}

		@Override
		public void close() throws IOException {
			if (null != queryExec) {
				this.queryExec.close();
				this.queryExec = null;
			}
			
		}

		@Override
		public boolean hasNext() {
			if (false == executeCountryQuery()) {
				return false;
			}
			
			String latitude = null;
			String longitude = null;
			
			boolean firstRound = null == this.previousCountryUri;
			boolean gotNewCountry = false;
			while (this.resultSet.hasNext()) {
				QuerySolution result = this.resultSet.next();
				RDFNode node = result.get("id");
				if (null == node) {
					return false;
				}
				
				String uriName = node.toString();
				if (null == this.previousCountryUri ||
					false == this.previousCountryUri.equals(uriName)) {
					this.previousCountryUri = uriName;
					
					this.finishedSolrDocument = this.currentSolrDocument;
					this.currentSolrDocument = new SolrUriInputDocument(uriName);
					
					this.currentSolrDocument.addFieldData("id", uriName);
					
					for (String type : this.types) {
						this.currentSolrDocument.addFieldData("type", type);
					}
					
					this.addCustomInformation(this.currentSolrDocument);
					
					if (false == firstRound) {
						gotNewCountry =  true;
					}
					
					firstRound = false;
					this.storedGeoInformation = false;
					this.labelsEn.clear();
					this.labelsDe.clear();
				}
				
				
				Iterator<String> variableNames = result.varNames();
				while (variableNames.hasNext()) {
					
					String variableName = variableNames.next();				
					if (variableName.startsWith("label")) {
						
						String label = result.get(variableName).asLiteral().getLexicalForm();
						String language = result.get(variableName).asLiteral().getLanguage();
						if ((null == language || language.isEmpty() || language.endsWith("en")) &&
							false == this.labelsEn.contains(label)) {
							this.currentSolrDocument.addFieldData("nameEn", label);
							this.labelsEn.add(label);
						} else if (null != language && language.endsWith("de") &&
								   false == this.labelsDe.contains(label)) {
							this.currentSolrDocument.addFieldData("nameDe", label);
							this.labelsDe.add(label);
						}
					} else if (false == this.storedGeoInformation && variableName.equals("lat")) {
						RDFNode latitudeNode = result.get("lat");
						if (null == latitudeNode) {
							continue;
						}
						
						if (latitudeNode.isLiteral()) {
							latitude = latitudeNode.asLiteral().toString();
						}
					} else if (false == this.storedGeoInformation && variableName.equals("long")) {
						RDFNode longitudeNode = result.get("long");
						if (null == longitudeNode) {
							continue;
						}
						
						if (longitudeNode.isLiteral()) {
							longitude = longitudeNode.asLiteral().toString();
						}
					}
					
					if (false == this.storedGeoInformation &&
						null != longitude && null != latitude) {
						this.storedGeoInformation = true;
						
						String latLong = latitude + "," + longitude;
						this.currentSolrDocument.addFieldData("locationLatLon", latLong);
						this.currentSolrDocument.addFieldData("locationRpt", latLong);
					}
				}
				
				// managed to find new country
				if (gotNewCountry) {
					return true;
				}
			}
			
			// checks whether we only did one iteration (e.g. only found one entity)
			if (null == this.finishedSolrDocument && null != this.currentSolrDocument) {
				this.finishedSolrDocument = this.currentSolrDocument;
				return true;
			} else {
				// execute again with new limit
				// in order to ensure that we really do not have any information left
				return hasNext();
			}
		}

		@Override
		public SolrUriInputDocument next() {
			return this.finishedSolrDocument;
		}
		
		protected QueryExecution getQueryResult(final String searchQuery, final String graphName) {
			
			QueryExecution queryResult = QueryExecutionFactory.sparqlService(
										this.sparqlEndpoint, searchQuery, graphName);

			return queryResult;
		}
		
		/**
		 * This method can be used to get the query of this class
		 * @return
		 */
		protected abstract String getQuery();
		
		/**
		 * This method can be used to add additional information to the solr uri doc
		 * @param solrDoc
		 */
		protected abstract void addCustomInformation(final SolrUriInputDocument solrDoc);
	}
	
	static class GeonamesCountryIterator extends GeonamesEntityIterator {

		public GeonamesCountryIterator(final String sparqlEndpoint, final String graphName, final List<String> types) {
			super(sparqlEndpoint, graphName, types, (SolrUriInputDocument) null);
		}

		@Override
		protected String getQuery() {
			StringBuilder builder = new StringBuilder();
			
			builder.append("SELECT DISTINCT * WHERE {\n");
			builder.append("{  SELECT DISTINCT * WHERE {\n");
			builder.append("		{ ?id <http://www.geonames.org/ontology#featureCode>");
			builder.append("              <http://www.geonames.org/ontology#A.PCLI> . \n}");
			builder.append("		OPTIONAL { ?id <http://www.geonames.org/ontology#officialName> ?label0 }\n");
			builder.append("		OPTIONAL { ?id <http://www.geonames.org/ontology#alternateName>  ?label1 }\n");
			builder.append("		OPTIONAL { ?id <http://www.geonames.org/ontology#name>  ?label2 }\n");
			builder.append("		OPTIONAL { ?id <http://www.w3.org/2003/01/geo/wgs84_pos#lat>  ?lat . }\n");
			builder.append("		OPTIONAL { ?id <http://www.w3.org/2003/01/geo/wgs84_pos#long>  ?long . }\n");
			builder.append("    } ORDER BY ASC(?id)");
			builder.append("}\n");
			builder.append("} LIMIT ").append(this.limit).append(" OFFSET ").append(this.offset);
			
			return builder.toString();
		}

		@Override
		protected void addCustomInformation(SolrUriInputDocument solrDoc) {
			// nothing to do here
		}
		
	}
	
	static class GeonamesCountyIterator extends GeonamesEntityIterator {
		
		public GeonamesCountyIterator(final String sparqlEndpoint, final String graphName,
									  final List<String> types, final SolrUriInputDocument parentDoc) {
			super(sparqlEndpoint, graphName, types, parentDoc);
		}

		@Override
		protected String getQuery() {
			StringBuilder builder = new StringBuilder();
			
			builder.append("SELECT DISTINCT * WHERE {\n");
			builder.append("{  SELECT DISTINCT * WHERE {\n");
			builder.append("		{ ?id <http://www.geonames.org/ontology#featureClass>");
			builder.append("              <http://www.geonames.org/ontology#A> .} \n");
			builder.append("		{ ?id <http://www.geonames.org/ontology#featureCode>");
			builder.append("              <http://www.geonames.org/ontology#A.ADM1> .} \n");
			if (null != this.parentId) {
				builder.append("		{ ?id <http://www.geonames.org/ontology#parentCountry>  <").append(this.parentId).append("> .}\n");
			}
		
			builder.append("		OPTIONAL { ?id <http://www.geonames.org/ontology#officialName> ?label0 . }\n");
			builder.append("		OPTIONAL { ?id <http://www.geonames.org/ontology#alternateName> ?label1 . }\n");
			builder.append("		OPTIONAL { ?id <http://www.w3.org/2003/01/geo/wgs84_pos#lat>  ?lat . }\n");
			builder.append("		OPTIONAL { ?id <http://www.w3.org/2003/01/geo/wgs84_pos#long>  ?long . }\n");
			builder.append("		OPTIONAL { ?id <http://www.geonames.org/ontology#name>  ?label2 . }\n");
			builder.append("    } ORDER BY ASC(?id)");
			builder.append("}\n");
			builder.append("} LIMIT ").append(this.limit).append(" OFFSET ").append(this.offset);
					
			return builder.toString();
		}

		@Override
		protected void addCustomInformation(SolrUriInputDocument solrDoc) {
			solrDoc.addFieldData("parentGeonamesId", this.parentId);			
		}
		
	}
	
	static class GeonamesCityIterator extends GeonamesEntityIterator {
		
		public GeonamesCityIterator(final String sparqlEndpoint, final String graphName,
									final List<String> types, final SolrUriInputDocument parentDoc) {
			super(sparqlEndpoint, graphName, types, parentDoc);
		}

		@Override
		protected String getQuery() {
			StringBuilder builder = new StringBuilder();
			
			builder.append("SELECT DISTINCT * WHERE {\n");
			builder.append("{  SELECT DISTINCT * WHERE {\n");
//			builder.append("		{ ?id <http://www.geonames.org/ontology#featureCode>\n");
//			builder.append("              <http://www.geonames.org/ontology#P.PPL> . }\n");
			builder.append("		{ ?id <http://www.geonames.org/ontology#featureClass>\n");
			builder.append("              <http://www.geonames.org/ontology#P> . }\n");
			builder.append("		{ ?id <http://www.geonames.org/ontology#officialName> ?label0 . } UNION \n");
			builder.append("		{ ?id <http://www.geonames.org/ontology#alternateName> ?label1 . } UNION \n");
			builder.append("		{ ?id <http://www.geonames.org/ontology#name> ?label2 . } \n");
			builder.append("		OPTIONAL { ?id <http://www.w3.org/2003/01/geo/wgs84_pos#lat>  ?lat . }\n");
			builder.append("		OPTIONAL { ?id <http://www.w3.org/2003/01/geo/wgs84_pos#long>  ?long . }\n");

			if (null != this.parentId) {
				builder.append("		{ ?id <http://www.geonames.org/ontology#parentADM1> <").append(this.parentId).append("> . } \n");
			}
			
			builder.append("    } ORDER BY ASC(?id)");
			builder.append("}\n");
			builder.append("} LIMIT ").append(this.limit).append(" OFFSET ").append(this.offset);
			
			return builder.toString();
		}
		
		@Override
		protected void addCustomInformation(SolrUriInputDocument solrDoc) {
			solrDoc.addFieldData("parentGeonamesId", this.parentId);			
		}
	}
}
