package a75f.io.renatus;

import org.junit.After;
import org.junit.Before;

import a75f.io.bo.building.definitions.MockTime;

/**
 * Created by samjithsadasivan on 9/22/17.
 */

public class LightControlSimulationTest extends BaseSimulationTest
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
        return LightControlSimulationTest.class.getSimpleName();
    }
    
    @Override
    public String getCCUStateFileName() {
         return "lighttest.json";
    }
    
    @Override
    public String getSimulationFileName() {
        return "lighttest.csv";
    }
    
    @Override
    public TestResult analyzeTestResults(SimulationTestInfo testLog) {
    
        for (SmartNodeParams param : testLog.nodeParams) {
            if (param.lighting_control_enabled == 0) {
                return TestResult.FAIL;
            }
        }
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
        MockTime.getInstance().setMockTime(true, System.currentTimeMillis()+ (8 * 3600000)); // Force the mocktime to out of schedule interval
        mRunner.resetRunner();
        mRunner.runSimulation();
   }
    
    
}
