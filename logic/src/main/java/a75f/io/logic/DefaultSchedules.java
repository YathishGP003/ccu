package a75f.io.logic;

import android.util.Log;

import org.joda.time.DateTime;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.Equip;
import a75f.io.logic.tuners.TunerUtil;

public class DefaultSchedules {

    public static double DEFAULT_COOLING_TEMP = 74.0F;
    public static double DEFAULT_HEATING_TEMP = 70.0F;

    public static String generateDefaultSchedule(boolean zone, String zoneId) {

        HRef siteId = CCUHsApi.getInstance().getSiteId();

        HDict[] days = new HDict[5];

        days[0] = getDefaultForDay(DAYS.MONDAY.ordinal());
        days[1] = getDefaultForDay(DAYS.TUESDAY.ordinal());
        days[2] = getDefaultForDay(DAYS.WEDNESDAY.ordinal());
        days[3] = getDefaultForDay(DAYS.THURSDAY.ordinal());
        days[4] = getDefaultForDay(DAYS.FRIDAY.ordinal());

        HList hList = HList.make(days);

        HRef localId = HRef.make(UUID.randomUUID().toString());
        HDictBuilder defaultSchedule = new HDictBuilder()
                .add("id", localId)
                .add("kind", "Number")
                .add(zone ? "zone":"building")
                .add("temp")
                .add("schedule")
                .add("heating")
                .add("cooling")
                .add("dis", zone ? "Zone Schedule" : "Building Schedule")
                .add("days", hList)
                .add("siteRef", siteId);
        
        if (zoneId != null) {
            defaultSchedule.add("roomRef", HRef.copy(zoneId));
            defaultSchedule.add("disabled");// Zone schedule to be disabled by default
        }
        

        CCUHsApi.getInstance().addSchedule(localId.toVal(), defaultSchedule.toDict());
        return localId.toCode();
    }


    public static HDict getDefaultForDay(int day) {
        HDict hDictDay = new HDictBuilder()
                .add("day", HNum.make(day))
                .add("sthh", HNum.make(8))
                .add("stmm", HNum.make(0))
                .add("ethh", HNum.make(17))
                .add("etmm", HNum.make(30))
                .add("coolVal", HNum.make(DEFAULT_COOLING_TEMP))
                .add("heatVal", HNum.make(DEFAULT_HEATING_TEMP))
                .toDict();

        return hDictDay;
    }

    public static void upsertVacation(String id, String vacationName, DateTime startDate, DateTime endDate)
    {
        HRef siteId = CCUHsApi.getInstance().getSiteId();
        HRef localId;

        if(id == null) {
            localId = HRef.make(UUID.randomUUID().toString());
        }
        else {
            localId = HRef.make(id);
        }

        HDict hDict = new HDictBuilder()
                .add("stdt", HDateTime.make(startDate.getMillis()))
                .add("etdt", HDateTime.make(endDate.getMillis())).toDict();

        HDict defaultSchedule = new HDictBuilder()
                .add("id", localId)
                .add("temp")
                .add("schedule")
                .add("building")
                .add("vacation")
                .add("cooling")
                .add("heating")
                .add("range", hDict)
                .add("dis", vacationName)
                .add("siteRef", siteId)
                .toDict();
        Log.d("CCU_HS","upsertVacation: "+defaultSchedule.toZinc());
        if (id == null)
        {
            CCUHsApi.getInstance().addSchedule(localId.toVal(), defaultSchedule);
        } else {
            CCUHsApi.getInstance().updateSchedule(localId.toVal(), defaultSchedule);
        }
    }
    
    public static void upsertZoneVacation(String id, String vacationName, DateTime startDate, DateTime endDate, String roomRef)
    {
        HRef siteId = CCUHsApi.getInstance().getSiteId();
        HRef localId;
        
        if(id == null) {
            localId = HRef.make(UUID.randomUUID().toString());
        }
        else {
            localId = HRef.make(id);
        }


        HDict hDict = new HDictBuilder()
                .add("stdt", HDateTime.make(startDate.getMillis()))
                .add("etdt", HDateTime.make(endDate.getMillis())).toDict();


        HDict defaultSchedule = new HDictBuilder()
                                        .add("id", localId)
                                        .add("temp")
                                        .add("schedule")
                                        .add("zone")
                                        .add("vacation")
                                        .add("cooling")
                                        .add("heating")
                                        .add("range", hDict)
                                        .add("dis", vacationName)
                                        .add("siteRef", siteId)
                                        .add("roomRef", HRef.copy(roomRef))
                                        .toDict();
    
        Log.d("CCU_HS","upsertZoneVacation: "+defaultSchedule.toZinc());
        if (id == null)
        {
            CCUHsApi.getInstance().addSchedule(localId.toVal(), defaultSchedule);
        } else {
            CCUHsApi.getInstance().updateSchedule(localId.toVal(), defaultSchedule);
        }
    }

    public static void setDefaultCoolingHeatingTemp(){
        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        Equip p = new Equip.Builder().setHashMap(tuner).build();

        double hdb = TunerUtil.getHeatingDeadband(p.getId());
        double cdb = TunerUtil.getCoolingDeadband(p.getId());
        HashMap coolULMap = CCUHsApi.getInstance().read("point and limit and max and cooling and user");
        HashMap heatULMap = CCUHsApi.getInstance().read("point and limit and max and heating and user");
        HashMap coolLLMap = CCUHsApi.getInstance().read("point and limit and min and cooling and user");
        HashMap heatLLMap = CCUHsApi.getInstance().read("point and limit and min and heating and user");
        double heatLL = getTuner(heatLLMap.get("id").toString());
        double heatUL = getTuner(heatULMap.get("id").toString());
        double coolLL = getTuner(coolLLMap.get("id").toString());
        double coolUL = getTuner(coolULMap.get("id").toString());

        double diffValue = (coolLL - heatLL);
        if (diffValue <= (hdb + cdb)){
            DEFAULT_COOLING_TEMP = coolLL + cdb;
            DEFAULT_HEATING_TEMP = heatLL - hdb;
        }else {
            DEFAULT_COOLING_TEMP = coolLL;
            DEFAULT_HEATING_TEMP = heatLL;
        }
    }

    public static double getTuner(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList values = hayStack.readPoint(id);
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
