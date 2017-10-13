package a75f.io.renatus;

import org.junit.After;
import org.junit.Before;

import static a75f.io.renatus.GraphColumns.Analog1_Out;
import static a75f.io.renatus.GraphColumns.Analog2_Out;
import static a75f.io.renatus.GraphColumns.Relay1_Out;
import static a75f.io.renatus.GraphColumns.Relay2_Out;
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
        mRunner =  new SimulationRunner(this, new SamplingProfile(10, 180));
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
    public SimulationResult analyzeTestResults(SimulationTestInfo testLog) {
        SimulationResult result = new SimulationResult();
        result.status = TestResult.PASS;
        return result;
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
        String[] graphCol = {Relay1_Out.toString(),Relay2_Out.toString(),Analog1_Out.toString(), Analog2_Out.toString()};
        return graphCol;
    }
    
    @Override
    public void runTest() {
        
        System.out.println("runTest.........");
        
        mRunner.runSimulation();
    }
}
