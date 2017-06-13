package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;

import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

public class PrintInvalidPredicates implements PropertyNormalizer {
	
	List<String> knownPrefixes = new ArrayList<>();
	
	Set<String> alreadyPrintedPredicates = new HashSet<>();
	
	final boolean filterUnknownProperties;
	
	public PrintInvalidPredicates(final boolean filterUnknownProperties) {
		
		this.filterUnknownProperties = filterUnknownProperties;
		
		this.knownPrefixes.add(CorpDbpedia.prefixOntology);
		this.knownPrefixes.add(CorpDbpedia.prefixResource);
		this.knownPrefixes.add(RDF.getURI().toString());
		this.knownPrefixes.add("http://www.w3.org/2004/02/skos/core#");
		this.knownPrefixes.add(W3COrg.prefix);
		this.knownPrefixes.add(FOAF.getURI().toString());
		this.knownPrefixes.add(OWL.getURI().toString());
		this.knownPrefixes.add("http://www.w3.org/ns/adms#");
		this.knownPrefixes.add("http://www.w3.org/ns/prov#");
		this.knownPrefixes.add("http://dbpedia.org/ontology/");
	}

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		
		for (String predicate : entity.getPredicates()) {
			
			boolean foundPrefix = false;
			for (String prefix : this.knownPrefixes) {
				if (predicate.startsWith(prefix)) {
					foundPrefix = true;
					break;
				}
			}
			
			if (false == foundPrefix && false == this.alreadyPrintedPredicates.contains(predicate)) {
				
				System.err.println("Should not have this predicate: " + predicate);
				this.alreadyPrintedPredicates.add(predicate);				
			}
		}
		
		if (this.filterUnknownProperties) {
			// filter out unknown predicates
			for (String unknownPredicate : this.alreadyPrintedPredicates) {
				entity.deleteProperty(unknownPredicate);
			}
		}
	}

}
