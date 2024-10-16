package a75f.io.messaging.handler;

import static a75f.io.messaging.handler.AdvanceAhuReconfigHandlerKt.isAdvanceAhuV2Profile;
import static a75f.io.messaging.handler.AdvanceAhuReconfigHandlerKt.reconfigureAdvanceAhuV2;

import com.google.gson.JsonObject;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.logic.DeviceBuilder;
import a75f.io.domain.logic.DomainManager;
import a75f.io.domain.logic.EntityMapper;
import a75f.io.domain.logic.ProfileEquipBuilder;
import a75f.io.domain.util.ModelLoader;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.SystemProfile;
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtuWithVfd;
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu;
import a75f.io.logic.bo.building.system.vav.VavAdvancedHybridRtu;
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu;
import a75f.io.logic.bo.building.system.vav.VavIERtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtuWithVfd;
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig;
import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig;
import a75f.io.logic.bo.building.system.vav.config.StagedVfdRtuProfileConfig;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.tuners.TunerUtil;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective;

/**
 * Handles remote config updates specific to System profile.
 */
class ConfigPointUpdateHandler {
    
    public static void updateConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigPoint "+msgObject.toString());
        if (configPoint.getMarkers().contains(Tags.IE)) {
            updateIEConfig(msgObject, configPoint, hayStack);
        }else if (configPoint.getMarkers().contains(Tags.ENABLED) || configPoint.getMarkers().contains(Tags.ENABLE)) {
            updateConfigEnabled(msgObject, configPoint, hayStack);
        } else if ((configPoint.getMarkers().contains(Tags.ASSOCIATION) )
            || configPoint.getMarkers().contains(Tags.HUMIDIFIER)) {
            updateConfigAssociation(msgObject, configPoint, hayStack);
        } else if (isAdvanceAhuV2Profile()) {
            reconfigureAdvanceAhuV2(msgObject, configPoint);
        }
    }
    
    private static void updateConfigEnabled(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigEnabled "+configPoint.getDisplayName());
        updatePhysicalConfigEnabled(msgObject, configPoint, hayStack);
        writePointFromJson(configPoint.getId(), msgObject, hayStack);
        updateConditioningMode();
    }
    
    private static void updateIEConfig(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateIEConfig "+configPoint.getDisplayName());
        VavIERtu systemProfile =  (VavIERtu) L.ccu().systemProfile;
        double val = msgObject.get("val").getAsDouble();
        if (configPoint.getMarkers().contains(Tags.MULTI_ZONE)) {
            systemProfile.handleMultiZoneEnable(val);
        } else {
            String userIntent = getUserIntentType(configPoint);
            if (userIntent != null &&
                    configPoint.getMarkers().contains(Tags.ENABLED)) {
                systemProfile.setConfigEnabled(userIntent, val);
            }
        }
        writePointFromJson(configPoint.getId(), msgObject, hayStack);
    }
    
    /**
     * Creates occupancy and humidifier logical points when config is enabled.
     * @param msgObject
     * @param configPoint
     */
    private static void updatePhysicalConfigEnabled(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        String configType = getOutputTagFromConfig(configPoint);
        if (configType == null) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Invalid config point update "+configPoint);
        }

        SystemProfile systemProfile = L.ccu().systemProfile;
        double val = msgObject.get("val").getAsDouble();



        if (systemProfile instanceof DabFullyModulatingRtu) {
            ((DabFullyModulatingRtu) systemProfile).setConfigEnabled(configType, val);
        } else if (systemProfile instanceof DabStagedRtu) {

            boolean isVfd = systemProfile instanceof DabStagedRtuWithVfd;
            SeventyFiveFProfileDirective model = (SeventyFiveFProfileDirective) (isVfd ? ModelLoader.INSTANCE.getDabStagedVfdRtuModelDef()
                    :ModelLoader.INSTANCE.getDabStageRtuModelDef());
            SeventyFiveFDeviceDirective deviceModel = (SeventyFiveFDeviceDirective) ModelLoader.INSTANCE.getCMDeviceModel();
            StagedRtuProfileConfig config = isVfd ? new StagedVfdRtuProfileConfig(model).getActiveConfiguration()
                    :new StagedRtuProfileConfig(model).getActiveConfiguration();

            if (configPoint.getDomainName().contains(DomainName.relay1OutputEnable)) {
                config.relay1Enabled.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay2OutputEnable)) {
                config.relay2Enabled.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay3OutputEnable)) {
                config.relay3Enabled.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay4OutputEnable)) {
                config.relay4Enabled.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay5OutputEnable)) {
                config.relay5Enabled.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay6OutputEnable)) {
                config.relay6Enabled.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay7OutputEnable)) {
                config.relay7Enabled.setEnabled(val > 0);
            }
            if (isVfd && configPoint.getDomainName().contains(DomainName.analog2OutputEnable)) {
                StagedVfdRtuProfileConfig vfdRtuProfileConfig = (StagedVfdRtuProfileConfig) config;
                vfdRtuProfileConfig.analogOut2Enabled.setEnabled(val > 0);
            }

            CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigAssociation dabStagedRtu"+ config);
            ProfileEquipBuilder equipBuilder = new ProfileEquipBuilder(hayStack);
            EntityMapper entityMapper = new EntityMapper(model);
            DeviceBuilder deviceBuilder = new DeviceBuilder(hayStack, entityMapper);
            HashMap<Object, Object> systemEquip = hayStack.readMapById(Domain.systemEquip.getEquipRef());
            equipBuilder.updateEquipAndPoints(config, model, hayStack.getSite().getId(),systemEquip.get("dis").toString() , true);
            deviceBuilder.updateDeviceAndPoints(config, deviceModel, Domain.systemEquip.getEquipRef(), hayStack.getSite().getId(), hayStack.getSiteName() + "-" + deviceModel.getName());
            DomainManager.INSTANCE.addSystemDomainEquip(hayStack);
            removeWritableTagFromCMDevicePort(configPoint, hayStack, val);

        } else if (systemProfile instanceof VavFullyModulatingRtu) {
            SeventyFiveFProfileDirective model = (SeventyFiveFProfileDirective) ModelLoader.INSTANCE.getVavModulatingRtuModelDef();
            ModulatingRtuProfileConfig config = new ModulatingRtuProfileConfig(model).getActiveConfiguration();
            if (configPoint.getDomainName().contains(DomainName.analog1OutputEnable)) {
                config.analog1OutputEnable.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.analog2OutputEnable)) {
                config.analog2OutputEnable.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.analog3OutputEnable)) {
                config.analog3OutputEnable.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.analog4OutputEnable)) {
                config.analog4OutputEnable.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay3OutputEnable)) {
                config.relay3OutputEnable.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay7OutputEnable)) {
                config.relay7OutputEnable.setEnabled(val > 0);
            }
            CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigPoint for VavFullyModulatingAhu" + config);
            ProfileEquipBuilder equipBuilder = new ProfileEquipBuilder(hayStack);
            HashMap<Object, Object> systemEquip = hayStack.readMapById(Domain.systemEquip.getEquipRef());
            equipBuilder.updateEquipAndPoints(config, model, hayStack.getSite().getId(), systemEquip.get("dis").toString(), true);
            DomainManager.INSTANCE.addSystemDomainEquip(hayStack);
            removeWritableTagFromCMDevicePort(configPoint, hayStack, val);
        } else if (systemProfile instanceof VavStagedRtu) {

            if (systemProfile instanceof VavAdvancedHybridRtu) {
                ((VavStagedRtu) systemProfile).setConfigEnabled(configType, val);
                return;
            }

            boolean isVfd = systemProfile instanceof VavStagedRtuWithVfd;
            SeventyFiveFProfileDirective model = (SeventyFiveFProfileDirective) (isVfd ? ModelLoader.INSTANCE.getVavStagedVfdRtuModelDef()
                                                        :ModelLoader.INSTANCE.getVavStageRtuModelDef());
            StagedRtuProfileConfig config = isVfd ? new StagedVfdRtuProfileConfig(model).getActiveConfiguration()
                                                :new StagedRtuProfileConfig(model).getActiveConfiguration();

            if (configPoint.getDomainName().contains(DomainName.relay1OutputEnable)) {
                config.relay1Enabled.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay2OutputEnable)) {
                config.relay2Enabled.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay3OutputEnable)) {
                config.relay3Enabled.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay4OutputEnable)) {
                config.relay4Enabled.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay5OutputEnable)) {
                config.relay5Enabled.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay6OutputEnable)) {
                config.relay6Enabled.setEnabled(val > 0);
            } else if (configPoint.getDomainName().contains(DomainName.relay7OutputEnable)) {
                config.relay7Enabled.setEnabled(val > 0);
            }
            if (isVfd && configPoint.getDomainName().contains(DomainName.analog2OutputEnable)) {
                StagedVfdRtuProfileConfig vfdRtuProfileConfig = (StagedVfdRtuProfileConfig) config;
                vfdRtuProfileConfig.analogOut2Enabled.setEnabled(val > 0);
            }
            CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigAssociation vavStagedRtu"+ config);
            ProfileEquipBuilder equipBuilder = new ProfileEquipBuilder(hayStack);
            HashMap<Object, Object> systemEquip = hayStack.readMapById(Domain.systemEquip.getEquipRef());
            equipBuilder.updateEquipAndPoints(config, model, hayStack.getSite().getId(),systemEquip.get("dis").toString() , true);
            DomainManager.INSTANCE.addSystemDomainEquip(hayStack);
            removeWritableTagFromCMDevicePort(configPoint, hayStack, val);
        } else if (isAdvanceAhuV2Profile()) {
            reconfigureAdvanceAhuV2(msgObject, configPoint);
        }

    }

     static void removeWritableTagFromCMDevicePort(Point configPoint, CCUHsApi hayStack, double val) {
        RawPoint cmDevicePort = Domain.cmBoardDevice.getPortsDomainNameWithPhysicalPoint().get(
                ControlMote.getSystemEquipPointsDomainNameWithCmPortsDomainName().get(configPoint.getDomainName()));
        if(cmDevicePort != null  && val == 1 && cmDevicePort.getMarkers().contains(Tags.WRITABLE)){
            CcuLog.d(L.TAG_CCU_PUBNUB,"remove Writable Tag From CMDevicePort "+cmDevicePort.getDisplayName());
            hayStack.clearAllAvailableLevelsInPoint(cmDevicePort.getId());
            cmDevicePort.getMarkers().remove(Tags.WRITABLE);
            hayStack.writeHisValById(cmDevicePort.getId(), 0.0);
            hayStack.updatePoint(cmDevicePort, cmDevicePort.getId());
        }
    }

    private static void updateConfigAssociation(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigAssociation "+configPoint.getDisplayName());
        
        String relayType = getOutputTagFromConfig(configPoint);
        if (relayType == null) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Invalid config point update "+configPoint.toString());
        }
        
        SystemProfile systemProfile = L.ccu().systemProfile;
        double val = msgObject.get("val").getAsDouble();
        
        if (systemProfile instanceof DabFullyModulatingRtu) {
            ((DabFullyModulatingRtu) systemProfile).setHumidifierConfigVal(relayType+" and humidifier and type", val);
        } else if (systemProfile instanceof DabStagedRtu) {
            boolean isVfd = systemProfile instanceof DabStagedRtuWithVfd;
            SeventyFiveFProfileDirective equipModel = (SeventyFiveFProfileDirective) (isVfd ? ModelLoader.INSTANCE.getDabStagedVfdRtuModelDef()
                    :ModelLoader.INSTANCE.getDabStageRtuModelDef());
            SeventyFiveFDeviceDirective deviceModel = (SeventyFiveFDeviceDirective) ModelLoader.INSTANCE.getCMDeviceModel();
            StagedRtuProfileConfig config = isVfd ? new StagedVfdRtuProfileConfig(equipModel).getActiveConfiguration()
                    :new StagedRtuProfileConfig(equipModel).getActiveConfiguration();

            if (configPoint.getDomainName().contains(DomainName.relay1OutputAssociation)) {
                config.relay1Association.setAssociationVal((int) val);
            } else if (configPoint.getDomainName().contains(DomainName.relay2OutputAssociation)) {
                config.relay2Association.setAssociationVal((int) val);
            } else if (configPoint.getDomainName().contains(DomainName.relay3OutputAssociation)) {
                config.relay3Association.setAssociationVal((int) val);
            } else if (configPoint.getDomainName().contains(DomainName.relay4OutputAssociation)) {
                config.relay4Association.setAssociationVal((int) val);
            } else if (configPoint.getDomainName().contains(DomainName.relay5OutputAssociation)) {
                config.relay5Association.setAssociationVal((int) val);
            } else if (configPoint.getDomainName().contains(DomainName.relay6OutputAssociation)) {
                config.relay6Association.setAssociationVal((int) val);
            } else if (configPoint.getDomainName().contains(DomainName.relay7OutputAssociation)) {
                config.relay7Association.setAssociationVal((int) val);
            }
            ProfileEquipBuilder equipBuilder = new ProfileEquipBuilder(hayStack);
            EntityMapper entityMapper = new EntityMapper(equipModel);
            DeviceBuilder deviceBuilder = new DeviceBuilder(hayStack, entityMapper);
            HashMap<Object, Object> systemEquip = hayStack.readMapById(Domain.systemEquip.getEquipRef());
            equipBuilder.updateEquipAndPoints(config, equipModel, hayStack.getSite().getId(),systemEquip.get("dis").toString() , true);
            deviceBuilder.updateDeviceAndPoints(config, deviceModel, Domain.systemEquip.getEquipRef(), hayStack.getSite().getId(), hayStack.getSiteName() + "-" + deviceModel.getName());
            DomainManager.INSTANCE.addSystemDomainEquip(hayStack);
        } else if (systemProfile instanceof VavFullyModulatingRtu) {
            SeventyFiveFProfileDirective model = (SeventyFiveFProfileDirective) ModelLoader.INSTANCE.getVavModulatingRtuModelDef();
            ModulatingRtuProfileConfig config = new ModulatingRtuProfileConfig(model).getActiveConfiguration();
            if (configPoint.getDomainName().contains(DomainName.relay7OutputAssociation)) {
                config.relay7Association.setAssociationVal((int) val);
            }
            ProfileEquipBuilder equipBuilder = new ProfileEquipBuilder(hayStack);
            HashMap<Object, Object> systemEquip = hayStack.readMapById(Domain.systemEquip.getEquipRef());
            equipBuilder.updateEquipAndPoints(config, model, hayStack.getSite().getId(),systemEquip.get("dis").toString() , true);
            DomainManager.INSTANCE.addSystemDomainEquip(hayStack);
        } else if (systemProfile instanceof VavStagedRtu) {

            if(systemProfile instanceof VavAdvancedHybridRtu){
                ((VavStagedRtu) systemProfile).setConfigAssociation(relayType, val);
                return;
            }
            boolean isVfd = systemProfile instanceof VavStagedRtuWithVfd;
            SeventyFiveFProfileDirective model = (SeventyFiveFProfileDirective) (isVfd ? ModelLoader.INSTANCE.getVavStagedVfdRtuModelDef()
                    :ModelLoader.INSTANCE.getVavStageRtuModelDef());
            StagedRtuProfileConfig config = isVfd ? new StagedVfdRtuProfileConfig(model).getActiveConfiguration()
                    :new StagedRtuProfileConfig(model).getActiveConfiguration();

            if (configPoint.getDomainName().contains(DomainName.relay1OutputAssociation)) {
                config.relay1Association.setAssociationVal((int) val);
            } else if (configPoint.getDomainName().contains(DomainName.relay2OutputAssociation)) {
                config.relay2Association.setAssociationVal((int) val);
            } else if (configPoint.getDomainName().contains(DomainName.relay3OutputAssociation)) {
                config.relay3Association.setAssociationVal((int) val);
            } else if (configPoint.getDomainName().contains(DomainName.relay4OutputAssociation)) {
                config.relay4Association.setAssociationVal((int) val);
            } else if (configPoint.getDomainName().contains(DomainName.relay5OutputAssociation)) {
                config.relay5Association.setAssociationVal((int) val);
            } else if (configPoint.getDomainName().contains(DomainName.relay6OutputAssociation)) {
                config.relay6Association.setAssociationVal((int) val);
            } else if (configPoint.getDomainName().contains(DomainName.relay7OutputAssociation)) {
                config.relay7Association.setAssociationVal((int) val);
            }
            ProfileEquipBuilder equipBuilder = new ProfileEquipBuilder(hayStack);
            HashMap<Object, Object> systemEquip = hayStack.readMapById(Domain.systemEquip.getEquipRef());
            equipBuilder.updateEquipAndPoints(config, model, hayStack.getSite().getId(),systemEquip.get("dis").toString() , true);
            DomainManager.INSTANCE.addSystemDomainEquip(hayStack);
        } else if (L.ccu().systemProfile instanceof DabAdvancedAhu
                || L.ccu().systemProfile instanceof VavAdvancedAhu) {
            reconfigureAdvanceAhuV2(msgObject, configPoint);
        }
        writePointFromJson(configPoint.getId(), msgObject, hayStack);
        updateConditioningMode();
    }

    
    /***
     * When cooling/heating is disabled remotely via reconfiguration, and the system cannot operate any more
     * in the selected conditioning mode, we should turn off the system.
     */
    public static void updateConditioningMode() {
        SystemProfile systemProfile = L.ccu().systemProfile;
        if (systemProfile instanceof DabStagedRtu) {
            ((DabStagedRtu) systemProfile).updateStagesSelected();
        } else if (systemProfile instanceof DabStagedRtuWithVfd) {
            ((DabStagedRtuWithVfd) systemProfile).updateStagesSelected();
        }

        SystemMode systemMode = SystemMode.values()[(int) HSUtil.getSystemUserIntentVal("conditioning and mode")];
        if (systemMode == SystemMode.OFF) {
            return;
        }

        if ((systemMode == SystemMode.AUTO && (!systemProfile.isCoolingAvailable() || !systemProfile.isHeatingAvailable()))
            || (systemMode == SystemMode.COOLONLY && !systemProfile.isCoolingAvailable())
            || (systemMode == SystemMode.HEATONLY && !systemProfile.isHeatingAvailable())) {
            
            CcuLog.i(L.TAG_CCU_PUBNUB, "Reconfig disabling conditioning mode !");
            TunerUtil.writeSystemUserIntentVal("conditioning and mode", SystemMode.OFF.ordinal());
        }
    }
    
    private static void writePointFromJson(String id, JsonObject msgObject, CCUHsApi hayStack) {
        String who = msgObject.get("who").getAsString();
        double val = msgObject.get("val").getAsDouble();
        int duration = msgObject.get("duration") != null ? msgObject.get("duration").getAsInt() : 0;
        int level = msgObject.get("level").getAsInt();
        hayStack.writePointLocal(id, level, who, val, duration);
    }

    private static String getOutputTagFromConfig(Point configPoint) {
        
        if (configPoint.getMarkers().contains(Tags.ANALOG1)) {
            return Tags.ANALOG1;
        } else if (configPoint.getMarkers().contains(Tags.ANALOG2)) {
            return Tags.ANALOG2;
        } else if (configPoint.getMarkers().contains(Tags.ANALOG3)) {
            return Tags.ANALOG3;
        } else if (configPoint.getMarkers().contains(Tags.ANALOG4)) {
            return Tags.ANALOG4;
        } else if (configPoint.getMarkers().contains(Tags.RELAY1)) {
            return Tags.RELAY1;
        } else if (configPoint.getMarkers().contains(Tags.RELAY2)) {
            return Tags.RELAY2;
        } else if (configPoint.getMarkers().contains(Tags.RELAY3)) {
            return Tags.RELAY3;
        } else if (configPoint.getMarkers().contains(Tags.RELAY4)) {
            return Tags.RELAY4;
        } else if (configPoint.getMarkers().contains(Tags.RELAY5)) {
            return Tags.RELAY5;
        } else if (configPoint.getMarkers().contains(Tags.RELAY6)) {
            return Tags.RELAY6;
        } else if (configPoint.getMarkers().contains(Tags.RELAY7)) {
            return Tags.RELAY7;
        }
        return null;
    }
    
    private static String getUserIntentType(Point configPoint) {
        if (configPoint.getMarkers().contains(Tags.COOLING)) {
            return Tags.COOLING;
        } else if (configPoint.getMarkers().contains(Tags.HEATING)) {
            return Tags.HEATING;
        } else if (configPoint.getMarkers().contains(Tags.FAN)) {
            return Tags.FAN;
        }
        return null;
    }

}

