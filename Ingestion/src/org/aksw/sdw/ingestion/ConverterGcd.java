package org.aksw.sdw.ingestion;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;

import aksw.org.kg.KgException;
import aksw.org.kg.input.CsvReader;
import aksw.org.sdw.kg.handler.sparql.SparqlHandler;

public class ConverterGcd {
	
	static String getQuery(final int offset) {
		
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("select distinct * where { Graph <http://dfki.gcd.new.de> { { select ?s { ?s a <http://www.w3.org/ns/org#Organization> } LIMIT 1 OFFSET " + offset + " } " +
					  "?s <http://www.w3.org/ns/adms#identifier>/<http://www.w3.org/2004/02/skos/core#notation> ?id . } } LIMIT 10000");
		
		return buffer.toString();
	}
	
	static public void main(String args[]) throws KgException, IOException {
		System.out.println("Test");
		
		String newFile = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/DFKI/17_03_04/firmendb-export/DFKI_FirmenDB_companies_clean_20170222.csv";
		CsvReader csvReaderGcdNew = new CsvReader(newFile, ",", Arrays.asList("id"), (Map<String, Map<String, List<String>>>) Collections.EMPTY_MAP);
		
		String newFile2 = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/DFKI/17_03_04/firmendb-export/DFKI_FirmenDB_id_mapping_20151216_20170222.csv";
		CsvReader csvReaderIdMapping = new CsvReader(newFile2, ",", Arrays.asList("old_id", "new_id"), (Map<String, Map<String, List<String>>>) Collections.EMPTY_MAP);
		
		String oldFile = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/DFKI/15_12_17/DFKI_FirmenDB_v1_20151216.csv";
		
		MappingHelper mappingHelperOld = new MappingHelper(oldFile);
		MappingHelper mappingHelperNew = new MappingHelper(newFile);
		
		mappingHelperOld.runMapper();
		mappingHelperNew.runMapper();
		
		//System.out.println("Header: " + header);
		System.out.println("ID old count size: " + mappingHelperOld.getIdMap().size());
		System.out.println("ID new count size: " + mappingHelperNew.getIdMap().size());
		
		Map<String, Set<String>> newMap = mappingHelperNew.getIdMap();
		Map<String, Set<String>> oldMap = mappingHelperOld.getIdMap();

		Set<String> maxSet = null;
		int maxCount = 0;
		
		int maxCountNew = 0;
		Set<String> maxSetNew = null;
		int countNew = 0;
		int countDoubleNew = 0;
		
		int countDouble = 0;
		int count = 0;
		
		Map<String, Set<String>> old2NewMap = new HashMap<>();
		
		for (String newHash : newMap.keySet()) {
			Set<String> newIds = newMap.get(newHash);
			Set<String> oldIds = oldMap.get(newHash);
			
			if (null != oldIds && null != newIds) {
				
				for (String oldId : oldIds) {
					Set<String> newIdsSet = old2NewMap.get(oldId);
					if (null == newIdsSet) {
						newIdsSet = new HashSet<>();
						old2NewMap.put(oldId, newIdsSet);					
					}
					
					newIdsSet.addAll(newIds);
				}
			}
			
			
			if (null == oldIds) {
				++count;
				System.out.println("Did not find: " + newHash + " with new ID: " + newIds);
			} else if (1 < oldIds.size()) {
				countDouble++;
				if (oldIds.size() > maxCount) {
					maxCount = oldIds.size();
					maxSet = oldIds;
				}
			} else {
				//System.out.println(newId + "," + oldId);
			}
			
			if (1 < newIds.size()) {
				maxCountNew = newIds.size();
				maxSetNew = newIds;
				countDoubleNew++;
			}
		}
		
		System.out.println("Was not able number of ids: " + count);
		System.out.println("Keys with more than one id: " + countDouble + " with max count: " + maxCount + " and: " + maxSet);
		System.out.println("Keys with more than one id: " + countDoubleNew + " with max count: " + maxCountNew + " and: " + maxSetNew);

		
		String dbpediaLinks = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Server/16_04_01/OrgLinks/Combined/GcdDBpedia/GcdDBpediaLinksSorted.nt";
		File dbpediaLinksFile = new File(dbpediaLinks);
		BufferedReader reader = new BufferedReader(new FileReader(dbpediaLinksFile));
		
		String outputDirectory = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Server/17_02_28/GCD/";
		String linksOutFile = outputDirectory + "dbpediaLinks.nt";
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(linksOutFile)));
		
		
		String uriStart = "<http://corp.dbpedia.org/resource/gcd_";
		String line = null;
		while (null != (line = reader.readLine())) {
			String[] components = line.split("\\s+");
			
			boolean useFirst = false;
			String uri;
			if (components[0].startsWith(uriStart)) {
				uri = components[0];
				useFirst = true;
			} else {
				uri = components[2];
			}
			
			int idStart = uri.lastIndexOf("_");
			
			String oldGcdId = uri.substring(idStart + 1, uri.length() - 1);
			
			Set<String> newIds = old2NewMap.get(oldGcdId);
			if (null == newIds || newIds.isEmpty()) {
				continue;
			}
			
			if (useFirst) {
				for (String newId : newIds) {
					writer.append(uriStart).append(newId).append("> ")
							.append(components[1]).append(" ")
							.append(components[2]).append(" .\n");
				}
			} else {
				for (String newId : newIds) {
					writer.append(components[1]).append(" ")
					.append(components[2]).append(" ")
					.append(uriStart).append(newId).append("> .\n");
				}
			}
			System.out.println(uri + ". Got Ids: " + newIds);
		}
		
		reader.close();
		writer.close();
		
		if (true) {
			return;
		}
		
		String result = csvReaderGcdNew.getEntityFieldValue("id", "1", "company_name");
		System.out.println("Result: " + result);
		
		String result2 = csvReaderIdMapping.getEntityFieldValue("old_id", "84", "new_id");
		System.out.println("Result: " + result2);
		
		SparqlHandler handler = new SparqlHandler("http://localhost:9890/sparql", "http://gcd.dfki.new.de");
		
		String currentString = null;
		int counter = 0;
		
		int index = 0;
		while (true) {
			String query = getQuery(index);
			QueryExecution queryExec = handler.createQueryExecuter(query);
			
			ResultSet gcdResult = queryExec.execSelect();
			if (false == gcdResult.hasNext()) {
				break;
			}

			while (gcdResult.hasNext()) {
				QuerySolution resultTriple = gcdResult.next();
				
				Resource subject = resultTriple.getResource("s");
				Literal id = resultTriple.getLiteral("id");
				
				String subjectString = subject.getURI();
				
				
				
				String idString = id.getLexicalForm();
				if (null == currentString || false == currentString.equals(idString)) {
					currentString = idString;
					
					
					String newId = csvReaderIdMapping.getEntityFieldValue("old_id", idString, "new_id");
					if (null == newId) {
						System.out.println(index + ": Test: " + subject + " and id: " + id.getLexicalForm() + " and new id: " + newId);
						++counter;
					} else {
						int indexStartId = subjectString.lastIndexOf("_");
						String newSubjectString = subjectString.substring(0, indexStartId) + "_" + newId;
						System.out.println(index + ": Test New: " + newSubjectString + " and id: " + id.getLexicalForm() + " and new id: " + newId);
					}
				}
			}
			
			queryExec.close();
			
			++index;
		}
		
		System.out.println("Test: " + counter);
		
		return;
	}
	
	static class MappingHelper {
		
		final String fileName;
		
		final Map<String, Set<String>> idMap = new LinkedHashMap<>();
		
		List<List<String>> csvLines = new ArrayList<>();
		
		List<String> header = new ArrayList<>();
		
		public MappingHelper(final String fileName) {
			this.fileName = fileName;
		}
		
		public Map<String, Set<String>> getIdMap() {
			return Collections.unmodifiableMap(this.idMap);
		}
		
		public List<String> getLine(final int lineNr) {
			return this.csvLines.get(lineNr);
		}
		
		public List<String> getHeaders() {
			return this.header;
		}
		
		public void runMapper() throws IOException {
			File newFileInstance = new File(this.fileName);
			BufferedReader newReader = new BufferedReader(new FileReader(newFileInstance));
			
			boolean hasHeader = false;
			
			String line = null;
			int inputIndex = 0;
			while (null != (line = newReader.readLine())) {
				String[] columns = line.split("\",\"");
				
				List<String> lineData = new ArrayList<>();
				for (String column : columns) {
						
					column = column.replaceAll("\"", "");					
					lineData.add(column);
				}
					
				if (false == hasHeader) {
					hasHeader = true;
					header.addAll(lineData);
				} else {
				
					String currentId = lineData.get(0);
					String companyName = lineData.get(1);
					String zipCode = lineData.get(4);
					if (null == zipCode || zipCode.trim().isEmpty()) {
						//throw new RuntimeException("No zip code in line: " + inputIndex + " and company " + companyName);
						continue;
					}
					
					 String hash = companyName + "_" + zipCode;
					 Set<String> storedId = this.idMap.get(hash);
					 if (null != storedId) {
						 //throw new RuntimeException("Stored Hash: " + hash + " in line: " + inputIndex);
						 storedId.add(currentId);
						 continue;
					 }
					 
					 Set<String> idSet = new LinkedHashSet<>();
					 idSet.add(currentId);
					 this.idMap.put(hash, idSet);
				}
				
				this.csvLines.add(lineData);
							
				++inputIndex;
			}
			
			newReader.close();
		}
	}
}
