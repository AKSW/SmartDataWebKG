package sdw.aksw.org.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.JsonElement;

import aksw.org.sdw.kg.handler.solr.KgSolrException;

/**
 * Basic interface for all related config instances
 * 
 * @author kay
 *
 */
public abstract class KgSolrConfigBase {
	
	/** config instance */
	static Map<Class<? extends KgSolrConfigBase>, KgSolrConfigBase> configMap = new HashMap<>();
	
	static protected Lock lock = new ReentrantLock(true); 
	
	protected KgSolrConfigBase() {
		
	}
	
	/**
	 * Get instance of this class
	 * 
	 * @param configClass
	 * @return
	 */
	static protected <T extends KgSolrConfigBase> T getInstance(Class<T> configClass) {
		
		lock.lock();		
		try {
			@SuppressWarnings("unchecked")
			T configInstance = (T) configMap.get(configClass);
			if (null == configInstance) {
				try {
					configInstance = configClass.newInstance();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				
				configMap.put(configClass, configInstance);
			}
	
			return configInstance;
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * This is the method which has to be called first for all config instances!
	 * 
	 * @param filePath
	 * @throws KgSolrException
	 */
	public <T extends KgSolrConfigBase> void init(final String filePath) throws KgSolrException {
		if (null == filePath) {
			return;
		}
		
		try {
			/** path to config file */
			JsonReader jsonReader = new JsonReader(filePath);
			
			JsonElement element = jsonReader.getJson();
			if (null == element || element.isJsonNull()) {
				return;
			}
			
			KgSolrConfigBase configInstance = getInstance(this.getClass());
			
			// now initialise everything!
			this.init(configInstance, jsonReader);			
			
		} catch (Exception e) {
			throw new KgSolrException("Was not able to init config", e);
		}
	}
	
	/**
	 * This method can be used to initialise the actual config instance
	 * 
	 * @param configInstance	- actual config instance
	 * @param jsonReader		- json reader which has loaded the current config file
	 * @throws KgSolrException
	 */
	public abstract void init(final KgSolrConfigBase configInstance, final JsonReader jsonReader) throws KgSolrException;

}
