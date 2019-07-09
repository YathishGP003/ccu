package a75f.io.logic.bo.building.ss4pfcu;

import android.util.Log;

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
import static a75f.io.logic.bo.building.definitions.StandaloneFanSpeed.OFF;

public class FourPipeFanCoilUnitProfile extends ZoneProfile {



    public static String TAG = FourPipeFanCoilUnitProfile.class.getSimpleName().toUpperCase();

    public HashMap<Short, FourPipeFanCoilUnitEquip> fourPfcuDeviceMap;
    double setTempCooling = 74.0;
    double setTempHeating = 70.0;
    double airflowTempTh1 = 0.0;

    public FourPipeFanCoilUnitProfile(){
        fourPfcuDeviceMap = new HashMap<>();
    }


    @Override
    public ProfileType getProfileType() {

        return ProfileType.SMARTSTAT_FOUR_PIPE_FCU;
    }



    @Override
    public boolean isZoneDead() {

        double buildingLimitMax =  TunerUtil.readTunerValByQuery("building and limit and max", L.ccu().systemProfile.getSystemEquipRef());
        double buildingLimitMin =  TunerUtil.readTunerValByQuery("building and limit and min", L.ccu().systemProfile.getSystemEquipRef());

        double tempDeadLeeway = TunerUtil.readTunerValByQuery("temp and dead and leeway",L.ccu().systemProfile.getSystemEquipRef());

        for (short node : fourPfcuDeviceMap.keySet())
        {
            double curTemp = fourPfcuDeviceMap.get(node).getCurrentTemp();
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

        for (short node : fourPfcuDeviceMap.keySet()) {
            if (fourPfcuDeviceMap.get(node) == null) {
                addLogicalMap(node);
                Log.d(TAG, " Logical Map added for smartstat " + node);
                continue;
            }
            Log.d(TAG, "SmartStat 4PFCU profile");
            FourPipeFanCoilUnitEquip fourPfcuDevice = fourPfcuDeviceMap.get(node);
            if (fourPfcuDevice.profileType != ProfileType.SMARTSTAT_FOUR_PIPE_FCU)
                continue;
            double roomTemp = fourPfcuDevice.getCurrentTemp();
            Equip fourPfcuEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
            if (isZoneDead()) {
                resetRelays(fourPfcuEquip.getId(), node, ZoneTempState.TEMP_DEAD);
                fourPfcuDevice.setStatus(state.ordinal());
                String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \"" + node + "\"");
                if (!curStatus.equals("Zone Temp Dead")) {
                    CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"", "Zone Temp Dead");
                }
                continue;

            }
            double averageDesiredTemp = (setTempCooling + setTempHeating) / 2.0;
            if (averageDesiredTemp != fourPfcuDevice.getDesiredTemp()) {
                fourPfcuDevice.setDesiredTemp(averageDesiredTemp);
            }
            Log.d(TAG, " smartstat 4pfcu, updates 111=" + fourPfcuEquip.getRoomRef() + "," + setTempHeating + "," + setTempCooling + "," + roomTemp );


            double ssOperatingMode = getOperationalModes("temp", fourPfcuEquip.getId());
            double ssFanOpMode = getOperationalModes("fan", fourPfcuEquip.getId());
            StandaloneOperationalMode opMode = StandaloneOperationalMode.values()[(int) ssOperatingMode];
            StandaloneFanSpeed fanSpeed = StandaloneFanSpeed.values()[(int) ssFanOpMode];
            setTempCooling = fourPfcuDevice.getDesiredTempCooling();
            setTempHeating = fourPfcuDevice.getDesiredTempHeating();
            String zoneId = HSUtil.getZoneIdFromEquipId(fourPfcuEquip.getId());
            Occupied occuStatus = ScheduleProcessJob.getOccupiedModeCache(zoneId);

            boolean occupied = (occuStatus == null ? false : occuStatus.isOccupied());
            //For dual temp but for single mode we use tuners

            if (!occupied && (fanSpeed != OFF)) {
                if (fanSpeed != StandaloneFanSpeed.AUTO) {
                    StandaloneScheduler.updateOperationalPoints(fourPfcuEquip.getId(), "fan and operation and mode", StandaloneFanSpeed.AUTO.ordinal());
                    fanSpeed = StandaloneFanSpeed.AUTO;
                }
            }
            if(fanSpeed != OFF){

                switch (opMode){
                    case AUTO:
                        if(roomTemp < averageDesiredTemp)
                            fcu4HeatOnlyMode(fourPfcuEquip.getId(),node,roomTemp,occuStatus,fanSpeed);
                        else
                            fcu4CoolOnlyMode(fourPfcuEquip.getId(),node,roomTemp,occuStatus,fanSpeed);
                        break;
                    case COOL_ONLY:
                    	fcu4CoolOnlyMode(fourPfcuEquip.getId(),node,roomTemp,occuStatus,fanSpeed);
                        break;
                    case HEAT_ONLY:
                            fcu4HeatOnlyMode(fourPfcuEquip.getId(),node,roomTemp,occuStatus,fanSpeed);
                        break;
                    case OFF:
                        fanOperationalModes(fourPfcuEquip.getId(),node,occuStatus,fanSpeed);
                        break;
                }
            }else {
                resetRelays(fourPfcuEquip.getId(),node,ZoneTempState.FAN_OP_MODE_OFF);
            }
            if (occuStatus != null) {
                fourPfcuDevice.setProfilePoint("occupancy and status", occuStatus.isOccupied() ? Occupancy.OCCUPIED.ordinal() : (occuStatus.isPreconditioning() ? Occupancy.PRECONDITIONING.ordinal() : (occuStatus.isForcedOccupied() ? Occupancy.FORCED_OCCUPIED.ordinal() : 0)));
            } else {
                fourPfcuDevice.setProfilePoint("occupancy and status", occupied ? 1 : 0);
            }
        }
    }


    private void resetRelays(String equipId, short node, ZoneTempState temperatureState) {
        //reset all relays to 0
        setCmdSignal("fan and medium",0,node);
        setCmdSignal("fan and high",0,node);
        setCmdSignal("fan and low",0,node);
        setCmdSignal("water and valve and heating",0,node);
            setCmdSignal("water and valve and cooling",0,node);
        StandaloneScheduler.updateSmartStatStatus(equipId, DEADBAND,new HashMap<String, Integer>() ,temperatureState);
    }
    @Override
    public ZoneState getState() {
        return state;
    }

    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address) {

        return fourPfcuDeviceMap.get(address) != null ? fourPfcuDeviceMap.get(address).getProfileConfiguration() : null;
    }
    /**
     * Only creates a run time instance of logical map for initialize.
     *
     * @param addr
     */
    public void addLogicalMap(short addr) {
        FourPipeFanCoilUnitEquip deviceMap = new FourPipeFanCoilUnitEquip(getProfileType(), addr);
        fourPfcuDeviceMap.put(addr, deviceMap);
    }

    /**
     * Only creates a run time instance of logical map for initialize.
     *
     * @param addr
     */
    public void addLogicalMap(short addr, String roomRef) {
        FourPipeFanCoilUnitEquip deviceMap = new FourPipeFanCoilUnitEquip(getProfileType(), addr);
        fourPfcuDeviceMap.put(addr, deviceMap);
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
    public void addLogicalMapAndPoints(short addr, FourPipeFanCoilUnitConfiguration config, String floorRef, String zoneRef) {
        FourPipeFanCoilUnitEquip deviceMap = new FourPipeFanCoilUnitEquip(getProfileType(), addr);
        deviceMap.createHaystackPoints(config, floorRef, zoneRef);

        fourPfcuDeviceMap.put(addr, deviceMap);
    }

    public void updateLogicalMapAndPoints(short addr, FourPipeFanCoilUnitConfiguration config, String roomRef) {
        FourPipeFanCoilUnitEquip deviceMap = fourPfcuDeviceMap.get(addr);
        deviceMap.updateHaystackPoints(config);
    }

    public Set<Short> getNodeAddresses() {
        return fourPfcuDeviceMap.keySet();
    }

    @Override
    public Equip getEquip() {
        for (short nodeAddress : fourPfcuDeviceMap.keySet()) {
            HashMap equip = CCUHsApi.getInstance().read("equip and group == \"" + nodeAddress + "\"");
            return new Equip.Builder().setHashMap(equip).build();
        }
        return null;
    }

    @Override
    public double getCurrentTemp() {
        for (short nodeAddress : fourPfcuDeviceMap.keySet()) {
            return fourPfcuDeviceMap.get(nodeAddress).getCurrentTemp();
        }
        return 0;
    }
    public double getDisplayCurrentTemp()
    {
        return getAverageZoneTemp();
    }


    @Override
    public double getAverageZoneTemp() {
        double tempTotal = 0;
        int nodeCount = 0;
        for (short nodeAddress : fourPfcuDeviceMap.keySet()) {
            if (fourPfcuDeviceMap.get(nodeAddress) == null) {
                continue;
            }
            if (fourPfcuDeviceMap.get(Short.valueOf(nodeAddress)).getCurrentTemp() > 0) {
                tempTotal += fourPfcuDeviceMap.get(Short.valueOf(nodeAddress)).getCurrentTemp();
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
        return CCUHsApi.getInstance().readHisValByQuery("point and standalone and fcu and pipe4 and cmd and his and " + cmd + " and group == \"" + node + "\"");
    }

    public void setCmdSignal(String cmd, double val, short node) {
        CCUHsApi.getInstance().writeHisValByQuery("point and standalone and fcu and pipe4 and cmd and his and " + cmd + " and group == \"" + node + "\"", val);
    }

    public double getOperationalModes(String cmd, String equipRef) {
        return CCUHsApi.getInstance().readHisValByQuery("point and standalone and operation and mode and his and " + cmd + " and equipRef == \"" + equipRef + "\"");
    }

    private void fanOperationalModes(String equipId, short addr, Occupied occuStatus, StandaloneFanSpeed fanSpeed){

        boolean isFanMediumEnabled = getConfigEnabled("relay1",addr) > 0 ? true : false;
        boolean isFanHighEnabled = getConfigEnabled("relay2",addr) > 0 ? true : false;
        boolean isFanLowEnabled = getConfigEnabled("relay3", addr) > 0 ? true : false; //relay3 for fan low
        boolean occupied = false;
        if(occuStatus != null)
            occupied = occuStatus.isOccupied();
        HashMap<String,Integer> relayStates = new HashMap<String, Integer>();
        //Deactivate all relays if fan speed is auto. Else R1,R2,R3 depends on user selection
        switch (fanSpeed){
            case AUTO:
                resetRelays(equipId,addr,ZoneTempState.NONE);
                break;

            case FAN_HIGH://Firmware mapping is medium for pfcu but for CPU and HPU it is high

                if (isFanLowEnabled && occupied) {
                        setCmdSignal("fan and low", 1.0, addr);
                        relayStates.put("FanStage1", 1);
                }
                if(isFanMediumEnabled && occupied){
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

                if (isFanLowEnabled && occupied) {
                    setCmdSignal("fan and low",1.0,addr);
                    relayStates.put("FanStage1",1);
                }
                if(isFanMediumEnabled && occupied){
                    if(getCmdSignal("fan and medium",addr) < 1.0)
                        setCmdSignal("fan and medium",1.0,addr);
                    relayStates.put("FanStage2",1);
                }
                if(isFanHighEnabled && occupied){
                    if(getCmdSignal("fan and high",addr) < 1.0)
                        setCmdSignal("fan and high",1.0,addr);
                    relayStates.put("FanStage3",1);
                }
                break;
            case FAN_LOW:
                if (isFanLowEnabled && occupied) {
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
        StandaloneScheduler.updateSmartStatStatus(equipId, DEADBAND,relayStates,ZoneTempState.NONE);
    }

    private void fcu4CoolOnlyMode(String equipId, short addr,double roomTemp, Occupied occuStatus,StandaloneFanSpeed fanSpeed){
        double hysteresis = StandaloneTunerUtil.getStandaloneStage1Hysteresis(equipId);
        boolean isFanMediumEnabled = getConfigEnabled("relay1",addr) > 0 ? true : false;
        boolean isFanHighEnabled = getConfigEnabled("relay2",addr) > 0 ? true : false;
        boolean isFanLowEnabled = getConfigEnabled("relay3", addr) > 0 ? true : false; //relay3 for fan low
        boolean isHeatingWaterValve = getConfigEnabled("relay4",addr)> 0 ? true : false;// Heating water valve
        boolean isCoolingWaterValve = getConfigEnabled("relay6",addr) > 0 ? true : false;//Cooling water valve
        double coolingDeadband = 2.0;
        boolean occupied = false;
        if(occuStatus != null){
            coolingDeadband = occuStatus.getCoolingDeadBand();
            occupied = occuStatus.isOccupied();
        }
        HashMap<String,Integer> relayStates = new HashMap<String, Integer>();
        if(isHeatingWaterValve){

            if(getCmdSignal("water and valve and heating",addr) > 0)
                setCmdSignal("water and valve and heating",0,addr);
        }
        if(isCoolingWaterValve){
            if(roomTemp >= setTempCooling){
                relayStates.put("CoolingStage1",1);
                setCmdSignal("cooling and water and valve",1.0,addr);
            }else if(roomTemp <= (setTempCooling - hysteresis)){
                if(getCmdSignal("cooling and water and valve",addr) > 0)
                    setCmdSignal("cooling and water and valve",0,addr);
            }else {
                if(getCmdSignal("cooling and water and valve",addr) > 0)
                    relayStates.put("CoolingStage1",1);
            }
        }
        switch (fanSpeed){
            case AUTO:
                if(isFanLowEnabled){
                    if(roomTemp >= setTempCooling ){
                        setCmdSignal("fan and low", 1.0 , addr);
                        relayStates.put("FanStage1",1);
                    }else if( (roomTemp <= setTempCooling - hysteresis)){
                        setCmdSignal("fan and low",0,addr);
                    }else {
                        if(getCmdSignal("fan and low",addr) > 0)
                            relayStates.put("FanStage1",1);
                    }
                }
                if(isFanMediumEnabled){
                    if((roomTemp >= setTempCooling + coolingDeadband) ){
                        setCmdSignal("fan and medium", 1.0 , addr);
                        relayStates.put("FanStage2",1);
                    }else if( (roomTemp <= setTempCooling)){
                        setCmdSignal("fan and medium",0,addr);
                    }else {
                        if(getCmdSignal("fan and medium",addr) > 0)
                            relayStates.put("FanStage2",1);
                    }
                }
                if(isFanHighEnabled){
                    if((roomTemp >= (setTempCooling + (coolingDeadband * 2.0)))){
                        setCmdSignal("fan and high", 1.0 , addr);
                        relayStates.put("FanStage3",1);
                    }else if((roomTemp <= (setTempCooling + coolingDeadband))){
                        setCmdSignal("fan and high",0,addr);
                    }else {
                        if(getCmdSignal("fan and high",addr) > 0)
                            relayStates.put("FanStage3",1);
                    }
                }
                break;
            case FAN_HIGH://Firmware mapping is medium for pfcu but for CPU and HPU it is high

                if (isFanLowEnabled && occupied) {
                    setCmdSignal("fan and low", 1.0, addr);
                    relayStates.put("FanStage1", 1);
                }
                if(isFanMediumEnabled && occupied){
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

                if (isFanLowEnabled && occupied) {
                    setCmdSignal("fan and low",1.0,addr);
                    relayStates.put("FanStage1",1);
                }
                if(isFanMediumEnabled && occupied){
                    if(getCmdSignal("fan and medium",addr) < 1.0)
                        setCmdSignal("fan and medium",1.0,addr);
                    relayStates.put("FanStage2",1);
                }
                if(isFanHighEnabled && occupied){
                    if(getCmdSignal("fan and high",addr) < 1.0)
                        setCmdSignal("fan and high",1.0,addr);
                    relayStates.put("FanStage3",1);
                }
                break;
            case FAN_LOW:
                if (isFanLowEnabled && occupied) {
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
        StandaloneScheduler.updateSmartStatStatus(equipId, COOLING,relayStates,ZoneTempState.NONE);
    }
    private void fcu4HeatOnlyMode(String equipId, short addr,double roomTemp, Occupied occuStatus,StandaloneFanSpeed fanSpeed){
        double hysteresis = StandaloneTunerUtil.getStandaloneStage1Hysteresis(equipId);
        boolean isFanMediumEnabled = getConfigEnabled("relay1",addr) > 0 ? true : false;
        boolean isFanHighEnabled = getConfigEnabled("relay2",addr) > 0 ? true : false;
        boolean isFanLowEnabled = getConfigEnabled("relay3", addr) > 0 ? true : false; //relay3 for fan low
        boolean isHeatingWaterValve = getConfigEnabled("relay4",addr)> 0 ? true : false;// Heating water valve
        boolean isCoolingWaterValve = getConfigEnabled("relay6",addr) > 0 ? true : false;//Cooling water valve
        double heatingDeadband = 2.0;
        boolean occupied = false;
        if(occuStatus != null){
            heatingDeadband = occuStatus.getHeatingDeadBand();
            occupied = occuStatus.isOccupied();
        }
        HashMap<String,Integer> relayStates = new HashMap<String, Integer>();
        if(isCoolingWaterValve){
            if(getCmdSignal("water and valve and cooling",addr) > 0)
                setCmdSignal("water and valve and cooling",0,addr);
        }
        if(isHeatingWaterValve){
            if(roomTemp <= setTempHeating){
                relayStates.put("HeatingStage1",1);
                setCmdSignal("heating and water and valve",1.0,addr);
            }else if(roomTemp >= (setTempHeating + hysteresis)){
                if(getCmdSignal("heating and water and valve",addr) > 0)
                    setCmdSignal("heating and water and valve",0,addr);
            }else {
                if(getCmdSignal("heating and water and valve",addr) > 0)
                    relayStates.put("HeatingStage1",1);
            }
        }
        switch (fanSpeed){
            case AUTO:
                if(isFanLowEnabled){
                    if( (roomTemp <= setTempHeating)){
                        setCmdSignal("fan and low", 1.0 , addr);
                        relayStates.put("FanStage1",1);
                    }else if( (roomTemp >= (setTempHeating+hysteresis))){
                        setCmdSignal("fan and low",0,addr);
                    }else {
                        if(getCmdSignal("fan and low",addr) > 0)
                            relayStates.put("FanStage1",1);
                    }
                }
                if(isFanMediumEnabled){
                    if((roomTemp <= (setTempHeating - heatingDeadband))){
                        setCmdSignal("fan and medium", 1.0 , addr);
                        relayStates.put("FanStage2",1);
                    }else if((roomTemp >= setTempHeating)){
                        setCmdSignal("fan and medium",0,addr);
                    }else {
                        if(getCmdSignal("fan and medium",addr) > 0)
                            relayStates.put("FanStage2",1);
                    }
                }
                if(isFanHighEnabled){
                    if((roomTemp <= (setTempHeating - (heatingDeadband * 2.0)))){
                        setCmdSignal("fan and high", 1.0 , addr);
                        relayStates.put("FanStage3",1);
                    }else if((roomTemp >= (setTempHeating-heatingDeadband)) ){
                        setCmdSignal("fan and high",0,addr);
                    }else {
                        if(getCmdSignal("fan and high",addr) > 0)
                            relayStates.put("FanStage3",1);
                    }
                }
                break;
            case FAN_HIGH://Firmware mapping is medium for pfcu but for CPU and HPU it is high

                if (isFanLowEnabled && occupied) {
                    setCmdSignal("fan and low", 1.0, addr);
                    relayStates.put("FanStage1", 1);
                }
                if(isFanMediumEnabled && occupied){
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

                if (isFanLowEnabled && occupied) {
                    setCmdSignal("fan and low",1.0,addr);
                    relayStates.put("FanStage1",1);
                }
                if(isFanMediumEnabled && occupied){
                    if(getCmdSignal("fan and medium",addr) < 1.0)
                        setCmdSignal("fan and medium",1.0,addr);
                    relayStates.put("FanStage2",1);
                }
                if(isFanHighEnabled && occupied){
                    if(getCmdSignal("fan and high",addr) < 1.0)
                        setCmdSignal("fan and high",1.0,addr);
                    relayStates.put("FanStage3",1);
                }
                break;
            case FAN_LOW:
                if (isFanLowEnabled && occupied) {
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
        StandaloneScheduler.updateSmartStatStatus(equipId, HEATING,relayStates,ZoneTempState.NONE);
    }
}
