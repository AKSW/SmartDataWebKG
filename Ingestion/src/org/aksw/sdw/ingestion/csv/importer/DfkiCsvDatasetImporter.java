package org.aksw.sdw.ingestion.csv.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.SKOS;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

public class DfkiCsvDatasetImporter implements DatasetImporter {
	
	final String inputFilePath;
	
	BufferedReader reader;
	
	final String fieldDelimiter;
	final String fieldSeparator;
	
	final boolean hasHeaders;
	List<String> headers;
	
	final Map<String, ColumnAdapter> columnPredicateMap;
	
	Iterator<Triple> iteratorEntityTriples = null;
	
	Node subjectNode = null;
	
	public DfkiCsvDatasetImporter(final String inputFilePath, final Map<String, ColumnAdapter> columnPredicateMap, final boolean hasHeaders, final String fieldDelimiter, final String fieldSeparator) {
		
		this.inputFilePath = inputFilePath;
		
		this.fieldDelimiter = fieldDelimiter;
		
		this.fieldSeparator = fieldSeparator;
		
		this.hasHeaders = hasHeaders;
		
		this.columnPredicateMap = columnPredicateMap;
		
		try {
			File inputFile = new File(inputFilePath);
			if (false == inputFile.exists() && false == inputFile.isFile()) {
				System.err.println("Was not able to find file: " + this.inputFilePath);
			}
			this.reader = new BufferedReader(new FileReader(inputFile));
		} catch (Exception e) {
			throw new RuntimeException("Was not able to open file: " + this.inputFilePath, e);
		}
	}

	@Override
	public void close() throws IOException {
		if (null != this.reader) {
			this.reader.close();
			this.reader = null;
		}
	}

	@Override
	public boolean hasNext() throws IngestionException {
		try {
			
			if (null != this.iteratorEntityTriples && this.iteratorEntityTriples.hasNext()) {
				return true;
			}
			
			// reset
			this.iteratorEntityTriples  = null;
			this.subjectNode = null;
			
			// get information for next line
			List<String> lineElement = this.getLine();
			if (null == lineElement || lineElement.isEmpty()) {
				return false;
			}

			// collect all triples for this entity
			boolean foundError = false;
			boolean hasAddress = false;
			List<Triple> entityTriples = new ArrayList<>();
			int index = 0;
			for (String header : this.headers) {
				
				String value = lineElement.get(index);
				if (null == value || value.isEmpty()) {
					++index;
					continue;
				}
				
				List<Triple> columnTriples = null;
				switch (header) {
				case "id":
					columnTriples = handleIdentifier(CorpDbpedia.prefixResource + "gcd", value);
					
					break;
				case "company_name":
					columnTriples = handleCompanyName(this.subjectNode, value);
					break;
					
				case "address":
				case "city":
				case "zip_code":
				case "latitude":
				case "longitude":
					if (false == hasAddress) {
						try {
							columnTriples = handleAddress(subjectNode, lineElement);
						} catch (Exception e) {
							// catch Exception
							System.out.println("\n\nJust printout exception! Program continues!");
							e.printStackTrace();
							foundError = true;
							System.out.println("Just printout exception! Program continues!\n\n");
							break;
						}
						
						hasAddress = true;
					}
					break;
					
				case "num_employees_from":
				case "num_employees_to":
					columnTriples = handleEmployeeCount(subjectNode, lineElement);
					break;
				case "judicial_form":
					columnTriples = handleJudicialForm(this.subjectNode, lineElement);
					break;
				case "website":
					columnTriples = handleWebsite(this.subjectNode, lineElement);
					break;
				case "freebase_id":
					columnTriples = handleFreebaseLink(this.subjectNode, lineElement);
					break;
				default:
					System.out.println("Do not know header: " + header);
					break;
				}
				
				if (foundError) {
					break; // skip this entity
				}
				
				if (null != columnTriples && false == columnTriples.isEmpty()) {
					entityTriples.addAll(columnTriples);
					
					// get subject of this entity!!
					if (null == this.subjectNode) {
						this.subjectNode = columnTriples.iterator().next().getSubject();
					}
				}
				
				// increment column index
				++index;
			}
			
			if (foundError) {
				this.iteratorEntityTriples = null;
				this.subjectNode = null;
				
				// no address --> get next one!
				return this.hasNext();
			}
			
			if (false == entityTriples.isEmpty()) {
				this.iteratorEntityTriples = entityTriples.iterator();
			}
			
			if (null != this.iteratorEntityTriples && this.iteratorEntityTriples.hasNext()) {
				return true;
			}
			
			return false;
			
		} catch (IOException e) {
			throw new IngestionException("Problems when retrieving Line", e);
		}
	}

