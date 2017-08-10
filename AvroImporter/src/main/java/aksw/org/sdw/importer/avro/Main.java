package aksw.org.sdw.importer.avro;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.RelationMention;
import aksw.org.sdw.importer.avro.annotations.RelationMentionImporter;
import aksw.org.sdw.importer.avro.annotations.beuth.BeuthImporter;
import aksw.org.sdw.importer.avro.annotations.dfki.Dfki2SdwKgMapper;
import aksw.org.sdw.importer.avro.annotations.dfki.DfkiImporter;
import aksw.org.sdw.importer.avro.annotations.nif.DocRdfGenerator;
import aksw.org.sdw.importer.avro.annotations.nif.NIFAnnotationGenerator;
import aksw.org.sdw.importer.avro.annotations.nif.RelationGenerator;

public class Main {

	enum InputType {
		BEUTH, DFKI, SIEMENS
	};

	public static void main(String[] args) throws IOException {
		// String b[] =
		// {"/home/kilt/Desktop/johannes-kilt/SDW/iter02/1.avro","/home/kilt/Desktop/johannes-kilt/SDW/iter02/1-b.csv"};
		// de.dfki.lt.spree.examples.RelationMentionPrinter.main(b);

		InputType inputType = InputType.DFKI;
		String filePath = null;
		String outputDirectoryPath = null;
		
		System.out.println("SDW Crawled Data Importer");

		HelpFormatter formatter = new HelpFormatter();

		Options options = new Options();
		options.addOption("h", "help", false, "show help.");
		options.addOption("p", "path", true, "path to avro file.");
		options.addOption("t", "type", true, "input type [BEUTH|DFKI|SIEMENS]");
		options.addOption("o", "out", true, "output folder directory");

		CommandLine commandLine = null;
		CommandLineParser parser = new BasicParser();
		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption("h")) {
				formatter.printHelp("avroimporter", options);
				System.exit(0);
			}

			if (commandLine.hasOption("p") && commandLine.hasOption("t")) {
				String str_type = commandLine.getOptionValue("t");
				if (str_type.toLowerCase().equals("beuth")) {
					inputType = InputType.BEUTH;
				} else if (str_type.toLowerCase().equals("dfki")) {
					inputType = InputType.DFKI;
				} else if (str_type.toLowerCase().equals("siemens")) {
					inputType = InputType.SIEMENS;
				} else {
					formatter.printHelp("avroimporter", options);
				}
				filePath = commandLine.getOptionValue("p");
				outputDirectoryPath = commandLine.getOptionValue("o");
			} else {
				formatter.printHelp("avroimporter", options);
				System.exit(1);
			}

		} catch (ParseException e) {
			formatter.printHelp("avroimporter", options);
			System.exit(1);
		}

		String filePrefix;
		File outputDirectory = new File(outputDirectoryPath);

		if (false == outputDirectory.exists() && false == outputDirectory.mkdirs()) {
			System.err.println("Was not able to create directories: " + outputDirectoryPath);
		}

		RelationMentionImporter importer;
		if (InputType.BEUTH == inputType) {
			importer = new BeuthImporter(filePath);
			filePrefix = "beuth";
		} 
		else if (InputType.DFKI == inputType) {
			importer = new DfkiImporter(filePath);
			filePrefix = "dfki";
		}
		else if (InputType.SIEMENS == inputType) {
			importer = null;//TODO Siemens Importer here
			filePrefix = "siemens";
		}
		else {
			throw new UnsupportedOperationException("conversion input type "+inputType+" is not supported");
		}
		
		int count = 0;
	//for all-in-memory conversion 
//		Map<String, Document> foundDocs = importer.getRelationshipMentions();
//		if (null == foundDocs || foundDocs.isEmpty()) {
//			System.err.println("Did not return any results");
//			return;
//		}
//
//		System.out.println("Number of documents: " + foundDocs.size());
//
//		for (Document doc : foundDocs.values()) {
	//for "streaming" conversion
		for ( Map.Entry<String, Document> entry : importer.getRelationshipMentionIterable()) {
			Document doc =entry.getValue(); 

			String countString = Integer.toString(count);

			String leadingZero = "";
			for (int i = 10 - countString.length(); i >= 0; --i) {
				leadingZero += "0";
			}
			if (count>100) break;
			

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
		
		System.out.println("missingMappingsDFKI:###"+Dfki2SdwKgMapper.missingMappings.toString());
		System.out.println("missingRelations:###"+RelationGenerator.missingRelations.toString());
		System.out.println("nary-Relations:###"+RelationGenerator.strangeRelations.toString());
		System.out.println("Number of documents: " + count);
	}
}
