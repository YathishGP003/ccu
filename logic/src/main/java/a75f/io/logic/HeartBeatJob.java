package a75f.io.logic;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.kinvey.android.store.DataStore;
import com.kinvey.android.store.UserStore;
import com.kinvey.android.sync.KinveyPullResponse;
import com.kinvey.android.sync.KinveyPushResponse;
import com.kinvey.android.sync.KinveySyncCallback;
import com.kinvey.java.Query;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.java.store.StoreType;

import java.io.IOException;

import a75f.io.bo.serial.CcuToCmOverUsbCcuHeartbeatMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.dal.CCUPreconfiguration;
import a75f.io.dal.Constants;
import a75f.io.dal.DalContext;

/**
 * Created by Yinten isOn 8/24/2017.
 */
//0x11 0x00
class HeartBeatJob extends BaseJob
{
    final DataStore<CCUPreconfiguration> preconfigurationDataStore;
    Query query;
    public HeartBeatJob()
    {
        super();
        
        preconfigurationDataStore = DataStore.collection(Constants.PRECONFIGURATION_NAME, CCUPreconfiguration.class, StoreType.CACHE, DalContext.getSharedClient());
        query = preconfigurationDataStore.query();
        query = query.equals("_id", "58b91c0c020c7b825f24e8e7");
    }
    
    public static final  String TAG                  = "HeartBeatJob";
    public static final  short  HEARTBEAT_INTERVAL   = 1;  // minutes
    private static final short  HEARTBEAT_MULTIPLIER = 5;
    String userId   = "userId";
    String password = "password";
    
    //This task should run every minute.
    public void doJob()
    {
        if (LSerial.getInstance().isConnected())
        {
            //TODO: why does the heart beat send the temperature offset.
            LSerial.getInstance().sendSerialToCM(getHeartBeat((short) 0));
            //59c8ec3c8f57b7f96ed57ca5
            final long currTime = System.nanoTime();
            Handler h = new Handler(Looper.getMainLooper());
            h.post(new Runnable()
            {
                
                public void run()
                {
                    if (!DalContext.getSharedClient().isUserLoggedIn())
                    {
                        try
                        {
                            UserStore.login(userId, password, DalContext.getSharedClient(), new KinveyClientCallback()
                            {
                                
                                @Override
                                public void onSuccess(Object o)
                                {
                                    final long lengthToLogin = System.nanoTime() - currTime;
                                    Log.i(TAG, "Length to Login in nanoseconds: " + lengthToLogin);
                                    Log.i(TAG, "USER LOGIN SUCCESS");
                                    
                                    preconfigurationDataStore.sync(query, new KinveySyncCallback<CCUPreconfiguration>()
                                    {
                                        @Override
                                        public void onSuccess(KinveyPushResponse kinveyPushResponse, KinveyPullResponse<CCUPreconfiguration> kinveyPullResponse)
                                        {
                                            long lengthToPullAndParse = System.nanoTime() - currTime - lengthToLogin;
                                            Log.i(TAG, "Length to pull and parse in nanoseconds: " + lengthToPullAndParse);
                                            try
                                            {
                                                Log.i(TAG, "CCUPreconfiguration: " + kinveyPullResponse.getResult().get(0).toPrettyString());
                                            }
                                            catch (IOException e)
                                            {
                                                e.printStackTrace();
                                            }
                                            Log.i(TAG, "onSuccess");
                                        }
                                        @Override
                                        public void onPullStarted()
                                        {
                                            Log.i(TAG, "onPullStarted");
                                        }
                                        @Override
                                        public void onPushStarted()
                                        {
                                        }
                                        @Override
                                        public void onPullSuccess(KinveyPullResponse<CCUPreconfiguration> kinveyPullResponse)
                                        {
                                            Log.i(TAG, "onPullSuccess");
                                        }
                                        @Override
                                        public void onPushSuccess(KinveyPushResponse kinveyPushResponse)
                                        {
                                            Log.i(TAG, "onPushSuccess");
                                        }
                                        @Override
                                        public void onFailure(Throwable throwable)
                                        {
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
                    else
                    {
                        preconfigurationDataStore.sync(query, new KinveySyncCallback<CCUPreconfiguration>()
                        {
                            @Override
                            public void onSuccess(KinveyPushResponse kinveyPushResponse, KinveyPullResponse<CCUPreconfiguration> kinveyPullResponse)
                            {
                                long lengthToPullAndParse = System.nanoTime() - currTime;
                                Log.i(TAG, "Length to pull and parse in nanoseconds: " + lengthToPullAndParse);
                                try
                                {
                                    Log.i(TAG, "CCUPreconfiguration: " + kinveyPullResponse.getResult().get(0).toPrettyString());
                                }
                                catch (IOException e)
                                {
                                    e.printStackTrace();
                                }
                                Log.i(TAG, "onSuccess");
                            }
                            @Override
                            public void onPullStarted()
                            {
                                Log.i(TAG, "onPullStarted");
                            }
                            @Override
                            public void onPushStarted()
                            {
                            }
                            @Override
                            public void onPullSuccess(KinveyPullResponse<CCUPreconfiguration> kinveyPullResponse)
                            {
                                Log.i(TAG, "onPullSuccess");
                            }
                            @Override
                            public void onPushSuccess(KinveyPushResponse kinveyPushResponse)
                            {
                                Log.i(TAG, "onPushSuccess");
                            }
                            @Override
                            public void onFailure(Throwable throwable)
                            {
                            }
                        });
                    }
                }
            });
        }
        else
        {
            Log.d(TAG, "Serial is not connected, rescheduling heartbeat");
        }
    }
    
    private static CcuToCmOverUsbCcuHeartbeatMessage_t getHeartBeat(short temperatureOffset)
    {
        CcuToCmOverUsbCcuHeartbeatMessage_t heartbeatMessage_t = new CcuToCmOverUsbCcuHeartbeatMessage_t();
        heartbeatMessage_t.interval.set(HEARTBEAT_INTERVAL);
        heartbeatMessage_t.messageType.set(MessageType.CCU_HEARTBEAT_UPDATE);
        heartbeatMessage_t.multiplier.set(HEARTBEAT_MULTIPLIER);
        heartbeatMessage_t.temperatureOffset.set((byte) temperatureOffset);
        return heartbeatMessage_t;
    }
}

