package a75f.io.renatus;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.cloud.OtpManager;
import a75f.io.logic.cloud.OtpResponseCallBack;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.renatus.ENGG.AppInstaller;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Mahesh on 17-07-2019.
 */
public class AboutFragment extends Fragment {


    private boolean mCCUAppDownloaded = false;
    private boolean mHomeAppDownloaded = false;
    private CountDownTimer otpCountDownTimer;
    @BindView(R.id.tvSerialNumber)
    TextView tvSerialNumber;
    @BindView(R.id.tvCcuVersion)
    TextView tvCcuVersion;
    @BindView(R.id.tvSiteId)
    TextView tvSiteId;

    ProgressBar loading;
    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;
    TextView tvmessage;
    public AboutFragment() {

    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                Log.d("CCU_DOWNLOAD", String.format("Received download complete for %d from %d and %d", downloadId, AppInstaller.getHandle().getCCUAppDownloadId(), AppInstaller.getHandle().getHomeAppDownloadId()));
                if (downloadId == AppInstaller.getHandle().getCCUAppDownloadId())
                    mCCUAppDownloaded = true;

                if (mCCUAppDownloaded) {
                    mCCUAppDownloaded = false;
                    mHomeAppDownloaded = false;
                    final boolean bNewHomeAppAvailable =false;
                    final boolean bNewCCUAppAvailable = AppInstaller.getHandle().isNewCCUAppAvailable();

                    if (bNewCCUAppAvailable || bNewHomeAppAvailable) {
                        if (alertDialog != null && alertDialog.isShowing()) {
                            loading.setVisibility(View.GONE);
							if (bNewCCUAppAvailable)
                                tvmessage.setText("New updates are available for Central Control Unit. Close to proceed.");
                            else if (bNewHomeAppAvailable)
                                tvmessage.setText("New updates are available for 75F Core Services Platform. Close to proceed.");
                            alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    AppInstaller.getHandle().install(getActivity(), bNewHomeAppAvailable, bNewCCUAppAvailable, true);
                                }
                            });
                        }
                    } else {
                        if (alertDialog != null && alertDialog.isShowing()) {
                            loading.setVisibility(View.GONE);
                            tvmessage.setText("No new updates available to install at this time.");
                        }
                    }
                }
            }
        }
    };
    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);
        if (getActivity() != null) {
            getActivity().registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
        ButterKnife.bind(this, rootView);

        HashMap site = CCUHsApi.getInstance().read("site");

        PackageManager pm = getActivity().getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0);
            String str = pi.versionName + "." + pi.versionCode;
            tvCcuVersion.setText(str);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String siteUID = CCUHsApi.getInstance().getSiteIdRef().toString();
        tvSiteId.setText(siteUID == null? site.get("id").toString() :siteUID);

        String ccuUID = CCUHsApi.getInstance().getCcuRef().toString();
        tvSerialNumber.setText(ccuUID == null ? CCUHsApi.getInstance().getCcuRef().toString() :ccuUID);
        return rootView;
    }

    @OnClick(R.id.otpGenerator)
    public void generateOTP(){
        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
        View dialogView= LayoutInflater.from(getContext()).inflate(R.layout.otp_generator_dailog,null);
        TextView otpValue = dialogView.findViewById(R.id.otpValue);
        alertDialog.setView(dialogView);
        alertDialog.setCancelable(false);
        alertDialog.show();
        TextView otpTimer = dialogView.findViewById(R.id.otptimer);
        EditText emailId = dialogView.findViewById(R.id.otpEmailId);
        emailId.setEnabled(false);
        emailId.setVisibility(View.GONE);
        setOTPValue(otpValue, otpTimer, emailId);
        ImageView otpRegenerate = dialogView.findViewById(R.id.otpRegenerate);
        refreshOTPValue(otpValue, otpRegenerate, otpTimer, emailId);
        Button otpEmail = dialogView.findViewById(R.id.buttonEmailOtp);
        emailOTP(emailId, otpEmail, alertDialog);
    }

    private void constructOTPValidationTime(String otpCreatedTime, TextView otpTimer) throws ParseException {
        Date otpGeneratedDate;
        SimpleDateFormat utcDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        otpGeneratedDate = utcDateFormat.parse(otpCreatedTime);
        long millisOTPAlive = otpGeneratedDate.getTime() - new Date().getTime();
        if(otpCountDownTimer != null){
            otpCountDownTimer.cancel();
        }
        otpCountDownTimer = getOtpCountDownTimerForOTP(otpTimer, millisOTPAlive);
        otpCountDownTimer.start();

    }

    @NotNull
    private CountDownTimer getOtpCountDownTimerForOTP(TextView otpTimer, long millisOTPAlive) {
        return new CountDownTimer(millisOTPAlive, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                NumberFormat f = new DecimalFormat("00");
                long hour = (millisUntilFinished / 3600000) % 24;
                long min = (millisUntilFinished / 60000) % 60;
                long sec = (millisUntilFinished / 1000) % 60;
                otpTimer.setText("It will expire in " + f.format(hour) + ":" + f.format(min) + ":" + f.format(sec) +
                        " hours");
            }
            @Override
            public void onFinish() {
                otpTimer.setText("OTP has expired. Please refresh to generate a new.");
            }
        };
    }

    private void emailOTP(EditText emailId, Button otpEmail, AlertDialog alertDialog) {
        otpEmail.setOnClickListener(v -> {
            if(emailId.getText().toString().isEmpty()){
                alertDialog.dismiss();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(emailId.getText().toString()).matches()){
                emailId.setError("Enter valid email id");
                return;
            }
            OtpResponseCallBack otpResponseCallBack = new OtpResponseCallBack() {
                @Override
                public void onOtpResponse(JSONObject response) throws JSONException {
                    Toast.makeText(getContext(),"email sent", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                }

                @Override
                public void onOtpErrorResponse(JSONObject response) throws JSONException {
                    Toast.makeText(getContext(),"Error in sending email", Toast.LENGTH_LONG).show();
                    alertDialog.dismiss();
                }
            };
            new OtpManager().postOTPShare(tvSiteId.getText().toString(), emailId.getText().toString(),
                    otpResponseCallBack);
        });
    }

    private void refreshOTPValue(TextView otpValue, ImageView otpRegenerate, TextView otpTimer, EditText emailId) {
        otpRegenerate.setOnClickListener(v -> {
            OtpResponseCallBack otpResponseCallBack = new OtpResponseCallBack() {
                @Override
                public void onOtpResponse(JSONObject response) throws JSONException {
                    ProgressDialogUtils.hideProgressDialog();
                    String otpGenerated = (String) ((JSONObject) response.get("siteCode")).get("code");
                    PreferenceUtil.setOTPGeneratedToAddOrReplaceCCU(otpGenerated);
                    emailId.setEnabled(true);
                    emailId.setVisibility(View.VISIBLE);
                    otpValue.setText(otpWithDoubleSpaceBetween(otpGenerated));
                    String expirationDateTime = (String) ((JSONObject) response.get("siteCode")).get(
                            "expirationDateTime");
                    try {
                        constructOTPValidationTime(expirationDateTime, otpTimer);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onOtpErrorResponse(JSONObject response) throws JSONException {
                    ProgressDialogUtils.hideProgressDialog();
                    String errorMessage = (String) response.get("response");
                    otpValue.setText(errorMessage);
                }
            };
            ProgressDialogUtils.showProgressDialog(getActivity(), "Fetching new OTP");
            new OtpManager().postOTPRefresh(tvSiteId.getText().toString(), otpResponseCallBack);
        });
    }

    private void setOTPValue(TextView otpValue, TextView otpTimer, EditText emailId) {
        OtpResponseCallBack otpResponseCallBack = new OtpResponseCallBack() {
            @Override
            public void onOtpResponse(JSONObject response) throws JSONException {
                ProgressDialogUtils.hideProgressDialog();
                String otpGenerated = (String) ((JSONObject) response.get("siteCode")).get("code");
                if(!PreferenceUtil.getOTPGeneratedToAddOrReplaceCCU().equals(otpGenerated)){
                    emailId.setEnabled(true);
                    emailId.setVisibility(View.VISIBLE);
                }
                PreferenceUtil.setOTPGeneratedToAddOrReplaceCCU(otpGenerated);
                otpValue.setText(otpWithDoubleSpaceBetween(otpGenerated));
                String expirationDateTime = (String) ((JSONObject) response.get("siteCode")).get(
                        "expirationDateTime");
                try {
                    constructOTPValidationTime(expirationDateTime, otpTimer);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onOtpErrorResponse(JSONObject response) throws JSONException {
                ProgressDialogUtils.hideProgressDialog();
                String errorMessage = (String) response.get("response");
                otpValue.setText(errorMessage);
            }
        };
        ProgressDialogUtils.showProgressDialog(getActivity(), "Fetching OTP");
        new OtpManager().getOTP(tvSiteId.getText().toString(), otpResponseCallBack);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
     @OnClick(R.id.checkUpdatesInstll)
    public void checkAndUpdate(){
        builder = new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle);
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.updates_custom_dialog, null);
        getActivity().findViewById(R.id.layout_root);
        TextView tv = (TextView) layout.findViewById(R.id.title);
        tv.setText("Download Updates");
        tvmessage = (TextView) layout.findViewById(R.id.text);
        loading = (ProgressBar) layout.findViewById(R.id.loading);
        tvmessage.setText("Checking for updates. Please wait...");
        Button close = (Button) layout.findViewById(R.id.close_button);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        builder.setView(layout);
        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();

        mCCUAppDownloaded = false;
        mHomeAppDownloaded = false;
        AppInstaller.getHandle().downloadInstalls();
    }

    @Override
    public void onDestroyView() {
        if (getActivity() != null) {
            getActivity().unregisterReceiver(receiver);
        }
        super.onDestroyView();
    }

    private String otpWithDoubleSpaceBetween(String otp){
        StringBuffer otpwithSpace = new StringBuffer();
        String doubleSpace = "  ";
        for(Character ch : otp.toCharArray()){
            otpwithSpace.append(ch);
            otpwithSpace.append(doubleSpace);
            otpwithSpace.append(doubleSpace);
        }
        return otpwithSpace.toString().trim();
    }
}
