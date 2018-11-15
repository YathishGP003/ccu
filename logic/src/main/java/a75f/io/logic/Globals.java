package a75f.io.logic;

import android.content.Context;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.kinveybo.AlgoTuningParameters;
import a75f.io.kinveybo.DalContext;
import a75f.io.logic.bo.building.CCUApplication;
import a75f.io.logic.bo.building.Day;
import a75f.io.logic.bo.building.NamedSchedule;
import a75f.io.logic.bo.building.Schedule;

/**
 * Created by rmatt isOn 7/19/2017.
 */


/*
    This is used to keep track of global static associated with application context.
 */
public class Globals
{

    private static final int      NUMBER_OF_CYCLICAL_TASKS_RENATUS_REQUIRES = 10;
    private static final int      TASK_SEPERATION                           = 3;
    private static final TimeUnit TASK_SERERATION_TIMEUNIT                  = TimeUnit.SECONDS;
    private static Globals globals;
    //HeartBeatJob mHeartBeatJob;
    BuildingProcessJob mProcessJob = new BuildingProcessJob();
    private ScheduledExecutorService taskExecutor;
    private Context                  mApplicationContext;
    private CCUApplication           mCCUApplication;
    private LZoneProfile             mLZoneProfile;
    private boolean isSimulation = false;
    private boolean isDeveloperTest = true;


    private Globals()
    {
    }


    public ScheduledExecutorService getScheduledThreadPool()
    {
        return getInstance().taskExecutor;
    }


    public static Globals getInstance()
    {
        if (globals == null)
        {
            globals = new Globals();
        }
        return globals;
    }


    public CCUApplication ccu()
    {
        if (getInstance().mCCUApplication == null)
        {
            getInstance().mCCUApplication = LocalStorage.getApplicationSettings();
        }
        return getInstance().mCCUApplication;
    }


    public LZoneProfile getLZoneProfile()
    {
        if (getInstance().mLZoneProfile == null)
        {
            getInstance().mLZoneProfile = new LZoneProfile();
        }
        return getInstance().mLZoneProfile;
    }


    public boolean isSimulation()
    {
        return isSimulation;
    }





    public Context getApplicationContext()
    {
        return mApplicationContext;
    }


    public void setApplicationContext(Context mApplicationContext)
    {
        if (this.mApplicationContext == null)
        {
            this.mApplicationContext = mApplicationContext;
            initilize();
        }
    }


    public void initilize()
    {
        taskExecutor = Executors.newScheduledThreadPool(NUMBER_OF_CYCLICAL_TASKS_RENATUS_REQUIRES);
        DalContext.instantiate(this.mApplicationContext);
        populate();
        //mHeartBeatJob = new HeartBeatJob();
        //5 seconds after application initializes start heart beat
        int DEFAULT_HEARTBEAT_INTERVAL = 60;
        
        mProcessJob.scheduleJob("Building Process Job", DEFAULT_HEARTBEAT_INTERVAL,
                TASK_SEPERATION * 2, TASK_SERERATION_TIMEUNIT);
        
        isSimulation = getApplicationContext().getResources().getBoolean(R.bool.simulation);
        isDeveloperTest = getApplicationContext().getResources().getBoolean(R.bool.developer_test);
        new CCUHsApi(this.mApplicationContext);
    }

    public DalContext getDalContext()
    {
        return DalContext.getInstance();
    }


    private void populate()
    {
        //TODO: get this from kinvey.
        //This seems like overkill, but it has to follow the meta to support the unit test
        // framework.


        if(ccu().getDefaultCCUTuners() == null)
        {
            AlgoTuningParameters algoTuningParameters = new AlgoTuningParameters();
            ccu().setDefaultCCUTuners(L.getDefaultTuners());
        }

        //TODO test method
        if(ccu().getLCMNamedSchedules().size() == 0)
        {
            //Mock schedule M-F, 8AM - 5:30PM turn isOn lights to value 100.
            //Mock schedule M-F, 8AM - 5:30PM turn isOn lights to value 100.


            NamedSchedule namedSchedule = new NamedSchedule();
            namedSchedule.setName("LCM Named Schedule 100");
            namedSchedule.setSchedule(getSchedules(100));


            NamedSchedule namedScheduleTwo = new NamedSchedule();
            namedScheduleTwo.setName("LCM Named Schedule 75");
            namedScheduleTwo.setSchedule(getSchedules(75));

            ccu().getLCMNamedSchedules().put(namedSchedule.getName(), namedSchedule);
            ccu().getLCMNamedSchedules().put(namedScheduleTwo.getName(), namedScheduleTwo);
            ccu().setDefaultLightSchedule(getSchedules(100));
            ccu().setDefaultTemperatureSchedule(getSchedules(75));
        }
    }

    private ArrayList<Schedule> getSchedules(int val)
    {
        Schedule schedule = new Schedule();
        int[] ints = {0, 1, 2, 3, 4};
        ArrayList<Day> intsaslist = new ArrayList<Day>();
        for(int i : ints)
        { //as
            Day day = new Day();
            day.setDay(i);
            day.setSthh(8);
            day.setStmm(00);
            day.setEthh(17);
            day.setEtmm(30);
            day.setVal((short) val);
            intsaslist.add(day);
        }
        schedule.setDays(intsaslist);
        ArrayList<Schedule> schedules = new ArrayList<Schedule>();
        schedules.add(schedule);

        return schedules;
    }


    public boolean isDeveloperTesting()
    {
        return isDeveloperTest;
    }

    public void setCCU(CCUApplication CCU)
    {
        this.mCCUApplication = CCU;
    }
    
    public void saveTags(){
        CCUHsApi.getInstance().saveTagsData();
    }
}
