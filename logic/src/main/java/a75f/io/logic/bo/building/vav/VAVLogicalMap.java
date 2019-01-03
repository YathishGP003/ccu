package a75f.io.logic.bo.building.vav;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75.io.algos.tr.TrimResponseRequest;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.ParallelFanVavUnit;
import a75f.io.logic.bo.building.hvac.SeriesFanVavUnit;
import a75f.io.logic.bo.building.hvac.VavUnit;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.tuners.BuildingTuners;
/**
 * Created by samjithsadasivan on 6/21/18.
 */

/**
 * A Profile logical Map represents the logical side of a haystack Equip entity.
 * It acts as a container of profile's PI controllers and TR Request objects and it interfaces profile with the haystack.
 * Current design requires only one equip/map per profile, but map/list of LogicalMap
 * per profile is maintained to support any requirement of adding multiple equips/devices per profile.
 */
public class VAVLogicalMap
{
    //TODO - Tuners
    int    integralMaxTimeout = 15;
    int proportionalSpread = 20;
    double proportionalGain = 0.5;
    double integralGain = 0.5;
    
    double      currentTemp;
    double desiredTemp;
    double supplyAirTemp;
    double dischargeTemp;
    double co2;
    double dischargeSp;
    
    double staticPressure;
    
    VavUnit             vavUnit;
    ControlLoop         coolingLoop;
    ControlLoop         heatingLoop;
    CO2Loop             co2Loop;
    GenericPIController valveController;// Use GenericPI as we need unmodulated op.
    
    public TrimResponseRequest satResetRequest;
    public TrimResponseRequest co2ResetRequest;
    public TrimResponseRequest spResetRequest;
    public TrimResponseRequest hwstResetRequest;
    
    int nodeAddr;
    ProfileType profileType;
    
    public VAVLogicalMap(ProfileType T, int node) {
        
        coolingLoop = new ControlLoop();
        heatingLoop = new ControlLoop();
        co2Loop = new CO2Loop();
        valveController = new GenericPIController();
        valveController.setIntegralMaxTimeout(integralMaxTimeout);
        valveController.setMaxAllowedError(proportionalSpread);
        valveController.setProportionalGain(proportionalGain);
        valveController.setIntegralGain(integralGain);
        
        satResetRequest = new TrimResponseRequest();
        co2ResetRequest = new TrimResponseRequest();
        spResetRequest = new TrimResponseRequest();
        hwstResetRequest = new TrimResponseRequest();
    
        profileType = T;
        switch (T) {
            case VAV_REHEAT:
                vavUnit = new VavUnit();
                break;
            case VAV_SERIES_FAN:
                vavUnit = new SeriesFanVavUnit();
                break;
            case VAV_PARALLEL_FAN:
                vavUnit = new ParallelFanVavUnit();
                break;
        }
        nodeAddr = node;
        //createHaystackPoints();
    }
    
