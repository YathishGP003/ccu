package a75f.io.logic.bo.building.dualduct;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;

public class DualDuctUtil {
    
    public static HashMap getEquipPointsForView(String equipID) {
        HashMap dualDuctPoints = new HashMap();
    
        dualDuctPoints.put("Profile","DAB Dual Duct");
        String equipStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \"" + equipID + "\"");
        double damperCoolingPos = CCUHsApi.getInstance().readHisValByQuery("point and zone and damper and " +
                                                                                "cooling and cmd and equipRef == \""+equipID+"\"");
    
        double damperHeatingPos = CCUHsApi.getInstance().readHisValByQuery("point and zone and damper and " +
                                                                                "heating and cmd and equipRef == \""+equipID+"\"");
        
        double dischargeAirflow = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge " +
                                                                          "and air and temp and equipRef == \""+equipID+"\"");
    
        double analog1Config = CCUHsApi.getInstance().readDefaultVal("point and zone and config and dualDuct and th1" +
                                                                      " and output and type and equipRef == \""+equipID+"\"");
        double analog2Config = CCUHsApi.getInstance().readDefaultVal("point and zone and config and dualDuct and th1 " +
                                                                   "and output and type and equipRef == \""+equipID+"\"");
        
        double th2Config = CCUHsApi.getInstance().readDefaultVal("point and zone and config and dualDuct and th2 and " +
                                                           "output and type and equipRef == \""+equipID+"\"");
        
        if (equipStatus.length() > 0)
        {
            dualDuctPoints.put("Status",equipStatus);
        }else{
            dualDuctPoints.put("Status","OFF");
        }
    
        if (th2Config == DualDuctThermistorConfig.COOLING_AIRFLOW_TEMP.getVal()) {
            double coolingSupplyAirflow = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and " +
                                                                                   "cooling and supply and air and " +
                                                                                   "temp and equipRef == \""+equipID+ "\"");
            dualDuctPoints.put("CoolingSupplyAirflow",coolingSupplyAirflow+" \u2109");
        } else if (th2Config == DualDuctThermistorConfig.HEATING_AIRFLOW_TEMP.getVal()){
            double heatingSupplyAirflow = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and " +
                                                                                   "heating and supply and air and " +
                                                                                   "temp and equipRef == \""+equipID+ "\"");
            dualDuctPoints.put("HeatingSupplyAirflow",heatingSupplyAirflow+" \u2109");
        }
    
        dualDuctPoints.put("DischargeAirflow",dischargeAirflow+" \u2109");
    
        dualDuctPoints.put("CoolingDamper",damperCoolingPos+"% Open");
        
        dualDuctPoints.put("HeatingDamper",damperHeatingPos+"% Open");
        
        return dualDuctPoints;
    }
    
}
