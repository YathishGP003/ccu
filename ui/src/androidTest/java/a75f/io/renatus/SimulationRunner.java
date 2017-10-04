package a75f.io.renatus;

/**
 * Created by samjithsadasivan on 9/21/17.
 */

import android.text.format.DateFormat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import a75f.io.bo.building.CCUApplication;

import static a75f.io.logic.L.ccu;
import static java.lang.Thread.sleep;

/**
 * SimulationRunner runs within android instrumentation shell to inject and verify simulation parameters.
 * It uses global static hashmap of SimulationContext class to track the tests beings run and corresponding param values.
 */
public class SimulationRunner
{
    
    public  List<String[]>    csvDataList = null;
    private SimulationContext mEnv        = null;
    
    private BaseSimulationTest mCurrentTest;
    
    String[] testVectorParams = null;
    CCUApplication appState;
    
    int secondsElapsed = 0;
    
    private List<Integer> mNodes = new ArrayList<>();
    
    String curTime = null;
    
    SimulationRunner(BaseSimulationTest ccuTest) {
        mCurrentTest = ccuTest;
        mEnv = SimulationContext.getInstance();
    
        appState = SimulationInputParser.parseStateConfig(mEnv, ccuTest.getCCUStateFileName());
    
        csvDataList = SimulationInputParser.parseSimulationFile(mEnv, ccuTest.getSimulationFileName());
        testVectorParams = csvDataList.get(0);
    }
    
    /**
     * Must be called from @Test method of SimulationTest
     */
    public void runSimulation() {
        
        injectState(appState);
        
        for (int simIndex = 1; simIndex < csvDataList.size(); simIndex ++) {
            String[] simData = csvDataList.get(simIndex);
            if (!mNodes.contains(Integer.parseInt(simData[1].trim()))) {
                mNodes.add(Integer.parseInt(simData[1].trim()));
            }
            injectSimulation(simData);
    
            addTestLog();//TODO - should wait ?
        }
        
    }
    
    private void injectState(CCUApplication state) {
        CCUApplication currentState = ccu();
        currentState.setTitle(state.getTitle());
        currentState.setFloors(state.getFloors());
        currentState.setSmartNodeAddressBand(state.getSmartNodeAddressBand());
        currentState.systemProfile = state.systemProfile;
        currentState.controlMote = state.controlMote;
        
    }
    
    private void injectSimulation(String[] testVals)
    {
        final String TIME_FORMAT = "hh:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
        curTime = DateFormat.format("dd-MMMM-yyyy_hh:mm:ss", System.currentTimeMillis()).toString();
        try
        {
            Date d = sdf.parse(testVals[0]);
            int waitTime = d.getHours()*3600+d.getMinutes()*60+d.getSeconds() - secondsElapsed;
            threadSleep(waitTime);
            secondsElapsed += waitTime;
            
            SimulationParams params  = new SimulationParams().build(testVals);
            String paramsJson = params.convertToJsonString();
    
            postJson("http://10.0.2.2:5000/nodetype/", getSmartnodeType());
            postJson("http://10.0.2.2:5000/state/smartnode?address="+testVals[1].trim(), paramsJson);
        }
        catch (ParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void postJson(String url, String data){
        HttpURLConnection httpURLConnection = null;
        try {
            URL restUrl = new URL(url);
            httpURLConnection = (HttpURLConnection)restUrl.openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.connect();
        
            DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();
        
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally {
            httpURLConnection.disconnect();
        }
    }
    
    private String getResult(String url){
        
        StringBuilder result = new StringBuilder();
            
        HttpURLConnection urlConnection = null;
        
        try {
            URL restUrl = new URL(url);
            urlConnection = (HttpURLConnection) restUrl.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            
        }catch( Exception e) {
            e.printStackTrace();
        }
        finally {
            urlConnection.disconnect();
        }
        return result.toString();
    }
    
    private String getSmartnodeType() {
        JSONObject snType = new JSONObject();
        try
        {
            snType.put("node_type", "smartnode");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return snType.toString();
    }
    
    private void addTestLog() {
        SimulationTestInfo info = mEnv.testSuite.getSimulationTest(mCurrentTest.getTestDescription());
        if (info == null) {
            info = new SimulationTestInfo();
            info.name = mCurrentTest.getTestDescription();
            info.simulationResult = new SimulationResult();
            info.simulationResult.result = TestResult.NA;
            
            info.simulationInput = csvDataList;
            info.inputCcuState = appState;
            mEnv.testSuite.addSimulationTest(mCurrentTest.getTestDescription(),info);
        }
        //String params = SimulationInputParser.readFileFromAssets(SimulationContext.getInstance().getContext(), "ccustates/testresult.json");
        
        //http://localhost:5000/log/smartnode?address=2000&since=05-April-2017_17:00:40&limit_results=1
        for(int node = 0; node < mNodes.size();node++)
        {
            postJson("http://10.0.2.2:5000/nodetype/", getSmartnodeType());
            String params = getResult("http://10.0.2.2:5000/log/smartnode?address=" + mNodes.get(node) + "&since=" + curTime + "&limit_results=1");
            info.nodeParams.add(SmartNodeParams.getParamsFromJson(params));
        }
        
        //String params2 = SimulationInputParser.readFileFromAssets(SimulationContext.getInstance().getContext(), "ccustates/testresult1.json");
        //info.nodeParams.add(SmartNodeParams.getParamsFromJson(params2));
        
        
    }
    
    public List<String[]> getSimulationInput() {
        return csvDataList;
    }
    
    public long duration() {
        int lastTestIndex = csvDataList.size();
        long duration = 0;
        final String TIME_FORMAT = "hh:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
        try
        {
            Date d = sdf.parse(csvDataList.get(lastTestIndex)[0]);
            duration = d.getHours() * 3600 + d.getMinutes() * 60 + d.getSeconds() ;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return duration * 1000;
    }
    
    private void threadSleep(int seconds) {
        try {
            sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}
