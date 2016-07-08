package aksw.org.kg.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import aksw.org.kg.KgException;
import aksw.org.sdw.kg.datasets.GeoNamesMapper;

/**
 * This class can be used to import a countryinfo.txt mapping from
 * http://download.geonames.org/export/dump/countryInfo.txt
 * 
 * @author kay
 *
 */
public class CsvReader {

	final static Logger logger = Logger.getLogger(GeoNamesMapper.class);

	/** pattern for delimeter */
	final Pattern delimeterPattern;

	/** clean-up pattern which can be used to find whitespaces */
	final static Pattern cleanupPattern = Pattern.compile("(\\s+)");

	/**
	 * map which stores an key/id and then all the fields which were stored for
	 * one entity
	 */
	Map<String, List<String>> idLineMappings = new HashMap<>();

	/** mapping field ID to map which maps value of field to entity id */
	/// TODO km: Think about changing entity ID mapping to List of IDs
	Map<Integer, Map<String, String>> fieldNameValueMap = new HashMap<>();

	/** list of all column names */
	List<String> headerNames = new ArrayList<>();
	
	/** list of mapping fields */
	List<String> mappingFields;
	
	/** synonyms for mapped value names */
	final Map<String, Map<String, List<String>>> mappingFieldsSynonyms;


	/**
	 * 
	 * @param fileName
	 *              - file name to geo names mapping file
	 * @param delimiter
	 * 				- Delimiter
	 * @param mappingFields
	 * 				- input fields which should be mapped
	 * @param mappingFieldsSynonyms
	 * 				- synonym map for mapped fields
	 * @throws KgException
	 */
	public CsvReader(final String fileName,
					 final String delimiter,
					 final List<String> mappingFields,
					 final Map<String, Map<String, List<String>>> mappingFieldsSynonyms) throws KgException {

		URL url = GeoNamesMapper.class.getClassLoader().getResource(fileName);
		if (null == url) {
			throw new KgException("Was not able to find file: " + fileName);
		}

		this.delimeterPattern = Pattern.compile(delimiter);
		
		this.mappingFields = mappingFields;
		
		this.mappingFieldsSynonyms = new HashMap<>();
		for (String columnName : mappingFieldsSynonyms.keySet()) {
			String cleanedColumnName = this.normalizeHeaderName(columnName);			
			this.mappingFieldsSynonyms.put(cleanedColumnName, mappingFieldsSynonyms.get(columnName));
		}

		try {
			File file = new File(url.getFile());
			readCsvFileContent(file, true);
		} catch (Exception e) {
			throw new KgException("Was not able to load input file: " + fileName, e);
		}
	}

	/**
	 * normalizes an input header name to a common format.
	 * 
	 * @param headerName
	 *            - original header name
	 * @return cleaned header
	 */
	protected String normalizeHeaderName(final String headerName) {
		String cleanedHeader = headerName.trim();
		cleanedHeader = cleanupPattern.matcher(cleanedHeader).replaceAll("_");
		return cleanedHeader;
	}

	/**
	 * This method can be used to obtain a clean header name.
	 * 
	 * @param cleanedHeaderName
	 * @return index of the header
	 * @throws KgException
	 */
	protected Integer getHeaderIndex(final String cleanedHeaderName) throws KgException {
		int fieldIndex = -1;
		for (int i = 0; i < this.headerNames.size(); ++i) {
			if (cleanedHeaderName.equals(this.headerNames.get(i))) {
				fieldIndex = i;
				break;
			}
		}

		if (-1 == fieldIndex) {
			throw new KgException("Was not able to find header name: " + cleanedHeaderName);
		}

		return fieldIndex;
	}

	/**
	 * This method is used to obtain CSV header information
	 * 
	 * @param csvFile
	 *            - reference to CSV file
	 * @param correctHeaders
	 *            - specifies whether headers should be corrected
	 * @throws KgException
	 */
	protected void readCsvFileContent(final File csvFile, final boolean correctHeaders)
			throws KgException {
		if (null == csvFile) {
			throw new KgException("No CSV file passed in");
		}

		// read input file
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(csvFile));
			// start looking for headers
			String line;
			List<String> headerList = null;
			while (null != (line = reader.readLine())) {
				line = line.trim();
				if (0 == line.length()) {
					// wait for line, which has actual content
					continue;
				}

				// get headers
				String[] headers = delimeterPattern.split(line);
				if (null == headers || 0 == headers.length) {
					return;
				}

				// clean up headers and store in result list
				headerList = new ArrayList<>();
				for (String header : headers) {
					if (correctHeaders) {
						String cleanedHeader = this.normalizeHeaderName(header);
						headerList.add(cleanedHeader);
					} else {
						headerList.add(header);
					}
				}

				// get out of loop
				break;
			}

