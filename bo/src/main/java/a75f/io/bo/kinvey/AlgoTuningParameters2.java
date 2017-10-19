package a75f.io.bo.kinvey;

import android.util.Log;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

import org.json.JSONObject;

/**
 * Created by Yinten on 10/14/2017.
 */

@SuppressWarnings("unused")
public class AlgoTuningParameters2 extends GenericJson
{
    
    static final String KINVEY_COLLECTION_NAME = "00AlgoTuningParameters";
    static final int ALGO_TUNERS_VERSION = 62;
    
    static final int ZONE_DEAD_TIME = 15;
    static final int BUILDING_TO_ZONE_DIFF = 3;
    static final int CM_HEART_BEAT_INTERVAL = 1;
    static final int HEART_BEAT_TO_SKIP = 5;
    static final double AUTO_MODE_CHANGE_OVER_MULTIPLER = 1.0;
    static final int AUTO_WAY_TIME = 60;
    static final int REBALANCE_HOLD_TIME = 20;
    static final int FORCED_OCCUPIED_TIME = 120;
    static final int COOLING_AIRFLOW_TEMP = 60;
    static final int HEATING_AIRFLOW_TEMP = 105;
    static final int COOLING_AIRFLOW_TEMP_LOWER_OFFSET = -20;
    static final int COOLING_AIRFLOW_TEMP_UPPER_OFFSET = -8;
    static final int HEATING_AIRFLOW_TEMP_LOWER_OFFSET = 25;
    static final int HEATING_AIRFLOW_TEMP_UPPER_OFFSET = 40;
    static final int AIRFLOW_TEMP_BREACH_HOLD_DOWN = 5;
    static final int HUMIDITY_THRESHOLD = 35;
    static final int HUMIDITY_COMP_FACTOR = 10;
    static final boolean USE_SAME_OCCU_TEMP_ACROSS_DAYS = true;
    static final int DCV_CO2_THRESHOLD_LEVEL = 1000;
    static final int DCV_DAMPER_OPENING_RATE = 10;
    static final int DUMB_MODE_DCV_DAMPER_OPENING = 30;
    static final int ABNORMAL_CUR_TEMP_TRIGGER_VAL = 4;
    static final int USER_LIMIT_SPREAD = 3;
    static final int PRECONDTION_RATE = 15;
    static final double SETBACK_MULTIPLIER = 1.5;
    static final int ENTHALPY_COMPENSATION = 0;
    static final int OUTSIDE_AIR_MIN_TEMP = 0;
    static final int OUTSIDE_AIR_MAX_TEMP = 70;
    static final int OUTSIDE_AIR_MIN_HUMIDITY = 10;
    static final int OUTSIDE_AIR_MAX_HUMIDITY = 95;
    static final int ECONOMIZER_LOAD_THRESHOLD = 30;
    static final int ECONOMIZER_HOLDTIME = 15;
    static final int ECONOMIZER_LOAD_DROP = 30;
    
    static final double ANALOG_FAN_SPEED_MULTIPLIER = 1.0;
    static final int ANALOG_MIN_HEATING = 50;
    static final int ANALOG_MAX_HEATING = 20;
    static final int ANALOG_MIN_COOLING = 70;
    static final int ANALOG_MAX_COOLING = 100;
    //default values for algo tuners
    static final double PC_CONST = 0.5; //prpcontrlConstant
    static final double IC_CONST = 0.5; // mIntegCtrlConstant
    static final int CUM_DAMPER_POS_TARGET = 70;
    static final int IC_TIMEOUT = 30;
    static final double PC_SPREAD = 2.0;
    static final double PC_MULTIPLIER = 2.0;
    static final int DXCOIL_HOLD_TIME_MULITPLIER = 2;
    static final int PERCENT_DEAD_ZONES = 50;
    static final double DXCOIL_OFFSET_LIMIT_MULTIPLIER = 0.5;
    static final double DXCOIL_OFFSET_HEAT_OFFSET = 0.0;
    static final double DXCOIL_OFFSET_COOL_OFFSET = 0.0;
    static final int CLOCK_INTERVAL = 15;
    static final double BACK_PRESSURE_LIMIT = 2.5;
    static final int CUM_DAMPERPOS_INC_BP = 5;
    static final int MULTI_ZONE_STAGE2_TIMER = 15;
    static final int SINGLE_ZONE_STAGE2_TIMER = 2;
    static final int MULTI_ZONE_STAGE2_PDROP = 50;
    static final int SINGLE_ZONE_STAGE2_OFFSET = 2;
    static final int IGNORE_CM_REPORT_ERR = 1;
    static final int IGNORE_FSV_NOT_INDBERR = 1;
    static final int SHOW_RPM_ALERTS = 0;
    static final int FORCED_OCCUPIED_ZONE_PRIORITY = 50000;
    static final int ZONE_PRIORITY_SPREAD = 2;
    static final double ZONE_PRIORITY_MULTIPLIER = 1.3;
    static final int ECON_IC_TIMEOUT = 30;
    static final double ECON_PC_SPREAD = 2.0;
    static final double ANALOG1_PC_CONST = 0.5;
    static final double ANALOG1_IC_CONST = 0.5;
    static final int ANALOG1_IC_TIMEOUT = 30;
    static final double ANALOG1_PC_SPREAD = 2.0;
    static final double ANALOG3_PC_CONST = 0.5;
    static final double ANALOG3_IC_CONST = 0.5;
    static final int ANALOG3_IC_TIMEOUT = 30;
    static final double ANALOG3_PC_SPREAD = 2.0;
    static final double AUTO_MODE_COOL_HEAT_DXCILIMIT = 2.0;
    static final double AUTO_MODE_TOTAL_DXCILIMIT = 0.2;
    static final boolean USE_COOLHEAT_DXCI_AUTOMODE = false;
    static final boolean USE_INSTANT_GRATIFICATION_MODE = false;
    static final boolean FOLLOW_AUTO_SCHEDULE = false;
    static final boolean USE_OUTSIDE_TEMP_LOCKOUT = false;
    static final boolean USE_CELSIUS = false;
    static final boolean USE_MILITARY_TIME = false;
    static final boolean SN_INSTALL = false;
    static final int FSV_START_ADDRESS = 1000;
    static final int NO_HEATING_ABOVE = 80;
    static final int NO_COOLING_BELOW = 60;
    static final int ZONE_DUMB_TEMP = 3; //curTemp * 1.5
    static final int STAGE1_FAN_ONTIME = 5;
    static final int STAGE1_FAN_OFFTIME = 0;
    static final int BUILDING_MAX_TEMP = 85; // in deg F
    static final int BUILDING_MIN_TEMP = 60; // in deg F
    static final int USER_MAX_TEMP = 73; //in deg F
    static final int USER_MIN_TEMP = 70; //in def F
    static final int WRM_FOR_HUMIDITY_VALUE = 0;
    static final boolean USE_EXT_HUMIDITY_SENSOR = false;
    static final int EXT_HUMIDITY_SENSOR_MIN_SP = 30;
    static final int EXT_HUMIDITY_SENSOR_MAX_SP = 70;
    static final int EXT_HUMIDITY_SENSOR_SP_TIMEOUT = 60;
    static final int WRM_FOR_DUMB_MODE_TEMP_VALUE = 0;
    static final int NO2_THRESHOLD_LEVEL = 5;
    static final int CO_THRESHOLD_LEVEL = 50;
    static final int NO2_DAMPER_OPENING_RATE = 20;
    static final int CO_DAMPER_OPENING_RATE = 10;
    static final int OAO_DAMPER_POS_MIN = 0;
    static final int OAO_DAMPER_POS_MAX = 100;
    static final int OAO_EXHAUST_FAN_THRESHOLD = 50;
    static final double PRESSURE_THRESHOLD_LEVEL = 0.01;
    static final int PRESSURE_DAMPER_OPENING_RATE = 5;
    static final int ANALOG1_MIN_VALUE = 0;
    static final int ANALOG1_MAX_VALUE = 10;
    static final int ANALOG3_MIN_VALUE = 0;
    static final int ANALOG3_MAX_VALUE = 10;
    static final int COOLING_AIRFLOW_TEMP_STAGE2_LOWER_OFFSET = -25;
    static final int COOLING_AIRFLOW_TEMP_STAGE2_UPPER_OFFSET = -12;
    static final int HEATING_AIRFLOW_TEMP_STAGE2_LOWER_OFFSET = 35;
    static final int HEATING_AIRFLOW_TEMP_STAGE2_UPPER_OFFSET = 50;
    static final int CHILLED_WATER_DELTAT_SETPOINT = 10;
    static final int CHILLED_WATER_FLOW_SETPOINT = 100;
    static final int CHILLED_WATER_MAX_FLOWRATE = 100;
    static final int CHILLED_WATER_ACTUATOR_MIN_POS = 0;
    static final int CHILLED_WATER_ACTUATOR_MAX_POS = 100;
    static final int AHU_FAN_MIN_LIMIT = 0;
    static final int AHU_FAN_MAX_LIMIT = 100;
    static final boolean ENERGY_METER_MONITOR = false;
    static final double PRESSURE_DEAD_ZONE = 0.01;
    static final double REHEAT_OFFSET = 1.0;
    static final int REFRIGERATION_LOWER_LIMIT = 23;
    static final int REFRIGERATION_HIGHER_LIMIT = 45;
    static final double REHEAT_DAMPER_REOPEN_OFFSET = 2.0;
    static final int REHEAT_MAX_DAMPER_POS = 40;
    static final int ALARM_VOLUME_DEFAULT = 0; //Max volume limit is 7;
    static final int ENERGY_METER_SETPOINT = 10;
    static final int LIGHTING_INTENSITY_OCCUPANT_DETECTED = 75;
    static final int MIN_LIGHTING_CONTROL_OVERRIDE_IN_MINUTES = 20;
    static final int STANDALONE_COOLING_DEADBAND= 1;
    static final int STANDALONE_HEATING_DEADBAND = 1;
    
    static AlgoTuningParameters mSelf = new AlgoTuningParameters();
    static public AlgoTuningParameters getHandle() {
        return mSelf;
    }
    
    @Key("_id")
    private String id = DalContext.getInstance().getKinveyId();
    
    @Key("_ccuName")
    private String mCCUName = "";
    
    @Key("_version")
    private int mVersion = -1;
    @Key
    private double mPropCtrlConstant = PC_CONST;
    @Key
    private double mIntegCtrlConstant = IC_CONST;
    @Key
    private double mZoneLoadCtrlConstant = 0;
    @Key
    private double mCoolingRateCtrlConstant = 0;
    @Key
    private int mCumulativeDamperPosTarget = CUM_DAMPER_POS_TARGET;
    
    @Key
    private int mDamperPosForDeadZone = CUM_DAMPER_POS_TARGET;
    @Key
    private int mTimeToDeclareDeadDamper = ZONE_DEAD_TIME; // mins
    @Key
    private int mIntegCtrlTimeOut = IC_TIMEOUT;
    
    @Key
    private double mPropCtrlSpread = PC_SPREAD;
    
    @Key
    private double mPreConditioningPropCtrlMultiplier = PC_MULTIPLIER;
    
    @Key
    private int mDXCoilHoldTimeMultiplier = DXCOIL_HOLD_TIME_MULITPLIER; // mins
    
    @Key
    private int mRebalanceHoldTime = REBALANCE_HOLD_TIME; // mins // 15-60
    
    @Key
    private int mPercentDeadZonesAllowed = PERCENT_DEAD_ZONES;
    
    @Key
    private double mDXCoilOffsetLimitMultiplier = DXCOIL_OFFSET_LIMIT_MULTIPLIER;
    
    @Key
    private double mDXCoilOffsetHeatOffset = DXCOIL_OFFSET_HEAT_OFFSET;
    
    @Key
    private double mDXCoilOffsetCoolOffset = DXCOIL_OFFSET_COOL_OFFSET;
    
    @Key
    private int mClockUpdateInterval = CLOCK_INTERVAL;
    
    @Key
    private int mBuildingToZoneTempLimitsDifferential = BUILDING_TO_ZONE_DIFF; // 3-20
    
    @Key
    private double mBackpressureLimit = BACK_PRESSURE_LIMIT;
    @Key
    private int mCumDamperPosIncrForBackpressure = CUM_DAMPERPOS_INC_BP;
    
    @Key
    private int mMultizoneStage2Timer = MULTI_ZONE_STAGE2_TIMER;
    @Key
    private int mSinglezoneStage2Timer = SINGLE_ZONE_STAGE2_TIMER;
    
    @Key
    private int mMultizoneStage2PercentDrop = MULTI_ZONE_STAGE2_PDROP;
    @Key
    private double mSinglezoneStage2Offset = SINGLE_ZONE_STAGE2_OFFSET;
    
    @Key
    private int mIgnoreCMReportedError = IGNORE_CM_REPORT_ERR;
    
    @Key
    private int mIgnoreFSVNotInDBError = IGNORE_FSV_NOT_INDBERR;
    
    @Key
    private int mHeatingAirflowTemperature = HEATING_AIRFLOW_TEMP; // 90-150
    
    @Key
    private int mCoolingAirflowTemperature = COOLING_AIRFLOW_TEMP; // 35-70 Alarm
    
    @Key
    private int mAirflowTemperatureBreachAlertHoldDown = AIRFLOW_TEMP_BREACH_HOLD_DOWN;
    
    @Key
    private int mHeartBeatInterval = CM_HEART_BEAT_INTERVAL; // 1-20
    
    @Key
    private int mHeartBeatsToSkip = HEART_BEAT_TO_SKIP; // 3-20
    
    @Key
    private double mAutoModeTemperatureMultiplier = AUTO_MODE_CHANGE_OVER_MULTIPLER; // 0.4 - 5
    
    @Key
    private int mAutoAwayTimePeriod = AUTO_WAY_TIME; // 40-300
    
    @Key
    private int mShowRPMAlerts = SHOW_RPM_ALERTS;
    
    @Key
    private int mForcedOccupiedZonePriority = FORCED_OCCUPIED_ZONE_PRIORITY;
    
    @Key
    private int mForcedOccupiedTimePeriod = FORCED_OCCUPIED_TIME; // 30-300
    
    @Key
    private int mHumidityThreshold = HUMIDITY_THRESHOLD ;
    
    @Key
    private int mHumidityPerDegreeFactor = HUMIDITY_COMP_FACTOR;
    
    @Key
    private boolean mUseSameOccuTempAcrossDays = USE_SAME_OCCU_TEMP_ACROSS_DAYS;
    
    @Key
    private int mDCVCO2ThresholdLevel = DCV_CO2_THRESHOLD_LEVEL;
    
    @Key
    private int mDCVDamperOpeningRate = DCV_DAMPER_OPENING_RATE;
    
    @Key
    private int mDumbModeDCVDamperOpening = DUMB_MODE_DCV_DAMPER_OPENING;
    
    @Key
    private int mZonePrioritySpread = ZONE_PRIORITY_SPREAD;
    
    @Key
    private double mZonePriorityMultiplier = ZONE_PRIORITY_MULTIPLIER;
    
    @Key
    private int mAbnormalCurTempChangeAlertTrigger = ABNORMAL_CUR_TEMP_TRIGGER_VAL;
    
    @Key
    private int mUserLimitSpread = USER_LIMIT_SPREAD;
    
    @Key
    private int mPreconditioningRate = PRECONDTION_RATE;
    
    @Key
    private double mSetbackMultiplier = SETBACK_MULTIPLIER;
    
    @Key
    private int mEnthalpyCompensation = ENTHALPY_COMPENSATION;
    
    @Key
    private int mOutsideAirMinTemp = OUTSIDE_AIR_MIN_TEMP;
    
    @Key
    private int mOutsideAirMaxTemp = OUTSIDE_AIR_MAX_TEMP;
    
    @Key
    private int mOutsideAirMinHumidity = OUTSIDE_AIR_MIN_HUMIDITY;
    
    @Key
    private int mOutsideAirMaxHumidity = OUTSIDE_AIR_MAX_HUMIDITY;
    
    @Key
    private int mEconomizerLoadThreshold = ECONOMIZER_LOAD_THRESHOLD;
    
    @Key
    private int mEconomizerHoldTime = ECONOMIZER_HOLDTIME;
    
    @Key
    private int mEconomizerStage1LoadDrop = ECONOMIZER_LOAD_DROP;
    
    @Key
    private int mEconIntegCtrlTimeOut = ECON_IC_TIMEOUT;
    
    @Key
    private double mEconPropCtrlSpread = ECON_PC_SPREAD;
    
    @Key
    private double mAnalog1PropCtrlConstant = ANALOG1_PC_CONST;
    @Key
    private double mAnalog1IntegCtrlConstant = ANALOG1_IC_CONST;
    @Key
    private int mAnalog1IntegCtrlTimeOut = ANALOG1_IC_TIMEOUT;
    @Key
    private double mAnalog1PropCtrlSpread = ANALOG1_PC_SPREAD;
    
    
    @Key
    private double mAnalog3PropCtrlConstant = ANALOG3_PC_CONST;
    @Key
    private double mAnalog3IntegCtrlConstant = ANALOG3_IC_CONST;
    @Key
    private int mAnalog3IntegCtrlTimeOut = ANALOG3_IC_TIMEOUT;
    @Key
    private double mAnalog3PropCtrlSpread = ANALOG3_PC_SPREAD;
    
