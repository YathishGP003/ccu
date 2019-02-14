package a75f.io.logic.jobs;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Schedule;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class VAVScheduler {


    private static final String TAG = "VAVScheduler";
    boolean occupied; // determined by schedule
    private static int heatingDeadBand = 5;
    private static int coolingDeadBand = 5;


    public static void processEquip(Equip equip, Schedule equipSchedule) {
    
        CcuLog.d(L.TAG_CCU_SCHEDULER,"Equip: " + equip);
        CcuLog.d(L.TAG_CCU_SCHEDULER, "Equip Schedule: " + equipSchedule);
        Occupied cooling = equipSchedule.getCurrentValueForMarker("cooling");
        Occupied heating = equipSchedule.getCurrentValueForMarker("heating");

        if (cooling != null) {
            Double coolingTemp = cooling.isOccupied() ? (double) cooling.getValue() : ((double) cooling.getValue() + coolingDeadBand);
            setDesiredTemp(equip, coolingTemp, "cooling");
        }
        if (heating != null) {
            Double heatingTemp = heating.isOccupied() ? (double) heating.getValue() : ((double) heating.getValue() - heatingDeadBand);
            setDesiredTemp(equip, heatingTemp, "heating");
        }

    }


    public static void setDesiredTemp(Equip equip, Double desiredTemp, String flag) {
        CcuLog.d(L.TAG_CCU_SCHEDULER, "Equip: " + equip.getId() + " Temp: " + desiredTemp + " Flag: " + flag);
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and " + flag + " and desired and sp and equipRef == \"" + equip.getId() + "\"");
        if (points == null || points.size() == 0) {
            return; //Equip might have been deleted.
        }
        String id = ((HashMap) points.get(0)).get("id").toString();
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
        try {
            CCUHsApi.getInstance().pointWrite(HRef.make(id.replace("@","")), 9, "Scheduler", desiredTemp != null ? HNum.make(desiredTemp) : HNum.make(0), HNum.make(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
