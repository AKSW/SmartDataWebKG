package org.aksw.sdw.ingestion.csv.normalizer;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;

import aksw.org.kg.entity.Entity;

/**
 * This interface can be used to normalize existing property data
 * 
 * @author kay
 *
 */
public interface PropertyNormalizer {

	/**
	 * This method can be used to normalise the content of the entity map
	 *
	 * @param entity
	 *            - entity map which stores all information which
	 *            are stored for an entity (including blank nodes, etc.)
	 * @param stats
	 *            - can be used to update statistics
	 * @throws IngestionException
	 */
	public void normalize(final Entity entity, final ConversionStats stats)
			throws IngestionException;

}
