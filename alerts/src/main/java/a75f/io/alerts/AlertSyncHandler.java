package a75f.io.alerts;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import a75f.io.alerts.cloud.AlertSyncDto;
import a75f.io.alerts.cloud.AlertsService;
import a75f.io.alerts.cloud.ServiceGenerator;
import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Predicate;
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
     * Returns same set, but with alerts' guids set and and sync status set to true.
     */
    public List<Alert> sync(Context c, List<Alert> alerts) {
        ArrayList<Alert> syncedAlerts = new ArrayList<>();

        // We are not doing anything with these rx disposables, but we could...
        AlertsService alertService = ServiceGenerator.getInstance().getAlertsService();
        String siteId = CCUHsApi.getInstance().getGlobalSiteIdNoAtSign();
        if (siteId == null) {
            return syncedAlerts;
        }

        for (Alert a : alerts)
        {
            String deviceId = CCUHsApi.getInstance().getGUID(a.deviceRef);
            
            if (deviceId == null) {
                continue;
            }

            AlertSyncDto clone = AlertSyncDto.fromAlert(a, deviceId);

            // Alert not present on service if no guid.  If none, create.  Else, update
            if (a.getGuid().equals(""))
            {
                // No schedulers for the rx calls: need to keep everything on this method on same thread
                // to keep this method working under existing pattern.

                alertService.createAlert(siteId, clone)
                            .subscribe(
                                    alert -> {
                                        a.setGuid(alert._id);
                                        a.setSyncStatus(true);
                                        syncedAlerts.add(a);
                                    },
                                    error -> CcuLog.w("CCU_ALERTS", "Unexpected error posting alert.", error)
                            );
            }else {
                alertService.updateAlert(siteId, a.getGuid(), clone)
                            .subscribe(
                                    alert -> {
                                        a.setSyncStatus(true);
                                        syncedAlerts.add(a);
                                    },
                                    error -> CcuLog.w("CCU_ALERTS", "Unexpected error updating alert.", error)
                            );
            }
        }

        if (alerts.size() != syncedAlerts.size()) {
            CcuLog.w("CCU_ALERTS", "Attempted to sync " + alerts.size() + "alerts, but synced only "
                    + syncedAlerts.size());
        }
        return syncedAlerts;
    }

    /**
     * Returns a Completable that, when subscribed to calls the delete alert service for the given
     * alert Id and completes the rx call.
     */
    public Completable delete(String id) {
        CcuLog.w("CCU_ALERTS", "called delete on service");
        return alertsService.deleteAlert(CCUHsApi.getInstance().getGlobalSiteIdNoAtSign(), id)
                            // return normally if the alert to be deleted is missing on the server
                            .onErrorComplete(throwable -> throwable instanceof HttpException &&
                                    throwable.getLocalizedMessage().contains("404"))
                .subscribeOn(Schedulers.io());

    }
}
