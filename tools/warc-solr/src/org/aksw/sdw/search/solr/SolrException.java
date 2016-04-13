package org.aksw.sdw.search.solr;

/**
 * Basic Exception for SOLR DataId related issues
 * 
 * @author mullekay
 *
 */
public class SolrException extends Exception {

	/**
	 * generated serial ID
	 */
	private static final long serialVersionUID = 1448001951030948224L;
	
	public SolrException(final String message) {
		super(message);
	}
	
	public SolrException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
