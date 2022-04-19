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

    public static double fahrenheitToCelsius(double temperature) {
        return CCUUtils.roundToTwoDecimal((temperature - 32) * 5/9);
    }

}
