package sdw.aksw.org.sparql;

import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.query.QuerySolution;

import sdw.aksw.org.config.KgSolrConfig.KgSolrMapping;

/**
 * This interface can be used to create a mapping class which can convert RDF
 * data into SOLR compatible data
 * 
 * @author kay
 *
 */
public interface Solr2SparqlMappingInterface {

	/**
	 * This method can be used to fill out the fieldDataMap which is used to
	 * create a SOLR document with Entity information
	 * 
	 * @param querySolution
	 *            - query result from SOLR
	 * @param mapping
	 *            - actual mapping
	 * @param matchingVarName
	 *            - matching variable name
	 * @param fieldDataMap
	 *            - field data map which is going to be filled
	 */
	public void fillFieldDataMap(final QuerySolution querySolution, final KgSolrMapping mapping,
			final String matchingVarName, final String solrFieldName, final Map<String, Set<String>> fieldDataMap);
}
