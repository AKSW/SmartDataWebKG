package aksw.org.sdw.importer.avro;

import java.io.*;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import aksw.org.sdw.importer.avro.annotations.GlobalConfig;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.RelationMention;
import aksw.org.sdw.importer.avro.annotations.RelationMentionImporter;
//import aksw.org.sdw.importer.avro.annotations.beuth.BeuthImporter;
import aksw.org.sdw.importer.avro.annotations.dfki.Dfki2SdwKgMapper;
import aksw.org.sdw.importer.avro.annotations.dfki.DfkiImporter;
import aksw.org.sdw.importer.avro.annotations.nif.DocRdfGenerator;
import aksw.org.sdw.importer.avro.annotations.nif.NIFAnnotationGenerator;
import aksw.org.sdw.importer.avro.annotations.nif.RelationGenerator;
import org.apache.jena.base.Sys;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;

import javax.xml.bind.annotation.XmlElementDecl;

public class Main {

	enum InputType {
		BEUTH, DFKI, SIEMENS
	};

	static boolean disablePrefix = false;
	static boolean rOnly = false;
	static String baseUri = null;

	static FileWriter fw = null;
	static {
		try {
			fw = new FileWriter(new File("SemanticRelations.nt"));
		} catch (IOException ioe) {
			Logger.getGlobal().warning("cant write seperate file");
		}
	}

	public static void main(String[] args) throws IOException {
		// String b[] =
		// {"/home/kilt/Desktop/johannes-kilt/SDW/iter02/1.avro","/home/kilt/Desktop/johannes-kilt/SDW/iter02/1-b.csv"};
		// de.dfki.lt.spree.examples.RelationMentionPrinter.main(b);

		InputType inputType = InputType.DFKI;
		String filePath = null;
		String inputDir = null;
		String outputDirectoryPath = null;
		String namespacePrefix = null;


		System.out.println("SDW Crawled Data Importer");

		HelpFormatter formatter = new HelpFormatter();

		Options options = new Options();
		options.addOption("h", "help", false, "show help.");
		options.addOption("p", "path", true, "path to avro file.");
		options.addOption("t", "type", true, "input type [BEUTH|DFKI|SIEMENS]");
		options.addOption("o", "out", true, "output folder directory");
		options.addOption("d", "dir", true, "convert directory to avro");
		options.addOption("i","iterationPrefix", true, "name of iteration cycle");
		options.addOption("c","disablePrefixes", false, "output without @prefix");
		options.addOption("r","relationsOnly", false, "generate rdf for relations only");
		options.addOption("b","baseUri", true, "baseUri for documents");

		CommandLine commandLine = null;
		CommandLineParser parser = new BasicParser();
		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption("h")) {
				formatter.printHelp("avroimporter", options);
				System.exit(0);
			}

