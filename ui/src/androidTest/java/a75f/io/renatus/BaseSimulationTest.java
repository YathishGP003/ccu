package a75f.io.renatus;

/**
 * Created by samjithsadasivan on 9/21/17.
 */

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Base class for functional test cases , runs with SimulationRunner and enforces each test to have config and test input files
 */
@RunWith(SimulationTestRunner.class)
public abstract class BaseSimulationTest
{
    //Description of test
    public abstract String getTestDescription();
    
    //CCU JsonState file name
    public abstract String getCCUStateFileName();
    
    //Simulation File CSV of time injected sensor events
    public abstract String getSimulationFileName();
    
    //JSON from http:localhost:5000/log/smartnode?address=2000 returns test result, open ended. Pass / Fail, reason for failing.
    public abstract TestResult analyzeTestResults(SimulationTestInfo testLog);
    
    //How long this test should run
    public abstract long testDuration();
    
    //report test results
    public abstract void reportTestResults(SimulationTestInfo testLog, TestResult result);
    
    //Columns to graph
    public abstract String[] graphColumns();
    
    @Test
    public abstract void runTest();
}
