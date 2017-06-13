package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CompanyHouseUris;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.log4j.Logger;

/**
 * This class can be used to add country geo names
 * to an entity
 * 
 * @author kay
 *
 */
public class CountryGeoNamesNormalizer implements PropertyNormalizer {
	
	final static Logger logger = Logger.getLogger(CountryGeoNamesNormalizer.class);

	@Override
	public void normalize(final Entity entity, final ConversionStats stats) throws IngestionException {
		
		Entity addressEntity = entity.getSubEntityByCategory("address");
		if (null == addressEntity) {
			return;
		}
		
		// get registered registered country for entity
		List<RDFNode> countryObjects = entity.getRdfObjects(CompanyHouseUris.predicateCountryOfOrigin);
		if (null == countryObjects) {
			countryObjects = entity.getRdfObjects(CompanyHouseUris.predicateUriCountryName);
		}
		
		if (null == countryObjects) {
			// has none --> continue
			return;
		}
		
		// stores names of known countries
		Set<String> knownCountries = new HashSet<>();
		Set<String> unknownCountries = new HashSet<>();
		
		// go through all found country objects
		for (RDFNode countryObject : countryObjects) {
			// get geo name id
			Literal countryNameLiteral = countryObject.asLiteral();
			String countryName = countryNameLiteral.getLexicalForm();
			String languageCode = countryNameLiteral.getLanguage();
			String geoNameId = PropertyNormalizerUtils.getInstance()
					.geoNamesMapper.getGeoNamesCountryId(countryName,
							(null == languageCode || languageCode.isEmpty() ? "en" : languageCode));			
			if (null == geoNameId) {
				
				if (false == unknownCountries.contains(countryName)) {
					unknownCountries.add(countryName);					
				}
				
				stats.incrementStats(CountryGeoNamesNormalizer.class, "didNotFindCountries");
				logger.warn("Was not able to find country: " + countryName);
				continue;
			}
			
			// store geo name id in entity value map
			ResourceImpl countryUri = new ResourceImpl(geoNameId);
			entity.addTriple(new PropertyImpl("http://dbpedia.org/ontology/location"), countryUri);
			
			// count all the countries we know
			if (false == knownCountries.contains(countryName)) {
				knownCountries.add(countryName);
			}
			stats.incrementStats(CountryGeoNamesNormalizer.class, "CountryGeoNamesNormalizer:foundCountries");
		}
		
		if (false == unknownCountries.isEmpty()) {
			stats.setStatText(getClass(), "unknownCountries", unknownCountries.toString());
		}
		
		if (false == knownCountries.isEmpty()) {
			stats.setStatText(getClass(), "knownCountries", knownCountries.toString());
		}
	}

}
