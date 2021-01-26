package a75f.io.logic.bo.util;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75.io.algos.CO2Loop;
import a75.io.algos.VOCLoop;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.jobs.ScheduleProcessJob;

public class TemperatureProfileUtil {
    
    public static double getDesiredTemp(int nodeAddr)
    {
        ArrayList
            points = CCUHsApi.getInstance().readAll("point and air and temp and desired and average and sp and group == \"" + nodeAddr + "\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        return CCUHsApi.getInstance().readDefaultValById(id);
    }
    public static void setDesiredTemp(int nodeAddr, double desiredTemp)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and average and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }
    
    public static double getDesiredTempCooling(int nodeAddr)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and cooling and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    public static void setDesiredTempCooling(int nodeAddr, double desiredTemp)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and cooling and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        //CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make(desiredTemp), HNum.make(0));
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }
    
    public static double getDesiredTempHeating(int nodeAddr)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and heating and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    
    public static void setDesiredTempHeating(int nodeAddr, double desiredTemp)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and heating and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        //CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make(desiredTemp), HNum.make(0));
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }
    
    public static double getDamperLimit(int nodeAddr, String coolHeat, String minMax)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and config and damper and pos and limit and "+coolHeat+
                                                          " and "+minMax+" and group == \""+nodeAddr+"\"");
        if (points.size() == 0) {
            return 0;
        }
        
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        return CCUHsApi.getInstance().readPointPriorityVal(id);
        //return CCUHsApi.getInstance().readDefaultValById(id);
    }
    public static void setDamperLimit(int nodeAddr, String coolHeat, String minMax, double val)
    {
        ArrayList points =
            CCUHsApi.getInstance().readAll("point and damper and pos and limit and "+coolHeat+" and "+minMax+" and " +
                                           "group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, val);
        CCUHsApi.getInstance().writeHisValById(id, val);
    }
    
    public static double getStatus(int nodeAddr) {
        return CCUHsApi.getInstance().readHisValByQuery("point and status and his and group == \""+nodeAddr+"\"");
    }
    
    public static void setStatus(int nodeAddr, double status, boolean emergency) {
        if (getStatus(nodeAddr) != status )
        {
            CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + nodeAddr + "\"", status);
        }
        
        String message;
        if (emergency) {
            message = (status == 0 ? "Recirculating Air" : status == 1 ? "Emergency Cooling" : "Emergency Heating");
        } else
        {
            if (ScheduleProcessJob.getSystemOccupancy() == Occupancy.PRECONDITIONING) {
                message = "In Preconditioning ";
            } else
            {
                message = (status == 0 ? "Recirculating Air" : status == 1 ? "Cooling Space" : "Warming Space");
            }
        }
        
        String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \""+nodeAddr+"\"");
        if (!curStatus.equals(message))
        {
            CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + nodeAddr + "\"", message);
        }
    }
    
    public static void setScheduleStatus(int nodeAddr, String status)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and scheduleStatus and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, status);
    }
    public static double getZonePriorityValue(int nodeAddr){
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+nodeAddr+"\"");
        return CCUHsApi.getInstance().readPointPriorityValByQuery("zone and priority and config and equipRef == \""+equip.get("id")+"\"");
    }
    
}
