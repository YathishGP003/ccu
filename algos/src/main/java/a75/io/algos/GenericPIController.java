package a75.io.algos;

import android.util.Log;

/**
 * Created by samjithsadasivan on 5/1/18.
 */

public class GenericPIController
{
    
    private double proportionalGain;
    private double integralGain;
    
    
    public double proportionalError;
    public double integralError;
    
    protected double maxAllowedError;
    
    protected double error;
    private double limitedError;
    
    private double cumulativeError;
    private int integralMaxTimeout;
    
    protected double controlVariable;
    
    public void updateControlVariable(double setPoint, double controlPoint) {
        error = setPoint - controlPoint;
        
        applyErrorLimits();
        calculateProportionalError();
        calculateIntegralError();
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
        
        cumulativeError = integralError + limitedError/integralMaxTimeout;
        
        //apply integral limits
        double integralLimit = maxAllowedError * integralGain;
        double negativeIntLimit = -1 * integralLimit;
       
        integralError = cumulativeError > integralLimit ? integralLimit : cumulativeError;
        
        if (integralError < negativeIntLimit) {
            integralError = negativeIntLimit;
        }
        
    }
    
    public void calculateControlVariable() {
        controlVariable = proportionalError + integralError;
        
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
    
    public double getControlVariable() {
        Log.d("VAV","PE: "+proportionalError+", IE : "+integralError+", CV: "+controlVariable);
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
}
