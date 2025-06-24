package a75f.io.api.haystack.util;

import org.projecthaystack.HDateTime;
import org.projecthaystack.ParseException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import a75f.io.api.haystack.DAYS;
import a75f.io.logger.CcuLog;


public class TimeUtil {

    private static final int END_HOUR = 24;

    /**
     *  Below method takes end hour as input
     *  If input is 24hr then it returns 23
     * @param endTimeHour: last hour of schedule
     */
    public static int getEndHour(int endTimeHour){
        if(endTimeHour == END_HOUR){
            return 23;
        }

        return endTimeHour;
    }

    /**
     *  Below method takes end hour as input
     *  If input is 24hr then it returns 59
     * @param endTimeHour: last hour of schedule
     */
    public static int getEndMinute(int endTimeHour, int endTimeMinute){
        if(endTimeHour == END_HOUR){
            return 59;
        }

        return endTimeMinute;
    }

    /**
     *  Below method takes end hour as input
     *  If input is 24hr then it returns 59
     * @param endTimeHour: last hour of schedule
     */
    public static int getEndSec(int endTimeHour){
        if(endTimeHour == END_HOUR){
            return 59;
        }
        return 0;
    }

    public static int getEndTimeHr(int hour, int minute){
        if(hour == 23 && minute == 59){
            return 24;
        }
        return hour;
    }

    public static int getEndTimeMin(int hour, int minute){
        if(hour == 23 && minute == 59){
            return 0;
        }
        return minute;
    }

    public static String getEndTimeHrStr(int hour, int minute){
        if(hour == 23 && minute == 59){
            return "24";
        }else {
            if(hour < 9) {
                return "0"+ hour;
            }else {
                return String.valueOf(hour);
            }
        }
    }

    public static String getEndTimeMinStr(int hour, int minute){
        if(hour == 23 && minute == 59){
            return "00";
        }
        else if(minute == 0){
            return "00";
        }
        return String.valueOf(minute);
    }

    public static long getDateTimeInMillis(String dateTime) {
        long dateTimeInMillis = 0;
        try {
            dateTimeInMillis = HDateTime.make(dateTime).millis();
           CcuLog.d("CCU_AUTO_COMMISSIONING","converted in millis "+dateTimeInMillis);
        }catch (ParseException pe){
           CcuLog.d("CCU_AUTO_COMMISSIONING", pe.getMessage()+" Exception caught while parsing received date "+dateTime);
            pe.printStackTrace();
        }
        return dateTimeInMillis;
    }

    public static boolean isCurrentDayWeekDay(int day) {
        return day == DAYS.MONDAY.ordinal() || day == DAYS.TUESDAY.ordinal() ||
                day == DAYS.WEDNESDAY.ordinal() || day == DAYS.THURSDAY.ordinal() ||
                day == DAYS.FRIDAY.ordinal();
    }
    public static boolean isCurrentDaySaturday(int day) {
        return day == DAYS.SATURDAY.ordinal();
    }
    public static boolean isCurrentDaySunday(int day) {
        return day == DAYS.SUNDAY.ordinal();
    }

    public static String convertTo12HourFormat(String inputTime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm");
            Date date = inputFormat.parse(inputTime);

            SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a");
            return outputFormat.format(date);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            return inputTime;
        }
    }

    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = new Date();
        return sdf.format(date);
    }
}
