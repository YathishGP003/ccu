package a75f.io.alerts;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import a75f.io.alerts.model.AlertDefOccurrence;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 4/24/18.
 */

/**
 * Core processing module that iterates through all the alert definitions , evaluates the conditional
 * and returns all the positive conditionals
 */
public class AlertProcessor
{
    // Parses a String into a list of AlertDefinitions
    AlertParser parser;
    private final String ccuId;
    private final CCUHsApi haystack;      // used only for luid/guid lookup

    private final SharedPreferences defaultSharedPrefs;

    AlertProcessor(Context c, String ccuId, CCUHsApi haystack) {
        this.defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(c);
        this.ccuId = ccuId;
        this.haystack = haystack;
        // great candidate for DI when we have it:
        CcuLog.d("CCU_ALERTS", "AlertProcessor Init");

        // The parser to
        parser = new AlertParser();
    }

    /**
     * Called from AlertRepo upon AlertProcessJob.doJob.
     */
    public List<AlertDefOccurrence> evaluateAlertDefinitions(List<AlertDefinition> alertDefs) {
        CcuLog.d("CCU_ALERTS", "processAlerts with count " + alertDefs.size());
        List<AlertDefOccurrence> occurrences = new ArrayList<>();

        for (AlertDefinition def : alertDefs) {
            boolean doProcess = inspectAlertDef(def, occurrences);
            if (!doProcess) {
                continue;
            }

            def.evaluate(defaultSharedPrefs);
            CcuLog.d("CCU_ALERTS", "Evaluate " + def.toString());
            Conditional.GrpOperator alertDefType = Conditional.GrpOperator.fromValue(def.conditionals.get(0).grpOperation); // See the note in ::inspectAlertDef regarding unique grpOperations

            if (alertDefType.equals(Conditional.GrpOperator.EQUIP) || alertDefType.equals(Conditional.GrpOperator.DELTA)) {
                occurrences.addAll(processForEquips(def));
            } else {
                occurrences.add(process(def));
            }
        }
        return occurrences;
    }

    /**
     * NOTE: This assumes the alert def has already been evaluated (AlertDefinition::evaluate)
     *
     * Process the alert def at the alert def level.
     * Each conditional will evaluate to exactly one evaluation status.
     * This will generate only one alert def occurrence.
     */
    private AlertDefOccurrence process(AlertDefinition def) {
        boolean result = false;
        for (int i = 0; i < def.conditionals.size(); i += 2) { // A multi-conditional alert def will have the conditions separated by a "comparision operator" condition. Skip these, hence, "i += 2"
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

    /**
     * NOTE: This assumes the alert def has already been evaluated (AlertDefinition::evaluate)
     *
     * Process an alert def at the equip level.
     * Each conditional can be evaluated against one or many equips.
     * This will generate an alert def occurrence for each equip.
     *
     * An alert def with an "equip" or "delta" grpOperation is evaluated in this manner.
     */
    private List<AlertDefOccurrence> processForEquips(AlertDefinition def) {
        Map<String, String> equipToPoint = new HashMap<>();

        // Find all the equips spread across the conditionals and initialize the results map
        Map<String, Boolean> equipToResult = def.conditionals.stream()
                .flatMap(c -> c.equipToStatus.keySet().stream())
                .distinct()
                .collect(Collectors.toMap(Function.identity(), v -> false));

        for (int i = 0; i < def.conditionals.size(); i += 2) { // A multi-conditional alert def will have the conditions separated by a "comparision" conditional. Skip these, hence, "i += 2"
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

        List<String> uniqueGrpOperations = def.conditionals.stream()
                .filter(conditional -> conditional.operator == null || conditional.operator.isEmpty()) // Skip the "comparision" conditionals
                .map(conditional -> conditional.grpOperation)
                .distinct()
                .collect(Collectors.toList());

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