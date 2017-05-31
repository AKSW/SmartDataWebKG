package aksw.org.kg.entity;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.update.UpdateAction;

/**
 * This class stores all the information which
 * can be found for one entity;
 * 
 * @author kay
 *
 */
public class Entity {
	
	/** clean-up pattern which can be used to find non used characters */
	final static Pattern cleanupPattern = Pattern.compile("([\\W\\s_]+)", Pattern.UNICODE_CHARACTER_CLASS);
	
	/** clean up in case white spaces are found */
	final static Pattern cleanupWhiteSpaces = Pattern.compile("\\s+");
	
	/** data store which will store all the entity information */
	//protected Map<String, Map<RdfObject, AtomicInteger>> entityInformation = new LinkedHashMap<>();
	
	/** container which can be used to store sub entity information */
	protected Map<String, Entity> subEntities = null;
	
	protected Map<String, Entity> subEntitiesCategoryMap = null;
	
	/** specifies the actual subject uri */
	protected String mainSubjectUri = null;
	
	/** main uri object for reuse */
	protected Resource mainSubjectUriObject = null;
	
	/** specifies whether this is a blank node */
	protected final boolean isBlankNode;
	
	/** uri prefix */
	final String uriPrefix;
	
	/** actual model of this entity */
	Model entityModel;
	
	/**
	 * Constructor which can be used to
	 * specify whether this is a blank node
	 * 
	 * @param isBlankNode
	 */
	public Entity(final String uriPrefix, final boolean isBlankNode) {
		this.isBlankNode = isBlankNode;
		this.uriPrefix = uriPrefix;
		
		this.entityModel = ModelFactory.createDefaultModel();
	}
	
	/**
	 * Constructor which create a normal entity (not a blank node)
	 */
	public Entity(final String uriPrefix) {
		this(uriPrefix, false);
	}
	
	/**
	 * Constructor which can be used to create blank nodes
	 */
	public Entity(final boolean isBlankNode) {
		this(null, isBlankNode);
	}
	
	public boolean isBlankNode() {
		return this.isBlankNode;
	}
	
	/**
	 * Ensures that all the stored data is thrown away
	 */
	public void reset() {
		
		if (null != this.entityModel) {
			this.entityModel.close();
			this.entityModel = null;
			
			this.entityModel = ModelFactory.createDefaultModel();
		}
		
		if (null != this.subEntities) {
			for (String key : subEntities.keySet()) {
				subEntities.get(key).reset();
			}
			this.subEntities.clear();
			
			for (String key : subEntitiesCategoryMap.keySet()) {
				subEntitiesCategoryMap.get(key).reset();
			}
			this.subEntitiesCategoryMap.clear();
		}
		
		this.mainSubjectUri = null;
		this.mainSubjectUriObject = null;
	}
	
	/**
	 * 
	 * @return true if data is stored and false otherwise
	 */
	public boolean isEmpty() {
		boolean isEmpty = this.entityModel.isEmpty();
		
		if (null != this.subEntities) {
			for (Entity subEntity : this.subEntities.values()) {
				isEmpty |= subEntity.isEmpty();
			}
		}
		
		return isEmpty;
	}
	
	/**
	 * Method which can be used to clean uri using the uri prefix
	 * 
	 * @param uri
	 * @param uriPrefix
	 * @return
	 */
	public static String cleanUriString(final String uri, final String uriPrefix) {
		if (null == uri || null == uriPrefix) {
			return uri;
		}
		
		if (false == uri.startsWith(uriPrefix)) {
			return uri;
		}
		
		int prefixLength = uriPrefix.length();
		int startName = uri.indexOf("_", prefixLength + 1);
		
		String name = uri.substring(startName);
		String cleanName = cleanupPattern.matcher(name).replaceAll(" ").trim();// TODO km: check when and why this is needed! .toLowerCase();
		cleanName = cleanupWhiteSpaces.matcher(cleanName).replaceAll("_");
		
		String rest = uri.substring(prefixLength, startName + 1);
		String cleanedSubjectUri = new StringBuffer().append(uriPrefix).append(rest).append(cleanName).toString();
		
		return cleanedSubjectUri;
	}
	
