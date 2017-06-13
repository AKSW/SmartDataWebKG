package org.aksw.sdw.ingestion.csv.importer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import java.util.Set;
import java.util.TimeZone;

import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.SKOS;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.aksw.sdw.ingestion.csv.constants.W3CProvenance;
import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper;
import org.aksw.sdw.ingestion.csv.utils.GeoNamesMapper.Coordinates;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import aksw.org.sdw.kg.handler.sparql.SparqlHandler;

public class DfkiDax30Json implements DatasetImporter {
	
	final String path2JsonFile;
	final JsonArray jsonArray;
	
	int index = -1;
	
	GeoNamesMapper geonamesMaper;
	SparqlHandler sparqlHandlerEn;
	SparqlHandler sparqlHandlerDe;
	
	Set<String> unhandledEntries = new HashSet<>();
	
	Iterator<Triple> tripleIterator;
	
	public DfkiDax30Json(final String path2JsonFile) throws IngestionException {
		try {
			this.path2JsonFile = path2JsonFile;
			
			File jsonFile = new File(path2JsonFile);
			FileReader reader = new FileReader(jsonFile);
			
			JsonParser parser = new JsonParser();
			this.jsonArray = (JsonArray) parser.parse(reader);
			
			reader.close();
		} catch (Exception e) {
			throw new IngestionException("Was not able to read JSON: " + path2JsonFile, e);
		}
		
		this.geonamesMaper = new GeoNamesMapper();
		this.sparqlHandlerEn = new SparqlHandler("http://dbpedia.org/sparql", "http://dbpedia.org");
		this.sparqlHandlerDe = new SparqlHandler("http://de.dbpedia.org/sparql", "http://de.dbpedia.org");

		
	}

	@Override
	public void close() throws IOException {
		
	}

	@Override
	public boolean hasNext() throws IngestionException {
		
		if (null != tripleIterator && tripleIterator.hasNext()) {
			return true;
		}
		
		// reset iterator
		this.tripleIterator = null;
		
		if (++this.index >= this.jsonArray.size()) {
			return false; // reached the end!
		}
		
		// create new entity triples instance
		List<Triple> entityTriples = new ArrayList<>();
		
		JsonObject companyData = (JsonObject) this.jsonArray.get(this.index);
		
		// get names for entity
		Node subject = this.handleNames(companyData, "altName", entityTriples);
		
		// add company types to entity
		handleCompanyTypes(subject, companyData, entityTriples);
		
		Set<Entry<String, JsonElement>> entrySet = companyData.entrySet();
		for (Entry<String, JsonElement> entry : entrySet) {
			String entryName = entry.getKey();
			
			switch (entryName) {
			case "altName":
				break; // done already
			case "idFirma":
				this.handleSameAs(subject, companyData, entityTriples);
				break;
			case "webLink":
			case "wikiLink":
			case "twitter":
				this.handleWebsite(subject, companyData, entityTriples);
				break;
			case "employees":
				this.handleEmployeeCount(subject, companyData, entityTriples);
				break;
			case "wikiAbstract":
				this.handleAbstract(subject, companyData, entityTriples);
				break;
			case "revenues":
				this.handleRevenues(subject, companyData, entityTriples);
				break;
			case "logo":
				this.handleLogo(subject, companyData, entityTriples);
				break;
			case "industries":
				this.handleIndustries(subject, companyData, entityTriples);
				break;
			case "technologies":
				this.handleTechnologies(subject, companyData, entityTriples);
				break;
			case "customerServicePhoneNr":
				this.handleCustomerServicePhoneNr(subject, companyData, entityTriples);
				break;
			case "stockQuote":
				this.handleStockQuote(subject, companyData, entityTriples);
				break;
			case "products":
				this.handleProducts(subject, companyData, entityTriples);
				break;
			case "children":
				this.handleChildren(subject, companyData, entityTriples);
				break;
			case "parent":
				this.handleParent(subject, companyData, entityTriples);
				break;
			case "customers":
				this.handleCustomers(subject, companyData, entityTriples);
				break;
			case "headquarters":
				this.handleHeadquarters(subject, companyData, entityTriples);
				break;
			case "facilities":
				this.handleFacilities(subject, companyData, entityTriples);
				break;
			case "founders":
				this.handleFounders(subject, companyData, entityTriples);
				break;
			case "ceos":
				this.handleCeos(subject, companyData, entityTriples);
				break;
			case "sales":
				this.handleSales(subject, companyData, entityTriples);
				break;
			case "acquisitions":
				this.handleAcquisitions(subject, companyData, entityTriples);
				break;
			case "spinoffs":
				this.handleSpinoffs(subject, companyData, entityTriples);
				break;
			case "mergers":
				this.handleMergers(subject, companyData, entityTriples);
				break;
			case "foundation":
				this.handleFoundation(subject, companyData, entityTriples);
				break;
			default:
				if (false == this.unhandledEntries.contains(entryName)) {
					this.unhandledEntries.add(entryName);
					System.out.println("Did not find: " + entryName);
				}
				break;
			}
		}
		
		if (false == entityTriples.isEmpty()) {
			this.tripleIterator = entityTriples.iterator();
			return true;
		} else {
			// if this entity did not work --> try next one
			return this.hasNext();
		}
	}

	@Override
	public Triple next() throws IngestionException {
		return this.tripleIterator.next();
	}
	
