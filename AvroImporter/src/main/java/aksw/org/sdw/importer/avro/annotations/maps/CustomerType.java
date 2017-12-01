package aksw.org.sdw.importer.avro.annotations.maps;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CustomerType {

    public static final Map<String, String> mappings;
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("standard","http://corp.dbpedia.org/ontology/importance/Standard");
        map.put("main","http://corp.dbpedia.org/ontology/importance/Main");
        map.put("reference","http://corp.dbpedia.org/ontology/importance/Reference");
        mappings= Collections.unmodifiableMap(map);
    }
}
