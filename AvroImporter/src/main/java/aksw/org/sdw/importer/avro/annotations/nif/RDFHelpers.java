/**
 * 
 */
package aksw.org.sdw.importer.avro.annotations.nif;

import org.apache.jena.atlas.lib.IRILib;

/**
 * @author kilt
 *
 */
public class RDFHelpers
{
	/**
	 * treats s as component and escapes it, therefore only relative IRIs are returned 
	 * 
	 * @param s
	 * @return
	 */
	public static String createValidIriComponent(String s)
	{
		return IRILib.encodeUriComponent(s);
	}
	
	public static String createValidIRIfromBase(String fragment,String iriBasePrefix)
	{
		return iriBasePrefix+"#"+IRILib.encodeUriComponent(fragment);
	}
}
