package sdw.aksw.org.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hp.hpl.jena.rdf.model.RDFNode;

import aksw.org.sdw.kg.handler.solr.KgSolrException;
import sdw.aksw.org.sparql.DefaultMapping;
import sdw.aksw.org.sparql.Solr2SparqlMappingInterface;

public class KgSolrConfig extends KgSolrConfigBase {
	
	/** config instance */
	static KgSolrConfig config = null;
	
	protected String solrUrl;
	protected String sparqlUrl;
	protected List<String> graphNames;
	protected String sourceType;
	protected List<KgSolrMapping> mappings;
	protected List<String> restrictions;
	
	protected KgSolrConfig() {
		
	}
	
	static public KgSolrConfig getInstance() {
		return KgSolrConfigBase.getInstance(KgSolrConfig.class);
	}
	
	@Override
	public void init(final KgSolrConfigBase configInstanceBase, final JsonReader jsonReader) throws KgSolrException {
		if (null == configInstanceBase ||
			false == KgSolrConfig.class.isInstance(configInstanceBase) ||
			null == jsonReader) {
			return;
		}
		
		try {			
			JsonElement element = jsonReader.getJson();
			if (null == element || element.isJsonNull()) {
				return;
			}
			
			KgSolrConfig configInstance = (KgSolrConfig) configInstanceBase;
			
			if (element.isJsonObject()) {
				JsonObject jsonObject = element.getAsJsonObject();
				configInstance.solrUrl = JsonReader.getElementString(jsonObject, "solrUrl", true);
				configInstance.sparqlUrl = JsonReader.getElementString(jsonObject, "sparqlUrl", true);
				configInstance.graphNames = JsonReader.getElementStringArray(jsonObject, "graphNames", true);
								
				configInstance.mappings = configInstance.createMappings(jsonObject);	
				configInstance.restrictions = JsonReader.getElementStringArray(jsonObject, "kgRestrictions", false);
			}
		} catch (Exception e) {
			throw new KgSolrException("Was not able to init config", e);
		}
	}
	
	/**
	 * This method can be used to read mappings from JSON config file
	 * 
	 * @param jsonObject
	 * @return
	 * @throws KgSolrException
	 */
	protected List<KgSolrMapping> createMappings(final JsonObject jsonObject) throws KgSolrException {
		JsonArray mappingArray = jsonObject.get("mappings").getAsJsonArray();
		
		List<KgSolrMapping> mappings = new ArrayList<>();
		for (int i = 0; i < mappingArray.size(); ++i) {
			
			JsonObject mappingObject = mappingArray.get(i).getAsJsonObject();
			
			List<String> requiredPredicates =
					JsonReader.getElementStringArray(mappingObject, "kgRequiredPredicateNames", false);
			List<String> kgOptionalPredicateNames =
					JsonReader.getElementStringArray(mappingObject, "kgOptionalPredicateNames", false);
			
			if ((null == requiredPredicates || requiredPredicates.isEmpty()) &&
				(null == kgOptionalPredicateNames || kgOptionalPredicateNames.isEmpty())) {
				continue;
			}
			
		
			// get variable name
			String kgVariableName = JsonReader.getElementString(mappingObject, "kgVariableName", true);
			
			JsonArray solrMappingGroups = mappingObject.get("solrMappingGroups").getAsJsonArray();
			
			List<String> solrFieldNames = new ArrayList<>();
			List<Pattern> matchPatterns = new ArrayList<>();
			
			for (int ii = 0; ii < solrMappingGroups.size(); ++ii) {
				JsonObject solrMappingInfoObject = solrMappingGroups.get(ii).getAsJsonObject();			
				
				String solrFieldName = JsonReader.getElementString(solrMappingInfoObject, "solrFieldName", true);
				solrFieldNames.add(solrFieldName);
			
				String matchPatternString = JsonReader.getElementString(solrMappingInfoObject, "matchPattern", false);
				Pattern matchPattern = (null != matchPatternString) ? Pattern.compile(matchPatternString) : null;
				matchPatterns.add(matchPattern);
			}
			
			String mappingClass = JsonReader.getElementString(mappingObject, "sparql2SolrMappingClass", false);
			
			KgSolrMapping mapping = new KgSolrMappingRegex(
					requiredPredicates, kgOptionalPredicateNames,
					kgVariableName, solrFieldNames, matchPatterns, mappingClass);
			
			mappings.add(mapping);
		}
		
		if (mappings.isEmpty()) {
			throw new KgSolrException("No mappings found in config file");
		}
		
		return mappings;
	}
	
