package a75f.io.device;

import java.util.concurrent.TimeUnit;

import a75f.io.logic.Globals;

/**
 * Created by Yinten on 9/14/2017.
 */

abstract class BaseJob
{
    
    protected String mName;
    
    
    protected void scheduleJob(String name, int interval, int taskSeperation, TimeUnit unit)
    {
        mName = name;
        LLog.Logd("Device Scheduling: " + name + " interval: " + interval + " task Seperation:  " +
                  taskSeperation + " unit: " + unit.name());
        // This task runs every minute.
        Globals.getInstance().getScheduledThreadPool().scheduleAtFixedRate(new Runnable()
        {
            public void run()
            {
                LLog.Logd("Job: " + mName + " executing");
                doJob();
            }
        }, taskSeperation, interval, unit);
    }
    
    
    protected abstract void doJob();
}
