package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.apache.jena.rdf.model.RDFNode;

import aksw.org.kg.entity.Entity;

/**
 * This class ensures that the supported relationship information
 * are properly stored in the target dataset
 * 
 * @author kay
 *
 */
public class RelationNormalizer implements PropertyNormalizer {
	
	/** normalizer which can be used to convert source URIs to target entity URIs */
	protected PropertyNormalizer nameNormalizer;
	
	public RelationNormalizer(final PropertyNormalizer nameNormalizer) {
		this.nameNormalizer = nameNormalizer;
	}

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {

		List<RDFNode> subOrganisations = entity.getRdfObjects(W3COrg.hasSubOrganization);
		if (null != subOrganisations) {
			for (RDFNode subOrganisation : subOrganisations) {
				
				Entity childEntity = new Entity(false);
				childEntity.setSubjectUri(subOrganisation.asResource().getURI());
				if (null != this.nameNormalizer) {
					this.nameNormalizer.normalize(childEntity, stats);
					
					entity.addTriple(W3COrg.hasUnit, childEntity.getSubjectUriObject());
					entity.addTriple(W3COrg.hasSubOrganization, childEntity.getSubjectUriObject());
					
					childEntity.addTriple(W3COrg.unitOf, entity.getSubjectUriObject());
					childEntity.addTriple(W3COrg.subOrganizationOf, entity.getSubjectUriObject());
					
					entity.addSubEntity(childEntity);
				}
				
				entity.addTriple(W3COrg.hasUnit, subOrganisation);
				
				stats.addNumberToStats(getClass(), "relationship.child", 1);
			}
		}
		
		List<RDFNode> parentOrgs = entity.getRdfObjects(W3COrg.subOrganizationOf);
		if (null != parentOrgs) {
			for (RDFNode parentOrg : parentOrgs) {
				Entity parentEntity = new Entity(false);
				parentEntity.setSubjectUri(parentOrg.asResource().getURI());
				if (null != this.nameNormalizer) {
					this.nameNormalizer.normalize(parentEntity, stats);
					
					entity.addTriple(W3COrg.unitOf, parentEntity.getSubjectUriObject());
					entity.addTriple(W3COrg.subOrganizationOf, parentEntity.getSubjectUriObject());
					
					parentEntity.addTriple(W3COrg.hasUnit, entity.getSubjectUriObject());
					parentEntity.addTriple(W3COrg.hasSubOrganization, entity.getSubjectUriObject());
					
					entity.addSubEntity(parentEntity);
				}
				
				entity.addTriple(W3COrg.unitOf, parentOrg);
				
				stats.addNumberToStats(getClass(), "relationship.parent", 1);
			}
		}
	}
}
