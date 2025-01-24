package a75f.io.renatus.registration;

import static android.content.Context.DOWNLOAD_SERVICE;
import android.app.DownloadManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONException;
import org.json.JSONObject;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.constants.HttpConstants;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.ccu.restore.CCU;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.ENGG.AppInstaller;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.util.remotecommand.RemoteCommandHandlerUtil;
import a75f.io.renatus.util.remotecommand.bundle.BundleInstallListener;
import a75f.io.renatus.util.remotecommand.bundle.BundleInstallManager;
import a75f.io.renatus.util.remotecommand.bundle.models.ArtifactDTO;
import a75f.io.renatus.util.remotecommand.bundle.models.UpgradeBundle;
import a75f.io.util.ExecutorTask;
import butterknife.BindView;

public class UpdateCCUFragment extends DialogFragment implements BundleInstallListener {

    private String currentVersionOfCCUString;
    private String recommendedVersionOfCCUString;
    private String fileSizeString;
    private String versionLabelString;
    private boolean isReplace;
    private boolean isBundleUpdate;
    boolean isServerDown;
    private String replacingCCUName;
    TextView currentVersionOfCCU;
    TextView updateAppReplace;
    TextView header;
    TextView ccuName;
    TextView recommendedVersionOfCCU;
    TextView latestVersionText;
    TextView fileSize;
    TextView cancel;
    TextView updateApp;
    TextView downloadedSizeText;
    TextView totalSize;
    TextView downloadingText;
    LinearProgressIndicator progressBar;
    LinearLayout linearLayout;
    LinearLayout progressLayout;
    Long downloadTd;
    LinearLayout cancel_update;
    LinearLayout update_details;
    LinearLayout update_details_parent;
    LinearLayout fragment_layout;
    LinearLayout server_down_layout;
    LinearLayout connectivityIssues;
    TextView cancelServerDown;
    boolean isNotFirstInvocation ;
    JSONObject config;
    boolean resumeUpdateCCU;
    boolean isDownloading;
    boolean isInstalling;
    UpgradeBundle bundle;
    TextView downloadPerc;
    TextView downloadSizeText;
    LinearLayout error_message_layout;
    LinearLayout error_image_layout;
    private View toastLayout;

    public UpdateCCUFragment showServerDownLayout(Boolean isServerDown) {
        this.isServerDown = isServerDown;
        return this;
    }
    public UpdateCCUFragment() {
    }
    public UpdateCCUFragment resumeDownloadProcess(boolean isDownloading, boolean isInstalling, boolean isReplace) throws JSONException {
        String updateNewCCUPreference;
        if(isReplace){
            updateNewCCUPreference = PreferenceUtil.getStringPreference("UPDATE_CCU_REPLACE_CONFIGURATION");
        }else {
            updateNewCCUPreference = PreferenceUtil.getStringPreference("UPDATE_CCU_NEW_CONFIGURATION");
        }
        config = new JSONObject(updateNewCCUPreference);
        this.currentVersionOfCCUString = config.getString("currentVersionOfCCUString");
        this.recommendedVersionOfCCUString =  config.getString("recommendedVersionOfCCUString");
        this.fileSizeString = config.getString("fileSizeString");
        this.versionLabelString = config.getString("versionLabelString");
        this.isReplace = isReplace;
        this.replacingCCUName = null;
        this.isServerDown = false;
        this.resumeUpdateCCU = true;
        this.isNotFirstInvocation = false;
        if(isReplace) {
            this.replacingCCUName = config.getString("replacingCCUName");
        }
        this.isDownloading = isDownloading;
        this.isInstalling = isInstalling;
        return this;
    }

