package aksw.org.sdw.kg.handler.solr;

import aksw.org.kg.KgException;

/**
 * General KG SOLR Exception
 * 
 * @author kay
 *
 */
public class KgSolrException extends KgException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5889244123317334649L;
	
	public KgSolrException(final String message) {
		super(message);
	}
	
	public KgSolrException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
