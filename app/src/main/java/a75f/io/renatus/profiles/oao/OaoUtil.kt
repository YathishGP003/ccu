package a75f.io.renatus.profiles.oao

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.oao.OAOProfileConfiguration
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective


fun deleteUnusedSystemPoints() {
    val oaoEquip = CCUHsApi.getInstance().readEntity("equip and oao")
    if (oaoEquip.isEmpty()) {
        CcuLog.d(Domain.LOG_TAG, "oao is not paired")
        return
    }
    if (getSystemProfileType() == Tags.DAB) {
        CCUHsApi.getInstance().deleteEntity(
            Domain
                .readPointForEquip(
                    DomainName.systemPurgeVavDamperMinOpenMultiplier,
                    oaoEquip["id"].toString()
                )["id"].toString()
        )
        CCUHsApi.getInstance().deleteEntity(
            Domain
                .readPointForEquip(
                    DomainName.systemPurgeVavMinFanLoopOutput,
                    oaoEquip["id"].toString()
                )["id"].toString()
        )
        CcuLog.d(
            Domain.LOG_TAG,
            "Deleting systemPurgeVavDamperMinOpenMultiplier and systemPurgeVavMinFanLoopOutput points"
        )
    } else if (getSystemProfileType() == Tags.VAV) {
        CCUHsApi.getInstance().deleteEntity(
            Domain
                .readPointForEquip(
                    DomainName.systemPurgeDabDamperMinOpenMultiplier,
                    oaoEquip["id"].toString()
                )["id"].toString()
        )
        CCUHsApi.getInstance().deleteEntity(
            Domain
                .readPointForEquip(
                    DomainName.systemPurgeDabMinFanLoopOutput,
                    oaoEquip["id"].toString()
                )["id"].toString()
        )
        CcuLog.d(
            Domain.LOG_TAG,
            "Deleting systemPurgeDabDamperMinOpenMultiplier and systemPurgeDabMinFanLoopOutput points"
        )
    }
}

private fun getSystemProfileType(): String {
    val profileType = L.ccu().systemProfile.profileType
    return when (profileType) {
        ProfileType.SYSTEM_DAB_ANALOG_RTU, ProfileType.SYSTEM_DAB_HYBRID_RTU, ProfileType.SYSTEM_DAB_STAGED_RTU, ProfileType.SYSTEM_DAB_STAGED_VFD_RTU, ProfileType.dabExternalAHUController, ProfileType.SYSTEM_DAB_ADVANCED_AHU -> Tags.DAB
        ProfileType.SYSTEM_VAV_ANALOG_RTU, ProfileType.SYSTEM_VAV_HYBRID_RTU, ProfileType.SYSTEM_VAV_IE_RTU, ProfileType.SYSTEM_VAV_STAGED_RTU, ProfileType.SYSTEM_VAV_STAGED_VFD_RTU, ProfileType.SYSTEM_VAV_ADVANCED_AHU, ProfileType.vavExternalAHUController -> Tags.VAV
        else -> {
            Tags.DEFAULT
        }
    }
}

fun updateOaoPoints() {
    val oaoEquip = CCUHsApi.getInstance().readEntity("equip and oao")
    try {
        if (oaoEquip.isNotEmpty()) {
            deleteUnusedSystemPoints()
            val model = ModelLoader.getSmartNodeOAOModelDef() as SeventyFiveFProfileDirective
            val profileConfiguration = OAOProfileConfiguration(
                oaoEquip["group"].toString().toInt(), nodeType = NodeType.SMART_NODE.toString(), 0,
                "SYSTEM", "SYSTEM", ProfileType.OAO, model
            ).getActiveConfiguration()
            val equipDis =
                CCUHsApi.getInstance().siteName + "-OAO-" + profileConfiguration.nodeAddress

            fun createPoint(domainName: String) {
                Domain.createDomainPoint(
                    model,
                    profileConfiguration,
                    oaoEquip["id"].toString(),
                    CCUHsApi.getInstance().site!!.id,
                    CCUHsApi.getInstance().site!!.tz,
                    equipDis,
                    domainName
                )
            }
            if (getSystemProfileType() == Tags.DAB) {
                if(Domain.readPointForEquip(
                    DomainName.systemPurgeDabDamperMinOpenMultiplier,
                    oaoEquip["id"].toString()
                ).isEmpty()) {
                    createPoint(DomainName.systemPurgeDabDamperMinOpenMultiplier)
                }
                if(Domain.readPointForEquip(
                    DomainName.systemPurgeDabMinFanLoopOutput,
                    oaoEquip["id"].toString()
                ).isEmpty()) {
                    createPoint(DomainName.systemPurgeDabMinFanLoopOutput)
                }
            } else if (getSystemProfileType() == Tags.VAV) {
                if(Domain.readPointForEquip(
                    DomainName.systemPurgeVavDamperMinOpenMultiplier,
                    oaoEquip["id"].toString()
                ).isEmpty()) {
                    createPoint(DomainName.systemPurgeVavDamperMinOpenMultiplier)
                }
                if(Domain.readPointForEquip(
                    DomainName.systemPurgeVavMinFanLoopOutput,
                    oaoEquip["id"].toString()
                ).isEmpty()) {
                    createPoint(DomainName.systemPurgeVavMinFanLoopOutput)
                }
            }
        }
    } catch (exception: Exception) {
        CcuLog.e(Domain.LOG_TAG, "Error updating OAO points: ${exception.message}")
        exception.printStackTrace()
    }
}



