package org.aksw.sdw.ingestion.csv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.importer.DatasetImporter;
import org.aksw.sdw.ingestion.csv.normalizer.*;
import org.aksw.sdw.ingestion.csv.normalizer.ActiveNormalizer.CompanyActivity;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;

import aksw.org.kg.entity.Entity;

import org.apache.jena.graph.Triple;

/**
 * This class can be used to create RDF out of an input CSV file.
 * 
 * It uses Claus Stadlers Sparqlify tool: https://github.com/AKSW/Sparqlify
 * 
 * @Todo: Create new base importer class
 *
 * @author mullekay
 *
 */
public class CsvSparqlify {
	
	final static Logger logger = Logger.getLogger(CsvSparqlify.class);
	
	/** uri prefix */
	final String uriPrefix;
	
	/** dataset importer */
	final DatasetImporter datasetImporter;
	
	/** clean-up pattern which can be used to find whitespaces */
	final static Pattern cleanupPattern = Pattern.compile("(\\s+)");
	
	/** clean-up pattern which can be used to find whitespaces at the end of a string */
	final static Pattern cleanupPatternObjectEnd = Pattern.compile("(\\s+\")");
	
	/** clean-up pattern which can be used to find whitespaces at the start of a string */
	final static Pattern cleanupPatternObjectStart = Pattern.compile("(\"\\s+)");
	
	/** checks for "" as an empty string */
	final static Pattern emptyObject = Pattern.compile("^\"\"$");
	
	final static Pattern findTypePattern = Pattern.compile("(?<=(\\^\\^))(.+)");
	
	/** can be used to store statistics */
	final ConversionStats stats = new ConversionStats();
	
	final List<PropertyNormalizer> normalizer = new ArrayList<>();
	
	final protected String outputPath;
	
	final protected String fileName;
	
	/** Here the output is stored */
	BufferedWriter outputFile;
	
	long entityCount = 0;
	
	long fileCount = 0;
	
