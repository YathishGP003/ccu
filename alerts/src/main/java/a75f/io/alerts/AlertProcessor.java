package a75f.io.alerts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.projecthaystack.HRef;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    
    ArrayList<AlertDefinition> predefinedAlerts;
    
    ArrayList<AlertDefinition> customAlerts = new ArrayList<>();
    AlertParser parser;
    
    Context mContext;
    private BoxStore     boxStore;
    private Box<Alert> alertBox;
    
    private static final File TEST_DIRECTORY = new File("objectbox-test/alert-db");
    //private static final File ALERT_DIRECTORY = new File("objectbox-alert/alert-db");
    private static final String PREFS_ALERT_DEFS = "ccu_alerts";
    private static final String PREFS_ALERTS_CUSTOM = "custom_alerts";
    private static final String PREFS_ALERTS_PREDEFINED = "predef_alerts";

    HashMap<String, Integer> offsetCounter = new HashMap<>();
    HashSet<String> activeAlertRefs ;
    private ScheduledExecutorService taskExecutor;
    
    AlertProcessor(Context c) {
        mContext = c;
        taskExecutor = Executors.newSingleThreadScheduledExecutor();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        if(boxStore != null && !boxStore.isClosed())
        {
            boxStore.close();
        }
        boxStore = CCUHsApi.getInstance().tagsDb.getBoxStore();//MyObjectBox.builder().androidContext(c).build();
        alertBox = boxStore.boxFor(Alert.class);
        parser = new AlertParser();
        predefinedAlerts = getPredefinedAlerts();
        taskExecutor.scheduleAtFixedRate(getWifiStatusRunnable(), 900, 60, TimeUnit.SECONDS );
        fetchAllPredefinedAlerts();
    }

    private Runnable getWifiStatusRunnable()
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                predefinedAlerts.add(parser.parseWifiAlerts(mContext).get(0));
            }
        };
    }

    private void clearElapsedAlerts()
    {
        ArrayList<Alert> alertList = new ArrayList<>(getAllAlerts());
        for (Alert a:alertList){
            //clear alerts after 24 hours
            if ((System.currentTimeMillis() - a.getStartTime()) >= 86400000){
                alertBox.remove(a.id);
            }
        }

        ArrayList<Alert> cmErrorAlertList = new ArrayList<>(getCmErrorAlerts());
        for (Alert a:cmErrorAlertList){
            //clear alerts after every hours
            if ((System.currentTimeMillis() - a.getStartTime()) >= 3600000){
                alertBox.remove(a.id);
            }
        }
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
        parser = new AlertParser();
        fetchPredefinedAlerts();
        
    }
    
    public ArrayList<AlertDefinition> getPredefinedAlerts() {
        String alerts = mContext.getSharedPreferences(PREFS_ALERT_DEFS, Context.MODE_PRIVATE).getString(PREFS_ALERTS_PREDEFINED, null);
        return alerts != null ? parser.parseAlertsString(alerts) : null;
    }
    
    public void fetchPredefinedAlerts() {
        try
        {
            HashMap site = CCUHsApi.getInstance().read("site");
            if (site == null || site.get("id") == null){
                return;
            }

            if (BuildConfig.DEBUG)
            {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
            }
            String siteGUID = CCUHsApi.getInstance().getGUID(site.get("id").toString());
            String alertDef = HttpUtil.sendRequest(mContext, "readPredefined", new JSONObject().put("siteRef", siteGUID.replace("@","")).toString());
            CcuLog.d("CCU_ALERTS", " alertDef " + alertDef);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            if ( alertDef != null) {
                AlertDefinition[] pojos = objectMapper.readValue(alertDef, AlertDefinition[].class);
                predefinedAlerts = new ArrayList<>(Arrays.asList(pojos));
            }
            if (predefinedAlerts!= null  && predefinedAlerts.size()>0)
            for (AlertDefinition d : predefinedAlerts)
            {
                CcuLog.d("CCU_ALERTS", "Predefined alertDef Fetched: " + d.toString());
            }
            savePredefinedAlertDefinitions(alertDef);
        }catch (JsonParseException | IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    public void savePredefinedAlertDefinitions(String alerts) {
        CcuLog.d("CCU_ALERTS", "Save Predefined Alerts "+alerts);
        mContext.getSharedPreferences(PREFS_ALERT_DEFS, Context.MODE_PRIVATE).edit().putString(PREFS_ALERTS_PREDEFINED, alerts).apply();
    }
    
    public void processAlerts() {
        
        CcuLog.d("CCU_ALERTS", "processAlerts ");
        /*if (boxStore != null && boxStore.isClosed()){
            boxStore = CCUHsApi.getInstance().tagsDb.getBoxStore();
            alertBox = boxStore.boxFor(Alert.class);
        }*/

        activeAlertRefs = new HashSet<>();
        for (AlertDefinition def : getAlertDefinitions())
        {
            if (!def.alert.ismEnabled())
            {
                continue;
            }
            CcuLog.d("CCU_ALERTS", def.toString());
            def.evaluate();
            ArrayList<String> pointList = null;
            boolean alertStatus = false;
            boolean statusInit = false;
            for (int i = 0; i < def.conditionals.size(); i+=2) {
                if (i == 0) {
                    if ((def.conditionals.get(0).grpOperation != null) && (def.conditionals.get(0).grpOperation.equals("equip")
                                                                                    || def.conditionals.get(0).grpOperation.equals("delta")))
                    {
                        pointList = def.conditionals.get(0).pointList;
                    }else if((def.conditionals.get(0).grpOperation != null) && (def.conditionals.get(0).grpOperation.equals("alert"))){
                        List<Alert> aList = getActiveAlerts();
                        for (Alert a:aList){
                            if (a.mTitle.equals(def.alert.mTitle)){
                                def.conditionals.get(0).status = true;
                                alertStatus = true;
                            }
                        }
                    } else {
                        statusInit = true;
                        alertStatus = def.conditionals.get(0).status;
                    }
                    continue;
                }
                
                if (def.conditionals.get(i-1).operator.contains("&&")) {
                    if (def.conditionals.get(i).grpOperation != null && ( def.conditionals.get(i).grpOperation.equals("equip")
                                                                          || def.conditionals.get(0).grpOperation.equals("delta")))
                    {
                        if (pointList == null) {
                            if (alertStatus)
                            {
                                pointList = def.conditionals.get(i).pointList;
                            }
                        } else {
                            if (def.conditionals.get(i).pointList != null)
                            {
                                pointList.retainAll(def.conditionals.get(i).pointList);
                            }
                        }
                        
                    } else {
                        if (statusInit) {
                            alertStatus = alertStatus && def.conditionals.get(i).status;
                        } else {
                            statusInit = true;
                            alertStatus = def.conditionals.get(i).status;
                        }
                        
                    }
                } else if (def.conditionals.get(i-1).operator.contains("||")) {
                    
                    if ((def.conditionals.get(i).grpOperation != null) && (def.conditionals.get(i).grpOperation.equals("equip")
                                                                            || def.conditionals.get(0).grpOperation.equals("delta")))
                    {
                        if (def.conditionals.get(i).pointList != null)
                        {
                            pointList.addAll(def.conditionals.get(i).pointList);
                        }
        
                    } else {
                        alertStatus = alertStatus || def.conditionals.get(i).status;
                    }
                }
            }
            if (pointList != null) {
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
                                CcuLog.d("CCU_ALERTS", "Point " + p.get("dis") + " addAlert " + def.toString());
                                addAlert(AlertBuilder.build(def, AlertFormatter.getFormattedMessage(def, point), point));
                                offset = 0;
                            } else
                            {
                                CcuLog.d("CCU_ALERTS", "Point "+p.get("dis") + " offset " +offset);
                            }
                            offsetCounter.put(def.alert.mTitle + p.get("id"), offset);
                        } else {
                            CcuLog.d("CCU_ALERTS", "Point " + p.get("dis") + " addAlert " + def.toString());
                            addAlert(AlertBuilder.build(def, AlertFormatter.getFormattedMessage(def, point), point));
                        }
                    }
                    activeAlertRefs.add(def.alert.mTitle+point);
                }
            } else if (alertStatus) {
                activeAlertRefs.add(def.alert.mTitle);
                if (alertActive(def.alert)){
                    continue;
                }
                
                if (def.offset != null && Integer.parseInt(def.offset) > 0) {
                    int offset = 0;
                    if (offsetCounter.get(def.alert.mTitle) != null)
                    {
                        offset = offsetCounter.get(def.alert.mTitle);
                    }
        
                    if (offset++ >= Integer.parseInt(def.offset)) {
                        addAlert(AlertBuilder.build(def, AlertFormatter.getFormattedMessage(def)));
                        offset = 0;
                    } else
                    {
                        CcuLog.d("CCU_ALERTS", " Alert offset " +offset);
                    }
                    offsetCounter.put(def.alert.mTitle, offset);
                } else {
                    addAlert(AlertBuilder.build(def, AlertFormatter.getFormattedMessage(def)));
                }
            }
        }
        
        //Fix alerts which are no more active
        for (Alert  a : getActiveAlerts()) {
            if (!activeAlertRefs.contains(a.mTitle+ (a.ref != null ? a.ref :""))) {
                fixAlert(a);
            }
        }
        clearElapsedAlerts();
        
        for (Alert a : getActiveAlerts()) {
            CcuLog.d("CCU_ALERTS"," Active Alert "+a.toString());
        }
        List<Alert> unsyncedAlerts = getUnSyncedAlerts();
        CcuLog.d("CCU_ALERTS"," Unsynced Alerts "+unsyncedAlerts.size());
        if (unsyncedAlerts.size() > 0)
        {
            if (!CCUHsApi.getInstance().isCCURegistered()){
                return;
            }

            List<Alert> syncedAlerts = AlertSyncHandler.sync(mContext, unsyncedAlerts);
            for (Alert a : syncedAlerts) {
                updateAlert(a);
            }
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
    
    public boolean alertActive(Alert a, String pointId) {
        for (Alert b : getActiveAlerts()) {
            if (b.mTitle.equals(a.mTitle) && b.ref.equals(pointId)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean alertActive(Alert a) {
        for (Alert b : getActiveAlerts()) {
            if (b.mTitle.equals(a.mTitle)) {
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
        for (AlertDefinition a : customAlerts) {
            CcuLog.d("CCU_ALERTS"," add def "+a.toString());
        }
        saveCustomAlertDefinitions(new Gson().toJson(customAlerts));
    }
    
    public void deleteCustomAlertDefinition(String id) {
        Iterator iterator = customAlerts.iterator();
        while(iterator.hasNext()) {
            AlertDefinition a = (AlertDefinition) iterator.next();
            if (a._id.equals(id)) {
                iterator.remove();
            }
        }
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

    public List<Alert> getActiveCMDeadAlerts(){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.equal(Alert_.isFixed, false)
                .equal(Alert_.mTitle,"CM DEAD")
                .orderDesc(Alert_.startTime);

        return alertQuery.build().find();
    }

    public List<Alert> getActiveDeviceDeadAlerts(){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.equal(Alert_.isFixed, false)
                .equal(Alert_.mTitle,"DEVICE DEAD")
                .orderDesc(Alert_.startTime);

        return alertQuery.build().find();
    }
    
    public List<Alert> getActiveUnSyncedAlerts(){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.equal(Alert_.isFixed, false)
                  .equal(Alert_.syncStatus,false)
                  .orderDesc(Alert_.startTime);
        
        return alertQuery.build().find();
    }
    
    public List<Alert> getUnSyncedAlerts(){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.equal(Alert_.syncStatus,false)
                  .orderDesc(Alert_.startTime);
        
        return alertQuery.build().find();
    }
    
    public List<Alert> getAllAlerts(){
        /*if (boxStore != null && boxStore.isClosed()){
            return new ArrayList<Alert>();
        }*/
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_INFO.ordinal());
        alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_LOW.ordinal());
        alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_MODERATE.ordinal());
        alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_SEVERE.ordinal());
        alertQuery.orderDesc(Alert_.startTime);
        return alertQuery.build().find();
    }

    public List<Alert> getCmErrorAlerts(){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.contains(Alert_.mTitle,"CM ERROR REPORT")
                  .orderDesc(Alert_.startTime);
        return alertQuery.build().find();
    }
    
    public Alert getAlert(String id) {
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.equal(Alert_._id, id);
        return alertQuery.build().findFirst();
    }
    
    public List<Alert> getAllAlerts(String message){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.equal(Alert_.mMessage, message)
                  .orderDesc(Alert_.startTime);
        
        return alertQuery.build().find();
    }
    
    public void addAlert(Alert alert) {
        for (Alert a : getActiveAlerts()) {
            if (a.mTitle.equals(alert.mTitle) && (a.ref != null && a.ref.equals(alert.ref)) && ((a.mMessage != null) && a.mMessage.equals(alert.mMessage))) {
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
        alert.setSyncStatus(false);
        alertBox.put(alert);

        if (alert.mTitle.equalsIgnoreCase("CCU RESTART")){
            SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(AlertManager.getInstance().getApplicationContext());
            spDefaultPrefs.edit().putBoolean("APP_RESTART", false).apply();
        }
    }
    
    public void deleteAlert(Alert alert) {
        //_id is empty if the alert is not synced to backend.
        if (alert._id.equals("") || AlertSyncHandler.delete(mContext, alert._id))
        {
            alertBox.remove(alert.id);
            if (AlertSyncHandler.mListener != null){
                AlertSyncHandler.mListener.onDeleteSuccess();
            }
        }
    }
    
    public void deleteAlert(String id) {
        Alert a = getAlert(id);
        if (a != null)
        {
            alertBox.remove(a.id);
        }
    }

    public void generateAlert(String title, String msg){
        ArrayList<AlertDefinition> alertDefinition = getAlertDefinitions();
        for (AlertDefinition ad :alertDefinition) {
            if(ad.alert.ismEnabled()) {
                if (ad.alert.mTitle.equals(title) && (!ad.alert.mMessage.equals(msg))) {
                    ad.alert.setmMessage(msg);
                    ad.alert.setmNotificationMsg(msg);
                    addAlert(AlertBuilder.build(ad, AlertFormatter.getFormattedMessage(ad)));
                }
            }
        }
    }

    public void generateCMDeadAlert(String title, String msg){
        ArrayList<AlertDefinition> alertDefinition = getAlertDefinitions();
        for (AlertDefinition ad :alertDefinition) {
            if(ad.alert.ismEnabled()) {
                if (ad.alert.mTitle.equals(title) && getActiveCMDeadAlerts().size() == 0) {
                    ad.alert.setmMessage(msg);
                    ad.alert.setmNotificationMsg(msg);
                    addAlert(AlertBuilder.build(ad, AlertFormatter.getFormattedMessage(ad)));
                }
            }
        }
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
        //alertList.clear();
        //activeAlertList.clear();
    }

    public void fetchAllPredefinedAlerts(){
        if (predefinedAlerts == null || predefinedAlerts.size() == 0) {
            fetchPredefinedAlerts();
        }
    }
}
