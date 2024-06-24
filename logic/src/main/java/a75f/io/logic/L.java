package a75f.io.logic;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.bo.building.CCUApplication;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.util.RxTask;

/**
 * Created by Yinten isOn 9/4/2017.
 */

public class L
{
    public static final String TAG_CCU = "CCU";
    public static final String TAG_CCU_INIT = "CCU_INIT";
    public static final String TAG_CCU_UI = "CCU_UI";
    public static final String TAG_CCU_ZONE = "CCU_ZONE";
    public static final String TAG_CCU_SYSTEM = "CCU_SYSTEM";
    public static final String TAG_CCU_JOB = "CCU_JOB";
    public static final String TAG_CCU_HS = "CCU_HS";
    public static final String TAG_CCU_OAO = "CCU_OAO";
    public static final String TAG_CCU_DEVICE = "CCU_DEVICE";
    public static final String TAG_CCU_SERIAL = "CCU_SERIAL";
    public static final String TAG_CCU_SCHEDULER = "CCU_SCHEDULER";
    public static final String TAG_CCU_PUBNUB = "CCU_MESSAGING";
    public static final String TAG_CCU_MODBUS = "CCU_MODBUS";
    public static final String TAG_CCU_TUNER = "CCU_TUNER";
    public static final String TAG_CCU_BACKUP = "CCU_BACKUP";
    public static final String TAG_CCU_REPLACE = "CCU_REPLACE";
    public static final String TAG_CCU_HSHST = "CCU_HST";
    public static final String TAG_CCU_HSCPU = "CCU_HSCPU";
    public static final String TAG_CCU_HSPIPE2 = "CCU_HSPIPE2";
    public static final String TAG_CCU_HSHPU = "CCU_HSHPU";
    public static final String TAG_CCU_HSSPLIT_CPUECON = "CCU_HSSPLIT_CPUECON";
    public static final String TAG_CCU_MESSAGING = "CCU_MESSAGING";
    public static final String TAG_CCU_WEATHER = "CCU_WEATHER";
    public static final String TAG_CCU_MIGRATION_UTIL = "MIGRATION_UTIL";
    public static final String TAG_OTN = "CCU_OTN";
    public static final String TAG_CCU_BLE = "CCU_BLE";
    public static final String TAG_CCU_READ_CHANGES = "CCU_READ_CHANGES";
    public static final String TAG_CCU_OTA_PROCESS= "OTA_PROCESS";
    public static final String TAG_CCU_AUTO_COMMISSIONING = "CCU_AUTO_COMMISSIONING";
    public static final String TAG_CCU_UPDATE = "CCU_UPDATE";
    public static final String TAG_SCHEDULABLE = "CCU_SCHEDULABLE";
    public static final String TAG_DESIRED_TEMP_MODE = "DESIRED_TEMP_MODE";
    public static final String TAG_CCU_BACNET = "CCU_BACNET";
    public static final String TAG_CCU_DOWNLOAD = "CCU_DOWNLOAD";
    public static final String CCU_REMOTE_ACCESS = "CCU_REMOTE_ACCESS";
    public static final String TAG_CCU_DR_MODE = "CCU_DR_MODE";

    public static final String TAG_CCU_SERIAL_CONNECT = "CCU_SERIAL_CONNECT";
    public static final String TAG_CCU_ERROR = "CCU_ERROR";
    public static final String TAG_CCU_SCHEDULE = "CCU_SCHEDULE";
    public static final String TAG_CCU_MASTER_CONTROL = "CCU_MASTER_CONTROL";
    public static final String TAG_CCU_TUNERS_UI = "CCU_TUNERS_UI";
    public static final String TAG_CCU_RANGE_CONTROL = "CCU_RANGE_CONTROL";
    public static final String TAG_CCU_MOVEMENT = "CCU_MOVEMENT";
    public static final String TAG_CCU_VAV_TEMP = "CCU_VAV_TEMP";
    public static final String TAG_CCU_ADD_EXISTING = "CCU_ADD_EXISTING";
    public static final String TAG_CCU_WIFI = "CCU_WIFI";
    public static final String TAG_CCU_REGISTER_GATHER_DETAILS = "CCU_REGISTER_GATHER_DETAILS";
    public static final String TAG_CCU_CRASH = "CCU_CRASH";
    public static final String TAG_CCU_SAFE_MODE = "CCU_SAFE_MODE";
    public static final String TAG_CCU_DM_MODBUS = "CCU_DM_MODBUS";
    public static Context app()
    {
        return Globals.getInstance().getApplicationContext();
    }


