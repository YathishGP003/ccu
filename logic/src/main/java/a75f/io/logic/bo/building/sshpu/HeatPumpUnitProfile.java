package a75f.io.logic.bo.building.sshpu;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.SmartStatFanRelayType;
import a75f.io.logic.bo.building.definitions.SmartStatHeatPumpChangeOverType;
import a75f.io.logic.bo.building.definitions.StandaloneFanSpeed;
import a75f.io.logic.bo.building.definitions.StandaloneOperationalMode;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.jobs.StandaloneScheduler;
import a75f.io.logic.tuners.StandaloneTunerUtil;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.definitions.StandaloneFanSpeed.OFF;

public class HeatPumpUnitProfile extends ZoneProfile {


    public static String TAG = HeatPumpUnitEquip.class.getSimpleName().toUpperCase();

    public HashMap<Short, HeatPumpUnitEquip> hpuDeviceMap;
    double setTempCooling = 74.0;
    double setTempHeating = 70.0;

    public HeatPumpUnitProfile() {

        hpuDeviceMap = new HashMap<>();
    }

    @JsonIgnore
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SMARTSTAT_HEAT_PUMP_UNIT;
    }

    @JsonIgnore
    @Override
    public void updateZonePoints() {

        if (mInterface != null) {
            mInterface.refreshView();
        }

        for (short node : hpuDeviceMap.keySet()) {
            if (hpuDeviceMap.get(node) == null) {
                addLogicalMap(node);
                Log.d(TAG, " Logical Map added for smartstat " + node);
                continue;
            }
            Log.d(TAG, "SmartStat HPU profile");

            //HashMap<String, Integer> relayStages = new HashMap<String, Integer>();
            HeatPumpUnitEquip hpuDevice = hpuDeviceMap.get(node);
            if (hpuDevice.profileType != ProfileType.SMARTSTAT_HEAT_PUMP_UNIT)
                continue;
            double roomTemp = hpuDevice.getCurrentTemp();
            Equip hpuEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
            if (roomTemp == 0) {
                resetRelays(null,node);
                hpuDevice.setStatus(state.ordinal());
                String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \"" + node + "\"");
                if (!curStatus.equals("Zone Temp Dead")) {
                    CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"", "Zone Temp Dead");
                }
                Log.d(TAG, "Invalid Temp , skip controls update for " + node + " roomTemp : " + hpuDeviceMap.get(node).getCurrentTemp());
                continue;
            }else {
                //TODO need to Fetch building limits once done on Building tuners page
                double minCoolingBL = 55.0;
                double maxHeatingBL = 90.0;

                if(roomTemp < (minCoolingBL - 10) || (roomTemp > (maxHeatingBL + 10))){
                    resetRelays(null, node);
                    hpuDevice.setStatus(state.ordinal());
                    String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \"" + node + "\"");
                    if (!curStatus.equals("Zone Temp Dead")) {
                        CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"", "Zone Temp Dead");
                    }
                    continue;

                }/*else if((roomTemp < minCoolingBL) || (roomTemp > maxHeatingBL )){
                    resetRelays(hpuEquip.getId(), node);
                    hpuDevice.setStatus(state.ordinal());
                    String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \"" + node + "\"");
                    if (!curStatus.equals("Zone Temp Dead")) {
                        CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"", "Zone Temp Dead");
                    }
                    continue;
                }*/
            }
            setTempCooling = hpuDevice.getDesiredTempCooling();
            setTempHeating = hpuDevice.getDesiredTempHeating();
            double averageDesiredTemp = (setTempCooling + setTempHeating) / 2.0;
            if (averageDesiredTemp != hpuDevice.getDesiredTemp()) {
                hpuDevice.setDesiredTemp(averageDesiredTemp);
            }

            Log.d(TAG, " smartstat hpu, updates 111=" + hpuEquip.getRoomRef() + "," + setTempHeating + "," + setTempCooling+","+roomTemp);

            String zoneId = HSUtil.getZoneIdFromEquipId(hpuEquip.getId());
            Occupied occuStatus = ScheduleProcessJob.getOccupiedModeCache(zoneId);

            boolean occupied = (occuStatus == null ? false : occuStatus.isOccupied());
            //For dual temp but for single mode we use tuners

            double ssOperatingMode = getOperationalModes("temp", hpuEquip.getId());
            double ssFanOpMode = getOperationalModes("fan", hpuEquip.getId());
            StandaloneOperationalMode opMode = StandaloneOperationalMode.values()[(int) ssOperatingMode];
            StandaloneFanSpeed fanSpeed = StandaloneFanSpeed.values()[(int) ssFanOpMode];
            if (!occupied && (fanSpeed != OFF)) {
                if (fanSpeed != StandaloneFanSpeed.AUTO) {
                    StandaloneScheduler.updateOperationalPoints(hpuEquip.getId(), "fan", StandaloneFanSpeed.AUTO.ordinal());
                    fanSpeed = StandaloneFanSpeed.AUTO;
                }
            }
            hpuDevice.setProfilePoint("occupancy and status", occupied ? 1 : 0);
            Log.d(TAG, " smartstat hpu, updates =" + node+","+roomTemp+","+occupied+","+occuStatus.getCoolingDeadBand()+","+state+","+occuStatus.getCoolingVal()+","+occuStatus.getHeatingVal());

            if((fanSpeed != OFF) ){
                switch (opMode){
                    case AUTO:
                        if(roomTemp > averageDesiredTemp){
                            state = COOLING;
                            hpuCoolOnlyMode(hpuDevice, hpuEquip.getId(), roomTemp,setTempCooling,occuStatus,node,fanSpeed);
                        }else if(roomTemp < averageDesiredTemp){
                            state = HEATING;
                            hpuHeatOnlyMode(hpuDevice,hpuEquip.getId(),roomTemp,setTempHeating,occuStatus,node,fanSpeed);
                        }else {
                            state = DEADBAND;
                        }
                        break;
                    case COOL_ONLY:
                        if(roomTemp > averageDesiredTemp){
                            state = COOLING;
                            hpuCoolOnlyMode(hpuDevice,hpuEquip.getId(),roomTemp,setTempCooling,occuStatus,node,fanSpeed);
                        }
                        break;
                    case HEAT_ONLY:
                        if(roomTemp < averageDesiredTemp){
                            state = HEATING;
                            hpuHeatOnlyMode(hpuDevice,hpuEquip.getId(),roomTemp,setTempHeating,occuStatus,node,fanSpeed);
                        }
                        break;
                    case OFF:
                        switch (fanSpeed){
                            case AUTO:
                                resetRelays(hpuEquip.getId(),node);
                                break;
                            case FAN_LOW:
                                if(occupied) {
                                    setCmdSignal("fan and stage1", 1.0, node);
                                    HashMap<String,Integer> relayStages = new HashMap<String,Integer>();
                                    relayStages.put("FanStage1",1);
                                    relayStages.put("FanStage2",1);
                                    StandaloneScheduler.updateSmartStatStatus(hpuEquip.getId(),state, relayStages);
                                }

                                break;
                            case FAN_HIGH:
                                if(occupied) {
                                    setCmdSignal("fan and stage1", 1.0, node);
                                    setCmdSignal("fan and stage2",1.0,node);

                                    HashMap<String,Integer> relayStages = new HashMap<String,Integer>();
                                    relayStages.put("FanStage1",1);
                                    relayStages.put("FanStage2",1);
                                    StandaloneScheduler.updateSmartStatStatus(hpuEquip.getId(),state, relayStages);
                                }
                                break;
                        }
                        break;
                }
            }else {
                //No condition happens
                state = DEADBAND;
                resetRelays(hpuEquip.getId(),node);
            }


            hpuDevice.setProfilePoint("temp and conditioning and mode",state.ordinal());
            if(hpuDevice.getStatus() != state.ordinal())
                hpuDevice.setStatus(state.ordinal());
        }
    }

    @Override
    public ZoneState getState() {
        return state;
    }

    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address) {

        return hpuDeviceMap.get(address) != null ? hpuDeviceMap.get(address).getProfileConfiguration() : null;
    }

    /**
     * Only creates a run time instance of logical map for initialize.
     *
     * @param addr
     */
    public void addLogicalMap(short addr) {
        HeatPumpUnitEquip deviceMap = new HeatPumpUnitEquip(getProfileType(), addr);
        hpuDeviceMap.put(addr, deviceMap);
    }

    /**
     * Only creates a run time instance of logical map for initialize.
     *
     * @param addr
     */
    public void addLogicalMap(short addr, String roomRef) {
        HeatPumpUnitEquip deviceMap = new HeatPumpUnitEquip(getProfileType(), addr);
        hpuDeviceMap.put(addr, deviceMap);
    }


    /**
     * When the profile is created first time , either via UI or from existing tagsMap
     * this method has to be called on the profile instance.
     *
     * @param addr
     * @param config
     * @param floorRef
     * @param zoneRef
     */
    public void addLogicalMapAndPoints(short addr, HeatPumpUnitConfiguration config, String floorRef, String zoneRef) {
        HeatPumpUnitEquip deviceMap = new HeatPumpUnitEquip(getProfileType(), addr);
        deviceMap.createHaystackPoints(config, floorRef, zoneRef);

        hpuDeviceMap.put(addr, deviceMap);
    }

    public void updateLogicalMapAndPoints(short addr, HeatPumpUnitConfiguration config, String roomRef) {
        HeatPumpUnitEquip deviceMap = hpuDeviceMap.get(addr);
        deviceMap.updateHaystackPoints(config);
    }

    @JsonIgnore
    public Set<Short> getNodeAddresses() {
        return hpuDeviceMap.keySet();
    }

    @JsonIgnore
    @Override
    public Equip getEquip() {
        for (short nodeAddress : hpuDeviceMap.keySet()) {
            HashMap equip = CCUHsApi.getInstance().read("equip and group == \"" + nodeAddress + "\"");
            return new Equip.Builder().setHashMap(equip).build();
        }
        return null;
    }

    @JsonIgnore
    @Override
    public double getCurrentTemp() {
        for (short nodeAddress : hpuDeviceMap.keySet()) {
            return hpuDeviceMap.get(nodeAddress).getCurrentTemp();
        }
        return 0;
    }

    @JsonIgnore
    public double getDisplayCurrentTemp() {
        return getAverageZoneTemp();
    }


    @JsonIgnore
    @Override
    public double getAverageZoneTemp() {
        double tempTotal = 0;
        int nodeCount = 0;
        for (short nodeAddress : hpuDeviceMap.keySet()) {
            if (hpuDeviceMap.get(nodeAddress) == null) {
                continue;
            }
            if (hpuDeviceMap.get(Short.valueOf(nodeAddress)).getCurrentTemp() > 0) {
                tempTotal += hpuDeviceMap.get(Short.valueOf(nodeAddress)).getCurrentTemp();
                nodeCount++;
            }
        }
        return nodeCount == 0 ? 0 : tempTotal / nodeCount;
    }


    public double getConfigEnabled(String config, short node) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and " + config + " and group == \"" + node + "\"");

    }
    public double getConfigType(String config, short node) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and type and " + config + " and group == \"" + node + "\"");

    }

    public double getCmdSignal(String cmd, short node) {
        return CCUHsApi.getInstance().readHisValByQuery("point and standalone and hpu and cmd and his and " + cmd + " and group == \"" + node + "\"");
    }

    public void setCmdSignal(String cmd, double val, short node) {
        CCUHsApi.getInstance().writeHisValByQuery("point and standalone and hpu and cmd and his and " + cmd + " and group == \"" + node + "\"", val);
    }

    public double getOperationalModes(String cmd, String equipRef) {
        return CCUHsApi.getInstance().readHisValByQuery("point and standalone and operation and mode and his and " + cmd + " and equipRef == \"" + equipRef + "\"");
    }

    private void resetRelays(String equipId, short node) {

        if (getCmdSignal("compressor and stage1", node) > 0)
            setCmdSignal("compressor and stage1", 0, node);
        if (getCmdSignal("compressor and stage2", node) > 0)
            setCmdSignal("compressor and stage2", 0, node);
        if (getCmdSignal("aux and heating ", node) > 0)
            setCmdSignal("aux and heating ", 0, node);
        if (getCmdSignal("changeover and stage1", node) > 0)
            setCmdSignal("changeover and stage1", 0, node);
        if (getCmdSignal("fan and stage1", node) > 0)
            setCmdSignal("fan and stage1", 0, node);
        if (getCmdSignal("fan and stage2", node) > 0)
            setCmdSignal("fan and stage2", 0, node);
        StandaloneScheduler.updateSmartStatStatus(equipId, DEADBAND, new HashMap<String, Integer>());
    }

    private void hpuCoolOnlyMode(HeatPumpUnitEquip hpuEquip, String equipId, double curTemp, double coolingDesiredTemp, Occupied occupied,Short addr, StandaloneFanSpeed fanSpeed){


        double hysteresis = StandaloneTunerUtil.getStandaloneStage1Hysteresis(equipId);
        boolean isCompressorStage1Enabled = getConfigEnabled("relay1",addr) > 0 ? true : false;
        boolean isCompressorStage2Enabled = getConfigEnabled("relay2",addr) > 0 ? true : false;
        boolean isFanStage1Enabled = getConfigEnabled("relay3", addr) > 0 ? true : false; //relay3 for fan low
        boolean isFanRelay5Enabled = getConfigEnabled("relay5", addr) > 0 ? true : false; //relay5 for fan high
        boolean isAuxHeatingEnabled = getConfigEnabled("relay4",addr)> 0 ? true : false;//Aux Heating
        double humidifierTargetThreshold = 25.0;
        int fanStage2Type = (int)getConfigType("relay5",addr);
        SmartStatFanRelayType fanRelayType = SmartStatFanRelayType.values()[fanStage2Type];
        switch (fanRelayType){
            case HUMIDIFIER:
                humidifierTargetThreshold = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and humidity and equipRef == \"" + equipId + "\"");
                break;
            case DE_HUMIDIFIER:
                humidifierTargetThreshold = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and dehumidifier and equipRef == \"" + equipId + "\"");
                break;
        }
        int heatPumpChangeoverType = (int)getConfigType("relay6",addr);
        SmartStatHeatPumpChangeOverType hpChangeOverType = SmartStatHeatPumpChangeOverType.values()[heatPumpChangeoverType];
        HashMap<String, Integer> relayStages = new HashMap<String, Integer>();
        Log.d(TAG,"hpuCoolOnlyMode ="+occupied.isOccupied()+","+addr+","+isAuxHeatingEnabled+","+isCompressorStage1Enabled+","+isCompressorStage2Enabled+","+isFanStage1Enabled);
        if(isAuxHeatingEnabled && (getCmdSignal("aux and heating ",addr) > 0))setCmdSignal("aux and heating ",0,addr);
        if(curTemp >= coolingDesiredTemp){
            //Turn on relay1
            if(isCompressorStage1Enabled){
                relayStages.put("CoolingStage1",1);
                setCmdSignal("compressor and stage1", 1.0, addr);
            }else{
                setCmdSignal("compressor and stage1", 0, addr);
            }
            if(curTemp >= (coolingDesiredTemp + occupied.getCoolingDeadBand())){
                //Turn on Stage 2
                if(isCompressorStage2Enabled) {
                    relayStages.put("CoolingStage2", 1);
                    setCmdSignal("compressor and stage2", 1.0, addr);
                }else {
                    if(getCmdSignal("compressor and stage2", addr) > 0)setCmdSignal("compressor and stage2",0,addr);
                }
            }else if(curTemp <= coolingDesiredTemp){
                setCmdSignal("compressor and stage2",0,addr);
            }else{
                if(getCmdSignal("compressor and stage2",addr) > 0){
                    relayStages.put("CoolingStage2",1);
                }
            }

            switch (hpChangeOverType){
                case ENERGIZE_IN_COOLING:
                    relayStages.put("CoolingStage1",1);
                    setCmdSignal("changeover and stage1",1.0,addr);
                    break;
                    default:
                        setCmdSignal("changeover and stage1",0,addr);
                        break;
            }

            if(isFanStage1Enabled){
                switch (fanSpeed){
                    case AUTO:
                        if(relayStages.containsKey("CoolingStage1")) {
                            relayStages.put("FanStage1",1);
                            setCmdSignal("fan and stage1", 1.0, addr);
                        }else
                            setCmdSignal("fan and stage1",0,addr);
                        break;
                    case FAN_LOW:
                        if(relayStages.containsKey("CoolingStage1") || relayStages.containsKey("CoolingStage2") || occupied.isOccupied()) {
                            relayStages.put("FanStage1",1);
                            setCmdSignal("fan and stage1", 1.0, addr);
                        }else
                            setCmdSignal("fan and stage1",0,addr);
                        break;
                    case FAN_HIGH:
                        if(occupied.isOccupied() || relayStages.containsKey("CoolingStage1")) {
                            relayStages.put("FanStage1",1);
                            setCmdSignal("fan and stage1", 1.0, addr);
                        }else
                            setCmdSignal("fan and stage1",0,addr);
                        break;
                }
            }
            if(isFanRelay5Enabled){
                switch (fanRelayType){
                    case FAN_STAGE2:
                        switch (fanSpeed){
                            case AUTO:
                                if(relayStages.containsKey("CoolingStage2")) {
                                    relayStages.put("FanStage2",1);
                                    setCmdSignal("fan and stage2", 1.0, addr);
                                }else
                                    setCmdSignal("fan and stage2",0,addr);
                                break;
                            case FAN_LOW:
                                if(relayStages.containsKey("CoolingStage2") && occupied.isOccupied()) {
                                    relayStages.put("FanStage2",1);
                                    setCmdSignal("fan and stage2", 1.0, addr);
                                }else
                                    setCmdSignal("fan and stage2",0,addr);
                                break;
                            case FAN_HIGH:
                                if(occupied.isOccupied() || relayStages.containsKey("CoolingStage2")) {
                                    relayStages.put("FanStage2",1);
                                    setCmdSignal("fan and stage2", 1.0, addr);
                                }else
                                    setCmdSignal("fan and stage2",0,addr);
                                break;
                        }
                        break;
                }
            }
        }else{
            if(curTemp <= coolingDesiredTemp - hysteresis){
                //Turn off stage1
                setCmdSignal("compressor and stage1",0,addr);
                switch (hpChangeOverType){
                    case ENERGIZE_IN_COOLING:
                        if(getCmdSignal("changeover and stage1",addr) > 0)
                            setCmdSignal("changeover and stage1",0,addr);
                        break;
                        default:
                            setCmdSignal("changeover and stage1",0,addr);
                            break;
                }
                if(occupied.isOccupied()){
                    switch (fanSpeed){
                        case AUTO:
                            setCmdSignal("fan and stage1",0,addr);
                            setCmdSignal("fan and stage2",0,addr);
                            break;
                        case FAN_LOW:
                            relayStages.put("FanStage1",1);
                            setCmdSignal("fan and stage1",1.0,addr);
                            break;
                        case FAN_HIGH:

                            if(isFanStage1Enabled) {
                                relayStages.put("FanStage1", 1);
                                setCmdSignal("fan and stage1", 1.0, addr);
                            }
                            if(isFanRelay5Enabled) {
                                relayStages.put("FanStage2", 1);
                                setCmdSignal("fan and stage2", 1.0, addr);
                            }
                            break;
                    }

                }else {
                    setCmdSignal("fan and stage1", 0, addr);
                    setCmdSignal("fan and stage2",0,addr);
                }
            }else{

                if((getCmdSignal("compressor and stage1", addr) > 0) || (isFanRelay5Enabled && (getCmdSignal("changeover and stage1",addr) > 0)))
                    relayStages.put("CoolingStage1",1);
                if(getCmdSignal("fan and stage1", addr) > 0)relayStages.put("FanStage1",1);
            }
        }

        switch (fanRelayType){

            case HUMIDIFIER:
                if(hpuEquip.getHumidity() > humidifierTargetThreshold) {
                    relayStages.put("Humidifier",1);
                    setCmdSignal("fan and stage2", 1.0, addr);
                }else if(getCmdSignal("fan and stage2",addr) > 0){
                    if(hpuEquip.getHumidity() < (humidifierTargetThreshold - (humidifierTargetThreshold * 0.05)))
                        setCmdSignal("fan and stage2",0, addr);
                    else
                        relayStages.put("Humdifier",1);
                }
                break;
            case DE_HUMIDIFIER:
                if(hpuEquip.getHumidity() < humidifierTargetThreshold) {
                    setCmdSignal("fan and stage2", 1.0, addr);
                    relayStages.put("Dehumidifier",1);
                }else if(getCmdSignal("fan and stage2",addr) > 0){
                    if(hpuEquip.getHumidity() > (humidifierTargetThreshold +(humidifierTargetThreshold * 0.05)))
                        setCmdSignal("fan and stage2",0, addr);
                    else
                        relayStages.put("Dehumidifier",1);
                }
                break;
        }
        StandaloneScheduler.updateSmartStatStatus(equipId, state, relayStages);
    }
    private void hpuHeatOnlyMode(HeatPumpUnitEquip hpuEquip,String equipId, double curTemp, double heatingDesiredTemp, Occupied occupied,Short addr, StandaloneFanSpeed fanSpeed){
        double hysteresis = StandaloneTunerUtil.getStandaloneStage1Hysteresis(equipId);
        boolean isCompressorStage1Enabled = getConfigEnabled("relay1",addr) > 0 ? true : false;
        boolean isCompressorStage2Enabled = getConfigEnabled("relay2",addr) > 0 ? true : false;
        boolean isFanStage1Enabled = getConfigEnabled("relay3", addr) > 0 ? true : false; //relay3 for fan low
        boolean isFanRelay5Enabled = getConfigEnabled("relay5", addr) > 0 ? true : false; //relay5 for fan high
        boolean isAuxHeatingEnabled = getConfigEnabled("relay4",addr)> 0 ? true : false;//Aux Heating
        int fanStage2Type = (int)getConfigType("relay5",addr);
        SmartStatFanRelayType fanRelayType = SmartStatFanRelayType.values()[fanStage2Type];
        int heatPumpChangeoverType = (int)getConfigType("relay6",addr);
        double humidifierTargetThreshold = 25.0;//

        switch (fanRelayType){
            case HUMIDIFIER:
                humidifierTargetThreshold = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and humidity and equipRef == \"" + equipId + "\"");
                break;
            case DE_HUMIDIFIER:
                humidifierTargetThreshold = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and dehumidifier and equipRef == \"" + equipId + "\"");
                break;
        }
        SmartStatHeatPumpChangeOverType hpChangeOverType = SmartStatHeatPumpChangeOverType.values()[heatPumpChangeoverType];
        HashMap<String, Integer> relayStages = new HashMap<String, Integer>();
        if(curTemp <= heatingDesiredTemp){
            if(isAuxHeatingEnabled){
                if(curTemp <= (heatingDesiredTemp - (2 * occupied.getHeatingDeadBand())) ) {
                    relayStages.put("HeatingStage2", 1);
                    setCmdSignal("aux and heating ",1.0,addr);
                }else if(!isCompressorStage2Enabled && (curTemp <= heatingDesiredTemp - occupied.getHeatingDeadBand())){
                    relayStages.put("HeatingStage2", 1);
                    setCmdSignal("aux and heating ",1.0,addr);
                    setCmdSignal("compressor and stage2",0,addr);
                } else if(!isCompressorStage1Enabled && (curTemp <= heatingDesiredTemp)) {
                    relayStages.put("HeatingStage1", 1);
                    setCmdSignal("aux and heating ",1.0,addr);
                    setCmdSignal("compressor and stage1",0,addr);
                } else if(getCmdSignal("aux and heating ",addr)> 0){
                    if( isCompressorStage2Enabled && isCompressorStage2Enabled && (curTemp >= (heatingDesiredTemp - occupied.getHeatingDeadBand())))
                        setCmdSignal("aux and heating ",0,addr);
                    else if(isCompressorStage1Enabled && !isCompressorStage2Enabled && (curTemp >= heatingDesiredTemp))
                        setCmdSignal("aux and heating ",0,addr);
                    else {
                        relayStages.put("HeatingStage2",1);
                        relayStages.put("HeatingStage1",1);
                    }
                }
            }else
                setCmdSignal("aux and heating ",0,addr);
            //Turn on relay1
            if(isCompressorStage1Enabled){
                relayStages.put("HeatingStage1",1);
                setCmdSignal("compressor and stage1", 1.0, addr);

            }else{
                setCmdSignal("compressor and stage1", 0, addr);
            }
            switch (hpChangeOverType){
                case ENERGIZE_IN_HEATING:
                    relayStages.put("HeatingStage1",1);
                    setCmdSignal("heatpump and changeover and stage1",1.0,addr);
                    break;
                default:
                    setCmdSignal("heatpump and changeover and stage1",0,addr);
                    break;
            }
            if(curTemp <= (heatingDesiredTemp - occupied.getHeatingDeadBand())){
                //Turn on Stage 2
                if(isCompressorStage2Enabled) {
                    relayStages.put("HeatingStage2", 1);
                    setCmdSignal("compressor and stage2", 1.0, addr);
                }else {
                    if(getCmdSignal("compressor and stage2", addr) > 0)setCmdSignal("compressor and stage2",0,addr);
                }
            }else if(curTemp >= heatingDesiredTemp){
                setCmdSignal("compressor and stage2",0,addr);
            }else{
                if(getCmdSignal("compressor and stage2",addr) > 0){
                    relayStages.put("HeatingStage2",1);
                }
            }

            if(isFanStage1Enabled){
                switch (fanSpeed){
                    case AUTO:
                        if(relayStages.containsKey("HeatingStage1")) {
                            relayStages.put("FanStage1",1);
                            setCmdSignal("fan and stage1", 1.0, addr);
                        }else
                            setCmdSignal("fan and stage1",0,addr);
                        break;
                    case FAN_LOW:
                        if(relayStages.containsKey("HeatingStage1") || relayStages.containsKey("HeatingStage2") || occupied.isOccupied()) {
                            relayStages.put("FanStage1", 1);
                            setCmdSignal("fan and stage1", 1.0, addr);
                        }else
                            setCmdSignal("fan and stage1",0,addr);
                        break;
                    case FAN_HIGH:
                        if(occupied.isOccupied() || relayStages.containsKey("HeatingStage1")) {
                            relayStages.put("FanStage1", 1);
                            setCmdSignal("fan and stage1", 1.0, addr);
                        }else
                            setCmdSignal("fan and stage1",0,addr);
                        break;
                }
            }
            if(isFanRelay5Enabled){
                switch (fanRelayType){
                    case FAN_STAGE2:
                        switch (fanSpeed){
                            case AUTO:
                                if(relayStages.containsKey("HeatingStage2")) {
                                    relayStages.put("FanStage2", 1);
                                    setCmdSignal("fan and stage2", 1.0, addr);
                                }else
                                    setCmdSignal("fan and stage2",0,addr);
                                break;
                            case FAN_LOW:
                                if(relayStages.containsKey("HeatingStage2") && occupied.isOccupied()) {
                                    relayStages.put("FanStage2", 1);
                                    setCmdSignal("fan and stage2", 1.0, addr);
                                }else
                                    setCmdSignal("fan and stage2",0,addr);
                                break;
                            case FAN_HIGH:
                                if(occupied.isOccupied() || relayStages.containsKey("HeatingStage2")) {
                                    relayStages.put("FanStage2", 1);
                                    setCmdSignal("fan and stage2", 1.0, addr);
                                }else
                                    setCmdSignal("fan and stage2",0,addr);
                                break;
                        }
                        break;
                }
            }
        }else{
            if(curTemp >= heatingDesiredTemp + hysteresis){
                //Turn off stage1

                if(isAuxHeatingEnabled ){
                    setCmdSignal("aux and heating ", 0, addr);

                }
                setCmdSignal("compressor and stage1",0,addr);
                setCmdSignal("compressor and stage2",0,addr);
                switch (hpChangeOverType){
                    case ENERGIZE_IN_HEATING:
                        if(getCmdSignal("heatpump and changeover and stage1",addr) > 0)
                            setCmdSignal("heatpump and changeover and stage1",0,addr);
                        break;
                }
                if(occupied.isOccupied()){
                    switch (fanSpeed){
                        case AUTO:
                            setCmdSignal("fan and stage1",0,addr);
                            setCmdSignal("fan and stage2",0,addr);
                            break;
                        case FAN_LOW:
                            if(isFanStage1Enabled) {
                                relayStages.put("FanStage1", 1);
                                setCmdSignal("fan and stage1", 1.0, addr);
                            }
                            break;
                        case FAN_HIGH:
                            if(isFanStage1Enabled) {
                                relayStages.put("FanStage1", 1);
                                setCmdSignal("fan and stage1", 1.0, addr);
                            }
                            if(isFanRelay5Enabled) {
                                relayStages.put("FanStage2", 1);
                                setCmdSignal("fan and stage2", 1.0, addr);
                            }
                            break;
                    }

                }else {
                    setCmdSignal("fan and stage1", 0, addr);
                    setCmdSignal("fan and stage2",0,addr);
                }
            }else{

                if(isAuxHeatingEnabled && (getCmdSignal("aux and heating ",addr) > 0)){
                         relayStages.put("HeatingStage2", 1);
                         relayStages.put("HeatingStage1", 1);
                }
                if((getCmdSignal("compressor and stage1", addr) > 0) || (getCmdSignal("changeover and stage1",addr) > 0))
                    relayStages.put("HeatingStage1",1);
                if(getCmdSignal("fan and stage1", addr) > 0)relayStages.put("FanStage1",1);
            }
        }

        switch (fanRelayType){

            case HUMIDIFIER:
                if(hpuEquip.getHumidity() < humidifierTargetThreshold) {
                    relayStages.put("Humidifier", 1);
                    setCmdSignal("fan and stage2", 1.0, addr);
                }else if(getCmdSignal("fan and stage2",addr) > 0){
                    if(hpuEquip.getHumidity() > (humidifierTargetThreshold + (humidifierTargetThreshold * 0.05)))
                        setCmdSignal("fan and stage2",0, addr);
                    else
                        relayStages.put("Humidifier",1);
                }
                break;
            case DE_HUMIDIFIER:
                if(hpuEquip.getHumidity() > humidifierTargetThreshold) {
                    relayStages.put("Dehumidifier", 1);
                    setCmdSignal("fan and stage2", 1.0, addr);
                }else if(getCmdSignal("fan and stage2",addr) > 0){
                    if(hpuEquip.getHumidity() < (humidifierTargetThreshold -(humidifierTargetThreshold * 0.05)))
                        setCmdSignal("fan and stage2",0, addr);
                    else
                        relayStages.put("Dehumidifier",1);
                }
                break;
        }
        StandaloneScheduler.updateSmartStatStatus(equipId, state, relayStages);

    }
}

