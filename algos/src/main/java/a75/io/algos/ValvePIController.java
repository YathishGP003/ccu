package a75.io.algos;

/*
  Created by samjithsadasivan on 5/1/18.
 */

import a75f.io.logger.CcuLog;

/**
 * ValvePIController modulates the loop output (valvePosition) from minPosition to maxPosition.
 * It varies from mid point of allowed range to maxPosition when controlVariable is positive.
 * And from mid point of allowed range to minPosition when controlVariable is negative.
 */
public class ValvePIController extends GenericPIController
{
    int minPosition;
    int maxPosition;
    
    public ValvePIController(int minValve, int maxValve) {
        minPosition = minValve;
        maxPosition = maxValve;
    }
    
    public double getValveControlSignal(double setTemp, double roomTemp) {
        CcuLog.d("ValvePIController", "setTemp " + setTemp + " roomTemp " + roomTemp);
        updateControlVariable(setTemp, roomTemp);
        return minPosition + ((double) maxPosition - minPosition)/2 * (1+ getControlVariable()/getMaxAllowedError());
    }
    
    public double getValveControlSignal() {
        return minPosition + ((double) maxPosition - minPosition)/2 * (1+ getControlVariable()/getMaxAllowedError());
    }
    
}
