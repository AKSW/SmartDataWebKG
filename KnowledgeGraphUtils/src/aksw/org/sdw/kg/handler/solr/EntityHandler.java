package aksw.org.sdw.kg.handler.solr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import aksw.org.kg.KgException;

/**
 * This class can be used to interact with the entity data
 * which is stored in SOLR
 * 
 * @author kay
 *
 */
public class EntityHandler {
	
	/** SOLR handler which can be used to communicate with SOLR backend */
	protected final SolrHandler solrHandler;
		
	public EntityHandler(final SolrHandler solrHandler) {
		this.solrHandler = solrHandler;		
	}

	public List<String> getMatchingEntities(final String entityName, final String languageCode, final String type) throws KgException {
		if (null == entityName) {
			return Collections.emptyList();
		}
		
		final String finalLanguageCode;
		if (null != languageCode && (languageCode.equals("en") || languageCode.equals("de"))) {
			finalLanguageCode = languageCode;
		} else {
			// default to English
			finalLanguageCode = "en";
		}
		
		StringBuffer buffer = new StringBuffer();
		if (finalLanguageCode.equals("en")) {
			buffer.append("nameEn:\"");
			buffer.append(entityName);
			buffer.append("\"");
		} else if (finalLanguageCode.equals("de")) {
			buffer.append("nameDe:\"");
			buffer.append(entityName);
			buffer.append("\"");
		}
		
		List<String> filterQueries = null;
		if (null != type) {
			filterQueries = Arrays.asList("type:\"" + type + "\"");
		}
		
		List<KgSolrResultDocument> solrDocs = this.solrHandler.executeQuery(buffer.toString(), filterQueries);
		if (null == solrDocs) {
			Collections.emptyList();
		}
		
		List<String> entityKeys = new ArrayList<>();
		for (KgSolrResultDocument solrDoc : solrDocs) {
			String id = (String) solrDoc.getFieldValueAsString("id");
			entityKeys.add(id);
		}
		
		return entityKeys;
	}
	
	public String getNamedEntitiesFromText(final SolrHandler solrHandler, final String text) {
		
		
		
		return null;
	}
}
