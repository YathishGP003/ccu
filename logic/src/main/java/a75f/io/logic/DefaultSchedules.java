package a75f.io.logic;

import org.joda.time.DateTime;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.UUID;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;

public class DefaultSchedules {

    public static final double DEFAULT_COOLING_TEMP = 75.0F;
    public static final double DEFAULT_HEATING_TEMP = 70.0F;

    public static String generateDefaultSchedule() {

        HRef siteId = CCUHsApi.getInstance().getSiteId();

        HDict[] days = new HDict[5];

        days[0] = getDefaultForDay(DAYS.MONDAY.ordinal());
        days[1] = getDefaultForDay(DAYS.TUESDAY.ordinal());
        days[2] = getDefaultForDay(DAYS.WEDNESDAY.ordinal());
        days[3] = getDefaultForDay(DAYS.THURSDAY.ordinal());
        days[4] = getDefaultForDay(DAYS.FRIDAY.ordinal());

        HList hList = HList.make(days);

        HRef localId = HRef.make(UUID.randomUUID().toString());
        HDict defaultSchedule = new HDictBuilder()
                .add("id", localId)
                .add("unit", "\\u00B0F")
                .add("kind", "Number")
                .add("system")
                .add("temp")
                .add("schedule")
                .add("heating")
                .add("cooling")
                .add("dis", "Default Site Schedule")
                .add("days", hList)
                .add("siteRef", siteId)
                .toDict();

        CCUHsApi.getInstance().addSchedule(localId.toVal(), defaultSchedule);
        return localId.toCode();
    }


    public static HDict getDefaultForDay(int day) {
        HDict hDictDay = new HDictBuilder()
                .add("day", HNum.make(day))
                .add("sthh", HNum.make(8))
                .add("stmm", HNum.make(0))
                .add("ethh", HNum.make(17))
                .add("etmm", HNum.make(30))
                .add("coolVal", HNum.make(DEFAULT_COOLING_TEMP))
                .add("heatVal", HNum.make(DEFAULT_HEATING_TEMP))
                .toDict();

        return hDictDay;
    }

    public static void upsertVacation(String id, String vacationName, DateTime startDate, DateTime endDate)
    {
        HRef siteId = CCUHsApi.getInstance().getSiteId();
        HRef localId;

        if(id == null) {
            localId = HRef.make(UUID.randomUUID().toString());
        }
        else {
            localId = HRef.make(id);
        }

        HDict defaultSchedule = new HDictBuilder()
                .add("id", localId)
                .add("temp")
                .add("schedule")
                .add("system")
                .add("vacation")
                .add("cooling")
                .add("heating")
                .add("stdt", HDateTime.make(startDate.getMillis()))
                .add("etdt", HDateTime.make(endDate.getMillis()))
                .add("dis", vacationName)
                .add("siteRef", siteId)
                .toDict();

        CCUHsApi.getInstance().addSchedule(localId.toVal(), defaultSchedule);
    }
}
