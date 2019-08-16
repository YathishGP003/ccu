package a75f.io.alerts;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by samjithsadasivan on 4/23/18.
 */

public class AlertParser
{
    public final String SYSTEM_ALERTS_FILE = "SystemAlerts.json";
    public final String ZONE_ALERTS_FILE = "ZoneAlerts.json";
    public final String NODE_ALERTS_FILE = "NodeAlerts.json";
    
    public ArrayList<AlertDefinition> parseAllAlerts(Context c) {
        ArrayList<AlertDefinition> systemAlerts = parseSystemAlerts(c);
        ArrayList<AlertDefinition> zoneAlerts = parseZoneAlerts(c);
        ArrayList<AlertDefinition> nodeAlerts = parseNodeAlerts(c);
    
        ArrayList<AlertDefinition> allAlerts = new ArrayList<>();
        
        if (systemAlerts != null) {
            allAlerts.addAll(systemAlerts);
        }
        if (zoneAlerts != null) {
            allAlerts.addAll(zoneAlerts);
        }
        if (nodeAlerts != null) {
            allAlerts.addAll(nodeAlerts);
        }
        
        return allAlerts;
    }
    
    public ArrayList<AlertDefinition> parseSystemAlerts(Context c) {
        
        String alerts = readFileFromAssets(c, SYSTEM_ALERTS_FILE);
        return alerts != null ? parseAlertsString(alerts) : null;
    }
    
    public ArrayList<AlertDefinition> parseZoneAlerts(Context c) {
        
        String alerts = readFileFromAssets(c, ZONE_ALERTS_FILE);
        return alerts != null ? parseAlertsString(alerts) : null;
    }
    
    public ArrayList<AlertDefinition> parseNodeAlerts(Context c) {
        
        String alerts = readFileFromAssets(c, NODE_ALERTS_FILE);
        return alerts != null ? parseAlertsString(alerts) : null;
    }
    
    public ArrayList<AlertDefinition> parseAlertsString(String json) {
        
        ArrayList<AlertDefinition> alertList = null;
        try
        {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            AlertDefinition[] pojos = objectMapper.readValue(json, AlertDefinition[].class);
            alertList = new ArrayList<>(Arrays.asList(pojos));
            
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return alertList;
    }
    
    
    public String readFileFromAssets(Context ctx, String pathToJson){
        InputStream rawInput;
        ByteArrayOutputStream rawOutput = null;
        try {
            rawInput = ctx.getAssets().open(pathToJson);
            byte[] buffer = new byte[rawInput.available()];
            rawInput.read(buffer);
            rawOutput = new ByteArrayOutputStream();
            rawOutput.write(buffer);
            rawOutput.close();
            rawInput.close();
            ;
        } catch (IOException e) {
            Log.e("Error", e.toString());
        }
        return rawOutput != null ? rawOutput.toString() : null;
    }
}
