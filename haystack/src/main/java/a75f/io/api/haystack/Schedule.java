package a75f.io.api.haystack;

import org.joda.time.DateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HVal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;


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


public class Schedule extends Entity
{

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
            s.mMarkers = this.mMarkers;
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
                    this.mId = pair.getValue().toString();
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


        private boolean isCooling;
        private boolean isHeating;


        private int mSthh;
        private int mStmm;
        private int mDay;
        private double mVal;
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

        public double getVal() {
            return mVal;
        }

        public void setVal(double val) {
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

            days.isCooling = hDict.has("cooling");
            days.isHeating = hDict.has("heating");
            days.mDay = hDict.getInt("day");
            days.mEthh = hDict.getInt("ethh");
            days.mEtmm = hDict.getInt("etmm");
            days.mSthh = hDict.getInt("sthh");
            days.mStmm = hDict.getInt("stmm");
            days.mSunrise = hDict.has("sunrise");
            days.mSunset = hDict.has("sunset");
            days.mVal = hDict.getDouble("curVal");

            return days;
        }

        public void check(int dayOfWeek, int hourOfDay, int minuteOfHour) {


        }
    }

    public HDict getScheduleHDict() {
        HDict[] days = new HDict[getDays().size()];

        for (int i = 0; i < getDays().size(); i++) {
            Days day = mDays.get(i);
            HDictBuilder hDictDay = new HDictBuilder()
                    .add(day.isCooling ? "cooling" : "heating")
                    .add("day", HNum.make(day.mDay))
                    .add("sthh", HNum.make(day.mSthh))
                    .add("stmm", HNum.make(day.mStmm))
                    .add("ethh", HNum.make(day.mEthh))
                    .add("etmm", HNum.make(day.mEtmm))
                    .add("curVal", HNum.make(day.mVal)); //need boolean & string support
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
