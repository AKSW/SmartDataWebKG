package org.aksw.sdw.search.solr;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//import org.aksw.dataid.solr.SolrException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.NamedList;

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
	 * throws SolrException
	 */
	@SuppressWarnings("unchecked")
	public Collection<String> getKnownSolrFieldNames() throws SolrException {
		
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
			throw new SolrException("Was not able to obtain field names from SOLR", e);
		}
	}
	
	
	/**
	 * This method can be used to add new SOLR documents to the solr index
	 * 
	 * @param solrDocuments	- solr document
	 * @throws SolrException
	 */
	public void addSolrDocuments(final Collection<SorlDocumentInput> solrDocuments) throws SolrException {
		if (null == solrDocuments || solrDocuments.isEmpty()) {
			return;
		}
		
		try {
			for (SorlDocumentInput solrDocument : solrDocuments) {
				solrClient.add(solrDocument.getSolrInputDocument(), 10000);
			}
			
			solrClient.commit();
		} catch (Exception e) {
			throw new SolrException("Was not able to add new SOLR document", e);
		}
	}
	
	/**
	 * This method can be used to add new SOLR documents to the solr index
	 * 
	 * @param solrDocuments	- solr document
	 * @throws SolrException
	 */
	public void addSolrDocument(final SorlDocumentInput solrDocument) throws SolrException {
		if (null == solrDocument) {
			return;
		}
		
		try {
			solrClient.add(solrDocument.getSolrInputDocument(), 10000);
			solrClient.commit();
		} catch (Exception e) {
			throw new SolrException("Was not able to add new SOLR document", e);
		}
	}
	
	public void deleteAllDocuments() throws SolrException {
		try {
			this.solrClient.deleteByQuery("*:*");
			this.solrClient.commit();
		} catch (Exception e) {
			throw new SolrException("Was not able to delete the documents", e);
		}
	}

	@Override
	public void close() throws IOException {
		if (null != this.solrClient) {
			this.solrClient.close();
			this.solrClient = null;
		}
	}
}
