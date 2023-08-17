package a75f.io.renatus

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
    var heatingMinSp: String by mutableStateOf("1.0")
    var heatingMaxSp: String by mutableStateOf("1.0")
    var coolingMinSp: String by mutableStateOf("1.0")
    var coolingMaxSp: String by mutableStateOf("1.0")

    //private val domainModeler = DomainModeler(application.baseContext)


    fun configModelDefinition(nodeType: NodeType, profile: ProfileType, context: Context){
       // TODO read model definition from domain model based on profile & device selected initialise to base configuration
    }


    fun saveConfiguration() {
       /* val profile = AdvancedAhuConfiguration(1000,"HS",0, "","")
        domainModeler.addEquip(profileConfiguration = profile)
        Log.i("Domain", "save configuration: ${getValues()}")
*/
    }
    private fun getValues(): String {
        return "setPointControl: $setPointControl " +
                "dualSetPointControl $dualSetPointControl "+
                "heatingMinSp $heatingMinSp heatingMaxSp $heatingMaxSp "+
                "coolingMinSp $coolingMinSp coolingMaxSp $coolingMaxSp "
    }


    fun getOptions(): List<String> {
        // TODO read it from model profile definition
        return listOf("1.0", "2.0", "3.0","4.0","5.0")
    }

    fun getIndexFromVal(value: String): Int {
        return getOptions().indexOf(value)
    }

}