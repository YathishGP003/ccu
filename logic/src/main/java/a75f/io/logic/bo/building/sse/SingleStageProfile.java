package a75f.io.logic.bo.building.sse;

import static a75f.io.logic.L.TAG_CCU_ZONE;
import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.RFDEAD;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.domain.api.Point;
import a75f.io.domain.config.ProfileConfiguration;
import a75f.io.domain.equips.SseEquip;
import a75f.io.domain.util.ModelLoader;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.SSERelay2;
import a75f.io.logic.bo.building.hvac.SSEStage;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.schedules.ScheduleUtil;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective;

/**
 * Created by Anilkumar on 8/19/2019.
 */

public class SingleStageProfile extends ZoneProfile
{
    SseEquip sseEquip;
    int nodeAddr;
    ProfileType profileType;
    SingleStageProfile sseProfile;

    double      currentTemp;
    double desiredTemp;

    String equipRef = null;

    public void addSSEEquip(short addr, String equipRef) {
        this.equipRef = equipRef;
        sseEquip = new SseEquip(equipRef);
        profileType = ProfileType.SSE;
        nodeAddr = addr;
        sseProfile = this;
    }

    public void addSSEEquip(short addr) {
        nodeAddr = addr;
        equipRef = getEquipRef();
        sseEquip = new SseEquip(equipRef);
        profileType = getProfileType();
        sseProfile = new SingleStageProfile();
    }

    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.SSE;
    }

    @Override
    public <T extends BaseProfileConfiguration> T getProfileConfiguration(short address) {
        return null;
    }

    @Override
    public ProfileConfiguration getDomainProfileConfiguration() {
        Equip equip = getEquip();
        NodeType nodeType = equip.getDomainName().contains("helionode") ? NodeType.HELIO_NODE : NodeType.SMART_NODE;
        return new SseProfileConfiguration(nodeAddr, nodeType.name(),
                0,
                equip.getRoomRef(),
                equip.getFloorRef() ,
                profileType,
                (SeventyFiveFProfileDirective) ModelLoader.INSTANCE.getModelForDomainName(equip.getDomainName()))
                .getActiveConfiguration();

    }

    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short) nodeAddr);
        }};
    }

    @Override
    public Equip getEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+ this.nodeAddr+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }

    @Override
    public void updateZonePoints()
    {
        if (isRFDead()) {
            handleRFDead();
            return;
        } else if (isZoneDead()) {
            reset((short) nodeAddr);
            CcuLog.d(TAG_CCU_ZONE,"sse Zone Temp Dead: "+ this.nodeAddr+" roomTemp : "+ this.getCurrentTemp());
            state = TEMPDEAD;
            if (sseEquip.getEquipStatus().readHisVal() != state.ordinal()) {
                sseEquip.getEquipStatusMessage().writeDefaultVal("Zone Temp Dead");
            }
            sseEquip.getEquipStatus().writeHisVal(TEMPDEAD.ordinal());
            setStatus(controlFanStage(), state.ordinal(), false);
            return;
        }

            Equip equip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + this.nodeAddr + "\"")).build();
            double setTempCooling = sseEquip.getDesiredTempCooling().readPriorityVal();
            double setTempHeating = sseEquip.getDesiredTempHeating().readPriorityVal();
            double avgSetTemp = getDesiredTemp();
            double roomTemp = getCurrentTemp();
            //For dual temp but for single mode we use tuners
            double hysteresis = sseEquip.getStandaloneStage1Hysteresis().readPriorityVal();
            double relay1config = sseEquip.getRelay1OutputState().readDefaultVal();
            SSEStage sseStage = SSEStage.NOT_INSTALLED;
            if(relay1config > 0)
                sseStage = SSEStage.values()[(int) sseEquip.getRelay1OutputAssociation().readDefaultVal() + 1];
            double relay2config = sseEquip.getRelay2OutputState().readDefaultVal();
            double relay2Association = sseEquip.getRelay2OutputAssociation().readDefaultVal();
            String zoneId = HSUtil.getZoneIdFromEquipId(equip.getId());
            Occupied occuStatus = ScheduleManager.getInstance().getOccupiedModeCache(zoneId);
            boolean occupied = ScheduleUtil.isZoneOccupied(CCUHsApi.getInstance(), zoneId, Occupancy.OCCUPIED);
            boolean autoForceOccupied = ScheduleUtil.isZoneOccupied(CCUHsApi.getInstance(), zoneId, Occupancy.AUTOFORCEOCCUPIED);
            boolean forceOccupied = ScheduleUtil.isZoneOccupied(CCUHsApi.getInstance(), zoneId, Occupancy.FORCEDOCCUPIED);
            String stageStatus = "";
            CcuLog.d(TAG_CCU_ZONE, "sse profile =" + roomTemp + "," + sseStage.name()+","+avgSetTemp);
            if ((roomTemp > 0) && (sseStage == SSEStage.COOLING)) {
                //Zone is in Cooling
                state = COOLING;
                if (roomTemp >= setTempCooling) {
                    stageStatus = " Stage 1 Cool ON";
                    setCmdSignal(sseEquip.getCoolingStage1(), 1.0);
                    if (relay2config > 0) {
                        if(relay2Association == SSERelay2.FAN.ordinal()){
                            stageStatus = stageStatus + ", Fan ON";
                            setCmdSignal(sseEquip.getFanStage1(), 1.0);
                        }else{
                            if(occupied) {
                                stageStatus = stageStatus + ", Equip ON";
                                sseEquip.getOccupiedEnable().writeHisVal(1.0);
                            }
                            else {
                                stageStatus = stageStatus + ", Equip OFF";
                                sseEquip.getOccupiedEnable().writeHisVal(0.0);
                            }
                        }
                    }
                } else if (roomTemp <= setTempCooling - hysteresis) {
                    setCmdSignal(sseEquip.getCoolingStage1(), 0);
                    if ((relay2config > 0) && isOccupied(occupied, forceOccupied, autoForceOccupied)) {
                        if(relay2Association == SSERelay2.FAN.ordinal()){
                            stageStatus = "Fan ON";
                            setCmdSignal(sseEquip.getFanStage1(), 1.0);
                        }else{
                            stageStatus = "Equip ON";
                            sseEquip.getOccupiedEnable().writeHisVal(1.0);
                        }
                    } else {
                        setCmdSignal(sseEquip.getFanStage1(), 0);
                        sseEquip.getOccupiedEnable().writeHisVal(0.0);
                    }
                } else {
                    if (sseEquip.getCoolingStage1().readHisVal() > 0)
                        stageStatus = " Stage 1 Cool ON";
                    if ((relay2config > 0) && occupied) {
                        if(relay2Association == SSERelay2.FAN.ordinal()){
                            stageStatus = stageStatus.isEmpty() ? "Fan ON" : stageStatus + ", Fan ON";
                            setCmdSignal(sseEquip.getFanStage1(), 1.0);
                        }else{
                            stageStatus = stageStatus.isEmpty() ? "Equip ON" : stageStatus + ", Equip ON";
                            sseEquip.getOccupiedEnable().writeHisVal(1.0);
                        }
                    } else {
                        setCmdSignal(sseEquip.getFanStage1(), 0);
                        sseEquip.getOccupiedEnable().writeHisVal(0.0);
                    }
                }
            } else if ((roomTemp > 0) && (sseStage == SSEStage.HEATING)) {
                //Zone is in heating
                state = HEATING;
                if (roomTemp <= setTempHeating) {
                    stageStatus = " Stage 1 Heat ON";
                    setCmdSignal(sseEquip.getHeatingStage1(), 1.0);
                    if (relay2config > 0) {
                        if(relay2Association == SSERelay2.FAN.ordinal()) {
                            stageStatus = stageStatus + ", Fan ON";
                            setCmdSignal(sseEquip.getFanStage1(), 1.0);
                        }else {
                            if(isOccupied(occupied, forceOccupied, autoForceOccupied)) {
                                stageStatus = stageStatus + ", Equip ON";
                                sseEquip.getOccupiedEnable().writeHisVal(1.0);
                            }else{
                                stageStatus = stageStatus + ", Equip OFF";
                                sseEquip.getOccupiedEnable().writeHisVal(0.0);
                            }
                        }
                    }
                } else if (roomTemp >= (setTempHeating + hysteresis)) {
                    setCmdSignal(sseEquip.getHeatingStage1(), 0);
                    if ((relay2config > 0) && isOccupied(occupied, forceOccupied, autoForceOccupied)) {
                        if(relay2Association == SSERelay2.FAN.ordinal()){
                            stageStatus = "Fan ON";
                            setCmdSignal(sseEquip.getFanStage1(), 1.0);
                        }else{
                            stageStatus = "Equip ON";
                            sseEquip.getOccupiedEnable().writeHisVal(1.0);
                        }
                    } else {
                        setCmdSignal(sseEquip.getFanStage1(), 0);
                        sseEquip.getOccupiedEnable().writeHisVal(0.0);
                    }
                } else {
                    if (sseEquip.getHeatingStage1().readHisVal() > 0)
                        stageStatus = " Stage 1 Heat ON";
                    if ((relay2config > 0) && occupied) {
                        if(relay2Association == SSERelay2.FAN.ordinal()){
                            stageStatus = stageStatus.isEmpty() ? "Fan ON" : stageStatus + ", Fan ON";
                            setCmdSignal(sseEquip.getFanStage1(), 1.0);
                        }else{
                            stageStatus = stageStatus.isEmpty() ? "Equip ON" : stageStatus + ", Equip ON";
                            sseEquip.getOccupiedEnable().writeHisVal(1.0);
                        }
                    } else {
                        setCmdSignal(sseEquip.getFanStage1(), 0);
                        sseEquip.getOccupiedEnable().writeHisVal(0.0);
                    }
                }
            } else {
                // neither heating, cooling, nor zone dead
               stageStatus = controlFanStage();
                //Fan is already handled. Just update heating/cooling.
                resetConditioning((short) this.nodeAddr);
                state = DEADBAND;
            }
           setStatus(stageStatus, state.ordinal(), (state == HEATING ? buildingLimitMinBreached() : state == COOLING ? buildingLimitMaxBreached() : false));
    }

    private void handleRFDead() {
        state = RFDEAD;
        sseEquip.getEquipStatus().writeHisVal(RFDEAD.ordinal());
        String curStatus = sseEquip.getEquipStatusMessage().readDefaultStrVal();
        if (!curStatus.equals(RFDead))
        {
            sseEquip.getEquipStatusMessage().writeDefaultVal(RFDead);
            sseEquip.getOccupancyMode().writeHisVal(0);
        }
    }

    @JsonIgnore
    public double getDisplayCurrentTemp() {
        return getCurrentTemp();
    }

    private void setCmdSignal(Point point, double value) {
        if(point.isWritable()) {
            point.writeDefaultVal(value);
            value = point.readPriorityVal();
        }
        point.writeHisVal(value);
    }

    public void reset(short node){
        setCmdSignal(sseEquip.getCoolingStage1(), 0);
        setCmdSignal(sseEquip.getHeatingStage1(), 0);
        setCmdSignal(sseEquip.getFanStage1(), 0);
    }
    
    public void resetConditioning(short node){
        //There might be failures here. We need to check if heating/cooling point exists before writing.
        setCmdSignal(sseEquip.getCoolingStage1(), 0);
        setCmdSignal(sseEquip.getHeatingStage1(), 0);
    }
    
    @Override
    public void reset(){
        if(sseEquip != null){
            setCurrentTemp(0);
        }
    }
    private String controlFanStage() {
        String zoneId = HSUtil.getZoneIdFromEquipId(equipRef);
        double relay2Association = sseEquip.getRelay2OutputAssociation().readPriorityVal();
        boolean occupied = ScheduleUtil.isZoneOccupied(CCUHsApi.getInstance(), zoneId, Occupancy.OCCUPIED);
        String stageStatus = "";
        if(sseEquip.getRelay2OutputState().readPriorityVal() > 0){
            if ((relay2Association == SSERelay2.FAN.ordinal()) && occupied) {
                stageStatus = "Fan ON";
                sseEquip.getFanStage1().writeHisVal(1.0);
            }else if ((relay2Association == SSERelay2.OCCUPIED_ENABLE.ordinal()) && occupied) {
                stageStatus = "Equip ON";
                sseEquip.getOccupiedEnable().writeHisVal(1.0);
            }else if((relay2Association == SSERelay2.OCCUPIED_ENABLE.ordinal()) && !occupied){
                stageStatus = "Equip OFF";
                sseEquip.getOccupiedEnable().writeHisVal(0.0);
            }
        }else{
                sseEquip.getFanStage1().writeHisVal(0.0);
                sseEquip.getOccupiedEnable().writeHisVal(0.0);
                stageStatus = "Equip Not Enabled";
        }
        return stageStatus;
    }

    private boolean isOccupied(boolean occupied, boolean forceOccupied, boolean autoForceOccupied) {
        return occupied || autoForceOccupied || forceOccupied;
    }

    public double getCurrentTemp()
    {
        currentTemp = sseEquip.getCurrentTemp().readHisVal();
        return currentTemp;
    }

    public void setCurrentTemp(double roomTemp)
    {
        sseEquip.getCurrentTemp().writeHisVal(roomTemp);
        this.currentTemp = roomTemp;
    }

    public double getDesiredTemp()
    {
        desiredTemp = sseEquip.getDesiredTemp().readDefaultVal();
        return desiredTemp;
    }

    public void setStatus(String sseStatus, double status, boolean emergency) {
        sseEquip.getEquipStatus().writeHisVal(status);
        String message;
        if (emergency) {
            message = (status == 0 ? "Recirculating Air" : status == 1 ? "Emergency Cooling" : "Emergency Heating");
        } else {
            message = (status == 3 ? "Zone Temp Dead" : status == 0 ? "" : status == 1 ? "Cooling Space" : "Warming Space");
            if(!sseStatus.isEmpty()){
                if(sseStatus.equals("Fan ON")) {
                    message = "Recirculating Air, " + sseStatus;
                } else if (!message.isEmpty()) {
                    message = message + ","+sseStatus;
                } else {
                    message = sseStatus;
                }
            }
        }
        String curStatus = sseEquip.getEquipStatusMessage().readDefaultStrVal();
        if (!curStatus.equals(message)) {
            sseEquip.getEquipStatusMessage().writeDefaultVal(message);
        }
        CcuLog.i(TAG_CCU_ZONE, "SSE("+getNodeAddresses().toString()+") status: " + message);
    }

   public String getEquipRef(){
       HashMap<Object, Object>  equipMap = CCUHsApi.getInstance().readEntity("equip and group == \"" + nodeAddr + "\"");
       if (equipMap != null && !equipMap.isEmpty()) {
           equipRef = equipMap.get("id").toString();
       }
       return equipRef;
    }
}