    public UpdateCCUFragment showUiForFreshAndAddSite(String currentVersionOfCCU, UpgradeBundle upgradeBundle,
                             String fileSize) throws JSONException {
        this.currentVersionOfCCUString = currentVersionOfCCU;
        this.recommendedVersionOfCCUString = upgradeBundle.getBundle().getBundleName();
        this.fileSizeString = fileSize + " MB";
        this.isReplace = false;
        this.replacingCCUName = null;
        this.isServerDown = false;
        this.isNotFirstInvocation = false;
        this.resumeUpdateCCU = false;
        isBundleUpdate = true;
        this.bundle = upgradeBundle;
        config = new JSONObject();
        config.put("currentVersionOfCCUString", currentVersionOfCCU);
        config.put("recommendedVersionOfCCUString", recommendedVersionOfCCU);
        config.put("fileSizeString", fileSize +" MB");
        PreferenceUtil.setStringPreference("UPDATE_CCU_NEW_CONFIGURATION",config.toString());
        return this;
    }
    public UpdateCCUFragment showUiForReplaceCCU(String currentVersionOfCCU, CCU ccu, String fileSize, boolean isReplace) throws JSONException {
        this.currentVersionOfCCUString = currentVersionOfCCU;
        this.fileSizeString = fileSize + " MB";
        this.recommendedVersionOfCCUString = ccu.getVersion();
        this.isReplace = isReplace;
        this.isServerDown = false;
        this.replacingCCUName = ccu.getName();
        this.versionLabelString = getReplaceCCUApk(recommendedVersionOfCCUString);
        this.isNotFirstInvocation = false;
        this.resumeUpdateCCU = false;
        config = new JSONObject();
        config.put("currentVersionOfCCUString", currentVersionOfCCU);
        config.put("fileSizeString", fileSize +" MB");
        config.put("recommendedVersionOfCCUString", ccu.getVersion());
        config.put("versionLabelString", getReplaceCCUApk(ccu.getName()));
        config.put("replacingCCUName", ccu.getName());

        PreferenceUtil.setStringPreference("UPDATE_CCU_REPLACE_CONFIGURATION",config.toString());
        return this;
    }

    public UpdateCCUFragment showBundleUpdateScreenFromReplaceCCU(boolean isReplace, boolean isBundleUpdate, String bundleName,
                             String fileSize, String currentAppVersionWithPatch, UpgradeBundle bundle
                                                                  ) throws JSONException {
        this.currentVersionOfCCUString = currentAppVersionWithPatch;
        this.recommendedVersionOfCCUString = bundleName;
        this.replacingCCUName = bundleName;
        this.isReplace = isReplace;
        this.isBundleUpdate = isBundleUpdate;
        this.isServerDown = false;
        this.isNotFirstInvocation = false;
        this.resumeUpdateCCU = false;
        this.fileSizeString = fileSize + " MB";
        this.bundle = bundle;
        config = new JSONObject();
        config.put("recommendedVersionOfCCUString", bundleName);
        PreferenceUtil.setStringPreference("UPDATE_CCU_REPLACE_CONFIGURATION",config.toString());
        return this;
    }
    public static void abortCCUDownloadProcess() {
        CcuLog.i(L.TAG_CCU_BUNDLE, "Aborting CCU download and installation process in Utility class");
        UpdateCCUFragment.stopAllDownloads();
        PreferenceUtil.stopUpdateCCU();
        PreferenceUtil.installationCompleted();
        PreferenceUtil.stopUpdateCCUInAboutScreen();
        PreferenceUtil.installationCompletedInAboutScreen();
    }
    private static void stopAllDownloads(){
        List<Long> downloadIds = getAllDownloadIds();
        DownloadManager downloadManager = (DownloadManager) Globals.getInstance().getApplicationContext()
                .getSystemService(DOWNLOAD_SERVICE);
        for (long downloadId : downloadIds) {
            downloadManager.remove(downloadId);
        }
    }

    private static List<Long> getAllDownloadIds() {
        List<Long> downloadIds = new ArrayList<>();
        DownloadManager downloadManager = (DownloadManager) Globals.getInstance().getApplicationContext()
                .getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterByStatus(DownloadManager.STATUS_RUNNING | DownloadManager.STATUS_PENDING);
        Cursor cursor = downloadManager.query(query);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long downloadId = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
                downloadIds.add(downloadId);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return downloadIds;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_update_c_c_u, null);
        toastLayout = inflater.inflate(R.layout.custom_layout_ccu_successful_update, view.findViewById(R.id.custom_toast_layout_update_ccu));

