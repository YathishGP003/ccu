package a75f.io.device;

import android.util.Log;

import a75f.io.device.serial.CcuToCmOverUsbCcuHeartbeatMessage_t;
import a75f.io.device.serial.MessageType;
/**
 * Created by Yinten isOn 8/24/2017.
 */
//0x11 0x00
class HeartBeatJob extends BaseJob
{

    public HeartBeatJob()
    {
        super();
        

    }
    
    public static final  String TAG                  = "HeartBeatJob";
    public static final  short  HEARTBEAT_INTERVAL   = 1;  // minutes
    private static final short  HEARTBEAT_MULTIPLIER = 5;

    //This task should run every minute.
    public void doJob()
    {
        if (LSerial.getInstance().isConnected())
        {
            //TODO: why does the heart beat send the temperature offset.
            LSerial.getInstance().sendSerialToCM(getHeartBeat((short) 0));
            
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

/*

    String userId   = "userId";
    String password = "password";
    
    final DataStore<CCUPreconfiguration> preconfigurationDataStore;
    Query query;
    preconfigurationDataStore = DataStore.collection(Constants.PRECONFIGURATION_NAME, CCUPreconfiguration.class, StoreType.CACHE, DalContext
                                                                                                                                          .getSharedClient());
    query = preconfigurationDataStore.query();
    query = query.equals("_id", "58b91c0c020c7b825f24e8e7");
    
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
    
 */