			if ( ( commandLine.hasOption("p") || commandLine.hasOption("d") )
					&& commandLine.hasOption("t") && commandLine.hasOption("o")
					&& commandLine.hasOption("i")) {
			
				String str_type = commandLine.getOptionValue("t").toLowerCase();
				if (str_type.equals("beuth")) {
					inputType = InputType.BEUTH;
				} else if (str_type.toLowerCase().equals("dfki")) {
					inputType = InputType.DFKI;
				} else if (str_type.toLowerCase().equals("siemens")) {
					inputType = InputType.SIEMENS;
				} else {
					formatter.printHelp("avroimporter", options);
				}
				GlobalConfig.getInstance().setWasAttributedTo(str_type);
				filePath = commandLine.getOptionValue("p");
				inputDir = commandLine.getOptionValue("d");
				if( commandLine.hasOption("b")) baseUri = commandLine.getOptionValue("b");
				outputDirectoryPath = commandLine.getOptionValue("o");
				namespacePrefix = "http://corp.dbpedia.org/extract/"+commandLine.getOptionValue("i")+"/"+inputType.toString().toLowerCase();
			} else {
				formatter.printHelp("avroimporter", options);
				System.exit(1);
			}
			if( commandLine.hasOption("c")) {
				disablePrefix = true;
			}
			if( commandLine.hasOption("r")) {
				rOnly = true;
			}
			if( commandLine.hasOption("b")) {
				baseUri = commandLine.getOptionValue("b");
			}

		} catch (ParseException e) {
			formatter.printHelp("avroimporter", options);
			System.exit(1);
		}

		File outputDirectory = new File(outputDirectoryPath);

		if (false == outputDirectory.exists() && false == outputDirectory.mkdirs()) {
			System.err.println("Was not able to create directories: " + outputDirectoryPath);
		}

		// for file ?
		if(!commandLine.hasOption('d')) {
			System.out.println("ConvertFile");
			forFile(inputType, filePath, outputDirectoryPath, namespacePrefix);
		// or for directory
		} else {
			System.out.println("ConvertDir");
			File dir = new File(inputDir);
			for(File x : dir.listFiles()) {
				if( x.toString().endsWith(".avro") ) {
					//TODO subfolder name ?
					String[] xarray = x.toString().split("/");
					String subfolderPath = outputDirectoryPath+"/"+xarray[xarray.length-1].replaceAll("\\.", "_");
					File subfolder = new File(subfolderPath);
					if (false == subfolder.exists() && false == subfolder.mkdirs()) {
						System.err.println("Was not able to create directories: " + outputDirectoryPath);
					}
					forFile(inputType, x.toString(), subfolderPath, namespacePrefix);
				}
			}
		}
	}
	
	public static void forFile(InputType inputType, String filePath, String outputDirectoryPath, String namespacePrefix) throws IOException{
		RelationMentionImporter importer;
		String filePrefix;
		//TODO all compatible with dfki?
		if (InputType.BEUTH == inputType) {
			importer = new DfkiImporter(filePath, namespacePrefix, rOnly);
			filePrefix = "beuth";
		} 
		else if (InputType.DFKI == inputType) {
			importer = new DfkiImporter(filePath, namespacePrefix, rOnly);
			filePrefix = "dfki";
		}
		else if (InputType.SIEMENS == inputType) {
			importer = new DfkiImporter(filePath, namespacePrefix, rOnly);
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
			++count;
//			if (count<329 ) continue;
//			if (count>329 ) break;

			GlobalConfig.getInstance().setNifid(UUID.randomUUID().toString());

			if(baseUri != null) doc.uri = baseUri.endsWith("/") ? baseUri : baseUri+"/";
			String countString = Integer.toString(count);

			String leadingZero = "";
			for (int i = 10 - countString.length(); i >= 0; --i) {
				leadingZero += "0";
			}
//			Only x files for test


			leadingZero += countString;

			String outputFile = outputDirectoryPath + "/doc_" + filePrefix + "_" + leadingZero + ".trig";
			//System.out.println(outputFile);
			OutputStream outputStream = new FileOutputStream(new File(outputFile));
			
			Level info_level = Level.INFO;
			Logger.getGlobal().log(info_level,	"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"+"\n"+
					 count + ": Doc ID: " + doc.id +"\n"+
												"Number of concept mentions: " + doc.conceptMentions.size() +"\n"+
												"Number of relationship mentions: " + doc.relationMentions.size());
			
			for (RelationMention relationshipMentions : doc.relationMentions) {
				Level config_level = Level.CONFIG;
				Logger.getGlobal().log(config_level,() -> "Relation: " + relationshipMentions.toJson()); //lambda to enable lazy evaluation for performance benefit when logging disabled
			}
			

			String nifUri = doc.entityIdGenerator.uriNamespace + "/nif/";
			String metadataUri = doc.entityIdGenerator.uriNamespace + "/metadata/";
			DocRdfGenerator rdfGnerator = new NIFAnnotationGenerator(nifUri, doc,
					new RelationGenerator(metadataUri, doc));


			if(disablePrefix)
			rdfGnerator.writeRdfDataAsTrig(outputStream);
			else rdfGnerator.writeRdfDataAsTrigWithPrefix(outputStream);

//			Model mm = ModelFactory.createDefaultModel();
//			mm.add(mm.createResource("http://test.de/n1/"),mm.createProperty("http://test.de/prop/"),mm.createTypedLiteral(100.0f, XSDDatatype.XSDfloat));
//			GlobalConfig.getInstance().addModel(mm);

			StmtIterator stmtIterator = GlobalConfig.getInstance().getModel().listStatements();

			while(stmtIterator.hasNext()) {
				org.apache.jena.rdf.model.Statement statement = stmtIterator.nextStatement();
				String s = "<"+statement.asTriple().getSubject().getURI()+">";
				String p = "<"+statement.asTriple().getPredicate().getURI()+">";
				String o = "";
				if ( statement.asTriple().getObject().isLiteral()) {
					o = "\""+statement.asTriple().getObject().getLiteral().getLexicalForm()+"\"^^<"+statement.asTriple().getObject().getLiteral().getDatatypeURI()+">";
				} else {
					o = "<"+statement.asTriple().getObject().getURI()+">";
				}
				fw.write(s+" "+p+" "+o+" .\n");
			}
			fw.flush();
			GlobalConfig.getInstance().resetModel();
		}
		fw.close();
		
		System.out.println("missingMappingsDFKI:###"+Dfki2SdwKgMapper.missingMappings.toString());
		System.out.println("Misc " +RelationGenerator.missingR);
		System.out.println("missingRelations:###"+RelationGenerator.missingRelations.toString());
		System.out.println("nary-Relations:###"+RelationGenerator.strangeRelations.toString());
		System.out.println("Number of documents: " + count); 

	}
}
