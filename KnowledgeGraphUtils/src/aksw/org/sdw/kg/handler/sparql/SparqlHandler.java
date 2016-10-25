package aksw.org.sdw.kg.handler.sparql;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;

//import com.hp.hpl.jena.query.QueryExecution;
//import com.hp.hpl.jena.query.QueryExecutionFactory;

/**
 * This class can be used to access a SPARQL backend.
 * 
 * @author kay
 *
 */
public class SparqlHandler {
	
	/** url to the sparql endpoint */
	final String sparqlEndpointUrl;
	
	/** graph name */
	final String graphName;
	
	public SparqlHandler(final String sparqlEndpointUrl, final String graphName) {
		this.sparqlEndpointUrl = sparqlEndpointUrl;
		this.graphName = graphName;		
	}
	
	public QueryExecution createQueryExecuter(final String queryString) {
		System.out.println("!Query:\n\n" + queryString);
		
		QueryExecution queryExecution =  QueryExecutionFactory.sparqlService(
				this.sparqlEndpointUrl, queryString);
		
		// is required, since we have invalid URIs in DBpedia at the moment
		// (e.g. "http://de.dbpedia.org/resource/Adm_–_Agentur_für_Dialogmarketing")
		//
		// Due to the different serialization in JSON, it works fine
		if (queryExecution instanceof QueryEngineHTTP) {
			((QueryEngineHTTP) queryExecution).setModelContentType("application/ld+json");
		}
		
		return queryExecution;
	}
}
