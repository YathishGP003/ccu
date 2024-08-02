package a75f.io.logic.bo.building.dab;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.mock.MockCcuHsApi;
import a75f.io.logic.bo.building.dualduct.DualDuctEquip;

/**
 * Created by Manjunath K on 30-03-2022.
 */
public class FeedbackPointTest extends TestCase {

    CCUHsApi hayStack;
    public void setUp() throws Exception {
        hayStack = new MockCcuHsApi();
    }

    public void tearDown() throws Exception {
        hayStack.tagsDb.getBoxStore().close();
    }


    // Function to test DualDuct point creation

    @Test
    public  void testCreateDualDuctFeedbackPoint(){
        String feedbackPointID =  DualDuctEquip.createFeedbackPoint(hayStack,1000,"siteDis","equipRef","siteRef","roomRef",
                "floorRef", "tz","Analog1");
        Assert.assertNotNull(feedbackPointID);
        HashMap<Object, Object> feedbackPoint = hayStack.readEntity("dualDuct and damper and sensor");
        Assert.assertFalse(feedbackPoint.isEmpty());

    }
    // Function to test Vav No Fan point creation
    @Test
    public  void testCreateVavNoFanFeedbackPoint(){
        String feedbackPointID =  VavEquip.createFeedbackPoint(hayStack,1000,"siteDis","equipRef","siteRef","roomRef",
                "floorRef", "","tz");
        Assert.assertNotNull(feedbackPointID);
        HashMap<Object, Object> feedbackPoint = hayStack.readEntity("vav and damper and sensor");
        Assert.assertFalse(feedbackPoint.isEmpty());

    }

    // Function to test Vav series Fan point creation

    @Test
    public  void testCreateVavSerialFanFeedbackPoint(){
        String feedbackPointID =  VavEquip.createFeedbackPoint(hayStack,1000,"siteDis","equipRef","siteRef","roomRef",
                "floorRef", "series","tz");
        Assert.assertNotNull(feedbackPointID);
        HashMap<Object, Object> feedbackPoint = hayStack.readEntity("vav and damper and sensor and series");
        Assert.assertFalse(feedbackPoint.isEmpty());

    }

    // Function to test Vav parallel Fan point creation
    @Test
    public  void testCreateVavParallelFanFeedbackPoint(){
        String feedbackPointID =  VavEquip.createFeedbackPoint(hayStack,1000,"siteDis","equipRef","siteRef","roomRef",
                "floorRef", "parallel","tz");
        Assert.assertNotNull(feedbackPointID);
        HashMap<Object, Object> feedbackPoint = hayStack.readEntity("vav and damper and sensor and parallel");
        Assert.assertFalse(feedbackPoint.isEmpty());

    }
}