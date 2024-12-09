package a75f.io.logic.bo.building.system.vav;

import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemConstants;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.SystemPILoopController;
import a75f.io.logic.bo.building.system.dab.DabExternalAhu;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.bo.util.SystemScheduleUtil;
import a75f.io.logic.bo.util.SystemTemperatureUtil;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.domain.api.DomainName.averageHumidity;
import static a75f.io.domain.api.DomainName.averageTemperature;
import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.bo.building.system.SystemController.State.OFF;
import static a75f.io.logic.bo.building.system.SystemMode.AUTO;
import static a75f.io.logic.bo.building.system.SystemMode.COOLONLY;
import static a75f.io.logic.bo.building.system.SystemMode.HEATONLY;

/**
 * VavSystemController applies Weighted average and Moving average filters on temperature diffs.
 * MA value is used to determine AHU change over. WA determines the heating signal.
 *
 */
public class VavSystemController extends SystemController
{
    int    integralMaxTimeout = 15;
    int proportionalSpread = 2;
    double proportionalGain = 0.5;
    double integralGain = 0.5;
    
    private static VavSystemController instance = new VavSystemController();
    
    SystemPILoopController piController;
    
    int coolingSignal;
    int heatingSignal;
    
    EvictingQueue<Double> weightedAverageCoolingOnlyLoadMAQueue = EvictingQueue.create(15);
    EvictingQueue<Double> weightedAverageHeatingOnlyLoadMAQueue = EvictingQueue.create(15);;
    
    int comfortIndex = 0;
    
    
    double totalCoolingLoad = 0;
    double totalHeatingLoad = 0;
    int zoneCount = 0;
    
    double weightedAverageCoolingOnlyLoadSum;
    double weightedAverageHeatingOnlyLoadSum;
    double weightedAverageHeatingConditioningLoadSum;
    
    double weightedAverageCoolingOnlyLoad;
    double weightedAverageHeatingOnlyLoad;
    double weightedAverageHeatingConditioningLoad;
    
    double weightedAverageCoolingOnlyLoadMA;
    double weightedAverageHeatingOnlyLoadMA;
    
    double averageSystemHumidity = 0;
    double averageSystemTemperature = 0;
    
    double weightedAverageCoolingOnlyLoadPostML;
    double weightedAverageHeatingOnlyLoadPostML;
    double weightedAverageLoadPostML;
    
    double co2WeightedAverage = 0;

    private SystemMode conditioningMode = SystemMode.OFF;
    private VavSystemProfile systemProfile = null;
    double prioritySum = 0;
    double co2LoopWeightedAverageASum = 0;
    double co2WeightedAverageSum = 0;
    int zoneDeadCount = 0;
    boolean hasTi = false;

    private boolean pendingTunerChange;
    public boolean hasPendingTunerChange() { return pendingTunerChange; }
    public void setPendingTunerChange() { pendingTunerChange = true; }
    
    private Occupancy currSystemOccupancy = Occupancy.UNOCCUPIED;
    
    private VavSystemController()
    {
        pendingTunerChange = false;

        proportionalGain =  TunerUtil.readTunerValByQuery("system and vav and pgain");
        integralGain = TunerUtil.readTunerValByQuery("system and vav and igain");
        proportionalSpread = (int)TunerUtil.readTunerValByQuery("system and vav and pspread");
        integralMaxTimeout = (int)TunerUtil.readTunerValByQuery("system and vav and itimeout");
    
        CcuLog.i(L.TAG_CCU_SYSTEM, "proportionalGain "+proportionalGain+" integralGain "+integralGain
                                   +" proportionalSpread "+proportionalSpread+" integralMaxTimeout "+integralMaxTimeout);
        
        piController = new SystemPILoopController();
        piController.setIntegralGain(integralGain);
        piController.setProportionalGain(proportionalGain);
        piController.setMaxAllowedError(proportionalSpread);
        piController.setIntegralMaxTimeout(integralMaxTimeout);
        
    }
    
    public static VavSystemController getInstance() {
        return instance;
    }
    
    public void runVavSystemControlAlgo() {

        initializeAlgoLoopVariables();

        ArrayList<HashMap<Object, Object>> allEquips = CCUHsApi
                                                           .getInstance()
                                                           .readAllEntities("(equip and zone and vav) or " +
                                                                            "(equip and zone and ti) or " +
                                                                   "(equip and zone and otn)"
        );

        updateSystemTempHumidity(allEquips);

        processZoneEquips(allEquips);
        processCCUAsZoneEquip();
        
        if (prioritySum == 0 || zoneCount == 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "No valid temperature, Skip VavSystemControlAlgo");
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


        updateLoopOpSignals();

        logAlgoLoopVariables();

        ArrayList<HashMap<Object, Object>> vavEquips = CCUHsApi.getInstance()
                                                               .readAllEntities("equip and zone and vav");
                                                               
        ArrayList<HashMap<Object, Object>> overriddenZoneEquips = new ArrayList<>();
    
        Iterator<HashMap<Object, Object>> equipIterator = vavEquips.iterator();
        while (equipIterator.hasNext()) {
            HashMap equipMap = equipIterator.next();
            if (isDamperOverrideActive(equipMap)) {
                overriddenZoneEquips.add(equipMap);
                equipIterator.remove();
            }
        }
        
        HashMap<String, Double> damperPosMap;

        if (systemState == HEATING && conditioningMode != SystemMode.OFF)
        {
            HashMap<String, Double> normalizedDamperPosMap = getNormalizedDamperPosMap(vavEquips,
                                                                              getBaseDamperPosMap(vavEquips));
            damperPosMap = getAdjustedDamperPosMap(vavEquips, normalizedDamperPosMap,
                                                            systemProfile.getSystemEquipRef());
        } else {
            damperPosMap = getBaseDamperPosMap(vavEquips);
        }

        applyLimitsAndSetDamperPosition(vavEquips, damperPosMap);
        if (!overriddenZoneEquips.isEmpty()) {
            applyOverriddenDampers(overriddenZoneEquips);
        }
    }

