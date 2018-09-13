package a75f.io.logic;

import android.content.Context;
import android.util.Log;

import com.google.api.client.json.jackson2.JacksonFactory;

import org.javolution.io.Struct;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import a75f.io.logic.bo.building.CCUApplication;
import a75f.io.logic.bo.building.lights.LightProfile;
import a75f.io.logic.bo.building.Node;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.Schedulable;
import a75f.io.logic.bo.building.Schedule;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.OverrideType;
import a75f.io.logic.bo.building.definitions.ScheduleMode;
import a75f.io.kinveybo.AlgoTuningParameters;
import a75f.io.kinveybo.CCUUser;
import a75f.io.kinveybo.DalContext;

import static a75f.io.logic.JsonSerializer.fromJson;
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
        Globals.getInstance().saveTags();
    }


    private static void sync()
    {
        //seed all ccus
        //send settings messages
        //send controls messages
    }
    ////Schedules////


    public static void sendTestMessage(short nodeAddress, Zone zone)
    {
        ArrayList<Struct> messages = LSmartNode.getTestMessages(zone);
        for (Struct message : messages)
        {
            MeshUpdateJob.sendStruct(nodeAddress, message);
        }
    }


    public static float resolveZoneProfileLogicalValue(ZoneProfile profile)
    {
        return LZoneProfile.resolveZoneProfileLogicalValue(profile);
    }


    public static float resolveZoneProfileLogicalValue(ZoneProfile profile, Output snOutput)
    {
        return LZoneProfile.resolveZoneProfileLogicalValue(profile, snOutput);
    }


    public static ArrayList<Schedule> resolveSchedules(Schedulable schedulable)
    {
        if (isNamedSchedule(schedulable))
        {
            return ccu().getLCMNamedSchedules().get(schedulable.getNamedSchedule()).getSchedule();
        }
        else if (schedulable.hasSchedules())
        {
            return schedulable.getSchedules();
        }
        else if(schedulable.getScheduleMode() == ScheduleMode.SystemSchedule)
        {
            return ccu().getDefaultTemperatureSchedule();
        }
        return null;
    }


    /****
     *
     * @return
     */
    public static CCUApplication ccu()
    {
        return Globals.getInstance().ccu();
    }


    public static void setupTestUserIfNeeded()
    {
        if (isDeveloperTesting())
        {
            JacksonFactory jacksonFactory = new JacksonFactory();
            InputStream inputStream = null;
            try
            {
                inputStream =
                        Globals.getInstance().getApplicationContext().getAssets().open("User.json");
                CCUUser user = fromJson(inputStream, CCUUser.class);
                ccu().setUser(user);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else if (isSimulation())
        {
            //TODO: this should be in the simulation file.
            JacksonFactory jacksonFactory = new JacksonFactory();
            InputStream inputStream = null;
            try
            {
                inputStream =
                        Globals.getInstance().getApplicationContext().getAssets().open("User.json");
                CCUUser user = fromJson(inputStream, CCUUser.class);
                ccu().setUser(user);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }


    public static boolean isDeveloperTesting()
    {
        return Globals.getInstance().isDeveloperTesting();
    }


    public static boolean isSimulation()
    {
        return Globals.getInstance().isSimulation();
    }




    public static void forceOverride(Zone zone, ZoneProfile zoneProfile, float desireTemp)
    {
        zoneProfile.setOverride(System.currentTimeMillis() +
                                (int) resolveTuningParameter(zone, AlgoTuningParameters.SSETuners.SSE_FORCED_OCCU_TIME) *
                                60 * 1000, OverrideType.RELEASE_TIME, (short) Math.round(
                desireTemp * 2));
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


    public static boolean isOccupied(Zone zone, ZoneProfile zoneProfile)
    {
        return false;
    }


    public static AlgoTuningParameters getDefaultTuners()
    {

        Log.e("Tuners", "Get default tuners");
        AlgoTuningParameters algoTuningParameters = null;
        try
        {
            InputStream inputStream = Globals.getInstance().getApplicationContext().getAssets()
                                             .open("DefaultTuningParameters_v100.json");
            algoTuningParameters = fromJson(inputStream, AlgoTuningParameters.class);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return algoTuningParameters;
    }


    public static DalContext getKinveyClient()
    {
        return Globals.getInstance().getDalContext();
    }


    //TODO: implement clear caches.
    public static void clearCaches()
    {
    }


    /*
    User can exist and not be registered, user can exist and already be registered, just need
    login, or user can just be logged in.
     */
    public static boolean isUserRegistered()
    {
        return LocalStorage.getIsUserRegistered();
    }


    public static void setUserRegistered(boolean userRegistered)
    {
        LocalStorage.setIsUserRegistered(userRegistered);
    }
    public static void saveCCUState(CCUApplication state)
    {
        Globals.getInstance().setCCU(state);
        saveCCUState();
    }
    
    /*
    This should set a preference to what environment
    the user would like to use with Kinvey for testing and development purposes.
     */
    public static void setServerEnvironment(String serverEnvironment) {
        //L.serverEnvironment = serverEnvironment;
    }
}
