package a75f.io.alerts;

import static a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import a75f.io.alerts.cloud.AlertsService;
import a75f.io.alerts.cloud.ServiceGenerator;
import a75f.io.alerts.log.SequenceLogs;
import a75f.io.alerts.model.AlertDefOccurrence;
import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import io.reactivex.rxjava3.core.Completable;

/*
  Created by samjithsadasivan on 4/24/18.
 */

/**
 * AlertManager provides APIs to process and access alerts.
 */

public class AlertManager implements CCUHsApi.EntityDeletedListener
{
    /** There will always be an instance, but there may be no service.  That occurs when
     * there is no bearer token.
     */
    private static AlertManager mInstance;

    public AlertListListener alertListListener;

    // Set at creation & whenever base url changes b/c of user setting on local build.
    private String baseUrl;

    // Created each time baseUrl or token changes.
    private AlertsService alertsService = null;

    // depends on AlertsService.  So, created anew each time AlertsService is created.
    AlertsRepository repo;

    private final Context appContext;

    private final HashMap<String, Object> persistentBlockMap= new HashMap<>();

    HashMap<String, SequenceLogs> sequenceLogsMap = new HashMap<>();

    /**
     * Call this when apiBase changes.  Token should not be null, so please include current token.
     */
    public void setAlertsApiBase(String alertsApiBase) {
        this.baseUrl = alertsApiBase;

        this.alertsService = ServiceGenerator.getInstance().createService(alertsApiBase);
        AlertProcessor processor = new AlertProcessor(appContext);
        repo = new AlertsRepository(
                new AlertsDataStore(appContext),
                processor,
                alertsService,
                new AlertSyncHandler(alertsService),
                CCUHsApi.getInstance()
        );
    }

    public AlertsRepository getRepo(){ return repo; }

    public AlertsService getAlertsService() {
        return alertsService;
    }

    /**
     * Please use this constructor (and not getInstance()) wherever possible to
     * avoid a crash if we haven't instantiated yet.
     *
     * @param appContext     application context (android)
     * @param alertsApiBase  the base of the URL for alerts service, e.g. "http://192.168.0.122:8087"
     */
    private AlertManager(Context appContext, String alertsApiBase) {
        this.appContext = appContext;
        setAlertsApiBase(alertsApiBase);
        CCUHsApi.getInstance().registerEntityDeletedListener(this);
    }

    public static AlertManager getInstance(Context c, String alertsApiBase) {
        if (mInstance == null) {
            mInstance = new AlertManager(c, alertsApiBase);
        }
        if (!mInstance.hasService()) {
            mInstance.setAlertsApiBase(alertsApiBase);
        }
        return mInstance;
    }

    /**
     * Please only call this from places you know registration is complete.
     * <p>
     * Otherwise there will be consequences.
     */
    public static AlertManager getInstance()
    {
        if (mInstance == null)
        {
            throw new IllegalStateException("No AlertManager instance found");
        }
        return mInstance;
    }

    public boolean hasService() {
        return alertsService != null && repo != null;
    }

    public void processAlerts() {
        CcuLog.d(TAG_CCU_ALERTS, "processAlerts: ");
        if (! repoCheck()) return;
        repo.processAlertDefs();
    }

    public void processAlertBox() {
        if (! repoCheck()) return;
        repo.handleAlertBoxItemsExceedingThreshold();
    }

    public void generateAlert(String title, String msg){
        if (! repoCheck()) return;
        repo.generateAlert(title,msg, "");
    }

    public void generateAlert(String title, String msg, String equipRef){
        if (! repoCheck()) return;
        repo.generateAlert(title,msg, equipRef);
    }

    public void generateAlertBlockly(String title, String msg, String equipRef, String creator, String blockId){
        if (! repoCheck()) return;
        repo.generateAlertBlockly(title,msg, equipRef, creator, blockId);
    }

