package aksw.org.sdw.importer.avro.annotations;

import java.util.HashSet;
import java.util.Set;

/**
 * Class which can be used to store mention information
 * 
 * @author kay
 *
 */
public class Mention  extends Annotation {
	
	/** actual mention type */
	public enum MentionType {ENTITY, RELATION};
	/** specifies whether it is an entity or relation*/
	public MentionType mentionType;	
	/** types of entity which was found */
	public Set<String> types = new HashSet<>();
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((mentionType == null) ? 0 : mentionType.hashCode());
		result = prime * result + ((types == null) ? 0 : types.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Mention other = (Mention) obj;
		if (mentionType != other.mentionType)
			return false;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}
}
