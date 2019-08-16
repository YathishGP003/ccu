package a75f.io.logic.pubnub;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;

import a75f.io.alerts.AlertDefinition;
import a75f.io.alerts.AlertManager;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import info.guardianproject.netcipher.NetCipher;

public class AlertDefinitionHandler
{
    public static final String CMD = "alertDefinition";
    
    public static void handleMessage(JsonObject msgObject)
    {
        String alertGUID = msgObject.get("alert_def_id").getAsString();
        CcuLog.d("CCU_PUBNUB"," alertGUID "+alertGUID);
        String alertDef = getAlertJson(alertGUID);
        CcuLog.d("CCU_PUBNUB"," alertDef "+alertDef);
        ArrayList<AlertDefinition> alertList = null;
        try
        {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            AlertDefinition[] pojos = objectMapper.readValue(alertDef, AlertDefinition[].class);
            alertList = new ArrayList<>(Arrays.asList(pojos));
            
            for(AlertDefinition d : alertList) {
                CcuLog.d("CCU_PUBNUB","alertDef Parsed "+d.toString());
            }
            if (alertList.size() > 0)
            {
                AlertManager.getInstance(Globals.getInstance().getApplicationContext()).addAlertDefinition(alertList);
            }
        }
        catch (IOException e)
        {
            CcuLog.d("CCU_PUBNUB","alertDef Parse Failed "+e.getMessage());
            e.printStackTrace();
        }
        
    }
    
    public static String getAlertJson(String id) {
        URL url;
        HttpsURLConnection connection = null;
        try {
            //Create connection
            url = new URL("https://ssap75f.azurewebsites.net/alerts/readDef");
            //connection = (HttpsURLConnection)url.openConnection();
            connection = NetCipher.getHttpsURLConnection(url);//TODO - Hack for SSLException
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/json");
    
            JSONObject postData = new JSONObject();
            postData.put("_id", id);
            
        
            CcuLog.i("CCU_PUBNUB",url.toString()+" "+postData.toString());
            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
        
            //Send request
            DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
            wr.writeBytes (postData.toString());
            wr.flush ();
            wr.close ();
        
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                CcuLog.i("CCU_HS","HttpError: responseCode "+responseCode);
            }
            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\n');
            }
            rd.close();
        
            return responseCode == 200 ? response.toString() : null;
        
        } catch (Exception e) {
        
            e.printStackTrace();
            return null;
        
        } finally {
        
            if(connection != null) {
                connection.disconnect();
            }
        }
    }
}
