package a75f.io.logic;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Site;
import a75f.io.logic.bo.building.CCUApplication;
import a75f.io.logic.bo.building.Day;
import a75f.io.logic.bo.building.NamedSchedule;
import a75f.io.logic.bo.building.Schedule;
import a75f.io.logic.tuners.BuildingTuners;

/**
 * Created by rmatt isOn 7/19/2017.
 */


/*
    This is used to keep track of global static associated with application context.
 */
public class Globals
{

    private static final int      NUMBER_OF_CYCLICAL_TASKS_RENATUS_REQUIRES = 10;
    private static final int      TASK_SEPERATION                           = 15;
    private static final TimeUnit TASK_SERERATION_TIMEUNIT                  = TimeUnit.SECONDS;
    private static Globals globals;
    //HeartBeatJob mHeartBeatJob;
    BuildingProcessJob mProcessJob = new BuildingProcessJob();
    private ScheduledExecutorService taskExecutor;
    private Context                  mApplicationContext;
    private CCUApplication           mCCUApplication;
    private LZoneProfile             mLZoneProfile;
    private boolean isSimulation = false;
    private boolean isDeveloperTest = true;

    PubNub pubnub;
    boolean pubnubSubscribed = false;


    private Globals()
    {
    }


    public ScheduledExecutorService getScheduledThreadPool()
    {
        return getInstance().taskExecutor;
    }


    public static Globals getInstance()
    {
        if (globals == null)
        {
            globals = new Globals();
        }
        return globals;
    }


    public CCUApplication ccu()
    {
        if (getInstance().mCCUApplication == null)
        {
            getInstance().mCCUApplication = LocalStorage.getApplicationSettings();
        }
        return getInstance().mCCUApplication;
    }


    public LZoneProfile getLZoneProfile()
    {
        if (getInstance().mLZoneProfile == null)
        {
            getInstance().mLZoneProfile = new LZoneProfile();
        }
        return getInstance().mLZoneProfile;
    }


    public boolean isSimulation()
    {
        return isSimulation;
    }





    public Context getApplicationContext()
    {
        return mApplicationContext;
    }


    public void setApplicationContext(Context mApplicationContext)
    {
        if (this.mApplicationContext == null)
        {
            this.mApplicationContext = mApplicationContext;
            initilize();
        }
    }


