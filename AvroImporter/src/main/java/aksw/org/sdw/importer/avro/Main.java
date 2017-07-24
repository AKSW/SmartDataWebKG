package aksw.org.sdw.importer.avro;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.RelationMention;
import aksw.org.sdw.importer.avro.annotations.RelationMentionImporter;
import aksw.org.sdw.importer.avro.annotations.beuth.BeuthImporter;
import aksw.org.sdw.importer.avro.annotations.dfki.DfkiImporter;
import aksw.org.sdw.importer.avro.annotations.nif.DocRdfGenerator;
import aksw.org.sdw.importer.avro.annotations.nif.NIFAnnotationGenerator;
import aksw.org.sdw.importer.avro.annotations.nif.RelationGenerator;

public class Main  {
	
	enum InputType {BEUTH, DFKI};

	public static void main(String[] args) throws IOException {
//		String b[]  = {"/home/kilt/Desktop/johannes-kilt/SDW/iter02/1.avro","/home/kilt/Desktop/johannes-kilt/SDW/iter02/1-b.csv"};
//		de.dfki.lt.spree.examples.RelationMentionPrinter.main(b);
		System.out.println("SDW Crawled Data Importer");
		
		InputType inputType = InputType.DFKI;
		
		String outputDirectoryPath;
		String filePath;
		String filePrefix;
		if (InputType.BEUTH == inputType) {
			filePath = new File(".").getAbsolutePath() + "/resources/example.json";
			outputDirectoryPath = new File(".").getAbsolutePath() + "/output";
			filePrefix = "beuth";
		} else {
			filePath = new File(".").getAbsolutePath() + "/resources/example.json";
		  filePath = "/home/kilt/Desktop/johannes-kilt/SDW/iter02/1.avro";
			outputDirectoryPath = new File(".").getAbsolutePath() + "/output";
			filePrefix = "dfki";
		}
		
		File outputDirectory = new File(outputDirectoryPath);
		
		if (false == outputDirectory.exists() && false == outputDirectory.mkdirs()) {
			System.err.println("Was not able to create directories: " + outputDirectoryPath);
		}
		
		RelationMentionImporter importer;
		if (InputType.BEUTH == inputType) {
			importer = new BeuthImporter(filePath);
		} else {
			importer = new DfkiImporter(filePath);
		}
		int count = 0;
		
		
		Map<String, Document> foundDocs = importer.getRelationshipMentions();
		if (null == foundDocs || foundDocs.isEmpty()) {
			System.err.println("Did not return any results");
			return;
		}
	
		System.out.println("Number of documents: " + foundDocs.size());
		
		
		for (Document doc : foundDocs.values()) {
		
		
		
		
//		for ( Map.Entry<String, Document> entry : importer.getRelationshipMentionIterable()) {
//			Document doc =entry.getValue();  
			
			String countString = Integer.toString(count);
			
			String leadingZero = "";
			for (int i = 10 - countString.length(); i >= 0; --i) {
				leadingZero += "0";
			}
			
			leadingZero += countString;
			
			String outputFile = outputDirectoryPath + "/doc_" + filePrefix + "_" + leadingZero + ".trig";
			OutputStream outputStream = new FileOutputStream(new File(outputFile));
			
			System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			System.out.println(++count + ": Doc ID: " + doc.id);
			System.out.println("Number of concept mentions: " + doc.conceptMentions.size());
			System.out.println("Number of relationship mentions: " + doc.relationMentions.size());
			
			for (RelationMention relationshipMentions : doc.relationMentions) {
				System.out.println("Relation: " + relationshipMentions.toJson());
			}
			
			String nifUri = doc.entityIdGenerator.uriNamespace + "nif/";
			String metadataUri = doc.entityIdGenerator.uriNamespace + "metadata/";
			DocRdfGenerator rdfGnerator = new NIFAnnotationGenerator(nifUri, doc,
											new RelationGenerator(metadataUri, doc));
			
			System.out.println("Relation: \n");
			
			rdfGnerator.writeRdfDataAsTrig(outputStream);
		}
	}

}
