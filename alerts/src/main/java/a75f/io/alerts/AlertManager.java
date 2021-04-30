package a75f.io.alerts;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import a75f.io.alerts.cloud.AlertsService;
import a75f.io.alerts.cloud.ServiceGenerator;
import a75f.io.alerts.model.AlertDefOccurrence;
import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
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
    AlertsRepository repo;

    private final Context appContext;

    /**
     * Call this when apiBase changes.  Token should not be null, so please include current token.
     */
    public void setAlertsApiBase(String alertsApiBase, String token) {
        this.baseUrl = alertsApiBase;

        if (token != null & !token.isEmpty()) {
            this.alertsService = ServiceGenerator.getInstance().createService(alertsApiBase, token);
            String ccuId = CCUHsApi.getInstance().getCcuRef().toVal();
            AlertProcessor processor = new AlertProcessor(appContext, ccuId, CCUHsApi.getInstance());
            repo = new AlertsRepository(
                    new AlertsDataStore(appContext),
                    processor,
                    alertsService,
                    new AlertSyncHandler(alertsService),
                    CCUHsApi.getInstance()
            );
        }
    }

    public void rebuildServiceNewToken(String token) {
        this.alertsService = ServiceGenerator.getInstance().createService(baseUrl,token);
        String ccuId = CCUHsApi.getInstance().getCcuRef().toVal();
        AlertProcessor processor = new AlertProcessor(appContext, ccuId, CCUHsApi.getInstance());
        repo = new AlertsRepository(
                new AlertsDataStore(appContext),
                processor,
                alertsService,
                new AlertSyncHandler(alertsService),
                CCUHsApi.getInstance()
        );
    }

    public AlertsService getAlertsService() {
        return alertsService;
    }

    /**
     * Please use this constructor (and not getInstance()) wherever possible to
     * avoid a crash if we haven't instantiated yet.
     *
     * @param appContext     application context (android)
     * @param alertsApiBase  the base of the URL for alerts service, e.g. "http://192.168.0.122:8087"
     * @param token         bearer token for authentication
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
        if (! repoCheck()) return;
        repo.processAlertDefs();
    }

    public void generateAlert(String title, String msg){
        if (! repoCheck()) return;
        repo.generateAlert(title,msg);
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
        return repo.getCurrentOccurrences();
    }

    public List<AlertDefinition> getAlertDefinitions() {
        if (! repoCheck()) return Collections.emptyList();

        return repo.getAlertDefinitions();
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
            CcuLog.d("CCU_ALERTS", "Repository null (no service) in AlertManager");
            return false;
        }
        return true;
    }

    public void fixDeviceDead(String address){
        if (! repoCheck()) return;

        for (Alert a: repo.getActiveDeviceDeadAlerts()){
            if (a.mMessage.contains(address)){
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
}

