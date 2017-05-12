package aksw.org.sdw.importer.avro.annotations;

import com.google.gson.Gson;

/**
 * Class which can be used to store span information of a mention
 * 
 * @author kay
 *
 */
public class Span {
	
	/** start of surface form offset */
	public int start;
	
	/** end of surface form offset */
	public int end;
	
//	/** token position of first token */
//	public int tokenPositionStart;
//	
//	/** token position of last token */
//	public int tokenPositionEnd;
	
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + end;
		result = prime * result + start;
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
		Span other = (Span) obj;
		if (end != other.end)
			return false;
		if (start != other.start)
			return false;
		return true;
	}		
}