	public static enum DatasetType {GRID, GRID_SOURCE, GCD_NT, GCD_CSV, DAXJSON, PERMID, DBPEDIA_ORG, DBPEDIA_PERSON};
	
	
	/**
	 * Constructor
	 * 
	 * @param csvFilePath	- path to csv file
	 * @param prefix		- prefix which is put before properties and resources
	 */
	public CsvSparqlify(final String uriPrefix,
						final DatasetImporter datasetImporter,
						final String outputPath, final String fileName,
						final DatasetType datasetType,
						final boolean filterUnknownProperties) throws IngestionException {
		
		this.datasetImporter = datasetImporter;
		
		this.uriPrefix = uriPrefix;
		
		this.outputPath = outputPath;
		this.fileName = fileName;
		
		UrlValidator validator = new UrlValidator();
		if (false == uriPrefix.startsWith("http") ||
			false == validator.isValid(uriPrefix)) {
			throw new IngestionException("The prefix is not a valid URL: " + uriPrefix);
		}
		
		switch (datasetType) {
		default:
			break;
		case GRID: {
		
			/// TODO : make this configurable (for each pipeline)
			this.normalizer.add(new EntityUriNormalizer(CorpDbpedia.prefixResource + "identifier_grid", ""));
			this.normalizer.add(new GridAddressNormalizer());
			this.normalizer.add(new GridIdentifierNormalizer());
			this.normalizer.add(new EventNormalizer());
			
			Map<ActiveNormalizer.CompanyActivity, List<String>> activityValueMap = new HashMap<>();
			activityValueMap.put(CompanyActivity.ACTIVE, Arrays.asList("active"));
			activityValueMap.put(CompanyActivity.INACTIVE, Arrays.asList("obsolete"));
			activityValueMap.put(CompanyActivity.REDIRECTED, Arrays.asList("redirected"));
			this.normalizer.add(new ActiveNormalizer("status", activityValueMap));
			this.normalizer.add(new GridRelationNormalizer());
			this.normalizer.add(new GridLabels());
			this.normalizer.add(new GridRedirects());
			this.normalizer.add(new TypeNormalizer());
//			this.normalizer.add(new HomepageNormalizer());
			
			this.normalizer.add(new PrintInvalidPredicates(filterUnknownProperties));
		} break;
		case PERMID: {
			this.normalizer.add(new EntityUriNormalizer(CorpDbpedia.prefixOntology + "identifier_permid", "permid_"));
			
			this.normalizer.add(new PermidSiteNormalizer("(headquarter|isDomiciledIn)", "_headquarterSite", CorpDbpedia.hasHeadquarterSite,
													   "http://ont.thomsonreuters.com/mdaas/HeadquartersAddress",
													   "http://permid.org/ontology/organization/hasHeadquartersPhoneNumber",
													   "http://permid.org/ontology/organization/hasHeadquartersFaxNumber",
													   "http://www.omg.org/spec/EDMC-FIBO/BE/LegalEntities/CorporateBodies/isDomiciledIn"));
//			this.normalizer.add(new HeadquarterSite());
			this.normalizer.add(new PermidSiteNormalizer("isIncorporatedIn", "_incorpSite", CorpDbpedia.hasIncorporatedSite,
					   null, null, null, "http://permid.org/ontology/organization/isIncorporatedIn"));
//			this.normalizer.add(new IncorpSiteNormalizer());
//			this.normalizer.add(new RegisteredSiteNormalizer());
			this.normalizer.add(new PermidSiteNormalizer("registered", "_registeredSite", CorpDbpedia.hasHeadquarterSite,
					   "http://ont.thomsonreuters.com/mdaas/RegisteredAddress",
					   "http://permid.org/ontology/organization/hasRegisteredPhoneNumber",
					   "http://permid.org/ontology/organization/hasRegisteredFaxNumber",
					   "http://www.omg.org/spec/EDMC-FIBO/BE/LegalEntities/CorporateBodies/isDomiciledIn"));
	
//			this.normalizer.add(new HomepageNormalizer());
			this.normalizer.add(new EventNormalizer());
			this.normalizer.add(new PermidIdentifierNormalizer());
			
			Map<ActiveNormalizer.CompanyActivity, List<String>> activityValueMap = new HashMap<>();
			activityValueMap.put(CompanyActivity.ACTIVE, Arrays.asList("http://permid.org/ontology/organization/statusActive"));
			activityValueMap.put(CompanyActivity.INACTIVE, Arrays.asList("http://permid.org/ontology/organization/statusInActive"));
			this.normalizer.add(new ActiveNormalizer("http://permid.org/ontology/organization/hasActivityStatus", activityValueMap));
			this.normalizer.add(new PermidIndustryTypeCleaner());
			this.normalizer.add(new TypeNormalizer());
			
			this.normalizer.add(new PrintInvalidPredicates(filterUnknownProperties));

		} break;
		case DAXJSON:
			this.normalizer.add(new AddressNormalizer());
			this.normalizer.add(new TypeNormalizer());
			this.normalizer.add(new HomepageNormalizer());
			this.normalizer.add(new PrintInvalidPredicates(filterUnknownProperties));
			break;
		case GCD_CSV:
			this.normalizer.add(new AddressNormalizer());
			this.normalizer.add(new TypeNormalizer());
			this.normalizer.add(new HomepageNormalizer());
			this.normalizer.add(new PrintInvalidPredicates(filterUnknownProperties));
			break;			
		case GCD_NT: {
			this.normalizer.add(new GcdAdjustSubjectUri());
			this.normalizer.add(new GcdSiteOfNormalizer());
			this.normalizer.add(new TypeNormalizer());
			this.normalizer.add(new GcdIdentifier());
//			this.normalizer.add(new HomepageNormalizer());
			
			this.normalizer.add(new PrintInvalidPredicates(filterUnknownProperties));
		} break;
		case DBPEDIA_ORG: {
			PropertyNormalizer dbpediaNameNormalizer = new PropertyNormalizer() {
				
				PropertyNormalizer nameNormalizer0 = 
						new SubjectNamespaceReplacer(
								"http://de.dbpedia.org/resource/",
								"http://corp.dbpedia.org/resource/dbpedia_de_");
				
				PropertyNormalizer nameNormalizer1 =
						new SubjectNamespaceReplacer(
								"http://dbpedia.org/resource/",
								"http://corp.dbpedia.org/resource/dbpedia_");
				
				@Override
				public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
					String originalUri = entity.getSubjectUri();
					
					this.nameNormalizer0.normalize(entity, stats);
					if (originalUri.equals(entity.getSubjectUri())) {
						this.nameNormalizer1.normalize(entity, stats);
					}
					
				}
			};
			this.normalizer.add(dbpediaNameNormalizer);
			this.normalizer.add(new LabelNormalizer());
			
			Map<ActiveNormalizer.CompanyActivity, List<String>> activityValueMap = new HashMap<>();
			activityValueMap.put(CompanyActivity.ACTIVE, Arrays.asList("Active", "online", "Public Access"));
			activityValueMap.put(CompanyActivity.INACTIVE, Arrays.asList("inactive", "Not active", "Fully closed",
																		  "Discontinued", "Dissolved", "Acquired",
																		  "closed", "Defunct", "Offline"));
			this.normalizer.add(new ActiveNormalizer("http://dbpedia.org/ontology/currentStatus", activityValueMap));
			
			this.normalizer.add(new GenericEventNormalizer("http://dbpedia.org/ontology/foundingDate", CorpDbpedia.CompanyFoundation, "_foundationEvent"));
			this.normalizer.add(new DbpediaExtinctionEventNormalizer("http://dbpedia.org/ontology/extinctionYear", CorpDbpedia.CompanyExtinction, "_extinctionEvent"));
			
			this.normalizer.add(new DBpediaHeadquarterNormaliser());
			this.normalizer.add(new DBpediaRelationshipNormalizer(dbpediaNameNormalizer));
			this.normalizer.add(new DBpediaTypeNormalizer());
			this.normalizer.add(new PrintInvalidPredicates(filterUnknownProperties));
		} break;
		case DBPEDIA_PERSON: {
			PropertyNormalizer dbpediaNameNormalizer = new PropertyNormalizer() {
				
				PropertyNormalizer nameNormalizer0 = 
						new SubjectNamespaceReplacer(
								"http://de.dbpedia.org/resource/",
								"http://corp.dbpedia.org/resource/dbpedia_de_");
				
				PropertyNormalizer nameNormalizer1 =
						new SubjectNamespaceReplacer(
								"http://dbpedia.org/resource/",
								"http://corp.dbpedia.org/resource/dbpedia_");
				
				@Override
				public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
					String originalUri = entity.getSubjectUri();
					
					this.nameNormalizer0.normalize(entity, stats);
					if (originalUri.equals(entity.getSubjectUri())) {
						this.nameNormalizer1.normalize(entity, stats);
					}
					
				}
			};
			this.normalizer.add(dbpediaNameNormalizer);			
		} break;
		}
	}
	
	public void run() throws Exception {
		String subjectUriString = null;
		
		if (null == this.datasetImporter) {
			logger.error("No Dataset Importer defined");
			return;
		}
				
		try {		
			StringBuilder builder = new StringBuilder();
			
			// this map stores all the predicates and object of an entity
			Entity entity = new Entity(uriPrefix);
			
			long startTime = System.currentTimeMillis();
			
			int countEmptyObjects = 0;
			Triple triple = null;
			while (null != (triple = this.getNextTriple(this.datasetImporter, entity, builder))) {				
				
				subjectUriString = entity.getSubjectUri();
				
				if (this.getTripleData(triple, entity)) {
					this.stats.incrementStats(CsvSparqlify.class, "tripleCount");
				} else {
					this.stats.incrementStats(CsvSparqlify.class, "emptyTriple");
				}
				
				long tripleCount = this.stats.getStatsEntry(getClass(), "tripleCount");
				if (0 == tripleCount % 1000) {
					long now = System.currentTimeMillis();
					long diff = now - startTime;
					startTime = now;
					
					logger.debug("Produced triples: " + tripleCount + ". 1000 triples in: " + diff + "ms." );
					System.out.println("Produced triples: " + tripleCount
										+ ". 1000 triples in: " + diff + "ms and entities: "
										+ this.stats.getStatsEntry(getClass(), "entityCount"));
				}				
			}			
			
			
			if (logger.isDebugEnabled()) {
				logger.debug("Counted empty objects: " + countEmptyObjects);
			}
			
			stats.addNumberToStats(CsvSparqlify.class, "emptyEntities", (long) countEmptyObjects);
			
		} catch (Throwable e) {
			System.err.println("Got error when looking at URI: " + subjectUriString);
			logger.error("Got error when looking at URI: " + subjectUriString);
			throw new Exception("Got error when looking at URI: " + subjectUriString, e);
		} finally {
			this.getOutputFile().close();
		}
	}
	
	/**
	 * This method can be used to process the entity and to store
	 * the results in an output file
	 * 
	 * @param entity
	 * @throws IngestionException
	 */
	protected void processEntity(final Entity entity) throws IngestionException {
		if (null == entity || entity.isEmpty()) {
			return;
		}
		
		// normalise the data
		for (PropertyNormalizer normalizer : this.normalizer) {
			normalizer.normalize(entity, this.stats);
		}
		
		try {
			this.fillOutputFile(entity);
		} catch (IOException e) {
			throw new IngestionException("Problems when writing the output file", e);
		}
	}
	
	protected Triple getNextTriple(final DatasetImporter datasetImporter,
								   final Entity entity,
								   final StringBuilder builder) throws IngestionException {
		
		boolean hasNext = datasetImporter.hasNext();
		if (false == hasNext) {				
			processEntity(entity);
			return null;
		}

		// get triple
		Triple triple = datasetImporter.next();
		
		// check whether it is a blank node
		boolean isBlankNode = triple.getSubject().isBlank();

		// get subjects
		String uri = triple.getSubject().toString();
		String subjectString = Entity.cleanUriString(uri, this.uriPrefix);
		String previousSubject = entity.getSubjectUri();
		
		String category = null;
		Entity subEntity = null;
		if (isBlankNode) {
			category = Entity.getBlankNodeCategory(subjectString);
			
			// ensure that we don't overlap information of two entities
			subEntity = entity.getSubEntityByCategory(category);
		}
		
		// in case the blankNode comes first
		if (isBlankNode && null == subEntity) {
			subEntity = new Entity(null, isBlankNode);
			subEntity.setSubjectUri(subjectString);
			entity.addSubEntity(subEntity);
		} else if (isBlankNode && null != subEntity) {
			// ignore this one
		} else if (null == previousSubject) {
			entity.setSubjectUri(subjectString);
		} else if (null != previousSubject && false == previousSubject.equals(subjectString) && subjectString.startsWith(previousSubject)) {
			if (null == entity.getSubEntityById(subjectString)) {
				// add, if we do not know the id subEntity yet
				subEntity = new Entity(null, false);
				subEntity.setSubjectUri(subjectString);
				entity.addSubEntity(subEntity);
			}
		} else if (null != previousSubject && false == previousSubject.equals(subjectString)) {			
			// make sure this entity is finishe up
			this.processEntity(entity);
			
			this.stats.addNumberToStats(CsvSparqlify.class, "tripleCount", (long) entity.getTripleCount());
			this.stats.incrementStats(CsvSparqlify.class, "entityCount");
			
			entity.reset();
			entity.setSubjectUri(subjectString);
		}
		
		return triple;
	}
	
	static final Pattern dataTypePattern = Pattern.compile("((\"@.{2,4}$)|(\\^\\^.*))");
	
	/**
	 * This method can be used to get all the data from the triple and to store
	 * it in the properties map
	 * 
	 * @param triple			- input triple
	 * @param entity			- entity which stores all properties and their values
	 * @return Entity URI
	 */
	public boolean getTripleData(final Triple triple, final Entity entity) {
		
		// only print out content which we actually have
		String objectString = triple.getObject().toString();
		if (objectString.startsWith("\"\"")) {
			return false;
		}
		
		objectString = cleanupPatternObjectStart.matcher(
				   				cleanupPatternObjectEnd.matcher(
				   						triple.getObject().toString()).
				   							replaceAll("\"")).replaceAll("\"");
		if (emptyObject.matcher(objectString).find()) {
			return false;
		}
		
		boolean isBlankNode = triple.getSubject().isBlank();
		
		String entityUriString = entity.getSubjectUri();
		String tripleSubjectUri = triple.getSubject().toString();
		boolean isSubEntity = (false == entityUriString.equals(tripleSubjectUri) && tripleSubjectUri.startsWith(entityUriString));
		
		final Entity storeEntity;
		if (isBlankNode) {
			// store in correct entity
			String subjectString = triple.getSubject().getBlankNodeLabel();
			
			String category = Entity.getBlankNodeCategory(subjectString);
			storeEntity = entity.getSubEntityByCategory(category);
		} else if (isSubEntity) {
			storeEntity = entity.getSubEntityById(tripleSubjectUri);
		} else {
			storeEntity = entity;
		}
		
		if (null == storeEntity) {
			throw new RuntimeException("Was not able to find: " + tripleSubjectUri + " in " + entityUriString);
		}
		
		storeEntity.addTriple(triple);
		return true;

	}
	
	protected BufferedWriter getOutputFile() throws IOException {
		if (null == this.outputFile || 0 == this.entityCount % 50000) {
			String newFileName = null;
			
			int dotIndex = this.fileName.lastIndexOf(".");
			dotIndex = (-1 == dotIndex) ? this.fileName.length() : dotIndex;
			
			String namePrefix = this.fileName.substring(0, dotIndex);
			String fileExtention = (-1 == dotIndex) ? "" : this.fileName.substring(dotIndex + 1);
			
			newFileName = this.outputPath + "/" + namePrefix + "_" + Long.toString(this.fileCount);
			if (-1 != dotIndex) {
				newFileName += "." + fileExtention;
			}
			
			File newOutputFile = new File(newFileName);
			FileWriter writer = new FileWriter(newOutputFile);
			BufferedWriter bufferedWriter = new BufferedWriter(writer);
			
			if (null != this.outputFile) {
				this.outputFile.close();
				this.outputFile = null;
			}
			
			// increment, just to make sure we do not create more files
			++this.entityCount;
			++this.fileCount;
			this.outputFile = bufferedWriter;			
		}
		
		return this.outputFile;
	}
	
	/**
	 * This method can be used to write all the triples which belong to an entity to the output file
	 * 
	 * @param entityProperties	- all entity properties with their values
	 * @param builder			- string builder instance
	 * @throws IOException when something goes wrong when writing data to the output file
	 */
	public void fillOutputFile(final Entity entity) throws IOException {
		if (false == entity.getSubjectUri().startsWith(CorpDbpedia.prefix)) {
			// ignore entities which are not ours
			return;
		}
		
		this.getOutputFile().append(entity.toString());
		++this.entityCount;
	}
	
	/**
	 * This method can be used to return the number of entities for address were wound
	 * @return
	 */
	public ConversionStats getStats() {
		return this.stats;
	}	
}
