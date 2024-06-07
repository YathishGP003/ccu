package a75f.io.renatus.util

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName

class TemperatureModeModel {
    fun getTemperatureMode(): Double {
        return Domain.readDefaultValByDomain(DomainName.temperatureMode)
    }
    fun setTemperatureMode(position: Int) {
        Domain.writeDefaultValByDomain(DomainName.temperatureMode, position.toDouble())
    }

    fun getTemperatureModeArray(): ArrayList<String> {
        val valuesList = ArrayList<String>()
        valuesList.add("Dual SetPoint Fixed Deaband")
        valuesList.add("Dual SetPoint Variable Deaband")
        return valuesList
    }
}