    public static byte[] getEncryptionKey()
    {
        return EncryptionPrefs.getEncryptionKey();
    }



    public static byte[] getFirmwareSignatureKey()
    {
        return EncryptionPrefs.getFirmwareSignatureKey();
    }



    public static byte[] getBLELinkKey()
    {
        return EncryptionPrefs.getBLELinkKey();
    }


    public static boolean isModbusSlaveIdExists(Short slaveId) {
        ArrayList<HashMap<Object, Object>> nodes = CCUHsApi.getInstance().readAllEntities("device and modbus");
        if(nodes.size() == 0)
            return false;

        for (HashMap<Object,Object> node : nodes)
        {
            if (node.get("addr").toString().equals(String.valueOf(slaveId))) {
                return true;
            }
        }
        return false;
    }
    
    public static short generateSmartNodeAddress()
    {
        short currentBand = L.ccu().getSmartNodeAddressBand();
        ArrayList<HashMap<Object,Object>> nodes = CCUHsApi.getInstance().readAllEntities("device and node");
        if (nodes.size() == 0) {
            return currentBand;
        }

        boolean addrUsed = true;
        short nextAddr = currentBand;
        while (addrUsed)
        {
            for (HashMap<Object,Object> node : nodes)
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
    

    public static void saveCCUState()
    {
        Globals.getInstance().saveTags();
    }
    

    public static float getDesiredTemp(ZoneProfile profile)
    {
        for (short node : profile.getNodeAddresses())
        {
            ArrayList<HashMap<Object, Object>> points = CCUHsApi.getInstance().readAllEntities("point and air and temp and desired and sp and group == \"" + node + "\"");
            String id = ((HashMap<?, ?>)points.get(0)).get("id").toString();
            if (id == null || id.equals("")) {
                throw new IllegalArgumentException();
            }
            return CCUHsApi.getInstance().readDefaultValById(id).floatValue();
        }
        return 0;
    }

    public static void setDesiredTemp(double desiredTemp, ZoneProfile profile)
    {
        for (short node : profile.getNodeAddresses())
        {
            ArrayList<HashMap<Object,Object>> points = CCUHsApi.getInstance().readAllEntities("point and air and temp and desired and sp and group == \"" + node + "\"");
            String id = ( points.get(0)).get("id").toString();
            if (id == null || id.equals(""))
            {
                throw new IllegalArgumentException();
            }
            CcuLog.d("CCU", "Set DesiredTemp : "+desiredTemp);
            CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
            CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
        }
    }

    public static CCUApplication ccu()
    {
        return Globals.getInstance().ccu();
    }


    public static boolean isSimulation()
    {
        return Globals.getInstance().isSimulation();
    }

    public static boolean isOccupied()
    {
        return false;
    }

    public static void saveCCUState(CCUApplication state)
    {
        Globals.getInstance().setCCU(state);
        saveCCUState();
    }


    public static void removeHSDeviceEntities(Short node) {
        CCUHsApi hsApi = CCUHsApi.getInstance();
        if (L.ccu().oaoProfile != null && L.ccu().oaoProfile.getNodeAddress() == node) {
            L.ccu().oaoProfile = null;
        }
        HashMap<Object, Object> equip = hsApi.readEntity("equip and group == \""+node+"\"");
        if (equip.get("id") != null)
        {
            hsApi.deleteEntityTree(equip.get("id").toString());
            
            if(equip.containsKey(Tags.OAO) &&
                            ccu().systemProfile.getProfileType() != ProfileType.SYSTEM_DEFAULT) {
                ccu().systemProfile.setOutsideTempCoolingLockoutEnabled(hsApi, false);
            }
        }
        HashMap<Object, Object> device = hsApi.readEntity("device and addr == \""+node+"\"");
        if (device.get("id") != null)
        {
            hsApi.deleteEntityTree(device.get("id").toString());
        }
        removeProfile(node);
    }

    public static ZoneProfile getProfile(short addr) {
        for (ZoneProfile p : L.ccu().zoneProfiles) {
            for (Short node : p.getNodeAddresses()) {
                if (node == addr) {
                    return p;
                }
            }

        }
        CcuLog.d("CCU","Profile Not found for "+addr);
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
        CcuLog.d("CCU_CLOUDSTATUS", "Ping cloud server");
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
    
    public static void saveCCUStateAsync() {
        RxTask.executeAsync(() -> Globals.getInstance().saveTags());
    }
}
