package aksw.org.kg;

/**
 * General KG SOLR Exception
 * 
 * @author kay
 *
 */
public class KgException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5889244123317334649L;
	
	public KgException(final String message) {
		super(message);
	}
	
	public KgException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
