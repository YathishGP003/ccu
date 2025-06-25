package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import java.lang.NullPointerException

class CCUEquipConfiguration (private val ccuBaseModel : SeventyFiveFProfileDirective?, val ccuHsApi : CCUHsApi) :
    ProfileConfiguration(0, "", 0, "SYSTEM",
        "SYSTEM", ccuBaseModel?.domainName ?: ""){
    lateinit var drEnrollment: EnableConfig

    override fun getDependencies(): List<ValueConfig> {
        TODO("Not yet implemented")
    }
    override fun getEnableConfigs() : List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(drEnrollment)
        }
    }
    fun getActiveConfiguration() : CCUEquipConfiguration {
        getDefaultConfiguration()
        val ccuEquipConfiguration = Domain.ccuEquip
        drEnrollment.enabled = ccuEquipConfiguration.demandResponseActivation.readDefaultVal() > 0
        return this
    }

    fun getDefaultConfiguration(): CCUEquipConfiguration {
        drEnrollment = getDefaultEnableConfig(DomainName.demandResponseEnrollment, ccuBaseModel!!)
        return this
    }

    fun saveDemandResponseEnrollmentStatus(isDREnabled : Boolean){
        CcuLog.i(Domain.LOG_TAG, "saveDemandResponseEnrollmentStatus: $isDREnabled")

        val ccuConfigurationModel =  CCUConfigurationModel(
            ModelLoader.getCCUBaseConfigurationModel() as (SeventyFiveFProfileDirective) , ccuHsApi)

        ccuConfigurationModel.handleDemandResponseState(isDREnabled)
    }
}