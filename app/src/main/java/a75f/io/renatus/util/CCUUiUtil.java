package a75f.io.renatus.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.preference.PreferenceManager;
import android.widget.Spinner;

import com.google.android.material.color.MaterialColors;

import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.R;

public class CCUUiUtil {


    public static int getPrimaryThemeColor(Context context){
        return MaterialColors.getColor(context, R.attr.orange_75f, Color.RED);
    }

    public static int getListSelectorBackground(Context context){
        return  context.getResources().getIdentifier("@drawable/ic_listselector", "drawable", context.getPackageName());
    }
    public static int getDayselectionBackgroud(Context context){
        return  context.getResources().getIdentifier("@drawable/bg_weekdays_selector", "drawable", context.getPackageName());
    }
    public static int getDrawableResouce(Context context, String name){
        return  context.getResources().getIdentifier("@drawable/"+name, "drawable", context.getPackageName());
    }
    public static void setThemeDetails(Activity activity){
        if(BuildConfig.BUILD_TYPE.equals("daikin_prod")||CCUUiUtil.isDaikinThemeEnabled(activity)){
            activity.setTheme(R.style.RenatusAppDaikinTheme);
        }
    }
    public static String getColorCode(Context context) {
        StringBuffer colorCode = new StringBuffer("#");
        colorCode.append(Integer.toHexString(getPrimaryThemeColor(context) & 0x00ffffff));
        return colorCode.toString();
    }

    public static boolean isDaikinThemeEnabled(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.prefs_theme_key), false);
    }

    public static void triggerRestart(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    public static void setSpinnerDropDownColor(Spinner spinnerView,Context context){
        spinnerView.getBackground().setColorFilter(CCUUiUtil.getPrimaryThemeColor(context), PorterDuff.Mode.SRC_ATOP);
    }
}
