package  aksw.org.sdw.rdf.namespaces;

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
	
	/** data types */
	public static final String dataTypeIdWebsite = prefixOntology + "DataTypeIdWebsite";
	public static final String dataTypeIdString = prefixOntology + "DataTypeIdString";
	
	/** classifications */
	public static final String orgType = prefixOntology + "orgType";
	public static final String orgStatus = prefixOntology + "orgStatus";
	
	/** events */
	
	public static final String IPO = prefixOntology + "IPO";
	public static final String CompanyFoundation = prefixOntology + "CompanyFoundation";
	public static final String CompanyExtinction = prefixOntology + "CompanyExtinction";
	
	/** related to products */
	public static final String providesProduct = prefixOntology + "providesProduct";

	/** Not added to ontology yet */
	public static final String relation = prefix+"Relation";
	public static final String relationMemmber = prefixOntology+"hasRelationMember";
	
	
}
