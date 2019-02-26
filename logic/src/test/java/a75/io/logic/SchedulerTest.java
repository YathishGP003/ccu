package a75.io.logic;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HRef;

import java.util.UUID;

import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Schedule;
import a75f.io.logic.DefaultSchedules;
import a75f.io.api.haystack.DAYS;

public class SchedulerTest
{

    private static final long MOCK_TIME = 1504901339999L;
    private static final long OUT_OF_SCHEDULE_MOCK_TIME = 1504912920000L;


	/*

	+++x +++++++++
	FRI
	2017 Sep 08 23:22:00 UTC
	FRI
	2017 Sep 08 18:22:00 UTC-5:00
	----  -  -- -- -- --
	*/
    private Schedule schedule;

    @Before
    public void setUpMockSchedule()
    {

        schedule = new Schedule.Builder().setHDict(generateDefaultTestSchedule()).build();
    }

    @Test
    public void testCoolingTemperatureInMockTime()
    {
        MockTime.getInstance().setMockTime(true, MOCK_TIME);
        Occupied coolingTemp = schedule.getCurrentValueForMarker("cooling");
        Assert.assertNotNull(coolingTemp);
        Assert.assertEquals(coolingTemp.getValue(), 75.0);
    }

    @Test
    public void testHeatingTemperatureInMockTime()
    {
        MockTime.getInstance().setMockTime(true, MOCK_TIME);
        Occupied coolingTemp = schedule.getCurrentValueForMarker("heating");
        Assert.assertNotNull(coolingTemp);
        Assert.assertEquals(coolingTemp.getValue(), 70.0);
    }

    @Test
    public void testCoolingTemperatureOutOfMockTime()
    {
        MockTime.getInstance().setMockTime(true, OUT_OF_SCHEDULE_MOCK_TIME);
        Occupied coolingTemp = schedule.getCurrentValueForMarker("cooling");
        Assert.assertNull(coolingTemp);
    }

    @Test
    public void testHeatingTemperatureOutOfMockTime()
    {
        MockTime.getInstance().setMockTime(true, OUT_OF_SCHEDULE_MOCK_TIME);
        Occupied coolingTemp = schedule.getCurrentValueForMarker("heating");
        Assert.assertNull(coolingTemp);
    }

    public static HDict generateDefaultTestSchedule() {

        HRef siteId = HRef.make("cooling_test");

        HDict[] days = new HDict[10];

        days[0] = DefaultSchedules.getDefaultForDay(true, DAYS.MONDAY.ordinal(), DefaultSchedules.DEFAULT_COOLING_TEMP);
        days[1] = DefaultSchedules.getDefaultForDay(true, DAYS.TUESDAY.ordinal(), DefaultSchedules.DEFAULT_COOLING_TEMP);
        days[2] = DefaultSchedules.getDefaultForDay(true, DAYS.WEDNESDAY.ordinal(), DefaultSchedules.DEFAULT_COOLING_TEMP);
        days[3] = DefaultSchedules.getDefaultForDay(true, DAYS.THURSDAY.ordinal(), DefaultSchedules.DEFAULT_COOLING_TEMP);
        days[4] = DefaultSchedules.getDefaultForDay(true, DAYS.FRIDAY.ordinal(), DefaultSchedules.DEFAULT_COOLING_TEMP);


        days[5] = DefaultSchedules.getDefaultForDay(false, DAYS.MONDAY.ordinal(), DefaultSchedules.DEFAULT_HEATING_TEMP);
        days[6] = DefaultSchedules.getDefaultForDay(false, DAYS.TUESDAY.ordinal(), DefaultSchedules.DEFAULT_HEATING_TEMP);
        days[7] = DefaultSchedules.getDefaultForDay(false, DAYS.WEDNESDAY.ordinal(), DefaultSchedules.DEFAULT_HEATING_TEMP);
        days[8] = DefaultSchedules.getDefaultForDay(false, DAYS.THURSDAY.ordinal(), DefaultSchedules.DEFAULT_HEATING_TEMP);
        days[9] = DefaultSchedules.getDefaultForDay(false, DAYS.FRIDAY.ordinal(), DefaultSchedules.DEFAULT_HEATING_TEMP);

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

}
