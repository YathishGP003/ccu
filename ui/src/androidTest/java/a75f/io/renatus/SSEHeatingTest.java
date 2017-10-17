package a75f.io.renatus;

import org.junit.After;
import org.junit.Before;

import a75f.io.renatus.framework.BaseSimulationTest;
import a75f.io.renatus.framework.SamplingProfile;
import a75f.io.renatus.framework.SimulationResult;
import a75f.io.renatus.framework.SimulationRunner;
import a75f.io.renatus.framework.SimulationTestInfo;
import a75f.io.renatus.framework.SmartNodeParams;
import a75f.io.renatus.framework.TestResult;

import static a75f.io.renatus.framework.GraphColumns.Analog1_Out;
import static a75f.io.renatus.framework.GraphColumns.Analog2_Out;
import static a75f.io.renatus.framework.GraphColumns.Relay1_Out;
import static a75f.io.renatus.framework.GraphColumns.Relay2_Out;
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
        return "The test injects a CcuState with Relay1 and Relay2 outputs of smartnode 7001 configured as Heating and Fan respectively." +
               "Default tuner parameters are userMinTemp:70, buildingMaxTemp:85, sseHeatingDeadBand:1, buildingMinTemp:60, " +
               "lightingIntensityOccupantDetected: 75, userMaxTemp:73, minLightControlOverInMinutes:20,sseCoolingDeadBand:1, zoneSetBack:5"+
               "It sends 10 sets of inputs with varying room-temperature and set-temperature evey 3 minutes." +
               "Test runs for 30 minutes fetching smart node params every 3 minutes.";
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
    public void analyzeTestResults(SimulationTestInfo testLog) {
        if (mRunner.getLoopCounter() == 0) {
            return; // Test run not started , nothing to analyze
        }
        SimulationResult result = testLog.simulationResult;
        if (testLog.resultParamsMap.get(new Integer(7001)) != null)
        {
            SmartNodeParams params = testLog.resultParamsMap.get(new Integer(7001)).get(mRunner.getLoopCounter());
            switch (mRunner.getLoopCounter())
            {
                case 1:
                case 2:
                case 7:
                    if ((params.digital_out_1 == 0) && (params.digital_out_2 == 0))
                    {
                        result.analysis += "<p>Check Point " + mRunner.getLoopCounter() + ": PASS" + "</p>";
                    }
                    else
                    {
                        result.analysis += "<p>Check Point " + mRunner.getLoopCounter() + ": FAIL" + "</p>";
                        result.status = TestResult.FAIL;
                    }
                    break;
                case 3:
                case 4:
                case 5:
                case 6:
                case 8:
                case 9:
                case 10:
                    if ((params.digital_out_1 == 1) && (params.digital_out_2 == 1))
                    {
                        result.analysis += "<p>Check Point " + mRunner.getLoopCounter() + ": PASS" + "</p>";
                    }
                    else
                    {
                        result.analysis += "<p>Check Point " + mRunner.getLoopCounter() + ": FAIL" + "</p>";
                        result.status = TestResult.FAIL;
                    }
                    break;
            }
            if (mRunner.getLoopCounter() == testLog.profile.getResultCount())
            {
                result.analysis += "<p>Verified that heating on relay_1 and fan on digital_2 are turned on when the room temperature is below set temperature by" +
                                   "heating deadband config.</p> ";
            }
        }
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
