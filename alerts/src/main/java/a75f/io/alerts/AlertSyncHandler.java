package a75f.io.alerts;

import java.util.ArrayList;
import java.util.List;

import a75f.io.alerts.cloud.AlertSyncDto;
import a75f.io.alerts.cloud.AlertsService;
import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.HttpException;

public class AlertSyncHandler
{
    private AlertsService alertsService;

    public AlertSyncHandler(AlertsService alertsService) {
        this.alertsService = alertsService;
    }

    /**
     * Creates or updates all listed alerts with service depending on if they already have a guid.
     * Pass in dataStore to update in parallel.
     * Returns same set, but with alerts' guids set and and sync status set to true.
     */
    public void sync(List<Alert> alerts, AlertsDataStore dataStore) {
        ArrayList<Alert> syncedAlerts = new ArrayList<>();

        // We are not doing anything with these rx disposables, but we could...
        AlertsService alertService = AlertManager.getInstance().getAlertsService();
        if (! CCUHsApi.getInstance().siteSynced()) {
            return;
        }
        String siteId = CCUHsApi.getInstance().getSiteIdRef().val;

        for (Alert a : alerts)
        {
            if (a.ccuIdNoAt == null) {
                continue;
            }

            AlertSyncDto dto = AlertSyncDto.fromAlert(a);

            // Alert not present on service if no guid.  If none, create.  Else, update
            if (a.getGuid().equals(""))
            {
                CcuLog.d("CCU_ALERTS", "Creating alert to alerts-service: " + a);

                alertService.createAlert(siteId, dto)
                            .subscribe(
                                    alert -> {
                                        a.setGuid(alert._id);
                                        a.setSyncStatus(true);
                                        syncedAlerts.add(a);
                                        dataStore.updateAlert(a);
                                    },
                                    error -> CcuLog.w("CCU_ALERTS", "Unexpected error posting alert.", error)
                            );
            }else {
                CcuLog.d("CCU_ALERTS", "Updating alert on alerts-service: " + a);

                alertService.updateAlert(siteId, a.getGuid(), dto)
                            .subscribe(
                                    alert -> {
                                        a.setSyncStatus(true);
                                        syncedAlerts.add(a);
                                        dataStore.updateAlert(a);
                                    },
                                    error -> CcuLog.w("CCU_ALERTS", "Unexpected error updating alert.", error)
                            );
            }
        }

        if (alerts.size() != syncedAlerts.size()) {
            CcuLog.w("CCU_ALERTS", "Attempted to sync " + alerts.size() + "alerts, but synced only "
                    + syncedAlerts.size());
        }
    }

    /**
     * Returns a Completable that, when subscribed to calls the delete alert service for the given
     * alert Id and completes the rx call.
     */
    public Completable delete(String id) {
        CcuLog.w("CCU_ALERTS", "called delete on service");
        return alertsService.deleteAlert(CCUHsApi.getInstance().getSiteIdRef().val, id)
                            // return normally if the alert to be deleted is missing on the server
                            .onErrorComplete(throwable -> throwable instanceof HttpException &&
                                    throwable.getLocalizedMessage().contains("404"))
                .subscribeOn(Schedulers.io());
    }
}
