package a75f.io.logic;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.client.HClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import a75f.io.alerts.AlertManager;
import a75f.io.alerts.AlertProcessJob;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.RestoreCCUHsApi;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.data.message.MessageDbUtilKt;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.devices.CCUDevice;
import a75f.io.domain.logic.CCUDeviceBuilder;
import a75f.io.domain.logic.DomainManager;
import a75f.io.domain.migration.DiffManger;
import a75f.io.domain.util.ModelCache;
import a75f.io.logger.CcuLog;
import a75f.io.logic.autocommission.AutoCommissioningState;
import a75f.io.logic.autocommission.AutoCommissioningUtil;
import a75f.io.logic.bo.building.CCUApplication;
import a75f.io.logic.bo.building.bacnet.BacnetProfile;
import a75f.io.logic.bo.building.bypassdamper.BypassDamperProfile;
import a75f.io.logic.bo.building.ccu.TIProfile;
import a75f.io.logic.bo.building.dab.DabProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.dualduct.DualDuctProfile;
import a75f.io.logic.bo.building.erm.EmrProfile;
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.HyperStatCpuProfile;
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HyperStatHpuProfile;
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2Profile;
import a75f.io.logic.bo.building.hyperstatmonitoring.HyperStatV2MonitoringProfile;
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconProfile;
import a75f.io.logic.bo.building.modbus.ModbusProfile;
import a75f.io.logic.bo.building.mystat.profiles.fancoilunit.pipe2.MyStatPipe2Profile;
import a75f.io.logic.bo.building.mystat.profiles.packageunit.cpu.MyStatCpuProfile;
import a75f.io.logic.bo.building.mystat.profiles.packageunit.hpu.MyStatHpuProfile;
import a75f.io.logic.bo.building.oao.OAOProfile;
import a75f.io.logic.bo.building.otn.OTNProfile;
import a75f.io.logic.bo.building.plc.PlcProfile;
import a75f.io.logic.bo.building.ss2pfcu.TwoPipeFanCoilUnitProfile;
import a75f.io.logic.bo.building.ss4pfcu.FourPipeFanCoilUnitProfile;
import a75f.io.logic.bo.building.sscpu.ConventionalUnitProfile;
import a75f.io.logic.bo.building.sse.SingleStageProfile;
import a75f.io.logic.bo.building.sshpu.HeatPumpUnitProfile;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu;
import a75f.io.logic.bo.building.system.dab.DabAdvancedHybridRtu;
import a75f.io.logic.bo.building.system.dab.DabExternalAhu;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtuWithVfd;
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu;
import a75f.io.logic.bo.building.system.vav.VavAdvancedHybridRtu;
import a75f.io.logic.bo.building.system.vav.VavBacnetRtu;
import a75f.io.logic.bo.building.system.vav.VavExternalAhu;
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu;
import a75f.io.logic.bo.building.system.vav.VavIERtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtuWithVfd;
import a75f.io.logic.bo.building.vav.VavAcbProfile;
import a75f.io.logic.bo.building.vav.VavParallelFanProfile;
import a75f.io.logic.bo.building.vav.VavReheatProfile;
import a75f.io.logic.bo.building.vav.VavSeriesFanProfile;
import a75f.io.logic.bo.building.vrv.VrvProfile;
import a75f.io.logic.bo.util.DesiredTempDisplayMode;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.logic.cloud.RenatusServicesUrls;
import a75f.io.logic.filesystem.FileSystemTools;
import a75f.io.logic.jobs.BuildingProcessJob;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.jobs.bearertoken.BearerTokenManager;
import a75f.io.logic.migration.MigrationHandler;
import a75f.io.logic.tuners.TunerEquip;
import a75f.io.logic.util.CCUProxySettings;
import a75f.io.logic.util.MigrationUtil;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.logic.watchdog.Watchdog;
import a75f.io.util.ExecutorTask;

/*
    This is used to keep track of global static associated with application context.
 */
