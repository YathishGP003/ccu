package a75f.io.logic;

import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_DEVICE_MAC_ADDR;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_SOURCE_ADDRESS;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.bo.building.CCUApplication;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.bacnet.BacnetProfile;
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.modbus.ModbusProfile;
import a75f.io.logic.bo.building.lowcode.LowCodeUtil;
import a75f.io.logic.bo.building.pcn.PCNUtil;
import a75f.io.util.ExecutorTask;

/**
 * Created by Yinten isOn 9/4/2017.
 */

public class L
{
    // External App package names
    public static final String BAC_APP_PACKAGE_NAME = "io.seventyfivef.bacapp";
    public static final String BAC_APP_PACKAGE_NAME_OBSOLETE = "com.example.ccu_bacapp";
    public static final String REMOTE_ACCESS_PACKAGE_NAME = "io.seventyfivef.remoteaccess";
    public static final String HOME_APP_PACKAGE_NAME = "com.x75frenatus.home";
    public static final String HOME_APP_PACKAGE_NAME_OBSOLETE = "io.seventyfivef.home";

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
    public static final String TAG_CCU_MSPIPE2 = "CCU_MSPIPE2";
    public static final String TAG_CCU_MSCPU = "CCU_MSCPU";
    public static final String TAG_CCU_MSHPU = "CCU_MSHPU";
    public static final String TAG_CCU_HSHPU = "CCU_HSHPU";
    public static final String TAG_CCU_MSHST = "CCU_MST";
    public static final String TAG_CCU_LOOP = "CCU_LOOP";
    public static final String TAG_CCU_HSSPLIT_CPUECON = "CCU_HSSPLIT_CPUECON";
    public static final String TAG_CCU_HSSPLIT_PIPE4_UV = "CCU_HSS_UVPIPE4";
    public static final String TAG_CCU_HSSPLIT_PIPE2_UV = "CCU_HSS_UVPIPE2";
    public static final String TAG_CCU_MESSAGING = "CCU_MESSAGING";
    public static final String TAG_CCU_COPY_CONFIGURATION = "CCU_COPY_CONFIGURATION";
    public static final String TAG_CCU_WEATHER = "CCU_WEATHER";
    public static final String TAG_CCU_MIGRATION_UTIL = "CCU_MIGRATION_UTIL";
    public static final String TAG_CCU_BLE = "CCU_BLE";
    public static final String TAG_CCU_READ_CHANGES = "CCU_READ_CHANGES";
    public static final String TAG_CCU_OTA_PROCESS= "CCU_OTA_PROCESS";
    public static final String TAG_CCU_AUTO_COMMISSIONING = "CCU_AUTO_COMMISSIONING";
    public static final String TAG_CCU_UPDATE = "CCU_UPDATE";
    public static final String TAG_SCHEDULABLE = "CCU_SCHEDULABLE";
    public static final String TAG_DESIRED_TEMP_MODE = "DESIRED_TEMP_MODE";
    public static final String TAG_CCU_BACNET = "CCU_BACNET";
    public static final String TAG_CCU_BACNET_MSTP = "CCU_BACNET_MSTP";
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
    public static final String TAG_CCU_CONNECTION_INFO = "CCU_CONNECTION_INFO";
    public static final String TAG_CCU_REMOTE_COMMAND = "CCU_REMOTE_COMMAND";
    public static final String TAG_CCU_CLOUD_STATUS = "CCU_CLOUD_STATUS";
    public static final String TAG_CCU_UI_PROFILING = "CCU_UI_PROFILING";
    public static final String TAG_CCU_DOMAIN = "CCU_DOMAIN";
    public static final String TAG_CCU_FILES = "CCU_FILES";
    public static final String TAG_ZONE_SCHEDULE_SPILL = "CCU_ZONE_SCHEDULE_SPILL";
    public static final String TAG_CCU_BUNDLE = "CCU_BUNDLE";

