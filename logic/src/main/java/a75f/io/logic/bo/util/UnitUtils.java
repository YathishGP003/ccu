package a75f.io.logic.bo.util;

public class UnitUtils {
    
    /**
     * Convert celsius to Fahrenheit
     * @param temperature
     * @return
     */
    public static double celsiusToFahrenheit(double temperature) {
        return CCUUtils.roundToTwoDecimal((temperature * 9/5) + 32);
    }

    public static double fahrenheitToCelsiusTwoDecimal(double temperature) {
        return CCUUtils.roundToOneDecimal((temperature - 32) * 5/9);
    }

    public static double fahrenheitToCelsius(double temperature) {
        double celsiusTemperature ;
        celsiusTemperature =((temperature - 32) * 5/9);
        double decimalValue = celsiusTemperature -(long) celsiusTemperature;

        if (decimalValue > 0.01 && decimalValue <= 0.3) {
            celsiusTemperature = (Math.round(celsiusTemperature- decimalValue));
        } else if (decimalValue > 0.3 && decimalValue <= 0.7) {
            celsiusTemperature = (Math.round(celsiusTemperature- decimalValue) + 0.5);
        } else if (decimalValue > 0.7 && decimalValue <= 0.99) {
            celsiusTemperature = (Math.round(celsiusTemperature - decimalValue) + 1.0);
        }
        return celsiusTemperature;
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