public class Globals {
    private static final String RESTART_CCU = "restart_ccu";
    private static final String RESTART_TABLET = "restart_tablet";
    public static final String DOMAIN_MODEL_SF = "domain_model_sf";
    public int selectedTab;

    public static final class IntentActions {
        public static final String LSERIAL_MESSAGE_OTA = "a75f.io.intent.action.LSERIAL_MESSAGE_OTA";
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
    private static final int TASK_SEPARATION = 30;
    private static final TimeUnit TASK_SEPARATION_TIMEUNIT = TimeUnit.SECONDS;

    private static int DEFAULT_HEARTBEAT_INTERVAL = 60;

    private static Globals globals;
    BuildingProcessJob mProcessJob = new BuildingProcessJob();
    ScheduleProcessJob mScheduleProcessJob = new ScheduleProcessJob();

    AlertProcessJob mAlertProcessJob;

    private ScheduledExecutorService taskExecutor;
    private Context mApplicationContext;
    private CCUApplication mCCUApplication;
    private boolean testHarness = true;

    private boolean _siteAlreadyCreated;

    private boolean isTempOverride = false;
    private int tempOverCount = 0;

    private long ccuUpdateTriggerTimeToken;

    private boolean recoveryMode = false;
    private boolean isInitCompleted = false;
    private SharedPreferences modelSharedPref = null;


    private final List<OnCcuInitCompletedListener> initCompletedListeners = new ArrayList<>();
    public LandingActivityListener landingActivityListener = null;
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

    public boolean isTemporaryOverrideMode() {
        return isTempOverride;
    }

    public void setTemporaryOverrideMode(boolean isTemporaryOverrideMode) {
        isTempOverride = isTemporaryOverrideMode;
    }

    public int gettempOverCount() {
        return tempOverCount;
    }

    public void incrementTempOverCount() {
        tempOverCount++;
    }

    public void resetTempOverCount() {
        tempOverCount = 0;
    }

