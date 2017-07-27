package aksw.org.sdw.importer.avro.annotations.nif;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.nlp2rdf.nif21.NIF21Format;

import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.Mention;
import aksw.org.sdw.importer.avro.annotations.Provenance;
import aksw.org.sdw.importer.avro.annotations.RelationMention;
import aksw.org.sdw.rdf.namespaces.CorpDbpedia;
import aksw.org.sdw.rdf.namespaces.W3COrg;
import aksw.org.sdw.rdf.namespaces.W3CProvenance;

/**
 * This class can be used to add actual graph data of extracted relationships.
 * In addition metadata for each triple is stored which is stored in a metadata sub graph
 * 
 * @author kay
 *
 */
public class RelationGenerator extends DocRdfGenerator {
	
	/** input document */
	final Document document;
	
	/** relation function map */
	final Map<String, Function<ModelData,Integer>> relationFunctionMap = new HashMap<>();
	
	/** count of how often a method was called */
	final ConcurrentMap<String, AtomicInteger> relationFunctionCount = new ConcurrentHashMap<>();
	
	static final public Map<String, AtomicInteger> missingRelations = new HashMap<>();
	
	final static String HandleEntityLabels		= "HandleEntityLabels";
	final static String HandleEntityTypes		= "HandleEntityTypes";
	final static String HandleProvenance		= "HandleProvenance";
	
	final static String CompanyTechnology		= "CompanyTechnology";
	final static String CompanyProvidesProduct	= "CompanyProvidesProduct";
	
	final static String CompanyHeadquarters		= "CompanyHeadquarters";
	final static String OrganizationLeadership	= "OrganizationLeadership";
	final static String CompanyRelationship		= "CompanyRelationship";
	
	public RelationGenerator(final String graphName,final Document document) {
		this(graphName, document, (DocRdfGenerator) null);
		
		relationFunctionMap.put(RelationGenerator.CompanyTechnology, 		this::handleCompanyTechnology);
		relationFunctionMap.put(RelationGenerator.CompanyProvidesProduct,	this::handleCompanyTechnology);
		relationFunctionMap.put(RelationGenerator.HandleEntityLabels,		this::handleLabel);
		relationFunctionMap.put(RelationGenerator.HandleEntityTypes,		this::handleTypes);
		relationFunctionMap.put(RelationGenerator.HandleProvenance,			this::handleProvenance);
		relationFunctionMap.put(RelationGenerator.CompanyHeadquarters,		this::handleHeadquarters);
		relationFunctionMap.put(RelationGenerator.OrganizationLeadership,	this::handleOrganizationLeadership);
		relationFunctionMap.put(RelationGenerator.CompanyRelationship,		this::handleCompanyRelationship);
	}
	
	public RelationGenerator(final String graphName, final Document document, final DocRdfGenerator relationGenerator) {
		super(graphName, relationGenerator);
		this.document = document;
	}
	
	protected void createRelationTriple(final String relationType,
										final RelationMention relationMention,
										final Model model) {
		Objects.requireNonNull(relationType, "No relation type name passed in");
		Objects.requireNonNull(model, "Model is null");
		
		Function<ModelData, Integer> methodReference = this.relationFunctionMap.get(relationType);
		if (null == methodReference) {
			Level level = Level.WARNING;
			Logger.getGlobal().log(level,"Unknown relation type: " + relationType + " for relationMention: " + relationMention);
			recordUnmappedRelations(relationType); 
			return;
			//throw new RuntimeException("Unknown relation type: " + relationType + " for relationMention: " + relationMention);
		}
		
		// gets count for how often method was called
		AtomicInteger tmpCount = new AtomicInteger(0);
		AtomicInteger counter = relationFunctionCount.putIfAbsent(relationType, tmpCount);
		
		ModelData input = new ModelData();
		input.model = model;
		input.relationMention = relationMention;
		input.count = (null == counter) ? tmpCount.getAndIncrement() : counter.getAndIncrement();
		
		methodReference.apply(input);
	}
	
	static public void recordUnmappedRelations(String sourcetype)
	{
		AtomicInteger count = missingRelations.get(sourcetype);
		if (count !=null)
			count.incrementAndGet();
		else 
			missingRelations.put(sourcetype, new AtomicInteger(1));
	}
	