    public void createHaystackPoints(VavProfileConfiguration config, String floor, String room) {
        
        //String floor = L.ccu().getFloorRef((short)nodeAddr);
        //String room = L.ccu().getZoneRef((short)nodeAddr);
        
        //Create Logical points
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis+"-VAV-"+nodeAddr;
        Equip v = new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(equipDis)
                          .setZoneRef(room)
                          .setFloorRef(floor)
                          .setProfile(profileType.name())
                          .setPriority(config.getPriority().name())
                          .addMarker("equip").addMarker("vav").addMarker("zone").addMarker("equipHis")
                          .setTz(tz)
                          .setGroup(String.valueOf(nodeAddr))
                          .build();
        String equipRef = CCUHsApi.getInstance().addEquip(v);
        
        BuildingTuners.getInstance().addEquipVavTuners(siteDis+"-VAV-"+nodeAddr, equipRef, config);
    
        Point datPoint = new Point.Builder()
                                .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-DischargeAirTemp")
                                .setEquipRef(equipRef)
                                .setSiteRef(siteRef)
                                .setZoneRef(room)
                                .setFloorRef(floor)
                                .addMarker("discharge")
                                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his").addMarker("logical").addMarker("zone").addMarker("equipHis")
                                .setGroup(String.valueOf(nodeAddr))
                                .setUnit("\u00B0F")
                                .setTz(tz)
                                .build();
        
        String datID = CCUHsApi.getInstance().addPoint(datPoint);
    
        Point eatPoint = new Point.Builder()
                                .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-EnteringAirTemp")
                                .setEquipRef(equipRef)
                                .setSiteRef(siteRef)
                                .setZoneRef(room)
                                .setFloorRef(floor)
                                .addMarker("entering")
                                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his").addMarker("logical").addMarker("zone").addMarker("equipHis")
                                .setGroup(String.valueOf(nodeAddr))
                                .setUnit("\u00B0F")
                                .setTz(tz)
                                .build();
        String eatID = CCUHsApi.getInstance().addPoint(eatPoint);
    
        Point damperPos = new Point.Builder()
                                .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-DamperPos")
                                .setEquipRef(equipRef)
                                .setSiteRef(siteRef)
                                .setZoneRef(room)
                                .setFloorRef(floor)
                                .addMarker("damper").addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone").addMarker("equipHis")
                                .setGroup(String.valueOf(nodeAddr))
                                .setUnit("\u00B0F")
                                .setTz(tz)
                                .build();
    
        String dpID = CCUHsApi.getInstance().addPoint(damperPos);
    
        Point reheatPos = new Point.Builder()
                                  .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-ReheatPos")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setZoneRef(room)
                                  .setFloorRef(floor)
                                  .addMarker("reheat")
                                  .addMarker("water").addMarker("valve").addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone").addMarker("equipHis")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String rhID = CCUHsApi.getInstance().addPoint(reheatPos);
    
        Point currentTemp = new Point.Builder()
                                  .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-currentTemp")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setZoneRef(room)
                                  .setFloorRef(floor)
                                  .addMarker("zone")
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical").addMarker("equipHis")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);
    
        Point desiredTemp = new Point.Builder()
                                    .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-desiredTemp")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setZoneRef(room)
                                    .setFloorRef(floor)
                                    .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired")
                                    .addMarker("sp").addMarker("writable").addMarker("his").addMarker("equipHis")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("\u00B0F")
                                    .setTz(tz)
                                    .build();
        CCUHsApi.getInstance().addPoint(desiredTemp);
    
        Point heatingLoopOp = new Point.Builder()
                                    .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-heatingLoopOp")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setZoneRef(room)
                                    .setFloorRef(floor)
                                    .addMarker("heating").addMarker("loop").addMarker("sp").addMarker("his").addMarker("zone").addMarker("equipHis")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("\u00B0F")
                                    .setTz(tz)
                                    .build();
        CCUHsApi.getInstance().addPoint(heatingLoopOp);
    
        Point coolingLoopOp = new Point.Builder()
                                      .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-coolingLoopOp")
                                      .setEquipRef(equipRef)
                                      .setSiteRef(siteRef)
                                      .setZoneRef(room)
                                      .setFloorRef(floor)
                                      .addMarker("cooling").addMarker("loop").addMarker("sp").addMarker("his").addMarker("zone").addMarker("equipHis")
                                      .setGroup(String.valueOf(nodeAddr))
                                      .setUnit("\u00B0F")
                                      .setTz(tz)
                                      .build();
        CCUHsApi.getInstance().addPoint(coolingLoopOp);
    
        Point dischargeSp = new Point.Builder()
                                      .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-dischargeSp")
                                      .setEquipRef(equipRef)
                                      .setSiteRef(siteRef)
                                      .setZoneRef(room)
                                      .setFloorRef(floor)
                                      .addMarker("discharge").addMarker("air").addMarker("temp").addMarker("zone").addMarker("equipHis")
                                      .addMarker("sp").addMarker("his")
                                      .setGroup(String.valueOf(nodeAddr))
                                      .setUnit("\u00B0F")
                                      .setTz(tz)
                                      .build();
        CCUHsApi.getInstance().addPoint(dischargeSp);
    
        Point satRequestPercentage = new Point.Builder()
                                    .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-satRequestPercentage")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setZoneRef(room)
                                    .setFloorRef(floor)
                                    .addMarker("request").addMarker("hour").addMarker("cumulative")
                                    .addMarker("tr").addMarker("supply").addMarker("air").addMarker("temp").addMarker("his").addMarker("zone").addMarker("equipHis")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("\u00B0F")
                                    .setTz(tz)
                                    .build();
        CCUHsApi.getInstance().addPoint(satRequestPercentage);
    
        Point co2RequestPercentage = new Point.Builder()
                                             .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-co2RequestPercentage")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setZoneRef(room)
                                             .setFloorRef(floor)
                                             .addMarker("request").addMarker("hour").addMarker("cumulative")
                                             .addMarker("tr").addMarker("co2").addMarker("temp").addMarker("his").addMarker("zone").addMarker("equipHis")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setUnit("\u00B0F")
                                             .setTz(tz)
                                             .build();
        CCUHsApi.getInstance().addPoint(co2RequestPercentage);
    
        Point hwstRequestPercentage = new Point.Builder()
                                             .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-hwstRequestPercentage")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setZoneRef(room)
                                             .setFloorRef(floor)
                                             .addMarker("request").addMarker("hour").addMarker("cumulative")
                                             .addMarker("tr").addMarker("hwst").addMarker("his").addMarker("zone").addMarker("equipHis")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setUnit("\u00B0F")
                                             .setTz(tz)
                                             .build();
        CCUHsApi.getInstance().addPoint(hwstRequestPercentage);
    
        Point pressureRequestPercentage = new Point.Builder()
                                             .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-pressureRequestPercentage")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setZoneRef(room)
                                             .setFloorRef(floor)
                                             .addMarker("request").addMarker("hour").addMarker("cumulative")
                                             .addMarker("tr").addMarker("pressure").addMarker("his").addMarker("zone").addMarker("equipHis")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setUnit("\u00B0F")
                                             .setTz(tz)
                                             .build();
        CCUHsApi.getInstance().addPoint(pressureRequestPercentage);
    
        Point damperMinCooling = new Point.Builder()
                                         .setDisplayName(equipDis+"-MinCoolingDamperPos")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("vav").addMarker("damper").addMarker("min").addMarker("cooling").addMarker("pos")
                                         .addMarker("sp").addMarker("writable").addMarker("zone")
                                         .setUnit("\u00B0")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String damperMinCoolingId = CCUHsApi.getInstance().addPoint(damperMinCooling);
        CCUHsApi.getInstance().writeDefaultValById(damperMinCoolingId, (double)config.getMinDamperCooling());
    
        Point damperMaxCooling = new Point.Builder()
                                         .setDisplayName(equipDis+"-MaxCoolingDamperPos")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("vav").addMarker("damper").addMarker("max").addMarker("cooling").addMarker("pos")
                                         .addMarker("sp").addMarker("writable").addMarker("zone")
                                         .setUnit("\u00B0")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String damperMaxCoolingId = CCUHsApi.getInstance().addPoint(damperMaxCooling);
        CCUHsApi.getInstance().writeDefaultValById(damperMaxCoolingId, (double)config.getMaxDamperCooliing());
        
    
        Point damperMinHeating = new Point.Builder()
                                         .setDisplayName(equipDis+"-MinHeatingDamperPos")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("vav").addMarker("damper").addMarker("min").addMarker("heating").addMarker("pos")
                                         .addMarker("sp").addMarker("writable").addMarker("zone")
                                         .setUnit("\u00B0")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String damperMinHeatingId = CCUHsApi.getInstance().addPoint(damperMinHeating);
        CCUHsApi.getInstance().writeDefaultValById(damperMinHeatingId, (double)config.getMinDamperHeating());
    
        Point damperMaxHeating = new Point.Builder()
                                         .setDisplayName(equipDis+"-MaxHeatingDamperPos")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("vav").addMarker("damper").addMarker("max").addMarker("heating").addMarker("pos")
                                         .addMarker("sp").addMarker("writable").addMarker("zone")
                                         .setUnit("\u00B0")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String damperMaxHeatingId = CCUHsApi.getInstance().addPoint(damperMaxHeating);
        CCUHsApi.getInstance().writeDefaultValById(damperMaxHeatingId, (double) config.getMaxDamperHeating());
        
        //Create Physical points and map
        SmartNode device = new SmartNode(nodeAddr, siteRef, floor, room);
        device.th1In.setPointRef(datID);
        device.th1In.setEnabled(true);
        device.th2In.setPointRef(eatID);
        device.th2In.setEnabled(true);
        device.analog1Out.setPointRef(dpID);
        //device.analog1Out.setEnabled(true);
        device.analog2Out.setPointRef(rhID);
        device.relay1.setPointRef(rhID);
        device.relay2.setPointRef(rhID);
        //device.analog2Out.setEnabled(true);
        device.currentTemp.setPointRef(ctID);
        device.currentTemp.setEnabled(true);
        for (Output op : config.getOutputs()) {
            switch (op.getPort()) {
                case ANALOG_OUT_ONE:
                    device.analog1Out.setType(op.getAnalogActuatorType());
                    break;
                case ANALOG_OUT_TWO:
                    device.analog2Out.setType(op.getAnalogActuatorType());
                    break;
                case RELAY_ONE:
                    device.relay1.setType(op.getRelayActuatorType());
                    break;
                case RELAY_TWO:
                    device.relay2.setType(op.getRelayActuatorType());
                    break;
            }
        }
        device.analog1Out.setEnabled(config.isOpConfigured(Port.ANALOG_OUT_ONE));
        device.analog2Out.setEnabled(config.isOpConfigured(Port.ANALOG_OUT_TWO));
        device.relay1.setEnabled(config.isOpConfigured(Port.RELAY_ONE));
        device.relay2.setEnabled(config.isOpConfigured(Port.RELAY_TWO));
        
        device.addPointsToDb();
        
        //Log.d("VAV", CCUHsApi.getInstance().tagsDb.getDbMap().toString());
    
        //Initialize write array for points, otherwise a read before write will throw exception
        setCurrentTemp(0);
        setDamperPos(0);
        setReheatPos(0);
        setDischargeTemp(0);
        setSupplyAirTemp(0);
        setDesiredTemp(72.0);
    
        new Thread() {
            @Override
            public void run() {
                super.run();
                CCUHsApi.getInstance().syncEntityTree();
            }
        }.start();
        
        
    }
    
