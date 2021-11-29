package a75f.io.alerts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

        List<String> otherAlertIds = null;
        try {
            otherAlertIds = parseAlertIdsFrom409Message(errorBody.string());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (otherAlertIds == null || otherAlertIds.isEmpty()) {
            CcuLog.e("CCU_ALERTS","Unable to parse AlertId(s) from the response body. Has the server's response format changed?" +
                    " Marking this new, duplicate alert as synced otherwise it will infinitely result in a 409." +
                    " Response body = " + errorBody.toString() +
                    " | New alert that was never created in the remote = " + alert);
            alert.setSyncStatus(true);
            dataStore.updateAlert(alert);
        } else {
            for (String otherAlertId : otherAlertIds) {
                Alert otherAlert = dataStore.getAlert(otherAlertId);

                if (otherAlert == null) { // The CCU does NOT know about the remote alert (i.e. it does not have a local copy of it).
                    // TODO: Two sources can create alerts - the CCU and the Alerts Service. When the Alerts Service creates an alert, the CCU needs to know about it.
                    //  Inspecting a 409 response and attaching a remote guid to a locally-created alert is probably not the best way to handle this communication.
                    //  Short term, the current method will keep the CCU from blasting the server with dup requests.
                    //  There is no risk of the local alert changing any of the remote alert's "true alert content" (i.e. start time, end time, etc.) other than the isFixed flag, which is desired.
                    //  Long term, perhaps a message can be published by the Alerts Service and consumed by the CCU?
                    //  The CCU could then determine whether it created a local duplicate before sending the POST request and take the necessary actions.
                    CcuLog.d("CCU_ALERTS",
                             "The remote alert does not exist locally. It must have been created by the Alert Service." +
                                     " Setting the local alert's guid equal to the remote alert's guid. The local alert has now assumed the identity of the remote alert.");
                    alert.setGuid(otherAlertId);

                    if (!alert.isFixed) {
                        CcuLog.i("CCU_ALERTS",
                                 "The local alert has assumed the remote alert's identity. The local alert is unfixed and so is the remote alert." +
                                         " No need to sync the local with the remote. Setting the local alert's sync status to true." +
                                         " Remote alertId = " + otherAlertId);
                        alert.setSyncStatus(true);
                    } else {
                        CcuLog.i("CCU_ALERTS",
                                 "The local alert has assumed the remote alert's identity. The local alert is fixed, but the remote alert is not." +
                                         " Need to sync the local with the remote. Setting the local alert's sync status to false." +
                                         " Remote alertId = " + otherAlertId);
                        alert.setSyncStatus(false);
                    }
                    dataStore.updateAlert(alert);
                } else {
                    // The CCU knows about the remote alert (i.e. it has a local copy of it).
                    // After the duplicate-alert data migration, this should no longer happen.
                    // Perhaps the data migration missed something? Or something else has not been accounted for?
                    //  Or the Alerts Service can throw a wrench in things when it processes alerts?
                    // Inspecting the state of the remote alert to aid in troubleshooting.
                    if (otherAlert.isFixed) {
                        if (otherAlert.syncStatus) {
                            // Unaware of any code paths in which this could happen.
                            // If an alert is marked as fixed by the CCU, its sync status is set to false.
                            // If the alert update fails with the Alert Service, the sync status will remain false.
                            // Did the Alerts Service mark it as fixed in its own processing logic?
                            // In any case, should this happen somehow someway, mark its local copy as unsynced so it can be re-synced as fixed in the remote.
                            // The new alert will then be able to be POSTed in the next loop.
                            CcuLog.i("CCU_ALERTS",
                                     "The remote alert's local copy is FIXED and synced, but is UNFIXED in the remote. How could this be?" +
                                             " Setting the local copy's sync status to false so it will be synced in the next loop. The remote will then be marked as fixed." +
                                             " If this 409 occurs in the next loop, then there is a BUG somewhere!!!!" +
                                             " Remote alertId = " + otherAlertId);
                        } else {
                            boolean isNewer = otherAlert.startTime > alert.getStartTime();
                            if (isNewer) {
                                // If this occurs, then there might be an issue with the data migration.
                                // The NEWER, unsynced duplicate alters (fixed or unfixed) should have been deleted, but it is still here?
                                // (I suppose the startTimes could have been changed, but there is no code path for that)
                                CcuLog.i("CCU_ALERTS",
                                         "The remote alert IS NOT fixed, but its local copy IS fixed AND it is NEWER than the new alert. How could this be?" +
                                                 " There could be a bug in the duplicate-alert data migration logic, or requests are not synchronous and in ascending order by startTime!!!! " +
                                                 " Good news - this remote alert should be synced as fixed later in THIS loop (because it is newer, the request has not yet been sent)" +
                                                 " If this 409 occurs in the next loop, then there is a BUG somewhere!!!!" +
                                                 " Remote alertId = " + otherAlertId);
                            }
                        }
                    } else {
                        // This remote alert's local copy is NOT fixed.
                        // If the data migration was successful, then the new alert was created in error. An existing, UNFIXED alert apparently already existed!!!
                        // Or the data migration was unsuccessful....
                        // Deleting the new, duplicated alert.
                        CcuLog.i("CCU_ALERTS",
                                 "The remote alert is NOT fixed. There is a BUG somewhere!!!" +
                                         "This alert should never have been created or it should have been cleaned up in the data migration. Deleting the new, duplicated alert." +
                                         "Remote alertId = " + otherAlertId + " | Alert = " + alert);
                        dataStore.deleteAlert(alert);
                    }
                }
            }
        }
    }

    // We are expecting a response body string, which gets put in Response's errorBody by retrofit adapter,
    // in the following format:  {"error":"Alert for definition = a3ee2d2c-0e4d-4931-8d68-d649e76afc405ee1c79bb061823fd96f3593 equip = bd5f4df1-8dca-483d-8c1b-0681be1e32f3 ccu = ed73674e-059b-4efd-afc1-9482c552732a already created and unresolved. Existing alertIds: [60d0944c387c257d4e2a0ec4]"}
    private @Nullable List<String> parseAlertIdsFrom409Message(String message) {
        if (message == null) {
            return Collections.emptyList();
        }
        int index = message.indexOf("Existing alertIds");
        if (index > - 1) {
            int startIndex = message.indexOf('[', index) + 1;
            int endIndex = message.indexOf(']', index);
            if (startIndex > 0 && endIndex > startIndex) {
                return Arrays.asList(message.substring(startIndex, endIndex).split(","));
            }
        }
        return Collections.emptyList();
    }
}