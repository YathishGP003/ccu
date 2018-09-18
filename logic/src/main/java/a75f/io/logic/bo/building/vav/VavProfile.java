package a75f.io.logic.bo.building.vav;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;

import a75.io.algos.tr.TrimResetListener;
import a75.io.algos.tr.TrimResponseRequest;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.hvac.ParallelFanVavUnit;
import a75f.io.logic.bo.building.hvac.SeriesFanVavUnit;
import a75f.io.logic.bo.building.hvac.Valve;
import a75f.io.logic.bo.building.hvac.VavUnit;

import static a75f.io.logic.bo.building.vav.VavProfile.ZonePriority.LOW;
import static a75f.io.logic.bo.building.vav.VavProfile.ZoneState.COOLING;
import static a75f.io.logic.bo.building.vav.VavProfile.ZoneState.HEATING;

/**
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
    
    
    enum ZoneState {
        COOLING,
        HEATING,
        DEADBAND
    }
    
    enum ZonePriority {
        NO(0), LOW(1), MEDIUM(2), HIGH(3);
        
        int multiplier;
        ZonePriority(int m) {
            multiplier = m;
        }
    }

    ZoneState state = COOLING;
    ZonePriority priority = LOW;
    
    HashMap<Short, VAVLogicalMap> vavDeviceMap;
    SatResetListener satResetListener;
    CO2ResetListener co2ResetListener;
    SpResetListener spResetListener;
    HwstResetListener hwstResetListener;
    
    public VavProfile() {
        vavDeviceMap = new HashMap<>();
        satResetListener = new SatResetListener();
        co2ResetListener = new CO2ResetListener();
        spResetListener = new SpResetListener();
        hwstResetListener = new HwstResetListener();
    }
    
    /*@Override
    public void mapRegularUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t regularUpdateMessage)
    {
        
        double roomTemp = (float) regularUpdateMessage.update.roomTemperature.get() / 10.0f;
        double dischargeTemp = (float) regularUpdateMessage.update.airflow1Temperature.get() / 10.0f;
        double supplyAirTemp = (float) regularUpdateMessage.update.airflow2Temperature.get() / 10.0f;
        double co2 = (float) regularUpdateMessage.update.externalAnalogVoltageInput1.get();
        double sp =  (float) regularUpdateMessage.update.externalAnalogVoltageInput2.get();//TODO
    
        short nodeAddr = (short)regularUpdateMessage.update.smartNodeAddress.get();
        Log.d(TAG," RegularUpdate : rT :"+roomTemp+" dT :"+dischargeTemp+" sT :"+supplyAirTemp+" CO2: "+co2+" SN:"+nodeAddr);
        
        VAVLogicalMap currentDevice = vavDeviceMap.get(nodeAddr);
        if (currentDevice == null) {
            //When node is not added while constructing profile.
            addLogicalMap(nodeAddr);
            currentDevice = vavDeviceMap.get(nodeAddr);
        }
        currentDevice.setRoomTemp(roomTemp);
        currentDevice.setDischargeTemp(dischargeTemp);
        currentDevice.setSupplyAirTemp(supplyAirTemp);
        currentDevice.setCO2(co2);
        currentDevice.setStaticPressure(sp);
        
        if(mInterface != null)
        {
            mInterface.refreshView();
        }
    }*/
    
    public void addLogicalMap(short addr) {
        VAVLogicalMap deviceMap = new VAVLogicalMap(getProfileType(), addr);
        vavDeviceMap.put(addr, deviceMap);
        deviceMap.satResetRequest.setImportanceMultiplier(getZonePriority());
        deviceMap.co2ResetRequest.setImportanceMultiplier(getZonePriority());
        deviceMap.spResetRequest.setImportanceMultiplier(getZonePriority());
        deviceMap.hwstResetRequest.setImportanceMultiplier(getZonePriority());
    }
    
    public void addLogicalMapAndPoints(short addr) {
        VAVLogicalMap deviceMap = new VAVLogicalMap(getProfileType(), addr);
        deviceMap.createHaystackPoints();
        vavDeviceMap.put(addr, deviceMap);
        deviceMap.satResetRequest.setImportanceMultiplier(getZonePriority());
        deviceMap.co2ResetRequest.setImportanceMultiplier(getZonePriority());
        deviceMap.spResetRequest.setImportanceMultiplier(getZonePriority());
        deviceMap.hwstResetRequest.setImportanceMultiplier(getZonePriority());
    }
    
    @JsonIgnore
    @Override
    public void updateZonePoints() {
        Log.d(TAG, " Invalid VAV Unit Type");
    }
    
    protected void setDamperLimits(short node, Damper d) {
        
        VavProfileConfiguration config = (VavProfileConfiguration) getProfileConfiguration(node);
        switch (state) {
            case COOLING:
                d.minPosition = config.getMinDamperCooling();
                d.maxPosition = config.getMaxDamperCooliing();
                break;
            case HEATING:
                d.minPosition = config.getMinDamperHeating();
                d.maxPosition = config.getMaxDamperHeating();
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
        return mProfileConfiguration.get(address);
    }
    
    @JsonIgnore
    public TrimResponseRequest getSATRequest(short node) {
    
        if (vavDeviceMap.get(node) ==  null) {
            return null;
        }
        double roomTemp = vavDeviceMap.get(node).getRoomTemp();
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
    public int getImportanceMultiplier() {
        return priority.multiplier;
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
    public double getAverageZoneTemp()
    {
        double[] temperature = new double[vavDeviceMap.size()];
        int tempIndex = 0;
        for (short nodeAddress : vavDeviceMap.keySet())
        {
            if (vavDeviceMap.get(nodeAddress) ==  null) {
                continue;
            }
            if (vavDeviceMap.get(Short.valueOf(nodeAddress)).getRoomTemp() > 0)
            {
                temperature[tempIndex++] = vavDeviceMap.get(Short.valueOf(nodeAddress)).getRoomTemp();
            }
        }
        //Log.d("VAV","Average Zone Temp "+(tempIndex == 0 ? 0 : MathLib.mean(temperature)));
        
        //return tempIndex == 0 ? 0 : MathLib.mean(temperature);
        
        return 0;//TODO
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
            tsdata.put("currentTemp"+node,vavDeviceMap.get(node).getRoomTemp());
            tsdata.put("setTemp"+node,setTemp);
            tsdata.put("heatingLoopOp"+node,vavDeviceMap.get(node).getHeatingLoop().getLoopOutput());
            tsdata.put("coolingLoopOp"+node,vavDeviceMap.get(node).getCoolingLoop().getLoopOutput());
            tsdata.put("dischargeTemp"+node,vavDeviceMap.get(node).getDischargeTemp());
            tsdata.put("dischargeSp"+node,vavDeviceMap.get(node).getDischargeSp());
            tsdata.put("supplyAirTemp"+node,vavDeviceMap.get(node).getSupplyAirTemp());
            tsdata.put("reheatValve"+node,(double)vavDeviceMap.get(node).getVavUnit().reheatValve.currentPosition);
            tsdata.put("vavDamper"+node,(double)vavDeviceMap.get(node).getVavUnit().vavDamper.currentPosition);
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
        }
        
        return tsdata;
    }
    
    @JsonIgnore
    public int getZonePriority()
    {
        int priority = 0;
        for (short nodeAddress : mProfileConfiguration.keySet())
        {
            if (vavDeviceMap.get(nodeAddress) ==  null) {
                continue;
            }
            VavProfileConfiguration config = (VavProfileConfiguration) mProfileConfiguration.get(nodeAddress);
            if (config.getPriority() > priority) {
                priority = config.getPriority();
            }
            
        }
        return priority;
    }
    
    @JsonIgnore
    public int getMinDamperCooling()
    {
        int damperPos = 0;
        for (short nodeAddress : mProfileConfiguration.keySet())
        {
            VavProfileConfiguration config = (VavProfileConfiguration) mProfileConfiguration.get(nodeAddress);
            if (damperPos == 0 || config.getMinDamperCooling() > damperPos) {
                damperPos = config.getMinDamperCooling();
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
            if (damperPos == 0 || config.getMaxDamperCooliing() < damperPos) {
                damperPos = config.getMaxDamperCooliing();
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
            if (damperPos == 0 || config.getMinDamperHeating() > damperPos) {
                damperPos = config.getMinDamperHeating();
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
            if (damperPos == 0 || (config.getMaxDamperHeating() < damperPos)) {
                damperPos = config.getMaxDamperHeating();
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
