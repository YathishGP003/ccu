package a75.io.algos;

/**
 * Created by samjithsadasivan on 6/4/18.
 */

/**
 * A wrapper around GenericPIController to run control loops.
 * Returns unmodulated loop output from 0-100.
 */


public class ControlLoop
{
    
    int    integralMaxTimeout = 15;
    int proportionalSpread = 3;
    double proportionalGain = 0.5;
    double integralGain = 0.5;
    
    GenericPIController piController;
    
    boolean enabled;
    
    public ControlLoop() {
        piController = new GenericPIController();
        piController.setIntegralGain(integralGain);
        piController.setProportionalGain(proportionalGain);
        piController.setMaxAllowedError(proportionalSpread);
        piController.setIntegralMaxTimeout(integralMaxTimeout);
        
    }
    
    public double getLoopOutput(double sp, double cp) {
        piController.updateControlVariable(sp, cp);
        return piController.getControlVariable() * 100 / proportionalSpread;
    }
    
    public double getLoopOutput() {
        return piController.getControlVariable() * 100 / proportionalSpread;
    }
    
    public void setEnabled() {
        enabled = true;
        piController.reset();
    }
    
    public void setDisabled() {
        enabled = false;
        piController.reset();
    }
    
    public void reset() {
        piController.reset();
    }
    
    public boolean getEnabled() {
        return enabled;
    }
    
    public void setIntegralMaxTimeout(int integralMaxTimeout)
    {
        this.integralMaxTimeout = integralMaxTimeout;
    }
    public void setProportionalSpread(int proportionalSpread)
    {
        this.proportionalSpread = proportionalSpread;
    }
    public void setProportionalGain(double proportionalGain)
    {
        this.proportionalGain = proportionalGain;
    }
    public void setIntegralGain(double integralGain)
    {
        this.integralGain = integralGain;
    }
    

}
