package a75f.io.logic.tuners;

/**
 * Created by samjithsadasivan on 10/5/18.
 */

public class TunerConstants
{
    
    public static final int DEFAULT_VAL_LEVEL = 17;
    public static final int TUNER_EQUIP_VAL_LEVEL = 8;
    public static final int TUNER_ZONE_VAL_LEVEL = 10;
    public static final int TUNER_SYSTEM_VAL_LEVEL = 14;
    public static final int TUNER_BUILDING_VAL_LEVEL = 16;
    
    public static final double SYSTEM_ANALOG1_MIN = 0;
    public static final double SYSTEM_ANALOG1_MAX = 10;
    public static final double SYSTEM_ANALOG2_MIN = 0;
    public static final double SYSTEM_ANALOG2_MAX = 10;
    public static final double SYSTEM_ANALOG3_MIN = 0;
    public static final double SYSTEM_ANALOG3_MAX = 10;
    public static final double SYSTEM_ANALOG4_MIN = 0;
    public static final double SYSTEM_ANALOG4_MAX = 10;
    public static final double SYSTEM_DEFAULT_CI = 2;
    public static final double SYSTEM_COOLING_SAT_MIN = 55;
    public static final double SYSTEM_COOLING_SAT_MAX = 65;
    public static final double SYSTEM_HEATING_SAT_MIN = 75;
    public static final double SYSTEM_HEATING_SAT_MAX = 100;
    public static final double SYSTEM_CO2TARGET_MIN = 800;
    public static final double SYSTEM_CO2TARGET_MAX = 1000;
    public static final double SYSTEM_SPTARGET_MIN = 0.5;
    public static final double SYSTEM_SPTARGET_MAX = 1.5;
    
    public static final int SYSTEM_DEFAULT_VAL_LEVEL = 17;
    public static final int UI_DEFAULT_VAL_LEVEL = 8;
    public static final int SYSTEM_BUILDING_VAL_LEVEL = 16;
    
    public static final double VAV_COOLING_DB = 2.0; //Default deadband value based on dual temp diff 70 and 74 ((74-70)/2.0)
    public static final double VAV_HEATING_DB = 2.0;
    public static final double VAV_COOLING_DB_MULTPLIER = 0.5;
    public static final double VAV_HEATING_DB_MULTIPLIER = 0.5;
    public static final double VAV_PROPORTIONAL_GAIN = 0.5;
    public static final double VAV_INTEGRAL_GAIN = 0.5;
    public static final double VAV_PROPORTIONAL_SPREAD = 2;
    public static final double VAV_INTEGRAL_TIMEOUT = 30;
    public static final double TARGET_CUMULATIVE_DAMPER = 70;
    public static final double TARGET_MAX_INSIDE_HUMIDITY = 45;
    public static final double TARGET_MIN_INSIDE_HUMIDITY = 25;
    public static final double ANALOG_FANSPEED_MULTIPLIER = 1;
    public static final double HUMIDITY_HYSTERESIS_PERCENT = 5;
    public static final double VALVE_START_DAMPER = 50;
    public static final double RELAY_DEACTIVATION_HYSTERESIS = 10;
    public static final double REBALANCE_HOLD_TIME = 20;
    
    public static final double ZONE_CO2_TARGET = 1000;
    public static final double ZONE_CO2_THRESHOLD = 800;
    public static final double ZONE_VOC_TARGET = 500;
    public static final double ZONE_VOC_THRESHOLD = 400;
    
    public static final double ZONE_PRIORITY_SPREAD = 2.0;
    public static final double ZONE_PRIORITY_MULTIPLIER = 1.3;
    public static final double ZONE_UNOCCUPIED_SETBACK = 5;
    public static final double ZONE_HEATING_USERLIMIT_MIN = 72;
    public static final double ZONE_HEATING_USERLIMIT_MAX = 67;
    public static final double ZONE_COOLING_USERLIMIT_MIN = 72;
    public static final double ZONE_COOLING_USERLIMIT_MAX = 77;
    public static final double ZONE_DEAD_TIME = 15;
    public static final double ZONE_AUTO_AWAY_TIME = 60;
    public static final double ZONE_FORCED_OCCUPIED_TIME = 120;
    public static final double CM_RESET_CMD_TIME = 90;
    
