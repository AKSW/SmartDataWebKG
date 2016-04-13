package org.aksw.sdw.input.warc;

/**
 * WARC Response Record class
 * 
 * @author kay
 *
 */
public class WarcResponseRecord extends WarcRecord {
	
	protected String warcTrecId;
	
	protected String warcIpAddress;
	
	protected String warcPayloadDigest;
	
	protected String warcTargetUri;
	
	public WarcResponseRecord(final WarcRecord original) {
		super(original);
	}
	
	public String getWarcTrecId() {
		return warcTrecId;
	}

	public void setWarcTrecId(String warcTrecId) {
		this.warcTrecId = warcTrecId;
	}

	public String getWarcIpAddress() {
		return warcIpAddress;
	}

	public void setWarcIpAddress(String warcIpAddress) {
		this.warcIpAddress = warcIpAddress;
	}

	public String getWarcPayloadDigest() {
		return warcPayloadDigest;
	}

	public void setWarcPayloadDigest(String warcPayloadDigest) {
		this.warcPayloadDigest = warcPayloadDigest;
	}

	public String getWarcTargetUri() {
		return warcTargetUri;
	}

	public void setWarcTargetUri(String warcTargetUri) {
		this.warcTargetUri = warcTargetUri;
	}
}
