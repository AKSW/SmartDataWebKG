package aksw.org.sdw.importer.avro.annotations;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GlobalConfig {


    private static GlobalConfig instance = null;

    private String nifid = null;
    private String wasAttributedTo = null;

    public static synchronized GlobalConfig getInstance() {
        if (instance == null) {
            instance = new GlobalConfig();
        }
        return instance;
    }

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
}
