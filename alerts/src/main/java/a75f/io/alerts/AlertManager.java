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

    public static AlertManager getInstance()
    {
        if (mInstance == null)
        {
            throw new IllegalStateException("No instance found");
        }
        return mInstance;
    }
    
    public void init(Context c) {
        mContext = c;
        readAlertDefinitions();
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
        //processor.runProcess(timeSeriesMap);
        return processor.getAllAlerts();
    }
    
    public void processAlerts() {
        processor.processAlerts();
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
    
    public void addAlertDefinition(List<AlertDefinition> list) {
        processor.addAlertDefinition(list);
    }
    
    public void fixAlert(Alert a) {
        processor.fixAlert(a);
    }
    public void deleteAlert(Alert a) {
        processor.deleteAlert(a);
    }
    
    public void deleteAlert(String id) {
        processor.deleteAlert(id);
    }
    public void addAlertDefinition(AlertDefinition alert) {
        processor.addAlertDefinition(alert);
    }
    public void deleteAlertDefinition(String _id) {
        processor.deleteCustomAlertDefinition(_id);
    }

    public Context getApplicationContext() {
        return mContext;
    }

    public void setApplicationContext(Context mApplicationContext) {
        if (this.mContext == null) {
            this.mContext = mApplicationContext;
        }
    }
    
}

