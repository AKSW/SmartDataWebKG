package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;

import aksw.org.kg.entity.Entity;

public class DBpediaTypeNormalizer extends TypeNormalizer {
	
	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		// run first previous method
		super.normalize(entity, stats);
		
		List<RDFNode> orgTypeObjects = entity.getRdfObjects(CorpDbpedia.orgType);
		
		List<RDFNode> dbpediaOrgTypeObjects = entity.getRdfObjects("http://dbpedia.org/ontology/type");
		List<RDFNode> dbpediaOrgTypeLabelObjects = entity.getRdfObjects("http://dbpedia.org/ontology/typeLabel");
		if (null == dbpediaOrgTypeLabelObjects || dbpediaOrgTypeLabelObjects.isEmpty()) {
			return;
		}
		
		for (RDFNode dbpediaOrgTypeLabelObject : dbpediaOrgTypeLabelObjects) {
			if (false == this.iterateOverTypeObjects(entity, dbpediaOrgTypeLabelObject, orgTypeObjects)) {
				continue;
			}
			
			break;
		}
	
		for (RDFNode dbpediaOrgTypeObject : dbpediaOrgTypeObjects) {
		if (false == this.iterateOverTypeObjects(entity, dbpediaOrgTypeObject, orgTypeObjects)) {
			continue;
		}
		
		break;
	}
	}
	
	protected boolean iterateOverTypeObjects(final Entity entity, final RDFNode dbpediaOrgTypeObject, final List<RDFNode> orgTypeObjects) {
		if (null == dbpediaOrgTypeObject || false == dbpediaOrgTypeObject instanceof Literal) {
			return false;
		}
		
		Literal orgTypeLabelObject = (Literal) dbpediaOrgTypeObject;
		String typeLabel = orgTypeLabelObject.getLexicalForm();
		String languageId = orgTypeLabelObject.getLanguage();
		
		String typeUri = this.getCompanyTypeUri(typeLabel, languageId);
		if (null == typeUri) {
			return false;
		}
		
		List<String> parentClassNames = PropertyNormalizerUtils.getInstance().getOntologyHandler().getParentClassNames(typeUri);
		if (null == parentClassNames || parentClassNames.isEmpty()) {
			return false;
		}
		
		if (null == orgTypeObjects) {
			entity.addTriple(CorpDbpedia.orgType, new ResourceImpl(typeUri));
			for (String parentClassName : parentClassNames) {
				entity.addTriple(CorpDbpedia.orgType, new ResourceImpl(parentClassName));
			}
		} else {
			ResourceImpl dbpediaCompanyType = new ResourceImpl(typeUri);
			// if it does not know it --> delete and add DBpedia company type
			if (false == orgTypeObjects.contains(dbpediaCompanyType)) {
				// delete other company type
				entity.deleteProperty(CorpDbpedia.orgType);
				
				// add DBpedia company type
				entity.addTriple(CorpDbpedia.orgType, new ResourceImpl(typeUri));
				for (String parentClassName : parentClassNames) {
					entity.addTriple(CorpDbpedia.orgType, new ResourceImpl(parentClassName));
				}
			} // do not do anything, if we found a match --> have information already
		}
		
		// got result
		return true;
	}

}
