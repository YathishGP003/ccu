package a75f.io.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.MeshNetwork;
import a75f.io.device.mesh.Pulse;
import a75f.io.device.modbus.ModbusNetwork;
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
        feedTestData();
    }
    
    
    private void feedTestData() {
        new Thread() {
            @Override public void run() {
                super.run();
                ArrayList<HashMap> devices = CCUHsApi.getInstance().readAll("device");
                Random rand = new Random();
                for (HashMap deviceMap : devices) {
                    if (deviceMap.containsKey("smartnode")) {
                        RxTask.executeAsync(() -> {
                            short addr = Short.parseShort(deviceMap.get("addr").toString());
                            CcuLog.d(L.TAG_CCU_JOB, "DeviceUpdateJob sendTestData to node " + addr);
                            CmToCcuOverUsbSnRegularUpdateMessage_t msg = new CmToCcuOverUsbSnRegularUpdateMessage_t();
                            msg.update.smartNodeAddress.set(addr);
                            msg.update.roomTemperature.set(650 + rand.nextInt(100));
                            msg.update.rssi.set((byte) -100);
                            SmartNodeSensorReading_t humidity = new SmartNodeSensorReading_t();
                            humidity.sensorType.set(1);
                            humidity.sensorData.set(20 + rand.nextInt(80));
                            SmartNodeSensorReading_t co2 = new SmartNodeSensorReading_t();
                            co2.sensorType.set(2);
                            co2.sensorData.set(1000 + rand.nextInt(1000));
                            SmartNodeSensorReading_t co = new SmartNodeSensorReading_t();
                            co.sensorType.set(3);
                            co.sensorData.set(200 + rand.nextInt(200));
                            SmartNodeSensorReading_t no = new SmartNodeSensorReading_t();
                            no.sensorType.set(4);
                            no.sensorData.set(rand.nextInt(10));
                            SmartNodeSensorReading_t[] sensors = new SmartNodeSensorReading_t[4];
                            sensors[0] = humidity;
                            sensors[1] = co2;
                            sensors[2] = co;
                            sensors[3] = no;
                            msg.update.sensorReadings = sensors;
                            Pulse.regularSNUpdate(msg);
                        });
                    }
                }
            }
        }.start();
    }
        
    }