	protected Node createGcdUri(final String companyName) {
		String cleanName = companyName.trim().toLowerCase().replaceAll("\\W+", "_");
		Node subject = NodeFactory.createURI(CorpDbpedia.prefix + "resouce/gcd2_" + cleanName);
		return subject;
	}
	
	public Node handleNames(final JsonObject entity, final String jsonElementName, final List<Triple> triples) {
		
		JsonElement namesJson = entity.get(jsonElementName);
		if (null == namesJson) {
			return null;
		}
		
		String companyName = namesJson.isJsonArray() ? namesJson.getAsJsonArray().get(0).getAsString() : namesJson.getAsString();
						
		Node subject = this.createGcdUri(companyName);
		Node predicateName = NodeFactory.createURI(SKOS.altLabel);
		
		if (namesJson.isJsonArray()) {
			for (JsonElement nameJson : namesJson.getAsJsonArray()) {
				// get company name and create URI
				Node nameNode = NodeFactory.createLiteral(nameJson.getAsString(), "de");
				
				// add triple
				triples.add(new Triple(subject, predicateName, nameNode));
			}
		} else {
			Node nameNode = NodeFactory.createLiteral(namesJson.getAsString(), "de");
			
			// add triple
			triples.add(new Triple(subject, predicateName, nameNode));
		}
		
		return subject;
	}
	
	public void handleCompanyTypes(final Node subject, final JsonObject entity, final List<Triple> triples) {
		Node dbpediaType = NodeFactory.createURI("http://dbpedia.org/ontology/Company");
		Node orgType = NodeFactory.createURI("http://dbpedia.org/ontology/Company");
		triples.add(new Triple(subject, RDF.type.asNode(), dbpediaType));
		triples.add(new Triple(subject, RDF.type.asNode(), orgType));
	}
	
	public void handlePersonTypes(final Node subject, final JsonObject entity, final List<Triple> triples) {
		triples.add(new Triple(subject, RDF.type.asNode(), FOAF.Agent.asNode()));
		triples.add(new Triple(subject, RDF.type.asNode(), FOAF.Person.asNode()));
	}
	
	public void handleSameAs(final Node subject, final JsonObject entity, final List<Triple> triples) {
		JsonElement idFirma = entity.get("idFirma");
		if (null == idFirma) {
			return;
		}
		
		Node idUri = NodeFactory.createURI(idFirma.getAsString());
		triples.add(new Triple(subject, OWL.sameAs.asNode(), idUri));
		
		return;
	}
	
	public void handleWebsite(final Node subject, final JsonObject entity, final List<Triple> triples) {
		JsonElement webLink = entity.get("webLink");
		if (null != webLink) {
			String webLinkString = webLink.getAsString();
			if (null != webLinkString && false == webLinkString.isEmpty()) {
				Node webLinkNode = NodeFactory.createURI(webLinkString);
				triples.add(new Triple(subject, FOAF.homepage.asNode(), webLinkNode));
			}
		}
		
		JsonElement twitter = entity.get("twitter");
		if (null != twitter) {

			for (JsonElement twitterAddress : twitter.getAsJsonArray()) {
				String twitterAddressString = twitterAddress.getAsString();
				if (null != twitterAddressString && false == twitterAddressString.isEmpty()) {
					Node twitterNode = NodeFactory.createURI(twitterAddressString);
					triples.add(new Triple(subject, NodeFactory.createURI(CorpDbpedia.twitterChannel), twitterNode));
				}
			}
		}
		
		JsonElement wikiLink = entity.get("wikiLink");
		if (null != wikiLink) {
			String wikiLinkString = wikiLink.getAsString();
			if (null != wikiLinkString && false == wikiLinkString.isEmpty()) {	
				Node wikiLinkNode = NodeFactory.createURI(wikiLinkString);
				triples.add(new Triple(subject, FOAF.isPrimaryTopicOf.asNode(), wikiLinkNode));
			}
		}
		
		return;
	}
	
	public void handleEmployeeCount(final Node subject, final JsonObject entity, final List<Triple> triples) {
		JsonElement employees = entity.get("employees");
		if (null == employees) {
			return;
		}
		
		if (1 < employees.getAsJsonArray().size()) {
			System.out.println("Employee Count: " + subject);
		}
		
		for (JsonElement revenue : employees.getAsJsonArray()) {
			JsonElement countObject = ((JsonObject) revenue).get("value");
			JsonElement yearElement = ((JsonObject) revenue).get("year");
		
			if (null == countObject) {
				continue;
			}
			
			String employeeCount = countObject.getAsString();
			
			if (null != yearElement) {
				String yearString = yearElement.getAsString();
				employeeCount += " (" + yearString + ")";
			}
			
			triples.add(new Triple(subject, NodeFactory.createURI("http://dbpedia.org/ontology/numberOfEmployees"),
					NodeFactory.createLiteral(employeeCount, XSDDatatype.XSDstring)));
		}

	}
	
