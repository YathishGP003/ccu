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

import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 4/23/18.
 *
 * This class has just two remaining functions, both of which could coneivably go away --
 *
 * 1) gets the wifi alert def from assets folder.
 * 2) A place holder for jackson deserialization.  (I'll also add serialization since we now need it).
 */
public class AlertParser
{

    ObjectMapper objectMapper;

    public AlertParser() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public ArrayList<AlertDefinition> parseAlertsString(String json) {
        
        ArrayList<AlertDefinition> alertList = null;
        try
        {
            AlertDefinition[] pojos = objectMapper.readValue(json, AlertDefinition[].class);
            alertList = new ArrayList<>(Arrays.asList(pojos));
            
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return alertList;
    }

    /**
     * Used to create String to write to prefs, using ObjectMapper.
     */
    public String alertDefsToString(ArrayList<AlertDefinition> alertDefs) {

        try {
            AlertDefinition[] alertDefsArray = alertDefs.toArray(new AlertDefinition[alertDefs.size()]);
            return objectMapper.writeValueAsString(alertDefsArray);
        }
        catch (IOException e) {
            CcuLog.i("CCU_ALERTS", "Error serialzing alert defs.", e);

            // We probably want the app to crash here.  Fail-fast.   (and fix)
            return null;
        }
    }
}
