package a75f.io.logic.jobs;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BaseJob;
import a75f.io.logic.L;

public class ScheduleProcessJob extends BaseJob {

    private static final String TAG = "ScheduleProcessJob";

    @Override
    public void doJob() {


        Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule();

        /* The systemSchedule isn't initiated yet, so schedules shouldn't be ran*/

        if(systemSchedule == null)
            return;
        CcuLog.d(L.TAG_CCU_JOB,"ScheduleProcessJob");
        //Read all equips
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");
        for(HashMap hs : equips)
        {
            Equip equip = new Equip.Builder().setHashMap(hs).build();
            CcuLog.d(L.TAG_CCU_JOB, "Equip Dis: " + equip.toString());


            if(equip != null) {
               
                Schedule equipSchedule = getScheduleForEquip(equip);
                if (equipSchedule != null) {
                    writePointsForEquip(equip, equipSchedule);
                } else {
                    CcuLog.d(L.TAG_CCU_JOB, "Schedule is Null, use system Schedule!");
                    writePointsForEquip(equip, systemSchedule);
                }
            }
            else
            {
                CcuLog.d(L.TAG_CCU_JOB, "Equip is Null!");
            }
        }
        CcuLog.d(L.TAG_CCU_JOB,"<- ScheduleProcessJob");
    }

    private void writePointsForEquip(Equip equip, Schedule equipSchedule) {
        if(equip.getMarkers().contains("vav"))
        {
            VAVScheduler.processEquip(equip, equipSchedule);
        }
    }


    /* Check to see if this equips zoneRef has a ScheduleRef, if it does use that or use */
    private Schedule getScheduleForEquip(Equip equip) {



        if(equip.getRoomRef() != null) {
            CcuLog.d(L.TAG_CCU_SCHEDULER, "Equip Zone Ref: " + equip.getRoomRef());
            HashMap zoneHashMap = CCUHsApi.getInstance().readMapById(equip.getRoomRef().replace("@", ""));

            Zone build = new Zone.Builder().setHashMap(zoneHashMap).build();
            if(build.getScheduleRef() != null && !build.getScheduleRef().equals(""))
            {
                Schedule schedule = CCUHsApi.getInstance().getScheduleById(build.getScheduleRef());

                if(schedule != null) {
                    CcuLog.d(L.TAG_CCU_SCHEDULER, "Schedule: "+ schedule.toString());

                    return schedule;
                }
            }
        }


        return null;
    }

}
