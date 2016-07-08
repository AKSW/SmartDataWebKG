package sdw.aksw.org;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import aksw.org.sdw.kg.handler.solr.KgSolrException;
import aksw.org.sdw.kg.handler.solr.SolrHandler;
import sdw.aksw.org.config.KgSolrConfig;
import sdw.aksw.org.config.KgSolrConfigBase;
import sdw.aksw.org.solr.KgJenaEntityToSolrImporter;

public class MainEntityLoader {
	
	public static void initConfig(String[] args, final Class<? extends KgSolrConfigBase> configClass) throws KgSolrException {
		System.out.println("This is the SPARQL to SOLR Entity Loader");
		
		try {
			Options options = new Options();
			options.addOption("c", true, "Path to configuration file");
			options.addOption("d", false, "");
			
			CommandLineParser cmdLineParser = new BasicParser();
			CommandLine parseResults = cmdLineParser.parse(options, args);
			
			if (false == parseResults.hasOption("c")) {
				System.err.println("Was not able to find path to configuration file!");
				return;
			}
			
			String pathToConfigFile = parseResults.getOptionValue("c");
			KgSolrConfigBase.getInstance(configClass).init(pathToConfigFile);			
			KgSolrConfigBase.getInstance(configClass).setDeleteAllSolrDocs(parseResults.hasOption("d"));
			
		} catch (Exception e) {
			throw new KgSolrException("Was not able to initialise the system", e);
		}
	}
	
	public static void main(String[] args) throws KgSolrException {
		
		initConfig(args, KgSolrConfig.class);
		
//		KgSolrConfig.init("/home/kay/Uni/Projects/SmartDataWeb/Code/SolrEntityLoader/resources/sdw/aksw/org/configGrid.json");
		//KgSolrConfig.init("/home/kay/Uni/Projects/SmartDataWeb/Code/SolrEntityLoader/resources/sdw/aksw/org/configDBpedia.json");
	
		SolrHandler solrHandler = new SolrHandler(KgSolrConfig.getInstance().getSolrUrl());
		
		try {
			if (KgSolrConfig.getInstance().deleteAllSolrDocs()) {
				// make sure nothing is in the store
				solrHandler.deleteAllDocuments();
				
			}
			
			KgJenaEntityToSolrImporter uriImporter = new KgJenaEntityToSolrImporter(solrHandler);
			
			uriImporter.transferData();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				solrHandler.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

}
