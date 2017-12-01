package aksw.org.sdw.importer.avro.annotations.maps;

import aksw.org.sdw.importer.avro.annotations.nif.RelationGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MapHandler {

    public final static Map<String, Function<String, String>> functionMap = new HashMap<>();

    static  {
        functionMap.put("customer-type", s -> mapCustomerType(s));
        functionMap.put("disaster-type", s -> mapDisaster(s));
        functionMap.put("facility-type", s -> mapFacilityType(s));
        functionMap.put("insolvency-cause", s -> mapInsolvencyCauseString(s));
        functionMap.put("org-position", s -> mapOrgPosition(s));
        functionMap.put("org-termination-type", s -> mapOrgTerminationType(s));
        functionMap.put("project-type", s -> mapProjectType(s));
        functionMap.put("provide-type", s -> mapProvideType(s));
        functionMap.put("trigger", s -> mapTrigger(s));
    }


    protected static String mapCustomerType(String lable) {
        return CustomerType.mappings.get(lable);
    }

    protected static String mapDisaster(String lable) {
        return Disaster.mappings.get(lable);
    }

    protected static String mapFacilityType(String lable) {
        return FacilityType.mappings.get(lable);
    }

    protected static String mapInsolvencyCauseString(String lable) {
        return InsolvencyCause.mappings.get(lable);
    }

    protected static String mapOrgPosition(String lable) {
        return OrgPosition.mappings.get(lable);
    }

    protected static String mapOrgTerminationType(String lable) {
        return OrgTerminationType.mappings.get(lable);
    }

    protected static String mapProjectType(String lable) {
        return ProjectType.mappings.get(lable);
    }

    protected static String mapProvideType(String lable) {
        return ProvideType.mappings.get(lable);
    }

    protected static String mapTrigger(String lable) {
        return Trigger.mappings.get(lable);
    }

    public static void main(String[] args) {
        System.out.println(MapHandler.functionMap.get("customer-type").apply("standard"));
        System.out.println(MapHandler.functionMap.get("disaster-type").apply("earthquake"));
        System.out.println(MapHandler.functionMap.get("facility-type").apply("factory"));
        System.out.println(MapHandler.functionMap.get("insolvency-cause").apply("liquidity problems"));
        System.out.println(MapHandler.functionMap.get("org-position").apply("Software Developer"));
        System.out.println(MapHandler.functionMap.get("org-termination-type").apply("acquisition"));
        System.out.println(MapHandler.functionMap.get("project-type").apply("standard"));
        System.out.println(MapHandler.functionMap.get("provide-type").apply("produce"));
        System.out.println(MapHandler.functionMap.get("trigger").apply("accounting adjustments"));
    }
}


