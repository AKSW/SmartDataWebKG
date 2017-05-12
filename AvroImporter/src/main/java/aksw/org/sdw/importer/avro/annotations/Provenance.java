package aksw.org.sdw.importer.avro.annotations;

import java.util.Date;

import com.google.gson.Gson;

/**
 * Class which can be used to store provenance information
 * 
 * @author kay
 *
 */
public class Provenance {

	public String source;
	public String annotator;
	public String license;
	public float score = 0f;
	public Date date;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotator == null) ? 0 : annotator.hashCode());
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((license == null) ? 0 : license.hashCode());
		result = prime * result + Float.floatToIntBits(score);
		result = prime * result + ((source == null) ? 0 : source.hashCode());
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
		Provenance other = (Provenance) obj;
		if (annotator == null) {
			if (other.annotator != null)
				return false;
		} else if (!annotator.equals(other.annotator))
			return false;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (license == null) {
			if (other.license != null)
				return false;
		} else if (!license.equals(other.license))
			return false;
		if (Float.floatToIntBits(score) != Float.floatToIntBits(other.score))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}

	public String toJson() {		
		Gson gson = new Gson();
		String json = gson.toJson(this);
		return json;
	}
	
	@Override
	public String toString() {
		Gson gson = new Gson();
		String json = gson.toJson(this);
		return json;
	}
}
