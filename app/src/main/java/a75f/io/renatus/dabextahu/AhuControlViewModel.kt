package a75f.io.renatus.dabextahu

import a75f.io.domain.config.AdvancedAhuConfiguration
import a75f.io.domain.util.ModelNames
import a75f.io.domain.util.ModelSource.Companion.getModelByProfileName
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

/**
 * Created by Manjunath K on 08-08-2022.
 */

class AhuControlViewModel(application: Application) : AndroidViewModel(application) {


    var setPointControl: Boolean by mutableStateOf(true)
    var dualSetPointControl: Boolean by mutableStateOf(true)
    var fanStaticSetPointControl: Boolean by mutableStateOf(true)
    var dcvControl: Boolean by mutableStateOf(true)
    var occupancyMode: Boolean by mutableStateOf(true)
    var humidifierControl: Boolean by mutableStateOf(true)
    var dehumidifierControl: Boolean by mutableStateOf(true)

    var heatingMinSp: String by mutableStateOf("1.0")
    var heatingMaxSp: String by mutableStateOf("1.0")
    var coolingMinSp: String by mutableStateOf("1.0")
    var coolingMaxSp: String by mutableStateOf("1.0")

    //private val domainModeler = DomainModeler(application.baseContext)


    fun configModelDefinition(nodeType: NodeType, profile: ProfileType, context: Context) {
        /*var modelDef = getModelByProfileName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        Log.i("DAB_EXT", "configModelDefinition:${modelDef!!.modelType} ")
        Log.i("DAB_EXT", "configModelDefinition:${modelDef.modelType} ")
        Log.i("DAB_EXT", "configModelDefinition:${modelDef.modelType} ")
        Log.i("DAB_EXT", "configModelDefinition:${modelDef.modelType} ")*/

    }


    fun saveConfiguration() {
        val profile = AdvancedAhuConfiguration(1000, "HS", 0, "", "")
        //domainModeler.addEquip(profileConfiguration = profile)
        //Log.i("Domain", "save configuration: ${getValues()}")

    }

    private fun getValues(): String {
        return "setPointControl: $setPointControl " +
                "dualSetPointControl $dualSetPointControl " +
                "heatingMinSp $heatingMinSp heatingMaxSp $heatingMaxSp " +
                "coolingMinSp $coolingMinSp coolingMaxSp $coolingMaxSp "
    }


    fun getOptions(): List<String> {
        // TODO read it from model profile definition
        return listOf("1.0", "2.0", "3.0", "4.0", "5.0")
    }

    fun getIndexFromVal(value: String): Int {
        return getOptions().indexOf(value)
    }

}