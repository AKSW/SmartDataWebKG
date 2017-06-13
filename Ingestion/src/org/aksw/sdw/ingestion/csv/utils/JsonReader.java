package org.aksw.sdw.ingestion.csv.utils;

import java.io.FileReader;
import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

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

}
