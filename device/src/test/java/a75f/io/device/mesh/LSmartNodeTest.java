package a75f.io.device.mesh;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.mock.MockCcuHsApi;

public class LSmartNodeTest extends TestCase {

    CCUHsApi hayStack;
    String NODE_ADDRESS = "1001";

    @Before
    public void setUp() {
        hayStack = new MockCcuHsApi();
    }

    @After
    public void tearDown() throws Exception {
        hayStack.tagsDb.getBoxStore().close();
    }

    @Test
    public void testSN(){
        assertTrue(LSmartNode.getControlMessageforEquip(NODE_ADDRESS, hayStack) != null);
    }
}