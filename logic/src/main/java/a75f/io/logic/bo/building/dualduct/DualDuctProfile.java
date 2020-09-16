package a75f.io.logic.bo.building.dualduct;

import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75.io.algos.CO2Loop;
import a75.io.algos.GenericPIController;
import a75.io.algos.VOCLoop;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.bo.util.TemperatureProfileUtil;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

public class DualDuctProfile extends ZoneProfile {
    
    DualDuctEquip dualDuctEquip;
    
    public void addDualDuctEquip(short addr, DualDuctProfileConfiguration config, String floorRef, String roomRef) {
        dualDuctEquip = new DualDuctEquip(getProfileType(), addr);
        dualDuctEquip.createEntities(config, floorRef, roomRef);
        dualDuctEquip.init();
    }
    
    public void addDabEquip(short addr) {
        dualDuctEquip = new DualDuctEquip(getProfileType(), addr);
        dualDuctEquip.init();
    }
    
    public void updateDualDuctEquip(DualDuctProfileConfiguration config) {
        dualDuctEquip.updateEquip(config);
    }
    
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.DAB;
    }
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return dualDuctEquip.getProfileConfiguration();
    }
    
    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short)dualDuctEquip.nodeAddr);
        }};
    }
    
    @Override
    public Equip getEquip()
    {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \"" + dualDuctEquip.nodeAddr + "\"");
        return new Equip.Builder().setHashMap(equip).build();
    }
    
    @Override
    public boolean isZoneDead() {
        
        double buildingLimitMax =  TunerUtil.readBuildingTunerValByQuery("building and limit and max");
        double buildingLimitMin =  TunerUtil.readBuildingTunerValByQuery("building and limit and min");
        
        double tempDeadLeeway = TunerUtil.readBuildingTunerValByQuery("temp and dead and leeway");
        
        if (dualDuctEquip.getCurrentTemp() > (buildingLimitMax + tempDeadLeeway)
            || dualDuctEquip.getCurrentTemp() < (buildingLimitMin - tempDeadLeeway))
        {
            return true;
        }
        
        return false;
    }
    
    @Override
    public void updateZonePoints()
    {
        if (isZoneDead()) {
            updateZoneDead();
            return;
        }
        
        double setTempCooling = TemperatureProfileUtil.getDesiredTempCooling(dualDuctEquip.nodeAddr);
        double setTempHeating = TemperatureProfileUtil.getDesiredTempHeating(dualDuctEquip.nodeAddr);
        double roomTemp       = dualDuctEquip.getCurrentTemp();
        
        GenericPIController heatingDamperController = dualDuctEquip.heatingDamperController;
        GenericPIController coolingDamperController = dualDuctEquip.coolingDamperController;
        
        Damper coolingDamper = new Damper();
        Damper heatingDamper = new Damper();
        SystemMode             systemMode   = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];
        
        if (roomTemp > setTempCooling && systemMode != SystemMode.OFF) {
            //Zone is in Cooling
            if (state != COOLING)
            {
                state = COOLING;
                heatingDamperController.reset();
                coolingDamperController.reset();
            }
            coolingDamperController.updateControlVariable(roomTemp, setTempCooling);
        } else if (roomTemp < setTempHeating && systemMode != SystemMode.OFF) {
            //Zone is in heating
            if (state != HEATING)
            {
                state = HEATING;
                heatingDamperController.reset();
                coolingDamperController.reset();
            }
            heatingDamperController.updateControlVariable(setTempHeating, roomTemp);
        } else {
            if (state != DEADBAND) {
                state = DEADBAND;
            }
            heatingDamperController.reset();
            coolingDamperController.reset();
        }
        heatingDamperController.dump();
        coolingDamperController.dump();
        
        setDamperLimits(coolingDamper, "cooling");
        setDamperLimits(heatingDamper, "heating");
        
        coolingDamper.iaqCompensatedMinPos = getIAQCompensatedDamperPosition(coolingDamper);
        heatingDamper.iaqCompensatedMinPos = getIAQCompensatedDamperPosition(coolingDamper);
        
        int coolingDamperPos = (int)(coolingDamper.iaqCompensatedMinPos +
                                (coolingDamper.maxPosition - coolingDamper.iaqCompensatedMinPos) *
                                (coolingDamperController.getControlVariable() / coolingDamperController.getMaxAllowedError()));
    
        int heatingDamperPos = (int)(heatingDamper.iaqCompensatedMinPos +
                                     (heatingDamper.maxPosition - heatingDamper.iaqCompensatedMinPos) *
                                     (heatingDamperController.getControlVariable() / heatingDamperController.getMaxAllowedError()));
        
        if(coolingDamper.currentPosition != coolingDamperPos) {
            coolingDamper.currentPosition = coolingDamperPos;
            dualDuctEquip.setDamperPos(coolingDamper.currentPosition, "cooling");
        }
    
        if(heatingDamper.currentPosition != heatingDamperPos) {
            coolingDamper.currentPosition = heatingDamperPos;
            dualDuctEquip.setDamperPos(heatingDamper.currentPosition, "heating");
        }
        
        updateZoneStatus();
    
        Log.d(L.TAG_CCU_ZONE, "DAB : roomTemp" + roomTemp + " setTempCooling:  " + setTempCooling+" " +
                              "setTempHeating: "+setTempHeating+" ZoneState: "+state);
    
        CcuLog.d(L.TAG_CCU_ZONE, "CoolingDamper : CV" + coolingDamperController.getControlVariable() + " " +
                                 "currentPosition " +coolingDamper.currentPosition);
    
        CcuLog.d(L.TAG_CCU_ZONE, "HearingDamper : CV" + heatingDamperController.getControlVariable() + " " +
                                 "currentPosition " +heatingDamper.currentPosition);
    }
    
    private void updateZoneDead() {
        CcuLog.d(L.TAG_CCU_ZONE, "Zone Temp Dead: " + dualDuctEquip.nodeAddr + " roomTemp : " + dualDuctEquip.getCurrentTemp());
        state = TEMPDEAD;
        String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \""+dualDuctEquip.nodeAddr+"\"");
        if (!curStatus.equals("Zone Temp Dead"))
        {
            CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + dualDuctEquip.nodeAddr + "\"", "Zone Temp Dead");
            double damperMin = TemperatureProfileUtil.getDamperLimit(dualDuctEquip.nodeAddr, "heating", "min");
            double damperMax = TemperatureProfileUtil.getDamperLimit(dualDuctEquip.nodeAddr, "heating", "max");
        
            dualDuctEquip.setDamperPos((damperMin + damperMax)/2 , "heating");
        
            damperMin = TemperatureProfileUtil.getDamperLimit(dualDuctEquip.nodeAddr, "cooling", "min");
            damperMax = TemperatureProfileUtil.getDamperLimit(dualDuctEquip.nodeAddr, "cooling", "max");
        
            dualDuctEquip.setDamperPos((damperMin + damperMax)/2 , "cooling");
            CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + dualDuctEquip.nodeAddr + "\"", (double) TEMPDEAD.ordinal());
        }
    }
    
    private int getIAQCompensatedDamperPosition(Damper damper) {
    
        CO2Loop co2Loop = dualDuctEquip.getCo2Loop();
        VOCLoop vocLoop = dualDuctEquip.getVOCLoop();
    
        double co2 = dualDuctEquip.getCO2();
        double voc = dualDuctEquip.getVOC();
        boolean  enabledCO2Control = dualDuctEquip.getConfigNumVal("enable and co2") > 0 ;
        boolean  enabledIAQControl = dualDuctEquip.getConfigNumVal("enable and iaq") > 0 ;
        String   zoneId            = HSUtil.getZoneIdFromEquipId(dualDuctEquip.getId());
        Occupied occ               = ScheduleProcessJob.getOccupiedModeCache(zoneId);
        boolean  occupied          = (occ == null ? false : occ.isOccupied());
    
        double        epidemicMode  = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+L.ccu().systemProfile.getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
    
        if((epidemicState != EpidemicState.OFF) && (L.ccu().oaoProfile != null)){
            double smartPurgeDABDamperMinOpenMultiplier = TunerUtil.readTunerValByQuery("purge and system and dab and damper and pos and multiplier and min ", L.ccu().oaoProfile.getEquipRef());
            damper.iaqCompensatedMinPos =(int) (damper.minPosition * smartPurgeDABDamperMinOpenMultiplier);
        } else {
            damper.iaqCompensatedMinPos = damper.minPosition;
        }
        //CO2 loop output from 0-50% modulates damper min position.
        if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) > 0)
        {
            damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * Math.min(50, co2Loop.getLoopOutput()) / 50;
            CcuLog.d(L.TAG_CCU_ZONE, "CO2LoopOp :" + co2Loop.getLoopOutput() + ", adjusted minposition " + damper.iaqCompensatedMinPos);
        }
    
        //VOC loop output from 0-50% modulates damper min position.
        if (enabledIAQControl && occupied && vocLoop.getLoopOutput(voc) > 0)
        {
            damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * Math.min(50, vocLoop.getLoopOutput()) / 50;
            CcuLog.d(L.TAG_CCU_ZONE,"VOCLoopOp :"+vocLoop.getLoopOutput()+", adjusted minposition "+damper.iaqCompensatedMinPos+" damper.minPosition "+damper.minPosition);
        }
        
        return damper.iaqCompensatedMinPos;
    
    }
    
    private void updateCompositeDamper(ZoneState state, Damper coolingDamper, Damper heatingDamper) {
        if (state == COOLING) {
        
        
        }
    }
    
    private void updateZoneStatus() {
        
        if(TemperatureProfileUtil.getStatus(dualDuctEquip.nodeAddr) != state.ordinal()) {
            TemperatureProfileUtil.setStatus(dualDuctEquip.nodeAddr, state.ordinal(),
                                             DabSystemController.getInstance().isEmergencyMode() &&
                                             (state == HEATING ? buildingLimitMinBreached() : state == COOLING ?
                                                                                                  buildingLimitMaxBreached() : false));
        }
    }
    
    protected void setDamperLimits(Damper d, String heatCool) {
        d.minPosition = (int)TemperatureProfileUtil.getDamperLimit(dualDuctEquip.nodeAddr, heatCool, "min");
        d.maxPosition = (int)TemperatureProfileUtil.getDamperLimit(dualDuctEquip.nodeAddr, heatCool, "max");
        d.iaqCompensatedMinPos = d.minPosition;
    }
    
    @Override
    public void reset(){
       
        dualDuctEquip.setDamperPos(TemperatureProfileUtil.getDamperLimit(dualDuctEquip.nodeAddr, "heating", "min")
                                                                , "heating");
        dualDuctEquip.setDamperPos(TemperatureProfileUtil.getDamperLimit(dualDuctEquip.nodeAddr, "cooling", "min")
                                                                 , "cooling");
       
        dualDuctEquip.setCurrentTemp(0);
    }
}
