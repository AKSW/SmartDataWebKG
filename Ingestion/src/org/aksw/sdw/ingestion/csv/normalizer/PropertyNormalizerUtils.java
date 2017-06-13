package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper;
import org.aksw.sdw.ingestion.csv.utils.OntologyHandler;

/**
 * This class supplies utility instances and methods
 * for dealing with normalizing entity property values 
 *  
 * @author kay
 *
 */
public class PropertyNormalizerUtils {
	
	static PropertyNormalizerUtils instance = null;
	
	/** mapper of country name to geo name */
	final GeoNamesMapper geoNamesMapper;
	
	final OntologyHandler ontologyHandler;
	
	public static String geoNamesMappingFilePath = null;
	
	private PropertyNormalizerUtils() {
		
		try {
			this.geoNamesMapper =
					new GeoNamesMapper(PropertyNormalizerUtils.geoNamesMappingFilePath );
			this.ontologyHandler = OntologyHandler.getInstance();
		} catch (IngestionException e) {
			throw new RuntimeException(e);
		}		
	}
	
	static public PropertyNormalizerUtils getInstance() {
		if (null == PropertyNormalizerUtils.instance) {
			PropertyNormalizerUtils.instance = new PropertyNormalizerUtils();
		}
		
		return PropertyNormalizerUtils.instance;
	}
	

	public static void init(final List<String> fileList, final String geoNamesMappingFile) {
		PropertyNormalizerUtils.geoNamesMappingFilePath = geoNamesMappingFile;
		
		OntologyHandler.init(fileList);
	}
	
	/**
	 * The geo names mapper can be used to map a country surface form name
	 * to geo names ids, post codes, etc.
	 * @return geo names mapper instance
	 */
	public GeoNamesMapper getGeoNamesMapper() {
		return this.geoNamesMapper;
	}
	
	public OntologyHandler getOntologyHandler() {
		return this.ontologyHandler;
	}

}