	public String getSolrUrl() {
		return this.solrUrl;
	}

	public String getSparqlUrl() {
		return this.sparqlUrl;
	}

	public List<String> getGraphNames() {
		return this.graphNames;
	}

	public String getSourceType() {
		return this.sourceType;
	}

	public List<KgSolrMapping> getMappings() {
		return this.mappings;
	}
	
	public List<String> getRestrictions() {
		return this.restrictions;
	}
	
	/**
	 * This class defines a simple mapping between
	 * RDF entity predicates and SOLR fields
	 */
	public static abstract class KgSolrMapping {
		/** name of required KG predicates */
		public final List<String> kgRequiredPredicateNames;
		/** name of optional KG predicates */
		public final List<String> kgOptionalPredicateNames;
		
		/** variable name of SPARQL variable */
		public final String kgVariableName;
		/** solr field name which should store data */
		public final List<String> solrFieldNames;
		
		public final int solrGroupSize;
		
		public final Solr2SparqlMappingInterface mappingInstance;
		
		public KgSolrMapping(final List<String> kgRequiredPredicateNames,
							 final List<String> kgOptionalPredicateNames,
							 final String kgVariableName, final List<String> solrFieldNames,
							 final String sparqlMappingClass) {
			this.kgRequiredPredicateNames = kgRequiredPredicateNames;
			this.kgOptionalPredicateNames = kgOptionalPredicateNames;
			
			this.kgVariableName = kgVariableName;
			this.solrFieldNames = solrFieldNames;
			this.solrGroupSize = solrFieldNames.size();
			
			if (null == sparqlMappingClass) {
				this.mappingInstance = new DefaultMapping();
			} else {
				
				try {
					Class<?> classInstance = Class.forName(sparqlMappingClass);
					this.mappingInstance = (Solr2SparqlMappingInterface) classInstance.newInstance();
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
					throw new RuntimeException("Was not able to create mapping class", e);
				}
			}			
		}
		
		/**
		 * This method can be used to chech whether an RDFNode matches
		 * this type
		 * 
		 * @param matchingNode	- RDF node which content should be checked
		 * @param matchingGroup	- specifies the matching group which should be checked
		 * @return
		 */
		public abstract boolean matches(final RDFNode matchingNode, final int matchingGroup);
	}
	
	/**
	 * This class can be used to define KG to SOLR mapping.
	 * The machPattern is used to filter out incompatible results.
	 * 
	 * @author kay
	 *
	 */
	public static class KgSolrMappingRegex extends KgSolrMapping {
		
		/** match pattern */
		final List<Pattern> matchPattern;

		public KgSolrMappingRegex(List<String> kgRequiredPredicateNames, List<String> kgOptionalPredicateNames,
				final String kgVariableName, final List<String> solrFieldName,
				final List<Pattern> matchPattern, final String sparqlMappingClass) {
			super(kgRequiredPredicateNames, kgOptionalPredicateNames, kgVariableName, solrFieldName, sparqlMappingClass);
			
			this.matchPattern = matchPattern;
		}

		@Override
		public boolean matches(RDFNode matchingNode, final int matchingGroup) {
			if (null == matchingNode) {
				return false;
			}
			
			String literalString = matchingNode.toString();
			if (null == literalString) {
				return false;
			}
			
			// it is not possible to decide whether it matches or not
			// --> make it true, since it exists
			if (null == this.matchPattern) {
				return true;
			}
			
			if (matchingGroup >= this.matchPattern.size()) {
				return false;
			}
			
			Pattern matchingPattern = this.matchPattern.get(matchingGroup);
			if (null == matchingPattern) {
				return true;
			}
			
			return matchingPattern.matcher(literalString).matches();
		}	
	}	
}
