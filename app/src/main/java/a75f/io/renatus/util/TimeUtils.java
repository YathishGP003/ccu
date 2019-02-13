package a75f.io.renatus.util;

import android.content.Context;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class TimeUtils {

    public static String valToTime(int value) {
        StringBuilder mBuilder = new StringBuilder();
        java.util.Formatter mFmt = new java.util.Formatter(mBuilder, java.util.Locale.US);
        Object[] mArgs = new Object[2];
        mArgs[0] = value/4;
        mArgs[1] = (value%4)*(15);
        mBuilder.delete(0, mBuilder.length());
        mFmt.format("%02d:%02d", mArgs);
        return mFmt.toString();
    }

    public static String valToTime12Hr(int value) {
        StringBuilder mBuilder = new StringBuilder();
        java.util.Formatter mFmt = new java.util.Formatter(mBuilder, java.util.Locale.US);
        Object[] mArgs = new Object[2];
        if (value/4 > 12)
            mArgs[0] = value/4 - 12;
        else
            mArgs[0] = value/4;
        mArgs[1] = (value%4)*(15);
        mBuilder.delete(0, mBuilder.length());
        if (value/4 > 11)
            mFmt.format("%02d:%02d PM", mArgs);
        else {
            if(value < 4)
                mFmt.format("12:%02d AM",mArgs[1]);
            else
                mFmt.format("%02d:%02d AM", mArgs);
        }
        return mFmt.toString();
    }
    public static Calendar valToTimeCal(int value) {
        int hour = value/4;
        int min = (value%4)*(15);
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, hour);
        now.set(Calendar.MINUTE, min);
        return now;
    }

    public static String strToDateMMMDD(Date val){
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.US);
        return sdf.format(val);
    }

    public static boolean isForcedTimeEnd(String forceTime){
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aaa");
            Date parse = sdf.parse(forceTime);
            Calendar fot = sdf.getCalendar();
            fot.setTime(parse);
            Calendar c = Calendar.getInstance();
            fot.set(Calendar.DATE,c.get(Calendar.DATE));
            fot.set(Calendar.MONTH,c.get(Calendar.MONTH));
            fot.set(Calendar.YEAR, c.get(Calendar.YEAR));
            Date ft = new Date(fot.getTimeInMillis());
            Date et = new Date();
            et.setTime(System.currentTimeMillis());
            return et.after(ft);
        }catch (ParseException pe){

        }
        return false;
    }
    public static Date strToDateMMDDYYYY(String val) throws ParseException {
        String myFormat = "MM-dd-yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        return sdf.parse(val);
    }

    public static String valToTime1x60(int value) {
        StringBuilder mBuilder = new StringBuilder();
        java.util.Formatter mFmt = new java.util.Formatter(mBuilder, java.util.Locale.US);
        Object[] mArgs = new Object[2];
        mArgs[0] = value/60;
        mArgs[1] = (value%60)*(60/60);
        mBuilder.delete(0, mBuilder.length());
        mFmt.format("%02d:%02d", mArgs);
        return mFmt.toString();
    }

    public static int timeToVal(String str) {
        int hr = Integer.parseInt(str.substring(0, 2));
        int min = Integer.parseInt(str.substring(3,5));
        if (str.contains("PM") && hr < 12)
            return (48+(hr*4))+(min/15);
        else if(str.contains("AM") && hr == 12){
            return 0+(min / 15);
        } else {
            return (hr * 4) + (min / 15);
        }
    }

    public static int dp2px(Context context, int dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static int getCurrentTimeSlot() {
        Calendar calendar = GregorianCalendar.getInstance();
        return (calendar.get(Calendar.HOUR_OF_DAY)*4) + (calendar.get(Calendar.MINUTE)/15);
    }

    public static int getCurrentDayOfWeekWithMondayAsStart() {
        Calendar calendar = GregorianCalendar.getInstance();
        switch (calendar.get(Calendar.DAY_OF_WEEK))
        {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            case Calendar.SUNDAY: return 6;
        }
        return 0;
    }

    public static String getNameOfDayWithMondayAsStart(int nDay) {
        switch (nDay) {
            case 0: return "monday";
            case 1: return "tuesday";
            case 2: return "wednesday";
            case 3: return "thursday";
            case 4: return "friday";
            case 5: return "saturday";
            case 6: return "sunday";
        }
        return "";
    }
}
