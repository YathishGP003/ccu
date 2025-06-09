package a75f.io.renatus.registration;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import a75f.io.renatus.util.CCUUiUtil;

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

    @SuppressLint("RestrictedApi")
    public static void showConfirmationMessage(View view, String title, String text, Runnable action) {
        Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE);

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.WHITE);

        @SuppressLint("RestrictedApi") Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbarView;
        FrameLayout.LayoutParams snackParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        snackParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        snackbarLayout.setLayoutParams(snackParams);
        snackbarLayout.removeAllViews();

        LinearLayout layout = new LinearLayout(view.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layout.setLayoutParams(layoutParams);

        TextView titleView = new TextView(view.getContext());
        titleView.setText(title);
        titleView.setTextSize(22);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(Color.RED);


        TextView messageView = new TextView(view.getContext());
        messageView.setText(text);
        messageView.setTextSize(20);
        messageView.setTextColor(Color.BLACK);
        messageView.setMaxLines(4);
        messageView.setEllipsize(null);

        Button okButton = new Button(view.getContext());
        okButton.setText("OK");
        okButton.setTextSize(18);
        okButton.setOnClickListener(v -> {
            snackbar.dismiss();
            action.run();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM | Gravity.END;
        okButton.setLayoutParams(params);
        okButton.setBackgroundColor(CCUUiUtil.getSecondaryColor());
        okButton.setPadding(36, 16, 36, 16);

        layout.addView(titleView);
        layout.addView(messageView);
        layout.addView(okButton);

        snackbarLayout.addView(layout);
        snackbar.show();
    }
}
