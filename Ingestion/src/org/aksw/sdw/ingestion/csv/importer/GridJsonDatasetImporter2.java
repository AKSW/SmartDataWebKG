package org.aksw.sdw.ingestion.csv.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.Set;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.importer.RdfNtDatasetImporter.RdfMapping;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDbinary;
import org.apache.jena.datatypes.xsd.impl.XSDBaseNumericType;
import org.apache.jena.datatypes.xsd.impl.XSDBaseStringType;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;

/**
 * This class can be used to convert grid.ac json dataset
 * into our common organization format
 * 
 * @author kay
 *
 */
public class GridJsonDatasetImporter2 extends JsonDatasetImporter {
	
	/** current json element of input file */
	protected JsonArray institutes;
	
	protected int instituteIndex = -1;
	
	final protected Entity currentEntity = new Entity(CorpDbpedia.prefix);
	
	Iterator<Triple> resultIterator = null;
	
	final Map<String, RdfMapping> mapping;
	
	/** specifies custom prefix which is used for uri before original ID is added */
	final String idPrefix;

	public GridJsonDatasetImporter2(final String filePath, final String idPrefix, final Map<String, RdfMapping> mapping) {
		super(filePath);
		
		this.mapping = mapping;
		this.idPrefix = idPrefix;
	}
	
	protected JsonArray getJsonInstitutes() throws IngestionException {
		try {
			JsonElement jsonElement = super.getJsonReader().getJson();
			
			JsonObject gridElement = jsonElement.getAsJsonObject();
			JsonArray institutesArray = gridElement.getAsJsonArray("institutes");
			return institutesArray;
		} catch (IOException e) {
			throw new IngestionException("Was not able to read from json instance", e);
		}
	}

	@Override
	public boolean hasNext() throws IngestionException {

		if (null == this.institutes) {
			this.institutes = this.getJsonInstitutes();
		}
		
		if (null != this.resultIterator && this.resultIterator.hasNext()) {
			return true;
		}
		
		// increment the index, since we want to get the next entity
		++this.instituteIndex;
		
		// check whethr the new index is valid
		if (this.instituteIndex >= this.institutes.size()) {
			// we have read all the institutes
			return false;
		}
		
		// get next entity
		JsonObject currentInstitute = this.institutes.get(this.instituteIndex).getAsJsonObject();
		
		String id = currentInstitute.get("id").getAsString().toLowerCase().replace(".", "_");
		String uriString = new StringBuffer().append(CorpDbpedia.prefixResource).
								append(this.idPrefix).append(id).toString();
		Node uri = NodeFactory.createURI(uriString);			
		
		List<Triple> resultTriples = new ArrayList<>();
		this.getJsonObjectTriple(currentInstitute, uri, this.idPrefix, resultTriples);
		
		this.resultIterator = resultTriples.iterator();
		return this.resultIterator.hasNext();
	}
	
	protected void getJsonObjectTriple(final JsonObject entity, final Node uri, final String predicatePrefix, final List<Triple> triples) {
		
		
		Set<Entry<String, JsonElement>> entrySet = entity.entrySet();
		Iterator<Entry<String, JsonElement>> iterator = entrySet.iterator();
		
		while (iterator.hasNext()) {
			Entry<String, JsonElement> nextElement = iterator.next();
			String key = nextElement.getKey();
			JsonElement jsonElement = nextElement.getValue();
						
			String predicateString;
			if (predicatePrefix.isEmpty()) {
				predicateString = key;
			} else {
				predicateString = new StringBuffer().append(predicatePrefix).append(".").append(key).toString();
			}
			this.getJsonElementInformation(jsonElement, uri, predicateString, triples);	
		}
	}
	
	/**
	 * This method can be used to create triple out of input data
	 * 
	 * @param targetMapping
	 * @param uri
	 * @param predicateString
	 * @param objectNode
	 * @return
	 */
	protected Triple createTriple(final RdfMapping targetMapping, final Node uri, final String predicateString, final Node objectNode) {
		final Node predicate;
		if (targetMapping.appendToTargetMappingPrefix) {
			predicate = NodeFactory.createURI(targetMapping.targetMapping + predicateString.replace(".", "/"));
		} else {
			predicate = NodeFactory.createURI(targetMapping.targetMapping);
		}
		
		Triple triple;
		if (false == objectNode.isLiteral()) {
			triple = new Triple(uri, predicate, objectNode);
		} else if (null != targetMapping.lang) {
			String objectString = objectNode.getLiteralValue().toString();
			
			String lang = targetMapping.lang.startsWith("@") ? targetMapping.lang.substring(1) : targetMapping.lang;
			Node languageNode = NodeFactory.createLiteral(objectString, lang);
			triple = new Triple(uri, predicate, languageNode);
		} else if (null != targetMapping.dataType) {
			String objectString = objectNode.getLiteralValue().toString();
			
			RDFDatatype dataType = targetMapping.dataType;
			Node languageNode = NodeFactory.createLiteral(objectString, dataType);
			triple = new Triple(uri, predicate, languageNode);
		} else {
			triple = new Triple(uri, predicate, objectNode);
		}
		
		return triple;
	}
	
	protected void addTriple(final List<Triple> triples, final Node uri, final String predicateString, final Node objectNode) {
		if (this.mapping.containsKey(predicateString)) {
			RdfMapping targetMapping = this.mapping.get(predicateString);			
			
			Triple triple = this.createTriple(targetMapping, uri, predicateString, objectNode);
			triples.add(triple);
			return;
		} 
		
		for (String key : this.mapping.keySet()) {
			Pattern pattern = Pattern.compile("^(" + key + ")(.*)$");
			
			if (pattern.matcher(predicateString).matches()) {
				
				RdfMapping targetMapping = this.mapping.get(key);			
				
				Triple triple = this.createTriple(targetMapping, uri, predicateString, objectNode);
				triples.add(triple);
				return;
			}
		}
		
		Node predicate = NodeFactory.createURI(predicateString);
		Triple triple = new Triple(uri, predicate, objectNode);
		triples.add(triple);
	}
	
