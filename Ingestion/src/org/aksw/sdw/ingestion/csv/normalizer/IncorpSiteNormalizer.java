package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;

/**
 * This method can be used to add an incorporated sub-entity
 * 
 * @author kay
 *
 */
public class IncorpSiteNormalizer implements PropertyNormalizer {

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {

		List<RDFNode> incorporatedLocations =
				entity.getRdfObjects("http://permid.org/ontology/organization/isIncorporatedIn");
		if (null == incorporatedLocations || incorporatedLocations.isEmpty()) {
			return;
		}
		
		GeoNamesMapper geonamesMapper = PropertyNormalizerUtils.getInstance().getGeoNamesMapper();
		
		Entity incorporatedSite = new Entity(false);
		
		for (RDFNode incorporatedLocation : incorporatedLocations) {
			String geonamesIdCountry = incorporatedLocation.asResource().getURI();
			
			ResourceImpl genomanesIdObject = new ResourceImpl(geonamesIdCountry);
			incorporatedSite.addTriple("http://corp.dbpedia.org/ontology#" + "geonamesIdCountry", genomanesIdObject);
			
			String nameEnglish = geonamesMapper.getNameFromId(geonamesIdCountry, "en");
			if (null != nameEnglish) {
				incorporatedSite.addTripleWithLiteral("http://corp.dbpedia.org/ontology#" + "countryName", nameEnglish, "en");
			}
			
			String nameGerman = geonamesMapper.getNameFromId(geonamesIdCountry, "de");
			if (null != nameGerman) {
				incorporatedSite.addTripleWithLiteral("http://corp.dbpedia.org/ontology#" + "countryName", nameGerman, "de");
			}
		}
		
		
		// if we added data --> add this to the main entity
		if (false == incorporatedSite.isEmpty()) {
			// make sure we mark where this site belongs to
			incorporatedSite.addTriple("http://corp.dbpedia.org/ontology#" + "siteOf", entity.getSubjectUriObject());
			
			String entityUri = entity.getSubjectUri() + "_incorpSite";
			incorporatedSite.setSubjectUri(entityUri);
			
			entity.addSubEntity(incorporatedSite);
			
			entity.addTriple("http://corp.dbpedia.org/ontology#" + "hasIncorporatedSite", incorporatedSite.getSubjectUriObject());
		}		
		
		// not required anymore
		entity.deleteProperty("http://permid.org/ontology/organization/isIncorporatedIn");		
	}

}
