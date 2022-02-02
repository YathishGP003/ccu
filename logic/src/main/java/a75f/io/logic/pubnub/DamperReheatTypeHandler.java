package a75f.io.logic.pubnub;

import com.google.gson.JsonObject;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.DamperType;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ReheatType;
import a75f.io.logic.bo.haystack.device.SmartNode;

public class DamperReheatTypeHandler {
    
    public static void updatePoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
    
        CcuLog.i(L.TAG_CCU_PUBNUB,
                 "update ["+configPoint+" ]: " + msgObject.toString());
        
        int address = Integer.parseInt(configPoint.getGroup());
        int typeVal = msgObject.get("val").getAsInt();
        
        if (configPoint.getMarkers().contains(Tags.DAMPER) && configPoint.getMarkers().contains(Tags.DAB)) {
            if (configPoint.getMarkers().contains(Tags.PRIMARY)) {
                SmartNode.updatePhysicalPointType(address, Port.ANALOG_OUT_ONE.toString(),
                                                  DamperType.values()[typeVal].displayName);
            } else if (configPoint.getMarkers().contains(Tags.SECONDARY)) {
                SmartNode.updatePhysicalPointType(address, Port.ANALOG_OUT_TWO.toString(),
                                                  DamperType.values()[typeVal].displayName);
            }
        } else if (configPoint.getMarkers().contains(Tags.DAMPER) && configPoint.getMarkers().contains(Tags.VAV)) {
            SmartNode.updatePhysicalPointType(address, Port.ANALOG_OUT_ONE.toString(),
                                              DamperType.values()[typeVal].displayName);
        } else if (configPoint.getMarkers().contains(Tags.REHEAT) && configPoint.getMarkers().contains(Tags.VAV)) {
            
            if (typeVal <= ReheatType.Pulse.ordinal()) {
                //Modulating Reheat -> Enable AnalogOut2 and disable relays
                SmartNode.updatePhysicalPointType(address, Port.ANALOG_OUT_TWO.toString(), ReheatType.values()[typeVal].displayName);
                SmartNode.setPointEnabled(address, Port.ANALOG_OUT_TWO.toString(), true);
    
                SmartNode.setPointEnabled(address, Port.RELAY_ONE.name(), false );
                
            } else {
                SmartNode.setPointEnabled(address, Port.ANALOG_OUT_TWO.name(), false);
                SmartNode.updatePhysicalPointType(address, Port.RELAY_ONE.toString(),
                                                  OutputRelayActuatorType.NormallyClose.displayName);
                SmartNode.setPointEnabled(address, Port.RELAY_ONE.toString(), true);
                if (typeVal == ReheatType.TwoStage.ordinal()) {
                    SmartNode.updatePhysicalPointType(address, Port.RELAY_TWO.toString(), OutputRelayActuatorType.NormallyClose.displayName);
                    SmartNode.setPointEnabled(address, Port.RELAY_TWO.toString(), true);
                }
            }
        }
    
        String who = msgObject.get("who").getAsString();
        int duration = msgObject.get("duration") != null ? msgObject.get("duration").getAsInt() : 0;
        int level = msgObject.get("level").getAsInt();
        hayStack.writePointLocal(configPoint.getId(), level, who, (double) typeVal, duration);
    }
}
