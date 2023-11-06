package a75f.io.renatus.profiles.vav

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain.getListByDomainName
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelSource
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.util.ProgressDialogUtils
import android.content.Context
import android.content.Intent
import android.os.Bundle
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

    private lateinit var model : SeventyFiveFProfileDirective
    lateinit var viewState: VavConfigViewState

    private lateinit var context : Context
    lateinit var hayStack : CCUHsApi

    lateinit var damperTypesList: List<String>
    lateinit var damperSizesList: List<String>
    lateinit var damperShapesList: List<String>
    lateinit var reheatTypesList: List<String>
    lateinit var zonePrioritiesList: List<String>

    lateinit var temperatureOffsetsList: List<String>

    lateinit var maxCoolingDamperPosList: List<String>
    lateinit var minCoolingDamperPosList: List<String>
    lateinit var maxHeatingDamperPosList: List<String>
    lateinit var minHeatingDamperPosList: List<String>

    lateinit var kFactorsList: List<String>

    lateinit var maxCFMCoolingList: List<String>
    lateinit var minCFMCoolingList: List<String>
    lateinit var maxCFMReheatingList: List<String>
    lateinit var minCFMReheatingList: List<String>

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

        initializeLists()

    }

    private fun initializeLists() {
        damperTypesList = getListByDomainName(a75f.io.domain.api.damperType, model)
        damperSizesList = getListByDomainName(a75f.io.domain.api.damperSize, model)
        damperShapesList = getListByDomainName(a75f.io.domain.api.damperShape, model)
        reheatTypesList = getListByDomainName(a75f.io.domain.api.reheatType, model)
        zonePrioritiesList = getListByDomainName(a75f.io.domain.api.zonePriority, model)

        temperatureOffsetsList = getListByDomainName(a75f.io.domain.api.temperatureOffset, model)

        maxCoolingDamperPosList = getListByDomainName(a75f.io.domain.api.maxCoolingDamperPos, model)
        minCoolingDamperPosList = getListByDomainName(a75f.io.domain.api.minCoolingDamperPos, model)
        maxHeatingDamperPosList = getListByDomainName(a75f.io.domain.api.maxHeatingDamperPos, model)
        minHeatingDamperPosList = getListByDomainName(a75f.io.domain.api.minHeatingDamperPos, model)

        // TODO: In v0.0.1 of the model, this is a 2000-item dropdown. Very slow, but it doesn't crash the CCU.
        kFactorsList = getListByDomainName(a75f.io.domain.api.kFactor, model)

        maxCFMCoolingList = getListByDomainName(a75f.io.domain.api.maxCFMCooling, model)
        minCFMCoolingList = getListByDomainName(a75f.io.domain.api.minCFMCooling, model)
        maxCFMReheatingList = getListByDomainName(a75f.io.domain.api.maxCFMReheating, model)
        minCFMReheatingList = getListByDomainName(a75f.io.domain.api.minCFMReheating, model)
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
