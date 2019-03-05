package a75f.io.logic.jobs;

import android.util.Log;

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

    private static final int SCHEDULER_PRIORITY = 12;


    private static final String TAG = "VAVScheduler";
    boolean occupied; // determined by schedule
    public static int heatingDeadBand = 5;
    public static int coolingDeadBand = 5;


    public static Occupied processEquip(Equip equip, Schedule equipSchedule) {


        Log.i(TAG, "Equip: " + equip);
        Log.i(TAG, "Equip Schedule: " + equipSchedule);
        Occupied occ = equipSchedule.getCurrentValues();


        if (occ != null && ScheduleProcessJob.putOccupiedModeCache(equip.getRoomRef(), occ)) {


            Double coolingTemp = occ.isOccupied() ? (double) occ.getCoolingVal() : ((double) occ.getCoolingVal() + coolingDeadBand);
            setDesiredTemp(equip, coolingTemp, "cooling");

            Double heatingTemp = occ.isOccupied() ? (double) occ.getHeatingVal() : ((double) occ.getHeatingVal() - heatingDeadBand);
            setDesiredTemp(equip, heatingTemp, "heating");
        }

        return occ;
    }


    public static void setDesiredTemp(Equip equip, Double desiredTemp, String flag) {
        CcuLog.d(L.TAG_CCU_SCHEDULER, "Equip: " + equip.getId() + " Temp: " + desiredTemp + " Flag: " + flag);
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and " + flag + " and desired and sp and equipRef == \"" + equip.getId() + "\"");
        if (points == null || points.size() == 0) {
            return; //Equip might have been deleted.
        }
        String id = ((HashMap) points.get(0)).get("id").toString();
        try {
            CCUHsApi.getInstance().pointWrite(HRef.make(id.replace("@","")), 9, "Scheduler", desiredTemp != null ? HNum.make(desiredTemp) : HNum.make(0), HNum.make(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
        CCUHsApi.getInstance().writeHisValById(id, getPriorityVal(id));
    }
    
    public static double getPriorityVal(String id) {
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
}
