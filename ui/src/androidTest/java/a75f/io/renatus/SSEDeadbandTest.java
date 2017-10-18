package a75f.io.renatus;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;

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
    
    @Before
    public void setUp() {
        mRunner =  new SimulationRunner(this, new SamplingProfile(10, 180));
    }
    
    @After
    public void tearDown() {
    }
    
    @Override
    public String getTestDescription() {
        return "Tests various deadband values under constant room_temperature and set temperature";
    }
    
    @Override
    public String getCCUStateFileName() {
        return null;
    }
    
    @Override
    public String getSimulationFileName() {
        return null;
    }
    
    @Override
    public void analyzeTestResults(SimulationTestInfo testLog) {
        if (mRunner.getLoopCounter() == 0) {
            return; // Test run not started , nothing to analyze
        }
        SimulationResult result = testLog.simulationResult;
        if (testLog.resultParamsMap.get(new Integer(7002)) != null)
        {
            SmartNodeParams params = testLog.resultParamsMap.get(new Integer(7002)).get(mRunner.getLoopCounter());
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
        DateTime sStart = new DateTime(System.currentTimeMillis(), DateTimeZone.getDefault());
        DateTime sEnd = new DateTime(System.currentTimeMillis() + 30*60000, DateTimeZone.getDefault());
        ArrayList<Schedule> schedules     = app.getFloors().get(0).mRoomList.get(0).mZoneProfiles.get(0).getSchedules();
        Day testDay = schedules.get(0).getDays().get(sStart.getDayOfWeek() - 1);
        testDay.setSthh(sStart.getHourOfDay());
        testDay.setEthh(sEnd.getHourOfDay());
        testDay.setStmm(sStart.getMinuteOfHour());
        testDay.setEtmm(sEnd.getMinuteOfHour());
    }
    @Override
    public void runTest() {
        
        System.out.println("runTest.........");
        
        mRunner.runSimulation();
    }
}
