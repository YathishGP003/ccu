package a75f.io.renatus.schedules;

import org.joda.time.Interval;
import org.projecthaystack.HDict;
import org.projecthaystack.HList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.renatus.util.Marker;

public class ZoneScheduleViewModel {

    public HashMap<String, ArrayList<Interval>> getScheduleSpills(ArrayList<Schedule.Days> daysArrayList, Schedule schedule) {

        LinkedHashMap<String, ArrayList<Interval>> spillsMap = new LinkedHashMap<>();
        if (schedule.isZoneSchedule()) {
            Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
            ArrayList<Interval> intervalSpills = new ArrayList<>();
            ArrayList<Interval> systemIntervals = systemSchedule.getMergedIntervals(daysArrayList);

            for (Interval v : systemIntervals) {
                CcuLog.d(L.TAG_CCU_UI, "Merged System interval " + v);
            }

            ArrayList<Interval> zoneIntervals = schedule.getScheduledIntervals(daysArrayList);

            for (Interval v : zoneIntervals) {
                CcuLog.d(L.TAG_CCU_UI, "Zone interval " + v);
            }

            for (Interval z : zoneIntervals) {
                boolean add = true;
                for (Interval s : systemIntervals) {
                    if (s.contains(z)) {
                        add = false;
                        break;
                    } else if (s.overlaps(z)) {
                        add = false;
                        for (Interval i : disconnectedIntervals(systemIntervals, z)) {
                            if (!intervalSpills.contains(i)) {
                                intervalSpills.add(i);
                            }
                        }
                    }
                }
                if (add) {
                    intervalSpills.add(z);
                    CcuLog.d(L.TAG_CCU_UI, " Zone Interval not contained " + z);
                }
            }
            if (intervalSpills.size() > 0) {
                spillsMap.put(schedule.getRoomRef(), intervalSpills);
            }

        }
        return spillsMap;
    }

    public List<Interval> disconnectedIntervals(List<Interval> intervals, Interval r) {
        List<Interval> result = new ArrayList<>();
        ArrayList<Marker> markers = new ArrayList<>();

        for (Interval i : intervals) {
            markers.add(new Marker(i.getStartMillis(), true));
            markers.add(new Marker(i.getEndMillis(), false));
        }

        Collections.sort(markers, (a, b) -> Long.compare(a.val, b.val));

        int overlap = 0;
        boolean endReached = false;

        if (markers.size() > 0 && markers.get(0).val > r.getStartMillis()) {
            result.add(new Interval(r.getStartMillis(), markers.get(0).val));
        }

        for (int i = 0; i < markers.size() - 1; i++) {
            Marker m = markers.get(i);

            overlap += m.start ? 1 : -1;
            Marker next = markers.get(i + 1);

            if (m.val != next.val && overlap == 0 && next.val > r.getStartMillis()) {
                long start = m.val > r.getStartMillis() ? m.val : r.getStartMillis();
                long end = next.val;
                if (next.val > r.getEndMillis()) {
                    end = r.getEndMillis();
                    endReached = true;
                }
                if (end > start) {
                    result.add(new Interval(start, end));
                }
                if (endReached)
                    break;
            }
        }

        if (!endReached) {
            Marker m = markers.get(markers.size() - 1);
            if (r.getEndMillis() > m.val) {
                result.add(new Interval(m.val, r.getEndMillis()));
            }
        }

        return result;
    }

    public void doScheduleUpdate(Schedule schedule) {
        if (schedule.isZoneSchedule()) {
            CCUHsApi.getInstance().updateZoneSchedule(schedule, schedule.getRoomRef());
        } else {
            CCUHsApi.getInstance().updateSchedule(schedule);
        }
        CCUHsApi.getInstance().syncEntityTree();

        ScheduleManager.getInstance().updateSchedules();
    }

    public void doFollowBuildingUpdate(boolean followBuilding, Schedule schedule) {
        if (followBuilding) {
            if (!schedule.getMarkers().contains("followBuilding")) {
                schedule.getMarkers().add(Tags.FOLLOW_BUILDING);
            }
        } else {
            if (schedule.getMarkers().contains(Tags.FOLLOW_BUILDING)) {
                schedule.getMarkers().remove(Tags.FOLLOW_BUILDING);
            }
        }
    }

