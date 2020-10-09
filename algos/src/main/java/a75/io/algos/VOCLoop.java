package a75.io.algos;

/**
 * Created by samjithsadasivan on 1/17/19.
 */

public class VOCLoop
{
    public void setVOCThreshold(double vocThreshold)
    {
        this.vocThreshold = vocThreshold;
    }
    public void setVOCTarget(double vocTarget)
    {
        this.vocTarget = vocTarget;
    }
    double vocThreshold = 400;
    double vocTarget = 500;
    
    public static final double PROPORTIONAL_GAIN = 1;
    public static final int PROPORTIONAL_SPREAD = 100;
    
    PControlLoop controlLoop;
    
    public VOCLoop() {
        controlLoop = new PControlLoop();
        controlLoop.setProportionalGain(PROPORTIONAL_GAIN);
        controlLoop.setMaxAllowedError(PROPORTIONAL_SPREAD);
    }
    
    public int getLoopOutput(double vocLvel) {
        if (vocLvel < vocThreshold ) {
            controlLoop.reset();
            return 0;
        } else {
            controlLoop.updateControlVariable(vocLvel, vocThreshold);
            return controlLoop.getLoopOutput();
        }
    }
    
    public int getLoopOutput() {
        return controlLoop.getLoopOutput();
    }
}
