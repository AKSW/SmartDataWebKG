package aksw.org.sdw.importer.avro.annotations.nif;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import aksw.org.sdw.importer.avro.annotations.GlobalConfig;
import aksw.org.sdw.rdf.namespaces.CorpDbpedia;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.nlp2rdf.NIF;
import org.nlp2rdf.bean.NIFBean;
import org.nlp2rdf.bean.NIFType;
import org.nlp2rdf.nif21.NIF21Format;
import org.nlp2rdf.nif21.impl.NIF21;

import com.github.jsonldjava.core.RDFDatasetUtils;

import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.Mention;
import aksw.org.sdw.importer.avro.annotations.Provenance;

/**
 * This class can be used to generated NIF models
 * for an entity
 * 
 * @author kay
 *
 */
public class NIFAnnotationGenerator extends DocRdfGenerator {
	
	/** input document */
	final Document document;
	final String baseUri;
	final String nifid;
	
//	public NIFAnnotationGenerator(final String graphName, final Document document) {
//		this((String) null, document, (DocRdfGenerator) null, (String) null);
//	}
	
	public NIFAnnotationGenerator(final String graphName, final Document document, final DocRdfGenerator relationGenerator) {
		super(graphName, relationGenerator);
		this.document = document;
		this.baseUri = document.uri;
		this.nifid = UUID.randomUUID().toString();
	}

	class HashedBean {
		public String beanUri;
		public String beanHash;

		HashedBean(String bean, String hash) {
			this.beanUri = bean;
			this.beanHash = hash;
		}
	}
	
