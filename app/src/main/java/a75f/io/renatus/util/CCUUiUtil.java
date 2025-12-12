package a75f.io.renatus.util;

import static a75f.io.alerts.AlertsConstantsKt.DEVICE_RESTART;
import static a75f.io.alerts.model.AlertCauses.CCU_RESTART;
import static a75f.io.renatus.UtilityApplication.context;

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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter;

public class CCUUiUtil {

    static final String AIROVERSE_PROD = "airoverse_prod";
    static final String CARRIER_PROD = "carrier_prod";
    static final String DAIKIN_PROD = "daikin_prod";
    public static int getPrimaryThemeColor(Context context){
        return MaterialColors.getColor(context, R.attr.orange_75f, Color.RED);
    }

    public static int getListSelectorBackground(Context context){
        return  context.getResources().getIdentifier("@drawable/ic_listselector", "drawable", context.getPackageName());
    }
    public static int getDaySelectionBackground(Context context){
        return  context.getResources().getIdentifier("@drawable/bg_weekdays_selector", "drawable", context.getPackageName());
    }

    public static void setThemeDetails(Activity activity){
        if(CCUUiUtil.isDaikinEnvironment(activity)){
            activity.setTheme(R.style.RenatusAppDaikinTheme);
        } else if (CCUUiUtil.isCarrierThemeEnabled(activity)) {
            activity.setTheme(R.style.RenatusAppCarrierTheme);
        } else if (CCUUiUtil.isAiroverseThemeEnabled(activity)) {
            activity.setTheme(R.style.RenatusAppAiroverseTheme);
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
        AlertManager.getInstance().clearAlertsWhenAppClose();
        AlertManager.getInstance().getRepo().setRestartAppToTrue();
        UpdateAppRestartCause(CCU_RESTART);
        Domain.diagEquip.getAppRestart().writeHisVal(1.0);
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
                                 .setPositiveButton(android.R.string.yes, (dialog1, which) -> {
                                     UpdateAppRestartCause(DEVICE_RESTART);
                                           RenatusApp.rebootTablet();
                                 })
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

    public static void showMstpDisabledDialog(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Bacnet MSTP was Disabled")
                .setMessage("Bacnet MSTP was disabled since adapter disconnected.\n" +
                        "It may be enabled again from USB Manager")
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.show();

    }

    public static String getCurrentCCUVersion(){
        String currentCCUVersion = BuildConfig.VERSION_NAME.replaceAll("[a-zA-Z]", "");
        return currentCCUVersion.replaceAll("_","");
    }

    public static String getCurrentCCUBundleName(){
        return CCUHsApi.getInstance().readDefaultStrVal("domainName == \"" + DomainName.bundleVersion + "\"");
    }

    public static ArrayAdapter<Double> getArrayAdapter(double start, double end, double increment, Context c) {
        ArrayList<Double> list = new ArrayList<>();
        for (double val = start;  val <= end; val += increment) {
            list.add(val);
        }
        ArrayAdapter<Double> adapter = getAdapterValue(c,list);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        return adapter;
    }

    public static boolean isDaikinEnvironment(Context context){
        return BuildConfig.BUILD_TYPE.equals(context.getString(R.string.Daikin_Environment))||CCUUiUtil.isDaikinThemeEnabled(context);
    }

    public static boolean isInvalidName(String enteredName){
        return enteredName.contains(".") || enteredName.contains("\\")
               || enteredName.contains("&") || enteredName.contains("#");
    }

    public static boolean isValidOrgName(String orgName){
        Pattern specialCharacters = Pattern.compile("[^a-z0-9_ -]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = specialCharacters.matcher(orgName);
        if(orgName.startsWith("_") || orgName.startsWith("-") || orgName.startsWith(" ") )
            return false;
        return !matcher.find();
    }

    public static boolean isValidIPAddress(String ip) {
        String zeroTo255 = "(\\d{1,2}|(0|1)\\" + "d{2}|2[0-4]\\d|25[0-5])";
        String regex = zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255;
        Pattern p = Pattern.compile(regex);
        if (ip == null) {
            return false;
        }
        //pattern class contains matcher() method to find matching between given IP address and regular expression.
        Matcher m = p.matcher(ip);
        // Return if the IP address matched the ReGex
        return m.matches();
    }

    public static boolean isValidNumber(int val, int min, int max, int multiple) {
        return (val >= min && val <= max && val % multiple == 0);
    }

    public static boolean isAlphaNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isLetterOrDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isCCUNeedsToBeUpdated(String currentAppVersionWithPatch, String recommendedVersionOfCCUWithPatch) {
        if (recommendedVersionOfCCUWithPatch == null) {
            CcuLog.w(L.TAG_CCU, "Recommended version is null, returning false (no update)");
            return false;
        }

        String[] currentVersionComponents = currentAppVersionWithPatch.split("\\.");
        String[] recommendedVersionComponents = recommendedVersionOfCCUWithPatch.split("\\.");
        int minLength = Math.min(currentVersionComponents.length, recommendedVersionComponents.length);
        for (int i = 0; i < minLength; i++) {
            int currentVersion = Integer.parseInt(currentVersionComponents[i]);
            int recommendedVersion = Integer.parseInt(recommendedVersionComponents[i]);
            if (recommendedVersion > currentVersion) {
                return true;
            } else if (currentVersion > recommendedVersion) {
                return false;
            }
        }
        return false;
    }
    public static boolean isCurrentVersionHigherOrEqualToRequired(
            String currentAppVersionWithPatch, String requiredVersionOfCCUWithPatch) {
        if(requiredVersionOfCCUWithPatch == null){
            return true;
        }
        String[] currentVersionComponents = currentAppVersionWithPatch.split("\\.");
        String[] requiredVersionVersionComponents = requiredVersionOfCCUWithPatch.split("\\.");
        int minLength = Math.min(currentVersionComponents.length, requiredVersionVersionComponents.length);
        for (int i = 0; i < minLength; i++) {
            int currentVersion = Integer.parseInt(currentVersionComponents[i]);
            int requiredVersionVersion = Integer.parseInt(requiredVersionVersionComponents[i]);
            if ((currentVersion > requiredVersionVersion) || currentAppVersionWithPatch.equals(requiredVersionOfCCUWithPatch)) {
                return true;
            } else if (currentVersion < requiredVersionVersion) {
                return false;
            }
        }
        return false;
    }
    public static boolean isCarrierThemeEnabled(Context context) {
        return BuildConfig.BUILD_TYPE.equals(context.getString(R.string.Carrier_Environment)) || PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.prefs_carrier_theme_key), false);
    }

    public static boolean isAiroverseThemeEnabled(Context context) {
        return BuildConfig.BUILD_TYPE.equals(context.getString(R.string.Airoverse_Environment)) || PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.prefs_airoverse_theme_key), false);
    }

    public static boolean isValidMacAddress(String ip) {
        //String regex = "^(?:[0-9A-Fa-f]{2}[:-]){5}(?:[0-9A-Fa-f]{2})$";
        String regex = "^[a-fA-F0-9]{2}(.[a-fA-F0-9]{2}){5}$";
        Pattern p = Pattern.compile(regex);
        if (ip == null) {
            return false;
        }
        //pattern class contains matcher() method to find matching between given Mac address and regular expression.
        Matcher m = p.matcher(ip);
        // Return if the mac address matched the ReGex
        return m.matches();
    }
    private static CustomSpinnerDropDownAdapter getAdapterValue(Context context,ArrayList values) {
        return new CustomSpinnerDropDownAdapter(context, R.layout.spinner_dropdown_item, values);
    }
    public static int getPrimaryColor() {
        int highlightColor;
        if(BuildConfig.BUILD_TYPE.equals(AIROVERSE_PROD)){
            highlightColor = ContextCompat.getColor(context, R.color.airoverse_primary);
        } else if(BuildConfig.BUILD_TYPE.equals(CARRIER_PROD)){
            highlightColor = ContextCompat.getColor(context, R.color.carrier_75f);
        } else if(BuildConfig.BUILD_TYPE.equals(DAIKIN_PROD)){
            highlightColor = ContextCompat.getColor(context, R.color.daikin_75f);
        } else {
            highlightColor = ContextCompat.getColor(context, R.color.renatus_75f_primary);
        }
        return highlightColor;
    }
    public static int getGreyColor() {
        return ContextCompat.getColor(context, R.color.tuner_group);
    }

    public static boolean isDomainEquip(String val, String filter) {
        if (filter.equals("node")) {
            return CCUHsApi.getInstance().readEntity("equip and group == \"" + val + "\"").containsKey("domainName");
        } else {
            return CCUHsApi.getInstance().readMapById(val).containsKey("domainName");
        }
    }

    public static Double readPriorityValByEquipRef(String domainName, String equipRef) {
        return CCUHsApi.getInstance().readPointPriorityValByQuery("point and domainName == \"" + domainName + "\" " +
                " and equipRef == \"" + equipRef + "\"");
    }

    public static Double readPriorityValByGroupId(String domainName, String groupId) {
        return CCUHsApi.getInstance().readPointPriorityValByQuery("point and domainName == \"" + domainName + "\" " +
                " and group == \"" + groupId + "\"");
    }

    public static Double readPriorityValByRoomRef(String domainName, String roomRef) {
        return CCUHsApi.getInstance().readPointPriorityValByQuery("point and domainName == \"" + domainName + "\" " +
                " and roomRef == \"" + roomRef + "\"");
    }

    public static Double readHisValByEquipRef(String domainName, String equipRef) {
        return CCUHsApi.getInstance().readHisValByQuery("point and domainName == \"" + domainName + "\" " +
                " and equipRef ==\"" + equipRef + "\"");
    }

    public static int getSecondaryColor() {
        if (BuildConfig.BUILD_TYPE.equals(CARRIER_PROD)) {
            return ContextCompat.getColor(context, R.color.carrier_75f_secondary);
        } else if (BuildConfig.BUILD_TYPE.equals(AIROVERSE_PROD)) {
            return ContextCompat.getColor(context, R.color.airoverse_secondary);
        } else {
            return ContextCompat.getColor(context, R.color.renatus_75f_secondary);
        }
    }

    public static String getAppVersion(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null; // app not installed
        }
    }

    public static void UpdateAppRestartCause(String cause) {
        Globals.getInstance().getApplicationContext().getSharedPreferences("crash_preference", Context.MODE_PRIVATE)
                .edit()
                .putString("app_restart_cause", cause).commit();
        CcuLog.i("USER_TEST", "App restart cause updated to: " + cause);
    }

    public static void updateBackgroundWaterMaker(View view){
        if(view!=null && !BuildConfig.BUILD_TYPE.equals("carrier_prod")){
            view.setBackgroundResource(R.drawable.bg_logoscreen);
        }
    }

}
