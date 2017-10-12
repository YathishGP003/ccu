package a75f.io.dal;

import com.google.api.client.util.Key;

import java.util.HashMap;

/**
 * Created by Yinten on 9/20/2017.
 */

public class AlgoTuningParameters
{
    //Max volume limit is 7;
    public static final int     LIGHTING_INTENSITY_OCCUPANT_DETECTED     = 75;
    public static final int     MIN_LIGHTING_CONTROL_OVERRIDE_IN_MINUTES = 20;
    static final        String  KINVEY_COLLECTION_NAME                   = "00AlgoTuningParameters";
    static final        int     ALGO_TUNERS_VERSION                      = 62;
    static final        int     ZONE_DEAD_TIME                           = 15;
    static final        int     BUILDING_TO_ZONE_DIFF                    = 3;
    static final        int     CM_HEART_BEAT_INTERVAL                   = 1;
    static final        int     HEART_BEAT_TO_SKIP                       = 5;
    static final        double  AUTO_MODE_CHANGE_OVER_MULTIPLER          = 1.0;
    static final        int     AUTO_WAY_TIME                            = 60;
    static final        int     REBALANCE_HOLD_TIME                      = 20;
    static final        int     FORCED_OCCUPIED_TIME                     = 120;
    static final        int     COOLING_AIRFLOW_TEMP                     = 60;
    static final        int     HEATING_AIRFLOW_TEMP                     = 105;
    static final        int     COOLING_AIRFLOW_TEMP_LOWER_OFFSET        = -20;
    static final        int     COOLING_AIRFLOW_TEMP_UPPER_OFFSET        = -8;
    static final        int     HEATING_AIRFLOW_TEMP_LOWER_OFFSET        = 25;
    static final        int     HEATING_AIRFLOW_TEMP_UPPER_OFFSET        = 40;
    static final        int     AIRFLOW_TEMP_BREACH_HOLD_DOWN            = 5;
    static final        int     HUMIDITY_THRESHOLD                       = 35;
    static final        int     HUMIDITY_COMP_FACTOR                     = 10;
    static final        boolean USE_SAME_OCCU_TEMP_ACROSS_DAYS           = true;
    static final        int     DCV_CO2_THRESHOLD_LEVEL                  = 1000;
    static final        int     DCV_DAMPER_OPENING_RATE                  = 10;
    static final        int     DUMB_MODE_DCV_DAMPER_OPENING             = 30;
    static final        int     ABNORMAL_CUR_TEMP_TRIGGER_VAL            = 4;
    static final        int     USER_LIMIT_SPREAD                        = 3;
    static final        int     PRECONDTION_RATE                         = 15;
    static final        double  SETBACK_MULTIPLIER                       = 1.5;
    static final        int     ENTHALPY_COMPENSATION                    = 0;
    static final        int     OUTSIDE_AIR_MIN_TEMP                     = 0;
    static final        int     OUTSIDE_AIR_MAX_TEMP                     = 70;
    static final        int     OUTSIDE_AIR_MIN_HUMIDITY                 = 10;
    static final        int     OUTSIDE_AIR_MAX_HUMIDITY                 = 95;
    static final        int     ECONOMIZER_LOAD_THRESHOLD                = 30;
    static final        int     ECONOMIZER_HOLDTIME                      = 15;
    static final        int     ECONOMIZER_LOAD_DROP                     = 30;
    static final        double  ANALOG_FAN_SPEED_MULTIPLIER              = 1.0;
    static final        int     ANALOG_MIN_HEATING                       = 50;
    static final        int     ANALOG_MAX_HEATING                       = 20;
    static final        int     ANALOG_MIN_COOLING                       = 70;
    static final        int     ANALOG_MAX_COOLING                       = 100;
    //default values for algo tuners
    static final        double  PC_CONST                                 = 0.5; //prpcontrlConstant
    static final        double  IC_CONST                                 = 0.5;
    // mIntegCtrlConstant
    static final        int     CUM_DAMPER_POS_TARGET                    = 70;
    static final        int     IC_TIMEOUT                               = 30;
    static final        double  PC_SPREAD                                = 2.0;
    static final        double  PC_MULTIPLIER                            = 2.0;
    static final        int     DXCOIL_HOLD_TIME_MULITPLIER              = 2;
    static final        int     PERCENT_DEAD_ZONES                       = 50;
    static final        double  DXCOIL_OFFSET_LIMIT_MULTIPLIER           = 0.5;
    static final        double  DXCOIL_OFFSET_HEAT_OFFSET                = 0.0;
    static final        double  DXCOIL_OFFSET_COOL_OFFSET                = 0.0;
    static final        int     CLOCK_INTERVAL                           = 15;
    static final        double  BACK_PRESSURE_LIMIT                      = 2.5;
    static final        int     CUM_DAMPERPOS_INC_BP                     = 5;
    static final        int     MULTI_ZONE_STAGE2_TIMER                  = 15;
    static final        int     SINGLE_ZONE_STAGE2_TIMER                 = 2;
    static final        int     MULTI_ZONE_STAGE2_PDROP                  = 50;
    static final        int     SINGLE_ZONE_STAGE2_OFFSET                = 2;
    static final        int     IGNORE_CM_REPORT_ERR                     = 1;
    static final        int     IGNORE_FSV_NOT_INDBERR                   = 1;
    static final        int     SHOW_RPM_ALERTS                          = 0;
    static final        int     FORCED_OCCUPIED_ZONE_PRIORITY            = 50000;
    static final        int     ZONE_PRIORITY_SPREAD                     = 2;
    static final        double  ZONE_PRIORITY_MULTIPLIER                 = 1.3;
    static final        int     ECON_IC_TIMEOUT                          = 30;
    static final        double  ECON_PC_SPREAD                           = 2.0;
    static final        double  ANALOG1_PC_CONST                         = 0.5;
    static final        double  ANALOG1_IC_CONST                         = 0.5;
    static final        int     ANALOG1_IC_TIMEOUT                       = 30;
    static final        double  ANALOG1_PC_SPREAD                        = 2.0;
    static final        double  ANALOG3_PC_CONST                         = 0.5;
    static final        double  ANALOG3_IC_CONST                         = 0.5;
    static final        int     ANALOG3_IC_TIMEOUT                       = 30;
    static final        double  ANALOG3_PC_SPREAD                        = 2.0;
    static final        double  AUTO_MODE_COOL_HEAT_DXCILIMIT            = 2.0;
    static final        double  AUTO_MODE_TOTAL_DXCILIMIT                = 0.2;
    static final        boolean USE_COOLHEAT_DXCI_AUTOMODE               = false;
    static final        boolean USE_INSTANT_GRATIFICATION_MODE           = false;
    static final        boolean FOLLOW_AUTO_SCHEDULE                     = false;
    static final        boolean USE_OUTSIDE_TEMP_LOCKOUT                 = false;
    static final        boolean USE_CELSIUS                              = false;
    static final        boolean USE_MILITARY_TIME                        = false;
    static final        boolean SN_INSTALL                               = false;
    static final        int     FSV_START_ADDRESS                        = 1000;
    static final        int     NO_HEATING_ABOVE                         = 80;
    static final        int     NO_COOLING_BELOW                         = 60;
    static final        int     ZONE_DUMB_TEMP                           = 3; //curTemp * 1.5
    static final        int     STAGE1_FAN_ONTIME                        = 5;
    static final        int     STAGE1_FAN_OFFTIME                       = 0;
    static final        int     BUILDING_MAX_TEMP                        = 85; // in deg F
    static final        int     BUILDING_MIN_TEMP                        = 60; // in deg F
    static final        int     USER_MAX_TEMP                            = 73; //in deg F
    static final        int     USER_MIN_TEMP                            = 70; //in def F
    static final        int     WRM_FOR_HUMIDITY_VALUE                   = 0;
    static final        boolean USE_EXT_HUMIDITY_SENSOR                  = false;
    static final        int     EXT_HUMIDITY_SENSOR_MIN_SP               = 30;
    static final        int     EXT_HUMIDITY_SENSOR_MAX_SP               = 70;
    static final        int     EXT_HUMIDITY_SENSOR_SP_TIMEOUT           = 60;
    static final        int     WRM_FOR_DUMB_MODE_TEMP_VALUE             = 0;
    static final        int     NO2_THRESHOLD_LEVEL                      = 5;
    static final        int     CO_THRESHOLD_LEVEL                       = 50;
    static final        int     NO2_DAMPER_OPENING_RATE                  = 20;
    static final        int     CO_DAMPER_OPENING_RATE                   = 10;
    static final        int     OAO_DAMPER_POS_MIN                       = 0;
    static final        int     OAO_DAMPER_POS_MAX                       = 100;
    static final        int     OAO_EXHAUST_FAN_THRESHOLD                = 50;
    static final        double  PRESSURE_THRESHOLD_LEVEL                 = 0.01;
    static final        int     PRESSURE_DAMPER_OPENING_RATE             = 5;
    static final        int     ANALOG1_MIN_VALUE                        = 0;
    static final        int     ANALOG1_MAX_VALUE                        = 10;
    static final        int     ANALOG3_MIN_VALUE                        = 0;
    static final        int     ANALOG3_MAX_VALUE                        = 10;
    static final        int     COOLING_AIRFLOW_TEMP_STAGE2_LOWER_OFFSET = -25;
    static final        int     COOLING_AIRFLOW_TEMP_STAGE2_UPPER_OFFSET = -12;
    static final        int     HEATING_AIRFLOW_TEMP_STAGE2_LOWER_OFFSET = 35;
    static final        int     HEATING_AIRFLOW_TEMP_STAGE2_UPPER_OFFSET = 50;
    static final        int     CHILLED_WATER_DELTAT_SETPOINT            = 10;
    static final        int     CHILLED_WATER_FLOW_SETPOINT              = 100;
    static final        int     CHILLED_WATER_MAX_FLOWRATE               = 100;
    static final        int     CHILLED_WATER_ACTUATOR_MIN_POS           = 0;
    static final        int     CHILLED_WATER_ACTUATOR_MAX_POS           = 100;
    static final        int     AHU_FAN_MIN_LIMIT                        = 0;
    static final        int     AHU_FAN_MAX_LIMIT                        = 100;
    static final        boolean ENERGY_METER_MONITOR                     = false;
    static final        double  PRESSURE_DEAD_ZONE                       = 0.01;
    static final        double  REHEAT_OFFSET                            = 1.0;
    static final        int     REFRIGERATION_LOWER_LIMIT                = 23;
    static final        int     REFRIGERATION_HIGHER_LIMIT               = 45;
    static final        double  REHEAT_DAMPER_REOPEN_OFFSET              = 2.0;
    static final        int     REHEAT_MAX_DAMPER_POS                    = 40;
    static final        int     ALARM_VOLUME_DEFAULT                     = 0;
    static final        int     SSE_COOLING_DEADBAND                     = 1;  //(°F)
    static final        int     SSE_HEATING_DEADBAND                     = 1;  //(°F)
    static final        int     ENERGY_METER_SETPOINT                    = 10;
    static final        int     SSE_ZONE_SETBACK                         = 5;
    
