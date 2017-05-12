package aksw.org.sdw.importer.avro.annotations;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.TreeMap;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import aksw.org.sdw.importer.avro.annotations.Mention;
//import aksw.org.sdw.importer.avro.annotations.RelationMention;
//
///**
// * This class can be used to store relation findings 
// * 
// * @author kay
// *
// */
//public class RelationFindings {
//	
//	/** string representation of relation type */
//	public String relationType;
//	
//	/** relation mentions */
//	public List<RelationMention> relationTypeMentions = new ArrayList<>();
//	
//	/** count of normalized entity labels */
//	private final Map<String, AtomicInteger> entityLabels = new HashMap<>();
//	
//	/** count of relationships labels (normalized) */
//	private final Map<String, AtomicInteger> relationLabels = new HashMap<>();
//	
//	/**
//	 * Returns statistics about found entities and relations
//	 * @return
//	 */
//	public String getStatsPrintout() {
//		StringBuilder builder = new StringBuilder();
//		
//		builder.append("mention count: " + relationTypeMentions.size() + ", ");
//		builder.append("unique entity mention count: " + entityLabels.size() + ", ");
//		builder.append("unique relation mention count: " + relationLabels.size() + ", ");
//		
//		return builder.toString();
//	}
//	
//	/**
//	 * 
//	 * @return Entity Labels and their count ordered by number of occurrence
//	 */
//	public Map<String, AtomicInteger> getEntityLabels() {
//		Map<String, AtomicInteger> sortedMap = new TreeMap<>(new Comparator<String>() {
//
//			@Override
//			public int compare(String o1, String o2) {
//				AtomicInteger countO1 = entityLabels.get(o1);
//				AtomicInteger countO2 = entityLabels.get(o2);
//
//				return countO2.get() - countO1.get();
//			}
//		});
//		
//		sortedMap.putAll(entityLabels);
//		return sortedMap;
//	}
//	
//	/**
//	 * 
//	 * @return Relationship labels and their count ordered by number of occurrence
//	 */
//	public Map<String, AtomicInteger> getRelationLabels() {
//		Map<String, AtomicInteger> sortedMap = new TreeMap<>(new Comparator<String>() {
//
//			@Override
//			public int compare(String o1, String o2) {
//				AtomicInteger countO1 = relationLabels.get(o1);
//				AtomicInteger countO2 = relationLabels.get(o2);
//
//				return countO2.get() - countO1.get();
//			}
//		});
//		
//		sortedMap.putAll(relationLabels);
//		return sortedMap;
//	}
//	
//	/**
//	 * 
//	 * @param mention Adding mention instance
//	 */
//	public void addRelationMention(final RelationMention mention) {
//		if (null == mention) {
//			return;
//		}
//		
//		// add mention
//		this.relationTypeMentions.add(mention);
//		
//		// go through all entities and count entity labels
//		for (Mention entity : mention.entities) {
//			
//			String typeAndLabel = entity.type + "/" + entity.textNormalized;
//			AtomicInteger entityLabelCount = entityLabels.get(typeAndLabel);
//			if (null == entityLabelCount) {
//				entityLabelCount = new AtomicInteger(0);
//				entityLabels.put(typeAndLabel, entityLabelCount);
//			}
//			
//			entityLabelCount.incrementAndGet();
//		}
//		
//		// go through all relations and count relationship labels
//		AtomicInteger relationLabelCount = relationLabels.get(mention.relation.textNormalized);
//		if (null == relationLabelCount) {
//			relationLabelCount = new AtomicInteger(0);
//			relationLabels.put(mention.relation.textNormalized, relationLabelCount);
//		}
//		
//		relationLabelCount.incrementAndGet();
//	}
//	
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + ((relationType == null) ? 0 : relationType.hashCode());
//		return result;
//	}
//	
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		RelationFindings other = (RelationFindings) obj;
//		if (relationType == null) {
//			if (other.relationType != null)
//				return false;
//		} else if (!relationType.equals(other.relationType))
//			return false;
//		return true;
//	}	
//}
//
