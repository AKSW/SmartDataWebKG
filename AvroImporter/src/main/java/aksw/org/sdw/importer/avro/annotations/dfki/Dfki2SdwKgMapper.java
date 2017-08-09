package aksw.org.sdw.importer.avro.annotations.dfki;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import aksw.org.sdw.rdf.namespaces.W3COrg;

public class Dfki2SdwKgMapper {
	
	/** This map can be used to map DFKI types to KG class URIs */
	static final private Map<String, Collection<String>> typeMappings = getTypeMappings();
	static final public Map<String, AtomicInteger> missingMappings = new HashMap<>();
	static final private Map<String, Collection<String>> getTypeMappings() {
		Map<String, Collection<String>> typeMappings = new HashMap<>();
		
		typeMappings.put("organization", Arrays.asList("http://www.w3.org/ns/org#Organization",
										"http://dbpedia.org/ontololgy/Organisation"));
		typeMappings.put("company", Arrays.asList("http://dbpedia.org/ontololgy/Organisation",
									"http://www.w3.org/ns/org#FormalOrganization",
									"http://corp.dbpedia.org/ontology#Company")); /// TODO: CHECK
		typeMappings.put("person", Collections.singleton("http://dbpedia.org/ontology/Person/"));
		typeMappings.put("location", Collections.singleton("http://dbpedia.org/ontology/Place"));
		typeMappings.put("product", Collections.singleton("http://corp.dbpedia.org/ontology#Product")); // TODO: Check!
		typeMappings.put("headquarter", Arrays.asList(W3COrg.site, "http://dbpedia.org/ontology/headquarter"));
		typeMappings.put("parent", Collections.singleton("dbpedia.org/ontology/parentCompany"));
		typeMappings.put("child", Collections.singleton("http://dbpedia.org/ontology/subsidiary"));
		
		typeMappings.put("industry", Collections.singleton("http://corp.dbpedia.org/ontology#OrgCategory"));
		typeMappings.put("org-position", Collections.singleton("http://corp.dbpedia.org/ontology#OrgCategory"));
		typeMappings.put("org-termination-type", Collections.singleton("http://dbpedia.org/ontology/Event"));
		typeMappings.put("project", Collections.singleton("http://corp.dbpedia.org/ontology#Project"));
		
		typeMappings.put("facility-type", Collections.singleton("http://dbpedia.org/ontology/Place"));
		typeMappings.put("disaster-type", Collections.singleton("http://dbpedia.org/ontology/Event"));
		
		typeMappings.put("url", Collections.singleton("http://dbpedia.org/ontology/Website"));
		typeMappings.put("number", Collections.singleton("http://dbpedia.org/ontology/MathematicalConcept"));
		
		typeMappings.put("date", Collections.singleton("http://dbpedia.org/ontology/TimePeriod"));
		typeMappings.put("time", Collections.singleton("http://dbpedia.org/ontology/TimePeriod"));
		typeMappings.put("duration", Collections.singleton("http://dbpedia.org/ontology/TimePeriod"));
		
		typeMappings.put("money", Collections.singleton("http://dbpedia.org/ontology/Statistic"));
		typeMappings.put("distance", Collections.singleton("http://dbpedia.org/ontology/Statistic"));
		typeMappings.put("percentage", Collections.singleton("http://dbpedia.org/ontology/Statistic"));
		typeMappings.put("ordinal", Collections.singleton("http://dbpedia.org/ontology/Statistic"));
		
		typeMappings.put("misc", Collections.singleton("http://dbpedia.org/ontology/Unknown"));
		typeMappings.put("set", Collections.singleton("http://dbpedia.org/ontology/Unknown"));
		
		typeMappings.put("sensor", Collections.singleton("http://dbpedia.org/resource/Sensor"));
						
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
		Collection<String> mappedTypes = Dfki2SdwKgMapper.typeMappings.get(sourceType.toLowerCase());
		if (null != mappedTypes) {
			targetTypes.addAll(mappedTypes);
		} else {
			Level level = Level.WARNING;
			Logger.getGlobal().log(level, "Was not able to map entity type: " + sourceType);
			recordUnmappedEntities(sourceType);
			targetTypes.add("http://UNMAPPED.ER/"+sourceType);
			//throw new RuntimeException("Was not able to map entity type: " + sourceType);
		}
	}
	
	static public void recordUnmappedEntities(String sourcetype)
	{
		AtomicInteger count = missingMappings.get(sourcetype);
		if (count !=null)
			count.incrementAndGet();
		else 
			missingMappings.put(sourcetype, new AtomicInteger(1));
	}

}
