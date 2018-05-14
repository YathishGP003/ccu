package a75.io.algos;

import android.util.Log;

/**
 * Created by samjithsadasivan on 5/1/18.
 */

public class HmpPIController extends GenericPIController
{
    int minValvePosition;
    int maxValvePosition;
    
    public HmpPIController(int minValve, int maxValve) {
        minValvePosition = minValve;
        maxValvePosition = maxValve;
    }
    
    public double getValveControlSignal(double setTemp, double airflowTemp) {
        Log.d("HMP_PIController","setTemp "+setTemp+" airflowTemp "+airflowTemp);
        updateControlVariable(setTemp, airflowTemp);
        return minValvePosition + (maxValvePosition -minValvePosition)/2 * (1+ getControlVariable()/getMaxAllowedError());
    }
    
    public double getValveControlSignal() {
        return minValvePosition + (maxValvePosition -minValvePosition)/2 * (1+ getControlVariable()/getMaxAllowedError());
    }

}
