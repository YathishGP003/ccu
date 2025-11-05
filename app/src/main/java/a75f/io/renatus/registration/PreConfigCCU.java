package a75f.io.renatus.registration;

import static com.raygun.raygun4android.RaygunClient.getApplicationContext;

import static a75f.io.logic.bo.util.CCUUtils.isRecommendedVersionCheckIsNotFalse;
import static a75f.io.renatus.UtilityApplication.context;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaos.view.PinView;

import org.json.JSONException;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.preconfig.InvalidPreconfigurationDataException;
import a75f.io.logic.preconfig.InvalidStagesException;
import a75f.io.logic.preconfig.PreconfigurationData;
import a75f.io.logic.preconfig.PreconfigurationHandler;
import a75f.io.logic.preconfig.PreconfigurationManager;
import a75f.io.logic.preconfig.PreconfigurationRepository;
import a75f.io.logic.preconfig.PreconfigurationState;
import a75f.io.logic.preconfig.UnsupportedTimeZoneException;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.renatus.R;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.util.ExecutorTask;

public class PreConfigCCU extends Fragment {

    private ImageView   imageGoback;
    private Button              mNext;
    private Context             mContext;

    PinView passCodeView;
    private Handler mainHandler;
    private static final String TAG = PreConfigCCU.class.getSimpleName();

    public PreConfigCCU() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StartCCUFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PreConfigCCU newInstance(String param1, String param2) {
        PreConfigCCU fragment = new PreConfigCCU();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_preconfigccu, container, false);

        mContext = getContext().getApplicationContext();

        imageGoback = rootView.findViewById(R.id.imageGoback);

        passCodeView = rootView.findViewById(R.id.pinView);

        mNext = rootView.findViewById(R.id.buttonNext);

        imageGoback.setOnClickListener(v -> {
            // TODO Auto-generated method stub
            ((FreshRegistration)getActivity()).selectItem(1);
        });

        mNext.setOnClickListener(v -> {
            String passCode = passCodeView.getText().toString();
            if (passCode.length() == 6) {
                validatePasscode(passCode);
            } else {
                SnackbarUtil.showInfoMessage(rootView, "Please enter a valid 6 digit passcode");
            }
        });

        mainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull android.os.Message msg) {
                SnackbarUtil.showInfoMessage(rootView, (String) msg.obj);
            }
        };

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PreconfigurationManager.INSTANCE.getState() instanceof PreconfigurationState.Started) {
            if (isRecommendedVersionCheckIsNotFalse()) {
                CcuLog.d(L.TAG_PRECONFIGURATION, "Preconfiguration : recommended version check");
                if (PreferenceUtil.getUpdateCCUStatus() || PreferenceUtil.isCCUInstalling()) {
                    FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                    Fragment fragmentByTag = getParentFragmentManager().findFragmentByTag("popup");
                    if (fragmentByTag != null) {
                        ft.remove(fragmentByTag);
                    }
                    try {
                        UpdateCCUFragment updateCCUFragment = new UpdateCCUFragment().resumeDownloadProcess(
                                PreferenceUtil.getUpdateCCUStatus(), PreferenceUtil.isCCUInstalling(), false);
                        updateCCUFragment.show(ft, "popup");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    View toastLayout = inflater.inflate(R.layout.custom_layout_ccu_successful_update, null);
                    UpdateCCUFragment updateCCUFragment = new UpdateCCUFragment();
                    updateCCUFragment.checkIsCCUHasRecommendedVersion(requireActivity(), getParentFragmentManager(), toastLayout, getContext(), requireActivity());
                }
            }
        } else if (PreconfigurationManager.INSTANCE.getState() instanceof PreconfigurationState.Downloaded) {
            CcuLog.d(L.TAG_PRECONFIGURATION, "Preconfiguration Resume : data already downloaded");
            PreconfigurationData preconfigurationData = PreconfigurationRepository.INSTANCE.getConfigurationData(mContext);
            confirmSiteDetails(preconfigurationData, getActivity());
        } else if (PreconfigurationManager.INSTANCE.getState() instanceof PreconfigurationState.Progress) {
            CcuLog.d(L.TAG_PRECONFIGURATION, "Preconfiguration Resume : was already in progress");
            PreconfigurationData preconfigurationData = PreconfigurationRepository.INSTANCE.getConfigurationData(mContext);
            doPreconfiguration(preconfigurationData, true );
        } else if (PreconfigurationManager.INSTANCE.getState() instanceof PreconfigurationState.Failed) {
            CcuLog.d(L.TAG_PRECONFIGURATION, "Preconfiguration Resume : failed");
            SnackbarUtil.showInfoMessage(getView(), "Preconfiguration Failed");
        }
    }

    private void validatePasscode(String passcode) {
        ExecutorTask.executeAsync(
                () -> ProgressDialogUtils.showProgressDialog(getActivity(),""),
                () -> {
                    String data = PreconfigurationRepository.INSTANCE.fetchPreconfigurationData(passcode);
                    CcuLog.d(L.TAG_PRECONFIGURATION, "Preconfiguration data fetched: " + data);
                    if (data != null) {
                        PreconfigurationRepository.INSTANCE.persistPreconfigurationData(data.toString(), mContext);
                        PreconfigurationManager.INSTANCE.transitionTo(PreconfigurationState.Downloaded.INSTANCE);
                    } else {
                        PreconfigurationManager.INSTANCE.transitionTo(PreconfigurationState.Failed.INSTANCE);
                    }

                },
                () -> {
                    ProgressDialogUtils.hideProgressDialog();
                    if (PreconfigurationManager.INSTANCE.getState() instanceof PreconfigurationState.Downloaded) {
                        CcuLog.d(L.TAG_PRECONFIGURATION, "Preconfiguration data fetch succeeded");
                        PreconfigurationData preconfigurationData = PreconfigurationRepository.INSTANCE.getConfigurationData(mContext);
                        if (preconfigurationData == null) {
                            PreconfigurationManager.INSTANCE.transitionTo(PreconfigurationState.Failed.INSTANCE);
                            postMessage("Failed to fetch preconfiguration data!!");
                            return;
                        }
                        confirmSiteDetails(preconfigurationData, getActivity());
                    } else {
                        CcuLog.d(L.TAG_PRECONFIGURATION, "Preconfiguration data fetch failed");
                        postMessage("Passcode validation Failed !!");
                    }
                });

    }

    @SuppressLint("SetTextI18n")
    private void confirmSiteDetails(PreconfigurationData data, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        SpannableStringBuilder spannable = new SpannableStringBuilder();

        String siteName = data.getSiteName();
        String siteAddress = data.getSiteAddress().getFormattedAddress();

        SpannableString boldName = new SpannableString(siteName + "\n");
        boldName.setSpan(new StyleSpan(Typeface.BOLD), 0, boldName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannable.append(boldName);
        spannable.append(siteAddress);

        TextView messageView = new TextView(context);
        messageView.setText(spannable);
        messageView.setPadding(15, 25, 15, 25);
        messageView.setTextSize(18);
        messageView.setBackgroundColor(CCUUiUtil.getSecondaryColor());
        messageView.setGravity(Gravity.START);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 20, 40, 20);
        container.setGravity(Gravity.CENTER);

        container.addView(messageView);

        builder.setTitle("Site Details")
                .setView(container)
                .setCancelable(true)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Next", (dialog, which) -> {
                    dialog.dismiss();
                    PreconfigurationManager.INSTANCE.transitionTo(PreconfigurationState.Progress.INSTANCE);
                    CcuLog.d(L.TAG_PRECONFIGURATION, "Preconfiguration in progress");
                    doPreconfiguration(data, false);
                });

        AlertDialog dialog = builder.create();
        dialog.show();
        Button cancel = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        Button next   = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        cancel.setTextColor(getPrimaryColor(context));
        next.setTextColor(getPrimaryColor(context));

    }
    private int getPrimaryColor(Context context) {
        if (CCUUiUtil.isCarrierThemeEnabled(context)) {
            return ContextCompat.getColor(context, R.color.carrier_75f);
        } else if (CCUUiUtil.isAiroverseThemeEnabled(context)) {
            return ContextCompat.getColor(context, R.color.airoverse_primary);
        } else {
            return ContextCompat.getColor(context, R.color.renatus_75f_primary);
        }
    }

    private void doPreconfiguration(PreconfigurationData data, Boolean isRetry) {
        ExecutorTask.executeAsync(
                () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Setting up the Site. This might take some time..."),
                () -> {
                    try {
                        CCUHsApi.getInstance().setPreconfigInProgress(true);
                        PreconfigurationHandler preconfigurationHandler = new PreconfigurationHandler();
                        preconfigurationHandler.validatePreconfigurationData(data);
                        Prefs prefs = new Prefs(getContext());
                        prefs.setString("sitePreConfigId", data.getSitePreConfigId());
                        if (isRetry) {
                            preconfigurationHandler.cleanUpPreconfigurationData(CCUHsApi.getInstance());
                        }
                        String siteId = preconfigurationHandler.handlePreconfiguration(data, CCUHsApi.getInstance());
                        prefs.setString("SITE_ID", siteId);
                        PreconfigurationManager.INSTANCE.transitionTo(PreconfigurationState.Completed.INSTANCE);
                        CcuLog.d(L.TAG_PRECONFIGURATION, "Preconfiguratzion completed successfully");
                    } catch (InvalidPreconfigurationDataException e) {
                        CcuLog.e(L.TAG_PRECONFIGURATION, "InvalidPreconfigurationDataException: "+e.getMessage());
                        PreconfigurationManager.INSTANCE.transitionTo(PreconfigurationState.Failed.INSTANCE);
                        postMessage("Invalid Preconfiguration Data: "+e.getMessage());
                    } catch (InvalidStagesException e) {
                        CcuLog.e(L.TAG_PRECONFIGURATION, "InvalidStagesException: "+e.getMessage());
                        PreconfigurationManager.INSTANCE.transitionTo(PreconfigurationState.Failed.INSTANCE);
                        postMessage("Invalid Stages: "+e.getMessage());
                   } catch (UnsupportedTimeZoneException e) {
                        CcuLog.e(L.TAG_PRECONFIGURATION, "UnsupportedTimeZoneException: "+e.getMessage());
                        PreconfigurationManager.INSTANCE.transitionTo(PreconfigurationState.Failed.INSTANCE);
                        postMessage(e.getMessage());
                    } catch (Exception e) {
                        CcuLog.e(L.TAG_PRECONFIGURATION, "Preconfiguration failed: " + e.getMessage());
                        e.printStackTrace();
                        PreconfigurationManager.INSTANCE.transitionTo(PreconfigurationState.Failed.INSTANCE);
                        postMessage("Preconfiguration failed: " + e.getMessage());
                    } finally {
                        CCUHsApi.getInstance().setPreconfigInProgress(false);
                    }
                },
                () -> {
                    ProgressDialogUtils.hideProgressDialog();
                    if (PreconfigurationManager.INSTANCE.getState() instanceof PreconfigurationState.Completed) {
                        LSerial.getInstance().setResetSeedMessage(true);
                        goTonext();
                    }
                }

        );


    }

    private void postMessage(String text) {
        Message message = Message.obtain();
        message.obj = text;
        mainHandler.sendMessage(message);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void goTonext() {
        ((FreshRegistration)getActivity()).selectItem(21);
    }
}
