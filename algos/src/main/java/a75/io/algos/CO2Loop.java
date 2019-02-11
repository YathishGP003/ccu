package a75.io.algos;

/**
 * Created by samjithsadasivan on 6/14/18.
 */

/**
 * CO2 Loop is implemented as a P-Only control loop that gives unmodulated output from 0-100 %
 * based on co2 values from 800-1000 PPM (Hard coding these values as this is
 * the ASHRAE standard across different types of VAV terminals)
 */
public class CO2Loop
{
    public void setCo2Threshold(double co2Threshold)
    {
        this.co2Threshold = co2Threshold;
    }
    public void setCo2Target(double co2Target)
    {
        this.co2Target = co2Target;
    }
    double co2Threshold = 800;
    double co2Target = 1000;
    
    public static final double PROPORTIONAL_GAIN = 1;
    public static final int PROPORTIONAL_SPREAD = 200;
    
    PControlLoop controlLoop;
    
    public CO2Loop() {
        controlLoop = new PControlLoop();
        controlLoop.setProportionalGain(PROPORTIONAL_GAIN);
        controlLoop.setMaxAllowedError(PROPORTIONAL_SPREAD);
    }
    
    public int getLoopOutput(double co2Lvel) {
        if (co2Lvel < co2Threshold ) {
            controlLoop.reset();
            return 0;
        } else {
            controlLoop.updateControlVariable(co2Target, co2Lvel);
            return controlLoop.getLoopOutput();
        }
    }
    
    public int getLoopOutput() {
        return controlLoop.getLoopOutput();
    }
}
