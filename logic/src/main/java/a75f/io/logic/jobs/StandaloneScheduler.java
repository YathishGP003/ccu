package a75f.io.logic.jobs;

import android.os.AsyncTask;
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
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.ZoneTempState;
import a75f.io.logic.pubnub.ZoneDataInterface;
import a75f.io.logic.tuners.StandaloneTunerUtil;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;

public class StandaloneScheduler {

    private static final int SCHEDULER_PRIORITY = 12;

    private static ZoneDataInterface zoneDataInterface = null;

    static HashMap<String, String> standaloneStatus = new HashMap<String, String>();

    private static final String TAG = "SAScheduler";
    public static Occupied processEquip( Equip equip, Schedule equipSchedule, Schedule vacation) {


        Log.i(TAG, "Equip: " + equip);
        Log.i(TAG, "Equip Schedule: " + equipSchedule);
        Occupied occ = equipSchedule.getCurrentValues();
        Occupied curOccu = ScheduleProcessJob.getOccupiedModeCache(equip.getRoomRef());
        //When schedule is deleted
        if (occ == null) {
            ScheduleProcessJob.occupiedHashMap.remove(equip.getRoomRef());
            return null;
        }
        if (vacation != null)
            occ.setOccupied(false);

        occ.setVacation(vacation);
        if(curOccu != null) {
            occ.setPreconditioning(curOccu.isPreconditioning());
            occ.setForcedOccupied(curOccu.isForcedOccupied());
        }


        double heatingDeadBand = StandaloneTunerUtil.readTunerValByQuery("heating and deadband", equip.getId());
        double coolingDeadBand = StandaloneTunerUtil.readTunerValByQuery("cooling and deadband", equip.getId());
        double setback = TunerUtil.readTunerValByQuery("unoccupied and setback", equip.getId());

        occ.setUnoccupiedZoneSetback(setback);
        occ.setHeatingDeadBand(heatingDeadBand);
        occ.setCoolingDeadBand(coolingDeadBand);
        if (occ != null && ScheduleProcessJob.putOccupiedModeCache(equip.getRoomRef(), occ)) {
            double avgTemp = (occ.getCoolingVal()+occ.getHeatingVal())/2.0;
            double deadbands = (occ.getCoolingVal() - occ.getHeatingVal()) / 2.0 ;
            occ.setCoolingDeadBand(deadbands);
            occ.setHeatingDeadBand(deadbands);
            Double coolingTemp = ((occ.isOccupied() || occ.isPreconditioning() || occ.isForcedOccupied()) ? occ.getCoolingVal() : (occ.getCoolingVal() + occ.getUnoccupiedZoneSetback()));
            setDesiredTemp(equip, coolingTemp, "cooling");
            Double heatingTemp = (occ.isOccupied() || occ.isPreconditioning() || occ.isForcedOccupied())? occ.getHeatingVal() : (occ.getHeatingVal() - occ.getUnoccupiedZoneSetback());
            setDesiredTemp(equip, heatingTemp, "heating");
            setDesiredTemp(equip, avgTemp, "average");
        }

        return occ;
    }


