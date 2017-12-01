package aksw.org.sdw.importer.avro.annotations.maps;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FacilityType {

    public static final Map<String, String> mappings;
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("factory","http://corp.dbpedia.org/ontology/facility/Factory");
        map.put("office","http://corp.dbpedia.org/ontology/facility/Office");
        map.put("office building","http://corp.dbpedia.org/ontology/facility/OfficeBuilding");
        map.put("warehouse","http://corp.dbpedia.org/ontology/facility/Warehouse");
        map.put("outlet","http://corp.dbpedia.org/ontology/facility/Outlet");
        map.put("logistics center","http://corp.dbpedia.org/ontology/facility/LogisticsCenter");
        map.put("plant","http://corp.dbpedia.org/ontology/facility/Plant");
        map.put("distribution center","http://corp.dbpedia.org/ontology/facility/DistributionCenter");
        map.put("airport","http://corp.dbpedia.org/ontology/facility/Airport");
        map.put("airbase","http://corp.dbpedia.org/ontology/facility/Airbase");
        map.put("train station","http://corp.dbpedia.org/ontology/facility/TrainStation");
        map.put("bus station","http://corp.dbpedia.org/ontology/facility/BusStation");
        map.put("refinery","http://corp.dbpedia.org/ontology/facility/Refinery");
        map.put("smelter","http://corp.dbpedia.org/ontology/facility/Smelter");
        map.put("military base","http://corp.dbpedia.org/ontology/facility/MilitaryBase");
        map.put("naval base","http://corp.dbpedia.org/ontology/facility/NavalBase");
        map.put("port","http://corp.dbpedia.org/ontology/facility/Port");
        map.put("hotel","http://corp.dbpedia.org/ontology/facility/Hotel");
        map.put("motel","http://corp.dbpedia.org/ontology/facility/Motel");
        map.put("power plant","http://corp.dbpedia.org/ontology/facility/PowerPlant");
        map.put("steel mill","http://corp.dbpedia.org/ontology/facility/SteelMill");
        map.put("bank","http://corp.dbpedia.org/ontology/facility/Bank");
        map.put("hospital","http://corp.dbpedia.org/ontology/facility/Hospital");
        mappings= Collections.unmodifiableMap(map);
    }

}
