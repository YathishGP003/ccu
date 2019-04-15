package a75f.io.logic.bo.building.hvac;

/**
 * Created by samjithsadasivan on 6/1/18.
 */

public class Valve implements Control
{
    public int minPosition = 0;
    public int maxPosition = 100;
    public int currentPosition;
    
    public int overriddenVal;
    
    public void applyLimits() {
        currentPosition = Math.min(currentPosition, maxPosition);
        currentPosition = Math.max(currentPosition, minPosition);
    }
    
    public void applyOverride(int val) {
        overriddenVal = currentPosition;
        currentPosition = val;
    }
    
    public void releaseOverride() {
        currentPosition = overriddenVal;
        overriddenVal = 0;
    }
}
