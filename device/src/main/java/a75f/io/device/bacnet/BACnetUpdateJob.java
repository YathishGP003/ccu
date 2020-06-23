package a75f.io.device.bacnet;

import com.renovo.bacnet4j.LocalDevice;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BaseJob;
import a75f.io.logic.L;
import a75f.io.logic.watchdog.WatchdogMonitor;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class BACnetUpdateJob extends BaseJob implements WatchdogMonitor
{
    BACNetwork BACnetNw;
    LocalDevice bacnetDevice;

    boolean watchdogMonitor = false;

    @Override
    public void bark() {
        watchdogMonitor = true;
    }

    @Override
    public boolean pet() {
        return watchdogMonitor;
    }

    public BACnetUpdateJob(LocalDevice localdevice)
    {
        super();
        bacnetDevice = localdevice;
        BACnetNw = new Bacnet();//TODO- TEMP
    }
    public BACnetUpdateJob() { super(); }
    public void doJob()
    {
        if(bacnetDevice != null/* && bacnetDevice.isInitialized()*/) {
            CcuLog.d(L.TAG_CCU_JOB, "BACnetUpdateJob -> ");
            watchdogMonitor = false;
            HashMap site = CCUHsApi.getInstance().read("site");
            if (site == null || site.size() == 0) {
                CcuLog.d(L.TAG_CCU_DEVICE, "No Site Registered ! <-BACnetUpdateJob ");
                return;
            }
            HashMap ccu = CCUHsApi.getInstance().read("ccu");
            if (ccu.size() == 0) {
                CcuLog.d(L.TAG_CCU_JOB, "No CCU Registered ! <-BACnetUpdateJob ");
                return;
            }
            BACnetNw.sendMessage(bacnetDevice);
            BACnetNw.sendSystemControl(bacnetDevice);
            CcuLog.d(L.TAG_CCU_JOB, "<-BACnetUpdateJob ");
        }
    }

    public LocalDevice getBacnetDevice(){
        return bacnetDevice;
    }

    public boolean terminateBACnet(){
        boolean isTerminated = false;
        bacnetDevice.terminate();
        isTerminated = bacnetDevice.isInitialized();
        bacnetDevice = null;
        return isTerminated;
    }
}
