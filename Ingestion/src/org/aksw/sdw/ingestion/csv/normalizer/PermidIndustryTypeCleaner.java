package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.HashSet;
import java.util.Set;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;

public class PermidIndustryTypeCleaner implements PropertyNormalizer {
	
	Set<String> filterPredicates = new HashSet<>();
	
	public PermidIndustryTypeCleaner() {
		
		this.filterPredicates.add("http://permid.org/ontology/organization/hasPrimaryIndustryGroup");
		this.filterPredicates.add("http://permid.org/ontology/organization/hasPrimaryBusinessSector");
		this.filterPredicates.add("http://permid.org/ontology/organization/hasPrimaryEconomicSector");
		this.filterPredicates.add("http://permid.org/ontology/financial/hasOrganizationPrimaryQuote");
		this.filterPredicates.add("http://permid.org/ontology/financial/hasPrimaryInstrument");
		/// TODO km: check whether this might be useful!
		this.filterPredicates.add("http://permid.org/ontology/organization/hasHoldingClassification");
	}

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		
		for (String filterPredicate : this.filterPredicates) {
			entity.deleteProperty(filterPredicate);
		}
		
	}

}
