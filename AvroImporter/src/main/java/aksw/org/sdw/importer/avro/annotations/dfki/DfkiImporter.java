package aksw.org.sdw.importer.avro.annotations.dfki;
import java.io.BufferedReader;
import java.io.File;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.avro.file.DataFileReader;

import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.RelationMentionImporter;
import de.dfki.lt.spree.io.AvroUtils;

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
			System.err.println(filePath);throw new RuntimeException(e);
		} finally {
			if (null != bufferedReader) {
				try { bufferedReader.close(); } catch (Exception e) {}
			}
		}
		
		return documentMap;
	}
	

	
public Iterator<Map.Entry<String, Document>> getRelationshipMentionIterator() {
		
		return new Iterator<Map.Entry<String, Document>>() {

			DataFileReader<de.dfki.lt.tap.Document> reader;
			
			{
//				BufferedReader bufferedReader = null;		
//				Map<String, Document> documentMap = new LinkedHashMap<>();
				
				try {
					File inputFile = new File(DfkiImporter.this.filePath);
					if (null == inputFile || false == inputFile.exists() || false == inputFile.isFile()) {
						throw new RuntimeException("Was not able to find file: " + DfkiImporter.this.filePath);
					}				
					// get documents
					reader = de.dfki.lt.spree.io.AvroUtils.createReader(inputFile);
					
				} catch (Exception e) {
					System.err.println(filePath); throw new RuntimeException(e); 
				} 
			}
			
			@Override
			public boolean hasNext()
			{
				return reader.hasNext();
			}

			@Override
			public Entry<String, Document> next()
			{
				de.dfki.lt.tap.Document dfkiDoc = reader.next();
				{
					String docId = dfkiDoc.getId();
					DfkiDocumentAdapter currentDoc  = new DfkiDocumentAdapter();
						currentDoc.addData(dfkiDoc, currentDoc);
						currentDoc.id = docId;					
						return new AbstractMap.SimpleEntry<String, Document>(docId, currentDoc)
						{
							private static final long serialVersionUID = 1L;
						}; 
				
				}
			}
			
		};
		
	}

}
