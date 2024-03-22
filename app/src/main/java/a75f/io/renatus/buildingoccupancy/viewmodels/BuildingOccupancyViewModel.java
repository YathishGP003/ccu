package a75f.io.renatus.buildingoccupancy.viewmodels;

import static a75f.io.api.haystack.util.TimeUtil.getEndTimeHr;
import static a75f.io.api.haystack.util.TimeUtil.getEndTimeMin;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.client.HClient;
import org.projecthaystack.io.HZincReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.schedule.BuildingOccupancy;
import a75f.io.domain.api.Room;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.util.OfflineModeUtilKt;
import a75f.io.renatus.buildingoccupancy.BuildingOccupancyDialogFragment;
import a75f.io.renatus.schedules.ScheduleUtil;
import a75f.io.renatus.schedules.SchedulerFragment;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.views.MasterControl.MasterControlUtil;

public class BuildingOccupancyViewModel {

    private String getDayString(DateTime d) {
        return ScheduleUtil.getDayString(d.getDayOfWeek());
    }

    public BuildingOccupancy.Days removeDaysEntry(int position, BuildingOccupancy buildingOccupancy){
        BuildingOccupancy.Days removeEntry = null;
        if (position != BuildingOccupancyDialogFragment.NO_REPLACE) {
            //sort schedule days according to the start hour of the day
            try {
                Collections.sort(buildingOccupancy.getDays(), Comparator.comparingInt(BuildingOccupancy.Days::getSthh));
                Collections.sort(buildingOccupancy.getDays(), Comparator.comparingInt(BuildingOccupancy.Days::getDay));
                removeEntry = buildingOccupancy.getDays().remove(position);
            }catch (ArrayIndexOutOfBoundsException e) {
                Log.d("CCU_UI", "onClickSave: " + e.getMessage());
            }
        }
        return removeEntry;
    }

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
                Log.d("CCU_UI"," overLap "+overlap);
                overlapDays.append(getDayString(overlap.getStart())+"("+overlap.getStart().hourOfDay().get()+":"+
                        (overlap.getStart().minuteOfHour().get() == 0 ? "00" : overlap.getStart().minuteOfHour().get())
                        +" - " +(getEndTimeHr(overlap.getEnd().hourOfDay().get(), overlap.getEnd().minuteOfHour().get()))
                        +":"+(getEndTimeMin(overlap.getEnd().hourOfDay().get(),
                        overlap.getEnd().minuteOfHour().get())  == 0 ? "00": overlap.getEnd().minuteOfHour().get())+
                        ") ");
            }
        }
        return overlapDays.toString();
    }
    public List<BuildingOccupancy.Days> getUnoccupiedDays(List<BuildingOccupancy.Days> days){
        Collections.sort(days, Comparator.comparingInt(BuildingOccupancy.Days::getSthh));
        Collections.sort(days, Comparator.comparingInt(BuildingOccupancy.Days::getDay));
        Map<Integer, List<BuildingOccupancy.Days>> occupiedDaysMap= new LinkedHashMap<>();
        for(int i = 0; i < days.size(); i++){
            if(occupiedDaysMap.containsKey((days.get(i).getDay()))){
                List<BuildingOccupancy.Days> availableDays = occupiedDaysMap.get(days.get(i).getDay());
                availableDays.add(days.get(i));
                occupiedDaysMap.put(days.get(i).getDay(), availableDays);
            }
            else{
                occupiedDaysMap.put(days.get(i).getDay(), new ArrayList<>(Arrays.asList(days.get(i))));
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
            if(occupiedTime.getSthh() == runningUnoccupiedPeriod.getSthh() && occupiedTime.getSthh() == runningUnoccupiedPeriod.getSthh()){
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

        LinkedHashMap<String, ArrayList<Interval>> spillsMap = new LinkedHashMap<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ArrayList<Schedule> scheduleList = new ArrayList<>();
        Map<String, Schedule> activeScheduleList = new LinkedHashMap<>();
        ArrayList<Zone> zoneList = new ArrayList<>();
        HashMap<Object,Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
        String siteRef = siteMap.get("id").toString();
        Map<String, List<Zone>> zoneMap = new LinkedHashMap<>();

        LinkedHashMap<String, ArrayList<Interval>> finalSpillsMap = spillsMap;
        Future<LinkedHashMap<String, ArrayList<Interval>>> future = executor.submit(() -> {

            if(OfflineModeUtilKt.isOfflineMode()){
                ArrayList<HashMap<Object, Object>> schedules = CCUHsApi.getInstance().readAllEntities
                        ("schedule and days and siteRef == " + siteRef);

                List<HashMap<Object, Object>> namedSchedule = CCUHsApi.getInstance().getAllNamedSchedules();

                ArrayList<HashMap<Object, Object>> zones = CCUHsApi.getInstance().readAllEntities
                        ("room");

                for (HashMap<Object, Object> schedule:schedules) {
                    scheduleList.add(CCUHsApi.getInstance().getScheduleById(schedule.get("id").toString()));
                }
                for (HashMap<Object, Object> schedule:namedSchedule) {
                    scheduleList.add(CCUHsApi.getInstance().getScheduleById(schedule.get("id").toString()));
                }

                for (HashMap<Object, Object> m : zones)
                {
                    zoneList.add(new Zone.Builder().setHashMap(m).build());
                }

            }else {
                HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
                HDict tDict = new HDictBuilder().add("filter", "schedule and days and siteRef == " + siteRef).toDict();
                HGrid schedulePoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));

                HDict queryDictionary = new HDictBuilder().add("filter",
                        "named and schedule and organization == \"" +
                                Objects.requireNonNull(CCUHsApi.getInstance().getSite()).getOrganization() + "\"").toDict();
                HGrid namedschedules = hClient.call("read", HGridBuilder.dictToGrid(queryDictionary));


                HDict roomDict = new HDictBuilder().add("filter", "room and siteRef == " + siteRef).toDict();
                HGrid roomPoint = hClient.call("read", HGridBuilder.dictToGrid(roomDict));

                CcuLog.d(L.TAG_CCU_SCHEDULER, "org =" + CCUHsApi.getInstance().getSite().getOrganization());

            if (roomPoint != null) {
                Iterator hZincReaderIterator = roomPoint.iterator();
                while (hZincReaderIterator.hasNext()) {
                    HDict dict = (HDict) hZincReaderIterator.next();
                    HashMap<Object, Object> map = new HashMap<>();
                    Iterator it = dict.iterator();
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry) it.next();
                        map.put(entry.getKey().toString(), entry.getValue().toString());
                    }
                    String floorRef = map.get("floorRef").toString();
                    zoneMap.putIfAbsent(floorRef, new ArrayList<>());
                    zoneMap.get(floorRef).add(new Zone.Builder().setHashMap(map).build());
                }
                for(List<Zone> zones: zoneMap.values()) {
                    zoneList.addAll(zones);
                }
                zoneMap.clear();
            }


                if (schedulePoint != null) {
                    Iterator it = schedulePoint.iterator();
                    while (it.hasNext()) {
                        HRow r = (HRow) it.next();
                        scheduleList.add(new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build());
                    }
                }
                if (namedschedules != null) {
                    Iterator it = namedschedules.iterator();
                    while (it.hasNext()) {
                        HRow r = (HRow) it.next();
                        Schedule schedule = new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build();
                        if (schedule.getMarkers().contains("default")
                                && !schedule.getmSiteId().equals(CCUHsApi.getInstance().getSiteIdRef().toString().replace("@", ""))) {
                            continue;
                        }
                        scheduleList.add(schedule);
                    }
                } else {
                    CcuLog.d(L.TAG_CCU_SCHEDULER, "Named sched is null");
                }

                CcuLog.i(L.TAG_CCU_SCHEDULER, "Retrieved schedule list of size " + scheduleList.size() + " for site " + siteRef);
            }

            for (Zone zone:zoneList) {
                for (Schedule schedule: scheduleList) {
                    if((schedule.getId().replace("@","")).equals(zone.getScheduleRef().replace("@",""))){
                        activeScheduleList.put(schedule.getId(), schedule);
                    }
                }
            }




               for (Schedule schedule : activeScheduleList.values()) {
                    ArrayList<Interval> intervalSpills = new ArrayList<>();
                    ArrayList<Interval> zoneIntervals = schedule.getScheduledIntervals();

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
                                    for (Interval i : SchedulerFragment.newInstance().disconnectedIntervals(systemIntervals, z)) {
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

                    if (intervalSpills.size() > 0) {
                        if(schedule.isNamedSchedule()){
                            for (Zone zone:zoneList) {
                                if(schedule.getId().replace("@","").equals(zone.getScheduleRef().replace("@",""))){
                                    finalSpillsMap.put(StringUtils.prependIfMissing(zone.getId(),"@"),intervalSpills);
                                }
                            }
                        }
                        else if(schedule.getRoomRef() != null) {
                            finalSpillsMap.put(schedule.getRoomRef(), intervalSpills);
                        }
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

    public String getWarningMessage(HashMap<String, ArrayList<Interval>> spillsMap){
        StringBuilder spillZones = new StringBuilder();
        StringBuilder spillNamedZones = new StringBuilder();
        ArrayList<String> namedheaders = new ArrayList<>();
        ArrayList<String> zoneheaders = new ArrayList<>();
        String Warning = "";

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> result = executor.submit(() -> {
            String schedules = "";
            for (String zone : spillsMap.keySet()) {
                for (Interval i : spillsMap.get(zone)) {
                    Zone z = new Zone();
                    Floor f = new Floor();
                    List<Equip> equipList = new ArrayList<>();

                    if(OfflineModeUtilKt.isOfflineMode()){
                        ArrayList<HashMap<Object, Object>> equipMaps = CCUHsApi.getInstance().
                                readAllEntities("equip and roomRef ==  \"" +zone + "\"");
                        if(equipMaps != null) {
                            for (HashMap equip : equipMaps) {
                                equipList.add(new Equip.Builder().setHashMap(equip).build());
                            }
                        }
                        HashMap<Object, Object> zonemap = CCUHsApi.getInstance().readMapById(zone);
                        z = new Zone.Builder().setHashMap(zonemap).build();
                        HashMap<Object, Object> floormap = CCUHsApi.getInstance().readMapById(z.getFloorRef());
                        f = new Floor.Builder().setHashMap(floormap).build();
                    }else {

                        HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
                        HDict equipDict = new HDictBuilder().add("filter", "equip and roomRef == " + zone).toDict();
                        HGrid equipGrid = hClient.call("read", HGridBuilder.dictToGrid(equipDict));
                        if (equipGrid != null) {
                            List<HashMap> equipMaps = CCUHsApi.getInstance().HGridToList(equipGrid);
                            for (HashMap equip : equipMaps) {
                                equipList.add(new Equip.Builder().setHashMap(equip).build());
                            }
                        }
                        String response = CCUHsApi.getInstance().fetchRemoteEntity(zone);


                        if (response != null) {
                            HZincReader hZincReader = new HZincReader(response);
                            Iterator hZincReaderIterator = hZincReader.readGrid().iterator();
                            while (hZincReaderIterator.hasNext()) {
                                HDict dict = (HDict) hZincReaderIterator.next();
                                HashMap<Object, Object> map = new HashMap<>();
                                Iterator it = dict.iterator();
                                while (it.hasNext()) {
                                    Map.Entry entry = (Map.Entry) it.next();
                                    map.put(entry.getKey().toString(), entry.getValue().toString());
                                }
                                z = new Zone.Builder().setHashMap(map).build();

                            }
                        }
                        String floorResponse = CCUHsApi.getInstance().fetchRemoteEntity(z.getFloorRef());
                        if (floorResponse != null) {
                            HZincReader hZincReader = new HZincReader(floorResponse);
                            Iterator hZincReaderIterator = hZincReader.readGrid().iterator();
                            while (hZincReaderIterator.hasNext()) {
                                HDict dict = (HDict) hZincReaderIterator.next();
                                HashMap<Object, Object> map = new HashMap<>();
                                Iterator it = dict.iterator();
                                while (it.hasNext()) {
                                    Map.Entry entry = (Map.Entry) it.next();
                                    map.put(entry.getKey().toString(), entry.getValue().toString());
                                }
                                f = new Floor.Builder().setHashMap(map).build();

                            }
                        }
                    }

                    if(equipList.size() > 0 && !MasterControlUtil.isNonTempModule(equipList.get(0).getProfile())) {
                        Schedule schedule = CCUHsApi.getInstance().getScheduleById(z.getScheduleRef());
                        if(schedule == null){
                             schedule = CCUHsApi.getInstance().getRemoteSchedule(z.getScheduleRef());
                        }
                        if (schedule.isNamedSchedule()) {
                            schedules = schedules.concat("named");
                            if (!namedheaders.contains(f.getDisplayName())) {
                                spillNamedZones.append("\t").append(f.getDisplayName()).append("->\n");
                                namedheaders.add(f.getDisplayName());
                            }
                            spillNamedZones.append("\t\t\tZone ").append(z.getDisplayName()).append(" ")
                                    .append(ScheduleUtil.getDayString(i.getStart().getDayOfWeek()))
                                    .append(" (").append(i.getStart().hourOfDay().get()).append(":")
                                    .append(i.getStart().minuteOfHour().get() == 0 ? "00" : i.getStart().minuteOfHour().get())
                                    .append(" - ").append(getEndTimeHr(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()))
                                    .append(":").append(getEndTimeMin(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()) == 0 ? "00" : i.getEnd().minuteOfHour().get()).append(") \n");
                        } else {
                            schedules = schedules.concat(Tags.ZONE);
                            if (!zoneheaders.contains(f.getDisplayName())) {
                                spillZones.append("\t").append(f.getDisplayName()).append("->\n");
                                zoneheaders.add(f.getDisplayName());
                            }
                            spillZones.append("\t\t\tZone ").append(z.getDisplayName()).append(" ").append(ScheduleUtil.getDayString(i.getStart().getDayOfWeek())).append(" (").append(i.getStart().hourOfDay().get()).append(":").append(i.getStart().minuteOfHour().get() == 0 ? "00" : i.getStart().minuteOfHour().get()).append(" - ").append(getEndTimeHr(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get())).append(":").append(getEndTimeMin(i.getEnd().hourOfDay().get(), i.getEnd().minuteOfHour().get()) == 0 ? "00" : i.getEnd().minuteOfHour().get()).append(") \n");
                        }
                    }
                }
            }

            String returnVal = "";
            String namedSchedulesWarning = "";
            String zoneSchedulesWarning = "";
            if (schedules.contains("named")) {
                namedSchedulesWarning = "Named Schedule for below zone(s) is outside updated " +
                        "building occupancy.\n"
                        + ((spillNamedZones.toString()).equals("") ? "" : "\tThe Schedule is " +
                        "outside by \n\t" + spillNamedZones.toString() + "\n");
                if (schedules.contains("zone")) {
                    zoneSchedulesWarning = "Zone Schedule for below zone(s) is outside updated " +
                            "building occupancy.\n" + (spillZones.toString().equals("") ? "" : "\tThe Schedule " +
                            "is outside by \n\t" + spillZones.toString());
                }
                returnVal = namedSchedulesWarning + zoneSchedulesWarning;

            } else if (schedules.contains("zone")) {
                zoneSchedulesWarning = "Zone Schedule for below zone(s) is outside updated " +
                        "building occupancy.\n" + (spillZones.toString().equals("") ? "" : "\tThe Schedule " +
                        "is outside by \n\t" + spillZones.toString());
                returnVal = zoneSchedulesWarning;

            }
            return returnVal;
        });


        try {
            Warning  =  result.get();
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

    public HashMap<String,ArrayList<Interval>> getRemoveScheduleSpills(BuildingOccupancy buildingOccupancy) {
        return getScheduleSpills(null,buildingOccupancy);
    }

    public boolean checkIfNonTempEquipInZone(HashMap<Object,Object> zone){
        ArrayList<HashMap<Object, Object>> equips = CCUHsApi.getInstance().readAllEntities("equip and roomRef ==\""
                + zone.get("id").toString() + "\"");
        return equips.stream().anyMatch(equip -> MasterControlUtil.isNonTempModule(equip.get("profile").toString()));
    }
}
