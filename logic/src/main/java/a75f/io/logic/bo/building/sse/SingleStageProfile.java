package a75f.io.logic.bo.building.sse;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.SSEStage;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.StandaloneTunerUtil;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

/**
 * Created by Anilkumar on 8/19/2019.
 */

public class SingleStageProfile extends ZoneProfile
{
    SingleStageEquip sseEquip;

    public void addSSEEquip(short addr, SingleStageConfig config, String floorRef, String roomRef) {
        sseEquip = new SingleStageEquip(getProfileType(), addr);
        sseEquip.createHaystackPoints(config, floorRef, roomRef);
    }

    public void addSSEEquip(short addr) {
        sseEquip = new SingleStageEquip(getProfileType(), addr);
    }

    public void updateSSEEquip(Short nodeaddr, SingleStageConfig config, String roomRef) {
        sseEquip.updateHaystackPoints(config);
    }

    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.SSE;
    }

    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return sseEquip.getProfileConfiguration();
    }

    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short)sseEquip.nodeAddr);
        }};
    }

    @Override
    public Equip getEquip()
    {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+sseEquip.nodeAddr+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }

    @Override
    public boolean isZoneDead() {

        double buildingLimitMax =  BuildingTunerCache.getInstance().getBuildingLimitMax();
        double buildingLimitMin =  BuildingTunerCache.getInstance().getBuildingLimitMin();

        double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();

        if (sseEquip.getCurrentTemp() > (buildingLimitMax + tempDeadLeeway)
                || sseEquip.getCurrentTemp() < (buildingLimitMin - tempDeadLeeway))
        {
            return true;
        }

        return false;
    }

    @Override
    public void updateZonePoints()
    {
        if (isZoneDead()) {
            reset((short) sseEquip.nodeAddr);
            CcuLog.d(L.TAG_CCU_UI,"sse Zone Temp Dead: "+sseEquip.nodeAddr+" roomTemp : "+sseEquip.getCurrentTemp());
            state = TEMPDEAD;
            CCUHsApi.getInstance().writeHisValByQuery("point and status and not message and his and group == \"" + sseEquip.nodeAddr + "\"", (double) TEMPDEAD.ordinal());
            String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \""+sseEquip.nodeAddr+"\"");
            if (!curStatus.equals("Zone Temp Dead"))
            {
                CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + sseEquip.nodeAddr + "\"", "Zone Temp Dead");
                CCUHsApi.getInstance().writeHisValByQuery("occupancy and mode and standalone and " +
                        "group == \"" + sseEquip.nodeAddr + "\"", 0.0);
            }
            return;
        }

            Equip equip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + sseEquip.nodeAddr + "\"")).build();
            double setTempCooling = sseEquip.getDesiredTempCooling();
            double setTempHeating = sseEquip.getDesiredTempHeating();
            double avgSetTemp = sseEquip.getDesiredTemp();
            double roomTemp = sseEquip.getCurrentTemp();
            //For dual temp but for single mode we use tuners
            double hysteresis = StandaloneTunerUtil.getStandaloneStage1Hysteresis(equip.getId());
            SSEStage sseStage = SSEStage.values()[(int) getConfigEnabled("enable and relay1",(short)sseEquip.nodeAddr)];
            double relay2config = getConfigEnabled("enable and relay2",(short)sseEquip.nodeAddr);
            String zoneId = HSUtil.getZoneIdFromEquipId(equip.getId());
            Occupied occuStatus = ScheduleProcessJob.getOccupiedModeCache(zoneId);
            boolean occupied = (occuStatus == null ? false : occuStatus.isOccupied());
            String stageStatus = "";
            Log.d("SSE", "sse profile11 =" + roomTemp + "," + sseStage.name()+","+avgSetTemp);
            if ((roomTemp > 0) && (sseStage == SSEStage.COOLING)) {
                //Zone is in Cooling
                state = COOLING;
                if (roomTemp >= setTempCooling) {
                    stageStatus = " Stage 1 Cool ON";
                    setCmdSignal("cooling and stage1", 1.0, (short) sseEquip.nodeAddr);
                    if (relay2config > 0) {
                        stageStatus = stageStatus + ", Fan ON";
                        setCmdSignal("fan and stage1", 1.0, (short) sseEquip.nodeAddr);
                    }
                } else if (roomTemp <= setTempCooling - hysteresis) {
                    setCmdSignal("cooling and stage1", 0, (short) sseEquip.nodeAddr);
                    if ((relay2config > 0) && occupied) {
                        stageStatus = "Fan ON";
                        setCmdSignal("fan and stage1", 1.0, (short) sseEquip.nodeAddr);
                    } else
                        setCmdSignal("fan and stage1", 0, (short) sseEquip.nodeAddr);
                } else {
                    if (getCmdSignal("cooling and stage1", (short) sseEquip.nodeAddr) > 0)
                        stageStatus = " Stage 1 Cool ON";
                    if ((relay2config > 0) && occupied) {
                        stageStatus = stageStatus.isEmpty() ? "Fan ON" : stageStatus + ", Fan ON";
                        setCmdSignal("fan and stage1", 1.0, (short) sseEquip.nodeAddr);
                    } else
                        setCmdSignal("fan and stage1", 0, (short) sseEquip.nodeAddr);
                }
            } else if ((roomTemp > 0) && (sseStage == SSEStage.HEATING)) {
                //Zone is in heating
                state = HEATING;
                if (roomTemp <= setTempHeating) {
                    stageStatus = " Stage 1 Heat ON";
                    setCmdSignal("heating and stage1", 1.0, (short) sseEquip.nodeAddr);
                    if (relay2config > 0) {
                        stageStatus = stageStatus + ", Fan ON";
                        setCmdSignal("fan and stage1", 1.0, (short) sseEquip.nodeAddr);
                    }
                } else if (roomTemp >= (setTempHeating + hysteresis)) {
                    setCmdSignal("heating and stage1", 0, (short) sseEquip.nodeAddr);
                    if ((relay2config > 0) && occupied) {
                        stageStatus = "Fan ON";
                        setCmdSignal("fan and stage1", 1.0, (short) sseEquip.nodeAddr);
                    } else
                        setCmdSignal("fan and stage1", 0, (short) sseEquip.nodeAddr);
                } else {
                    if (getCmdSignal("heating and stage1", (short) sseEquip.nodeAddr) > 0)
                        stageStatus = " Stage 1 Heat ON";
                    if ((relay2config > 0) && occupied) {
                        stageStatus = stageStatus.isEmpty() ? "Fan ON" : stageStatus + ", Fan ON";
                        setCmdSignal("fan and stage1", 1.0, (short) sseEquip.nodeAddr);
                    } else
                        setCmdSignal("fan and stage1", 0, (short) sseEquip.nodeAddr);
                }
            } else {
                // neither heating, cooling, nor zone dead
                if ((relay2config > 0) && occupied) {
                    stageStatus = stageStatus.isEmpty() ? "Fan ON" : stageStatus + ", Fan ON";
                    setCmdSignal("fan and stage1", 1.0, (short) sseEquip.nodeAddr);
                } else
                    setCmdSignal("fan and stage1", 0, (short) sseEquip.nodeAddr);
                
                //Fan is already handled. Just update heating/cooling.
                resetConditioning((short) sseEquip.nodeAddr);
                state = DEADBAND;
            }
           sseEquip.setStatus(stageStatus, state.ordinal(), (state == HEATING ? buildingLimitMinBreached() : state == COOLING ? buildingLimitMaxBreached() : false));
            if (occuStatus != null) {
                sseEquip.setProfilePoint("occupancy and status", occuStatus.isOccupied() ? Occupancy.OCCUPIED.ordinal() : (occuStatus.isPreconditioning() ? Occupancy.PRECONDITIONING.ordinal() : (occuStatus.isForcedOccupied() ? Occupancy.FORCEDOCCUPIED.ordinal() : 0)));
            } else {
                sseEquip.setProfilePoint("occupancy and status", occupied ? 1 : 0);
            }
    }
    @JsonIgnore
    @Override
    public double getCurrentTemp() {
        return sseEquip.getCurrentTemp();
    }
    @JsonIgnore
    public double getDisplayCurrentTemp()
    {
        return sseEquip.getCurrentTemp();
    }

    public double getConfigEnabled(String config, short node) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and "+config+" and group == \"" + node + "\"");

    }

    public double getCmdSignal(String cmd, short node) {
        return CCUHsApi.getInstance().readHisValByQuery("point and sse and cmd and his and "+cmd+" and group == \"" + node + "\"");
    }
    public void setCmdSignal(String cmd, double val, short node) {
        CCUHsApi.getInstance().writeHisValByQuery("point and sse and cmd and his and "+cmd+" and group == \"" + node + "\"", val);
    }
    public void reset(short node){
        setCmdSignal("cooling and stage1",0,node);
        setCmdSignal("heating and stage1",0,node);
        setCmdSignal("fan and stage1",0,node);
    }
    
    public void resetConditioning(short node){
        //There might be failures here. We need to check if heating/cooling point exists before writing.
        setCmdSignal("cooling and stage1",0,node);
        setCmdSignal("heating and stage1",0,node);
    }
    
    @Override
    public void reset(){
        if(sseEquip != null){
            sseEquip.setCurrentTemp(0);

        }
    }
}
