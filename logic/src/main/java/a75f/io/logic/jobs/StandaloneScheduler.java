package a75f.io.logic.jobs;

import android.os.AsyncTask;
import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.tuners.StandaloneTunerUtil;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;

public class StandaloneScheduler {

    private static final int SCHEDULER_PRIORITY = 12;


    static HashMap<String, String> standaloneStatus = new HashMap<String, String>();

    private static final String TAG = "SAScheduler";


    public static Occupied processEquip(Equip equip, Schedule equipSchedule, Schedule vacation) {


        Log.i(TAG, "Equip: " + equip);
        Log.i(TAG, "Equip Schedule: " + equipSchedule);
        Occupied occ = equipSchedule.getCurrentValues();
        //When schedule is deleted
        if (occ == null) {
            ScheduleProcessJob.occupiedHashMap.remove(equip.getRoomRef());
            return null;
        }
        if (vacation != null)
            occ.setOccupied(false);

        occ.setVacation(vacation);


        double heatingDeadBand = StandaloneTunerUtil.readTunerValByQuery("heating and deadband", equip.getId());
        double coolingDeadBand = StandaloneTunerUtil.readTunerValByQuery("cooling and deadband", equip.getId());
        double setback = TunerUtil.readTunerValByQuery("unoccupied and setback", equip.getId());

        occ.setHeatingDeadBand(heatingDeadBand);
        occ.setCoolingDeadBand(coolingDeadBand);
        occ.setUnoccupiedZoneSetback(setback);


        if (occ != null && ScheduleProcessJob.putOccupiedModeCache(equip.getRoomRef(), occ)) {


            Double coolingTemp = occ.isOccupied() ? occ.getCoolingVal() : (occ.getCoolingVal() + occ.getUnoccupiedZoneSetback());
            setDesiredTemp(equip, coolingTemp, "cooling");

            Double heatingTemp = occ.isOccupied() ? occ.getHeatingVal() : (occ.getHeatingVal() - occ.getUnoccupiedZoneSetback());
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
        if (getPriorityVal(id , 8) == desiredTemp) {
            CcuLog.d(L.TAG_CCU_SCHEDULER, flag+"DesiredTemp not changed : Skip PointWrite");
            return;
        }
        try {
            CCUHsApi.getInstance().pointWrite(HRef.make(id.replace("@", "")), 9, "Scheduler", desiredTemp != null ? HNum.make(desiredTemp) : HNum.make(0), HNum.make(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
        CCUHsApi.getInstance().writeHisValById(id, getPriorityVal(id));
    }

    public static double getPriorityVal(String id) {
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }

    public static void updateSmartStatStatus(String equipId,ZoneState state, HashMap<String,Integer> relayStages){

        String status = "OFF ";
        switch (state){
            case COOLING:
                if(relayStages.containsKey("CoolingStage2") && relayStages.containsKey("CoolingStage1"))
                    status = "Cooling 1&2 ON,";
                else if(relayStages.containsKey("CoolingStage2"))
                    status = "Cooling 2 ON,";
                else if(relayStages.containsKey("CoolingStage1"))
                    status = "Cooling 1 ON,";
                else
                    status = "Cooling OFF,";

                break;
            case HEATING:
                if(relayStages.containsKey("HeatingStage2") && relayStages.containsKey("HeatingStage1"))
                    status = "Heating 1&2 ON,";
                else if(relayStages.containsKey("HeatingStage2"))
                    status = "Heating 2 ON,";
                else if(relayStages.containsKey("HeatingStage1"))
                    status = "Heating 1 ON,";
                else
                    status = "Heating OFF,";
                break;
            case DEADBAND:
                break;
        }

        if(status.equals("OFF") && relayStages.size() > 0) status = "";
        if(relayStages.containsKey("FanStage2") && relayStages.containsKey("FanStage1"))
            status = status + " Fan 1&2 ON";
        else if(relayStages.containsKey("FanStage2"))
            status = status + " Fan 2 ON";
        else if(relayStages.containsKey("FanStage1"))
            status = status + " Fan 1 ON";
        else
            status = status +" Fan OFF";

        //TODO if change in status need to update haystack string for App consuming this status update KUMAR
        if(equipId != null)
            standaloneStatus.put(equipId,status);

    }

    public static String getSmartStatStatusString(String equipRef) {
        if(standaloneStatus.size() > 0 && standaloneStatus.containsKey(equipRef))
            return standaloneStatus.get(equipRef);
        else
            return "OFF";

    }

    public static void updateOperationalPoints(final String equipRef, final String cmd, final double val) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground( final String ... params ) {

                CCUHsApi hayStack = CCUHsApi.getInstance();
                HashMap cdb = hayStack.read("point and standalone and operation and mode and "+cmd+" and equipRef == \""+equipRef+"\"");
                if(cdb != null && (cdb.get("id") != null)) {
                    String id = cdb.get("id").toString();
                    Point p = new Point.Builder().setHashMap(hayStack.readMapById(id)).build();
                    for (String marker : p.getMarkers()) {
                        if (marker.equals("writable")) {
                            CcuLog.d(L.TAG_CCU_UI, "Set Writbale Val " + p.getDisplayName() + ": " + val);
                            //TODO Duration is set only for 2 hours or 0??? kumar doubts
                            CCUHsApi.getInstance().pointWrite(HRef.copy(id), TunerConstants.MANUAL_OVERRIDE_VAL_LEVEL, "manual", HNum.make(val), HNum.make(2 * 60 * 60 * 1000, "ms"));
                        }
                    }

                    for (String marker : p.getMarkers()) {
                        if (marker.equals("his")) {
                            CcuLog.d(L.TAG_CCU_UI, "Set His Val " + id + ": " + val);
                            hayStack.writeHisValById(id, val);
                        }
                    }
                }


                return null;
            }

            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }


    public static double getPriorityVal(String id, int level) {
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(level));
            if (valMap.get("val") != null) {
                return Double.parseDouble(valMap.get("val").toString());
            }
        }
        return 0;
    }
}
