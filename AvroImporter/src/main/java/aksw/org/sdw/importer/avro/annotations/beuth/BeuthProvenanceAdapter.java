package aksw.org.sdw.importer.avro.annotations.beuth;

import org.codehaus.jackson.JsonNode;

import aksw.org.sdw.importer.avro.annotations.DataImportAdapter;
import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.Provenance;

/**
 * This class can be used to obtain provenance information from Beuth source JSON
 * 
 * @author kay
 *
 */
public class BeuthProvenanceAdapter extends Provenance implements DataImportAdapter<JsonNode> {
	
	@Override
	public void addData_internal(final JsonNode provenanceNode, final Document document) {
		
		
		String annotatorString = provenanceNode.get("annotator").asText();
//		if (annotatorString.contains("http")) {
//			int urlStart = annotatorString.indexOf("http");
//			annotatorString = annotatorString.substring(urlStart);
//		}
		
		this.annotator = annotatorString;		
		
		// check if uberMetrics provided source
		if (document.id.startsWith("uber/")) {
			int end = document.id.lastIndexOf("@");
			int start = "uber/".length();
			String sourceUrl = document.id.substring(start, end);
			
			this.source = sourceUrl;
		}
	}

	@Override
	public boolean validIncomingData(final JsonNode input) {
		if (null == input) {
			return false;
		}
		
		return true;
	}

}
