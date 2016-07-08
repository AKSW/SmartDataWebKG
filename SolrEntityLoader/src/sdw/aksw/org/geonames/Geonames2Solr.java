package sdw.aksw.org.geonames;

import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

import aksw.org.sdw.kg.handler.solr.SolrHandler;
import sdw.aksw.org.dbpedia.DbpediaUtils;

/**
 * This is a base class for loading Geonames content from a SPARQL backend
 * and storing it in SOLR
 * 
 * @author kay
 *
 */
public class Geonames2Solr {
	
	final DbpediaUtils dbpediaUtils;
	
	final SolrHandler solrHandler;
	
	public Geonames2Solr(final SolrHandler solrHandler, final DbpediaUtils dbpediaUtils) {
		this.solrHandler = solrHandler;	
		this.dbpediaUtils = dbpediaUtils;
	}
	
	public void getDbpediaSameAs(SolrInputDocument solrInputDocument) {
		if (null == solrInputDocument || false == solrInputDocument.hasChildDocuments()) {
			return;
		}
		
		// find sameAs relationships
		SolrInputField id = solrInputDocument.get("id");		
		List<String> links = dbpediaUtils.getSameAsUris((String) id.getValue());
		if (null != links) {
			for (String link : links) {
				solrInputDocument.addField("sameAs", link);
			}
		}	
		
		// check for children as well
		for (SolrInputDocument childDoc : solrInputDocument.getChildDocuments()) {
			this.getDbpediaSameAs(childDoc);
		}
	}
}
