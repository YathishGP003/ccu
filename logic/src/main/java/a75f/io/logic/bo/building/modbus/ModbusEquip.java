package a75f.io.logic.bo.building.modbus;

import static a75f.io.logic.bo.building.pcn.PCNUtil.MODBUS_SLAVE_ID_LIMIT;
import static a75f.io.logic.bo.util.CustomScheduleUtilKt.updateWritableDataUponCustomControlChanges;
import static a75f.io.api.haystack.util.StringUtil.addAtSymbolIfMissing;
import static a75f.io.logic.util.NonModelPointUtilKt.addEquipScheduleStatusPoint;

import org.projecthaystack.HDict;
import org.projecthaystack.HNum;
import org.projecthaystack.HStr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.modbus.Command;
import a75f.io.api.haystack.modbus.Condition;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.LogicalPointTags;
import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.api.haystack.modbus.UserIntentPointTags;
import a75f.io.domain.util.CommonQueries;
import a75f.io.logger.CcuLog;
import a75f.io.logic.UtilKt;
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ScheduleType;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;
import kotlin.Pair;

public class ModbusEquip {
    ProfileType profileType;
    public short slaveId;
    String equipRef = null;
    public List<Parameter> configuredParams= new ArrayList<>();
    CCUHsApi hayStack = CCUHsApi.getInstance();
    private static final String TAG = "Modbus";

    public ModbusEquip(ProfileType type, short node) {
        profileType = type;
        slaveId = node;
    }

    public void init(short slaveId) {
        if (equipRef == null) {
            HashMap equip = hayStack.read("equip and modbus and group == \"" + slaveId + "\"");
            if (equip.isEmpty()) {
                CcuLog.e("Modbus","Init Failed : Equip does not exist "+slaveId);
                return;
            }
            equipRef = equip.get("id").toString();
        }
    }