    public void generateAlertSequencerBlockly(String title, String msg, String equipRef,
                                              String creator, String blockId, AlertDefinition alertDef){
        if (! repoCheck()) return;
        repo.generateAlertSequencerBlockly(title,msg, equipRef, creator, blockId, alertDef);
    }

    public void generateCMDeadAlert(String title, String msg){
        if (! repoCheck()) return;
        repo.generateCMDeadAlert(title,msg);
    }

    public void fetchPredefinedAlertsIfEmpty(){
        if (! repoCheck()) return;
        repo.fetchAlertDefssIfEmpty();
    }

    public void fetchPredefinedAlerts(){
        if (! repoCheck()) return;
        repo.fetchAlertsDefinitions();
    }
    
    public List<Alert> getActiveAlerts() {
        if (! repoCheck()) return Collections.emptyList();

        return repo.getActiveAlerts();
    }

    public List<Alert> getActiveAlertsByCreator(String creator) {
        if (! repoCheck()) return Collections.emptyList();

        return repo.getAlertsByCreator(creator);
    }

    public List<Alert> getActiveAlertsByCreatorAndBlockId(String creator, String blockId) {
        if (! repoCheck()) return Collections.emptyList();

        return repo.getAlertsByCreatorAndBlockId(creator, blockId);
    }

    public List<Alert> getUnsyncedAlerts() {
        if (! repoCheck()) return Collections.emptyList();

        return repo.getUnsyncedAlerts();
    }

    public List<Alert> getAllAlertsOldestFirst() {
        if (! repoCheck()) return Collections.emptyList();

        return repo.getAllAlertsOldestFirst();
    }

    public List<Alert> getAllAlertsNotInternal() {
        if (! repoCheck()) return Collections.emptyList();

        return repo.getAllAlertsNotInternal();
    }

    /** Exposed for monitoring */
    public Map<AlertDefOccurrence, Integer> getOffsetCounter() {
        if (! repoCheck()) return new HashMap<>();

        return repo.getOffsetCounter();
    }

    public List<AlertDefOccurrence> getAlertDefOccurrences() {
        if (! repoCheck()) return new ArrayList<>();

        return repo.getCurrentOccurrences();
    }

    public void fixAlert(Alert a) {
        if (! repoCheck()) return;
        repo.fixAlert(a);
    }

    public Completable deleteAlert(Alert a) {
        if (! repoCheck()) return Completable.complete();
        return repo.deleteAlert(a);
    }
    
    public void deleteAlertInternal(String id) {
        if (! repoCheck()) return;
        repo.deleteAlertInternal(id);
    }
    public void addAlertDefinition(AlertDefinition alert) {
        if (! repoCheck()) return;
        repo.addAlertDefinition(alert);
    }
    public void deleteAlertDefinition(String _id) {
        if (! repoCheck()) return;
        repo.deleteAlertDefinition(_id);
    }

    public void deleteAlertsForDef(AlertDefinition alertDef){
        if (! repoCheck()) return;
        repo.deleteAlertsForSequencerDef(alertDef);
    }

    public void fixAlertByDef(AlertDefinition alertDef){
        if (! repoCheck()) return;
        repo.fixAlertByDef(alertDef);
    }

    // This method seems to mark an alert as fixed if it has been synced, and
    //  delete the alert if it hasn't
    public void clearAlertsWhenAppClose(){
        if (! repoCheck()) return;

       repo.close();

        for (Alert a: getActiveAlerts()){
            if (!a.isFixed() && a.getSyncStatus()){
               fixAlert(a);
            } else if (!a.getSyncStatus()){
                deleteAlert(a)
                        .subscribe();
            }
        }
    }

    private boolean repoCheck() {
        if (repo == null) {
            CcuLog.d(TAG_CCU_ALERTS, "Repository null (no service) in AlertManager");
            return false;
        }
        return true;
    }

