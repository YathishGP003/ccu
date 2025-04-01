package a75f.io.logic.util.bacnet


object  BacnetConfigConstants {
    const val BROADCAST_BACNET_APP_CONFIGURATION_TYPE = "a75f.io.renatus.BACNET_APP_CONFIGURATION"
    const val BROADCAST_BACNET_APP_START = "a75f.io.renatus.BACNET_APP_START"
    const val BROADCAST_BACNET_APP_STOP = "a75f.io.renatus.BACNET_APP_STOP"
    const val BROADCAST_BACNET_ZONE_ADDED = "a75f.io.renatus.BACNET_ZONE_ADDED"

    const val BACNET_CONFIGURATION = "BACnet_Config"
    const val IS_BACNET_CONFIG_FILE_CREATED = "isBACnetConfigFileCreated"
    const val IS_BACNET_INITIALIZED = "isBACnetinitialized"
    const val BACNET_DEVICE_TYPE = "bacnetDeviceType"
    const val BACNET_DEVICE_TYPE_BBMD = "bbmd"
    const val BACNET_DEVICE_TYPE_FD = "fd"
    const val BACNET_DEVICE_TYPE_NORMAL = "normal"
    const val BACNET_BBMD_CONFIGURATION = "bacnetBbmdConfiguration"
    const val BACNET_FD_CONFIGURATION = "bacnetFdConfiguration"
    const val HTTP_SERVER_STATUS = "httpServerStatus"
    const val BACNET_HEART_BEAT = "BACnet_HeartBeat"
    const val BACNET_ID = "bacnetId"
    const val BACNET_TYPE = "bacnetType"
    const val BACNET_FD_AUTO_STATE = "fdAutoState"
    const val IS_GLOBAL = "isGlobal"
    const val IS_BACNET_STACK_INITIALIZED = "isBacnetStackInitialized"



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