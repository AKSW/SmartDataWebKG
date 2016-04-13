package org.aksw.sdw.input.warc;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

/**
 * This class can be used to read  * WARC files
 * 
 * It is based on the iterator pattern and with each iteration
 * it creates a new WarcRecord which contains meta information
 * and the actual content/website.
 * 
 * Furthermore the class create a WARC Info Record which contains
 * information about the dataset itself.
 * 
 * @author kay
 *
 */
public class WarcReader implements Iterator<WarcRecord> {

	/** specifies if the input file is compressed */
	protected final boolean sourceIsCompressed;
	
	/** buffered reader instance to input WARC file */
	protected final LineNumberReader bufferedReader;
	
	/** reference to WARC info record */
	protected WarcInfoRecord warcInfoRecord;
	
	/** reference to current WARC record */
	protected WarcRecord currentWarcRecord = null;
	
	/**
	 * 
	 * @param filePath - absolute or relative path to the input file
	 * @throws IOException
	 */
	public WarcReader(final String filePath) throws IOException {
		
		if (null == filePath) {
			throw new IOException("File path is null");
		}
		
		File inputFile = new File(filePath);
		if (false == inputFile.isFile()) {
			URL fileUrl = WarcReader.class.getClassLoader().getResource(filePath);
			if (null == fileUrl) {
				throw new IOException("Can not find file: " + filePath);
			}
			
			inputFile = new File(fileUrl.getFile());
		}
		
		if (filePath.trim().endsWith(".gz")) {
			FileInputStream fis = new FileInputStream(inputFile);
            GZIPInputStream gis = new GZIPInputStream(fis);
            
            InputStreamReader streamReader = new InputStreamReader(gis);
            this.bufferedReader = new LineNumberReader(streamReader);
		} else {
			FileReader fileReader = new FileReader(inputFile);
			this.bufferedReader = new LineNumberReader(fileReader);
		}
		
		this.sourceIsCompressed = false;
	}
	
	public boolean sourceIsCompressed() {
		return this.sourceIsCompressed;
	}
	
