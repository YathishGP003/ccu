package a75f.io.alerts;


import com.udojava.evalex.Expression;

import junit.framework.Assert;

import org.junit.Test;

import java.math.BigDecimal;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class EvalExLibTest
{
    @Test
    public void relationlOpsTest() throws Exception
    {
        double setTemp = 70.0;
        double roomTemp = 75.0;
        Expression expression = new Expression(roomTemp+ ">" +setTemp);
        BigDecimal result = expression.eval();
        System.out.println(result.intValue());
        Assert.assertTrue(result.intValue() > 0);
        
        int battery = 70;
        boolean charging = true;
    
        expression = new Expression(battery+"<="+75+"&&"+charging+"=="+false);
        result = expression.eval();
        System.out.println(result.intValue());
        Assert.assertTrue(result.intValue() == 0);
        charging = false;
    
        expression = new Expression(battery+"<="+75+"&&"+charging+"=="+false);
        result = expression.eval();
        Assert.assertTrue(result.intValue() > 0);
        System.out.println(result.intValue());
        
        
        
        int timeCount = 10;
        expression = new Expression(roomTemp+">="+setTemp+"&&"+timeCount+"=="+10);
        result = expression.eval();
        System.out.println(result);
        Assert.assertTrue(result.intValue() > 0);
        
        expression = new Expression("100 < 90 || 190 <= 200");
        System.out.println(expression.eval());
    
        expression = new Expression("-6 > -5");
        System.out.println(expression.eval());
        
    }
    
   
}