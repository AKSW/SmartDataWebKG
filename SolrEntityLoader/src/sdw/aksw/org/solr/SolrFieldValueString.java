package sdw.aksw.org.solr;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SolrFieldValueString implements SolrFieldValue<String> {
	
	Set<String> fieldValues = new HashSet<String>();

	@Override
	public void addFieldValue(String... values) {
		if (null == values) {
			return;
		}
		
		for (String value : values) {
			fieldValues.add(value);
		}
		
	}

	@Override
	public Collection<String> getFieldValues() {
		return Collections.unmodifiableSet(this.fieldValues);
	}

	@Override
	public boolean isEmpty() {
		return this.fieldValues.isEmpty();
	}

}
