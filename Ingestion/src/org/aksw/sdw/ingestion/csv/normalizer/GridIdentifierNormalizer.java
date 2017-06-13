package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.SKOS;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.aksw.sdw.ingestion.csv.utils.CustomJenaType;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

public class GridIdentifierNormalizer extends JsonPropertyNormalizer {
	
	static public final String purlCreator = "http://purl.org/dc/terms/creator";
	
	static public final String admsIdentifierClass = "http://www.w3.org/ns/adms#Identifier";

	static public final String admsIdentifierProperty = "http://www.w3.org/ns/adms#identifier";
	
	static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	static final List<String> gridIdPrefixes = getGridIdPrefixes();
	static List<String> getGridIdPrefixes() {
		List<String> idPrefixes = new ArrayList<>();
		
		idPrefixes.add("id");
		idPrefixes.add("wikipedia_url");
		idPrefixes.add("external_ids.ISNI.");
		idPrefixes.add("external_ids.FundRef.");
		idPrefixes.add("external_ids.OrgRef.");
		idPrefixes.add("external_ids.Wikidata.");
		idPrefixes.add("external_ids.HESA.");
		idPrefixes.add("external_ids.UCAS.");
		idPrefixes.add("external_ids.UKPRN.");
		idPrefixes.add("external_ids.CNRS.");
		idPrefixes.add("external_ids.LinkedIn.");
		
		return idPrefixes;
	}
	
	public GridIdentifierNormalizer() {
		super(gridIdPrefixes);
	}
	
	/**
	 * This method can be used to add identifier information to a given entity
	 * 
	 * @param mainEntity		- main entity to which the new information is going to be stored in
	 * @param notationLiterals	- literal objects for notation
	 * @param creatorName		- creator name of authority which is used in the uri of the ID
	 * @param index				- index of this instance
	 * @param creatorUriString  - creator URI of authority which created ID
	 * @param idClassName		- name of the ID class
	 */
	public static void addIdEnttiy(final Entity mainEntity, final List<Literal> notationLiterals,
							   final String creatorName,
							   final String index,
							   final String creatorUriString,
							   final String idClassName) {
		if (null == mainEntity || mainEntity.isEmpty()) {
			return;
		}
	
		Entity idEntity = new Entity(false);
		idEntity.setSubjectUri(mainEntity.getSubjectUri() + "_" + creatorName.toLowerCase().trim() + index);
		
		for (RDFNode notationLiteral : notationLiterals) {
			idEntity.addTriple(SKOS.corePrefix + "notation", notationLiteral);
		}
		
		ResourceImpl creatorUri = new ResourceImpl(creatorUriString);
		idEntity.addTriple(purlCreator, creatorUri);
		idEntity.addTriple(RDF.type.getURI().toString(),
						 new ResourceImpl(admsIdentifierClass)); 
		
		mainEntity.addSubEntity(idEntity);
		mainEntity.addTriple(admsIdentifierProperty, idEntity.getSubjectUriObject());
		if (null != idClassName && false == idClassName.isEmpty()) {
			mainEntity.addTriple(CorpDbpedia.prefixOntology + idClassName, idEntity.getSubjectUriObject());
		}
		
		// add general one
		mainEntity.addTriple(W3COrg.identifier, idEntity.getSubjectUriObject());		
	}
	
		
	public static void getNotationLiterals(final List<Literal> notationLiterals, final Entity entity, final String predicateName, final String dataTypeString) {
		
		List<RDFNode> ids = entity.getRdfObjects(predicateName);
		if (null == ids || ids.isEmpty()) {
			return;
		}
		
		for (RDFNode id : ids) {
			if (id.isResource()) {
				continue; // just need ID strings which are no IRIs
			}
			
			String idString = id.asLiteral().getLexicalForm();
			String finalString = WHITESPACE_PATTERN.matcher(idString.trim()).replaceAll("");
			
			Literal notationLiteral = entity.getLiteral(finalString, new CustomJenaType(dataTypeString));			
			notationLiterals.add(notationLiteral);
		}		
	}
	
