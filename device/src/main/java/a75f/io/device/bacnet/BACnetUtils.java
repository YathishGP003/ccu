package a75f.io.device.bacnet;

import android.util.Log;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.enums.Month;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.DateTime;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.Time;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import a75f.io.device.mesh.Pulse;

public class BACnetUtils{

    private static LocalDevice localDevice;
    //Zone Points Instance number for identification
    //Except Schedules, Calender and Trendlogs, all other zone points will have SmartNodeAddress+"01" until 99 objects
    // for unique ID instance example current temp of zone2 with 9901 will be say "990101" where 9900 is the start address
    //Analog Value objects instance number defined here until 40
    public static final int currentTemp = 1;
    public static final int desiredTemp = 1;
    public static final int desiredTempCooling = 2;
    public static final int desiredTempHeating = 3;
    public static final int damperPos = 4;
    public static final int notificationID = 10001;
    public static final int occupancyStatus = 5; //setback, setpoint, precondition, temphold, vacation
    public static final int conditioningStatus = 6; // off, cooling, heating, tempdead
    public static final int HUMIDITY_SENSOR_VALUE = 7; //7 till 17 we use it for sensor values
    public static final int UVI_SENSOR_VALUE = 17;
    public static final int BATTERY_STATUS = 100;
    public static final int BATTERY_STATUS_NOTIFICATION = 101;
    public static final int ALERT_FATAL = 1001;
    public static final int ALERT_ERROR = 1002;
    public static final int ALERT_WARN = 1003;
    public static final int ALERT_PRIORITY = 1004;
    public static final String ALERT_FATAL_TITLE = "SEVERE_ALARM";
    public static final String ALERT_ERROR_TITLE = "MODERATE_ALARM";
    public static final String ALERT_WARN_TITLE = "WARN_ALARM";
    public static final String ALERT_PRIORITY_TITLE = "PRIORITY_ALARM";
    public static final double defaultTemp = 72.0;
    //MultiState Value objects instance number defined here from 41 till 70

    //Binary value objects instance number defined here from 71 till 99

    //System BV Points
    public static int relay1 = 1;
    public static int relay2 = 2;
    public static int relay3 = 3;
    public static int relay4 = 4;
    public static int relay5 = 5;
    public static int relay6 = 6;
    public static int relay7 = 7;
    public static int analogOut1 = 8;
    public static int analogOut2 = 9;
    public static int analogOut3 = 10;
    public static int analogOut4 = 11;
    public static int analogIn1 = 12;
    public static int analogIn2 = 13;
    public static int thermister1 = 14;
    public static int thermister2 = 15;

    //Bacnet Schedule
    public static int systemSchedule = 16;
    public static int zoneSchedule = 17;
    public static int systemCalendar = 18;
    public static int zoneCalendar = 19;
    public static int schedulePriority = 9;
    public static int scheduleOccupancy = 20;
    public static int occupanyHeatingDT = 21;
    public static int occupanyCoolingDT = 22;

    //Trend Object
    public static int bufferSize = 200;
    public static int logInterval = 60; //Ideally 300 Seconds or 60 Seconds for demo
    public static int covResubscriptionInterval = 900; //Ideally 300 Seconds or 60 Seconds for demo
    public static int buildingLimitMin = 55;
    public static int buildingLimitMax = 90;

    //Increment Values
    public static float incrementValue = 0.5f;
    public static float currentTempCOV = 0.1f;

    //Password for Device Initialise
    public static String PASSWORD = "admin@75f";
    public static void setLocalDevice(LocalDevice bacnetDevice){
        localDevice = bacnetDevice;
    }

