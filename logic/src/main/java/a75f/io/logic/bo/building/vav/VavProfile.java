package a75f.io.logic.bo.building.vav;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Set;

import a75.io.algos.tr.TrimResetListener;
import a75.io.algos.tr.TrimResponseRequest;
import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.hvac.ParallelFanVavUnit;
import a75f.io.logic.bo.building.hvac.SeriesFanVavUnit;
import a75f.io.logic.bo.building.hvac.Valve;
import a75f.io.logic.bo.building.hvac.VavUnit;

import static a75f.io.logic.bo.building.ZonePriority.NO;
import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.HEATING;

/**
 *
 * Created by samjithsadasivan on 5/31/18.
 */

public abstract class VavProfile extends ZoneProfile
{
    
    public static String TAG = VavProfile.class.getSimpleName().toUpperCase();
    public static final int MAX_DISCHARGE_TEMP = 90;
    public static final int HEATING_LOOP_OFFSET = 20;
    public static final int REHEAT_THRESHOLD_TEMP = 50;
    
    double  setTemp = 72.0; //TODO
    int deadBand = 1;
    
    public HashMap<Short, VAVLogicalMap> vavDeviceMap;
    SatResetListener satResetListener;
    CO2ResetListener co2ResetListener;
    SpResetListener spResetListener;
    HwstResetListener hwstResetListener;
    VavTRSystem trSystem = null;
    
    public VavProfile() {
        vavDeviceMap = new HashMap<>();
        satResetListener = new SatResetListener();
        co2ResetListener = new CO2ResetListener();
        spResetListener = new SpResetListener();
        hwstResetListener = new HwstResetListener();
        //initTRSystem();
    }
    
    public void initTRSystem() {
        trSystem = (VavTRSystem) L.ccu().systemProfile.trSystem;
        trSystem.getSystemSATTRProcessor().addTRListener(satResetListener);
        trSystem.getSystemCO2TRProcessor().addTRListener(co2ResetListener);
        trSystem.getSystemSpTRProcessor().addTRListener(spResetListener);
        trSystem.getSystemHwstTRProcessor().addTRListener(hwstResetListener);
    }
    
    public void updateTRResponse(short node) {
        if (trSystem == null) {
            initTRSystem();
        }
        VavTRSystem trSystem = (VavTRSystem) L.ccu().systemProfile.trSystem;
        trSystem.updateSATRequest(getSATRequest(node));
        trSystem.updateCO2Request(getCO2Requests(node));
        trSystem.updateSpRequest(getSpRequests(node));
        trSystem.updateHwstRequest(getHwstRequests(node));
    }
    
    /**
     * Only creates a run time instance of logical map to run the PI/TR logical loops.
     * @param addr
     */
    public void addLogicalMap(short addr) {
        VAVLogicalMap deviceMap = new VAVLogicalMap(getProfileType(), addr);
        vavDeviceMap.put(addr, deviceMap);
        deviceMap.satResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.co2ResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.spResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.hwstResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.setPITuners();
    }
    
    /**
     * When the profile is created first time , either via UI or from existing tagsMap
     * this method has to be called on the profile instance.
     * @param addr
     * @param config
     * @param floorRef
     * @param zoneRef
     */
    public void addLogicalMapAndPoints(short addr, VavProfileConfiguration config, String floorRef, String zoneRef) {
        VAVLogicalMap deviceMap = new VAVLogicalMap(getProfileType(), addr);
        deviceMap.createHaystackPoints(config, floorRef, zoneRef );
        vavDeviceMap.put(addr, deviceMap);
        deviceMap.satResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.co2ResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.spResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.hwstResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.setPITuners();
    }
    
    public void updateLogicalMapAndPoints(short addr, VavProfileConfiguration config) {
        VAVLogicalMap deviceMap = vavDeviceMap.get(addr);
        deviceMap.updateHaystackPoints(config);
    
        deviceMap.satResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.co2ResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.spResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.hwstResetRequest.setImportanceMultiplier(getPriority().multiplier);
    }
    
    
    @JsonIgnore
    @Override
    public void updateZonePoints() {
        Log.d(TAG, " Invalid VAV Unit Type");
    }
    
