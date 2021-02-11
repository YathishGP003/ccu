package a75f.io.logic.tuners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import a75f.io.api.haystack.HSUtil;

class BuildingTunerFallback {
    
    /***
     * When BuildingTuner fails to fetch a point from remote , it should fall back to below values
     * on non-primary CCUs.
     * @param tags
     * @return
     */
    public static double getDefaultTunerVal(String tags) {
        
        List<String> tagList = Arrays.asList(tags.split(HSUtil.QUERY_JOINER));
        Optional<Double> defaultVal = getDefaultTagsValMap().entrySet().stream()
                                                            .filter(m -> {
                                                                for (String tag : tagList) {
                                                                    if (!m.getKey().contains(tag)) {
                                                                        return false;
                                                                    }
                                                                }
                                                                return true;
                                                            })
                                                            .map(m -> m.getValue())
                                                            .findFirst();
        return defaultVal.isPresent() ? defaultVal.get() : 0;
    }
    
    
    public static HashMap<String, Double> getDefaultTagsValMap() {
        HashMap<String, Double> tagsValMap = new HashMap<>();
        
        tagsValMap.put("unoccupied,setback",TunerConstants.ZONE_UNOCCUPIED_SETBACK);
        tagsValMap.put("dead,time",TunerConstants.ZONE_DEAD_TIME);
        tagsValMap.put("auto,away,time",TunerConstants.ZONE_AUTO_AWAY_TIME);
        tagsValMap.put("forced,occupied,time",TunerConstants.ZONE_FORCED_OCCUPIED_TIME);
        tagsValMap.put("adr,cooling,deadband",TunerConstants.ADR_COOLING_DEADBAND);
        tagsValMap.put("adr,heating,deadband",TunerConstants.ADR_HEATING_DEADBAND);
        tagsValMap.put("sn,cooling,airflow",TunerConstants.SN_COOLING_AIRFLOW_TEMP);
        tagsValMap.put("sn,heating,airflow",TunerConstants.SN_HEATING_AIRFLOW_TEMP);
        tagsValMap.put("constant,temp,alert,time",TunerConstants.ZONE_PRIORITY_SPREAD);
        tagsValMap.put("abnormal,cur,temp,rise",TunerConstants.ZONE_PRIORITY_SPREAD);
        
        tagsValMap.put("priority,spread",TunerConstants.ZONE_PRIORITY_SPREAD);
        tagsValMap.put("priority,multiplier",TunerConstants.ZONE_PRIORITY_MULTIPLIER);
        tagsValMap.put("cooling,deadband",TunerConstants.VAV_COOLING_DB);
        tagsValMap.put("cooling,deadband,multiplier",TunerConstants.VAV_COOLING_DB_MULTPLIER);
        tagsValMap.put("heating,deadband",TunerConstants.VAV_HEATING_DB);
        tagsValMap.put("heating,deadband,multiplier",TunerConstants.VAV_HEATING_DB_MULTIPLIER);
        tagsValMap.put("pgain",TunerConstants.VAV_PROPORTIONAL_GAIN);
        tagsValMap.put("pspread",TunerConstants.VAV_PROPORTIONAL_SPREAD);
        tagsValMap.put("igain",TunerConstants.VAV_INTEGRAL_GAIN);
        tagsValMap.put("itimeout",TunerConstants.VAV_INTEGRAL_TIMEOUT);
        tagsValMap.put("zone,co2,target",TunerConstants.ZONE_CO2_TARGET);
        tagsValMap.put("zone,co2,threshold",TunerConstants.ZONE_CO2_THRESHOLD);
        tagsValMap.put("zone,voc,target",TunerConstants.ZONE_VOC_TARGET);
        tagsValMap.put("zone,voc,threshold",TunerConstants.ZONE_VOC_THRESHOLD);
    
        tagsValMap.put("standalone,heating,deadband",TunerConstants.STANDALONE_HEATING_DEADBAND_DEFAULT);
        tagsValMap.put("standalone,cooling,deadband",TunerConstants.STANDALONE_COOLING_DEADBAND_DEFAULT);
        tagsValMap.put("standalone,stage1,hysteresis",TunerConstants.STANDALONE_STAGE1_HYSTERESIS_DEFAULT);
        tagsValMap.put("standalone,airflow,sample,wait",TunerConstants.STANDALONE_AIRFLOW_SAMPLE_WAIT_TIME);
        tagsValMap.put("standalone,stage1,cooling,lower,offset",TunerConstants.STANDALONE_COOLING_STAGE1_LOWER_OFFSET);
        tagsValMap.put("standalone,stage1,cooling,upper,offset",TunerConstants.STANDALONE_COOLING_STAGE1_UPPER_OFFSET);
        tagsValMap.put("standalone,stage1,heating,lower,offset",TunerConstants.STANDALONE_HEATING_STAGE1_LOWER_OFFSET);
        tagsValMap.put("standalone,stage1,heating,upper,offset",TunerConstants.STANDALONE_HEATING_STAGE1_UPPER_OFFSET);
        tagsValMap.put("standalone,stage2,cooling,lower,offset",TunerConstants.STANDALONE_COOLING_STAGE2_LOWER_OFFSET);
        tagsValMap.put("standalone,stage2,cooling,upper,offset",TunerConstants.STANDALONE_COOLING_STAGE2_UPPER_OFFSET);
        tagsValMap.put("standalone,stage2,heating,lower,offset",TunerConstants.STANDALONE_HEATING_STAGE2_LOWER_OFFSET);
        tagsValMap.put("standalone,stage2,heating,upper,offset",TunerConstants.STANDALONE_HEATING_STAGE2_UPPER_OFFSET);
        
        tagsValMap.put("standalone,cooling,preconditioning,rate",TunerConstants.STANDALONE_COOLING_PRECONDITIONING_RATE);
        tagsValMap.put("standalone,heating,preconditioning,rate", TunerConstants.STANDALONE_HEATING_PRECONDITIONING_RATE);
        tagsValMap.put("standalone,heating,threshold,pipe2,fcu",TunerConstants.STANDALONE_HEATING_THRESHOLD_2PFCU_DEFAULT);
        tagsValMap.put("standalone,cooling,threshold,pipe2,fcu",TunerConstants.STANDALONE_COOLING_THRESHOLD_2PFCU_DEFAULT);
    
        tagsValMap.put("valve,start,damper",TunerConstants.VALVE_START_DAMPER);
    
        tagsValMap.put("tr,co2,ignoreRequest",TunerConstants.TR_IGNORE_REQUEST);
        tagsValMap.put("tr,co2,spinit",TunerConstants.TR_SP_INIT_CO2);
        tagsValMap.put("tr,co2,spmax",TunerConstants.TR_SP_MAX_CO2);
        tagsValMap.put("tr,co2,spmin",TunerConstants.TR_SP_MIN_CO2);
        tagsValMap.put("tr,co2,spres",TunerConstants.TR_SP_RES_CO2);
        tagsValMap.put("tr,co2,spresmax",TunerConstants.TR_SP_RESMAX_CO2);
        tagsValMap.put("tr,co2,sptrim",TunerConstants.TR_SP_TRIM_CO2);
        tagsValMap.put("tr,co2,timeDelay",TunerConstants.TR_TIME_DELAY);
        tagsValMap.put("tr,co2,timeInterval",TunerConstants.TR_TIME_INTERVAL);
        
        tagsValMap.put("tr,sat,ignoreRequest",TunerConstants.TR_IGNORE_REQUEST);
        tagsValMap.put("tr,sat,spinit",TunerConstants.TR_SP_INIT_SAT);
        tagsValMap.put("tr,sat,spmax",TunerConstants.TR_SP_MAX_SAT);
        tagsValMap.put("tr,sat,spmin",TunerConstants.TR_SP_MIN_SAT);
        tagsValMap.put("tr,sat,spres",TunerConstants.TR_SP_RES_SAT);
        tagsValMap.put("tr,sat,spresmax",TunerConstants.TR_SP_RESMAX_SAT);
        tagsValMap.put("tr,sat,sptrim",TunerConstants.TR_SP_TRIM_SAT);
        tagsValMap.put("tr,sat,timeDelay",TunerConstants.TR_TIME_DELAY);
        tagsValMap.put("tr,sat,timeInterval",TunerConstants.TR_SP_MAX_CO2);
        
        tagsValMap.put("tr,staticPressure,ignoreRequest",TunerConstants.TR_IGNORE_REQUEST);
        tagsValMap.put("tr,staticPressure,spinit",TunerConstants.TR_SP_INIT_SP);
        tagsValMap.put("tr,staticPressure,spmax",TunerConstants.TR_SP_MAX_SP);
        tagsValMap.put("tr,staticPressure,spmin",TunerConstants.TR_SP_MIN_SP);
        tagsValMap.put("tr,staticPressure,spres",TunerConstants.TR_SP_RES_SP);
        tagsValMap.put("tr,staticPressure,spresmax",TunerConstants.TR_SP_RESMAX_SAT);
        tagsValMap.put("tr,staticPressure,sptrim",TunerConstants.TR_SP_TRIM_SP);
        tagsValMap.put("tr,staticPressure,timeDelay",TunerConstants.TR_TIME_DELAY);
        tagsValMap.put("tr,staticPressure,timeInterval",TunerConstants.TR_TIME_INTERVAL);
        
        
        return tagsValMap;
    }
    
    
}