	public void handleRevenues(final Node subject, final JsonObject entity, final List<Triple> triples) {
		JsonElement revenues = entity.get("revenues");
		if (null == revenues) {
			return;
		}
		
		if (1 < revenues.getAsJsonArray().size()) {
			System.out.println("Revenue Count: " + subject);
		}
		
		for (JsonElement revenue : revenues.getAsJsonArray()) {
			JsonElement revenueObject = ((JsonObject) revenue).get("value");
			JsonElement yearElement = ((JsonObject) revenue).get("year");
		
			if (null == revenueObject) {
				continue;
			}
			
			String revenueString = revenueObject.getAsString();
			
			if (null != yearElement) {
				String yearString = yearElement.getAsString();
				revenueString += " (" + yearString + ")";
			}
			
			triples.add(new Triple(subject, NodeFactory.createURI("http://dbpedia.org/ontology/revenue"),
					NodeFactory.createLiteral(revenueString, XSDDatatype.XSDstring)));
		}

	}
	
	public void handleAbstract(final Node subject, final JsonObject entity, final List<Triple> triples) {
		JsonElement wikiAbtract = entity.get("wikiAbstract");
		if (null == wikiAbtract) {
			return;
		}
		
		String abstractString = wikiAbtract.getAsString();
		if (null == abstractString || abstractString.isEmpty()) {
			return;
		}
		
		
		triples.add(new Triple(subject, NodeFactory.createURI("http://dbpedia.org/ontology/abstract"),
				NodeFactory.createLiteral(abstractString.replaceAll("\"", "\\\""), "de")));
	}
	
	public void handleLogo(final Node subject, final JsonObject entity, final List<Triple> triples) {
		JsonElement logo = entity.get("logo");
		if (null == logo) {
			return;
		}
		
		String logoString = logo.getAsString();
		if (null == logoString || logoString.isEmpty()) {
			return;
		}
		
		triples.add(new Triple(subject, FOAF.depiction.asNode(),
				NodeFactory.createURI(logoString)));
	}
	
	public void handleIndustries(final Node subject, final JsonObject entity, final List<Triple> triples) {
		JsonElement industries = entity.get("industries");
		if (null == industries) {
			return;
		}
		
		for (JsonElement industry : industries.getAsJsonArray()) {
			JsonElement industryElement = industry.getAsJsonObject().get("name");
			if (null == industryElement) {
				continue;
			}
			
			String industryNameString = industryElement.getAsString();
			if (null == industryNameString || industryNameString.isEmpty()) {
				continue;
			}
			
			String[] industryNames = industryNameString.split(",");
			for (String industryName : industryNames) {
			
				Node categoryPredicate = NodeFactory.createURI(CorpDbpedia.orgCategory);
				triples.add(new Triple(subject, categoryPredicate,
						NodeFactory.createLiteral(industryName.trim(), "de")));
			}
		}
	}
	
	public void handleTechnologies(final Node subject, final JsonObject entity, final List<Triple> triples) {
		JsonElement technologies = entity.get("technologies");
		if (null == technologies) {
			return;
		}
		
		for (JsonElement technology : technologies.getAsJsonArray()) {
			JsonElement technologyElement = technology.getAsJsonObject().get("name");
			if (null == technologyElement) {
				continue;
			}
			
			String technologyNameString = technologyElement.getAsString();
			if (null == technologyNameString || technologyNameString.isEmpty()) {
				continue;
			}
			
			String[] technologyNames = technologyNameString.split(",");
			for (String technologyName : technologyNames) {
			
				Node categoryPredicate = NodeFactory.createURI(CorpDbpedia.providesTechnology);
				triples.add(new Triple(subject, categoryPredicate,
						NodeFactory.createLiteral(technologyName, "de")));
			}
		}
	}
	
	public void handleCustomerServicePhoneNr(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		JsonElement customerServicePhoneNr = entity.get("customerServicePhoneNr");
		if (null == customerServicePhoneNr) {
			return;
		}
		
		String customerServicePhoneNrString = customerServicePhoneNr.getAsString();
		if (null == customerServicePhoneNrString || customerServicePhoneNrString.isEmpty()) {
			return;
		}
		
		Node phonePredicate = NodeFactory.createURI(CorpDbpedia.customerServicePhoneNr);
		Node phoneLiteral = NodeFactory.createLiteral(customerServicePhoneNrString, XSDDatatype.XSDstring);
		triples.add(new Triple(subject, phonePredicate, phoneLiteral));
		
		
	}
	
	public void handleStockQuote(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		JsonElement stockQuoteElement = entity.get("stockQuote");
		if (null == stockQuoteElement) {
			return;
		}
		
		String stockQuoteString = stockQuoteElement.getAsString();
		if (null == stockQuoteString || stockQuoteString.isEmpty()) {
			return;
		}
		
		Node stockQuotePredicate = NodeFactory.createURI(CorpDbpedia.stockQuote);
		Node stockQuoteLiteral = NodeFactory.createLiteral(stockQuoteString, XSDDatatype.XSDstring);
		triples.add(new Triple(subject, stockQuotePredicate, stockQuoteLiteral));
	}
	
	public void handleProducts(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		JsonElement productElements = entity.get("products");
		if (null == productElements) {
			return;
		}
		
		Node productTypePredicate = NodeFactory.createURI(CorpDbpedia.productCategory);
		for (JsonElement productElement : productElements.getAsJsonArray()) {
			String productString = productElement.getAsJsonObject().get("name").getAsString();			
			Node productTypeLiteral = NodeFactory.createLiteral(productString, "de");
			
			triples.add(new Triple(subject, productTypePredicate, productTypeLiteral));
		}
	}
	