    public static final double ADR_COOLING_DEADBAND = 3;
    public static final double ADR_HEATING_DEADBAND = 3;
    
    public static final double SN_COOLING_AIRFLOW_TEMP = 60;
    public static final double SN_HEATING_AIRFLOW_TEMP = 105;
    
    public static final double CONSTANT_TEMP_ALERT_TIME = 40;
    public static final double ABNORMAL_TEMP_RISE_TRIGGER = 4;

    
    public static final double SYSTEM_PRECONDITION_RATE = 15.0;
    public static final double USER_LIMIT_SPREAD = 4;
    public static final double BUILDING_LIMIT_MIN = 55;
    public static final double BUILDING_LIMIT_MAX = 90;
    public static final double BUILDING_TO_ZONE_DIFFERENTIAL = 3;
    public static final double ZONE_TEMP_DEAD_LEEWAY = 10;
    public static final double CM_TEMP_INFLU_PERCENTILE_ZONE_DEAD = 50;

    public static final double MIN_COOLING_DAMPER = 40;
    public static final double MAX_COOLING_DAMPER = 80;
    public static final double MIN_HEATING_DAMPER = 40;
    public static final double MAX_HEATING_DAMPER = 80;
    
    public static final int VAV_DEFAULT_VAL_LEVEL = 17;
    public static final int VAV_BUILDING_VAL_LEVEL = 16;
	public static final int MANUAL_OVERRIDE_VAL_LEVEL = 7;

    public static final double STANDALONE_HEATING_DEADBAND_DEFAULT = 2.0;//Default deadband value based on dual temp diff 70 and 74 ((74-70)/2.0)
    public static final double STANDALONE_COOLING_DEADBAND_DEFAULT = 2.0;
    public static final double STANDALONE_STAGE1_HYSTERESIS_DEFAULT = 0.5;
    public static final double STANDALONE_AIRFLOW_SAMPLE_WAIT_TIME = 30;//in minutes
    public static final double STANDALONE_COOLING_STAGE1_LOWER_OFFSET = -20;
    public static final double STANDALONE_COOLING_STAGE1_UPPER_OFFSET = -8;
    public static final double STANDALONE_HEATING_STAGE1_LOWER_OFFSET = 25;
    public static final double STANDALONE_HEATING_STAGE1_UPPER_OFFSET = 40;
    public static final double STANDALONE_COOLING_STAGE2_LOWER_OFFSET = -25;
    public static final double STANDALONE_COOLING_STAGE2_UPPER_OFFSET = -12;
    public static final double STANDALONE_HEATING_STAGE2_LOWER_OFFSET = 35;
    public static final double STANDALONE_HEATING_STAGE2_UPPER_OFFSET = 50;
    public static final double STANDALONE_HEATING_THRESHOLD_2PFCU_DEFAULT = 85.0;
    public static final double STANDALONE_COOLING_THRESHOLD_2PFCU_DEFAULT = 65.0;
    public static final double STANDALONE_COOLING_PRECONDITIONING_RATE = 15;
    public static final double STANDALONE_HEATING_PRECONDITIONING_RATE = 15;

    public static final double STANDALONE_TARGET_DEHUMIDIFIER = 45;
    public static final double STANDALONE_TARGET_HUMIDITY = 25;
    public static final double STANDALONE_DEFAULT_FAN_OPERATIONAL_MODE = 1.0;//AUTO
    public static final double STANDALONE_DEFAULT_CONDITIONAL_MODE = 1.0;//Auto
    
