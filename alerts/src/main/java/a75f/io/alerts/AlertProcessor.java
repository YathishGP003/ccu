package a75f.io.alerts;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import a75f.io.alerts.model.AlertDefOccurrence;
import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
//import org.mozilla.javascript.Context;

/*
  Created by samjithsadasivan on 4/24/18.
 */

/**
 * Core processing module that iterates through all the alert definitions , evaluates the conditional
 * and returns all the positive conditionals
 */
public class AlertProcessor
{
    // Parses a String into a list of AlertDefinitions
    AlertParser parser;

    public static final String TAG_CCU_ALERTS = "CCU_ALERTS";

    public static final String TAG_CCU_ALERT_FORMATTER = "CCU_ALERT_FORMATTER";

    public static final String TAG_CCU_DEV_DEBUG = "CCU_DEV_DEBUG";

    public static final String TAG_CCU_HTTP_REQUEST = "CCU_HTTP_REQUEST";

    public static final String TAG_CCU_HTTP_RESPONSE = "CCU_HTTP_RESPONSE";

    private final SharedPreferences defaultSharedPrefs;

    private Context mContext;

    AlertProcessor(Context c) {
        mContext  = c;
        this.defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(c);

        // great candidate for DI when we have it:
        CcuLog.d(TAG_CCU_ALERTS, "AlertProcessor Init");

        // The parser to
        parser = new AlertParser();
    }

    /**
     * Called from AlertRepo upon AlertProcessJob.doJob.
     */
    public List<AlertDefOccurrence> evaluateAlertDefinitions(List<AlertDefinition> alertDefs) {

        //AlertManager.getInstance().fixAlert("CCU IN SAFE MODE", "CCU is in safe mode", false);

        mapOfPastAlerts.clear();
        List<AlertDefOccurrence> occurrences = new ArrayList<>();

        // Exit without processing alert defs if registration is not yet complete
        if (CCUHsApi.getInstance().getCcuRef() == null) {
            return occurrences;
        }

        for (AlertDefinition def : alertDefs) {
            try{
                boolean doProcess = inspectAlertDef(def, occurrences);
                if (!doProcess) {
                    continue;
                }

                if(isInAutoCommissioningMode() && suppressAlert(def)) {
                    CcuLog.d(TAG_CCU_ALERTS, "In AutoCommissioning mode ");
                    continue;
                }
                Conditional.GrpOperator alertDefType = Conditional.GrpOperator.fromValue(def.conditionals.get(0).grpOperation); // See the note in ::inspectAlertDef regarding unique grpOperations
                if(def.alertBuilder != null){
                    // new alert definition found use rhino processor to generate alert
                    CcuLog.d(TAG_CCU_ALERTS, "new alert definition found evaluating alert-->"+def.alert.mTitle);
                    //String jsForTesting = loadLocalJs(mContext, "test1.js");
                    //String jsForTesting = loadLocalJs(mContext, def.alertBuilder.getSnippet());
                    String jsForTesting = def.alertBuilder.getSnippet();
                    //evaluateJs(def.alertBuilder);
                    alertJsUtil.def = def;
                    def.evaluateJs(def, jsForTesting, mContext, alertJsUtil);
                }else {
                    def.evaluate(defaultSharedPrefs);

                    if (alertDefType.equals(Conditional.GrpOperator.EQUIP) || alertDefType.equals(Conditional.GrpOperator.DELTA)) {
                        List<AlertDefOccurrence> retunredList = processForEquips(def);
                        occurrences.addAll(retunredList);
                    } else {
                        occurrences.add(process(def));
                    }
                }
            }catch (Exception e) {
                CcuLog.e(TAG_CCU_ALERTS, "Error in evaluating alert definition-->" + def.alert.mTitle);
                e.printStackTrace();
            }
        }
        CcuLog.d(TAG_CCU_ALERTS, "evaluateAlertDefinitions - end");

        // this change is there if next time alert is not there then it will be fixed,
        // suppose alerts A, B, C are there in db and now only A, C are triggered then B will be fixed
        AlertManager.getInstance().getActiveAlertsByCreator("blockly").forEach(alert -> {
            String keyFromDb = alert.blockId+":"+alert.equipId;
            if (!mapOfPastAlerts.containsKey(keyFromDb)) {
                //AlertManager.getInstance().deleteAlert(alert);
                AlertManager.getInstance().fixAlert(alert);
            }
        });

        mapOfPastAlerts.forEach((k, v) -> {
            Alert tempAlert = (Alert) v;
            AlertManager.getInstance().generateAlertBlockly(tempAlert.mTitle, tempAlert.mMessage, tempAlert.equipId, "blockly", tempAlert.blockId);
        });
        return occurrences;
    }