    public static final String TAG_CCU_LOGS_FILE_PATH = "CCU_LOGS_FILE_PATH";
    public static final String TAG_CCU_LOGS_FILE_ID = "CCU_LOGS_FILE_ID";
    public static final String TAG_SEQUENCER_LOGS_FILE_PATH = "CCU_SEQUENCER_LOGS_FILE_PATH";
    public static final String TAG_SEQUENCER_LOGS_FILE_ID = "CCU_SEQUENCER_LOGS_FILE_ID";
    public static final String TAG_ALERT_LOGS_FILE_PATH = "CCU_ALERT_LOGS_FILE_PATH";
    public static final String TAG_ALERT_LOGS_FILE_ID = "CCU_ALERT_LOGS_FILE_ID";

    public static final String TAG_CCU_LOGS = "CCU_LOGS";
    public static final String TAG_SEQUENCER_LOGS = "CCU_SEQUENCER_LOGS";
    public static final String TAG_ALERT_LOGS = "CCU_ALERT_LOGS";
    public static final String TAG_PRECONFIGURATION = "CCU_PRECONFIGURATION";
    public static final String TAG_CONNECT_NODE = "CCU_CONNECT_NODE";
    public static final String TAG_CCU_SEQUENCE_APPLY = "CCU_SEQUENCE_APPLY";
    public static final String TAG_REGISTRATION = "CCU_REGISTRATION";
    public static final String TAG_CCU_POINT_SCHEDULE = "CCU_POINT_SCHEDULE";
    public static final String TAG_PCN = "CCU_PCN";

    public static final String TAG_CCU_PROXY = "CCU_PROXY";
    public static final String TAG_CCU_MSPIPE4 = "CCU_MSPIPE4";



    public static final String TAG_USB_MANAGER = "CCU_USB_MANAGER";
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

    public static boolean modbusSlaveIdExistsInTheRoom(Short slaveId, String roomRef) {
        CcuLog.d(TAG_CCU_MODBUS, "Checking Modbus slave ID existence: " + slaveId + " in room: " + roomRef);
        CCUHsApi hsApi = CCUHsApi.getInstance();
        ArrayList<HashMap<Object, Object>> nodes = hsApi.readAllEntities("device and modbus and roomRef == \"" + roomRef + "\"");
        List<Integer> lowCodeSlaveIdList = LowCodeUtil.Companion.getLowCodeSlaveIdList(hsApi);
        if(nodes.isEmpty() && lowCodeSlaveIdList.isEmpty()){
            return false;
        }

        for (HashMap<Object,Object> node : nodes) {
            if (node.get("addr").toString().equals(String.valueOf(slaveId))) {
                return true;
            }
        }
        for (Integer node : lowCodeSlaveIdList) {
            if (node == slaveId.intValue()) {
                return true;
            }
        }
        CcuLog.d(TAG_CCU_MODBUS, "Modbus slave ID " + slaveId + " does not exist in room: " + roomRef);
        return false;
    }


    public static boolean isModbusSlaveIdExists(Short slaveId, String port) {
        CcuLog.d(TAG_CCU_MODBUS, "Checking Modbus slave ID existence: " + slaveId + " on port: " + port);
        CCUHsApi hsApi = CCUHsApi.getInstance();
        ArrayList<HashMap<Object, Object>> nodes = hsApi.readAllEntities("device and modbus");
        List<Integer> lowCodeSlaveIdList = LowCodeUtil.Companion.getLowCodeSlaveIdList(hsApi);
        if(nodes.isEmpty() && lowCodeSlaveIdList.isEmpty()){
            return false;
        }

        for (HashMap<Object,Object> node : nodes) {
            if (node.get("addr").toString().equals(String.valueOf(slaveId))) {
                if (port.isEmpty()) {
                    return true;
                }
                try {
                    String equipRef = node.get("equipRef").toString();
                    HashMap<Object, Object> equip = hsApi.readEntity("equip and id == " + equipRef);
                    CcuLog.d(TAG_CCU_MODBUS, "Found equip for node " + node + ": " + equip);
                    Object modbusPort = equip.get("port");
                    if (modbusPort != null && modbusPort.toString().equals(port)) {
                        return true;
                    } else {
                        CcuLog.d(TAG_CCU_MODBUS, "Modbus port does not exist for : "+node);
                    }
                } catch (Exception e) {
                    CcuLog.e(TAG_CCU_MODBUS, "Error checking Modbus port for slave ID existence", e);
                }
            }
        }
        for (Integer node : lowCodeSlaveIdList) {
            if (node == slaveId.intValue()) {
                return true;
            }
        }
        CcuLog.d(TAG_CCU_MODBUS, "Modbus slave ID " + slaveId + " does not exist on port: " + port);
        return false;
    }