	protected Dataset addToRdfData_internal(final Dataset dataset) {
		List<NIFBean> result = new ArrayList<>();

		String contextUri = null;

//		Model result = ModelFactory.createDefaultModel();

		Map<String, String> beanUriHash = new HashMap<>();

		List<HashedBean> hashedBeans = new ArrayList<>();
        NIFBean.NIFBeanBuilder builderContext = new NIFBean.NIFBeanBuilder();
        
        String text = this.document.text;
        if (null != text) {

			builderContext.context(baseUri, 0, text.length());
	        builderContext.nifType(NIFType.CONTEXT);
	        // set text index
	        builderContext.beginIndex(0);
	        builderContext.endIndex(text.length());
	        builderContext.mention(text);
	
	        NIFBean beanContext = new NIFBean(builderContext);
	
	        result.add(beanContext);
	        contextUri =  new Formatter().format("%s#offset_%d_%d",document.uri,0,text.length()).toString();
//	        result.add(new NIF21(Arrays.asList(beanContext)).getModel());
        } else {
        	builderContext.context(baseUri, 0, 0);
	        builderContext.nifType(NIFType.CONTEXT);

	        // set text index
	        builderContext.beginIndex(0);
	        builderContext.endIndex(0);
	        
	        builderContext.mention("no text");
	
	        NIFBean beanContext = new NIFBean(builderContext);
	
	        result.add(beanContext);
			contextUri =  new Formatter().format("%s#offset_%d_%d",document.uri,0,0).toString();
//			result.add(new NIF21(Arrays.asList(beanContext)).getModel());
        }

        for (Mention conceptMention : this.document.conceptMentions) {

        	List<NIFBean> conceptBeanList = new ArrayList<>();
            NIFBean.NIFBeanBuilder builderMention = new NIFBean.NIFBeanBuilder();
            builderMention.nifType(NIFType.ENTITY);
            
            builderMention.taIdentRef(conceptMention.generatedUri);
        	
        	List<String> typesMention = new ArrayList<String>();

//			if ( conceptMention.types.contains("http://UNMAPPED.ER/financial-event")) {
//				System.out.println(conceptMention.types);
//			}
			if ( conceptMention.mentionType == Mention.MentionType.RELATION) {
				for (String type : conceptMention.types) {
					typesMention.add(CorpDbpedia.prefixOntology+type);
				}
			} else {
				for (String type : conceptMention.types)
				{
//					if(!type.startsWith("http://UNMAPPED.ER/")) typesMention.add(type);
					typesMention.add(type);
				}
			}
        	builderMention.types(typesMention);
        	builderMention.context(baseUri, conceptMention.span.start, conceptMention.span.end);
        	if (null != conceptMention.text) {
        		builderMention.mention(conceptMention.text);
        	} else if (null != conceptMention.textNormalized) {
        		builderMention.mention(conceptMention.textNormalized);
        	}
        	
        	builderMention.beginIndex(conceptMention.span.start);
        	builderMention.endIndex(conceptMention.span.end);
        	
        	// add provenance information
        	Iterator<Provenance> provenanceIt = conceptMention.provenanceSet.iterator();

//			String hashedNif = GlobalConfig.getInstance().makeNifHash(conceptMention, document);
			String hashedNif = new Formatter().format("%s#offset_%d_%d",document.uri+"?lid="+GlobalConfig.getInstance()
					.makeNifHash(conceptMention, document),conceptMention.span.start,conceptMention.span.end).toString();


			String beanUri = new Formatter().format("%s#offset_%d_%d",document.uri,conceptMention.span.start,conceptMention.span.end).toString();
        	//ANNOTATOR
        	if( !provenanceIt.hasNext() ) builderMention.annotator(RDFHelpers.createValidIRIfromBase("default","http://corp.dbpedia.org/annotator"));
        	while (provenanceIt.hasNext()) {
        		Provenance provenance = provenanceIt.next();
        		if (null != provenance.annotator) {
        			builderMention.annotator( RDFHelpers.createValidIRIfromBase(provenance.annotator ,"http://corp.dbpedia.org/annotator"));
        			if( -1.0f != provenance.score ) builderMention.score((double) provenance.score);
        			break;
        		} else {
        			continue;
        		}
        	}

            NIFBean bean = new NIFBean(builderMention);
        	beanUriHash.put(beanUri, hashedNif);
//			beanUriHash.add(new HashedBean(beanUri, hashedNif));
        	result.add(bean);
		}


        NIF nif21 = new NIF21(result);
        Model nifModel = nif21.getModel();
        Model outModel = ModelFactory.createDefaultModel();

//		Logger.getGlobal().info(beanUriHash.keySet().toString());
		StmtIterator iter = nifModel.listStatements();
		while(iter.hasNext()) {
			Statement stmt = iter.nextStatement();

			Resource s = stmt.getSubject();
			Property p = stmt.getPredicate();
			RDFNode o = stmt.getObject();
//			Logger.getGlobal().info(s.getURI());
			if(beanUriHash.keySet().contains(s.getURI())) {



				outModel.add(ResourceFactory.createResource(beanUriHash.get(s.getURI())), p, o);
//				outModel.add(ResourceFactory.createResource(s.getURI() + "?lid=" + beanUriHash.get(s.getURI())), p, o);
			} else {
				outModel.add(s, p, o);
			}
		}

		//DocAttributes
		outModel.add(addContextInformation(contextUri,document));

        dataset.addNamedModel(this.graphName, outModel);
		nifModel.close();
        outModel.close();
        
        return dataset;
	}

	public static Model addContextInformation(String uri, Document doc) {
		Model m = ModelFactory.createDefaultModel();

		Resource subject = m.createResource(uri);

//		date, language, doctype, dct:title, provenance, confidence
		if( null != doc.date)
			m.add(subject, m.createProperty(CorpDbpedia.hasDate),doc.date.toString(), XSDDatatype.XSDdateTime);
		if( null != doc.docType)
			m.add(subject, m.createProperty(CorpDbpedia.hasDocType),m.createLiteral(doc.docType.toString(), doc.langCode));
		if( null != doc.langCode)
			m.add(subject, m.createProperty("http://purl.org/dc/terms/language"),m.createLiteral(doc.langCode));
		if( null != doc.title)
			m.add(subject, m.createProperty("http://purl.org/dc/terms/title"), m.createLiteral(doc.title, doc.langCode));

		for(Provenance p : doc.provenanceSet) {
			if( null != p.annotator) {
				RDFNode annotaorObject = m.createResource(RDFHelpers.createValidIRIfromBase(p.annotator,"http://corp.dbpedia.org/annotator"));
				m.add(subject, m.createProperty(NIF21Format.RDF_PROPERTY_ANNOTATOR), annotaorObject);
				m.add(subject, m.createProperty(NIF21Format.RDF_PROPERTY_CONFIDENCE), m.createTypedLiteral(p.score, XSDDatatype.XSDfloat));
			}
		}

		return m;
	}
}
