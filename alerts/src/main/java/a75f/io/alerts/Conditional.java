package a75f.io.alerts;

import android.content.SharedPreferences;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.udojava.evalex.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;
import a75f.io.logger.CcuLog;
/**
 * Created by samjithsadasivan on 4/27/18.
 */

/**
 * A conditional defines evaluable key-value pair and a condition.
 * Its evaluate() method returns true if the conditional is satisfied for current KVP.
 *
 */
@JsonIgnoreProperties(value = { "pointValList", "pointList", "resVal","status", "val", "lastValue", "presentValue", "equipList", "error", "sb" })
public class Conditional
{
    int order;

    // For combining other conditions.  E.g. && and ||
    String operator;

    //LHS  the query to find the point(s) with the value to analyze
    String key;

    //RHS  the value to compare against, usually a limit, e.g. maxTemp.  Can also be expressed as query for a point.
    String value;

    //Middle of condition expression, e.g. >, >, and ==
    String condition;

    // see comment below
    String grpOperation;

    // Value for each point in the pointList OR, in non-zone grp or no grp, all the points retrieved with key (LHS) and their values.
    ArrayList<PointVal> pointValList;
    ArrayList<PointVal>  lastValue = new ArrayList<>();
    ArrayList<PointVal>  presentValue = new ArrayList<>();

    // points, one per equip, that evaluate to true.  his value of point is LHS of evaluation.
    ArrayList<String> pointList;
    ArrayList<String> equipList;

    // value for LHS
    double resVal;

    // condition is true or false
    boolean status;

    // value for RHS
    String val;

    // Error message, if any
    String error = null;
    // For debugging
    StringBuilder sb = null;

    // "isOperator" was causing gson to deserialize server response `operator` as boolean.
    boolean isThisOperator() {
        return order%2 == 1;
    }

    // "isCondition" was causing gson to deserialize server response `condition` as boolean.
    boolean isThisCondition() {
        return order%2 == 0;
    }

    public boolean keyValueConditionAllEmpty() {
        return key.isEmpty() && value.isEmpty() && condition.isEmpty();
    }

    /*
     * Evaluation produces 3 types results for a conditional
     *  - when grpOperation is empty (run the query system wide and fetch the matching point) , update the status boolean
     *  - when grpOperation is equip/delta , create pointList having all the points satisfying the conditional
     *  - when grpOperation is max/min/bottom etc , we are comparing multiple points, resulting in single status boolean.
     *  - when grpOperation is oao, similar logic to equip, but don't create pointList and use result from last equip evaluated (perhaps its the only matching equip?)
     *  - when grpOperation is security, special logic evaluating password attempts.
     */

