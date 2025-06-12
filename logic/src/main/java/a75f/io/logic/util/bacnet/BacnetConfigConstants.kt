package a75f.io.logic.util.bacnet


object  BacnetConfigConstants {
    const val BROADCAST_BACNET_APP_CONFIGURATION_TYPE = "a75f.io.renatus.BACNET_APP_CONFIGURATION"
    const val BROADCAST_BACNET_APP_START = "a75f.io.renatus.BACNET_APP_START"
    const val MSTP_CONFIGURATION_INTENT: String = "MSTP_CONFIGURATION"
    const val BROADCAST_BACNET_APP_STOP = "a75f.io.renatus.BACNET_APP_STOP"
    const val BROADCAST_BACNET_ZONE_ADDED = "a75f.io.renatus.BACNET_ZONE_ADDED"
    const val BROADCAST_BACNET_CONFIG_CHANGE = "a75f.io.renatus.BACNET_CONFIG_CHANGE"

    const val BACNET_CONFIGURATION = "BACnet_Config"
    const val IS_BACNET_CONFIG_FILE_CREATED = "isBACnetConfigFileCreated"
    const val IS_BACNET_INITIALIZED = "isBACnetinitialized"
    const val IS_BACNET_MSTP_INITIALIZED = "isBacnetMstpInitialized"
    const val BACNET_DEVICE_TYPE = "bacnetDeviceType"
    const val BACNET_DEVICE_TYPE_BBMD = "bbmd"
    const val BACNET_DEVICE_TYPE_FD = "fd"
    const val BACNET_DEVICE_TYPE_NORMAL = "normal"
    const val BACNET_BBMD_CONFIGURATION = "bacnetBbmdConfiguration"
    const val BACNET_MSTP_CONFIGURATION = "bacnetMstpConfiguration"
    const val BACNET_FD_CONFIGURATION = "bacnetFdConfiguration"
    const val HTTP_SERVER_STATUS = "httpServerStatus"
    const val BACNET_HEART_BEAT = "BACnet_HeartBeat"
    const val BACNET_MSTP_HEART_BEAT = "BACnet_Mstp_HeartBeat"
    const val BACNET_ID = "bacnetId"
    const val BACNET_TYPE = "bacnetType"
    const val BACNET_FD_AUTO_STATE = "fdAutoState"
    const val IS_GLOBAL = "isGlobal"
    const val IS_BACNET_STACK_INITIALIZED = "isBacnetStackInitialized"

    const val PREF_MSTP_BAUD_RATE = "mstp_baudrate"
    const val PREF_MSTP_SOURCE_ADDRESS = "mstp_source_address"
    const val PREF_MSTP_MAX_MASTER = "mstp_max_master"
    const val PREF_MSTP_MAX_FRAME = "mstp_max_frame"
    const val PREF_MSTP_DEVICE_ID = "mstp_device_id"
    const val PREF_MSTP_PORT_ADDRESS = "mstp_port_address"
    const val CONFIGURATION_TYPE = "Configuration Type"
    const val MSTP_CONFIGURATION = "MSTP Configuration"
    const val IP_CONFIGURATION = "IP Configuration"
    const val DEVICE_ID = "deviceId"
    const val DESTINATION_IP = "destinationIp"
    const val DESTINATION_PORT = "destinationPort"
    const val MAC_ADDRESS = "macAddress"
    const val DEVICE_NETWORK = "deviceNetwork"

    const val ZONE_TO_VIRTUAL_DEVICE_MAPPING = "zoneToVirtualDeviceMapping"
    //net work config constants
    const val IP_ADDRESS = "ipAddress"
    const val LOCAL_NETWORK_NUMBER = "localNetNum"
    const val VIRTUAL_NETWORK_NUMBER = "virtualNetNum"
    const val PORT = "port"
    const val NETWORK_INTERFACE = "networkInterface"