	public void handleFoundation(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		JsonElement foundationElement = entity.get("foundation");
		if (null == foundationElement) {
			return;
		}
		
		JsonElement dateElement = foundationElement.getAsJsonObject().get("date");
		JsonElement location = foundationElement.getAsJsonObject().get("location");

		// create URI for the event itself
		String foundationEventUri = subject.getURI() + "_Foundation";
		Node foundationNode = NodeFactory.createURI(foundationEventUri);
		
		triples.add(new Triple(subject, NodeFactory.createURI(W3COrg.changedBy), foundationNode));
		triples.add(new Triple(subject, NodeFactory.createURI(CorpDbpedia.changedByCompanyFoundation), foundationNode));

		boolean hasData = false;
		if (null != location) {
			
			String eventSiteUri = foundationEventUri + "EventSite";
			
			hasData = true;
			
			// mark foundation event site
			Node hasEventSitePredicate = NodeFactory.createURI(CorpDbpedia.hasEventSite);
			Node eventSiteNode = NodeFactory.createURI(eventSiteUri + "0");
			triples.add(new Triple(foundationNode, hasEventSitePredicate, eventSiteNode));			
			Node eventType = NodeFactory.createURI(CorpDbpedia.eventSite);
			triples.add(new Triple(eventSiteNode, RDF.type.asNode(), eventType));
			
			this.addLocationCoordinates(null, eventSiteUri, entity, location.getAsJsonObject(), 0, triples, false);
		}
		
		if (null != dateElement) {
			hasData = true;
			
			String dateString = dateElement.getAsString();
			String convertedDate = this.convertDateString(dateString);
			
//			System.out.println("Date: " + convertedDate + "/" + dateString);
				
			Node predicateStartTime = NodeFactory.createURI(W3CProvenance.startedAtTime);
			Node dateLiteral = NodeFactory.createLiteral(((null == convertedDate) ? dateString : convertedDate), XSDDatatype.XSDdate);
			triples.add(new Triple(foundationNode, predicateStartTime, dateLiteral));
		}
		
		if (hasData) {	
			Node changeEvent = NodeFactory.createURI(W3COrg.ChangeEvent);
			triples.add(new Triple(foundationNode, RDF.type.asNode(), changeEvent));
			
			Node foundationType = NodeFactory.createURI(CorpDbpedia.CompanyFoundation);
			triples.add(new Triple(foundationNode, RDF.type.asNode(), foundationType));
		}

	}
	
	protected String convertDateString(final String orignialDateString) {
		if (null == orignialDateString) {
			return null;
		}
		
		String convertedDate = null;
		
		try {
			DatatypeFactory df = DatatypeFactory.newInstance();
			XMLGregorianCalendar dateTime = df.newXMLGregorianCalendar(orignialDateString);
			
			convertedDate = dateTime.toXMLFormat();				
		} catch (Exception e) {
		   // ignore
		}
		
		if (null == convertedDate) {
			try {
				 String datePattern = "dd.MM.yyyy";
				 SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
				 dateFormat.setTimeZone(TimeZone.getTimeZone("CET"));
				 
				 Date convertedDateTmp = dateFormat.parse(orignialDateString);
				 					 
				 LocalDate convertedDateTmp2 = convertedDateTmp.toInstant().atZone(ZoneId.of("CET")).toLocalDate();
				 
				 DatatypeFactory df = DatatypeFactory.newInstance();
				 XMLGregorianCalendar dateTime = df.newXMLGregorianCalendarDate(convertedDateTmp2.getYear(),
						 			convertedDateTmp2.getMonth().getValue(), convertedDateTmp2.getDayOfMonth(), DatatypeConstants.FIELD_UNDEFINED);
				 dateTime.setTimezone(0);
				 
				 convertedDate = dateTime.toXMLFormat();				

			} catch (Exception e) {
				// ignore
			}
		} 
		
		if (null == convertedDate) {
			try {
				 String datePattern = "MM.yyyy";
				 SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
				 dateFormat.setTimeZone(TimeZone.getTimeZone("CET"));
				 
				 Date convertedDateTmp = dateFormat.parse(orignialDateString);
				 					 
				 LocalDate convertedDateTmp2 = convertedDateTmp.toInstant().atZone(ZoneId.of("CET")).toLocalDate();
				 
				 DatatypeFactory df = DatatypeFactory.newInstance();
				 XMLGregorianCalendar dateTime = df.newXMLGregorianCalendarDate(convertedDateTmp2.getYear(),
						 			convertedDateTmp2.getMonth().getValue(),  DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED);
				 dateTime.setTimezone(0);
				 
				 convertedDate = dateTime.toXMLFormat();				

			} catch (Exception e) {
				// ignore
			}
		}			
		
		return convertedDate;
	}
	
	protected void addEvent() {
		
	}
	
	public void handleFounders(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		JsonElement founders = entity.get("founders");
		if (null == founders) {
			return;
		}
		
		Node hasFounder = NodeFactory.createURI(CorpDbpedia.hasFounder);
		
		for (JsonElement founder : founders.getAsJsonArray()) {
			Node founderUri = this.handleNames(founder.getAsJsonObject(), "name", triples);
			if (null == founderUri) {
				continue;
			}
			
			this.handlePersonTypes(founderUri, entity, triples);
			
			this.addSameAs(founder.getAsJsonObject().get("uri"), founderUri, triples);
			
			triples.add(new Triple(subject, hasFounder, founderUri));
		}
	}
	
