package aksw.org.sdw.importer.avro.annotations.nif;

import java.io.OutputStream;
import java.util.Objects;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

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
	
	public void writeRdfDataAsTrig(final OutputStream outputStream) {
		Objects.requireNonNull(outputStream, "Did not get output stream");
		
		Dataset dataset = this.generateRdfData();
		RDFDataMgr.write(outputStream, dataset, Lang.TRIG);
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
		return ModelFactory.createDefaultModel();
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
