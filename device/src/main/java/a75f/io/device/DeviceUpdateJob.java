package a75f.io.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.MeshNetwork;
import a75f.io.device.mesh.Pulse;
import a75f.io.device.modbus.ModbusNetwork;
import a75f.io.device.serial.CmToCcuOverUsbSmartStatRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.device.serial.SmartNodeSensorReading_t;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.util.RxTask;
import a75f.io.logic.watchdog.WatchdogMonitor;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class DeviceUpdateJob extends BaseJob implements WatchdogMonitor
{
    DeviceNetwork deviceNw;
    ModbusNetwork modbusNetwork;
    boolean watchdogMonitor = false;
    
    private Lock jobLock = new ReentrantLock();
    private DeviceStatusUpdateJob deviceStatusUpdateJob;
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
    
        deviceStatusUpdateJob = new DeviceStatusUpdateJob();
        deviceStatusUpdateJob.scheduleJob("deviceStatusUpdateJob", 60,
                                    15, TimeUnit.SECONDS);
    }
    
    public void doJob()
    {
        if (jobLock.tryLock()) {
            try {
                CcuLog.d(L.TAG_CCU_JOB, "DeviceUpdateJob -> ");
                watchdogMonitor = false;
                
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
                modbusNetwork.sendMessage();
                CcuLog.d(L.TAG_CCU_JOB, "<-DeviceUpdateJob ");
            }
            catch (Exception e) {
                CcuLog.e(L.TAG_CCU_DEVICE, "DeviceUpdateJob Exception! ", e);
            } finally {
                jobLock.unlock();
            }
        } else {
            CcuLog.d(L.TAG_CCU_JOB, "DeviceUpdateJob <- Instance of job still running");
        }
    }
}
