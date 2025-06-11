package a75f.io.domain.logic

import a75f.io.api.haystack.BuildConfig
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.util.ModelLoader.getCCUBaseConfigurationModel
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags
import org.projecthaystack.HDateTime

class CCUBaseConfigurationBuilder(private val hayStack : CCUHsApi): DefaultEquipBuilder() {
    fun createCCUBaseConfiguration(ccuName : String,
                                   installerEmail : String,
                                   managerEmail : String,
                                   diagEquipId: String,
                                   ccuConfigurationModelDef: ModelDirective
    ): String {
        CcuLog.i(Domain.LOG_TAG, "Creating CCU Base Configuration $ccuName")
        val siteRef = hayStack.siteIdRef.toString()

        /*This is main CCU-Device, this is physical entity this CCU-Device will deleted and added
        * when CCU is re-registered and un-registered,
        *  This ccuDeviceId is ccuRef for all ccu related entities*/
        val ccuDeviceId = CCUDeviceBuilder().buildCCUDevice(
            diagEquipId,
            siteRef,
            ccuName,
            installerEmail,
            managerEmail,
            Domain.checkSystemEquipInitialisedAndGetId()
        )
        createCCUEquip(ccuConfigurationModelDef, ccuName)
        return ccuDeviceId
    }

    fun createCCUEquip(ccuConfigurationModelDef: ModelDirective, ccuName: String) {
        val ccuConfigurationModel = ccuConfigurationModelDef as SeventyFiveFProfileDirective
        val profileConfiguration = CCUEquipConfiguration(ccuConfigurationModel, hayStack).getDefaultConfiguration()

        /* CCU-Equip will be logical entity all CCU related points are children of this equip
        * And all these point's equipRef is CCU-Equip's ID */
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipId = equipBuilder.buildEquipAndPoints(
            profileConfiguration, ccuConfigurationModel, hayStack.site!!
                .id, "$ccuName-${ccuConfigurationModel.name}"
        )
        CcuLog.i(Domain.LOG_TAG, "Created equipId $equipId")

        DomainManager.addCCUEquip(hayStack)
        updateLogLevelPointBasedOnBuildType()
    }

    private fun updateLogLevelPointBasedOnBuildType() {
        val isCCUBuildInNonProd = isCCUBuildInNonProdEnvironment()
        if(isCCUBuildInNonProd) {
            Domain.ccuEquip.logLevel.writeDefaultVal(0)
        }
    }

     private fun isCCUBuildInNonProdEnvironment(): Boolean {
         return BuildConfig.BUILD_TYPE == "dev" || BuildConfig.BUILD_TYPE == "qa" ||
                 BuildConfig.BUILD_TYPE == "dev_qa" || BuildConfig.BUILD_TYPE == "staging"
     }

    fun updateCcuConfigAhuRef(systemEquipId: String) {
        val ccuName = Domain.ccuDevice.ccuDisName
        val hayStackEquip = getCcuEquip(ccuName)
        val ccuEquipId = Domain.ccuEquip.getId()
        hayStackEquip.gatewayRef = systemEquipId
        hayStackEquip.ahuRef = systemEquipId
        hayStackEquip.lastModifiedDateTime = HDateTime.make(System.currentTimeMillis())
        hayStackEquip.id = ccuEquipId
        hayStack.updateEquip(hayStackEquip, ccuEquipId)
    }

    fun getCcuEquip(ccuName: String): Equip {
        val siteRef = hayStack.siteIdRef.toString()
        val tz = hayStack.timeZone
        val ccuConfigModelDef = getCCUBaseConfigurationModel()
        val profileConfiguration = CCUEquipConfiguration(ccuConfigModelDef as SeventyFiveFProfileDirective, hayStack).getDefaultConfiguration()
        return buildEquip(EquipBuilderConfig(ccuConfigModelDef, profileConfiguration, siteRef,tz,
            "$ccuName-${ccuConfigModelDef.name}")
        )
    }

    /*
    * IN few CCU's addressBand point is not present , so creating it here
    * This is one time operating in CCU's life*/
    fun createAddressBandPoint(ccuEquip: HashMap<Any, Any>) {
        val ccuBaseConfigurationModel = getCCUBaseConfigurationModel()

        val modelPointDef = ccuBaseConfigurationModel.points.find { it.domainName == DomainName.addressBand }

        val profileConfiguration = CCUEquipConfiguration(ccuBaseConfigurationModel as SeventyFiveFProfileDirective,
            hayStack).getDefaultConfiguration()

        createPointFromDef(PointBuilderConfig(modelPointDef!!, profileConfiguration, ccuEquip[Tags.ID].toString(),
            hayStack.siteIdRef.toString(), hayStack.timeZone, "${hayStack.ccuName}-${ccuBaseConfigurationModel.name}"))
    }
}