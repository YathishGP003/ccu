package a75f.io.alerts;

import static a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS;

import android.content.Context;
import android.content.SharedPreferences;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import a75f.io.alerts.log.LogLevel;
import a75f.io.alerts.log.LogOperation;
import a75f.io.alerts.log.SequencerLogsCallback;
import a75f.io.alerts.model.AlertScope;
import a75f.io.alerts.model.AlertScopeEquip;
import a75f.io.api.haystack.Alert;
import a75f.io.logger.CcuLog;
/**
 * The format for alerts defined in json.
 */

/**
 * A sample definition
 * <p>
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
    public String _id;
    
    public ArrayList<Conditional> conditionals;
    public String                 offset;
    public Alert                  alert;
    public boolean                custom;
    public AlertScope             alertScope;
    @SerializedName("alertBuilder")
    public AlertJs alertBuilder;

    @SerializedName("emitter")
    public String emitter;

    public AlertDefinition(){
    
    }

    /** evaluate this alert definition against current values  */
    public void evaluate(SharedPreferences sharedPrefs) {
        CcuLog.d(TAG_CCU_ALERTS, "Evaluate " + this);
        List<String> mutedEquipIds = getMutedEquipIds();
        for (Conditional c : conditionals) {
            if (c.operator == null) {
                // if one alert definition conditional throws an unexpected exception, catch it here so
                // that all of alert defs does not fail.
                try {
                    c.evaluate(sharedPrefs, mutedEquipIds);
                } catch (RuntimeException error) {
                    CcuLog.e(TAG_CCU_ALERTS, "Parsing error in alert def: " + this, error);
                    c.status = false;
                    c.error = "Parse Exception - " + error.getClass().getSimpleName() + ", " + error.getLocalizedMessage();
                }
            }
        }
    }

    public boolean validate() {
        if (conditionals.size() % 2 == 0) {
            logValidation("Incorrect number of conditionals "+conditionals.size());
            return false;
        }
        
        for (int i = 0; i < conditionals.size() ; i+=2) {
            Conditional c = conditionals.get(i);
            if ( (c.isThisOperator() && c.operator != null) || (c.isThisCondition() && c.operator == null)) {
                logValidation("Operator not allowed "+ c);
                return false;
            }
            if (c.isThisCondition() && (c.grpOperation != null) && !c.grpOperation.isEmpty() && !c.grpOperation.equals("equip") && !c.grpOperation.equals("average")
                        && !c.grpOperation.contains("top") && !c.grpOperation.contains("bottom")
                        && !c.grpOperation.contains("min") && !c.grpOperation.contains("max")) {
                logValidation("grpOperator not supported "+c.grpOperation);
                return false;
            }
        }
        if (alert.mSeverity == null) {
            logValidation("missing severity level");
            return false;
        }

        return true;
    }

    private void logValidation(String msg) {
        CcuLog.e(TAG_CCU_ALERTS,"Invalid Alert Definition : "+msg);
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
                else sb.append(c);
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
        b.append("AlertDefinition, Title: ").append(alert.mTitle).append(" Message ").append(alert.mMessage);
        for (Conditional c : conditionals) {
            b.append("{").append(c.toString()).append("} ");
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

    public List<String> getMutedEquipIds() {
        if (alertScope == null) {
            return List.of();
        }
        List<String> mutedEquipIds = new ArrayList<>();
        alertScope.getDevices().forEach(
                device ->  {
                    mutedEquipIds.addAll(
                            device.getEquips().stream()
                                .filter(equip -> isMuted(device.getDeviceId(), equip.getEquipId()))
                                .map(AlertScopeEquip::getEquipId)
                                .collect(Collectors.toList()));
        });
        return mutedEquipIds;
    }
    void evaluateJsJ2v8(
            AlertDefinition def,
            String javascriptSnippet,
            Context mContext,
            AlertJsUtil alertJsUtil,
            SequencerLogsCallback sequenceLogUtil
    ) {
        try (V8 runtime = V8.createV8Runtime(null, mContext.getApplicationInfo().dataDir)) {

            HaystackService haystackService = new HaystackService(sequenceLogUtil);
            V8Object haystackServiceObject = new V8Object(runtime);

            registerAllMethods(haystackService, haystackServiceObject, sequenceLogUtil);
            haystackServiceObject.add("fetchValueById", haystackService.fetchValueById(runtime));
            runtime.add("haystack", haystackServiceObject);

            PersistBlockService persistBlockService = PersistBlockService.getInstance(def._id);
            V8Object persistBlockServiceObject = new V8Object(runtime);
            registerAllMethods(persistBlockService, persistBlockServiceObject, sequenceLogUtil);
            runtime.add("persistBlock", persistBlockServiceObject);

            V8Object alertJsUtilJsObject = new V8Object(runtime);
            registerAllMethods(alertJsUtil, alertJsUtilJsObject, sequenceLogUtil);
            runtime.add("alerts", alertJsUtilJsObject);

            V8Object contextJsObject = new V8Object(runtime);
            CustomContext customContext = new CustomContext(sequenceLogUtil);
            registerAllMethods(customContext, contextJsObject, sequenceLogUtil);
            runtime.add("ctx", contextJsObject);

            // Execute the JavaScript snippet
            runtime.executeVoidScript(javascriptSnippet);

            // Release resources
            haystackService.release();
            haystackServiceObject.close();
            persistBlockServiceObject.close();
            alertJsUtilJsObject.close();
            contextJsObject.close();

            // Log release status
            if (haystackServiceObject.isReleased()) {
                CcuLog.d(TAG_CCU_ALERTS, "haystackServiceObject is released");
            }
            if (persistBlockServiceObject.isReleased()) {
                CcuLog.d(TAG_CCU_ALERTS, "persistBlockServiceObject is released");
            }
            if (alertJsUtilJsObject.isReleased()) {
                CcuLog.d(TAG_CCU_ALERTS, "alertJsUtilJsObject is released");
            }
            if (contextJsObject.isReleased()) {
                CcuLog.d(TAG_CCU_ALERTS, "contextJsObject is released");
            }
            if (runtime.isReleased()) {
                CcuLog.d(TAG_CCU_ALERTS, "runtime is released");
            }
        }
    }

    public void registerAllMethods(Object javaObject, V8Object v8Object, SequencerLogsCallback sequenceLogsUtil){
        Class<?> clazz = javaObject.getClass();

        for (final Method method : clazz.getMethods()) {
            final String methodName = method.getName();
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final int paramCount = parameterTypes.length;

            JavaCallback callback = (receiver, parameters) -> {
                for (int i = 0; i < parameters.length(); i++) {
                    if (parameters.getType(i) == V8Value.UNDEFINED) {
                        sequenceLogsUtil.logError(
                                LogLevel.ERROR,
                                LogOperation.SEQUENCER_LOG,
                                "Skipped call to '$methodName': parameter at index $i is undefined",
                                "error"
                        );
                        CcuLog.d(TAG_CCU_ALERTS, "🚫 Skipped call to '" + methodName + "': parameter at index " + i + " is undefined");
                        return null;
                    }
                }

                List<Object> v8ValuesToRelease = new ArrayList<>();

                try {
                    Object[] args = new Object[paramCount];
                    for (int i = 0; i < paramCount; i++) {
                        Object v8Value = parameters.get(i);
                        v8ValuesToRelease.add(v8Value);
                        args[i] = v8Value;
                    }

                    return method.invoke(javaObject, args);

                } catch (Exception e) {
                    sequenceLogsUtil.logError(
                            LogLevel.ERROR,
                            LogOperation.SEQUENCER_LOG,
                            "Error calling method '$methodName': ${e.message}",
                            "error"
                    );
                    CcuLog.e(TAG_CCU_ALERTS, "❌ Error calling method '" + methodName + "': " + e.getMessage());
                    return null;

                } finally {
                    CcuLog.i(TAG_CCU_ALERTS, "Releasing V8 values for method '" + methodName + "'");
                    for (Object v8 : v8ValuesToRelease) {
                        try {
                            ((V8Value) v8).close();
                        } catch (Exception ignore) {
                            // Ignore release failures
                        }
                    }
                }
            };

            v8Object.registerJavaMethod(callback, methodName);
        }
    }
}