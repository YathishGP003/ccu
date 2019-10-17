package a75f.io.logic.bo.building.vav;

import android.util.Log;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75.io.algos.VOCLoop;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.hvac.Valve;
import a75f.io.logic.bo.building.hvac.VavUnit;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

/**
 * Created by samjithsadasivan on 8/23/18.
 */

public class VavReheatProfile extends VavProfile
{
  
    private boolean satCompensationEnabled = false;
    
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.VAV_REHEAT;
    }
    
    //VAV damper and reheat coil control logic is implemented according to section 1.3.E.6 of
    //ASHRAE RP-1455: Advanced Control Sequences for HVAC Systems Phase I, Air Distribution and Terminal Systems
    @Override
    public void updateZonePoints() {
        
        if(mInterface != null)
        {
            mInterface.refreshView();
        }
        
        for (short node : vavDeviceMap.keySet())
        {
            if (vavDeviceMap.get(node) == null) {
                addLogicalMap(node);
                CcuLog.d(L.TAG_CCU_ZONE, " Logical Map added for node " + node);
                continue;
            }
            if (isZoneDead()) {
                CcuLog.d(L.TAG_CCU_ZONE,"Zone Temp Dead "+node+" roomTemp : "+vavDeviceMap.get(node).getCurrentTemp());
                state = TEMPDEAD;
                String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \""+node+"\"");
                if (!curStatus.equals("Zone Temp Dead"))
                {
                    CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"", "Zone Temp Dead");
                    VAVLogicalMap vavDevice = vavDeviceMap.get(node);
                    double damperMin = vavDevice.getDamperLimit(state == HEATING ? "heating":"cooling", "min");
                    double damperMax = vavDevice.getDamperLimit(state == HEATING ? "heating":"cooling", "max");
                    double damperPos = (damperMax+damperMin)/2;
                    vavDevice.setDamperPos(damperPos);
                    vavDevice.setNormalizedDamperPos(damperPos);
                    vavDevice.setReheatPos(0);
                    CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + node + "\"", (double) TEMPDEAD.ordinal());
                }
                continue;
            }
            
            VAVLogicalMap vavDevice = vavDeviceMap.get(node);
            ControlLoop coolingLoop = vavDevice.getCoolingLoop();
            ControlLoop heatingLoop = vavDevice.getHeatingLoop();
            CO2Loop co2Loop = vavDeviceMap.get(node).getCo2Loop();
            VOCLoop vocLoop = vavDeviceMap.get(node).getVOCLoop();
            VavUnit vavUnit = vavDevice.getVavUnit();
            GenericPIController valveController = vavDevice.getValveController();
    
            double roomTemp = vavDevice.getCurrentTemp();
            double dischargeTemp = vavDevice.getDischargeTemp();
            double supplyAirTemp = vavDevice.getSupplyAirTemp();
            double co2 = vavDeviceMap.get(node).getCO2();
            double voc = vavDeviceMap.get(node).getVOC();
            double dischargeSp = vavDevice.getDischargeSp();
            setTempCooling = vavDevice.getDesiredTempCooling();
            setTempHeating = vavDevice.getDesiredTempHeating();
            
            double averageDesiredTemp = (setTempCooling+setTempHeating)/2;
            if (averageDesiredTemp != vavDevice.getDesiredTemp())
            {
                vavDevice.setDesiredTemp(averageDesiredTemp);
            }
            
            Damper damper = vavUnit.vavDamper;
            Valve valve = vavUnit.reheatValve;
            setDamperLimits(node, damper);
    
            Equip vavEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
            
            int loopOp = 0;
            //If supply air temperature from air handler is greater than room temperature, Cooling shall be
            //locked out.
            SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();
            if (roomTemp > setTempCooling)
            {
                //Zone is in Cooling
                if (state != COOLING)
                {
                    state = COOLING;
                    //valveController.reset();
                    valve.currentPosition = 0;
                    coolingLoop.setEnabled();
                    heatingLoop.setDisabled();
                }
                if (conditioning == SystemController.State.COOLING)
                {
                    loopOp = (int) coolingLoop.getLoopOutput(roomTemp, setTempCooling);
                }
                
            }
            else if (roomTemp < setTempHeating)
            {
                //Zone is in heating
                if (state != HEATING)
                {
                    state = HEATING;
                    heatingLoop.setEnabled();
                    coolingLoop.setDisabled();
                }
                int heatingLoopOp = (int) heatingLoop.getLoopOutput(setTempHeating, roomTemp);
                if (conditioning == SystemController.State.COOLING)
                {
                    if (heatingLoopOp <= 50)
                    {
                        //Control reheat valve when heating loop is <=50
                        double datMax = (roomTemp + HEATING_LOOP_OFFSET) > MAX_DISCHARGE_TEMP ? MAX_DISCHARGE_TEMP : (roomTemp + HEATING_LOOP_OFFSET);
                        dischargeSp = supplyAirTemp + (datMax - supplyAirTemp) * heatingLoopOp / 50;
                        vavDevice.setDischargeSp(dischargeSp);
                        valveController.updateControlVariable(dischargeSp, dischargeTemp);
                        valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                        loopOp = 0;
                        CcuLog.d(L.TAG_CCU_ZONE, "dischargeSp: " + dischargeSp);
                    }
                    else
                    {
                        //Control airflow when heating loop is 51-100
                        //Also update valve control to account for change in dischargeTemp
                        if (dischargeSp == 0)
                        {
                            double datMax = (roomTemp + HEATING_LOOP_OFFSET) > MAX_DISCHARGE_TEMP ? MAX_DISCHARGE_TEMP : (roomTemp + HEATING_LOOP_OFFSET);
                            dischargeSp = supplyAirTemp + (datMax - supplyAirTemp) * heatingLoopOp / 100;
                            vavDevice.setDischargeSp(dischargeSp);
                        }
                        
                        if (dischargeSp > dischargeTemp)
                        {
                            valveController.updateControlVariable(dischargeSp, dischargeTemp);
                            valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                            CcuLog.d(L.TAG_CCU_ZONE, "dischargeSp: "+dischargeSp+" dischargeTemp "+dischargeTemp);
                        } else {
                            CcuLog.d(L.TAG_CCU_ZONE,"Invalid air temp :  supplyAirTemp: "+supplyAirTemp+" dischargeTemp: "+dischargeTemp+" dischargeSp: "+dischargeSp);
                            valve.currentPosition = 0;
    
                        }
                        loopOp = heatingLoopOp;
                        
                        }
                } else if (conditioning == SystemController.State.HEATING)
                {
                    loopOp = heatingLoopOp;
                }
            }
            else
            {
                //Zone is in deadband
                if (state != DEADBAND) {
                    state = DEADBAND;
                    //valveController.reset();
                    valve.currentPosition = 0;
                    heatingLoop.setDisabled();
                    coolingLoop.setDisabled();
                }
            }
            
            if (conditioning == SystemController.State.OFF || valveController.getControlVariable() == 0)
            {
                valve.currentPosition = 0;
            }
            
            boolean  enabledCO2Control = vavDevice.getConfigNumVal("enable and co2") > 0 ;
            boolean  enabledIAQControl = vavDevice.getConfigNumVal("enable and iaq") > 0 ;
            String zoneId = HSUtil.getZoneIdFromEquipId(vavEquip.getId());
            Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
            boolean occupied = (occ == null ? false : occ.isOccupied()) || (ScheduleProcessJob.getSystemOccupancy() == Occupancy.PRECONDITIONING);
            Log.d(L.TAG_CCU_ZONE, "Zone occupaancy : "+occupied+" occ "+occ);
            //CO2 loop output from 0-50% modulates damper min position.
            if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) > 0)
            {
                damper.iaqCompensatedMinPos = damper.minPosition + (damper.maxPosition - damper.minPosition) * Math.min(50, co2Loop.getLoopOutput()) / 50;
                CcuLog.d(L.TAG_CCU_ZONE,"CO2LoopOp :"+co2Loop.getLoopOutput()+", adjusted minposition "+damper.iaqCompensatedMinPos);
            }
    
            //VOC loop output from 0-50% modulates damper min position.
            if (enabledIAQControl && occupied && vocLoop.getLoopOutput(voc) > 0)
            {
                damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * Math.min(50, vocLoop.getLoopOutput()) / 50;
                CcuLog.d(L.TAG_CCU_ZONE,"VOCLoopOp :"+vocLoop.getLoopOutput()+", adjusted minposition "+damper.iaqCompensatedMinPos);
            }
            
            if (loopOp == 0)
            {
                damper.currentPosition = damper.iaqCompensatedMinPos;
            }
            else
            {
                damper.currentPosition = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * loopOp / 100;
            }
            //In any Mode except Unoccupied, the hot water valve shall be
            //modulated to maintain a supply air temperature no lower than 50°F.
            /*if (state != HEATING && supplyAirTemp < REHEAT_THRESHOLD_TEMP*//* && mode != UNOCCUPIED*//*)
            {
                satCompensationEnabled = true;
                valveController.updateControlVariable(REHEAT_THRESHOLD_TEMP, supplyAirTemp);
                valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                Log.d(TAG, "SAT below threshold "+supplyAirTemp+" => valve :  " + valve.currentPosition);
            } else if (satCompensationEnabled) {
                satCompensationEnabled = false;
                valveController.reset();
            }*/
    
            //REHEAT control during heating does not follow RP1455.
            if (conditioning == SystemController.State.HEATING && state == HEATING)
            {
                double valveStartDamperPercent = TunerUtil.readTunerValByQuery("vav and valve and start and damper and equipRef == \""+vavEquip.getId()+"\"");
                double maxHeatingPos = vavDevice.getDamperLimit("heating", "max");
                double minHeatingPos = vavDevice.getDamperLimit("heating", "min");
                double valveStart = minHeatingPos + (maxHeatingPos - minHeatingPos) * valveStartDamperPercent / 100;
                CcuLog.d(L.TAG_CCU_ZONE," valveStartDamperPercent "+valveStartDamperPercent+" valveStart "+valveStart );
                if (damper.currentPosition > valveStart)
                {
                    valve.currentPosition = (int) ((damper.currentPosition - valveStart) * 100 / (maxHeatingPos - valveStart));
                }
                else
                {
                    valve.currentPosition = 0;
                }
            }
            
            CcuLog.d(L.TAG_CCU_ZONE,"CoolingLoop "+node +" roomTemp :"+roomTemp+" setTempCooling: "+setTempCooling);
            coolingLoop.dump();
            CcuLog.d(L.TAG_CCU_ZONE,"HeatingLoop "+node +" roomTemp :"+roomTemp+" setTempHeating: "+setTempHeating);
            heatingLoop.dump();
            
            CcuLog.d(L.TAG_CCU_ZONE, "System Conditioning :"+conditioning+" ZoneState : "+getState()+" ,loopOp: " + loopOp + " ,damper:" + damper.currentPosition+", valve:"+valve.currentPosition);
            updateTRResponse(node);
    
            valve.applyLimits();
            damper.applyLimits();
    
            vavDevice.setDamperPos(damper.currentPosition);
            vavDevice.setReheatPos(valve.currentPosition);
            CcuLog.d(L.TAG_CCU_ZONE, "buildingLimitMaxBreached "+buildingLimitMaxBreached()+" buildingLimitMinBreached "+buildingLimitMinBreached());
            
            vavDevice.setStatus(state.ordinal(), VavSystemController.getInstance().isEmergencyMode() && (state == HEATING ? buildingLimitMinBreached()
                                                         : state == COOLING ? buildingLimitMaxBreached() : false));
            vavDevice.updateLoopParams();
        }
    }
    @Override
    public ZoneState getState() {
        return state;
    }
}
