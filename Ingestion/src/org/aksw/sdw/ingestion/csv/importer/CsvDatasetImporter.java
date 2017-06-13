//package org.aksw.sdw.ingestion.csv.importer;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URL;
//import java.sql.ResultSet;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Pattern;
//
//import org.aksw.sdw.ingestion.csv.IngestionException;
//import org.aksw.sparqlify.config.syntax.NamedViewTemplateDefinition;
//import org.aksw.sparqlify.config.syntax.TemplateConfig;
//import org.aksw.sparqlify.config.syntax.ViewTemplateDefinition;
//import org.aksw.sparqlify.csv.CsvMapperCliMain;
//import org.aksw.sparqlify.csv.CsvParserConfig;
//import org.aksw.sparqlify.csv.InputSupplierCSVReader;
//import org.aksw.sparqlify.csv.TripleIteratorTracking;
//import org.antlr.runtime.RecognitionException;
//import org.apache.log4j.Logger;
//
//import com.google.common.io.InputSupplier;
//import com.hp.hpl.jena.graph.Triple;
//
//import au.com.bytecode.opencsv.CSVReader;
//
///**
// * This class can be used to import a CSV dataset
// * 
// * @author kay
// *
// */
//public class CsvDatasetImporter implements DatasetImporter {
//	
//	final static Logger logger = Logger.getLogger(CsvDatasetImporter.class);
//
//	
//	final String path;
//	
//	final String fileName;
//	
//	final String mappingFilePath;
//
//	/** delimeter which is used in csv file */
//	final Character delimiter;
//	
//	final Pattern delimeterPattern;
//	
//	/** clean-up pattern which can be used to find whitespaces */
//	final static Pattern cleanupPattern = Pattern.compile("(\\s+)");
//	
//	/** iterator which points to triples */
//	TripleIteratorTracking iterator;
//	
//	public CsvDatasetImporter(final String path, final String fileName,
//							  final String mappingFilePath,
//							  final Character delimiter) {
//		this.path = path;
//		this.fileName = fileName;
//		this.mappingFilePath = mappingFilePath;
//		this.delimiter = delimiter;
//		
//		this.delimeterPattern = Pattern.compile("(" + delimiter + ")");
//
//		
//		
//	}
//		
//	/**
//	 * This method is used to obtain CSV header information
//	 * 
//	 * @param csvFile			- reference to CSV file
//	 * @param correctHeaders	- specifies whether headers should be corrected
//	 * @return list of header names
//	 * @throws IngestionException
//	 */
//	protected List<String> readCsvFileHeaders(final File csvFile, final boolean correctHeaders) throws IngestionException {
//		if (null == csvFile) {
//			throw new IngestionException("No CSV file passed in");
//		}
//		
//		// read input file
//		BufferedReader reader = null;
//		try {
//			reader = new BufferedReader(new FileReader(csvFile));
//			// start looking for headers
//			String line;
//			List<String> headerList = null;
//			while (null != (line = reader.readLine())) {
//				line = line.trim();
//				if (0 == line.length()) {
//					// wait for line, which has actual content
//					continue;
//				}
//				
//				// get headers
//				String[] headers = delimeterPattern.split(line);
//				if (null == headers || 0 == headers.length) {
//					return Collections.emptyList();
//				}
//				
//				// clean up headers and store in result list
//				headerList = new ArrayList<>();
//				for (String header : headers) {
//					if (correctHeaders) {
//						String cleanedHeader = header.trim();
//						cleanedHeader = cleanupPattern.matcher(cleanedHeader).replaceAll("_");
//						
//						headerList.add(cleanedHeader);
//					} else {
//						headerList.add(header);
//					}
//				}
//				
//				// get out of loop
//				break;
//			}
//			
//			if (logger.isDebugEnabled()) {
//				logger.debug("Was able to find " + headerList.size() + " headers from " + this.path + "/" + this.fileName);
//			}
//			
//			return headerList;
//		
//		} catch (IOException e) {
//			throw new IngestionException("Exception while reader headers: ", e);
//		} finally {
//			try {
//				if (null != reader) {
//					reader.close();
//				}
//			} catch (IOException e) {
//				throw new IngestionException("Exception while reader headers: ", e);
//			}
//		}
//	}
//	
//	protected void process() throws IOException, IngestionException, RecognitionException {
//		File csvFile = DatasetImporterUtils.getFileInstance(this.path + "/" + this.fileName);
//		File mappingFile = DatasetImporterUtils.getFileInstance(this.mappingFilePath);
//		
//		// read csv file headers
//		List<String> headers;
//		headers = this.readCsvFileHeaders(csvFile, false);
//		if (null == headers || headers.isEmpty()) {
//			logger.warn("Was not able to obtain headers from " + this.path + "/" + this.fileName);			
//			return;
//		}	
//		
//		StringBuffer buffer = new StringBuffer();
//		for (String header : headers) {
//			buffer.append(header.trim());
//			buffer.append(",");						
//		}
//		
//		File tmpDir = new File("/tmp");
//		File tmpCsvFile = File.createTempFile("inputfile", "nt", tmpDir);
//		tmpCsvFile.deleteOnExit();
//		
//		BufferedWriter writer = new BufferedWriter(new FileWriter(tmpCsvFile));
//		writer.write(buffer.toString());
//		writer.newLine();
//		
//		BufferedReader reader = new BufferedReader(new FileReader(csvFile));
//		String line = null;
//		
//		boolean hasNoHeader = true;
//		while (null != (line = reader.readLine())) {
//			// get header and don't write it
//			if (hasNoHeader) {
//				line = line.trim();
//				if (0 == line.length()) {
//					// wait for line, which has actual content
//					continue;
//				}
//				
//				// we have found header
//				hasNoHeader = false;
//				continue;
//			}
//			
//			// add data
//			writer.append(line);
//			writer.newLine();				
//		}
//		
//		reader.close();
//		writer.close();
//		
//		CsvParserConfig csvConfig = new CsvParserConfig();
//        csvConfig.setFieldDelimiter('\"');
//        csvConfig.setEscapeCharacter('\\');
//        csvConfig.setFieldSeparator(this.delimiter);
//		
//        InputSupplier<CSVReader> csvReaderSupplier = new InputSupplierCSVReader(tmpCsvFile, csvConfig);
//		
//		ResultSet resultSet = CsvMapperCliMain.createResultSetFromCsv(csvReaderSupplier, true, 100);
//		if (null == resultSet) {
//			throw new IngestionException("Empty Resultset");
//		}
//		
//		tmpCsvFile.delete();
//		
//		InputStream in = new FileInputStream(mappingFile);
//        TemplateConfig config;
//        try {
//            config = CsvMapperCliMain.readTemplateConfig(in, null);
//        } finally {
//            in.close();
//        }
//		
//		List<NamedViewTemplateDefinition> views = config.getDefinitions();
//	    if(views.isEmpty()) {
//	    	logger.warn("No view definitions found");
//	    }
//
//	    // Index the views by name	    
//	    Map<String, NamedViewTemplateDefinition> viewIndex = CsvMapperCliMain.indexViews(views, null);
//	    if (null == viewIndex) {
//	    	throw new IngestionException("Did not get index views");
//	    }
//
//	    ViewTemplateDefinition view = CsvMapperCliMain.pickView(viewIndex, "");
//		TripleIteratorTracking trackingIt = CsvMapperCliMain.createTripleIterator(resultSet, view);
//		if (null == trackingIt || false == trackingIt.hasNext()) {
//			throw new IngestionException("Was not able to get triples");
//		}
//		
//		this.iterator = trackingIt;
//	}
//
//	@Override
//	public boolean hasNext() throws IngestionException {
//		try {
//			if (null == this.iterator) {
//				process(); // lazy load
//			}
//			
//			return this.iterator.hasNext();
//		} catch (Exception e) {
//			throw new IngestionException("Was not able to look for new triple", e);
//		}
//	}
//
//	@Override
//	public Triple next() throws IngestionException {
//		if (false == this.iterator.hasNext()) {
//			return null;
//		}
//		
//		return this.iterator.next();
//	}
//
//	@Override
//	public void close() throws IOException {
//		// TODO Auto-generated method stub
//		
//	}
//
//}
