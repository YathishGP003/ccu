package a75f.io.renatus;

import static a75f.io.renatus.util.CCUUiUtil.isCurrentVersionHigherOrEqualToRequired;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.domain.api.Domain;
import a75f.io.domain.equips.CCUDiagEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.cloud.OtpManager;
import a75f.io.logic.cloud.ResponseCallback;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.renatus.ENGG.AppInstaller;
import a75f.io.renatus.util.CloudStatus;
import a75f.io.renatus.util.CloudStatusAdapter;
import a75f.io.renatus.util.DialogManager;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.util.remotecommand.bundle.BundleInstallListener;
import a75f.io.renatus.util.remotecommand.bundle.BundleInstallManager;
import a75f.io.renatus.util.remotecommand.bundle.models.UpgradeBundle;
import a75f.io.util.ExecutorTask;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Mahesh on 17-07-2019.
 */
public class AboutFragment extends Fragment implements BundleInstallListener {


    private boolean mCCUAppDownloaded = false;
    private CountDownTimer otpCountDownTimer;
    @BindView(R.id.tvSerialNumber)
    TextView tvSerialNumber;
    @BindView(R.id.tvCcuVersion)
    TextView tvCcuVersion;
    @BindView(R.id.validOTP)
    TextView validOTP;
    @BindView(R.id.otpCountDown)
    TextView otpCountDown;
    @BindView(R.id.tvSiteId)
    TextView tvSiteId;
    @BindView(R.id.latestVersion)
    TextView latestVersion;
    @BindView(R.id.update_ccu_screen)
    LinearLayout updateScreenLayout;
    @BindView(R.id.update_ccu)
    TextView updateCCU;
    @BindView(R.id.update_app_text)
    LinearLayout updateAppText;
    @BindView(R.id.layoutId)
    LinearLayout linearLayout;
    @BindView(R.id.progress_bar)
    LinearProgressIndicator progressBar;
    @BindView(R.id.progress_bar_layout)
    LinearLayout progress_bar_layout;
    @BindView(R.id.error_message_layout)
    LinearLayout error_message_layout;
    @BindView(R.id.error_image_layout)
    LinearLayout error_image_layout;

    @BindView(R.id.downloading_text)
    TextView downloadingText;
    String bundleLabelString;
    int percComplete;
    String fileSize;
    @BindView(R.id.update_status)
    TextView updateStatus;
    @BindView(R.id.version_text)
    TextView verSionText;
    @BindView(R.id.latest_version_text)
    TextView latest_version_text;
    @BindView(R.id.connectivityIssues)
    LinearLayout connectivityIssues;
    @BindView(R.id.layout_connection_up)
    LinearLayout connectionUpLayout;
    @BindView(R.id.connection_down_layout)
    LinearLayout connectionDownLayout;

    @BindView(R.id.download_perc)
    TextView downloadPerc;

    @BindView(R.id.imBundleHint)
    ImageView imBundleHint;

    EditText code1;
    EditText code2;
    EditText code3;
    EditText code4;
    EditText code5;
    EditText code6;
    Button next;
    String enteredPassCode;

    ProgressBar loading;
    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;
    TextView tvmessage;
    private boolean isPopupVisible = false;
    private PopupWindow popupWindow;
    @BindView(R.id.cloudStatusRecyclerView)
    RecyclerView cloudStatusRecyclerView;
    @BindView(R.id.tvCloudStatus)
    TextView tvCloudStatus;

