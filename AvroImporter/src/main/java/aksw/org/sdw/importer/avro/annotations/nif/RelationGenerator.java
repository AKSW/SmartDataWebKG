package aksw.org.sdw.importer.avro.annotations.nif;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import aksw.org.sdw.importer.avro.annotations.*;
import aksw.org.sdw.importer.avro.annotations.dfki.Dfki2SdwKgMapper;
import aksw.org.sdw.importer.avro.annotations.ids.UniqueIdGenerator;
import com.sun.org.apache.regexp.internal.RE;
import org.apache.avro.JsonProperties;
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

import javax.jws.WebParam;

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
			final Model model) {
		Objects.requireNonNull(relationType, "No relation type name passed in");
		Objects.requireNonNull(model, "Model is null");

		// gets count for how often method was called
		AtomicInteger tmpCount = new AtomicInteger(0);
		AtomicInteger counter = relationFunctionCount.putIfAbsent(relationType, tmpCount);

		ModelData input = new ModelData();
		input.model = model;
		input.relationMention = relationMention;
		input.count = (null == counter) ? tmpCount.getAndIncrement() : counter.getAndIncrement();

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

    /**
     * TODO
     * @param dset
     * @return
     */
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
				this.createRelationTriple(relationType, relationMention, binaryModel);
				StmtIterator stmtIterator = binaryModel.listStatements();
				while(stmtIterator.hasNext()) {

					Statement statement = stmtIterator.nextStatement();
					String subj = statement.getSubject().getURI();
					String pred = statement.getPredicate().getURI();
					RDFNode obj = statement.getObject();

					String gfHashUri = this.graphName.substring(0,this.graphName.length()-1)+"#"+globalFactHash(subj,pred,obj);
					dataset.addNamedModel(gfHashUri, ModelFactory.createDefaultModel().add(statement));
					GlobalConfig.getInstance().addModel(binaryModel);
					Resource r = ResourceFactory.createResource(gfHashUri);
					RDFNode rMuri = metadataModel.createResource(relationMention.relation.generatedUri);
					metadataModel.add(r,p,rMuri);
				}

//				argument.tripleId.set(this.graphName.substring(0,this.graphName.length()-1)+"#"+
//						globalFactHash(organisationUri, CorpDbpedia.hasHeadquarterSite, locationUri));

				// raw relation
				ModelData rawModelData = new ModelData();
				rawModelData.model = relationModel;
				rawModelData.relationMention = relationMention;
				this.handleRawRelationship(rawModelData);
			}

			this.createRelationTriple(RelationGenerator.HandleEntityLabels, relationMention, relationModel);
			this.createRelationTriple(RelationGenerator.HandleEntityTypes, relationMention, relationModel);

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
//		AtomicReference<String> tripleId;
	}

    /**
     * TODO
     * @param argument
     * @return
     */
	protected int handleLabel(final ModelData argument) {
		Objects.requireNonNull(argument);

		Model relationModel = argument.model;

		for (Mention entity : argument.relationMention.entities.values()) { // TODO check whether it is still correct
																			// after changing entities from list to MAP
			String leftLabel = (null == entity.text) ? entity.textNormalized : entity.text;
			if (null != leftLabel) {
				RDFNode object = relationModel.createLiteral(leftLabel, this.document.langCode);
				this.addStatement(entity.generatedUri, RDFS.label.getURI(), object, relationModel);
				GlobalConfig.getInstance().getModel().add(relationModel.createResource(entity.generatedUri), relationModel.createProperty(RDFS.label.getURI()),object);
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
				GlobalConfig.getInstance().getModel().add(relationModel.createResource(entity.generatedUri), relationModel.createProperty(RDF.type.getURI()), object);
			}
		}

		return 0;
	}

    /**
     * TODO
     * @param argument
     * @return
     */
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

    /**
     * TODO
     * @param arg
     * @return
     */
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

	/**
	 * This method can be used to create relationships for companies which provide a
	 * technology
	 *
	 * @param argument
	 * @return
	 */
	protected int handleCompanyTechnology(ModelData argument) {
        Objects.requireNonNull(argument);
        boolean failure = this.addSemanticRelation("company",CorpDbpedia.providesProduct,"product",argument);
        if (!failure) this.addSemanticRelation("company",CorpDbpedia.providesProduct,"sensor",argument);
        return 0;
	}

    /**
     * TODO
     * @param argument
     * @return
     */
	protected int handleCompanyFoundation(ModelData argument) {
        Objects.requireNonNull(argument);
        try {
            // keys: company, founder
            String leftEntityUri = argument.relationMention.entities.get("company").generatedUri;
            String rightEntityUri = argument.relationMention.entities.get("founder").generatedUri;
            // hasFounder
            RDFNode object = argument.model.createResource(rightEntityUri);
            this.addStatement(leftEntityUri, CorpDbpedia.hasFounder, object, argument.model);
       } catch ( NullPointerException npe ) {
            logSemanticRelationNullPointer(argument);
       }
		return 0;
	}

    /**
     * TODO
     * @param argument
     * @return
     */
	protected int handleHeadquarters(final ModelData argument) {
		Objects.requireNonNull(argument);
        try {
            String leftEntityUri = argument.relationMention.entities.get("company").generatedUri;
            String rightEntityUri = argument.relationMention.entities.get("headquarter").generatedUri;
            // hasHeadquarterSite
            RDFNode rightEntity = argument.model.createResource(rightEntityUri);
            this.addStatement(leftEntityUri, CorpDbpedia.hasHeadquarterSite, rightEntity, argument.model);
            // siteAddress
            String locationLabel = (null != argument.relationMention.entities.get("headquarter").text) ?
                    argument.relationMention.entities.get("headquarter").text :
                    argument.relationMention.entities.get("headquarter").textNormalized;
            RDFNode addressLiteral = argument.model.createLiteral(locationLabel, this.document.langCode);
            this.addStatement(rightEntityUri, W3COrg.siteAddress, addressLiteral, argument.model);
        } catch ( NullPointerException npe ) {
            logSemanticRelationNullPointer(argument);
        }
		return 0;
	}

    /**
     * TODO
     * @param argument
     * @return
     */
	protected int handleOrganizationLeadership(final ModelData argument) {
		Objects.requireNonNull(argument);
        try {
            // keys: person, organization
			String leftEntityUri = argument.relationMention.entities.get("person").generatedUri;
			String rightEntityUri = argument.relationMention.entities.get("organization").generatedUri;
			// headOf
            RDFNode object = argument.model.createResource(rightEntityUri);
            this.addStatement(leftEntityUri, W3COrg.headOf, object, argument.model);
		} catch (NullPointerException npe ) {
            logSemanticRelationNullPointer(argument);
		}
		return 0;
	}

    /**
     * TODO
     * @param argument
     * @return
     */
	protected int handleCompanyFinancialEvent(final ModelData argument) {
		Objects.requireNonNull(argument);
		try {
		    // keys: event_type, company
			String leftEntityUri = argument.relationMention.entities.get("event_type").generatedUri;
			String rightEntityUri = argument.relationMention.entities.get("company").generatedUri;
			// hasFinancialEvent
            RDFNode event = argument.model.createResource(leftEntityUri);
            this.addStatement(rightEntityUri, CorpDbpedia.hasFinancialEvent, event , argument.model);
            // hasDATE ?
            try {
                String dateString = argument.relationMention.entities.get("date").textNormalized;
                RDFNode literal = argument.model.createLiteral(dateString, this.document.langCode);
                this.addStatement(leftEntityUri, CorpDbpedia.hasDate, literal, argument.model);
            } catch( Exception e) {
                return 0;
            }
		} catch ( NullPointerException npe ) {
            logSemanticRelationNullPointer(argument);
		}
		return 0;
	}

    /**
     * TODO
     * @param argument
     * @return
     */
	protected int handleCompanyRelationship(final ModelData argument) {
		Objects.requireNonNull(argument);
		try {
			String leftEntityUri= argument.relationMention.entities.get("parent").generatedUri;
			String rightEntityUri = argument.relationMention.entities.get("child").generatedUri;
			// hasSubOrganization
			RDFNode childCompany = argument.model.createResource(rightEntityUri);
			this.addStatement(leftEntityUri, W3COrg.hasSubOrganization, childCompany, argument.model);
			// subOrganuzationOf
			RDFNode parentCompany = argument.model.createResource(leftEntityUri);
			this.addStatement(rightEntityUri, W3COrg.subOrganizationOf, parentCompany, argument.model);
		} catch (NullPointerException npe ) {
            logSemanticRelationNullPointer(argument);
			return 0;
		}
		return 0;
	}

    /**
     * TODO
     * @param argument
     * @return
     */
	protected int handleSpinOff(final ModelData argument) {
		Objects.requireNonNull(argument);
		try {
			// keys: parent, child
			String leftEntityUri = argument.relationMention.entities.get("parent").generatedUri;
			String rightEntityUri= argument.relationMention.entities.get("child").generatedUri;
			// hasSpinOff
			RDFNode childCompany = argument.model.createResource(rightEntityUri);
			this.addStatement(leftEntityUri, CorpDbpedia.hasSpinOff, childCompany, argument.model);
			// isSpinOff
			RDFNode parentCompany = argument.model.createResource(leftEntityUri);
			this.addStatement(rightEntityUri, CorpDbpedia.isSpinOff, parentCompany, argument.model);
		} catch (NullPointerException npe) {
            logSemanticRelationNullPointer(argument);
		}
		return 0;
	}

    /**
     * TODO
     * @param argument
     * @return
     */
	protected int handleCompanyProject(final ModelData argument) {
		Objects.requireNonNull(argument);
		try {
            // keys: project, company
            String leftEntityUri = argument.relationMention.entities.get("company").generatedUri;
            String rightEntityUri = argument.relationMention.entities.get("project").generatedUri;
            // hasProject
            RDFNode project = argument.model.createResource(rightEntityUri);
            this.addStatement(leftEntityUri, CorpDbpedia.hasProject, project, argument.model);
            // isProjectOf
            RDFNode company = argument.model.createResource(leftEntityUri);
            this.addStatement(rightEntityUri, CorpDbpedia.isProjectOf, company, argument.model);
        } catch ( NullPointerException npe ) {
            logSemanticRelationNullPointer(argument);
        }
		return 0;
	}

    /**
     * TODO
     * @param argument
     * @return
     */
	protected int handleDisaster(final ModelData argument) {
		Objects.requireNonNull(argument);
//		this.addSemanticRelation("location",CorpDbpedia.hasDisaster,"type",argument);
		try {
            // keys: location, type
			String leftEntityUri = argument.relationMention.entities.get("location").generatedUri;
			String rightEntityUri = argument.relationMention.entities.get("type").generatedUri;
			// hasDisaster
            RDFNode type = argument.model.createResource(rightEntityUri);
            this.addStatement(leftEntityUri, CorpDbpedia.hasDisaster, type, argument.model);
		} catch (NullPointerException npe) {
            logSemanticRelationNullPointer(argument);
		}
		return 0;
	}

    /**
     * TODO
     * @param argument
     * @return
     */
	protected int handleCompanyIndustry(final ModelData argument) {
		Objects.requireNonNull(argument);
//		this.addSemanticRelation("company",CorpDbpedia.industry, "industry", argument );
        try {
            // keys: company, industry
            String leftEntityUri = argument.relationMention.entities.get("company").generatedUri;
            String rightEntityUri = argument.relationMention.entities.get("industry").generatedUri;
            // industry
            RDFNode rightEntity = argument.model.createResource(rightEntityUri);
            this.addStatement(leftEntityUri, CorpDbpedia.industry, rightEntity, argument.model);
        } catch (NullPointerException npe ) {
            logSemanticRelationNullPointer(argument);
        }
        return 0;
	}

    /**
     * keys: acquired, buyer and fix for siemensdata company_acquirer, company_beingacquired
     * @param argument
     * @return
     */
	protected int handleAcquisition(final ModelData argument) {
		Objects.requireNonNull(argument);
        // acquired
        boolean next = this.addSemanticRelation("acquired",CorpDbpedia.acquiredBy,"buyer",argument);
        if( !next ) this.addSemanticRelation("company_beingacquired",CorpDbpedia.acquiredBy,"company_acquirer",argument);
        // acquiredBy
        boolean next2 = this.addSemanticRelation("buyer",CorpDbpedia.acquired,"acquired",argument);
        if( !next2 ) this.addSemanticRelation("company_acquirer",CorpDbpedia.acquired,"company_beingacquired",argument);
		return 0;
	}

    /**
     * TODO
     * @param argument
     * @return
     */
	protected  int handleCompanyCustomer( final ModelData argument ) {
		Objects.requireNonNull(argument);
		try {
		    // keys: company, customer
            String leftEntityUri = argument.relationMention.entities.get("company").generatedUri;
            String rightEntityUri = argument.relationMention.entities.get("customer").generatedUri;
            // customer
            RDFNode rightEntity = argument.model.createResource(rightEntityUri);
            this.addStatement(leftEntityUri, CorpDbpedia.customer, rightEntity, argument.model);
            // customerof
            RDFNode leftEntity = argument.model.createResource(leftEntityUri);
    		this.addStatement(rightEntityUri, CorpDbpedia.customerOf, leftEntity, argument.model);
        } catch (NullPointerException npe ) {
		    logSemanticRelationNullPointer(argument);
        }
		return 0;
	}

    /**
     * TODO
     * @param subjectUri
     * @param predicateUri
     * @param rdfNode
     * @param model
     */
	protected void addStatement(final String subjectUri, final String predicateUri, final RDFNode rdfNode,
			final Model model) {
		Objects.requireNonNull(subjectUri);
		Objects.requireNonNull(predicateUri);
		Objects.requireNonNull(rdfNode);
		Objects.requireNonNull(model);

		Resource subject = model.createResource(subjectUri);
		Property predicate = model.createProperty(predicateUri);

		Statement statement = new StatementImpl(subject, predicate, rdfNode);
		model.add(statement);
	}

    /**
     * TODO
     * @param subjectUri
     * @param predicateUri
     * @param lex
     * @param datatype
     * @param model
     */
	protected void addStatementWithLiteral(final String subjectUri, final String predicateUri, Object lex, final RDFDatatype datatype,
			final Model model) {
		Objects.requireNonNull(subjectUri);
		Objects.requireNonNull(predicateUri);
		Objects.requireNonNull(lex);
		Objects.requireNonNull(model);

		Resource subject = model.createResource(subjectUri);
		Property predicate = model.createProperty(predicateUri);

		if(null == datatype) model.addLiteral(subject,predicate,lex);
		else model.add(subject, predicate, (String) lex, datatype);
	}

    /**
     * TODO
     * @param subject
     * @param predicate
     * @param object
     * @return
     */
	public static String globalFactHash(String subject, String predicate, RDFNode object) {

		String value;
		String lada = "";

		String subjHash = sha256Hash(subject);
		String predHash = sha256Hash(predicate);

		if( object.isURIResource() ) {
			value = object.asResource().getURI();
		} else {
			value = object.asLiteral().getLexicalForm();
			lada = object.asLiteral().getLanguage();
			if( "".equals(lada) ) lada = object.asLiteral().getDatatypeURI();
		}

		String objHash = sha256Hash(value);
		String ladaHash = sha256Hash(lada);

		//  SCALA : sha256Hash(List(subject,predicate,value,lada).flatMap(Option(_)).mkString(","))
		List<String> list = Arrays.asList(subjHash,predHash,objHash,ladaHash);
		return sha256Hash(String.join(",",list));
	}

    /**
     * TODO
     * @param text
     * @return
     */
	public static String sha256Hash(String text) {
		String ret = null;
		try {
			ret = String.format("%064x", new java.math.BigInteger(1, MessageDigest.getInstance("SHA-256").digest(text.getBytes())));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

    /**
     * Could be used to simplify addStatement for semantic relations,
     * but is incomplete no case for typed literal at the moment
     * @param leftEntityName
     * @param predicate
     * @param rightEntityName
     * @param argument
     * @return wasaddable
     */
    public boolean addSemanticRelation(String leftEntityName, String predicate, String rightEntityName, final ModelData argument) {
        try {
            String leftEntityUri = argument.relationMention.entities.get(leftEntityName).generatedUri;
            String rightEntityUri = argument.relationMention.entities.get(rightEntityName).generatedUri;
            // hasFinancialEvent
            RDFNode object = argument.model.createResource(leftEntityUri);
            this.addStatement(leftEntityUri, predicate, object, argument.model);

        } catch (NullPointerException npe ) {
            logSemanticRelationNullPointer(argument);
            return false;
        }
        return true;
    }

    /**
     * TODO
     * @param argument
     */
    public void logSemanticRelationNullPointer(ModelData argument) {
        String relationName = argument.relationMention.relation.text;
        if ( relationName == null ) relationName = argument.relationMention.relation.textNormalized;
        Logger.getGlobal().log(Level.WARNING, "Failed to map relation "+relationName+": " + argument.relationMention.entities.keySet() );
    }
}
