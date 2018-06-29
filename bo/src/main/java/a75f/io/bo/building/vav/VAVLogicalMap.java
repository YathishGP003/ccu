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
    int proportionalSpread = 5;
    double proportionalGain = 0.5;
    double integralGain = 0.5;
    
    double      roomTemp;
    double      supplyAirTemp;
    double      dischargeTemp;
    double pressure;
    
    VavUnit             vavUnit;
    ControlLoop         coolingLoop;
    ControlLoop         heatingLoop;
    ControlLoop         damperLoop;
    CO2Loop             co2Loop;
    GenericPIController valveController;// Use GenericPI as we need unmodulated op.
    
    public TrimResponseRequest satResetRequest;
    public TrimResponseRequest co2ResetRequest;
    
    public VAVLogicalMap() {
        vavUnit = new VavUnit();
        coolingLoop = new ControlLoop();
        heatingLoop = new ControlLoop();
        damperLoop = new ControlLoop();
        co2Loop = new CO2Loop();
        valveController = new GenericPIController();
        valveController.setIntegralMaxTimeout(integralMaxTimeout);
        valveController.setMaxAllowedError(proportionalSpread);
        valveController.setProportionalGain(proportionalGain);
        valveController.setIntegralGain(integralGain);
        
        satResetRequest = new TrimResponseRequest();
        co2ResetRequest = new TrimResponseRequest();
        
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
    public double getPressure()
    {
        return pressure;
    }
    public void setPressure(double pressure)
    {
        this.pressure = pressure;
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
    public ControlLoop getDamperLoop()
    {
        return damperLoop;
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
    
}
