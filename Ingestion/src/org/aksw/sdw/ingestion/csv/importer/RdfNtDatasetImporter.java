package org.aksw.sdw.ingestion.csv.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aksw.sdw.ingestion.IngestionException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;

/**
 * This class can be used to import a simple
 * RDF NT file
 * 
 * @author kay
 *
 */
public class RdfNtDatasetImporter implements DatasetImporter {
	
	static final Pattern uriEndingPattern = Pattern.compile("[<>]");
	
	/** Path to either file or directory */
	final String path;
	
	/** predicate mapper */
	final Map<String, RdfMapping> predicateMapper;
	
	/** list of files which should be read */
	Queue<File> fileList;
	
	/** current file */
	BufferedReader currentFile;
	
	Triple currentTriple = null;
	
	public static class RdfMapping {
		public String targetMapping;
		public RDFDatatype dataType;
		public String lang;
		/** specifies whether the original predicate should be append to the targetMappingPrefix */
		public boolean appendToTargetMappingPrefix;
	}
	
	public RdfNtDatasetImporter(final String path, final Map<String, RdfMapping> predicateMapper) {
		this.path = path;
		this.predicateMapper = predicateMapper;
	}
	
	protected void process() throws IngestionException {
		File inputFile = DatasetImporterUtils.getFileInstance(this.path);
		if (null == inputFile) {
			throw new IngestionException("Was not able to get file: " + this.path);
		}
		
		try {
			if (inputFile.isDirectory()) {
				this.fileList = new LinkedList<>(Arrays.asList(inputFile.listFiles()));
			} else {
				this.fileList = new LinkedList<>(Arrays.asList(inputFile));		
			}
			
			this.currentFile = this.getNextCurrentFile();
		} catch (Exception e) {
			throw new IngestionException("Was not able to read input", e);
		}		
	}
	
	protected BufferedReader getNextCurrentFile() throws IngestionException {
		if (null == this.fileList || this.fileList.isEmpty()) {
			return null;
		}
		
		try {
			File file = this.fileList.poll();
	
			FileReader reader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(reader);
		
			// make sure old files are closed
			if (null != this.currentFile) {
				this.currentFile.close();
			}
			this.currentFile =  bufferedReader;
			return bufferedReader;
		} catch (Exception e) {
			throw new IngestionException("Was not able to get new file", e);
		}
	}

	@Override
	public boolean hasNext() throws IngestionException {
		if (null == this.currentFile) {
			this.process();
		}
		
		try {
			String currentLine = null;
			do {
				currentLine = this.currentFile.readLine();
				if (null == currentLine && (null == this.fileList || this.fileList.isEmpty())) {
					this.currentTriple = null;
					return false;
				} else if (null == currentLine) {
					this.getNextCurrentFile();
				}
			} while (null == currentLine);
			
			String[] parts = new String[3];
			
			boolean isBlankNode = false;
			int start0;
			int end0;
			if (currentLine.startsWith("_:")) {
				start0 = currentLine.indexOf(":") + 1;
				end0 = currentLine.indexOf(" ", start0);
				
				isBlankNode = true;
			} else {
				start0 = currentLine.indexOf("<");
				end0 = currentLine.indexOf(">", start0);
			}
			
			parts[0] = currentLine.substring(start0, end0 + 1);
			
			int start1 = currentLine.indexOf("<", end0);
			int end1 = currentLine.indexOf(">", start1);
			
			parts[1] = currentLine.substring(start1, end1 + 1);
			
			int end2 = currentLine.lastIndexOf(".");
			parts[2] = currentLine.substring(end1 + 2, end2).trim();			
			
			String subject = parts[0].trim();
			subject = RdfNtDatasetImporter.uriEndingPattern.matcher(subject).replaceAll("");
			
			Node subjectNode;
			if (false == isBlankNode) {
				subjectNode = NodeFactory.createURI(subject);
			} else {
				// create blank node
				subjectNode = NodeFactory.createAnon(subject);
			}
			
			
			
			String predicate = parts[1].trim();
			predicate = RdfNtDatasetImporter.uriEndingPattern.matcher(predicate).replaceAll("");
			
			RdfMapping mapping = this.predicateMapper.get(predicate);
			
			String mappedPredicate = (null == mapping || null == mapping.targetMapping) ? predicate : mapping.targetMapping;
			RDFDatatype dataType = (null == mapping || null == mapping.targetMapping) ? XSDDatatype.XSDstring : mapping.dataType;
			String lang = (null == mapping) ? null : mapping.lang;
			// filter out id tag for homepages
			if (null != lang && lang.endsWith("@id")) {
				lang = null;
			}
			
			Node predicateNode = NodeFactory.createURI(mappedPredicate);
			
			Node objectNode;
			String object = parts[2].trim();
			
			if (object.trim().startsWith("<")) {
				Matcher matcher = RdfNtDatasetImporter.uriEndingPattern.matcher(object);
				object = matcher.replaceAll("");
				objectNode = NodeFactory.createURI(object);
			} else if (object.contains("@")) {
				int startLang = object.lastIndexOf("@");
				String objectString = object.substring(0, startLang);

				if (null != lang) {
					objectNode = NodeFactory.createLiteral(objectString, lang);
				} else if (0 <= startLang && object.length() - 3 <= startLang) {
					String objectLang = object.substring(startLang + 1);
					
					if (3 >= objectLang.length()) {
						objectNode = NodeFactory.createLiteral(objectString, objectLang);
					} else {
						objectNode = NodeFactory.createLiteral(objectString, dataType);
					}
				} else {
					objectNode = NodeFactory.createLiteral(objectString, dataType);
				}
			} else if (object.contains("^^")) {
				int startType = object.indexOf("^^");
					
				String objectString = object.substring(0, startType).replace("\"", "");
				
				if (null != lang) {
					objectNode = NodeFactory.createLiteral(objectString, lang);
				} else {
					objectNode = NodeFactory.createLiteral(objectString, dataType);
				}
			} else {
				objectNode = NodeFactory.createLiteral(object);
			}
						
			Triple triple = new Triple(subjectNode, predicateNode, objectNode);
			this.currentTriple = triple;
			
			return true;
		} catch (Exception e) {
			throw new IngestionException("Was not able to load next item", e);
		}
	}

	@Override
	public Triple next() throws IngestionException {
		return this.currentTriple;
	}

	@Override
	public void close() throws IOException {
		if (null != this.currentFile) {
			this.currentFile.close();
			this.currentFile = null;
		}		
	}
}
