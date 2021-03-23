package a75f.io.alerts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import a75f.io.alerts.cloud.AlertsService;
import a75f.io.alerts.cloud.DefinitionsResponse;
import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.Alert_;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.QueryBuilder;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
/**
 * Created by samjithsadasivan on 4/24/18.
 */

/**
 * Core processing module that iterates through all the alert definitions , evaluates the conditional
 * and generates a new alert if needed.
 */
// This class communicates with the service, and with the ObjectBox DB (alerts) and Prefs DB (alert defs).
// So its managing the data between those AND executing our business logic for alerts and
// *processing* the alerts -- i.e. checking the conditional.  This does it all.  Not sure what function
// AlertsManager has --> I guess it is a facade on top of this class, providing access to outside world.

// this is really an application level class.  It's a member to AlertManager, which is instantiated in
//  Globals as a singleton.
public class AlertProcessor
{
    
    ArrayList<AlertDefinition> predefinedAlerts;
    ArrayList<AlertDefinition> customAlerts = new ArrayList<>();

    // Parses a String into a list of AlertDefinitions
    AlertParser parser;
    
    private Context mContext;
    private SharedPreferences defaultSharedPrefs;
    private SharedPreferences alertsSharedPrefs;
    private AlertsService alertsService;
    private AlertSyncHandler alertSyncHandler;
    private BoxStore     boxStore;
    private Box<Alert> alertBox;

    private static final String PREFS_ALERT_DEFS = "ccu_alerts";
    private static final String PREFS_ALERTS_CUSTOM = "custom_alerts";
    private static final String PREFS_ALERTS_PREDEFINED = "predef_alerts";

    // Tracks how many times an Alert appears, so it will only be released when it reaches offset minutes.
    // NOTE: I don't see any logic where these have to be consecutive minutes.  They can occur anywhere
    //   over the lifetime of the app, raising an alert.
    HashMap<String, Integer> offsetCounter = new HashMap<>();
    // Set of alerts raised in single processAlerts.  Should be local variable.
    HashSet<String> activeAlertRefs ;

    AlertProcessor(Context c, AlertsService alertsService) {
        mContext = c;
        this.alertsService = alertsService;
        this.defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        this.alertsSharedPrefs = mContext.getSharedPreferences(PREFS_ALERT_DEFS, Context.MODE_PRIVATE);
        // great candidate for DI when we have it:
        this.alertSyncHandler = new AlertSyncHandler(alertsService);

        CcuLog.d("CCU_ALERTS", "AlertProcessor Init");

        // what are we doing here with StrictMode policy?  Are we making this code OK to call from main thread?
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Get Alert table from ObjectBox DB.  (make sure boxStore is closed()?  Seems dangerous.)
        if(boxStore != null && !boxStore.isClosed())
        {
            boxStore.close();
        }
        boxStore = CCUHsApi.getInstance().tagsDb.getBoxStore();
        alertBox = boxStore.boxFor(Alert.class);

        // The parser to
        parser = new AlertParser();

        // Get predefined alerts from disk (sys prefs)
        predefinedAlerts = getPredefinedAlerts();
        // Fetches predefined alerts from service if there are none.
        CcuLog.d("CCU_ALERTS", "fetching alerts");

        fetchAllPredefinedAlerts();
    }

    // todo: is this still needed, or is this alert present on server?
    private void parseWifiSignalAlertDefinition()
    {
        AlertDefinition wifiSignalDefinition = parser.parseWifiAlerts(mContext).get(0);
        if (!predefinedAlerts.contains(wifiSignalDefinition)){
            predefinedAlerts.add(wifiSignalDefinition);
        }
    }

