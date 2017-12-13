package aksw.org.sdw.importer.avro.annotations.dfki;

import aksw.org.sdw.importer.avro.annotations.DataImportAdapter;
import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.Provenance;

public class DfkiProvenanceAdapter extends Provenance implements DataImportAdapter<de.dfki.lt.tap.Provenance>{
	
	@Override
	public void addData_internal(de.dfki.lt.tap.Provenance provenance, Document document) {
		this.license = provenance.getLicense();
		this.score = (null == provenance.getScore() ? -1.0f : provenance.getScore());
		this.annotator = provenance.getAnnotator();		
		
	}

	@Override
	public boolean validIncomingData(de.dfki.lt.tap.Provenance input) {
		return null != input;
	}
}
