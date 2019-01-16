package a75f.io.device;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import a75f.io.device.mesh.DLog;
import a75f.io.logic.Globals;

/**
 * Created by Yinten on 9/14/2017.
 */

abstract class BaseJob
{
    
    protected String mName;
    public ScheduledFuture<?> scheduledFuture = null;
    public void scheduleJob(String name, int interval, int taskSeperation, TimeUnit unit)
    {
        mName = name;
        DLog.Logd("Device Scheduling: " + name + " interval: " + interval + " task Seperation:  " +
                  taskSeperation + " unit: " + unit.name());
        // This task runs every minute.
        scheduledFuture = Globals.getInstance().getScheduledThreadPool().scheduleAtFixedRate(new Runnable()
        {
            public void run()
            {
                DLog.Logd("Job: " + mName + " executing");
                doJob();
            }
        }, taskSeperation, interval, unit);
    }
    
    
    public abstract void doJob();
}
