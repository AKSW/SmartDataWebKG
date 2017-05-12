package aksw.org.sdw.importer.avro.annotations.ids;

import java.util.UUID;

/**
 * This class can be used to generate unique IDs
 * 
 * @author kay
 *
 */
public class UniqueIdGenerator {
	
	/**
	 * 
	 * @return unique ID as String based on Java UUID class
	 */
	public String getUniqueId() {
	    UUID uniqueId = UUID.randomUUID();
	    return uniqueId.toString();
	}
}
