package a75f.io.logic;

import android.util.Log;

import org.joda.time.DateTime;
import org.projecthaystack.HDate;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HTime;
import org.projecthaystack.HTimeZone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Queries;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.tuners.BuildingTunerCache;

public class DefaultSchedules {

    public static double DEFAULT_COOLING_TEMP = 74.0F;
    public static double DEFAULT_HEATING_TEMP = 70.0F;

    public static String generateDefaultSchedule(boolean zone, String zoneId) {

        if (!zone) {
            ArrayList<Schedule> buildingSchedules = CCUHsApi.getInstance().getSystemSchedule(false);
            if (!buildingSchedules.isEmpty()) {
                Schedule buildingSchedule = buildingSchedules.get(0);
                return buildingSchedule.getId();
            }
        }
        HRef siteId = CCUHsApi.getInstance().getSiteIdRef();

        HDict[] days = new HDict[5];

        days[0] = getDefaultForDay(DAYS.MONDAY.ordinal(), zoneId);
        days[1] = getDefaultForDay(DAYS.TUESDAY.ordinal(), zoneId);
        days[2] = getDefaultForDay(DAYS.WEDNESDAY.ordinal(), zoneId);
        days[3] = getDefaultForDay(DAYS.THURSDAY.ordinal(), zoneId);
        days[4] = getDefaultForDay(DAYS.FRIDAY.ordinal(), zoneId);

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
                .add("createdDateTime", HDateTime.make(System.currentTimeMillis()))
                .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()))
                .add("lastModifiedBy", CCUHsApi.getInstance().getCCUUserName())
                .add("siteRef", siteId);
        if (zoneId != null) {
            defaultSchedule.add("roomRef", HRef.copy(zoneId));
            defaultSchedule.add("ccuRef", HRef.copy(CCUHsApi.getInstance().getCcuId()));
            defaultSchedule.add(Tags.FOLLOW_BUILDING);
            defaultSchedule.add(Tags.UNOCCUPIED_ZONE_SETBACK, getZoneScheduablePoint(Queries.ZONE_UNOCCUPIED_ZONE_SETBACK,
                    zoneId));

        }
        

        CCUHsApi.getInstance().addSchedule(localId.toVal(), defaultSchedule.toDict());
        return localId.toCode();
    }


    public static HDict getDefaultForDay(int day, String zoneId) {
        HDictBuilder hDictDay = new HDictBuilder()
                .add("day", HNum.make(day))
                .add("sthh", HNum.make(8))
                .add("stmm", HNum.make(0))
                .add("ethh", HNum.make(17))
                .add("etmm", HNum.make(30))
                .add("coolVal", HNum.make(DEFAULT_COOLING_TEMP))
                .add("heatVal", HNum.make(DEFAULT_HEATING_TEMP));
        if (zoneId != null) {
            hDictDay.add(Tags.HEATING_USER_LIMIT_MIN, getZoneScheduablePoint(Queries.ZONE_HEATING_USER_LIMIT_MIN,
                    zoneId));
            hDictDay.add(Tags.HEATING_USER_LIMIT_MAX, getZoneScheduablePoint(Queries.ZONE_HEATING_USER_LIMIT_MAX,
                            zoneId));
            hDictDay.add(Tags.COOLING_USER_LIMIT_MIN, getZoneScheduablePoint(Queries.ZONE_COOLING_USER_LIMIT_MIN,
                            zoneId));
            hDictDay.add(Tags.COOLING_USER_LIMIT_MAX, getZoneScheduablePoint(Queries.ZONE_COOLING_USER_LIMIT_MAX
                            , zoneId));
            hDictDay.add(Tags.COOLING_DEADBAND, getZoneScheduablePoint(Queries.ZONE_COOLING_DEADBAND, zoneId));
            hDictDay.add(Tags.HEATING_DEADBAND, getZoneScheduablePoint(Queries.ZONE_HEATING_DEADBAND, zoneId));
        }
        return hDictDay.toDict();
    }

    public static void upsertVacation(String id, String vacationName, DateTime startDate, DateTime endDate)
    {
        HRef siteId = CCUHsApi.getInstance().getSiteIdRef();
        HRef localId;
        HDateTime createdDateTime = null;

        if(id == null) {
            localId = HRef.make(UUID.randomUUID().toString());
            createdDateTime = HDateTime.make(System.currentTimeMillis());
        }
        else {
            localId = HRef.make(id);
            HDict vacDict = CCUHsApi.getInstance().getScheduleDictById(id);
            if(vacDict.has("createdDateTime")) {
                createdDateTime = HDateTime.make(vacDict.get("createdDateTime").toString());
            }
        }

        HDictBuilder defaultSchedule = new HDictBuilder()
                .add("id", localId)
                .add("temp")
                .add("schedule")
                .add("building")
                .add("vacation")
                .add("cooling")
                .add("heating")
                .add("range", buildVacationHDictRange(startDate, endDate))
                .add("dis", vacationName)
                .add("siteRef", siteId)
                .add("lastModifiedBy", CCUHsApi.getInstance().getCCUUserName())
                .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()));

        if(createdDateTime != null){
            defaultSchedule.add("createdDateTime", createdDateTime);
        }
        if (id == null)
        {
            CCUHsApi.getInstance().addSchedule(localId.toVal(), defaultSchedule.toDict());
        } else {
            CCUHsApi.getInstance().updateSchedule(localId.toVal(), defaultSchedule.toDict());
        }
        Log.d("CCU_HS","upsertVacation: "+defaultSchedule.toDict().toZinc());
    }

    private static HDict buildVacationHDictRange(DateTime startDate, DateTime endDate){
        String timeZoneFromHS = CCUHsApi.getInstance().readEntity("site").get("tz").toString();

        HDate startHdate = HDate.make(startDate.getYear(), startDate.getMonthOfYear(), startDate.getDayOfMonth());
        HTime startHTime = HTime.make(startDate.getHourOfDay(), startDate.getMinuteOfHour(),
                startDate.getSecondOfMinute());

        HDate endHdate = HDate.make(endDate.getYear(), endDate.getMonthOfYear(), endDate.getDayOfMonth());
        HTime endHTime = HTime.make(endDate.getHourOfDay(), endDate.getMinuteOfHour(), endDate.getSecondOfMinute());

        return new HDictBuilder()
                .add("stdt", HDateTime.make(startHdate, startHTime, HTimeZone.make(timeZoneFromHS)))
                .add("etdt", HDateTime.make(endHdate, endHTime, HTimeZone.make(timeZoneFromHS))).toDict();
    }
    
    public static void upsertZoneVacation(String id, String vacationName, DateTime startDate, DateTime endDate, String roomRef)
    {
        HRef siteId = CCUHsApi.getInstance().getSiteIdRef();
        HRef localId;
        HDateTime createdDateTime = null;

        if(id == null) {
            localId = HRef.make(UUID.randomUUID().toString());
            createdDateTime = HDateTime.make(System.currentTimeMillis());
        }
        else {
            localId = HRef.make(id);
            HDict vacDict = CCUHsApi.getInstance().getScheduleDictById(id);
            if(vacDict.has("createdDateTime")) {
                createdDateTime = HDateTime.make(vacDict.get("createdDateTime").toString());
            }
        }

        HDictBuilder defaultSchedule = new HDictBuilder()
                                        .add("id", localId)
                                        .add("temp")
                                        .add("schedule")
                                        .add("zone")
                                        .add("vacation")
                                        .add("cooling")
                                        .add("heating")
                                        .add("range", buildVacationHDictRange(startDate, endDate))
                                        .add("dis", vacationName)
                                        .add("siteRef", siteId)
                                        .add("roomRef", HRef.copy(roomRef))
                                        .add("ccuRef", HRef.copy(CCUHsApi.getInstance().getCcuId()))
                                        .add("lastModifiedBy", CCUHsApi.getInstance().getCCUUserName())
                                       .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()));
                                        //.toDict();
        if(createdDateTime != null){
            defaultSchedule.add("createdDateTime", createdDateTime);
        }

        if (id == null)
        {
            CCUHsApi.getInstance().addSchedule(localId.toVal(), defaultSchedule.toDict());
        } else {
            CCUHsApi.getInstance().updateSchedule(localId.toVal(), defaultSchedule.toDict());
        }
        Log.d("CCU_HS","upsertZoneVacation: "+defaultSchedule.toDict().toZinc());
    }

    public static void setDefaultCoolingHeatingTemp(){
        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        BuildingTunerCache buildingTunerCache = BuildingTunerCache.getInstance();

        HashMap<Object,Object> coolDB = CCUHsApi.getInstance().readEntity("point and cooling and deadband and schedulable and default");
        HashMap<Object,Object> heatDB = CCUHsApi.getInstance().readEntity("point and heating and deadband and schedulable and default");

        double hdb = HSUtil.getLevelValueFrom16(heatDB.get("id").toString());
        double cdb = HSUtil.getLevelValueFrom16(coolDB.get("id").toString());
        /*HashMap<Object,Object> coolULMap =  CCUHsApi.getInstance().readEntity("schedulable and point and limit and max and cooling and user and default");
        HashMap<Object,Object> heatULMap =  CCUHsApi.getInstance().readEntity("schedulable and point and limit and min and heating and user and default");
        HashMap<Object,Object> coolLLMap =  CCUHsApi.getInstance().readEntity("schedulable and point and limit and min and cooling and user and default");
        HashMap<Object,Object> heatLLMap =  CCUHsApi.getInstance().readEntity("schedulable and point and limit and max and heating and user and default");
       */
        double heatLL = buildingTunerCache.getMaxHeatingUserLimit();
        double coolLL = buildingTunerCache.getMinCoolingUserLimit();

        double diffValue = (coolLL - heatLL);
        if (diffValue <= (hdb + cdb)){
            double value = ((hdb + cdb) - diffValue)/2;
            DEFAULT_COOLING_TEMP = coolLL + value;
            DEFAULT_HEATING_TEMP = heatLL - value;
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

    private static Double getZoneScheduablePoint(String query, String roomRef){
        return CCUHsApi.getInstance().readPointPriorityValByQuery(query+ " \""+roomRef+"\"");
    }
}
