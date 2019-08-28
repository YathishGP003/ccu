package a75f.io.logic.pubnub;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import a75f.io.alerts.AlertDefinition;
import a75f.io.alerts.AlertManager;
import a75f.io.alerts.HttpUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;

public class AlertDefinitionHandler
{
    public static final String CMD = "alertDefinition";
    
    public static void handleMessage(JsonObject msgObject)
    {
        String alertGUID = msgObject.get("alert_def_id").getAsString();
        CcuLog.d("CCU_PUBNUB"," alertGUID "+alertGUID);
        
        ArrayList<AlertDefinition> alertList = null;
        try
        {
            JSONObject postData = new JSONObject();
            postData.put("_id", alertGUID);
    
            String alertDef = HttpUtil.sendRequest(Globals.getInstance().getApplicationContext(),
                                    "readDef", postData.toString());//getAlertJson(alertGUID);
            CcuLog.d("CCU_PUBNUB"," alertDef "+alertDef);
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
        catch (Exception e)
        {
            CcuLog.d("CCU_PUBNUB","alertDef Parse Failed "+e.getMessage());
            e.printStackTrace();
        }
        
    }
}
