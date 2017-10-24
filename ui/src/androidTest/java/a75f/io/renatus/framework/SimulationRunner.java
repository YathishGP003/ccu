package a75f.io.renatus.framework;

/**
 * Created by samjithsadasivan on 9/21/17.
 */

import android.text.format.DateFormat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import a75f.io.bo.building.CCUApplication;
import a75f.io.logic.L;

import static a75f.io.logic.L.ccu;
import static java.lang.Thread.sleep;

/**
 * SimulationRunner runs within android instrumentation shell to inject and verify simulation parameters.
 * It uses global static hashmap of SimulationContext class to track the tests beings run and corresponding param values.
 */
public class SimulationRunner
{
    public static final String NODE_TYPE_REST_URL = "http://10.0.2.2:5000/nodetype/";
    public static final String SMARTNODE_LOG_REST_URL = "http://10.0.2.2:5000/log/smartnode?address=";
    public static final String SMARTNODE_STATE_REST_URL = "http://10.0.2.2:5000/state/smartnode?address=";
    
    public  List<String[]>    csvDataList = null;
    private SimulationContext mEnv        = null;
    
    private BaseSimulationTest mCurrentTest;
    
    String[] testVectorParams = null;
    CCUApplication appState;
    
    int secondsElapsed = 0;
    
    private List<Integer> mNodes = new ArrayList<>();
    private SamplingProfile mProfile;
    
    long resultTime;
    int loopCounter;
       
    public SimulationRunner(BaseSimulationTest ccuTest, SamplingProfile profile) {
        mCurrentTest = ccuTest;
        mProfile = profile;
        mEnv = SimulationContext.getInstance();
    
        appState = SimulationInputParser.parseStateConfig(mEnv, ccuTest.getCCUStateFileName());
    
        csvDataList = SimulationInputParser.parseSimulationFile(mEnv, ccuTest.getSimulationFileName());
        testVectorParams = csvDataList.get(0);
        fillNodes();
    }
    
    /**
     * Must be called from @Test method of SimulationTest
     * Each invocation of method seeds the current app state to mesh network and starts a fresh round of injection of simulation values.
     */
    public void runSimulation() {
        if (mCurrentTest == null) {
            Assert.fail("Invalid test configuration");
        }
        if (appState == null || csvDataList == null)
        {
            addNTTestInfo();
        }
        loopCounter = 0;
        mCurrentTest.customizeTestData(appState);
        injectState(appState);
        DateTime currentSystemTime = new DateTime(System.currentTimeMillis());
        resultTime = System.currentTimeMillis();
        threadSleep(45);
        addTestLog();
        for (int simIndex = 1; simIndex < csvDataList.size(); simIndex ++) {
            String[] simData = csvDataList.get(simIndex);
            //injectSimulation(simData);
            SimulationParams params  = new SimulationParams().build(simData);
            String paramsJson = params.convertToJsonString();
            executePost(SMARTNODE_STATE_REST_URL + simData[1].trim(), getHttpPostParams(paramsJson));
            threadSleep(mProfile.resultPeriodSecs);
            loopCounter++;
            addTestLog();
            resultTime = System.currentTimeMillis();
        }
        
        runResultLoop();
    }
    
