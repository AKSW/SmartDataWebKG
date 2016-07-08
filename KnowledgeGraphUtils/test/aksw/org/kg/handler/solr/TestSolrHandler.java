package aksw.org.kg.handler.solr;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import aksw.org.kg.KgException;
import aksw.org.sdw.kg.handler.solr.KgSolrResultDocument;
import aksw.org.sdw.kg.handler.solr.SolrHandler;
import aksw.org.sdw.kg.handler.solr.SolrHandler.AnnotationInfo;
import aksw.org.sdw.kg.handler.solr.SolrHandler.TAGGER_ANNOTATION_OVERLAP;
import aksw.org.sdw.kg.handler.solr.SolrHandler.TAGGER_LANGUAGE;

public class TestSolrHandler {
	
	static String solrUrl = "http://localhost:8983/solr/companies";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * This method can be used to check whether we can connect with SOLR
	 * @throws IOException 
	 */
	@Test
	public void contactSolr() throws IOException {
		SolrHandler solrHandler = new SolrHandler("http://localhost:8983/solr/companies");
		solrHandler.close();
	}
	
	/**
	 * This method can be used to execute solr search queries
	 * 
	 * @throws IOException 
	 * @throws KgException 
	 */
	@Test
	public void testQueryExecutionSolr() throws IOException, KgException {
		SolrHandler solrHandler = new SolrHandler(solrUrl);
		
		List<KgSolrResultDocument> resuts = solrHandler.executeQuery("nameEn:\"Germany\"", null);
		assertNotNull("Got results", resuts);
		assertFalse("Got results", resuts.isEmpty());
		
		solrHandler.close();
	}
	
	/**
	 * This method can be used to execute solr search queries with filters
	 * 
	 * @throws IOException 
	 * @throws KgException 
	 */
	@Test
	public void testQueryFilterExecutionSolr() throws IOException, KgException {
		SolrHandler solrHandler = new SolrHandler(solrUrl);
		
		List<KgSolrResultDocument> resuts = solrHandler.executeQuery("nameEn:\"Germany\"",
									Arrays.asList("type:\"http://dbpedia.org/ontology/Country\""));
		assertNotNull("Got results", resuts);
		assertFalse("Got results", resuts.isEmpty());
		
		solrHandler.close();
	}
	
	/**
	 * Check whether basic annotations can be found and are returned.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQueryFromText() throws Exception {
		SolrHandler solrHandler = new SolrHandler(solrUrl);
		
		Map<AnnotationInfo, List<KgSolrResultDocument>> result = solrHandler.getNamedEntitiesFromText("Berlin is a great city in Germany", null, null,
				  TAGGER_LANGUAGE.ENGLISH, TAGGER_ANNOTATION_OVERLAP.ALL);
		solrHandler.close();
		
		assertNotNull("Got a result", result);
		assertEquals("Got two resutls", 2, result.size());
		
		List<String> matchingTexts = Arrays.asList("Berlin", "Germany");
		for (AnnotationInfo info : result.keySet()) {
			assertTrue("Found correct match", matchingTexts.contains(info.matchText));
		}
	}
	
	/**
	 * Check whether it is possible to filter by type
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQueryFromTextWithFilter() throws Exception {
		SolrHandler solrHandler = new SolrHandler(solrUrl);
		
		List<String> filterQuery = Arrays.asList("type:\"http://dbpedia.org/ontology/Country\"");
		Map<AnnotationInfo, List<KgSolrResultDocument>> result = solrHandler.getNamedEntitiesFromText("Berlin is a great city in Germany", filterQuery, null,
				  TAGGER_LANGUAGE.ENGLISH, TAGGER_ANNOTATION_OVERLAP.ALL);
		solrHandler.close();
		
		assertNotNull("Got a result", result);
		assertEquals("Got two resutls", 1, result.size());
		
		List<String> matchingTexts = Arrays.asList("Germany");
		for (AnnotationInfo info : result.keySet()) {
			assertTrue("Found correct match", matchingTexts.contains(info.matchText));
		}
	}
	
	/**
	 * Check whether it is possible to use multiple filters
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQueryFromTextWithFilter2() throws Exception {
		SolrHandler solrHandler = new SolrHandler(solrUrl);
		
		List<String> filterQuery = Arrays.asList("type:\"http://dbpedia.org/ontology/Country\"", "nameEn:\"England\"");
		//Map<AnnotationInfo, List<KgSolrResultDocument>> result = solrHandler.getNamedEntitiesFromTextEn("Berlin is a great city in Germany", filterQuery, null);
		Map<AnnotationInfo, List<KgSolrResultDocument>> result = solrHandler.getNamedEntitiesFromText("Berlin is a great city in Germany", filterQuery, null,
																									  TAGGER_LANGUAGE.ENGLISH, TAGGER_ANNOTATION_OVERLAP.ALL);
		
		solrHandler.close();
		
		assertNull("Got no result", result);
	}
	
	/**
	 * Check whether it is possible to filter by type
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQueryFromTextWithFilter3() throws Exception {
		SolrHandler solrHandler = new SolrHandler(solrUrl);
		
		List<String> filterQuery = Arrays.asList("type:\"http://dbpedia.org/ontology/Country\"", "nameDe:Deutschland");
		Map<AnnotationInfo, List<KgSolrResultDocument>> result = solrHandler.getNamedEntitiesFromText("Berlin is a great city in Germany", filterQuery, null,
				  TAGGER_LANGUAGE.ENGLISH, TAGGER_ANNOTATION_OVERLAP.ALL);
		solrHandler.close();
		
		assertNotNull("Got a result", result);
		assertEquals("Got two resutls", 1, result.size());
		
		List<String> matchingTexts = Arrays.asList("Germany");
		for (AnnotationInfo info : result.keySet()) {
			assertTrue("Found correct match", matchingTexts.contains(info.matchText));
		}
	}
}
