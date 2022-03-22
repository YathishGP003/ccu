package a75f.io.renatus.registration;

import static com.raygun.raygun4android.RaygunClient.getApplicationContext;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.exception.NullHGridException;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.ccu.restore.CCU;
import a75f.io.logic.ccu.restore.EquipResponseCallback;
import a75f.io.logic.ccu.restore.RestoreCCU;
import a75f.io.logic.cloud.FileBackupManager;
import a75f.io.logic.cloud.OtpManager;
import a75f.io.logic.cloud.ResponseCallback;
import a75f.io.modbusbox.EquipsManager;
import a75f.io.renatus.R;
import a75f.io.renatus.util.CCUListAdapter;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;

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

    public ReplaceCCU() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    private ProgressDialog progressBar;
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

        progressBar = new ProgressDialog(getContext());
        progressBar.setCancelable(false);
        progressBar.setMessage("Restoring all modules...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        passCode_1.requestFocus();
        addTextWatcher(passCode_1);
        addTextWatcher(passCode_2);
        addTextWatcher(passCode_3);
        addTextWatcher(passCode_4);
        addTextWatcher(passCode_5);
        addTextWatcher(passCode_6);

        toastFail = getLayoutInflater().inflate(R.layout.custom_toast_layout_failed,
                (ViewGroup) rootView.findViewById(R.id.custom_toast_layout_fail));
        toastCcuRestoreSuccess = getLayoutInflater().inflate(R.layout.custom_toast_replace_ccu_success_layout,
                (ViewGroup) rootView.findViewById(R.id.custom_toast_layout));

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

        return rootView;
    }


    List<CCU> ccuList;

    private void getAllCCUs(String siteCode, JSONArray ccuArray) {
        RxjavaUtil.executeBackgroundTask( () -> ProgressDialogUtils.showProgressDialog(getActivity(),
                "Getting list of CCUs..."),
                () -> {
                    RestoreCCU restoreCCU = new RestoreCCU();
                    ccuList = restoreCCU.getCCUList(siteCode, ccuArray);
                },
                ()-> {
                    ProgressDialogUtils.hideProgressDialog();
                    showCCUListDialog();
                }
        );
    }

    private void validateBuildingPassCode(String enteredPassCode) {
        ResponseCallback responseCallBack = new ResponseCallback() {
            @Override
            public void onSuccessResponse(JSONObject response) throws JSONException {
                ProgressDialogUtils.hideProgressDialog();
                Toast toast = new Toast(getApplicationContext());
                toast.setGravity(Gravity.BOTTOM, 50, 50);
                if (response.getString("valid") == "true") {
                    getAllCCUs(response.getJSONObject("siteCode").getString("siteId"), response.getJSONArray("devices"));

                } else {
                    toast.setView(toastFail);
                }
                toast.setDuration(Toast.LENGTH_LONG);
                toast.show();
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
        CCUListAdapter adapter = new CCUListAdapter(ccuList,getContext(), this);
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
    private AlertDialog replaceCCUErrorDailog;
    @Override
    public void onCCUSelect(CCU ccu) {
        replaceCCUDailog = new AlertDialog.Builder(getContext()).create();
        replaceCCUDailog.setTitle("Do you want to replace "+ ccu.getName()+"?");
        replaceCCUDailog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", (dialogInterface, i) -> {
            alertDialog.dismiss();
            retriveCCUDetails(ccu);
        });
        replaceCCUDailog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", (dialogInterface, i) -> replaceCCUDailog.dismiss());
        replaceCCUDailog.setIcon(R.drawable.ic_alert);
        replaceCCUDailog.show();
    }

    private void retriveCCUDetails(CCU ccu){
        ResponseCallback responseCallBack = new ResponseCallback() {
            @Override
            public void onSuccessResponse(JSONObject response) throws JSONException {
                ProgressDialogUtils.hideProgressDialog();
                String accessToken = response.getString("accessToken");
                CCUHsApi.getInstance().setJwt(accessToken);
                AtomicBoolean success = new AtomicBoolean(true);
                RxjavaUtil.executeBackgroundTask( () -> ProgressDialogUtils.showProgressDialog(getActivity(),
                            "Syncing Site Details. This may take more than a minute..."),
                        () -> {
                                try {
                                    initRestoreCCUProcess(ccu);
                                    List<HashMap> devices = CCUHsApi.getInstance().readAll("device and addr");
                                    int pairingAddress = 1000;
                                    for(HashMap device : devices){
                                        int devicePairingAddress  = Integer.parseInt(device.get("addr").toString());
                                        if(devicePairingAddress > pairingAddress){
                                            pairingAddress = devicePairingAddress;
                                        }
                                    }
                                    pairingAddress = (pairingAddress/100) * 100;
                                    L.ccu().setSmartNodeAddressBand((short)pairingAddress);
                                }
                                catch(NullHGridException nullHGridException){
                                    success.set(false);
                                    Log.i("Replace CCU", nullHGridException.getMessage());
                                    nullHGridException.printStackTrace();
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            replaceCCUErrorDailog = new AlertDialog.Builder(getContext()).create();
                                            replaceCCUErrorDailog.setTitle("Error occurred while replacing "+ ccu.getName());
                                            replaceCCUErrorDailog.setMessage( "Please check your wifi connection,  " +
                                                    "delete Renatus data and try once again.");
                                            replaceCCUErrorDailog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", (dialogInterface, i) -> {
                                                ProgressDialogUtils.hideProgressDialog();
                                                System.exit(0);
                                            });
                                            replaceCCUErrorDailog.setIcon(R.drawable.ic_alert);
                                            replaceCCUErrorDailog.show();
                                        }
                                  });
                                }
                            },
                        ()-> {
                            if(success.get()){
                                displayToastMessageOnRestoreSuccess(ccu);
                                loadRenatusLandingIntent();
                                updatePreference();
                            }

                        }
                    );
            }
            @Override
            public void onErrorResponse(JSONObject response) throws JSONException {
                ProgressDialogUtils.hideProgressDialog();
                Toast toast = new Toast(getApplicationContext());
                toast.setGravity(Gravity.BOTTOM, 50, 50);
                toast.setView(toastFail);
                TextView textView = toast.getView().findViewById(R.id.custom_toast_message_detail);
                textView.setText(ccu.getName()+"  replace is failed. Please clear the app data and try " +
                        "once again");
                toast.setDuration(Toast.LENGTH_LONG);
                toast.show();
            }
        };
        ProgressDialogUtils.showProgressDialog(getActivity(), "Validating token...");
        new OtpManager().postBearerToken(ccu.getCcuId(), enteredPassCode, responseCallBack);
    }

    private void initRestoreCCUProcess(CCU ccu) {
        new FileBackupManager().getConfigFiles(ccu.getSiteCode().replaceFirst("@", ""),
                ccu.getCcuId().replaceFirst("@", ""));
        new FileBackupManager().getModbusSideLoadedJsonsFiles(ccu.getSiteCode().replaceFirst("@", ""),
                ccu.getCcuId().replaceFirst("@", ""));
        RestoreCCU restoreCCU = new RestoreCCU();
        restoreCCU.getCCUEquip(ccu.getCcuId());
        final int[] deviceCount = {restoreCCU.equipCountsInCCU(ccu.getCcuId(), ccu.getSiteCode())};
        restoreCCU.syncExistingSite(ccu.getSiteCode());
        restoreCCU.getSystemProfileOfCCU(ccu.getCcuId(), ccu.getSiteCode());
        restoreCCU.getCMDeviceOfCCU(ccu.getCcuId(), ccu.getSiteCode());
        restoreCCU.getDiagEquipOfCCU(ccu.getCcuId(), ccu.getSiteCode());
        EquipsManager.getInstance().getProcessor().readExternalJsonData();
        int total = deviceCount[0] - 1; // -1 because by now diag equip is already restored.
        ProgressDialogUtils.hideProgressDialog();
        new Handler(Looper.getMainLooper()).post(() -> {
            progressBar.setMax(total);
            progressBar.show();
        });
        EquipResponseCallback equipResponseCallback = remainingCount -> {
            deviceCount[0] = remainingCount;
            new Handler(Looper.getMainLooper()).post(() -> progressBar.setProgress((total - remainingCount) + 1));
        };
        restoreCCU.getModbusSystemEquip(ccu.getCcuId(), ccu.getSiteCode(), deviceCount[0], equipResponseCallback);
        restoreCCU.getZoneEquipsOfCCU(ccu.getCcuId(), ccu.getSiteCode(), deviceCount[0], equipResponseCallback);
        restoreCCU.getOAOEquip(ccu.getCcuId(), ccu.getSiteCode(), deviceCount[0], equipResponseCallback);
    }

    private void displayToastMessageOnRestoreSuccess(CCU ccu){
        Toast toast = new Toast(getApplicationContext());
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
    }
}
