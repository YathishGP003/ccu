package a75f.io.logic.bo.building.dualduct;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;

public class DualDuctUtil {
    
    public static HashMap getEquipPointsForView(String equipID) {
        HashMap dualDuctPoints = new HashMap();
    
        dualDuctPoints.put("Profile","DAB Dual Duct");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \"" + equipID + "\"");
        double damperCoolingPos = CCUHsApi.getInstance().readHisValByQuery("point and zone and damper and " +
                                                                                "cooling and pos and equipRef == \""+equipID+"\"");
    
        double damperHeatingPos = CCUHsApi.getInstance().readHisValByQuery("point and zone and damper and " +
                                                                                "heating and pos and equipRef == \""+equipID+"\"");
        
        double dischargeAirflow = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge " +
                                                                          "and air and temp and equipRef == \""+equipID+"\"");
    
        double analog1Config = CCUHsApi.getInstance().readDefaultVal("point and zone and config and dualDuct and th1" +
                                                                      " and output and type and equipRef == \""+equipID+"\"");
        double analog2Config = CCUHsApi.getInstance().readDefaultVal("point and zone and config and dualDuct and th1 " +
                                                                   "and output and type and equipRef == \""+equipID+"\"");
        
        double th1Config = CCUHsApi.getInstance().readDefaultVal("point and zone and config and dualDuct and th1 and " +
                                                           "output and type and equipRef == \""+equipID+"\"");
        
        if (equipStatusPoint.length() > 0)
        {
            dualDuctPoints.put("Status",equipStatusPoint);
        }else{
            dualDuctPoints.put("Status","OFF");
        }
    
        if (th1Config == 0) {
            double coolingSupplyAirflow = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and " +
                                                                                   "cooling and supply and air and " +
                                                                                   "temp and equipRef == \""+equipID+
                                                                                   "\"");
            dualDuctPoints.put("CoolingSupplyAirflow",coolingSupplyAirflow+" \u2109");
        } else {
            double heatingSupplyAirflow = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and " +
                                                                                   "heating and supply and air and " +
                                                                                   "temp and equipRef == \""+equipID+
                                                                                   "\"");
            dualDuctPoints.put("CoolingSupplyAirflow",heatingSupplyAirflow+" \u2109");
        }
    
        dualDuctPoints.put("DischargeAirflow",dischargeAirflow+" \u2109");
    
        dualDuctPoints.put("CoolingDamper",damperCoolingPos+"% Open");
        
        dualDuctPoints.put("HeatingDamper",damperCoolingPos+"% Open");
        
        return dualDuctPoints;
    }
    
}
