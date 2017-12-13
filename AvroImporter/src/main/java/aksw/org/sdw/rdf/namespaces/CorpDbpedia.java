package  aksw.org.sdw.rdf.namespaces;

public class CorpDbpedia {
	
	public static final String prefix = "http://corp.dbpedia.org/";
	
	public static final String prefixResource = prefix + "resource/";
	
	public static final String prefixOntology = prefix + "ontology#";

	/** classes **/
	public static final String company = prefixOntology + "Company";
	public static final String disaster = prefixOntology + "Disaster";
	public static final String financialEvent = prefixOntology + "FinancialEvent";


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

	/**
	 *  Not added to ontology yet NEW
	 **/
	
	/** nary relation */
	public static final String relation = prefixOntology+"Relation";
	public static final String hasRelationMember = prefixOntology+"hasRelationMember";

	/** FinancialEvnet */
	public static final String hasFinancialEvent = prefixOntology+"hasFinancialEvent";
	public static final String hasEventType = prefixOntology+"hasEventType";
	
	/** Date **/ //TODO multi Format
	public static final String hasDate = prefixOntology+"hasDate";
	
	/** SpinOff */	
	public static final String hasSpinOff = prefixOntology+"hasSpinOff";
	public static final String isSpinOff = prefixOntology+"isSpinOff";
	
	/** Acquired */
	public static final String acquired= prefixOntology+"acquired";
	public static final String acquiredBy = prefixOntology+"acquiredBy";
	
	/** Disaster */
	public static final String hasDisaster = prefixOntology+"hasDisaster";
	
	/** CompanyProject */
	public static final String hasProject = prefixOntology+"Project";
	public static final String isProjectOf = prefixOntology+"ProjectOf";
	
	/** Industry **/
	public static final String industry = prefixOntology+"Industry";

	/** Customer **/
	public static final String customer = prefixOntology+"customer";
	public static final String customerOf = prefixOntology+"customerOf";

	/**
	 * Annotator
	 */
	
	public static final String prefixAnnotator = prefix+"annotator";

	/** MemberRoleResource **/
	public static final String memberRoleResource = prefixOntology+"MemberRoleResource";
	public static final String relationMemberPosition = prefixOntology+"RelationMemberPosition";
	public static final String relationMemberRole = prefixOntology+"RelationMemberRole";
	public static final String relationMember = prefixOntology+"RelationMember";
	public static final String relationMemberMentioned = prefixOntology+"RelationMemberMentioned";

	public static final String hasFounder= prefixOntology+"hasFounder";
	public static final String hasDocType= prefixOntology+"hasDocType";

	/** **/
	public static final String placeHolder = prefix+"placeholder/";
	
}
