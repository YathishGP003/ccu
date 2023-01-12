package a75f.io.logic.bo.building.dab;

import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.schedules.ScheduleUtil;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.tuners.BuildingTunerCache;
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
    
    //State prior to changeover. User to identify the direction in which the PiLoop to be run while in deadband.
    ZoneState prevState = DEADBAND;
    
    private static final int LOOP_OP_MIDPOINT = 50;

    private ControlLoop heatingLoop;
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
        dabEquip.init();
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
        
        double buildingLimitMax =  BuildingTunerCache.getInstance().getBuildingLimitMax();
        double buildingLimitMin =  BuildingTunerCache.getInstance().getBuildingLimitMin();
        
        double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();
    
        if (dabEquip.getCurrentTemp() > (buildingLimitMax + tempDeadLeeway)
            || dabEquip.getCurrentTemp() < (buildingLimitMin - tempDeadLeeway))
        {
            return true;
        }
        
        return false;
    }
    
    @Override
    public void updateZonePoints() {
        
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

        heatingLoop = dabEquip.getHeatingLoop();

        SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();
        SystemMode systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];

        int satConditioning = CCUHsApi.getInstance().readHisValByQuery("system and sat and conditioning").intValue();
        if (systemMode != SystemMode.OFF) {
            if (satConditioning > 0) {
                //Effective SAT conditioning is available. Run the PI loop based on that.
                if (satConditioning == SystemController.EffectiveSatConditioning.SAT_COOLING.ordinal()) {
                    damperOpController.updateControlVariable(roomTemp, setTempCooling);
                } else {
                    damperOpController.updateControlVariable(setTempHeating, roomTemp);
                }
            } else {
                //Fall back to System-conditioning based PI loop.
                if (conditioning == SystemController.State.COOLING) {
                    damperOpController.updateControlVariable(roomTemp, setTempCooling);
                } else {
                    damperOpController.updateControlVariable(setTempHeating, roomTemp);
                }
            }

            if (CCUHsApi.getInstance().readDefaultVal("reheat and type and equipRef == \""+dabEquip.getId()+"\"").intValue() > 0) {
                handleReheat(setTempHeating, roomTemp);
                heatingLoop.dump();
            }

        } else {
            damperOpController.reset();
            CCUHsApi.getInstance().writeHisValByQuery("reheat and pos and equipRef == \""+dabEquip.getId()+"\"", 0.0);
        }

        updateZoneState(roomTemp, setTempCooling, setTempHeating);

        Log.d(L.TAG_CCU_ZONE, "DAB-"+dabEquip.nodeAddr+" : roomTemp " + roomTemp
                + " setTempCooling:  " + setTempCooling+" setTempHeating: "+setTempHeating
                + " satConditioning "+satConditioning);
        damperOpController.dump();

        //Loop Output varies from 0-100% such that, it is 50% at 0 error, 0% at maxNegative error, 100% at maxPositive
        //error
        double midPointBalancedLoopOp = LOOP_OP_MIDPOINT +
                damperOpController.getControlVariable() * LOOP_OP_MIDPOINT / damperOpController.getMaxAllowedError();

        setDamperLimits(damper, conditioning);
        updateDamperIAQCompensation();

        damper.currentPosition =
            (int)(damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * midPointBalancedLoopOp / 100);

        dabEquip.setDamperPos(damper.currentPosition, "primary");
        dabEquip.setDamperPos(damper.currentPosition, "secondary");
        
        dabEquip.setStatus(state.ordinal(), DabSystemController.getInstance().isEmergencyMode() && (state == HEATING ? buildingLimitMinBreached()
                                                    : state == COOLING ? buildingLimitMaxBreached() : false));
        CcuLog.d(L.TAG_CCU_ZONE, "System STATE :" + conditioning
                                 + " ZoneState : " + getState()
                                 + " ,CV: " + damperOpController.getControlVariable()
                                 +" , midPointBalancedLoopOp "+midPointBalancedLoopOp
                                 + " ,damper:" + damper.currentPosition);
    }

    private void updateZoneState(double roomTemp, double setTempCooling, double setTempHeating) {
        if (roomTemp > setTempCooling) {
            state = COOLING;
        } else if (roomTemp < setTempHeating) {
            state = HEATING;
        } else {
            state = DEADBAND;
        }

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
            CCUHsApi.getInstance().writeHisValByQuery("reheat and pos and equipRef == \""+dabEquip.getId()+"\"", 0.0);
            CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + dabEquip.nodeAddr + "\"", (double) TEMPDEAD.ordinal());
        }
    }
    
    private void updateDamperIAQCompensation() {
        boolean  enabledCO2Control = dabEquip.getConfigNumVal("enable and co2") > 0 ;
        boolean  enabledIAQControl = dabEquip.getConfigNumVal("enable and iaq") > 0 ;
        String zoneId = HSUtil.getZoneIdFromEquipId(dabEquip.getId());
        boolean occupied = ScheduleUtil.isZoneOccupied(CCUHsApi.getInstance(), zoneId);
    
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+L.ccu().systemProfile.getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        if((epidemicState != EpidemicState.OFF) && (L.ccu().oaoProfile != null)) {
            double smartPurgeDABDamperMinOpenMultiplier = TunerUtil.readTunerValByQuery("purge and system and dab and damper and pos and multiplier and min ", L.ccu().oaoProfile.getEquipRef());
            damper.iaqCompensatedMinPos =(int) (damper.minPosition * smartPurgeDABDamperMinOpenMultiplier);
        } else
            damper.iaqCompensatedMinPos = damper.minPosition;
        //CO2 loop output from 0-100% modulates damper min position.
        if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) > 0)
        {
            damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * co2Loop.getLoopOutput() / 100;
            CcuLog.d(L.TAG_CCU_ZONE, "CO2LoopOp :" + co2Loop.getLoopOutput() + ", adjusted minposition " + damper.iaqCompensatedMinPos);
        }
    
        //VOC loop output from 0-100% modulates damper min position.
        if (enabledIAQControl && occupied && vocLoop.getLoopOutput(voc) > 0)
        {
            damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * vocLoop.getLoopOutput() / 100;
            CcuLog.d(L.TAG_CCU_ZONE,"VOCLoopOp :"+vocLoop.getLoopOutput()+", adjusted minposition "+damper.iaqCompensatedMinPos+" damper.minPosition "+damper.minPosition);
        }
    }
    
    protected void setDamperLimits(Damper d, SystemController.State conditioning) {
        d.minPosition = (int)dabEquip.getDamperLimit(conditioning == SystemController.State.HEATING ? "heating":"cooling", "min");
        d.maxPosition = (int)dabEquip.getDamperLimit(conditioning == SystemController.State.HEATING ? "heating":"cooling", "max");
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

    private void handleReheat(double desiredTempHeating, double currentTemp) {
        double reheatOffset = TunerUtil.readTunerValByQuery("tuner and reheat and offset and equipRef == \"" +
                                            dabEquip.getId()+"\"");
        double heatingLoopOp = Math.max(0, heatingLoop.getLoopOutput(desiredTempHeating - reheatOffset, currentTemp));
        CcuLog.i(L.TAG_CCU_ZONE, "handleReheat : reheatOffset "+reheatOffset+" heatingLoopOp "+heatingLoopOp);
        CCUHsApi.getInstance().writeHisValByQuery("reheat and pos and equipRef == \""+dabEquip.getId()+"\"",
                                                Double.valueOf((int)heatingLoopOp));
    }
}
