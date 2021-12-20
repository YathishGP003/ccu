package a75f.io.device.daikin;

const val IE_POINT_TYPE_MI = "MI"
const val IE_POINT_TYPE_MV = "MV"
const val IE_POINT_TYPE_AV = "AV"
const val IE_POINT_TYPE_SY = "SY"
const val IE_POINT_TYPE_AI = "AI"

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
const val IE_POINT_NAME_EFF_DAT_SETPOINT = "EffDATSetpoint"
const val IE_POINT_NAME_DAT_VAL = "DAT"
const val IE_POINT_NAME_SF_CAPACITY_FEEDBACK = "SFCapFbk"

const val IE_POINT_NAME_CCU_SYSTEM_CLOCK = "CCU_SystemClock"

const val IE_MSG_BODY = "<requests><request>%f</request></requests>"

internal enum class OccMode {
    Occ, Unocc, TntOvrd, Auto, UnInit
}

internal enum class NetApplicMode {
    Null, Off, HeatOnly, CoolOnly, FanOnly, Auto, Invalid, UnInit
}

internal enum class HumidityCtrl {
    None, RelHum, DewPt, Always, UnInit
}
