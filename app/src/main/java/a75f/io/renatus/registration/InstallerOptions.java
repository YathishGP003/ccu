package a75f.io.renatus.registration;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.material.textfield.TextInputLayout;
import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.service.unconfirmed.IAmRequest;
import com.renovo.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Segmentation;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;

import org.projecthaystack.HGrid;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.SettingPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.UtilityApplication;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.views.MasterControl.MasterControlView;
import a75f.io.renatus.views.TempLimit.TempLimitView;
import androidx.fragment.app.Fragment;

import static a75f.io.logic.L.ccu;
import static a75f.io.logic.bo.util.UnitUtils.celsiusToFahrenheit;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius;
import static a75f.io.renatus.SettingsFragment.ACTION_SETTING_SCREEN;
import static a75f.io.renatus.views.MasterControl.MasterControlView.getTuner;

public class InstallerOptions extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ImageView imageGoback;
    TempLimitView imageTemp;
    Button mNext;
    Context mContext;
    Spinner mAddressBandSpinner;
    ToggleButton mToggleTempAll;
    HGrid mCCUS;
    HGrid mSite;
    String mSiteId;
    String addressBandSelected = "1000";
    Prefs prefs;
    String localSiteID;
    String CCU_ID = "";
    private boolean isFreshRegister;
    //
    float lowerHeatingTemp;
    float upperHeatingTemp;
    float lowerCoolingTemp;
    float upperCoolingTemp;
    float lowerBuildingTemp;
    float upperBuildingTemp;
    float mSetBack;
    float zoneDiff;
    float cdb, hdb;

    //BACnet Setup
    ToggleButton toggleBACnet;
    ToggleButton toggleCelsius;
    TextView textCelsiusEnable;
    RelativeLayout relativeLayoutBACnet;
    EditText editIPAddr,editSubnet,editGateway;
    Button buttonInitialise;
    String networkConfig = "";
    boolean isEthernet = false;
    UtilityApplication utilityApplication;
    LocalDevice baCnetDevice = null;
    TextInputLayout textInputIP;
    RadioGroup radioGroupConfig;
    Button buttonSendIAM;
    TextView textBacnetEnable;
    TextView textNetworkError;
    private BroadcastReceiver mNetworkReceiver;
    
    private ToggleButton toggleCoolingLockout;
    private ToggleButton toggleHeatingLockout;
    private TextView textCoolingLockoutTemp;
    private Spinner spinnerCoolingLockoutTemp;
    private TextView textHeatingLockoutTemp;
    private Spinner spinnerHeatingLockoutTemp;
    
    private TextView textCoolingLockout;
    private TextView textUseCoolingLockoutDesc;
    private TextView textHeatingLockout;
    private TextView textHeatingLockoutDesc;
    
    private static final String TAG = InstallerOptions.class.getSimpleName();

    MasterControlView.OnClickListener onSaveChangeListener = (lowerHeatingTemp, upperHeatingTemp, lowerCoolingTemp, upperCoolingTemp, lowerBuildingTemp, upperBuildingTemp, setBack, zoneDiff, hdb, cdb) -> {
        imageTemp.setTempControl(lowerHeatingTemp, upperHeatingTemp, lowerCoolingTemp, upperCoolingTemp, lowerBuildingTemp, upperBuildingTemp);

        this.lowerHeatingTemp = lowerHeatingTemp;
        this.upperHeatingTemp = upperHeatingTemp;
        this.lowerCoolingTemp = lowerCoolingTemp;
        this.upperCoolingTemp = upperCoolingTemp;
        this.lowerBuildingTemp = lowerBuildingTemp;
        this.upperBuildingTemp = upperBuildingTemp;
        this.mSetBack = setBack;
        this.zoneDiff = zoneDiff;
        this.cdb = cdb;
        this.hdb = hdb;
    };

    public InstallerOptions() {
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
    public static InstallerOptions newInstance(String param1, String param2) {
        InstallerOptions fragment = new InstallerOptions();
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
        mNetworkReceiver = new NetworkChangeReceiver();
        getActivity().registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //((FreshRegistration)getActivity()).showIcons(false);
        View rootView = inflater.inflate(R.layout.fragment_installeroption, container, false);

        mContext = getContext().getApplicationContext();

        prefs = new Prefs(mContext);
        isFreshRegister = getActivity() instanceof FreshRegistration;
        CCU_ID = prefs.getString("CCU_ID");
        utilityApplication = new RenatusApp();
        if (!isFreshRegister) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) rootView.getLayoutParams();
            p.setMargins(50, 0, 0, 0);
        }

        imageGoback = rootView.findViewById(R.id.imageGoback);
        mAddressBandSpinner = rootView.findViewById(R.id.spinnerAddress);
        CCUUiUtil.setSpinnerDropDownColor(mAddressBandSpinner,getContext());
        mToggleTempAll = rootView.findViewById(R.id.toggleTempAll);
        mNext = rootView.findViewById(R.id.buttonNext);
        imageTemp = rootView.findViewById(R.id.imageTemp);

        //BACnet Setup UI Components
        toggleBACnet = rootView.findViewById(R.id.toggleBACnet);
        toggleCelsius= rootView.findViewById(R.id.toggleCelsius);
        textCelsiusEnable = rootView.findViewById(R.id.textUseCelsius);
        relativeLayoutBACnet = rootView.findViewById(R.id.relativeLayoutBACnet);
        editIPAddr = rootView.findViewById(R.id.editIPaddr);
        editSubnet = rootView.findViewById(R.id.editSubnet);
        editGateway = rootView.findViewById(R.id.editGateway);
        buttonInitialise = rootView.findViewById(R.id.buttonInitialise);
        textInputIP = rootView.findViewById(R.id.textInputIP);
        radioGroupConfig = rootView.findViewById(R.id.radioGroupConfig);
        buttonSendIAM = rootView.findViewById(R.id.buttonSendIAM);
        textBacnetEnable = rootView.findViewById(R.id.textBacnetEnable);
        textNetworkError = rootView.findViewById(R.id.textNetworkError);
        relativeLayoutBACnet.setVisibility(View.GONE);
        buttonSendIAM.setVisibility(View.GONE);
        
        toggleCoolingLockout = rootView.findViewById(R.id.toggleCoolingLockout);
        toggleHeatingLockout = rootView.findViewById(R.id.toggleHeatingLockout);
        textCoolingLockoutTemp = rootView.findViewById(R.id.textCoolingLockoutTemp);
        spinnerCoolingLockoutTemp = rootView.findViewById(R.id.spinnerCoolingLockoutTemp);
        textHeatingLockoutTemp = rootView.findViewById(R.id.textHeatingLockoutTemp);
        spinnerHeatingLockoutTemp = rootView.findViewById(R.id.spinnerHeatingLockoutTemp);
        textCoolingLockout = rootView.findViewById(R.id.textCoolingLockout);
        textUseCoolingLockoutDesc = rootView.findViewById(R.id.textUseCoolingLockoutDesc);
        textHeatingLockout = rootView.findViewById(R.id.textHeatingLockout);
        textHeatingLockoutDesc = rootView.findViewById(R.id.textHeatingLockoutDesc);
        
        initializeTempLockoutUI(CCUHsApi.getInstance());
		HRef ccuId = CCUHsApi.getInstance().getCcuRef();
        String ccuUid = null;

        textCelsiusEnable.setVisibility(View.VISIBLE);
        toggleCelsius.setVisibility(View.VISIBLE);
        HashMap<Object, Object> useCelsius = CCUHsApi.getInstance().readEntity("useCelsius");

        if( (double) getTuner(useCelsius.get("id").toString())==TunerConstants.USE_CELSIUS_FLAG_ENABLED) {
           toggleCelsius.setChecked(true);
           prefs.setBoolean(getString(R.string.USE_CELSIUS_KEY), true);
        } else {
           toggleCelsius.setChecked(false);
           prefs.setBoolean(getString(R.string.USE_CELSIUS_KEY), false);
        }

        if (ccuId != null) {
            ccuUid = CCUHsApi.getInstance().getCcuRef().toString();
        }

        if(CCUUiUtil.isDaikinEnvironment(getContext()))
        {
            textBacnetEnable.setVisibility(View.GONE);
            toggleBACnet.setVisibility(View.GONE);
        }else{
            if(CCUHsApi.getInstance().isCCURegistered() && ccuUid != null){
                textBacnetEnable.setVisibility(View.VISIBLE);
                toggleBACnet.setVisibility(View.VISIBLE);
            }else {
                textBacnetEnable.setVisibility(View.GONE);
                toggleBACnet.setVisibility(View.GONE);
            }
        }

        ArrayList<String> addressBand = new ArrayList<>();
        for (int addr = 1000; addr <= 10900; addr += 100) {
            addressBand.add(String.valueOf(addr));
        }

        ArrayAdapter<String> analogAdapter = new ArrayAdapter<String>(mContext, R.layout.spinner_item, addressBand);
        analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        mAddressBandSpinner.setAdapter(analogAdapter);


        HashMap ccu = CCUHsApi.getInstance().read("ccu");
        //if ccu exists
        if (ccu.size() > 0) {
            for (String addBand : addressBand) {
                String addB = String.valueOf(L.ccu().getSmartNodeAddressBand());
                if (addBand.equals(addB)) {
                    mAddressBandSpinner.setSelection(analogAdapter.getPosition(addBand),false);
                    break;
                }
            }
        } else {
            ccu().setSmartNodeAddressBand((short) 1000);
        }

        mAddressBandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("CCU", "AddressBandSelected : " + mAddressBandSpinner.getSelectedItem());
                if (i > 0) {
                    addressBandSelected = mAddressBandSpinner.getSelectedItem().toString();
                    L.ccu().setSmartNodeAddressBand(Short.parseShort(addressBandSelected));
                    if (!isFreshRegister){
                        HashMap band = CCUHsApi.getInstance().read("point and snband");
                        SettingPoint.Builder sp = new SettingPoint.Builder().setHashMap(band);
                        sp.setVal(addressBandSelected);
                        SettingPoint snBand = sp.build();

                        CCUHsApi.getInstance().updateSettingPoint(snBand, snBand.getId());
                    }
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        mToggleTempAll.setChecked(prefs.getBoolean(getString(R.string.USE_SAME_TEMP_ALL_DAYS)));
        mToggleTempAll.setOnCheckedChangeListener((compoundButton, isChecked) -> prefs.setBoolean(getString(R.string.USE_SAME_TEMP_ALL_DAYS), isChecked));


        if (isFreshRegister) mNext.setVisibility(View.VISIBLE);
        else mNext.setVisibility(View.GONE);
        mNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ccu.size() == 0) {
                    Toast.makeText(getContext(), "CCU device does not exist. Please clear app data and retry " +
                                                 "registration", Toast.LENGTH_LONG).show();
                    return;
                }
                mNext.setEnabled(false);
                String ccuId = ccu.get("id").toString();
                ccuId = ccuId.replace("@", "");
                String ccuName = ccu.get("dis").toString();
                CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(ccuId));
                HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
                SettingPoint snBand = new SettingPoint.Builder()
                        .setDeviceRef(ccuId)
                        .setSiteRef(siteMap.get("id").toString())
                        .setDisplayName(ccuName + "-smartNodeBand")
                        .addMarker("snband").addMarker("sp").setVal(addressBandSelected).build();
                CCUHsApi.getInstance().addPoint(snBand);



                SettingPoint useBacnet = new SettingPoint.Builder()
                        .setDeviceRef(ccuId)
                        .setSiteRef(siteMap.get("id").toString())
                        .setDisplayName(ccuName + "-useBacnet")
                        .addMarker("bacnet").addMarker("enabled").addMarker("sp").setVal(toggleBACnet.isChecked() ? "true":"false").build();
                CCUHsApi.getInstance().addPoint(useBacnet);
                SettingPoint bacnetConfig = new SettingPoint.Builder()
                        .setDeviceRef(ccuId)
                        .setSiteRef(siteMap.get("id").toString())
                        .setDisplayName(ccuName + "-bacnetConfig")
                        .addMarker("bacnet").addMarker("config").addMarker("sp")/*.setVal(radioGroup_config.getCheckedRadioButtonId() == R.id.rbAuto ? "0" :"1")*/.build();
                CCUHsApi.getInstance().addPoint(bacnetConfig);
                SettingPoint bacnetIp = new SettingPoint.Builder()
                        .setDeviceRef(ccuId)
                        .setSiteRef(siteMap.get("id").toString())
                        .setDisplayName(ccuName + "-bacnetIp")
                        .addMarker("bacnet").addMarker("ipconfig").addMarker("sp")/*.setProtocol(editIPAddr.getText() != null ? editIPAddr.getText().toString() : "")*/.build();
                CCUHsApi.getInstance().addPoint(bacnetIp);
                SettingPoint bacnetSubnet = new SettingPoint.Builder()
                        .setDeviceRef(ccuId)
                        .setSiteRef(siteMap.get("id").toString())
                        .setDisplayName(ccuName + "-bacnetSubnet")
                        .addMarker("bacnet").addMarker("ipsubnet").addMarker("sp").setVal(editSubnet.getText() != null ? editSubnet.getText().toString() : "").build();
                CCUHsApi.getInstance().addPoint(bacnetSubnet);
                SettingPoint bacnetGateway = new SettingPoint.Builder()
                        .setDeviceRef(ccuId)
                        .setSiteRef(siteMap.get("id").toString())
                        .setDisplayName(ccuName + "-bacnetGateway")
                        .addMarker("bacnet").addMarker("ipgateway").addMarker("sp").setVal(editGateway.getText() != null ? editGateway.getText().toString() : "").build();
                CCUHsApi.getInstance().addPoint(bacnetGateway);
                // TODO Auto-generated method stub
                goTonext();
            }
        });

        if (!isFreshRegister) {
            ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone");
            if (equips != null && equips.size() > 0) {
                mAddressBandSpinner.setEnabled(false);
            } else {
                mAddressBandSpinner.setEnabled(true);
            }
        }

        imageTemp.setOnClickListener(view -> {
            openMasterControllerDialog();

        });

        getTempValues();

        toggleCelsius.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked) {
                    prefs.setBoolean(getString(R.string.USE_CELSIUS_KEY), isChecked);
                    CCUHsApi.getInstance().writePoint(useCelsius.get("id").toString(), TunerConstants.TUNER_BUILDING_VAL_LEVEL,
                            CCUHsApi.getInstance().getCCUUserName(), 1.0, 0);
                } else {
                    prefs.setBoolean(getString(R.string.USE_CELSIUS_KEY), isChecked);
                    CCUHsApi.getInstance().writePoint(useCelsius.get("id").toString(), TunerConstants.TUNER_BUILDING_VAL_LEVEL,
                            CCUHsApi.getInstance().getCCUUserName(), 0.0, 0);
                }
                getTempValues();
            }
        });

        toggleBACnet.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    if (utilityApplication.checkNetworkConnected()) {
                        relativeLayoutBACnet.setVisibility(View.VISIBLE);
                        buttonInitialise.setEnabled(true);
                        buttonInitialise.setText("Initialise");
                        buttonInitialise.requestFocus();
                        enableConfigType(true);
                        ((RadioButton) radioGroupConfig.getChildAt(0)).setChecked(true);
                        if (utilityApplication.CheckEthernet()) {
                            isEthernet = true;
                            networkConfig = utilityApplication.getIPConfig();
                            String[] ethConfig = networkConfig.split(":");
                            textInputIP.setHint("Ethernet-IP Address");
                            editIPAddr.setText(ethConfig[1]);
                            editGateway.setText(ethConfig[2]);
                            editSubnet.setText(ethConfig[3]);
                        } else {
                            isEthernet = false;
                            networkConfig = utilityApplication.getWiFiConfig();
                            textInputIP.setHint("Wifi-IP Address");
                            String[] ethConfig = networkConfig.split(":");
                            editIPAddr.setText(ethConfig[1]);
                            editGateway.setText(ethConfig[2]);
                            editSubnet.setText(ethConfig[3]);
                        }
                    }else{
                        Log.i("CCU_UTILITYAPP", "checkNetworkConnected:textNetworkError:" +utilityApplication.checkNetworkConnected());
                        textNetworkError.setVisibility(View.VISIBLE);
                    }
                }
                else{
                    try {
                        relativeLayoutBACnet.setVisibility(View.GONE);
                        textNetworkError.setVisibility(View.GONE);
                        utilityApplication.terminateBACnet();
                        L.ccu().setUseBACnet(false);
                        setDefaultNetwork();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        radioGroupConfig.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                if(checkedId == R.id.rbAuto){
                    editIPAddr.setEnabled(false);
                    editGateway.setEnabled(false);
                    editSubnet.setEnabled(false);
                }else if(checkedId == R.id.rbManual){
                    editIPAddr.setEnabled(true);
                    editGateway.setEnabled(false);
                    editSubnet.setEnabled(false);
                }
            }
        });


        buttonInitialise.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startBACnetDevice();
            }
        });

        buttonSendIAM.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                baCnetDevice = utilityApplication.getLocalDevice();
                baCnetDevice.send(baCnetDevice.getLocalBroadcastAddress(),new WhoIsRequest(null, null));
                //baCnetDevice.send(baCnetDevice.getLocalBroadcastAddress(),new IAmRequest(new ObjectIdentifier(ObjectType.device,baCnetDevice.getInstanceNumber()), baCnetDevice.get(PropertyIdentifier.maxApduLengthAccepted), baCnetDevice.get(PropertyIdentifier.segmentationSupported),baCnetDevice.get(PropertyIdentifier.vendorIdentifier)));
                baCnetDevice.sendLocalBroadcast(new IAmRequest(new ObjectIdentifier(ObjectType.device,baCnetDevice.getInstanceNumber()), baCnetDevice.get(PropertyIdentifier.maxApduLengthAccepted), Segmentation.segmentedBoth,baCnetDevice.get(PropertyIdentifier.vendorIdentifier)));
                Log.i("Bacnet", "Device:" + baCnetDevice.getId() + " Name:" + baCnetDevice.getDeviceObject().getObjectName()+" Sent IAM");
            }
        });

        getActivity().registerReceiver(mPairingReceiver, new IntentFilter(ACTION_SETTING_SCREEN));

        getBACnetConfig();

        return rootView;
    }

    private void lockBACnetConfig(){
        Log.i("Bacnet", "Initialize Button Pressed");
        enableConfigType(false);
        buttonInitialise.setText("BACnet Initialised");
        buttonInitialise.setEnabled(false);
        editIPAddr.setEnabled(false);
        editGateway.setEnabled(false);
        editSubnet.setEnabled(false);
        buttonSendIAM.setEnabled(true);
        buttonSendIAM.setVisibility(View.GONE);
    }
    
    private void hideTempLockoutUI() {
        toggleCoolingLockout.setVisibility(View.GONE);
        toggleHeatingLockout.setVisibility(View.GONE);
        textCoolingLockoutTemp.setVisibility(View.GONE);
        spinnerCoolingLockoutTemp.setVisibility(View.GONE);
        textHeatingLockoutTemp.setVisibility(View.GONE);
        spinnerHeatingLockoutTemp.setVisibility(View.GONE);
        textCoolingLockout.setVisibility(View.GONE);
        textUseCoolingLockoutDesc.setVisibility(View.GONE);
        textHeatingLockout.setVisibility(View.GONE);
        textHeatingLockoutDesc.setVisibility(View.GONE);
    }
    private void initializeTempLockoutUI(CCUHsApi hayStack) {
        
        if (ccu().systemProfile == null || ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT) {
            hideTempLockoutUI();
            return;
        }
        
        ArrayAdapter<Double> coolingLockoutAdapter = CCUUiUtil.getArrayAdapter(0,70,1, getActivity());
        spinnerCoolingLockoutTemp.setAdapter(coolingLockoutAdapter);
        spinnerCoolingLockoutTemp.setSelection(coolingLockoutAdapter.getPosition(ccu().systemProfile.getCoolingLockoutVal()),
                                                                                                        false);
    
        spinnerCoolingLockoutTemp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setCoolingLockoutVal(hayStack, Double.parseDouble(spinnerCoolingLockoutTemp.getSelectedItem().toString()));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                //Not handled
            }
        });
        
        ArrayAdapter<Double> heatingLockoutAdapter = CCUUiUtil.getArrayAdapter(50,100,1, getActivity());
        spinnerHeatingLockoutTemp.setAdapter(heatingLockoutAdapter);
        spinnerHeatingLockoutTemp.setSelection(heatingLockoutAdapter.getPosition(ccu().systemProfile.getHeatingLockoutVal())
                                                                                                        , false);
        spinnerHeatingLockoutTemp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setHeatingLockoutVal(hayStack,
                                     Double.parseDouble(spinnerHeatingLockoutTemp.getSelectedItem().toString()));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                //Not handled
            }
        });
        boolean coolingLockoutConfig = ccu().systemProfile.isOutsideTempCoolingLockoutEnabled(hayStack);
        toggleCoolingLockout.setChecked(coolingLockoutConfig);
        updateCoolingLockoutUIVisibility(coolingLockoutConfig);
        
        toggleCoolingLockout.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ccu().systemProfile.setOutsideTempCoolingLockoutEnabled(hayStack, isChecked);
            updateCoolingLockoutUIVisibility(isChecked);
        });
    
        boolean heatingLockoutConfig = ccu().systemProfile.isOutsideTempHeatingLockoutEnabled(hayStack);
        toggleHeatingLockout.setChecked(heatingLockoutConfig);
        updateHeatingLockoutUIVisibility(heatingLockoutConfig);
        
        toggleHeatingLockout.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ccu().systemProfile.setOutsideTempHeatingLockoutEnabled(hayStack, isChecked);
            updateHeatingLockoutUIVisibility(isChecked);
        });
    }
    
    private void updateCoolingLockoutUIVisibility(boolean isVisible) {
        textCoolingLockoutTemp.setVisibility(isVisible ? View.VISIBLE:View.GONE);
        spinnerCoolingLockoutTemp.setVisibility(isVisible ? View.VISIBLE:View.GONE);
    }
    
    private void updateHeatingLockoutUIVisibility(boolean isVisible) {
        textHeatingLockoutTemp.setVisibility(isVisible ? View.VISIBLE:View.GONE);
        spinnerHeatingLockoutTemp.setVisibility(isVisible ? View.VISIBLE:View.GONE);
    }
    
    private void setCoolingLockoutVal(CCUHsApi hayStack, double val) {
        HashMap<Object, Object> coolingLockoutPoint = hayStack.readEntity("point and tuner and outsideTemp " +
                                                                          "and cooling and lockout and equipRef ==\""
                                                                          +ccu().systemProfile.getSystemEquipRef()+"\"");
        if (!coolingLockoutPoint.isEmpty()) {
            RxjavaUtil.executeBackground(() ->hayStack.writePointForCcuUser(coolingLockoutPoint.get("id").toString(),
                                                                       HayStackConstants.SYSTEM_POINT_LEVEL, val, 0));
        }
    }
    
    private void setHeatingLockoutVal(CCUHsApi hayStack, double val) {
        HashMap<Object, Object> heatingLockoutPoint = hayStack.readEntity("point and tuner and outsideTemp " +
                                                                          "and heating and lockout and equipRef ==\""
                                                                          +ccu().systemProfile.getSystemEquipRef()+"\"");
        
        if (!heatingLockoutPoint.isEmpty()) {
            RxjavaUtil.executeBackground(() ->hayStack.writePointForCcuUser(heatingLockoutPoint.get("id").toString(),
                                                                            HayStackConstants.SYSTEM_POINT_LEVEL, val, 0));
        }
    }

    public void startBACnetDevice(){
        LocalDevice localDevice = null;
        int checkedId = radioGroupConfig.getCheckedRadioButtonId();
        if(checkedId == R.id.rbAuto) {
            localDevice = autoConfigBACnetDevice();
        }else if(checkedId == R.id.rbManual){
            localDevice = manualConfigBACnetDevice();
        }
        if(localDevice != null) {
            lockBACnetConfig();
            L.ccu().setUseBACnet(true);
            utilityApplication.sendWhoIs(localDevice);
            utilityApplication.setWifiasDefault();
        }
    }

    public LocalDevice autoConfigBACnetDevice(){
        LocalDevice localDevice = null;
        if (isEthernet) {
            localDevice = utilityApplication.enableBACnet(networkConfig);
            try {
                localDevice.initialize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            prefs.setBoolean("BACnetLAN",true);
        } else {
            localDevice = utilityApplication.enableBACnetWifi();
            prefs.setBoolean("BACnetLAN",false);
        }
        if (localDevice != null) {
            utilityApplication.setLocalDevice(localDevice,true);
        }
        return localDevice;
    }

    public LocalDevice manualConfigBACnetDevice(){
        LocalDevice localDevice = null;
        if(validateIPAddress(editIPAddr.getText().toString())){
            utilityApplication.setNetwork(editIPAddr.getText().toString(),editGateway.getText().toString(), editSubnet.getText().toString(),isEthernet);
            if(isEthernet) {
                networkConfig = utilityApplication.getIPConfig();
                prefs.setBoolean("BACnetLAN",true);
            }else {
                networkConfig = utilityApplication.getWiFiConfig();
                prefs.setBoolean("BACnetLAN",false);
            }
            localDevice = utilityApplication.enableBACnet(networkConfig);
            prefs.setString("BACnetConfig",networkConfig);
            if (localDevice != null) {
                utilityApplication.setLocalDevice(localDevice,false);
            }
        }else {
            editIPAddr.setError("Invalid IP");
        }
        return localDevice;
    }

    // initial master control values
    private void getTempValues() {

        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        Equip p = new Equip.Builder().setHashMap(tuner).build();

        hdb = (float) TunerUtil.getHeatingDeadband(p.getId());
        cdb = (float) TunerUtil.getCoolingDeadband(p.getId());
        HashMap coolUL = CCUHsApi.getInstance().read("point and limit and max and cooling and user");
        HashMap heatUL = CCUHsApi.getInstance().read("point and limit and min and heating and user");
        HashMap coolLL = CCUHsApi.getInstance().read("point and limit and min and cooling and user");
        HashMap heatLL = CCUHsApi.getInstance().read("point and limit and max and heating and user");
        HashMap buildingMin = CCUHsApi.getInstance().read("building and limit and min");
        HashMap buildingMax = CCUHsApi.getInstance().read("building and limit and max");
        HashMap setbackMap = CCUHsApi.getInstance().read("unoccupied and setback and equipRef == \"" + p.getId() + "\"");
        HashMap zoneDiffMap = CCUHsApi.getInstance().read("building and zone and differential");

        upperCoolingTemp = (float) getTuner(coolUL.get("id").toString());
        lowerCoolingTemp = (float) getTuner(coolLL.get("id").toString());
        upperHeatingTemp = (float) getTuner(heatUL.get("id").toString());
        lowerHeatingTemp = (float) getTuner(heatLL.get("id").toString());
        lowerBuildingTemp = (float) getTuner(buildingMin.get("id").toString());
        upperBuildingTemp = (float) getTuner(buildingMax.get("id").toString());
        mSetBack = (float) getTuner(setbackMap.get("id").toString());
        zoneDiff = (float) getTuner(zoneDiffMap.get("id").toString());
    }

    //custom dialog to control building temperature
    private void openMasterControllerDialog() {
        if (isAdded()) {

            final Dialog dialog = new Dialog(getActivity());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.dialog_master_control);
            MasterControlView masterControlView = dialog.findViewById(R.id.masterControlView);

            dialog.findViewById(R.id.btnCancel).setOnClickListener(view -> dialog.dismiss());
            dialog.findViewById(R.id.btnClose).setOnClickListener(view -> dialog.dismiss());

            dialog.findViewById(R.id.btnSet).setOnClickListener(view -> masterControlView.setTuner(dialog));

            masterControlView.setOnClickChangeListener(onSaveChangeListener);

            new CountDownTimer(100, 100) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    masterControlView.setMasterControl(lowerHeatingTemp, upperHeatingTemp, lowerCoolingTemp,
                            upperCoolingTemp, lowerBuildingTemp, upperBuildingTemp,
                            mSetBack, zoneDiff, hdb, cdb);
                }
            }.start();

            dialog.show();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void goTonext() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                //showProgressDialog();
            }

            @Override
            protected Void doInBackground(Void... voids) {

                if (!Globals.getInstance().siteAlreadyCreated()) {
                    BuildingTuners.getInstance();
                    DefaultSchedules.setDefaultCoolingHeatingTemp();
                    DefaultSchedules.generateDefaultSchedule(false, null);
                }

                L.saveCCUState();
                CCUHsApi.getInstance().log();
                CCUHsApi.getInstance().syncEntityTree();


                return null;
            }

            @Override
            protected void onPostExecute(Void nul) {
                super.onPostExecute(nul);
                //hideProgressDialog();
                prefs.setBoolean("PROFILE_SETUP", false);
                prefs.setBoolean("CCU_SETUP", true);
                ((FreshRegistration) getActivity()).selectItem(5);

            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    @Override
    public void onDestroyView() {
        if (getActivity() != null){
            getActivity().unregisterReceiver(mPairingReceiver);
        }
        super.onDestroyView();
    }

    private final BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null){
                return;
            }

            if (intent.getAction().equals(ACTION_SETTING_SCREEN)) {
                if (mAddressBandSpinner != null) {
                    ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone");
                    if (equips != null && equips.size() > 0) {
                        mAddressBandSpinner.setEnabled(false);
                    } else {
                        mAddressBandSpinner.setEnabled(true);
                    }
                }
            }
        }
    };

    public void setDefaultNetwork(){
        final ConnectivityManager connMgr = (ConnectivityManager) Globals.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            ConnectivityManager.setProcessDefaultNetwork(null);
        } else {
            connMgr.bindProcessToNetwork(null);
        }
    }

    public boolean validateIPAddress(String manualIPAddress) {
        if(!Patterns.IP_ADDRESS.matcher(manualIPAddress).matches()){
            return false;
        }else{
            String[] manualIp = (manualIPAddress).split("\\.");
            return  Integer.parseInt(manualIp[0]) < 255 & Integer.parseInt(manualIp[0]) > 0 &
                    Integer.parseInt(manualIp[1]) < 255 & Integer.parseInt(manualIp[1]) > 0 &
                    Integer.parseInt(manualIp[2]) < 255 & Integer.parseInt(manualIp[2]) >= 0 &
                    Integer.parseInt(manualIp[3]) < 255 & Integer.parseInt(manualIp[3]) > 0;
        }
    }

    public class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("CCU_UTILITYAPP", "NetworkChangeReceiver:" +utilityApplication.checkNetworkConnected());
            if(utilityApplication.checkNetworkConnected()) {
                textNetworkError.setVisibility(View.GONE);
                getBACnetConfig();
            }else {
                if(toggleBACnet.isChecked()) {
                    textNetworkError.setVisibility(View.VISIBLE);
                    relativeLayoutBACnet.setVisibility(View.GONE);
                    if(!utilityApplication.isBACnetEnabled()){
                        toggleBACnet.setChecked(false);
                    }
                }
            }
        }
    }

    public void enableConfigType(boolean lockOption){
        for (int i = 0; i < radioGroupConfig.getChildCount(); i++) {
            radioGroupConfig.getChildAt(i).setEnabled(lockOption);
        }
    }

    public void getBACnetConfig(){
        if (utilityApplication.isBACnetEnabled()) {
            toggleBACnet.setChecked(true);
            if(utilityApplication.checkNetworkConnected()) {
                relativeLayoutBACnet.setVisibility(View.VISIBLE);
                if (!utilityApplication.isAutoMode()) { // Check for BACnet Enabled in Auto or Manual
                    networkConfig = prefs.getString("BACnetConfig");
                    radioGroupConfig.check(R.id.rbManual);
                    String[] ethConfig = networkConfig.split(":");
                    editIPAddr.setText(ethConfig[1]);
                    editGateway.setText(ethConfig[2]);
                    editSubnet.setText(ethConfig[3]);
                }
                lockBACnetConfig();
            }
        }
    }
}