    // NOTE:  algorithm part of the code is formatted normally on the left.
    // NOTE:  debugging code is indented far to the right and can be ingored when reading the algorithm.
    void evaluate(SharedPreferences spDefaultPrefs) {
                                // these values are for debugging, not part of the algorithm
                                error = null;
                                sb = new StringBuilder();

        if (grpOperation!=null && !grpOperation.isEmpty() && grpOperation.contains("Clear Password")){
            clearPassword(value, spDefaultPrefs);
            return;
        }
        if (key.isEmpty() || value.isEmpty() || condition.isEmpty()) {
                                    sb.append("key: ").append(key);
                                    sb.append(",value: ").append(value);
                                    sb.append(",condition: ").append(condition);
                                    sb.append("  Ending evaluation.");
            return;
        }
                                if (grpOperation != null && !grpOperation.equals(""))
                                    sb.append("grpOperation: ").append(grpOperation).append("\n");


                                if (! isNumeric(value)) {
                                    sb.append("value: ").append(value).append(" -- reading his val \n");
                                }
        val = isNumeric(value) ? value : String.valueOf(CCUHsApi.getInstance().readHisValByQuery(value));

                                sb.append("val (RHS): ").append(val);
                                if (isNumeric(value)) sb.append("  (numeric)");
                                sb.append("\nkey (LHS): ").append(key);
        CcuLog.d("CCU_ALERTS"," Evaluate Conditional : "+key+ " "+condition+" "+val + (isNumeric(value) ? "" : "("+value+")" ));

        if (grpOperation == null || grpOperation.equals("")) {
                                    sb.append("  -- find point from haystack");
            HashMap point = CCUHsApi.getInstance().read(key);
            if (point.size() == 0) {
                                     error = "no point for " + key;
                                     sb.append("\nNo point for key. Ending evaluation.");
                status = false;
                return;
            }
                                    sb.append("\nFound point").append(point.get("id")).append(" -- reading his val");

            resVal = CCUHsApi.getInstance().readHisValById(point.get("id").toString());
                                    sb.append("\nresVal (LHS): ").append(resVal);
            Expression expression = new Expression(resVal + " " + condition + " " + val);
            status = expression.eval().intValue() > 0;
                                    sb.append("\n").append(expression).append(": ").append(status);
        } else if (grpOperation.contains("equip")) {
            pointValList = new ArrayList<>();
            pointList = new ArrayList<>();
            equipList = new ArrayList<>();
            ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("zone and equip");
                                    sb.append("\nEvaluating for ").append(equips.size()).append(" equips");
            for (Map q : equips) {
                                        sb.append("\nEquip: ").append(q.get("dis"));
                HashMap point = CCUHsApi.getInstance().read(key+" and equipRef == \""+q.get("id")+"\"");
                if (point.size() == 0) {
                                            error = "no point for " + key+" and equipRef == \""+q.get("id")+"\"";
                                            sb.append("\n   no point for " + key+" and equipRef == \""+q.get("id")+"\"");
                    continue;
                }
                if (value.contains("zone")) {
                    val = String.valueOf(CCUHsApi.getInstance().readHisValByQuery(value+" and equipRef == \""+q.get("id")+"\""));
                                            sb.append("\n   val (RHS): ").append(val).append("   --re-evaluated for this zone.");
                }
                                            sb.append("\n   Found point assoc w/ this equip").append(point.get("id").toString().substring(0,6)).append(" -- reading his val");
                resVal = CCUHsApi.getInstance().readHisValById(point.get("id").toString());
                                            sb.append("\n   resVal (LHS): ").append(resVal);
                if ((value.contains("co2")&& value.contains("target")) || (value.contains("voc")&& value.contains("target"))){
                    val = String.valueOf(Double.parseDouble(val) + (Double.parseDouble(val)/10.0));
                                                sb.append("\n   val (RHS): ").append(val).append("   --adjusted as co2/voc target.");
                }
                Expression expression = new Expression(resVal+ " "+condition+" " + val);
                                            sb.append("\n").append(expression).append(": ");
                if (expression.eval().intValue() > 0) {
                                                sb.append("TRUE");
                    pointList.add(point.get("id").toString());
                    equipList.add(q.get("id").toString());
                    if ((value.contains("co2")&& value.contains("target")) || (value.contains("voc")&& value.contains("target"))){
                        val = String.valueOf(CCUHsApi.getInstance().readHisValByQuery(value+" and equipRef == \""+q.get("id")+"\""));
                    }
                    pointValList.add(new PointVal(point.get("id").toString(), Double.parseDouble(val)));
                } else {
                                                sb.append("FALSE");
                }
            }
                                        sb.append("\nAfter Equips, have point list size ").append(pointList.size());
                                        for (PointVal pv : pointValList) {
                                                sb.append(" (").append(pv.id.substring(0,6)).append(", ").append(pv.val).append(")");
                                        }
        } else if (grpOperation.contains("oao")){
            ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and oao");
                                        sb.append("\nEvaluating for ").append(equips.size()).append("OAO equips");
            if (equips.size()!=0) {
                for (Map q : equips) {
                                                sb.append("\nEquip: ").append(q.get("dis")).append("  -- expecting just one.");

                    HashMap point = CCUHsApi.getInstance().read(key+" and equipRef == \""+q.get("id")+"\"");
                    if (point.size() == 0) {
                                                    error = "no point for " + key+" and equipRef == \""+q.get("id")+"\"";
                                                    sb.append("no point for " + key+" and equipRef == \""+q.get("id")+"\"");
                        continue;
                    }
                                                sb.append("\nFound point").append(point.get("id")).append(" -- reading his val");
                    val = String.valueOf(CCUHsApi.getInstance().readHisValByQuery(value+" and equipRef == \""+q.get("id")+"\""));
                                                sb.append("\n   val (RHS): ").append(val).append("   --re-evaluated for this zone.");

                    resVal = CCUHsApi.getInstance().readHisValById(point.get("id").toString());
                                                sb.append("\n   resVal (LHS): ").append(resVal);

                    Expression expression = new Expression(resVal + " " + condition + " " + val);
                    status = expression.eval().intValue() > 0;
                                                sb.append("\n").append(expression).append(": ").append(status);
                }
            } else {
                                            sb.append(":  None.");
            }
        } else if (grpOperation.contains("security")){
                                        sb.append("\n grpOperation: security");
            resVal = spDefaultPrefs.getInt("PASSWORD_ATTEMPT",0);
                                        sb.append("\n   resVal (LHS): ").append(resVal).append(" -- password attempts recorded in prefs");

            CcuLog.d("CCU_ALERTS ", " Evaluate Conditional: "+toString());
            val = isNumeric(value) ? value : String.valueOf(CCUHsApi.getInstance().read(value));

                                        if (! isNumeric(value)) {
                                            sb.append("\nvalue: ").append(value);
                                            sb.append(" -- reading his val ");
                                        }
                                        sb.append("\nval (RHS): ").append(val);
                                        if (isNumeric(value)) sb.append(" (numeric)");

            Expression expression = new Expression(resVal+ " "+condition+" " + value);
            status = expression.eval().intValue() > 0;
                                        sb.append("\n").append(expression).append(": ").append(status);
        }
        else {
            pointList = null;
            equipList = null;
            pointValList = new ArrayList<>();
            ArrayList<HashMap> points = CCUHsApi.getInstance().readAll(key);
            for (HashMap m : points)
            {
                pointValList.add(new PointVal(m.get("id").toString(),CCUHsApi.getInstance().readHisValById(m.get("id").toString())));
            }

                                        sb.append("\n Found ").append(points.size()).append(" points").append(" for ").append(key);
            for (PointVal pv : pointValList) {
                                            sb.append(" ").append(pv.id.substring(0,6)).append(", with his val: ").append(pv.val);
            }

            if (grpOperation.contains("top"))
            {
                Collections.sort(pointValList, new PointValDesComparator());
                int percent = Integer.parseInt(grpOperation.replaceAll("[^0-9]", ""));
                                            sb.append("\nparsed percent threshold is ").append(percent);
                int percentCount = pointValList.size() * percent / 100;
                                            sb.append("\npercent count for this sample size is ").append(percentCount);
                for (int i = 0; i < percentCount; i++)
                {
                    Expression expression = new Expression(pointValList.get(i).val + " " + condition + " " + val);
                    status = expression.eval().intValue() > 0;
                    if (!status)
                    {
                                                    sb.append("\nStatus is false for ").append(expression).append(" so breaking check");
                        break;
                    }
                }
                                            sb.append("\nMade it through all TRUE");
            }
            else if (grpOperation.contains("bottom"))
            {
                Collections.sort(pointValList, new PointValAscComparator());
                int percent = Integer.parseInt(grpOperation.replaceAll("[^0-9]", ""));
                                          sb.append("\nparsed percent threshold is ").append(percent);
                int percentCount = pointValList.size() * percent / 100;
                                            sb.append("\npercent count for this sample size is ").append(percentCount);
                for (int i = 0; i < percentCount; i++)
                {
                    Expression expression = new Expression(pointValList.get(i).val + " " + condition + " " + val);
                    status = expression.eval().intValue() > 0;
                    if (!status)
                    {
                                                    sb.append("\nStatus is false for ").append(expression).append(" so breaking check");
                        break;
                    }
                }
                                            sb.append("\nMade it through all TRUE");
            }
            else if (grpOperation.contains("average"))
            {
                double valSum = 0;
                for (PointVal v : pointValList)
                {
                    valSum += v.val;
                }
                resVal = (valSum / pointValList.size());
                                            sb.append("\nAverage of ").append(pointValList.size()).append(" points calculated to ").append(resVal);
                Expression expression = new Expression(resVal + " " + condition + " " + val);
                status = expression.eval().intValue() > 0;
                                            sb.append("\n").append(expression).append(": ").append(status);
            }
            else if (grpOperation.contains("min"))
            {
                Collections.sort(pointValList, new PointValAscComparator());
                resVal = pointValList.get(0).val;
                                            sb.append("\nMin of ").append(pointValList.size()).append(" points calculated to ").append(resVal);
                Expression expression = new Expression( resVal+ " " + condition + " " + val);
                status = expression.eval().intValue() > 0;
                                            sb.append("\n").append(expression).append(": ").append(status);
            }
            else if (grpOperation.contains("max"))
            {
                Collections.sort(pointValList, new PointValDesComparator());
                resVal = pointValList.get(0).val;
                                            sb.append("\nMax of ").append(pointValList.size()).append(" points calculated to ").append(resVal);
                Expression expression = new Expression(resVal + " " + condition + " " + val);
                status = expression.eval().intValue() > 0;
                                            sb.append("\n").append(expression).append(": ").append(status);
            }
            else if (grpOperation.contains("delta"))
            {
                pointList = new ArrayList<>();
                pointValList = new ArrayList<>();
                equipList = new ArrayList<>();
                lastValue.clear();
                presentValue.clear();
                ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("zone and equip");
                                    sb.append("\nEvaluating for ").append(equips.size()).append(" equips for delta");
                for (Map q : equips) {
                                        sb.append("\nEquip: ").append(q.get("dis"));
                    HashMap point = CCUHsApi.getInstance().read(key+" and equipRef == \""+q.get("id")+"\"");
                    if (point.size() == 0) {
                                                    error = "no point for " + key+" and equipRef == \""+q.get("id")+"\"";
                                                    sb.append("\n   no point for " + key+" and equipRef == \""+q.get("id")+"\"");
                        continue;
                    }
                                                sb.append("\n   Found point assoc w/ this equip").append(point.get("id").toString().substring(0,6)).append(" -- reading last two his vals from today");

                    //List<HisItem> hisItems = CCUHsApi.getInstance().hisRead(point.get("id").toString(),HDateTimeRange.make(HDateTime.make(System.currentTimeMillis()-1800000,HTimeZone.make(TimeZone.getDefault().getDisplayName(false,TimeZone.SHORT))), HDateTime.make(System.currentTimeMillis(),HTimeZone.make(TimeZone.getDefault().getDisplayName(false,TimeZone.SHORT)))));
                    List<HisItem> hisItems = CCUHsApi.getInstance().hisRead(point.get("id").toString(),"today");
                    ///List<HisItem> hisItems= CCUHsApi.getInstance().hisReadRemote(CCUHsApi.getInstance().getGUID(point.get("id").toString()),HDateTimeRange.make(HDateTime.make(System.currentTimeMillis() - 3600000, HTimeZone.make(point.get("tz").toString())), HDateTime.make(System.currentTimeMillis(),HTimeZone.make(point.get("tz").toString()))));

                    if (hisItems.size() < 2) {
                                                    error = " Not enough his vals to evaluate conditional ";
                                                    sb.append("\n Not enough his vals " + hisItems.size() + " to evaluate conditional.  Return");
                        return;
                    }
                    
                    HisItem reading1 = hisItems.get(hisItems.size() - 2);
                    HisItem reading2 = hisItems.get(hisItems.size() - 1);
    
                    if (value.contains("zone")) {
                        val = String.valueOf(CCUHsApi.getInstance().readHisValByQuery(value+" and equipRef == \""+q.get("id")+"\""));
                                                    sb.append("\nval (RHS): ").append(val).append("  -- recalculated for this zone");
                    }

                    // check for last updated current temp value
                    if (System.currentTimeMillis() - reading2.getDate().getTime() >= 65000){
                        reading1 = reading2 ;
                                                    sb.append("\nOlder his reading is more than 65 sec old, so setting readings to the same");
                    }

                    resVal = reading1.getVal() - reading2.getVal();
                                               sb.append("\n   resVal (LHS): ").append(resVal).append("  -- difference between readinngs.");

                    Expression expression = new Expression(resVal+ " "+condition+" " + val);
                    CcuLog.d("CCU_ALERTS", " expression "+expression.toString());
                                                sb.append("\n").append(expression).append(": ");

                    if (expression.eval().intValue() > 0) {
                                                    sb.append("TRUE");
                        CcuLog.d("CCU_ALERTS", " Add to pointList");
                        pointList.add(point.get("id").toString());
                        equipList.add(q.get("id").toString());
                        pointValList.add(new PointVal(point.get("id").toString(), Double.parseDouble(val)));
                        lastValue.add(new PointVal(point.get("id").toString(), reading1.getVal()));
                        presentValue.add(new PointVal(point.get("id").toString(), reading2.getVal()));
                    } else {
                                                    sb.append("FALSE");
                    }
                }
                                            sb.append("\nAfter Equips, have point list size ").append(pointList.size());
                                            for (PointVal pv : pointValList) {
                                                sb.append("  ").append(pv.id.substring(0,6)).append("  ").append(pv.val);
                                            }
            }
        }
        
        CcuLog.d("CCU_ALERTS ", " Evaluated Conditional: "+toString()+" ,"+ (grpOperation != null ?(grpOperation.equals("equip") ? pointList.size() : status):"")+" resVal "+resVal);
    }

