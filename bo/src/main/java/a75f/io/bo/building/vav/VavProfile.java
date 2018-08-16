package a75f.io.bo.building.vav;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.javolution.lang.MathLib;

import java.util.HashMap;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75.io.algos.TrimResetListener;
import a75.io.algos.TrimResponseRequest;
import a75f.io.bo.building.BaseProfileConfiguration;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.building.hvac.Damper;
import a75f.io.bo.building.hvac.Valve;
import a75f.io.bo.building.hvac.VavUnit;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

import static a75f.io.bo.building.vav.VavProfile.ZonePriority.LOW;
import static a75f.io.bo.building.vav.VavProfile.ZoneState.COOLING;
import static a75f.io.bo.building.vav.VavProfile.ZoneState.DEADBAND;
import static a75f.io.bo.building.vav.VavProfile.ZoneState.HEATING;

/**
 * Created by samjithsadasivan on 5/31/18.
 */

public class VavProfile extends ZoneProfile implements TrimResetListener
{
    
    private static String TAG = VavProfile.class.getSimpleName().toUpperCase();
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
    
    public VavProfile() {
        vavDeviceMap = new HashMap<>();
        satResetListener = new SatResetListener();
        co2ResetListener = new CO2ResetListener();
        spResetListener = new SpResetListener();
    }
    
