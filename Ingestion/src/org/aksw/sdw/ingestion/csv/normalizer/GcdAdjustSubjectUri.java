package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.apache.jena.rdf.model.RDFNode;

/**
 * This class can be used to remove the postal code from the URI
 * 
 * @author kay
 *
 */
public class GcdAdjustSubjectUri implements PropertyNormalizer {

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		String entityUri = entity.getSubjectUri();
		
		List<RDFNode> idObjects = entity.getRdfObjects("http://corp.dbpedia.org/ontology#identifier_gcd");
		if (null == idObjects || 1 != idObjects.size()) {
			throw new IngestionException("Could not find original id: " + entityUri);
		}
		
		StringBuffer buffer = new StringBuffer();
		buffer.append(CorpDbpedia.prefixResource).append("gcd_").append(idObjects.get(0).asResource().getURI());
		entity.setSubjectUri(buffer.toString());
	}

}
