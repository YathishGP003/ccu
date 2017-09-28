package a75f.io.renatus;

/**
 * Created by samjithsadasivan on 9/21/17.
 */

import android.os.Environment;

import org.joda.time.DateTime;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import a75f.io.bo.building.CCUApplication;

import static a75f.io.logic.L.ccu;
import static java.lang.Thread.sleep;

public class SimulationRunner
{
    
    public List<String[]> csvDataArray = null;
    CcuTestEnv mEnv = null;
    
    CcuSimulationTest mCurrentTest;
    
    String[] testVectorParams = null;
    CCUApplication appState;
    
    public void runSimulation(CcuSimulationTest ccuTest) {
    
        mCurrentTest = ccuTest;
        mEnv = CcuTestEnv.getInstance();
    
        appState = CcuTestInputParser.parseStateConfig(mEnv, ccuTest.getCCUStateFileName());
        injectState(appState);
    
        csvDataArray = CcuTestInputParser.parseSimulationFile(mEnv, ccuTest.getSimulationFileName());
        testVectorParams = csvDataArray.get(0);
        
        for (int simIndex = 1; simIndex < csvDataArray.size(); simIndex ++) {
            injectSimulation(csvDataArray.get(simIndex));
        }
        
        addTestInfo();
        
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
        try
        {
            Date d = sdf.parse(testVals[0]);
            //int waitTime = d.getHours()*3600+d.getMinutes()*60+d.getSeconds();
            //sleep(d.getTime());
            //threadSleep();
            SimulationParams params  = new SimulationParams().build(testVals);
            
            String paramsJson = params.convertToJsonString();
    
            //postJson("http://localhost:5000/state/smartnode?address="+testVals[1].trim(), paramsJson);
        }
        catch (ParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       /* catch (InterruptedException e) {
            e.printStackTrace();
        }*/
       
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
    
    public void addTestInfo() {
        CcuSimulationTestInfo info = new CcuSimulationTestInfo();
        info.name = mCurrentTest.getTestDescription();
        info.simulationResult = new SimulationResult();
        info.simulationResult.result = TestResult.PASS;
        
        info.simulationInput = csvDataArray;
        info.inputCcuState = appState;
        String params = CcuTestInputParser.readFileFromAssets(CcuTestEnv.getInstance().getContext(), "ccustates/testresult.json");
        SmartNodeParams snParams = null;
        snParams = SmartNodeParams.getParamsFromJson(params);
        info.nodeParams.add(snParams);
        mEnv.testSuite.put(mCurrentTest.getTestDescription(),info);
    }
    
    public void saveReport() {
        
        String path = Environment.getExternalStorageDirectory().getPath();//style="width:100%"
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
    
        //String fileName = DateFormat.format("dd_MM_yyyy_hh_mm_ss", System.currentTimeMillis()).toString();
        String name = "sim.html";
        File file = new File(path, name);
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
    
    public void threadSleep(int seconds) {
        try {
            sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}
