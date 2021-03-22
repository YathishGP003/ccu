package a75f.io.device.alerts;

import a75f.io.alerts.AlertManager;
import a75f.io.logic.Globals;

/**
 * Created by mahesh on 26-11-2019.
 */
public class AlertGenerateHandler {

   //
    public static final String FSV_REBOOT = "fsvReboot";
    public static final String CM_ERROR_REPORT = "CM ERROR REPORT";
    public static final String CM_TO_CCU_OVER_USB_SN_REBOOT = "snReboot";
    public static final String DEVICE_RESTART = "DEVICE RESTART COMMAND";
    public static final String CM_RESET = "CM RESET";
    public static final String DEVICE_REBOOT = "DEVICE REBOOT";
    public static final String FIRMWARE_OTA_UPDATE_STARTED = "FIRMWARE OTA UPDATE STARTED";
    public static final String FIRMWARE_OTA_UPDATE_ENDED = "FIRMWARE OTA UPDATE ENDED";
    public static final String CM_DEAD = "CM DEAD";
    public static final String DEVICE_DEAD = "DEVICE DEAD";
    public static final String DEVICE_LOW_SIGNAL = "DEVICE LOW SIGNAL";

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
