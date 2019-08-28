package a75f.io.alerts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.udojava.evalex.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
/**
 * Created by samjithsadasivan on 4/27/18.
 */

/**
 * A conditional defines evaluable key-value pair and a condition.
 * Its evaluate() method returns true if the conditional is satisfied for current KVP.
 *
 */
@JsonIgnoreProperties(value = { "pointValList", "pointList", "resVal","status" })
public class Conditional
{
    int order;
    
    String operator;
    //LHS
    String key;
    //RHS
    String value;
    
    String condition;
    
    String grpOperation;
    
    ArrayList<PointVal> pointValList;
    
    ArrayList<String> pointList;
    
    double resVal;
    
    boolean status;
    
    boolean evaluate(String ref) {
        if (key.isEmpty() || value.isEmpty() || condition.isEmpty()) {
            throw new IllegalArgumentException("Invalid Conditional");
        }
        resVal = CCUHsApi.getInstance().readHisValByQuery(key+" and equipRef == \""+ref+"\"");
        CcuLog.d("CCU_ALERTS ", " Evaluate Conditional: "+toString());
        if (!isNumeric(value)) {
            value = String.valueOf(CCUHsApi.getInstance().read(value));
        }
        
        Expression expression = new Expression(resVal+ " "+condition+" " + value);
        status = expression.eval().intValue() > 0;
        return status;
    }
    
    void evaluate() {
        
        if (key.isEmpty() || value.isEmpty() || condition.isEmpty()) {
            throw new IllegalArgumentException("Invalid Conditional");
        }
        
        if (grpOperation.contains("equip")) {
            pointValList = null;
            pointList = new ArrayList<>();
            ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("zone and equip");
            for (Map q : equips) {
                HashMap point = CCUHsApi.getInstance().read(key+" and equipRef == \""+q.get("id")+"\"");
                resVal = CCUHsApi.getInstance().readHisValById(point.get("id").toString());
                if (!isNumeric(value)) {
                    value = String.valueOf(CCUHsApi.getInstance().read(value));
                }
                Expression expression = new Expression(resVal+ " "+condition+" " + value);
                if (expression.eval().intValue() > 0) {
                    pointList.add(point.get("id").toString());
                }
            }
        } else {
            pointList = null;
            pointValList = new ArrayList<>();
            ArrayList<HashMap> points = CCUHsApi.getInstance().readAll(key);
            for (HashMap m : points)
            {
                pointValList.add(new PointVal(m.get("id").toString(),CCUHsApi.getInstance().readHisValById(m.get("id").toString())));
            }
            
            if (grpOperation.contains("top"))
            {
                Collections.sort(pointValList, new PointValDesComparator());
                int percent = Integer.parseInt(grpOperation.replaceAll("[^0-9]", ""));
                int percentCount = pointValList.size() * percent / 100;
                if (!isNumeric(value))
                {
                    value = String.valueOf(CCUHsApi.getInstance().readHisValByQuery(value));
                }
                for (int i = 0; i < percentCount; i++)
                {
                    Expression expression = new Expression(pointValList.get(i).val + " " + condition + " " + value);
                    status = expression.eval().intValue() > 0;
                    if (!status)
                    {
                        break;
                    }
                }
            }
            else if (grpOperation.contains("bottom"))
            {
                Collections.sort(pointValList, new PointValAscComparator());
                int percent = Integer.parseInt(grpOperation.replaceAll("[^0-9]", ""));
                int percentCount = pointValList.size() * percent / 100;
                if (!isNumeric(value))
                {
                    value = String.valueOf(CCUHsApi.getInstance().readHisValByQuery(value));
                }
                for (int i = 0; i < percentCount; i++)
                {
                    Expression expression = new Expression(pointValList.get(i).val + " " + condition + " " + value);
                    status = expression.eval().intValue() > 0;
                    if (!status)
                    {
                        break;
                    }
                }
            }
            else if (grpOperation.contains("average"))
            {
                if (!isNumeric(value))
                {
                    value = String.valueOf(CCUHsApi.getInstance().readHisValByQuery(value));
                }
                double valSum = 0;
                for (PointVal v : pointValList)
                {
                    valSum += v.val;
                }
                resVal = (valSum / pointValList.size());
                Expression expression = new Expression(resVal + " " + condition + " " + value);
                status = expression.eval().intValue() > 0;
            }
            else if (grpOperation.contains("min"))
            {
                Collections.sort(pointValList, new PointValAscComparator());
                if (!isNumeric(value))
                {
                    value = String.valueOf(CCUHsApi.getInstance().readHisValByQuery(value));
                }
                resVal = pointValList.get(0).val;
                Expression expression = new Expression( resVal+ " " + condition + " " + value);
                status = expression.eval().intValue() > 0;
            }
            else if (grpOperation.contains("max"))
            {
                Collections.sort(pointValList, new PointValDesComparator());
                if (!isNumeric(value))
                {
                    value = String.valueOf(CCUHsApi.getInstance().readHisValByQuery(value));
                }
                resVal = pointValList.get(0).val;
                Expression expression = new Expression(resVal + " " + condition + " " + value);
                status = expression.eval().intValue() > 0;
                
            } else if (grpOperation.equals("") || grpOperation == null) {
                
                resVal = CCUHsApi.getInstance().readHisValByQuery(key);
                if (!isNumeric(value)) {
                    value = String.valueOf(CCUHsApi.getInstance().read(value));
                }
                Expression expression = new Expression(resVal + " " + condition + " " + value);
                status = expression.eval().intValue() > 0;
            }
        }
    
        CcuLog.d("CCU_ALERTS ", " Evaluated Conditional: "+toString()+" ,"+(grpOperation.equals("equip") ? pointList.size() : status)+" resVal "+resVal);
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
    
    public static boolean isNumeric(String strNum) {
        return strNum.matches("-?\\d+(\\.\\d+)?");
    }
    
    @Override
    public String toString() {
        return operator != null ? operator : key+" "+condition+" "+value+" ("+grpOperation+")";
    }
    
}
