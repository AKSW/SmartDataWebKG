package aksw.org.kg.entity;

/**
 * This class can be used to store RDF Object information
 * 
 * @author kay
 *
 */
public class RdfObjectUri extends RdfObject {
	
	/** specifies whether an object which refers to a UI with prefix (e.g. blank node) */
	protected final boolean isPrefixUri;
	
	/**
	 * 
	 * @param objectString	- uri
	 * @param isPrefixUri 	- specifies whether an object which refers
	 * 						to a UI with prefix (e.g. blank node)
	 */
	public RdfObjectUri(final String objectString) {
		super(objectString);
		
		this.isPrefixUri = (null == objectString) ? false :
						   false == objectString.startsWith("http") &&
						   objectString.contains(":");
	}
	
	@Override
	protected void createString() {
		StringBuilder builder = new StringBuilder();
		
		if (this.isPrefixUri) {
			builder.append(" ");
			builder.append(this.objectString);
			builder.append(" ");
		} else {
			builder.append(" <");
			builder.append(this.objectString);
			builder.append("> ");
		}
		
		super.fullObjectString = builder.toString();
	}
}
