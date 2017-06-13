package org.aksw.sdw.ingestion.csv.constants;

public class CorpDbpedia {
	
	public static final String prefix = "http://corp.dbpedia.org/";
	
	public static final String prefixResource = prefix + "resource/";
	
	public static final String prefixOntology = prefix + "ontology#";
	
	public static final String company = prefixOntology + "Company";
	
	/** company status */
	
	public static final String companyStatus = prefixOntology + "orgStatus";	
	public static final String companyStatusActive = prefixOntology + "Active";
	public static final String companyStatusInActive = prefixOntology + "InActive";
	
	/** geo information */
	
	public static final String cityName = prefixOntology + "cityName";
	public static final String cityGeonamesId = prefixOntology + "cityGeonamesId";
	public static final String countyName = prefixOntology + "countyName";
	public static final String countyGeonamesId = prefixOntology + "countyGeonamesId";
	public static final String countryName = prefixOntology + "countryName";
	public static final String countryGeoNamesId = prefixOntology + "countryGeonamesId";
	public static final String phoneNumber = prefixOntology + "phoneNumber";
	public static final String faxNumber = prefixOntology + "faxNumber";
	public static final String postalCode = prefixOntology + "postalCode";
	public static final String hasHeadquarterSite = prefixOntology + "hasHeadquarterSite";
	public static final String hasIncorporatedSite = prefixOntology + "hasIncorporatedSite";
	public static final String hasRegisteredSite = prefixOntology + "hasRegisteredSite";
	
	public static final String eventSite = prefixOntology + "EventSite";
	public static final String hasEventSite = prefixOntology + "hasEventSite";

	
	public static final String customerServicePhoneNr = prefixOntology + "customerServicePhoneNr";
	public static final String stockQuote = prefixOntology + "stockQuote";
	public static final String hasReferenceCustomer = prefixOntology + "hasReferenceCustomer";
	public static final String hasFounder = prefixOntology + "hasFounder";
	public static final String ceoOf = prefixOntology + "ceoOf";
	
	public static final String changedByCompanySale = prefixOntology + "changedByCompanySale";
	public static final String changedByCompanyFoundation = prefixOntology + "changedByCompanyFoundation";
	public static final String changedByCompanySpinoff = prefixOntology + "changedByCompanySpinoff";
	public static final String changedByCompanyMerger = prefixOntology + "changedByCompanyMerger";
	public static final String changedByCompanyAcquisition = prefixOntology + "changedByCompanyAcquisition";
	
	/** data types */
	public static final String dataTypeIdWebsite = prefixOntology + "DataTypeIdWebsite";
	public static final String dataTypeIdString = prefixOntology + "DataTypeIdString";
	
	public static final String twitterChannel = prefixOntology + "twitterChannel";
	
	/** classifications */
	public static final String productCategory = prefixOntology + "productCategory";
	public static final String orgType = prefixOntology + "orgType";
	public static final String orgStatus = prefixOntology + "orgStatus";
	public static final String orgCategory = prefixOntology + "orgCategory";
	public static final String providesTechnology = prefixOntology + "providesTechnology";
	
	/** events */
	
	public static final String IPO = prefixOntology + "IPO";
	public static final String CompanyFoundation = prefixOntology + "CompanyFoundation";
	public static final String CompanySales = prefixOntology + "CompanySales";
	public static final String CompanyExtinction = prefixOntology + "CompanyExtinction";
	public static final String CompanySpinoff = prefixOntology + "CompanySpinoff";
	public static final String CompanyMerger = prefixOntology + "CompanyMerger";
	public static final String CompanyAcquisition = prefixOntology + "CompanyAcquisition";

}
