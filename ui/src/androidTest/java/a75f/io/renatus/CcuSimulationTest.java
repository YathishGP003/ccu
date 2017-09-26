package a75f.io.renatus;

/**
 * Created by samjithsadasivan on 9/21/17.
 */

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Base class for functional test cases , runs with CcuTestRunner and enforces each test to have config and test input files
 */
@RunWith(AndroidJUnit4.class)
public abstract class CcuSimulationTest
{
    //Description of test
    public abstract String getTestDescription();
    
    //CCU JsonState file name
    public abstract String getCCUStateFileName();
    
    //Simulation File CSV of time injected sensor events
    public abstract String getSimulationFileName();
    
    //JSON from http:localhost:5000/log/smartnode?address=2000 returns test result, open ended. Pass / Fail, reason for failing.
    public abstract TestResult analyzeTestResults(TestLog testLog);
    
    //How long this test should run
    public abstract long testDuration();
    
    //report test results
    public abstract void reportTestResults(TestLog testLog, TestResult result);
    
    //Columns to graph
    public abstract String[] graphColumns();
    
    @Test
    public abstract void runTest();
}
