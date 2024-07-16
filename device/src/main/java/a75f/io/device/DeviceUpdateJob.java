package a75f.io.device;

import static java.lang.Thread.sleep;
import static a75f.io.device.serial.MessageType.HYPERSTAT_CM_TO_CCU_SERIALIZED_MESSAGE;
import static a75f.io.logic.bo.building.system.util.AdvancedAhuUtilKt.isConnectModuleAvailable;

import android.content.Context;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.bacnet.BacnetUtilKt;
import a75f.io.device.connect.ConnectModbusSerialComm;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.mesh.MeshNetwork;
import a75f.io.device.modbus.ModbusNetwork;
import a75f.io.device.serial.MessageType;
import a75f.io.domain.api.Domain;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu;
import a75f.io.logic.watchdog.WatchdogMonitor;
import a75f.io.usbserial.SerialAction;
import a75f.io.usbserial.SerialEvent;

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

        //TODO - TEMP code for performance testing to simulate device load. Remove this code after performance issue resolved
        //injectTestInputMessage();
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
                    if (isConnectModuleAvailable()) {
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

    private void injectTestInputMessage() {
        try {
            sleep(40000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        new Thread(() -> {
            while (true) {
                List<HashMap<Object, Object>> hsDevices = CCUHsApi.getInstance().readAllEntities("device and hyperstat");
                hsDevices.forEach(device -> {
                    injectTestRegularUpdateMessage(Integer.parseInt(device.get("addr").toString()), CCUHsApi.getInstance());
                    try {
                        sleep(1500);
                    } catch (InterruptedException e) {
                        CcuLog.e(L.TAG_CCU_DEVICE, "error ", e);
                    }
                });
            }
        }).start();
    }
    public static void injectTestRegularUpdateMessage(int address, CCUHsApi hayStack) {

        Random randomNumGenerator = new Random();
        a75f.io.device.HyperStat.HyperStatRegularUpdateMessage_t regularUpdateMessage = a75f.io.device.HyperStat.HyperStatRegularUpdateMessage_t.newBuilder()
                .setRoomTemperature(650 + randomNumGenerator.nextInt(150))
                .setHumidity(200 + randomNumGenerator.nextInt(400))
                .setExternalThermistorInput1(randomNumGenerator.nextInt(20000))
                .setExternalThermistorInput2(randomNumGenerator.nextInt(20000))
                .setExternalAnalogVoltageInput1(randomNumGenerator.nextInt(10000))
                .setExternalAnalogVoltageInput2(randomNumGenerator.nextInt(10000))
                .setOccupantDetected(randomNumGenerator.nextBoolean())
                .setIlluminance(randomNumGenerator.nextInt(500))
                .build();
        //handleRegularUpdate(regularUpdateMessage, nodeAddress, hayStack);
        int FIXED_INT_BYTES_SIZE = 4;
        byte[] dataBytes = regularUpdateMessage.toByteArray();
        byte[] msgBytes = new byte[dataBytes.length + FIXED_INT_BYTES_SIZE * 4 + 1];
        msgBytes[0] = (byte)HYPERSTAT_CM_TO_CCU_SERIALIZED_MESSAGE.ordinal();

        //Network requires un-encoded node address occupying the first 4 bytes
        System.arraycopy(getByteArrayFromInt(address), 0, msgBytes, 1, FIXED_INT_BYTES_SIZE);

        //Network requires un-encoded message type occupying the next 4 bytes
        System.arraycopy(getByteArrayFromInt(MessageType.HYPERSTAT_REGULAR_UPDATE_MESSAGE.ordinal()),
                0, msgBytes, FIXED_INT_BYTES_SIZE * 3 + 1, FIXED_INT_BYTES_SIZE);

        //Now fill the serialized protobuf messages
        System.arraycopy(dataBytes, 0, msgBytes,  4 * FIXED_INT_BYTES_SIZE + 1, dataBytes.length);

        SerialAction serialAction = SerialAction.MESSAGE_FROM_SERIAL_PORT;
        SerialEvent serialEvent = new SerialEvent(serialAction, msgBytes);
        LSerial.getInstance().handleSerialEvent(hayStack.getContext(), serialEvent);
    }
    private static byte[] getByteArrayFromInt(int integerVal) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(integerVal).array();
    }
}
