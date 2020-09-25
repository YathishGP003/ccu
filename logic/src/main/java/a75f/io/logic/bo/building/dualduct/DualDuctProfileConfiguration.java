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
    
    private int minHeatingDamperPos;
    private int maxHeatingDamperPos;
    private int minCoolingDamperPos;
    private int maxCoolingDamperPos;
    
    private double analog1OutAtMinDamperHeating;
    private double analog1OutAtMaxDamperHeating;
    private double analog1OutAtMinDamperCooling;
    private double analog1OutAtMaxDamperCooling;
    private double analog2OutAtMinDamperHeating;
    private double analog2OutAtMaxDamperHeating;
    private double analog2OutAtMinDamperCooling;
    private double analog2OutAtMaxDamperCooling;
    
    public double getTemperatureOffset() {
        return temperatureOffset;
    }
    public void setTemperatureOffset(double temperatureOffset) {
        this.temperatureOffset = temperatureOffset;
    }
    public int getAnalogOut1Config() {
        return analogOut1Config;
    }
    public void setAnalogOut1Config(int analogOut1Config) {
        this.analogOut1Config = analogOut1Config;
    }
    public int getAnalogOut2Config() {
        return analogOut2Config;
    }
    public void setAnalogOut2Config(int analogOut2Config) {
        this.analogOut2Config = analogOut2Config;
    }
    public int getThermistor1Config() {
        return thermistor1Config;
    }
    public void setThermistor1Config(int thermistor1Config) {
        this.thermistor1Config = thermistor1Config;
    }
    public int getThermistor2Config() {
        return thermistor2Config;
    }
    public void setThermistor2Config(int thermistor2Config) {
        this.thermistor2Config = thermistor2Config;
    }
    public boolean isEnableOccupancyControl() {
        return enableOccupancyControl;
    }
    public void setEnableOccupancyControl(boolean enableOccupancyControl) {
        this.enableOccupancyControl = enableOccupancyControl;
    }
    public boolean isEnableCO2Control() {
        return enableCO2Control;
    }
    public void setEnableCO2Control(boolean enableCO2Control) {
        this.enableCO2Control = enableCO2Control;
    }
    public boolean isEnableIAQControl() {
        return enableIAQControl;
    }
    public void setEnableIAQControl(boolean enableIAQControl) {
        this.enableIAQControl = enableIAQControl;
    }
    public int getMinHeatingDamperPos() {
        return minHeatingDamperPos;
    }
    public void setMinHeatingDamperPos(int minHeatingDamperPos) {
        this.minHeatingDamperPos = minHeatingDamperPos;
    }
    public int getMaxHeatingDamperPos() {
        return maxHeatingDamperPos;
    }
    public void setMaxHeatingDamperPos(int maxHeatingDamperPos) {
        this.maxHeatingDamperPos = maxHeatingDamperPos;
    }
    public int getMinCoolingDamperPos() {
        return minCoolingDamperPos;
    }
    public void setMinCoolingDamperPos(int minCoolingDamperPos) {
        this.minCoolingDamperPos = minCoolingDamperPos;
    }
    public int getMaxCoolingDamperPos() {
        return maxCoolingDamperPos;
    }
    public void setMaxCoolingDamperPos(int maxCoolingDamperPos) {
        this.maxCoolingDamperPos = maxCoolingDamperPos;
    }
    
    public double getAnalog1OutAtMinDamperHeating() {
        return analog1OutAtMinDamperHeating;
    }
    public void setAnalog1OutAtMinDamperHeating(double analog1OutAtMinDamperHeating) {
        this.analog1OutAtMinDamperHeating = analog1OutAtMinDamperHeating;
    }
    public double getAnalog1OutAtMaxDamperHeating() {
        return analog1OutAtMaxDamperHeating;
    }
    public void setAnalog1OutAtMaxDamperHeating(double analog1OutAtMaxDamperHeating) {
        this.analog1OutAtMaxDamperHeating = analog1OutAtMaxDamperHeating;
    }
    public double getAnalog1OutAtMinDamperCooling() {
        return analog1OutAtMinDamperCooling;
    }
    public void setAnalog1OutAtMinDamperCooling(double analog1OutAtMinDamperCooling) {
        this.analog1OutAtMinDamperCooling = analog1OutAtMinDamperCooling;
    }
    public double getAnalog1OutAtMaxDamperCooling() {
        return analog1OutAtMaxDamperCooling;
    }
    public void setAnalog1OutAtMaxDamperCooling(double analog1OutAtMaxDamperCooling) {
        this.analog1OutAtMaxDamperCooling = analog1OutAtMaxDamperCooling;
    }
    public double getAnalog2OutAtMinDamperHeating() {
        return analog2OutAtMinDamperHeating;
    }
    public void setAnalog2OutAtMinDamperHeating(double analog2OutAtMinDamperHeating) {
        this.analog2OutAtMinDamperHeating = analog2OutAtMinDamperHeating;
    }
    public double getAnalog2OutAtMaxDamperHeating() {
        return analog2OutAtMaxDamperHeating;
    }
    public void setAnalog2OutAtMaxDamperHeating(double analog2OutAtMaxDamperHeating) {
        this.analog2OutAtMaxDamperHeating = analog2OutAtMaxDamperHeating;
    }
    public double getAnalog2OutAtMinDamperCooling() {
        return analog2OutAtMinDamperCooling;
    }
    public void setAnalog2OutAtMinDamperCooling(double analog2OutAtMinDamperCooling) {
        this.analog2OutAtMinDamperCooling = analog2OutAtMinDamperCooling;
    }
    public double getAnalog2OutAtMaxDamperCooling() {
        return analog2OutAtMaxDamperCooling;
    }
    public void setAnalog2OutAtMaxDamperCooling(double analog2OutAtMaxDamperCooling) {
        this.analog2OutAtMaxDamperCooling = analog2OutAtMaxDamperCooling;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        
        DualDuctProfileConfiguration config = (DualDuctProfileConfiguration)obj;
        
        if (temperatureOffset != config.temperatureOffset ||
            analogOut1Config != config.analogOut1Config ||
            analogOut2Config != config.analogOut2Config ||
            thermistor1Config != config.thermistor1Config||
            thermistor2Config != config.thermistor2Config ||
            enableOccupancyControl != config.enableOccupancyControl ||
            enableCO2Control != config.enableCO2Control ||
            enableIAQControl != config.enableIAQControl ||
            minHeatingDamperPos != config.minHeatingDamperPos ||
            maxHeatingDamperPos != config.maxHeatingDamperPos ||
            minCoolingDamperPos != config.minCoolingDamperPos ||
            maxCoolingDamperPos != config.maxCoolingDamperPos ||
            analog1OutAtMinDamperHeating != config.analog1OutAtMinDamperHeating ||
            analog1OutAtMaxDamperHeating != config.analog1OutAtMaxDamperHeating ||
            analog1OutAtMinDamperCooling != config.analog1OutAtMinDamperCooling ||
            analog1OutAtMaxDamperCooling != config.analog1OutAtMaxDamperCooling ||
            analog2OutAtMinDamperHeating != config.analog2OutAtMinDamperHeating ||
            analog2OutAtMaxDamperHeating != config.analog2OutAtMaxDamperHeating ||
            analog2OutAtMinDamperCooling != config.analog2OutAtMinDamperCooling ||
            analog2OutAtMaxDamperCooling != config.analog2OutAtMaxDamperCooling) {
            return false;
        }
        
        return true;
    }
}

