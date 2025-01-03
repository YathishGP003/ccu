package a75f.io.api.haystack;

import static a75f.io.api.haystack.util.TimeUtil.getEndHour;
import static a75f.io.api.haystack.util.TimeUtil.getEndMinute;
import static a75f.io.api.haystack.util.TimeUtil.getEndSec;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HTimeZone;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.UUID;

import a75f.io.api.haystack.schedule.BuildingOccupancy;
import a75f.io.api.haystack.util.TimeUtil;
import a75f.io.logger.CcuLog;

/***
 * Supports Schedule to HDict
 * HDict to Schedule
 *
 * Raw creation requires id field is set to  String localId = UUID.randomUUID().toString();
 *
 */
public class Schedule extends Entity
{
    private static final DateTimeFormatter SS_DATE_TIME_FORMATTER  =  DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final String SCHEDULE_GROUP = "scheduleGroup";
    public boolean isBuildingSchedule()
    {
        return getMarkers().contains("building") && !getMarkers().contains("named");
    }

    public boolean isZoneSchedule()
    {
        return getMarkers().contains("zone");
    }

    public boolean isNamedSchedule()
    {
        return getMarkers().contains("named");
    }


    public static String getZoneIdByEquipId(String equipId)
    {
        HDict equipDict = CCUHsApi.getInstance().readHDictById(equipId);
        Equip   equip        = new Equip.Builder().setHDict(equipDict).build();
        return equip.getRoomRef();
    }

    public static Schedule getScheduleByEquipId(String equipId)
    {
        HDict equipDict = CCUHsApi.getInstance().readHDictById(equipId);
        Equip   equip        = new Equip.Builder().setHDict(equipDict).build();

        return getScheduleForZone(equip.getRoomRef().replace("@", ""), false);
    }

    public static Schedule getVacationByEquipId(String equipId)
    {
        HDict equipDict = CCUHsApi.getInstance().readHDictById(equipId);
        Equip   equip        = new Equip.Builder().setHDict(equipDict).build();

        return getScheduleForZone(equip.getRoomRef().replace("@", ""), true);

    }

    public static int getInt(String intString){
        if(intString.contains(".")){
            String[] numerics = intString.split("\\.");
            return Integer.parseInt(numerics[0]);
        }
        return Integer.parseInt(intString);
    }

