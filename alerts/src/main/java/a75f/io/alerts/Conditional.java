package a75f.io.alerts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.udojava.evalex.Expression;

import java.util.ArrayList;
import java.util.HashMap;

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
@JsonIgnoreProperties(value = { "trueList","val","status" })
public class Conditional
{
    int order;
    
    String operator;
    //LHS
    String key;
    //RHS
    String value;
    
    String condition;
    
    ArrayList<String> trueList;
    
    double val;
    
    boolean status;
    
    boolean evaluate(String ref) {
        if (key.isEmpty() || value.isEmpty() || condition.isEmpty()) {
            throw new IllegalArgumentException("Invalid Conditional");
        }
        val = CCUHsApi.getInstance().readHisValByQuery(key+" and equipRef == \""+ref+"\"");
        CcuLog.d("CCU_ALERTS ", " Evaluate Conditional: "+key+" "+condition+" "+value+", val "+val);
        if (!isNumeric(value)) {
            value = String.valueOf(CCUHsApi.getInstance().read(value));
        }
        
        Expression expression = new Expression(val+ " "+condition+" " + value);
        status = expression.eval().intValue() > 0;
        return status;
    }
    
    void evaluate() {
        if (key.isEmpty() || value.isEmpty() || condition.isEmpty()) {
            throw new IllegalArgumentException("Invalid Conditional");
        }
        trueList = new ArrayList<>();
        ArrayList<HashMap> point = CCUHsApi.getInstance().readAll(key);
        for (HashMap m : point)
        {
            double hisVal = CCUHsApi.getInstance().readHisValById(m.get("id").toString());
    
            if (!isNumeric(value)) {
                value = String.valueOf(CCUHsApi.getInstance().read(value));
            }
            CcuLog.d("CCU_ALERTS ", m.get("dis")+" Evaluate Conditional: "+key+" "+condition+" "+value+", val "+hisVal);
            Expression expression = new Expression(hisVal + " " + condition + " " + value);
            if (expression.eval().intValue() > 0) {
                trueList.add(m.get("id").toString());
            }
        }
    }
    
    public static boolean isNumeric(String strNum) {
        return strNum.matches("-?\\d+(\\.\\d+)?");
    }
    
    @Override
    public String toString() {
        return operator != null ? operator : key+" "+condition+" "+value;
    }
    
}
