package aksw.org.sdw.importer.avro.annotations.beuth;

import java.util.Iterator;

import org.codehaus.jackson.JsonNode;

import aksw.org.sdw.importer.avro.annotations.DataImportAdapter;
import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.Mention;
import aksw.org.sdw.importer.avro.annotations.Provenance;
import aksw.org.sdw.importer.avro.annotations.RelationMention;

public class BeuthRelationMentionAdapter extends RelationMention implements DataImportAdapter<JsonNode> {

	@Override
	public void addData_internal(final JsonNode relationMentionNode, final Document document) {
		
		JsonNode relationTypeName = relationMentionNode.get("name");
		
		JsonNode relationSpan = relationMentionNode.get("span");
		this.relation.span.start = relationSpan.get("start").asInt();
		this.relation.span.end = relationSpan.get("end").asInt();
		
		JsonNode relationMap = relationMentionNode.get("attributes").get("map");
		this.relation.text = relationMap.get("value").asText();
		this.relation.textNormalized = relationMap.get("lemma").asText();
		
//		this.relation.id = node.get("id").asText();
		
//		Beuth2SdwKgMapper.addEntityTypeMapping(relationTypeName.asText(), mention.relation.types);
		this.relation.types.add(relationTypeName.asText());
		this.relation.mentionType = Mention.MentionType.RELATION;
		
		this.relation.span.start = relationMap.get("charBegin").asInt();
		this.relation.span.end = relationMap.get("charEnd").asInt();
//		mention.relation.span.tokenPositionStart = relationMap.get("segmentBegin").asInt();
//		mention.relation.span.tokenPositionEnd = relationMap.get("segmentEnd").asInt();
		
		// get entity information							
		JsonNode entityArray;
		if (null != (entityArray = relationMentionNode.get("args").get("array")) &&
			false == entityArray.isNull()) {
		
			Iterator<JsonNode> entityIt = entityArray.getElements();								
			while (entityIt.hasNext()) {
				JsonNode entityNode = entityIt.next();
				if (null == entityNode || entityNode.isNull()) {
					continue;
				}
				
				JsonNode mentionNode = entityNode.get("conceptMention");
				BeuthMentionAdapter newEntity = new BeuthMentionAdapter();
				newEntity.addData(mentionNode, document);
				
				if (false == this.entities.contains(newEntity)) {
					this.entities.add(newEntity);
					
				}
			}
		}
		
		if (2 != this.entities.size()) {
			throw new RuntimeException("Entity count in doc: " + document.id + " is: " + this.entities.size());
		}
		
		JsonNode provenanceNodes = relationMentionNode.get("provenance");
		if (null != provenanceNodes && false == provenanceNodes.isNull()) {
			Iterator<JsonNode> provenanceIt = provenanceNodes.get("array").getElements();
			while (provenanceIt.hasNext()) {
				JsonNode provenanceNode = provenanceIt.next();
				BeuthProvenanceAdapter provenance =
						new BeuthProvenanceAdapter();
				provenance.addData(provenanceNode, document);
				
				this.provenance.add(provenance);
			}
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
