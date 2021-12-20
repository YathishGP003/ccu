package a75f.io.renatus.registration;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.google.android.material.textfield.TextInputLayout;
import a75f.io.logic.bo.util.RenatusLogicIntentActions;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.RxjavaUtil;

import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimeZone;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.diag.DiagEquip;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.R;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;



public class CreateNewSite extends Fragment {
    private static final String TAG = CreateNewSite.class.getSimpleName();
    TextInputLayout mTextInputSitename;
    EditText mSiteName;

    Spinner mTimeZoneSelector;
    TextView mTextTimeZone;
    ArrayAdapter<String> timeZoneAdapter;

    TextInputLayout mTextInputStreetAdd;
    EditText mStreetAdd;

    TextInputLayout mTextInputCity;
    EditText mSiteCity;

    TextInputLayout mTextInputState;
    EditText mSiteState;

    TextInputLayout mTextInputCountry;
    EditText mSiteCountry;

    TextInputLayout mTextInputZip;
    EditText mSiteZip;

    TextInputLayout mTextInputCCU;
    EditText mSiteCCU;

    TextInputLayout mTextInputEmail;
    EditText mSiteEmailId;

    TextInputLayout mTextInputInstallerEmail;
    EditText mSiteInstallerEmailId;

    TextInputLayout mTextInputOrg;
    EditText mSiteOrg;

    Button mNext;

    private TextView btnEditSite;
    private TextView btnUnregisterSite;

    private ImageView imgEditSite;
    private ImageView imgUnregisterSite;

