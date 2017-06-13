package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.aksw.sdw.ingestion.csv.constants.VCARD;

public class NormalizerCorrectSubject implements PropertyNormalizer {

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		
		Entity addressEntity = entity.getSubEntityByCategory("address");
		if (null == addressEntity || addressEntity.isEmpty()) {
			return;
		}
		
		List<RDFNode> countryObjects = addressEntity.getRdfObjects(VCARD.ADDR_COUNTRY_NAME);
		if (null == countryObjects || countryObjects.isEmpty()) {
			return;
		}
		
		String nameEN = null;
		String nameDE = null;
		String countryCode = null;
		for (RDFNode countryObject : countryObjects) {			
			if (countryObject instanceof Resource) {
				String geonamesUri = countryObject.asResource().getURI();
				
				countryCode = PropertyNormalizerUtils.getInstance().
						getGeoNamesMapper().getCountryCode(geonamesUri);
				
				nameEN = PropertyNormalizerUtils.getInstance().
						getGeoNamesMapper().getNameFromId(geonamesUri, "@en");
				nameDE  = PropertyNormalizerUtils.getInstance().
						getGeoNamesMapper().getNameFromId(geonamesUri, "@de");
				break;
			}
		}
			
		if (null != nameEN) {
			String cleanName = nameEN.substring(0, nameEN.indexOf("@"));
			addressEntity.addTripleWithLiteral(VCARD.ADDR_COUNTRY_NAME, cleanName, "en");
		}
		
		if (null != nameDE) {
			String cleanName = nameDE.substring(0, nameDE.indexOf("@"));
			addressEntity.addTripleWithLiteral(VCARD.ADDR_COUNTRY_NAME, cleanName, "de");
		}
		
		String name = null;
		List<RDFNode> nameObjects = entity.getRdfObjects("http://www.w3.org/2004/02/skos/core#prefLabel");
		if (null == nameObjects || nameObjects.isEmpty()) {
			return;
		}
		for (RDFNode nameObject : nameObjects) {
			name = nameObject.asLiteral().getLexicalForm();
			break;
		}
		
		String id = null;
		List<RDFNode> idObjects = entity.getRdfObjects(W3COrg.identifier);
		if (null != idObjects && false == idObjects.isEmpty()) {
			for (RDFNode idObject : idObjects) {
				id = idObject.asResource().getURI();
				break;
			}
		}
		
		String newUri = CorpDbpedia.prefixResource + countryCode.toLowerCase() + "/permid_" + name + ((null == id) ? null : "_" + id);
		entity.setSubjectUri(newUri);
		
		List<String> oldPrefixes = Arrays.asList(
				"http://www.omg.org",
				"http://permid.org",
				"http://ont.thomsonreuters.com"
				);
		
		// clean old predicates
		Collection<String> predicateStrings = entity.getPredicates();
		Iterator<String> predicateIt = predicateStrings.iterator();
		while (predicateIt.hasNext()) {
			boolean filterOut = false;
			String predicateString = predicateIt.next();
			
			for (String oldPrefix : oldPrefixes) {
				if (predicateString.startsWith(oldPrefix)) {
					filterOut = true;
					break;
				}
			}
			
			if (filterOut) {
				predicateIt.remove();
			}
		}		
	}

}