    private void initializeAlgoLoopVariables() {

        systemProfile = (VavSystemProfile) L.ccu().systemProfile;
        conditioningMode = SystemMode.values()[(int)systemProfile.getUserIntentVal("conditioning and mode")];
        CcuLog.d(L.TAG_CCU_SYSTEM, "runVavSystemControlAlgo -> conditioningMode: " + conditioningMode);

        weightedAverageCoolingOnlyLoadSum = 0;
        weightedAverageHeatingOnlyLoadSum = 0;
        weightedAverageHeatingConditioningLoadSum = 0;
        weightedAverageHeatingConditioningLoad = 0;
        co2WeightedAverage = 0;
        totalCoolingLoad = 0;
        totalHeatingLoad = 0;
        zoneCount = 0;
        prioritySum = 0;
        co2LoopWeightedAverageASum = 0;
        co2WeightedAverageSum = 0;
        zoneDeadCount = 0;
        hasTi = false;

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
        proportionalGain =  TunerUtil.readTunerValByQuery("system and vav and pgain");
        integralGain = TunerUtil.readTunerValByQuery("system and vav and igain");
        proportionalSpread = (int)TunerUtil.readTunerValByQuery("system and vav and pspread");
        integralMaxTimeout = (int)TunerUtil.readTunerValByQuery("system and vav and itimeout");

        CcuLog.i(L.TAG_CCU_SYSTEM, "proportionalGain "+proportionalGain+" integralGain "+integralGain
                +" proportionalSpread "+proportionalSpread+" integralMaxTimeout "+integralMaxTimeout);

        piController.setIntegralGain(integralGain);
        piController.setProportionalGain(proportionalGain);
        piController.setMaxAllowedError(proportionalSpread);
        piController.setIntegralMaxTimeout(integralMaxTimeout);

        VavSystemProfile profile = (VavSystemProfile) L.ccu().systemProfile;
        profile.refreshTRTuners();

        pendingTunerChange = false;
    }

    private void updateSystemTempHumidity(ArrayList<HashMap<Object, Object>> allEquips) {
        
        updateSystemHumidity(allEquips);
        updateSystemTemperature(allEquips);
        updateSystemDesiredTemp();

        if (L.ccu().systemProfile instanceof VavExternalAhu) {
            CCUHsApi.getInstance().writeHisValByQuery("domainName == \""+averageHumidity+"\"", averageSystemHumidity);
            CCUHsApi.getInstance().writeHisValByQuery("domainName == \""+averageTemperature+"\"", averageSystemTemperature);
        } else {
            systemProfile.setSystemPoint("(average or avg) and humidity", averageSystemHumidity);
            systemProfile.setSystemPoint("(average or avg) and temp", averageSystemTemperature);
        }

    }
    
    private boolean isEmergencyCoolingRequired() {
        return systemState !=  HEATING &&
                (buildingLimitMaxBreached("vav") || buildingLimitMaxBreached("otn") || buildingLimitMaxBreached("ti")) &&
                conditioningMode != SystemMode.OFF;
    }
    
    private boolean isEmergencyHeatingRequired() {
        return systemState != COOLING &&
                (buildingLimitMinBreached("vav") || buildingLimitMinBreached("otn") || buildingLimitMinBreached("ti")) &&
               conditioningMode != SystemMode.OFF;
    }

