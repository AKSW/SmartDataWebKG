package aksw.org.sdw.importer.avro.annotations.nif;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.nlp2rdf.NIF;
import org.nlp2rdf.bean.NIFBean;
import org.nlp2rdf.bean.NIFType;
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
	
	public NIFAnnotationGenerator(final String graphName, final Document document) {
		this((String) null, document, (DocRdfGenerator) null);
	}
	
	public NIFAnnotationGenerator(final String graphName, final Document document, final DocRdfGenerator relationGenerator) {
		super(graphName, relationGenerator);
		this.document = document;
	}
	
	protected Dataset addToRdfData_internal(final Dataset dataset) {
		List<NIFBean> result = new ArrayList<>();

        NIFBean.NIFBeanBuilder builderContext = new NIFBean.NIFBeanBuilder();
        
        String text = this.document.text;
        if (null != text) {

	        builderContext.context(this.document.id, 0, text.length());
	        builderContext.nifType(NIFType.CONTEXT);
	        
	        // set text index
	        builderContext.beginIndex(0);
	        builderContext.endIndex(text.length());
	        
	        builderContext.mention(text);
	
	        NIFBean beanContext = new NIFBean(builderContext);
	
	        result.add(beanContext);
        } else {
        	builderContext.context(this.document.id, 0, 0);
	        builderContext.nifType(NIFType.CONTEXT);
	        
	        // set text index
	        builderContext.beginIndex(0);
	        builderContext.endIndex(0);
	        
	        builderContext.mention("no text");
	
	        NIFBean beanContext = new NIFBean(builderContext);
	
	        result.add(beanContext);
        }
        
        for (Mention conceptMention : this.document.conceptMentions) {
        	
            NIFBean.NIFBeanBuilder builderMention = new NIFBean.NIFBeanBuilder();
            builderMention.nifType(NIFType.ENTITY);
            
            builderMention.taIdentRef(conceptMention.generatedUri);
        	
        	List<String> typesMention = new ArrayList<String>();
        	for (String type : conceptMention.types) {
        		typesMention.add(type);
        	}
        	builderMention.types(typesMention);
        	
        	builderMention.context(this.document.id, conceptMention.span.start, conceptMention.span.end);
        	if (null != conceptMention.text) {
        		builderMention.mention(conceptMention.text);
        	} else if (null != conceptMention.textNormalized) {
        		builderMention.mention(conceptMention.textNormalized);
        	}
        	
        	builderMention.beginIndex(conceptMention.span.start);
        	builderMention.endIndex(conceptMention.span.end);
        	
        	// add provenance information
        	Iterator<Provenance> provenanceIt = conceptMention.provenanceSet.iterator();

        	if( !provenanceIt.hasNext() ) builderMention.annotator(RDFHelpers.createValidIriComponent("EMPTY-ANNOTATOR"));
        	while (provenanceIt.hasNext()) {
        		Provenance provenance = provenanceIt.next();
        		if (null != provenance.annotator) {
        			builderMention.annotator( RDFHelpers.createValidIriComponent(provenance.annotator));
        			builderMention.score((double) provenance.score);
        			break;
        		} else {
        			continue;
        		}
        	}

            NIFBean bean = new NIFBean(builderMention);
            result.add(bean);
        }
        
        NIF nif21 = new NIF21(result);
        
        // add new annotations
        Model nifModel = nif21.getModel();  
        
        dataset.addNamedModel(this.graphName, nifModel);
        nifModel.close();
        
        return dataset;
	}

}
