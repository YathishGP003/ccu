package a75f.io.messaging.handler;

import static a75f.io.logic.bo.building.dab.DabReheatPointsKt.resetAO2ToSecondaryDamper;
import static a75f.io.logic.bo.building.dab.DabReheatPointsKt.updateReheatType;

import com.google.gson.JsonObject;

import java.util.HashMap;

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
import a75f.io.logic.bo.haystack.device.DeviceUtil;
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
                double reheatType = hayStack.readDefaultVal("reheat and type and group == \""+configPoint.getGroup()+"\"");
                if (reheatType == 0 || (reheatType - 1) > ReheatType.Pulse.ordinal()  ) {
                    //When reheat is mapped to AO2 , we cant use it.
                    SmartNode.updatePhysicalPointType(address, Port.ANALOG_OUT_TWO.toString(),
                            DamperType.values()[typeVal].displayName);
                    SmartNode.setPointEnabled(address, Port.ANALOG_OUT_TWO.toString(), true);

                    if (typeVal < DamperType.MAT.ordinal()) {
                        HashMap<Object, Object> normalizedSecDamper = hayStack
                                .readEntity("normalized and secondary and damper and cmd and equipRef ==\""+configPoint.getEquipRef()+"\"");
                        CcuLog.i("CCU_ZONE", normalizedSecDamper.toString());
                        if (!normalizedSecDamper.isEmpty()) {
                            DeviceUtil.updatePhysicalPointRef(Integer.parseInt(configPoint.getGroup()),
                                    Port.ANALOG_OUT_TWO.name(), normalizedSecDamper.get("id").toString());

                        }
                    }
                }
            }
        } else if (configPoint.getMarkers().contains(Tags.REHEAT) && configPoint.getMarkers().contains(Tags.DAB)) {
            updateReheatType(typeVal, 40, configPoint.getEquipRef(), hayStack);
            if (typeVal == 0) {
                SmartNode.setPointEnabled(address, Port.RELAY_ONE.name(), false );
                SmartNode.setPointEnabled(address, Port.RELAY_TWO.name(), false );
            } else if ((typeVal - 1) <= ReheatType.Pulse.ordinal()) {
                //Modulating Reheat -> Enable AnalogOut2 and disable relays
                SmartNode.updatePhysicalPointType(address, Port.ANALOG_OUT_TWO.toString(), ReheatType.values()[typeVal-1].displayName);
                SmartNode.setPointEnabled(address, Port.ANALOG_OUT_TWO.toString(), true);
                SmartNode.setPointEnabled(address, Port.RELAY_ONE.name(), false );
                SmartNode.setPointEnabled(address, Port.RELAY_TWO.name(), false );
            } else {
                SmartNode.updatePhysicalPointType(address, Port.RELAY_ONE.toString(),
                        OutputRelayActuatorType.NormallyClose.displayName);
                SmartNode.setPointEnabled(address, Port.RELAY_ONE.toString(), true);
                if ((typeVal - 1) == ReheatType.TwoStage.ordinal()) {
                    SmartNode.updatePhysicalPointType(address, Port.RELAY_TWO.toString(), OutputRelayActuatorType.NormallyClose.displayName);
                    SmartNode.setPointEnabled(address, Port.RELAY_TWO.toString(), true);
                } else {
                    SmartNode.setPointEnabled(address, Port.RELAY_TWO.toString(), false);
                }

                double damperType = hayStack.readDefaultVal("secondary and damper and type and group == \""+configPoint.getGroup()+"\"");
                if (damperType == DamperType.MAT.ordinal()) {
                    SmartNode.setPointEnabled(address, Port.ANALOG_OUT_TWO.toString(), true);
                    SmartNode.updatePhysicalPointType(address, Port.ANALOG_OUT_TWO.toString(),
                                                                    DamperType.MAT.displayName);
                }
            }
        } else if (configPoint.getMarkers().contains(Tags.DAMPER) && configPoint.getMarkers().contains(Tags.VAV)) {
            SmartNode.updatePhysicalPointType(address, Port.ANALOG_OUT_ONE.toString(),
                                              DamperType.values()[typeVal].displayName);
        } else if (configPoint.getMarkers().contains(Tags.REHEAT) && configPoint.getMarkers().contains(Tags.VAV)) {

            if (typeVal < 0) {
                SmartNode.setPointEnabled(address, Port.ANALOG_OUT_TWO.toString(), false);
            } else if (typeVal <= ReheatType.Pulse.ordinal()) {
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
