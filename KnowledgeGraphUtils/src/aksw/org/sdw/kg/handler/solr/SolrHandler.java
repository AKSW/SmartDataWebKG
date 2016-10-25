package aksw.org.sdw.kg.handler.solr;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.common.util.NamedList;

import aksw.org.kg.KgException;

/**
 * This class can be used to use a SOLR instance:
 * - connect
 * - make query
 * - submit SOLR document
 * - delete SOLR document
 * 
 * @author kay
 *
 */
public class SolrHandler implements Closeable {
	
	/** SOLR connection URL */
	final String solrUrl;
	
	/** SOLR client instance */
	SolrClient solrClient;
	
	/**
	 * Constr
	 * @param solrUrl
	 */
	public SolrHandler(final String solrUrl) {
		this.solrUrl = solrUrl;
		this.solrClient = this.getSolrClient(solrUrl);
	}
	
	public void commit() throws KgSolrException {		
		try {
			this.solrClient.commit();
		} catch (Exception e) {
			throw new KgSolrException("Was not able to commit any data", e);
		}
	}
	
	/**
	 * This method can be used to obtain a Solr client instance
	 * 
	 * @todo Check whether we want to support Solr Cloud setup
	 * @param solrUrl	- URL which contain IP/DNS port and collection name
	 * (e.g. "http://localhost:8983/solr/Test")
	 * @return solr client instance
	 */
	protected SolrClient getSolrClient(final String solrUrl) {
		//SolrClient solrClient = new ConcurrentUpdateSolrClient(solrUrl, 16, 64);
		SolrClient solrClient = new HttpSolrClient(solrUrl);
		return solrClient;	    
	}
	
	/**
	 * This method can be used to obtain all the registered and known field names,
	 * which are stored in SOLR.
	 * 
	 * @return collection of known field names
	 * throws DataIdSolrException
	 */
	@SuppressWarnings("unchecked")
	public Collection<String> getKnownSolrFieldNames() throws KgSolrException {
		
		try {
			// create query to obtain field information from SOLR
			SolrQuery query = new SolrQuery();
			query.setRequestHandler("/schema/fields");
			query.setParam("includeDynamic", "true");
			query.setParam("showDefaults", "true");
			
			QueryResponse result = this.solrClient.query(query);
			if (null == result) {
				return null;
			}
			
			NamedList<Object> response = result.getResponse();
			if (null == response) {
				return null;
			}
			
			// get actual field data
			List<Object> fieldsInfo = (List<Object>) response.get("fields");
			if (null == fieldsInfo) {
				return null;
			}
			
			// go through all the fields and get name
			Set<String> knownFieldNames = new HashSet<>();
			for (int i = 0; i < fieldsInfo.size(); ++i) {
				NamedList<Object> fieldInfo = (NamedList<Object>) fieldsInfo.get(i);
				if (null == fieldInfo) {
					continue;
				}
				
				String fieldName = (String) fieldInfo.get("name");
				if (null != fieldName && false == fieldName.isEmpty()) {				
					knownFieldNames.add(fieldName);
				}
			}			
			
			// return known field names
			return knownFieldNames;
		} catch (Exception e) {
			throw new KgSolrException("Was not able to obtain field names from SOLR", e);
		}
	}
	
	
	/**
	 * This method can be used to add new SOLR documents to the solr index
	 * 
	 * @param solrDocuments	- solr document
	 * @throws KgSolrException
	 */
	public void addSolrDocuments(final Collection<KgSorlInputDocument> solrDocuments) throws KgSolrException {
		if (null == solrDocuments || solrDocuments.isEmpty()) {
			return;
		}
		// create a batch and send in one go!
		List<SolrInputDocument> solrInputDocuments = new ArrayList<>();		
		for (KgSorlInputDocument solrDocument : solrDocuments) {
			solrInputDocuments.add(solrDocument.getSolrInputDocument());
		}
		
		try {
			solrClient.add(solrInputDocuments, 10000);
			solrClient.commit();
		} catch (Exception e) {
			throw new KgSolrException("Was not able to add new SOLR document", e);
		}
	}
	
	public void addSolrDocument(final KgSorlInputDocument solrDocument) throws KgSolrException {
		this.addSolrDocument(solrDocument, false);
	}
	
	/**
	 * This method can be used to add new SOLR documents to the solr index
	 * 
	 * @param solrDocuments	- solr document
	 * @param commit		- commits data to solr
	 * @throws KgSolrException
	 */
	public void addSolrDocument(final KgSorlInputDocument solrDocument, final boolean commit) throws KgSolrException {
		if (null == solrDocument) {
			return;
		}
		
		try {
			solrClient.add(solrDocument.getSolrInputDocument(), 100000);
			
			if (commit) {
				solrClient.commit();
			}
		} catch (Exception e) {
			throw new KgSolrException("Was not able to add new SOLR document", e);
		}
	}
	
