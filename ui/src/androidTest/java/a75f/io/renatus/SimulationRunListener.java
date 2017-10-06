package a75f.io.renatus;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Created by samjithsadasivan on 10/4/17.
 */

public class SimulationRunListener extends RunListener
{
    SimulationContext tContext = SimulationContext.getInstance();
    public void testRunStarted(Description description) throws Exception {
        //tContext.testCount++;
    }
    
    public void testRunFinished(Result result) throws Exception {
        tContext.runCount++;
        
        if (tContext.runCount == tContext.testCount) {
            tContext.testSuite.saveReport();
        }
        System.out.println("Number of tests executed: " + result.getRunCount());
    }
    
    public void testStarted(Description description) throws Exception {
        System.out.println("Starting: " + description.getMethodName());
    }
    
    public void testFinished(Description description) throws Exception {
        System.out.println("Finished: " + description.getMethodName());
    }
    
    public void testFailure(Failure failure) throws Exception {
        System.out.println("Failed: " + failure.getDescription().getMethodName());
    }
    
    public void testAssumptionFailure(Failure failure) {
        System.out.println("Failed: " + failure.getDescription().getMethodName());
    }
    
    public void testIgnored(Description description) throws Exception {
        System.out.println("Ignored: " + description.getMethodName());
    }
}
