package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.SystemMode
import a75f.io.logic.bo.building.system.vav.VavStagedRtu
import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.SystemProfileUtil
import android.app.Activity
import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VavStagedRtuViewModel : StagedRtuProfileViewModel() {
    fun init(context: Context, hayStack : CCUHsApi) {
        super.init(context, ModelLoader.getVavStageRtuModelDef(), hayStack)

        CcuLog.i(Domain.LOG_TAG, "VavStagedRtuViewModel Init")

        val systemEquip = hayStack.readEntity("system and equip and not modbus") //TODO - via domain
        CcuLog.i(Domain.LOG_TAG, "Current System Equip $systemEquip")

        if (systemEquip["profile"].toString() == "vavStagedRtu" ||
            systemEquip["profile"].toString() == ProfileType.SYSTEM_VAV_STAGED_RTU.name) {
            CcuLog.i(Domain.LOG_TAG, "Get active config for systemEquip")
            profileConfiguration = StagedRtuProfileConfig(model).getActiveConfiguration()
        } else {
            CcuLog.i(Domain.LOG_TAG, "Get default config for systemEquip")
            profileConfiguration = StagedRtuProfileConfig(model).getDefaultConfiguration()
            val newEquipId = createNewEquip(systemEquip["id"].toString())
            L.ccu().systemProfile = VavStagedRtu()
            L.ccu().systemProfile.addSystemEquip()
            L.ccu().systemProfile.updateAhuRef(newEquipId)
        }
        val stagedRtu = L.ccu().systemProfile as VavStagedRtu
        stagedRtu.updateStagesSelected()

        CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
        viewState = StagedRtuViewState.fromProfileConfig(profileConfiguration)
        CcuLog.i(Domain.LOG_TAG, "VavStagedRtuViewModel Loaded")
        modelLoaded = true
    }

    private fun initializeLists() {
        relay1AssociationList = Domain.getListByDomainName(DomainName.relay1OutputAssociation, model)
        relay2AssociationList = Domain.getListByDomainName(DomainName.relay2OutputAssociation, model)
        relay3AssociationList = Domain.getListByDomainName(DomainName.relay3OutputAssociation, model)
        relay4AssociationList = Domain.getListByDomainName(DomainName.relay4OutputAssociation, model)
        relay5AssociationList = Domain.getListByDomainName(DomainName.relay5OutputAssociation, model)
        relay6AssociationList = Domain.getListByDomainName(DomainName.relay6OutputAssociation, model)
        relay7AssociationList = Domain.getListByDomainName(DomainName.relay7OutputAssociation, model)
    }

    override fun saveConfiguration() {
        viewModelScope.launch {
            ProgressDialogUtils.showProgressDialog(context, "Saving VAV Configuration")
            withContext(Dispatchers.IO) {
                super.saveConfiguration()
                val stagedRtu = L.ccu().systemProfile as VavStagedRtu
                stagedRtu.updateStagesSelected()
                DesiredTempDisplayMode.setSystemModeForVav(hayStack)
                hayStack.syncEntityTree()
                withContext(Dispatchers.Main) {
                    ProgressDialogUtils.hideProgressDialog()
                    updateSystemMode()
                }
            }
        }
    }
}