	/**
	 * This method can be used to read the next WARC record
	 * 
	 * @param reader
	 * @return WARC-Record instance
	 * @throws IOException
	 */
	protected WarcRecord getNextWarcRecord(final LineNumberReader reader) throws IOException {
		
		WarcRecord warcRecord = null;
		
		// get WARC record meta information
		boolean gotValue = false;
		String line = null;
		while(null != (line = reader.readLine())) {
			line = line.trim();
			
			// get out of here
			if ("".equals(line)) {
				if (gotValue) {
					break;
				} else {
					continue;
				}
			}
			
			gotValue = true;
			
			if (null == warcRecord && line.startsWith("WARC/")) {
				String versionString = line.substring("WARC/".length());
				float versionNumber = Float.parseFloat(versionString);
				
				// new warc record
				warcRecord = new WarcRecord();
				warcRecord.setWarcVersion(versionNumber);
				continue;
			} else if (null == warcRecord) {
				throw new RuntimeException("Did not find WARC version, but: " + line);
			}
			
			int separatorIndex = line.indexOf(":");
			if (0 > separatorIndex) {
				throw new IOException("Invalid WARC file");
			}
			
			// warc record information
			String warcItem = line.substring(0, separatorIndex++).trim().toLowerCase();
			String warcItemValue = line.substring(separatorIndex).trim();
			
			switch(warcItem) {
			case "warc-type":
				if ("warcinfo".equals(warcItemValue)) {
					warcRecord = new WarcInfoRecord(warcRecord);
				} else if ("response".equals(warcItemValue)) {
					warcRecord = new WarcResponseRecord(warcRecord);
				}
				
				warcRecord.setWarcType(warcItemValue);
				break;
			case "warc-date":
				warcRecord.setWarcDate(warcItemValue);
				break;
			case "warc-number-of-documents":
				if (warcRecord instanceof WarcInfoRecord) {
					int warcNumberOfDocuments = Integer.parseInt(warcItemValue);
					((WarcInfoRecord) warcRecord).setGetWarcNumberOfRecords(warcNumberOfDocuments);
				}
				break;
			case "warc-file-length":
				if (warcRecord instanceof WarcInfoRecord) {
					int warcFileLength = Integer.parseInt(warcItemValue);
					((WarcInfoRecord) warcRecord).setWarcFileLength(warcFileLength);
				}
				break;
			case "warc-filename":
				if (warcRecord instanceof WarcInfoRecord) {
					((WarcInfoRecord) warcRecord).setWarcFileName(warcItemValue);
				}
				break;
			case "warc-data-type":
				warcRecord.setWarcDataType(warcItemValue);
				break;
			case "warc-record-id":
				warcRecord.setWarcRecordId(warcItemValue);
				break;
			case "content-type":
				warcRecord.setContentType(warcItemValue);
				break;
			case "content-length":
				int contentLength = Integer.parseInt(warcItemValue);
				warcRecord.setContentLength(contentLength);
			case "warc-trec-id":
				if (warcRecord instanceof WarcResponseRecord) {
					((WarcResponseRecord) warcRecord).setWarcTrecId(warcItemValue);
				}
				break;
			case "warc-ip-address":
				if (warcRecord instanceof WarcResponseRecord) {
					((WarcResponseRecord) warcRecord).setWarcIpAddress(warcItemValue);
				}
				break;
			case "warc-payload-digest":
				if (warcRecord instanceof WarcResponseRecord) {
					((WarcResponseRecord) warcRecord).setWarcPayloadDigest(warcItemValue);
				}
				break;
			case "warc-target-uri":
				if (warcRecord instanceof WarcResponseRecord) {
					((WarcResponseRecord) warcRecord).setWarcTargetUri(warcItemValue);
				}
				break;
			default:
				System.out.println("Did not find WARC record operator: " + warcItem +
								   " for value: " + warcItemValue);
			}
		}
		
		if (null == warcRecord || 0 > warcRecord.getWarcVersion()) {
			return null;
		}		

		// reset line
		line = null;
		gotValue = false;
		
		// add warc operators
		while(null != (line = reader.readLine())) {
			line = line.trim();
			
			// get out of here
			if ("".equals(line)) {
				if (gotValue) {
					break;
				} else {
					continue;
				}
			}
		
			gotValue = true;
			
			if (line.startsWith("HTTP")) {
				warcRecord.addOperator("response", line);
				continue;
			}
			
			int separatorIndex = line.indexOf(":");
			if (0 > separatorIndex) {
				throw new IOException("Invalid WARC file");
			}
			
			// operator record information
			String operatorName = line.substring(0, separatorIndex++).trim().toLowerCase();
			String operatorValue = line.substring(separatorIndex).trim();			
			warcRecord.addOperator(operatorName, operatorValue);
		}
		
		if (warcRecord instanceof WarcInfoRecord) {
			return warcRecord;
		}
		
		if (false == reader.markSupported()) {
			throw new RuntimeException("Find another way!");
		}
		
		final int max = 10000;
		/// TODO km: try to use the content-length information! Find problem!
		StringBuffer buffer = new StringBuffer();
		while(null != (line = reader.readLine())) {
			
			if (line.trim().startsWith("WARC/")) {
				// reset to previous line
				reader.reset();
				break;
			}
			
			// mark current position
			reader.mark(max);
			
			buffer.append(line);
			buffer.append("\n");
		}
		
		if (0 < buffer.length()) {
			warcRecord.setContent(buffer.toString());
		}
		
		return warcRecord;
	}
	
	/**
	 * Get WARC info record which belongs to the WARC info file
	 * 
	 * @return WARC info record of this WARC file
	 */
	public WarcInfoRecord getWarcInfoRecord() {
		return this.warcInfoRecord;
	}

	@Override
	public boolean hasNext() {
		try {
			WarcRecord warcRecord = this.getNextWarcRecord(this.bufferedReader);
			if (null == warcRecord) {
				return false;
			}
			
			if (warcRecord instanceof WarcInfoRecord) {
				this.warcInfoRecord = (WarcInfoRecord) warcRecord;
				
				warcRecord = this.getNextWarcRecord(this.bufferedReader);
				if (null == warcInfoRecord) {
					return false;
				}
			}
			
			this.currentWarcRecord = warcRecord;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return true;
	}

	@Override
	public WarcRecord next() {
		return this.currentWarcRecord;
	}

	@Override
	public void remove() {
		this.currentWarcRecord = null;
	}
}
