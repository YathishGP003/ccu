package a75f.io.renatus.registration;

import static a75f.io.api.haystack.util.SchedulableMigrationKt.validateMigration;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_CONFIGURATION;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_DEVICE_INSTANCE_NUMBER;
import static a75f.io.logic.util.bacnet.BacnetUtilKt.sendBroadCast;
import static a75f.io.logic.L.ccu;
import static a75f.io.logic.bo.util.UnitUtils.celsiusToFahrenheit;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsiusRelative;
import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;
import static a75f.io.logic.service.FileBackupJobReceiver.performConfigFileBackup;
import static a75f.io.renatus.SettingsFragment.ACTION_SETTING_SCREEN;
import static a75f.io.renatus.util.BackFillViewModel.BackFillDuration;
import static a75f.io.renatus.util.BackFillViewModel.backfieldTimeSelectedValue;
import static a75f.io.renatus.util.BackFillViewModel.generateToastMessage;
import static a75f.io.renatus.util.BackFillViewModel.getBackFillDuration;
import static a75f.io.renatus.util.BackFillViewModel.getBackFillTimeArrayAdapter;
import static a75f.io.renatus.util.extension.FragmentContextKt.showMigrationErrorDialog;
import static a75f.io.renatus.views.MasterControl.MasterControlUtil.getAdapterVal;
import static a75f.io.renatus.views.MasterControl.MasterControlUtil.getAdapterValDeadBand;
import static a75f.io.renatus.views.MasterControl.MasterControlUtil.getAdapterValDiff;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONException;
import org.json.JSONObject;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.UnknownRecException;
import org.projecthaystack.client.HClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BackfillUtilKt;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu;
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.bo.util.DemandResponseMode;
import a75f.io.logic.interfaces.MasterControlLimitListener;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerEquip;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.util.OfflineModeUtilKt;
import a75f.io.messaging.exceptions.ScheduleMigrationNotComplete;
import a75f.io.messaging.handler.UpdatePointHandler;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.UtilityApplication;
import a75f.io.renatus.buildingoccupancy.BuildingOccupancyFragment;
import a75f.io.renatus.profiles.system.advancedahu.dab.DabAdvancedHybridAhuFragment;
import a75f.io.renatus.profiles.system.advancedahu.vav.VavAdvancedHybridAhuFragment;
import a75f.io.renatus.tuners.TunerFragment;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.TemperatureModeUtil;
import a75f.io.renatus.views.CustomCCUSwitch;
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter;
import a75f.io.renatus.views.MasterControl.MasterControlUtil;
import a75f.io.renatus.views.MasterControl.MasterControlView;
import a75f.io.renatus.views.TempLimit.TempLimitView;
import a75f.io.util.ExecutorTask;

public class InstallerOptions extends Fragment implements MasterControlLimitListener {
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
    String addressBandSelected = "1000";
    Prefs prefs;
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
    CustomCCUSwitch toggleCelsius;
    TextView textCelsiusEnable;

    CustomCCUSwitch toggleOfflineMode;
    TextView textOfflineMode;
    TextView descOfflineMode;

    UtilityApplication utilityApplication;
    LinearLayout linearLayout;
    View toastLayout;

    private CustomCCUSwitch toggleCoolingLockout;
    private CustomCCUSwitch toggleHeatingLockout;
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
    CustomCCUSwitch switchDREnrollment;
    private Spinner temperatureModeSpinner;
    private static final String TAG = InstallerOptions.class.getSimpleName();
    ArrayList<String> regAddressBands = new ArrayList<>();
    ArrayList<String> addressBand = new ArrayList<>();
    private View toastWarning;
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
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //((FreshRegistration)getActivity()).showIcons(false);
        View rootView = inflater.inflate(R.layout.fragment_installeroption, container, false);

        mContext = getContext().getApplicationContext();
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();

        prefs = new Prefs(mContext);
        isFreshRegister = getActivity() instanceof FreshRegistration;
        CCU_ID = prefs.getString("CCU_ID");
        utilityApplication = new RenatusApp();

