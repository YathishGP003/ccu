package a75f.io.alerts;

import android.content.Context;

import java.util.List;
import java.util.Map;

import a75f.io.api.haystack.Alert;
/**
 * Created by samjithsadasivan on 4/24/18.
 */

/**
 * AlertManager provides APIs to process and access alerts.
 * It is implemented as a singleton and modules using it should pass Context via init method before calling other apis.
 * Then invoke processAlerts passing a map of time-series keys and values.
 *
 * Sample usage
 * --------------
 *  AlertManager m = AlertManager.getInstance();
 *  m.init(appContext);
 *  m.processAlerts(tsMap);
 *
 */

public class AlertManager
{
    private Context mContext;
    private static AlertManager mInstance;
    
    boolean alertsDefinied;
    
    AlertProcessor processor;
    
    private AlertManager(Context c) {
        processor = new AlertProcessor(c);
    }
    
    public static AlertManager getInstance(Context c) {
        if (mInstance == null) {
            mInstance = new AlertManager(c);
        }
        return mInstance;
    }
    
    public void init(Context c) {
        mContext = c;
        readAlertDefinitions();
    }
    
    /**
     * Add a new alert definition at runtime
     */
    public void addAlertDefinition(AlertDefinition aDef) {
        alertsDefinied = true;
        //processor.alertDefinitions.add(aDef);
    }
    
    /**
     * Add list of json alert definitions as string
     */
    public void addAlertDefinitions(String alerts) {
        alertsDefinied = true;
        //processor.updateAlertDefinitions(alerts);
        processor.clearAlerts();
    }
    
    /**
     * Read alert definitions from local assets folder.
     * This is implicitly done if init is called.
     */
    public void readAlertDefinitions() {
        alertsDefinied = true;
        //processor.updateAlertDefinitions(mContext);
        processor.clearAlerts();
    
    }
    
    public List<Alert> processAlerts(Map<String,Object> timeSeriesMap) {
        if (alertsDefinied != true) {
            throw new IllegalStateException("AlertManager not initialized");
        }
        processor.runProcess(timeSeriesMap);
        return processor.getAllAlerts();
    }
    
    public void processAlerts() {
        processor.runProcess();
    }
    
    public List<Alert> getActiveAlerts() {
        return processor.getActiveAlerts();
    }
    
    public List<Alert> getAllAlerts() {
        return processor.getAllAlerts();
    }
    
    public void clearAlerts() {
        processor.clearAlerts();
    }
    
    /*public void clearAlertDefinitions() {
        processor.alertDefinitions.clear();
    }*/
    
    public List<AlertDefinition> getAlertDefinitions() {
        return processor.getAlertDefinitions();
    }
}
