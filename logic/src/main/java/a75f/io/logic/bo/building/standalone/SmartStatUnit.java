package a75f.io.logic.bo.building.standalone;

import a75f.io.logic.bo.building.definitions.StandaloneFanSpeed;
import a75f.io.logic.bo.building.definitions.StandaloneOperationalMode;

public class SmartStatUnit implements Control {
    StandaloneFanSpeed fanOperationalMode;
    StandaloneOperationalMode saOperationalMode;
    public int overriddenVal;
    public int currentVal;
    public SmartStatUnit(){
        fanOperationalMode = StandaloneFanSpeed.AUTO;
        saOperationalMode = StandaloneOperationalMode.AUTO;
    }
    public int getFanOperationalMode() {
        return fanOperationalMode.ordinal();
    }

    public void setFanOperationalMode(int fanSpeed){
        fanOperationalMode = StandaloneFanSpeed.values()[fanSpeed];
    }

    public int getOperationalMode() {
        return saOperationalMode.ordinal();
    }

    public void setOperationalMode(int opMode){
        saOperationalMode = StandaloneOperationalMode.values()[opMode];
    }



    public void applyOverride(int val) {
        overriddenVal = currentVal;
        currentVal = val;
    }

    public void releaseOverride() {
        currentVal = overriddenVal;
        overriddenVal = 0;
    }

    public boolean isOverrideActive() {
        return overriddenVal != 0;
    }
}
