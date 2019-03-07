package a75f.io.api.haystack;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import a75f.io.logger.CcuLog;


/***
 * Supports Schedule to HDict
 * HDict to Schedule
 *
 * Raw creation requires id field is set to  String localId = UUID.randomUUID().toString();
 *
 * TODO: support all curVal types
 * TODO: return values queried anything that has this scheduleRef should be able to query it and have the results returned if it is in schedule.
 *
 */
public class Schedule extends Entity {

    public boolean isSiteSchedule() {
        return getMarkers().contains("system");
    }

    public boolean isZoneSchedule() {
        return getMarkers().contains("zone");
    }

    public boolean isNamedSchedule() {
        return getMarkers().contains("named");
    }


    public static String getZoneIdByEquipId(String equipId) {
        HashMap equipHashMap = CCUHsApi.getInstance().readMapById(equipId);
        Equip equip = new Equip.Builder().setHashMap(equipHashMap).build();
        return equip.getRoomRef().replace("@", "");
    }

    public static Schedule getScheduleByEquipId(String equipId) {
        HashMap equipHashMap = CCUHsApi.getInstance().readMapById(equipId);
        Equip equip = new Equip.Builder().setHashMap(equipHashMap).build();

        return getScheduleForZone(equip.getRoomRef().replace("@", ""), false);
    }

    public static Schedule getVacationByEquipId(String equipId) {
        HashMap equipHashMap = CCUHsApi.getInstance().readMapById(equipId);
        Equip equip = new Equip.Builder().setHashMap(equipHashMap).build();

        return getScheduleForZone(equip.getRoomRef().replace("@", ""), true);

    }

    public static Schedule getScheduleForZone(String zoneId, boolean vacation) {

        CcuLog.d("Schedule", "Equip Zone Ref: " + zoneId);
        HashMap zoneHashMap = CCUHsApi.getInstance().readMapById(zoneId);

        Zone build = new Zone.Builder().setHashMap(zoneHashMap).build();

        String ref = null;
        if (vacation)
            ref = build.getVacationRef();
        else
            ref = build.getScheduleRef();

        if (ref != null && !ref.equals("")) {
            Schedule schedule = CCUHsApi.getInstance().getScheduleById(ref);

            if (schedule != null && !schedule.mMarkers.contains("disabled")) {
                CcuLog.d("Schedule", "Schedule: " + schedule.toString());
                return schedule;
            }
        }

        return CCUHsApi.getInstance().getSystemSchedule(vacation);
    }


    public static Schedule disableScheduleForZone(String zoneId, boolean enabled)
    {
        HashMap zoneHashMap = CCUHsApi.getInstance().readMapById(zoneId);
        Zone build = new Zone.Builder().setHashMap(zoneHashMap).build();
        boolean currentlyDisabled = build.getMarkers().contains("disabled");

        //if(currentlyDisabled)
          //  return;
        //else
        return null;

    }




    public boolean checkIntersection(ArrayList<Days> daysArrayList) {

        ArrayList<Interval> intervalsOfAdditions = getScheduledIntervals(daysArrayList);
        ArrayList<Interval> intervalsOfCurrent = getScheduledIntervals(getDaysSorted());

        for (Interval additions : intervalsOfAdditions) {
            for (Interval current : intervalsOfCurrent) {

                boolean hasOverlap = additions.overlaps(current);
                if (hasOverlap)
                    return true;
            }
        }

        return false;
    }

    private DateTime getTime() {
        return new DateTime(MockTime.getInstance().getMockTime());
    }

