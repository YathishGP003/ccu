package a75.io.algos;

import org.junit.Assert;
import org.junit.Test;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class PiControllerTest
{
    
    @Test
    public void hmpPIControllerTest() throws Exception
    {
        int hmpMinValve = 40;
        int hmpMaxValve = 80;
       
        HmpPIController hmpPi = new HmpPIController(hmpMinValve,hmpMaxValve);
        hmpPi.setIntegralMaxTimeout(15);
        hmpPi.setMaxAllowedError(5.0);
        hmpPi.setProportionalGain(0.5);
        hmpPi.setIntegralGain(0.5);
        
        
        hmpPi.updateControlVariable(110.0,90.0);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(71.33, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
        
        hmpPi.updateControlVariable(110.0,90.0);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(72.67, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,90.0);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(74.00, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,90.0);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(75.33, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,90.0);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(76.67, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,90.0);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(78.00, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,90.0);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(79.33, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,90.0);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(80.00, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(58.67, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(57.33, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(56.00, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(54.67, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(53.33, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(52.00, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(50.67, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(49.33, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(48.00, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(46.67, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(45.33, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(44.00, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(42.67, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(41.33, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
    
        hmpPi.updateControlVariable(110.0,130);
        System.out.println(Math.round(hmpPi.getValveControlSignal()*100.0)/100.0);
        Assert.assertEquals(40.00, Math.round(hmpPi.getValveControlSignal()*100.0)/100.0,0.0);
        
        
    }
}