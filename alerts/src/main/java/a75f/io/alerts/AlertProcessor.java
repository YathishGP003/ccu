package a75f.io.alerts;

import android.content.Context;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by samjithsadasivan on 4/24/18.
 */

public class AlertProcessor
{
    
    ArrayList<Alert> alertList;
    ArrayList<Alert> activeAlertList;
    ArrayList<AlertDefinition> alertDefinitions;
    
    AlertParser parser;
    
    AlertProcessor() {
        
        alertList = new ArrayList<>();
        activeAlertList = new ArrayList<>();
        alertDefinitions = new ArrayList<>();
        parser = new AlertParser();
    }
    
    public void runProcess(Map<String,Object> tsData) {
        
        for (AlertDefinition def : alertDefinitions) {
            
            if (def.evaluate(tsData)) {
                if (Integer.parseInt(def.offset.trim()) == 0) {
                    addAlert(AlertBuilder.build(def));
                }else if (Integer.parseInt(def.offset.trim()) <= ++def.offsetCount ) {
                    def.offsetCount = 0;
                    addAlert(AlertBuilder.build(def));
                }
            } else {
                def.offsetCount = 0;
                fixAlert(def.alert.getAlertType());
            }
        }
    
    }
    
    public ArrayList<Alert> getAllDefinedAlerts(){
        ArrayList<Alert> definedAlerts = new ArrayList<>();
        for(AlertDefinition d : alertDefinitions) {
            definedAlerts.add(d.alert);
        }
        return definedAlerts;
    }
    
    public ArrayList<Alert> getActiveAlerts(){
        return activeAlertList;
    }
    
    public ArrayList<Alert> getAllAlerts(){
        return alertList;
    }
    
    public void addAlert(Alert alert) {
        
        for (Alert a: activeAlertList) {
            if (a.getAlertType().equals(alert.getAlertType())) {
                a.alertCount++;
                return;
            }
        }
        
        alertList.add(alert);
        activeAlertList.add(alert);
    }
    
    public void fixAlert(String type) {
        Iterator<Alert> activeAlerts = activeAlertList.iterator();
        while (activeAlerts.hasNext()) {
            Alert al = activeAlerts.next();
            if (al.getAlertType().equals(type)) {
                al.isFixed = true;
                al.setEndTime(GregorianCalendar.getInstance());
                activeAlerts.remove();
                break;
            }
        }
    }
    
    public void updateAlertDefinitions(Context c) {
        alertDefinitions = parser.parseAllAlerts(c);
    }
    
    public void updateAlertDefinitions(String alerts) {
        if (alertDefinitions.size() == 0)
        {
            alertDefinitions = parser.parseAlertsString(alerts);
        } else {
            for (AlertDefinition def :parser.parseAlertsString(alerts)) {
                alertDefinitions.add(def);
            }
        }
    }
    
    public void updateAlertDefinitions(AlertDefinition d) {
        alertDefinitions.add(d);
    }
    
    public void clearAlerts() {
        alertList.clear();
        activeAlertList.clear();
    }
}
