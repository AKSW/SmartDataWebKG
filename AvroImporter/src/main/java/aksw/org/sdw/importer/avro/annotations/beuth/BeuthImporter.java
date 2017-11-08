package aksw.org.sdw.importer.avro.annotations.beuth;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.avro.file.DataFileReader;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.RelationMentionImporter;
import aksw.org.sdw.importer.avro.annotations.dfki.DfkiDocumentAdapter;
import aksw.org.sdw.importer.avro.annotations.dfki.DfkiImporter;

/**
 * This class implements an importer for the Beuth Avro-JSON input file.
 * 
 * @author kay
 *
 */
public class BeuthImporter implements RelationMentionImporter {

	final static String namespacePrefix = "http://corp.dbpedia.org/extract/it02/beuth/";

	/** path to the input file */
	final String filePath;

	/**
	 * 
	 * @param inputFileName
	 *            name of the input file
	 */
	public BeuthImporter(final String inputFileName) {
		this.filePath = inputFileName;
	}

	@Override
	public Map<String, Document> getRelationshipMentions() {
		return null;
	}
//		BufferedReader bufferedReader = null;
//		try {
//			File inputFile = new File(this.filePath);
//			if (null == inputFile || false == inputFile.exists() || false == inputFile.isFile()) {
//				throw new RuntimeException("Was not able to find file: " + this.filePath);
//			}
//
//			bufferedReader = new BufferedReader(new FileReader(inputFile));
//
//			StringBuilder builder = null;
//			String line;
//
//			// only used to find matching doc
//			Map<String, Document> docMap = new HashMap<>();
//			while (null != (line = bufferedReader.readLine())) {
//
//				if (null == builder) {
//					builder = new StringBuilder();
//				}
//
//				builder.append(line);
//
//				// load input text into memory byte stream
//				// --> once entity is loaded --> start parsing and extracting
//				if (line.startsWith("}")) {
//					byte[] jsonBytes = builder.toString().getBytes();
//					ByteArrayInputStream memoryStream = new ByteArrayInputStream(jsonBytes);
//					builder = null; // not required until next JSON object is read
//
//					// read JSON file
//					ObjectMapper mapper = new ObjectMapper();
//					JsonNode node = mapper.readTree(memoryStream);
//
//					JsonNode relationMentions = node.get("relationMentions");
//					JsonNode relationMentionsArray = relationMentions.get("array");
//					if (null != relationMentionsArray && relationMentionsArray.isArray()) {
//						Iterator<JsonNode> iterator = relationMentionsArray.getElements();
//
//						while (iterator.hasNext()) {
//							JsonNode relationMentionNode = iterator.next();
//
//							try {
//								String docId = node.get("id").asText();
//
//								// check if we already now this relation type
//								BeuthDocumentAdapter currentDoc = (BeuthDocumentAdapter) docMap.get(docId);
//								if (null == currentDoc) {
//									currentDoc = new BeuthDocumentAdapter(BeuthImporter.namespacePrefix, docId);
//
//									docMap.put(docId, currentDoc);
//								}
//
//								currentDoc.addData(relationMentionNode, currentDoc);
//							} catch (Exception e) {
//								e.printStackTrace(System.out);
//							}
//						}
//					}
//				}
//			}
//
//			return docMap;
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		} finally {
//			if (null != bufferedReader) {
//				try {
//					bufferedReader.close();
//				} catch (Exception e) {
//				}
//			}
//		}
//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aksw.org.sdw.importer.avro.annotations.RelationMentionImporter#
	 * getRelationshipMentionIterator()
	 */
	@Override
	public Iterator<Entry<String, Document>> getRelationshipMentionIterator() {
		return new Iterator<Map.Entry<String, Document>>() {

			DataFileReader<de.dfki.lt.tap.Document> reader;

			{
				// BufferedReader bufferedReader = null;
				// Map<String, Document> documentMap = new LinkedHashMap<>();

				try {
					File inputFile = new File(BeuthImporter.this.filePath);
					if (null == inputFile || false == inputFile.exists() || false == inputFile.isFile()) {
						throw new RuntimeException("Was not able to find file: " + BeuthImporter.this.filePath);
					}
					// get documents
					reader = de.dfki.lt.spree.io.AvroUtils.createReader(inputFile);

				} catch (Exception e) {
					System.err.println(filePath);
					throw new RuntimeException(e);
				}
			}

			@Override
			public boolean hasNext() {
				return reader.hasNext();
			}

			@Override
			public Entry<String, Document> next() {
				de.dfki.lt.tap.Document dfkiDoc = reader.next();
				{
					String docId = dfkiDoc.getId();
					DfkiDocumentAdapter currentDoc  = new DfkiDocumentAdapter("test");
					currentDoc.addData(dfkiDoc, currentDoc);
					currentDoc.id = docId;	
//					BeuthDocumentAdapter currentDoc = new BeuthDocumentAdapter(BeuthImporter.namespacePrefix, docId);
//					currentDoc.addData( , currentDoc);
					return new AbstractMap.SimpleEntry<String, Document>(docId, currentDoc) {
						private static final long serialVersionUID = 1L;
					};

				}
			}

		};
	}
}
