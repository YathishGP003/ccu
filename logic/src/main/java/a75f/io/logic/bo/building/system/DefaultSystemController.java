package a75f.io.logic.bo.building.system;

import a75f.io.api.haystack.Occupied;

import static a75f.io.logic.bo.building.system.SystemController.State.OFF;

/**
 * Created by samjithsadasivan on 3/20/19.
 */

public class DefaultSystemController extends SystemController
{
    private static DefaultSystemController instance = new DefaultSystemController();
    private DefaultSystemController() {
    }
    
    public static DefaultSystemController getInstance() {
        return instance;
    }
    
    @Override
    public int getCoolingSignal() {
        return 0;
    }
    
    @Override
    public int getHeatingSignal() {
        return 0;
    }
    
    @Override
    public double getAverageSystemHumidity() {
        return 0;
    }
    @Override
    public double getAverageSystemTemperature() {
        return 0;
    }
    
    @Override
    public SystemController.State getConditioningForecast(Occupied occupiedSchedule) {
        return OFF;
    }
}
