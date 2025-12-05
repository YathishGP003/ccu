package a75f.io.logic.watchdog;

import static android.os.Process.SIGNAL_QUIT;
import static android.os.Process.myPid;
import static android.os.Process.sendSignal;

import static a75f.io.util.UtilKt.triggerRebirth;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.concurrent.CopyOnWriteArrayList;

import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;

public class Watchdog extends Thread
{
    public static final int WDT_INTERVAL_MINUTES = 5;
    public static final String TAG = "CCU_WDT";
    
    static Watchdog watchdog = new Watchdog();

    CopyOnWriteArrayList<WatchdogMonitor> wdtMonitors = new CopyOnWriteArrayList<>();
    
    public static Watchdog getInstance()
    {
        return watchdog;
    }
    
    public void monitor() {
        CcuLog.d(TAG," Start WDT Monitoring: ");
        watchdog.start();
    }
    
    private Watchdog()
    {
        super("watchdog");
    }
    
    public void addMonitor(WatchdogMonitor m) {
        wdtMonitors.add(m);
    }
    
    @Override
    public void run()
    {
        while (true)
        {
            for (WatchdogMonitor m : wdtMonitors) {
                if (m.pet()) {
                    CcuLog.d(TAG,"Job not responding "+m.getClass().getSimpleName()+", Watchdog killing Renatus : GOOD BYE !!!");
                    bite();
                    break;
                } else {
                    CcuLog.d(TAG,"Bark: "+m.getClass().getSimpleName());
                    m.bark();
                }
            }
            
            try
            {
                sleep(WDT_INTERVAL_MINUTES * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
        }
    }
    
    private void bite() {
        CcuLog.w(TAG," Watchdog biting - Putting Thread Dump in data/anr/traces.txt and restarting application in 3 seconds");
        sendSignal(myPid(), SIGNAL_QUIT);
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        CcuLog.w(TAG," Watchdog biting - Restarting Application");
        triggerRebirth(Globals.getInstance().getApplicationContext());
    }
}
