package a75f.io.logic.bo.building.ss2pfcu;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.RFDEAD;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;
import static a75f.io.logic.bo.util.CCUUtils.DEFAULT_COOLING_DESIRED;
import static a75f.io.logic.bo.util.CCUUtils.DEFAULT_HEATING_DESIRED;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.ZoneTempState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.StandaloneLogicalFanSpeeds;
import a75f.io.logic.bo.building.definitions.StandaloneOperationalMode;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage;
import a75f.io.logic.jobs.StandaloneScheduler;
import a75f.io.logic.tuners.StandaloneTunerUtil;
import a75f.io.logic.tuners.TunerUtil;

public class TwoPipeFanCoilUnitProfile extends ZoneProfile {



    public static String TAG = TwoPipeFanCoilUnitProfile.class.getSimpleName().toUpperCase();

    public HashMap<Short, TwoPipeFanCoilUnitEquip> twoPfcuDeviceMap;
    double setTempCooling = 74.0;
    double setTempHeating = 70.0;
    double supplyWaterTempTh2 = 0.0;
    double heatingThreshold = 85.0;
    double coolingThreshold = 65.0;

    public TwoPipeFanCoilUnitProfile(){
        twoPfcuDeviceMap = new HashMap<>();
    }
    @Override
    public ProfileType getProfileType() {

        return ProfileType.SMARTSTAT_TWO_PIPE_FCU;
    }

    @Override
    public void updateZonePoints() {
        if (Globals.getInstance().isTestMode()){
            return;
        }
        if (mInterface != null) {
            mInterface.refreshView();
        }

        for (short node : twoPfcuDeviceMap.keySet()) {
            if (twoPfcuDeviceMap.get(node) == null) {
                addLogicalMap(node);
                CcuLog.d(TAG, " Logical Map added for smartstat " + node);
                continue;
            }
            CcuLog.i(TAG, "SmartStat 2PFCU profile");
            TwoPipeFanCoilUnitEquip twoPfcuDevice = twoPfcuDeviceMap.get(node);
            if (twoPfcuDevice.profileType != ProfileType.SMARTSTAT_TWO_PIPE_FCU)
                continue;
            double roomTemp = twoPfcuDevice.getCurrentTemp();
            Equip twoPfcuEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
            if (isRFDead()) {
                handleRFDead(node, twoPfcuDevice, twoPfcuEquip);
                return;
            } else if (isZoneDead()) {
                resetRelays(twoPfcuEquip.getId(), node,ZoneTempState.TEMP_DEAD);
                twoPfcuDevice.setStatus(TEMPDEAD.ordinal());
                String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \"" + node + "\"");
                if (!curStatus.equals("Zone Temp Dead")) {
                    CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"", "Zone Temp Dead");
                }
                CCUHsApi.getInstance().writeHisValByQuery("point and not ota and status and his and group == \"" + node + "\"", (double) TEMPDEAD.ordinal());
                continue;

            }
            //fcuWaterSamplingEveryHour(node);
            checkWaterSampling(node);
            double setTempCooling ;
            double setTempHeating ;
            CCUHsApi hayStack = CCUHsApi.getInstance();
            if (hayStack.isScheduleSlotExitsForRoom(twoPfcuEquip.getId())) {
                Double unoccupiedSetBack = hayStack.getUnoccupiedSetback(twoPfcuEquip.getId());
                CcuLog.d(TAG, "Schedule slot Not  exists for room:  SmartStat two pipe : " + twoPfcuEquip.getId() + "node address : " + node);
                setTempCooling = DEFAULT_COOLING_DESIRED + unoccupiedSetBack;
                setTempHeating = DEFAULT_HEATING_DESIRED - unoccupiedSetBack;
                twoPfcuDevice.setDesiredTempCooling(setTempCooling);
                twoPfcuDevice.setDesiredTempHeating(setTempHeating);
                twoPfcuDevice.setDesiredTemp((setTempHeating + setTempCooling) / 2);
            } else {
                setTempCooling = twoPfcuDevice.getDesiredTempCooling();
                setTempHeating = twoPfcuDevice.getDesiredTempHeating();
            }

            double averageDesiredTemp = (setTempCooling + setTempHeating) / 2.0;
            if (averageDesiredTemp != twoPfcuDevice.getDesiredTemp()) {
                twoPfcuDevice.setDesiredTemp(averageDesiredTemp);
            }


            double ssOperatingMode = getOperationalModes("temp and conditioning", twoPfcuEquip.getId());
            double ssFanOpMode = getOperationalModes("fan and operation", twoPfcuEquip.getId());
            updateThresholdsForTwoPipe(twoPfcuEquip.getId());
            StandaloneOperationalMode opMode = StandaloneOperationalMode.values()[(int) ssOperatingMode];
            StandaloneLogicalFanSpeeds fanSpeed = StandaloneLogicalFanSpeeds.values()[(int) ssFanOpMode];
            setTempCooling = twoPfcuDevice.getDesiredTempCooling();
            setTempHeating = twoPfcuDevice.getDesiredTempHeating();
            supplyWaterTempTh2 = twoPfcuDevice.getSupplyWaterTemp();
            String zoneId = HSUtil.getZoneIdFromEquipId(twoPfcuEquip.getId());
            Occupied occuStatus = ScheduleManager.getInstance().getOccupiedModeCache(zoneId);
            FanModeCacheStorage fanCacheStorage = FanModeCacheStorage.Companion.getSmartStatFanModeCache();
            int fanModeSaved = fanCacheStorage.getFanModeFromCache(twoPfcuEquip.getId());
            CcuLog.d(TAG, " smartstat 2pfcu, updates 111=" + heatingThreshold+","+coolingThreshold + "," + setTempHeating + "," + setTempCooling + "," + roomTemp+","+supplyWaterTempTh2);

            boolean occupied = (occuStatus != null && occuStatus.isOccupied());
            //For dual temp but for single mode we use tuners

