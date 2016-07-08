package sdw.aksw.org.solr;

import java.util.Collection;

/**
 * Basic interface for storing information for SOLR fields
 * 
 * @author kay
 *
 */
public interface SolrFieldValue<T> {
	
	public boolean isEmpty();
	
	public void addFieldValue(T ... values);
	
	public Collection<T> getFieldValues();

}