    HashMap mapOfPastAlerts = new HashMap<String, Alert>();
    AlertJsUtil alertJsUtil = new AlertJsUtil(new AlertJsCallback() {
        @Override
        public boolean triggerAlert(String blockId, String notificationMsg, String message, String entityId, Object contextHelper, AlertDefinition def) {

            HashMap<Object, Object> map = CCUHsApi.getInstance().readMapById(entityId);
            if(map == null || map.isEmpty()){
                CcuLog.d(TAG_CCU_ALERTS, "---triggerAlert-blockId5005@@ invalid id->"+entityId);
                return false;
            }else{
                CcuLog.d(TAG_CCU_ALERTS, "---triggerAlert-blockId5005@@-"+blockId + " notificationMsg: " + notificationMsg + " message: " + message + " entityId: " + entityId + "-current thread->"+Thread.currentThread().getName());
                Alert alert = AlertBuilder.build(def, message, CCUHsApi.getInstance(),entityId,"");
                alert.blockId = blockId;
                String tempId = entityId.replaceFirst("@","");
                mapOfPastAlerts.put(blockId+":"+tempId, alert);
                return true;
            }
        }
    });

    private String loadLocalJs(Context context, String fileName){
        return readTextFileFromAssets(context, fileName);
    }

    public static String readTextFileFromAssets(Context context, String fileName) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open(fileName);

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }

    public static class ConsolePrint {
        public void print(String message) {
            System.out.println(message);
        }
    }

    private boolean suppressAlert(AlertDefinition def) {
        if (def.conditionals.size() > 0) {
            boolean isAlertMatches = false;
            for (Conditional conditional : def.conditionals) {
                if(conditional.value != null && (conditional.value.equalsIgnoreCase("system and building and limit and min") ||
                        conditional.value.equalsIgnoreCase("system and building and limit and max")) &&
                        (!def.alert.getmSeverity().toString().equalsIgnoreCase("SEVERE"))) {
                    isAlertMatches = true;
                    CcuLog.d(TAG_CCU_ALERTS, def.alert.getmTitle()+ " alert suppressed - conditional.value is -  "+conditional.value);
                }
            }
            return isAlertMatches;
        }
        return false;
    }

    private boolean isInAutoCommissioningMode() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        return hayStack.readPointPriorityValByQuery("point and diag and auto and commissioning") == 1.0;
    }

    /**
     * NOTE: This assumes the alert def has already been evaluated (AlertDefinition::evaluate)
     * <p>
     * Process the alert def at the alert def level.
     * Each conditional will evaluate to exactly one evaluation status.
     * This will generate only one alert def occurrence.
     */
    private AlertDefOccurrence process(AlertDefinition def) {
        boolean result = false;
        for (int i = 0; i < def.conditionals.size(); i += 2) { // A multi-conditional alert def will have the conditions separated by a "comparison operator" condition. Skip these, hence, "i += 2"
            Conditional conditional = def.conditionals.get(i);
            if (i == 0) {
                result = conditional.status;
            } else {
                Conditional operatorConditional = def.conditionals.get(i - 1);
                Conditional.Operator operator = Conditional.Operator.fromValue(operatorConditional.operator);
                result = calculateResult(result, operator, conditional.status);
            }
        }
        return buildOccurrence(def, result, false, null, null);
    }

    private AlertDefOccurrence processJsDef(AlertDefinition def) {
        return buildOccurrence(def, true, false, null, null);
    }
    
    /**
     * NOTE: This assumes the alert def has already been evaluated (AlertDefinition::evaluate)
     * <p>
     * Process an alert def at the equip level.
     * Each conditional can be evaluated against one or many equips.
     * This will generate an alert def occurrence for each equip.
     * <p>
     * An alert def with an "equip" or "delta" grpOperation is evaluated in this manner.
     */
    private List<AlertDefOccurrence> processForEquips(AlertDefinition def) {
        Map<String, String> equipToPoint = new HashMap<>();
        // Find all the equips spread across the conditionals and initialize the results map
        Map<String, Boolean> equipToResult = def.conditionals.stream()
                .flatMap(c -> c.equipToStatus.keySet().stream())
                .distinct()
                .collect(Collectors.toMap(Function.identity(), v -> false));

        for (int i = 0; i < def.conditionals.size(); i += 2) { // A multi-conditional alert def will have the conditions separated by a "comparison" conditional. Skip these, hence, "i += 2"
            Conditional conditional = def.conditionals.get(i);

            // For each equip, set (or recalculate) it's overall result
            for (Map.Entry<String, Boolean> e : equipToResult.entrySet()) {
                String equipRef = e.getKey();
                boolean status = conditional.equipToStatus.getOrDefault(equipRef, false); // If the equip was not evaluated, then the status is false
                if (i == 0) {
                    equipToResult.put(equipRef, status);
                } else {
                    Conditional operatorConditional = def.conditionals.get(i - 1);
                    Conditional.Operator operator = Conditional.Operator.fromValue(operatorConditional.operator);
                    equipToResult.compute(equipRef, (k, v) -> calculateResult(v, operator, status));
                }
            }

            conditional.equipToPoint.entrySet().forEach(e -> {
                equipToPoint.putIfAbsent(e.getKey(), e.getValue()); // Theoretically, the same equip and point can be evaluated by more than one conditional.
            });
        }

        // Build alert def occurrences
        List<AlertDefOccurrence> occurrences = new ArrayList<>();
        if (equipToResult.isEmpty()) {
            // This will occur when not enough data exists for the points evaluated within a 'delta' grpOperation
            occurrences.add(new AlertDefOccurrence(
                    def,
                    false,
                    false,
                    "The conditional(s) were not evaluated against any equips.",
                    null,
                    null));
        } else  {
            String ccuId = CCUHsApi.getInstance().getCcuRef().toVal();

            equipToResult.entrySet().forEach(e -> {
                String equipRef = e.getKey();
                String pointRef = equipToPoint.get(equipRef);
                boolean isEquipMuted = def.isMuted(ccuId, refToId(equipRef));
                boolean result = isEquipMuted ? false : e.getValue();
                occurrences.add(buildOccurrence(def, result, isEquipMuted, equipRef, pointRef));
            });
        }
        return occurrences;
    }

    private String refToId(String ref) {
        return ref.startsWith("@") ? ref.substring(1) : ref;
    }

    private boolean calculateResult(boolean currentResult, Conditional.Operator operator, Boolean status) {
        return operator.equals(Conditional.Operator.AND) ? currentResult && status : currentResult || status;
    }

    private AlertDefOccurrence buildOccurrence(AlertDefinition def,
                               boolean result,
                               boolean isMuted,
                               String equipRef,
                               String pointRef) {
        StringBuilder sb = new StringBuilder(def.evaluationString() + "Evaluates to: " + result);
        if (def.alertScope != null) {
            sb.append("\nAlertScope (muting): ").append(def.alertScope);
        }
        if (equipRef != null) {
            sb.append("\nThis EquipRef: ").append(equipRef);
        }
        if (isMuted) {
            sb.append(" MUTED");
        }
        return new AlertDefOccurrence(def,
                                   isMuted,
                                   result,
                                   sb.toString(),
                                   pointRef,
                                   equipRef);
    }

    /**
     * An alert def may not need to be processed.
     * If:
     * 1) The alert is not enabled
     * 2) or the alert def is muted
     * 3) or a multi-conditional alert def has more than one grpOperation present. (This was added to the PM specs and is currently not enforced by the Alerts Service)
     * 4) or the alert def's grpOperation is "alert" (This was added as part of the refactor. See comment below.)
     * Then the alert def will not be processed.
     */
    private boolean inspectAlertDef(AlertDefinition def, List<AlertDefOccurrence> occurrences) {
        String ccuId = CCUHsApi.getInstance().getCcuRef().toVal();

        List<String> uniqueGrpOperations = def.conditionals.stream()
                .filter(conditional -> conditional.operator == null || conditional.operator.isEmpty()) // Skip the "comparison" conditionals
                .map(conditional -> conditional.grpOperation)
                .distinct()
                .collect(Collectors.toList());
        if(!def.emitter.equalsIgnoreCase("ALERT_BUILDER_CCU") && !def.emitter.equalsIgnoreCase("CCU")){
            return false;
        }

        boolean isMuted = false;
        String evaluationString = "";
        if (!def.alert.ismEnabled()) {
            evaluationString = "The alert is disabled. Not evaluated.";
        } else if (def.isMuted(ccuId, null)) {
            isMuted = true;
            evaluationString = "The alert def is muted at the alert def level or the CCU level. Not evaluated. The alert def is deep muted until " + def.deepMuteEndTimeString(ccuId, null);
        } else if (uniqueGrpOperations.size() != 1) {
            // If an alert def has more than one condition, then all conditions must have the same grpOperation.
            // Perhaps enforce this when an alert def is created? Or store the grpOperation at the alert def level, not the conditional level?
            evaluationString = "Not evaluated. A multi-conditional alert def has more than one grpOperation. Only one is allowed.";
        } else if (Conditional.GrpOperator.ALERT.equals(Conditional.GrpOperator.fromValue(uniqueGrpOperations.get(0)))) {
                // JJG - I am unsure why the "alert" grp operator was even evaluated by this class.
                //  These alert defs are evaluated elsewhere in the CCU.
                //  Per the PM docs: "Special hard-coded logic on the CCU defines these alerts".
                //  Presumably these are evaluated in the "AlertGenerateHandler" class?
                //  Either way, a new positive occurrence was created by this class ONLY IF there is an active alert of the alert def.
                //  Therefore, the AlertsRepository::processAlertsDef will not consider this positive occurrence as a "new alert",
                //  and hence it will do nothing with the positive occurrence created in this method.
                //  To my knowledge, creating a positive occurrence in this manner does nothing.
                //  Plus, the Conditional::evaluate does not even evaluate this grpOperation.
                //  This is long way of saying: This class will no longer process these alert defs.
                evaluationString = "Not evaluated. Alert Def's with 'grpOperator' = 'alert' is processed elsewhere on the CCU.";
        }

        if (!evaluationString.isEmpty()) {
            evaluationString += "  Alert Def Id = " + def._id;
            // JJG - I'm unsure why an occurrence is created for a negative evaluation.
            //  This was the logic before the refactor. Leaving it here to minimize the regression.
            //  Someday this trail should be investigated. This method could largely be refactored as well.
            occurrences.add(new AlertDefOccurrence(def, isMuted, false, evaluationString, null, null));
            return false;
        } else {
            return  true;
        }
    }
}