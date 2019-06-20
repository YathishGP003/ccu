package a75f.io.logic.bo.building.sscpu;

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
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.ZoneTempState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.SmartStatFanRelayType;
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

public class ConventionalUnitProfile extends ZoneProfile {


    public static String TAG = ConventionalUnitProfile.class.getSimpleName().toUpperCase();

    public HashMap<Short, ConventionalUnitLogicalMap> cpuDeviceMap;
    double setTempCooling = 73.0;
    double setTempHeating = 71.0;
    boolean occupied;

    public ConventionalUnitProfile(){

        cpuDeviceMap = new HashMap<>();
    }

    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.SMARTSTAT_CONVENTIONAL_PACK_UNIT;
    }
	
	@Override
    public boolean isZoneDead() {
    
        double buildingLimitMax =  TunerUtil.readTunerValByQuery("building and limit and max", L.ccu().systemProfile.getSystemEquipRef());
        double buildingLimitMin =  TunerUtil.readTunerValByQuery("building and limit and min", L.ccu().systemProfile.getSystemEquipRef());
    
        double tempDeadLeeway = TunerUtil.readTunerValByQuery("temp and dead and leeway",L.ccu().systemProfile.getSystemEquipRef());
    
        for (short node : cpuDeviceMap.keySet())
        {
            double curTemp = cpuDeviceMap.get(node).getCurrentTemp();
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

        if(mInterface != null)
        {
            mInterface.refreshView();
        }

        for (short node : cpuDeviceMap.keySet())
        {
            if (cpuDeviceMap.get(node) == null) {
                addLogicalMap(node);
                Log.d(TAG, " Logical Map added for smartstat " + node);
                continue;
            }
            Log.d(TAG,"SmartStat CPU profile");

            HashMap<String,Integer> relayStages = new HashMap<String,Integer>();
            ConventionalUnitLogicalMap cpuDevice = cpuDeviceMap.get(node);
            if(cpuDevice.profileType != ProfileType.SMARTSTAT_CONVENTIONAL_PACK_UNIT)
                continue;
            double roomTemp = cpuDevice.getCurrentTemp();
            Equip cpuEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
            if(isZoneDead()){
                resetRelays(cpuEquip.getId(),node);
                cpuDevice.setStatus(state.ordinal());
                String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \"" + node + "\"");
                if (!curStatus.equals("Zone Temp Dead")) {
                    CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"", "Zone Temp Dead");
                }
				Log.d(TAG,"Invalid Temp , skip controls update for "+node+" roomTemp : "+cpuDeviceMap.get(node).getCurrentTemp());
                continue;
            }
			setTempCooling = cpuDevice.getDesiredTempCooling();
            setTempHeating = cpuDevice.getDesiredTempHeating();
            double averageDesiredTemp = (setTempCooling+setTempHeating)/2.0;
            if (averageDesiredTemp != cpuDevice.getDesiredTemp())
            {
                cpuDevice.setDesiredTemp(averageDesiredTemp);
            }

            Log.d(TAG, " smartstat cpu, updates 111="+cpuEquip.getRoomRef()+","+setTempHeating+","+setTempCooling);

            String zoneId = HSUtil.getZoneIdFromEquipId(cpuEquip.getId());
            Occupied occuStatus = ScheduleProcessJob.getOccupiedModeCache(zoneId);
            double coolingDeadband = 2.0;
            double heatingDeadband = 2.0;
            if(occuStatus != null){
                coolingDeadband = occuStatus.getCoolingDeadBand();
                heatingDeadband = occuStatus.getHeatingDeadBand();
                occupied = occuStatus.isOccupied();
            }else
                occupied = false;
            //For dual temp but for single mode we use tuners
            double hysteresis = StandaloneTunerUtil.getStandaloneStage1Hysteresis(cpuEquip.getId());

            double ssOperatingMode = getOperationalModes("temp",cpuEquip.getId());
            double ssFanOpMode = getOperationalModes("fan",cpuEquip.getId());
            int fanStage2Type = (int)getConfigType("relay6",node);
            StandaloneOperationalMode opMode = StandaloneOperationalMode.values()[(int)ssOperatingMode];
            StandaloneFanSpeed fanSpeed = StandaloneFanSpeed.values()[(int)ssFanOpMode];
            SmartStatFanRelayType fanHighType = SmartStatFanRelayType.values()[(int)fanStage2Type];
            if(!occupied &&(fanSpeed != OFF ) ){
                if(fanSpeed != StandaloneFanSpeed.AUTO) {
                    StandaloneScheduler.updateOperationalPoints(cpuEquip.getId(), "fan and operation and mode", StandaloneFanSpeed.AUTO.ordinal());
                    fanSpeed = StandaloneFanSpeed.AUTO;
                }
            }

            if(occuStatus != null){
                cpuDevice.setProfilePoint("occupancy and status", occuStatus.isOccupied() ? Occupancy.OCCUPIED.ordinal() : (occuStatus.isPreconditioning() ? Occupancy.PRECONDITIONING.ordinal() : (occuStatus.isForcedOccupied() ? Occupancy.FORCED_OCCUPIED.ordinal() : 0)));
            }else {
                cpuDevice.setProfilePoint("occupancy and status", occupied ? 1 : 0);
            }
            double targetThreshold = 25.0;

            switch (fanHighType){
                case HUMIDIFIER:
                    targetThreshold = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and humidity and equipRef == \"" + cpuEquip.getId() + "\"");
                    break;
                case DE_HUMIDIFIER:
                    targetThreshold = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and dehumidifier and equipRef == \"" + cpuEquip.getId() + "\"");
                    break;
            }
            boolean isFanStage1Enabled = getConfigEnabled("relay3", node) > 0 ? true : false;
            boolean isFanStage2Enabled = getConfigEnabled("relay6", node) > 0 ? true : false;
            Log.d(TAG, " smartstat cpu, updates =" + node+","+roomTemp+","+occupied+","+coolingDeadband+","+state+","+isFanStage1Enabled+","+isFanStage2Enabled);
            if ((fanSpeed != OFF) && ((opMode == StandaloneOperationalMode.AUTO) || (opMode == StandaloneOperationalMode.COOL_ONLY))&& (roomTemp >= (setTempCooling - hysteresis)) )
            {

                //Zone is in Cooling
                state = COOLING;
                double cs1 = getConfigEnabled("relay1",node);
                boolean isCoolingStage1Enabled = cs1 > 0 ? true : false;
                boolean isCoolingStage2Enabled = getConfigEnabled("relay2", node) > 0 ? true : false;
                setCmdSignal("heating and stage1",0,node);
				setCmdSignal("heating and stage2",0,node);
                if (isCoolingStage1Enabled) {
                    if(roomTemp >= setTempCooling) {
                        relayStages.put("CoolingStage1",1);
                        relayStages.put("FanStage1",1);
                        setCmdSignal("cooling and stage1", 1.0, node);
                        if(isFanStage1Enabled) setCmdSignal("fan and stage1",1.0,node);
                    }else{
                        if (roomTemp <= (setTempCooling-hysteresis)){//Turn off stage1
                            if(getCmdSignal("cooling and stage1", node) > 0)
                                setCmdSignal("cooling and stage1",0,node);

                            if(occupied && isFanStage1Enabled && ((fanSpeed == StandaloneFanSpeed.FAN_LOW) || (fanSpeed == StandaloneFanSpeed.FAN_HIGH))){
                                relayStages.put("FanStage1",1);
                                setCmdSignal("fan and stage1",1.0,node);
                            }else if(isFanStage1Enabled)
                                setCmdSignal("fan and stage1",0,node);
                        }else {
                            if(getCmdSignal("cooling and stage1", node) > 0)relayStages.put("CoolingStage1",1);
                            if(getCmdSignal("fan and stage1", node) > 0)relayStages.put("FanStage1",1);
                        }
                    }
                }else{
                    //TODO Do we  need to send his data when not enabled???

                    if(occupied && isFanStage1Enabled && (fanSpeed == StandaloneFanSpeed.FAN_LOW)){
                        relayStages.put("FanStage1",1);
                        setCmdSignal("fan and stage1",1.0,node);
                    }else if(isFanStage1Enabled)
                        setCmdSignal("fan and stage1",0,node);
                    setCmdSignal("cooling and stage1", 0, node);
                }
                if(isCoolingStage2Enabled){
                    if (roomTemp >= (setTempCooling + coolingDeadband)) {
                        setCmdSignal("cooling and stage2", 1.0, node);
                        relayStages.put("CoolingStage2",1);
                        if(isFanStage2Enabled && (fanHighType == SmartStatFanRelayType.FAN_STAGE2)){
                            relayStages.put("FanStage2",1);
                            setCmdSignal("fan and stage2",1.0,node);
                        }
                    } else{
                        if (roomTemp <= setTempCooling) {//Turn off stage 2
                            if(getCmdSignal("cooling and stage2", node) > 0)
                                setCmdSignal("cooling and stage2", 0, node);
                            if (occupied && isFanStage2Enabled && (fanHighType == SmartStatFanRelayType.FAN_STAGE2) && (fanSpeed == StandaloneFanSpeed.FAN_HIGH)) {
                                relayStages.put("FanStage2", 1);
                                setCmdSignal("fan and stage2", 1.0, node);
                            } else if (isFanStage2Enabled && (fanHighType == SmartStatFanRelayType.FAN_STAGE2))
                                setCmdSignal("fan and stage2", 0, node);
                        }else {
                            if(getCmdSignal("cooling and stage2", node) > 0)relayStages.put("CoolingStage2",1);
                            if(getCmdSignal("fan and stage2", node) > 0)relayStages.put("FanStage2",1);
                        }
                    }
                }else{
                    if(occupied && isFanStage2Enabled && (fanHighType == SmartStatFanRelayType.FAN_STAGE2)&& (fanSpeed == StandaloneFanSpeed.FAN_HIGH)){
                        relayStages.put("FanStage2",1);
                        setCmdSignal("fan and stage2",1.0,node);
                    }
                    else if(isFanStage2Enabled && (fanHighType == SmartStatFanRelayType.FAN_STAGE2)) setCmdSignal("fan and stage2",0,node);
                    setCmdSignal("cooling and stage2", 0, node);
                }
                if(isFanStage2Enabled && ((fanHighType == SmartStatFanRelayType.HUMIDIFIER) || (fanHighType == SmartStatFanRelayType.DE_HUMIDIFIER)))
                    updateHumidityStatus(fanHighType,node,cpuDevice.getHumidity(),targetThreshold,relayStages);
                Log.d(TAG, " smartstat cpu,cooling updates =" + node+","+roomTemp+","+occupied+","+isCoolingStage1Enabled+","+opMode.name()+","+fanSpeed.name()+","+cs1);
            }
            else if ((fanSpeed != OFF) &&((opMode == StandaloneOperationalMode.AUTO) || (opMode == StandaloneOperationalMode.HEAT_ONLY))&&(roomTemp <= (setTempHeating + hysteresis)))
            {
                //Zone is in heating

                state = HEATING;
                setCmdSignal("cooling and stage1",0,node);
                setCmdSignal("cooling and stage2",0,node);
                boolean isHeatingStage1Enabled = getConfigEnabled("relay4", node) > 0;
                boolean isHeatingStage2Enabled = getConfigEnabled("relay5", node) > 0;
                if (isHeatingStage1Enabled)
                {
                    if(roomTemp <= setTempHeating ) {
                        setCmdSignal("heating and stage1", 1.0, node);
                        if(isFanStage1Enabled) setCmdSignal("fan and stage1",1.0,node);
                        relayStages.put("HeatingStage1",1);
                        relayStages.put("FanStage1",1);
                    }else {
                        if( roomTemp >= (setTempHeating + hysteresis)){
                            if(getCmdSignal("heating and stage1", node) > 0)
                                setCmdSignal("heating and stage1",0,node);

                            if(occupied && isFanStage1Enabled &&   ((fanSpeed == StandaloneFanSpeed.FAN_LOW) || (fanSpeed == StandaloneFanSpeed.FAN_HIGH))){
                                relayStages.put("FanStage1",1);
                                setCmdSignal("fan and stage1",1.0,node);
                            }else if(isFanStage1Enabled){
                                setCmdSignal("fan and stage1",0,node);
                            }
                        }else {
                            if(getCmdSignal("heating and stage1", node) > 0)relayStages.put("HeatingStage1",1);
                            if(getCmdSignal("fan and stage1", node) > 0)relayStages.put("FanStage1",1);
                        }
                    }
                }else{

                    if(occupied && isFanStage1Enabled &&  (fanSpeed == StandaloneFanSpeed.FAN_LOW)){
                        relayStages.put("FanStage1",1);
                        setCmdSignal("fan and stage1",1.0,node);
                    }else if(isFanStage1Enabled)setCmdSignal("fan and stage1",0,node);

                    setCmdSignal("heating and stage1", 0, node);
                }
                if(isHeatingStage2Enabled){

                    if (roomTemp <= (setTempHeating - heatingDeadband)) {
                        setCmdSignal("heating and stage2", 1.0, node);
                        if(isFanStage2Enabled && (fanHighType == SmartStatFanRelayType.FAN_STAGE2)){
                            relayStages.put("FanStage2",1);
                            setCmdSignal("fan and stage2",1.0,node);
                        }
                        relayStages.put("HeatingStage2",1);
                    } else {
                        if (roomTemp >= setTempHeating) {//Turn off stage 2
                            if(getCmdSignal("heating and stage2", node) > 0)
                                setCmdSignal("heating and stage2", 0, node);
                            if (occupied && isFanStage2Enabled && (fanHighType == SmartStatFanRelayType.FAN_STAGE2) && (fanSpeed == StandaloneFanSpeed.FAN_HIGH)) {
                                relayStages.put("FanStage2", 1);
                                setCmdSignal("fan and stage2", 1.0, node);
                            } else if (isFanStage2Enabled && (fanHighType == SmartStatFanRelayType.FAN_STAGE2)){
                                setCmdSignal("fan and stage2", 0, node);
                            }
                        }else {
                            if(getCmdSignal("heating and stage2", node) > 0)relayStages.put("HeatingStage2",1);
                            if(getCmdSignal("fan and stage2", node) > 0)relayStages.put("FanStage2",1);
                        }
                    }
                }else{
                    if(occupied && isFanStage2Enabled && (fanHighType == SmartStatFanRelayType.FAN_STAGE2) && (fanSpeed == StandaloneFanSpeed.FAN_HIGH)){
                        relayStages.put("FanStage2",1);
                        setCmdSignal("fan and stage2",1.0,node);
                    }
                    else if(isFanStage2Enabled)setCmdSignal("fan and stage2",0,node);
                    setCmdSignal("heating and stage2", 0, node);
                }

                if(isFanStage2Enabled && ((fanHighType == SmartStatFanRelayType.HUMIDIFIER) || (fanHighType == SmartStatFanRelayType.DE_HUMIDIFIER)))
                    updateHumidityStatus(fanHighType,node,cpuDevice.getHumidity(),targetThreshold,relayStages);
                Log.d(TAG, " smartstat cpu,heating updates =" + node+","+roomTemp+","+occupied+","+setTempHeating+","+heatingDeadband+","+opMode.name()+","+fanSpeed.name());
            }
            else
            {
                if(occupied && (fanSpeed != OFF)) {
                    if(isFanStage1Enabled &&   ((fanSpeed == StandaloneFanSpeed.FAN_LOW) || (fanSpeed == StandaloneFanSpeed.FAN_HIGH))) {
                        relayStages.put("FanStage1", 1);
                        setCmdSignal("fan and stage1",1.0,node);
                    }else{
                        setCmdSignal("fan and stage1",0,node);
                    }
                    if(isFanStage2Enabled  && (fanHighType == SmartStatFanRelayType.FAN_STAGE2) &&  (fanSpeed == StandaloneFanSpeed.FAN_HIGH)) {
                        relayStages.put("FanStage2", 1);
                        setCmdSignal("fan and stage2",1.0,node);
                    }else if(fanHighType == SmartStatFanRelayType.FAN_STAGE2){
                        setCmdSignal("fan and stage2",0,node);
                    }else if(isFanStage2Enabled && ((fanHighType == SmartStatFanRelayType.HUMIDIFIER) || (fanHighType == SmartStatFanRelayType.DE_HUMIDIFIER)))
                        updateHumidityStatus(fanHighType,node,cpuDevice.getHumidity(),targetThreshold,relayStages);
                }else{
                    state = DEADBAND;
                    if(getCmdSignal("fan and stage1",node) > 0)
                        setCmdSignal("fan and stage1",0,node);
                    if (fanHighType == SmartStatFanRelayType.FAN_STAGE2) {
                        if (getCmdSignal("fan and stage2", node) > 0)
                            setCmdSignal("fan and stage2", 0, node);
                    }else if( (fanSpeed != OFF ) && isFanStage2Enabled && ((fanHighType == SmartStatFanRelayType.HUMIDIFIER) || (fanHighType == SmartStatFanRelayType.DE_HUMIDIFIER)))
                        updateHumidityStatus(fanHighType,node,cpuDevice.getHumidity(),targetThreshold,relayStages);
                }
                //Turn off all cooling and heating stages
                if(getCmdSignal("cooling and stage1",node) > 0)
                    setCmdSignal("cooling and stage1",0,node);
                if(getCmdSignal("cooling and stage2",node) > 0)
                    setCmdSignal("cooling and stage2",0,node);
                if(getCmdSignal("heating and stage1",node) > 0)
                    setCmdSignal("heating and stage1",0,node);
                if(getCmdSignal("heating and stage2",node) > 0)
                    setCmdSignal("heating and stage2",0,node);
            }


            cpuDevice.setProfilePoint("temp and conditioning and mode",state.ordinal());
            if(cpuDevice.getStatus() != state.ordinal())
                cpuDevice.setStatus(state.ordinal());
           ZoneTempState temperatureState = ZoneTempState.NONE;
            if(buildingLimitMinBreached() ||  buildingLimitMaxBreached() )
                temperatureState = ZoneTempState.EMERGENCY;

            StandaloneScheduler.updateSmartStatStatus(cpuEquip.getId(),state, relayStages, temperatureState);
        }
    }

    @Override
    public ZoneState getState() {
        return state;
    }

    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address) {

        return cpuDeviceMap.get(address) != null ? cpuDeviceMap.get(address).getProfileConfiguration() : null;
    }
    /**
     * Only creates a run time instance of logical map for initialize.
     * @param addr
     */
    public void addLogicalMap(short addr) {
        ConventionalUnitLogicalMap deviceMap = new ConventionalUnitLogicalMap(getProfileType(), addr);
        cpuDeviceMap.put(addr, deviceMap);
    }
    /**
     * Only creates a run time instance of logical map for initialize.
     * @param addr
     */
    public void addLogicalMap(short addr,String roomRef) {
        ConventionalUnitLogicalMap deviceMap = new ConventionalUnitLogicalMap(getProfileType(), addr);
        cpuDeviceMap.put(addr, deviceMap);
    }


    /**
     * When the profile is created first time , either via UI or from existing tagsMap
     * this method has to be called on the profile instance.
     * @param addr
     * @param config
     * @param floorRef
     * @param zoneRef
     */
    public void addLogicalMapAndPoints(short addr, ConventionalUnitConfiguration config, String floorRef, String zoneRef) {
        ConventionalUnitLogicalMap deviceMap = new ConventionalUnitLogicalMap(getProfileType(), addr);
        deviceMap.createHaystackPoints(config, floorRef, zoneRef );

        cpuDeviceMap.put(addr, deviceMap);
    }

    public void updateLogicalMapAndPoints(short addr, ConventionalUnitConfiguration config, String roomRef) {
        ConventionalUnitLogicalMap deviceMap = cpuDeviceMap.get(addr);
        deviceMap.updateHaystackPoints(config);
    }

    @JsonIgnore
    public Set<Short> getNodeAddresses()
    {
        return cpuDeviceMap.keySet();
    }
    @JsonIgnore
    @Override
    public Equip getEquip()
    {
        for (short nodeAddress : cpuDeviceMap.keySet())
        {
            HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+nodeAddress+"\"");
            return new Equip.Builder().setHashMap(equip).build();
        }
        return null;
    }

    @JsonIgnore
    @Override
    public double getCurrentTemp() {
        for (short nodeAddress : cpuDeviceMap.keySet())
        {
            return cpuDeviceMap.get(nodeAddress).getCurrentTemp();
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
    public double getAverageZoneTemp()
    {
        double tempTotal = 0;
        int nodeCount = 0;
        for (short nodeAddress : cpuDeviceMap.keySet())
        {
            if (cpuDeviceMap.get(nodeAddress) ==  null) {
                continue;
            }
            if (cpuDeviceMap.get(Short.valueOf(nodeAddress)).getCurrentTemp() > 0)
            {
                tempTotal += cpuDeviceMap.get(Short.valueOf(nodeAddress)).getCurrentTemp();
                nodeCount++;
            }
        }
        return nodeCount == 0 ? 0 : tempTotal/nodeCount;
    }



    public double getConfigEnabled(String config, short node) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and "+config+" and group == \"" + node + "\"");

    }
    public double getConfigType(String config, short node) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and type and " + config + " and group == \"" + node + "\"");

    }

    public double getCmdSignal(String cmd, short node) {
        return CCUHsApi.getInstance().readHisValByQuery("point and standalone and cpu and cmd and his and "+cmd+" and group == \"" + node + "\"");
    }
    public void setCmdSignal(String cmd, double val, short node) {
        CCUHsApi.getInstance().writeHisValByQuery("point and standalone and cpu and cmd and his and "+cmd+" and group == \"" + node + "\"", val);
    }

    public double getOperationalModes(String cmd, String equipRef){
        return CCUHsApi.getInstance().readHisValByQuery("point and standalone and operation and mode and his and "+cmd+" and equipRef == \"" + equipRef + "\"");
    }

    private void resetRelays(String equipRef, short node){

        if(getCmdSignal("cooling and stage1", node) > 0)
            setCmdSignal("cooling and stage1",0,node);
        if(getCmdSignal("cooling and stage2", node) > 0)
            setCmdSignal("cooling and stage2",0,node);
        if(getCmdSignal("heating and stage1", node) > 0)
            setCmdSignal("heating and stage1",0,node);
        if(getCmdSignal("heating and stage2", node) > 0)
            setCmdSignal("heating and stage2",0,node);
        if(getCmdSignal("fan and stage1", node) > 0)
            setCmdSignal("fan and stage1",0,node);
        if(getCmdSignal("fan and stage2", node) > 0)
            setCmdSignal("fan and stage2",0,node);
        StandaloneScheduler.updateSmartStatStatus(equipRef,DEADBAND, new HashMap<String, Integer>(),ZoneTempState.TEMP_DEAD);
    }

    public void updateHumidityStatus(SmartStatFanRelayType fanHighType, Short addr, double curValue, double targetThreshold, HashMap<String,Integer> relayStages){
        switch (fanHighType){

            case HUMIDIFIER:
                if((curValue > 0) && occupied) {
                    if (curValue < targetThreshold) {
                        relayStages.put("Humidifier", 1);
                        setCmdSignal("fan and stage2", 1.0, addr);
                    } else if (getCmdSignal("fan and stage2", addr) > 0) {
                        if (curValue > (targetThreshold + 5.0))
                            setCmdSignal("fan and stage2", 0, addr);
                        else
                            relayStages.put("Humdifier", 1);
                    }
                }else
                    setCmdSignal("fan and stage2", 0, addr);

                break;
            case DE_HUMIDIFIER:
                if((curValue > 0) && occupied) {
                    if (curValue > targetThreshold) {
                        setCmdSignal("fan and stage2", 1.0, addr);
                        relayStages.put("Dehumidifier", 1);
                    } else if (getCmdSignal("fan and stage2", addr) > 0) {
                        if (curValue < (targetThreshold - 5.0))
                            setCmdSignal("fan and stage2", 0, addr);
                        else
                            relayStages.put("Dehumidifier", 1);
                    }
                }else
                    setCmdSignal("fan and stage2", 0, addr);
                break;
        }
    }

}
