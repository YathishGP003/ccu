package a75f.io.renatus.externalahu

import a75f.io.domain.api.DomainName.systemCoolingSATMaximum
import a75f.io.domain.api.DomainName.systemCoolingSATMinimum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMinimum
import a75f.io.domain.api.DomainName.systemHeatingSATMaximum
import a75f.io.domain.api.DomainName.systemHeatingSATMinimum
import a75f.io.domain.api.DomainName.systemSATMaximum
import a75f.io.domain.api.DomainName.systemSATMinimum
import a75f.io.domain.api.DomainName.systemStaticPressureMaximum
import a75f.io.domain.api.DomainName.systemStaticPressureMinimum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMaximum
import a75f.io.domain.config.ExternalAhuConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef

/**
 * Created by Manjunath K on 22-09-2023.
 */

class ExternalAhuConfigModel {

    var setPointControl: Boolean by mutableStateOf(false)
    var dualSetPointControl: Boolean by mutableStateOf(false)
    var fanStaticSetPointControl: Boolean by mutableStateOf(false)
    var dcvControl: Boolean by mutableStateOf(false)
    var occupancyMode: Boolean by mutableStateOf(false)
    var humidifierControl: Boolean by mutableStateOf(false)
    var dehumidifierControl: Boolean by mutableStateOf(false)

    var satMin: String by mutableStateOf("0")
    var satMax: String by mutableStateOf("0")
    var heatingMinSp: String by mutableStateOf("0")
    var heatingMaxSp: String by mutableStateOf("0")
    var coolingMinSp: String by mutableStateOf("0")
    var coolingMaxSp: String by mutableStateOf("0")
    var fanMinSp: String by mutableStateOf("0")
    var fanMaxSp: String by mutableStateOf("0")
    var dcvMin: String by mutableStateOf("0")
    var dcvMax: String by mutableStateOf("0")
    var targetHumidity: String by mutableStateOf("0")
    var targetDeHumidity: String by mutableStateOf("0")


    fun controlName(modelDefinition: SeventyFiveFProfileDirective, domainName: String): String {
        val pointDef = getPointByDomainName(modelDefinition,domainName)
        if (pointDef != null)
            return pointDef.name
        return NOT_FOUND
    }


    fun getPointByDomainName(modelDefinition: SeventyFiveFProfileDirective, domainName: String): SeventyFiveFProfilePointDef? {
        return modelDefinition.points.find { (it.domainName.contentEquals(domainName)) }
    }

    fun getConfiguration(): ExternalAhuConfiguration {
        val config =  ExternalAhuConfiguration()
        config.setPointControl.enabled = this.setPointControl
        config.dualSetPointControl.enabled = this.dualSetPointControl
        config.fanStaticSetPointControl.enabled = this.fanStaticSetPointControl
        config.dcvControl.enabled = this.dcvControl
        config.occupancyMode.enabled = this.occupancyMode
        config.humidifierControl.enabled = this.humidifierControl
        config.dehumidifierControl.enabled = this.dehumidifierControl

        config.satMin.currentVal = this.satMin.toDouble()
        config.satMax.currentVal = this.satMax.toDouble()
        config.heatingMinSp.currentVal = this.heatingMinSp.toDouble()
        config.heatingMaxSp.currentVal = this.heatingMaxSp.toDouble()
        config.coolingMinSp.currentVal = this.coolingMinSp.toDouble()
        config.coolingMaxSp.currentVal = this.coolingMaxSp.toDouble()
        config.fanMinSp.currentVal = this.fanMinSp.toDouble()
        config.fanMaxSp.currentVal = this.fanMaxSp.toDouble()
        config.dcvMin.currentVal = this.dcvMin.toDouble()
        config.dcvMax.currentVal = this.dcvMax.toDouble()

        return config
    }

    fun toConfig(modelDef: SeventyFiveFProfileDirective){
        satMin = getPointByDomainName(modelDef,systemSATMinimum)?.defaultValue.toString()
        satMax = getPointByDomainName(modelDef,systemSATMaximum)?.defaultValue.toString()
        heatingMinSp = getPointByDomainName(modelDef,systemHeatingSATMinimum)?.defaultValue.toString()
        heatingMaxSp = getPointByDomainName(modelDef,systemHeatingSATMaximum)?.defaultValue.toString()
        coolingMinSp = getPointByDomainName(modelDef,systemCoolingSATMinimum)?.defaultValue.toString()
        coolingMaxSp = getPointByDomainName(modelDef,systemCoolingSATMaximum)?.defaultValue.toString()
        fanMinSp = getPointByDomainName(modelDef,systemStaticPressureMinimum)?.defaultValue.toString()
        fanMaxSp = getPointByDomainName(modelDef,systemStaticPressureMaximum)?.defaultValue.toString()
        dcvMin = getPointByDomainName(modelDef,systemDCVDamperPosMinimum)?.defaultValue.toString()
        dcvMax = getPointByDomainName(modelDef,systemDCVDamperPosMaximum)?.defaultValue.toString()
    }
}