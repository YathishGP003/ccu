package a75f.io.alerts;

import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.Alert;

/**
 * Created by samjithsadasivan on 4/23/18.
 */

public class AlertTest
{
    
    public static final String TEST_ALERTS = "[\n" + "    {\n" + "      \"conditionals\":[\n" + "        {\n" + "          \"key\" : \"Battery\",\n" + "          \"value\" : \"80\",\n" + "          \"condition\" :\"<=\"\n" + "        },\n" + "        {\n" + "          \"key\" : \"Battery\",\n" + "          \"value\" : \"50\",\n" + "          \"condition\" :\">\"\n" + "        },\n" + "        {\n" + "          \"key\" : \"Charging\",\n" + "          \"value\" : \"==\",\n" + "          \"condition\" :\"false\"\n" + "        }\n" + "\n" + "      ],\n" + "      \"offset\": \"0\",\n" + "      \"alert\": {\n" + "        \"mAlertType\": \"BATTERY_LEVEL_WARN\",\n" + "        \"mTitle\": \"Battery level low on CCU [Warn]\",\n" + "        \"mMessage\": \"The battery level of your CCU [%s] has dropped below 75%% and is not charging.Please check that the tablet is secured to it's mount. if it is plugged in, please contact 75F support.\",\n" + "        \"mNotificationMsg\": \"The battery level of your CCU has dropped below 75% and is not charging.Please check that the tablet is secured to it's mount. If it is plugged in, please contact 75F support.\",\n" + "        \"mSeverity\": \"0\",\n" + "        \"mEnabled\": \"true\"\n" + "      }\n" + "    },\n" + "    {\n" + "    \"conditionals\":[\n" + "      {\n" + "        \"key\" : \"Battery\",\n" + "        \"value\" : \"50\",\n" + "        \"condition\" :\"<=\"\n" + "      },\n" + "      {\n" + "        \"key\" : \"Battery\",\n" + "        \"value\" : \"20\",\n" + "        \"condition\" :\">\"\n" + "      },\n" + "      {\n" + "        \"key\" : \"Charging\",\n" + "        \"value\" : \"==\",\n" + "        \"condition\" :\"false\"\n" + "      }\n" + "\n" + "    ],\n" + "    \"offset\": \"0\",\n" + "    \"alert\": {\n" + "      \"mAlertType\": \"BATTERY_LEVEL_ERROR\",\n" + "      \"mTitle\": \"Battery level low on CCU [Error]\",\n" + "      \"mMessage\": \"The battery level of your CCU [%s] has dropped below 50%% and is not charging.Please check that the tablet is secured to it's mount. if it is plugged in, please contact 75F support.\",\n" + "      \"mNotificationMsg\": \"The battery level of your CCU has dropped below 50% and is not charging.Please check that the tablet is secured to it's mount. If it is plugged in, please contact 75F support.\",\n" + "      \"mSeverity\": \"1\",\n" + "      \"mEnabled\": \"true\"\n" + "    }\n" + "    },\n" + "  {\n" + "    \"conditionals\":[\n" + "      {\n" + "        \"key\" : \"Battery\",\n" + "        \"value\" : \"20\",\n" + "        \"condition\" :\"<=\"\n" + "      }\n" + "    ],\n" + "    \"offset\": \"0\",\n" + "    \"alert\": {\n" + "      \"mAlertType\": \"BATTERY_LEVEL_FATAL\",\n" + "      \"mTitle\": \"Battery level low on CCU [Fatal]\",\n" + "      \"mMessage\": \"The battery level of your CCU [%s] has dropped below 20%% and is not charging.Please check that the tablet is secured to it's mount. if it is plugged in, please contact 75F support.\",\n" + "      \"mNotificationMsg\": \"The battery level of your CCU has dropped below 20% and is not charging.Please check that the tablet is secured to it's mount. If it is plugged in, please contact 75F support.\",\n" + "      \"mSeverity\": \"0\",\n" + "      \"mEnabled\": \"true\"\n" + "    }\n" + "  }\n" + "  ]\n" + "\n";
    
    @Test
    public void addAlertDefTest() {
        
        AlertDefinition df = new AlertDefinition();
        
        ArrayList<Conditional> conditionals = new ArrayList<>();
        Conditional c = new Conditional();
        c.key = "X";
        c.value = "10";
        c.condition = ">";
        df.conditionals = conditionals;
        conditionals.add(c);
        
        Alert a = new Alert();
        a.mAlertType = "TEST_ALERT";
        a.mTitle = "Test Alert";
        a.mMessage = "This is a test alert";
        a.mNotificationMsg = "This is a test alert";
        
        
        df.alert = a;
        df.offset = "0";
        
        AlertProcessor p = new AlertProcessor(TEST_ALERTS);
        //m.clearAlertDefinitions();
        p.addAlertDefinition(df);
        //p.clearAlerts();
        
        
        for (AlertDefinition d : p.getAlertDefinitions()) {
            System.out.println(d.alert.mAlertType);
        }
        
    }
    
    @Test
    public void addAlertJsonTest() {
        AlertParser p = new AlertParser();
        ArrayList<AlertDefinition> def = p.parseAlertsString(TEST_ALERTS);
        Assert.assertEquals(3 ,def.size());
        
    }
    
