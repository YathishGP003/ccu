package a75f.io.renatus;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import java.util.HashMap;

/**
 * Created by samjithsadasivan on 9/21/17.
 */

public class CcuTestEnv
{
    private static CcuTestEnv                            INSTANCE  = null;
    
    //Key for testSuite could simple class name of test
    public         HashMap<String,CcuSimulationTestInfo> testSuite = new HashMap<>();
    
    //public ArrayList<CcuSimulationTest> testSuite = new ArrayList<>();
    private Context mContext;
    
    public CcuSimulationTest mCurrentTest;
    
    private CcuTestEnv() {
        mContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
    }
    
    public static CcuTestEnv getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CcuTestEnv();
        }
        return INSTANCE;
    }
    
    public Context getContext() {
        return mContext;
    }
}
