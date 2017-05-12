package aksw.org.sdw.importer.avro.annotations;

public interface DataImportAdapter<ImportType> {
	
	/**
	 * This method can be used to import data
	 * from an external resource
	 * 
	 * @param input		- input data which are imported
	 * @param document	- document to which the input data belongs to
	 */
	default void addData(final ImportType input, final Document document) {
		if (null == document || false == this.validIncomingData(input)) {
			throw new RuntimeException("Invalid input parameters");
		}
		
		this.addData_internal(input, document);
	}
	
	/**
	 * This method can be used to import data
	 * from an external resource
	 * 
	 * @param input		- input data which are imported
	 * @param document	- document to which the input data belongs to
	 */
	public void addData_internal(final ImportType input, final Document document);
	
	/**
	 * This method can be used to check whether the input data
	 * is valid for the target class.
	 * 
	 * @param input
	 * @return true if the input data is valid and false otherwise
	 */
	public boolean validIncomingData(final ImportType input);


}
