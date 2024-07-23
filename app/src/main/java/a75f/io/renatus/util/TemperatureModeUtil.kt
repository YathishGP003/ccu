package a75f.io.renatus.util

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.logic.tuners.TunerConstants

class TemperatureModeUtil {
    fun getTemperatureMode(): Double {
        return Domain.readValAtLevelByDomain(DomainName.temperatureMode,
            TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)
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