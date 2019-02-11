package a75f.io.device.mesh;

import android.util.Log;

import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.device.DeviceNetwork;
import a75f.io.device.json.serializers.JsonSerializer;
import a75f.io.device.serial.AddressedStruct;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.haystack.device.ControlMote;

import static a75f.io.device.DeviceConstants.TAG;
import static a75f.io.device.mesh.MeshUtil.sendStruct;

/**
 * Created by samjithsadasivan on 9/19/18.
 */

public class MeshNetwork extends DeviceNetwork
{
    @Override
    public void sendMessage() {
        Log.d(TAG, "MeshNetwork SendNodeMessage");
    
        if (!LSerial.getInstance().isConnected()) {
            Log.d(TAG,"Device not connected !!");
            return;
        }
        
        MeshUtil.sendHeartbeat((short)0);
        
        MeshUtil.tSleep(1000);
        
        try
        {
            for (Floor floor : HSUtil.getFloors())
            {
                for (Zone zone : HSUtil.getZones(floor.getId()))
                {
                    DLog.Logw("=============Zone: " + zone.getDisplayName() + " ==================");
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
                        Log.w(DLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
                        if (sendStruct(extraMessage.getAddress(), extraMessage.getStruct()))
                        {
                            //Log.w(DLog.UPDATED_ZONE_TAG, JsonSerializer.toJson(zone, true));
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
    
    public void sendSystemControl() {
        Log.d(TAG, "MeshNetwork SendSystemControl");
    
        if (!LSerial.getInstance().isConnected()) {
            Log.d(TAG,"Device not connected !!");
            return;
        }
        
        if (L.ccu().systemProfile == null) {
            Log.d(TAG, "MeshNetwork SendSystemControl : Abort , No system profile");
            return;
        }
        if (!(L.ccu().systemProfile instanceof DefaultSystem))
        {
            L.ccu().systemProfile.doSystemControl();
        }
    
        CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
        msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_CM_RELAY_ACTIVATION);
        msg.analog0.set((short) ControlMote.getAnalogOut("analog1"));
        msg.analog1.set((short) ControlMote.getAnalogOut("analog2"));
        msg.analog2.set((short) ControlMote.getAnalogOut("analog3"));
        msg.analog3.set((short) ControlMote.getAnalogOut("analog4"));
        int relayBitmap = 0;
    
        for (int i = 1; i <=7 ;i++)
        {
            if (ControlMote.getRelayState("relay"+i) > 0) {
                relayBitmap |= 1 << (i-1);
            }
        }
        msg.relayBitmap.set((short)relayBitmap);
        //DLog.LogdStructAsJson(msg);
        MeshUtil.sendStructToCM(msg);
        
    }
}
