package aksw.org.sdw.importer.avro.annotations.beuth;

import org.codehaus.jackson.JsonNode;

import aksw.org.sdw.importer.avro.annotations.DataImportAdapter;
import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.Mention;

/**
 * This class can be used to extract mention/entity
 * information from JSON Beuth input format.
 * 
 * @author kay
 *
 */
public class BeuthMentionAdapter extends Mention implements DataImportAdapter<JsonNode> {
	
	@Override
	public void addData_internal(final JsonNode conceptMention, final Document document) {
		
		
		JsonNode attributes = conceptMention.get("attributes");
		JsonNode map = attributes.get("map");
			
		this.span.start = map.get("charBegin").asInt();
		this.span.end = map.get("charEnd").asInt();
			
		this.textNormalized = conceptMention.get("normalizedValue").get("string").asText();
		this.text = map.get("value").asText();
		this.id = map.get("stringId").asText();
		
		this.generatedId = document.entityIdGenerator.addUniqueId(this);
			
		Beuth2SdwKgMapper.addEntityTypeMapping(conceptMention.get("type").asText(), this.types);
		this.mentionType = Mention.MentionType.ENTITY;
		
		document.conceptMentions.add(this);		
	}

	@Override
	public boolean validIncomingData(JsonNode input) {
		if (null == input) {
			return false;
		}
		
		return true;
	}
}
