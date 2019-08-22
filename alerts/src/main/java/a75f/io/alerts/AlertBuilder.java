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
        alert.setAlertId(a.alertId);
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
        alert.setSiteRef(a.siteRef);
        return alert;
    }
    public static Alert build(AlertDefinition def) {
        Alert alert = new Alert();
        alert.setAlertId("");
        alert.setStartTime(GregorianCalendar.getInstance().getTimeInMillis());
        alert.setmTitle(def.alert.mTitle);
        alert.setmMessage(def.alert.mMessage);
        alert.setmNotificationMsg(def.alert.mNotificationMsg);
        alert.setmSeverity(def.alert.mSeverity);
        return alert;
    }
    
    public static Alert build(AlertDefinition def, String message, String id) {
        Alert alert = new Alert();
        alert.setAlertId("");
        alert.setStartTime(GregorianCalendar.getInstance().getTimeInMillis());
        alert.setmTitle(def.alert.mTitle);
        alert.setmMessage(message);
        alert.setmNotificationMsg(message);
        alert.setmSeverity(def.alert.mSeverity);
        alert.ref = id;
        alert.siteRef = CCUHsApi.getInstance().getSiteId().toString();
        alert.deviceRef = CCUHsApi.getInstance().getCcuId().toString();
        return alert;
    }
    
    
}
