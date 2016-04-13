package org.aksw.sdw.input.warc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class stores WARC record information
 * 
 * @author kay
 *
 */
public class WarcRecord {
	
	protected float warcVersion = -1;
	
	protected String warcType;
	
	protected String warcNumber;
	
	protected String warcdDataType;
	
	protected String warcRecordId;
	
	protected String warcDate;
	
	protected String contentType;
	
	protected int contentLength;
	
	protected String warcDataType;
		
	/** stores WARC operators */
	protected Map<String, String> operator = new LinkedHashMap<>();
	
	/** WARC content */
	protected String content;
	
	public WarcRecord() {
		
	}
	
	// use copy constructor to ensure that all the data is kept
	public WarcRecord(final WarcRecord original) {
		this.warcVersion = original.warcVersion;		
		this.warcType  = original.warcType;		
		this.warcNumber = original.warcNumber;		
		this.warcdDataType = original.warcdDataType;		
		this.warcRecordId = original.warcRecordId;		
		this.contentType = original.contentType;		
		this.contentLength = original.contentLength;
		this.content = original.content;
		this.operator.putAll(original.operator);
		this.warcDataType = original.warcDataType;
	}

	public float getWarcVersion() {
		return warcVersion;
	}

	public void setWarcVersion(float warcVersion) {
		this.warcVersion = warcVersion;
	}

	public String getWarcType() {
		return warcType;
	}

	public void setWarcType(String warcType) {
		this.warcType = warcType;
	}

	public String getWarcNumber() {
		return warcNumber;
	}

	public void setWarcNumber(String warcNumber) {
		this.warcNumber = warcNumber;
	}

	public String getWarcdDataType() {
		return warcdDataType;
	}

	public void setWarcdDataType(String warcdDataType) {
		this.warcdDataType = warcdDataType;
	}

	public String getWarcRecordId() {
		return warcRecordId;
	}

	public void setWarcRecordId(String warcRecordId) {
		this.warcRecordId = warcRecordId;
	}
	
	public String getWarcDate() {
		return this.warcDate;
	}
	
	public void setWarcDate(final String warcDate) {
		this.warcDate = warcDate;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public int getContentLength() {
		return contentLength;
	}

	public void setContentLength(final int contentLength) {
		this.contentLength = contentLength;
	}

	public String getOperatorValue(final String operatorName) {
		if (null == operatorName) {
			return null;
		}
		
		String normalisedOperatorName = operatorName.trim().toLowerCase();		
		return operator.get(normalisedOperatorName);
	}
	
	/**
	 * 
	 * @return all WARC record operator names
	 */
	public Set<String> getOperatorNames() {
		return this.operator.keySet();
	}

	public void addOperator(final String operatorName, final String operatorValue) {
		if (null == operatorName || null == operatorValue) {
			throw new NullPointerException("No valid operator");
		}
		
		this.operator.put(operatorName, operatorValue);
	}

	public String getContent() {
		return content;
	}

	public void setContent(final String content) {
		this.content = content;
	}
	
	public String getWarcDataType() {
		return this.warcDataType;
	}
	
	public void setWarcDataType(final String warcDataType) {
		this.warcDataType = warcDataType;
	}
}
