package a75f.io.logic.bo.building.plc;

import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;

/**
 * Created by samjithsadasivan on 2/25/19.
 */

public class PlcProfile extends ZoneProfile
{
    PlcEquip plcEquip;
    int outputSignal = 0;
    
    public void addPlcEquip(short addr, PlcProfileConfiguration config, String floorRef, String roomRef, String processVariable, String dynamicTargetTag) {
        plcEquip = new PlcEquip(getProfileType(), addr);
        plcEquip.createEntities(config, floorRef, roomRef, processVariable, dynamicTargetTag);
        plcEquip.init();
    }
    
    public void addPlcEquip(short addr) {
        plcEquip = new PlcEquip(getProfileType(), addr);
        plcEquip.init();
    }
    
    public void updatePlcEquip(PlcProfileConfiguration config, String floorRef, String zoneRef, String processTag, String dynamicTargetTag) {
        plcEquip.update(config,floorRef,zoneRef,processTag, dynamicTargetTag);
        plcEquip.init();
    }
    
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.PLC;
    }
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return plcEquip.getProfileConfiguration();
    }
    
    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short)plcEquip.nodeAddr);
        }};
    }
    
    @Override
    public Equip getEquip()
    {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+plcEquip.nodeAddr+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }
    
    @Override
    public void updateZonePoints() {
    
        double processVariable = plcEquip.getProcessVariable();
        double targetValue = plcEquip.getTargetValue();
        double controlVariable;
        
        if (plcEquip.isEnabledAnalog2InForSp()) {
            Log.d(L.TAG_CCU_ZONE,"Use analog 2 offset "+plcEquip.getSpVariable());
            targetValue = plcEquip.getSpVariable();
        }
        
        double controlLoopInversion = plcEquip.getConfigNumVal("loop and inversion");
        
        if (controlLoopInversion > 0) {
            plcEquip.getPIController().updateControlVariable(processVariable, targetValue);
        } else {
            plcEquip.getPIController().updateControlVariable(targetValue, processVariable);
        }
        
        if (plcEquip.isEnabledZeroErrorMidpoint()) {
            controlVariable = 50.0 + plcEquip.getPIController().getControlVariable() * 50.0 / plcEquip.getPIController().getMaxAllowedError();
        } else {
            //Get only the 0-100% portion of cv
            controlVariable = plcEquip.getPIController().getControlVariable() * 100.0 / plcEquip.getPIController().getMaxAllowedError();
            if (controlVariable < 0) {
                controlVariable = 0;
            }
        }
        double curCv = Math.round(100*controlVariable)/100;
        int eStatus = (int)(Math.round(100*controlVariable)/100);
        if(plcEquip.getControlVariable() != curCv )
            plcEquip.setControlVariable(curCv);
        plcEquip.setEquipStatus(getStatusMessage(plcEquip.equipRef, eStatus, CCUHsApi.getInstance()));
        outputSignal = eStatus;
    
        handleRelayOp(Tags.RELAY1, curCv);
        handleRelayOp(Tags.RELAY2, curCv);
        
        plcEquip.getPIController().dump();
        Log.d(L.TAG_CCU_ZONE, "PlcProfile, processVariable: "+processVariable+", targetValue: "+targetValue+", controlVariable: "+controlVariable);
    }
    
    /**
     * Generate a PI Loop Status Message.
     * Defer to CCU-UI spec of PI Profile for the format of this message.
     * @param equipID
     * @param outputSignal
     * @param hayStack
     * @return
     */
    private String getStatusMessage(String equipID, int outputSignal, CCUHsApi hayStack) {
        
        double relay1Config = hayStack.readDefaultVal("point and relay1 and config and" +
                                                                    " enabled and equipRef == \""+equipID+"\"");
        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("Loop output is " + (outputSignal > 0 ? "active" : "inactive"));
        if (relay1Config > Math.abs(0.01)) {
            double relay1Status = hayStack.readHisValByQuery("point and relay1 and cmd and equipRef" +
                                                                           " == \""+equipID+"\"");
            statusBuilder.append(", Relay1 "+(relay1Status > Math.abs(0.01) ? "ON":"OFF"));
        }
        double relay2Config = hayStack.readDefaultVal("point and relay2 and config and" +
                                                                    " enabled and equipRef == \""+equipID+"\"");
    
        if (relay2Config > Math.abs(0.01)) {
            double relay2Status = hayStack.readHisValByQuery("point and relay2 and cmd and equipRef" +
                                                                           " == \""+equipID+"\"");
            statusBuilder.append(", Relay2 "+(relay2Status > Math.abs(0.01) ? "ON":"OFF"));
        }
    
        return statusBuilder.toString();
    }
    
    /**
     * Update the relay output status based on current loop output and threshold configurations.
     * @param relay
     * @param loopOp
     */
    private void handleRelayOp(String relay, double loopOp) {
    
        double relayConfig = plcEquip.getConfigNumVal(relay+" and enabled");
        if (Math.abs(relayConfig) < 0.01) {
            plcEquip.setCmdVal(relay, 0);
            return;
        }
        
        double relayOnThreshold = plcEquip.getConfigNumVal(relay+" and on and threshold");
        double relayOffThreshold = plcEquip.getConfigNumVal(relay+" and off and threshold");
    
        double relayStatus = plcEquip.getCmdVal(relay);
        
        if (loopOp > relayOnThreshold) {
            plcEquip.setCmdVal(relay, 1);
        } else if (relayStatus > 0 && loopOp < relayOffThreshold) {
            plcEquip.setCmdVal(relay, 0);
        }
        
    }
    
    
    @Override
    public void reset(){
        plcEquip.setControlVariable(0);
        plcEquip.setProcessVariable(0);
    }
    
}
