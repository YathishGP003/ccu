package a75f.io.device.bacnet;

import android.util.Log;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.enums.DayOfWeek;
import com.renovo.bacnet4j.enums.Month;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.AnalogValueObject;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.obj.BinaryValueObject;
import com.renovo.bacnet4j.obj.CalendarObject;
import com.renovo.bacnet4j.obj.MultistateValueObject;
import com.renovo.bacnet4j.obj.ScheduleObject;
import com.renovo.bacnet4j.type.constructed.BACnetArray;
import com.renovo.bacnet4j.type.constructed.CalendarEntry;
import com.renovo.bacnet4j.type.constructed.DailySchedule;
import com.renovo.bacnet4j.type.constructed.DateRange;
import com.renovo.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.constructed.SpecialEvent;
import com.renovo.bacnet4j.type.constructed.TimeValue;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.enumerated.BinaryPV;
import com.renovo.bacnet4j.type.enumerated.EngineeringUnits;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.Date;
import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.type.primitive.Null;
import com.renovo.bacnet4j.type.primitive.Primitive;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.type.primitive.Time;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

import org.joda.time.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

public class BACnetScheduler {

    public static ScheduleObject buildingSchedule;
    public static ScheduleObject zoneSchedule;
    public static ArrayList<Schedule> vacations = null;
    public static Schedule schedule = null;
    public static Schedule systemSchedule = null;
    public static Device zoneDevice = null;
    public BinaryValueObject occupancyObject = null;
    public AnalogValueObject occupancyCoolingTemp = null;
    public AnalogValueObject occupancyHeatingTemp = null;
    public BinaryPV occupancyStatus = null;
    public BACnetScheduler() {
    }
    public BACnetScheduler(LocalDevice localDevice, Zone zone) {

        zoneDevice = HSUtil.getDevices(zone.getId()).get(0);

        schedule = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
        systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);

