package a75f.io.alerts;

/**
 * Created by samjithsadasivan on 4/23/18.
 */

import android.content.SharedPreferences;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import a75f.io.alerts.model.AlertScope;
import a75f.io.alerts.model.AlertsModelUtilKt;
import a75f.io.api.haystack.Alert;
import a75f.io.logger.CcuLog;
/**
 * The format for alerts defined in json.
 */

/**
 * A sample definition
 *
    [
        {
            "conditionals":[
                             {
                             "order" : "1",
                             "key" : “current and temp and his“,
                             "value" : “80”,
                             "condition" :”>”
                             },
                             {
                             "order" : "2",
                             "operator" : "&&"
                             },
                             {
                             "order" : "3",
                             "key" : "current and temp and his“,
                             "value" : “100”,
                             "condition": "<"
                             }
                            ],
             "offset": "0",
             "alert": {
             
                         "mTitle": “Temperature breach detected“,
                         "mMessage": "Equip #equipname1 is reporting a temperature of #pointval1 which is greater than  #condval1 and less than #condval3”,
                         "mNotificationMsg": "Equip #equipname1 is reporting a temperature of #pointval1 which is greater than  #condval1 and less than #condval3",
                         "mSeverity": “WARN”,
                         "mEnabled": "true"
                         }
        }
    ]
 */

public class AlertDefinition
{
    String _id;
    
    public ArrayList<Conditional> conditionals;
    public String                 offset;
    public Alert                  alert;
    public boolean                custom;
    public AlertScope             alertScope;

    public AlertDefinition(){
    
    }

    /** evaluate this alert definition against current values
     * @return a string describing the evaluation, for inspection */
    public void evaluate(SharedPreferences sharedPrefs) {
        for (Conditional c : conditionals)
        {
            if (c.operator == null)
            {
                c.evaluate(sharedPrefs);
            }
        }
    }

    public boolean validate() {
        if (conditionals.size() % 2 == 0) {
            logValidatation("Incorrect number of conditionals "+conditionals.size());
            return false;
        }
        
        for (int i = 0; i < conditionals.size() ; i+=2) {
            Conditional c = conditionals.get(i);
            if ( (c.isThisOperator() && c.operator != null) || (c.isThisCondition() && c.operator == null)) {
                logValidatation("Operator not allowed "+c.toString());
                return false;
            }
            if (c.isThisCondition() && (c.grpOperation != null) && !c.grpOperation.equals("") && !c.grpOperation.equals("equip") && !c.grpOperation.equals("average")
                        && !c.grpOperation.contains("top") && !c.grpOperation.contains("bottom")
                        && !c.grpOperation.contains("min") && !c.grpOperation.contains("max")) {
                logValidatation("grpOperator not supported "+c.grpOperation);
                return false;
            }
        }
        if (alert.mSeverity == null) {
            logValidatation("missing severity level");
            return false;
        }

        return true;
    }

    private void logValidatation(String msg) {
        CcuLog.e("CCU_ALERTS","Invalid Alert Definition : "+msg);
    }

    private boolean isInternalLogic() {
        return conditionals.get(0).grpOperation.equals("alert");
    }

    private String logicDescription() {
        switch (alert.mTitle) {
            case AlertsConstantsKt.FSV_REBOOT:
            case AlertsConstantsKt.CM_ERROR_REPORT:
            case AlertsConstantsKt.CM_TO_CCU_OVER_USB_SN_REBOOT:
            case AlertsConstantsKt.DEVICE_RESTART:
            case AlertsConstantsKt.CM_RESET:
                return "No internal logic to trigger alert.";
            case AlertsConstantsKt.CM_DEAD:
                return "Device system logic triggers alerts after 15 minutes if USBService reporting not " +
                        "connected.";
            case AlertsConstantsKt.DEVICE_REBOOT:
                return "Triggered when Device system logic receives a device reboot message from the control mote, or a smart devices reboot message over the network.";
            case AlertsConstantsKt.FIRMWARE_OTA_UPDATE_STARTED:
                return "Triggered by the OTA Service during update, if the update seems to be going successfully.  (Logic there is murky.)";
            case AlertsConstantsKt.FIRMWARE_OTA_UPDATE_ENDED:
                return "Triggered by the OTA Service during update, during the latter part of an OTA update";
            case AlertsConstantsKt.DEVICE_DEAD:
                return "Triggered by the device system if a device has given no update in the last zoneDeadTime(def=15) minutes.";
            case AlertsConstantsKt.DEVICE_LOW_SIGNAL:
                return "Triggered by the device system when either a SmartNode or SmartStat has many messages indicating low signal.";
            default:
                return "Undefined.";
        }
    }

    /** Build a string for debugging using the conditionals' debugging strings */
    public String evaluationString() {
        if (isInternalLogic()) return logicDescription();

        StringBuilder sb = new StringBuilder();
        for (int i=0; i < conditionals.size(); i++) {
            Conditional c = conditionals.get(i);
            sb.append("Condition ").append(i+1);
            if (i%2 == 1) {
                sb.append(": ");
                if (c.operator != null) sb.append(c.operator);
                else if (c.keyValueConditionAllEmpty()) sb.append("?? ");
                else sb.append(c.toString());
            }
            else if (c.keyValueConditionAllEmpty()) sb.append(": Empty");
            else {
                sb.append(" --- ").append("\n");
                sb.append(c.sb.toString());
            }
            sb.append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("AlertDefinition, Title: "+alert.mTitle+" Message "+alert.mMessage);
        for (Conditional c : conditionals) {
            b.append("{"+c.toString()+"} ");
        }
        b.append(", AlertScope=").append(alertScope);
        return b.toString();
    }

    /** Whether this alert definition is currently muted for given ccuId and equipId, if any. */
    public boolean isMuted(@Nullable String ccuId, @Nullable String equipId) {
        if (alertScope != null) {
            return alertScope.isMuted(ccuId, equipId);
        }
        return false;
    }

    /** Return string description of when current deep muting ends for ccuId and equipId */
    public String deepMuteEndTimeString(@Nullable String ccuId, @Nullable String equipId) {
        if (alertScope == null) return "none";
        else return alertScope.deepMuteEndTimeString(ccuId, equipId);
    }
}
