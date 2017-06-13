package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;
import java.util.regex.Pattern;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CompanyHouseUris;
import org.aksw.sdw.ingestion.csv.constants.RdfDataTypes;
import org.aksw.sdw.ingestion.csv.utils.CustomJenaType;
import org.apache.jena.rdf.model.RDFNode;

/**
 * This class can be used to add a registered address to the corpus
 * which come from a companies house dataset
 * 
 * @author kay
 *
 */
public class RegisteredAddressNormalizerCH implements PropertyNormalizer {
	
	/** non-word characters pattern */
	/// TODO km : check whether they support unicode as well
	final static Pattern nonWordCharacter = Pattern.compile("[\\W]+", Pattern.UNICODE_CHARACTER_CLASS);
	
	/**
	 * This class can be used to normalise an object string where required
	 * @author kay
	 *
	 */
	abstract class ObjectNormaliser {
		abstract String normaliseObject(final String objectString);
	}
	
	
	public void addPredicate(final StringBuilder builder, final Entity entity, final String ... predicates) {
		this.addPredicate(builder, entity, null, predicates);
	}
	
	/**
	 * This method can be used to add a predicate to the address string builder
	 * 
	 * @param builder	- string builder which is used to store the address
	 * @param entityMap - entity map which contains all the known entity properties
	 * @param objectStringNormalizer - class which can be used to normalisee string
	 * @param predicates - array of predicates
	 */
	public void addPredicate(final StringBuilder builder, final Entity entity,
							 final ObjectNormaliser objectStringNormalizer, final String ... predicates) {
		for (String predicate : predicates) {
			List<RDFNode> rdfObject = entity.getRdfObjects(predicate);
			if (null != rdfObject && false == rdfObject.isEmpty()) {
				if (0 < builder.length()) {
					builder.append(",");
				}
			
				String objectString = null == objectStringNormalizer ?
						rdfObject.get(0).toString() :
						objectStringNormalizer.normaliseObject(rdfObject.get(0).toString());
						
				// remove any characters which are not part of standard words
				/// TODO km : support unicode!!!
				String cleanedObjectString = RegisteredAddressNormalizerCH.nonWordCharacter.
												matcher(objectString).replaceAll(" ").trim();						
				builder.append(cleanedObjectString);
				break;
			}
		}
	}
	
	@Override
	public void normalize(final Entity entity, final ConversionStats stats) throws IngestionException {
		StringBuilder builder = new StringBuilder();
		
		this.addPredicate(builder, entity, CompanyHouseUris.predicateAddressCareOf);
		this.addPredicate(builder, entity, CompanyHouseUris.predicateAddressPOBox);
		this.addPredicate(builder, entity, CompanyHouseUris.predicateAddressAddressLine1);
		this.addPredicate(builder, entity, CompanyHouseUris.predicateAddressAddressLine2);		
		this.addPredicate(builder, entity, CompanyHouseUris.predicateUriCityName);		
		this.addPredicate(builder, entity, CompanyHouseUris.predicateUriRegionName);
		
		// make sure that there are no spaces in the zip code
		this.addPredicate(builder, entity, new ObjectNormaliser() {
			
			@Override
			String normaliseObject(String objectString) {
				return objectString.replace(" ", "");
			}
		}, CompanyHouseUris.predicateUriZipCode);
		
		// pick out country of origin or reg-country
		this.addPredicate(builder, entity, CompanyHouseUris.predicateCountryOfOrigin,
											  CompanyHouseUris.predicateUriCountryName);
		
		String address = builder.toString();
		if (address.isEmpty()) {
			stats.incrementStats(RegisteredAddressNormalizerCH.class, "noAddress");
			stats.setStatText(RegisteredAddressNormalizerCH.class, "noAddress", entity.getSubjectUri());
			return;
		}
				
		entity.addTripleWithLiteral(CompanyHouseUris.predicateUriRegisteredAddress,
				address, new CustomJenaType(RdfDataTypes.XmlSchemaString));
	}
}
