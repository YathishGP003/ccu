package a75.io.algos;

import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 5/1/18.
 */

public class GenericPIController
{
    
    private double proportionalGain;
    private double integralGain;
    private boolean isNegativeCumulativeErrNeeded;
    
    public double proportionalError;
    public double integralError;
    
    protected double maxAllowedError;
    
    protected double error;
    protected double limitedError;
    
    private double cumulativeError;
    private int integralMaxTimeout;
    
    protected double controlVariable;

    public GenericPIController() {
        this.isNegativeCumulativeErrNeeded = true;
    }

    public void updateControlVariable(double setPoint, double controlPoint) {
        error = setPoint - controlPoint;
        applyErrorLimits();
        calculateProportionalError();
        if (isNegativeCumulativeErrNeeded)
            calculateIntegralError();
        else
            calculateIEWithoutNegativeCE();
        calculateControlVariable();
        
    }
    
    public void applyErrorLimits() {
        
        double negativeLimit = -1 * maxAllowedError;
        
        if (error > maxAllowedError) {
            limitedError = maxAllowedError;
        } else if (error < negativeLimit) {
            limitedError = negativeLimit;
        } else {
            limitedError = error;
        }
        
    }
    
    public void calculateProportionalError() {
        proportionalError = limitedError * proportionalGain;
    }
    
    public void calculateIntegralError() {
        
        integralError = limitedError * integralGain;
        cumulativeError = cumulativeError + integralError/integralMaxTimeout;

        //apply integral limits
        double integralLimit = maxAllowedError * integralGain;
        double negativeIntLimit = -1 * integralLimit;
    
        cumulativeError = Math.min(cumulativeError, integralLimit);
        cumulativeError = Math.max(cumulativeError, negativeIntLimit);
        
    }
    // Calculate the integral Error without negative Cumulative error
    public void calculateIEWithoutNegativeCE() {

    integralError = limitedError * integralGain;
        if(cumulativeError > 0)
            cumulativeError = cumulativeError + integralError/integralMaxTimeout;
        else
            cumulativeError = integralError/integralMaxTimeout;

    //apply integral limits
    double integralLimit = maxAllowedError * integralGain;
    double negativeIntLimit = -1 * integralLimit;

    cumulativeError = Math.min(cumulativeError, integralLimit);
    cumulativeError = Math.max(cumulativeError, negativeIntLimit);

}


    public void calculateControlVariable() {
        controlVariable = proportionalError + cumulativeError;
        
    }
    
    public double getProportionalGain()
    {
        return proportionalGain;
    }
    public void setProportionalGain(double proportionalGain)
    {
        this.proportionalGain = proportionalGain;
    }
    public double getIntegralGain()
    {
        return integralGain;
    }
    public void setIntegralGain(double integralGain)
    {
        this.integralGain = integralGain;
    }
    public double getMaxAllowedError()
    {
        return maxAllowedError;
    }
    public void setMaxAllowedError(double maxAllowedError)
    {
        this.maxAllowedError = maxAllowedError;
    }
    public double getError()
    {
        return error;
    }
    public void setError(double error)
    {
        this.error = error;
    }
    public int getIntegralMaxTimeout()
    {
        return integralMaxTimeout;
    }
    public void setIntegralMaxTimeout(int integralMaxTimeout)
    {
        this.integralMaxTimeout = integralMaxTimeout;
    }

    public void setNegativeCumulativeErrNeeded(boolean negativeCumulativeErrNeeded) {
        isNegativeCumulativeErrNeeded = negativeCumulativeErrNeeded;
    }

    public double getControlVariable() {
        //Log.d("VAV", "PE: " + proportionalError + ", IE : " + cumulativeError + ", CV: " + controlVariable);
        return controlVariable;
    }
    
    public void reset() {
        error = 0;
        limitedError = 0;
        proportionalError = 0;
        integralError = 0;
        cumulativeError = 0;
        controlVariable = 0;
    }
    
    public void dump() {
        CcuLog.d("CCU_ZONE","PI-LOOP error: " + error + " limitedErr: " + limitedError + " proportionalErr: " + proportionalError +
        " integralErr: " + integralError + " cumulativeErr: " + cumulativeError + " cv: " + controlVariable);
    }
    public void dumpHyperstat() {
        CcuLog.d("CCU_HSCPU",
                "PI-LOOP error: " + error + " limitedErr: " + limitedError + " proportionalErr: " + proportionalError +
                " integralErr: " + integralError + " cumulativeErr: " + cumulativeError + " cv: " + controlVariable);
    }
    @Override
    public String toString() {
        return  "PI-LOOP error: " + error + " limitedErr: " + limitedError + " proportionalErr: " + proportionalError +
                            " integralErr: " + integralError + " cumulativeErr: " + cumulativeError + " cv: " + controlVariable;
    }
}
