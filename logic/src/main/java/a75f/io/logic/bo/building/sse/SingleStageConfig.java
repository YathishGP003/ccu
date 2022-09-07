package a75f.io.logic.bo.building.sse;

import a75f.io.logic.bo.building.BaseProfileConfiguration;

public class SingleStageConfig extends BaseProfileConfiguration {

    public int enableRelay1;
    public int enableRelay2;
    public boolean enableThermistor1;
    public boolean enableThermistor2;
    public boolean enableAutoAway;
    public boolean enableAutoForceOccupied;

    public double temperaturOffset;
}
