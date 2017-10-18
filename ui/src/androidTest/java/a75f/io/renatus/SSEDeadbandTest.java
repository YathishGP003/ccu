package a75f.io.renatus;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.Day;
import a75f.io.bo.building.Schedule;
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
 * Created by samjithsadasivan on 10/17/17.
 */

public class SSEDeadbandTest extends BaseSimulationTest
{
    SimulationRunner mRunner = null;
    int runCounter = 0;
    int testDeadBandVal;
    
    @Before
    public void setUp() {
        mRunner =  new SimulationRunner(this, new SamplingProfile(1, 120));
    }
    
    @After
    public void tearDown() {
    }
    
    @Override
    public String getTestDescription() {
        return "Tests various deadband values under constant room_temperature and set temperature." +
               "Creates a 30 minute schedule to start cooling immediately.Relay1 and Relay2 outputs of smartnode 7003 configured as Cooling and Fan respectively." +
               "Room temperature is kept above set temperature. Activation of cooling is monitored for different deadband values.";
    }
    
    @Override
    public String getCCUStateFileName() {
        return "ssedeadband.json";
    }
    
    @Override
    public String getSimulationFileName() {
        return "ssedeadband.csv";
    }
    
    @Override
    public void analyzeTestResults(SimulationTestInfo testLog) {
        if (mRunner.getLoopCounter() == 0) {
            return; // Test run not started , nothing to analyze
        }
        SimulationResult result = testLog.simulationResult;
        if (testLog.resultParamsMap.get(new Integer(7003)) != null)
        {
            SmartNodeParams params = testLog.resultParamsMap.get(new Integer(7003)).get(runCounter);
            switch (runCounter)
            {
                case 1:
                case 5:
                    //cooling and fan should be ON
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
                case 3:
                case 7:
                    //cooling should be OFF with fan ON
                    if ((params.digital_out_1 == 0) && (params.digital_out_2 == 1))
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
                result.analysis += "<p>Verified that cooling on relay_1 is turned ON/OFF appropriately for current value of deadband</p> ";
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
    public void customizeTestData(CCUApplication app) {
        if (runCounter <= 1)
        {
            DateTime sStart = new DateTime(System.currentTimeMillis(), DateTimeZone.getDefault());
            DateTime sEnd = new DateTime(System.currentTimeMillis() + 30 * 60000, DateTimeZone.getDefault());
            ArrayList<Schedule> schedules = app.getFloors().get(0).mRoomList.get(0).mZoneProfiles.get(0).getSchedules();
            Day testDay = schedules.get(0).getDays().get(sStart.getDayOfWeek() - 1);
            testDay.setSthh(sStart.getHourOfDay());
            testDay.setEthh(sEnd.getHourOfDay());
            testDay.setStmm(sStart.getMinuteOfHour());
            testDay.setEtmm(sEnd.getMinuteOfHour());
        }
    
        HashMap<String, Object> algoMap = app.getDefaultCCUTuners();
        algoMap.put("sseCoolingDeadBand", testDeadBandVal);
    }
    @Override
    public void runTest() {
        
        System.out.println("runTest.........");
        //rt=72, st= 70
        runCounter++;
        testDeadBandVal = 0;
        mRunner.runSimulation();
        runCounter +=2;
        testDeadBandVal = 0;
        mRunner.runSimulation();
        runCounter +=2;
        testDeadBandVal = 1;
        mRunner.runSimulation();
        runCounter +=2;
        testDeadBandVal = 3;
        mRunner.runSimulation();
    }
}
