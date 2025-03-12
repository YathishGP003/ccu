package a75f.io.logic.util

import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.logic.L
import a75f.io.logic.jobs.bearertoken.BearerTokenManager
import java.util.Date

const val TIMER_TO_BE_VALID = 900000

fun isOfflineMode():Boolean{
    Domain.getDomainCCUEquip()?.let {
        return it.offlineMode.readDefaultVal() > 0.0
    }

    return hayStack.readDefaultValByDomainName(DomainName.offlineMode) > 0.0
}

fun fetchToken(){
    BearerTokenManager.getInstance().fetchToken(hayStack)
}

fun updateMechanicalLockout() {
    val outsideTemp = hayStack.readEntity("point and outside and air and temp and sensor and oao")
    if(outsideTemp.isNotEmpty())
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