    public static int getUtcOffset()
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault());
        TimeZone timeZone = TimeZone.getTimeZone("GMT").getDefault();
        int offsetInMillis = timeZone.getOffset(calendar.getTimeInMillis());

        String offset = String.format("%02d:%02d", Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60));
        int inHrs = Math.abs(offsetInMillis / 3600000);
        int totalMins = inHrs*60 + Math.abs((offsetInMillis / 60000) % 60);
        offset = (offsetInMillis >= 0 ? "+" : "-") + totalMins;
        Log.i("Bacnet"," offset:"+offset+" inHrs:"+inHrs+" total Offset Mins:"+totalMins);

        //String offset = String.format("%02d:%02d", Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60));
        //offset = (offsetInMillis >= 0 ? "+" : "-") + offset;

        return -(Integer.parseInt(offset));
    }
    public static void removeZone(short nodeAddress) {

        Log.i("BACnet","Remove Zone:"+nodeAddress);
        if(localDevice != null)
            new ZonePoints().deleteZonePoints(localDevice,nodeAddress);
    }

    public static void removeModule(short nodeAddress) {
        Log.i("BACnet","Remove Module:"+nodeAddress);
        if(localDevice != null)
            new ZonePoints().deleteZonePoints(localDevice,nodeAddress);

    }

    public static String getCurrentTimezoneOffset() {

        TimeZone tz = TimeZone.getDefault();
        Calendar cal = GregorianCalendar.getInstance(tz);
        int offsetInMillis = tz.getOffset(cal.getTimeInMillis());

        String offset = String.format("%02d:%02d", Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60));
        offset = (offsetInMillis >= 0 ? "+" : "-") + offset;

        return offset;
    }

    public static void updateBacnetChanges(Address from, BACnetObject object, PropertyValue pv, boolean isSameallDay, boolean isWeeklySchedule){
        //if(object.getObjectName().contains("heatingDesiredTemp")){
        Log.i("Bacnet","Write Data-Type:"+object.get(PropertyIdentifier.objectType)+" property:"+ pv.getPropertyIdentifier());
        if(object.get(PropertyIdentifier.objectType) == ObjectType.analogValue && pv.getPropertyIdentifier().equals(PropertyIdentifier.presentValue))
        {
            String address = String.valueOf(object.getInstanceId()).substring(0,4);
            int addressNumber = Integer.parseInt(address+"00");
            int instanceCoolID = addressNumber + BACnetUtils.desiredTempCooling;
            int instanceHeatID = addressNumber + BACnetUtils.desiredTempHeating;
                if (object.getInstanceId() == instanceHeatID) {
                    Pulse.updateSetTempFromBacnet(Short.valueOf(address), Double.parseDouble(pv.getValue().toString()), "heating");
                }if (object.getInstanceId() == instanceCoolID) {
                    Pulse.updateSetTempFromBacnet(Short.valueOf(address), Double.parseDouble(pv.getValue().toString()), "cooling");
                }
        }else //if(object.getObjectName().contains("Building Schedule")){
            //TODO handle Schedule and vacation from Bacnet objects for building Schedule changes
            if(object.get(PropertyIdentifier.objectType) == ObjectType.schedule && isWeeklySchedule)
            {
                Log.i("BACnetUtil","Building Schedule="+object.getObjectName()+" value:"+pv.getValue());
                Log.i("Bacnet","Building Schedule="+object.getObjectName()+" value:"+pv.getValue());
                BACnetScheduler.updateSchedule(object,isSameallDay);
            //}
        }else if(object.getObjectName().contains("Schedule Occupied Heating")){
            //TODO handle Schedule and vacation from Bacnet objects for building Schedule changes
            Log.i("BACnetUtil","Building Schedule="+object.getObjectName()+" value:"+pv.getValue());
            BACnetScheduler.updateSchedule(object,isSameallDay);

        }else if(object.getObjectName().contains("Schedule Occupied Cooling")){
            //TODO handle Schedule and vacation from Bacnet objects for building Schedule changes
            Log.i("BACnetUtil","Building Schedule="+object.getObjectName()+" value:"+pv.getValue());
            BACnetScheduler.updateSchedule(object,isSameallDay);

        }else if(object.get(PropertyIdentifier.objectType).equals(ObjectType.calendar) && pv.getPropertyIdentifier().equals(PropertyIdentifier.dateList)){
            //TODO handle Vacation from Bacnet objects for building vacation changes
            Log.i("BACnetUtil","Building Vacations="+object.getObjectName()+" value:"+pv.getValue());
            BACnetScheduler.updateVacations(object);
        }
    }

    public static String convertDateTime(DateTime dateTime){
            String dateTimeStr = null;
                int year =  dateTime.getDate().getYear()+1900;
                int date =  dateTime.getDate().getDay();
                Month monthObj = dateTime.getDate().getMonth();
                int month = Month.getIDof(monthObj);

                int hour =  dateTime.getTime().getHour();
                int minute =  dateTime.getTime().getMinute();
                int seconds =  dateTime.getTime().getSecond();


                String dateStr = Integer.toString(date);
                String monthStr = Integer.toString(month);
                String hrStr = Integer.toString(hour);
                String minStr = Integer.toString(minute);
                String secStr = Integer.toString(seconds);

                if(month<10){
                    monthStr = "0"+month;
                }if(date<10){
                    dateStr = "0"+date;
                }
                if(hour<10){
                    hrStr = "0"+hour;
                }
                if(minute<10){
                    minStr = "0"+minute;
                }
                if(seconds<10){
                    secStr = "0"+seconds;
                }

                dateTimeStr = monthStr+dateStr+hrStr+minStr+year+"."+secStr;
            return dateTimeStr;
    }

    public static String convertDateTime(com.renovo.bacnet4j.type.primitive.Date dateValue, Time timeValue){
                String dateTimeStr = null;
                int year =  dateValue.getYear()+1900;
                int date =  dateValue.getDay();
                Month monthObj = dateValue.getMonth();
                int month = Month.getIDof(monthObj);

                int hour =  timeValue.getHour();
                int minute =  timeValue.getMinute();
                int seconds = timeValue.getSecond();


                String dateStr = Integer.toString(date);
                String monthStr = Integer.toString(month);
                String hrStr = Integer.toString(hour);
                String minStr = Integer.toString(minute);
                String secStr = Integer.toString(seconds);

                if(month<10){
                    monthStr = "0"+month;
                }if(date<10){
                    dateStr = "0"+date;
                }
                if(hour<10){
                    hrStr = "0"+hour;
                }
                if(minute<10){
                    minStr = "0"+minute;
                }
                if(seconds<10){
                    secStr = "0"+seconds;
                }

                dateTimeStr = monthStr+dateStr+hrStr+minStr+year+"."+secStr;
            return dateTimeStr;
    }

    public static String convertUTCtime(DateTime dateTime, int utcOffset){
        String dateTimeStr = null;
        try {
            String dateTimeVal = convertDateTime(dateTime);
            SimpleDateFormat sdf = new SimpleDateFormat("MMddHHmmyyyy.ss");
            Date theDate = sdf.parse (dateTimeVal);
            long minutes = theDate.getTime() / 60000;
            long actualTime = TimeUnit.MINUTES.toMillis(minutes);
            long utcTime = TimeUnit.MINUTES.toMillis(minutes-utcOffset);

            Date result = new Date(utcTime);
            dateTimeStr = sdf.format(result);
            Log.i("Bacnet","DateTime:"+dateTime+" minutes:"+minutes+" date:"+theDate+" actualTime:"+actualTime+" utcTimeDiff:"+utcTime+" converted:"+result+" formatted:"+result);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateTimeStr;
    }
}
