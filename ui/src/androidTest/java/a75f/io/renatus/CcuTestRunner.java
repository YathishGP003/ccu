package a75f.io.renatus;

/**
 * Created by samjithsadasivan on 9/21/17.
 */

import android.support.test.InstrumentationRegistry;

import java.util.ArrayList;

/**
 * CcuTestTracker maintains list of all the test cases to be executed and pass/fail status, reason for failure, log etc
 */
public class CcuTestTracker
{
    private static CcuTestTracker               INSTANCE  = null;
    public         ArrayList<CcuSimulationTest> testSuite = new ArrayList<>();
    private        CcuTestEnv                   mEnv      = null;
    private CcuTestTracker() {
        mEnv = new CcuTestEnv(InstrumentationRegistry.getTargetContext().getApplicationContext());
    }
    
    public static CcuTestTracker getInstance() {
        if (INSTANCE == null) {
            return new CcuTestTracker();
        }
        return INSTANCE;
    }
    
    public CcuTestEnv getTestEnv() {
        return mEnv;
    }
    
}
