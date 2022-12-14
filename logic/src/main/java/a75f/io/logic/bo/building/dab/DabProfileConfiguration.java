package a75f.io.logic.bo.building.dab;

import a75f.io.logic.bo.building.BaseProfileConfiguration;

/**
 * Created by samjithsadasivan on 3/13/19.
 */

public class DabProfileConfiguration extends BaseProfileConfiguration
{
    public int damper1Type;
    public int damper1Size;
    public int damper1Shape;
    
    public int damper2Type;
    public int damper2Size;
    public int damper2Shape;
    
    public boolean enableOccupancyControl;
    public boolean enableCO2Control;
    public boolean enableIAQControl;
    public boolean enableAutoForceOccupied;
    public boolean enableAutoAwayControl;
    
    public int minDamperCooling;
    public int maxDamperCooling;
    public int minDamperHeating;
    public int maxDamperHeating;
    
    public double temperaturOffset;

    public int minCFMForIAQ;
    public boolean enableCFMControl;
    public double kFactor;
}
