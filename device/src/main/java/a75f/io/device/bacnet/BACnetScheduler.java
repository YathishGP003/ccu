package a75f.io.device.bacnet;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.enums.DayOfWeek;
import com.renovo.bacnet4j.enums.Month;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.AnalogValueObject;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.obj.BinaryValueObject;
import com.renovo.bacnet4j.obj.CalendarObject;
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

public class BACnetScheduler {

    public static ScheduleObject buildingSchedule;
    public static ArrayList<Schedule> vacationsList = null;
    public static Schedule schedule = null;
    public static Schedule systemSchedule = null;
    public static Device zoneDevice = null;
    public BinaryValueObject occupancyObject = null;
    public AnalogValueObject occupancyCoolingTemp = null;
    public AnalogValueObject occupancyHeatingTemp = null;

    public BACnetScheduler(LocalDevice localDevice, Zone zone) {

        zoneDevice = HSUtil.getDevices(zone.getId()).get(0);

        schedule = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
        systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);

        //Todo Schedule Objects
        try {
            if (!localDevice.checkObjectByID(BACnetUtils.scheduleOccupancy)) {
                occupancyObject = new BinaryValueObject(localDevice, BACnetUtils.scheduleOccupancy, "Building Schedule Occupancy", BinaryPV.inactive, false);
                occupancyObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Occupancy Status")));
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

            if(currentTime.after(scheduleStartTime) && currentTime.before(scheduleEndTime)) {
                if(occupancyCoolingTemp.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)){
                    occupancyCoolingTemp.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, new Real(currentDay.getCoolingVal().floatValue())));
                    occupancyCoolingTemp.writePropertyInternal(PropertyIdentifier.relinquishDefault, new Real(currentDay.getCoolingVal().floatValue()+(float) getSetbackTemp()));
                }
                if(occupancyHeatingTemp.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
                    occupancyHeatingTemp.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, new Real(currentDay.getHeatingVal().floatValue())));
                    occupancyHeatingTemp.writePropertyInternal(PropertyIdentifier.relinquishDefault, new Real(currentDay.getHeatingVal().floatValue() - (float) getSetbackTemp()));
                }
            }else{
                if(occupancyCoolingTemp.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
                    occupancyCoolingTemp.writePropertyInternal(PropertyIdentifier.relinquishDefault, new Real(currentDay.getCoolingVal().floatValue()+(float) getSetbackTemp()));
                    occupancyCoolingTemp.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, new Null()));
                }
                if(occupancyHeatingTemp.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
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
                vacationsList = CCUHsApi.getInstance().getSystemSchedule( true);
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
                if(vacationsList != null) {
                    SequenceOf<CalendarEntry> vacationCalendarEntry  = calendarObject.get(PropertyIdentifier.dateList); // Todo Fetching Existing datelist
                    if(compareVacations(vacationCalendarEntry, vacationsList)) {
                        calendarObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.dateList, new SequenceOf<CalendarEntry>()));
                        SequenceOf<CalendarEntry> newVactionEntry  = calendarObject.get(PropertyIdentifier.dateList);
                        for (int i = 0; i < vacationsList.size(); i++) {
                            DateTime vacStartDate = vacationsList.get(i).getStartDate();
                            DateTime vacEndDate = vacationsList.get(i).getEndDate();
                            long daysDiff = (vacEndDate.getMillis() - vacStartDate.getMillis()) / (1000 * 60 * 60 * 24);
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


                // Creating Link to Set Temperature BACnet object to BACnet Schedule
                final SequenceOf<DeviceObjectPropertyReference> listOfObjectPropertyReferences = new SequenceOf<>();
                listOfObjectPropertyReferences.add(new DeviceObjectPropertyReference(occupancyObject.getId(), PropertyIdentifier.presentValue, null, null));
                final DateRange effectivePeriod = new DateRange(Date.UNSPECIFIED, Date.UNSPECIFIED);
                if(!localDevice.checkObjectByID(BACnetUtils.systemSchedule)) {
                    buildingSchedule = new ScheduleObject(localDevice, BACnetUtils.systemSchedule, "Building Schedule Object", effectivePeriod, weeklySchedule, exceptionSchedule, BinaryPV.inactive, listOfObjectPropertyReferences, 9, false);
                    buildingSchedule.writePropertyInternal(PropertyIdentifier.description, new CharacterString("Building Schedule - 75F" ));
                }else{
                    ScheduleObject oldBuildingSchedule = (ScheduleObject) localDevice.getObjectByID(BACnetUtils.systemSchedule);
                    oldBuildingSchedule.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.weeklySchedule, weeklySchedule));
                    oldBuildingSchedule.updatePresentValue();
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
                    }if(sDays.getDay() == 3){
                        SequenceOf<TimeValue> thursdaySchedule = dailySchedules.get(3).getDaySchedule();
                        for(TimeValue daySchedule: thursdaySchedule)
                        {
                            sDays.setDay(3);
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
                    }if(sDays.getDay() == 4){
                        SequenceOf<TimeValue> fridaySchedule = dailySchedules.get(4).getDaySchedule();
                        for(TimeValue daySchedule: fridaySchedule)
                        {
                            sDays.setDay(4);
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
                    }if(sDays.getDay() == 5){
                        SequenceOf<TimeValue> saturdaySchedule = dailySchedules.get(5).getDaySchedule();
                        for(TimeValue daySchedule: saturdaySchedule)
                        {
                            sDays.setDay(5);
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
                    }if(sDays.getDay() == 6){
                        SequenceOf<TimeValue> saturdaySchedule = dailySchedules.get(6).getDaySchedule();
                        for(TimeValue daySchedule: saturdaySchedule)
                        {
                            sDays.setDay(6);
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
                    }

                }
            }if (scheduleObject.readProperty(PropertyIdentifier.objectType) == ObjectType.analogValue) {
                 if(scheduleObject.getId().getInstanceNumber() == BACnetUtils.occupanyHeatingDT){
                     if(isSameallDay){
                         for (Schedule.Days scheduleDays: systemSchedule.getDays()){
                             scheduleDays.setHeatingVal(Double.parseDouble(scheduleObject.get(PropertyIdentifier.presentValue).toString()));
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
            HashMap configPoint = hayStack.read("unoccupied and setback and siteRef == \"" +site.get("id").toString() + "\"");
            return hayStack.readHisValById(configPoint.get("id").toString());
        } catch (Exception e) {
            return 0;
        }
    }


    public static void updateVacations(BACnetObject calendarObject) {
            vacationsList = CCUHsApi.getInstance().getSystemSchedule( true);
            SequenceOf<CalendarEntry> calendarEntries = calendarObject.get(PropertyIdentifier.dateList);
            SequenceOf<CalendarEntry> calendar75Entries = new SequenceOf<>();
            ArrayList<Vacations> vacations75f = new ArrayList<>();
            ArrayList<Vacations> vacationsBacnet = new ArrayList<>();
            ArrayList<Vacations> vacationsCommon = new ArrayList<>();
            ArrayList<Vacations> vacationsNew = new ArrayList<>();
            if(calendarEntries.getCount() > 0){
                if(compareVacations(calendarEntries, vacationsList))
                {
                    for(int i = 0; i< vacationsList.size(); i++){
                        Vacations vacItem = new Vacations(null,convertDateTimetoDate(vacationsList.get(i).getStartDate()),convertDateTimetoDate(vacationsList.get(i).getEndDate()));
                        Vacations vacItemCommon = new Vacations(vacationsList.get(i).getId(),convertDateTimetoDate(vacationsList.get(i).getStartDate()),convertDateTimetoDate(vacationsList.get(i).getEndDate()));
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
                    ArrayList<Vacations> results = new ArrayList<>();
                    for (Vacations vacation2 : vacationsCommon) {
                        if(!vacationsBacnet.contains(vacation2)){
                            results.add(vacation2);
                        }
                    }
                    for(Vacations vacationChanged : results){
                        for(Schedule scheduleItem : vacationsList){
                            if(scheduleItem.getId().equals(vacationChanged.getVacationID()))
                            {
                                vacationsList.remove(scheduleItem);
                                CCUHsApi.getInstance().deleteEntity("@"+scheduleItem.getId());
                                ScheduleProcessJob.updateSchedules();
                            }
                        }
                    }
                    vacationsCommon.clear();
                    vacationsList = CCUHsApi.getInstance().getSystemSchedule( true);
                    for(int i = 0; i< vacationsList.size(); i++){
                        Vacations vacItemCommon = new Vacations(vacationsList.get(i).getId(),convertDateTimetoDate(vacationsList.get(i).getStartDate()),convertDateTimetoDate(vacationsList.get(i).getEndDate()));
                        vacationsNew.add(vacItemCommon);
                    }
                    for (Vacations vacation2 : vacationsBacnet) {
                        if(!vacationsNew.contains(vacation2)){
                            DefaultSchedules.upsertVacation(null, "BACnetVacation", new DateTime(vacation2.startDate), new DateTime(vacation2.endDate));
                        }
                    }
                    CCUHsApi.getInstance().syncEntityTree();
                    ScheduleProcessJob.updateSchedules();
                    vacationsList = CCUHsApi.getInstance().getSystemSchedule( true);
                }
            }
    }

    public static void addNewVacations(BACnetObject scheduleObject, PropertyValue pv) {
        vacationsList = CCUHsApi.getInstance().getSystemSchedule(true);
        SequenceOf<SpecialEvent> exceptionSchedules = pv.getValue();
        SequenceOf<CalendarEntry> calendarEntries = new SequenceOf<>();
        if(exceptionSchedules.getCount() > 0){
            for (SpecialEvent specialEvent: exceptionSchedules){
                calendarEntries.add(specialEvent.getCalendarEntry());
            }
            for(Schedule vac75f: vacationsList) {
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
                    }
                }
            }
            CCUHsApi.getInstance().syncEntityTree();
            ScheduleProcessJob.updateSchedules();
        }
    }

    public static boolean compareVacations(SequenceOf<CalendarEntry> calendarEntries, ArrayList<Schedule> vacationSchedule){
        boolean changeDone = false;
        for (int i = 0; i < vacationSchedule.size(); i++) {
            DateTime vacStartDate = vacationSchedule.get(i).getStartDate();
            DateTime vacEndDate = vacationSchedule.get(i).getEndDate();
            long daysDiff = (vacEndDate.getMillis()-vacStartDate.getMillis())/(1000*60*60*24);

            if(daysDiff == 0){ //Single Calendar Entry
                CalendarEntry newCalendarEntry = new CalendarEntry(new Date(vacStartDate.getYear(), Month.valueOf(vacStartDate.getMonthOfYear()), vacStartDate.getDayOfMonth(), DayOfWeek.UNSPECIFIED));
                if(!calendarEntries.contains(newCalendarEntry)) {
                    changeDone = true;
                }
            }
            else { //Multiple Date Calendar Entry
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

}
