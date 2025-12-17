package a75f.io.messaging.handler;

import com.google.gson.JsonObject;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.api.PhysicalPoint;
import a75f.io.domain.logic.ProfileEquipBuilder;
import a75f.io.domain.util.ModelLoader;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.domain.logic.DeviceBuilder;
import a75f.io.domain.logic.EntityMapper;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.sse.SingleStageProfile;
import a75f.io.logic.bo.building.sse.SseEquipUtil;
import a75f.io.logic.bo.building.sse.SseProfileConfiguration;
import a75f.io.logic.bo.util.DesiredTempDisplayMode;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective;

class SSEConfigHandler {

    public static void updateConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        double val = msgObject.get("val").getAsDouble();
        if(val == CCUHsApi.getInstance().readDefaultValById(configPoint.getId())) {
            CcuLog.d(L.TAG_CCU_PUBNUB, "updateSSEConfigPoint - Message is not handled");
            return;
        }
        short address = Short.parseShort(configPoint.getGroup());
        SingleStageProfile profile = (SingleStageProfile) L.getProfile(address);
        assert profile != null;
        SseProfileConfiguration sseConfig = (SseProfileConfiguration) profile.getDomainProfileConfiguration();
        ProfileEquipBuilder equipBuilder = new ProfileEquipBuilder(hayStack);
        Equip equip = profile.getEquip();
        NodeType nodeType = equip.getDomainName().contains("helionode") ? NodeType.HELIO_NODE : NodeType.SMART_NODE;
        SeventyFiveFProfileDirective equipModel = (nodeType == NodeType.HELIO_NODE)
                ? (SeventyFiveFProfileDirective) ModelLoader.INSTANCE.getHelioNodeSSEModel()
                : (SeventyFiveFProfileDirective) ModelLoader.INSTANCE.getSmartNodeSSEModel();

        SeventyFiveFDeviceDirective deviceModel = (nodeType == NodeType.HELIO_NODE)
                ? (SeventyFiveFDeviceDirective) ModelLoader.INSTANCE.getSmartNodeDevice()
                : (SeventyFiveFDeviceDirective) ModelLoader.INSTANCE.getHelioNodeDevice();

        DeviceBuilder deviceBuilder = new DeviceBuilder(hayStack, new EntityMapper(equipModel));

        if(configPoint.getMarkers().contains(Tags.ENABLE)) {
            switch (configPoint.getDomainName()) {
                case DomainName.relay1OutputEnable:
                    sseConfig.relay1EnabledState.setEnabled(val > 0);
                    if(val == 0) {
                        String deviceId = hayStack.readId("device and addr == \"" + address + "\"");
                        (new PhysicalPoint(DomainName.relay1, deviceId)).writePointValue(0);
//                        Map<Object, Object> relay1Map = hayStack.readEntity("domainName==\"" + DomainName.relay1 + "\" and deviceRef == \"" + deviceId + "\"");
//                        hayStack.writePointValue(relay1Map, 0);
                    }
                    break;
                case DomainName.relay2OutputEnable:
                    sseConfig.relay2EnabledState.setEnabled(val > 0);
                    if(val == 0) {
                        String deviceId = hayStack.readId("device and addr == \"" + address + "\"");
                        (new PhysicalPoint(DomainName.relay2, deviceId)).writePointValue(0);
//                        Map<Object, Object> relay1Map = hayStack.readEntity("domainName==\"" + DomainName.relay2 + "\" and deviceRef == \"" + deviceId + "\"");
//                        hayStack.writePointValue(relay1Map, 0);
                    }
                    break;
                case DomainName.analog1InputEnable:
                    sseConfig.analog1InEnabledState.setEnabled(val > 0);
                    break;
                case DomainName.thermistor1InputEnable:
                    sseConfig.th1EnabledState.setEnabled(val > 0);
                    break;
                case DomainName.thermistor2InputEnable:
                    sseConfig.th2EnabledState.setEnabled(val > 0);
                    break;
            }
        }else if (configPoint.getMarkers().contains(Tags.ASSOCIATION)) {
            if(configPoint.getDomainName().equals(DomainName.relay1OutputAssociation)){
                sseConfig.relay1Association.setAssociationVal((int) val);
            }
            if(configPoint.getDomainName().equals(DomainName.relay2OutputAssociation)){
                sseConfig.relay2Association.setAssociationVal((int) val);
            }
            if(configPoint.getDomainName().equals(DomainName.analog1InputAssociation)){
                sseConfig.analog1InAssociation.setAssociationVal((int) val);
            }
        }
        equipBuilder.updateEquipAndPoints(sseConfig,
            ModelLoader.INSTANCE.getModelForDomainName(equip.getDomainName()),
            equip.getSiteRef(),
            equip.getDisplayName(), true);

        String deviceName = (nodeType == NodeType.HELIO_NODE) ? "-HN-" : "-SN-";
        deviceBuilder.updateDeviceAndPoints(sseConfig,
            deviceModel, equip.getId(),
                equip.getSiteRef(),
            hayStack.getSiteName() + deviceName + sseConfig.getNodeAddress(),
                null
        );
        SseEquipUtil.Companion.updatePortConfiguration(
                hayStack,
                sseConfig,
                deviceBuilder,
                deviceModel
        );
        writePointFromJson(configPoint, msgObject, hayStack);
        CCUHsApi.getInstance().scheduleSync();
    }

    public static void updateTemperatureMode(Point configPoint, CCUHsApi ccuHsApi) {
        if (configPoint.getDomainName().equals(DomainName.relay1OutputAssociation)) {
            DesiredTempDisplayMode.setModeType(configPoint.getRoomRef(), ccuHsApi);
        }
    }
    
    private static void writePointFromJson(Point configPoint, JsonObject msgObject, CCUHsApi hayStack) {
        try {
            String who = msgObject.get(HayStackConstants.WRITABLE_ARRAY_WHO).getAsString();
            int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
            double val = msgObject.get(HayStackConstants.WRITABLE_ARRAY_VAL).getAsDouble();
            double durationDiff = MessageUtil.Companion.returnDurationDiff(msgObject);
            hayStack.writePointLocal(configPoint.getId(), level, who, val, durationDiff);
            CcuLog.d(L.TAG_CCU_PUBNUB, "SSE: writePointFromJson - level: " + level + " who: " + who + " val: " + val +  " durationDiff: " + durationDiff);
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : "+msgObject+" ; "+e.getMessage());
        }
    }
}
