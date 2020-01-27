package a75f.io.logic.bo.building.sshpu;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.ZoneTempState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.SmartStatFanRelayType;
import a75f.io.logic.bo.building.definitions.SmartStatHeatPumpChangeOverType;
import a75f.io.logic.bo.building.definitions.StandaloneLogicalFanSpeeds;
import a75f.io.logic.bo.building.definitions.StandaloneOperationalMode;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.jobs.StandaloneScheduler;
import a75f.io.logic.tuners.StandaloneTunerUtil;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

public class HeatPumpUnitProfile extends ZoneProfile {


    public static String TAG = HeatPumpUnitProfile.class.getSimpleName().toUpperCase();

    public HashMap<Short, HeatPumpUnitEquip> hpuDeviceMap;
    double setTempCooling = 74.0;
    double setTempHeating = 70.0;
    boolean occupied;

    public HeatPumpUnitProfile() {

        hpuDeviceMap = new HashMap<>();
    }

    @Override
    public ProfileType getProfileType() {
        return ProfileType.SMARTSTAT_HEAT_PUMP_UNIT;
    }
    @Override
    public boolean isZoneDead() {

        double buildingLimitMax =  TunerUtil.readBuildingTunerValByQuery("building and limit and max");
        double buildingLimitMin =  TunerUtil.readBuildingTunerValByQuery("building and limit and min");

        double tempDeadLeeway = TunerUtil.readBuildingTunerValByQuery("temp and dead and leeway");

        for (short node : hpuDeviceMap.keySet())
        {
            double curTemp = hpuDeviceMap.get(node).getCurrentTemp();
            if (curTemp > (buildingLimitMax + tempDeadLeeway)
                    || curTemp < (buildingLimitMin - tempDeadLeeway))
            {
                return true;
            }
        }
        return false;
    }
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
            HeatPumpUnitEquip hpuDevice = hpuDeviceMap.get(node);
            if (hpuDevice.profileType != ProfileType.SMARTSTAT_HEAT_PUMP_UNIT)
                continue;
            double roomTemp = hpuDevice.getCurrentTemp();
            double curHumidity = hpuDevice.getHumidity();
            Equip hpuEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
            

