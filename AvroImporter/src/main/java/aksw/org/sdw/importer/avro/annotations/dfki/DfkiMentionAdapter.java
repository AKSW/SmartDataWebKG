package aksw.org.sdw.importer.avro.annotations.dfki;

import java.util.List;

import org.apache.avro.specific.SpecificRecordBase;

import aksw.org.sdw.importer.avro.annotations.DataImportAdapter;
import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.Mention;
import de.dfki.lt.tap.ConceptMention;
import de.dfki.lt.tap.RelationMention;


/**
 * This Adapter can be used to instantiate a DFKI Mention instance as a SDW Mention instance
 * @author kay
 *
 */
public class DfkiMentionAdapter extends Mention implements DataImportAdapter<org.apache.avro.specific.SpecificRecordBase> {
	
	@Override
	/* (non-Javadoc)
	 * @see aksw.org.sdw.importer.avro.annotations.DataImportAdapter#addData_internal(java.lang.Object, aksw.org.sdw.importer.avro.annotations.Document)
	 */
	public void addData_internal(SpecificRecordBase input, Document document)
	{
		// TODO Auto-generated method stub
		if (input instanceof ConceptMention)//(input.getClass().equals(ConceptMention.class))
			this.addData_internal((ConceptMention) input, document);
		else if (input instanceof RelationMention)
			this.addData_internal((RelationMention) input, document);
		else throw new UnsupportedOperationException("ConceptMention or RelationMention are valid input object typtes but submitted class:"+input.getClass());
	}
	
	
	public void addData_internal(final ConceptMention conceptMention, final Document document) {
		this.id = conceptMention.getId();
		this.textNormalized = conceptMention.getNormalizedValue();
		
		this.span = new DfkiSpanAdapter();
		((DfkiSpanAdapter) this.span).addData_internal(conceptMention.getSpan(), document);
		
		Dfki2SdwKgMapper.addEntityTypeMapping(conceptMention.getType(), this.types);
				
		List<de.dfki.lt.tap.Provenance> dfkiProvenances = conceptMention.getProvenance();
		if (null != dfkiProvenances && false == dfkiProvenances.isEmpty()) {
			for (de.dfki.lt.tap.Provenance dfkiProvenance : dfkiProvenances) {
				
				DfkiProvenanceAdapter provenance = new DfkiProvenanceAdapter();
				provenance.addData_internal(dfkiProvenance, document);
				
				this.provenanceSet.add(provenance);
			}
		}
		
		this.generatedId = document.entityIdGenerator.addUniqueId(this);		
		document.conceptMentions.add(this);
		this.mentionType=mentionType.ENTITY;
	}
	
	// TODO check wheter everything is mapped
	public void addData_internal(RelationMention relationMention, final Document document) {
		this.id = relationMention.getId();
		this.textNormalized = relationMention.getName();
		
		this.span = new DfkiSpanAdapter();
		((DfkiSpanAdapter) this.span).addData_internal(relationMention.getSpan(), document);
		
		//Dfki2SdwKgMapper.addEntityTypeMapping(relationMention.getType(), this.types);
				
		List<de.dfki.lt.tap.Provenance> dfkiProvenances = relationMention.getProvenance();
		if (null != dfkiProvenances && false == dfkiProvenances.isEmpty()) {
			for (de.dfki.lt.tap.Provenance dfkiProvenance : dfkiProvenances) {
				
				DfkiProvenanceAdapter provenance = new DfkiProvenanceAdapter();
				provenance.addData_internal(dfkiProvenance, document);
				
				this.provenanceSet.add(provenance);
			}
		}
		
		this.generatedId = document.entityIdGenerator.addUniqueId(this);		
		document.conceptMentions.add(this);
		this.mentionType=mentionType.RELATION;
	}


	/* (non-Javadoc)
	 * @see aksw.org.sdw.importer.avro.annotations.DataImportAdapter#validIncomingData(java.lang.Object)
	 */
	@Override
	public boolean validIncomingData(SpecificRecordBase input)
	{
		// TODO Auto-generated method stub
		return null != input;
	}

//	@Override
//	public boolean validIncomingData(final ConceptMention input) {
//		return null != input;
//	}
//	
//	@Override
//	public void addData(ConceptMention input, Document document)
//	{
//		DataImportAdapter.super.addData(input, document);
//	}
}
