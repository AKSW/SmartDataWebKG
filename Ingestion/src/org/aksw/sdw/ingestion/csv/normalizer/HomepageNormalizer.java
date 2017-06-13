package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.vocabulary.FOAF;

public class HomepageNormalizer implements PropertyNormalizer {
	
	/** this pattern can be used to check for URL prefix */
	final static protected Pattern homepageUrlPrefixPattern = Pattern.compile("http(s)?:\\/\\/www\\.");

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {

		List<RDFNode> homepageObjects = entity.getRdfObjects(FOAF.homepage.getURI());
		if (null == homepageObjects || homepageObjects.isEmpty()) {
			return;
		}
		

		List<RDFNode> homepageUris = null;
		Iterator<RDFNode> homepageObjectIterator = homepageObjects.iterator();
		while (homepageObjectIterator.hasNext()) {
			RDFNode homepageObject = homepageObjectIterator.next();
			String homepageUrlOrig = (homepageObject.isLiteral()
					? homepageObject.asLiteral().getLexicalForm().trim() : homepageObject.asResource().getURI());
			
			String homepageUrlNew = homepageUrlOrig;
			
			// remove "/" character at the end
			if (homepageUrlNew.endsWith("/")) {
				homepageUrlNew = homepageUrlNew.substring(0, homepageUrlNew.length() - 1);
			}
			
			if (false == homepageUrlPrefixPattern.matcher(homepageUrlNew).find()) {
				// if it does not start with http
				if (homepageUrlNew.startsWith("http://www.") ||
					homepageUrlNew.startsWith("https://www.")) {
					// ignore --> something went wrong with the regex
				} else if (false == homepageUrlNew.startsWith("http")) {
					homepageUrlNew = "http://www." + homepageUrlNew;				
				} else if (homepageUrlNew.startsWith("http://")) {
					homepageUrlNew = homepageUrlNew.replace("http://", "http://www.");
				} else if (homepageUrlNew.startsWith("https://")) {
					homepageUrlNew = homepageUrlNew.replace("https://", "https://www.");
				}
			}
			
			stats.addNumberToStats(getClass(), "homepage.totalCount", 1);

			
			// store the corrected version
			if (false == homepageUrlNew.equals(homepageUrlOrig)) {				
				stats.addNumberToStats(getClass(), "homepage.changedCount", 1);
			}
			
			// make sure they are URIs
			if (homepageObject instanceof Resource) {
				homepageObjectIterator.remove();
				
				if (null == homepageUris) {
					homepageUris = new ArrayList<>();
				}
				homepageUris.add(new ResourceImpl(homepageUrlNew));
			}
		}
		
		// add URI objects which were deleted in last iteration
		if (null != homepageUris) {
			
			// delete old values
			entity.deleteProperty(FOAF.homepage.getURI());
			
			// add new values to entity
			for (RDFNode homepageUri : homepageUris) {
				entity.addTriple(new PropertyImpl(FOAF.homepage.getURI()), homepageUri);
			}			
		}
	}

}
