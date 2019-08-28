package a75f.io.alerts;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.Assert;

import org.junit.Test;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HHisItem;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.client.HClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TimeZone;

import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Site;

/**
 * Created by samjithsadasivan on 4/23/18.
 */

public class AlertTest
{
    
    public static final String TEST_ALERTS = "[\n" + "    {\n" + "      \"conditionals\":[\n" + "        {\n" + "          \"key\" : \"Battery\",\n" + "          \"value\" : \"80\",\n" + "          \"condition\" :\"<=\"\n" + "        },\n" + "        {\n" + "          \"key\" : \"Battery\",\n" + "          \"value\" : \"50\",\n" + "          \"condition\" :\">\"\n" + "        },\n" + "        {\n" + "          \"key\" : \"Charging\",\n" + "          \"value\" : \"==\",\n" + "          \"condition\" :\"false\"\n" + "        }\n" + "\n" + "      ],\n" + "      \"offset\": \"0\",\n" + "      \"alert\": {\n" + "        \"mAlertType\": \"BATTERY_LEVEL_WARN\",\n" + "        \"mTitle\": \"Battery level low on CCU [Warn]\",\n" + "        \"mMessage\": \"The battery level of your CCU [%s] has dropped below 75%% and is not charging.Please check that the tablet is secured to it's mount. if it is plugged in, please contact 75F support.\",\n" + "        \"mNotificationMsg\": \"The battery level of your CCU has dropped below 75% and is not charging.Please check that the tablet is secured to it's mount. If it is plugged in, please contact 75F support.\",\n" + "        \"mSeverity\": \"0\",\n" + "        \"mEnabled\": \"true\"\n" + "      }\n" + "    },\n" + "    {\n" + "    \"conditionals\":[\n" + "      {\n" + "        \"key\" : \"Battery\",\n" + "        \"value\" : \"50\",\n" + "        \"condition\" :\"<=\"\n" + "      },\n" + "      {\n" + "        \"key\" : \"Battery\",\n" + "        \"value\" : \"20\",\n" + "        \"condition\" :\">\"\n" + "      },\n" + "      {\n" + "        \"key\" : \"Charging\",\n" + "        \"value\" : \"==\",\n" + "        \"condition\" :\"false\"\n" + "      }\n" + "\n" + "    ],\n" + "    \"offset\": \"0\",\n" + "    \"alert\": {\n" + "      \"mAlertType\": \"BATTERY_LEVEL_ERROR\",\n" + "      \"mTitle\": \"Battery level low on CCU [Error]\",\n" + "      \"mMessage\": \"The battery level of your CCU [%s] has dropped below 50%% and is not charging.Please check that the tablet is secured to it's mount. if it is plugged in, please contact 75F support.\",\n" + "      \"mNotificationMsg\": \"The battery level of your CCU has dropped below 50% and is not charging.Please check that the tablet is secured to it's mount. If it is plugged in, please contact 75F support.\",\n" + "      \"mSeverity\": \"1\",\n" + "      \"mEnabled\": \"true\"\n" + "    }\n" + "    },\n" + "  {\n" + "    \"conditionals\":[\n" + "      {\n" + "        \"key\" : \"Battery\",\n" + "        \"value\" : \"20\",\n" + "        \"condition\" :\"<=\"\n" + "      }\n" + "    ],\n" + "    \"offset\": \"0\",\n" + "    \"alert\": {\n" + "      \"mAlertType\": \"BATTERY_LEVEL_FATAL\",\n" + "      \"mTitle\": \"Battery level low on CCU [Fatal]\",\n" + "      \"mMessage\": \"The battery level of your CCU [%s] has dropped below 20%% and is not charging.Please check that the tablet is secured to it's mount. if it is plugged in, please contact 75F support.\",\n" + "      \"mNotificationMsg\": \"The battery level of your CCU has dropped below 20% and is not charging.Please check that the tablet is secured to it's mount. If it is plugged in, please contact 75F support.\",\n" + "      \"mSeverity\": \"0\",\n" + "      \"mEnabled\": \"true\"\n" + "    }\n" + "  }\n" + "  ]\n" + "\n";
    
    
    public static final String TEST_ALERTS_NEW = "[{\n" + "    \"conditionals\":[\n" + "      {\n" + "        \"order\" : \"1\",\n" + "        \"key\" : \"zone and status and his\",\n" + "        \"value\" : \"3\",\n" + "        \"condition\" :\"==\"\n" + "      },\n" + "      {\n" + "        \"order\" : \"2\",\n" + "        \"operator\" : \"&&\"\n" + "      },\n" + "      {\n" + "        \"order\" : \"3\",\n" + "        \"key\" : \"current and temp\",\n" + "        \"value\" : \"80\",\n" + "        \"condition\": \"==\"\n" + "      },\n" + "      {\n" + "        \"order\" : \"4\",\n" + "        \"operator\" : \"||\"\n" + "      },\n" + "      {\n" + "        \"order\" : \"5\",\n" + "        \"key\" : \"desired and temp and cooling\",\n" + "        \"value\" : \"80\",\n" + "        \"condition\": \"==\"\n" + "      }\n" + "\n" + "    ],\n" + "    \"offset\": \"0\",\n" + "    \"alert\": {\n" + "      \"mAlertType\": \"ZONE_TEMP_DEAD\",\n" + "      \"mTitle\": \"Zone Temperature Dead\",\n" + "      \"mMessage\": \"Equip #equipname3 is reporting a temperature of #pointval3 outside the defined building limits\",\n" + "      \"mNotificationMsg\": \"Equip %s is reporting a temperature of %s outside the defined building limits\",\n" + "      \"mSeverity\": \"0\",\n" + "      \"mEnabled\": \"true\"\n" + "    }\n" + "  }]\n";
    
