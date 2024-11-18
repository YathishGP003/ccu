package a75f.io.device

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.diag.DiagEquip

/**
 * This could have been done from DeviceUpdateJob. But having a dedicated status update job to avoids device status
 * not being available at backend if DeviceUpdateJob takes more than a minute due to HW delays and timeouts.
 */
internal class DiagUpdateJob : BaseJob() {
    override fun doJob() {
        CcuLog.d(L.TAG_CCU_JOB, "diagUpdateJob -> ")
        val diagEquip =
            CCUHsApi.getInstance().readEntity("domainName == \"" + DomainName.diagEquip + "\"")
        if (diagEquip.isNotEmpty() && Domain.isDiagEquipInitialised()) {
            Domain.diagEquip.serialConnection.writeHisVal(if (LSerial.getInstance().isConnected) 1.0 else 0.0)
            DiagEquip.getInstance().updatePoints()
        } else {
            CcuLog.d(L.TAG_CCU_JOB, "diagUpdateJob: diagEquip not found")
        }
        CcuLog.d(L.TAG_CCU_JOB, "<- diagUpdateJob ")
    }
}
