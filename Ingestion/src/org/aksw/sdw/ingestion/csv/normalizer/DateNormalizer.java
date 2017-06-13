package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.aksw.sdw.ingestion.ConversionStats;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

import aksw.org.kg.entity.Entity;
import aksw.org.kg.entity.Entity.EntityPropertyNormalizer;

/**
 * This class can be used to normalise date inputs
 * into the correct date XML format.
 * 
 * @author kay
 *
 */
public class DateNormalizer implements PropertyNormalizer {	
	
	/** only match '/', if there is a number before and behind this character */
	final static Pattern cleanupPatternDate = Pattern.compile("(?<=[0-9])[/](?=[0-9])");

	/// TODO km : improve date normalization
	@Override
	public void normalize(final Entity entity, final ConversionStats stats) {
		
		entity.normalizePropertyValue(Pattern.compile("((?i)^(.*date)$)"), new EntityPropertyNormalizer() {
			
			@Override
			protected void normalize(Collection<RDFNode> rdfObjects, String subjectUri, String predicateUri) {
				
				List<RDFNode> rdfObjectList = new ArrayList<>(rdfObjects);
				
				// go through the date
				for (int i = 0; i < rdfObjects.size(); ++i) {
					RDFNode rdfObject = rdfObjectList.get(i);					
					if (false == rdfObject instanceof Literal) {
						continue;
					}
					
					// clean up string
					String objectString = rdfObject.asLiteral().getLexicalForm();
					
					/// TODO km : think of other ways of using Date Formatter!
					String cleanedObjectString = DateNormalizer.cleanupPatternDate.
							matcher(objectString).replaceAll("-");
					
					if (false == cleanedObjectString.equals(objectString)) {
						// if difference is found --> update
						Literal rdfObjectLiteral = (Literal) rdfObject;
					}
				}				
			}
		});
	}

}
