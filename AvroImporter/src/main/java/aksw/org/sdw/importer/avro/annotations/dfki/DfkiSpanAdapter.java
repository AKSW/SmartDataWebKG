package aksw.org.sdw.importer.avro.annotations.dfki;

import aksw.org.sdw.importer.avro.annotations.DataImportAdapter;
import aksw.org.sdw.importer.avro.annotations.Document;
import aksw.org.sdw.importer.avro.annotations.Span;

public class DfkiSpanAdapter extends Span implements DataImportAdapter<de.dfki.lt.tap.Span> {
	
	@Override
	public void addData_internal(de.dfki.lt.tap.Span span, Document document) {
		this.start = span.getStart();
		this.end = span.getEnd();		
	}

	@Override
	public boolean validIncomingData(de.dfki.lt.tap.Span input) {
		return null != input; 
	}
}
