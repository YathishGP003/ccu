package a75f.io.alerts;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
jk            boolean doProcess = inspectAlertDef(def, occurrences);
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
     * Each conditional will evaluate to exactly one evaluation status (one-conditional-to-ONE-evaluation-status relationship).
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
     * Each conditional can evaluate to one or many evaluation statuses, one for each equip (one-conditional-to-MANY-evaluation-status relationship).
     * This can generate one or many alert def occurrences, one for each equip.
     *
     * An alert def with an "equip" or "delta" grpOperation evaluates each conditional against a one or many equips.
     * An alert def can have one or many conditionals.
     * A particular equip may or may not have been evaluated by each conditional.
     * In the case an equip was not evaluated against every conditional, the operator "leading into" the skipped conditional(s) is ignored.
     *
     * For example, an alert def has 3 conditionals.
     *  The 1st and 2nd conditionals are AND'd and the 2nd and 3rd are OR'd: (1st result) AND (2nd result) OR (3rd result).
     *  Equip E1 WAS NOT evaluated against the 2nd conditional key, but WAS evaluated against the 1st and 3rd.
     *  The resulting evaluation will be: (1st result) OR (3rd result).
     */
    private List<AlertDefOccurrence> processForEquips(AlertDefinition def) {
        Map<String, Boolean> equipToResult = new HashMap<>();
        Map<String, String> equipToPoint = new HashMap<>();

        for (int i = 0; i < def.conditionals.size(); i += 2) { // A multi-conditional alert def will have the conditions separated by a "comparision" conditional. Skip these, hence, "i += 2"
            Conditional conditional = def.conditionals.get(i);

            // For each equip evaluated by the conditional, set or recalculate its overall result
            for (Map.Entry<String, Boolean> e : conditional.equipToStatus.entrySet()) {
                String equipRef = e.getKey();
                Boolean status = e.getValue();
                if (!equipToResult.containsKey(equipRef)) {
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

        // NOTE: A "delta" alert def requires a minimum amount of point data in order to evaluate the conditionals.
        //  If no conditionals are executed as a result of this, 'equipToResults' will be empty and an occurrence will NOT be created.
        //  This is misleading because it appears the alert def was not processed while actually it was.
        //  Forgoing a solution at this time because a proper solution requires transparency into these "lack of data" results at the *equip* level.
        //  However, the Conditional class does not currently provide such transparency and a non-trivial refactor is required.
        //  Because this alert-def-was-evaluated-but-an-alert-was-not-raised bug is only visible as a Dev Tool feature on the CCU
        //  and alerts should be holistically addressed, deciding to not create a ticket and wait for someone to notice.
        equipToResult.entrySet().forEach(e -> {
            String equipRef = e.getKey();
            String pointRef = equipToPoint.get(equipRef);
            boolean isEquipMuted = def.isMuted(ccuId, equipRef);
            boolean result = isEquipMuted ? false : e.getValue();
            occurrences.add(buildOccurrence(def, result, isEquipMuted, equipRef, pointRef));
        });
        return occurrences;
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
     * 3) or a multi-conditional alert def has more than one grpOperation present. (This is currently not enforced by the Alerts Service)
     * 4) or the alert def's grpOperation is "alert" (This was added as part of the refactor)
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