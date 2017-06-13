package org.aksw.sdw.ingestion.csv.importer;

import java.io.File;
import java.net.URL;

import org.aksw.sdw.ingestion.IngestionException;
import org.apache.log4j.Logger;

public class DatasetImporterUtils {
	
	final static Logger logger = Logger.getLogger(DatasetImporterUtils.class);
	
	/**
	 * This method is used to obtain the actual
	 * CSV file instance
	 * @return CSV file instance
	 */
	final public static File getFileInstance(final String filePath) throws IngestionException {
//		throw new RuntimeException("Method is not supported for now");
		
		File file = new File(filePath);
		if (false == file.exists()) {
			// get file
			URL csvUrl = DatasetImporterUtils.class.getClassLoader().getResource(filePath);
			if (null == csvUrl) {
				String message = "Was not able to load file: " + filePath;
				logger.error(message);
				throw new IngestionException(message);			
			}
			
			file = new File(csvUrl.getPath());
		}
		
		if (false == file.exists()) {
			String message = "File does not exist: " + file.getAbsolutePath();
			logger.error(message);
			throw new IngestionException(message);
		}
		
		return file;
	}

}