    public AboutFragment() {

    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                CcuLog.d("CCU_DOWNLOAD", String.format("Received download complete for %d from %d and %d", downloadId, AppInstaller.getHandle().getCCUAppDownloadId(), AppInstaller.getHandle().getHomeAppDownloadId()));
                if (downloadId == AppInstaller.getHandle().getCCUAppDownloadId())
                    mCCUAppDownloaded = true;

                if (mCCUAppDownloaded) {
                    mCCUAppDownloaded = false;
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


        String softwareVersion = "";
        String bundleVersion = Domain.diagEquip.getBundleVersion().readDefaultStrVal();

        if (!bundleVersion.isEmpty()) {
            softwareVersion = bundleVersion;
        } else {
            PackageManager pm = Globals.getInstance().getApplicationContext().getPackageManager();
            PackageInfo pi;
            try {
                pi = pm.getPackageInfo("a75f.io.renatus", 0);
                softwareVersion =  pi.versionName.substring(pi.versionName.lastIndexOf('_')+1);
            } catch (PackageManager.NameNotFoundException e) {
                CcuLog.e(L.TAG_CCU_UI, "Error getting package info for renatus", e);
                e.printStackTrace();
            }
        }

        tvCcuVersion.setText(softwareVersion);

        String siteUID = CCUHsApi.getInstance().getSiteIdRef().toString();
        tvSiteId.setText(siteUID == null? site.get("id").toString() :siteUID);

        String ccuUID = CCUHsApi.getInstance().getCcuRef().toString();
        tvSerialNumber.setText(ccuUID == null ? CCUHsApi.getInstance().getCcuRef().toString() :ccuUID);
        setOTPOnAboutPage();
        updateCloudStatusView();
        if(PreferenceUtil.getUpdateCCUStatusInAboutScreen()){
            getRecommendedCCUDataFromPreference();
            startDownloadingApk();
        } else if(PreferenceUtil.isCCUInstallingInAboutScreen()){
            installNewApk();
        } else {
            checkIsCCUHasRecommendedVersion(getActivity());
        }
        imBundleHint.setOnClickListener(imBundleHintListener);
       return rootView;
    }

    private void updateCloudStatusView() {
        CcuLog.d(L.TAG_CCU_CLOUD_STATUS, "Checking all services");
        CloudStatus.INSTANCE.checkAllServices(serviceStatuses -> {
            for (CloudStatus.ServiceStatus status : serviceStatuses) {
                CcuLog.d(L.TAG_CCU_CLOUD_STATUS,  "Health check URL " + status.getHealthCheckUrl()
                        + " " + status.getServiceName() + " is " + (status.isUp()? "UP" : "DOWN") + "\n"+
                        "Version check URL "+ status.getInfoUrl()+ " Version: " + status.getServiceVersion());
            }

            tvCloudStatus.setVisibility(View.VISIBLE);
            cloudStatusRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
            cloudStatusRecyclerView.setAdapter(new CloudStatusAdapter(serviceStatuses));
            ProgressDialogUtils.hideProgressDialog();
            return null;
        });
    }

    private void getRecommendedCCUDataFromPreference() {
        bundleLabelString = PreferenceUtil.getStringPreference("versionLabel");
        percComplete = PreferenceUtil.getIntPreference("percComplete");
    }


    private void installNewApk() {
        PreferenceUtil.installCCUInAboutScreen();
        linearLayout.setVisibility(View.VISIBLE);
        updateAppText.setVisibility(View.GONE);
        updateCCU.setVisibility(View.GONE);

        // Set visibility again as it calls from onResume too
        progress_bar_layout.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.VISIBLE);
        connectivityIssues.setVisibility(View.GONE);
        updateScreenLayout.setVisibility(View.VISIBLE);
        downloadingText.setVisibility(View.VISIBLE);
        latestVersion.setText(bundleLabelString);
        downloadPerc.setVisibility(View.VISIBLE);
    }

    public void checkIsCCUHasRecommendedVersion(FragmentActivity activity) {
        ExecutorTask.executeAsync(
                () -> ProgressDialogUtils.showProgressDialog(activity, "Checking for recommended version and fetching cloud status"),
                () -> {
                    // Retrieve the recommended upgrade bundle
                    BundleInstallManager bundleInstallManager = BundleInstallManager.Companion.getInstance();
                    UpgradeBundle upgradeBundle = bundleInstallManager.getRecommendedUpgradeBundle(false);

                    // Handle the case where CCU is offline
                    if (upgradeBundle == null) {
                        postToMainThread(() -> {
                            connectionDownLayout.setVisibility(View.VISIBLE);
                            connectionUpLayout.setVisibility(View.GONE);
                        });
                    } else if(upgradeBundle.getUpgradeOkay() && upgradeBundle.getComponentsToUpgrade().size() == 0){
                        postToMainThread(this::softwareIsUpToDate);
                    } else {
                        postToMainThread(() -> {
                            latestVersion.setText(upgradeBundle.component1().getBundleName());
                            bundleLabelString = upgradeBundle.component1().getBundleName();
                            downloadingText.setVisibility(View.GONE);
                        });

                        // Handle error messages in the upgrade bundle
                        if (!upgradeBundle.getErrorMessages().isEmpty()) {
                            postToMainThread(() -> handleUpgradeBundleErrors(upgradeBundle.getErrorMessages()));
                        } else {
                            postToMainThread(() -> setupUpdateCCUButton(upgradeBundle, bundleInstallManager));
                        }
                    }
                },
                ()->{
                    // Progress bar will be hidden when CCU successfully fetched cloud status
                    CcuLog.d(L.TAG_CCU_UI, "Checking for recommended version and cloud status completed, Hiding progress dialog");
                }
        );
    }