			if (null == headerList || headerList.isEmpty()) {
				throw new KgException("Was not able to load header for file: " + csvFile.getAbsolutePath());
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Was able to find " + headerList.size() + " headers from " + csvFile.getAbsolutePath());
			}

			// ensure that the header names are saved
			this.headerNames = headerList;			
			
			// get list of fields which should be mapped			
			int[] mappedIndexFields = new int[this.mappingFields.size()];
			Arrays.fill(mappedIndexFields, -1);
			
			// fill the array with values
			for (int i = 0; i < mappingFields.size(); ++i) {
				String mappedColumn = mappingFields.get(i);

				String cleanedColumnName = this.normalizeHeaderName(mappedColumn);
				int fieldIndex = this.getHeaderIndex(cleanedColumnName);
				mappedIndexFields[i] = fieldIndex;
			}
		

			// this is the key for each line/entity
			Integer key = 0;
			while (null != (line = reader.readLine())) {
				line = line.trim();
				if (0 == line.length()) {
					// wait for line, which has actual content
					continue;
				}

				// get headers
				String[] fields = delimeterPattern.split(line);
				if (null == fields || 1 >= fields.length || this.headerNames.size() < fields.length) {
					continue;
				}

				List<String> fieldValues = new ArrayList<>(fields.length);
				for (String fieldValue : fields) {
					fieldValues.add(fieldValue);
				}

				this.idLineMappings.put(key.toString(), fieldValues);

				for (int fieldIndex : mappedIndexFields) {
					if (-1 == fieldIndex) {
						continue;
					}

					// store
					Map<String, String> fieldValueMap = this.fieldNameValueMap.get(fieldIndex);
					if (null == fieldValueMap) {
						fieldValueMap = new HashMap<>();
					}

					// store the mapping of value to id in the map
					String fieldValue = fieldValues.get(fieldIndex);

					String valueName = fieldValue.trim().toLowerCase();
					fieldValueMap.put(valueName, key.toString());
					this.fieldNameValueMap.put(fieldIndex, fieldValueMap);
					
					String columnName = this.headerNames.get(fieldIndex);

					// allow alternative country names
					Map<String, List<String>> synonymMap = this.mappingFieldsSynonyms.get(columnName);
					if (null != synonymMap && false == synonymMap.isEmpty()) {
						List<String> countryNameSynonyms = synonymMap.get(valueName);
						if (null != countryNameSynonyms) {
							for (String countryNameSynonym : countryNameSynonyms) {
								fieldValueMap.put(countryNameSynonym, key.toString());
							}
						}
					}
				}

				++key;
			}

			return;

		} catch (IOException e) {
			throw new KgException("Exception while reader headers: ", e);
		} finally {
			try {
				if (null != reader) {
					reader.close();
				}
			} catch (IOException e) {
				throw new KgException("Exception while reader headers: ", e);
			}
		}
	}

	/**
	 * This method can be used to obtain a value for an entity
	 * 
	 * @param columnName
	 *            - mapped column name
	 * @param columnValue
	 *            - value of the entity
	 * @param entityColumn
	 *            - entity column name
	 * @return
	 * @throws KgException
	 */
	public String getEntityFieldValue(final String columnName, final String columnValue, final String entityColumn)
			throws KgException {

		String cleanedColumnName = this.normalizeHeaderName(columnName);
		int fieldIndex = this.getHeaderIndex(cleanedColumnName);

		Map<String, String> fieldValueMap = this.fieldNameValueMap.get(fieldIndex);
		if (null == fieldValueMap) {
			throw new KgException("ColumnName is not mapped: " + columnName);
		}

		String key = fieldValueMap.get(columnValue);
		if (null == key) {
			return null;
			//throw new KgException("Was not able to find entity: " + columnValue);
		}

		List<String> entityValues = this.idLineMappings.get(key);
		if (null == entityValues) {
			throw new KgException("Was not able to find entity values: " + columnValue + " and key: " + key);
		}

		String cleanedEntityColumn = this.normalizeHeaderName(entityColumn);
		fieldIndex = this.getHeaderIndex(cleanedEntityColumn);
		String entityValue = entityValues.get(fieldIndex);
		return entityValue;
	}
}