	public void deleteAllDocuments() throws KgSolrException {
		try {
			this.solrClient.deleteByQuery("*:*");
			this.solrClient.commit();
		} catch (Exception e) {
			throw new KgSolrException("Was not able to delete the documents", e);
		}
	}
	
	/**
	 * This method can be used to obtain results based on an input query
	 * @param query
	 * @return
	 */
	public List<KgSolrResultDocument> executeQuery(final String query, final List<String> filterQueries) throws KgException {
		if (null == query) {
			return Collections.emptyList();
		}
		
		
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery(query);
		solrQuery.setRows(10000);
		
		if (null != filterQueries) {
			int arrayLength = filterQueries.size();
			solrQuery.addFilterQuery(filterQueries.toArray(new String[arrayLength]));
		}
		
		try {
			
			List<KgSolrResultDocument> results = this.executeSolrQuery(solrQuery);
			if (null == results) {
				return Collections.emptyList();
			}
			
			return results;			
		} catch (Exception e) {
			throw new KgException("Was not able to execute SOLR query", e);
		}
	}
	
	/**
	 * This method can be used to execute a solr query against a SOLR backend
	 * 
	 * @param solrQuery
	 * @return
	 * @throws KgException
	 */
	protected List<KgSolrResultDocument> executeSolrQuery(final SolrQuery solrQuery) throws KgException {
		try {
			QueryResponse response = this.solrClient.query(solrQuery);
			if (null == response) {
				return null;
			}
			
			SolrDocumentList results = response.getResults();
			if (null == results) {
				return null;
			}
			
			if (0 >= results.getNumFound()) {
				return null;
			}	
			
			List<KgSolrResultDocument> resultList = new ArrayList<>();			
			
			ListIterator<SolrDocument> documentIterator = results.listIterator();
			while (documentIterator.hasNext()) {
				SolrDocument solrDoc = documentIterator.next();
				KgSolrResultDocument resultDoc = new KgSolrResultDocument(solrDoc);
				
				resultList.add(resultDoc);
			}
			
			return resultList;
		} catch (Exception e) {
			throw new KgException("Was not able to execute SOLR query", e);
		}
	}
	
	/** enum which specifies the input language */
	public enum TAGGER_LANGUAGE {ENGLISH, GERMAN};
	
	/** enum which specifies how the backend should treat overlapping annotations */
	public enum TAGGER_ANNOTATION_OVERLAP {
		/** Emit all tags */
		ALL,
		/** Don't emit a tag that is completely within another tag (i.e. no subtag). */
		NO_SUB,
		/**
		 * Given a cluster of overlapping tags, emit the longest one (by character length).
		 * If there is a tie, pick the right-most. Remove any tags overlapping with this tag
		 * then repeat the algorithm to potentially find other tags that can be emitted in the cluster.
		 */
		LONGEST_DOMINANT_RIGHT;
	};
	

