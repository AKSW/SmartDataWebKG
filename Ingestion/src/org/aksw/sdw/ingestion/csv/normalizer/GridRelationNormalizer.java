package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;
import java.util.Map;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;

public class GridRelationNormalizer extends JsonPropertyNormalizer {
		
	public GridRelationNormalizer() {
		super("relationships.");
	}

	@Override
	public void normalize(Entity entity, ConversionStats stats, final Map<String, List<MatchingPredicateStruct>> relationshipsMap) throws IngestionException {
		
		for (String index : relationshipsMap.keySet()) {
			
			// stores relationshipType
			String relationshipType = null;
			
			List<MatchingPredicateStruct> predicates = relationshipsMap.get(index);
			for (MatchingPredicateStruct predicate : predicates) {
				
				String shortPredicate = predicate.shortenedPredicate;
				if (shortPredicate.equals("type")) {
					List<RDFNode> types = entity.getRdfObjects(predicate.fullPredicate);
					if (null == types || types.isEmpty()) {
						continue;
					}
					
					for (RDFNode type : types) {
						String relationshipTypeTmp = type.asLiteral().getLexicalForm();
						if (null != relationshipTypeTmp) {
							
							switch(relationshipTypeTmp) {
								case "Related":
								case "Child":
								case "Parent":
									relationshipType = relationshipTypeTmp;
									break;
								default:
									System.err.println("\n\nUnkonwn relationship type: " + relationshipTypeTmp + "\n\n");
									break;
							}							
						
						}
					}
				} else if (null != relationshipType && shortPredicate.equals("id")) {
					List<RDFNode> ids = entity.getRdfObjects(predicate.fullPredicate);
					if (null == ids || ids.isEmpty()) {
						continue;
					}
					
					for (RDFNode id : ids) {
						String idString = id.asLiteral().getLexicalForm();
						String normalizedIdString = idString.replace(".", "_");
						
						String relatedUri = CorpDbpedia.prefixResource + normalizedIdString;						
						ResourceImpl relatedUriObject = new ResourceImpl(relatedUri);
	
						switch (relationshipType) {
							case "Related": {
								entity.addTriple(W3COrg.prefix + "linkedTo", relatedUriObject);
								stats.addNumberToStats(getClass(), "relationship.related", 1);
							} break;
							case "Child": {
								entity.addTriple(W3COrg.prefix + "hasUnit", relatedUriObject);
								entity.addTriple(W3COrg.prefix + "hasSubOrganization", relatedUriObject);
								
								Entity childEntity = new Entity(false);
								childEntity.setSubjectUri(relatedUri);
								
								childEntity.addTriple(W3COrg.prefix + "unitOf", entity.getSubjectUriObject());
								childEntity.addTriple(W3COrg.prefix + "subOrganizationOf", entity.getSubjectUriObject());
								
								entity.addSubEntity(childEntity);
								stats.addNumberToStats(getClass(), "relationship.child", 1);
							} break;
							case "Parent": {
								entity.addTriple(W3COrg.prefix + "unitOf", relatedUriObject);
								entity.addTriple(W3COrg.prefix + "subOrganizationOf", relatedUriObject);
								
								Entity parentEntity = new Entity(false);
								parentEntity.setSubjectUri(relatedUri);
								
								parentEntity.addTriple(W3COrg.prefix + "hasUnit", entity.getSubjectUriObject());
								parentEntity.addTriple(W3COrg.prefix + "hasSubOrganization", entity.getSubjectUriObject());
								
								entity.addSubEntity(parentEntity);
								stats.addNumberToStats(getClass(), "relationship.parent", 1);
							}
						}
					}
				}
			}
		}
	}

}
