package a75f.io.alerts;

import android.content.Context;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.Alert_;
import a75f.io.api.haystack.MyObjectBox;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.DebugFlags;
import io.objectbox.query.QueryBuilder;
/**
 * Created by samjithsadasivan on 4/24/18.
 */

/**
 * Core processing module that iterates through all the alert definitions , evaluates the conditional
 * and generates a new alert if needed.
 */
public class AlertProcessor
{
    
    ArrayList<Alert> alertList;
    ArrayList<Alert> activeAlertList;
    ArrayList<AlertDefinition> predefinedAlerts;
    
    AlertParser parser;
    
    Context mContext;
    private BoxStore     boxStore;
    private Box<Alert> alertBox;
    
    AlertProcessor instance = null;
    
    private static final File TEST_DIRECTORY = new File("objectbox-test/alert-db");
    
    private static final String PREFS_ALERT_DEFS = "ccu_alerts";
    private static final String PREFS_ALERTS_CUSTOM = "custom_alerts";
    
    AlertProcessor(Context c) {
        mContext = c;
    
        if(boxStore != null && !boxStore.isClosed())
        {
            boxStore.close();
        }
        boxStore = MyObjectBox.builder().androidContext(c).build();
        alertBox = boxStore.boxFor(Alert.class);
        alertList = new ArrayList<>();
        activeAlertList = new ArrayList<>();
        predefinedAlerts = parser.parseAllAlerts(c);
        parser = new AlertParser();
    }
    
    AlertProcessor(String alertDef) {
        if (boxStore == null)
        {
            BoxStore.deleteAllFiles(TEST_DIRECTORY);
            boxStore = MyObjectBox.builder()
                                  // add directory flag to change where ObjectBox puts its database files
                                  .directory(TEST_DIRECTORY)
                                  // optional: add debug flags for more detailed ObjectBox log output
                                  .debugFlags(DebugFlags.LOG_QUERIES | DebugFlags.LOG_QUERY_PARAMETERS).build();
            alertBox = boxStore.boxFor(Alert.class);
        }
        alertList = new ArrayList<>();
        activeAlertList = new ArrayList<>();
        parser = new AlertParser();
        predefinedAlerts = parser.parseAlertsString(alertDef);
    }
    
    public void runProcess() {
        for (AlertDefinition def : getAlertDefinitions()) {
        
            if (def.evaluate()) {
                if (Integer.parseInt(def.offset.trim()) == 0) {
                    addAlert(AlertBuilder.build(def));
                }else if (Integer.parseInt(def.offset.trim()) <= ++def.offsetCount ) {
                    def.offsetCount = 0;
                    addAlert(AlertBuilder.build(def));
                }
            } else {
                def.offsetCount = 0;
                //fixAlert(def.alert.getAlertType());
            }
        }
    }
    
    public void runProcess(Map<String,Object> tsData) {
        
        /*for (AlertDefinition def : predefinedAlerts) {
            
            if (def.evaluate(tsData)) {
                if (Integer.parseInt(def.offset.trim()) == 0) {
                    addAlert(AlertBuilder.build(def));
                }else if (Integer.parseInt(def.offset.trim()) <= ++def.offsetCount ) {
                    def.offsetCount = 0;
                    addAlert(AlertBuilder.build(def));
                }
            } else {
                def.offsetCount = 0;
                fixAlert(def.alert.getAlertType());
            }
        }*/
    
    }
    
    public ArrayList<AlertDefinition> getAlertDefinitions(){
        ArrayList<AlertDefinition> definedAlerts = new ArrayList<>();
        definedAlerts.addAll(predefinedAlerts);
        definedAlerts.addAll(getCustomAlertDefinitions());
        return definedAlerts;
    }
    
    public List<AlertDefinition> getCustomAlertDefinitions() {
        String alerts = mContext.getSharedPreferences(PREFS_ALERT_DEFS, Context.MODE_PRIVATE).getString(PREFS_ALERTS_CUSTOM, null);
        return parser.parseAlertsString(alerts);
    }
    
    public void updateCustomAlertDefinitions(List<AlertDefinition> aList) {
        List<AlertDefinition> customAlerts = getCustomAlertDefinitions();
        customAlerts.addAll(aList);
        
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
            saveCustomAlertDefinitions(jsonString);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void saveCustomAlertDefinitions(String alerts) {
        mContext.getSharedPreferences(PREFS_ALERT_DEFS, Context.MODE_PRIVATE).edit().putString(PREFS_ALERTS_CUSTOM, alerts).apply();
    }
    
    public void addAlertDefinition(AlertDefinition d) {
        //alertDefBox.put(d);
    }
    
    public List<Alert> getActiveAlerts(){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.equal(Alert_.isFixed, false)
                .orderDesc(Alert_.startTime);
    
        return alertQuery.build().find();
    }
    
    public List<Alert> getAllAlerts(){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.orderDesc(Alert_.startTime);
    
        return alertQuery.build().find();
    }
    
    public List<Alert> getAllAlerts(String type){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.equal(Alert_.mAlertType, type)
                  .orderDesc(Alert_.startTime);
        
        return alertQuery.build().find();
    }
    
    public void addAlert(Alert alert) {
        alert.setStartTime((new DateTime()).getMillis());
        alertBox.put(alert);
    }
    
    public void fixAlert(Alert alert) {
        alert.setEndTime((new DateTime()).getMillis());
        alert.setFixed(true);
        alertBox.put(alert);
    }
    
    /*public void updateAlertDefinitions(Context c) {
        alertDefinitions = parser.parseAllAlerts(c);
    }
    
    public void updateAlertDefinitions(String alerts) {
        if (alertDefinitions.size() == 0)
        {
            alertDefinitions = parser.parseAlertsString(alerts);
        } else {
            for (AlertDefinition def :parser.parseAlertsString(alerts)) {
                alertDefinitions.add(def);
            }
        }
    }
    
    public void updateAlertDefinitions(AlertDefinition d) {
        alertDefinitions.add(d);
    }*/
    
    public void clearAlerts() {
        alertList.clear();
        activeAlertList.clear();
    }
}
