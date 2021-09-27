package a75f.io.logic;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.bo.building.CCUApplication;
import a75f.io.logic.bo.building.Node;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.Schedulable;
import a75f.io.logic.bo.building.Schedule;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.lights.LightProfile;

/**
 * Created by Yinten isOn 9/4/2017.
 */

public class L
{
    public static final String TAG_CCU = "CCU";
    public static final String TAG_CCU_UI = "CCU_UI";
    public static final String TAG_CCU_ZONE = "CCU_ZONE";
    public static final String TAG_CCU_SYSTEM = "CCU_SYSTEM";
    public static final String TAG_CCU_JOB = "CCU_JOB";
    public static final String TAG_CCU_HS = "CCU_HS";
    public static final String TAG_CCU_OAO = "CCU_OAO";
    public static final String TAG_CCU_DEVICE = "CCU_DEVICE";
    public static final String TAG_CCU_SERIAL = "CCU_SERIAL";
    public static final String TAG_CCU_SCHEDULER = "CCU_SCHEDULER";
    public static final String TAG_CCU_PUBNUB = "CCU_PUBNUB";
    public static final String TAG_CCU_WARN = "CCU_WARN";
    public static final String TAG_CCU_MODBUS = "CCU_MODBUS";
    public static final String TAG_CCU_TUNER = "CCU_TUNER";
    public static final String TAG_CCU_MESSAGING = "CCU_MESSAGING";

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


    public static boolean isModbusSlaveIdExists(Short slaveId) {
        ArrayList<HashMap> nodes = CCUHsApi.getInstance().readAll("device and modbus");
        if(nodes.size() == 0)
            return false;

        for (HashMap node : nodes)
        {
            if (node.get("addr").toString().equals(String.valueOf(slaveId))) {
                return true;
            }
        }
        return false;
    }
    public static List<String> getExistingModbusSlaveIds() {
        List<String> existingSlaveIds = new ArrayList<String>();
        ArrayList<HashMap> nodes = CCUHsApi.getInstance().readAll("device and modbus");
        if(nodes.size() == 0)
            return existingSlaveIds;

        for (HashMap node : nodes)
        {
            existingSlaveIds.add(node.get("addr").toString());
        }
        return existingSlaveIds;
    }
    public static short generateSmartNodeAddress()
    {
        short currentBand = L.ccu().getSmartNodeAddressBand();
        ArrayList<HashMap> nodes = CCUHsApi.getInstance().readAll("device and node");
        if (nodes.size() == 0) {
            return currentBand;
        }

        boolean addrUsed = true;
        short nextAddr = currentBand;
        while (addrUsed)
        {
            for (HashMap node : nodes)
            {
                if (node.get("addr").toString().equals(String.valueOf(nextAddr))) {
                    nextAddr++;
                    addrUsed = true;
                    break;
                } else {
                    addrUsed = false;
                }
            }
        }
        return nextAddr;
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
        //LocalStorage.setApplicationSettings();
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
        /*ArrayList<Struct> messages = LSmartNode.getTestMessages(zone);
        for (Struct message : messages)
        {
            MeshUpdateJob.sendStruct(nodeAddress, message);
        }*/
    }


    public static float resolveZoneProfileLogicalValue(ZoneProfile profile)
    {
        return LZoneProfile.resolveZoneProfileLogicalValue(profile);
    }

