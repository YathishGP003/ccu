package a75f.io.bo.ble;

import java.util.HashMap;

/**
 * Created by ryanmattison isOn 7/17/17.
 */

public class GattAttributes {

    private static HashMap<String, String> attributes = new HashMap();
    private static HashMap<Integer, String> appearances = new HashMap();
    private static HashMap<Integer, String> deviceTypes = new HashMap();

        /*
        Device Information
Service
00000000-0000-1000-0000-000000000000 Collection of attributes providing various device
information
--
Device Type 00000000-0000-1001-0000-000000000000 Type of Device: Smart Node, RTS, etc. R
Current Firmware
Version
00000000-0000-1002-0000-000000000000 Major and minor firmware version R
Serial Number 00000000-0000-1003-0000-000000000000 Unique device identifier R
Pin Code 00000000-0000-1004-0000-000000000000 Identifies device during provisioning R
Zone Settings
Service
00000000-0000-1100-0000-000000000000 Settings specific to the zone --
Peer Link Key 00000000-0000-1101-0000-000000000000 Information necessary to form a BLE link between
the Smart Node and a peer (i.e. wireless RTS)
R/W
Room Name 00000000-0000-1102-0000-000000000000 A text description of the room (zone) R/W
LW Mesh Pairing
Address
00000000-0000-1103-0000-000000000000 The address of the Smart Node isOn the LW Mesh
network
R/W
LW Mesh Security
Key
00000000-0000-1104-0000-000000000000 The security key to use for encrypting messages
sent over LW Mesh
R/W
Firmware Signature
Key
00000000-0000-1105-0000-000000000000 The key used to generate a signature or hashbased
message authentication code (HMAC) of
the application binary image
R/W
Zone Configuration
Status
00000000-0000-1106-0000-000000000000 Indicates the success or failure of an update to a
zone setting
R/W
Zone Configuration
CRC
00000000-0000-1107-0000-000000000000 Cyclic Redundancy Check value computed over
the other attributes in the Zone Settings service
R/W
         */

    // Note: Bluetooth base UUID is 00000000-0000-1000-8000-00805F9B34FB

    // User Defined Services
    public static final String DEVICE_INFORMATION_SERVICE = "00000000-0000-0000-1000-000000000000";
    public static final String ZONE_SETTINGS_SERVICE = "00000000-0000-0000-1100-000000000000";
    public static final String OPERATIONAL_DATA_SERVICE = "00000000-0000-0000-1200-000000000000";
    public static final String FIRMWARE_UPDATE_SERVICE = "00000000-0000-0000-1300-000000000000";

    // Generic Characteristics
    public static final String DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb";
    public static final String APPEARANCE = "00002a01-0000-1000-8000-00805f9b34fb";
    public static final String PERIPHERAL_PRIVACY_FLAG = "00002a02-0000-1000-8000-00805f9b34fb";
    public static final String RECONNECTION_ADDRESS = "00002a03-0000-1000-8000-00805f9b34fb";
    public static final String PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = "00002a04-0000-1000-8000-00805f9b34fb";
    public static final String SERVICE_CHANGED = "00002a05-0000-1000-8000-00805f9b34fb";
    public static final String MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    // User Defined Characteristics
    public static final String DEVICE_TYPE = "00000000-0000-0000-1001-000000000000";
    public static final String CURRENT_FIRMWARE_VERSION = "00000000-0000-0000-1002-000000000000";
    public static final String SERIAL_NUMBER = "00000000-0000-0000-1003-000000000000";
    public static final String BLE_PIN = "00000000-0000-0000-1004-000000000000";
    public static final String BLE_LINK_KEY = "00000000-0000-0000-1101-000000000000";
    public static final String ROOM_NAME = "00000000-0000-0000-1102-000000000000";
    public static final String LW_MESH_PAIRING_ADDRESS = "00000000-0000-0000-1103-000000000000";
    public static final String LW_MESH_SECURITY_KEY = "00000000-0000-0000-1104-000000000000";
    public static final String FIRMWARE_SIGNATURE_KEY = "00000000-0000-0000-1105-000000000000";
    public static final String ZONE_CONFIGURATION_STATUS = "00000000-0000-0000-1106-000000000000";
    public static final String CRC = "00000000-0000-0000-1107-000000000000";
    public static final String ROOM_TEMP = "00000000-0000-0000-1201-000000000000";
    public static final String FACTORY_CAL_TEMP_OFFSET = "00000000-0000-0000-1202-000000000000";
    public static final String SYSTEM_TEMP_OFFSET = "00000000-0000-0000-1203-000000000000";
    public static final String LOW_BATTERY = "00000000-0000-0000-1204-000000000000";
    public static final String FIRMWARE_UPDATE_STATUS = "00000000-0000-0000-1301-000000000000";
    public static final String FIRMWARE_UPDATE_VERSION = "00000000-0000-0000-1302-000000000000";
    public static final String FIRMWARE_UPDATE_DATA = "00000000-0000-0000-1303-000000000000";

    public static final byte ZONE_CONFIGURATION_IN_PROGRESS = (byte) (0);
    public static final byte ZONE_CONFIGURATION_SUCCESS = (byte) (1);
    public static final byte ZONE_CONFIGURATION_FAILURE = (byte) (2);

