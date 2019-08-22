package a75f.io.alerts;

import android.content.Context;

import com.google.gson.Gson;

import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.Alert_;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.MyObjectBox;
import a75f.io.logger.CcuLog;
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
    
    ArrayList<AlertDefinition> customAlerts = new ArrayList<>();
    AlertParser parser;
    
    Context mContext;
    private BoxStore     boxStore;
    private Box<Alert> alertBox;
    
    AlertProcessor instance = null;
    
    private static final File TEST_DIRECTORY = new File("objectbox-test/alert-db");
    
    private static final String PREFS_ALERT_DEFS = "ccu_alerts";
    private static final String PREFS_ALERTS_CUSTOM = "custom_alerts";
    
    HashMap<String, Integer> offsetCounter = new HashMap<>();
    HashSet<String> activeAlertRefs ;
    
    AlertProcessor(Context c) {
        mContext = c;
    
        if(boxStore != null && !boxStore.isClosed())
        {
            boxStore.close();
        }
        boxStore = CCUHsApi.getInstance().tagsDb.getBoxStore();//MyObjectBox.builder().androidContext(c).build();
        alertBox = boxStore.boxFor(Alert.class);
        alertList = new ArrayList<>();
        activeAlertList = new ArrayList<>();
        parser = new AlertParser();
        predefinedAlerts = parser.parseAllAlerts(c);
    }
    
    //For Unit testing
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
    
    public void processAlerts() {
        CcuLog.d("CCU_ALERTS", "processAlerts ");
        activeAlertRefs = new HashSet<>();
        for (AlertDefinition def : getAlertDefinitions()) {
            if (!def.alert.ismEnabled()) {
                continue;
            }
            
            if (isZoneAlert(def)) {
                CcuLog.d("CCU_ALERTS", "processAlerts: Check equips for  "+def.toString());
                ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("zone and equip");
                for (HashMap eq : equips) {
                    if (def.evaluate(eq.get("id").toString())) {
                        activeAlertRefs.add(def.alert.mTitle+eq.get("id"));
                        if (alertActive(def.alert,eq.get("id").toString())){
                            continue;
                        }
                        if (Integer.parseInt(def.offset) > 0) {
                            int offset = 0;
                            if (offsetCounter.get(def.alert.mTitle+eq.get("id")) != null)
                            {
                                offset = offsetCounter.get(def.alert.mTitle + eq.get("id"));
                            }
                            
                            if (offset++ >= Integer.parseInt(def.offset)) {
                                addAlert(AlertBuilder.build(def, AlertFormatter.getFormattedMessage(def),eq.get("id").toString()));
                                CcuLog.d("CCU_ALERTS", "Equip "+eq.get("dis")+" addAlert "+def.toString());
                            } else
                            {
                                CcuLog.d("CCU_ALERTS", "Equip "+eq.get("dis") + " offset " +offset);
                            }
                            offsetCounter.put(def.alert.mTitle + eq.get("id"), offset);
                        } else {
                            addAlert(AlertBuilder.build(def, AlertFormatter.getFormattedMessage(def), eq.get("id").toString()));
                        }
                        
                    }
                }
            } else {
                CcuLog.d("CCU_ALERTS", "processAlerts: Find points for "+def.toString());
                def.evaluate();
                ArrayList<String> pointList = null;
                for (int i = 0; i < def.conditionals.size(); i+=2) {
                    if (i == 0) {
                        pointList = def.conditionals.get(0).trueList;
                        continue;
                    }
                    if (def.conditionals.get(i-1).operator.contains("&&")) {
                        pointList.retainAll(def.conditionals.get(i).trueList);
                    } else if (def.conditionals.get(i-1).operator.contains("||")) {
                        pointList.addAll(def.conditionals.get(i).trueList);
                    }
                }
                for(String point : pointList) {
                    if (!alertActive(def.alert, point))
                    {
                        HashMap p = CCUHsApi.getInstance().readMapById(point);
                        if (Integer.parseInt(def.offset) > 0) {
                            int offset = 0;
                            if (offsetCounter.get(def.alert.mTitle+p.get("id")) != null)
                            {
                                offset = offsetCounter.get(def.alert.mTitle + p.get("id"));
                            }
                            if (offset++ >= Integer.parseInt(def.offset)) {
                                addAlert(AlertBuilder.build(def, AlertFormatter.getFormattedMessage(def), point));
                                CcuLog.d("CCU_ALERTS", "Point " + p.get("dis") + " addAlert " + def.toString());
                            } else
                            {
                                CcuLog.d("CCU_ALERTS", "Point "+p.get("dis") + " offset " +offset);
                            }
                        } else {
                            addAlert(AlertBuilder.build(def, AlertFormatter.getFormattedMessage(def), point));
                        }
                    }
                    activeAlertRefs.add(def.alert.mTitle+point);
                }
            }
        }
        
        //Fix alerts which are no more active
        for (Alert  a : getActiveAlerts()) {
            if (!activeAlertRefs.contains(a.mTitle+a.ref)) {
                fixAlert(a);
            }
        }
        
        for (Alert a : getActiveUnSyncedAlerts()) {
            CcuLog.d("CCU_ALERTS"," Active Alert "+a.toString());
        }
    }
    
    public boolean isZoneAlert(AlertDefinition ad) {
        for (Conditional d : ad.conditionals) {
            if (d.operator != null ) continue;
            if (d.key.contains("zone")) {
                return true;
            }
        }
        return false;
    }
    
    public boolean alertActive(Alert a, String ref) {
        for (Alert b : getActiveAlerts()) {
            if (b.mMessage.equals(a.mMessage) && b.ref.equals(ref)) {
                return true;
            }
        }
        return false;
    }
    
    public ArrayList<AlertDefinition> getAlertDefinitions(){
        ArrayList<AlertDefinition> definedAlerts = new ArrayList<>();
        definedAlerts.addAll(predefinedAlerts);
        definedAlerts.addAll(getCustomAlertDefinitions());
        return definedAlerts;
    }
    
    public List<AlertDefinition> getCustomAlertDefinitions() {
        if (customAlerts.size() == 0)
        {
            String alerts = mContext.getSharedPreferences(PREFS_ALERT_DEFS, Context.MODE_PRIVATE).getString(PREFS_ALERTS_CUSTOM, null);
            if (alerts != null) {
                customAlerts = parser.parseAlertsString(alerts);
            }
        }
        return customAlerts;
    }
    
    public void updateCustomAlertDefinitions(List<AlertDefinition> aList) {
        Iterator iterator = customAlerts.iterator();
        for (AlertDefinition d : aList) {
            while(iterator.hasNext()) {
                AlertDefinition a = (AlertDefinition) iterator.next();
                if (a.alert.mTitle.equals(d.alert.mTitle)) {
                    iterator.remove();
                }
            }
        }
        customAlerts.addAll(aList);
        saveCustomAlertDefinitions(new Gson().toJson(customAlerts));
    }
    
    public void saveCustomAlertDefinitions(String alerts) {
        CcuLog.d("CCU_ALERTS", "Save Custom Alerts "+alerts);
        mContext.getSharedPreferences(PREFS_ALERT_DEFS, Context.MODE_PRIVATE).edit().putString(PREFS_ALERTS_CUSTOM, alerts).apply();
    }
    
    public List<Alert> getActiveAlerts(){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.equal(Alert_.isFixed, false)
                .orderDesc(Alert_.startTime);
    
        return alertQuery.build().find();
    }
    
    public List<Alert> getActiveUnSyncedAlerts(){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.equal(Alert_.isFixed, false)
                  .equal(Alert_.alertId,"")
                  .orderDesc(Alert_.startTime);
        
        return alertQuery.build().find();
    }
    
    
    public List<Alert> getAllAlerts(){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.orderDesc(Alert_.startTime);
    
        return alertQuery.build().find();
    }
    
    public List<Alert> getAllAlerts(String message){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.equal(Alert_.mMessage, message)
                  .orderDesc(Alert_.startTime);
        
        return alertQuery.build().find();
    }
    
    public void addAlert(Alert alert) {
        //Need to handle equip level alerts
        for (Alert a : getActiveAlerts()) {
            if (a.mTitle.equals(alert.mTitle)) {
                return;
            }
        }
        alertBox.put(alert);
    }
    
    public void updateAlert(Alert alert) {
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
    
    public void addAlertDefinition(List<AlertDefinition> list) {
        updateCustomAlertDefinitions(list);
    }
    public void addAlertDefinition(AlertDefinition alert) {
        updateCustomAlertDefinitions(Arrays.asList(alert));
    }
    
    public void clearAlerts() {
        alertList.clear();
        activeAlertList.clear();
    }
}