            if(isZoneDead()){
                resetRelays(hpuEquip.getId(),node,curHumidity, ZoneTempState.TEMP_DEAD);
                hpuDevice.setStatus(DEADBAND.ordinal());
                String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \"" + node + "\"");
                if (!curStatus.equals("Zone Temp Dead")) {
                    CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"", "Zone Temp Dead");
                }
                CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + node + "\"", (double) TEMPDEAD.ordinal());
                continue;

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

            occupied = (occuStatus == null ? false : occuStatus.isOccupied());
            //For dual temp but for single mode we use tuners

            double ssOperatingMode = getOperationalModes("temp and conditioning", hpuEquip.getId());
            double ssFanOpMode = getOperationalModes("fan and operation", hpuEquip.getId());
            StandaloneOperationalMode opMode = StandaloneOperationalMode.values()[(int) ssOperatingMode];
            StandaloneLogicalFanSpeeds fanSpeed = StandaloneLogicalFanSpeeds.values()[(int) ssFanOpMode];
            int fanModeSaved = Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).getInt(hpuEquip.getId(),0);
            if (!occupied && (fanSpeed != StandaloneLogicalFanSpeeds.OFF)) {
                if ((fanSpeed != StandaloneLogicalFanSpeeds.AUTO) && (fanSpeed != StandaloneLogicalFanSpeeds.FAN_LOW_ALL_TIMES) && (fanSpeed != StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES)) {
                    StandaloneScheduler.updateOperationalPoints(hpuEquip.getId(), "fan and operation and mode", StandaloneLogicalFanSpeeds.AUTO.ordinal());
                    fanSpeed = StandaloneLogicalFanSpeeds.AUTO;
                }
            }
            if(occupied && (fanSpeed == StandaloneLogicalFanSpeeds.AUTO) && (fanModeSaved != 0)){
                //KUMAR need to reset back for FAN_LOW_OCCIPIED or FAN_MEDIUM_OCCUPIED or FAN_HIGH_OCCUPIED_PERIOD means next day schedule periods
                if(fanSpeed == StandaloneLogicalFanSpeeds.AUTO) {
                    StandaloneScheduler.updateOperationalPoints(hpuEquip.getId(), "fan and operation and mode", fanModeSaved);
                    fanSpeed = StandaloneLogicalFanSpeeds.values()[ fanModeSaved];
                }
            }
            if(occuStatus != null){
                hpuDevice.setProfilePoint("occupancy and status", occuStatus.isOccupied() ? Occupancy.OCCUPIED.ordinal() : (occuStatus.isPreconditioning() ? Occupancy.PRECONDITIONING.ordinal() : (occuStatus.isForcedOccupied() ? Occupancy.FORCEDOCCUPIED.ordinal() : 0)));
            }else {
                hpuDevice.setProfilePoint("occupancy and status", occupied ? 1 : 0);
            }
            Log.d(TAG, " smartstat hpu, updates =" + node+","+roomTemp+","+occupied+","+","+state);

            if((fanSpeed != StandaloneLogicalFanSpeeds.OFF) && (roomTemp > 0)){
                switch (opMode){
                    case AUTO:
                        if(roomTemp < averageDesiredTemp){
                            //state = HEATING;
                            hpuHeatOnlyMode(hpuDevice,hpuEquip.getId(),roomTemp,occuStatus,node,fanSpeed);
                        }else {
                            //state = COOLING;
                            hpuCoolOnlyMode(hpuDevice, hpuEquip.getId(), roomTemp,occuStatus,node,fanSpeed);
                        }
                            break;
                    case COOL_ONLY:
                        if(roomTemp > averageDesiredTemp){
                           // state = COOLING;
                            hpuCoolOnlyMode(hpuDevice,hpuEquip.getId(),roomTemp,occuStatus,node,fanSpeed);
                        }else {
                            //state = DEADBAND;
                            if(fanSpeed == StandaloneLogicalFanSpeeds.AUTO)
                                resetRelays(hpuEquip.getId(),node,curHumidity,ZoneTempState.NONE);
                            else {
                                handleFanStages(hpuEquip.getId(), node, fanSpeed, curHumidity);
                            }
                            if(hpuDevice.getStatus() != DEADBAND.ordinal())
                                hpuDevice.setStatus(DEADBAND.ordinal());
                        }
                        break;
                    case HEAT_ONLY:
                        if(roomTemp < averageDesiredTemp){
                            //state = HEATING;
                            hpuHeatOnlyMode(hpuDevice,hpuEquip.getId(),roomTemp,occuStatus,node,fanSpeed);
                        }else {
                            //state = DEADBAND;
                            if(fanSpeed == StandaloneLogicalFanSpeeds.AUTO)
                                resetRelays(hpuEquip.getId(),node,curHumidity,ZoneTempState.NONE);
                            else {
                                handleFanStages(hpuEquip.getId(), node, fanSpeed, curHumidity);
                            }
                            if(hpuDevice.getStatus() != DEADBAND.ordinal())
                                hpuDevice.setStatus(DEADBAND.ordinal());
                        }
                        break;
                    case OFF: {
                        handleFanStages(hpuEquip.getId(),node,fanSpeed,curHumidity);
                        if(hpuDevice.getStatus() != DEADBAND.ordinal())
                            hpuDevice.setStatus(DEADBAND.ordinal());
                    }
                        break;
                }
            }else {
                //No condition happens
                //state = DEADBAND;
                resetRelays(hpuEquip.getId(),node,curHumidity,ZoneTempState.FAN_OP_MODE_OFF);
            }


            //hpuDevice.setProfilePoint("temp and operating and mode",state.ordinal());
            /*if(hpuDevice.getStatus() != state.ordinal())
                hpuDevice.setStatus(state.ordinal());*/
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
        return CCUHsApi.getInstance().readHisValByQuery("point and standalone and mode and his and " + cmd + " and equipRef == \"" + equipRef + "\"");
    }

    private void handleFanStages(String equipId, short node, StandaloneLogicalFanSpeeds fanSpeed, double curHumidity){
        int fanStage2Type = (int) getConfigType("relay5", node);
        setCmdSignal("compressor and stage1", 0, node);
        setCmdSignal("compressor and stage2", 0, node);
        HashMap<String, Integer> relayStages = new HashMap<String, Integer>();
        switch (fanSpeed) {
            case AUTO:
                resetRelays(equipId, node,curHumidity,ZoneTempState.NONE);
                break;
            case FAN_LOW_CURRENT_OCCUPIED:
            case FAN_LOW_OCCUPIED:
                if (occupied) {
                    setCmdSignal("fan and stage1", 1.0, node);
                    relayStages.put("FanStage1", 1);
                }else
                    setCmdSignal("fan and stage1", 0, node);

                if (fanStage2Type == SmartStatFanRelayType.FAN_STAGE2.ordinal()) {
                    setCmdSignal("fan and stage2", 0, node);
                }
                break;
            case FAN_LOW_ALL_TIMES:
                setCmdSignal("fan and stage1", 1.0, node);
                relayStages.put("FanStage1", 1);
                if (fanStage2Type == SmartStatFanRelayType.FAN_STAGE2.ordinal()) {
                    setCmdSignal("fan and stage2", 0, node);
                }
            case FAN_HIGH_CURRENT_OCCUPIED:
            case FAN_HIGH_OCCUPIED:
                if (occupied && (fanStage2Type == SmartStatFanRelayType.FAN_STAGE2.ordinal())) {
                    setCmdSignal("fan and stage1", 1.0, node);
                    setCmdSignal("fan and stage2", 1.0, node);
                    relayStages.put("FanStage1", 1);
                    relayStages.put("FanStage2", 1);
                }else {
                    setCmdSignal("fan and stage1", 0, node);
                    if (!occupied && (fanStage2Type == SmartStatFanRelayType.FAN_STAGE2.ordinal())) setCmdSignal("fan and stage2", 0, node);
                }
                break;
            case FAN_HIGH_ALL_TIMES:
                if (fanStage2Type == SmartStatFanRelayType.FAN_STAGE2.ordinal()) {
                    setCmdSignal("fan and stage1", 1.0, node);
                    setCmdSignal("fan and stage2", 1.0, node);
                    relayStages.put("FanStage1", 1);
                    relayStages.put("FanStage2", 1);
                }
                break;
        }

        ZoneTempState temperatureState = ZoneTempState.NONE;
        if(buildingLimitMinBreached() ||  buildingLimitMaxBreached() )
            temperatureState = ZoneTempState.EMERGENCY;
        updateHumidityStatus(fanStage2Type,node,equipId,curHumidity,relayStages);
        StandaloneScheduler.updateSmartStatStatus(equipId, DEADBAND, relayStages, temperatureState);
    }
    private void resetRelays(String equipId, short node, double humidity, ZoneTempState temperatureState) {


        int fanStage2Type = (int) getConfigType("relay5", node);
        int changeoverType = (int)getConfigType("relay6",node);
        SmartStatHeatPumpChangeOverType hpChangeOverType = SmartStatHeatPumpChangeOverType.values()[changeoverType];
        try {
            //if (getCmdSignal("compressor and stage1", node) > 0)
                setCmdSignal("compressor and stage1", 0, node);
            //if (getCmdSignal("compressor and stage2", node) > 0)
                setCmdSignal("compressor and stage2", 0, node);
            //if (getCmdSignal("aux and heating ", node) > 0)
                setCmdSignal("aux and heating ", 0, node);
            if(hpChangeOverType == SmartStatHeatPumpChangeOverType.ENERGIZE_IN_COOLING)
                setCmdSignal("changeover and cooling and stage1", 0, node);
            else 
                setCmdSignal("changeover and heating and stage1", 0, node);
            //if (getCmdSignal("fan and stage1", node) > 0)
                setCmdSignal("fan and stage1", 0, node);
            if((fanStage2Type == SmartStatFanRelayType.FAN_STAGE2.ordinal()) || (temperatureState == ZoneTempState.FAN_OP_MODE_OFF)) {
                //if (getCmdSignal("fan and stage2", node) > 0)
                    setCmdSignal("fan and stage2", 0, node);
            }
        }catch (Exception e){

            if(temperatureState == ZoneTempState.TEMP_DEAD){
                setCmdSignal("compressor and stage1", 0, node);
                setCmdSignal("compressor and stage2", 0, node);
                setCmdSignal("aux and heating ", 0, node);
                setCmdSignal("changeover and cooling and stage1", 0, node);
                setCmdSignal("changeover and heating and stage1", 0, node);
            }
        }
         HashMap<String,Integer> relayStages = new HashMap<String, Integer>();
         if(temperatureState != ZoneTempState.FAN_OP_MODE_OFF)
             updateHumidityStatus(fanStage2Type,node,equipId,humidity,relayStages);
        StandaloneScheduler.updateSmartStatStatus(equipId, DEADBAND,relayStages ,temperatureState);
    }

    private void hpuCoolOnlyMode(HeatPumpUnitEquip hpuEquip, String equipId, double curTemp,  Occupied occuStatus,Short addr, StandaloneLogicalFanSpeeds fanSpeed){


        double hysteresis = StandaloneTunerUtil.getStandaloneStage1Hysteresis(equipId);
        boolean isCompressorStage1Enabled = getConfigEnabled("relay1",addr) > 0 ? true : false;
        boolean isCompressorStage2Enabled = getConfigEnabled("relay2",addr) > 0 ? true : false;
        boolean isFanStage1Enabled = getConfigEnabled("relay3", addr) > 0 ? true : false; //relay3 for fan low
        boolean isFanRelay5Enabled = getConfigEnabled("relay5", addr) > 0 ? true : false; //relay5 for fan high
        boolean isAuxHeatingEnabled = getConfigEnabled("relay4",addr)> 0 ? true : false;//Aux
        double curHumidity = hpuEquip.getHumidity();
        double humidifierTargetThreshold = 25.0;
        int fanStage2Type = (int)getConfigType("relay5",addr);
        double coolingDeadband = 2.0;
        boolean occupied = false;
        if(occuStatus != null){
            coolingDeadband = occuStatus.getCoolingDeadBand();
            occupied = occuStatus.isOccupied();
        }
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
        Log.d(TAG,"hpuCoolOnlyMode ="+occupied+","+addr+","+isAuxHeatingEnabled+","+isCompressorStage1Enabled+","+isCompressorStage2Enabled+","+isFanStage1Enabled);
        if(isAuxHeatingEnabled && (getCmdSignal("aux and heating ",addr) > 0))setCmdSignal("aux and heating ",0,addr);
        if(curTemp >= setTempCooling){
            //Turn on relay1
            if(isCompressorStage1Enabled){
                relayStages.put("CoolingStage1",1);
                setCmdSignal("compressor and stage1", 1.0, addr);
            }else{
                setCmdSignal("compressor and stage1", 0, addr);
            }
            if(curTemp >= (setTempCooling + coolingDeadband)){
                //Turn on Stage 2
                if(isCompressorStage2Enabled) {
                    relayStages.put("CoolingStage2", 1);
                    setCmdSignal("compressor and stage2", 1.0, addr);
                }else {
                    if(getCmdSignal("compressor and stage2", addr) > 0)setCmdSignal("compressor and stage2",0,addr);
                }
            }else if(curTemp <= setTempCooling){
                setCmdSignal("compressor and stage2",0,addr);
            }else{
                if(getCmdSignal("compressor and stage2",addr) > 0){
                    relayStages.put("CoolingStage2",1);
                }
            }

            switch (hpChangeOverType){
                case ENERGIZE_IN_COOLING:
                    relayStages.put("CoolingStage1",1);
                    setCmdSignal("changeover and cooling and stage1",1.0,addr);
                    break;
                case ENERGIZE_IN_HEATING:
                    setCmdSignal("changeover and heating and stage1",0,addr);
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
                    case FAN_LOW_CURRENT_OCCUPIED:
                    case FAN_LOW_OCCUPIED:
                        if(relayStages.containsKey("CoolingStage1") || relayStages.containsKey("CoolingStage2") || occupied) {
                            relayStages.put("FanStage1",1);
                            setCmdSignal("fan and stage1", 1.0, addr);
                        }else
                            setCmdSignal("fan and stage1",0,addr);
                        break;
                    case FAN_LOW_ALL_TIMES:
                    case FAN_HIGH_ALL_TIMES:
                        relayStages.put("FanStage1",1);
                        setCmdSignal("fan and stage1", 1.0, addr);
                        break;
                    case FAN_HIGH_CURRENT_OCCUPIED:
                    case FAN_HIGH_OCCUPIED:
                        if(occupied || relayStages.containsKey("CoolingStage1")) {
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
                            case FAN_LOW_CURRENT_OCCUPIED:
                            case FAN_LOW_OCCUPIED:
                            case FAN_LOW_ALL_TIMES:
                                /*if(relayStages.containsKey("CoolingStage2") && occupied) {
                                    relayStages.put("FanStage2",1);
                                    setCmdSignal("fan and stage2", 1.0, addr);
                                }else*/
                                    setCmdSignal("fan and stage2",0,addr);
                                break;
                            case FAN_HIGH_CURRENT_OCCUPIED:
                            case FAN_HIGH_OCCUPIED:
                                if(occupied || relayStages.containsKey("CoolingStage2")) {
                                    relayStages.put("FanStage2",1);
                                    setCmdSignal("fan and stage2", 1.0, addr);
                                }else
                                    setCmdSignal("fan and stage2",0,addr);
                                break;
                            case FAN_HIGH_ALL_TIMES:
                                relayStages.put("FanStage2",1);
                                setCmdSignal("fan and stage2", 1.0, addr);
                                break;
                        }
                        break;
                }
            }
        }else{
            if(curTemp <= (setTempCooling - hysteresis)){
                //Turn off stage1
                setCmdSignal("compressor and stage1",0,addr);
                setCmdSignal("compressor and stage2",0,addr);
                switch (hpChangeOverType){
                    case ENERGIZE_IN_HEATING:
                        if(getCmdSignal("changeover and heating and stage1",addr) > 0)
                            setCmdSignal("changeover and heating and stage1",0,addr);
                        break;
                    case ENERGIZE_IN_COOLING:
                        setCmdSignal("changeover and cooling and stage1",0,addr);
                        break;
                }
                if(occupied){
                    switch (fanSpeed){
                        case AUTO:
                            setCmdSignal("fan and stage1",0,addr);
                            setCmdSignal("fan and stage2",0,addr);
                            break;
                        case FAN_LOW_ALL_TIMES:
                        case FAN_LOW_CURRENT_OCCUPIED:
                        case FAN_LOW_OCCUPIED:
                            relayStages.put("FanStage1",1);
                            setCmdSignal("fan and stage1",1.0,addr);

                            if(isFanRelay5Enabled && (fanRelayType == SmartStatFanRelayType.FAN_STAGE2)) {
                                setCmdSignal("fan and stage2", 0, addr);
                            }
                            break;
                        case FAN_HIGH_ALL_TIMES:
                        case FAN_HIGH_CURRENT_OCCUPIED:
                        case FAN_HIGH_OCCUPIED:

                            if(isFanStage1Enabled) {
                                relayStages.put("FanStage1", 1);
                                setCmdSignal("fan and stage1", 1.0, addr);
                            }
                            if(isFanRelay5Enabled && (fanRelayType == SmartStatFanRelayType.FAN_STAGE2)) {
                                relayStages.put("FanStage2", 1);
                                setCmdSignal("fan and stage2", 1.0, addr);
                            }
                            break;
                    }

                }else {
                    switch (fanSpeed){
                        case FAN_LOW_ALL_TIMES:
                            if(isFanStage1Enabled) {
                                relayStages.put("FanStage1", 1);
                                setCmdSignal("fan and stage1", 1.0, addr);
                            }
                            if(isFanRelay5Enabled && (fanRelayType == SmartStatFanRelayType.FAN_STAGE2)) {
                                setCmdSignal("fan and stage2", 0, addr);
                            }
                            break;
                        case FAN_HIGH_ALL_TIMES:
                            if(isFanStage1Enabled) {
                                relayStages.put("FanStage1", 1);
                                setCmdSignal("fan and stage1", 1.0, addr);
                            }
                            if(isFanRelay5Enabled && (fanRelayType == SmartStatFanRelayType.FAN_STAGE2)) {
                                relayStages.put("FanStage2", 1);
                                setCmdSignal("fan and stage2", 1.0, addr);
                            }
                            break;
                        default:
                            setCmdSignal("fan and stage1", 0, addr);
                            setCmdSignal("fan and stage2",0,addr);
                            break;
                    }
                }
            }else{
                if((getCmdSignal("compressor and stage1", addr) > 0) || ((hpChangeOverType == SmartStatHeatPumpChangeOverType.ENERGIZE_IN_COOLING) && (getCmdSignal("changeover and cooling and stage1",addr) > 0)))
                    relayStages.put("CoolingStage1",1);
                else{
                    setCmdSignal("compressor and stage1",0,addr);
                    setCmdSignal("compressor and stage2",0,addr);
                }
                if(getCmdSignal("fan and stage1", addr) > 0)relayStages.put("FanStage1",1);
            }
        }

        switch (fanRelayType){

            case HUMIDIFIER:
                if(curHumidity > 0 && occupied) {
                    if (curHumidity < humidifierTargetThreshold) {
                        relayStages.put("Humidifier", 1);
                        setCmdSignal("humidifier", 1.0, addr);
                    } else if (getCmdSignal("humidifier", addr) > 0) {
                        if (hpuEquip.getHumidity() > (humidifierTargetThreshold + 5.0))
                            setCmdSignal("humidifier", 0, addr);
                        else
                            relayStages.put("Humdifier", 1);
                    }
                }else
                    setCmdSignal("humidifier", 0, addr);
                break;
            case DE_HUMIDIFIER:
                if(curHumidity > 0 && occupied) {
                    if (curHumidity > humidifierTargetThreshold) {
                        setCmdSignal("dehumidifier", 1.0, addr);
                        relayStages.put("Dehumidifier", 1);
                    } else if (getCmdSignal("dehumidifier", addr) > 0) {
                        if (curHumidity < (humidifierTargetThreshold - 5.0))
                            setCmdSignal("dehumidifier", 0, addr);
                        else
                            relayStages.put("Dehumidifier", 1);
                    }
                }else
                    setCmdSignal("dehumidifier", 0, addr);
                break;
        }

        ZoneTempState temperatureState = ZoneTempState.NONE;
        if(buildingLimitMinBreached() ||  buildingLimitMaxBreached() )
            temperatureState = ZoneTempState.EMERGENCY;
        ZoneState curstate = relayStages.size() > 0 ?  (relayStages.containsKey("CoolingStage1") ? COOLING : DEADBAND ) : DEADBAND;
        StandaloneScheduler.updateSmartStatStatus(equipId, curstate, relayStages,temperatureState);
        if(hpuEquip.getStatus() != curstate.ordinal())
            hpuEquip.setStatus(curstate.ordinal());
    }
    private void hpuHeatOnlyMode(HeatPumpUnitEquip hpuEquip,String equipId, double curTemp, Occupied occuStatus,Short addr, StandaloneLogicalFanSpeeds fanSpeed){
        double hysteresis = StandaloneTunerUtil.getStandaloneStage1Hysteresis(equipId);
        boolean isCompressorStage1Enabled = getConfigEnabled("relay1",addr) > 0 ? true : false;
        boolean isCompressorStage2Enabled = getConfigEnabled("relay2",addr) > 0 ? true : false;
        boolean isFanStage1Enabled = getConfigEnabled("relay3", addr) > 0 ? true : false; //relay3 for fan low
        boolean isFanRelay5Enabled = getConfigEnabled("relay5", addr) > 0 ? true : false; //relay5 for fan high
        boolean isAuxHeatingEnabled = getConfigEnabled("relay4",addr)> 0 ? true : false;//Aux Heating
        int fanStage2Type = (int)getConfigType("relay5",addr);
        double curHumidity = hpuEquip.getHumidity();
        SmartStatFanRelayType fanRelayType = SmartStatFanRelayType.values()[fanStage2Type];
        int heatPumpChangeoverType = (int)getConfigType("relay6",addr);
        double humidifierTargetThreshold = 25.0;//
        double heatingDeadband = 2.0;
        boolean occupied = false;
        if(occuStatus != null){
            heatingDeadband = occuStatus.getHeatingDeadBand();
            occupied = occuStatus.isOccupied();
        }
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
        if(curTemp <= setTempHeating){
            if(isAuxHeatingEnabled){
                if(isCompressorStage1Enabled && isCompressorStage2Enabled && (curTemp <= (setTempHeating - (2 * heatingDeadband))) ) { //tunrs on 61 and below
                    relayStages.put("HeatingStage2", 1);
                    setCmdSignal("aux and heating ",1.0,addr);
                }else if(!isCompressorStage2Enabled && (curTemp <= setTempHeating - heatingDeadband)){
                    relayStages.put("HeatingStage2", 1);
                    setCmdSignal("aux and heating ",1.0,addr);
                    setCmdSignal("compressor and stage2",0,addr);
                } else if(!isCompressorStage1Enabled && (curTemp <= setTempHeating)) {
                    relayStages.put("HeatingStage1", 1);
                    setCmdSignal("aux and heating ",1.0,addr);
                    setCmdSignal("compressor and stage1",0,addr);
                } else if(getCmdSignal("aux and heating ",addr)> 0){
                    if( isCompressorStage2Enabled && isCompressorStage2Enabled && (curTemp >= (setTempHeating - heatingDeadband)))
                        setCmdSignal("aux and heating ",0,addr);
                    else if(isCompressorStage1Enabled && !isCompressorStage2Enabled && (curTemp >= setTempHeating))
                        setCmdSignal("aux and heating ",0,addr);
                    else {
                        if(isCompressorStage2Enabled && isCompressorStage1Enabled)relayStages.put("HeatingStage2",1);
                        else if(!isCompressorStage2Enabled && isCompressorStage1Enabled)relayStages.put("HeatingStage2",1);
                        else if(!isCompressorStage1Enabled)relayStages.put("HeatingStage1",1);
                        else relayStages.put("HeatingStage1",1);
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
                    setCmdSignal("heatpump and changeover and heating and stage1",1.0,addr);
                    break;
                case ENERGIZE_IN_COOLING:
                    setCmdSignal("heatpump and changeover and cooling and stage1",0,addr);
                    break;
            }
            if(curTemp <= (setTempHeating - heatingDeadband)){
                //Turn on Stage 2
                if(isCompressorStage2Enabled) {
                    relayStages.put("HeatingStage2", 1);
                    setCmdSignal("compressor and stage2", 1.0, addr);
                }else {
                    if(getCmdSignal("compressor and stage2", addr) > 0)setCmdSignal("compressor and stage2",0,addr);
                }
            }else if(curTemp >= setTempHeating){
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
                    case FAN_LOW_CURRENT_OCCUPIED:
                    case FAN_LOW_OCCUPIED:
                        if(relayStages.containsKey("HeatingStage1") || relayStages.containsKey("HeatingStage2") || occupied) {
                            relayStages.put("FanStage1", 1);
                            setCmdSignal("fan and stage1", 1.0, addr);
                        }else
                            setCmdSignal("fan and stage1",0,addr);
                        break;
                    case FAN_HIGH_CURRENT_OCCUPIED:
                    case FAN_HIGH_OCCUPIED:
                        if(occupied || relayStages.containsKey("HeatingStage1")) {
                            relayStages.put("FanStage1", 1);
                            setCmdSignal("fan and stage1", 1.0, addr);
                        }else
                            setCmdSignal("fan and stage1",0,addr);
                        break;
                    case FAN_HIGH_ALL_TIMES:
                    case FAN_LOW_ALL_TIMES:
                        relayStages.put("FanStage1", 1);
                        setCmdSignal("fan and stage1", 1.0, addr);
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
                            case FAN_LOW_CURRENT_OCCUPIED:
                            case FAN_LOW_OCCUPIED:
                                /*if(relayStages.containsKey("HeatingStage2") && occupied) {
                                    relayStages.put("FanStage2", 1);
                                    setCmdSignal("fan and stage2", 1.0, addr);
                                }else*/
                                    setCmdSignal("fan and stage2",0,addr);
                                break;
                            case FAN_HIGH_CURRENT_OCCUPIED:
                            case FAN_HIGH_OCCUPIED:
                                if(occupied || relayStages.containsKey("HeatingStage2")) {
                                    relayStages.put("FanStage2", 1);
                                    setCmdSignal("fan and stage2", 1.0, addr);
                                }else
                                    setCmdSignal("fan and stage2",0,addr);
                                break;
                            case FAN_HIGH_ALL_TIMES:
                                relayStages.put("FanStage2", 1);
                                setCmdSignal("fan and stage2", 1.0, addr);
                                break;
                        }
                        break;
                }
            }
        }else{
            if(curTemp >= (setTempHeating + hysteresis)){
                //Turn off stage1

                if(isAuxHeatingEnabled ){
                    setCmdSignal("aux and heating ", 0, addr);

                }
                setCmdSignal("compressor and stage1",0,addr);
                setCmdSignal("compressor and stage2",0,addr);
                switch (hpChangeOverType){
                    case ENERGIZE_IN_COOLING:
                        if(getCmdSignal("heatpump and changeover and cooling and stage1",addr) > 0)
                            setCmdSignal("heatpump and changeover and cooling and stage1",0,addr);
                        break;
                    case ENERGIZE_IN_HEATING:
                        setCmdSignal("heatpump and changeover and heating and stage1",0,addr);
                        break;
                }
                if(occupied){
                    switch (fanSpeed){
                        case AUTO:
                            setCmdSignal("fan and stage1",0,addr);
                            setCmdSignal("fan and stage2",0,addr);
                            break;
                        case FAN_LOW_ALL_TIMES:
                        case FAN_LOW_CURRENT_OCCUPIED:
                        case FAN_LOW_OCCUPIED:
                            if(isFanStage1Enabled) {
                                relayStages.put("FanStage1", 1);
                                setCmdSignal("fan and stage1", 1.0, addr);
                            }
                            if(isFanRelay5Enabled && (fanRelayType == SmartStatFanRelayType.FAN_STAGE2)) {
                                setCmdSignal("fan and stage2", 0, addr);
                            }
                            break;
                        case FAN_HIGH_ALL_TIMES:
                        case FAN_HIGH_CURRENT_OCCUPIED:
                        case FAN_HIGH_OCCUPIED:
                            if(isFanStage1Enabled) {
                                relayStages.put("FanStage1", 1);
                                setCmdSignal("fan and stage1", 1.0, addr);
                            }
                            if(isFanRelay5Enabled && (fanRelayType == SmartStatFanRelayType.FAN_STAGE2)) {
                                relayStages.put("FanStage2", 1);
                                setCmdSignal("fan and stage2", 1.0, addr);
                            }
                            break;
                    }

                }else {
                    switch (fanSpeed){
                        case FAN_LOW_ALL_TIMES:
                            if(isFanStage1Enabled) {
                                relayStages.put("FanStage1", 1);
                                setCmdSignal("fan and stage1", 1.0, addr);
                            }
                            if(isFanRelay5Enabled && (fanRelayType == SmartStatFanRelayType.FAN_STAGE2)) {
                                setCmdSignal("fan and stage2", 0, addr);
                            }

                            break;
                        case FAN_HIGH_ALL_TIMES:
                            if(isFanStage1Enabled) {
                                relayStages.put("FanStage1", 1);
                                setCmdSignal("fan and stage1", 1.0, addr);
                            }
                            if(isFanRelay5Enabled && (fanRelayType == SmartStatFanRelayType.FAN_STAGE2)) {
                                relayStages.put("FanStage2", 1);
                                setCmdSignal("fan and stage2", 1.0, addr);
                            }
                            break;
                        default:
                            setCmdSignal("fan and stage1", 0, addr);
                            setCmdSignal("fan and stage2",0,addr);
                            break;
                    }
                }
            }else{

                if(isAuxHeatingEnabled && (getCmdSignal("aux and heating ",addr) > 0)){
                         relayStages.put("HeatingStage2", 1);
                         relayStages.put("HeatingStage1", 1);
                }
                if((getCmdSignal("compressor and stage1", addr) > 0) || (getCmdSignal("changeover and heating and stage1",addr) > 0))
                    relayStages.put("HeatingStage1",1);
                if(getCmdSignal("fan and stage1", addr) > 0)relayStages.put("FanStage1",1);
            }
        }

        switch (fanRelayType){

            case HUMIDIFIER:
                if(curHumidity > 0 && occupied) {
                    if (curHumidity < humidifierTargetThreshold) {
                        relayStages.put("Humidifier", 1);
                        setCmdSignal("humidifier", 1.0, addr);
                    } else if (getCmdSignal("fan and stage2", addr) > 0) {
                        if (curHumidity > (humidifierTargetThreshold + 5.0))
                            setCmdSignal("humidifier", 0, addr);
                        else
                            relayStages.put("Humidifier", 1);
                    }
                }else {
                    setCmdSignal("humidifier", 0, addr);
                }
                break;
            case DE_HUMIDIFIER:
                if((curHumidity > 0) && occupied) {
                    if (curHumidity > humidifierTargetThreshold) {
                        relayStages.put("Dehumidifier", 1);
                        setCmdSignal("dehumidifier", 1.0, addr);
                    } else if (getCmdSignal("fan and stage2", addr) > 0) {
                        if (curHumidity < (humidifierTargetThreshold - 5.0))
                            setCmdSignal("dehumidifier", 0, addr);
                        else
                            relayStages.put("Dehumidifier", 1);
                    }
                }else {
                    setCmdSignal("dehumidifier",0,addr);
                }
                break;
        }

        ZoneTempState temperatureState = ZoneTempState.NONE;
        if(buildingLimitMinBreached() ||  buildingLimitMaxBreached() )
            temperatureState = ZoneTempState.EMERGENCY;
        ZoneState curstate = relayStages.size() > 0 ?  (relayStages.containsKey("HeatingStage1") ? HEATING : DEADBAND ) : DEADBAND;
        StandaloneScheduler.updateSmartStatStatus(equipId, curstate, relayStages,temperatureState);
        //if(hpuEquip.getStatus() != curstate.ordinal())
            hpuEquip.setStatus(curstate.ordinal());

    }
    public void updateHumidityStatus(int fanStage2Type, Short addr,String equipId, double curValue, HashMap<String,Integer> relayStages){

        SmartStatFanRelayType fanRelayType = SmartStatFanRelayType.values()[fanStage2Type];
        switch (fanRelayType){

            case HUMIDIFIER:

                double targetThreshold = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and humidity and equipRef == \"" + equipId + "\"");
                if((curValue > 0) && occupied) {
                    if (curValue < targetThreshold) {
                        relayStages.put("Humidifier", 1);
                        setCmdSignal("humidifier", 1.0, addr);
                    } else if (getCmdSignal("humidifier", addr) > 0) {
                        if (curValue > (targetThreshold + 5.0))
                            setCmdSignal("humidifier", 0, addr);
                        else
                            relayStages.put("Humdifier", 1);
                    }
                }else
                    setCmdSignal("humidifier", 0, addr);
                break;
            case DE_HUMIDIFIER:
                double targetDehumidityThreshold = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and dehumidifier and equipRef == \"" + equipId + "\"");
                if((curValue > 0) && occupied) {
                    if (curValue > targetDehumidityThreshold) {
                        setCmdSignal("dehumidifier", 1.0, addr);
                        relayStages.put("Dehumidifier", 1);
                    } else if (getCmdSignal("dehumidifier", addr) > 0) {
                        if (curValue < (targetDehumidityThreshold - 5.0))
                            setCmdSignal("dehumidifier", 0, addr);
                        else
                            relayStages.put("Dehumidifier", 1);
                    }
                }else
                    setCmdSignal("dehumidifier", 0, addr);
                break;
        }
    }
    @Override
    public void reset(){
        for (short node : hpuDeviceMap.keySet())
        {
            hpuDeviceMap.get(node).setCurrentTemp(0);

        }
    }
}

