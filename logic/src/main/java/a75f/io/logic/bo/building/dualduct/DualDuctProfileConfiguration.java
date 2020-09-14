package a75f.io.logic.bo.building.dualduct;

import a75f.io.logic.bo.building.BaseProfileConfiguration;

public class DualDuctProfileConfiguration extends BaseProfileConfiguration {

    private double temperatureOffset;
    
    private int analogOut1Config;
    private int analogOut2Config;
    private int thermistor1Config;
    private int thermistor2Config;
    
    private boolean enableOccupancyControl;
    private boolean enableCO2Control;
    private boolean enableIAQControl;
    
    private int minDamperCooling;
    private int maxDamperCooling;
    private int minDamperHeating;
    private int maxDamperHeating;
    
    private int analogOut1AtMinDamperHeating;
    private int analogOut1AtMaxDamperHeating;
    private int analogOut1AtMinDamperCooling;
    private int analogOut1AtMaxDamperCooling;
    private int analogOut2AtMinDamperHeating;
    private int analogOut2AtMaxDamperHeating;
    private int analogOut2AtMinDamperCooling;
    private int analogOut2AtMaxDamperCooling;
    
    private int zoneCO2Threshold;
    private int zoneCO2Target;
    private int zoneVOCThreshold;
    private int zoneVOCTarget;
}

