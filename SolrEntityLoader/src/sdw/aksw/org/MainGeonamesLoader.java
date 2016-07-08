package sdw.aksw.org;

import java.io.IOException;

import aksw.org.sdw.kg.datasets.GeonamesLoader;
import aksw.org.sdw.kg.handler.solr.KgSolrException;
import aksw.org.sdw.kg.handler.solr.SolrHandler;
import aksw.org.sdw.kg.handler.solr.SolrUriInputDocument;
import sdw.aksw.org.config.KgSolrConfig;
import sdw.aksw.org.config.KgSolrGeonamesConfig;
import sdw.aksw.org.dbpedia.DbpediaUtils;
import sdw.aksw.org.geonames.Geonames2Solr;

/**
 * This class can be used to execute the Geonames SPARQL to SOLR ingestion
 *  
 * @author kay
 *
 */
public class MainGeonamesLoader {
	
	public static void main(String[] args) throws IOException, KgSolrException {
		System.out.println("Country Loader");
		
		MainEntityLoader.initConfig(args, KgSolrGeonamesConfig.class);		
		
		GeonamesLoader loader = new GeonamesLoader(
				KgSolrGeonamesConfig.getInstance().getSparqlUrl(),
				KgSolrGeonamesConfig.getInstance().getGraphNameGeonames());
		DbpediaUtils dbpediaUtils = new DbpediaUtils(
				KgSolrGeonamesConfig.getInstance().getSparqlUrl(),
				KgSolrGeonamesConfig.getInstance().getGraphDBpedia());
		
		SolrHandler solrHandler = new SolrHandler(KgSolrGeonamesConfig.getInstance().getSolrUrl());
		
		if (KgSolrGeonamesConfig.getInstance().deleteAllSolrDocs()) {
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
