package aksw.org.sdw.importer.avro.annotations.dfki;

import java.util.List;

import aksw.org.sdw.importer.avro.annotations.DataImportAdapter;
import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.RelationMention;
import de.dfki.lt.tap.ConceptMention;
import de.dfki.lt.tap.RelationArgument;

public class DfkiRelationMentionAdapter extends RelationMention implements DataImportAdapter<de.dfki.lt.tap.RelationMention> {
	
	@Override
	public void addData_internal(de.dfki.lt.tap.RelationMention dfkiRelationMention, Document document) {
		List<de.dfki.lt.tap.Provenance> dfkiProvenanceList = dfkiRelationMention.getProvenance();
		
		// add provenance
		if (null != dfkiProvenanceList && false == dfkiProvenanceList.isEmpty()) {
			
			for (de.dfki.lt.tap.Provenance dfkiProvenance : dfkiProvenanceList) {
				DfkiProvenanceAdapter provenance = new DfkiProvenanceAdapter();
				provenance.addData_internal(dfkiProvenance, document);
				
				this.provenance.add(provenance);
			}
		}
		
		// add relation arguments (concept mentions)
		List<RelationArgument> relationArguments = dfkiRelationMention.getArgs();
		if (null != relationArguments && false == relationArguments.isEmpty()) {
			int count = 1;
			for (RelationArgument relationArgument : relationArguments) {
				
				// get entities
				if (null != relationArgument.getConceptMention()) {
					ConceptMention dfkiConceptMention = relationArgument.getConceptMention();
					DfkiMentionAdapter conceptMention = new DfkiMentionAdapter();
					conceptMention.addData_internal(dfkiConceptMention, document);

					String role = relationArgument.getRole().toLowerCase();

					// TODO here this should be the name of the relation not the
					Dfki2SdwKgMapper.addEntityTypeMapping(role, conceptMention.types);

					if (false == this.entities.containsKey(role)) {
						this.entities.put(role,conceptMention);
					} else {
						//TODO: better count -> second map
						this.entities.put(role+count, conceptMention);
						count++;
					}
					if (false == document.conceptMentions.contains(conceptMention)) 
						document.conceptMentions.add(conceptMention);
				}
			}
		}
		
		// add relation // TODO
		
		DfkiMentionAdapter relation = new DfkiMentionAdapter(); relation.addData(dfkiRelationMention,document);
		relation.addData(dfkiRelationMention, document); 
		this.relation = relation;
		
		// add attributes // TODO
		
		// add span // TODO
		
		// add id // TODO
		
		
		
		
		this.relation.span = new DfkiSpanAdapter();
		((DfkiSpanAdapter) this.relation.span).addData_internal(dfkiRelationMention.getSpan(), document);
		
		this.relation.id = dfkiRelationMention.getId();		
		this.relation.types.add(dfkiRelationMention.getName());
		
	}

	@Override
	public boolean validIncomingData(de.dfki.lt.tap.RelationMention input) {
		return null != input;
	}

}
