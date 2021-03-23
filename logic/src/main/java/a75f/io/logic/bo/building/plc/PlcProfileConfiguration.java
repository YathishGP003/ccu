package a75f.io.logic.bo.building.plc;

import a75f.io.logic.bo.building.BaseProfileConfiguration;

/**
 * Created by samjithsadasivan on 2/25/19.
 */

public class PlcProfileConfiguration extends BaseProfileConfiguration
{
    public int analog1InputSensor;
    public int th1InputSensor;
    public double pidTargetValue;
    public double pidProportionalRange;
    public double analog1AtMinOutput;
    public double analog1AtMaxOutput;
    
    public boolean useAnalogIn2ForSetpoint;
    public int analog2InputSensor;
    public double setpointSensorOffset;
    
    public boolean expectZeroErrorAtMidpoint;
    public int nativeSensorInput;
    
    public boolean relay1ConfigEnabled;
    public double relay1OnThresholdVal;
    public double relay1OffThresholdVal;
    
    public boolean relay2ConfigEnabled;
    public double relay2OnThresholdVal;
    public double relay2OffThresholdVal;
    
    public boolean controlLoopInversion;
}
