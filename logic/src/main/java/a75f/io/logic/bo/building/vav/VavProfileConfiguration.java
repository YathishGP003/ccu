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
    public static int nuMaxCFMCooling;
    public static int  numMinCFMCooling;
    public static int  numMinCFMReheating;
    public static int numMaxCFMReheating;
    public static boolean enableCFMControl;
    public static double kFactor;
}
