package org.aksw.sdw.ingestion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class LimesAcceptedRemoveDuplicates {

	public static void main(String[] args) throws IOException {
		String fileName = "/home/kay/Programs/Limes/LIMES.0.6.RC4/Release_Examples/accepted.nt";
		File file = new File(fileName);
		
		if (false == file.exists()) {
			System.out.println("File does not exist: " + fileName);
			return;
		}
		
		FileReader reader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(reader);
		
		int lineCount = 0;
		int individualCount = 0;;
		String line = null;
		while (null != (line = bufferedReader.readLine())) {
			++lineCount;
			String[] parts = line.split("\\s++");

			String uri0 = parts[0];
			String uri1 = parts[2];
			
			if (uri0.equals(uri1)) {
				continue;
			}
			
			System.out.println("Line: " + line);
			++individualCount;
		}
		
		bufferedReader.close();
		
		System.out.println("Found lineCount: " + lineCount);
		System.out.println("Found individualCount: " + individualCount);
		
	}

}
