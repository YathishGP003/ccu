package a75f.io.logic;

import android.util.Log;

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.Zone;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;

import static a75f.io.logic.L.ccu;
import static a75f.io.logic.LLog.Logd;

/**
 * Created by Yinten isOn 8/24/2017.
 */
//0x11 0x00
class MeshUpdateJob extends BaseJob
{
    
    public static final String TAG = "HeartBeatJob";
    
    
    //This task should run every minute.
    public void doJob()
    {
        Logd("MeshUpdateJob running");
        if (LSerial.getInstance().isConnected())
        {
            for (Floor floor : ccu().getFloors())
            {
                for (Zone zone : floor.mRoomList)
                {
                    for (CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage : zone.getSeedMessages(EncryptionPrefs
                                                                                                          .getEncryptionKey()))
                    {
                        LSerial.getInstance()
                               .sendSerialStructToNode((short) seedMessage.smartNodeAddress
                                                                       .get(), seedMessage);
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
}

