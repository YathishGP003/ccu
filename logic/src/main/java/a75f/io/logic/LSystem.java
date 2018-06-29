package a75f.io.logic;

import android.util.Log;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.VAVSystemProfile;
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
    
    //private static String connString = "HostName=RenatusIOTHubTrial.azure-devices.net;DeviceId=AndroidCCU;SharedAccessKey=tYDsbTjcCuzQOnqcSiDw+2m71vbViOapCJ8K/pPQ54o=";
    //private static String deviceId = "AndroidCCU";
    
    public static int msgCntr = 0;
    public static JSONObject msgStr = null;
    
    public static void handleSystemControl() {
        Log.d("VAV", "handleSystemControl");
        ccu().systemProfile.doSystemControl();
        msgStr = new JSONObject();
        
        collectTSData();
    
        
        new Thread()
        {
            @Override
            public void run()
            {
                super.run();
                try {
                    SendTSDataMessage();
                } catch (URISyntaxException|IOException e)
                {
                    Log.d("VAV","Exception while opening IoTHub connection: " + e.toString());
                }
            }
        }.start();
    }
    
    public static void collectTSData()
    {
        addParamToMsg("deviceId", deviceId);
        addParamToMsg("messageId", String.valueOf(++msgCntr));
    
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
        if (L.ccu().systemProfile instanceof VAVSystemProfile)
        {
            VAVSystemProfile p = (VAVSystemProfile) ccu().systemProfile;
            addParamToMsg("SAT", String.valueOf(p.getCurrentSAT()));
        }
    }
    
    public static void addParamToMsg(String key, String val){
        try
        {
            msgStr.put(key, val);
        
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    public static void SendTSDataMessage() throws URISyntaxException, IOException
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
    
    
        if (L.ccu().systemProfile instanceof VAVSystemProfile)
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
