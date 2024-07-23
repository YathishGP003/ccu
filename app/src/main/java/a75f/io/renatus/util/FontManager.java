package a75f.io.renatus.util;

import android.content.Context;
import android.graphics.Typeface;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class FontManager {

    public static final String ROOT = "fonts/",
            FONTAWESOME = ROOT + "fontawesome.ttf";

    public static Typeface getTypeface(Context context, String font) {
        return Typeface.createFromAsset(context.getAssets(), font);
    }

    public static String getColoredSpanned(String text, String color) {
        String input = "<font color=" + color + ">" + text + "</font>";
        CcuLog.i(L.TAG_CCU_UI,"ColorFromHex String:"+text+" Color:"+color);
        return input;
    }


}
