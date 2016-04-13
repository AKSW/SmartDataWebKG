package org.aksw.sdw.search.solr;

import org.aksw.sdw.input.warc.WarcInfoRecord;
import org.aksw.sdw.input.warc.WarcRecord;
import org.aksw.sdw.input.warc.WarcResponseRecord;
import org.apache.solr.common.SolrInputDocument;

/**
 * This class can be used to create
 * Apache SOLR input document from WarcRecords
 * 
 * @author kay
 *
 */
public class WarcSolrDocument implements SorlDocumentInput {
	
	/** warc record */
	final WarcRecord warcRecord;
	
	public WarcSolrDocument(final WarcRecord warcRecord) {
		this.warcRecord = warcRecord;
	}
	
	public void addValue(final SolrInputDocument solrDocument, final String fieldName, final Object fieldValue) {
		if (null == solrDocument || null == fieldName || null == fieldValue) {
			return;
		}
		
		solrDocument.addField(fieldName, fieldValue);
		
	}

	@Override
	public SolrInputDocument getSolrInputDocument() {
		if (null == this.warcRecord) {
			return null;
		}
		
		SolrInputDocument solrDocument = new SolrInputDocument();
		
		this.addValue(solrDocument, "content", this.warcRecord.getContent());
		this.addValue(solrDocument, "content-type", this.warcRecord.getContentType());
		this.addValue(solrDocument, "content-length", this.warcRecord.getContentLength());
		
		this.addValue(solrDocument, "warc-data-type", this.warcRecord.getWarcDataType());
		this.addValue(solrDocument, "warc-date", this.warcRecord.getWarcDate());
		this.addValue(solrDocument, "warc-number", this.warcRecord.getWarcNumber());
		this.addValue(solrDocument, "warc-record-id", this.warcRecord.getWarcRecordId());
		this.addValue(solrDocument, "warc-version", this.warcRecord.getWarcVersion());
		
		if (this.warcRecord instanceof WarcInfoRecord) {
			WarcInfoRecord infoRecord = (WarcInfoRecord) this.warcRecord;
			
			this.addValue(solrDocument, "warc-file-length", infoRecord.getWarcFileLength());
			this.addValue(solrDocument, "warc-number-of-documents", infoRecord.getWarcNumberOfDocuments());
			this.addValue(solrDocument, "warc-file-name", infoRecord.getWarcFileName());			
		}
		
		if (this.warcRecord instanceof WarcResponseRecord) {
			WarcResponseRecord responseRecord = (WarcResponseRecord) this.warcRecord;
			
			this.addValue(solrDocument, "warc-ip-address", responseRecord.getWarcIpAddress());
			this.addValue(solrDocument, "warc-payload-digest", responseRecord.getWarcPayloadDigest());
			this.addValue(solrDocument, "warc-target-uri", responseRecord.getWarcTargetUri());
			this.addValue(solrDocument, "warc-trec-id", responseRecord.getWarcTrecId());
		}
		
		for (String operatorName : warcRecord.getOperatorNames()) {
			String operatorValue = warcRecord.getOperatorValue(operatorName);
			if (null == operatorValue) {
				continue;
			}
			
			this.addValue(solrDocument, "op_" + operatorName, operatorValue);
		}
		

		return solrDocument;
	}

}
