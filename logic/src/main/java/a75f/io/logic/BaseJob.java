package a75f.io.logic;

import java.util.concurrent.TimeUnit;

import static a75f.io.logic.LLog.Logd;

/**
 * Created by Yinten on 9/14/2017.
 */

abstract class BaseJob
{
    
    protected String mName;
    
    
    protected void scheduleJob(String name, int interval, int taskSeperation, TimeUnit unit)
    {
        mName = name;
        Logd("Scheduling: " + name + " interval: " + interval + " task Seperation:  " +
             taskSeperation + " unit: " + unit.name());
        // This task runs every minute.
        Globals.getInstance().getScheduledThreadPool().scheduleAtFixedRate(new Runnable()
        {
            public void run()
            {
                Logd("Job: " + mName + " executing");
                doJob();
            }
        }, taskSeperation, interval, unit);
    }
    
    
    protected abstract void doJob();
}
