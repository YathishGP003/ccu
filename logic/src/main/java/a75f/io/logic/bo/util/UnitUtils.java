package a75f.io.logic.bo.util;

import static a75f.io.logic.tuners.TunerUtil.getTuner;

import android.util.Log;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
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

        double conversionToFarh = (value * 9 / 5) + 32;
        double conversionValue = (((conversionToFarh) * 10) / 10);
        return Math.round(conversionValue);

    }

    public static String StatusCelsiusVal(String temp, int modeType) {
        boolean tempContainsF = false;
        CcuLog.i(L.TAG_DESIRED_TEMP_MODE,"temp "+temp+ "modeType > "+modeType);

        // Below method checks whether String "temp" cpntains "F" or not because based
        // on this character below code converts temperature from fahrenheit to celsius.
        tempContainsF = temp.contains("F");

        if(!tempContainsF){
            return temp;
        }

        String s = "";
        ArrayList<Double> myDoubles = new ArrayList<Double>();
        Matcher matcher = Pattern.compile("[-+]?\\d*\\.?\\d+([eE][-+]?\\d+)?").matcher(temp);

        Pattern p = Pattern.compile("[a-zA-Z]+");
        Matcher m1 = p.matcher(temp);
        while (m1.find()) {
            s = s + m1.group() + " ";
        }

        while (matcher.find()) {
            double element = Double.parseDouble(matcher.group());
            myDoubles.add(Math.abs(element));
        }
        final String f = s.substring(0, s.lastIndexOf("F")) + " ";
        DecimalFormat timeFormatter = new DecimalFormat("00");
        if(modeType == TemperatureMode.DUAL.ordinal()){
            if (myDoubles.size() > 0) {
                if(myDoubles.size() > 3) {
                    return (f + (CCUUtils.roundToOneDecimal(fahrenheitToCelsius(myDoubles.get(0))))
                            + "-" + (CCUUtils.roundToOneDecimal(fahrenheitToCelsius(myDoubles.get(1))))
                            + " \u00B0C" + " at " + (myDoubles.get(2).intValue()) + ":" + timeFormatter.format(myDoubles.get(3).intValue()));
                }else {
                    // When refresh screen is called then status is fetched from haystack there we
                    // have single temperature so my doubles size will be only 3.
                    return (f + (CCUUtils.roundToOneDecimal(fahrenheitToCelsius(myDoubles.get(0))))
                            + " \u00B0C" + " at " + (myDoubles.get(1).intValue()) + ":" +
                            timeFormatter.format(myDoubles.get(2).intValue()));
                }
            }else {
                return temp;
            }
        }
        if (modeType == TemperatureMode.HEATING.ordinal()) {
            if (myDoubles.size() > 0) {
                if(myDoubles.size() > 3) {
                    return (f + (CCUUtils.roundToOneDecimal(fahrenheitToCelsius(myDoubles.get(0))))
                            + " \u00B0C" + " at " + (myDoubles.get(2).intValue()) + ":" +
                            timeFormatter.format(myDoubles.get(3).intValue()));
                }else {
                    // When refresh screen is called then status is fetched from haystack there we have single temperature so mydoubles size will be only 3.
                    return (f + (CCUUtils.roundToOneDecimal(fahrenheitToCelsius(myDoubles.get(0))))
                            + " \u00B0C" + " at " + (myDoubles.get(1).intValue()) + ":" +
                            timeFormatter.format(myDoubles.get(2).intValue()));
                }
            }else {
                return temp;
            }
        }else if (modeType == TemperatureMode.COOLING.ordinal()) {
            Log.i("fatal","mydoubles "+f+",,,  "+myDoubles);
            if (myDoubles.size() > 0) {
                if (myDoubles.size() > 3) {
                    Log.i("fatal", "return  " + (f + (CCUUtils.roundToOneDecimal(
                            fahrenheitToCelsius(myDoubles.get(1)))) + " \u00B0C" + " at " +
                            (myDoubles.get(2).intValue()) + ":" + timeFormatter.format(myDoubles.get(3).intValue())));
                    return (f + (CCUUtils.roundToOneDecimal(fahrenheitToCelsius(myDoubles.get(1))))
                            + " \u00B0C" + " at " + (myDoubles.get(2).intValue()) + ":" + timeFormatter.format
                            (myDoubles.get(3).intValue()));
                }else {
                    return (f + (CCUUtils.roundToOneDecimal(fahrenheitToCelsius(myDoubles.get(0))))
                            + " \u00B0C" + " at " + (myDoubles.get(1).intValue()) + ":" + myDoubles.get(2).intValue());
                }
            }else {
                return temp;
            }
        }else {
            return temp;
        }

    }

    public static double convertingRelativeValueFtoC(double deadBandValue) {

        double relativeValue = (deadBandValue / 1.8);
        double conversionDeadBandValue = ((relativeValue) * 10) / 10;
        BigDecimal numberBigDecimal = new BigDecimal(conversionDeadBandValue);
        return Double.parseDouble(String.valueOf(numberBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP)));
    }



    public static double convertingRelativeValueCtoF(double deadBandValue) {

        double relativeValue = (deadBandValue * 1.8);
        double conversionDeadBandValue = ((relativeValue) * 10) / 10;
        BigDecimal numberBigDecimal = new BigDecimal(conversionDeadBandValue);
        return Double.parseDouble(String.valueOf(numberBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP)));

    }

    public static double convertingDeadBandValueFtoC(double deadBandValue) {

        double conversionDeadband = (deadBandValue / 1.8);
        double conversionDeadbandValue = (((conversionDeadband) * 10) / 10);
        BigDecimal numberBigDecimal = new BigDecimal(conversionDeadbandValue);
        return Double.parseDouble(String.valueOf(numberBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP)));
    }

    public static double convertingDeadBandValueCtoF(double deadBandValue) {
        double conversionDeadband = (deadBandValue * 1.8);
        double conversionDeadbandValue = (((conversionDeadband) * 10) / 10);
        BigDecimal numberBigDecimal = new BigDecimal(conversionDeadbandValue);
        return Double.parseDouble(String.valueOf(numberBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP)));
    }



    public static boolean doesPointNeedRelativeConversion(HashMap<Object,Object> tunerItem) {
             return   tunerItem.containsKey("spread") || tunerItem.containsKey("abnormal") ||
                (tunerItem.containsKey("chilled") || tunerItem.containsKey("pspread")) ||
                tunerItem.containsKey("leeway") || tunerItem.containsKey("setback") ||
                     tunerItem.containsKey("differential")|| tunerItem.containsKey("sat")
                     || (tunerItem.containsKey("reheat") && tunerItem.containsKey("offset"))
                     || (tunerItem.containsKey("aux") && tunerItem.containsKey("heating") && tunerItem.containsKey("stage1"))
                     || (tunerItem.containsKey("aux") && tunerItem.containsKey("heating") && tunerItem.containsKey("stage2"));

    }

    public static boolean doesPointNeedRelativeDeadBandConversion(HashMap<Object,Object> tunerItem) {
        return   tunerItem.containsKey("deadband");
    }
}
