package a75f.io.logic.bo.building.system.dab;

import static a75f.io.domain.api.DomainName.averageHumidity;
import static a75f.io.domain.api.DomainName.averageTemperature;
import static a75f.io.domain.api.DomainName.systemCI;
import static a75f.io.logic.bo.building.system.SystemController.EffectiveSatConditioning.SAT_COOLING;
import static a75f.io.logic.bo.building.system.SystemController.EffectiveSatConditioning.SAT_HEATING;
import static a75f.io.logic.bo.building.system.SystemController.EffectiveSatConditioning.SAT_OFF;
import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.bo.building.system.SystemController.State.OFF;
import static a75f.io.logic.bo.building.system.SystemMode.AUTO;
import static a75f.io.logic.bo.building.system.SystemMode.COOLONLY;
import static a75f.io.logic.bo.building.system.SystemMode.HEATONLY;
import static a75f.io.logic.bo.building.truecfm.TrueCFMUtil.getDamperSizeFromEnum;

import com.google.common.collect.EvictingQueue;

import org.projecthaystack.UnknownRecException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.dab.DabProfile;
import a75f.io.logic.bo.building.dualduct.DualDuctProfile;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemConstants;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.SystemPILoopController;
import a75f.io.logic.bo.building.truecfm.DabTrueCfmHandler;
import a75f.io.logic.bo.building.truecfm.TrueCFMUtil;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.bo.util.SystemScheduleUtil;
import a75f.io.logic.bo.util.SystemTemperatureUtil;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Created by samjithsadasivan on 1/9/19.
 */

public class DabSystemController extends SystemController
{
    private static final int MOVING_AVERAGE_QUEUE_SIZE = 15;
    int    integralMaxTimeout = 15;
    int proportionalSpread = 2;
    double proportionalGain = 0.5;
    double integralGain = 0.5;
    
    private static final DabSystemController instance = new DabSystemController();
    
    SystemPILoopController piController;
    
    int coolingSignal;
    int heatingSignal;
    
    EvictingQueue<Double> weightedAverageChangeOverLoadQueue = EvictingQueue.create(MOVING_AVERAGE_QUEUE_SIZE);
    
    
    int ciDesired;
    int comfortIndex = 0;
    
    
    double totalCoolingLoad = 0;
    double totalHeatingLoad = 0;
    int zoneCount = 0;
    
    double weightedAverageChangeOverLoadMA;
    double weightedAverageCoolingLoadPostML ;
    double weightedAverageHeatingLoadPostML  ;
    
    double averageSystemHumidity = 0;
    double averageSystemTemperature = 0;
    
    double co2LoopOpWeightedAverage = 0;
    double co2WeightedAverage = 0;

    SystemMode conditioningMode = SystemMode.OFF;
    private DabSystemProfile systemProfile = null;
    double prioritySum = 0;
    double co2LoopWeightedAverageASum = 0;
    double co2WeightedAverageSum = 0;
    int zoneDeadCount = 0;
    boolean hasTi = false;
    
    double weightedAverageChangeoverLoadSum = 0;
    double weightedAverageChangeoverLoad = 0;
    
    private double weightedAverageCoolingLoadSum = 0;
    private double weightedAverageHeatingLoadSum = 0;
    
    
    Occupancy currSystemOccupancy = Occupancy.UNOCCUPIED;

    private boolean pendingTunerChange;
    public boolean hasPendingTunerChange() { return pendingTunerChange; }
    public void setPendingTunerChange() { pendingTunerChange = true; }
    
    private DabSystemController()
    {
        pendingTunerChange = false;

        proportionalGain =  TunerUtil.readTunerValByQuery("system and dab and pgain");
        integralGain = TunerUtil.readTunerValByQuery("system and dab and igain");
        proportionalSpread = (int)TunerUtil.readTunerValByQuery("system and dab and pspread");
        integralMaxTimeout = (int)TunerUtil.readTunerValByQuery("system and dab and itimeout");
    
        CcuLog.i(L.TAG_CCU_SYSTEM, "proportionalGain "+proportionalGain+" integralGain "+integralGain
                                   +" proportionalSpread "+proportionalSpread+" integralMaxTimeout "+integralMaxTimeout);
        
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
                                                           .readAllEntities("equip and zone and (dab or dualDuct or " +
                                                                            "ti or otn)");
        updateDeadZones(allEquips);
        updateSystemTempHumidity(allEquips);

        processZoneEquips(allEquips);
        processCCUAsZoneEquip();
        
        if (prioritySum == 0 || zoneCount == 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "No valid temperature, Skip DabSystemControlAlgo");
            systemState = OFF;
            reset();
            return;
        }else if(systemState == OFF) {
            //Initialize System State
            if (conditioningMode == AUTO || conditioningMode == COOLONLY) {
                systemState = COOLING;
            } else if (conditioningMode == HEATONLY) {
                systemState = HEATING;
            }
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
        updateWeightedAverageConditioningLoad();
        writeAlgoVariablesToDb();
        updateLoopOpSignals();
        
        logAlgoVariables();
        updateSatConditioning(CCUHsApi.getInstance());
        updateDabZoneDamperPositions();
        
    }

    private void updateDeadZones(ArrayList<HashMap<Object, Object>> allEquips) {
        deadZones.clear();
        allEquips.forEach( equip -> {
            String equipId = equip.get(Tags.ID).toString();
            if (isZoneDead(equipId) || isRFDead(equipId)) {
                deadZones.add(equipId);
            }
        });
    }

