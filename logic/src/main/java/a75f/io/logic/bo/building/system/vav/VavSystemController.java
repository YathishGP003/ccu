package a75f.io.logic.bo.building.system.vav;

import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.HashMap;

import a75.io.algos.ControlLoop;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
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
 * Created by samjithsadasivan on 11/9/18.
 */

/**
 * DxController applies Weighted average and Moving average filters on temperature diffs.
 * MA value is used to determine AHU change over. WA determines the heating signal.
 */
public class VavSystemController extends SystemController
{
    
    private static VavSystemController instance = new VavSystemController();
    
    ControlLoop piController;
    
    int coolingSignal;
    int heatingSignal;
    
    EvictingQueue<Double> weightedAverageCoolingOnlyLoadMAQueue = EvictingQueue.create(15);
    EvictingQueue<Double> weightedAverageHeatingOnlyLoadMAQueue = EvictingQueue.create(15);;
    
    int ciDesired;
    int comfortIndex = 0;
    
    
    double totalCoolingLoad = 0;
    double totalHeatingLoad = 0;
    int zoneCount = 0;
    
    double weightedAverageCoolingOnlyLoadSum;
    double weightedAverageHeatingOnlyLoadSum;
    double weightedAverageLoadSum;
    
    double weightedAverageCoolingOnlyLoad;
    double weightedAverageHeatingOnlyLoad;
    double weightedAverageLoad;
    
    double weightedAverageCoolingOnlyLoadMA;
    double weightedAverageHeatingOnlyLoadMA;
    
    //int systemOccupancy = 0;
    
    double averageSystemHumidity = 0;
    double averageSystemTemperature = 0;
    
    double weightedAverageCoolingOnlyLoadPostML;
    double weightedAverageHeatingOnlyLoadPostML;
    double weightedAverageLoadPostML;
    
    
    private VavSystemController()
    {
        piController = new ControlLoop();
        piController.setProportionalSpread(2);
    }
    
    public static VavSystemController getInstance() {
        return instance;
    }
    
    public void runVavSystemControlAlgo() {
        
        double prioritySum = 0;
        VavSystemProfile profile = (VavSystemProfile) L.ccu().systemProfile;
        ciDesired = (int)profile.getUserIntentVal("desired and ci");
        SystemMode systemMode = SystemMode.values()[(int)profile.getUserIntentVal("rtu and mode")];
        CcuLog.d(L.TAG_CCU_SYSTEM, "runVavSystemControlAlgo -> ciDesired: " + ciDesired + " systemMode: " + systemMode);
    
        weightedAverageCoolingOnlyLoadSum = weightedAverageHeatingOnlyLoadSum = weightedAverageLoadSum = 0;
        //systemOccupancy = 0;
        
        updateSystemHumidity();
        updateSystemTemperature();
        
        profile.setSystemPoint("average and humidity", averageSystemHumidity);
        profile.setSystemPoint("average and temp", averageSystemTemperature);
        
        for (Floor f: HSUtil.getFloors())
        {
            for(Zone z: HSUtil.getZones(f.getId())) {
                for (Equip q : HSUtil.getEquips(z.getId()))
                {
                    
                    if (q.getMarkers().contains("vav") == false || isZoneDead(q))
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
                    weightedAverageCoolingOnlyLoadSum += zoneCoolingLoad * zoneDynamicPriority;
                    weightedAverageHeatingOnlyLoadSum += zoneHeatingLoad * zoneDynamicPriority;
                    weightedAverageLoadSum = +(zoneCoolingLoad * zoneDynamicPriority) - (zoneHeatingLoad * zoneDynamicPriority);
                    prioritySum += zoneDynamicPriority;
                    CcuLog.d(L.TAG_CCU_SYSTEM, q.getDisplayName() + " zoneDynamicPriority: " + zoneDynamicPriority + " zoneCoolingLoad: " + zoneCoolingLoad + " zoneHeatingLoad: " + zoneHeatingLoad);
                    CcuLog.d(L.TAG_CCU_SYSTEM, q.getDisplayName() + " weightedAverageCoolingOnlyLoadSum:" + weightedAverageCoolingOnlyLoadSum + " weightedAverageHeatingOnlyLoadSum " + weightedAverageHeatingOnlyLoadSum);
                }
            }
        
        }
        
        if (prioritySum == 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "No valid temperature, Skip VavSystemControlAlgo");
            systemState = OFF;
            return;
        }
        
