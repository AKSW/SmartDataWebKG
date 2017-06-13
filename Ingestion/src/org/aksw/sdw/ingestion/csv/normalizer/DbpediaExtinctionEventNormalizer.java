package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;

import aksw.org.kg.entity.Entity;

/**
 * This class can be used to add events where companies where closed
 * 
 * @author kay
 *
 */
public class DbpediaExtinctionEventNormalizer extends GenericEventNormalizer {

	public DbpediaExtinctionEventNormalizer(String sourcePredicate, String targetPredicate, String enitySuffix) {
		super(sourcePredicate, targetPredicate, enitySuffix);
	}
	
	
	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		super.normalize(entity, stats);
		
		// find closed event
		Entity closedEventEntity = null;
		for (Entity subEntity : entity.getSubEntities()) {
			if (subEntity.getSubjectUri().contains(this.enitySuffix)) {
				closedEventEntity = subEntity;
				break; // there can only be one close event per company
			}
		}
		
		List<RDFNode> extinctionYear = entity.getRdfObjects("http://dbpedia.org/ontology/extinctionYear");
		List<RDFNode> successors = entity.getRdfObjects("http://dbpedia.org/ontology/successor");
		List<RDFNode> defunctObject = entity.getRdfObjects("http://dbpedia.org/property/defunct");
		
		// make sure that the company status is set correctly!!
		boolean foundExtinctionEvent = false;
		if (null != closedEventEntity ||
			(null != defunctObject && false == defunctObject.isEmpty()) ||
			(null != extinctionYear && false == extinctionYear.isEmpty()) ||
			(null != successors && false == successors.isEmpty())) {
			// no matter what we wrote before --> the company does not exist anymore!
			entity.deleteProperty(CorpDbpedia.companyStatus);
			entity.addTriple(new PropertyImpl(CorpDbpedia.companyStatus), new ResourceImpl(CorpDbpedia.companyStatusInActive));
			foundExtinctionEvent = true;
		}
		
		if (false == foundExtinctionEvent) {
			return; // did not find this type of event
		}
		
		if (null == closedEventEntity) {
			closedEventEntity = this.createNewEventEntity(entity, 0);
		}
		
		// delete what ever is in there now
		closedEventEntity.deleteProperty(W3COrg.resultingOrganization);
		for (RDFNode successor : successors) {
			closedEventEntity.addTriple(new PropertyImpl(W3COrg.resultingOrganization), successor.asResource());
		}
		
		// make sure we delete all the properties
		entity.deleteProperty("http://dbpedia.org/ontology/extinctionYear");
		entity.deleteProperty("http://dbpedia.org/ontology/successor");
		entity.deleteProperty("http://dbpedia.org/property/defunct");
	}
}
