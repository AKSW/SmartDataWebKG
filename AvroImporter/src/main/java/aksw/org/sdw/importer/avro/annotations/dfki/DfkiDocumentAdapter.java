package aksw.org.sdw.importer.avro.annotations.dfki;

import java.util.List;

import aksw.org.sdw.importer.avro.annotations.DataImportAdapter;
import aksw.org.sdw.importer.avro.annotations.Document;

/**
 * This class can be used to create new SDW KG Documents from DFKI input documents
 * 
 * @author kay
 *
 */
public class DfkiDocumentAdapter extends Document implements DataImportAdapter<de.dfki.lt.tap.Document> {
	
	final static String namespacePrefix = "http://corp.dbpedia.org/extract/it02/dfki/";
	
	public DfkiDocumentAdapter() {
		super(DfkiDocumentAdapter.namespacePrefix);		
	}
	
	@Override
	public void addData_internal(de.dfki.lt.tap.Document dfkiDocument, Document document) {
		if (null == this.langCode) {
			this.langCode = dfkiDocument.getLangCode();
		}
		
		if (null == this.text) {
			this.text = dfkiDocument.getText();
		}
		
		List<de.dfki.lt.tap.Provenance> dfkiDocProvenanceList = dfkiDocument.getProvenance();
		if (null != dfkiDocProvenanceList && false == dfkiDocProvenanceList.isEmpty()) {
			for (de.dfki.lt.tap.Provenance dfkiDocProvenance : dfkiDocProvenanceList) {
				DfkiProvenanceAdapter provenance = new DfkiProvenanceAdapter();
				provenance.addData_internal(dfkiDocProvenance, this);
				
				this.provenanceSet.add(provenance);
			}
		}
		
		// check for named entities/concepts
		List<de.dfki.lt.tap.ConceptMention> dfkiConceptMentions = dfkiDocument.getConceptMentions();
		if (null != dfkiConceptMentions && false == dfkiConceptMentions.isEmpty()) {
			for (de.dfki.lt.tap.ConceptMention dfkiConceptMention : dfkiConceptMentions) {
				DfkiMentionAdapter dfkiEntity = new DfkiMentionAdapter();
				
				dfkiEntity.addData_internal(dfkiConceptMention, this);
				this.conceptMentions.add(dfkiEntity);
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
            	
            	this.relationMentions.add(relationMention);   
            }
        }	
	}

	@Override
	public boolean validIncomingData(de.dfki.lt.tap.Document input) {
		return null != input;
	}

}
