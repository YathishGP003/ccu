package a75f.io.logic.watchdog;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import a75f.io.logger.CcuLog;

public class Watchdog extends Thread
{
    public static final int WDT_INTERVAL_MINUTES = 5;
    
    static Watchdog watchdog = new Watchdog();

    CopyOnWriteArrayList<WatchdogMonitor> wdtMonitors = new CopyOnWriteArrayList<>();
    
    public static Watchdog getInstance()
    {
        return watchdog;
    }
    
    public void monitor() {
        CcuLog.d("CCU_WDT"," Start WDT Monitoring: ");
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
                    CcuLog.d("CCU_WDT","Job not responding "+m.getClass().getSimpleName()+", Watchdog killing Renatus : GOOD BYE !!!");
                    bite();
                    break;
                } else {
                    CcuLog.d("CCU_WDT","Bark: "+m.getClass().getSimpleName());
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
        int pid = android.os.Process.myPid();
    
        try
        {
            Process p = Runtime.getRuntime().exec("su");
            InputStream es = p.getErrorStream();
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("kill -3 " + pid + "\n");
            os.writeBytes("exit\n");
            os.flush();
            os.close();
            int read;
            byte[] buffer = new byte[4096];
            String output = new String();
            while ((read = es.read(buffer)) > 0)
            {
                output += new String(buffer, 0, read);
            }
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        android.os.Process.killProcess(pid);
        System.exit(10);
    }
}
