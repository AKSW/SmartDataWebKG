package aksw.org.sdw.importer.avro.annotations;

import aksw.org.sdw.nel.Spotlight;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Statement;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GlobalConfig {


    private static GlobalConfig instance = null;

    private Model model = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
    private String nifid = null;
    private String wasAttributedTo = null;

    public static synchronized GlobalConfig getInstance() {
        if (instance == null) {
            instance = new GlobalConfig();
        }
        return instance;
    }

    public Spotlight spotlight;
    private LoadingCache<String, String> linkCache;

    public String getNifid() {
        return nifid;
    }

    public void setNifid(String nifid) {
        this.nifid = nifid;
    }

    public String getWasAttributedTo() {
        return wasAttributedTo;
    }

    public void setWasAttributedTo(String str) {
        this.wasAttributedTo = str;
    }
/*
        nif:anchorOf "Beacon Capital Management" ;
		nif:beginIndex "1858"^^xsd:nonNegativeInteger ;
		nif:endIndex "1883"^^xsd:nonNegativeInteger ;
		nif:referenceContext <http://www.watchlistnews.com/on-semiconductor-corp-on-cfo-bernard-gutmann-sells-30000-shares-of-stock/582309.html/#offset_0_4865> ;
		:taAnnotatorsRef <http://corp.dbpedia.org/annotator#Stanford%20CoreNLP%203.7.0> ;
		:taClassRef org:Organization , dbo:Organisation ;
		:taConfidence "1.0"^^xsd:double ;
		:taIdentRef
 */
    public String makeNifHash(Mention mention, Document document) {
        String hashsum = "";
        try {

            hashsum += String.format("%064x", new java.math.BigInteger(1, MessageDigest.getInstance("SHA-256").digest(mention.textNormalized.getBytes())));
            hashsum += String.format("%064x", new java.math.BigInteger(1, MessageDigest.getInstance("SHA-256").digest(String.valueOf(mention.span.start).getBytes())));
            hashsum += String.format("%064x", new java.math.BigInteger(1, MessageDigest.getInstance("SHA-256").digest(String.valueOf(mention.span.end).getBytes())));
            hashsum += String.format("%064x", new java.math.BigInteger(1, MessageDigest.getInstance("SHA-256").digest(document.uri.getBytes())));
            for( Provenance provenance : mention.provenanceSet) {
                if( null != provenance.annotator)
                hashsum += String.format("%064x", new java.math.BigInteger(1, MessageDigest.getInstance("SHA-256").digest(provenance.annotator.getBytes())));
                if( -1.0f != provenance.score)
                    hashsum += String.format("%064x", new java.math.BigInteger(1, MessageDigest.getInstance("SHA-256").digest(String.valueOf(provenance.score).getBytes())));
            }
            for( String type : mention.types ) {
                if( null != type )
                    hashsum += String.format("%064x", new java.math.BigInteger(1, MessageDigest.getInstance("SHA-256").digest(type.getBytes())));
            }
            hashsum = String.format("%064x", new java.math.BigInteger(1, MessageDigest.getInstance("SHA-256").digest(hashsum.getBytes())));
        } catch (Exception e) {
            e.printStackTrace();
        }
//        System.out.println("anchor "+mention.textNormalized+" hashsum "+hashsum);
        return hashsum;
    }
//    public String makeNifHash(Set<Provenance> provenanceSet, String docid) {
//        Iterator<Provenance> provenanceIt = provenanceSet.iterator();
//        String hashed = "";
//        try {
//            while (provenanceIt.hasNext()) {
//                Provenance provenance = provenanceIt.next();
//                hashed += String.format("%064x", new java.math.BigInteger(1, MessageDigest.getInstance("SHA-256").digest(provenance.toString().getBytes())));
//            }
//            hashed += String.format("%064x", new java.math.BigInteger(1, MessageDigest.getInstance("SHA-256").digest(docid.getBytes())));
//        } catch ( Exception e ) {
//            e.printStackTrace();
//        }
//        return hashed;
//    }

//    public String globalFactHash(String subject, String predicate, RDFNode object) {
//
//        String value;
//        String lada = "";
//
//        String subjHash = sha256Hash(subject);
//        String predHash = sha256Hash(predicate);
//
//        if( object.isURIResource() ) {
//            value = object.asResource().getURI();
//        } else {
//            value = object.asLiteral().getLexicalForm();
//            lada = object.asLiteral().getLanguage();
//            if( "".equals(lada) ) lada = object.asLiteral().getDatatypeURI();
//        }
//
//        String objHash = sha256Hash(value);
//        String ladaHash = sha256Hash(lada);
//
//        //  SCALA : sha256Hash(List(subject,predicate,value,lada).flatMap(Option(_)).mkString(","))
//        List<String> list = Arrays.asList(subjHash,predHash,objHash,ladaHash);
//        return sha256Hash(String.join(",",list));
//    }
//
//    public String sha256Hash(String text) {
//        String ret = null;
//        try {
//            ret = String.format("%064x", new java.math.BigInteger(1, MessageDigest.getInstance("SHA-256").digest(text.getBytes())));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return ret;
//    }

    public String link(String label) {
        if(spotlight != null) {
            return linkCache.getUnchecked(label);
        } else return "";
    }

    public String smrDir = null;

    public String getSmrDir() {
        return smrDir;
    }

    public void setSmrDir(String changeto) {
        smrDir = changeto;
    }

    public void setSpotlight(String url) {
        spotlight = new Spotlight(url);
        linkCache = CacheBuilder.newBuilder()
                .maximumSize(50000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<String, String>() {
                            public String load(String key) {
                                return spotlight.getLink(key);
                            }
                        });
    }

    public void addModel(Model m) {
        model.add(m);
    }

    public void resetModel() {
        model = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
    }

    public Model getModel() {
        return model;
    }
}
