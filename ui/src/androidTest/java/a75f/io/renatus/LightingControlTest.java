package a75f.io.renatus;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.logic.bo.building.CCUApplication;
import a75f.io.logic.bo.building.Day;
import a75f.io.logic.bo.building.Schedule;
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
 * Created by samjithsadasivan on 9/22/17.
 */

public class LightingControlTest extends BaseSimulationTest
{
    SimulationRunner mRunner = null;
    
    DateTime sStart;
    DateTime sEnd;
    
    @Before
    public void setUp() {
        mRunner =  new SimulationRunner(this,  new SamplingProfile(10, 180));
    }
    
    
    
    
    @After
    public void tearDown() {
    }
    
    @Override
    public String getTestDescription() {
        return " The test injects CcuApp state with a valid lightprofile with two analog out ports of smartnode 7000. " +
                          "It then creates a schedule starting at current system time and ends 15 minutes later, configuring analog1_out and analog2_out at val=80." +
                          "Test runs for 30 minutes and fetches smartnode state every 3 minutes.";
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
    public void analyzeTestResults(SimulationTestInfo testLog) {
    
        if (mRunner.getLoopCounter() == 0) {
            return; // Test run not started , nothing to analyze
        }
        SimulationResult result = testLog.simulationResult;
        if (testLog.resultParamsMap.get(new Integer(7000)) != null)
        {
            SmartNodeParams params = testLog.resultParamsMap.get(new Integer(7000)).get(mRunner.getLoopCounter());
            switch (mRunner.getLoopCounter())
            {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    
                    if ((params.lighting_control_enabled == 1) && (params.analog_out_1 == 80) && (params.analog_out_2 == 80))
                    {
                        result.analysis += "<p>Check Point " + mRunner.getLoopCounter() + ": PASS" + "</p>";
                    }
                    else
                    {
                        /*DateTimeFormatter formatter = DateTimeFormat.forPattern("dd MMM yyyy HH:mm:ss");
                        DateTime resultSt = formatter.parseDateTime(params.timestamp);
                        if (resultSt.isAfter(sEnd)) {
                            //Work around for a case when schedule ends even before the local loop reaches 5
                            result.analysis += "<p>Check Point " + mRunner.loopCounter + ": PASS" + "</p>";
                        } else*/
                        {
                            result.analysis += "<p>Check Point " + mRunner.getLoopCounter() + ": FAIL" + "</p>";
                            result.status = TestResult.FAIL;
                        }
                    }
                    break;
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                    if ((params.lighting_control_enabled == 1) && (params.analog_out_1 == 0) && (params.analog_out_2 == 0))
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
                result.analysis += "<p>Verified that lighting_control_enabled is set since the test injected a light profile configuration." +
                                   "Verified that lights are turned on with val=80 when schedule is active." + "Verified that lights are turned off when schedule expired.</p>";
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
        DateTime sStart = new DateTime(System.currentTimeMillis()/*, DateTimeZone.getDefault()*/);
        DateTime sEnd = new DateTime(System.currentTimeMillis() + 15*60000/*, DateTimeZone.getDefault()*/);
        ArrayList<Schedule> schedules     = app.getFloors().get(0).mRoomList.get(0).mZoneProfiles.get(0).getSchedules();
        Day testDay = schedules.get(0).getDays().get(sStart.getDayOfWeek()-1);
        testDay.setSthh(sStart.getHourOfDay());
        testDay.setEthh(sEnd.getHourOfDay());
        testDay.setStmm(sStart.getMinuteOfHour());
        testDay.setEtmm(sEnd.getMinuteOfHour()+1); //seconds are rounded-off by the scheduler , so adding a minute of padding to avoid scheduler firing earlier than 15 mins
    }
    
    @Override
    public void runTest() {
        mRunner.runSimulation();
        //MockTime.getInstance().setMockTime(true, System.currentTimeMillis()+ (8 * 3600000)); // Force the mocktime to out of schedule interval
        //mRunner.resetRunner();
        //mRunner.runSimulation();
   }
    
    
}
