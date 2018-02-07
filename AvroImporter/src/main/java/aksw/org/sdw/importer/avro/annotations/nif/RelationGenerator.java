package aksw.org.sdw.importer.avro.annotations.nif;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import aksw.org.sdw.importer.avro.annotations.*;
import aksw.org.sdw.importer.avro.annotations.dfki.Dfki2SdwKgMapper;
import aksw.org.sdw.importer.avro.annotations.ids.UniqueIdGenerator;
import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.base.Sys;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.apache.velocity.runtime.directive.Foreach;
import org.apache.xerces.impl.io.MalformedByteSequenceException;
import org.apache.xerces.stax.events.StartElementImpl;
import org.nlp2rdf.nif21.NIF21Format;

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

	final Map<String, String> prefixMap = new HashMap<String,String>();

	/** relation function map */
	final Map<String, Function<ModelData, Integer>> relationFunctionMap = new HashMap<>();

	/** count of how often a method was called */
	final ConcurrentMap<String, AtomicInteger> relationFunctionCount = new ConcurrentHashMap<>();

	private Object entity;

	static final public Map<String, AtomicInteger> missingRelations = new HashMap<>();
	static final public Map<String, AtomicInteger> strangeRelations = new HashMap<>();
	static final public Map<String, AtomicInteger> missingR = new HashMap<>();

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
	final static String CompanyCustomer = "CompanyCustomer";

	final static String CompanyFoundation = "CompanyFoundation";

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
		relationFunctionMap.put(RelationGenerator.CompanyCustomer, this::handleCompanyCustomer);
		relationFunctionMap.put(RelationGenerator.Acquisition,	this::handleAcquisition);
		relationFunctionMap.put(RelationGenerator.CompanyFoundation,	this::handleCompanyFoundation);
	}

	public RelationGenerator(final String graphName, final Document document, final DocRdfGenerator relationGenerator) {
		super(graphName, relationGenerator);
		this.document = document;

		prefixMap.put("dbc", "http://corp.dbpedia.org/ontology");
	}

	private Dataset dataset = null;
	/**
	 * 
	 * @param relationType
	 * @param relationMention
	 * @param model
	 *            the jena model which will contain the output triples as 'return
	 *            value' for this relation
	 */
	protected void createRelationTriple(final String relationType, final RelationMention relationMention,
			final Model model, final AtomicReference<String> tripleId) {
		Objects.requireNonNull(relationType, "No relation type name passed in");
		Objects.requireNonNull(model, "Model is null");

		// gets count for how often method was called
		AtomicInteger tmpCount = new AtomicInteger(0);
		AtomicInteger counter = relationFunctionCount.putIfAbsent(relationType, tmpCount);

		ModelData input = new ModelData();
		input.model = model;
		input.relationMention = relationMention;
		input.count = (null == counter) ? tmpCount.getAndIncrement() : counter.getAndIncrement();
		input.tripleId = tripleId;

		Function<ModelData, Integer> methodReference = this.relationFunctionMap.get(relationType);

		if (null == methodReference) {
			Level level = Level.WARNING;
			Logger.getGlobal().log(level,
					"Unknown relation type: " + relationType + " for relationMention: " + relationMention);
			recordUnmappedRelations(relationType);
		}
		else {
			methodReference.apply(input);
		}
	}

	static public void recordTypes(String relation) {
		AtomicInteger count = missingR.get(relation);
		if (count != null)
			count.incrementAndGet();
		else
			missingR.put(relation, new AtomicInteger(1));
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

	protected Dataset addToRdfData_internal(final Dataset dset) {

		this.dataset = dset;
		String langCode = this.document.langCode;
		if (null == langCode) {
			langCode = "en"; // TODO km: introduce default language code for dataset
		}

		for (RelationMention relationMention : document.relationMentions) {

			Model relationModel = this.createNewModel(); // Model 1
			Model metadataModel = this.createNewModel(); // Model 2

			boolean exceptThis = false;
			if(relationMention.relation.types.contains("CompanyFinancialEvent")) exceptThis = true;
			
			if (2 != relationMention.entities.size() && !exceptThis) {
//			if(true) {
				Level level = Level.WARNING;
				Logger.getGlobal().log(level,
						"Received nary relation of degree (" + relationMention.entities.size() + ") with entities: "
								+ relationMention.entities + "\n\t in Relation: " + relationMention.relation);
				recordStrangeRelationsNumber(relationMention.relation.textNormalized + relationMention.entities.size()
						+ "''" + relationMention.entities.keySet());
			}

			String uniqueId = this.document.entityIdGenerator.getUniqueId();
			// TODO solve /#
			// ID
			relationMention.generatedId = ( "" == document.id || null == document.id ) ?
					uniqueId :
					document.id;
			relationMention.generatedUri = RDFHelpers.createValidIRIfromBase(relationMention.generatedId, this.graphName.substring(0,this.graphName.length()-1));
//			String relationMetadataUri = relationMention.generatedUri;

			Property p = ResourceFactory.createProperty(CorpDbpedia.prefixOntology+"hasRelationMention");

			for (String relationType : relationMention.relation.types) {

				// binary relation
				Model binaryModel = this.createNewModel(); // Model 3
				AtomicReference<String> tripleId = new AtomicReference<String>("");
				this.createRelationTriple(relationType, relationMention, binaryModel, tripleId);
				if ( "" != tripleId.toString() ) {
					dataset.addNamedModel(tripleId.toString(), binaryModel);
					GlobalConfig.getInstance().addModel(binaryModel);
					Resource r = ResourceFactory.createResource(tripleId.toString());
					RDFNode rMuri = metadataModel.createResource(relationMention.relation.generatedUri);
					metadataModel.add(r,p,rMuri);
				}

				// raw relation
				ModelData rawModelData = new ModelData();
				rawModelData.model = relationModel;
				rawModelData.relationMention = relationMention;
				this.handleRawRelationship(rawModelData);
			}

			this.createRelationTriple(RelationGenerator.HandleEntityLabels, relationMention, relationModel, null);
			this.createRelationTriple(RelationGenerator.HandleEntityTypes, relationMention, relationModel, null);

			dataset.addNamedModel(relationMention.generatedUri, relationModel);
//			relationModel.write(System.out,"TURTLE");


//			this.createRelationTriple(RelationGenerator.HandleProvenance, relationMention, metadataModel, null);
			if (false == metadataModel.isEmpty()) {
				dataset.addNamedModel(this.graphName, metadataModel);
//				metadataModel.write(System.out, "TURTLE");
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
		/** triple Id */
		AtomicReference<String> tripleId;
	}

	/**
	 * This method can be used to create relationships for companies which provide a
	 * technology
	 * 
	 * @param argument
	 * @return
	 */
	protected int handleCompanyTechnology(ModelData argument) {
		argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+UUID.randomUUID().toString());

		Objects.requireNonNull(argument);
		RelationMention relationMention = argument.relationMention;

		Mention organisation = relationMention.entities.get("company");
		Mention product = relationMention.entities.get("product");
		// for beuth
		if (null == product)
			product = relationMention.entities.get("sensor");
		
		try {
			String str = relationMention.entities.get("type").generatedUri;
			//System.out.println("type "+str);
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

	protected int handleCompanyFoundation(ModelData argument) {
		argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+UUID.randomUUID().toString());

		Objects.requireNonNull(argument);
		RelationMention relationMention = argument.relationMention;

		Mention company = relationMention.entities.get("company");
		Mention founder = relationMention.entities.get("founder");

		if (null == company || null == founder) {
			Logger.getGlobal().log(Level.WARNING, "Failed Relation CompanyTechnology: " + relationMention.entities );
			return 0;
		}

		String leftEntity = company.generatedUri;
		String rightEntity = founder.generatedUri;

		RDFNode object = argument.model.createResource(rightEntity);
		this.addStatement(leftEntity, CorpDbpedia.hasFounder, object, argument.model);

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

	protected int handleTypes(final ModelData argument) {
		Objects.requireNonNull(argument);

		Model relationModel = argument.model;


		for (Mention entity : argument.relationMention.entities.values()) {
			// add type triples
			if (Dfki2SdwKgMapper.datatypeMapping.values().containsAll(entity.types)) continue;
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

				//ANNOTATOR
				RDFNode annotatorObject = metadataModel.createTypedLiteral(RDFHelpers.createValidIRIfromBase(provenance.annotator,"http://corp.dbpedia.org/annotator"));

				this.addStatement(uriString, NIF21Format.RDF_PROPERTY_ANNOTATOR, annotatorObject, metadataModel);

				RDFNode scoreObject = metadataModel.createTypedLiteral(provenance.score);
				this.addStatement(uriString, NIF21Format.RDF_PROPERTY_CONFIDENCE, scoreObject, metadataModel);
			}

			if (null != provenance.source) {
				RDFNode sourceObject = metadataModel.createResource(provenance.source);
				this.addStatement(uriString, W3CProvenance.hadPrimarySource, sourceObject, metadataModel);
			}

			if ( null != this.document.date ) {
				this.addStatementWithLiteral(uriString, CorpDbpedia.hasDate,this.document.date, XSDDatatype.XSDdateTime , metadataModel);
			}

			if ( null != this.document.docType ) {
				RDFNode docTypeliteral = metadataModel.createLiteral(this.document.docType.toString(), document.langCode);
				this.addStatement(uriString, CorpDbpedia.hasDocType, docTypeliteral, metadataModel);
			}
		}

		return 0;
	}

	protected int handleHeadquarters(final ModelData argument) {
		argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+UUID.randomUUID().toString());
		Objects.requireNonNull(argument);

		RelationMention relationMention = argument.relationMention;
		Model relationModel = argument.model;


		Mention organisation = relationMention.entities.get("company");
		Mention headquarter = relationMention.entities.get("headquarter");

		String organisationUri = organisation.generatedUri;

		// String locationUriString = headquarter.generatedUri + "/site" +

		RDFNode locationUri = relationModel.createResource(headquarter.generatedUri);
		this.addStatement(organisationUri, CorpDbpedia.hasHeadquarterSite, locationUri, relationModel);

		String locationLabel = (null != headquarter.text) ? headquarter.text : headquarter.textNormalized;
		RDFNode addressLiteral = relationModel.createLiteral(locationLabel, this.document.langCode);
		this.addStatement(headquarter.generatedUri, W3COrg.siteAddress, addressLiteral, relationModel);

		return 0;
	}

	protected int handleOrganizationLeadership(final ModelData argument) {
		argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+UUID.randomUUID().toString());
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

		RDFNode object = arg.model.createResource(CorpDbpedia.prefixOntology+"RawRelationMention");//+rm.relation.textNormalized);
		
		this.addStatement(rm.relation.generatedUri, RDF.type.getURI() , object, arg.model);
		
		RDFNode label = arg.model.createLiteral(rm.relation.textNormalized, document.langCode);
		this.addStatement(rm.relation.generatedUri, RDFS.label.getURI(), label, arg.model);

		String attrTo = GlobalConfig.getInstance().getWasAttributedTo();

		RDFNode attributedTo = arg.model.createResource(CorpDbpedia.prefix+"crawler/"+attrTo);
		this.addStatement(rm.relation.generatedUri, "http://www.w3.org/ns/prov#wasAttributedTo", attributedTo, arg.model);

//		RDFNode attributedLabel = arg.model.createLiteral(attrTo, this.document.langCode);
//		this.addStatement(CorpDbpedia.prefix+"crawler/"+attrTo, RDFS.label.getURI(), attributedLabel,arg.model);

		int memberPosition = 1;
		for (String key : rm.entities.keySet()) {
			Mention mem = rm.entities.get(key);
			String suffix = "__memberRole__"+String.format("%03d", memberPosition);
			RDFNode member = arg.model.createResource(rm.relation.generatedUri+suffix);
			this.addStatement(rm.relation.generatedUri, CorpDbpedia.hasRelationMember , member, arg.model);

			// a  dbc:MemberRoleResource;
			this.addStatement(member.toString(), RDF.type.getURI(),arg.model.createResource(CorpDbpedia.memberRoleResource), arg.model);
			// :memberPosition
			this.addStatementWithLiteral(member.toString(), CorpDbpedia.relationMemberPosition,String.valueOf(memberPosition), XSDDatatype.XSDnonNegativeInteger, arg.model);
			// :memberRole
			this.addStatement(member.toString(), CorpDbpedia.relationMemberRole,arg.model.createLiteral(key,document.langCode),arg.model);
			// :member
			this.addStatement(member.toString(), CorpDbpedia.relationMember,arg.model.createResource(mem.generatedUri),arg.model);
			// :mentioned  ## link to concept mention
			int offset_beginn = rm.entities.get(key).span.start;
			int offset_end = rm.entities.get(key).span.end;
			String mentionedMember = new Formatter().format("%s#offset_%d_%d",document.uri+"?lid="+GlobalConfig.getInstance()
					.makeNifHash(rm.entities.get(key), document),offset_beginn,offset_end).toString();
//			String mentionedMember = new Formatter().format("%s#offset_%d_%d",document.uri,offset_beginn,offset_end).toString();
//			rm.entities.get(key).provenanceSet
//			mentionedMember += "?lid="+GlobalConfig.getInstance()
//					.makeNifHash(rm.entities.get(key), document);
			this.addStatement(member.toString(), CorpDbpedia.relationMemberMentioned,arg.model.createResource(mentionedMember),arg.model);

			memberPosition++;
		}

		return 0;
	}

	protected int handleCompanyFinancialEvent(final ModelData argument) {
		//argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+UUID.randomUUID().toString());

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

		RDFNode event = argument.model.createResource(leftEntityString);

		// SMR Triple
		this.addStatement(rightEntityString, CorpDbpedia.hasFinancialEvent, event , argument.model);
		argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+
		GlobalConfig.getInstance().globalFactHash(rightEntityString, CorpDbpedia.hasFinancialEvent,event));

		try {
			dateString = argument.relationMention.entities.get("date").textNormalized;
//			System.out.println("DATE FOUND");
			//DONE
			RDFNode literal = argument.model.createLiteral(dateString, this.document.langCode);
			this.addStatement(leftEntityString, CorpDbpedia.hasDate, literal, argument.model);
		} catch( Exception e) {
//			System.out.println("DATE ERROR");
			return 0;
		}
		return 0;
	}

	protected int handleCompanyRelationship(final ModelData argument) {
		argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+UUID.randomUUID().toString());
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
		argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+UUID.randomUUID().toString());
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
		argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+UUID.randomUUID().toString());
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
		argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+UUID.randomUUID().toString());
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
		argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+UUID.randomUUID().toString());
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
		argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+UUID.randomUUID().toString());
		Objects.requireNonNull(argument);
		RelationMention relationMention = argument.relationMention;

		Mention acquired = argument.relationMention.entities.get("acquired");
		if ( null == acquired )
			acquired = relationMention.entities.get("company_beingacquired");
		Mention buyer = relationMention.entities.get("buyer");
		if ( null == buyer )
			buyer = relationMention.entities.get("company_acquirer");

		String leftEntityString = buyer.generatedUri;
		String rightEntityString = acquired.generatedUri;

		// DONE: COMPANY x COMPANY
		RDFNode childCompany = argument.model.createResource(rightEntityString);
		this.addStatement(leftEntityString, CorpDbpedia.acquired, childCompany, argument.model);

		RDFNode parentCompany = argument.model.createResource(leftEntityString);
		this.addStatement(rightEntityString, CorpDbpedia.acquiredBy, parentCompany, argument.model);

		// keys: acquired, buyer
		return 0;
	}

	protected  int handleCompanyCustomer( final ModelData argument ) {
		argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+UUID.randomUUID().toString());
		Objects.requireNonNull(argument);
		RelationMention relationMention = argument.relationMention;

		//Kaeufer
		Mention customer = relationMention.entities.get("customer");
		//Anbieter
		Mention provider = relationMention.entities.get("company");

		if( customer == null || provider == null ) return 0;
		String leftEntityString = provider.generatedUri;
		String rightEntityString = customer.generatedUri;

		RDFNode customerCompany = argument.model.createResource(rightEntityString);
		this.addStatement(leftEntityString, CorpDbpedia.customer, customerCompany, argument.model);

		RDFNode providerCompany = argument.model.createResource(leftEntityString);
		this.addStatement(rightEntityString, CorpDbpedia.customerOf, providerCompany, argument.model);

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
	
	protected void addStatementWithLiteral(final String subjectUri, final String predicateUri, Object lex, final RDFDatatype datatype,
			final Model model) {
		Objects.requireNonNull(subjectUri);
		Objects.requireNonNull(predicateUri);
		Objects.requireNonNull(lex);
		//Objects.requireNonNull(datatype);
		Objects.requireNonNull(model);

		Resource subject = model.createResource(subjectUri);
		Property predicate = model.createProperty(predicateUri);

		if(null == datatype) model.addLiteral(subject,predicate,lex);
		else model.add(subject, predicate, (String) lex, datatype);
	}

	private String generateTripleId(String triple ) {
		String hashed = null;
		try {
			hashed = String.format("%064x", new java.math.BigInteger(1, MessageDigest.getInstance("SHA-256").digest(triple.getBytes("UTF-8"))));
		} catch ( Exception e ) {
			e.printStackTrace();
			System.exit(0);
		}
		return hashed;
	}


}
