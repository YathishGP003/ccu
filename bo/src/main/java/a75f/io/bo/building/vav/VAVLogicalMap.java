package a75f.io.bo.building.vav;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75.io.algos.TrimResponseRequest;
import a75f.io.bo.building.hvac.VavUnit;

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
    //ControlLoop         damperLoop;
    CO2Loop             co2Loop;
    GenericPIController valveController;// Use GenericPI as we need unmodulated op.
    
    public TrimResponseRequest satResetRequest;
    public TrimResponseRequest co2ResetRequest;
    public TrimResponseRequest spResetRequest;
    public TrimResponseRequest hwstResetRequest;
    
    public VAVLogicalMap() {
        vavUnit = new VavUnit();
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
