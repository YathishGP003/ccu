package a75f.io.renatus;

/**
 * Created by samjithsadasivan on 9/21/17.
 */

import android.os.Environment;
import android.text.format.DateFormat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.Map;

import a75f.io.bo.building.CCUApplication;

import static a75f.io.logic.L.ccu;
import static java.lang.Thread.sleep;

public class SimulationRunner
{
    
    public List<String[]> csvDataList = null;
    private CcuTestEnv mEnv = null;
    
    private CcuSimulationTest mCurrentTest;
    
    String[] testVectorParams = null;
    CCUApplication appState;
    
    int secondsElapsed = 0;
    
    private List<Integer> mNodes = new ArrayList<>();
    
    String curTime = null;
    int resultCounter = 0;
    
    SimulationRunner(CcuSimulationTest ccuTest) {
        mCurrentTest = ccuTest;
        mEnv = CcuTestEnv.getInstance();
    
        appState = CcuTestInputParser.parseStateConfig(mEnv, ccuTest.getCCUStateFileName());
    
        csvDataList = CcuTestInputParser.parseSimulationFile(mEnv, ccuTest.getSimulationFileName());
        testVectorParams = csvDataList.get(0);
    }
    
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
    
    public void injectState(CCUApplication state) {
        CCUApplication currentState = ccu();
        currentState.setTitle(state.getTitle());
        currentState.setFloors(state.getFloors());
        currentState.setSmartNodeAddressBand(state.getSmartNodeAddressBand());
        currentState.systemProfile = state.systemProfile;
        currentState.controlMote = state.controlMote;
        
    }
    
    public void injectSimulation(String[] testVals)
    {
        final String TIME_FORMAT = "hh:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
        curTime = new Date(System.currentTimeMillis()).toString();
        try
        {
            Date d = sdf.parse(testVals[0]);
            int waitTime = d.getHours()*3600+d.getMinutes()*60+d.getSeconds() - secondsElapsed;
            threadSleep(waitTime);
            secondsElapsed += waitTime;
            
            SimulationParams params  = new SimulationParams().build(testVals);
            String paramsJson = params.convertToJsonString();
    
            JSONObject snType = new JSONObject();
            try
            {
                snType.put("node_type", "smartnode");
            } catch (JSONException e) {
                e.printStackTrace();
            }
    
            postJson("http://localhost:5000/nodetype/", snType.toString());
            postJson("http://localhost:5000/state/smartnode?address="+testVals[1].trim(), paramsJson);
        }
        catch (ParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void postJson(String url, String data){
        try {
            URL restUrl = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection)restUrl.openConnection();
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
        }
    }
    
    public String getResult(String url){
        
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
    
    
    public void addTestLog() {
        CcuSimulationTestInfo info = mEnv.testSuite.get(mCurrentTest.getTestDescription());
        if (info == null) {
            info = new CcuSimulationTestInfo();
            info.name = mCurrentTest.getTestDescription();
            info.simulationResult = new SimulationResult();
            info.simulationResult.result = TestResult.NA;
            
            info.simulationInput = csvDataList;
            info.inputCcuState = appState;
            mEnv.testSuite.put(mCurrentTest.getTestDescription(),info);
        }
        //String params = CcuTestInputParser.readFileFromAssets(CcuTestEnv.getInstance().getContext(), "ccustates/testresult.json");
        
        //http://localhost:5000/log/smartnode?address=2000&since=05-April-2017_17:00:40&limit_results=1
        for(int node = 0; node < mNodes.size();node++)
        {
            String params = getResult("http://localhost:5000/log/smartnode?address=" + mNodes.get(node) + "&since=" + curTime + "&limit_results=1");
            info.nodeParams.add(SmartNodeParams.getParamsFromJson(params));
        }
        
        //String params2 = CcuTestInputParser.readFileFromAssets(CcuTestEnv.getInstance().getContext(), "ccustates/testresult1.json");
        //info.nodeParams.add(SmartNodeParams.getParamsFromJson(params2));
        
        
    }
    
    public void saveReport() {
        
        String path = Environment.getExternalStorageDirectory().getPath();
        String testReport = "<br /><br /><h3>Simulation Test Summary</h3>"
                                    .concat("<table width:200; border=1; cellspacing=0; cellpadding=0; table-layout:fixed; word-wrap:break-word;>")
                                    .concat("<tr>")
                                    .concat("<th> Test Case </th>")
                                    .concat("<th> Status </th>")
                                    .concat("<th> Simulation Input </th>")
                                    .concat("<th> Ccu State </th>")
                                    .concat("<th> SmartNode State</th>")
                                    .concat("</tr>");
        
        for (Map.Entry<String, CcuSimulationTestInfo> test : mEnv.testSuite.entrySet()) {
            testReport = testReport.concat(test.getValue().getHtml());
        }
        
        testReport = testReport.concat("</table>");
    
        String fileName = DateFormat.format("dd_MM_yyyy_hh_mm_ss", System.currentTimeMillis()).toString();
        File file = new File(path, fileName+"_Simulation.html");
        String html = "<html><head><title>Simulation Test Report</title></head><body>"+testReport+"</body></html>";

        try {
            FileOutputStream out = new FileOutputStream(file);
            byte[] data = html.getBytes();
            out.write(data);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    
    public void threadSleep(int seconds) {
        try {
            sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}
