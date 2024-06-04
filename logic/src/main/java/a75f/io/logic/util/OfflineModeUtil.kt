package a75f.io.logic.util

import a75f.io.api.haystack.*
import a75f.io.api.haystack.util.hayStack
import a75f.io.logic.L
import a75f.io.logic.jobs.bearertoken.BearerTokenManager
import a75f.io.logic.tuners.SystemTuners
import a75f.io.logic.tuners.TunerConstants
import org.projecthaystack.HDateTime
import java.util.*

val TIMER_TO_BE_VALID = 900000

fun createOfflineModePoint(){

    val siteMap = hayStack.readEntity(Tags.SITE)
    val equipMap = hayStack.readEntity("equip and system and not modbus and not connectModule")
    val equip = Equip.Builder().setHashMap(equipMap).build()
    val equipRef = equip.id
    val siteRef = Objects.requireNonNull(siteMap[Tags.ID]).toString()
    val tz = Objects.requireNonNull(siteMap["tz"]).toString()
    val equipDis = Objects.requireNonNull(siteMap["dis"]).toString() + "-SystemEquip"
    if(isPointNotAvailable(equipRef)) {
        val offlineMode: Point =
            Point.Builder()
                .setDisplayName(SystemTuners.getDisplayNameFromVariation(equipDis + "-" + "offlineMode"))
                .setSiteRef(siteRef).setEquipRef(equipRef)
                .addMarker("sp").addMarker("system").setHisInterpolate("cov")
                .addMarker("writable").addMarker("his")
                .addMarker("offline")
                .addMarker("config")
                .addMarker("mode")
                .addMarker("cur")
                .setEnums("online,offline")
                .setKind(Kind.NUMBER)
                .setTz(tz).build()
        val offlineModeId: String = CCUHsApi.getInstance().addPoint(offlineMode)
        CCUHsApi.getInstance()
            .writePointForCcuUser(offlineModeId, TunerConstants.UI_DEFAULT_VAL_LEVEL, 0.0, 0)
        CCUHsApi.getInstance().writeHisValById(offlineModeId, 0.0)
    }
}

fun isPointNotAvailable(equipRef: String?): Boolean {
    val offlineModePoint = hayStack.readEntity(
        "point and offline and mode and equipRef == \"$equipRef\""
    )
    return offlineModePoint.isEmpty()
}

fun isOfflineMode():Boolean{
    return hayStack.readDefaultVal("offline and mode and point") > 0

}

fun fetchToken(){
    BearerTokenManager.getInstance().fetchToken(hayStack)
}

fun updateMechanicalLockout() {
    val outsideTemp = hayStack.readEntity("point and outside and air and temp and sensor and oao")
    if(!outsideTemp.isEmpty())
        if(!isOATValid() && !isWeatherAPIValid()) {
            L.ccu().systemProfile.setOutsideTempCoolingLockoutEnabled(hayStack, false)
        }
}

fun isOATValid() : Boolean{
    val outsideTemp = hayStack.readEntity("point and outside and air and temp and sensor and oao")
    val hisitem = hayStack.curRead(outsideTemp["id"].toString())
    val hisItemDate :Date = hisitem.date
    val outsideTempVal = hisitem.`val`
    val lastModifiedDateTime = hisItemDate.time
    val currentTime =  Date (System.currentTimeMillis())
    return !((currentTime.time - lastModifiedDateTime > TIMER_TO_BE_VALID) || outsideTempVal == 0.0)

}

fun isWeatherAPIValid() : Boolean{
    return hayStack.getExternalTemp() != 0.0
}
