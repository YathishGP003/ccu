package a75f.io.alerts;

import android.content.Context;

import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;

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
    private static AlertManager mInstance;
    
    AlertProcessor processor;

    // This is set from logic module, RenatusServiceEnv#setupUrls()
    // This is needed, instead of just injecting an instance RenatusServiceEnv, b/c of the way the modules
    //   are set up.
    private AlertsService alertsService = null;

    public void setAlertsApiBase(String alertsApiBase) {
        this.alertsService = ServiceGenerator.getInstance().updateAlertsUrl(alertsApiBase);
    }

    /**
     * Depending on setAlertsApiBase being called to ensure non-nullness.
     */
    public @Nonnull AlertsService getAlertsService() {
        return alertsService;
    }

    private AlertManager(Context appContext, String alertsApiBase) {
        CcuLog.d("CCU_ALERTS", "Instantiating AlertManager");
        setAlertsApiBase(alertsApiBase);

        processor = new AlertProcessor(appContext, this.alertsService);
    }

    public static AlertManager getInstance(Context c, String alertsApiBase) {
        if (mInstance == null) {
            mInstance = new AlertManager(c, alertsApiBase);
        }
        return mInstance;
    }

    public static AlertManager getInstance()
    {
        if (mInstance == null)
        {
            throw new IllegalStateException("No instance found");
        }
        return mInstance;
    }
    
    public void processAlerts() {
        processor.processAlerts();
    }

    public void generateAlert(String title, String msg){
        processor.generateAlert(title,msg);
    }

    public void generateCMDeadAlert(String title, String msg){
        processor.generateCMDeadAlert(title,msg);
    }

    public void fetchAllPredefinedAlerts(){
        processor.fetchAllPredefinedAlerts();
    }

    public void fetchPredefinedAlerts(){
        processor.fetchPredefinedAlerts();
    }
    
    public List<Alert> getActiveAlerts() {
        return processor.getActiveAlerts();
    }

    public List<Alert> getAllAlertsOldestFirst() {
        return processor.getAllAlertsOldestFirst();
    }

    public List<Alert> getAllAlertsNotInternal() {
        return processor.getAllAlertsNotInternal();
    }

    /** Exposed for monitoring */
    public HashMap<String, Integer> getOffsetCounter() {
        return processor.offsetCounter;
    }

    public List<AlertDefinition> getAlertDefinitions() {
        return processor.getAlertDefinitions();
    }
    
    public void addAlertDefinition(List<AlertDefinition> list) {
        processor.addAlertDefinition(list);
    }
    
    public void fixAlert(Alert a) {
        processor.fixAlert(a);
    }

    public Completable deleteAlert(Alert a) {
        return processor.deleteAlert(a);
    }
    
    public void deleteAlertInternal(String id) {
        processor.deleteAlertInternal(id);
    }
    public void addAlertDefinition(AlertDefinition alert) {
        processor.addAlertDefinition(alert);
    }
    public void deleteAlertDefinition(String _id) {
        processor.deleteCustomAlertDefinition(_id);
    }

    // This method seems to mark an alert as fixed if it has been synced, and
    //  delete the alert if it hasn't
    public void clearAlertsWhenAppClose(){

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

    public void fixDeviceDead(String address){
        for (Alert a: processor.getActiveDeviceDeadAlerts()){
            if (a.mMessage.contains(address)){
                fixAlert(a);
            }
        }
    }

    public void fixCMDead(){
        for (Alert a: processor.getActiveCMDeadAlerts()){
            fixAlert(a);
        }
    }
}