        UpdatePointHandler.setMasterControlLimitListener(this);

        imageGoback = rootView.findViewById(R.id.imageGoback);
        mAddressBandSpinner = rootView.findViewById(R.id.spinnerAddress);
        CCUUiUtil.setSpinnerDropDownColor(mAddressBandSpinner,getContext());
        mToggleTempAll = rootView.findViewById(R.id.toggleTempAll);
        mNext = rootView.findViewById(R.id.buttonNext);
        imageTemp = rootView.findViewById(R.id.imageTemp);

        //BACnet Setup UI Components
        toggleCelsius= rootView.findViewById(R.id.toggleCelsius);
        textCelsiusEnable = rootView.findViewById(R.id.textUseCelsius);

        toggleOfflineMode = rootView.findViewById(R.id.offlineModetoggle);
        textOfflineMode = rootView.findViewById(R.id.textofflineMode);
        descOfflineMode = rootView.findViewById(R.id.textofflineModeDesc);

        linearLayout = rootView.findViewById(R.id.layoutFooterButtons);
        Button buttonApply = rootView.findViewById(R.id.buttonApply);
        Button buttonCancel = rootView.findViewById(R.id.buttonCancel);
        linearLayout = rootView.findViewById(R.id.layoutFooterButtons);
        LayoutInflater li = getLayoutInflater();
        toastLayout = li.inflate(R.layout.custom_toast_layout_backfill, rootView.findViewById(R.id.custom_toast_layout_backfill));

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
        switchDREnrollment = rootView.findViewById(R.id.switchDRMode);

        initializeTempLockoutUI(ccuHsApi);
		HRef ccuId = ccuHsApi.getCcuRef();
        String ccuUid = null;

        textCelsiusEnable.setVisibility(View.VISIBLE);
        toggleCelsius.setVisibility(View.VISIBLE);

        toggleOfflineMode.setVisibility(View.VISIBLE);
        textOfflineMode.setVisibility(View.VISIBLE);
        descOfflineMode.setVisibility(View.VISIBLE);

        toastWarning = getLayoutInflater().inflate(R.layout.custom_toast_layout_warning, rootView.findViewById(R.id.custom_toast_layout_warning));

        setToggleCheck();

        toggleOfflineMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            double offlineValue = isChecked ? 1.0 : 0.0;
            ccuHsApi.writeDefaultVal("offline and mode ",offlineValue);
            ccuHsApi.writeHisValByQuery("offline and mode ",offlineValue);
            TunerUtil.updateDefault(offlineValue);
        });

        if (ccuId != null) {
            ccuUid = ccuHsApi.getCcuRef().toString();
        }

        for (int addr = 1000; addr <= 10900; addr += 100) {
            addressBand.add(String.valueOf(addr));
        }

        ArrayList<HashMap> equipments = ccuHsApi.readAll("equip and zone or (equip and oao and not hyperstatsplit) or equip and connectModule");
        if (equipments.isEmpty())
            getRegisteredAddressBand(); // doing this when no equips available
        else
            setNodeAddress();

        mAddressBandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                CcuLog.d(L.TAG_CCU, "AddressBandSelected : " + mAddressBandSpinner.getSelectedItem());
                if (i >= 0) {
                    addressBandSelected = mAddressBandSpinner.getSelectedItem().toString();
                    L.ccu().setAddressBand(Short.parseShort(addressBandSelected));
                    if (!isFreshRegister){
                        String addressBand = Domain.ccuEquip.getAddressBandValue();
                        if(addressBand != null && !addressBandSelected.equals(addressBand) && regAddressBands.contains(addressBandSelected)) {
                            Toast toast = new Toast(Globals.getInstance().getApplicationContext());
                            toast.setGravity(Gravity.BOTTOM, 50, 50);
                            toast.setView(toastWarning);
                            toast.setDuration(Toast.LENGTH_LONG);
                            toast.show();
                        }
                        Domain.ccuEquip.updateAddressBand(addressBandSelected);
                        regAddressBands.remove(addressBand);
                        regAddressBands.add(addressBandSelected);
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

        if (!isFreshRegister) {
            ArrayList<HashMap> equips = ccuHsApi.readAll("equip and (zone or bypassDamper or oao)");
            mAddressBandSpinner.setEnabled(equips == null || equips.isEmpty());
        }

        imageTemp.setOnClickListener(view -> {
            try {
                openMasterControllerDialog();
            } catch (ScheduleMigrationNotComplete e) {
                e.printStackTrace();
                if (!validateMigration()) {
                    showMigrationErrorDialog(requireContext());
                }
            }
        });

        getTempValues();

        toggleCelsius.setOnCheckedChangeListener((buttonView, isChecked) -> {
            HashMap<Object, Object> useCelsius = ccuHsApi.readEntity("displayUnit");
            intimateAdvanceAhu();
            if (!useCelsius.isEmpty()) {
                if (isChecked) {
                    ccuHsApi.writePoint(useCelsius.get("id").toString(), TunerConstants.TUNER_BUILDING_VAL_LEVEL,
                            ccuHsApi.getCCUUserName(), 1.0, 0);
                } else {
                    ccuHsApi.writePoint(useCelsius.get("id").toString(), TunerConstants.TUNER_BUILDING_VAL_LEVEL,
                            ccuHsApi.getCCUUserName(), 0.0, 0);
                }
            } else {
                Toast.makeText(getContext(), "To enable \"Use Celsius\" feature on this device upgrade Primary CCU.\nRestart app on this device after Primary CCU is Upgraded.", Toast.LENGTH_LONG).show();
            }
            getTempValues();
            initializeTempLockoutUI(ccuHsApi);
            if (TunerFragment.newInstance().tunerExpandableLayoutHelper != null) {
                TunerFragment.newInstance().tunerExpandableLayoutHelper.notifyDataSetChanged();
            }
            toggleCelsius.setEnabled(false);
            new Handler().postDelayed(() -> toggleCelsius.setEnabled(true),2000);
        });




        getActivity().registerReceiver(mPairingReceiver, new IntentFilter(ACTION_SETTING_SCREEN));

        setBackFillTimeSpinner(rootView);
        setUpDREnrollmentMode(ccuHsApi);
        setUpTemperatureModeSpinner(rootView, ccuHsApi);

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

    private void intimateAdvanceAhu() {
        if (ccu().systemProfile instanceof VavAdvancedAhu && VavAdvancedHybridAhuFragment.Companion.getInstance() != null) {
            VavAdvancedHybridAhuFragment.Companion.getInstance().getViewModel().toggleChecked();
        }
        if (ccu().systemProfile instanceof DabAdvancedAhu && DabAdvancedHybridAhuFragment.Companion.getInstance() != null) {
            DabAdvancedHybridAhuFragment.Companion.getInstance().getViewModel().toggleChecked();
        }
    }

    private void setUpTemperatureModeSpinner(View rootView, CCUHsApi ccuHsApi) {
        this.temperatureModeSpinner = rootView.findViewById(R.id.spinnerTemperatureMode);
        CCUUiUtil.setSpinnerDropDownColor(temperatureModeSpinner,this.getContext());
        ArrayAdapter<String> temperatureModeAdapter = getAdapterValue(new TemperatureModeUtil().getTemperatureModeArray());
        this.temperatureModeSpinner.setAdapter(temperatureModeAdapter);
        this.temperatureModeSpinner.setSelection((int) new TemperatureModeUtil().getTemperatureMode());
        this.temperatureModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                new TemperatureModeUtil().setTemperatureMode(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void setUpDREnrollmentMode(CCUHsApi ccuHsApi) {
        switchDREnrollment.setChecked(DemandResponseMode.isDREnrollmentSelected());
        switchDREnrollment.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CcuLog.i(L.TAG_CCU_DR_MODE,"Demand response point status is : "+isChecked);
            DemandResponseMode.handleDRActivationConfiguration(isChecked, ccuHsApi);
        });
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


    public void setToggleCheck() {
        boolean newValue = isCelsiusTunerAvailableStatus();
        if ( toggleCelsius != null && toggleCelsius.isChecked() != newValue) {
            toggleCelsius.setChecked(newValue);
        }

        boolean isOfflineMode = CCUHsApi.getInstance().readDefaultVal("offline and mode") > 0;
        if ( toggleOfflineMode != null && toggleOfflineMode.isChecked() != isOfflineMode) {
            toggleOfflineMode.setChecked(isOfflineMode);
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
                    ccu().systemProfile.setCoolingLockoutVal(hayStack, Math.round(celsiusToFahrenheit(Double.parseDouble(spinnerCoolingLockoutTemp.getSelectedItem().toString()))));
                } else {
                    ccu().systemProfile.setCoolingLockoutVal(hayStack, Double.parseDouble(spinnerCoolingLockoutTemp.getSelectedItem().toString()));
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
                    ccu().systemProfile.setHeatingLockoutVal(hayStack,
                            Math.round(celsiusToFahrenheit(Double.parseDouble(spinnerHeatingLockoutTemp.getSelectedItem().toString()))));
                } else {
                    ccu().systemProfile.setHeatingLockoutVal(hayStack,
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

    // initial master control values
    private void getTempValues() {

        hdb = (float) Domain.buildingEquip.getHeatingDeadband().readPriorityVal();
        cdb = (float) Domain.buildingEquip.getCoolingDeadband().readPriorityVal();
        upperCoolingTemp = (float) Domain.buildingEquip.getCoolingUserLimitMax().readPriorityVal();
        lowerCoolingTemp = (float) Domain.buildingEquip.getCoolingUserLimitMin().readPriorityVal();
        upperHeatingTemp = (float) Domain.buildingEquip.getHeatingUserLimitMax().readPriorityVal();
        lowerHeatingTemp = (float) Domain.buildingEquip.getHeatingUserLimitMin().readPriorityVal();

        lowerBuildingTemp = (float) Domain.buildingEquip.getBuildingLimitMin().readPriorityVal();
        upperBuildingTemp = (float) Domain.buildingEquip.getBuildingLimitMax().readPriorityVal();
        mSetBack = (float) Domain.buildingEquip.getUnoccupiedZoneSetback().readPriorityVal();
        zoneDiff = (float) Domain.buildingEquip.getBuildingToZoneDifferential().readPriorityVal();
        CcuLog.i(Domain.LOG_TAG,hdb+" "+cdb+" "+upperBuildingTemp+" "+lowerCoolingTemp+
                " "+upperHeatingTemp+" "+lowerHeatingTemp+" "+lowerBuildingTemp+" "+upperBuildingTemp+
                " "+mSetBack+" "+zoneDiff);

    }

    //custom dialog to control building temperature
    private void openMasterControllerDialog() throws ScheduleMigrationNotComplete {
        if (isAdded()) {

            final Dialog dialog = new Dialog(getActivity());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.dialog_master_control);
            MasterControlView masterControlView = dialog.findViewById(R.id.masterControlView);

            //12950- New User limits UI

            double heatDBVal;
            double coolDBVal;
            double coolMaxVal;
            double coolMinVal;
            double heatMinVal;
            double heatMaxVal;
            double setBack;
            double buildingMinLimit;
            double buildingMaxLimit;
            double zoneDiff;

            try {
                heatDBVal = (float) Domain.buildingEquip.getHeatingDeadband().readPriorityVal();
                coolDBVal = (float) Domain.buildingEquip.getCoolingDeadband().readPriorityVal();
                coolMaxVal = (float) Domain.buildingEquip.getCoolingUserLimitMax().readPriorityVal();
                coolMinVal = (float) Domain.buildingEquip.getCoolingUserLimitMin().readPriorityVal();
                heatMinVal = (float) Domain.buildingEquip.getHeatingUserLimitMin().readPriorityVal();
                heatMaxVal = (float) Domain.buildingEquip.getHeatingUserLimitMax().readPriorityVal();
                setBack = (float) Domain.buildingEquip.getUnoccupiedZoneSetback().readPriorityVal();
                buildingMinLimit = (float) Domain.buildingEquip.getBuildingLimitMin().readPriorityVal();
                buildingMaxLimit = (float) Domain.buildingEquip.getBuildingLimitMax().readPriorityVal();
                zoneDiff = (float) Domain.buildingEquip.getBuildingToZoneDifferential().readPriorityVal();
            } catch (UnknownRecException e) {
                throw new ScheduleMigrationNotComplete("Schedule revamp migration is not completed");
            }

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

            double minDeadBandVal = Double.parseDouble(Domain.readPointForEquip(DomainName.coolingDeadband,Domain.buildingEquip.getEquipRef()).get("minVal").toString());
            double maxDeadBandVal = Double.parseDouble(Domain.readPointForEquip(DomainName.coolingDeadband,Domain.buildingEquip.getEquipRef()).get("maxVal").toString());

            if(isCelsiusTunerAvailableStatus()){
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
                for (double val = minDeadBandVal;  val <= maxDeadBandVal; val += 0.5) {
                    deadBand.add(val+"\u00B0F  (" + (fahrenheitToCelsiusRelative(val)) + "\u00B0C)");
                }
                for (int val = 0;  val <= 20; val += 1) {
                    zonediff.add(val+"\u00B0F  (" + (fahrenheitToCelsiusRelative(val)) + "\u00B0C)");
                }

            }else{

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
                for (double val = minDeadBandVal;  val <= maxDeadBandVal; val += 0.5) {
                    deadBand.add(val+"\u00B0F");
                }
                for (int val = 0;  val <= 20; val += 1) {
                    zonediff.add(val+"\u00B0F");
                }
            }

            ArrayAdapter<String> zoneDiffadapter = getAdapterValue(zonediff);
            zoneDiffadapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            buildingToZoneDiff.setAdapter(zoneDiffadapter);

            ArrayAdapter<String> adapter = getAdapterValue(list);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            buildingLimitMin.setAdapter(adapter);

            buildingLimitMax.setAdapter(adapter);

            ArrayAdapter<String> setbackadapter = getAdapterValue(zoneSetBack);
            setbackadapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            unoccupiedZoneSetback.setAdapter(setbackadapter);

            ArrayAdapter<String> heatingAdapter = getAdapterValue(heatingLimit);
            heatingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            heatingLimitMin.setAdapter(heatingAdapter);
            heatingLimitMax.setAdapter(heatingAdapter);

            ArrayAdapter<String> coolingAdapter = getAdapterValue(coolingLimit);
            coolingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            coolingLimitMin.setAdapter(coolingAdapter);
            coolingLimitMax.setAdapter(coolingAdapter);

            ArrayAdapter<String> deadBandAdapter = getAdapterValue(deadBand);
            deadBandAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            coolingDeadBand.setAdapter(deadBandAdapter);
            heatingDeadBand.setAdapter(deadBandAdapter);

            buildingToZoneDiff.setSelection(zoneDiffadapter.getPosition(
                    getAdapterValDiff(zoneDiff)));
            coolingDeadBand.setSelection(deadBandAdapter.getPosition(
                    getAdapterValDeadBand(//coolDBVal == 0.0 ? 2:
                            coolDBVal, false)));
            heatingDeadBand.setSelection(deadBandAdapter.getPosition(
                    getAdapterValDeadBand(//heatDBVal == 0.0 ? 2 :
                            heatDBVal, false)));
            heatingLimitMin.setSelection(heatingAdapter.getPosition(
                    getAdapterVal(heatMinVal == 0.0 ? 67 : heatMinVal, false)));
            coolingLimitMin.setSelection(coolingAdapter.getPosition(
                    getAdapterVal(coolMinVal == 0.0 ? 72: coolMinVal, false)));
            heatingLimitMax.setSelection(heatingAdapter.getPosition(
                    getAdapterVal(heatMaxVal == 0.0 ?72 :heatMaxVal, false)));
            coolingLimitMax.setSelection(coolingAdapter.getPosition(
                    getAdapterVal(coolMaxVal == 0.0 ?77 :coolMaxVal, false)));
            unoccupiedZoneSetback.setSelection(setbackadapter.getPosition(
                    getAdapterValDeadBand(//setBack == 0.0 ? 5:
                            setBack, false)));
            buildingLimitMin.setSelection(adapter.getPosition(
                    getAdapterVal(buildingMinLimit, false)));
            buildingLimitMax.setSelection(adapter.getPosition(
                    getAdapterVal(buildingMaxLimit, false)));


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
                List<Zone> zones = new ArrayList<>();
                List<Equip> equipList = new ArrayList<>();
                HashMap<Object,Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
                final String[] warning = new String[1];

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(() -> {
                    HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
                    String siteRef = siteMap.get("id").toString();
                    if(OfflineModeUtilKt.isOfflineMode()){
                        ArrayList<HashMap<Object, Object>> schedules = CCUHsApi.getInstance().readAllEntities
                                ("schedule and days and siteRef ==  " +siteRef );

                        List<HashMap<Object, Object>> namedSchedule = CCUHsApi.getInstance().getAllNamedSchedules();

                        ArrayList<HashMap<Object, Object>> zoneList = CCUHsApi.getInstance().readAllEntities
                                ("room");

                        for (HashMap<Object, Object> schedule:schedules) {
                            scheduleList.add(CCUHsApi.getInstance().getScheduleById(schedule.get("id").toString()));
                        }
                        for (HashMap<Object, Object> schedule:namedSchedule) {
                            scheduleList.add(CCUHsApi.getInstance().getScheduleById(schedule.get("id").toString()));
                        }

                        for (HashMap<Object, Object> m : zoneList)
                        {
                            zones.add(new Zone.Builder().setHashMap(m).build());
                        }

                        ArrayList<HashMap<Object, Object>> equipMaps = CCUHsApi.getInstance().
                                readAllEntities("equip and siteRef ==  \"" +siteRef + "\"");
                        if(equipMaps != null) {
                            for (HashMap equip : equipMaps) {
                                equipList.add(new Equip.Builder().setHashMap(equip).build());
                            }
                        }

                    }else {
                        HDict tDict = new HDictBuilder().add("filter", "schedule and days and siteRef == " + siteMap.get("id").toString()).toDict();
                        HGrid schedulePoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
                        if (schedulePoint != null) {
                            Iterator it = schedulePoint.iterator();
                            while (it.hasNext()) {
                                HRow r = (HRow) it.next();
                                scheduleList.add(new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build());
                            }
                        }
                        HDict zoneDict = new HDictBuilder().add("filter", "room and siteRef == " + CCUHsApi.getInstance().readEntity("site").get("id").toString()).toDict();
                        HGrid zoneGrid = hClient.call("read", HGridBuilder.dictToGrid(zoneDict));
                        if (zoneGrid != null) {
                            List<HashMap> zoneMaps = CCUHsApi.getInstance().HGridToList(zoneGrid);
                            for (HashMap zone : zoneMaps) {
                                zones.add(new Zone.Builder().setHashMap(zone).build());
                            }
                        }
                        HDict equipDict = new HDictBuilder().add("filter", "equip and siteRef == " + CCUHsApi.getInstance().readEntity("site").get("id").toString()).toDict();
                        HGrid equipGrid = hClient.call("read", HGridBuilder.dictToGrid(equipDict));
                        if (equipGrid != null) {
                            List<HashMap> equipMaps = CCUHsApi.getInstance().HGridToList(equipGrid);
                            for (HashMap equip : equipMaps) {
                                equipList.add(new Equip.Builder().setHashMap(equip).build());
                            }
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
                                    buildingToZoneDiff.getSelectedItem().toString(), zones, equipList);
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
                                builder.setNegativeButton("OKAY", (dialog1, id) -> dialog1.dismiss());

                                AlertDialog alert = builder.create();
                                alert.show();

                            }
                        }

                    });
                });

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
                            mSetBack, (float) zoneDiff, hdb, cdb);
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

    private void goToNext() {
        ExecutorTask.executeAsync(
                () -> {
                    if (!Globals.getInstance().siteAlreadyCreated()) {
                        TunerEquip.INSTANCE.initialize(CCUHsApi.getInstance(), false);
                        //BuildingTuners.getInstance();
                        //SchedulabeLimits.Companion.addSchedulableLimits(true,null,null);
                        DefaultSchedules.setDefaultCoolingHeatingTemp();
//                    DefaultSchedules.generateDefaultSchedule(false, null);
                    }

                    L.saveCCUState();
                    CCUHsApi.getInstance().log();
                    CCUHsApi.getInstance().syncEntityTree();
                },
                () -> {
                    prefs.setBoolean("PROFILE_SETUP", false);
                    prefs.setBoolean("CCU_SETUP", true);
                    ((FreshRegistration) getActivity()).selectItem(5);
                }
        );
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
                    ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and (zone or bypassDamper or oao)");
                    mAddressBandSpinner.setEnabled(equips == null || equips.isEmpty());
                }
            }
        }
    };

    private void setBackFillTimeSpinner(View rootView) {

        this.backFillTimeSpinner = rootView.findViewById(R.id.spinnerBackfillTime);
        CCUUiUtil.setSpinnerDropDownColor(backFillTimeSpinner,this.getContext());
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
        ExecutorTask.executeAsync(
                ()-> regAddressBands.clear(),
                ()->{
                    HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
                    String siteUID = CCUHsApi.getInstance().getSiteIdRef().toString();
                    HDict tDict = new HDictBuilder().add("filter", "domainName == \"" + DomainName.addressBand + "\" and siteRef == " + siteUID).toDict();
                    HGrid addressPoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
                    if(addressPoint == null) {
                        CcuLog.w(L.TAG_CCU_REGISTER_GATHER_DETAILS,"HGrid(schedulePoint) is null.");
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getActivity(),"Couldn't find the remote node addresses, Please choose the node address which is not used already.", Toast.LENGTH_LONG).show();
                            setNodeAddress();
                        });
                    }
                    Iterator it = addressPoint.iterator();
                    while (it.hasNext())
                    {
                        HRow r = (HRow) it.next();
                        if (r.has("val")) {
                            regAddressBands.add(r.get("val").toString());
                        }
                    }
                },
                this::setNodeAddress
        );
    }

    public void setNodeAddress(){
        ArrayAdapter<String> analogAdapter = getAdapterValue(addressBand);
        analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mAddressBandSpinner.setAdapter(analogAdapter);

        boolean isCcuDeviceExists = Domain.ccuDevice.isCCUExists();
        //if ccu exists
        if (isCcuDeviceExists) {
            for (String addBand : addressBand) {
                String addB = String.valueOf(L.ccu().getAddressBand());
                if (addBand.equals(addB)) {
                    mAddressBandSpinner.setSelection(analogAdapter.getPosition(addBand),false);
                    break;
                }
            }
        } else {
            ccu().setAddressBand((short) 1000);
        }
    }
    private CustomSpinnerDropDownAdapter getAdapterValue(ArrayList values) {
        return new CustomSpinnerDropDownAdapter(requireContext(), R.layout.spinner_dropdown_item, values);
    }

    @Override
    public void onMasterControlLimitChanged() {
        if(imageTemp != null){
            imageTemp.updateData();
        }
    }
}