	public Collection<String> getPredicates() {
		StmtIterator statements = this.entityModel.listStatements();
		
		Set<String> statementUris = new HashSet<>();
		
		while (statements.hasNext()) {
			Statement statement = statements.next();
			
			Property predicate = statement.getPredicate();
			String predicateUri = predicate.getURI();
			
			statementUris.add(predicateUri);
		}
		
		return Collections.unmodifiableCollection(statementUris);
	}
	

	/**
	 * 
	 * @param uri - main uri which can be used to identify this entity
	 */
	public void setSubjectUri(final String uri) {
		if (null == uri) {
			return;
		}
		
		String cleanedSubjectUri;
		if (null == this.uriPrefix) {
			// make sure there are no whitespaces
			cleanedSubjectUri = cleanupWhiteSpaces.matcher(uri).replaceAll(" ");
			cleanedSubjectUri = cleanupWhiteSpaces.matcher(cleanedSubjectUri).replaceAll("_");
		} else {
			cleanedSubjectUri = Entity.cleanUriString(uri, this.uriPrefix);
		}
		
		// create unique blank node or just take the uri as is
		if (this.isBlankNode) {
			long randomID = UUID.randomUUID().getLeastSignificantBits();
			// make sure all of them are positive and still unique
			randomID = ((0L > randomID) ? -10L * randomID : randomID);
			String blankUri = cleanedSubjectUri + "_" + ((0L > randomID) ? -1L * randomID : randomID);
			this.mainSubjectUri = blankUri;
		} else {
			this.mainSubjectUri = cleanedSubjectUri;
		}
		
		// make sure that we create a new one
		this.mainSubjectUriObject = null;
	}
	
	/**
	 * 
	 * @return main uri which can be used to identify this entity
	 */
	public String getSubjectUri() {
		return this.mainSubjectUri;
	}
	
	public Resource getSubjectUriObject() {
		if (null == this.mainSubjectUriObject && null != this.mainSubjectUri) {
			this.mainSubjectUriObject = new ResourceImpl(this.mainSubjectUri);
		}
		
		return this.mainSubjectUriObject;
	}
	
	public long getTripleCount() {
		
		long totalTripleCount = this.entityModel.size();		
		
		// just check if the other sub-entities have queries as well
		if (null != this.subEntities) {
			for (Entity entity : this.subEntities.values()) {
				totalTripleCount += entity.getTripleCount();
			}
		}
		
		return totalTripleCount;
	}
	
	/**
	 * This method can be used to add a new triple
	 * 
	 * @param triple
	 */
	public void addTriple(final Triple triple) {
		if (null == triple) {
			return;
		}
		
		// create statement from triple
		Statement statement = this.entityModel.asStatement(triple);		
		
		// add statement
		this.entityModel.add(statement);
	}
	
	public Literal getLiteral(final String label) {
		return this.entityModel.createLiteral(label);
	}
	
	/**
	 * Creates Literal from given label and language Code
	 * 
	 * @param label
	 * @param languageCode
	 * @return
	 */
	public Literal getLiteral(final String label, final String languageCode) {
		return this.entityModel.createLiteral(label, languageCode);
	}
	
	/**
	 * Creates Literal from given label and datatype instance
	 * 
	 * @param label
	 * @param languageCode
	 * @return
	 */
	public Literal getLiteral(final String label, final RDFDatatype dataType) {
		return this.entityModel.createTypedLiteral(label, dataType);
	}
	
	/**
	 * Add triple with literal with language code
	 * 
	 * @param predicate
	 * @param label
	 */
	public void addTripleWithLiteral(final String predicateString, final String label) {
		this.addTriple(new PropertyImpl(predicateString), this.getLiteral(label));
	}
	
