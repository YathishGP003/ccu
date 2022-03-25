package a75f.io.renatus.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import androidx.appcompat.app.AlertDialog;

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

    public static void showRebootDialog(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                                 .setTitle("Serial Disconnected")
                                 .setMessage("No serial port connection detected for 30 minutes.Tablet will reboot " +
                                             "and try to reconnect. \n \n Press Cancel to avoid reboot.")
                                 .setPositiveButton(android.R.string.yes, (dialog1, which) -> RenatusApp.rebootTablet())
                                 .setNegativeButton(android.R.string.no, null)
                                 .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            private static final int AUTO_DISMISS_MILLIS = 15000;
            @Override
            public void onShow(final DialogInterface dialog) {
                final Button defaultButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                final CharSequence negativeButtonText = defaultButton.getText();
                new CountDownTimer(AUTO_DISMISS_MILLIS, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        defaultButton.setText(String.format(
                            Locale.getDefault(), "%s (%d)",
                            negativeButtonText,
                            TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1 //add one so it never displays zero
                        ));
                    }
                    @Override
                    public void onFinish() {
                        if (((AlertDialog) dialog).isShowing()) {
                            dialog.dismiss();
                            RenatusApp.rebootTablet();
                        }
                    }
                }.start();
            }
        });
        dialog.show();

    }

    public static String getCurrentCCUVersion(){
        String currentCCUVersion = BuildConfig.VERSION_NAME.replaceAll("[a-zA-Z]", "");
        return currentCCUVersion.replaceAll("_","");
    }
    
    public static ArrayAdapter<Double> getArrayAdapter(double start, double end, double increment, Context c) {
        ArrayList<Double> list = new ArrayList<>();
        for (double val = start;  val <= end; val += increment) {
            list.add(val);
        }
        ArrayAdapter<Double> adapter = new ArrayAdapter<>(c, R.layout.spinner_dropdown_item, list);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        return adapter;
    }
}
