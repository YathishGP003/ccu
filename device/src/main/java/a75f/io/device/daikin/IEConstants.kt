package a75f.io.device.daikin;

const val IE_POINT_TYPE_MI = "MI"
const val IE_POINT_TYPE_MV = "MV"
const val IE_POINT_TYPE_AV = "AV"

const val IE_POINT_NAME_OCCUPANCY = "OccMode"
const val IE_POINT_NAME_CONDITIONING_MODE = "NetApplicMode"
const val IE_POINT_NAME_DAT_SETPOINT = "DATClgSetpoint"
const val IE_POINT_NAME_DSP_SETPOINT = "DSPSpt"
const val IE_POINT_NAME_FAN_SPEED_CONTROL = "RemoteSFCap"
const val IE_POINT_NAME_HUMIDITY_CONTROL = "HumidityCtrl"
const val IE_POINT_NAME_RELATIVE_HUMIDITY = "BACnetRH"
const val IE_POINT_NAME_HUMIDITY_SETPOINT = "HumiditySPT"

const val IE_POINT_NAME_SYSTEM_CLOCK = "SystemClock"
const val IE_POINT_NAME_ALARM_WARN = "ActiveWarnEnu"
const val IE_POINT_NAME_ALARM_PROB = "ActiveProbEnu"
const val IE_POINT_NAME_ALARM_FAULT = "ActiveFaultEnu"
const val IE_POINT_NAME_OCCUPANCY_STATUS = "OccStatus"

const val IE_MSG_BODY = "<requests>\n<request>\n%f\n</request>\n</requests>"
