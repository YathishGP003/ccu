package a75.io.renatus.testharness;

import org.junit.Test;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * Created by samjithsadasivan on 4/3/18.
 */

public class TestHarnessRunner extends BlockJUnit4ClassRunner
{
    public TestHarnessRunner(Class<?> klass)
            throws InitializationError
    {
        super(klass);
    }
    
    @Override
    protected Statement withPotentialTimeout(FrameworkMethod method, Object test, Statement next) {
        long timeout = getTimeout(method.getAnnotation(Test.class));
        if (timeout > 0) {
            return new FailOnTimeout(next, timeout);
        }
        return next;
    }

    private long getTimeout(Test annotation) {
        if (annotation == null) {
            return 0;
        }
        return annotation.timeout();
    }

    @Override
    public void run(RunNotifier notifier){
        notifier.addListener(new TestHarnessListener());
        super.run(notifier);
    }
}
