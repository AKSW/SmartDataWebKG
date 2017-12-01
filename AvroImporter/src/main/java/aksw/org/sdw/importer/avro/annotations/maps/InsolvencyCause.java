package aksw.org.sdw.importer.avro.annotations.maps;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InsolvencyCause {

    public static final Map<String, String> mappings;
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("liquidity problems","http://corp.dbpedia.org/ontology/insolvency-cause/LiquidityProblems");
        map.put("cash flow crisis","http://corp.dbpedia.org/ontology/insolvency-cause/CashFlowCrisis");
        map.put("economic crisis","http://corp.dbpedia.org/ontology/insolvency-cause/EconomicCrisis");
        map.put("industry crisis","http://corp.dbpedia.org/ontology/insolvency-cause/IndustryCrisis");
        map.put("industry sector crisis","http://corp.dbpedia.org/ontology/insolvency-cause/IndustrySectorCrisis");
        map.put("fiscal problems","http://corp.dbpedia.org/ontology/insolvency-cause/FiscalProblems");
        map.put("financial mismanagement","http://corp.dbpedia.org/ontology/insolvency-cause/FinancialMismanagement");
        map.put("management failure","http://corp.dbpedia.org/ontology/insolvency-cause/ManagementFailure");
        map.put("overextension of credit","http://corp.dbpedia.org/ontology/insolvency-cause/OverextensionOfCredit");
        map.put("credit problems","http://corp.dbpedia.org/ontology/insolvency-cause/CreditProblems");
        map.put("lack of capital","http://corp.dbpedia.org/ontology/insolvency-cause/LackOfCapital");
        map.put("lack of funds","http://corp.dbpedia.org/ontology/insolvency-cause/LackOfFunds");
        map.put("loss of market","http://corp.dbpedia.org/ontology/insolvency-cause/LossOfMarket");
        map.put("loss of business","http://corp.dbpedia.org/ontology/insolvency-cause/LossOfBusiness");
        map.put("excessive overheads","http://corp.dbpedia.org/ontology/insolvency-cause/ExcessiveOverheads");
        mappings= Collections.unmodifiableMap(map);
    }
}
