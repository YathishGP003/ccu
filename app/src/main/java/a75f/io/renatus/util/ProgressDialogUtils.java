package a75f.io.renatus.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.R;
import a75f.io.renatus.views.GifView;

/**
 * Created by mahesh on 24-09-2019.
 */
public class ProgressDialogUtils {

    //
    private static Dialog progressDialog;

    public static void showProgressDialog(Context context, String message) {
        if (progressDialog == null) {
            progressDialog = new Dialog(context);
            progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            progressDialog.setContentView(R.layout.progress_dialog);
            progressDialog.setCancelable(false);
            GifView gifView = progressDialog.findViewById(R.id.gifLoader);
            TextView tv = progressDialog.findViewById(R.id.tvMessage);
            if (!TextUtils.isEmpty(message) || message != null) {
                tv.setVisibility(View.VISIBLE);
                tv.setText(message);
            } else {
                tv.setVisibility(View.GONE);
            }
            if(CCUUiUtil.isDaikinEnvironment(context))
                gifView.setImageResource(R.drawable.daikin_loader);
            else
                gifView.setImageResource(R.drawable.loader1);
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            progressDialog.show();
        }
    }

    public static void hideProgressDialog() {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public static boolean isDialogShowing() {
        return progressDialog != null && progressDialog.isShowing();
    }
}
