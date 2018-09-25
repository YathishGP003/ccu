package a75f.io.device.mesh;

import android.util.Log;

import a75f.io.device.DeviceNetwork;
import a75f.io.device.json.serializers.JsonSerializer;
import a75f.io.device.serial.AddressedStruct;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Floor;
import a75f.io.logic.bo.building.Zone;

import static a75f.io.device.DeviceConstants.TAG;
import static a75f.io.device.mesh.MeshUtil.sendStruct;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class MeshNetwork extends DeviceNetwork
{
    @Override
    public void sendMessage() {
        Log.d(TAG, "MeshNetwork SendMessage");
    
        if (!LSerial.getInstance().isConnected()) {
            Log.d(TAG,"Device not connected !!");
            return;
        }
        
        MeshUtil.sendHeartbeat((short)0);
        
        MeshUtil.tSleep(1000);
        
        try
        {
            for (Floor floor : L.ccu().getFloors())
            {
                for (Zone zone : floor.mRoomList)
                {
                    DLog.Logw("=============Zone: " + zone.roomName + " ==================");
                    DLog.Logw("=================NOW SENDING SEEDS=====================");
                    for (CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage : LSmartNode.getSeedMessages(zone))
                    {
                        if (sendStruct((short) seedMessage.smartNodeAddress.get(), seedMessage))
                        {
                            Log.w(DLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
                        }
                    }
                    DLog.Logw("=================NOW SENDING CONTROLS=====================");
                    for (CcuToCmOverUsbSnControlsMessage_t controlsMessage : LSmartNode.getControlMessages(zone))
                    {
                        if (sendStruct((short) controlsMessage.smartNodeAddress.get(), controlsMessage))
                        {
                            Log.w(DLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
                        }
                    }
                    DLog.Logw("=================NOW SENDING EXTRA MESSAGES LIKE SCHEDULES====================");
                    for (AddressedStruct extraMessage : LSmartNode.getExtraMessages(floor, zone))
                    {
                        if (sendStruct(extraMessage.getAddress(), extraMessage.getStruct()))
                        {
                            Log.w(DLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
                        }
                    }
                }
            }
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