    static {
        // Generic Services
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Generic Access Service");
        attributes.put("00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute Service");

        // User Defined Services
        attributes.put(DEVICE_INFORMATION_SERVICE, "Device Information Service");
        attributes.put(OPERATIONAL_DATA_SERVICE, "Operational Data Service");
        attributes.put(ZONE_SETTINGS_SERVICE, "Zone Settings Service");
        attributes.put(FIRMWARE_UPDATE_SERVICE, "Firmware Update Service");

        // Sample Characteristics.
        attributes.put(DEVICE_NAME, "Device Name");
        attributes.put(APPEARANCE, "Appearance");
        attributes.put(PERIPHERAL_PRIVACY_FLAG, "Peripheral Privacy Flag");
        attributes.put(RECONNECTION_ADDRESS, "Reconnection Address");
        attributes.put(PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS, "Peripheral Preferred Connection Parameters");
        attributes.put(SERVICE_CHANGED, "Service Changed");
        attributes.put(MANUFACTURER_NAME_STRING, "Manufacturer Name String");
        attributes.put(DEVICE_TYPE, "Device Type");
        attributes.put(CURRENT_FIRMWARE_VERSION, "Current Firmware Version");
        attributes.put(SERIAL_NUMBER, "Serial Number");
        attributes.put(BLE_LINK_KEY, "BLE Link Key");
        attributes.put(BLE_PIN, "BLE PIN");
        attributes.put(CRC, "CRC");
        attributes.put(ROOM_NAME, "Room Name");
        attributes.put(LW_MESH_PAIRING_ADDRESS, "LW Mesh Pairing Address");
        attributes.put(LW_MESH_SECURITY_KEY, "LW Mesh Security Key");
        attributes.put(FIRMWARE_SIGNATURE_KEY, "Firmware Signature Key");
        attributes.put(ZONE_CONFIGURATION_STATUS, "Zone Configuration Status");
        attributes.put(ROOM_TEMP, "Room Temp");
        attributes.put(FACTORY_CAL_TEMP_OFFSET, "Factory Cal Temp Offset");
        attributes.put(SYSTEM_TEMP_OFFSET, "System Temp Offset");
        attributes.put(LOW_BATTERY, "Low Battery");
        attributes.put(FIRMWARE_UPDATE_STATUS, "Firmware Update Status");
        attributes.put(FIRMWARE_UPDATE_VERSION, "Firmware Update Version");
        attributes.put(FIRMWARE_UPDATE_DATA, "Firmware Update Data");

        // Appearance Values
        appearances.put(0, "Unknown");
        appearances.put(32, "75F Remote Temperature Sensor");
        appearances.put(64, "Generic Phone");
        appearances.put(128, "Generic Computer");
        appearances.put(192, "Generic Watch");
        appearances.put(256, "Generic Clock");
        appearances.put(320, "Generic Display");
        appearances.put(384, "Generic Remote Control");
        appearances.put(448, "Generic Eye-Glasses");
        appearances.put(512, "Generic Tag");
        appearances.put(576, "Generic Keyring");
        appearances.put(640, "Generic Media Player");
        appearances.put(704, "Generic Barcode Scanner");
        appearances.put(768, "Generic Therometer");
        appearances.put(769, "Thermometer: Ear");
        appearances.put(832, "Generic Heart Rate Sensor");
        appearances.put(833, "Heart Rate Sensor: Heart Rate Belt");
        appearances.put(896, "Generic Blood Pressure");
        appearances.put(897, "Blood Pressure: Arm");
        appearances.put(898, "Blood Pressure: Wrist");
        appearances.put(960, "Human Interface Device");
        appearances.put(961, "Keyboard");
        appearances.put(962, "Mouse");
        appearances.put(963, "Joystick");
        appearances.put(964, "Gamepad");
        appearances.put(965, "Digitizer Tablet");
        appearances.put(966, "Card Reader");
        appearances.put(967, "Digital Pen");
        appearances.put(968, "Barcode Scanner");
        appearances.put(1024, "Generic Glucose Meter");
        appearances.put(1088, "Generic: Running Walkng Sensor");
        appearances.put(1089, "Running Walking Sensor: In-Shoe");
        appearances.put(1090, "Running Walking Sensor: On-Shoe");
        appearances.put(1091, "Running Walking Sensor: On-Hip");
        appearances.put(1152, "Generic: Cycling");
        appearances.put(1153, "Cycling: Cycling Computer");
        appearances.put(1154, "Cycling: Speed Sensor");
        appearances.put(1155, "Cycling: Cadence Sensor");
        appearances.put(1156, "Cycling: Power Sensor");
        appearances.put(1157, "Cycling: Speed and Cadence Sensor");
        appearances.put(3136, "Generic: Pulse Oximeter");
        appearances.put(3137, "Fingertip Pulse Oximeter");
        appearances.put(3138, "Wrist Worn Pulse Oximeter");
        appearances.put(3200, "Generic: Weight Scale");
        appearances.put(5184, "Generic: Outdoor Sports Activity");
        appearances.put(5185, "Location Display Device");
        appearances.put(5186, "Location and Navigation Display Device");
        appearances.put(5187, "Location Pod");
        appearances.put(5188, "Location and Navigation Pod");

        deviceTypes.put(0, "Remote Temperature Sensor");
        deviceTypes.put(1, "Smart Node");
        deviceTypes.put(2, "Control Mote");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public static String lookupAppearance(int value) {
        String appearance = appearances.get(value);
        return appearance == null ? "Undefined" : appearance;
    }

    public static String lookupDeviceType(int value) {
        String deviceType = deviceTypes.get(value);
        return deviceType == null ? "Undefined" : deviceType;
    }
}
