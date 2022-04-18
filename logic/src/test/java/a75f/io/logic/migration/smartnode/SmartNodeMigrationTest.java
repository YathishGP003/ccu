package a75f.io.logic.migration.smartnode;

import android.preference.PreferenceManager;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.mock.MockCcuHsApi;

public class SmartNodeMigrationTest extends TestCase {

    CCUHsApi hayStack;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        hayStack = new MockCcuHsApi();

    }

    @After
    public void tearDown() throws Exception {
        hayStack.tagsDb.getBoxStore().close();
    }

    @Test
    public void testSNMigration() {
        SmartNodeMigration.init();
    }

}