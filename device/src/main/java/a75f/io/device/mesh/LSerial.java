package a75f.io.device.mesh;

import android.os.Build;
import android.content.Context;
import android.content.Intent;

import org.javolution.io.Struct;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;

import a75f.io.device.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSmartStatLocalControlsOverrideMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSmartStatRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnLocalControlsOverrideMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.WrmOrCmRebootIndicationMessage_t;
import a75f.io.logic.Globals;
import a75f.io.usbserial.SerialAction;
import a75f.io.usbserial.SerialEvent;
import a75f.io.usbserial.UsbService;

/**
 * Created by Yinten isOn 8/21/2017.
 */

public class LSerial
{
    private static LSerial    mLSerial;
    private        UsbService mUsbService;
    private static boolean mSendSeedMsgs;

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
        mSendSeedMsgs =bSeedMsg;
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
        DLog.LogdSerial("Event Type: " + event.getSerialAction().name());
        if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_PORT)
        {
            byte[] data = event.getBytes();
            MessageType messageType = MessageType.values()[(event.getBytes()[0] & 0xff)];
            if (messageType == MessageType.CM_REGULAR_UPDATE)
            {
                Pulse.regularCMUpdate(fromBytes(data, CmToCcuOverUsbCmRegularUpdateMessage_t.class));
            }
            else if (messageType == MessageType.CM_TO_CCU_OVER_USB_SN_REGULAR_UPDATE)
            {
                Pulse.regularSNUpdate(fromBytes(data, CmToCcuOverUsbSnRegularUpdateMessage_t.class));
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
            }

            // Pass event to external handlers
            Intent eventIntent = new Intent(Globals.IntentActions.LSERIAL_MESSAGE);
            eventIntent.putExtra("eventType", messageType);
            eventIntent.putExtra("eventBytes", data);

            context.sendBroadcast(eventIntent);
            //context.startService(eventIntent);
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
        DLog.Logd("Incoming Hexadecimal: " + struct.toString());
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            DLog.LogdStructAsJson(struct);
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

        //Only if the struct was wrote to serial should it be logged.
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            DLog.LogdStructAsJson(struct);
        mUsbService.write(struct.getOrderedBuffer());
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
            //DLog.LogdStructAsJson(struct);
            DLog.Logd("Struct " + struct.getClass().getSimpleName() + " was already sent, returning");
            return true;
        }

        //Only if the struct was wrote to serial should it be logged.
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            DLog.LogdStructAsJson(struct);
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
        //Only if the struct was wrote to serial should it be logged.
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            DLog.LogdStructAsJson(struct);
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

        //Only if the struct was wrote to serial should it be logged.
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            DLog.LogdStructAsJson(struct);

        mUsbService.write(struct.getOrderedBuffer());
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
}
