package a75f.io.renatus.buildingoccupancy.viewmodels;

import static a75f.io.api.haystack.util.TimeUtil.getEndTimeHr;
import static a75f.io.api.haystack.util.TimeUtil.getEndTimeMin;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.schedule.BuildingOccupancy;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.schedule.PossibleScheduleImpactTable;
import a75f.io.logic.schedule.ScheduleGroup;
import a75f.io.logic.util.OfflineModeUtilKt;
import a75f.io.renatus.schedules.CommonTimeSlotFinder;
import a75f.io.renatus.schedules.ScheduleImpactDialogFragment.ScheduleImpact;
import a75f.io.renatus.schedules.ScheduleUtil;
import a75f.io.renatus.schedules.ZoneScheduleSpillGenerator;
import a75f.io.renatus.util.Marker;
import a75f.io.renatus.views.MasterControl.MasterControlUtil;

public class BuildingOccupancyViewModel  {

    private String getDayString(DateTime d) {
        return ScheduleUtil.getDayString(d.getDayOfWeek());
    }
    public Map<String, Schedule> activeZoneScheduleList = new LinkedHashMap<>();
    LinkedHashMap<String, ArrayList<Interval>> spillsMap = new LinkedHashMap<>();

    LinkedHashMap<String, ArrayList<Interval>> finalSpillsMap = spillsMap;
    List<ZoneScheduleSpillGenerator.ZoneScheduleSpill> zoneScheduleSpills = new ArrayList<>();

    public List<BuildingOccupancy.Days> constructBuildingOccupancyDays(int startTimeHour, int endTimeHour,
                                                                       int startTimeMinute, int endTimeMinute,
                                                                       ArrayList<DAYS> days){
        List<BuildingOccupancy.Days> daysList = new ArrayList<>();
        if (days != null) {
            for (DAYS day : days) {
                BuildingOccupancy.Days dayBO = new BuildingOccupancy.Days();
                dayBO.setEthh(endTimeHour);
                dayBO.setSthh(startTimeHour);
                dayBO.setEtmm(endTimeMinute);
                dayBO.setStmm(startTimeMinute);
                dayBO.setDay(day.ordinal());
                daysList.add(dayBO);
            }
        }
        return daysList;
    }


