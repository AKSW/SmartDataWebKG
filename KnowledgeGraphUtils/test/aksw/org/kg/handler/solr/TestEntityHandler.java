package aksw.org.kg.handler.solr;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import aksw.org.kg.KgException;
import aksw.org.sdw.kg.handler.solr.EntityHandler;
import aksw.org.sdw.kg.handler.solr.SolrHandler;

public class TestEntityHandler {
	
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

	@Test
	public void testEntities0() throws KgException {
		SolrHandler solrHandler = new SolrHandler(solrUrl);
		
		EntityHandler entityHandler = new EntityHandler(solrHandler);
		
		List<String> ids = entityHandler.getMatchingEntities("Deutschland", "de", null);
		assertNotNull("Managed to get ids", ids);
		assertFalse("Managed to get ids", ids.isEmpty());
	}
	
	@Test
	public void testEntities1() throws KgException {
		SolrHandler solrHandler = new SolrHandler(solrUrl);
		
		EntityHandler entityHandler = new EntityHandler(solrHandler);
		
		List<String> ids = entityHandler.getMatchingEntities("Germany", "de", null);
		assertNotNull("Managed to get ids", ids);
		assertTrue("Managed to get no ids", ids.isEmpty());
	}
	
	@Test
	public void testEntities2() throws KgException {
		SolrHandler solrHandler = new SolrHandler(solrUrl);
		
		EntityHandler entityHandler = new EntityHandler(solrHandler);
		
		List<String> ids = entityHandler.getMatchingEntities("Ireland", "en", null);
		assertNotNull("Managed to get ids", ids);
		assertFalse("Managed to get ids", ids.isEmpty());
	}
	
	@Test
	public void testEntities3() throws KgException {
		SolrHandler solrHandler = new SolrHandler(solrUrl);
		
		EntityHandler entityHandler = new EntityHandler(solrHandler);
		
		List<String> ids = entityHandler.getMatchingEntities("United States", "en", null);
		assertNotNull("Managed to get ids", ids);
		assertFalse("Managed to get ids", ids.isEmpty());
	}
	
	@Test
	public void testEntitiesWithType0() throws KgException {
		SolrHandler solrHandler = new SolrHandler(solrUrl);
		
		EntityHandler entityHandler = new EntityHandler(solrHandler);
		
		List<String> ids = entityHandler.getMatchingEntities("United States of America", "en",
															 "http://dbpedia.org/ontology/Country");
		assertNotNull("Managed to get ids", ids);
		assertFalse("Managed to get ids", ids.isEmpty());
	}
	
	/**
	 * This test checks whether a wrong type results no results for "United States"
	 * @throws KgException
	 */
	@Test
	public void testEntitiesWithType1() throws KgException {
		SolrHandler solrHandler = new SolrHandler(solrUrl);
		
		EntityHandler entityHandler = new EntityHandler(solrHandler);
		
		List<String> ids = entityHandler.getMatchingEntities("United States", "en",
															 "http://dbpedia.org/ontology/City");
		assertNotNull("Managed to get ids", ids);
		assertTrue("Managed to get ids", ids.isEmpty());
	}
}
