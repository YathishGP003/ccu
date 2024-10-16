package a75f.io.renatus.schedules;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.schedule.BuildingOccupancy;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.schedule.ScheduleGroup;
import a75f.io.logic.util.CommonTimeSlotFinder;
import a75f.io.renatus.util.RxjavaUtil;

public class ScheduleUtil {
    public static int SAT_OR_SUN = 1;

    public static int WEEK_END_SIZE = 2;
    public static int WEEK_DAY_SIZE = 5;

    public static int WEEK_DAY_SATURDAY_OR_SUN = 6;
    public static int WEEK_DAY_WEEK_END_SIZE = 7;

    public static void trimZoneSchedules(HashMap<String, ArrayList<Interval>> spillsMap) {

        for (String zoneId : spillsMap.keySet()) {

            Zone z = new Zone.Builder().setHashMap(CCUHsApi.getInstance().readMapById(zoneId)).build();
            HashMap<Object, Object> scheduleHashMap = CCUHsApi.getInstance().readEntity("schedule and not " +
                    "vacation and not special and roomRef == " + z.getId());
            if (scheduleHashMap.isEmpty())
                continue;
            Schedule zoneSchedule = CCUHsApi.getInstance().getScheduleById(scheduleHashMap.get("id").toString());
            ArrayList<Interval> spills = spillsMap.get(zoneId);

            Iterator daysIterator = zoneSchedule.getDays().iterator();
            while (daysIterator.hasNext()) {
                Schedule.Days d = (Schedule.Days) daysIterator.next();
                Interval i = zoneSchedule.getScheduledInterval(d);

                for (Interval spill : spills) {
                    if (!i.contains(spill)) {
                        continue;
                    }
                    if (spill.getStartMillis() <= i.getStartMillis() &&
                            spill.getEndMillis() >= i.getEndMillis()) {
                        daysIterator.remove();
                        continue;
                    }

                    if (spill.getStartMillis() <= i.getStartMillis()) {
                        d.setSthh(spill.getEnd().getHourOfDay());
                        d.setStmm(spill.getEnd().getMinuteOfHour());
                    } else if (i.getEndMillis() >= spill.getStartMillis()) {
                        d.setEthh(spill.getStart().getHourOfDay());
                        d.setEtmm(spill.getStart().getMinuteOfHour());
                    }
                }
            }

            CcuLog.d(L.TAG_CCU_UI, " Trimmed Zone Schedule" + zoneSchedule);
            CCUHsApi.getInstance().updateZoneSchedule(zoneSchedule, zoneSchedule.getRoomRef());
        }
    }


    public static Interval OverNightEnding(Interval Ending) {
        DateTime initialEnding = Ending.getStart().withTime(23, 59, 59, 0);
        return new Interval(Ending.getStart(), initialEnding);
    }

    public static Interval OverNightStarting(Interval Start) {
        DateTime subsequentStart = Start.getEnd().withTime(0, 0, 0, 0);
        return new Interval(subsequentStart, Start.getEnd());
    }