	/**
	 * Add triple with literal with language code
	 * 
	 * @param predicate
	 * @param label
	 * @param languageCode
	 */
	public void addTripleWithLiteral(final String predicateString, final String label, final String languageCode) {
		this.addTripleWithLiteral(new PropertyImpl(predicateString), label, languageCode);
	}
	
	/**
	 * Add triple with literal with language code
	 * 
	 * @param predicate
	 * @param label
	 * @param languageCode
	 */
	public void addTripleWithLiteral(final Property predicate, final String label, final String languageCode) {
		this.addTriple(predicate, this.getLiteral(label, languageCode));
	}
	
	/**
	 * Add triple with literal and literal data type information
	 * 
	 * @param predicate
	 * @param lexicalForm
	 * @param dataType
	 */
	public void addTripleWithLiteral(final String predicateString, final String label, final RDFDatatype dataType) { 
		this.addTripleWithLiteral(new PropertyImpl(predicateString), label, dataType);
	}
	
	/**
	 * Add triple with literal and literal data type information
	 * 
	 * @param predicate
	 * @param lexicalForm
	 * @param dataType
	 */
	public void addTripleWithLiteral(final Property predicate, final String label, final RDFDatatype dataType) {
		this.addTriple(predicate, this.getLiteral(label, dataType));
	}
	
	/**
	 * Adds triple with RDFNode (e.g. URI, Literal)
	 * 
	 * @param predicate
	 * @param object
	 */
	public void addTriple(final String predicateString, final RDFNode object) {
		this.addTriple(new PropertyImpl(predicateString), object);
	}
	
	/**
	 * Adds triple with RDFNode (e.g. URI, Literal)
	 * 
	 * @param predicate
	 * @param object
	 */
	public void addTriple(final Property predicate, final RDFNode object) {
		if (null == predicate || null == object) {
			return;
		}
		
		Statement statement = ResourceFactory.createStatement(
				getSubjectUriObject(), predicate, object);		
		
		// add statement
		this.entityModel.add(statement);
	}
	
	/**
	 * This method can be used to update triple objects within the entity model
	 * 
	 * @param predicateString	- predicate uri string of predicate which should be changed
	 * @param oldObject			- corresponding object which should be changed
	 * @param newObject			- new object
	 */
	public void updateTripleObject(final String predicateString, final RDFNode oldObject, final RDFNode newObject) {
		this.updateTripleObject(this.entityModel.createProperty(predicateString), oldObject, newObject);
	}

	
	/**
	 * This method can be used to update triple objects within the entity model
	 * 
	 * @param predicateString	-  predicate which should be changed
	 * @param oldObject			- corresponding object which should be changed
	 * @param newObject			- new object
	 */
	public void updateTripleObject(final Property predicate, final RDFNode oldObject, final RDFNode newObject) {
		if (null == predicate || null == oldObject || null == newObject) {
			return;
		}
		
		// create models
		Model updateModelOld = ModelFactory.createDefaultModel();
		Model updateModelNew = ModelFactory.createDefaultModel();
		
		// create tripples
		updateModelOld.add(this.mainSubjectUriObject, predicate, oldObject);
		updateModelNew.add(this.mainSubjectUriObject, predicate, newObject);

		// create bye output stream
		OutputStream outputStreamOld = new ByteArrayOutputStream();
		OutputStream outputStreamNew = new ByteArrayOutputStream();
		
		// write to output stream
		RDFDataMgr.write(outputStreamOld, updateModelOld, RDFFormat.NT);		
		RDFDataMgr.write(outputStreamNew, updateModelNew, RDFFormat.NT);
		
		// get actual triple strings
		String oldTriples = outputStreamOld.toString().replaceAll("\\n", "");
		String newTriples = outputStreamNew.toString().replaceAll("\\n", "");


		// create update query
		StringBuilder builder = new StringBuilder();		
		builder.append("DELETE { ").append(oldTriples).append(" }\n");
		builder.append("INSERT { ").append(newTriples).append(" }\n");
		builder.append("WHERE { ").append(oldTriples).append(" }");		
		
		// update within main model
		UpdateAction.parseExecute(builder.toString(), this.entityModel);
	}
	