    private static Date getWeekStartDate() {
        Calendar calendar = Calendar.getInstance();
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DATE, -1);
        }
        return calendar.getTime();
    }
    private static Date getWeekEndDate() {
        Calendar calendar = Calendar.getInstance();
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            calendar.add(Calendar.DATE, 1);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    /**
     *
     * @param roomRef roomRef = null is building otherwise for a zone
     */
    private static List<Schedule.Days> getSpecialScheduleDaysForRunningWeek(String roomRef){
        List<Schedule.Days> daysList = new ArrayList<>();
        List<HashMap<Object, Object>> specialScheduleList = CCUHsApi.getInstance().getSpecialSchedules(roomRef);
        for(HashMap<Object, Object> specialSchedule : specialScheduleList){
            HDict range = (HDict) specialSchedule.get(Tags.RANGE);
            String beginDate = range.get(Tags.STDT).toString();
            int beginHour = getInt(range.get(Tags.STHH).toString());
            int beginMin = getInt(range.get(Tags.STMM).toString());
            String endDate = range.get(Tags.ETDT).toString();
            int endHour = getInt(range.get(Tags.ETHH).toString());
            int endMin = getInt(range.get(Tags.ETMM).toString());

            endMin = getInt(range.get(Tags.ETHH).toString()) == 24 ? 59 : endMin;
            endHour = TimeUtil.getEndHour(endHour);

            DateTime beginDateTime = SS_DATE_TIME_FORMATTER.parseDateTime(beginDate)
                    .withHourOfDay(beginHour)
                    .withMinuteOfHour(beginMin);
            DateTime endDateTime = SS_DATE_TIME_FORMATTER.parseDateTime(endDate)
                    .withHourOfDay(getEndHour(endHour))
                    .withMinuteOfHour(getEndMinute(endHour, endMin));

            int dayNumber = 0; //0-6 (Monday-Sunday) Schedule->days->day

            Date weekStartDate = getWeekStartDate();
            Date weekEndDate = getWeekEndDate();
            Calendar calendar = Calendar.getInstance();
            while(weekStartDate.before(weekEndDate)){
                calendar.setTime(weekStartDate);
                DateTime currentDateTime = new DateTime(weekStartDate);
                calendar.add(Calendar.DATE, 1);
                weekStartDate = calendar.getTime();
                if(currentDateTime.getDayOfYear() >= beginDateTime.getDayOfYear() &&
                        currentDateTime.getDayOfYear() <= endDateTime.getDayOfYear()){

                    if(range.has(Tags.COOLING_USER_LIMIT_MAX)) {

                        HDictBuilder hDictDay = new HDictBuilder()
                                .add(Tags.DAY, HNum.make(dayNumber))
                                .add(Tags.STHH, HNum.make(beginHour))
                                .add(Tags.STMM, HNum.make(beginMin))
                                .add(Tags.ETHH, HNum.make(endHour))
                                .add(Tags.ETMM, HNum.make(endMin))
                                .add(Tags.COOLVAL, HNum.make(Double.parseDouble(range.get(Tags.COOLVAL).toString())))
                                .add(Tags.HEATVAL, HNum.make(Double.parseDouble(range.get(Tags.HEATVAL).toString())))
                                .add(Tags.COOLING_USER_LIMIT_MAX, HNum.make(Double.parseDouble(range.get(Tags.COOLING_USER_LIMIT_MAX).toString())))
                                .add(Tags.COOLING_USER_LIMIT_MIN, HNum.make(Double.parseDouble(range.get(Tags.COOLING_USER_LIMIT_MIN).toString())))
                                .add(Tags.HEATING_USER_LIMIT_MAX, HNum.make(Double.parseDouble(range.get(Tags.HEATING_USER_LIMIT_MAX).toString())))
                                .add(Tags.HEATING_USER_LIMIT_MIN, HNum.make(Double.parseDouble(range.get(Tags.HEATING_USER_LIMIT_MIN).toString())))
                                .add(Tags.COOLING_DEADBAND, HNum.make(Double.parseDouble(range.get("coolingDeadband") != null ? range.get("coolingDeadband").toString() : "2.0")))
                                .add(Tags.HEATING_DEADBAND, HNum.make(Double.parseDouble(range.get("heatingDeadband") != null ? range.get("heatingDeadband").toString() : "2.0")));
                        daysList.add(Schedule.Days.parseSingleDay(hDictDay.toDict()));
                    }else{
                        HDictBuilder hDictDay = new HDictBuilder()
                                .add(Tags.DAY, HNum.make(dayNumber))
                                .add(Tags.STHH, HNum.make(beginHour))
                                .add(Tags.STMM, HNum.make(beginMin))
                                .add(Tags.ETHH, HNum.make(endHour))
                                .add(Tags.ETMM, HNum.make(endMin))
                                .add(Tags.COOLVAL, HNum.make(Double.parseDouble(range.get(Tags.COOLVAL).toString())))
                                .add(Tags.HEATVAL, HNum.make(Double.parseDouble(range.get(Tags.HEATVAL).toString())));
                                daysList.add(Schedule.Days.parseSingleDay(hDictDay.toDict()));
                    }

                }
                dayNumber++;
            }
        }
        return daysList;
    }

    public static List<Schedule.Days> getSpecialScheduleDaysForZone(String zoneId){
        return getSpecialScheduleDaysForRunningWeek(zoneId);
    }

    private static Schedule.Days getSpecialScheduleDays(HashMap<Object, Object> specialSchedule) {
        HDict range = (HDict) specialSchedule.get(Tags.RANGE);
        int beginHour = getInt(range.get(Tags.STHH).toString());
        int beginMin = getInt(range.get(Tags.STMM).toString());
        int endHour = getInt(range.get(Tags.ETHH).toString());
        int endMin = getInt(range.get(Tags.ETMM).toString());
        int dayNumber = 0; //0-6 (Monday-Sunday) Schedule->days->day
        //based on the startdate of the special schedule, get the day of the week
        String beginDate = range.get(Tags.STDT).toString();
        LocalDate startDate = LocalDate.parse(beginDate);
        int dayOfWeek = startDate.getDayOfWeek();
        dayNumber = dayOfWeek - 1;

        HDictBuilder hDictDay = new HDictBuilder()
                .add(Tags.DAY, HNum.make(dayNumber))
                .add(Tags.STHH, HNum.make(beginHour))
                .add(Tags.STMM, HNum.make(beginMin))
                .add(Tags.ETHH, HNum.make(endHour))
                .add(Tags.ETMM, HNum.make(endMin))
                .add(Tags.COOLVAL, HNum.make(Double.parseDouble(range.get(Tags.COOLVAL).toString())))
                .add(Tags.HEATVAL, HNum.make(Double.parseDouble(range.get(Tags.HEATVAL).toString())));

          if (range.has(Tags.COOLING_USER_LIMIT_MAX)) {
            hDictDay.add(Tags.COOLING_USER_LIMIT_MAX, HNum.make(Double.parseDouble(range.get(Tags.COOLING_USER_LIMIT_MAX).toString())));
            hDictDay.add(Tags.COOLING_USER_LIMIT_MIN, HNum.make(Double.parseDouble(range.get(Tags.COOLING_USER_LIMIT_MIN).toString())));
            hDictDay.add(Tags.HEATING_USER_LIMIT_MAX, HNum.make(Double.parseDouble(range.get(Tags.HEATING_USER_LIMIT_MAX).toString())));
            hDictDay.add(Tags.HEATING_USER_LIMIT_MIN, HNum.make(Double.parseDouble(range.get(Tags.HEATING_USER_LIMIT_MIN).toString())));
            hDictDay.add(Tags.COOLING_DEADBAND, HNum.make(Double.parseDouble(range.get("coolingDeadband") != null ? range.get("coolingDeadband").toString() : "2.0")));
            hDictDay.add(Tags.HEATING_DEADBAND, HNum.make(Double.parseDouble(range.get("heatingDeadband") != null ? range.get("heatingDeadband").toString() : "2.0")));
           }
            return Schedule.Days.parseSingleDay(hDictDay.toDict());
        }
    private static Schedule createScheduleForSpecialSchedule(List<Schedule.Days> specialScheduleForZone, boolean isZone){
        Schedule specialSchedule = null;
        if(specialScheduleForZone.size() > 0){
            Schedule.Days[] zonesDaySchedule = new Schedule.Days[specialScheduleForZone.size()];
            zonesDaySchedule = specialScheduleForZone.toArray(zonesDaySchedule);
            List<HDict> specialScheduleList = new ArrayList<>();
            for(int i=0; i < specialScheduleForZone.size(); i++) {
                HDictBuilder hDictDay;
                if (zonesDaySchedule[i].getCoolingUserLimitMin() != null){
                    hDictDay = new HDictBuilder()
                            .add(Tags.DAY, HNum.make(zonesDaySchedule[i].getDay()))
                            .add(Tags.STHH, HNum.make(zonesDaySchedule[i].getSthh()))
                            .add(Tags.STMM, HNum.make(zonesDaySchedule[i].getStmm()))
                            .add(Tags.ETHH, HNum.make(zonesDaySchedule[i].getEthh()))
                            .add(Tags.ETMM, HNum.make(zonesDaySchedule[i].getEtmm()))
                            .add(Tags.COOLVAL, HNum.make(zonesDaySchedule[i].getCoolingVal()))
                            .add(Tags.HEATVAL, HNum.make(zonesDaySchedule[i].getHeatingVal()))
                            .add(Tags.COOLING_USER_LIMIT_MIN, HNum.make(zonesDaySchedule[i].getCoolingUserLimitMin()))
                            .add(Tags.COOLING_USER_LIMIT_MAX, HNum.make(zonesDaySchedule[i].getCoolingUserLimitMax()))
                            .add(Tags.HEATING_USER_LIMIT_MIN, HNum.make(zonesDaySchedule[i].getHeatingUserLimitMin()))
                            .add(Tags.HEATING_USER_LIMIT_MAX, HNum.make(zonesDaySchedule[i].getHeatingUserLimitMax()))
                            .add(Tags.COOLING_DEADBAND, HNum.make(zonesDaySchedule[i].getCoolingDeadBand()))
                            .add(Tags.HEATING_DEADBAND, HNum.make(zonesDaySchedule[i].getHeatingDeadBand()));
            }else{
                    hDictDay = new HDictBuilder()
                            .add(Tags.DAY, HNum.make(zonesDaySchedule[i].getDay()))
                            .add(Tags.STHH, HNum.make(zonesDaySchedule[i].getSthh()))
                            .add(Tags.STMM, HNum.make(zonesDaySchedule[i].getStmm()))
                            .add(Tags.ETHH, HNum.make(zonesDaySchedule[i].getEthh()))
                            .add(Tags.ETMM, HNum.make(zonesDaySchedule[i].getEtmm()))
                            .add(Tags.COOLVAL, HNum.make(zonesDaySchedule[i].getCoolingVal()))
                            .add(Tags.HEATVAL, HNum.make(zonesDaySchedule[i].getHeatingVal()));
            }
                specialScheduleList.add(hDictDay.toDict());
            }
            HList hList = HList.make(specialScheduleList);
            HDictBuilder hDictBuilder = new HDictBuilder()
                    .add(Tags.ID, UUID.randomUUID().toString())
                    .add("kind", "Number")
                    .add("temp")
                    .add("heating")
                    .add("cooling")
                    .add("specialschedule")
                    .add("dis", "Special Schedule")
                    .add("days", hList)
                    .add("siteRef", CCUHsApi.getInstance().getSiteIdRef());
            if(isZone){
                hDictBuilder.add("zone");
                hDictBuilder.add("ccuRef", CCUHsApi.getInstance().getCcuId());
            }
            else{
                hDictBuilder.add("building");
            }
            specialSchedule = new Schedule.Builder().setHDict(hDictBuilder.toDict()).build();
        }
        return specialSchedule;
    }

    public static Schedule getScheduleForZone(String zoneId, boolean vacation) {
        HashMap<Object, Object> zoneHashMap = CCUHsApi.getInstance().readMapById(zoneId);
        Zone zone = new Zone.Builder().setHashMap(zoneHashMap).build();
        String ref;
        if (vacation)
            ref = zone.getVacationRef();
        else
            ref = zone.getScheduleRef();

        Double scheduleType = CCUHsApi.getInstance().readPointPriorityValByQuery("point and scheduleType " +
                "and roomRef == \""+ StringUtils.prependIfMissing(zoneId, "@")+"\"");
        //ScheduleType enum is not reachable in haystack module ,hence using hardcoded ordinal value.
        if (ref != null && !ref.equals("") && scheduleType != null && scheduleType.intValue() != 0) {
            Schedule schedule = CCUHsApi.getInstance().getScheduleById(ref);
            
            if (schedule != null )
            {
                //CcuLog.i("CCU_SCHEDULE", "Zone Schedule: for "+zone.getDisplayName()+" : "+ schedule.toString());
                return schedule;
            }
        }

        CcuLog.d("CCU_SCHEDULE", "Referenced schedule does not exist : ref "+ref);
        List<Schedule> zoneSchedules = CCUHsApi.getInstance().getZoneSchedule(zone.getId(), false);
        if (!zoneSchedules.isEmpty()) {
            CcuLog.i("CCU_SCHEDULE", "Default zone schedule:  " + zoneSchedules.get(0));
            return zoneSchedules.get(0);
        }

        CcuLog.e("CCU_SCHEDULE", " !! A zone without valid schedule : something is broken for "+zone.getDisplayName());
        CcuLog.d("Schedule", " ScheduleType is null returning defaultnamed");
        return CCUHsApi.getInstance().getDefaultNamedSchedule();
    }

    private static boolean isLessPriorityScheduleAvailableOnTheDayMorePrioritySchedulePresent(Schedule.Days lessPrioritySchedule,
                                                                                  Set<Schedule.Days> morePriorityScheduleList){
        boolean isPresent = false;
        for(Schedule.Days morePrioritySchedule : morePriorityScheduleList){
            if(morePrioritySchedule.getDay() == lessPrioritySchedule.getDay()){
                isPresent = true;
                break;
            }
        }
        return isPresent;
    }

    private static boolean isScheduleColliding(Schedule.Days morePrioritySchedule, Schedule.Days lessPrioritySchedule){
        /*In Schedule Entity days are stored in 0-6(Monday to Sunday) and in Joda time, it is 1-7(Monday to Sunday).
        Hence +1*/
        int dayAdjustConst = 1;
        DateTime morePriorityScheduleBeginTime =
                new DateTime().withDayOfWeek(morePrioritySchedule.getDay() + dayAdjustConst)
                        .withTime(morePrioritySchedule.getSthh(), morePrioritySchedule.getStmm(),
                0, 0);
        DateTime morePriorityScheduleEndTime =
        new DateTime().withDayOfWeek(morePrioritySchedule.getDay() + dayAdjustConst)
                .withTime(getEndHour(morePrioritySchedule.getEthh()),
                        getEndMinute(morePrioritySchedule.getEthh(), morePrioritySchedule.getEtmm()),
                        getEndSec(morePrioritySchedule.getEthh()), 0);
        Interval morePriorityScheduleInterval = new Interval(morePriorityScheduleBeginTime,
                morePriorityScheduleEndTime);

        DateTime lessPriorityScheduleBeginTime =
                new DateTime().withDayOfWeek(lessPrioritySchedule.getDay() + dayAdjustConst)
                        .withTime(lessPrioritySchedule.getSthh(), lessPrioritySchedule.getStmm(),
                0, 0);
        DateTime lessPriorityScheduleEndTime =
                new DateTime().withDayOfWeek(lessPrioritySchedule.getDay() + dayAdjustConst)
                        .withTime(getEndHour(lessPrioritySchedule.getEthh()),
                                getEndMinute(lessPrioritySchedule.getEthh(), lessPrioritySchedule.getEtmm()),
                                getEndSec(lessPrioritySchedule.getEthh()), 0);
        Interval lessPriorityScheduleInterval = new Interval(lessPriorityScheduleBeginTime,
                lessPriorityScheduleEndTime);
        return morePriorityScheduleInterval.overlaps(lessPriorityScheduleInterval);
    }

    private static void combineSchedules(Set<Schedule.Days> morePriorityScheduleList, Set<Schedule.Days> lessPriorityScheduleList,
                                         Set<Schedule.Days> intermediateScheduleList, Set<Schedule.Days> daysList){
        for(Schedule.Days morePrioritySchedule : morePriorityScheduleList){
            for(Schedule.Days lessPrioritySchedule : lessPriorityScheduleList){
                if (!isLessPriorityScheduleAvailableOnTheDayMorePrioritySchedulePresent(lessPrioritySchedule,
                        morePriorityScheduleList)) {
                    daysList.add(lessPrioritySchedule);
                }
                else if(morePrioritySchedule.getDay() == lessPrioritySchedule.getDay()){
                    boolean scheduleCollision = isScheduleColliding(morePrioritySchedule, lessPrioritySchedule);
                    if(scheduleCollision && isLessPriorityScheduleWithinMorePrioritySchedule(morePrioritySchedule,
                            lessPrioritySchedule)){
                        continue;
                    }
                    else if(scheduleCollision && isLessPriorityScheduleBeginsBeforeMorePrioritySchedule(
                            morePrioritySchedule, lessPrioritySchedule)){
                        Schedule.Days firstSplitSchedule = new Schedule.Days();
                        firstSplitSchedule.setDay(lessPrioritySchedule.getDay());
                        firstSplitSchedule.setSthh(lessPrioritySchedule.getSthh());
                        firstSplitSchedule.setStmm(lessPrioritySchedule.getStmm());
                        firstSplitSchedule.setEthh(morePrioritySchedule.getSthh());
                        firstSplitSchedule.setEtmm(morePrioritySchedule.getStmm());
                        firstSplitSchedule.setCoolingVal(lessPrioritySchedule.getCoolingVal());
                        firstSplitSchedule.setHeatingVal(lessPrioritySchedule.getHeatingVal());
                        intermediateScheduleList.add(firstSplitSchedule);
                    }
                    else if(scheduleCollision && isLessPriorityScheduleEndsAfterMorePrioritySchedule(
                            morePrioritySchedule, lessPrioritySchedule)){
                        Schedule.Days firstSplitSchedule = new Schedule.Days();
                        firstSplitSchedule.setDay(lessPrioritySchedule.getDay());
                        firstSplitSchedule.setSthh(morePrioritySchedule.getEthh());
                        firstSplitSchedule.setStmm(morePrioritySchedule.getEtmm());
                        firstSplitSchedule.setEthh(lessPrioritySchedule.getEthh());
                        firstSplitSchedule.setEtmm(lessPrioritySchedule.getEtmm());
                        firstSplitSchedule.setCoolingVal(lessPrioritySchedule.getCoolingVal());
                        firstSplitSchedule.setHeatingVal(lessPrioritySchedule.getHeatingVal());
                        intermediateScheduleList.add(firstSplitSchedule);
                    }
                    else if(scheduleCollision && isLessPriorityScheduleBeginsBeforeAndEndsAfterMorePrioritySchedule(
                            morePrioritySchedule, lessPrioritySchedule)){
                        Schedule.Days firstSplitSchedule = new Schedule.Days();
                        firstSplitSchedule.setDay(lessPrioritySchedule.getDay());
                        firstSplitSchedule.setSthh(lessPrioritySchedule.getSthh());
                        firstSplitSchedule.setStmm(lessPrioritySchedule.getStmm());
                        firstSplitSchedule.setEthh(morePrioritySchedule.getSthh());
                        firstSplitSchedule.setEtmm(morePrioritySchedule.getStmm());
                        firstSplitSchedule.setCoolingVal(lessPrioritySchedule.getCoolingVal());
                        firstSplitSchedule.setHeatingVal(lessPrioritySchedule.getHeatingVal());
                        intermediateScheduleList.add(firstSplitSchedule);
                        Schedule.Days secondSplitSchedule = new Schedule.Days();
                        secondSplitSchedule.setDay(lessPrioritySchedule.getDay());
                        secondSplitSchedule.setSthh(morePrioritySchedule.getEthh());
                        secondSplitSchedule.setStmm(morePrioritySchedule.getEtmm());
                        secondSplitSchedule.setEthh(lessPrioritySchedule.getEthh());
                        secondSplitSchedule.setEtmm(lessPrioritySchedule.getEtmm());
                        secondSplitSchedule.setCoolingVal(lessPrioritySchedule.getCoolingVal());
                        secondSplitSchedule.setHeatingVal(lessPrioritySchedule.getHeatingVal());
                        intermediateScheduleList.add(secondSplitSchedule);
                    }
                    else if(!isScheduleColliding(morePriorityScheduleList, lessPrioritySchedule)){
                        daysList.add(lessPrioritySchedule);
                    }

                }
            }
        }
    }

    private static Set<Schedule.Days> schedulesWithPriority(Set<Schedule.Days> morePriorityScheduleList,
                                                             Set<Schedule.Days> lessPriorityScheduleList){
        Set<Schedule.Days> daysList = new TreeSet<>(sortSchedules());
        daysList.addAll(morePriorityScheduleList);
        Set<Schedule.Days> intermediateScheduleList = new TreeSet<>(sortSchedules());
        combineSchedules(morePriorityScheduleList, lessPriorityScheduleList, intermediateScheduleList, daysList);
        Set<Schedule.Days> intermediateScheduleListWithoutCollision = new TreeSet<>(sortSchedules());
        combineSchedules(morePriorityScheduleList, intermediateScheduleList, intermediateScheduleListWithoutCollision, daysList);
        for(Schedule.Days intermediateSchedule : intermediateScheduleListWithoutCollision){
            if(!isScheduleColliding(morePriorityScheduleList, intermediateSchedule)){
                daysList.add(intermediateSchedule);
            }
        }
       return daysList;
    }
    private static boolean isScheduleColliding(Set<Days> morePriorityScheduleList, Days lessPrioritySchedule){
        boolean scheduleCollision = false;
        for(Days morePrioritySchedule : morePriorityScheduleList){
            scheduleCollision = isScheduleColliding(morePrioritySchedule, lessPrioritySchedule);
            if(scheduleCollision){
                break;
            }
        }
        return scheduleCollision;
    }

    private static boolean isLessPriorityScheduleWithinMorePrioritySchedule(Days morePrioritySchedule,
                                                                            Days lessPrioritySchedule) {
        return ((lessPrioritySchedule.getSthh() == morePrioritySchedule.getSthh() &&
                lessPrioritySchedule.getStmm() >= morePrioritySchedule.getStmm())
                || lessPrioritySchedule.getSthh() > morePrioritySchedule.getSthh())
                &&
                ((lessPrioritySchedule.getEthh() == morePrioritySchedule.getEthh() &&
                lessPrioritySchedule.getEtmm() <= morePrioritySchedule.getEtmm())
                || lessPrioritySchedule.getEthh() < morePrioritySchedule.getEthh());
    }

    private static boolean isLessPriorityScheduleBeginsBeforeMorePrioritySchedule(Days morePrioritySchedule,
                                                                             Days lessPrioritySchedule) {
        return ((lessPrioritySchedule.getSthh() == morePrioritySchedule.getSthh() &&
                lessPrioritySchedule.getStmm() < morePrioritySchedule.getStmm())
                || lessPrioritySchedule.getSthh() < morePrioritySchedule.getSthh())
                &&
                ((lessPrioritySchedule.getEthh() == morePrioritySchedule.getEthh() &&
                        lessPrioritySchedule.getEtmm() <= morePrioritySchedule.getEtmm())
                        || lessPrioritySchedule.getEthh() < morePrioritySchedule.getEthh());

    }

    private static boolean isLessPriorityScheduleEndsAfterMorePrioritySchedule(Days morePrioritySchedule,
                                                                            Days lessPrioritySchedule) {
        return ((lessPrioritySchedule.getSthh() == morePrioritySchedule.getSthh() &&
                lessPrioritySchedule.getStmm() >= morePrioritySchedule.getStmm())
                || lessPrioritySchedule.getSthh() > morePrioritySchedule.getSthh())
                &&
                ((lessPrioritySchedule.getEthh() == morePrioritySchedule.getEthh() &&
                lessPrioritySchedule.getEtmm() >= morePrioritySchedule.getEtmm())
                ||(lessPrioritySchedule.getEthh() > morePrioritySchedule.getEthh()));
    }

    private static boolean isLessPriorityScheduleBeginsBeforeAndEndsAfterMorePrioritySchedule(Days morePrioritySchedule,
                                                                                              Days lessPrioritySchedule) {
        return ((lessPrioritySchedule.getSthh() == morePrioritySchedule.getSthh() &&
                lessPrioritySchedule.getStmm() >= morePrioritySchedule.getStmm())
                || lessPrioritySchedule.getSthh() < morePrioritySchedule.getSthh())
                &&
                ((lessPrioritySchedule.getEthh() == morePrioritySchedule.getEthh() &&
                        lessPrioritySchedule.getEtmm() >= morePrioritySchedule.getEtmm())
                        || lessPrioritySchedule.getEthh() > morePrioritySchedule.getEthh());
    }

    private static Comparator<Schedule.Days> sortSchedules() {
        return (o1, o2) -> {
            int dayResult = o1.getDay() - o2.getDay();
            if (dayResult == 0) {
                int startHour = o1.getSthh() - o2.getSthh();
                if (startHour == 0) {
                    int startMin = o1.getStmm() - o2.getStmm();
                    if (startMin == 0) {
                        int endHour = o1.getEthh() - o2.getEthh();
                        if (endHour == 0) {
                            return o1.getEtmm() - o2.getEtmm();
                        }
                        return endHour;
                    }
                    return startMin;
                }
                return startHour;
            }
            return dayResult;
        };
    }
    public static Set<Days> combineSpecialSchedules(String zoneId){
        Set<Days> specialScheduleForZone = new TreeSet<>(sortSchedules());
        Set<Days> specialScheduleForBuilding =  new TreeSet<>(sortSchedules());
         if(!(zoneId.contains("@"))){
            zoneId = "@" + zoneId;
         }
         //Zone level special schedules
        List<HashMap<Object, Object>> specialScheduleZone = CCUHsApi.getInstance().getSpecialSchedules(zoneId);
        for(HashMap<Object, Object> specialSchedule : specialScheduleZone){
            if(isSpecialScheduleForCurrentDay(specialSchedule)){
                specialScheduleForZone.add(getSpecialScheduleDays(specialSchedule));
            }
        }
        //Building level special schedules
        List<HashMap<Object, Object>> specialScheduleBuilding = CCUHsApi.getInstance().getSpecialSchedules(null);
        for(HashMap<Object, Object> specialSchedule : specialScheduleBuilding){
            if(isSpecialScheduleForCurrentDay(specialSchedule)){
                specialScheduleForBuilding.add(getSpecialScheduleDays(specialSchedule));
            }
        }
        Set<Days> combinedSpecialSchedules =  new TreeSet<>(sortSchedules());
        if(!specialScheduleForZone.isEmpty() && !specialScheduleForBuilding.isEmpty()){
            combinedSpecialSchedules.addAll(schedulesWithPriority(specialScheduleForZone, specialScheduleForBuilding));
        }
        else if(specialScheduleForZone.isEmpty()){
            combinedSpecialSchedules.addAll(specialScheduleForBuilding);
        }
        else if(specialScheduleForBuilding.isEmpty()){
            combinedSpecialSchedules.addAll(specialScheduleForZone);
        }
        return combinedSpecialSchedules;
    }

    private static boolean isSpecialScheduleForCurrentDay(HashMap<Object, Object> specialSchedule){
        HDict range = (HDict) specialSchedule.get(Tags.RANGE);
        int beginHour = getInt(range.get(Tags.STHH).toString());
        int beginMin = getInt(range.get(Tags.STMM).toString());
        String beginDate = range.get(Tags.STDT).toString();
        LocalDate startDate = LocalDate.parse(beginDate);  // Assuming the date is in ISO format (YYYY-MM-DD)
        LocalTime startTime = LocalTime.parse(beginHour+ ":" + beginMin);
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        return startDate.equals(currentDate) && !currentTime.isBefore(startTime);
    }
    public static Schedule getScheduleForZoneScheduleProcessing(String zoneId)
    {
        Set<Schedule.Days> combinedSpecialSchedules =  combineSpecialSchedules(zoneId);
        HashMap<Object, Object> zoneHashMap = CCUHsApi.getInstance().readMapById(zoneId);
        Zone build = new Zone.Builder().setHashMap(zoneHashMap).build();
        String ref = build.getScheduleRef();

        if (ref != null && !ref.equals(""))
        {
            Schedule schedule = CCUHsApi.getInstance().getScheduleById(ref);
            if (schedule != null)
            {
                schedule = mergeSpecialScheduleWithZoneSchedule(combinedSpecialSchedules, schedule, true);
                CcuLog.d("Schedule", "Zone Schedule with special schedule: for "+build.getDisplayName()+" : "
                        + schedule.toString());
                return schedule;
            }
        }
        CcuLog.d("Schedule", " Zone Schedule disabled:  get Building Schedule");

        CcuLog.d("Schedule", " Zone Id ="+zoneId);
        HashMap<Object,Object> scheduleTypePoint = CCUHsApi.getInstance().readEntity("scheduleType and roomRef == \"" + zoneId + "\"");
        if(scheduleTypePoint.isEmpty()){
            CcuLog.d("Schedule", " ScheduleType is null returning defaultnamed");
            return CCUHsApi.getInstance().getDefaultNamedSchedule();
        }

        double scheduleType = CCUHsApi.getInstance().readPointPriorityValByQuery("scheduleType and roomRef == \"" + zoneId + "\"");
        if (scheduleType == 2) {
            Schedule schedule = CCUHsApi.getInstance().getScheduleById(ref);
            if (schedule != null) {
                schedule = mergeSpecialScheduleWithZoneSchedule(combinedSpecialSchedules, schedule, false);
                CcuLog.d("Schedule", "Building Schedule with special schedule:  " + schedule);
                return schedule;
            }
        }
        CcuLog.d("Schedule", " ScheduleType is null returning defaultnamed");
        return CCUHsApi.getInstance().getDefaultNamedSchedule();
    }

    private static Schedule mergeSpecialScheduleWithZoneSchedule(Set<Schedule.Days> combinedSpecialSchedules,
                                                                 Schedule zoneSchedule, boolean isZone){
        if(combinedSpecialSchedules.isEmpty()){
            CcuLog.i("CCU_SCHEDULER","schedule with out special schedule "+zoneSchedule);
            return zoneSchedule;
        }
        Set<Schedule.Days> zoneScheduleDays = new TreeSet<>(sortSchedules());
        zoneScheduleDays.addAll(zoneSchedule.getDays());
        /*
        The below loop is to handle overnight schedule and ONS stands for overnight schedule
         */
        List<Days> nextDayONSList = new ArrayList<>();
        for (Schedule.Days days : zoneScheduleDays) {
            if (days.getEthh() < days.getSthh() || (days.getEthh() == days.getSthh()
                    && days.getEtmm() < days.getStmm())) {
                Days nextDayONS = new Days();
                nextDayONS.setDay((days.getDay()+1) % 7);
                nextDayONS.setSthh(0);
                nextDayONS.setStmm(0);
                nextDayONS.setEthh(days.getEthh());
                nextDayONS.setEtmm(days.getEtmm());
                nextDayONS.setHeatingVal(days.getHeatingVal());
                nextDayONS.setCoolingVal(days.getCoolingVal());
                nextDayONSList.add(nextDayONS);
                days.setEthh(23);
                days.setEtmm(59);
            }
            nextDayONSList.add(days);
        }
        zoneScheduleDays.addAll(nextDayONSList);
        List<Schedule.Days> intrinsicScheduleDays = new ArrayList<>();
        intrinsicScheduleDays.addAll(schedulesWithPriority(combinedSpecialSchedules, zoneScheduleDays));
        return createScheduleForSpecialSchedule(intrinsicScheduleDays, isZone);
    }
    public static Zone getZoneforEquipId(String equipId)
    {
        HashMap<Object, Object> equipHashMap  = CCUHsApi.getInstance().readMapById(equipId);
        Equip   equip        = new Equip.Builder().setHashMap(equipHashMap).build();
        HashMap<Object, Object> zoneHashMap  = CCUHsApi.getInstance().readMapById(equip.getRoomRef().replace("@", ""));

        Zone build = new Zone.Builder().setHashMap(zoneHashMap).build();

        return build;
    }


    public static Schedule disableScheduleForZone(String zoneId, boolean enabled)
    {
        HashMap<Object, Object> zoneHashMap       = CCUHsApi.getInstance().readMapById(zoneId);
        Zone    build             = new Zone.Builder().setHashMap(zoneHashMap).build();
        boolean currentlyDisabled = build.getMarkers().contains("disabled");

        return null;
    }


    public boolean checkIntersection(ArrayList<Days> daysArrayList)
    {

        ArrayList<Interval> intervalsOfAdditions = getScheduledIntervals(daysArrayList);
        ArrayList<Interval> intervalsOfCurrent   = getScheduledIntervals(getDaysSorted());

        for (Interval additions : intervalsOfAdditions)
        {
            for (Interval current : intervalsOfCurrent)
            {
                boolean hasOverlap = additions.overlaps(current);
                if (hasOverlap)
                {
                    CcuLog.d("CCU_UI"," hasOverlap "+" additions "+additions+" current "+current);
                    return true;
                }
                //If current day is monday , it could conflict with next week's multi-day sunday schedule.
                if (current.getStart().getDayOfWeek() == 7 && additions.getStart().getDayOfWeek() == 1) {
                    DateTime start = new DateTime(additions.getStartMillis()+ 7*24*60*60*1000);
                    DateTime end = new DateTime(additions.getEndMillis()+ 7*24*60*60*1000);
                    additions = new Interval(start, end);
                    hasOverlap = current.overlaps(additions);
                    if (hasOverlap) {
                        return true;
                    }
                } else if (current.getStart().getDayOfWeek() == 1 && additions.getStart().getDayOfWeek() == 7) {
                    DateTime start = new DateTime(current.getStartMillis()+ 7*24*60*60*1000);
                    DateTime end = new DateTime(current.getEndMillis()+ 7*24*60*60*1000);
                    current = new Interval(start, end);
                    hasOverlap = additions.overlaps(current);
                    if (hasOverlap) {
                        return true;
                    }
                }
                
            }
            
        }

        return false;
    }
    
    public ArrayList<Interval> getOverLapInterval(Days day) {
        ArrayList<Interval> intervalsOfCurrent   = getScheduledIntervals(getDaysSorted());
        Interval intervalOfAddition = getScheduledInterval(day);
        ArrayList<Interval> overLaps = new ArrayList<>();
        for (Interval current : intervalsOfCurrent)
        {
            boolean hasOverlap = intervalOfAddition.overlaps(current);
            if (hasOverlap)
            {
                CcuLog.d("CCU_UI"," Current "+current+" new "+intervalOfAddition+" overlaps "+hasOverlap);
                if (current.getStart().minuteOfDay().get() < current.getEnd().minuteOfDay().get())
                {
                    overLaps.add(current.overlap(intervalOfAddition));
                } else {
                    //Multi-day schedule
                    if (intervalOfAddition.getEndMillis() > current.getStartMillis() &&
                                                intervalOfAddition.getStartMillis() < current.getStartMillis())
                    {
                        overLaps.add(new Interval(current.getStartMillis(), intervalOfAddition.getEndMillis()));
                    }
                    else if (intervalOfAddition.getStartMillis() < current.getStartMillis() && intervalOfAddition.getStartMillis() < current.getEndMillis())
                    {
                        overLaps.add(new Interval(intervalOfAddition.getStartMillis(), current.getEndMillis()));
                      //  overLaps.add(new Interval(intervalOfAddition.getStartMillis(), intervalOfAddition.getEndMillis()));
                    }
                    else if (current.getStartMillis() < intervalOfAddition.getStartMillis() && current.getEndMillis() < intervalOfAddition.getEndMillis())
                    {
                        overLaps.add(new Interval(intervalOfAddition.getStartMillis(), current.getEndMillis()));
                    }
                    else if (intervalOfAddition.getStartMillis() < current.getEndMillis())
                    {
                        overLaps.add(new Interval(intervalOfAddition.getStartMillis(), intervalOfAddition.getEndMillis()));
                    }
                }
            }
    
            //If current day is monday , it could conflict with next week's multi-day sunday schedule.
            if (current.getStart().getDayOfWeek() == 7
                && intervalOfAddition.getStart().getDayOfWeek() == 1) {
                DateTime start = new DateTime(intervalOfAddition.getStartMillis()+ 7*24*60*60*1000);
                DateTime end = new DateTime(intervalOfAddition.getEndMillis()+ 7*24*60*60*1000);
                Interval addition = new Interval(start, end);
                hasOverlap = current.overlaps(addition);
                if (hasOverlap) {
                    overLaps.add(current.overlap(addition));
                }
            }else if (current.getStart().getDayOfWeek() == 1 && intervalOfAddition.getStart().getDayOfWeek() == 7) {
                DateTime start = new DateTime(current.getStartMillis()+ 7*24*60*60*1000);
                DateTime end = new DateTime(current.getEndMillis()+ 7*24*60*60*1000);
                current = new Interval(start, end);
                hasOverlap = current.overlaps(intervalOfAddition);
                if (hasOverlap) {
                    overLaps.add(current.overlap(intervalOfAddition));
                }
            }
        }
        return overLaps;
    }
    
    public ArrayList<Interval> getMergedIntervals() {
        return getMergedIntervals(getDaysSorted());
    }
    public ArrayList<Interval> getMergedIntervals(ArrayList<Days> daysSorted) {
    
        ArrayList<Interval> intervals   = getScheduledIntervalsForDays(daysSorted);
        intervals.sort((p1, p2) -> Long.compare(p1.getStartMillis(), p2.getStartMillis()));
        
        Stack<Interval> stack=new Stack<>();
        if (intervals.size() > 0)
        {
            stack.push(intervals.get(0));
            for (int i = 1; i < intervals.size(); i++)
            {
                Interval top = stack.peek();
                Interval interval = intervals.get(i);
                if (top.getEndMillis() < interval.getStartMillis())
                {
                    stack.push(interval);
                }
                else if (top.getEndMillis() < interval.getEndMillis())
                {
                    Interval t = new Interval(top.getStartMillis(), interval.getEndMillis());
                    stack.pop();
                    stack.push(t);
                }
            }
        }
        return new ArrayList<>(stack);
        
    }
    
    private DateTime getTime()
    {
        return new DateTime(MockTime.getInstance().getMockTime());
    }

    public Occupied getCurrentValues()
    {
        Occupied            occupied           = null;
        ArrayList<Days>     daysSorted         = getSplitDays();
        ArrayList<Interval> scheduledIntervals = getScheduledIntervals(daysSorted);

        Collections.sort(scheduledIntervals, new Comparator<Interval>() {
            @Override
            public int compare(Interval lhs, Interval rhs) {
                return Long.compare(lhs.getStart().getMillis(), rhs.getStart().getMillis());
            }
        });

        for (int i = 0; i < daysSorted.size(); i++)
        {
            if (scheduledIntervals.get(i).contains(getTime().getMillis()) || scheduledIntervals.get(i).isAfter(getTime().getMillis()))
            {

                boolean currentlyOccupied = scheduledIntervals.get(i).contains(getTime().getMillis());
                occupied = new Occupied();
                occupied.setOccupied(currentlyOccupied);
                occupied.setValue(daysSorted.get(i).mVal);
                occupied.setCoolingVal(daysSorted.get(i).mCoolingVal);
                occupied.setHeatingVal(daysSorted.get(i).mHeatingVal);

                if (currentlyOccupied)
                {
                    CcuLog.d("CCU_SCHEDULER","Currently Occupied "+daysSorted.get(i)+" Current Time "+getTime());
                    occupied.setCurrentlyOccupiedSchedule(daysSorted.get(i));
                } else
                {
                    occupied.setNextOccupiedSchedule(daysSorted.get(i));
                }

                DateTime startDateTime = new DateTime(MockTime.getInstance().getMockTime())
                        .withHourOfDay(daysSorted.get(i).getSthh())
                        .withMinuteOfHour(daysSorted.get(i).getStmm())
                        .withDayOfWeek(daysSorted.get(i).getDay() + 1)
                        .withSecondOfMinute(0);
                occupied.setMillisecondsUntilNextChange(startDateTime.getMillis() - MockTime.getInstance().getMockTime());
                if( (i != 0) && (scheduledIntervals.get(i-1) != null) && scheduledIntervals.get(i-1).isBefore(getTime().getMillis())){
                    if(daysSorted.get(i-1).getSthh() > daysSorted.get(i-1).getEthh()) {
                        DateTime endDateTime = new DateTime(MockTime.getInstance().getMockTime())
                                .withHourOfDay(getEndHour(daysSorted.get(i - 1).getEthh()))
                                .withMinuteOfHour(getEndMinute(daysSorted.get(i - 1).getEthh(), daysSorted.get(i - 1).getEtmm()))
                                .withDayOfWeek(daysSorted.get(i).getDay() + 1)
                                .withSecondOfMinute(getEndSec(daysSorted.get(i - 1).getEthh()));
                        occupied.setPreviouslyOccupiedSchedule(daysSorted.get(i -1));
                        occupied.setMillisecondsUntilPrevChange(MockTime.getInstance().getMockTime() -endDateTime.getMillis());
					}else {
                        DateTime endDateTime = new DateTime(MockTime.getInstance().getMockTime())
                                .withHourOfDay(getEndHour(daysSorted.get(i - 1).getEthh()))
                                .withMinuteOfHour(getEndMinute(daysSorted.get(i - 1).getEthh(), daysSorted.get(i - 1).getEtmm()))
                                .withDayOfWeek(daysSorted.get(i - 1).getDay() + 1)
                                .withSecondOfMinute(getEndSec(daysSorted.get(i - 1).getEthh()));
                        occupied.setPreviouslyOccupiedSchedule(daysSorted.get(i -1));
                        occupied.setMillisecondsUntilPrevChange(MockTime.getInstance().getMockTime() -endDateTime.getMillis());
                    }
                }
                return occupied;
            }
        }


        /* In case it runs off the ends of the schedule */
        if (daysSorted.size() > 0)
        {
            int j = daysSorted.size() -1;
            occupied = new Occupied();
            occupied.setOccupied(false);
            occupied.setValue(daysSorted.get(0).mVal);
            occupied.setCoolingVal(daysSorted.get(0).mCoolingVal);
            occupied.setHeatingVal(daysSorted.get(0).mHeatingVal);
            occupied.setNextOccupiedSchedule(daysSorted.get(0));
            DateTime startDateTime = new DateTime(MockTime.getInstance().getMockTime())
                    .withHourOfDay(daysSorted.get(0).getSthh())
                    .withMinuteOfHour(daysSorted.get(0).getStmm())
                    .withDayOfWeek(daysSorted.get(0).getDay() + 1)
                    .withSecondOfMinute(0);
            occupied.setMillisecondsUntilNextChange(startDateTime.getMillis() - MockTime.getInstance().getMockTime());

            DateTime endDateTime = new DateTime(MockTime.getInstance().getMockTime())
                    .withHourOfDay(getEndHour(daysSorted.get(j).getEthh()))
                    .withMinuteOfHour(getEndMinute(daysSorted.get(j).getEthh(), daysSorted.get(j).getEtmm()))
                    .withDayOfWeek(daysSorted.get(j).getDay() + 1)
                    .withSecondOfMinute(getEndSec(daysSorted.get(j).getEthh()));
            occupied.setPreviouslyOccupiedSchedule(daysSorted.get(j));
            occupied.setMillisecondsUntilPrevChange(MockTime.getInstance().getMockTime() - endDateTime.getMillis());
            
        }

        return occupied;
    }


    /*{stdt:2018-12-18T10:13:55.185-06:00 Chicago
        dis:"Simple Schedule" etdt:2018-12-18T10:13:55.185-06:00 Chicago
        vacation
        temp
        kind:"Number"
        schedule unit:"\\u00B0F"
        days:[{cooling
        days:[
                {ethh:16 sthh:13 day:0.0 val:68},{ethh:16 sthh:9 day:1.0 etmm:12 val:80 stmm:0.0},
                {sunrise:T day:1.0 val:80 sunset:T},{sunrise:F day:3 val:80 sunset:F},
                {sunrise:T day:3 val:80 sunset:T}]},
                {heating
                    days:
                    [{ethh:16 sthh:13 day:0.0 val:68},
                    {ethh:16 sthh:9 day:1.0 etmm:12 val:80 stmm:0.0},
                    {sunrise:T day:1.0 val:80 sunset:T},{sunrise:F day:3 val:80 sunset:F},{sunrise:T day:3 val:80 sunset:T}]}]}*/

    private String          mId;
    private boolean         mIsVacation;
    private String          mDis;
    private Double unoccupiedZoneSetback;
    private HashSet<String> mMarkers;
    private String          mKind;
    private String          mUnit;
    private ArrayList<Days> mDays = new ArrayList<Days>();

    private DateTime mStartDate;
    private DateTime mEndDate;
    private String ccuRef;

    private String org;
    private int scheduleGroup;

    public String getCcuRef() {
        return ccuRef;
    }

    public String getorg() {
        return org;
    }
    public void setCcuRef(String ccuRef) {
        this.ccuRef = ccuRef;
    }

    public String getRoomRef()
    {
        return mRoomRef;
    }
    public void setRoomRef(String roomRef)
    {
        this.mRoomRef = roomRef;
    }
    private String mRoomRef;


    public String getmSiteId()
    {
        return mSiteId;
    }
    
    public void setmSiteId(String mSiteId)
    {
        this.mSiteId = mSiteId;
    }
    private String mSiteId;

    public String getTZ()
    {
        return mTZ;
    }

    private String mTZ;

    public String getId()
    {
        return mId;
    }
    
    public void setId(String mId) {
        this.mId = mId;
    }

    public boolean isVacation()
    {
        return mIsVacation;
    }

    public String getDis()
    {
        return mDis;
    }

    public Double getUnoccupiedZoneSetback() {
        return unoccupiedZoneSetback ;
    }

    public HashSet<String> getMarkers()
    {
        return mMarkers;
    }

    public String getKind()
    {
        return mKind;
    }

    public String getUnit()
    {
        return mUnit;
    }

    public ArrayList<Days> getDays()
    {
        Collections.sort(mDays, new Comparator<Days>()
        {
            @Override
            public int compare(Days d1, Days d2)
            {
                return d1.mDay - d2.mDay;
            }
        });
        return mDays;
    }
    
    public Days getDay(Days day)
    {
        for (Days d : mDays) {
            if (d.mDay == day.mDay) {
                return d;
            }
        }
        return null;
    }
    
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append(mDis).append("-");
        if (mId != null) {
            b.append(mId).append(" ");
        }
        if (isVacation()) {
            b.append(mStartDate.toString()+"-"+mEndDate.toString());
        }else
        {
            for (Days d : mDays)
            {
                b.append(d.toString());
            }
        }
        for (String m :mMarkers ) {
            b.append(m+" ");
        }
        b.append(mRoomRef);
        return b.toString();
    }
    public Integer getScheduleGroup()
    {
        return scheduleGroup;
    }
    //Get existing intervals for selected days
    public ArrayList<Interval> getScheduledIntervalsForDays(ArrayList<Days> daysSorted) {
        ArrayList<Interval> daysIntervals = new ArrayList<Interval>();
        ArrayList<Interval> allIntervals = getScheduledIntervals(getDaysSorted());
        if(!daysSorted.isEmpty() && ( daysSorted.get(0).getSthh() > daysSorted.get(0).getEthh()
                || (daysSorted.get(0).getSthh() == daysSorted.get(0).getEthh() && daysSorted.get(0).getStmm() > daysSorted.get(0).getEtmm()) ) ) {
            if(daysSorted.get(daysSorted.size()-1).getDay()==6) {

                //overnight scenario
                Interval iv = allIntervals.get(allIntervals.size() - 1);
                DateTime startInterval = iv.getStart();
                DateTime nextDay = startInterval.plusDays(1);
                int nextDayofWeek = nextDay.getDayOfWeek() - 1;
                int monthNext = nextDay.getMonthOfYear();
                int yearNext = nextDay.getYear();
                int dayNext = nextDay.getDayOfMonth();
                BuildingOccupancy boTemp = CCUHsApi.getInstance().getBuildingOccupancy();
                List<BuildingOccupancy.Days> tempDayList = boTemp.getDays();
                BuildingOccupancy.Days individualDay = null;
                for (int i = 0; i < tempDayList.size(); i++) {
                    if (nextDayofWeek == tempDayList.get(i).getDay()) {
                        individualDay = tempDayList.get(i);
                    }
                }
                if (individualDay != null) {
                    int startHr = individualDay.getSthh();
                    int startMin = individualDay.getStmm();
                    int endHr = individualDay.getEthh();
                    int endMin = individualDay.getEtmm();
                    if (endHr == 24) {
                        endHr = 23;
                        endMin = 59;
                    }
                    DateTime startNext = new DateTime(yearNext, monthNext, dayNext, startHr, startMin);
                    DateTime endNext = new DateTime(yearNext, monthNext, dayNext, endHr, endMin);
                    Interval nextDayInterval = new Interval(startNext, endNext);
                    allIntervals.add(nextDayInterval);
                }
            }
            return allIntervals;
        }

            for (Interval i : allIntervals)
        {
            for (Days d : daysSorted)
            {
                if (d.mDay == i.getStart().getDayOfWeek()-1) {
                    daysIntervals.add(i);
                } else if (d.mDay == i.getEnd().getDayOfWeek()-1) {
                    long now = MockTime.getInstance().getMockTime();
                    DateTime startTime = new DateTime(now)
                                                   .withHourOfDay(0)
                                                   .withMinuteOfHour(0)
                                                   .withSecondOfMinute(0).withMillisOfSecond(0).withDayOfWeek(i.getEnd().getDayOfWeek());
                    if (d.mDay == DAYS.MONDAY.ordinal()) {
                        DateTime endTime = new DateTime(now).withHourOfDay(i.getEnd().getHourOfDay())
                                                            .withMinuteOfHour(i.getEnd().getMinuteOfHour())
                                                            .withSecondOfMinute(i.getEnd().getSecondOfMinute())
                                                            .withMillisOfSecond(i.getEnd().getMillisOfSecond()).withDayOfWeek(d.mDay+1);
                        daysIntervals.add(i.toInterval().withStartMillis(startTime.getMillis()).withEndMillis(endTime.getMillis()));
                    }else
                    {
                        daysIntervals.add(i.toInterval().withStartMillis(startTime.getMillis()));
                    }
                }else if (d.mDay == DAYS.SUNDAY.ordinal() &&((d.getSthh()*60 + d.getStmm()) > (d.getEthh()*60+d.getEtmm()))
                                                    && i.getStart().getDayOfWeek() == 1) {
                    if (i.getEnd().getMinuteOfDay() < (d.getEthh()*60+d.getEtmm()))
                    {
                        daysIntervals.add(i);
                    }else if (i.getStart().getMinuteOfDay() < (d.getEthh()*60+d.getEtmm()) ){
                        DateTime endTime = i.getEnd().withHourOfDay(d.getEthh()).withMinuteOfHour(d.getEtmm());
                        daysIntervals.add(i.toInterval().withEndMillis(endTime.getMillis()));
                    }
                }
                
                
            }
        }
        for(Interval i : daysIntervals) {
            CcuLog.d("CCU_UI", "Scheduled interval for days"+i);
        }
        return daysIntervals;
    }
    
    public ArrayList<Interval> getScheduledIntervals() {
        return getScheduledIntervals(getDaysSorted());
    }
    
    public ArrayList<Interval> getScheduledIntervals(ArrayList<Days> daysSorted)
    {
        ArrayList<Interval> intervals = new ArrayList<Interval>();

        for (Days day : daysSorted)
        {

            long now = MockTime.getInstance().getMockTime();

            DateTime startDateTime = new DateTime(now)
                    .withHourOfDay(day.getSthh())
                    .withMinuteOfHour(day.getStmm())
                    .withDayOfWeek(day.getDay() + 1)
                    .withSecondOfMinute(0)
                    .withMillisOfSecond(0);
            DateTime endDateTime = new DateTime(now)
                    .withHourOfDay(getEndHour(day.getEthh()))
                    .withMinuteOfHour(getEndMinute(day.getEthh(), day.getEtmm()))
                    .withSecondOfMinute(getEndSec(day.getEthh())).withMillisOfSecond(0).withDayOfWeek(
                            day.getDay() +
                                    1);

            Interval scheduledInterval = null;
            if (startDateTime.isAfter(endDateTime))
            {
                if (day.getDay() == DAYS.SUNDAY.ordinal()) {
                    if (startDateTime.getWeekOfWeekyear() >= 52){
                        scheduledInterval = new Interval(startDateTime, endDateTime.plusDays(1));

                    }else
                    scheduledInterval = new Interval(startDateTime, endDateTime.withWeekOfWeekyear(startDateTime.getWeekOfWeekyear()+1)
                                                                               .withDayOfWeek(DAYS.values()[day.getDay()].getNextDay().ordinal() + 1));
                } else {
                    scheduledInterval = new Interval(startDateTime, endDateTime.withDayOfWeek(DAYS.values()[day.getDay()].getNextDay().ordinal() + 1));
                }
            } else
            {
                scheduledInterval =
                        new Interval(startDateTime, endDateTime);
            }


            intervals.add(scheduledInterval);
        }

        return intervals;
    }
    
    public Interval getScheduledInterval(Days day)
    {
        
        long now = MockTime.getInstance().getMockTime();
        
        DateTime startDateTime = new DateTime(now)
                                         .withHourOfDay(day.getSthh())
                                         .withMinuteOfHour(day.getStmm())
                                         .withDayOfWeek(day.getDay() + 1)
                                         .withSecondOfMinute(0)
                                         .withMillisOfSecond(0);
        DateTime endDateTime = new DateTime(now)
                .withHourOfDay(getEndHour(day.getEthh()))
                .withMinuteOfHour(getEndMinute(day.getEthh(), day.getEtmm()))
                .withSecondOfMinute(getEndSec(day.getEthh())).withMillisOfSecond(0).withDayOfWeek(
                        day.getDay() +
                        1);
        
        if (startDateTime.isAfter(endDateTime))
        {
            if (day.getDay() == DAYS.SUNDAY.ordinal()) {
                return new Interval(startDateTime, endDateTime.withWeekOfWeekyear(startDateTime.getWeekOfWeekyear()+1)
                                                                           .withDayOfWeek(DAYS.values()[day.getDay()].getNextDay().ordinal() + 1));
            } else {
                return new Interval(startDateTime, endDateTime.withDayOfWeek(DAYS.values()[day.getDay()].getNextDay().ordinal() + 1));
            }
        } else
        {
            return new Interval(startDateTime, endDateTime);
        }
        
    }
    
    /**
     * Sorts the days by MM, then by HH, then by DD
     *
     * @return Sorted list of days
     */
    public ArrayList<Days> getDaysSorted()
    {

        Collections.sort(mDays, new Comparator<Days>()
        {
            @Override
            public int compare(Days o1, Days o2)
            {
                return Integer.valueOf(o1.getStmm()).compareTo(Integer.valueOf(o2.getStmm()));
            }
        });

        Collections.sort(mDays, new Comparator<Days>()
        {
            @Override
            public int compare(Days o1, Days o2)
            {
                return Integer.valueOf(o1.getSthh()).compareTo(Integer.valueOf(o2.getSthh()));
            }
        });

        Collections.sort(mDays, new Comparator<Days>()
        {
            @Override
            public int compare(Days o1, Days o2)
            {
                return Integer.valueOf(o1.mDay).compareTo(Integer.valueOf(o2.mDay));
            }
        });

        return mDays;
    }

    public ArrayList<Days> getSplitDays(){
        ArrayList<Days> days = new ArrayList<>();

        for (Days day : getDaysSorted()){
            if ((day.getSthh() > day.getEthh() || (day.getSthh() == day.getEthh() && day.getStmm() > day.getEtmm()) ) && (day.getDay() == DAYS.SUNDAY.ordinal())){
                Days d = new Days();
                d.setSthh(day.mSthh);
                d.setStmm(day.mStmm);
                d.setEthh(23);
                d.setEtmm(59);
                d.setDay(day.mDay);
                d.setVal(day.mVal);
                d.setCoolingVal(day.getCoolingVal());
                d.setHeatingVal(day.getHeatingVal());
                d.setHeatingUserLimitMin(day.getHeatingUserLimitMin());
                d.setHeatingUserLimitMax(day.getHeatingUserLimitMax());
                d.setCoolingUserLimitMin(day.getCoolingUserLimitMin());
                d.setCoolingUserLimitMax(day.getCoolingUserLimitMax());
                d.setCoolingDeadBand(day.getCoolingDeadBand());
                d.setHeatingDeadBand(day.getHeatingDeadBand());
                days.add(d);

                Days d2 = new Days();
                d2.setSthh(00);
                d2.setStmm(00);
                d2.setEthh(day.mEthh);
                d2.setEtmm(day.mEtmm);
                d.setVal(day.mVal);

                if (day.getDay() == DAYS.SUNDAY.ordinal()){
                    d2.setDay(0);
                } else {
                    d2.setDay(day.mDay + 1);
                }

                d2.setCoolingVal(day.getCoolingVal());
                d2.setHeatingVal(day.getHeatingVal());
                d2.setHeatingUserLimitMin(day.getHeatingUserLimitMin());
                d2.setHeatingUserLimitMax(day.getHeatingUserLimitMax());
                d2.setCoolingUserLimitMin(day.getCoolingUserLimitMin());
                d2.setCoolingUserLimitMax(day.getCoolingUserLimitMax());
                d2.setCoolingDeadBand(day.getCoolingDeadBand());
                d2.setHeatingDeadBand(day.getHeatingDeadBand());
                days.add(d2);

                continue;
            }

            days.add(day);
        }

        Collections.sort(days, new Comparator<Days>()
        {
            @Override
            public int compare(Days o1, Days o2)
            {
                return Integer.valueOf(o1.getStmm()).compareTo(Integer.valueOf(o2.getStmm()));
            }
        });

        Collections.sort(days, new Comparator<Days>()
        {
            @Override
            public int compare(Days o1, Days o2)
            {
                return Integer.valueOf(o1.getSthh()).compareTo(Integer.valueOf(o2.getSthh()));
            }
        });

        Collections.sort(days, new Comparator<Days>()
        {
            @Override
            public int compare(Days o1, Days o2)
            {
                return Integer.valueOf(o1.mDay).compareTo(Integer.valueOf(o2.mDay));
            }
        });

        return days;
    }

    public void populateIntersections()
    {
        ArrayList<Interval> scheduledIntervals = getScheduledIntervals(getDays());

        for (int i = 0; i < scheduledIntervals.size(); i++)
        {
            for (int ii = 0; ii < scheduledIntervals.size(); ii++)
            {
                if (scheduledIntervals.get(i).getEndMillis() == scheduledIntervals.get(ii).getStartMillis())
                {
                    this.mDays.get(i).setIntersection(true);
                }else if((scheduledIntervals.get(i).getEnd().getDayOfWeek() == scheduledIntervals.get(ii).getStart().getDayOfWeek())
                        && (scheduledIntervals.get(i).getEnd().getHourOfDay() == scheduledIntervals.get(ii).getStart().getHourOfDay())
                        && (scheduledIntervals.get(i).getEnd().getMinuteOfHour() == scheduledIntervals.get(ii).getStart().getMinuteOfHour())){
                    //Multi day schedule intersection check
                    this.mDays.get(i).setIntersection(true);
                }
            }
        }

    }

    public String getEndDateString()
    {
        return mEndDate != null ? mEndDate.toString("y-M-d") : "No end date";
    }

    public String getStartDateString()
    {
        return mStartDate != null ? mStartDate.toString("y-M-d") : "No start date";
    }

    public DateTime getStartDate()
    {
        return mStartDate;
    }

    public DateTime getEndDate()
    {
        return mEndDate;
    }

    public boolean isActiveVacation()
    {
        CcuLog.d("CCU_JOB","isActiveVacation  vacStart "+getStartDate().getMillis()+" vacEnd "+getEndDate().getMillis()+" Curr "+MockTime.getInstance().getMockTime());
        Interval interval = new Interval(getStartDate(), getEndDate());
        return interval.contains(MockTime.getInstance().getMockTime());
    }

    public void setDisabled(boolean disabled)
    {

        if (disabled) mMarkers.add("disabled");
        else mMarkers.remove("disabled");
    }
    
    public boolean getDisabled() {
        return mMarkers.contains("disabled");
    }

    public void setDaysCoolVal(double val, boolean allDays) {
        setDaysTemperatureVal(val, true);
    }

    public void setDaysHeatVal(double val, boolean allDays) {
        setDaysTemperatureVal(val, false);
    }

    private void setDaysTemperatureVal(double val, boolean isCooling) {
        int day = DateTime.now().dayOfWeek().get() - 1;
        Calendar calendar = Calendar.getInstance();
        int curTime = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        for (Days d : mDays) {
            boolean isInScheduleTime = curTime > (d.mSthh * 60 + d.mStmm) && curTime < (d.mEthh * 60 + d.mEtmm);
            if (isMatchingDay(d, day) && isInScheduleTime) {
                if (isCooling) {
                    d.mCoolingVal = val;
                } else {
                    d.mHeatingVal = val;
                }
                CcuLog.d("CCU_JOB", (isCooling ? "Set mCoolingVal" : "Set mHeatingVal") + " : " + val + " day " + d.mDay);
            }
        }
    }

    // Here schedule group 0 is for Seven Days, 1 is for Weekday+Saturday+Sunday,
    // 2 is for Weekday+Weekend, 3 for Everyday
    private boolean isMatchingDay(Days d, int day) {
        switch (scheduleGroup) {
            case 0:
                return d.mDay == day;
            case 1:
                return (TimeUtil.isCurrentDayWeekDay(day) && d.mDay >= DAYS.MONDAY.ordinal() && d.mDay <= DAYS.FRIDAY.ordinal())
                        || (TimeUtil.isCurrentDaySaturday(day) && d.mDay == DAYS.SATURDAY.ordinal())
                        || (!TimeUtil.isCurrentDayWeekDay(day) && !TimeUtil.isCurrentDaySaturday(day) && d.mDay == DAYS.SUNDAY.ordinal());
            case 2:
                return (TimeUtil.isCurrentDayWeekDay(day) && d.mDay >= DAYS.MONDAY.ordinal() && d.mDay <= DAYS.FRIDAY.ordinal())
                        || ((TimeUtil.isCurrentDaySaturday(day) || TimeUtil.isCurrentDaySunday(day))
                        && (d.mDay == DAYS.SATURDAY.ordinal() || d.mDay == DAYS.SUNDAY.ordinal()));
            default:
                return true;
        }
    }

    public static int getCurrentDayOfWeekWithMondayAsStart() {
        Calendar calendar = GregorianCalendar.getInstance();
        switch (calendar.get(Calendar.DAY_OF_WEEK))
        {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            case Calendar.SUNDAY: return 6;
        }
        return 0;
    }

    public static class Builder
    {
        private String mId;

        private boolean         mIsVacation;
        private String          mDis;
        private Double unoccupiedZoneSetback;
        private HashSet<String> mMarkers = new HashSet<String>();
        private String          mKind;
        private String          mUnit;
        private ArrayList<Days> mDays    = new ArrayList<Days>();
        private String          mTZ;
        private String          mSiteId;
        private DateTime        mStartDate;
        private DateTime        mEndDate;
        private String mRoomRef;
        private String ccuRef;
        private HDateTime createdDateTime;
        private HDateTime lastModifiedDateTime;
        private String lastModifiedBy;
        private int scheduleGroup;
    
        public Schedule.Builder setmRoomRef(String mRoomRef)
        {
            this.mRoomRef = mRoomRef;
            return this;
        }

        public Schedule.Builder setCcuRef(String ccuRef)
        {
            this.ccuRef = ccuRef;
            return this;
        }

        public Schedule.Builder setId(String id)
        {
            this.mId = id;
            return this;
        }

        public Schedule.Builder setVacation(boolean vacation)
        {
            this.mIsVacation = vacation;
            return this;
        }

        public Schedule.Builder setDisplayName(String displayName)
        {
            this.mDis = displayName;
            return this;
        }

        public Schedule.Builder setMarkers(HashSet<String> markers)
        {
            this.mMarkers = markers;
            return this;
        }

        public Schedule.Builder addMarker(String marker)
        {
            this.mMarkers.add(marker);
            return this;
        }

        public Schedule.Builder setKind(String kind)
        {
            this.mKind = kind;
            return this;
        }

        //TODO make unit enum / strings
        public Schedule.Builder setUnit(String unit)
        {
            this.mUnit = unit;
            return this;
        }

        public Schedule.Builder setDays(ArrayList<Days> days)
        {
            this.mDays = days;
            return this;
        }

        public Schedule.Builder setTz(String tz)
        {
            this.mTZ = tz;
            return this;
        }
        public Schedule.Builder setScheduleGroup(int scheduleGroup) {
            this.scheduleGroup = scheduleGroup;
            return this;
        }

        public Schedule build()
        {
            Schedule s = new Schedule();
            s.mId = this.mId;
            s.mMarkers = this.mMarkers;
            s.mIsVacation = this.mIsVacation;
            s.mDis = this.mDis;
            s.unoccupiedZoneSetback = this.unoccupiedZoneSetback;
            s.mKind = this.mKind;
            s.mSiteId = this.mSiteId;
            s.mUnit = this.mUnit;
            s.mDays = this.mDays;
            s.mTZ = this.mTZ;
            s.mStartDate = this.mStartDate;
            s.mEndDate = this.mEndDate;
            s.mRoomRef = this.mRoomRef;
            s.ccuRef = this.ccuRef;
            s.setCreatedDateTime(createdDateTime);
            s.setLastModifiedDateTime(lastModifiedDateTime);
            s.setLastModifiedBy(lastModifiedBy);
            s.setScheduleGroup(scheduleGroup);
            return s;
        }


        public Schedule.Builder setHDict(HDict schedule)
        {

            Iterator it = schedule.iterator();
            while (it.hasNext())
            {
                Map.Entry pair = (Map.Entry) it.next();
                //System.out.println(pair.getKey() + " = " + pair.getValue());
                if (pair.getKey().equals("id"))
                {
                    this.mId = pair.getValue().toString().replaceAll("@", "");
                } else if (pair.getKey().equals("dis"))
                {
                    this.mDis = pair.getValue().toString();
                }
                else if (pair.getKey().equals("unoccupiedZoneSetback"))
                {
                    this.unoccupiedZoneSetback = Double.valueOf(pair.getValue().toString());
                }
                else if (pair.getKey().equals("vacation"))
                {
                    this.mIsVacation = true;
                } else if (pair.getKey().equals("kind") && pair.getValue() != null)
                {
                    this.mKind = pair.getValue().toString();
                } else if (pair.getKey().equals("unit"))
                {
                    this.mUnit = pair.getValue().toString();
                } else if (pair.getKey().equals("tz"))
                {
                    this.mTZ = pair.getValue().toString();
                } else if (pair.getKey().equals("days"))
                {
                    this.mDays = Days.parse((HList) pair.getValue());
                } else if (pair.getKey().equals("siteRef"))
                {
                    this.mSiteId = schedule.getRef("siteRef").val;
                } else if (pair.getKey().equals("roomRef"))
                {
                    this.mRoomRef = schedule.getRef("roomRef").toString();
                }
                else if (pair.getKey().equals("ccuRef"))
                {
                    this.ccuRef = CCUHsApi.getInstance().getCcuId();
                }
                else if (pair.getKey().equals("createdDateTime") && !pair.getValue().toString().equals("marker"))
                {
                    this.createdDateTime = HDateTime.make(pair.getValue().toString());
                }
                else if (pair.getKey().equals("lastModifiedDateTime") && !pair.getValue().toString().equals("marker"))
                {
                    this.lastModifiedDateTime = HDateTime.make(pair.getValue().toString());
                }
                else if (pair.getKey().equals("lastModifiedBy") && !pair.getValue().toString().equals("marker"))
                {
                    this.lastModifiedBy = pair.getValue().toString();
                }
                else if(pair.getKey().equals("range"))
                {
                    HDict range = (HDict) schedule.get("range");
                    this.mStartDate = new DateTime((HDateTime.make(range.get("stdt").toString()).millisDefaultTZ()));
                    this.mEndDate = new DateTime((HDateTime.make(range.get("etdt").toString()).millisDefaultTZ()));

                }
                else if (pair.getKey().equals("stdt"))
                {
                    this.mStartDate = new DateTime(((HDateTime) schedule.get("stdt")).millisDefaultTZ());
                } else if (pair.getKey().equals("etdt"))
                {
                    this.mEndDate = new DateTime(((HDateTime) schedule.get("etdt")).millisDefaultTZ());
                } else if (pair.getValue() != null && pair.getValue().toString().equals("marker"))
                {
                    this.mMarkers.add(pair.getKey().toString());
                }
                else if (pair.getKey().equals(SCHEDULE_GROUP)) {
                    this.scheduleGroup = (int)Double.parseDouble(pair.getValue().toString());
                }
            }
            return this;
        }
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Schedule s = (Schedule) o;
        if (this.mDays.size() !=  s.mDays.size()) return false;
        
        for(Days day : this.mDays) {
            if (s.getDay(day).equals(day)) return false;
        }
        
        if (!this.getMarkers().equals(s.getMarkers())) return false;
        
        return true;
    }

    public static class Days
    {


        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Days days = (Days) o;
            return mSthh == days.mSthh &&
                    mStmm == days.mStmm &&
                    mDay == days.mDay &&
                    mEtmm == days.mEtmm &&
                    mEthh == days.mEthh &&
                    mSunrise == days.mSunrise &&
                    mSunset == days.mSunset &&
                    Objects.equals(mVal, days.mVal) &&
                    Objects.equals(mHeatingVal, days.mHeatingVal) &&
                    Objects.equals(mCoolingVal, days.mCoolingVal) &&
                    Objects.equals(heatingUserLimitMin, days.heatingUserLimitMin) &&
                    Objects.equals(heatingUserLimitMax, days.heatingUserLimitMax) &&
                    Objects.equals(coolingUserLimitMin, days.coolingUserLimitMin) &&
                    Objects.equals(coolingUserLimitMax, days.coolingUserLimitMax) &&
                    Objects.equals(coolingDeadBand, days.coolingDeadBand) &&
                    Objects.equals(heatingDeadBand, days.heatingDeadBand);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mSthh, mStmm, mDay, mVal, mHeatingVal, mCoolingVal, mEtmm, mEthh, mSunrise, mSunset,
                    heatingUserLimitMin, heatingUserLimitMax, coolingUserLimitMin, coolingUserLimitMax, coolingDeadBand,
                    heatingDeadBand);
        }


        private boolean mIntersection;
        private int     mSthh;
        private int     mStmm;
        private int     mDay;
        private Double  mVal;
        private Double  mHeatingVal;
        private Double  mCoolingVal;
        private int     mEtmm;
        private int     mEthh;
        private boolean mSunrise;
        private boolean mSunset;
        private Double  heatingUserLimitMin;
        private Double  heatingUserLimitMax;
        private Double  coolingUserLimitMin;
        private Double  coolingUserLimitMax;
        private Double  coolingDeadBand;
        private Double  heatingDeadBand;


        public int getSthh()
        {
            return mSthh;
        }

        public void setSthh(int sthh)
        {
            this.mSthh = sthh;
        }

        public int getStmm()
        {
            return mStmm;
        }

        public void setStmm(int stmm)
        {
            this.mStmm = stmm;
        }

        public int getDay()
        {
            return mDay;
        }

        public void setDay(int day)
        {
            this.mDay = day;
        }

        public Double getVal()
        {
            return mVal;
        }

        public void setVal(Double val)
        {
            this.mVal = val;
        }

        public int getEtmm()
        {
            return mEtmm;
        }

        public void setEtmm(int etmm)
        {
            this.mEtmm = etmm;
        }

        public int getEthh()
        {
            return mEthh;
        }

        public void setEthh(int ethh)
        {
            this.mEthh = ethh;
        }

        public boolean isSunrise()
        {
            return mSunrise;
        }

        public void setSunrise(boolean sunrise)
        {
            this.mSunrise = sunrise;
        }

        public boolean isSunset()
        {
            return mSunset;
        }

        public void setSunset(boolean sunset)
        {
            this.mSunset = sunset;
        }

        public Double getHeatingUserLimitMin() {
            return heatingUserLimitMin;
        }

        public void setHeatingUserLimitMin(Double heatingUserLimitMin) {
            this.heatingUserLimitMin = heatingUserLimitMin;
        }

        public Double getHeatingUserLimitMax() {
            return heatingUserLimitMax;
        }

        public void setHeatingUserLimitMax(Double heatingUserLimitMax) {
            this.heatingUserLimitMax = heatingUserLimitMax;
        }


        public Double getCoolingUserLimitMin() {
            return coolingUserLimitMin;
        }

        public void setCoolingUserLimitMin(Double coolingUserLimitMin) {
            this.coolingUserLimitMin = coolingUserLimitMin;
        }

        public Double getCoolingUserLimitMax() {
            return coolingUserLimitMax;
        }

        public void setCoolingUserLimitMax(Double coolingUserLimitMax) {
            this.coolingUserLimitMax = coolingUserLimitMax;
        }

        public Double getCoolingDeadBand() {
            return coolingDeadBand;
        }

        public void setCoolingDeadBand(Double coolingDeadBand) {
            this.coolingDeadBand = coolingDeadBand;
        }

        public Double getHeatingDeadBand() {
            return heatingDeadBand;
        }

        public void setHeatingDeadBand(Double heatingDeadBand) {
            this.heatingDeadBand = heatingDeadBand;
        }

        public static ArrayList<Days> parse(HList value)
        {
            ArrayList<Days> days = new ArrayList<Days>();
            for (int i = 0; i < value.size(); i++)
            {
                days.add(parseSingleDay((HDict) value.get(i)));
            }

            return days;
        }

        public static Days parseSingleDay(HDict hDict)
        {
            Days days = new Days();

            days.mDay = hDict.getInt("day");
            days.mEthh = hDict.has("ethh") ? hDict.getInt("ethh") : -1;
            days.mEtmm = hDict.has("etmm") ? hDict.getInt("etmm") : -1;
            days.mSthh = hDict.has("sthh") ? hDict.getInt("sthh") : -1;
            days.mStmm = hDict.has("stmm") ? hDict.getInt("stmm") : -1;
            days.mSunrise = hDict.has("sunrise");
            days.mSunset = hDict.has("sunset");

            days.mVal = hDict.has("curVal") ? hDict.getDouble("curVal") : null;
            days.mCoolingVal = hDict.has("coolVal") ? hDict.getDouble("coolVal") : null;
            days.mHeatingVal = hDict.has("heatVal") ? hDict.getDouble("heatVal") : null;
            days.heatingUserLimitMin = hDict.has(Tags.HEATING_USER_LIMIT_MIN) ? hDict.getDouble(Tags.HEATING_USER_LIMIT_MIN) : 67;
            days.heatingUserLimitMax = hDict.has(Tags.HEATING_USER_LIMIT_MAX) ? hDict.getDouble(Tags.HEATING_USER_LIMIT_MAX) : 72;
            days.coolingUserLimitMin = hDict.has(Tags.COOLING_USER_LIMIT_MIN) ? hDict.getDouble(Tags.COOLING_USER_LIMIT_MIN) : 72;
            days.coolingUserLimitMax = hDict.has(Tags.COOLING_USER_LIMIT_MAX) ? hDict.getDouble(Tags.COOLING_USER_LIMIT_MAX) : 77;
            days.coolingDeadBand = hDict.has(Tags.COOLING_DEADBAND) ? hDict.getDouble(Tags.COOLING_DEADBAND) : 2;
            days.heatingDeadBand = hDict.has(Tags.HEATING_DEADBAND) ? hDict.getDouble(Tags.HEATING_DEADBAND) : 2;

            return days;
        }

        public Double getHeatingVal()
        {
            return mHeatingVal;
        }

        public void setHeatingVal(Double heatingVal)
        {
            this.mHeatingVal = heatingVal;
        }

        public Double getCoolingVal()
        {
            return mCoolingVal;
        }

        public void setCoolingVal(Double coolingVal)
        {
            this.mCoolingVal = coolingVal;
        }


        public boolean isIntersection()
        {
            return mIntersection;
        }

        public void setIntersection(boolean mIntersection)
        {
            this.mIntersection = mIntersection;
        }
        
        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append(" {mDay: "+mDay);
            str.append(" Time "+mSthh+":"+mStmm+" - "+mEthh+":"+mEtmm);
            str.append(" heatingVal "+mHeatingVal+" coolingVal "+mCoolingVal);
            str.append(" heatingUserLimitMin :"+heatingUserLimitMin);
            str.append(" heatingUserLimitMax :"+heatingUserLimitMax);
            str.append(" coolingUserLimitMin :"+coolingUserLimitMin);
            str.append(" coolingUserLimitMax :"+coolingUserLimitMax);
            str.append(" coolingDeadBand :"+coolingDeadBand);
            str.append(" heatingDeadBand :"+heatingDeadBand +"}");
            return str.toString();
        }
    }

    public HDict getScheduleHDict()
    {
        if (isVacation()) {
            //range,building,dis,vacation,id,heating,temp,siteRef,schedule,cooling
            //{stdt:2019-07-04T05:00:00Z Rel etdt:2019-07-14T04:59:59Z Rel},M,"vaca1",M,@5d0bd3a5f987526c76b06132 "vaca1",M,M,@5d0ba7e5d099b1630edee18e,M,M
            HTimeZone tz =  HTimeZone.make(CCUHsApi.getInstance().getTimeZone());
            HDict hDict = new HDictBuilder()
                                  .add("stdt", HDateTime.make(mStartDate.getMillis(), tz))
                                  .add("etdt", HDateTime.make(mEndDate.getMillis(), tz)).toDict();

            Log.i("manju","Vacation tz "+tz);
            Log.i("manju","Vacation mStartDate"+mStartDate + "milises: "+mStartDate.getMillis());
            Log.i("manju","Vacation mEndDate"+mEndDate+ "milises: "+mEndDate.getMillis());
            Log.i("manju","Vacation Schedule HDict: "+hDict);
            Log.i("manju","Vacation Schedule without tz: "+HDateTime.make(mStartDate.getMillis()));
            Log.i("manju","Vacation Schedule without tz: "+HDateTime.make(mEndDate.getMillis()));


            Log.i("manju","Vacation Schedule with tz: "+HDateTime.make(mStartDate.getMillis(), tz));
            Log.i("manju","Vacation Schedule with tz: "+HDateTime.make(mEndDate.getMillis(), tz));

            HDictBuilder vacationSchedule = new HDictBuilder()
                                            .add("id", HRef.copy(getId()))
                                            .add("temp")
                                            .add("schedule")
                                            .add("building")
                                            .add("vacation")
                                            .add("cooling")
                                            .add("heating")
                                            .add("range", hDict)
                                            .add("dis", getDis())
                                            .add("siteRef", HRef.copy(mSiteId));

            if (getCreatedDateTime() != null) {
                vacationSchedule.add("createdDateTime", getCreatedDateTime());
            }
            if (getLastModifiedDateTime() != null) {
                vacationSchedule.add("lastModifiedDateTime", getLastModifiedDateTime());
            }
            if (getLastModifiedBy() != null) {
                vacationSchedule.add("lastModifiedBy", getLastModifiedBy());
            }
            return vacationSchedule.toDict();
        }
        
        HDict[] days = new HDict[getDays().size()];

        for (int i = 0; i < getDays().size(); i++)
        {
            Days day = mDays.get(i);
                    HDictBuilder hDictDay = new HDictBuilder()
                    .add("day", HNum.make(day.mDay))
                    .add("sthh", HNum.make(day.mSthh))
                    .add("stmm", HNum.make(day.mStmm))
                    .add("ethh", HNum.make(day.mEthh))
                    .add("etmm", HNum.make(day.mEtmm));
            if (day.heatingUserLimitMin != null)
                hDictDay.add("heatingUserLimitMin", HNum.make(day.heatingUserLimitMin));
            if (day.heatingUserLimitMax != null)
                hDictDay.add("heatingUserLimitMax", HNum.make(day.heatingUserLimitMax));
            if (day.coolingUserLimitMin != null)
                hDictDay.add("coolingUserLimitMin", HNum.make(day.coolingUserLimitMin));
            if (day.coolingUserLimitMax != null)
                hDictDay.add("coolingUserLimitMax", HNum.make(day.coolingUserLimitMax));
            if (day.coolingDeadBand != null)
                hDictDay.add("coolingDeadBand", HNum.make(day.coolingDeadBand));
            if (day.heatingDeadBand != null)
                hDictDay.add("heatingDeadBand", HNum.make(day.heatingDeadBand));
            if (day.mHeatingVal != null)
                hDictDay.add("heatVal", HNum.make(day.getHeatingVal()));
            if (day.mCoolingVal != null)
                hDictDay.add("coolVal", HNum.make(day.getCoolingVal()));
            if (day.mVal != null)
                hDictDay.add("curVal", HNum.make(day.getVal()));

            //need boolean & string support
            if (day.mSunset) hDictDay.add("sunset", day.mSunset);
            if (day.mSunrise) hDictDay.add("sunrise", day.mSunrise);

            days[i] = hDictDay.toDict();
        }

        HList hList = HList.make(days);
        HDictBuilder defaultSchedule = new HDictBuilder()
                .add("id", HRef.copy(getId()))
                .add("kind", getKind())
                .add("dis", getDis())
                .add("days", hList)
                .add("ccuRef", HRef.copy(CCUHsApi.getInstance().getCcuId()));

        if(mSiteId != null)
            defaultSchedule.add("siteRef", HRef.copy(mSiteId));

        if(mRoomRef != null)
            defaultSchedule.add("roomRef", HRef.copy(mRoomRef));

        if (getCreatedDateTime() != null) {
            defaultSchedule.add("createdDateTime", getCreatedDateTime());
        }
        if (getLastModifiedDateTime() != null) {
            defaultSchedule.add("lastModifiedDateTime", getLastModifiedDateTime());
        }
        if (getLastModifiedBy() != null) {
            defaultSchedule.add("lastModifiedBy", getLastModifiedBy());
        }

        for (String marker : getMarkers())
        {
            defaultSchedule.add(marker);
        }

        if(getUnoccupiedZoneSetback() != null) {
            defaultSchedule.add("unoccupiedZoneSetback", getUnoccupiedZoneSetback());
        }
        if(getScheduleGroup() != null) {
            defaultSchedule.add(SCHEDULE_GROUP, getScheduleGroup());
        }

        return defaultSchedule.toDict();
    }
    
    public HDict getZoneScheduleHDict(String roomRef)
    {
        if (isVacation()) {
            //range,building,dis,vacation,id,heating,temp,siteRef,schedule,cooling
            //{stdt:2019-07-04T05:00:00Z Rel etdt:2019-07-14T04:59:59Z Rel},M,"vaca1",M,@5d0bd3a5f987526c76b06132 "vaca1",M,M,@5d0ba7e5d099b1630edee18e,M,M
            HTimeZone tz =  HTimeZone.make(CCUHsApi.getInstance().getTimeZone());
            HDict hDict = new HDictBuilder()
                    .add("stdt", HDateTime.make(mStartDate.getMillis(), tz))
                    .add("etdt", HDateTime.make(mEndDate.getMillis(), tz)).toDict();
            Log.i("manju","Vacation tz "+tz);
            Log.i("manju","Vacation mStartDate"+mStartDate + "milises: "+mStartDate.getMillis());
            Log.i("manju","Vacation mEndDate"+mEndDate+ "milises: "+mEndDate.getMillis());
            Log.i("manju","Vacation Schedule HDict: "+hDict);
            Log.i("manju","Vacation Schedule without tz: "+HDateTime.make(mStartDate.getMillis()));
            Log.i("manju","Vacation Schedule without tz: "+HDateTime.make(mEndDate.getMillis()));


            Log.i("manju","Vacation Schedule with tz: "+HDateTime.make(mStartDate.getMillis(), tz));
            Log.i("manju","Vacation Schedule with tz: "+HDateTime.make(mEndDate.getMillis(), tz));
            HDictBuilder vacationSchedule = new HDictBuilder()
                                             .add("id", HRef.copy(getId()))
                                             .add("temp")
                                             .add("schedule")
                                             .add("zone")
                                             .add("vacation")
                                             .add("cooling")
                                             .add("heating")
                                             .add("range", hDict)
                                             .add("dis", getDis())
                                             .add("siteRef", HRef.copy(mSiteId))
                                             .add("roomRef", HRef.copy(roomRef))
                                             .add("ccuRef", HRef.copy(CCUHsApi.getInstance().getCcuId()));

            if (getCreatedDateTime() != null) {
                vacationSchedule.add("createdDateTime", getCreatedDateTime());
            }
            if (getLastModifiedDateTime() != null) {
                vacationSchedule.add("lastModifiedDateTime", getLastModifiedDateTime());
            }
            if (getLastModifiedBy() != null) {
                vacationSchedule.add("lastModifiedBy", getLastModifiedBy());
            }

            return vacationSchedule.toDict();
        }
        
        HDict[] days = new HDict[getDays().size()];
        
        for (int i = 0; i < getDays().size(); i++)
        {
            Days day = mDays.get(i);
            HDictBuilder hDictDay = new HDictBuilder()
                                            .add("day", HNum.make(day.mDay))
                                            .add("sthh", HNum.make(day.mSthh))
                                            .add("stmm", HNum.make(day.mStmm))
                                            .add("ethh", HNum.make(day.mEthh))
                                            .add("etmm", HNum.make(day.mEtmm));
            if (day.mHeatingVal != null)
                hDictDay.add("heatVal", HNum.make(day.getHeatingVal()));
            if (day.mCoolingVal != null)
                hDictDay.add("coolVal", HNum.make(day.getCoolingVal()));
            if (day.mVal != null)
                hDictDay.add("curVal", HNum.make(day.getVal()));


            //add default value if the days value is null as all zone schedule should have these values
            if (day.heatingUserLimitMin != null)
                hDictDay.add(Tags.HEATING_USER_LIMIT_MIN, HNum.make(day.getHeatingUserLimitMin()));
            else
                hDictDay.add(Tags.HEATING_USER_LIMIT_MIN, 67.0);

            if (day.heatingUserLimitMax != null)
                hDictDay.add(Tags.HEATING_USER_LIMIT_MAX, HNum.make(day.getHeatingUserLimitMax()));
            else
                hDictDay.add(Tags.HEATING_USER_LIMIT_MAX, 72.0);

            if (day.coolingUserLimitMin != null)
                hDictDay.add(Tags.COOLING_USER_LIMIT_MIN, HNum.make(day.getCoolingUserLimitMin()));
            else
                hDictDay.add(Tags.COOLING_USER_LIMIT_MIN, 72.0);

            if (day.coolingUserLimitMax != null)
                hDictDay.add(Tags.COOLING_USER_LIMIT_MAX, HNum.make(day.getCoolingUserLimitMax()));
            else
                hDictDay.add(Tags.COOLING_USER_LIMIT_MAX, 77.0);

            if (day.coolingDeadBand != null)
                hDictDay.add(Tags.COOLING_DEADBAND, HNum.make(day.getCoolingDeadBand()));
            else
                hDictDay.add(Tags.COOLING_DEADBAND, 2.0);

            if (day.heatingDeadBand != null)
                hDictDay.add(Tags.HEATING_DEADBAND, HNum.make(day.getHeatingDeadBand()));
            else
                hDictDay.add(Tags.HEATING_DEADBAND, 2.0);


            //need boolean & string support
            if (day.mSunset) hDictDay.add("sunset", day.mSunset);
            if (day.mSunrise) hDictDay.add("sunrise", day.mSunrise);
            
            days[i] = hDictDay.toDict();
        }
        
        HList hList = HList.make(days);
        HDictBuilder defaultSchedule = new HDictBuilder()
                                               .add("id", HRef.copy(getId()))
                                               .add("kind", getKind())
                                               .add("dis", "Zone Schedule")
                                               .add("days", hList)
                                               .add("roomRef",HRef.copy(roomRef))
                                               .add("siteRef", HRef.copy(mSiteId))
                                               .add("ccuRef", HRef.copy(CCUHsApi.getInstance().getCcuId()));

        if(getUnoccupiedZoneSetback() != null)
            defaultSchedule.add("unoccupiedZoneSetback", getUnoccupiedZoneSetback());
        else
            defaultSchedule.add("unoccupiedZoneSetback", 5.0);

        if (getCreatedDateTime() != null) {
            defaultSchedule.add("createdDateTime", getCreatedDateTime());
        }
        if (getLastModifiedDateTime() != null) {
            defaultSchedule.add("lastModifiedDateTime", getLastModifiedDateTime());
        }
        if (getLastModifiedBy() != null) {
            defaultSchedule.add("lastModifiedBy", getLastModifiedBy());
        }
        if(getScheduleGroup() != null){
            defaultSchedule.add(SCHEDULE_GROUP, getScheduleGroup());
        }
        for (String marker : getMarkers())
        {
            defaultSchedule.add(marker);
        }

        return defaultSchedule.toDict();
    }

    public void setUnoccupiedZoneSetback(Double unoccupiedZoneSetback){
        this.unoccupiedZoneSetback = unoccupiedZoneSetback;
    }
    public void setScheduleGroup(Integer scheduleGroup){
        this.scheduleGroup = scheduleGroup;
    }

    public HDict getNamedScheduleHDict()
    {
        HDict[] days = new HDict[getDays().size()];

        for (int i = 0; i < getDays().size(); i++)
        {
            Days day = mDays.get(i);
            HDictBuilder hDictDay = new HDictBuilder()
                    .add("day", HNum.make(day.mDay))
                    .add("sthh", HNum.make(day.mSthh))
                    .add("stmm", HNum.make(day.mStmm))
                    .add("ethh", HNum.make(day.mEthh))
                    .add("etmm", HNum.make(day.mEtmm));
            if (day.mHeatingVal != null)
                hDictDay.add("heatVal", HNum.make(day.getHeatingVal()));
            if (day.mCoolingVal != null)
                hDictDay.add("coolVal", HNum.make(day.getCoolingVal()));
            if (day.mVal != null)
                hDictDay.add("curVal", HNum.make(day.getVal()));
            if (day.heatingUserLimitMin != null)
                hDictDay.add(Tags.HEATING_USER_LIMIT_MIN, HNum.make(day.getHeatingUserLimitMin()));
            if (day.heatingUserLimitMax != null)
                hDictDay.add(Tags.HEATING_USER_LIMIT_MAX, HNum.make(day.getHeatingUserLimitMax()));
            if (day.coolingUserLimitMin != null)
                hDictDay.add(Tags.COOLING_USER_LIMIT_MIN, HNum.make(day.getCoolingUserLimitMin()));
            if (day.coolingUserLimitMax != null)
                hDictDay.add(Tags.COOLING_USER_LIMIT_MAX, HNum.make(day.getCoolingUserLimitMax()));
            if (day.coolingDeadBand != null)
                hDictDay.add(Tags.COOLING_DEADBAND, HNum.make(day.getCoolingDeadBand()));
            if (day.heatingDeadBand != null)
                hDictDay.add(Tags.HEATING_DEADBAND, HNum.make(day.getHeatingDeadBand()));
            //need boolean & string support
            if (day.mSunset) hDictDay.add("sunset", day.mSunset);
            if (day.mSunrise) hDictDay.add("sunrise", day.mSunrise);

            days[i] = hDictDay.toDict();
        }

        HList hList = HList.make(days);
        HDictBuilder namedSchedule = new HDictBuilder()
                .add("id", HRef.copy(getId()))
                .add("dis", getDis())
                .add("days", hList);

        if(mSiteId != null)
            namedSchedule.add("siteRef", HRef.copy(mSiteId));


        if(getUnoccupiedZoneSetback() != null)
            namedSchedule.add("unoccupiedZoneSetback", getUnoccupiedZoneSetback());

        if (getCreatedDateTime() != null) {
            namedSchedule.add("createdDateTime", getCreatedDateTime());
        }
        if (getLastModifiedDateTime() != null) {
            namedSchedule.add("lastModifiedDateTime", getLastModifiedDateTime());
        }
        if (getLastModifiedBy() != null) {
            namedSchedule.add("lastModifiedBy", getLastModifiedBy());
        }
        for (String marker : getMarkers())
        {
            namedSchedule.add(marker);
        }
        if(getScheduleGroup() != null){
            namedSchedule.add(SCHEDULE_GROUP, getScheduleGroup());
        }


        namedSchedule.add("named");
        namedSchedule.add("schedule");

        Site site = CCUHsApi.getInstance().getSite();
        namedSchedule.add("organization", site.getOrganization());

        return namedSchedule.toDict();
    }

}