    // "device" constants
    const val IP_DEVICE_INSTANCE_NUMBER = "ipDeviceInstanceNum"
    const val IP_DEVICE_OBJECT_NAME = "ipDeviceObjectName"
    const val VENDOR_ID = "vendorId"
    const val VENDOR_NAME = "vendorName"
    const val MODEL_NAME = "modelName"
    const val APPLICATION_SOFTWARE_VERSION = "applicationSoftwareVersion"
    const val FIRMWARE_REVISION = "firmwareRevision"
    const val LOCATION = "location"
    const val PASSWORD = "password"
    const val APDU_TIMEOUT = "apduTimeout"
    const val NUMBER_OF_APDU_RETRIES = "numOfApduRetries"
    const val APDU_SEGMENT_TIMEOUT = "apduSegmentTimeout"
    const val UTC_OFFSET = "utcOffset"
    const val DAYLIGHT_SAVING_STATUS = "daylightSavingStatus"
    const val SERIAL_NUMBER = "serialNumber"
    const val DESCRIPTION = "description"
    const val NULL = "null"
    const val EMPTY_STRING = ""

    //"objectConf" constants
    const val NUMBER_OF_NOTIFICATION_CLASS_OBJECTS = "noOfNotificationClassObjects"
    const val NUMBER_OF_TREND_LOG_OBJECTS = "noOfTrendLogObjects"
    const val NUMBER_OF_SCHEDULE_OBJECTS = "noOfScheduleObjects"
    const val NUMBER_OF_OFFSET_VALUES = "noOfOffsetValues"


    const val IP_ADDRESS_VAL = "192.168.1.1"
    const val PORT_VAL = "47808"
    const val VENDOR_ID_VALUE = 1181
    const val VENDOR_NAME_VALUE = "75F"

    const val BROADCAST_BACNET_APP_GLOBAL_PARAM = "a75f.io.renatus.BROADCAST_BACNET_APP_GLOBAL_PARAM"


}
enum class BacnetServerStatus {
    NOT_INITIALIZED,
    INITIALIZED_ONLINE,
    INITIALIZED_OFFLINE
}

