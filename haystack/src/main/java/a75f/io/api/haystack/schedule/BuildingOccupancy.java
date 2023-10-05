package a75f.io.api.haystack.schedule;

import static a75f.io.api.haystack.util.TimeUtil.getEndHour;
import static a75f.io.api.haystack.util.TimeUtil.getEndMinute;
import static a75f.io.api.haystack.util.TimeUtil.getEndSec;

import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Constants;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Tags;

public class BuildingOccupancy {
    private String id;
    private Set<String> markers;
    private String dis;
    private String kind;
    private String siteRef;
    private List<Days> days = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getMarkers() {
        return markers;
    }

    public void setMarkers(Set<String> markers) {
        this.markers = markers;
    }

    public String getDis() {
        return dis;
    }

    public void setDis(String dis) {
        this.dis = dis;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getSiteRef() {
        return siteRef;
    }

    public void setSiteRef(String siteRef) {
        this.siteRef = siteRef;
    }

    public List<Days> getDays() {
        Collections.sort(days, Comparator.comparingInt(d -> d.day));
        return days;
    }

    public void setDays(List<Days> days) {
        this.days = days;
    }

    public static String buildDefaultBuildingOccupancy(){
        HDict[] days = new HDict[5];
        days[0] = getDefaultForDay(DAYS.MONDAY.ordinal());
        days[1] = getDefaultForDay(DAYS.TUESDAY.ordinal());
        days[2] = getDefaultForDay(DAYS.WEDNESDAY.ordinal());
        days[3] = getDefaultForDay(DAYS.THURSDAY.ordinal());
        days[4] = getDefaultForDay(DAYS.FRIDAY.ordinal());
        HList hList = HList.make(days);

        HRef localId = HRef.make(UUID.randomUUID().toString());
        HDictBuilder defaultSchedule = new HDictBuilder()
                .add(Tags.ID, localId)
                .add(Tags.BUILDING)
                .add(Tags.OCCUPANCY)
                .add(Tags.DAYS, hList)
                .add(Tags.DIS, Constants.BUILDING_OCCUPANCY_DIS)
                .add(Tags.KIND, Constants.KIND_NUMBER)
                .add(Tags.SITEREF, CCUHsApi.getInstance().getSiteIdRef());

        CCUHsApi.getInstance().addSchedule(localId.toVal(), defaultSchedule.toDict());
        return localId.toCode();
    }

    private static HDict getDefaultForDay(int day) {
        HDict hDictDay = new HDictBuilder()
                .add(Tags.DAY, HNum.make(day))
                .add(Tags.STHH, HNum.make(8))
                .add(Tags.STMM, HNum.make(0))
                .add(Tags.ETHH, HNum.make(17))
                .add(Tags.ETMM, HNum.make(30))
                .toDict();
        return hDictDay;
    }

    public static class Days {

        private int day;
        private int sthh;
        private int stmm;
        private int ethh;
        private int etmm;
        private boolean intersection;
        public Days(){

        }
        public Days(int day, int sthh, int stmm, int ethh, int etmm, boolean intersection) {
            this.day = day;
            this.sthh = sthh;
            this.stmm = stmm;
            this.ethh = ethh;
            this.etmm = etmm;
            this.intersection = intersection;
        }

        public int getDay() {
            return day;
        }

        public void setDay(int day) {
            this.day = day;
        }

        public int getSthh() {
            return sthh;
        }

        public void setSthh(int sthh) {
            this.sthh = sthh;
        }

        public int getStmm() {
            return stmm;
        }

        public void setStmm(int stmm) {
            this.stmm = stmm;
        }

        public int getEthh() {
            return ethh;
        }

        public void setEthh(int ethh) {
            this.ethh = ethh;
        }

        public int getEtmm() {
            return etmm;
        }

        public void setEtmm(int etmm) {
            this.etmm = etmm;
        }

        public boolean isIntersection() {
            return intersection;
        }

        public void setIntersection(boolean intersection) {
            this.intersection = intersection;
        }

        public static List<BuildingOccupancy.Days> parse(HList value) {
            List<BuildingOccupancy.Days> days = new ArrayList<>();
            for (int i = 0; i < value.size(); i++) {
                days.add(parseSingleDay((HDict) value.get(i)));
            }
            return days;
        }

        public static BuildingOccupancy.Days parseSingleDay(HDict hDict) {
            BuildingOccupancy.Days days = new BuildingOccupancy.Days();
            days.day = hDict.getInt(Tags.DAY);
            days.ethh = hDict.has(Tags.ETHH) ? hDict.getInt(Tags.ETHH) : -1;
            days.etmm = hDict.has(Tags.ETMM) ? hDict.getInt(Tags.ETMM) : -1;
            days.sthh = hDict.has(Tags.STHH) ? hDict.getInt(Tags.STHH) : -1;
            days.stmm = hDict.has(Tags.STMM) ? hDict.getInt(Tags.STMM) : -1;
            return days;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Days)) return false;
            Days days = (Days) o;
            return getDay() == days.getDay() && getSthh() == days.getSthh() && getStmm() == days.getStmm() &&
                    getEthh() == days.getEthh() && getEtmm() == days.getEtmm() &&
                    isIntersection() == days.isIntersection();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getDay(), getSthh(), getStmm(), getEthh(), getEtmm(), isIntersection());
        }

        @Override
        public String toString() {
            return "Days{" +
                    "day=" + day +
                    ", sthh=" + sthh +
                    ", stmm=" + stmm +
                    ", ethh=" + ethh +
                    ", etmm=" + etmm +
                    ", intersection=" + intersection +
                    '}';
        }
    }
    public static class Builder {
        private String id;
        private Set<String> markers = new HashSet<>();
        private String dis;
        private String kind;
        private String siteRef;
        private List<Days> days = new ArrayList<>();

        public BuildingOccupancy.Builder setHDict(HDict buildingOccupancy) {
            if (buildingOccupancy == null) {
                return null;
            }
            Iterator it = buildingOccupancy.iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                if (pair.getKey().equals(Tags.ID)) {
                    this.id = pair.getValue().toString().replaceAll("@", "");
                } else if (pair.getKey().equals(Tags.DIS)) {
                    this.dis = pair.getValue().toString();
                } else if (pair.getKey().equals(Tags.KIND)) {
                    this.kind = pair.getValue().toString();
                } else if (pair.getKey().equals(Tags.SITEREF)) {
                    this.siteRef = buildingOccupancy.getRef(Tags.SITEREF).val;
                } else if (pair.getKey().equals(Tags.DAYS)) {
                    this.days = BuildingOccupancy.Days.parse((HList) pair.getValue());
                } else {
                    this.markers.add(pair.getKey().toString());
                }
            }
            return this;
        }
        public BuildingOccupancy build(){
            BuildingOccupancy buildingOccupancy = new BuildingOccupancy();
            buildingOccupancy.id = id;
            buildingOccupancy.markers = markers;
            buildingOccupancy.dis = dis;
            buildingOccupancy.kind = kind;
            buildingOccupancy.siteRef = siteRef;
            buildingOccupancy.days = days;
            return buildingOccupancy;

        }
    }
    public HDict getBuildingOccupancyHDict(){
        HDict[] days = new HDict[getDays().size()];
        int index = 0;
        for(Days day : getDays()){
            HDictBuilder hDictDay = new HDictBuilder()
                    .add(Tags.DAY, HNum.make(day.day))
                    .add(Tags.STHH, HNum.make(day.sthh))
                    .add(Tags.STMM, HNum.make(day.stmm))
                    .add(Tags.ETHH, HNum.make(day.ethh))
                    .add(Tags.ETMM, HNum.make(day.etmm));
            days[index++] = hDictDay.toDict();
        }
        HList hList = HList.make(days);
        HDictBuilder buildingOccupancy = new HDictBuilder()
                .add(Tags.ID, HRef.copy(getId()))
                .add(Tags.BUILDING)
                .add(Tags.OCCUPANCY)
                .add(Tags.DAYS, hList)
                .add(Tags.DIS, Constants.BUILDING_OCCUPANCY_DIS)
                .add(Tags.KIND, Constants.KIND_NUMBER)
                .add(Tags.SITEREF, CCUHsApi.getInstance().getSiteIdRef());
        return buildingOccupancy.toDict();

    }

    public void populateIntersections(){
        List<Interval> scheduledIntervals = getScheduledIntervals(getDays());
        for (int i = 0; i < scheduledIntervals.size(); i++) {
            for (int j = 0; j < scheduledIntervals.size(); j++) {
                if (scheduledIntervals.get(i).getEndMillis() == scheduledIntervals.get(j).getStartMillis()) {
                    this.days.get(i).setIntersection(true);
                }else if((scheduledIntervals.get(i).getEnd().getDayOfWeek() == scheduledIntervals.get(j).getStart().getDayOfWeek())
                        && (scheduledIntervals.get(i).getEnd().getHourOfDay() == scheduledIntervals.get(j).getStart().getHourOfDay())
                        && (scheduledIntervals.get(i).getEnd().getMinuteOfHour() == scheduledIntervals.get(j).getStart().getMinuteOfHour())){
                    //Multi day schedule intersection check
                    this.days.get(i).setIntersection(true);
                }
            }
        }
    }

    public List<Interval> getScheduledIntervals(List<BuildingOccupancy.Days> daysSorted) {
        List<Interval> intervals = new ArrayList<>();
        for (BuildingOccupancy.Days day : daysSorted) {
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
                    .withSecondOfMinute(getEndSec(day.getEthh())).withMillisOfSecond(0).withDayOfWeek(day.getDay() + 1);

            Interval scheduledInterval;
            if (startDateTime.isAfter(endDateTime)) {
                if (day.getDay() == DAYS.SUNDAY.ordinal()) {
                    if (startDateTime.getWeekOfWeekyear() >= 52){
                        scheduledInterval = new Interval(startDateTime, endDateTime.plusDays(1));
                    }else
                        scheduledInterval = new Interval(startDateTime, endDateTime.withWeekOfWeekyear(startDateTime.getWeekOfWeekyear()+1)
                                .withDayOfWeek(DAYS.values()[day.getDay()].getNextDay().ordinal() + 1));
                } else {
                    scheduledInterval = new Interval(startDateTime, endDateTime.withDayOfWeek(DAYS.values()[day.getDay()].getNextDay().ordinal() + 1));
                }
            } else {
                scheduledInterval = new Interval(startDateTime, endDateTime);
            }
            intervals.add(scheduledInterval);
        }
        return intervals;
    }
    public List<Days> getDaysSorted() {

        Collections.sort(days, Comparator.comparing(o -> Integer.valueOf(o.getStmm())));

        Collections.sort(days, Comparator.comparing(o -> Integer.valueOf(o.getSthh())));

        Collections.sort(days, Comparator.comparing(o -> Integer.valueOf(o.day)));

        return days;
    }

    public boolean checkIntersection(List<Days> daysArrayList) {
        List<Interval> intervalsOfAdditions = getScheduledIntervals(daysArrayList);
        List<Interval> intervalsOfCurrent   = getScheduledIntervals(getDaysSorted());

        for (Interval additions : intervalsOfAdditions) {
            for (Interval current : intervalsOfCurrent) {
                boolean hasOverlap = additions.overlaps(current);
                if (hasOverlap) {
                    Log.d("CCU_UI"," hasOverlap "+" additions "+additions+" current "+current);
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
    public Interval getScheduledInterval(Days day) {

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

        if (startDateTime.isAfter(endDateTime)) {
            if (day.getDay() == DAYS.SUNDAY.ordinal()) {
                return new Interval(startDateTime, endDateTime.withWeekOfWeekyear(startDateTime.getWeekOfWeekyear()+1)
                        .withDayOfWeek(DAYS.values()[day.getDay()].getNextDay().ordinal() + 1));
            } else {
                return new Interval(startDateTime, endDateTime.withDayOfWeek(DAYS.values()[day.getDay()].getNextDay().ordinal() + 1));
            }
        } else {
            return new Interval(startDateTime, endDateTime);
        }

    }
    public List<Interval> getOverLapInterval(Days day) {
        List<Interval> intervalsOfCurrent   = getScheduledIntervals(getDaysSorted());
        Interval intervalOfAddition = getScheduledInterval(day);
        List<Interval> overLaps = new ArrayList<>();
        for (Interval current : intervalsOfCurrent) {
            boolean hasOverlap = intervalOfAddition.overlaps(current);
            if (hasOverlap) {
                Log.d("CCU_UI"," Current "+current+" new "+intervalOfAddition+" overlaps "+hasOverlap);
                if (current.getStart().minuteOfDay().get() < current.getEnd().minuteOfDay().get()) {
                    overLaps.add(current.overlap(intervalOfAddition));
                } else {
                    //Multi-day schedule
                    if (intervalOfAddition.getEndMillis() > current.getStartMillis() &&
                            intervalOfAddition.getStartMillis() < current.getStartMillis()) {
                        overLaps.add(new Interval(current.getStartMillis(), intervalOfAddition.getEndMillis()));
                    }
                    else if (intervalOfAddition.getStartMillis() < current.getStartMillis() &&
                            intervalOfAddition.getStartMillis() < current.getEndMillis()) {
                        overLaps.add(new Interval(intervalOfAddition.getStartMillis(), current.getEndMillis()));
                        //  overLaps.add(new Interval(intervalOfAddition.getStartMillis(), intervalOfAddition.getEndMillis()));
                    }
                    else if (current.getStartMillis() < intervalOfAddition.getStartMillis() &&
                            current.getEndMillis() < intervalOfAddition.getEndMillis()) {
                        overLaps.add(new Interval(intervalOfAddition.getStartMillis(), current.getEndMillis()));
                    }
                    else if (intervalOfAddition.getStartMillis() < current.getEndMillis()) {
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

    public List<Interval> getMergedIntervals() {
        return getMergedIntervals(getDaysSorted());
    }
    public List<Interval> getMergedIntervals(List<BuildingOccupancy.Days> daysSorted) {

        List<Interval> intervals   = getScheduledIntervals(daysSorted);
        Collections.sort(intervals, new Comparator<Interval>() {
                    public int compare(Interval p1, Interval p2) {
                        return Long.compare(p1.getStartMillis(), p2.getStartMillis());
                    }
                }
        );

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
}
