package org.aksw.sdw.ingestion.csv.constants;

public class VCARD {
	
	public static final String PREFIX = "http://www.w3.org/2006/vcard/ns#";
	
	public static final String ADDR_STR = VCARD.PREFIX + "street-address";
	public static final String ADDR_POST_CODE = VCARD.PREFIX + "postal-code";
	public static final String ADDR_LOCALITY = VCARD.PREFIX + "locality";
	public static final String ADDR_COUNTRY_NAME = VCARD.PREFIX + "country-name";
	public static final String ADDR_REGION = VCARD.PREFIX + "region";
	public static final String ADDR_HAS_ADDRESS = VCARD.PREFIX + "hasAddress";
}