    private void clearPassword(String value, SharedPreferences pref) {
        SharedPreferences.Editor editor = pref.edit();
        if (value.contains("Zone")){
            editor.putString("zone_settings_password","");
            editor.putBoolean("set_zone_password",false);
        } else if (value.contains("System")){
            editor.putString("system_settings_password","");
            editor.putBoolean("set_system_password",false);
        } else if (value.contains("Building")){
            editor.putString("building_settings_password","");
            editor.putBoolean("set_building_password",false);
        } else if (value.contains("Setup")){
            editor.putString("use_setup_password","");
            editor.putBoolean("set_setup_password",false);
        } else if (value.contains("Reset All")){
		
            editor.putString("zone_settings_password","");
            editor.putBoolean("set_zone_password",false);
            editor.putString("system_settings_password","");
            editor.putBoolean("set_system_password",false);
            editor.putString("building_settings_password","");
            editor.putBoolean("set_building_password",false);
            editor.putString("use_setup_password","");
            editor.putBoolean("set_setup_password",false);
		}
        editor.apply();
    }

    class PointValAscComparator implements Comparator<PointVal>
    {
        public int compare(PointVal a, PointVal b)
        {
            return (int)(a.val - b.val);
        }
    }
    
    class PointValDesComparator implements Comparator<PointVal>
    {
        public int compare(PointVal a, PointVal b)
        {
            return (int)(b.val - a.val);
        }
    }

    public boolean isValueNumeric() {
        return value != null && isNumeric(value);
    }

    public static boolean isNumeric(String strNum) {
        return strNum.matches("-?\\d+(\\.\\d+)?");
    }
    
    @Override
    public String toString() {
        return operator != null ? order+":"+operator : order+": "+key+" "+condition+" "+value+" ("+grpOperation+")";
    }

}