    public static boolean isBacnetMstpMacAddressExists(int slaveId) {
        CcuLog.d(TAG_CCU_BACNET_MSTP, "Checking Bacnet MSTP MAC Address existence: " + slaveId );

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext());
        int deviceMacAddress = sharedPreferences.getInt(PREF_MSTP_SOURCE_ADDRESS, 1);
        if (deviceMacAddress == slaveId) {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "Bacnet MSTP MAC Address " + slaveId + " is the same as device MAC address");
            return true;
        }

        CCUHsApi hsApi = CCUHsApi.getInstance();
        ArrayList<HashMap<Object, Object>> bacnetequips = hsApi.readAllEntities("equip and bacnetMstp");

        if(bacnetequips.isEmpty()){
            return false;
        }

        for (HashMap<Object,Object> equip : bacnetequips) {
            if (equip.get(BACNET_DEVICE_MAC_ADDR).toString().equals(String.valueOf(slaveId))) {
               return true;
            }
        }
        CcuLog.d(TAG_CCU_BACNET_MSTP, "Bacnet MSTP MAC Address " + slaveId );
        return false;
    }

    /**
     * Generates the next available Connect Module address while ensuring:
     * 1. The address belongs to the current address band
     * 2. The address is not zero (0x00)
     * 3. The address is not already used by another Connect Module
     * 4. The Modbus slave ID (address % 100) is not already in use
     *
     * Algorithm:
     * 1. Starts checking from (current address band + 1) to avoid zero address
     * 2. For each candidate address:
     *    a. Checks against existing Connect Modules
     *    b. Verifies Modbus slave ID availability
     * 3. Returns the first available address that meets all criteria
     *
     * @return short The next available Connect Module address
     * @throws IllegalStateException if no valid address can be found after reasonable attempts
     */
    public static short generateLowCodeAddrSkipZero() {
        final short currentBand = L.ccu().getAddressBand();
        final short maxAttempts = 200; // Prevent infinite loops
        short attempts = 0;

        // Get all existing devices (excluding BACnet devices)
        final ArrayList<HashMap<Object, Object>> nodes = CCUHsApi.getInstance().
                readAllEntities("device and (node or connectModule or pcn) and not bacnet");

        // Start checking from currentBand + 1 (to avoid 0x00)
        short nextAddr = (short) (currentBand + 1);

        while (attempts++ < maxAttempts) {
            boolean addressAvailable = true;

            // Check against existing nodes
            for (HashMap<Object, Object> node : nodes) {
                short nodeAddress = Short.parseShort(node.get("addr").toString());
                if (nodeAddress == nextAddr) {
                    addressAvailable = false;
                    break;
                }
            }

            // Check Modbus slave ID availability if address appears available
            if (addressAvailable && !isModbusSlaveIdExists((short) (nextAddr % 100),"")) {
                return nextAddr;
            }

            nextAddr++;
        }

        throw new IllegalStateException("Failed to find available Connect Module address after " +
                maxAttempts + " attempts");
    }

    public static short generateSmartNodeAddress()
    {
        short currentBand = L.ccu().getAddressBand();
        ArrayList<HashMap<Object,Object>> nodes = CCUHsApi.getInstance().readAllEntities("device and (node or connectModule) and not bacnet");
        if (nodes.isEmpty()) {
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

    public static short generateBacnetNodeAddres() {

        short currentBand = 500;
        ArrayList<HashMap<Object,Object>> nodes = CCUHsApi.getInstance().readAllEntities("bacnet and device");
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


    public static void removeHSDeviceEntities(Long node, String roomRef) {
        CCUHsApi hsApi = CCUHsApi.getInstance();
        if (L.ccu().oaoProfile != null && L.ccu().oaoProfile.getNodeAddress() == node) {
            L.ccu().oaoProfile = null;
        }
        HashMap<Object, Object> equip = hsApi.readEntity("equip and group == \""+node+"\" and roomRef == \""+roomRef+"\"");
        if (equip.get("id") != null)
        {
            CcuLog.d("CCU","equip to be deleted - Node address: "+node+", id: "+equip.get("id").toString());
            hsApi.deleteEntityTree(equip.get("id").toString());
            
            if(equip.containsKey(Tags.OAO) &&
                            ccu().systemProfile.getProfileType() != ProfileType.SYSTEM_DEFAULT) {
                ccu().systemProfile.setOutsideTempCoolingLockoutEnabled(hsApi, false);
            }
        }

        if(!ConnectNodeUtil.Companion.getConnectNodeByNodeAddress(node.toString(), hsApi).isEmpty()){
           ConnectNodeUtil.Companion.removeConnectNodeEquips(node.toString(), hsApi);
        }

        HashMap<Object, Object> device = hsApi.readEntity("device and addr == \""+node+"\" and roomRef == \""+roomRef+"\"");
        if (device.get("id") != null)
        {
            hsApi.deleteEntityTree(device.get("id").toString());
        }
        removeProfile(node, roomRef);
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
    public static ZoneProfile getModbusProfile(short addr, String zoneRef) {
        for (ZoneProfile p : L.ccu().zoneProfiles) {
            for (Short node : p.getNodeAddresses()) {
                if (node == addr) {
                    HashMap<Object, Object> equip = CCUHsApi.getInstance().readEntity("equip and group == \"" + addr + "\" and roomRef == \"" + zoneRef + "\"");
                    if (!equip.isEmpty()) {
                        CcuLog.d("CCU_MODBUS", "Modbus Profile found for " + addr + " with zoneRef " + zoneRef+" "+equip+": "+p);
                        return p;
                    } else {
                        CcuLog.d("CCU_MODBUS", "Modbus Profile Not found for " + addr + " with zoneRef " + zoneRef);
                    }
                }
            }

        }
        return null;
    }

    public static ZoneProfile getProfile(long addr) {
        for (ZoneProfile p : L.ccu().zoneProfiles) {
            if(p instanceof BacnetProfile){
                if(((BacnetProfile) p).getSlaveId() == addr){
                    return p;
                }
            }else{
                for (Short node : p.getNodeAddresses()) {
                    if (node == addr) {
                        return p;
                    }
                }
            }
        }
        CcuLog.d("CCU","Profile Not found for "+addr);
        return null;
    }

    public static void removeProfile(long addr, String roomRef) {
        ZoneProfile deleteProfile = null;
        for(ZoneProfile p : L.ccu().zoneProfiles) {
            if(p instanceof BacnetProfile){
                if(((BacnetProfile) p).getSlaveId() == addr){
                    deleteProfile = p;
                    break;
                }
            } else if (p instanceof  ModbusProfile) {
                ModbusProfile modbusProfile = (ModbusProfile) p;
                if(modbusProfile.getSlaveId() == addr && modbusProfile.getRoomRef().equals(roomRef)){
                    CcuLog.i(L.TAG_CCU_MODBUS, "Removing Modbus profile for addr: " + addr + " in room: " + roomRef);
                    deleteProfile = p;
                    break;
                }
            } else {
                for (Short node : p.getNodeAddresses()) {
                    if (node == addr) {
                        deleteProfile = p;
                        break;
                    }
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
        ExecutorTask.executeBackground(() -> Globals.getInstance().saveTags());
    }
}
