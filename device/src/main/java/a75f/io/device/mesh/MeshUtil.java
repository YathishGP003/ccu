package a75f.io.device.mesh;

import org.javolution.io.Struct;

import a75f.io.device.serial.CcuToCmOverUsbCcuHeartbeatMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;

import static a75f.io.device.DeviceConstants.HEARTBEAT_INTERVAL;
import static a75f.io.device.DeviceConstants.HEARTBEAT_MULTIPLIER;
import static a75f.io.device.DeviceConstants.SIMULATION_SLEEP_TIME;

import android.util.Log;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class MeshUtil
{

    //SmartNode Reset constants
    public static final int POWER_ON_RESET = 0x01;
    public static final int CORE_BROWNOUT_RESET = 0x02;
    public static final int VDD_BROWNOUT_RESET = 0x04;
    public static final int EXTERNAL_RESET = 0x10;
    public static final int WATCHDOG_RESET = 0x20;
    public static final int SOFTWARE_RESET = 0x40;
    public static final int BACKUP_RESET = 0x80;
    
    public static final int RELAY_BITMAP_POS_Y1 = 0;
    public static final int RELAY_BITMAP_POS_Y2 = 1;
    public static final int RELAY_BITMAP_POS_G1 = 2;
    public static final int RELAY_BITMAP_POS_G2 = 3;
    public static final int RELAY_BITMAP_POS_W1 = 4;
    public static final int RELAY_BITMAP_POS_W2 = 5;
    public static final int RELAY_BITMAP_POS_AUX = 6;
    
    
    public static boolean sendHeartbeat(short temperatureOffset)
    {
        CcuToCmOverUsbCcuHeartbeatMessage_t heartbeatMessage_t = new CcuToCmOverUsbCcuHeartbeatMessage_t();
        heartbeatMessage_t.interval.set(HEARTBEAT_INTERVAL);
        heartbeatMessage_t.messageType.set(MessageType.CCU_HEARTBEAT_UPDATE);
        heartbeatMessage_t.multiplier.set(HEARTBEAT_MULTIPLIER);
        heartbeatMessage_t.temperatureOffset.set((byte) temperatureOffset);
    
        boolean retVal = LSerial.getInstance().sendSerialToCM(heartbeatMessage_t);
        return retVal;
    }

    public static boolean checkDuplicateStruct(short smartNodeAddress, Struct struct)
    {
        return  LSerial.getInstance().compareStructSendingToNode(smartNodeAddress, struct);
    }
    public static boolean sendStructToNodes(Struct struct)
    {
        boolean retVal = LSerial.getInstance().sendSerialToNodes(struct);
        //If the application is in simualtion mode to work over FTDI with biskit,
        // sleep between messages, so biskit doesn't fall behind.
        if (Globals.getInstance().isSimulation())
        {
            tSleep(SIMULATION_SLEEP_TIME);
        }
        return retVal;
    }
    
    public static boolean sendStruct(short smartNodeAddress, Struct struct)
    {
        boolean retVal = LSerial.getInstance().sendSerialStructToNode(smartNodeAddress, struct);
        //If the application is in simualtion mode to work over FTDI with biskit,
        // sleep between messages, so biskit doesn't fall behind.
        if (Globals.getInstance().isSimulation())
        {
            tSleep(SIMULATION_SLEEP_TIME);
        }
        return retVal;
    }
    
    public static boolean sendStructToCM(Struct struct)
    {
        Log.i(Globals.TAG, "sendStructToCM: sendSerialToCM sending data to mesh point ");
        boolean retVal = LSerial.getInstance().sendSerialToCM(struct);
        //If the application is in simualtion mode to work over FTDI with biskit,
        // sleep between messages, so biskit doesn't fall behind.
        if (Globals.getInstance().isSimulation())
        {
            tSleep(SIMULATION_SLEEP_TIME);
        }
        return retVal;
    }
    
    public static boolean sendStructTest(short smartNodeAddress, Struct struct)
    {
        boolean retVal = LSerial.getInstance().sendSerialStructToNodeWithoutHashCheck
                                                       (smartNodeAddress, struct);
        
        //If the application is in simualtion mode to work over FTDI with biskit,
        // sleep between messages, so biskit doesn't fall behind.
        
        if (Globals.getInstance().isSimulation())
        {
            tSleep(SIMULATION_SLEEP_TIME);
        }
        return retVal;
    }
    
    public static void tSleep(int sleepTime)
    {
        CcuLog.d(L.TAG_CCU_DEVICE, "sleeping: " + sleepTime);
        try
        {
            Thread.sleep(sleepTime);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    
    /**
     * Relays on CM are not mapped in logical order.
     * HardCode mapping position according label on CM
     *   ->  Y1, Y2 , G1, W1, W2, G2  <-
     *
     */
    public static int getRelayMapping(int relayPos) {
        switch (relayPos) {
            case 1:
                return RELAY_BITMAP_POS_Y1;
            case 2:
                return RELAY_BITMAP_POS_Y2;
            case 3:
                return RELAY_BITMAP_POS_G1;
            case 4:
                return RELAY_BITMAP_POS_W1;
            case 5:
                return RELAY_BITMAP_POS_W2;
            case 6:
                return RELAY_BITMAP_POS_G2;
            case 7:
                return RELAY_BITMAP_POS_AUX;
        }
        return relayPos;
    }
}