    @Key("_id")
    private String id = "0";
    
    @Key("_ccuName")
    private String mCCUName = "";
    
    @Key("_version")
    private int    mVersion                   = -1;
    @Key
    private double mPropCtrlConstant          = PC_CONST;
    @Key
    private double mIntegCtrlConstant         = IC_CONST;
    @Key
    private double mZoneLoadCtrlConstant      = 0;
    @Key
    private double mCoolingRateCtrlConstant   = 0;
    @Key
    private int    mCumulativeDamperPosTarget = CUM_DAMPER_POS_TARGET;
    
    @Key
    private int mDamperPosForDeadZone    = CUM_DAMPER_POS_TARGET;
    @Key
    private int mTimeToDeclareDeadDamper = ZONE_DEAD_TIME; // mins
    @Key
    private int mIntegCtrlTimeOut        = IC_TIMEOUT;
    
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
    private double mBackpressureLimit               = BACK_PRESSURE_LIMIT;
    @Key
    private int    mCumDamperPosIncrForBackpressure = CUM_DAMPERPOS_INC_BP;
    
    @Key
    private int mMultizoneStage2Timer  = MULTI_ZONE_STAGE2_TIMER;
    @Key
    private int mSinglezoneStage2Timer = SINGLE_ZONE_STAGE2_TIMER;
    
    @Key
    private int    mMultizoneStage2PercentDrop = MULTI_ZONE_STAGE2_PDROP;
    @Key
    private double mSinglezoneStage2Offset     = SINGLE_ZONE_STAGE2_OFFSET;
    
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
    private int mHumidityThreshold = HUMIDITY_THRESHOLD;
    
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
    private double mAnalog1PropCtrlConstant  = ANALOG1_PC_CONST;
    @Key
    private double mAnalog1IntegCtrlConstant = ANALOG1_IC_CONST;
    @Key
    private int    mAnalog1IntegCtrlTimeOut  = ANALOG1_IC_TIMEOUT;
    @Key
    private double mAnalog1PropCtrlSpread    = ANALOG1_PC_SPREAD;
    
    @Key
    private double mAnalog3PropCtrlConstant  = ANALOG3_PC_CONST;
    @Key
    private double mAnalog3IntegCtrlConstant = ANALOG3_IC_CONST;
    @Key
    private int    mAnalog3IntegCtrlTimeOut  = ANALOG3_IC_TIMEOUT;
    @Key
    private double mAnalog3PropCtrlSpread    = ANALOG3_PC_SPREAD;
    
    @Key
    private double mAnalogFanSpeedMultiplier = ANALOG_FAN_SPEED_MULTIPLIER;
    @Key
    private int    mAnalogMinHeating         = ANALOG_MIN_HEATING;
    @Key
    private int    mAnalogMaxHeating         = ANALOG_MAX_HEATING;
    @Key
    private int    mAnalogMinCooling         = ANALOG_MIN_COOLING;
    @Key
    private int    mAnalogMaxCooling         = ANALOG_MAX_COOLING;
    
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
    private int mUserAllowNoHotter     = USER_MAX_TEMP;
    @Key
    private int mUserAllowNoCooler     = USER_MIN_TEMP;
    
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
    private int mCOThresholdLevel  = CO_THRESHOLD_LEVEL;
    
    @Key
    private int mNO2DamperOpeningRate = NO2_DAMPER_OPENING_RATE;
    
    @Key
    private int    mCODamperOpeningRate       = CO_DAMPER_OPENING_RATE;
    @Key
    private int    mOAODamperPosMin           = OAO_DAMPER_POS_MIN;
    @Key
    private int    mOAODamperPosMax           = OAO_DAMPER_POS_MAX;
    @Key
    private int    mOAOExhaustFanThreshold    = OAO_EXHAUST_FAN_THRESHOLD;
    @Key
    private double mPressureThresholdLevel    = PRESSURE_THRESHOLD_LEVEL;
    @Key
    private double mPressureDeadZone          = PRESSURE_DEAD_ZONE;
    @Key
    private int    mPressureDamperOpeningRate = PRESSURE_DAMPER_OPENING_RATE;
    
