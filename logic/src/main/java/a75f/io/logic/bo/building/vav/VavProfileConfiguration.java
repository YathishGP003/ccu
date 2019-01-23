package a75f.io.logic.bo.building.vav;

import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.definitions.DamperType;
import a75f.io.logic.bo.building.definitions.ReheatType;

/**
 * Created by samjithsadasivan on 6/27/18.
 */

public class VavProfileConfiguration extends BaseProfileConfiguration
{
    
    public DamperType damperType;
    public int damperSize;
    public String damperShape;
    public ReheatType reheatType;
    
    public boolean enableOccupancyControl;
    public boolean enableCO2Control;
    public boolean enableIAQControl;
    
    public int minDamperCooling;
    public int maxDamperCooling;
    public int minDamperHeating;
    public int maxDamperHeating;
    
    public double temperaturOffset;
}