    public static final String PROCESS_TEST_ALERT = "[{\n" + "        \"siteRef\" : \"5d5c131b07dba3262fb48505\",\n" + "        \"custom\" : true,\n" + "        \"conditionals\":[\n" + "          {\n" + "            \"order\" : \"1\",\n" + "            \"key\" : \"current and temp and his\",\n" + "            \"grpOperation\" : \"\",\n" + "            \"condition\" :\">\",\n" + "            \"value\" : \"80\"\n" + "          }\n" + "        ],\n" + "        \"offset\": \"0\",\n" + "        \"alert\": {\n" + "          \"mTitle\": \"Temperature breach detected HHHHHOOO\",\n" + "          \"mMessage\": \"Equip #equipname1 is reporting a temperature of #pointval1 which is greater than  #condval1\",\n" + "          \"mNotificationMsg\": \"Equip #equipname1 is reporting a temperature of #pointval1 which is greater than  #condval1\",\n" + "          \"mSeverity\": \"WARN\",\n" + "          \"mEnabled\": \"true\"\n" + "        }\n" + "      }]";
    public static final String PROCESS_TEST_ALERT_OFFSET = "[\n" + "    {\n" + "      \"conditionals\":[\n" + "        {\n" + "          \"order\" : \"1\",\n" + "          \"key\" : \"zone and current and temp and his\",\n" + "          \"value\" : \"80\",\n" + "          \"condition\" :\">\"\n" + "        },\n" + "        {\n" + "          \"order\" : \"2\",\n" + "          \"operator\" : \"&&\"\n" + "        },\n" + "        {\n" + "          \"order\" : \"3\",\n" + "          \"key\" : \"zone and current and temp and his\",\n" + "          \"value\" : \"100\",\n" + "          \"condition\": \"<\"\n" + "        }\n" + "      ],\n" + "      \"offset\": \"5\",\n" + "      \"alert\": {\n" + "\n" + "        \"mTitle\": \"Temperature breach detected\",\n" + "        \"mMessage\": \"Equip #equipname1 is reporting a temperature of #pointval1 which is greater than  #condval1 and less than #condval3\",\n" + "        \"mNotificationMsg\": \"Equip #equipname1 is reporting a temperature of #pointval1 which is greater than  #condval1 and less than #condval3\",\n" + "        \"mSeverity\": \"WARN\",\n" + "        \"mEnabled\": \"true\"\n" + "      }\n" + "    }\n" + "  ]";
    
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
            System.out.println(d.alert.mMessage);
        }
        
    }
    
    @Test
    public void alertDefTestNew() {
        AlertProcessor p = new AlertProcessor(TEST_ALERTS_NEW);
        
        ArrayList<AlertDefinition> pl = p.predefinedAlerts;
        for (AlertDefinition d : pl) {
            Collections.sort(d.conditionals, new ConditionalComparator());
            for (Conditional c: d.conditionals)
            {
                System.out.println(c.key+" "+c.order);
            }
            
        }
        
    }
    
    class ConditionalComparator implements Comparator<Conditional>
    {
        @Override
        public int compare(Conditional a, Conditional b) {
            return a.order - b.order;
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
        a.mTitle = "Test Alert A";
        a.mMessage = "This is a test alert";
        a.mNotificationMsg = "This is a test alert";
    
        Alert b = new Alert();
        b.mTitle = "Test Alert B";
        b.mMessage = "This is a test alert";
        b.mNotificationMsg = "This is a test alert";
        
        AlertProcessor p = new AlertProcessor(TEST_ALERTS);
        p.addAlert(a);
        p.addAlert(b);
        
        for (Alert a1 : p.getActiveAlerts()) {
            System.out.println(a1.toString());
            
            if (a1.mMessage.equals(b.mMessage)) {
                System.out.println(" Fix "+a.toString());
                p.fixAlert(a1);
            }
        }
    
        for (Alert a1 : p.getAllAlerts()) {
            System.out.println(a1.toString());
        }
        
    }
    
    @Test
    public void testAlertProcess() {
        CCUHsApi hayStack = new CCUHsApi();
        int nodeAddr = 7000;
    
        String tz = TimeZone.getDefault().getID().substring(TimeZone.getDefault().getID().lastIndexOf("/") + 1);
        Site s = new Site.Builder()
                         .setDisplayName("Test")
                         .addMarker("site")
                         .setGeoCity("Burnsville")
                         .setGeoState("MN")
                         .setTz(tz)
                         .setArea(1000).build();
        String siteRef = hayStack.addSite(s);
        Equip v = new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName("Test-VAV-"+nodeAddr)
                          .setRoomRef("room")
                          .setFloorRef("floor")
                          .addMarker("equip")
                          .addMarker("vav")
                          .addMarker("zone")
                          .setGroup(String.valueOf(nodeAddr))
                          .build();
        String equipRef = hayStack.addEquip(v);
    
        Point testPoint = new Point.Builder()
                                  .setDisplayName("Test AHU-"+nodeAddr+"-TestTemp")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef("room")
                                  .setFloorRef("floor")
                                  .addMarker("current")
                                  .addMarker("zone")
                                  .addMarker("temp").addMarker("air").addMarker("sensor").addMarker("his")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setTz(tz)
                                  .setUnit("\u00B0F")
                                  .build();
    
        String ctId = CCUHsApi.getInstance().addPoint(testPoint);
        HClient hsClient = hayStack.getHSClient();
        ArrayList<HHisItem> hislist = new ArrayList<>();
        HHisItem[] hisArray = new HHisItem[3];
        long now = System.currentTimeMillis();
        hisArray[0] = HHisItem.make(HDateTime.make(now), HNum.make(90));
        hislist.add(hisArray[0]);
    
        hsClient.hisWrite(HRef.copy(ctId),hislist.toArray(new HHisItem[hislist.size()]));
    
    
        AlertProcessor p = new AlertProcessor(PROCESS_TEST_ALERT);
        p.processAlerts();
        Assert.assertEquals(1, p.getActiveAlerts().size());
        for (Alert al : p.getActiveAlerts()) {
            System.out.println(" Active Alert "+al.toString());
        }
    
        for (Alert al : p.getAllAlerts()) {
            System.out.println(" All Alert "+al.toString());
        }
    
    }
    
    @Test
    public void testAlertProcessOffset() {
        CCUHsApi hayStack = new CCUHsApi();
        int nodeAddr = 7000;
        
        String tz = TimeZone.getDefault().getID().substring(TimeZone.getDefault().getID().lastIndexOf("/") + 1);
        Site s = new Site.Builder()
                         .setDisplayName("Test")
                         .addMarker("site")
                         .setGeoCity("Burnsville")
                         .setGeoState("MN")
                         .setTz(tz)
                         .setArea(1000).build();
        String siteRef = hayStack.addSite(s);
        Equip v = new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName("Test-VAV-"+nodeAddr)
                          .setRoomRef("room")
                          .setFloorRef("floor")
                          .addMarker("equip")
                          .addMarker("vav")
                          .addMarker("zone")
                          .setGroup(String.valueOf(nodeAddr))
                          .build();
        String equipRef = hayStack.addEquip(v);
        
        Point testPoint = new Point.Builder()
                                  .setDisplayName("Test AHU-"+nodeAddr+"-TestTemp")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef("room")
                                  .setFloorRef("floor")
                                  .addMarker("current")
                                  .addMarker("zone")
                                  .addMarker("temp").addMarker("air").addMarker("sensor").addMarker("his")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setTz(tz)
                                  .setUnit("\u00B0F")
                                  .build();
        
        String ctId = CCUHsApi.getInstance().addPoint(testPoint);
        HClient hsClient = hayStack.getHSClient();
        ArrayList<HHisItem> hislist = new ArrayList<>();
        HHisItem[] hisArray = new HHisItem[3];
        long now = System.currentTimeMillis();
        hisArray[0] = HHisItem.make(HDateTime.make(now), HNum.make(90));
        hislist.add(hisArray[0]);
        
        hsClient.hisWrite(HRef.copy(ctId),hislist.toArray(new HHisItem[hislist.size()]));
        
        
        AlertProcessor p = new AlertProcessor(PROCESS_TEST_ALERT_OFFSET);
        p.processAlerts();
        Assert.assertEquals(0, p.getActiveAlerts().size());
        for (Alert al : p.getActiveAlerts()) {
            System.out.println(" Active Alert "+al.toString());
        }
        p.processAlerts();
        Assert.assertEquals(0, p.getActiveAlerts().size());
        for (Alert al : p.getActiveAlerts()) {
            System.out.println(" Active Alert "+al.toString());
        }
        p.processAlerts();
        Assert.assertEquals(0, p.getActiveAlerts().size());
        for (Alert al : p.getActiveAlerts()) {
            System.out.println(" Active Alert "+al.toString());
        }
    
        p.processAlerts();
        Assert.assertEquals(0, p.getActiveAlerts().size());
        for (Alert al : p.getActiveAlerts()) {
            System.out.println(" Active Alert "+al.toString());
        }
    
        p.processAlerts();
        Assert.assertEquals(0, p.getActiveAlerts().size());
        for (Alert al : p.getActiveAlerts()) {
            System.out.println(" Active Alert "+al.toString());
        }
    
        p.processAlerts();
        Assert.assertEquals(1, p.getActiveAlerts().size());
        for (Alert al : p.getActiveAlerts()) {
            System.out.println(" Active Alert "+al.toString());
        }
    
        p.processAlerts();
        Assert.assertEquals(1, p.getActiveAlerts().size());
        for (Alert al : p.getActiveAlerts()) {
            System.out.println(" Active Alert "+al.toString());
        }
        
        for (Alert al : p.getAllAlerts()) {
            System.out.println(" All Alert "+al.toString());
        }
        
    }
    
    
    @Test
    public void testAlertDefParser() {
        String alerts = "[{\"_id\": \"5d5c67866a00740025ed905c\", \"siteRef\": \"5d5c131b07dba3262fb48505\", \"conditionals\": [{\"order\": \"1\", \"key\": \"zone and current and temp and his\", \"value\": \"80\", \"condition\": \">\"}, {\"order\": \"2\", \"operator\": \"&&\"}, {\"order\": \"3\", \"key\": \"zone and current and temp and his\", \"value\": \"100\", \"condition\": \"<\"}], \"offset\": \"0\", \"alert\": {\"mTitle\": \"Temperature breach detected\", \"mMessage\": \"Equip #equipname1 is reporting a temperature of #pointval1 which is greater than  #condval1 and less than #condval3\", \"mNotificationMsg\": \"Equip #equipname1 is reporting a temperature of #pointval1 which is greater than  #condval1 and less than #condval3\", \"mSeverity\": \"WARN\", \"mEnabled\": \"true\"}, \"type\": \"AlertDefinition\"}]";
    
        /*try
        {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
            String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(alerts);
            System.out.println(jsonString);
            
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    */
        AlertProcessor p = new AlertProcessor(TEST_ALERTS_NEW);
    
        try
        {
            System.out.println(alerts);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            AlertDefinition[] pojos = objectMapper.readValue(alerts, AlertDefinition[].class);
            ArrayList<AlertDefinition> alertList = new ArrayList<>(Arrays.asList(pojos));
            p.updateCustomAlertDefinitions(alertList);
        }catch (Exception e) {
            e.printStackTrace();
        }
        
        
    }
}
