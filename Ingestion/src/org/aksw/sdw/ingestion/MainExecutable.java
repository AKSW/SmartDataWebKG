package org.aksw.sdw.ingestion;


import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.aksw.sdw.ingestion.csv.CsvSparqlify;
import org.aksw.sdw.ingestion.csv.CsvSparqlify.DatasetType;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
//import org.aksw.sdw.ingestion.csv.importer.CsvDatasetImporter;
import org.aksw.sdw.ingestion.csv.importer.DatasetImporter;
import org.aksw.sdw.ingestion.csv.importer.DfkiCsvDatasetImporter;
import org.aksw.sdw.ingestion.csv.importer.GridJsonDatasetImporter2;
import org.aksw.sdw.ingestion.csv.importer.RdfNtDatasetImporter;
import org.aksw.sdw.ingestion.csv.importer.RdfNtDatasetImporter.RdfMapping;
import org.aksw.sdw.ingestion.csv.importer.SparqlDatasetImporter;
import org.aksw.sdw.ingestion.csv.importer.DfkiCsvDatasetImporter.ColumnAdapter;
import org.aksw.sdw.ingestion.csv.importer.DfkiDax30Json;
import org.aksw.sdw.ingestion.csv.normalizer.PropertyNormalizerUtils;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

import aksw.org.kg.input.CsvReader;

public class MainExecutable {
	
	public static void addMapping(final Map<String, RdfMapping> mapping, final String sourcePredicate,
			final String targetPredicate, final RDFDatatype dataType, final String lang) {
		addMapping(mapping, sourcePredicate, targetPredicate, dataType, lang, false);
	}

	public static void addMapping(final Map<String, RdfMapping> mapping, final String sourcePredicate,
			final String targetPredicate, final RDFDatatype dataType, final String lang, final boolean append) {
		RdfMapping locationMapping = new RdfMapping();

		locationMapping.targetMapping = targetPredicate;
		locationMapping.dataType = dataType;
		locationMapping.lang = lang;
		locationMapping.appendToTargetMappingPrefix = append;

		mapping.put(sourcePredicate, locationMapping);
	}

