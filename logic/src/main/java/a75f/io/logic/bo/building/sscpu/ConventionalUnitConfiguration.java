package a75f.io.logic.bo.building.sscpu;


import a75f.io.logic.bo.building.BaseProfileConfiguration;

public class ConventionalUnitConfiguration extends BaseProfileConfiguration {
    public boolean enableOccupancyControl;
    public boolean enableThermistor1;
    public boolean enableThermistor2;
    public boolean enableFanStage1;

    public double temperatureOffset;
    public boolean enableRelay1;
    public boolean enableRelay2;
    public boolean enableRelay3;
    public boolean enableRelay4;
    public boolean enableRelay5;
    public boolean enableRelay6;
    public int relay6Type;
}