    public static float getDesiredTemp(ZoneProfile profile)
    {
        for (short node : profile.getNodeAddresses())
        {
            ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and sp and group == \"" + node + "\"");
            String id = ((HashMap)points.get(0)).get("id").toString();
            if (id == null || id == "") {
                throw new IllegalArgumentException();
            }
            float desiredTemp = CCUHsApi.getInstance().readDefaultValById(id).floatValue();
            //Log.d("CCU", "DesiredTemp : "+desiredTemp);
            return desiredTemp;
        }
        return 0;
    }

    public static void setDesiredTemp(double desiredTemp, ZoneProfile profile)
    {
        for (short node : profile.getNodeAddresses())
        {
            ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and sp and group == \"" + node + "\"");
            String id = ((HashMap) points.get(0)).get("id").toString();
            if (id == null || id == "")
            {
                throw new IllegalArgumentException();
            }
            Log.d("CCU", "Set DesiredTemp : "+desiredTemp);
            CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
            CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
        }
    }


    public static float resolveZoneProfileLogicalValue(ZoneProfile profile, Output snOutput)
    {
        return LZoneProfile.resolveZoneProfileLogicalValue(profile, snOutput);
    }


    public static ArrayList<Schedule> resolveSchedules(Schedulable schedulable)
    {
        /*if (isNamedSchedule(schedulable))
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
        }*/
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


    public static boolean isTestHarness()
    {
        return Globals.getInstance().testHarness();
    }


    public static boolean isSimulation()
    {
        return Globals.getInstance().isSimulation();
    }




    public static void forceOverride(Zone zone, ZoneProfile zoneProfile, float desireTemp)
    {
        /*zoneProfile.setOverride(System.currentTimeMillis() +
                                (int) resolveTuningParameter(zone, AlgoTuningParameters.SSETuners.SSE_FORCED_OCCU_TIME) *
                                60 * 1000, OverrideType.RELEASE_TIME, (short) Math.round(
                desireTemp * 2));*/
    }


    public static Object resolveTuningParameter(Zone zone, String key)
    {
        if (zone.getTuningParameters().containsKey(key))
        {
            return zone.getTuningParameters().get(key);
        }
        else
        {
            return null;//TODO - ccu().getDefaultCCUTuners().get(key);
        }
    }


    public static boolean isOccupied(Zone zone, ZoneProfile zoneProfile)
    {
        return false;
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

    public static void removeHSDeviceEntities(Short node) {
        CCUHsApi hsApi = CCUHsApi.getInstance();
        if (L.ccu().oaoProfile != null && L.ccu().oaoProfile.getNodeAddress() == node) {
            L.ccu().oaoProfile = null;
        }
        HashMap equip = hsApi.read("equip and group == \""+node+"\"");
        if (equip.get("id") != null)
        {
            hsApi.deleteEntityTree(equip.get("id").toString());
        }
        HashMap device = hsApi.read("device and addr == \""+node+"\"");
        if (device.get("id") != null)
        {
            hsApi.deleteEntityTree(device.get("id").toString());
        }
        removeProfile(node);
    }

    public static ZoneProfile getProfile(short addr) {
        Log.d("CCU","Profiles "+L.ccu().zoneProfiles.size());

        for (Iterator<ZoneProfile> it = L.ccu().zoneProfiles.iterator(); it.hasNext();)
        {
            ZoneProfile p = it.next();
            Log.d("CCU","Profile "+p.getProfileType());
            for (Short node : p.getNodeAddresses()) {
                if (node == addr) {
                    return p;
                }
            }

        }
        Log.d("CCU","Profile Not found for "+addr);
        return null;
    }

    public static void removeProfile(short addr) {
        ZoneProfile deleteProfile = null;
        for(ZoneProfile p : L.ccu().zoneProfiles) {
            for (Short node : p.getNodeAddresses()) {
                if (node == addr) {
                    deleteProfile = p;
                    break;
                }
            }
        }
        if (deleteProfile != null)
        {
            L.ccu().zoneProfiles.remove(deleteProfile);
        }
    }

    public static synchronized void pingCloudServer() {
        final ConnectivityManager connMgr = (ConnectivityManager) Globals.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        Log.d("CCU_CLOUDSTATUS", "Ping cloud server");
        if (netInfo != null && netInfo.isConnected()) {
            //  Some sort of connection is open, check if server is reachable
            SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext());
            spDefaultPrefs.edit().putBoolean("75fNetworkAvailable", true).commit();
        }
        else {
            SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext());
            spDefaultPrefs.edit().putBoolean("75fNetworkAvailable", false).commit();
        }
    }
}
