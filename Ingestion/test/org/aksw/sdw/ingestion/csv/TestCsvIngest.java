//package org.aksw.sdw.ingestion.csv;
//
//import static org.junit.Assert.*;
//
//import java.io.File;
//import java.util.List;
//
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import junitparams.JUnitParamsRunner;
//import junitparams.Parameters;
//
//@RunWith(JUnitParamsRunner.class)
//public class TestCsvIngest {
//
//	@BeforeClass
//	public static void setUpBeforeClass() throws Exception {
//	}
//
//	@AfterClass
//	public static void tearDownAfterClass() throws Exception {
//	}
//
//	@Before
//	public void setUp() throws Exception {
//	}
//
//	@After
//	public void tearDown() throws Exception {
//	}
//
//	/**
//	 * This test checks whether headers can be obtained
//	 * @throws Exception 
//	 */
//	@Test
//	public void testHeaders() throws Exception {
//		CsvSparqlify csvImport = new CsvSparqlify(null, "test0.csv", "", true, ',', "/temp/test.nt");
//		
//		File csvFile = CsvSparqlify.getFileInstance("test0.csv");
//		List<String> headers = csvImport.readCsvFileHeaders(csvFile, true);
//		assertNotNull("Headers not null", headers);
//		
//		assertEquals("Found right amount of headers", 4, headers.size());
//		
//		// check that we don't have any spaces left
//		for (String header : headers) {
//			assertFalse("Was not able to find space", header.contains(" "));
//		}
//	}
//	
//	public Object[] parametersForTestWrongUrl() {
//		String[] prefixes = {
//				"example.org",
//				"example",
//				"http:example",
//				"http://example",
//				"http:// example.org"
//		};
//		
//		return prefixes;
//	}
//	
//	/**
//	 * This test checks whether an invalid url is found
//	 * @throws Exception 
//	 */
//	@Test(expected=IngestionException.class)
//	@Parameters(source = TestCsvIngest.class, method = "parametersForTestWrongUrl")
//	public void testWrongUrl(String prefix) throws Exception {
//		CsvSparqlify csvImport = new CsvSparqlify(prefix, "test0.csv", "", true, ',', "/tmp/test.nt");
//	}
//	
//	/**
//	 * This method is used to run a full creation cycle
//	 * 
//	 * @throws Exception
//	 */
//	@Test
//	public void testMapping0() throws Exception {
//		CsvSparqlify csvImport = new CsvSparqlify(null, "test0.csv", "mapping0.txt", true, ',', "/tmp/test.nt");
//		
//		csvImport.run();
//	}
//	
//	/**
//	 * This method is used to run a full creation cycle
//	 * 
//	 * @throws Exception
//	 */
//	@Test
//	public void testMapping1() throws Exception {
//		CsvSparqlify csvImport = new CsvSparqlify(null, "test1.csv", "mapping2.txt", true, ',', "/tmp/test.nt");
//		
//		csvImport.run();
//	}
//}
