package a75f.io.logic.schedule;

import org.joda.time.DateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HNum;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.DefaultSchedules;

public class IntrinsicScheduleCreator {

    private Schedule getBuildingSchedule(){
        return CCUHsApi.getInstance().getSystemSchedule(false).get(0);
    }

    private List<Schedule> getBuildingVacation(){
        return CCUHsApi.getInstance().getSystemSchedule(true);
    }

    private List<HashMap<Object, Object>> getZones(){
        return CCUHsApi.getInstance().readAllEntities("room");
    }

    private boolean isAnyZoneFollowingBuildingSchedule(List<HashMap<Object, Object>> rooms){
        for(HashMap<Object, Object> room : rooms){
            String query = "point and scheduleType and roomRef == \"" + room.get("id") + "\"";
            if(CCUHsApi.getInstance().readHisValByQuery(query).intValue() == 0){
                return true;
            }
        }
        return false;
    }

    private void calculateIntrinsicScheduleWithZones(List<HashMap<Object, Object>> rooms, int dayNumber,
                                                     DateTime currentDateTime, List<HDict> intrinsicScheduleList){
        Set<Schedule.Days> zonesDayScheduleSet = new TreeSet<>(sortSchedules());
        for(HashMap<Object, Object> room : rooms){
            if(!isZoneOnVacationForCurrentDay(room, currentDateTime)){
                zonesDayScheduleSet.addAll(getZoneSchedulesForDay(room, dayNumber));
            }
        }
        calculateIntrinsicScheduleForDay(dayNumber, intrinsicScheduleList, zonesDayScheduleSet);
    }

    private void calculateIntrinsicScheduleForDay(int dayNumber, List<HDict> intrinsicScheduleList, Set<Schedule.Days> dayScheduleSet) {
        if(dayScheduleSet.isEmpty()){
            return;
        }
        Schedule.Days[] zonesDaySchedule = new Schedule.Days[dayScheduleSet.size()];
        zonesDaySchedule = dayScheduleSet.toArray(zonesDaySchedule);

        if(zonesDaySchedule.length == 1){
            HDictBuilder hDictDay = new HDictBuilder()
                    .add(Tags.DAY, HNum.make(dayNumber))
                    .add(Tags.STHH, HNum.make(zonesDaySchedule[0].getSthh()))
                    .add(Tags.STMM, HNum.make(zonesDaySchedule[0].getStmm()))
                    .add(Tags.ETHH, HNum.make(zonesDaySchedule[0].getEthh()))
                    .add(Tags.ETMM, HNum.make(zonesDaySchedule[0].getEtmm()))
                    .add(Tags.COOLVAL, HNum.make(DefaultSchedules.DEFAULT_COOLING_TEMP))
                    .add(Tags.HEATVAL, HNum.make(DefaultSchedules.DEFAULT_HEATING_TEMP));
            intrinsicScheduleList.add(hDictDay.toDict());
        }
        for(int index = 1; index <zonesDaySchedule.length; index++){
            if(isScheduleColliding(zonesDaySchedule[index-1], zonesDaySchedule[index])){
                zonesDaySchedule[index].setSthh(zonesDaySchedule[index-1].getSthh());
                zonesDaySchedule[index].setStmm(zonesDaySchedule[index-1].getStmm());
                if(isCurrScheduleEndingBeforePrevSchedule(zonesDaySchedule[index-1], zonesDaySchedule[index])){
                    zonesDaySchedule[index].setEthh(zonesDaySchedule[index-1].getEthh());
                    zonesDaySchedule[index].setEtmm(zonesDaySchedule[index-1].getEtmm());
                }
                else{
                    zonesDaySchedule[index].setEthh(zonesDaySchedule[index].getEthh());
                    zonesDaySchedule[index].setEtmm(zonesDaySchedule[index].getEtmm());
                }
            }
            else{
                HDictBuilder hDictDay = new HDictBuilder()
                        .add(Tags.DAY, HNum.make(dayNumber))
                        .add(Tags.STHH, HNum.make(zonesDaySchedule[index-1].getSthh()))
                        .add(Tags.STMM, HNum.make(zonesDaySchedule[index-1].getStmm()))
                        .add(Tags.ETHH, HNum.make(zonesDaySchedule[index-1].getEthh()))
                        .add(Tags.ETMM, HNum.make(zonesDaySchedule[index-1].getEtmm()))
                        .add(Tags.COOLVAL, HNum.make(DefaultSchedules.DEFAULT_COOLING_TEMP))
                        .add(Tags.HEATVAL, HNum.make(DefaultSchedules.DEFAULT_HEATING_TEMP));
                intrinsicScheduleList.add(hDictDay.toDict());
            }
            if(index == zonesDaySchedule.length-1){
                HDictBuilder hDictDay = new HDictBuilder()
                        .add(Tags.DAY, HNum.make(dayNumber))
                        .add(Tags.STHH, HNum.make(zonesDaySchedule[index].getSthh()))
                        .add(Tags.STMM, HNum.make(zonesDaySchedule[index].getStmm()))
                        .add(Tags.ETHH, HNum.make(zonesDaySchedule[index].getEthh()))
                        .add(Tags.ETMM, HNum.make(zonesDaySchedule[index].getEtmm()))
                        .add(Tags.COOLVAL, HNum.make(DefaultSchedules.DEFAULT_COOLING_TEMP))
                        .add(Tags.HEATVAL, HNum.make(DefaultSchedules.DEFAULT_HEATING_TEMP));
                    intrinsicScheduleList.add(hDictDay.toDict());
            }
        }
    }

