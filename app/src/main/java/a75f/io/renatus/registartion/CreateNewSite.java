package a75f.io.renatus.registartion;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
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

import org.json.JSONException;
import org.json.JSONObject;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.diag.DiagEquip;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.renatus.R;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;

public class CreateNewSite extends Fragment {
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
    TextView btnEditSite;
    TextView btnUnregisterSite;
    Context mContext;
    LinearLayout btnSetting;
    Prefs prefs;
    private boolean isFreshRegister;
    private static final String TAG = CreateNewSite.class.getSimpleName();

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
        btnUnregisterSite = rootView.findViewById(R.id.btnUnregisterSite);

        if (isFreshRegister) {
            mNext.setVisibility(View.VISIBLE);
            btnSetting.setVisibility(View.GONE);
        } else {
            mNext.setVisibility(View.GONE);
            btnSetting.setVisibility(View.VISIBLE);
            enableViews(false);
        }
        populateAndUpdateTimeZone();

        mSiteName.setHint(Html.fromHtml("<small><font color='#E24301'>" + getString(R.string.mandatory) + " " + "</font><?small>" + "<big><font color='#99000000'>" + getString(R.string.input_sitename) + "</font></big>"));
        mStreetAdd.setHint(Html.fromHtml("<small><font color='#E24301'>" + getString(R.string.mandatory) + " " + "</font><?small>" + "<big><font color='#99000000'>" + getString(R.string.input_streetadd) + "</font></big>"));
        mSiteCity.setHint(Html.fromHtml("<small><font color='#E24301'>" + getString(R.string.mandatory) + " " + "</font><?small>" + "<big><font color='#99000000'>" + getString(R.string.input_city) + "</font></big>"));
        mSiteState.setHint(Html.fromHtml("<small><font color='#E24301'>" + getString(R.string.mandatory) + " " + "</font><?small>" + "<big><font color='#99000000'>" + getString(R.string.input_state) + "</font></big>"));
        mSiteCountry.setHint(Html.fromHtml("<small><font color='#E24301'>" + getString(R.string.mandatory) + " " + "</font><?small>" + "<big><font color='#99000000'>" + getString(R.string.input_country) + "</font></big>"));
        mSiteZip.setHint(Html.fromHtml("<small><font color='#E24301'>" + getString(R.string.mandatory) + " " + "</font><?small>" + "<big><font color='#99000000'>" + getString(R.string.input_zip) + "</font></big>"));
        mSiteCCU.setHint(Html.fromHtml("<small><font color='#E24301'>" + getString(R.string.mandatory) + " " + "</font><?small>" + "<big><font color='#99000000'>" + getString(R.string.input_ccuname) + "</font></big>"));
        mSiteEmailId.setHint(Html.fromHtml("<small><font color='#E24301'>" + getString(R.string.mandatory) + " " + "</font><?small>" + "<big><font color='#99000000'>" + getString(R.string.input_facilityemail) + "</font></big>"));
        mSiteOrg.setHint(Html.fromHtml("<small><font color='#E24301'>" + getString(R.string.mandatory) + " " + "</font><?small>" + "<big><font color='#99000000'>" + getString(R.string.input_facilityorg) + "</font></big>"));
        mSiteInstallerEmailId.setHint(Html.fromHtml("<small><font color='#E24301'>" + getString(R.string.mandatory) + " " + "</font><?small>" + "<big><font color='#99000000'>" + getString(R.string.input_installer_email) + "</font></big>"));