    public static Interval AddingNextWeekDayForOverNight(Schedule interval) {
        ArrayList<Interval> allIntervals = interval.getScheduledIntervals(interval.getDaysSorted());
        Interval AddingNewData = null;
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
            AddingNewData = new Interval(startNext, endNext);
        }
        return AddingNewData;
    }


    public static String getDayString(int day) {
        switch (day) {
            case 1:
                return "Monday";
            case 2:
                return "Tuesday";
            case 3:
                return "Wednesday";
            case 4:
                return "Thursday";
            case 5:
                return "Friday";
            case 6:
                return "Saturday";
            case 7:
                return "Sunday";
        }
        return "";
    }

    public static String getNamedScheduleHeader(Integer scheduleGroup, int day) {
        if (scheduleGroup == ScheduleGroup.SEVEN_DAY.ordinal()) {
            return DAYS.values()[day].name();
        } else if (scheduleGroup == ScheduleGroup.WEEKDAY_WEEKEND.ordinal()) {
            if (day == DAYS.SATURDAY.ordinal() || day == DAYS.SUNDAY.ordinal()) {
                return "Weekend";
            } else {
                return "Weekday";
            }
        } else if (scheduleGroup == ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal()) {
            if (day == DAYS.SATURDAY.ordinal()) {
                return "Saturday";
            } else if (day == DAYS.SUNDAY.ordinal()) {
                return "Sunday";
            } else {
                return "Weekday";
            }
        } else {
            return "Everyday";
        }
    }

    public static boolean isWeekDaysSatAndSunNotPresentInBuildingOccupancy(List<Integer> daysPresent) {
        return !daysPresent.containsAll(DAYS.getWeekDaysOrdinal()) &&
                !daysPresent.contains(DAYS.SATURDAY.ordinal()) &&
                !daysPresent.contains(DAYS.SUNDAY.ordinal());
    }

    public static boolean isAnyDaysNotPresentInBuildingOccupancy(List<Integer> presentDays) {
        return presentDays.size() < DAYS.values().length;
    }

    public static boolean isWeekDaysWeekendNotPresentInBuildingOccupancy(List<Integer> daysPresent) {
        return !daysPresent.containsAll(DAYS.getWeekDaysOrdinal()) &&
                !daysPresent.containsAll(DAYS.getWeekEndsOrdinal());
    }

    public static boolean isAllDaysNotPresentInBuildingOccupancy(List<Integer> presentDays) {
        return presentDays.isEmpty();
    }

    public static void trimScheduleTowardCommonTimeSlot(Schedule zoneSchedule,
                                                        List<List<CommonTimeSlotFinder.TimeSlot>> commonTimeSlot,
                                                        CommonTimeSlotFinder commonTimeSlotFinder, CCUHsApi ccuHsApi) {
        RxjavaUtil.executeBackground(() -> commonTimeSlotFinder.trimScheduleTowardCommonTimeSlotAndSync(zoneSchedule, commonTimeSlot, ccuHsApi));
    }

    public static String getDayString(int day, int mScheduleGroup) {
        switch (day) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                if (mScheduleGroup == ScheduleGroup.EVERYDAY.ordinal()) {
                    return "All Days";
                } else if (mScheduleGroup == ScheduleGroup.WEEKDAY_WEEKEND.ordinal() ||
                        mScheduleGroup == ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal()) {
                    return "Weekday";
                } else {
                    return getDayString(day);
                }
            case 6:
                if (mScheduleGroup == ScheduleGroup.EVERYDAY.ordinal()) {
                    return "All Days";
                } else if (mScheduleGroup == ScheduleGroup.WEEKDAY_WEEKEND.ordinal()) {
                    return "Weekend";
                }  else {
                    return "Saturday";
                }
            case 7:
                if (mScheduleGroup == ScheduleGroup.EVERYDAY.ordinal()) {
                    return "All Days";
                } else if (mScheduleGroup == ScheduleGroup.WEEKDAY_WEEKEND.ordinal()) {
                    return "Weekend";
                } else {
                    return "Sunday";
                }
            default:
                return "";
        }
    }

    /*Both heating and cooling deadband's min values are same*/
    public static double getDeadBandValue(String val, String roomRef){
        if(roomRef == "" || roomRef == null){
            String buildingEquipRef = Domain.buildingEquip.getEquipRef();
            if(val == "minVal"){
                return Double.parseDouble(Domain.readPointForEquip(DomainName.coolingDeadband, buildingEquipRef).get("minVal").toString());
            } else {
                return Double.parseDouble(Domain.readPointForEquip(DomainName.coolingDeadband, buildingEquipRef).get("maxVal").toString());
            }
        }
        return Double.parseDouble(CCUHsApi.getInstance().readEntity(
                "schedulable and deadband and (heating or cooling) and roomRef == \"" + roomRef + "\"").get(val).toString());
    }
}
