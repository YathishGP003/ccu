package a75f.io.logic.bo.building.system.dab;

import a75f.io.logic.bo.building.BaseProfileConfiguration;

/**
 * Created by samjithsadasivan on 3/13/19.
 */

public class DabProfileConfiguration extends BaseProfileConfiguration
{
    public int damperType;
    public int damperSize;
    public int damperShape;
    
    public boolean enableOccupancyControl;
    public boolean enableCO2Control;
    public boolean enableIAQControl;
    
    public int minDamperCooling;
    public int maxDamperCooling;
    public int minDamperHeating;
    public int maxDamperHeating;
    
    public double temperaturOffset;
}
