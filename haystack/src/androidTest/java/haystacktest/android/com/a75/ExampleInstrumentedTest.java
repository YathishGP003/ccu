package haystacktest.android.com.a75;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logger.CcuLog;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest
{
    @Test
    public void useAppContext() throws Exception
    {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("haystacktest.android.com.a75.test", appContext.getPackageName());
    }



    @Test
    public void getToken()
    {

        String scope = "";

        String tokenJson = HttpUtil.authorizeToken(HttpUtil.CLIENT_ID, scope, HttpUtil.CLIENT_SECRET, HttpUtil.TENANT_ID);
        CcuLog.d("CCU_HS", "Token : " + tokenJson);
    }

    @Test
    public void addSite()
    {


    }

}
