package sdw.aksw.org.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import aksw.org.sdw.kg.handler.solr.KgSolrException;

public class KgSolrGeonamesConfig extends KgSolrConfigBase {

	/** config instance */
	static KgSolrGeonamesConfig config = null;
	
	/** path to config file */
	static JsonReader jsonReader = null;
	
	protected String solrUrl;

	protected String sparqlUrl;	
	
	protected String graphNameGeonames;
	
	protected String graphNameDbpedia;
	
	protected KgSolrGeonamesConfig() {
		
	}
	
	static public KgSolrGeonamesConfig getInstance() {
		return KgSolrConfigBase.getInstance(KgSolrGeonamesConfig.class);
	}
	
	@Override
	public void init(KgSolrConfigBase configInstanceBase, JsonReader jsonReader) throws KgSolrException {	
		if (null == configInstanceBase ||
			false == KgSolrGeonamesConfig.class.isInstance(configInstanceBase) ||
			null == jsonReader) {
			return;
		}
		
		try {
			JsonElement element = jsonReader.getJson();
			if (null == element || element.isJsonNull()) {
				return;
			}
			
			KgSolrGeonamesConfig configInstance = (KgSolrGeonamesConfig) configInstanceBase;
			
			if (element.isJsonObject()) {
				JsonObject jsonObject = element.getAsJsonObject();
				configInstance.solrUrl = JsonReader.getElementString(jsonObject, "solrUrl", true);
				configInstance.sparqlUrl = JsonReader.getElementString(jsonObject, "sparqlUrl", true);
				configInstance.graphNameGeonames = JsonReader.getElementString(jsonObject, "graphNameGeonames", true);
				configInstance.graphNameDbpedia = JsonReader.getElementString(jsonObject, "graphNameDbpedia", true);
			}
		} catch (Exception e) {
			throw new KgSolrException("Was not able to init config", e);
		}
	}
	
	public String getSolrUrl() {
		return this.solrUrl;
	}

	public String getSparqlUrl() {
		return this.sparqlUrl;
	}
	
	public String getGraphNameGeonames() {
		return this.graphNameGeonames;
	}
	
	public String getGraphDBpedia() {
		return this.graphNameDbpedia;
	}
}
