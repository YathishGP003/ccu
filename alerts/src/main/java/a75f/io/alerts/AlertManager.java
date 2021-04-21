package a75f.io.alerts;

import android.content.Context;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import a75f.io.alerts.cloud.AlertsService;
import a75f.io.alerts.cloud.ServiceGenerator;
import a75f.io.api.haystack.Alert;
import a75f.io.logger.CcuLog;
import io.reactivex.rxjava3.core.Completable;
/**
 * Created by samjithsadasivan on 4/24/18.
 */

/**
 * AlertManager provides APIs to process and access alerts.
 */

public class AlertManager
{
    /** There will always be an instance, but there may be no service.  That occurs when
     * there is no bearer token.
     */
    private static AlertManager mInstance;

    // Set at creation & whenever base url changes b/c of user setting on local build.
    private String baseUrl;

    // Created each time baseUrl or token changes.
    private AlertsService alertsService = null;

    // depends on AlertsService.  So, created anew each time AlertsService is created.
    AlertProcessor processor;

    private Context appContext;

    /**
     * Call this when apiBase changes.  Token should not be null, so please include current token.
     */
    public void setAlertsApiBase(String alertsApiBase, String token) {
        this.baseUrl = alertsApiBase;

        if (token != null & !token.isEmpty()) {
            this.alertsService = ServiceGenerator.getInstance().createService(alertsApiBase, token);
            processor = new AlertProcessor(appContext, this.alertsService);
        }
    }

    public void rebuildServiceNewToken(String token) {
        this.alertsService = ServiceGenerator.getInstance().createService(baseUrl,token);
        processor = new AlertProcessor(appContext, this.alertsService);
    }

    public AlertsService getAlertsService() {
        return alertsService;
    }

    /**
     * Please use this constructor (and not getInstance()) wherever possible to
     * avoid a crash if we haven't instantiated yet.
     *
     * @param appContext
     * @param alertsApiBase
     * @param token
     */
    private AlertManager(Context appContext, String alertsApiBase, String token) {
        this.appContext = appContext;
        setAlertsApiBase(alertsApiBase, token);
    }

    public static AlertManager getInstance(Context c, String alertsApiBase, String token) {
        if (mInstance == null) {
            mInstance = new AlertManager(c, alertsApiBase, token);
        }
        if (!mInstance.hasService()) {
            mInstance.setAlertsApiBase(alertsApiBase, token);
        }
        return mInstance;
    }

    /**
     * Please only call this from places you know registration is complete.
     *
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
        return alertsService != null;
    }

    public void processAlerts() {
        if (! processorCheck()) return;
        processor.processAlerts();
    }

    public void generateAlert(String title, String msg){
        if (! processorCheck()) return;
        processor.generateAlert(title,msg);
    }

    public void generateCMDeadAlert(String title, String msg){
        if (! processorCheck()) return;
        processor.generateCMDeadAlert(title,msg);
    }

    public void fetchPredefinedAlertsIfEmpty(){
        if (! processorCheck()) return;
        processor.fetchPredefinedAlertsIfEmpty();
    }

    public void fetchPredefinedAlerts(){
        if (! processorCheck()) return;
        processor.fetchPredefinedAlerts();
    }
    
    public List<Alert> getActiveAlerts() {
        if (! processorCheck()) return Collections.emptyList();

        return processor.getActiveAlerts();
    }

    public List<Alert> getAllAlertsOldestFirst() {
        if (! processorCheck()) return Collections.emptyList();

        return processor.getAllAlertsOldestFirst();
    }

    public List<Alert> getAllAlertsNotInternal() {
        if (! processorCheck()) return Collections.emptyList();

        return processor.getAllAlertsNotInternal();
    }

    /** Exposed for monitoring */
    public HashMap<String, Integer> getOffsetCounter() {
        if (! processorCheck()) return new HashMap<>();

        return processor.offsetCounter;
    }

    public List<AlertDefinition> getAlertDefinitions() {
        if (! processorCheck()) return Collections.emptyList();

        return processor.getAlertDefinitions();
    }
    
    public void fixAlert(Alert a) {
        if (! processorCheck()) return;
        processor.fixAlert(a);
    }

    public Completable deleteAlert(Alert a) {
        if (! processorCheck()) return Completable.complete();
        return processor.deleteAlert(a);
    }
    
    public void deleteAlertInternal(String id) {
        if (! processorCheck()) return;
        processor.deleteAlertInternal(id);
    }
    public void addAlertDefinition(AlertDefinition alert) {
        if (! processorCheck()) return;
        processor.addAlertDefinition(alert);
    }
    public void deleteAlertDefinition(String _id) {
        if (! processorCheck()) return;
        processor.deleteCustomAlertDefinition(_id);
    }

    // This method seems to mark an alert as fixed if it has been synced, and
    //  delete the alert if it hasn't
    public void clearAlertsWhenAppClose(){
        if (! processorCheck()) return;

        processor.close();

        for (Alert a: getActiveAlerts()){
            if (!a.isFixed() && a.getSyncStatus()){
               fixAlert(a);
            } else if (!a.getSyncStatus()){
                deleteAlert(a)
                        .subscribe();
            }
        }
    }

    private boolean processorCheck() {
        if (processor == null) {
            CcuLog.d("CCU_ALERTS", "Processor null (no service) in AlertManager");
            return false;
        }
        return true;
    }

    public void fixDeviceDead(String address){
        if (! processorCheck()) return;

        for (Alert a: processor.getActiveDeviceDeadAlerts()){
            if (a.mMessage.contains(address)){
                fixAlert(a);
            }
        }
    }

    public void fixCMDead(){
        if (! processorCheck()) return;

        for (Alert a: processor.getActiveCMDeadAlerts()){
            fixAlert(a);
        }
    }
}