    public static final double OAO_CO2_DAMPER_OPENING_RATE = 10 ;//(%/100ppm)
    public static final double OAO_ECONOMIZING_MIN_TEMP = 0 ;
    public static final double OAO_ECONOMIZING_MAX_TEMP = 70 ;
    public static final double OAO_ECONOMIZING_MIN_HUMIDITY = 0 ;
    public static final double OAO_ECONOMIZING_MAX_HUMIDITY = 100 ;
    public static final double OAO_OA_DAMPER_MAT_TARGET = 50 ;
    public static final double OAO_OA_DAMPER_MAT_MIN = 44 ;
    public static final double OAO_ECONOMIZING_TO_MAIN_COOLING_LOOP_MAP = 30 ;
    public static final double OAO_ENTHALPY_DUCT_COMPENSATION_OFFSET = 0 ;
    public static final double OAO_SMART_PURGE_RUNTIME_DEFAULT = 120 ;
    public static final double OAO_SMART_PRE_PURGE_START_TIME_OFFSET = 180 ;
    public static final double OAO_SMART_POST_PURGE_START_TIME_OFFSET = 20 ;
    public static final double OAO_SMART_PURGE_FAN_SPEED = 50 ;
    public static final double OAO_SMART_PURGE_FAN_LOOP_OUTPUT_MIN = 50 ;
    public static final double OAO_SMART_PURGE_MIN_DAMPER_OPEN_MULTIPLIER = 1.5 ;
    public static final double OAO_ECONOMIZING_DRY_BULB_THRESHOLD = 55 ;
    
	public static final String VAV_TUNER_GROUP = "VAV";
    public static final String DAB_TUNER_GROUP = "DAB";
    public static final String TI_TUNER_GROUP = "TI";
    public static final String OAO_TUNER_GROUP = "OAO";
    public static final String GENERIC_TUNER_GROUP = "GENERIC";
    public static final String TIMER_TUNER = "TIMER";
    public static final String TEMPERATURE_LIMIT = "TEMPERATURE";
    public static final String ALERT_TUNER = "ALERT";
    public static final String LCM_TUNER = "LCM";
    public static final String DUAL_DUCT_TUNER_GROUP = "DAB";
    
    public static final double DEFAULT_COOLING_DB = 2.0; //Default deadband value based on dual temp diff 70 and 74 (// (74-70)/2.0)
    public static final double DEFAULT_HEATING_DB = 2.0;
    public static final double DEFAULT_COOLING_DB_MULTPLIER = 0.5;
    public static final double DEFAULT_HEATING_DB_MULTIPLIER = 0.5;
    public static final double DEFAULT_PROPORTIONAL_GAIN = 0.5;
    public static final double DEFAULT_INTEGRAL_GAIN = 0.5;
    public static final double DEFAULT_PROPORTIONAL_SPREAD = 2;
    public static final double DEFAULT_INTEGRAL_TIMEOUT = 30;
    
    public static final double DEFAULT_MODE_CHANGEOVER_HYSTERESIS = 0.5;
    public static final double DEFAULT_STAGE_UP_TIMER_COUNTER = 5;
    public static final double DEFAULT_STAGE_DOWN_TIMER_COUNTER = 2;
    public static final double DEFAULT_FAN_ON_CONTROL_DELAY = 1;
    public static final double DEFAULT_REHEAT_ZONE_DAT_MIN_DIFFERENTIAL = 9;
    
    public static final double TR_IGNORE_REQUEST = 2.0;
    public static final double TR_TIME_DELAY = 2.0;
    public static final double TR_TIME_INTERVAL = 2.0;
    
    public static final double TR_SP_INIT_CO2 = 800.0;
    public static final double TR_SP_MAX_CO2 = 1000.0;
    public static final double TR_SP_MIN_CO2 = 800.0;
    public static final double TR_SP_RES_CO2 = -10.0;
    public static final double TR_SP_RESMAX_CO2 = -30.0;
    public static final double TR_SP_TRIM_CO2 = 20.0;
    
    public static final double TR_SP_INIT_SAT = 65;
    public static final double TR_SP_MAX_SAT = 65;
    public static final double TR_SP_MIN_SAT = 55;
    public static final double TR_SP_RES_SAT = -0.3;
    public static final double TR_SP_RESMAX_SAT = -1.0;
    public static final double TR_SP_TRIM_SAT = 0.2;
    
    public static final double TR_SP_INIT_SP = 0.2;
    public static final double TR_SP_MAX_SP = 1.0;
    public static final double TR_SP_MIN_SP = 0.2;
    public static final double TR_SP_RES_SP = 0.05;
    public static final double TR_SP_RESMAX_SP = 1.0;
    public static final double TR_SP_TRIM_SP = -0.02;
    
    public static final double ADAPTIVE_COMFORT_THRESHOLD_MARGIN = 4;
    public static final double CHILLED_WATER_TEMP_PROPORTION_SPREAD = 4;

}
