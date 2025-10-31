package a75f.io.device.mesh;

import static a75f.io.device.mesh.DLog.LogdStructAsJson;
import static a75f.io.device.mesh.MeshUtil.sendStructToCM;
import static a75f.io.device.serial.MessageType.CCU_TO_CM_MODBUS_SERVER_REGULAR_UPDATE_SETTINGS;
import static a75f.io.device.serial.MessageType.CM_TO_CCU_OVER_USB_MODBUS_SERVER_REGULAR_UPDATE;
import static a75f.io.device.serial.MessageType.PCN_MODBUS_SERVER_REBOOT;
import static a75f.io.logic.L.ccu;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.javolution.io.Struct;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.device.cm.ControlMoteMessageHandlerKt;
import a75f.io.device.connect.ConnectModbusSerialComm;
import a75f.io.device.mesh.hypersplit.HyperSplitMessageSender;
import a75f.io.device.mesh.hypersplit.HyperSplitMsgReceiver;
import a75f.io.device.mesh.hyperstat.HyperStatMessageSender;
import a75f.io.device.mesh.hyperstat.HyperStatMsgReceiver;
import a75f.io.device.mesh.mystat.MyStatMsgReceiverKt;
import a75f.io.device.mesh.mystat.MyStatMsgSender;
import a75f.io.device.modbus.ModbusPulse;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnSettings2Message_t;
import a75f.io.device.serial.CmToCcuOtaStatus_t;
import a75f.io.device.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbFirmwarePacketRequest_t;
import a75f.io.device.serial.CmToCcuOverUsbFirmwareUpdateAckMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSmartStatLocalControlsOverrideMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSmartStatRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnLocalControlsOverrideMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.PcnRebootIndicationMessage_t;
import a75f.io.device.serial.SnRebootIndicationMessage_t;
import a75f.io.device.serial.WrmOrCmRebootIndicationMessage_t;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.usbserial.SerialAction;
import a75f.io.usbserial.SerialEvent;
import a75f.io.usbserial.UsbConnectService;
import a75f.io.usbserial.UsbModbusService;
import a75f.io.usbserial.UsbService;

/**
 * Created by Yinten isOn 8/21/2017.
 */

public class LSerial
{
    private static LSerial    mLSerial;
    private        UsbService mUsbService;
    private UsbModbusService  mUsbModbusService;
    private UsbConnectService mUsbConnectService;
    private static boolean mSendSeedMsgs;
    private static boolean isNodeSeeding;
    private static boolean mWritePcnUpdate = false;

    /***
     * D
     * Node 1001 -
     * 	"CcuToCmOverUsbSnControlsMessage_t", 12345
     * 	"CcuToCmOverUsbDatabaseSeedSnMessage_t", 12333
     * Where 12345 & 12333 are Arrays.hashCode(byte[])
     */
    private HashMap<Short, HashMap<String, Integer>> structs =
            new HashMap<Short, HashMap<String, Integer>>();


    /***
     * Default empty constructor for a singleton.
     */
    private LSerial()
    {
    }


    public static LSerial getInstance()
    {
        if (mLSerial == null)
        {
            mLSerial = new LSerial();
            mSendSeedMsgs = true;
        }
        return mLSerial;
    }



    public boolean isReseedMessage(){
        if(mSendSeedMsgs)
            return true;
        else
            return false;
    }

    public void setResetSeedMessage(boolean bSeedMsg){
        CcuLog.d(L.TAG_CCU_DEVICE,"setResetSeedMessage "+bSeedMsg);
        mSendSeedMsgs = bSeedMsg;
    }

    public boolean isWritePcnUpdate(){
        return mWritePcnUpdate;
    }

    public void setWritePcnUpdate(boolean writePcnUpdate){
        mWritePcnUpdate = writePcnUpdate;
    }

    /***
     * Handles all incoming messages from the CM.   It will parse them and
     * determine where they should be sent. It will also broadcast the events as an Intent
     * so that any external handlers can receive them.
     *
     * Logs to logcat.
     *
     * @param context The caller's Context, which will be used to broadcast the event to external handlers
     * @param event The serial event from the CM
     */