    public void initilize()
    {
        taskExecutor = Executors.newScheduledThreadPool(NUMBER_OF_CYCLICAL_TASKS_RENATUS_REQUIRES);
        populate();
        //mHeartBeatJob = new HeartBeatJob();
        //5 seconds after application initializes start heart beat
        int DEFAULT_HEARTBEAT_INTERVAL = 30;
        
        mProcessJob.scheduleJob("Building Process Job", DEFAULT_HEARTBEAT_INTERVAL,
                TASK_SEPERATION * 2, TASK_SERERATION_TIMEUNIT);
        
        isSimulation = getApplicationContext().getResources().getBoolean(R.bool.simulation);
        isDeveloperTest = getApplicationContext().getResources().getBoolean(R.bool.developer_test);

        
        new CCUHsApi(this.mApplicationContext);
        //TODO - Test => Should be moved to registration module
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() == 0) {
            //TODO - demo
            Site s75f = new Site.Builder()
                                .setDisplayName("75F")
                                .addMarker("site")
                                .setGeoCity("Burnsville")
                                .setGeoState("MN")
                                .setTz("Chicago")
                                .setArea(10000).build();
            CCUHsApi.getInstance().addSite(s75f);
            BuildingTuners.getInstance();//To init Building tuner
        }
    }

    private void populate()
    {
        //TODO: get this from kinvey.
        //This seems like overkill, but it has to follow the meta to support the unit test
        // framework.
        
        //TODO test method
        if(ccu().getLCMNamedSchedules().size() == 0)
        {
            //Mock schedule M-F, 8AM - 5:30PM turn isOn lights to value 100.
            //Mock schedule M-F, 8AM - 5:30PM turn isOn lights to value 100.


            NamedSchedule namedSchedule = new NamedSchedule();
            namedSchedule.setName("LCM Named Schedule 100");
            namedSchedule.setSchedule(getSchedules(100));


            NamedSchedule namedScheduleTwo = new NamedSchedule();
            namedScheduleTwo.setName("LCM Named Schedule 75");
            namedScheduleTwo.setSchedule(getSchedules(75));

            ccu().getLCMNamedSchedules().put(namedSchedule.getName(), namedSchedule);
            ccu().getLCMNamedSchedules().put(namedScheduleTwo.getName(), namedScheduleTwo);
            ccu().setDefaultLightSchedule(getSchedules(100));
            ccu().setDefaultTemperatureSchedule(getSchedules(75));
        }
    }

    private ArrayList<Schedule> getSchedules(int val)
    {
        Schedule schedule = new Schedule();
        int[] ints = {0, 1, 2, 3, 4};
        ArrayList<Day> intsaslist = new ArrayList<Day>();
        for(int i : ints)
        { //as
            Day day = new Day();
            day.setDay(i);
            day.setSthh(8);
            day.setStmm(00);
            day.setEthh(17);
            day.setEtmm(30);
            day.setVal((short) val);
            intsaslist.add(day);
        }
        schedule.setDays(intsaslist);
        ArrayList<Schedule> schedules = new ArrayList<Schedule>();
        schedules.add(schedule);

        return schedules;
    }


    public boolean isDeveloperTesting()
    {
        return isDeveloperTest;
    }

    public void setCCU(CCUApplication CCU)
    {
        this.mCCUApplication = CCU;
    }
    
    public void saveTags(){
        CCUHsApi.getInstance().saveTagsData();
    }
    
    public void registerSiteToPubNub(final String siteId) {
        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey("sub-c-dea182aa-e109-11e8-a36a-3a3b171d1021");
        pnConfiguration.setPublishKey("pub-c-2e374aa8-7e94-47e3-b51d-7d8e1b73aa14");
        pnConfiguration.setSecure(false);
        
        pubnub = new PubNub(pnConfiguration);
    
        //HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        //final String channelName = (String) siteMap.get(Tags.ID);
        
        // create message payload using Gson
        final JsonObject messageJsonObject = new JsonObject();
        messageJsonObject.addProperty("msg", "hello");
    
        System.out.println("CCU Message to send: " + messageJsonObject.toString());
    
        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
            
            
                if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                    // This event happens when radio / connectivity is lost
                }
            
                else if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {
                
                    // Connect event. You can do stuff like publish, and know you'll get it.
                    // Or just use the connected event to confirm you are subscribed for
                    // UI / internal notifications, etc
                
                    if (status.getCategory() == PNStatusCategory.PNConnectedCategory){
                        Log.d("CCU", "PNConnectedCategory publish");
                        pubnub.publish().channel(siteId).message(messageJsonObject).async(new PNCallback<PNPublishResult>() {
                            @Override
                            public void onResponse(PNPublishResult result, PNStatus status) {
                                // Check whether request successfully completed or not.
                                if (!status.isError()) {
                                
                                    // Message successfully published to specified channel.
                                }
                                // Request processing failed.
                                else {
                                
                                    // Handle message publish error. Check 'category' property to find out possible issue
                                    // because of which request did fail.
                                    //
                                    // Request can be resent using: [status retry];
                                }
                            }
                        });
                    }
                }
                else if (status.getCategory() == PNStatusCategory.PNReconnectedCategory) {
                
                    // Happens as part of our regular operation. This event happens when
                    // radio / connectivity is lost, then regained.
                }
                else if (status.getCategory() == PNStatusCategory.PNDecryptionErrorCategory) {
                
                    // Handle messsage decryption error. Probably client configured to
                    // encrypt messages and on live data feed it received plain text.
                }
            }
        
            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
                // Handle new message stored in message.message
                if (message.getChannel() != null) {
                    // Message has been received on channel group stored in
                    // message.getChannel()
                }
                else {
                    // Message has been received on channel stored in
                    // message.getSubscription()
                }
            
                JsonElement receivedMessageObject = message.getMessage();
                System.out.println("CCU PubNub Received message content: " + receivedMessageObject.toString());
                // extract desired parts of the payload, using Gson
                String msg = message.getMessage().getAsJsonObject().get("msg").getAsString();
                System.out.println("CCU PubNub msg content: " + msg);


            /*
                log the following items with your favorite logger
                    - message.getMessage()
                    - message.getSubscription()
                    - message.getTimetoken()
            */
            }
        
            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {
            
            }
        });
    
        pubnub.subscribe().channels(Arrays.asList(siteId)).execute();
        pubnubSubscribed = true;
    }
    
    public boolean isPubnubSubscribed()
    {
        return pubnubSubscribed;
    }
    
}
