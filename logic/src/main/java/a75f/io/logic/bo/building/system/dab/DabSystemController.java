package a75f.io.logic.bo.building.system.dab;

import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.HashMap;

import a75.io.algos.ControlLoop;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.dab.DabProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.util.HSEquipUtil;
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
    private static DabSystemController instance = new DabSystemController();
    
    ControlLoop piController;
    
    int coolingSignal;
    int heatingSignal;
    
    EvictingQueue<Double> weightedAverageLoadPostMLQ = EvictingQueue.create(15);
    
    
    int ciDesired;
    int comfortIndex = 0;
    
    
    double totalCoolingLoad = 0;
    double totalHeatingLoad = 0;
    int zoneCount = 0;
    
    double weightedAverageLoadSum;
    
    double weightedAverageLoad;
    double weightedAverageLoadPostML;
    
    double weightedAverageLoadMA;
    double weightedAverageCoolingLoadPostML ;
    double weightedAverageHeatingLoadPostML  ;
    
    double averageSystemHumidity = 0;
    double averageSystemTemperature = 0;
    
    double co2LoopOpWA = 0;
    private DabSystemController()
    {
        piController = new ControlLoop();
        piController.setProportionalSpread(2);
    }
    
    public static DabSystemController getInstance() {
        return instance;
    }
    
    public void runDabSystemControlAlgo() {
        
        double prioritySum = 0;
        DabSystemProfile profile = (DabSystemProfile) L.ccu().systemProfile;
        ciDesired = (int)profile.getUserIntentVal("desired and ci");
        SystemMode systemMode = SystemMode.values()[(int)profile.getUserIntentVal("rtu and mode")];
        CcuLog.d(L.TAG_CCU_SYSTEM, "runDabSystemControlAlgo -> ciDesired: " + ciDesired + " systemMode: " + systemMode);
    
        weightedAverageCoolingLoadPostML = weightedAverageHeatingLoadPostML = weightedAverageLoadSum = 0;
        totalCoolingLoad = totalHeatingLoad = zoneCount = 0;
        updateSystemHumidity();
        updateSystemTemperature();
        
        profile.setSystemPoint("average and humidity", averageSystemHumidity);
        profile.setSystemPoint("average and temp", averageSystemTemperature);
        
        double co2LoopWASum = 0;
        
        for (Floor f: HSUtil.getFloors())
        {
            for(Zone z: HSUtil.getZones(f.getId())) {
                
                for (Equip q : HSUtil.getEquips(z.getId()))
                {
                    
                    if (q.getMarkers().contains("dab") == false || isZoneDead(q) || !hasTemp(q))
                    {
                        continue;
                    }
                    double zoneCurTemp = getEquipCurrentTemp(q.getId());
                    double zoneTargetTemp = getEquipTempTarget(q.getId());
                    double desiredTempCooling = HSEquipUtil.getDesiredTempCooling(q.getId());
                    double desiredTempHeating = HSEquipUtil.getDesiredTempHeating(q.getId());
                    
                    double zoneCoolingLoad = zoneCurTemp > desiredTempCooling ? zoneCurTemp - desiredTempCooling : 0;
                    double zoneHeatingLoad = zoneCurTemp < desiredTempHeating ? desiredTempHeating - zoneCurTemp : 0;
                    double zoneDynamicPriority = getEquipDynamicPriority(zoneCoolingLoad > 0 ? zoneCoolingLoad : zoneHeatingLoad, q.getId());
                    totalCoolingLoad += zoneCoolingLoad;
                    totalHeatingLoad += zoneHeatingLoad;
                    zoneCount++;
                    
                    weightedAverageLoadSum += (zoneCoolingLoad * zoneDynamicPriority) - (zoneHeatingLoad * zoneDynamicPriority);
                    prioritySum += zoneDynamicPriority;
                    co2LoopWASum += (getEquipCo2LoopOp(q.getId()) * zoneDynamicPriority);
                    CcuLog.d(L.TAG_CCU_SYSTEM, q.getDisplayName() + " zoneDynamicPriority: " + zoneDynamicPriority + " zoneCoolingLoad: " + zoneCoolingLoad + " zoneHeatingLoad: " + zoneHeatingLoad);
                    CcuLog.d(L.TAG_CCU_SYSTEM, q.getDisplayName() + " weightedAverageLoadSum: "+weightedAverageLoadSum+" co2LoopWASum "+co2LoopWASum);
                }
            }
            
        }
        
        if (prioritySum == 0 || zoneCount == 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "No valid temperature, Skip DabSystemControlAlgo");
            systemState = OFF;
            reset();
            return;
        }
        
        weightedAverageLoad = weightedAverageLoadSum / prioritySum;
        co2LoopOpWA = co2LoopWASum/prioritySum;
        
        comfortIndex = (int)(totalCoolingLoad + totalHeatingLoad) /zoneCount;
        
        profile.setSystemPoint("ci and running", comfortIndex);
    
        weightedAverageLoadPostML = weightedAverageLoad ;//+buildingLoadOffsetML
        weightedAverageLoadPostMLQ.add(weightedAverageLoadPostML);
        
        
        double weightedAverageLoadPostMLQSum = 0;
        for (double val : weightedAverageLoadPostMLQ) {
            weightedAverageLoadPostMLQSum += val;
        }
        weightedAverageLoadMA = weightedAverageLoadPostMLQSum/weightedAverageLoadPostMLQ.size();
        
        if ((systemState != HEATING) && buildingLimitMaxBreached("dab")) {
            CcuLog.d(L.TAG_CCU_SYSTEM, " Emergency COOLING Active");
            emergencyMode = true;
            if (systemMode == COOLONLY || systemMode == AUTO)
            {
                if (systemState != COOLING)
                {
                    systemState = COOLING;
                    piController.reset();
                }
            } else {
                systemState = OFF;
            }
            
        } else if ((systemState != COOLING) && buildingLimitMinBreached("dab")) {
            CcuLog.d(L.TAG_CCU_SYSTEM, " Emergency HEATING Active");
            emergencyMode = true;
            if (systemMode == HEATONLY || systemMode == AUTO)
            {
                if (systemState != HEATING)
                {
                    systemState = HEATING;
                    piController.reset();
                }
            } else {
                systemState = OFF;
            }
        } else
        {
            if (emergencyMode) {
                CcuLog.d(L.TAG_CCU_SYSTEM, " Emergency CONDITIONING Disabled");
                piController.reset();
                emergencyMode = false;
            }
            
            if ((systemMode == COOLONLY || systemMode == AUTO) && weightedAverageLoadMA > 0)
            {
                if (systemState != COOLING)
                {
                    systemState = COOLING;
                    piController.reset();
                }
            }
            else if ((systemMode == HEATONLY || systemMode == AUTO) && weightedAverageLoadMA < 0)
            {
                if (systemState != HEATING)
                {
                    systemState = HEATING;
                    piController.reset();
                }
            }
            else
            {
                systemState = OFF;
            }
        }
    
        CcuLog.d(L.TAG_CCU_SYSTEM, "weightedAverageLoadMA "+weightedAverageLoadMA+" co2LoopOpWA "+co2LoopOpWA+" systemState: "+systemState);
        profile.setSystemPoint("operating and mode", systemState.ordinal());
    
        weightedAverageCoolingLoadPostML = weightedAverageLoadPostML > 0 ? weightedAverageLoadPostML : 0;
    
        weightedAverageHeatingLoadPostML = weightedAverageLoadPostML < 0 ? Math.abs(weightedAverageLoadPostML) : 0;
    
        piController.dump();
        if (systemState == COOLING) {
            heatingSignal = 0;
            coolingSignal = (int)piController.getLoopOutput(weightedAverageCoolingLoadPostML, 0);
        } else if (systemState == HEATING){
            coolingSignal = 0;
            heatingSignal = (int)piController.getLoopOutput(weightedAverageHeatingLoadPostML, 0);
        } else {
            coolingSignal = 0;
            heatingSignal = 0;
        }
        piController.dump();
        CcuLog.d(L.TAG_CCU_SYSTEM, "weightedAverageCoolingLoadPostML: "+weightedAverageCoolingLoadPostML+" weightedAverageHeatingLoadPostML: "
                                   +weightedAverageHeatingLoadPostML+" coolingSignal: "+coolingSignal+" heatingSignal: "+heatingSignal);
        
        profile.setSystemPoint("weighted and average and moving and load",weightedAverageLoadMA);
        profile.setSystemPoint("weighted and average and cooling and load",weightedAverageCoolingLoadPostML);
        profile.setSystemPoint("weighted and average and heating and load",weightedAverageHeatingLoadPostML);
        
        if (systemState != OFF)
        {
            normalizeAirflow();
            adjustDamperForCumulativeTarget(profile.getSystemEquipRef());
        } else {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            ArrayList<HashMap> dabEquips = hayStack.readAll("equip and dab and zone");
            for (HashMap m : dabEquips) {
                HashMap damper = hayStack.read("point and damper and base and cmd and equipRef == \""+m.get("id").toString()+"\"");
                double damperPos = hayStack.readHisValById(damper.get("id").toString());
                HashMap normalizedDamper = hayStack.read("point and damper and normalized and cmd and equipRef == \""+m.get("id").toString()+"\"");
                hayStack.writeHisValById(normalizedDamper.get("id").toString(), damperPos);
            }
        }
        
        setDamperLimits();
        
    }
    
    @Override
    public int getCoolingSignal() {
        return coolingSignal;
    }
    
    @Override
    public int getHeatingSignal() {
        return heatingSignal;
    }
    
    public boolean isAllZonesHeating() {
        for (ZoneProfile p: L.ccu().zoneProfiles)
        {
            if (p.getState() != ZoneState.HEATING) {
                CcuLog.d(L.TAG_CCU_SYSTEM," Equip "+p.getProfileType()+" is not in Heating");
                return false;
            }
        }
        return true;
    }
    
    public SystemController.State getConditioningForecast(Occupied occupiedSchedule) {
        DabSystemProfile profile = (DabSystemProfile) L.ccu().systemProfile;
        SystemMode systemMode = SystemMode.values()[(int)profile.getUserIntentVal("rtu and mode")];
        
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
    
    public boolean isAnyZoneCooling() {
        for (ZoneProfile p: L.ccu().zoneProfiles)
        {
            System.out.println(" Zone State " +p.state);
            if (p.getState() == ZoneState.COOLING) {
                CcuLog.d(L.TAG_CCU_SYSTEM," Equip "+p.getProfileType()+" is in Cooling");
                return true;
            }
        }
        return false;
    }
    
    public boolean isDabEquip(Equip q) {
        if (q.getProfile() == ProfileType.DAB.name()) {
            return true;
        }
        return false;
    }
    
    public double getEquipCurrentTemp(String equipRef) {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and equipRef == \""+equipRef+"\"");
    }
    
    public ZonePriority getEquipPriority(String equipRef) {
        double priorityVal = CCUHsApi.getInstance().readDefaultVal("point and zone and config and dab and priority and equipRef == \""+equipRef+"\"");
        return ZonePriority.values()[(int) priorityVal];
    }
    
    public double getEquipDesiredTemp(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and air and temp and desired and equipRef == \""+equipRef+"\"");
        
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 72;
    }
    
    public double getEquipTempTarget(String zoneRef) //Humidity compensated
    {
        return getEquipDesiredTemp(zoneRef);//TODO - TEMP
    }
    
    public double getEquipDynamicPriority(double zoneLoad, String equipRef) {
        ZonePriority p = getEquipPriority(equipRef);
        if (getEquipCurrentTemp(equipRef) == 0) {
            return p.val;
        }
        
        double zonePrioritySpread = TunerUtil.readTunerValByQuery("point and tuner and dab and zone and priority and spread and equipRef == \"" + equipRef + "\"");;
        double zonePriorityMultiplier = TunerUtil.readTunerValByQuery("point and tuner and dab and zone and priority and multiplier and equipRef == \""+equipRef+"\"");;
        
        return p.val * Math.pow(zonePriorityMultiplier, (zoneLoad/zonePrioritySpread) > 10 ? 10 : (zoneLoad/zonePrioritySpread));
    }
    
    public double getEquipCo2LoopOp(String equipRef){
        for (ZoneProfile f : L.ccu().zoneProfiles) {
            Equip q = f.getEquip();
            if (q.getMarkers().contains("dab"))
            {
                double enabledCO2Control = CCUHsApi.getInstance().readDefaultVal("point and config and dab and enable and co2 and equipRef == \"" + q.getId() + "\"");
                if (q.getId().equals(equipRef) && enabledCO2Control > 0)
                {
                    DabProfile d = (DabProfile) f;
                    return d.getCo2LoopOp();
                }
            }
        }
        return 0;
    }
    
    public void updateSystemHumidity() {
        //Average across zones or from proxy zone.
        double humiditySum = 0;
        double humidityZones = 0;
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> dabEquips = hayStack.readAll("equip and zone");
        
        for (HashMap q : dabEquips)
        {
            double humidityVal = hayStack.readHisValByQuery("point and air and humidity and sensor and current and equipRef == \""+q.get("id")+"\"");
            
            if (humidityVal != 0) {
                humiditySum += humidityVal;
                humidityZones++;
            }
        }
        averageSystemHumidity = humidityZones == 0 ? 0 : humiditySum/humidityZones;
    }
    
    @Override
    public double getAverageSystemHumidity() {
        return averageSystemHumidity;
    }
    
    public void updateSystemTemperature() {
        //Average across zones or from proxy zone.
        double tempSum = 0;
        double tempZones = 0;
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> dabEquips = hayStack.readAll("equip and zone");
        
        for (HashMap q : dabEquips)
        {
            double tempVal = hayStack.readHisValByQuery("point and air and temp and sensor and current and equipRef == \""+q.get("id")+"\"");
            if (!isZoneDead(new Equip.Builder().setHashMap(q).build()) && tempVal > 0) {
                tempSum += tempVal;
                tempZones++;
            }
        }
        averageSystemTemperature = tempZones == 0 ? 0 : tempSum/tempZones;
    }
    
    public boolean isZoneDead(Equip q) {
        try
        {
            return CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and equipRef == \"" + q.getId() + "\"").equals("Zone Temp Dead");
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean hasTemp(Equip q) {
        try
        {
            return CCUHsApi.getInstance().readHisValByQuery("point and current and temp and equipRef == \"" + q.getId() + "\"") > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public double getAverageSystemTemperature() {
        return averageSystemTemperature;
    }
    
    public double getWACo2LoopOp() {
        return co2LoopOpWA;
    }
    
    /*public double getDynamicPriority(double zoneLoad, String zoneRef) {
        
        ZonePriority p = getZonePriority(zoneRef);
        if (getZoneCurrentTemp(zoneRef) == 0) {
            return p.val;
        }
    
        double zonePrioritySpread = 2;
        double zonePriorityMultiplier = 1.3;
    
        for (ZoneProfile z : L.ccu().zoneProfiles)
        {
            Equip q = z.getEquip();
            if (q.getZoneRef().equals(zoneRef)) {
                zonePrioritySpread = TunerUtil.readTunerValByQuery("point and tuner and dab and zone and priority and spread and equipRef == \""+q.getId()+"\"");
                break;
            }
        }
    
        for (ZoneProfile z : L.ccu().zoneProfiles)
        {
            Equip q = z.getEquip();
            if (q.getZoneRef().equals(zoneRef)) {
                zonePriorityMultiplier = TunerUtil.readTunerValByQuery("point and tuner and dab and zone and priority and multiplier and equipRef == \""+q.getId()+"\"");
                break;
            }
        }
        
        //zoneDynamicPriority = zoneBasePriority*((zonePriorityMultiplier )^(rounddownTo10(zoneCoolingLoad/zonePrioritySpread))
        
        return p.val * Math.pow(zonePriorityMultiplier, (zoneLoad/zonePrioritySpread) > 10 ? 10 : (zoneLoad/zonePrioritySpread));
    }*/
    
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
    public void normalizeAirflow() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> dabEquips = hayStack.readAll("equip and dab and zone");
        double maxDamperPos = 0;
        for (HashMap m : dabEquips) {
            if (isZoneDead(new Equip.Builder().setHashMap(m).build())) {
                continue;
            }
            HashMap damper = hayStack.read("point and damper and base and cmd and equipRef == \""+m.get("id").toString()+"\"");
            double damperPos = hayStack.readHisValById(damper.get("id").toString());
            if ( damperPos >= maxDamperPos) {
                maxDamperPos = damperPos;
            }
        }
        
        if (maxDamperPos == 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM," Abort normalizeAirflow : maxDamperPos = "+maxDamperPos);
            return;
        }
        
        double targetPercent = (100 - maxDamperPos) * 100/ maxDamperPos ;
        
        
        for (HashMap m : dabEquips) {
            if (isZoneDead(new Equip.Builder().setHashMap(m).build())) {
                continue;
            }
            //Primary and secondary have the same base damper opening now.
            HashMap damper = hayStack.read("point and damper and base and cmd and equipRef == \""+m.get("id").toString()+"\"");
            double damperPos = hayStack.readHisValById(damper.get("id").toString());
            int normalizedDamperPos = (int) (damperPos + damperPos * targetPercent/100);
            HashMap normalizedPrimaryDamper = hayStack.read("point and damper and normalized and primary and cmd and equipRef == \""+m.get("id").toString()+"\"");
            HashMap normalizedSecondaryamper = hayStack.read("point and damper and normalized and secondary and cmd and equipRef == \""+m.get("id").toString()+"\"");
            CcuLog.d(L.TAG_CCU_SYSTEM,"normalizeAirflow"+" Equip: "+m.get("dis")+" ,damperPos :"+damperPos+" targetPercent: "+targetPercent+" normalizedDamperPos: "+normalizedDamperPos);
            hayStack.writeHisValById(normalizedPrimaryDamper.get("id").toString(), (double)normalizedDamperPos);
            hayStack.writeHisValById(normalizedSecondaryamper.get("id").toString(), (double)normalizedDamperPos);
        }
        
        
    }
    /*
     * Take weighted damper opening (where weightedDamperOpening = (zone1_damper_opening*zone1_damper_size +
     * zone2_damper_opening*zone2_damper_size +..)/(zone1_damper_size + zone2_damper_size + .. )
     * If weighted damper opening is < targetCumulativeDamper  increase the dampers proportionally(each damper
     * by 1% of its value) till it is at least targetCumulativeDamper
     **/
    public void adjustDamperForCumulativeTarget( String systemEquipRef) {
        
        double cumulativeDamperTarget = TunerUtil.readTunerValByQuery("target and cumulative and damper", systemEquipRef );
        double weightedDamperOpening = getWeightedDamperOpening();
        CcuLog.d(L.TAG_CCU_SYSTEM,"weightedDamperOpening : "+weightedDamperOpening +" cumulativeDamperTarget : "+cumulativeDamperTarget);
        
        while(weightedDamperOpening > 0 && weightedDamperOpening < cumulativeDamperTarget) {
            adjustDamperOpening(1);
            weightedDamperOpening = getWeightedDamperOpening();
            CcuLog.d(L.TAG_CCU_SYSTEM,"weightedDamperOpening : "+weightedDamperOpening +" cumulativeDamperTarget : "+cumulativeDamperTarget);
        }
    }
    
    public double getWeightedDamperOpening() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> dabEquips = hayStack.readAll("equip and dab and zone");
        int damperSizeSum = 0;
        int weightedDamperOpeningSum = 0;
        for (HashMap m : dabEquips) {
            HashMap damperPos = hayStack.read("point and damper and normalized and cmd and equipRef == \""+m.get("id").toString()+"\"");
            HashMap damperSize = hayStack.read("point and config and damper and size and equipRef == \""+m.get("id").toString()+"\"");
            
            double damperPosVal = hayStack.readHisValById(damperPos.get("id").toString());
            double damperSizeVal = hayStack.readDefaultValById(damperSize.get("id").toString());
            weightedDamperOpeningSum += damperPosVal * damperSizeVal;
            damperSizeSum += damperSizeVal;
        }
        return damperSizeSum == 0 ? 0 : weightedDamperOpeningSum / damperSizeSum;
    }
    
    public void adjustDamperOpening(int percent) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> dabEquips = hayStack.readAll("equip and dab and zone");
        for (HashMap m : dabEquips) {
            HashMap damperPos = hayStack.read("point and damper and normalized and cmd and equipRef == \""+m.get("id").toString()+"\"");
            double damperPosVal = hayStack.readHisValById(damperPos.get("id").toString());
            double adjustedDamperPos = damperPosVal + (damperPosVal * percent) /100.0;
            hayStack.writeHisValById(damperPos.get("id").toString() , adjustedDamperPos);
        }
    }
    
    public void setDamperLimits() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> dabEquips = hayStack.readAll("equip and dab and zone");
        for (HashMap m : dabEquips) {
            HashMap primaryDamperPos = hayStack.read("point and damper and normalized and primary and cmd and equipRef == \""+m.get("id").toString()+"\"");
            double limitedPrimaryDamperPos = hayStack.readHisValById(primaryDamperPos.get("id").toString());
    
            HashMap secondoryDamperPos = hayStack.read("point and damper and normalized and secondary and cmd and equipRef == \""+m.get("id").toString()+"\"");
            double limitedSecondaryDamperPos = hayStack.readHisValById(secondoryDamperPos.get("id").toString());
            
            double minLimit = 0, maxLimit = 0;
            if (getStatus(m.get("group").toString()) == ZoneState.COOLING.ordinal()) {
                minLimit = hayStack.readDefaultVal("point and min and damper and cooling and equipRef == \""+m.get("id").toString()+"\"");
                maxLimit = hayStack.readDefaultVal("point and max and damper and cooling and equipRef == \""+m.get("id").toString()+"\"");
            } else {
                minLimit = hayStack.readDefaultVal("point and min and damper and heating and equipRef == \""+m.get("id").toString()+"\"");
                maxLimit = hayStack.readDefaultVal("point and max and damper and heating and equipRef == \""+m.get("id").toString()+"\"");
            }
            CcuLog.d(L.TAG_CCU_SYSTEM,"setDamperLimits : Equip "+m.get("dis")+" minLimit "+minLimit+" maxLimit "+maxLimit);
            limitedPrimaryDamperPos = Math.min(limitedPrimaryDamperPos, maxLimit);
            limitedPrimaryDamperPos = Math.max(limitedPrimaryDamperPos, minLimit);
    
            limitedSecondaryDamperPos = Math.min(limitedSecondaryDamperPos, maxLimit);
            limitedSecondaryDamperPos = Math.max(limitedSecondaryDamperPos, minLimit);
            
            hayStack.writeHisValById(primaryDamperPos.get("id").toString() , limitedPrimaryDamperPos);
            hayStack.writeHisValById(secondoryDamperPos.get("id").toString() , limitedSecondaryDamperPos);
        }
    }
    
    public double getStatus(String nodeAddr) {
        return CCUHsApi.getInstance().readHisValByQuery("point and status and his and group == \""+nodeAddr+"\"");
    }
    
    @Override
    public void reset(){
        weightedAverageLoadPostMLQ.clear();
        piController.reset();
        heatingSignal = 0;
        coolingSignal = 0;
    }
}