	protected Dataset addToRdfData_internal(final Dataset dataset) {
		
		String langCode = this.document.langCode;
		if (null == langCode) {
			langCode = "en"; // TODO km: introduce default language code for dataset
		}
		
		for (RelationMention relationMention : document.relationMentions) {
			if (2 != relationMention.entities.size()) {
				throw new RuntimeException("Did get uneven number ("
						+ relationMention.entities.size()
						+ ") of entities: " + relationMention.entities +"\n\t in Relation: " + relationMention);
			}
			
			Model relationModel = this.createNewModel();
			
			for (String relationType : relationMention.relation.types) {
				this.createRelationTriple(relationType, relationMention, relationModel);
			}
			
			this.createRelationTriple(RelationGenerator.HandleEntityLabels, relationMention, relationModel);
			this.createRelationTriple(RelationGenerator.HandleEntityTypes, relationMention, relationModel);
			
			String uniqueId = this.document.entityIdGenerator.getUniqueId();
			String relationMetadataUri = this.graphName + uniqueId;
			dataset.addNamedModel(relationMetadataUri, relationModel);
			
			Model metadataModel = this.createNewModel();
			relationMention.generatedUri = relationMetadataUri;
			relationMention.generatedId = uniqueId;

			this.createRelationTriple(RelationGenerator.HandleProvenance, relationMention, metadataModel);			
			if (false == metadataModel.isEmpty()) {
				dataset.addNamedModel(this.graphName, metadataModel);
			}
		}
		
		return dataset;		
	}
	
	static class ModelData {
		/** number of times this method was called */
		Integer count;
		/** model which can be used to fill RDF data */
		Model model;
		/** relation of concern */
		RelationMention relationMention;
	}
	
	/**
	 * This method can be used to create relationships for companies which provide a technology
	 * 
	 * @param argument
	 * @return
	 */
	protected int handleCompanyTechnology(ModelData argument) {
		Objects.requireNonNull(argument);
		
		RelationMention relationMention = argument.relationMention;
		
		Mention organisation = (relationMention.entities.get(0).types.contains(W3COrg.organization)
				? relationMention.entities.get(0) : relationMention.entities.get(1));
		
		Mention product = (relationMention.entities.get(1).types.contains(W3COrg.organization)
				? relationMention.entities.get(0) : relationMention.entities.get(1));
		
		String leftEntity = organisation.generatedUri;
		String rightEntity = product.generatedUri;
				
		RDFNode object = argument.model.createResource(rightEntity);
		this.addStatement(leftEntity, CorpDbpedia.providesProduct, object, argument.model);
		
		return 0;
	}
	
	protected int handleLabel(final ModelData argument) {
		Objects.requireNonNull(argument);
		
		Model relationModel = argument.model;
		
		for (Mention entity : argument.relationMention.entities) {					
			String leftLabel = (null == entity.text) ? entity.textNormalized : entity.text;	
			if (null != leftLabel) {						
				RDFNode object = relationModel.createLiteral(leftLabel, this.document.langCode);			
				this.addStatement(entity.generatedUri, RDFS.label.getURI(), object, relationModel);
			}
		}
		
		return 0;
	}
	
	protected int handleTypes(final ModelData argument) {
		Objects.requireNonNull(argument);
		
		Model relationModel = argument.model;
		
		for (Mention entity : argument.relationMention.entities) {
			// add type triples
			for (String type : entity.types) {	
				
				RDFNode object = relationModel.createResource(type);				
				this.addStatement(entity.generatedUri, RDF.type.getURI(), object, relationModel);
			}
		}
		
		return 0;
	}
	