    /**
     * Called from processAlerts.
     * Clears all alerts older than 24 hours
     * Clears mcError alerts after 1 hour.
     */
    private void clearElapsedAlerts()
    {
        ArrayList<Alert> alertList = new ArrayList<>(getAllAlertsNotInternal());
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
    
    private ArrayList<AlertDefinition> getPredefinedAlerts() {
        String alerts = alertsSharedPrefs.getString(PREFS_ALERTS_PREDEFINED, "");
        return StringUtils.isNotBlank(alerts) ? parser.parseAlertsString(alerts) : new ArrayList<AlertDefinition>();
    }

    private Disposable fetchDisposable = null;
    /**
     * Fetches predefined Alert String from server and parses it into List of Alert Definitions/
     *
     * Called from here, and from AlertManager upon pubnub "predefinedAlertDefinition".
     */
    public void fetchPredefinedAlerts() {

        final HashMap<String, String> site = CCUHsApi.getInstance().read("site");
        if (site == null || site.get("id") == null) {
            return;
        }

        fetchDisposable =
            alertsService.getPredefinedDefinitions()
                .subscribeOn(Schedulers.io())
                .map(DefinitionsResponse::getData)
                .subscribe(
                        this::handleRetreivedAlerts,
                        error -> { CcuLog.e("CCU_ALERTS", "Unexpected error fetching or parsing site definitions.", error); }
                );
    }
    
    private void handleRetreivedAlerts(ArrayList<AlertDefinition> alertDefs) {

        this.predefinedAlerts = alertDefs;
        //add wifi signal alert definition
        parseWifiSignalAlertDefinition();

        if (predefinedAlerts != null  && predefinedAlerts.size() > 0)
            for (AlertDefinition d : predefinedAlerts) {
                CcuLog.d("CCU_ALERTS", "Predefined alertDef Fetched: " + d.toString());
            }
        //save predefined alert definitions to SharedPreferences
        savePredefinedAlertDefinitions();
    }

    private void savePredefinedAlertDefinitions() {
        String alerts = parser.alertDefsToString(this.predefinedAlerts);
        CcuLog.d("CCU_ALERTS", "Save Predefined Alerts "+alerts);
        this.alertsSharedPrefs.edit().putString(PREFS_ALERTS_PREDEFINED, alerts).apply();
    }

    /**
     * Long method (160 lines)
     * Called from AlertManager upon AlertProcessJob.doJob, every 60 sec.
     */
    public void processAlerts() {
        
        CcuLog.d("CCU_ALERTS", "processAlerts ");

        activeAlertRefs = new HashSet<>();
        // for all alert definitions..
        for (AlertDefinition def : getAlertDefinitions())
        {
            // check for enabled
            if (!def.alert.ismEnabled())
            {
                continue;
            }
            CcuLog.d("CCU_ALERTS", def.toString());

            // Evaluates each conditional of the alert condition that is not an operator.
            // Result is conditional's state is populated, especially "status: Bool" and "resVal: Double" but others like pointList.
            def.evaluate(defaultSharedPrefs);
            ArrayList<String> pointList = null;
            boolean alertStatus = false;
            boolean statusInit = false;
            // for each even numbered conditional (i.e. not an operator)
            for (int i = 0; i < def.conditionals.size(); i+=2) {
                // for first conditional..
                if (i == 0) {
                    // if its a grpOperation equal to "equip" or "delta", collect its pointList.
                    if ((def.conditionals.get(0).grpOperation != null) && (def.conditionals.get(0).grpOperation.equals("equip")
                                                                                    || def.conditionals.get(0).grpOperation.equals("delta")))
                    {
                        pointList = def.conditionals.get(0).pointList;
                    }else if((def.conditionals.get(0).grpOperation != null) && (def.conditionals.get(0).grpOperation.equals("alert"))){
                    // else if grpOperation == alert, then..
                        List<Alert> aList = getActiveAlerts();
                        for (Alert a:aList){
                            // if we find a matching active alert,
                            if (a.mTitle.equals(def.alert.mTitle)){
                                // set conditional status to true, and our alertStatus to true.
                                def.conditionals.get(0).status = true;
                                alertStatus = true;
                            }
                        }
                    } else {
                    // else set our statusInit on & our alertStatus to conditional status
                        statusInit = true;
                        alertStatus = def.conditionals.get(0).status;
                    }
                    continue;
                }

                // subsequent conditionals  (differentiate here between && and ||)
                if (def.conditionals.get(i-1).operator.contains("&&")) {
                    // For grpOperation == ("equip" or "delta") collect pointList and, if already present, take intersection of two conditionals
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
                   // else update statusInit & alertStatus based on && logic.
                        if (statusInit) {
                            alertStatus = alertStatus && def.conditionals.get(i).status;
                        } else {
                            statusInit = true;
                            alertStatus = def.conditionals.get(i).status;
                        }
                        
                    }
                } else if (def.conditionals.get(i-1).operator.contains("||")) {
                    // For grpOperation == ("equip" or "delta") collect pointList and, if already present, take union of two conditionals
                    if ((def.conditionals.get(i).grpOperation != null) && (def.conditionals.get(i).grpOperation.equals("equip")
                                                                            || def.conditionals.get(0).grpOperation.equals("delta")))
                    {
                        if (def.conditionals.get(i).pointList != null)
                        {
                            pointList.addAll(def.conditionals.get(i).pointList);
                        }
        
                    } else {
                        // else update statusInit & alertStatus based on && logic.
                        alertStatus = alertStatus || def.conditionals.get(i).status;
                    }
                }
            }
            // process alert points if present
            if (pointList != null) {
                for(String point : pointList) {
                    // make sure there is not already an active alert with same point ref and title
                    if (!alertActive(def.alert, point))
                    {
                        // Account for offset.
                        // Only propagate alert if offset == 0, or we reach offset (and manage offsetCount)
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
                    // note that we have an "active" ref for this processJob, even if not an issued alert yet.
                    activeAlertRefs.add(def.alert.mTitle+point);
                }
            // OR process if alertStatus true.
            } else if (alertStatus) {
                // note that we have an "active" ref for this processJob, even if not an issued alert yet.
                activeAlertRefs.add(def.alert.mTitle);
                // make sure there is not already an active alert with same point ref and title
                if (alertActive(def.alert)){
                    continue;
                }

                // And all the same offset logic for this conditional
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
        // even if active, we clear them at max time = 24 hours
        clearElapsedAlerts();
        
        for (Alert a : getActiveAlerts()) {
            CcuLog.d("CCU_ALERTS"," Active Alert "+a.toString());
        }
        List<Alert> unsyncedAlerts = getUnSyncedAlerts();
        CcuLog.d("CCU_ALERTS"," Unsynced Alerts "+unsyncedAlerts.size());

        // For any unsynced alerts, if ccu registered, sync them and update their state in DB.
        if (unsyncedAlerts.size() > 0)
        {
            if (!CCUHsApi.getInstance().isCCURegistered()){
                return;
            }

            List<Alert> syncedAlerts = alertSyncHandler.sync(mContext, unsyncedAlerts);
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

    /**
     * @return combined alert definitions
     */
    @Nonnull
    public ArrayList<AlertDefinition> getAlertDefinitions(){
        ArrayList<AlertDefinition> definedAlerts = new ArrayList<>();
        definedAlerts.addAll(predefinedAlerts);
        definedAlerts.addAll(getCustomAlertDefinitions());
        return definedAlerts;
    }
    
    private List<AlertDefinition> getCustomAlertDefinitions() {
        if (customAlerts.size() == 0)
        {
            String alerts = this.alertsSharedPrefs.getString(PREFS_ALERTS_CUSTOM, null);
            if (alerts != null) {
                customAlerts = parser.parseAlertsString(alerts);
            }
        }
        return customAlerts;
    }
    
    private void updateCustomAlertDefinitions(List<AlertDefinition> aList) {
        
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
        this.alertsSharedPrefs.edit().putString(PREFS_ALERTS_CUSTOM, alerts).apply();
    }

    /**
     * @return  All Alerts in DB that are not fixed.
     */
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
    

    /**
     * Database query for alerts with syncStatus false
     */
    public List<Alert> getUnSyncedAlerts(){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.equal(Alert_.syncStatus,false)
                  .orderDesc(Alert_.startTime);
        
        return alertQuery.build().find();
    }

    /**
     * @return Looks like this returns all alerts with severity not equal to an INTERNAL status
     */
    public List<Alert> getAllAlertsNotInternal(){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_INFO.ordinal());
        alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_LOW.ordinal());
        alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_MODERATE.ordinal());
        alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_SEVERE.ordinal());
        alertQuery.orderDesc(Alert_.startTime);
        return alertQuery.build().find();
    }

    public List<Alert> getAllAlertsOldestFirst(){
        QueryBuilder<Alert> alertQuery = alertBox.query();
        alertQuery.order(Alert_.startTime);
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

    // "Resolve" an alert
    public void fixAlert(Alert alert) {
        alert.setEndTime((new DateTime()).getMillis());
        alert.setFixed(true);
        alert.setSyncStatus(false);
        alertBox.put(alert);

        // special handling in preferences for "CCU RESTART" alert.  Is that restart alert, or that it was restarted?
        if (alert.mTitle.equalsIgnoreCase("CCU RESTART")){
            this.defaultSharedPrefs.edit().putBoolean("APP_RESTART", false).apply();
        }
    }
    
    public Completable deleteAlert(Alert alert) {
        CcuLog.w("CCU_ALERTS", "deleteAlert call in Processor ");

        //_id is empty if the alert is not synced to backend.
        if (alert._id.equals("")) {
            CcuLog.w("CCU_ALERTS", "empty global Id; just remove in alertBox");

            alertBox.remove(alert.id);
            return Completable.complete();
        } else {
            return alertSyncHandler.delete(alert._id)
                    .doOnComplete(() -> alertBox.remove(alert.id))
                    .doOnError(throwable -> CcuLog.w("CCU_ALERTS", "Delete alert failed "+alert._id, throwable));
        }
    }
    
    public void deleteAlertInternal(String id) {
        Alert a = getAlert(id);
        if (a != null)
        {
            alertBox.remove(a.id);
        }
    }

    /**
     * Generates an alert and puts it in our data store IF
     *  -- there is an existing alert definition with matching title
     *        -- that is enabled, and
     *        -- does NOT have a matching message.
     *
     * Questions:  why the above logic?    What if the alert we send has a message matching the alert definition.
     * It looks like this fails the second time it is tried since the alert definition message has been updated to match the message.
     */
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
    
    public void addAlertDefinition(List<AlertDefinition> list) {
        updateCustomAlertDefinitions(list);
    }
    public void addAlertDefinition(AlertDefinition alert) {
        updateCustomAlertDefinitions(Arrays.asList(alert));
    }

    /**
     * Called from here and from AlertManager during Registration.
     */
    public void fetchAllPredefinedAlerts(){
        if (predefinedAlerts == null || predefinedAlerts.size() == 0) {
            CcuLog.d("CCU_ALERTS", "Making Call");

            fetchPredefinedAlerts();
        } else {
            parseWifiSignalAlertDefinition();
        }
        CcuLog.d("CCU_ALERTS", "Predefined alerts");
        for (AlertDefinition def : predefinedAlerts) {
            CcuLog.d("CCU_ALERTS", "AlerDef:" + def);
        }
    }

    public void close() {
        if (fetchDisposable != null) {
            fetchDisposable.dispose();
        }
    }
}