    @Key
    private int mAnalog1MinValue               = ANALOG1_MIN_VALUE;
    @Key
    private int mAnalog1MaxValue               = ANALOG1_MAX_VALUE;
    @Key
    private int mAnalog3MinValue               = ANALOG3_MIN_VALUE;
    @Key
    private int mAnalog3MaxValue               = ANALOG3_MAX_VALUE;
    @Key
    private int mCoolingAirflowTempLowerOffset = COOLING_AIRFLOW_TEMP_LOWER_OFFSET;
    // default offset -20
    
    @Key
    private int mCoolingAirflowTempUpperOffset = COOLING_AIRFLOW_TEMP_UPPER_OFFSET;
    //default offset -8
    
    @Key
    private int mHeatingAirflowTempLowerOffset = HEATING_AIRFLOW_TEMP_LOWER_OFFSET;
    //default offset 25
    
    @Key
    private int mHeatingAirflowTempUpperOffset  = HEATING_AIRFLOW_TEMP_UPPER_OFFSET;
    //default offset 40
    @Key
    private int mCoolingAirflow2TempLowerOffset = COOLING_AIRFLOW_TEMP_STAGE2_LOWER_OFFSET;
    //default offset -25
    
    @Key
    private int mCoolingAirflow2TempUpperOffset = COOLING_AIRFLOW_TEMP_STAGE2_UPPER_OFFSET;
    //default offset -12
    
    @Key
    private int mHeatingAirflow2TempLowerOffset = HEATING_AIRFLOW_TEMP_STAGE2_LOWER_OFFSET;
    //default offset 35
    
    @Key
    private int mHeatingAirflow2TempUpperOffset = HEATING_AIRFLOW_TEMP_STAGE2_UPPER_OFFSET;
    //default offset 50
    
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
    private int mReheatMaxDamperPos = REHEAT_MAX_DAMPER_POS;
    
    @Key
    private int mRefHighLimit = REFRIGERATION_HIGHER_LIMIT;
    @Key
    private int mRefLowLimit  = REFRIGERATION_LOWER_LIMIT;
    
    @Key
    private int mAlarmVolume         = ALARM_VOLUME_DEFAULT;
    @Key
    private int mEnergyMeterSetpoint = ENERGY_METER_SETPOINT;
    
    @Key
    private short mMinLightingControlOverrideInMinutes = MIN_LIGHTING_CONTROL_OVERRIDE_IN_MINUTES;
    @Key
    private short mLightingIntensityForOccupancyDetect = LIGHTING_INTENSITY_OCCUPANT_DETECTED;
    
    @Key
    private int mCoolingDeadBand = SSE_COOLING_DEADBAND;
    
    @Key
    private int mHeatingDeadBand = SSE_HEATING_DEADBAND;
    
