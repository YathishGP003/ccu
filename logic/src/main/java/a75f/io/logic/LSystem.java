package a75f.io.logic;

import android.util.Log;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;

import org.javolution.io.Struct;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.bo.building.Floor;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;

import static a75f.io.logic.L.ccu;

/**
 * Created by samjithsadasivan on 6/4/18.
 */

public class LSystem
{
    
    private static String connString = "HostName=RenatusIOTHub.azure-devices.net;DeviceId=MyAndroidDevice;SharedAccessKey=R0XZQPtgu8J4ayDc3hGLw5YQtxpOVmDn0kFxG0Xwf9k=";
    private static String deviceId = "MyAndroidDevice";
    
    public static int msgCntr = 0;
    public static HashMap<String, String> msgStr = null;
    public static String measurementTag = "VAV";
    public static long startTimeInMillis = System.currentTimeMillis();
    //public static JSONObject msgStr = null;
    
    public static Struct getSystemControlMsg() {
        if (ccu().systemProfile == null)
        {
            Log.d("VAV", "handleSystemControl : SystemProfile not configured");
           return null;
        }
        ccu().systemProfile.doSystemControl();
        msgStr = new HashMap<>();
        
        collectTSData();
        
        new Thread()
        {
            @Override
            public void run()
            {
                super.run();
                sendTSDataToInfluxDB();
                msgCntr++;
            }
        }.start();
        return ccu().systemProfile.getSystemControlMsg();
    }
    
    public static void collectTSData()
    {
        //addParamToMsg("deviceId", deviceId);
        addParamToMsg("messageId", String.valueOf(msgCntr));
    
        for (Floor floor : ccu().getFloors())
        {
            for (Zone zone : floor.mRoomList)
            {
                for (ZoneProfile zp : zone.mZoneProfiles)
                {
                    HashMap<String, Double> tsdata = zp.getTSData();
                    if (tsdata != null) {
                        for (Map.Entry<String, Double> entry : tsdata.entrySet()) {
                            addParamToMsg(entry.getKey(),String.valueOf(entry.getValue()));
                        }
                    }
                    
                }
            }
        }
        if (L.ccu().systemProfile.trSystem instanceof VavTRSystem)
        {
            VavTRSystem p = (VavTRSystem) ccu().systemProfile.trSystem;
            addParamToMsg("SAT", String.valueOf(p.getCurrentSAT()));
            addParamToMsg("CO2", String.valueOf(p.getCurrentCO2()));
            addParamToMsg("SP", String.valueOf(p.getCurrentSp()));
        }
    }
    
    public static void addParamToMsg(String key, String val){
            msgStr.put(key, val);
    }
    
    public static void sendTSDataToInfluxDB() {
    
        String url = new InfluxDbUtil.URLBuilder().setProtocol(InfluxDbUtil.HTTPS)
                                                  .setHost("influx-a75f.aivencloud.com")
                                                  .setPort(27304)
                                                  .setOp(InfluxDbUtil.WRITE)
                                                  .setDatabse("defaultdb")
                                                  .setUser("avnadmin")
                                                  .setPassword("mhur2n42y4l58xlx")
                                                  .buildUrl();
        
        if (L.app().getResources().getInteger(R.integer.heartbeat) != 60) {
            //############# Testing  ##########################
            
            if (msgCntr < 60)
            {
                measurementTag = "VAVTest";
                long testStartTime = SimulationTestHelper.getVavTestStartTime();
                long testTime;
                if (testStartTime == 0) {
                    testTime = System.currentTimeMillis();
                } else if (testStartTime == 100) {
                    testTime = startTimeInMillis + msgCntr * 60000;
                } else {
                    testTime = testStartTime + msgCntr * 60000;
                }
                
                InfluxDbUtil.writeData(url, measurementTag, msgStr, testTime);
            } else {
                Log.d("VAV"," Test time exceeded , not uploading data :msgCntr:"+msgCntr);
            }
            
        } else {
            InfluxDbUtil.writeData(url,measurementTag, msgStr, System.currentTimeMillis());
        }
        
        
    }
    
    
    public static void SendTSDataToAzure() throws URISyntaxException, IOException
    {
        // Comment/uncomment from lines below to use HTTPS or MQTT protocol
        // IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
        
        DeviceClient client = new DeviceClient(connString, protocol);
        
        try
        {
            client.open();
        } catch (Exception e2)
        {
            Log.d("VAV","Exception while opening IoTHub connection: " + e2.toString());
        }
    
    
        if (L.ccu().systemProfile.trSystem instanceof VavTRSystem)
        {
            Log.d("VAV"," Send TS Data : "+msgStr);
            try
            {
                Message msg = new Message(msgStr.toString());
                //msg.setProperty("temperatureAlert", temperature > 28 ? "true" : "false");
                msg.setMessageId(java.util.UUID.randomUUID().toString());
                System.out.println(msgStr);
                EventCallback eventCallback = new EventCallback();
                client.sendEventAsync(msg, eventCallback, msgCntr);
            }
            catch (Exception e)
            {
               Log.d("VAV","Exception while sending event: " + e.getMessage());
            }
            
        }
        try
        {
            Thread.sleep(2000);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        client.closeNow();
    }
    
    static class EventCallback implements IotHubEventCallback
    {
        public void execute(IotHubStatusCode status, Object context)
        {
            Integer i = (Integer) context;
            Log.d("VAV","IoT Hub responded to message " + i.toString()
                               + " with status " + status.name());
        }
    }
    
}
