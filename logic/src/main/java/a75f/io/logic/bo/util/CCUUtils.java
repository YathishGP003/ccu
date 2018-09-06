package a75f.io.logic.bo.util;

import java.text.DecimalFormat;

/**
 * Created by Yinten on 10/11/2017.
 */

public class CCUUtils
{
    public static double roundTo2Decimal(double number) {
        DecimalFormat df = new DecimalFormat("#.#");
        return Double.parseDouble(df.format(number));
    }
}
