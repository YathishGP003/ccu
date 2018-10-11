package a75f.io.alerts;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import junit.framework.Assert;

import org.junit.Test;

import java.util.HashMap;

/**
 * Created by samjithsadasivan on 4/30/18.
 */

public class AlertInstrumentationTest
{
    
    @Test
    public void generateAlertTest() {
        Context appContext = InstrumentationRegistry.getTargetContext();
    
        HashMap<String, Object> tsData = new HashMap<>();
        tsData.put("Battery",60);
        tsData.put("Charging",false);
    
        System.out.println("Battery = 60");
        
        AlertManager m = AlertManager.getInstance();
        m.init(appContext);
        m.processAlerts(tsData);
        
        Assert.assertEquals(m.getAllAlerts().size() ,1);
        Assert.assertEquals(m.getActiveAlerts().size() ,1);
        for (Alert a : m.getAllAlerts()) {
            System.out.println(a.getTitle()+" ,fixed :"+a.isFixed);
        }
    
        tsData.put("Battery",45);
        tsData.put("Charging",false);
    
        System.out.println("Battery = 45");
    
        m.processAlerts(tsData);
        Assert.assertEquals(m.getAllAlerts().size() ,2);
        Assert.assertEquals(m.getActiveAlerts().size() ,1);
        for (Alert a : m.getAllAlerts()) {
            System.out.println(a.getTitle()+" ,fixed :"+a.isFixed);
        }
    
        tsData.put("Battery",15);
    
        System.out.println("Battery = 15");
        m.processAlerts(tsData);
        Assert.assertEquals(m.getAllAlerts().size() ,3);
        Assert.assertEquals(m.getActiveAlerts().size() ,1);
    
        for (Alert a : m.getAllAlerts()) {
            System.out.println(a.getTitle()+" ,fixed :"+a.isFixed);
        }
    }
}
