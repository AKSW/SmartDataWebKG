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

import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.velocity.runtime.directive.Foreach;
import org.apache.xerces.impl.io.MalformedByteSequenceException;
import org.apache.xerces.stax.events.StartElementImpl;
import org.nlp2rdf.nif21.NIF21Format;

import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.Mention;
import aksw.org.sdw.importer.avro.annotations.Provenance;
import aksw.org.sdw.importer.avro.annotations.RelationMention;
import aksw.org.sdw.rdf.namespaces.CorpDbpedia;
import aksw.org.sdw.rdf.namespaces.RdfDataTypes;
import aksw.org.sdw.rdf.namespaces.W3COrg;
import aksw.org.sdw.rdf.namespaces.W3CProvenance;

/**
 * This class can be used to add actual graph data of extracted relationships.
 * In addition metadata for each triple is stored which is stored in a metadata
 * sub graph
 * 
 * @author kay
 *
 */
public class RelationGenerator extends DocRdfGenerator {

	/** input document */
	final Document document;

	/** relation function map */
	final Map<String, Function<ModelData, Integer>> relationFunctionMap = new HashMap<>();

	/** count of how often a method was called */
	final ConcurrentMap<String, AtomicInteger> relationFunctionCount = new ConcurrentHashMap<>();

	private Object entity;

	static final public Map<String, AtomicInteger> missingRelations = new HashMap<>();
	static final public Map<String, AtomicInteger> strangeRelations = new HashMap<>();

	final static String HandleEntityLabels = "HandleEntityLabels";
	final static String HandleEntityTypes = "HandleEntityTypes";
	final static String HandleProvenance = "HandleProvenance";

	final static String CompanyTechnology = "CompanyTechnology";
	final static String CompanyProvidesProduct = "CompanyProvidesProduct";
	final static String CompanyFinancialEvent = "CompanyFinancialEvent";

	final static String CompanyHeadquarters = "CompanyHeadquarters";
	final static String OrganizationLeadership = "OrganizationLeadership";
	final static String CompanyRelationship = "CompanyRelationship";
	final static String RawRelationship = "RawRelationship";

	final static String SpinOff = "SpinOff";
	final static String CompanyProject = "CompanyProject";
	final static String Disaster = "Disaster";
	final static String CompanyIndustry = "CompanyIndustry";
	final static String Acquisition = "Acquisition";

	public RelationGenerator(final String graphName, final Document document) {
		this(graphName, document, (DocRdfGenerator) null);

		relationFunctionMap.put(RelationGenerator.CompanyFinancialEvent, this::handleCompanyFinancialEvent);
		relationFunctionMap.put(RelationGenerator.CompanyTechnology, this::handleCompanyTechnology);
		relationFunctionMap.put(RelationGenerator.CompanyProvidesProduct, this::handleCompanyTechnology);
		relationFunctionMap.put(RelationGenerator.HandleEntityLabels, this::handleLabel);
		relationFunctionMap.put(RelationGenerator.HandleEntityTypes, this::handleTypes);
		relationFunctionMap.put(RelationGenerator.HandleProvenance, this::handleProvenance);
		relationFunctionMap.put(RelationGenerator.CompanyHeadquarters, this::handleHeadquarters);
		relationFunctionMap.put(RelationGenerator.OrganizationLeadership, this::handleOrganizationLeadership);
		relationFunctionMap.put(RelationGenerator.CompanyRelationship, this::handleCompanyRelationship);

		relationFunctionMap.put(RelationGenerator.SpinOff, this::handleSpinOff);
		relationFunctionMap.put(RelationGenerator.CompanyProject, this::handleCompanyProject);
		relationFunctionMap.put(RelationGenerator.Disaster, this::handleDisaster);
		relationFunctionMap.put(RelationGenerator.CompanyIndustry, this::handleCompanyIndustry);
		relationFunctionMap.put(RelationGenerator.Acquisition,	this::handleAcquisition);
	}

	public RelationGenerator(final String graphName, final Document document, final DocRdfGenerator relationGenerator) {
		super(graphName, relationGenerator);
		this.document = document;
	}

