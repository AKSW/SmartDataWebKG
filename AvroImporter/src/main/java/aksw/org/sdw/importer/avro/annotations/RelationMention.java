package aksw.org.sdw.importer.avro.annotations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Class which can be used to handle a relationship mention
 * 
 * @author kay
 *
 */
public class RelationMention {
	
	public String generatedId;
	
	public String generatedUri;
	
	/** All entities which were found for this relationship */
	public List<Mention> entities = new ArrayList<>();
	
//	/** concept on the left **/
//	public Mention leftConcept;
//	
//	/** concept on the right **/
//	public Mention rightConcept;
	
	/** actual relationship mention */
	public Mention relation = new Mention();
	
	/** provenance for this relationship */
	public Set<Provenance> provenance = new LinkedHashSet<>();
	
	public String toJson() {
		GsonBuilder gb = new GsonBuilder();
		gb.serializeSpecialFloatingPointValues();
		Gson gson = gb.create();
		String json = gson.toJson(this);
		return json;
	}
	
	@Override
	public String toString() {
		Gson gson = new Gson();
		String json = gson.toJson(this);
		return json;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entities == null) ? 0 : entities.hashCode());
		result = prime * result + ((provenance == null) ? 0 : provenance.hashCode());
		result = prime * result + ((relation == null) ? 0 : relation.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelationMention other = (RelationMention) obj;
		if (entities == null) {
			if (other.entities != null)
				return false;
		} else if (!entities.equals(other.entities))
			return false;
		if (provenance == null) {
			if (other.provenance != null)
				return false;
		} else if (!provenance.equals(other.provenance))
			return false;
		if (relation == null) {
			if (other.relation != null)
				return false;
		} else if (!relation.equals(other.relation))
			return false;
		return true;
	}
}