    private void initializeAlgoLoopVariables() {

        systemProfile = (DabSystemProfile) L.ccu().systemProfile;
        conditioningMode = SystemMode.values()[(int)systemProfile.getUserIntentVal("conditioning and mode")];
        CcuLog.d(L.TAG_CCU_SYSTEM, "runDabSystemControlAlgo ->  conditioningMode: " + conditioningMode);
    
        weightedAverageCoolingLoadSum = 0;
        weightedAverageHeatingLoadSum = 0;
        weightedAverageCoolingLoadPostML = 0;
        weightedAverageHeatingLoadPostML = 0;
        
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

        if (hasPendingTunerChange()) refreshPITuners();

        Occupancy occupancy = ScheduleManager.getInstance().getSystemOccupancy();
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

    private void refreshPITuners() {
        proportionalGain =  TunerUtil.readTunerValByQuery("system and dab and pgain");
        integralGain = TunerUtil.readTunerValByQuery("system and dab and igain");
        proportionalSpread = (int)TunerUtil.readTunerValByQuery("system and dab and pspread");
        integralMaxTimeout = (int)TunerUtil.readTunerValByQuery("system and dab and itimeout");

        CcuLog.i(L.TAG_CCU_SYSTEM, "proportionalGain "+proportionalGain+" integralGain "+integralGain
                +" proportionalSpread "+proportionalSpread+" integralMaxTimeout "+integralMaxTimeout);

        piController.setIntegralGain(integralGain);
        piController.setProportionalGain(proportionalGain);
        piController.setMaxAllowedError(proportionalSpread);
        piController.setIntegralMaxTimeout(integralMaxTimeout);

        pendingTunerChange = false;
    }

    private void updateSystemTempHumidity(ArrayList<HashMap<Object, Object>> allEquips) {

        updateSystemHumidity(allEquips);
        updateSystemTemperature(allEquips);
        updateSystemDesiredTemp();
        if (L.ccu().systemProfile instanceof  DabExternalAhu ||
                (L.ccu().systemProfile instanceof DabStagedRtu && !(L.ccu().systemProfile instanceof DabAdvancedHybridRtu)) ||
                L.ccu().systemProfile instanceof DabStagedRtuWithVfd ||
                L.ccu().systemProfile instanceof DabAdvancedAhu) {
            CCUHsApi.getInstance().writeHisValByQuery("domainName == \""+averageHumidity+"\"", averageSystemHumidity);
            CCUHsApi.getInstance().writeHisValByQuery("domainName == \""+averageTemperature+"\"", averageSystemTemperature);
        } else {
            systemProfile.setSystemPoint("average and humidity", averageSystemHumidity);
            systemProfile.setSystemPoint("average and temp", averageSystemTemperature);
        }
    }
    
    private boolean isEmergencyCoolingRequired() {
        return systemState != HEATING &&
               (buildingLimitMaxBreached("dab") || buildingLimitMaxBreached("dualDuct")
                || buildingLimitMaxBreached("otn") || buildingLimitMaxBreached("ti")) &&
               conditioningMode != SystemMode.OFF;
    }
    
    private boolean isEmergencyHeatingRequired() {
        return systemState != COOLING &&
               (buildingLimitMinBreached("dab") || buildingLimitMinBreached("dualDuct")
                       || buildingLimitMinBreached("otn") || buildingLimitMinBreached("ti")) &&
               conditioningMode != SystemMode.OFF;
    }

    private void processZoneEquips(ArrayList<HashMap<Object, Object>> allEquips) {

        for (HashMap<Object, Object> equipMap : allEquips) {

            Equip equip = new Equip.Builder().setHashMap(equipMap).build();
            hasTi = hasTi || equip.getMarkers().contains("ti") || equip.getMarkers().contains("otn");

            if (deadZones.contains(equip.getId())) {
                zoneDeadCount++;
            } else if (hasTemp(equip)) {
                double zoneCurTemp = getEquipCurrentTemp(equip.getId());
                double desiredTempCooling = SystemTemperatureUtil.getDesiredTempCooling(equip.getId());
                double desiredTempHeating = SystemTemperatureUtil.getDesiredTempHeating(equip.getId());
                
                double tempMidPoint = (desiredTempCooling + desiredTempHeating)/2;
                double zoneCoolingLoad = zoneCurTemp > tempMidPoint ? zoneCurTemp - desiredTempCooling :
                                                                      tempMidPoint - desiredTempCooling;
                double zoneHeatingLoad = zoneCurTemp < tempMidPoint ? desiredTempHeating - zoneCurTemp :
                                                                      desiredTempHeating - tempMidPoint;
                double zoneLoad = Math.max(zoneCoolingLoad, zoneHeatingLoad);
                double zoneDynamicPriority = getEquipDynamicPriority(Math.max(zoneLoad, 0), equip.getId());
                totalCoolingLoad += Math.max(zoneCoolingLoad, 0);
                totalHeatingLoad += Math.max(zoneHeatingLoad, 0);
                zoneCount++;
                
                weightedAverageChangeoverLoadSum += (zoneCurTemp - tempMidPoint) * zoneDynamicPriority;
    
                weightedAverageCoolingLoadSum += zoneCoolingLoad * zoneDynamicPriority;
                weightedAverageHeatingLoadSum += zoneHeatingLoad * zoneDynamicPriority;
                
                prioritySum += zoneDynamicPriority;
                co2LoopWeightedAverageASum += (getEquipCo2LoopOp(equip.getId()) * zoneDynamicPriority);
                co2WeightedAverageSum += (getEquipCo2(equip.getId()) * zoneDynamicPriority);
    
                CcuLog.d(L.TAG_CCU_SYSTEM, equip.getDisplayName() + " zoneDynamicPriority: " + zoneDynamicPriority +
                                           " zoneCoolingLoad: " + zoneCoolingLoad + " zoneHeatingLoad: " + zoneHeatingLoad
                );
                CcuLog.d(L.TAG_CCU_SYSTEM,
                         equip.getDisplayName() + " weightedAverageCoolingLoadSum: " + weightedAverageCoolingLoadSum +
                         " weightedAverageHeatingLoadSum "+weightedAverageHeatingLoadSum +
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
            CcuLog.d(L.TAG_CCU_SYSTEM, "DabSysController = "+hasTi+","+zoneDeadCount+"," +zoneCount+","
                    +cmTempInfForPercentileZonesDead);

        if( (zoneCount == 0) || (!hasTi && ((zoneDeadCount > 0)
                   && (((double)(zoneDeadCount*100)/(zoneDeadCount + zoneCount)) >= cmTempInfForPercentileZonesDead)))){

            String sysEquip = L.ccu().systemProfile.getSystemEquipRef();
            if(sysEquip != null) {
                double cmCurrentTemp = getCMCurrentTemp(sysEquip);
                if(isCMTempDead(cmCurrentTemp)) {
                    double desiredTempCooling = ScheduleManager.getInstance().getSystemCoolingDesiredTemp();
                    double desiredTempHeating = ScheduleManager.getInstance().getSystemHeatingDesiredTemp();
                    double tempMidPoint = (desiredTempCooling + desiredTempHeating)/2;
                    
                    double cmCoolingLoad = cmCurrentTemp > tempMidPoint ? cmCurrentTemp - desiredTempCooling :
                                                                          tempMidPoint - desiredTempCooling;
                    double cmHeatingLoad = cmCurrentTemp < tempMidPoint ? desiredTempHeating - cmCurrentTemp :
                                                                          desiredTempHeating - tempMidPoint;

                    double zoneLoad = Math.max(cmCoolingLoad, cmHeatingLoad);
                    double zoneDynamicPriority = getCMDynamicPriority(Math.max(zoneLoad, 0));
                    
                    totalCoolingLoad += Math.max(cmCoolingLoad, 0);
                    totalHeatingLoad += Math.max(cmHeatingLoad, 0);
                    zoneCount++;
                    
                    weightedAverageChangeoverLoadSum += (cmCurrentTemp - tempMidPoint) * zoneDynamicPriority;
    
                    weightedAverageCoolingLoadSum = cmCoolingLoad * zoneDynamicPriority;
                    weightedAverageHeatingLoadSum = cmHeatingLoad * zoneDynamicPriority;
                    
                    prioritySum += zoneDynamicPriority;
                    
                    CcuLog.d(L.TAG_CCU_SYSTEM, "CM dab zoneDynamicPriority: " + zoneDynamicPriority +
                                               " cmCoolingLoad: " + cmCoolingLoad +
                                               " cmHeatingLoad: " + cmHeatingLoad +
                                               " weightedAverageCoolingLoadSum " + weightedAverageCoolingLoadSum +
                                               " weightedAverageHeatingLoadSum " + weightedAverageHeatingLoadSum +
                                               "," + prioritySum + "," + cmCurrentTemp+
                                               " weightedAverageChangeoverLoadSum "+weightedAverageChangeoverLoadSum
                    );
                }
            }
        }
    }
    
    private void updateWeightedAverageLoad() {
        
        co2LoopOpWeightedAverage = co2LoopWeightedAverageASum/prioritySum;
        co2WeightedAverage = co2WeightedAverageSum/prioritySum;
        
        comfortIndex = (int)(totalCoolingLoad + totalHeatingLoad) /zoneCount;
        
        weightedAverageChangeoverLoad = weightedAverageChangeoverLoadSum/prioritySum;
        
        weightedAverageChangeOverLoadQueue.add(weightedAverageChangeoverLoad);
        
        double weightedAverageChangeOverLoadQueueSum = 0;
        for (double val : weightedAverageChangeOverLoadQueue) {
            weightedAverageChangeOverLoadQueueSum += val;
        }
        weightedAverageChangeOverLoadMA = weightedAverageChangeOverLoadQueueSum/weightedAverageChangeOverLoadQueue.size();
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
        if ((conditioningMode == COOLONLY || conditioningMode == AUTO)
                                && weightedAverageChangeOverLoadMA > modeChangeoverHysteresis) {
            if (systemState != COOLING) {
                systemState = COOLING;
                piController.reset();
            }
        } else if ((conditioningMode == HEATONLY || conditioningMode == AUTO)
                                && weightedAverageChangeOverLoadMA < (-1 * modeChangeoverHysteresis)) {
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
    
    private void updateWeightedAverageConditioningLoad() {
        
        if (systemState == COOLING) {
            weightedAverageCoolingLoadPostML = weightedAverageCoolingLoadSum/prioritySum;
        } else {
            weightedAverageCoolingLoadPostML = 0;
        }
    
        if (systemState == HEATING) {
            weightedAverageHeatingLoadPostML = weightedAverageHeatingLoadSum/prioritySum;
        } else {
            weightedAverageHeatingLoadPostML = 0;
        }
    }
    
    private void writeAlgoVariablesToDb() {

        if (L.ccu().systemProfile instanceof DabExternalAhu ||
                (L.ccu().systemProfile instanceof DabStagedRtu && !(L.ccu().systemProfile instanceof DabAdvancedHybridRtu)) ||
                L.ccu().systemProfile instanceof DabStagedRtuWithVfd
        ) {
            Domain.writeHisValByDomain(systemCI, comfortIndex);
            Domain.writeHisValByDomain(DomainName.weightedAverageLoadMA, weightedAverageChangeOverLoadMA);
            Domain.writeHisValByDomain(DomainName.weightedAverageCoolingLoadPostML, weightedAverageCoolingLoadPostML);
            Domain.writeHisValByDomain(DomainName.weightedAverageHeatingLoadPostML, weightedAverageHeatingLoadPostML);
        } else {
            systemProfile.setSystemPoint("ci and running", comfortIndex);

            systemProfile.setSystemPoint("operating and mode", systemState.ordinal());

            systemProfile.setSystemPoint("weighted and average and moving and load",
                    CCUUtils.roundToTwoDecimal(weightedAverageChangeOverLoadMA));
            systemProfile.setSystemPoint("weighted and average and cooling and load",
                    CCUUtils.roundToTwoDecimal(weightedAverageCoolingLoadPostML));
            systemProfile.setSystemPoint("weighted and average and heating and load",
                    CCUUtils.roundToTwoDecimal(weightedAverageHeatingLoadPostML));
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
    
    private void logAlgoVariables() {
        
        CcuLog.d(L.TAG_CCU_SYSTEM, "weightedAverageChangeOverLoadMA "+weightedAverageChangeOverLoadMA+
                                   " co2LoopOpWA "+co2LoopOpWeightedAverage+" " +
                                   "systemState: "+systemState
        );

        CcuLog.d(L.TAG_CCU_SYSTEM, " weightedAverageCoolingLoadPostML: " + weightedAverageCoolingLoadPostML +
                                   " weightedAverageHeatingLoadPostML: " + weightedAverageHeatingLoadPostML +
                                   " coolingSignal: " + coolingSignal + " heatingSignal: " + heatingSignal
        );

    }
    
    private boolean isNormalizationRequired() {
        
        if (conditioningMode == SystemMode.OFF) {
            return false;
        }
        //This is not neat, but the only way to check current system status for all different types of DAB profiles.
        String status = CCUHsApi.getInstance().readDefaultStrVal("system and status and message");
        return !status.equals("System OFF");
    }
    
    private HashMap<String, Double> getBaseDamperPosMap(ArrayList<HashMap<Object, Object>> dabEquips) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<String, Double> damperPosMap = new HashMap<>();
        for (HashMap<Object, Object> dabEquip : dabEquips) {
            HashMap<Object, Object> primaryDamper = hayStack.readEntity("point and domainName == \"" + DomainName.damper1Cmd + "\" and equipRef == \""+dabEquip.get("id").toString()+"\"");
            double primaryDamperPos = hayStack.readHisValById(primaryDamper.get("id").toString());

            HashMap<Object, Object> secondaryDamper = hayStack.readEntity("point and domainName == \"" + DomainName.damper2Cmd + "\" and equipRef == \""+dabEquip.get("id").toString()+"\"");
            double secondaryDamperPos = hayStack.readHisValById(secondaryDamper.get("id").toString());

            HashMap<Object, Object> normalizedPrimaryDamper = hayStack.readEntity("point and domainName == \"" + DomainName.normalizedDamper1Cmd + "\" and equipRef == \""+dabEquip.get("id").toString()+"\"");
            HashMap<Object, Object> normalizedSecondaryDamper = hayStack.readEntity("point and domainName == \"" + DomainName.normalizedDamper2Cmd + "\" and equipRef == \""+dabEquip.get("id").toString()+"\"");

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
        return CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and  (current or space) and equipRef == \"" +equipRef+"\""
        );
    }
    public double getCMCurrentTemp(String equipRef) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and temp and cm and (current or space) and equipRef == \""
                                                        +equipRef+"\""
        );
    }
    
    public ZonePriority getEquipPriority(String equipRef) {
        double priorityVal = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and config and priority and equipRef == \""
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
    
        double equipDynamicPriority;
        
        if (SystemScheduleUtil.isZoneForcedOccupied(equipRef)) {
            equipDynamicPriority = SystemConstants.FORCED_OCCUPIED_ZONE_PRIORITY_VAL;
        } else {
            double zonePrioritySpread = TunerUtil.readTunerValByQuery(
                "point and tuner and zone and priority and spread and equipRef == \"" + equipRef + "\"");
            double zonePriorityMultiplier = TunerUtil.readTunerValByQuery(
                "point and tuner and zone and priority and multiplier and equipRef == \"" + equipRef + "\"");
            equipDynamicPriority = p.val * Math.pow(zonePriorityMultiplier, (zoneLoad / zonePrioritySpread) > 10 ? 10
                                                                                : (zoneLoad / zonePrioritySpread));
            equipDynamicPriority = CCUUtils.roundToTwoDecimal(equipDynamicPriority);
        }
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
        return CCUHsApi.getInstance().readHisValByQuery("point and air and co2 and sensor and (current or space) and equipRef == \"" +equipRef+"\""
        );
    }
    
    public void updateSystemHumidity(ArrayList<HashMap<Object, Object>> allEquips) {
        //Average across zones or from proxy zone.
        double humiditySum = 0;
        double humidityZones = 0;
        
        for (HashMap<Object, Object> equip : allEquips)
        {
            double humidityVal = CCUHsApi.getInstance().readHisValByQuery("point and air and humidity and sensor and (current or space) " +
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

            double desiredTempCooling = ScheduleManager.getInstance().getSystemCoolingDesiredTemp();
            double desiredTempHeating = ScheduleManager.getInstance().getSystemHeatingDesiredTemp();

            String coolingPointId = CCUHsApi.getInstance().readId("point and system and cm and cooling" +
                    " and desired and temp and equipRef == \"" +
                    L.ccu().systemProfile.getSystemEquipRef() + "\"");

            String heatingPointId = CCUHsApi.getInstance().readId("point and system and cm and heating" +
                    " and desired and temp and equipRef == \"" +
                    L.ccu().systemProfile.getSystemEquipRef() + "\"");

            CCUHsApi.getInstance().writeHisValById(coolingPointId, desiredTempCooling);
            CCUHsApi.getInstance().writeDefaultValById(coolingPointId, desiredTempCooling);


            CCUHsApi.getInstance().writeHisValById(heatingPointId, desiredTempHeating);
            CCUHsApi.getInstance().writeDefaultValById(heatingPointId, desiredTempHeating);

        }catch (NullPointerException | UnknownRecException e){
            CcuLog.e(L.TAG_CCU_SYSTEM, "Id for desired temp not found in haystack");
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
                    equip.getMarkers().contains("otn") || equip.getMarkers().contains("ti")) {
                double tempVal = CCUHsApi.getInstance().readHisValByQuery(
                        "point and air and temp and sensor and (current or space) and equipRef == \"" + equipMap.get("id") + "\""
                );
                hasTi = hasTi || equip.getMarkers().contains("ti") || equip.getMarkers().contains("otn");
                if (!deadZones.contains(equip.getId()) && (tempVal > 0)) {
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
            CcuLog.d(L.TAG_CCU_SYSTEM, "DabSysController, cmTemp "+cmTemp+" tempZone "+tempZones);
        }
        averageSystemTemperature = tempZones == 0 ? 0 : tempSum/tempZones;
        averageSystemTemperature =CCUUtils.roundToOneDecimal(averageSystemTemperature);
        
        CcuLog.d(L.TAG_CCU_SYSTEM, "averageSystemTemperature "+averageSystemTemperature+" tempZone "+tempZones
                                                                +" tempSum "+tempSum);
    }
    public boolean isCMTempDead(double cmTemp) {

        double buildingLimitMax =  BuildingTunerCache.getInstance().getBuildingLimitMax();
        double buildingLimitMin =  BuildingTunerCache.getInstance().getBuildingLimitMin();

        double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();
        return !(cmTemp > (buildingLimitMax + tempDeadLeeway)) && !(cmTemp < (buildingLimitMin - tempDeadLeeway));
    }

    public boolean hasTemp(Equip q) {
        try
        {
            return CCUHsApi.getInstance().readHisValByQuery("point and (current or space) and temp and sensor and equipRef == \"" + q.getId()
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
    
    private void updateDabZoneDamperPositions() {
        ArrayList<HashMap<Object, Object>> dabEquips = CCUHsApi.getInstance()
                                                               .readAllEntities("equip and zone and dab");
    
        if (isNormalizationRequired()) {
            HashMap<String, Double> normalizedDamperPosMap = getNormalizedDamperPosMap(dabEquips,
                                                                                       getBaseDamperPosMap(dabEquips));
        
            HashMap<String, Double> cumulativeDamperAdjustedPosMap = getAdjustedDamperPosMap(dabEquips,
                                                                                             normalizedDamperPosMap,
                                                                                             systemProfile.getSystemEquipRef());
        
            HashMap<String, Double> cfmUpdatedDamperPosMap = DabTrueCfmHandler.getInstance()
                                                                              .getCfMUpdatedDamperPosMap(dabEquips,
                                                                                                         cumulativeDamperAdjustedPosMap,
                                                                                                         CCUHsApi.getInstance());
        
            applyLimitsAndSetDamperPosition(dabEquips, cfmUpdatedDamperPosMap);
        } else if (conditioningMode != SystemMode.OFF) {
            HashMap<String, Double> cfmUpdatedDamperPosMap = DabTrueCfmHandler.getInstance()
                                                                              .getCfMUpdatedDamperPosMap(dabEquips,
                                                                                                         getBaseDamperPosMap(dabEquips),
                                                                                                         CCUHsApi.getInstance());
            applyLimitsAndSetDamperPosition(dabEquips, cfmUpdatedDamperPosMap);
        }
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
            HashMap<Object, Object> normalizedPrimaryDamper = hayStack.readEntity("point and domainName == \"" + DomainName.normalizedDamper1Cmd + "\"  and equipRef == \""+dabEquip.get("id").toString()+"\"");
            HashMap<Object, Object> normalizedSecondaryamper = hayStack.readEntity("point and domainName == \"" + DomainName.normalizedDamper2Cmd + "\" and equipRef == \""+dabEquip.get("id").toString()+"\"");
            double primaryDamperPos;
            double secondaryDamperPos;
            double normalizedPrimaryDamperPos;
            double normalizedSecondaryDamperPos;
    
            primaryDamperPos = damperPosMap.get(normalizedPrimaryDamper.get("id").toString());
            secondaryDamperPos = damperPosMap.get(normalizedSecondaryamper.get("id").toString());
            if (deadZones.contains(dabEquip.get("id").toString())) {
                CcuLog.d(L.TAG_CCU_SYSTEM, "Skip Normalize, Equip Dead " + dabEquip);
                normalizedPrimaryDamperPos = primaryDamperPos;
                normalizedSecondaryDamperPos = secondaryDamperPos;
            } else {
                normalizedPrimaryDamperPos = (primaryDamperPos + primaryDamperPos * targetPercent / 100);
                normalizedSecondaryDamperPos = (secondaryDamperPos + secondaryDamperPos * targetPercent / 100);
            }
            
            normalizedDamperPosMap.put(Objects.requireNonNull(normalizedPrimaryDamper.get("id")).toString(), normalizedPrimaryDamperPos);
            normalizedDamperPosMap.put(normalizedSecondaryamper.get("id").toString(), normalizedSecondaryDamperPos);
    
            CcuLog.d(L.TAG_CCU_SYSTEM,
                     "normalizeAirflow" + " Equip: " +
                     dabEquip.get("dis") + " ,damperPos :" + primaryDamperPos
                     + " targetPercent: " + targetPercent +
                     " normalizedPrimaryDamperPos: " + normalizedPrimaryDamperPos+
                     " normalizedSecondaryDamperPos: "+normalizedSecondaryDamperPos
            );
    
        }
        return normalizedDamperPosMap;
    }
    
    private double getMaxDamperPos(ArrayList<HashMap<Object, Object>> dabEquips) {
        double maxDamperPos = 0;
        for (HashMap<Object, Object> dabEquip : dabEquips) {
            if (deadZones.contains(dabEquip.get("id").toString())) {
                continue;
            }
            HashMap<Object, Object> damper = CCUHsApi.getInstance().readEntity("point and domainName == \"" + DomainName.damper1Cmd + "\" " +
                    "and equipRef == \""+Objects.requireNonNull(dabEquip.get("id"))+"\"");
            
            double damperPos = CCUHsApi.getInstance().readHisValById(Objects.requireNonNull(damper.get("id")).toString());
            if (damperPos >= maxDamperPos) {
                maxDamperPos = damperPos;
            }
    
            damper = CCUHsApi.getInstance().readEntity("point and domainName == \"" + DomainName.damper2Cmd + "\" " +
                    "and equipRef == \""+Objects.requireNonNull(dabEquip.get("id"))+"\"");
    
            damperPos = CCUHsApi.getInstance().readHisValById(Objects.requireNonNull(damper.get("id")).toString());
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

        boolean anyZoneAlive = dabEquips.stream()
                                    .filter(equip -> !deadZones.contains(equip.get("id").toString()))
                                    .collect(Collectors.toList()).size() > 0;

        CcuLog.d(L.TAG_CCU_SYSTEM, "weightedDamperOpening : " + weightedDamperOpening + " cumulativeDamperTarget : " +
                                   cumulativeDamperTarget+" anyZoneAlive "+anyZoneAlive);
        if (weightedDamperOpening > 0 && weightedDamperOpening < cumulativeDamperTarget && anyZoneAlive) {
            HashMap<String, Double> adjustedDamperPosMap = normalizedDamperPosMap;
            double adjustedWeightedDamperOpening;
            do {
                adjustedDamperPosMap = adjustDamperOpening(dabEquips,
                        adjustedDamperPosMap,
                        SystemConstants.DEFAULT_DAMPER_ADJ_INCREMENT
                );

                adjustedWeightedDamperOpening = getWeightedDamperOpening(dabEquips, adjustedDamperPosMap);
                CcuLog.d(L.TAG_CCU_SYSTEM, " adjustDamperOpening : weightedDamperOpening "+weightedDamperOpening+
                                        " adjustedWeightedDamperOpening "+adjustedWeightedDamperOpening);
                //Adjust is not yielding any results. Zones could be dead
                if (adjustedWeightedDamperOpening == weightedDamperOpening) {
                    break;
                }
                weightedDamperOpening = adjustedWeightedDamperOpening;
            } while (adjustedWeightedDamperOpening < cumulativeDamperTarget);

            return adjustedDamperPosMap;
        }
        return normalizedDamperPosMap;
    }
    
    public double getWeightedDamperOpening(ArrayList<HashMap<Object, Object>> dabEquips,
                                           HashMap<String, Double> normalizedDamperPosMap) {
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        int damperSizeSum = 0;
        int weightedDamperOpeningSum = 0;
        for (HashMap<Object, Object> dabEquip : dabEquips) {
            
            HashMap<Object, Object> damperPosPrimary = hayStack.readEntity("point and domainName == \"" + DomainName.normalizedDamper1Cmd + "\" " +
                    "and equipRef == \""+dabEquip.get("id").toString()+"\"");
            HashMap<Object, Object> damperSizePrimary = hayStack.readEntity("point and domainName == \"" + DomainName.damper1Size + "\" " +
                    "and equipRef == \""+dabEquip.get("id").toString()+"\"");
            
            double damperPosVal = normalizedDamperPosMap.get(damperPosPrimary.get("id").toString());
            double damperSizeVal = hayStack.readDefaultValById(damperSizePrimary.get("id").toString());
            
            weightedDamperOpeningSum += (damperPosVal *  getDamperSizeFromEnum((int)damperSizeVal));
            damperSizeSum += getDamperSizeFromEnum((int)damperSizeVal);
            
            HashMap<Object, Object> damperPosSecondary = hayStack.readEntity("point and domainName == \"" + DomainName.normalizedDamper2Cmd + "\"" +
                    " and equipRef == \""+dabEquip.get("id").toString()+"\"");
            HashMap<Object, Object> damperSizeSecondary = hayStack.readEntity("point and domainName == \"" + DomainName.damper2Size + "\" " +
                    "and equipRef == \""+dabEquip.get("id").toString()+"\"");
            
            damperPosVal = normalizedDamperPosMap.get(damperPosSecondary.get("id").toString());
            damperSizeVal = hayStack.readDefaultValById(damperSizeSecondary.get("id").toString());
            
            weightedDamperOpeningSum += (damperPosVal *  getDamperSizeFromEnum((int)damperSizeVal));
            damperSizeSum += getDamperSizeFromEnum((int)damperSizeVal);
            
        }
        return damperSizeSum == 0 ? 0 : CCUUtils.roundToTwoDecimal((double) weightedDamperOpeningSum / damperSizeSum);
    }
    
    public HashMap<String, Double> adjustDamperOpening(ArrayList<HashMap<Object, Object>> dabEquips,
                                    HashMap<String, Double> normalizedDamperPosMap,
                                    int percent) {
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<String, Double> adjustedDamperOpeningMap = new HashMap<>();
        for (HashMap dabEquip : dabEquips) {
            
            HashMap<Object, Object> damperPosPrimary = hayStack.readEntity("point and domainName == \"" + DomainName.normalizedDamper1Cmd + "\"" +
                    " and equipRef == \""+dabEquip.get("id").toString()+"\"");
            double primaryDamperVal = normalizedDamperPosMap.get(damperPosPrimary.get("id").toString());
            primaryDamperVal = Math.max(primaryDamperVal, MIN_DAMPER_FOR_CUMULATIVE_CALCULATION);
            double adjustedDamperPos = primaryDamperVal + (primaryDamperVal * percent) / 100;
            adjustedDamperPos = Math.min(adjustedDamperPos, SystemConstants.DAMPER_POSITION_MAX);
            
            if (deadZones.contains(dabEquip.get("id").toString())) {
                CcuLog.d(L.TAG_CCU_SYSTEM, "Skip Cumulative damper adjustment, Equip Dead " + dabEquip);
                adjustedDamperOpeningMap.put(damperPosPrimary.get("id").toString(), primaryDamperVal);
            } else {
                adjustedDamperOpeningMap.put(damperPosPrimary.get("id").toString(), adjustedDamperPos);
            }
            
            HashMap<Object, Object> damperPosSecondary = hayStack.readEntity("point and domainName == \"" + DomainName.normalizedDamper2Cmd + "\" " +
                    "and equipRef == \""+dabEquip.get("id").toString()+"\"");
            double secondaryDamperVal = normalizedDamperPosMap.get(damperPosSecondary.get("id").toString());
            secondaryDamperVal = Math.max(secondaryDamperVal, MIN_DAMPER_FOR_CUMULATIVE_CALCULATION);
            adjustedDamperPos = secondaryDamperVal + (secondaryDamperVal * percent) / 100;
            adjustedDamperPos = Math.min(adjustedDamperPos, SystemConstants.DAMPER_POSITION_MAX);
            
            if (deadZones.contains(dabEquip.get("id").toString())) {
                CcuLog.d("CCU_SYSTEM", "Skip Cumulative damper adjustment, Equip Dead " + dabEquip);
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
            String equipRef = dabEquip.get("id").toString();
            HashMap<Object, Object> primaryDamperPosPoint = hayStack.readEntity("point and domainName == \"" + DomainName.normalizedDamper1Cmd + "\" and equipRef == \""+equipRef+"\"");
            
            double limitedPrimaryDamperPos = normalizedDamperPosMap.get(primaryDamperPosPoint.get("id").toString());
            limitedPrimaryDamperPos = CCUUtils.roundToTwoDecimal(limitedPrimaryDamperPos);
            
            HashMap<Object, Object> secondoryDamperPosPoint = hayStack.readEntity("point and domainName == \"" + DomainName.normalizedDamper2Cmd + "\" and equipRef == \""+equipRef+"\"");
            
            double limitedSecondaryDamperPos = normalizedDamperPosMap.get(secondoryDamperPosPoint.get("id").toString());
            limitedSecondaryDamperPos = CCUUtils.roundToTwoDecimal(limitedSecondaryDamperPos);
            
            double minLimit, maxLimit;
            int systemConditioning = systemState.ordinal();
            int satConditioning = CCUHsApi.getInstance().readHisValByQuery("system and sat and conditioning").intValue();
            if (satConditioning > 0) {
                systemConditioning = satConditioning;
            }
            if (systemConditioning == COOLING.ordinal()) {
                minLimit = hayStack.readDefaultVal(
                    "point and min and damper and cooling and equipRef == \"" + equipRef + "\""
                );
                maxLimit = hayStack.readDefaultVal(
                    "point and max and damper and cooling and equipRef == \"" + equipRef + "\""
                );
            } else {
                minLimit = hayStack.readDefaultVal(
                    "point and min and damper and heating and equipRef == \"" + equipRef + "\""
                );
                maxLimit = hayStack.readDefaultVal(
                    "point and max and damper and heating and equipRef == \"" + equipRef + "\""
                );
            }
            CcuLog.d(L.TAG_CCU_SYSTEM,
                     "setDamperLimits : Equip " + dabEquip.get("dis") + " minLimit " + minLimit + " maxLimit " + maxLimit);


            if (hayStack.readHisValByQuery("reheat and cmd and equipRef ==\""+equipRef+"\"") > 0) {
                double reheatMinDamper = hayStack.readDefaultVal("config and reheat and min and damper and equipRef ==\""+equipRef+"\"");

                if (limitedPrimaryDamperPos < reheatMinDamper) {
                    limitedPrimaryDamperPos = reheatMinDamper;
                    CcuLog.d(L.TAG_CCU_SYSTEM, " reheatMinDamper applied to primary "+reheatMinDamper);
                }

                if (limitedSecondaryDamperPos < reheatMinDamper) {
                    limitedSecondaryDamperPos = reheatMinDamper;
                    CcuLog.d(L.TAG_CCU_SYSTEM, " reheatMinDamper applied to secondary "+reheatMinDamper);
                }
            }
            
            limitedPrimaryDamperPos = Math.min(limitedPrimaryDamperPos, maxLimit);
            limitedPrimaryDamperPos = Math.max(limitedPrimaryDamperPos, minLimit);
            hayStack.writeHisValById(primaryDamperPosPoint.get("id").toString(), limitedPrimaryDamperPos);

            limitedSecondaryDamperPos = Math.min(limitedSecondaryDamperPos, maxLimit);
            limitedSecondaryDamperPos = Math.max(limitedSecondaryDamperPos, minLimit);
            hayStack.writeHisValById(secondoryDamperPosPoint.get("id").toString(), limitedSecondaryDamperPos);

            CcuLog.d(L.TAG_CCU_SYSTEM,
                     " limitedPrimaryDamperPos : " + limitedPrimaryDamperPos + ",  " +
                     "limitedSecondaryDamperPos : " + limitedSecondaryDamperPos
            );
            
            if (TrueCFMUtil.isTrueCfmEnabled(hayStack, equipRef)) {
                CcuLog.d(L.TAG_CCU_SYSTEM, "UpdateTrueCfm moving average queue "+dabEquip.get("dis"));
                DabTrueCfmHandler.getInstance().updateDamperPosQueueMap(primaryDamperPosPoint.get("id").toString(),
                                                                        limitedPrimaryDamperPos);
                DabTrueCfmHandler.getInstance().updateAirflowMAQueue(hayStack, equipRef, Tags.PRIMARY,
                                                                     primaryDamperPosPoint.get("id").toString());
                
                DabTrueCfmHandler.getInstance().updateDamperPosQueueMap(secondoryDamperPosPoint.get("id").toString(),
                                                                        limitedSecondaryDamperPos);
                DabTrueCfmHandler.getInstance().updateAirflowMAQueue(hayStack, equipRef, Tags.SECONDARY,
                                                                     secondoryDamperPosPoint.get("id").toString());
            }
        }
    }
    
    public double getStatus(String nodeAddr) {
        return CCUHsApi.getInstance().readHisValByQuery("point and not ota and status and his and group == \""+nodeAddr+"\"");
    }
    
    @Override
    public void reset(){
        CcuLog.i(L.TAG_CCU_SYSTEM, "Reset system loop");
        weightedAverageChangeOverLoadQueue.clear();
        piController.reset();
        heatingSignal = 0;
        coolingSignal = 0;
        systemState = OFF;
    }
    
    public void resetLoop() {
        piController.reset();
        heatingSignal = 0;
        coolingSignal = 0;
    }

    private boolean isSupplyAirTempValid(double satVal) {
        return satVal > 0 && satVal < 200;
    }

    private double getSupplyAirTemp(HashMap equip, CCUHsApi hayStack) {
        double supplyAirTemp1 = hayStack.readHisValByQuery("(domainName == \"" + DomainName.dischargeAirTemp1 + "\" and equipRef == \""+equip.get("id")+"\") or supply and air and temp " +
                                        "and primary and equipRef == \""+equip.get("id")+"\"");
        double supplyAirTemp2 = hayStack.readHisValByQuery("(domainName == \"" + DomainName.dischargeAirTemp2 + "\" and equipRef == \""+equip.get("id")+"\") or supply and air and temp " +
                                        "and secondary and equipRef == \""+equip.get("id")+"\" ");

        if (!isSupplyAirTempValid(supplyAirTemp1)) {
            supplyAirTemp1 = 0;
        }
        if (!isSupplyAirTempValid(supplyAirTemp2)) {
            supplyAirTemp2 = 0;
        }

        if (supplyAirTemp1 > 0 && supplyAirTemp2 > 0) {
            return (supplyAirTemp1 + supplyAirTemp2)/2;
        } else if (supplyAirTemp1 > 0) {
            return supplyAirTemp1;
        } else if (supplyAirTemp2 > 0) {
            return supplyAirTemp2;
        }
        return 0;
    }

    private double getAverageDesiredTemp(String type, ArrayList<HashMap<Object, Object>> dabEquips, CCUHsApi hayStack) {
        double sum = dabEquips.stream()
                    .map( equip -> hayStack.readHisValByQuery("desired and temp and "+type+" and equipRef ==\""+equip.get("id")+"\""))
                    .reduce(0.0, Double::sum);

        return sum/dabEquips.size();
    }


    private void updateSatConditioning(CCUHsApi hayStack) {
        ArrayList<HashMap<Object, Object>> allEquips = CCUHsApi.getInstance().readAllEntities("equip and dab and zone");
        ArrayList<HashMap<Object, Object>> equipsWithValidSAT = new ArrayList<>();
        double satSum = 0;
        for (HashMap dabEquip : allEquips) {
            if (deadZones.contains(dabEquip.get("id").toString())) {
                continue;
            }
            double supplyAirTemp = getSupplyAirTemp(dabEquip, hayStack);
            CcuLog.i(L.TAG_CCU_SYSTEM, "supplyAirTemp for "+dabEquip.get("dis")+" "+supplyAirTemp);
            if (supplyAirTemp > 0) {
                equipsWithValidSAT.add(dabEquip);
                satSum += supplyAirTemp;
            }
        }

        if (equipsWithValidSAT.isEmpty()) {
            hayStack.writeHisValByQuery("system and sat and conditioning",(double) SAT_OFF.ordinal());
            CcuLog.i(L.TAG_CCU_SYSTEM, "updateSatConditioning aborted , no valid zones ");
            return;
        }
        double effectiveSat = satSum / equipsWithValidSAT.size();
        CcuLog.i(L.TAG_CCU_SYSTEM, "effectiveSat "+effectiveSat);

        double coolingDesiredTemp = getAverageDesiredTemp(Tags.COOLING, equipsWithValidSAT, hayStack);
        CcuLog.i(L.TAG_CCU_SYSTEM, "updateSatConditioning coolingDesiredTemp "+coolingDesiredTemp);
        if (effectiveSat < coolingDesiredTemp) {
            hayStack.writeHisValByQuery("system and sat and conditioning", (double) SAT_COOLING.ordinal());
            return;
        }
        double heatingDesiredTemp = getAverageDesiredTemp(Tags.HEATING, equipsWithValidSAT, hayStack);
        CcuLog.i(L.TAG_CCU_SYSTEM, "updateSatConditioning heatingDesiredTemp "+heatingDesiredTemp);
        if (effectiveSat > heatingDesiredTemp) {
            hayStack.writeHisValByQuery("system and sat and conditioning", (double) SAT_HEATING.ordinal());
        }
    }
}

