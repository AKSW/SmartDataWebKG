package org.aksw.sdw.ingestion.csv.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MappingHelper {
	
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
		}
		
		newReader.close();
	}
}