    private void handleUpgradeBundleErrors(List<String> upgradeBundleErrorMessages) {
        downloadingText.setVisibility(View.GONE);
        progress_bar_layout.setVisibility(View.GONE);
        updateCCU.setVisibility(View.GONE);
        error_message_layout.setVisibility(View.VISIBLE);
        error_image_layout.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.VISIBLE);

        // Build and display error messages
        StringBuilder errorMessages = new StringBuilder();
        error_message_layout.removeAllViews(); // Clear any previous messages

        if (!upgradeBundleErrorMessages.isEmpty()) {
            for (int i = 0; i < upgradeBundleErrorMessages.size(); i++) {
                // Create a horizontal LinearLayout for each message with a number and message text
                LinearLayout messageRow = new LinearLayout(requireContext());
                messageRow.setOrientation(LinearLayout.HORIZONTAL);

                TextView numberView = new TextView(requireContext());
                numberView.setText((i + 1) + ". ");
                messageRow.addView(numberView);
                numberView.setTextAppearance(requireContext(), R.style.title_normal);
                numberView.setTextSize(20);


                // Create and set the error message TextView
                TextView messageView = new TextView(requireContext(), null);
                messageView.setText(upgradeBundleErrorMessages.get(i));
                messageView.setTextAppearance(requireContext(), R.style.title_normal); // Apply your style
                messageView.setTextSize(20);
                messageView.setPadding(0, 8, 0, 0);
                messageRow.addView(messageView);

                // Add the constructed row to the parent layout
                error_message_layout.addView(messageRow);
            }
        }

