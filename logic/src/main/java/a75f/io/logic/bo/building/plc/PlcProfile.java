package a75f.io.logic.bo.building.plc;

import static a75f.io.logic.bo.building.plc.PlcProfileUtilKt.getDynamicTargetPoint;
import static a75f.io.logic.bo.building.plc.PlcProfileUtilKt.getProcessVariableMappedPoint;

import org.apache.commons.lang3.StringUtils;
import org.projecthaystack.HDict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75.io.algos.GenericPIController;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.config.ProfileConfiguration;
import a75f.io.domain.util.ModelCache;
import a75f.io.domain.util.ModelLoader;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;
import a75f.io.logic.tuners.TunerUtil;
import io.seventyfivef.domainmodeler.client.ModelDirective;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective;

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

    private String processVariableDomainName;
    private String dynamicTargetDomainName;

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
        HDict equip = CCUHsApi.getInstance().readHDict("equip and group == \"" + nodeAddr + "\"");
        return new Equip.Builder().setHDict(equip).build();
    }

    @Override
    public void updateZonePoints() {
        updateSensorLinkedLogicalPoints();
        double processVariable =plcEquip.getProcessVariable().readHisVal();
        double targetValue = plcEquip.getPidTargetValue().readPriorityVal();
        double controlVariable;
        if (hasPendingTunerChange()) refreshPITuners();

        if (isEnabledAnalog2InForSp) {
            CcuLog.d(L.TAG_CCU_ZONE, "Use analog 2 offset , dynamicTargetDomainName: " + dynamicTargetDomainName);
            targetValue = plcEquip.getDynamicTargetValue().readDefaultVal();
        }
        CcuLog.d(L.TAG_CCU_ZONE, "PlcProfile, processVariable: "+processVariableDomainName+" : "
                + processVariable + ", targetValue: " + targetValue);

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

        String statusMessage = getStatusMessage(eStatus);
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
     */
    private String getStatusMessage(int outputSignal) {

        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("Loop output is ").append(outputSignal > 0 ? "active" : "inactive");
        if (plcEquip.getRelay1OutputEnable().readDefaultVal() > 0) {
            statusBuilder.append(", Relay1 ").append(plcEquip.getRelay1Cmd().readHisVal() > 0 ? "ON" : "OFF");
        }
        if (plcEquip.getRelay2OutputEnable().readDefaultVal() > 0) {
            statusBuilder.append(", Relay2 ").append(plcEquip.getRelay2Cmd().readHisVal() > 0 ? "ON" : "OFF");
        }

        return statusBuilder.toString();
    }

    /**
     * Update the relay output status based on current loop output and threshold configurations.
     *
     * @param loopOp
     */
    private void handleRelayOp(double loopOp) {

        boolean relay1OnStatus = plcEquip.getRelay1OutputEnable().readDefaultVal() > 0 &&
                            loopOp > plcEquip.getRelay1OnThreshold().readDefaultVal() &&
                            loopOp < plcEquip.getRelay1OffThreshold().readDefaultVal();
        plcEquip.getRelay1Cmd().writePointValue(relay1OnStatus ? 1 : 0);

        boolean relay2OnStatus = plcEquip.getRelay2OutputEnable().readDefaultVal() > 0 &&
                            loopOp > plcEquip.getRelay2OnThreshold().readDefaultVal() &&
                            loopOp < plcEquip.getRelay2OffThreshold().readDefaultVal();
        plcEquip.getRelay2Cmd().writePointValue(relay2OnStatus ? 1 : 0);
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
        boolean isSmartNode = CCUHsApi.getInstance().readMapById(equipRef).containsKey(Tags.SMART_NODE);
        ModelDirective model = isSmartNode ? ModelLoader.INSTANCE.getSmartNodePidModel()
                                    : ModelLoader.INSTANCE.getHelioNodePidModel();
        processVariableDomainName = getProcessVariableMappedPoint(plcEquip, model);
        if (isEnabledAnalog2InForSp) {
            dynamicTargetDomainName = getDynamicTargetPoint(plcEquip, model);
        }
        CcuLog.i(L.TAG_CCU_ZONE, "PLC initialized : proportionalGain " + plc.getProportionalGain() + ", integralGain " + plc.getIntegralGain() +
                ", proportionalSpread " + plc.getMaxAllowedError() + ", integralMaxTimeout " + plc.getIntegralMaxTimeout());

        //TODO - TEMP code for debugging an issue where proportionalRange is set to 0
        if (plc.getMaxAllowedError() == 0) {
            CcuLog.e(L.TAG_CCU_ZONE, "PLC initialized with zero proportional spread. Please check the configuration. ");
            CcuLog.e(L.TAG_CCU_ZONE, " Point "+plcEquip.getPidProportionalRange().getPoint()+" val "+plcEquip.getPidProportionalRange().readPriorityVal());
        }
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

    @Override
    public ProfileConfiguration getDomainProfileConfiguration() {
        Equip equip = getEquip();
        NodeType nodeType = equip.getDomainName().contains("helionode") ? NodeType.HELIO_NODE : NodeType.SMART_NODE;
        return new PlcProfileConfig(nodeAddr, nodeType.name(),
                0,
                equip.getRoomRef(),
                equip.getFloorRef() ,
                ProfileType.PLC,
                (SeventyFiveFProfileDirective) ModelLoader.INSTANCE.getModelForDomainName(equip.getDomainName()))
                .getActiveConfiguration();

    }

    public String getProcessVariableDomainName() {
        return processVariableDomainName;
    }

    public String getDynamicTargetDomainName() {
        return dynamicTargetDomainName;
    }

    private void updateSensorLinkedLogicalPoints() {
        if (StringUtils.isNotEmpty(processVariableDomainName)) {
            double processVariableSensorVal = CCUHsApi.getInstance().readHisValByQuery("point and equipRef == \"" + equipRef + "\" and domainName == \"" + processVariableDomainName + "\"");
            plcEquip.getProcessVariable().writePointValue(processVariableSensorVal);
        }

        if (isEnabledAnalog2InForSp && StringUtils.isNotEmpty(dynamicTargetDomainName)) {
            double dynamicTargetSensorVal = CCUHsApi.getInstance().readHisValByQuery("point and equipRef == \"" + equipRef + "\" and domainName == \"" + dynamicTargetDomainName + "\"");
            plcEquip.getDynamicTargetValue().writePointValue(dynamicTargetSensorVal + spSensorOffset);
        }

    }
}
