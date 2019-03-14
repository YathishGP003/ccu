package a75f.io.logic.bo.building.system.dab;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import a75.io.algos.GenericPIController;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Created by samjithsadasivan on 3/13/19.
 */

public class DabEquip
{
    public int nodeAddr;
    ProfileType profileType;
    
    double damperPos= 0;
    
    GenericPIController damperController;
    CCUHsApi hayStack = CCUHsApi.getInstance();
    String equipRef = null;
    
    public DabEquip(ProfileType type, int node) {
        profileType = type;
        nodeAddr = node;
        
    }
    
    public void init() {
        damperController = new GenericPIController();
        damperController.setMaxAllowedError(hayStack.readDefaultVal("point and proportional and range and equipRef == \""+equipRef+"\""));
        damperController.setIntegralGain(TunerUtil.readTunerValByQuery("pid and igain and equipRef == \"" + equipRef + "\""));
        damperController.setProportionalGain(TunerUtil.readTunerValByQuery("pid and pgain and equipRef == \""+equipRef+"\""));
        damperController.setIntegralMaxTimeout((int)TunerUtil.readTunerValByQuery("pid and itimeout and equipRef == \""+equipRef+"\""));
    
    }
    
    public void createEntities(DabProfileConfiguration config, String floorRef, String roomRef)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis+"-VAV-"+nodeAddr;
        String ahuRef = null;
        HashMap systemEquip = CCUHsApi.getInstance().read("equip and system");
        if (systemEquip != null && systemEquip.size() > 0) {
            ahuRef = systemEquip.get("id").toString();
        }
    
