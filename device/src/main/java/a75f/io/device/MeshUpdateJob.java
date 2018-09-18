package a75f.io.device;

import org.javolution.io.Struct;

import a75f.io.logic.Globals;

/**
 * Created by Yinten isOn 8/24/2017.
 */
//0x11 0x00
class MeshUpdateJob extends BaseJob
{
    
    public static final  String TAG                   = "HeartBeatJob";
    public static final int    SIMULATION_SLEEP_TIME = 100;
    
    //This task should run every minute.
    public void doJob()
    {
        /*try
        {
            LLog.Logw("MeshUpdateJob running");
            if (LSerial.getInstance().isConnected())
            {
                for (Floor floor : L.ccu().getFloors())
                {
                    for (Zone zone : floor.mRoomList)
                    {
                        LLog.Logw("=============Zone: " + zone.roomName + " ==================");
                        LLog.Logw("=================NOW SENDING SEEDS=====================");
                        for (CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage : LSmartNode.getSeedMessages(zone))
                        {
                            if (sendStruct((short) seedMessage.smartNodeAddress.get(), seedMessage))
                            {
                                Log.w(LLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
                            }
                        }
                        LLog.Logw("=================NOW SENDING CONTROLS=====================");
                        for (CcuToCmOverUsbSnControlsMessage_t controlsMessage : LSmartNode.getControlMessages(zone))
                        {
                            if (sendStruct((short) controlsMessage.smartNodeAddress.get(), controlsMessage))
                            {
                                Log.w(LLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
                            }
                        }
                        LLog.Logw("=================NOW SENDING EXTRA MESSAGES LIKE SCHEDULES====================");
                        for (AddressedStruct extraMessage : LSmartNode.getExtraMessages(floor, zone))
                        {
                            if (sendStruct(extraMessage.getAddress(), extraMessage.getStruct()))
                            {
                                Log.w(LLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
                            }
                        }
                    }
                }
                
                LLog.Logw("=================NOW SENDING SYSTEM CONTROL MESSAGE ====================");
                //sendStructToCM(LSystem.getSystemControlMsg());
            
            }
            else
            {
                Log.d(TAG, "Serial is not connected, rescheduling heartbeat");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }*/
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
    
    private static void tSleep(int sleepTime)
    {
        LLog.Logd("sleeping: " + sleepTime);
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

