package aksw.org.sdw.importer.avro.annotations;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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
	
	/**
	 * 
	 * @return virtual Map of relationship name and all its relation mentions
	 */
	public Iterator<Map.Entry<String, Document>> getRelationshipMentionIterator();
	
	public default Iterable<Map.Entry<String, Document>> getRelationshipMentionIterable() {
		return new Iterable<Map.Entry<String, Document>>() {

			@Override
			public Iterator<Entry<String, Document>> iterator()
			{
				return getRelationshipMentionIterator() ;
			}
		};
	}

}