            if (!occupied && (fanSpeed != StandaloneLogicalFanSpeeds.OFF)) {
                if ((fanSpeed != StandaloneLogicalFanSpeeds.AUTO) && (fanSpeed != StandaloneLogicalFanSpeeds.FAN_LOW_ALL_TIMES) && (fanSpeed != StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES) && (fanSpeed != StandaloneLogicalFanSpeeds.FAN_HIGH2_ALL_TIMES)) {
                    StandaloneScheduler.updateOperationalPoints(twoPfcuEquip.getId(), "fan and operation and mode", StandaloneLogicalFanSpeeds.AUTO.ordinal());
                    fanSpeed = StandaloneLogicalFanSpeeds.AUTO;
                }
            }

            if(occupied && (fanSpeed == StandaloneLogicalFanSpeeds.AUTO) && (fanModeSaved != 0)){
                //KUMAR need to reset back for FAN_LOW_OCCIPIED or FAN_MEDIUM_OCCUPIED or FAN_HIGH_OCCUPIED_PERIOD means next day schedule periods
                if(fanSpeed == StandaloneLogicalFanSpeeds.AUTO) {
                    StandaloneScheduler.updateOperationalPoints(twoPfcuEquip.getId(), "fan and operation and mode", fanModeSaved);
                    fanSpeed = StandaloneLogicalFanSpeeds.values()[ fanModeSaved];
                }
            }
            if((roomTemp > 0) && (fanSpeed != StandaloneLogicalFanSpeeds.OFF)){
                switch (opMode){
                    case AUTO:
                        if(supplyWaterTempTh2 > coolingThreshold){
                            twoPipeFCUHeatOnlyMode(twoPfcuEquip,node,roomTemp,occuStatus,fanSpeed);
                        } else {
                            twoPipeFCUCoolOnlyMode(twoPfcuEquip,node,roomTemp,occuStatus,fanSpeed, opMode);
                        }
                        break;
                    case COOL_ONLY:
                        if((supplyWaterTempTh2 < coolingThreshold) && (roomTemp > 0)){
                            twoPipeFCUCoolOnlyMode(twoPfcuEquip,node,roomTemp,occuStatus,fanSpeed, opMode);
                        }else {
                            fanOperationalModes(twoPfcuEquip.getId(),fanSpeed,node,occupied,opMode,roomTemp);
                        }
                        break;
                    case HEAT_ONLY:
                        if((supplyWaterTempTh2 > coolingThreshold) && (roomTemp > 0)){
                            twoPipeFCUHeatOnlyMode(twoPfcuEquip,node,roomTemp,occuStatus,fanSpeed);
                        }else {
                            fanOperationalModes(twoPfcuEquip.getId(),fanSpeed,node,occupied,opMode, roomTemp);
                        }
                        break;
                    case OFF:
                        fanOperationalModes(twoPfcuEquip.getId(),fanSpeed,node,occupied, opMode, roomTemp);
                        break;
                }

            }else{
                resetRelays(twoPfcuEquip.getId(),node,ZoneTempState.FAN_OP_MODE_OFF);

            }
            dumpOutput(node,fanSpeed,opMode);
        }

    }

    private void handleRFDead(short node, TwoPipeFanCoilUnitEquip twoPfcuDevice,Equip equip) {
        twoPfcuDevice.setStatus(RFDEAD.ordinal());
        StandaloneScheduler.updateSmartStatStatus(equip.getId(), RFDEAD, new HashMap<>(), ZoneTempState.RF_DEAD);

        String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \"" + node + "\"");
        if (!curStatus.equals(RFDead)) {
            CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"", RFDead);
        }
        CCUHsApi.getInstance().writeHisValByQuery("point and not ota and status and his and group == \"" + node + "\"", (double) RFDEAD.ordinal());
    }

    @Override
    public ZoneState getState() {
        return state;
    }

    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address) {

        return twoPfcuDeviceMap.get(address) != null ? twoPfcuDeviceMap.get(address).getProfileConfiguration() : null;
    }
    /**
     * Only creates a run time instance of logical map for initialize.
     *
     * @param addr address
     */
    public void addLogicalMap(short addr) {
        TwoPipeFanCoilUnitEquip deviceMap = new TwoPipeFanCoilUnitEquip(getProfileType(), addr);
        twoPfcuDeviceMap.put(addr, deviceMap);
    }

    /**
     * Only creates a run time instance of logical map for initialize.
     *
     * @param addr address
     */
    public void addLogicalMap(short addr, String roomRef) {
        TwoPipeFanCoilUnitEquip deviceMap = new TwoPipeFanCoilUnitEquip(getProfileType(), addr);
        twoPfcuDeviceMap.put(addr, deviceMap);
    }


    /**
     * When the profile is created first time , either via UI or from existing tagsMap
     * this method has to be called on the profile instance.
     *
     * @param addr address
     * @param config configuration
     * @param floorRef floor reference
     * @param zoneRef zone reference
     */
    public void addLogicalMapAndPoints(short addr, TwoPipeFanCoilUnitConfiguration config, String floorRef, String zoneRef) {
        TwoPipeFanCoilUnitEquip deviceMap = new TwoPipeFanCoilUnitEquip(getProfileType(), addr);
        deviceMap.createHaystackPoints(config, floorRef, zoneRef);

        twoPfcuDeviceMap.put(addr, deviceMap);
    }

    public void updateLogicalMapAndPoints(short addr, TwoPipeFanCoilUnitConfiguration config) {
        TwoPipeFanCoilUnitEquip deviceMap = twoPfcuDeviceMap.get(addr);
        deviceMap.updateHaystackPoints(config);
    }

    public Set<Short> getNodeAddresses() {
        return twoPfcuDeviceMap.keySet();
    }

    @Override
    public Equip getEquip() {
        for (short nodeAddress : twoPfcuDeviceMap.keySet()) {
            HashMap equip = CCUHsApi.getInstance().read("equip and group == \"" + nodeAddress + "\"");
            return new Equip.Builder().setHashMap(equip).build();
        }
        return null;
    }

    @JsonIgnore
    @Override
    public double getCurrentTemp() {
        for (short nodeAddress : twoPfcuDeviceMap.keySet()) {
            return twoPfcuDeviceMap.get(nodeAddress).getCurrentTemp();
        }
        return 0;
    }
    @JsonIgnore
    public double getDisplayCurrentTemp()
    {
        return getAverageZoneTemp();
    }


    @JsonIgnore
    @Override
    public double getAverageZoneTemp() {
        double tempTotal = 0;
        int nodeCount = 0;
        for (short nodeAddress : twoPfcuDeviceMap.keySet()) {
            if (twoPfcuDeviceMap.get(nodeAddress) == null) {
                continue;
            }
            if (twoPfcuDeviceMap.get(nodeAddress).getCurrentTemp() > 0) {
                tempTotal += twoPfcuDeviceMap.get(nodeAddress).getCurrentTemp();
                nodeCount++;
            }
        }
        return nodeCount == 0 ? 0 : tempTotal / nodeCount;
    }


    public double getConfigEnabled(String config, short node) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and " + config + " and group == \"" + node + "\"");

    }

    public double getCmdSignal(String cmd, short node) {
        return CCUHsApi.getInstance().readHisValByQuery("point and standalone and pipe2 and fcu and cmd and his and " + cmd + " and group == \"" + node + "\"");
    }

    public void setCmdSignal(String cmd, double val, short node) {
        CcuLog.d(TAG, cmd + " : "+val);
        CCUHsApi.getInstance().writeHisValByQuery("point and standalone and pipe2 and fcu and cmd and his and " + cmd + " and group == \"" + node + "\"", val);
    }

    public double getOperationalModes(String cmd, String equipRef) {
        return CCUHsApi.getInstance().readHisValByQuery("point and standalone and mode and his and " + cmd + " and equipRef == \"" + equipRef + "\"");
    }
    private void fanOperationalModes(String equipId, StandaloneLogicalFanSpeeds fanSpeed, short addr, boolean occupied, StandaloneOperationalMode opMode, double roomTemp){

        double hysteresis = StandaloneTunerUtil.getStandaloneStage1Hysteresis(equipId);
        HashMap<String,Integer> relayStates = new HashMap<>();
        boolean isFanMediumEnabled = getConfigEnabled("relay1", addr) > 0;
        boolean isFanHighEnabled = getConfigEnabled("relay2", addr) > 0;
        boolean isFanLowEnabled = getConfigEnabled("relay3", addr) > 0; //relay3 for fan low
        boolean isAuxHeatingEnabled = getConfigEnabled("relay4", addr) > 0;//Aux Heating
        boolean isWaterValveEnabled = getConfigEnabled("relay6", addr) > 0;
        switch (fanSpeed) {
            case AUTO:
                resetRelays(equipId,addr,ZoneTempState.NONE);
                break;
            case FAN_HIGH_CURRENT_OCCUPIED://Firmware mapping is medium
            case FAN_HIGH_OCCUPIED:
            case FAN_HIGH_ALL_TIMES:

                if (isFanLowEnabled) {
                    if (occupied) {
                        setCmdSignal("fan and low", 0, addr);
                    }
                }
                if (isFanMediumEnabled) {
                    if(occupied || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES)) {
                        if (getCmdSignal("fan and medium", addr) == 0)
                            setCmdSignal("fan and medium", 1.0, addr);
                        relayStates.put("FanStage2", 1);
                    }
                }
                if (isFanHighEnabled) {
                    if (getCmdSignal("fan and high", addr) > 0)
                        setCmdSignal("fan and high", 0, addr);
                }
                break;
            case FAN_HIGH2_CURRENT_OCCUPIED://firmware mapping is high
            case FAN_HIGH2_OCCUPIED:
            case FAN_HIGH2_ALL_TIMES:

                if (isFanLowEnabled) {
                    setCmdSignal("fan and low", 0, addr);
                }
                if (isFanMediumEnabled) {
                    if (getCmdSignal("fan and medium", addr) > 0)
                        setCmdSignal("fan and medium", 0, addr);
                }
                if (isFanHighEnabled) {
                    if(occupied || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH2_ALL_TIMES)) {
                        if (getCmdSignal("fan and high", addr) == 0)
                            setCmdSignal("fan and high", 1.0, addr);
                        relayStates.put("FanStage3", 1);
                    }
                }
                break;
            case FAN_LOW_CURRENT_OCCUPIED:
            case FAN_LOW_OCCUPIED:
            case FAN_LOW_ALL_TIMES:
                if (isFanLowEnabled) {
                    if(occupied || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_LOW_ALL_TIMES)) {
                        setCmdSignal("fan and low", 1.0, addr);
                        relayStates.put("FanStage1", 1);
                    }
                }
                if (isFanMediumEnabled) {
                    if (getCmdSignal("fan and medium", addr) > 0)
                        setCmdSignal("fan and medium", 0, addr);
                }
                if (isFanHighEnabled) {
                    if (getCmdSignal("fan and high", addr) > 0)
                        setCmdSignal("fan and high", 0, addr);
                }
                break;
        }

        if(isWaterValveEnabled){
            double waterVal = getCmdSignal("pipe2 and fcu and water and valve", addr);
            switch (opMode) {
                case AUTO:
                    if(occupied || (roomTemp > setTempCooling) || (roomTemp > 0 && roomTemp < setTempHeating)) {
                        // run periodic water valve check 2 mins on and 5 mins OFF
                        fcuPeriodicWaterValveCheck(addr, false);
                    }else if((twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() != 0) && (waterVal > 0)&& (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000) > 3) ) {
                        CcuLog.d("FCU","fcuPeriodicWaterCheck AUTO fanmode="+addr+","+twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer()+","+((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000)+","+((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime())/60000));
                        //setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                        turnOffWaterValve(addr);
                        twoPfcuDeviceMap.get(addr).setWaterValvePeriodicTimer(0);
                    }else if((twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime() != 0) && (waterVal > 0) && (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime())/60000) < 50)){
                        //setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                        turnOffWaterValve(addr);
                    }
                    break;
                case COOL_ONLY:
                    if (supplyWaterTempTh2 > heatingThreshold) {
                        if (waterVal > 0) {
                            turnOffWaterValve(addr);
                        }
                            //setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                    } else if(supplyWaterTempTh2 > coolingThreshold){
                        if( occupied || (!occupied && ((roomTemp > setTempCooling) || ( (roomTemp > 0) && (roomTemp < setTempHeating)))) ){
                            // run periodic water valve check 2 mins on and 5 mins OFF
                            fcuPeriodicWaterValveCheck(addr, false);
                        }else if((twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() != 0) && (waterVal > 0)&& (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000) > 3) ) {
                            //setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                            turnOffWaterValve(addr);
                            twoPfcuDeviceMap.get(addr).setWaterValvePeriodicTimer(0);
                        }else if((twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime() != 0) && (waterVal > 0) && (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime())/60000) < 50)){
                            //setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                            turnOffWaterValve(addr);
                        }
                    }
                    break;
                case HEAT_ONLY:
                    if (supplyWaterTempTh2 < coolingThreshold) {
                        if (waterVal > 0) {
                            turnOffWaterValve(addr);
                        }
                            //setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                    } else if(supplyWaterTempTh2 < heatingThreshold){
                        if( occupied || (!occupied && ((roomTemp > setTempCooling) || ( (roomTemp > 0) && (roomTemp < setTempHeating)))) ){
                            // run periodic water valve check 2 mins on and 5 mins OFF
                            fcuPeriodicWaterValveCheck(addr, false);
                        }else if((twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() != 0) && (waterVal > 0)&& (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000) > 3) ) {
                            //setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                            turnOffWaterValve(addr);
                            twoPfcuDeviceMap.get(addr).setWaterValvePeriodicTimer(0);
                        }else if((twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime() != 0) && (waterVal > 0) && (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime())/60000) < 50)){
                            turnOffWaterValve(addr);
                        }
                    }
                    break;
                case OFF:
                    if (waterVal > 0)
                        turnOffWaterValve(addr);
                    break;
            }
        }
        if(isAuxHeatingEnabled){
            if (opMode.equals(StandaloneOperationalMode.COOL_ONLY) || opMode.equals(StandaloneOperationalMode.OFF)) {
                if (getCmdSignal("aux and heating", addr) > 0)
                    setCmdSignal("aux and heating", 0, addr);
            } else {
                if(isAuxHeatingEnabled){
                    if(roomTemp <= setTempHeating){
                        relayStates.put("HeatingStage1",1);
                        setCmdSignal("aux and heating",1.0,addr);
                    }else if(roomTemp >= (setTempHeating + hysteresis)){
                        if(getCmdSignal("aux and heating",addr) > 0)
                            setCmdSignal("aux and heating",0,addr);

                    } else {
                        if(getCmdSignal("aux and heating",addr) > 0)
                            relayStates.put("HeatingStage1",1);
                    }
                }
            }
        }
        StandaloneScheduler.updateSmartStatStatus(equipId, DEADBAND,relayStates ,ZoneTempState.NONE);
        twoPfcuDeviceMap.get(addr).setStatus(DEADBAND.ordinal());
    }
    private void resetRelays(String equipId, short node, ZoneTempState temperatureState) {
        //reset all relays to 0
        setCmdSignal("fan and medium",0,node);
        setCmdSignal("fan and high",0,node);
        setCmdSignal("fan and low",0,node);
        setCmdSignal("aux and heating",0,node);
        if(temperatureState == ZoneTempState.FAN_OP_MODE_OFF)
            turnOffWaterValve(node);
        StandaloneScheduler.updateSmartStatStatus(equipId, DEADBAND, new HashMap<>() ,temperatureState);
        twoPfcuDeviceMap.get(node).setStatus(DEADBAND.ordinal());
    }

    private void twoPipeFCUCoolOnlyMode(Equip equip, short addr, double roomTemp, Occupied occuStatus, StandaloneLogicalFanSpeeds fanSpeed, StandaloneOperationalMode opMode){

        double hysteresis = StandaloneTunerUtil.getStandaloneStage1Hysteresis(equip.getId());
        boolean isFanMediumEnabled = getConfigEnabled("relay1", addr) > 0;
        boolean isFanHighEnabled = getConfigEnabled("relay2", addr) > 0;
        boolean isFanLowEnabled = getConfigEnabled("relay3", addr) > 0; //relay3 for fan low
        boolean isAuxHeatingEnabled = getConfigEnabled("relay4", addr) > 0;//Aux Heating
        boolean isWaterValve = getConfigEnabled("relay6", addr) > 0;
        double coolingDeadband = 2.0;
        double heatingDeadband = 2.0;
        boolean occupied = false;
        if(occuStatus != null){
            ArrayList<HashMap<Object , Object>> isSchedulableAvailable = CCUHsApi.getInstance().readAllNativeSchedulable();
            HashMap<Object,Object> hDBMap = CCUHsApi.getInstance().readEntity("zone and heating and deadband and roomRef == \"" + equip.getRoomRef() + "\"");
            if (!isSchedulableAvailable.isEmpty() && !hDBMap.isEmpty()) {
                heatingDeadband = CCUHsApi.getInstance().readPointPriorityValByQuery("zone and heating and deadband and roomRef == \"" + equip.getRoomRef() + "\"");
                coolingDeadband = CCUHsApi.getInstance().readPointPriorityValByQuery("zone and cooling and deadband and roomRef == \"" + equip.getRoomRef() + "\"");
            }else{
                heatingDeadband = TunerUtil.readTunerValByQuery("heating and deadband and base", equip.getId());
                coolingDeadband = TunerUtil.readTunerValByQuery("cooling and deadband and base", equip.getId());
            }
            occupied = occuStatus.isOccupied();
        }
        HashMap<String,Integer> relayStates = new HashMap<>();

        String fanstages = "";
        switch (fanSpeed){
            case AUTO:
                if(isFanLowEnabled){
                    if(((roomTemp >= setTempCooling) && (roomTemp < (setTempCooling+coolingDeadband)))
                            || ((roomTemp <= setTempHeating) && (roomTemp > (setTempHeating - heatingDeadband)))
                            || (!isFanMediumEnabled && !isFanHighEnabled && (roomTemp >= setTempCooling || roomTemp <= setTempHeating))){
                        if(getCmdSignal("fan and medium", addr) == 0) {
                            setCmdSignal("fan and low", 1.0, addr);
                            setCmdSignal("fan and medium", 0, addr);
                            setCmdSignal("fan and high", 0, addr);
                            fanstages = "FanStage1";
                        }else if((getCmdSignal("fan and low",addr) == 0) && (roomTemp == setTempCooling)) {
                            setCmdSignal("fan and low", 1.0, addr);
                            fanstages = "FanStage1";
                        }
                    }else if( (roomTemp >= (setTempHeating+hysteresis)) && (roomTemp <= setTempCooling - hysteresis)){
                        setCmdSignal("fan and low",0,addr);
                    }else {
                        if(getCmdSignal("fan and low",addr) > 0)
                            fanstages = "FanStage1";
                    }
                }
                if(isFanMediumEnabled){
                    if((roomTemp >= (setTempCooling + coolingDeadband) ) || (roomTemp <= (setTempHeating - heatingDeadband))){
                        if(getCmdSignal("fan and high",addr) == 0) {
                            setCmdSignal("fan and medium", 1.0, addr);
                            setCmdSignal("fan and low", 0, addr);
                            setCmdSignal("fan and high", 0, addr);
                            fanstages = "FanStage2";
                        }
                    }else if((roomTemp >= setTempHeating) && (roomTemp <= setTempCooling)){
                        setCmdSignal("fan and medium",0,addr);
                    }else {
                        if(getCmdSignal("fan and medium",addr) > 0)
                            fanstages = "FanStage2";
                    }
                }
                if(isFanHighEnabled){
                    if((roomTemp >= (setTempCooling + (coolingDeadband * 2.0))) || (roomTemp <= (setTempHeating - (heatingDeadband * 2.0)))){
                        setCmdSignal("fan and high", 1.0 , addr);
                        setCmdSignal("fan and medium",0,addr);
                        setCmdSignal("fan and low",0,addr);
                        fanstages = "FanStage3";
                    }else if((roomTemp >= (setTempHeating - heatingDeadband)) && (roomTemp <= (setTempCooling + coolingDeadband))){
                        setCmdSignal("fan and high",0,addr);
                    }else {
                        if(getCmdSignal("fan and high",addr) > 0)
                            fanstages = "FanStage3";
                    }
                }
                if(!fanstages.isEmpty())relayStates.put(fanstages,1);
                break;
            case FAN_HIGH_CURRENT_OCCUPIED://Firmware mapping is medium for pfcu but for CPU and HPU it is high
            case FAN_HIGH_OCCUPIED:
            case FAN_HIGH_ALL_TIMES:

                if (isFanLowEnabled) {
                    setCmdSignal("fan and low", 0, addr);

                }
                if(isFanMediumEnabled){
                    if(occupied || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES)) {
                        if (getCmdSignal("fan and medium", addr) == 0)
                            setCmdSignal("fan and medium", 1.0, addr);
                        relayStates.put("FanStage2", 1);
                    }
                }
                if(isFanHighEnabled){
                    if(getCmdSignal("fan and high",addr) > 0)
                        setCmdSignal("fan and high",0,addr);
                }
                break;
            case FAN_HIGH2_CURRENT_OCCUPIED://firmware mapping is high, only for pfcu
            case FAN_HIGH2_OCCUPIED:
            case FAN_HIGH2_ALL_TIMES:

                if (isFanLowEnabled) {
                    setCmdSignal("fan and low",0,addr);
                }
                if(isFanMediumEnabled){
                    if(getCmdSignal("fan and medium",addr) > 0)
                        setCmdSignal("fan and medium",0,addr);
                }
                if(isFanHighEnabled){

                    if(occupied || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH2_ALL_TIMES)) {
                        if (getCmdSignal("fan and high", addr) == 0)
                            setCmdSignal("fan and high", 1.0, addr);
                        relayStates.put("FanStage3", 1);
                    }
                }
                break;
            case FAN_LOW_OCCUPIED:
            case FAN_LOW_CURRENT_OCCUPIED:
            case FAN_LOW_ALL_TIMES:
                if (isFanLowEnabled) {
                    if(occupied || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_LOW_ALL_TIMES)) {
                        setCmdSignal("fan and low", 1.0, addr);
                        relayStates.put("FanStage1", 1);
                    }
                }
                if(isFanMediumEnabled){
                    if(getCmdSignal("fan and medium",addr) > 0)
                        setCmdSignal("fan and medium",0,addr);
                }
                if(isFanHighEnabled){
                    if(getCmdSignal("fan and high",addr) > 0)
                        setCmdSignal("fan and high",0,addr);
                }
                break;
        }
        if(isWaterValve){
            if(roomTemp >= setTempCooling){
                if(!isSupplyWithInThreshold()){
                    relayStates.put("CoolingStage1",1);
                    twoPfcuDeviceMap.get(addr).setWaterValveLastOnTime(System.currentTimeMillis());
                    //setCmdSignal("pipe2 and fcu and water and valve",1.0,addr);
                    turnOnWaterValve(addr);
                }
            }else if(roomTemp <= (setTempCooling - hysteresis) /*&& (twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() == 0)*/){
                if(getCmdSignal("pipe2 and fcu and water and valve",addr) > 0)
                    //setCmdSignal("pipe2 and fcu and water and valve",0,addr);
                    turnOffWaterValve(addr);
            }else {
                if(getCmdSignal("pipe2 and fcu and water and valve",addr) > 0)
                    relayStates.put("CoolingStage1",1);
            }
        }
        if(isAuxHeatingEnabled){
            if (opMode.equals(StandaloneOperationalMode.COOL_ONLY) || opMode.equals(StandaloneOperationalMode.OFF)) {
                if (getCmdSignal("aux and heating", addr) > 0)
                    setCmdSignal("aux and heating", 0, addr);
            } else {
                if(isAuxHeatingEnabled){
                    if(roomTemp <= setTempHeating){
                        relayStates.put("HeatingStage1",1);
                        setCmdSignal("aux and heating",1.0,addr);
                    }else if(roomTemp >= (setTempHeating + hysteresis)){
                        if(getCmdSignal("aux and heating",addr) > 0)
                            setCmdSignal("aux and heating",0,addr);

                    } else {
                        if(getCmdSignal("aux and heating",addr) > 0)
                            relayStates.put("HeatingStage1",1);
                    }
                }
            }
        }
        CcuLog.d(TAG, Arrays.toString(twoPfcuDeviceMap.keySet().toArray())+"SmartStat - 111 2PFCcool only :"+fanSpeed.name()+","+Arrays.toString(relayStates.entrySet().toArray())+","+heatingDeadband+","+roomTemp+","
                +setTempCooling+" "+isFanLowEnabled+" "+isFanMediumEnabled+" "+isFanHighEnabled+" "+isAuxHeatingEnabled+" "+isWaterValve);

        //ZoneState curstate = relayStates.size() > 0 ?  (relayStates.containsKey("CoolingStage1") ? COOLING : DEADBAND ) : DEADBAND;
        StandaloneScheduler.updateSmartStatStatus(equip.getId(), COOLING,relayStates,ZoneTempState.NONE);
        //if(twoPfcuDeviceMap.get(addr).getStatus() != curstate.ordinal())
            twoPfcuDeviceMap.get(addr).setStatus(COOLING.ordinal());
    }

    private void twoPipeFCUHeatOnlyMode(Equip equip, short addr, double roomTemp, Occupied occuStatus, StandaloneLogicalFanSpeeds fanSpeed){
        double hysteresis = StandaloneTunerUtil.getStandaloneStage1Hysteresis(equip.getId());
        boolean isFanMediumEnabled = getConfigEnabled("relay1", addr) > 0;
        boolean isFanHighEnabled = getConfigEnabled("relay2", addr) > 0;
        boolean isFanLowEnabled = getConfigEnabled("relay3", addr) > 0; //relay3 for fan low
        boolean isAuxHeatingEnabled = getConfigEnabled("relay4", addr) > 0;//Aux Heating
        boolean isWaterValve = getConfigEnabled("relay6", addr) > 0;
        double heatingDeadband = 2.0;
        boolean occupied = false;
        if(occuStatus != null){
            heatingDeadband = occuStatus.getHeatingDeadBand();
            occupied = occuStatus.isOccupied();
        }
        HashMap<String,Integer> relayStates = new HashMap<>();

        String fanstages = "";

        switch (fanSpeed){
            case AUTO:
                // Turn on only one relay at any given point of time say if 70 is dt, then 70-68- fan low on, 66-68 - fan medium on, 64-66 - fan high on,
                if(isFanLowEnabled){
                    if(((roomTemp <= setTempHeating) &&(roomTemp > (setTempHeating - heatingDeadband)))
                        || (!isFanMediumEnabled && !isFanHighEnabled && roomTemp <= setTempHeating)){
                        if(getCmdSignal("fan and medium",addr) == 0) {
                            if(getCmdSignal("fan and low",addr) == 0)
                                setCmdSignal("fan and low", 1.0, addr);
                            if(getCmdSignal("fan and medium",addr) > 0)
                                setCmdSignal("fan and medium", 0, addr);
                            if(getCmdSignal("fan and high",addr) > 0)
                                setCmdSignal("fan and high", 0, addr);
                            fanstages = "FanStage1";
                        }else if((getCmdSignal("fan and low",addr) == 0) && (roomTemp == setTempHeating)) {
                            setCmdSignal("fan and low", 1.0, addr);
                            fanstages = "FanStage1";
                        }
                    }else if(roomTemp >= setTempHeating + hysteresis){
                        if(getCmdSignal("fan and low",addr) > 0)
                            setCmdSignal("fan and low",0,addr);
                    }else {
                        if(getCmdSignal("fan and low",addr) > 0)
                            fanstages = "FanStage1";
                    }
                }
                if(isFanMediumEnabled){
                    if((roomTemp <= (setTempHeating - heatingDeadband))){
                        if(getCmdSignal("fan and high",addr) == 0) {
                            if (getCmdSignal("fan and low", addr) > 0)
                                setCmdSignal("fan and low", 0, addr);
                            if (getCmdSignal("fan and high", addr) > 0)
                                setCmdSignal("fan and high", 0, addr);
                            if (getCmdSignal("fan and medium", addr) == 0)
                                setCmdSignal("fan and medium", 1.0, addr);
                            fanstages = "FanStage2";
                        }
                    }else if(roomTemp >= setTempHeating){
                        if(getCmdSignal("fan and medium",addr) > 0)
                            setCmdSignal("fan and medium",0,addr);
                    }else {
                        if(getCmdSignal("fan and medium",addr) > 0)
                            fanstages = "FanStage2";
                    }
                }
                if(isFanHighEnabled){
                    if(roomTemp <= (setTempHeating - (heatingDeadband * 2.0))){
                        if(getCmdSignal("fan and high",addr) == 0)
                            setCmdSignal("fan and high", 1.0 , addr);
                        if(getCmdSignal("fan and low",addr) > 0)
                            setCmdSignal("fan and low",0,addr);
                        if(getCmdSignal("fan and medium",addr) > 0)
                            setCmdSignal("fan and medium",0,addr);
                        fanstages = "FanStage3";
                        relayStates.put("FanStage3",1);
                    }else if(roomTemp >= (setTempHeating - heatingDeadband)){
                        if(getCmdSignal("fan and high",addr) > 0)
                            setCmdSignal("fan and high",0,addr);
                    }else {
                        if(getCmdSignal("fan and high",addr) > 0)
                            fanstages = "FanStage3";
                    }
                }
                if(!fanstages.isEmpty())relayStates.put(fanstages,1);
                break;
            case FAN_HIGH_CURRENT_OCCUPIED://Firmware mapping is medium for pfcu but for CPU and HPU it is high
            case FAN_HIGH_OCCUPIED:
            case FAN_HIGH_ALL_TIMES:

                if (isFanLowEnabled) {
                    if(occupied) {
                        if(getCmdSignal("fan and low",addr) > 0)
                            setCmdSignal("fan and low", 0, addr);
                    }
                }
                if(isFanMediumEnabled){
                    if(occupied || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES)) {
                        if (getCmdSignal("fan and medium", addr) == 0)
                            setCmdSignal("fan and medium", 1.0, addr);
                        relayStates.put("FanStage2", 1);
                    }
                }
                if(isFanHighEnabled){
                    if(getCmdSignal("fan and high",addr) > 0)
                        setCmdSignal("fan and high",0,addr);
                }
                break;
            case FAN_HIGH2_CURRENT_OCCUPIED://firmware mapping is high, only for pfcu
            case FAN_HIGH2_OCCUPIED:
            case FAN_HIGH2_ALL_TIMES:

                if (isFanLowEnabled) {
                    if(getCmdSignal("fan and low",addr) > 0)
                        setCmdSignal("fan and low",0,addr);
                }
                if(isFanMediumEnabled){
                    if(getCmdSignal("fan and medium",addr) > 0)
                        setCmdSignal("fan and medium",0,addr);
                }
                if(isFanHighEnabled){
                    if(occupied || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH2_ALL_TIMES)) {
                        if (getCmdSignal("fan and high", addr) == 0)
                            setCmdSignal("fan and high", 1.0, addr);
                        relayStates.put("FanStage3", 1);
                    }
                }
                break;
            case FAN_LOW_CURRENT_OCCUPIED:
            case FAN_LOW_OCCUPIED:
            case FAN_LOW_ALL_TIMES:
                if (isFanLowEnabled) {
                    if(occupied || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_LOW_ALL_TIMES)) {
                        setCmdSignal("fan and low", 1.0, addr);
                        relayStates.put("FanStage1", 1);
                    }
                }
                if(isFanMediumEnabled){
                    if(getCmdSignal("fan and medium",addr) > 0)
                        setCmdSignal("fan and medium",0,addr);
                }
                if(isFanHighEnabled){
                    if(getCmdSignal("fan and high",addr) > 0)
                        setCmdSignal("fan and high",0,addr);
                }
                break;
        }
        if(isWaterValve){
            if(isSupplyWithInThreshold() && twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() == 0) {
                //setCmdSignal("pipe2 and fcu and water and valve", 0.0, addr);
                turnOffWaterValve(addr);
            }
            if(roomTemp <= setTempHeating){
                if(!isSupplyWithInThreshold()) {
                    twoPfcuDeviceMap.get(addr).setWaterValveLastOnTime(System.currentTimeMillis());
                    relayStates.put("HeatingStage1", 1);
                   // setCmdSignal("pipe2 and fcu and water and valve", 1.0, addr);
                    turnOnWaterValve(addr);
                }
            }else if(roomTemp >= (setTempHeating + hysteresis) /*&& (twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() == 0)*/){ //Todo check for periodic timer == 0
                if(getCmdSignal("pipe2 and fcu and water and valve",addr) > 0)
                    turnOffWaterValve(addr);
                    //setCmdSignal("pipe2 and fcu and water and valve",0,addr);
            }else {
                if(getCmdSignal("pipe2 and fcu and water and valve",addr) > 0)
                    relayStates.put("HeatingStage1",1);
            }
        }
        if(isAuxHeatingEnabled){
            if(roomTemp <= setTempHeating){
                relayStates.put("HeatingStage1",1);
                setCmdSignal("aux and heating",1.0,addr);
            }else if(roomTemp >= (setTempHeating + hysteresis)){
                if(getCmdSignal("aux and heating",addr) > 0)
                    setCmdSignal("aux and heating",0,addr);

            } else {
                if(getCmdSignal("aux and heating",addr) > 0)
                    relayStates.put("HeatingStage1",1);
            }
        }

        CcuLog.d(TAG, Arrays.toString(twoPfcuDeviceMap.keySet().toArray())+"SmartStat - 111 2PFHeat only :"+fanSpeed.name()+","+Arrays.toString(relayStates.entrySet().toArray())+","+heatingDeadband+","+roomTemp+","
                +setTempCooling+" "+isFanLowEnabled+" "+isFanMediumEnabled+" "+isFanHighEnabled+" "+isAuxHeatingEnabled+" "+isWaterValve);

        //ZoneState curstate = relayStates.size() > 0 ?  (relayStates.containsKey("CoolingStage1") ? COOLING : DEADBAND ) : DEADBAND;
        StandaloneScheduler.updateSmartStatStatus(equip.getId(), HEATING,relayStates ,ZoneTempState.NONE);
       //if(twoPipeFanCoilUnitEquip.getStatus() != curstate.ordinal())
            twoPfcuDeviceMap.get(addr).setStatus(HEATING.ordinal());
    }
    private void updateThresholdsForTwoPipe(String equipId){
        try {

            heatingThreshold = CCUHsApi.getInstance().readHisValByQuery("point and tuner and heating and threshold and pipe2 and fcu and equipRef == \"" + equipId + "\"");
            coolingThreshold = CCUHsApi.getInstance().readHisValByQuery("point and tuner and cooling and threshold and pipe2 and fcu and equipRef == \"" + equipId + "\"");
        }catch (Exception e){
            heatingThreshold = 85;
            coolingThreshold = 65;
        }
    }

    private boolean fcuPeriodicWaterValveCheck(short addr, boolean isHourlyCheck){
        CcuLog.d(TAG,"fcuPeriodicWaterCheck="+addr+","+isHourlyCheck+","+twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer()+","+((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000)+","+((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime())/60000));
        if((twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() == 0) ||(((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000) < 2)){ //ON for two mins
            if((getCmdSignal("pipe2 and fcu and water and valve",addr) == 0) || ( twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() == 0)) {

                if (isHourlyCheck) {
                    turnOnWaterValve(addr);
                    //setCmdSignal("pipe2 and fcu and water and valve", 1.0, addr);
                    twoPfcuDeviceMap.get(addr).setWaterValvePeriodicTimer(System.currentTimeMillis());
                    CcuLog.i("SSPIPE2", "fcuPeriodicWaterValveCheck: turning on water valve");
                } else {
                    if(!isSupplyWithInThreshold()) {
                        turnOnWaterValve(addr);
                        //setCmdSignal("pipe2 and fcu and water and valve", 1.0, addr);
                        twoPfcuDeviceMap.get(addr).setWaterValvePeriodicTimer(System.currentTimeMillis());
                    }
                }
                return true;
            }
        }else if(!isHourlyCheck && (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000) < 7)){//OFF for 5 mins
            //if(getCmdSignal("pfcu2 and water and valve",addr) > 0) {
                //setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                turnOffWaterValve(addr);
                return true;
            //}
        }else
            twoPfcuDeviceMap.get(addr).setWaterValvePeriodicTimer(0);
        return false;

    }

    private void checkWaterSampling (short address) {
        boolean isWaterValveEnabled = getConfigEnabled("relay6", address) > 0;
        CcuLog.d(TAG, "isWaterValveEnabled is enabled "+isWaterValveEnabled);

        int waitingDuration = 58;

        if (isSupplyWithInThreshold()){
            waitingDuration = 5;
        }

        if (isWaterValveEnabled) {
            double currentValveStatus = getCmdSignal("pipe2 and fcu and water and valve", address);
            long runningValveTime = Objects.requireNonNull(twoPfcuDeviceMap.get(address)).getWaterValvePeriodicTimer();
            long lastWaterValveOnTime = Objects.requireNonNull(twoPfcuDeviceMap.get(address)).getWaterValveLastOnTime();
            CcuLog.d(TAG, "runningValveTime : "+runningValveTime );

            if (lastWaterValveOnTime != 0) {
                if (runningValveTime == 0) {
                    long waitingTimeMin = milliToMin (System.currentTimeMillis() - lastWaterValveOnTime);
                    CcuLog.d(TAG, "Waiting to turn on : "+waitingTimeMin);
                    if (waitingTimeMin > waitingDuration) {
                        if (currentValveStatus != 1.0) {
                            Objects.requireNonNull(twoPfcuDeviceMap.get(address)).setWaterValvePeriodicTimer(System.currentTimeMillis());
                            turnOnWaterValve(address);
                        }
                    }
                } else {
                    long samplingSinceFrom = milliToMin (System.currentTimeMillis() - lastWaterValveOnTime);
                    CcuLog.d(TAG, "checkWaterSampling is running from min "+samplingSinceFrom);
                    if (samplingSinceFrom >= 2) {
                        Objects.requireNonNull(twoPfcuDeviceMap.get(address)).setWaterValvePeriodicTimer(0);
                        Objects.requireNonNull(twoPfcuDeviceMap.get(address)).setWaterValveLastOnTime(System.currentTimeMillis());
                        turnOffWaterValve( address);
                        CcuLog.i(TAG, "Resetting WATER_VALVE to OFF");
                    }
                }
            } else {
                Objects.requireNonNull(twoPfcuDeviceMap.get(address)).setWaterValveLastOnTime(System.currentTimeMillis());
            }
        }
    }

    private long milliToMin (long milliseconds)  {
        return (milliseconds / (1000 * 60) % 60);
    }

    private void turnOnWaterValve (short address) {
        if (getCmdSignal("pipe2 and fcu and water and valve",  address) == 0.0) {
            setCmdSignal("pipe2 and fcu and water and valve", 1.0, address);
            Objects.requireNonNull(twoPfcuDeviceMap.get(address)).setWaterValveLastOnTime(System.currentTimeMillis());
        }

    }

    private void turnOffWaterValve (short address) {
       if (Objects.requireNonNull(twoPfcuDeviceMap.get(address)).getWaterValvePeriodicTimer() == 0) {
           if (getCmdSignal("pipe2 and fcu and water and valve",  address) == 1.0) {
               setCmdSignal("pipe2 and fcu and water and valve", 0.0, address);
           }
       }
    }



    @Override
    public void reset(){
        for (short node : twoPfcuDeviceMap.keySet())
        {
            twoPfcuDeviceMap.get(node).setCurrentTemp(0);
        }
    }

    private boolean isSupplyWithInThreshold(){
        return ( supplyWaterTempTh2 >= coolingThreshold && supplyWaterTempTh2 <= heatingThreshold );
    }

    void dumpOutput(short node, StandaloneLogicalFanSpeeds fanSpeed, StandaloneOperationalMode opMode){
        CcuLog.d(TAG,
                " \nSWT :  "+supplyWaterTempTh2+"\nCT : "+getCurrentTemp() +"\ndesired : "+setTempHeating +" "+setTempCooling+
                        " \nFan mode : "+fanSpeed+" Conditioning Mode : "+opMode+
                        " \nWater valve     "+getCmdSignal("pipe2 and fcu and water and valve",node) +
                        " \nAux Heat        "+getCmdSignal("aux and heating",node)+
                        " \nLOW             "+getCmdSignal("fan and low",node)+
                        " \nMEDIUM          "+getCmdSignal("fan and medium",node)+
                        " \nHIGH            "+getCmdSignal("fan and high",node));
    }
}
