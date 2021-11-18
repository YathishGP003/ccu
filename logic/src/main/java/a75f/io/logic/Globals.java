package a75f.io.logic;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import a75f.io.alerts.AlertProcessJob;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.bo.building.CCUApplication;
import a75f.io.logic.bo.building.bpos.BPOSProfile;
import a75f.io.logic.bo.building.ccu.CazProfile;
import a75f.io.logic.bo.building.dab.DabProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.dualduct.DualDuctProfile;
import a75f.io.logic.bo.building.erm.EmrProfile;
import a75f.io.logic.bo.building.hyperstat.cpu.HyperStatCpuProfile;
import a75f.io.logic.bo.building.hyperstatsense.HyperStatSenseProfile;
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
import a75f.io.logic.bo.building.vrv.VrvProfile;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.logic.cloud.RenatusServicesUrls;
import a75f.io.logic.messaging.MessagingAckJob;
import a75f.io.logic.migration.firmware.FirmwareVersionPointMigration;
import a75f.io.logic.migration.heartbeat.HeartbeatDiagMigration;
import a75f.io.logic.migration.heartbeat.HeartbeatMigration;
import a75f.io.logic.jobs.BuildingProcessJob;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.jobs.bearertoken.BearerTokenManager;
import a75f.io.logic.migration.heartbeat.HeartbeatTagMigration;
import a75f.io.logic.migration.oao.OAODamperOpenReasonMigration;
import a75f.io.logic.messaging.MessagingClient;
import a75f.io.logic.pubnub.PbSubscriptionHandler;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.logic.tuners.TunerUpgrades;
import a75f.io.logic.util.MigrationUtil;
import a75f.io.logic.util.PreferenceUtil;
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

    MessagingAckJob messagingAckJob;

    private ScheduledExecutorService taskExecutor;
    private Context mApplicationContext;
    private CCUApplication mCCUApplication;
    private boolean isSimulation = false;
    private boolean testHarness = true;

    private boolean _siteAlreadyCreated;
    private boolean isTempOverride = false;
    
    private static long ccuUpdateTriggerTimeToken;
    private volatile boolean isCcuReady = false;
    
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
    public boolean isAckdMessagingEnabled() {
        return Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                .getBoolean("ackd_messaging_enabled", true);
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

        //mHeartBeatJob = new HeartBeatJob();
        //5 seconds after application initializes start heart beat
        
        Log.d(L.TAG_CCU_JOB, " Create Process Jobs");
        
        isSimulation = getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                .getBoolean("biskit_mode", false);
        testHarness = getApplicationContext().getResources().getBoolean(R.bool.test_harness);


        RenatusServicesEnvironment servicesEnv = RenatusServicesEnvironment.createWithSharedPrefs(
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        RenatusServicesUrls urls = servicesEnv.getUrls();

        CCUHsApi ccuHsApi = new CCUHsApi(this.mApplicationContext, urls.getHaystackUrl(), urls.getCaretakerUrl());
        PreferenceUtil.setContext(this.mApplicationContext);
        ccuHsApi.testHarnessEnabled = testHarness;

        //set SN address band
        String addrBand = getSmartNodeBand();
        L.ccu().setSmartNodeAddressBand(addrBand == null ? 1000 : Short.parseShort(addrBand));

        importTunersAndScheduleJobs();
    }
    
    private void migrateHeartbeatPointForEquips(HashMap<Object, Object> site){
        if (!site.isEmpty()) {
            HeartbeatMigration.initHeartbeatMigration();
        }
    }

    private void migrateHeartbeatDiagPointForEquips(HashMap<Object, Object> site){
        if (!site.isEmpty()) {
            HeartbeatDiagMigration.initHeartbeatDiagMigration();
        }
    }

    private void migrateHeartbeatwithNewtags(HashMap<Object, Object> site){
        if (!site.isEmpty()) {
            HeartbeatTagMigration.initHeartbeatTagMigration();
        }
    }

    private void OAODamperOpenReasonMigration(HashMap<Object, Object> site){
        if (!site.isEmpty()) {
            OAODamperOpenReasonMigration.initOAOFreeCoolingReasonMigration();
        }
    }

    private void firmwareVersionPointMigration(HashMap<Object, Object> site){
        if (!site.isEmpty()) {
            FirmwareVersionPointMigration.initFirmwareVersionPointMigration();
        }
    }

    private void performBuildingTunerUprades(HashMap<Object, Object> site) {
        //If site already exists , import building tuners from backend before initializing building tuner equip.
        if (!site.isEmpty()) {
            if (CCUHsApi.getInstance().isPrimaryCcu()) {
                        /* Only primary CCUs shall create new tuners created in the upgrade releases and
                        non-primary CCUs should fetch in the next app start up.*/
                BuildingTuners.getInstance().updateBuildingTuners();
            } else {
                        /*If a non-primary tuner fails to load all the  building tuners, it should
                        fall back hard-coded constant tuner values. Creating new tuner instances here will result in
                        multiple CCUs having duplicate instances of tuners. */
                CCUHsApi.getInstance().importBuildingTuners();
            }
            TunerUpgrades.handleBuildingTunerForceClear(mApplicationContext, CCUHsApi.getInstance());
        }
    }

    private void importTunersAndScheduleJobs() {

        new Thread()
        {
            @Override
            public void run()
            {
                HashMap<Object, Object> site = CCUHsApi.getInstance().readEntity("site");
                MigrationUtil.doMigrationTasksIfRequired();
                performBuildingTunerUprades(site);
                migrateHeartbeatPointForEquips(site);
                migrateHeartbeatDiagPointForEquips(site);
                migrateHeartbeatwithNewtags(site);
                OAODamperOpenReasonMigration(site);
                firmwareVersionPointMigration(site);
                loadEquipProfiles();
            
                if (!PbSubscriptionHandler.getInstance().isPubnubSubscribed())
                {
                    if (!site.isEmpty()) {
                        if (CCUHsApi.getInstance().siteSynced()) {
                            MessagingClient.getInstance().init();
                        }
                    }
                }
            
                mProcessJob.scheduleJob("BuildingProcessJob", DEFAULT_HEARTBEAT_INTERVAL,
                                        TASK_SEPARATION, TASK_SEPARATION_TIMEUNIT);
            
                mScheduleProcessJob.scheduleJob("Schedule Process Job", DEFAULT_HEARTBEAT_INTERVAL,
                                                TASK_SEPARATION +15, TASK_SEPARATION_TIMEUNIT);

                BearerTokenManager.getInstance().scheduleJob();

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

    public void scheduleMessagingAckJob() {
        if (CCUHsApi.getInstance().isCCURegistered() && messagingAckJob == null) {
            String ccuId = CCUHsApi.getInstance().getCcuId().substring(1);
            String messagingUrl = RenatusServicesEnvironment.instance.getUrls().getMessagingUrl();
            String bearerToken = CCUHsApi.getInstance().getJwt();

            messagingAckJob = new MessagingAckJob(ccuId, messagingUrl, bearerToken);
            Globals.getInstance().getScheduledThreadPool().scheduleAtFixedRate(messagingAckJob.getJobRunnable(), TASK_SEPARATION + 30, DEFAULT_HEARTBEAT_INTERVAL, TASK_SEPARATION_TIMEUNIT);
        }
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
                        case HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT:
                            HyperStatCpuProfile cpuProfile = new HyperStatCpuProfile();
                            cpuProfile.addEquip(Short.parseShort(eq.getGroup()));
                            L.ccu().zoneProfiles.add(cpuProfile);
                            break;
                        case HYPERSTAT_SENSE:
                            HyperStatSenseProfile hssense = new HyperStatSenseProfile();
                            hssense.addHyperStatSenseEquip(Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(hssense);
                            break;
                        case BPOS:
                            BPOSProfile bpos = new BPOSProfile();
                            bpos.addBPOSEquip(Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(bpos);
                            break;
                        case HYPERSTAT_VRV:
                            VrvProfile vrv = new VrvProfile();
                            vrv.addEquip(CCUHsApi.getInstance(), Short.valueOf(eq.getGroup()));
                            L.ccu().zoneProfiles.add(vrv);
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
                        case MODBUS_UPS150:
                        case MODBUS_EMR:
                        case MODBUS_BTU:
                        case MODBUS_UPS40K:
                        case MODBUS_UPSL:
                        case MODBUS_UPSV:
                        case MODBUS_UPSVL:
                        case MODBUS_VAV_BACnet:
                        case MODBUS_EMR_ZONE:
                        case MODBUS_DEFAULT:
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


        /**
         * Get all the default BTU_Meter profile details
         */
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and btu");

        for (HashMap m : equips)
        {
            ModbusProfile mbProfile = new ModbusProfile();
            short address =Short.parseShort(m.get("group").toString());
            mbProfile.addMbEquip(Short.valueOf( address), ProfileType.MODBUS_BTU);
            L.ccu().zoneProfiles.add(mbProfile);
        }

        /**
         * Get all the default BTU_Meter profile details
         */
        ArrayList<HashMap> emEquips = CCUHsApi.getInstance().readAll("equip and emr and modbus");

        for (HashMap m : emEquips)
        {
            ModbusProfile mbProfile = new ModbusProfile();
            short address =Short.parseShort(m.get("group").toString());
            mbProfile.addMbEquip(Short.valueOf( address), ProfileType.MODBUS_EMR);
            L.ccu().zoneProfiles.add(mbProfile);
        }
    }

    public String getSmartNodeBand() {
        HashMap device = CCUHsApi.getInstance().read("device and addr");
        if (device != null && device.size() > 0 && device.get("modbus") == null && device.get("addr") != null) {
            String nodeAdd = device.get("addr").toString();
            return nodeAdd.substring(0, nodeAdd.length()-2).concat("00");
        } else {
            HashMap band = CCUHsApi.getInstance().read("point and snband");
            if (band != null && band.size() > 0 && band.get("val") != null) {
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
    
    public void setCcuUpdateTriggerTimeToken(long time) {
        ccuUpdateTriggerTimeToken = time;
    }
    
    public long getCcuUpdateTriggerTimeToken() {
        return ccuUpdateTriggerTimeToken;
    }
    
    public boolean isCcuReady() {
        return isCcuReady;
    }
    public void setCcuReady(boolean ccuReady) {
        isCcuReady = ccuReady;
    }
}
