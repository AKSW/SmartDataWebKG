package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.Iterator;
import java.util.List;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL;

/**
 * This class is used to clean "RDFS:sameAs" relationships if they are not followed by a full ID
 * @author kay
 *
 */
public class SameAsNormalizer implements PropertyNormalizer {

	@Override
	public void normalize(final Entity entity, ConversionStats stats)
			throws IngestionException {
		List<RDFNode> sameAsObjects = entity.getRdfObjects(OWL.sameAs.getURI());
		if (null == sameAsObjects || sameAsObjects.isEmpty()) {
			return;
		}
		
		/// TODO km : Ensure that the links are URIs and not literals!!
		Iterator<RDFNode> iterator = sameAsObjects.iterator();
		while (iterator.hasNext()) {
			RDFNode sameAsObject = iterator.next();
			String sameAsString = sameAsObject.asResource().getURI();
			if (sameAsString.endsWith("/")) {
				iterator.remove();
			} else {
				stats.incrementStats(SameAsNormalizer.class, "sameAsCount");
			}
		}
	}

}
