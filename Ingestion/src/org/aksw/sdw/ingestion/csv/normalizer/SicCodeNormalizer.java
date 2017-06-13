package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.csv.constants.RdfDataTypes;
import org.apache.jena.rdf.model.RDFNode;

	
/**
 * This class can be used to obtain the SIC code from an existing SIC-Text property
 * 
 * @author kay
 *
 */
public class SicCodeNormalizer implements PropertyNormalizer {
	
	/** number pattern */
	static final Pattern numberPattern = Pattern.compile("([0-9]+)");
	
	/** predicate URI */
	static final String predicateSicCodeText = "http://corp.dbpedia.org/ontology/uk/limited-company/SICCodeSicText";
	
	/** predicate sic code */
	static final String predicateSicCode = "http://corp.dbpedia.org/ontology/uk/limited-company/SICCode";

	@Override
	public void normalize(final Entity entity, final ConversionStats stats) {		
		
		List<RDFNode> objects = entity.getRdfObjects(predicateSicCodeText);
		if (null == objects) {
			return;
		}
		
		for (RDFNode object : objects) {
			String objectString = object.toString();		
			
			// find out whether we found a sic-code
			Matcher matcher = SicCodeNormalizer.numberPattern.matcher(objectString);
			if (matcher.find()) {
				// get the sic-code
				String sicCode = matcher.group(0);
				entity.addTripleWithLiteral(SicCodeNormalizer.predicateSicCode, sicCode, RdfDataTypes.XmlSchemaInteger);
			}
		}		
	}
	
}

