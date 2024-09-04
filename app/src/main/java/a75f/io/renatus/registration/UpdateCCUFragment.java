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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import org.json.JSONArray;
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
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.util.remotecommand.RemoteCommandHandlerUtil;
import a75f.io.util.ExecutorTask;

public class UpdateCCUFragment extends DialogFragment {

    private String currentVersionOfCCUString;
    private String recommendedVersionOfCCUString;
    private String fileSizeString;
    private String versionLabelString;
    private boolean isReplace;
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

    public UpdateCCUFragment(Boolean isServerDown) {
        this.isServerDown = isServerDown;
    }
    public UpdateCCUFragment() {
    }
    public UpdateCCUFragment(boolean isDownloading, boolean isInstalling, boolean isReplace) throws JSONException {
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
    }
    public UpdateCCUFragment(String currentVersionOfCCU, String recommendedVersionOfCCU,
                             String fileSize, String versionLabel) throws JSONException {
        this.currentVersionOfCCUString = currentVersionOfCCU;
        this.recommendedVersionOfCCUString = recommendedVersionOfCCU;
        this.fileSizeString = fileSize + " MB";
        this.versionLabelString = versionLabel;
        this.isReplace = false;
        this.replacingCCUName = null;
        this.isServerDown = false;
        this.isNotFirstInvocation = false;
        this.resumeUpdateCCU = false;
        config = new JSONObject();
        config.put("currentVersionOfCCUString", currentVersionOfCCU);
        config.put("recommendedVersionOfCCUString", recommendedVersionOfCCU);
        config.put("fileSizeString", fileSize +" MB");
        config.put("versionLabelString", versionLabel);
        PreferenceUtil.setStringPreference("UPDATE_CCU_NEW_CONFIGURATION",config.toString());
    }
    public UpdateCCUFragment(String currentVersionOfCCU, CCU ccu,
                             String fileSize, boolean isReplace) throws JSONException {
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
    }
    public static void abortCCUDownloadProcess() {
        UpdateCCUFragment.stopAllDownloads();
        PreferenceUtil.stopUpdateCCU();
        PreferenceUtil.installationCompleted();
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
        DownloadManager manager =
                (DownloadManager) RenatusApp.getAppContext().getSystemService(DOWNLOAD_SERVICE);
        if (downloadTd != null) {
            manager.remove(downloadTd);
            Globals.getInstance().setCcuUpdateTriggerTimeToken(0);
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
        PreferenceUtil.startUpdateCCU();
        showProgressBar();
        if (versionLabelString != null) {
            RemoteCommandHandlerUtil.updateCCU(versionLabelString, UpdateCCUFragment.this, getActivity());
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
        updateAppReplace = view.findViewById(R.id.update_app_replace);
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
        if (isReplace) {
            header.setText("Do you want to replace");
            ccuName.setText(replacingCCUName+"?");
            updateAppReplace.setVisibility(View.VISIBLE);
            latestVersionText.setText("Replacing CCU's version:");
            updateApp.setText("UPDATE & CONTINUE");
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) update_details.getLayoutParams();
            layoutParams.width = 660;
            update_details.setLayoutParams(layoutParams);

            FrameLayout.LayoutParams update_detailsParams = (FrameLayout.LayoutParams) fragment_layout.getLayoutParams();
            update_detailsParams.width = 750;
            fragment_layout.setLayoutParams(update_detailsParams);
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
        progressBar.setVisibility(View.GONE);
        cancel_update.setVisibility(View.GONE);
        update_details_parent.setPadding(65,0,15,15);
    }

    public void showCCUUpdateToast(String response, View toastLayout, Context context, FragmentActivity fragmentActivity) {
        if (response != null) {
            try {
                JSONArray array = new JSONArray(response);
                for (int i=0; i < array.length(); i++) {
                    JSONObject jsonObject = array.getJSONObject(i);
                    if (jsonObject.getBoolean("recommended")) {
                        String recommendedVersionOfCCUWithPatch = getRecommendedVersionOfCCUWithPatch(jsonObject);
                        new Handler(Looper.getMainLooper()).post(() -> {
                                Toast toast = new Toast(context);
                                toast.setGravity(Gravity.BOTTOM, 50, 50);
                                toast.setView(toastLayout);
                                toast.setDuration(Toast.LENGTH_LONG);
                                TextView textView = toast.getView().findViewById(R.id.custom_toast_message_detail);
                                if(!isCurrentVersionMatchesWithRecommended(
                                        getCurrentAppVersionWithPatch(),recommendedVersionOfCCUWithPatch)){
                                    textView.setText("CCU successfully updated");
                                }else {
                                    textView.setText("CCU successfully updated to the latest version");
                                }
                                toast.show();

                        });
                    }
                }
            } catch (JSONException e) {
                CcuLog.e(L.TAG_CCU_UPDATE, "Unable to parse JSON to retrieve recommended CCU version " + e.getMessage());
            }
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
    public Boolean isCCUHasRecommendedVersion(FragmentManager parentFragmentManager, String response) {
        FragmentTransaction ft = parentFragmentManager.beginTransaction();
        Fragment fragmentByTag = parentFragmentManager.findFragmentByTag("popup");
        if (fragmentByTag != null) {
            ft.remove(fragmentByTag);
        }
        if (response == null) {
            UpdateCCUFragment updateCCUFragment = new UpdateCCUFragment(true);
            updateCCUFragment.show(ft, "popup");
            return false;
        } else {
            try {
                JSONArray array = new JSONArray(response);
                for (int i=0; i < array.length(); i++) {
                    JSONObject jsonObject = array.getJSONObject(i);
                    if (jsonObject.getBoolean("recommended")) {
                        String versionLabel = jsonObject.getString("versionLabel");
                        String size = jsonObject.get("size").toString();
                        String currentAppVersionWithPatch = getCurrentAppVersionWithPatch();
                        String recommendedVersionOfCCUWithPatch = getRecommendedVersionOfCCUWithPatch(jsonObject);
                        CcuLog.i(L.TAG_CCU_UPDATE,"CCU version "+currentAppVersionWithPatch +
                                " , Recommended Version "+recommendedVersionOfCCUWithPatch);
                        if (CCUUiUtil.isCCUNeedsToBeUpdated(currentAppVersionWithPatch, recommendedVersionOfCCUWithPatch)) {
                            CcuLog.i(L.TAG_CCU_UPDATE,"CCU Not has recommended Version");
                            UpdateCCUFragment updateCCUFragment = new UpdateCCUFragment(currentAppVersionWithPatch,
                                    recommendedVersionOfCCUWithPatch, size, versionLabel);
                            updateCCUFragment.show(ft, "popup");
                            return false;
                        }else {
                            CcuLog.i(L.TAG_CCU_UPDATE,"CCU has recommended Version");
                            return true;
                        }
                    }
                }
            } catch (JSONException e) {
                CcuLog.e(L.TAG_CCU_UPDATE, "Unable to parse JSON to retrieve recommended CCU version " + e.getMessage());
            }
        }
        return true;
    }
    public void checkIsCCUHasRecommendedVersion(FragmentActivity activity, FragmentManager parentFragmentManager,
                                                       View toastLayout, Context context, FragmentActivity fragmentActivity) {
        ExecutorTask.executeAsync(()-> ProgressDialogUtils.showProgressDialog(activity,"Checking for recommended version"),
                ()->{String response = getRecommendedCCUVersion();
                    boolean isCCCUHasRecommendedVersion = isCCUHasRecommendedVersion(parentFragmentManager, response);
                    if(isCCCUHasRecommendedVersion){
                        showCCUUpdateToast(response, toastLayout, context, fragmentActivity);
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
}