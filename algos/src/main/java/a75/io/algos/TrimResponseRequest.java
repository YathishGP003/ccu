package a75.io.algos;

import android.util.Log;

/**
 * Created by samjithsadasivan on 6/1/18.
 */

public class TrimResponseRequest
{
    
    int importanceMultiplier;
    double requestIntervalMins = 1;
    
    public double requestHours;
    
    //This is the zone/system Request Hours divided by the zone/system run-hours (the hours in any Mode other than Unoccupied Mode)
    //since the last reset, expressed as a percentage
    public double cumulativeRequestHoursPercent;
    
    public int currentRequests;
    
    public int minuteCounter;
    
    public TrimResponseRequest(int im) {
        importanceMultiplier = im;
    }
    
    public void handleReset() {
        minuteCounter = 0;
        requestHours = 0;
        cumulativeRequestHoursPercent = 0;
    }
    
    public void handleRequestUpdate() {
    
        ++minuteCounter;
        Log.d("VAV","Request params : minuteCounter: "+minuteCounter+" currentRequests "+currentRequests);
        
        if (minuteCounter % requestIntervalMins == 0) {
            requestHours = requestHours + (currentRequests * requestIntervalMins/60);
        }
        
        if (minuteCounter % 60 == 0) {
            int hours = minuteCounter/60;
            cumulativeRequestHoursPercent = (requestHours / hours) * 100;
        }
    }
}
