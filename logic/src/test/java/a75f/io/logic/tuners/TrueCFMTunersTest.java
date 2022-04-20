package a75f.io.logic.tuners;



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


import a75f.io.logic.bo.building.vav.VavProfileConfiguration;


public class TrueCFMTunersTest {
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

    public void createTrueCFMAirflowCFMIntegralTimePoint() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TrueCFMTuners trueCFMTuners=new TrueCFMTuners();
        Method method= TrueCFMTuners.class.getDeclaredMethod("createTrueCFMAirflowCFMIntegralTimePoint",CCUHsApi.class,Equip.class,String.class,String.class);
        method.setAccessible(true);
        CCUHsApi ccuHsApi=hayStack;
        Equip equip1=equip;
        String a="vav";
        String ab="VAV";
        method.invoke(trueCFMTuners,ccuHsApi,equip1,a,ab);
        HashMap<Object, Object> testTrueCFMEnabledPoint = hayStack.readEntity("cfm and airflow and itimeout");
        Assert.assertFalse(testTrueCFMEnabledPoint.isEmpty());

    }
    @Test
    public void createTrueCFMAirflowCFMProportionalRangePoint() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TrueCFMTuners trueCFMTuners=new TrueCFMTuners();
        Method method= TrueCFMTuners.class.getDeclaredMethod("createTrueCFMAirflowCFMProportionalRangePoint",CCUHsApi.class,Equip.class,String.class,String.class);
        method.setAccessible(true);
        CCUHsApi ccuHsApi=hayStack;
        Equip equip1=equip;
        String a="vav";
        String ab="VAV";
        method.invoke(trueCFMTuners,ccuHsApi,equip1,a,ab);
        HashMap<Object, Object> createTrueCFMAirflowCFMProportionalRangePoint = hayStack.readEntity("cfm and airflow and prange");
        Assert.assertFalse(createTrueCFMAirflowCFMProportionalRangePoint.isEmpty());

    }

    @Test
    public void createTrueCFMAirflowCFMProportionalKFactorPoint() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TrueCFMTuners trueCFMTuners=new TrueCFMTuners();
        Method method= TrueCFMTuners.class.getDeclaredMethod("createTrueCFMAirflowCFMProportionalKFactorPoint",CCUHsApi.class,Equip.class,String.class,String.class);
        method.setAccessible(true);
        CCUHsApi ccuHsApi=hayStack;
        Equip equip1=equip;
        String a="vav";
        String ab="VAV";
        method.invoke(trueCFMTuners,ccuHsApi,equip1,a,ab);
        HashMap<Object, Object> createTrueCFMAirflowCFMProportionalKFactorPoint = hayStack.readEntity("cfm and airflow and pgain");
        Assert.assertFalse(createTrueCFMAirflowCFMProportionalKFactorPoint.isEmpty());

    }

    @Test
    public void createTrueCFMAirflowCFMIntegralKFactorPoint() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TrueCFMTuners trueCFMTuners=new TrueCFMTuners();
        Method method= TrueCFMTuners.class.getDeclaredMethod("createTrueCFMAirflowCFMIntegralKFactorPoint",CCUHsApi.class,Equip.class,String.class,String.class);
        method.setAccessible(true);
        CCUHsApi ccuHsApi=hayStack;
        Equip equip1=equip;
        String a="vav";
        String ab="VAV";
        method.invoke(trueCFMTuners,ccuHsApi,equip1,a,ab);
        HashMap<Object, Object> createTrueCFMAirflowCFMIntegralKFactorPoint = hayStack.readEntity("cfm and airflow and igain");
        Assert.assertFalse(createTrueCFMAirflowCFMIntegralKFactorPoint.isEmpty());

    }



}