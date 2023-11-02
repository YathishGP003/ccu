package a75f.io.renatus.profiles.vav

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelSource
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.definitions.DamperShape
import a75f.io.logic.bo.building.definitions.DamperType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.definitions.ReheatType
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.ModbusLevel
import a75f.io.renatus.util.ProgressDialogUtils
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class VavProfileViewModel : ViewModel() {

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    private var deviceAddress by Delegates.notNull<Short>()

    lateinit var profileConfiguration: VavProfileConfiguration

    val damperTypes = DamperType.values().map { it.displayName }
    val damperSizes = (2..22 step 2).map { it.toString() }
    val reheatTypes = mutableListOf("Not Installed") + ReheatType.values().map { it.displayName }
    val zonePriorities = ZonePriority.values().map { it.name }
    val damperShapes = DamperShape.values().map { it.displayName }

    private lateinit var model : SeventyFiveFProfileDirective
    lateinit var viewState: VavConfigViewState

    private lateinit var context : Context
    lateinit var hayStack : CCUHsApi

    fun init(bundle: Bundle, context: Context, hayStack : CCUHsApi) {
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        val profileOriginalValue = bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)
        profileType = ProfileType.values()[profileOriginalValue]
        model = ModelSource.getModelByProfileName("smartnodeVAVReheatNoFan") as SeventyFiveFProfileDirective

        profileConfiguration = VavProfileConfiguration(deviceAddress.toInt(), NodeType.SMART_NODE.name, 0,
                                        zoneRef, floorRef , model ).getDefaultConfiguration()
        viewState = VavConfigViewState.fromVavProfileConfig(profileConfiguration)
        this.context = context
        this.hayStack = hayStack
        CcuLog.i("CCU_DOMAIN"," offset "+viewState.temperatureOffset)
    }


    fun saveConfiguration() {
        CcuLog.i("CCU_DOMAIN", " Save Profile : damperType ${viewState.damperType}")
        CcuLog.i("CCU_DOMAIN", " Save Profile : damperSize ${viewState.damperSize}")

        viewModelScope.launch {
            ProgressDialogUtils.showProgressDialog(context, "Saving VAV Configuration")
            withContext(Dispatchers.IO) {

                viewState.updateConfigFromViewState(profileConfiguration)
                val equipBuilder = ProfileEquipBuilder(hayStack)
                if (profileConfiguration.isDefault) {
                    equipBuilder.buildEquipAndPoints(profileConfiguration, model, hayStack.site!!.id)
                } else {
                    equipBuilder.updateEquipAndPoints(profileConfiguration, model, hayStack.site!!.id)
                }

                withContext(Dispatchers.Main) {
                    ProgressDialogUtils.hideProgressDialog()
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                }
            }
        }

    }
}
