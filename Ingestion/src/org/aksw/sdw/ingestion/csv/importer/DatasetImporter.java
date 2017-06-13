package org.aksw.sdw.ingestion.csv.importer;

import java.io.Closeable;

import org.aksw.sdw.ingestion.IngestionException;
import org.apache.jena.graph.Triple;

/**
 * Interface for Dataset importers
 * 
 * @author kay
 *
 */
public interface DatasetImporter extends Closeable {
	
	/**
	 * 
	 * @return true has triple, false otherwise
	 * @throws IngestionException
	 */
	public boolean hasNext() throws IngestionException;
	
	/**
	 * 
	 * @return gets next available triple
	 * @throws IngestionException
	 */
	public Triple next() throws IngestionException;
	

}
