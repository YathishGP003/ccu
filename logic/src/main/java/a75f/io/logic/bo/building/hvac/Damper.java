package a75f.io.logic.bo.building.hvac;

/**
 * Created by samjithsadasivan on 6/1/18.
 */

public class Damper implements Control
{
    public int minPosition;
    public int maxPosition;
    public int currentPosition;
    public int iaqCompensatedMinPos;
    
    public int overriddenVal;
    
    public Damper() {
        minPosition = 40;
        maxPosition = 80;
        currentPosition = minPosition; // Required for accurate initial error in control loops
        iaqCompensatedMinPos = minPosition;
    }
    
    public void applyLimits() {
        currentPosition = Math.min(currentPosition, maxPosition);
        currentPosition = Math.max(currentPosition, minPosition);
    }
    
    public enum TYPE {
        MAT_RADIAL1("MAT Radial",38, 12, 80, 5, 5),
        MAT_RADIAL2("MAT Butterfly",18, 12, 60, 5, 5),
        GENERIC_0To10V("0-10V Damper", 0, 0, 0, 0, 0),
        GENERIC_2TO10V("2-10V Damper", 0, 0, 0, 0, 0),
        NOT_INSTALLED("Not Installed",0, 0, 0, 0, 0),
        GENERIC_10To0V("10-0V Damper", 0, 0, 0, 0, 0),
        GENERIC_10To2V("10-2V Damper", 0, 0, 0, 0, 0);
        String sName;
        int    mMotorRPM;
        int    mOperatingCurrent;
        int    mStallCurrent;
        int    mForwardBacklash;
        int    mReverseBacklash;
        TYPE(String str, int motorRPM, int operatingCurrent, int stallCurrent, int forwardBacklash, int reverseBacklash) {
            sName = str;
            mMotorRPM = motorRPM;
            mOperatingCurrent = operatingCurrent;
            mStallCurrent = stallCurrent;
            mForwardBacklash = forwardBacklash;
            mReverseBacklash = reverseBacklash;
            
        }
        
        public String toString() {
            return sName;
        }
        
        public int getDefaultMotorRPM() {
            return mMotorRPM;
        }
        
        public int getDefaultOperatingCurrent() {
            return mOperatingCurrent;
        }
        
        public int getDefaultStallCurrent() {
            return mStallCurrent;
        }
        
        public int getDefaultForwardBacklash() {
            return mForwardBacklash;
        }
        
        public int getDefaultReverseBacklash() {
            return mReverseBacklash;
        }
    };
    static enum SHAPE { ROUND, SQUARE };
    
    public static class Parameters {
        int    mDamperType       = -1;
        String mName             = "";
        int    mMotorRPM         = 0;
        int    mOperatingCurrent = 0;
        int    mStallCurrent     = 0;
        int    mForwardBacklash  = 0;
        int    mReverseBacklash  = 0;
        
        public Parameters(int nDamperType, String sName, int nMotorRPM, int nOperatingCurrent, int nStallCurrent, int nForwardBacklash, int nReverseBacklash) {
            mDamperType = nDamperType;
            mName = sName;
            mMotorRPM = nMotorRPM;
            mOperatingCurrent = nOperatingCurrent;
            mStallCurrent = nStallCurrent;
            mForwardBacklash = nForwardBacklash;
            mReverseBacklash = nReverseBacklash;
        }
        
        public String toString() {
            return mName;
        }
        
    };
    
    public void applyOverride(int val) {
        overriddenVal = currentPosition;
        currentPosition = val;
    }
    
    public void releaseOverride() {
        currentPosition = overriddenVal;
        overriddenVal = 0;
    }
    
    public boolean isOverrideActive() {
        return overriddenVal != 0;
    }
    
}