	public void addSameAs(final JsonElement uriElement, final Node subjectNode, final List<Triple> triples) {		
		if (null != uriElement && false == uriElement.isJsonNull()) {
			String uriString = uriElement.getAsString();
			if (false == uriString.isEmpty()) {
				Node sameAsUri = NodeFactory.createURI(uriElement.getAsString());
				triples.add(new Triple(subjectNode, OWL.sameAs.asNode(), sameAsUri));
			}
		}
	}
	
	public void handleCeos(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		JsonElement ceos = entity.get("ceos");
		if (null == ceos) {
			return;
		}
		
		Node headOf = NodeFactory.createURI(W3COrg.headOf);
		
		for (JsonElement ceo : ceos.getAsJsonArray()) {
			Node ceoUri = this.handleNames(ceo.getAsJsonObject(), "name", triples);
			if (null == ceoUri) {
				continue;
			}
			
			this.handlePersonTypes(ceoUri, entity, triples);
			
			this.addSameAs(ceo.getAsJsonObject().get("uri"), ceoUri, triples);

			
			triples.add(new Triple(subject, headOf, ceoUri));
		}
	}
	
	public void handleMergers(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		JsonElement mergers = entity.get("mergers");
		if (null == mergers) {
			return;
		}
				
		Node changedBy = NodeFactory.createURI(W3COrg.changedBy);
		int count = 0;
		for (JsonElement merger : mergers.getAsJsonArray()) {
			String mergerUriString = subject.getURI() + "_Merger" + count++;
			Node mergerNode = NodeFactory.createURI(mergerUriString);
			
			triples.add(new Triple(subject, changedBy, mergerNode));
			triples.add(new Triple(subject, NodeFactory.createURI(CorpDbpedia.changedByCompanyMerger), mergerNode));
			
			Node companyUri = this.handleNames(merger.getAsJsonObject(), "name", triples);
			if (null == companyUri) {
				continue;
			}
			
			this.addSameAs(merger.getAsJsonObject().get("uri"), mergerNode, triples);

			
			this.handleCompanyTypes(companyUri, entity, triples);
			
			triples.add(new Triple(mergerNode, NodeFactory.createURI(W3CProvenance.used), companyUri));

			
			Node changeEvent = NodeFactory.createURI(W3COrg.ChangeEvent);
			triples.add(new Triple(mergerNode, RDF.type.asNode(), changeEvent));
			
			Node companyspinoffType = NodeFactory.createURI(CorpDbpedia.CompanyMerger);
			triples.add(new Triple(mergerNode, RDF.type.asNode(), companyspinoffType));
			
			JsonElement dateElement = merger.getAsJsonObject().get("date");			
			if (null != dateElement) {				
				String dateString = dateElement.getAsString();
				String convertedDate = this.convertDateString(dateString);
									
				Node predicateStartTime = NodeFactory.createURI(W3CProvenance.startedAtTime);
				Node dateLiteral = NodeFactory.createLiteral(((null == convertedDate) ? dateString : convertedDate), XSDDatatype.XSDdate);
				triples.add(new Triple(mergerNode, predicateStartTime, dateLiteral));
			}
		}
	}
	
	public void handleSpinoffs(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		JsonElement spinoffs = entity.get("spinoffs");
		if (null == spinoffs) {
			return;
		}
				
		Node changedBy = NodeFactory.createURI(W3COrg.changedBy);
		int count = 0;
		for (JsonElement spinoff : spinoffs.getAsJsonArray()) {
			String spinoffUriString = subject.getURI() + "_Spinoff" + count++;
			Node spinoffNode = NodeFactory.createURI(spinoffUriString);
			
			triples.add(new Triple(subject, changedBy, spinoffNode));
			triples.add(new Triple(subject, NodeFactory.createURI(CorpDbpedia.changedByCompanySpinoff), spinoffNode));
			
			Node spinoffCompanyUri = this.handleNames(spinoff.getAsJsonObject(), "name", triples);
			if (null == spinoffCompanyUri) {
				continue;
			}
			
			this.handleCompanyTypes(spinoffNode, entity, triples);
			
			this.addSameAs(spinoff.getAsJsonObject().get("uri"), spinoffCompanyUri, triples);

			
			triples.add(new Triple(spinoffNode, NodeFactory.createURI(W3CProvenance.used), spinoffCompanyUri));
			
			this.handleCompanyTypes(spinoffCompanyUri, entity, triples);
			
			Node changeEvent = NodeFactory.createURI(W3COrg.ChangeEvent);
			triples.add(new Triple(spinoffNode, RDF.type.asNode(), changeEvent));
			
			Node companyspinoffType = NodeFactory.createURI(CorpDbpedia.CompanySpinoff);
			triples.add(new Triple(spinoffNode, RDF.type.asNode(), companyspinoffType));
			
			JsonElement dateElement = spinoff.getAsJsonObject().get("date");			
			if (null != dateElement) {				
				String dateString = dateElement.getAsString();
				String convertedDate = this.convertDateString(dateString);
									
				Node predicateStartTime = NodeFactory.createURI(W3CProvenance.startedAtTime);
				Node dateLiteral = NodeFactory.createLiteral(((null == convertedDate) ? dateString : convertedDate), XSDDatatype.XSDdate);
				triples.add(new Triple(spinoffNode, predicateStartTime, dateLiteral));
			}
		}
	}
	
