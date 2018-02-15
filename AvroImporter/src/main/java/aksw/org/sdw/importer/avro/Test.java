package aksw.org.sdw.importer.avro;

import aksw.org.sdw.importer.avro.annotations.GlobalConfig;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.impl.XSDBaseStringType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.trig.TriGParser;

import javax.xml.bind.annotation.XmlElementDecl;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;

public class Test {
    public static void main(String[] args) {

//        String CONTEXT_FORMAT = "%s#offset_%d_%d";
//
//        System.out.println(new Formatter().format("%s#offset_%d_%d","NEW",21,25).toString());

        Map<String, String > map = new HashMap<>();

        System.out.println(map.get("b"));
//        Model m = ModelFactory.createDefaultModel();
//
//        String subject = "http://dbpedia.org/resource/100";
//        String predicate = "http://dbpedia.org/predicate/200";
//        RDFNode obj1 = m.createLiteral("LanguageBased","en");
//        RDFNode obj2 = m.createTypedLiteral(100.0f, XSDDatatype.XSDfloat);
//        RDFNode obj3 = m.createResource("http://dbpedia.org/resource/300");
//        RDFNode obj4 = m.createTypedLiteral("", XSDBaseStringType.XSDstring);
//
//        Set<String> set = new HashSet<>();
//        set.add(null);
//        set.add("Lol");
//        set.removeAll(Collections.singleton(null));
//
//        for ( String s : set ) {
//            System.out.println(s);
//        }

//        System.out.println("## LanguageBased");
//        GlobalConfig.getInstance().globalFactHash(subject,predicate,obj1);
//        System.out.println("## TypedLiteral");
//        GlobalConfig.getInstance().globalFactHash(subject,predicate,obj2);
//        System.out.println("## Resource");
//        GlobalConfig.getInstance().globalFactHash(subject,predicate,obj3);
//        System.out.println("## EmptyString");
//        System.out.println(GlobalConfig.getInstance().globalFactHash(subject,predicate,obj4));

//        System.out.println("s "+subject);
//        System.out.println("sh "+subjHash);
//        System.out.println("p "+predicate);
//        System.out.println("ph "+predHash);
//        System.out.println("v "+value);
//        System.out.println("vh "+objHash);
//        System.out.println("l "+lada);
//        System.out.println("lh "+ladaHash);
//        String filename = "/home/vehnem/workspace/files_avroconv/rdf/it02/wopref/doc_dfki_00000000097.trig";
//
//        java.util.ArrayList myList = new ArrayList();
//        StatementCollector collector = new StatementCollector(myList);
//
//        try {
//
//            BufferedReader input = Files.newBufferedReader(Paths.get(filename), StandardCharsets.UTF_8);
//
//            RDFParser rdfParser = new TriGParser();
//            rdfParser.setRDFHandler(collector);
//            rdfParser.parse(input, "");
//
//            Iterator<Statement> it = collector.getStatements().iterator();
//
//            FileWriter fw = new FileWriter("out.trig");
//            RDFWriter rdfStreamWriter = Rio.createWriter(RDFFormat.TRIG, fw);
//            rdfStreamWriter.startRDF();
//            rdfStreamWriter.handleNamespace("", "http://www.w3.org/2005/11/its/rdf#");
//            rdfStreamWriter.handleNamespace("org", "http://www.w3.org/ns/org#");
//            rdfStreamWriter.handleNamespace("schema", "http://schema.org/");
//            rdfStreamWriter.handleNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
//            rdfStreamWriter.handleNamespace("dbo", "http://dbpedia.org/ontology/");
//            rdfStreamWriter.handleNamespace("dbco", "http://corp.dbpedia.org/ontology#");
//            rdfStreamWriter.handleNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
//            rdfStreamWriter.handleNamespace("nif", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#");
//            while (it.hasNext()) {
//                Statement st = it.next();
//                rdfStreamWriter.handleStatement(st);
//            }
//            rdfStreamWriter.endRDF();
//        } catch (Exception e) {
//            e.printStackTrace();
//
//        }
    }
}
