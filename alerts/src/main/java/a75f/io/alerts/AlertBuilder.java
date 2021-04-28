package a75f.io.alerts;

import java.util.GregorianCalendar;

import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;

/**
 * Created by samjithsadasivan on 4/25/18.
 */

public class AlertBuilder
{
    public static Alert build(Alert a) {
        Alert alert = new Alert();
        alert.setId(a.id);
        alert.setGuid(a._id);
        alert.setStartTime(a.startTime);
        alert.setEndTime(a.endTime);
        alert.setmTitle(a.mTitle);
        alert.setmMessage(a.mMessage);
        alert.setmNotificationMsg(a.mNotificationMsg);
        alert.setmEnabled(a.mEnabled);
        alert.setFixed(a.isFixed);
        alert.setmSeverity(a.mSeverity);
        alert.setRef(a.ref);
        alert.setDeviceRef(a.deviceRef);
        alert.setSyncStatus(a.syncStatus);
        alert.setmAlertType(a.mAlertType);
        return alert;
    }
    public static Alert build(AlertDefinition def) {
        Alert alert = new Alert();
        alert.setGuid("");
        alert.setStartTime(GregorianCalendar.getInstance().getTimeInMillis());
        alert.setmTitle(def.alert.mTitle);
        alert.setmMessage(def.alert.mMessage);
        alert.setmNotificationMsg(def.alert.mNotificationMsg);
        alert.setmSeverity(def.alert.mSeverity);
        alert.setmAlertType(def.alert.mAlertType);
        alert.setmEnabled(true);
        alert.setSyncStatus(false);
        return alert;
    }
    
    public static Alert build(AlertDefinition def, String message) {
        Alert alert = new Alert();
        alert.setGuid("");
        alert.setStartTime(GregorianCalendar.getInstance().getTimeInMillis());
        alert.setmTitle(def.alert.mTitle);
        alert.setmMessage(message);
        alert.setmNotificationMsg(message);
        alert.setmSeverity(def.alert.mSeverity);
        alert.setmAlertType(def.alert.mAlertType);
        alert.setDeviceRef(CCUHsApi.getInstance().getCcuRef().toString());
        alert.setmEnabled(true);
        alert.setSyncStatus(false);
        return alert;
    }
    
    public static Alert build(AlertDefinition def, String message, String id) {
        Alert alert = new Alert();
        alert.setGuid("");
        alert.setStartTime(GregorianCalendar.getInstance().getTimeInMillis());
        alert.setmTitle(def.alert.mTitle);
        alert.setmMessage(message);
        alert.setmNotificationMsg(message);
        alert.setmSeverity(def.alert.mSeverity);
        alert.setmAlertType(def.alert.mAlertType);
        alert.setRef(id);
        alert.setDeviceRef(CCUHsApi.getInstance().getCcuRef().toString());
        alert.setmEnabled(true);
        alert.setSyncStatus(false);
        return alert;
    }
    
    
}