    private Comparator<Schedule.Days> sortSchedules() {
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

    private boolean isScheduleColliding(Schedule.Days prev, Schedule.Days curr){
        return((prev.getEthh() > curr.getSthh()) || (prev.getEthh() == curr.getSthh() && prev.getEtmm() >= curr.getStmm()));
    }

    private  boolean isCurrScheduleEndingBeforePrevSchedule(Schedule.Days prev, Schedule.Days curr){
        return((prev.getEthh() > curr.getEthh()) || (prev.getEthh() == curr.getEthh() && prev.getEtmm() > curr.getEtmm()));
    }

    private List<Schedule.Days> getZoneSchedulesForDay(HashMap<Object, Object> room, int dayNumber){
        List<Schedule> zoneScheduleList = CCUHsApi.getInstance().getZoneSchedule(room.get("id").toString(), false);
        List<Schedule.Days> zoneDayScheduleList = new ArrayList<>();
        for(Schedule zoneSchedule : zoneScheduleList){
            for(Schedule.Days day : zoneSchedule.getDays()){
                if(day.getDay() == dayNumber){
                    zoneDayScheduleList.add(day);
                }
            }
        }
        return zoneDayScheduleList;
    }

    private List<Schedule.Days> getBuildingScheduleForDay(Schedule buildingSchedule, int dayNumber){
        List<Schedule.Days> buildingDayScheduleList = new ArrayList<>();
        for(Schedule.Days day : buildingSchedule.getDays()){
            if(day.getDay() == dayNumber){
                buildingDayScheduleList.add(day);
            }
        }
        return buildingDayScheduleList;
    }

    private boolean isZoneOnVacationForCurrentDay(HashMap<Object, Object> room, DateTime currentDateTime){
        List<Schedule> zoneVacations = CCUHsApi.getInstance().getZoneSchedule(room.get("id").toString(), true);
        for(Schedule zoneVacation : zoneVacations){
            if((zoneVacation.getStartDate().getMillis() <= currentDateTime.getMillis()) &&
                    (currentDateTime.getMillis() <= zoneVacation.getEndDate().getMillis())){
                return true;
            }
        }
        return false;
    }

    private Date getWeekStartDate() {
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
        return calendar.getTime();
    }

    private boolean isCurrentDayOnBuildingVacation(List<Schedule> buildingVacations, DateTime currentDateTime){
        for(Schedule buildingVacation : buildingVacations){
            if((buildingVacation.getStartDate().getMillis() <= currentDateTime.getMillis()) &&
                    (currentDateTime.getMillis() <= buildingVacation.getEndDate().getMillis())){
                return true;
            }
        }
        return false;
    }

    private void calculateIntrinsicScheduleWithBuildingSchedule(Schedule buildingSchedule, int dayNumber,
                                                                List<HDict> intrinsicScheduleList) {
        Set<Schedule.Days> buildingDayScheduleSet = new TreeSet<>(sortSchedules());
        buildingDayScheduleSet.addAll(getBuildingScheduleForDay(buildingSchedule, dayNumber));
        calculateIntrinsicScheduleForDay(dayNumber, intrinsicScheduleList, buildingDayScheduleSet);
    }

    public Schedule buildIntrinsicScheduleForCurrentWeek(){
        List<Schedule> buildingVacations = getBuildingVacation();
        Collections.sort(buildingVacations, Comparator.comparing(Schedule::getStartDate));
        Collections.sort(buildingVacations, Comparator.comparing(Schedule::getEndDate));
        List<HashMap<Object, Object>> zones = getZones();
        Schedule buildingSchedule = getBuildingSchedule();
        List<HDict> intrinsicScheduleList = new ArrayList<>();
        Date weekStartDate = getWeekStartDate();
        int dayNumber = 0; //0-6 (Monday-Sunday) Schedule->days->day
        Calendar calendar = Calendar.getInstance();
        while(weekStartDate.before(getWeekEndDate()) && !zones.isEmpty()) {
            calendar.setTime(weekStartDate);
            DateTime currentDateTime = new DateTime(weekStartDate);
            calendar.add(Calendar.DATE, 1);
            weekStartDate = calendar.getTime();
            if(isCurrentDayOnBuildingVacation(buildingVacations, currentDateTime)){
                dayNumber++;
                continue;
            }
            else if(isAnyZoneFollowingBuildingSchedule(zones)){
                calculateIntrinsicScheduleWithBuildingSchedule(buildingSchedule,dayNumber, intrinsicScheduleList);
            }
            else{
                calculateIntrinsicScheduleWithZones(zones, dayNumber, currentDateTime, intrinsicScheduleList);
            }
            dayNumber++;
        }
        HList hList = HList.make(intrinsicScheduleList);
        String localId = UUID.randomUUID().toString();
        return new Schedule.Builder().setHDict(createIntrinsicSchedule(localId, hList).toDict()).build();
    }

     private HDictBuilder createIntrinsicSchedule(String localId, HList hList){
        return new HDictBuilder()
                .add(Tags.ID, localId)
                .add("kind", "Number")
                .add("intrinsic")
                .add("schedule")
                .add("dis", "Intrinsic Schedule")
                .add("days", hList)
                .add("siteRef", CCUHsApi.getInstance().getSiteIdRef());
    }

}
