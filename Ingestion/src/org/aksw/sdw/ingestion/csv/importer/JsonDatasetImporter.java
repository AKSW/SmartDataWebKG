package org.aksw.sdw.ingestion.csv.importer;

import org.aksw.sdw.ingestion.csv.utils.JsonReader;

public abstract class JsonDatasetImporter implements DatasetImporter {
	
	/** instance which can be used to load a json file into memory */
	final JsonReader jsonReader;
	
	public JsonDatasetImporter(final String filePath) {
		this.jsonReader = new JsonReader(filePath);
		
	}
	
	protected JsonReader getJsonReader() {
		return this.jsonReader;
	}
}
