package a75.io.algos;

import android.util.Log;

/**
 * Created by samjithsadasivan on 5/1/18.
 */

public class DamperPIController extends GenericPIController
{
    int minPosition;
    int maxPosition;
    
    public DamperPIController(int minValve, int maxValve) {
        minPosition = minValve;
        maxPosition = maxValve;
    }
    
    public double getDamperPosition(double setTemp, double roomTemp) {
        Log.d("DamperPIController", "setTemp " + setTemp + " roomTemp " + roomTemp);
        updateControlVariable(setTemp, roomTemp);
        return minPosition + (maxPosition - minPosition)/2 * (1+ getControlVariable()/getMaxAllowedError());
    }
    
    public double getDamperPosition() {
        return minPosition + (maxPosition - minPosition)/2 * (1+ getControlVariable()/getMaxAllowedError());
    }
}
