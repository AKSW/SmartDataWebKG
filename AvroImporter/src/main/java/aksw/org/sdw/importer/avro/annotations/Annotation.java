package aksw.org.sdw.importer.avro.annotations;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Marks general annotation instances
 * 
 * @author kay
 *
 */
public abstract class Annotation {
	
	/** generated unique id */
	public String generatedId;
	/** generated URI */
	public String generatedUri;
	/** unique id of annotation  from source */
	public String id;
	/** provenance which is associated to this annotation */
	public Set<Provenance> provenanceSet = new HashSet<>();
	/** language code of this tex */
	public String langCode;
	/** date when this instance was created */
	final public Date currentDate = new Date();
	
	/** text which is covered by this annotation */
	public String text;
	/** normalized text */
	public String textNormalized;
	/** span which is covered by this annoation */
	
	public Span span = new Span();
	
	public String toJson() {		
		Gson gson = new Gson();
		String json = gson.toJson(this);
		return json;
	}
	
	@Override
	public String toString() {
		GsonBuilder gb = new GsonBuilder();
		gb.serializeSpecialFloatingPointValues();
		Gson gson = gb.create();
		String json = gson.toJson(this);
		return json;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((generatedId == null) ? 0 : generatedId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((langCode == null) ? 0 : langCode.hashCode());
		result = prime * result + ((provenanceSet == null) ? 0 : provenanceSet.hashCode());
		result = prime * result + ((span == null) ? 0 : span.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
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
		Annotation other = (Annotation) obj;
		if (generatedId == null) {
			if (other.generatedId != null)
				return false;
		} else if (!generatedId.equals(other.generatedId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (langCode == null) {
			if (other.langCode != null)
				return false;
		} else if (!langCode.equals(other.langCode))
			return false;
		if (provenanceSet == null) {
			if (other.provenanceSet != null)
				return false;
		} else if (!provenanceSet.equals(other.provenanceSet))
			return false;
		if (span == null) {
			if (other.span != null)
				return false;
		} else if (!span.equals(other.span))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}
}
