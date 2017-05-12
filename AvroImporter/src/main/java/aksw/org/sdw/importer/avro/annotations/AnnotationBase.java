package aksw.org.sdw.importer.avro.annotations;

public class AnnotationBase extends Annotation {
		
	/** text which is covered by this annotation */
	public String text;
	/** normalized text */
	public String textNormalized;
	/** span which is covered by this annoation */
	
	public Span span = new Span();
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((span == null) ? 0 : span.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
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
		AnnotationBase other = (AnnotationBase) obj;
		if (span == null) {
			if (other.span != null)
				return false;
		} else if (!span.equals(other.span))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}
	

	
}
