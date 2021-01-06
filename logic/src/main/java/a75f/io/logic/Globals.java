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

import a75f.io.alerts.AlertProcessJob;
import a75f.io.api.haystack.BuildConfig;
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
import a75f.io.logic.bo.building.ccu.CazProfile;
import a75f.io.logic.bo.building.dab.DabProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.dualduct.DualDuctProfile;
import a75f.io.logic.bo.building.erm.EmrProfile;
import a75f.io.logic.bo.building.modbus.ModbusProfile;
import a75f.io.logic.bo.building.oao.OAOProfile;
import a75f.io.logic.bo.building.plc.PlcProfile;
import a75f.io.logic.bo.building.ss2pfcu.TwoPipeFanCoilUnitProfile;
import a75f.io.logic.bo.building.ss4pfcu.FourPipeFanCoilUnitProfile;
import a75f.io.logic.bo.building.sscpu.ConventionalUnitProfile;
import a75f.io.logic.bo.building.sse.SingleStageProfile;
import a75f.io.logic.bo.building.sshpu.HeatPumpUnitProfile;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.building.system.dab.DabAdvancedHybridRtu;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtuWithVfd;
import a75f.io.logic.bo.building.system.vav.VavAdvancedHybridRtu;
import a75f.io.logic.bo.building.system.vav.VavBacnetRtu;
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu;
import a75f.io.logic.bo.building.system.vav.VavIERtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtuWithVfd;
import a75f.io.logic.bo.building.vav.VavParallelFanProfile;
import a75f.io.logic.bo.building.vav.VavReheatProfile;
import a75f.io.logic.bo.building.vav.VavSeriesFanProfile;
import a75f.io.logic.jobs.BuildingProcessJob;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.pubnub.PubNubHandler;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.logic.watchdog.Watchdog;

/*
    This is used to keep track of global static associated with application context.
 */
public class Globals {
    public static final class IntentActions {
        public static final String LSERIAL_MESSAGE = "a75f.io.intent.action.LSERIAL_MESSAGE";
        public static final String ACTIVITY_MESSAGE = "a75f.io.intent.action.ACTIVITY_MESSAGE";
        public static final String ACTIVITY_RESET = "a75f.io.intent.action.ACTIVITY_RESET";
        public static final String PUBNUB_MESSAGE = "a75f.io.intent.action.PUBNUB_MESSAGE";
        public static final String OTA_UPDATE_START = "a75f.io.intent.action.OTA_UPDATE_START";
        public static final String OTA_UPDATE_CM_ACK = "a75f.io.intent.action.OTA_UPDATE_CM_ACK";
        public static final String OTA_UPDATE_PACKET_REQ = "a75f.io.action.OTA_UPDATE_PACKET_REQ";
        public static final String OTA_UPDATE_NODE_REBOOT = "a75f.io.action.OTA_UPDATE_NODE_REBOOT";
        public static final String OTA_UPDATE_TIMED_OUT = "a75f.io.intent.action.OTA_UPDATE_TIMED_OUT";
        public static final String OTA_UPDATE_COMPLETE = "a75f.io.intent.action.OTA_UPDATE_COMPLETE";
    }

    private static final int      NUMBER_OF_CYCLICAL_TASKS_RENATUS_REQUIRES = 10;
    private static final int TASK_SEPARATION = 15;
    private static final TimeUnit TASK_SEPARATION_TIMEUNIT = TimeUnit.SECONDS;
    
    private static final int DEFAULT_HEARTBEAT_INTERVAL = 60;
    
    private static Globals globals;
    BuildingProcessJob mProcessJob = new BuildingProcessJob();
    ScheduleProcessJob mScheduleProcessJob = new ScheduleProcessJob();
    
    AlertProcessJob mAlertProcessJob;

    private ScheduledExecutorService taskExecutor;
    private Context mApplicationContext;
    private CCUApplication mCCUApplication;
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

    public boolean isSimulation() {
        return getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                .getBoolean("biskit_mode", false);
    }
    
