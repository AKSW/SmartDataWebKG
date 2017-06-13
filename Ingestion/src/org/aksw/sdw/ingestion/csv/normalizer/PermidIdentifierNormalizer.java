package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.ArrayList;
import java.util.List;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

public class PermidIdentifierNormalizer implements PropertyNormalizer {
	
	static final String permidPredicate = "http://corp.dbpedia.org/ontology#identifier_permid";

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		List<RDFNode> idObjects = entity.getRdfObjects(permidPredicate);
		if (null == idObjects || idObjects.isEmpty()) {
			return;
		}

		List<Literal> notationLiterals = new ArrayList<>();
		int index = 0;
		GridIdentifierNormalizer.getNotationLiterals(notationLiterals, entity, permidPredicate, CorpDbpedia.dataTypeIdString);
		GridIdentifierNormalizer.getNotationLiteralsWithPrefixSuffix(notationLiterals, entity, permidPredicate, "https://permid.org/1-", null, CorpDbpedia.dataTypeIdWebsite);
		GridIdentifierNormalizer.addIdEnttiy(entity, notationLiterals,
				"THOMSON REUTERS", Integer.toString(index++), "https://permid.org", (String) null);
	
		entity.deleteProperty(permidPredicate);
	}

}
