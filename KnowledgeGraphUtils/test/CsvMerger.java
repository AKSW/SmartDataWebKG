import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CsvMerger {
	
	static public void main(String[] args) throws IOException {
		System.out.println("Test");
		
		final String resultPath = args[0];
		final String outputTsv = args[1];
		final String delimeter = args[2];
		final int columnNumber = Integer.parseInt(args[3]);
		final String timeoutValue = args[4];
		
		File directory  = new File(resultPath);		
		if (false == directory.isDirectory()) {
			System.err.println("The path is not pointing to a directory");
			return;
		}
		
		
		File outputFile = new File(outputTsv);
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		
		Map<String, BufferedReader> readerMap = new TreeMap<>();
		// created ordered list of file namea
		for (File file : directory.listFiles()) {
			if (false == file.isFile()) {
				continue;
			}
			
			String columnName = file.getName();
			columnName = columnName.replace("results_sdw_blazegraph_", "");
			columnName = columnName.replace("results_sdw_virtuoso_", "");
			columnName = columnName.replace(".csv", "");
			
			
			BufferedReader reader = new BufferedReader(new FileReader(file));
			readerMap.put(columnName, reader);
		}
		
		for (String fileName : readerMap.keySet()) {
			writer.append(fileName);
			writer.append(delimeter);
		}
		
		boolean haveActiveFile = false;
		do {
			haveActiveFile = false;
			
			StringBuilder builder = new StringBuilder();
			boolean isBegin = true;
			for (String fileName : readerMap.keySet()) {
				// get file
				BufferedReader reader = readerMap.get(fileName);
				
				String line = reader.readLine();				
				if (null == line || line.startsWith("END")) {
					builder.append(delimeter);
					continue;
				}
				

				if (line.startsWith("BEGIN")) {
					builder.append(delimeter);
					haveActiveFile = true;
					continue;
				}
				
				haveActiveFile = true;
				isBegin = false; // once we get here, there is no beginning anymore
				
				String[] columns = line.split(",");
				if (6 != columns.length) {
					System.err.println("Column count differs for file: " + fileName);
				}
				
				String value = columns[columnNumber];
				if (null == value || value.isEmpty()) {
					System.err.println("Empty value in file: " + fileName);
				}
				
				if ("timeout".equals(columns[5])) {
					value = timeoutValue;
				}
				
				builder.append(value);
				builder.append(delimeter);
			}
			
			if (false == isBegin) {
				writer.append(builder).append("\n");
			} else {
				writer.newLine();
			}
		} while (haveActiveFile);
		
		writer.close();
	}

}
