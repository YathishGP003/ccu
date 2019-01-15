package a75f.io.logic.jobs;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.BaseJob;

public class ScheduleProcessJob extends BaseJob {

    private static final String TAG = "ScheduleProcessJob";

    @Override
    public void doJob() {
        Log.i(TAG, "Write Schedule VALUES");

        Log.d("CCU","ScheduleProcessJob ->");

        Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule();


        Log.i(TAG, "System Schedule != null " + (systemSchedule != null));

        //Read all equips
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");

        for(HashMap hs : equips)
        {
            Equip equip = new Equip.Builder().setHashMap(hs).build();
            Log.i(TAG, "Equip Dis: " + equip.toString());


            if(equip != null) {
                Schedule equipSchedule = getScheduleForEquip(equip);

                if (equipSchedule != null) {
                    writePointsForEquip(equip, equipSchedule);
                } else {
                    Log.e(TAG, "Schedule is Null, use system Schedule!");
                    writePointsForEquip(equip, systemSchedule);
                }
            }
            else
            {
                Log.e(TAG, "Equip is Null!");
            }
        }



        Log.d("CCU","< - END ScheduleProcessJob");



    }

    private void writePointsForEquip(Equip equip, Schedule equipSchedule) {


        Log.i(TAG, "Equip: " + equip);
        Log.i(TAG, "Equip Schedule: " + equipSchedule);
    }



    /* Check to see if this equips zoneRef has a ScheduleRef, if it does use that or use */
    private Schedule getScheduleForEquip(Equip equip) {



        if(equip.getZoneRef() != null) {
            Log.i(TAG, "Equip Zone Ref: " + equip.getZoneRef().toString());



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
