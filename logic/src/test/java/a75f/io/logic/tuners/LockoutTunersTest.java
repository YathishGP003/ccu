package a75f.io.logic.tuners;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.mock.MockCcuHsApi;

public class LockoutTunersTest {
    
    CCUHsApi hayStack;
    
    @Before
    public void setUp() {
        hayStack = new MockCcuHsApi();
    }
    
    @After
    public void tearDown() throws Exception {
        hayStack.tagsDb.getBoxStore().close();
    }
    
    @Test
    public void testDefaultDabCoolingLockoutPoint() {
    
        SystemTuners.createCoolingTempLockoutPoint(hayStack, "siteRef", "equipRef", "equipDis",
                                                   "Chicago", Tags.DAB, true);
        
        HashMap<Object, Object> coolingLockoutPoint = hayStack.readEntity("cooling and lockout and dab and default");
        Assert.assertFalse(coolingLockoutPoint.isEmpty());
        Assert.assertEquals(hayStack.readHisValById(coolingLockoutPoint.get("id").toString()).doubleValue(),
                          TunerConstants.OUTSIDE_TEMP_COOLING_LOCKOUT_DEFAULT, 0);
        double val = hayStack.readPointPriorityVal(coolingLockoutPoint.get("id").toString());
        
        Assert.assertEquals(val, TunerConstants.OUTSIDE_TEMP_COOLING_LOCKOUT_DEFAULT, 0);
    }
    
    @Test
    public void testSystemDabCoolingLockoutPoint() {
    
        SystemTuners.createCoolingTempLockoutPoint(hayStack, "siteRef", "equipRef", "equipDis",
                                                   "Chicago", Tags.DAB, true);
        SystemTuners.createCoolingTempLockoutPoint(hayStack, "siteRef", "equipRef", "equipDis",
                                                   "Chicago", Tags.DAB, false);
        
        HashMap<Object, Object> coolingLockoutPoint = hayStack.readEntity("cooling and lockout and dab and system");
        Assert.assertFalse(coolingLockoutPoint.isEmpty());
        Assert.assertEquals(hayStack.readHisValById(coolingLockoutPoint.get("id").toString()).doubleValue(),
                          TunerConstants.OUTSIDE_TEMP_COOLING_LOCKOUT_DEFAULT, 0);
        double val = hayStack.readPointPriorityVal(coolingLockoutPoint.get("id").toString());
        
        Assert.assertTrue(val == TunerConstants.OUTSIDE_TEMP_COOLING_LOCKOUT_DEFAULT);
    }
    
    @Test
    public void testDefaultVavCoolingLockoutPoint() {
        
        SystemTuners.createCoolingTempLockoutPoint(hayStack, "siteRef", "equipRef", "equipDis",
                                                   "Chicago", Tags.DAB, true);
        
        HashMap<Object, Object> coolingLockoutPoint = hayStack.readEntity("cooling and lockout and dab and default");
        Assert.assertFalse(coolingLockoutPoint.isEmpty());
        Assert.assertEquals(hayStack.readHisValById(coolingLockoutPoint.get("id").toString()).doubleValue(),
                          TunerConstants.OUTSIDE_TEMP_COOLING_LOCKOUT_DEFAULT, 0);
        double val = hayStack.readPointPriorityVal(coolingLockoutPoint.get("id").toString());
        
        Assert.assertEquals(val, TunerConstants.OUTSIDE_TEMP_COOLING_LOCKOUT_DEFAULT, 0);
    }
    
    @Test
    public void testSystemVavCoolingLockoutPoint() {
    
        SystemTuners.createCoolingTempLockoutPoint(hayStack, "siteRef", "equipRef", "equipDis",
                                                   "Chicago", Tags.VAV, true);
        SystemTuners.createCoolingTempLockoutPoint(hayStack, "siteRef", "equipRef", "equipDis",
                                                   "Chicago", Tags.VAV, false);
        
        HashMap<Object, Object> coolingLockoutPoint = hayStack.readEntity("cooling and lockout and vav and system");
        Assert.assertFalse(coolingLockoutPoint.isEmpty());
        Assert.assertEquals(hayStack.readHisValById(coolingLockoutPoint.get("id").toString()).doubleValue(),
                          TunerConstants.OUTSIDE_TEMP_COOLING_LOCKOUT_DEFAULT, 0);
        double val = hayStack.readPointPriorityVal(coolingLockoutPoint.get("id").toString());
        
        Assert.assertEquals(val, TunerConstants.OUTSIDE_TEMP_COOLING_LOCKOUT_DEFAULT, 0);
    }
    
