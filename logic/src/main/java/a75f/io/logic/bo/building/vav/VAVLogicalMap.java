package a75f.io.logic.bo.building.vav;

import android.util.Log;

import java.util.HashMap;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75.io.algos.tr.TrimResponseRequest;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.ParallelFanVavUnit;
import a75f.io.logic.bo.building.hvac.SeriesFanVavUnit;
import a75f.io.logic.bo.building.hvac.VavUnit;
import a75f.io.logic.bo.haystack.device.SmartNode;

/**
 * Created by samjithsadasivan on 6/21/18.
 */

public class VAVLogicalMap
{
    //TODO - Tuners
    int    integralMaxTimeout = 15;
    int proportionalSpread = 20;
    double proportionalGain = 0.5;
    double integralGain = 0.5;
    
    double      roomTemp;
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
    
    public void createHaystackPoints() {
    
        //Add site point if not done already.
        HashMap site = CCUHsApi.getInstance().read("site");
        
        //TODO - Unit test
        String floor = L.ccu().getFloor((short)nodeAddr);
        String room = L.ccu().getRoom((short)nodeAddr);
        
        if (site.size() == 0) {
            //TODO - demo
            Site s75f = new Site.Builder()
                                .setDisplayName("75F")
                                .addMarker("site")
                                .setGeoCity("Burnsville")
                                .setGeoState("MN")
                                .setTz("Chicago")
                                .setArea(10000).build();
            CCUHsApi.getInstance().addSite(s75f);
        }
        String tz = "Chicago";
        //Create Logical points
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        Equip v = new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(siteDis+"-VAV-"+nodeAddr)
                          .setRoomRef(room)
                          .setFloorRef(floor)
                          .addMarker("equip")
                          .addMarker("vav")
                          .setGroup(String.valueOf(nodeAddr))
                          .build();
        String equipRef = CCUHsApi.getInstance().addEquip(v);
    
        Point datPoint = new Point.Builder()
                                .setDisplayName(siteDis+"VAV-"+nodeAddr+"-DischargeAirTemp")
                                .setEquipRef(equipRef)
                                .setSiteRef(siteRef)
                                .setRoomRef(room)
                                .setFloorRef(floor)
                                .addMarker("discharge")
                                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his")
                                .setGroup(String.valueOf(nodeAddr))
                                .setUnit("\u00B0F")
                                .setTz(tz)
                                .build();
        
        String datID = CCUHsApi.getInstance().addPoint(datPoint);
    
        Point eatPoint = new Point.Builder()
                                .setDisplayName(siteDis+"VAV-"+nodeAddr+"-EnteringAirTemp")
                                .setEquipRef(equipRef)
                                .setSiteRef(siteRef)
                                .setRoomRef(room)
                                .setFloorRef(floor)
                                .addMarker("entering")
                                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his")
                                .setGroup(String.valueOf(nodeAddr))
                                .setUnit("\u00B0F")
                                .setTz(tz)
                                .build();
        String eatID = CCUHsApi.getInstance().addPoint(eatPoint);
    
        Point damperPos = new Point.Builder()
                                .setDisplayName(siteDis+"VAV-"+nodeAddr+"-DamperPos")
                                .setEquipRef(equipRef)
                                .setSiteRef(siteRef)
                                .setRoomRef(room)
                                .setFloorRef(floor)
                                .addMarker("air")
                                .addMarker("damper").addMarker("cmd").addMarker("his")
                                .setGroup(String.valueOf(nodeAddr))
                                .setUnit("\u00B0F")
                                .setTz(tz)
                                .build();
    
        String dpID = CCUHsApi.getInstance().addPoint(damperPos);
    
        Point reheatPos = new Point.Builder()
                                  .setDisplayName(siteDis+"VAV-"+nodeAddr+"-ReheatPos")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(room)
                                  .setFloorRef(floor)
                                  .addMarker("reheat")
                                  .addMarker("water").addMarker("valve").addMarker("cmd").addMarker("his")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String rhID = CCUHsApi.getInstance().addPoint(reheatPos);
    
        Point currentTemp = new Point.Builder()
                                  .setDisplayName(siteDis+"VAV-"+nodeAddr+"-currentTemp")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(room)
                                  .setFloorRef(floor)
                                  .addMarker("zone")
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current").addMarker("his")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);
    
        Point desiredTemp = new Point.Builder()
                                    .setDisplayName(siteDis+"VAV-"+nodeAddr+"-desiredTemp")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(room)
                                    .setFloorRef(floor)
                                    .addMarker("zone")
                                    .addMarker("air").addMarker("temp").addMarker("desired").addMarker("sp").addMarker("writable")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("\u00B0F")
                                    .setTz(tz)
                                    .build();
        CCUHsApi.getInstance().addPoint(desiredTemp);
        
        
        //Create Physical points and map
        SmartNode device = new SmartNode(nodeAddr);
        device.th1In.setPointRef(datID);
        CCUHsApi.getInstance().addPoint(device.th1In);
        device.th2In.setPointRef(eatID);
        CCUHsApi.getInstance().addPoint(device.th2In);
        device.analog1Out.setPointRef(dpID);
        CCUHsApi.getInstance().addPoint(device.analog1Out);
        device.analog2Out.setPointRef(rhID);
        CCUHsApi.getInstance().addPoint(device.analog2Out);
        device.currentTemp.setPointRef(ctID);
        CCUHsApi.getInstance().addPoint(device.currentTemp);
        Log.d("VAV", CCUHsApi.getInstance().tagsDb.getDbMap().toString());
    
        //Create write array for points, otherwise a read before write will throw exception
        setRoomTemp(0);
        setDamperPos(0);
        setReheatPos(0);
        setDesiredTemp(0);
        setDischargeTemp(0);
        setSupplyAirTemp(0);
        
    }
    
    public double getRoomTemp()
    {
        roomTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and group == \""+nodeAddr+"\"");
        return roomTemp;
    }
    public void setRoomTemp(double roomTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and current and group == \""+nodeAddr+"\"", roomTemp);
        this.roomTemp = roomTemp;
    }
    
    public double getDesiredTemp()
    {
        desiredTemp = CCUHsApi.getInstance().readDefaultVal("point and air and temp and desired and sp and group == \""+nodeAddr+"\"");
        return desiredTemp;
    }
    public void setDesiredTemp(double desiredTemp)
    {
        CCUHsApi.getInstance().writeDefaultVal("point and air and temp and desired and sp and group == \""+nodeAddr+"\"", desiredTemp);
        this.desiredTemp = desiredTemp;
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
        CCUHsApi.getInstance().writeHisValByQuery("point and air and damper and cmd and group == \""+nodeAddr+"\"", damperPos);
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
    
}
