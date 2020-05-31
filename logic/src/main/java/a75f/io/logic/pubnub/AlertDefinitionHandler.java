package a75f.io.logic.pubnub;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import a75f.io.alerts.AlertDefinition;
import a75f.io.alerts.AlertManager;
import a75f.io.alerts.HttpUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;

public class AlertDefinitionHandler
{
    public static final String CMD = "alertDefinition";
    
    public static void handleMessage(JsonObject msgObject)
    {
        String alertGUID = msgObject.get("alert_def_id").getAsString();
        ArrayList<AlertDefinition> alertList = null;
        try
        {
            JSONObject postData = new JSONObject();
            postData.put("_id", alertGUID);
    
            String alertDef = HttpUtil.sendRequest("readDef", postData.toString());
            CcuLog.d(L.TAG_CCU_PUBNUB," alertDef "+alertDef);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            AlertDefinition[] pojos = objectMapper.readValue(alertDef, AlertDefinition[].class);
            alertList = new ArrayList<>(Arrays.asList(pojos));
    
            Iterator iterator = alertList.iterator();
            while(iterator.hasNext())
            {
                AlertDefinition a = (AlertDefinition) iterator.next();
                if (a.validate() == false) {
                    CcuLog.d(L.TAG_CCU_PUBNUB, " Invalid Alert Definition "+a.toString());
                    iterator.remove();
                }
            }
            if (alertList.size() > 0)
            {
                AlertManager.getInstance(Globals.getInstance().getApplicationContext()).addAlertDefinition(alertList);
            }
        }
        catch (Exception e)
        {
            CcuLog.d(L.TAG_CCU_PUBNUB,"alertDef Parse Failed "+e.getMessage());
            e.printStackTrace();
        }
        
    }
}
