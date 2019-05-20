package a75f.io.logic.bo.building.sshpu;

import a75f.io.logic.bo.building.BaseProfileConfiguration;

public class HeatPumpUnitConfiguration extends BaseProfileConfiguration {
    public boolean enableOccupancyControl;
    public boolean enableThermistor1;
    public boolean enableThermistor2;

    public double temperatureOffset;
    public boolean enableRelay1;
    public boolean enableRelay2;
    public boolean enableRelay3;
    public boolean enableRelay4;
    public boolean enableRelay5;
    public boolean enableRelay6;
    public int changeOverRelay6Type; //NotEnabled(0), Energize in cooling(1), Energize in heating (2)
    public int fanRelay5Type;//NotEnabled(0), Fan Stage2(1), Humidifier(2), De-humidifier(3)
}