	/**
	 * 
	 * @param relationType
	 * @param relationMention
	 * @param model
	 *            the jena model which will contain the output triples as 'return
	 *            value' for this relation
	 */
	protected void createRelationTriple(final String relationType, final RelationMention relationMention,
			final Model model) {
		Objects.requireNonNull(relationType, "No relation type name passed in");
		Objects.requireNonNull(model, "Model is null");

		Function<ModelData, Integer> methodReference = this.relationFunctionMap.get(relationType);
		if (null == methodReference) {
			Level level = Level.WARNING;
			Logger.getGlobal().log(level,
					"Unknown relation type: " + relationType + " for relationMention: " + relationMention);
			recordUnmappedRelations(relationType);
			methodReference = this::handleRawRelationship;
			// throw new RuntimeException("Unknown relation type: " + relationType + " for
			// relationMention: " + relationMention);
		}

		// gets count for how often method was called
		AtomicInteger tmpCount = new AtomicInteger(0);
		AtomicInteger counter = relationFunctionCount.putIfAbsent(relationType, tmpCount);

		ModelData input = new ModelData();
		input.model = model;
		input.relationMention = relationMention;
		input.count = (null == counter) ? tmpCount.getAndIncrement() : counter.getAndIncrement();

		//if (relationMention.generatedUri == null ) System.out.println("NULL"); else System.out.println(relationMention.generatedUri);;
		methodReference.apply(input);
	}

	static public void recordUnmappedRelations(String sourcetype) {
		AtomicInteger count = missingRelations.get(sourcetype);
		if (count != null)
			count.incrementAndGet();
		else
			missingRelations.put(sourcetype, new AtomicInteger(1));
	}

	static public void recordStrangeRelationsNumber(String sourcetype) {
		System.out.println("record nary: " + sourcetype);
		AtomicInteger count = strangeRelations.get(sourcetype);
		if (count != null)
			count.incrementAndGet();
		else
			strangeRelations.put(sourcetype, new AtomicInteger(1));
	}

