package sdw.aksw.org.sparql;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import sdw.aksw.org.config.KgSolrConfig.KgSolrMapping;

/**
 * Default mapping class
 * 
 * @author kay
 *
 */
public class DefaultMapping implements Solr2SparqlMappingInterface {
	
	@Override
	public void fillFieldDataMap(final QuerySolution querySolution, final KgSolrMapping mapping,
								 final String matchingVarName, final String solrFieldName,
								 final Map<String, Set<String>> fieldDataMap) {
		
		
		RDFNode node = querySolution.get(matchingVarName);
		if (null == node) {
			return;
		}
			
		Set<String> fieldData = fieldDataMap.get(solrFieldName);
		if (null == fieldData) {
			fieldData = new HashSet<>();
			fieldDataMap.put(solrFieldName, fieldData);
		}
			
		if (node.isLiteral()) {
			Literal literal = node.asLiteral();
				
			fieldData.add(literal.getLexicalForm());

		} else if (node.isResource()) {
			Resource resource = node.asResource();
			
			fieldData.add(resource.toString());
		}
	}	
}