    public void setDate(DateTime dateTime)
    {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            
            os.writeBytes("date -s 20120419.024012; \n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    private void runResultLoop() {
        while (++loopCounter <= mProfile.resultCount)
        {
            threadSleep(mProfile.resultPeriodSecs);
            addTestLog();
        }
    }
    
    public int getLoopCounter() {
        return loopCounter;
    }
    
    private void injectState(CCUApplication state) {
        L.saveCCUState(state);
    }
    
    private void injectSimulation(String[] testVals)
    {
        final String TIME_FORMAT = "hh:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
        try
        {
            Date d = sdf.parse(testVals[0]);
            int waitTime = d.getHours()*3600+d.getMinutes()*60+d.getSeconds() - secondsElapsed;
            threadSleep(waitTime);
            secondsElapsed += waitTime;
            SimulationParams params  = new SimulationParams().build(testVals);
            String paramsJson = params.convertToJsonString();
            executePost(SMARTNODE_STATE_REST_URL+testVals[1].trim(), getHttpPostParams(paramsJson) );
        }
        catch (ParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public String getHttpPostParams(String json) {
        final ObjectMapper mapper = new ObjectMapper();
        StringBuilder urlParameters = new StringBuilder();
        try
        {
            final JsonNode paramsTree = mapper.readTree(json);
            Iterator<String> fields = paramsTree.fieldNames();
            while (fields.hasNext())
            {
                String key = fields.next();
                String val = paramsTree.get(key).asText();
                if (urlParameters.toString() == "")
                {
                    urlParameters.append(key + "=" + URLEncoder.encode(val, "UTF-8"));
                } else {
                    urlParameters.append("&"+ key + "=" + URLEncoder.encode(val, "UTF-8"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urlParameters.toString();
    }
    
    private String executePost(String targetURL, String urlParameters)
    {
        URL url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            url = new URL(targetURL);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            
            connection.setRequestProperty("Content-Length", "" +
                                                            Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");
            
            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            
            //Send request
            DataOutputStream wr = new DataOutputStream (connection.getOutputStream ());
            wr.writeBytes (urlParameters);
            wr.flush ();
            wr.close ();
            
            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
            
        } catch (Exception e) {
            
            e.printStackTrace();
            return null;
            
        } finally {
            
            if(connection != null) {
                connection.disconnect();
            }
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
        SimulationTestInfo info = mEnv.testSuite.getSimulationTest(mCurrentTest.getClass().getSimpleName());
        if (info == null) {
            info = new SimulationTestInfo();
            info.name = mCurrentTest.getClass().getSimpleName();;
            info.description = mCurrentTest.getTestDescription();
            info.simulationResult = new SimulationResult();
            info.simulationInput = csvDataList;
            info.inputCcuState = appState;
            info.ipGraphColumns = mCurrentTest.inputGraphData();
            info.graphColumns = mCurrentTest.graphColumns();
            info.profile= mProfile;
            mEnv.testSuite.addSimulationTest(mCurrentTest.getClass().getSimpleName(),info);
            //info.nodeParams.add(new SmartNodeParams());
            for(int node : mNodes)
            {
                info.resultParamsMap.put(node, new ArrayList<SmartNodeParams>());
            }
        }
        
        for(int node : mNodes)
        {
            String params = getResult(SMARTNODE_LOG_REST_URL + node + "&since=" + DateFormat.format("dd-MMMM-yyyy_hh:mm:ss", resultTime).toString());
            try
            {
                JSONArray jsonArray = new JSONArray(params);
                SmartNodeParams result = SmartNodeParams.getParamsFromJson(jsonArray.get(jsonArray.length()-1).toString());
                info.resultParamsMap.get(node).add(result);
                
            } catch (JSONException e) {
                //TODO- Refactor
                //Random failures observed while retrieving json params.We just proceed with adding an empty struct to avoid losing check point mapping.
                info.resultParamsMap.get(node).add(new SmartNodeParams());
                e.printStackTrace();
            }
        }
        try
        {
            mCurrentTest.analyzeTestResults(info); //result is updated in info object
        } catch (Exception e) {
            //Test should go on even if one check point analysis failed
            info.simulationResult.analysis += "<p>Check Point " + loopCounter + " NA : Exception "+e.getMessage() + "</p>";
        }
    }
    
    private void addNTTestInfo() {
        SimulationTestInfo info = mEnv.testSuite.getSimulationTest(mCurrentTest.getClass().getSimpleName());
        if (info == null) {
            info = new SimulationTestInfo();
            info.name = mCurrentTest.getClass().getSimpleName();;
            info.description = mCurrentTest.getTestDescription();
            info.simulationResult = new SimulationResult();
            info.simulationResult.status = TestResult.NT;
            info.simulationInput = csvDataList;
            info.inputCcuState = appState;
            info.graphColumns = mCurrentTest.graphColumns();
            info.profile= mProfile;
            mEnv.testSuite.addSimulationTest(mCurrentTest.getClass().getSimpleName(),info);
        }
    }
    private void fillNodes() {
        for (int i = 1; i < csvDataList.size(); i++)
        {
            String[] simData = csvDataList.get(i);
            if (!mNodes.contains(Integer.parseInt(simData[1].trim())))
            {
                mNodes.add(Integer.parseInt(simData[1].trim()));
            }
        }
    }
    
    public void resetRunner() {
        secondsElapsed = 0;
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
