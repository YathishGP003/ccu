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
}
