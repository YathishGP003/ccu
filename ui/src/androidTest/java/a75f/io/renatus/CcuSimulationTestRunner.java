package a75f.io.renatus;

import android.support.test.internal.util.AndroidRunnerParams;

import org.junit.Test;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * Created by samjithsadasivan on 9/26/17.
 */

//TODO - May override timeout, and implement junit testwatcher/testlistener
    
public class CcuSimulationTestRunner extends BlockJUnit4ClassRunner
{
    private final AndroidRunnerParams mAndroidRunnerParams;
    
    public CcuSimulationTestRunner(Class<?> klass, AndroidRunnerParams runnerParams)
            throws InitializationError
    {
        super(klass);
        mAndroidRunnerParams = runnerParams;
    }
    
    @Override
    protected Statement withPotentialTimeout(FrameworkMethod method, Object test, Statement next) {
        long timeout = getTimeout(method.getAnnotation(Test.class));
        if (timeout > 0) {
            return new FailOnTimeout(next, timeout);
        } else if (mAndroidRunnerParams.getPerTestTimeout() > 0) {
            return new FailOnTimeout(next, mAndroidRunnerParams.getPerTestTimeout());
        }
        return next;
    }
    
    private long getTimeout(Test annotation) {
        if (annotation == null) {
            return 0;
        }
        return annotation.timeout();
    }
}
