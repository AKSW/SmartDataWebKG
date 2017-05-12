package aksw.org.sdw.importer.avro.annotations.dfki;
import java.io.BufferedReader;
import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.RelationMentionImporter;

public class DfkiImporter implements RelationMentionImporter {
	
	/** path to the input file */
	final String filePath;
	
	/**
	 * 
	 * @param inputFileName	name of the input file
	 */
	public DfkiImporter(final String inputFileName) {
		this.filePath = inputFileName;
	}

	@Override
	public Map<String, Document> getRelationshipMentions() {
		
		BufferedReader bufferedReader = null;		
		Map<String, Document> documentMap = new LinkedHashMap<>();
		
		try {
			File inputFile = new File(this.filePath);
			if (null == inputFile || false == inputFile.exists() || false == inputFile.isFile()) {
				throw new RuntimeException("Was not able to find file: " + this.filePath);
			}	
			
			
			// get documents
			List<de.dfki.lt.tap.Document> dfkiDocs = de.dfki.lt.spree.io.AvroUtils.readDocuments(inputFile);
			if (null == dfkiDocs) {
				return Collections.emptyMap();
			}
			
			for (de.dfki.lt.tap.Document dfkiDoc : dfkiDocs) {
				
				String docId = dfkiDoc.getId();
				DfkiDocumentAdapter currentDoc = (DfkiDocumentAdapter) documentMap.get(docId);
				if (null == currentDoc) {
					currentDoc = new DfkiDocumentAdapter();
					currentDoc.addData(dfkiDoc, currentDoc);
					currentDoc.id = docId;
					
					documentMap.put(docId, currentDoc);
				} else {
					((DfkiDocumentAdapter) currentDoc).addData(dfkiDoc, currentDoc);
				}
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (null != bufferedReader) {
				try { bufferedReader.close(); } catch (Exception e) {}
			}
		}
		
		return documentMap;
	}

}
