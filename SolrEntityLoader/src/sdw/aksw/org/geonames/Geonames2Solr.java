package sdw.aksw.org.geonames;

import sdw.aksw.org.Main;
import sdw.aksw.org.config.KgSolrConfig;

import java.io.IOException;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

import aksw.org.sdw.kg.datasets.GeonamesLoader;
import aksw.org.sdw.kg.handler.solr.KgSolrException;
import aksw.org.sdw.kg.handler.solr.SolrHandler;
import aksw.org.sdw.kg.handler.solr.SolrUriInputDocument;
import sdw.aksw.org.dbpedia.DbpediaUtils;

public class Geonames2Solr {
	
	final DbpediaUtils dbpediaUtils;
	
	final SolrHandler solrHandler;
	
	public Geonames2Solr(final SolrHandler solrHandler, final DbpediaUtils dbpediaUtils) {
		this.solrHandler = solrHandler;	
		this.dbpediaUtils = dbpediaUtils;
	}
	
	public void runQuery() {

	}
	
	void getDbpediaSameAs(SolrInputDocument solrInputDocument) {
		if (null == solrInputDocument || false == solrInputDocument.hasChildDocuments()) {
			return;
		}
		
		// find sameAs relationships
		SolrInputField id = solrInputDocument.get("id");		
		List<String> links = dbpediaUtils.getSameAsUris((String) id.getValue());
		if (null != links) {
			for (String link : links) {
				solrInputDocument.addField("sameAs", link);
			}
		}	
		
		// check for children as well
		for (SolrInputDocument childDoc : solrInputDocument.getChildDocuments()) {
			this.getDbpediaSameAs(childDoc);
		}
	}
	
	public static void main(String[] args) throws IOException, KgSolrException {
		System.out.println("Country Loader");
		
		Main.init(args);		
		
		GeonamesLoader loader = new GeonamesLoader(KgSolrConfig.getInstance().getSparqlUrl(), "http://geonames.org");
		DbpediaUtils dbpediaUtils = new DbpediaUtils(KgSolrConfig.getInstance().getSparqlUrl(), "http://dbpedia.org");
		
		SolrHandler solrHandler = new SolrHandler(KgSolrConfig.getInstance().getSolrUrl());
		
		if (true) {
			solrHandler.deleteAllDocuments();
		}
		
		Geonames2Solr geonames2Solr = new Geonames2Solr(solrHandler, dbpediaUtils);
		
		int count = 0;
		while (loader.hasNext()) {
			SolrUriInputDocument solrDoc = loader.next();
			System.out.println("Got: " + solrDoc);	
			
			geonames2Solr.getDbpediaSameAs(solrDoc.getSolrInputDocument());
			
			solrHandler.addSolrDocument(solrDoc);
			++count;
		}
		
		loader.close();
		
		solrHandler.close();
		
		System.out.println("Count: " + count);
	}
}
