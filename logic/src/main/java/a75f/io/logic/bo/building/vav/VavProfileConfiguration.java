package a75f.io.logic.bo.building.vav;

import a75f.io.logic.bo.building.BaseProfileConfiguration;

/**
 * Created by samjithsadasivan on 6/27/18.
 */

public class VavProfileConfiguration extends BaseProfileConfiguration
{
    
    public int damperType;
    public int damperSize;
    public int damperShape;
    public int reheatType;
    
    public boolean enableOccupancyControl;
    public boolean enableCO2Control;
    public boolean enableIAQControl;
    
    public int minDamperCooling;
    public int maxDamperCooling;
    public int minDamperHeating;
    public int maxDamperHeating;
    
    public double temperaturOffset;
    public int nuMaxCFMCooling;
    public int  numMinCFMCooling;
    public int  numMinCFMReheating;
    public int numMaxCFMReheating;
    public boolean enableCFMControl;
    public double kFactor;
}