    private boolean bUploadedToKinvey = false;
    
    
    public String getId()
    {
        return id;
    }
    
    
    public void setId(String id)
    {
        this.id = id;
    }
    
    
    public String getmCCUName()
    {
        return mCCUName;
    }
    
    
    public void setmCCUName(String mCCUName)
    {
        this.mCCUName = mCCUName;
    }
    
    
    public int getmVersion()
    {
        return mVersion;
    }
    
    
    public void setmVersion(int mVersion)
    {
        this.mVersion = mVersion;
    }
    
    
    public double getmPropCtrlConstant()
    {
        return mPropCtrlConstant;
    }
    
    
    public void setmPropCtrlConstant(double mPropCtrlConstant)
    {
        this.mPropCtrlConstant = mPropCtrlConstant;
    }
    
    
    public double getmIntegCtrlConstant()
    {
        return mIntegCtrlConstant;
    }
    
    
    public void setmIntegCtrlConstant(double mIntegCtrlConstant)
    {
        this.mIntegCtrlConstant = mIntegCtrlConstant;
    }
    
    
    public double getmZoneLoadCtrlConstant()
    {
        return mZoneLoadCtrlConstant;
    }
    
    
    public void setmZoneLoadCtrlConstant(double mZoneLoadCtrlConstant)
    {
        this.mZoneLoadCtrlConstant = mZoneLoadCtrlConstant;
    }
    
    
    public double getmCoolingRateCtrlConstant()
    {
        return mCoolingRateCtrlConstant;
    }
    
    
    public void setmCoolingRateCtrlConstant(double mCoolingRateCtrlConstant)
    {
        this.mCoolingRateCtrlConstant = mCoolingRateCtrlConstant;
    }
    
    
    public int getmCumulativeDamperPosTarget()
    {
        return mCumulativeDamperPosTarget;
    }
    
    
    public void setmCumulativeDamperPosTarget(int mCumulativeDamperPosTarget)
    {
        this.mCumulativeDamperPosTarget = mCumulativeDamperPosTarget;
    }
    
    
    public int getmDamperPosForDeadZone()
    {
        return mDamperPosForDeadZone;
    }
    
    
    public void setmDamperPosForDeadZone(int mDamperPosForDeadZone)
    {
        this.mDamperPosForDeadZone = mDamperPosForDeadZone;
    }
    
    
    public int getmTimeToDeclareDeadDamper()
    {
        return mTimeToDeclareDeadDamper;
    }
    
    
    public void setmTimeToDeclareDeadDamper(int mTimeToDeclareDeadDamper)
    {
        this.mTimeToDeclareDeadDamper = mTimeToDeclareDeadDamper;
    }
    
    
    public int getmIntegCtrlTimeOut()
    {
        return mIntegCtrlTimeOut;
    }
    
    
    public void setmIntegCtrlTimeOut(int mIntegCtrlTimeOut)
    {
        this.mIntegCtrlTimeOut = mIntegCtrlTimeOut;
    }
    
    
    public double getmPropCtrlSpread()
    {
        return mPropCtrlSpread;
    }
    
    
    public void setmPropCtrlSpread(double mPropCtrlSpread)
    {
        this.mPropCtrlSpread = mPropCtrlSpread;
    }
    
    
    public double getmPreConditioningPropCtrlMultiplier()
    {
        return mPreConditioningPropCtrlMultiplier;
    }
    
    
    public void setmPreConditioningPropCtrlMultiplier(double mPreConditioningPropCtrlMultiplier)
    {
        this.mPreConditioningPropCtrlMultiplier = mPreConditioningPropCtrlMultiplier;
    }
    
    
    public int getmDXCoilHoldTimeMultiplier()
    {
        return mDXCoilHoldTimeMultiplier;
    }
    
    
    public void setmDXCoilHoldTimeMultiplier(int mDXCoilHoldTimeMultiplier)
    {
        this.mDXCoilHoldTimeMultiplier = mDXCoilHoldTimeMultiplier;
    }
    
    
    public int getmRebalanceHoldTime()
    {
        return mRebalanceHoldTime;
    }
    
    
    public void setmRebalanceHoldTime(int mRebalanceHoldTime)
    {
        this.mRebalanceHoldTime = mRebalanceHoldTime;
    }
    
    
    public int getmPercentDeadZonesAllowed()
    {
        return mPercentDeadZonesAllowed;
    }
    
    
    public void setmPercentDeadZonesAllowed(int mPercentDeadZonesAllowed)
    {
        this.mPercentDeadZonesAllowed = mPercentDeadZonesAllowed;
    }
    
    
    public double getmDXCoilOffsetLimitMultiplier()
    {
        return mDXCoilOffsetLimitMultiplier;
    }
    
    
    public void setmDXCoilOffsetLimitMultiplier(double mDXCoilOffsetLimitMultiplier)
    {
        this.mDXCoilOffsetLimitMultiplier = mDXCoilOffsetLimitMultiplier;
    }
    
    
    public double getmDXCoilOffsetHeatOffset()
    {
        return mDXCoilOffsetHeatOffset;
    }
    
    
    public void setmDXCoilOffsetHeatOffset(double mDXCoilOffsetHeatOffset)
    {
        this.mDXCoilOffsetHeatOffset = mDXCoilOffsetHeatOffset;
    }
    
    
    public double getmDXCoilOffsetCoolOffset()
    {
        return mDXCoilOffsetCoolOffset;
    }
    
    
    public void setmDXCoilOffsetCoolOffset(double mDXCoilOffsetCoolOffset)
    {
        this.mDXCoilOffsetCoolOffset = mDXCoilOffsetCoolOffset;
    }
    
    
    public int getmClockUpdateInterval()
    {
        return mClockUpdateInterval;
    }
    
    
    public void setmClockUpdateInterval(int mClockUpdateInterval)
    {
        this.mClockUpdateInterval = mClockUpdateInterval;
    }
    
    
    public int getmBuildingToZoneTempLimitsDifferential()
    {
        return mBuildingToZoneTempLimitsDifferential;
    }
    
    
    public void setmBuildingToZoneTempLimitsDifferential(int mBuildingToZoneTempLimitsDifferential)
    {
        this.mBuildingToZoneTempLimitsDifferential = mBuildingToZoneTempLimitsDifferential;
    }
    
    
    public double getmBackpressureLimit()
    {
        return mBackpressureLimit;
    }
    
    
    public void setmBackpressureLimit(double mBackpressureLimit)
    {
        this.mBackpressureLimit = mBackpressureLimit;
    }
    
    
    public int getmCumDamperPosIncrForBackpressure()
    {
        return mCumDamperPosIncrForBackpressure;
    }
    
    
    public void setmCumDamperPosIncrForBackpressure(int mCumDamperPosIncrForBackpressure)
    {
        this.mCumDamperPosIncrForBackpressure = mCumDamperPosIncrForBackpressure;
    }
    
    
    public int getmMultizoneStage2Timer()
    {
        return mMultizoneStage2Timer;
    }
    
    
    public void setmMultizoneStage2Timer(int mMultizoneStage2Timer)
    {
        this.mMultizoneStage2Timer = mMultizoneStage2Timer;
    }
    
    
    public int getmSinglezoneStage2Timer()
    {
        return mSinglezoneStage2Timer;
    }
    
    
    public void setmSinglezoneStage2Timer(int mSinglezoneStage2Timer)
    {
        this.mSinglezoneStage2Timer = mSinglezoneStage2Timer;
    }
    
    
    public int getmMultizoneStage2PercentDrop()
    {
        return mMultizoneStage2PercentDrop;
    }
    
    
    public void setmMultizoneStage2PercentDrop(int mMultizoneStage2PercentDrop)
    {
        this.mMultizoneStage2PercentDrop = mMultizoneStage2PercentDrop;
    }
    
    
    public double getmSinglezoneStage2Offset()
    {
        return mSinglezoneStage2Offset;
    }
    
    
    public void setmSinglezoneStage2Offset(double mSinglezoneStage2Offset)
    {
        this.mSinglezoneStage2Offset = mSinglezoneStage2Offset;
    }
    
    
    public int getmIgnoreCMReportedError()
    {
        return mIgnoreCMReportedError;
    }
    
    
    public void setmIgnoreCMReportedError(int mIgnoreCMReportedError)
    {
        this.mIgnoreCMReportedError = mIgnoreCMReportedError;
    }
    
    
    public int getmIgnoreFSVNotInDBError()
    {
        return mIgnoreFSVNotInDBError;
    }
    
    
    public void setmIgnoreFSVNotInDBError(int mIgnoreFSVNotInDBError)
    {
        this.mIgnoreFSVNotInDBError = mIgnoreFSVNotInDBError;
    }
    
    
    public int getmHeatingAirflowTemperature()
    {
        return mHeatingAirflowTemperature;
    }
    
    
    public void setmHeatingAirflowTemperature(int mHeatingAirflowTemperature)
    {
        this.mHeatingAirflowTemperature = mHeatingAirflowTemperature;
    }
    
    
    public int getmCoolingAirflowTemperature()
    {
        return mCoolingAirflowTemperature;
    }
    
    
    public void setmCoolingAirflowTemperature(int mCoolingAirflowTemperature)
    {
        this.mCoolingAirflowTemperature = mCoolingAirflowTemperature;
    }
    
    
    public int getmAirflowTemperatureBreachAlertHoldDown()
    {
        return mAirflowTemperatureBreachAlertHoldDown;
    }
    
    
    public void setmAirflowTemperatureBreachAlertHoldDown(int mAirflowTemperatureBreachAlertHoldDown)
    {
        this.mAirflowTemperatureBreachAlertHoldDown = mAirflowTemperatureBreachAlertHoldDown;
    }
    
    
    public int getmHeartBeatInterval()
    {
        return mHeartBeatInterval;
    }
    
    
    public void setmHeartBeatInterval(int mHeartBeatInterval)
    {
        this.mHeartBeatInterval = mHeartBeatInterval;
    }
    
    
    public int getmHeartBeatsToSkip()
    {
        return mHeartBeatsToSkip;
    }
    
    
    public void setmHeartBeatsToSkip(int mHeartBeatsToSkip)
    {
        this.mHeartBeatsToSkip = mHeartBeatsToSkip;
    }
    
    
    public double getmAutoModeTemperatureMultiplier()
    {
        return mAutoModeTemperatureMultiplier;
    }
    
    
    public void setmAutoModeTemperatureMultiplier(double mAutoModeTemperatureMultiplier)
    {
        this.mAutoModeTemperatureMultiplier = mAutoModeTemperatureMultiplier;
    }
    
    
    public int getmAutoAwayTimePeriod()
    {
        return mAutoAwayTimePeriod;
    }
    
    
    public void setmAutoAwayTimePeriod(int mAutoAwayTimePeriod)
    {
        this.mAutoAwayTimePeriod = mAutoAwayTimePeriod;
    }
    
    
    public int getmShowRPMAlerts()
    {
        return mShowRPMAlerts;
    }
    
    
    public void setmShowRPMAlerts(int mShowRPMAlerts)
    {
        this.mShowRPMAlerts = mShowRPMAlerts;
    }
    
    
    public int getmForcedOccupiedZonePriority()
    {
        return mForcedOccupiedZonePriority;
    }
    
    
    public void setmForcedOccupiedZonePriority(int mForcedOccupiedZonePriority)
    {
        this.mForcedOccupiedZonePriority = mForcedOccupiedZonePriority;
    }
    
    
    public int getmForcedOccupiedTimePeriod()
    {
        return mForcedOccupiedTimePeriod;
    }
    
    
    public void setmForcedOccupiedTimePeriod(int mForcedOccupiedTimePeriod)
    {
        this.mForcedOccupiedTimePeriod = mForcedOccupiedTimePeriod;
    }
    
    
    public int getmHumidityThreshold()
    {
        return mHumidityThreshold;
    }
    
    
    public void setmHumidityThreshold(int mHumidityThreshold)
    {
        this.mHumidityThreshold = mHumidityThreshold;
    }
    
    
    public int getmHumidityPerDegreeFactor()
    {
        return mHumidityPerDegreeFactor;
    }
    
    
    public void setmHumidityPerDegreeFactor(int mHumidityPerDegreeFactor)
    {
        this.mHumidityPerDegreeFactor = mHumidityPerDegreeFactor;
    }
    
    
    public boolean ismUseSameOccuTempAcrossDays()
    {
        return mUseSameOccuTempAcrossDays;
    }
    
    
    public void setmUseSameOccuTempAcrossDays(boolean mUseSameOccuTempAcrossDays)
    {
        this.mUseSameOccuTempAcrossDays = mUseSameOccuTempAcrossDays;
    }
    
    
    public int getmDCVCO2ThresholdLevel()
    {
        return mDCVCO2ThresholdLevel;
    }
    
    
    public void setmDCVCO2ThresholdLevel(int mDCVCO2ThresholdLevel)
    {
        this.mDCVCO2ThresholdLevel = mDCVCO2ThresholdLevel;
    }
    
    
    public int getmDCVDamperOpeningRate()
    {
        return mDCVDamperOpeningRate;
    }
    
    
    public void setmDCVDamperOpeningRate(int mDCVDamperOpeningRate)
    {
        this.mDCVDamperOpeningRate = mDCVDamperOpeningRate;
    }
    
    
    public int getmDumbModeDCVDamperOpening()
    {
        return mDumbModeDCVDamperOpening;
    }
    
    
    public void setmDumbModeDCVDamperOpening(int mDumbModeDCVDamperOpening)
    {
        this.mDumbModeDCVDamperOpening = mDumbModeDCVDamperOpening;
    }
    
    
    public int getmZonePrioritySpread()
    {
        return mZonePrioritySpread;
    }
    
    
    public void setmZonePrioritySpread(int mZonePrioritySpread)
    {
        this.mZonePrioritySpread = mZonePrioritySpread;
    }
    
    
    public double getmZonePriorityMultiplier()
    {
        return mZonePriorityMultiplier;
    }
    
    
    public void setmZonePriorityMultiplier(double mZonePriorityMultiplier)
    {
        this.mZonePriorityMultiplier = mZonePriorityMultiplier;
    }
    
    
    public int getmAbnormalCurTempChangeAlertTrigger()
    {
        return mAbnormalCurTempChangeAlertTrigger;
    }
    
    
    public void setmAbnormalCurTempChangeAlertTrigger(int mAbnormalCurTempChangeAlertTrigger)
    {
        this.mAbnormalCurTempChangeAlertTrigger = mAbnormalCurTempChangeAlertTrigger;
    }
    
    
    public int getmUserLimitSpread()
    {
        return mUserLimitSpread;
    }
    
    
    public void setmUserLimitSpread(int mUserLimitSpread)
    {
        this.mUserLimitSpread = mUserLimitSpread;
    }
    
    
    public int getmPreconditioningRate()
    {
        return mPreconditioningRate;
    }
    
    
    public void setmPreconditioningRate(int mPreconditioningRate)
    {
        this.mPreconditioningRate = mPreconditioningRate;
    }
    
    
    public double getmSetbackMultiplier()
    {
        return mSetbackMultiplier;
    }
    
    
    public void setmSetbackMultiplier(double mSetbackMultiplier)
    {
        this.mSetbackMultiplier = mSetbackMultiplier;
    }
    
    
    public int getmEnthalpyCompensation()
    {
        return mEnthalpyCompensation;
    }
    
    
    public void setmEnthalpyCompensation(int mEnthalpyCompensation)
    {
        this.mEnthalpyCompensation = mEnthalpyCompensation;
    }
    
    
    public int getmOutsideAirMinTemp()
    {
        return mOutsideAirMinTemp;
    }
    
    
    public void setmOutsideAirMinTemp(int mOutsideAirMinTemp)
    {
        this.mOutsideAirMinTemp = mOutsideAirMinTemp;
    }
    
    
    public int getmOutsideAirMaxTemp()
    {
        return mOutsideAirMaxTemp;
    }
    
    
    public void setmOutsideAirMaxTemp(int mOutsideAirMaxTemp)
    {
        this.mOutsideAirMaxTemp = mOutsideAirMaxTemp;
    }
    
    
    public int getmOutsideAirMinHumidity()
    {
        return mOutsideAirMinHumidity;
    }
    
    
    public void setmOutsideAirMinHumidity(int mOutsideAirMinHumidity)
    {
        this.mOutsideAirMinHumidity = mOutsideAirMinHumidity;
    }
    
    
    public int getmOutsideAirMaxHumidity()
    {
        return mOutsideAirMaxHumidity;
    }
    
    
    public void setmOutsideAirMaxHumidity(int mOutsideAirMaxHumidity)
    {
        this.mOutsideAirMaxHumidity = mOutsideAirMaxHumidity;
    }
    
    
    public int getmEconomizerLoadThreshold()
    {
        return mEconomizerLoadThreshold;
    }
    
    
    public void setmEconomizerLoadThreshold(int mEconomizerLoadThreshold)
    {
        this.mEconomizerLoadThreshold = mEconomizerLoadThreshold;
    }
    
    
    public int getmEconomizerHoldTime()
    {
        return mEconomizerHoldTime;
    }
    
    
    public void setmEconomizerHoldTime(int mEconomizerHoldTime)
    {
        this.mEconomizerHoldTime = mEconomizerHoldTime;
    }
    
    
    public int getmEconomizerStage1LoadDrop()
    {
        return mEconomizerStage1LoadDrop;
    }
    
    
    public void setmEconomizerStage1LoadDrop(int mEconomizerStage1LoadDrop)
    {
        this.mEconomizerStage1LoadDrop = mEconomizerStage1LoadDrop;
    }
    
    
    public int getmEconIntegCtrlTimeOut()
    {
        return mEconIntegCtrlTimeOut;
    }
    
    
    public void setmEconIntegCtrlTimeOut(int mEconIntegCtrlTimeOut)
    {
        this.mEconIntegCtrlTimeOut = mEconIntegCtrlTimeOut;
    }
    
    
    public double getmEconPropCtrlSpread()
    {
        return mEconPropCtrlSpread;
    }
    
    
    public void setmEconPropCtrlSpread(double mEconPropCtrlSpread)
    {
        this.mEconPropCtrlSpread = mEconPropCtrlSpread;
    }
    
    
    public double getmAnalog1PropCtrlConstant()
    {
        return mAnalog1PropCtrlConstant;
    }
    
    
    public void setmAnalog1PropCtrlConstant(double mAnalog1PropCtrlConstant)
    {
        this.mAnalog1PropCtrlConstant = mAnalog1PropCtrlConstant;
    }
    
    
    public double getmAnalog1IntegCtrlConstant()
    {
        return mAnalog1IntegCtrlConstant;
    }
    
    
    public void setmAnalog1IntegCtrlConstant(double mAnalog1IntegCtrlConstant)
    {
        this.mAnalog1IntegCtrlConstant = mAnalog1IntegCtrlConstant;
    }
    
    
    public int getmAnalog1IntegCtrlTimeOut()
    {
        return mAnalog1IntegCtrlTimeOut;
    }
    
    
    public void setmAnalog1IntegCtrlTimeOut(int mAnalog1IntegCtrlTimeOut)
    {
        this.mAnalog1IntegCtrlTimeOut = mAnalog1IntegCtrlTimeOut;
    }
    
    
    public double getmAnalog1PropCtrlSpread()
    {
        return mAnalog1PropCtrlSpread;
    }
    
    
    public void setmAnalog1PropCtrlSpread(double mAnalog1PropCtrlSpread)
    {
        this.mAnalog1PropCtrlSpread = mAnalog1PropCtrlSpread;
    }
    
    
    public double getmAnalog3PropCtrlConstant()
    {
        return mAnalog3PropCtrlConstant;
    }
    
    
    public void setmAnalog3PropCtrlConstant(double mAnalog3PropCtrlConstant)
    {
        this.mAnalog3PropCtrlConstant = mAnalog3PropCtrlConstant;
    }
    
    
    public double getmAnalog3IntegCtrlConstant()
    {
        return mAnalog3IntegCtrlConstant;
    }
    
    
    public void setmAnalog3IntegCtrlConstant(double mAnalog3IntegCtrlConstant)
    {
        this.mAnalog3IntegCtrlConstant = mAnalog3IntegCtrlConstant;
    }
    
    
    public int getmAnalog3IntegCtrlTimeOut()
    {
        return mAnalog3IntegCtrlTimeOut;
    }
    
    
    public void setmAnalog3IntegCtrlTimeOut(int mAnalog3IntegCtrlTimeOut)
    {
        this.mAnalog3IntegCtrlTimeOut = mAnalog3IntegCtrlTimeOut;
    }
    
    
    public double getmAnalog3PropCtrlSpread()
    {
        return mAnalog3PropCtrlSpread;
    }
    
    
    public void setmAnalog3PropCtrlSpread(double mAnalog3PropCtrlSpread)
    {
        this.mAnalog3PropCtrlSpread = mAnalog3PropCtrlSpread;
    }
    
    
    public double getmAnalogFanSpeedMultiplier()
    {
        return mAnalogFanSpeedMultiplier;
    }
    
    
    public void setmAnalogFanSpeedMultiplier(double mAnalogFanSpeedMultiplier)
    {
        this.mAnalogFanSpeedMultiplier = mAnalogFanSpeedMultiplier;
    }
    
    
    public int getmAnalogMinHeating()
    {
        return mAnalogMinHeating;
    }
    
    
    public void setmAnalogMinHeating(int mAnalogMinHeating)
    {
        this.mAnalogMinHeating = mAnalogMinHeating;
    }
    
    
    public int getmAnalogMaxHeating()
    {
        return mAnalogMaxHeating;
    }
    
    
    public void setmAnalogMaxHeating(int mAnalogMaxHeating)
    {
        this.mAnalogMaxHeating = mAnalogMaxHeating;
    }
    
    
    public int getmAnalogMinCooling()
    {
        return mAnalogMinCooling;
    }
    
    
    public void setmAnalogMinCooling(int mAnalogMinCooling)
    {
        this.mAnalogMinCooling = mAnalogMinCooling;
    }
    
    
    public int getmAnalogMaxCooling()
    {
        return mAnalogMaxCooling;
    }
    
    
    public void setmAnalogMaxCooling(int mAnalogMaxCooling)
    {
        this.mAnalogMaxCooling = mAnalogMaxCooling;
    }
    
    
    public double getmAutoModeCoolHeatDXCILimit()
    {
        return mAutoModeCoolHeatDXCILimit;
    }
    
    
    public void setmAutoModeCoolHeatDXCILimit(double mAutoModeCoolHeatDXCILimit)
    {
        this.mAutoModeCoolHeatDXCILimit = mAutoModeCoolHeatDXCILimit;
    }
    
    
    public double getmAutoModeTotalDXCILimit()
    {
        return mAutoModeTotalDXCILimit;
    }
    
    
    public void setmAutoModeTotalDXCILimit(double mAutoModeTotalDXCILimit)
    {
        this.mAutoModeTotalDXCILimit = mAutoModeTotalDXCILimit;
    }
    
    
    public boolean ismUseCoolHeatDXCIForAutoMode()
    {
        return mUseCoolHeatDXCIForAutoMode;
    }
    
    
    public void setmUseCoolHeatDXCIForAutoMode(boolean mUseCoolHeatDXCIForAutoMode)
    {
        this.mUseCoolHeatDXCIForAutoMode = mUseCoolHeatDXCIForAutoMode;
    }
    
    
    public boolean isUseCelsius()
    {
        return useCelsius;
    }
    
    
    public void setUseCelsius(boolean useCelsius)
    {
        this.useCelsius = useCelsius;
    }
    
    
    public boolean isUseMilitaryTime()
    {
        return useMilitaryTime;
    }
    
    
    public void setUseMilitaryTime(boolean useMilitaryTime)
    {
        this.useMilitaryTime = useMilitaryTime;
    }
    
    
    public boolean isUseInstantGratificationMode()
    {
        return useInstantGratificationMode;
    }
    
    
    public void setUseInstantGratificationMode(boolean useInstantGratificationMode)
    {
        this.useInstantGratificationMode = useInstantGratificationMode;
    }
    
    
    public boolean isFollowAutoModeSchedule()
    {
        return followAutoModeSchedule;
    }
    
    
    public void setFollowAutoModeSchedule(boolean followAutoModeSchedule)
    {
        this.followAutoModeSchedule = followAutoModeSchedule;
    }
    
    
    public boolean isUseOutsideTemperatureLockout()
    {
        return useOutsideTemperatureLockout;
    }
    
    
    public void setUseOutsideTemperatureLockout(boolean useOutsideTemperatureLockout)
    {
        this.useOutsideTemperatureLockout = useOutsideTemperatureLockout;
    }
    
    
    public int getFSVPairingStartAddress()
    {
        return FSVPairingStartAddress;
    }
    
    
    public void setFSVPairingStartAddress(int FSVPairingStartAddress)
    {
        this.FSVPairingStartAddress = FSVPairingStartAddress;
    }
    
    
    public int getOutsideCoolingTempLockout()
    {
        return outsideCoolingTempLockout;
    }
    
    
    public void setOutsideCoolingTempLockout(int outsideCoolingTempLockout)
    {
        this.outsideCoolingTempLockout = outsideCoolingTempLockout;
    }
    
    
    public int getOutsideHeatingTempLockout()
    {
        return outsideHeatingTempLockout;
    }
    
    
    public void setOutsideHeatingTempLockout(int outsideHeatingTempLockout)
    {
        this.outsideHeatingTempLockout = outsideHeatingTempLockout;
    }
    
    
    public int getmStage1FanOnTime()
    {
        return mStage1FanOnTime;
    }
    
    
    public void setmStage1FanOnTime(int mStage1FanOnTime)
    {
        this.mStage1FanOnTime = mStage1FanOnTime;
    }
    
    
    public int getmStage1FanOffTime()
    {
        return mStage1FanOffTime;
    }
    
    
    public void setmStage1FanOffTime(int mStage1FanOffTime)
    {
        this.mStage1FanOffTime = mStage1FanOffTime;
    }
    
    
    public int getmBuildingAllowNoHotter()
    {
        return mBuildingAllowNoHotter;
    }
    
    
    public void setmBuildingAllowNoHotter(int mBuildingAllowNoHotter)
    {
        this.mBuildingAllowNoHotter = mBuildingAllowNoHotter;
    }
    
    
    public int getmBuildingAllowNoCooler()
    {
        return mBuildingAllowNoCooler;
    }
    
    
    public void setmBuildingAllowNoCooler(int mBuildingAllowNoCooler)
    {
        this.mBuildingAllowNoCooler = mBuildingAllowNoCooler;
    }
    
    
    public int getmUserAllowNoHotter()
    {
        return mUserAllowNoHotter;
    }
    
    
    public void setmUserAllowNoHotter(int mUserAllowNoHotter)
    {
        this.mUserAllowNoHotter = mUserAllowNoHotter;
    }
    
    
    public int getmUserAllowNoCooler()
    {
        return mUserAllowNoCooler;
    }
    
    
    public void setmUserAllowNoCooler(int mUserAllowNoCooler)
    {
        this.mUserAllowNoCooler = mUserAllowNoCooler;
    }
    
    
    public int getmWRMForHumidityValue()
    {
        return mWRMForHumidityValue;
    }
    
    
    public void setmWRMForHumidityValue(int mWRMForHumidityValue)
    {
        this.mWRMForHumidityValue = mWRMForHumidityValue;
    }
    
    
    public boolean ismUseExtHumiditySensor()
    {
        return mUseExtHumiditySensor;
    }
    
    
    public void setmUseExtHumiditySensor(boolean mUseExtHumiditySensor)
    {
        this.mUseExtHumiditySensor = mUseExtHumiditySensor;
    }
    
    
    public int getmExtHumiditySensorMinSP()
    {
        return mExtHumiditySensorMinSP;
    }
    
    
    public void setmExtHumiditySensorMinSP(int mExtHumiditySensorMinSP)
    {
        this.mExtHumiditySensorMinSP = mExtHumiditySensorMinSP;
    }
    
    
    public int getmExtHumiditySensorMaxSP()
    {
        return mExtHumiditySensorMaxSP;
    }
    
    
    public void setmExtHumiditySensorMaxSP(int mExtHumiditySensorMaxSP)
    {
        this.mExtHumiditySensorMaxSP = mExtHumiditySensorMaxSP;
    }
    
    
    public int getmExtHumiditySPTimeoutInterval()
    {
        return mExtHumiditySPTimeoutInterval;
    }
    
    
    public void setmExtHumiditySPTimeoutInterval(int mExtHumiditySPTimeoutInterval)
    {
        this.mExtHumiditySPTimeoutInterval = mExtHumiditySPTimeoutInterval;
    }
    
    
    public int getmWRMForDumbModeTempValue()
    {
        return mWRMForDumbModeTempValue;
    }
    
    
    public void setmWRMForDumbModeTempValue(int mWRMForDumbModeTempValue)
    {
        this.mWRMForDumbModeTempValue = mWRMForDumbModeTempValue;
    }
    
    
    public int getmNO2ThresholdLevel()
    {
        return mNO2ThresholdLevel;
    }
    
    
    public void setmNO2ThresholdLevel(int mNO2ThresholdLevel)
    {
        this.mNO2ThresholdLevel = mNO2ThresholdLevel;
    }
    
    
    public int getmCOThresholdLevel()
    {
        return mCOThresholdLevel;
    }
    
    
    public void setmCOThresholdLevel(int mCOThresholdLevel)
    {
        this.mCOThresholdLevel = mCOThresholdLevel;
    }
    
    
    public int getmNO2DamperOpeningRate()
    {
        return mNO2DamperOpeningRate;
    }
    
    
    public void setmNO2DamperOpeningRate(int mNO2DamperOpeningRate)
    {
        this.mNO2DamperOpeningRate = mNO2DamperOpeningRate;
    }
    
    
    public int getmCODamperOpeningRate()
    {
        return mCODamperOpeningRate;
    }
    
    
    public void setmCODamperOpeningRate(int mCODamperOpeningRate)
    {
        this.mCODamperOpeningRate = mCODamperOpeningRate;
    }
    
    
    public int getmOAODamperPosMin()
    {
        return mOAODamperPosMin;
    }
    
    
    public void setmOAODamperPosMin(int mOAODamperPosMin)
    {
        this.mOAODamperPosMin = mOAODamperPosMin;
    }
    
    
    public int getmOAODamperPosMax()
    {
        return mOAODamperPosMax;
    }
    
    
    public void setmOAODamperPosMax(int mOAODamperPosMax)
    {
        this.mOAODamperPosMax = mOAODamperPosMax;
    }
    
    
    public int getmOAOExhaustFanThreshold()
    {
        return mOAOExhaustFanThreshold;
    }
    
    
    public void setmOAOExhaustFanThreshold(int mOAOExhaustFanThreshold)
    {
        this.mOAOExhaustFanThreshold = mOAOExhaustFanThreshold;
    }
    
    
    public double getmPressureThresholdLevel()
    {
        return mPressureThresholdLevel;
    }
    
    
    public void setmPressureThresholdLevel(double mPressureThresholdLevel)
    {
        this.mPressureThresholdLevel = mPressureThresholdLevel;
    }
    
    
    public double getmPressureDeadZone()
    {
        return mPressureDeadZone;
    }
    
    
    public void setmPressureDeadZone(double mPressureDeadZone)
    {
        this.mPressureDeadZone = mPressureDeadZone;
    }
    
    
    public int getmPressureDamperOpeningRate()
    {
        return mPressureDamperOpeningRate;
    }
    
    
    public void setmPressureDamperOpeningRate(int mPressureDamperOpeningRate)
    {
        this.mPressureDamperOpeningRate = mPressureDamperOpeningRate;
    }
    
    
    public int getmAnalog1MinValue()
    {
        return mAnalog1MinValue;
    }
    
    
    public void setmAnalog1MinValue(int mAnalog1MinValue)
    {
        this.mAnalog1MinValue = mAnalog1MinValue;
    }
    
    
    public int getmAnalog1MaxValue()
    {
        return mAnalog1MaxValue;
    }
    
    
    public void setmAnalog1MaxValue(int mAnalog1MaxValue)
    {
        this.mAnalog1MaxValue = mAnalog1MaxValue;
    }
    
    
    public int getmAnalog3MinValue()
    {
        return mAnalog3MinValue;
    }
    
    
    public void setmAnalog3MinValue(int mAnalog3MinValue)
    {
        this.mAnalog3MinValue = mAnalog3MinValue;
    }
    
    
    public int getmAnalog3MaxValue()
    {
        return mAnalog3MaxValue;
    }
    
    
    public void setmAnalog3MaxValue(int mAnalog3MaxValue)
    {
        this.mAnalog3MaxValue = mAnalog3MaxValue;
    }
    
    
    public int getmCoolingAirflowTempLowerOffset()
    {
        return mCoolingAirflowTempLowerOffset;
    }
    
    
    public void setmCoolingAirflowTempLowerOffset(int mCoolingAirflowTempLowerOffset)
    {
        this.mCoolingAirflowTempLowerOffset = mCoolingAirflowTempLowerOffset;
    }
    
    
    public int getmCoolingAirflowTempUpperOffset()
    {
        return mCoolingAirflowTempUpperOffset;
    }
    
    
    public void setmCoolingAirflowTempUpperOffset(int mCoolingAirflowTempUpperOffset)
    {
        this.mCoolingAirflowTempUpperOffset = mCoolingAirflowTempUpperOffset;
    }
    
    
    public int getmHeatingAirflowTempLowerOffset()
    {
        return mHeatingAirflowTempLowerOffset;
    }
    
    
    public void setmHeatingAirflowTempLowerOffset(int mHeatingAirflowTempLowerOffset)
    {
        this.mHeatingAirflowTempLowerOffset = mHeatingAirflowTempLowerOffset;
    }
    
    
    public int getmHeatingAirflowTempUpperOffset()
    {
        return mHeatingAirflowTempUpperOffset;
    }
    
    
    public void setmHeatingAirflowTempUpperOffset(int mHeatingAirflowTempUpperOffset)
    {
        this.mHeatingAirflowTempUpperOffset = mHeatingAirflowTempUpperOffset;
    }
    
    
    public int getmCoolingAirflow2TempLowerOffset()
    {
        return mCoolingAirflow2TempLowerOffset;
    }
    
    
    public void setmCoolingAirflow2TempLowerOffset(int mCoolingAirflow2TempLowerOffset)
    {
        this.mCoolingAirflow2TempLowerOffset = mCoolingAirflow2TempLowerOffset;
    }
    
    
    public int getmCoolingAirflow2TempUpperOffset()
    {
        return mCoolingAirflow2TempUpperOffset;
    }
    
    
    public void setmCoolingAirflow2TempUpperOffset(int mCoolingAirflow2TempUpperOffset)
    {
        this.mCoolingAirflow2TempUpperOffset = mCoolingAirflow2TempUpperOffset;
    }
    
    
    public int getmHeatingAirflow2TempLowerOffset()
    {
        return mHeatingAirflow2TempLowerOffset;
    }
    
    
    public void setmHeatingAirflow2TempLowerOffset(int mHeatingAirflow2TempLowerOffset)
    {
        this.mHeatingAirflow2TempLowerOffset = mHeatingAirflow2TempLowerOffset;
    }
    
    
    public int getmHeatingAirflow2TempUpperOffset()
    {
        return mHeatingAirflow2TempUpperOffset;
    }
    
    
    public void setmHeatingAirflow2TempUpperOffset(int mHeatingAirflow2TempUpperOffset)
    {
        this.mHeatingAirflow2TempUpperOffset = mHeatingAirflow2TempUpperOffset;
    }
    
    
    public int getmChilledWaterDeltaTSetpoint()
    {
        return mChilledWaterDeltaTSetpoint;
    }
    
    
    public void setmChilledWaterDeltaTSetpoint(int mChilledWaterDeltaTSetpoint)
    {
        this.mChilledWaterDeltaTSetpoint = mChilledWaterDeltaTSetpoint;
    }
    
    
    public int getmChilledWaterFlowSetpoint()
    {
        return mChilledWaterFlowSetpoint;
    }
    
    
    public void setmChilledWaterFlowSetpoint(int mChilledWaterFlowSetpoint)
    {
        this.mChilledWaterFlowSetpoint = mChilledWaterFlowSetpoint;
    }
    
    
    public int getmChilledWaterMaxFlowRate()
    {
        return mChilledWaterMaxFlowRate;
    }
    
    
    public void setmChilledWaterMaxFlowRate(int mChilledWaterMaxFlowRate)
    {
        this.mChilledWaterMaxFlowRate = mChilledWaterMaxFlowRate;
    }
    
    
    public int getmChilledWaterActuatorMinPos()
    {
        return mChilledWaterActuatorMinPos;
    }
    
    
    public void setmChilledWaterActuatorMinPos(int mChilledWaterActuatorMinPos)
    {
        this.mChilledWaterActuatorMinPos = mChilledWaterActuatorMinPos;
    }
    
    
    public int getmChilledWaterActuatorMaxPos()
    {
        return mChilledWaterActuatorMaxPos;
    }
    
    
    public void setmChilledWaterActuatorMaxPos(int mChilledWaterActuatorMaxPos)
    {
        this.mChilledWaterActuatorMaxPos = mChilledWaterActuatorMaxPos;
    }
    
    
    public int getmAHUFanMinLimit()
    {
        return mAHUFanMinLimit;
    }
    
    
    public void setmAHUFanMinLimit(int mAHUFanMinLimit)
    {
        this.mAHUFanMinLimit = mAHUFanMinLimit;
    }
    
    
    public int getmAHUFanMaxLimit()
    {
        return mAHUFanMaxLimit;
    }
    
    
    public void setmAHUFanMaxLimit(int mAHUFanMaxLimit)
    {
        this.mAHUFanMaxLimit = mAHUFanMaxLimit;
    }
    
    
    public boolean ismSmartNodeInstall()
    {
        return mSmartNodeInstall;
    }
    
    
    public void setmSmartNodeInstall(boolean mSmartNodeInstall)
    {
        this.mSmartNodeInstall = mSmartNodeInstall;
    }
    
    
    public boolean isEnergyMeterMonitor()
    {
        return energyMeterMonitor;
    }
    
    
    public void setEnergyMeterMonitor(boolean energyMeterMonitor)
    {
        this.energyMeterMonitor = energyMeterMonitor;
    }
    
    
    public double getmReheatOffset()
    {
        return mReheatOffset;
    }
    
    
    public void setmReheatOffset(double mReheatOffset)
    {
        this.mReheatOffset = mReheatOffset;
    }
    
    
    public double getmReheatDamperReopenOffset()
    {
        return mReheatDamperReopenOffset;
    }
    
    
    public void setmReheatDamperReopenOffset(double mReheatDamperReopenOffset)
    {
        this.mReheatDamperReopenOffset = mReheatDamperReopenOffset;
    }
    
    
    public int getmReheatMaxDamperPos()
    {
        return mReheatMaxDamperPos;
    }
    
    
    public void setmReheatMaxDamperPos(int mReheatMaxDamperPos)
    {
        this.mReheatMaxDamperPos = mReheatMaxDamperPos;
    }
    
    
    public int getmRefHighLimit()
    {
        return mRefHighLimit;
    }
    
    
    public void setmRefHighLimit(int mRefHighLimit)
    {
        this.mRefHighLimit = mRefHighLimit;
    }
    
    
    public int getmRefLowLimit()
    {
        return mRefLowLimit;
    }
    
    
    public void setmRefLowLimit(int mRefLowLimit)
    {
        this.mRefLowLimit = mRefLowLimit;
    }
    
    
    public int getmAlarmVolume()
    {
        return mAlarmVolume;
    }
    
    
    public void setmAlarmVolume(int mAlarmVolume)
    {
        this.mAlarmVolume = mAlarmVolume;
    }
    
    
    public int getmEnergyMeterSetpoint()
    {
        return mEnergyMeterSetpoint;
    }
    
    
    public void setmEnergyMeterSetpoint(int mEnergyMeterSetpoint)
    {
        this.mEnergyMeterSetpoint = mEnergyMeterSetpoint;
    }
    
    
    public HashMap<String, Object> getHashMap()
    {
        HashMap<String, Object> algoHashMap = new HashMap<String, Object>();
        algoHashMap
                .put("lightingIntensityOccupantDetected", getmLightingIntensityForOccupancyDetect());
        algoHashMap.put("minLightControlOverInMinutes", getmMinLightingControlOverrideInMinutes());
        algoHashMap.put("sseCoolingDeadBand", SSE_COOLING_DEADBAND);
        algoHashMap.put("sseHeatingDeadBand", SSE_HEATING_DEADBAND);
        algoHashMap.put("buildingMaxTemp", mBuildingAllowNoHotter);
        algoHashMap.put("buildingMinTemp", mBuildingAllowNoCooler);
        algoHashMap.put("userMaxTemp", mUserAllowNoHotter);
        algoHashMap.put("userMinTemp", mUserAllowNoCooler);
        algoHashMap.put("zoneSetBack", SSE_ZONE_SETBACK);
        algoHashMap.put("forcedOccupiedTime", FORCED_OCCUPIED_TIME);
        return algoHashMap;
    }
    
    
    public short getmLightingIntensityForOccupancyDetect()
    {
        return mLightingIntensityForOccupancyDetect;
    }
    
    
    public short getmMinLightingControlOverrideInMinutes()
    {
        return mMinLightingControlOverrideInMinutes;
    }
    
    
    public void setmMinLightingControlOverrideInMinutes(short mMinLightingControlOverrideInMinutes)
    {
        this.mMinLightingControlOverrideInMinutes = mMinLightingControlOverrideInMinutes;
    }
    
    
    public void setmLightingIntensityForOccupancyDetect(short mLightingIntensityForOccupancyDetect)
    {
        this.mLightingIntensityForOccupancyDetect = mLightingIntensityForOccupancyDetect;
    }
    
    
    public int getmCoolingDeadBand()
    {
        return mCoolingDeadBand;
    }
    
    
    public void setmCoolingDeadBand(int mCoolingDeadBand)
    {
        this.mCoolingDeadBand = mCoolingDeadBand;
    }
    
    
    public int getmHeatingDeadBand()
    {
        return mHeatingDeadBand;
    }
    
    
    public void setmHeatingDeadBand(int mHeatingDeadBand)
    {
        this.mHeatingDeadBand = mHeatingDeadBand;
    }
    
    
    public boolean isbUploadedToKinvey()
    {
        return bUploadedToKinvey;
    }
    
    
    public void setbUploadedToKinvey(boolean bUploadedToKinvey)
    {
        this.bUploadedToKinvey = bUploadedToKinvey;
    }
}