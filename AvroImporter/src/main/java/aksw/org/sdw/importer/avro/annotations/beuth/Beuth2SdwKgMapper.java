package aksw.org.sdw.importer.avro.annotations.beuth;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import aksw.org.sdw.rdf.namespaces.CorpDbpedia;
import aksw.org.sdw.rdf.namespaces.W3COrg;

public class Beuth2SdwKgMapper {
	
	/** This map can be used to map DFKI types to KG class URIs */
	static final private Map<String, Collection<String>> typeMappings = getTypeMappings();	
	static final private Map<String,  Collection<String>> getTypeMappings() {
		Map<String,  Collection<String>> typeMappings = new HashMap<>();
		
		typeMappings.put("COMPANY",
				Arrays.asList(W3COrg.organization, W3COrg.FormalOrganization, CorpDbpedia.company));
		
		typeMappings.put("SENSOR",
				Arrays.asList("http://corp.dbpedia.org/ontology#Technology",
						"http://corp.dbpedia.org/ontology#TechnologySensor"));

		
		return Collections.unmodifiableMap(typeMappings);
	}
	
	/**
	 * This method can be used to map the input source entity type to a target entity type and
	 * to add the mapped type to the target type list.
	 * 
	 * @param sourceType	- source entity type
	 * @param targetTypes	- target entity type collection
	 */
	static public void addEntityTypeMapping(final String sourceType, final Collection<String> targetTypes) {
		if (null == sourceType || sourceType.isEmpty()) {
			return;
		}
		
		if (null == targetTypes) {
			return;
		}
		
		// find mapping and add it to the target collection
		Collection<String> mappedTypes = Beuth2SdwKgMapper.typeMappings.get(sourceType);
		if (null != mappedTypes) {
			targetTypes.addAll(mappedTypes);
		} else {
			throw new RuntimeException("Was not able to map entity type: " + sourceType);
		}
	}

}