    public List<UnOccupiedDays> getUnoccupiedDays(List<Schedule.Days> days){
        Collections.sort(days, Comparator.comparingInt(Schedule.Days::getSthh));
        Collections.sort(days, Comparator.comparingInt(Schedule.Days::getDay));
        Map<Integer, List<Schedule.Days>> occupiedDaysMap= new LinkedHashMap<>();
        for(int i = 0; i < days.size(); i++){
            if(occupiedDaysMap.containsKey((days.get(i).getDay()))){
                List<Schedule.Days> availableDays = occupiedDaysMap.get(days.get(i).getDay());
                availableDays.add(days.get(i));
                occupiedDaysMap.put(days.get(i).getDay(), availableDays);
            }
            else{
                occupiedDaysMap.put(days.get(i).getDay(), new ArrayList<>(Arrays.asList(days.get(i))));
            }
        }
        List<UnOccupiedDays> unoccupiedDays = new ArrayList<>();
        for(int i = 0; i < 7; i++){
            if(!occupiedDaysMap.containsKey(i)){
                unoccupiedDays.add(new UnOccupiedDays(i, 0, 0, 24, 0, false));
            }
            else{
                unoccupiedDays.addAll(getUnoccupiedPeriodForDay(occupiedDaysMap.get(i)));
            }
        }
        return unoccupiedDays;
    }
    private List<UnOccupiedDays> getUnoccupiedPeriodForDay(List<Schedule.Days> occupiedTimeList){
        List<UnOccupiedDays> unoccupiedDays = new ArrayList<>();
        UnOccupiedDays runningUnoccupiedPeriod = new UnOccupiedDays(occupiedTimeList.get(0).getDay(), 0,
                0, 24, 0, false);
        for(Schedule.Days occupiedTime : occupiedTimeList){
            if(occupiedTime.getSthh() == runningUnoccupiedPeriod.getSthh() && occupiedTime.getStmm() == runningUnoccupiedPeriod.getStmm()){
                runningUnoccupiedPeriod.setSthh(occupiedTime.getEthh());
                runningUnoccupiedPeriod.setStmm(occupiedTime.getEtmm());
            }
            else if(occupiedTime.getSthh() > runningUnoccupiedPeriod.getSthh() ||
                    (occupiedTime.getSthh() == runningUnoccupiedPeriod.getSthh() && occupiedTime.getStmm() > runningUnoccupiedPeriod.getStmm())){
                unoccupiedDays.add( new UnOccupiedDays(occupiedTimeList.get(0).getDay(), runningUnoccupiedPeriod.getSthh(),
                        runningUnoccupiedPeriod.getStmm(), occupiedTime.getSthh(), occupiedTime.getStmm(), false));
                runningUnoccupiedPeriod.setSthh(occupiedTime.getEthh());
                runningUnoccupiedPeriod.setStmm(occupiedTime.getEtmm());
            }
        }

        if(runningUnoccupiedPeriod.getSthh() != 24) {
            unoccupiedDays.add(runningUnoccupiedPeriod);
        }
        return unoccupiedDays;
    }

    public String validateUnOccupiedZoneSetBack(Double heatingUserLimitMin, int unOccupiedZoneSetBackVal,
                                              Double buildingToZoneDiff, Double buildingLimitMin, Double buildingLimitMax, Schedule.Days schedule) {
        String WarningMessage = "";
        if(!((heatingUserLimitMin - (unOccupiedZoneSetBackVal + buildingToZoneDiff)) >= buildingLimitMin)) {

            WarningMessage = WarningMessage+ScheduleUtil.getDayString(schedule.getDay()+1)+"("+schedule.getSthh()+
                    ":"+schedule.getStmm()+" - "+schedule.getEthh()+":"+schedule.getEtmm()+")"
                    +" Heating Limit Min "+": "+schedule.getHeatingUserLimitMin()+" Building limit "+": "
                    +buildingLimitMin+" to "+buildingLimitMax+"\n";
            return  WarningMessage;
        }
        return "";
    }
    public String validateUnOccupiedZoneSetBackCooling(Double coolingUserLimitMax, int unOccupiedZoneSetBackVal,
                                                Double buildingToZoneDiff, Double buildingLimitMin, Double buildingLimitMax, Schedule.Days schedule) {
        String WarningMessage = "";
        if (!((coolingUserLimitMax + (unOccupiedZoneSetBackVal + buildingToZoneDiff)) <= buildingLimitMax)) {
            WarningMessage = WarningMessage+ScheduleUtil.getDayString(schedule.getDay()+1)+"("+schedule.getSthh()+
                    ":"+schedule.getStmm()+" - "+schedule.getEthh()+":"+schedule.getEtmm()+")"
                    +" Cooling Limit Max "+": "+schedule.getCoolingUserLimitMax()+" Building limit "+": "
                    +buildingLimitMin+" to "+buildingLimitMax+"\n";
                return WarningMessage;
            }
        return "";
    }

}


