package org.aksw.sdw.ingestion.csv.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;

//import org.apache.jena.ontology.OntClass;
//import org.apache.jena.ontology.OntModel;
//import org.apache.jena.ontology.OntModelSpec;
//import org.apache.jena.ontology.OntProperty;
//import org.apache.jena.rdf.model.ModelFactory;
//import org.apache.jena.rdf.model.RDFNode;
//import org.apache.jena.util.iterator.ExtendedIterator;

import jersey.repackaged.com.google.common.cache.Cache;
import jersey.repackaged.com.google.common.cache.CacheBuilder;

/**
 * This class can be used to deal with ontology related questions
 * 
 * @author kay
 *
 */
public class OntologyHandler {
	
	protected static OntologyHandler ontologyHandler = null;
	
	protected static List<String> fileList = null;
	
	/** owl model which loads company ontology */
	final protected OntModel owlModel;
	
	/**
	 * This is a country object cache
	 */
	final static Cache<String, Object> parentCache =
			CacheBuilder.newBuilder()
			.concurrencyLevel(8)
			.maximumSize(15000)
			.build();
	
	
	/**
	 * 
	 * @param fileList List to ontology files
	 */
	private OntologyHandler(final List<String> fileList) {
		this.owlModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
		
		for (String filePath : fileList) {
			InputStream stream = OntologyHandler.class.getClassLoader().getResourceAsStream(filePath);
			if (null == stream) {
				File file = new File(filePath);
				if (false == file.exists() && false == file.isFile()) {
					throw new RuntimeException("Was not able to load file: " + filePath);
				}
				
				try {
					stream = Files.newInputStream(Paths.get(file.getAbsolutePath()));
				} catch (IOException e) {
					throw new RuntimeException("Was not able to load file: " + filePath);
				}
			}
			
			this.owlModel.read(stream, "RDF/XML");
		}
	}
	
	/**
	 * This method can be used to get all children class names of the
	 * pass in ontology class
	 * 
	 * @param ontologyClassName
	 * @return list of ontology children class names
	 */
	public List<String> getChildClassNames(final String ontologyClassName) {
		if (null == ontologyClassName) {
			return Collections.emptyList();
		}
		try {
			@SuppressWarnings("unchecked")
			List<String> classNames = (List<String>) parentCache.get(ontologyClassName, new Callable<Object>() {
	
				@Override
				public Object call() throws Exception {
					OntClass event = owlModel.getOntClass(ontologyClassName);
					if (null == event) {
						return Collections.emptyList();
					}

					List<String> classNames = new ArrayList<>();
					ExtendedIterator<OntClass> it = event.listSubClasses(false);
			        while (it.hasNext()) {  
			          OntClass ontologyClass = (OntClass) it.next();
			          
			          classNames.add(ontologyClass.getURI());
			        }
			        
			        it.close();
			        
			        return classNames;
				}
			});
			
			return classNames;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		
	}
	

	/**
	 * This method can be used to get all children class names of the
	 * pass in ontology class with all their labels
	 * 
	 * @param ontologyClassName
	 * @return Map of class name of map of language code and its labels
	 */
	public Map<String, Map<String, List<String>>> getChildClassNamesWithLabels(final String ontologyClassName) {
		if (null == ontologyClassName) {
			return Collections.emptyMap();
		}
		
		OntClass event = this.owlModel.getOntClass(ontologyClassName);
		if (null == event) {
			return Collections.emptyMap();
		}

		Map<String, Map<String, List<String>>> tmpCountryCompanyTypeUriMap = new HashMap<>();
		
        for (Iterator<OntClass> it = event.listSubClasses(false); it.hasNext(); ) {  
          OntClass ontologyClass = (OntClass) it.next();
          ExtendedIterator<RDFNode> labels = ontologyClass.listLabels(null);
          
          while (labels.hasNext()) {
        	  RDFNode next = labels.next();
        	      	  
        	  String label = next.asLiteral().getValue().toString().toLowerCase();
        	  String language = next.asLiteral().getLanguage();
        	  
        	  Map<String, List<String>> languageMap = tmpCountryCompanyTypeUriMap.get(language);
        	  if (null == languageMap) {
        		  languageMap = new HashMap<>();
        		  tmpCountryCompanyTypeUriMap.put(language, languageMap);
        	  }        	  
   	  
        	  List<String> uriLanguageList =  languageMap.get(label);
        	  if (null == uriLanguageList) {
        		  uriLanguageList = new ArrayList<>();
        		  languageMap.put(label, uriLanguageList);
        	  }
        	  
        	  uriLanguageList.add(ontologyClass.getURI());
          }
          
          labels.close();
        }
        
        return tmpCountryCompanyTypeUriMap;
	}
	
	/**
	 * This method can be used to return parent class names
	 * of the passed in ontology class name
	 * 
	 * @param ontologyClassName
	 * @return list of parent class names
	 */
	public List<String> getParentClassNames(final String ontologyClassName) {
		if (null == ontologyClassName) {
			return Collections.emptyList();
		}
		
		try {
			@SuppressWarnings("unchecked")
			List<String> classNames = (List<String>) parentCache.get(ontologyClassName, new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					OntClass event = owlModel.getOntClass(ontologyClassName);
					if (null == event) {
						return Collections.emptyList();
					}
	
					List<String> classNames = new ArrayList<>();
					ExtendedIterator<OntClass> it = event.listSuperClasses();
			        while(it.hasNext()) { 
			        	OntClass superClass = it.next();
	
			        	String uriString = superClass.getURI();
			        	classNames.add(uriString);        	
			        }
			        
			        it.close();
			        
			        return classNames;
				}
			});
			
			return classNames;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * This method can be used to return parent class names
	 * of the passed in ontology class name
	 * 
	 * @param ontologyClassName
	 * @return list of parent class names
	 */
	public List<String> getParentPropertyNames(final String propertyName) {
		if (null == propertyName) {
			return Collections.emptyList();
		}
		
		try {
			@SuppressWarnings("unchecked")
			List<String> propertyNames = (List<String>) parentCache.get(propertyName, new Callable<Object>() {
	
				@Override
				public Object call() throws Exception {
					OntProperty property = owlModel.getOntProperty(propertyName);
					if (null == property) {
						return Collections.emptyList();
					}
	
					List<String> propertyNames = new ArrayList<>();
					ExtendedIterator<? extends OntProperty> it = property.listSuperProperties();
					while (it.hasNext()) { 
			        	OntProperty superProperty = it.next();
	
			        	String uriString = superProperty.getURI();
			        	propertyNames.add(uriString);        	
			        }
			        
			        it.close();
			        
			        return propertyNames;
				}
			});
			
			return propertyNames;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void init(final List<String> fileList) {
		if (null == fileList) {
			return;
		}
		
		OntologyHandler.fileList = fileList;
	}
	
	public static OntologyHandler getInstance() {
		if (null == ontologyHandler) {
			if (null == fileList) {
				throw new RuntimeException("File list is not initialised");
			}
			
			ontologyHandler = new OntologyHandler(fileList);
		}
		
		return ontologyHandler;
		
	}

}