    public boolean isTestMode() {
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

    private RenatusServicesUrls renatusServicesUrls;

    public void initilize() {
        CcuLog.i(L.TAG_CCU_INIT, "Globals Initialize");
        taskExecutor = Executors.newScheduledThreadPool(NUMBER_OF_CYCLICAL_TASKS_RENATUS_REQUIRES);
        //5 seconds after application initializes start heart beat
        testHarness = getApplicationContext().getResources().getBoolean(R.bool.test_harness);
        RenatusServicesEnvironment servicesEnv = RenatusServicesEnvironment.createWithSharedPrefs(
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        RenatusServicesUrls urls = servicesEnv.getUrls();
        CcuLog.i(L.TAG_CCU_INIT, "Initialize Haystack");
        renatusServicesUrls = urls;
        CCUHsApi hsApi = new CCUHsApi(this.mApplicationContext, urls.getHaystackUrl(), urls.getCaretakerUrl(), urls.getGatewayUrl());
    }

    public void startTimerTask() {

        new RestoreCCUHsApi();
        PreferenceUtil.setContext(this.mApplicationContext);
        CCUHsApi.getInstance().testHarnessEnabled = testHarness;
        AlertManager.getInstance(mApplicationContext, renatusServicesUrls.getAlertsUrl()).initiateAlertOperations(getScheduledThreadPool());

        //set SN address band
        try {
            String addrBand = getSmartNodeBand();
            L.ccu().setAddressBand(addrBand == null ? 1000 : Short.parseShort(addrBand));
        } catch (NumberFormatException e) {
            CcuLog.i(L.TAG_CCU_INIT, "Failerd to read device address band ", e);
            L.ccu().setAddressBand((short) 1000);
        }
        CCUHsApi.getInstance().trimObjectBoxHisStore();
        importTunersAndScheduleJobs();
        handleAutoCommissioning();
        setRecoveryMode();

        MessageDbUtilKt.updateAllRemoteCommandsHandled(getApplicationContext(), RESTART_CCU);
        MessageDbUtilKt.updateAllRemoteCommandsHandled(getApplicationContext(), RESTART_TABLET);
        CCUProxySettings.setUpProxySettingsIfExists();
    }


    private void importTunersAndScheduleJobs() {
        ExecutorTask.executeBackground(() -> {
            MigrationHandler migrationHandler = new MigrationHandler(CCUHsApi.getInstance());
            try {
                CcuLog.i(L.TAG_CCU_INIT, "Run Migrations");
                ModelCache.INSTANCE.init(mApplicationContext, CCUHsApi.getInstance());
                HashMap<Object, Object> site = CCUHsApi.getInstance().readEntity("site");
                if (!isSafeMode()) {
                    migrationHandler.doMigration();
                    MigrationUtil.doMigrationTasksIfRequired();
                    CcuLog.i(L.TAG_CCU_INIT, "Load Profiles");
                    isInitCompleted = true;
                    Site siteObject = new Site.Builder().setHashMap(site).build();
                    CCUHsApi.getInstance().importNamedSchedulebySite(new HClient(CCUHsApi.getInstance().getHSUrl(),
                            HayStackConstants.USER, HayStackConstants.PASS), siteObject);
                }
                CcuLog.i(L.TAG_CCU_INIT, "Schedule Jobs");
                //TunerUpgrades.migrateAutoAwaySetbackTuner(CCUHsApi.getInstance());

                modelMigration(migrationHandler);
                migrationHandler.doPostModelMigrationTasks();

                /*Below migration scripts should be handled after model migration*/
                migrationHandler.temperatureModeMigration();
                /*checkBacnetIdMigrationRequired migration script will update source model version
                 of system Equip, This will affect DM TO DM migration*/
                migrationHandler.checkBacnetIdMigrationRequired();
                migrationHandler.removeRedundantDevicePoints();

                CcuLog.i(L.TAG_CCU_INIT, "Init Watchdog");
                Watchdog.getInstance().addMonitor(mProcessJob);
                Watchdog.getInstance().addMonitor(mScheduleProcessJob);
                Watchdog.getInstance().start();

                migrationHandler.initAddressBand();
            } catch (Exception e) {
                //Catch ignoring any exception here to avoid app from not loading in case of an init failure.
                //Init would retried during next app restart.
                CcuLog.i(L.TAG_CCU_INIT, "Init failed");
                e.printStackTrace();
            } finally {
                CcuLog.i(L.TAG_CCU_INIT, "Init Completed");

                try {
                    loadEquipProfiles();
                } catch (Exception e) {
                    CcuLog.i(L.TAG_CCU_INIT, "Failed to load profiles", e);
                }
                isInitCompleted = true;
                updateTemperatureModeForEquips(); // Update temperature mode fo all equips while app restart
                DEFAULT_HEARTBEAT_INTERVAL = Globals.getInstance().getApplicationContext().getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                        .getInt("control_loop_frequency", 60);
                initCompletedListeners.forEach(OnCcuInitCompletedListener::onInitCompleted);
                mProcessJob.scheduleJob("BuildingProcessJob", DEFAULT_HEARTBEAT_INTERVAL, TASK_SEPARATION, TASK_SEPARATION_TIMEUNIT);
                mScheduleProcessJob.scheduleJob("Schedule Process Job", DEFAULT_HEARTBEAT_INTERVAL, TASK_SEPARATION + 15, TASK_SEPARATION_TIMEUNIT);
                BearerTokenManager.getInstance().scheduleJob();
                updateCCUAhuRef();
            }
        });

        if (isTestMode()) {
            setTestMode(false);
        }
    }

    private void updateTemperatureModeForEquips() {
        CcuLog.d(L.TAG_CCU, "UtilityApplication.updateTemperatureModeForEquips");
        DesiredTempDisplayMode.setSystemModeForVav(CCUHsApi.getInstance());
        DesiredTempDisplayMode.setSystemModeForDab(CCUHsApi.getInstance());
        DesiredTempDisplayMode.setSystemModeForStandaloneProfile(CCUHsApi.getInstance());
    }

    private void modelMigration(MigrationHandler migrationHandler) {
        try {
            String modelsPath = mApplicationContext.getFilesDir().getAbsolutePath() + "/models";
            DiffManger diffManger = new DiffManger(getApplicationContext());
            if (migrationHandler.isMigrationRequired() && CCUHsApi.getInstance().isCCURegistered()) {
                HashMap<Object, Object> site = CCUHsApi.getInstance().readEntity("site");
                modelSharedPref = Globals.getInstance().mApplicationContext
                        .getSharedPreferences(DOMAIN_MODEL_SF, Context.MODE_PRIVATE);
                diffManger.registerOnMigrationCompletedListener(TunerEquip.INSTANCE);
                diffManger.processModelMigration(site.get("id").toString(), modelSharedPref, modelsPath);
                TunerEquip.INSTANCE.initialize(CCUHsApi.getInstance(), false);
                migrationHandler.updateMigrationVersion();
                copyModels();
            }
        } catch (Exception e) {
            //Catch ignoring any exception here to avoid app from not loading in case of an init failure.
            CcuLog.i(L.TAG_CCU_INIT, "modelMigration is failed", e);
            e.printStackTrace();
        }
    }

    public void copyModels() {
        String modelsPath = mApplicationContext.getFilesDir().getAbsolutePath() + "/models";
        FileSystemTools fileSystemTools = new FileSystemTools(getApplicationContext());
        fileSystemTools.createDirectory(modelsPath);
        fileSystemTools.copyModels(Globals.getInstance().getApplicationContext(),
                "assets/75f", modelsPath);
    }


    public void setCCU(CCUApplication CCU) {
        this.mCCUApplication = CCU;
    }

    public void saveTags() {
        CCUHsApi.getInstance().saveTagsData();
    }

    public void loadEquipProfiles() {
        HashMap<Object, Object> site = CCUHsApi.getInstance().readEntity(Tags.SITE);
        if (site == null || site.size() == 0) {
            CcuLog.d(L.TAG_CCU, "Site does not exist. Profiles not loaded");
            return;
        }
        HashMap<Object, Object> equip = CCUHsApi.getInstance().readEntity("equip and system and not modbus and not connectModule");
        DomainManager.INSTANCE.buildDomain(CCUHsApi.getInstance());

        boolean isDefaultSystem = false;
        if (equip != null && equip.size() > 0) {
            //BuildingTuners.getInstance().addBuildingTunerEquip();
            Equip eq = new Equip.Builder().setHashMap(equip).build();
            CcuLog.d(L.TAG_CCU, "Load SystemEquip " + eq.getDisplayName() + " System profile " + eq.getProfile());
            if (eq.getProfile().equals("vavStagedRtu")) {
                L.ccu().systemProfile = new VavStagedRtu();
            } else if (eq.getProfile().equals("vavStagedRtuVfdFan")) {
                L.ccu().systemProfile = new VavStagedRtuWithVfd();
            } else if (eq.getProfile().equals("vavAdvancedHybridAhuV2")) {
                L.ccu().systemProfile = new VavAdvancedAhu();
            } else if (eq.getProfile().equals("dabAdvancedHybridAhuV2")) {
                L.ccu().systemProfile = new DabAdvancedAhu();
            }
            else if (eq.getProfile().equals("vavFullyModulatingAhu")) {
                L.ccu().systemProfile = new VavFullyModulatingRtu();
            } else if (eq.getProfile().equals("dabStagedRtu")) {
                L.ccu().systemProfile = new DabStagedRtu();
            } else if (eq.getProfile().equals("dabStagedRtuVfdFan")) {
                L.ccu().systemProfile = new DabStagedRtuWithVfd();
            } else if (eq.getProfile().equals("dabFullyModulatingAhu")) {
                L.ccu().systemProfile = new DabFullyModulatingRtu();
            } else {

                switch (ProfileType.valueOf(getDomainSafeProfile(eq.getProfile()))) {
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
                    case dabExternalAHUController:
                        L.ccu().systemProfile = new DabExternalAhu();
                        break;
                    case vavExternalAHUController:
                        L.ccu().systemProfile = new VavExternalAhu();
                        break;
                    default:
                        L.ccu().systemProfile = new DefaultSystem();
                        isDefaultSystem = true;
                        break;
                }
            }
        } else {
            CcuLog.d(L.TAG_CCU, "System Equip does not exist.Create Default System Profile");
            L.ccu().systemProfile = new DefaultSystem();
            isDefaultSystem = true;
        }
        if (!isDefaultSystem)
            L.ccu().systemProfile.addSystemEquip();

        for (Floor f : HSUtil.getFloors()) {
            for (Zone z : HSUtil.getZones(f.getId())) {
                for (Equip eq : HSUtil.getEquips(z.getId())) {
                    CcuLog.d(L.TAG_CCU, "Load Equip " + eq.getDisplayName() + " profile : " + eq.getProfile());
                    switch (ProfileType.valueOf(getDomainSafeProfile(eq.getProfile()))) {
                        case VAV_REHEAT:
                            VavReheatProfile vr = new VavReheatProfile(eq.getId(), Short.parseShort(eq.getGroup()));
                            L.ccu().zoneProfiles.add(vr);
                            break;
                        case VAV_SERIES_FAN:
                            VavSeriesFanProfile vsf = new VavSeriesFanProfile(eq.getId(), Short.parseShort(eq.getGroup()));
                            L.ccu().zoneProfiles.add(vsf);
                            break;
                        case VAV_PARALLEL_FAN:
                            VavParallelFanProfile vpf = new VavParallelFanProfile(eq.getId(), Short.parseShort(eq.getGroup()));
                            L.ccu().zoneProfiles.add(vpf);
                            break;
                        case VAV_ACB:
                            VavAcbProfile acb = new VavAcbProfile(eq.getId(), Short.parseShort(eq.getGroup()));
                            L.ccu().zoneProfiles.add(acb);
                            break;
                        case DAB:
                            DabProfile dab = new DabProfile(Short.parseShort(eq.getGroup()));
                            L.ccu().zoneProfiles.add(dab);
                            break;
                        case DUAL_DUCT:
                            DualDuctProfile dualDuct = new DualDuctProfile();
                            dualDuct.addDualDuctEquip(Short.parseShort(eq.getGroup()));
                            L.ccu().zoneProfiles.add(dualDuct);
                            break;
                        case PLC:
                            PlcProfile plc = new PlcProfile(Short.parseShort(eq.getGroup()), eq.getId());
                            plc.init();
                            L.ccu().zoneProfiles.add(plc);
                            break;
                        case EMR:
                            EmrProfile emr = new EmrProfile();
                            emr.addEmrEquip(Short.parseShort(eq.getGroup()));
                            L.ccu().zoneProfiles.add(emr);
                            break;
                        case TEMP_INFLUENCE:
                            TIProfile caz = new TIProfile(eq.getId(),Short.parseShort(eq.getGroup()));
                            L.ccu().zoneProfiles.add(caz);
                            break;
                        case SSE:
                            SingleStageProfile sse = new SingleStageProfile();
                            sse.addSSEEquip(Short.parseShort(eq.getGroup()));
                            L.ccu().zoneProfiles.add(sse);
                            break;
                        case SMARTSTAT_CONVENTIONAL_PACK_UNIT:
                            ConventionalUnitProfile cpu = new ConventionalUnitProfile();
                            cpu.addLogicalMap(Short.parseShort(eq.getGroup()), z.getId());
                            L.ccu().zoneProfiles.add(cpu);
                            break;
                        case SMARTSTAT_HEAT_PUMP_UNIT:
                            HeatPumpUnitProfile hpu = new HeatPumpUnitProfile();
                            hpu.addLogicalMap(Short.parseShort(eq.getGroup()), z.getId());
                            L.ccu().zoneProfiles.add(hpu);
                            break;
                        case SMARTSTAT_TWO_PIPE_FCU:
                            TwoPipeFanCoilUnitProfile twoPfcu = new TwoPipeFanCoilUnitProfile();
                            twoPfcu.addLogicalMap(Short.parseShort(eq.getGroup()), z.getId());
                            L.ccu().zoneProfiles.add(twoPfcu);
                            break;
                        case SMARTSTAT_FOUR_PIPE_FCU:
                            FourPipeFanCoilUnitProfile fourPfcu = new FourPipeFanCoilUnitProfile();
                            fourPfcu.addLogicalMap(Short.parseShort(eq.getGroup()), z.getId());
                            L.ccu().zoneProfiles.add(fourPfcu);
                            break;
                        case HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT:
                            HyperStatCpuProfile cpuProfile = new HyperStatCpuProfile();
                            cpuProfile.addEquip(eq.getId());
                            L.ccu().zoneProfiles.add(cpuProfile);
                            break;
                        case HYPERSTAT_HEAT_PUMP_UNIT:
                            HyperStatHpuProfile hpuProfile = new HyperStatHpuProfile();
                            hpuProfile.addEquip(eq.getId());
                            L.ccu().zoneProfiles.add(hpuProfile);
                            break;

                        case HYPERSTAT_TWO_PIPE_FCU:
                            HyperStatPipe2Profile pipe2Profile = new HyperStatPipe2Profile();
                            pipe2Profile.addEquip(eq.getId());
                            L.ccu().zoneProfiles.add(pipe2Profile);
                            break;

                        case HYPERSTAT_MONITORING:
                            HyperStatV2MonitoringProfile hyperStatMonitoringProfile = new HyperStatV2MonitoringProfile(eq.getId(), Short.parseShort(eq.getGroup()));
                            hyperStatMonitoringProfile.addHyperStatMonitoringEquip();
                            L.ccu().zoneProfiles.add(hyperStatMonitoringProfile);
                            break;
                        case OTN:
                            OTNProfile otnProfile = new OTNProfile(eq.getId(), Short.parseShort(eq.getGroup()));
                            L.ccu().zoneProfiles.add(otnProfile);
                            break;
                        case HYPERSTAT_VRV:
                            VrvProfile vrv = new VrvProfile();
                            vrv.addEquip(CCUHsApi.getInstance(), Short.parseShort(eq.getGroup()));
                            L.ccu().zoneProfiles.add(vrv);
                            break;

                        case HYPERSTATSPLIT_CPU:
                            HyperStatSplitCpuEconProfile cpuEcon = new HyperStatSplitCpuEconProfile(eq.getId(), Short.parseShort(eq.getGroup()));
                            L.ccu().zoneProfiles.add(cpuEcon);
                            break;

                        case MYSTAT_CPU:
                            MyStatCpuProfile mystatCpuProfile = new MyStatCpuProfile();
                            mystatCpuProfile.addEquip(eq.getId());
                            L.ccu().zoneProfiles.add(mystatCpuProfile);
                            break;
                        case MYSTAT_PIPE2:
                            MyStatPipe2Profile mystatPipe2Profile = new MyStatPipe2Profile();
                            mystatPipe2Profile.addEquip(eq.getId());
                            L.ccu().zoneProfiles.add(mystatPipe2Profile);
                            break;
                        case MYSTAT_HPU:
                            MyStatHpuProfile mystatHpuProfile = new MyStatHpuProfile();
                            mystatHpuProfile.addEquip(eq.getId());
                            L.ccu().zoneProfiles.add(mystatHpuProfile);
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
                            mbProfile.addMbEquip(Short.parseShort(eq.getGroup()), ProfileType.valueOf(eq.getProfile()));
                            L.ccu().zoneProfiles.add(mbProfile);
                            break;
                        case BACNET_DEFAULT:
                            BacnetProfile bacnetProfile = new BacnetProfile();
                            bacnetProfile.addBacAppEquip(Long.parseLong(eq.getGroup()), ProfileType.valueOf(eq.getProfile()));
                            L.ccu().zoneProfiles.add(bacnetProfile);
                            break;

                    }
                }
            }

        }

        HashMap<Object, Object> oaoEquip = CCUHsApi.getInstance().readEntity("equip and oao and not hyperstatsplit");
        if (oaoEquip != null && oaoEquip.size() > 0) {
            CcuLog.d(L.TAG_CCU, "Create Default OAO Profile");
            OAOProfile oao = new OAOProfile();
            oao.addOAOEquip(oaoEquip.get("id").toString()
                    , Short.parseShort(oaoEquip.get("group").toString())
                    , ProfileType.OAO);
            L.ccu().oaoProfile = oao;
        }

        HashMap<Object, Object> bypassDamperEquip = CCUHsApi.getInstance().readEntity("equip and domainName == \"" + DomainName.smartnodeBypassDamper + "\"");
        if (bypassDamperEquip != null && bypassDamperEquip.size() > 0) {
            CcuLog.d(L.TAG_CCU, "Create Default Bypass Damper Profile");
            L.ccu().bypassDamperProfile = new BypassDamperProfile(bypassDamperEquip.get("id").toString(), Short.parseShort(bypassDamperEquip.get("group").toString()));
        }

        /*
         * Get all the default BTU_Meter profile details
         */
        ArrayList<HashMap<Object, Object>> equips = CCUHsApi.getInstance().readAllEntities("equip and btu");

        for (HashMap<Object, Object> m : equips) {
            ModbusProfile mbProfile = new ModbusProfile();
            short address = Short.parseShort(m.get("group").toString());
            mbProfile.addMbEquip(address, ProfileType.MODBUS_BTU);
            L.ccu().zoneProfiles.add(mbProfile);
        }

        /*
         * Get system level EMR profile details
         */
        ArrayList<HashMap<Object, Object>> emEquips = CCUHsApi.getInstance().readAllEntities("equip and emr and modbus" +
                " and not zone");

        for (HashMap<Object, Object> m : emEquips) {
            ModbusProfile mbProfile = new ModbusProfile();
            short address = Short.parseShort(m.get("group").toString());
            mbProfile.addMbEquip(address, ProfileType.MODBUS_EMR);
            L.ccu().zoneProfiles.add(mbProfile);
        }
    }


    private String getDomainSafeProfile(String profile) {
        switch (profile) {
            case DomainName.vavReheatNoFan:
                return ProfileType.VAV_REHEAT.name();
            case DomainName.vavReheatParallelFan:
                return ProfileType.VAV_PARALLEL_FAN.name();
            case DomainName.vavReheatSeriesFan:
                return ProfileType.VAV_SERIES_FAN.name();
            case DomainName.activeChilledBeam:
                return ProfileType.VAV_ACB.name();
            case DomainName.hyperstatSplitCPU:
                return ProfileType.HYPERSTATSPLIT_CPU.name();
            case DomainName.smartnodePID:
            case DomainName.helionodePID:
                return ProfileType.PLC.name();
            default:
                return profile;
        }
    }

    public String getSmartNodeBand() {
        HashMap<Object, Object> device = CCUHsApi.getInstance().readEntity("device and node and addr");
        CcuLog.i(Domain.LOG_TAG, "Deviceband " + device);
        if (device != null && device.size() > 0 && device.get("modbus") == null && device.get("addr") != null) {
            String nodeAdd = device.get("addr").toString();
            return nodeAdd.substring(0, nodeAdd.length() - 2).concat("00");
        } else {
            HashMap<Object, Object> addressBand = (HashMap<Object, Object>) Domain.readPoint(DomainName.addressBand);
            CcuLog.i(Domain.LOG_TAG, "AddressBand fetching from point" + addressBand);
            if (addressBand != null && addressBand.size() > 0) {

                if (addressBand.get("val") != null) {
                    return String.valueOf(addressBand.get("val"));
                }

                return String.valueOf((int) CCUHsApi.getInstance().
                        readDefaultValById(addressBand.get("id").toString()).doubleValue());
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

    public void setRecoveryMode() {
        recoveryMode = SystemProperties.getInt("renatus_recovery", 0) > 0;
    }

    public boolean isRecoveryMode() {
        return recoveryMode;
    }

    public boolean isSafeMode() {
        HashMap<Object, Object> safeModeObj = CCUHsApi.getInstance().readEntity("safe and mode");
        if (!safeModeObj.isEmpty()) {
            return CCUHsApi.getInstance().readHisValById(safeModeObj.get("id").toString()) == 1;
        }
        return false;
    }

    public boolean getBuildingProcessStatus() {
        return mProcessJob.getStatus();
    }

    /**
     * Below method ensures systemEquip Id is mapped to ahuRef
     */
    private void updateCCUAhuRef() {
        HashMap<Object, Object> ccuDevice = CCUHsApi.getInstance().readEntity("device and ccu");
        HashMap<Object, Object> systemProfile = CCUHsApi.getInstance().readEntity("system and profile and not modbus and not connectModule");

        if (systemProfile.isEmpty() || ccuDevice.isEmpty()) {
            return;
        }

        String ahuRef = ccuDevice.get("ahuRef").toString();
        String systemProf = systemProfile.get("id").toString();

        if (!(systemProf.equals(ahuRef))) {
            CCUDeviceBuilder ccuDeviceBuilder = new CCUDeviceBuilder();
            CCUDevice ccuDeviceObj = Domain.ccuDevice;
            ccuDeviceBuilder.buildCCUDevice(ccuDeviceObj.getEquipRef(), ccuDeviceObj.getSiteRef(), ccuDeviceObj.getCcuDisName(),
                    ccuDeviceObj.getInstallerEmail(), ccuDeviceObj.getManagerEmail(),
                    systemProf, true);
        }
    }

    private void handleAutoCommissioning() {
        String autoCommissioningPointId = AutoCommissioningUtil.getAutoCommissioningPointId();
        if (autoCommissioningPointId != null && AutoCommissioningUtil.isAutoCommissioningStarted()) {
            long scheduledStopDatetimeInMillis = PreferenceUtil.getScheduledStopDatetime(AutoCommissioningUtil.SCHEDULEDSTOPDATETIME);
            AutoCommissioningUtil.handleAutoCommissioningState(scheduledStopDatetimeInMillis);
        }

        if (AutoCommissioningUtil.getAutoCommissionState() == AutoCommissioningState.ABORTED ||
                AutoCommissioningUtil.getAutoCommissionState() == AutoCommissioningState.COMPLETED) {
            CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(autoCommissioningPointId),
                    HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make((double) AutoCommissioningState.NOT_STARTED.ordinal()), HNum.make(0));
            CCUHsApi.getInstance().writeHisValById(autoCommissioningPointId, (double) AutoCommissioningState.NOT_STARTED.ordinal());
        }
    }

    public interface OnCcuInitCompletedListener {
        void onInitCompleted();
    }

    public void registerOnCcuInitCompletedListener(OnCcuInitCompletedListener listener) {
        initCompletedListeners.add(listener);
        if (isInitCompleted) {
            CcuLog.i("UI_PROFILING", "CCU Already initialized");
            listener.onInitCompleted();
        }
    }

    public void unRegisterOnCcuInitCompletedListener(OnCcuInitCompletedListener listener) {
        initCompletedListeners.remove(listener);
    }

    public int getSelectedTab(){
        return selectedTab;
    }

    public interface LandingActivityListener {
        void onLandingActivityLoaded();
    }

    public void registerLandingActivityListener(LandingActivityListener listener) {
        if (listener != null) {
            landingActivityListener = listener;
        }
    }

    public void unRegisterLandingActivityListener() {
        if (landingActivityListener != null) {
            landingActivityListener = null;
        }
    }
}