    public void updateHaystackPoints(VavProfileConfiguration config) {
        for (Output op : config.getOutputs()) {
            switch (op.getPort()) {
                case ANALOG_OUT_ONE:
                case ANALOG_OUT_TWO:
                    Log.d("CCU"," Update analog"+op.getPort()+" type "+op.getAnalogActuatorType());
                    SmartNode.updatePhysicalPoint(nodeAddr, op.getPort().toString(), op.getAnalogActuatorType());
                    break;
                case RELAY_ONE:
                case RELAY_TWO:
                    SmartNode.updatePhysicalPoint(nodeAddr, op.getPort().toString(), op.getRelayActuatorType());
                    break;
            }
        }
        
        SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_OUT_TWO.name(), config.isOpConfigured(Port.ANALOG_OUT_TWO) );
        SmartNode.setPointEnabled(nodeAddr, Port.RELAY_ONE.name(), config.isOpConfigured(Port.RELAY_ONE) );
        SmartNode.setPointEnabled(nodeAddr, Port.RELAY_TWO.name(), config.isOpConfigured(Port.RELAY_TWO) );
        
        setDamperLimit("cooling","min",config.getMinDamperCooling());
        setDamperLimit("cooling","max",config.getMaxDamperCooliing());
        setDamperLimit("heating","min",config.getMinDamperHeating());
        setDamperLimit("heating","max",config.getMaxDamperHeating());
    }
    
    public void deleteHaystackPoints() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and vav and group == \""+nodeAddr+"\"");
        if (equip != null)
        {
            hayStack.deleteEntityTree(equip.get("id").toString());
        }
        
        HashMap device = hayStack.read("device and addr == \""+nodeAddr+"\"");
        if (device != null)
        {
            hayStack.deleteEntityTree(device.get("id").toString());
        }
    }
    
    public VavProfileConfiguration getProfileConfiguration() {
        VavProfileConfiguration config = new VavProfileConfiguration();
        config.setMinDamperCooling((int)getDamperLimit("cooling","min"));
        config.setMaxDamperCooliing((int)getDamperLimit("cooling","max"));
        config.setMinDamperHeating((int)getDamperLimit("heating","min"));
        config.setMaxDamperHeating((int)getDamperLimit("heating","max"));
        
        config.setNodeType(NodeType.SMART_NODE);//TODO - revisit
        config.setPriority(ZonePriority.LOW);
        
        RawPoint a1 = SmartNode.getPhysicalPoint(nodeAddr, Port.ANALOG_OUT_ONE.toString());
        if (a1 != null && a1.getEnabled()) {
            Output analogOne = new Output();
            analogOne.setAddress((short)nodeAddr);
            analogOne.setPort(Port.ANALOG_OUT_ONE);
            analogOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.getEnum(a1.getType());
            config.getOutputs().add(analogOne);
        }
    
        RawPoint a2 = SmartNode.getPhysicalPoint(nodeAddr, Port.ANALOG_OUT_TWO.toString());
        if (a2 != null  && a2.getEnabled()) {
            Output analogTwo = new Output();
            analogTwo.setAddress((short)nodeAddr);
            analogTwo.setPort(Port.ANALOG_OUT_TWO);
            Log.d("CCU"," Get analog out 2 type "+a2.getType());
            analogTwo.mOutputAnalogActuatorType = OutputAnalogActuatorType.getEnum(a2.getType());
            config.getOutputs().add(analogTwo);
        }
    
        RawPoint r1 = SmartNode.getPhysicalPoint(nodeAddr, Port.RELAY_ONE.toString());
        if (r1 != null && r1.getEnabled()) {
            Output relay1 = new Output();
            relay1.setAddress((short)nodeAddr);
            relay1.setPort(Port.RELAY_ONE);
            relay1.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r1.getType());
            config.getOutputs().add(relay1);
        }
    
        RawPoint r2 = SmartNode.getPhysicalPoint(nodeAddr, Port.RELAY_TWO.toString());
        if (r2 != null && r2.getEnabled()) {
            Output relay2 = new Output();
            relay2.setAddress((short)nodeAddr);
            relay2.setPort(Port.RELAY_TWO);
            relay2.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r2.getType());
            config.getOutputs().add(relay2);
        }
        return config;
    }
    
    public double getCurrentTemp()
    {
        currentTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and group == \""+nodeAddr+"\"");
        return currentTemp;
    }
    public void setCurrentTemp(double roomTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and current and group == \""+nodeAddr+"\"", roomTemp);
        this.currentTemp = roomTemp;
    }
    
    public double getDesiredTemp()
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        desiredTemp = CCUHsApi.getInstance().readDefaultValById(id);
        return desiredTemp;
    }
    public void setDesiredTemp(double desiredTemp)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
        this.desiredTemp = desiredTemp;
    }
    
    public double getDamperLimit(String coolHeat, String minMax)
    {
        Log.d("CCU"," getDamperLimit "+coolHeat+" minMax");
        ArrayList points = CCUHsApi.getInstance().readAll("point and config and damper and pos and "+coolHeat+" and "+minMax+" and group == \""+nodeAddr+"\"");
        if (points.size() == 0) {
            Log.d("CCU"," getDamperLimit 0");
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
    
    public double getSupplyAirTemp()
    {
        supplyAirTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and entering and group == \""+nodeAddr+"\"");
        return supplyAirTemp;
    }
    public void setSupplyAirTemp(double supplyAirTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and entering and group == \""+nodeAddr+"\"", supplyAirTemp);
        this.supplyAirTemp = supplyAirTemp;
    }
    public double getDischargeTemp()
    {
        dischargeTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and discharge and group == \""+nodeAddr+"\"");
        return dischargeTemp;
    }
    public void setDischargeTemp(double dischargeTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and discharge and group == \""+nodeAddr+"\"", dischargeTemp);
        this.dischargeTemp = dischargeTemp;
    }
    
    public double getDamperPos()
    {
        Double damperpos = CCUHsApi.getInstance().readHisValByQuery("point and air and damper and cmd and group == \""+nodeAddr+"\"");
        this.vavUnit.vavDamper.currentPosition = damperpos.intValue();
        return this.vavUnit.vavDamper.currentPosition;
    }
    public void setDamperPos(double damperPos)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and damper and cmd and group == \""+nodeAddr+"\"", damperPos);
        this.vavUnit.vavDamper.currentPosition = (int)damperPos;
    }
    
    public double getReheatPos()
    {
        Double damperpos = CCUHsApi.getInstance().readHisValByQuery("point and reheat and cmd and group == \""+nodeAddr+"\"");
        this.vavUnit.reheatValve.currentPosition = damperpos.intValue();
        return this.vavUnit.reheatValve.currentPosition;
    }
    public void setReheatPos(double reheatPos)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and reheat and cmd and group == \""+nodeAddr+"\"", reheatPos);
        this.vavUnit.reheatValve.currentPosition = (int)reheatPos;
    }
    
    
    public double getCO2()
    {
        return co2;
    }
    public void setCO2(double co2)
    {
        this.co2 = co2;
    }
    public double getDischargeSp()
    {
        return dischargeSp;
    }
    public void setDischargeSp(double dischargeSp)
    {
        this.dischargeSp = dischargeSp;
    }
    public VavUnit getVavUnit()
    {
        return vavUnit;
    }
    public ControlLoop getCoolingLoop()
    {
        return coolingLoop;
    }
    public ControlLoop getHeatingLoop()
    {
        return heatingLoop;
    }
    public GenericPIController getValveController()
    {
        return valveController;
    }
    public void setValveController(GenericPIController valveController)
    {
        this.valveController = valveController;
    }
    public CO2Loop getCo2Loop()
    {
        return co2Loop;
    }
    public double getStaticPressure()
    {
        return staticPressure;
    }
    public void setStaticPressure(double staticPressure)
    {
        this.staticPressure = staticPressure;
    }
    
    public void updateLoopParams() {
    
        CCUHsApi.getInstance().writeHisValByQuery("point and heating and loop and sp and his and group == \""+nodeAddr+"\"", heatingLoop.getLoopOutput());
    
    
        CCUHsApi.getInstance().writeHisValByQuery("point and cooling and loop and sp and his and group == \""+nodeAddr+"\"", coolingLoop.getLoopOutput());
    
        CCUHsApi.getInstance().writeHisValByQuery("point and discharge and air and temp and sp and his and group == \""+nodeAddr+"\"", dischargeSp);
    
    
        CCUHsApi.getInstance().writeHisValByQuery("point and request and hour and cumulative and tr and " +
                                                  "supply and air and temp and his and group == \""+nodeAddr+"\"", satResetRequest.cumulativeRequestHoursPercent);
    
        CCUHsApi.getInstance().writeHisValByQuery("point and request and hour and cumulative and tr and " +
                                                  "co2 and his and group == \""+nodeAddr+"\"", co2ResetRequest.cumulativeRequestHoursPercent);
        CCUHsApi.getInstance().writeHisValByQuery("point and request and hour and cumulative and tr and " +
                                                  "hwst and his and group == \""+nodeAddr+"\"", hwstResetRequest.cumulativeRequestHoursPercent);
        CCUHsApi.getInstance().writeHisValByQuery("point and request and hour and cumulative and tr and " +
                                                  "pressure and his and group == \""+nodeAddr+"\"", spResetRequest.cumulativeRequestHoursPercent);
    
    
    }
}