    public String createEntities(String floorRef, String roomRef, EquipmentDevice equipmentInfo,
                               List<Parameter> configParams, String parentEquipId, boolean isSlaveIdSameAsParent,
                                 String modbusLevel,String modelVersion, boolean isConnectNode, boolean isPCN,boolean isExternalEquip, Map<String,
                    Pair<String, String>> registerAddressMap, boolean isSubEquip, String modelSuffix,
                                 String nodeAddress, String modelId, String parentDeviceId, String port) {
        String deviceId;
        if (parentDeviceId != null) {
            deviceId = addAtSymbolIfMissing(parentDeviceId);
        } else {
            deviceId = "";
        }

        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String modbusEquipType;
        double equipScheduleTypeVal;

        if(roomRef.contains("SYSTEM")) {
            equipScheduleTypeVal = ScheduleType.ZONE.ordinal();
        }else{
            equipScheduleTypeVal = (UtilKt.getSchedule(roomRef, floorRef)).isZoneSchedule()? ScheduleType.ZONE.ordinal() :
                    ScheduleType.NAMED.ordinal();
        }

        List<String> modbusEquipTypes = Arrays.asList(equipmentInfo.getEquipType().replaceAll("\\s", "").split(","));
        if (hasEquipType(ModbusEquipTypes.EMR_ZONE, modbusEquipTypes)) {
            modbusEquipType = String.valueOf(ModbusEquipTypes.EMR);
        } else {
            modbusEquipType = modbusEquipTypes.get(0);
        }
        String modbusName = equipmentInfo.getName();
        String equipDis;
        String equipDisplayName = equipmentInfo.getEquipDisplayName();
        if (isConnectNode) {
            if (Integer.parseInt(nodeAddress) <= MODBUS_SLAVE_ID_LIMIT) {
                equipDis = siteDis + "-" + modbusName + modelSuffix;
            } else {
                equipDis = siteDis + "-" + modbusName + modelSuffix;
            }
        } else if (isPCN) {
            equipDis = siteDis + "-" + modbusName + modelSuffix;
        } else if(equipDisplayName == null || equipDisplayName.isEmpty()){
            equipDis = siteDis + "-"+modbusName+"-"+ equipmentInfo.getSlaveId();
        }
        else {
            equipDis = siteDis + "-" + modbusName + "-" + equipDisplayName + "-" + equipmentInfo.getSlaveId();
        }
        String gatewayRef = null;
        configuredParams = configParams;
        CcuLog.d("Modbus",modbusEquipType+"MbEquip create Entity = "+configuredParams.size());
        HashMap systemEquip = hayStack.read(CommonQueries.SYSTEM_PROFILE);
        if (systemEquip != null && !systemEquip.isEmpty()) {
            gatewayRef = systemEquip.get("id").toString();
        }

        Equip.Builder mbEquip = new Equip.Builder().setSiteRef(siteRef)
                    .setDisplayName(equipDis)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef)
                    .setProfile(profileType.name())
                    .addMarker("equip")
                    .addTag("version", HStr.make(modelVersion))
                    .setGatewayRef(gatewayRef).setTz(tz).setGroup(String.valueOf(equipmentInfo.getSlaveId()));
        if (port != null && !port.isEmpty()) {
            mbEquip.addTag("port", HStr.make(port));
        } else {
            CcuLog.d(TAG, "ModbusEquip port is null or empty");
        }
        if (parentEquipId != null) {
            mbEquip.setEquipRef(parentEquipId);
        }
        if (isConnectNode) {
            mbEquip.addMarker(Tags.CONNECTMODULE);
            mbEquip.addTag("modelId", HStr.make(modelId));
            if (Integer.parseInt(nodeAddress) <= MODBUS_SLAVE_ID_LIMIT) {
                mbEquip.addTag("deviceRef", HStr.make(deviceId));
            } else {
                mbEquip.addTag("connectAddress", HStr.make(nodeAddress));
            }
        } else if (isPCN) {
            mbEquip.addMarker(Tags.PCN);
            mbEquip.addTag("deviceRef", HStr.make(deviceId));
            mbEquip.addTag("modelId", HStr.make(modelId));
        } else if (isExternalEquip) {
            mbEquip.addTag("deviceRef", HStr.make(deviceId));
            mbEquip.addMarker("modbus");
        } else {
            mbEquip.addMarker("modbus");
        }
        mbEquip.setEquipType(equipmentInfo.getEquipType());

        for (String equip :
                modbusEquipTypes) {
            if (isEquipTypeInUpperCase(equip)) {
                mbEquip.addMarker(equip.toLowerCase().trim());
            } else {
                mbEquip.addMarker(equip.trim());
            }
        }
        mbEquip.addMarker(modbusLevel.toLowerCase().trim());

        if (equipmentInfo.getVendor()!= null && !equipmentInfo.getVendor().isEmpty()) {
            mbEquip.setVendor(equipmentInfo.getVendor());
        }
        if (equipmentInfo.getModelNumbers() != null && !equipmentInfo.getModelNumbers().isEmpty()) {

            mbEquip.setModel(equipmentInfo.getModelNumbers().get(0));
        }
        if (equipmentInfo.getCell() != null) {
            mbEquip.setCell(equipmentInfo.getCell());
        }
        if (equipmentInfo.getCapacity() != null) {
            mbEquip.setCapacity(equipmentInfo.getCapacity());
        }
        String equipmentRef = hayStack.addEquip(mbEquip.build());

        if(!isSlaveIdSameAsParent) {
            try {
                Point heartBeatPoint = HeartBeat.getHeartBeatPoint(equipDis, equipmentRef,
                        siteRef, roomRef, floorRef, equipmentInfo.getSlaveId(), "modbus", profileType, tz);
                int band = 2000;
                if(parentEquipId != null) { // Updating BacnetId for sub equipment
                    band = 2500;
                }
                int uniqueBacnetId = band + equipmentInfo.getSlaveId();
                String uniqueId = uniqueBacnetId + "029";
                heartBeatPoint.setBacnetId(Integer.valueOf(uniqueId));
                CCUHsApi.getInstance().addPoint(heartBeatPoint);
            }catch (Exception e){
                CcuLog.e(TAG,"Error in creating heartbeat point for "+equipDis);
                e.printStackTrace();
            }
        }

