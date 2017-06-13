package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;

public abstract class JsonPropertyNormalizer implements PropertyNormalizer {
	
	/** predicate prefix */
	final List<String> predicatePrefixes;	
	
	/**
	 * 
	 * @param predicatePrefix	Predicate prefix (e.g. labels.)
	 */
	public JsonPropertyNormalizer(final List<String> predicatePrefixes) {
		this.predicatePrefixes = predicatePrefixes;
	}
	
	/**
	 * 
	 * @param predicatePrefix	Predicate prefix (e.g. labels.)
	 */
	public JsonPropertyNormalizer(final String predicatePrefix) {
		this(Collections.singletonList(predicatePrefix));
	}
	
	/**
	 *  * This method can be used to obtain matching predicate strings which share a common predicate prefix.
	 * The method is intended for predicates of arrays: e.g. labels.0.name.
	 * 
	 * The returned map key is the "id/index" which comes after the prefix (e.g. 0 in the example above).
	 * The list of strings which is associated with the "id/index" contains all the full predicate names
	 * which are associated/grouped with the same "id/index" (e.g. lables.0.name)
	 * 
	 * @param entity		- entity instance
	 * @param stats			- statistics
	 * @param addressMap	- map with predicates grouped by index
	 * @throws IngestionException
	 */
	protected abstract void normalize(Entity entity, ConversionStats stats, final Map<String, List<MatchingPredicateStruct>> addressMap) throws IngestionException;

	
	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		Map<String, List<MatchingPredicateStruct>> predicateMap = this.getMatchingPredicates(entity);
		if (null == predicateMap || predicateMap.isEmpty()) {
			return;
		}
		
		this.normalize(entity, stats, predicateMap);
		
		this.deleteMatchingPredicates(entity, predicateMap);
	}
	
	
	/**
	 * This method can be used to obtain matching predicate strings which share a common predicate prefix.
	 * The method is intended for predicates of arrays: e.g. labels.0.name.
	 * 
	 * The returned map key is the "id/index" which comes after the prefix (e.g. 0 in the example above).
	 * The list of strings which is associated with the "id/index" contains all the full predicate names
	 * which are associated/grouped with the same "id/index" (e.g. lables.0.name)
	 *	 
	 * @param entity - entity instance
	 * @return map as described above (empty if no matches where found)
	 */
	protected Map<String, List<MatchingPredicateStruct>> getMatchingPredicates(final Entity entity) {
		Map<String, List<MatchingPredicateStruct>> predicateMap = new LinkedHashMap<>();
		
		if (null == this.predicatePrefixes ||
			this.predicatePrefixes.isEmpty() ||
			null == entity || entity.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Collection<String> predicateStrings = entity.getPredicates();
		
		for (String predicateString : predicateStrings) {
			
			for (String predicatePrefix : this.predicatePrefixes) {
				boolean isArray = predicatePrefix.endsWith(".");
				
				if (isArray && predicateString.startsWith(predicatePrefix)) {
					
					int prefixLength = predicatePrefix.length();
					int dotIndex = predicateString.indexOf(".", prefixLength);
					
					String matchingPrefix;
					if (0 > dotIndex) {
						dotIndex = predicateString.length();
						String indexString = predicateString.substring(prefixLength);
						matchingPrefix = new StringBuffer().append(predicatePrefix).append(indexString).toString();
					} else {
						String indexString = predicateString.substring(prefixLength, dotIndex);
						matchingPrefix = new StringBuffer().append(predicatePrefix).append(indexString).append(".").toString();
					}
					
					List<MatchingPredicateStruct> addressInfoPRedicates = predicateMap.get(matchingPrefix);
					if (null == addressInfoPRedicates) {
						addressInfoPRedicates = new ArrayList<>();
						predicateMap.put(matchingPrefix, addressInfoPRedicates);
					}
					
					MatchingPredicateStruct matchingPredicate = new MatchingPredicateStruct();					
					matchingPredicate.fullPredicate = predicateString;
					
					matchingPredicate.shortenedPredicate = predicateString.substring(matchingPrefix.length());
					addressInfoPRedicates.add(matchingPredicate);
					break;
				} else if (false == isArray && predicateString.equals(predicatePrefix)) {
					List<MatchingPredicateStruct> addressInfoPRedicates = predicateMap.get(predicatePrefix);
					if (null == addressInfoPRedicates) {
						addressInfoPRedicates = new ArrayList<>();
						predicateMap.put(predicatePrefix, addressInfoPRedicates);
					}
					
					MatchingPredicateStruct matchingPredicate = new MatchingPredicateStruct();					
					matchingPredicate.fullPredicate = predicateString;
					matchingPredicate.shortenedPredicate = predicatePrefix;
					addressInfoPRedicates.add(matchingPredicate);
					break;
				}
			}
		}
		
		return predicateMap;
	}
	
	/**
	 * This method can be used to delete matching predicate strings from the
	 * registered entity.
	 * 
	 */
	protected void deleteMatchingPredicates(final Entity entity, final Map<String, List<MatchingPredicateStruct>> predicateMap) {
		if (predicateMap.isEmpty()) {
			return;
		}
		
		for (String index : predicateMap.keySet()) {
			List<MatchingPredicateStruct> matchingPredicates = predicateMap.get(index);
			
			for (MatchingPredicateStruct matchingPredicate : matchingPredicates) {
				entity.deleteProperty(matchingPredicate.fullPredicate);
			}
		}
	}
	
	public static class MatchingPredicateStruct {
		public String fullPredicate;
		public String shortenedPredicate;
	}

}
