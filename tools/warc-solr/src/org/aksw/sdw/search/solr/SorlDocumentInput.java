package org.aksw.sdw.search.solr;

import org.apache.solr.common.SolrInputDocument;

/**
 * This is a general information for solr documents
 * 
 * @author kay
 *
 */
public interface SorlDocumentInput {
	
	/**
	 * This method can be used to obtain the generated SOLR input document
	 * 
	 * @return solr input document which contains the provided information
	 */
	public SolrInputDocument getSolrInputDocument();

}

