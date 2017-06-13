package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.apache.jena.rdf.model.RDFNode;

import aksw.org.kg.entity.Entity;

public class DBpediaRelationshipNormalizer extends RelationNormalizer {

	public DBpediaRelationshipNormalizer(PropertyNormalizer nameNormalizer) {
		super(nameNormalizer);
	}
	
	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		super.normalize(entity, stats);
		
		List<RDFNode> headOfObjects = entity.getRdfObjects(W3COrg.headOf);
		if (null == headOfObjects || headOfObjects.isEmpty()) {
			return;
		}
		
		Entity tmpEntity = new Entity(false);
		for (RDFNode headOfObject : headOfObjects) {
			
			tmpEntity.setSubjectUri(headOfObject.asResource().getURI());			
			this.nameNormalizer.normalize(tmpEntity, stats);
		}
	}
}