	public void handleAcquisitions(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		JsonElement acquisitions = entity.get("acquisitions");
		if (null == acquisitions) {
			return;
		}
				
		Node changedBy = NodeFactory.createURI(W3COrg.changedBy);
		int count = 0;
		for (JsonElement acquisition : acquisitions.getAsJsonArray()) {
			String acquisitionsUriString = subject.getURI() + "_Acquisitions" + count++;
			Node acquisitionsNode = NodeFactory.createURI(acquisitionsUriString);
			
			triples.add(new Triple(subject, changedBy, acquisitionsNode));
			triples.add(new Triple(subject, NodeFactory.createURI(CorpDbpedia.changedByCompanyAcquisition), acquisitionsNode));

			
			Node acquisitionCompanyUri = this.handleNames(acquisition.getAsJsonObject(), "name", triples);
			if (null == acquisitionCompanyUri) {
				continue;
			}
			

			this.addSameAs(acquisition.getAsJsonObject().get("uri"), acquisitionCompanyUri, triples);

			
			this.handleCompanyTypes(acquisitionCompanyUri, entity, triples);
			
			triples.add(new Triple(acquisitionsNode, NodeFactory.createURI(W3CProvenance.used), acquisitionCompanyUri));
			
			Node changeEvent = NodeFactory.createURI(W3COrg.ChangeEvent);
			triples.add(new Triple(acquisitionsNode, RDF.type.asNode(), changeEvent));
			
			Node companyAcquisitionType = NodeFactory.createURI(CorpDbpedia.CompanyAcquisition);
			triples.add(new Triple(acquisitionsNode, RDF.type.asNode(), companyAcquisitionType));
			
			JsonElement dateElement = acquisition.getAsJsonObject().get("date");			
			if (null != dateElement) {				
				String dateString = dateElement.getAsString();
				String convertedDate = this.convertDateString(dateString);
									
				Node predicateStartTime = NodeFactory.createURI(W3CProvenance.startedAtTime);
				Node dateLiteral = NodeFactory.createLiteral(((null == convertedDate) ? dateString : convertedDate), XSDDatatype.XSDdate);
				triples.add(new Triple(acquisitionsNode, predicateStartTime, dateLiteral));
			}
		}
	}
	
	public void handleSales(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		JsonElement sales = entity.get("sales");
		if (null == sales) {
			return;
		}
			
		Node changedBy = NodeFactory.createURI(W3COrg.changedBy);
		int count = 0;
		for (JsonElement sale : sales.getAsJsonArray()) {
			String salesUriString = subject.getURI() + "_Sale" + count++;
			Node salesNode = NodeFactory.createURI(salesUriString);
			
			triples.add(new Triple(subject, changedBy, salesNode));
			triples.add(new Triple(subject, NodeFactory.createURI(CorpDbpedia.changedByCompanySale), salesNode));
			
			Node soldCompanyUri = this.handleNames(sale.getAsJsonObject(), "name", triples);
			if (null == soldCompanyUri) {
				continue;
			}
			
			this.addSameAs(sale.getAsJsonObject().get("uri"), soldCompanyUri, triples);
			
			triples.add(new Triple(salesNode, NodeFactory.createURI(W3CProvenance.used), soldCompanyUri));
			
			this.handleCompanyTypes(soldCompanyUri, entity, triples);
			
			Node changeEvent = NodeFactory.createURI(W3COrg.ChangeEvent);
			triples.add(new Triple(salesNode, RDF.type.asNode(), changeEvent));
			
			Node companySalesType = NodeFactory.createURI(CorpDbpedia.CompanySales);
			triples.add(new Triple(salesNode, RDF.type.asNode(), companySalesType));
			
			JsonElement dateElement = sale.getAsJsonObject().get("date");			
			if (null != dateElement) {				
				String dateString = dateElement.getAsString();
				String convertedDate = this.convertDateString(dateString);
									
				Node predicateStartTime = NodeFactory.createURI(W3CProvenance.startedAtTime);
				Node dateLiteral = NodeFactory.createLiteral(((null == convertedDate) ? dateString : convertedDate), XSDDatatype.XSDdate);
				triples.add(new Triple(salesNode, predicateStartTime, dateLiteral));
			}
			
		}
	}
	
	public void handleFacilities(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		
		JsonElement facilities = entity.get("facilities");
		if (null == facilities) {
			return;
		}
		
		String headQuarterUriString = subject.getURI() + "_site";
		this.addCoordinates(subject, headQuarterUriString, entity, facilities.getAsJsonArray(), triples, false);
	}
	
	public void handleCustomers(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		
		JsonElement customers = entity.get("customers");
		if (null == customers) {
			return;
		}
		
		Node hasReferenceCustomer = NodeFactory.createURI(CorpDbpedia.hasReferenceCustomer);
		
		for (JsonElement customer : customers.getAsJsonArray()) {			
			Node customerUri = this.handleNames(customer.getAsJsonObject(), "name", triples);
			if (null == customerUri) {
				continue;
			}
			
			this.handleCompanyTypes(customerUri, entity, triples);
			
			this.addSameAs(customer.getAsJsonObject().get("uri"), customerUri, triples);
			
			triples.add(new Triple(subject, hasReferenceCustomer, customerUri));
		}
	}
	
