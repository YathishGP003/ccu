package a75f.io.device.alerts;

import a75f.io.alerts.AlertManager;

import static a75f.io.alerts.AlertsConstantsKt.CM_DEAD;
import static a75f.io.alerts.AlertsConstantsKt.CM_ERROR_REPORT;
import static a75f.io.alerts.AlertsConstantsKt.CM_RESET;
import static a75f.io.alerts.AlertsConstantsKt.CM_TO_CCU_OVER_USB_SN_REBOOT;
import static a75f.io.alerts.AlertsConstantsKt.DEVICE_DEAD;
import static a75f.io.alerts.AlertsConstantsKt.DEVICE_LOW_SIGNAL;
import static a75f.io.alerts.AlertsConstantsKt.DEVICE_REBOOT;
import static a75f.io.alerts.AlertsConstantsKt.DEVICE_RESTART;
import static a75f.io.alerts.AlertsConstantsKt.FIRMWARE_OTA_UPDATE_ENDED;
import static a75f.io.alerts.AlertsConstantsKt.FIRMWARE_OTA_UPDATE_STARTED;
import static a75f.io.alerts.AlertsConstantsKt.FSV_REBOOT;

/**
 * Created by mahesh on 26-11-2019.
 */
public class AlertGenerateHandler {

   //

    /** NOTE:  Descriptions of the logic for triggering these alerts is given in AlertDefinition.  If you
     * add a new trigger, please update the description there.
     */
    public static void handleMessage(String cmd, String msg) {
            switch (cmd) {
                case FSV_REBOOT:
                    AlertManager.getInstance().generateAlert(FSV_REBOOT, msg);
                    break;
                case CM_ERROR_REPORT:
                    AlertManager.getInstance().generateAlert(CM_ERROR_REPORT, msg);
                    break;
                case CM_TO_CCU_OVER_USB_SN_REBOOT:
                    AlertManager.getInstance().generateAlert(CM_TO_CCU_OVER_USB_SN_REBOOT, msg);
                    break;
                case DEVICE_RESTART:
                    AlertManager.getInstance().generateAlert(DEVICE_RESTART, msg);
                    break;
                case CM_RESET:
                    AlertManager.getInstance().generateAlert(CM_RESET, msg);
                    break;
                case DEVICE_REBOOT:
                    AlertManager.getInstance().generateAlert(DEVICE_REBOOT, msg);
                    break;
                case FIRMWARE_OTA_UPDATE_STARTED:
                    AlertManager.getInstance().generateAlert(FIRMWARE_OTA_UPDATE_STARTED, msg);
                    break;
                case FIRMWARE_OTA_UPDATE_ENDED:
                    AlertManager.getInstance().generateAlert(FIRMWARE_OTA_UPDATE_ENDED, msg);
                    break;
                case CM_DEAD:
                    AlertManager.getInstance().generateCMDeadAlert(CM_DEAD, msg);
                    break;
                case DEVICE_DEAD:
                    AlertManager.getInstance().generateAlert(DEVICE_DEAD, msg);
                    break;
                case DEVICE_LOW_SIGNAL:
                    AlertManager.getInstance().generateAlert(DEVICE_LOW_SIGNAL, msg);
                    break;
            }
    }
}
