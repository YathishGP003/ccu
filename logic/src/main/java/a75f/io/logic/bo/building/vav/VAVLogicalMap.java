package a75f.io.logic.bo.building.vav;

import java.util.HashMap;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75.io.algos.tr.TrimResponseRequest;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.ParallelFanVavUnit;
import a75f.io.logic.bo.building.hvac.SeriesFanVavUnit;
import a75f.io.logic.bo.building.hvac.VavUnit;
import a75f.io.logic.bo.haystack.Equip;
import a75f.io.logic.bo.haystack.Point;
import a75f.io.logic.bo.haystack.Tags;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.haystack.CCUHsApi;

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
    double      supplyAirTemp;
    double      dischargeTemp;
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
        //damperLoop = new ControlLoop();
        //damperLoop.setProportionalSpread(10);//Revisit
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
        createHaystackPoints();
    }
    
    public void createHaystackPoints() {
        
        //Create Logical points
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        Equip v = new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(siteDis+"-VAV-"+nodeAddr)
                          .addMarker("equip")
                          .addMarker("vav")
                          .build();
        String equipRef = CCUHsApi.getInstance().addEquip(v);
    
        Point dtPoint = new Point.Builder()
                                .setDisplayName(siteDis+"VAV-"+nodeAddr+"-DischargeAirTemp")
                                .setEquipRef(equipRef)
                                .setSiteRef(siteRef)
                                .addMarker("discharge")
                                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                .setUnit("\u00B0F")
                                .build();
        
        String dtID = CCUHsApi.getInstance().addPoint(dtPoint);
    
        Point etPoint = new Point.Builder()
                                .setDisplayName(siteDis+"VAV-"+nodeAddr+"-EnteringAirTemp")
                                .setEquipRef(equipRef)
                                .setSiteRef(siteRef)
                                .addMarker("entering")
                                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                .setUnit("\u00B0F")
                                .build();
        String etID = CCUHsApi.getInstance().addPoint(etPoint);
    
        Point damperPos = new Point.Builder()
                                .setDisplayName(siteDis+"VAV-"+nodeAddr+"-DamperPos")
                                .setEquipRef(equipRef)
                                .setSiteRef(siteRef)
                                .addMarker("air")
                                .addMarker("damper").addMarker("cmd").addMarker("writable")
                                .setUnit("\u00B0F")
                                .build();
    
        String dpID = CCUHsApi.getInstance().addPoint(damperPos);
    
        Point reheatPos = new Point.Builder()
                                  .setDisplayName(siteDis+"VAV-"+nodeAddr+"-ReheatPos")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .addMarker("reheat")
                                  .addMarker("water").addMarker("valve").addMarker("cmd").addMarker("writable")
                                  .setUnit("\u00B0F")
                                  .build();
        String rhID = CCUHsApi.getInstance().addPoint(reheatPos);
        
        //Create Physical points and map
        SmartNode device = new SmartNode(nodeAddr);
        device.analog1In.setPointRef(dtID);
        CCUHsApi.getInstance().addPoint(device.analog1In);
        device.analog2In.setPointRef(etID);
        CCUHsApi.getInstance().addPoint(device.analog2In);
        device.analog1Out.setPointRef(dpID);
        CCUHsApi.getInstance().addPoint(device.analog1Out);
        device.analog2Out.setPointRef(rhID);
        CCUHsApi.getInstance().addPoint(device.analog2Out);
        
    }
    
    public double getRoomTemp()
    {
        return roomTemp;
    }
    public void setRoomTemp(double roomTemp)
    {
        this.roomTemp = roomTemp;
    }
    public double getSupplyAirTemp()
    {
        return supplyAirTemp;
    }
    public void setSupplyAirTemp(double supplyAirTemp)
    {
        this.supplyAirTemp = supplyAirTemp;
    }
    public double getDischargeTemp()
    {
        return dischargeTemp;
    }
    public void setDischargeTemp(double dischargeTemp)
    {
        this.dischargeTemp = dischargeTemp;
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
