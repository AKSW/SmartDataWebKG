package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.OWL;



public class EntityUriNormalizer implements PropertyNormalizer {
	
	final String datasetIdPredicate;
	
	final String idPrefix;
	
	public EntityUriNormalizer(final String datasetIdPredicate, final String idPrefix) {
		
		this.idPrefix = idPrefix;
		this.datasetIdPredicate = datasetIdPredicate;		
	}

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		String uri = entity.getSubjectUri();
		if (null == uri || uri.startsWith(CorpDbpedia.prefixResource )) {
			return;
		}
		
		ResourceImpl sameAsUri = new ResourceImpl(uri);
		entity.addTriple(new PropertyImpl(OWL.sameAs.getURI()), sameAsUri);
		
		List<RDFNode> id = entity.getRdfObjects(this.datasetIdPredicate);
		if (null != id && 1 == id.size()) {
			StringBuffer buffer = new StringBuffer();
			
			buffer.append(CorpDbpedia.prefixResource)
				.append(this.idPrefix)
				.append(id.get(0).asLiteral().getLexicalForm());
			
			entity.setSubjectUri(buffer.toString());
		}
	}

}
