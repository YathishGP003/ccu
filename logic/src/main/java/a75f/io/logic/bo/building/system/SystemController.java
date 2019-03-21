package a75f.io.logic.bo.building.system;

import static a75f.io.logic.bo.building.system.SystemController.State.OFF;

/**
 * Created by samjithsadasivan on 3/20/19.
 */

public abstract class SystemController
{
    public enum State {OFF, COOLING, HEATING};
    public State systemState = OFF;
    
    public abstract int getCoolingSignal() ;
    public abstract int getHeatingSignal();
    public abstract int getSystemOccupancy();
    public abstract double getAverageSystemHumidity();
    public abstract double getAverageSystemTemperature();
}
