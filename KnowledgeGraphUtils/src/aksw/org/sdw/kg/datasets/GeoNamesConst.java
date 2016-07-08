package aksw.org.sdw.kg.datasets;

public class GeoNamesConst {
	
	public final static String prefix = "http://sws.geonames.org/";
	
	public static String createGeonamesUri(final String geonamesId) {
		if (null == geonamesId || geonamesId.startsWith(prefix)) {
			return geonamesId;
		}
		
		return prefix + geonamesId + "/";
	}

}
