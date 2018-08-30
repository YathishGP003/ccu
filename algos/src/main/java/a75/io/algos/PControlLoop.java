package a75.io.algos;

/**
 * Created by samjithsadasivan on 6/13/18.
 */

/**
 * P - Only Control Loop
 */
public class PControlLoop extends GenericPIController
{
    @Override
    public void updateControlVariable(double setPoint, double controlPoint) {
        error = controlPoint - setPoint;
        applyErrorLimits();
        calculateProportionalError();
        calculateControlVariable();
        
    }
    
    public int getLoopOutput() {
        return (int)(getControlVariable() * 100 / maxAllowedError);
    }
}