	@Override
	public Triple next() throws IngestionException {
		Triple triple = this.iteratorEntityTriples.next();		
		return triple;
	}
	
	protected List<String> getLine() throws IOException {
		List<String> lineElements = new ArrayList<>();
		
		String line = null;
		line = this.reader.readLine();
		
		if (null == line) {
			return lineElements;
		}
		
		int endField = -1;
		do {
			++endField;
			int startField = line.indexOf(this.fieldDelimiter, endField);
			if (0 > startField) {
				break; // reached end!
			}
			
			endField = line.indexOf(this.fieldDelimiter + this.fieldSeparator, startField);
			if (0 > endField) {
				endField = line.indexOf(this.fieldDelimiter, ((line.length() <= startField + 1) ? line.length() - 1 : startField + 1));
			}
			
			if (startField == endField) {
				break; // found end
			}
			
			// can not find an end
			if (0 >= endField) {
				throw new RuntimeException("Was not able to find an end!");
			}
			
			String fieldValue = line.substring(startField + 1, endField);
			if (null == fieldValue) {
				break;
			}
			
			lineElements.add(fieldValue);
		} while (true);
		
		// if we store headers, then get headers and get next line with content
		if (this.hasHeaders && null == this.headers) {
			this.headers = lineElements;
			lineElements = this.getLine();
		}
		
		return lineElements;
	}
	
	/**
	 * This class can be used to handle operations which have to be used to create a predicate
	 *  
	 * @author kay
	 *
	 */
	public static class ColumnAdapter {
		
		//public METHO predicate = DfkiCsvDatasetImporter::handleIdentifier;
	}
	
	public List<Triple> handleIdentifier(final String uriPrefix, final String value) {
		
		// create subject URI
		String subjectUri = uriPrefix + (uriPrefix.endsWith("_") ? "" : "_") + value;
		Node subject = NodeFactory.createURI(subjectUri);
		String predicate = "http://www.w3.org/ns/adms#identifier";
		Node predicateId = NodeFactory.createURI(predicate);
		
		String subEntityUri = subjectUri + "_gcdIdentifier";
		Node idEntity = NodeFactory.createURI(subEntityUri);

		List<Triple> triples = new ArrayList<>();
		triples.add(new Triple(subject, predicateId, idEntity));
		
		String predicateNotationString = "http://www.w3.org/2004/02/skos/core#notation";
		Node predicateNotation = NodeFactory.createURI(predicateNotationString);

		Node idLiteral = NodeFactory.createLiteral(value, XSDDatatype.XSDstring.getURI());
		triples.add(new Triple(idEntity, predicateNotation, idLiteral));
		
		String predicateCreatorString = "http://purl.org/dc/terms/creator";
		Node predicateCreator = NodeFactory.createURI(predicateCreatorString);
		
		String creatorString = "http://dfki.gcd.de";
		Node creator = NodeFactory.createURI(creatorString);
		triples.add(new Triple(idEntity, predicateCreator, creator));
		
		String idTypeString = "http://www.w3.org/ns/adms#Identifier";
		triples.add(new Triple(idEntity, RDF.type.asNode(), NodeFactory.createURI(idTypeString)));
		
		return triples;
	}
	
	public List<Triple> handleCompanyName(final Node subject, final String value) {
		List<Triple> triples = new ArrayList<>();
		
		Node predicateName = NodeFactory.createURI(SKOS.prefLabel);
		Node nameLiteral = NodeFactory.createLiteral(value, "de");
		triples.add(new Triple(subject, predicateName, nameLiteral));

		return triples;
	}
	
