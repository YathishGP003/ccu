package a75f.io.logic;

import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * Created by samjithsadasivan on 9/14/18.
 */

public abstract class BaseJob
{
    protected String mName;
    
    
    public void scheduleJob(String name, int interval, int taskSeperation, TimeUnit unit)
    {
        mName = name;
        Log.i("CCU_LOGIC","Scheduling: " + name + " interval: " + interval + " task Seperation:  " + taskSeperation + " unit: " + unit.name());
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
                        Log.i("CCU_LOGIC", "Job: " + mName + " executing");
                        doJob();
                        Log.i("CCU_LOGIC", "Job: " + mName + " finishing");
                    }
                }.start();

            }
        }, taskSeperation, interval, unit);
    }
    
    
    public abstract void doJob();
}
