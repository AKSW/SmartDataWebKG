package sdw.aksw.org.sparql;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;

import sdw.aksw.org.config.KgSolrConfig.KgSolrMapping;

public class EntityGeoLocationMapping implements Solr2SparqlMappingInterface {
	
	final static protected Pattern coordinatesPattern = Pattern.compile("(?<=POINT\\()([0-9.]+)(\\s+)([0-9.]+)");

	@Override
	public void fillFieldDataMap(final QuerySolution querySolution, final KgSolrMapping mapping,
			final String matchingVarName, final String solrFieldName, final Map<String, Set<String>> fieldDataMap) {
		RDFNode node = querySolution.get(matchingVarName);
		if (null == node) {
			return;
		}
		
		if (false == node.isLiteral()) {
			return;
		}
		
		String coordinatesLiteral = node.asLiteral().getLexicalForm();
		
		Matcher matcher = coordinatesPattern.matcher(coordinatesLiteral);
		if (3 != matcher.groupCount()) {
			return;
		}
		
		String[] coordinateArray = null;
		while (matcher.find()) {
			int start = matcher.start();
			int end = matcher.end();
			
			coordinateArray = coordinatesLiteral.substring(start, end).split("\\s+");
		}
		
		if (null == coordinateArray || 2 != coordinateArray.length) {
			return;
		}
		
		String latitude = coordinateArray[1];
		String longitude = coordinateArray[0];
		String latLong = latitude + "," + longitude;
		
		fieldDataMap.get("locationLatLon");
		Set<String> coordinateSet0 = fieldDataMap.get("locationLatLon");
		if (null == coordinateSet0) {
			coordinateSet0 = new HashSet<>();
			fieldDataMap.put("locationLatLon", coordinateSet0);
		}
		
		Set<String> coordinateSet1 = fieldDataMap.get("locationRpt");
		if (null == coordinateSet1) {
			coordinateSet1 = new HashSet<>();
			fieldDataMap.put("locationRpt", coordinateSet1);
		}
		
		coordinateSet0.add(latLong);
		coordinateSet1.add(latLong);
	}
}
