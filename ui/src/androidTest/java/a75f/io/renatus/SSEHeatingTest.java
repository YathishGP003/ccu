package a75f.io.renatus;

import org.junit.After;
import org.junit.Before;

/**
 * Created by samjithsadasivan on 10/4/17.
 */

/**
 * Test the heating is turned on when current temp drops below set temp
 */
public class SSEHeatingTest extends BaseSimulationTest
{
    SimulationRunner mRunner = null;
    
    @Before
    public void setUp() {
        mRunner =  new SimulationRunner(this);
    }
    
    @After
    public void tearDown() {
    }
    
    @Override
    public String getTestDescription() {
        return SSEHeatingTest.class.getSimpleName();
    }
    
    @Override
    public String getCCUStateFileName() {
        return "sseheatccustate.json";
    }
    
    @Override
    public String getSimulationFileName() {
        return "sseheatingtest.csv";
    }
    
    @Override
    public TestResult analyzeTestResults(SimulationTestInfo testLog) {
        return TestResult.PASS;
    }
    
    @Override
    public long testDuration() {
        return mRunner.duration();
    }
    
    @Override
    public void reportTestResults(SimulationTestInfo testLog, TestResult result) {
        
    }
    
    @Override
    public String[] graphColumns() {
        return null;
    }
    
    @Override
    public void runTest() {
        
        System.out.println("runTest.........");
        
        mRunner.runSimulation();
    }
}
