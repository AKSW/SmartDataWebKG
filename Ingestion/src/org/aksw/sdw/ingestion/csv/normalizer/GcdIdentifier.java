package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.SKOS;
import org.aksw.sdw.ingestion.csv.utils.CustomJenaType;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;

public class GcdIdentifier implements PropertyNormalizer {
	
	final static String oldIdProperty = "http://corp.dbpedia.org/ontology#identifier_gcd"; 

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		List<RDFNode> idObjects = entity.getRdfObjects(GcdIdentifier.oldIdProperty);
		if (null == idObjects || idObjects.isEmpty()) {
			throw new IngestionException("No ID for entity was found: " + entity.getSubjectUri());
		}
		
		Entity idEntity = new Entity(false);
		idEntity.setSubjectUri(entity.getSubjectUri() + "_gcdIdentifier");
		
		for (RDFNode idObject : idObjects) {
			String id = idObject.asResource().getURI();
			
			/// TODO: Ensure that we can add other data types
			
			idEntity.addTripleWithLiteral(SKOS.notation, id, new CustomJenaType(CorpDbpedia.dataTypeIdString));
		}
		
		ResourceImpl creatorUri = new ResourceImpl("http://dfki.gcd.de");
		idEntity.addTriple(GridIdentifierNormalizer.purlCreator, creatorUri);
		idEntity.addTriple(RDF.type.getURI().toString(),
						 new ResourceImpl(GridIdentifierNormalizer.admsIdentifierClass)); 
		
		entity.addSubEntity(idEntity);
		entity.addTriple(GridIdentifierNormalizer.admsIdentifierProperty, idEntity.getSubjectUriObject());
		entity.addTriple(CorpDbpedia.prefixOntology + "identifier_gcd", idEntity.getSubjectUriObject());
		
		// delete old entry
		entity.deleteProperty(GcdIdentifier.oldIdProperty);
	}

}
