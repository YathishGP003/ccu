package a75f.io.alerts;

import java.util.GregorianCalendar;

import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;

/**
 * Created by samjithsadasivan on 4/25/18.
 */

public class AlertBuilder
{
    
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