    Context mContext;
    LinearLayout btnSetting;
    Prefs prefs;
    private boolean isFreshRegister;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_createnewsite, container, false);

        mContext = getContext().getApplicationContext();
        isFreshRegister = getActivity() instanceof FreshRegistration;

        if (!isFreshRegister) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) rootView.getLayoutParams();
            p.setMargins(50, 50, 0, 0);
        }

        prefs = new Prefs(mContext);

        HashMap site = CCUHsApi.getInstance().read("site");
        HashMap ccu = CCUHsApi.getInstance().read("device and ccu");

        mTextInputSitename = rootView.findViewById(R.id.textInputSitename);
        mSiteName = rootView.findViewById(R.id.editSitename);

        mTimeZoneSelector = rootView.findViewById(R.id.timeZoneSelector);
        CCUUiUtil.setSpinnerDropDownColor(mTimeZoneSelector,getContext());
        mTextTimeZone = rootView.findViewById(R.id.textTimeZone);

        mTextInputStreetAdd = rootView.findViewById(R.id.textInputStreeAdd);
        mStreetAdd = rootView.findViewById(R.id.editStreetAdd);

        mTextInputCity = rootView.findViewById(R.id.textInputCity);
        mSiteCity = rootView.findViewById(R.id.editCity);

        mTextInputState = rootView.findViewById(R.id.textInputState);
        mSiteState = rootView.findViewById(R.id.editState);

        mTextInputCountry = rootView.findViewById(R.id.textInputCountry);
        mSiteCountry = rootView.findViewById(R.id.editCountry);

        mTextInputZip = rootView.findViewById(R.id.textInputZip);
        mSiteZip = rootView.findViewById(R.id.editZip);

        mTextInputCCU = rootView.findViewById(R.id.textInputCCU);
        mSiteCCU = rootView.findViewById(R.id.editCCU);

        mTextInputEmail = rootView.findViewById(R.id.textInputEmail);
        mSiteEmailId = rootView.findViewById(R.id.editFacilityEmail);

        mTextInputInstallerEmail = rootView.findViewById(R.id.textInputInstallerEmail);
        mSiteInstallerEmailId = rootView.findViewById(R.id.editInstallerEmail);

        mTextInputOrg = rootView.findViewById(R.id.textInputOrganization);
        mSiteOrg = rootView.findViewById(R.id.editFacilityOrganization);

        mNext = rootView.findViewById(R.id.buttonNext);
        btnSetting = rootView.findViewById(R.id.btnSetting);
        btnEditSite = rootView.findViewById(R.id.btnEditSite);
        imgEditSite = rootView.findViewById(R.id.imgEditSite);
        btnUnregisterSite = rootView.findViewById(R.id.btnUnregisterSite);
        imgUnregisterSite = rootView.findViewById(R.id.imgUnregisterSite);

        if (isFreshRegister) {
            mNext.setVisibility(View.VISIBLE);
            btnSetting.setVisibility(View.GONE);
        } else {
            mNext.setVisibility(View.GONE);
            btnSetting.setVisibility(View.VISIBLE);
            enableViews(false);
        }
        populateAndUpdateTimeZone();

        mSiteName.setHint(getHTMLCodeForHints(R.string.input_sitename));
        mStreetAdd.setHint(getHTMLCodeForHints(R.string.input_streetadd));
        mSiteCity.setHint(getHTMLCodeForHints(R.string.input_city));
        mSiteState.setHint(getHTMLCodeForHints(R.string.input_state));
        mSiteCountry.setHint(getHTMLCodeForHints(R.string.input_country));
        mSiteZip.setHint(getHTMLCodeForHints(R.string.input_zip));
        mSiteCCU.setHint(getHTMLCodeForHints(R.string.input_ccuname));
        mSiteEmailId.setHint(getHTMLCodeForHints(R.string.input_facilityemail));
        mSiteOrg.setHint(getHTMLCodeForHints(R.string.input_facilityorg));
        mSiteInstallerEmailId.setHint(getHTMLCodeForHints(R.string.input_installer_email));

        if (CCUHsApi.getInstance().isCCURegistered()) {
            btnUnregisterSite.setText("Unregister");
            btnUnregisterSite.setTextColor(getResources().getColor(R.color.black_listviewtext));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setCompoundDrawableColor(btnUnregisterSite, R.color.black_listviewtext);
            }
            imgUnregisterSite.setColorFilter(getResources().getColor(R.color.black_listviewtext));
            btnEditSite.setEnabled(true);
        } else {
            btnEditSite.setEnabled(false);
            btnUnregisterSite.setText("Register");
            btnUnregisterSite.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
            imgUnregisterSite.setColorFilter(CCUUiUtil.getPrimaryThemeColor(getContext()));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setCompoundDrawableColor(btnUnregisterSite, CCUUiUtil.getPrimaryThemeColor(getContext()));
            }
        }

        mTextInputSitename.setHintEnabled(true);

        mTextInputSitename.setErrorEnabled(true);
        mTextInputStreetAdd.setErrorEnabled(true);
        mTextInputCity.setErrorEnabled(true);
        mTextInputState.setErrorEnabled(true);
        mTextInputCountry.setErrorEnabled(true);
        mTextInputZip.setErrorEnabled(true);
        mTextInputCCU.setErrorEnabled(true);
        mTextInputEmail.setErrorEnabled(true);
        mTextInputInstallerEmail.setErrorEnabled(true);
        mTextInputOrg.setErrorEnabled(true);

        mTextInputSitename.setError(getString(R.string.hint_sitename));
        mTextInputStreetAdd.setError("");
        mTextInputCity.setError("");
        mTextInputState.setError("");
        mTextInputCountry.setError("");
        mTextInputZip.setError("");
        mTextInputCCU.setError("");
        mTextInputEmail.setError("");
        mTextInputInstallerEmail.setError("");
        mTextInputOrg.setError("");


        mSiteName.addTextChangedListener(new EditTextWatcher(mSiteName));
        mStreetAdd.addTextChangedListener(new EditTextWatcher(mStreetAdd));
        mSiteCity.addTextChangedListener(new EditTextWatcher(mSiteCity));
        mSiteState.addTextChangedListener(new EditTextWatcher(mSiteState));
        mSiteCountry.addTextChangedListener(new EditTextWatcher(mSiteCountry));
        mSiteZip.addTextChangedListener(new EditTextWatcher(mSiteZip));
        mSiteCCU.addTextChangedListener(new EditTextWatcher(mSiteCCU));
        mSiteEmailId.addTextChangedListener(new EditTextWatcher(mSiteEmailId));
        mSiteInstallerEmailId.addTextChangedListener(new EditTextWatcher(mSiteInstallerEmailId));
        mSiteOrg.addTextChangedListener(new EditTextWatcher(mSiteOrg));


        mNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mNext.setEnabled(false);
                int[] mandotaryIds = new int[]
                        {
                                R.id.editSitename,
                                R.id.editStreetAdd,
                                R.id.editCity,
                                R.id.editState,
                                R.id.editCountry,
                                R.id.editZip,
                                R.id.editCCU,
                                R.id.editFacilityEmail,
                                R.id.editFacilityOrganization,
                                R.id.editInstallerEmail,
                        };
                if (!validateEditText(mandotaryIds) && Patterns.EMAIL_ADDRESS.matcher(mSiteEmailId.getText().toString()).matches()
                    && Patterns.EMAIL_ADDRESS.matcher(mSiteInstallerEmailId.getText().toString()).matches()) {
                    ProgressDialogUtils.showProgressDialog(getActivity(),"Adding New Site...");
                    String siteName = mSiteName.getText().toString();
                    String siteCity = mSiteCity.getText().toString();
                    String siteZip = mSiteZip.getText().toString();
                    String siteAddress = mStreetAdd.getText().toString();
                    String siteState = mSiteState.getText().toString();
                    String siteCountry = mSiteCountry.getText().toString();

                    String managerEmail = mSiteEmailId.getText().toString();
                    String installerEmail = mSiteInstallerEmailId.getText().toString();
                    String installerOrg = mSiteOrg.getText().toString();
                    String ccuName = mSiteCCU.getText().toString();

                    if (site.size() > 0) {
                        String siteId = site.get("id").toString();
                        updateSite(siteName, siteCity, siteZip, siteAddress, siteState, siteCountry, siteId,installerOrg, installerEmail, managerEmail);
                    } else {
                        saveSite(siteName, siteCity, siteZip, siteAddress, siteState, siteCountry, installerOrg, installerEmail,managerEmail);
                    }

                    if (ccu.size() > 0) {
                        String ahuRef = ccu.get("ahuRef").toString();
                        CCUHsApi.getInstance().updateCCU(ccuName, installerEmail, ahuRef, managerEmail);
                        L.ccu().setCCUName(ccuName);
                    } else {
                        String localId = CCUHsApi.getInstance().createCCU(ccuName, installerEmail, DiagEquip.getInstance().create(),managerEmail);
                        L.ccu().setCCUName(ccuName);
                        CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(localId));
                    }
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mNext.setEnabled(true);
                            ProgressDialogUtils.hideProgressDialog();
                            goTonext();
                        }
                    }, 1000);
                } else {
                    mNext.setEnabled(true);
                }
            }
        });
        
        View.OnClickListener editSiteOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnEditSite.getText().toString().equals(getResources().getString(R.string.title_edit))) {
                    enableViews(true);
                    btnEditSite.setText(getResources().getString(R.string.title_save));
                } else {
                    enableViews(false);
                    btnEditSite.setText(getResources().getString(R.string.title_edit));
                    int[] mandotaryIds = new int[]
                            {
                                    R.id.editSitename,
                                    R.id.editStreetAdd,
                                    R.id.editCity,
                                    R.id.editState,
                                    R.id.editCountry,
                                    R.id.editZip,
                                    R.id.editCCU,
                                    R.id.editFacilityEmail,
                                    R.id.editFacilityOrganization,
                                    R.id.editInstallerEmail,
                            };
                    if (!validateEditText(mandotaryIds)) {
                        String siteName = mSiteName.getText().toString();
                        String siteCity = mSiteCity.getText().toString();
                        String siteZip = mSiteZip.getText().toString();
                        String siteAddress = mStreetAdd.getText().toString();
                        String siteState = mSiteState.getText().toString();
                        String siteCountry = mSiteCountry.getText().toString();

                        String installerEmail = mSiteInstallerEmailId.getText().toString();
                        String facilityManagerEmail = mSiteEmailId.getText().toString();
                        String installerOrg = mSiteOrg.getText().toString();
                        String ccuName = mSiteCCU.getText().toString();

                        if (site.size() > 0) {
                            String siteId = site.get("id").toString();
                            updateSite(siteName, siteCity, siteZip, siteAddress, siteState, siteCountry, siteId, installerOrg, installerEmail, facilityManagerEmail);
                        } else {
                            saveSite(siteName, siteCity, siteZip, siteAddress, siteState, siteCountry,installerOrg, installerEmail,facilityManagerEmail);
                        }
    
                        Intent locationUpdateIntent = new Intent(RenatusLogicIntentActions.ACTION_SITE_LOCATION_UPDATED);
                        getContext().sendBroadcast(locationUpdateIntent);
                        
                        if (ccu.size() > 0) {
                            if (!ccu.get("dis").toString().equals(ccuName) ||
                                !ccu.get("installerEmail").toString().equals(installerEmail) ||
                                !ccu.get("fmEmail").toString().equals(facilityManagerEmail) ) {
                                
                                CcuLog.e(TAG, "Update CCU "+ccu);
                                String ahuRef = ccu.get("ahuRef").toString();
                                CCUHsApi.getInstance().updateCCU(ccuName, installerEmail, ahuRef, facilityManagerEmail);
                                L.ccu().setCCUName(ccuName);
                            }
                            
                        } else {
                            String localId = CCUHsApi.getInstance().createCCU(ccuName, installerEmail, DiagEquip.getInstance().create(), facilityManagerEmail);
                            L.ccu().setCCUName(ccuName);
                            CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(localId));
                        }
                    }
                    L.saveCCUState();
                    CCUHsApi.getInstance().syncEntityTree();
                    Toast.makeText(getActivity(),"Edited details saved successfully",Toast.LENGTH_LONG).show();
                }
            }
        };

        btnEditSite.setOnClickListener(editSiteOnClickListener);
        imgEditSite.setOnClickListener(editSiteOnClickListener);
        if (site.size() > 0) {
            //if Site Exists
            String siteName = site.get("dis").toString();
            String siteAdd = site.get("geoAddr").toString();
            String siteCity = site.get("geoCity").toString();
            String siteState = site.get("geoState").toString();
            String siteCountry = site.get("geoCountry").toString();
            String siteZipCode = site.get("geoPostalCode").toString();
            String ccuFmEmail = site.get("fmEmail") != null ?  site.get("fmEmail").toString() : "";
            String ccuInstallerEmail = site.get("installerEmail") != null ? site.get("installerEmail").toString() : "";
            String siteTz = site.get("tz").toString();
            String siteOrg = site.get("organization") != null ? site.get("organization").toString(): "";

            mSiteName.setText(siteName);
            mStreetAdd.setText(siteAdd);
            mSiteCity.setText(siteCity);
            mSiteState.setText(siteState);
            mSiteCountry.setText(siteCountry);
            mSiteZip.setText(siteZipCode);
            mSiteOrg.setText(siteOrg);
            mSiteEmailId.setText(ccuFmEmail);
            mSiteInstallerEmailId.setText(ccuInstallerEmail);

            String[] tzIds = TimeZone.getAvailableIDs();
            for (String timeZone : tzIds) {
                if (timeZone.contains(siteTz)) {
                    mTimeZoneSelector.setSelection(timeZoneAdapter.getPosition(timeZone));
                    break;
                }
            }
            if (ccu.size() > 0) {
                //if CCU Exists
                String ccuName = ccu.get("dis").toString();
                ccuFmEmail = ccu.get("fmEmail").toString();
                ccuInstallerEmail = ccu.get("installerEmail") != null ? ccu.get("installerEmail").toString() : "";
                mSiteCCU.setText(ccuName);
                mSiteEmailId.setText(ccuFmEmail);
                mSiteInstallerEmailId.setText(ccuInstallerEmail);
            }

        }

        View.OnClickListener UnregisterSiteOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CCUHsApi.getInstance().isCCURegistered()){
                    showUnregisterAlertDialog();
                } else {
                    btnEditSite.setEnabled(true);
                    btnUnregisterSite.setEnabled(false);
                    //removeCCU api call would have already deleted this CCU entity from server
                    //We just need to delete it locally before creating a new CCU device.
                    CCUHsApi.getInstance().deleteEntityLocally(CCUHsApi.getInstance().getCcuRef().toString());

                    String facilityManagerEmail = mSiteEmailId.getText().toString();
                    String installerEmail = mSiteInstallerEmailId.getText().toString();
                    String ccuName = mSiteCCU.getText().toString();
                    HashMap diagEquip = CCUHsApi.getInstance().read("equip and diag");
                    String localId = CCUHsApi.getInstance().createCCU(ccuName, installerEmail,diagEquip.get("id").toString(),facilityManagerEmail);
                    L.ccu().setCCUName(ccuName);
                    CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(localId));
                    L.saveCCUState();
                    CCUHsApi.getInstance().syncEntityTree();

                    RxjavaUtil.executeBackgroundTask(
                            () -> ProgressDialogUtils.showProgressDialog(getActivity(), "Registering CCU..."),
                            () -> CCUHsApi.getInstance().registerCcu(installerEmail),
                            ()-> {
                                if (!CCUHsApi.getInstance().isCCURegistered()) {
                                    Toast.makeText(getActivity(), "CCU Registration Failed ", Toast.LENGTH_LONG).show();
                                } else {
                                    btnUnregisterSite.setText("Unregister");
                                    btnUnregisterSite.setEnabled(true);
                                    btnUnregisterSite.setTextColor(getResources().getColor(R.color.black_listviewtext));
                                    imgUnregisterSite.setColorFilter(getResources().getColor(R.color.black_listviewtext), PorterDuff.Mode.SRC_IN);
                                    setCompoundDrawableColor(btnUnregisterSite, R.color.black_listviewtext);
                                    Toast.makeText(getActivity(), "CCU Registered Successfully ", Toast.LENGTH_LONG).show();
                                    CCUHsApi.getInstance().resetSync();
                                }
                                ProgressDialogUtils.hideProgressDialog();
                            });
                }
            }
        };
        btnUnregisterSite.setOnClickListener(UnregisterSiteOnClickListener);
        imgUnregisterSite.setOnClickListener(UnregisterSiteOnClickListener);
        checkDebugPrepopulate();

        return rootView;
    }

    //1650 W 82nd St #200, Bloomington, MN 55431
    @SuppressLint("SetTextI18n")
    private void checkDebugPrepopulate() {
        //noinspection ConstantConditions
        if ((BuildConfig.BUILD_TYPE.equals("local") || BuildConfig.BUILD_TYPE.equals("dev"))
                && !BuildConfig.DEBUG_USER.isEmpty()) {

            String user = BuildConfig.DEBUG_USER;
            int index = user.indexOf('@');
            @SuppressWarnings("ConstantConditions")
            String ccuName = index > 0 ? user.substring(0, index) + " CCU" : "My CCU";
            mSiteCCU.setText(ccuName);
            mStreetAdd.setText("1650 W 82nd St #200");
            mSiteCity.setText("Bloomington");
            mSiteState.setText("MN");
            mSiteCountry.setText("USA");
            mSiteZip.setText("55431");
            mSiteOrg.setText("75F Dev");
            mSiteEmailId.setText(user);
            mSiteInstallerEmailId.setText(user);
        }
    }

    private void setCompoundDrawableColor(TextView textView, int color) {
        for (Drawable drawable : textView.getCompoundDrawablesRelative()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (drawable != null) {
                    drawable.setTint(getResources().getColor(color));
                }
            }
        }
    }

    private void showUnregisterAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setIcon(R.drawable.ic_warning);
        builder.setTitle("Unregister CCU");
        builder.setMessage("\n"+"Are you sure you want to unregister ccu?");
        builder.setCancelable(false);
        builder.setPositiveButton("YES", (dialog, which) -> {

            HashMap ccu = CCUHsApi.getInstance().read("device and ccu");
            String ahuRef = ccu.get("ahuRef").toString();
            String managerEmail = mSiteEmailId.getText().toString();
            String installerEmail = mSiteInstallerEmailId.getText().toString();
            String ccuName = mSiteCCU.getText().toString();
            CCUHsApi.getInstance().unRegisterCCU(ccuName, installerEmail, ahuRef, managerEmail);
            L.ccu().setCCUName(ccuName);

            ProgressDialogUtils.showProgressDialog(getActivity(), "UnRegistering CCU...");

            String ccuUID = ccu.get("id").toString();
            new Handler().postDelayed(() -> {
                removeCCU(ccuUID);

            }, 10000);

            dialog.dismiss();
        });

        builder.setNegativeButton("NO", (dialog, which) -> {

            dialog.dismiss();
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    private void removeCCU(String ccuId) {
        //We would consider CCU unregistered from this point itself.
        //Otherwise pubnubs generated due to unregister may arrive before the response itself and CCU
        //handling it can lead to inconsistencies.
        CCUHsApi.getInstance().setCcuUnregistered();

        AsyncTask<Void, Void, String> ccuUnReg = new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {

                HDictBuilder b = new HDictBuilder()
                        .add("ccuId", HRef.copy(ccuId));
                HDict[] dictArr = {b.toDict()};
                String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "removeCCU/", HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
                return response;
            }

            @Override
            protected void onPostExecute(String response) {
                super.onPostExecute(response);
                ProgressDialogUtils.hideProgressDialog();
                if( (response != null) && (!response.equals(""))){
                        HZincReader zReader = new HZincReader(response);
                        Iterator it = zReader.readGrid().iterator();
                        while (it.hasNext())
                        {
                            HRow row = (HRow) it.next();
                            String ccuId = row.get("removeCCUId").toString();
                            if (ccuId != null && ccuId != "")
                            {
                                btnUnregisterSite.setText("Register");
                                btnUnregisterSite.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
                                btnEditSite.setEnabled(false);
                                imgUnregisterSite.setColorFilter(CCUUiUtil.getPrimaryThemeColor(getContext()));
                                CCUHsApi.getInstance().setJwt("");
                                Toast.makeText(getActivity(), "CCU unregistered successfully " +ccuId, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(), "Failed to unregistered the CCU", Toast.LENGTH_LONG).show();
                            }
                        }
                } else {
                    Toast.makeText(getActivity(), "Failed to remove CCU", Toast.LENGTH_LONG).show();
                    CCUHsApi.getInstance().setCcuRegistered();
                }
            }
        };

        ccuUnReg.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * @param isEnable boolean to set views
     */
    private void enableViews(boolean isEnable) {
        mSiteName.setEnabled(isEnable);
        mTimeZoneSelector.setEnabled(isEnable);
        mStreetAdd.setEnabled(isEnable);
        mSiteCity.setEnabled(isEnable);
        mSiteState.setEnabled(isEnable);
        mSiteCountry.setEnabled(isEnable);
        mSiteZip.setEnabled(isEnable);
        mSiteCCU.setEnabled(isEnable);
        mSiteEmailId.setEnabled(isEnable);
        mSiteInstallerEmailId.setEnabled(isEnable);
        mSiteOrg.setEnabled(isEnable);
    }


    private void populateAndUpdateTimeZone() {
        
        timeZoneAdapter = new ArrayAdapter<String>(mContext, R.layout.spinner_item, getSupportedTimeZones());
        timeZoneAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mTimeZoneSelector.setAdapter(timeZoneAdapter);
        mTimeZoneSelector.setSelection(timeZoneAdapter.getPosition(TimeZone.getDefault().getID()));
        mTextTimeZone.setText(getString(R.string.input_timezone));
        mTextTimeZone.setTextColor(getResources().getColor(R.color.hint_color));

    }
    
    private ArrayList<String> getSupportedTimeZones() {
        String[] tzIds = TimeZone.getAvailableIDs();
        ArrayList<String> supportedTimeZones = new ArrayList<>();
        HashSet<String> regions = CCUHsApi.getInstance().getSupportedRegions();
        
        for (String tz : tzIds) {
            String[] parts = tz.split("/");
            String region = parts[0];
            if (regions.contains(region)) {
                supportedTimeZones.add(tz);
            }
        }
        
        return supportedTimeZones;
    }

    private class EditTextWatcher implements TextWatcher {

        private View view;

        private EditTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.editSitename:
                    if (mSiteName.getText().length() > 0) {
                        mTextInputSitename.setErrorEnabled(true);
                        mTextInputSitename.setError(getString(R.string.input_sitename));
                        mSiteName.setError(null);
                    } else {
                        mTextInputSitename.setError(getString(R.string.hint_sitename));
                    }

                case R.id.editStreetAdd:
                    if (mStreetAdd.getText().length() > 0) {
                        mTextInputStreetAdd.setErrorEnabled(true);
                        mTextInputStreetAdd.setError(" " + getString(R.string.input_streetadd));
                        mStreetAdd.setError(null);
                    } else {
                        mTextInputStreetAdd.setError("");
                    }

                case R.id.editCity:
                    if (mSiteCity.getText().length() > 0) {
                        mTextInputCity.setErrorEnabled(true);
                        mTextInputCity.setError(getString(R.string.input_city));
                        mSiteCity.setError(null);
                    } else {
                        mTextInputCity.setError("");
                    }

                case R.id.editState:
                    if (mSiteState.getText().length() > 0) {
                        mTextInputState.setErrorEnabled(true);
                        mTextInputState.setError(getString(R.string.input_state));
                        mSiteState.setError(null);
                    } else {
                        mTextInputState.setError("");
                    }

                case R.id.editCountry:
                    if (mSiteCountry.getText().length() > 0) {
                        mTextInputCountry.setErrorEnabled(true);
                        mTextInputCountry.setError(getString(R.string.input_country));
                        mSiteCountry.setError(null);
                    } else {
                        mTextInputCountry.setError("");
                    }

                case R.id.editZip:
                    if (mSiteZip.getText().length() > 0) {
                        mTextInputZip.setErrorEnabled(true);
                        mTextInputZip.setError(getString(R.string.input_zip));
                        mSiteZip.setError(null);
                    } else {
                        mTextInputZip.setError("");
                    }

                case R.id.editCCU:
                    if (mSiteCCU.getText().length() > 0) {
                        mTextInputCCU.setErrorEnabled(true);
                        mTextInputCCU.setError(getString(R.string.input_ccuname));
                        mSiteCCU.setError(null);
                    } else {
                        mTextInputCCU.setError("");
                    }
                case R.id.editFacilityOrganization:
                    if (mSiteOrg.getText().length() > 0) {
                        mTextInputOrg.setErrorEnabled(true);
                        mTextInputOrg.setError(getString(R.string.input_facilityorg));
                        mSiteOrg.setError(null);
                    } else {
                        mTextInputOrg.setError("");
                    }
                case R.id.editFacilityEmail:
                    if (mSiteEmailId.getText().length() > 0) {
                        mTextInputEmail.setErrorEnabled(true);
                        mTextInputEmail.setError(getString(R.string.input_facilityemail));
                        mSiteEmailId.setError(null);
                        String emailID = mSiteEmailId.getText().toString();
                        if (Patterns.EMAIL_ADDRESS.matcher(emailID).matches()) {

                        } else {
                            mSiteEmailId.setError("Invalid Email Address");
                        }
                    } else {
                        mTextInputEmail.setError("");
                        mSiteEmailId.setError(null);
                    }
                case R.id.editInstallerEmail:
                    if (mSiteInstallerEmailId.getText().length() > 0) {
                        mTextInputInstallerEmail.setErrorEnabled(true);
                        mTextInputInstallerEmail.setError(getString(R.string.input_installer_email));
                        mSiteInstallerEmailId.setError(null);
                        String emailID = mSiteInstallerEmailId.getText().toString();
                        if (Patterns.EMAIL_ADDRESS.matcher(emailID).matches()) {

                        } else {
                            mSiteInstallerEmailId.setError("Invalid Email Address");
                        }
                    } else {
                        mTextInputInstallerEmail.setError("");
                        mSiteInstallerEmailId.setError(null);
                    }
            }
        }
    }


    public boolean validateEditText(int[] ids) {
        boolean isEmpty = false;

        for (int id : ids) {
            EditText et = (EditText) getView().findViewById(id);

            if (TextUtils.isEmpty(et.getText().toString())) {
                et.setError("Must enter Value");
                isEmpty = true;
            }
        }

        return isEmpty;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
    /* This site never existed we are creating a new orphaned site. */

    public String saveSite(String siteName, String siteCity, String siteZip, String geoAddress, String siteState, String siteCountry, String org, String installer, String fcManager) {
        String tzID = mTimeZoneSelector.getSelectedItem().toString();
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.setTimeZone(tzID);

        Site s75f = new Site.Builder()
                .setDisplayName(siteName)
                .addMarker("site")
                .setGeoCity(siteCity)
                .setGeoState(siteState)
                .setTz(tzID.substring(tzID.lastIndexOf("/") + 1))
                .setGeoZip(siteZip)
                .setGeoCountry(siteCountry)
                .setOrgnization(org)
                .setInstaller(installer)
                .setFcManager(fcManager)
                .setGeoAddress(geoAddress)
                .setGeoFence("2.0")
                .setArea(10000).build();

        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        String localSiteId = ccuHsApi.addSite(s75f);
        CCUHsApi.getInstance().setPrimaryCcu(true);
        BuildingTuners.getInstance().updateBuildingTuners();
        //SystemEquip.getInstance();
        Log.i(TAG, "LocalSiteID: " + localSiteId);
        ccuHsApi.log();
        L.ccu().systemProfile = new DefaultSystem();
        prefs.setString("SITE_ID", localSiteId);
        return localSiteId;
    }

    public void updateSite(String siteName, String siteCity, String siteZip, String geoAddress, String siteState, String siteCountry, String siteId, String org, String installer, String fcManager) {

        String tzID = mTimeZoneSelector.getSelectedItem().toString();
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.setTimeZone(tzID);
    
        HashMap site = CCUHsApi.getInstance().read("site");
        String curTz = site.get("tz").toString();
        
        Site s75f = new Site.Builder()
                .setDisplayName(siteName)
                .addMarker("site")
                .setGeoCity(siteCity)
                .setGeoState(siteState)
                .setTz(tzID.substring(tzID.lastIndexOf("/") + 1))
                .setGeoZip(siteZip)
                .setGeoCountry(siteCountry)
                .setGeoAddress(geoAddress)
                .setOrgnization(org)
                .setInstaller(installer)
                .setFcManager(fcManager)
                .setArea(10000).build();

        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
    
        Site currentSite = new Site.Builder().setHashMap(site).build();
        if (currentSite.equals(s75f)) {
            CcuLog.d(TAG, "Update Site not detected : return");
            return;
        }
        
        ccuHsApi.updateSite(s75f, siteId);
        CcuLog.d(TAG, "Update Site curTz "+curTz+" newTz "+s75f.getTz());
        if (!curTz.equals(s75f.getTz())) {
            CCUHsApi.getInstance().updateTimeZone(s75f.getTz());
        }
        BuildingTuners.getInstance();
        ccuHsApi.log();
      //  L.ccu().systemProfile = new DefaultSystem();
        ccuHsApi.saveTagsData();

    }

    private void goTonext() {
        prefs.setBoolean("CCU_SETUP", false);
        ((FreshRegistration) getActivity()).selectItem(4);
    }

    private Spanned getHTMLCodeForHints( int resource){
        return Html.fromHtml("<small><font color='#E24301'>" + getString(R.string.mandatory)
                + " " + "</font><?small>" + "<big><font color='#99000000'>" + getString(resource) + "</font></big>");
    }
}
