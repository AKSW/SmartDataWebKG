package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;
import java.util.Map;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.OWL;

/**
 * This class can be used to 
 * @author kay
 *
 */
public class GridRedirects extends JsonPropertyNormalizer {
	
	public GridRedirects() {
		super("redirect");
	}

	@Override
	protected void normalize(Entity entity, ConversionStats stats, Map<String, List<MatchingPredicateStruct>> addressMap)
			throws IngestionException {
		
		List<MatchingPredicateStruct> matchingPredicates = addressMap.values().iterator().next();
		if (1 != matchingPredicates.size()) {
			return;
		}
		
		MatchingPredicateStruct matchingPredicate = matchingPredicates.get(0);
		List<RDFNode> redirects = entity.getRdfObjects(matchingPredicate.fullPredicate);		
		for (RDFNode redirect : redirects) {
			String redirectString = redirect.asLiteral().getLexicalForm();
			if (redirectString.startsWith("grid")) {
				String sameAsUriString = CorpDbpedia.prefixResource + redirectString.replace(".", "_");
				ResourceImpl sameAsUri = new ResourceImpl(sameAsUriString);
				
				entity.addTriple(OWL.sameAs.getURI().toString(), sameAsUri);
			}
		}
	}

}
