package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper;
import org.apache.jena.datatypes.xsd.impl.XSDBaseStringType;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;

/**
 * This class can be used to add header quarter location information
 * 
 * @author kay
 *
 */
public class HeadquarterSite implements PropertyNormalizer {

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		boolean hasHeadquarterInformation = false;
		
		for (String predicateName : entity.getPredicates()) {
			// try to find out, whether we have headquarter information
			if (predicateName.toLowerCase().contains("headquarter")) {
				hasHeadquarterInformation = true;
				break;
			}
			
			if (predicateName.equals("http://www.omg.org/spec/EDMC-FIBO/BE/LegalEntities/CorporateBodies/isDomiciledIn")) {
				hasHeadquarterInformation = true;
				break;
			}
		}
		
		if (false == hasHeadquarterInformation) {
			return;
		}
		
		GeoNamesMapper geonamesMapper = PropertyNormalizerUtils.getInstance().getGeoNamesMapper();
		
		Entity headQuarterSite = new Entity(false);
		
		String addressCountryName = null;
		
		List<RDFNode> addressObjects = entity.getRdfObjects("http://ont.thomsonreuters.com/mdaas/HeadquartersAddress");
		if (null != addressObjects) {
			for (RDFNode headquarterAddress : addressObjects) {
				String addressString = headquarterAddress.asLiteral().getLexicalForm();
				
				String[] addressComponents = addressString.split("\\\\n");
				if (null != addressComponents && 1 <= addressComponents.length) {
					addressCountryName = addressComponents[addressComponents.length - 1];
				}
				
				headQuarterSite.addTripleWithLiteral("http://corp.dbpedia.org/ontology#siteAddress", addressString, XSDBaseStringType.XSDstring);
			}
		}
		
		List<RDFNode> phoneNumbers = entity.getRdfObjects("http://permid.org/ontology/organization/hasHeadquartersPhoneNumber");
		if (null != phoneNumbers) {
			for (RDFNode phoneNumber : phoneNumbers) {
				headQuarterSite.addTriple("http://corp.dbpedia.org/ontology#phoneNumber", phoneNumber);
			}
		}
		
		List<RDFNode> faxNumbers = entity.getRdfObjects("http://permid.org/ontology/organization/hasHeadquartersFaxNumber");
		if (null != faxNumbers) {
			for (RDFNode faxNumber : faxNumbers) {
				headQuarterSite.addTriple("http://corp.dbpedia.org/ontology#faxNumber", faxNumber);
			}
		}
		
		boolean addedCountryInformation = false;
		if (null != addressCountryName) {
			try {
				int bracketsIndex = addressCountryName.indexOf("(");
				String cleanCountryName = ((0 < bracketsIndex) ? addressCountryName.substring(0, bracketsIndex).trim() : addressCountryName.trim());
				
				String geonamesIdCountry = geonamesMapper.getGeoNamesCountryId(cleanCountryName, "en");
				if (null != geonamesIdCountry && false == geonamesIdCountry.isEmpty()) {
					ResourceImpl genomanesIdObject = new ResourceImpl(geonamesIdCountry);
					headQuarterSite.addTriple("http://corp.dbpedia.org/ontology#" + "geonamesIdCountry", genomanesIdObject);
					
					String nameEnglish = geonamesMapper.getNameFromId(geonamesIdCountry, "en");
					if (null != nameEnglish) {
						headQuarterSite.addTripleWithLiteral("http://corp.dbpedia.org/ontology#" + "countryName", nameEnglish, "en");
					}
					
					String nameGerman = geonamesMapper.getNameFromId(geonamesIdCountry, "de");
					if (null != nameGerman) {
						headQuarterSite.addTripleWithLiteral("http://corp.dbpedia.org/ontology#" + "countryName", nameGerman, "de");
					}
					
					addedCountryInformation = true;
				}
			} catch (IngestionException e) {
				System.err.println("Exception when trying to get geonames entity: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
		
		// add information about country
		List<RDFNode> domiciledIn = entity.getRdfObjects("http://www.omg.org/spec/EDMC-FIBO/BE/LegalEntities/CorporateBodies/isDomiciledIn");
		if (false == addedCountryInformation && null != domiciledIn) {
			for (RDFNode domiciledInTmp : domiciledIn) {
				String geonamesIdCountry = domiciledInTmp.asResource().getURI();
				
				ResourceImpl genomanesIdObject = new ResourceImpl(geonamesIdCountry);
				headQuarterSite.addTriple("http://corp.dbpedia.org/ontology#" + "geonamesIdCountry", genomanesIdObject);
				
				String nameEnglish = geonamesMapper.getNameFromId(geonamesIdCountry, "en");
				if (null != nameEnglish) {
					headQuarterSite.addTripleWithLiteral("http://corp.dbpedia.org/ontology#" + "countryName", nameEnglish, "en");
				}
				
				String nameGerman = geonamesMapper.getNameFromId(geonamesIdCountry, "de");
				if (null != nameGerman) {
					headQuarterSite.addTripleWithLiteral("http://corp.dbpedia.org/ontology#" + "countryName", nameGerman, "de");
				}
			}
		}
		
		if (false == headQuarterSite.isEmpty()) {
			String uri = entity.getSubjectUri() + "_headquarterSite";
			
			headQuarterSite.setSubjectUri(uri);
			
			headQuarterSite.addTriple("http://corp.dbpedia.org/ontology#" + "siteOf", entity.getSubjectUriObject());
			
			entity.addSubEntity(headQuarterSite);
			
			entity.addTriple("http://corp.dbpedia.org/ontology#" + "hasHeadquarterSite", headQuarterSite.getSubjectUriObject());			
		}
		
		entity.deleteProperty("http://permid.org/ontology/organization/hasHeadquartersPhoneNumber");
		entity.deleteProperty("http://permid.org/ontology/organization/hasHeadquartersFaxNumber");
		entity.deleteProperty("http://www.omg.org/spec/EDMC-FIBO/BE/LegalEntities/CorporateBodies/isDomiciledIn");
		entity.deleteProperty("http://ont.thomsonreuters.com/mdaas/HeadquartersAddress");
	}

}
