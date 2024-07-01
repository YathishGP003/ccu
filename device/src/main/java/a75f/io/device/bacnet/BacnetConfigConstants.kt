package a75f.io.device.bacnet


object  BacnetConfigConstants {
    const val BROADCAST_BACNET_APP_CONFIGURATION_TYPE = "a75f.io.renatus.BACNET_APP_CONFIGURATION"
    const val BROADCAST_BACNET_APP_START = "a75f.io.renatus.BACNET_APP_START"
    const val BROADCAST_BACNET_APP_STOP = "a75f.io.renatus.BACNET_APP_STOP"
    const val BROADCAST_BACNET_ZONE_ADDED = "a75f.io.renatus.BACNET_ZONE_ADDED"

    const val BACNET_CONFIGURATION = "BACnet_Config"
    const val IS_BACNET_CONFIG_FILE_CREATED = "isBACnetConfigFileCreated"
    const val IS_BACNET_INITIALIZED = "isBACnetinitialized"
    const val HTTP_SERVER_STATUS = "httpServerStatus"
    const val BACNET_HEART_BEAT = "BACnet_HeartBeat"
    const val BACNET_ID = "bacnetId"
    const val BACNET_TYPE = "bacnetType"



    const val ZONE_TO_VIRTUAL_DEVICE_MAPPING = "zoneToVirtualDeviceMapping"
    //net work config constants
    const val IP_ADDRESS = "ipAddress"
    const val LOCAL_NETWORK_NUMBER = "localNetNum"
    const val VIRTUAL_NETWORK_NUMBER = "virtualNetNum"
    const val PORT = "port"

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
}