package a75f.io.renatus.registration;

import static a75f.io.logic.L.TAG_CCU_REPLACE;
import static a75f.io.logic.util.backupfiles.FileConstants.CCU_REPLACE_BACNET_CONFIG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.RetryCountCallback;
import a75f.io.logic.util.bacnet.BacnetConfigConstants;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.ccu.restore.CCU;
import a75f.io.logic.ccu.restore.EquipResponseCallback;
import a75f.io.logic.ccu.restore.ReplaceCCUTracker;
import a75f.io.logic.ccu.restore.ReplaceStatus;
import a75f.io.logic.ccu.restore.RestoreCCU;
import a75f.io.logic.cloud.FileBackupManager;
import a75f.io.logic.cloud.OtpManager;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.logic.cloud.RenatusServicesUrls;
import a75f.io.logic.cloud.ResponseCallback;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.messaging.client.MessagingClient;
import a75f.io.messaging.handler.DashboardHandlerKt;
import a75f.io.renatus.R;
import a75f.io.renatus.UtilityApplication;
import a75f.io.renatus.util.CCUListAdapter;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.sitesequencer.SequenceManager;
import a75f.io.util.DashboardUtilKt;
import a75f.io.util.ExecutorTask;

public class ReplaceCCU extends Fragment implements CCUSelect {

    private Context mContext;
    private EditText passCode_1;
    private EditText passCode_2;
    private EditText passCode_3;
    private EditText passCode_4;
    private EditText passCode_5;
    private EditText passCode_6;
    private Button next;
    private String enteredPassCode;
    private View toastFail;
    private View toastCcuRestoreSuccess;
    private volatile AtomicBoolean isProcessPaused;
    private CopyOnWriteArrayList<Future<?>> futures;
    private ExecutorService executorService;
    private Handler handler;
    private boolean isReplaceClosed;