    @Test
    public void generateAlertTest() {
        HashMap<String, Object>  tsData = new HashMap<>();
        tsData.put("Battery",60);
        tsData.put("Charging",false);
        
        System.out.println("Battery = 60");
        AlertManager m = AlertManager.getInstance(null);
        //m.clearAlertDefinitions();
        m.addAlertDefinitions(TEST_ALERTS);
        m.processAlerts(tsData);
        Assert.assertEquals(1 ,m.getAllAlerts().size());
        Assert.assertEquals(1 ,m.getActiveAlerts().size());
        
        
        for (Alert a : m.getAllAlerts()) {
            System.out.println(a.mTitle+" ,fixed :"+a.isFixed);
        }
        tsData.put("Battery",45);
        tsData.put("Charging",false);
    
        System.out.println("Battery = 45");
        
        m.processAlerts(tsData);
        Assert.assertEquals(2 ,m.getAllAlerts().size());
        Assert.assertEquals(1 ,m.getActiveAlerts().size());
        
        for (Alert a : m.getAllAlerts()) {
            System.out.println(a.mTitle+" ,fixed :"+a.isFixed);
        }
    
        tsData.put("Battery",15);
    
        System.out.println("Battery = 15");
        m.processAlerts(tsData);
        Assert.assertEquals(3 ,m.getAllAlerts().size());
        Assert.assertEquals(1 ,m.getActiveAlerts().size());
    
        for (Alert a : m.getAllAlerts()) {
            System.out.println(a.mTitle+" ,fixed :"+a.isFixed);
        }
        
        
    }
    
    @Test
    public void fixAlertTest() {
        HashMap<String, Object>  tsData = new HashMap<>();
        tsData.put("Battery",60);
        tsData.put("Charging",false);
    
        
        AlertManager m = AlertManager.getInstance(null);
        //m.clearAlertDefinitions();
        m.clearAlerts();
        m.addAlertDefinitions(TEST_ALERTS);
        m.processAlerts(tsData);
        
        Assert.assertEquals(1 ,m.getActiveAlerts().size());
    
    
        for (Alert a : m.getAllAlerts()) {
            System.out.println(a.mTitle+" ,fixed :"+a.isFixed);
        }
        tsData.put("Battery",90);
        tsData.put("Charging",true);
        
    
        m.processAlerts(tsData);
        Assert.assertEquals(0 ,m.getActiveAlerts().size());
    }
    
    @Test
    public void addAlertTest() {
        Alert a = new Alert();
        a.mAlertType = "TEST_ALERT_A";
        a.mTitle = "Test Alert";
        a.mMessage = "This is a test alert";
        a.mNotificationMsg = "This is a test alert";
    
        Alert b = new Alert();
        b.mAlertType = "TEST_ALERT_B";
        b.mTitle = "Test Alert";
        b.mMessage = "This is a test alert";
        b.mNotificationMsg = "This is a test alert";
        
        AlertProcessor p = new AlertProcessor(TEST_ALERTS);
        p.addAlert(a);
        p.addAlert(b);
        
        for (Alert a1 : p.getActiveAlerts()) {
            System.out.println(a1.toString());
            
            if (a1.mAlertType.equals("TEST_ALERT_A")) {
                System.out.println(" Fix "+a.toString());
                p.fixAlert(a1);
            }
        }
    
        for (Alert a1 : p.getAllAlerts()) {
            System.out.println(a1.toString());
        }
        
    }
    
    
    /**
     * Alert should be generated after 5 invocation of processAlert(). During app context processAlert
     * will get called every minute. So 5 invocations implies 5 minutes.
     */
    @Test
    public void alertOffsetTest() {
        AlertDefinition df = new AlertDefinition();
    
        ArrayList<Conditional> conditionals = new ArrayList<>();
        Conditional c = new Conditional();
        c.key = "X";
        c.value = "10";
        c.condition = ">";
        df.conditionals = conditionals;
        conditionals.add(c);
    
        Alert a = new Alert();
        a.mAlertType = "TEST_ALERT";
        a.mTitle = "Test Alert";
        a.mMessage = "This is a test alert";
        a.mNotificationMsg = "This is a test alert";
    
    
        df.alert = a;
        df.offset = "5";
    
        AlertManager m = AlertManager.getInstance(null);
        m.addAlertDefinition(df);
        m.clearAlerts();
    
        HashMap<String, Object>  tsData = new HashMap<>();
        tsData.put("X",100);
    
        m.processAlerts(tsData);
        Assert.assertEquals(0, m.getAllAlerts().size()); // Minute -1
        m.processAlerts(tsData);
        Assert.assertEquals(0, m.getAllAlerts().size()); // Minute -2
        m.processAlerts(tsData);
        Assert.assertEquals(0, m.getAllAlerts().size()); // Minute -3
        m.processAlerts(tsData);
        Assert.assertEquals(0, m.getAllAlerts().size()); // Minute -4
        m.processAlerts(tsData);
        Assert.assertEquals(1, m.getAllAlerts().size()); // Minute -5
        
        
    }
    
    
}