	public void handleChildren(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		
		JsonElement children = entity.get("children");
		if (null == children) {
			return;
		}
		
		Node hasSubOrganization = NodeFactory.createURI(W3COrg.hasSubOrganization);
		Node hasUnit = NodeFactory.createURI(W3COrg.hasUnit);
		
		Node unitOf = NodeFactory.createURI(W3COrg.unitOf);
		Node subOrganizationOf = NodeFactory.createURI(W3COrg.subOrganizationOf);

		for (JsonElement child : children.getAsJsonArray()) {			
			Node childUri = this.handleNames(child.getAsJsonObject(), "name", triples);
			if (null == childUri) {
				continue;
			}
			
			this.handleCompanyTypes(childUri, entity, triples);

			this.addSameAs(child.getAsJsonObject().get("uri"), childUri, triples);

			
			triples.add(new Triple(subject, hasUnit, childUri));
			triples.add(new Triple(subject, hasSubOrganization, childUri));
			
			triples.add(new Triple(childUri, unitOf, subject));
			triples.add(new Triple(childUri, subOrganizationOf, subject));
		}
	}
	
	public void handleParent(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		
		JsonElement parent = entity.get("parent");
		if (null == parent) {
			return;
		}
		
		Node hasSubOrganization = NodeFactory.createURI(W3COrg.hasSubOrganization);
		Node hasUnit = NodeFactory.createURI(W3COrg.hasUnit);
		
		Node unitOf = NodeFactory.createURI(W3COrg.unitOf);
		Node subOrganizationOf = NodeFactory.createURI(W3COrg.subOrganizationOf);
					
		Node parentUri = this.handleNames(parent.getAsJsonObject(), "name", triples);
		if (null == parentUri) {
			return;
		}
		
		this.handleCompanyTypes(parentUri, entity, triples);
		
		this.addSameAs(parent.getAsJsonObject().get("uri"), parentUri, triples);
		
		triples.add(new Triple(parentUri, hasUnit, subject));
		triples.add(new Triple(parentUri, hasSubOrganization, subject));
		
		triples.add(new Triple(subject, unitOf, parentUri));
		triples.add(new Triple(subject, subOrganizationOf, parentUri));

	}
	
	public void handleHeadquarters(final Node subject, final JsonObject entity, final List<Triple> triples) throws IngestionException {
		
		JsonElement headquarters = entity.get("headquarters");
		if (null == headquarters) {
			return;
		}
		
		String headQuarterUriString = subject.getURI() + "_headQuarter";
		this.addCoordinates(subject, headQuarterUriString, entity, headquarters.getAsJsonArray(), triples, true);
	}
	
	protected void addCoordinates(final Node subject, final String headQuarterUriString, final JsonObject entity, final JsonArray sites, final List<Triple> triples, boolean isHeaderQuarter) throws IngestionException {
		
		int count = -1;
		for (JsonElement site : sites) {
			this.addLocationCoordinates(subject, headQuarterUriString, entity, site.getAsJsonObject(), ++count, triples, isHeaderQuarter);
		}
	}
	
	protected void addLocationCoordinates(final Node subject, final String headQuarterUriString, final JsonObject entity, final JsonObject site, final int count, final List<Triple> triples, boolean isHeaderQuarter) throws IngestionException {
		JsonElement uriElement = ((JsonObject) site).get("uri");
		if (null != uriElement) {
			
			// try to add uri
			String uriString = uriElement.getAsString();
			if (null != uriString && false == uriString.isEmpty()
				&& this.addCoordinates(subject, headQuarterUriString, count, uriString, entity, triples, isHeaderQuarter)) {
				return;
			}
		}
		
		JsonElement nameElement = ((JsonObject) site).get("name");
		if (null != nameElement) {
			
			String nameString = nameElement.getAsString();
			// try German cities first and then many other variations
			String geonamesId = this.geonamesMaper.getGeoNamesCityId(nameString, "de", "http://sws.geonames.org/2921044/");
			if (null == geonamesId) {
				geonamesId = this.geonamesMaper.getGeoNamesCityId(nameString, "de");
				if (null == geonamesId) {
					geonamesId = this.geonamesMaper.getGeoNamesCityId(nameString, "en");
					if (null == geonamesId) {
						geonamesId = this.geonamesMaper.getGeoNamesCountryId(nameString, "de");
						if (null == geonamesId) {
							geonamesId = this.geonamesMaper.getGeoNamesCountryId(nameString, "en");
						}
					}
				}
			}
			
			if (null == geonamesId) {
				return; // nothing worked
			}
			
			this.addCoordinates(subject, headQuarterUriString, count, geonamesId, entity, triples, isHeaderQuarter);
		}
	}
	
