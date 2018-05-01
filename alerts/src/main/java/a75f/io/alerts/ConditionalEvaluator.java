package a75f.io.alerts;

import com.udojava.evalex.Expression;

/**
 * Created by samjithsadasivan on 4/25/18.
 */

public class ConditionalEvaluator
{
    /**
     *  Performs a relational operation on key and value
     * @param key
     * @param operation
     * @param val
     * @return boolean result of evaluated expression
     */
    public static boolean evaluate(String key, String operation, String val) {
    
        Expression expression = new Expression(key + operation + val);
        return expression.eval().intValue() > 0;
    }
    
    /**
     *  Evaluates an expression string and returns true/false
     * @param expString
     * @return
     */
    public static boolean evaluate(String expString) {
        Expression expression = new Expression(expString);
        return expression.eval().intValue() > 0;
    }
    
    /**
     *  Evaluates an expression string and returns its int value
     * @param expString
     * @return
     */
    public static int evaluateInt(String expString) {
        Expression expression = new Expression(expString);
        return expression.eval().intValue();
    }
    
    /**
     *  Evaluates an expression string and returns its double values
     * @param expString
     * @return
     */
    public static double evaluateDouble(String expString) {
        Expression expression = new Expression(expString);
        return expression.eval().doubleValue();
    }
}