    @Key
    private double mAnalogFanSpeedMultiplier = ANALOG_FAN_SPEED_MULTIPLIER;
    @Key
    private int mAnalogMinHeating = ANALOG_MIN_HEATING;
    @Key
    private int mAnalogMaxHeating = ANALOG_MAX_HEATING;
    @Key
    private int mAnalogMinCooling = ANALOG_MIN_COOLING;
    @Key
    private int mAnalogMaxCooling = ANALOG_MAX_COOLING;
    
    @Key
    private double mAutoModeCoolHeatDXCILimit = AUTO_MODE_COOL_HEAT_DXCILIMIT;
    
    @Key
    private double mAutoModeTotalDXCILimit = AUTO_MODE_TOTAL_DXCILIMIT;
    
    @Key
    private boolean mUseCoolHeatDXCIForAutoMode = USE_COOLHEAT_DXCI_AUTOMODE;
    
    @Key
    private boolean useCelsius = USE_CELSIUS;
    
    @Key
    private boolean useMilitaryTime = USE_MILITARY_TIME;
    
    @Key
    private boolean useInstantGratificationMode = USE_INSTANT_GRATIFICATION_MODE;
    
    @Key
    private boolean followAutoModeSchedule = FOLLOW_AUTO_SCHEDULE;
    
    @Key
    private boolean useOutsideTemperatureLockout = USE_OUTSIDE_TEMP_LOCKOUT;
    
    @Key
    private int FSVPairingStartAddress = FSV_START_ADDRESS;
    
    @Key
    private int outsideCoolingTempLockout = NO_COOLING_BELOW;
    
    @Key
    private int outsideHeatingTempLockout = NO_HEATING_ABOVE;
    
    @Key
    private int mStage1FanOnTime = STAGE1_FAN_ONTIME;
    
    @Key
    private int mStage1FanOffTime = STAGE1_FAN_OFFTIME;
    
    
    @Key
    private int mBuildingAllowNoHotter = BUILDING_MAX_TEMP;
    @Key
    private int mBuildingAllowNoCooler = BUILDING_MIN_TEMP;
    @Key
    private int mUserAllowNoHotter = USER_MAX_TEMP;
    @Key
    private int mUserAllowNoCooler = USER_MIN_TEMP;
    
    @Key
    private int mWRMForHumidityValue = WRM_FOR_HUMIDITY_VALUE;
    
    @Key
    private boolean mUseExtHumiditySensor = USE_EXT_HUMIDITY_SENSOR;
    
    @Key
    private int mExtHumiditySensorMinSP = EXT_HUMIDITY_SENSOR_MIN_SP;
    
    @Key
    private int mExtHumiditySensorMaxSP = EXT_HUMIDITY_SENSOR_MAX_SP;
    
    @Key
    private int mExtHumiditySPTimeoutInterval = EXT_HUMIDITY_SENSOR_SP_TIMEOUT;
    
    @Key
    private int mWRMForDumbModeTempValue = WRM_FOR_DUMB_MODE_TEMP_VALUE;
    
    @Key
    private int mNO2ThresholdLevel = NO2_THRESHOLD_LEVEL;
    @Key
    private int mCOThresholdLevel = CO_THRESHOLD_LEVEL;
    
    @Key
    private int mNO2DamperOpeningRate = NO2_DAMPER_OPENING_RATE;
    
    @Key
    private int mCODamperOpeningRate = CO_DAMPER_OPENING_RATE;
    @Key
    private int mOAODamperPosMin = OAO_DAMPER_POS_MIN;
    @Key
    private int mOAODamperPosMax = OAO_DAMPER_POS_MAX;
    @Key
    private int mOAOExhaustFanThreshold = OAO_EXHAUST_FAN_THRESHOLD;
    @Key
    private double mPressureThresholdLevel = PRESSURE_THRESHOLD_LEVEL;
    @Key
    private double mPressureDeadZone = PRESSURE_DEAD_ZONE;
    @Key
    private int mPressureDamperOpeningRate = PRESSURE_DAMPER_OPENING_RATE;
    
    @Key
    private int mAnalog1MinValue = ANALOG1_MIN_VALUE;
    @Key
    private int mAnalog1MaxValue = ANALOG1_MAX_VALUE;
    @Key
    private int mAnalog3MinValue = ANALOG3_MIN_VALUE;
    @Key
    private int mAnalog3MaxValue = ANALOG3_MAX_VALUE;
    @Key
    private int mCoolingAirflowTempLowerOffset = COOLING_AIRFLOW_TEMP_LOWER_OFFSET;  // default offset -20
    
    @Key
    private int mCoolingAirflowTempUpperOffset = COOLING_AIRFLOW_TEMP_UPPER_OFFSET; //default offset -8
    
    @Key
    private int mHeatingAirflowTempLowerOffset = HEATING_AIRFLOW_TEMP_LOWER_OFFSET; //default offset 25
    
    @Key
    private int mHeatingAirflowTempUpperOffset = HEATING_AIRFLOW_TEMP_UPPER_OFFSET; //default offset 40
    @Key
    private int mCoolingAirflow2TempLowerOffset = COOLING_AIRFLOW_TEMP_STAGE2_LOWER_OFFSET; //default offset -25
    
    @Key
    private int mCoolingAirflow2TempUpperOffset = COOLING_AIRFLOW_TEMP_STAGE2_UPPER_OFFSET; //default offset -12
    
    @Key
    private int mHeatingAirflow2TempLowerOffset = HEATING_AIRFLOW_TEMP_STAGE2_LOWER_OFFSET; //default offset 35
    
    @Key
    private int mHeatingAirflow2TempUpperOffset = HEATING_AIRFLOW_TEMP_STAGE2_UPPER_OFFSET; //default offset 50
    
    @Key
    private int mChilledWaterDeltaTSetpoint = CHILLED_WATER_DELTAT_SETPOINT; // default 10
    
    @Key
    private int mChilledWaterFlowSetpoint = CHILLED_WATER_FLOW_SETPOINT;
    
    @Key
    private int mChilledWaterMaxFlowRate = CHILLED_WATER_MAX_FLOWRATE;
    
    @Key
    private int mChilledWaterActuatorMinPos = CHILLED_WATER_ACTUATOR_MIN_POS; // default 0
    
    @Key
    private int mChilledWaterActuatorMaxPos = CHILLED_WATER_ACTUATOR_MAX_POS; // default 100
    
    @Key
    private int mAHUFanMinLimit = AHU_FAN_MIN_LIMIT; // default 0
    
    @Key
    private int mAHUFanMaxLimit = AHU_FAN_MAX_LIMIT; // default 100
    
    @Key
    private boolean mSmartNodeInstall = SN_INSTALL;
    
    @Key
    private boolean energyMeterMonitor = ENERGY_METER_MONITOR;
    
    
    @Key
    private double mReheatOffset = REHEAT_OFFSET;
    
    @Key
    private double mReheatDamperReopenOffset = REHEAT_DAMPER_REOPEN_OFFSET;
    
    @Key
    private  int mReheatMaxDamperPos = REHEAT_MAX_DAMPER_POS;
    
    @Key
    private int mRefHighLimit = REFRIGERATION_HIGHER_LIMIT;
    @Key
    private int mRefLowLimit = REFRIGERATION_LOWER_LIMIT;
    
    @Key
    private  int mAlarmVolume = ALARM_VOLUME_DEFAULT;
    @Key
    private int mEnergyMeterSetpoint = ENERGY_METER_SETPOINT;
    
    @Key
    private int mMinLightingControlOverrideInMinutes = MIN_LIGHTING_CONTROL_OVERRIDE_IN_MINUTES;
    @Key
    private int mLightingIntensityForOccupancyDetect = LIGHTING_INTENSITY_OCCUPANT_DETECTED;
    @Key
    private int mStandaloneCoolingDeadband = STANDALONE_COOLING_DEADBAND;
    @Key
    private int mStandaloneHeatingDeadband = STANDALONE_HEATING_DEADBAND;
    private boolean bUploadedToKinvey = false;
    