        CcuLog.d(L.TAG_CCU_BUNDLE, errorMessages.toString());
    }

    private void setupUpdateCCUButton(UpgradeBundle upgradeBundle, BundleInstallManager bundleInstallManager) {
        updateCCU.setEnabled(true);
        if(upgradeBundle.getErrorMessages().isEmpty()) {
            error_message_layout.setVisibility(View.GONE);
            error_image_layout.setVisibility(View.GONE);
        }
        updateCCU.setOnClickListener(v -> {
            if ("UPDATE NOW".equalsIgnoreCase(updateCCU.getText().toString())) {
                startUpdateProcess(upgradeBundle, bundleInstallManager);
            } else {
                cancelUpdateProcess(bundleInstallManager);
            }
        });

        // when onResume is called, the progress bar is set to 0, so we need to set it to the last known value
        if(PreferenceUtil.isCCUInstallingInAboutScreen()){
            progressBar.setProgressCompat(percComplete, true);
            downloadingText.setText("Installing Bundle");
            String perc = percComplete + "%";
            downloadPerc.setText(perc);
            installNewApk();
        }
    }

    private void startUpdateProcess(UpgradeBundle upgradeBundle, BundleInstallManager bundleInstallManager) {
        CcuLog.d(L.TAG_CCU_BUNDLE, "Bundle upgrade initiated from About screen");
        startDownloadingApk();
        PreferenceUtil.startUpdateCCUInAboutScreen();
        PreferenceUtil.setStringPreference("versionLabel", upgradeBundle.component1().getBundleName());
        RxjavaUtil.executeBackgroundWithDisposable(() -> {
            bundleInstallManager.initiateBundleUpgrade(upgradeBundle, this);
        });
    }

    private void cancelUpdateProcess(BundleInstallManager bundleInstallManager) {
        updateCCU.setVisibility(View.VISIBLE);
        updateCCU.setEnabled(true);
        updateCCU.setText("UPDATE NOW");

        connectivityIssues.setVisibility(View.GONE);
        updateAppText.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.GONE);
        progress_bar_layout.setVisibility(View.GONE);
        PreferenceUtil.stopUpdateCCUInAboutScreen();
        PreferenceUtil.installationCompletedInAboutScreen();
        bundleInstallManager.cancelBundleInstallation();
    }

    private void postToMainThread(Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }


    private void startDownloadingApk(){
        progress_bar_layout.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.VISIBLE);
        connectivityIssues.setVisibility(View.GONE);
        updateScreenLayout.setVisibility(View.VISIBLE);
        downloadingText.setVisibility(View.VISIBLE);
        updateCCU.setText("CANCEL");
        latestVersion.setText(bundleLabelString);
    }

    View.OnClickListener imBundleHintListener = view -> {

        if (isPopupVisible) {
            imBundleHint.setAlpha(0.5f);
            isPopupVisible = false;
        } else {
            imBundleHint.setAlpha(1f);
            isPopupVisible = true;

            imBundleHint.setAlpha(1f);
            LayoutInflater inflater = getLayoutInflater();
            View tooltipView = inflater.inflate(R.layout.tooltip_layout, null);
            TextView ccuToolTip = tooltipView.findViewById(R.id.tvTooltipCCU);
            TextView homeAppToolTip = tooltipView.findViewById(R.id.tvTooltipHomeApp);
            TextView remoteAppToolTip = tooltipView.findViewById(R.id.tvTooltipRemoteApp);
            TextView bacAppToolTip = tooltipView.findViewById(R.id.tvTooltipBACApp);

            CCUDiagEquip ccuDiagEquip = Domain.diagEquip;

            ccuToolTip.setText("CCU:v" + ccuDiagEquip.getAppVersion().readDefaultStrVal());

            if(ccuDiagEquip.getHomeAppVersion().readDefaultStrVal() != ""){
                homeAppToolTip.setText("HomeApp:v" + ccuDiagEquip.getHomeAppVersion().readDefaultStrVal());
            } else {
                homeAppToolTip.setText("HomeApp: Not Installed");
            }

            if(ccuDiagEquip.getRemoteAccessAppVersion().readDefaultStrVal() != ""){
                remoteAppToolTip.setText("RemoteApp:v" + ccuDiagEquip.getRemoteAccessAppVersion().readDefaultStrVal());
            } else {
                remoteAppToolTip.setText("RemoteApp: Not Installed");
            }

            if(ccuDiagEquip.getBacnetAppVersion().readDefaultStrVal() != ""){
                bacAppToolTip.setText("BAC App:v" + ccuDiagEquip.getBacnetAppVersion().readDefaultStrVal());
            } else {
                bacAppToolTip.setText("BAC App: Not Installed");
            }

            popupWindow = new PopupWindow(tooltipView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true); // Focusable set to true

            popupWindow.setOnDismissListener(() -> {
                imBundleHint.setAlpha(0.5f);
                isPopupVisible = false;
            });

            popupWindow.setBackgroundDrawable(new ColorDrawable());
            popupWindow.setOutsideTouchable(true);
            int[] location = new int[2];
            tvCcuVersion.getLocationOnScreen(location);

            int xOffset = tvCcuVersion.getWidth() - popupWindow.getWidth(); // Align to the end of TextView
            int yOffset = tvCcuVersion.getHeight(); // Position below the TextView

            popupWindow.showAtLocation(tvCcuVersion, Gravity.NO_GRAVITY, location[0] + xOffset, location[1] + yOffset);
        }
    };

    private void softwareIsUpToDate() {
        updateStatus.setText("Software is upto date");
        updateAppText.setVisibility(View.GONE);
        updateCCU.setVisibility(View.GONE);
        verSionText.setVisibility(View.GONE);
        latest_version_text.setVisibility(View.GONE);
        latestVersion.setVisibility(View.GONE);
        downloadingText.setVisibility(View.GONE);
        progress_bar_layout.setVisibility(View.GONE);
        PreferenceUtil.installationCompletedInAboutScreen();
    }

    private void setOTPOnAboutPage(){
        ResponseCallback responseCallBack = new ResponseCallback() {
            @Override
            public void onSuccessResponse(JSONObject response) throws JSONException {
                    String otpGenerated = (String) ((JSONObject) response.get("siteCode")).get("code");
                    validOTP.setText(otpGenerated);
                String expirationDateTime = (String) ((JSONObject) response.get("siteCode")).get(
                        "expirationDateTime");
                try {
                    constructOTPValidationTime(expirationDateTime, otpCountDown, true);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onErrorResponse(JSONObject response) {

            }
        };
        if(!PreferenceUtil.getOTPGeneratedToAddOrReplaceCCU().isEmpty()) {
            new OtpManager().getOTP(tvSiteId.getText().toString(), responseCallBack, true);
        }
    }

    @OnClick(R.id.otpGenerator)
    public void generateOTP(){
        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
        View dialogView= LayoutInflater.from(getContext()).inflate(R.layout.otp_generator_dailog,null);
        TextView otpValue = dialogView.findViewById(R.id.otpValue);
        alertDialog.setView(dialogView);
        alertDialog.setCancelable(false);
        alertDialog.show();
        DialogManager.INSTANCE.register(alertDialog);
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

    private void constructOTPValidationTime(String otpCreatedTime, TextView otpTimer, boolean isFromAboutPage) throws ParseException {
        Date otpGeneratedDate;
        SimpleDateFormat utcDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        otpGeneratedDate = utcDateFormat.parse(otpCreatedTime);
        long millisOTPAlive = otpGeneratedDate.getTime() - new Date().getTime();
        if(otpCountDownTimer != null){
            otpCountDownTimer.cancel();
        }
        otpCountDownTimer = getOtpCountDownTimerForOTP(otpTimer, millisOTPAlive, isFromAboutPage);
        otpCountDownTimer.start();
    }

    @NotNull
    private CountDownTimer getOtpCountDownTimerForOTP(TextView otpTimer, long millisOTPAlive, boolean isFromAboutPage) {
        return new CountDownTimer(millisOTPAlive, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                NumberFormat f = new DecimalFormat("00");
                long hour = (millisUntilFinished / 3600000) % 24;
                long min = (millisUntilFinished / 60000) % 60;
                long sec = (millisUntilFinished / 1000) % 60;
                StringBuffer otpExpirationTimer = new StringBuffer();
                if(isFromAboutPage){
                    otpExpirationTimer.append("(");
                    otpExpirationTimer.append("Expires in ");
                }
                else{
                    otpExpirationTimer.append("It will expire in " );
                }
                otpExpirationTimer.append(f.format(hour));
                otpExpirationTimer.append(":");
                otpExpirationTimer.append(f.format(min));
                otpExpirationTimer.append(":" );
                otpExpirationTimer.append(f.format(sec));
                otpExpirationTimer.append(" hours");
                if(isFromAboutPage){
                    otpExpirationTimer.append(")");
                }
                otpTimer.setText(otpExpirationTimer.toString());
            }
            @Override
            public void onFinish() {
                if(isFromAboutPage){
                    otpTimer.setText("");
                }
                else{
                    otpTimer.setText("Passcode has expired. Please refresh to generate a new.");
                }
            }
        };
    }

    private void emailOTP(EditText emailId, Button otpEmail, AlertDialog alertDialog) {
        otpEmail.setOnClickListener(v -> {
            if(emailId.getText().toString().isEmpty()){
                alertDialog.dismiss();
                setOTPOnAboutPage();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(emailId.getText().toString()).matches()){
                emailId.setError("Enter valid email id");
                return;
            }
            ResponseCallback responseCallBack = new ResponseCallback() {
                @Override
                public void onSuccessResponse(JSONObject response) throws JSONException {
                    Toast.makeText(getContext(),"email sent", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                    setOTPOnAboutPage();
                }

                @Override
                public void onErrorResponse(JSONObject response) throws JSONException {
                    Toast.makeText(getContext(),"Error in sending email", Toast.LENGTH_LONG).show();
                    alertDialog.dismiss();
                    setOTPOnAboutPage();
                }
            };
            new OtpManager().postOTPShare(tvSiteId.getText().toString(), emailId.getText().toString(),
                    responseCallBack);
        });
    }

    private void refreshOTPValue(TextView otpValue, ImageView otpRegenerate, TextView otpTimer, EditText emailId) {
        otpRegenerate.setOnClickListener(v -> {
            ResponseCallback responseCallBack = new ResponseCallback() {
                @Override
                public void onSuccessResponse(JSONObject response) throws JSONException {
                    ProgressDialogUtils.hideProgressDialog();
                    String otpGenerated = null;
                    otpGenerated = (String) ((JSONObject) response.get("siteCode")).get("code");
                    PreferenceUtil.setOTPGeneratedToAddOrReplaceCCU(otpGenerated);
                    emailId.setEnabled(true);
                    emailId.setVisibility(View.VISIBLE);
                    otpValue.setText(otpWithDoubleSpaceBetween(otpGenerated));
                    String expirationDateTime = (String) ((JSONObject) response.get("siteCode")).get(
                            "expirationDateTime");
                    try {
                        constructOTPValidationTime(expirationDateTime, otpTimer, false);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onErrorResponse(JSONObject response) throws JSONException {
                    ProgressDialogUtils.hideProgressDialog();
                    String errorMessage = errorMessage = (String) response.get("response");
                    otpValue.setText(errorMessage);
                }
            };
            ProgressDialogUtils.showProgressDialog(getActivity(), "Fetching new Passcode");
            new OtpManager().postOTPRefresh(tvSiteId.getText().toString(), responseCallBack);
        });
    }

    private void setOTPValue(TextView otpValue, TextView otpTimer, EditText emailId) {
        ResponseCallback responseCallBack = new ResponseCallback() {
            @Override
            public void onSuccessResponse(JSONObject response) throws JSONException {
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
                    constructOTPValidationTime(expirationDateTime, otpTimer, false);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onErrorResponse(JSONObject response) throws JSONException {
                ProgressDialogUtils.hideProgressDialog();
                String errorMessage = null;
                errorMessage = (String) response.get("response");
                otpValue.setText(errorMessage);
            }
        };
        ProgressDialogUtils.showProgressDialog(getActivity(), "Fetching Passcode");
        new OtpManager().getOTP(tvSiteId.getText().toString(), responseCallBack, false);
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
        close.setOnClickListener(v -> alertDialog.dismiss());
        builder.setView(layout);
        builder.setCancelable(false);
        alertDialog = builder.create();
        DialogManager.INSTANCE.register(alertDialog);
        alertDialog.show();

        mCCUAppDownloaded = false;
         AppInstaller.getHandle().downloadInstalls();
    }

    @Override
    public void onDestroyView() {
        if (getActivity() != null) {
            getActivity().unregisterReceiver(receiver);
        }
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        if (isPopupVisible && popupWindow != null) {
            popupWindow.dismiss();
        }
        super.onPause();
    }
    private String otpWithDoubleSpaceBetween(String otp){
        StringBuilder otpwithSpace = new StringBuilder();
        String doubleSpace = "  ";
        for(Character ch : otp.toCharArray()){
            otpwithSpace.append(ch);
            otpwithSpace.append(doubleSpace);
            otpwithSpace.append(doubleSpace);
        }
        return otpwithSpace.toString().trim();
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.activateSession)
    public void activateSession(){
        AlertDialog alertActivateSession = new AlertDialog.Builder(getActivity()).create();
        View dialogView= LayoutInflater.from(getContext()).inflate(R.layout.layout_activate_session_passcode,null);
        alertActivateSession.setView(dialogView);
        alertActivateSession.setCancelable(false);
        alertActivateSession.show();
        DialogManager.INSTANCE.register(alertActivateSession);
        code1 =  dialogView.findViewById(R.id.code1);
        code2 =  dialogView.findViewById(R.id.code2);
        code3 =  dialogView.findViewById(R.id.code3);
        code4 =  dialogView.findViewById(R.id.code4);
        code5 =  dialogView.findViewById(R.id.code5);
        code6 =  dialogView.findViewById(R.id.code6);


        code1.requestFocus();
        addTextWatcher(code1);
        addTextWatcher(code2);
        addTextWatcher(code3);
        addTextWatcher(code4);
        addTextWatcher(code5);
        addTextWatcher(code6);

        ImageView imgclose= dialogView.findViewById(R.id.icon_close);
        imgclose.setOnClickListener(view -> {
            if( alertActivateSession.isShowing()){
                alertActivateSession.dismiss();
            }
        });

        next = dialogView.findViewById(R.id.buttonNext);

        next.setOnClickListener(v -> {
            enteredPassCode = code1.getText().toString() + code2.getText().toString() +
                    code3.getText().toString() + code4.getText().toString() +
                    code5.getText().toString() + code6.getText().toString();
            if (enteredPassCode.length() == 6) {
                alertActivateSession.dismiss();
                validateBuildingPassCode(enteredPassCode);

            } else {
                Toast.makeText(Globals.getInstance().getApplicationContext(), "Please check the Building Passcode", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validateBuildingPassCode(String enteredPassCode) {
        ResponseCallback responseCallBack = new ResponseCallback() {
            @Override
            public void onSuccessResponse(JSONObject response) throws JSONException {
                ProgressDialogUtils.hideProgressDialog();
                if (response.getString("valid").equals("true")) {
                    String ccuId = CCUHsApi.getInstance().getCcuId();
                    retriveCCUDetails(ccuId);
                } else {
                    Toast.makeText(Globals.getInstance().getApplicationContext(), getString(R.string.error_msg), Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onErrorResponse(JSONObject response) throws JSONException {
                ProgressDialogUtils.hideProgressDialog();
                String errResponse = "No address associated with hostname";
                if ((response.get("response").toString()).contains(errResponse)) {
                    Toast.makeText(Globals.getInstance().getApplicationContext(), getString(R.string.no_internet),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(Globals.getInstance().getApplicationContext(), getString(R.string.error_msg),
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        ProgressDialogUtils.showProgressDialog(getActivity(), getString(R.string.passcode_validation_msg));
        new OtpManager().validateOTP(enteredPassCode, responseCallBack);
    }


    private void retriveCCUDetails(String ccu) {
        AlertDialog alertInProgress = new AlertDialog.Builder(getActivity()).create();
        View dialogView= LayoutInflater.from(getContext()).inflate(R.layout.layout_session_inprogress,null);
        alertInProgress.setView(dialogView);
        alertInProgress.setCancelable(false);
        DialogManager.INSTANCE.register(alertInProgress);
        alertInProgress.show();
        ResponseCallback responseCallBack = new ResponseCallback() {
            @Override
            public void onSuccessResponse(JSONObject response) throws JSONException {
                alertInProgress.dismiss();
                showSuccessDailog();
            }

            @Override
            public void onErrorResponse(JSONObject response) throws JSONException {
                Toast.makeText(Globals.getInstance().getApplicationContext(),R.string.token_error_msg,Toast.LENGTH_LONG).show();
            }
        };
        new OtpManager().postBearerToken(ccu, enteredPassCode, responseCallBack);
    }

    private void showSuccessDailog() {
        AlertDialog alertSuccess = new AlertDialog.Builder(getActivity()).create();
        View dialogView= LayoutInflater.from(getContext()).inflate(R.layout.layout_session_success,null);
        Button okay = dialogView.findViewById(R.id.Okay);
        okay.setOnClickListener(v -> alertSuccess.dismiss());
        alertSuccess.setView(dialogView);
        alertSuccess.setCancelable(false);
        DialogManager.INSTANCE.register(alertSuccess);
        alertSuccess.show();

    }


    private void addTextWatcher(final EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                 //Nothing to do
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Nothing to do
            }

            @SuppressLint("NonConstantResourceId")
            @Override
            public void afterTextChanged(Editable s) {
                switch (editText.getId()) {
                    case R.id.code1:
                        if (editText.length() == 1) {
                            code2.requestFocus();
                        }
                        break;
                    case R.id.code2:
                        if (editText.length() == 1) {
                            code3.requestFocus();
                        } else if (editText.length() == 0) {
                            code1.requestFocus();
                        }
                        break;
                    case R.id.code3:
                        if (editText.length() == 1) {
                            code4.requestFocus();
                        } else if (editText.length() == 0) {
                            code2.requestFocus();
                        }
                        break;
                    case R.id.code4:
                        if (editText.length() == 1) {
                            code5.requestFocus();
                        } else if (editText.length() == 0) {
                            code3.requestFocus();
                        }
                        break;
                    case R.id.code5:
                        if (editText.length() == 1) {
                            code6.requestFocus();
                        } else if (editText.length() == 0) {
                            code4.requestFocus();
                        }
                        break;
                    case R.id.code6:
                        if (editText.length() == 1) {
                           next.setEnabled(true);
                        } else if (editText.length() == 0) {
                            code5.requestFocus();
                            next.setEnabled(false);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + editText.getId());
                }
            }
        });
    }

    private List<String> getSortedList(Map<String, String> hashmaps, String currentAppVersion) {
        List<String> stringList = new ArrayList<>();
        for (Map.Entry<String, String> entry : hashmaps.entrySet()) {
            if(!isCurrentVersionHigherOrEqualToRequired(currentAppVersion, entry.getKey())) {
                stringList.add(entry.getKey());
            }
        }
        stringList.sort(new VersionComparator());
        List<String> tempHash = new ArrayList<>();
        for (String version : stringList) {
            for (Map.Entry<String, String> entry : hashmaps.entrySet()) {
                if (entry.getKey().equals(version)) {
                    tempHash.add(entry.getKey() + " " + entry.getValue());
                }
            }
        }
        return tempHash;
    }

    @Override
    public void onBundleInstallMessage(@NonNull BundleInstallManager.BundleInstallState installState,
                                       int percentComplete, @NonNull String message) {
        CcuLog.d("CCU_BUNDLE", "Message: "+message + " percComp " + percentComplete +
                " installState " + installState);
        if(percentComplete == 0 && installState == installState.DOWNLOAD_PAUSED){
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            progressBar.setProgressCompat(percentComplete, true);
            String perc = percentComplete + "%";
            downloadingText.setText(message);
            downloadPerc.setText(perc);
            connectivityIssues.setVisibility(View.GONE);
            if (installState == installState.INSTALLING) {
                PreferenceUtil.stopUpdateCCUInAboutScreen();
                PreferenceUtil.setIntPreference("percComplete", percentComplete);
                installNewApk();
            } else if(installState == installState.COMPLETED){
                softwareIsUpToDate();
            } else if(installState == installState.DOWNLOAD_PAUSED){
                connectivityIssues.setVisibility(View.VISIBLE);
            } else if(installState == installState.DOWNLOAD_FAILED || installState == installState.FAILED){
                handleUpgradeBundleErrors(Arrays.asList(message));
                PreferenceUtil.stopUpdateCCUInAboutScreen();
                PreferenceUtil.installationCompletedInAboutScreen();
            }
        });
    }

    static class VersionComparator implements Comparator<String> {
        @Override
        public int compare(String version1, String version2) {
            String[] parts1 = version1.split("\\.");
            String[] parts2 = version2.split("\\.");

            int maxLength = Math.max(parts1.length, parts2.length);

            for (int i = 0; i < maxLength; i++) {
                int part1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
                int part2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

                if (part1 != part2) {
                    return part2 - part1;
                }
            }
            return 0;
        }
    }

    @Override
    public void onResume() {
        getRecommendedCCUDataFromPreference();
        BundleInstallManager.Companion.getInstance().addBundleInstallListener(this);
        RxjavaUtil.executeBackground(() -> {
            BundleInstallManager bundleInstallManager = BundleInstallManager.Companion.getInstance();
            UpgradeBundle upgradeBundle = bundleInstallManager.getRecommendedUpgradeBundle(false);
            if(upgradeBundle != null) {
                updateToUi(upgradeBundle, bundleInstallManager);
            }
        });
        CcuLog.i("AboutFragment", "onResume");
        super.onResume();
    }

    private void updateToUi(UpgradeBundle upgradeBundle, BundleInstallManager bundleInstallManager) {
        postToMainThread(() -> setupUpdateCCUButton(upgradeBundle, bundleInstallManager));
    }
}

