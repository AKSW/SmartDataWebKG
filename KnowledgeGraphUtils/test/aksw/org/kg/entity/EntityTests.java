package aksw.org.kg.entity;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class EntityTests {
	
	static final String testUri = "http://example.org/test";
	static final String testPredicateDelete = "http://example.org/predicate/delete";
	
	Entity entity;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		
		this.entity = new Entity(false);
		this.entity.setSubjectUri(EntityTests.testUri);
	}

	@After
	public void tearDown() throws Exception {		
		
		this.entity = null;
	}

	/**
	 * This test can be used to check whether it is possible to delete a predicate with all its objects.
	 */
	@Test
	public void testDelete() {
		// add triples to entity
		this.entity.addTriple(testPredicateDelete, entity.getLiteral("Label0"));
		this.entity.addTriple(testPredicateDelete, entity.getLiteral("Label1"));
		this.entity.addTriple(testPredicateDelete, entity.getLiteral("Label2"));
		
		Collection<String> predicatesBefore = this.entity.getPredicates();
		
		this.entity.deleteProperty(testPredicateDelete);
		
		Collection<String> predicatesAfter = this.entity.getPredicates();
		
		String output = this.entity.toString();
		assertFalse("Does not contain added predicate", output.contains(testPredicateDelete));
		assertNotEquals("Are not the same", predicatesBefore, predicatesAfter);
		assertEquals("Deleted predicate", 0, predicatesAfter.size());
	}

}
