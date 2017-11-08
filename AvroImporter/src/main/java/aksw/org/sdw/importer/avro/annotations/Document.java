package aksw.org.sdw.importer.avro.annotations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import aksw.org.sdw.importer.avro.annotations.ids.EntityIdGenerator;

public class Document extends Annotation {
	
	public enum DocTypes {TWITTER_JSON, RSS_XML, NEWS_HTML}
	public DocTypes docType;
	
	public String uri;	
	public String signature;

	public String title;
	
	public Set<RelationMention> relationMentions = new LinkedHashSet<>();
	public Set<Mention> conceptMentions = new LinkedHashSet<>();
	
	/** count of normalized entity labels */
	private final Map<String, AtomicInteger> entityLabels = new HashMap<>();	
	/** count of relationships labels (normalized) */
	private final Map<String, AtomicInteger> relationLabels = new HashMap<>();
	
	public final EntityIdGenerator entityIdGenerator;
	
//	public List<de.dfki.lt.tap.Token> tokens;
//	public List<de.dfki.lt.tap.Sentence> sentences;
//	public List<de.dfki.lt.tap.CorefChain> corefChains;

//	
//	public List<de.dfki.lt.tap.Annotation> annotations;
//	public List<de.dfki.lt.tap.Provenance> provenance;
	public Map<String, String> refids;

//	public Map<java.lang.String,java.lang.String> attributes;
	
	/** namespace prefix which will be used to create URIs for all the entities */
	public String uriNamespace;
	
	public Document(final String uriNamespace) {
		this.uriNamespace = uriNamespace;
		this.entityIdGenerator = new EntityIdGenerator(uriNamespace);

		// add ids to the document as well, in case it is required later on
		this.entityIdGenerator.addUniqueId(this);
	}
}