    private void processZoneEquips(ArrayList<HashMap<Object, Object>> allEquips) {
        
        for (HashMap<Object, Object> equipMap : allEquips) {

            Equip equip = new Equip.Builder().setHashMap(equipMap).build();
            hasTi = hasTi || equip.getMarkers().contains("ti") || equip.getMarkers().contains("otn");

            if (isZoneDead(equip.getId()) || isRFDead(equip.getId())) {
                zoneDeadCount++;
            } else if (hasTemp(equip)) {
                double zoneCurTemp = getEquipCurrentTemp(equip.getId());
                double desiredTempCooling = SystemTemperatureUtil.getDesiredTempCooling(equip.getId());
                double desiredTempHeating = SystemTemperatureUtil.getDesiredTempHeating(equip.getId());

                double zoneCoolingLoad = zoneCurTemp > desiredTempCooling ? zoneCurTemp - desiredTempCooling : 0;
                double zoneHeatingLoad = zoneCurTemp < desiredTempHeating ? desiredTempHeating - zoneCurTemp : 0;
                double zoneDynamicPriority = getEquipDynamicPriority(zoneCoolingLoad > 0 ? zoneCoolingLoad : zoneHeatingLoad, equip.getId());
                totalCoolingLoad += zoneCoolingLoad;
                totalHeatingLoad += zoneHeatingLoad;
                zoneCount++;
                weightedAverageCoolingOnlyLoadSum += zoneCoolingLoad * zoneDynamicPriority;
                weightedAverageHeatingOnlyLoadSum += zoneHeatingLoad * zoneDynamicPriority;
    
                double tempMidPoint = (desiredTempCooling + desiredTempHeating)/2;
                double zoneHeatingConditioningLoad = zoneCurTemp < tempMidPoint ? desiredTempHeating - zoneCurTemp : 0;
                weightedAverageHeatingConditioningLoadSum += zoneHeatingConditioningLoad * zoneDynamicPriority;
                
                prioritySum += zoneDynamicPriority;
                co2WeightedAverageSum += (getEquipCo2(equip.getId()) * zoneDynamicPriority);
                CcuLog.d(L.TAG_CCU_SYSTEM, equip.getDisplayName() + " zoneDynamicPriority: " + zoneDynamicPriority +
                                           " zoneCoolingLoad: " + zoneCoolingLoad + " zoneHeatingLoad: " + zoneHeatingLoad+
                                           " co2WASum "+co2WeightedAverageSum
                );
                CcuLog.d(L.TAG_CCU_SYSTEM, equip.getDisplayName() + " weightedAverageCoolingOnlyLoadSum:" +
                                           weightedAverageCoolingOnlyLoadSum +
                                           " weightedAverageHeatingOnlyLoadSum " + weightedAverageHeatingOnlyLoadSum
                );
            }
        }

    }

    private void processCCUAsZoneEquip() {

        double cmTempInfForPercentileZonesDead = TunerUtil.readTunerValByQuery("dead and percent and influence",
                                                                               L.ccu().systemProfile.getSystemEquipRef());
        if(zoneDeadCount > 0 )
            CcuLog.d(L.TAG_CCU_SYSTEM, "VavSysController = "+hasTi+","+zoneDeadCount+","+zoneCount+","+
                                       cmTempInfForPercentileZonesDead+","+((zoneDeadCount*100)/(zoneDeadCount + zoneCount))
            );

        if((zoneCount == 0) || (!hasTi && ((zoneDeadCount > 0) && (((double)(zoneDeadCount*100)/(zoneDeadCount + zoneCount))
                                                                   >= cmTempInfForPercentileZonesDead)))){


            String sysEquip = L.ccu().systemProfile.getSystemEquipRef();
            if(sysEquip != null) {

                double cmCurrentTemp = getCMCurrentTemp(sysEquip);
                if(isCMTempDead(cmCurrentTemp)) {
                    double desiredTempCooling = ScheduleManager.getInstance().getSystemCoolingDesiredTemp();
                    double desiredTempHeating = ScheduleManager.getInstance().getSystemHeatingDesiredTemp();

                    double zoneCoolingLoad = cmCurrentTemp > desiredTempCooling ? cmCurrentTemp - desiredTempCooling : 0;
                    double zoneHeatingLoad = cmCurrentTemp < desiredTempHeating ? desiredTempHeating - cmCurrentTemp : 0;
                    double zoneDynamicPriority = getCMDynamicPriority(zoneCoolingLoad > 0 ? zoneCoolingLoad : zoneHeatingLoad);
                    totalCoolingLoad += zoneCoolingLoad;
                    totalHeatingLoad += zoneHeatingLoad;
                    zoneCount++;
                    weightedAverageCoolingOnlyLoadSum += zoneCoolingLoad * zoneDynamicPriority;
                    weightedAverageHeatingOnlyLoadSum += zoneHeatingLoad * zoneDynamicPriority;
                    
                    double tempMidPoint = (desiredTempCooling + desiredTempHeating)/2;
                    double zoneHeatingConditioningLoad = cmCurrentTemp < tempMidPoint ? desiredTempHeating - cmCurrentTemp : 0;
                    weightedAverageHeatingConditioningLoadSum += zoneHeatingConditioningLoad * zoneDynamicPriority;
                    
                    prioritySum += zoneDynamicPriority;
                    CcuLog.d(L.TAG_CCU_SYSTEM, "CM zoneDynamicPriority: " + zoneDynamicPriority +
                                               " zoneCoolingLoad: " + zoneCoolingLoad + " zoneHeatingLoad: " +
                                               "" + zoneHeatingLoad + " weightedAverageCoolingOnlyLoadSum " +
                                               weightedAverageCoolingOnlyLoadSum + ", prioritySum" + prioritySum +
                                               ", cmCurrentTemp" + cmCurrentTemp+
                                               ", weightedAverageHeatingConditioningLoadSum "+weightedAverageHeatingConditioningLoadSum
                    );
                }
            }
        }
    }


