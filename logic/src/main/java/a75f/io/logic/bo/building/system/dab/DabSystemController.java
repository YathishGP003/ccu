package a75f.io.logic.bo.building.system.dab;

import android.util.Log;

import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.dab.DabProfile;
import a75f.io.logic.bo.building.dualduct.DualDuctProfile;
import a75f.io.logic.bo.building.system.SystemConstants;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.SystemPILoopController;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.bo.util.HSEquipUtil;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.bo.building.system.SystemController.State.OFF;
import static a75f.io.logic.bo.building.system.SystemMode.AUTO;
import static a75f.io.logic.bo.building.system.SystemMode.COOLONLY;
import static a75f.io.logic.bo.building.system.SystemMode.HEATONLY;

/**
 * Created by samjithsadasivan on 1/9/19.
 */

public class DabSystemController extends SystemController
{
    int    integralMaxTimeout = 15;
    int proportionalSpread = 2;
    double proportionalGain = 0.5;
    double integralGain = 0.5;
    
    private static DabSystemController instance = new DabSystemController();
    
    SystemPILoopController piController;
    
    int coolingSignal;
    int heatingSignal;
    
    EvictingQueue<Double> weightedAverageChangeOverLoadQueue = EvictingQueue.create(15);
    
    
    int ciDesired;
    int comfortIndex = 0;
    
    
    double totalCoolingLoad = 0;
    double totalHeatingLoad = 0;
    int zoneCount = 0;
    
    double weightedAverageLoadSum;
    
    double weightedAverageConditioningLoad;
    double weightedAverageConditioningLoadPostML;
    
    double weightedAverageLoadMA;
    double weightedAverageCoolingLoadPostML ;
    double weightedAverageHeatingLoadPostML  ;
    
    double averageSystemHumidity = 0;
    double averageSystemTemperature = 0;
    
    double co2LoopOpWeightedAverage = 0;
    double co2WeightedAverage = 0;

    private SystemMode conditioningMode = SystemMode.OFF;
    private DabSystemProfile systemProfile = null;
    double prioritySum = 0;
    double co2LoopWeightedAverageASum = 0;
    double co2WeightedAverageSum = 0;
    int zoneDeadCount = 0;
    boolean hasTi = false;
    
    double weightedAverageChangeoverLoadSum = 0;
    double weightedAverageChangeoverLoad = 0;
    
    private Occupancy currSystemOccupancy = Occupancy.UNOCCUPIED;

    private DabSystemController()
    {
        //Read tuners to initialize PI variables
        
        piController = new SystemPILoopController();
        piController.setIntegralGain(integralGain);
        piController.setProportionalGain(proportionalGain);
        piController.setMaxAllowedError(proportionalSpread);
        piController.setIntegralMaxTimeout(integralMaxTimeout);
    }
    
    public static DabSystemController getInstance() {
        return instance;
    }
    
    public void runDabSystemControlAlgo() {
        
        initializeAlgoLoopVariables();

        ArrayList<HashMap<Object, Object>> allEquips = CCUHsApi
                                                           .getInstance()
                                                           .readAllEntities("(equip and zone and dab) or " +
                                                                            "(equip and zone and dualDuct) or " +
                                                                            "(equip and zone and ti)"
        );
    
        updateSystemTempHumidity(allEquips);
        
        processZoneEquips(allEquips);
        processCCUAsZoneEquip();
        
        if (prioritySum == 0 || zoneCount == 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "No valid temperature, Skip DabSystemControlAlgo");
            systemState = OFF;
            reset();
            return;
        }else if(systemState == OFF) {
            systemState = COOLING;
            piController.reset();
        }

        updateWeightedAverageLoad();
        
        if (isEmergencyCoolingRequired()) {
            handleEmergencyCooling();
        } else if (isEmergencyHeatingRequired()) {
            handleEmergencyHeating();
        } else {
            handleOperationalChangeOver();
        }

        systemProfile.setSystemPoint("operating and mode", systemState.ordinal());
        updateLoopOpSignals();
        
        logAlgoVariables();
        
        ArrayList<HashMap<Object, Object>> dabEquips = CCUHsApi.getInstance()
                                                               .readAllEntities("equip and zone and dab");
        HashMap<String, Double> damperPosMap;

        if (isNormalizationRequired()) {
            HashMap<String, Double> normalizedDamperPosMap = getNormalizedDamperPosMap(dabEquips,
                                                                                       getBaseDamperPosMap(dabEquips));
            damperPosMap = getAdjustedDamperPosMap(dabEquips, normalizedDamperPosMap, systemProfile.getSystemEquipRef());
        } else {
            damperPosMap = getBaseDamperPosMap(dabEquips);
        }

