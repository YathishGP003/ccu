package a75f.io.renatus;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.Day;
import a75f.io.bo.building.Schedule;
import a75f.io.renatus.framework.BaseSimulationTest;
import a75f.io.renatus.framework.SamplingProfile;
import a75f.io.renatus.framework.SimulationParams;
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

public class SSEScheduleTest extends BaseSimulationTest
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
        return "Tests whether a future cooling schedule is fired on time." +
               "Creates a schedule to start cooling 15 minutes later.Relay1 and Relay2 outputs of smartnode 7004 configured as Cooling and Fan respectively." +
               "Room temperature is kept above set temperature beyond the deadband value to trigger cooling at the start of schedule.";
    }
    
    @Override
    public String getCCUStateFileName() {
        return "sseschedule.json";
    }
    
    @Override
    public String getSimulationFileName() {
        return "sseschedule.csv";
    }
    
    @Override
    public void analyzeTestResults(SimulationTestInfo testLog) {
        if (mRunner.getLoopCounter() == 0) {
            return; // Test run not started , nothing to analyze
        }
        SimulationResult result = testLog.simulationResult;
        if (testLog.resultParamsMap.get(new Integer(7004)) != null)
        {
            SmartNodeParams params = testLog.resultParamsMap.get(new Integer(7004)).get(mRunner.getLoopCounter());
            switch (mRunner.getLoopCounter())
            {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    // Not in schedule ; both cooling and fan should be OFF
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
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                    //IN schedule ; both cooling and fan should be on
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
                result.analysis += "<p>Verified that cooling on relay_1 and fan on digital_2 are turned ON after 15 mins when schedule started</p> ";
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
    public void customizeTestData(CCUApplication app) {
        DateTime sStart = new DateTime(System.currentTimeMillis() + 15*60000, DateTimeZone.getDefault());
        DateTime sEnd = new DateTime(System.currentTimeMillis() + 30*60000, DateTimeZone.getDefault());
        ArrayList<Schedule> schedules     = app.getDefaultTemperatureSchedule();
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