	protected int handleProvenance(final ModelData argument) {
		Objects.requireNonNull(argument);
		
		RelationMention relationMention = argument.relationMention;
		Model metadataModel = argument.model;
		
		String uriString = argument.relationMention.generatedUri;
		
		for (Provenance provenance : relationMention.provenance) {
			if (null != provenance.annotator) {
				
				RDFNode annotatorObject = metadataModel.createResource(provenance.annotator);
				this.addStatement(uriString, NIF21Format.RDF_PROPERTY_ANNOTATOR, annotatorObject, metadataModel);
				
				RDFNode scoreObject = metadataModel.createTypedLiteral(provenance.score);
				this.addStatement(uriString, NIF21Format.RDF_PROPERTY_CONFIDENCE, scoreObject, metadataModel);
			}
			
			if (null != provenance.source) {			
				RDFNode sourceObject = metadataModel.createResource(provenance.source);
				this.addStatement(uriString, W3CProvenance.hadPrimarySource, sourceObject, metadataModel);
			}
		}
		
//		for (Provenance provenance : this.document.provenanceSet) {
//			System.out.println("provenance: " + provenance);
//		}
		
		return 0;
	}
	
	protected int handleHeadquarters(final ModelData argument) {
		Objects.requireNonNull(argument);
		
		RelationMention relationMention = argument.relationMention;
		Model relationModel = argument.model;
		
		Mention organisation = (relationMention.entities.get(1).types.contains(W3COrg.site)
				? relationMention.entities.get(0) : relationMention.entities.get(1));
		Mention headquarter = (relationMention.entities.get(0).types.contains(W3COrg.site)
				? relationMention.entities.get(0) : relationMention.entities.get(1));
		
		String organisationUri = organisation.generatedUri;
		
//		String locationUriString = headquarter.generatedUri + "/site" + argument.count;
		RDFNode locationUri = relationModel.createResource(headquarter.generatedUri);
		this.addStatement(organisationUri, CorpDbpedia.hasHeadquarterSite, locationUri, relationModel);
		
		String locationLabel = (null != headquarter.text) ?  headquarter.text :  headquarter.textNormalized;
		RDFNode addressLiteral = relationModel.createTypedLiteral(locationLabel, this.document.langCode);
		this.addStatement(headquarter.generatedUri, W3COrg.siteAddress, addressLiteral, relationModel);
		
		return 0;
	}
	
	protected int handleOrganizationLeadership(final ModelData argument) {
		Objects.requireNonNull(argument);
		
		String leftEntity = argument.relationMention.entities.get(0).generatedUri;
		String rightEntity = argument.relationMention.entities.get(1).generatedUri;
				
		RDFNode object = argument.model.createResource(rightEntity);
		this.addStatement(leftEntity, W3COrg.headOf, object, argument.model);
		
		return 0;
	}
	
	protected int handleCompanyRelationship(final ModelData argument) {
		Objects.requireNonNull(argument);
		
		Mention leftEntity = argument.relationMention.entities.get(0);
		
		String leftEntityString = argument.relationMention.entities.get(0).generatedUri;
		String rightEntityString = argument.relationMention.entities.get(1).generatedUri;
		
		if (leftEntity.types.contains("dbpedia.org/ontology/parentCompany")) {
			
			RDFNode childCompany = argument.model.createResource(rightEntityString);
			this.addStatement(leftEntityString, W3COrg.hasSubOrganization, childCompany, argument.model);
			
			RDFNode parentCompany = argument.model.createResource(leftEntityString);
			this.addStatement(rightEntityString, W3COrg.subOrganizationOf, parentCompany, argument.model);
		} else {
			
			RDFNode childCompany = argument.model.createResource(leftEntityString);
			this.addStatement(rightEntityString, W3COrg.hasSubOrganization, childCompany, argument.model);
			
			RDFNode parentCompany = argument.model.createResource(rightEntityString);
			this.addStatement(leftEntityString, W3COrg.subOrganizationOf, parentCompany, argument.model);
		}		
		
		return 0;
	}
	
	protected void addStatement(final String subjectUri, final String predicateUri, final RDFNode rdfNode, final Model model) {
		Objects.requireNonNull(subjectUri);
		Objects.requireNonNull(predicateUri);
		Objects.requireNonNull(rdfNode);
		Objects.requireNonNull(model);
		
		Resource subject = model.createResource(subjectUri);
		Property predicate = model.createProperty(predicateUri);				
		RDFNode object = rdfNode;
			
		Statement statement = new StatementImpl(subject, predicate, object);			
		model.add(statement);		
	}
}