    private void updateWeightedAverageLoad() {
        
        weightedAverageCoolingOnlyLoad = weightedAverageCoolingOnlyLoadSum / prioritySum;
        weightedAverageHeatingOnlyLoad = weightedAverageHeatingOnlyLoadSum / prioritySum;
        weightedAverageHeatingConditioningLoad = weightedAverageHeatingConditioningLoadSum / prioritySum;
        
        co2WeightedAverage = co2WeightedAverageSum/prioritySum;
        comfortIndex = (int)(totalCoolingLoad + totalHeatingLoad) /zoneCount;

        systemProfile.setSystemPoint("ci and running", comfortIndex);
    
        weightedAverageCoolingOnlyLoadPostML = weightedAverageCoolingOnlyLoad ;//+buildingLoadOffsetML
        weightedAverageHeatingOnlyLoadPostML = weightedAverageHeatingOnlyLoad ;//+buildingLoadOffsetML
        
        weightedAverageCoolingOnlyLoadMAQueue.add(weightedAverageCoolingOnlyLoadPostML);
        weightedAverageHeatingOnlyLoadMAQueue.add(weightedAverageHeatingOnlyLoadPostML);

        double weightedAverageCoolingOnlyLoadMASum = 0;
        for (double val : weightedAverageCoolingOnlyLoadMAQueue) {
            weightedAverageCoolingOnlyLoadMASum += val;
        }
        weightedAverageCoolingOnlyLoadMA = weightedAverageCoolingOnlyLoadMASum/weightedAverageCoolingOnlyLoadMAQueue.size();

        double weightedAverageHeatingOnlyLoadMASum = 0;
        for (double val : weightedAverageHeatingOnlyLoadMAQueue) {
            weightedAverageHeatingOnlyLoadMASum += val;
        }
        weightedAverageHeatingOnlyLoadMA = weightedAverageHeatingOnlyLoadMASum/weightedAverageHeatingOnlyLoadMAQueue.size();
        
        systemProfile.setSystemPoint("moving and (average or avg) and cooling and load",
                                     CCUUtils.roundToTwoDecimal(weightedAverageCoolingOnlyLoadMA));
        systemProfile.setSystemPoint("moving and (average or avg) and heating and load",
                                     CCUUtils.roundToTwoDecimal(weightedAverageHeatingOnlyLoadMA));


    }

