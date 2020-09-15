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
    
    private int analog1OutAtMinDamperHeating;
    private int analog1OutAtMaxDamperHeating;
    private int analog1OutAtMinDamperCooling;
    private int analog1OutAtMaxDamperCooling;
    private int analog2OutAtMinDamperHeating;
    private int analog2OutAtMaxDamperHeating;
    private int analog2OutAtMinDamperCooling;
    private int analog2OutAtMaxDamperCooling;
    
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
    
    public int getAnalog1OutAtMinDamperHeating() {
        return analog1OutAtMinDamperHeating;
    }
    public void setAnalog1OutAtMinDamperHeating(int analog1OutAtMinDamperHeating) {
        this.analog1OutAtMinDamperHeating = analog1OutAtMinDamperHeating;
    }
    public int getAnalog1OutAtMaxDamperHeating() {
        return analog1OutAtMaxDamperHeating;
    }
    public void setAnalog1OutAtMaxDamperHeating(int analog1OutAtMaxDamperHeating) {
        this.analog1OutAtMaxDamperHeating = analog1OutAtMaxDamperHeating;
    }
    public int getAnalog1OutAtMinDamperCooling() {
        return analog1OutAtMinDamperCooling;
    }
    public void setAnalog1OutAtMinDamperCooling(int analog1OutAtMinDamperCooling) {
        this.analog1OutAtMinDamperCooling = analog1OutAtMinDamperCooling;
    }
    public int getAnalog1OutAtMaxDamperCooling() {
        return analog1OutAtMaxDamperCooling;
    }
    public void setAnalog1OutAtMaxDamperCooling(int analog1OutAtMaxDamperCooling) {
        this.analog1OutAtMaxDamperCooling = analog1OutAtMaxDamperCooling;
    }
    public int getAnalog2OutAtMinDamperHeating() {
        return analog2OutAtMinDamperHeating;
    }
    public void setAnalog2OutAtMinDamperHeating(int analog2OutAtMinDamperHeating) {
        this.analog2OutAtMinDamperHeating = analog2OutAtMinDamperHeating;
    }
    public int getAnalog2OutAtMaxDamperHeating() {
        return analog2OutAtMaxDamperHeating;
    }
    public void setAnalog2OutAtMaxDamperHeating(int analog2OutAtMaxDamperHeating) {
        this.analog2OutAtMaxDamperHeating = analog2OutAtMaxDamperHeating;
    }
    public int getAnalog2OutAtMinDamperCooling() {
        return analog2OutAtMinDamperCooling;
    }
    public void setAnalog2OutAtMinDamperCooling(int analog2OutAtMinDamperCooling) {
        this.analog2OutAtMinDamperCooling = analog2OutAtMinDamperCooling;
    }
    public int getAnalog2OutAtMaxDamperCooling() {
        return analog2OutAtMaxDamperCooling;
    }
    public void setAnalog2OutAtMaxDamperCooling(int analog2OutAtMaxDamperCooling) {
        this.analog2OutAtMaxDamperCooling = analog2OutAtMaxDamperCooling;
    }
}

