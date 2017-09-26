package a75f.io.renatus;

/**
 * Created by samjithsadasivan on 9/21/17.
 */

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import a75f.io.bo.building.CCUApplication;

import static a75f.io.logic.L.ccu;
import static java.lang.Thread.sleep;


public class SimulationRunner
{
    
    public List<String[]> csvDataArray = null;
    CcuTestEnv mEnv = null;
    
    String[] testVectorParams = null;
    
    public void runSimulation(CcuSimulationTest ccuTest) {
        
        mEnv = CcuTestEnv.getInstance();
        
        CCUApplication state = CcuTestInputParser.parseStateConfig(mEnv, ccuTest.getCCUStateFileName());
        injectState(state);
    
        csvDataArray = CcuTestInputParser.parseSimulationFile(mEnv, ccuTest.getSimulationFileName());
        testVectorParams = csvDataArray.get(0);
        
        for (int simIndex = 1; simIndex < csvDataArray.size(); simIndex ++) {
            injectSimulation(csvDataArray.get(simIndex));
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
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
        try
        {
            Date d = sdf.parse(testVals[0]);
            sleep(d.getTime() - System.currentTimeMillis());
            SimulationParams params  = new SimulationParams().build(testVals);
            
            String paramsJson = params.convertToJsonString();
    
            postJson("http://localhost:5000/state/smartnode?address="+testVals[1].trim(), paramsJson);
        }
        catch (ParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InterruptedException e) {
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
    
    
    public void threadSleep(int seconds) {
        try {
            sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}
