package a75.io.renatus.testharness;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.SystemProperties;
import a75f.io.renatus.RenatusLandingActivity;
import a75f.io.renatus.SplashActivity;

import static java.lang.Thread.sleep;

/**
 * Created by samjithsadasivan on 4/3/18.
 */
@RunWith (TestHarnessRunner.class)
public class SimulationTest
{
    //http://127.0.0.1:8080


    public static final String TEST_DATA_API  = "RETRIEVE_TEST_DATA";
    public static final String START_TEST_API = "START_TEST";

    private Context mContext;
    String testURL;
    long   testTime;
    
    String ccuState;
    String siteId;
    String biskitState;
    
    private boolean mStartTest = false;
    
    
    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
        String biskitIp = SystemProperties.get("biskit_ip");
        if (biskitIp != null && !biskitIp.isEmpty()) {
            testURL = "http://"+biskitIp+":8080/";
        } else {
            testURL = "http://10.0.2.2:8080/";// For emulator running on the same host
        }
    }
    
    @After
    public void tearDown() {
    }
    
    public void trialRun() {
        
        CCUStateParser parser = new CCUStateParser();
        parser.pullHayStackDb("5c4ff0e824aa9a00f4a99bb4");
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(mContext,RenatusLandingActivity.class.getName());
        mContext.startActivity(intent);
        threadSleep(120);
        
        /*HashMap site = CCUHsApi.getInstance().read("site");
        CCUHsApi.getInstance().deleteEntityTree(site.get("id").toString());
        parser.createHayStackDb(TestConfig.sGrid);
        mContext.startActivity(intent);
        threadSleep(120);*/

    }
    
    @Test
    public void runTest() {
        
        CCUStateParser parser = new CCUStateParser();
        boolean runTest = true;
        CCUHsApi.getInstance().testHarnessEnabled = true;
        do {
    
            threadSleep(60);
            
            if (mStartTest) {
                System.out.println("Start Test");
                startTest();
            } else
            {

                String data = executeHttpGet(testURL + TEST_DATA_API);
                if (data == null || data.length() == 0) {
                    continue;
                }
                parseBiskitData(data);
                if ("NOT_RUNNING".equals(biskitState))
                {
                    //Finish test when BISKIT returns empty data
                    if (ccuState.isEmpty())
                    {
                        System.out.println("No valid state , exit Test");
                        runTest = false;
                    }
                    else
                    {
                        System.out.println("Test data : " + ccuState);
                        //parser.parseAndInjectState(mContext, ccuState);
                        parser.pullHayStackDb(siteId);
                        setTime();
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setClassName(mContext,SplashActivity.class.getName());
                        mContext.startActivity(intent);
                        mStartTest = true;
                    }
                }
            }
            
        } while (runTest);
    
        CCUHsApi.getInstance().testHarnessEnabled = false;
    }
    
    private void startTest() {
        
        String response = executeHttpGet(testURL + START_TEST_API);
        try {
        JSONObject responseJson = new JSONObject(response);
        String status = responseJson.getString("status");
        if ("Success".equals(status)) {
            mStartTest = false;
        }
        } catch (JSONException e) {
            Assert.fail("Failed to Start Test " + e.getMessage());
        }
    }
    
    private void parseBiskitData(String data) {
        try {
            JSONObject stateJson = new JSONObject(data);
        
            biskitState = stateJson.getString("state");
            //ccuState = stateJson.getString("data");
            
            JSONObject dataJson = stateJson.getJSONObject("data");
            Log.d("TestHarness"," dataJson "+dataJson);
            siteId = dataJson.getString("siteId");
            Log.d("TestHarness"," siteId "+siteId);
            testTime = stateJson.optLong("time");
        
        }catch (JSONException e) {
            Assert.fail("Failed to parse BISKIT data " + e.getMessage());
        }
    }
    
    private String executeHttpGet(String targetURL)
    {
        System.out.println("Test : GET " + targetURL);
        URL url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            url = new URL(targetURL);
            connection = (HttpURLConnection)url.openConnection();
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
            Assert.fail(e.getMessage());
            e.printStackTrace();
            return null;
            
        } finally {
            
            if(connection != null) {
                connection.disconnect();
            }
        }
    }

    private void setTime() {

        if (testTime == 0) {
            System.out.println("Invalid time");
            return;
        }
        // date command has been changed in Marshmallow ( emulator)
        // Temp fix to assume the test is being run from Kitkat tablet if biskit_ip is set.
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            SimpleDateFormat formatter ;
            String dateTime;
            String deviceIp = SystemProperties.get("biskit_ip");
            if (deviceIp != null && deviceIp.length() > 0) {
                formatter =  new SimpleDateFormat("yyyyMMdd.HHmmss");
                dateTime = formatter.format(new Date(testTime));
                os.writeBytes("date -s "+dateTime);
            } else {
                formatter = new SimpleDateFormat("MMddHHmmyy.ss");
                dateTime = formatter.format(new Date(testTime));
                os.writeBytes("date "+dateTime+" SET");
            }
            System.out.println("Test : set Time " + dateTime);
            
        } catch (IOException e) {

            System.out.println("Test : Failed to set time " + e.getMessage());
        }


    }
    
    private void threadSleep(int seconds) {
        try {
            sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    
}
