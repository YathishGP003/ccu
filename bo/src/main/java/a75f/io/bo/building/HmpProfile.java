package a75f.io.bo.building;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75.io.algos.HmpPIController;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

/**
 * Created by samjithsadasivan on 5/2/18.
 */

public class HmpProfile extends ZoneProfile
{
    private double[] resistance = { 878900, 617590, 439340, 316180, 230060, 169150, 125550, 94143, 71172, 54308, 41505, 32014, 31225, 30458, 29713, 28989, 28284, 27600, 26934,
            26287, 25657, 25045, 24449, 23851, 23279, 22723, 22181, 21655, 21143, 20644, 20160, 19688, 19228, 18782, 18347, 17923, 17511, 17109, 16719, 16338, 15967, 15606,
            15255, 14912, 14578, 14253, 13936, 13628, 13327, 13034, 12748, 12469, 12198, 11933, 11675, 11423, 11178, 10938, 10705, 10477, 10255, 10038, 9827, 9621, 9419, 9223,
            9031, 8844, 8661, 8483, 8309, 8139, 7973, 7811, 7653, 7499, 7348, 7200, 7057, 6916, 6779, 6644, 6513, 6385, 6260, 6138, 6018, 5901, 5787, 5675, 5566, 5459, 5355,
            5253, 5153, 5056, 4960, 4867, 4776, 4686, 4599, 4514, 4430, 4348, 4268, 4190, 4113, 4038, 3965, 3893, 3822, 3754, 3686, 3620, 3556, 3492, 3430, 3370, 3310, 3252, 3195,
            3139, 3085, 3031, 2978, 2927, 2877, 2827, 2779, 2731, 2685, 2639, 2128, 1794, 1518, 1290, 1100, 942, 809, 697, 604, 525, 457, 400, 351, 308, 272, 240, 213, 189,168 };
   
    private double[] temperature = {-67, -58, -49, -40, -31, -22, -13, -4, 5, 14, 23, 32, 32.9, 33.8, 34.7, 35.6, 36.5, 37.4, 38.3, 39.2, 40.1, 41, 41.9, 42.8, 43.7, 44.6,
            45.5, 46.4, 47.3, 48.2, 49.1, 50, 50.9, 51.8, 52.7, 53.6, 54.5, 55.4, 56.3, 57.2, 58.1, 59, 59.9, 60.8, 61.7, 62.6, 63.5, 64.4, 65.3, 66.2, 67.1, 68, 68.9, 69.8,
            70.7, 71.6, 72.5, 73.4, 74.3, 75.2, 76.1, 77, 77.9, 78.8, 79.7, 80.6, 81.5, 82.4, 83.3, 84.2, 85.1, 86, 86.9, 87.8, 88.7, 89.6, 90.5, 91.4, 92.3, 93.2, 94.1,
            95, 95.9, 96.8, 97.7, 98.6, 99.5, 100.4, 101.3, 102.2, 103.1, 104, 104.9, 105.8, 106.7, 107.6, 108.5, 109.4, 110.3, 111.2, 112.1, 113, 113.9, 114.8, 115.7,
            116.6, 117.5, 118.4, 119.3, 120.2, 121.1, 122, 122.9, 123.8, 124.7, 125.6, 126.5, 127.4, 128.3, 129.2, 130.1, 131, 131.9, 132.8, 133.7, 134.6, 135.5, 136.4,
            137.3, 138.2, 139.1, 140, 149, 158, 167, 176, 185, 194, 203, 212, 221, 230, 239, 248, 257, 266, 275, 284, 293, 302, 311};
    
    double           hwTemperature;
    double           setTemperature = 110.0;
    HmpPIController hmpPIController ;
    
    int minValvePosition = 40; //TODO - Tuners
    int    maxValvePosition = 80;
    int    integralMaxTimeout = 15;
    int proportionalSpread = 5;
    double proportionalGain = 0.5;
    double integralGain = 0.5;
    
    public HmpProfile() {
       hmpPIController = new HmpPIController(minValvePosition,maxValvePosition);
       hmpPIController.setIntegralMaxTimeout(integralMaxTimeout);
       hmpPIController.setMaxAllowedError(proportionalSpread);
       hmpPIController.setProportionalGain(proportionalGain);
       hmpPIController.setIntegralGain(integralGain);
    }
    
    public int getMinValvePosition()
    {
        return minValvePosition;
    }
    public void setMinValvePosition(int minValvePosition)
    {
        this.minValvePosition = minValvePosition;
    }
    public int getMaxValvePosition()
    {
        return maxValvePosition;
    }
    public void setMaxValvePosition(int maxValvePosition)
    {
        this.maxValvePosition = maxValvePosition;
    }
    public int getIntegralMaxTimeout()
    {
        return integralMaxTimeout;
    }
    public void setIntegralMaxTimeout(int integralMaxTimeout)
    {
        this.integralMaxTimeout = integralMaxTimeout;
    }
    public int getProportionalSpread()
    {
        return proportionalSpread;
    }
    public void setProportionalSpread(int proportionalSpread)
    {
        this.proportionalSpread = proportionalSpread;
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
    
    public void setSetTemperature(float setTemp){
        setTemperature = setTemp;
    }
    
    public double getSetTemperature() {
        return setTemperature;
    }
    
    public double getHwTemperature() {
        return hwTemperature;
    }
    
    @Override
    public void mapRegularUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t regularUpdateMessage)
    {
        //TODO - Setpoint
        hwTemperature = (float) regularUpdateMessage.update.airflow1Temperature.get() / 10.0f;
        //airflowTemperature = getThermisterValueToTemp(regularUpdateMessage.update.externalThermistorInput1.get() * 10);
        Log.d("HMPProfile","mapRegularUpdate :airflowTemp = "+hwTemperature);
    }
    
    @JsonIgnore
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.HMP;
    }
    
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return mProfileConfiguration.get(address);
    }
    
    @JsonIgnore
    public double getUpdatedHmpValvePosition() {
        if (setTemperature == 0 || hwTemperature == 0) {
            Log.d("HMP", "Skip PI update ; setTemperature ="+setTemperature+" , airflowTemperature ="+hwTemperature);
            return 0.0;
        }
        return hmpPIController.getValveControlSignal(setTemperature, hwTemperature);
    }
    
    @JsonIgnore
    public double getHmpValvePosition() {
        if (setTemperature == 0 || hwTemperature == 0) {
            Log.d("HMP", "Skip PI update ; setTemperature ="+setTemperature+" , airflowTemperature ="+hwTemperature);
            return 0.0;
        }
        return hmpPIController.getValveControlSignal();
    }
    
    
    public double getThermisterValueToTemp(double rawValue) {
        int leftIndex = 0;
        int rightIndex = resistance.length - 1;
        if (rawValue >= resistance[leftIndex])
            return temperature[leftIndex];
        else if (rawValue <= resistance[rightIndex])
            return temperature[rightIndex];
        
        boolean found = false;
        while (!found) {
            if (rightIndex == leftIndex + 1) {
                return (temperature[leftIndex] +
                        (rawValue - resistance[leftIndex]) / (resistance[rightIndex] - resistance[leftIndex]) * (temperature[rightIndex] - temperature[leftIndex]));
            } else {
                int midIndex = (int) (leftIndex + rightIndex) / 2;
                if (rawValue == resistance[midIndex])
                    return temperature[midIndex];
                else {
                    if (rawValue > resistance[midIndex]) {
                        rightIndex = midIndex;
                    } else {
                        leftIndex = midIndex;
                    }
                }
            }
        }
        return 0;
    }
}
