package a75f.io.logic.bo.building.bacnet;

import android.util.Log;

import org.projecthaystack.HDict;
import org.projecthaystack.HNum;
import org.projecthaystack.HStr;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.bacnet.parser.BacnetModelDetailResponse;
import a75f.io.api.haystack.bacnet.parser.BacnetPoint;
import a75f.io.api.haystack.bacnet.parser.BacnetProperty;
import a75f.io.api.haystack.bacnet.parser.TagItem;
import a75f.io.logger.CcuLog;
import a75f.io.logic.UtilKt;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ScheduleType;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;

public class BacnetEquip {
    ProfileType profileType;
    public long slaveId;
    String equipRef = null;
    CCUHsApi hayStack = CCUHsApi.getInstance();

    private static final String TAG = "BacNetEquip";

    private static final String CONST_BACNET = "bacnet";
    private static final String  CONST_BACNET_CUR = "bacnetCur";
    private static final String CONST_BACNET_DEVICE_ID = "bacnetDeviceId";

    private static final String CONST_LOGICAL = "logical";

    public BacnetEquip(ProfileType type, long node) {
        profileType = type;
        slaveId = node;
    }

    public void init(long slaveId) {
        if (equipRef == null) {
            HashMap equip = hayStack.read("equip and bacnet and group == \"" + slaveId + "\"");
            if (equip.isEmpty()) {
                CcuLog.e("Bacnet","Init Failed : Equip does not exist "+slaveId);
                return;
            }
            equipRef = equip.get("id").toString();
        }
    }

    public String createEntities(String configParam, String modelConfig, String deviceId, String slaveId1, String floorRef, String roomRef,
                                 BacnetModelDetailResponse equipmentInfo, String parentEquipId,String moduleLevel,String modelVersion) {
        CcuLog.d(TAG, "##--bacnet client starting points creation--");
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String bacNetEquipType = "BACNET_DEFAULT";
        double equipScheduleTypeVal;

        if(roomRef.contains("SYSTEM")) {
            equipScheduleTypeVal = ScheduleType.ZONE.ordinal();
        }else{
            equipScheduleTypeVal = (UtilKt.getSchedule(roomRef, floorRef)).isZoneSchedule()? ScheduleType.ZONE.ordinal() :
                    ScheduleType.NAMED.ordinal();
        }

        String equipName = equipmentInfo.getName();
        String equipDisplayName = equipmentInfo.getDisplayName();
        List<TagItem> equipTagsList = equipmentInfo.getEquipTagsList();
        String equipDis = siteDis + "-"+equipName+"-"+equipDisplayName+"-"+ slaveId;
        String gatewayRef = null;

        HashMap systemEquip = hayStack.read("equip and system and not bacnet");
        if (systemEquip != null && systemEquip.size() > 0) {
            gatewayRef = systemEquip.get("id").toString();
        }

        int bacnetIdForEquip = HSUtil.generateBacnetId(String.valueOf(slaveId),false,false);

        Equip.Builder bacNetEquip = new Equip.Builder().setSiteRef(siteRef)
                    .setDisplayName(equipDis)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef)
                    .setProfile(profileType.name())
                    .setBacnetId(bacnetIdForEquip)
                    .setBacnetType("device")
                    .addMarker("equip").addMarker(CONST_BACNET).addMarker(CONST_BACNET_CUR)
                    .addTag(CONST_BACNET_DEVICE_ID, HNum.make(Integer.parseInt(deviceId)))
                    .addTag("version", HStr.make(modelVersion))
                    .addTag("bacnetConfig", HStr.make(configParam))
                    .addTag("modelConfig", HStr.make(modelConfig))
                    .setGatewayRef(gatewayRef).setTz(tz).setGroup(String.valueOf(slaveId));
        if (parentEquipId != null) {
            bacNetEquip.setEquipRef(parentEquipId);
        }

        if(equipTagsList.size() > 0){
            for(TagItem tagItem : equipTagsList){
                if(tagItem.getKind().toLowerCase().equals("marker")){
                    bacNetEquip.addMarker(tagItem.getName().toLowerCase().trim());
                }else{
                    try {
                        bacNetEquip.addTag(tagItem.getKind().toLowerCase(), HStr.make(tagItem.getDefaultValue()));
                    }catch (Exception e){
                        CcuLog.d(TAG, "hit exception 6-->" + e.getMessage());
                    }
                }
            }
        }

        bacNetEquip.setEquipType("BACNET-DEFAULT");
        bacNetEquip.addMarker(moduleLevel.toLowerCase().trim());
        String equipmentRef = hayStack.addEquip(bacNetEquip.build());

        CCUHsApi.getInstance().addPoint(HeartBeat.getHeartBeatPoint(equipDis, equipmentRef,
                siteRef, roomRef, floorRef, (int) slaveId, "bacnet", tz));