	public static void getNotationLiteralsWithPrefixSuffix(final List<Literal> notationLiterals, final Entity entity, final String predicateName,
										     final String idPrefix, final String idSuffix, final String dataTypeString) {
		
		List<RDFNode> ids = entity.getRdfObjects(predicateName);
		if (null == ids || ids.isEmpty()) {
			return;
		}
		
		for (RDFNode id : ids) {
			String idString = id.asLiteral().getLexicalForm();
			String finalString = WHITESPACE_PATTERN.matcher(idString.trim()).replaceAll("");
			
			StringBuffer buffer = new StringBuffer();
			if (null != idPrefix) {
				buffer.append(idPrefix).append(finalString);
			} else {
				buffer.append(finalString);
			}
			
			if (null != idSuffix) {
				buffer.append(idSuffix);
			}					
			
			Literal notationLiteral = entity.getLiteral(buffer.toString(), new CustomJenaType(dataTypeString));
			notationLiterals.add(notationLiteral);
		}		
	}
	
	public static void addSameAsRelationship(final Entity entity, final String predicateName, final String idPrefix, final String idSuffix) {
		List<RDFNode> ids = entity.getRdfObjects(predicateName);
		if (null == ids || ids.isEmpty()) {
			return;
		}
		
		for (RDFNode id : ids) {
			String idString = id.asLiteral().getLexicalForm();
			String finalString = WHITESPACE_PATTERN.matcher(idString.trim()).replaceAll("");
			
			StringBuffer buffer = new StringBuffer();
			if (null != idPrefix) {
				buffer.append(idPrefix).append(finalString);
			} else {
				buffer.append(finalString);
			}
			
			if (null != idSuffix) {
				buffer.append(idSuffix);
			}
			
			ResourceImpl sameAsUri = new ResourceImpl(buffer.toString());
			entity.addTriple(OWL.sameAs.getURI().toString(), sameAsUri);
		}	
	}

