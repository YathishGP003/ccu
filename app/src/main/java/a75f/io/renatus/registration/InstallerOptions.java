package a75f.io.renatus.registration;

import android.app.AlertDialog;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.client.HClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.SettingPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BackfillUtilKt;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.diag.otastatus.OtaStatusDiagPoint;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.limits.SchedulabeLimits;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.UtilityApplication;
import a75f.io.renatus.buildingoccupancy.BuildingOccupancyFragment;
import a75f.io.renatus.tuners.TunerFragment;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.views.MasterControl.MasterControlUtil;
import a75f.io.renatus.views.MasterControl.MasterControlView;
import a75f.io.renatus.views.TempLimit.TempLimitView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import static a75f.io.device.bacnet.BacnetConfigConstants.BACNET_CONFIGURATION;
import static a75f.io.device.bacnet.BacnetConfigConstants.IP_DEVICE_INSTANCE_NUMBER;
import static a75f.io.device.bacnet.BacnetUtilKt.sendBroadCast;
import static a75f.io.logic.L.ccu;

import static a75f.io.logic.bo.util.UnitUtils.celsiusToFahrenheit;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsiusRelative;
import static a75f.io.logic.bo.util.UnitUtils.celsiusToFahrenheit;
import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;
import static a75f.io.logic.service.FileBackupJobReceiver.performConfigFileBackup;
import static a75f.io.renatus.SettingsFragment.ACTION_SETTING_SCREEN;
import static a75f.io.renatus.util.BackFillViewModel.*;
import static a75f.io.renatus.views.MasterControl.MasterControlUtil.getAdapterVal;
import static a75f.io.renatus.views.MasterControl.MasterControlUtil.getAdapterValDeadBand;
import static a75f.io.renatus.views.MasterControl.MasterControlUtil.getAdapterValDiff;
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
    private static InstallerOptions instance;
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
    ToggleButton toggleCelsius;
    TextView textCelsiusEnable;

    EditText editIPAddr,editSubnet,editGateway;
    Button buttonInitialise;
    String networkConfig = "";
    boolean isEthernet = false;
    UtilityApplication utilityApplication;
    LinearLayout linearLayout;
    TextView textNetworkError;
    private BroadcastReceiver mNetworkReceiver;

    View toastLayout;

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

    private Spinner coolingLimitMin;
    private Spinner coolingLimitMax;
    private Spinner heatingLimitMin;
    private Spinner heatingLimitMax;
    private Spinner coolingDeadBand;
    private Spinner heatingDeadBand;
    private Spinner buildingLimitMin;
    private Spinner buildingLimitMax;
    private Spinner unoccupiedZoneSetback;
    private Spinner buildingToZoneDiff;

    private Spinner backFillTimeSpinner;

    private static final String TAG = InstallerOptions.class.getSimpleName();
    ArrayList<String> regAddressBands = new ArrayList<>();
    ArrayList<String> addressBand = new ArrayList<>();
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
          instance=this;
    }

    public static InstallerOptions getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Api is not initialized");
        }
        return instance;
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

    @SuppressLint("MissingInflatedId")
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

        imageGoback = rootView.findViewById(R.id.imageGoback);
        mAddressBandSpinner = rootView.findViewById(R.id.spinnerAddress);
        CCUUiUtil.setSpinnerDropDownColor(mAddressBandSpinner,getContext());
        mToggleTempAll = rootView.findViewById(R.id.toggleTempAll);
        mNext = rootView.findViewById(R.id.buttonNext);
        imageTemp = rootView.findViewById(R.id.imageTemp);

        //BACnet Setup UI Components
        toggleCelsius= rootView.findViewById(R.id.toggleCelsius);
        textCelsiusEnable = rootView.findViewById(R.id.textUseCelsius);
        textNetworkError = rootView.findViewById(R.id.textNetworkError);
        linearLayout = rootView.findViewById(R.id.layoutFooterButtons);
        Button buttonApply = rootView.findViewById(R.id.buttonApply);
        Button buttonCancel = rootView.findViewById(R.id.buttonCancel);
        linearLayout = rootView.findViewById(R.id.layoutFooterButtons);
        LayoutInflater li = getLayoutInflater();
        toastLayout = li.inflate(R.layout.custom_toast_layout_backfill, (ViewGroup) rootView.findViewById(R.id.custom_toast_layout_backfill));

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

        setToggleCheck();

        if (ccuId != null) {
            ccuUid = CCUHsApi.getInstance().getCcuRef().toString();
        }

        for (int addr = 1000; addr <= 10900; addr += 100) {
            addressBand.add(String.valueOf(addr));
        }

        ArrayList<HashMap> equipments = CCUHsApi.getInstance().readAll("equip and zone");
        if (equipments.size() == 0)
            getRegisteredAddressBand(); // doing this when no equips available
        else
            setNodeAddress();

        mAddressBandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("CCU", "AddressBandSelected : " + mAddressBandSpinner.getSelectedItem());
                if (i >= 0) {
                    addressBandSelected = mAddressBandSpinner.getSelectedItem().toString();
                    L.ccu().setSmartNodeAddressBand(Short.parseShort(addressBandSelected));
                    if (!isFreshRegister){
                        HashMap band = CCUHsApi.getInstance().read("point and snband");
                        SettingPoint.Builder sp = new SettingPoint.Builder().setHashMap(band);
                        sp.setVal(addressBandSelected);
                        SettingPoint snBand = sp.build();

                        CCUHsApi.getInstance().updateSettingPoint(snBand, snBand.getId());

                        try {
                            String confString = prefs.getString(BACNET_CONFIGURATION);
                            JSONObject config = new JSONObject(confString);
                            JSONObject deviceObject = config.getJSONObject("device");
                            deviceObject.put(IP_DEVICE_INSTANCE_NUMBER,Integer.parseInt(addressBandSelected) + 99);
                            prefs.setString(BACNET_CONFIGURATION, config.toString());
                            sendBroadCast(mContext, "a75f.io.renatus.BACNET_CONFIG_CHANGE", "BACnet configurations are changed");
                            performConfigFileBackup();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

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
        HashMap ccu = CCUHsApi.getInstance().read("ccu");
        mNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ccu.size() == 0) {
                    Toast.makeText(getContext(), "CCU device does not exist. Please clear app data and retry " +
                                                 "registration", Toast.LENGTH_LONG).show();
                    return;
                }
                mNext.setEnabled(false);
                createInstallerPoints(ccu, prefs);
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
                HashMap<Object, Object> useCelsius = CCUHsApi.getInstance().readEntity("displayUnit");

                if (!useCelsius.isEmpty()) {
                    if (isChecked) {
                        CCUHsApi.getInstance().writePoint(useCelsius.get("id").toString(), TunerConstants.TUNER_BUILDING_VAL_LEVEL,
                                CCUHsApi.getInstance().getCCUUserName(), 1.0, 0);
                    } else {
                        CCUHsApi.getInstance().writePoint(useCelsius.get("id").toString(), TunerConstants.TUNER_BUILDING_VAL_LEVEL,
                                CCUHsApi.getInstance().getCCUUserName(), 0.0, 0);
                    }
                } else {
                    Toast.makeText(getContext(), "To enable \"Use Celsius\" feature on this device upgrade Primary CCU.\nRestart app on this device after Primary CCU is Upgraded.", Toast.LENGTH_LONG).show();
                }
                getTempValues();
                initializeTempLockoutUI(CCUHsApi.getInstance());
                if (TunerFragment.newInstance().tunerExpandableLayoutHelper != null) {
                    TunerFragment.newInstance().tunerExpandableLayoutHelper.notifyDataSetChanged();
                }
                toggleCelsius.setEnabled(false);
                new Handler().postDelayed(() -> toggleCelsius.setEnabled(true),2000);
            }
        });

        getActivity().registerReceiver(mPairingReceiver, new IntentFilter(ACTION_SETTING_SCREEN));

        setBackFillTimeSpinner(rootView);

        buttonApply.setOnClickListener(view -> {
            int selectedSpinnerItem = backFillTimeSpinner.getSelectedItemPosition();
            int[] durations = BackFillDuration.toIntArray();
            int index = selectedSpinnerItem > 0 ? Math.min(selectedSpinnerItem , durations.length - 1) : 0;
            int backFillDurationSelected = durations[index];

            BackfillUtilKt.updateBackfillDuration(backFillDurationSelected);

            if (!isFreshRegister) {
                generateToastMessage(toastLayout);
            }
            linearLayout.setVisibility(View.INVISIBLE);
        });

        buttonCancel.setOnClickListener(view -> backFillTimeSpinner.setSelection(BackFillDuration.getIndex(BackFillDuration.toIntArray(), getBackFillDuration(), 24)));

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (!isFreshRegister) {
                    ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                    p.setMargins(50, 0, 0, 0);
                    view.setLayoutParams(p);
                }
            }
        });

        Fragment childFragment = new BuildingOccupancyFragment();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.buildingOccupancyFragmentContainer, childFragment).commit();
    }

    public void createInstallerPoints(HashMap ccu, Prefs prefs) {
        String ccuId = ccu.get("id").toString();
        ccuId = ccuId.replace("@", "");
        String ccuName = ccu.get("dis").toString();
        CCUHsApi.getInstance().addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(ccuId));
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);

        if(!prefs.getString("INSTALL_TYPE").equals("ADDCCU")) {
            SettingPoint snBand = new SettingPoint.Builder()
                    .setDeviceRef(ccuId)
                    .setSiteRef(siteMap.get("id").toString())
                    .setDisplayName(ccuName + "-smartNodeBand")
                    .addMarker("snband").addMarker("sp").setVal(addressBandSelected).build();
            CCUHsApi.getInstance().addPoint(snBand);
        }
        OtaStatusDiagPoint.Companion.addOTAStatusPoint(
                siteMap.get("dis").toString()+"-CCU",
                ccu.get("equipRef").toString(),
                ccu.get("siteRef").toString(),
                siteMap.get(Tags.TZ).toString(),
                CCUHsApi.getInstance()
        );

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

        BackfillUtilKt.addBackFillDurationPointIfNotExists(CCUHsApi.getInstance());
    }

    private String getEditGateWay(EditText editGateway) {
        return editGateway.getText() != null ? editGateway.getText().toString() : "";
    }

    private String getEditSubnet(EditText editSubnet) {
        return editSubnet.getText() != null ? editSubnet.getText().toString() : "";
    }

    private String getToggleForBacnet(ToggleButton toggleBACnet) {
        return toggleBACnet.isChecked() ? "true":"false";
    }

    public void setToggleCheck() {
        if (toggleCelsius!=null && isCelsiusTunerAvailableStatus()) {
            toggleCelsius.setChecked(true);
        } else {
            toggleCelsius.setChecked(false);
        }
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

        ArrayAdapter<Double> coolingLockoutAdapter;
        double coolingVal = ccu().systemProfile.getCoolingLockoutVal();
        if (isCelsiusTunerAvailableStatus()){
            coolingLockoutAdapter = CCUUiUtil.getArrayAdapter(Math.round(fahrenheitToCelsius(0)),Math.round(fahrenheitToCelsius(70)),1, getActivity());
            coolingVal =  Math.round(fahrenheitToCelsius(coolingVal));
        } else {
            coolingLockoutAdapter = CCUUiUtil.getArrayAdapter(0,70,1, getActivity());
        }
        spinnerCoolingLockoutTemp.setAdapter(coolingLockoutAdapter);
        spinnerCoolingLockoutTemp.setSelection(coolingLockoutAdapter.getPosition(coolingVal),false);
    
        spinnerCoolingLockoutTemp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isCelsiusTunerAvailableStatus()) {
                    setCoolingLockoutVal(hayStack, Math.round(celsiusToFahrenheit(Double.parseDouble(spinnerCoolingLockoutTemp.getSelectedItem().toString()))));
                } else {
                    setCoolingLockoutVal(hayStack, Double.parseDouble(spinnerCoolingLockoutTemp.getSelectedItem().toString()));
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                //Not handled
            }
        });


        ArrayAdapter<Double> heatingLockoutAdapter;
        double heatingVal = ccu().systemProfile.getHeatingLockoutVal();
        if (isCelsiusTunerAvailableStatus()){
            heatingLockoutAdapter = CCUUiUtil.getArrayAdapter(Math.round(fahrenheitToCelsius(50)),Math.round(fahrenheitToCelsius(100)),1, getActivity());
            heatingVal =  Math.round(fahrenheitToCelsius(heatingVal));
        } else {
            heatingLockoutAdapter = CCUUiUtil.getArrayAdapter(50,100,1, getActivity());
        }
        spinnerHeatingLockoutTemp.setAdapter(heatingLockoutAdapter);
        spinnerHeatingLockoutTemp.setSelection(heatingLockoutAdapter.getPosition(heatingVal), false);
        spinnerHeatingLockoutTemp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isCelsiusTunerAvailableStatus()) {
                    setHeatingLockoutVal(hayStack,
                            Math.round(celsiusToFahrenheit(Double.parseDouble(spinnerHeatingLockoutTemp.getSelectedItem().toString()))));
                } else {
                    setHeatingLockoutVal(hayStack,
                            Double.parseDouble(spinnerHeatingLockoutTemp.getSelectedItem().toString()));
                }
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

    // initial master control values
    private void getTempValues() {

        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        Equip p = new Equip.Builder().setHashMap(tuner).build();
        if(MasterControlUtil.isMigrated()) {
            HashMap<Object, Object> coolDB = CCUHsApi.getInstance().readEntity("point and cooling and deadband and schedulable and default");
            HashMap<Object, Object> heatDB = CCUHsApi.getInstance().readEntity("point and heating and deadband and schedulable and default");
            HashMap<Object, Object> coolUL = CCUHsApi.getInstance().readEntity("schedulable and point and limit and max and cooling and user and default");
            HashMap<Object, Object> heatUL = CCUHsApi.getInstance().readEntity("schedulable and point and limit and min and heating and user and default");
            HashMap<Object, Object> coolLL = CCUHsApi.getInstance().readEntity("schedulable and point and limit and min and cooling and user and default");
            HashMap<Object, Object> heatLL = CCUHsApi.getInstance().readEntity("schedulable and point and limit and max and heating and user and default");
            HashMap<Object, Object> buildingMin = CCUHsApi.getInstance().readEntity("building and limit and min and not tuner");
            HashMap<Object, Object> buildingMax = CCUHsApi.getInstance().readEntity("building and limit and max and not tuner");
            HashMap<Object, Object> setbackMap = CCUHsApi.getInstance().readEntity("unoccupied and setback and equipRef == \"" + p.getId() + "\"");
            HashMap<Object, Object> zoneDiffMap = CCUHsApi.getInstance().readEntity("building and zone and differential");

            hdb = (float) HSUtil.getLevelValueFrom16(heatDB.get("id").toString());
            cdb = (float) HSUtil.getLevelValueFrom16(coolDB.get("id").toString());
            upperCoolingTemp = (float) HSUtil.getLevelValueFrom16(coolUL.get("id").toString());
            lowerCoolingTemp = (float) HSUtil.getLevelValueFrom16(coolLL.get("id").toString());
            upperHeatingTemp = (float) HSUtil.getLevelValueFrom16(heatUL.get("id").toString());
            lowerHeatingTemp = (float) HSUtil.getLevelValueFrom16(heatLL.get("id").toString());

            lowerBuildingTemp = (float) getTuner(buildingMin.get("id").toString());
            upperBuildingTemp = (float) getTuner(buildingMax.get("id").toString());
            mSetBack = (float) getTuner(setbackMap.get("id").toString());
            zoneDiff = (float) getTuner(zoneDiffMap.get("id").toString());
        }else{

            hdb = (float) TunerUtil.getHeatingDeadband(p.getId());
            cdb = (float) TunerUtil.getCoolingDeadband(p.getId());
            HashMap coolUL = CCUHsApi.getInstance().read("point and limit and max and cooling and user and tuner");
            HashMap heatUL = CCUHsApi.getInstance().read("point and limit and min and heating and user and tuner");
            HashMap coolLL = CCUHsApi.getInstance().read("point and limit and min and cooling and user and tuner");
            HashMap heatLL = CCUHsApi.getInstance().read("point and limit and max and heating and user and tuner");
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

    }

    //custom dialog to control building temperature
    private void openMasterControllerDialog() {
        if (isAdded()) {

            final Dialog dialog = new Dialog(getActivity());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.dialog_master_control);
            MasterControlView masterControlView = dialog.findViewById(R.id.masterControlView);

            //12950- New User limits UI

            HashMap<Object,Object> buildingCoolingUpperLimit = CCUHsApi.getInstance().readEntity("schedulable and point and limit and max and cooling and user and default");
            HashMap<Object,Object> buildingHeatingUpperLimit = CCUHsApi.getInstance().readEntity("schedulable and point and limit and min and heating and user and default");
            HashMap<Object,Object> buildingCoolingLowerLimit = CCUHsApi.getInstance().readEntity("schedulable and point and limit and min and cooling and user and default");
            HashMap<Object,Object> buildingHeatingLowerLimit = CCUHsApi.getInstance().readEntity("schedulable and point and limit and max and heating and user and default");
            HashMap<Object,Object> buildingMin = CCUHsApi.getInstance().readEntity("building and limit and min and not tuner");
            HashMap<Object,Object> buildingMax = CCUHsApi.getInstance().readEntity("building and limit and max and not tuner");

            HashMap<Object,Object> coolingDeadbandObj = CCUHsApi.getInstance().readEntity("schedulable and cooling and deadband and default");
            HashMap<Object,Object> heatingDeadbandObj = CCUHsApi.getInstance().readEntity("schedulable and heating and deadband and default");
            HashMap<Object,Object> unoccupiedZoneObj = CCUHsApi.getInstance().readEntity("schedulable and unoccupied and default and setback");
            HashMap<Object,Object> buildingToZoneDiffObj = CCUHsApi.getInstance().readEntity("building and zone and differential and cur");

            buildingLimitMin = dialog.findViewById(R.id.buildinglimmin);
            buildingLimitMax =  dialog.findViewById(R.id.buildinglimitmax);
            unoccupiedZoneSetback =  dialog.findViewById(R.id.unoccupiedzonesetback);
            heatingLimitMin =  dialog.findViewById(R.id.heatinglimmin);
            heatingLimitMax =  dialog.findViewById(R.id.heatinglimmax);
            coolingLimitMin =  dialog.findViewById(R.id.coolinglimmin);
            coolingLimitMax =  dialog.findViewById(R.id.coolinglimmax);
            coolingDeadBand =  dialog.findViewById(R.id.coolingdeadband);
            heatingDeadBand =  dialog.findViewById(R.id.heatingdeadband);
            buildingToZoneDiff =  dialog.findViewById(R.id.buildingtozonediff);



            CCUUiUtil.setSpinnerDropDownColor(buildingLimitMin,getContext());
            CCUUiUtil.setSpinnerDropDownColor(buildingLimitMax,getContext());
            CCUUiUtil.setSpinnerDropDownColor(unoccupiedZoneSetback,getContext());
            CCUUiUtil.setSpinnerDropDownColor(heatingLimitMin ,getContext());
            CCUUiUtil.setSpinnerDropDownColor(heatingLimitMax ,getContext());
            CCUUiUtil.setSpinnerDropDownColor(coolingLimitMin ,getContext());
            CCUUiUtil.setSpinnerDropDownColor(coolingLimitMax ,getContext());
            CCUUiUtil.setSpinnerDropDownColor(coolingDeadBand ,getContext());
            CCUUiUtil.setSpinnerDropDownColor(heatingDeadBand ,getContext());
            CCUUiUtil.setSpinnerDropDownColor(buildingToZoneDiff,getContext());



            ArrayList<String> list = new ArrayList<>();
            ArrayList<String > zoneSetBack = new ArrayList<>();
            ArrayList<String> heatingLimit = new ArrayList<>();
            ArrayList<String> coolingLimit = new ArrayList<>();
            ArrayList<String> deadBand = new ArrayList<>();
            ArrayList<String> zonediff = new ArrayList<>();

            if(isCelsiusTunerAvailableStatus()){
                buildingLimitMin.setDropDownWidth(150);
                buildingLimitMax.setDropDownWidth(150);
                unoccupiedZoneSetback.setDropDownWidth(150);
                heatingLimitMin.setDropDownWidth(150);
                heatingLimitMax.setDropDownWidth(150);
                coolingLimitMin.setDropDownWidth(150);
                coolingLimitMax.setDropDownWidth(150);
                coolingDeadBand.setDropDownWidth(150);
                heatingDeadBand.setDropDownWidth(150);
                buildingToZoneDiff.setDropDownWidth(150);
                for (int val = 32;  val <= 140; val += 1) {
                    list.add(val+"\u00B0F  (" + fahrenheitToCelsius(val) + "\u00B0C)");
                }
                for (double val = 0;  val <= 20; val += 1) {
                    zoneSetBack.add(val+"\u00B0F  (" + CCUUtils.roundToOneDecimal(fahrenheitToCelsiusRelative(val)) + "\u00B0C)");
                }
                for (int val = 50;  val <= 100; val += 1) {
                    heatingLimit.add(val+"\u00B0F  (" + fahrenheitToCelsius(val) + "\u00B0C)");
                }
                for (int val = 50;  val <= 100; val += 1) {
                    coolingLimit.add(val+"\u00B0F  (" + fahrenheitToCelsius(val) + "\u00B0C)");
                }
                for (double val = 0;  val <= 10; val += 0.5) {
                    deadBand.add(val+"\u00B0F  (" + (fahrenheitToCelsiusRelative(val)) + "\u00B0C)");
                }
                for (int val = 0;  val <= 20; val += 1) {
                    zonediff.add(val+"\u00B0F  (" + (fahrenheitToCelsiusRelative(val)) + "\u00B0C)");
                }

            }else{
                buildingLimitMin.setDropDownWidth(70);
                buildingLimitMax.setDropDownWidth(70);
                unoccupiedZoneSetback.setDropDownWidth(70);
                heatingLimitMin.setDropDownWidth(70);
                heatingLimitMax.setDropDownWidth(70);
                coolingLimitMin.setDropDownWidth(70);
                coolingLimitMax.setDropDownWidth(70);
                coolingDeadBand.setDropDownWidth(70);
                heatingDeadBand.setDropDownWidth(70);
                buildingToZoneDiff.setDropDownWidth(70);

                for (int val = 32;  val <= 140; val += 1) {
                    list.add(val+"\u00B0F");
                }
                for (double val = 0;  val <= 20; val += 1) {
                    zoneSetBack.add(val+"\u00B0F");
                }
                for (int val = 50;  val <= 100; val += 1) {
                    heatingLimit.add(val+"\u00B0F");
                }
                for (int val = 50;  val <= 100; val += 1) {
                    coolingLimit.add(val+"\u00B0F");
                }
                for (double val = 0;  val <= 10; val += 0.5) {
                    deadBand.add(val+"\u00B0F");
                }
                for (int val = 0;  val <= 20; val += 1) {
                    zonediff.add(val+"\u00B0F");
                }
            }

            ArrayAdapter<String> zoneDiffadapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zonediff);
            zoneDiffadapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            buildingToZoneDiff.setAdapter(zoneDiffadapter);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, list);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            buildingLimitMin.setAdapter(adapter);

            buildingLimitMax.setAdapter(adapter);

            ArrayAdapter<String> setbackadapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoneSetBack);
            setbackadapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            unoccupiedZoneSetback.setAdapter(setbackadapter);

            ArrayAdapter<String> heatingAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, heatingLimit);
            heatingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            heatingLimitMin.setAdapter(heatingAdapter);
            heatingLimitMax.setAdapter(heatingAdapter);

            ArrayAdapter<String> coolingAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, coolingLimit);
            coolingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            coolingLimitMin.setAdapter(coolingAdapter);
            coolingLimitMax.setAdapter(coolingAdapter);

            ArrayAdapter<String> deadBandAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, deadBand);
            deadBandAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            coolingDeadBand.setAdapter(deadBandAdapter);
            heatingDeadBand.setAdapter(deadBandAdapter);


            double coolDBVal = HSUtil.getLevelValueFrom16(coolingDeadbandObj.get("id").toString());
            double heatDBVal = HSUtil.getLevelValueFrom16(heatingDeadbandObj.get("id").toString());
            double heatMinVal = HSUtil.getLevelValueFrom16(buildingHeatingUpperLimit.get("id").toString());
            double coolMinVal = HSUtil.getLevelValueFrom16(buildingCoolingLowerLimit.get("id").toString());
            double heatMaxVal = HSUtil.getLevelValueFrom16(buildingHeatingLowerLimit.get("id").toString());
            double coolMaxVal = HSUtil.getLevelValueFrom16(buildingCoolingUpperLimit.get("id").toString());
            double setBack = HSUtil.getLevelValueFrom16(unoccupiedZoneObj.get("id").toString());

            buildingToZoneDiff.setSelection(zoneDiffadapter.getPosition(
                    getAdapterValDiff(HSUtil.getLevelValueFrom16(buildingToZoneDiffObj.get("id").toString()))));
            coolingDeadBand.setSelection(deadBandAdapter.getPosition(
                    getAdapterValDeadBand(coolDBVal == 0.0 ? 2: coolDBVal, false)));
            heatingDeadBand.setSelection(deadBandAdapter.getPosition(
                    getAdapterValDeadBand(heatDBVal == 0.0 ? 2 : heatDBVal, false)));
            heatingLimitMin.setSelection(heatingAdapter.getPosition(
                    getAdapterVal(heatMinVal == 0.0 ? 67 : heatMinVal, false)));
            coolingLimitMin.setSelection(coolingAdapter.getPosition(
                    getAdapterVal(coolMinVal == 0.0 ? 72: coolMinVal, false)));
            heatingLimitMax.setSelection(heatingAdapter.getPosition(
                    getAdapterVal(heatMaxVal == 0.0 ?72 :heatMaxVal, false)));
            coolingLimitMax.setSelection(coolingAdapter.getPosition(
                    getAdapterVal(coolMaxVal == 0.0 ?77 :coolMaxVal, false)));
            unoccupiedZoneSetback.setSelection(setbackadapter.getPosition(
                    getAdapterValDeadBand(setBack == 0.0 ? 5:setBack, false)));
            buildingLimitMin.setSelection(adapter.getPosition(
                    getAdapterVal(HSUtil.getLevelValueFrom16(buildingMin.get("id").toString()), false)));
            buildingLimitMax.setSelection(adapter.getPosition(
                    getAdapterVal(HSUtil.getLevelValueFrom16(buildingMax.get("id").toString()), false)));


            dialog.findViewById(R.id.btnCancel).setOnClickListener(view -> dialog.dismiss());
            dialog.findViewById(R.id.btnClose).setOnClickListener(view -> dialog.dismiss());


            dialog.findViewById(R.id.btnSet).setOnClickListener(view ->

                    /*
                    LOWER_BUILDING_LIMIT,
                    UPPER_BUILDING_LIMIT,
                    LOWER_HEATING_LIMIT,
                    UPPER_HEATING_LIMIT,
                    LOWER_COOLING_LIMIT,
                    UPPER_COOLING_LIMIT
                     */{
                ProgressDialogUtils.showProgressDialog(getContext(), "Validating...");

                ArrayList<Schedule> scheduleList = new ArrayList<>();
                HashMap<Object,Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
                final String[] warning = new String[1];

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {
                    HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
                    HDict tDict = new HDictBuilder().add("filter", "schedule and days and siteRef == " + siteMap.get("id").toString()).toDict();
                    HGrid schedulePoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
                    if (schedulePoint != null) {
                        Iterator it = schedulePoint.iterator();
                        while (it.hasNext()) {
                            HRow r = (HRow) it.next();
                            scheduleList.add(new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build());
                        }
                    }
                    handler.post(() -> {
                        ProgressDialogUtils.hideProgressDialog();
                        if(scheduleList.isEmpty()){
                            Toast.makeText(getContext(),"Unable to fetch global schedules",Toast.LENGTH_LONG).show();
                            warning[0] = "Nope";
                        }else{
                            warning[0] = MasterControlUtil.isValidData(scheduleList,heatingLimitMax.getSelectedItem().toString(),
                                    heatingLimitMin.getSelectedItem().toString(),
                                    coolingLimitMax.getSelectedItem().toString(),
                                    coolingLimitMin.getSelectedItem().toString(),
                                    coolingDeadBand.getSelectedItem().toString(),
                                    heatingDeadBand.getSelectedItem().toString(),
                                    buildingLimitMin.getSelectedItem().toString(),
                                    buildingLimitMax.getSelectedItem().toString(),
                                    unoccupiedZoneSetback.getSelectedItem().toString(),
                                    buildingToZoneDiff.getSelectedItem().toString());
                            if (warning[0] == null) {
                                masterControlView.saveUserLimitChange(1, buildingLimitMin.getSelectedItem().toString());
                                masterControlView.saveUserLimitChange(2, buildingLimitMax.getSelectedItem().toString());
                                masterControlView.saveUserLimitChange(3, heatingLimitMax.getSelectedItem().toString());
                                masterControlView.saveUserLimitChange(4, heatingLimitMin.getSelectedItem().toString());
                                masterControlView.saveUserLimitChange(5, coolingLimitMin.getSelectedItem().toString());
                                masterControlView.saveUserLimitChange(6, coolingLimitMax.getSelectedItem().toString());
                                masterControlView.updateBuildingToZoneDiff( buildingToZoneDiff.getSelectedItem().toString());
                                masterControlView.updateDeadBand("cooling", coolingDeadBand.getSelectedItem().toString());
                                masterControlView.updateDeadBand("heating", heatingDeadBand.getSelectedItem().toString());
                                masterControlView.updateUnoccupiedZoneSetBack(unoccupiedZoneSetback.getSelectedItem().toString());
                                masterControlView.setTuner(dialog);
                            } else {

                                android.app.AlertDialog.Builder builder =
                                        new android.app.AlertDialog.Builder(getActivity());
                                builder.setMessage(warning[0]);
                                builder.setCancelable(false);
                                builder.setTitle(R.string.schedule_error);
                                builder.setIcon(R.drawable.ic_alert);
                                builder.setNegativeButton("OKAY", (dialog1, id) -> {
                                    dialog1.dismiss();
                                });

                                AlertDialog alert = builder.create();
                                alert.show();

                            }
                        }

                    });
                });






/*                         String warning = MasterControlUtil.isValidData(zones, heatingLimitMax.getSelectedItem().toString(),
                                 heatingLimitMin.getSelectedItem().toString(),
                                 coolingLimitMax.getSelectedItem().toString(),
                                 coolingLimitMin.getSelectedItem().toString(),
                                 coolingDeadBand.getSelectedItem().toString(),
                                 heatingDeadBand.getSelectedItem().toString(),
                                 buildingLimitMin.getSelectedItem().toString(),
                                 buildingLimitMax.getSelectedItem().toString(),
                                 unoccupiedZoneSetback.getSelectedItem().toString(),
                                 buildingToZoneDiff.getSelectedItem().toString());*/

            });



            masterControlView.setOnClickChangeListener(onSaveChangeListener);

            new CountDownTimer(100, 100) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    masterControlView.setMasterControl((float) MasterControlUtil.zoneMaxHeatingVal()
                            , (float) MasterControlUtil.zoneMinHeatingVal(),(float) MasterControlUtil.zoneMinCoolingVal(),
                            (float)MasterControlUtil.zoneMaxCoolingVal(), lowerBuildingTemp, upperBuildingTemp,
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
                    SchedulabeLimits.Companion.addSchedulableLimits(true,null,null);
                    DefaultSchedules.setDefaultCoolingHeatingTemp();
//                    DefaultSchedules.generateDefaultSchedule(false, null);
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
            }else {
                textNetworkError.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setBackFillTimeSpinner(View rootView) {

        this.backFillTimeSpinner = rootView.findViewById(R.id.spinnerBackfillTime);
        this.backFillTimeSpinner.setAdapter(getBackFillTimeArrayAdapter(getContext()));
        this.backFillTimeSpinner.setSelection(backfieldTimeSelectedValue(getBackFillTimeArrayAdapter(getContext())));
        this.backFillTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (backfieldTimeSelectedValue(getBackFillTimeArrayAdapter(getContext())) == i) {
                    linearLayout.setVisibility(View.INVISIBLE);
                } else {
                    if (!isFreshRegister) {
                        linearLayout.setVisibility(View.VISIBLE);
                    }
                }
                adapterView.setSelection(i);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void getRegisteredAddressBand() {
        RxjavaUtil.executeBackgroundTask(
                ()->{
                    regAddressBands.clear();
                },
                ()->{
                    HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
                    String siteUID = CCUHsApi.getInstance().getSiteIdRef().toString();
                    HDict tDict = new HDictBuilder().add("filter", "equip and group and siteRef == " + siteUID).toDict();
                    HGrid addressPoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
                    if(addressPoint == null) {
                        Log.w("RegisterGatherCCUDetails","HGrid(schedulePoint) is null.");
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getActivity(),"Couldn't find the remote node addresses, Please choose the node address which is not used already.", Toast.LENGTH_LONG).show();
                            setNodeAddress();
                        });
                    }
                    Iterator it = addressPoint.iterator();
                    while (it.hasNext())
                    {
                        HRow r = (HRow) it.next();
                        if (r.getStr("group") != null) {
                            regAddressBands.add(r.getStr("group"));
                        }
                    }
                },
                ()->{
                    for(int i = 0; i < regAddressBands.size(); i++)
                    {
                        for(int j = 0; j < addressBand.size(); j++)
                        {
                            if(regAddressBands.get(i).equals(addressBand.get(j)))
                            {
                                addressBand.remove(regAddressBands.get(i));
                            }
                        }
                    }
                    setNodeAddress();
                }
        );
    }

    public void setNodeAddress(){
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
    }
}
