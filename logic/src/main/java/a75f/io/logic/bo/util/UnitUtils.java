package a75f.io.logic.bo.util;

public class UnitUtils {
    
    /**
     * Convert celsius to Fahrenheit
     * @param T
     * @return
     */
    public static double celsiusToFahrenheit(double T) {
        return CCUUtils.roundToTwoDecimal((T*9/5)+32);
    }

    public static double fahrenheitToCelsius(double T) {
        return CCUUtils.roundToTwoDecimal((T-32)*5/9);
    }
    public static double celsiusToFahrenheitUnitChange(double T){
        return CCUUtils.roundToTwoDecimal(T*1.8);
    }
    public static double fahrenheitToCelsiusUnitChange(double T) {
        return CCUUtils.roundToTwoDecimal(T/1.8);
    }
    public static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }
    public static double roundToHalf(double d)
    {
        return 0.5 * Math.round(d * 2);
    }

}
