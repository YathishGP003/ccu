package a75f.io.logic;

import java.util.concurrent.TimeUnit;

import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 9/14/18.
 */

public abstract class BaseJob
{
    protected String mName;
    
    
    public void scheduleJob(String name, int interval, int taskSeperation, TimeUnit unit)
    {
        mName = name;
        CcuLog.i(L.TAG_CCU_JOB, "Scheduling: " + name + " interval: " + interval + " task Seperation:  " + taskSeperation + " unit: " + unit.name());
        // This task runs every minute.
        Globals.getInstance().getScheduledThreadPool().scheduleAtFixedRate(new Runnable()
        {

            @Override
            public void run()
            {
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        super.run();
                        CcuLog.i(L.TAG_CCU_JOB, "Job: " + mName + " executing");
                        doJob();
                        CcuLog.i(L.TAG_CCU_JOB, "Job: " + mName + " finishing");
                    }
                }.start();

            }
        }, taskSeperation, interval, unit);
    }
    
    
    public abstract void doJob();
}
