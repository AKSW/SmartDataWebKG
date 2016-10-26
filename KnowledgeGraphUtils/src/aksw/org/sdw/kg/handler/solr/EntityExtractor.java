package aksw.org.sdw.kg.handler.solr;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import aksw.org.kg.KgException;
import aksw.org.sdw.kg.handler.sparql.SparqlHandler;

/**
 * This class can be used to extract entity information from SDW KG backend.
 * 
 * @author kay
 *
 */
public class EntityExtractor {

        public static void main(String[] args) throws IOException, KgException {
                System.out.println("Entity Extractor");


                String solrUrl = args[0];
                System.out.println("Apache Solr instance: " + solrUrl);

                String solrQuery = args[1];
                System.out.println("Solr Query: " + solrQuery);

                String sparqlBackend = args[2];
                System.out.println("SPARQL Backend: " + sparqlBackend);
                
                String outputFileName = args[3];
                System.out.println("Output file: " + outputFileName);

                SolrHandler solrHandler = new SolrHandler(solrUrl);
                
                boolean fileExists = false;
                File outputFile = new File(outputFileName);
                if (outputFile.exists()) {
                	fileExists = true;
                }
                
		// open file and if exists --> append
                BufferedWriter output = new BufferedWriter(new FileWriter(outputFile, true));
                if (false == fileExists) {
			// write out headline --> only when the file did not exist before!
                	output.append("uri\tlabel\thomepage\n");
                }

                SparqlHandler sparqlHandler = new SparqlHandler(sparqlBackend, null);

                try {
                        List<KgSolrResultDocument> searchResults = solrHandler.executeQuery(solrQuery, null);
                        System.out.println("Number of results: " + searchResults.size());
                        
                        Map<String, String> varName2UriMap = new HashMap<>();
                        Map<String, List<String>> uri2Homepages = new HashMap<>();
                        Map<String, List<String>> uri2Labels = new HashMap<>();
                        
                        int index = 0;
                        Iterator<KgSolrResultDocument> iterator = searchResults.iterator();
                        
                        int round = 1;
                        int offset = 20;
                        
                        while (iterator.hasNext()) {
                        	
                        	// will store the SPARQL search query
                            StringBuffer query = new StringBuffer();
                            query.append("SELECT DISTINCT * WHERE { ");
                        	
                        	// computes maximum iteration count
                        	int maxRound = offset * round;
	                        for (int i = offset * (round - 1); i < maxRound && iterator.hasNext(); ++i) {
	
	                            KgSolrResultDocument result = iterator.next();
	                            System.out.println(++index + ". Doc: \n" + result);
	
	                            String uri = result.getFieldValueAsString("id");
	                            String varName = "homepage" + index;
	                            
	                            List<String> labels = result.getFieldValueAsStringList("nameEn");
	                            if (null != labels && false == labels.isEmpty()) {
	                            	uri2Labels.put(uri, labels);
	                            }
	                            
	                            // helps to perform a mapping
	                            varName2UriMap.put(varName, uri);
	                            
	                            query.append("\n\tOPTIONAL { <" + uri + "> <http://xmlns.com/foaf/0.1/homepage> ?" + varName + " }");                           
	                            
	
	                            List<String> sameAsLinks = result.getFieldValueAsStringList("sameAs");
	                            if (null != sameAsLinks &&  false == sameAsLinks.isEmpty()) {
	                                    for (String sameAsLink : sameAsLinks) {
	                                            // only look within our own dataset
	                                            if (sameAsLink.startsWith("http://corp.dbpedia.org")) {
	                                                    query.append("\n\tOPTIONAL { <" + sameAsLink + "> <http://xmlns.com/foaf/0.1/homepage> ?" + varName + " }");
	                                            }
	                                    }
	                            }
	                        }
	
		                    query.append("\n}");
		
	
	                        QueryExecution queryExec = sparqlHandler.createQueryExecuter(query.toString());
	
	                        ResultSet sparqlResults = queryExec.execSelect();
	                        System.out.println("Got results: " + sparqlResults.getRowNumber());
	
	                        int count = 0;
	                        while (sparqlResults.hasNext()) {
	                                QuerySolution sparqlResult = sparqlResults.next();
	
	                                Iterator<String> varNames = sparqlResult.varNames();
	                                while (varNames.hasNext()) {
	                                        String varName = varNames.next();
	                                        String uri = varName2UriMap.get(varName);
	                                        
	                                        List<String> homepages = uri2Homepages.get(uri);
	                                        if (null == homepages) {
	                                        	homepages = new ArrayList<>();
	                                        	uri2Homepages.put(uri, homepages);
	                                        }
	                                        
	                                        homepages.add(sparqlResult.get(varName).toString());
	                                        ++count;
	                                }
	                        }
	                        
	                        System.out.println("Got: " + count);

	                        queryExec.close();
                        
                        }                        
                        
                        int finalCount = 0;
//                        System.out.println("index\turi\tlabel\thomepage");                        
                        for (String uri : uri2Labels.keySet()) {
                                output.append(uri + "\t" + uri2Labels.get(uri)
                                				   + (null == uri2Homepages.get(uri) ? "\t" : "\t" + uri2Homepages.get(uri)));
				output.append("\n"); // add a new line
                        }

                        output.flush();
                        System.out.println("Homepage count: " + uri2Homepages.size());

                        
                } finally {
                        solrHandler.close();
                        output.close();
                }
        }
}