	protected Dataset addToRdfData_internal(final Dataset dataset) {

		String langCode = this.document.langCode;
		if (null == langCode) {
			langCode = "en"; // TODO km: introduce default language code for dataset
		}

		for (RelationMention relationMention : document.relationMentions) {
			
			//System.out.println(relationMention.relation.types+" "+relationMention.entities.keySet());
			Model relationModel = this.createNewModel();
			boolean exceptThis = false;
			//TODO exceptThis betters
			if(relationMention.relation.types.contains("CompanyFinancialEvent")) exceptThis = true;
			
			if (2 != relationMention.entities.size() && !exceptThis) {
				Level level = Level.WARNING;
				Logger.getGlobal().log(level,
						"Received nary relation of degree (" + relationMention.entities.size() + ") with entities: "
								+ relationMention.entities + "\n\t in Relation: " + relationMention.relation);
				recordStrangeRelationsNumber(relationMention.relation.textNormalized + relationMention.entities.size()
						+ "''" + relationMention.entities.keySet());
				return dataset;
				// throw new RuntimeException("Did get uneven number ("
				// + relationMention.entities.size()
				// + ") of entities: " + relationMention.entities +"\n\t in Relation: " +
				// relationMention);
			}

			// Model relationModel = this.createNewModel();

			String uniqueId = this.document.entityIdGenerator.getUniqueId();
			String relationMetadataUri = this.graphName + uniqueId;
			
			relationMention.generatedUri = relationMetadataUri;
			relationMention.generatedId = uniqueId;
			
			for (String relationType : relationMention.relation.types) {
				System.out.println(relationType+" "+relationMention.entities.keySet());
				this.createRelationTriple(relationType, relationMention, relationModel);
			}

			this.createRelationTriple(RelationGenerator.HandleEntityLabels, relationMention, relationModel);
			this.createRelationTriple(RelationGenerator.HandleEntityTypes, relationMention, relationModel);


			dataset.addNamedModel(relationMetadataUri, relationModel);

			Model metadataModel = this.createNewModel();


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
	 * This method can be used to create relationships for companies which provide a
	 * technology
	 * 
	 * @param argument
	 * @return
	 */
	protected int handleCompanyTechnology(ModelData argument) {
		
		Objects.requireNonNull(argument);
		RelationMention relationMention = argument.relationMention;

		Mention organisation = relationMention.entities.get("company");
		Mention product = relationMention.entities.get("product");
		// for beuth
		if (null == product)
			product = relationMention.entities.get("sensor");
		
		try {
			String str = relationMention.entities.get("type").generatedUri;
			System.out.println("type "+str);
		} catch( Exception e ) {
			
		}

		if (null == product || null == organisation) {
			Logger.getGlobal().log(Level.WARNING, "Failed Relation CompanyTechnology: " + relationMention.entities );
			return 0;
		}

		String leftEntity = organisation.generatedUri;
		String rightEntity = product.generatedUri;

		RDFNode object = argument.model.createResource(rightEntity);
		this.addStatement(leftEntity, CorpDbpedia.providesProduct, object, argument.model);

		return 0;
	}

	protected int handleLabel(final ModelData argument) {
		Objects.requireNonNull(argument);

		Model relationModel = argument.model;

		for (Mention entity : argument.relationMention.entities.values()) { // TODO check whether it is still correct
																			// after changing entities from list to MAP
			String leftLabel = (null == entity.text) ? entity.textNormalized : entity.text;
			if (null != leftLabel) {
				RDFNode object = relationModel.createLiteral(leftLabel, this.document.langCode);
				this.addStatement(entity.generatedUri, RDFS.label.getURI(), object, relationModel);
			}
		}

		return 0;
	}

	protected int handleTypes(final ModelData argument) { // check whether it is still correct after changing entities
														  // from list to MAP
		Objects.requireNonNull(argument);

		Model relationModel = argument.model;

		for (Mention entity : argument.relationMention.entities.values()) {
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

				RDFNode annotatorObject = metadataModel.createTypedLiteral(provenance.annotator);

				this.addStatement(uriString, NIF21Format.RDF_PROPERTY_ANNOTATOR, annotatorObject, metadataModel);

				RDFNode scoreObject = metadataModel.createTypedLiteral(provenance.score);
				this.addStatement(uriString, NIF21Format.RDF_PROPERTY_CONFIDENCE, scoreObject, metadataModel);
			}

			if (null != provenance.source) {
				RDFNode sourceObject = metadataModel.createResource(provenance.source);
				this.addStatement(uriString, W3CProvenance.hadPrimarySource, sourceObject, metadataModel);
			}
		}

		// for (Provenance provenance : this.document.provenanceSet) {
		// System.out.println("provenance: " + provenance);
		// }

		return 0;
	}

	protected int handleHeadquarters(final ModelData argument) {
		Objects.requireNonNull(argument);

		RelationMention relationMention = argument.relationMention;
		Model relationModel = argument.model;

		// Mention organisation =
		// (relationMention.entities.get(1).types.contains(W3COrg.site)
		// ? relationMention.entities.get(0) : relationMention.entities.get(1));
		// Mention headquarter =
		// (relationMention.entities.get(0).types.contains(W3COrg.site)
		// ? relationMention.entities.get(0) : relationMention.entities.get(1));

		Mention organisation = relationMention.entities.get("company");
		Mention headquarter = relationMention.entities.get("headquarter");

		String organisationUri = organisation.generatedUri;

		// String locationUriString = headquarter.generatedUri + "/site" +
		// argument.count;
		RDFNode locationUri = relationModel.createResource(headquarter.generatedUri);
		this.addStatement(organisationUri, CorpDbpedia.hasHeadquarterSite, locationUri, relationModel);

		String locationLabel = (null != headquarter.text) ? headquarter.text : headquarter.textNormalized;
		RDFNode addressLiteral = relationModel.createTypedLiteral(locationLabel, this.document.langCode);
		this.addStatement(headquarter.generatedUri, W3COrg.siteAddress, addressLiteral, relationModel);

		return 0;
	}

	protected int handleOrganizationLeadership(final ModelData argument) {
		Objects.requireNonNull(argument);

		String leftEntity = argument.relationMention.entities.get("person").generatedUri;
		String rightEntity = argument.relationMention.entities.get("organization").generatedUri;

		RDFNode object = argument.model.createResource(rightEntity);
		this.addStatement(leftEntity, W3COrg.headOf, object, argument.model);

		return 0;
	}

	protected int handleRawRelationship(final ModelData arg) {
		Objects.requireNonNull(arg);
		
		

		RelationMention rm = arg.relationMention;
		Model m = arg.model;
		
		RDFNode object = arg.model.createResource("http://corp.dbpedia.org/naryRelation/"+rm.relation.textNormalized);
		
		this.addStatement(rm.relation.generatedUri, RDF.type.getURI() , object, arg.model);
		
		for (Mention mem : rm.entities.values()) {
			RDFNode member = arg.model.createResource(mem.generatedUri);
			this.addStatement(rm.relation.generatedUri, "http://corp.dbpedia.org/hasRelationMember" , member, arg.model);
		}
		
//		Resource relUri = m.createResource(rm.generatedUri);
//		relUri.addLiteral(m.createProperty("http://corp.dbpedia.org/naryRelation"), rm.relation.generatedUri);

//		int count = 0;
//		for (Mention e : arg.relationMention.entities.values()) {
//			relUri.addLiteral(m.createProperty("http://UNMAPPED.ER/hasRelationEntity#" + count), e.generatedUri);
//			count++;
//		}

		// Mention leftEntity = argument.relationMention.entities.get(0);
		//
		// String leftEntityString =
		// argument.relationMention.entities.get(0).generatedUri;
		// String rightEntityString =
		// argument.relationMention.entities.get(1).generatedUri;
		//
		// if (leftEntity.types.contains("dbpedia.org/ontology/parentCompany")) {
		//
		// RDFNode childCompany = argument.model.createResource(rightEntityString);
		// this.addStatement(leftEntityString, W3COrg.hasSubOrganization, childCompany,
		// argument.model);
		//
		// RDFNode parentCompany = argument.model.createResource(leftEntityString);
		// this.addStatement(rightEntityString, W3COrg.subOrganizationOf, parentCompany,
		// argument.model);
		// } else {
		//
		// RDFNode childCompany = argument.model.createResource(leftEntityString);
		// this.addStatement(rightEntityString, W3COrg.hasSubOrganization, childCompany,
		// argument.model);
		//
		// RDFNode parentCompany = argument.model.createResource(rightEntityString);
		// this.addStatement(leftEntityString, W3COrg.subOrganizationOf, parentCompany,
		// argument.model);
		// }

		return 0;
	}

	protected int handleCompanyFinancialEvent(final ModelData argument) {

		Objects.requireNonNull(argument);
		
		String leftEntityString;
		String rightEntityString;
		String dateString;
		try { 
			leftEntityString = argument.relationMention.entities.get("event_type").generatedUri;
			rightEntityString = argument.relationMention.entities.get("company").generatedUri;
		} catch ( Exception e ) {
			return 0;
		}
		RDFNode event = argument.model.createResource(rightEntityString);
		this.addStatement(leftEntityString, CorpDbpedia.hasFinancialEvent, event, argument.model);
		
		try {
			dateString = argument.relationMention.entities.get("date").textNormalized;
//			System.out.println("DATE FOUND");
			//DONE
			this.addStatementWithLiteral(leftEntityString, CorpDbpedia.hasDate, dateString, RDF.dtLangString, argument.model);
		} catch( Exception e) {
//			System.out.println("DATE ERROR");
			return 0;
		}
		return 0;
	}

	protected int handleCompanyRelationship(final ModelData argument) {
		Objects.requireNonNull(argument);

		String leftEntityString = argument.relationMention.entities.get("parent").generatedUri;

		String rightEntityString = argument.relationMention.entities.get("child").generatedUri;

		RDFNode childCompany = argument.model.createResource(rightEntityString);
		this.addStatement(leftEntityString, W3COrg.hasSubOrganization, childCompany, argument.model);

		RDFNode parentCompany = argument.model.createResource(leftEntityString);
		this.addStatement(rightEntityString, W3COrg.subOrganizationOf, parentCompany, argument.model);

		return 0;
	}

	protected int handleSpinOff(final ModelData argument) {
		Objects.requireNonNull(argument);

		String leftEntityString = argument.relationMention.entities.get("parent").generatedUri;
		String rightEntityString = argument.relationMention.entities.get("child").generatedUri;

		// DONE: hasSpinOff and isSpinOff
		RDFNode childCompany = argument.model.createResource(rightEntityString);
		this.addStatement(leftEntityString, CorpDbpedia.hasSpinOff, childCompany, argument.model);

		RDFNode parentCompany = argument.model.createResource(leftEntityString);
		this.addStatement(rightEntityString, CorpDbpedia.isSpinOff, parentCompany, argument.model);

		// keys: parent, child
		return 0;
	}

	protected int handleCompanyProject(final ModelData argument) {
		Objects.requireNonNull(argument);
		
		String leftEntityString = argument.relationMention.entities.get("company").generatedUri;
		String rightEntityString = argument.relationMention.entities.get("project").generatedUri;

		// DONE: COMPANY x PROJECT
		RDFNode project = argument.model.createResource(rightEntityString);
		this.addStatement(leftEntityString, CorpDbpedia.hasProject, project, argument.model);

		RDFNode company = argument.model.createResource(leftEntityString);
		this.addStatement(rightEntityString, CorpDbpedia.isProjectOf, company, argument.model);

		// keys: project, company
		return 0;
	}

	protected int handleDisaster(final ModelData argument) {
		Objects.requireNonNull(argument);
		String leftEntityString;
		String rightEntityString;
		try {
			leftEntityString = argument.relationMention.entities.get("location").generatedUri;
			rightEntityString = argument.relationMention.entities.get("type").generatedUri;
			
		} catch(Exception e) {
			return 0;
		}
		
		// DONE: check relation
		RDFNode type = argument.model.createResource(rightEntityString);
		this.addStatement(leftEntityString, CorpDbpedia.hasDisaster, type, argument.model);

		// keys: location, type
		return 0;
	}

	protected int handleCompanyIndustry(final ModelData argument) {
		Objects.requireNonNull(argument);

		String leftEntityString = argument.relationMention.entities.get("company").generatedUri;
		String rightEntityString = argument.relationMention.entities.get("industry").generatedUri;

		RDFNode industry = argument.model.createResource(rightEntityString);
		this.addStatement(leftEntityString, CorpDbpedia.industry, industry, argument.model);

//		RDFNode parentCompany = argument.model.createResource(leftEntityString);
//		this.addStatement(rightEntityString, CorpDbpedia.prefixOntology+"isSpinOff", parentCompany, argument.model);

		// keys: company, industry http://corp.dbpedia.org/ontology#orgCategory
		return 0;
	}

	protected int handleAcquisition(final ModelData argument) {
		Objects.requireNonNull(argument);

		String leftEntityString = argument.relationMention.entities.get("acquired").generatedUri;
		String rightEntityString = argument.relationMention.entities.get("buyer").generatedUri;

		// DONE: COMPANY x COMPANY
		RDFNode childCompany = argument.model.createResource(rightEntityString);
		this.addStatement(leftEntityString, CorpDbpedia.acquired, childCompany, argument.model);

		RDFNode parentCompany = argument.model.createResource(leftEntityString);
		this.addStatement(rightEntityString, CorpDbpedia.acquiredBy, parentCompany, argument.model);

		// keys: acquired, buyer
		return 0;
	}

	protected void addStatement(final String subjectUri, final String predicateUri, final RDFNode rdfNode,
			final Model model) {
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
	
	protected void addStatementWithLiteral(final String subjectUri, final String predicateUri, String lex, final RDFDatatype datatype,
			final Model model) {
		Objects.requireNonNull(subjectUri);
		Objects.requireNonNull(predicateUri);
		Objects.requireNonNull(lex);
		Objects.requireNonNull(datatype);
		Objects.requireNonNull(model);

		Resource subject = model.createResource(subjectUri);
		Property predicate = model.createProperty(predicateUri);

		model.add(subject, predicate, lex, datatype);
	}
}
