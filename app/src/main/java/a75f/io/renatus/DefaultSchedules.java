package a75f.io.renatus;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HTimeZone;

import java.util.UUID;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.bo.building.definitions.DAYS;

public class DefaultSchedules {


    private static final double DEFAULT_COOLING_VACATION_TEMP = 77.0F;
    private static final double DEFAULT_HEATING_VACATION_TEMP = 65.0F;

    private static final double DEFAULT_COOLING_TEMP = 75.0F;
    private static final double DEFAULT_HEATING_TEMP = 70.0F;

    public static void generateDefaultSchedule() {

        HRef siteId = CCUHsApi.getInstance().getSiteId();

        HDict[] daysArrayCooling = new HDict[5];

        daysArrayCooling[0] = getDefaultForDay(DAYS.MONDAY.ordinal(), DEFAULT_COOLING_TEMP);
        daysArrayCooling[1] = getDefaultForDay(DAYS.TUESDAY.ordinal(), DEFAULT_COOLING_TEMP);
        daysArrayCooling[2] = getDefaultForDay(DAYS.WEDNESDAY.ordinal(), DEFAULT_COOLING_TEMP);
        daysArrayCooling[3] = getDefaultForDay(DAYS.THURSDAY.ordinal(), DEFAULT_COOLING_TEMP);
        daysArrayCooling[4] = getDefaultForDay(DAYS.FRIDAY.ordinal(), DEFAULT_COOLING_TEMP);

        HDict[] daysArrayHeating = new HDict[5];

        daysArrayHeating[0] = getDefaultForDay(DAYS.MONDAY.ordinal(), DEFAULT_HEATING_TEMP);
        daysArrayHeating[1] = getDefaultForDay(DAYS.TUESDAY.ordinal(), DEFAULT_HEATING_TEMP);
        daysArrayHeating[2] = getDefaultForDay(DAYS.WEDNESDAY.ordinal(), DEFAULT_HEATING_TEMP);
        daysArrayHeating[3] = getDefaultForDay(DAYS.THURSDAY.ordinal(), DEFAULT_HEATING_TEMP);
        daysArrayHeating[4] = getDefaultForDay(DAYS.FRIDAY.ordinal(), DEFAULT_HEATING_TEMP);

        HList hListCooling = HList.make(daysArrayCooling);
        HList hListHeating = HList.make(daysArrayHeating);

        HDict hDictCooling = new HDictBuilder()
                .add("cooling")
                .add("days", hListCooling).toDict();

        HDict hDictHeating = new HDictBuilder()
                .add("heating")
                .add("days", hListHeating).toDict();

        HDict[] coolingHeatingArray = new HDict[2];

        coolingHeatingArray[0] = hDictCooling;
        coolingHeatingArray[1] = hDictHeating;

        HList coolingHeatingList = HList.make(coolingHeatingArray);




        String localId = UUID.randomUUID().toString();
        HDict defaultSchedule = new HDictBuilder()
                .add("id", localId)
                .add("unit", "\\u00B0F")
                .add("kind", "Number")
                .add("temp")
                .add("schedule")
                .add("dis", "Default Site Schedule")
                .add("days", coolingHeatingList)
                .add("siteRef", siteId)
                .toDict();

        CCUHsApi.getInstance().addSchedule(localId, defaultSchedule);
    }

    private static HDict getDefaultForDay(int day, double temp)
    {
        HDict hDictDay = new HDictBuilder()
                .add("day", HNum.make(day))
                .add("sthh", HNum.make(8))
                .add("stmm", HNum.make(30))
                .add("ethh", HNum.make(17))
                .add("etmm", HNum.make(30))
                .add("val", HNum.make(temp)).toDict();

        return hDictDay;
    }


    public static void generateDefaultVacation()
    {

        HRef siteId = CCUHsApi.getInstance().getSiteId();

        HDict[] daysArrayCooling = new HDict[5];

        daysArrayCooling[0] = getDefaultForDay(DAYS.MONDAY.ordinal(), DEFAULT_COOLING_VACATION_TEMP);
        daysArrayCooling[1] = getDefaultForDay(DAYS.TUESDAY.ordinal(), DEFAULT_COOLING_VACATION_TEMP);
        daysArrayCooling[2] = getDefaultForDay(DAYS.WEDNESDAY.ordinal(), DEFAULT_COOLING_VACATION_TEMP);
        daysArrayCooling[3] = getDefaultForDay(DAYS.THURSDAY.ordinal(), DEFAULT_COOLING_VACATION_TEMP);
        daysArrayCooling[4] = getDefaultForDay(DAYS.FRIDAY.ordinal(), DEFAULT_COOLING_VACATION_TEMP);

        HDict[] daysArrayHeating = new HDict[5];

        daysArrayHeating[0] = getDefaultForDay(DAYS.MONDAY.ordinal(), DEFAULT_HEATING_VACATION_TEMP);
        daysArrayHeating[1] = getDefaultForDay(DAYS.TUESDAY.ordinal(), DEFAULT_HEATING_VACATION_TEMP);
        daysArrayHeating[2] = getDefaultForDay(DAYS.WEDNESDAY.ordinal(), DEFAULT_HEATING_VACATION_TEMP);
        daysArrayHeating[3] = getDefaultForDay(DAYS.THURSDAY.ordinal(), DEFAULT_HEATING_VACATION_TEMP);
        daysArrayHeating[4] = getDefaultForDay(DAYS.FRIDAY.ordinal(), DEFAULT_HEATING_VACATION_TEMP);



        HList hListCooling = HList.make(daysArrayCooling);
        HList hListHeating = HList.make(daysArrayHeating);

        HDict hDictCooling = new HDictBuilder()
                .add("cooling")
                .add("days", hListCooling).toDict();

        HDict hDictHeating = new HDictBuilder()
                .add("heating")
                .add("days", hListHeating).toDict();

        HDict[] coolingHeatingArray = new HDict[2];

        coolingHeatingArray[0] = hDictCooling;
        coolingHeatingArray[1] = hDictHeating;

        HList coolingHeatingList = HList.make(coolingHeatingArray);




        String localId = UUID.randomUUID().toString();
        HDict defaultSchedule = new HDictBuilder()
                .add("id", localId)
                .add("unit", "\\u00B0F")
                .add("kind", "Number")
                .add("temp")
                .add("schedule")
                .add("vacation")
                .add("stdt", HDateTime.make(2019, 12, 24, 0,0, HTimeZone.UTC, 0))
                .add("etdt", HDateTime.make(2019, 12, 30, 0,0, HTimeZone.UTC, 0))
                .add("dis", "Default Site Schedule")
                .add("days", coolingHeatingList)
                .add("siteRef", siteId)
                .toDict();


        CCUHsApi.getInstance().addSchedule(localId, defaultSchedule);

    }
}
