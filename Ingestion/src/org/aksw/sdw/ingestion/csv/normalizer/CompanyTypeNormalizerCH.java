package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;
import java.util.regex.Pattern;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
//import aksw.org.kg.entity.RdfObject;
//import aksw.org.kg.entity.RdfObjectLiteral;
//import aksw.org.kg.entity.RdfObjectUri;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;

/**
 * This class can be used to add company data types
 * @author kay
 *
 */
public class CompanyTypeNormalizerCH implements PropertyNormalizer {
	
	// word character pattern
	final static Pattern nonWordCharacter = Pattern.compile("[\\W\\s]+", Pattern.UNICODE_CHARACTER_CLASS);

	@Override
	public void normalize(final Entity entity, final ConversionStats stats) throws IngestionException {
		
		if (true) {
			throw new RuntimeException("Do not use this anymore!");
		}
		
		entity.addTriple(new PropertyImpl(RDF.type.getURI()), new ResourceImpl(W3COrg.organization));
	
		List<RDFNode> classificationObjects = entity.getRdfObjects(W3COrg.classfication);
		if (null == classificationObjects) {
			return;
		}
		
		for (RDFNode classificationObject : classificationObjects) {
			String classification = classificationObject.asLiteral().getLexicalForm().trim();
			if (null == classification) {
				continue;
			}
			
			// first change everything to white spaces --> then trim
			String cleanClassification = CompanyTypeNormalizerCH.nonWordCharacter.
					matcher(classification).replaceAll(" ").toLowerCase().trim();
			// now take the remaining whitespaces and replace them with '_'
			cleanClassification = CompanyTypeNormalizerCH.nonWordCharacter.matcher(cleanClassification).replaceAll("_");
			
			String companyUri = CorpDbpedia.prefixOntology + "/de/" + cleanClassification;
			entity.addTriple(new PropertyImpl(RDF.type.getURI()), new ResourceImpl(companyUri));

		}
		
		// rmemove classification
		entity.deleteProperty(W3COrg.classfication);
	}
}
