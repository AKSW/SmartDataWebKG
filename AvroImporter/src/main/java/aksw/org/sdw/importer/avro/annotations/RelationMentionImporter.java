package aksw.org.sdw.importer.avro.annotations;
import java.util.Map;

/**
 * Interface which can be used to import mention information
 * 
 * @author kay
 *
 */
public interface RelationMentionImporter {
	
	/**
	 * 
	 * @return Map of relationship name and all its relation mentions
	 */
	public Map<String, Document> getRelationshipMentions();

}
