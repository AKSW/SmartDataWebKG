package org.aksw.sdw.ingestion.csv.normalizer;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.OWL;

/**
 * This class can be used to replace an existing URI namespace with another one
 * 
 * @author kay
 *
 */
public class SubjectNamespaceReplacer implements PropertyNormalizer {
	
	final String originalNameSpace;
	final String newNameSpace;
	
	public SubjectNamespaceReplacer(final String originalNameSpace, final String newNameSpace) {
		this.originalNameSpace = originalNameSpace;
		this.newNameSpace = newNameSpace;		
	}

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		if (null == entity) {
			return;
		}
		
		String uriString = entity.getSubjectUri();		
		if (false == uriString.startsWith(this.originalNameSpace)) {
			// if we do not start with this namespace --> something is wrong
			return;
		}
		
		// create sameAs Link to original URI
		entity.addTriple(OWL.sameAs.getURI(), new ResourceImpl(uriString));
		
		// replace original with new namespace
		String newUriString = uriString.replace(this.originalNameSpace, this.newNameSpace);
		entity.setSubjectUri(newUriString);
	}

}
