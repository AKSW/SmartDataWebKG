package aksw.org.sdw.importer.avro.annotations.dfki;

import java.util.List;

import aksw.org.sdw.importer.avro.annotations.DataImportAdapter;
import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.Mention;
import de.dfki.lt.tap.ConceptMention;


/**
 * This Adapter can be used to instantiate a DFKI Mention instance as a SDW Mention instance
 * @author kay
 *
 */
public class DfkiMentionAdapter extends Mention implements DataImportAdapter<de.dfki.lt.tap.ConceptMention> {
	
	@Override
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
	}

	@Override
	public boolean validIncomingData(final ConceptMention input) {
		return null != input;
	}
}
