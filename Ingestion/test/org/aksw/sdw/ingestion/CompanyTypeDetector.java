//package org.aksw.sdw.ingestion;
//
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicLong;
//
//import org.aksw.sdw.ingestion.csv.IngestionException;
//import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
//import org.aksw.sdw.ingestion.csv.normalizer.PropertyNormalizerUtils;
//import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper;
//
//import com.hp.hpl.jena.ontology.OntClass;
//import com.hp.hpl.jena.ontology.OntModel;
//import com.hp.hpl.jena.ontology.OntModelSpec;
//import com.hp.hpl.jena.query.Query;
//import com.hp.hpl.jena.query.QueryExecution;
//import com.hp.hpl.jena.query.QueryExecutionFactory;
//import com.hp.hpl.jena.query.QueryFactory;
//import com.hp.hpl.jena.query.QuerySolution;
//import com.hp.hpl.jena.query.ResultSet;
//import com.hp.hpl.jena.rdf.model.Literal;
//import com.hp.hpl.jena.rdf.model.Model;
//import com.hp.hpl.jena.rdf.model.ModelFactory;
//import com.hp.hpl.jena.rdf.model.RDFNode;
//import com.hp.hpl.jena.util.iterator.ExtendedIterator;
//import com.hp.hpl.jena.vocabulary.RDFS;
//
///**
// * This class can be used to detect the company type of a given company
// * 
// * @author kay
// *
// */
//public class CompanyTypeDetector {
//	
//	public static void main(String[] args) throws IngestionException {
//		System.out.println("Test");
//		
////		String name0 = "CONSERVATION INVESTMENT FUND L L C";
////		String cleanName0 = name0.replaceAll("(?<=((^|\\W)[\\w]))\\s+(?=[\\w]($|\\W))", "");
////		System.out.println("clean: " + cleanName0);
////		
////		if (true) {
////			return;
////		}
//		
//		OntModel owlModel =ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
//		
//		InputStream stream = CompanyTypeDetector.class.getClassLoader().getResourceAsStream("org.owl");
//		owlModel.read(stream, "RDF/XML");
//		
//		InputStream streamTypes = CompanyTypeDetector.class.getClassLoader().getResourceAsStream("CompanyTypes.owl");
//		owlModel.read(streamTypes, "RDF/XML");
//		
//		if (owlModel.isEmpty()) {
//			System.err.println("Empty model");
//		} else {
//			System.out.println("Got model");
//		}
//		
//		
//		OntClass event = owlModel.getOntClass(CorpDbpedia.company);	    
//
//		Map<String, List<String>> companyTypeUriMap = new HashMap<>();
//		Map<String, Map<String, List<String>>> countryCompanyTypeUriMap = new HashMap<>();
//		
//        for (Iterator<OntClass> i = event.listSubClasses(false); i.hasNext(); ) {  
//          OntClass c = (OntClass) i.next();
//          System.out.println( c.getURI() );
//          ExtendedIterator<RDFNode> labels = c.listLabels(null);
//          
//          while (labels.hasNext()) {
//        	  RDFNode next = labels.next();
//        	  //System.out.println(c.getURI() + " : " + next.asLiteral().getValue() + " (" + next.asLiteral().getLanguage() + ")");
//        	      	  
//        	  String label = next.asLiteral().getValue().toString().toLowerCase();
//        	  String language = next.asLiteral().getLanguage();
//        	  
//        	  Map<String, List<String>> languageMap = countryCompanyTypeUriMap.get(language);
//        	  if (null == languageMap) {
//        		  languageMap = new HashMap<>();
//        		  countryCompanyTypeUriMap.put(language, languageMap);
//        	  }
//        	  
//        	  List<String> uriList = companyTypeUriMap.get(label);
//        	  if (null == uriList) {
//        		  uriList = new ArrayList<>();
//        		  companyTypeUriMap.put(label, uriList);
//        	  }
//        	  
//        	  uriList.add(c.getURI());
//        	  
//        	  List<String> uriLanguageList =  languageMap.get(label);
//        	  if (null == uriLanguageList) {
//        		  uriLanguageList = new ArrayList<>();
//        		  languageMap.put(label, uriLanguageList);
//        	  }
//        	  
//        	  uriLanguageList.add(c.getURI());
//        	  
//        	  
//          }          
//        }
//        
//        List<String> names = Arrays.asList("blabla", "InfAI e.V.", "GmbH", "Gesellschaft mit  beschränkter Haftung", "Siemens AG", "IBM Corp.", "IBM Corporation");
//        List<String> languages = Arrays.asList("de", "de", "de", "de", "de", "en", "en");
//        for (int i = 0; i < names.size(); ++i) {
//        	String name = names.get(i);
//        	
//        	String cleanName = name.toLowerCase().trim().replaceAll("\\s+", " ");
//        	
//        	List<String> uri = null;
//        	for (String companyType : companyTypeUriMap.keySet()) {
//        		if (cleanName.endsWith(companyType)) {
//        			uri = companyTypeUriMap.get(companyType);
//        			break;
//        		}
//        	}
//        	
//        	if (null == uri) {
//        		System.err.println("Was not able to find company type of entity: " + cleanName);
//        		continue;
//        	}
//        	
//        	System.out.println("DEFAULT: " + uri);
//        	
//        	String languageCode = languages.get(i);
//        	Map<String, List<String>> languageUriMap = countryCompanyTypeUriMap.get(languageCode);
//        	for (String companyType : languageUriMap.keySet()) {
//        		if (cleanName.endsWith(companyType)) {
//        			uri = languageUriMap.get(companyType);
//        			break;
//        		}
//        	}
//        	
//        	System.out.println("LC: " + uri);
//        	
//        }
//        
//        System.out.println("Start reading input file");
//        Model dataSet = ModelFactory.createDefaultModel();
//        dataSet.read("/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Server/16_01_22/GCD/16_01_19_gcd_1.nt", "N-TRIPLES");
//        //dataSet.read("/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Server/16_01_22/PermId/NT/16_01_20_PermId_9.nt");
//        System.out.println("Finished reading input file");
//        
//        dataSet.isEmpty();
//        
//        String queryString = "SELECT ?s ?l ?countryUri WHERE { ?s <http://www.w3.org/2004/02/skos/core#prefLabel> ?l ."
//        					 + " ?s <http://www.w3.org/2006/vcard/ns#hasAddress> ?b . ?b <http://www.w3.org/2006/vcard/ns#country-name> ?countryUri . }";
//        Query query = QueryFactory.create(queryString);
//        
//        QueryExecution queryExec = QueryExecutionFactory.create(query, dataSet);
//        ResultSet result = queryExec.execSelect();
//        
//        System.out.println("Run query: " + queryString);
//        
//        Map<String, AtomicLong> stats = new HashMap<>();
//        
//        long error = 0;
//        while (result.hasNext()) {
//        	QuerySolution triple = result.next();
//        	
//        	String subject = triple.get("s").asNode().getURI();
//        	Literal label = triple.get("l").asLiteral();
//        	
//        	String countryUri = triple.get("countryUri").toString();
//        	if (null == countryUri || false == countryUri.contains("http")) {
//        		continue;
//        	}
//        	
//        	List<String> languageCodes = PropertyNormalizerUtils.getInstance().geoNamesMapper.getLanguageCodes(countryUri);
//        	
//        	
//        	String name = label.getValue().toString();
//        	
//        	
//        	// remove white-spaces between capital letters
//        	String cleanName = name.replaceAll("(?<=((^|\\W)[\\w]))\\s+(?=[\\w]($|\\W))", "");
//        	// make all lower case and replace too many white spaces
//        	
//        	cleanName = cleanName.replaceAll("[()]", " ");
//        	cleanName = cleanName.toLowerCase().trim().replaceAll("\\s+", " ");
//
//        	
//        	List<String> companyTypeUri = null;
//        	if (null != languageCodes) {
//        		
//        		for (String languageCode : languageCodes) {
//        			
//	        		Map<String, List<String>> languageUriMap = countryCompanyTypeUriMap.get(languageCode);
//	        		if (null == languageUriMap) {
//	        			continue;
//	        		}
//	        		
//	            	for (String companyType : languageUriMap.keySet()) {
//	            		if (cleanName.endsWith(" " + companyType)) {
//	            			companyTypeUri = languageUriMap.get(companyType);
//	            			break;
//	            		}
//	            		
//	            		if (cleanName.startsWith(companyType + " ")) {
//	            			companyTypeUri = languageUriMap.get(companyType);
//	            			break;
//	            		}
//	            	}
//	            	
//	            	if (null != companyTypeUri) {
//	            		break;
//	            	}
//        		}
//        	}
//        	
//        	String languageCode = label.getLanguage();
//        	
//        	if (null == companyTypeUri && null != languageCode) {
//        		Map<String, List<String>> languageUriMap = countryCompanyTypeUriMap.get(languageCode);
//            	for (String companyType : languageUriMap.keySet()) {
//            		if (cleanName.endsWith(" " + companyType)) {
//            			companyTypeUri = languageUriMap.get(companyType);
//            			break;
//            		}
//            	}
//        	} else if (null == companyTypeUri) {
//        		for (String companyType : companyTypeUriMap.keySet()) {
//            		if (cleanName.endsWith(" " + companyType)) {
//            			companyTypeUri = companyTypeUriMap.get(companyType);
//            			break;
//            		}
//            	}
//        	}
//        	
//        	if (null == companyTypeUri) {
//        		System.err.println("Was not able to find type for company: '" + name + "'/'" + cleanName + "' (" + languageCode + ")");
//        		++error;
//        		continue;
//        	}
//        	
//        	String key = null == languageCode ? companyTypeUri.toString() : companyTypeUri.toString() + "(" + languageCode + ")";
//        	AtomicLong count = stats.get(key);
//        	if (null == count) {
//        		count = new AtomicLong(0);
//        		stats.put(key, count);
//        	}
//        	
//        	count.incrementAndGet();
//        }
//        
//        dataSet.close();
//        
//        owlModel.close();
//        
//        long all = 0;
//        for (String uri : stats.keySet()) {
//        	AtomicLong count = stats.get(uri);
//        	all += count.get();
//        	
//        	System.out.println("Found: " + uri + " many times: " + count.get());        	
//        }
//        
//        System.err.println("Number of errors: " + error + "/" + (error + all));
//        System.out.println("Number of matches: " + all + "/" + (error + all));
//        
//        System.out.println("Number of matches: " + (all / (double) (error + all) * 100) + "%");
//        
//	}
//
//}
