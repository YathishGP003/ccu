package a75f.io.logic.bo.building.statprofiles.util

/**
 * Created by Manjunath K on 22-04-2025.
 */

enum class PossibleFanMode {
    OFF, LOW, MED, HIGH, LOW_MED_HIGH, LOW_MED, LOW_HIGH, MED_HIGH, AUTO
}

enum class MyStatPossibleFanMode {
    OFF, LOW, HIGH, LOW_HIGH, AUTO
}

enum class PossibleConditioningMode {
    OFF, COOLONLY, HEATONLY, BOTH
}

enum class FanSpeed {
    OFF,LOW,MEDIUM,HIGH
}

enum class StatZoneStatus {
    STATUS,
    TARGET_HUMIDITY,
    TARGET_DEHUMIDIFY,
    CONDITIONING_ENABLED,
    CONDITIONING_MODE,
    FAN_MODE,
    FAN_LEVEL,
    DISCHARGE_AIRFLOW,
    CONFIG,
    EQUIP,
    PROFILE_NAME,
    PROFILE_TYPE,
    SUPPLY_TEMP,
    MIXED_AIR_TEMP
}

// MyStat Does not have medium fan options So specific enum is required
enum class MyStatFanStages {
    OFF,
    AUTO,
    LOW_CUR_OCC,
    LOW_OCC,
    LOW_ALL_TIME,
    HIGH_CUR_OCC,
    HIGH_OCC,
    HIGH_ALL_TIME,
}



