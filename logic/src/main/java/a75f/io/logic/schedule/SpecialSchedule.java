package a75f.io.logic.schedule;


import static a75f.io.api.haystack.util.TimeUtil.getEndHour;
import static a75f.io.api.haystack.util.TimeUtil.getEndMinute;
import static a75f.io.api.haystack.util.TimeUtil.getEndTimeHrStr;
import static a75f.io.api.haystack.util.TimeUtil.getEndTimeMinStr;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.projecthaystack.HDate;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;

public class SpecialSchedule {

    private SpecialSchedule(){

    }
    public static final DateTimeFormatter SS_DATE_TIME_FORMATTER  =  DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final String OVERLAP_DAY = "YYYY-MM-dd";

    public static void createSpecialSchedule(String specialScheduleId, String scheduleName, DateTime startDate,
                                             DateTime endDate, double coolVal, double heatVal, boolean isZone,
                                             String zoneId){

        HRef siteId = CCUHsApi.getInstance().getSiteIdRef();

        boolean isLastHour = endDate.getHourOfDay() == 23 && endDate.getMinuteOfHour() == 59;
        HDict hDict = new HDictBuilder()
                .add("stdt", HDate.make(startDate.getYear(), startDate.getMonthOfYear(), startDate.getDayOfMonth()))
                .add("etdt", HDate.make(endDate.getYear(), endDate.getMonthOfYear(), endDate.getDayOfMonth()))
                .add("sthh",startDate.getHourOfDay())
                .add("stmm", startDate.getMinuteOfHour())
                .add("ethh", isLastHour ? 24 : endDate.getHourOfDay())
                .add("etmm", isLastHour ? 00 : endDate.getMinuteOfHour())
                .add("coolVal", HNum.make(coolVal))
                .add("heatVal", HNum.make(heatVal))
                .toDict();

        HDictBuilder specialSchedule = new HDictBuilder()
                .add("special")
                .add(isZone ? "zone":"building")
                .add("temp")
                .add("schedule")
                .add("heating")
                .add("cooling")
                .add("dis", scheduleName)
                .add("range", hDict)
                .add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()))
                .add("lastModifiedBy", CCUHsApi.getInstance().getCCUUserName())
                .add("siteRef", siteId);

        if (zoneId != null) {
            specialSchedule.add("roomRef", HRef.copy(zoneId));
            specialSchedule.add("ccuRef", CCUHsApi.getInstance().getCcuId());
        }
        HRef localId;
        if(StringUtils.isEmpty(specialScheduleId)) {
            specialSchedule.add("createdDateTime", HDateTime.make(System.currentTimeMillis()));
            localId = HRef.make(UUID.randomUUID().toString());
        }
        else {
            String ssId = specialScheduleId.replaceFirst("@", "");
            localId = HRef.make(ssId);
            HDict ssDict = CCUHsApi.getInstance().getScheduleDictById(ssId);
            if(ssDict.has("createdDateTime")){
                specialSchedule.add("createdDateTime", HDateTime.make(ssDict.get("createdDateTime").toString()));
            }
        }
        specialSchedule.add("id", localId);
        CCUHsApi.getInstance().addSchedule(localId.toVal(), specialSchedule.toDict());
    }

    public static boolean isSpecialScheduleNameAvailable(String specialScheduleName,
                                                         List<HashMap<Object, Object>> specialScheduleList){
        for(HashMap<Object, Object> specialSchedule : specialScheduleList){
            if(specialSchedule.get("dis").toString().trim().equalsIgnoreCase(specialScheduleName.trim())){
                return false;
            }
        }
        return true;
    }

    public static int getInt(String intString){
        if(intString.contains(".")){
            String[] numerics = intString.split("\\.");
            return Integer.parseInt(numerics[0]);
        }
        return Integer.parseInt(intString);
    }
    private static Map<Interval, String> getAvailableSpecialScheduleIntervals(List<HashMap<Object, Object>> specialScheduleList){
        Map<Interval, String> specialScheduleIntervalMap = new HashMap<>();
        for(HashMap<Object, Object> specialSchedule : specialScheduleList){
            String scheduleName = specialSchedule.get(Tags.DIS).toString();
            HDict range = (HDict) specialSchedule.get(Tags.RANGE);
            String beginDate = range.get(Tags.STDT).toString();
            int beginHour = getInt(range.get(Tags.STHH).toString());
            int beginMin = getInt(range.get(Tags.STMM).toString());
            String endDate = range.get(Tags.ETDT).toString();
            int endHour = getInt(range.get(Tags.ETHH).toString());
            int endMin = getInt(range.get(Tags.ETMM).toString());
            int beginDateNum = SS_DATE_TIME_FORMATTER.parseDateTime(beginDate).getDayOfYear();
            int endDateNum = SS_DATE_TIME_FORMATTER.parseDateTime(endDate).getDayOfYear();
            int count = 0;

            do{
                DateTime beginDateTime = SS_DATE_TIME_FORMATTER.parseDateTime(beginDate).plusDays(count)
                        .withHourOfDay(beginHour)
                        .withMinuteOfHour(beginMin);
                DateTime endDateTime = SS_DATE_TIME_FORMATTER.parseDateTime(beginDate).plusDays(count)
                        .withHourOfDay(getEndHour(endHour))
                        .withMinuteOfHour(getEndMinute(endHour, endMin));
                specialScheduleIntervalMap.put(new Interval(beginDateTime, endDateTime), scheduleName);
                count++;
                beginDateNum++;
            }while(beginDateNum <= endDateNum);
        }
        return specialScheduleIntervalMap;

    }
    private static List<Interval> getDesiredSpecialScheduleList(DateTime scheduleStartDate, DateTime scheduleEndDate){
        List<Interval> specialScheduleIntervals = new ArrayList<>();
        int beginDateNum = scheduleStartDate.getDayOfYear();
        int endDateNum = scheduleEndDate.getDayOfYear();
        int count = 0;
        do{
            DateTime beginDateTime = scheduleStartDate.plusDays(count);
            DateTime endDateTime = scheduleStartDate.plusDays(count)
                    .withHourOfDay(scheduleEndDate.getHourOfDay())
                    .withMinuteOfHour(scheduleEndDate.getMinuteOfHour());
            Interval interval = new Interval(beginDateTime, endDateTime);
            specialScheduleIntervals.add(interval);
            count++;
            beginDateNum++;
        }while(beginDateNum <= endDateNum);

        return specialScheduleIntervals;
    }

    /**
     *
     * @param scheduleStartDate begin date and time of special schedule
     * @param scheduleEndDate end date and time of special schedule
     * @param specialScheduleList List of special schedule available
     * @return MultiValuedMap<String, String> : This holds available overlapping special schedule name as value and
     * key would be each day's begin time and end time.
     */
    public static MultiValuedMap<String, String>  getListOfOverlapSpecialSchedules(DateTime scheduleStartDate,
                                                      DateTime scheduleEndDate, List<HashMap<Object, Object>> specialScheduleList){
        MultiValuedMap<String, String> overlapSchedules = new ArrayListValuedHashMap<>();
        Map<Interval, String> availableSpecialScheduleIntervalsMap =
                getAvailableSpecialScheduleIntervals(specialScheduleList);
        List<Interval> desiredSpecialScheduleIntervals = getDesiredSpecialScheduleList(scheduleStartDate, scheduleEndDate);
        for(Map.Entry<Interval, String> availableSpecialScheduleIntervalEntry :
                availableSpecialScheduleIntervalsMap.entrySet()) {
            Interval availableSpecialInterval = availableSpecialScheduleIntervalEntry.getKey();
            for (Interval desiredSpeInterval : desiredSpecialScheduleIntervals) {
                Interval overlapInterval = desiredSpeInterval.overlap(availableSpecialInterval);
                if (overlapInterval != null)
                    overlapSchedules.put(availableSpecialScheduleIntervalEntry.getValue(),
                            overlapInterval.getStart().toString(OVERLAP_DAY)+" "+
                                    getEndTimeHrStr(Integer.parseInt(overlapInterval.getStart().hourOfDay().getAsString()),
                                            Integer.parseInt(overlapInterval.getStart().minuteOfHour().getAsString())) +
                                    ":" + getEndTimeMinStr(Integer.parseInt(overlapInterval.getStart().hourOfDay().getAsString()),
                                    Integer.parseInt(overlapInterval.getStart().minuteOfHour().getAsString()))
                                    + " to " + getEndTimeHrStr(Integer.parseInt(overlapInterval.getEnd().hourOfDay().getAsString()),
                                    Integer.parseInt(overlapInterval.getEnd().minuteOfHour().getAsString())) +
                                    ":" + getEndTimeMinStr(Integer.parseInt(overlapInterval.getEnd().hourOfDay().getAsString()),
                                    Integer.parseInt(overlapInterval.getEnd().minuteOfHour().getAsString())));

                }
        }
        return overlapSchedules;
    }

    /**
     *
     * @param roomRef roomRef = null is building otherwise for a zone
     */
    public static List<Schedule.Days> getSpecialScheduleForRunningWeekForZone(String roomRef, int intrinsicDayNum){
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

            DateTime beginDateTime = SS_DATE_TIME_FORMATTER.parseDateTime(beginDate)
                    .withHourOfDay(beginHour)
                    .withMinuteOfHour(beginMin);
            DateTime endDateTime = SS_DATE_TIME_FORMATTER.parseDateTime(endDate)
                    .withHourOfDay(getEndHour(endHour))
                    .withMinuteOfHour(getEndMinute(endHour, endMin));

            int dayNumber = 0; //0-6 (Monday-Sunday) Schedule->days->day

            Date weekStartDate = IntrinsicScheduleCreator.getWeekStartDate();
            Calendar calendar = Calendar.getInstance();
            while(weekStartDate.before(IntrinsicScheduleCreator.getWeekEndDate())){
                calendar.setTime(weekStartDate);
                DateTime currentDateTime = new DateTime(weekStartDate);
                calendar.add(Calendar.DATE, 1);
                weekStartDate = calendar.getTime();
                if(currentDateTime.getDayOfYear() >= beginDateTime.getDayOfYear() &&
                        currentDateTime.getDayOfYear() <= endDateTime.getDayOfYear()){
                    HDictBuilder hDictDay = new HDictBuilder()
                            .add(Tags.DAY, HNum.make(dayNumber))
                            .add(Tags.STHH, HNum.make(beginHour))
                            .add(Tags.STMM, HNum.make(beginMin))
                            .add(Tags.ETHH, HNum.make(endHour))
                            .add(Tags.ETMM, HNum.make(endMin))
                            .add(Tags.COOLVAL, HNum.make(Double.parseDouble(range.get(Tags.COOLVAL).toString())))
                            .add(Tags.HEATVAL, HNum.make(Double.parseDouble(range.get(Tags.HEATVAL).toString())));
                    if(intrinsicDayNum == dayNumber){
                        daysList.add(Schedule.Days.parseSingleDay(hDictDay.toDict()));
                    }

                }
                dayNumber++;
            }
        }
        return daysList;
    }

}
