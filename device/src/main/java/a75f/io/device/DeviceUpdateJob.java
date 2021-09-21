package a75f.io.device;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.MeshNetwork;
import a75f.io.device.modbus.ModbusNetwork;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.watchdog.WatchdogMonitor;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class DeviceUpdateJob extends BaseJob implements WatchdogMonitor
{
    DeviceNetwork deviceNw;
    ModbusNetwork modbusNetwork;
    boolean watchdogMonitor = false;
    
    private volatile boolean isJobRunning = false;
    
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
        modbusNetwork = new ModbusNetwork();
    }
    
    public void doJob()
    {
        try {
            CcuLog.d(L.TAG_CCU_JOB, "DeviceUpdateJob -> ");
            watchdogMonitor = false;
            if (isJobRunning) {
                CcuLog.d(L.TAG_CCU_JOB,"DeviceUpdateJob <- Instance of job still running");
                return;
            }
            
            isJobRunning = true;
            
            if (Globals.getInstance().isRecoveryModeActive()) {
                CcuLog.d(L.TAG_CCU_JOB,"DeviceUpdateJob <- RecoveryMode");
                return;
            }
            HashMap site = CCUHsApi.getInstance().read("site");
            if (site == null || site.size() == 0) {
                CcuLog.d(L.TAG_CCU_DEVICE, "No Site Registered ! <-DeviceUpdateJob ");
                return;
            }
            HashMap ccu = CCUHsApi.getInstance().read("ccu");
            if (ccu.size() == 0) {
                CcuLog.d(L.TAG_CCU_JOB, "No CCU Registered ! <-DeviceUpdateJob ");
                return;
            }
            deviceNw.sendMessage();
            deviceNw.sendSystemControl();
            CcuLog.d(L.TAG_CCU_JOB, "<-DeviceUpdateJob ");
            //Todo tobe tested with real device setup
            modbusNetwork.sendMessage();
            
            isJobRunning = false;
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_DEVICE,"DeviceUpdateJob Exception! ", e);
        }
    }
    
}
