package a75f.io.renatus;

import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.renatus.framework.BaseSimulationTest;
import a75f.io.renatus.framework.SamplingProfile;
import a75f.io.renatus.framework.SimulationParams;
import a75f.io.renatus.framework.SimulationResult;
import a75f.io.renatus.framework.SimulationRunner;
import a75f.io.renatus.framework.SimulationTestInfo;
import a75f.io.renatus.framework.TestResult;

import static a75f.io.renatus.framework.GraphColumns.Analog1_Out;
import static a75f.io.renatus.framework.GraphColumns.Analog2_Out;
import static a75f.io.renatus.framework.GraphColumns.Relay1_Out;
import static a75f.io.renatus.framework.GraphColumns.Relay2_Out;

/**
 * Created by ryant on 10/25/2017.
 */

public class SSEScheduleInjectTest extends BaseSimulationTest
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
        return "Tests a pre-cofngiured schedule by changing the CCU and biskit(PC) time." +
               "Relay1 and Relay2 outputs of smartnode 7007 configured as Cooling and Fan respectively." +
               "Room temperature is kept above set temperature beyond the deadband value to trigger cooling at the start of schedule.";
    }
    
    @Override
    public String getCCUStateFileName() {
        return "ssescheduleinject.json";
    }
    
    @Override
    public String getSimulationFileName() {
        return "ssescheduleinject.csv";
    }
    
    @Override
    public void analyzeTestResults(SimulationTestInfo testLog) {
        if (mRunner.getLoopCounter() == 0) {
            return; // Test run not started , nothing to analyze
        }
        SimulationResult result = testLog.simulationResult;
        result.analysis = "Not analysed";
    }
    
    @Override
    public long testDuration() {
        return mRunner.duration();
    }
    
    @Override
    public void reportTestResults(SimulationTestInfo testLog, TestResult result) {
        
    }
    
    @Override
    public HashMap<String,ArrayList<Float>> inputGraphData() {
        ArrayList<Float> rt = new ArrayList<>();
        HashMap<String,ArrayList<Float>> graphData = new HashMap<>();
        List<String[]> ip = mRunner.getSimulationInput();
        for (int simIndex = 1; simIndex < ip.size(); simIndex ++)
        {
            String[] simData = ip.get(simIndex);
            SimulationParams params = new SimulationParams().build(simData);
            rt.add(params.room_temperature);
        }
        graphData.put("room_temperature",rt);
        
        return graphData;
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
