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
		System.out.println("SDW Crawled Data Importer");
		
		InputType inputType = InputType.DFKI;
		
		String outputDirectoryPath;
		String filePath;
		if (InputType.BEUTH == inputType) {
			filePath = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Crawl/it02/Beuth/it02_beuth_company_technology.json";
//			filePath = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Crawl/it02/Beuth/test.json";
			outputDirectoryPath = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Crawl/it02/Beuth/output";
		} else {
			filePath = "/home/kay/Uni/Projects/SmartDataWeb/Code/IngestionRitesh/DFKIAvro/1.avro";
			outputDirectoryPath = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Crawl/it02/DFKI/output";
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
		
		Map<String, Document> foundDocs = importer.getRelationshipMentions();
		if (null == foundDocs || foundDocs.isEmpty()) {
			System.err.println("Did not return any results");
			return;
		}
		
		
		int count = 0;
		System.out.println("Number of documents: " + foundDocs.size());
		for (Document doc : foundDocs.values()) {
			if (1 >=  doc.relationMentions.size()) {
				continue;
			}
			
			String outputFile = outputDirectoryPath + "/" + doc.generatedId + ".trig";
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
