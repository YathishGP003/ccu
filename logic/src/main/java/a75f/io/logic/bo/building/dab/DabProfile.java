package a75f.io.logic.bo.building.dab;

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
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

/**
 * Created by samjithsadasivan on 3/13/19.
 */

public class DabProfile extends ZoneProfile
{
    DabEquip dabEquip;
    
    CO2Loop co2Loop;
    VOCLoop vocLoop;
    
    double co2;
    double voc;
    
    Damper damper = new Damper();
    
    public void addDabEquip(short addr, DabProfileConfiguration config, String floorRef, String roomRef) {
        dabEquip = new DabEquip(getProfileType(), addr);
        dabEquip.createEntities(config, floorRef, roomRef);
        dabEquip.init();
    }
    
    public void addDabEquip(short addr) {
        dabEquip = new DabEquip(getProfileType(), addr);
        dabEquip.init();
    }
    
    public void updateDabEquip(DabProfileConfiguration config) {
        dabEquip.update(config);
    }
    
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.DAB;
    }
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return dabEquip.getProfileConfiguration();
    }
    
    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short)dabEquip.nodeAddr);
        }};
    }
    
    @Override
    public Equip getEquip()
    {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+dabEquip.nodeAddr+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }
    
    @Override
    public boolean isZoneDead() {
        
        double buildingLimitMax =  TunerUtil.readBuildingTunerValByQuery("building and limit and max");
        double buildingLimitMin =  TunerUtil.readBuildingTunerValByQuery("building and limit and min");
        
        double tempDeadLeeway = TunerUtil.readBuildingTunerValByQuery("temp and dead and leeway");
    
        if (dabEquip.getCurrentTemp() > (buildingLimitMax + tempDeadLeeway)
            || dabEquip.getCurrentTemp() < (buildingLimitMin - tempDeadLeeway))
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
        
        double setTempCooling = dabEquip.getDesiredTempCooling();
        double setTempHeating = dabEquip.getDesiredTempHeating();
        double roomTemp = dabEquip.getCurrentTemp();
        GenericPIController damperOpController = dabEquip.damperController;
    
        co2Loop = dabEquip.getCo2Loop();
        vocLoop = dabEquip.getVOCLoop();
        
        co2 = dabEquip.getCO2();
        voc = dabEquip.getVOC();
        
        Log.d(L.TAG_CCU_ZONE, "DAB : roomTemp" + roomTemp + " setTempCooling:  " + setTempCooling+" setTempHeating: "+setTempHeating);
    
        SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();
        SystemMode systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];
        if ((roomTemp > setTempCooling) && (conditioning == SystemController.State.COOLING) && (systemMode != SystemMode.OFF))
        {
            //Zone is in Cooling
            if (state != COOLING)
            {
                state = COOLING;
                damperOpController.reset();
            }
            damperOpController.updateControlVariable(roomTemp, setTempCooling);
        }
        else if ((roomTemp < setTempHeating) && (conditioning == SystemController.State.HEATING) && (systemMode != SystemMode.OFF))
        {
            //Zone is in heating
            if (state != HEATING)
            {
                state = HEATING;
                damperOpController.reset();
            }
            damperOpController.updateControlVariable(setTempHeating, roomTemp);
        } else {
            if (state != DEADBAND) {
                state = DEADBAND;
                damperOpController.reset();
            }
           
        }
        damperOpController.dump();
        setDamperLimits(damper);
    
        updateDamperIAQCompensation();
        
        damper.currentPosition =
            (int)(damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * (damperOpController.getControlVariable() / damperOpController.getMaxAllowedError()));
        
        double hisDamperPos = CCUHsApi.getInstance().readHisValByQuery("point and damper and base and cmd and primary"+
                                                                    " and group == \""+dabEquip.nodeAddr+"\"");
        if(damper.currentPosition != hisDamperPos) {
            dabEquip.setDamperPos(damper.currentPosition, "primary");
            dabEquip.setDamperPos(damper.currentPosition, "secondary");
        }
        
        dabEquip.setStatus(state.ordinal(), DabSystemController.getInstance().isEmergencyMode() && (state == HEATING ? buildingLimitMinBreached()
                                                    : state == COOLING ? buildingLimitMaxBreached() : false));
        CcuLog.d(L.TAG_CCU_ZONE, "System STATE :" + DabSystemController.getInstance().getSystemState() + " ZoneState : " + getState() + " ,CV: " + damperOpController.getControlVariable() + " ,damper:" + damper.currentPosition);
    
    }
    
    private void updateZoneDead() {
        CcuLog.d(L.TAG_CCU_ZONE,"Zone Temp Dead: "+dabEquip.nodeAddr+" roomTemp : "+dabEquip.getCurrentTemp());
        state = TEMPDEAD;
        String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \""+dabEquip.nodeAddr+"\"");
        if (!curStatus.equals("Zone Temp Dead"))
        {
            CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + dabEquip.nodeAddr + "\"", "Zone Temp Dead");
        
            SystemMode systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];
            double damperMin = dabEquip.getDamperLimit(state == HEATING ? "heating":"cooling", "min");
            double damperMax = dabEquip.getDamperLimit(state == HEATING ? "heating":"cooling", "max");
        
            double damperPos =(damperMax+damperMin)/2;
            if(systemMode == SystemMode.OFF) {
                damperPos = dabEquip.getDamperPos() > 0 ? dabEquip.getDamperPos() : damperMin;
            }
            dabEquip.setDamperPos(damperPos, "primary");
            dabEquip.setDamperPos(damperPos, "secondary");
            dabEquip.setNormalizedDamperPos(damperPos, "primary");
            dabEquip.setNormalizedDamperPos(damperPos, "secondary");
            CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + dabEquip.nodeAddr + "\"", (double) TEMPDEAD.ordinal());
        }
    }
    
    private void updateDamperIAQCompensation() {
        boolean  enabledCO2Control = dabEquip.getConfigNumVal("enable and co2") > 0 ;
        boolean  enabledIAQControl = dabEquip.getConfigNumVal("enable and iaq") > 0 ;
        String zoneId = HSUtil.getZoneIdFromEquipId(dabEquip.getId());
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
        boolean occupied = (occ == null ? false : occ.isOccupied());
    
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+L.ccu().systemProfile.getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        if((epidemicState != EpidemicState.OFF) && (L.ccu().oaoProfile != null)) {
            double smartPurgeDABDamperMinOpenMultiplier = TunerUtil.readTunerValByQuery("purge and system and dab and damper and pos and multiplier and min ", L.ccu().oaoProfile.getEquipRef());
            damper.iaqCompensatedMinPos =(int) (damper.minPosition * smartPurgeDABDamperMinOpenMultiplier);
        } else
            damper.iaqCompensatedMinPos = damper.minPosition;
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
    }
    
    protected void setDamperLimits(Damper d) {
        d.minPosition = (int)dabEquip.getDamperLimit(state == HEATING ? "heating":"cooling", "min");
        d.maxPosition = (int)dabEquip.getDamperLimit(state == HEATING ? "heating":"cooling", "max");
        d.iaqCompensatedMinPos = d.minPosition;
    }
    
    public double getCo2LoopOp() {
        return dabEquip.getCo2Loop().getLoopOutput();
    }
    
    @Override
    public void reset(){
        double damperMin = dabEquip.getDamperLimit(state == HEATING ? "heating":"cooling", "min");
        dabEquip.setDamperPos(damperMin, "primary");
        dabEquip.setDamperPos(damperMin, "secondary");
        dabEquip.setCurrentTemp(0);
    }
}
