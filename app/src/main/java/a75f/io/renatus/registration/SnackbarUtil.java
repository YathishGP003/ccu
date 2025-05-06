package a75f.io.renatus.registration;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

public class SnackbarUtil {
    public static void showInfoMessage(View view, String text) {
        Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG)
                .setAction("OK", null);

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.WHITE);
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextSize(24);
        textView.setTextColor(Color.RED);

        TextView actionView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_action);
        actionView.setTextSize(18);
        snackbarView.setPadding(32, 32, 32, 32);
        snackbar.show();
    }

    public static void showConfirmationMessage(View view, String text, Runnable action) {
        Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", v -> action.run());

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.WHITE);
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextSize(24);
        textView.setTextColor(Color.RED);

        TextView actionView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_action);
        actionView.setTextSize(18);
        snackbarView.setPadding(32, 32, 32, 32);
        snackbar.show();
    }
}
