package a75f.io.logic.bo.building.plc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75.io.algos.GenericPIController;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Created by samjithsadasivan on 2/25/19.
 */

public class PlcProfile extends ZoneProfile {
    //PlcEquip plcEquip;
    int outputSignal = 0;

    public PlcProfile() {
    }

    public PlcProfile(short addr) {
        //plcEquip = new PlcEquip(ProfileType.PLC, addr);
        //plcEquip.init();
    }

    public void addPlcEquip(short addr, PlcProfileConfiguration config, String floorRef,
                            String roomRef, String processVariable, String dynamicTargetTag, NodeType nodeType) {
        //plcEquip = new PlcEquip(getProfileType(), addr);
        //plcEquip.createEntities(config, floorRef, roomRef, processVariable, dynamicTargetTag, nodeType);
        //plcEquip.init();
    }

    public void addPlcEquip(short addr) {
        //plcEquip = new PlcEquip(getProfileType(), addr);
        //plcEquip.init();
    }

    public void updatePlcEquip(PlcProfileConfiguration config, String floorRef, String zoneRef, String processTag, String dynamicTargetTag) {
        //plcEquip.update(config,floorRef,zoneRef,processTag, dynamicTargetTag);
        //plcEquip.init();
    }

    public PlcProfile(short addr, String equipRef) {
        this.equipRef = equipRef;
        this.nodeAddr = addr;
        init();
    }

    a75f.io.domain.equips.PlcEquip plcEquip;
    String equipRef = null;
    Short nodeAddr;
    GenericPIController plc;

    double targetValue;
    double spSensorOffset;
    boolean isEnabledAnalog2InForSp;
    boolean isEnabledZeroErrorMidpoint;

    private boolean pendingTunerChange;

    public boolean hasPendingTunerChange() {
        return pendingTunerChange;
    }

    public void setPendingTunerChange() {
        pendingTunerChange = true;
    }

    // public void setPendingTunerChange() { plcEquip.setPendingTunerChange(); }

    private String processVariableDomainName;

