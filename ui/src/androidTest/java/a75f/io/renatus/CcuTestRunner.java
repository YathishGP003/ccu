package a75f.io.renatus;

/**
 * Created by samjithsadasivan on 9/21/17.
 */

import android.support.test.InstrumentationRegistry;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import a75f.io.bo.building.CCUApplication;

import static java.lang.Thread.sleep;

/**
 * CcuTestRunner maintains list of all the test cases to be executed and pass/fail status, reason for failure, log and generic test run loop.
 */
public class CcuTestRunner
{
    private static CcuTestRunner                INSTANCE  = null;
    public         ArrayList<CcuSimulationTest> testSuite = new ArrayList<>();
    private        CcuTestEnv                   mEnv      = null;
    String[] testVectorParams = null;
    
    private CcuTestRunner() {
        mEnv = new CcuTestEnv(InstrumentationRegistry.getTargetContext().getApplicationContext());
    }
    
    public static CcuTestRunner getInstance() {
        if (INSTANCE == null) {
            return new CcuTestRunner();
        }
        return INSTANCE;
    }
    
    public CcuTestEnv getTestEnv() {
        return mEnv;
    }
    
    public void run(CcuSimulationTest ccuTest) {
        testSuite.add(ccuTest);
        CCUApplication state = CcuTestInputParser.parseStateConfig(mEnv, ccuTest.getCCUStateFileName());
        List<String[]> csvRows = CcuTestInputParser.parseSimulationFile(mEnv, ccuTest.getSimulationFileName());
    
        testVectorParams = csvRows.get(0);
        
        for (int simIndex = 1; simIndex < csvRows.size(); simIndex ++) {
            injectSimulation(csvRows.get(simIndex));
        }
        
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
    
    
    public void threadSleep(int seconds) {
        try {
            sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}