        if (isServerDown) {
            fragment_layout = view.findViewById(R.id.update_ccu_fragment_layout);
            server_down_layout = view.findViewById(R.id.server_down_layout);
            cancelServerDown = view.findViewById(R.id.cancel_server_down);
            fragment_layout.setVisibility(View.GONE);
            server_down_layout.setBackgroundResource(R.color.lite_pink);
            server_down_layout.setVisibility(View.VISIBLE);
            cancelServerDown.setOnClickListener(cancelInServerDownLayout);
        } else {
            updateDialog(view);
            updateUI();
            cancel.setOnClickListener(cancelOnClickListener);
            updateApp.setOnClickListener(updateOnClickListener);
            if(resumeUpdateCCU) {
                if(isDownloading) {
                    resumeProgressBar();
                    showProgressBar();
                } else if(isInstalling){
                    installNewApk();
                }
            }
        }
        setCancelable(false);
        return new AlertDialog.Builder(requireActivity(), R.style.NewDialogStyle)
                .setView(view)
                .create();
    }

    View.OnClickListener cancelOnClickListener = view -> {
        PreferenceUtil.stopUpdateCCU();
        getParentFragmentManager().popBackStack();
        connectivityIssues.setVisibility(View.GONE);
        if (isBundleUpdate) {
            RxjavaUtil.executeBackgroundWithDisposable(() -> {
                BundleInstallManager bundleInstallManager = BundleInstallManager.Companion.getInstance();
                bundleInstallManager.cancelBundleInstallation();
            });

        } else {
            DownloadManager manager =
                    (DownloadManager) RenatusApp.getAppContext().getSystemService(DOWNLOAD_SERVICE);
            if (downloadTd != null) {
                manager.remove(downloadTd);
                Globals.getInstance().setCcuUpdateTriggerTimeToken(0);
            }
        }
        dismiss();
        if (!isReplace) {
            if(requireActivity() instanceof FreshRegistration){
                ((FreshRegistration) requireActivity()).selectItem(2);
            }
        }
        if (resumeUpdateCCU){
            Intent i = new Intent(getActivity(), FreshRegistration.class);
            i.putExtra("viewpager_position", 8);
            startActivity(i);
            getActivity().finish();
        }
    };

    View.OnClickListener cancelInServerDownLayout = view -> {
        if(requireActivity() instanceof FreshRegistration){
            ((FreshRegistration) requireActivity()).selectItem(2);
        }
        dismiss();
    };
    View.OnClickListener updateOnClickListener = view -> {
        BundleInstallManager bundleInstallManager = BundleInstallManager.Companion.getInstance();
        PreferenceUtil.startUpdateCCU();
        showProgressBar();
        if (isBundleUpdate) {
            RxjavaUtil.executeBackgroundWithDisposable(() -> bundleInstallManager.initiateBundleUpgrade(bundle, this));        } else {
            if (versionLabelString != null) {
                RemoteCommandHandlerUtil.updateCCU(versionLabelString, UpdateCCUFragment.this, getActivity());
            }
        }
    };
    @Override
    public void onPause() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.remove(this);
        transaction.commit();
        super.onPause();
    }
    private void showProgressBar() {
        connectivityIssues.setVisibility(View.GONE);
        updateApp.setEnabled(false);
        progressLayout.setVisibility(View.VISIBLE);
        updateApp.setTextColor(Color.parseColor("#CCCCCC"));
    }
    public void downloadCanceled() {
        PreferenceUtil.stopUpdateCCU();
        isNotFirstInvocation = false;
        if (getActivity() != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                updateApp.setEnabled(true);
                progressLayout.setVisibility(View.GONE);
                updateApp.setTextColor(Color.parseColor("#E24301"));
            });
        }
        Globals.getInstance().setCcuUpdateTriggerTimeToken(0);
    }

    private void updateUI() {
        currentVersionOfCCU.setText(currentVersionOfCCUString);
        recommendedVersionOfCCU.setText(recommendedVersionOfCCUString);
        fileSize.setText(fileSizeString);
    }

    private void updateDialog(View view) {
        header = view.findViewById(R.id.header);
        ccuName = view.findViewById(R.id.ccu_name);
        currentVersionOfCCU = view.findViewById(R.id.currentVersion);
        recommendedVersionOfCCU = view.findViewById(R.id.latestVersion);
        latestVersionText = view.findViewById(R.id.latestVersionText);
        fileSize = view.findViewById(R.id.downloadSize);
        cancel = view.findViewById(R.id.cancel);
        updateApp = view.findViewById(R.id.updateApp);
        progressBar = view.findViewById(R.id.progress_bar);
        downloadedSizeText = view.findViewById(R.id.downloaded_size);
        totalSize = view.findViewById(R.id.file_size);
        downloadingText = view.findViewById(R.id.downloading_text);
        totalSize.setText(fileSizeString);
        progressBar.setIndeterminate(false);
        linearLayout = view.findViewById(R.id.layoutId);
        progressLayout = view.findViewById(R.id.progress_layout);
        cancel_update = view.findViewById(R.id.cancel_update);
        update_details = view.findViewById(R.id.update_details);
        update_details_parent = view.findViewById(R.id.update_details_parent);
        fragment_layout = view.findViewById(R.id.update_ccu_fragment_layout);
        server_down_layout = view.findViewById(R.id.server_down_layout);
        connectivityIssues = view.findViewById(R.id.connectivityIssues);
        currentVersionOfCCU.setText(currentVersionOfCCUString);
        recommendedVersionOfCCU.setText(recommendedVersionOfCCUString);
        fileSize.setText(fileSizeString);
        downloadPerc = view.findViewById(R.id.download_perc);
        downloadSizeText = view.findViewById(R.id.downloadSizeText);
        error_message_layout = view.findViewById(R.id.error_message_layout);
        error_image_layout = view.findViewById(R.id.error_image_layout);
        if (isReplace & !isBundleUpdate) {
            downloadSizeText.setVisibility(View.VISIBLE);
            header.setText("Do you want to replace");
            latestVersionText.setText("Replacing CCU's version:");
            updateApp.setText("UPDATE & CONTINUE");
        } else if (isReplace && isBundleUpdate) {
            totalSize.setVisibility(View.GONE);
            fileSize.setVisibility(View.GONE);
            fileSize.setText(fileSizeString);
            header.setText("Please update the software to continue...");
            latestVersionText.setText("Replacing CCU’s Software:");
            updateApp.setText("UPDATE NOW");
        } else if(isBundleUpdate) {
            totalSize.setVisibility(View.GONE);
            fileSize.setVisibility(View.GONE);
            fileSize.setText(fileSizeString);
            header.setText("Please update the software to continue...");
        }
    }

    public void setProgress(int value, long downloadId, int columnIndex){
        this.downloadTd = downloadId;
        CcuLog.i(L.TAG_CCU_DOWNLOAD, "progress " + value);
        DecimalFormat df = new DecimalFormat("#.##");
        double downloadSize = Double.parseDouble(df.format(value * .01 * Double.parseDouble(
                (fileSize.getText().toString().split(" MB")[0]))));
        String downloadedSize = downloadSize + " MB/";
        if (getActivity() != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                    downloadedSizeText.setText((downloadedSize));
                    progressBar.setProgressCompat(value, true);
                    connectivityIssues.setVisibility(View.GONE);
                    if (columnIndex == 4 || columnIndex == 1 && isNotFirstInvocation) {
                        connectivityIssues.setVisibility(View.VISIBLE);
                    }
                    isNotFirstInvocation = true;
                    if (value == 100) {
                        PreferenceUtil.stopUpdateCCU();
                        PreferenceUtil.installCCU();
                        installNewApk();
                    }
            });
        }
    }


    private void installNewApk() {
        downloadingText.setText("Installing..");
        downloadedSizeText.setVisibility(View.INVISIBLE);
        totalSize.setVisibility(View.INVISIBLE);
        cancel_update.setVisibility(View.GONE);
    }

    public void showCCUUpdateToast(View toastLayout, Context context) {
        if(context != null && toastLayout != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast toast = new Toast(context);
                toast.setGravity(Gravity.BOTTOM, 50, 50);
                toast.setView(toastLayout);
                toast.setDuration(Toast.LENGTH_LONG);
                TextView textView = toast.getView().findViewById(R.id.custom_toast_message_detail);
                textView.setText("CCU bundle successfully updated");
                toast.show();
            });
        }
    }
    public String getRecommendedCCUVersion(){
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
    public Boolean isCCUHasRecommendedVersion(FragmentManager parentFragmentManager, UpgradeBundle upgradeBundle){
        FragmentTransaction ft = parentFragmentManager.beginTransaction();
        Fragment fragmentByTag = parentFragmentManager.findFragmentByTag("popup");
        if (fragmentByTag != null) {
            ft.remove(fragmentByTag);
        }
        if (upgradeBundle == null) {
            UpdateCCUFragment updateCCUFragment = new UpdateCCUFragment().showServerDownLayout(true);
            updateCCUFragment.show(ft, "popup");
            return false;
        } else if (upgradeBundle.getUpgradeOkay() && upgradeBundle.getComponentsToUpgrade().size() == 0){
            CcuLog.i(L.TAG_CCU_UPDATE,"CCU has recommended Version");
            return true;
        }
        double size = 0;
        for (ArtifactDTO artifactDTO : upgradeBundle.getComponentsToUpgrade()) {
            size += Double.parseDouble(artifactDTO.getFileSize());
            CcuLog.d(L.TAG_CCU_BUNDLE, "Upgradable ArtifactDTO name "+artifactDTO.getFileName() + " version "+artifactDTO.getVersion() + " size "+artifactDTO.getFileSize());
        }
        String currentAppVersionWithPatch = getCurrentAppVersionWithPatch();
        try {
            UpdateCCUFragment updateCCUFragment = new UpdateCCUFragment().showUiForFreshAndAddSite(currentAppVersionWithPatch,
                    upgradeBundle, String.valueOf(size));
            updateCCUFragment.show(ft, "popup");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;

    }
    public void checkIsCCUHasRecommendedVersion(FragmentActivity activity, FragmentManager parentFragmentManager,
                                                       View toastLayout, Context context, FragmentActivity fragmentActivity) {
        ExecutorTask.executeAsync(()-> ProgressDialogUtils.showProgressDialog(activity,"Checking for recommended version"),
                ()->{
                    BundleInstallManager bundleInstallManager = BundleInstallManager.Companion.getInstance();
                    UpgradeBundle upgradeBundle = bundleInstallManager.getRecommendedUpgradeBundle(true);
                    boolean isCCCUHasRecommendedVersion = isCCUHasRecommendedVersion(parentFragmentManager, upgradeBundle);
                    if(isCCCUHasRecommendedVersion && upgradeBundle != null){
                        showCCUUpdateToast(toastLayout, context);
                    }
                },
                ProgressDialogUtils::hideProgressDialog);
    }
    public String getRecommendedVersionOfCCUWithPatch(JSONObject jsonObject) throws JSONException {
        String majorVersion = jsonObject.getString("majorVersion");
        String minorVersion = jsonObject.getString("minorVersion");
        String patchVersion = jsonObject.getString("patchVersion");
        return majorVersion +"."+ minorVersion+"."+patchVersion;
    }
    public boolean isCurrentVersionMatchesWithRecommended(String currentAppVersionWithPatch,
                                                                 String recommendedVersionOfCCUWithPatch) {
        String[] currentVersionComponents = currentAppVersionWithPatch.split("\\.");
        String[] recommendedVersionComponents = recommendedVersionOfCCUWithPatch.split("\\.");
        return Arrays.equals(currentVersionComponents, recommendedVersionComponents);
    }
    public String getCurrentAppVersionWithPatch() {
        return BuildConfig.VERSION_NAME.split(BuildConfig.BUILD_TYPE +"_")[1];
    }
    private String getReplaceCCUApk(String version) {
        return BuildConfig.VERSION_NAME.split("\\d+")[0]+version+".apk";
    }
    public void resumeProgressBar(){
        List<Long> downloadIds = UpdateCCUFragment.getAllDownloadIds();
        DownloadManager downloadManager = (DownloadManager) Globals.getInstance().getApplicationContext()
                .getSystemService(DOWNLOAD_SERVICE);
        AppInstaller appInstaller = new AppInstaller();
        if(!downloadIds.isEmpty()) {
            appInstaller.checkDownload(downloadIds.get(0), downloadManager, UpdateCCUFragment.this, null);
        }
    }

    @Override
    public void onBundleInstallMessage(@NonNull BundleInstallManager.BundleInstallState installState, int percentComplete, @NonNull String message) {
        CcuLog.i(L.TAG_CCU_UPDATE, "Bundle install state: " + installState + " message: " + message + " percentComplete: "+percentComplete);
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
                PreferenceUtil.stopUpdateCCU();
                installNewApk();
            } else if(installState == installState.COMPLETED){
                cancelUpdateProcess(BundleInstallManager.Companion.getInstance());
                dismiss();
                showCCUUpdateToast(toastLayout, getContext());
            } else if(installState == installState.DOWNLOAD_PAUSED){
                connectivityIssues.setVisibility(View.VISIBLE);
            } else if(installState == installState.DOWNLOAD_FAILED | installState == installState.FAILED){
                handleUpgradeBundleErrors(Arrays.asList(message));
            }
        });

    }

    private void cancelUpdateProcess(BundleInstallManager bundleInstallManager) {
        progressLayout.setVisibility(View.GONE);
        cancel_update.setVisibility(View.VISIBLE);
        downloadPerc.setVisibility(View.GONE);
        downloadingText.setVisibility(View.GONE);
        bundleInstallManager.cancelBundleInstallation();
        PreferenceUtil.stopUpdateCCU();
        PreferenceUtil.installationCompleted();
    }
    private void handleUpgradeBundleErrors(List<String> upgradeBundleErrorMessages) {
        downloadPerc.setVisibility(View.GONE);
        downloadingText.setVisibility(View.GONE);
        progressLayout.setVisibility(View.GONE);
        error_message_layout.setVisibility(View.VISIBLE);
        error_image_layout.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.VISIBLE);
        PreferenceUtil.stopUpdateCCU();
        PreferenceUtil.installationCompleted();

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

        CcuLog.d(L.TAG_CCU_UI, errorMessages.toString());
    }
}