    @Test
    public void testDefaultDabHeatingLockoutPoint() {
        
        SystemTuners.createHeatingTempLockoutPoint(hayStack, "siteRef", "equipRef", "equipDis",
                                                   "Chicago", Tags.DAB, true);
        
        HashMap<Object, Object> heatingLockoutPoint = hayStack.readEntity("heating and lockout and dab and default");
        Assert.assertFalse(heatingLockoutPoint.isEmpty());
        Assert.assertEquals(hayStack.readHisValById(heatingLockoutPoint.get("id").toString()).doubleValue(),
                          TunerConstants.OUTSIDE_TEMP_HEATING_LOCKOUT_DEFAULT, 0);
        double val = hayStack.readPointPriorityVal(heatingLockoutPoint.get("id").toString());
        
        Assert.assertEquals(val, TunerConstants.OUTSIDE_TEMP_HEATING_LOCKOUT_DEFAULT, 0);
    }
    
    @Test
    public void testDefaultVavHeatingLockoutPoint() {
        
        SystemTuners.createHeatingTempLockoutPoint(hayStack, "siteRef", "equipRef", "equipDis",
                                                   "Chicago", Tags.VAV, true);
        
        HashMap<Object, Object> heatingLockoutPoint = hayStack.readEntity("heating and lockout and vav and default");
        Assert.assertFalse(heatingLockoutPoint.isEmpty());
        Assert.assertEquals(hayStack.readHisValById(heatingLockoutPoint.get("id").toString()).doubleValue(),
                          TunerConstants.OUTSIDE_TEMP_HEATING_LOCKOUT_DEFAULT);
        double val = hayStack.readPointPriorityVal(heatingLockoutPoint.get("id").toString());
        
        Assert.assertTrue(val == TunerConstants.OUTSIDE_TEMP_HEATING_LOCKOUT_DEFAULT);
    }
    
    @Test
    public void testSystemDabHeatingLockoutPoint() {
    
        SystemTuners.createHeatingTempLockoutPoint(hayStack, "siteRef", "equipRef", "equipDis",
                                                   "Chicago", Tags.DAB, true);
        SystemTuners.createHeatingTempLockoutPoint(hayStack, "siteRef", "equipRef", "equipDis",
                                                   "Chicago", Tags.DAB, false);
        
        HashMap<Object, Object> heatingLockoutPoint = hayStack.readEntity("heating and lockout and dab and system");
        Assert.assertFalse(heatingLockoutPoint.isEmpty());
        Assert.assertEquals(hayStack.readHisValById(heatingLockoutPoint.get("id").toString()).doubleValue(),
                          TunerConstants.OUTSIDE_TEMP_HEATING_LOCKOUT_DEFAULT, 0);
        double val = hayStack.readPointPriorityVal(heatingLockoutPoint.get("id").toString());
        
        Assert.assertEquals(val, TunerConstants.OUTSIDE_TEMP_HEATING_LOCKOUT_DEFAULT, 0);
    }
    
    @Test
    public void testSystemVavHeatingLockoutPoint() {
    
        SystemTuners.createHeatingTempLockoutPoint(hayStack, "siteRef", "equipRef", "equipDis",
                                                   "Chicago", Tags.VAV, true);
        SystemTuners.createHeatingTempLockoutPoint(hayStack, "siteRef", "equipRef", "equipDis",
                                                   "Chicago", Tags.VAV, false);
        
        HashMap<Object, Object> heatingLockoutPoint = hayStack.readEntity("heating and lockout and vav and default");
        Assert.assertFalse(heatingLockoutPoint.isEmpty());
        Assert.assertEquals(hayStack.readHisValById(heatingLockoutPoint.get("id").toString()).doubleValue(),
                          TunerConstants.OUTSIDE_TEMP_HEATING_LOCKOUT_DEFAULT, 0);
        double val = hayStack.readPointPriorityVal(heatingLockoutPoint.get("id").toString());
        
        Assert.assertEquals(val, TunerConstants.OUTSIDE_TEMP_HEATING_LOCKOUT_DEFAULT, 0);
    }
}

