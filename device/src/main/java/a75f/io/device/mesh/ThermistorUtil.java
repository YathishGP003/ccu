package a75f.io.device.mesh;

/**
 * Created by samjithsadasivan on 12/13/18.
 */

public class ThermistorUtil
{
    private static double[] resistance = {878900,617590,439340,316180,230060,169150,125550,94143,71172,54308,41505,32014,30458.21332,28988.52852,
            27599.64351,26286.61385,25011,23819.18292,22692.06612,21625.7403,20616.55496,19691,18790.04267,17936.19935,17126.72184,16359.03786,
            15618,14918.21847,14254.29479,13624.17847,13025.94775,12474,11926.93707,11407.33235,10913.64704,10444.43705,10000,9580.490907,
            9181.202064,8801.042381,8438.985565,8080,7748.737774,7433.095706,7132.248636,6845.419405,6569,6305.953806,6055.036411,5815.620791,
            5587.115636,5372,5162.018093,4961.499301,4769.965054,4586.963468,4423.5,4254.824168,4093.574337,3939.383288,3791.903834,3661,
            3524.905276,3394.660686,3269.983475,3150.60598,3039.3,2929.230137,2823.777441,2722.723898,2625.862866,2535.9,2128.3,1794.2,1518.3,
            1290.1,1100.2,941.79,808.96,697.22,603.97,524.93,457.33,399.63,350.59,308.44,271.92,240.34,212.85,188.95,168.13};
    private static double[] temperature = {-67,-58,-49,-40,-31,-22,-13,-4,5,14,23,32,33.8,35.6,37.4,39.2,41,42.8,44.6,46.4,48.2,50,51.8,53.6,
            55.4,57.2,59,60.8,62.6,64.4,66.2,68,69.8,71.6,73.4,75.2,77,78.8,80.6,82.4,84.2,86,87.8,89.6,91.4,93.2,95,96.8,98.6,100.4,102.2,
            104,105.8,107.6,109.4,111.2,113,114.8,116.6,118.4,120.2,122,123.8,125.6,127.4,129.2,131,132.8,134.6,136.4,138.2,140,149,158,167,
            176,185,194,203,212,221,230,239,248,257,266,275,284,293,302,311};
    
    public static double getThermisterValueToTemp(double rawValue) {
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
