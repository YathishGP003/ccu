package a75f.io.renatus.framework;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
/**
 * Created by samjithsadasivan on 9/21/17.
 */
/**
 * Global static wrapper around android instrumentation context.
 * It also helps track all the tests part of current suite.
 */
public class SimulationContext
{
    private static SimulationContext INSTANCE = null;
    
    public SimulationTestSuite testSuite = new SimulationTestSuite();
    
    private Context mContext;
    
    public BaseSimulationTest mCurrentTest;
    
    public int testCount;
    public int runCount;
    
    private SimulationContext() {
        mContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
    }
    
    public static SimulationContext getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SimulationContext();
        }
        return INSTANCE;
    }
    
    public Context getContext() {
        return mContext;
    }
    
    
}
