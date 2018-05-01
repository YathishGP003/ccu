package a75f.io.alerts;

import com.udojava.evalex.Expression;

import java.util.Map;

/**
 * Created by samjithsadasivan on 4/27/18.
 */

public class Conditional
{
    //LHS
    String key;
    //RHS
    String value;
    
    String condition;
    
    boolean evaluate(Map<String,Object> tsData) {
        if (key.isEmpty() || value.isEmpty() || condition.isEmpty()) {
            throw new IllegalArgumentException("Invalid Conditional");
        }
        
        if (tsData.get(key) == null) {
            throw new IllegalStateException("Invalid Key");
        }
        Expression expression = new Expression(String.valueOf(tsData.get(key)) + " "+condition+" " + value);
        return expression.eval().intValue() > 0;
    }
    
}