	public List<Triple> handleAddress(final Node subject, final List<String> lineComponents) {
		if (null == lineComponents || lineComponents.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<Triple> triples = new ArrayList<>();
		
		String zipCodeString = getFieldValue("zip_code", lineComponents);
	
		Node predicateAddress = NodeFactory.createURI(W3COrg.hasSite);
		// create entity name --> if no zipCode exists use "0"
		String zipCodeForId = zipCodeString.replaceAll("\\D", "");
		Node addressEntity = NodeFactory.createURI(subject.getURI() + "_" + ((null == zipCodeForId || zipCodeForId.isEmpty()) ? "0" : zipCodeForId));
		triples.add(new Triple(subject, predicateAddress, addressEntity));
		
		triples.add(new Triple(addressEntity, RDF.type.asNode(),  NodeFactory.createURI(W3COrg.site)));

		if (null != zipCodeString && false == zipCodeString.isEmpty()) {
			Node predicateZipCode = NodeFactory.createURI(CorpDbpedia.postalCode);
			Node zipCode = NodeFactory.createLiteral(zipCodeString, XSDDatatype.XSDstring.getURI());
			triples.add(new Triple(addressEntity, predicateZipCode, zipCode));
		}
		
		String cityString = getFieldValue("city", lineComponents);
		if (null != cityString) {
			Node predicateCity = NodeFactory.createURI(CorpDbpedia.cityName);
			Node city = NodeFactory.createLiteral(cityString, "de");
			triples.add(new Triple(addressEntity, predicateCity, city));
		}
		
		String addressString = getFieldValue("address", lineComponents);
		if (null != addressString) {
			Node addressPredicate = NodeFactory.createURI(W3COrg.siteAddress);
			Node address = NodeFactory.createLiteral(addressString, XSDDatatype.XSDstring.getURI());
			triples.add(new Triple(addressEntity, addressPredicate, address));
		}
		

		
		String latitudeString = getFieldValue("latitude", lineComponents);
		String longitudeString = getFieldValue("longitude", lineComponents);
		if (null != latitudeString && false == latitudeString.isEmpty()
			&& null != longitudeString && false == longitudeString.isEmpty()) {
			//"POINT(6.2415399 51.8406475)"^^<http://www.openlinksw.com/schemas/virtrdf#Geometry>  
			String pointString = "POINT(" + longitudeString + " " + latitudeString + ")";
			Node point = NodeFactory.createLiteral(pointString, "http://www.openlinksw.com/schemas/virtrdf#Geometry");
			Node pointPredicate = NodeFactory.createURI("http://www.opengis.net/ont/geosparql#asWKT");
			
			triples.add(new Triple(addressEntity, pointPredicate,  point));
			triples.add(new Triple(addressEntity, RDF.type.asNode(),  NodeFactory.createURI("http://geovocab.org/geometry#Geometry")));
		}
		
		return triples;
	}
	
	protected String getFieldValue(final String columnName, final List<String> lineComponents) {
		if (null == columnName || columnName.isEmpty() || null == lineComponents || lineComponents.isEmpty()) {
			return null;
		}
		
		int columnIndex = headers.indexOf(columnName);
		if (0 > columnIndex) {
			return null;
		}
		
		String value = lineComponents.get(columnIndex);		
		return value;
	}
	
	protected List<Triple> handleEmployeeCount(final Node subject, final List<String> lineComponents) {
		List<Triple> triples = new ArrayList<>();
		
		String employeeCount = this.getFieldValue("num_employees_to", lineComponents);
		if (null != employeeCount) {
			triples.add(new Triple(subject, NodeFactory.createURI("http://dbpedia.org/ontology/numberOfEmployees"),
					NodeFactory.createLiteral(employeeCount, XSDDatatype.XSDinteger.getURI())));
		}
			
		return triples;
	}
	
	protected List<Triple> handleJudicialForm(final Node subject, final List<String> lineComponents) {
		
		String judicialFormString = this.getFieldValue("judicial_form", lineComponents);
		if (null == judicialFormString || judicialFormString.isEmpty()) {
			return null;
		}
		
		Pattern pattern = Pattern.compile("[\\W]+");
		String judicialFormStringClean = pattern.matcher(judicialFormString.trim()).replaceAll("_").toLowerCase();
		
		// emitate old behaviour
		String prefixGcd = CorpDbpedia.prefix + "ontology/de/";
		String typeUriString = prefixGcd + judicialFormStringClean;
		
		Node typeUri = NodeFactory.createURI(typeUriString);		
		Node predicateType = RDF.type.asNode();


		List<Triple> triples = new ArrayList<>();
		triples.add(new Triple(subject, predicateType, typeUri));

		return triples;
	}
	
	protected List<Triple> handleWebsite(final Node subject, final List<String> lineComponents) {
		
		String websiteString = this.getFieldValue("website", lineComponents);
		if (null == websiteString || websiteString.isEmpty()) {
			return null;
		}
		
		Node homepageUri = NodeFactory.createURI(websiteString);		
		Node predicateHomepage = FOAF.homepage.asNode();


		List<Triple> triples = new ArrayList<>();
		triples.add(new Triple(subject, predicateHomepage, homepageUri));

		return triples;
	}
	
	protected List<Triple> handleFreebaseLink(final Node subject, final List<String> lineComponents) {
		
		String freebaseId = this.getFieldValue("freebase_id", lineComponents);
		if (null == freebaseId || freebaseId.isEmpty()) {
			return null;
		}
		
		Node homepageUri = NodeFactory.createURI("<http://rdf.freebase.com/ns/" + freebaseId);		
		Node sameAs = OWL.sameAs.asNode();


		List<Triple> triples = new ArrayList<>();
		triples.add(new Triple(subject, sameAs, homepageUri));

		return triples;
	}
}
