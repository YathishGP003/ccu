package a75f.io.renatus;

import android.util.Log;

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
import a75f.io.kinveybo.AlgoTuningParameters;
import a75f.io.renatus.framework.BaseSimulationTest;
import a75f.io.renatus.framework.SamplingProfile;
import a75f.io.renatus.framework.SimulationParams;
import a75f.io.renatus.framework.SimulationResult;
import a75f.io.renatus.framework.SimulationRunner;
import a75f.io.renatus.framework.SimulationTestInfo;
import a75f.io.renatus.framework.SmartNodeParams;
import a75f.io.renatus.framework.TestResult;

import static a75f.io.logic.L.ccu;
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

    int[] deadBandValArray = {0,0,4,4,1,1,3,3};

    @Before
    public void setUp() {
        mRunner =  new SimulationRunner(this, new SamplingProfile(1, 180));
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
                        result.analysis += "<p>Check Point " + runCounter + ": PASS" + "</p>";
                    }
                    else
                    {
                        result.analysis += "<p>Check Point " + runCounter + ": FAIL" + "</p>";
                        result.status = TestResult.FAIL;
                    }
                    break;
                case 3:
                case 7:
                    //cooling should be OFF with fan ON
                    if ((params.digital_out_1 == 0) && (params.digital_out_2 == 1))
                    {
                        result.analysis += "<p>Check Point " + runCounter + ": PASS" + "</p>";
                    }
                    else
                    {
                        result.analysis += "<p>Check Point " + runCounter + ": FAIL" + "</p>";
                        result.status = TestResult.FAIL;
                    }
                    break;
            }
            if (runCounter ==7)
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
    public HashMap<String,ArrayList<Float>> inputGraphData() {
        ArrayList<Float> rt = new ArrayList<>();
        ArrayList<Float> st = new ArrayList<>();
        final int maxRunCount = 7;
        HashMap<String,ArrayList<Float>> graphData = new HashMap<>();
        List<String[]> ip = mRunner.getSimulationInput();
        for (int simIndex = 1; simIndex < ip.size(); simIndex ++)
        {
            String[] simData = ip.get(simIndex);
            SimulationParams params = new SimulationParams().build(simData);
            rt.add(params.room_temperature);
            st.add(params.set_temperature);
        }

        while (rt.size() < maxRunCount)
        {
            Float lastVal = rt.get(rt.size()-1);
            rt.add(lastVal);
        }
        while (st.size() < maxRunCount)
        {
            Float lastVal = st.get(st.size()-1);
            st.add(lastVal);
        }

        graphData.put("room_temperature",rt);
        graphData.put("set_temperature",st);

        ArrayList<Float> db = new ArrayList<>();
        for (int val : deadBandValArray) {
            db.add((float)val);
        }
        graphData.put("cooling-deadband",db);
        return graphData;
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
            ArrayList<Schedule> schedules = app.getDefaultTemperatureSchedule();
            Day testDay = schedules.get(0).getDays().get(sStart.getDayOfWeek() - 1);
            testDay.setSthh(sStart.getHourOfDay());
            testDay.setEthh(sEnd.getHourOfDay());
            testDay.setStmm(sStart.getMinuteOfHour());
            testDay.setEtmm(sEnd.getMinuteOfHour());
            testDay.setVal((short)70);
        }

        AlgoTuningParameters algoMap = ccu().getDefaultCCUTuners();
        Log.i(TAG, "Putting " + AlgoTuningParameters.SSETuners.SSE_COOLING_DEADBAND + " : " +
                   testDeadBandVal);
        algoMap.put(AlgoTuningParameters.SSETuners.SSE_COOLING_DEADBAND, testDeadBandVal);
    }

    @Override
    public void runTest() {

        System.out.println("runTest.........");
        runCounter++;
        testDeadBandVal = deadBandValArray[runCounter];
        mRunner.runSimulation();
        runCounter +=2;
        testDeadBandVal = deadBandValArray[runCounter];
        mRunner.runSimulation();
        runCounter +=2;
        testDeadBandVal = deadBandValArray[runCounter];
        mRunner.runSimulation();
        runCounter +=2;
        testDeadBandVal = deadBandValArray[runCounter];
        mRunner.runSimulation();
        //rt=72, st= 70
    }
}
