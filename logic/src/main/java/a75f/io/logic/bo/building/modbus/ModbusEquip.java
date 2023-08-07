package a75f.io.logic.bo.building.modbus;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HStr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;
import a75f.io.modbusbox.ModbusCategory;

public class ModbusEquip {
    ProfileType profileType;
    public short slaveId;
    String equipRef = null;
    public List<Parameter> configuredParams= new ArrayList<Parameter>();
    CCUHsApi hayStack = CCUHsApi.getInstance();

    public ModbusEquip(ProfileType type, short node) {
        profileType = type;
        slaveId = node;
    }

    public void init(short slaveId) {
        if (equipRef == null) {
            HashMap equip = hayStack.read("equip and modbus and group == \"" + slaveId + "\"");
            if (equip.isEmpty()) {
                Log.e("Modbus","Init Failed : Equip does not exist "+slaveId);
                return;
            }
            equipRef = equip.get("id").toString();
        }
    }

 /*   public String createEntities(String floorRef, String roomRef, EquipmentDevice equipmentInfo,
                                 List<Parameter> configParams) {
     return createEntities(floorRef, roomRef, equipmentInfo, configParams, null, false,null);
    }
*/
    public String createEntities(String floorRef, String roomRef, EquipmentDevice equipmentInfo,
                               List<Parameter> configParams, String parentEquipId, boolean isSlaveIdSameAsParent,String modbusLevel) {
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String modbusEquipType;
        List<String> modbusEquipTypes = Arrays.asList(equipmentInfo.getEquipType().replaceAll("\\s", "").split(","));
        if (hasEquipType(ModbusEquipTypes.EMR_ZONE, modbusEquipTypes)) {
            modbusEquipType = String.valueOf(ModbusEquipTypes.EMR);
        } else {
            modbusEquipType = modbusEquipTypes.get(0);
        }
        String modbusName = equipmentInfo.getName();
        String equipDis = siteDis + "-"+modbusName+"-"+modbusEquipType+"-" + equipmentInfo.getSlaveId();
        String gatewayRef = null;
        configuredParams = configParams;
        Log.d("Modbus",modbusEquipType+"MbEquip create Entity = "+configuredParams.size());
        HashMap systemEquip = hayStack.read("equip and system");
        if (systemEquip != null && systemEquip.size() > 0) {
            gatewayRef = systemEquip.get("id").toString();
        }

        Equip.Builder mbEquip = new Equip.Builder().setSiteRef(siteRef)
                    .setDisplayName(equipDis)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef)
                    .setProfile(profileType.name())
                    .addMarker("equip").addMarker("modbus")
                    .setGatewayRef(gatewayRef).setTz(tz).setGroup(String.valueOf(equipmentInfo.getSlaveId()));
        if (parentEquipId != null) {
            mbEquip.setEquipRef(parentEquipId);
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

        /*
        if (profileType != ProfileType.MODBUS_EMR && profileType != ProfileType.MODBUS_BTU) {
            mbEquip.addMarker("zone");
        }*/

        mbEquip.addMarker(modbusLevel);

        if (equipmentInfo.getVendor()!= null && !equipmentInfo.getVendor().equals("")) {
            mbEquip.setVendor(equipmentInfo.getVendor());
        }
        if (equipmentInfo.getModelNumbers() != null && equipmentInfo.getModelNumbers().size() >0) {

            mbEquip.setModel(equipmentInfo.getModelNumbers().get(0));
        }
        if (equipmentInfo.getCell() != null) {
            mbEquip.setCell(equipmentInfo.getCell());
        }
        if (equipmentInfo.getCapacity() != null) {
            mbEquip.setCapacity(equipmentInfo.getCapacity());
        }
        String equipmentRef = hayStack.addEquip(mbEquip.build());

        String zoneMarker = "";
        if (profileType != ProfileType.MODBUS_EMR && profileType != ProfileType.MODBUS_BTU) {
            zoneMarker = "zone";
        }
        if(!isSlaveIdSameAsParent) {
            CCUHsApi.getInstance().addPoint(HeartBeat.getHeartBeatPoint(equipDis, equipmentRef,
                    siteRef, roomRef, floorRef, equipmentInfo.getSlaveId(), "modbus", profileType, tz));
        }
        Point equipScheduleType = new Point.Builder()
                    .setDisplayName(siteDis+"-"+modbusEquipType+"-"+equipmentInfo.getSlaveId()+"-scheduleType")
                    .setEquipRef(equipmentRef)
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef).setHisInterpolate("cov")
                    .addMarker(modbusEquipType.toLowerCase()).addMarker("modbus").addMarker("scheduleType").addMarker("writable").addMarker("his")
                    .addMarker(zoneMarker)
                    .setGroup(String.valueOf(equipmentInfo.getSlaveId()))
                    .setEnums("building,named")
                    .setTz(tz).build();

        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType);
        CCUHsApi.getInstance().writeDefaultValById(equipScheduleTypeId, 0.0);
        CCUHsApi.getInstance().writeHisValById(equipScheduleTypeId, 0.0);

        Device modbusDevice = new Device.Builder()
                .setDisplayName(modbusEquipType+"-"+equipmentInfo.getSlaveId())
                .addMarker("network").addMarker("modbus").addMarker(modbusEquipType.toLowerCase()).addMarker("his")
                .setAddr(equipmentInfo.getSlaveId())
                .setSiteRef(siteRef)
                .setFloorRef(floorRef)
                .setEquipRef(equipmentRef)
                .setRoomRef(roomRef)
                .build();
        String deviceRef = CCUHsApi.getInstance().addDevice(modbusDevice);

        for(Parameter configParam : configParams){
            Point.Builder logicalParamPoint = new Point.Builder()
                        .setDisplayName(equipDis+"-"+configParam.getName())
                        .setShortDis(configParam.getName())
                        .setEquipRef(equipmentRef)
                        .setSiteRef(siteRef)
                        .setRoomRef(roomRef)
                        .setFloorRef(floorRef).addMarker("logical").addMarker("modbus")
                        .setGroup(String.valueOf(equipmentInfo.getSlaveId()))
                        .setTz(tz);
            if (profileType != ProfileType.MODBUS_EMR && profileType != ProfileType.MODBUS_BTU) {
                 logicalParamPoint.addMarker("zone");
            }
            RawPoint.Builder physicalParamPoint = new RawPoint.Builder()
                    .setDisplayName("register"+configParam.getRegisterAddress()+"-bits-"+configParam.getStartBit()+"-"+configParam.getEndBit())
                    .setShortDis(configParam.getName())
                    .setDeviceRef(deviceRef)
                    .setFloorRef(floorRef)
                    .setRoomRef(roomRef)
                    .setRegisterAddress(String.valueOf(configParam.getRegisterAddress()))
                    .setStartBit(String.valueOf(configParam.getStartBit()))
                    .setEndBit(String.valueOf(configParam.getEndBit()))
                    .setRegisterNumber(configParam.getRegisterNumber())
                    .setRegisterType(configParam.getRegisterType())
                    .setParameterId(configParam.getParameterId())
                    .setSiteRef(siteRef).addMarker("register").addMarker("modbus")
                    .setTz(tz)
                    .addTag("parameterDefinitionType", HStr.make(configParam.getParameterDefinitionType()))
                    .addTag("multiplier", HStr.make(configParam.getMultiplier()))
                    .addTag("wordOrder", HStr.make(configParam.getWordOrder()))
                    .addTag("bitParamRange", HStr.make(configParam.getBitParamRange()))
                    .addTag("bitParam", HStr.make( (configParam.getBitParam() != null) ? configParam.getBitParam().toString() : "0"));

            if(configParam.isDisplayInUI()){
                logicalParamPoint.addMarker("displayInUi");
            }
            for(LogicalPointTags marker : configParam.getLogicalPointTags()) {
                if(Objects.nonNull(marker.getTagValue())){
                    if(marker.getTagName().contains("unit")) {
                        logicalParamPoint.setUnit(marker.getTagValue());
                        physicalParamPoint.setUnit(marker.getTagValue());
                    }
                    if(marker.getTagName().contains("hisInterpolate")){
                        logicalParamPoint.setHisInterpolate(marker.getTagValue());
                    }
                    if(marker.getTagName().contains("minVal")){
                        logicalParamPoint.setMinVal(String.valueOf(marker.getTagValue()));
                    }
                    if(marker.getTagName().contains("maxVal")){
                        logicalParamPoint.setMaxVal(String.valueOf(marker.getTagValue()));
                    }
                    if(marker.getTagName().contains("incrementVal")){
                        logicalParamPoint.setIncrementVal(String.valueOf(marker.getTagValue()));
                    }
                    if(marker.getTagName().contains("cell")){
                        logicalParamPoint.setCell(String.valueOf(marker.getTagValue()));
                    }

                }else{
                    logicalParamPoint.addMarker(marker.getTagName());
                    physicalParamPoint.addMarker(marker.getTagName());
                }
                Log.d("Modbus", modbusEquipType+"MBEquip logical and physical  markers="+marker.getTagName());
            }
            if(Objects.nonNull(configParam.getUserIntentPointTags())) {
                for (UserIntentPointTags marker : configParam.getUserIntentPointTags()) {
                    if (Objects.nonNull(marker.getTagValue())) {
                        if (marker.getTagName().contains("unit")) {
                            logicalParamPoint.setUnit(marker.getTagValue());
                            physicalParamPoint.setUnit(marker.getTagValue());
                        }
                        if (marker.getTagName().contains("hisInterpolate")) {
                            logicalParamPoint.setHisInterpolate(marker.getTagValue());
                        }
                        if(marker.getTagName().contains("minVal")){
                            logicalParamPoint.setMinVal(String.valueOf(marker.getTagValue()));
                        }
                        if(marker.getTagName().contains("maxVal")){
                            logicalParamPoint.setMaxVal(String.valueOf(marker.getTagValue()));
                        }
                        if(marker.getTagName().contains("incrementVal")){
                            logicalParamPoint.setHisInterpolate(String.valueOf(marker.getTagValue()));
                        }
                        if(marker.getTagName().contains("cell")){
                            logicalParamPoint.setCell(String.valueOf(marker.getTagValue()));
                        }
                        /*if (marker.getTagName().contains("kind")) {
                            logicalParamPoint.setKind(marker.getTagValue());
                        }*/
                    } else {
                        logicalParamPoint.addMarker(marker.getTagName());
                        physicalParamPoint.addMarker(marker.getTagName());
                    }
                    Log.d("Modbus", modbusEquipType + "MBEquip UserIntent  markers=" + marker.getTagName());
                }
            }
            StringBuffer enumVariables = new StringBuffer();
            if(Objects.nonNull(configParam.getConditions())) {
                for(Condition readCondition :configParam.getConditions()) {
                    if(Objects.nonNull(readCondition.getBitValues())) {
                        if(enumVariables.length() == 0)
                            enumVariables.append(readCondition.getName()+"="+readCondition.getBitValues());
                        else {
                            enumVariables.append(",");
                            enumVariables.append(readCondition.getName()+"="+readCondition.getBitValues());
                        }
                    }
                }
                if (Objects.nonNull(enumVariables)) {
                    logicalParamPoint.setEnums(enumVariables.toString());
                    Log.d("Modbus", modbusEquipType+"MBEquip read params enums=" + enumVariables.toString());
                }
            }else if(Objects.nonNull( configParam.getCommands())){

                for(Command writeCommand :configParam.getCommands()) {
                    if(Objects.nonNull(writeCommand.getBitValues())) {
                        if(enumVariables.length() == 0)
                            enumVariables.append(writeCommand.getName()+"="+writeCommand.getBitValues());
                        else {
                            enumVariables.append(",");
                            enumVariables.append(writeCommand.getName()+"="+writeCommand.getBitValues());
                        }
                    }
                }
                if (Objects.nonNull(enumVariables)) {
                    logicalParamPoint.setEnums(enumVariables.toString());
                    Log.d("Modbus", modbusEquipType+"MBEquip write params enums=" + enumVariables.toString());
                }
            }
            Point logicalPoint = logicalParamPoint.build();
            String logicalParamId = CCUHsApi.getInstance().addPoint(logicalPoint);
            RawPoint physicalPoint = physicalParamPoint.setPointRef(logicalParamId).build();
            String physicalParamId = CCUHsApi.getInstance().addPoint(physicalPoint);
            if (configParam.getUserIntentPointTags() != null) {
                if (configParam.getCommands() != null && configParam.getCommands().size() > 0) {
                    CCUHsApi.getInstance().writeHisValById(logicalParamId, Double.parseDouble(configParam.getCommands().get(0).getBitValues()));
                    CCUHsApi.getInstance().writeDefaultValById(logicalParamId, Double.parseDouble(configParam.getCommands().get(0).getBitValues()));
                } else {
                    if (logicalPoint.getMinVal() != null) {
                        CCUHsApi.getInstance().writeHisValById(logicalParamId, Double.parseDouble(logicalPoint.getMinVal()));
                        CCUHsApi.getInstance().writeDefaultValById(logicalParamId, Double.parseDouble(logicalPoint.getMinVal()));
                    } else {
                        CCUHsApi.getInstance().writeHisValById(logicalParamId, 0.0);
                        CCUHsApi.getInstance().writeDefaultValById(logicalParamId, 0.0);
                    }
                }

            } else {
                if (configParam.getConditions() != null && configParam.getConditions().size() > 0) {
                    CCUHsApi.getInstance().writeHisValById(logicalParamId, Double.parseDouble(configParam.getConditions().get(0).getBitValues()));
                    if (logicalPoint.getMarkers().contains("writable")) {
                        CCUHsApi.getInstance().writeDefaultValById(logicalParamId, Double.parseDouble(configParam.getConditions().get(0).getBitValues()));
                    }
                } else {
                    if (logicalPoint.getMinVal() != null) {
                        CCUHsApi.getInstance().writeHisValById(logicalParamId, Double.parseDouble(logicalPoint.getMinVal()));
                        if (logicalPoint.getMarkers().contains("writable")) {
                            CCUHsApi.getInstance().writeDefaultValById(logicalParamId, Double.parseDouble(logicalPoint.getMinVal()));
                        }
                    } else {
                        CCUHsApi.getInstance().writeHisValById(logicalParamId, 0.0);
                        if (logicalPoint.getMarkers().contains("writable")) {
                            CCUHsApi.getInstance().writeDefaultValById(logicalParamId, 0.0);
                        }
                    }
                }
            }

            CCUHsApi.getInstance().writeHisValById(physicalParamId,0.0);
            if (physicalPoint.getMarkers().contains("writable")){
                CCUHsApi.getInstance().writeDefaultValById(physicalParamId,0.0);
            }

        }
        CCUHsApi.getInstance().syncEntityTree();
        return equipmentRef;
    }

    public void updateHaystackPoints(String equipRef, EquipmentDevice equipmentDevice, List<Parameter> configuredParams) {
        for (Parameter configParams : configuredParams) {
            //Read all points for this markers
            StringBuilder tags = new StringBuilder();
            for (LogicalPointTags marker : configParams.getLogicalPointTags()) {
                if (!Objects.nonNull(marker.getTagValue())) {
                    tags.append(" and ").append(marker.getTagName());
                }
            }

            if (tags.length() > 0) {
                String zoneTag="";
                boolean isZone = false;
                String[] modbusEquipTypes = equipmentDevice.getEquipType().replaceAll("\\s","").split(",");
                for (String equipType :
                        modbusEquipTypes) {
                    isZone = !equipType.equals(ModbusCategory.BTU.displayName)
                            && !equipType.equals(ModbusCategory.EMR.displayName);
                }
                if(!isZone){
                    zoneTag="and zone";
                }
                HashMap pointRead = CCUHsApi.getInstance().read("point and logical and modbus "+zoneTag + tags + " and equipRef == \"" + equipRef + "\"");
                Point logicalPoint = new Point.Builder().setHashMap(pointRead).build();

                if (configParams.isDisplayInUI()) {
                    if (!logicalPoint.getMarkers().contains("displayInUi")) {
                        logicalPoint.getMarkers().add("displayInUi");
                        if (logicalPoint.getId() != null) {
                            CCUHsApi.getInstance().updatePoint(logicalPoint, logicalPoint.getId());
                        }
                    }
                } else if (logicalPoint.getMarkers().contains("displayInUi")) {
                    logicalPoint.getMarkers().remove("displayInUi");
                    if (logicalPoint.getId() != null) {
                        CCUHsApi.getInstance().updatePoint(logicalPoint, logicalPoint.getId());
                    }
                }
            }
        }
        CCUHsApi.getInstance().syncEntityTree();
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

    public List<Parameter> getProfileConfiguration(short slaveId){
        if(configuredParams != null && (configuredParams.size() > 0))
            return configuredParams;
        else {
            //TODO need to fetch all configured data

            return null;
        }
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
}
