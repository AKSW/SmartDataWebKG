package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;
import java.util.Map;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.SKOS;
import org.apache.jena.rdf.model.RDFNode;

/**
 * This class can be used to extract labels from the original dataset
 * and to store them as skos:altLabel
 * 
 * @author kay
 *
 */
public class GridLabels extends JsonPropertyNormalizer {
	
	public GridLabels() {
		super("labels.");
	}


	@Override
	public void normalize(Entity entity, ConversionStats stats, final Map<String, List<MatchingPredicateStruct>> labelPredicateMap) throws IngestionException {
		if (null == labelPredicateMap || labelPredicateMap.isEmpty()) {
			return;
		}
		
		for (String index : labelPredicateMap.keySet()) {
			List<MatchingPredicateStruct> matchingPredicates = labelPredicateMap.get(index);
						
			String label = null;
			String languageCode = null;
			for (MatchingPredicateStruct matchingPredicate : matchingPredicates) {
				
				String shortPrefix = matchingPredicate.shortenedPredicate;
				if (shortPrefix.equals("label")) {
					List<RDFNode> labelObjects = entity.getRdfObjects(matchingPredicate.fullPredicate);
					if (null == labelObjects || labelObjects.isEmpty()) {
						continue;
					}
					
					if (labelObjects.get(0).isLiteral()) {
						label = labelObjects.get(0).asLiteral().getLexicalForm();
					}
					
				} else if (shortPrefix.equals("iso639")) {
					List<RDFNode> labelObjects = entity.getRdfObjects(matchingPredicate.fullPredicate);
					if (null == labelObjects || labelObjects.isEmpty()) {
						continue;
					}
					
					languageCode = labelObjects.get(0).asLiteral().getLexicalForm();
				}
				
				/// TODO check whether it works without correction --> Should be handled by Jena
//				String cleanLabel = RdfObjectLiteral.correctObjectString(label);				
				if (null != label && false == label.isEmpty() && null != languageCode) {					
					entity.addTripleWithLiteral(SKOS.altLabel, label, languageCode);
				}
			}
		}
	}

}