    public void fixDeviceDead(String address){
        if (! repoCheck()) return;

        for (Alert a: repo.getActiveDeviceDeadAlerts()){
            if (a.mMessage.split(",")[a.mMessage.split(",").length-1].contains(address)){
                fixAlert(a);
            }
        }
    }

    public void fixCMDead(){
        if (! repoCheck()) return;

        for (Alert a: repo.getActiveCMDeadAlerts()){
            fixAlert(a);
        }
    }

    /**Fixing Safe mode explicitly
     * as this value is set and restarted immediately
     */
    public void fixSafeMode(){
        if (! repoCheck()) return;

        for (Alert a: repo.getActiveSafeModeAlert()){
            fixAlert(a);
        }
    }

    // This method is called when a device is rebooted and there is an existing active DEVICE REBOOT ALERT
    // It fixes the existing active alert, so that a new DEVICE REBOOT alert can be generated for the same device.
    public void fixActiveDeviceRebootAlert(String deviceRef) {
        if (! repoCheck()) return;
        for (Alert a: repo.getDeviceRebootActiveAlert(deviceRef.replace("@",""))) {
            fixAlert(a);
        }
    }

    public void generateCrashAlert(String title, String msg){
        if (! repoCheck()) return;
        repo.generateCrashAlertWithMessage(title,msg);
    }


    /**Fixing CrashAlert explicitly
     * if there is a upcoming crash_alert
     */
    public void fixPreviousCrashAlert(){
        if (! repoCheck()) return;

        for (Alert a: repo.getActiveCrashAlert()){
            fixAlert(a);
        }
    }

    public void initValue(String key, Object value) {
        persistentBlockMap.computeIfAbsent(key, k -> value);
    }

    public void putValue(String key, Object value) {
        persistentBlockMap.put(key, value);
    }

    public Object getValue(String key) {
        return persistentBlockMap.get(key);
    }

    public interface AlertListListener {
        void onAlertsChanged();
    }

    public void setAlertListListener(AlertListListener alertListListener) {
        this.alertListListener = alertListListener;
    }

    // This overridden method is called when a device is deleted to fix all the alerts associated with the device
    @Override
    public void onDeviceDeleted(String deviceRef) {
        if (! repoCheck()) return;
        for(Alert alert: repo.getActiveAlertsByRef(deviceRef.replace("@",""))) {
            CcuLog.d(TAG_CCU_ALERTS, "Device deleted: " + deviceRef + " Fixing alert: " + alert.mTitle);
            fixAlert(alert);
        }
    }

    /* This method is called when the CCU is opened.
    *  It fetches the alert definitions, processes the active predefined and legacy custom alerts
    *  and schedule the alert definitions' evaluation after every minute
    * */
    public void initiateAlertOperations(ScheduledExecutorService taskExecutor) {
        new Thread("AlertOperationsInitiated") {
            @Override
            public void run() {
                CcuLog.d(TAG_CCU_ALERTS, "Alert Operations Init started: ");
                fetchPredefinedAlertsIfEmpty();
                repo.processAlertsOnAppOpen();
                taskExecutor.scheduleAtFixedRate(new AlertProcessJob(appContext).getJobRunnable(), 60, 60, TimeUnit.SECONDS);
                CcuLog.d(TAG_CCU_ALERTS, "Alert Operations Init completed: ");
            }
        }.start();
    }

    public void fixAlertLocally(Alert a) {
        if (! repoCheck()) return;
        repo.fixAlertLocally(a);
    }

    public HashMap<String, SequenceLogs> getSequenceLogsMap() {
        return sequenceLogsMap;
    }

    public void addAlertDef(String key, AlertDefinition alertDefinition){
        repo.addSequencerOccurrence(key, alertDefinition);
    }

    public void removeAlertDef(String key){
        repo.removeSequencerOccurrence(key);
    }

    public void removeAlertDefUsingAlertDefId(String defId){
        repo.removeSequencerOccurrenceByDefId(defId);
    }
}