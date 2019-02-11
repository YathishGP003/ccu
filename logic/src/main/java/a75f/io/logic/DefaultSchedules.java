package a75f.io.logic;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HTimeZone;

import java.util.UUID;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.bo.building.definitions.DAYS;

public class DefaultSchedules {


    public static final double DEFAULT_COOLING_VACATION_TEMP = 77.0F;
    public static final double DEFAULT_HEATING_VACATION_TEMP = 65.0F;

    public static final double DEFAULT_COOLING_TEMP = 75.0F;
    public static final double DEFAULT_HEATING_TEMP = 70.0F;

    public static String generateDefaultSchedule() {

        HRef siteId = CCUHsApi.getInstance().getSiteId();

        HDict[] days = new HDict[10];

        days[0] = getDefaultForDay(true, DAYS.MONDAY.ordinal(), DEFAULT_COOLING_TEMP);
        days[1] = getDefaultForDay(true, DAYS.TUESDAY.ordinal(), DEFAULT_COOLING_TEMP);
        days[2] = getDefaultForDay(true, DAYS.WEDNESDAY.ordinal(), DEFAULT_COOLING_TEMP);
        days[3] = getDefaultForDay(true, DAYS.THURSDAY.ordinal(), DEFAULT_COOLING_TEMP);
        days[4] = getDefaultForDay(true, DAYS.FRIDAY.ordinal(), DEFAULT_COOLING_TEMP);


        days[5] = getDefaultForDay(false, DAYS.MONDAY.ordinal(), DEFAULT_HEATING_TEMP);
        days[6] = getDefaultForDay(false, DAYS.TUESDAY.ordinal(), DEFAULT_HEATING_TEMP);
        days[7] = getDefaultForDay(false, DAYS.WEDNESDAY.ordinal(), DEFAULT_HEATING_TEMP);
        days[8] = getDefaultForDay(false, DAYS.THURSDAY.ordinal(), DEFAULT_HEATING_TEMP);
        days[9] = getDefaultForDay(false, DAYS.FRIDAY.ordinal(), DEFAULT_HEATING_TEMP);

        HList hList = HList.make(days);

        String localId = UUID.randomUUID().toString();
        HDict defaultSchedule = new HDictBuilder()
                .add("id", localId)
                .add("unit", "\\u00B0F")
                .add("kind", "Number")
                .add("system")
                .add("temp")
                .add("schedule")
                .add("dis", "Default Site Schedule")
                .add("days", hList)
                .add("siteRef", siteId)
                .toDict();

        CCUHsApi.getInstance().addSchedule(localId, defaultSchedule);
        return localId;
    }

    public static HDict generateDefaultTestSchedule() {

        HRef siteId = CCUHsApi.getInstance().getSiteId();

        HDict[] days = new HDict[10];

        days[0] = getDefaultForDay(true, DAYS.MONDAY.ordinal(), DEFAULT_COOLING_TEMP);
        days[1] = getDefaultForDay(true, DAYS.TUESDAY.ordinal(), DEFAULT_COOLING_TEMP);
        days[2] = getDefaultForDay(true, DAYS.WEDNESDAY.ordinal(), DEFAULT_COOLING_TEMP);
        days[3] = getDefaultForDay(true, DAYS.THURSDAY.ordinal(), DEFAULT_COOLING_TEMP);
        days[4] = getDefaultForDay(true, DAYS.FRIDAY.ordinal(), DEFAULT_COOLING_TEMP);


        days[5] = getDefaultForDay(false, DAYS.MONDAY.ordinal(), DEFAULT_HEATING_TEMP);
        days[6] = getDefaultForDay(false, DAYS.TUESDAY.ordinal(), DEFAULT_HEATING_TEMP);
        days[7] = getDefaultForDay(false, DAYS.WEDNESDAY.ordinal(), DEFAULT_HEATING_TEMP);
        days[8] = getDefaultForDay(false, DAYS.THURSDAY.ordinal(), DEFAULT_HEATING_TEMP);
        days[9] = getDefaultForDay(false, DAYS.FRIDAY.ordinal(), DEFAULT_HEATING_TEMP);

        HList hList = HList.make(days);

        String localId = UUID.randomUUID().toString();
        HDict defaultSchedule = new HDictBuilder()
                .add("id", localId)
                .add("unit", "\\u00B0F")
                .add("kind", "Number")
                .add("temp")
                .add("schedule")
                .add("dis", "Default Site Schedule")
                .add("days", hList)
                .add("siteRef", siteId)
                .toDict();

        return defaultSchedule;
    }

