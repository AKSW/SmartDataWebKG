package aksw.org.kg.handler.solr.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;

import aksw.org.kg.KgException;
import aksw.org.sdw.kg.handler.solr.KgSolrResultDocument;
import aksw.org.sdw.kg.handler.solr.SolrHandler;
import aksw.org.sdw.kg.handler.sparql.SparqlHandler;

/**
 * This class can be used to extract entity information from SDW KG backend.
 * 
 * @author kay
 *
 */
public class EntityExtractor {
	
	private static String solrUrl = "http://localhost:8983/solr/companies";

	public static void main(String[] args) throws IOException, KgException {
		System.out.println("Entity Extractor");		
		
		
		String solrUrl = args[0];
		System.out.println("Apache Solr instance: " + solrUrl);
		
		String solrQuery = args[1];
		System.out.println("Solr Query: " + solrQuery);
		
		String sparqlBackend = args[2];
		System.out.println("SPARQL Backend: " + sparqlBackend);
		
		SolrHandler solrHandler = new SolrHandler(solrUrl);
		
		SparqlHandler sparqlHandler = new SparqlHandler(sparqlBackend, null);
		
		try {
			List<KgSolrResultDocument> searchResults = solrHandler.executeQuery(solrQuery, null);
			System.out.println("Number of results: " + searchResults.size());
			
			// will store the SPARQL search query
			StringBuffer query = new StringBuffer();
			
			query.append("SELECT DISTINCT * WHERE { ");
			
			int index = 0;
			Iterator<KgSolrResultDocument> iterator = searchResults.iterator();			
			while (iterator.hasNext()) {
				KgSolrResultDocument result = iterator.next();
				System.out.println(++index + ". Doc: \n" + result);
				
				query.append("\n\tOPTIONAL { <" + result.getFieldValueAsString("id") + "> <http://xmlns.com/foaf/0.1/homepage> ?homepage" + index + " }");
				
				List<String> sameAsLinks = result.getFieldValueAsStringList("sameAs");
				if (null != sameAsLinks &&  false == sameAsLinks.isEmpty()) {									
					for (String sameAsLink : sameAsLinks) {
						// only look within our own dataset
						if (sameAsLink.startsWith("http://corp.dbpedia.org")) {
							query.append("\n\tOPTIONAL { <" + sameAsLink + "> <http://xmlns.com/foaf/0.1/homepage> ?homepage" + index + " }");
						}
					}
				}
			}
			
			query.append("\n}");
			
			
			QueryExecution queryExec = sparqlHandler.createQueryExecuter(query.toString());
			
			ResultSet sparqlResult = queryExec.execSelect();
			System.out.println("Got results: " + sparqlResult.getRowNumber());
			
			queryExec.close();
		} finally {
			solrHandler.close();
		}
	}
}