    public static void setDesiredTemp(Equip equip, Double desiredTemp, String flag) {
        //CcuLog.d(L.TAG_CCU_SCHEDULER, "Equip: " + equip.getId() + " Temp: " + desiredTemp + " Flag: " + flag);
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and " + flag + " and desired and sp and equipRef == \"" + equip.getId() + "\"");
        if (points == null || points.size() == 0) {
            return; //Equip might have been deleted.
        }
        final String id = ((HashMap) points.get(0)).get("id").toString();
        if (HSUtil.getPriorityLevelVal(id,8) == desiredTemp) {
            CcuLog.d(L.TAG_CCU_SCHEDULER, flag+"DesiredTemp not changed : Skip PointWrite");
            return;
        }
        try {
            CCUHsApi.getInstance().pointWrite(HRef.make(id.replace("@", "")), 8, "Scheduler", desiredTemp != null ? HNum.make(desiredTemp) : HNum.make(0), HNum.make(0));
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                CCUHsApi.getInstance().writeHisValById(id, HSUtil.getPriorityVal(id));
            }
        },100);
    }

    public static void updateSmartStatStatus(String equipId, ZoneState state, HashMap<String,Integer> relayStages, ZoneTempState temperatureState){
        String status = "OFF ";
        switch (temperatureState){
            case RF_DEAD:
                status = "RF Signal dead ";
                break;
            case TEMP_DEAD:
                status = "Zone Temp Dead ";
                break;
            case EMERGENCY:
                status = "Emergency ";
                break;
            case NONE:
                status = "";
                break;
            case FAN_OP_MODE_OFF:
                status = "OFF ";
                break;
        }

        switch (state){
            case COOLING:
                if(relayStages.containsKey("CoolingStage2") && relayStages.containsKey("CoolingStage1"))
                    status = status +"Cooling 1&2 ON,";
                else if(relayStages.containsKey("CoolingStage2"))
                    status = status +" Cooling 2 ON,";
                else if(relayStages.containsKey("CoolingStage1"))
                    status = status + "Cooling 1 ON,";
                else if(relayStages.containsKey("HeatingStage1")) //For Two PFCU alone
                    status = "Heating ON,";
                else
                    status = "Cooling OFF,";

                break;
            case HEATING:
                if(relayStages.containsKey("HeatingStage2") && relayStages.containsKey("HeatingStage1"))
                    status = status + "Heating 1&2 ON,";
                else if(relayStages.containsKey("HeatingStage2"))
                    status = status + "Heating 2 ON,";
                else if(relayStages.containsKey("HeatingStage1"))
                    status = status + "Heating 1 ON,";
                else
                    status = "Heating OFF,";
                break;
            case DEADBAND:
                break;
        }

        if((temperatureState != ZoneTempState.FAN_OP_MODE_OFF) && (temperatureState != ZoneTempState.TEMP_DEAD)) {
            if (status.equals("OFF ") && relayStages.size() > 0) status = "";
            /*if(relayStages.containsKey("FanStage3") && relayStages.containsKey("FanStage2") && relayStages.containsKey("FanStage1"))
                status = status + " Fan 1,2&3 ON";
            else if (relayStages.containsKey("FanStage3") && relayStages.containsKey("FanStage2"))
                status = status + " Fan 2&3 ON";
            else if (relayStages.containsKey("FanStage3") && relayStages.containsKey("FanStage1"))
                status = status + " Fan 1&3 ON";*/
           else if (relayStages.containsKey("FanStage2") && relayStages.containsKey("FanStage1"))
                status = status + " Fan 1&2 ON";
            else if (relayStages.containsKey("Humidifier") && relayStages.containsKey("FanStage1"))
                status = status + " Fan 1 ON, Humidifier ON";
            else if (relayStages.containsKey("Dehumidifier") && relayStages.containsKey("FanStage1"))
                status = status + " Fan 1 ON, Dehumidifier ON";
            else if (relayStages.containsKey("FanStage3"))
                status = status + " Fan 3 ON";
            else if (relayStages.containsKey("FanStage2"))
                status = status + " Fan 2 ON";
            else if (relayStages.containsKey("Humidifier"))
                status = status + " Humidifier ON";
            else if (relayStages.containsKey("Dehumidifier"))
                status = status + " Dehumidifier ON";
            else if (relayStages.containsKey("FanStage1"))
                status = status + " Fan 1 ON";
            else
                status = status + " Fan OFF";
        }
        if(equipId != null) {
            if(getSmartStatStatusString(equipId).equals(status) == false) {
                if(standaloneStatus.containsKey(equipId))standaloneStatus.remove(equipId);
                standaloneStatus.put(equipId, status);
                updateStandaloneEquipStatus(equipId,status,state);
            }
        }

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
                HashMap cdb = hayStack.read("point and standalone and "+cmd+" and equipRef == \""+equipRef+"\"");
                if(cdb != null && (cdb.get("id") != null)) {
                    String id = cdb.get("id").toString();
                    Point p = new Point.Builder().setHashMap(hayStack.readMapById(id)).build();
                    for (String marker : p.getMarkers()) {
                        if (marker.equals("writable")) {
                            CcuLog.d(L.TAG_CCU_UI, "Set Writbale Val " + p.getDisplayName() + ": " + val);
                            CCUHsApi.getInstance().pointWrite(HRef.copy(id), TunerConstants.UI_DEFAULT_VAL_LEVEL, "ccu", HNum.make(val), HNum.make(0));
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
                if (zoneDataInterface != null) {
                    Log.i("PubNub","Zone Data updateOperationalPoints Refresh");
                    zoneDataInterface.refreshScreen("");
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

	public static void updateStandaloneEquipStatus(String equipId,String status, ZoneState state) {
        CCUHsApi.getInstance().writeHisValByQuery("point and temp and operating and mode and his and equipRef == \""+equipId+"\"" , (double)state.ordinal());
        CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and equipRef == \""+equipId+"\"", status);
        if (zoneDataInterface != null) {
            Log.i("PubNub","updateStandaloneEquipStatus Refresh");
            zoneDataInterface.refreshScreen("");
        }

    }

    public static void setZoneDataInterface(ZoneDataInterface in) { zoneDataInterface = in; }
}
