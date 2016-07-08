package aksw.org.kg.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

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
	protected Map<String, Map<RdfObject, AtomicInteger>> entityInformation = new LinkedHashMap<>();
	
	/** container which can be used to store sub entity information */
	protected Map<String, Entity> subEntities = null;
	
	protected Map<String, Entity> subEntitiesCategoryMap = null;
	
	/** counts the number of triples which are stored in this entity */
	protected long tripleCount = 0L;
	
	/** specifies the actual subject uri */
	protected String mainSubjectUri = null;
	
	/** main uri object for reuse */
	protected RdfObjectUri mainSubjectUriObject = null;
	
	/** specifies whether this is a blank node */
	protected final boolean isBlankNode;
	
	/** uri prefix */
	final String uriPrefix;
	
	/**
	 * Constructor which can be used to
	 * specify whether this is a blank node
	 * 
	 * @param isBlankNode
	 */
	public Entity(final String uriPrefix, final boolean isBlankNode) {
		this.isBlankNode = isBlankNode;
		this.uriPrefix = uriPrefix;
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
		
		for (String key : entityInformation.keySet()) {
			entityInformation.get(key).clear();
		}
		this.entityInformation.clear();
		this.tripleCount = 0L;
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
	}
	
	/**
	 * 
	 * @return true if data is stored and false otherwise
	 */
	public boolean isEmpty() {
		boolean isEmpty = this.entityInformation.isEmpty();
		
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
		String cleanName = cleanupPattern.matcher(name).replaceAll(" ").trim().toLowerCase();
		cleanName = cleanupWhiteSpaces.matcher(cleanName).replaceAll("_");
		
		String rest = uri.substring(prefixLength, startName + 1);
		String cleanedSubjectUri = new StringBuffer().append(uriPrefix).append(rest).append(cleanName).toString();
		
		return cleanedSubjectUri;
	}
	
	public Collection<String> getPredicates() {
		return Collections.unmodifiableCollection(this.entityInformation.keySet());
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
	
	public RdfObjectUri getSubjectUriObject() {
		if (null == this.mainSubjectUriObject && null != this.mainSubjectUri) {
			this.mainSubjectUriObject = new RdfObjectUri(this.mainSubjectUri);
		}
		
		return this.mainSubjectUriObject;
	}
	
	public long getTripleCount() {
		long totalTripleCount = this.tripleCount;
		
		// just check if the other sub-entities have queries as well
		if (null != this.subEntities) {
			for (Entity entity : this.subEntities.values()) {
				totalTripleCount += entity.getTripleCount();
			}
		}
		
		return totalTripleCount;
	}
	
	/**
	 * This method can be used to add new triple information
	 * 
	 * @param subjectUri
	 * @param predicate
	 * @param object
	 */
	public void addTriple(final String predicate, final RdfObject object) {
		if (null == predicate || null == object) {
			return;
		}
		
		Map<RdfObject, AtomicInteger> rdfObjects = this.entityInformation.get(predicate);
		if (null == rdfObjects) {
			rdfObjects = new HashMap<>();
			this.entityInformation.put(predicate, rdfObjects);
		}
		
		if (false == rdfObjects.containsKey(object)) {
			rdfObjects.put(object, new AtomicInteger(1));
			++this.tripleCount;
		} else {
			// if we know it already --> add count
			rdfObjects.get(object).incrementAndGet();
		}
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
	public List<RdfObject> getRdfObjects(final String predicateString) {
		if (null == this.mainSubjectUri) {
			return Collections.emptyList();
		}
		
		Map<RdfObject, AtomicInteger> rdfObjects = this.entityInformation.get(predicateString);
		if (null == rdfObjects) {
			return Collections.emptyList();
		} else {
			return new ArrayList<>(rdfObjects.keySet());
		}
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
		
		Map<RdfObject, AtomicInteger> objects = this.entityInformation.get(predicateUri);
		if (null != objects) {
			objects.clear();
		}
		
		this.entityInformation.remove(predicateUri);
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
		return Collections.unmodifiableCollection(this.entityInformation.keySet());
	}
	
	public String toStringWithCount(final boolean withCount) {
		StringBuilder builder = new StringBuilder();
		
		for (String predicateString : this.entityInformation.keySet()) {

			Map<RdfObject, AtomicInteger> objects =  this.entityInformation.get(predicateString);
			if (null == objects || objects.isEmpty()) {
				continue;
			}
			
			for (RdfObject object : objects.keySet()) {
				
				if (this.isBlankNode) {
					// correct URI and remove all spaces
					builder.append(this.mainSubjectUri);
				} else {
					// correct URI and remove all spaces
					builder.append("<");
					builder.append(this.mainSubjectUri);
					builder.append(">");
				}
				
				builder.append(" <");
				builder.append(cleanupWhiteSpaces.matcher(predicateString).replaceAll("_"));
				builder.append("> ");				

				builder.append(object);
				if (withCount) {
					builder.append(" {");
					builder.append(objects.get(object).get());
					builder.append("} ");
				}
				
				builder.append(" .\n");
			}
		}
		
		if (null != this.subEntities) {
			for (String entityId : this.subEntities.keySet()) {
				Entity entity = this.subEntities.get(entityId);			
				builder.append(entity.toString());
			}
		}
		
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return this.toStringWithCount(false);
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
		
		for (String predicateUri : this.entityInformation.keySet()) {
			if (predicatePattern.matcher(predicateUri).matches()) {
				Map<RdfObject, AtomicInteger> rdfObjects = this.entityInformation.get(predicateUri);
				if (null == rdfObjects || rdfObjects.isEmpty()) {
					continue;
				}
				
				// execute normalizer
				normalizer.normalize(rdfObjects.keySet(), this.mainSubjectUri, predicateUri);
			}
		}
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
		protected abstract void normalize(final Collection<RdfObject> rdfObjects,
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
