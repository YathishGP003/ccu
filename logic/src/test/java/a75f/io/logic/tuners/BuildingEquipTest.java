package a75f.io.logic.tuners;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.mock.MockCcuHsApi;

public class BuildingEquipTest {

    CCUHsApi hayStack;

    @Before
    public void setUp() {
        hayStack = new MockCcuHsApi();
    }

    @After
    public void tearDown() {
        hayStack.tagsDb.getBoxStore().close();
    }

    @Test
    public void testBuildingEquipCreation() {
        TunerEquip.INSTANCE.initialize(hayStack, false);
        System.out.println(hayStack.readEntity("tuner and equip"));
    }
}
