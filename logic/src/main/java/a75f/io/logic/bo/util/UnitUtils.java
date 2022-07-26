package a75f.io.logic.bo.util;

import static a75f.io.logic.tuners.TunerUtil.getTuner;

import android.util.Log;

import java.math.BigDecimal;
import java.text.DecimalFormat;
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

    public static double fahrenheitToCelsiusTuner(double value) {

        double conversionToCel = ((value - 32) * 0.5555555555);
        double conversionValue = (((conversionToCel) * 10) / 10);
        return Math.round((conversionValue));

    }

    public static double celsiusToFahrenheitTuner(double value) {

        double conversionToFarh = ((value) * (9 / 5) + 32);
        double conversionValue = (((conversionToFarh) * 10) / 10);
        return Math.round(conversionValue);

    }

    public static double convertingRelativeValueFtoC(double deadBandValue) {

        double relativeValue = (deadBandValue / 1.8);
        double conversionDeadBandValue = ((relativeValue) * 10) / 10;
        return Math.round(conversionDeadBandValue);
    }



    public static double convertingRelativeValueCtoF(double deadBandValue) {

        double relativeValue = (deadBandValue * 1.8);
        double conversionDeadBandValue = ((relativeValue) * 10) / 10;
        return Math.round(conversionDeadBandValue);

    }

    public static double convertingDeadBandValueFtoC(double deadBandValue) {

        double conversionDeadband = (deadBandValue / 1.8);
        double conversionDeadbandValue = (((conversionDeadband) * 10) / 10);
        BigDecimal numberBigDecimal = new BigDecimal(conversionDeadbandValue);
        return Double.parseDouble(String.valueOf(numberBigDecimal.setScale(2, BigDecimal.ROUND_UP)));
    }

    public static double convertingDeadBandValueCtoF(double deadBandValue) {
        double conversionDeadband = (deadBandValue * 1.8);
        double conversionDeadbandValue = (((conversionDeadband) * 10) / 10);
        BigDecimal numberBigDecimal = new BigDecimal(conversionDeadbandValue);
        return Double.parseDouble(String.valueOf(numberBigDecimal.setScale(2, BigDecimal.ROUND_UP)));
    }



    public static boolean doesPointNeedRelativeConversion(HashMap<Object,Object> tunerItem) {
        return   tunerItem.containsKey("deadband") || tunerItem.containsKey("spread") ||
                (tunerItem.containsKey("pspread") && !tunerItem.containsKey("chilled")) ||
                tunerItem.containsKey("leeway") || tunerItem.containsKey("setback") ||
                tunerItem.containsKey("differential") ;
    }

    public static boolean doesPointNeedRelativeDeadBandConversion(HashMap<Object,Object> tunerItem) {
        return  tunerItem.containsKey("abnormal") || tunerItem.containsKey("deadband") ||
                tunerItem.containsKey("sat") || (tunerItem.containsKey("chilled") && tunerItem.containsKey("pspread"));
    }






}
