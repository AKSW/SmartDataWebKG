package aksw.org.kg;

import static org.junit.Assert.*;

import org.apache.jena.rdf.model.Literal;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import aksw.org.kg.entity.Entity;

/**
 * This test can be used to check whether the update functionality works
 * 
 * @author kay
 *
 */
public class UpdateTests {
	
	Entity testEntity;
	
	static final String testPredicate = "http://example.org/resource/test";
	
	Literal oldLiteral;
	Literal newLiteral;


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		this.testEntity = new Entity(false);
		
		this.testEntity.setSubjectUri(UpdateTests.testPredicate);
		
		this.oldLiteral = this.testEntity.getLiteral("Hello Old Day");
		this.newLiteral = this.testEntity.getLiteral("Hello New Day", "en");		
		
		this.testEntity.addTriple(UpdateTests.testPredicate, this.oldLiteral);
	}

	@After
	public void tearDown() throws Exception {
		this.testEntity = null;
	}

	@Test
	public void test() {
		String before = this.testEntity.toString();
		this.testEntity.updateTripleObject(UpdateTests.testPredicate, this.oldLiteral, this.newLiteral);
		String after = this.testEntity.toString();
		
		assertNotEquals("Was able to update String", before, after);
		assertTrue("Contains new literal", after.contains("New Day"));
		assertTrue("Contains language code", after.contains("@en"));		
	}
}
