package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.SKOS;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper;
import org.aksw.sdw.ingestion.csv.utils.OntologyHandler;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;

/**
 * This method can be used to adjust the types of the object
 * 
 * @author kay
 *
 */
public class TypeNormalizer implements PropertyNormalizer {
	
	/** country to label to URI map */
	protected Map<String, Map<String, List<String>>> countryCompanyTypeUriMap = new HashMap<>();
	
	/** map which contains a papttern for each known label */
	protected Map<String, Pattern> labelPatternMap = new HashMap<>();
	
	final static Pattern cleanCompanyLabel0 = Pattern.compile("(?<=((^|\\W)[\\w]))\\s+(?=[\\w]($|\\W))");
	final static Pattern cleanCompanyLabel1 = Pattern.compile("((\\(|\\[).+(\\)|\\]))");
	
	/** mappings of company types from old dataset */
	static final Map<String, String> companyLabelMapping = getCompanyLabelMappings();
	static final Map<String, String> getCompanyLabelMappings() {
		Map<String, String> mappings = new HashMap<>();
		
		mappings.put("gmbh co", "gmbh & co kg");
		mappings.put("a g", "ag");
		mappings.put("mbh co", "mbh & co kg");
		mappings.put("ag co", "ag & co kg");
		//mappings.put("kapitalgesellschaft", "ag");
		mappings.put("gesellschaft mit beschränkter haftung", "gmbh");

		
		return mappings;
	}
	
	/** mapping of company names to types */
	static final Map<String, String> mapCompanyName2Type = getCompanyNameTypeMappings();
	static final Map<String, String> getCompanyNameTypeMappings() {
		Map<String, String> mappings = new HashMap<>();
		
		mappings.put("sparkasse", "aör");
		mappings.put("spk-mslo", "aör");
		
		return mappings;
	}
	
	public TypeNormalizer() {
		OntologyHandler ontologyHandler = PropertyNormalizerUtils.getInstance().getOntologyHandler();
		Map<String, Map<String, List<String>>> tmpCountryCompanyTypeUriMap =
				ontologyHandler.getChildClassNamesWithLabels(CorpDbpedia.prefixOntology + "OrgType");
        
        for (String languageCode : tmpCountryCompanyTypeUriMap.keySet()) {
        	Map<String, List<String>> companyTypeUriMap = tmpCountryCompanyTypeUriMap.get(languageCode);
        	List<String> sortedCompanyTypesList = new ArrayList<>(companyTypeUriMap.keySet());
        	
        	// sort list by length
        	Collections.sort(sortedCompanyTypesList, new Comparator<String>() {

				@Override
				public int compare(String o1, String o2) {
					return o2.length() - o1.length();
				}
			});
        	
        	Map<String, List<String>> sortedCompanyUriMap = this.countryCompanyTypeUriMap.get(languageCode);
        	if (null == sortedCompanyUriMap) {
        		sortedCompanyUriMap = new LinkedHashMap<>();
        		this.countryCompanyTypeUriMap.put(languageCode, sortedCompanyUriMap);
        	}
        	
        	for (String companyType : sortedCompanyTypesList) {
        		// get uri list
        		List<String> companyTypeUri = companyTypeUriMap.get(companyType);
        		
        		// add to sorted list
        		sortedCompanyUriMap.put(companyType, companyTypeUri);
        		
        		Pattern companyTypeLabelPattern = Pattern.compile("(?i)((\\s+|^)(" + companyType + "))$");
        		this.labelPatternMap.put(companyType, companyTypeLabelPattern);
        	}
        }
	}
	
