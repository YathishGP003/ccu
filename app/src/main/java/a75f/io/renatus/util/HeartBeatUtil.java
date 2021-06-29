package a75f.io.renatus.util;

import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import a75f.io.renatus.R;

public class HeartBeatUtil {
    public static void zoneStatus(TextView zoneStatus, boolean isAlive){
        if (isAlive) {
            zoneStatus.setBackgroundResource(R.drawable.module_alive);
        } else {
            zoneStatus.setBackgroundResource(R.drawable.module_dead);
        }

    }

    public static void moduleSatus(TextView moduleStatus, String nodeAddress){
        if (isModuleAlive(nodeAddress)) {
            moduleStatus.setBackgroundResource(R.drawable.module_alive);
        } else {
            moduleStatus.setBackgroundResource(R.drawable.module_dead);
        }
    }

    public static boolean isModuleAlive(String nodeAddress){
        Date updatedTime = a75f.io.logic.bo.util.CCUUtils.getLastReceivedTime(nodeAddress);
        if(updatedTime == null){
            return false;
        }
        return TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - updatedTime.getTime()) < 15;
    }
    public static String getLastUpdatedTime(String nodeAddress){
        Date updatedTime = a75f.io.logic.bo.util.CCUUtils.getLastReceivedTime(nodeAddress);
        if(updatedTime == null){
            return "n/a";
        }
        StringBuffer message = new StringBuffer();
        Date currTime = new Date();
        if(currTime.getDate() == updatedTime.getDate()){
            return message.append("Today, ")
                    .append(getTime(updatedTime)).toString();

        } else{
            return getLastUpdatedTime(message, updatedTime).toString();
        }
    }

    public static String getDateSuffix(int updatedDate){
        if( updatedDate == 1 || updatedDate == 21 || updatedDate == 31) {
            return "st";
        }
        else if(updatedDate == 2 || updatedDate == 22) {
            return "nd";
        }
        else if(updatedDate == 3 || updatedDate == 23) {
            return "rd";
        }
        return "th";
    }

    public static String getLastUpdatedTime(StringBuffer message, Date updatedTime){
        SimpleDateFormat monthYearFormatter = new SimpleDateFormat("MMM, yyyy");
        String space = " ";
        return message.append("On")
                .append(space)
                .append(String.valueOf(updatedTime.getDate()))
                .append(getDateSuffix(updatedTime.getDate()))
                .append(space)
                .append(monthYearFormatter.format(updatedTime))
                .append(space)
                .append("|")
                .append(space)
                .append(getTime(updatedTime)).toString()
                .replace("pm", "PM")
                .replace("am", "AM");

    }

    public static String getTime(Date time){
        SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm:ss aa");
        return timeFormatter.format(time);
    }

    public static boolean isZoneAlive(ArrayList<HashMap> equips){
        for(HashMap equip : equips){
            String address = equip.get("group").toString();
            if(!isModuleAlive(address)){
                return false;
            }
        }
        return true;
    }
}
