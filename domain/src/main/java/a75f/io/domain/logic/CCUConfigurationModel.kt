package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import androidx.lifecycle.ViewModel
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

class CCUConfigurationModel(
    private val ccuBaseModel: SeventyFiveFProfileDirective,
    val hayStack: CCUHsApi
) : ViewModel() {
    private lateinit var ccuEquipConfiguration: CCUEquipConfiguration

    fun handleDemandResponseState(isDREnrollmentEnabled: Boolean) {
        ccuEquipConfiguration = CCUEquipConfiguration(ccuBaseModel, hayStack)
        ccuEquipConfiguration.getActiveConfiguration().drEnrollment.enabled = isDREnrollmentEnabled
        saveConfiguration()
    }

    private fun saveConfiguration() {
        val ccuEquip = Domain.ccuEquip
        val equipBuilder = ProfileEquipBuilder(hayStack)

        equipBuilder.updateEquipAndPoints(
            ccuEquipConfiguration, ccuBaseModel,
            hayStack.getSiteIdRef().toString(), ccuEquip.disName, true
        )
        hayStack.scheduleSync()
    }
}