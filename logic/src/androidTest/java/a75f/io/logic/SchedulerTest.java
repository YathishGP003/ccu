package a75f.io.logic;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.kinvey.android.store.DataStore;
import com.kinvey.android.store.UserStore;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.java.store.StoreType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.kinvey.CCUPreconfiguration;
import a75f.io.bo.kinvey.Constants;
import a75f.io.bo.kinvey.DalContext;

/**
 * Created by samjithsadasivan on 9/11/17.
 */
@RunWith(AndroidJUnit4.class)
public class SchedulerTest
{
    Context                       context ;
    CCUApplication                mCcuApplication;
    String TAG = "DALTest";
    String   userId   = "userId";
    String   password = "password";

    
    @Before
    public void setup() {
        context = InstrumentationRegistry.getTargetContext().getApplicationContext();
        Globals.getInstance().setApplicationContext(context);

    }
    
    
    @Test
    public void testCCUPreconfiguration()
    {
        //59c8ec3c8f57b7f96ed57ca5
        DalContext.instantiate(context);
        final DataStore<CCUPreconfiguration> preconfigurationDataStore = DataStore
                                                                                 .collection(Constants.PRECONFIGURATION_NAME, CCUPreconfiguration.class, StoreType.CACHE, DalContext
                                                                                                                                                                                  .getSharedClient());
        
        final long currTime = System.nanoTime();
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable()
        {
            
            public void run()
            {
                try
                {
                    UserStore.login(userId, password, DalContext
                                                              .getSharedClient(), new KinveyClientCallback()
                    {
                        
                        @Override
                        public void onSuccess(Object o)
                        {
                            Log.i(TAG, "USER LOGIN SUCCESS");
                            preconfigurationDataStore.find("59c8ec3c8f57b7f96ed57ca5", new KinveyClientCallback<CCUPreconfiguration>()
                            {
                                @Override
                                public void onSuccess(CCUPreconfiguration ccuPreconfiguration)
                                {
                                    long lengthToPullAndParse = System.nanoTime() - currTime;
                                    Log.i(TAG, "Length to pull and parse in nanoseconds: " + lengthToPullAndParse);
                                    
                                    try
                                    {
                                        Log.i(TAG, "CCUPreconfiguration: "  + ccuPreconfiguration.toPrettyString());
                                    }
                                    catch (IOException e)
                                    {
                                        e.printStackTrace();
                                    }
                                }
                                @Override
                                public void onFailure(Throwable throwable)
                                {
                                    throwable.printStackTrace();
                                }
                            });
                        }
                        
                        
                        @Override
                        public void onFailure(Throwable throwable)
                        {
                            throwable.printStackTrace();
                        }
                    });
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
        try
        {
            Thread.sleep(30000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        
    }
    
    
    
    
}
