package a75f.io.renatus.util;

import android.app.Activity;

import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.R;
import androidx.appcompat.app.AlertDialog;

import static a75f.io.renatus.util.RxjavaUtil.executeBackground;

public class SystemProfileUtil {
    
    public static void setUserIntentBackground(String query, double val) {
        executeBackground(() -> TunerUtil.writeSystemUserIntentVal(query, val));
    }
    
    public static void showConditioningDisabledDialog(Activity context, SystemMode mode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.NewDialogStyle);
        String str = "Conditioning Mode changed from '" + mode.name() + "' to '" + SystemMode.OFF.name() + "' based " +
                     "on changed equipment selection.\nPlease select appropriate conditioning mode from System Settings.";
        builder.setCancelable(false)
               .setPositiveButton("OK", (dialog, id) -> dialog.cancel())
               .setTitle("System Conditioning Mode Changed")
               .setMessage(str);
    
        AlertDialog dlg = builder.create();
        dlg.show();
        SystemProfileUtil.setUserIntentBackground("conditioning and mode", SystemMode.OFF.ordinal());
    }
}