        Equip.Builder b = new Equip.Builder()
                                  .setSiteRef(siteRef)
                                  .setDisplayName(equipDis)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef)
                                  .setProfile(profileType.name())
                                  .setPriority(config.getPriority().name())
                                  .addMarker("equip").addMarker("dab").addMarker("zone").addMarker("equipHis")
                                  .setAhuRef(ahuRef)
                                  .setTz(tz)
                                  .setGroup(String.valueOf(nodeAddr));
        String equipRef = CCUHsApi.getInstance().addEquip(b.build());
        BuildingTuners.getInstance().addEquipDabTuners(siteDis + "-DAB-" + nodeAddr, equipRef);
        createDabConfigPoints(config, equipRef);
    
        Point damperPos = new Point.Builder()
                                  .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-damperPos")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef)
                                  .addMarker("damper").addMarker("dab").addMarker("base").addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone").addMarker("equipHis")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setTz(tz)
                                  .build();
        String dpID = CCUHsApi.getInstance().addPoint(damperPos);
    
        Point normalizedDamperPos = new Point.Builder()
                                            .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-normalizedDamperPos")
                                            .setEquipRef(equipRef)
                                            .setSiteRef(siteRef)
                                            .setRoomRef(roomRef)
                                            .setFloorRef(floorRef)
                                            .addMarker("damper").addMarker("dab").addMarker("normalized").addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone").addMarker("equipHis")
                                            .setGroup(String.valueOf(nodeAddr))
                                            .setTz(tz)
                                            .build();
        String normalizedDPId = CCUHsApi.getInstance().addPoint(normalizedDamperPos);
    
        Point currentTemp = new Point.Builder()
                                    .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-currentTemp")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(roomRef)
                                    .setFloorRef(floorRef)
                                    .addMarker("zone").addMarker("dab")
                                    .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical").addMarker("equipHis")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("\u00B0F")
                                    .setTz(tz)
                                    .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);
    
        Point humidity = new Point.Builder()
                                 .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-humidity")
                                 .setEquipRef(equipRef)
                                 .setSiteRef(siteRef)
                                 .setRoomRef(roomRef)
                                 .setFloorRef(floorRef)
                                 .addMarker("zone").addMarker("dab")
                                 .addMarker("air").addMarker("humidity").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical").addMarker("equipHis")
                                 .setGroup(String.valueOf(nodeAddr))
                                 .setTz(tz)
                                 .build();
        String humidityId = CCUHsApi.getInstance().addPoint(humidity);
    
        Point co2 = new Point.Builder()
                            .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-co2")
                            .setEquipRef(equipRef)
                            .setSiteRef(siteRef)
                            .setRoomRef(roomRef)
                            .setFloorRef(floorRef)
                            .addMarker("zone").addMarker("dab")
                            .addMarker("air").addMarker("co2").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical").addMarker("equipHis")
                            .setGroup(String.valueOf(nodeAddr))
                            .setTz(tz)
                            .build();
        String co2Id = CCUHsApi.getInstance().addPoint(co2);
    
        Point voc = new Point.Builder()
                            .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-voc")
                            .setEquipRef(equipRef)
                            .setSiteRef(siteRef)
                            .setRoomRef(roomRef)
                            .setFloorRef(floorRef)
                            .addMarker("zone").addMarker("dab")
                            .addMarker("air").addMarker("voc").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical").addMarker("equipHis")
                            .setGroup(String.valueOf(nodeAddr))
                            .setTz(tz)
                            .build();
        String vocId = CCUHsApi.getInstance().addPoint(voc);
    
        Point desiredTemp = new Point.Builder()
                                    .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-desiredTemp")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(roomRef)
                                    .setFloorRef(floorRef)
                                    .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("dab").addMarker("average")
                                    .addMarker("sp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("userIntent")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("\u00B0F")
                                    .setTz(tz)
                                    .build();
        String dtId = CCUHsApi.getInstance().addPoint(desiredTemp);
    
        Point desiredTempCooling = new Point.Builder()
                                           .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-desiredTempCooling")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("dab").addMarker("cooling")
                                           .addMarker("sp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("userIntent")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
        CCUHsApi.getInstance().addPoint(desiredTempCooling);
    
        Point desiredTempHeating = new Point.Builder()
                                           .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-desiredTempHeating")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("dab").addMarker("heating")
                                           .addMarker("sp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("userIntent")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
        CCUHsApi.getInstance().addPoint(desiredTempHeating);
    
        SmartNode device = new SmartNode(nodeAddr, siteRef, floorRef, roomRef, equipRef);
        device.analog1Out.setPointRef(normalizedDPId);
        device.currentTemp.setPointRef(ctID);
        device.currentTemp.setEnabled(true);
        device.humidity.setPointRef(humidityId);
        device.humidity.setEnabled(true);
        device.co2.setPointRef(co2Id);
        device.co2.setEnabled(true);
        device.voc.setPointRef(vocId);
        device.voc.setEnabled(true);
        device.desiredTemp.setPointRef(dtId);
        device.desiredTemp.setEnabled(true);
        for (Output op : config.getOutputs()) {
            switch (op.getPort()) {
                case ANALOG_OUT_ONE:
                    device.analog1Out.setType(op.getAnalogActuatorType());
                    break;
            }
        }
        device.analog1Out.setEnabled(config.isOpConfigured(Port.ANALOG_OUT_ONE));
        
    
        device.addPointsToDb();
    
        
        setCurrentTemp(0);
        setDamperPos(0);
        setDesiredTempCooling(73.0);
        setDesiredTemp(72.0);
        setDesiredTempHeating(71.0);
        setHumidity(0);
        setCO2(0);
        setVOC(0);
    
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    
    public void createDabConfigPoints(DabProfileConfiguration config, String equipRef) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis+"-DAB-"+nodeAddr;
        String tz = siteMap.get("tz").toString();
        
        Point damperType = new Point.Builder()
                                   .setDisplayName(equipDis+"-damperType")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                   .addMarker("damper").addMarker("type").addMarker("sp")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setTz(tz)
                                   .build();
        String damperTypeId = CCUHsApi.getInstance().addPoint(damperType);
        CCUHsApi.getInstance().writeDefaultValById(damperTypeId, (double)config.damperType);
        
        Point damperSize = new Point.Builder()
                                   .setDisplayName(equipDis+"-damperSize")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                   .addMarker("damper").addMarker("size").addMarker("sp")
                                   .setUnit("\u00B0")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setTz(tz)
                                   .build();
        String damperSizeId = CCUHsApi.getInstance().addPoint(damperSize);
        CCUHsApi.getInstance().writeDefaultValById(damperSizeId, (double)config.damperSize);
        
        Point damperShape = new Point.Builder()
                                    .setDisplayName(equipDis+"-damperShape")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                    .addMarker("damper").addMarker("shape").addMarker("sp")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setTz(tz)
                                    .build();
        String damperShapeId = CCUHsApi.getInstance().addPoint(damperShape);
        CCUHsApi.getInstance().writeDefaultValById(damperShapeId, (double)config.damperShape);
        
        
        Point enableOccupancyControl = new Point.Builder()
                                               .setDisplayName(equipDis+"-enableOccupancyControl")
                                               .setEquipRef(equipRef)
                                               .setSiteRef(siteRef)
                                               .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                               .addMarker("enable").addMarker("occupancy").addMarker("control").addMarker("sp")
                                               .setGroup(String.valueOf(nodeAddr))
                                               .setTz(tz)
                                               .build();
        String enableOccupancyControlId = CCUHsApi.getInstance().addPoint(enableOccupancyControl);
        CCUHsApi.getInstance().writeDefaultValById(enableOccupancyControlId, config.enableOccupancyControl == true ? 1.0 :0);
        
        Point enableCO2Control = new Point.Builder()
                                         .setDisplayName(equipDis+"-enableCO2Control")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                         .addMarker("enable").addMarker("co2").addMarker("control").addMarker("sp")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String enableCO2ControlId = CCUHsApi.getInstance().addPoint(enableCO2Control);
        CCUHsApi.getInstance().writeDefaultValById(enableCO2ControlId, config.enableCO2Control == true ? 1.0 :0);
        
        Point enableIAQControl = new Point.Builder()
                                         .setDisplayName(equipDis+"-enableIAQControl")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                         .addMarker("enable").addMarker("iaq").addMarker("control").addMarker("sp")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String enableIAQControlId = CCUHsApi.getInstance().addPoint(enableIAQControl);
        CCUHsApi.getInstance().writeDefaultValById(enableIAQControlId, config.enableIAQControl == true ? 1.0 :0);
        
        Point zonePriority = new Point.Builder()
                                     .setDisplayName(equipDis+"-zonePriority")
                                     .setEquipRef(equipRef)
                                     .setSiteRef(siteRef)
                                     .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                     .addMarker("priority").addMarker("sp")
                                     .setGroup(String.valueOf(nodeAddr))
                                     .setTz(tz)
                                     .build();
        String zonePriorityId = CCUHsApi.getInstance().addPoint(zonePriority);
        CCUHsApi.getInstance().writeDefaultValById(zonePriorityId, (double)config.getPriority().ordinal());
        
        Point temperatureOffset = new Point.Builder()
                                          .setDisplayName(equipDis+"-temperatureOffset")
                                          .setEquipRef(equipRef)
                                          .setSiteRef(siteRef)
                                          .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                          .addMarker("temperature").addMarker("offset").addMarker("sp")
                                          .setGroup(String.valueOf(nodeAddr))
                                          .setTz(tz)
                                          .build();
        String temperatureOffsetId = CCUHsApi.getInstance().addPoint(temperatureOffset);
        CCUHsApi.getInstance().writeDefaultValById(temperatureOffsetId, (double)config.temperaturOffset);
        
        Point damperMinCooling = new Point.Builder()
                                         .setDisplayName(equipDis+"-minCoolingDamperPos")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("dab").addMarker("damper").addMarker("min").addMarker("cooling").addMarker("pos")
                                         .addMarker("sp").addMarker("writable").addMarker("zone")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String damperMinCoolingId = CCUHsApi.getInstance().addPoint(damperMinCooling);
        CCUHsApi.getInstance().writeDefaultValById(damperMinCoolingId, (double)config.minDamperCooling);
        
        Point damperMaxCooling = new Point.Builder()
                                         .setDisplayName(equipDis+"-maxCoolingDamperPos")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("dab").addMarker("damper").addMarker("max").addMarker("cooling").addMarker("pos")
                                         .addMarker("sp").addMarker("writable").addMarker("zone")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String damperMaxCoolingId = CCUHsApi.getInstance().addPoint(damperMaxCooling);
        CCUHsApi.getInstance().writeDefaultValById(damperMaxCoolingId, (double)config.maxDamperCooling);
        
        
        Point damperMinHeating = new Point.Builder()
                                         .setDisplayName(equipDis+"-minHeatingDamperPos")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("dab").addMarker("damper").addMarker("min").addMarker("heating").addMarker("pos")
                                         .addMarker("sp").addMarker("writable").addMarker("zone")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String damperMinHeatingId = CCUHsApi.getInstance().addPoint(damperMinHeating);
        CCUHsApi.getInstance().writeDefaultValById(damperMinHeatingId, (double)config.minDamperHeating);
        
        Point damperMaxHeating = new Point.Builder()
                                         .setDisplayName(equipDis+"-maxHeatingDamperPos")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("dab").addMarker("damper").addMarker("max").addMarker("heating").addMarker("pos")
                                         .addMarker("sp").addMarker("writable").addMarker("zone")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String damperMaxHeatingId = CCUHsApi.getInstance().addPoint(damperMaxHeating);
        CCUHsApi.getInstance().writeDefaultValById(damperMaxHeatingId, (double) config.maxDamperHeating);
    }
    
    public DabProfileConfiguration getProfileConfiguration() {
        DabProfileConfiguration config = new DabProfileConfiguration();
        config.minDamperCooling = ((int)getDamperLimit("cooling","min"));
        config.maxDamperCooling = ((int)getDamperLimit("cooling","max"));
        config.minDamperHeating = ((int)getDamperLimit("heating","min"));
        config.maxDamperHeating = ((int)getDamperLimit("heating","max"));
    
        config.damperType = (int)getConfigNumVal("damper and type");
        config.damperSize = (int)getConfigNumVal("damper and size");
        config.damperShape = (int)getConfigNumVal("damper and shape");
        config.enableOccupancyControl = getConfigNumVal("enable and occupancy") > 0 ? true : false ;
        config.enableCO2Control = getConfigNumVal("enable and co2") > 0 ? true : false ;
        config.enableIAQControl = getConfigNumVal("enable and iaq") > 0 ? true : false ;
        config.setPriority(ZonePriority.values()[(int)getConfigNumVal("priority")]);
        config.temperaturOffset = getConfigNumVal("temperature and offset");
    
        config.setNodeType(NodeType.SMART_NODE);//TODO - revisit
    
    
        RawPoint a1 = SmartNode.getPhysicalPoint(nodeAddr, Port.ANALOG_OUT_ONE.toString());
        if (a1 != null && a1.getEnabled()) {
            Output analogOne = new Output();
            analogOne.setAddress((short)nodeAddr);
            analogOne.setPort(Port.ANALOG_OUT_ONE);
            analogOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.getEnum(a1.getType());
            config.getOutputs().add(analogOne);
        }
        return config;
    }
    
    public void update(DabProfileConfiguration config) {
        for (Output op : config.getOutputs()) {
            switch (op.getPort()) {
                case ANALOG_OUT_ONE:
                    CcuLog.d(L.TAG_CCU_ZONE, " Update analog" + op.getPort() + " type " + op.getAnalogActuatorType());
                    SmartNode.updatePhysicalPointType(nodeAddr, op.getPort().toString(), op.getAnalogActuatorType());
                    break;
            }
        }
        
    
        setConfigNumVal("damper and type",config.damperType);
        setConfigNumVal("damper and size",config.damperSize);
        setConfigNumVal("damper and shape",config.damperShape);
        setConfigNumVal("enable and occupancy",config.enableOccupancyControl == true ? 1.0 : 0);
        setConfigNumVal("enable and co2",config.enableCO2Control == true ? 1.0 : 0);
        setConfigNumVal("enable and iaq",config.enableCO2Control == true ? 1.0 : 0);
        setConfigNumVal("priority",config.getPriority().ordinal());
        setConfigNumVal("temperature and offset",config.temperaturOffset);
        setDamperLimit("cooling","min",config.minDamperCooling);
        setDamperLimit("cooling","max",config.maxDamperCooling);
        setDamperLimit("heating","min",config.minDamperHeating);
        setDamperLimit("heating","max",config.maxDamperHeating);
    }
    
    public void setConfigNumVal(String tags,double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and zone and config and dab and "+tags+" and group == \""+nodeAddr+"\"", val);
    }
    
    public double getConfigNumVal(String tags) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and dab and "+tags+" and group == \""+nodeAddr+"\"");
    }
    
    public double getCurrentTemp()
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and group == \""+nodeAddr+"\"");
    }
    public void setCurrentTemp(double roomTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and current and group == \""+nodeAddr+"\"", roomTemp);
    }
    
    public double getHumidity()
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and humidity and sensor and current and group == \""+nodeAddr+"\"");
    }
    public void setHumidity(double humidity)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and humidity and sensor and current and group == \""+nodeAddr+"\"", humidity);
    }
    
    public double getCO2()
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and co2 and sensor and current and group == \""+nodeAddr+"\"");
    }
    public void setCO2(double co2)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and co2 and sensor and current and group == \""+nodeAddr+"\"", co2);
    }
    
    public double getVOC()
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and voc and sensor and current and group == \""+nodeAddr+"\"");
    }
    public void setVOC(double voc)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and voc and sensor and current and group == \""+nodeAddr+"\"", voc);
    }
    
    public double getDesiredTemp()
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and average and sp and group == \"" + nodeAddr + "\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        return CCUHsApi.getInstance().readDefaultValById(id);
    }
    public void setDesiredTemp(double desiredTemp)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and average and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }
    
    public double getDesiredTempCooling()
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and cooling and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    public void setDesiredTempCooling(double desiredTemp)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and cooling and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }
    
    public double getDesiredTempHeating()
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and heating and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    
    public void setDesiredTempHeating(double desiredTemp)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and heating and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }
    
    public double getDamperLimit(String coolHeat, String minMax)
    {
        Log.d("CCU", " getDamperLimit " + coolHeat + " minMax");
        ArrayList points = CCUHsApi.getInstance().readAll("point and config and damper and pos and "+coolHeat+" and "+minMax+" and group == \""+nodeAddr+"\"");
        if (points.size() == 0) {
            return 0;
        }
        
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        return CCUHsApi.getInstance().readDefaultValById(id);
    }
    public void setDamperLimit(String coolHeat, String minMax, double val)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and damper and pos and "+coolHeat+" and "+minMax+" and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, val);
    }
    
    public double getDamperPos()
    {
        return damperPos;
    }
    public void setDamperPos(double damperPos)
    {
        this.damperPos = damperPos;
        CCUHsApi.getInstance().writeHisValByQuery("point and damper and base and cmd and group == \""+nodeAddr+"\"", damperPos);
    }
}
