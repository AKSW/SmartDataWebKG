package aksw.org.sdw.importer.avro.annotations.nif;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.trig.TriGParser;

public abstract class DocRdfGenerator	 {
	
	/** next rdf generator */
	final DocRdfGenerator next;
	
	/** graph name which should be used for output of this instance */
	final String graphName;
	
	public DocRdfGenerator(final String graphName, final DocRdfGenerator generator) {
		this.next = generator;
		this.graphName = graphName;
	}
	
	
	public Dataset generateRdfData() {
		Dataset dataset = this.createNewDataset();
		return this.generateRdfData(dataset);
	}

	public void writeRdfDataAsTrig( final OutputStream outputStream ) {
		Objects.requireNonNull(outputStream, "Did not get output stream");
		Dataset dataset = this.generateRdfData();
		RDFDataMgr.write(outputStream, dataset, RDFFormat.TRIG);
	}

	public void writeRdfDataAsTrigWithPrefix( final OutputStream outputStream ) {
		Objects.requireNonNull(outputStream, "Did not get output stream");

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		Dataset dataset = this.generateRdfData();

		RDFDataMgr.write(byteArrayOutputStream, dataset, RDFFormat.TRIG);

		java.util.ArrayList myList = new ArrayList();
		StatementCollector collector = new StatementCollector(myList);

		try {

			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
			RDFParser rdfParser = new TriGParser();
			rdfParser.setRDFHandler(collector);
			rdfParser.parse(byteArrayInputStream, "");

			Iterator<Statement> it = collector.getStatements().iterator();

			RDFWriter rdfStreamWriter = Rio.createWriter(org.eclipse.rdf4j.rio.RDFFormat.TRIG, outputStream);
			rdfStreamWriter.startRDF();

			rdfStreamWriter.handleNamespace("", "http://www.w3.org/2005/11/its/rdf#");
			rdfStreamWriter.handleNamespace("org", "http://www.w3.org/ns/org#");
			rdfStreamWriter.handleNamespace("schema", "http://schema.org/");
			rdfStreamWriter.handleNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
			rdfStreamWriter.handleNamespace("dbo", "http://dbpedia.org/ontology/");
			rdfStreamWriter.handleNamespace("dbco", "http://corp.dbpedia.org/ontology#");
			rdfStreamWriter.handleNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
			rdfStreamWriter.handleNamespace("nif", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#");

			while (it.hasNext()) {
				Statement st = it.next();
				rdfStreamWriter.handleStatement(st);
			}
			rdfStreamWriter.endRDF();
		} catch (RDFParseException rpe) {
			rpe.printStackTrace();
		}catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void writeRdfDataAsQuad(final OutputStream outputStream) {
		Objects.requireNonNull(outputStream, "Did not get output stream");
		
		Dataset dataset = this.generateRdfData();
		RDFDataMgr.write(outputStream, dataset, Lang.NQUADS);
	}
	
	protected Dataset generateRdfData(final Dataset dataset) {		
		Dataset resultDataset = this.addToRdfData_internal(dataset);
		
		if (null != this.next) {
			return this.next.generateRdfData(resultDataset);
		} else {
			return resultDataset;
		}
	}
	
	
	protected Model createNewModel() {
		Model newmodel = ModelFactory.createDefaultModel();
		newmodel.setNsPrefix("dbco", "http://corp.dbpedia.org/ontology#");
		return newmodel;
	}
	
	protected Dataset createNewDataset() {
		return DatasetFactory.create();
	}

	
	/**
	 * This method can be used to add new information to existing RDF model
	 *  
	 * @return RDF Model after processing
	 */
	protected abstract Dataset addToRdfData_internal(final Dataset dataset);


}
