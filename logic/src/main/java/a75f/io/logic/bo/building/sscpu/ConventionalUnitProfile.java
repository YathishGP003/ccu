package a75f.io.logic.bo.building.sscpu;

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
import static a75f.io.logic.bo.building.definitions.StandaloneLogicalFanSpeeds.AUTO;
import static a75f.io.logic.bo.building.definitions.StandaloneLogicalFanSpeeds.OFF;

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
    
        double buildingLimitMax =  TunerUtil.readBuildingTunerValByQuery("building and limit and max");
        double buildingLimitMin =  TunerUtil.readBuildingTunerValByQuery("building and limit and min");
    
        double tempDeadLeeway = TunerUtil.readBuildingTunerValByQuery("temp and dead and leeway");
    
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
            ZoneState curState = DEADBAND;
            ConventionalUnitLogicalMap cpuDevice = cpuDeviceMap.get(node);
            if(cpuDevice.profileType != ProfileType.SMARTSTAT_CONVENTIONAL_PACK_UNIT)
                continue;
            double roomTemp = cpuDevice.getCurrentTemp();
            Equip cpuEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
            if(isZoneDead()){
                resetRelays(cpuEquip.getId(),node);
                if(cpuDevice.getStatus() != state.ordinal())
                    cpuDevice.setStatus(state.ordinal());
                String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \"" + node + "\"");
                if (!curStatus.equals("Zone Temp Dead")) {
                    CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"", "Zone Temp Dead");
                }
				Log.d(TAG,"Invalid Temp , skip controls update for "+node+" roomTemp : "+cpuDeviceMap.get(node).getCurrentTemp());
                CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + node + "\"", (double) TEMPDEAD.ordinal());
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

            double ssOperatingMode = getOperationalModes("temp and conditioning",cpuEquip.getId());
            double ssFanOpMode = getOperationalModes("fan and operation",cpuEquip.getId());
            int fanStage2Type = (int)getConfigType("relay6",node);
            boolean enableFanStage1DuringOccupied = getConfigEnabled("fan and stage1",node) > 0 ? true : false;
            StandaloneOperationalMode opMode = StandaloneOperationalMode.values()[(int)ssOperatingMode];
            StandaloneLogicalFanSpeeds fanSpeed = StandaloneLogicalFanSpeeds.values()[(int)ssFanOpMode];
            SmartStatFanRelayType fanHighType = SmartStatFanRelayType.values()[(int)fanStage2Type];
            int fanModeSaved = Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).getInt(cpuEquip.getId(),0);
            Log.d("FANMODE","CPU profile - fanmodesaved="+fanModeSaved+","+fanSpeed.name()+","+occupied);
            if(!occupied &&(fanSpeed != OFF ) && (fanSpeed != StandaloneLogicalFanSpeeds.FAN_LOW_ALL_TIMES) && (fanSpeed != StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES)&& (fanSpeed != StandaloneLogicalFanSpeeds.FAN_HIGH2_ALL_TIMES)){
                //Reset to auto during unoccupied hours, if it is not all times set
                if((fanSpeed != StandaloneLogicalFanSpeeds.AUTO) && (fanSpeed != StandaloneLogicalFanSpeeds.FAN_LOW_ALL_TIMES) && (fanSpeed != StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES)) {
                    StandaloneScheduler.updateOperationalPoints(cpuEquip.getId(), "fan and operation and mode", StandaloneLogicalFanSpeeds.AUTO.ordinal());
                    fanSpeed = StandaloneLogicalFanSpeeds.AUTO;
                }
            }
            if(occupied && (fanSpeed == AUTO) && (fanModeSaved != 0)){
                //KUMAR need to reset back for FAN_LOW_OCCIPIED or FAN_MEDIUM_OCCUPIED or FAN_HIGH_OCCUPIED_PERIOD means next day schedule periods
                if(fanSpeed == StandaloneLogicalFanSpeeds.AUTO) {
                    StandaloneScheduler.updateOperationalPoints(cpuEquip.getId(), "fan and operation and mode", fanModeSaved);
                    fanSpeed = StandaloneLogicalFanSpeeds.values()[ fanModeSaved];
                }
            }

            if(occuStatus != null){
                cpuDevice.setProfilePoint("occupancy and mode", occuStatus.isOccupied() ? Occupancy.OCCUPIED.ordinal() : (occuStatus.isPreconditioning() ? Occupancy.PRECONDITIONING.ordinal() : (occuStatus.isForcedOccupied() ? Occupancy.FORCEDOCCUPIED.ordinal() : 0)));
            }else {
                cpuDevice.setProfilePoint("occupancy and mode", occupied ? 1 : 0);
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
            if ((fanSpeed != OFF) && (((opMode == StandaloneOperationalMode.AUTO) && (roomTemp >=  averageDesiredTemp)) || (opMode == StandaloneOperationalMode.COOL_ONLY)) )
            {

                //Zone is in Cooling
                curState = COOLING;
                double cs1 = getConfigEnabled("relay1",node);
                boolean isCoolingStage1Enabled = cs1 > 0 ? true : false;
                boolean isCoolingStage2Enabled = getConfigEnabled("relay2", node) > 0 ? true : false;
                if(getCmdSignal("heating and stage1", node) > 0)
                    setCmdSignal("heating and stage1",0,node);
                if(getCmdSignal("heating and stage2", node) > 0)
                    setCmdSignal("heating and stage2",0,node);
                if (isCoolingStage1Enabled) {
                    if(roomTemp >= setTempCooling) {
                        relayStages.put("CoolingStage1",1);
                        relayStages.put("FanStage1",1);
                        if(getCmdSignal("cooling and stage1",node) == 0)
                            setCmdSignal("cooling and stage1", 1.0, node);
                        if(isFanStage1Enabled || (occupied && enableFanStage1DuringOccupied))
                            if(getCmdSignal("fan and stage1",node) == 0)
                                setCmdSignal("fan and stage1",1.0,node);
                    }else{
                        if (roomTemp <= (setTempCooling-hysteresis)){//Turn off stage1
                            if(getCmdSignal("cooling and stage1", node) > 0)
                                setCmdSignal("cooling and stage1",0,node);

                            if((occupied && isFanStage1Enabled && (fanSpeed != StandaloneLogicalFanSpeeds.AUTO)) || (occupied && enableFanStage1DuringOccupied)){
                                relayStages.put("FanStage1",1);
                                if(getCmdSignal("fan and stage1",node) == 0) setCmdSignal("fan and stage1",1.0,node);
                            }else if(isFanStage1Enabled)
                                setCmdSignal("fan and stage1",0,node);
                        }else {
                            if(getCmdSignal("cooling and stage1", node) > 0)relayStages.put("CoolingStage1",1);
                            if(getCmdSignal("fan and stage1", node) > 0)relayStages.put("FanStage1",1);
                        }
                    }
                }else{

                    if((occupied && isFanStage1Enabled && (fanSpeed != StandaloneLogicalFanSpeeds.AUTO)) ||  (occupied && enableFanStage1DuringOccupied)){
                        relayStages.put("FanStage1",1);
                        if(getCmdSignal("fan and stage1",node) == 0)
                            setCmdSignal("fan and stage1",1.0,node);
                    }else if(isFanStage1Enabled) {
                        if(getCmdSignal("fan and stage1",node) >  0)
                            setCmdSignal("fan and stage1", 0, node);
                    }
                    if(getCmdSignal("cooling and stage1",node) > 0)
                        setCmdSignal("cooling and stage1", 0, node);
                }
                if(isCoolingStage2Enabled){
                    if (roomTemp >= (setTempCooling + coolingDeadband)) {
                        if(getCmdSignal("cooling and stage2",node) == 0)
                            setCmdSignal("cooling and stage2", 1.0, node);
                        relayStages.put("CoolingStage2",1);
                        if(((isFanStage2Enabled  && ((fanSpeed == AUTO) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_CURRENT_OCCUPIED) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_OCCUPIED)) ) ||  (occupied && enableFanStage1DuringOccupied))&& (fanHighType == SmartStatFanRelayType.FAN_STAGE2)){
                            relayStages.put("FanStage2",1);
                            if(getCmdSignal("fan and stage2",node) == 0)
                                setCmdSignal("fan and stage2",1.0,node);
                        }
                    } else{
                        if (roomTemp <= setTempCooling) {//Turn off stage 2
                            if(getCmdSignal("cooling and stage2", node) > 0)
                                setCmdSignal("cooling and stage2", 0, node);
                            if (occupied && isFanStage2Enabled && (fanHighType == SmartStatFanRelayType.FAN_STAGE2) && ((fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_CURRENT_OCCUPIED) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_OCCUPIED)   ) /*(fanSpeed == StandaloneFanSpeed.FAN_HIGH)*/) {
                                relayStages.put("FanStage2", 1);
                                if(getCmdSignal("fan and stage2", node) == 0) setCmdSignal("fan and stage2", 1.0, node);
                            } else if ((isFanStage2Enabled || enableFanStage1DuringOccupied)&& (fanHighType == SmartStatFanRelayType.FAN_STAGE2))
                                if(getCmdSignal("fan and stage2", node) > 0)setCmdSignal("fan and stage2", 0, node);
                        }else {
                            if(getCmdSignal("cooling and stage2", node) > 0)relayStages.put("CoolingStage2",1);
                            if(getCmdSignal("fan and stage2", node) > 0)relayStages.put("FanStage2",1);
                        }
                    }
                }else{
                    if(occupied && isFanStage2Enabled && (fanHighType == SmartStatFanRelayType.FAN_STAGE2)&& ((fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_CURRENT_OCCUPIED) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_OCCUPIED)   ) /*(fanSpeed == StandaloneFanSpeed.FAN_HIGH)*/){
                        relayStages.put("FanStage2",1);
                        if(getCmdSignal("fan and stage2", node) == 0)setCmdSignal("fan and stage2",1.0,node);
                    }
                    else if(isFanStage2Enabled && (fanHighType == SmartStatFanRelayType.FAN_STAGE2))
                        if(getCmdSignal("fan and stage2", node) > 0)
                            setCmdSignal("fan and stage2",0,node);
                    if(getCmdSignal("cooling and stage2", node) > 0)
                        setCmdSignal("cooling and stage2", 0, node);
                }
                if(isFanStage2Enabled && ((fanHighType == SmartStatFanRelayType.HUMIDIFIER) || (fanHighType == SmartStatFanRelayType.DE_HUMIDIFIER)))
                    updateHumidityStatus(fanHighType,node,cpuDevice.getHumidity(),targetThreshold,relayStages);
                Log.d(TAG, " smartstat cpu,cooling updates =" + node+","+roomTemp+","+occupied+","+isCoolingStage1Enabled+","+opMode.name()+","+fanSpeed.name()+","+cs1);
            }
            else if ((roomTemp > 0) && (fanSpeed != OFF) &&(((opMode == StandaloneOperationalMode.AUTO) &&(roomTemp < averageDesiredTemp))|| (opMode == StandaloneOperationalMode.HEAT_ONLY)))
            {
                //Zone is in heating

                curState = HEATING;
                if(getCmdSignal("cooling and stage1", node) > 0)
                    setCmdSignal("cooling and stage1",0,node);
                if(getCmdSignal("cooling and stage2", node) > 0)
                    setCmdSignal("cooling and stage2",0,node);
                boolean isHeatingStage1Enabled = getConfigEnabled("relay4", node) > 0;
                boolean isHeatingStage2Enabled = getConfigEnabled("relay5", node) > 0;
                if (isHeatingStage1Enabled)
                {
                    if(roomTemp <= setTempHeating ) {
                        if(getCmdSignal("heating and stage1", node) == 0)
                            setCmdSignal("heating and stage1", 1.0, node);
                        if(isFanStage1Enabled || (occupied && enableFanStage1DuringOccupied)){
                            if(getCmdSignal("fan and stage1", node) > 0)
                                setCmdSignal("fan and stage1",1.0,node);
                        }
                        relayStages.put("HeatingStage1",1);
                        relayStages.put("FanStage1",1);
                    }else {
                        if( roomTemp >= (setTempHeating + hysteresis)){
                            if(getCmdSignal("heating and stage1", node) > 0)
                                setCmdSignal("heating and stage1",0,node);

                            if((occupied && isFanStage1Enabled &&  (fanSpeed != AUTO)) || (occupied && enableFanStage1DuringOccupied)){
                                relayStages.put("FanStage1",1);
                                if(getCmdSignal("fan and stage1", node) == 0)
                                    setCmdSignal("fan and stage1",1.0,node);
                            }else if(isFanStage1Enabled){
                                if(getCmdSignal("fan and stage1", node) > 0)
                                    setCmdSignal("fan and stage1",0,node);
                            }
                        }else {
                            if(getCmdSignal("heating and stage1", node) > 0)relayStages.put("HeatingStage1",1);
                            if(getCmdSignal("fan and stage1", node) > 0)relayStages.put("FanStage1",1);
                        }
                    }
                }else{

                    if((occupied && isFanStage1Enabled && (fanSpeed != StandaloneLogicalFanSpeeds.AUTO)) || (occupied && enableFanStage1DuringOccupied)){
                        relayStages.put("FanStage1",1);
                        if(getCmdSignal("fan and stage1", node) == 0)
                            setCmdSignal("fan and stage1",1.0,node);
                    }else if(isFanStage1Enabled){
                        if(getCmdSignal("fan and stage1", node) > 0)
                            setCmdSignal("fan and stage1",0,node);
                    }
                    if(getCmdSignal("heating and stage1", node) > 0)
                        setCmdSignal("heating and stage1", 0, node);
                }
                if(isHeatingStage2Enabled){

                    if (roomTemp <= (setTempHeating - heatingDeadband)) {
                        if(getCmdSignal("heating and stage2", node) == 0)
                            setCmdSignal("heating and stage2", 1.0, node);
                        if(((isFanStage2Enabled  && ((fanSpeed == AUTO) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_CURRENT_OCCUPIED) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_OCCUPIED)  )) || (occupied && enableFanStage1DuringOccupied)) && (fanHighType == SmartStatFanRelayType.FAN_STAGE2)){
                            relayStages.put("FanStage2",1);
                            if(getCmdSignal("fan and stage2", node) == 0)
                                setCmdSignal("fan and stage2",1.0,node);
                        }
                        relayStages.put("HeatingStage2",1);
                    } else {
                        if (roomTemp >= setTempHeating) {//Turn off stage 2
                            if(getCmdSignal("heating and stage2", node) > 0)
                                setCmdSignal("heating and stage2", 0, node);
                            if (occupied && (isFanStage2Enabled || enableFanStage1DuringOccupied)&& (fanHighType == SmartStatFanRelayType.FAN_STAGE2) && ((fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_CURRENT_OCCUPIED) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_OCCUPIED)   )/*(fanSpeed == StandaloneFanSpeed.FAN_HIGH)*/) {
                                relayStages.put("FanStage2", 1);
                                if(getCmdSignal("fan and stage2", node) == 0)setCmdSignal("fan and stage2", 1.0, node);
                            } else if ((isFanStage2Enabled || enableFanStage1DuringOccupied) && (fanHighType == SmartStatFanRelayType.FAN_STAGE2)){
                                if(getCmdSignal("fan and stage2", node) > 0)
                                    setCmdSignal("fan and stage2", 0, node);
                            }
                        }else {
                            if(getCmdSignal("heating and stage2", node) > 0)relayStages.put("HeatingStage2",1);
                            if(getCmdSignal("fan and stage2", node) > 0)relayStages.put("FanStage2",1);
                        }
                    }
                }else{
                    if(occupied && (isFanStage2Enabled || enableFanStage1DuringOccupied) && (fanHighType == SmartStatFanRelayType.FAN_STAGE2) && ((fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_CURRENT_OCCUPIED) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_OCCUPIED)   )/*(fanSpeed == StandaloneFanSpeed.FAN_HIGH)*/){
                        relayStages.put("FanStage2",1);
                        if(getCmdSignal("fan and stage2", node) == 0)setCmdSignal("fan and stage2",1.0,node);
                    }
                    else if(isFanStage2Enabled){
                        if(getCmdSignal("fan and stage2", node) > 0)
                            setCmdSignal("fan and stage2",0,node);
                    }
                    if(getCmdSignal("heating and stage2", node) > 0)
                        setCmdSignal("heating and stage2", 0, node);
                }

                if(isFanStage2Enabled && ((fanHighType == SmartStatFanRelayType.HUMIDIFIER) || (fanHighType == SmartStatFanRelayType.DE_HUMIDIFIER)))
                    updateHumidityStatus(fanHighType,node,cpuDevice.getHumidity(),targetThreshold,relayStages);
                Log.d(TAG, " smartstat cpu,heating updates =" + node+","+roomTemp+","+occupied+","+setTempHeating+","+heatingDeadband+","+opMode.name()+","+fanSpeed.name());
            }
            else
            {
                if(occupied && (fanSpeed != OFF)) {
                    curState = DEADBAND;
                    if(enableFanStage1DuringOccupied || (isFanStage1Enabled &&  (fanSpeed != AUTO)) /*((fanSpeed == StandaloneFanSpeed.FAN_LOW) || (fanSpeed == StandaloneFanSpeed.FAN_HIGH))*/) {
                        relayStages.put("FanStage1", 1);
                        if(getCmdSignal("fan and stage1", node) == 0)
                            setCmdSignal("fan and stage1",1.0,node);
                    }else{
                        if(getCmdSignal("fan and stage1", node) > 0)
                            setCmdSignal("fan and stage1",0,node);
                    }
                    if(isFanStage2Enabled  && (fanHighType == SmartStatFanRelayType.FAN_STAGE2) && ((fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_CURRENT_OCCUPIED) || (fanSpeed == StandaloneLogicalFanSpeeds.FAN_HIGH_OCCUPIED)   ) /*(fanSpeed == StandaloneFanSpeed.FAN_HIGH)*/) {
                        relayStages.put("FanStage2", 1);
                        if(getCmdSignal("fan and stage2", node) == 0)
                            setCmdSignal("fan and stage2",1.0,node);
                    }else if(fanHighType == SmartStatFanRelayType.FAN_STAGE2){
                        if(getCmdSignal("fan and stage2", node) > 0)
                            setCmdSignal("fan and stage2",0,node);
                    }else if(isFanStage2Enabled && ((fanHighType == SmartStatFanRelayType.HUMIDIFIER) || (fanHighType == SmartStatFanRelayType.DE_HUMIDIFIER)))
                        updateHumidityStatus(fanHighType,node,cpuDevice.getHumidity(),targetThreshold,relayStages);
                }else{
                    curState = DEADBAND;
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


            cpuDevice.setProfilePoint("temp and operating and mode",curState.ordinal());
            if(cpuDevice.getStatus() != curState.ordinal())
                cpuDevice.setStatus(curState.ordinal());
           ZoneTempState temperatureState = ZoneTempState.NONE;
            if(buildingLimitMinBreached() ||  buildingLimitMaxBreached() )
                temperatureState = ZoneTempState.EMERGENCY;

            StandaloneScheduler.updateSmartStatStatus(cpuEquip.getId(),curState, relayStages, temperatureState);
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
        return CCUHsApi.getInstance().readHisValByQuery("point and standalone and mode and his and "+cmd+" and equipRef == \"" + equipRef + "\"");
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
                if((curValue > 0) && occupied) {
                    if (curValue > targetThreshold) {
                        setCmdSignal("dehumidifier", 1.0, addr);
                        relayStages.put("Dehumidifier", 1);
                    } else if (getCmdSignal("dehumidifier", addr) > 0) {
                        if (curValue < (targetThreshold - 5.0))
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
        for (short node : cpuDeviceMap.keySet())
        {
            cpuDeviceMap.get(node).setCurrentTemp(0);

        }
    }
}
