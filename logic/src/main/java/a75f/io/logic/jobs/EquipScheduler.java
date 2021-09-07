package a75f.io.logic.jobs;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Schedule;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.tuners.TunerUtil;

public class EquipScheduler {

    private static final int SCHEDULER_PRIORITY = 12;


    private static final String TAG = "VAVScheduler";
    public static Occupied processEquip(Equip equip, Schedule equipSchedule, Schedule vacation, Occupancy systemOcc) {


        Log.i(TAG, "Equip: " + equip);
        Log.i(TAG, "Equip Schedule: " + equipSchedule);
        Occupied occ = equipSchedule.getCurrentValues();
        
        //When schedule is deleted
        if (occ == null) {
            ScheduleProcessJob.occupiedHashMap.remove(equip.getRoomRef());
            return null;
        }

        if(vacation != null)
            occ.setOccupied(false);

        occ.setVacation(vacation);
        occ.setSystemZone(true);

        double occuStatus = CCUHsApi.getInstance().readHisValByQuery("point and occupancy and mode and equipRef == \""+equip.getId()+"\"");
        double heatingDeadBand = TunerUtil.readTunerValByQuery("heating and deadband and base", equip.getId());
        double coolingDeadBand = TunerUtil.readTunerValByQuery("cooling and deadband and base", equip.getId());
        double setback = TunerUtil.readTunerValByQuery("unoccupied and setback", equip.getId());
        
        occ.setHeatingDeadBand(heatingDeadBand);
        occ.setCoolingDeadBand(coolingDeadBand);
        occ.setUnoccupiedZoneSetback(setback);
        Occupancy curOccupancy = Occupancy.values()[(int)occuStatus];
        if(curOccupancy == Occupancy.PRECONDITIONING)
            occ.setPreconditioning(true);
        else if(curOccupancy == Occupancy.FORCEDOCCUPIED)
            occ.setForcedOccupied(true);
        if (occ != null && ScheduleProcessJob.putOccupiedModeCache(equip.getRoomRef(), occ)) {
            double avgTemp = (occ.getCoolingVal()+occ.getHeatingVal())/2.0;
            double deadbands = (occ.getCoolingVal() - occ.getHeatingVal()) / 2.0 ;
            occ.setCoolingDeadBand(deadbands);
            occ.setHeatingDeadBand(deadbands);
            Double coolingTemp = (occ.isOccupied() || occ.isPreconditioning()/* || occ.isForcedOccupied() */|| (systemOcc == Occupancy.PRECONDITIONING) /*|| (systemOcc == Occupancy.FORCEDOCCUPIED)*/) ? occ.getCoolingVal() : (occ.getCoolingVal() + occ.getUnoccupiedZoneSetback());

            setDesiredTemp(equip, coolingTemp, "cooling",occ.isForcedOccupied() || systemOcc == Occupancy.FORCEDOCCUPIED);

            Double heatingTemp = (occ.isOccupied() || occ.isPreconditioning() /*|| occ.isForcedOccupied()*/ || (systemOcc == Occupancy.PRECONDITIONING) /*|| (systemOcc == Occupancy.FORCEDOCCUPIED)*/) ? occ.getHeatingVal() : (occ.getHeatingVal() - occ.getUnoccupiedZoneSetback());
            setDesiredTemp(equip, heatingTemp, "heating",occ.isForcedOccupied() || systemOcc == Occupancy.FORCEDOCCUPIED);
            setDesiredTemp(equip, avgTemp, "average",occ.isForcedOccupied() || systemOcc == Occupancy.FORCEDOCCUPIED);
        }

        return occ;
    }


    public static void setDesiredTemp(Equip equip, Double desiredTemp, String flag,boolean isForcedOccupied) {
        CcuLog.d(L.TAG_CCU_SCHEDULER, "ZoneSchedule Equip: " + equip.getDisplayName() + " Temp: " + desiredTemp + " Flag: " + flag+","+isForcedOccupied);
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and " + flag + " and desired and sp and equipRef == \"" + equip.getId() + "\"");
        if (points == null || points.size() == 0) {
            return; //Equip might have been deleted.
        }
        final String id = ((HashMap) points.get(0)).get("id").toString();
        if(isForcedOccupied)
            return;
        if (HSUtil.getPriorityLevelVal(id,8) == desiredTemp) {
            CcuLog.d(L.TAG_CCU_SCHEDULER, flag+"DesiredTemp not changed : Skip PointWrite");
            return;
        }
        
        CCUHsApi.getInstance().pointWrite(HRef.make(id.replace("@","")), 8, "Scheduler", desiredTemp != null ? HNum.make(desiredTemp) : HNum.make(0), HNum.make(0));
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                CCUHsApi.getInstance().writeHisValById(id, HSUtil.getPriorityVal(id));
            }
        },100);
    }
    
    
    
}
