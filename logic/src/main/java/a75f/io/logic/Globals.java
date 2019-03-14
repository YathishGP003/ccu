package a75f.io.logic;

import android.content.Context;

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

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.bo.building.CCUApplication;
import a75f.io.logic.bo.building.Day;
import a75f.io.logic.bo.building.NamedSchedule;
import a75f.io.logic.bo.building.Schedule;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.plc.PlcProfile;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.building.system.dab.DabProfile;
import a75f.io.logic.bo.building.system.dab.DabStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavAdvancedHybridRtu;
import a75f.io.logic.bo.building.system.vav.VavAnalogRtu;
import a75f.io.logic.bo.building.system.vav.VavBacnetRtu;
import a75f.io.logic.bo.building.system.vav.VavIERtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtuWithVfd;
import a75f.io.logic.bo.building.vav.VavParallelFanProfile;
import a75f.io.logic.bo.building.vav.VavReheatProfile;
import a75f.io.logic.bo.building.vav.VavSeriesFanProfile;
import a75f.io.logic.jobs.BuildingProcessJob;
import a75f.io.logic.jobs.PrintProcessJob;
import a75f.io.logic.jobs.PrintProcessJobTwo;
import a75f.io.logic.jobs.ScheduleProcessJob;

/**
 * Created by rmatt isOn 7/19/2017.
 */


/*
    This is used to keep track of global static associated with application context.
 */
public class Globals {


    private static final int      NUMBER_OF_CYCLICAL_TASKS_RENATUS_REQUIRES = 10;
    private static final int      TASK_SEPERATION                           = 45;
    private static final TimeUnit TASK_SERERATION_TIMEUNIT                  = TimeUnit.SECONDS;

    private static Globals globals;
    //HeartBeatJob mHeartBeatJob;
    BuildingProcessJob mProcessJob = new BuildingProcessJob();
    ScheduleProcessJob mScheduleProcessJob = new ScheduleProcessJob();

    PrintProcessJob mPrintProcessJob = new PrintProcessJob();
    PrintProcessJobTwo mPrintProcessJobTwo = new PrintProcessJobTwo();

    private ScheduledExecutorService taskExecutor;
    private Context mApplicationContext;
    private CCUApplication mCCUApplication;
    private LZoneProfile mLZoneProfile;
    private boolean isSimulation = false;
    private boolean testHarness = true;

    PubNub pubnub;
    boolean pubnubSubscribed = false;
    private boolean _siteAlreadyCreated;


    private Globals() {
    }


    public ScheduledExecutorService getScheduledThreadPool() {
        return getInstance().taskExecutor;
    }


    public static Globals getInstance() {
        if (globals == null) {
            globals = new Globals();
        }
        return globals;
    }


    public CCUApplication ccu() {
        if (getInstance().mCCUApplication == null) {
            getInstance().mCCUApplication = LocalStorage.getApplicationSettings();
        }
        return getInstance().mCCUApplication;
    }


    public LZoneProfile getLZoneProfile() {
        if (getInstance().mLZoneProfile == null) {
            getInstance().mLZoneProfile = new LZoneProfile();
        }
        return getInstance().mLZoneProfile;
    }


    public boolean isSimulation() {
        return getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                .getBoolean("biskit_mode", false);
    }


    public Context getApplicationContext() {
        return mApplicationContext;
    }


    public void setApplicationContext(Context mApplicationContext) {
        if (this.mApplicationContext == null) {
            this.mApplicationContext = mApplicationContext;
            initilize();
        }
    }


    public void initilize() {
        taskExecutor = Executors.newScheduledThreadPool(NUMBER_OF_CYCLICAL_TASKS_RENATUS_REQUIRES);
        populate();
        //mHeartBeatJob = new HeartBeatJob();
        //5 seconds after application initializes start heart beat

        int DEFAULT_HEARTBEAT_INTERVAL = 60;
        
        mProcessJob.scheduleJob("BuildingProcessJob", DEFAULT_HEARTBEAT_INTERVAL,
                TASK_SEPERATION , TASK_SERERATION_TIMEUNIT);

        mScheduleProcessJob.scheduleJob("Schedule Process Job", DEFAULT_HEARTBEAT_INTERVAL - 10,
                TASK_SEPERATION, TASK_SERERATION_TIMEUNIT);


        isSimulation = getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                .getBoolean("biskit_mode", false);
        testHarness = getApplicationContext().getResources().getBoolean(R.bool.test_harness);


        new CCUHsApi(this.mApplicationContext);
        CCUHsApi.getInstance().testHarnessEnabled = testHarness;
        addProfilesForEquips();

        String addrBand = getSmartNodeBand();
        L.ccu().setSmartNodeAddressBand(addrBand == null ? 1000 : Short.parseShort(addrBand));

    }