    public static HDict getDefaultForDay(boolean cooling, int day, double temp) {
        HDict hDictDay = new HDictBuilder()
                .add(cooling ? "cooling" : "heating")
                .add("day", HNum.make(day))
                .add("sthh", HNum.make(8))
                .add("stmm", HNum.make(0))
                .add("ethh", HNum.make(17))
                .add("etmm", HNum.make(30))
                .add("curVal", HNum.make(temp)).toDict();

        return hDictDay;
    }


    public static void generateDefaultVacation() {

        HRef siteId = CCUHsApi.getInstance().getSiteId();

        HDict[] days = new HDict[10];

        days[0] = getDefaultForDay(true, DAYS.MONDAY.ordinal(), DEFAULT_COOLING_VACATION_TEMP);
        days[1] = getDefaultForDay(true, DAYS.TUESDAY.ordinal(), DEFAULT_COOLING_VACATION_TEMP);
        days[2] = getDefaultForDay(true, DAYS.WEDNESDAY.ordinal(), DEFAULT_COOLING_VACATION_TEMP);
        days[3] = getDefaultForDay(true, DAYS.THURSDAY.ordinal(), DEFAULT_COOLING_VACATION_TEMP);
        days[4] = getDefaultForDay(true, DAYS.FRIDAY.ordinal(), DEFAULT_COOLING_VACATION_TEMP);

        days[5] = getDefaultForDay(false, DAYS.MONDAY.ordinal(), DEFAULT_HEATING_VACATION_TEMP);
        days[6] = getDefaultForDay(false, DAYS.TUESDAY.ordinal(), DEFAULT_HEATING_VACATION_TEMP);
        days[7] = getDefaultForDay(false, DAYS.WEDNESDAY.ordinal(), DEFAULT_HEATING_VACATION_TEMP);
        days[8] = getDefaultForDay(false, DAYS.THURSDAY.ordinal(), DEFAULT_HEATING_VACATION_TEMP);
        days[9] = getDefaultForDay(false, DAYS.FRIDAY.ordinal(), DEFAULT_HEATING_VACATION_TEMP);


        HList hList = HList.make(days);


        String localId = UUID.randomUUID().toString();
        HDict defaultSchedule = new HDictBuilder()
                .add("id", localId)
                .add("unit", "\\u00B0F")
                .add("kind", "Number")
                .add("temp")
                .add("schedule")
                .add("vacation")
                .add("stdt", HDateTime.make(2019, 12, 24, 0, 0, HTimeZone.UTC, 0))
                .add("etdt", HDateTime.make(2019, 12, 30, 0, 0, HTimeZone.UTC, 0))
                .add("dis", "Default Site Schedule")
                .add("days", hList)
                .add("siteRef", siteId)
                .toDict();


        CCUHsApi.getInstance().addSchedule(localId, defaultSchedule);

    }

    public static void generateDefaultSchedule(HRef siteId) {

        HDict[] days = new HDict[10];

        days[0] = getDefaultForDay(true, DAYS.MONDAY.ordinal(), DEFAULT_COOLING_TEMP);
        days[1] = getDefaultForDay(true, DAYS.TUESDAY.ordinal(), DEFAULT_COOLING_TEMP);
        days[2] = getDefaultForDay(true, DAYS.WEDNESDAY.ordinal(), DEFAULT_COOLING_TEMP);
        days[3] = getDefaultForDay(true, DAYS.THURSDAY.ordinal(), DEFAULT_COOLING_TEMP);
        days[4] = getDefaultForDay(true, DAYS.FRIDAY.ordinal(), DEFAULT_COOLING_TEMP);


        days[5] = getDefaultForDay(false, DAYS.MONDAY.ordinal(), DEFAULT_HEATING_TEMP);
        days[6] = getDefaultForDay(false, DAYS.TUESDAY.ordinal(), DEFAULT_HEATING_TEMP);
        days[7] = getDefaultForDay(false, DAYS.WEDNESDAY.ordinal(), DEFAULT_HEATING_TEMP);
        days[8] = getDefaultForDay(false, DAYS.THURSDAY.ordinal(), DEFAULT_HEATING_TEMP);
        days[9] = getDefaultForDay(false, DAYS.FRIDAY.ordinal(), DEFAULT_HEATING_TEMP);

        HList hList = HList.make(days);

        String localId = UUID.randomUUID().toString();
        HDict defaultSchedule = new HDictBuilder()
                .add("id", localId)
                .add("unit", "\\u00B0F")
                .add("kind", "Number")
                .add("system")
                .add("temp")
                .add("schedule")
                .add("dis", "Default Site Schedule")
                .add("days", hList)
                .add("siteRef", siteId)
                .toDict();

        CCUHsApi.getInstance().addSchedule(localId, defaultSchedule);
    }
}
