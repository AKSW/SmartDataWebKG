package aksw.org.sdw.importer.avro.annotations.maps;

import aksw.org.sdw.importer.avro.annotations.nif.RelationGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public interface MapHandlerInterface {

    final Map<String, Function<String, Integer>> functionMap = new HashMap<>();

}
