package a75f.io.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.MeshNetwork;
import a75f.io.device.mesh.Pulse;
import a75f.io.device.modbus.ModbusNetwork;
import a75f.io.device.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.logger.CcuLog;
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
        CcuLog.d(L.TAG_CCU_JOB, "DeviceUpdateJob -> ");
        watchdogMonitor = false;
        feedTestData();
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site == null || site.size() == 0) {
            CcuLog.d(L.TAG_CCU_DEVICE,"No Site Registered ! <-DeviceUpdateJob ");
            return;
        }
        HashMap ccu = CCUHsApi.getInstance().read("ccu");
        if (ccu.size() == 0) {
            CcuLog.d(L.TAG_CCU_JOB,"No CCU Registered ! <-DeviceUpdateJob ");
            return;
        }
        deviceNw.sendMessage();
        deviceNw.sendSystemControl();
        CcuLog.d(L.TAG_CCU_JOB, "<-DeviceUpdateJob ");

        //Todo tobe tested with real device setup
        modbusNetwork.sendMessage();
        modbusNetwork.sendSystemControl();
    }
    
    private void feedTestData() {
        new Thread() {
            @Override public void run() {
                super.run();
                ArrayList<HashMap> devices = CCUHsApi.getInstance().readAll("device");
                Random rand = new Random();
                for (HashMap deviceMap : devices) {
                    if (deviceMap.containsKey("smartnode")) {
                        short addr = Short.parseShort(deviceMap.get("addr").toString());
                        if (addr % 10 > 0) {
                            CcuLog.d(L.TAG_CCU_JOB, "DeviceUpdateJob sendTestData to node " + addr);
                            CmToCcuOverUsbSnRegularUpdateMessage_t msg = new CmToCcuOverUsbSnRegularUpdateMessage_t();
                            msg.update.smartNodeAddress.set(addr);
                            msg.update.roomTemperature.set(600 + rand.nextInt(200));
                            Pulse.regularSNUpdate(msg);
                        }
                    }
                }
            }
        }.start();
    }
    
}