enum class ObjectType(val key: String, val value: Int) {
    OBJECT_ANALOG_INPUT("OBJECT_ANALOG_INPUT",0),
    OBJECT_ANALOG_OUTPUT("OBJECT_ANALOG_OUTPUT",1),
    OBJECT_ANALOG_VALUE("OBJECT_ANALOG_VALUE",2),
    OBJECT_BINARY_INPUT("OBJECT_BINARY_INPUT",3),
    OBJECT_BINARY_OUTPUT("OBJECT_BINARY_OUTPUT",4),
    OBJECT_BINARY_VALUE("OBJECT_BINARY_VALUE",5),
    OBJECT_CALENDAR("OBJECT_CALENDAR",6),
    OBJECT_COMMAND("OBJECT_COMMAND",7),
    OBJECT_DEVICE("OBJECT_DEVICE",8),
    OBJECT_EVENT_ENROLLMENT("OBJECT_EVENT_ENROLLMENT",9),
    OBJECT_FILE("OBJECT_FILE",10),
    OBJECT_GROUP("OBJECT_GROUP",11),
    OBJECT_LOOP("OBJECT_LOOP",12),
    OBJECT_MULTI_STATE_INPUT("OBJECT_MULTI_STATE_INPUT",13),
    OBJECT_MULTI_STATE_OUTPUT("OBJECT_MULTI_STATE_OUTPUT",14),
    OBJECT_NOTIFICATION_CLASS("OBJECT_NOTIFICATION_CLASS",15),
    OBJECT_PROGRAM("OBJECT_PROGRAM",16),
    OBJECT_SCHEDULE("OBJECT_SCHEDULE",17),
    OBJECT_AVERAGING("OBJECT_AVERAGING",18),
    OBJECT_MULTI_STATE_VALUE("OBJECT_MULTI_STATE_VALUE",19),
    OBJECT_TRENDLOG("OBJECT_TRENDLOG",20),
    OBJECT_LIFE_SAFETY_POINT("OBJECT_LIFE_SAFETY_POINT",21),
    OBJECT_LIFE_SAFETY_ZONE("OBJECT_LIFE_SAFETY_ZONE",22),
    OBJECT_ACCUMULATOR("OBJECT_ACCUMULATOR",23),
    OBJECT_PULSE_CONVERTER("OBJECT_PULSE_CONVERTER",24),
    OBJECT_EVENT_LOG("OBJECT_EVENT_LOG",25),
    OBJECT_GLOBAL_GROUP("OBJECT_GLOBAL_GROUP",26),
    OBJECT_TREND_LOG_MULTIPLE("OBJECT_TREND_LOG_MULTIPLE",27),
    OBJECT_LOAD_CONTROL("OBJECT_LOAD_CONTROL",28),
    OBJECT_STRUCTURED_VIEW("OBJECT_STRUCTURED_VIEW",29),
    OBJECT_ACCESS_DOOR("OBJECT_ACCESS_DOOR",30),
    OBJECT_TIMER("OBJECT_TIMER",31),
    OBJECT_ACCESS_CREDENTIAL("OBJECT_ACCESS_CREDENTIAL",32),
    OBJECT_ACCESS_POINT("OBJECT_ACCESS_POINT",33),
    OBJECT_ACCESS_RIGHTS("OBJECT_ACCESS_RIGHTS",34),
    OBJECT_ACCESS_USER("OBJECT_ACCESS_USER",35),
    OBJECT_ACCESS_ZONE("OBJECT_ACCESS_ZONE",36),
    OBJECT_CREDENTIAL_DATA_INPUT("OBJECT_CREDENTIAL_DATA_INPUT", 37),
    OBJECT_NETWORK_SECURITY("OBJECT_NETWORK_SECURITY", 38),
    OBJECT_BITSTRING_VALUE("OBJECT_BITSTRING_VALUE", 39),
    OBJECT_CHARACTERSTRING_VALUE("OBJECT_CHARACTERSTRING_VALUE",40),
    OBJECT_DATE_PATTERN_VALUE("OBJECT_DATE_PATTERN_VALUE",41),
    OBJECT_DATE_VALUE("OBJECT_DATE_VALUE",42),
    OBJECT_DATETIME_PATTERN_VALUE("OBJECT_DATETIME_PATTERN_VALUE",43),
    OBJECT_DATETIME_VALUE("OBJECT_DATETIME_VALUE",44),
    OBJECT_INTEGER_VALUE("OBJECT_INTEGER_VALUE",45),
    OBJECT_LARGE_ANALOG_VALUE("OBJECT_LARGE_ANALOG_VALUE",46),
    OBJECT_OCTETSTRING_VALUE("OBJECT_OCTETSTRING_VALUE",47),
    OBJECT_POSITIVE_INTEGER_VALUE("OBJECT_POSITIVE_INTEGER_VALUE",48),
    OBJECT_TIME_PATTERN_VALUE("OBJECT_TIME_PATTERN_VALUE",49),
    OBJECT_TIME_VALUE("OBJECT_TIME_VALUE",50),
    OBJECT_NOTIFICATION_FORWARDER("OBJECT_NOTIFICATION_FORWARDER",51),
    OBJECT_ALERT_ENROLLMENT("OBJECT_ALERT_ENROLLMENT",52),
    OBJECT_CHANNEL("OBJECT_CHANNEL",53),
    OBJECT_LIGHTING_OUTPUT("OBJECT_LIGHTING_OUTPUT",54),
    OBJECT_BINARY_LIGHTING_OUTPUT("OBJECT_BINARY_LIGHTING_OUTPUT",55),
    OBJECT_NETWORK_PORT("OBJECT_NETWORK_PORT",56),
    OBJECT_ELEVATOR_GROUP("OBJECT_ELEVATOR_GROUP",57),
    OBJECT_ESCALATOR("OBJECT_ESCALATOR",58),
    OBJECT_LIFT("OBJECT_LIFT",59);