	@Override
	protected void normalize(Entity entity, ConversionStats stats,
			Map<String, List<MatchingPredicateStruct>> addressMap) throws IngestionException {
		
		List<Literal> notationLiterals = new ArrayList<>();
		
		for (String prefix : addressMap.keySet()) {
			
			List<MatchingPredicateStruct> matchingPrefixes = addressMap.get(prefix);
			for (MatchingPredicateStruct matchingPrefix : matchingPrefixes) {
				
				notationLiterals.clear();			
			
				if (matchingPrefix.fullPredicate.startsWith("wikipedia_url")) {
					
					getNotationLiterals(notationLiterals, entity, matchingPrefix.fullPredicate,
									CorpDbpedia.dataTypeIdWebsite);				
					
					addIdEnttiy(entity, notationLiterals,
									 "wikipedia", "", "https://www.wikipedia.org", null);
					
				} else if (matchingPrefix.fullPredicate.startsWith("id")) {
					
					getNotationLiterals(notationLiterals, entity, matchingPrefix.fullPredicate,
									CorpDbpedia.dataTypeIdString);
					
					getNotationLiteralsWithPrefixSuffix(notationLiterals, entity, matchingPrefix.fullPredicate,
									"https://grid.ac/institutes/", null,
									CorpDbpedia.dataTypeIdWebsite);
					
					addIdEnttiy(entity, notationLiterals,
							 "grid", "", "https://grid.ac", "identifier_grid");
				} else if (matchingPrefix.fullPredicate.contains("ISNI.")) {
					
					getNotationLiterals(notationLiterals, entity, matchingPrefix.fullPredicate,
									CorpDbpedia.dataTypeIdString);
					
					addSameAsRelationship(entity, matchingPrefix.fullPredicate,
											   "http://isni.org/isni/", null);
					
					int indexDot = matchingPrefix.fullPredicate.lastIndexOf(".");
					addIdEnttiy(entity, notationLiterals,
							 "isni", matchingPrefix.fullPredicate.substring(indexDot + 1),
							 "http://www.isni.org", null);
					
				} else if (matchingPrefix.fullPredicate.contains("FundRef.")) {					
					
					getNotationLiterals(notationLiterals, entity, matchingPrefix.fullPredicate,
									CorpDbpedia.dataTypeIdString);
					
					addSameAsRelationship(entity, matchingPrefix.fullPredicate,
							  "http://data.crossref.org/fundingdata/funder/10.13039/", null);
					
					int indexDot = matchingPrefix.fullPredicate.lastIndexOf(".");
					addIdEnttiy(entity, notationLiterals,
							 "fundRef", matchingPrefix.fullPredicate.substring(indexDot + 1),
							 "www.crossref.org", null);					

				} else if (matchingPrefix.fullPredicate.contains("OrgRef.")) {
					
					getNotationLiterals(notationLiterals, entity, matchingPrefix.fullPredicate,
									CorpDbpedia.dataTypeIdString);
					
					int indexDot = matchingPrefix.fullPredicate.lastIndexOf(".");
					addIdEnttiy(entity, notationLiterals,
							 "orgref", matchingPrefix.fullPredicate.substring(indexDot + 1),
							 "www.orgref.org", null);					

				} else if (matchingPrefix.fullPredicate.contains("Wikidata")) {					
					
					getNotationLiterals(notationLiterals, entity, matchingPrefix.fullPredicate,
										CorpDbpedia.dataTypeIdString);
					getNotationLiteralsWithPrefixSuffix(notationLiterals, entity, matchingPrefix.fullPredicate,
									"https://www.wikidata.org/wiki/", null, CorpDbpedia.dataTypeIdWebsite);
					
					addIdEnttiy(entity, notationLiterals,
							 "wikidata", "",
							 "https://www.wikidata.org", null);
				} else if (matchingPrefix.fullPredicate.contains("HESA")) {
					
					getNotationLiterals(notationLiterals, entity, matchingPrefix.fullPredicate,
									CorpDbpedia.dataTypeIdString);
					
					int indexDot = matchingPrefix.fullPredicate.lastIndexOf(".");
					addIdEnttiy(entity, notationLiterals,
							 "hesa", matchingPrefix.fullPredicate.substring(indexDot + 1),
							 "https://www.hesa.ac.uk", null);
				} else if (matchingPrefix.fullPredicate.contains("UCAS")) {
					
					getNotationLiterals(notationLiterals, entity, matchingPrefix.fullPredicate,
									CorpDbpedia.dataTypeIdString);
					
					int indexDot = matchingPrefix.fullPredicate.lastIndexOf(".");
					addIdEnttiy(entity, notationLiterals,
							 "ucas", matchingPrefix.fullPredicate.substring(indexDot + 1),
							 "https://www.ucas.com", null);
				} else if (matchingPrefix.fullPredicate.contains("UKPRN")) {
					
					getNotationLiterals(notationLiterals, entity, matchingPrefix.fullPredicate,
									CorpDbpedia.dataTypeIdString);
					
					int indexDot = matchingPrefix.fullPredicate.lastIndexOf(".");
					addIdEnttiy(entity, notationLiterals,
							 "ukprn", matchingPrefix.fullPredicate.substring(indexDot + 1),
							 "https://www.ukrlp.co.uk", null);
				} else if (matchingPrefix.fullPredicate.contains("CNRS")) {
					
					getNotationLiterals(notationLiterals, entity, matchingPrefix.fullPredicate,
									CorpDbpedia.dataTypeIdString);
					
					int indexDot = matchingPrefix.fullPredicate.lastIndexOf(".");
					addIdEnttiy(entity, notationLiterals,
							 "cnrs", matchingPrefix.fullPredicate.substring(indexDot + 1),
							 "http://http://www.cnrs.fr", null);
				} else if (matchingPrefix.fullPredicate.contains("LinkedIn")) {
					
					getNotationLiterals(notationLiterals, entity, matchingPrefix.fullPredicate,
									CorpDbpedia.dataTypeIdString);
					
					addIdEnttiy(entity, notationLiterals,
							 "linkedin", "", "https://www.linkedin.com", null);
				}
			}
		}		
	}
}