	protected boolean addCoordinates(final Node subject, final String headQuarterUriString, final int count, final String uriString, final JsonObject entity, final List<Triple> triples, boolean isHeaderQuarter) throws IngestionException {
		Node headerQuarterUri = NodeFactory.createURI(headQuarterUriString + count);
		
		boolean worked = false;
		if (uriString.contains("dbpedia.org/resource/")) {
			
			try {
				Thread.sleep(140);
			} catch (InterruptedException e) {
				// ignore
			}
			
			StringBuilder builder = new StringBuilder();
			
			builder.append("SELECT * WHERE {\n");
			builder.append("\t<").append(uriString).append("> <" + OWL.sameAs.getURI()).append("> ?sameAs . \n");
			builder.append("\t OPTIONAL{ <").append(uriString).append("> <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?long . }\n");
			builder.append("\t OPTIONAL{ <").append(uriString).append("> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat . }\n}");

			QueryExecution queryExec;
			if (uriString.contains("de.dbpedia")) {
				queryExec = this.sparqlHandlerDe.createQueryExecuter(builder.toString());
			} else {
				queryExec = this.sparqlHandlerEn.createQueryExecuter(builder.toString());
			}
			
			Coordinates coordinates = null;
			String geonamesId = null;
			ResultSet results = queryExec.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.next();
				RDFNode sameAsUri = result.get("sameAs");
				
				String sameAsString = sameAsUri.asNode().toString();
				
				if (sameAsString.contains("geonames")) {
					geonamesId = sameAsString;
					break;
				}
				
				// do this as backup
				RDFNode longNode = result.get("long");
				RDFNode latNode = result.get("lat");
				if (null != longNode && null != latNode) {
					String longitude = longNode.asNode().getLiteralLexicalForm();
					String latitude = latNode.asNode().getLiteralLexicalForm();
					
					coordinates = new Coordinates(latitude, longitude);
					// do not break here, since we prefer to get geonames ID
				}
			}
			
			if (null == coordinates && null != geonamesId) {
				
				geonamesId = ((geonamesId.endsWith("/")) ? geonamesId : geonamesId + "/");
				coordinates = this.geonamesMaper.getCoordinatesFromId(geonamesId);
			}
			
			if (null != coordinates) {
				String latitudeString = coordinates.latitude;
				String longitudeString = coordinates.longitude;
				
				this.addCoordinates(latitudeString, longitudeString, geonamesId, headerQuarterUri, subject, triples, isHeaderQuarter);			
				worked = true;
			}
			
			//System.out.println("DBpedia: " + uriString);
		} else if (uriString.startsWith("http://sws.geonames.org/")) {
			
			String queryUriString = ((uriString.endsWith("/")) ? uriString : uriString + "/");
			Coordinates coordinates = this.geonamesMaper.getCoordinatesFromId(queryUriString);
			
			String latitudeString = coordinates.latitude;
			String longitudeString = coordinates.longitude;
			
			this.addCoordinates(latitudeString, longitudeString, queryUriString, headerQuarterUri, subject, triples, isHeaderQuarter);			
			worked = true;

			
		} else {
			System.out.println("Do not know: " + uriString);
		}
		
		return worked;
	}
	
	public void addCoordinates(final String latitudeString, final String longitudeString, final String geonamesId, final Node headerQuarterUri, final Node subject, final List<Triple> triples, boolean isHeaderQuarter) throws IngestionException {
		
		if (null != latitudeString && false == latitudeString.isEmpty()
				&& null != longitudeString && false == longitudeString.isEmpty()) {
				String pointString = "POINT(" + longitudeString + " " + latitudeString + ")";
				Node point = NodeFactory.createLiteral(pointString, "http://www.openlinksw.com/schemas/virtrdf#Geometry");
				Node pointPredicate = NodeFactory.createURI("http://www.opengis.net/ont/geosparql#asWKT");
				
				triples.add(new Triple(headerQuarterUri, pointPredicate,  point));
				triples.add(new Triple(headerQuarterUri, RDF.type.asNode(),
							NodeFactory.createURI("http://geovocab.org/geometry#Geometry")));
				
				triples.add(new Triple(headerQuarterUri, RDF.type.asNode(),  NodeFactory.createURI(W3COrg.site)));
				
				if (null != geonamesId) {
					// if we get language codes --> it must be a country!
					List<String> languageCodes = geonamesMaper.getLanguageCodes(geonamesId);				
					if (null == languageCodes || languageCodes.isEmpty()) {
						String nameEn = this.geonamesMaper.getNameFromId(geonamesId, "en");
						if (null != nameEn) {
							Node predicateCity = NodeFactory.createURI(CorpDbpedia.cityName);
							Node city = NodeFactory.createLiteral(nameEn, "en");
							triples.add(new Triple(headerQuarterUri, predicateCity, city));
						}
						
						String nameDe = this.geonamesMaper.getNameFromId(geonamesId, "de");
						if (null != nameDe) {
							Node predicateCity = NodeFactory.createURI(CorpDbpedia.cityName);
							Node city = NodeFactory.createLiteral(nameDe, "de");
							triples.add(new Triple(headerQuarterUri, predicateCity, city));
						}
						
						Node predicateCityId = NodeFactory.createURI(CorpDbpedia.cityGeonamesId);
						Node cityId = NodeFactory.createURI(geonamesId);
						triples.add(new Triple(headerQuarterUri, predicateCityId, cityId));
					}
				}				
				
				if (null != subject) {
					triples.add(new Triple(subject, NodeFactory.createURI(W3COrg.hasSite), headerQuarterUri));
					
					if (isHeaderQuarter)
						triples.add(new Triple(subject, NodeFactory.createURI(CorpDbpedia.hasHeadquarterSite), headerQuarterUri));
				}
		}
	}
}
