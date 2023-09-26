package a75f.io.renatus.dabextahu

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

    var minSp: Int by mutableStateOf(0)
    var maxSp: Int by mutableStateOf(5)
    var heatingMinSp: Int by mutableStateOf(0)
    var heatingMaxSp: Int by mutableStateOf(5)
    var coolingMinSp: Int by mutableStateOf(0)
    var coolingMaxSp: Int by mutableStateOf(5)



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