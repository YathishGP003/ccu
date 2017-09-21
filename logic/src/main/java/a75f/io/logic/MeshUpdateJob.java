package a75f.io.logic;

import android.util.Log;

import org.javolution.io.Struct;

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.Zone;
import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;

import static a75f.io.logic.L.ccu;
import static a75f.io.logic.LLog.Logd;
import static a75f.io.logic.LLog.Logw;

/**
 * Created by Yinten isOn 8/24/2017.
 */
//0x11 0x00
class MeshUpdateJob extends BaseJob
{
    
    public static final String TAG                 = "HeartBeatJob";
    private static final int SIMULATION_SLEEP_TIME = 100;
    
    
    //This task should run every minute.
    public void doJob()
    {
        try
        {
            Logw("MeshUpdateJob running");
            if (LSerial.getInstance().isConnected())
            {
                for (Floor floor : ccu().getFloors())
                {
                    for (Zone zone : floor.mRoomList)
                    {
                        Logw("=============Zone: " + zone.roomName + " ==================");
                        Logw("=================NOW SENDING SEEDS=====================");
                        for (CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage : zone.getSeedMessages(EncryptionPrefs
                                                                                                              .getEncryptionKey()))
                        {
                            if(sendStruct((short) seedMessage.smartNodeAddress.get(), seedMessage))
                            {
                                Log.w(LLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
                            }
                        }
                        Logw("=================NOW SENDING CONTROLS=====================");
                        for (CcuToCmOverUsbSnControlsMessage_t controlsMessage : zone.getControlsMessages())
                        {
                            if(sendStruct((short) controlsMessage.smartNodeAddress.get(),
                                    controlsMessage))
                            {
                                Log.w(LLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
                            }
                        }
                    }
                }
                //Foreach smart node, send seed messages & controls messages.   TODO worry about
                // duplciates.
            }
            else
            {
                Log.d(TAG, "Serial is not connected, rescheduling heartbeat");
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
    private boolean sendStruct(short smartNodeAddress, Struct struct)
    {
        boolean retVal = LSerial.getInstance()
               .sendSerialStructToNode(smartNodeAddress, struct);
    
        //If the application is in simualtion mode to work over FTDI with biskit,
        // sleep between messages, so biskit doesn't fall behind.
        if(Globals.getInstance().isSimulation())
        {
            tSleep(SIMULATION_SLEEP_TIME);
        }
        return retVal;
    }
    
    
    private void tSleep(int sleepTime)
    {
        Logd("sleeping: " + sleepTime);
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

