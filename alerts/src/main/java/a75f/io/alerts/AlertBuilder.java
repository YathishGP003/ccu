package a75f.io.alerts;

import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * Created by samjithsadasivan on 4/25/18.
 */

public class AlertBuilder
{
    
    public static Alert build(AlertDefinition def) {
        Alert alert = new Alert();
        alert.alertID = UUID.randomUUID();
        alert.setStartTime(GregorianCalendar.getInstance());
        alert.setTitle(def.alert.getTitle());
        alert.setAlertType(def.alert.getAlertType());
        alert.setMessage(def.alert.getMessage());
        alert.setNotificationMsg(def.alert.getNotificationMsg());
        alert.setSeverity(def.alert.getSeverity());
        alert.setEmails(def.alert.emails);
        alert.setSms(def.alert.sms);
        alert.setSms(def.alert.pushNotifications);
        return alert;
    }
    
    
}
