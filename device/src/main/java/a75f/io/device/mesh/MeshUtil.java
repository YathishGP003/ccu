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

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class MeshUtil
{
    
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
}