    public static void handleSerialEvent(Context context, SerialEvent event)
    {
        DLog.LogdSerial("Serial Event Type: " + event.getSerialAction().name());
        if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_PORT)
        {
            byte[] data = event.getBytes();
            MessageType messageType = MessageType.values()[(event.getBytes()[0] & 0xff)];
            DLog.LogdSerial("Received Event Type: " + messageType.name());
            if (messageType == MessageType.CM_REGULAR_UPDATE)
            {
                Pulse.regularCMUpdate(fromBytes(data, CmToCcuOverUsbCmRegularUpdateMessage_t.class));
            }
            else if (messageType == MessageType.CM_TO_CCU_OVER_USB_SN_REGULAR_UPDATE)
            {
                if (data.length == 43) {
                    Pulse.regularSNUpdate(fromBytes((modifyByteArray(data)), CmToCcuOverUsbSnRegularUpdateMessage_t.class));
                } else {
                    Pulse.regularSNUpdate(fromBytes((data), CmToCcuOverUsbSnRegularUpdateMessage_t.class));
                }
            }
            else if (messageType == MessageType.CM_TO_CCU_OVER_USB_SMART_STAT_REGULAR_UPDATE)
            {
                DLog.LogdSerial("Event Type: " + data.length+","+data.toString());
                Pulse.regularSmartStatUpdate(fromBytes(data, CmToCcuOverUsbSmartStatRegularUpdateMessage_t.class));
            }
            else if (messageType == MessageType.CM_TO_CCU_OVER_USB_SN_SET_TEMPERATURE_UPDATE)
            {
                DLog.LogdSerial("Event Type:updateSetTempFromSmartNode="+data.length+","+data.toString());
                Pulse.updateSetTempFromSmartNode(fromBytes(data, CmToCcuOverUsbSnLocalControlsOverrideMessage_t.class));;
            }
            else if(messageType == MessageType.FSV_REBOOT)
            {
                DLog.LogdSerial("Event Type DEVICE_REBOOT:"+data.length+","+data.toString());
                Pulse.rebootMessageFromCM(fromBytes(data, WrmOrCmRebootIndicationMessage_t.class));
            }
            else if(messageType == MessageType.CM_TO_CCU_OVER_USB_SMART_STAT_LOCAL_CONTROLS_OVERRIDE)
            {
                DLog.LogdSerial("Event Type:CM_TO_CCU_OVER_USB_SMART_STAT_LOCAL_CONTROLS_OVERRIDE="+data.length+","+data.toString());
                Pulse.updateSetTempFromSmartStat(fromBytes(data, CmToCcuOverUsbSmartStatLocalControlsOverrideMessage_t.class));
            } else if (messageType == MessageType.CM_TO_CCU_OVER_USB_FIRMWARE_UPDATE_ACK) {
                CmToCcuOverUsbFirmwareUpdateAckMessage_t msg = new CmToCcuOverUsbFirmwareUpdateAckMessage_t();
                msg.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
                LogdStructAsJson(msg);
            } else if (messageType == MessageType.CM_TO_CCU_OVER_USB_FIRMWARE_PACKET_REQUEST) {
                CmToCcuOverUsbFirmwarePacketRequest_t msg = new CmToCcuOverUsbFirmwarePacketRequest_t();
                msg.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
                LogdStructAsJson(msg);
            }else if(messageType == MessageType.CM_TO_CCU_OVER_USB_CM4_REGULAR_UPDATE){
                ControlMoteMessageHandlerKt.handleCMRegularUpdate(data);
            }else if(messageType == MessageType.CM_TO_CCU_OVER_USB_SN_REBOOT){
                DLog.LogdSerial("Event Type CM_TO_CCU_OVER_USB_SN_REBOOT DEVICE_REBOOT:"+data.length+","+data.toString());
                Pulse.smartDevicesRebootMessage(fromBytes(data, SnRebootIndicationMessage_t.class));
                Pulse.rebootMessageFromCM(fromBytes(data, WrmOrCmRebootIndicationMessage_t.class));
            } else if(messageType == PCN_MODBUS_SERVER_REBOOT) {
                DLog.LogdSerial("Event Type PCN_MODBUS_SERVER_REBOOT DEVICE_REBOOT:"+data.length+","+data.toString());
                Pulse.pcnDevicesRebootMessage(fromBytes(data, PcnRebootIndicationMessage_t.class));
                Pulse.rebootMessageFromCM(fromBytes(data, WrmOrCmRebootIndicationMessage_t.class));
            }

            // HyperStat and HyperSplit message are both sent to CCU through HyperStatCmToCcuSerializedMessage.
            // If a HyperStatCmToCcuSerializedMessage is received, call both the HyperStat and HyperSplit handler methods.
            // The handler methods then inspect the message contents and filter out messages of the incorrect type.
            else if (messageType == MessageType.HYPERSTAT_CM_TO_CCU_SERIALIZED_MESSAGE) {
                HyperStatMsgReceiver.processMessage(data, CCUHsApi.getInstance());
                HyperSplitMsgReceiver.processMessage(data, CCUHsApi.getInstance());
                MyStatMsgReceiverKt.processMessage(data);
                // Decode serialized PCN message
                LPcn.processMessage(data);
            }

            else if (isHyperStatMessage(messageType) ) {
                HyperStatMsgReceiver.processMessage(data, CCUHsApi.getInstance());
            } else if (isHyperSplitMessage(messageType)) {
                HyperSplitMsgReceiver.processMessage(data, CCUHsApi.getInstance());
            } else if (isMyStatMessage(messageType)) {
                MyStatMsgReceiverKt.processMessage(data);
            }
            else if (messageType == MessageType.CM_TO_CCU_OTA_STATUS) {
                CmToCcuOtaStatus_t msg = new CmToCcuOtaStatus_t();
                msg.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
                DLog.LogdSerial("Event Type CM_TO_CCU_OTA_STATUS Status :" +msg.currentState+ " Data : "+msg.data);

            } else if (messageType == MessageType.CM_TO_CCU_OVER_USB_CM_SERIAL_REGULAR_UPDATE) {
                ControlMoteMessageHandlerKt.handleCMRegularUpdate(data);
            } else if (messageType == MessageType.MODBUS_MESSAGE) {
                try {
                    byte[] modbusData = Arrays.copyOfRange(data, 1, data.length);
                    ConnectModbusSerialComm.handleModbusResponse(modbusData);
                } catch (Exception e) {
                    CcuLog.e(L.TAG_CCU_DEVICE, "Error handling modbus message !!! ", e);
                }
            } else if(CM_TO_CCU_OVER_USB_MODBUS_SERVER_REGULAR_UPDATE == messageType) {
                LPcn.handlePcnRegularUpdateSettings(data);
            }

            // Pass event to external handlers
            if (messageType == MessageType.CM_TO_CCU_OVER_USB_FIRMWARE_UPDATE_ACK ||
                    messageType == MessageType.CM_TO_CCU_OVER_USB_FIRMWARE_PACKET_REQUEST ||
                    messageType == MessageType.CM_TO_CCU_OTA_STATUS ||
                    messageType == MessageType.CM_TO_CCU_OVER_USB_SN_REBOOT ||
                    messageType == MessageType.PCN_MODBUS_SERVER_REBOOT ||
                    messageType == MessageType.CM_TO_CCU_OVER_USB_SEQUENCE_PACKET_REQUEST ||
                    messageType == MessageType.CM_TO_CCU_OVER_USB_SEQUENCE_OTA_STATUS ||
                    messageType == MessageType.CM_ERROR_REPORT) {
                Intent eventIntent = new Intent(Globals.IntentActions.LSERIAL_MESSAGE_OTA);
                eventIntent.putExtra("eventType", messageType);
                eventIntent.putExtra("eventBytes", data);
                context.sendBroadcast(eventIntent);
            }
        }else if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_MODBUS) {
            byte[] data = event.getBytes();
            ModbusPulse.handleModbusPulseData(data, (event.getBytes()[0] & 0xff));
        } else if (event.getSerialAction() == SerialAction.MESSAGE_FROM_CM_CONNECT_PORT) {
            byte[] data = event.getBytes();
            ConnectModbusSerialComm.handleModbusResponse(data);
        }
    }


    /***
     * Construct the Struct based on a class type.   It will log message type, data return size,
     * incoming hexadecimal, and json.
     * This is a lot of parsing, so it should only be used for
     * @param data
     * @param pojoClass
     * @param <T>
     * @return
     */
    public static <T extends Struct> T fromBytes(byte[] data, Class<T> pojoClass)
    {
        T struct = null;
        try
        {
            struct = pojoClass.newInstance();
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        struct.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
        DLog.Logd("Message Type: " + pojoClass.getSimpleName());
        DLog.Logd("Data return size: " + data.length);

        //Log hexadecimal
        try {
            DLog.Logd("Incoming Hexadecimal: " + struct.toString());
        } catch (IndexOutOfBoundsException e) {
            DLog.Logd("Can't log " + pojoClass.getSimpleName() + "; received message is shorter than struct stored in CCU");
        }

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            LogdStructAsJson(struct);
        return struct;
    }


    public boolean isConnected()
    {
        if (mUsbService == null)
        {
            return false;
        }
        return mUsbService.isConnected();
    }
    public boolean isModbusConnected()
    {
        if (mUsbModbusService == null)
        {
            return false;
        }
        return mUsbModbusService.isConnected();
    }

    public boolean isConnectModuleConnected()
    {
        if (mUsbConnectService == null)
        {
            return false;
        }
        return mUsbConnectService.isConnected();
    }


    /***
     * This is the setter method for the USB Service.
     *
     * All the members of BaseSerialAppCompatActivity are private and shouldn't be used.   The
     * only place the usbService should be interacted with is through the LSerial.
     *
     * This will be help when we move onto the state machines.
     *
     * The structs need to be cleared because when the USB reconnects this method is called.
     *
     * @param usbService
     */
    public void setUSBService(UsbService usbService)
    {
        structs.clear();
        mUsbService = usbService;
    }
    public void setModbusUSBService(UsbModbusService modbusUSBService)
    {
        structs.clear();
        mUsbModbusService = modbusUSBService;
    }

    public void setUsbConnectService(UsbConnectService usbService)
    {
        structs.clear();
        mUsbConnectService = usbService;
    }



    /***
     * This method will handle all  messages being sent to the Nodes.
     * This method is synchronized with sendSerialToCM, so a heartbeat doesn't write in the
     * middle of a smartnode update or another thread isn't writing to the struct at the same
     * time.
     * Logs to logcat.
     *
     * @param struct This is a representation of a C Struct, loggable to hexadecimal and JSON.
     *                  It is a convience to deal with ByteOrder and following interface
     *                  documentation isOn Sharepoint.
     * @return success If serial was open and the usbService was successfully able to try to send
     * to CM without Android stopping it.  It doesn't nessacarily mean any messages went to
     * either the CM or the Node.
     *
     */

    public synchronized boolean sendSerialStructToNode(short smartNodeAddress, Struct struct)
    {

        Integer structHash = Arrays.hashCode(struct.getOrderedBuffer());
        if (checkDuplicate(Short.valueOf(smartNodeAddress), struct.getClass()
                .getSimpleName(), structHash))
        {
            //DLog.LogdStructAsJson(struct);
            DLog.Logd("Struct " + struct.getClass().getSimpleName() + " was already sent, returning");
            return false;
        }
        if (mUsbService == null)
        {
            DLog.logUSBServiceNotInitialized();
            return false;
        }
        LogdStructAsJson(struct);
        mUsbService.write(struct.getOrderedBuffer());
        return true;
    }

    public synchronized boolean sendSerialBytesToNodes(short smartNodeAddress, byte[] data)
    {

        Integer structHash = Arrays.hashCode(data);
        if (checkDuplicate(Short.valueOf(smartNodeAddress), "byteArray", structHash))
        {
            //DLog.LogdStructAsJson(struct);
            DLog.Logd("byte array was already sent, returning");
            return false;
        }
        if (mUsbService == null)
        {
            DLog.logUSBServiceNotInitialized();
            return false;
        }
        mUsbService.write(data);
        return true;
    }

    /***
     * Compare of struct before sending packets to CM
     *
     * @param struct This is a representation of a C Struct, loggable to hexadecimal and JSON.
     *                  It is a convience to deal with ByteOrder and following interface
     *                  documentation isOn Sharepoint.
     * @return success If serial was open and the usbService was successfully able to try to send
     * to CM without Android stopping it.  It doesn't nessacarily mean any messages went to
     * either the CM or the Node.
     *
     */

    public synchronized boolean compareStructSendingToNode(short smartNodeAddress, Struct struct)
    {
        
        Integer structHash = Arrays.hashCode(struct.getOrderedBuffer());
        if (checkDuplicate(Short.valueOf(smartNodeAddress), struct.getClass()
                .getSimpleName(), structHash))
        {
            DLog.Logd("Struct " + struct.getClass().getSimpleName() + " was already sent, returning");
            return true;
        }
        return false;
    }
    /***
     * This method will handle all  messages being sent to the CM.  Do not use this method when
     * dealing with structures that are supposed to go to the Smart Stat or Smart Node
     *
     * Logs to logcat.
     *
     * @param struct This is a representation of a C Struct, loggable to hexadecimal and JSON.
     *                  It is a convience to deal with ByteOrder and following interface
     *                  documentation isOn Sharepoint.
     * @return success If serial was open and the usbService was successfully able to try to send
     * to CM without Android stopping it.  It doesn't nessacarily mean any messages went to
     * either the CM or the Node.
     *
     */

    public synchronized boolean sendSerialToNodes(Struct struct)
    {

        if (mUsbService == null)
        {
            DLog.logUSBServiceNotInitialized();
            return false;
        }
        LogdStructAsJson(struct);
        mUsbService.write(struct.getOrderedBuffer());
        return true;
    }
    /***
     * This method will handle all  messages being sent to the CM.  Do not use this method when
     * dealing with structures that are supposed to go to the Smart Stat or Smart Node
     *
     * Logs to logcat.
     *
     * @param struct This is a representation of a C Struct, loggable to hexadecimal and JSON.
     *                  It is a convience to deal with ByteOrder and following interface
     *                  documentation isOn Sharepoint.
     * @return success If serial was open and the usbService was successfully able to try to send
     * to CM without Android stopping it.  It doesn't nessacarily mean any messages went to
     * either the CM or the Node.
     *
     */

    public synchronized boolean sendSerialToCM(Struct struct)
    {

        if (mUsbService == null)
        {
            DLog.logUSBServiceNotInitialized();
            return false;
        }

        LogdStructAsJson(struct);
        mUsbService.write(struct.getOrderedBuffer());
        return true;
    }
    public synchronized boolean sendSerialToModbus(byte[] data)
    {

        if (mUsbModbusService == null)
        {
            DLog.logUSBServiceNotInitialized();
            return false;
        }

        mUsbModbusService.modbusWrite(data);
        return true;
    }
    
    public synchronized boolean sendSerialBytesToCM(byte[] data)
    {
        
        if (mUsbService == null) {
            DLog.logUSBServiceNotInitialized();
            return false;
        }
        
        mUsbService.write(data);
        return true;
    }

    public synchronized boolean sendSerialBytesToConnect(byte[] data)
    {
        if (mUsbConnectService == null) {
            CcuLog.d(L.TAG_CCU_DEVICE, "sendSerialBytesToConnect Failed");
            DLog.logUSBServiceNotInitialized();
            return false;
        }

        mUsbConnectService.write(data);
        return true;
    }

    /***
     *  This method maintains the hash, if it returns false, proceed without needing to add extra
     *  data to the hash.
     * @param smartNodeAddress The address of where the struct is going
     * @param simpleName The simple name of the struct
     * @param structHash The hash value of the byte array of the struct
     * @return if it is a duplicate
     */
    private boolean checkDuplicate(Short smartNodeAddress, String simpleName, Integer
            structHash)
    {
        if (structs.containsKey(smartNodeAddress))
        {
            HashMap<String, Integer> stringIntegerHashMap = structs.get(smartNodeAddress);
            if (stringIntegerHashMap.containsKey(simpleName))
            {
                Integer previousHash = stringIntegerHashMap.get(simpleName);
                if (previousHash.equals(structHash))
                {
                    DLog.Logd("Struct was already sent, returning");
                    return true;
                }
            }
        }
        else
        {
            structs.put(Short.valueOf(smartNodeAddress), new HashMap<String, Integer>());
        }
        structs.get(Short.valueOf(smartNodeAddress)).put(simpleName, structHash);
        return false;
    }


    public boolean sendSerialStructToNodeWithoutHashCheck(short smartNodeAddress, Struct struct)
    {
        mUsbService.write(struct.getOrderedBuffer());
        return true;
    }

    public boolean isNodesSeeding(){
        return isNodeSeeding;
    }
    public void setNodeSeeding(boolean isSeeding){
        isNodeSeeding = isSeeding;
    }
    public void sendSeedMessage(boolean isSs, boolean isTi, Short addr, String roomRef, String floorRef ){
        if(isConnected()) {
            isNodeSeeding = true;
            Pulse.sendSeedMessage(isSs, isTi, addr, roomRef, floorRef);
        }
    }

    public void sendOAOSeedMessage(){
        if(isConnected()) {
            LSerial.getInstance().setNodeSeeding(true);
            CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING OAO SN SEED Message =====================");
            CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = LSmartNode.getSeedMessage(new Zone.Builder().setDisplayName("OAO").build(),
                    (short)L.ccu().oaoProfile.getNodeAddress(), ccu().oaoProfile.getEquipRef(),"oao");
            sendStructToCM(seedMessage);
            CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING OAO SN SETTING2 Message =====================");
            CcuToCmOverUsbSnSettings2Message_t settings2Message = LSmartNode.getSettings2Message(new Zone.Builder().setDisplayName("OAO").build()
                    , (short)L.ccu().oaoProfile.getNodeAddress(), ccu().oaoProfile.getEquipRef(), "oao");
            sendStructToCM(settings2Message);
            LSerial.getInstance().setNodeSeeding(false);
        }
    }

    public void sendBypassSeedMessage(){
        if(isConnected()) {
            LSerial.getInstance().setNodeSeeding(true);
            CcuLog.d(L.TAG_CCU_DEVICE, "=================NOW SENDING BYPASS DAMPER SN SEED Message =====================");
            CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = LSmartNode.getSeedMessage(new Zone.Builder().setDisplayName("BYPASS DAMPER").build(),
                    (short) ccu().bypassDamperProfile.getNodeAddr(), ccu().bypassDamperProfile.getEquipRef(),"bypass");
            sendStructToCM(seedMessage);
            CcuLog.d("CCU_SN_MESSAGES", "=================NOW SENDING SN Settings2=====================");
            CcuToCmOverUsbSnSettings2Message_t settings2Message = LSmartNode.getSettings2Message(new Zone.Builder().setDisplayName("BYPASS DAMPER").build(),
                    (short) ccu().bypassDamperProfile.getNodeAddr(), ccu().bypassDamperProfile.getEquipRef(), "bypass");
            sendStructToCM(settings2Message);
            LSerial.getInstance().setNodeSeeding(false);
        }
    }

    public void sendMyStatSeedMessage(Short addr, String roomRef, String floorRef) {
        if (isConnected()) {
            isNodeSeeding = true;
            CcuLog.d(L.TAG_CCU_DEVICE,
                    "=================NOW SEEDING NEW PROFILE=====================" + addr + "," + roomRef);
            Device d = HSUtil.getDevice(addr);
            Zone zone = HSUtil.getZone(roomRef, floorRef);
            MyStatMsgSender.INSTANCE.sendSeedMessage(zone.getDisplayName(), Integer.parseInt(d.getAddr()),
                    d.getEquipRef(), false);
            LSerial.getInstance().setNodeSeeding(false);
        }
    }

    public void sendHyperStatSeedMessage(Short addr, String roomRef, String floorRef, boolean isVRV) {
        if (isConnected()) {
            isNodeSeeding = true;
            CcuLog.d(L.TAG_CCU_DEVICE,
                     "=================NOW SEEDING NEW PROFILE=====================" + addr + "," + roomRef);
            Device d = HSUtil.getDevice(addr);
            Zone zone = HSUtil.getZone(roomRef, floorRef);
            if (isVRV){
                HyperStatMessageSender.sendIduSeedSetting(zone.getDisplayName(), Integer.parseInt(d.getAddr()),
                        d.getEquipRef(), false);
            } else {
                HyperStatMessageSender.sendSeedMessage(zone.getDisplayName(), Integer.parseInt(d.getAddr()),
                        d.getEquipRef(), false);
            }
            LSerial.getInstance().setNodeSeeding(false);
        }
    }

    public void sendHyperSplitSeedMessage(Short addr, String roomRef, String floorRef) {
        if (isConnected()) {
            isNodeSeeding = true;
            CcuLog.d(L.TAG_CCU_DEVICE,
                    "=================NOW SEEDING NEW PROFILE=====================" + addr + "," + roomRef);
            Device d = HSUtil.getDevice(addr);
            Zone zone = HSUtil.getZone(roomRef, floorRef);
            HyperSplitMessageSender.sendSeedMessage(zone.getDisplayName(), Integer.parseInt(d.getAddr()),
                    d.getEquipRef(), false);

            HyperSplitMessageSender.sendSettings3Message(Integer.parseInt(d.getAddr()),
                    d.getEquipRef(), false);

            HyperSplitMessageSender.sendSettings4Message(Integer.parseInt(d.getAddr()),
                    d.getEquipRef(), false);

            LSerial.getInstance().setNodeSeeding(false);
        }
    }
    
    private static boolean isHyperStatMessage(MessageType messageType) {
        return messageType == MessageType.HYPERSTAT_REGULAR_UPDATE_MESSAGE ||
               messageType == MessageType.HYPERSTAT_LOCAL_CONTROLS_OVERRIDE_MESSAGE;
    }

    private static boolean isHyperSplitMessage(MessageType messageType) {
        return messageType == MessageType.HYPERSPLIT_REGULAR_UPDATE_MESSAGE ||
                messageType == MessageType.HYPERSTAT_LOCAL_CONTROLS_OVERRIDE_MESSAGE;
    }

    private static boolean isMyStatMessage(MessageType messageType) {
        return messageType == MessageType.MYSTAT_REGULAR_UPDATE_MESSAGE ||
                messageType == MessageType.MYSTAT_LOCAL_CONTROLS_OVERRIDE_MESSAGE;
    }

    private static byte[] modifyByteArray(byte[] currentData) {

        /*
         * New 2 fields are introduced as part of True CFM feature at position 42, 43
         * As it is introduced at middle it is not backward compatible so adding 2 additional bytes for new fields
         *
         */
        byte[] modifiedData = new byte[45];
        System.arraycopy(currentData, 0, modifiedData, 0, 43);
        modifiedData[43] = modifiedData[41]; // moving cmLqi as they are shifted
        modifiedData[44] = modifiedData[42]; // RSSI
        modifiedData[41] = 127; // clear feedback points as they have wrong values
        modifiedData[42] = 127; //
        DLog.Logd("Converted Byte array "+Arrays.toString(modifiedData));
        return modifiedData;
    }

}