	public static void main(String[] args) {

		try {

			String path = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/GB/15_11_01/";
			String uriPrefix = CorpDbpedia.prefixResource;

			Map<String, RdfMapping> mapping = new HashMap<>();
			final CsvSparqlify.DatasetType datasetType = CsvSparqlify.DatasetType.PERMID;
			boolean filterUnknownProperties = true;
			
			PropertyNormalizerUtils.init(Arrays.asList("resources/org.owl","resources/CompanyTypes.owl"),
									//"/home/kay/Uni/Projects/SmartDataWeb/Code/git/SmartDataWebKG/Ingestion/GeoNamesMapping/countryCodeInfo.tsv");
									"resources/countryCodeInfo.tsv");

			String outputDirectory = null;
			String outputFileName = null;
			String sourceFile = null;
			switch (datasetType) {
				case DAXJSON:
					outputDirectory = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Server/17_02_28/Dax/";
					outputFileName = "17_03_09_Dax.testModel.nt";
					sourceFile = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/DFKI/17_03_09_Dax/DFKI_DAX30_v1_20170713.json";
					break;
				case GRID_SOURCE:
					outputDirectory = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Server/17_02_28/Grid";
					outputFileName = "16_05_18_Grid_Source_View.test.nt";
					sourceFile = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Grid/16_04_18_GRID/grid20160401/grid.json";
					break;
				case GRID:
					outputDirectory = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Server/17_02_28/Grid";
					outputFileName = "16_04_28_Grid.test.nt";
					sourceFile = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Grid/16_04_18_GRID/grid20160401/grid.json";
					break;
				case PERMID:
					outputDirectory = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Server/17_05_03/PermID";
					outputFileName = "16_04_28_PermID.test.nt";
					sourceFile = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/PermId/16_04_13/OpenPermID-bulk-organization-20160410_070326.ntriples";
					break;
				case GCD_NT:
					outputDirectory = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Server/17_02_28/GCD";
					outputFileName = "17_03_04_GCD.test.nt";
					sourceFile = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Server/16_01_22/GCD/16_01_19_gcd_1.nt";
					break;
				case GCD_CSV:
					outputDirectory = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Server/17_02_28/GCD";
					outputFileName = "17_03_04_GCD_new_2.test.nt";
					sourceFile = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/DFKI/17_03_04/firmendb-export/DFKI_FirmenDB_companies_clean_20170222.csv";
					break;
				case DBPEDIA_ORG:
					outputDirectory = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Server/16_04_01/DBpedia";
					outputFileName = "16_08_01_dbpedia_orgs.test.nt";
					sourceFile = "/home/kay/Uni/Project";
					break;
				case DBPEDIA_PERSON:
					outputDirectory = "/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Server/16_04_01/DBpedia";
					outputFileName = "16_08_01_dbpedia_persons.test.nt";
					sourceFile = "/home/kay/Uni/Project";
					break;
			}
			
			System.out.println("Convert dataset: " + datasetType);
			System.out.println("outputDirectory: " + outputDirectory);
			System.out.println("outputFileName: " + outputFileName);
			System.out.println("sourceFile: " + sourceFile);

			
			switch (datasetType) {
				case GRID_SOURCE:
					addMapping(mapping, ".*", "http://grid.source.ac/", null, null, true);
					break;
				case GRID: {			
					/// GRID
					addMapping(mapping, "name", "http://www.w3.org/2004/02/skos/core#prefLabel", null, "en");
					addMapping(mapping, "acronyms", "http://www.w3.org/2004/02/skos/core#altLabel", null, "en");
					addMapping(mapping, "aliases", "http://www.w3.org/2004/02/skos/core#altLabel", null, "en");
					addMapping(mapping, "links", FOAF.homepage.getURI().toString(), null, null);
					addMapping(mapping, "email_address", CorpDbpedia.prefixOntology + "emailAddress", null, null);
				} break;
				case PERMID: {
					addMapping(mapping, "http://permid.org/ontology/common/hasPermId",
						CorpDbpedia.prefixOntology + "identifier_permid", null, null);
				
					addMapping(mapping, "http://www.w3.org/2006/vcard/ns#organization-name",
						"http://www.w3.org/2004/02/skos/core#prefLabel", null, "en");
				
					addMapping(mapping, "http://www.w3.org/2006/vcard/ns#hasURL",
							FOAF.homepage.getURI().toString(), null, null);
					addMapping(mapping, "http://corp.dbpedia.org/ontology#geonamesIdCountry",
							CorpDbpedia.countryGeoNamesId, null, null);
					addMapping(mapping, "http://corp.dbpedia.org/ontology#geonamesIdCounty",
							CorpDbpedia.countyGeonamesId, null, null);
					addMapping(mapping, "http://corp.dbpedia.org/ontology#geonamesIdCity",
							CorpDbpedia.cityGeonamesId, null, null);
								
				} break;
				case GCD_NT: {
					addMapping(mapping, "http://www.w3.org/ns/org#identifier",
							CorpDbpedia.prefixOntology + "identifier_gcd", null, null);
					addMapping(mapping, "http://www.w3.org/2006/vcard/ns#country-name",
							CorpDbpedia.prefixOntology + "countryName", null, null);
					addMapping(mapping, "http://www.w3.org/2006/vcard/ns#region",
							CorpDbpedia.prefixOntology + "countyName",
							null, null);
					addMapping(mapping, "http://www.w3.org/2006/vcard/ns#locality",
							CorpDbpedia.prefixOntology + "cityName",
							null, null);
					addMapping(mapping, "http://www.w3.org/2006/vcard/ns#street-address",
							CorpDbpedia.prefixOntology + "siteAddress", null, null);
					addMapping(mapping, "http://www.w3.org/2006/vcard/ns#postal-code",
							CorpDbpedia.prefixOntology + "postalCode", null, null);
					addMapping(mapping, "http://corp.dbpedia.org/ontology/relationship/parent",
							OWL.sameAs.getURI(), null, null);
					
				} break;
				case DBPEDIA_ORG:
					addMapping(mapping, RDFS.label.toString(), SKOS.prefLabel.toString(), null, null);
					addMapping(mapping, FOAF.name.toString(), SKOS.prefLabel.toString(), null, null);
					addMapping(mapping, "http://dbpedia.org/property/acronym", SKOS.altLabel.toString(), null, null);
					addMapping(mapping, "http://dbpedia.org/property/acronyms", SKOS.altLabel.toString(), null, null);
					addMapping(mapping, "http://dbpedia.org/property/abbreviation", SKOS.altLabel.toString(), null, null);
					addMapping(mapping, "http://dbpedia.org/property/name", SKOS.altLabel.toString(), null, null);
					addMapping(mapping, "http://dbpedia.org/property/companyName", SKOS.altLabel.toString(), null, null);
					break;
			}
			
			
			String endpoint = "http://localhost:9890/sparql";
			String graphName = "http://dbpedia.org";
			
			DatasetImporter datasetImporter = null;
			switch (datasetType) {
			
			case GRID_SOURCE:
				case DAXJSON:
					datasetImporter = new DfkiDax30Json(sourceFile);
					break;
				case GRID:
					datasetImporter = new GridJsonDatasetImporter2(
							sourceFile, "", mapping);
					break;
				case PERMID:
				case GCD_NT:
					datasetImporter = new RdfNtDatasetImporter(
							sourceFile, mapping);
					break;
				case GCD_CSV:
					
					Map<String, ColumnAdapter> columnPredicateMap = new HashMap<>();
					
					ColumnAdapter adapterId = new ColumnAdapter();
					//adapterId.predicate = CorpDbpedia.prefixOntology  + "identifier_gcd";
					columnPredicateMap.put("id", adapterId);
					
					datasetImporter = new DfkiCsvDatasetImporter(sourceFile, columnPredicateMap, true, "\"", ",");
					break;
				case DBPEDIA_ORG: {
					String construct = "CONSTRUCT { ?subjectUri <http://www.w3.org/2004/02/skos/core#prefLabel> ?prefLabel . "
							+ "?subjectUri <http://www.w3.org/2004/02/skos/core#altLabel> ?altLabel . "
							+ "?subjectUri <http://dbpedia.org/ontology/headquarter> ?headquarter . "
							+ "?subjectUri <http://xmlns.com/foaf/0.1/homepage> ?homepage . "
							+ "?subjectUri <http://dbpedia.org/ontology/foundingDate> ?foundationYear . "
							+ "?subjectUri <http://dbpedia.org/ontology/foundingDate> ?foundationDate . "
							+ "?subjectUri <http://dbpedia.org/ontology/numberOfEmployees> ?numberOfEmployees . "
							+ "?subjectUri <http://www.w3.org/ns/org#subOrganizationOf> ?parentCompany . "
							+ "?subjectUri <http://www.w3.org/ns/org#hasSubOrganization> ?subsidiary . "
							+ "?subjectUri <" + OWL.sameAs.toString() + "> ?sameAs ."
						    + "?subjectUri <http://dbpedia.org/property/defunct> ?defunct ."
							+ "?subjectUri <http://dbpedia.org/ontology/extinctionYear> ?extinctionYear ."
						    + "?subjectUri <http://dbpedia.org/ontology/successor> ?successor ."
							+ "?subjectUri <http://dbpedia.org/ontology/type> ?typeUri . "
							+ "?subjectUri <http://dbpedia.org/ontology/typeLabel> ?typeLabel . "
							+ "?subjectUri <http://www.w3.org/ns/org#headOf> ?headOf . "
							+ "}";
					
					String select = "SELECT * WHERE { " +
						   "     { ?subjectUri <http://www.w3.org/2000/01/rdf-schema#label> ?prefLabel . } UNION  { ?subjectUri <http://xmlns.com/foaf/0.1/name> ?prefLabel . } " +
						   "     OPTIONAL { ?subjectUri <" + OWL.sameAs.toString() + "> ?sameAs . }" +
						   "     OPTIONAL { ?subjectUri <http://dbpedia.org/property/acronym> ?altLabel . }" +
						   " 	 OPTIONAL { ?subjectUri <http://dbpedia.org/property/acronyms> ?altLabel . } " +
						   " 	 OPTIONAL { ?subjectUri <http://dbpedia.org/property/abbreviation> ?altLabel . } " +
						   " 	 OPTIONAL { ?subjectUri <http://dbpedia.org/property/name> ?altLabel . } " +
						   " 	 OPTIONAL { ?subjectUri <http://dbpedia.org/property/companyName> ?altLabel . } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/headquarter> ?headquarter } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/locationCity> ?headquarter } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/locationCountry> ?headquarter } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/location> ?headquarter } " +
						   "	 OPTIONAL { ?subjectUri <http://de.dbpedia.org/property/sitz> ?headquarter } " +
						   "	 OPTIONAL { ?subjectUri <http://xmlns.com/foaf/0.1/homepage> ?homepage } " +
//						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/wikiPageExternalLink> ?homepage } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/foundingYear> ?foundationYear } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/foundingDate> ?foundationDate } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/formationDate> ?foundationDate } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/property/gründungsdatum> ?foundationDate } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/numberOfEmployees> ?numberOfEmployees } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/parentCompany> ?parentCompany } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/owningCompany> ?parentCompany } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/subsidiary> ?subsidiary } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/extinctionYear> ?extinctionYear } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/auflösungsdatum> ?extinctionYear } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/property/defunct> ?defunct } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/successor> ?successor } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/type> ?typeUri } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/type>/<http://www.w3.org/2000/01/rdf-schema#label> ?typeLabel } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/chairman> ?headOf . { ?headOf a <http://dbpedia.org/ontology/Person> } UNION { ?headOf a <http://dbpedia.org/ontology/Company> } UNION { ?headOf a <http://dbpedia.org/ontology/Organisation> } } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/keyPerson> ?headOf . { ?headOf a <http://dbpedia.org/ontology/Person> } UNION { ?headOf a <http://dbpedia.org/ontology/Company> } UNION { ?headOf a <http://dbpedia.org/ontology/Organisation> } } " + 
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/owner> ?headOf . { ?headOf a <http://dbpedia.org/ontology/Person> } UNION { ?headOf a <http://dbpedia.org/ontology/Company> } UNION { ?headOf a <http://dbpedia.org/ontology/Organisation> } } " +
						   "	 OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/owningCompany> ?headOf . { ?headOf a <http://dbpedia.org/ontology/Person> } UNION { ?headOf a <http://dbpedia.org/ontology/Company> } UNION { ?headOf a <http://dbpedia.org/ontology/Organisation> } } " +
						   //http://dbpedia.org/ontology/keyPerson
						   //http://dbpedia.org/ontology/chairman
						   "  }";
					
					String filterQuery = "SELECT * WHERE { { ?subjectUri a <http://dbpedia.org/ontology/Company> . } UNION { ?subjectUri a <http://dbpedia.org/ontology/Organisation> . } }";
					
//					String query = "	CONSTRUCT { ?s <http://www.w3.org/2004/02/skos/core#prefLabel> ?label } " +
//								   "FROM <http://dbpedia_new.org> WHERE { " +
//								   " { " +
//								   "   SELECT * WHERE { " +
//								   "     ?s a <http://dbpedia.org/ontology/Company> . " +
//								   "     ?s <http://www.w3.org/2000/01/rdf-schema#label> ?label . " +
//								   "  } ORDER BY ?s " +
//								   " } " +
//								   "} LIMIT 10000 OFFSET 10000";
					
					datasetImporter = new SparqlDatasetImporter(endpoint, graphName, filterQuery, construct, select);
				} break;
				case DBPEDIA_PERSON: {
					String construct = "CONSTRUCT { ?subjectUri a <http://xmlns.com/foaf/0.1/Person> . "
							+ " ?subjectUri <" + OWL.sameAs.toString() + "> ?sameAs . "
							+ " ?subjectUri <http://xmlns.com/foaf/0.1/givenName> ?givenName . "
							+ " ?subjectUri <http://xmlns.com/foaf/0.1/surname> ?surname . "
							+ " ?subjectUri <http://xmlns.com/foaf/0.1/name> ?name . "
							+ " ?subjectUri <http://dbpedia.org/ontology/deathDate> ?deathDate . "
							+ " ?subjectUri <http://dbpedia.org/ontology/birthDate> ?birthDate . "
							+ " ?subjectUri <http://xmlns.com/foaf/0.1/homepage> ?homepage . "
 						    + " ?subjectUri <" + OWL.sameAs.toString() + "> ?subjectUri . " // ensure that we keep the link to the original source
							+ "}";
					
					String select = "SELECT * WHERE { "
						   + "     { ?subjectUri <http://www.w3.org/2000/01/rdf-schema#label> ?name . } UNION  { ?subjectUri <http://xmlns.com/foaf/0.1/name> ?name . } "
						   + "     OPTIONAL { ?subjectUri <" + OWL.sameAs.toString() + "> ?sameAs . }"
						   + "     OPTIONAL { ?subjectUri <http://xmlns.com/foaf/0.1/givenName> ?givenName . }"
						   + "     OPTIONAL { ?subjectUri <http://xmlns.com/foaf/0.1/surname> ?surname . }"
						   + "	   OPTIONAL { ?subjectUri <http://xmlns.com/foaf/0.1/homepage> ?homepage . }"
						   + "	   OPTIONAL { ?subjectUri <http://xmlns.com/foaf/0.1/nick> ?nick . }"
						   + "	   OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/deathDate> ?deathDate . }"
						   + "	   OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/deathyear> ?deathDate . }"
						   + "	   OPTIONAL { ?subjectUri <http://dbpedia.org/ontology/birthDate> ?birthDate . }"
						   + "  }";
					
					String filterQuery = "SELECT * WHERE { ?subjectUri a <http://dbpedia.org/ontology/Person> . }";
					
//					String query = "	CONSTRUCT { ?s <http://www.w3.org/2004/02/skos/core#prefLabel> ?label } " +
//								   "FROM <http://dbpedia_new.org> WHERE { " +
//								   " { " +
//								   "   SELECT * WHERE { " +
//								   "     ?s a <http://dbpedia.org/ontology/Company> . " +
//								   "     ?s <http://www.w3.org/2000/01/rdf-schema#label> ?label . " +
//								   "  } ORDER BY ?s " +
//								   " } " +
//								   "} LIMIT 10000 OFFSET 10000";
					
					datasetImporter = new SparqlDatasetImporter(endpoint, graphName, filterQuery, construct, select);
				} break;
			}
			
			/// Permid orginal organization nt file


			/// from old NT triples file GCD!!!!
//			addMapping(mapping, "http://www.w3.org/ns/org#identifier",
//					CorpDbpedia.prefixOntology + "identifier_gcd", null, null);
//			addMapping(mapping, "http://www.w3.org/2006/vcard/ns#country-name",
//					CorpDbpedia.prefixOntology + "countryName", null, null);
//			addMapping(mapping, "http://www.w3.org/2006/vcard/ns#region", CorpDbpedia.prefixOntology + "countyName",
//					null, null);
//			addMapping(mapping, "http://www.w3.org/2006/vcard/ns#locality", CorpDbpedia.prefixOntology + "cityName",
//					null, null);
//			addMapping(mapping, "http://www.w3.org/2006/vcard/ns#street-address",
//					CorpDbpedia.prefixOntology + "siteAddress", null, null);
//			addMapping(mapping, "http://www.w3.org/2006/vcard/ns#postal-code",
//					CorpDbpedia.prefixOntology + "postalCode", null, null);
//			addMapping(mapping, CorpDbpedia.prefixOntology + "relationship/parent", OWL.sameAs.getURI(), null, null);
			
			
			//addMapping(mapping, "http://xmlns.com/foaf/0.1/homepage", "http://xmlns.com/foaf/0.1/homepage", "", null);
			// addMapping(mapping,
			// "http://ont.thomsonreuters.com/mdaas/HeadquartersAddress",
			// VCARD.ADDR_STR, null, "en");
			//
			// addMapping(mapping,
			// "http://www.omg.org/spec/EDMC-FIBO/BE/LegalEntities/CorporateBodies/isDomiciledIn",
			// VCARD.ADDR_COUNTRY_NAME, null, "en");
			//
			// addMapping(mapping,
			// "http://permid.org/ontology/organization/isIncorporatedIn",
			// VCARD.ADDR_COUNTRY_NAME, null, "en");
			//
			// addMapping(mapping,
			// "http://www.w3.org/2006/vcard/ns#organization-name",
			// "http://www.w3.org/2004/02/skos/core#prefLabel", null, "en");
			//
			// addMapping(mapping,
			// "http://permid.org/ontology/common/hasPermId",
			// OrgW3c.identifier, XSDDatatype.XSDstring, null);
			//
			// addMapping(mapping, "http://www.w3.org/2006/vcard/ns#hasURL",
			// FOAF.homepage.getURI(), XSDDatatype.XSDstring, null);

			// http://www.omg.org/spec/EDMC-FIBO/BE/LegalEntities/CorporateBodies/isDomiciledIn

			// RdfMapping labelMapping = new RdfMapping();
			// labelMapping.targetMapping =
			// "http://www.w3.org/2004/02/skos/core#prefLabel";
			// labelMapping.dataType = XSDDatatype.XSDstring;
			// mapping.put("<http://www.w3.org/2000/01/rdf-schema#label",
			// labelMapping);
			//
			// RdfMapping idMapping = new RdfMapping();
			// idMapping.targetMapping = "http://www.w3.org/ns/org#identifier";
			// idMapping.dataType = XSDDatatype.XSDstring;
			// mapping.put(CorpDbpedia.prefixOntology + "permid>",
			// idMapping);
			//
			// RdfMapping typeMapping = new RdfMapping();
			// typeMapping.targetMapping = RDF.type.toString();
			// mapping.put(RDF.type.toString(), typeMapping);
			//
			// RdfMapping sameMapping = new RdfMapping();
			// sameMapping.targetMapping = OWL.sameAs.toString();
			// mapping.put(OWL.sameAs.toString(), sameMapping);
			//
			// RdfMapping foundingDateMapping = new RdfMapping();
			// foundingDateMapping.targetMapping =
			// "http://dbpedia.org/ontology/foundingDate";
			// foundingDateMapping.dataType = XSDDatatype.XSDdateTime;
			// mapping.put("http://dbpedia.org/ontology/foundingDate",
			// foundingDateMapping);
			//
			// RdfMapping homepageMapping = new RdfMapping();
			// homepageMapping.targetMapping =
			// "http://xmlns.com/foaf/0.1/homepage";
			// homepageMapping.targetMapping = RDF.type.toString();
			// mapping.put("http://xmlns.com/foaf/0.1/homepage>",
			// homepageMapping);
			
			// GRID


			// new
			// RdfNtDatasetImporter("/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/PermId_Martin/NT/",
			// mapping);
			//
			// GridJsonDatasetImporter datasetImporter = new
			// GridJsonDatasetImporter("/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Grid/16_01_20_GRID/grid20151214/grid.json");
			//
			// File outputFile = new
			// File("/home/kay/Uni/Projects/SmartDataWeb/CompanyData/DataSets/Grid/16_01_20_GRID/grid20151214/test_new.nt");
			// FileWriter fileWriter = new FileWriter(outputFile);
			// BufferedWriter writer = new BufferedWriter(fileWriter);
			//
			// while (datasetImporter.hasNext()) {
			// Entity entity = datasetImporter.nextEntity();
			//
			// // make them all organizations
			// RdfObjectUri organizationType = new
			// RdfObjectUri(OrgW3c.organization);
			// entity.addTriple(RDF.type.getNameSpace(), organizationType);
			// entity.addTriple(RDF.type.toString(), new
			// RdfObjectUri("http://dbpedia.org/ontology/Organisation"));
			//
			// writer.append(entity.toString());
			// }
			//
			// writer.close();
			//
			// if (true) {
			// return;
			// }
			
			

			CsvSparqlify csvImport = new CsvSparqlify(uriPrefix, datasetImporter,
					outputDirectory, outputFileName, datasetType, filterUnknownProperties);

			csvImport.run();

			System.out.println("Stats: " + csvImport.getStats().getStatistics());

			// String outputString = csvImport.getOutput();
			// BufferedWriter write = new BufferedWriter(new FileWriter(new
			// File("/home/kay/test.rdf")));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
