package a75f.io.logic;

import android.content.Context;

import org.javolution.io.Struct;

import java.util.ArrayList;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.Node;
import a75f.io.bo.building.Output;
import a75f.io.bo.building.Schedulable;
import a75f.io.bo.building.Schedule;
import a75f.io.bo.building.SingleStageProfile;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.OverrideType;

import static a75f.io.logic.LZoneProfile.isNamedSchedule;

/**
 * Created by Yinten isOn 9/4/2017.
 */

public class L
{
    
    
    
    public static Context app()
    {
        return Globals.getInstance().getApplicationContext();
    }
    
    
    /****
     *
     * @return
     */
    public static CCUApplication ccu()
    {
        return Globals.getInstance().ccu();
    }
    
    
    /****
     *
     * @return
     */
    public static byte[] getEncryptionKey()
    {
        return EncryptionPrefs.getEncryptionKey();
    }
    
    
    /****
     *
     * @return
     */
    public static byte[] getFirmwareSignatureKey()
    {
        return EncryptionPrefs.getFirmwareSignatureKey();
    }
    
    
    /****
     *
     * @return
     */
    public static byte[] getBLELinkKey()
    {
        return EncryptionPrefs.getBLELinkKey();
    }
    
    
    public static short generateSmartNodeAddress()
    {
        return LSmartNode.nextSmartNodeAddress();
    }
    
    
    public static Zone findZoneByName(String mFloorName, String mRoomName)
    {
        return ZoneBLL.findZoneByName(mFloorName, mRoomName);
    }
    
    
    public static void sendLightControlsMessage(Zone zone)
    {
        //TODO: revist
        //ZoneBLL.sendControlsMessage(zone);
    }
    
    
    public static void addZoneProfileToZone(Node node, Zone zone, LightProfile mLightProfile)
    {
        saveCCUState();
    }
    
    
    public static void saveCCUState()
    {
        LocalStorage.setApplicationSettings();
        sync();
    }
    
    
    ////Schedules////
    
    
    
    
    
    private static void sync()
    {
        //seed all ccus
        //send settings messages
        //send controls messages
    }
    
    
    public static boolean isSimulation()
    {
        return Globals.getInstance().isSimulation();
    }
    
    
    public static void sendTestMessage(short nodeAddress, Zone zone)
    {
        ArrayList<Struct> messages = LSmartNode.getTestMessages(zone);
        for (Struct message : messages)
        {
            MeshUpdateJob.sendStruct(nodeAddress, message);
        }
    }
    
    
    public static int resolveZoneProfileLogicalValue(ZoneProfile profile)
    {
        return LZoneProfile.resolveZoneProfileLogicalValue(profile);
    }
    
    
    public static int resolveZoneProfileLogicalValue(ZoneProfile profile, Output snOutput)
    {
        return LZoneProfile.resolveZoneProfileLogicalValue(profile, snOutput);
    }
    
    
    public static ArrayList<Schedule> resolveSchedules(Schedulable schedulable)
    {
        if(isNamedSchedule(schedulable))
        {
            return ccu().getLCMNamedSchedules().get(schedulable.getNamedSchedule()).getSchedule();
        }
        else if(schedulable.hasSchedules())
        {
            return schedulable.getSchedules();
        }
        
        return null;
    }
    
    public static Object resolveTuningParameter(Zone zone, String key)
    {
        if (zone.getTuningParameters().containsKey(key))
        {
            return zone.getTuningParameters().get(key);
        }
        else
        {
            return ccu().getDefaultCCUTuners().get(key);
        }
    }
    
    
    public static void forceOverride(Zone zone, ZoneProfile zoneProfile, float desireTemp)
    {
        zoneProfile.setOverride(System.currentTimeMillis() + (int)resolveTuningParameter(zone,
                SingleStageProfile.Tuners.SSE_FORCED_OCCU_TIME), OverrideType.RELEASE_TIME,
                (short)Math.round(desireTemp * 2));
    }
    
    
    public static boolean isOccupied(Zone zone, ZoneProfile zoneProfile)
    {
        return false;
    }
}
