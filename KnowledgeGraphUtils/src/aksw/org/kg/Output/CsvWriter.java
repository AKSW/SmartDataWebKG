package aksw.org.kg.Output;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * This class can be used to write CSV content
 * @author kay
 *
 */
public class CsvWriter implements Closeable {
	
	/** path of output file */
	final String filePath;
	
	/** buffered writer instance which will be used to write to CSV file */
	final BufferedWriter bufferedWriter;
	
	/** path to headers of CSV file */
	final String[] headers;
	
	/** specifies the seperator which is used between data items */
	final String separator;
	
	public CsvWriter(final String filePath, final String[] headers) throws IOException {
		this(filePath, headers, ",");
	}
	
	public CsvWriter(final String filePath, final String[] headers, final String seperator) throws IOException {
		
		try {
			this.filePath = filePath;
			this.headers = headers;
			this.separator = seperator;
			
			File file = new File(filePath);
			Writer writer = new FileWriter(file);
			this.bufferedWriter = new BufferedWriter(writer);
		} catch (Exception e) {
			throw new IOException("Was not able to create/open file: " + filePath, e);
		}
		
		try {
			this.writeHeaders();
		} catch (Exception e) {
			throw new IOException("Was not able to write headers to file: " + filePath, e);
		}
	}
	
	/**
	 * This method can be used to write headers to the top of the CSV file
	 * @throws IOException 
	 */
	protected void writeHeaders() throws IOException {
		if (null == this.headers || 0 == this.headers.length || null == this.bufferedWriter) {
			throw new RuntimeException("Class is not properly initialised");
		}
				
		this.writeLine(this.headers);
	}
	
	public void writeLine(final String[] items) throws IOException {
		if (null == items || 0 == items.length) {
			return;
		}
		
		if (this.headers.length != items.length) {
			throw new RuntimeException("Expected " + this.headers.length +
									   " data items and got " + items.length);
		}
		
		for (String item : items) {
			this.bufferedWriter.append(item);
			this.bufferedWriter.append(this.separator);
		}
		
		this.bufferedWriter.newLine();
	}

	@Override
	public void close() throws IOException {
		if (null != this.bufferedWriter) {
			this.bufferedWriter.close();
		}
	}

}