    public ReplaceCCU() {
        // Required empty public constructor
        futures = new CopyOnWriteArrayList<>();
        isProcessPaused = new AtomicBoolean(false);
        restoreCCU = new RestoreCCU();
        handler = new Handler();
        isReplaceClosed = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    @Override
    public void onResume() {
        if(PreferenceUtil.getUpdateCCUStatus() || PreferenceUtil.isCCUInstalling()) {
            try {
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                UpdateCCUFragment updateCCUFragment = new UpdateCCUFragment().resumeDownloadProcess(
                        PreferenceUtil.getUpdateCCUStatus(), PreferenceUtil.isCCUInstalling(), true);
                updateCCUFragment.show(ft, "popup");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        super.onResume();
    }
    private AlertDialog progressAlertDialog;
    private ProgressBar progressBar;
    private TextView replaceStatus;
    private TextView pauseStatus;
    private TextView cancel;
    private TextView pauseSync;
    private TextView connectivityIssue;
    private ImageView pauseAlert;
    private ImageView connectivityAlert;
    private SharedPreferences bacnet_pref;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_replaceccu, container, false);
        mContext = requireContext().getApplicationContext();
        passCode_1 = rootView.findViewById(R.id.passcode_text1);
        passCode_2 = rootView.findViewById(R.id.passcode_text2);
        passCode_3 = rootView.findViewById(R.id.passcode_text3);
        passCode_4 = rootView.findViewById(R.id.passcode_text4);
        passCode_5 = rootView.findViewById(R.id.passcode_text5);
        passCode_6 = rootView.findViewById(R.id.passcode_text6);
        next = rootView.findViewById(R.id.buttonNext);

        passCode_1.requestFocus();
        addTextWatcher(passCode_1);
        addTextWatcher(passCode_2);
        addTextWatcher(passCode_3);
        addTextWatcher(passCode_4);
        addTextWatcher(passCode_5);
        addTextWatcher(passCode_6);

        progressAlertDialog = new AlertDialog.Builder(getContext()).create();
        View progressBarView = LayoutInflater.from(getContext()).inflate(R.layout.ccu_replace_progress_bar, null);
        progressBar = progressBarView.findViewById(R.id.replace_progress_bar);
        progressAlertDialog.setView(progressBarView);
        progressAlertDialog.setCancelable(false);
        replaceStatus = progressBarView.findViewById(R.id.completedStatus);
        pauseStatus = progressBarView.findViewById(R.id.pauseStatus);
        cancel = progressBarView.findViewById(R.id.btnCancel);
        pauseSync = progressBarView.findViewById(R.id.btnPause);
        connectivityIssue = progressBarView.findViewById(R.id.connectivityIssue);
        pauseAlert = progressBarView.findViewById(R.id.pauseAlert);
        connectivityAlert = progressBarView.findViewById(R.id.connectivityAlert);
        bacnet_pref = Globals.getInstance().getApplicationContext().getSharedPreferences(CCU_REPLACE_BACNET_CONFIG,
                Context.MODE_PRIVATE);

        cancel.setOnClickListener(view -> {
            Toast.makeText(getActivity() , "Cancelling Replace CCU...", Toast.LENGTH_LONG).show();
            deleteRenatusData();
        });
        pauseSync.setOnClickListener(view -> {
            isProcessPaused.set(!isProcessPaused.get());
            if(isProcessPaused.get()) {
                Toast.makeText(getActivity(), "Pausing Replace CCU...", Toast.LENGTH_LONG).show();
                pauseReplaceProcess();
                pauseAlert.setVisibility(View.VISIBLE);
                pauseStatus.setText("PAUSED");
                pauseSync.setText("RESUME SYNCING");
            }
            else{
                pauseAlert.setVisibility(View.INVISIBLE);
                pauseStatus.setText("");
                pauseSync.setText(" PAUSE SYNCING");
                ExecutorTask.executeBackground( () -> resumeRestoreCCUProcess());
            }
        });

        toastFail = getLayoutInflater().inflate(R.layout.custom_toast_layout_failed,
                rootView.findViewById(R.id.custom_toast_layout_fail));
        toastCcuRestoreSuccess = getLayoutInflater().inflate(R.layout.custom_toast_replace_ccu_success_layout,
                rootView.findViewById(R.id.custom_toast_layout));

        next.setOnClickListener(v -> {
            enteredPassCode = passCode_1.getText().toString() + passCode_2.getText().toString() +
                    passCode_3.getText().toString() + passCode_4.getText().toString() +
                    passCode_5.getText().toString() + passCode_6.getText().toString();
            if (enteredPassCode.length() == 6) {
                validateBuildingPassCode(enteredPassCode);
            } else {
                Toast.makeText(mContext, "Please check the Building Passcode", Toast.LENGTH_SHORT).show();
            }
        });
        if(RestoreCCU.isReplaceCCUUnderProcess()){
            ProgressDialogUtils.showProgressDialog(getActivity(), "Retrieving Replace Status...");
            isReplaceClosed = true;
            pauseReplaceProcess();
            passCode_1.setFocusable(false);
            passCode_2.setFocusable(false);
            passCode_3.setFocusable(false);
            passCode_4.setFocusable(false);
            passCode_5.setFocusable(false);
            passCode_6.setFocusable(false);
            next.setClickable(false);
            SharedPreferences sharedPreferences =
                    Globals.getInstance().getApplicationContext().getSharedPreferences(ReplaceCCUTracker.REPLACING_CCU_INFO,
                            Context.MODE_PRIVATE);
            CCU ccu =  new Gson().fromJson(sharedPreferences.getString("CCU", ""), CCU.class);
            ExecutorTask.executeBackground(() -> initRestoreCCUProcess(ccu));

        }
        return rootView;
    }


    List<CCU> ccuList;

    private void getAllCCUs(String siteCode, JSONArray ccuArray) {
        ExecutorTask.executeAsync( () -> ProgressDialogUtils.showProgressDialog(getActivity(),
                "Getting list of CCUs..."),
                () -> {
                    RestoreCCU restoreCCU = new RestoreCCU();
                    ccuList = restoreCCU.getCCUList(siteCode, ccuArray);
                },
                ()-> {
                    ProgressDialogUtils.hideProgressDialog();
                    if (ccuList.size() > 0) {
                        showCCUListDialog();
                    } else {
                        showNoCCUFoundDialog();
                    }
                }
        );
    }

    private void validateBuildingPassCode(String enteredPassCode) {
        ResponseCallback responseCallBack = new ResponseCallback() {
            @Override
            public void onSuccessResponse(JSONObject response) throws JSONException {
                ProgressDialogUtils.hideProgressDialog();
                if (response.getString("valid").equals("true")) {
                    getAllCCUs(response.getJSONObject("siteCode").getString("siteId"), response.getJSONArray("devices"));
                } else {
                    Toast toast = new Toast(Globals.getInstance().getApplicationContext());
                    toast.setGravity(Gravity.BOTTOM, 50, 50);
                    toast.setView(toastFail);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.show();
                }

            }

            @Override
            public void onErrorResponse(JSONObject response) throws JSONException {
                ProgressDialogUtils.hideProgressDialog();
                response.getString("response");
                Toast.makeText(mContext, "Error while reading Building Passcode", Toast.LENGTH_LONG).show();
            }
        };
        ProgressDialogUtils.showProgressDialog(getActivity(), "Validating Building Passcode...");
        new OtpManager().validateOTP(enteredPassCode, responseCallBack);
    }

    private AlertDialog alertDialog;
    private void showCCUListDialog() {
        alertDialog = new AlertDialog.Builder(getContext()).create();
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.cuu_list_view, null);
        RecyclerView ccuListRecyclerView = dialogView.findViewById(R.id.ccus);
        TextView ccuVersionTextView = dialogView.findViewById(R.id.curr_ccu_version);
        ImageView close = dialogView.findViewById(R.id.close_button);
        CCUListAdapter adapter = new CCUListAdapter(ccuList,getContext(), this, getParentFragmentManager());
        ccuListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ccuListRecyclerView.setHasFixedSize(true);
        ccuListRecyclerView.setItemAnimator(new DefaultItemAnimator());
        ccuListRecyclerView.setAdapter(adapter);
        ccuVersionTextView.setText(CCUUiUtil.getCurrentCCUVersion());
        alertDialog.setView(dialogView);
        alertDialog.setCancelable(false);
        alertDialog.show();
        close.setOnClickListener(view -> { alertDialog.dismiss(); });
    }
    private void showNoCCUFoundDialog() {
        alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage("There are no CCUs present at this site.");
        alertDialog.setIcon(R.drawable.ic_dialog_alert);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OKAY", (dialogInterface, i) -> {
            alertDialog.dismiss();
        });
        alertDialog.show();
    }

    private void addTextWatcher(final EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @SuppressLint("NonConstantResourceId")
            @Override
            public void afterTextChanged(Editable s) {
                switch (editText.getId()) {
                    case R.id.passcode_text1:
                        if (editText.length() == 1) {
                            passCode_2.requestFocus();
                        }
                        break;
                    case R.id.passcode_text2:
                        if (editText.length() == 1) {
                            passCode_3.requestFocus();
                        } else if (editText.length() == 0) {
                            passCode_1.requestFocus();
                        }
                        break;
                    case R.id.passcode_text3:
                        if (editText.length() == 1) {
                            passCode_4.requestFocus();
                        } else if (editText.length() == 0) {
                            passCode_2.requestFocus();
                        }
                        break;
                    case R.id.passcode_text4:
                        if (editText.length() == 1) {
                            passCode_5.requestFocus();
                        } else if (editText.length() == 0) {
                            passCode_3.requestFocus();
                        }
                        break;
                    case R.id.passcode_text5:
                        if (editText.length() == 1) {
                            passCode_6.requestFocus();
                        } else if (editText.length() == 0) {
                            passCode_4.requestFocus();
                        }
                        break;
                    case R.id.passcode_text6:
                        if (editText.length() == 1) {
                            InputMethodManager inputManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                            next.setEnabled(true);
                        } else if (editText.length() == 0) {
                            passCode_5.requestFocus();
                            next.setEnabled(false);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + editText.getId());
                }
            }
        });
    }
    private AlertDialog replaceCCUDailog;
    @Override
    public void onCCUSelect(CCU ccu) {
        replaceCCUDailog = new AlertDialog.Builder(requireContext()).create();
        replaceCCUDailog.setTitle("Do you want to replace "+ ccu.getName()+"?");
        replaceCCUDailog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", (dialogInterface, i) -> {
            alertDialog.dismiss();
            retrieveCCUDetails(ccu);
        });
        replaceCCUDailog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", (dialogInterface, i) -> replaceCCUDailog.dismiss());
        replaceCCUDailog.setIcon(R.drawable.ic_alert);
        replaceCCUDailog.show();
    }

    private void retrieveCCUDetails(CCU ccu){
        ResponseCallback responseCallBack = new ResponseCallback() {
            @Override
            public void onSuccessResponse(JSONObject response) throws JSONException {
                ProgressDialogUtils.hideProgressDialog();
                String accessToken = response.getString("accessToken");
                CCUHsApi.getInstance().setJwt(accessToken);
                ProgressDialogUtils.showProgressDialog(getActivity(),
                        "Fetching equip details...");
                SharedPreferences.Editor editor =
                        Globals.getInstance().getApplicationContext().getSharedPreferences(ReplaceCCUTracker.REPLACING_CCU_INFO,
                        Context.MODE_PRIVATE).edit();
                editor.putString("CCU", new Gson().toJson(ccu));
                editor.commit();
                ExecutorTask.executeBackground(() -> initRestoreCCUProcess(ccu));

            }
            @Override
            public void onErrorResponse(JSONObject response) {
                ProgressDialogUtils.hideProgressDialog();
                Toast toast = new Toast(Globals.getInstance().getApplicationContext());
                toast.setGravity(Gravity.BOTTOM, 50, 50);
                toast.setView(toastFail);
                TextView textView = toast.getView().findViewById(R.id.custom_toast_message_detail);
                textView.setText(ccu.getName()+"  replace is failed as Bearer token cannot be generated. Please try again");
                toast.setDuration(Toast.LENGTH_LONG);
                toast.show();
                deleteRenatusData();
                CcuLog.e(TAG_CCU_REPLACE, "Replace CCU abrupted");
            }
        };
        ProgressDialogUtils.showProgressDialog(getActivity(), "Validating token...");
        new OtpManager().postBearerToken(ccu.getCcuId(), enteredPassCode, responseCallBack);
    }

    private void deleteRenatusData(){
       try{
            String packageName = Globals.getInstance().getApplicationContext().getPackageName();
            Runtime runtime = Runtime.getRuntime();
            runtime.exec("pm clear "+packageName);
        }
        catch (IOException exception){
            CcuLog.e(TAG_CCU_REPLACE, exception.getMessage());
            exception.printStackTrace();
        }
    }

    private void updateModbusConfigValues(Map<String, Integer> modbusConfigs){
        Prefs prefs = new Prefs(getContext());
        for (Map.Entry<String,Integer> mapElement : modbusConfigs.entrySet()) {
            prefs.setInt( mapElement.getKey(), mapElement.getValue());
        }
    }

    public void restoreConfigAndModBusJsonFiles(CCU ccu, AtomicInteger deviceCount, EquipResponseCallback
            equipResponseCallback, ReplaceCCUTracker replaceCCUTracker){
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.CONFIG_FILES, ReplaceStatus.RUNNING.toString());
        Map<String, Integer> modbusConfigs = new FileBackupManager().getConfigFiles(ccu.getSiteCode().replaceFirst("@"
                , ""), ccu.getCcuId().replaceFirst("@", ""), bacnet_pref);
        updateModbusConfigValues(modbusConfigs);
        equipResponseCallback.onEquipRestoreComplete(deviceCount.decrementAndGet());
        replaceCCUTracker.updateReplaceStatus(RestoreCCU.CONFIG_FILES, ReplaceStatus.COMPLETED.toString());
    }

    private void resumeRestoreCCUProcess(){
        CcuLog.i(TAG_CCU_REPLACE, "Resuming Replace CCU process");
        ReplaceCCUTracker replaceCCUTracker = new ReplaceCCUTracker();
        ConcurrentHashMap<String, ?> currentReplacementProgress =
                new ConcurrentHashMap<> (replaceCCUTracker.getReplaceCCUStatus());
        for (String equipId : currentReplacementProgress.keySet()) {
            if(currentReplacementProgress.get(equipId).toString().equals(ReplaceStatus.RUNNING.toString())){
                replaceCCUTracker.updateReplaceStatus(equipId, ReplaceStatus.PENDING.toString());
            }
        }
        replaceEquipsParallelly(replaceCCUTracker, getEquipResponseCallback(ccu), currentReplacementProgress,
                getRetryCountCallback());
    }

    private CCU ccu;
    private RestoreCCU restoreCCU;
    private Map<String, Set<String>> floorAndZoneIds;
    private int total;
    private AtomicInteger deviceCount;

    private void initRestoreCCUProcess(CCU ccu) {
        this.ccu = ccu;
        CcuLog.i(TAG_CCU_REPLACE, "Replace CCU Started");
        ReplaceCCUTracker replaceCCUTracker = new ReplaceCCUTracker();
        floorAndZoneIds = restoreCCU.getEquipDetailsOfCCU(ccu.getCcuId(), ccu.getSiteCode(),
                replaceCCUTracker.getEditor(), isReplaceClosed);
        ConcurrentHashMap<String, ?> currentReplacementProgress =
                new ConcurrentHashMap<> (replaceCCUTracker.getReplaceCCUStatus());
        restoreCCU.restoreCCUDevice(ccu);

        deviceCount = new AtomicInteger();
        deviceCount.set(currentReplacementProgress.size());
        total = deviceCount.get();

        handler.post(() -> {
            ProgressDialogUtils.hideProgressDialog();
            progressAlertDialog.dismiss();
            progressBar.setMax(total);
            progressAlertDialog.show();
        });
        EquipResponseCallback equipResponseCallback = getEquipResponseCallback(ccu);

        currentReplacementProgress.values().forEach(v ->{
            if(v.equals(ReplaceStatus.COMPLETED.toString())){
                equipResponseCallback.onEquipRestoreComplete(deviceCount.decrementAndGet());
            }
        });

        replaceEquipsParallelly(replaceCCUTracker, equipResponseCallback, currentReplacementProgress,
                getRetryCountCallback());
    }

    @NonNull
    private EquipResponseCallback getEquipResponseCallback(CCU ccu) {
        EquipResponseCallback equipResponseCallback = remainingCount -> handler.post(() -> {
            progressBar.setProgress((total - remainingCount));
            int percentCompleted = 100 - (int) ((remainingCount * 1.0 / total) * 100);
            CcuLog.i(TAG_CCU_REPLACE, "Syncing.. " + (total - remainingCount) + "/" + total + " (" + percentCompleted +
                    "%)");
            replaceStatus.setText("Syncing.. " + (total - remainingCount) + "/" + total + " (" + percentCompleted +
                    "%)");
            if (RestoreCCU.isReplaceCCUCompleted()) {
                Globals.getInstance().copyModels();
                CcuLog.i(TAG_CCU_REPLACE, "Replace CCU successfully completed");
                progressAlertDialog.dismiss();
                ReplaceCCU.this.displayToastMessageOnRestoreSuccess(ccu);
                ReplaceCCU.this.loadRenatusLandingIntent();
                ReplaceCCU.this.updatePreference();
                // Set the flag to indicate that After CCU replace is done, side Apps installation not required.
                PreferenceUtil.setSideAppsUpdateFinished();
                MessagingClient.getInstance().init();
                UtilityApplication.scheduleMessagingAckJob();
                CCUHsApi.getInstance().updateLocalTimeZone();
                RenatusServicesUrls renatusServicesUrls = RenatusServicesEnvironment.getInstance().getUrls();
                SequenceManager.getInstance(Globals.getInstance().getApplicationContext(), renatusServicesUrls.getSequencerUrl())
                        .fetchPredefinedSequencesIfEmpty();
            }
        });
        return equipResponseCallback;
    }

    @NonNull
    private RetryCountCallback getRetryCountCallback(){
        RetryCountCallback retryCountCallback = retryCount -> handler.post(() -> {
            if (retryCount == 0) {
                connectivityAlert.setVisibility(View.INVISIBLE);
                connectivityIssue.setText("");
            }
            else{
                connectivityAlert.setVisibility(View.VISIBLE);
                connectivityIssue.setText("Connectivity issues. Retry attempt("+retryCount+")");
                if(retryCount == 15 && !isProcessPaused.get()){
                    isProcessPaused.set(true);
                    Toast.makeText(getActivity(), "Pausing Replace CCU...", Toast.LENGTH_LONG).show();
                    pauseAlert.setVisibility(View.VISIBLE);
                    pauseStatus.setText("PAUSED");
                    pauseSync.setText("RESUME SYNCING");
                }
            }
            if(isProcessPaused.get()){
                pauseReplaceProcess();
            }
        });
        return retryCountCallback;
    }

    private void replaceEquipsParallelly(ReplaceCCUTracker replaceCCUTracker, EquipResponseCallback equipResponseCallback,
                                         ConcurrentHashMap<String, ?> currentReplacementProgress,
                                         RetryCountCallback retryCountCallback) {
        performSiteSyncIfNotCompleted(currentReplacementProgress, equipResponseCallback, replaceCCUTracker, retryCountCallback);
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() -1);
        for (String equipId : currentReplacementProgress.keySet()) {
            if(currentReplacementProgress.get(equipId).toString().equals(ReplaceStatus.COMPLETED.toString())
                    || equipId.equalsIgnoreCase(RestoreCCU.SYNC_SITE)){
                continue;
            }
            if (equipId.equalsIgnoreCase(RestoreCCU.CONFIG_FILES)) {
                restoreConfigAndModBusJsonFiles(ccu, deviceCount, equipResponseCallback, replaceCCUTracker);
                continue;
            }
            RestoreEntity restoreEntity = new RestoreEntity(ccu, restoreCCU, equipId, floorAndZoneIds,
                    equipResponseCallback, deviceCount, replaceCCUTracker, retryCountCallback);
            Future<?> future = executorService.submit(restoreEntity);
            futures.add(future);
        }
        for (Future<?> future : futures) {
            try {
                if(!future.isCancelled()) {
                    future.get();
                }
            } catch (CancellationException | InterruptedException | ExecutionException e ) {
                CcuLog.e(TAG_CCU_REPLACE,"Pausing replace process "+ e);
                e.printStackTrace();
            }
        }
    }

    private void pauseReplaceProcess(){
        CcuLog.i(TAG_CCU_REPLACE, "Pausing Replace CCU process");
        for (Future<?> future : futures) {
            future.cancel(true);
        }
        if(executorService != null) {
            executorService.shutdownNow();
        }
        futures.clear();
    }

    private void displayToastMessageOnRestoreSuccess(CCU ccu){
        Toast toast = new Toast(Globals.getInstance().getApplicationContext());
        toast.setGravity(Gravity.BOTTOM, 50, 50);
        toast.setView(toastCcuRestoreSuccess);
        TextView textView = toast.getView().findViewById(R.id.custom_toast_message_detail);
        textView.setText(ccu.getName()+" has been replaced successfully!");
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }

    private void loadRenatusLandingIntent(){
        Globals.getInstance().loadEquipProfiles();
        Intent intent = new Intent(getActivity(), a75f.io.renatus.RenatusLandingActivity.class);
        startActivity(intent);
    }

    private void updatePreference(){
        Prefs prefs = new Prefs(getContext());
        prefs.setBoolean("REGISTRATION", true);
        prefs.setBoolean("CCU_SETUP", true);
        prefs.setBoolean("PROFILE_SETUP", true);
        prefs.setBoolean("isCcuRegistered", true);
        PreferenceUtil.setTempModeMigrationNotRequired();
        prefs.setString(BacnetConfigConstants.BACNET_CONFIGURATION, bacnet_pref.getString(BacnetConfigConstants.BACNET_CONFIGURATION,null));
        prefs.setBoolean(BacnetConfigConstants.IS_BACNET_INITIALIZED, bacnet_pref.getBoolean(BacnetConfigConstants.IS_BACNET_INITIALIZED,false));
        prefs.setBoolean(BacnetConfigConstants.IS_BACNET_CONFIG_FILE_CREATED, bacnet_pref.getBoolean(BacnetConfigConstants.IS_BACNET_CONFIG_FILE_CREATED,false));

        prefs.setBoolean(BacnetConfigConstants.BACNET_FD_AUTO_STATE, bacnet_pref.getBoolean(BacnetConfigConstants.BACNET_FD_AUTO_STATE,false));
        prefs.setString(BacnetConfigConstants.BACNET_BBMD_CONFIGURATION, bacnet_pref.getString(BacnetConfigConstants.BACNET_BBMD_CONFIGURATION,null));
        prefs.setString(BacnetConfigConstants.BACNET_FD_CONFIGURATION, bacnet_pref.getString(BacnetConfigConstants.BACNET_FD_CONFIGURATION,null));
        prefs.setString(BacnetConfigConstants.BACNET_DEVICE_TYPE, bacnet_pref.getString(BacnetConfigConstants.BACNET_DEVICE_TYPE,null));
        DashboardHandlerKt.getDashboardConfiguration();
    }

    private void performSiteSyncIfNotCompleted(ConcurrentHashMap<String, ?> currentReplacementProgress,
                                              EquipResponseCallback equipResponseCallback,
                                              ReplaceCCUTracker replaceCCUTracker, RetryCountCallback retryCountCallback) {
        if(currentReplacementProgress.get(RestoreCCU.SYNC_SITE).toString().equals(ReplaceStatus.COMPLETED.toString())){
            CcuLog.d(TAG_CCU_REPLACE, "SYNC_SITE operation is already completed. Skipping the operation.");
            return;
        }
        CcuLog.d(TAG_CCU_REPLACE, "Performing SYNC_SITE operation.");
        restoreCCU.syncExistingSite(ccu.getSiteCode(), deviceCount, equipResponseCallback,
                replaceCCUTracker, retryCountCallback);
        CcuLog.d(TAG_CCU_REPLACE, "SYNC_SITE operation executed.");
    }
}
