package a75f.io.alerts;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import a75f.io.api.haystack.Alert;
import a75f.io.logger.CcuLog;
import org.apache.commons.lang3.StringUtils;

import a75f.io.alerts.model.AlertDefOccurrence;
import a75f.io.api.haystack.CCUHsApi;

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
    public List<AlertDefOccurrence> evaluateAlertDefinitions(
            List<AlertDefinition> alertDefs,
            List<Alert> activeAlerts) {

        CcuLog.d("CCU_ALERTS", "processAlerts with count " + alertDefs.size());

        List<AlertDefOccurrence> occurrences = new ArrayList<>();

        // for all alert definitions..
        for (AlertDefinition def : alertDefs) {
            // check for enabled
            if (!def.alert.ismEnabled()) {
                occurrences.add(new AlertDefOccurrence(def, false, false, "Alert disabled -- no evaluation", null, null));
                continue;
            }
            if (def.isMuted(ccuId, null)) {
                occurrences.add(new AlertDefOccurrence(def, true, false, "No condition check.  Whole alert def deep muted until " + def.deepMuteEndTimeString(ccuId, null),
                        null, null));
                continue;
            }
            CcuLog.d("CCU_ALERTS", "Evaluate " + def.toString());

            // Evaluates each conditional of the alert condition that is not an operator.
            // Result is conditional's state is populated, especially "status: Bool" and "resVal: Double" but others like pointList.
            def.evaluate(defaultSharedPrefs);

            ArrayList<String> pointList = null;
            ArrayList<String> equipList = null;
            boolean alertStatus = false;
            boolean statusInit = false;
            // for each even numbered conditional (i.e. not an operator)
            for (int i = 0; i < def.conditionals.size(); i += 2) {
                // for first conditional..
                if (i == 0) {
                    // if its a grpOperation equal to "equip" or "delta", collect its pointList.
                    if ((def.conditionals.get(0).grpOperation != null) && (def.conditionals.get(0).grpOperation.equals("equip")
                            || def.conditionals.get(0).grpOperation.equals("delta"))) {
                        pointList = def.conditionals.get(0).pointList;
                        equipList = def.conditionals.get(0).equipList;
                    } else if ((def.conditionals.get(0).grpOperation != null) && (def.conditionals.get(0).grpOperation.equals("alert"))) {
                        // else if grpOperation == alert, then..
                        for (Alert a : activeAlerts) {
                            // if we find a matching active alert,
                            if (a.mTitle.equals(def.alert.mTitle)) {
                                // set conditional status to true, and our alertStatus to true.
                                def.conditionals.get(0).status = true;
                                alertStatus = true;
                            }
                        }
                    } else {
                        // else set our statusInit on & our alertStatus to conditional status
                        statusInit = true;
                        alertStatus = def.conditionals.get(0).status;
                    }
                    continue;
                }

                // subsequent conditionals  (differentiate here between && and ||)
                if (def.conditionals.get(i - 1).operator != null
                        && def.conditionals.get(i - 1).operator.contains("&&")) {
                    // For grpOperation == ("equip" or "delta") collect pointList and, if already present, take intersection of two conditionals
                    if (def.conditionals.get(i).grpOperation != null && (def.conditionals.get(i).grpOperation.equals("equip")
                            || def.conditionals.get(0).grpOperation.equals("delta"))) {
                        if (pointList == null) {
                            if (alertStatus) {
                                pointList = def.conditionals.get(i).pointList;
                                equipList = def.conditionals.get(i).equipList;
                            }
                        } else {
                            if (def.conditionals.get(i).pointList != null) {
                                pointList.retainAll(def.conditionals.get(i).pointList);
                                equipList.retainAll(def.conditionals.get(i).equipList);
                            }
                        }

                    } else {
                        // else update statusInit & alertStatus based on && logic.
                        if (statusInit) {
                            alertStatus = alertStatus && def.conditionals.get(i).status;
                        } else {
                            statusInit = true;
                            alertStatus = def.conditionals.get(i).status;
                        }

                    }
                } else if (def.conditionals.get(i - 1).operator != null
                        && def.conditionals.get(i - 1).operator.contains("||")) {
                    // For grpOperation == ("equip" or "delta") collect pointList and, if already present, take union of two conditionals
                    if ((def.conditionals.get(i).grpOperation != null) && (def.conditionals.get(i).grpOperation.equals("equip")
                            || def.conditionals.get(0).grpOperation.equals("delta"))) {
                        if (def.conditionals.get(i).pointList != null) {
                            pointList.addAll(def.conditionals.get(i).pointList);
                            equipList.addAll(def.conditionals.get(i).equipList);
                        }

                    } else {
                        // else update statusInit & alertStatus based on && logic.
                        alertStatus = alertStatus || def.conditionals.get(i).status;
                    }
                }
            }
            String evalString = def.evaluationString() + "Evaluates to: " + (alertStatus || (pointList != null && !pointList.isEmpty()));
            if (def.alertScope != null) {
                evalString = evalString + "\nAlertScope (muting): " + def.alertScope.toString();
            }

            // process alert points if present
            if (pointList != null) {
                if (pointList.isEmpty()) {
                    occurrences.add(new AlertDefOccurrence(def, false, false, evalString, null, null));
                }
                for (int i=0; i < pointList.size(); i++) {
                    String pointId = pointList.get(i);
                    String equipId = haystack.getGUID(StringUtils.prependIfMissing(equipList.get(i), "@"));
                    evalString = evalString + "\nThis EquipId: " + equipId;
                    if (equipId != null) {
                        equipId = StringUtils.stripStart(equipId, "@");
                    }
                    boolean deepMuted = def.isMuted(ccuId, equipId);
                    if (deepMuted) {
                        evalString = evalString + " MUTED";
                        occurrences.add(new AlertDefOccurrence(def, true, false, evalString,
                                pointId, equipId));
                    } else {
                        occurrences.add(new AlertDefOccurrence(def, false, true, evalString, pointId, equipId));
                    }
                }
                // OR process if alertStatus true.
            } else if (alertStatus) {
                occurrences.add(new AlertDefOccurrence(def, false, true, evalString, null, null));
            }
            else {
                occurrences.add(new AlertDefOccurrence(def, false, false, evalString, null, null));
            }
        }
        return occurrences;
    }
}