        if (prefs.getBoolean("registered")) {
            btnUnregisterSite.setText("UnRegister");
            btnUnregisterSite.setTextColor(getResources().getColor(R.color.black_listviewtext));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                setCompoundDrawableColor(btnUnregisterSite, R.color.black_listviewtext);
            }
            btnEditSite.setEnabled(true);
        } else {
            btnEditSite.setEnabled(false);
            btnUnregisterSite.setText("Register");
            btnUnregisterSite.setTextColor(getResources().getColor(R.color.accent));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                setCompoundDrawableColor(btnUnregisterSite, R.color.accent);
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
                if (!validateEditText(mandotaryIds) && Patterns.EMAIL_ADDRESS.matcher(mSiteEmailId.getText().toString()).matches()) {
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

                    prefs.setBoolean("registered", true);
                    ProgressDialogUtils.showProgressDialog(getActivity(),"Adding New Site...");
                    if (site.size() > 0) {
                        String siteId = site.get("id").toString();
                        String geoFence = site.get("geoFence") != null ? site.get("geoFence").toString() : "2.0";
                        updateSite(siteName, siteCity, siteZip, siteAddress, siteState, siteCountry, siteId,installerOrg, installerEmail, managerEmail,geoFence);
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
                            ProgressDialogUtils.hideProgressDialog();
                            goTonext();
                        }
                    }, 20000);
                }
            }
        });

        btnEditSite.setOnClickListener(view -> {
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
                    String managerEmail = mSiteEmailId.getText().toString();
                    String installerOrg = mSiteOrg.getText().toString();
                    String ccuName = mSiteCCU.getText().toString();

                    if (site.size() > 0) {
                        String siteId = site.get("id").toString();
                        String geoFence = site.get("geoFence") != null ? site.get("geoFence").toString() : "2.0";
                        updateSite(siteName, siteCity, siteZip, siteAddress, siteState, siteCountry, siteId, installerOrg, installerEmail, managerEmail,geoFence);
                    } else {
                        saveSite(siteName, siteCity, siteZip, siteAddress, siteState, siteCountry,installerOrg, installerEmail,managerEmail);
                    }

                    if (ccu.size() > 0) {
                        String ahuRef = ccu.get("ahuRef").toString();
                        CCUHsApi.getInstance().updateCCU(ccuName, installerEmail, ahuRef, managerEmail);
                        L.ccu().setCCUName(ccuName);
                    } else {
                        String localId = CCUHsApi.getInstance().createCCU(ccuName, installerEmail, DiagEquip.getInstance().create(), managerEmail);
                        L.ccu().setCCUName(ccuName);
                        CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(localId));
                    }
                    try {
                        JSONObject ccuRegInfo = new JSONObject();
                        if(site.size() > 0) {
                            String siteGUID = CCUHsApi.getInstance().getGUID(site.get("id").toString());
                            JSONObject locInfo = new JSONObject();
                            locInfo.put("geoCity", siteCity);
                            locInfo.put("geoCountry", siteCountry);
                            locInfo.put("geoState", siteState);
                            locInfo.put("geoAddr", siteAddress);
                            locInfo.put("geoPostalCode", siteZip);
                            ccuRegInfo.put("organization", installerOrg);
                            ccuRegInfo.put("siteName",siteName);
                            ccuRegInfo.put("siteId",  siteGUID);
                            if(ccu.size() > 0) {
                                String ccuGUID = CCUHsApi.getInstance().getGUID(ccu.get("id").toString());
                                ccuRegInfo.put("deviceId", ccuGUID);
                                ccuRegInfo.put("deviceName", ccuName);
                                ccuRegInfo.put("facilityManagerEmail", managerEmail);
                                ccuRegInfo.put("installerEmail", installerEmail);
                                ccuRegInfo.put("locationDetails", locInfo);

                                updateCCURegistrationInfo(ccuRegInfo.toString());
                            }
                        }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }
                L.saveCCUState();
                CCUHsApi.getInstance().syncEntityTree();
                Toast.makeText(getActivity(),"Edited details saved successfully",Toast.LENGTH_LONG).show();
            }
        });


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

        btnUnregisterSite.setOnClickListener(view -> {
            if (prefs.getBoolean("registered")){
                showUnregisterAlertDialog();
            } else {
                btnEditSite.setEnabled(true);
                CCUHsApi.getInstance().deleteEntity(CCUHsApi.getInstance().getCcuId().toString());

                ProgressDialogUtils.showProgressDialog(getActivity(), "Registering CCU...");
                prefs.setBoolean("registered", true);

                String managerEmail = mSiteEmailId.getText().toString();
                String installerEmail = mSiteInstallerEmailId.getText().toString();
                String ccuName = mSiteCCU.getText().toString();
                HashMap diagEquip = CCUHsApi.getInstance().read("equip and diag");
                String localId = CCUHsApi.getInstance().createCCU(ccuName, installerEmail,diagEquip.get("id").toString(),managerEmail);
                L.ccu().setCCUName(ccuName);
                CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(localId));
                L.saveCCUState();
                CCUHsApi.getInstance().syncEntityTree();

                new Handler().postDelayed(() -> {
                    HashMap newCCU = CCUHsApi.getInstance().read("device and ccu");
                    JSONObject ccuRegInfo = new JSONObject();
                    String ccuGUID = CCUHsApi.getInstance().getGUID(newCCU.get("id").toString());
                    String siteName = site.get("dis").toString();
                    String siteAddress = site.get("geoAddr").toString();
                    String siteCity = site.get("geoCity").toString();
                    String siteState = site.get("geoState").toString();
                    String siteCountry = site.get("geoCountry").toString();
                    String siteZip = site.get("geoPostalCode").toString();
                    String siteOrg = site.get("organization") != null ? site.get("organization").toString(): "";
                    String siteGUID = CCUHsApi.getInstance().getGUID(site.get("id").toString());
                    try {
                        JSONObject locInfo = new JSONObject();
                        locInfo.put("geoCity", siteCity);
                        locInfo.put("geoCountry", siteCountry);
                        locInfo.put("geoState", siteState);
                        locInfo.put("geoAddr", siteAddress);
                        locInfo.put("geoPostalCode", siteZip);

                        ccuRegInfo.put("organization", siteOrg);
                        ccuRegInfo.put("siteName",siteName);
                        ccuRegInfo.put("siteId",  siteGUID);
                        ccuRegInfo.put("deviceId", ccuGUID);
                        ccuRegInfo.put("deviceName", ccuName);
                        ccuRegInfo.put("facilityManagerEmail", managerEmail);
                        ccuRegInfo.put("installerEmail", installerEmail);
                        ccuRegInfo.put("locationDetails", locInfo);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    updateCCURegistrationInfo(ccuRegInfo.toString());
                   CCUHsApi.getInstance().resetSync();
                }, 25000);
            }

        });

        return rootView;
    }

    private void setCompoundDrawableColor(TextView textView, int color) {
        for (Drawable drawable : textView.getCompoundDrawablesRelative()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (drawable != null)
                {
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

            String ccuGUID = CCUHsApi.getInstance().getGUID(ccu.get("id").toString());
            new Handler().postDelayed(() -> {
                removeCCU(ccuGUID);

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
                                btnUnregisterSite.setTextColor(getResources().getColor(R.color.accent));
                                btnEditSite.setEnabled(false);
                                setCompoundDrawableColor(btnUnregisterSite, R.color.accent);

                                prefs.setString("token","");
                                prefs.setBoolean("registered", false);
                                Toast.makeText(getActivity(), "CCU removed Successfully " +ccuId, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(), "Fails to remove CCU", Toast.LENGTH_LONG).show();
                            }
                        }
                }else {
                    Toast.makeText(getActivity(), "Fails to remove CCU", Toast.LENGTH_LONG).show();
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

        String[] tzIds = TimeZone.getAvailableIDs();
        Log.i("timeZones", "time:" + tzIds.toString());

        timeZoneAdapter = new ArrayAdapter<String>(mContext, R.layout.spinner_item, tzIds);
        timeZoneAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mTimeZoneSelector.setAdapter(timeZoneAdapter);
        mTimeZoneSelector.setSelection(timeZoneAdapter.getPosition(TimeZone.getDefault().getID()));
        mTextTimeZone.setText(getString(R.string.input_timezone));
        mTextTimeZone.setTextColor(getResources().getColor(R.color.hint_color));

    }
    private void retryRegistrationInfo() {
        AsyncTask<Void, Void, String> updateCCUReg = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {

                //TODO upload CCU Registration details to Server for user management here //KUMAR 30/01/2020
                String response = "";
                try {

                    HashMap ccu = CCUHsApi.getInstance().read("device and ccu");
                    HashMap site = CCUHsApi.getInstance().read("site");
                    if(site.size() > 0) {
                        String siteGUID = CCUHsApi.getInstance().getGUID(site.get("id").toString());
                        JSONObject ccuRegInfo = new JSONObject();
                        ccuRegInfo.put("siteId", siteGUID);
                        ccuRegInfo.put("siteName", site.get("dis").toString());
                        JSONObject locInfo = new JSONObject();
                        locInfo.put("geoCity", site.get("geoCity").toString());
                        locInfo.put("geoCountry", site.get("geoCountry").toString());
                        locInfo.put("geoState", site.get("geoState").toString());
                        locInfo.put("geoAddr", site.get("geoAddr").toString());
                        locInfo.put("geoPostalCode", site.get("geoPostalCode").toString());
                        if(site.get("organization") != null)
                            ccuRegInfo.put("organization", site.get("organization").toString());
                        if(ccu.size() > 0) {
                            String ccuGUID = CCUHsApi.getInstance().getGUID(ccu.get("id").toString());
                            ccuRegInfo.put("deviceId", ccuGUID);
                            ccuRegInfo.put("deviceName", ccu.get("dis").toString());
                            ccuRegInfo.put("facilityManagerEmail", ccu.get("fmEmail").toString());
                            if (ccu.get("installerEmail") != null) {
                                ccuRegInfo.put("installerEmail", ccu.get("installerEmail").toString());
                            }
                            ccuRegInfo.put("locationDetails", locInfo);


                            response = HttpUtil.executeJSONPost(CCUHsApi.getInstance().getAuthenticationUrl() + "api/v1/device/register", ccuRegInfo.toString(),"");
                            Log.d("CCURegistration", " Response : " + response);
                        }
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
                return response;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                if( (result != null) && (!result.equals(""))){

                    try {
                        JSONObject resString = new JSONObject(result);
                        if(resString.getBoolean("success")){

                            Toast.makeText(getActivity(), "CCU Registered Successfully "+resString.getString("deviceId"), Toast.LENGTH_LONG).show();
                        }else
                            Toast.makeText(getActivity(), "CCU Registration is not Successful "+resString.getString("deviceId"), Toast.LENGTH_LONG).show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else {
                    retryRegistrationInfo();
                    //Toast.makeText(getActivity(), "CCU Registration is not Successful", Toast.LENGTH_LONG).show();
                }
            }
        };

        updateCCUReg.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void updateCCURegistrationInfo(final String ccuRegInfo) {
        AsyncTask<Void, Void, String> updateCCUReg = new AsyncTask<Void, Void, String>() {


            @Override
            protected String doInBackground(Void... voids) {

                Log.d("CCURegInfo","createNewSite Edit backgroundtask="+ccuRegInfo);
                return  HttpUtil.executeJSONPost(CCUHsApi.getInstance().getAuthenticationUrl()+"api/v1/device/register",ccuRegInfo, "");
            }

            @Override
            protected void onPostExecute(String result) {

                Log.d("CCURegInfo","createNewSite Edit onPostExecute="+result);
                ProgressDialogUtils.hideProgressDialog();
                if((result != null) && (!result.equals(""))){

                    try {
                        JSONObject resString = new JSONObject(result);
                        if(resString.getBoolean("success")){
                            btnUnregisterSite.setText("UnRegister");
                            btnUnregisterSite.setTextColor(getResources().getColor(R.color.black_listviewtext));
                            setCompoundDrawableColor(btnUnregisterSite, R.color.black_listviewtext);

                            prefs.setString("token",resString.getString("token"));
                            Toast.makeText(getActivity(), "CCU Registered Successfully "+resString.getString("deviceId"), Toast.LENGTH_LONG).show();
                        }else
                            Toast.makeText(getActivity(), "CCU Registration is not Successful "+resString.getString("deviceId"), Toast.LENGTH_LONG).show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else {
                    retryRegistrationInfo();
                    //Toast.makeText(getActivity(), "CCU Registration is not Successful", Toast.LENGTH_LONG).show();
                }
            }
        };

        updateCCUReg.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
                .addMarker("orphan")
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
        Log.i(TAG, "LocalSiteID: " + localSiteId + " tz " + s75f.getTz());
        BuildingTuners.getInstance();
        //SystemEquip.getInstance();
        Log.i(TAG, "LocalSiteID: " + localSiteId);
        ccuHsApi.log();
        L.ccu().systemProfile = new DefaultSystem();
        ccuHsApi.saveTagsData();

        prefs.setString("SITE_ID", localSiteId);
        return localSiteId;
    }

    public void updateSite(String siteName, String siteCity, String siteZip, String geoAddress, String siteState, String siteCountry, String siteId, String org, String installer, String fcManager,String geoFence) {

        String tzID = mTimeZoneSelector.getSelectedItem().toString();
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.setTimeZone(tzID);

        Site s75f = new Site.Builder()
                .setDisplayName(siteName)
                .addMarker("site")
                .addMarker("orphan")
                .setGeoCity(siteCity)
                .setGeoState(siteState)
                .setTz(tzID.substring(tzID.lastIndexOf("/") + 1))
                .setGeoZip(siteZip)
                .setGeoCountry(siteCountry)
                .setGeoAddress(geoAddress)
                .setGeoFence(geoFence)
                .setOrgnization(org)
                .setInstaller(installer)
                .setFcManager(fcManager)
                .setArea(10000).build();

        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        ccuHsApi.updateSite(s75f, siteId);
        BuildingTuners.getInstance();
        ccuHsApi.log();
      //  L.ccu().systemProfile = new DefaultSystem();
        ccuHsApi.saveTagsData();

    }

    private void goTonext() {
        //Intent i = new Intent(mContext, RegisterGatherCCUDetails.class);
        //startActivity(i);
        prefs.setBoolean("CCU_SETUP", false);
        ((FreshRegistration) getActivity()).selectItem(4);
    }

}
