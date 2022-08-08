package a75f.io.logic.bo.util;

import static a75f.io.logic.tuners.TunerUtil.getTuner;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.tuners.TunerConstants;

public class UnitUtils {
    
    /**
     * Convert celsius to Fahrenheit
     *
     * @param temperature
     * @return
     */
    public static double celsiusToFahrenheit(double temperature) {
        return CCUUtils.roundToTwoDecimal((temperature * 9 / 5) + 32);
    }

    public static double fahrenheitToCelsiusTwoDecimal(double temperature) {
        return CCUUtils.roundToOneDecimal((temperature - 32) * 5 / 9);
    }

    public static double fahrenheitToCelsius(double temperature) {
        double celsiusTemperature;
        celsiusTemperature = ((temperature - 32) * 5 / 9);
        double decimalValue = celsiusTemperature - (long) celsiusTemperature;

        if (decimalValue > 0.01 && decimalValue <= 0.3) {
            celsiusTemperature = (Math.round(celsiusTemperature - decimalValue));
        } else if (decimalValue > 0.3 && decimalValue <= 0.7) {
            celsiusTemperature = (Math.round(celsiusTemperature - decimalValue) + 0.5);
        } else if (decimalValue > 0.7 && decimalValue <= 0.99) {
            celsiusTemperature = (Math.round(celsiusTemperature - decimalValue) + 1.0);
        }
        return celsiusTemperature;
    }

    public static double fahrenheitToCelsiusRelative(double temperature) {
        double celsiusTemperature ;
        celsiusTemperature =(temperature/ 1.8);
        double decimalValue = celsiusTemperature -(long) celsiusTemperature;

        if (decimalValue > 0.01 && decimalValue <= 0.3) {
            celsiusTemperature = (Math.round(celsiusTemperature- decimalValue));
        } else if (decimalValue > 0.3 && decimalValue <= 0.7) {
            celsiusTemperature = (Math.round(celsiusTemperature- decimalValue) + 0.5);
        } else if (decimalValue > 0.7 && decimalValue <= 0.99) {
            celsiusTemperature = (Math.round(celsiusTemperature - decimalValue) + 1.0);
        }
        return celsiusTemperature;
    }

    public static double celsiusToFahrenheitRelativeChange(double temperature) {
        double fahrenheitTemperature ;
        fahrenheitTemperature = (temperature * 1.8);
        double decimalValue = fahrenheitTemperature - (long) fahrenheitTemperature;

        if (decimalValue > 0.01 && decimalValue <= 0.3) {
            fahrenheitTemperature = (Math.round(fahrenheitTemperature- decimalValue));
        } else if (decimalValue > 0.3 && decimalValue <= 0.7) {
            fahrenheitTemperature = (Math.round(fahrenheitTemperature- decimalValue) + 0.5);
        } else if (decimalValue > 0.7 && decimalValue <= 0.99) {
            fahrenheitTemperature = (Math.round(fahrenheitTemperature - decimalValue) + 1.0);
        }
        return fahrenheitTemperature;

    }

    public static boolean isCelsiusTunerAvailableStatus() {
        HashMap<Object, Object> useCelsius = CCUHsApi.getInstance().readEntity("displayUnit");
        if((!useCelsius.isEmpty()) && (double) getTuner(useCelsius.get("id").toString())== TunerConstants.USE_CELSIUS_FLAG_ENABLED) {
           return true;
        } else {
            return false;
        }
    }

    public static double roundToHalf(double d)
    {
        return 0.5 * Math.round(d * 2);
    }






}