	/**
	 * This can be used to store sub-entities (e.g. blank nodes)
	 * of this entity
	 * 
	 * @param id		- id which can be used to identify this entity
	 * @param entity	- sub entity
	 */
	public void addSubEntity(final Entity entity) {
		if (null == entity) {
			return;
		}
		
		if (null == this.subEntities) {
			this.subEntities = new LinkedHashMap<>();
			this.subEntitiesCategoryMap = new HashMap<>();
		}
		
		String uri = entity.getSubjectUri();
		this.subEntities.put(uri, entity);
		
		// only follow that scheme for blank nodes
		if (entity.isBlankNode) {
			// get category of blank node
			String categoryName = Entity.getBlankNodeCategory(uri);
			
			if (false == subEntitiesCategoryMap.containsKey(categoryName)) {
				this.subEntitiesCategoryMap.put(categoryName, entity);
			}
		}
	}
	
	public Collection<Entity> getSubEntities() {
		if (null == this.subEntities) {
			return Collections.emptyList();
		} else {
			return Collections.unmodifiableCollection(this.subEntities.values());
		}
	}
	
	/**
	 * Get an sub-entity which is associated with this entity
	 * 
	 * @param category - category of this blank node
	 * @param id	   - id given at conversion time
	 * @return sub entity or null when it is unknown
	 */
	public Entity getSubEntityById(final String id) {
		if (null == id ||
			null == this.subEntities || this.subEntities.isEmpty()) {
			return null;
		}
		
		Entity subEntity = this.subEntities.get(id);
		if (null == subEntity) {
			return null;
		}
		
		return subEntity;
	}
	
	/**
	 * This method can be used to get blank nodes which represent a certain category
	 * 
	 * @param category
	 * @return
	 */
	public Entity getSubEntityByCategory(final String category) {
		if (null == category || null == this.subEntitiesCategoryMap ||
			this.subEntitiesCategoryMap.isEmpty()) {
			return null;
		}
		
		return this.subEntitiesCategoryMap.get(category);
	}
	
	/**
	 * 
	 * @return true if sub-entities are registered
	 */
	public boolean hasSubEntity() {
		return null != this.subEntities && false == this.subEntities.isEmpty();
	}
	
	/**
	 * This method can be used to retrieve the objects which are associated with the main subject uri
	 * 
	 * @param predicateString
	 * @return
	 */
	public List<RDFNode> getRdfObjects(final String predicateString) {
		if (null == predicateString || null == this.mainSubjectUri) {
			return Collections.emptyList();
		}
		
		Property predicate = new PropertyImpl(predicateString);
		NodeIterator objectList = this.entityModel.listObjectsOfProperty(predicate);
		
		List<RDFNode> objects = new ArrayList<>();
		while (objectList.hasNext()) {
			RDFNode object = objectList.next();
			
			objects.add(object);
		}
		
		return objects;
	}
	
	/**
	 * This method can be used to remove
	 * all the information which are stored for
	 * a given predicate URI
	 * 
	 * @param predicateUri
	 */
	public void deleteProperty(final String predicateUri) {
		if (null == predicateUri) {
			return;
		}
		
		Property predicate = new PropertyImpl(predicateUri);
		this.entityModel.removeAll(null, predicate, null);
	}
	