	protected void getJsonElementInformation(final JsonElement jsonElement, final Node uri, final String predicateString, final List<Triple> triples) {
		if (null == jsonElement) {
			return;
		}
		
		if (jsonElement.isJsonPrimitive()) {
			JsonPrimitive primitive = (JsonPrimitive) jsonElement;
			if (primitive.getAsString().isEmpty() || primitive.isJsonNull()) {
				return; // no data in here
			}
			
			if (primitive.isBoolean()) {
				Node objectNode = NodeFactory.createLiteral(primitive.getAsString(), XSDbinary.XSDboolean);
				
				this.addTriple(triples, uri, predicateString, objectNode);
			} else if (primitive.isString()) {
				String primitiveString = primitive.getAsString();
				
				Node objectNode;
				if (primitiveString.startsWith("http") && false == primitiveString.contains(" ")) {
					objectNode = NodeFactory.createURI(primitiveString);
				} else {
					objectNode = NodeFactory.createLiteral(primitive.getAsString(), XSDBaseStringType.XSDstring);
				}				
				
				this.addTriple(triples, uri, predicateString, objectNode);
			} else if (primitive.isNumber()) {
				
				// according to JSON definition --> number = double
				/// --> http://www.tutorialspoint.com/json/json_data_types.htm
				Number number = primitive.getAsNumber();
				String numberString = number.toString();
				Double.parseDouble(numberString);
				RDFDatatype dataType = XSDBaseNumericType.XSDdouble;
				
				Node objectNode = NodeFactory.createLiteral(numberString, dataType);
				this.addTriple(triples, uri, predicateString, objectNode);

			} else if (primitive.isJsonObject()) {
				JsonObject jsonObject = primitive.getAsJsonObject();
				this.getJsonObjectTriple(jsonObject, uri, predicateString, triples);
			} else if (primitive.isJsonArray()) {
				JsonArray jsonArray = primitive.getAsJsonArray();

				String uriStringTmpPrev = null;
				for (int i = 0; i < jsonArray.size(); ++i) {
					JsonElement jsonElementNew = jsonArray.get(i);
					
					Node uriTmp = null;
					String newPredicateString = null;
					if (this.mapping.containsKey(".*")) {
						String uriStringTmp = new StringBuffer().append(uri).append("_").append(predicateString.replace(".", "_")).
														append(Integer.toString(i)).toString();
						uriTmp = NodeFactory.createURI(uriStringTmp);
		
						// add link to new predicate instance
						if (null == uriStringTmpPrev || false == uriStringTmpPrev.equals(uriStringTmp)) {
							RdfMapping targetMapping = this.mapping.get(".*");
							
							Triple triple = null;
							if (null == targetMapping) {
								Node predicate = NodeFactory.createURI(predicateString);
								triple = new Triple(uri, predicate, uriTmp);
							} else {
								triple = this.createTriple(targetMapping, uri, predicateString, uriTmp);
							}
							triples.add(triple);
							
							uriStringTmpPrev = uriStringTmp;
							newPredicateString = predicateString;
						}
					} else {
						uriTmp = uri;
						newPredicateString = new StringBuffer().append(predicateString).append(".").
								append(Integer.toString(i)).toString();
					}
					
					
					this.getJsonElementInformation(jsonElementNew, uriTmp, newPredicateString, triples);

				}
			}
		} else if (jsonElement.isJsonObject()) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			this.getJsonObjectTriple(jsonObject, uri, predicateString, triples);
		} else if (jsonElement.isJsonNull()) {
			// ignore
		} else if (jsonElement.isJsonArray()) {
			JsonArray jsonArray = (JsonArray) jsonElement;
			
			String uriStringTmpPrev = null;
			for (int i = 0; i < jsonArray.size(); ++i) {
				JsonElement jsonElementNew = jsonArray.get(i);
				
				Node uriTmp = null;
				String predicateStringTmp = null;
				if (this.mapping.containsKey(".*")) {
					String uriStringTmp = new StringBuffer().append(uri).append("_").append(predicateString.replace(".", "_")).
													append(Integer.toString(i)).toString();
					uriTmp = NodeFactory.createURI(uriStringTmp);
	
					// add link to new predicate instance
					if (null == uriStringTmpPrev || false == uriStringTmpPrev.equals(uriStringTmp)) {
						RdfMapping targetMapping = this.mapping.get(".*");
						
						Triple triple = null;
						if (null == targetMapping) {
							Node predicate = NodeFactory.createURI(predicateString);
							triple = new Triple(uri, predicate, uriTmp);
						} else {
							triple = this.createTriple(targetMapping, uri, predicateString, uriTmp);
						}
						triples.add(triple);
						
						uriStringTmpPrev = uriStringTmp;
						predicateStringTmp = predicateString;
					}
				} else {
					uriTmp = uri;
					predicateStringTmp = new StringBuffer().append(predicateString).append(".").
							append(Integer.toString(i)).toString();
				}
				
				this.getJsonElementInformation(jsonElementNew, uriTmp, predicateStringTmp, triples);
			}
		}
	}

	@Override
	public Triple next() throws IngestionException {
		return this.resultIterator.next();
	}

	@Override
	public void close() throws IOException {
		// nothing to do
	}

}
