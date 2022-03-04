package a75f.io.device;

import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.diag.DiagEquip;

/**
 * This could have been done from DeviceUpdateJob. But having a dedicated status update job to avoids device status
 * not being available at backend if DeviceUpdateJob takes more than a minute due to HW delays and timeouts.
 */
class DeviceStatusUpdateJob extends BaseJob{
    public void doJob() {
        CcuLog.d(L.TAG_CCU_JOB, "DeviceStatusUpdateJob -> ");
        DiagEquip.getInstance().setDiagHisVal("serial and connection",
                                                LSerial.getInstance().isConnected() ? 1.0 :0);
        DiagEquip.getInstance().updatePoints();
        CcuLog.d(L.TAG_CCU_JOB, "<- DeviceStatusUpdateJob ");
    }
}