        //Todo Schedule Objects
        try {
            if (!localDevice.checkObjectByID(BACnetUtils.scheduleOccupancy)) {
                occupancyObject = new BinaryValueObject(localDevice, BACnetUtils.scheduleOccupancy, "Building Schedule Occupancy", BinaryPV.inactive, false);
                occupancyObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Occupancy Status")));
                //occupancyObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.activeText, new CharacterString("Occupied")));
                //occupancyObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.inactiveText, new CharacterString("Unoccupied")));
                occupancyObject.supportStateText("Unoccupied","Occupied");
                occupancyObject.supportCommandable(BinaryPV.inactive);
                occupancyObject.supportCovReporting();
            }else {
                occupancyObject = (BinaryValueObject) localDevice.getObjectByID(BACnetUtils.scheduleOccupancy);
            }
            if (!localDevice.checkObjectByID(BACnetUtils.occupanyCoolingDT)) {
                occupancyCoolingTemp = new AnalogValueObject(localDevice, BACnetUtils.occupanyCoolingDT, "Building Schedule Occupied Cooling", (float) BACnetUtils.defaultTemp, EngineeringUnits.degreesFahrenheit, false);
                occupancyCoolingTemp.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Occupancy Cooling Temperature")));
                occupancyCoolingTemp.supportCommandable(72f);
            }else {
                occupancyCoolingTemp = (AnalogValueObject) localDevice.getObjectByID(BACnetUtils.occupanyCoolingDT);
            }
            if (!localDevice.checkObjectByID(BACnetUtils.occupanyHeatingDT)) {
                occupancyHeatingTemp = new AnalogValueObject(localDevice, BACnetUtils.occupanyHeatingDT, "Building Schedule Occupied Heating", (float) BACnetUtils.defaultTemp, EngineeringUnits.degreesFahrenheit, false);
                occupancyHeatingTemp.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Occupancy Heating Temperature")));
                occupancyHeatingTemp.supportCommandable(72f);
            }else {
                occupancyHeatingTemp = (AnalogValueObject) localDevice.getObjectByID(BACnetUtils.occupanyHeatingDT);
            }

        if(systemSchedule != null && systemSchedule.isBuildingSchedule()) {
            //Log.i("Bacnet", "System Schedule Days:" + systemSchedule.getDays());
            //Log.i("Bacnet", "System Schedule:" + systemSchedule.toString());
            ArrayList<Schedule.Days> weekDays = systemSchedule.getDays();

            SequenceOf<TimeValue> mondaySchedule = new SequenceOf<>();
            SequenceOf<TimeValue> tuesdaySchedule = new SequenceOf<>();
            SequenceOf<TimeValue> wednesdaySchedule = new SequenceOf<>();
            SequenceOf<TimeValue> thursdaySchedule = new SequenceOf<>();
            SequenceOf<TimeValue> fridaySchedule = new SequenceOf<>();
            SequenceOf<TimeValue> saturdaySchedule = new SequenceOf<>();
            SequenceOf<TimeValue> sundaySchedule = new SequenceOf<>();

            Schedule.Days currentDay = getCurrentDaySchedule(weekDays);
            java.util.Date currentTime = Calendar.getInstance().getTime();
            String currentDate = currentTime.getDate()+"/"+(currentTime.getMonth()+1)+"/"+(1900+currentTime.getYear());
            String strStartTime = currentDate+" "+currentDay.getSthh()+":"+currentDay.getStmm();
            String strEndTime = currentDate+" "+currentDay.getEthh()+":"+currentDay.getEtmm();

            SimpleDateFormat timeFormat = new SimpleDateFormat("dd/M/yyyy h:mm");
            java.util.Date scheduleStartTime = null;
            java.util.Date scheduleEndTime = null;
            try {
                scheduleStartTime = timeFormat.parse(strStartTime);
                scheduleEndTime = timeFormat.parse(strEndTime);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Log.i("Bacnet","Current Time:"+currentTime+" sT:"+scheduleStartTime+" eT:"+scheduleEndTime+" Date:"+currentDate);
            if(currentTime.after(scheduleStartTime) && currentTime.before(scheduleEndTime)) {
                Log.i("Bacnet","in Occupied Setting Temp to BS - Cooling DT:"+currentDay.getCoolingVal()+" Heating DT:"+currentDay.getHeatingVal()+" SetBack:"+getSetbackTemp());
                //occupancyObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, BinaryPV.active));
                if(occupancyCoolingTemp.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)){
                    occupancyCoolingTemp.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, new Real(currentDay.getCoolingVal().floatValue())));
                    occupancyCoolingTemp.writePropertyInternal(PropertyIdentifier.relinquishDefault, new Real(currentDay.getCoolingVal().floatValue()+(float) getSetbackTemp()));
                }
                if(occupancyHeatingTemp.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
                    occupancyHeatingTemp.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, new Real(currentDay.getHeatingVal().floatValue())));
                    occupancyHeatingTemp.writePropertyInternal(PropertyIdentifier.relinquishDefault, new Real(currentDay.getHeatingVal().floatValue() - (float) getSetbackTemp()));
                }
                //occupancyCoolingTemp.supportCommandable(currentDay.getCoolingVal().floatValue()+(float) getSetbackTemp());
                //occupancyHeatingTemp.supportCommandable(currentDay.getHeatingVal().floatValue()-(float) getSetbackTemp());
            }else{
                Log.i("Bacnet","UnOccupied");
                //occupancyObject.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.inactive);
                if(occupancyCoolingTemp.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
                    //occupancyCoolingTemp.writePropertyInternal(PropertyIdentifier.presentValue, new Null());
                    occupancyCoolingTemp.writePropertyInternal(PropertyIdentifier.relinquishDefault, new Real(currentDay.getCoolingVal().floatValue()+(float) getSetbackTemp()));
                    occupancyCoolingTemp.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, new Null()));
                }
                if(occupancyHeatingTemp.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
                    //occupancyHeatingTemp.writePropertyInternal(PropertyIdentifier.presentValue, new Null());
                    occupancyHeatingTemp.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, new Null()));
                    occupancyHeatingTemp.writePropertyInternal(PropertyIdentifier.relinquishDefault, new Real(currentDay.getHeatingVal().floatValue() - (float) getSetbackTemp()));
                }
            }

            for (int i = 0; i < weekDays.size(); i++) {
                if (weekDays.get(i).getDay() == 0) {
                    Schedule.Days monDay = weekDays.get(i);
                    mondaySchedule.add(new TimeValue(new Time(monDay.getSthh(), monDay.getStmm(), 0, 0), BinaryPV.active));
                    mondaySchedule.add(new TimeValue(new Time(monDay.getEthh(), monDay.getEtmm(), 0, 0), BinaryPV.inactive));
                }
                if (weekDays.get(i).getDay() == 1) {
                    Schedule.Days tuesDay = weekDays.get(i);
                    tuesdaySchedule.add(new TimeValue(new Time(tuesDay.getSthh(), tuesDay.getStmm(), 0, 0), BinaryPV.active));
                    tuesdaySchedule.add(new TimeValue(new Time(tuesDay.getEthh(), tuesDay.getEtmm(), 0, 0), BinaryPV.inactive));
                }
                if (weekDays.get(i).getDay() == 2) {
                    Schedule.Days wednesDay = weekDays.get(i);
                    wednesdaySchedule.add(new TimeValue(new Time(wednesDay.getSthh(), wednesDay.getStmm(), 0, 0), BinaryPV.active));
                    wednesdaySchedule.add(new TimeValue(new Time(wednesDay.getEthh(), wednesDay.getEtmm(), 0, 0), BinaryPV.inactive));
                }
                if (weekDays.get(i).getDay() == 3) {
                    Schedule.Days thursDay = weekDays.get(i);
                    thursdaySchedule.add(new TimeValue(new Time(thursDay.getSthh(), thursDay.getStmm(), 0, 0), BinaryPV.active));
                    thursdaySchedule.add(new TimeValue(new Time(thursDay.getEthh(), thursDay.getEtmm(), 0, 0), BinaryPV.inactive));
                }
                if (weekDays.get(i).getDay() == 4) {
                    Schedule.Days friDay = weekDays.get(i);
                    fridaySchedule.add(new TimeValue(new Time(friDay.getSthh(), friDay.getStmm(), 0, 0), BinaryPV.active));
                    fridaySchedule.add(new TimeValue(new Time(friDay.getEthh(), friDay.getEtmm(), 0, 0), BinaryPV.inactive));
                }
                if (weekDays.get(i).getDay() == 5) {
                    Schedule.Days satDay = weekDays.get(i);
                    saturdaySchedule.add(new TimeValue(new Time(satDay.getSthh(), satDay.getStmm(), 0, 0), BinaryPV.active));
                    saturdaySchedule.add(new TimeValue(new Time(satDay.getEthh(), satDay.getEtmm(), 0, 0), BinaryPV.inactive));
                }
                if (weekDays.get(i).getDay() == 6) {
                    Schedule.Days sunDay = weekDays.get(i);
                    sundaySchedule.add(new TimeValue(new Time(sunDay.getSthh(), sunDay.getStmm(), 0, 0), BinaryPV.active));
                    sundaySchedule.add(new TimeValue(new Time(sunDay.getEthh(), sunDay.getEtmm(), 0, 0), BinaryPV.inactive));
                }
            }
            //Log.i("Bacnet", "Monday Schedule ST:" + systemSchedule.getDays().get(0).getSthh() + ":" + systemSchedule.getDays().get(0).getStmm() + " ET:" + systemSchedule.getDays().get(0).getEthh() + ":" + systemSchedule.getDays().get(0).getEtmm());
            BACnetArray<DailySchedule> weeklySchedule = new BACnetArray<>(
                    new DailySchedule(mondaySchedule),
                    new DailySchedule(tuesdaySchedule),
                    new DailySchedule(wednesdaySchedule),
                    new DailySchedule(thursdaySchedule),
                    new DailySchedule(fridaySchedule),
                    new DailySchedule(saturdaySchedule),
                    new DailySchedule(sundaySchedule)
            );


            if (systemSchedule.isBuildingSchedule()) {
                vacations = CCUHsApi.getInstance().getSystemSchedule( true);
                Log.i("Bacnet","Vacations:"+vacations.toString());
            }
            try {
                final SequenceOf<CalendarEntry> dateList = new SequenceOf<>();
                CalendarObject calendarObject = null;
                if(!localDevice.checkObjectByID(BACnetUtils.systemCalendar)){
                    calendarObject = new CalendarObject(localDevice, BACnetUtils.systemCalendar, "Holiday Calendar", dateList);
                    calendarObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Holiday Calendar - 75F" )));
                }else{
                    calendarObject = (CalendarObject) localDevice.getObjectByID(BACnetUtils.systemCalendar);
                }
                if(vacations != null) {
                    SequenceOf<CalendarEntry> vacationCalendarEntry  = calendarObject.get(PropertyIdentifier.dateList); // Todo Fetching Existing datelist
                    if(compareVacations(vacationCalendarEntry,vacations)) {
                        calendarObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.dateList, new SequenceOf<CalendarEntry>()));
                        SequenceOf<CalendarEntry> newVactionEntry  = calendarObject.get(PropertyIdentifier.dateList);
                        for (int i = 0; i < vacations.size(); i++) {
                            DateTime vacStartDate = vacations.get(i).getStartDate();
                            DateTime vacEndDate = vacations.get(i).getEndDate();
                            long daysDiff = (vacEndDate.getMillis() - vacStartDate.getMillis()) / (1000 * 60 * 60 * 24);
                            Log.i("Bacnet", "Vacations-StartDate:" + vacStartDate + " EndDate:" + vacEndDate + " Diffs:" + daysDiff);
                            if (daysDiff == 0) {//Todo Single Calendar Entry
                                CalendarEntry newCalendarEntry = new CalendarEntry(new Date(vacStartDate.getYear(), Month.valueOf(vacStartDate.getMonthOfYear()), vacStartDate.getDayOfMonth(), DayOfWeek.UNSPECIFIED));

                                if (!newVactionEntry.contains(newCalendarEntry)) {
                                    newVactionEntry.add(newCalendarEntry);
                                }
                            } else {//Todo Multiple Date Calendar Entry
                                Date startDate = new Date(vacStartDate.getYear(), Month.valueOf(vacStartDate.getMonthOfYear()), vacStartDate.getDayOfMonth(), DayOfWeek.UNSPECIFIED);
                                Date endDate = new Date(vacEndDate.getYear(), Month.valueOf(vacEndDate.getMonthOfYear()), vacEndDate.getDayOfMonth(), DayOfWeek.UNSPECIFIED);
                                CalendarEntry newCalendarEntry = new CalendarEntry(new DateRange(startDate, endDate));
                                if (!newVactionEntry.contains(newCalendarEntry)) {
                                    newVactionEntry.add(newCalendarEntry);
                                }
                            }
                        }
                        calendarObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.dateList, newVactionEntry));
                    }
                }

                final SequenceOf<SpecialEvent> exceptionSchedule = new SequenceOf<>(new SpecialEvent(calendarObject.getId(),
                        new SequenceOf<>(new TimeValue(new Time(0, 0, 0, 0), BinaryPV.inactive)), new UnsignedInteger(10)));


                //Todo Creating Link to Set Temperature BACnet object to BACnet Schedule
                final SequenceOf<DeviceObjectPropertyReference> listOfObjectPropertyReferences = new SequenceOf<>();
                int zoneAddress = Integer.parseInt(zoneDevice.getAddr());
                int instanceID = zoneAddress + BACnetUtils.desiredTemp;
                /*if (localDevice.checkObjectByID(instanceID)) {
                    //BinaryValueObject occupiedStatus = new BinaryValueObject(localDevice,1001,"Occupancy", BinaryPV.active,false);
                    AnalogValueObject setTempObject = (AnalogValueObject)localDevice.getObjectByID(instanceID);
                    //listOfObjectPropertyReferences.add(new DeviceObjectPropertyReference(occupiedStatus.getId(), PropertyIdentifier.presentValue, null, null));
                    listOfObjectPropertyReferences.add(new DeviceObjectPropertyReference(setTempObject.getId(), PropertyIdentifier.presentValue, null, null));

                }*/
                //BinaryValueObject occupiedStatus = new BinaryValueObject(localDevice,1001,"Occupancy", BinaryPV.active,false);

                listOfObjectPropertyReferences.add(new DeviceObjectPropertyReference(occupancyObject.getId(), PropertyIdentifier.presentValue, null, null));
                Log.i("Bacnet","BV is added to Building Schedule:"+occupancyObject.getId());
                final DateRange effectivePeriod = new DateRange(Date.UNSPECIFIED, Date.UNSPECIFIED);
                if(!localDevice.checkObjectByID(BACnetUtils.systemSchedule)) {
                    Log.i("Bacnet","Creating Building Schedule:"+BACnetUtils.systemSchedule+" LOPS:"+listOfObjectPropertyReferences);
                    buildingSchedule = new ScheduleObject(localDevice, BACnetUtils.systemSchedule, "Building Schedule Object", effectivePeriod, weeklySchedule, exceptionSchedule, BinaryPV.inactive, listOfObjectPropertyReferences, 9, false);
                    buildingSchedule.writePropertyInternal(PropertyIdentifier.description, new CharacterString("Building Schedule - 75F" ));
                    createTestSchedules(localDevice,effectivePeriod,calendarObject);
                }else{
                    Log.i("Bacnet","Updating Building Schedule:"+BACnetUtils.systemSchedule);
                    ScheduleObject oldBuildingSchedule = (ScheduleObject) localDevice.getObjectByID(BACnetUtils.systemSchedule);
                    //oldBuildingSchedule.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.effectivePeriod, effectivePeriod));
                    oldBuildingSchedule.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.weeklySchedule, weeklySchedule));
                    oldBuildingSchedule.updatePresentValue();
                    //oldBuildingSchedule.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.exceptionSchedule, exceptionSchedule));
                }
            } catch (BACnetServiceException e) {
                e.printStackTrace();
            }
        }
        if (schedule != null && schedule.isZoneSchedule()) {
            ArrayList<Schedule.Days> weekDays = schedule.getDays();
            SequenceOf<TimeValue> mondaySchedule = new SequenceOf<>();
            SequenceOf<TimeValue> tuesdaySchedule = new SequenceOf<>();
            SequenceOf<TimeValue> wednesdaySchedule = new SequenceOf<>();
            SequenceOf<TimeValue> thursdaySchedule = new SequenceOf<>();
            SequenceOf<TimeValue> fridaySchedule = new SequenceOf<>();
            SequenceOf<TimeValue> saturdaySchedule = new SequenceOf<>();
            SequenceOf<TimeValue> sundaySchedule = new SequenceOf<>();
            float desiredTemp = (float) getSetTemp(zoneDevice);
            for (int i = 0; i < weekDays.size(); i++) {
                if (weekDays.get(i).getDay() == 0) {
                    Schedule.Days monDay = weekDays.get(i);
                    mondaySchedule.add(new TimeValue(new Time(monDay.getSthh(), monDay.getStmm(), 0, 0), new Real(desiredTemp)));
                }
                if (weekDays.get(i).getDay() == 1) {
                    Schedule.Days tuesDay = weekDays.get(i);
                    tuesdaySchedule.add(new TimeValue(new Time(tuesDay.getSthh(), tuesDay.getStmm(), 0, 0), new Real(desiredTemp)));
                }
                if (weekDays.get(i).getDay() == 2) {
                    Schedule.Days wednesDay = weekDays.get(i);
                    wednesdaySchedule.add(new TimeValue(new Time(wednesDay.getSthh(), wednesDay.getStmm(), 0, 0), new Real(desiredTemp)));
                }
                if (weekDays.get(i).getDay() == 3) {
                    Schedule.Days thursDay = weekDays.get(i);
                    thursdaySchedule.add(new TimeValue(new Time(thursDay.getSthh(), thursDay.getStmm(), 0, 0), new Real(desiredTemp)));
                }
                if (weekDays.get(i).getDay() == 4) {
                    Schedule.Days friDay = weekDays.get(i);
                    fridaySchedule.add(new TimeValue(new Time(friDay.getSthh(), friDay.getStmm(), 0, 0), new Real(desiredTemp)));
                }
                if (weekDays.get(i).getDay() == 5) {
                    Schedule.Days satDay = weekDays.get(i);
                    saturdaySchedule.add(new TimeValue(new Time(satDay.getSthh(), satDay.getStmm(), 0, 0), new Real(desiredTemp)));
                }
                if (weekDays.get(i).getDay() == 6) {
                    Schedule.Days sunDay = weekDays.get(i);
                    sundaySchedule.add(new TimeValue(new Time(sunDay.getSthh(), sunDay.getStmm(), 0, 0), new Real(desiredTemp)));
                }
            }

            BACnetArray<DailySchedule> weeklySchedule = new BACnetArray<>(
                    new DailySchedule(mondaySchedule),
                    new DailySchedule(tuesdaySchedule),
                    new DailySchedule(wednesdaySchedule),
                    new DailySchedule(thursdaySchedule),
                    new DailySchedule(fridaySchedule),
                    new DailySchedule(saturdaySchedule),
                    new DailySchedule(sundaySchedule)
            );

            //Todo Zone Schedule
            /*if (schedule.isZoneSchedule() && schedule.getRoomRef() != null) {
                vacations = CCUHsApi.getInstance().getZoneSchedule(schedule.getRoomRef(), true);
            }
            try {
                final SequenceOf<CalendarEntry> dateList = new SequenceOf<>();
                if(vacations != null) {
                    for (int i = 0; i < vacations.size(); i++) {
                        DateTime vacStartDate = vacations.get(i).getStartDate();
                        dateList.add(new CalendarEntry(new Date(vacStartDate.getYear(), Month.valueOf(vacStartDate.getMonthOfYear()), vacStartDate.getDayOfMonth(), DayOfWeek.UNSPECIFIED)));
                    }
                }

                CalendarObject calendarObject = null;
                if(!localDevice.checkObjectByID(BACnetUtils.zoneCalendar)){
                    calendarObject = new CalendarObject(localDevice, BACnetUtils.zoneCalendar, "Zone Schedule Calendar", dateList);
                }else{
                    calendarObject = (CalendarObject) localDevice.getObjectByID(BACnetUtils.zoneCalendar);
                }
                final SequenceOf<SpecialEvent> exceptionSchedule = new SequenceOf<>(new SpecialEvent(calendarObject.getId(),
                        new SequenceOf<>(new TimeValue(new Time(0, 0, 0, 0), new Real(74))), new UnsignedInteger(10)));

                //Todo Creating Link to Set Temperature BACnet object to BACnet Schedule
                final SequenceOf<DeviceObjectPropertyReference> listOfObjectPropertyReferences = new SequenceOf<>();
                int zoneAddress = Integer.parseInt(zoneDevice.getAddr());
                int instanceID = zoneAddress + BACnetUtils.desiredTempCooling;
                if (localDevice.checkObjectByID(instanceID)) {
                    AnalogValueObject setTempObject = (AnalogValueObject)localDevice.getObjectByID(instanceID);
                    listOfObjectPropertyReferences.add(new DeviceObjectPropertyReference(setTempObject.getId(), PropertyIdentifier.presentValue, null, null));
                }

                final DateRange effectivePeriod = new DateRange(Date.UNSPECIFIED, Date.UNSPECIFIED);
                if(!localDevice.checkObjectByID(BACnetUtils.zoneSchedule)){
                    Log.i("Bacnet","Creating Zone Schedule:"+BACnetUtils.zoneSchedule);
                zoneSchedule = new ScheduleObject(localDevice, BACnetUtils.zoneSchedule, zone.getDisplayName()+"-Zone Schedule Object", effectivePeriod, weeklySchedule, exceptionSchedule, new Real(72f), listOfObjectPropertyReferences, 9, false);
                }else{
                    Log.i("Bacnet","Updating Zone Schedule:"+BACnetUtils.zoneSchedule);
                    ScheduleObject oldBuildingSchedule = (ScheduleObject) localDevice.getObjectByID(BACnetUtils.zoneSchedule);
                    oldBuildingSchedule.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.effectivePeriod, effectivePeriod));
                    oldBuildingSchedule.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.weeklySchedule, weeklySchedule));
                    oldBuildingSchedule.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.exceptionSchedule, exceptionSchedule));
                }
            } catch (BACnetServiceException e) {
                e.printStackTrace();
            }*/
        }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public double getSetTemp(Device equip){
        double setTemp = 0;
        setTemp = CCUHsApi.getInstance().readHisValByQuery("zone and point and desired and air and temp and average and equipRef == \""+equip.getEquipRef()+"\"");
        return setTemp;
    }

    public static void updateSchedule(BACnetObject scheduleObject,boolean isSameallDay) {
        try {
            systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
            Log.i("BACnetUtil","75F Schedule-Before Change:"+systemSchedule.toString());
            if (scheduleObject.readProperty(PropertyIdentifier.objectType) == ObjectType.schedule ){
                SequenceOf<DailySchedule> dailySchedules = scheduleObject.get(PropertyIdentifier.weeklySchedule);
                Primitive activeStatus = new Enumerated(1);
                Primitive inactiveStatus = new Enumerated(0);
                for (Schedule.Days sDays : systemSchedule.getDays()) {
                    if(sDays.getDay() == 0){
                        SequenceOf<TimeValue> mondaySchedule = dailySchedules.get(0).getDaySchedule();
                        for(TimeValue daySchedule: mondaySchedule)
                        {
                            sDays.setDay(0);
                            if(daySchedule.getValue().equals(activeStatus)) {
                                String startTime = daySchedule.getTime().toString();
                                String startHrs = startTime.split(":")[0];
                                String startMins = startTime.split(":")[1];
                                sDays.setSthh(Integer.parseInt(startHrs));
                                sDays.setStmm(Integer.parseInt(startMins));
                            }if(daySchedule.getValue().equals(inactiveStatus)) {
                                String endTime = daySchedule.getTime().toString();
                                String endHrs = endTime.split(":")[0];
                                String endMins = endTime.split(":")[1];
                                sDays.setEthh(Integer.parseInt(endHrs));
                                sDays.setEtmm(Integer.parseInt(endMins));
                            }
                        }
                    }if(sDays.getDay() == 1){
                        SequenceOf<TimeValue> tuesdaySchedule = dailySchedules.get(1).getDaySchedule();
                        for(TimeValue daySchedule: tuesdaySchedule)
                        {
                            sDays.setDay(1);
                            if(daySchedule.getValue().equals(activeStatus)) {
                                String startTime = daySchedule.getTime().toString();
                                String startHrs = startTime.split(":")[0];
                                String startMins = startTime.split(":")[1];
                                sDays.setSthh(Integer.parseInt(startHrs));
                                sDays.setStmm(Integer.parseInt(startMins));
                            }if(daySchedule.getValue().equals(inactiveStatus)) {
                                String endTime = daySchedule.getTime().toString();
                                String endHrs = endTime.split(":")[0];
                                String endMins = endTime.split(":")[1];
                                sDays.setEthh(Integer.parseInt(endHrs));
                                sDays.setEtmm(Integer.parseInt(endMins));
                            }
                        }
                    }if(sDays.getDay() == 2){
                        SequenceOf<TimeValue> wednesdaySchedule = dailySchedules.get(2).getDaySchedule();
                        for(TimeValue daySchedule: wednesdaySchedule)
                        {
                            sDays.setDay(2);
                            if(daySchedule.getValue().equals(activeStatus)) {
                                String startTime = daySchedule.getTime().toString();
                                String startHrs = startTime.split(":")[0];
                                String startMins = startTime.split(":")[1];
                                Log.i("BACnetUtil","75F Schedule="+"Time:"+startTime+" StHR:"+startHrs+" StMn:"+startMins);
                                sDays.setSthh(Integer.parseInt(startHrs));
                                sDays.setStmm(Integer.parseInt(startMins));
                            }if(daySchedule.getValue().equals(inactiveStatus)) {
                                String endTime = daySchedule.getTime().toString();
                                String endHrs = endTime.split(":")[0];
                                String endMins = endTime.split(":")[1];
                                Log.i("BACnetUtil","75F Schedule="+"Time:"+endTime+" StHR:"+endHrs+" StMn:"+endMins);
                                sDays.setEthh(Integer.parseInt(endHrs));
                                sDays.setEtmm(Integer.parseInt(endMins));
                            }
                        }
                    }if(sDays.getDay() == 3){
                        SequenceOf<TimeValue> thursdaySchedule = dailySchedules.get(3).getDaySchedule();
                        for(TimeValue daySchedule: thursdaySchedule)
                        {
                            sDays.setDay(3);
                            if(daySchedule.getValue().equals(activeStatus)) {
                                String startTime = daySchedule.getTime().toString();
                                String startHrs = startTime.split(":")[0];
                                String startMins = startTime.split(":")[1];
                                Log.i("BACnetUtil","75F Schedule="+"Time:"+startTime+" StHR:"+startHrs+" StMn:"+startMins);
                                sDays.setSthh(Integer.parseInt(startHrs));
                                sDays.setStmm(Integer.parseInt(startMins));
                            }if(daySchedule.getValue().equals(inactiveStatus)) {
                                String endTime = daySchedule.getTime().toString();
                                String endHrs = endTime.split(":")[0];
                                String endMins = endTime.split(":")[1];
                                Log.i("BACnetUtil","75F Schedule="+"Time:"+endTime+" StHR:"+endHrs+" StMn:"+endMins);
                                sDays.setEthh(Integer.parseInt(endHrs));
                                sDays.setEtmm(Integer.parseInt(endMins));
                            }
                        }
                    }if(sDays.getDay() == 4){
                        SequenceOf<TimeValue> fridaySchedule = dailySchedules.get(4).getDaySchedule();
                        for(TimeValue daySchedule: fridaySchedule)
                        {
                            sDays.setDay(4);
                            if(daySchedule.getValue().equals(activeStatus)) {
                                String startTime = daySchedule.getTime().toString();
                                String startHrs = startTime.split(":")[0];
                                String startMins = startTime.split(":")[1];
                                Log.i("BACnetUtil","75F Schedule="+"Time:"+startTime+" StHR:"+startHrs+" StMn:"+startMins);
                                sDays.setSthh(Integer.parseInt(startHrs));
                                sDays.setStmm(Integer.parseInt(startMins));
                            }if(daySchedule.getValue().equals(inactiveStatus)) {
                                String endTime = daySchedule.getTime().toString();
                                String endHrs = endTime.split(":")[0];
                                String endMins = endTime.split(":")[1];
                                Log.i("BACnetUtil","75F Schedule="+"Time:"+endTime+" StHR:"+endHrs+" StMn:"+endMins);
                                sDays.setEthh(Integer.parseInt(endHrs));
                                sDays.setEtmm(Integer.parseInt(endMins));
                            }
                        }
                    }if(sDays.getDay() == 5){
                        SequenceOf<TimeValue> saturdaySchedule = dailySchedules.get(5).getDaySchedule();
                        for(TimeValue daySchedule: saturdaySchedule)
                        {
                            sDays.setDay(5);
                            if(daySchedule.getValue().equals(activeStatus)) {
                                String startTime = daySchedule.getTime().toString();
                                String startHrs = startTime.split(":")[0];
                                String startMins = startTime.split(":")[1];
                                Log.i("BACnetUtil","75F Schedule="+"Time:"+startTime+" StHR:"+startHrs+" StMn:"+startMins);
                                sDays.setSthh(Integer.parseInt(startHrs));
                                sDays.setStmm(Integer.parseInt(startMins));
                            }if(daySchedule.getValue().equals(inactiveStatus)) {
                                String endTime = daySchedule.getTime().toString();
                                String endHrs = endTime.split(":")[0];
                                String endMins = endTime.split(":")[1];
                                Log.i("BACnetUtil","75F Schedule="+"Time:"+endTime+" StHR:"+endHrs+" StMn:"+endMins);
                                sDays.setEthh(Integer.parseInt(endHrs));
                                sDays.setEtmm(Integer.parseInt(endMins));
                            }
                        }
                    }if(sDays.getDay() == 6){
                        SequenceOf<TimeValue> saturdaySchedule = dailySchedules.get(6).getDaySchedule();
                        for(TimeValue daySchedule: saturdaySchedule)
                        {
                            sDays.setDay(6);
                            if(daySchedule.getValue().equals(activeStatus)) {
                                String startTime = daySchedule.getTime().toString();
                                String startHrs = startTime.split(":")[0];
                                String startMins = startTime.split(":")[1];
                                Log.i("BACnetUtil","75F Schedule="+"Time:"+startTime+" StHR:"+startHrs+" StMn:"+startMins);
                                sDays.setSthh(Integer.parseInt(startHrs));
                                sDays.setStmm(Integer.parseInt(startMins));
                            }if(daySchedule.getValue().equals(inactiveStatus)) {
                                String endTime = daySchedule.getTime().toString();
                                String endHrs = endTime.split(":")[0];
                                String endMins = endTime.split(":")[1];
                                Log.i("BACnetUtil","75F Schedule="+"Time:"+endTime+" StHR:"+endHrs+" StMn:"+endMins);
                                sDays.setEthh(Integer.parseInt(endHrs));
                                sDays.setEtmm(Integer.parseInt(endMins));
                            }
                        }
                    }

                }
                Log.i("BACnetUtil","75F Schedule-After Change:"+systemSchedule.toString());

            }if (scheduleObject.readProperty(PropertyIdentifier.objectType) == ObjectType.analogValue) {
                 if(scheduleObject.getId().getInstanceNumber() == BACnetUtils.occupanyHeatingDT){
                     if(isSameallDay){
                         for (Schedule.Days sDays: systemSchedule.getDays()){
                             Log.i("BACnetUtil","Schedule="+sDays.getHeatingVal());
                             sDays.setHeatingVal(Double.parseDouble(scheduleObject.get(PropertyIdentifier.presentValue).toString()));
                             Log.i("BACnetUtil","Schedule="+sDays.getHeatingVal());
                         }
                     }else{
                         getCurrentDaySchedule(systemSchedule.getDays()).setHeatingVal(Double.parseDouble(scheduleObject.get(PropertyIdentifier.presentValue).toString()));
                     }
                 }if(scheduleObject.getId().getInstanceNumber() == BACnetUtils.occupanyCoolingDT){
                    if(isSameallDay) {
                        for (Schedule.Days sDays : systemSchedule.getDays()) {
                            sDays.setCoolingVal(Double.parseDouble(scheduleObject.get(PropertyIdentifier.presentValue).toString()));
                        }
                    }else {
                        getCurrentDaySchedule(systemSchedule.getDays()).setCoolingVal(Double.parseDouble(scheduleObject.get(PropertyIdentifier.presentValue).toString()));
                    }
                 }
            }
            doScheduleUpdate();
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }

    }

    private static void doScheduleUpdate() {
        {
            CCUHsApi.getInstance().updateSchedule(systemSchedule);
        }
        CCUHsApi.getInstance().syncEntityTree();
        ScheduleProcessJob.updateSchedules();
    }

    private static Schedule.Days getCurrentDaySchedule(ArrayList<Schedule.Days> weekDays){
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        int day = calendar.get(Calendar.DAY_OF_WEEK)-2;
        if(day==-1){
            day = 6;
        }
        return weekDays.get(day);
    }


    public double getSetbackTemp(){
        try {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            HashMap site = hayStack.read("site");
            //String siteGUID = CCUHsApi.getInstance().getGUID(site.get("id").toString());
            HashMap configPoint = hayStack.read("unoccupied and setback and siteRef == \"" +site.get("id").toString() + "\"");
            //Log.i("Bacnet","setback map:"+configPoint.toString()+" default value:"+hayStack.readDefaultValById(configPoint.get("id").toString())+" hvalue:"+hayStack.readHisValById(configPoint.get("id").toString()));
            return hayStack.readHisValById(configPoint.get("id").toString());
        } catch (Exception e) {
            return 0;
        }
    }


    public static void updateVacations(BACnetObject calendarObject) {
            vacations = CCUHsApi.getInstance().getSystemSchedule( true);
            SequenceOf<CalendarEntry> calendarEntries = calendarObject.get(PropertyIdentifier.dateList);
            SequenceOf<CalendarEntry> calendar75Entries = new SequenceOf<>();
            ArrayList<Vacations> vacations75f = new ArrayList<>();
            ArrayList<Vacations> vacationsBacnet = new ArrayList<>();
            ArrayList<Vacations> vacationsCommon = new ArrayList<>();
            ArrayList<Vacations> vacationsNew = new ArrayList<>();
            if(calendarEntries.getCount() > 0){
                if(compareVacations(calendarEntries,vacations))
                {
                    for(int i = 0;i<vacations.size();i++){
                        Vacations vacItem = new Vacations(null,convertDateTimetoDate(vacations.get(i).getStartDate()),convertDateTimetoDate(vacations.get(i).getEndDate()));
                        Vacations vacItemCommon = new Vacations(vacations.get(i).getId(),convertDateTimetoDate(vacations.get(i).getStartDate()),convertDateTimetoDate(vacations.get(i).getEndDate()));
                        vacations75f.add(vacItem);
                        vacationsCommon.add(vacItemCommon);
                    }
                    for(int i = 0;i<calendarEntries.size();i++){
                        Vacations vacItem = null;
                        if(calendarEntries.get(i).isDate()) {
                            vacItem = new Vacations(null, convertCalendarEntrytoDate(calendarEntries.get(i)),convertCalendarEntrytoDate(calendarEntries.get(i)));
                        }else {
                            vacItem = new Vacations(null, convertBacnetDate(calendarEntries.get(i).getDateRange().getStartDate()), convertBacnetDate(calendarEntries.get(i).getDateRange().getEndDate()));
                        }
                        vacationsBacnet.add(vacItem);
                    }
                    Collections.sort(vacations75f, new Comparator<Vacations>() {
                        @Override
                        public int compare(Vacations lhs, Vacations rhs) {
                            return (lhs.getStartDate()).compareTo(rhs.getStartDate());
                        }
                    });
                    Collections.sort(vacationsBacnet, new Comparator<Vacations>() {
                        @Override
                        public int compare(Vacations lhs, Vacations rhs) {
                            return (lhs.getStartDate()).compareTo(rhs.getStartDate());
                        }
                    });

                    Log.i("BACnetUtil","75f Data:"+vacations75f.toString());
                    Log.i("BACnetUtil","BACnet Data:"+vacationsBacnet.toString());
                    ArrayList<Vacations> results = new ArrayList<>();
                    for (Vacations vacation2 : vacationsCommon) {
                        if(!vacationsBacnet.contains(vacation2)){
                            results.add(vacation2);
                        }
                    }
                    Log.i("BACnetUtil","data:"+results.toString());
                    for(Vacations vacationChanged : results){
                        for(Schedule scheduleItem : vacations){
                            if(scheduleItem.getId().equals(vacationChanged.getVacationID()))
                            {
                                vacations.remove(scheduleItem);
                                CCUHsApi.getInstance().deleteEntity("@"+scheduleItem.getId());
                                ScheduleProcessJob.updateSchedules();
                                Log.i("BACnetUtil","vacations removed:"+scheduleItem.toString());
                            }
                        }
                    }
                    vacationsCommon.clear();
                    Log.i("BACnetUtil","vacationsCommon cleared:"+vacationsCommon.toString());
                    vacations = CCUHsApi.getInstance().getSystemSchedule( true);
                    Log.i("BACnetUtil","vacations modified:"+vacations.toString());
                    for(int i = 0;i<vacations.size();i++){
                        Vacations vacItemCommon = new Vacations(vacations.get(i).getId(),convertDateTimetoDate(vacations.get(i).getStartDate()),convertDateTimetoDate(vacations.get(i).getEndDate()));
                        vacationsNew.add(vacItemCommon);
                    }
                    for (Vacations vacation2 : vacationsBacnet) {
                        if(!vacationsNew.contains(vacation2)){
                            Log.i("BACnetUtil","vacation added :"+vacation2.toString());
                            DefaultSchedules.upsertVacation(null, "BACnetVacation", new DateTime(vacation2.startDate), new DateTime(vacation2.endDate));
                        }
                    }
                    CCUHsApi.getInstance().syncEntityTree();
                    ScheduleProcessJob.updateSchedules();
                    vacations = CCUHsApi.getInstance().getSystemSchedule( true);
                    Log.i("BACnetUtil","vacations updated final:"+vacations.toString());
                    Log.i("BACnetUtil","==============================================");
                }
            }
    }

    public static void addNewVacations(BACnetObject scheduleObject, PropertyValue pv) {
        vacations = CCUHsApi.getInstance().getSystemSchedule(true);
        SequenceOf<SpecialEvent> exceptionSchedules = pv.getValue();
        SequenceOf<CalendarEntry> calendarEntries = new SequenceOf<>();
        ArrayList<Vacations> vacations75f = new ArrayList<>();
        ArrayList<Vacations> vacationsBacnet = new ArrayList<>();
        if(exceptionSchedules.getCount() > 0){
            for (SpecialEvent specialEvent: exceptionSchedules){
                calendarEntries.add(specialEvent.getCalendarEntry());
            }
            for(Schedule vac75f:vacations) {
                CalendarEntry calEntry = null;
                if (vac75f.getStartDate() == vac75f.getEndDate()) {
                    Date vacDate = new Date(vac75f.getStartDate().getYear(), Month.valueOf(vac75f.getStartDate().getMonthOfYear()), vac75f.getStartDate().getDayOfMonth(), DayOfWeek.UNSPECIFIED);
                    calEntry = new CalendarEntry(vacDate);
                } else if (vac75f.getStartDate() != vac75f.getEndDate() && vac75f.getStartDate().isBefore(vac75f.getEndDate())) {
                    Date vacSdate = new Date(vac75f.getStartDate().getYear(), Month.valueOf(vac75f.getStartDate().getMonthOfYear()), vac75f.getStartDate().getDayOfMonth(), DayOfWeek.UNSPECIFIED);
                    Date vacEdate = new Date(vac75f.getEndDate().getYear(), Month.valueOf(vac75f.getEndDate().getMonthOfYear()), vac75f.getEndDate().getDayOfMonth(), DayOfWeek.UNSPECIFIED);
                    DateRange vacDateRange = new DateRange(vacSdate, vacEdate);
                    calEntry = new CalendarEntry(vacDateRange);
                }
                if (!calendarEntries.contains(calEntry)) {
                    for(CalendarEntry newVacation : calendarEntries) {
                        if(newVacation.isDate()) {
                            DefaultSchedules.upsertVacation(null, "BACnetVacation-E", new DateTime(convertBacnetDate(newVacation.getDate())), new DateTime(convertBacnetDate(newVacation.getDate())));
                        }if(newVacation.isDateRange()) {
                            DefaultSchedules.upsertVacation(null, "BACnetVacation-E", new DateTime(convertBacnetDate(newVacation.getDateRange().getStartDate())), new DateTime(convertBacnetDate(newVacation.getDateRange().getEndDate())));
                        }
                        Log.i("BACnetUtil","vacations added from Exception:"+newVacation.toString());
                    }
                }
            }
            CCUHsApi.getInstance().syncEntityTree();
            ScheduleProcessJob.updateSchedules();
        }
        /*
        for(int i = 0;i<vacations.size();i++){
            Vacations vacation75f = new Vacations(vacations.get(i).getId(),convertDateTimetoDate(vacations.get(i).getStartDate()),convertDateTimetoDate(vacations.get(i).getEndDate()));
            vacations75f.add(vacation75f);
        }
        for(int i = 0;i<calendarEntries.size();i++){
            Vacations vacItem = null;
            if(calendarEntries.get(i).isDate()) {
                vacItem = new Vacations(null, convertCalendarEntrytoDate(calendarEntries.get(i)),convertCalendarEntrytoDate(calendarEntries.get(i)));
            }else {
                vacItem = new Vacations(null, convertBacnetDate(calendarEntries.get(i).getDateRange().getStartDate()), convertBacnetDate(calendarEntries.get(i).getDateRange().getEndDate()));
            }
            vacationsBacnet.add(vacItem);
        }
        Collections.sort(vacations75f, new Comparator<Vacations>() {
            @Override
            public int compare(Vacations lhs, Vacations rhs) {
                return (lhs.getStartDate()).compareTo(rhs.getStartDate());
            }
        });
        Collections.sort(vacationsBacnet, new Comparator<Vacations>() {
            @Override
            public int compare(Vacations lhs, Vacations rhs) {
                return (lhs.getStartDate()).compareTo(rhs.getStartDate());
            }
        });*/
    }

    public static boolean compareVacations(SequenceOf<CalendarEntry> calendarEntries, ArrayList<Schedule> vacationSchedule){
        boolean changeDone = false;
        for (int i = 0; i < vacationSchedule.size(); i++) {
            DateTime vacStartDate = vacationSchedule.get(i).getStartDate();
            DateTime vacEndDate = vacationSchedule.get(i).getEndDate();
            long daysDiff = (vacEndDate.getMillis()-vacStartDate.getMillis())/(1000*60*60*24);
            //Log.i("Bacnet","Vacations-StartDate:"+vacStartDate+" EndDate:"+vacEndDate+" Diffs:"+daysDiff);
            if(daysDiff == 0){//Todo Single Calendar Entry
                CalendarEntry newCalendarEntry = new CalendarEntry(new Date(vacStartDate.getYear(), Month.valueOf(vacStartDate.getMonthOfYear()), vacStartDate.getDayOfMonth(), DayOfWeek.UNSPECIFIED));
                if(!calendarEntries.contains(newCalendarEntry)) {
                    changeDone = true;
                }
            }
            else {//Todo Multiple Date Calendar Entry
                Date startDate = new Date(vacStartDate.getYear(), Month.valueOf(vacStartDate.getMonthOfYear()), vacStartDate.getDayOfMonth(), DayOfWeek.UNSPECIFIED);
                Date endDate = new Date(vacEndDate.getYear(), Month.valueOf(vacEndDate.getMonthOfYear()), vacEndDate.getDayOfMonth(), DayOfWeek.UNSPECIFIED);
                CalendarEntry newCalendarEntry = new CalendarEntry(new DateRange(startDate,endDate));
                if(!calendarEntries.contains(newCalendarEntry)){
                    changeDone = true;
                }
            }
        }
        if(calendarEntries.getCount() != vacationSchedule.size()){
            changeDone = true;
        }
        return changeDone;
    }


    public static SequenceOf<CalendarEntry> compareVacations(SequenceOf<CalendarEntry> calendarEntries, SequenceOf<CalendarEntry> calendarEntries75f){
        SequenceOf<CalendarEntry> changedEntries = new SequenceOf<>();
            for(int i =0;i<calendarEntries.size();i++){
                if(!calendarEntries75f.contains(calendarEntries.get(i))){
                 changedEntries.add(calendarEntries.get(i));
                }
            }
        Log.i("BACnetUtil", "Vacation Bacnet Entries:" + changedEntries.toString());
        return changedEntries;
    }

    public static SequenceOf<CalendarEntry> compare75fVacations(SequenceOf<CalendarEntry> calendarEntries, SequenceOf<CalendarEntry> calendarEntries75f){
        SequenceOf<CalendarEntry> changed75fEntries = new SequenceOf<>();
            for(int i =0;i<calendarEntries75f.size();i++){
                if(!calendarEntries.contains(calendarEntries75f.get(i))){
                    changed75fEntries.add(calendarEntries75f.get(i));
                }
            }
        Log.i("BACnetUtil", "Vacation 75f Entries:" + changed75fEntries.toString());
        return changed75fEntries;
    }

    public static Schedule compareEachVacation(CalendarEntry calendarEntry, ArrayList<Schedule> vacationSchedule){
        Schedule changedSchedule = null;
            if(calendarEntry.isDate()) {
                java.util.Date vacationDate = convertCalendarEntrytoDate(calendarEntry);
                for(int i=0;i<vacationSchedule.size();i++){
                    java.util.Date newDate = convertDateTimetoDate(vacationSchedule.get(i).getStartDate());
                    Log.i("Bacnet", "Vacation Compare Date:" + newDate.toString()+" Bacnet:"+ vacationDate.toString());
                    if(newDate.equals(vacationDate)){
                        continue;
                    }else{
                        changedSchedule = vacationSchedule.get(i);
                    }
                }
            }else if(calendarEntry.isDateRange()){
                java.util.Date vacationStartDate = convertBacnetDate(calendarEntry.getDateRange().getStartDate());
                java.util.Date vacationEndDate = convertBacnetDate(calendarEntry.getDateRange().getEndDate());
                for(int i=0;i<vacationSchedule.size();i++){
                    java.util.Date startDateTime = convertDateTimetoDate(vacationSchedule.get(i).getStartDate());
                    java.util.Date endDateTime = convertDateTimetoDate(vacationSchedule.get(i).getEndDate());
                    if(startDateTime.equals(vacationStartDate) && endDateTime.equals(vacationEndDate)){
                        Log.i("BACnetUtil", "Vacation Compare Start Date:" + startDateTime.toString()+" Bacnet:"+ vacationStartDate.toString());
                        Log.i("BACnetUtil", "Vacation Compare End Date:" + endDateTime.toString()+" Bacnet:"+ vacationEndDate.toString());
                        continue;
                    }else{
                        changedSchedule = vacationSchedule.get(i);
                    }
                }
            }
        return changedSchedule;
    }

    public static java.util.Date convertDateTimetoDate(DateTime dateTime)
    {
        java.util.Date convertedDate = null;
        String strDate = dateTime.getDayOfMonth()+"/"+dateTime.getMonthOfYear()+"/"+(dateTime.getYear());
        try {
            convertedDate = new SimpleDateFormat("dd/MM/yyyy").parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return convertedDate;
    }

    public static java.util.Date convertCalendarEntrytoDate(CalendarEntry calendarEntry)
    {
        java.util.Date convertedDate = null;
        String strDate = calendarEntry.getDate().getDay()+"/"+Month.getIDof(calendarEntry.getDate().getMonth())+"/"+(1900+calendarEntry.getDate().getYear());
        try {
            convertedDate = new SimpleDateFormat("dd/MM/yyyy").parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return convertedDate;
    }

    public static java.util.Date convertBacnetDate(Date bacnetDate)
    {
        java.util.Date convertedDate = null;
        String strDate=bacnetDate.getDay()+"/"+Month.getIDof(bacnetDate.getMonth())+"/"+(1900+bacnetDate.getYear());
        try {
            convertedDate = new SimpleDateFormat("dd/MM/yyyy").parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return convertedDate;
    }

    public void createTestSchedules(LocalDevice localDevice, DateRange effectivePeriod, CalendarObject  calendarObject){
        try {
            // Analog Value based Schedule
            AnalogValueObject testAV1 = new AnalogValueObject(localDevice,1116, "AV-Schedule Test", (float) BACnetUtils.defaultTemp, EngineeringUnits.degreesFahrenheit, false);
            testAV1.supportCommandable(70f);
            BACnetArray<DailySchedule> weeklySchedule = new BACnetArray<>(
                    new DailySchedule(new SequenceOf<TimeValue>()),
                    new DailySchedule(new SequenceOf<TimeValue>()),
                    new DailySchedule(new SequenceOf<TimeValue>()),
                    new DailySchedule(new SequenceOf<TimeValue>()),
                    new DailySchedule(new SequenceOf<TimeValue>()),
                    new DailySchedule(new SequenceOf<TimeValue>()),
                    new DailySchedule(new SequenceOf<TimeValue>())
            );
            final SequenceOf<DeviceObjectPropertyReference> lopAv = new SequenceOf<>();
            lopAv.add(new DeviceObjectPropertyReference(testAV1.getId(), PropertyIdentifier.presentValue, null, null));

            final SequenceOf<SpecialEvent> exceptionSchedule = new SequenceOf<>(new SpecialEvent(calendarObject.getId(),
                    new SequenceOf<>(new TimeValue(new Time(0, 0, 0, 0), new Real(72.0f))), new UnsignedInteger(10)));
            ScheduleObject avSchedule = new ScheduleObject(localDevice, 116, "AV-Schedule Object", effectivePeriod, weeklySchedule, exceptionSchedule, new Real(72.0f), lopAv, 9, false);
            avSchedule.writePropertyInternal(PropertyIdentifier.description, new CharacterString("AV Schedule - 75F" ));

            // Multi State Value based Schedule
            MultistateValueObject testMsV1 = new MultistateValueObject(localDevice,1117, "MSV-Schedule Test", 3,null,1, false);
            testMsV1.writeProperty(null, new PropertyValue(PropertyIdentifier.stateText, new BACnetArray<>(new CharacterString("State 1"), new CharacterString("State 2"), new CharacterString("State 3"))));
            testMsV1.writePropertyInternal(PropertyIdentifier.numberOfStates, new UnsignedInteger(3));
            testMsV1.supportCommandable(new UnsignedInteger(1));
            testMsV1.supportCovReporting();
            BACnetArray<CharacterString> statesMSV = testMsV1.get(PropertyIdentifier.stateText);
            final SequenceOf<DeviceObjectPropertyReference> lopMsv = new SequenceOf<>();
            lopMsv.add(new DeviceObjectPropertyReference(testMsV1.getId(), PropertyIdentifier.presentValue, null, null));
            //lopMsv.add(new DeviceObjectPropertyReference(localDevice.getObjectByID(109901).getId(), PropertyIdentifier.stateText, new UnsignedInteger(1), null));


            final SequenceOf<SpecialEvent> excScheduleMSV = new SequenceOf<>(new SpecialEvent(calendarObject.getId(),
                    new SequenceOf<>(new TimeValue(new Time(0, 0, 0, 0), new UnsignedInteger(1))), new UnsignedInteger(10)));
            ScheduleObject msvSchedule = new ScheduleObject(localDevice, 117, "MSV-Schedule Object", effectivePeriod, weeklySchedule, excScheduleMSV,new UnsignedInteger(1), lopMsv, 9, false);
            msvSchedule.writePropertyInternal(PropertyIdentifier.description, new CharacterString("MSV Schedule - 75F" ));
            } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public double getMaxBuildingLimits(){
        return TunerUtil.readBuildingTunerValByQuery("building and limit and max");
    }
    public double getMinBuildingLimits(){
        return TunerUtil.readBuildingTunerValByQuery("building and limit and min");
    }

    private static float getMaxUserTempLimits(String tag){
        float maxVal = (float) TunerUtil.readBuildingTunerValByQuery("user and limit and max and "+tag);
        return maxVal;
    }

    private static float getMinUserTempLimits(String tag){
        float minVal =  (float) TunerUtil.readBuildingTunerValByQuery("user and limit and min and "+tag);
        return minVal;
    }
}
