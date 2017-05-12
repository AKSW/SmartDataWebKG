package aksw.org.sdw.importer.avro.annotations.beuth;

import java.util.Iterator;

import org.codehaus.jackson.JsonNode;

import aksw.org.sdw.importer.avro.annotations.DataImportAdapter;
import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.Mention;
import aksw.org.sdw.importer.avro.annotations.Provenance;
import aksw.org.sdw.importer.avro.annotations.RelationMention;

/**
 * This document can be used to import Avro JSON Data
 * from a crawl coming from Beuth
 * 
 * @author kay
 *
 */
public class BeuthDocumentAdapter extends Document implements DataImportAdapter<JsonNode> {

	public BeuthDocumentAdapter(final String uriNamespace, final String docId) {
		super(uriNamespace);
		
		this.id = docId;
	}

	@Override
	public void addData_internal(JsonNode relationMentionNode, final Document document) {
				
		BeuthRelationMentionAdapter relationMention = new BeuthRelationMentionAdapter();
		relationMention.addData(relationMentionNode, document);

		// get document provenance
		if (this.id.startsWith("uber/")) {
			int end = this.id.lastIndexOf("@");
			int start = "uber/".length();
			String sourceUrl = this.id.substring(start, end);
			
			Provenance provenance = new Provenance();
			provenance.source = sourceUrl;
			
			this.provenanceSet.add(provenance);
		}		
	}

	@Override
	public boolean validIncomingData(JsonNode input) {
		if (null == input) {
			return false;
		}
		
		return true;
	}
}
