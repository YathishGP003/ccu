package a75f.io.logic.bo.building.ss2pfcu;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.ZoneTempState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.StandaloneFanSpeed;
import a75f.io.logic.bo.building.definitions.StandaloneOperationalMode;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.jobs.StandaloneScheduler;
import a75f.io.logic.tuners.StandaloneTunerUtil;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.definitions.StandaloneFanSpeed.AUTO;
import static a75f.io.logic.bo.building.definitions.StandaloneFanSpeed.OFF;

public class TwoPipeFanCoilUnitProfile extends ZoneProfile {



    public static String TAG = TwoPipeFanCoilUnitProfile.class.getSimpleName().toUpperCase();

    public HashMap<Short, TwoPipeFanCoilUnitEquip> twoPfcuDeviceMap;
    double setTempCooling = 74.0;
    double setTempHeating = 70.0;
    double supplyWaterTempTh2 = 0.0;
    double airflowTempTh1 = 0.0;
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
    public boolean isZoneDead() {

        double buildingLimitMax =  TunerUtil.readTunerValByQuery("building and limit and max", L.ccu().systemProfile.getSystemEquipRef());
        double buildingLimitMin =  TunerUtil.readTunerValByQuery("building and limit and min", L.ccu().systemProfile.getSystemEquipRef());

        double tempDeadLeeway = TunerUtil.readTunerValByQuery("temp and dead and leeway",L.ccu().systemProfile.getSystemEquipRef());

        for (short node : twoPfcuDeviceMap.keySet())
        {
            double curTemp = twoPfcuDeviceMap.get(node).getCurrentTemp();
            Log.d("SmartStat","isZoneDead="+buildingLimitMax+","+buildingLimitMin+","+tempDeadLeeway+","+curTemp);
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

        for (short node : twoPfcuDeviceMap.keySet()) {
            if (twoPfcuDeviceMap.get(node) == null) {
                addLogicalMap(node);
                Log.d(TAG, " Logical Map added for smartstat " + node);
                continue;
            }
            Log.d(TAG, "SmartStat 2PFCU profile");
            TwoPipeFanCoilUnitEquip twoPfcuDevice = twoPfcuDeviceMap.get(node);
            if (twoPfcuDevice.profileType != ProfileType.SMARTSTAT_TWO_PIPE_FCU)
                continue;
            double roomTemp = twoPfcuDevice.getCurrentTemp();
            Equip twoPfcuEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
            if (isZoneDead()) {
                resetRelays(twoPfcuEquip.getId(), node,ZoneTempState.TEMP_DEAD);
                twoPfcuDevice.setStatus(state.ordinal());
                String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \"" + node + "\"");
                if (!curStatus.equals("Zone Temp Dead")) {
                    CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"", "Zone Temp Dead");
                }
                continue;

            }
            fcuWaterSamplingEveryHour(node);
            double averageDesiredTemp = (setTempCooling + setTempHeating) / 2.0;
            if (averageDesiredTemp != twoPfcuDevice.getDesiredTemp()) {
                twoPfcuDevice.setDesiredTemp(averageDesiredTemp);
            }


            double ssOperatingMode = getOperationalModes("temp", twoPfcuEquip.getId());
            double ssFanOpMode = getOperationalModes("fan", twoPfcuEquip.getId());
            updateThresholdsForTwoPipe(twoPfcuEquip.getId());
            StandaloneOperationalMode opMode = StandaloneOperationalMode.values()[(int) ssOperatingMode];
            StandaloneFanSpeed fanSpeed = StandaloneFanSpeed.values()[(int) ssFanOpMode];
            setTempCooling = twoPfcuDevice.getDesiredTempCooling();
            setTempHeating = twoPfcuDevice.getDesiredTempHeating();
            supplyWaterTempTh2 = twoPfcuDevice.getSupplyWaterTemp();
            String zoneId = HSUtil.getZoneIdFromEquipId(twoPfcuEquip.getId());
            Occupied occuStatus = ScheduleProcessJob.getOccupiedModeCache(zoneId);
            Log.d(TAG, " smartstat 2pfcu, updates 111=" + heatingThreshold+","+coolingThreshold + "," + setTempHeating + "," + setTempCooling + "," + roomTemp+","+supplyWaterTempTh2);

            boolean occupied = (occuStatus == null ? false : occuStatus.isOccupied());
            //For dual temp but for single mode we use tuners

            if (!occupied && (fanSpeed != OFF)) {
                if (fanSpeed != StandaloneFanSpeed.AUTO) {
                    StandaloneScheduler.updateOperationalPoints(twoPfcuEquip.getId(), "fan and operation and mode", StandaloneFanSpeed.AUTO.ordinal());
                    fanSpeed = StandaloneFanSpeed.AUTO;
                }
            }
            if(fanSpeed != OFF){
                switch (opMode){
                    case AUTO:
                        if(supplyWaterTempTh2 > heatingThreshold){
                            twoPipeFCUHeatOnlyMode(twoPfcuEquip.getId(),node,roomTemp,occuStatus,fanSpeed);
                        }else if(supplyWaterTempTh2 < coolingThreshold){
                            twoPipeFCUCoolOnlyMode(twoPfcuEquip.getId(),node,roomTemp,occuStatus,fanSpeed);
                        }else {
                            fanOperationalModes(twoPfcuEquip.getId(),fanSpeed,node,occupied, opMode,roomTemp);
                        }
                        break;
                    case COOL_ONLY:
                        if((supplyWaterTempTh2 < coolingThreshold) && (roomTemp > averageDesiredTemp)){
                            twoPipeFCUCoolOnlyMode(twoPfcuEquip.getId(),node,roomTemp,occuStatus,fanSpeed);
                        }else {
                            fanOperationalModes(twoPfcuEquip.getId(),fanSpeed,node,occupied,opMode,roomTemp);
                        }
                        break;
                    case HEAT_ONLY:
                        if((supplyWaterTempTh2 > heatingThreshold) && (roomTemp < averageDesiredTemp)){
                            twoPipeFCUHeatOnlyMode(twoPfcuEquip.getId(),node,roomTemp,occuStatus,fanSpeed);
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
            if(occuStatus != null){
                twoPfcuDevice.setProfilePoint("occupancy and status", occuStatus.isOccupied() ? Occupancy.OCCUPIED.ordinal() : (occuStatus.isPreconditioning() ? Occupancy.PRECONDITIONING.ordinal() : (occuStatus.isForcedOccupied() ? Occupancy.FORCED_OCCUPIED.ordinal() : 0)));
            }else {
                twoPfcuDevice.setProfilePoint("occupancy and status", occupied ? 1 : 0);
            }
        }

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
     * @param addr
     */
    public void addLogicalMap(short addr) {
        TwoPipeFanCoilUnitEquip deviceMap = new TwoPipeFanCoilUnitEquip(getProfileType(), addr);
        twoPfcuDeviceMap.put(addr, deviceMap);
    }

    /**
     * Only creates a run time instance of logical map for initialize.
     *
     * @param addr
     */
    public void addLogicalMap(short addr, String roomRef) {
        TwoPipeFanCoilUnitEquip deviceMap = new TwoPipeFanCoilUnitEquip(getProfileType(), addr);
        twoPfcuDeviceMap.put(addr, deviceMap);
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
    public void addLogicalMapAndPoints(short addr, TwoPipeFanCoilUnitConfiguration config, String floorRef, String zoneRef) {
        TwoPipeFanCoilUnitEquip deviceMap = new TwoPipeFanCoilUnitEquip(getProfileType(), addr);
        deviceMap.createHaystackPoints(config, floorRef, zoneRef);

        twoPfcuDeviceMap.put(addr, deviceMap);
    }

    public void updateLogicalMapAndPoints(short addr, TwoPipeFanCoilUnitConfiguration config, String roomRef) {
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
            if (twoPfcuDeviceMap.get(Short.valueOf(nodeAddress)).getCurrentTemp() > 0) {
                tempTotal += twoPfcuDeviceMap.get(Short.valueOf(nodeAddress)).getCurrentTemp();
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
        return CCUHsApi.getInstance().readHisValByQuery("point and standalone and pipe2 and fcu and cmd and his and " + cmd + " and group == \"" + node + "\"");
    }

    public void setCmdSignal(String cmd, double val, short node) {
        CCUHsApi.getInstance().writeHisValByQuery("point and standalone and pipe2 and fcu and cmd and his and " + cmd + " and group == \"" + node + "\"", val);
    }

    public double getOperationalModes(String cmd, String equipRef) {
        return CCUHsApi.getInstance().readHisValByQuery("point and standalone and operation and mode and his and " + cmd + " and equipRef == \"" + equipRef + "\"");
    }
    private void fanOperationalModes(String equipId, StandaloneFanSpeed fanSpeed, short addr, boolean occupied, StandaloneOperationalMode opMode, double roomTemp){

        HashMap<String,Integer> relayStates = new HashMap<String, Integer>();
        boolean isFanMediumEnabled = getConfigEnabled("relay1",addr) > 0 ? true : false;
        boolean isFanHighEnabled = getConfigEnabled("relay2",addr) > 0 ? true : false;
        boolean isFanLowEnabled = getConfigEnabled("relay3", addr) > 0 ? true : false; //relay3 for fan low
        boolean isAuxHeatingEnabled = getConfigEnabled("relay4",addr)> 0 ? true : false;//Aux Heating
        boolean isWaterValveEnabled = getConfigEnabled("relay6",addr) > 0 ? true : false;
        switch (fanSpeed) {
            case AUTO:
                resetRelays(equipId,addr,ZoneTempState.NONE);
                break;
            case FAN_HIGH://Firmware mapping is medium

                if (isFanLowEnabled) {
                    if (occupied) {
                        setCmdSignal("fan and low", 1.0, addr);
                        relayStates.put("FanStage1", 1);
                    }
                }
                if (isFanMediumEnabled) {
                    if (getCmdSignal("fan and medium", addr) < 1.0)
                        setCmdSignal("fan and medium", 1.0, addr);
                    relayStates.put("FanStage2", 1);
                }
                if (isFanHighEnabled) {
                    if (getCmdSignal("fan and high", addr) > 0)
                        setCmdSignal("fan and high", 0, addr);
                }
                break;
            case FAN_HIGH2://firmware mapping is high

                if (isFanLowEnabled) {
                    setCmdSignal("fan and low", 1.0, addr);
                    relayStates.put("FanStage1", 1);
                }
                if (isFanMediumEnabled) {
                    if (getCmdSignal("fan and medium", addr) < 1.0)
                        setCmdSignal("fan and medium", 1.0, addr);
                    relayStates.put("FanStage2", 1);
                }
                if (isFanHighEnabled) {
                    if (getCmdSignal("fan and high", addr) < 1.0)
                        setCmdSignal("fan and high", 1.0, addr);
                    relayStates.put("FanStage3", 1);
                }
                break;
            case FAN_LOW:
                if (isFanLowEnabled) {
                    setCmdSignal("fan and low", 1.0, addr);
                    relayStates.put("FanStage1", 1);
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
                    if( occupied || (!occupied && ((roomTemp > setTempCooling) || ( (roomTemp > 0) && (roomTemp < setTempHeating)))) ){
                        // run periodic water valve check 2 mins on and 5 mins OFF
                        fcuPeriodicWaterValveCheck(addr, false);
                    }else if((twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() != 0) && (waterVal > 0)&& (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000) > 3) ) {
                        Log.d("FCU","fcuPeriodicWaterCheck AUTO fanmode="+addr+","+twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer()+","+((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000)+","+((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime())/60000));
                        setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                        twoPfcuDeviceMap.get(addr).setWaterValvePeriodicTimer(0);
                    }else if((twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime() != 0) && (waterVal > 0) && (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime())/60000) < 50)){
                        setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                    }
                    break;
                case COOL_ONLY:
                    if (supplyWaterTempTh2 > heatingThreshold) {
                        if (waterVal > 0)
                            setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                    } else {
                        if( occupied || (!occupied && ((roomTemp > setTempCooling) || ( (roomTemp > 0) && (roomTemp < setTempHeating)))) ){
                            // run periodic water valve check 2 mins on and 5 mins OFF
                            fcuPeriodicWaterValveCheck(addr, false);
                        }else if((twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() != 0) && (waterVal > 0)&& (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000) > 3) ) {
                            setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                            twoPfcuDeviceMap.get(addr).setWaterValvePeriodicTimer(0);
                        }else if((twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime() != 0) && (waterVal > 0) && (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime())/60000) < 50)){
                            setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                        }
                    }
                    break;
                case HEAT_ONLY:
                    if (supplyWaterTempTh2 < coolingThreshold) {
                        if (waterVal > 0)
                            setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                    } else {
                        if( occupied || (!occupied && ((roomTemp > setTempCooling) || ( (roomTemp > 0) && (roomTemp < setTempHeating)))) ){
                            // run periodic water valve check 2 mins on and 5 mins OFF
                            fcuPeriodicWaterValveCheck(addr, false);
                        }else if((twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() != 0) && (waterVal > 0)&& (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000) > 3) ) {
                            setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                            twoPfcuDeviceMap.get(addr).setWaterValvePeriodicTimer(0);
                        }else if((twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime() != 0) && (waterVal > 0) && (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime())/60000) < 50)){
                            setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                        }
                    }
                    break;
                case OFF:
                    if (waterVal > 0)
                        setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                    break;
            }
        }
        if(isAuxHeatingEnabled){
            if(getCmdSignal("aux and heating",addr) > 0)
                setCmdSignal("aux and heating",0, addr);
        }
        StandaloneScheduler.updateSmartStatStatus(equipId, DEADBAND,relayStates ,ZoneTempState.NONE);
    }
    private void resetRelays(String equipId, short node, ZoneTempState temperatureState) {
        //reset all relays to 0
        setCmdSignal("fan and medium",0,node);
        setCmdSignal("fan and high",0,node);
        setCmdSignal("fan and low",0,node);
        setCmdSignal("aux and heating",0,node);
        if(temperatureState == ZoneTempState.FAN_OP_MODE_OFF)
            setCmdSignal("water and valve",0,node);
        StandaloneScheduler.updateSmartStatStatus(equipId, DEADBAND,new HashMap<String, Integer>() ,temperatureState);
    }

    private void twoPipeFCUCoolOnlyMode(String equipId, short addr, double roomTemp,Occupied occuStatus,StandaloneFanSpeed fanSpeed){

        double hysteresis = StandaloneTunerUtil.getStandaloneStage1Hysteresis(equipId);
        boolean isFanMediumEnabled = getConfigEnabled("relay1",addr) > 0 ? true : false;
        boolean isFanHighEnabled = getConfigEnabled("relay2",addr) > 0 ? true : false;
        boolean isFanLowEnabled = getConfigEnabled("relay3", addr) > 0 ? true : false; //relay3 for fan low
        boolean isAuxHeatingEnabled = getConfigEnabled("relay4",addr)> 0 ? true : false;//Aux Heating
        boolean isWaterValve = getConfigEnabled("relay6",addr) > 0 ? true : false;
        double coolingDeadband = 2.0;
        double heatingDeadband = 2.0;
        boolean occupied = false;
        if(occuStatus != null){
            coolingDeadband = occuStatus.getCoolingDeadBand();
            heatingDeadband = occuStatus.getHeatingDeadBand();
            occupied = occuStatus.isOccupied();
        }
        HashMap<String,Integer> relayStates = new HashMap<String, Integer>();

        switch (fanSpeed){
            case AUTO:
                if(isFanLowEnabled){
                    if(roomTemp >= setTempCooling || (roomTemp <= setTempHeating)){
                        setCmdSignal("fan and low", 1.0 , addr);
                        relayStates.put("FanStage1",1);
                    }else if( (roomTemp > (setTempHeating+hysteresis)) && (roomTemp <= setTempCooling - hysteresis)){
                        setCmdSignal("fan and low",0,addr);
                    }else {
                        if(getCmdSignal("fan and low",addr) > 0)
                            relayStates.put("FanStage1",1);
                    }
                }
                if(isFanMediumEnabled){
                    if((roomTemp >= setTempCooling + coolingDeadband) || (roomTemp <= (setTempHeating - heatingDeadband))){
                        setCmdSignal("fan and medium", 1.0 , addr);
                        relayStates.put("FanStage2",1);
                    }else if((roomTemp >= setTempHeating) && (roomTemp <= setTempCooling)){
                        setCmdSignal("fan and medium",0,addr);
                    }else {
                        if(getCmdSignal("fan and medium",addr) > 0)
                            relayStates.put("FanStage2",1);
                    }
                }
                if(isFanHighEnabled){
                    if((roomTemp >= (setTempCooling + (coolingDeadband * 2.0))) || (roomTemp <= (setTempHeating - (heatingDeadband * 2.0)))){
                        setCmdSignal("fan and high", 1.0 , addr);
                        relayStates.put("FanStage3",1);
                    }else if((roomTemp >= (setTempHeating+heatingDeadband)) && (roomTemp <= (setTempCooling + coolingDeadband))){
                        setCmdSignal("fan and high",0,addr);
                    }else {
                        if(getCmdSignal("fan and high",addr) > 0)
                            relayStates.put("FanStage3",1);
                    }
                }
                break;
            case FAN_HIGH://Firmware mapping is medium for pfcu but for CPU and HPU it is high

                if (isFanLowEnabled) {
                    if(occupied) {
                        setCmdSignal("fan and low", 1.0, addr);
                        relayStates.put("FanStage1", 1);
                    }
                }
                if(isFanMediumEnabled){
                    if(getCmdSignal("fan and medium",addr) < 1.0)
                        setCmdSignal("fan and medium",1.0,addr);
                    relayStates.put("FanStage2",1);
                }
                if(isFanHighEnabled){
                    if(getCmdSignal("fan and high",addr) > 0)
                        setCmdSignal("fan and high",0,addr);
                }
                break;
            case FAN_HIGH2://firmware mapping is high, only for pfcu

                if (isFanLowEnabled) {
                    setCmdSignal("fan and low",1.0,addr);
                    relayStates.put("FanStage1",1);
                }
                if(isFanMediumEnabled){
                    if(getCmdSignal("fan and medium",addr) < 1.0)
                        setCmdSignal("fan and medium",1.0,addr);
                    relayStates.put("FanStage2",1);
                }
                if(isFanHighEnabled){
                    if(getCmdSignal("fan and high",addr) < 1.0)
                        setCmdSignal("fan and high",1.0,addr);
                    relayStates.put("FanStage3",1);
                }
                break;
            case FAN_LOW:
                if (isFanLowEnabled) {
                    setCmdSignal("fan and low",1.0,addr);
                    relayStates.put("FanStage1",1);
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
                relayStates.put("CoolingStage1",1);
                twoPfcuDeviceMap.get(addr).setWaterValveLastOnTime(System.currentTimeMillis());
                setCmdSignal("pipe2 and fcu and water and valve",1.0,addr);
            }else if(roomTemp <= (setTempCooling - hysteresis) /*&& (twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() == 0)*/){
                if(getCmdSignal("pipe2 and fcu and water and valve",addr) > 0)
                    setCmdSignal("pipe2 and fcu and water and valve",0,addr);
            }else {
                if(getCmdSignal("pipe2 and fcu and water and valve",addr) > 0)
                    relayStates.put("CoolingStage1",1);
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
        StandaloneScheduler.updateSmartStatStatus(equipId, COOLING,relayStates,ZoneTempState.NONE);
    }

    private void twoPipeFCUHeatOnlyMode(String equipId, short addr, double roomTemp,Occupied occuStatus,StandaloneFanSpeed fanSpeed){
        double hysteresis = StandaloneTunerUtil.getStandaloneStage1Hysteresis(equipId);
        boolean isFanMediumEnabled = getConfigEnabled("relay1",addr) > 0 ? true : false;
        boolean isFanHighEnabled = getConfigEnabled("relay2",addr) > 0 ? true : false;
        boolean isFanLowEnabled = getConfigEnabled("relay3", addr) > 0 ? true : false; //relay3 for fan low
        boolean isAuxHeatingEnabled = getConfigEnabled("relay4",addr)> 0 ? true : false;//Aux Heating
        boolean isWaterValve = getConfigEnabled("relay6",addr) > 0 ? true : false;
        double heatingDeadband = 2.0;
        boolean occupied = false;
        if(occuStatus != null){
            heatingDeadband = occuStatus.getHeatingDeadBand();
            occupied = occuStatus.isOccupied();
        }
        HashMap<String,Integer> relayStates = new HashMap<String, Integer>();

        switch (fanSpeed){
            case AUTO:
                if(isFanLowEnabled){
                    if(roomTemp <= setTempHeating){
                        setCmdSignal("fan and low", 1.0 , addr);
                        relayStates.put("FanStage1",1);
                    }else if(roomTemp >= setTempHeating + hysteresis){
                        setCmdSignal("fan and low",0,addr);
                    }else {
                        if(getCmdSignal("fan and low",addr) > 0)
                            relayStates.put("FanStage1",1);
                    }
                }
                if(isFanMediumEnabled){
                    if(roomTemp <= (setTempHeating - heatingDeadband)){
                        setCmdSignal("fan and medium", 1.0 , addr);
                        relayStates.put("FanStage2",1);
                    }else if(roomTemp >= setTempHeating){
                        setCmdSignal("fan and medium",0,addr);
                    }else {
                        if(getCmdSignal("fan and medium",addr) > 0)
                            relayStates.put("FanStage2",1);
                    }
                }
                if(isFanHighEnabled){
                    if(roomTemp <= (setTempHeating - (heatingDeadband * 2.0))){
                        setCmdSignal("fan and high", 1.0 , addr);
                        relayStates.put("FanStage3",1);
                    }else if(roomTemp >= (setTempHeating - heatingDeadband)){
                        setCmdSignal("fan and high",0,addr);
                    }else {
                        if(getCmdSignal("fan and high",addr) > 0)
                            relayStates.put("FanStage3",1);
                    }
                }
                break;
            case FAN_HIGH://Firmware mapping is medium for pfcu but for CPU and HPU it is high

                if (isFanLowEnabled) {
                    if(occupied) {
                        setCmdSignal("fan and low", 1.0, addr);
                        relayStates.put("FanStage1", 1);
                    }
                }
                if(isFanMediumEnabled){
                    if(getCmdSignal("fan and medium",addr) < 1.0)
                        setCmdSignal("fan and medium",1.0,addr);
                    relayStates.put("FanStage2",1);
                }
                if(isFanHighEnabled){
                    if(getCmdSignal("fan and high",addr) > 0)
                        setCmdSignal("fan and high",0,addr);
                }
                break;
            case FAN_HIGH2://firmware mapping is high, only for pfcu

                if (isFanLowEnabled) {
                    setCmdSignal("fan and low",1.0,addr);
                    relayStates.put("FanStage1",1);
                }
                if(isFanMediumEnabled){
                    if(getCmdSignal("fan and medium",addr) < 1.0)
                        setCmdSignal("fan and medium",1.0,addr);
                    relayStates.put("FanStage2",1);
                }
                if(isFanHighEnabled){
                    if(getCmdSignal("fan and high",addr) < 1.0)
                        setCmdSignal("fan and high",1.0,addr);
                    relayStates.put("FanStage3",1);
                }
                break;
            case FAN_LOW:
                if (isFanLowEnabled) {
                    setCmdSignal("fan and low",1.0,addr);
                    relayStates.put("FanStage1",1);
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
            if(roomTemp <= setTempHeating){
                twoPfcuDeviceMap.get(addr).setWaterValveLastOnTime(System.currentTimeMillis());
                relayStates.put("HeatingStage1",1);
                setCmdSignal("pipe2 and fcu and water and valve",1.0,addr);
            }else if(roomTemp >= (setTempHeating + hysteresis) /*&& (twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() == 0)*/){ //Todo check for periodic timer == 0
                if(getCmdSignal("pipe2 and fcu and water and valve",addr) > 0)
                    setCmdSignal("pipe2 and fcu and water and valve",0,addr);
            }else {
                if(getCmdSignal("pipe2 and fcu and water and valve",addr) > 0)
                    relayStates.put("HeatingStage1",1);
            }
        }
        if(isAuxHeatingEnabled){
            if(getCmdSignal("aux and heating",addr) > 0)
                setCmdSignal("aux and heating",0,addr);
        }
        StandaloneScheduler.updateSmartStatStatus(equipId, HEATING,relayStates ,ZoneTempState.NONE);
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
        Log.d("FCU","fcuPeriodicWaterCheck="+addr+","+isHourlyCheck+","+twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer()+","+((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000)+","+((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime())/60000));
        if((twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() == 0) ||(((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000) < 2)){ //ON for two mins
            if((getCmdSignal("pipe2 and fcu and water and valve",addr) < 1.0) || ( twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() == 0)) {
                setCmdSignal("pipe2 and fcu and water and valve", 1.0, addr);
                twoPfcuDeviceMap.get(addr).setWaterValvePeriodicTimer(System.currentTimeMillis());
                if(!isHourlyCheck)
                    twoPfcuDeviceMap.get(addr).setWaterValveLastOnTime(System.currentTimeMillis());
                return true;
            }
        }else if(!isHourlyCheck && (((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000) < 7)){//OFF for 5 mins
            //if(getCmdSignal("pfcu2 and water and valve",addr) > 0) {
                setCmdSignal("pipe2 and fcu and water and valve", 0, addr);
                return true;
            //}
        }else
            twoPfcuDeviceMap.get(addr).setWaterValvePeriodicTimer(0);
        return false;

    }
    private boolean fcuWaterSamplingEveryHour(short addr){
        if(twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime() != 0){
            if((((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValveLastOnTime())/(60000)) > 58)){//Starts with 0 and at 60th minute we turn on
                if((twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer() == 0 ) || ((((System.currentTimeMillis() - twoPfcuDeviceMap.get(addr).getWaterValvePeriodicTimer())/60000) < 2))){
                    return fcuPeriodicWaterValveCheck(addr,true);
                }else {
                    twoPfcuDeviceMap.get(addr).setWaterValveLastOnTime(System.currentTimeMillis());
                    twoPfcuDeviceMap.get(addr).setWaterValvePeriodicTimer(0);
                    if(getCmdSignal("pipe2 and fcu and water and valve",addr) > 0)
                        setCmdSignal("pipe2 and fcu and water and valve",0,addr);
                }

            }
        }else {
            twoPfcuDeviceMap.get(addr).setWaterValveLastOnTime(System.currentTimeMillis());
        }
        return false;
    }
}
