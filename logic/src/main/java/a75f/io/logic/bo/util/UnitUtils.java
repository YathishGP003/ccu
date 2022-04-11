package a75f.io.logic.bo.util;

public class UnitUtils {
    
    /**
     * Convert celsius to Fahrenheit
     * @param T
     * @return
     */
    public static double celsiusToFahrenheit(double T) {
        return CCUUtils.roundToTwoDecimal((T * 9/5) + 32);
    }

    public static double fahrenheitToCelsius(double T) {
        return CCUUtils.roundToTwoDecimal((T - 32) * 5/9);
    }

}
