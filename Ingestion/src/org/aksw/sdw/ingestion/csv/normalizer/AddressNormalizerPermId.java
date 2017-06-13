package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;

import aksw.org.kg.entity.Entity;
//import aksw.org.kg.entity.RdfObject;
import org.aksw.sdw.ingestion.csv.constants.VCARD;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.PropertyImpl;

public class AddressNormalizerPermId implements PropertyNormalizer {

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {

		Entity addressEntity = entity.getSubEntityByCategory("address");
		if (null == addressEntity) {
			return;
		}
		
		List<RDFNode> countryObjects = entity.getRdfObjects(VCARD.ADDR_COUNTRY_NAME);
		if (null != countryObjects && false == countryObjects.isEmpty()) {
			
			Property addressCountryName = new PropertyImpl(VCARD.ADDR_COUNTRY_NAME);
			for (RDFNode countryObject : countryObjects) {
				addressEntity.addTriple(addressCountryName, countryObject);
			}
		}
		
		entity.deleteProperty(VCARD.ADDR_COUNTRY_NAME);
		
		List<RDFNode> addressObjects = entity.getRdfObjects(VCARD.ADDR_STR);
		if (null != addressObjects && false == addressObjects.isEmpty()) {
			
			Property addressStreet = new PropertyImpl(VCARD.ADDR_STR);
			for (RDFNode addressObject : addressObjects) {
//				String addressString = addressObject.getObjectString();
//				addressObject.updateObjectString(addressString.replaceAll("[\\n]", ","));
				
				addressEntity.addTriple(addressStreet, addressObject);
			}
		}
		
		entity.deleteProperty(VCARD.ADDR_STR);
	}

}
