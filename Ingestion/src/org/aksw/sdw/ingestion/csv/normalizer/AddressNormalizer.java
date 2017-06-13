package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper;
import org.aksw.sdw.ingestion.csv.utils.NominatimRetriever;
import org.aksw.sdw.ingestion.csv.utils.NominatimRetriever.AddressInformation;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;

import aksw.org.kg.entity.Entity;

public class AddressNormalizer implements PropertyNormalizer {
	
	String searchUrl = "http://nominatim.linkedgeodata.org/";
	
	final NominatimRetriever nominatimRetriever;
	final GeoNamesMapper geoNamesMapper;
	
	public AddressNormalizer() throws IngestionException {
		this.nominatimRetriever = new NominatimRetriever(this.searchUrl);
		this.geoNamesMapper = new GeoNamesMapper(PropertyNormalizerUtils.geoNamesMappingFilePath);
	}

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		
		List<RDFNode> siteUris = entity.getRdfObjects(W3COrg.hasSite);
		if (null == siteUris || siteUris.isEmpty()) {
			return;
		}
		
		try {
			for (RDFNode siteUri : siteUris) {
				
				Entity siteEntity = entity.getSubEntityById(siteUri.asResource().getURI());
				
				boolean errorWithCoordinates = false;
				AddressInformation addressInformation = null;
				
				List<RDFNode> coordinates = siteEntity.getRdfObjects("http://www.opengis.net/ont/geosparql#asWKT");
				if (null != coordinates) {
					for (RDFNode coordinate : coordinates) {
						String coordinateString = coordinate.asLiteral().getLexicalForm();
						
						int start = coordinateString.indexOf("(");
						int end = coordinateString.indexOf(")");
						if (0 > start || 0 > end) {
							continue;
						}
						
						String[] longLat = coordinateString.substring(start + 1, end).split("\\s+");
						if (null == longLat || 0 == longLat.length) {
							errorWithCoordinates = true;
							continue;
						}
						
						String longitude = longLat[0];
						String latitude = longLat[1];
						
						
						addressInformation = nominatimRetriever.getAddressInformationFromCoordinates(longitude, latitude);
						if (null != addressInformation) {
							break;
						}
					}
					
					if (errorWithCoordinates) {
						siteEntity.deleteProperty("http://www.opengis.net/ont/geosparql#asWKT");
					}
				}
				
				// address information
				if (null != addressInformation) {					
					String geonamesIdCountry = this.addCountryInformation(siteEntity, addressInformation.country, "de");
					if (null == geonamesIdCountry) {
						geonamesIdCountry = this.addCountryInformation(siteEntity, addressInformation.country, "en");
					}
					
					// first try German and then English
					if (false == this.addCityInformation(siteEntity, geonamesIdCountry, addressInformation.town, "de")) {
						this.addCityInformation(siteEntity, geonamesIdCountry, addressInformation.town, "en");
					}
				
					/// TODO km: IMPROVE for Rest and add general handling without coordinates!
					this.addCountyInformation(siteEntity, geonamesIdCountry, addressInformation);
					// leave out for now, since it can add false information
					//this.addAddressInformation(siteEntity, geonamesIdCountry, addressInformation);
				}
			}
		} catch (Exception e) {
			throw new IngestionException("Was not able to normalize address", e);
		}
	}
	
	/**
	 * Adds country information to address entity
	 * 
	 * @param addressEntity
	 * @param countryName
	 * @param languageCode
	 * @return Geonames ID of country
	 * @throws IngestionException
	 */
	protected String addCountryInformation(final Entity addressEntity, final String countryName, final String languageCode) throws IngestionException {
		if (null == countryName || null == languageCode) {
			return null;
			
		}
		
		// check for geonames country ids
		String countyGeonamesId = null;
		List<RDFNode> geonamesCountryIds = addressEntity.getRdfObjects(CorpDbpedia.countryGeoNamesId);
		if (null == geonamesCountryIds || geonamesCountryIds.isEmpty()) {
			
			countyGeonamesId = this.geoNamesMapper.
					getGeoNamesCountryId(countryName, languageCode);
			if (null != countyGeonamesId) {				
				addressEntity.addTriple(new PropertyImpl(CorpDbpedia.countryGeoNamesId),
										new ResourceImpl(countyGeonamesId));
			} else {
				return null;
			}
		} else {
			// if we know it --> use it
			countyGeonamesId = geonamesCountryIds.iterator().next().asResource().getURI();
		}
		
		if (null == countyGeonamesId) {
			return null;
		}
		
		List<RDFNode> countryNames = addressEntity.getRdfObjects(CorpDbpedia.countryName);
		if (null == countryNames || 1 >= countryNames.size()) {
		
			String countryNameDe = this.geoNamesMapper.
					getNameFromId(countyGeonamesId, "de");
			if (null != countryNameDe) {				
				addressEntity.addTriple(new PropertyImpl(CorpDbpedia.countryName), new ResourceImpl(countryNameDe, "de"));
			}
			
			String countryNameEn = this.geoNamesMapper.
					getNameFromId(countyGeonamesId, "en");
			if (null != countryNameEn) {				
				addressEntity.addTriple(new PropertyImpl(CorpDbpedia.countryName), new ResourceImpl(countryNameEn, "en"));
			}
		}
		
		return countyGeonamesId;
	}
	
	protected boolean addCityInformation(final Entity addressEntity, final String geonamesIdCountry, final String cityName, final String languageCode) throws IngestionException {
		if (null == cityName || null == languageCode) {
			return false;			
		}
		
		String cityGeonamesId = null;
		
		List<RDFNode> geonamesCityIds = addressEntity.getRdfObjects(CorpDbpedia.cityGeonamesId);
		if (null == geonamesCityIds || geonamesCityIds.isEmpty()) {
			
			if (null != geonamesIdCountry) {
				cityGeonamesId = this.geoNamesMapper.
						getGeoNamesCityId(cityName, languageCode, geonamesIdCountry);
			} 
			
			if (null == cityGeonamesId) {
				cityGeonamesId = this.geoNamesMapper.
						getGeoNamesCityId(cityName, languageCode);
			}
			
			if (null != cityGeonamesId) {				
				addressEntity.addTriple(new PropertyImpl(CorpDbpedia.cityGeonamesId), new ResourceImpl(cityGeonamesId));
			}
		} else {
			// if we know it --> use it
			cityGeonamesId = geonamesCityIds.iterator().next().asResource().getURI();
		}
		
		if (null == cityGeonamesId) {
			return false;
		}
		
		List<RDFNode> cityNames = addressEntity.getRdfObjects(CorpDbpedia.cityName);
		if (null == cityNames || 1 >= cityNames.size()) {

			String cityNameDe = this.geoNamesMapper.
					getNameFromId(cityGeonamesId, "de");
			if (null != cityNameDe) {				
				addressEntity.addTriple(new PropertyImpl(CorpDbpedia.cityName), new ResourceImpl(cityNameDe, "de"));
			}
			
			String cityNameEn = this.geoNamesMapper.
					getNameFromId(cityGeonamesId, "en");
			if (null != cityNameEn) {				
				addressEntity.addTriple(new PropertyImpl(CorpDbpedia.cityName), new ResourceImpl(cityNameEn, "en"));
			}
		}
		
		return true;
	}
	
	protected void addCountyInformation(final Entity addressEntity, final String geonamesIdCountry, final AddressInformation addressInformation) throws IngestionException {
		if (null == addressInformation || null == addressInformation.state) {
			return;			
		}
		
		String countyGeoNamesId = null;
		
		List<RDFNode> geonamesCountyIds = addressEntity.getRdfObjects(CorpDbpedia.countyGeonamesId);
		if (null == geonamesCountyIds || geonamesCountyIds.isEmpty()) {
			
			if (null != geonamesIdCountry) {
				countyGeoNamesId = this.geoNamesMapper.
						getGeoNamesCountyId(addressInformation.state, geonamesIdCountry, "de");
			}			
			
			if (null != countyGeoNamesId) {				
				addressEntity.addTriple(new PropertyImpl(CorpDbpedia.countyGeonamesId), new ResourceImpl(countyGeoNamesId));
			}
		} else {
			// if we know it --> use it
			countyGeoNamesId = geonamesCountyIds.iterator().next().asResource().getURI();
		}
		
		if (null == countyGeoNamesId) {
			return;
		}
		
		List<RDFNode> countyNames = addressEntity.getRdfObjects(CorpDbpedia.countyName);
		if (null == countyNames || 1 >= countyNames.size()) {

			String countyNameDe = this.geoNamesMapper.
					getNameFromId(countyGeoNamesId, "de");
			if (null != countyNameDe) {				
				addressEntity.addTriple(new PropertyImpl(CorpDbpedia.countyName), new ResourceImpl(countyNameDe, "@de"));
			}
			
			String countyNameEn = this.geoNamesMapper.
					getNameFromId(countyGeoNamesId, "en");
			if (null != countyNameEn) {				
				addressEntity.addTriple(new PropertyImpl(CorpDbpedia.countyName), new ResourceImpl(countyNameEn, "@en"));
			}
		}
	}
	
	/**
	 * This method can be used to add address line, post code information to the entity
	 * 
	 * @param addressEntity
	 * @param geonamesIdCountry
	 * @param addressInformation
	 * @throws IngestionException
	 */
	protected void addAddressInformation(final Entity addressEntity, final String geonamesIdCountry, final AddressInformation addressInformation) throws IngestionException {
		if (null == addressInformation || null == addressInformation.road || null == addressInformation.postcode) {
			return;			
		}
		
		List<RDFNode> addresses = addressEntity.getRdfObjects(W3COrg.siteAddress);
		if (null == addresses || addresses.isEmpty()) {
			String addressLine = addressInformation.road;
			addressEntity.addTriple(new PropertyImpl(W3COrg.siteAddress), new ResourceImpl(addressLine));
		}
		
		List<RDFNode> zipCodes = addressEntity.getRdfObjects(CorpDbpedia.postalCode);
		if (null == zipCodes || zipCodes.isEmpty()) {
			String zipCode = addressInformation.postcode;
			addressEntity.addTriple(new PropertyImpl(CorpDbpedia.postalCode), new ResourceImpl(zipCode));
		}
	}
}
