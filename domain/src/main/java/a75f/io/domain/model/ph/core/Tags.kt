package a75f.io.domain.model.ph.core

/**
 * Tags names defined by Project Haystack. Custom tag names will not be included.
 */
class Tags {
    companion object {
        const val ALLOWED_VALUES = "allowedValues"
        const val CMD = "cmd"
        const val DIS = "dis"
        const val DEVICE = "device"
        const val DEVICE_REF = "deviceRef"
        const val EQUIP_REF = "equipRef"
        const val EQUIP = "equip"
        const val FLOOR = "floor"
        const val HIS = "his"
        const val HIS_INTERPOLATE = "hisInterpolate"
        const val HIS_MODE = "hisMode"
        const val ID = "id"
        const val INCREMENT_VAL = "incrementVal"
        const val KIND = "kind"
        const val MIN_VAL = "minVal"
        const val MAX_VAL = "maxVal"
        const val PROFILE = "profile"
        const val PHYSICAL = "physical"
        const val POINT = "point"
        const val SENSOR = "sensor"
        const val SITE_REF = "siteRef"
        const val SPACE_REF = "spaceRef"
        const val SITE = "site"
        const val SP = "sp"
        const val TUNER = "tuner"
        const val TZ = "tz"
        const val UNIT = "unit"
        const val WRITABLE = "writable"
        const val WEATHER_STATION = "weatherStation"
        const val WEATHER = "weather"
        const val ZONE = "zone"
        val functionTags = setOf(CMD, SENSOR, SP)
    }
}
