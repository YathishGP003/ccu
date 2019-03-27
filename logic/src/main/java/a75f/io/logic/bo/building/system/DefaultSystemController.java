package a75f.io.logic.bo.building.system;

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
}