    companion object {
        infix fun from(value: Int): ObjectType? = ObjectType.values().firstOrNull { it.value == value }
    }
}
object BacnetTypeMapper {

    private val typeMap = mapOf(
        "AnalogInput" to "ANALOG_INPUT",
        "AnalogOutput" to "ANALOG_OUTPUT",
        "AnalogValue" to "ANALOG_VALUE",
        "BinaryInput" to "BINARY_INPUT",
        "BinaryOutput" to "BINARY_OUTPUT",
        "BinaryValue" to "BINARY_VALUE",
        "Calendar" to "CALENDAR",
        "Command" to "COMMAND",
        "Device" to "DEVICE",
        "EventEnrollment" to "EVENT_ENROLLMENT",
        "File" to "FILE",
        "Group" to "GROUP",
        "Loop" to "LOOP",
        "MultiStateInput" to "MULTI_STATE_INPUT",
        "MultiStateOutput" to "MULTI_STATE_OUTPUT",
        "NotificationClass" to "NOTIFICATION_CLASS",
        "Program" to "PROGRAM",
        "Schedule" to "SCHEDULE",
        "Averaging" to "AVERAGING",
        "MultiStateValue" to "MULTI_STATE_VALUE",
        "TrendLog" to "TRENDLOG",
        "LifeSafetyPoint" to "LIFE_SAFETY_POINT",
        "LifeSafetyZone" to "LIFE_SAFETY_ZONE",
        "Accumulator" to "ACCUMULATOR",
        "PulseConverter" to "PULSE_CONVERTER",
        "EventLog" to "EVENT_LOG",
        "GlobalGroup" to "GLOBAL_GROUP",
        "TrendLogMultiple" to "TREND_LOG_MULTIPLE",
        "LoadControl" to "LOAD_CONTROL",
        "StructuredView" to "STRUCTURED_VIEW",
        "AccessDoor" to "ACCESS_DOOR",
        "Timer" to "TIMER",
        "AccessCredential" to "ACCESS_CREDENTIAL",
        "AccessPoint" to "ACCESS_POINT",
        "AccessRights" to "ACCESS_RIGHTS",
        "AccessUser" to "ACCESS_USER",
        "AccessZone" to "ACCESS_ZONE",
        "CredentialDataInput" to "CREDENTIAL_DATA_INPUT",
        "NetworkSecurity" to "NETWORK_SECURITY",
        "BitStringValue" to "BITSTRING_VALUE",
        "CharacterStringValue" to "CHARACTERSTRING_VALUE",
        "DatePatternValue" to "DATE_PATTERN_VALUE",
        "DateValue" to "DATE_VALUE",
        "DateTimePatternValue" to "DATETIME_PATTERN_VALUE",
        "DateTimeValue" to "DATETIME_VALUE",
        "IntegerValue" to "INTEGER_VALUE",
        "LargeAnalogValue" to "LARGE_ANALOG_VALUE",
        "OctetStringValue" to "OCTETSTRING_VALUE",
        "PositiveIntegerValue" to "POSITIVE_INTEGER_VALUE",
        "TimePatternValue" to "TIME_PATTERN_VALUE",
        "TimeValue" to "TIME_VALUE",
        "NotificationForwarder" to "NOTIFICATION_FORWARDER",
        "AlertEnrollment" to "ALERT_ENROLLMENT",
        "Channel" to "CHANNEL",
        "LightingOutput" to "LIGHTING_OUTPUT",
        "BinaryLightingOutput" to "BINARY_LIGHTING_OUTPUT",
        "NetworkPort" to "NETWORK_PORT",
        "ElevatorGroup" to "ELEVATOR_GROUP",
        "Escalator" to "ESCALATOR",
        "Lift" to "LIFT"
    )

    fun getObjectType(value: String): String {
        return typeMap[value] ?: "UNKNOWN_TYPE"
    }
}