package a75f.io.device;

import static java.lang.Thread.sleep;
import static a75f.io.device.serial.MessageType.HYPERSTAT_CM_TO_CCU_SERIALIZED_MESSAGE;
import static a75f.io.device.serial.MessageType.MYSTAT_REGULAR_UPDATE_MESSAGE;
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
import a75f.io.device.connect.ConnectModbusSerialComm;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.mesh.MeshNetwork;
import a75f.io.device.mesh.Pulse;
import a75f.io.device.modbus.ModbusNetwork;
import a75f.io.device.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.SmartNodeSensorReading_t;
import a75f.io.domain.api.Domain;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil;
import a75f.io.logic.util.bacnet.BacnetUtilKt;
import a75f.io.logic.watchdog.WatchdogMonitor;
import a75f.io.usbserial.SerialAction;
import a75f.io.usbserial.SerialEvent;
import kotlin.Triple;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class DeviceUpdateJob extends BaseJob implements WatchdogMonitor
{
    DeviceNetwork deviceNw;
    ModbusNetwork modbusNetwork;
    boolean watchdogMonitor = false;
    private static int modbusDiagInterval = 5; // So that first time execution is possible
    private static final int MODBUS_DIAG_INTERVAL_COUNT = 5; // 5 min
    
    private Lock jobLock = new ReentrantLock();
    private DiagUpdateJob diagUpdateJob;
    private List<String> connectNodeList = null;
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
        deviceNw = new MeshNetwork();
        modbusNetwork = new ModbusNetwork();
    
        diagUpdateJob = new DiagUpdateJob();
        diagUpdateJob.scheduleJob("diagUpdateJob", Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                        .getInt("control_loop_frequency",60), 45, TimeUnit.SECONDS);

        //TODO - TEMP code for performance testing to simulate device load. Remove this code after performance issue resolved
       //injectTestInputMessage(1);
    }

    public void doJob()
    {
        watchdogMonitor = false;
//        ConnectModbusSerialComm.testReadOp();
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
                        ConnectModbusSerialComm.sendSettingConfig();
                        ConnectModbusSerialComm.sendControlsMessage(Domain.connect1Device);
                        ConnectModbusSerialComm.getRegularUpdate();
                    } else {
                        CcuLog.e(L.TAG_CCU_DEVICE, "Connect module not available");
                    }
                    if(ConnectNodeUtil.Companion.isConnectNodeAvailable()) {
                        connectNodeList = ConnectNodeUtil.Companion.getConnectNodeAddressList(CCUHsApi.getInstance());

                        // Read the diagnostic info from the connect module every MODBUS_DIAG_INTERVAL_COUNT minutes(5 min)
//                        if (modbusDiagInterval % MODBUS_DIAG_INTERVAL_COUNT == 0) {
                            modbusDiagInterval = 0;
                            // Get the connect node list
                            for (String nodeAddress : connectNodeList) {
                                // Extract the LSB which is a slave address of the node
                                List<Triple<String, String, String>> myTriple = ConnectNodeUtil.Companion.retrievePointSlaveIdRegAddr(
                                        ConnectNodeUtil.Companion.connectNodeEquip(Integer.parseInt(nodeAddress)).getId()
                                );

                                // Output sorted list
//                                for (Triple<String, String, String> triple : myTriple) {
//                                    CcuLog.d(L.TAG_CCU_JOB, String.valueOf(triple));
//                                }
                                // Check for empty list
                                if (!myTriple.isEmpty() && myTriple.get(0) != null) {
                                    ConnectModbusSerialComm.getPointInfo(myTriple, (int) myTriple.size(), Integer.parseInt(nodeAddress) % 100);
                                }
                                // Update the diagnostic info for the connect node
                                ConnectModbusSerialComm.getDiagnosticInfo(Integer.parseInt(nodeAddress) % 100);
                                CcuLog.d(L.TAG_CCU_JOB, "Connected Node: " + nodeAddress);
                            }
//                        }
                        modbusDiagInterval++;
                    } else {
                        CcuLog.e(L.TAG_CCU_DEVICE, "Connect node not available");
                    }
                    deviceNw.sendMessage();
                    deviceNw.sendSystemControl();
                    } else {
                        CcuLog.e(L.TAG_CCU_DEVICE, "Device update skipped , buildingProcess not running");
                    }
                    BacnetUtilKt.checkBacnetHealth();
                    BacnetUtilKt.updateBacnetMstpLinearAndCovSubscription(false);
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

    private void injectTestInputMessage(long initialDelaySeconds) {
        try {
            sleep(initialDelaySeconds * 40);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        CcuLog.i(L.TAG_CCU_DEVICE, "Injecting test messages devices "+CCUHsApi.getInstance().isCcuReady());
        new Thread(() -> {
            while (true) {
                List<HashMap<Object, Object>> hsDevices = CCUHsApi.getInstance().readAllEntities("device and hyperstat");
                CcuLog.i(L.TAG_CCU_DEVICE, "Injecting test messages for " + hsDevices.size() + " hyperstat devices");
                hsDevices.forEach(device -> {
                    injectTestRegularUpdateMessage(Integer.parseInt(device.get("addr").toString()), CCUHsApi.getInstance());
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        CcuLog.e(L.TAG_CCU_DEVICE, "error ", e);
                    }
                });

                List<HashMap<Object, Object>> snDevices = CCUHsApi.getInstance().readAllEntities("device and smartnode");
                CcuLog.i(L.TAG_CCU_DEVICE, "Injecting test messages for " + snDevices.size() + " smartnode devices");
                snDevices.forEach(device -> {
                    injectTestRegularUpdateMessageSmartNode(Integer.parseInt(device.get("addr").toString()), CCUHsApi.getInstance());
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        CcuLog.e(L.TAG_CCU_DEVICE, "error ", e);
                    }
                });

                try {
                    sleep(30000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                List<HashMap<Object, Object>> msDevices = CCUHsApi.getInstance().readAllEntities("device and mystat");
                CcuLog.i(L.TAG_CCU_DEVICE, "Injecting test messages for " + msDevices.size() + " mystat devices");
                msDevices.forEach(device -> {
                    injectTestMyStatRegularUpdateMessage(Integer.parseInt(device.get("addr").toString()), CCUHsApi.getInstance());
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        CcuLog.e(L.TAG_CCU_DEVICE, "error ", e);
                    }
                });

                injectRegularCMUpdate();
            }
        }).start();
    }
    private static void injectTestRegularUpdateMessage(int address, CCUHsApi hayStack) {

        Random randomNumGenerator = new Random();
        a75f.io.device.HyperStat.HyperStatRegularUpdateMessage_t regularUpdateMessage = HyperStat.HyperStatRegularUpdateMessage_t.newBuilder()
                .setRoomTemperature(650 + randomNumGenerator.nextInt(150))
                .setHumidity(200 + randomNumGenerator.nextInt(400))
                .setExternalThermistorInput1(randomNumGenerator.nextInt(20000))
                .setExternalThermistorInput2(randomNumGenerator.nextInt(20000))
                .setExternalAnalogVoltageInput1(randomNumGenerator.nextInt(10000))
                .setExternalAnalogVoltageInput2(randomNumGenerator.nextInt(10000))
                .setOccupantDetected(randomNumGenerator.nextBoolean())
                .setIlluminance(randomNumGenerator.nextInt(500))
                .addSensorReadings(HyperStat.SensorReadingPb_t.newBuilder().setSensorType(5).setSensorData(randomNumGenerator.nextInt(1000)))
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


    private static void injectTestMyStatRegularUpdateMessage(int address, CCUHsApi hayStack) {

        Random randomNumGenerator = new Random();

        MyStat.MyStatRegularUpdateCommon_t regularUpdateMessageCommand = MyStat.MyStatRegularUpdateCommon_t.newBuilder()
                .setRoomTemperature(780)
                .setHumidity(200 + randomNumGenerator.nextInt(400))
                .setOccupantDetected(true)
                .build();

        MyStat.MyStatRegularUpdateMessage_t regularUpdateMessage = MyStat.MyStatRegularUpdateMessage_t.newBuilder()
                .setRegularUpdateCommon(regularUpdateMessageCommand)
                .setUniversalInput1Value(56000)
                .setDoorWindowSensor(15000)
                .build();

        int FIXED_INT_BYTES_SIZE = 4;
        byte[] dataBytes = regularUpdateMessage.toByteArray();
        byte[] msgBytes = new byte[dataBytes.length + FIXED_INT_BYTES_SIZE * 4 + 1];
        msgBytes[0] = (byte)MYSTAT_REGULAR_UPDATE_MESSAGE.ordinal();

        //Network requires un-encoded node address occupying the first 4 bytes
        System.arraycopy(getByteArrayFromInt(address), 0, msgBytes, 1, FIXED_INT_BYTES_SIZE);

        //Network requires un-encoded message type occupying the next 4 bytes
        System.arraycopy(getByteArrayFromInt(MYSTAT_REGULAR_UPDATE_MESSAGE.ordinal()),
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

    private static void injectTestRegularUpdateMessageSmartNode(int address, CCUHsApi hayStack) {
        Random randomNumGenerator = new Random();
        CmToCcuOverUsbSnRegularUpdateMessage_t msg = new CmToCcuOverUsbSnRegularUpdateMessage_t();
        msg.update.smartNodeAddress.set(address);
        msg.update.roomTemperature.set(65+randomNumGenerator.nextInt(10));
        msg.update.externalAnalogVoltageInput1.set(randomNumGenerator.nextInt(10000));
        msg.update.externalThermistorInput1.set(randomNumGenerator.nextInt(10000));
        msg.update.externalAnalogVoltageInput2.set(randomNumGenerator.nextInt(10000));

        SmartNodeSensorReading_t sensorReadingHumidity = new SmartNodeSensorReading_t();
        sensorReadingHumidity.sensorType.set(1);//humidity
        sensorReadingHumidity.sensorData.set(randomNumGenerator.nextInt(1000));
        msg.update.sensorReadings[0] = sensorReadingHumidity;

        SmartNodeSensorReading_t sensorReadingCO2 = new SmartNodeSensorReading_t();
        sensorReadingCO2.sensorType.set(2);//CO2
        sensorReadingCO2.sensorData.set(400+randomNumGenerator.nextInt(1000));
        msg.update.sensorReadings[1] = sensorReadingCO2;

        SmartNodeSensorReading_t sensorReadingCO = new SmartNodeSensorReading_t();
        sensorReadingCO.sensorType.set(3);//CO
        sensorReadingCO.sensorData.set(randomNumGenerator.nextInt(200));
        msg.update.sensorReadings[2] = sensorReadingCO;

        SmartNodeSensorReading_t sensorReadingNO = new SmartNodeSensorReading_t();
        sensorReadingNO.sensorType.set(5);//VOC
        sensorReadingNO.sensorData.set(randomNumGenerator.nextInt(200));
        msg.update.sensorReadings[3] = sensorReadingNO;
        Pulse.regularSNUpdate(msg);
    }

    private static void injectRegularCMUpdate() {
        Random randomNumGenerator = new Random();
        CmToCcuOverUsbCmRegularUpdateMessage_t msg = new CmToCcuOverUsbCmRegularUpdateMessage_t();
        msg.roomTemperature.set(650 + randomNumGenerator.nextInt(150));
        msg.humidity.set((short) randomNumGenerator.nextInt(100));
        msg.thermistor1.set((short) randomNumGenerator.nextInt(10000));
        msg.thermistor2.set((short) randomNumGenerator.nextInt(10000));
        msg.analogSense1.set((short) randomNumGenerator.nextInt(10000));
        msg.analogSense2.set((short) randomNumGenerator.nextInt(10000));

        Pulse.regularCMUpdate(msg);
    }
}
