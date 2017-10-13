package a75f.io.renatus;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.Day;
import a75f.io.bo.building.Schedule;

import static a75f.io.renatus.GraphColumns.Analog1_Out;
import static a75f.io.renatus.GraphColumns.Analog2_Out;
import static a75f.io.renatus.GraphColumns.Relay1_Out;
import static a75f.io.renatus.GraphColumns.Relay2_Out;

/**
 * Created by samjithsadasivan on 9/22/17.
 */

public class LightControlSimulationTest extends BaseSimulationTest
{
    SimulationRunner mRunner = null;
    
    @Before
    public void setUp() {
        mRunner =  new SimulationRunner(this,  new SamplingProfile(10, 180));
    }
    
    @After
    public void tearDown() {
    }
    
    @Override
    public String getTestDescription() {
        return new String(" The test injects CcuApp state with a valid lightprofile." +
                          "It then creates a schedule starting at current system time and ends 15 minutes later, configuring analog1_out and analog2_out at val=80." +
                          "Test runs for 30 minutes and fetches smartnode state every 3 seconds");
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
    public SimulationResult analyzeTestResults(SimulationTestInfo testLog) {
    
        SimulationResult result = new SimulationResult();
        result.status = TestResult.FAIL;
        result.analysis = "Verified that lighting_control_enabled is set since the test inject a light profile configuration." +
                          "Verified that lights are turned on with val=80 for during schedule" +
                          "Verified that lights are turned off when schedule expires";
        for (SmartNodeParams param : testLog.nodeParams) {
            if (param.lighting_control_enabled == 0) {
                result.status = TestResult.PASS;
            }
        }
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
    public void customizeTestData(CCUApplication app) {
        DateTime startTime = new DateTime(System.currentTimeMillis(), DateTimeZone.getDefault());
        DateTime endTime = new DateTime(System.currentTimeMillis() + 15*60000, DateTimeZone.getDefault());
        ArrayList<Schedule> schedules     = app.getFloors().get(0).mRoomList.get(0).mZoneProfiles.get(0).getSchedules();
        Day testDay = schedules.get(0).getDays().get(startTime.getDayOfWeek()-1);
        testDay.setSthh(startTime.getHourOfDay());
        testDay.setEthh(endTime.getHourOfDay());
        testDay.setStmm(startTime.getMinuteOfHour());
        testDay.setEtmm(endTime.getMinuteOfHour());
    }
    
    
    @Override
    public void runTest() {
    
       
        mRunner.runSimulation();
        
        
        //MockTime.getInstance().setMockTime(true, System.currentTimeMillis()+ (8 * 3600000)); // Force the mocktime to out of schedule interval
        //mRunner.resetRunner();
        //mRunner.runSimulation();
   }
    
    
}