    public String getScheduleOverlapMessage(List<BuildingOccupancy.Days> daysList, BuildingOccupancy buildingOccupancy){
        StringBuilder overlapDays = new StringBuilder();
        for (BuildingOccupancy.Days day : daysList) {
            List<Interval> overlaps = buildingOccupancy.getOverLapInterval(day);
            for (Interval overlap : overlaps) {
                CcuLog.d(L.TAG_CCU_UI," overLap "+overlap);
                overlapDays.append(getDayString(overlap.getStart() )).append("(").append(overlap.getStart().hourOfDay().get()).append(":").append(overlap.getStart().minuteOfHour().get() == 0 ? "00" : overlap.getStart().minuteOfHour().get()).append(" - ").append(getEndTimeHr(overlap.getEnd().hourOfDay().get(), overlap.getEnd().minuteOfHour().get())).append(":").append(getEndTimeMin(overlap.getEnd().hourOfDay().get(),
                        overlap.getEnd().minuteOfHour().get()) == 0 ? "00" : overlap.getEnd().minuteOfHour().get()).append(") ");
            }
        }
        return overlapDays.toString();
    }
    public List<BuildingOccupancy.Days> getUnoccupiedDays(List<BuildingOccupancy.Days> days){
        days.sort(Comparator.comparingInt(BuildingOccupancy.Days::getSthh));
        days.sort(Comparator.comparingInt(BuildingOccupancy.Days::getDay));
        Map<Integer, List<BuildingOccupancy.Days>> occupiedDaysMap= new LinkedHashMap<>();
        for(int i = 0; i < days.size(); i++){
            if(occupiedDaysMap.containsKey((days.get(i).getDay()))){
                List<BuildingOccupancy.Days> availableDays = occupiedDaysMap.get(days.get(i).getDay());
                availableDays.add(days.get(i));
                occupiedDaysMap.put(days.get(i).getDay(), availableDays);
            }
            else{
                occupiedDaysMap.put(days.get(i).getDay(), new ArrayList<>(Collections.singletonList(days.get(i))));
            }
        }
        List<BuildingOccupancy.Days> unoccupiedDays = new ArrayList<>();
        for(int i = 0; i < 7; i++){
            if(!occupiedDaysMap.containsKey(i)){
                unoccupiedDays.add(new BuildingOccupancy.Days(i, 0, 0, 24, 0, false));
            }
            else{
                unoccupiedDays.addAll(getUnoccupiedPeriodForDay(occupiedDaysMap.get(i)));
            }
        }
        return unoccupiedDays;
    }
    private List<BuildingOccupancy.Days> getUnoccupiedPeriodForDay(List<BuildingOccupancy.Days> occupiedTimeList){
        List<BuildingOccupancy.Days> unoccupiedDays = new ArrayList<>();
        BuildingOccupancy.Days runningUnoccupiedPeriod = new BuildingOccupancy.Days(occupiedTimeList.get(0).getDay(), 0,
                0, 24, 0, false);
        for(BuildingOccupancy.Days occupiedTime : occupiedTimeList){
            if(occupiedTime.getSthh() == runningUnoccupiedPeriod.getSthh() && occupiedTime.getStmm() == runningUnoccupiedPeriod.getStmm()){
                runningUnoccupiedPeriod.setSthh(occupiedTime.getEthh());
                runningUnoccupiedPeriod.setStmm(occupiedTime.getEtmm());
            }
            else if(occupiedTime.getSthh() > runningUnoccupiedPeriod.getSthh() ||
                    (occupiedTime.getSthh() == runningUnoccupiedPeriod.getSthh() && occupiedTime.getStmm() > runningUnoccupiedPeriod.getStmm())){
                unoccupiedDays.add( new BuildingOccupancy.Days(occupiedTimeList.get(0).getDay(), runningUnoccupiedPeriod.getSthh(),
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

    public HashMap<String, ArrayList<Interval>> getScheduleSpills(List<BuildingOccupancy.Days> daysList, BuildingOccupancy buildingOccupancy) {
        activeZoneScheduleList.clear();
        finalSpillsMap.clear();
        spillsMap.clear();
        zoneScheduleSpills.clear();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ArrayList<Schedule> scheduleList = new ArrayList<>();
        HashMap<String, Zone> zoneList = new HashMap<>();
        HashMap<String, HashMap<Object, Object>> floorList = new HashMap<>();
        HashMap<Object,Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
        String siteRef = siteMap.get("id").toString();
        Future<LinkedHashMap<String, ArrayList<Interval>>> future = executor.submit(() -> {

            if(OfflineModeUtilKt.isOfflineMode()){
                ArrayList<HashMap<Object, Object>> schedules = CCUHsApi.getInstance().readAllEntities
                        ("schedule and days and siteRef == " + siteRef);

                List<HashMap<Object, Object>> namedSchedule = CCUHsApi.getInstance().getAllNamedSchedules();

                ArrayList<HashMap<Object, Object>> zones = CCUHsApi.getInstance().readAllEntities
                        ("room");
                ArrayList<HashMap<Object, Object>> floors = CCUHsApi.getInstance().readAllEntities
                        ("floor");

                for (HashMap<Object, Object> schedule:schedules) {
                    scheduleList.add(CCUHsApi.getInstance().getScheduleById(schedule.get("id").toString()));
                }
                for (HashMap<Object, Object> schedule:namedSchedule) {
                    scheduleList.add(CCUHsApi.getInstance().getScheduleById(schedule.get("id").toString()));
                }
                for (HashMap<Object, Object> floor : floors) {
                    floorList.put(floor.get("id").toString(), floor);
                }

                for (HashMap<Object, Object> m : zones)
                {
                    zoneList.put(m.get("id").toString(), new Zone.Builder().setHashMap(m).build());
                }
                Map<String, String> scheduleToZoneMap = new HashMap<>();

                for (Zone zone : zoneList.values()) {
                    scheduleToZoneMap.put(zone.getScheduleRef().replace("@", ""), zone.getId());
                }


                for (Schedule schedule : scheduleList) {
                    String cleanedScheduleId = schedule.getId().replace("@", "");
                    if (scheduleToZoneMap.containsKey(cleanedScheduleId)) {
                        activeZoneScheduleList.put(schedule.getId(), schedule);
                    }
                }

                for (Schedule schedule : activeZoneScheduleList.values()) {
                    String roomRef;
                    if (schedule.isNamedSchedule()) {
                        roomRef = scheduleToZoneMap.get(schedule.getId().replace("@", ""));
                    } else {
                        roomRef = schedule.getRoomRef();
                    }
                    HashMap<Object, Object> floor = floorList.get(zoneList.get(roomRef).getFloorRef());
                    HashMap<Object, Object> equip = CCUHsApi.getInstance().readEntity("equip and roomRef == \"" + roomRef+"\"");

                    zoneScheduleSpills.add(new ZoneScheduleSpillGenerator.ZoneScheduleSpill(
                            roomRef, zoneList.get(roomRef).getDisplayName(), equip.get(Tags.PROFILE).toString(),
                            floor.get(Tags.DIS).toString(), schedule));
                    handleSpills(schedule, roomRef, buildingOccupancy, daysList);
                }

            }else {
                ZoneScheduleSpillGenerator zoneScheduleSpillGenerator = new ZoneScheduleSpillGenerator();
                zoneScheduleSpills = zoneScheduleSpillGenerator.generateSpill();

                for (ZoneScheduleSpillGenerator.ZoneScheduleSpill zoneScheduleSpill : zoneScheduleSpills) {
                    Schedule schedule = zoneScheduleSpill.getSchedule();
                    activeZoneScheduleList.put(schedule.getId(), schedule);
                    handleSpills(schedule, zoneScheduleSpill.getZoneRef(), buildingOccupancy, daysList);
                }
            }
            return finalSpillsMap;
        });



        try {
            spillsMap = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        executor.shutdown();


        return spillsMap;
    }

    private void handleSpills(Schedule schedule, String zoneRef, BuildingOccupancy buildingOccupancy, List<BuildingOccupancy.Days> daysList) {
        ArrayList<Interval> intervalSpills = new ArrayList<>();
        ArrayList<Interval> zoneIntervals = schedule.getScheduledIntervals();
        separateOvernightSchedules(zoneIntervals);

        for (Interval v : zoneIntervals) {
            CcuLog.d(L.TAG_CCU_UI, "Zone interval " + v);
        }

        List<Interval> systemIntervals = buildingOccupancy.getMergedIntervals();
        if (daysList != null) {
            systemIntervals.addAll(buildingOccupancy.getScheduledIntervals(daysList));
        }
        ArrayList<Interval> splitSchedules = new ArrayList<>();
        for (Interval v : systemIntervals) {
            if (v.getStart().getDayOfWeek() == 7 && v.getEnd().getDayOfWeek() == 1) {
                long now = MockTime.getInstance().getMockTime();
                DateTime startTime = new DateTime(now)
                        .withHourOfDay(0)
                        .withMinuteOfHour(0)
                        .withSecondOfMinute(0).withMillisOfSecond(0).withDayOfWeek(1);

                DateTime endTime = new DateTime(now).withHourOfDay(v.getEnd().getHourOfDay())
                        .withMinuteOfHour(v.getEnd().getMinuteOfHour())
                        .withSecondOfMinute(v.getEnd().getSecondOfMinute())
                        .withMillisOfSecond(v.getEnd().getMillisOfSecond()).withDayOfWeek(1);
                splitSchedules.add(new Interval(startTime, endTime));
            }
        }
        systemIntervals.addAll(splitSchedules);
        for (Interval v : systemIntervals) {
            CcuLog.d(L.TAG_CCU_UI, "Merged System interval " + v);
        }

        for (Interval z : zoneIntervals) {
            boolean contains = false;
            for (Interval s : systemIntervals) {
                if (s.contains(z)) {
                    contains = true;
                    break;
                }
            }

            if (!contains) {
                for (Interval s : systemIntervals) {
                    if (s.overlaps(z)) {
                        for (Interval i : disconnectedIntervals(systemIntervals, z)) {
                            if (!intervalSpills.contains(i)) {
                                intervalSpills.add(i);
                            }
                        }
                        contains = true;
                        break;
                    }
                }
            }

            if (!contains) {
                intervalSpills.add(z);
                CcuLog.d(L.TAG_CCU_UI, " Zone Interval not contained " + z);
            }


        }

        if (!intervalSpills.isEmpty()) {
            finalSpillsMap.put(StringUtils.prependIfMissing(zoneRef, "@"), intervalSpills);
        }
    }

    private List<Interval> disconnectedIntervals(List<Interval> intervals, Interval r) {
        List<Interval> result = new ArrayList<>();
        List<Marker> markers = new ArrayList<>();

        for (Interval i : intervals) {
            markers.add(new Marker(i.getStartMillis(), true));
            markers.add(new Marker(i.getEndMillis(), false));
        }

        markers.sort(Comparator.comparingLong(a -> a.val));

        int overlap = 0;
        boolean endReached = false;

        if (!markers.isEmpty() && markers.get(0).val > r.getStartMillis()) {
            result.add(new Interval(r.getStartMillis(), markers.get(0).val));
        }

        for (int i = 0; i < markers.size() - 1; i++) {
            Marker m = markers.get(i);

            overlap += m.start ? 1 : -1;
            Marker next = markers.get(i + 1);

            if (m.val != next.val && overlap == 0 && next.val > r.getStartMillis()) {
                long start = Math.max(m.val, r.getStartMillis());
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
    private void separateOvernightSchedules(ArrayList<Interval> zoneIntervals) {
        int size = zoneIntervals.size();
        for (int i = 0; i < size; i++) {
            Interval it = zoneIntervals.get(i);

            LocalTime startTimeOfDay = it.getStart().toLocalTime();
            LocalTime endTimeOfDay = it.getEnd().toLocalTime();

            // Check if the start time is after the end time and separating the overnight schedule
            if (startTimeOfDay.isAfter(endTimeOfDay)) {
                zoneIntervals.set(i, ScheduleUtil.OverNightEnding(it));
                zoneIntervals.add(ScheduleUtil.OverNightStarting(it));
            }
        }
    }

    public List<ScheduleImpact> getWarningMessage(HashMap<String, ArrayList<Interval>> spillsMap) {
        List<ScheduleImpact> scheduleImpacts = new ArrayList<>();
        StringBuilder spillZones = new StringBuilder();
        StringBuilder spillNamedZones = new StringBuilder();
        ArrayList<String> namedHeaders = new ArrayList<>();
        ArrayList<String> zoneHeaders = new ArrayList<>();
        List<ScheduleImpact> Warning;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<ScheduleImpact>> result = executor.submit(() -> {
            String schedules = "";
            for (ZoneScheduleSpillGenerator.ZoneScheduleSpill zoneScheduleSpill : zoneScheduleSpills) {
                String zoneRef = zoneScheduleSpill.getZoneRef();
                ArrayList<Interval> intervals = spillsMap.get(zoneRef);

                if (intervals == null) {
                    // Log and skip if no intervals are found for this zoneRef
                    CcuLog.i("ZoneScheduleSpillGenerator","No intervals found for zoneRef: " + zoneRef);
                    continue;
                }

                for (Interval i : intervals) {
                    String zoneDis = zoneScheduleSpill.getZoneDis();
                    String zoneProfile = zoneScheduleSpill.getZoneProfile();
                    String floorDis = zoneScheduleSpill.getFloorDis();
                    Schedule schedule = zoneScheduleSpill.getSchedule();

                    if (!MasterControlUtil.isNonTempModule(zoneProfile)) {
                        StringBuilder scheduleImpactConstructor = new StringBuilder();
                        scheduleImpactConstructor.append(ScheduleUtil.getDayString(i.getStart().getDayOfWeek(), schedule.getScheduleGroup()))
                                .append(" (").append(i.getStart().hourOfDay().get()).append(":")
                                .append(i.getStart().minuteOfHour().get() == 0 ? "00" : i.getStart().minuteOfHour().get())
                                .append(" - ").append(getEndTimeHr(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()))
                                .append(":").append(getEndTimeMin(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()) == 0 ? "00" : i.getEnd().minuteOfHour().get()).append(") \n");

                        if (schedule.isNamedSchedule()) {
                            schedules = schedules.concat("named");
                            if (!namedHeaders.contains(floorDis)) {
                                spillNamedZones.append("\t").append(floorDis).append("->\n");
                                namedHeaders.add(floorDis);
                            }
                            scheduleImpacts.add(new ScheduleImpact(floorDis, zoneDis,
                                    scheduleImpactConstructor.toString(), getScheduleGroup(schedule.getScheduleGroup(), true)));
                            spillNamedZones.append("\t\t\tZone ").append(zoneDis).append(" ")
                                    .append(ScheduleUtil.getDayString(i.getStart().getDayOfWeek(), schedule.getScheduleGroup()))
                                    .append(" (").append(i.getStart().hourOfDay().get()).append(":")
                                    .append(i.getStart().minuteOfHour().get() == 0 ? "00" : i.getStart().minuteOfHour().get())
                                    .append(" - ").append(getEndTimeHr(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()))
                                    .append(":").append(getEndTimeMin(i.getEnd().hourOfDay().get(),
                                            i.getEnd().minuteOfHour().get()) == 0 ? "00" : i.getEnd().minuteOfHour().get()).append(")");
                        } else {
                            schedules = schedules.concat(Tags.ZONE);
                            if (!zoneHeaders.contains(floorDis)) {
                                spillZones.append("\t").append(floorDis).append("->\n");
                                zoneHeaders.add(floorDis);
                            }
                            scheduleImpacts.add(new ScheduleImpact(floorDis, zoneDis,
                                    scheduleImpactConstructor.toString(), getScheduleGroup(schedule.getScheduleGroup(), false)));
                            spillZones.append("\t\t\tZone ").append(zoneDis).append(" ")
                                    .append(ScheduleUtil.getDayString(i.getStart().getDayOfWeek(), schedule.getScheduleGroup()))
                                    .append(" (").append(i.getStart().hourOfDay().get()).append(":")
                                    .append(i.getStart().minuteOfHour().get() == 0 ? "00" : i.getStart().minuteOfHour().get())
                                    .append(" - ").append(getEndTimeHr(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()))
                                    .append(":").append(getEndTimeMin(i.getEnd().hourOfDay().get(),
                                            i.getEnd().minuteOfHour().get()) == 0 ? "00" : i.getEnd().minuteOfHour().get()).append(") \n");
                        }
                    }
                }
            }

            return scheduleImpacts;
        });

        try {
            Warning = result.get();
            executor.shutdown();
            return Warning;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve the interrupted status
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
        return null;
    }

    private PossibleScheduleImpactTable getScheduleGroup(Integer scheduleGroup, boolean isNamedSchedule) {
        if (scheduleGroup == ScheduleGroup.EVERYDAY.ordinal()){
            if(isNamedSchedule){
                return PossibleScheduleImpactTable.NAMED_EVERYDAY;
            } else {
                return PossibleScheduleImpactTable.EVERYDAY;
            }
        } else if(scheduleGroup == ScheduleGroup.WEEKDAY_SATURDAY_SUNDAY.ordinal()){
            if(isNamedSchedule){
                return PossibleScheduleImpactTable.NAMED_WEEKDAY_SATURDAY_SUNDAY;
            } else {
                return PossibleScheduleImpactTable.WEEKDAY_SATURDAY_SUNDAY;
            }
        } else if(scheduleGroup == ScheduleGroup.WEEKDAY_WEEKEND.ordinal()){
            if(isNamedSchedule){
                return PossibleScheduleImpactTable.NAMED_WEEKDAY_WEEKEND;
            } else {
                return PossibleScheduleImpactTable.WEEKDAY_WEEKEND;
            }
        } else {
            if(isNamedSchedule){
                return PossibleScheduleImpactTable.NAMED_SEVEN_DAY;
            } else {
                return PossibleScheduleImpactTable.SEVEN_DAY;
            }
        }
    }

    public HashMap<String,ArrayList<Interval>> getRemoveScheduleSpills(BuildingOccupancy buildingOccupancy) {
        return getScheduleSpills(null,buildingOccupancy);
    }


    public void forceTrimScheduleTowardsCommonTimeslot(CCUHsApi ccuHsApi) {
        Schedule buildingOccupancy = ccuHsApi.getSystemSchedule(false).get(0);
        CcuLog.d(L.TAG_CCU_SCHEDULE, "Trimming schedule for building occupancy "+buildingOccupancy.getDays());
        CommonTimeSlotFinder commonTimeSlotFinder = new CommonTimeSlotFinder();
        for(Schedule zoneSchedule : activeZoneScheduleList.values()) {
            CcuLog.d(L.TAG_CCU_SCHEDULE, "Trimming schedule for zone : " + zoneSchedule.getDis() + "Schedule Days :" + zoneSchedule.getDays());
            List<List<CommonTimeSlotFinder.TimeSlot>> commonTimeslot = commonTimeSlotFinder.
                    getCommonTimeSlot(zoneSchedule.getScheduleGroup(), buildingOccupancy.getDays(),
                            zoneSchedule.getDays(), true);
            commonTimeSlotFinder.trimScheduleTowardCommonTimeSlotAndSync(zoneSchedule, commonTimeslot, ccuHsApi);
        }
    }

    public boolean isTimeSlotExpanded(BuildingOccupancy.Days removeEntry, int startTimeHour, int endTimeHour, int startTimeMinute, int endTimeMinute) {
        return (removeEntry.getSthh() > startTimeHour) || (removeEntry.getSthh() >= startTimeHour && removeEntry.getStmm() > startTimeMinute)
                || (removeEntry.getEthh() < endTimeHour) ||  (removeEntry.getEthh() <= endTimeHour && removeEntry.getEtmm() < endTimeMinute);
    }
}
