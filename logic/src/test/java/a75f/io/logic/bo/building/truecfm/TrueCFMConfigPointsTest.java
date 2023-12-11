package a75f.io.logic.bo.building.truecfm;



import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.mock.MockCcuHsApi;


public class TrueCFMConfigPointsTest {
    CCUHsApi hayStack;
    Equip equip;
    VavProfileConfiguration vavProfileConfiguration;


    @Before
    public void setUp() {
        hayStack = new MockCcuHsApi();
        equip=new Equip();
        vavProfileConfiguration=new VavProfileConfiguration();
    }

    @After
    public void tearDown() throws Exception {
        hayStack.tagsDb.getBoxStore().close();
    }

    @Test

    public void testTrueCFMCoolingMaxPoint() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TrueCFMPointsHandler trueCFMConfigPoints=new TrueCFMPointsHandler();
        Method method= TrueCFMPointsHandler.class.getDeclaredMethod("createTrueCFMCoolingMax", CCUHsApi.class, Equip.class, String.class, double.class, String.class);
        method.setAccessible(true);
        CCUHsApi ccuHsApi=hayStack;
        Equip equip1=equip;
        String tag="vav";
        Double val=50.0;
        String fanMarker = "series";
        method.invoke(trueCFMConfigPoints,ccuHsApi,equip1,tag,val,fanMarker);
        HashMap<Object, Object> testTrueCFMCoolingMaxPoint = hayStack.readEntity("trueCfm and cooling and max");
        Assert.assertFalse(testTrueCFMCoolingMaxPoint.isEmpty());

    }
    @Test
    public void testTrueCFMKFactorPoint() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TrueCFMPointsHandler trueCFMConfigPoints=new TrueCFMPointsHandler();
        Method method= TrueCFMPointsHandler.class.getDeclaredMethod("createTrueCFMKFactorPoint", CCUHsApi.class, Equip.class, String.class, double.class, String.class);
        method.setAccessible(true);
        CCUHsApi ccuHsApi=hayStack;
        Equip equip1=equip;
        String tag="vav";
        Double val=150.0;
        String fanMarker = "series";
        method.invoke(trueCFMConfigPoints,ccuHsApi,equip1,tag,val,fanMarker);
        HashMap<Object, Object> testTrueCFMKFactorPoint = hayStack.readEntity("trueCfm and kfactor ");
        Assert.assertFalse(testTrueCFMKFactorPoint.isEmpty());

    }
    @Test
    public void testTrueCFMCoolingMinPoint() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TrueCFMPointsHandler trueCFMConfigPoints=new TrueCFMPointsHandler();
        Method method= TrueCFMPointsHandler.class.getDeclaredMethod("createTrueCFMCoolingMin", CCUHsApi.class, Equip.class, String.class, double.class, String.class);
        method.setAccessible(true);
        CCUHsApi ccuHsApi=hayStack;
        Equip equip1=equip;
        String tag="vav";
        Double val=50.0;
        String fanMarker = "series";
        method.invoke(trueCFMConfigPoints,ccuHsApi,equip1,tag,val,fanMarker);
        HashMap<Object, Object> testTrueCFMCoolingMinPoint = hayStack.readEntity("trueCfm and cooling and min");
        Assert.assertFalse(testTrueCFMCoolingMinPoint.isEmpty());

    }
    @Test
    public void testTrueCFMReheatMinPoint() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TrueCFMPointsHandler trueCFMConfigPoints=new TrueCFMPointsHandler();
        Method method= TrueCFMPointsHandler.class.getDeclaredMethod("createTrueCFMReheatMin", CCUHsApi.class, Equip.class, String.class, double.class, String.class);
        method.setAccessible(true);
        CCUHsApi ccuHsApi=hayStack;
        Equip equip1=equip;
        String tag="vav";
        Double val=50.0;
        String fanMarker = "series";
        method.invoke(trueCFMConfigPoints,ccuHsApi,equip1,tag,val,fanMarker);
        HashMap<Object, Object> testTrueCFMReheatMinPoint = hayStack.readEntity("trueCfm and heating and min");
        Assert.assertFalse(testTrueCFMReheatMinPoint.isEmpty());

    }
    @Test
    public void testTrueCFMReheatMaxPoint() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TrueCFMPointsHandler trueCFMConfigPoints=new TrueCFMPointsHandler();
        Method method= TrueCFMPointsHandler.class.getDeclaredMethod("createTrueCFMReheatMax", CCUHsApi.class, Equip.class, String.class, double.class, String.class);
        method.setAccessible(true);
        CCUHsApi ccuHsApi=hayStack;
        Equip equip1=equip;
        String tag="vav";
        Double val=50.0;
        String fanMarker = "series";
        method.invoke(trueCFMConfigPoints,ccuHsApi,equip1,tag,val,fanMarker);
        HashMap<Object, Object> testTrueCFMReheatMaxPoint = hayStack.readEntity("trueCfm and heating and max");
        Assert.assertFalse(testTrueCFMReheatMaxPoint.isEmpty());

    }

}