    public Occupied getCurrentValues() {
        Occupied occupied = null;
        ArrayList<Days> daysSorted = getDaysSorted();
        ArrayList<Interval> scheduledIntervals = getScheduledIntervals(daysSorted);

        for (int i = 0; i < daysSorted.size(); i++) {
            if (scheduledIntervals.get(i).contains(getTime().getMillis()) || scheduledIntervals.get(i).isAfter(getTime().getMillis())) {

                boolean currentlyOccupied = scheduledIntervals.get(i).contains(getTime().getMillis());
                occupied = new Occupied();
                occupied.setOccupied(currentlyOccupied);
                occupied.setValue(daysSorted.get(i).mVal);
                occupied.setCoolingVal(daysSorted.get(i).mCoolingVal);
                occupied.setHeatingVal(daysSorted.get(i).mHeatingVal);

                if (currentlyOccupied) {
                    occupied.setCurrentlyOccupiedSchedule(daysSorted.get(i));
                } else {
                    occupied.setNextOccupiedSchedule(daysSorted.get(i));
                }

                return occupied;
            }
        }


        /* In case it runs off the ends of the schedule */
        if (daysSorted.size() > 0) {
            occupied = new Occupied();
            occupied.setOccupied(false);
            occupied.setValue(daysSorted.get(0).mVal);
            occupied.setCoolingVal(daysSorted.get(0).mCoolingVal);
            occupied.setHeatingVal(daysSorted.get(0).mHeatingVal);
            occupied.setNextOccupiedSchedule(daysSorted.get(0));
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

    private String mId;
    private boolean mIsVacation;
    private String mDis;
    private HashSet<String> mMarkers;
    private String mKind;
    private String mUnit;
    private ArrayList<Days> mDays = new ArrayList<Days>();

    public String getmSiteId() {
        return mSiteId;
    }

    private String mSiteId;

    public String getTZ() {
        return mTZ;
    }

    private String mTZ;

    public String getId() {
        return mId;
    }

    public boolean isVacation() {
        return mIsVacation;
    }

    public String getDis() {
        return mDis;
    }

    public HashSet<String> getMarkers() {
        return mMarkers;
    }

    public String getKind() {
        return mKind;
    }

    public String getUnit() {
        return mUnit;
    }

    public ArrayList<Days> getDays() {
        return mDays;
    }


    private ArrayList<Interval> getScheduledIntervals(ArrayList<Days> daysSorted) {
        ArrayList<Interval> intervals = new ArrayList<Interval>();

        for (Days day : daysSorted) {


            DateTime startDateTime = new DateTime(MockTime.getInstance().getMockTime())
                    .withHourOfDay(day.getSthh())
                    .withMinuteOfHour(day.getStmm())
                    .withDayOfWeek(day.getDay() + 1)
                    .withSecondOfMinute(0);
            DateTime endDateTime = new DateTime(MockTime.getInstance().getMockTime())
                    .withHourOfDay(day.getEthh())
                    .withMinuteOfHour(day.getEtmm())
                    .withSecondOfMinute(0).withDayOfWeek(
                            day.getDay() +
                                    1);

            Interval scheduledInterval = null;
            if(startDateTime.isAfter(endDateTime))
            {
                scheduledInterval = new Interval(endDateTime, startDateTime.withDayOfWeek(DAYS.values()[day.getDay()].getNextDay().ordinal() + 1));
            }
            else {
                scheduledInterval =
                        new Interval(startDateTime, endDateTime);
            }


            intervals.add(scheduledInterval);
        }

        return intervals;
    }

    /**
     * Sorts the days by MM, then by HH, then by DD
     *
     * @return Sorted list of days
     */
    public ArrayList<Days> getDaysSorted() {

        Collections.sort(mDays, new Comparator<Days>() {
            @Override
            public int compare(Days o1, Days o2) {
                return Integer.valueOf(o1.getStmm()).compareTo(Integer.valueOf(o2.getStmm()));
            }
        });

        Collections.sort(mDays, new Comparator<Days>() {
            @Override
            public int compare(Days o1, Days o2) {
                return Integer.valueOf(o1.getSthh()).compareTo(Integer.valueOf(o2.getSthh()));
            }
        });

        Collections.sort(mDays, new Comparator<Days>() {
            @Override
            public int compare(Days o1, Days o2) {
                return Integer.valueOf(o1.mDay).compareTo(Integer.valueOf(o2.mDay));
            }
        });

        return mDays;
    }

    public void populateIntersections() {
        ArrayList<Interval> scheduledIntervals = getScheduledIntervals(getDays());

        for(int i = 0; i < scheduledIntervals.size(); i++)
        {
            for(int ii = 0; ii < scheduledIntervals.size(); ii++)
            {
                if(scheduledIntervals.get(i).getEndMillis() == scheduledIntervals.get(ii).getStartMillis())
                {
                    this.mDays.get(i).setIntersection(true);
                }
            }
        }

    }


    public static class Builder {
        private String mId;

        private boolean mIsVacation;
        private String mDis;
        private HashSet<String> mMarkers = new HashSet<String>();
        private String mKind;
        private String mUnit;
        private ArrayList<Days> mDays = new ArrayList<Days>();
        private String mTZ;
        private String mSiteId;

        public Schedule.Builder setId(String id) {
            this.mId = id;
            return this;
        }

        public Schedule.Builder setVacation(boolean vacation) {
            this.mIsVacation = vacation;
            return this;
        }

        public Schedule.Builder setDisplayName(String displayName) {
            this.mDis = displayName;
            return this;
        }

        public Schedule.Builder setMarkers(HashSet<String> markers) {
            this.mMarkers = markers;
            return this;
        }

        public Schedule.Builder addMarker(String marker) {
            this.mMarkers.add(marker);
            return this;
        }

        public Schedule.Builder setKind(String kind) {
            this.mKind = kind;
            return this;
        }

        //TODO make unit enum / strings
        public Schedule.Builder setUnit(String unit) {
            this.mUnit = unit;
            return this;
        }

        public Schedule.Builder setDays(ArrayList<Days> days) {
            this.mDays = days;
            return this;
        }

        public Schedule.Builder setTz(String tz) {
            this.mTZ = tz;
            return this;
        }

        public Schedule build() {
            Schedule s = new Schedule();
            s.mId = this.mId;
            s.mMarkers = this.mMarkers;
            s.mIsVacation = this.mIsVacation;
            s.mDis = this.mDis;
            s.mKind = this.mKind;
            s.mSiteId = this.mSiteId;
            s.mUnit = this.mUnit;
            s.mDays = this.mDays;
            s.mTZ = this.mTZ;
            return s;
        }


        public Schedule.Builder setHDict(HDict schedule) {

            Iterator it = schedule.iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                //System.out.println(pair.getKey() + " = " + pair.getValue());
                if (pair.getKey().equals("id")) {
                    this.mId = pair.getValue().toString().replaceAll("@", "");
                } else if (pair.getKey().equals("dis")) {
                    this.mDis = pair.getValue().toString();
                } else if (pair.getKey().equals("vacation")) {
                    this.mIsVacation = true;
                } else if (pair.getKey().equals("kind")) {
                    this.mKind = pair.getValue().toString();
                } else if (pair.getKey().equals("unit")) {
                    this.mUnit = pair.getValue().toString();
                } else if (pair.getKey().equals("tz")) {
                    this.mTZ = pair.getValue().toString();
                } else if (pair.getKey().equals("days")) {
                    this.mDays = Days.parse((HList) pair.getValue());
                } else if (pair.getKey().equals("siteRef")) {
                    this.mSiteId = schedule.getRef("siteRef").val;
                } else {
                    this.mMarkers.add(pair.getKey().toString());
                }
            }
            return this;
        }
    }


    public static class Days {


        @Override
        public boolean equals(Object o) {
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
                    Objects.equals(mCoolingVal, days.mCoolingVal);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mSthh, mStmm, mDay, mVal, mHeatingVal, mCoolingVal, mEtmm, mEthh, mSunrise, mSunset);
        }


        private boolean mIntersection;
        private int mSthh;
        private int mStmm;
        private int mDay;
        private Double mVal;
        private Double mHeatingVal;
        private Double mCoolingVal;
        private int mEtmm;
        private int mEthh;
        private boolean mSunrise;
        private boolean mSunset;


        public int getSthh() {
            return mSthh;
        }

        public void setSthh(int sthh) {
            this.mSthh = sthh;
        }

        public int getStmm() {
            return mStmm;
        }

        public void setStmm(int stmm) {
            this.mStmm = stmm;
        }

        public int getDay() {
            return mDay;
        }

        public void setDay(int day) {
            this.mDay = day;
        }

        public Double getVal() {
            return mVal;
        }

        public void setVal(Double val) {
            this.mVal = val;
        }

        public int getEtmm() {
            return mEtmm;
        }

        public void setEtmm(int etmm) {
            this.mEtmm = etmm;
        }

        public int getEthh() {
            return mEthh;
        }

        public void setEthh(int ethh) {
            this.mEthh = ethh;
        }

        public boolean isSunrise() {
            return mSunrise;
        }

        public void setSunrise(boolean sunrise) {
            this.mSunrise = sunrise;
        }

        public boolean isSunset() {
            return mSunset;
        }

        public void setSunset(boolean sunset) {
            this.mSunset = sunset;
        }

        public static ArrayList<Days> parse(HList value) {
            ArrayList<Days> days = new ArrayList<Days>();
            for (int i = 0; i < value.size(); i++) {
                days.add(parseSingleDay((HDict) value.get(i)));
            }

            return days;
        }

        private static Days parseSingleDay(HDict hDict) {
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

            return days;
        }

        public Double getHeatingVal() {
            return mHeatingVal;
        }

        public void setHeatingVal(Double heatingVal) {
            this.mHeatingVal = heatingVal;
        }

        public Double getCoolingVal() {
            return mCoolingVal;
        }

        public void setCoolingVal(Double coolingVal) {
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
    }

    public HDict getScheduleHDict() {
        HDict[] days = new HDict[getDays().size()];

        for (int i = 0; i < getDays().size(); i++) {
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

            //need boolean & string support
            if (day.mSunset) hDictDay.add("sunset", day.mSunset);
            if (day.mSunrise) hDictDay.add("sunrise", day.mSunrise);

            days[i] = hDictDay.toDict();
        }

        HList hList = HList.make(days);
        HDictBuilder defaultSchedule = new HDictBuilder()
                .add("id", getId())
                .add("unit", getUnit())
                .add("kind", getKind())
                .add("dis", "Default Site Schedule")
                .add("days", hList)
                .add("siteRef", HRef.make(mSiteId));

        for (String marker : getMarkers()) {
            defaultSchedule.add(marker);
        }

        return defaultSchedule.toDict();
    }


}
