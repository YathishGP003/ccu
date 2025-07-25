package a75f.io.device.alerts;

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

import java.util.HashMap;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;

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
                case CM_DEAD:
                    AlertManager.getInstance().generateCMDeadAlert(CM_DEAD, msg);
                    break;
            }
    }

    public static void handleDeviceMessage(String cmd, String msg, String deviceRef) {
        AlertManager alertManager = AlertManager.getInstance();
        switch (cmd) {
            case DEVICE_REBOOT:
                alertManager.fixActiveDeviceRebootAlert(deviceRef);
                alertManager.generateAlert(DEVICE_REBOOT, msg, getEquipRef(deviceRef));
                break;
            case DEVICE_DEAD:
                alertManager.generateAlert(DEVICE_DEAD, msg, getEquipRef(deviceRef));
                break;
            case DEVICE_LOW_SIGNAL:
                alertManager.generateAlert(DEVICE_LOW_SIGNAL, msg, getEquipRef(deviceRef));
                break;
            case FIRMWARE_OTA_UPDATE_STARTED:
                alertManager.generateAlert(FIRMWARE_OTA_UPDATE_STARTED, msg, getEquipRef(deviceRef));
                break;
            case FIRMWARE_OTA_UPDATE_ENDED:
                alertManager.generateAlert(FIRMWARE_OTA_UPDATE_ENDED, msg, getEquipRef(deviceRef));
                break;
        }
    }

    private static String getEquipRef(String deviceRef) {
        HashMap<Object, Object> device = CCUHsApi.getInstance().readMapById(deviceRef);
        if (device == null) {
            return deviceRef;
        }
        if (device.containsKey(Tags.CONNECTMODULE) && device.containsKey("lowCode")) {
            return deviceRef;
        }
        // for devices like CN, PCN, we dont have equipRef in device
        return device.get("equipRef") != null ? device.get("equipRef").toString() : "";
    }

}
