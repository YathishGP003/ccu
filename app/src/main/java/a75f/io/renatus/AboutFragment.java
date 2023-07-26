package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
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
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.constants.HttpConstants;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.cloud.OtpManager;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.logic.cloud.ResponseCallback;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.renatus.ENGG.AppInstaller;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.util.remotecommand.RemoteCommandHandlerUtil;
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
    @BindView(R.id.validOTP)
    TextView validOTP;
    @BindView(R.id.otpCountDown)
    TextView otpCountDown;
    @BindView(R.id.tvSiteId)
    TextView tvSiteId;
    @BindView(R.id.download_size_about)
    TextView downloadSize;
    @BindView(R.id.download_size_abou)
    TextView downloadSizeText;
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
    @BindView(R.id.downloaded_size)
    TextView totalDownloadedSize;
    @BindView(R.id.file_size)
    TextView totalFileSize;
    @BindView(R.id.downloading_text)
    TextView downloadingText;
    String versionLabelString;
    String fileSize;
    Long downloadTd;
    @BindView((R.id.imageViewCancel))
    ImageView cancel;
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
    boolean isNotFirstInvocation ;


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
        setOTPOnAboutPage();

        checkIsCCUHasRecommendedVersion(getActivity());
        updateCCU.setOnClickListener(updateOnClickListener);
        cancel.setOnClickListener(cancelOnClickListener);

       return rootView;
    }

    public void checkIsCCUHasRecommendedVersion(FragmentActivity activity) {
        RxjavaUtil.executeBackgroundTaskWithDisposable(()->{
                    ProgressDialogUtils.showProgressDialog(activity,"Checking for recommended version");
                },
                ()->{String response = getRecommendedCCUVersion();
                    updateAboutFragmentUI(response);
                },
                ProgressDialogUtils::hideProgressDialog);
    }

    View.OnClickListener updateOnClickListener = view -> {
        progressBar.setVisibility(View.VISIBLE);
        cancel.setVisibility(View.VISIBLE);
        updateCCU.setEnabled(false);
        updateAppText.setVisibility(View.GONE);
        linearLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        connectivityIssues.setVisibility(View.GONE);
        cancel.setEnabled(true);
        cancel.setVisibility(View.VISIBLE);
        RemoteCommandHandlerUtil.updateCCU(versionLabelString, AboutFragment.this);
    };

    public void cancelUpdateCCU(){
        new Handler(Looper.getMainLooper()).post(() -> {
            connectivityIssues.setVisibility(View.GONE);
            updateCCU.setEnabled(true);
            updateAppText.setVisibility(View.VISIBLE);
            linearLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            cancel.setEnabled(false);
            cancel.setVisibility(View.GONE);
        });
        Globals.getInstance().setCcuUpdateTriggerTimeToken(0);
        isNotFirstInvocation = false;
    }
    View.OnClickListener cancelOnClickListener = view -> {
        connectivityIssues.setVisibility(View.GONE);
        DownloadManager manager =
                (DownloadManager) RenatusApp.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if(downloadTd != null) {
            manager.remove(downloadTd);
            updateCCU.setEnabled(true);
            updateAppText.setVisibility(View.VISIBLE);
            linearLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            cancel.setEnabled(false);
            cancel.setVisibility(View.GONE);
            Globals.getInstance().setCcuUpdateTriggerTimeToken(0);
            isNotFirstInvocation = false;
        }
    };

    private void updateAboutFragmentUI(String response) {
        if (response != null) {
            try {
                JSONArray array = new JSONArray(response);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject jsonObject = array.getJSONObject(i);
                    if (jsonObject.getBoolean("recommended")) {
                        String majorVersion = jsonObject.getString("majorVersion");
                        String minorVersion = jsonObject.getString("minorVersion");
                        String patchVersion = jsonObject.getString("patchVersion");
                        versionLabelString = jsonObject.getString("versionLabel");
                        fileSize = jsonObject.getString("size");
                        isNotFirstInvocation = false;
                        String size = jsonObject.get("size") +" MB";
                        String recommendedVersion = majorVersion+"."+minorVersion+"."+patchVersion;
                        String currentAppVersion =  CCUUiUtil.getCurrentCCUVersion();

                        if (getActivity() != null) {
                            new Handler(Looper.getMainLooper()).post(() ->  {
                                if (CCUUiUtil.isCCUNeedsToBeUpdated(currentAppVersion, recommendedVersion)){
                                    cancel.setEnabled(true);
                                    updateScreenLayout.setVisibility(View.VISIBLE);
                                    latestVersion.setText(recommendedVersion);
                                    downloadSize.setText(size);
                                    totalFileSize.setText(size);
                                }else {
                                    downloadSizeText.setVisibility(View.GONE);
                                    updateStatus.setText("CCU is up to date");
                                    updateAppText.setVisibility(View.GONE);
                                    updateCCU.setVisibility(View.GONE);
                                    verSionText.setVisibility(View.VISIBLE);
                                    latest_version_text.setVisibility(View.GONE);
                                    latestVersion.setText(currentAppVersion);
                                }
                            });
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            if (getActivity() != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    connectionUpLayout.setVisibility(View.GONE);
                    connectionDownLayout.setVisibility(View.VISIBLE);
                });
            }
        }
    }

    private String getRecommendedCCUVersion(){
        String response = HttpUtil.executeJson(
                RenatusServicesEnvironment.getInstance().getUrls().getRecommendedCCUVersion(),
                null,
                a75f.io.api.haystack.BuildConfig.HAYSTACK_API_KEY,
                true,
                HttpConstants.HTTP_METHOD_GET
        );
        CcuLog.i(L.TAG_CCU_UPDATE,"Response of recommended CCU version API "+response);
        return response;
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

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.activateSession)
    public void activateSession(){
        AlertDialog alertActivateSession = new AlertDialog.Builder(getActivity()).create();
        View dialogView= LayoutInflater.from(getContext()).inflate(R.layout.layout_activate_session_passcode,null);
        alertActivateSession.setView(dialogView);
        alertActivateSession.setCancelable(false);
        alertActivateSession.show();

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

    public void setProgress(int value, long downloadId, int columnIndex) {
        CcuLog.i("CCU_DOWNLOAD", "progress " + value);
        this.downloadTd = downloadId;
        DecimalFormat df = new DecimalFormat("#.##");
        double downloadSize = Double.parseDouble(df.format(value * .01 * Double.parseDouble(
                fileSize)));
        String downloadedSize = downloadSize + " MB/";
        if (getActivity() != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                    totalDownloadedSize.setText((downloadedSize));
                    progressBar.setProgressCompat(value, true);
                    connectivityIssues.setVisibility(View.GONE);
                    if (columnIndex == 4 || columnIndex == 1 && isNotFirstInvocation) {
                        connectivityIssues.setVisibility(View.VISIBLE);
                    }
                    isNotFirstInvocation = true;
                    if (value == 100) {
                        totalDownloadedSize.setVisibility(View.INVISIBLE);
                        totalFileSize.setVisibility(View.INVISIBLE);
                        downloadingText.setText("Installing..");
                        progressBar.setVisibility(View.GONE);
                        cancel.setVisibility(View.GONE);
                    }
            });
        }
    }
    }