	public void deleteSubEntity(final Entity entity) {
		if (null == entity || null == this.subEntities || this.subEntities.isEmpty()) {
			return;
		}
		
		String entityUri = entity.getSubjectUri();
		if (null == entityUri) {
			return;
		}
		
		if (this.subEntities.containsKey(entityUri)) {
			this.subEntities.remove(entityUri);
			
			if (entity.isBlankNode) {
				String category = Entity.getBlankNodeCategory(entityUri);
				this.subEntitiesCategoryMap.remove(category);
			}
		}
		
		entity.reset();
	}
	
	/**
	 * 
	 * @return All registered predicate uris for this entity
	 * (sub-entities have to be queried separately)
	 */
	public Collection<String> getProperties() {
		return this.getPredicates();
	}
	
	public String toStringWithCount(ByteArrayOutputStream outputStream) {
		if (null == outputStream) {
			outputStream = new ByteArrayOutputStream();
		}
		
		// write output into output stream
		RDFDataMgr.write(outputStream, this.entityModel, RDFFormat.NT);		
		StringBuilder builder = new StringBuilder();
		builder.append(outputStream.toByteArray());		
						
		if (null != this.subEntities) {
			for (String entityId : this.subEntities.keySet()) {
				Entity entity = this.subEntities.get(entityId);			
				builder.append(entity.toStringWithCount(outputStream));
			}
		}
		
		return new String(outputStream.toByteArray(), Charset.defaultCharset());
	}
	
	@Override
	public String toString() {
		return this.toStringWithCount(null);
	}
	
	/**
	 * This method can be used to normalize property values
	 * 
	 * @param predicateUri	- specifies the uri
	 * @param normalizer	- normalizer which can be used to normalize a property value
	 */
	public void normalizePropertyValue(final Pattern predicatePattern, EntityPropertyNormalizer normalizer) {
		if (null == predicatePattern || null == normalizer) {
			return;
		}
		
		throw new RuntimeException("Why did we call this?");
//		
//		for (String predicateUri : this.entityInformation.keySet()) {
//			if (predicatePattern.matcher(predicateUri).matches()) {
//				Map<RdfObject, AtomicInteger> rdfObjects = this.entityInformation.get(predicateUri);
//				if (null == rdfObjects || rdfObjects.isEmpty()) {
//					continue;
//				}
//				
//				// execute normalizer
//				normalizer.normalize(rdfObjects.keySet(), this.mainSubjectUri, predicateUri);
//			}
//		}
	}
	
	/**
	 * This class can be used to normalize property values
	 * @author kay
	 *
	 */
	static public abstract class EntityPropertyNormalizer {
		
		/// TODO km : Think of better way
		/**
		 *
		 * This method can be used to normalize property values
		 * of an entity
		 * 
		 * @param rdfObjects
		 * @param subjectUri
		 * @param predicateUri
		 */
		protected abstract void normalize(final Collection<RDFNode> rdfObjects,
						final String subjectUri, final String predicateUri);
	}
	
	/**
	 * This method can be used to obtain the category of a blank node uri
	 * 
	 * @param blankNodeUri
	 * @return category or null (on error)
	 */
	public static String getBlankNodeCategory(final String blankNodeUri) {
		if (null == blankNodeUri) {
			return null;
		}
		
		String category;
		if (blankNodeUri.startsWith("_")) {
			int end = blankNodeUri.indexOf("_", 2);
			category = blankNodeUri.substring(2, end);
		} else {
			int end = blankNodeUri.indexOf("_");
			category = blankNodeUri.substring(0, end);
		}
		
		return category;
	}

	/**
	 * This method can be used to obtain the id of a blank node uri
	 * 
	 * @param blankNodeUri
	 * @return category or null (on error)
	 */

	public static String getBlankNodeId(final String blankNodeUri) {
		if (null == blankNodeUri) {
			return null;
		}
		
		int end = blankNodeUri.indexOf("_", 2);
		int start = end + 1;
		int end2 = blankNodeUri.indexOf("_", end + 1);
		if (-1 == end2) {
			end2 = blankNodeUri.length();
		}
		
		String id = blankNodeUri.substring(start, end2);
		return id;
	}

}
