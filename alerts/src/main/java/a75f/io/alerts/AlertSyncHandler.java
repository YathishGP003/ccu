package a75f.io.alerts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import a75f.io.alerts.cloud.AlertSyncDto;
import a75f.io.alerts.cloud.AlertsService;
import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;

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
     *
     * Note:  all the calls are made synchronously, on the same thread.
     * The oldest are occurring first.  This ensures, among other things, that fixed alerts are
     * getting synced on the backend before any newly raised alerts of the same type.
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
                if (a.isFixed) CcuLog.d("CCU_ALERTS", "Creating *fixed* alert, i.e. alert without a remote id, to alerts-service: ");

                alertService.createAlert(siteId, dto)
                            .subscribe(
                                    response -> {
                                        if (response.isSuccessful()) {
                                            Alert alert = response.body();
                                            a.setGuid(alert._id);
                                            a.setSyncStatus(true);
                                            syncedAlerts.add(a);
                                            dataStore.updateAlert(a);
                                        } else {
                                            handleCreateAlertErrorResponse(response, a, dataStore);
                                        }
                                    },
                                    error -> CcuLog.w("CCU_ALERTS", "Unexpected error posting alert.", error)
                            );
            } else {
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
            CcuLog.w("CCU_ALERTS", "Attempted to sync " + alerts.size() + " alerts, but synced only "
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

    private void handleCreateAlertErrorResponse(Response<Alert> response, Alert alert, AlertsDataStore dataStore) {
        if (response.code() == 409) {
            ResponseBody errorBody = response.errorBody();
            if (errorBody != null) {
                handleDuplicateAlert(response.errorBody(), alert, dataStore);
            } else {
                CcuLog.w("CCU_ALERTS", "409 response with null error body");
                alert.setSyncStatus(true);
                dataStore.updateAlert(alert);
            }
        } else {
            CcuLog.w("CCU_ALERTS", "Unexpected error posting alert: " + response.toString());
        }
    }

    private void handleDuplicateAlert(ResponseBody errorBody, Alert alert, AlertsDataStore dataStore) {
        try {
            String alertId = parseAlertIdFrom409Message(errorBody.string());

            if (alertId != null) {
                // Does this id from the server belong to a different alert?
                Alert existingAlert = dataStore.getAlert(alertId);

                if (existingAlert == null) {
                    CcuLog.d("CCU_ALERTS", "handleDuplicateAlert, no existing alert for Id ,so giving Id to this alert");
                    alert.setGuid(alertId);

                    if (! alert.isFixed) {
                        CcuLog.i("CCU_ALERTS", "Alert matches unfixed alert on server.  Mark as synced.");
                        alert.setSyncStatus(true);
                    } else {
                        CcuLog.i("CCU_ALERTS", "Existing Alert is fixed.  Server needs to know, so leave unsynced.");
                    }
                    dataStore.updateAlert(alert);
                }
                else {
                    // we have a different, existing alert with the id on the server!
                    if (existingAlert.isFixed()) {
                        // we want to mark it as fixed on the server so this current alert can go up.
                        if (existingAlert.syncStatus) {
                            CcuLog.i("CCU_ALERTS", "There is an existing synced, fixed alert! Changing to unsynced so it will tell server its fixed. alertId = " + alertId);
                            existingAlert.setSyncStatus(false);
                            dataStore.updateAlert(existingAlert);
                        } else {
                            CcuLog.w("CCU_ALERTS", "There is an existing synced, unfixed alert!  It should sync on its own and make everything Ok. alertId = " + alertId);
                        }
                    } else {
                        CcuLog.e("CCU_ALERTS", "There is an existing alert, not fixed!  There must be an error in a uniqueness test. alertId = " + alertId);
                    }
                }
            } else {
                CcuLog.e("CCU_ALERTS", "Unable to parse AlertId from errorBody.  Has server response format changed?  Mark this as synced and proceed is all we can do");
                alert.setSyncStatus(true);
                dataStore.updateAlert(alert);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // We are expecting a response body string, which gets put in Response's errorBody by retrofit adapter,
    // in the following format:  {"error":"Alert for definition = a3ee2d2c-0e4d-4931-8d68-d649e76afc405ee1c79bb061823fd96f3593 equip = bd5f4df1-8dca-483d-8c1b-0681be1e32f3 ccu = ed73674e-059b-4efd-afc1-9482c552732a already created and unresolved. Existing alertIds: [60d0944c387c257d4e2a0ec4]"}
    private @Nullable String parseAlertIdFrom409Message(String message) {
        if (message == null) return null;
        int index = message.indexOf("Existing alertIds");
        if (index >= 0) {
            int startIndex = message.indexOf('[', index);
            int endIndex = message.indexOf(']', index);
            if (startIndex > 0 && endIndex > startIndex) {
                return message.substring(startIndex+1, endIndex);
            }
        }
        return null;
    }
}
