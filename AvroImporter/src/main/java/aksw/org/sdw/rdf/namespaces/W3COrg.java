package  aksw.org.sdw.rdf.namespaces;

/**
 * This class can be used to obtain URIs for the W3C organization ontology
 * 
 * @author kay
 *
 */
public class W3COrg {
	
	final static public String prefix = "http://www.w3.org/ns/org#";
	
	final static public String organization = W3COrg.prefix + "Organization";
	final static public String FormalOrganization = W3COrg.prefix + "FormalOrganization"; 
	
	final static public String identifier = W3COrg.prefix + "identifier";
	
	final static public String classfication = W3COrg.prefix + "classification";
	
	/** property which is subPropertyOf org:linkedTo */
	final static public String affiliated = W3COrg.prefix + "affiliated";

	final static public String site = W3COrg.prefix + "Site";
	final static public String siteOf = W3COrg.prefix + "siteOf";
	final static public String hasSite = W3COrg.prefix + "hasSite";
	final static public String siteAddress = W3COrg.prefix + "siteAddress";
	
	final static public String ChangeEvent = W3COrg.prefix + "ChangeEvent";
	final static public String changedBy = W3COrg.prefix + "changedBy";
	final static public String resultedFrom = W3COrg.prefix + "resultedFrom";
	final static public String originalOrganization = W3COrg.prefix + "originalOrganization";
	final static public String resultingOrganization = W3COrg.prefix + "resultingOrganization";
	
	final static public String unitOf = W3COrg.prefix + "unitOf";
	final static public String subOrganizationOf = W3COrg.prefix + "subOrganizationOf";

	final static public String hasUnit = W3COrg.prefix + "hasUnit";
	final static public String hasSubOrganization = W3COrg.prefix + "hasSubOrganization";
	
	final static public String linkedTo = W3COrg.prefix + "linkedTo";
	final static public String headOf = W3COrg.prefix + "headOf";

}
