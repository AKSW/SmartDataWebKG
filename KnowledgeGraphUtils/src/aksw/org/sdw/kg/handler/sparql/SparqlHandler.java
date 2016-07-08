package aksw.org.sdw.kg.handler.sparql;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;

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
		System.out.println("Query:\n\n" + queryString);
		
		QueryExecution queryExecution =  QueryExecutionFactory.sparqlService(
				this.sparqlEndpointUrl, queryString, graphName);
		
		return queryExecution;
	}
}
