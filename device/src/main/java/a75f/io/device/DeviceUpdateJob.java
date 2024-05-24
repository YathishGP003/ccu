package a75f.io.device;

import android.content.Context;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.bacnet.BacnetUtilKt;
import a75f.io.device.connect.ConnectModbusSerialComm;
import a75f.io.device.mesh.MeshNetwork;
import a75f.io.device.modbus.ModbusNetwork;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu;
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
        deviceNw = new MeshNetwork();//TODO- TPpoEMP
        modbusNetwork = new ModbusNetwork();
    
        deviceStatusUpdateJob = new DeviceStatusUpdateJob();
        deviceStatusUpdateJob.scheduleJob("deviceStatusUpdateJob", Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                        .getInt("control_loop_frequency",60), 45, TimeUnit.SECONDS);
    }
    
    public void doJob()
    {
        watchdogMonitor = false;
        ConnectModbusSerialComm.testReadOp();
        CcuLog.d(L.TAG_CCU_JOB, "DeviceUpdateJob -> ");
        if (!CCUHsApi.getInstance().isCCUConfigured() || Globals.getInstance().isRecoveryMode() ||
                Globals.getInstance().isSafeMode()) {
            CcuLog.d(L.TAG_CCU_JOB,"CCU not configured ! <-DeviceUpdateJob ");
            return;
        }
        
        if (!CCUHsApi.getInstance().isCcuReady()) {
            CcuLog.d(L.TAG_CCU_JOB,"CCU not ready ! <-DeviceUpdateJob ");
            return;
        }
        if (jobLock.tryLock()) {
            try {
                if (Globals.getInstance().getBuildingProcessStatus()) {
                    if (L.ccu().systemProfile instanceof VavAdvancedAhu) {
                        VavAdvancedAhu profile = (VavAdvancedAhu) L.ccu().systemProfile;
                        if (profile.isConnectModuleAvailable()) {
                            ConnectModbusSerialComm.sendSettingConfig();
                            ConnectModbusSerialComm.sendControlsMessage(Domain.connect1Device);
                            ConnectModbusSerialComm.getRegularUpdate();
                        } else {
                            CcuLog.e(L.TAG_CCU_DEVICE, "Connect device not found");
                        }

                    }
                    deviceNw.sendMessage();
                    deviceNw.sendSystemControl();
                    } else {
                        CcuLog.e(L.TAG_CCU_DEVICE, "Device update skipped , buildingProcess not running");
                    }
                    BacnetUtilKt.checkBacnetHealth();
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
