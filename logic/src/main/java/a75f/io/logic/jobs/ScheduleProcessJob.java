package a75f.io.logic.jobs;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.VAVScheduler;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.BaseJob;

public class ScheduleProcessJob extends BaseJob {

    private static final String TAG = "ScheduleProcessJob";

    @Override
    public void doJob() {


        Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule();

        /* The systemSchedule isn't initiated yet, so schedules shouldn't be ran*/

        if(systemSchedule == null)
            return;
        Log.d("CCU","ScheduleProcessJob -> 1");

       // Log.i(TAG, "System Schedule != null " + (systemSchedule != null));


        Log.d("CCU","ScheduleProcessJob -> 2");
        //Read all equips
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");


        Log.d("CCU","ScheduleProcessJob -> 3");
        for(HashMap hs : equips)
        {
            Equip equip = new Equip.Builder().setHashMap(hs).build();
            Log.i(TAG, "Equip Dis: " + equip.toString());


            if(equip != null) {
                Log.d("CCU","ScheduleProcessJob -> 4");
                Schedule equipSchedule = getScheduleForEquip(equip);


                Log.d("CCU","ScheduleProcessJob -> 5");
                if (equipSchedule != null) {
                    writePointsForEquip(equip, equipSchedule);
                } else {
                    Log.e(TAG, "Schedule is Null, use system Schedule!");
                    Log.d("CCU","ScheduleProcessJob -> 6");
                    writePointsForEquip(equip, systemSchedule);
                }
            }
            else
            {
                Log.e(TAG, "Equip is Null!");
            }
        }

        Log.d("CCU","ScheduleProcessJob -> 7");

        Log.d("CCU","< - END ScheduleProcessJob");
        Log.i(TAG, "Write Schedule VALUES");
        Log.d("CCU","ScheduleProcessJob ->");
    }

    private void writePointsForEquip(Equip equip, Schedule equipSchedule) {
        if(equip.getMarkers().contains("VAV"))
        {
            VAVScheduler.processEquip(equip, equipSchedule);
        }
    }

    public void setDesiredTemp(Equip equip, Double desiredTemp)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and sp and equipRef == \""+equip.getId()+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }

        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
        CCUHsApi.getInstance().pointWrite(HRef.make(id), 9, "Scheduler", desiredTemp == null ? HNum.make(desiredTemp) : null, null);
    }

    /* Check to see if this equips zoneRef has a ScheduleRef, if it does use that or use */
    private Schedule getScheduleForEquip(Equip equip) {



        if(equip.getZoneRef() != null) {
            Log.i(TAG, "Equip Zone Ref: " + equip.getZoneRef());
            HashMap zoneHashMap = CCUHsApi.getInstance().readMapById(equip.getZoneRef());

            Zone build = new Zone.Builder().setHashMap(zoneHashMap).build();
            if(build.getScheduleRef() != null && !build.getScheduleRef().equals(""))
            {
                Schedule schedule = CCUHsApi.getInstance().getScheduleById(build.getScheduleRef());

                if(schedule != null) {
                    Log.i(TAG, "Schedule: "+ schedule.toString());

                    return schedule;
                }
            }
        }


        return null;
    }

}