    @Override
    public void mapRegularUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t regularUpdateMessage)
    {
        
        double roomTemp = (float) regularUpdateMessage.update.roomTemperature.get() / 10.0f;
        double dischargeTemp = (float) regularUpdateMessage.update.airflow1Temperature.get() / 10.0f;
        double supplyAirTemp = (float) regularUpdateMessage.update.airflow2Temperature.get() / 10.0f;
        double co2 = (float) regularUpdateMessage.update.externalAnalogVoltageInput1.get();
        double sp =  (float) regularUpdateMessage.update.externalAnalogVoltageInput2.get();//TODO
    
        Log.d(TAG," RegularUpdate : rT :"+roomTemp+" dT :"+dischargeTemp+" sT :"+supplyAirTemp+" CO2: "+co2+" SN:"+regularUpdateMessage.update.smartNodeAddress.get());
        VAVLogicalMap currentDevice = vavDeviceMap.get((short)regularUpdateMessage.update.smartNodeAddress.get());
        if (currentDevice == null) {
            currentDevice = new VAVLogicalMap();
            vavDeviceMap.put((short)regularUpdateMessage.update.smartNodeAddress.get(), currentDevice);
            currentDevice.satResetRequest.setImportanceMultiplier(getZonePriority());
            currentDevice.co2ResetRequest.setImportanceMultiplier(getZonePriority());
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
    }
    
    //VAV damper and reheat coil control logic is implemented according to section 1.3.E.6 of
    //ASHRAE RP-1455: Advanced Control Sequences for HVAC Systems Phase I, Air Distribution and Terminal Systems
    @JsonIgnore
    @Override
    public void updateZoneControls(double desiredTemp) {
    
        setTemp = desiredTemp;
        
        for (short node : getNodeAddresses())
        {
            if (vavDeviceMap.get(node) == null) {
                Log.d(TAG," Logical Map does not exist for node "+node);
                continue;
            }
            VAVLogicalMap vavDevice = vavDeviceMap.get(node);
            ControlLoop coolingLoop = vavDevice.getCoolingLoop();
            ControlLoop heatingLoop = vavDevice.getHeatingLoop();
            CO2Loop co2Loop = vavDeviceMap.get(node).getCo2Loop();
            VavUnit vavUnit = vavDevice.getVavUnit();
            GenericPIController valveController = vavDevice.getValveController();
    
            double roomTemp = vavDevice.getRoomTemp();
            double dischargeTemp = vavDevice.getDischargeTemp();
            double supplyAirTemp = vavDevice.getSupplyAirTemp();
            double co2 = vavDeviceMap.get(node).getCO2();
            double dischargeSp = vavDevice.getDischargeSp();
            
            if (roomTemp == 0) {
                Log.d(TAG,"Skip PI update for "+node+" roomTemp : "+roomTemp);
                continue;
            }
            
            
            Damper damper = vavUnit.vavDamper;
            Valve valve = vavUnit.reheatValve;
            int loopOp;//New value of loopOp
            //TODO
            //If supply air temperature from air handler is greater than room temperature, Cooling shall be
            //locked out.
            if (roomTemp > (setTemp + deadBand))
            {
                //Zone is in Cooling
                if (state != COOLING)
                {
                    state = COOLING;
                    coolingLoop.setEnabled();
                    heatingLoop.setDisabled();
                }
                int coolingOp = (int) coolingLoop.getLoopOutput(roomTemp, setTemp+deadBand);
                loopOp = coolingOp;
                valveController.reset();
            }
            else if (roomTemp < (setTemp - deadBand))
            {
                //Zone is in heating
                if (state != HEATING)
                {
                    state = HEATING;
                    heatingLoop.setEnabled();
                    coolingLoop.setDisabled();
                }
                
                int heatingLoopOp = (int) heatingLoop.getLoopOutput(setTemp-deadBand, roomTemp);
                
                if (heatingLoopOp <= 50)
                {
                    //Control reheat valve when heating loop is <=50
                    double datMax = (roomTemp + HEATING_LOOP_OFFSET) > MAX_DISCHARGE_TEMP ? MAX_DISCHARGE_TEMP : (roomTemp + HEATING_LOOP_OFFSET);
                    dischargeSp = supplyAirTemp + (datMax - supplyAirTemp) * heatingLoopOp/50;
                    vavDevice.setDischargeSp(dischargeSp);
                    valveController.updateControlVariable(dischargeSp, dischargeTemp);
                    valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                    loopOp = 0;
                    Log.d(TAG,"dischargeTempSP: "+dischargeSp);
                }
                else
                {
                    //Control airflow when heating loop is 51-100
                    //Also update valve control to account for change in dischargeTemp
                    if (dischargeSp == 0) {
                        double datMax = (roomTemp + HEATING_LOOP_OFFSET) > MAX_DISCHARGE_TEMP ? MAX_DISCHARGE_TEMP : (roomTemp + HEATING_LOOP_OFFSET);
                        dischargeSp = supplyAirTemp + (datMax - supplyAirTemp) * heatingLoopOp/100;
                        vavDevice.setDischargeSp(dischargeSp);
                    }
                    valveController.updateControlVariable(dischargeSp, dischargeTemp);
                    valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                    loopOp = heatingLoopOp;
                }
    
            }
            else
            {
                //Zone is in deadband
                state = DEADBAND;
                loopOp = 0;
                valveController.reset();
                heatingLoop.setDisabled();
                coolingLoop.setDisabled();
            }
            
            if (valveController.getControlVariable() == 0)
            {
                valve.currentPosition = 0;
            }
            
            setDamperLimits(node, damper);
            
            //CO2 loop output from 0-50% modulates damper min position.
            if (/*mode == OCCUPIED && */co2Loop.getLoopOutput(co2) <= 50)
            {
                damper.co2CompensatedMinPos = damper.minPosition + (damper.maxPosition - damper.minPosition) * co2Loop.getLoopOutput() / 50;
                Log.d("VAV","CO2LoopOp :"+co2Loop.getLoopOutput()+", adjusted minposition "+damper.minPosition);
            }
            
            if (loopOp == 0)
            {
                damper.currentPosition = damper.co2CompensatedMinPos;
            }
            else
            {
                damper.currentPosition = damper.co2CompensatedMinPos + (damper.maxPosition - damper.co2CompensatedMinPos) * loopOp / 100;
            }
            Log.d(TAG, "STATE :"+state+" ,loopOp: " + loopOp + " ,damper:" + damper.currentPosition+", valve:"+valve.currentPosition);
            
            //In any Mode except Unoccupied, the hot water valve shall be
            //modulated to maintain a supply air temperature no lower than 50°F.
            if (state != HEATING && supplyAirTemp < REHEAT_THRESHOLD_TEMP/* && mode != UNOCCUPIED*/)
            {
                valveController.updateControlVariable(REHEAT_THRESHOLD_TEMP, supplyAirTemp);
                valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                Log.d(TAG, "SAT below threshold valve :  " + valve.currentPosition);
            }
            
            //Normalize
            damper.normalize();
            valve.normalize();
        }
    }
    
    private void setDamperLimits(short node, Damper d) {
        
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
    
    @JsonIgnore
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.VAV;
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
        for (short node : getNodeAddresses())
        {
            //TODO - Should be done separately
            vavDeviceMap.get(node).satResetRequest.handleReset();
            vavDeviceMap.get(node).co2ResetRequest.handleReset();
        }
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
        Log.d("VAV","Average Zone Temp "+(tempIndex == 0 ? 0 : MathLib.mean(temperature)));
        
        return tempIndex == 0 ? 0 : MathLib.mean(temperature);
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
            tsdata.put("CO2"+node,vavDeviceMap.get(node).getCO2());
            tsdata.put("co2LoopOp"+node,(double)vavDeviceMap.get(node).getCo2Loop().getLoopOutput());
            tsdata.put("CO2-requestHours"+node,vavDeviceMap.get(node).co2ResetRequest.requestHours);
            tsdata.put("SP"+node,vavDeviceMap.get(node).getStaticPressure());
            tsdata.put("SP-requestHours"+node,vavDeviceMap.get(node).spResetRequest.requestHours);
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
            Log.d("VAV", "Zone priority "+nodeAddress+" = "+config.getPriority());
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
    
}