        Point.Builder equipScheduleType = new Point.Builder()
                    .setDisplayName(siteDis+"-"+modbusEquipType+"-"+equipmentInfo.getSlaveId()+"-scheduleType")
                    .setEquipRef(equipmentRef)
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef).setHisInterpolate("cov")
                    .addMarker(modbusEquipType.toLowerCase()).addMarker("scheduleType").addMarker("writable").addMarker("his")
                    .addMarker(modbusLevel.toLowerCase().trim())
                    .setGroup(String.valueOf(equipmentInfo.getSlaveId()))
                    .setEnums("building,named")
                    .setTz(tz);

        if (isConnectNode) {
            equipScheduleType.addMarker(Tags.CONNECTMODULE);
        } else if (isPCN) {
            equipScheduleType.addMarker(Tags.PCN);
        } else {
            equipScheduleType.addMarker(Tags.MODBUS);
        }

        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType.build());
        CCUHsApi.getInstance().writeDefaultValById(equipScheduleTypeId, equipScheduleTypeVal);
        CCUHsApi.getInstance().writeHisValById(equipScheduleTypeId,equipScheduleTypeVal);
        String deviceRef;
        if (isConnectNode || isPCN) {
            deviceRef = deviceId;
        } else {
            Device modbusDevice = new Device.Builder()
                    .setDisplayName(modbusEquipType + "-" + equipmentInfo.getSlaveId())
                    .addMarker("network").addMarker("modbus").addMarker(modbusEquipType.toLowerCase())
                    .setAddr(equipmentInfo.getSlaveId())
                    .setSiteRef(siteRef)
                    .setFloorRef(floorRef)
                    .setEquipRef(equipmentRef)
                    .setRoomRef(roomRef)
                    .build();
            deviceRef = CCUHsApi.getInstance().addDevice(modbusDevice);
        }

        for(Parameter configParam : configParams){
            Double defaultValue = configParam.getDefaultValue();
            String disName = configParam.getName();
            CcuLog.d(TAG, "disName: "+disName+"  ModbusEquip default value: "+defaultValue);
            Point.Builder logicalParamPoint = new Point.Builder()
                        .setDisplayName(equipDis+"-"+configParam.getName()+modelSuffix)
                        .setShortDis(configParam.getName())
                        .setEquipRef(equipmentRef)
                        .setSiteRef(siteRef)
                        .setRoomRef(roomRef)
                        .setFloorRef(floorRef).addMarker("logical")
                        .setGroup(String.valueOf(equipmentInfo.getSlaveId()))
                        .setTz(tz);
            logicalParamPoint.addMarker(modbusLevel.toLowerCase().trim());

            RawPoint.Builder physicalParamPoint = new RawPoint.Builder()
                    .setDisplayName("register" + configParam.getRegisterAddress() + "-bits-" + configParam.getStartBit() + "-" + configParam.getEndBit())
                    .setShortDis(configParam.getName())
                    .setDeviceRef(deviceRef)
                    .setFloorRef(floorRef)
                    .setRoomRef(roomRef)
                    .setStartBit(String.valueOf(configParam.getStartBit()))
                    .setEndBit(String.valueOf(configParam.getEndBit()))
                    .setRegisterType(configParam.getRegisterType())
                    .setRegisterAddress(String.valueOf(configParam.getRegisterAddress()))
                    .setRegisterNumber(configParam.getRegisterNumber())
                    .setParameterId(configParam.getParameterId())
                    .setSiteRef(siteRef).addMarker("register").addMarker("modbus")
                    .setTz(tz)
                    .addTag("parameterDefinitionType", HStr.make(configParam.getParameterDefinitionType()))
                    .addTag("multiplier", HStr.make(configParam.getMultiplier()))
                    .addTag("wordOrder", HStr.make(configParam.getWordOrder()))
                    .addTag("bitParamRange", HStr.make(configParam.getBitParamRange()))
                    .addTag("bitParam", HStr.make((configParam.getBitParam() != null) ? configParam.getBitParam().toString() : "0"));

            if (isConnectNode || isPCN) {
                if(!registerAddressMap.containsKey(configParam.getName())) {
                    continue;
                }
                logicalParamPoint.addMarker(isConnectNode ? Tags.CONNECTMODULE : Tags.PCN);
                logicalParamPoint.addTag("registerNumber", HStr.make(registerAddressMap.get(configParam.getName()).getFirst()))
                        .addTag("registerAddress", HStr.make(registerAddressMap.get(configParam.getName()).getSecond()))
                        .addTag("registerType", HStr.make("holdingRegister"))
                        .addTag("parameterDefinitionType", HStr.make("float"))
                        .addTag("wordOrder", HStr.make("bigEndian"));
            } else {
                logicalParamPoint.addMarker("modbus");
            }
            if(configParam.isDisplayInUI()){
                logicalParamPoint.addMarker("displayInUi");
            }

            if(configParam.getIsSchedulable()) {
                logicalParamPoint.addMarker(Tags.SCHEDULABLE);
            } else {
                logicalParamPoint.removeMarker(Tags.SCHEDULABLE);
                if(configParam.userIntentPointTags != null) {
                    configParam.logicalPointTags.removeIf(marker -> marker.getTagName().equals(Tags.SCHEDULABLE));
                    configParam.userIntentPointTags.removeIf(marker -> marker.getTagName().equals(Tags.SCHEDULABLE));
                }
            }
            boolean isWritable = false;
            for(LogicalPointTags marker : configParam.getLogicalPointTags()) {
                if(Objects.nonNull(marker.getTagValue())){
                    if(marker.getTagName().contains("bacnetId")) {
                        int band = 2000;
                        if(isSubEquip) {
                           band = 2500;
                        }
                         band = band + equipmentInfo.getSlaveId();
                        String uniqueId = band + formatNumber(Integer.parseInt(marker.getTagValue()));
                        CcuLog.d(TAG, "assign bacnet id to modbus point-->"+uniqueId);
                        logicalParamPoint.setBacnetId(Integer.parseInt(uniqueId));
                    }
                    if(marker.getTagName().contains("bacnetType")) {
                        CcuLog.d(TAG, "assign bacnet type to modbus point-->"+marker.getTagValue());
                        logicalParamPoint.setBacnetType(marker.getTagValue());
                    }
                    if(marker.getTagName().contains("unit")) {
                        logicalParamPoint.setUnit(marker.getTagValue());
                        physicalParamPoint.setUnit(marker.getTagValue());
                    }
                    else if(marker.getTagName().contains("hisInterpolate")){
                        logicalParamPoint.setHisInterpolate(marker.getTagValue());
                    }
                    else if(marker.getTagName().contains("minVal")){
                        logicalParamPoint.setMinVal(String.valueOf(marker.getTagValue()));
                    }
                    else if(marker.getTagName().contains("maxVal")){
                        logicalParamPoint.setMaxVal(String.valueOf(marker.getTagValue()));
                    }
                    else if(marker.getTagName().contains("incrementVal")){
                        logicalParamPoint.setIncrementVal(String.valueOf(marker.getTagValue()));
                    }
                    else if(marker.getTagName().contains("cell")){
                        logicalParamPoint.setCell(String.valueOf(marker.getTagValue()));
                    }
                    else if(!marker.getTagName().contains("kind")) {
                        if(isInt(marker.getTagValue())) {
                            logicalParamPoint.addTag(marker.getTagName(), HNum.make(Integer.parseInt(marker.getTagValue())));
                        } else if(isLong(marker.getTagValue())) {
                            logicalParamPoint.addTag(marker.getTagName(), HNum.make(Long.parseLong(marker.getTagValue())));
                        } else if(isDouble(marker.getTagValue())) {
                            logicalParamPoint.addTag(marker.getTagName(), HNum.make(Double.parseDouble(marker.getTagValue())));
                        } else {
                            logicalParamPoint.addTag(marker.getTagName(), HStr.make(marker.getTagValue()));
                        }

                    }

                }else{
                    logicalParamPoint.addMarker(marker.getTagName());
                    physicalParamPoint.addMarker(marker.getTagName());
                }
                if (marker.getTagName().contains("writable")) {
                    isWritable = true;
                }
                CcuLog.d(TAG, modbusEquipType+"MBEquip logical and physical  markers="+marker.getTagName());
            }
            if(Objects.nonNull(configParam.getUserIntentPointTags())) {
                for (UserIntentPointTags marker : configParam.getUserIntentPointTags()) {
                    if (Objects.nonNull(marker.getTagValue())) {
                        if (marker.getTagName().contains("unit")) {
                            logicalParamPoint.setUnit(marker.getTagValue());
                            physicalParamPoint.setUnit(marker.getTagValue());
                        }
                        else if (marker.getTagName().contains("hisInterpolate")) {
                            logicalParamPoint.setHisInterpolate(marker.getTagValue());
                        }
                        else if(marker.getTagName().contains("minVal")){
                            logicalParamPoint.setMinVal(String.valueOf(marker.getTagValue()));
                        }
                        else if(marker.getTagName().contains("maxVal")){
                            logicalParamPoint.setMaxVal(String.valueOf(marker.getTagValue()));
                        }
                        else if(marker.getTagName().contains("incrementVal")){
                            logicalParamPoint.setIncrementVal(String.valueOf(marker.getTagValue()));
                        }
                        else if(marker.getTagName().contains("cell")){
                            logicalParamPoint.setCell(String.valueOf(marker.getTagValue()));
                        }
                        else if(!marker.getTagName().contains("kind")) {
                            if(isInt(marker.getTagValue())) {
                                logicalParamPoint.addTag(marker.getTagName(), HNum.make(Integer.parseInt(marker.getTagValue())));
                            } else if(isLong(marker.getTagValue())) {
                                logicalParamPoint.addTag(marker.getTagName(), HNum.make(Long.parseLong(marker.getTagValue())));
                            } else if(isDouble(marker.getTagValue())) {
                                logicalParamPoint.addTag(marker.getTagName(), HNum.make(Double.parseDouble(marker.getTagValue())));
                            } else {
                                logicalParamPoint.addTag(marker.getTagName(), HStr.make(marker.getTagValue()));
                            }
                        }
                    } else {
                        logicalParamPoint.addMarker(marker.getTagName());
                        physicalParamPoint.addMarker(marker.getTagName());
                    }
                    if (marker.getTagName().contains("writable")) {
                        isWritable = true;
                    }
                    CcuLog.d("Modbus", modbusEquipType + "MBEquip UserIntent  markers=" + marker.getTagName());
                }
            }
            StringBuffer enumVariables = new StringBuffer();
            if(Objects.nonNull(configParam.getConditions())) {
                for(Condition readCondition :configParam.getConditions()) {
                    if(Objects.nonNull(readCondition.getBitValues())) {
                        if(enumVariables.length() == 0)
                            enumVariables.append(readCondition.getName()).append("=").append(readCondition.getBitValues());
                        else {
                            enumVariables.append(",");
                            enumVariables.append(readCondition.getName()).append("=").append(readCondition.getBitValues());
                        }
                    }
                }
                if (Objects.nonNull(enumVariables)) {
                    logicalParamPoint.setEnums(enumVariables.toString());
                    CcuLog.d("Modbus", modbusEquipType+"MBEquip read params enums=" + enumVariables);
                }
            }else if(Objects.nonNull( configParam.getCommands())){

                for(Command writeCommand :configParam.getCommands()) {
                    if(Objects.nonNull(writeCommand.getBitValues())) {
                        if(enumVariables.length() == 0)
                            enumVariables.append(writeCommand.getName()).append("=").append(writeCommand.getBitValues());
                        else {
                            enumVariables.append(",");
                            enumVariables.append(writeCommand.getName()).append("=").append(writeCommand.getBitValues());
                        }
                    }
                }
                if (Objects.nonNull(enumVariables)) {
                    logicalParamPoint.setEnums(enumVariables.toString());
                    CcuLog.d("Modbus", modbusEquipType+"MBEquip write params enums=" + enumVariables);
                }
            }
            Point logicalPoint = logicalParamPoint.build();
            String logicalParamId = CCUHsApi.getInstance().addPoint(logicalPoint);
            RawPoint physicalPoint = physicalParamPoint.setPointRef(logicalParamId).build();

            // Do not create physical point if it is connect node
            String physicalParamId = null;
            if (!isConnectNode || isPCN) {
                 physicalParamId = CCUHsApi.getInstance().addPoint(physicalPoint);
                if(defaultValue != null){
                    CCUHsApi.getInstance().writeHisValById(physicalParamId,defaultValue);
                    if (physicalPoint.getMarkers().contains("writable")) {
                        CCUHsApi.getInstance().writeDefaultValById(physicalParamId,defaultValue);
                    }
                } else {
                    CCUHsApi.getInstance().writeHisValById(physicalParamId,0.0);
                    if (physicalPoint.getMarkers().contains("writable")) {
                        CCUHsApi.getInstance().writeDefaultValById(physicalParamId,0.0);
                    }
                }
            }
            if (configParam.getUserIntentPointTags() != null) {
                if (configParam.getCommands() != null && !configParam.getCommands().isEmpty()) {
                    if (defaultValue != null) {
                        CcuLog.d(TAG, "writing default value for command point pointId: " + logicalParamId +
                                " default value: " + defaultValue);
                        CCUHsApi.getInstance().writeHisValById(logicalParamId, defaultValue);
                        if(isWritable) CCUHsApi.getInstance().writeDefaultValById(logicalParamId, defaultValue);
                        if (isZonePoint(logicalPoint)) {
                            CCUHsApi.getInstance().writePoint(
                                    logicalParamId,
                                    17,
                                    CCUHsApi.getInstance().getCCUUserName(),
                                    defaultValue,
                                    0
                            );
                        }
                    } else {
                        CCUHsApi.getInstance().writeHisValById(logicalParamId, Double.parseDouble(configParam.getCommands().get(0).getBitValues()));
                        if(isWritable) CCUHsApi.getInstance().writeDefaultValById(logicalParamId, Double.parseDouble(configParam.getCommands().get(0).getBitValues()));
                        if (isZonePoint(logicalPoint)) {
                            CCUHsApi.getInstance().writePoint(
                                    logicalParamId,
                                    17,
                                    CCUHsApi.getInstance().getCCUUserName(),
                                    Double.parseDouble(configParam.getCommands().get(0).getBitValues()),
                                    0
                            );
                        }
                    }
                } else {
                    if (defaultValue != null) {
                        CcuLog.d(TAG, "writing default value pointId: " + logicalParamId +
                                " default value: " + defaultValue);
                        CCUHsApi.getInstance().writeHisValById(logicalParamId, defaultValue);
                        if(isWritable) CCUHsApi.getInstance().writeDefaultValById(logicalParamId, defaultValue);
                        if (isZonePoint(logicalPoint)) {
                            CCUHsApi.getInstance().writePoint(
                                    logicalParamId,
                                    17,
                                    CCUHsApi.getInstance().getCCUUserName(),
                                    defaultValue,
                                    0
                            );
                        }
                    } else if (logicalPoint.getMinVal() != null) {
                        CcuLog.d(TAG, "writing min value pointId: " + logicalParamId +
                                " min value: " + logicalPoint.getMinVal());
                        CCUHsApi.getInstance().writeHisValById(logicalParamId,
                                Double.parseDouble(logicalPoint.getMinVal()));
                        if (logicalPoint.getMarkers().contains("writable")) {
                            CCUHsApi.getInstance().writeDefaultValById(logicalParamId,
                                    Double.parseDouble(logicalPoint.getMinVal()));
                            if (isZonePoint(logicalPoint)) {
                                CCUHsApi.getInstance().writePoint(
                                        logicalParamId,
                                        17,
                                        CCUHsApi.getInstance().getCCUUserName(),
                                        Double.parseDouble(logicalPoint.getMinVal()),
                                        0
                                );
                            }
                        }
                    } else {
                        CcuLog.d(TAG, "writing 0 value pointId: " + logicalParamId + " value: " + 0);
                        CCUHsApi.getInstance().writeHisValById(logicalParamId, 0.0);
                        if(isWritable) CCUHsApi.getInstance().writeDefaultValById(logicalParamId, 0.0);
                        if (logicalPoint.getMarkers().contains("writable")) {
                            if (isZonePoint(logicalPoint)) {
                                CCUHsApi.getInstance().writePoint(
                                        logicalParamId,
                                        17,
                                        CCUHsApi.getInstance().getCCUUserName(),
                                        0.0,
                                        0
                                );
                            }
                        }
                    }
                }

            } else {
                if (configParam.getConditions() != null && !configParam.getConditions().isEmpty()) {
                    CCUHsApi.getInstance().writeHisValById(logicalParamId,
                            Double.parseDouble(configParam.getConditions().get(0).getBitValues()));
                    if (defaultValue != null) {
                        if(isWritable) CCUHsApi.getInstance().writeDefaultValById(logicalParamId, defaultValue);
                        CCUHsApi.getInstance().writeHisValById(logicalParamId, defaultValue);
                        if (isZonePoint(logicalPoint)) {
                            CCUHsApi.getInstance().writePoint(
                                    logicalParamId,
                                    17,
                                    CCUHsApi.getInstance().getCCUUserName(),
                                    defaultValue,
                                    0
                            );
                        }
                    } else if (logicalPoint.getMarkers().contains("writable")) {
                        if(isWritable) CCUHsApi.getInstance().writeDefaultValById(logicalParamId,
                                Double.parseDouble(configParam.getConditions().get(0).getBitValues()));

                        if (isZonePoint(logicalPoint)) {
                            CCUHsApi.getInstance().writePoint(
                                    logicalParamId,
                                    17,
                                    CCUHsApi.getInstance().getCCUUserName(),
                                    Double.parseDouble(configParam.getConditions().get(0).getBitValues()),
                                    0
                            );
                        }
                    }
                } else {
                    if (defaultValue != null) {
                        CcuLog.d(TAG, "writing default value pointId: " +
                                logicalParamId + " default value: " + defaultValue);
                        CCUHsApi.getInstance().writeHisValById(physicalParamId, defaultValue);
                        if(isWritable) CCUHsApi.getInstance().writeDefaultValById(logicalParamId, defaultValue);
                        if (isZonePoint(logicalPoint)) {
                            CCUHsApi.getInstance().writePoint(
                                    logicalParamId,
                                    17,
                                    CCUHsApi.getInstance().getCCUUserName(),
                                    Double.parseDouble(logicalPoint.getMinVal()),
                                    0
                            );
                        }
                    } else if (logicalPoint.getMinVal() != null) {
                        CcuLog.d(TAG, "writing min value pointId: " + logicalParamId +
                                " min value: " + logicalPoint.getMinVal());
                        CCUHsApi.getInstance().writeHisValById(logicalParamId,
                                Double.parseDouble(logicalPoint.getMinVal()));
                        if (logicalPoint.getMarkers().contains("writable")) {
                            CCUHsApi.getInstance().writeDefaultValById(logicalParamId,
                                    Double.parseDouble(logicalPoint.getMinVal()));
                            if (isZonePoint(logicalPoint)) {
                                CCUHsApi.getInstance().writePoint(
                                        logicalParamId,
                                        17,
                                        CCUHsApi.getInstance().getCCUUserName(),
                                        Double.parseDouble(logicalPoint.getMinVal()),
                                        0
                                );
                            }
                        }
                    } else {
                        CcuLog.d(TAG, "writing 0 value pointId: " + logicalParamId + " value: " + 0);
                        CCUHsApi.getInstance().writeHisValById(logicalParamId, 0.0);
                        if (logicalPoint.getMarkers().contains("writable")) {
                            CCUHsApi.getInstance().writeDefaultValById(logicalParamId, 0.0);
                            if (isZonePoint(logicalPoint)) {
                                CCUHsApi.getInstance().writePoint(
                                        logicalParamId,
                                        17,
                                        CCUHsApi.getInstance().getCCUUserName(),
                                        0.0,
                                        0
                                );
                            }
                        }
                    }
                }
            }
        }

        if(modbusLevel.equals("zone")) {
            addEquipScheduleStatusPoint(mbEquip.build(), equipmentRef);
        }

        CCUHsApi.getInstance().syncEntityTree();
        return equipmentRef;
    }

    public static String formatNumber(int number) {
        return String.format("%03d", number);
    }

    public void updateHaystackPoints(String equipRef, List<Parameter> configuredParams) {
        for (Parameter configParams : configuredParams) {
                HDict pointRead = hayStack.readHDictById(configParams.getLogicalId());
            if(pointRead != null) {
                Point logicalPoint = new Point.Builder().setHDict(pointRead).build();
                CcuLog.d("Modbus", "UpdateHaystackPoints: Logical Point -> " + pointRead + " name " + configParams.getName() + " display in UI " + configParams.isDisplayInUI());

                boolean continuePointUpdate = false;
                if (configParams.isDisplayInUI()) {
                    if (!logicalPoint.getMarkers().contains("displayInUi")) {
                        logicalPoint.getMarkers().add("displayInUi");
                        CcuLog.d("Modbus", "UpdateHaystackPoints: Add displayUI tag " + logicalPoint.getDisplayName());
                        continuePointUpdate = true;
                    }
                } else if (logicalPoint.getMarkers().contains("displayInUi")) {
                    logicalPoint.getMarkers().remove("displayInUi");
                    CcuLog.d("Modbus", "UpdateHaystackPoints: Remove displayUI tag " + logicalPoint.getDisplayName());
                    continuePointUpdate = true;
                }

                if (configParams.getIsSchedulable()) {
                    if (!logicalPoint.getMarkers().contains(Tags.SCHEDULABLE)) {
                        logicalPoint.getMarkers().add(Tags.SCHEDULABLE);
                        CcuLog.d("Modbus", "UpdateHaystackPoints: Add schedulable tag " + logicalPoint.getDisplayName());
                        continuePointUpdate = true;
                    }
                } else if (logicalPoint.getMarkers().contains(Tags.SCHEDULABLE)) {
                    logicalPoint.getMarkers().remove(Tags.SCHEDULABLE);
                    CcuLog.d("Modbus", "UpdateHaystackPoints: Remove schedulable tag " + logicalPoint.getDisplayName());
                    continuePointUpdate = true;
                }

                if (continuePointUpdate && logicalPoint.getId() != null) {
                    updateWritableDataUponCustomControlChanges(logicalPoint);
                    hayStack.updatePoint(logicalPoint, logicalPoint.getId());
                }
            }
        }
        hayStack.syncEntityTree();
    }

    public boolean hasEquipType(ModbusEquipTypes modbusEquipTyp, List<String> modbusEquipTypes) {
        for (String eqType :
                modbusEquipTypes) {
            if (eqType.equalsIgnoreCase(String.valueOf(modbusEquipTyp))){
                return true;
            }
        }
        return false;
    }

    public static boolean isEquipTypeInUpperCase(String str) {
        String[] words = str.split("\\s+");

        for (String word : words) {
            if (!word.equals(word.toUpperCase())) {
                return false;
            }
        }

        return true;
    }

    public static boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isLong(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isZonePoint(Point point){
        return point.getMarkers().contains("zone");
    }
}
