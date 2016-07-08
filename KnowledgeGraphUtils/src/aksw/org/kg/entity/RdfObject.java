package aksw.org.kg.entity;

/**
 * This class can be used to store RDF Object information
 * 
 * @author kay
 *
 */
public abstract class RdfObject {
	
	/** string of triple object */
	String objectString;
	
	/** full triple object string which can be added to output */
	String fullObjectString = null;
	
	public RdfObject(final String objectString) {
		this.objectString = objectString;
	}
	
	public String getObjectString() {
		return this.objectString;
	}
	
	/**
	 * This method can be used to create an object output string
	 */
	abstract protected void createString();
	
	/**
	 * This method can be used to change the object string
	 * 
	 * @param newObjectString
	 */
	public void updateObjectString(final String newObjectString) {
		if (null == newObjectString) {
			return;
		}
		
		this.objectString = newObjectString;
	}
	
	
	@Override
	public String toString() {
		if (null == this.fullObjectString) {
			this.createString();
		}
		
		return this.fullObjectString;		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((objectString == null) ? 0 : objectString.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RdfObject other = (RdfObject) obj;
		if (objectString == null) {
			if (other.objectString != null)
				return false;
		} else if (!objectString.equals(other.objectString))
			return false;
		return true;
	}
}
