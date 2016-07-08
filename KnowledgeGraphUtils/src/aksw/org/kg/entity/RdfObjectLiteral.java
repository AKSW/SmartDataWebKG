package aksw.org.kg.entity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class can be used to store RDF Object information
 * 
 * @author kay
 *
 */
public class RdfObjectLiteral extends RdfObject {
	
	/** data type */
	final String dataType;

	/** pattern which can be used to detect single quotes */
	final static Pattern singleQuotePattern = Pattern.compile("\\\\*\"");
	
	/** pattern which can be used to detect quotes which cover a whole string */
	final static Pattern stringQuotePattern = Pattern.compile("^(" + singleQuotePattern.pattern() +
													")(.+)(" + singleQuotePattern.pattern() + ")$");
	
	/** This pattern can be used to detect a backslash at the end of the string */
	final static Pattern endSlashPattern = Pattern.compile("((\\\\)+(?=$))");

	
	public RdfObjectLiteral(final String objectString) {
		this(objectString, null);
	}

	
	public RdfObjectLiteral(final String objectString, final String dataType) {
		super(correctObjectString(objectString));
		this.dataType = dataType;
	}
	
	public String getDataType() {
		return this.dataType;
	}
	
	public static String correctObjectString(final String inputString) {
		if (null == inputString) {
			return inputString;
		}
		
		String objectString = inputString.trim();
		if (endSlashPattern.matcher(objectString).find()) {			
			objectString = endSlashPattern.matcher(objectString).replaceAll("");
		}
		
//		// remove quotes which cover the whole string

		if (stringQuotePattern.matcher(objectString).find()) {

			int quoteStart = 0;
			int textStart = -1;
			int textEnd = -1;
			StringBuffer buffer = new StringBuffer();
			
			while (quoteStart < objectString.length()) {
				Matcher quoteMatch = singleQuotePattern.matcher(objectString.substring(quoteStart));
				if (quoteMatch.find()) {
					int start = quoteStart + quoteMatch.start();
					int end = quoteStart + quoteMatch.end();
					
					if (0 > textStart &&  0 == start) {
						textStart = quoteStart + quoteMatch.end();
					} else if (0 > textEnd && objectString.length() - 1 <= end){
						textEnd = quoteStart + quoteMatch.start();
					}
					
					quoteStart = quoteStart + quoteMatch.end() + 1;
				} else {
					break;
				}
				
				if (0 <= textStart && 0 <= textEnd) {
					String match = objectString.substring(textStart, textEnd);
					buffer.append(match);
					
					textStart = -1;
					textEnd = -1;
				}
			};
			
			if (0 < buffer.length()) {
				objectString = buffer.toString();
			}
		}
		
		
		
		// replace single quotes with "'"
		objectString = singleQuotePattern.matcher(objectString).replaceAll("'");
		if (objectString.startsWith("\"@")) {
			throw new RuntimeException("Found problem string: " + inputString);
		}
		
		return objectString.trim();
	}
	
	
	@Override
	protected void createString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("\"");
		builder.append(this.objectString);
		builder.append("\"");
		
		if (null != this.dataType) {
			if (this.dataType.startsWith("@")) {
				builder.append(this.dataType);
			} else {
				builder.append("^^<");
				builder.append(this.dataType);
				builder.append("> ");
			}			
		}
		
		super.fullObjectString = builder.toString();
		if (this.fullObjectString.startsWith("\"\"@")) {
			throw new RuntimeException("Found entity with problem: '" + this.objectString + "'" +
									   " and datatype: " + this.dataType);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		RdfObjectLiteral other = (RdfObjectLiteral) obj;
		if (dataType == null) {
			if (other.dataType != null)
				return false;
		} else if (!dataType.equals(other.dataType))
			return false;
		return true;
	}
	
	
}