    private void handleEmergencyCooling() {

        CcuLog.d(L.TAG_CCU_SYSTEM, " Emergency COOLING Active");
        if ((conditioningMode == COOLONLY || conditioningMode == AUTO) && weightedAverageCoolingOnlyLoadMA > 0)
        {
            CcuLog.d(L.TAG_CCU_SYSTEM, " handleEmergencyCooling ");
            emergencyMode = true;
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

        CcuLog.d(L.TAG_CCU_SYSTEM, " handleEmergencyHeating ");
        if ((conditioningMode == HEATONLY || conditioningMode == AUTO) &&
            (weightedAverageCoolingOnlyLoadMA == 0 && weightedAverageHeatingOnlyLoadMA > 0))
        {
            CcuLog.d(L.TAG_CCU_SYSTEM, " Emergency HEATING Active");
            emergencyMode = true;
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
        CcuLog.d(L.TAG_CCU_SYSTEM," handleOperationalChangeOver : conditioningMode " + conditioningMode + ", weightedAverageCoolingOnlyLoadMA " + weightedAverageCoolingOnlyLoadMA + ", weightedAverageHeatingLoadMA " + weightedAverageHeatingOnlyLoadMA + ", modeChangeoverHysteresis "+modeChangeoverHysteresis);

        if ((conditioningMode == COOLONLY || conditioningMode == AUTO) && weightedAverageCoolingOnlyLoadMA > 0)
        {
            // Only switch into cooling if weightedAverageCoolingOnlyLoadMA > modeChangeoverHysteresis
            // (But we want to be in this block any time there's cooling load, so that loops do not reset)
            if (weightedAverageCoolingOnlyLoadMA > modeChangeoverHysteresis && systemState != COOLING)
            {
                systemState = COOLING;
                piController.reset();
            }
        }
        else if ((conditioningMode == HEATONLY || conditioningMode == AUTO) && (weightedAverageCoolingOnlyLoadMA == 0 && weightedAverageHeatingOnlyLoadMA > 0))
        {
            if (systemState != HEATING)
            {
                systemState = HEATING;
                piController.reset();
            }
        }
        else
        {
            //systemState = OFF;

            coolingSignal = 0;
            heatingSignal = 0;
            piController.reset();
        }
        CcuLog.d(L.TAG_CCU_SYSTEM, "systemState " + systemState + ", coolingSignal " + coolingSignal + ", heatingSignal " + heatingSignal);
    }

    private void updateLoopOpSignals() {
        
        piController.dump();
        if ((systemState == COOLING) && (conditioningMode == COOLONLY || conditioningMode == AUTO)){
            heatingSignal = 0;
            coolingSignal = (int)piController.getLoopOutput(weightedAverageCoolingOnlyLoadPostML, 0);
        } else if ((systemState == HEATING) && (conditioningMode == HEATONLY || conditioningMode == AUTO)){
            coolingSignal = 0;
            heatingSignal = (int)piController.getLoopOutput(weightedAverageHeatingConditioningLoad, 0);
        } else {
            coolingSignal = 0;
            heatingSignal = 0;
            piController.reset();
        }
        piController.dump();
    }
    
    private void logAlgoLoopVariables() {

        CcuLog.d(L.TAG_CCU_SYSTEM, "weightedAverageCoolingOnlyLoadMA: "+weightedAverageCoolingOnlyLoadMA+
                                   " weightedAverageHeatingOnlyLoadMA: " +weightedAverageHeatingOnlyLoadMA +
                                   " weightedAverageHeatingConditioningLoad "+weightedAverageHeatingConditioningLoad +
                                   " systemState: "+systemState+
                                   " coolingSignal: "+coolingSignal+
                                   " heatingSignal: "+heatingSignal
        );
    }
    
    private HashMap<String, Double> getBaseDamperPosMap(ArrayList<HashMap<Object, Object>> vavEquips) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
    
        HashMap<String, Double> baseDamperPosMap = new HashMap<>();
        for (HashMap<Object, Object> equip : vavEquips) {
            HashMap<Object, Object> damper =
                hayStack.readEntity("point and damper and base and cmd and equipRef == \""+equip.get("id")
                                                                                                .toString()+"\""
            );
            double damperPos = hayStack.readHisValById(damper.get("id").toString());
            HashMap<Object, Object> normalizedDamper =
                hayStack.readEntity("point and damper and normalized and cmd and equipRef == \""+equip.get("id")
                                                                                                      .toString()+ "\""
            );
            baseDamperPosMap.put(normalizedDamper.get("id").toString(), damperPos);
        }
        return baseDamperPosMap;
    }
    
    @Override
    public int getCoolingSignal() {
        return coolingSignal;
    }
    
    @Override
    public int getHeatingSignal() {
        return heatingSignal;
    }
    
    @Override
    public SystemController.State getConditioningForecast(Occupied occupiedSchedule) {
        VavSystemProfile profile = (VavSystemProfile) L.ccu().systemProfile;
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
        return CCUHsApi.getInstance().readHisValByQuery("temp and sensor and (current or space) and equipRef == " +
                                                        "\""+equipRef+"\""
        );
    }

    public double getCMCurrentTemp(String equipRef) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and temp and cm and (current or space) and equipRef == " +
                                                        "\""+equipRef+"\""
        );
    }
    public ZonePriority getEquipPriority(String equipRef) {
        double priorityVal = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and config and priority and " +
                                                                   "equipRef == \""+equipRef+"\""
        );
        return ZonePriority.values()[(int) priorityVal];
    }
    public boolean isCMTempDead(double cmTemp) {

        double buildingLimitMax = BuildingTunerCache.getInstance().getBuildingLimitMax();
        double buildingLimitMin = BuildingTunerCache.getInstance().getBuildingLimitMin();

        double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();
        return !(cmTemp > (buildingLimitMax + tempDeadLeeway)) && !(cmTemp < (buildingLimitMin - tempDeadLeeway));
    }

    public boolean hasTemp(Equip q) {
        try
        {
            return CCUHsApi.getInstance().readHisValByQuery("point and (space or current) and temp and sensor and equipRef == \"" + q.getId() + "\"") > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public double getCMDynamicPriority(double zoneLoad){
        ZonePriority p = ZonePriority.NORMAL;
        double zonePrioritySpread = TunerUtil.readTunerValByQuery("point and default and tuner and zone and priority and spread and vav");
        double zonePriorityMultiplier = TunerUtil.readTunerValByQuery("point and default and tuner and zone and priority and multiplier and vav");
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
                "point and tuner and zone and priority and spread and " + "equipRef == \"" + equipRef + "\"");
            double zonePriorityMultiplier = TunerUtil.readTunerValByQuery(
                "point and tuner and zone and priority and multiplier " + "and equipRef == \"" + equipRef + "\"");
            equipDynamicPriority = p.val * Math.pow(zonePriorityMultiplier,
                                                           (zoneLoad / zonePrioritySpread) > 10 ? 10
                                                               : (zoneLoad / zonePrioritySpread));
            equipDynamicPriority = CCUUtils.roundToTwoDecimal(equipDynamicPriority);
        }
        try {
            HashMap<Object, Object> zdpPoint = CCUHsApi.getInstance().readEntity("point and zone and dynamic and priority and" +
                                                                         " equipRef == \"" + equipRef + "\""
            );
            double zdpPointValue = CCUHsApi.getInstance().readHisValById(zdpPoint.get("id").toString());
            if (zdpPointValue != equipDynamicPriority)
                CCUHsApi.getInstance().writeHisValById(zdpPoint.get("id").toString(), equipDynamicPriority);
        }catch (Exception e){
            e.printStackTrace();
        }
        return equipDynamicPriority;
    }
    
    private double getEquipCo2(String equipRef) {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and co2 and sensor and (current or space) and equipRef == \""+equipRef+"\"");
    }
    
    @Override
    public double getSystemCO2WA() {
        return co2WeightedAverage;
    }
    
    public void updateSystemHumidity(ArrayList<HashMap<Object, Object>> allEquips) {
        //Average across zones or from proxy zone.
        double humiditySum = 0;
        double humidityZones = 0;

        for (HashMap<Object, Object> equip : allEquips)
        {
            double humidityVal = CCUHsApi.getInstance().readHisValByQuery("point and air and humidity and sensor and (current or space) " +
                                                                "and equipRef == \""+equip.get("id")+"\""
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
            HashMap<Object, Object> coolTempPoint = CCUHsApi.getInstance()
                                                            .readEntity("point and system and cooling and " +
                                                                        "desired and temp and equipRef == " + "\"" +
                                                                         L.ccu().systemProfile.getSystemEquipRef() + "\""
            );

            CCUHsApi.getInstance().writeHisValById(coolTempPoint.get("id").toString(), desiredTempCooling);
            CCUHsApi.getInstance().writeDefaultValById(coolTempPoint.get("id").toString(), desiredTempCooling);

            HashMap<Object, Object> heatTempPoint = CCUHsApi.getInstance().readEntity("point and system and " +
                                                                           "heating and desired and temp and equipRef == \"" +
                                                                            L.ccu().systemProfile.getSystemEquipRef() + "\""
            );
            CCUHsApi.getInstance().writeHisValById(heatTempPoint.get("id").toString(), desiredTempHeating);
            CCUHsApi.getInstance().writeDefaultValById(heatTempPoint.get("id").toString(), desiredTempHeating);
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
            if(equip.getMarkers().contains("vav") || equip.getMarkers().contains("ti") || equip.getMarkers().contains("otn")) {
                double tempVal = CCUHsApi.getInstance().readHisValByQuery(
                    "temp and sensor and (current or space) and equipRef == \"" + equipMap.get("id") + "\""
                );
                hasTi = hasTi || equip.getMarkers().contains("ti") || equip.getMarkers().contains("otn");
                if (!isZoneDead(equip.getId()) && (tempVal > 0) && !isRFDead(equip.getId())) {
                    tempSum += tempVal;
                    tempZones++;
                }
                totalEquips++;
            }
        }
        double cmTempInfForPercentileZonesDead = TunerUtil.readTunerValByQuery(
                        "dead and percent and influence",L.ccu().systemProfile.getSystemEquipRef());


        if(totalEquips > 0)
            CcuLog.d(L.TAG_CCU_SYSTEM, "VavSysController = "+hasTi+","+tempZones+","+totalEquips+", "+tempSum+", "
                       +cmTempInfForPercentileZonesDead+","+(((totalEquips - tempZones)*100)/(totalEquips)));
        if((totalEquips == 0) || (!hasTi && ((((totalEquips - tempZones)*100)/(totalEquips))
                                             >= cmTempInfForPercentileZonesDead))){
            double cmTemp = getCMCurrentTemp(L.ccu().systemProfile.getSystemEquipRef());
            if(isCMTempDead(cmTemp)) {
                tempSum += cmTemp;
                tempZones++;
            }
            CcuLog.d(L.TAG_CCU_SYSTEM, "VavSysController, cmTemp "+cmTemp+" tempZone "+tempZones);
        }
        averageSystemTemperature = tempZones == 0 ? 0 : tempSum/tempZones;
        averageSystemTemperature = CCUUtils.roundToOneDecimal(averageSystemTemperature);
    }
    
    @Override
    public double getAverageSystemTemperature() {
        return averageSystemTemperature;
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
    private HashMap<String, Double> getNormalizedDamperPosMap(ArrayList<HashMap<Object, Object>> vavEquips,
                                                             HashMap<String, Double> damperPosMap) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        double maxDamperPos = getMaxDamperPos(vavEquips);
        
        if (maxDamperPos == 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM," Abort normalizeAirflow : maxDamperPos = "+maxDamperPos);
            return damperPosMap;
        }
    
        HashMap<String, Double> normalizedDamperPosMap = new HashMap<>();
        double targetPercent = (100 - maxDamperPos) * 100/ maxDamperPos ;
        
        for (HashMap<Object, Object> equip : vavEquips) {
            
            HashMap<Object, Object> normalizedDamper = hayStack.readEntity(
                "point and damper and normalized and cmd and equipRef == \"" + equip.get("id").toString() + "\""
            );
    
            double damperPos = damperPosMap.get(normalizedDamper.get("id").toString());
            double normalizedDamperPos;
            if (isZoneDead(equip.get("id").toString()) || isRFDead(equip.get("id").toString())) {
                normalizedDamperPos = damperPos;
            } else {
                normalizedDamperPos = (damperPos + damperPos * targetPercent / 100);
            }
            
            CcuLog.d(L.TAG_CCU_SYSTEM,
                     "normalizeAirflow" + "Equip: " + equip.get("dis") + ",damperPos :" + damperPos
                     + " targetPercent:" + targetPercent + " normalizedDamper:" + normalizedDamperPos
            );
            normalizedDamperPosMap.put(normalizedDamper.get("id").toString(), normalizedDamperPos);
        }
        return normalizedDamperPosMap;
    }
    
    public double getMaxDamperPos(ArrayList<HashMap<Object, Object>> vavEquips) {
        double maxDamperPos = 0;
        for (HashMap<Object, Object> equip : vavEquips) {
            if (isZoneDead(equip.get("id").toString()) || isRFDead(equip.get("id").toString())) {
                continue;
            }
            HashMap<Object, Object> damper = CCUHsApi.getInstance().readEntity("point and damper and base and cmd and " +
                                                                       "equipRef == \"" +equip.get("id").toString()+"\""
            );
            double damperPos = CCUHsApi.getInstance().readHisValById(damper.get("id").toString());
            if ( damperPos >= maxDamperPos) {
                maxDamperPos = damperPos;
            }
        }
        return maxDamperPos;
    }

    /*
     * Take weighted damper opening (where weightedDamperOpening = (zone1_damper_opening*zone1_damper_size +
     * zone2_damper_opening*zone2_damper_size +..)/(zone1_damper_size + zone2_damper_size + .. )
     * If weighted damper opening is < targetCumulativeDamper  increase the dampers proportionally so that the
     * cumulative damper opening is at least targetCumulativeDamper
     **/
    public HashMap<String, Double> getAdjustedDamperPosMap (ArrayList<HashMap<Object, Object>> vavEquips,
                                                            HashMap<String, Double> normalizedDamperPosMap,
                                                            String systemEquipRef) {
    
        double cumulativeDamperTarget =
            TunerUtil.readTunerValByQuery("target and cumulative and damper", systemEquipRef);
        double weightedDamperOpening = getWeightedDamperOpening(vavEquips,normalizedDamperPosMap);
        CcuLog.d(L.TAG_CCU_SYSTEM, "weightedDamperOpening : " + weightedDamperOpening + " cumulativeDamperTarget : " +
                                   cumulativeDamperTarget);
        if (weightedDamperOpening > 0 && weightedDamperOpening < cumulativeDamperTarget) {
        
            int damperAdjPercent = getDamperAdjustmentTargetPercent(vavEquips,
                                                                    normalizedDamperPosMap,
                                                                    cumulativeDamperTarget
            );
        
            HashMap<String, Double> adjustedDamperPosMap = adjustDamperOpening(vavEquips,
                                                                               normalizedDamperPosMap,
                                                                               damperAdjPercent
            );
        
        
            weightedDamperOpening = getWeightedDamperOpening(vavEquips, normalizedDamperPosMap);
            CcuLog.d(L.TAG_CCU_SYSTEM, "weightedDamperOpening : " + weightedDamperOpening +
                                       " cumulativeDamperTarget : " + cumulativeDamperTarget +
                                       " damperAdjPercent : "+damperAdjPercent
            );
            return adjustedDamperPosMap;
        }
        return normalizedDamperPosMap;
    }
    
    public int getDamperAdjustmentTargetPercent(ArrayList<HashMap<Object, Object>> vavEquips,
                                                HashMap<String, Double> damperPosMap,
                                                double damperTargetOpening) {
        
        double currentWeightedDamperOpening = getWeightedDamperOpening(vavEquips, damperPosMap);
        
        HashMap<String, Double> adjustedDamperPosMap = adjustDamperOpening(vavEquips,
                                                                           damperPosMap,
                                                                           SystemConstants.DEFAULT_DAMPER_ADJ_INCREMENT
        );
        
        double adjustedWeightedDamperOpening = getWeightedDamperOpening(vavEquips, adjustedDamperPosMap);
        
        double damperChangeFor1Percentage = adjustedWeightedDamperOpening - currentWeightedDamperOpening;
        
        double requiredAdjustment = (damperTargetOpening - currentWeightedDamperOpening ) / damperChangeFor1Percentage;
        
        return (int)Math.ceil(requiredAdjustment);
        
    }
    
    public double getWeightedDamperOpening(ArrayList<HashMap<Object, Object>> vavEquips,
                                           HashMap<String, Double> normalizedDamperPosMap) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        int damperSizeSum = 0;
        int weightedDamperOpeningSum = 0;
        for (HashMap<Object, Object> equip : vavEquips) {
            HashMap<Object, Object> damperPos = hayStack.readEntity("point and damper and normalized and cmd and " +
                                                                    "equipRef == \""+equip.get("id").toString()+"\""
            );
            HashMap<Object, Object> damperSize = hayStack.readEntity("point and config and damper and size and " +
                                                                     "equipRef == \""+equip.get("id").toString()+"\""
            );
        
            double damperPosVal = normalizedDamperPosMap.get(damperPos.get("id").toString());
            double damperSizeVal = hayStack.readDefaultValById(damperSize.get("id").toString());
            weightedDamperOpeningSum += damperPosVal * damperSizeVal;
            damperSizeSum += damperSizeVal;
        }
        return damperSizeSum == 0 ? 0 : (double) weightedDamperOpeningSum / damperSizeSum;
    }
    
    public HashMap<String, Double> adjustDamperOpening(ArrayList<HashMap<Object, Object>> vavEquips,
                                                       HashMap<String, Double> normalizedDamperPosMap,
                                                       int percent) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<String, Double> adjustedDamperOpeningMap = new HashMap<>();
        for (HashMap<Object, Object> equip : vavEquips) {
            
            HashMap<Object, Object> damperPos = hayStack.readEntity("point and damper and normalized and cmd and equipRef " +
                                                              "== \""+ equip.get("id").toString()+"\""
            );
            double damperPosVal = normalizedDamperPosMap.get(damperPos.get("id").toString());
            
            if (isZoneDead(equip.get("id").toString()) || isRFDead(equip.get("id").toString())) {
                adjustedDamperOpeningMap.put(damperPos.get("id").toString() , damperPosVal);
            } else {
                damperPosVal = Math.max(damperPosVal, MIN_DAMPER_FOR_CUMULATIVE_CALCULATION);
                double adjustedDamperPos = damperPosVal + (damperPosVal * percent) / 100.0;
                adjustedDamperPos = Math.min(adjustedDamperPos, SystemConstants.DAMPER_POSITION_MAX);
                adjustedDamperOpeningMap.put(damperPos.get("id").toString(), adjustedDamperPos);
            }
        }
        return adjustedDamperOpeningMap;
    }
    
    public void applyLimitsAndSetDamperPosition(ArrayList<HashMap<Object, Object>> vavEquips,
                                                HashMap<String, Double> normalizedDamperMap) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        for (HashMap<Object, Object> equip : vavEquips) {
            HashMap<Object, Object> damperPos = hayStack.readEntity("point and damper and normalized and cmd and " +
                                                                 "equipRef == \""
                                              +equip.get("id").toString()+"\"");
            double normalizedDamperPos = normalizedDamperMap.get(damperPos.get("id").toString());
            double minLimit = 0, maxLimit = 0;
            if (systemState == COOLING) {
                if (getStatus(equip.get("group").toString()) == ZoneState.HEATING.ordinal()) {
                    minLimit = hayStack.readDefaultVal("point and zone and config and min and damper" +
                            " and heating and equipRef == \"" + equip.get("id").toString() + "\"");
                    maxLimit = hayStack.readDefaultVal("point and zone and config and max and damper " +
                            "and heating and equipRef == \"" + equip.get("id").toString() + "\"");
                } else {
                    minLimit = hayStack.readDefaultVal("point and zone and config and min and damper " +
                            "and cooling and equipRef == \"" + equip.get("id").toString() + "\"");
                    maxLimit = hayStack.readDefaultVal("point and zone and config and max and damper" +
                            " and cooling and equipRef == \"" + equip.get("id").toString() + "\"");
                }

            } else {
                minLimit = hayStack.readDefaultVal("point and zone and config and min and damper" +
                        " and heating and equipRef == \"" + equip.get("id").toString() + "\"");
                maxLimit = hayStack.readDefaultVal("point and zone and config and max and damper " +
                        "and heating and equipRef == \"" + equip.get("id").toString() + "\"");
            }
            //MaxLimit point does not exist when trueCFM is enabled.
            if (maxLimit == 0) {
                maxLimit = 100;
            }
            double limitedNormalizedDamperPos = Math.min(normalizedDamperPos, maxLimit);
            limitedNormalizedDamperPos = Math.max(limitedNormalizedDamperPos, minLimit);
            
            limitedNormalizedDamperPos = CCUUtils.roundToTwoDecimal(limitedNormalizedDamperPos);
            
            double curNormalisedDamper = hayStack.readHisValById(damperPos.get("id").toString());
            if (curNormalisedDamper != limitedNormalizedDamperPos) {
                hayStack.writeHisValById(damperPos.get("id").toString() , limitedNormalizedDamperPos);
            }
    
            CcuLog.d(L.TAG_CCU_SYSTEM,"setDamperLimits : Equip "+equip.get("dis")+" minLimit "
                                      +minLimit+" maxLimit "+maxLimit+" normalizedDamperPos "+normalizedDamperPos
                                        +" limitedNormalizedDamperPos :"+limitedNormalizedDamperPos
            );
    
        }
    }
    
    private void applyOverriddenDampers(ArrayList<HashMap<Object, Object>> equips) {
       
        for (HashMap<Object, Object> equip : equips) {
            CcuLog.d(L.TAG_CCU_SYSTEM, " applyOverriddenDampers "+equip.get("dis"));
            HashMap<Object, Object> normalizedDamper = CCUHsApi.getInstance().readEntity(
                "point and damper and normalized and cmd and equipRef == \"" + equip.get("id").toString() + "\""
            );
    
            double curDamperPos = CCUHsApi.getInstance().readHisValById(normalizedDamper.get("id").toString());
            if (curDamperPos > 0) {
                CCUHsApi.getInstance().writeHisValById(normalizedDamper.get("id").toString() , 0.0);
            }
        }
        
    }
    
    public double getStatus(String nodeAddr) {
        return CCUHsApi.getInstance().readHisValByQuery("point and not ota and status and his and group == \""+nodeAddr+"\"");
    }
    
    private boolean isDamperOverrideActive(HashMap<Object, Object> equipMap) {
        
        Short nodeAddr = Short.parseShort(equipMap.get("group").toString());
        ZoneProfile profile = L.getProfile(nodeAddr);
        if (profile != null) {
            CcuLog.d(L.TAG_CCU_SYSTEM, " isDamperOverrideActive " + equipMap.get("dis") + " : " + profile.isDamperOverrideActive());
            return profile.isDamperOverrideActive();
        } else {
            CcuLog.d(L.TAG_CCU_SYSTEM," isDamperOverrideActive : Profile null for "+equipMap.get("dis"));
            return false;
        }
    }
    
    @Override
    public void reset(){
        CcuLog.i(L.TAG_CCU_SYSTEM, "Reset system loop");
        weightedAverageCoolingOnlyLoadMAQueue.clear();
        weightedAverageHeatingOnlyLoadMAQueue.clear();
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
}
