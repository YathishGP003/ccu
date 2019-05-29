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
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMP_DEAD;

/**
 * Created by samjithsadasivan on 3/13/19.
 */

public class DabProfile extends ZoneProfile
{
    DabEquip dabEquip;
    
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
        
        double buildingLimitMax =  TunerUtil.readTunerValByQuery("building and limit and max", L.ccu().systemProfile.getSystemEquipRef());
        double buildingLimitMin =  TunerUtil.readTunerValByQuery("building and limit and min", L.ccu().systemProfile.getSystemEquipRef());
        
        double tempDeadLeeway = TunerUtil.readTunerValByQuery("temp and dead and leeway",L.ccu().systemProfile.getSystemEquipRef());
    
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
            CcuLog.d(L.TAG_CCU_ZONE,"Zone Temp Dead: "+dabEquip.nodeAddr+" roomTemp : "+dabEquip.getCurrentTemp());
            state = TEMP_DEAD;
            String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \""+dabEquip.nodeAddr+"\"");
            if (!curStatus.equals("Zone Temp Dead"))
            {
                CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + dabEquip.nodeAddr + "\"", "Zone Dead");
            }
            return;
        }
        
        double setTempCooling = dabEquip.getDesiredTempCooling();
        double setTempHeating = dabEquip.getDesiredTempHeating();
        double roomTemp = dabEquip.getCurrentTemp();
        GenericPIController damperOpController = dabEquip.damperController;
    
        CO2Loop co2Loop = dabEquip.getCo2Loop();
        VOCLoop vocLoop = dabEquip.getVOCLoop();
        
        double co2 = dabEquip.getCO2();
        double voc = dabEquip.getVOC();
        
        Damper damper = new Damper();
        Log.d(L.TAG_CCU_ZONE, "DAB : roomTemp" + roomTemp + " setTempCooling:  " + setTempCooling+" setTempHeating: "+setTempHeating);
    
        SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();
    
        if (roomTemp > setTempCooling)
        {
            //Zone is in Cooling
            if (state != COOLING)
            {
                state = COOLING;
                damperOpController.reset();
            }
            if (conditioning == SystemController.State.COOLING)
            {
                damperOpController.updateControlVariable(roomTemp, setTempCooling);
            }
        }
        else if (roomTemp < setTempHeating)
        {
            //Zone is in heating
            if (state != HEATING)
            {
                state = HEATING;
                damperOpController.reset();
            }
            if (conditioning == SystemController.State.HEATING)
            {
                damperOpController.updateControlVariable(setTempHeating, roomTemp);
            }
        } else {
            if (state != DEADBAND) {
                state = DEADBAND;
                damperOpController.reset();
            }
           
        }
        damperOpController.dump();
        setDamperLimits(damper);
    
        boolean  enabledCO2Control = dabEquip.getConfigNumVal("enable and co2") > 0 ;
        boolean  enabledIAQControl = dabEquip.getConfigNumVal("enable and iaq") > 0 ;
        String zoneId = HSUtil.getZoneIdFromEquipId(dabEquip.getId());
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
        boolean occupied = (occ == null ? false : occ.isOccupied());
        
        //CO2 loop output from 0-50% modulates damper min position.
        if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) > 0)
        {
            damper.iaqCompensatedMinPos = damper.minPosition + (damper.maxPosition - damper.minPosition) * Math.min(50, co2Loop.getLoopOutput()) / 50;
            CcuLog.d(L.TAG_CCU_ZONE, "CO2LoopOp :" + co2Loop.getLoopOutput() + ", adjusted minposition " + damper.iaqCompensatedMinPos);
        }
    
        //VOC loop output from 0-50% modulates damper min position.
        if (enabledIAQControl && occupied && vocLoop.getLoopOutput(voc) > 0)
        {
            damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * Math.min(50, vocLoop.getLoopOutput()) / 50;
            CcuLog.d(L.TAG_CCU_ZONE,"VOCLoopOp :"+vocLoop.getLoopOutput()+", adjusted minposition "+damper.iaqCompensatedMinPos+" damper.minPosition "+damper.minPosition);
        }
    
        
        damper.currentPosition = (int)(damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * (damperOpController.getControlVariable() / damperOpController.getMaxAllowedError()));
        
        dabEquip.setDamperPos(damper.currentPosition, "primary");
        dabEquip.setDamperPos(damper.currentPosition, "secondary");
        
        dabEquip.setNormalizedDamperPos(damper.currentPosition, "primary");
        dabEquip.setNormalizedDamperPos(damper.currentPosition, "secondary");
    
        dabEquip.setStatus(state.ordinal(), DabSystemController.getInstance().isEmergencyMode() && (state == HEATING ? buildingLimitMinBreached()
                                                    : state == COOLING ? buildingLimitMaxBreached() : false));
        CcuLog.d(L.TAG_CCU_ZONE, "System STATE :" + DabSystemController.getInstance().getSystemState() + " ZoneState : " + getState() + " ,CV: " + damperOpController.getControlVariable() + " ,damper:" + damper.currentPosition);
    
    }
    
    protected void setDamperLimits(Damper d) {
        d.minPosition = (int)dabEquip.getDamperLimit(state == HEATING ? "heating":"cooling", "min");
        d.maxPosition = (int)dabEquip.getDamperLimit(state == HEATING ? "heating":"cooling", "max");
        d.iaqCompensatedMinPos = d.minPosition;
    }
}