        weightedAverageCoolingOnlyLoad = weightedAverageCoolingOnlyLoadSum / prioritySum;
        weightedAverageHeatingOnlyLoad = weightedAverageHeatingOnlyLoadSum / prioritySum;
        weightedAverageLoad = weightedAverageLoadSum / prioritySum;
        
        comfortIndex = (int)(totalCoolingLoad + totalHeatingLoad) /zoneCount;
        
        profile.setSystemPoint("ci and running", comfortIndex);
    
        weightedAverageCoolingOnlyLoadPostML = weightedAverageCoolingOnlyLoad ;//+buildingLoadOffsetML
        weightedAverageHeatingOnlyLoadPostML = weightedAverageHeatingOnlyLoad ;//+buildingLoadOffsetML
        weightedAverageLoadPostML = weightedAverageLoad ;///+buildingLoadOffsetML
        
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
    
        if ((systemState == COOLING || systemState == OFF) && buildingLimitMaxBreached("vav")) {
            CcuLog.d(L.TAG_CCU_SYSTEM, " Emergency COOLING Active");
            emergencyMode = true;
            if (systemState != COOLING)
            {
                systemState = COOLING;
                piController.reset();
            }
        } else if ((systemState == HEATING || systemState == OFF) && buildingLimitMinBreached("vav")) {
            CcuLog.d(L.TAG_CCU_SYSTEM, " Emergency HEATING Active");
            emergencyMode = true;
            if (systemState != HEATING)
            {
                systemState = HEATING;
                piController.reset();
            }
        } else
        {
            if (emergencyMode) {
                CcuLog.d(L.TAG_CCU_SYSTEM, " Emergency CONDITIONING Disabled");
                piController.reset();
                emergencyMode = false;
            }
            if ((systemMode == COOLONLY || systemMode == AUTO) && weightedAverageCoolingOnlyLoadMA > 0)
            {
                if (systemState != COOLING)
                {
                    systemState = COOLING;
                    piController.reset();
                }
            }
            else if ((systemMode == HEATONLY || systemMode == AUTO) && (weightedAverageCoolingOnlyLoadMA == 0 && weightedAverageHeatingOnlyLoadMA > 0))
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
        
        piController.dump();
        if (systemState == COOLING) {
            heatingSignal = 0;
            coolingSignal = (int)piController.getLoopOutput(weightedAverageCoolingOnlyLoadPostML, 0);
        } else if (systemState == HEATING){
            coolingSignal = 0;
            heatingSignal = (int)piController.getLoopOutput(weightedAverageHeatingOnlyLoadPostML, 0);
        } else {
            coolingSignal = 0;
            heatingSignal = 0;
        }
        piController.dump();
        CcuLog.d(L.TAG_CCU_SYSTEM, "weightedAverageCoolingOnlyLoadMA: "+weightedAverageCoolingOnlyLoadMA+" weightedAverageHeatingOnlyLoadMA: "
                                                    +weightedAverageHeatingOnlyLoadMA +" systemState: "+systemState+" coolingSignal: "+coolingSignal+" heatingSignal: "+heatingSignal);
    
        profile.setSystemPoint("moving and average and cooling and load",weightedAverageCoolingOnlyLoadMA);
        profile.setSystemPoint("moving and average and heating and load",weightedAverageHeatingOnlyLoadMA);
        
        if (systemState == HEATING)
        {
            normalizeAirflow();
            adjustDamperForCumulativeTarget(profile.getSystemEquipRef());
        } else {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav and zone");
            for (HashMap m : vavEquips) {
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
    
    public boolean isVavEquip(Equip q) {
        if (q.getProfile() == ProfileType.VAV_REHEAT.name() ||
            q.getProfile() == ProfileType.VAV_SERIES_FAN.name() ||
            q.getProfile() == ProfileType.VAV_PARALLEL_FAN.name()) {
            return true;
        }
        return false;
    }
    
    public double getEquipCurrentTemp(String equipRef) {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and equipRef == \""+equipRef+"\"");
    }
    
    public ZonePriority getEquipPriority(String equipRef) {
        double priorityVal = CCUHsApi.getInstance().readDefaultVal("point and zone and config and vav and priority and equipRef == \""+equipRef+"\"");
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
    
    public boolean isZoneDead(Equip q) {
    
        return CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and equipRef == \""+q.getId()+"\"")
                       .equals("Zone Temp Dead");
    }
    
    public double getEquipDynamicPriority(double zoneLoad, String equipRef) {
        ZonePriority p = getEquipPriority(equipRef);
        if (getEquipCurrentTemp(equipRef) == 0) {
            return p.val;
        }
    
        double zonePrioritySpread = TunerUtil.readTunerValByQuery("point and tuner and vav and zone and priority and spread and equipRef == \""+equipRef+"\"");;
        double zonePriorityMultiplier = TunerUtil.readTunerValByQuery("point and tuner and vav and zone and priority and multiplier and equipRef == \""+equipRef+"\"");;
        
        return p.val * Math.pow(zonePriorityMultiplier, (zoneLoad/zonePrioritySpread) > 10 ? 10 : (zoneLoad/zonePrioritySpread));
    }
    
    
    /*public double getZoneCurrentTemp(String zoneRef) {
        double tempSum = 0;
        int tempCount = 0;
        for (ZoneProfile p : L.ccu().zoneProfiles)
        {
            if (isVavEquip(p.getEquip()) && p.getEquip().getZoneRef().equals(zoneRef) && p.getCurrentTemp() > 0) {
                tempSum += p.getCurrentTemp();
                tempCount++;
            }
        }
        return tempCount > 0 ? tempSum/tempCount : 0;
    }
    
    public ZonePriority getZonePriority(String zoneRef) {
        ZonePriority priority = NONE;
        for (ZoneProfile p : L.ccu().zoneProfiles)
        {
            if (isVavEquip(p.getEquip()) && p.getEquip().getZoneRef().equals(zoneRef) && p.getPriority().ordinal() > priority.ordinal()) {
                priority = p.getPriority();
            }
        }
        return priority;
    }
    
    public double getZoneDesiredTemp(String zoneRef)
    {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and air and temp and desired and zoneRef == \""+zoneRef+"\"");
    
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
    
    public double getZoneTempTarget(String zoneRef) //Humidity compensated
    {
        return getZoneDesiredTemp(zoneRef);//TODO - TEMP
    }*/
    
    public void updateSystemHumidity() {
        //Average across zones or from proxy zone.
        double humiditySum = 0;
        double humidityZones = 0;
    
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav and zone");
    
        for (HashMap q : vavEquips)
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
        ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav and zone");
        
        for (HashMap q : vavEquips)
        {
            double tempVal = hayStack.readHisValByQuery("point and air and temp and sensor and current and equipRef == \""+q.get("id")+"\"");
            
            if (tempVal != 0) {
                tempSum += tempVal;
                tempZones++;
            }
        }
        averageSystemTemperature = tempZones == 0 ? 0 : tempSum/tempZones;
    }
    
    @Override
    public double getAverageSystemTemperature() {
        return averageSystemTemperature;
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
                zonePrioritySpread = TunerUtil.readTunerValByQuery("point and tuner and vav and zone and priority and spread and equipRef == \""+q.getId()+"\"");
                break;
            }
        }
    
        for (ZoneProfile z : L.ccu().zoneProfiles)
        {
            Equip q = z.getEquip();
            if (q.getZoneRef().equals(zoneRef)) {
                zonePriorityMultiplier = TunerUtil.readTunerValByQuery("point and tuner and vav and zone and priority and multiplier and equipRef == \""+q.getId()+"\"");
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
        ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav and zone");
        double maxDamperPos = 0;
        for (HashMap m : vavEquips) {
            HashMap damper = hayStack.read("point and damper and base and cmd and equipRef == \""+m.get("id").toString()+"\"");
            double damperPos = hayStack.readHisValById(damper.get("id").toString());
            if ( damperPos >= maxDamperPos) {
                maxDamperPos = damperPos;
            }
        }
        
        if (maxDamperPos == 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM," Abort normalizeAirflow : maxDamperPos = "+maxDamperPos);
        }
        
        double targetPercent = (100 - maxDamperPos) * 100/ maxDamperPos ;
        
    
        for (HashMap m : vavEquips) {
            HashMap damper = hayStack.read("point and damper and base and cmd and equipRef == \""+m.get("id").toString()+"\"");
            double damperPos = hayStack.readHisValById(damper.get("id").toString());
            int normalizedDamperPos = (int) (damperPos + damperPos * targetPercent/100);
            HashMap normalizedDamper = hayStack.read("point and damper and normalized and cmd and equipRef == \""+m.get("id").toString()+"\"");
            CcuLog.d(L.TAG_CCU_SYSTEM,"normalizeAirflow"+"Equip: "+m.get("dis")+",damperPos :"+damperPos+"targetPercent:"+targetPercent+" normalizedDamper:"+normalizedDamperPos);
            hayStack.writeHisValById(normalizedDamper.get("id").toString(), (double)normalizedDamperPos);
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
        ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav and zone");
        int damperSizeSum = 0;
        int weightedDamperOpeningSum = 0;
        for (HashMap m : vavEquips) {
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
        ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav and zone");
        for (HashMap m : vavEquips) {
            HashMap damperPos = hayStack.read("point and damper and normalized and cmd and equipRef == \""+m.get("id").toString()+"\"");
            double damperPosVal = hayStack.readHisValById(damperPos.get("id").toString());
            double adjustedDamperPos = damperPosVal + (damperPosVal * percent) /100.0;
            hayStack.writeHisValById(damperPos.get("id").toString() , adjustedDamperPos);
        }
    }
    
    public void setDamperLimits() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav and zone");
        for (HashMap m : vavEquips) {
            HashMap damperPos = hayStack.read("point and damper and normalized and cmd and equipRef == \""+m.get("id").toString()+"\"");
            double limitedDamperPos = hayStack.readHisValById(damperPos.get("id").toString());
            double minLimit = 0, maxLimit = 0;
            if (getStatus(m.get("group").toString()) == ZoneState.COOLING.ordinal()) {
                minLimit = hayStack.readDefaultVal("point and zone and config and vav and min and damper and cooling and equipRef == \""+m.get("id").toString()+"\"");
                maxLimit = hayStack.readDefaultVal("point and zone and config and vav and max and damper and cooling and equipRef == \""+m.get("id").toString()+"\"");
            } else {
                minLimit = hayStack.readDefaultVal("point and zone and config and vav and min and damper and heating and equipRef == \""+m.get("id").toString()+"\"");
                maxLimit = hayStack.readDefaultVal("point and zone and config and vav and max and damper and heating and equipRef == \""+m.get("id").toString()+"\"");
            }
            CcuLog.d(L.TAG_CCU_SYSTEM,"setDamperLimits : Equip "+m.get("dis")+" minLimit "+minLimit+" maxLimit "+maxLimit);
            limitedDamperPos = Math.min(limitedDamperPos, maxLimit);
            limitedDamperPos = Math.max(limitedDamperPos, minLimit);
            hayStack.writeHisValById(damperPos.get("id").toString() , limitedDamperPos);
        }
    }
    
    public double getStatus(String nodeAddr) {
        return CCUHsApi.getInstance().readHisValByQuery("point and status and his and group == \""+nodeAddr+"\"");
    }
}