    @Override
    public ProfileType getProfileType() {
        return ProfileType.PLC;
    }

    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address) {
        return null;
    }

    @Override
    public Set<Short> getNodeAddresses() {
        return new HashSet<Short>() {{
            add(nodeAddr);
        }};
    }

    @Override
    public Equip getEquip() {
        HashMap<Object, Object> equip = CCUHsApi.getInstance().readEntity("equip and group == \"" + nodeAddr + "\"");
        return new Equip.Builder().setHashMap(equip).build();
    }

    @Override
    public void updateZonePoints() {

        double processVariable = CCUHsApi.getInstance().readHisValByQuery("point and equipRef == \"" + equipRef + "\" and domainName == \"" + processVariableDomainName + "\"");
        double targetValue = plcEquip.getPidTargetValue().readPriorityVal();
        double controlVariable;
        if (hasPendingTunerChange()) refreshPITuners();

        if (isEnabledAnalog2InForSp) {
            CcuLog.d(L.TAG_CCU_ZONE, "Use analog 2 offset ");
            targetValue = plcEquip.getDynamicTargetValue().readDefaultVal();
        }
        CcuLog.d(L.TAG_CCU_ZONE, "PlcProfile, processVariable: " + processVariable + ", targetValue: " + targetValue);

        if (plcEquip.getInvertControlLoopoutput().readDefaultVal() > 0) {
            plc.updateControlVariable(processVariable, targetValue);
        } else {
            plc.updateControlVariable(targetValue, processVariable);
        }

        if (isEnabledZeroErrorMidpoint) {
            controlVariable = 50.0 + plc.getControlVariable() * 50.0 / plc.getMaxAllowedError();
        } else {
            //Get only the 0-100% portion of cv
            controlVariable = plc.getControlVariable() * 100.0 / plc.getMaxAllowedError();
            if (controlVariable < 0) {
                controlVariable = 0;
            }
        }

        double curCv = (double) Math.round(100 * controlVariable) / 100;
        int eStatus = (int) (Math.round(100 * controlVariable) / 100);
        plcEquip.getControlVariable().writePointValue(curCv);

        String statusMessage = getStatusMessage(equipRef, eStatus, CCUHsApi.getInstance());
        plcEquip.getEquipStatusMessage().writeDefaultVal(statusMessage);
        outputSignal = eStatus;

        handleRelayOp(outputSignal);

        plc.dump();
        CcuLog.i(L.TAG_CCU_ZONE, "PI Tuners: proportionalGain " + plc.getProportionalGain() + ", integralGain " + plc.getIntegralGain() +
                ", proportionalSpread " + plc.getMaxAllowedError() + ", integralMaxTimeout " + plc.getIntegralMaxTimeout());
        CcuLog.d(L.TAG_CCU_ZONE, "PlcProfile, processVariable: " + processVariable + ", targetValue: " + targetValue + ", controlVariable: " + controlVariable);
        CcuLog.i(L.TAG_CCU_ZONE, "PlcProfile, outputSignal: " + outputSignal + ", statusMessage: " + statusMessage);
    }

    /**
     * Generate a PI Loop Status Message.
     * Defer to CCU-UI spec of PI Profile for the format of this message.
     *
     * @param equipID
     * @param outputSignal
     * @param hayStack
     * @return
     */
    private String getStatusMessage(String equipID, int outputSignal, CCUHsApi hayStack) {

        double relay1Config = hayStack.readDefaultVal("point and relay1 and config and" +
                " enabled and equipRef == \"" + equipID + "\"");
        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("Loop output is " + (outputSignal > 0 ? "active" : "inactive"));
        if (relay1Config > Math.abs(0.01)) {
            double relay1Status = hayStack.readHisValByQuery("point and relay1 and cmd and equipRef" +
                    " == \"" + equipID + "\"");
            statusBuilder.append(", Relay1 " + (relay1Status > Math.abs(0.01) ? "ON" : "OFF"));
        }
        double relay2Config = hayStack.readDefaultVal("point and relay2 and config and" +
                " enabled and equipRef == \"" + equipID + "\"");

        if (relay2Config > Math.abs(0.01)) {
            double relay2Status = hayStack.readHisValByQuery("point and relay2 and cmd and equipRef" +
                    " == \"" + equipID + "\"");
            statusBuilder.append(", Relay2 " + (relay2Status > Math.abs(0.01) ? "ON" : "OFF"));
        }

        return statusBuilder.toString();
    }

    /**
     * Update the relay output status based on current loop output and threshold configurations.
     *
     * @param loopOp
     */
    private void handleRelayOp(double loopOp) {

        if (plcEquip.getRelay1OutputEnable().readDefaultVal() > 0) {
            if (loopOp > plcEquip.getRelay1OnThreshold().readDefaultVal()) {
                plcEquip.getRelay1Cmd().writePointValue(1);
            } else if (plcEquip.getRelay1Cmd().readHisVal() > 0 && loopOp < plcEquip.getRelay1OffThreshold().readDefaultVal()) {
                plcEquip.getRelay1Cmd().writePointValue(0);
            }
        } else {
            plcEquip.getRelay1Cmd().writePointValue(0);
        }

        if (plcEquip.getRelay2OutputEnable().readDefaultVal() > 0) {
            if (loopOp > plcEquip.getRelay2OnThreshold().readDefaultVal()) {
                plcEquip.getRelay2Cmd().writePointValue(1);
            } else if (plcEquip.getRelay2Cmd().readHisVal() > 0 && loopOp < plcEquip.getRelay2OffThreshold().readDefaultVal()) {
                plcEquip.getRelay2Cmd().writePointValue(0);
            }
        } else {
            plcEquip.getRelay2Cmd().writePointValue(0);
        }

    }


    public void init() {
        plcEquip = new a75f.io.domain.equips.PlcEquip(equipRef);
        pendingTunerChange = false;
        plc = new GenericPIController();
        plc.setMaxAllowedError(plcEquip.getPidProportionalRange().readPriorityVal());
        plc.setIntegralGain(plcEquip.getIntegralKFactor().readPriorityVal());
        plc.setProportionalGain(plcEquip.getProportionalKFactor().readPriorityVal());
        plc.setIntegralMaxTimeout((int) plcEquip.getPidIntegralTime().readPriorityVal());

        targetValue = plcEquip.getPidTargetValue().readDefaultVal();
        spSensorOffset = plcEquip.getSetpointSensorOffset().readDefaultVal();
        isEnabledAnalog2InForSp = plcEquip.getUseAnalogIn2ForSetpoint().readDefaultVal() > 0;
        isEnabledZeroErrorMidpoint = plcEquip.getExpectZeroErrorAtMidpoint().readDefaultVal() > 0;
        processVariableDomainName = updateProcessVariableDomainName();
    }

    public void refreshPITuners() {
        // proportionalSpread is a config point, so init() will be called if this value is changed.
        plc.setIntegralGain(plcEquip.getIntegralKFactor().readPriorityVal());
        plc.setProportionalGain(plcEquip.getProportionalKFactor().readPriorityVal());
        plc.setIntegralMaxTimeout((int) plcEquip.getPidIntegralTime().readPriorityVal());

        pendingTunerChange = false;
    }

    @Override
    public void reset() {
        plcEquip.getControlVariable().writePointValue(0);
    }

    private String updateProcessVariableDomainName() {
        ArrayList<HashMap<Object, Object>> points = CCUHsApi.getInstance().readAllEntities("point and equipRef == \"" + equipRef + "\"");
        for (HashMap<Object, Object> point : points) {
            if (point.get(Tags.DIS).toString().contains("processVariable")) {
                return point.get(Tags.DOMAIN_NAME).toString();
            }
        }
        return "";
    }

    public String getProcessVariableDomainName() {
        return processVariableDomainName;
    }
}