    private void populate() {
        //TODO: get this from kinvey.
        //This seems like overkill, but it has to follow the meta to support the unit test
        // framework.

        //TODO test method
        if (ccu().getLCMNamedSchedules().size() == 0) {
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

    private ArrayList<Schedule> getSchedules(int val) {
        Schedule schedule = new Schedule();
        int[] ints = {0, 1, 2, 3, 4};
        ArrayList<Day> intsaslist = new ArrayList<Day>();
        for (int i : ints) { //as
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


    public boolean testHarness() {
        return testHarness;
    }

    public void setCCU(CCUApplication CCU) {
        this.mCCUApplication = CCU;
    }

    public void saveTags() {
        CCUHsApi.getInstance().saveTagsData();
    }

    public void registerSiteToPubNub(final String siteId) {

        CcuLog.d(L.TAG_CCU,"registerSiteToPubNub "+siteId.replace("@",""));

        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey("sub-c-6a55a31c-d30e-11e8-b41d-e643bd6bdd68");
        pnConfiguration.setPublishKey("pub-c-6873a2c5-ec27-4604-a235-38a3f4eed9a6");
        pnConfiguration.setSecure(false);

        pubnub = new PubNub(pnConfiguration);

        //HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        //final String channelName = (String) siteMap.get(Tags.ID);

        // create message payload using Gson
        final JsonObject messageJsonObject = new JsonObject();

        messageJsonObject.addProperty("msg", "Configuration");
    
        CcuLog.d(L.TAG_CCU,"CCU Message to send: " + messageJsonObject.toString());

        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {


                if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                    // This event happens when radio / connectivity is lost
                } else if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {

                    // Connect event. You can do stuff like publish, and know you'll get it.
                    // Or just use the connected event to confirm you are subscribed for
                    // UI / internal notifications, etc

                    if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {
                        CcuLog.d(L.TAG_CCU, "PNConnectedCategory publish");
                        pubnub.publish().channel(siteId.replace("@","")).message(messageJsonObject).async(new PNCallback<PNPublishResult>() {
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
                } else if (status.getCategory() == PNStatusCategory.PNReconnectedCategory) {

                    // Happens as part of our regular operation. This event happens when
                    // radio / connectivity is lost, then regained.
                } else if (status.getCategory() == PNStatusCategory.PNDecryptionErrorCategory) {

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
                } else {
                    // Message has been received on channel stored in
                    // message.getSubscription()
                }

                JsonElement receivedMessageObject = message.getMessage();
                CcuLog.d(L.TAG_CCU, "PubNub Received message content: " + receivedMessageObject.toString());
                // extract desired parts of the payload, using Gson
                JsonObject msgObject = message.getMessage().getAsJsonObject();
                String cmd = msgObject.get("cmd") != null ? msgObject.get("cmd").getAsString(): "";
                if (cmd.equals("updatePoint"))
                {
                    String who = msgObject.get("who").getAsString();
                    String level = msgObject.get("level").getAsString();
                    String val = msgObject.get("val").getAsString();
                    String id = msgObject.get("id").getAsString();
                    CcuLog.d("CCU", "Update point: cmd: " + cmd + " who: " + who + " level: " + level + " val: " + val + " id: " + id);
    
                    CCUHsApi.getInstance().getHSClient()
                            .pointWrite(HRef.make(CCUHsApi.getInstance().getLUID(id)), (int) Double.parseDouble(level), who, HNum.make(Double.parseDouble(val)), HNum.make(0));
                }
                
                


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

    
        pubnub.subscribe().channels(Arrays.asList(siteId.replace("@",""))).execute();

        pubnubSubscribed = true;
    }

    public boolean isPubnubSubscribed() {
        return pubnubSubscribed;
    }

    public void addProfilesForEquips() {
        HashMap site = CCUHsApi.getInstance().read(Tags.SITE);
        if (site == null || site.size() == 0) {
            CcuLog.d(L.TAG_CCU, "Site does not exist. Profiles not loaded");
            return;
        }
        for (Floor f : HSUtil.getFloors()) {
            for (Zone z : HSUtil.getZones(f.getId())) {
                for (Equip eq : HSUtil.getEquips(z.getId())) {
                    CcuLog.d(L.TAG_CCU, " Equip " + eq.getDisplayName() + " profile : " + eq.getProfile());
                    switch (ProfileType.valueOf(eq.getProfile())) {
                        case VAV_REHEAT:
                            VavReheatProfile vr = new VavReheatProfile();
                            vr.addLogicalMap(Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(vr);
                            break;
                        case VAV_SERIES_FAN:
                            VavSeriesFanProfile vsf = new VavSeriesFanProfile();
                            vsf.addLogicalMap(Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(vsf);
                            break;
                        case VAV_PARALLEL_FAN:
                            VavParallelFanProfile vpf = new VavParallelFanProfile();
                            vpf.addLogicalMap(Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(vpf);
                            break;
                        case PLC:
                            PlcProfile plc = new PlcProfile();
                            plc.addPlcEquip(Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(plc);
                            break;
                        case DAB:
                            DabProfile dab = new DabProfile();
                            dab.addDabEquip(Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(dab);
                            break;
                    }
                }
            }

        }

        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        if (equip != null && equip.size() > 0) {
            Equip eq = new Equip.Builder().setHashMap(equip).build();
            CcuLog.d(L.TAG_CCU, "SystemEquip " + eq.getDisplayName() + " System profile " + eq.getProfile());
            switch (ProfileType.valueOf(eq.getProfile())) {
                case SYSTEM_VAV_ANALOG_RTU:
                    VavAnalogRtu analogRtuProfile = new VavAnalogRtu();
                    analogRtuProfile.addSystemEquip();
                    L.ccu().systemProfile = analogRtuProfile;
                    
                    break;
                case SYSTEM_VAV_STAGED_RTU:
                    VavStagedRtu stagedRtuProfile = new VavStagedRtu();
                    stagedRtuProfile.addSystemEquip();
                    L.ccu().systemProfile = stagedRtuProfile;
                    break;
                case SYSTEM_VAV_STAGED_VFD_RTU:
                    VavStagedRtuWithVfd stagedVfdRtuProfile = new VavStagedRtuWithVfd();
                    stagedVfdRtuProfile.addSystemEquip();
                    L.ccu().systemProfile = stagedVfdRtuProfile;
                    break;
                case SYSTEM_VAV_HYBRID_RTU:
                    VavAdvancedHybridRtu hybridRtuProfile = new VavAdvancedHybridRtu();
                    hybridRtuProfile.addSystemEquip();
                    L.ccu().systemProfile = hybridRtuProfile;
                    break;
                case SYSTEM_VAV_IE_RTU:
                    VavIERtu ieRtuProfile = new VavIERtu();
                    ieRtuProfile.addSystemEquip();
                    L.ccu().systemProfile = ieRtuProfile;
                    break;
                case SYSTEM_VAV_BACNET_RTU:
                    VavBacnetRtu bacnetRtu = new VavBacnetRtu();
                    //bacnetRtu.initTRSystem();
                    L.ccu().systemProfile = bacnetRtu;
                    break;
                case SYSTEM_DAB_STAGED_RTU:
                    L.ccu().systemProfile = new DabStagedRtu();
                    break;
                default:
                    L.ccu().systemProfile = new DefaultSystem();
            }
        } else {
            CcuLog.d(L.TAG_CCU, "System Equip does not exist.Create Dafault System Profile");
            L.ccu().systemProfile = new DefaultSystem();

        }
    }

    public String getSmartNodeBand() {
        HashMap band = CCUHsApi.getInstance().read("point and snband");
        if (band != null && band.size() > 0) {
            return band.get("val").toString();
        }
        return null;
    }

    public boolean siteAlreadyCreated() {
        return _siteAlreadyCreated;
    }

    public void setSiteAlreadyCreated(boolean siteAlreadyCreated) {
        _siteAlreadyCreated = siteAlreadyCreated;
    }
}