    public boolean isTestMode()
    {
        return Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                      .getBoolean("test_mode", false);
    }
    public void setTestMode(boolean isTestMode) {
        Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                .edit().putBoolean("test_mode", isTestMode).apply();
    }
    public boolean isWeatherTest() {
        return Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                .getBoolean("weather_test", false);
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
        
        Log.d(L.TAG_CCU_JOB, " Create Process Jobs");
        
        isSimulation = getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                .getBoolean("biskit_mode", false);
        testHarness = getApplicationContext().getResources().getBoolean(R.bool.test_harness);


        CCUHsApi ccuHsApi = new CCUHsApi(this.mApplicationContext);
        ccuHsApi.testHarnessEnabled = testHarness;

        //set SN address band
        String addrBand = getSmartNodeBand();
        L.ccu().setSmartNodeAddressBand(addrBand == null ? 1000 : Short.parseShort(addrBand));
        
        importTunersAndScheduleJobs();
        
    }
    
    
    private void importTunersAndScheduleJobs() {

        new Thread()
        {
            @Override
            public void run()
            {
                //If site already exists , import building tuners from backend before initializing building tuner equip.
                HashMap<Object, Object> site = CCUHsApi.getInstance().readEntity("site");
                if (!site.isEmpty()) {
                    if (!CCUHsApi.getInstance().isPrimaryCcu()) {
                        CCUHsApi.getInstance().importBuildingTuners();
                    }
                    BuildingTuners.getInstance().updateBuildingTuners();
                    CCUHsApi.getInstance().syncEntityTree();
                }
            
                loadEquipProfiles();
            
                if (!isPubnubSubscribed())
                {
                    if (!site.isEmpty()) {
                        String siteGUID = CCUHsApi.getInstance().getGlobalSiteId();
                        if (siteGUID != null && siteGUID != "") {
                            Globals.getInstance().registerSiteToPubNub(siteGUID);
                        }
                    }
                }
            
                mProcessJob.scheduleJob("BuildingProcessJob", DEFAULT_HEARTBEAT_INTERVAL,
                                        TASK_SEPARATION, TASK_SEPARATION_TIMEUNIT);
            
                mScheduleProcessJob.scheduleJob("Schedule Process Job", DEFAULT_HEARTBEAT_INTERVAL,
                                                TASK_SEPARATION +15, TASK_SEPARATION_TIMEUNIT);
            
                mAlertProcessJob = new AlertProcessJob(mApplicationContext);
                getScheduledThreadPool().scheduleAtFixedRate(mAlertProcessJob.getJobRunnable(), TASK_SEPARATION +30, DEFAULT_HEARTBEAT_INTERVAL, TASK_SEPARATION_TIMEUNIT);
            
                Watchdog.getInstance().addMonitor(mProcessJob);
                Watchdog.getInstance().addMonitor(mScheduleProcessJob);
                Watchdog.getInstance().start();
            
                CCUHsApi.getInstance().syncEntityWithPointWrite();
            
            }
        }.start();
        
        if (isTestMode()) {
            setTestMode(false);
        }
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

        final String pubnubSubscribeKey = BuildConfig.PUBNUB_SUBSCRIBE_KEY;
        final String pubnubPublishKey = BuildConfig.PUBNUB_PUBLISH_KEY;

        pnConfiguration.setSubscribeKey(pubnubSubscribeKey);
        pnConfiguration.setPublishKey(pubnubPublishKey);

        pnConfiguration.setSecure(false);

        pubnub = new PubNub(pnConfiguration);

        // create message payload using Gson
        final JsonObject messageJsonObject = new JsonObject();

        messageJsonObject.addProperty("msg", "Configuration");
    
        CcuLog.d(L.TAG_CCU,"CCU Message to send: " + messageJsonObject.toString());

        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {


                if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                    CcuLog.d(L.TAG_CCU, "pubnub PNUnexpectedDisconnectCategory ");
                    pubnub.reconnect();
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
                }else if(status.getCategory() == PNStatusCategory.PNTimeoutCategory){

                    CcuLog.d(L.TAG_CCU, "pubnub  PNTimeoutCategory ");
                    pubnub.reconnect();
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
                CcuLog.d(L.TAG_CCU_PUBNUB, "PubNub Received message content: " + receivedMessageObject.toString());
                
                try
                {
                    PubNubHandler.handleMessage(message.getMessage().getAsJsonObject(), getApplicationContext());
                } catch (NumberFormatException e) {
                    Log.d(L.TAG_CCU_PUBNUB, " Ignoring PubNub Message "+e.getMessage());
                }
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

    public void loadEquipProfiles() {
        HashMap site = CCUHsApi.getInstance().read(Tags.SITE);
        if (site == null || site.size() == 0) {
            CcuLog.d(L.TAG_CCU, "Site does not exist. Profiles not loaded");
            return;
        }
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        boolean isDefaultSystem = false;
        if (equip != null && equip.size() > 0) {
            BuildingTuners.getInstance().addBuildingTunerEquip();
            Equip eq = new Equip.Builder().setHashMap(equip).build();
            CcuLog.d(L.TAG_CCU, "Load SystemEquip " + eq.getDisplayName() + " System profile " + eq.getProfile());
            switch (ProfileType.valueOf(eq.getProfile())) {
                case SYSTEM_VAV_ANALOG_RTU:
                    L.ccu().systemProfile = new VavFullyModulatingRtu();
                    break;
                case SYSTEM_VAV_STAGED_RTU:
                    L.ccu().systemProfile = new VavStagedRtu();
                    break;
                case SYSTEM_VAV_STAGED_VFD_RTU:
                    L.ccu().systemProfile = new VavStagedRtuWithVfd();
                    break;
                case SYSTEM_VAV_HYBRID_RTU:
                    L.ccu().systemProfile = new VavAdvancedHybridRtu();
                    break;
                case SYSTEM_VAV_IE_RTU:
                    L.ccu().systemProfile = new VavIERtu();
                    break;
                case SYSTEM_VAV_BACNET_RTU:
                    L.ccu().systemProfile = new VavBacnetRtu();
                    break;
                case SYSTEM_DAB_ANALOG_RTU:
                    L.ccu().systemProfile = new DabFullyModulatingRtu();
                    break;
                case SYSTEM_DAB_STAGED_RTU:
                    L.ccu().systemProfile = new DabStagedRtu();
                    break;
                case SYSTEM_DAB_STAGED_VFD_RTU:
                    L.ccu().systemProfile = new DabStagedRtuWithVfd();
                    break;
                case SYSTEM_DAB_HYBRID_RTU:
                    L.ccu().systemProfile = new DabAdvancedHybridRtu();
                    break;
                default:
                    L.ccu().systemProfile = new DefaultSystem();
                    isDefaultSystem = true;
                    break;
            }
        } else {
            CcuLog.d(L.TAG_CCU, "System Equip does not exist.Create Dafault System Profile");
            L.ccu().systemProfile = new DefaultSystem();
            isDefaultSystem = true;

        }
        if(!isDefaultSystem)
            L.ccu().systemProfile.addSystemEquip();
        
        for (Floor f : HSUtil.getFloors()) {
            for (Zone z : HSUtil.getZones(f.getId())) {
                for (Equip eq : HSUtil.getEquips(z.getId())) {
                    CcuLog.d(L.TAG_CCU, "Load Equip " + eq.getDisplayName() + " profile : " + eq.getProfile());
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
                        case DAB:
                            DabProfile dab = new DabProfile();
                            dab.addDabEquip(Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(dab);
                            break;
                        case DUAL_DUCT:
                            DualDuctProfile dualDuct = new DualDuctProfile();
                            dualDuct.addDualDuctEquip(Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(dualDuct);
                            break;
                        case PLC:
                            PlcProfile plc = new PlcProfile();
                            plc.addPlcEquip(Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(plc);
                            break;
                        case EMR:
                            EmrProfile emr = new EmrProfile();
                            emr.addEmrEquip(Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(emr);
                            break;
                        case TEMP_INFLUENCE:
                            CazProfile caz = new CazProfile();
                            caz.addCcuAsZoneEquip(Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(caz);
                            break;
                        case SSE:
                            SingleStageProfile sse = new SingleStageProfile();
                            sse.addSSEEquip(Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(sse);
                            break;
                        case SMARTSTAT_CONVENTIONAL_PACK_UNIT:
                            ConventionalUnitProfile cpu = new ConventionalUnitProfile();
                            cpu.addLogicalMap(Short.valueOf(eq.getGroup()), z.getId());
                            L.ccu().zoneProfiles.add(cpu);
                            break;
                        case SMARTSTAT_HEAT_PUMP_UNIT:
                            HeatPumpUnitProfile hpu = new HeatPumpUnitProfile();
                            hpu.addLogicalMap(Short.valueOf(eq.getGroup()), z.getId());
                            L.ccu().zoneProfiles.add(hpu);
                            break;
                        case SMARTSTAT_TWO_PIPE_FCU:
                            TwoPipeFanCoilUnitProfile twoPfcu = new TwoPipeFanCoilUnitProfile();
                            twoPfcu.addLogicalMap(Short.valueOf(eq.getGroup()), z.getId());
                            L.ccu().zoneProfiles.add(twoPfcu);
                            break;
                        case SMARTSTAT_FOUR_PIPE_FCU:
                            FourPipeFanCoilUnitProfile fourPfcu = new FourPipeFanCoilUnitProfile();
                            fourPfcu.addLogicalMap(Short.valueOf(eq.getGroup()), z.getId());
                            L.ccu().zoneProfiles.add(fourPfcu);
                            break;
                        case MODBUS_PAC:
                        case MODBUS_RRS:
                        case MODBUS_VRF:
                        case MODBUS_UPS30:
                        case MODBUS_UPS80:
                        case MODBUS_UPS400:
                        case MODBUS_WLD:
                        case MODBUS_EM:
                        case MODBUS_EMS:
                        case MODBUS_ATS:
                            ModbusProfile mbProfile = new ModbusProfile();
                            mbProfile.addMbEquip(Short.valueOf(eq.getGroup()), ProfileType.valueOf(eq.getProfile()));
                            L.ccu().zoneProfiles.add(mbProfile);
                            break;
                            
                    }
                }
            }

        }
    
        HashMap oaoEquip = CCUHsApi.getInstance().read("equip and oao");
        if (oaoEquip != null && oaoEquip.size() > 0)
        {
            CcuLog.d(L.TAG_CCU, "Create Dafault OAO Profile");
            OAOProfile oao = new OAOProfile();
            oao.addOaoEquip(Short.parseShort(oaoEquip.get("group").toString()));
            L.ccu().oaoProfile = oao;
        }
        
    }

    public String getSmartNodeBand() {
        HashMap device = CCUHsApi.getInstance().read("device and addr");
        if (device != null && device.size() > 0 && device.get("modbus") == null) {
            String nodeAdd = device.get("addr").toString();
            return nodeAdd.substring(0, 2).concat("00");
        } else {
            HashMap band = CCUHsApi.getInstance().read("point and snband");
            if (band != null && band.size() > 0) {
                return band.get("val").toString();
            }
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
