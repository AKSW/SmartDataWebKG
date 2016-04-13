package org.aksw.sdw.input.warc;

/**
 * Class which stores all WARC info record information
 * 
 * @author kay
 *
 */
public class WarcInfoRecord extends WarcRecord {
	
	protected int warcNumberOfDocuments;
	
	protected int warcFileLength;
	
	protected String warcFileName;
	
	// use copy constructor to ensure that all the data is kept
	public WarcInfoRecord(final WarcRecord original) {
		super(original);
	}
	
	public int getWarcNumberOfDocuments() {
		return this.warcNumberOfDocuments;
	}
	
	public void setGetWarcNumberOfRecords(final int warcNumberOfDocuments) {
		this.warcNumberOfDocuments = warcNumberOfDocuments;
	}
	
	public int getWarcFileLength() {
		return this.warcFileLength;
	}
	
	public void setWarcFileLength(final int warcFileLength) {
		this.warcFileLength = warcFileLength;
	}
	
	public String getWarcFileName() {
		return warcFileName;
	}

	public void setWarcFileName(String warcFileName) {
		this.warcFileName = warcFileName;
	}
}
