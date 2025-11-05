package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.StringUtil.addAtSymbolIfMissing
import a75f.io.constants.CcuFieldConstants
import a75f.io.domain.api.Domain
import org.projecthaystack.HDateTime
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HRef
import org.projecthaystack.HStr
import java.util.UUID

class CCUDeviceBuilder {
    fun buildCCUDevice(
        equipRef: String,
        siteRef: String,
        ccuName: String,
        installerEmail: String,
        managerEmail: String,
        ahuRef: String
    ): String {
        val ccuDevice = CCUHsApi.getInstance().readEntity("ccu and device")
        val hsApi = Domain.hayStack

        val hDictBuilder = HDictBuilder()
        val localId = if(ccuDevice.isNotEmpty()) { ccuDevice[Tags.ID].toString() } else { UUID.randomUUID().toString() }
        val cleanedCCURef = localId.replaceFirst("@", "")
        hDictBuilder.add(HayStackConstants.ID, HRef.make(cleanedCCURef))
        hDictBuilder.add(CcuFieldConstants.DESCRIPTION, HStr.make(ccuName))
        hDictBuilder.add(CcuFieldConstants.INSTALLER_EMAIL, HStr.make(installerEmail))
        hDictBuilder.add(CcuFieldConstants.FACILITY_MANAGER_EMAIL, HStr.make(managerEmail))
        hDictBuilder.add(CcuFieldConstants.SITEREF, siteRef)
        hDictBuilder.add(CcuFieldConstants.EQUIPREF, equipRef)
        hDictBuilder.add(CcuFieldConstants.CREATED_DATE, HDateTime.make(System.currentTimeMillis()).date)
        hDictBuilder.add(CcuFieldConstants.LAST_MODIFIED_DATE, HDateTime.make(System.currentTimeMillis()))
        hDictBuilder.add(Tags.DEVICE)
        hDictBuilder.add(Tags.CCU)
        hDictBuilder.add(CcuFieldConstants.GATEWAYREF, ahuRef)
        hDictBuilder.add(CcuFieldConstants.AHUREF, ahuRef)
        hsApi.tagsDb.addHDict(cleanedCCURef, hDictBuilder.toDict())
        if(ccuDevice.isNotEmpty()){
            hsApi.syncStatusService.addUpdatedEntity(
                addAtSymbolIfMissing(localId)
            )
            hsApi.scheduleSync()
        } else {
            hsApi.syncStatusService.addUnSyncedEntity(
                addAtSymbolIfMissing(localId)
            )
            hsApi.addCCURefForDiagAndSystemEntities()
        }
        DomainManager.addCCUDevice(hsApi)
        return localId
    }
}