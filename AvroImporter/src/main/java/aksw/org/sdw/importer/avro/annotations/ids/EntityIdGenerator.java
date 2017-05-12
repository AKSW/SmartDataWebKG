package aksw.org.sdw.importer.avro.annotations.ids;

import java.util.HashMap;
import java.util.Map;

import aksw.org.sdw.importer.avro.annotations.Annotation;
import aksw.org.sdw.importer.avro.annotations.Span;

public class EntityIdGenerator extends UniqueIdGenerator {
	
	/** store IDs for spans */
	Map<Span, String> spanIdMap = new HashMap<>();
	
	/** namespace of URI */
	final public String uriNamespace;
	
	public EntityIdGenerator(final String uriNamespace) {
		this.uriNamespace = uriNamespace;
	}
	
	
	/**
	 * This method can be used to add
	 * a unique generated ID and generated URI to the annotation
	 * 
	 * @param annotation - annotation for which the id is required
	 * @return Generated unique ID for this annotation
	 */
	/**
	 * This method can be used to add
	 * unique generated ID and generated URI to the annotation
	 * 
	 * @param annotation 			- annotation for which the id is required
	 * @param namespaceExtension	- extension to base namespace
	 * @return
	 */
	public synchronized String addUniqueId(final Annotation annotation) {
		if (null == annotation || null == annotation.span) {
			return null;
		}
		
		// get annotation span
		Span span = annotation.span;
		String generatedId = this.spanIdMap.get(span);		
		if (null == generatedId) {
			generatedId = this.getUniqueId();
			
			
			this.spanIdMap.put(span, generatedId);
		}
		
		String generatedUri = (null == this.uriNamespace) ? generatedId : this.uriNamespace + generatedId;
		annotation.generatedUri = generatedUri;
		
		annotation.generatedId = generatedId;
		return generatedId;
	}
}
