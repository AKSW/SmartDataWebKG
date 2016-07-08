package sdw.aksw.org.dbpedia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * This class can be used in conjunction with DBpedia
 * 
 * @author kay
 *
 */
public class DbpediaUtils {
	
	final String sparqlEndpoint;
	
	final String graphName;
	
	public DbpediaUtils(final String sparqlEndpoint, final String graphName) {
		this.sparqlEndpoint = sparqlEndpoint;
		this.graphName = graphName;
	}
	
	/**
	 * This method can be used to obtain owl:sameAs Links
	 * from DBpedia
	 * 
	 * @param sourceUri	- source URI which should be matched
	 * @return
	 */
	public List<String> getSameAsUris(final String sourceUri) {
		if (null == sourceUri) {
			return Collections.emptyList(); 
		}
		
		List<String> sameAsLinks = new ArrayList<>();
		
		// create simple owl:sameAs query
		StringBuffer queryBuffer = new StringBuffer();		
		queryBuffer.append("SELECT * WHERE { ?id <" + OWL.sameAs.getURI() + "> <" + sourceUri + "> . }");
		
		
		QueryExecution queryResult = QueryExecutionFactory.sparqlService(
				this.sparqlEndpoint, queryBuffer.toString(), this.graphName);
		if (null == queryResult) {
			return Collections.emptyList();
		}
		
		ResultSet resultSet = queryResult.execSelect();
		while (resultSet.hasNext()) {
			QuerySolution result = resultSet.next();
			
			RDFNode node = result.get("id");
			String uri = node.toString();
			
			sameAsLinks.add(uri);
		}
		
		return sameAsLinks;
	}

}