        Point equipScheduleType = new Point.Builder()
                    .setDisplayName(siteDis+"-"+bacNetEquipType+"-"+slaveId+"-scheduleType")
                    .setEquipRef(equipmentRef)
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef).setHisInterpolate("cov")
                    .addMarker(bacNetEquipType.toLowerCase()).addMarker("bacnet").addMarker("scheduleType").addMarker("writable").addMarker("his")
                    .addMarker(moduleLevel.toLowerCase().trim())
                    .setGroup(String.valueOf(slaveId))
                    .setEnums("building,named")
                    .setTz(tz).build();

        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType);
        CCUHsApi.getInstance().writeDefaultValById(equipScheduleTypeId, equipScheduleTypeVal);
        CCUHsApi.getInstance().writeHisValById(equipScheduleTypeId,equipScheduleTypeVal);

        Device bacnetDevice = new Device.Builder()
                .setDisplayName(bacNetEquipType+"-"+slaveId)
                .addMarker("network").addMarker("bacnet").addMarker(bacNetEquipType.toLowerCase()).addMarker("his").addMarker("node")
                .setAddr((int) slaveId)
                .setSiteRef(siteRef)
                .setFloorRef(floorRef)
                .setEquipRef(equipmentRef)
                .setRoomRef(roomRef)
                .build();
        String deviceID = CCUHsApi.getInstance().addDevice(bacnetDevice);
        CcuLog.d(TAG, "BACnet Device Entity created... device id--> "+deviceID);

        List<BacnetPoint> points = equipmentInfo.getPoints();
        CcuLog.d(TAG, "points to add-->"+points.size());
        try {
            for (BacnetPoint point : points){

                boolean isPointHistorized = point.getEquipTagNames().contains("his");
                boolean isWritable = point.getEquipTagNames().contains("writable");

                int bacnetId = point.getProtocolData().getBacnet().getObjectId();
                Point.Builder logicalParamPoint = new Point.Builder()
                        .setDisplayName(equipDis+"-"+point.getName())
                        .setShortDis(point.getName())
                        .setEquipRef(equipmentRef)
                        .setSiteRef(siteRef)
                        .setRoomRef(roomRef)
                        .setBacnetId(bacnetId)
                        .setUnit(point.getDefaultUnit())
                        .setBacnetType(getBacNetType(Objects.requireNonNull(point.getProtocolData()).getBacnet().getObjectType()))
                        .setFloorRef(floorRef).addMarker(CONST_LOGICAL)
                        .addMarker(CONST_BACNET).addMarker(CONST_BACNET_CUR)
                        .addTag(CONST_BACNET_DEVICE_ID, HNum.make(Integer.parseInt(deviceId)))
                        .setGroup(String.valueOf(slaveId))
                        .setTz(tz);

                if(Objects.requireNonNull(point.getValueConstraint()).getAllowedValues() != null){
                    AtomicReference<String> values = new AtomicReference<>("");
                    point.getValueConstraint().getAllowedValues().forEach((allowedValue) -> {
                        if(values.get().equals("")) {
                            values.set(allowedValue.getValue());
                        }else{
                            values.set(values + ", " + allowedValue.getValue());
                        }
                    });
                    logicalParamPoint.addTag("enum", HStr.make(String.valueOf(values)));
                }


                if(point.getPresentationData()!= null) {
                    String incrementStep = point.getPresentationData().getTagValueIncrement();
                    logicalParamPoint.addTag("incrementStep", HStr.make(incrementStep));
                }

//                RawPoint.Builder physicalParamPoint = new RawPoint.Builder()
//                        .setDisplayName(equipDis+"-"+point.getName())
//                        .setShortDis(point.getName())
//                        .setDeviceRef(deviceRef)
//                        .setFloorRef(floorRef)
//                        .setRoomRef(roomRef)
//                        .setParameterId(point.getId())
//                        .setSiteRef(siteRef).addMarker("register").addMarker("bacnet")
//                        .setTz(tz);


                if(point.getEquipTagsList().size() > 0){
                    for(TagItem tagItem : point.getEquipTagsList()){
                        if(tagItem.getKind().toLowerCase().equals("marker")){
                            bacNetEquip.addMarker(tagItem.getName().toLowerCase().trim());
                        }else{
                            try {
                                bacNetEquip.addTag(tagItem.getKind().toLowerCase(), HStr.make(tagItem.getDefaultValue()));
                            }catch (Exception e){
                                CcuLog.d(TAG, "hit exception 5-->" + e.getMessage());
                            }
                        }
                    }
                }

                if(point.getProtocolData().getBacnet().getDisplayInUIDefault()){
                    logicalParamPoint.addMarker("displayInUi");
                    //physicalParamPoint.addMarker("displayInUi");
                }
                //AtomicBoolean isWritable = new AtomicBoolean(false);
                AtomicBoolean hasPresentValue = new AtomicBoolean(false);
                AtomicReference<String> presentValue = new AtomicReference<>("");
                point.getBacnetProperties().forEach((bacnetProperty) -> {
                    if(bacnetProperty.getName().equals("PRESENT_VALUE")){
                        String presentVal = getValue(bacnetProperty);
                        if(presentVal != null && !presentVal.equals("null") && !presentVal.equals("") && !presentVal.equals("NA")){
                            hasPresentValue.set(true);
                            presentValue.set(presentVal);
                        }
                    }else if(bacnetProperty.getName().equals("MIN_PRESENT_VALUE")){
                        String minValue = getValue(bacnetProperty);
                        if(minValue != null && !minValue.equals("null") && !minValue.equals("") && !minValue.equals("NA")){
                            logicalParamPoint.setMinVal(minValue);
                        }
                    }else if(bacnetProperty.getName().equals("MAX_PRESENT_VALUE")){
                        String minValue = getValue(bacnetProperty);
                        if(minValue != null && !minValue.equals("null") && !minValue.equals("") && !minValue.equals("NA")){
                            logicalParamPoint.setMaxVal(minValue);
                        }
                    }else {
                        try {
                            String value = String.valueOf(bacnetProperty.getSelectedValue());
                            CcuLog.d(TAG, "tag to add-->" +bacnetProperty.getName().toLowerCase() + " value-->"+HStr.make(value));
                            if(value != null && !value.equals("null") && !value.equals("") && !value.equals("NA")){
                                logicalParamPoint.addTag(bacnetProperty.getName().toLowerCase(), HStr.make(value));
                            }
                        }catch (Exception e) {
                            CcuLog.d(TAG, "hit exception 1-->" + e.getMessage());
                        }
                    }

                });

                if(isWritable){
                    logicalParamPoint.addMarker("writable");
                    logicalParamPoint.addTag("defaultWriteLevel", HStr.make(point.getDefaultWriteLevel()));
                }

                if(isPointHistorized){
                    logicalParamPoint.addMarker("his");
                }

                logicalParamPoint.setHisInterpolate(point.getHisInterpolate().toLowerCase());
                Point logicalPoint = logicalParamPoint.build();
                String logicalParamId = CCUHsApi.getInstance().addPoint(logicalPoint);

                if(hasPresentValue.get()){
                    try {
                        if(presentValue.get() != null && !presentValue.get().equals("null") && !presentValue.get().equals("") && !presentValue.get().equals("NA")){
                            Log.d(TAG, "write present value to the point id-->"+logicalParamId+" value-->"+presentValue +"<--is writable-->"+isWritable);
                            CCUHsApi.getInstance().writeDefaultValById(logicalParamId, Double.parseDouble(presentValue.get()));
                            CCUHsApi.getInstance().writeHisValById(equipScheduleTypeId,Double.parseDouble(presentValue.get()));
                        }
                    }catch (Exception e) {
                        CcuLog.d(TAG, "hit exception 4-->" + e.getMessage());
                    }

//                    if(isWritable.get()){
//                        CCUHsApi.getInstance().writeDefaultValById(logicalParamId, presentValue.get());
//                    }else{
//                        CCUHsApi.getInstance().writeHisValById(logicalParamId, Double.parseDouble(presentValue.get()));
//                    }

                }
                CcuLog.d(TAG, "point added to haystack-->"+logicalPoint.getDisplayName()+"--id-->"+logicalParamId);
            }
        }catch (Exception e) {
            e.printStackTrace();
            CcuLog.d(TAG, "hit exception-->"+e.getMessage());
        }

        CCUHsApi.getInstance().syncEntityTree();
        CcuLog.d(TAG, "##--bacnet client finished points creation--");
        return equipmentRef;
    }

    private static String getBacNetType(String objectType){
        if(objectType.equalsIgnoreCase("ANALOG_VALUE")){
            return "AnalogValue";
        }else if(objectType.equalsIgnoreCase("BINARY_VALUE")){
            return "BinaryValue";
        }else if(objectType.equalsIgnoreCase("MULTI_STATE_VALUE")){
            return "MultiStateValue";
        }
        return "AnalogValue";
    }

    private static String getValue(BacnetProperty bacnetProperty) {
        String minValue = bacnetProperty.getDefaultValue();
        if(bacnetProperty.getSelectedValue() == 1){
            minValue = bacnetProperty.getFetchedValue();
        }
        return minValue;
    }

    public void updateHaystackPoints(String equipRef, List<BacnetPoint> configuredParams) {
        for (BacnetPoint bacnetPoint : configuredParams) {
            HDict pointRead = CCUHsApi.getInstance().readHDictById(bacnetPoint.getId());
            if(pointRead != null) {
                Point logicalPoint = new Point.Builder().setHDict(pointRead).build();
                if (bacnetPoint.getProtocolData().getBacnet().getDisplayInUIDefault()) {
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
}
