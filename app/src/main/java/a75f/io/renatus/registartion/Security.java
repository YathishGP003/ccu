package a75f.io.renatus.registartion;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.diag.PasswordUtils;
import a75f.io.renatus.R;
import a75f.io.renatus.util.Prefs;
import butterknife.BindView;
import butterknife.ButterKnife;

public class Security extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    @BindView(R.id.textPassforZone)
    TextView             mZonePass;
    @BindView(R.id.textTapZone)
    TextView             mZoneTap;
    @BindView(R.id.textPassforSystem)
    TextView             mSystemPass;
    @BindView(R.id.textTapSystem)
    TextView             mSystemtap;
    @BindView(R.id.textPassforBuild)
    TextView             mBuildingPass;
    @BindView(R.id.textTapBuild)
    TextView             mBuildingTap;
    @BindView(R.id.textPassforSetup)
    TextView             mSetupPass;
    @BindView(R.id.textTapSetup)
    TextView             mSetupTap;
    @BindView(R.id.toggleZonePass)
    ToggleButton             mZonePassTb;
    @BindView(R.id.toggleSystemPass)
    ToggleButton             mSystemPassTb;
    @BindView(R.id.toggleBuildingPass)
    ToggleButton             mBuildingPassTb;
    @BindView(R.id.toggleSetupPass)
    ToggleButton             mSetupPassTb;
    @BindView(R.id.buttonNext)
    Button             mNext;
    Context              mContext;
    Prefs                prefs;
    private static final String TAG = Security.class.getSimpleName();

    String mTitle;
    String mKey;
    String mSetKey;
    String zonePassword;
    String systemPassword;
    String buildingPassword;
    String setupPassword;
    private boolean isFreshRegister;
    public Security() {
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
    public static Security newInstance(String param1, String param2) {
        Security fragment = new Security();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //((FreshRegistration)getActivity()).showIcons(false);
        View rootView = inflater.inflate(R.layout.fragment_security, container, false);
        ButterKnife.bind(this, rootView);

        mContext = getContext().getApplicationContext();
        prefs = new Prefs(mContext);
        isFreshRegister = getActivity() instanceof FreshRegistration;


        zonePassword = prefs.getString(getString(R.string.ZONE_SETTINGS_PASSWORD_KEY));
        if(zonePassword.length()>0) {
            mZoneTap.setText(getString(R.string.taptochange));
        }else
        {
            mZoneTap.setText(getString(R.string.taptoset));
        }

        systemPassword = prefs.getString(getString(R.string.SYSTEM_SETTINGS_PASSWORD_KEY));
        if(systemPassword.length()>0) {
            mSystemtap.setText(getString(R.string.taptochange));
        }else
        {
            mSystemtap.setText(getString(R.string.taptoset));
        }

        buildingPassword = prefs.getString(getString(R.string.BUILDING_SETTINGS_PASSWORD_KEY));
        if(buildingPassword.length()>0) {
            mBuildingTap.setText(getString(R.string.taptochange));
        }else
        {
            mBuildingTap.setText(getString(R.string.taptoset));
        }

        setupPassword = prefs.getString(getString(R.string.USE_SETUP_PASSWORD_KEY));
        if(setupPassword.length()>0) {
            mSetupTap.setText(getString(R.string.taptochange));
        }else
        {
            mSetupTap.setText(getString(R.string.taptoset));
        }

        mZonePassTb.setChecked(prefs.getBoolean(getString(R.string.SET_ZONE_PASSWORD)));
        mSystemPassTb.setChecked(prefs.getBoolean(getString(R.string.SET_SYSTEM_PASSWORD)));
        mBuildingPassTb.setChecked(prefs.getBoolean(getString(R.string.SET_BUILDING_PASSWORD)));
        mSetupPassTb.setChecked(prefs.getBoolean(getString(R.string.SET_SETUP_PASSWORD)));

        mZonePass.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mTitle = getString(R.string.zonesettings);
                mKey = getString(R.string.ZONE_SETTINGS_PASSWORD_KEY);
                mSetKey = getString(R.string.SET_ZONE_PASSWORD);
                showCustomDialog(mTitle,mKey,mSetKey,mZonePassTb,mZoneTap);
            }
        });

        mSystemPass.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mTitle = getString(R.string.systemsettings);
                mKey = getString(R.string.SYSTEM_SETTINGS_PASSWORD_KEY);
                mSetKey = getString(R.string.SET_SYSTEM_PASSWORD);
                showCustomDialog(mTitle,mKey,mSetKey,mSystemPassTb,mSystemtap);
            }
        });


        mBuildingPass.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mTitle = getString(R.string.buildingchange);
                mKey = getString(R.string.BUILDING_SETTINGS_PASSWORD_KEY);
                mSetKey = getString(R.string.SET_BUILDING_PASSWORD);
                showCustomDialog(mTitle,mKey,mSetKey,mBuildingPassTb,mBuildingTap);
            }
        });


        mSetupPass.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mTitle = getString(R.string.setupchange);
                mKey = getString(R.string.USE_SETUP_PASSWORD_KEY);
                mSetKey = getString(R.string.SET_SETUP_PASSWORD);
                showCustomDialog(mTitle,mKey,mSetKey,mSetupPassTb,mSetupTap);
            }
        });


        mZonePassTb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mTitle = getString(R.string.zonesettings);
                mKey = getString(R.string.ZONE_SETTINGS_PASSWORD_KEY);
                mSetKey = getString(R.string.SET_ZONE_PASSWORD);
                zonePassword = prefs.getString(mKey);
                if(isChecked){
                    if(zonePassword.length()>0)
                    {
                        prefs.setBoolean(mSetKey, true);
                        mZoneTap.setText(getString(R.string.taptochange));
                    }
                    else {
                        mTitle = getString(R.string.zonesettings);
                        mKey = getString(R.string.ZONE_SETTINGS_PASSWORD_KEY);
                        showCustomDialog(mTitle,mKey,mSetKey,mZonePassTb,mZoneTap);
                    }
                }
                else{
                    prefs.setBoolean(mSetKey, false);
                }
            }
        });


        mSystemPassTb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mTitle = getString(R.string.systemsettings);
                mKey = getString(R.string.SYSTEM_SETTINGS_PASSWORD_KEY);
                mSetKey = getString(R.string.SET_SYSTEM_PASSWORD);
                systemPassword = prefs.getString(mKey);
                if(isChecked){
                    if(systemPassword.length()>0)
                    {
                        prefs.setBoolean(mSetKey, true);
                        mSystemtap.setText(getString(R.string.taptochange));
                    }
                    else {
                        mTitle = getString(R.string.systemsettings);
                        mKey = getString(R.string.SYSTEM_SETTINGS_PASSWORD_KEY);
                        showCustomDialog(mTitle,mKey,mSetKey,mSystemPassTb,mSystemtap);
                    }
                }
                else{
                    prefs.setBoolean(mSetKey, false);
                }
            }
        });


        mBuildingPassTb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mTitle = getString(R.string.buildingchange);
                mKey = getString(R.string.BUILDING_SETTINGS_PASSWORD_KEY);
                mSetKey = getString(R.string.SET_BUILDING_PASSWORD);
                buildingPassword = prefs.getString(mKey);
                if(isChecked){
                    if(buildingPassword.length()>0)
                    {
                        prefs.setBoolean(mSetKey, true);
                        mBuildingTap.setText(getString(R.string.taptochange));
                    }
                    else {
                        mTitle = getString(R.string.buildingchange);
                        mKey = getString(R.string.BUILDING_SETTINGS_PASSWORD_KEY);
                        showCustomDialog(mTitle,mKey,mSetKey,mBuildingPassTb,mBuildingTap);
                    }
                }
                else{
                    prefs.setBoolean(mSetKey, false);
                }
            }
        });


        mSetupPassTb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mTitle = getString(R.string.setupchange);
                mKey = getString(R.string.USE_SETUP_PASSWORD_KEY);
                mSetKey = getString(R.string.SET_SETUP_PASSWORD);
                setupPassword = prefs.getString(mKey);
                if(isChecked){
                    if(setupPassword.length()>0)
                    {
                        prefs.setBoolean(mSetKey, true);
                        mSetupTap.setText(getString(R.string.taptochange));
                    }
                    else {
                        mTitle = getString(R.string.setupchange);
                        mKey = getString(R.string.SETUP_PASSWORD_KEY);
                        showCustomDialog(mTitle,mKey,mSetKey,mSetupPassTb,mSetupTap);
                    }
                }
                else{
                    prefs.setBoolean(mSetKey, false);
                }
            }
        });



        mNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                goTonext();
            }
        });

        if (isFreshRegister)  mNext.setVisibility(View.VISIBLE); else mNext.setVisibility(View.GONE);


        return rootView;
    }

    private void showCustomDialog(String title, String key, String setKey, ToggleButton togglePassword, TextView textViewTap) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        View layoutView = getLayoutInflater().inflate(R.layout.dialog_password, null);
        dialogBuilder.setView(layoutView);
        Dialog alertDialog = dialogBuilder.create();


        TextView mTitlePass = layoutView.findViewById(R.id.textPasswordFor);
        EditText mCurrentPass = layoutView.findViewById(R.id.editCurrentPass);
        EditText mNewPass = layoutView.findViewById(R.id.editNewPass);
        EditText mConfirmPass = layoutView.findViewById(R.id.editRepeatPass);
        Button buttonSetNew = layoutView.findViewById(R.id.button_setpass);
        Button buttonClear = layoutView.findViewById(R.id.button_cancel);
        ImageView mClose = layoutView.findViewById(R.id.imageViewCancel);

        String oldPass = prefs.getString(mKey);
        if(oldPass.length()>0)
        {
            mCurrentPass.setVisibility(View.VISIBLE);
            buttonClear.setVisibility(View.VISIBLE);
        }
        else {
            mCurrentPass.setVisibility(View.GONE);
            buttonClear.setVisibility(View.GONE);
        }

        buttonSetNew.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                String password = mNewPass.getText().toString().trim();
                String confirmpassword = mConfirmPass.getText().toString().trim();
                if(oldPass.length()>0)
                {
                    if (oldPass.equals(mCurrentPass.getText().toString())) {
                        if (password.length() > 0 && confirmpassword.length() > 0) {
                            if (password.equals(confirmpassword)) {
                                prefs.setBoolean(setKey, true);
                                prefs.setString(key, password);
                                togglePassword.setChecked(true);
                                alertDialog.dismiss();
                                saveToDiag(key, password);
                                textViewTap.setText(getString(R.string.taptochange));
                                Toast.makeText(mContext, "Password is set for " + title, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(mContext, "Password is not same!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(mContext, "Password fields cannot be empty!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else{
                        Toast.makeText(mContext, "Current Password is Invalid!", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    if (password.length() > 0 && confirmpassword.length() > 0) {
                        if (password.equals(confirmpassword)) {
                            prefs.setBoolean(setKey, true);
                            prefs.setString(key, password);
                            saveToDiag(key, password);
                            togglePassword.setChecked(true);
                            alertDialog.dismiss();
                            Toast.makeText(mContext, "Password is set for " + title, Toast.LENGTH_SHORT).show();
                            textViewTap.setText(getString(R.string.taptochange));
                        } else {
                            Toast.makeText(mContext, "Password is not same!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(mContext, "Password fields cannot be empty!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        buttonClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(oldPass.length()>0) {
                    if (oldPass.equals(mCurrentPass.getText().toString()))
                    {
                        prefs.setBoolean(setKey, false);
                        prefs.setString(key, "");
                        togglePassword.setChecked(false);
                        alertDialog.dismiss();
                        textViewTap.setText(getString(R.string.taptoset));
                        Toast.makeText(mContext, title+" Password is cleared", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mContext, "Invalid Password!", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(mContext, "Current Password Cannot be empty!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(oldPass.length()>0)
                {
                    textViewTap.setText(getString(R.string.taptochange));
                }else {
                    textViewTap.setText(getString(R.string.taptoset));
                }
                togglePassword.setChecked(false);
                prefs.setBoolean(setKey, false);
                alertDialog.dismiss();
            }
        });

        mTitlePass.setText(title);

        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        alertDialog.show();
        alertDialog.setCancelable(false);
        alertDialog.getWindow().setLayout(436, 304);
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


    private void goTonext() {
        //Intent i = new Intent(mContext, RegisterGatherCCUDetails.class);
        //startActivity(i);
        ((FreshRegistration)getActivity()).selectItem(9);
    }

    private void saveToDiag(String key,String val){
        String tag = "";
        if (key.contains("zone")){
           tag = "zone and password";
        } else if (key.contains("building")){
            tag = "building and password";
        } else if (key.contains("system")){
            tag = "system and password";
        } else if (key.contains("setup")){
            tag = "setup and password";
        }
        CCUHsApi.getInstance().writeDefaultVal("point and diag and "+tag, val);
    }
}