	/**
	 * This method can be used to obtain annotations from a given text using SOLR Text Tagger. It is required that the SOLR core
	 * documents store entity information.
	 * 
	 * The SOLR Text Tagger is a very naive tagger and it can create a lot of noise! Hence only use it with causion and potentially as a candidate generator for a NER toolchain.
	 * 
	 * @param text				- Input text which is supposed to be annotated.
	 * @param filterQueries		- SOLR filter queries (e.g. filter on type)
	 * @param requiredFields	- fields which are required from the SOLR document
	 * @param language			- specifies the text language
	 * @param overlap			- specifies how overlapping annotations should be treated
	 * @return Map of annotation information and corresponding SOLR documents
	 * @throws KgException
	 */
	public Map<AnnotationInfo, List<KgSolrResultDocument>> getNamedEntitiesFromText(final String text, final List<String> filterQueries,
																					   final Set<String> requiredFields,
																					   final TAGGER_LANGUAGE language,
																					   final TAGGER_ANNOTATION_OVERLAP overlap) throws KgException {
		return this.getNamedEntitiesFromText(text, filterQueries, requiredFields, 5000, language, overlap);
	}
	
	
	/**
	 * This method can be used to obtain annotations from a given text using SOLR Text Tagger. It is required that the SOLR core
	 * documents store entity information.
	 * 
	 * The SOLR Text Tagger is a very naive tagger and it can create a lot of noise! Hence only use it with causion and potentially as a candidate generator for a NER toolchain.
	 * 
	 * @param text				- Input text which is supposed to be annotated.
	 * @param filterQueries		- SOLR filter queries (e.g. filter on type)
	 * @param requiredFields	- fields which are required from the SOLR document
	 * @param tagsLimit			- maximum number of annotations/tags which be found in the input text
	 * @param language			- specifies the text language
	 * @param overlap			- specifies how overlapping annotations should be treated
	 * @return Map of annotation information and corresponding SOLR documents
	 * @throws KgException
	 */
	@SuppressWarnings("unchecked")
	protected Map<AnnotationInfo, List<KgSolrResultDocument>> getNamedEntitiesFromText(final String text, final List<String> filterQueries,
																					   final Set<String> requiredFields,
																					   final int tagsLimit,
																					   final TAGGER_LANGUAGE language,
																					   final TAGGER_ANNOTATION_OVERLAP overlap) throws KgException {
		
		try {			
			
			String handler = (language == TAGGER_LANGUAGE.ENGLISH) ? "/tagEn" : "/tagDe";
		    ContentStreamUpdateRequest req = new ContentStreamUpdateRequest(handler);
		    req.setParam("overlaps", overlap.name());
		    req.setParam("tagsLimit", Integer.toString(tagsLimit));		    
		    req.setParam("matchText", "true");
		    
		    // add filter queries
		    if (null != filterQueries) {
		    	for (String filterQuery : filterQueries) {
		    		req.setParam("fq", filterQuery);
		    	}
		    }
		    
		    // add required fields
		    if (null != requiredFields) {
		    	StringBuffer buffer = new StringBuffer();
		    	for (String requiredField : requiredFields) {
		    		buffer.append(requiredField);
		    		buffer.append(" ");
		    	}
		    	
		    	req.setParam("fl", buffer.toString().trim());
		    } else {
		    	req.setParam("fl", "*");
		    }
		    
		    ContentStreamBase textStream = new ContentStreamBase.StringStream(text, "text/plain");
		    req.addContentStream(textStream);
		    
		    NamedList<Object> results = this.solrClient.request(req);
			if (null == results) {
				return null;
			}
			
			Integer tagCount = (Integer) results.get("tagsCount");
			if (null == tagCount || 0 >= tagCount) {
				return null;
			}
			
			List<Object> annotations = (List<Object>) results.get("tags");
			if (null == annotations) {
				return null;
			}
			
			// get all annotation information
			Map<AnnotationInfo, List<KgSolrResultDocument>> annotationInfoMap = new LinkedHashMap<>();
			for (Object annotation : annotations) {
				NamedList<Object> annotationInfo = (NamedList<Object>)  annotation;
				int startOffset = (int) annotationInfo.get("startOffset");
				int endOffset = (int) annotationInfo.get("endOffset");
				String matchText = (String) annotationInfo.get("matchText");
				List<String> ids = (List<String>)  annotationInfo.get("ids");
				
				AnnotationInfo annotationInformation = new AnnotationInfo(startOffset, endOffset, matchText, ids);
				annotationInfoMap.put(annotationInformation, new ArrayList<>());
			}
			
			// load SOLR documents
			Map<String, KgSolrResultDocument> matchingDocumentMap = new HashMap<>();
			SolrDocumentList documentList = (SolrDocumentList) results.get("response");			
			for (SolrDocument solrDoc : documentList) {
				String id = (String) solrDoc.get("id");
				
				matchingDocumentMap.put(id, new KgSolrResultDocument(solrDoc));
			}
			
			// now match offsets to SOLR documents
			for (AnnotationInfo annotationInfo : annotationInfoMap.keySet()) {
				
				List<KgSolrResultDocument> resultDocuments = annotationInfoMap.get(annotationInfo);
				
				for (String id : annotationInfo.ids) {
					KgSolrResultDocument resultDocument = matchingDocumentMap.get(id);
					if (null == resultDocument) {
						continue;
					}
					
					resultDocuments.add(resultDocument);
				}
			}

			return annotationInfoMap;
		} catch (Exception e) {
			throw new KgSolrException("Was not able to get entities from text", e);
		}		
	}

	@Override
	public void close() throws IOException {
		if (null != this.solrClient) {
			this.solrClient.close();
			this.solrClient = null;
		}
	}
	
	/**
	 * This class can be used to store annotation information
	 * which come from the SOLR Text Tagger
	 * 
	 * @author kay
	 *
	 */
	static public class AnnotationInfo implements Comparable<AnnotationInfo> {
		final public int startOffset;
		final public int endOffset;
		final public String matchText;
		final public List<String> ids;
		
		public AnnotationInfo(final int startOffset, final int endOffset, final String matchText, final List<String> ids) {
			this.startOffset = startOffset;
			this.endOffset = endOffset;
			this.matchText = matchText;
			this.ids = ids;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + endOffset;
			result = prime * result + ((matchText == null) ? 0 : matchText.hashCode());
			result = prime * result + startOffset;
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
			AnnotationInfo other = (AnnotationInfo) obj;
			if (matchText == null) {
				if (other.matchText != null)
					return false;
			} else if (!matchText.equals(other.matchText))
				return false;
			if (endOffset != other.endOffset)
				return false;			
			if (startOffset != other.startOffset)
				return false;
			return true;
		}

		@Override
		public int compareTo(AnnotationInfo o) {
			if (this.equals(o)) {
				return 0;
			}
			
			int diffStart = this.startOffset - o.startOffset;
			int diffEnd = this.endOffset - o.endOffset;
			
			if (0 != diffStart) {
				return diffStart;
			} else if (0 != diffEnd) {
				return diffEnd;
			}
			
			return 0;
		}
	}
}

