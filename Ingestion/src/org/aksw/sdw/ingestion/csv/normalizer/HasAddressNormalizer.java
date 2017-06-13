package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.aksw.sdw.ingestion.csv.constants.VCARD;

/**
 * This normalizer ensures that the correct blank node id is used in the output file
 * 
 * @author kay
 *
 */
public class HasAddressNormalizer implements PropertyNormalizer {

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		Entity addressEntity = entity.getSubEntityByCategory("address");
		
		// in case no address property is specified --> add it
		String blankNodeUri = null;
		List<RDFNode> idObjects = entity.getRdfObjects(W3COrg.identifier);
		if (null != idObjects && false == idObjects.isEmpty()) {
			String id = idObjects.get(0).asLiteral().getLexicalForm();			
			blankNodeUri = "_:address" + ((null != id) ? "_" + id : "");
		}  else {
			blankNodeUri = "_:address";
		}
		
		if (null == addressEntity) {
			// now store address entity with correct URI
			addressEntity = new Entity(null, true);
			addressEntity.setSubjectUri(blankNodeUri);;
			entity.addSubEntity(addressEntity);
		} else {
			// or just update existing address entity
			addressEntity.setSubjectUri(blankNodeUri);
		}
			
		// delete old entry
		entity.deleteProperty(VCARD.ADDR_HAS_ADDRESS);

		// store new blank node URI
		String finalBlankNodeUri = addressEntity.getSubjectUri();
		entity.addTriple(VCARD.ADDR_HAS_ADDRESS, new ResourceImpl(finalBlankNodeUri));
		
		
	}

}
