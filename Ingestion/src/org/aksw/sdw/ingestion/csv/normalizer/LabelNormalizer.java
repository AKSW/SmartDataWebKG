package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.SKOS;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

import aksw.org.kg.entity.Entity;

/**
 * This class can be used to remove duplicate name labels
 * and to only specify 1 prefLabel per language
 * 
 * @author kay
 *
 */
public class LabelNormalizer implements PropertyNormalizer {
	
	/** can be used to check, whether labels contain at least one latin character */ 
	final static Pattern latinChars = Pattern.compile("[a-zA-Z]");


	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		
		List<RDFNode> prefLabels = entity.getRdfObjects(SKOS.prefLabel);
		List<RDFNode> altLabels = entity.getRdfObjects(SKOS.altLabel);
		
		Map<String, RDFNode> finalPrefLabelObjects = null;
		List<RDFNode> finalAltLabels = new ArrayList<>();
		
		if (null == prefLabels && null != altLabels) {
			finalPrefLabelObjects = this.getMaxlengthLabelPerLength(altLabels);			
			
			this.fillLabelList(finalAltLabels, finalPrefLabelObjects.values(), altLabels);
		} else if (null != prefLabels) {
			finalPrefLabelObjects = this.getMaxlengthLabelPerLength(prefLabels);
			
			this.fillLabelList(finalAltLabels, finalPrefLabelObjects.values(), prefLabels);
			if (null != altLabels) {
				this.fillLabelList(finalAltLabels, finalPrefLabelObjects.values(), altLabels);
			}
		}		
		
		
		// delete old ones
		entity.deleteProperty(SKOS.prefLabel);
		entity.deleteProperty(SKOS.altLabel);
		
		// add new prefLabel
		for (RDFNode prefLabelObject : finalPrefLabelObjects.values()) {
			entity.addTriple(SKOS.prefLabel, prefLabelObject);
		}
		
		for (RDFNode finalAltLabel : finalAltLabels) {
			entity.addTriple(SKOS.altLabel, finalAltLabel);
		}		
	}
	
	protected void fillLabelList(final List<RDFNode> targetLabelList,
								 final Collection<RDFNode> usedLabels,
								 final Collection<RDFNode> otherLabels) {
		for (RDFNode altLabel : otherLabels) {
			
			if (altLabel instanceof Literal) {
				Literal literalObject = (Literal) altLabel;
				
				String language = literalObject.getLanguage();
				boolean isLabel = latinChars.matcher(((Literal) altLabel).getLexicalForm()).find() &&
								  (null == language || language.isEmpty()); 
			
				if (isLabel && false == usedLabels.contains(altLabel) &&
					false == targetLabelList.contains(altLabel)) {
					targetLabelList.add(altLabel);	
				}
			}
		}
	}
	
	protected Map<String, RDFNode> getMaxlengthLabelPerLength(final List<RDFNode> labels) {
		if (null == labels|| labels.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<String, RDFNode> labelMap = new HashMap<>();
		
		
		for (RDFNode label : labels) {
			
			if (label instanceof Literal) {
				Literal literalObject = (Literal) label;
				String literal = literalObject.getLexicalForm();
				String language = literalObject.getLanguage();
				if (null == language || false == language.startsWith("@")) {
					continue;
				}
				
				boolean isLabel = latinChars.matcher(literal).find() &&
								  (null == language || language.isEmpty() || language.startsWith("@"));
				if (false == isLabel) {
					continue;
				}
				
				RDFNode storedObject = labelMap.get(language);				
				if (null == storedObject) {
					if (false == literal.contains("(")) {
						labelMap.put(language, literalObject);
					}
				} else {
					String storedLiteral = storedObject.asLiteral().getLexicalForm();
					if (0 > storedLiteral.compareTo(literal) &&
						false == literal.contains("(")) {
						labelMap.remove(language);
						labelMap.put(language, literalObject);
					}
				}
			}			
		}
		
		return labelMap;
	}

}
			