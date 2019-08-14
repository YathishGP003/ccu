package a75f.io.alerts;

import com.udojava.evalex.Expression;

import a75f.io.api.haystack.CCUHsApi;
/**
 * Created by samjithsadasivan on 4/27/18.
 */

/**
 * A conditional defines evaluable key-value pair and a condition.
 * Its evaluate() method returns true if the conditional is satisfied for current KVP.
 *
 */
public class Conditional
{
    //LHS
    String key;
    //RHS
    String value;
    
    String condition;
    
    boolean evaluate() {
        if (key.isEmpty() || value.isEmpty() || condition.isEmpty()) {
            throw new IllegalArgumentException("Invalid Conditional");
        }
        double hisVal = CCUHsApi.getInstance().readHisValByQuery(key);
        Expression expression = new Expression(hisVal+ " "+condition+" " + value);
        return expression.eval().intValue() > 0;
    }
    
}
