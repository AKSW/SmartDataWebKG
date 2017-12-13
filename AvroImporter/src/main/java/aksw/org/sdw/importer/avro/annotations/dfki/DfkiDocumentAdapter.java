package aksw.org.sdw.importer.avro.annotations.dfki;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aksw.org.sdw.importer.avro.annotations.DataImportAdapter;
import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.Mention;
import de.dfki.lt.tap.RelationArgument;

/**
 * This class can be used to create new SDW KG Documents from DFKI input documents
 * 
 * @author kay
 *
 */
public class DfkiDocumentAdapter extends Document implements DataImportAdapter<de.dfki.lt.tap.Document> {
	
	//final static String namespacePrefix = "http://corp.dbpedia.org/extract/it02/dfki/";

	final boolean rOnly;
	
	public DfkiDocumentAdapter(String namespacePrefix, boolean rOnly) {
		super(namespacePrefix);
		this.rOnly = rOnly;
	}
	
	@Override
	public void addData_internal(de.dfki.lt.tap.Document dfkiDocument, Document document) {
		if (null == this.langCode) {
			this.langCode = dfkiDocument.getLangCode();
		}
		
		if (null == this.text) {
			this.text = dfkiDocument.getText();
		}
		if ( null == this.uri ) {
			this.uri = dfkiDocument.getUri().endsWith("/") ? dfkiDocument.getUri() : dfkiDocument.getUri()+"/";
		}

		if (null == this.docType && dfkiDocument.getDocType() != null ) {
			this.docType = DocTypes.valueOf(dfkiDocument.getDocType().toString());
		}

		if ( null == this.date ) {
			this.date = dfkiDocument.getDate();
		}
		
		//check for provenance
		List<de.dfki.lt.tap.Provenance> dfkiDocProvenanceList = dfkiDocument.getProvenance();
		if (null != dfkiDocProvenanceList && false == dfkiDocProvenanceList.isEmpty()) {
			for (de.dfki.lt.tap.Provenance dfkiDocProvenance : dfkiDocProvenanceList) {
				DfkiProvenanceAdapter provenance = new DfkiProvenanceAdapter();
				provenance.addData_internal(dfkiDocProvenance, this);
				
				this.provenanceSet.add(provenance);
			}
		}

		// check if we have relationship mentions
		List<de.dfki.lt.tap.RelationMention> relationMentions = dfkiDocument.getRelationMentions();
		if (null != relationMentions && false == relationMentions.isEmpty()) {

			// iterate over relation mentions of document
			// (alternative - iterate over sentences, then over relation mentions)
			for (de.dfki.lt.tap.RelationMention dfkiRelationMention : relationMentions) {

				DfkiRelationMentionAdapter relationMention = new DfkiRelationMentionAdapter();
				relationMention.addData_internal(dfkiRelationMention, this);

				for(RelationArgument relationArgument : dfkiRelationMention.getArgs()) {
					conceptsInRelations.add(relationArgument.getConceptMention().getId());
				}

				this.relationMentions.add(relationMention);
			}
		}

		if(!rOnly) {
			// check for named entities/concepts
			List<de.dfki.lt.tap.ConceptMention> dfkiConceptMentions = dfkiDocument.getConceptMentions();
			if (null != dfkiConceptMentions && false == dfkiConceptMentions.isEmpty()) {
				for (de.dfki.lt.tap.ConceptMention dfkiConceptMention : dfkiConceptMentions) {

					if(conceptsInRelations.contains(dfkiConceptMention.getId())) continue;

					DfkiMentionAdapter dfkiEntity = new DfkiMentionAdapter();
					dfkiEntity.addData_internal(dfkiConceptMention, this);
					this.conceptMentions.add(dfkiEntity);
				}
			}
		}
	}

	@Override
	public boolean validIncomingData(de.dfki.lt.tap.Document input) {
		return null != input;
	}

	protected Set<String> conceptsInRelations = new HashSet<>();

}
