package a75f.io.renatus;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
        return "Verifies SSE cooling during an occupied schedule." +
               "The test injects a CcuState with a 30 mins schedule starting at current time.Relay1 and Relay2 outputs of smartnode 7001 configured as Heating and Fan respectively." +
               "It sends 10 sets of inputs with varying room-temperature and set-temperature evey 3 minutes." +
               "and fetches smart node params corresponding to each input.";
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
    public HashMap<String,ArrayList<Float>> inputGraphData() {
        ArrayList<Float> rt = new ArrayList<>();
        ArrayList<Float> st = new ArrayList<>();
        HashMap<String,ArrayList<Float>> graphData = new HashMap<>();
        List<String[]> ip = mRunner.getSimulationInput();
        for (int simIndex = 1; simIndex < ip.size(); simIndex ++)
        {
            String[] simData = ip.get(simIndex);
            SimulationParams params = new SimulationParams().build(simData);
            rt.add(params.room_temperature);
            st.add(params.set_temperature);
        }
        graphData.put("room_temperature",rt);
        graphData.put("set_temperature",st);
        
        return graphData;
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
