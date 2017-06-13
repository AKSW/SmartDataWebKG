package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;
import java.util.Map;

import aksw.org.kg.entity.Entity;
//import aksw.org.kg.entity.RdfObject;
//import aksw.org.kg.entity.RdfObjectUri;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;

public class ActiveNormalizer implements PropertyNormalizer {
	
	/** map of all supported statuses */
	final Map<CompanyActivity, List<String>> statusPredicateMap;
	
	/** name of the predicate string */
	final String predicateName;
	
	/** enum which specifies the supported statuses */
	public enum CompanyActivity {ACTIVE, INACTIVE, REDIRECTED};
	
	public ActiveNormalizer(final String predicateName, final Map<CompanyActivity, List<String>> statusPredicateMap) {
		this.statusPredicateMap = statusPredicateMap;
		this.predicateName = predicateName;
	}

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {

		// find out whether a PermID company is active
		List<RDFNode> activeObjects = entity.getRdfObjects(this.predicateName);
		if (null == activeObjects || activeObjects.isEmpty() || 2 <= activeObjects.size()) {
			// in case we do not know --> assume it is open
			entity.addTriple(new PropertyImpl(CorpDbpedia.companyStatus),
							 new ResourceImpl(CorpDbpedia.companyStatusActive));
			return;
		}
		
		RDFNode activityStatus = activeObjects.get(0);
		if (this.hasMatch(CompanyActivity.ACTIVE, activityStatus)) {
			entity.addTriple(new PropertyImpl(CorpDbpedia.companyStatus), new ResourceImpl(CorpDbpedia.companyStatusActive));
		} else if (this.hasMatch(CompanyActivity.INACTIVE, activityStatus)) {
			entity.addTriple(new PropertyImpl(CorpDbpedia.companyStatus), new ResourceImpl(CorpDbpedia.companyStatusInActive));
		} else if (this.hasMatch(CompanyActivity.REDIRECTED, activityStatus)) {
			/// TODO km: check what this is all about!S
			//entity.addTriple(CorpDbpedia.companyStatus, new RdfObjectUri("http://corp.dbpedia.org/ontology#redirected"));
			entity.addTriple(new PropertyImpl(CorpDbpedia.companyStatus), new ResourceImpl(CorpDbpedia.companyStatusInActive));
		} else {
			// in case we do not know --> assume it is open
			entity.addTriple(new PropertyImpl(CorpDbpedia.companyStatus), new ResourceImpl(CorpDbpedia.companyStatusActive));
		}
		
		// delete old entry
		entity.deleteProperty(this.predicateName);
	}
	
	/**
	 * Find out whether the rdfObject literal/uri matches registered
	 * @param rdfObject
	 * @return
	 */
	protected boolean hasMatch(final CompanyActivity status, final RDFNode rdfObject) {
		if (null == rdfObject) {
			return false;
		}
		
		List<String> statusStrings = this.statusPredicateMap.get(status);
		if (null == statusStrings|| statusStrings.isEmpty()) {
			return false;
		}
		
		String statusStringObject = rdfObject.toString().toLowerCase();		
		for (String statusString : statusStrings) {
			if (statusStringObject.contains(statusString.toLowerCase())) {
				return true; // found match
			}
		}
		
		return false;		
	}
}