    public void AlgoTuningParameter() {
    }
//
//    public void load() {
//        loadFromSharedPrefs();
//        loadFromKinvey();
//    }
    
//    public void loadFromSharedPrefs() {
//        SharedPreferences
//                spAlgoSettings =  CCUApp.getAppContext().getSharedPreferences("algo_tuning_vars", 0);
//        setAlgoConstants(spAlgoSettings.getFloat("prop_ctrl_const", (float) getPropConstant()),
//                spAlgoSettings.getFloat("integ_ctrl_const", (float) getIntegConstant()),
//                0,0);
//        setPropCtrlSpread(spAlgoSettings.getFloat("prop_ctrl_range", (float) getPropCtrlSpread()));
//        setPreConditioningPropCtrlMultiplier(spAlgoSettings.getFloat("precond_prop_ctrl_multiplier", (float) getPreConditioningPropCtrlMultiplier()));
//        setIntegCtrlTimeOut(spAlgoSettings.getInt("integ_time_out", getIntegCtrlTimeOut()));
//        setDXCoilHoldTimeMultiplier(spAlgoSettings.getInt("dx_coil_hold_time_multiplier", getDXCoilHoldTimeMultiplier()));
//        setPercentDeadZonesAllowed(spAlgoSettings.getInt("dead_zones_allowed", getPercentDeadZonesAllowed()));
//        setRebalanceHoldTime(spAlgoSettings.getInt("rebalance_hold_time", getRebalanceHoldTime()));
//        setCumulativeDamperPosTarget(spAlgoSettings.getInt("target_cum_damper_pos", getCumulativeDamperPosTarget()));
//        setDamperPosForDeadZone(spAlgoSettings.getInt("dead_zone_damper_pos", getDamperPosForDeadZone()));
//        setTimeToDeclareDeadDamper(spAlgoSettings.getInt("zone_dead_time", getTimeToDeclareDeadDamper()));
//        setDXCoilOffsetLimitMultiplier(spAlgoSettings.getFloat("dxcoil_ci_offset_limit_multiplier", (float) getDXCoilOffsetLimitMultiplier()));
//        setDxCoilOffsetHeatOffset(spAlgoSettings.getFloat("dxcoil_offset_heat_offset", (float) getDxCoilOffsetHeatOffset()));
//        setDXCoilOffsetCoolOffset(spAlgoSettings.getFloat("dxcoil_offset_cool_offset", (float) getDXCoilOffsetCoolOffset()));
//        setClockUpdateInterval(spAlgoSettings.getInt("clock_update_interval", getClockUpdateInterval()));
//        setBuildingToZoneTempLimitsDifferential(spAlgoSettings.getInt("temp_differential", getBuildingToZoneTempLimitsDifferential()));
//        setBackpressureLimit(spAlgoSettings.getFloat("backpressure_limit", (float) getBackpressureLimit()));
//        setCumDamperPosIncrForBackpressure(spAlgoSettings.getInt("damperpos_increment_for_bp", getCumDamperPosIncrForBackpressure()));
//        setMultizoneStage2Timer(spAlgoSettings.getInt("multizone_stage2_timer", getMultizoneStage2Timer()));
//        setSinglezoneStage2Timer(spAlgoSettings.getInt("singlezone_stage2_timer", getSinglezoneStage2Timer()));
//        setMultizoneStage2PercentDrop(spAlgoSettings.getInt("multizone_stage2_percent_drop", getMultizoneStage2PercentDrop()));
//        setSinglezoneStage2Offset(spAlgoSettings.getFloat("singlezone_stage2_offset", (float) getSinglezoneStage2Offset()));
//        setAutoModeTemperatureMultiplier(spAlgoSettings.getFloat("auto_mode_temp_multiplier", (float)getAutoModeTemperatureMultiplier()));
//        setIgnoreCMReportedErrors(spAlgoSettings.getInt("ignore_cm_reported_errors", getIgnoreCMReportedErrors()));
//        setIgnoreFSVNotInDBError(spAlgoSettings.getInt("ignore_fsv_not_in_db_error", getIgnoreFSVNotInDBError()));
//        setHeatingAirflowTemperature(spAlgoSettings.getInt("heating_airflow_temperature", getHeatingAirflowTemperature()));
//        setHeatingAirflowTemperatureLowerOffset(spAlgoSettings.getInt("heating_airflow_temp_lower_offset", getHeatingAirflowTemperatureLowerOffset()));
//        setHeatingAirflowTemperatureUpperOffset(spAlgoSettings.getInt("heating_airflow_temp_upper_offset", getHeatingAirflowTemperatureUpperOffset()));
//        setCoolingAirflowTemperature(spAlgoSettings.getInt("cooling_airflow_temperature", getCoolingAirflowTemperature()));
//        setCoolingAirflowTemperatureLowerOffset(spAlgoSettings.getInt("cooling_airflow_temp_lower_offset", getCoolingAirflowTemperatureLowerOffset()));
//        setCoolingAirflowTemperatureUpperOffset(spAlgoSettings.getInt("cooling_airflow_temp_upper_offset", getCoolingAirflowTemperatureUpperOffset()));
//        setHeatingAirflow2TemperatureLowerOffset(spAlgoSettings.getInt("heating_airflow2_temp_lower_offset", getHeatingAirflow2TemperatureLowerOffset()));
//        setHeatingAirflow2TemperatureUpperOffset(spAlgoSettings.getInt("heating_airflow2_temp_upper_offset", getHeatingAirflow2TemperatureUpperOffset()));
//        setCoolingAirflow2TemperatureLowerOffset(spAlgoSettings.getInt("cooling_airflow2_temp_lower_offset", getCoolingAirflow2TemperatureLowerOffset()));
//        setCoolingAirflow2TemperatureUpperOffset(spAlgoSettings.getInt("cooling_airflow2_temp_upper_offset", getCoolingAirflow2TemperatureUpperOffset()));
//        setAirflowTemperatureBreachAlertHoldDown(spAlgoSettings.getInt("airflow_temp_breach_holddown", getAirflowTemperatureBreachAlertHoldDown()));
//        setHeartBeatUpdateInterval(spAlgoSettings.getInt("heart_beat_interval", getHeartBeatUpdateInterval()));
//        setHeartBeatsToSkip(spAlgoSettings.getInt("heart_beat_to_skip", getHeartBeatsToSkip()));
//        setAutoAwayTime(spAlgoSettings.getInt("auto_away_time", getAutoAwayTime()));
//        setShowRPMAlerts(spAlgoSettings.getInt("show_rpm_alerts", getShowRPMAlerts()));
//        setForcedOccupiedZonePriority(spAlgoSettings.getInt("forced_occupied_zone_priority", getForcedOccupiedZonePriority()));
//        setForcedOccupiedTimePeriod(spAlgoSettings.getInt("forced_occupied_time_period", getForcedOccupiedTimePeriod()));
//        setHumidityThreshold(spAlgoSettings.getInt("humidity_threshold", getHumidityThreshold()));
//        setHumidityPerDegreeFactor(spAlgoSettings.getInt("humidity_comp_factor", getHumidityPerDegreeFactor()));
//        setUseSameOccuTempAcrossDays(spAlgoSettings.getBoolean("use_same_occu_temp", getUseSameOccuTempAcrossDays()));
//        setDCVCO2ThresholdLevel(spAlgoSettings.getInt("dcv_co2_threshold", getDCVCO2ThresholdLevel()));
//        setDCVDamperOpeningRate(spAlgoSettings.getInt("dcv_damper_opening_rate", getDCVDamperOpeningRate()));
//        setDumbModeDCVDamperOpening(spAlgoSettings.getInt("dumb_mode_dcv_damper_opening", getDumbModeDCVDamperOpening()));
//        setZonePrioritySpread(spAlgoSettings.getInt("zone_priority_spread", getZonePrioritySpread()));
//        setZonePriorityMultiplier(spAlgoSettings.getFloat("zone_priority_multiplier", (float)getZonePriorityMultiplier()));
//        setAbnormalCurTempChangeAlertTrigger(spAlgoSettings.getInt("abnormal_cur_temp_change_trigger", getAbnormalCurTempChangeAlertTrigger()));
//        setUserLimitSpread(spAlgoSettings.getInt("user_limit_spread", getUserLimitSpread()));
//        setSetbackMultiplier(spAlgoSettings.getFloat("user_limit_multiplier", (float)getSetbackMultiplier()));
//        setPreconditioningRate(spAlgoSettings.getInt("preconditioning_rate", getPreconditioningRate()));
//        setEnthalpyCompensation(spAlgoSettings.getInt("enthalpy_comp", getEnthalpyCompensation()));
//        setOutsideAirMinTemp(spAlgoSettings.getInt("outside_air_min_temp", getOutsideAirMinTemp()));
//        setOutsideAirMaxTemp(spAlgoSettings.getInt("outside_air_max_temp", getOutsideAirMaxTemp()));
//        setOutsideAirMinHumidity(spAlgoSettings.getInt("outside_air_min_humidity", getOutsideAirMinHumidity()));
//        setOutsideAirMaxHumidity(spAlgoSettings.getInt("outside_air_max_humidity", getOutsideAirMaxHumidity()));
//        setEconomizerLoadThreshold(spAlgoSettings.getInt("economizer_load_threshold", getEconomizerLoadThreshold()));
//        setEconomizerHoldTime(spAlgoSettings.getInt("economizer_hold_time", getEconomizerHoldTime()));
//        setEconomizerStage1LoadDrop(spAlgoSettings.getInt("economizer_load_drop", getEconomizerStage1LoadDrop()));
//        setEconPropCtrlSpread(spAlgoSettings.getFloat("econ_prop_spread_f", (float)getEconPropCtrlSpread()));
//        setEconIntegCtrlTimeOut(spAlgoSettings.getInt("econ_integ_timeout", getEconIntegCtrlTimeOut()));
//
//        setAnalog1PropCtrlConstant(spAlgoSettings.getFloat("analog1_prop_const", (float) getAnalog1PropCtrlConstant()));
//        setAnalog1IntegCtrlConstant(spAlgoSettings.getFloat("analog1_integ_const", (float) getAnalog1IntegCtrlConstant()));
//        setAnalog1IntegCtrlTimeOut(spAlgoSettings.getInt("analog1_integ_timeout", getAnalog1IntegCtrlTimeOut()));
//        setAnalog1PropCtrlSpread(spAlgoSettings.getFloat("analog1_prop_spread_f", (float)getAnalog1PropCtrlSpread()));
//        setAnalog3PropCtrlConstant(spAlgoSettings.getFloat("analog3_prop_const", (float) getAnalog3PropCtrlConstant()));
//        setAnalog3IntegCtrlConstant(spAlgoSettings.getFloat("analog3_integ_const", (float) getAnalog3IntegCtrlConstant()));
//        setAnalog3IntegCtrlTimeOut(spAlgoSettings.getInt("analog3_integ_timeout", getAnalog3IntegCtrlTimeOut()));
//        setAnalog3PropCtrlSpread(spAlgoSettings.getFloat("analog3_prop_spread_f", (float)getAnalog3PropCtrlSpread()));
//        setAnalogFanSpeedMultiplier(spAlgoSettings.getFloat("analog_fanspeed_multiplier", (float) getAnalogFanSpeedMultiplier()));
//        setAnalogMinHeating(spAlgoSettings.getInt("analog_min_heating", getAnalogMinHeating()));
//        setAnalogMaxHeating(spAlgoSettings.getInt("analog_max_heating", getAnalogMaxHeating()));
//        setAnalogMinCooling(spAlgoSettings.getInt("analog_min_cooling", getAnalogMinCooling()));
//        setAnalogMaxCooling(spAlgoSettings.getInt("analog_max_cooling",getAnalogMaxCooling()));
//
//        setAutoModeCoolHeatDXCILimit(spAlgoSettings.getFloat("auto_heat_cool_dxci_limit", (float)getAutoModeCoolHeatDXCILimit()));
//        setAutoModeTotalDXCILimit(spAlgoSettings.getFloat("auto_total_dxci_limit", (float) getAutoModeTotalDXCILimit()));
//        setUseCoolHeatDXCIForAutoMode(spAlgoSettings.getBoolean("use_cool_heat_dx_for_auto", getUseCoolHeatDXCIForAutoMode()));
//        setUseCelsius(spAlgoSettings.getBoolean("use_celsius", SystemSettingsData.useCelsius()));
//        setUseMilitaryTime(spAlgoSettings.getBoolean("use_military_time", SystemSettingsData.useMilitaryTime()));
//        setUseInstantGratificationMode(spAlgoSettings.getBoolean("use_instant_gratification_mode", useInstantGratificationMode()));
//        setUseOutsideTemperatureLockout(spAlgoSettings.getBoolean("use_outside_temp_lockout", SystemSettingsData.useOutsideTemperatureLockout()));
//        setFollowAutoModeSchedule(spAlgoSettings.getBoolean("follow_auto_mode_schedule", SystemSettingsData.getFollowAutoSchedule()));
//        setFSVPairingStartAddress(spAlgoSettings.getInt("fsv_start_address", SystemSettingsData.getFSVStartAddress()));
//        setOutsideTempLockoutHeating(spAlgoSettings.getInt("no_heating_above", SystemSettingsData.getNoHeatingAboveLockoutTemperature()));
//        setOutsideTempLockoutCooling(spAlgoSettings.getInt("no_cooling_below", SystemSettingsData.getNoCoolingBelowLockoutTemperature()));
//        setStage1FanOnTime(spAlgoSettings.getInt("stage1_fan_on_time", getStage1FanOnTime()));
//        setStage1FanOffTime(spAlgoSettings.getInt("stage1_fan_off_time", getStage1FanOffTime()));
//        setBuildingAllowNoHotter(spAlgoSettings.getInt("building_no_hotter",SystemSettingsData.getBuildingAllowNoHotter()));
//        setBuildingAllowNoCooler(spAlgoSettings.getInt("building_no_cooler",SystemSettingsData.getBuildingAllowNoCooler()));
//        setUserAllowNoHotter(spAlgoSettings.getInt("user_no_hotter",SystemSettingsData.getUserAllowNoHotter()));
//        setUserAllowNoCooler(spAlgoSettings.getInt("user_no_cooler",SystemSettingsData.getUserAllowNoCooler()));
//        setWRMForHumidityValue(spAlgoSettings.getInt("wrm_for_humidity_value", getWRMForHumidityValue()));
//        setUseExtHumiditySensor(spAlgoSettings.getBoolean("use_ext_humidity_sensor", getUseExtHumiditySensor()));
//        setExtHumiditySensorMinSP(spAlgoSettings.getInt("ext_humidity_sensor_min_sp", getExtHumiditySensorMinSP()));
//        setExtHumiditySensorMaxSP(spAlgoSettings.getInt("ext_humidity_sensor_max_sp", getExtHumiditySensorMaxSP()));
//        setExtHumiditySensorSPTimeoutInterval(spAlgoSettings.getInt("ext_humidity_sensor_sp_timeout", getExtHumiditySPTimeoutInterval()));
//        setWRMForDumbModeTempValue(spAlgoSettings.getInt("wrm_for_dumb_mode_temp_value", getWRMForDumbModeTempValue()));
//        setNO2ThresholdLevel(spAlgoSettings.getInt("no2_threshold", getNO2ThresholdLevel()));
//        setCOThresholdLevel(spAlgoSettings.getInt("co_threshold", getCOThresholdLevel()));
//        setNO2DamperOpeningRate(spAlgoSettings.getInt("no2_damper_opening_rate",getNO2DamperOpeningRate()));
//        setCODamperOpeningRate(spAlgoSettings.getInt("co_damper_opening_rate",getCODamperOpeningRate()));
//        setOAODamperPosMin(spAlgoSettings.getInt("oao_damper_pos_min",getOAODamperPosMin()));
//        setOAODamperPosMax(spAlgoSettings.getInt("oao_damper_pos_max",getOAODamperPosMax()));
//        setOAOExhaustFanThreshold(spAlgoSettings.getInt("oao_exhaust_fan_threshold",getOAOExhaustFanThreshold()));
//        setPressureThresholdLevel(spAlgoSettings.getFloat("dp_pressure_threshold",(float)getPressureThresholdLevel()));
//        setPressureDeadZone(spAlgoSettings.getFloat("dp_pressure_dead_zone",(float)getPressureDeadZone()));
//        setPressureDamperOpeningRate(spAlgoSettings.getInt("pressure_damper_opening_rate",getPressureDamperOpeningRate()));
//        setAnalog1MinValue(spAlgoSettings.getInt("analog1_min_value",getAnalog1MinValue()));
//        setAnalog1MaxValue(spAlgoSettings.getInt("analog1_max_value",getAnalog1MaxValue()));
//        setAnalog3MinValue(spAlgoSettings.getInt("analog3_min_value",getAnalog3MinValue()));
//        setAnalog3MaxValue(spAlgoSettings.getInt("analog3_max_value",getAnalog3MaxValue()));
//        setChilledWaterDeltatSetpoint(spAlgoSettings.getInt("chilled_water_deltaT_sp", getChilledWaterDeltatSetpoint()));
//        setChilledWaterFlowSetpoint(spAlgoSettings.getInt("chilled_water_flow_sp", getChilledWaterFlowSetpoint()));
//        setChilledWaterMaxFlowRate(spAlgoSettings.getInt("chilled_water_max_flow_rate", getChilledWaterMaxFlowRate()));
//        setChilledWaterActuatorMinPos(spAlgoSettings.getInt("chilled_water_actuator_min_pos", getChilledWaterActuatorMinPos()));
//        setChilledWaterActuatorMaxPos(spAlgoSettings.getInt("chilled_water_actuator_max_pos", getChilledWaterActuatorMaxPos()));
//        setAHUFanMinLimit(spAlgoSettings.getInt("ahu_fan_min_limit", getAHUFanMinLimit()));
//        setAHUFanMaxLimit(spAlgoSettings.getInt("ahu_fan_max_limit", getAHUFanMaxLimit()));
//        setEnergyMeterMonitor(spAlgoSettings.getBoolean("show_emr_graph", isEnergyMeterMonitor()));
//        setReheatOffset(spAlgoSettings.getFloat("reheat_offset_f", (float)getReheatOffset()));
//        setReheatDamperReopenOffset(spAlgoSettings.getFloat("reheat_damper_reopen_offset_f", (float)getReheatDamperReopenOffset()));
//        setReheatMaxDamperPos(spAlgoSettings.getInt("reheat_max_damper_pos", getReheatMaxDamperPos()));
//        setRefrigerationLowerLimit(spAlgoSettings.getInt("ref_low_limit",getRefrigerationLowerLimit()));
//        setRefrigerationHigherLimit(spAlgoSettings.getInt("ref_high_limit",getRefrigerationHigherLimit()));
//        setAlarmVolume(spAlgoSettings.getInt("alarm_volume",getAlarmVolume()));
//        setEnergyMeterSetpoint(spAlgoSettings.getInt("energy_meter_sp",getEnergyMeterSetpoint()));
//        setUseSmartNodeInstall(spAlgoSettings.getBoolean("use_smart_node", getUseSmartNodeInstall()));
//        setLightingIntensityOccupantDetected(spAlgoSettings.getInt("lcm_intensity",getLightingIntensityOccupantDetected()));
//        setMinLightingControlOverrideInMinutes(spAlgoSettings.getInt("lcm_min_override",getMinLightingControlOverrideInMinutes()));
//        setStandaloneCoolingDeadband(spAlgoSettings.getInt("sa_cool_deadband",getStandaloneCoolingDeadband()));
//        setStandaloneHeatingDeadband(spAlgoSettings.getInt("sa_heat_deadband",getStandaloneHeatingDeadband()));
//    }
    
//    public void loadFromKinvey() {
//        AsyncAppData<AlgoTuningParameters> kinveyAppData = CCUKinveyInterface.getKinveyClient("AlgoTuner").appData(KINVEY_COLLECTION_NAME, AlgoTuningParameters.class);
//        kinveyAppData.setOffline(OfflinePolicy.ALWAYS_ONLINE, new SqlLiteOfflineStore<AlgoTuningParameters>(CCUApp.getAppContext()));
//
//        kinveyAppData.getEntity(id, new KinveyClientCallback<AlgoTuningParameters>() {
//            @Override
//            public void onSuccess(AlgoTuningParameters result) {
//                try {
//                    if (result == null) {
//                        if (!bUploadedToKinvey) {
//                            bUploadedToKinvey = true;
//                            saveToKinvey();
//                        }
//                    }
//                    else {
//                        Log.v("KINVEY_ALGO", "received " + result.toPrettyString());
//                        Log.v("KINVEY_ALGO", String.format("Current version: %d (%d), Kinvey Version: %d", getVersionNumber(), ALGO_TUNERS_VERSION, result.getVersionNumber()));
//                        setAlgoConstants(result.getPropConstant(), result.getIntegConstant(), 0, 0);
//                        setPropCtrlSpread(result.getPropCtrlSpread());
//                        setPreConditioningPropCtrlMultiplier(result.getPreConditioningPropCtrlMultiplier());
//                        setIntegCtrlTimeOut(result.getIntegCtrlTimeOut());
//                        setDXCoilHoldTimeMultiplier(result.getDXCoilHoldTimeMultiplier());
//                        setPercentDeadZonesAllowed(result.getPercentDeadZonesAllowed());
//                        setRebalanceHoldTime(result.getRebalanceHoldTime());
//                        setCumulativeDamperPosTarget(result.getCumulativeDamperPosTarget());
//                        setDamperPosForDeadZone(result.getDamperPosForDeadZone());
//                        setTimeToDeclareDeadDamper(result.getTimeToDeclareDeadDamper());
//                        setDXCoilOffsetLimitMultiplier(result.getDXCoilOffsetLimitMultiplier());
//                        setDxCoilOffsetHeatOffset(result.getDxCoilOffsetHeatOffset());
//                        setDXCoilOffsetCoolOffset(result.getDXCoilOffsetCoolOffset());
//                        setClockUpdateInterval(result.getClockUpdateInterval());
//                        setBuildingToZoneTempLimitsDifferential(result.getBuildingToZoneTempLimitsDifferential());
//                        setBackpressureLimit(result.getBackpressureLimit());
//                        setCumDamperPosIncrForBackpressure(result.getCumDamperPosIncrForBackpressure());
//                        setMultizoneStage2Timer(result.getMultizoneStage2Timer());
//                        setSinglezoneStage2Timer(result.getSinglezoneStage2Timer());
//                        setMultizoneStage2PercentDrop(result.getMultizoneStage2PercentDrop());
//                        setSinglezoneStage2Offset(result.getSinglezoneStage2Offset());
//                        setAutoModeTemperatureMultiplier(result.getAutoModeTemperatureMultiplier());
//                        setIgnoreCMReportedErrors(result.getIgnoreCMReportedErrors());
//                        setIgnoreFSVNotInDBError(result.getIgnoreFSVNotInDBError());
//                        setHeartBeatUpdateInterval(result.getHeartBeatUpdateInterval());
//                        setHeartBeatsToSkip(result.getHeartBeatsToSkip());
//                        setHeatingAirflowTemperature(result.getHeatingAirflowTemperature());
//                        setHeatingAirflowTemperatureLowerOffset(result.getHeatingAirflowTemperatureLowerOffset());
//                        setHeatingAirflowTemperatureUpperOffset(result.getHeatingAirflowTemperatureUpperOffset());
//                        setCoolingAirflowTemperature(result.getCoolingAirflowTemperature());
//                        setCoolingAirflowTemperatureLowerOffset(result.getCoolingAirflowTemperatureLowerOffset());
//                        setCoolingAirflowTemperatureUpperOffset(result.getCoolingAirflowTemperatureUpperOffset());
//                        setHeatingAirflow2TemperatureLowerOffset(result.getHeatingAirflow2TemperatureLowerOffset());
//                        setHeatingAirflow2TemperatureUpperOffset(result.getHeatingAirflow2TemperatureUpperOffset());
//                        setCoolingAirflow2TemperatureLowerOffset(result.getCoolingAirflow2TemperatureLowerOffset());
//                        setCoolingAirflow2TemperatureUpperOffset(result.getCoolingAirflow2TemperatureUpperOffset());
//                        setAirflowTemperatureBreachAlertHoldDown(result.getAirflowTemperatureBreachAlertHoldDown());
//                        setAutoAwayTime(result.getAutoAwayTime());
//                        setShowRPMAlerts(result.getShowRPMAlerts());
//                        setForcedOccupiedZonePriority(result.getForcedOccupiedZonePriority());
//                        setForcedOccupiedTimePeriod(result.getForcedOccupiedTimePeriod());
//                        setHumidityThreshold(result.getHumidityThreshold());
//                        setHumidityPerDegreeFactor(result.getHumidityPerDegreeFactor());
//                        setUseSameOccuTempAcrossDays(result.getUseSameOccuTempAcrossDays());
//                        setDCVCO2ThresholdLevel(result.getDCVCO2ThresholdLevel());
//                        setDCVDamperOpeningRate(result.getDCVDamperOpeningRate());
//                        setDumbModeDCVDamperOpening(result.getDumbModeDCVDamperOpening());
//                        setZonePrioritySpread(result.getZonePrioritySpread());
//                        setZonePriorityMultiplier(result.getZonePriorityMultiplier());
//                        setAbnormalCurTempChangeAlertTrigger(result.getAbnormalCurTempChangeAlertTrigger());
//                        setUserLimitSpread(result.getUserLimitSpread());
//                        setSetbackMultiplier(result.getSetbackMultiplier());
//                        setPreconditioningRate(result.getPreconditioningRate());
//                        setEnthalpyCompensation(result.getEnthalpyCompensation());
//                        setOutsideAirMinTemp(result.getOutsideAirMinTemp());
//                        setOutsideAirMaxTemp(result.getOutsideAirMaxTemp());
//                        setOutsideAirMinHumidity(result.getOutsideAirMinHumidity());
//                        setOutsideAirMaxHumidity(result.getOutsideAirMaxHumidity());
//                        setEconomizerLoadThreshold(result.getEconomizerLoadThreshold());
//                        setEconomizerHoldTime(result.getEconomizerHoldTime());
//                        setEconomizerStage1LoadDrop(result.getEconomizerStage1LoadDrop());
//                        setEconPropCtrlSpread(result.getEconPropCtrlSpread());
//                        setEconIntegCtrlTimeOut(result.getEconIntegCtrlTimeOut());
//
//                        setAnalog1PropCtrlConstant(result.getAnalog1PropCtrlConstant());
//                        setAnalog1IntegCtrlConstant(result.getAnalog1IntegCtrlConstant());
//                        setAnalog1IntegCtrlTimeOut(result.getAnalog1IntegCtrlTimeOut());
//                        setAnalog1PropCtrlSpread(result.getAnalog1PropCtrlSpread());
//                        setAnalog3PropCtrlConstant(result.getAnalog3PropCtrlConstant());
//                        setAnalog3IntegCtrlConstant(result.getAnalog3IntegCtrlConstant());
//                        setAnalog3IntegCtrlTimeOut(result.getAnalog3IntegCtrlTimeOut());
//                        setAnalog3PropCtrlSpread(result.getAnalog3PropCtrlSpread());
//                        setAnalogFanSpeedMultiplier(result.getAnalogFanSpeedMultiplier());
//                        setAnalogMinHeating(result.getAnalogMinHeating());
//                        setAnalogMaxHeating(result.getAnalogMaxHeating());
//                        setAnalogMinCooling(result.getAnalogMinCooling());
//                        setAnalogMaxCooling(result.getAnalogMaxCooling());
//
//                        setAutoModeCoolHeatDXCILimit(result.getAutoModeCoolHeatDXCILimit());
//                        setAutoModeTotalDXCILimit(result.getAutoModeTotalDXCILimit());
//                        setUseCoolHeatDXCIForAutoMode(result.getUseCoolHeatDXCIForAutoMode());
//
//                        setUseCelsius(result.useCelsius());
//                        setUseMilitaryTime(result.useMilitaryTime());
//                        setUseInstantGratificationMode(result.useInstantGratificationMode());
//                        setUseOutsideTemperatureLockout(result.useOutsideTemperatureLockout());
//                        setFollowAutoModeSchedule(result.getFollowAutoModeSchedule());
//                        //setFSVPairingStartAddress(result.getFSVPairingStartAddress());
//                        setOutsideTempLockoutHeating(result.getNoHeatingAboveLockoutTemperature());
//                        setOutsideTempLockoutCooling(result.getNoCoolingBelowLockoutTemperature());
//                        setStage1FanOnTime(result.getStage1FanOnTime());
//                        setStage1FanOffTime(result.getStage1FanOffTime());
//                        setWRMForHumidityValue(result.getWRMForHumidityValue());
//                        setUseExtHumiditySensor(result.getUseExtHumiditySensor());
//                        setExtHumiditySensorMinSP(result.getExtHumiditySensorMinSP());
//                        setExtHumiditySensorMaxSP(result.getExtHumiditySensorMaxSP());
//                        setExtHumiditySensorSPTimeoutInterval(result.getExtHumiditySPTimeoutInterval());
//                        setWRMForDumbModeTempValue(result.getWRMForDumbModeTempValue());
//                        setNO2ThresholdLevel(result.getNO2ThresholdLevel());
//                        setCOThresholdLevel(result.getCOThresholdLevel());
//                        setNO2DamperOpeningRate(result.getNO2DamperOpeningRate());
//                        setCODamperOpeningRate(result.getCODamperOpeningRate());
//                        setOAODamperPosMin(result.getOAODamperPosMin());
//                        setOAODamperPosMax(result.getOAODamperPosMax());
//                        setOAOExhaustFanThreshold(result.getOAOExhaustFanThreshold());
//                        setPressureThresholdLevel(result.getPressureThresholdLevel());
//                        setPressureDeadZone(result.getPressureDeadZone());
//                        setPressureDamperOpeningRate(result.getPressureDamperOpeningRate());
//                        setAnalog1MinValue(result.getAnalog1MinValue());
//                        setAnalog1MaxValue(result.getAnalog1MaxValue());
//                        setAnalog3MinValue(result.getAnalog3MinValue());
//                        setAnalog3MaxValue(result.getAnalog3MaxValue());
//                        setChilledWaterDeltatSetpoint(result.getChilledWaterDeltatSetpoint());
//                        setChilledWaterFlowSetpoint(result.getChilledWaterFlowSetpoint());
//                        setChilledWaterMaxFlowRate(result.getChilledWaterMaxFlowRate());
//                        setChilledWaterActuatorMinPos(result.getChilledWaterActuatorMinPos());
//                        setChilledWaterActuatorMaxPos(result.getChilledWaterActuatorMaxPos());
//                        setAHUFanMinLimit(result.getAHUFanMinLimit());
//                        setAHUFanMaxLimit(result.getAHUFanMaxLimit());
//                        setEnergyMeterMonitor(result.isEnergyMeterMonitor());
//                        setReheatOffset(result.getReheatOffset());
//                        setReheatDamperReopenOffset(result.getReheatDamperReopenOffset());
//                        setReheatMaxDamperPos(result.getReheatMaxDamperPos());
//                        setRefrigerationLowerLimit(result.getRefrigerationLowerLimit());
//                        setRefrigerationHigherLimit(result.getRefrigerationHigherLimit());
//                        setAlarmVolume(result.getAlarmVolume());
//                        setEnergyMeterSetpoint(result.getEnergyMeterSetpoint());
//                        setUseSmartNodeInstall(result.getUseSmartNodeInstall());
//                        setMinLightingControlOverrideInMinutes(result.getMinLightingControlOverrideInMinutes());
//                        setLightingIntensityOccupantDetected(result.getLightingIntensityOccupantDetected());
//                        setStandaloneCoolingDeadband(result.getStandaloneCoolingDeadband());
//                        setStandaloneHeatingDeadband(result.getStandaloneHeatingDeadband());
//
//                        saveToSharedPrefs();
//                        setChangesForBuildingAndUserLimits(result.getBuildingAllowNoHotter(), result.getBuildingAllowNoCooler(), result.getUserAllowNoHotter(), result.getUserAllowNoCooler());
//
//                        if (result.getVersionNumber() != ALGO_TUNERS_VERSION) {
//                            setVersionNumber(ALGO_TUNERS_VERSION);
//                            saveToKinvey();
//                        } else
//                            setVersionNumber(ALGO_TUNERS_VERSION);
//                    }
//
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//            @Override
//            public void onFailure(Throwable error) {
//                Log.e("KINVEY_ALGO", "failed to fetchByFilterCriteria", error);
//            }
//        });
//    }
//
//    public void save() {
//        saveToSharedPrefs();
//        saveToKinvey();
//    }
//
//    public void saveToSharedPrefs() {
//        SharedPreferences spAlgoSettings =  CCUApp.getAppContext().getSharedPreferences("algo_tuning_vars", 0);
//        SharedPreferences.Editor editor = spAlgoSettings.edit();
//
//        editor.putFloat("prop_ctrl_const", (float) getPropConstant());
//        editor.putFloat("integ_ctrl_const", (float) getIntegConstant());
//        editor.putFloat("prop_ctrl_range", (float) getPropCtrlSpread());
//        editor.putFloat("precond_prop_ctrl_multiplier", (float) getPreConditioningPropCtrlMultiplier());
//        editor.putInt("integ_time_out", getIntegCtrlTimeOut());
//        editor.putInt("dx_coil_hold_time_multiplier", getDXCoilHoldTimeMultiplier());
//        editor.putInt("dead_zones_allowed", getPercentDeadZonesAllowed());
//        editor.putInt("rebalance_hold_time", getRebalanceHoldTime());
//        editor.putInt("target_cum_damper_pos", getCumulativeDamperPosTarget());
//        editor.putInt("dead_zone_damper_pos", getDamperPosForDeadZone());
//        editor.putInt("zone_dead_time", getTimeToDeclareDeadDamper());
//        editor.putFloat("dxcoil_ci_offset_limit_multiplier", (float) getDXCoilOffsetLimitMultiplier());
//        editor.putFloat("dxcoil_offset_heat_offset", (float) getDxCoilOffsetHeatOffset());
//        editor.putFloat("dxcoil_offset_cool_offset", (float) getDXCoilOffsetCoolOffset());
//        editor.putInt("clock_update_interval", getClockUpdateInterval());
//        editor.putInt("temp_differential", getBuildingToZoneTempLimitsDifferential());
//        editor.putFloat("backpressure_limit", (float) getBackpressureLimit());
//        editor.putInt("damperpos_increment_for_bp", getCumDamperPosIncrForBackpressure());
//        editor.putInt("multizone_stage2_timer", getMultizoneStage2Timer());
//        editor.putInt("singlezone_stage2_timer", getSinglezoneStage2Timer());
//        editor.putInt("multizone_stage2_percent_drop", getMultizoneStage2PercentDrop());
//        editor.putFloat("singlezone_stage2_offset", (float) getSinglezoneStage2Offset());
//        editor.putFloat("auto_mode_temp_multiplier", (float) getAutoModeTemperatureMultiplier());
//        editor.putInt("ignore_cm_reported_errors", getIgnoreCMReportedErrors());
//        editor.putInt("ignore_fsv_not_in_db_error", getIgnoreFSVNotInDBError());
//        editor.putInt("heart_beat_interval", getHeartBeatUpdateInterval());
//        editor.putInt("heart_beat_to_skip", getHeartBeatsToSkip());
//        editor.putInt("heating_airflow_temperature", getHeatingAirflowTemperature());
//        editor.putInt("heating_airflow_temp_lower_offset", getHeatingAirflowTemperatureLowerOffset());
//        editor.putInt("heating_airflow_temp_upper_offset", getHeatingAirflowTemperatureUpperOffset());
//        editor.putInt("cooling_airflow_temperature", getCoolingAirflowTemperature());
//        editor.putInt("cooling_airflow_temp_lower_offset", getCoolingAirflowTemperatureLowerOffset());
//        editor.putInt("cooling_airflow_temp_upper_offset", getCoolingAirflowTemperatureUpperOffset());
//        editor.putInt("heating_airflow2_temp_lower_offset", getHeatingAirflow2TemperatureLowerOffset());
//        editor.putInt("heating_airflow2_temp_upper_offset", getHeatingAirflow2TemperatureUpperOffset());
//        editor.putInt("cooling_airflow2_temp_lower_offset", getCoolingAirflow2TemperatureLowerOffset());
//        editor.putInt("cooling_airflow2_temp_upper_offset", getCoolingAirflow2TemperatureUpperOffset());
//        editor.putInt("airflow_temp_breach_holddown", getAirflowTemperatureBreachAlertHoldDown());
//        editor.putInt("auto_away_time", getAutoAwayTime());
//        editor.putInt("show_rpm_alerts", getShowRPMAlerts());
//        editor.putInt("forced_occupied_zone_priority", getForcedOccupiedZonePriority());
//        editor.putInt("forced_occupied_time_period", getForcedOccupiedTimePeriod());
//        editor.putInt("humidity_threshold", getHumidityThreshold());
//        editor.putInt("humidity_comp_factor", getHumidityPerDegreeFactor());
//        editor.putBoolean("use_same_occu_temp", getUseSameOccuTempAcrossDays());
//        editor.putInt("dcv_co2_threshold", getDCVCO2ThresholdLevel());
//        editor.putInt("dcv_damper_opening_rate", getDCVDamperOpeningRate());
//        editor.putInt("dumb_mode_dcv_damper_opening", getDumbModeDCVDamperOpening());
//        editor.putInt("zone_priority_spread", getZonePrioritySpread());
//        editor.putFloat("zone_priority_multiplier", (float)getZonePriorityMultiplier());
//        editor.putInt("abnormal_cur_temp_change_trigger", getAbnormalCurTempChangeAlertTrigger());
//        editor.putInt("user_limit_spread", getUserLimitSpread());
//        editor.putFloat("user_limit_multiplier", (float)getSetbackMultiplier());
//        editor.putInt("preconditioning_rate", getPreconditioningRate());
//        editor.putInt("enthalpy_comp", getEnthalpyCompensation());
//        editor.putInt("outside_air_min_temp", getOutsideAirMinTemp());
//        editor.putInt("outside_air_max_temp", getOutsideAirMaxTemp());
//        editor.putInt("outside_air_min_humidity", getOutsideAirMinHumidity());
//        editor.putInt("outside_air_max_humidity", getOutsideAirMaxHumidity());
//        editor.putInt("economizer_load_threshold", getEconomizerLoadThreshold());
//        editor.putInt("economizer_hold_time", getEconomizerHoldTime());
//        editor.putInt("economizer_load_drop", getEconomizerStage1LoadDrop());
//        editor.putFloat("econ_prop_spread_f", (float)getEconPropCtrlSpread());
//        editor.putInt("econ_integ_timeout", getEconIntegCtrlTimeOut());
//
//        editor.putFloat("analog1_prop_const", (float) getAnalog1PropCtrlConstant());
//        editor.putFloat("analog1_integ_const", (float) getAnalog1IntegCtrlConstant());
//        editor.putInt("analog1_integ_timeout", getAnalog1IntegCtrlTimeOut());
//        editor.putFloat("analog1_prop_spread_f", (float)getAnalog1PropCtrlSpread());
//        editor.putFloat("analog3_prop_const", (float) getAnalog3PropCtrlConstant());
//        editor.putFloat("analog3_integ_const", (float) getAnalog3IntegCtrlConstant());
//        editor.putInt("analog3_integ_timeout", getAnalog3IntegCtrlTimeOut());
//        editor.putFloat("analog3_prop_spread_f", (float)getAnalog3PropCtrlSpread());
//        editor.putFloat("analog_fanspeed_multiplier", (float) getAnalogFanSpeedMultiplier());
//        editor.putInt("analog_min_heating", getAnalogMinHeating());
//        editor.putInt("analog_max_heating", getAnalogMaxHeating());
//        editor.putInt("analog_min_cooling", getAnalogMinCooling());
//        editor.putInt("analog_max_cooling",getAnalogMaxCooling());
//
//        editor.putFloat("auto_heat_cool_dxci_limit", (float)getAutoModeCoolHeatDXCILimit());
//        editor.putFloat("auto_total_dxci_limit", (float) getAutoModeTotalDXCILimit());
//        editor.putBoolean("use_cool_heat_dx_for_auto", getUseCoolHeatDXCIForAutoMode());
//
//        editor.putBoolean("use_celsius", useCelsius());
//        editor.putBoolean("use_military_time", useMilitaryTime());
//        editor.putBoolean("use_instant_gratification_mode", useInstantGratificationMode());
//        editor.putBoolean("use_outside_temp_lockout", useOutsideTemperatureLockout());
//        editor.putBoolean("follow_auto_mode_schedule", getFollowAutoModeSchedule());
//        editor.putInt("fsv_start_address", getFSVPairingStartAddress());
//        editor.putInt("no_heating_above",getNoHeatingAboveLockoutTemperature());
//        editor.putInt("no_cooling_below",getNoCoolingBelowLockoutTemperature());
//        editor.putInt("stage1_fan_on_time", getStage1FanOnTime());
//        editor.putInt("stage1_fan_off_time", getStage1FanOffTime());
//        editor.putInt("building_no_hotter",getBuildingAllowNoHotter());
//        editor.putInt("building_no_cooler",getBuildingAllowNoCooler());
//        editor.putInt("user_no_hotter",getUserAllowNoHotter());
//        editor.putInt("user_no_cooler",getUserAllowNoCooler());
//        editor.putInt("wrm_for_humidity_value", getWRMForHumidityValue());
//        editor.putBoolean("use_ext_humidity_sensor", getUseExtHumiditySensor());
//        editor.putInt("ext_humidity_sensor_min_sp", getExtHumiditySensorMinSP());
//        editor.putInt("ext_humidity_sensor_max_sp", getExtHumiditySensorMaxSP());
//        editor.putInt("ext_humidity_sensor_sp_timeout", getExtHumiditySPTimeoutInterval());
//        editor.putInt("wrm_for_dumb_mode_temp_value", getWRMForDumbModeTempValue());
//        editor.putInt("no2_threshold", getNO2ThresholdLevel());
//        editor.putInt("co_threshold", getCOThresholdLevel());
//        editor.putInt("no2_damper_opening_rate",getNO2DamperOpeningRate());
//        editor.putInt("co_damper_opening_rate",getCODamperOpeningRate());
//        editor.putInt("oao_damper_pos_min",getOAODamperPosMin());
//        editor.putInt("oao_damper_pos_max",getOAODamperPosMax());
//        editor.putInt("oao_exhaust_fan_threshold",getOAODamperPosMin());
//        editor.putFloat("dp_pressure_threshold",(float) getPressureThresholdLevel());
//        editor.putFloat("dp_pressure_dead_zone",(float) getPressureDeadZone());
//        editor.putInt("pressure_damper_opening_rate",getPressureDamperOpeningRate());
//        editor.putInt("analog1_min_value",getAnalog1MinValue());
//        editor.putInt("analog1_max_value",getAnalog1MaxValue());
//        editor.putInt("analog3_min_value",getAnalog3MinValue());
//        editor.putInt("analog3_max_value",getAnalog3MaxValue());
//        editor.putInt("chilled_water_deltaT_sp", getChilledWaterDeltatSetpoint());
//        editor.putInt("chilled_water_flow_sp", getChilledWaterFlowSetpoint());
//        editor.putInt("chilled_water_max_flow_rate", getChilledWaterMaxFlowRate());
//        editor.putInt("chilled_water_actuator_min_pos", getChilledWaterActuatorMinPos());
//        editor.putInt("chilled_water_actuator_max_pos", getChilledWaterActuatorMaxPos());
//        editor.putInt("ahu_fan_min_limit", getAHUFanMinLimit());
//        editor.putInt("ahu_fan_max_limit", getAHUFanMaxLimit());
//        editor.putBoolean("show_emr_graph", isEnergyMeterMonitor());
//        editor.putFloat("reheat_offset_f", (float)getReheatOffset());
//        editor.putFloat("reheat_damper_reopen_offset_f", (float)getReheatDamperReopenOffset());
//        editor.putInt("reheat_max_damper_pos", getReheatMaxDamperPos());
//        editor.putInt("ref_low_limit",getRefrigerationLowerLimit());
//        editor.putInt("ref_high_limit",getRefrigerationHigherLimit());
//        editor.putInt("alarm_volume",getAlarmVolume());
//        editor.putInt("energy_meter_sp",getEnergyMeterSetpoint());
//        editor.putBoolean("use_smart_node", getUseSmartNodeInstall());
//        editor.putInt("lcm_intensity",getLightingIntensityOccupantDetected());
//        editor.putInt("lcm_min_override",getMinLightingControlOverrideInMinutes());
//        editor.putInt("sa_cool_deadband",getStandaloneCoolingDeadband());
//        editor.putInt("sa_heat_deadband",getStandaloneHeatingDeadband());
//        editor.commit();
//    }
    
//    public void saveToKinvey() {
//        mCCUName = CCUKinveyInterface.getCCUName();
//        AsyncAppData<AlgoTuningParameters> kinveyAppData = CCUKinveyInterface.getKinveyClient("AlgoTuner").appData(KINVEY_COLLECTION_NAME, AlgoTuningParameters.class);
//        kinveyAppData.setOffline(OfflinePolicy.ALWAYS_ONLINE, new SqlLiteOfflineStore<AlgoTuningParameters>(CCUApp.getAppContext()));
//        kinveyAppData.save(this, new KinveyClientCallback<AlgoTuningParameters>() {
//            @Override
//            public void onFailure(Throwable e) {
//                Log.e("KINVEY_ALGO", "failed to save event data", e);
//            }
//            @Override
//            public void onSuccess(AlgoTuningParameters r) {
//                try {
//                    Log.d("KINVEY_ALGO", "saved data for entity "+ r.toPrettyString());
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        });
//    }
    
    public void setVersionNumber(int version) {
        this.mVersion = version;
    }
    
    public int getVersionNumber() {
        return this.mVersion;
    }
    
    public void setAlgoConstants (double propCtrlConst, double integCtrlConst,
                                  double zoneLoadCtrlConst, double coolingRateCtrlConst) {
        this.mPropCtrlConstant = propCtrlConst;
        this.mIntegCtrlConstant = integCtrlConst;
        this.mZoneLoadCtrlConstant = zoneLoadCtrlConst;
        this.mCoolingRateCtrlConstant = coolingRateCtrlConst;
	/*	for (FloorData floorData: FloorData.getFloorData()) {
			for (RoomData roomData: floorData.getRoomData()) {
				for (FSVData fsvData: roomData.getFSVData()) {
					fsvData.getFsvData().settings.k1 = (int) (propCtrlConst*100);
					fsvData.getFsvData().settings.k2 = (int) (integCtrlConst*100);
				}
			}
		}
	*/
    }
    
    public double getPropConstant() {
        return mPropCtrlConstant;
    }
    
    public double getIntegConstant() {
        return mIntegCtrlConstant;
    }
    
    public double getZoneLoadConstant() {
        return mZoneLoadCtrlConstant;
    }
    
    public double getCoolingRateConstant() {
        return mCoolingRateCtrlConstant;
    }
    
    public int getCumulativeDamperPosTarget() {
        return mCumulativeDamperPosTarget;
    }
    
    public void setCumulativeDamperPosTarget(int nCumulativeDamperPosTarget) {
        this.mCumulativeDamperPosTarget = nCumulativeDamperPosTarget;
    }
    
    public int getDamperPosForDeadZone() {
        return mDamperPosForDeadZone;
    }
    
    public int getTimeToDeclareDeadDamper() {
        return mTimeToDeclareDeadDamper;
    }
    
    public int getIntegCtrlTimeOut() {
        return mIntegCtrlTimeOut;
    }
    
    public double getCalculatedPropCtrlSpread() {
//        int timeToOccupied = SystemSettingsData.timeToOccupiedSlot();
//        if (SystemSettingsData.isDemoModeEnabled())
//            return 5.0;
//        else if (timeToOccupied < 4 && timeToOccupied > 0)
//            return mPropCtrlSpread*mPreConditioningPropCtrlMultiplier;
//        else
            return mPropCtrlSpread;
    }
    
    /* PANKAJ: This function should only be used to get the actual tuner value.
     * For using with algo, the above function should be used
     */
    public double getPropCtrlSpread() {
        return mPropCtrlSpread;
    }
    
    public double getPreConditioningPropCtrlMultiplier() {
        return mPreConditioningPropCtrlMultiplier;
    }
    
    public void setDamperPosForDeadZone(int mDamperPosForDeadZone) {
        this.mDamperPosForDeadZone = mDamperPosForDeadZone;
    }
    
    public void setTimeToDeclareDeadDamper(int mTimeToDeclareDeadDamper) {
        this.mTimeToDeclareDeadDamper = mTimeToDeclareDeadDamper;
    }
    
    public void setIntegCtrlTimeOut(int nIntegCtrlTimeOut) {
        this.mIntegCtrlTimeOut = nIntegCtrlTimeOut;
/*		for (FloorData floorData: FloorData.getFloorData()) {
			for (RoomData roomData: floorData.getRoomData()) {
				for (FSVData fsvData: roomData.getFSVData()) {
					fsvData.getFsvData().settings.integration_time = nIntegCtrlTimeOut;
				}
			}
		}*/
    }
    
    public void setPropCtrlSpread(double nPropCtrlSpread) {
        this.mPropCtrlSpread = nPropCtrlSpread;
/*		for (FloorData floorData: FloorData.getFloorData()) {
			for (RoomData roomData: floorData.getRoomData()) {
				for (FSVData fsvData: roomData.getFSVData()) {
					fsvData.getFsvData().settings.proportional_temperature_range = (int) (nPropCtrlSpread*10);
				}
			}
		}*/
    }
    
    public void setPreConditioningPropCtrlMultiplier(double val) {
        this.mPreConditioningPropCtrlMultiplier = val;
    }
    
    
    public int getDXCoilHoldTimeMultiplier() {
        return mDXCoilHoldTimeMultiplier;
    }
    
    public void setDXCoilHoldTimeMultiplier(int val) {
        this.mDXCoilHoldTimeMultiplier = val;
    }
    
    public int getRebalanceHoldTime() {
        return mRebalanceHoldTime;
    }
    
    public void setRebalanceHoldTime(int mRebalanceHoldTime) {
        this.mRebalanceHoldTime = mRebalanceHoldTime;
    }
    
    public int getPercentDeadZonesAllowed() {
        return mPercentDeadZonesAllowed;
    }
    
    public void setPercentDeadZonesAllowed(int nPercentDeadZonesAllowed) {
        this.mPercentDeadZonesAllowed = nPercentDeadZonesAllowed;
    }
    
    public double getDxCoilOffsetHeatOffset() {
        return mDXCoilOffsetHeatOffset;
    }
    
    public void setDxCoilOffsetHeatOffset(double val) {
        mDXCoilOffsetHeatOffset = val;
    }
    
    public double getDXCoilOffsetCoolOffset() {
        return mDXCoilOffsetCoolOffset;
    }
    
    public void setDXCoilOffsetCoolOffset(double val) {
        mDXCoilOffsetCoolOffset = val;
    }
    
    public double getDXCoilOffsetLimitMultiplier() {
        return mDXCoilOffsetLimitMultiplier;
    }
    
    public void setDXCoilOffsetLimitMultiplier(double val) {
        mDXCoilOffsetLimitMultiplier = val;
    }
    
    public double getAutoModeTemperatureMultiplier() {
        return mAutoModeTemperatureMultiplier;
    }
    
    public void setAutoModeTemperatureMultiplier(double val) {
        mAutoModeTemperatureMultiplier = val;
    }
    
    public int getClockUpdateInterval() {
        return mClockUpdateInterval;
    }
    
    public void setClockUpdateInterval(int nVal) {
        mClockUpdateInterval = nVal;
    }
    
    public int getBuildingToZoneTempLimitsDifferential() {
        return mBuildingToZoneTempLimitsDifferential;
    }
    
    public void setBuildingToZoneTempLimitsDifferential(
                                                               int mBuildingToZoneTempLimitsDifferential) {
        this.mBuildingToZoneTempLimitsDifferential = mBuildingToZoneTempLimitsDifferential;
    }
    
    public double getBackpressureLimit() {
        return mBackpressureLimit;
    }
    
    public void setBackpressureLimit(double mBackpressureLimit) {
        this.mBackpressureLimit = mBackpressureLimit;
    }
    
    public int getCumDamperPosIncrForBackpressure() {
        return mCumDamperPosIncrForBackpressure;
    }
    
    public void setCumDamperPosIncrForBackpressure(
                                                          int mCumDamperPosIncrForBackpressure) {
        this.mCumDamperPosIncrForBackpressure = mCumDamperPosIncrForBackpressure;
    }
    
    public int getMultizoneStage2Timer() {
        return mMultizoneStage2Timer;
    }
    
    public void setMultizoneStage2Timer(int mMultizoneStage2Timer) {
        this.mMultizoneStage2Timer = mMultizoneStage2Timer;
    }
    
    public int getSinglezoneStage2Timer() {
        return mSinglezoneStage2Timer;
    }
    
    public void setSinglezoneStage2Timer(int mSinglezoneStage2Timer) {
        this.mSinglezoneStage2Timer = mSinglezoneStage2Timer;
    }
    
    public int getMultizoneStage2PercentDrop() {
        return mMultizoneStage2PercentDrop;
    }
    
    public void setMultizoneStage2PercentDrop(int mMultizoneStage2PercentDrop) {
        this.mMultizoneStage2PercentDrop = mMultizoneStage2PercentDrop;
    }
    
    public double getSinglezoneStage2Offset() {
        return mSinglezoneStage2Offset;
    }
    
    public void setSinglezoneStage2Offset(double mSinglezoneStage2Offset) {
        this.mSinglezoneStage2Offset = mSinglezoneStage2Offset;
    }
    
    public int getIgnoreFSVNotInDBError() {
        return mIgnoreFSVNotInDBError;
    }
    
    public void setIgnoreFSVNotInDBError(int val) {
        mIgnoreFSVNotInDBError = val;
    }
    
    public int getHeartBeatUpdateInterval() {
        return mHeartBeatInterval;
    }
    
    public void setHeartBeatUpdateInterval(int val) {
        mHeartBeatInterval = val;
    }
    
    public int getHeartBeatsToSkip() {
        return mHeartBeatsToSkip;
    }
    
    public void setHeartBeatsToSkip(int val) {
        mHeartBeatsToSkip = val;
    }
    
    public int getCoolingAirflowTemperature() {
        return mCoolingAirflowTemperature;
    }
    
    public int getCoolingAirflowTemperatureLowerOffset() {
        return mCoolingAirflowTempLowerOffset;
    }
    
    public int getCoolingAirflowTemperatureUpperOffset() {
        return mCoolingAirflowTempUpperOffset;
    }
    
    public int getHeatingAirflowTemperature() {
        return mHeatingAirflowTemperature;
    }
    
    public int getHeatingAirflowTemperatureLowerOffset() {
        return mHeatingAirflowTempLowerOffset;
    }
    
    public int getHeatingAirflowTemperatureUpperOffset() {
        return mHeatingAirflowTempUpperOffset;
    }
    
    public int getCoolingAirflow2TemperatureLowerOffset() {
        return mCoolingAirflow2TempLowerOffset;
    }
    
    public int getCoolingAirflow2TemperatureUpperOffset() {
        return mCoolingAirflow2TempUpperOffset;
    }
    
    public int getHeatingAirflow2TemperatureLowerOffset() {
        return mHeatingAirflow2TempLowerOffset;
    }
    
    public int getHeatingAirflow2TemperatureUpperOffset() {
        return mHeatingAirflow2TempUpperOffset;
    }
    
    public int getAirflowTemperatureBreachAlertHoldDown() {
        return mAirflowTemperatureBreachAlertHoldDown;
    }
    
    public int getIgnoreCMReportedErrors() {
        return mIgnoreCMReportedError;
    }
    
    public void setCoolingAirflowTemperature(int val) {
        mCoolingAirflowTemperature = val;
    }
    
    public void setCoolingAirflowTemperatureLowerOffset(int val) {
        mCoolingAirflowTempLowerOffset = val;
    }
    
    public void setCoolingAirflowTemperatureUpperOffset(int val) {
        mCoolingAirflowTempUpperOffset = val;
    }
    
    
    public void setHeatingAirflowTemperature(int val) {
        mHeatingAirflowTemperature = val;
    }
    
    public void setHeatingAirflowTemperatureLowerOffset(int val) {
        mHeatingAirflowTempLowerOffset = val;
    }
    
    public void setHeatingAirflowTemperatureUpperOffset(int val) {
        mHeatingAirflowTempUpperOffset = val;
    }
    
    public void setCoolingAirflow2TemperatureLowerOffset(int val) {
        mCoolingAirflow2TempLowerOffset = val;
    }
    
    public void setCoolingAirflow2TemperatureUpperOffset(int val) {
        mCoolingAirflow2TempUpperOffset = val;
    }
    
    public void setHeatingAirflow2TemperatureLowerOffset(int val) {
        mHeatingAirflow2TempLowerOffset = val;
    }
    
    public void setHeatingAirflow2TemperatureUpperOffset(int val) {
        mHeatingAirflow2TempUpperOffset = val;
    }
    
    public void setAirflowTemperatureBreachAlertHoldDown(int val) {
        mAirflowTemperatureBreachAlertHoldDown = val;
    }
    
    public void setIgnoreCMReportedErrors(int val) {
        mIgnoreCMReportedError = val;
    }
    
    public int getAutoAwayTime() {
        return mAutoAwayTimePeriod;
    }
    
    public void setAutoAwayTime(int val) {
        mAutoAwayTimePeriod = val;
    }
    
    public int getShowRPMAlerts() {
        return mShowRPMAlerts;
    }
    
    public void setShowRPMAlerts(int val) {
        mShowRPMAlerts = val;
    }
    
    public int getForcedOccupiedZonePriority() {
        return mForcedOccupiedZonePriority;
    }
    
    public void setForcedOccupiedZonePriority(int val) {
        mForcedOccupiedZonePriority = val;
    }
    
    public int getForcedOccupiedTimePeriod() {
        return mForcedOccupiedTimePeriod;
    }
    
    public void setForcedOccupiedTimePeriod(int val) {
        mForcedOccupiedTimePeriod = val;
    }
    
    public int getHumidityThreshold() {
        return mHumidityThreshold;
    }
    
    public void setHumidityThreshold(int val) {
        mHumidityThreshold = val;
    }
    
    public int getHumidityPerDegreeFactor() {
        return mHumidityPerDegreeFactor;
    }
    
    public void setHumidityPerDegreeFactor(int val) {
        mHumidityPerDegreeFactor = val;
    }
    
    public boolean getUseSameOccuTempAcrossDays() {
        return mUseSameOccuTempAcrossDays;
    }
    
    public void setUseSameOccuTempAcrossDays(boolean val) {
        mUseSameOccuTempAcrossDays = val;
    }
    
    public int getDCVCO2ThresholdLevel() {
        return mDCVCO2ThresholdLevel;
    }
    
    public void setDCVCO2ThresholdLevel(int val) {
        mDCVCO2ThresholdLevel = val;
    }
    
    public int getDCVDamperOpeningRate() {
        return mDCVDamperOpeningRate;
    }
    
    public void setDCVDamperOpeningRate(int val) {
        mDCVDamperOpeningRate = val;
    }
    
    public int getDumbModeDCVDamperOpening() {
        return mDumbModeDCVDamperOpening;
    }
    
    public void setDumbModeDCVDamperOpening(int val) {
        mDumbModeDCVDamperOpening = val;
    }
    
    public int getZonePrioritySpread() {
        return mZonePrioritySpread;
    }
    
    public void setZonePrioritySpread(int val) {
        mZonePrioritySpread = val;
    }
    
    public double getZonePriorityMultiplier() {
        return mZonePriorityMultiplier;
    }
    
    public void setZonePriorityMultiplier(double val) {
        mZonePriorityMultiplier = val;
    }
    
    public int getAbnormalCurTempChangeAlertTrigger() {
        return mAbnormalCurTempChangeAlertTrigger;
    }
    
    public void setAbnormalCurTempChangeAlertTrigger(int val) {
        mAbnormalCurTempChangeAlertTrigger = val;
    }
    
    public int getUserLimitSpread() {
        return mUserLimitSpread;
    }
    
    public void setUserLimitSpread(int val) {
        mUserLimitSpread = val;
    }
    
    public double getSetbackMultiplier() {
        return mSetbackMultiplier;
    }
    
    public void setSetbackMultiplier(double val) {
        mSetbackMultiplier = val; //CCUUtils.roundTo2Decimal(val);
    }
    
    public int getPreconditioningRate() {
        return mPreconditioningRate;
    }
    
    public void setPreconditioningRate(int val) {
        mPreconditioningRate = val;
    }
    
    public int getEnthalpyCompensation() {
        return mEnthalpyCompensation;
    }
    
    public void setEnthalpyCompensation(int val) {
        mEnthalpyCompensation = val;
    }
    
    public int getOutsideAirMinTemp() {
        return mOutsideAirMinTemp;
    }
    
    public void setOutsideAirMinTemp(int val) {
        mOutsideAirMinTemp = val;
    }
    
    public int getOutsideAirMaxTemp() {
        return mOutsideAirMaxTemp;
    }
    
    public void setOutsideAirMaxTemp(int val) {
        mOutsideAirMaxTemp = val;
    }
    
    public int getOutsideAirMinHumidity() {
        return mOutsideAirMinHumidity;
    }
    
    public void setOutsideAirMinHumidity(int val) {
        mOutsideAirMinHumidity = val;
    }
    
    public int getOutsideAirMaxHumidity() {
        return mOutsideAirMaxHumidity;
    }
    
    public void setOutsideAirMaxHumidity(int val) {
        mOutsideAirMaxHumidity = val;
    }
    
    public int getEconomizerLoadThreshold() {
        return mEconomizerLoadThreshold;
    }
    
    public void setEconomizerLoadThreshold(int val) {
        mEconomizerLoadThreshold = val;
    }
    
    public int getEconomizerHoldTime() {
        return mEconomizerHoldTime;
    }
    
    public void setEconomizerHoldTime(int val) {
        mEconomizerHoldTime = val;
    }
    
    public int getEconomizerStage1LoadDrop() {
        return mEconomizerStage1LoadDrop;
    }
    
    public void setEconomizerStage1LoadDrop(int val) {
        mEconomizerStage1LoadDrop = val;
    }
    
    public double getEconPropCtrlSpread() {
        return mEconPropCtrlSpread;
    }
    
    public void setEconPropCtrlSpread(double val) {
        mEconPropCtrlSpread = val;
    }
    
    public int getEconIntegCtrlTimeOut() {
        return mEconIntegCtrlTimeOut;
    }
    
    public void setEconIntegCtrlTimeOut(int val) {
        mEconIntegCtrlTimeOut = val;
    }
    
    public double getAnalog1PropCtrlConstant() {
        return mAnalog1PropCtrlConstant;
    }
    
    public void setAnalog1PropCtrlConstant(double mAnalog1PropCtrlConstant) {
        this.mAnalog1PropCtrlConstant = mAnalog1PropCtrlConstant;
    }
    
    public double getAnalog1IntegCtrlConstant() {
        return mAnalog1IntegCtrlConstant;
    }
    
    public void setAnalog1IntegCtrlConstant(double mAnalog1IntegCtrlConstant) {
        this.mAnalog1IntegCtrlConstant = mAnalog1IntegCtrlConstant;
    }
    
    public int getAnalog1IntegCtrlTimeOut() {
        return mAnalog1IntegCtrlTimeOut;
    }
    
    public void setAnalog1IntegCtrlTimeOut(int mAnalog1IntegCtrlTimeOut) {
        this.mAnalog1IntegCtrlTimeOut = mAnalog1IntegCtrlTimeOut;
    }
    
    public double getAnalog1PropCtrlSpread() {
        return mAnalog1PropCtrlSpread;
    }
    
    public void setAnalog1PropCtrlSpread(double mAnalog1PropCtrlSpread) {
        this.mAnalog1PropCtrlSpread = mAnalog1PropCtrlSpread;
    }
    
    public double getAnalog3PropCtrlConstant() {
        return mAnalog3PropCtrlConstant;
    }
    
    public void setAnalog3PropCtrlConstant(double mAnalog3PropCtrlConstant) {
        this.mAnalog3PropCtrlConstant = mAnalog3PropCtrlConstant;
    }
    
    public double getAnalog3IntegCtrlConstant() {
        return mAnalog3IntegCtrlConstant;
    }
    
    public void setAnalog3IntegCtrlConstant(double mAnalog3IntegCtrlConstant) {
        this.mAnalog3IntegCtrlConstant = mAnalog3IntegCtrlConstant;
    }
    
    public int getAnalog3IntegCtrlTimeOut() {
        return mAnalog3IntegCtrlTimeOut;
    }
    
    public void setAnalog3IntegCtrlTimeOut(int mAnalog3IntegCtrlTimeOut) {
        this.mAnalog3IntegCtrlTimeOut = mAnalog3IntegCtrlTimeOut;
    }
    
    public double getAnalog3PropCtrlSpread() {
        return mAnalog3PropCtrlSpread;
    }
    
    public void setAnalog3PropCtrlSpread(double mAnalog3PropCtrlSpread) {
        this.mAnalog3PropCtrlSpread = mAnalog3PropCtrlSpread;
    }
    
    public double getAnalogFanSpeedMultiplier() {
        return mAnalogFanSpeedMultiplier;
    }
    
    public void setAnalogFanSpeedMultiplier(double mAnalogFanSpeedMultiplier) {
        this.mAnalogFanSpeedMultiplier = mAnalogFanSpeedMultiplier; //CCUUtils.roundTo2Decimal
        // (mAnalogFanSpeedMultiplier);
    }
    
    public int getAnalogMinHeating() {
        return mAnalogMinHeating;
    }
    
    public void setAnalogMinHeating(int mAnalogMinHeating) {
        this.mAnalogMinHeating = mAnalogMinHeating;
    }
    
    public int getAnalogMaxHeating() {
        return mAnalogMaxHeating;
    }
    
    public void setAnalogMaxHeating(int mAnalogMaxHeating) {
        this.mAnalogMaxHeating = mAnalogMaxHeating;
    }
    
    public int getAnalogMinCooling() {
        return mAnalogMinCooling;
    }
    
    public void setAnalogMinCooling(int mAnalogMinCooling) {
        this.mAnalogMinCooling = mAnalogMinCooling;
    }
    
    public int getAnalogMaxCooling() {
        return mAnalogMaxCooling;
    }
    
    public void setAnalogMaxCooling(int mAnalogMaxCooling) {
        this.mAnalogMaxCooling = mAnalogMaxCooling;
    }
    
    public double getAutoModeCoolHeatDXCILimit() {
        return mAutoModeCoolHeatDXCILimit;
    }
    
    public void setAutoModeCoolHeatDXCILimit(double mAutoModeCoolHeatDXCILimit) {
        this.mAutoModeCoolHeatDXCILimit = mAutoModeCoolHeatDXCILimit;
    }
    
    public double getAutoModeTotalDXCILimit() {
        return mAutoModeTotalDXCILimit;
    }
    
    public void setAutoModeTotalDXCILimit(double mAutoModeTotalDXCILimit) {
        this.mAutoModeTotalDXCILimit = mAutoModeTotalDXCILimit;
    }
    
    
    public boolean getUseCoolHeatDXCIForAutoMode() {
        return mUseCoolHeatDXCIForAutoMode;
    }
    
    public void setUseCoolHeatDXCIForAutoMode(boolean mUseCoolHeatDXCIForAutoMode) {
        this.mUseCoolHeatDXCIForAutoMode = mUseCoolHeatDXCIForAutoMode;
    }
    
    public boolean useCelsius() {
        return useCelsius;
    }
    
    public void setUseCelsius(boolean useCelcius) {
        if (this.useCelsius != useCelcius)
            //SystemSettingsData.setUseCelsius(useCelcius);
        this.useCelsius = useCelcius;
    }
    
    
    public boolean useMilitaryTime() {
        return useMilitaryTime;
    }
    
    public void setUseMilitaryTime(boolean useMilitaryTime) {
        if (this.useMilitaryTime != useMilitaryTime) {
            this.useMilitaryTime = useMilitaryTime;
//            for (FloorData floor : FloorData.getFloorData())
//                for (RoomData room : floor.getRoomData())
//                    for (FSVData fsv: room.getFSVData())
//                        fsv.sendUpdatedDamperPosToFSV();
            
        }
    }
    
    public boolean useInstantGratificationMode() {
        return useInstantGratificationMode;
    }
    
    public void setUseInstantGratificationMode(boolean useInstantGratificationMode) {
        this.useInstantGratificationMode = useInstantGratificationMode;
    }
    
    public boolean getFollowAutoModeSchedule() {
        return followAutoModeSchedule;
    }
    
    public void setFollowAutoModeSchedule(boolean followAutoModeSchedule) {
        this.followAutoModeSchedule = followAutoModeSchedule;
    }
    
    public boolean useOutsideTemperatureLockout() {
        return useOutsideTemperatureLockout;
    }
    
    public void setUseOutsideTemperatureLockout(boolean useOutsideTemperatureLockout) {
        this.useOutsideTemperatureLockout = useOutsideTemperatureLockout;
    }
    
    public int getFSVPairingStartAddress() {
        return FSVPairingStartAddress;
    }
    
    public void setFSVPairingStartAddress(int FSVPairingStartAddress) {
        this.FSVPairingStartAddress = FSVPairingStartAddress;
    }
    
    public int getNoCoolingBelowLockoutTemperature() {
        return outsideCoolingTempLockout;
    }
    
    public int getNoHeatingAboveLockoutTemperature() {
        return outsideHeatingTempLockout;
    }
    
    public void setOutsideTempLockoutHeating(int nNoHeatingAbove) {
        this.outsideHeatingTempLockout = nNoHeatingAbove;
    }
    
    public void setOutsideTempLockoutCooling(int nNoCoolingBelow) {
        this.outsideCoolingTempLockout = nNoCoolingBelow;
    }
    
    public void setStage1FanOnTime(int val) {
        mStage1FanOnTime = val;
    }
    
    public int getStage1FanOnTime() {
        return mStage1FanOnTime;
    }
    
    public void setStage1FanOffTime(int val) {
        mStage1FanOffTime = val;
    }
    
    public int getStage1FanOffTime() {
        return mStage1FanOffTime;
    }
    
    public int getBuildingAllowNoHotter() {
        return this.mBuildingAllowNoHotter;
    }
    
    public void setBuildingAllowNoHotter(int value) {
        this.mBuildingAllowNoHotter = value;
    }
    
    public int getBuildingAllowNoCooler() {
        return this.mBuildingAllowNoCooler;
    }
    public void setBuildingAllowNoCooler(int value) {
        this.mBuildingAllowNoCooler = value;
    }
    public int getUserAllowNoHotter() {
        return this.mUserAllowNoHotter;
    }
    public void setUserAllowNoHotter(int value) {
        this.mUserAllowNoHotter = value;
    }
    public int getUserAllowNoCooler() {
        return this.mUserAllowNoCooler;
    }
    public void setUserAllowNoCooler(int value) {
        this.mUserAllowNoCooler = value;
    }
    
    public int getWRMForHumidityValue() {
        return mWRMForHumidityValue;
    }
    
    public void setWRMForHumidityValue(int mWRMForHumidityValue) {
        this.mWRMForHumidityValue = mWRMForHumidityValue;
    }
    
    public boolean getUseExtHumiditySensor() {
        return mUseExtHumiditySensor;
    }
    
    public void setUseExtHumiditySensor(boolean bVal) {
        this.mUseExtHumiditySensor = bVal;
    }
    
    public int getExtHumiditySensorMinSP() {
        return mExtHumiditySensorMinSP;
    }
    
    public void setExtHumiditySensorMinSP(int val) {
        this.mExtHumiditySensorMinSP = val;
    }
    
    public int getExtHumiditySensorMaxSP() {
        return mExtHumiditySensorMaxSP;
    }
    
    public void setExtHumiditySensorMaxSP(int val) {
        this.mExtHumiditySensorMaxSP = val;
    }
    
    public int getExtHumiditySPTimeoutInterval() {
        return mExtHumiditySPTimeoutInterval;
    }
    
    public void setExtHumiditySensorSPTimeoutInterval(int val) {
        this.mExtHumiditySPTimeoutInterval = val;
    }
    
    public int getWRMForDumbModeTempValue() {
        return mWRMForDumbModeTempValue;
    }
    
    public void setWRMForDumbModeTempValue(int mWRMForDumbModeTempValue) {
        this.mWRMForDumbModeTempValue = mWRMForDumbModeTempValue;
    }
    
    public int getNO2ThresholdLevel() {
        return mNO2ThresholdLevel;
    }
    
    public void setNO2ThresholdLevel(int val) {
        mNO2ThresholdLevel = val;
    }
    public int getCOThresholdLevel() {
        return mCOThresholdLevel;
    }
    
    public void setCOThresholdLevel(int val) {
        mCOThresholdLevel = val;
    }
    public int getNO2DamperOpeningRate() {
        return mNO2DamperOpeningRate;
    }
    
    public void setNO2DamperOpeningRate(int val) {
        mNO2DamperOpeningRate = val;
    }
    public int getCODamperOpeningRate() {
        return mCODamperOpeningRate;
    }
    
    public void setCODamperOpeningRate(int val) {
        mCODamperOpeningRate = val;
    }
    
    public int getOAODamperPosMin() {
        return mOAODamperPosMin;
    }
    
    public void setOAODamperPosMin(int val) {
        mOAODamperPosMin = val;
    }
    
    public int getOAODamperPosMax() {
        return mOAODamperPosMax;
    }
    
    public void setOAODamperPosMax(int val) {
        mOAODamperPosMax = val;
    }
    
    public int getOAOExhaustFanThreshold() {
        return mOAOExhaustFanThreshold;
    }
    
    public void setOAOExhaustFanThreshold(int val) {
        mOAOExhaustFanThreshold = val;
    }
    
    public double getPressureDeadZone() {
        return mPressureDeadZone;
    }
    
    public void setPressureDeadZone(double val) {
        mPressureDeadZone = val; //CCUUtils.roundToTwoDecimal(val);
    }
    public double getPressureThresholdLevel() {
        return mPressureThresholdLevel;
    }
    
    public void setPressureThresholdLevel(double val) {
        mPressureThresholdLevel = val; //CCUUtils.roundToTwoDecimal(val);
    }
    
    public int getPressureDamperOpeningRate() {
        return mPressureDamperOpeningRate;
    }
    
    public void setPressureDamperOpeningRate(int val) {
        mPressureDamperOpeningRate = val;
    }
    
    public void setChangesForBuildingAndUserLimits( int buildingMax, int buildingMin, int userMax, int userMin){
//        if((this.mBuildingAllowNoHotter != buildingMax) || (this.mBuildingAllowNoCooler != buildingMin ) || (this.mUserAllowNoHotter != userMax) || (this.mUserAllowNoCooler != userMin)) {
//            if(SystemSettingsData.verifyAllValuesAreCorrect(buildingMax,buildingMin,userMax,userMin)) {
//                SystemSettingsData.updateSchedulesAndDesiredSetpoints(buildingMax, buildingMin, userMax, userMin, true);
//
//            }else
//                saveToKinvey();
//
//        }
    }
    public int getAnalog1MinValue() {
        return mAnalog1MinValue;
    }
    
    public void setAnalog1MinValue(int value) {
        this.mAnalog1MinValue = value;
    }
    public int getAnalog1MaxValue() {
        return mAnalog1MaxValue;
    }
    
    public void setAnalog1MaxValue(int value) {
        this.mAnalog1MaxValue = value;
    }
    
    public int getAnalog3MinValue() {
        return mAnalog3MinValue;
    }
    
    public void setAnalog3MinValue(int value) {
        this.mAnalog3MinValue = value;
    }
    
    public int getAnalog3MaxValue() {
        return mAnalog3MaxValue;
    }
    
    public void setAnalog3MaxValue(int value) {
        this.mAnalog3MaxValue = value;
    }
    
    public int getChilledWaterDeltatSetpoint() {
        return mChilledWaterDeltaTSetpoint;
    }
    
    public void setChilledWaterDeltatSetpoint(int value) {
        this.mChilledWaterDeltaTSetpoint = value;
    }
    
    public int getChilledWaterFlowSetpoint() {
        return mChilledWaterFlowSetpoint;
    }
    
    public void setChilledWaterFlowSetpoint(int val) {
        this.mChilledWaterFlowSetpoint = val;
    }
    
    public int getChilledWaterMaxFlowRate() {
        return mChilledWaterMaxFlowRate;
    }
    
    public void setChilledWaterMaxFlowRate(int val) {
        this.mChilledWaterMaxFlowRate = val;
    }
    
    public int getChilledWaterActuatorMinPos() {
        return mChilledWaterActuatorMinPos;
    }
    
    public void setChilledWaterActuatorMinPos(int value) {
        this.mChilledWaterActuatorMinPos = value;
    }
    
    public int getChilledWaterActuatorMaxPos() {
        return mChilledWaterActuatorMaxPos;
    }
    
    public void setChilledWaterActuatorMaxPos(int value) {
        this.mChilledWaterActuatorMaxPos = value;
    }
    
    public int getAHUFanMinLimit() {
        return mAHUFanMinLimit;
    }
    
    public void setAHUFanMinLimit(int value) {
        this.mAHUFanMinLimit = value;
    }
    
    public int getAHUFanMaxLimit() {
        return mAHUFanMaxLimit;
    }
    
    public void setAHUFanMaxLimit(int value) {
        this.mAHUFanMaxLimit = value;
    }
    
    public boolean isEnergyMeterMonitor() {
        return energyMeterMonitor;
    }
    
    public void setEnergyMeterMonitor(boolean energyMonitor) {
        this.energyMeterMonitor = energyMonitor;
    }
    
    public double getReheatOffset() {
        return mReheatOffset;
    }
    
    public void setReheatOffset(double value) {
        this.mReheatOffset = value;
    }
    
    public double getReheatDamperReopenOffset() {
        return mReheatDamperReopenOffset;
    }
    
    public void setReheatDamperReopenOffset(double value) {
        this.mReheatDamperReopenOffset = value;
    }
    
    public int getReheatMaxDamperPos() {
        return mReheatMaxDamperPos;
    }
    public void setReheatMaxDamperPos(int val) {
        this.mReheatMaxDamperPos = val;
    }
    public int getRefrigerationLowerLimit() {
        return mRefLowLimit;
    }
    
    public void setRefrigerationLowerLimit(int value) {
        this.mRefLowLimit = value;
    }
    
    public int getRefrigerationHigherLimit() {
        return mRefHighLimit;
    }
    
    public void setRefrigerationHigherLimit(int value) {
        this.mRefHighLimit = value;
    }
    
    public int getEnergyMeterSetpoint() {
        return mEnergyMeterSetpoint;
    }
    
    public void setEnergyMeterSetpoint(int val) {
        this.mEnergyMeterSetpoint = val;
    }
    
    
    
    public int getAlarmVolume() {
        return mAlarmVolume;
    }
    
    public boolean getUseSmartNodeInstall() {
        return mSmartNodeInstall;
    }
    
    public void setAlarmVolume(int val) {
        this.mAlarmVolume = val;
    }
    
    public void setUseSmartNodeInstall(boolean value) {
        this.mSmartNodeInstall = value;
    }
    public int getMinLightingControlOverrideInMinutes() {
        return mMinLightingControlOverrideInMinutes;
    }
    public void setMinLightingControlOverrideInMinutes(int val) {
        this.mMinLightingControlOverrideInMinutes = val;
    }
    public int getLightingIntensityOccupantDetected() {
        return mLightingIntensityForOccupancyDetect;
    }
    public void setLightingIntensityOccupantDetected(int val) {
        this.mLightingIntensityForOccupancyDetect = val;
    }
    
    public int getStandaloneCoolingDeadband() {
        return mStandaloneCoolingDeadband;
    }
    public void setStandaloneCoolingDeadband(int val) {
        this.mStandaloneCoolingDeadband = val;
    }
    public int getStandaloneHeatingDeadband() {
        return mStandaloneHeatingDeadband;
    }
    public void setStandaloneHeatingDeadband(int val) {
        this.mStandaloneHeatingDeadband = val;
    }
    public void updatePreconfigTuners(JSONObject preconfigData){
        if(preconfigData != null){
            id = DalContext.getInstance().getKinveyId();
            mVersion = getVersionNumber();
            mPropCtrlConstant = preconfigData.optDouble("mPropCtrlConstant",getPropConstant());
            mIntegCtrlConstant = preconfigData.optDouble("mIntegCtrlConstant",getIntegConstant());
            mZoneLoadCtrlConstant = preconfigData.optDouble("mZoneLoadCtrlConstant",getZoneLoadConstant());
            mCoolingRateCtrlConstant = preconfigData.optDouble("mCoolingRateCtrlConstant",getCoolingRateConstant());
            mCumulativeDamperPosTarget = preconfigData.optInt("mCumulativeDamperPosTarget",getCumulativeDamperPosTarget());
            mDamperPosForDeadZone = preconfigData.optInt("mDamperPosForDeadZone",getDamperPosForDeadZone());
            mIntegCtrlTimeOut = preconfigData.optInt("mIntegCtrlTimeOut",getIntegCtrlTimeOut());
            mPropCtrlSpread = preconfigData.optDouble("mPropCtrlSpread",getPropCtrlSpread());
            mPreConditioningPropCtrlMultiplier = preconfigData.optDouble("mPreConditioningPropCtrlMultiplier",getPreConditioningPropCtrlMultiplier());
            mDXCoilHoldTimeMultiplier = preconfigData.optInt("mDXCoilHoldTimeMultiplier",getDXCoilHoldTimeMultiplier());
            mPercentDeadZonesAllowed = preconfigData.optInt("mPercentDeadZonesAllowed",getPercentDeadZonesAllowed());
            mDXCoilOffsetLimitMultiplier = preconfigData.optDouble("mDXCoilOffsetLimitMultiplier",getDXCoilOffsetLimitMultiplier());
            mClockUpdateInterval = preconfigData.optInt("mClockUpdateInterval", getClockUpdateInterval());
            mBuildingToZoneTempLimitsDifferential = preconfigData.optInt("mBuildingToZoneTempLimitsDifferential",getBuildingToZoneTempLimitsDifferential());
            mBackpressureLimit  = preconfigData.optDouble("mBackpressureLimit",getBackpressureLimit());
            mCumDamperPosIncrForBackpressure = preconfigData.optInt("mCumDamperPosIncrForBackpressure",getCumDamperPosIncrForBackpressure());
            mMultizoneStage2Timer = preconfigData.optInt("mMultizoneStage2Timer",getMultizoneStage2Timer());
            mSinglezoneStage2Timer = preconfigData.optInt("mSinglezoneStage2Timer",getSinglezoneStage2Timer());
            mMultizoneStage2PercentDrop = preconfigData.optInt("mMultizoneStage2PercentDrop",getMultizoneStage2PercentDrop());
            mSinglezoneStage2Offset = preconfigData.optDouble("mSinglezoneStage2Offset",getSinglezoneStage2Offset());
            mIgnoreCMReportedError = preconfigData.optInt("mIgnoreCMReportedError",getIgnoreCMReportedErrors());
            mIgnoreFSVNotInDBError = preconfigData.optInt("mIgnoreFSVNotInDBError",getIgnoreFSVNotInDBError());
            mShowRPMAlerts = preconfigData.optInt("mShowRPMAlerts",getShowRPMAlerts());
            mHumidityPerDegreeFactor = preconfigData.optInt("mHumidityPerDegreeFactor",getHumidityPerDegreeFactor());
            mZonePrioritySpread = preconfigData.optInt("mZonePrioritySpread",getZonePrioritySpread());
            mZonePriorityMultiplier =preconfigData.optDouble("mZonePriorityMultiplier",getZonePriorityMultiplier());
            mUserLimitSpread = preconfigData.optInt("mUserLimitSpread",getUserLimitSpread());
            mEconIntegCtrlTimeOut = preconfigData.optInt("mEconIntegCtrlTimeOut",getEconIntegCtrlTimeOut());
            mEconPropCtrlSpread = preconfigData.optDouble("mEconPropCtrlSpread",getEconPropCtrlSpread());
            mAnalog1PropCtrlConstant = preconfigData.optDouble("mAnalog1PropCtrlConstant",getAnalog1PropCtrlConstant());
            mAnalog1PropCtrlSpread = preconfigData.optDouble("mAnalog1PropCtrlSpread", getAnalog1PropCtrlSpread());
            mAnalog1IntegCtrlConstant = preconfigData.optDouble("mAnalog1IntegCtrlConstant",getAnalog1IntegCtrlConstant());
            mAnalog1IntegCtrlTimeOut = preconfigData.optInt("mAnalog1IntegCtrlTimeOut", getAnalog1IntegCtrlTimeOut());
            mAnalog3PropCtrlConstant = preconfigData.optDouble("mAnalog3PropCtrlConstant", getAnalog3PropCtrlConstant());
            mAnalog3PropCtrlSpread = preconfigData.optDouble("mAnalog3PropCtrlSpread", getAnalog3PropCtrlSpread());
            mAnalog3IntegCtrlConstant = preconfigData.optDouble("mAnalog3IntegCtrlConstant", getAnalog3IntegCtrlConstant());
            mAnalog3IntegCtrlTimeOut = preconfigData.optInt("mAnalog3IntegCtrlTimeOut",getAnalog3IntegCtrlTimeOut());
            mAutoModeCoolHeatDXCILimit = preconfigData.optDouble("mAutoModeCoolHeatDXCILimit",getAutoModeCoolHeatDXCILimit());
            mAutoModeTotalDXCILimit = preconfigData.optDouble("mAutoModeTotalDXCILimit", getAutoModeTotalDXCILimit());
            mUseCoolHeatDXCIForAutoMode = preconfigData.optBoolean("mUseCoolHeatDXCIForAutoMode", getUseCoolHeatDXCIForAutoMode());
            
            mForcedOccupiedZonePriority = preconfigData.optInt("mForcedOccupiedZonePriority",getForcedOccupiedZonePriority());
            mTimeToDeclareDeadDamper = preconfigData.optInt("mTimeToDeclareDeadDamper",getTimeToDeclareDeadDamper());
            mAutoAwayTimePeriod = preconfigData.optInt("mAutoAwayTimePeriod", getAutoAwayTime());
            mForcedOccupiedTimePeriod = preconfigData.optInt("mForcedOccupiedTimePeriod",getForcedOccupiedTimePeriod());
            mRebalanceHoldTime = preconfigData.optInt("mRebalanceHoldTime",getRebalanceHoldTime());
            mStage1FanOnTime = preconfigData.optInt("mStage1FanOnTime",getStage1FanOnTime());
            mStage1FanOffTime = preconfigData.optInt("mStage1FanOffTime",getStage1FanOffTime());
            mHeartBeatInterval = preconfigData.optInt("mHeartBeatInterval",getHeartBeatUpdateInterval());
            mHeartBeatsToSkip = preconfigData.optInt("mHeartBeatsToSkip",getHeartBeatsToSkip());
            mAutoModeTemperatureMultiplier = preconfigData.optDouble("mAutoModeTemperatureMultiplier",getAutoModeTemperatureMultiplier());
            mAbnormalCurTempChangeAlertTrigger = preconfigData.optInt("mAbnormalCurTempChangeAlertTrigger",getAbnormalCurTempChangeAlertTrigger());
            mPreconditioningRate = preconfigData.optInt("mPreconditioningRate",getPreconditioningRate());
            mSetbackMultiplier = preconfigData.optDouble("mSetbackMultiplier",getSetbackMultiplier());
            mAnalogFanSpeedMultiplier = preconfigData.optDouble("mAnalogFanSpeedMultiplier",getAnalogFanSpeedMultiplier());
            mAnalogMinHeating = preconfigData.optInt("mAnalogMinHeating",getAnalogMinHeating());
            mAnalogMaxHeating = preconfigData.optInt("mAnalogMaxHeating",getAnalogMaxHeating());
            mAnalogMinCooling = preconfigData.optInt("mAnalogMinCooling",getAnalogMinCooling());
            mAnalogMaxCooling = preconfigData.optInt("mAnalogMaxCooling",getAnalogMaxCooling());
            mAnalog1MinValue = preconfigData.optInt("mAnalog1MinValue",getAnalog1MinValue());
            mAnalog1MaxValue = preconfigData.optInt("mAnalog1MaxValue",getAnalog1MaxValue());
            mAnalog3MinValue = preconfigData.optInt("mAnalog3MinValue",getAnalog3MinValue());
            mAnalog3MaxValue = preconfigData.optInt("mAnalog3MaxValue",getAnalog3MaxValue());
            mCoolingAirflowTemperature = preconfigData.optInt("mCoolingAirflowTemperature",getCoolingAirflowTemperature());
            mHeatingAirflowTemperature = preconfigData.optInt("mHeatingAirflowTemperature",getHeatingAirflowTemperature());
            mCoolingAirflowTempLowerOffset = preconfigData.optInt("mCoolingAirflowTempLowerOffset",getCoolingAirflowTemperatureLowerOffset());
            mCoolingAirflowTempUpperOffset = preconfigData.optInt("mCoolingAirflowTempUpperOffset",getCoolingAirflowTemperatureUpperOffset());
            mHeatingAirflowTempLowerOffset = preconfigData.optInt("mHeatingAirflowTempLowerOffset",getHeatingAirflowTemperatureLowerOffset());
            mHeatingAirflowTempUpperOffset = preconfigData.optInt("mHeatingAirflowTempUpperOffset", getHeatingAirflowTemperatureUpperOffset());
            mAirflowTemperatureBreachAlertHoldDown = preconfigData.optInt("mAirflowTemperatureBreachAlertHoldDown",getAirflowTemperatureBreachAlertHoldDown());
            
            mCoolingAirflow2TempLowerOffset = preconfigData.optInt("mCoolingAirflow2TempLowerOffset",getCoolingAirflow2TemperatureLowerOffset());
            mCoolingAirflow2TempUpperOffset = preconfigData.optInt("mCoolingAirflow2TempUpperOffset",getCoolingAirflow2TemperatureUpperOffset());
            mHeatingAirflow2TempLowerOffset = preconfigData.optInt("mHeatingAirflow2TempLowerOffset",getHeatingAirflow2TemperatureLowerOffset());
            mHeatingAirflow2TempUpperOffset = preconfigData.optInt("mHeatingAirflow2TempUpperOffset", getHeatingAirflow2TemperatureUpperOffset());
            mHumidityThreshold = preconfigData.optInt("mHumidityThreshold",getHumidityThreshold());
            mUseExtHumiditySensor = preconfigData.optBoolean("mUseExtHumiditySensor",getUseExtHumiditySensor());
            mExtHumiditySensorMinSP = preconfigData.optInt("mExtHumiditySensorMinSP", getExtHumiditySensorMinSP());
            mExtHumiditySensorMaxSP = preconfigData.optInt("mExtHumiditySensorMaxSP",getExtHumiditySensorMaxSP());
            mExtHumiditySPTimeoutInterval = preconfigData.optInt("mExtHumiditySPTimeoutInterval",getExtHumiditySPTimeoutInterval());
            mDCVDamperOpeningRate = preconfigData.optInt("mDCVDamperOpeningRate",getDCVDamperOpeningRate());
            mDumbModeDCVDamperOpening = preconfigData.optInt("mDumbModeDCVDamperOpening",getDumbModeDCVDamperOpening());
            mEnthalpyCompensation = preconfigData.optInt("mEnthalpyCompensation",getEnthalpyCompensation());
            mOutsideAirMinTemp = preconfigData.optInt("mOutsideAirMinTemp",getOutsideAirMinTemp());
            mOutsideAirMaxTemp = preconfigData.optInt("mOutsideAirMaxTemp",getOutsideAirMaxTemp());
            mOutsideAirMinHumidity = preconfigData.optInt("mOutsideAirMinHumidity",getOutsideAirMinHumidity());
            mOutsideAirMaxHumidity = preconfigData.optInt("mOutsideAirMaxHumidity",getOutsideAirMaxHumidity());
            mEconomizerLoadThreshold = preconfigData.optInt("mEconomizerLoadThreshold",getEconomizerLoadThreshold());
            mEconomizerHoldTime = preconfigData.optInt("mEconomizerHoldTime",getEconomizerHoldTime());
            mEconomizerStage1LoadDrop = preconfigData.optInt("mEconomizerStage1LoadDrop",getEconomizerStage1LoadDrop());
            mNO2DamperOpeningRate = preconfigData.optInt("mNO2DamperOpeningRate",getNO2DamperOpeningRate());
            mCODamperOpeningRate = preconfigData.optInt("mCODamperOpeningRate",getCODamperOpeningRate());
            mOAODamperPosMin = preconfigData.optInt("mOAODamperPosMin",getOAODamperPosMin());
            mOAODamperPosMax = preconfigData.optInt("mOAODamperPosMax",getOAODamperPosMax());
            mOAOExhaustFanThreshold = preconfigData.optInt("mOAOExhaustFanThreshold",getOAOExhaustFanThreshold());
            mPressureDamperOpeningRate = preconfigData.optInt("mPressureDamperOpeningRate",getPressureDamperOpeningRate());
            mPressureDeadZone = preconfigData.optDouble("mPressureDeadZone",getPressureDeadZone());
            mChilledWaterDeltaTSetpoint = preconfigData.optInt("mChilledWaterDeltaTSetpoint",getChilledWaterDeltatSetpoint());
            mChilledWaterActuatorMinPos = preconfigData.optInt("mChilledWaterActuatorMinPos",getChilledWaterActuatorMinPos());
            mChilledWaterActuatorMaxPos = preconfigData.optInt("mChilledWaterActuatorMaxPos",getChilledWaterActuatorMaxPos());
            mChilledWaterFlowSetpoint = preconfigData.optInt("mChilledWaterFlowSetpoint",getChilledWaterFlowSetpoint());
            mChilledWaterMaxFlowRate = preconfigData.optInt("mChilledWaterMaxFlowRate",getChilledWaterMaxFlowRate());
            mAHUFanMinLimit = preconfigData.optInt("mAHUFanMinLimit",getAHUFanMinLimit());
            mAHUFanMaxLimit = preconfigData.optInt("mAHUFanMaxLimit",getAHUFanMaxLimit());
            useCelsius = preconfigData.optBoolean("useCelsius",useCelsius());
            useMilitaryTime = preconfigData.optBoolean("useMilitaryTime",useMilitaryTime());
            mUseSameOccuTempAcrossDays  = preconfigData.optBoolean("mUseSameOccuTempAcrossDays",getUseSameOccuTempAcrossDays());
            followAutoModeSchedule = preconfigData.optBoolean("followAutoModeSchedule",getFollowAutoModeSchedule());
            useOutsideTemperatureLockout = preconfigData.optBoolean("useOutsideTemperatureLockout", useOutsideTemperatureLockout());
            outsideHeatingTempLockout = preconfigData.optInt("outsideHeatingTempLockout",getNoHeatingAboveLockoutTemperature());
            outsideCoolingTempLockout = preconfigData.optInt("outsideCoolingTempLockout",getNoCoolingBelowLockoutTemperature());
            useInstantGratificationMode = preconfigData.optBoolean("useInstantGratificationMode",useInstantGratificationMode());
            FSVPairingStartAddress = preconfigData.optInt("FSVPairingStartAddress",getFSVPairingStartAddress());
            mWRMForHumidityValue = preconfigData.optInt("mWRMForHumidityValue",getWRMForHumidityValue());
            mWRMForDumbModeTempValue = preconfigData.optInt("mWRMForDumbModeTempValue",getWRMForDumbModeTempValue());
            mDCVCO2ThresholdLevel = preconfigData.optInt("mDCVCO2ThresholdLevel",getDCVCO2ThresholdLevel());
            mCOThresholdLevel = preconfigData.optInt("mCOThresholdLevel",getCOThresholdLevel());
            mPressureThresholdLevel = preconfigData.optDouble("mPressureThresholdLevel",getPressureThresholdLevel());
            mNO2ThresholdLevel = preconfigData.optInt("mNO2ThresholdLevel",getNO2ThresholdLevel());
            energyMeterMonitor = preconfigData.optBoolean("energyMeterMonitor",isEnergyMeterMonitor());
            mReheatOffset = preconfigData.optDouble("mReheatOffset",getReheatOffset());
            mRefHighLimit = preconfigData.optInt("mRefHighLimit", getRefrigerationHigherLimit());
            mRefLowLimit = preconfigData.optInt("mRefLowLimit", getRefrigerationLowerLimit());
            mBuildingAllowNoHotter = preconfigData.optInt("mBuildingAllowNoHotter",getBuildingAllowNoHotter());
            mBuildingAllowNoCooler = preconfigData.optInt("mBuildingAllowNoCooler",getBuildingAllowNoCooler());
            mUserAllowNoCooler = preconfigData.optInt("mUserAllowNoCooler", getUserAllowNoCooler());
            mUserAllowNoHotter = preconfigData.optInt("mUserAllowNoHotter", getUserAllowNoHotter());
            mReheatDamperReopenOffset = preconfigData.optDouble("mReheatDamperReopenOffset",getReheatDamperReopenOffset());
            mReheatMaxDamperPos = preconfigData.optInt("mReheatMaxDamperPos",getReheatMaxDamperPos());
            mDXCoilOffsetHeatOffset = preconfigData.optDouble("mDXCoilOffsetHeatOffset",getDxCoilOffsetHeatOffset());
            mDXCoilOffsetCoolOffset = preconfigData.optDouble("mDXCoilOffsetCoolOffset",getDXCoilOffsetCoolOffset());
            mEnergyMeterSetpoint = preconfigData.optInt("mEnergyMeterSetpoint",getEnergyMeterSetpoint());
            mSmartNodeInstall = preconfigData.optBoolean("mSmartNodeInstall",getUseSmartNodeInstall());
            Log.d("CCU_REPLACE","updatePreconfigData===>"+preconfigData.optInt("mBuildingAllowNoHotter",0)+","+mBuildingAllowNoHotter+","+
                                preconfigData.optInt("mBuildingAllowNoCooler",0)+","+mBuildingAllowNoCooler);
            //save();
        }
    }
    
    public GenericJson algoTunerBackup(){
        GenericJson algoTuners = new GenericJson();
        algoTuners.put("id",DalContext.getInstance().getKinveyId());
        algoTuners.put("mVersion",getVersionNumber());
        algoTuners.put("mPropCtrlConstant",getPropConstant());
        algoTuners.put("mIntegCtrlConstant",getIntegConstant());
        algoTuners.put("mZoneLoadCtrlConstant",getZoneLoadConstant());
        algoTuners.put("mCoolingRateCtrlConstant",getCoolingRateConstant());
        algoTuners.put("mCumulativeDamperPosTarget",getCumulativeDamperPosTarget());
        algoTuners.put("mDamperPosForDeadZone",getDamperPosForDeadZone());
        algoTuners.put("mIntegCtrlTimeOut",getIntegCtrlTimeOut());
        algoTuners.put("mPropCtrlSpread",getPropCtrlSpread());
        algoTuners.put("mPreConditioningPropCtrlMultiplier",getPreConditioningPropCtrlMultiplier());
        algoTuners.put("mDXCoilHoldTimeMultiplier",getDXCoilHoldTimeMultiplier());
        algoTuners.put("mPercentDeadZonesAllowed",getPercentDeadZonesAllowed());
        algoTuners.put("mDXCoilOffsetLimitMultiplier",getDXCoilOffsetLimitMultiplier());
        algoTuners.put("mClockUpdateInterval", getClockUpdateInterval());
        algoTuners.put("mBuildingToZoneTempLimitsDifferential",getBuildingToZoneTempLimitsDifferential());
        algoTuners.put("mBackpressureLimit",getBackpressureLimit());
        algoTuners.put("mCumDamperPosIncrForBackpressure",getCumDamperPosIncrForBackpressure());
        algoTuners.put("mMultizoneStage2Timer",getMultizoneStage2Timer());
        algoTuners.put("mSinglezoneStage2Timer",getSinglezoneStage2Timer());
        algoTuners.put("mMultizoneStage2PercentDrop",getMultizoneStage2PercentDrop());
        algoTuners.put("mSinglezoneStage2Offset",getSinglezoneStage2Offset());
        algoTuners.put("mIgnoreCMReportedError",getIgnoreCMReportedErrors());
        algoTuners.put("mIgnoreFSVNotInDBError",getIgnoreFSVNotInDBError());
        algoTuners.put("mShowRPMAlerts",getShowRPMAlerts());
        algoTuners.put("mHumidityPerDegreeFactor",getHumidityPerDegreeFactor());
        algoTuners.put("mZonePrioritySpread",getZonePrioritySpread());
        algoTuners.put("mZonePriorityMultiplier",getZonePriorityMultiplier());
        algoTuners.put("mUserLimitSpread",getUserLimitSpread());
        algoTuners.put("mEconIntegCtrlTimeOut",getEconIntegCtrlTimeOut());
        algoTuners.put("mEconPropCtrlSpread",getEconPropCtrlSpread());
        algoTuners.put("mAnalog1PropCtrlConstant",getAnalog1PropCtrlConstant());
        algoTuners.put("mAnalog1PropCtrlSpread", getAnalog1PropCtrlSpread());
        algoTuners.put("mAnalog1IntegCtrlConstant",getAnalog1IntegCtrlConstant());
        algoTuners.put("mAnalog1IntegCtrlTimeOut", getAnalog1IntegCtrlTimeOut());
        algoTuners.put("mAnalog3PropCtrlConstant", getAnalog3PropCtrlConstant());
        algoTuners.put("mAnalog3PropCtrlSpread", getAnalog3PropCtrlSpread());
        algoTuners.put("mAnalog3IntegCtrlConstant", getAnalog3IntegCtrlConstant());
        algoTuners.put("mAnalog3IntegCtrlTimeOut",getAnalog3IntegCtrlTimeOut());
        algoTuners.put("mAutoModeCoolHeatDXCILimit",getAutoModeCoolHeatDXCILimit());
        algoTuners.put("mAutoModeTotalDXCILimit", getAutoModeTotalDXCILimit());
        algoTuners.put("mUseCoolHeatDXCIForAutoMode", getUseCoolHeatDXCIForAutoMode());
        algoTuners.put("mForcedOccupiedZonePriority",getForcedOccupiedZonePriority());
        algoTuners.put("mTimeToDeclareDeadDamper",getTimeToDeclareDeadDamper());
        algoTuners.put("mAutoAwayTimePeriod", getAutoAwayTime());
        algoTuners.put("mForcedOccupiedTimePeriod",getForcedOccupiedTimePeriod());
        algoTuners.put("mRebalanceHoldTime",getRebalanceHoldTime());
        algoTuners.put("mStage1FanOnTime",getStage1FanOnTime());
        algoTuners.put("mStage1FanOffTime",getStage1FanOffTime());
        algoTuners.put("mHeartBeatInterval",getHeartBeatUpdateInterval());
        algoTuners.put("mHeartBeatsToSkip",getHeartBeatsToSkip());
        algoTuners.put("mAutoModeTemperatureMultiplier",getAutoModeTemperatureMultiplier());
        algoTuners.put("mAbnormalCurTempChangeAlertTrigger",getAbnormalCurTempChangeAlertTrigger());
        algoTuners.put("mPreconditioningRate",getPreconditioningRate());
        algoTuners.put("mSetbackMultiplier",getSetbackMultiplier());
        algoTuners.put("mAnalogFanSpeedMultiplier",getAnalogFanSpeedMultiplier());
        algoTuners.put("mAnalogMinHeating",getAnalogMinHeating());
        algoTuners.put("mAnalogMaxHeating",getAnalogMaxHeating());
        algoTuners.put("mAnalogMinCooling",getAnalogMinCooling());
        algoTuners.put("mAnalogMaxCooling",getAnalogMaxCooling());
        algoTuners.put("mAnalog1MinValue",getAnalog1MinValue());
        algoTuners.put("mAnalog1MaxValue",getAnalog1MaxValue());
        algoTuners.put("mAnalog3MinValue",getAnalog3MinValue());
        algoTuners.put("mAnalog3MaxValue",getAnalog3MaxValue());
        algoTuners.put("mCoolingAirflowTemperature",getCoolingAirflowTemperature());
        algoTuners.put("mHeatingAirflowTemperature",getHeatingAirflowTemperature());
        algoTuners.put("mCoolingAirflowTempLowerOffset",getCoolingAirflowTemperatureLowerOffset());
        algoTuners.put("mCoolingAirflowTempUpperOffset",getCoolingAirflowTemperatureUpperOffset());
        algoTuners.put("mHeatingAirflowTempLowerOffset",getHeatingAirflowTemperatureLowerOffset());
        algoTuners.put("mHeatingAirflowTempUpperOffset", getHeatingAirflowTemperatureUpperOffset());
        algoTuners.put("mAirflowTemperatureBreachAlertHoldDown",getAirflowTemperatureBreachAlertHoldDown());
        
        algoTuners.put("mCoolingAirflow2TempLowerOffset",getCoolingAirflow2TemperatureLowerOffset());
        algoTuners.put("mCoolingAirflow2TempUpperOffset",getCoolingAirflow2TemperatureUpperOffset());
        algoTuners.put("mHeatingAirflow2TempLowerOffset",getHeatingAirflow2TemperatureLowerOffset());
        algoTuners.put("mHeatingAirflow2TempUpperOffset", getHeatingAirflow2TemperatureUpperOffset());
        algoTuners.put("mHumidityThreshold",getHumidityThreshold());
        algoTuners.put("mUseExtHumiditySensor",getUseExtHumiditySensor());
        algoTuners.put("mExtHumiditySensorMinSP", getExtHumiditySensorMinSP());
        algoTuners.put("mExtHumiditySensorMaxSP",getExtHumiditySensorMaxSP());
        algoTuners.put("mExtHumiditySPTimeoutInterval",getExtHumiditySPTimeoutInterval());
        algoTuners.put("mDCVDamperOpeningRate",getDCVDamperOpeningRate());
        algoTuners.put("mDumbModeDCVDamperOpening",getDumbModeDCVDamperOpening());
        algoTuners.put("mEnthalpyCompensation",getEnthalpyCompensation());
        algoTuners.put("mOutsideAirMinTemp",getOutsideAirMinTemp());
        algoTuners.put("mOutsideAirMaxTemp",getOutsideAirMaxTemp());
        algoTuners.put("mOutsideAirMinHumidity",getOutsideAirMinHumidity());
        algoTuners.put("mOutsideAirMaxHumidity",getOutsideAirMaxHumidity());
        algoTuners.put("mEconomizerLoadThreshold",getEconomizerLoadThreshold());
        algoTuners.put("mEconomizerHoldTime",getEconomizerHoldTime());
        algoTuners.put("mEconomizerStage1LoadDrop",getEconomizerStage1LoadDrop());
        algoTuners.put("mNO2DamperOpeningRate",getNO2DamperOpeningRate());
        algoTuners.put("mCODamperOpeningRate",getCODamperOpeningRate());
        algoTuners.put("mOAODamperPosMin",getOAODamperPosMin());
        algoTuners.put("mOAODamperPosMax",getOAODamperPosMax());
        algoTuners.put("mOAOExhaustFanThreshold",getOAOExhaustFanThreshold());
        algoTuners.put("mPressureDamperOpeningRate",getPressureDamperOpeningRate());
        algoTuners.put("mPressureDeadZone",getPressureDeadZone());
        algoTuners.put("mChilledWaterDeltaTSetpoint",getChilledWaterDeltatSetpoint());
        algoTuners.put("mChilledWaterActuatorMinPos",getChilledWaterActuatorMinPos());
        algoTuners.put("mChilledWaterActuatorMaxPos",getChilledWaterActuatorMaxPos());
        algoTuners.put("mChilledWaterFlowSetpoint",getChilledWaterFlowSetpoint());
        algoTuners.put("mChilledWaterMaxFlowRate",getChilledWaterMaxFlowRate());
        algoTuners.put("mAHUFanMinLimit",getAHUFanMinLimit());
        algoTuners.put("mAHUFanMaxLimit",getAHUFanMaxLimit());
        algoTuners.put("useCelsius",useCelsius());
        algoTuners.put("useMilitaryTime",useMilitaryTime());
        algoTuners.put("mUseSameOccuTempAcrossDays",getUseSameOccuTempAcrossDays());
        algoTuners.put("followAutoModeSchedule",getFollowAutoModeSchedule());
        algoTuners.put("useOutsideTemperatureLockout", useOutsideTemperatureLockout());
        algoTuners.put("outsideHeatingTempLockout",getNoHeatingAboveLockoutTemperature());
        algoTuners.put("outsideCoolingTempLockout",getNoCoolingBelowLockoutTemperature());
        algoTuners.put("useInstantGratificationMode",useInstantGratificationMode());
        algoTuners.put("FSVPairingStartAddress",getFSVPairingStartAddress());
        algoTuners.put("mWRMForHumidityValue",getWRMForHumidityValue());
        algoTuners.put("mWRMForDumbModeTempValue",getWRMForDumbModeTempValue());
        algoTuners.put("mDCVCO2ThresholdLevel",getDCVCO2ThresholdLevel());
        algoTuners.put("mCOThresholdLevel",getCOThresholdLevel());
        algoTuners.put("mPressureThresholdLevel",getPressureThresholdLevel());
        algoTuners.put("mNO2ThresholdLevel",getNO2ThresholdLevel());
        algoTuners.put("energyMeterMonitor",isEnergyMeterMonitor());
        algoTuners.put("mReheatOffset",getReheatOffset());
        algoTuners.put("mRefHighLimit", getRefrigerationHigherLimit());
        algoTuners.put("mRefLowLimit", getRefrigerationLowerLimit());
        algoTuners.put("mBuildingAllowNoHotter",getBuildingAllowNoHotter());
        algoTuners.put("mBuildingAllowNoCooler",getBuildingAllowNoCooler());
        algoTuners.put("mUserAllowNoCooler", getUserAllowNoCooler());
        algoTuners.put("mUserAllowNoHotter", getUserAllowNoHotter());
        algoTuners.put("mReheatDamperReopenOffset",getReheatDamperReopenOffset());
        algoTuners.put("mReheatMaxDamperPos",getReheatMaxDamperPos());
        algoTuners.put("mDXCoilOffsetHeatOffset",getDxCoilOffsetHeatOffset());
        algoTuners.put("mDXCoilOffsetCoolOffset",getDXCoilOffsetCoolOffset());
        algoTuners.put("mEnergyMeterSetpoint",getEnergyMeterSetpoint());
        algoTuners.put("mSmartNodeInstall",getUseSmartNodeInstall());
        return algoTuners;
    }
}
