package aksw.org.sdw.importer.avro.annotations.maps;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ProvideType {

    public static final Map<String, String> mappings;
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("produce","http://corp.dbpedia.org/ontology/importance/Produce");
        map.put("distribute","http://corp.dbpedia.org/ontology/importance/Distribute");
        mappings= Collections.unmodifiableMap(map);
    }
}
