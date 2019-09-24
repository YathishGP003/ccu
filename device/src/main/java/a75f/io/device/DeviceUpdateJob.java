package a75f.io.device;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.MeshNetwork;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.watchdog.WatchdogMonitor;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class DeviceUpdateJob extends BaseJob implements WatchdogMonitor
{
    DeviceNetwork deviceNw;
    
    boolean watchdogMonitor = false;
    
    @Override
    public void bark() {
        watchdogMonitor = true;
    }
    
    @Override
    public boolean pet() {
        return watchdogMonitor;
    }
    
    public DeviceUpdateJob()
    {
        super();
        deviceNw = new MeshNetwork();//TODO- TEMP
    }
    
    public void doJob()
    {
        CcuLog.d(L.TAG_CCU_JOB, "DeviceUpdateJob -> ");
        watchdogMonitor = false;
        
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site == null || site.size() == 0) {
            CcuLog.d(L.TAG_CCU_DEVICE,"No Site Registered ! <-DeviceUpdateJob ");
            return;
        }
        deviceNw.sendMessage();
        deviceNw.sendSystemControl();
        CcuLog.d(L.TAG_CCU_JOB, "<-DeviceUpdateJob ");
    }
}