	/**
	 * This method can be used to obtain company type labels
	 * 
	 * @param companyTypeLabel
	 * @param languageCode
	 * @return
	 */
	protected String getCompanyTypeUri(final String companyTypeLabel, final String languageCode) {
		if (null == companyTypeLabel || companyTypeLabel.isEmpty()) {
			return null;
		}
		
		Map<String, List<String>> languageUriMap = this.countryCompanyTypeUriMap.get(languageCode);
		if (null == languageUriMap) {
			return null;
		}
		
       	// remove white-spaces between capital letters
    	String cleanName = cleanCompanyLabel0.matcher(companyTypeLabel).replaceAll("");
    	// make all lower case and replace too many white spaces
    	
    	cleanName = cleanName.replaceAll("_", " ");
    	// remove brackets and their content
    	cleanName = cleanCompanyLabel1.matcher(cleanName).replaceAll("");
    	cleanName = (cleanName.contains("-->") ? cleanName.replace("-->", "") : cleanName);
    	cleanName = cleanName.toLowerCase().trim().replaceAll("\\s+", " ");
    	
		
		List<String> companyTypeUris = null;
		String companyTypeUri = null;
    	for (String companyType : languageUriMap.keySet()) {
    		
    		Pattern companyTypePattern = this.labelPatternMap.get(companyType);
    		if (companyTypePattern.matcher(companyTypeLabel).find()) {
    			
    			companyTypeUris = languageUriMap.get(companyType);
    			if (null != companyTypeUris) {
    				companyTypeUri = companyTypeUris.get(0);
    				break;
    			}    			
    		}
    	}
    	
    	if (null == companyTypeUri && "de".equals(languageCode)) {
    		String mappingTypeLabel = companyLabelMapping.get(companyTypeLabel);
    		if (null != mappingTypeLabel) {
    			return this.getCompanyTypeUri(mappingTypeLabel, languageCode);
    		}
    	}
    	
    	return companyTypeUri;
	}

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {		
		
		List<RDFNode> typeObjects = entity.getRdfObjects(RDF.type.getURI());
		boolean addTypes = false;
		if (null == typeObjects || typeObjects.isEmpty()) {
			addTypes = true;
			typeObjects = new ArrayList<>();
		}
		
		List<RDFNode> companyNameObjects = new ArrayList<>();
		List<RDFNode> preferedCompanyNames = entity.getRdfObjects(SKOS.prefLabel);
		if (null != preferedCompanyNames && false == preferedCompanyNames.isEmpty()) {			
			companyNameObjects.addAll(preferedCompanyNames);
		}
		
		List<RDFNode> alternativeCompanyNames = entity.getRdfObjects(SKOS.altLabel);
		if (null != alternativeCompanyNames && false == alternativeCompanyNames.isEmpty()) {			
			companyNameObjects.addAll(alternativeCompanyNames);
		}
		
		
		GeoNamesMapper geonamesMapper = PropertyNormalizerUtils.getInstance().getGeoNamesMapper();
		
		String normalizedCompanyType = null;
		RDFNode deTypeObject = null;
		RDFNode newTypeObject = null;
		boolean hasDBpediaCompany = false;
		boolean hasOrg = false;
		
		if (null != typeObjects && false == typeObjects.isEmpty()) {

			String prefixGcd = CorpDbpedia.prefix + "ontology/de/";
			
			Iterator<RDFNode> rdfObjectIterator = typeObjects.iterator();
			while (rdfObjectIterator.hasNext()) {
				if (null != newTypeObject) {
					break;
				}
				
				RDFNode typeObject = rdfObjectIterator.next();
				String predicateUri = typeObject.asResource().getURI();
				
				// find old GCD company type
				if (predicateUri.startsWith(CorpDbpedia.prefixOntology.substring(0, CorpDbpedia.prefixOntology.length() - 1) + "/de/")) {
					deTypeObject = typeObject;				
					
					// get company type name from uri and try to translate it
					normalizedCompanyType = predicateUri.substring(prefixGcd.length());
					String typeUri = this.getCompanyTypeUri(normalizedCompanyType, "de");
					if (null == typeUri) {
						// check if we have a company name match from our mappings
						for (RDFNode companyNameObject : companyNameObjects) {
							String companyName = companyNameObject.asLiteral().getLexicalForm().toLowerCase();
							typeUri = mapCompanyName2Type.get(companyName);
							if (null != typeUri) {
								typeUri = this.getCompanyTypeUri(typeUri, "de");
								break;
							}
						}
					}
					if (null != typeUri) {
						newTypeObject = new PropertyImpl(typeUri);
						break;
					}
				}
				
				// TODO: km create hashmap with types which should be deleted in general?
				if (predicateUri.equals("http://permid.org/ontology/organization/Organization")) {
					rdfObjectIterator.remove();
					continue;
				}
				
				if (predicateUri.equals("http://dbpedia.org/ontology/Company")) {
					hasDBpediaCompany = true;
				}
				
				if (predicateUri.equals("http://www.w3.org/ns/org#Organization")) {
					hasOrg = true;
				}
			}
		}
		
		if (false == hasDBpediaCompany) {
			typeObjects.add(new PropertyImpl("http://dbpedia.org/ontology/Company"));
			addTypes = true; // have to change type information
		}
		
		if (false == hasOrg) {
			typeObjects.add(new PropertyImpl("http://www.w3.org/ns/org#Organization"));
			addTypes = true; // have to change type information
		}
		
		// try out all the company names + its assigned language code
		if (null == newTypeObject) {
			
			/// TODO km: get type			
			for (RDFNode companyNameObject : companyNameObjects) {
				if (null == companyNameObject || false == companyNameObject instanceof Literal) {
					continue;
				}
				
				String companyName = companyNameObject.asLiteral().getLexicalForm();
				String objectLanguageCode = companyNameObject.asLiteral().getLanguage();
				
				// try out language code which comes from company name
				if (null != objectLanguageCode) {
					String typeUri = this.getCompanyTypeUri(companyName, objectLanguageCode.toLowerCase());
					if (null != typeUri) {
						newTypeObject = new ResourceImpl(typeUri);
						// found match
						break;
					}
				}
			}
		}
	
		// try to use country language codes + names to find solution
		if (null == newTypeObject) {	
			Collection<Entity> subEntities = entity.getSubEntities();
			for (Entity subEntity : subEntities) {
				if (null != newTypeObject) {
					break;					
				}
				
				List<RDFNode> geoNamesIds = subEntity.getRdfObjects(CorpDbpedia.countryGeoNamesId);
				if (null == geoNamesIds || geoNamesIds.isEmpty()) {
					continue;
				}
			
				String geoNamesId = geoNamesIds.get(0).asResource().getURI();
				
				for (RDFNode companyNameObject : companyNameObjects) {
					if (null == companyNameObject || false == companyNameObject.isLiteral()) {
						continue;
					}
					
					String companyName = companyNameObject.asLiteral().getLexicalForm();
				
						
					// otherwise try language codes coming from country
					List<String> languageCodes = geonamesMapper.getLanguageCodes(geoNamesId);
					if (null == languageCodes || languageCodes.isEmpty()) {
						continue;
					}
						
					for (String languageCode : languageCodes) {
						String typeUri = this.getCompanyTypeUri(companyName, languageCode.toLowerCase());
						if (null != typeUri) {
							newTypeObject = new ResourceImpl(typeUri);
							// found match
							break;
						}
					}				
				}				
			}
		}
		
		// remove old type
		if (null != deTypeObject) {
			typeObjects.remove(deTypeObject);
		}
		
		// check for IPO Event --> make sure it is not private
		if (null != newTypeObject && newTypeObject.asResource().getURI().toLowerCase().contains("privatelimited")) {
			for (Entity subEntity : entity.getSubEntities()) {
				List<RDFNode> ipoEvents = subEntity.getRdfObjects(CorpDbpedia.prefixOntology + "IPO");
				if (null != ipoEvents && false == ipoEvents.isEmpty()) {
					newTypeObject = new ResourceImpl(CorpDbpedia.prefixOntology + "PublicLimitedCompany");
					break;
				}
			}
		}
		
		// add new type
		if (null != newTypeObject) {
			Property orgType = new PropertyImpl(CorpDbpedia.orgType);
			
			List<String> parentClassNames = PropertyNormalizerUtils.getInstance().
					getOntologyHandler().getParentClassNames(newTypeObject.asResource().getURI());
	        for (String parentClassName : parentClassNames) { 
	        	
	        	String uriString = parentClassName;
	        	if (false == CorpDbpedia.orgType.toLowerCase().equals(uriString.toLowerCase()) &&
	        		(uriString.startsWith(CorpDbpedia.prefixOntology) || uriString.startsWith(W3COrg.prefix))) {
	        		ResourceImpl superClassUri = new ResourceImpl(uriString);
		        	entity.addTriple(orgType, superClassUri);
	        	}
	        }
	        
	        entity.addTriple(orgType, newTypeObject);
		} else {
			//System.err.println("Was not able to detect uri for: " + normalizedCompanyType +
			//				   " and company " + companyName);
		}
		
		if (addTypes) {
			// delete old entries
			entity.deleteProperty(RDF.type.getURI());
			
			// add types to entity
			Property type = new PropertyImpl(RDF.type.getURI());
			for (RDFNode typeObject : typeObjects) {
				entity.addTriple(type, typeObject);
			}
		}
	}
}
