package org.aksw.sdw.ingestion.csv.constants;

public class CompanyHouseUris {
	
	public static final String namespacePrefixCorp = CorpDbpedia.prefixOntology;
	
	public static final String predicateUriRegisteredAddress = namespacePrefixCorp + "registeredAddress";

	public static final String predicateAddressCareOf = namespacePrefixCorp + "addressCareOf";
	public static final String predicateAddressPOBox = namespacePrefixCorp + "addressPOBox";
	public static final String predicateAddressAddressLine1 = namespacePrefixCorp + "addressAddressLine1";
	public static final String predicateAddressAddressLine2 = namespacePrefixCorp + "addressAddressLine2";
	public static final String predicateUriCityName = namespacePrefixCorp + "cityName";
	public static final String predicateUriRegionName = namespacePrefixCorp + "regionName";
	public static final String predicateUriCountryName = namespacePrefixCorp + "countryName";
	public static final String predicateUriZipCode = namespacePrefixCorp + "postal-code";
	public static final String predicateCountryOfOrigin = namespacePrefixCorp + "countryOfOrigin";
}
