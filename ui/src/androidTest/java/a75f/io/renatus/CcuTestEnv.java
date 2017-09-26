package a75f.io.renatus;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 9/21/17.
 */

public class CcuTestEnv
{
    private static CcuTestEnv                INSTANCE  = null;
    public ArrayList<CcuSimulationTest> testSuite = new ArrayList<>();
    private Context mContext;
    
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
