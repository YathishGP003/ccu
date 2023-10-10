package a75f.io.renatus.externalahu

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



    fun render(modelDefinition: SeventyFiveFProfileDirective) {
        var pointDef = getPointByDomainName(modelDefinition,SET_POINT_CONTROL)

    }

    fun controlName(modelDefinition: SeventyFiveFProfileDirective, domainName: String): String {
        val pointDef = getPointByDomainName(modelDefinition,domainName)
        if (pointDef != null)
            return pointDef.name
        return NOT_FOUND
    }


    fun getPointByDomainName(modelDefinition: SeventyFiveFProfileDirective, domainName: String): SeventyFiveFProfilePointDef? {
        return modelDefinition.points.find { (it.domainName.contentEquals(domainName)) }
    }

}