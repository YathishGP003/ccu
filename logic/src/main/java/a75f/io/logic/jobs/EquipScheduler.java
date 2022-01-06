package a75f.io.logic.jobs;

import android.util.Log;

import org.joda.time.DateTime;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Point;
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

        double occupancyvalue = CCUHsApi.getInstance().readHisValByQuery("point and occupancy and" +
                " mode and equipRef == \"" + equip.getId() + "\"");
        
        occ.setHeatingDeadBand(heatingDeadBand);
        occ.setCoolingDeadBand(coolingDeadBand);
        occ.setUnoccupiedZoneSetback(setback);
        Occupancy curOccupancy = Occupancy.values()[(int)occuStatus];
        if(curOccupancy == Occupancy.PRECONDITIONING)
            occ.setPreconditioning(true);
        else if(curOccupancy == Occupancy.FORCEDOCCUPIED)
            occ.setForcedOccupied(true);
        if (occ != null && ScheduleProcessJob.putOccupiedModeCache(equip.getRoomRef(), occ)) {

            double avgTemp = (occ.getCoolingVal() + occ.getHeatingVal()) / 2.0;
            double deadbands = (occ.getCoolingVal() - occ.getHeatingVal()) / 2.0;
            occ.setCoolingDeadBand(deadbands);
            occ.setHeatingDeadBand(deadbands);
            Double heatingTemp;
            Double coolingTemp;
            if (equip.getMarkers().contains("bpos") && occupancyvalue == Occupancy.AUTOAWAY.ordinal()) {
                Log.d("BPOSProfile", "in bpos vav: ");
                handleAutoaway(equip,
                        occ.isForcedOccupied() || systemOcc == Occupancy.FORCEDOCCUPIED);
            }
            coolingTemp = (occ.isOccupied() || occ.isPreconditioning()/* || occ
                .isForcedOccupied() */ || (systemOcc == Occupancy.PRECONDITIONING) /*||
                (systemOcc == Occupancy.FORCEDOCCUPIED)*/) ? occ.getCoolingVal() :
                    (occ.getCoolingVal() + occ.getUnoccupiedZoneSetback());

            heatingTemp = (occ.isOccupied() || occ.isPreconditioning() /*|| occ
                .isForcedOccupied()*/
                    || (systemOcc == Occupancy.PRECONDITIONING) /*|| (systemOcc == Occupancy
                        .FORCEDOCCUPIED)*/) ? occ.getHeatingVal() :
                    (occ.getHeatingVal() - occ.getUnoccupiedZoneSetback());
            setDesiredTemp(equip, coolingTemp, "cooling",
                    occ.isForcedOccupied() || systemOcc == Occupancy.FORCEDOCCUPIED);
            setDesiredTemp(equip, heatingTemp, "heating",
                    occ.isForcedOccupied() || systemOcc == Occupancy.FORCEDOCCUPIED);
            setDesiredTemp(equip, avgTemp, "average",
                    occ.isForcedOccupied() || systemOcc == Occupancy.FORCEDOCCUPIED);

        }


        return occ;
    }


    public static void setDesiredTemp(Equip equip, Double desiredTemp, String flag,
                                      boolean isForcedOccupied) {
        CcuLog.d(L.TAG_CCU_SCHEDULER, "ZoneSchedule Equip: " + equip.getDisplayName() + " Temp: " + desiredTemp + " Flag: " + flag+","+isForcedOccupied);
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and " + flag + " and desired and sp and equipRef == \"" + equip.getId() + "\"");
        if (point == null || point.size() == 0) {
            return; //Equip might have been deleted.
        }
        final String id = point.get("id").toString();
        if(isForcedOccupied) {
            CCUHsApi.getInstance().writeHisValById(id, HSUtil.getPriorityVal(id));
            return;
        }
        if (HSUtil.getPriorityLevelVal(id,8) == desiredTemp) {
            CcuLog.d(L.TAG_CCU_SCHEDULER, flag+"DesiredTemp not changed : Skip PointWrite");
            CCUHsApi.getInstance().writeHisValById(id, HSUtil.getPriorityVal(id));
            return;
        }
        
        CCUHsApi.getInstance().pointWrite(HRef.make(id.replace("@","")), 8, "Scheduler", desiredTemp != null ? HNum.make(desiredTemp) : HNum.make(0), HNum.make(0));
        CCUHsApi.getInstance().writeHisValById(id, HSUtil.getPriorityVal(id));
    }

    private static void handleAutoaway(Equip equip,boolean isForcedOccupied) {

        HashMap coolingDtPoint = CCUHsApi.getInstance().read("point and air and temp and " +
                "desired and cooling and sp and equipRef == \"" + equip.getId() + "\"");
        HashMap heatinDtPoint = CCUHsApi.getInstance().read("point and air and temp and " +
                "desired and heating and sp and equipRef == \"" + equip.getId() + "\"");
        double autoawaysetback = TunerUtil.readTunerValByQuery("auto and away and setback");

        double heatingDT = getPriorityDesiredTemp(heatinDtPoint.get("id").toString());
        double coolingDT = getPriorityDesiredTemp(coolingDtPoint.get("id").toString());

        Point hp = new Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(heatinDtPoint.get("id").toString())).build();
        Point cp = new Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(coolingDtPoint.get("id").toString())).build();
        setDesiredatLevel3(hp,(heatingDT-autoawaysetback),isForcedOccupied,equip,"heating");
        setDesiredatLevel3(cp,(coolingDT+autoawaysetback),isForcedOccupied,equip,"cooling");

    }

    private static void setDesiredatLevel3(Point p,Double desiredTemp, boolean isForcedOccupied, Equip equip, String flag){

        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(p.getRoomRef());
        CcuLog.d(L.TAG_CCU_SCHEDULER, "setDesiredatlevel3 Equip: " + equip.getDisplayName() + " Temp: " + desiredTemp + " Flag: " + flag+","+isForcedOccupied);
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and " + flag + " and desired and sp and equipRef == \"" + equip.getId() + "\"");
        if (point == null || point.size() == 0) {
            return; //Equip might have been deleted.
        }
        final String id = point.get("id").toString();
        if(isForcedOccupied)
            return;
        if (HSUtil.getPriorityLevelVal(id,3) == desiredTemp) {
            CcuLog.d(L.TAG_CCU_SCHEDULER, flag+"DesiredTemp not changed : Skip PointWrite");
            return;
        }

        Schedule.Days day = occ.getCurrentlyOccupiedSchedule();

        if(day == null)
            return;



        DateTime overrideExpiry = new DateTime(MockTime.getInstance().getMockTime())
                .withHourOfDay(day.getEthh())
                .withMinuteOfHour(day.getEtmm())
                .withDayOfWeek(day.getDay() + 1)
                .withSecondOfMinute(0);

        CCUHsApi.getInstance().pointWrite(HRef.make(id.replace("@","")), 3,
                "Scheduler",HNum.make(desiredTemp),HNum.make(overrideExpiry.getMillis()
                        - System.currentTimeMillis(), "ms"));
        CCUHsApi.getInstance().writeHisValById(id, HSUtil.getPriorityVal(id));

        SystemScheduleUtil.setAppOverrideExpiry(p,overrideExpiry.getMillis());
    }

    private static double getPriorityDesiredTemp(String id){

        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 4; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;

    }
    
    
    
}
