package aksw.org.sdw.importer.avro.annotations.dfki;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import aksw.org.sdw.importer.avro.annotations.maps.MapHandler;
import aksw.org.sdw.rdf.namespaces.W3COrg;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

public class Dfki2SdwKgMapper {

	/** This map can be used to map DFKI types to KG class URIs */
	static final private Map<String, Collection<String>> typeMappings = getTypeMappings();
	static final public Map<String, AtomicInteger> missingMappings = new HashMap<>();

	static final private String dbo = "http://dbpedia.org/ontology/";
	static final private String org =  "http://www.w3.org/ns/org#";
	static final private String	dbco = "http://corp.dbpedia.org/ontology#";
	static final private String dbtax = "http://dbpedia.org/dbtax/";
	static final private String	schema = "http://schema.org/";
	static final private String	dbt = "http://dbpedia.org/datatype/";

	static final private Map<String, Collection<String>> getTypeMappings() {


		Map<String, Collection<String>> typeMappings = new HashMap<>();
		typeMappings.put( "organization", Arrays.asList( org+"Organization", dbo+"Organisation" ));
		typeMappings.put( "organization-company", Arrays.asList( dbo+"Company", dbo+"Organisation" ));
		typeMappings.put( "product", Arrays.asList( dbo+"Product", schema+"Product" ));
		typeMappings.put( "sensor", Arrays.asList( dbo+"Product", schema+"Product" ));
		typeMappings.put( "technology", Arrays.asList( dbtax+"Technology", dbco+"Technology" ));
		typeMappings.put( "org-termination-type", Arrays.asList( dbo+"Event" ));
		typeMappings.put( "org-position", Arrays.asList( dbco+"OrgCategory" ));
		typeMappings.put( "disaster-type", Arrays.asList( dbco+"Disaster" ));
		typeMappings.put( "facility-type", Arrays.asList( dbo+"Place" ));
		typeMappings.put( "project", Arrays.asList( dbco+"Project" ));
		typeMappings.put( "industry", Arrays.asList( dbco+"OrgCategory" ));
		typeMappings.put( "person", Arrays.asList( dbo+"Person" ));
		typeMappings.put( "url", Arrays.asList( dbo+"Website" ));
		typeMappings.put( "location", Arrays.asList( dbo+"Place" ));
		typeMappings.put( "location-city", Arrays.asList( dbo+"City" ));
		typeMappings.put( "location-street", Arrays.asList( dbo+"Street" ));
		typeMappings.put( "founder", Arrays.asList( dbo+"Person" ));
		typeMappings.put( "headquarter", Arrays.asList( dbo+"location" ));
		typeMappings.put( "new", Arrays.asList( org+"Organization", dbo+"Organisation" ));
		typeMappings.put( "old", Arrays.asList( org+"Organization", dbo+"Organisation" ));
		typeMappings.put( "seller", Arrays.asList( org+"Organization", dbo+"Organisation" ));
		typeMappings.put( "start_loc", Arrays.asList( dbo+"Place" ));
		typeMappings.put( "acquired", Arrays.asList( org+"Organization", dbo+"Organisation" ));
		typeMappings.put( "buyer", Arrays.asList( org+"Organization", dbo+"Organisation" ));
		typeMappings.put( "child", Arrays.asList( org+"Organization", dbo+"Organisation" ));
		typeMappings.put( "company", Arrays.asList( dbo+"Company" ));
		typeMappings.put( "customer", Arrays.asList( org+"Organization", dbo+"Organisation" ));
		typeMappings.put( "financial-event", Arrays.asList("dbco:FinancialEvent"));
		typeMappings.put( "event_type", Arrays.asList("dbco:FinancialEvent"));
		typeMappings.put( "parent", Arrays.asList( org+"Organization", dbo+"Organisation" ));

		// SIEMENS
		typeMappings.put( "company_acquirer", Arrays.asList( org+"Organization", dbo+"Organisation" , dbo+"Company"));
		typeMappings.put( "company_beingacquired", Arrays.asList( org+"Organization", dbo+"Organisation" , dbo+"Company"));
		typeMappings.put( "company_customer", Arrays.asList( org+"Organization", dbo+"Organisation" , dbo+"Company"));
		typeMappings.put( "company_provider", Arrays.asList( org+"Organization", dbo+"Organisation" , dbo+"Company"));

		// FAST fixes
		typeMappings.put( "money", Arrays.asList( dbt+"Currency" ));
		typeMappings.put( "duration", Arrays.asList( dbt+"second" ));

		return Collections.unmodifiableMap(typeMappings);
	}

	public static final Map<String, RDFDatatype> rdfDatatypeMapping;
	static {
		Map<String, RDFDatatype> map = new HashMap<String, RDFDatatype>();
		map.put("date", XSDDatatype.XSDdate);
		map.put("number", XSDDatatype.XSDdouble);
		map.put("set", XSDDatatype.XSDstring);
		map.put("damage_costs", XSDDatatype.XSDdouble);
		map.put("percent", XSDDatatype.XSDdouble);
		map.put("misc", XSDDatatype.XSDstring);
		map.put("ordinal", XSDDatatype.XSDstring);
		rdfDatatypeMapping = Collections.unmodifiableMap(map);
	}

	public static final Map<String, String> datatypeMapping;
	static {
		Map<String, String> map = new HashMap<String, String>();
		for(String key : rdfDatatypeMapping.keySet()) {
			map.put(key, rdfDatatypeMapping.get(key).getURI());
		}
		datatypeMapping = Collections.unmodifiableMap(map);
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

		// wenn ein datatype dann targetTypes clear und nur das neue
		if( datatypeMapping.keySet().contains(sourceType) ){
			targetTypes.clear();
			targetTypes.add(datatypeMapping.get( sourceType ));
			return;
		}

		// wenn schon gemapped existiert return;
		if ( datatypeMapping.values().contains(targetTypes)) {
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