    protected void setDamperLimits(short node, Damper d) {
        VAVLogicalMap deviceMap = vavDeviceMap.get(node);
        switch (state) {
            case COOLING:
                d.minPosition = (int)deviceMap.getDamperLimit("cooling", "min");
                d.maxPosition = (int)deviceMap.getDamperLimit("cooling", "max");;
                break;
            case HEATING:
                d.minPosition = (int)deviceMap.getDamperLimit("heating", "min");;
                d.maxPosition = (int)deviceMap.getDamperLimit("heating", "max");;
                break;
            case DEADBAND:
                //TODO - ?
                break;
        }
    }
    
    @JsonIgnore
    public VavUnit getVavControls(short address) {
        return vavDeviceMap.get(address) != null ? vavDeviceMap.get(address).getVavUnit() : null;
    }
    
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return vavDeviceMap.get(address) != null ? vavDeviceMap.get(address).getProfileConfiguration() : null;
    }
    
    @JsonIgnore
    public TrimResponseRequest getSATRequest(short node) {
    
        if (vavDeviceMap.get(node) ==  null) {
            return null;
        }
        double roomTemp = vavDeviceMap.get(node).getCurrentTemp();
        TrimResponseRequest satResetRequest = vavDeviceMap.get(node).satResetRequest;
        
        if (state == COOLING) {
            if (vavDeviceMap.get(node).getCoolingLoop().getLoopOutput() < 85) {
                satResetRequest.currentRequests = 0;
            }
            if (vavDeviceMap.get(node).getCoolingLoop().getLoopOutput() > 95) {
                satResetRequest.currentRequests = 1;
            }
            if ((roomTemp - setTemp) >= 3) {//TODO - for 2 mins
                satResetRequest.currentRequests = 2;
            }
            if ((roomTemp - setTemp) >= 5) {//TODO - for 5 mins
                satResetRequest.currentRequests = 3;
            }
        } else {
            satResetRequest.currentRequests = 0;
        }
        satResetRequest.handleRequestUpdate();
        return satResetRequest;
    }
    
    @JsonIgnore
    public TrimResponseRequest getCO2Requests(short node)
    {
        if (vavDeviceMap.get(node) ==  null) {
            return null;
        }
        
        int co2LoopOp = vavDeviceMap.get(node).getCo2Loop().getLoopOutput();
        TrimResponseRequest co2ResetRequest = vavDeviceMap.get(node).co2ResetRequest;
        if (co2LoopOp == 100) {
            co2ResetRequest.currentRequests = 4;
        } else if (co2LoopOp > 90){
            co2ResetRequest.currentRequests = 3;
        } else if (co2LoopOp > 75) {
            co2ResetRequest.currentRequests = 2;
        } else if (co2LoopOp > 60) {
            co2ResetRequest.currentRequests = 1;
        } else if (co2LoopOp < 50) {
            co2ResetRequest.currentRequests = 0;
        }
        co2ResetRequest.handleRequestUpdate();
        return co2ResetRequest;
        
    }
    
    @JsonIgnore
    public TrimResponseRequest getSpRequests(short node)
    {
        if (vavDeviceMap.get(node) ==  null) {
            return null;
        }
        
        Damper d = vavDeviceMap.get(node).getVavUnit().vavDamper;
        int damperLoopOp = (d.currentPosition - d.co2CompensatedMinPos) * 100/ (d.maxPosition - d.co2CompensatedMinPos);
        
        TrimResponseRequest spResetRequest = vavDeviceMap.get(node).spResetRequest;
        if (damperLoopOp > 95) {
            spResetRequest.currentRequests = 3;
        } else if (damperLoopOp > 85){
            spResetRequest.currentRequests = 2;
        } else if (damperLoopOp > 75) {
            spResetRequest.currentRequests = 1;
        } else if (damperLoopOp < 65) {
            spResetRequest.currentRequests = 0;
        }
        spResetRequest.handleRequestUpdate();
        return spResetRequest;
    }
    
    @JsonIgnore
    public TrimResponseRequest getHwstRequests(short node)
    {
        if (vavDeviceMap.get(node) ==  null) {
            return null;
        }
    
        Valve v = vavDeviceMap.get(node).getVavUnit().reheatValve;
        double sat = vavDeviceMap.get(node).getSupplyAirTemp();
        TrimResponseRequest hwstRequest = vavDeviceMap.get(node).hwstResetRequest;
        
        if (state == HEATING)
        {
            if ((setTemp - sat) > 30)
            { // TODO- 5 mins
                hwstRequest.currentRequests = 3;
            }
            else if ((setTemp - sat) > 15)
            { //TODO - 5 mins
                hwstRequest.currentRequests = 2;
            }
            else if (v.currentPosition > 95)
            {
                hwstRequest.currentRequests = 1;
            }
            else if (v.currentPosition < 85)
            {
                hwstRequest.currentRequests = 0;
            }
        } else {
            hwstRequest.currentRequests = 0;
        }
        hwstRequest.handleRequestUpdate();
        return hwstRequest;
    }
    
    @JsonIgnore
    public int getConditioningMode() {
        return state.ordinal();
    }
    
    @JsonIgnore
    public void handleSystemReset() {
        Log.d("VAV","handleSystemReset");
    }
    
    @JsonIgnore
    public double getDisplayCurrentTemp()
    {
        return getAverageZoneTemp();
    }
    
    @JsonIgnore
    public double getSetTemp()
    {
        return setTemp;
    }
    
    
    @JsonIgnore
    @Override
    public double getAverageZoneTemp()
    {
        double tempTotal = 0;
        int nodeCount = 0;
        for (short nodeAddress : vavDeviceMap.keySet())
        {
            if (vavDeviceMap.get(nodeAddress) ==  null) {
                continue;
            }
            if (vavDeviceMap.get(Short.valueOf(nodeAddress)).getCurrentTemp() > 0)
            {
                tempTotal += vavDeviceMap.get(Short.valueOf(nodeAddress)).getCurrentTemp();
                nodeCount++;
            }
        }
        return nodeCount == 0 ? 0 : tempTotal/nodeCount;
    }
    
    @JsonIgnore
    public Set<Short> getNodeAddresses()
    {
        return vavDeviceMap.keySet();
    }
    
    @Override
    public HashMap<String, Double> getTSData() {
        HashMap<String, Double> tsdata = new HashMap<>();
    
        for (short node : getNodeAddresses())
        {
            if (vavDeviceMap.get(node) ==  null) {
                continue;
            }
            tsdata.put("SAT-requestHours"+node,vavDeviceMap.get(node).satResetRequest.requestHours);
            tsdata.put("heatingLoopOp"+node,vavDeviceMap.get(node).getHeatingLoop().getLoopOutput());
            tsdata.put("coolingLoopOp"+node,vavDeviceMap.get(node).getCoolingLoop().getLoopOutput());
            tsdata.put("dischargeSp"+node,vavDeviceMap.get(node).getDischargeSp());
            tsdata.put("supplyAirTemp"+node,vavDeviceMap.get(node).getSupplyAirTemp());
            if (getProfileType() == ProfileType.VAV_SERIES_FAN) {
                double fanStart = ((SeriesFanVavUnit)vavDeviceMap.get(node).getVavUnit()).fanStart ? 100 : 0;
                tsdata.put("fanStart"+node, fanStart);
            } else if (getProfileType() == ProfileType.VAV_PARALLEL_FAN) {
                double fanStart = ((ParallelFanVavUnit)vavDeviceMap.get(node).getVavUnit()).fanStart ? 100 : 0;
                tsdata.put("fanStart"+node, fanStart);
            }
            tsdata.put("CO2"+node,vavDeviceMap.get(node).getCO2());
            tsdata.put("co2LoopOp"+node,(double)vavDeviceMap.get(node).getCo2Loop().getLoopOutput());
            tsdata.put("CO2-requestHours"+node,vavDeviceMap.get(node).co2ResetRequest.requestHours);
            tsdata.put("SP"+node,vavDeviceMap.get(node).getStaticPressure());
            tsdata.put("SP-requestHours"+node,vavDeviceMap.get(node).spResetRequest.requestHours);
            tsdata.put("HWST-requestHours"+node,vavDeviceMap.get(node).hwstResetRequest.requestHours);
    
            tsdata.put("setTemp"+node,setTemp);
        }
        
        return tsdata;
    }
    
    @JsonIgnore
    @Override
    public ZonePriority getPriority()
    {
        ZonePriority priority = NO;
        for (short nodeAddress : vavDeviceMap.keySet())
        {
            HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+nodeAddress+"\"");
            if (ZonePriority.valueOf(equip.get("priority").toString()).ordinal() > priority.ordinal()) {
                priority = ZonePriority.valueOf(equip.get("priority").toString());
            }
        }
        return priority;
    }
    
    @JsonIgnore
    @Override
    public double getCurrentTemp() {
        for (short nodeAddress : vavDeviceMap.keySet())
        {
            return vavDeviceMap.get(nodeAddress).getCurrentTemp();
        }
        return 0;
    }
    
    @JsonIgnore
    @Override
    public Equip getEquip()
    {
        for (short nodeAddress : vavDeviceMap.keySet())
        {
            HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+nodeAddress+"\"");
            return new Equip.Builder().setHashMap(equip).build();
        }
        return null;
    }
    
    
    @JsonIgnore
    public int getMinDamperCooling()
    {
        int damperPos = 0;
        for (short nodeAddress : mProfileConfiguration.keySet())
        {
            VavProfileConfiguration config = (VavProfileConfiguration) mProfileConfiguration.get(nodeAddress);
            if (damperPos == 0 || config.minDamperCooling > damperPos) {
                damperPos = config.minDamperCooling;
            }
            
        }
        return damperPos;
    }
    
    @JsonIgnore
    public int getMaxDamperCooliing()
    {
        int damperPos = 0;
        for (short nodeAddress : mProfileConfiguration.keySet())
        {
            VavProfileConfiguration config = (VavProfileConfiguration) mProfileConfiguration.get(nodeAddress);
            if (damperPos == 0 || config.maxDamperCooling < damperPos) {
                damperPos = config.maxDamperCooling;
            }
            
        }
        return damperPos;
    }
    
    @JsonIgnore
    public int getMinDamperHeating()
    {
        int damperPos = 0;
        for (short nodeAddress : mProfileConfiguration.keySet())
        {
            VavProfileConfiguration config = (VavProfileConfiguration) mProfileConfiguration.get(nodeAddress);
            if (damperPos == 0 || config.minDamperHeating > damperPos) {
                damperPos = config.minDamperHeating;
            }
            
        }
        return damperPos;
    }
    
    @JsonIgnore
    public int getMaxDamperHeating()
    {
        int damperPos = 0;
        for (short nodeAddress : mProfileConfiguration.keySet())
        {
            VavProfileConfiguration config = (VavProfileConfiguration) mProfileConfiguration.get(nodeAddress);
            if (damperPos == 0 || (config.maxDamperHeating < damperPos)) {
                damperPos = config.maxDamperHeating;
            }
            
        }
        return damperPos;
    }
    
    class SatResetListener implements TrimResetListener {
        public void handleSystemReset() {
            Log.d("VAV","handleSATReset");
            for (short node : getNodeAddresses())
            {
                vavDeviceMap.get(node).satResetRequest.handleReset();
            }
        }
    }
    
    @JsonIgnore
    public SatResetListener getSatResetListener() {
        return satResetListener;
    }
    
    class CO2ResetListener implements TrimResetListener {
        public void handleSystemReset() {
            Log.d("VAV","handleCO2Reset");
            for (short node : getNodeAddresses())
            {
                vavDeviceMap.get(node).co2ResetRequest.handleReset();
            }
        }
    }
    
    @JsonIgnore
    public CO2ResetListener getCO2ResetListener() {
        return co2ResetListener;
    }
    
    class SpResetListener implements TrimResetListener {
        public void handleSystemReset() {
            Log.d("VAV","handleSPReset");
            for (short node : getNodeAddresses())
            {
                vavDeviceMap.get(node).spResetRequest.handleReset();
            }
        }
    }
    
    @JsonIgnore
    public SpResetListener getSpResetListener() {
        return spResetListener;
    }
    
    class HwstResetListener implements TrimResetListener {
        public void handleSystemReset() {
            Log.d("VAV","handleHWSTReset");
            for (short node : getNodeAddresses())
            {
                vavDeviceMap.get(node).hwstResetRequest.handleReset();
            }
        }
    }
    
    @JsonIgnore
    public HwstResetListener getHwstResetListener() {
        return hwstResetListener;
    }
    
}
