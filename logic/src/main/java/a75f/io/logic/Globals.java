package a75f.io.logic;

import android.content.Context;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.Day;
import a75f.io.bo.building.NamedSchedule;
import a75f.io.bo.building.Schedule;
import a75f.io.dal.AlgoTuningParameters;
import a75f.io.dal.DalContext;

import static a75f.io.logic.LLog.Logd;

/**
 * Created by rmatt isOn 7/19/2017.
 */


/*
    This is used to keep track of global static associated with application context.
 */
class Globals
{
    
    private static final int      NUMBER_OF_CYCLICAL_TASKS_RENATUS_REQUIRES = 10;
    private static final int      TASK_SEPERATION                           = 3;
    private static final TimeUnit TASK_SERERATION_TIMEUNIT                  = TimeUnit.SECONDS;
    private static Globals globals;
    HeartBeatJob mHeartBeatJob;
    MeshUpdateJob mMeshUpdateJob = new MeshUpdateJob();
    private ScheduledExecutorService taskExecutor;
    private Context                  mApplicationContext;
    private CCUApplication           mCCUApplication;
    private LZoneProfile             mLZoneProfile;
    private boolean isSimulation = false;
    
    
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
    }    public CCUApplication ccu()
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
        mHeartBeatJob = new HeartBeatJob();
        //5 seconds after application initializes start heart beat
        int HEARTBEAT_INTERVAL = 60;
        Logd("Scheduling ---- HeartBeat Job");
        mHeartBeatJob
                .scheduleJob("Heartbeat Job", HEARTBEAT_INTERVAL, TASK_SEPERATION, TASK_SERERATION_TIMEUNIT);
        Logd("Scheduling ---- MeshUpdate Job");
        mMeshUpdateJob.scheduleJob("Mesh Update Job", HEARTBEAT_INTERVAL,
                TASK_SEPERATION * 2, TASK_SERERATION_TIMEUNIT);
        //5 seconds after heart beat initializes start profile scheduler.
        //Application in simulation mode notes
        //--Skips BLE
        //--Adds sleeps between any Serial Activity to work with biskit.
        isSimulation = getApplicationContext().getResources().getBoolean(R.bool.simulation);
        Logd("Simulation ----- " + isSimulation);
        
        
    }
    
    
    private void populate()
    {
        //TODO: get this from kinvey.
        //This seems like overkill, but it has to follow the meta to support the unit test
        // framework.
        AlgoTuningParameters algoTuningParameters = new AlgoTuningParameters();
        ccu().setDefaultCCUTuners(algoTuningParameters.getHashMap());
        
        //TODO test method
        if(ccu().getLCMNamedSchedules().size() == 0)
        {
            //Mock schedule M-F, 8AM - 5:30PM turn isOn lights to value 100.
            //Mock schedule M-F, 8AM - 5:30PM turn isOn lights to value 100.
            Schedule schedule = new Schedule();
            int[] ints = {1, 2, 3, 4, 5};
            ArrayList<Day> intsaslist = new ArrayList<Day>();
            for(int i : ints)
            { //as
                Day day = new Day();
                day.setDay(i);
                day.setSthh(8);
                day.setStmm(00);
                day.setEthh(17);
                day.setEtmm(30);
                day.setVal((short) 100);
                intsaslist.add(day);
            }
            schedule.setDays(intsaslist);
            NamedSchedule namedSchedule = new NamedSchedule();
            namedSchedule.setName("LCM Named Schedule");
            ArrayList<Schedule> schedules = new ArrayList<Schedule>();
            schedules.add(schedule);
            namedSchedule.setSchedule(schedules);
            ccu().getLCMNamedSchedules().put(namedSchedule.getName(), namedSchedule);
        }
    }
}