        applyLimitsAndSetDamperPosition(dabEquips, damperPosMap);
    }

    private void initializeAlgoLoopVariables() {

        systemProfile = (DabSystemProfile) L.ccu().systemProfile;
        ciDesired = (int)systemProfile.getUserIntentVal("desired and ci");
        conditioningMode = SystemMode.values()[(int)systemProfile.getUserIntentVal("conditioning and mode")];
        CcuLog.d(L.TAG_CCU_SYSTEM, "runDabSystemControlAlgo -> ciDesired: " + ciDesired + " conditioningMode: "
                                   + conditioningMode
        );

        weightedAverageCoolingLoadPostML = 0;
        weightedAverageHeatingLoadPostML = 0;
        weightedAverageLoadSum = 0;
        co2LoopOpWeightedAverage = 0;
        co2WeightedAverage = 0;
        totalCoolingLoad = 0;
        totalHeatingLoad = 0;
        zoneCount = 0;
        prioritySum = 0;
        co2LoopWeightedAverageASum = 0;
        co2WeightedAverageSum = 0;
        zoneDeadCount = 0;
        hasTi = false;
        weightedAverageChangeoverLoadSum = 0;
        weightedAverageChangeoverLoad = 0;
    
        Occupancy occupancy = ScheduleProcessJob.getSystemOccupancy();
        if (currSystemOccupancy == Occupancy.OCCUPIED ||
            currSystemOccupancy == Occupancy.PRECONDITIONING ||
            currSystemOccupancy == Occupancy.FORCEDOCCUPIED ||
            currSystemOccupancy == Occupancy.OCCUPANCYSENSING) {
            
            if (occupancy == Occupancy.UNOCCUPIED ||
                occupancy == Occupancy.VACATION) {
                CcuLog.d(L.TAG_CCU_SYSTEM, "Reset Loop : Occupancy changed from "+currSystemOccupancy+" to "+occupancy);
                resetLoop();
            }
        }
        currSystemOccupancy = occupancy;
    }

    private void updateSystemTempHumidity(ArrayList<HashMap<Object, Object>> allEquips) {

        updateSystemHumidity(allEquips);
        updateSystemTemperature(allEquips);
        updateSystemDesiredTemp();

        systemProfile.setSystemPoint("average and humidity", averageSystemHumidity);
        systemProfile.setSystemPoint("average and temp", averageSystemTemperature);

    }
    
    private boolean isEmergencyCoolingRequired() {
        return systemState != HEATING &&
               (buildingLimitMaxBreached("dab") || buildingLimitMaxBreached("dualDuct")) &&
               conditioningMode != SystemMode.OFF;
    }
    
    private boolean isEmergencyHeatingRequired() {
        return systemState != COOLING &&
               (buildingLimitMinBreached("dab") || buildingLimitMinBreached("dualDuct")) &&
               conditioningMode != SystemMode.OFF;
    }

    private void processZoneEquips(ArrayList<HashMap<Object, Object>> allEquips) {

        for (HashMap<Object, Object> equipMap : allEquips) {

            Equip equip = new Equip.Builder().setHashMap(equipMap).build();
            hasTi = hasTi || equip.getMarkers().contains("ti");

            if (isZoneDead(equip)) {
                zoneDeadCount++;
            } else if (hasTemp(equip)) {
                double zoneCurTemp = getEquipCurrentTemp(equip.getId());
                double desiredTempCooling = HSEquipUtil.getDesiredTempCooling(equip.getId());
                double desiredTempHeating = HSEquipUtil.getDesiredTempHeating(equip.getId());
                double tempMidPoint = (desiredTempCooling + desiredTempHeating)/2;
                double zoneCoolingLoad = zoneCurTemp > tempMidPoint ? zoneCurTemp - desiredTempCooling : 0;
                double zoneHeatingLoad = zoneCurTemp < tempMidPoint ? desiredTempHeating - zoneCurTemp : 0;
                double zoneDynamicPriority = getEquipDynamicPriority(zoneCoolingLoad > 0 ?
                                                                         zoneCoolingLoad : zoneHeatingLoad, equip.getId());
                totalCoolingLoad += zoneCoolingLoad;
                totalHeatingLoad += zoneHeatingLoad;
                zoneCount++;
                weightedAverageLoadSum +=
                    (zoneCoolingLoad * zoneDynamicPriority) - (zoneHeatingLoad * zoneDynamicPriority);
                prioritySum += zoneDynamicPriority;
                co2LoopWeightedAverageASum += (getEquipCo2LoopOp(equip.getId()) * zoneDynamicPriority);
                co2WeightedAverageSum += (getEquipCo2(equip.getId()) * zoneDynamicPriority);
                
                weightedAverageChangeoverLoadSum += (zoneCurTemp - tempMidPoint) * zoneDynamicPriority;
                
                CcuLog.d(L.TAG_CCU_SYSTEM, equip.getDisplayName() + " zoneDynamicPriority: " + zoneDynamicPriority +
                                           " zoneCoolingLoad: " + zoneCoolingLoad + " zoneHeatingLoad: " + zoneHeatingLoad
                );
                CcuLog.d(L.TAG_CCU_SYSTEM,
                         equip.getDisplayName() + " weightedAverageLoadSum: " + weightedAverageLoadSum +
                         " co2LoopWASum " + co2LoopWeightedAverageASum + " co2WASum " + co2WeightedAverageSum+
                         " weightedAverageChangeoverLoadSum "+weightedAverageChangeoverLoadSum+
                         " prioritySum "+prioritySum
                );
            }
        }

    }

    private void processCCUAsZoneEquip() {

        double cmTempInfForPercentileZonesDead = TunerUtil.readTunerValByQuery("dead and percent and influence",
                                                                               L.ccu().systemProfile.getSystemEquipRef());
        if(zoneDeadCount > 0)
            CcuLog.d(L.TAG_CCU_SYSTEM, "DabSysController = "+hasTi+","+zoneDeadCount+"," +
                                       ""+zoneCount+","+cmTempInfForPercentileZonesDead
            );

        if( (zoneCount == 0) || (!hasTi && ((zoneDeadCount > 0)
                   && (((double)(zoneDeadCount*100)/(zoneDeadCount + zoneCount)) >= cmTempInfForPercentileZonesDead)))){

            String sysEquip = L.ccu().systemProfile.getSystemEquipRef();
            if(sysEquip != null) {
                double cmCurrentTemp = getCMCurrentTemp(sysEquip);
                if(isCMTempDead(cmCurrentTemp)) {
                    double desiredTempCooling = ScheduleProcessJob.getSystemCoolingDesiredTemp();
                    double desiredTempHeating = ScheduleProcessJob.getSystemHeatingDesiredTemp();
                    double tempMidPoint = (desiredTempCooling + desiredTempHeating)/2;
                    
                    double cmCoolingLoad = cmCurrentTemp > tempMidPoint ? cmCurrentTemp - desiredTempCooling : 0;
                    double cmHeatingLoad = cmCurrentTemp < tempMidPoint ? desiredTempHeating - cmCurrentTemp : 0;
                    double zoneDynamicPriority = getCMDynamicPriority(cmCoolingLoad > 0 ? cmCoolingLoad : cmHeatingLoad);
                    totalCoolingLoad += cmCoolingLoad;
                    totalHeatingLoad += cmHeatingLoad;
                    zoneCount++;

                    weightedAverageLoadSum += (cmCoolingLoad * zoneDynamicPriority) - (cmHeatingLoad * zoneDynamicPriority);
                    prioritySum += zoneDynamicPriority;
                    
                    weightedAverageChangeoverLoadSum += (cmCurrentTemp - tempMidPoint) * zoneDynamicPriority;
                    
                    CcuLog.d(L.TAG_CCU_SYSTEM, "CM dab zoneDynamicPriority: " + zoneDynamicPriority +
                                               " cmCoolingLoad: " + cmCoolingLoad +
                                               " cmHeatingLoad: " + cmHeatingLoad +
                                               " weightedAverageLoadSum " + weightedAverageLoadSum +
                                               "," + prioritySum + "," + cmCurrentTemp+
                                               " weightedAverageChangeoverLoadSum "+weightedAverageChangeoverLoadSum
                    );
                }
            }
        }
    }
    
    private void handleEmergencyCooling() {
        
        CcuLog.d(L.TAG_CCU_SYSTEM, " Emergency COOLING Active");
        emergencyMode = true;
        if (conditioningMode == COOLONLY || conditioningMode == AUTO)
        {
            if (systemState != COOLING)
            {
                systemState = COOLING;
                piController.reset();
            }
        } else {
            //systemState = OFF;
            coolingSignal = 0;
            heatingSignal = 0;
            piController.reset();
        }
    }

    private void handleEmergencyHeating() {
        
        CcuLog.d(L.TAG_CCU_SYSTEM, " Emergency HEATING Active");
        emergencyMode = true;
        if (conditioningMode == HEATONLY || conditioningMode == AUTO)
        {
            if (systemState != HEATING)
            {
                systemState = HEATING;
                piController.reset();
            }
        } else {
            //systemState = OFF;
            coolingSignal = 0;
            heatingSignal = 0;
            piController.reset();
        }
    }

    private void handleOperationalChangeOver() {
        
        if (emergencyMode) {
            CcuLog.d(L.TAG_CCU_SYSTEM, " Emergency CONDITIONING Disabled");
            piController.reset();
            emergencyMode = false;
        }

        double modeChangeoverHysteresis = TunerUtil.readTunerValByQuery("system and mode and changeover and " +
                                                                        "hysteresis and equipRef == \""+
                                                                        L.ccu().systemProfile.getSystemEquipRef()+"\"");
        CcuLog.d(L.TAG_CCU_SYSTEM," handleOperationalChangeOver : modeChangeoverHysteresis "+modeChangeoverHysteresis);
        if ((conditioningMode == COOLONLY || conditioningMode == AUTO) && weightedAverageLoadMA > modeChangeoverHysteresis) {
            if (systemState != COOLING) {
                systemState = COOLING;
                piController.reset();
            }
        } else if ((conditioningMode == HEATONLY || conditioningMode == AUTO) && weightedAverageLoadMA < (-1 * modeChangeoverHysteresis)) {
            if (systemState != HEATING) {
                systemState = HEATING;
                piController.reset();
            }
        } else {
            //systemState = OFF;

            coolingSignal = 0;
            heatingSignal = 0;
            piController.reset();
        }
    }
    
    private void updateLoopOpSignals() {

        piController.dump();
        if ((systemState == COOLING) && (conditioningMode == COOLONLY || conditioningMode == AUTO)){
            heatingSignal = 0;
            coolingSignal = (int)piController.getLoopOutput(weightedAverageCoolingLoadPostML, 0);
        } else if ((systemState == HEATING) && (conditioningMode == HEATONLY || conditioningMode == AUTO)){
            coolingSignal = 0;
            heatingSignal = (int)piController.getLoopOutput(weightedAverageHeatingLoadPostML, 0);
        } else {
            coolingSignal = 0;
            heatingSignal = 0;
        }
        piController.dump();
    }

    private void updateWeightedAverageLoad() {

        weightedAverageConditioningLoad = weightedAverageLoadSum / prioritySum;
        co2LoopOpWeightedAverage = co2LoopWeightedAverageASum/prioritySum;
        co2WeightedAverageSum = co2WeightedAverageSum/prioritySum;

        comfortIndex = (int)(totalCoolingLoad + totalHeatingLoad) /zoneCount;

        systemProfile.setSystemPoint("ci and running", comfortIndex);
    
        weightedAverageConditioningLoadPostML = weightedAverageConditioningLoad ;//+buildingLoadOffsetML
        
        weightedAverageChangeoverLoad = weightedAverageChangeoverLoadSum/prioritySum;
    
        weightedAverageChangeOverLoadQueue.add(weightedAverageChangeoverLoad);
        
        double weightedAverageChangeOverLoadQueueSum = 0;
        for (double val : weightedAverageChangeOverLoadQueue) {
            weightedAverageChangeOverLoadQueueSum += val;
        }
        weightedAverageLoadMA = weightedAverageChangeOverLoadQueueSum/weightedAverageChangeOverLoadQueue.size();
    
        weightedAverageCoolingLoadPostML = weightedAverageLoadMA > 0 ? weightedAverageConditioningLoadPostML : 0;
        weightedAverageHeatingLoadPostML = weightedAverageLoadMA < 0 ? -1 * weightedAverageConditioningLoadPostML : 0;
        
        systemProfile.setSystemPoint("weighted and average and moving and load",
                               CCUUtils.roundToTwoDecimal(weightedAverageLoadMA));
        systemProfile.setSystemPoint("weighted and average and cooling and load",
                               CCUUtils.roundToTwoDecimal(weightedAverageCoolingLoadPostML));
        systemProfile.setSystemPoint("weighted and average and heating and load",
                               CCUUtils.roundToTwoDecimal(weightedAverageHeatingLoadPostML));
    }
    
    private void logAlgoVariables() {
        
        CcuLog.d(L.TAG_CCU_SYSTEM, "weightedAverageLoadMA "+weightedAverageLoadMA+
                                   " co2LoopOpWA "+co2LoopOpWeightedAverage+" " +
                                   "systemState: "+systemState
        );

        CcuLog.d(L.TAG_CCU_SYSTEM, "weightedAverageConditioningLoadPostML "+weightedAverageConditioningLoadPostML+
                                   " weightedAverageCoolingLoadPostML: " + weightedAverageCoolingLoadPostML +
                                   " weightedAverageHeatingLoadPostML: " + weightedAverageHeatingLoadPostML +
                                   " coolingSignal: " + coolingSignal + " heatingSignal: " + heatingSignal
        );

    }
    
    private boolean isNormalizationRequired() {
        return (systemState != OFF)
               && (coolingSignal > 0 || heatingSignal > 0)
               && (conditioningMode != SystemMode.OFF);
    }
    
    private HashMap<String, Double> getBaseDamperPosMap(ArrayList<HashMap<Object, Object>> dabEquips) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<String, Double> damperPosMap = new HashMap<>();
        for (HashMap<Object, Object> dabEquip : dabEquips) {
            HashMap<Object, Object> primaryDamper = hayStack.readEntity(
                "point and damper and base and primary and cmd and equipRef == \"" +
                dabEquip.get("id").toString() + "\""
            );
            double primaryDamperPos = hayStack.readHisValById(primaryDamper.get("id").toString());
    
            HashMap<Object, Object> secondaryDamper = hayStack.readEntity(
                "point and damper and base and secondary and cmd and equipRef == \""
                + dabEquip.get("id").toString() + "\""
            );
            double secondaryDamperPos = hayStack.readHisValById(secondaryDamper.get("id").toString());
            
            HashMap<Object, Object> normalizedPrimaryDamper = hayStack.readEntity(
                "point and damper and normalized and primary and cmd and equipRef == \"" +
                dabEquip.get("id").toString() + "\""
            );
            HashMap<Object, Object> normalizedSecondaryDamper = hayStack.readEntity(
                "point and damper and normalized and secondary and " + "cmd and equipRef == \"" +
                dabEquip.get("id").toString() + "\""
            );
            
            damperPosMap.put(normalizedPrimaryDamper.get("id").toString(), primaryDamperPos);
            damperPosMap.put(normalizedSecondaryDamper.get("id").toString(), secondaryDamperPos);
        }
        return damperPosMap;
    }
    
    @Override
    public int getCoolingSignal() {
        return coolingSignal;
    }
    
    @Override
    public int getHeatingSignal() {
        return heatingSignal;
    }
    
    public SystemController.State getConditioningForecast(Occupied occupiedSchedule) {
        DabSystemProfile profile = (DabSystemProfile) L.ccu().systemProfile;
        SystemMode systemMode = SystemMode.values()[(int)profile.getUserIntentVal("conditioning and mode")];
        
        if ((systemMode == COOLONLY || systemMode == AUTO) && (getAverageSystemTemperature() > occupiedSchedule.getCoolingVal()))
        {
            return COOLING;
        }
        else if ((systemMode == HEATONLY || systemMode == AUTO) && (getAverageSystemTemperature() < occupiedSchedule.getHeatingVal()))
        {
            return HEATING;
        }
        else
        {
            return OFF;
        }
        
    }
    
    public double getEquipCurrentTemp(String equipRef) {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and equipRef == \"" +
                                                        ""+equipRef+"\""
        );
    }
    public double getCMCurrentTemp(String equipRef) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and temp and cm and current and equipRef == \""
                                                        +equipRef+"\""
        );
    }
    
    public ZonePriority getEquipPriority(String equipRef) {
        double priorityVal = CCUHsApi.getInstance().readDefaultVal("point and zone and config and priority and equipRef == \""
                                                                   +equipRef+"\""
        );
        return ZonePriority.values()[(int) priorityVal];
    }

    public double getCMDynamicPriority(double zoneLoad){
        ZonePriority p = ZonePriority.NORMAL;
        double zonePrioritySpread = TunerUtil.readTunerValByQuery("point and default and tuner and zone and priority and " +
                                                                  "spread and dab"
        );
        double zonePriorityMultiplier = TunerUtil.readTunerValByQuery("point and default and tuner and zone and priority " +
                                                                      "and multiplier and dab");
        return p.val * Math.pow(zonePriorityMultiplier, (zoneLoad/zonePrioritySpread) > 10 ? 10 : (zoneLoad/zonePrioritySpread));
    }

    public double getEquipDynamicPriority(double zoneLoad, String equipRef) {
        ZonePriority p = getEquipPriority(equipRef);
        if (getEquipCurrentTemp(equipRef) == 0) {
            return p.val;
        }
        
        double zonePrioritySpread = TunerUtil.readTunerValByQuery("point and tuner and zone and priority and spread and " +
                                                                  "equipRef == \"" + equipRef + "\""
        );
        double zonePriorityMultiplier = TunerUtil.readTunerValByQuery("point and tuner and zone and priority and multiplier " +
                                                                      "and equipRef == \""+equipRef+"\""
        );
        double equipDynamicPriority = p.val * Math.pow(zonePriorityMultiplier, (zoneLoad/zonePrioritySpread) > 10 ? 10 :
                                                                                   (zoneLoad/zonePrioritySpread));
        equipDynamicPriority = CCUUtils.roundToTwoDecimal(equipDynamicPriority);
        try {
            HashMap<Object, Object> zdpPoint =
                CCUHsApi.getInstance().readEntity("point and zone and dynamic and priority and equipRef == \""
                                                  + equipRef + "\""
            );
            double zdpPointValue = CCUHsApi.getInstance().readHisValById(zdpPoint.get("id").toString());
            if (zdpPointValue != equipDynamicPriority)
                CCUHsApi.getInstance().writeHisValById(zdpPoint.get("id").toString(), equipDynamicPriority);
        }catch (Exception e){
            e.printStackTrace();
        }
        return equipDynamicPriority;
    }
    
    public double getEquipCo2LoopOp(String equipRef){
        for (ZoneProfile profile : L.ccu().zoneProfiles) {
            Equip equip = profile.getEquip();
            if (equip.getMarkers().contains("dab") || equip.getMarkers().contains("dualDuct"))
            {
                double enabledCO2Control = CCUHsApi.getInstance().readDefaultVal("point and config and dab and enable " +
                                                                                 "and co2 and equipRef == \"" + equip.getId() + "\""
                );
                if (equip.getId().equals(equipRef) && enabledCO2Control > 0)
                {
                    if (profile instanceof DabProfile) {
                        DabProfile dabProfile = (DabProfile) profile;
                        return dabProfile.getCo2LoopOp();
                    } else if (profile instanceof DualDuctProfile) {
                        DualDuctProfile dualDuctProfile = (DualDuctProfile) profile;
                        return dualDuctProfile.getCo2LoopOp();
                    }
                }
            }
        }
        return 0;
    }
    
    private double getEquipCo2(String equipRef) {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and co2 and sensor and current and equipRef == \"" +
                                                        ""+equipRef+"\""
        );
    }
    
    public void updateSystemHumidity(ArrayList<HashMap<Object, Object>> allEquips) {
        //Average across zones or from proxy zone.
        double humiditySum = 0;
        double humidityZones = 0;
        
        for (HashMap<Object, Object> equip : allEquips)
        {
            double humidityVal = CCUHsApi.getInstance().readHisValByQuery("point and air and humidity and sensor and current " +
                                                                "and equipRef == \""+ equip.get("id")+"\""
            );
            
            if (humidityVal != 0) {
                humiditySum += humidityVal;
                humidityZones++;
            }
        }
        averageSystemHumidity = humidityZones == 0 ? 0 : humiditySum/humidityZones;
        averageSystemHumidity = CCUUtils.roundToOneDecimal(averageSystemHumidity);
    }
    
    @Override
    public double getAverageSystemHumidity() {
        return averageSystemHumidity;
    }

    public void updateSystemDesiredTemp(){
        try {

            double desiredTempCooling = ScheduleProcessJob.getSystemCoolingDesiredTemp();
            double desiredTempHeating = ScheduleProcessJob.getSystemHeatingDesiredTemp();
            HashMap<Object, Object> coolTempPoint = CCUHsApi.getInstance()
                                                            .readEntity("point and system and cm and cooling" +
                                                                    " and desired and temp and equipRef == \"" +
                                                                    L.ccu().systemProfile.getSystemEquipRef() + "\""
            );
            CCUHsApi.getInstance().writeHisValById(coolTempPoint.get("id").toString(), desiredTempCooling);
            HashMap<Object, Object> heatTempPoint = CCUHsApi.getInstance()
                                                            .readEntity("point and system and cm and " +
                                                                        "heating and desired and temp and equipRef == \"" +
                                                                        L.ccu().systemProfile.getSystemEquipRef() + "\""
            );
            CCUHsApi.getInstance().writeHisValById(heatTempPoint.get("id").toString(), desiredTempHeating);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    
    public void updateSystemTemperature(ArrayList<HashMap<Object, Object>> allEquips) {
        //Average across zones or from proxy zone.
        double tempSum = 0;
        double tempZones = 0;
        int totalEquips = 0;
        boolean hasTi = false;
        
        for (HashMap<Object, Object> equipMap : allEquips)
        {
            Equip equip = new Equip.Builder().setHashMap(equipMap).build();
            if(equip.getMarkers().contains("dab") || equip.getMarkers().contains("dualDuct") ||
                                                                    equip.getMarkers().contains("ti")) {
                double tempVal = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and " +
                                                             "equipRef == \"" + equipMap.get("id") + "\""
                );
                hasTi = hasTi || equip.getMarkers().contains("ti");
                if (!isZoneDead(equip) && (tempVal > 0)) {
                    tempSum += tempVal;
                    tempZones++;
                }
                totalEquips++;
            }
        }
        double cmTempInfForPercentileZonesDead = TunerUtil.readTunerValByQuery("dead and percent and influence",
                                                                               L.ccu().systemProfile.getSystemEquipRef());
        if( (totalEquips == 0) || (!hasTi && ((((totalEquips -tempZones )*100)/(totalEquips)) >= cmTempInfForPercentileZonesDead))){
            double cmTemp = getCMCurrentTemp(L.ccu().systemProfile.getSystemEquipRef());
            if(isCMTempDead(cmTemp)) {
                tempSum += cmTemp;
                tempZones++;
            }
        }
        averageSystemTemperature = tempZones == 0 ? 0 : tempSum/tempZones;
        averageSystemTemperature =CCUUtils.roundToOneDecimal(averageSystemTemperature);
        
        CcuLog.d(L.TAG_CCU_SYSTEM, "averageSystemTemperature "+averageSystemTemperature+" tempZone "+tempZones
                                                                +" tempSum "+tempSum);
    }
    
    public boolean isZoneDead(Equip q) {
        try
        {
            return CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and equipRef " +
                                                            "== \"" + q.getId() + "\"").equals("Zone Temp Dead"
                   );
        } catch (Exception e) {
            return false;
        }
    }
    public boolean isCMTempDead(double cmTemp) {

        double buildingLimitMax =  TunerUtil.readBuildingTunerValByQuery("building and limit and max");
        double buildingLimitMin =  TunerUtil.readBuildingTunerValByQuery("building and limit and min");

        double tempDeadLeeway = TunerUtil.readBuildingTunerValByQuery("temp and dead and leeway");
        return !(cmTemp > (buildingLimitMax + tempDeadLeeway)) && !(cmTemp < (buildingLimitMin - tempDeadLeeway));
    }

    public boolean hasTemp(Equip q) {
        try
        {
            return CCUHsApi.getInstance().readHisValByQuery("point and current and temp and equipRef == \"" + q.getId()
                                                            + "\"") > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public double getAverageSystemTemperature() {
        return averageSystemTemperature;
    }
    
    public double getWACo2LoopOp() {
        return co2LoopOpWeightedAverage;
    }
    
    @Override
    public double getSystemCO2WA() {
        return co2WeightedAverage;
    }
    
    @Override
    public State getSystemState() {
        return systemState;
    }
    
    /*
     * Of all the zones, find the zone with the largest damperPosition. If this is not 100, then proportionally
     * increase damper positions in all zones such that this chosen one  has 100% damper opening.  Eg. if there are
     * calculated damper positions of 40, 70, 80, 90. Then we increase each by 11% to get rounded values of
     * 44, 78,89, 100% after normalizing.
     * */
    public HashMap<String, Double> getNormalizedDamperPosMap(ArrayList<HashMap<Object, Object>> dabEquips,
                                                             HashMap<String, Double> damperPosMap) {
    
        CCUHsApi hayStack = CCUHsApi.getInstance();
        double maxDamperPos = getMaxDamperPos(dabEquips);
        
        //maxDamperPos could be zero when all the zones are dead
        if (maxDamperPos == 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM, " Abort normalizeAirflow : maxDamperPos = " + maxDamperPos);
            return damperPosMap;
        }
    
        HashMap<String, Double> normalizedDamperPosMap = new HashMap<>();
        double targetPercent = (100 - maxDamperPos) * 100 / maxDamperPos;
        for (HashMap<Object, Object> dabEquip : dabEquips) {
            HashMap<Object, Object> normalizedPrimaryDamper = hayStack.readEntity(
                "point and damper and normalized and primary and cmd and equipRef == \""
                                                                + dabEquip.get("id").toString() + "\""
            );
            HashMap<Object, Object> normalizedSecondaryamper = hayStack.readEntity(
                "point and damper and normalized and secondary and cmd and equipRef == \""
                                                                + dabEquip.get("id").toString() + "\""
            );
            
            double primaryDamperPos;
            double secondaryDamperPos;
            double normalizedPrimaryDamperPos;
            double normalizedSecondaryDamperPos;
    
            primaryDamperPos = damperPosMap.get(normalizedPrimaryDamper.get("id").toString());
            secondaryDamperPos = damperPosMap.get(normalizedSecondaryamper.get("id").toString());
            if (isZoneDead(new Equip.Builder().setHashMap(dabEquip).build())) {
                Log.d("CCU_SYSTEM", "Skip Normalize, Equip Dead " + dabEquip.toString());
                normalizedPrimaryDamperPos = primaryDamperPos;
                normalizedSecondaryDamperPos = secondaryDamperPos;
            } else {
                normalizedPrimaryDamperPos = (primaryDamperPos + primaryDamperPos * targetPercent / 100);
                normalizedSecondaryDamperPos = (secondaryDamperPos + secondaryDamperPos * targetPercent / 100);
            }
            
            normalizedDamperPosMap.put(normalizedPrimaryDamper.get("id").toString(), normalizedPrimaryDamperPos);
            normalizedDamperPosMap.put(normalizedSecondaryamper.get("id").toString(), normalizedSecondaryDamperPos);
    
            CcuLog.d(L.TAG_CCU_SYSTEM,
                     "normalizeAirflow" + " Equip: " +
                     dabEquip.get("dis") + " ,damperPos :" + primaryDamperPos
                     + " targetPercent: " + targetPercent +
                     " normalizedDamperPos: " + normalizedPrimaryDamperPos
            );
    
        }
        return normalizedDamperPosMap;
    }
    
    private double getMaxDamperPos(ArrayList<HashMap<Object, Object>> dabEquips) {
        double maxDamperPos = 0;
        for (HashMap<Object, Object> dabEquip : dabEquips) {
            if (isZoneDead(new Equip.Builder().setHashMap(dabEquip).build())) {
                continue;
            }
            HashMap<Object, Object> damper = CCUHsApi.getInstance()
                                     .readEntity("point and damper and base and primary and cmd and equipRef == \"" +
                                                 dabEquip.get("id").toString() + "\""
            );
            
            double damperPos = CCUHsApi.getInstance().readHisValById(damper.get("id").toString());
            if (damperPos >= maxDamperPos) {
                maxDamperPos = damperPos;
            }
    
            damper = CCUHsApi.getInstance()
                             .readEntity("point and damper and base and secondary and cmd and equipRef == \"" +
                                         dabEquip.get("id").toString() + "\""
            );
    
            damperPos = CCUHsApi.getInstance().readHisValById(damper.get("id").toString());
            if (damperPos >= maxDamperPos) {
                maxDamperPos = damperPos;
            }
        }
        return maxDamperPos;
    }
    
    /*
     * Take weighted damper opening (where weightedDamperOpening = (zone1_damper_opening*zone1_damper_size +
     * zone2_damper_opening*zone2_damper_size +..)/(zone1_damper_size + zone2_damper_size + .. )
     * If weighted damper opening is < targetCumulativeDamper  increase the dampers proportionally
     * so that weighted damper opening is at least targetCumulativeDamper
     **/
    public HashMap<String, Double> getAdjustedDamperPosMap (ArrayList<HashMap<Object, Object>> dabEquips,
                                                HashMap<String, Double> normalizedDamperPosMap,
                                                String systemEquipRef) {
    
        double cumulativeDamperTarget =
            TunerUtil.readTunerValByQuery("target and cumulative and damper", systemEquipRef);
        double weightedDamperOpening = getWeightedDamperOpening(dabEquips,normalizedDamperPosMap);
        CcuLog.d(L.TAG_CCU_SYSTEM, "weightedDamperOpening : " + weightedDamperOpening + " cumulativeDamperTarget : " +
                                   cumulativeDamperTarget);
        if (weightedDamperOpening > 0 && weightedDamperOpening < cumulativeDamperTarget) {
            
            int damperAdjPercent = getDamperAdjustmentTargetPercent(dabEquips,
                                                                     normalizedDamperPosMap,
                                                                     cumulativeDamperTarget);
    
            HashMap<String, Double> adjustedDamperPosMap = adjustDamperOpening(dabEquips,
                                                                               normalizedDamperPosMap,
                                                                               damperAdjPercent);
            
            
            weightedDamperOpening = getWeightedDamperOpening(dabEquips, normalizedDamperPosMap);
            CcuLog.d(L.TAG_CCU_SYSTEM, "weightedDamperOpening : " + weightedDamperOpening +
                                       " cumulativeDamperTarget : " + cumulativeDamperTarget +
                                        "damperAdjPercent : "+damperAdjPercent);
            return adjustedDamperPosMap;
        }
        return normalizedDamperPosMap;
    }
    
    public int getDamperAdjustmentTargetPercent(ArrayList<HashMap<Object, Object>> dabEquips,
                                                   HashMap<String, Double> damperPosMap,
                                                   double damperTargetOpening) {
        
        double currentWeightedDamperOpening = getWeightedDamperOpening(dabEquips, damperPosMap);
        
        HashMap<String, Double> adjustedDamperPosMap = adjustDamperOpening(dabEquips,
                                                                           damperPosMap,
                                                                           SystemConstants.DEFAULT_DAMPER_ADJ_INCREMENT
        );
        
        double adjustedWeightedDamperOpening = getWeightedDamperOpening(dabEquips, adjustedDamperPosMap);
        
        double damperChangeFor1Percentage = adjustedWeightedDamperOpening - currentWeightedDamperOpening;
        
        double requiredAdjustment = (damperTargetOpening - currentWeightedDamperOpening ) / damperChangeFor1Percentage;
        
        return (int)Math.ceil(requiredAdjustment);
        
    }
    
    public double getWeightedDamperOpening(ArrayList<HashMap<Object, Object>> dabEquips,
                                           HashMap<String, Double> normalizedDamperPosMap) {
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        int damperSizeSum = 0;
        int weightedDamperOpeningSum = 0;
        for (HashMap<Object, Object> dabEquip : dabEquips) {
            
            HashMap<Object, Object> damperPosPrimary = hayStack.readEntity(
                "point and damper and normalized and primary and cmd and equipRef == \"" + dabEquip.get("id").toString() +
                "\""
            );
            HashMap<Object, Object> damperSizePrimary = hayStack.readEntity(
                "point and config and damper and primary and size and equipRef == \"" + dabEquip.get("id").toString() + "\""
            );
            
            double damperPosVal = normalizedDamperPosMap.get(damperPosPrimary.get("id").toString());
            double damperSizeVal = hayStack.readDefaultValById(damperSizePrimary.get("id").toString());
            
            weightedDamperOpeningSum += (damperPosVal * damperSizeVal);
            damperSizeSum += damperSizeVal;
            
            HashMap<Object, Object> damperPosSecondary = hayStack.readEntity(
                "point and damper and normalized and secondary and cmd and equipRef == \""
                                                        + dabEquip.get("id").toString() + "\""
            );
            HashMap<Object, Object> damperSizeSecondary = hayStack.readEntity(
                "point and config and damper and secondary and size and equipRef == \""
                                                         + dabEquip.get("id").toString() + "\""
            );
            
            damperPosVal = normalizedDamperPosMap.get(damperPosSecondary.get("id").toString());
            damperSizeVal = hayStack.readDefaultValById(damperSizeSecondary.get("id").toString());
            
            weightedDamperOpeningSum += (damperPosVal * damperSizeVal);
            damperSizeSum += damperSizeVal;
            
        }
        return damperSizeSum == 0 ? 0 : (double) weightedDamperOpeningSum / damperSizeSum;
    }
    
    public HashMap<String, Double> adjustDamperOpening(ArrayList<HashMap<Object, Object>> dabEquips,
                                    HashMap<String, Double> normalizedDamperPosMap,
                                    int percent) {
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<String, Double> adjustedDamperOpeningMap = new HashMap<>();
        for (HashMap dabEquip : dabEquips) {
            
            HashMap<Object, Object> damperPosPrimary = hayStack.readEntity(
                "point and damper and normalized and primary and cmd and equipRef == \""
                                                                            + dabEquip.get("id").toString() + "\""
             );
            double primaryDamperVal = normalizedDamperPosMap.get(damperPosPrimary.get("id").toString());
            double adjustedDamperPos = primaryDamperVal + (primaryDamperVal * percent) / 100;
            adjustedDamperPos = Math.min(adjustedDamperPos, SystemConstants.DAMPER_POSITION_MAX);
            
            if (isZoneDead(new Equip.Builder().setHashMap(dabEquip).build())) {
                Log.d("CCU_SYSTEM", "Skip Cumulative damper adjustment, Equip Dead " + dabEquip.toString());
                adjustedDamperOpeningMap.put(damperPosPrimary.get("id").toString(), primaryDamperVal);
            } else {
                adjustedDamperOpeningMap.put(damperPosPrimary.get("id").toString(), adjustedDamperPos);
            }
            
            HashMap<Object, Object> damperPosSecondary = hayStack.readEntity(
                "point and damper and normalized and secondary and cmd and equipRef == \""
                                                                             + dabEquip.get("id").toString() + "\""
            );
            double secondaryDamperVal = normalizedDamperPosMap.get(damperPosSecondary.get("id").toString());
            adjustedDamperPos = secondaryDamperVal + (secondaryDamperVal * percent) / 100;
            adjustedDamperPos = Math.min(adjustedDamperPos, SystemConstants.DAMPER_POSITION_MAX);
            
            if (isZoneDead(new Equip.Builder().setHashMap(dabEquip).build())) {
                Log.d("CCU_SYSTEM", "Skip Cumulative damper adjustment, Equip Dead " + dabEquip.toString());
                adjustedDamperOpeningMap.put(damperPosSecondary.get("id").toString(), secondaryDamperVal);
            } else {
                adjustedDamperOpeningMap.put(damperPosSecondary.get("id").toString(), adjustedDamperPos);
            }
        }
        return adjustedDamperOpeningMap;
    }
    
    public void applyLimitsAndSetDamperPosition(ArrayList<HashMap<Object, Object>> dabEquips,
                                                HashMap<String, Double> normalizedDamperPosMap) {
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        for (HashMap<Object, Object> dabEquip : dabEquips) {
            
            HashMap<Object, Object> primaryDamperPosPoint = hayStack.readEntity(
                                                "point and damper and normalized and primary and cmd "
                                                + "and equipRef == \"" + dabEquip.get("id").toString() + "\""
            );
            
            double limitedPrimaryDamperPos = normalizedDamperPosMap.get(primaryDamperPosPoint.get("id").toString());
            
            HashMap<Object, Object> secondoryDamperPosPoint = hayStack.readEntity(
                                                "point and damper and normalized and secondary and cmd " +
                                                "and equipRef == \"" + dabEquip.get("id").toString() + "\""
            );
            
            double limitedSecondaryDamperPos = normalizedDamperPosMap.get(secondoryDamperPosPoint.get("id").toString());
            
            double minLimit = 0, maxLimit = 0;
            if (getStatus(dabEquip.get("group").toString()) == ZoneState.COOLING.ordinal()) {
                
                minLimit = hayStack.readDefaultVal(
                    "point and min and damper and cooling and equipRef == \"" + dabEquip.get("id").toString() + "\""
                );
                maxLimit = hayStack.readDefaultVal(
                    "point and max and damper and cooling and equipRef == \"" + dabEquip.get("id").toString() + "\""
                );
                
            } else if (getStatus(dabEquip.get("group").toString()) == ZoneState.HEATING.ordinal()
                       || getStatus(dabEquip.get("group").toString()) == ZoneState.DEADBAND.ordinal()
                       || getStatus(dabEquip.get("group").toString()) == ZoneState.TEMPDEAD.ordinal()){
                
                minLimit = hayStack.readDefaultVal(
                    "point and min and damper and heating and equipRef == \"" + dabEquip.get("id").toString() + "\""
                );
                maxLimit = hayStack.readDefaultVal(
                    "point and max and damper and heating and equipRef == \"" + dabEquip.get("id").toString() + "\""
                );
                
            }
            CcuLog.d(L.TAG_CCU_SYSTEM,
                     "setDamperLimits : Equip " + dabEquip.get("dis") + " minLimit " + minLimit + " maxLimit " + maxLimit);
            
            limitedPrimaryDamperPos = Math.min(limitedPrimaryDamperPos, maxLimit);
            limitedPrimaryDamperPos = Math.max(limitedPrimaryDamperPos, minLimit);
            double curPrimaryNormalisedDamper = hayStack.readHisValById(primaryDamperPosPoint.get("id").toString());
            if (limitedPrimaryDamperPos != curPrimaryNormalisedDamper) {
                hayStack.writeHisValById(primaryDamperPosPoint.get("id").toString(), limitedPrimaryDamperPos);
            }
            
            limitedSecondaryDamperPos = Math.min(limitedSecondaryDamperPos, maxLimit);
            limitedSecondaryDamperPos = Math.max(limitedSecondaryDamperPos, minLimit);
            double curSecondaryNormalisedDamper = hayStack.readHisValById(secondoryDamperPosPoint.get("id").toString());
            if (limitedSecondaryDamperPos != curSecondaryNormalisedDamper) {
                hayStack.writeHisValById(secondoryDamperPosPoint.get("id").toString(), limitedSecondaryDamperPos);
            }
            
            CcuLog.d(L.TAG_CCU_SYSTEM,
                     " limitedPrimaryDamperPos : " + limitedPrimaryDamperPos + ",  " +
                     "limitedSecondaryDamperPos : " + limitedSecondaryDamperPos
            );
        }
    }
    
    public double getStatus(String nodeAddr) {
        return CCUHsApi.getInstance().readHisValByQuery("point and status and his and group == \""+nodeAddr+"\"");
    }
    
    @Override
    public void reset(){
        weightedAverageChangeOverLoadQueue.clear();
        piController.reset();
        heatingSignal = 0;
        coolingSignal = 0;
    }
    
    public void resetLoop() {
        piController.reset();
        heatingSignal = 0;
        coolingSignal = 0;
    }
}

