package a75f.io.renatus.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.tuners.TunerConstants

class TemperatureModeUtil {
    fun getTemperatureMode(): Double {
        val point: HashMap<Any, Any> = Domain.hayStack.readEntity("point and domainName == \"${DomainName.temperatureMode}\"")
        if (point.isNotEmpty()) {
            val id = point["id"].toString()
            return CCUHsApi.getInstance().readDefaultValByLevel(
                id,
                TunerConstants.SYSTEM_BUILDING_VAL_LEVEL
            )
        } else {
            CcuLog.i(Domain.LOG_TAG,"Temperature Mode point does not exist")
            /*By default point value should be 1*/
            return 1.0
        }
    }
    fun setTemperatureMode(position: Int) {
        Domain.writeValAtLevelByDomain(DomainName.temperatureMode,
            TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, position.toDouble())
    }

    fun getTemperatureModeArray(): ArrayList<String> {
        val valuesList = ArrayList<String>()
        valuesList.add("Dual SetPoint Fixed Deaband")
        valuesList.add("Dual SetPoint Variable Deaband")
        return valuesList
    }
}