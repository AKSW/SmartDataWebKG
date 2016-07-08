package aksw.org.kg.input;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import aksw.org.sdw.kg.handler.solr.KgSolrException;

/**
 * This class can be used to read an
 * JSON file and convert it into JSON objects;
 * 
 * @author kay
 *
 */
public class JsonReader {
	
	/** path to json input file */
	final String filePath;
	
	/** JSON parser for this class */
	final JsonParser jsonParser = new JsonParser();	
	
	public JsonReader(final String filePath) {
		this.filePath = filePath;		
	}
	
	/**
	 * This method can be used to get json instance
	 * @return
	 * @throws IOException
	 */
	public JsonElement getJson() throws IOException {
		
		FileReader file = null;
		try {
			file = new FileReader(this.filePath);
			JsonElement jsonElement = this.jsonParser.parse(file);
			
			return jsonElement;
		} finally {
			if (null != file) {
				file.close();
			}
		}
	}
	
	static protected String getElementString(final JsonObject jsonObject, final String elementName, final boolean required) throws KgSolrException {
		JsonElement solrUrl = jsonObject.get(elementName);
		if (null != solrUrl && solrUrl.isJsonPrimitive()) {
			return solrUrl.getAsString();
		} else if (required) {
			throw new KgSolrException("Was not able to find: " + elementName);
		}
		
		return null;
	}
	
	static protected List<String> getElementStringArray(final JsonObject jsonObject, final String elementName, final boolean required) throws KgSolrException {
		JsonElement solrUrl = jsonObject.get(elementName);
		if (null != solrUrl && solrUrl.isJsonArray()) {
			JsonArray array = solrUrl.getAsJsonArray();
			
			List<String> elements = new ArrayList<>();
			for (int i = 0; i < array.size(); ++i) {
				JsonElement element = array.get(i);
				if (null == element || element.isJsonNull()) {
					continue;
				}
				
				if (element.isJsonPrimitive()) {
					elements.add(array.get(i).getAsString());
				}
			}
			
			return elements;
		} else if (required) {
			throw new KgSolrException("Was not able to find: " + elementName);
		}
		
		return null;
	}
}
