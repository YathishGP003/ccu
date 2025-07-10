package a75f.io.renatus;

import static a75f.io.logic.L.BAC_APP_PACKAGE_NAME;
import static a75f.io.logic.L.TAG_CCU_BACNET_MSTP;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.APDU_SEGMENT_TIMEOUT;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.APDU_TIMEOUT;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_BBMD_CONFIGURATION;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_BBMD_CONFIGURATION_BACKUP;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_CONFIGURATION;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE_BBMD;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE_FD;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE_NORMAL;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_FD_AUTO_STATE;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_FD_CONFIGURATION;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_FD_CONFIGURATION_BACKUP;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_MSTP_CONFIGURATION;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_CONFIGURATION_TYPE;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_START;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_STOP;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.DESCRIPTION;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.EMPTY_STRING;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_ADDRESS;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_DEVICE_INSTANCE_NUMBER;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_DEVICE_OBJECT_NAME;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_INITIALIZED;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_MSTP_INITIALIZED;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.LOCAL_NETWORK_NUMBER;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.LOCATION;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.NETWORK_INTERFACE;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.NULL;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.NUMBER_OF_APDU_RETRIES;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.NUMBER_OF_NOTIFICATION_CLASS_OBJECTS;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.NUMBER_OF_OFFSET_VALUES;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.NUMBER_OF_SCHEDULE_OBJECTS;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.NUMBER_OF_TREND_LOG_OBJECTS;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PASSWORD;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PORT;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_BAUD_RATE;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_MAX_FRAME;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_MAX_MASTER;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_DEVICE_ID;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_PORT_ADDRESS;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_SOURCE_ADDRESS;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.VIRTUAL_NETWORK_NUMBER;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.ZONE_TO_VIRTUAL_DEVICE_MAPPING;
import static a75f.io.logic.util.bacnet.BacnetUtilKt.cancelScheduleJobToResubscribeBacnetMstpCOV;
import static a75f.io.logic.util.bacnet.BacnetUtilKt.sendBroadCast;
import static a75f.io.logic.L.TAG_CCU_BACNET;
import static a75f.io.logic.service.FileBackupJobReceiver.performConfigFileBackup;
import static a75f.io.renatus.UtilityApplication.context;
import static a75f.io.renatus.UtilityApplication.startRestServer;
import static a75f.io.renatus.UtilityApplication.stopRestServer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.interfaces.MstpDataInterface;
import a75f.io.logic.util.bacnet.BacnetUtilKt;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.renatus.bacnet.BacnetConfigChange;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.CustomSelectionAdapter;
import a75f.io.renatus.util.DataBbmd;
import a75f.io.renatus.util.DataBbmdObj;
import a75f.io.renatus.util.DataFd;
import a75f.io.renatus.util.DataFdObj;
import a75f.io.renatus.util.DataMstp;
import a75f.io.renatus.util.DataMstpObj;
import a75f.io.renatus.util.UsbHelper;
import a75f.io.renatus.views.CustomCCUSwitch;
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter;
import a75f.io.restserver.server.HttpServer;
import a75f.io.usbserial.UsbModbusService;
import a75f.io.usbserial.UsbPortTrigger;
import a75f.io.util.DashboardUtilKt;
import butterknife.BindView;
import butterknife.ButterKnife;

public class Communication extends Fragment implements MstpDataInterface {
    
    private final String PREF_MB_BAUD_RATE = "mb_baudrate";
    private final String PREF_MB_PARITY = "mb_parity";
    private final String PREF_MB_DATA_BITS = "mb_databits";
    private final String PREF_MB_STOP_BITS = "mb_stopbits";

    private String deviceIpAddress = "";
    private int portNumber = 47808;
    private int subnetMask = 1;
    private int bacnetConfigSelectedPosition = 2; // Default value mapped to Normal

    @BindView(R.id.spinnerBaudRate) Spinner spinnerBaudRate;
    
    @BindView(R.id.spinnerParity) Spinner spinnerParity;
    
    @BindView(R.id.spinnerDatabits) Spinner spinnerDatabits;
    
    @BindView(R.id.spinnerStopBits) Spinner spinnerStopbits;
    
    @BindView(R.id.btnRestart) Button btnRestart;

    @BindView(R.id.mstpSpinnerBaudRate) Spinner mstpSpinnerBaudRate;

    @BindView(R.id.tvMstpBaudRate) TextView tvMstpBaudRate;

    @BindView(R.id.etMstpSourceAddress) EditText etMstpSourceAddress;

    @BindView(R.id.tvMstpSourceAddress) TextView tvMstpSourceAddress;

    @BindView(R.id.etMstpMaxMaster) EditText etMstpMaxMaster;

    @BindView(R.id.etMstpDeviceId) EditText etMstpDeviceId;

    @BindView(R.id.tvMstpDeviceId) TextView tvMstpDeviceId;

    @BindView(R.id.tvMstpMaxMaster) TextView tvMstpMaxMaster;

    @BindView(R.id.etMstpMaxFrame) EditText etMstpMaxFrame;

    @BindView(R.id.tvMstpMaxFrame) TextView tvMstpMaxFrame;

    @BindView(R.id.tv_mstp_initialize) TextView btnMstpInitialize;

    @BindView(R.id.tv_mstp_disable) TextView btnMstpDisable;

    @BindView(R.id.tvMstpUsbSerialConnection) TextView usbSerial;

    @BindView(R.id.mstpConfigLayout) LinearLayout mstpConfigLayout;

    @BindView(R.id.tv1IPDeviceObjectName) TextView tvIPDeviceObjectName;

    @BindView(R.id.toggleZoneToVirtualDeviceMapping)
    CustomCCUSwitch toggleZoneToVirtualDeviceMapping;

    @BindView(R.id.etDescription) EditText description;

    @BindView(R.id.tvDescription1) TextView tvDescription;

    @BindView(R.id.etLocation) EditText location;

    @BindView(R.id.tvLocation1) TextView tvLocation;

    @BindView(R.id.layoutTextInput) TextInputLayout textInputLayout;

    @BindView(R.id.etPassword) EditText password;

    @BindView(R.id.tvPassword1) TextView tvPassword;


    @BindView(R.id.ipSpinner) Spinner ipAddressSpinner;

    @BindView(R.id.etlocalNetworkAddress) EditText localNetworkNumber;

    @BindView(R.id.tvLocalNetworkAddress1) TextView tvLocalNetworkNumber;

    @BindView(R.id.etVirtualNetworkAddress) EditText virtualNetworkNumber;

    @BindView(R.id.tvVirtualNetworkAddress) TextView tvVirtualNetworkAddress;

    @BindView(R.id.etPort) EditText port;

    @BindView(R.id.tvPort1) TextView tvPort;

    @BindView(R.id.etIPDeviceInstanceNumber) EditText ipDeviceInstanceNumber;

    @BindView(R.id.tvIPDeviceInstanceNumber1) TextView tvIPDeviceInstanceNumber;

    @BindView(R.id.etAPDU_Timeout) EditText apduTimeout;

    @BindView(R.id.tvAPDU_Timeout1) TextView tvAPDUTimeout;

    @BindView(R.id.etNumberofAPDURetries) EditText numberOfAPDURetries;

    @BindView(R.id.tvNumberofAPDURetries1) TextView tvNumberofAPDURetries;

    @BindView(R.id.etAPDUSegmentTimeout) EditText apduSegmentTimeout;

    @BindView(R.id.tvAPDUSegmentTimeout1) TextView tvApduSegmentTimeout;

    @BindView(R.id.etNotificationClassObjects) EditText etNotificationClassObjects;

    @BindView(R.id.tvNotificationClassObjects1) TextView tvNotificationClassObjects;

    @BindView(R.id.etTrendLogObjects) EditText etTrendLogObjects;

    @BindView(R.id.tvTrendLogObjects1) TextView tvTrendLogObjects;

    @BindView(R.id.etScheduleObjects) EditText etScheduleObjects;

    @BindView(R.id.tvScheduleObjects1) TextView tvScheduleObjects;

    @BindView(R.id.tvInitializeBACnet) TextView initializeBACnet;

    @BindView(R.id.tvDisableBACnet) TextView disableBACnet;

    @BindView(R.id.imZvdmHint) ImageView zvdmHint;

    @BindView(R.id.tvZvdmHint) TextView tvzvdmHint;


    @BindView(R.id.etOffsetValues) EditText etOffsetValues;

    @BindView(R.id.tvOffsetValue1) TextView tvOffsetValue;

    @BindView(R.id.fdInputView) View fdInputView;

    @BindView(R.id.bbmdInputContainer) View bbmdInputContainer;

    @BindView(R.id.normalView) View normalView;
    @BindView(R.id.etFdIp) EditText etFDIP;
    @BindView(R.id.etFdPort) EditText etFDPort;
    @BindView(R.id.etFdTime) EditText etFDTime;

    @BindView(R.id.tvFdSubmit) TextView tvFdSubmit;

    @BindView(R.id.tvNormalSubmit) View tvNormalSubmit;

    @BindView(R.id.bacnetConfigureLayout) RelativeLayout bacnetConfigureLayout;

    @BindView(R.id.tvBbmdAdd) View tvBbmdAdd;

    @BindView(R.id.tvBbmdSubmit) TextView tvBbmdSubmit;

    @BindView(R.id.bbmdInputViews)
    LinearLayout bbmdInputViews;

    @BindView(R.id.iv_refresh_ip)
    ImageView ivRefreshView;

    @BindView(R.id.tvBacAppVersion) TextView tvBacAppVersion;

    @BindView(R.id.bacnetConfigSpinner) Spinner bacnetConfigSpinner;

    SharedPreferences sharedPreferences;
    JSONObject config;
    JSONObject networkObject;
    JSONObject deviceObject;
    JSONObject objectConf;

    private View rootView;

    private ExecutorService executorService;

    boolean isZoneToVirtualDeviceErrorShowing = false;

    boolean isUSBSerialPortAvailable = false;

    private CustomSelectionAdapter<String> spinnerBacnetConfigAdapter;

    private String portAddress = "/dev/ttyUSB0";

    public Communication() {
    
    }
    
    public static Communication newInstance() {
        return new Communication();
    }

    private BacnetConfigChange bacnetConfigChangeListener = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
                                                                                                  Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_modbusconfig, container, false);
        ButterKnife.bind(this, rootView);
        executorService = Executors.newFixedThreadPool(1);
        bacnetConfigChangeListener = (BacnetConfigChange) requireActivity();
        return rootView;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        HttpServer.Companion.setMstpDataInterface(this);
        this.rootView = view;
        if(UtilityApplication.isBacnetMstpInitialized()) {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "mstp config found prepare for mstp init");
            executorService.submit(() -> {
                UsbHelper.runChmodUsbDevices();
                UsbPortTrigger.triggerUsbSerialBinding(requireContext());
                UsbHelper.listUsbDevices(requireContext());
                UsbHelper.runAsRoot("ls /dev/tty*");
                updateMstpUi();
            });
        }else{
            CcuLog.d(TAG_CCU_BACNET_MSTP, "no mstp config found");
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        String selectedDeviceType = sharedPreferences.getString(BACNET_DEVICE_TYPE, BACNET_DEVICE_TYPE_NORMAL);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mstpConfigLayout.setVisibility(View.VISIBLE);
        } else {
            mstpConfigLayout.setVisibility(View.GONE);
        }

        ArrayAdapter<String> spinnerBaudRateAdapter = getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray(R.array.mb_config_baudrate_array))));
        spinnerBaudRate.setAdapter(spinnerBaudRateAdapter);
        spinnerBaudRate.setSelection(((ArrayAdapter<String>)spinnerBaudRate.getAdapter())
                                         .getPosition(String.valueOf(readIntPref(PREF_MB_BAUD_RATE, 9600))));
        spinnerBaudRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                writeIntPref(PREF_MB_BAUD_RATE, Integer.parseInt(spinnerBaudRate.getSelectedItem().toString()));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter<String> spinnerParityAdapter = getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray(R.array.mb_config_parity_array))));
        spinnerParity.setAdapter(spinnerParityAdapter);
        spinnerParity.setSelection(readIntPref(PREF_MB_PARITY, 0),false);
        spinnerParity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                writeIntPref(PREF_MB_PARITY, position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter<String> spinnerDatabitsAdapter = getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray(R.array.mb_config_databits_array))));
        spinnerDatabits.setAdapter(spinnerDatabitsAdapter);
        spinnerDatabits.setSelection(((ArrayAdapter<String>)spinnerDatabits.getAdapter())
                       .getPosition(String.valueOf(readIntPref(PREF_MB_DATA_BITS, 8))),false);
        spinnerDatabits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                writeIntPref(PREF_MB_DATA_BITS, Integer.parseInt(spinnerDatabits.getSelectedItem().toString()));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });

        ArrayAdapter<String> spinnerStopbitsAdapter = getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray(R.array.mb_config_stopbits_array))));
        spinnerStopbits.setAdapter(spinnerStopbitsAdapter);
        spinnerStopbits.setSelection(((ArrayAdapter<String>)spinnerStopbits.getAdapter())
                                         .getPosition(String.valueOf(readIntPref(PREF_MB_STOP_BITS, 1))));
        spinnerStopbits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                writeIntPref(PREF_MB_STOP_BITS, Integer.parseInt(spinnerStopbits.getSelectedItem().toString()));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
    
        btnRestart.setOnClickListener(v -> CCUUiUtil.triggerRestart(getActivity()));
        setSpinnerBackground();
        String confString = sharedPreferences.getString(BACNET_CONFIGURATION,null);

        try {
            config = new JSONObject(confString);
            networkObject = config.getJSONObject("network");
            deviceObject = config.getJSONObject("device");
            objectConf = config.getJSONObject("objectConf");

            // Following objects were not required, So we have initialized the value with 0
            objectConf.put("noOfScheduleObjects",0);
            objectConf.put("noOfTrendLogObjects",0);
            objectConf.put("noOfNotificationClassObjects",0);
            config.put("objectConf", objectConf);
            sharedPreferences.edit().putString(BACNET_CONFIGURATION,config.toString()).apply();

            getIpAddress();
            setBACnetConfigurationValues();
            doBACnetConfigurationValidation();
            initializeBACnet();
        } catch (JSONException e) {
            CcuLog.d(TAG_CCU_BACNET,"Config data: "+config+", Error message: "+e.getMessage());
            e.printStackTrace();
        }

        // Setting up the MSTP configuration
        ArrayAdapter<String> mstpSpinnerBaudRateAdapter = getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray(R.array.mstp_config_baudrate_array))));
        mstpSpinnerBaudRate.setAdapter(mstpSpinnerBaudRateAdapter);
        mstpSpinnerBaudRate.setSelection(((ArrayAdapter<String>)mstpSpinnerBaudRate.getAdapter())
                .getPosition(String.valueOf(readIntPref(PREF_MSTP_BAUD_RATE, 38400))));
        mstpSpinnerBaudRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                writeIntPref(PREF_MSTP_BAUD_RATE, Integer.parseInt(mstpSpinnerBaudRate.getSelectedItem().toString()));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        etMstpSourceAddress.setText(String.valueOf(readIntPref(PREF_MSTP_SOURCE_ADDRESS, 1)));
        etMstpMaxMaster.setText(String.valueOf(readIntPref(PREF_MSTP_MAX_MASTER, 127)));
        etMstpMaxFrame.setText(String.valueOf(readIntPref(PREF_MSTP_MAX_FRAME, 1)));
        etMstpDeviceId.setText(String.valueOf(readIntPref(PREF_MSTP_DEVICE_ID, 1)));


        etMstpSourceAddress.addTextChangedListener(new EditTextWatcher(etMstpSourceAddress));
        etMstpMaxMaster.addTextChangedListener(new EditTextWatcher(etMstpMaxMaster));
        etMstpMaxFrame.addTextChangedListener(new EditTextWatcher(etMstpMaxFrame));
        etMstpDeviceId.addTextChangedListener(new EditTextWatcher(etMstpDeviceId));

        if( isMstpConfigValid()) {
            btnMstpInitialize.setEnabled(true);
            btnMstpInitialize.setClickable(true);
            if (CCUUiUtil.isCarrierThemeEnabled(context)) {
                btnMstpInitialize.setTextColor(ContextCompat.getColor(context, R.color.carrier_75f));
            } else if (CCUUiUtil.isAiroverseThemeEnabled(context)) {
                btnMstpInitialize.setTextColor(ContextCompat.getColor(context, R.color.airoverse_primary));
            } else {
                btnMstpInitialize.setTextColor(ContextCompat.getColor(context, R.color.renatus_75F_accent));
            }
        } else {
            btnMstpInitialize.setEnabled(false);
            btnMstpInitialize.setClickable(false);
            btnMstpInitialize.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
        }

        if (sharedPreferences.getBoolean(IS_BACNET_MSTP_INITIALIZED, false)) {
            hideMstpConfigView();
        } else {
            enableMstpConfigView();
        }

        btnMstpInitialize.setOnClickListener(v -> {
            prepareMstpInit();
        });

        btnMstpDisable.setOnClickListener(v -> {
            RenatusApp.backgroundServiceInitiator.initServices();
            sharedPreferences.edit().putBoolean(IS_BACNET_MSTP_INITIALIZED, false).apply();
            Intent intent = new Intent("MSTP_STOP");
            context.sendBroadcast(intent);
            boolean isBacnetIpInitialized = sharedPreferences.getBoolean(IS_BACNET_INITIALIZED, false);
            if (!DashboardUtilKt.isDashboardConfig(context) && !isBacnetIpInitialized) {
                stopRestServer();
            }
            enableMstpConfigView();
            cancelScheduleJobToResubscribeBacnetMstpCOV("BACnet Mstp was disabled, cancelling COV subscription");
        });

        ////////////////////

        spinnerBacnetConfigAdapter =  new CustomSelectionAdapter<>(getContext(), R.layout.custom_textview, Arrays.asList(getResources().getStringArray(R.array.bacnet_config_array)),false);
        bacnetConfigSpinner.setAdapter(spinnerBacnetConfigAdapter);
        bacnetConfigSelectedPosition = getBacnetConfiguration(selectedDeviceType);
        bacnetConfigSpinner.setSelection(bacnetConfigSelectedPosition);
        spinnerBacnetConfigAdapter.setConfiguredIndex(bacnetConfigSelectedPosition);

        bacnetConfigSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                bacnetConfigSelectedPosition = position;
                String selectedItem = bacnetConfigSpinner.getSelectedItem().toString();
                handleConfigurationType(selectedItem);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        bbmdInputContainer.setVisibility(View.GONE);

        fdInputView.setVisibility(View.GONE);

        normalView.setVisibility(View.VISIBLE);

        // Below mentioned functionality was not required. Because it was handled in the BACapp
        sharedPreferences.edit().putBoolean(BACNET_FD_AUTO_STATE, false).apply();

        String bacAppVersion = CCUHsApi.getInstance().readDefaultStrVal("point and diag and version and bacnet");
        if (bacAppVersion.isEmpty()) {
            bacAppVersion = "Not Installed";
        }
        tvBacAppVersion.setText(bacAppVersion);

        tvFdSubmit.setOnClickListener(view1 -> {
            if (BACnetConfigurationType.FD.ordinal() == spinnerBacnetConfigAdapter.getConfiguredIndex() || BACnetConfigurationType.NORMAL.ordinal() == spinnerBacnetConfigAdapter.getConfiguredIndex()) {
                saveBacnetConfiguration(BACnetConfigurationType.FD.ordinal());
            } else {
                showConfirmationForBacnetConfiguration(false, bacnetConfigSelectedPosition);
            }
        });

        tvNormalSubmit.setOnClickListener(view1 -> { // Updating submit button for Normal
            if (BACnetConfigurationType.NORMAL.ordinal() == spinnerBacnetConfigAdapter.getConfiguredIndex()) {
                saveBacnetConfiguration(BACnetConfigurationType.NORMAL.ordinal());
            } else {
                showConfirmationForBacnetConfiguration(false, bacnetConfigSelectedPosition);
            }
        });

        tvBbmdAdd.setOnClickListener(view1 -> {
            CcuLog.d(TAG_CCU_BACNET, "add bbmd config");
            View bbmdView = LayoutInflater.from(getContext()).inflate(R.layout.lyt_bbmd_view, null);
            showBBMDDialogView(bbmdView, false);
            bbmdView.findViewById(R.id.tvBbmdRemove).setOnClickListener(view2 -> {
                View parent = bbmdView.findViewById(R.id.bbmdViewContainer);
                showAlertForRemoveBBMD(parent);
            });
            bbmdView.findViewById(R.id.tvBbmdEdit).setOnClickListener(view2 -> {
                showBBMDDialogView(bbmdView, true);
            });

        });

        tvBbmdSubmit.setOnClickListener(view1 -> {
            if (BACnetConfigurationType.BBMD.ordinal() == spinnerBacnetConfigAdapter.getConfiguredIndex() || BACnetConfigurationType.NORMAL.ordinal() == spinnerBacnetConfigAdapter.getConfiguredIndex()) {
                saveBacnetConfiguration(BACnetConfigurationType.BBMD.ordinal());
            } else {
                showConfirmationForBacnetConfiguration(false, bacnetConfigSelectedPosition);
            }
        });

        ivRefreshView.setOnClickListener(view12 -> {
            if(!UtilityApplication.isBACnetIntialized()){
                getIpAddress();
            }else{
                Toast.makeText(requireContext(), "Disable bacnet to fetch ip", Toast.LENGTH_SHORT).show();
            }
        });

        CcuLog.d(TAG_CCU_BACNET, "last selected bacnet device type==>"+selectedDeviceType);
        if(selectedDeviceType.equalsIgnoreCase(BACNET_DEVICE_TYPE_BBMD)){
            bacnetConfigSpinner.setSelection(BACnetConfigurationType.BBMD.ordinal());
            updateBbmdListItems();
            updateBacnetConfigStateChanged(false);
        }else if(selectedDeviceType.equalsIgnoreCase(BACNET_DEVICE_TYPE_FD)){
            bacnetConfigSpinner.setSelection(BACnetConfigurationType.FD.ordinal());
            updateFdView();
            updateBacnetConfigStateChanged(false);
        }else{
            bacnetConfigSpinner.setSelection(BACnetConfigurationType.NORMAL.ordinal());
        }

        RenatusLandingActivity.isBacnetConfigStateChanged = false;
    }

    private void enableMstpConfigView() {
        mstpSpinnerBaudRate.setVisibility(View.VISIBLE);
        tvMstpBaudRate.setVisibility(View.GONE);
        btnMstpInitialize.setVisibility(View.VISIBLE);
        btnMstpDisable.setVisibility(View.GONE);
        etMstpSourceAddress.setVisibility(View.VISIBLE);
        tvMstpSourceAddress.setVisibility(View.GONE);
        etMstpMaxMaster.setVisibility(View.VISIBLE);
        tvMstpMaxMaster.setVisibility(View.GONE);
        etMstpMaxFrame.setVisibility(View.VISIBLE);
        tvMstpMaxFrame.setVisibility(View.GONE);
        etMstpDeviceId.setVisibility(View.VISIBLE);
        tvMstpDeviceId.setVisibility(View.GONE);
    }

    private void updateMstpUi(){
        String textMessage;
        ArrayList<String> usbSerialPorts = getPortAddressMstpDevices();

        if(!usbSerialPorts.isEmpty() && usbSerialPorts.get(0).contains("USB")) {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "USB Serial Ports found: " + usbSerialPorts);
            portAddress = usbSerialPorts.get(0);
            isUSBSerialPortAvailable = true;
            textMessage = "Connected";
        } else {
            CcuLog.d(TAG_CCU_BACNET_MSTP, "No USB Serial Ports found");
            usbSerial.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
            isUSBSerialPortAvailable = false;
            textMessage = "Not Connected";
        }

        String finalTextMessage = textMessage;
        requireActivity().runOnUiThread(() -> usbSerial.setText(finalTextMessage));
    }

    private void hideMstpConfigView() {
        mstpSpinnerBaudRate.setVisibility(View.GONE);
        tvMstpBaudRate.setText(String.valueOf(readIntPref(PREF_MSTP_BAUD_RATE, 38400)));
        tvMstpBaudRate.setVisibility(View.VISIBLE);
        hideView(etMstpSourceAddress,tvMstpSourceAddress);
        hideView(etMstpMaxMaster,tvMstpMaxMaster);
        hideView(etMstpMaxFrame,tvMstpMaxFrame);
        hideView(etMstpDeviceId,tvMstpDeviceId);
        btnMstpInitialize.setVisibility(View.GONE);
        btnMstpDisable.setVisibility(View.VISIBLE);
    }

    private boolean isMstpConfigValid() {
        if (etMstpSourceAddress.getText().toString().isEmpty() || !CCUUiUtil.isValidNumber(Integer.parseInt(etMstpSourceAddress.getText().toString()), 1, 127, 1)) {
            return false;
        }

        if (etMstpMaxMaster.getText().toString().isEmpty() || !CCUUiUtil.isValidNumber(Integer.parseInt(etMstpMaxMaster.getText().toString()), 1, 127, 1)) {
            return false;
        }
        if (etMstpMaxFrame.getText().toString().isEmpty() || !CCUUiUtil.isValidNumber(Integer.parseInt(etMstpMaxFrame.getText().toString()), 1, 255, 1)) {
            return false;
        }
        if(etMstpDeviceId.getText().toString().isEmpty() || !CCUUiUtil.isValidNumber(Integer.parseInt(etMstpDeviceId.getText().toString()), 1, 4194302, 1)) {
            return false;
        }
        return true;
    }

    private void validateAndUpdateMstpConfig(EditText view, String key, String value, int min, int max, int multiplier, String errorMessage) {

           if (view.getText().toString().isEmpty() || !CCUUiUtil.isValidNumber(Integer.parseInt(view.getText().toString()), min, max, multiplier)) {
               view.setError(errorMessage);
               view.setSelected(true);
               btnMstpInitialize.setEnabled(false);
               btnMstpInitialize.setClickable(false);
               btnMstpInitialize.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
           } else {
               view.setError(null);
               view.setSelected(false);

               if (isMstpConfigValid()) {
                   btnMstpInitialize.setEnabled(true);
                   btnMstpInitialize.setClickable(true);
                   if (CCUUiUtil.isCarrierThemeEnabled(context)) {
                       btnMstpInitialize.setTextColor(ContextCompat.getColor(context, R.color.carrier_75f));
                   } else if (CCUUiUtil.isDaikinEnvironment(context)) {
                       btnMstpInitialize.setTextColor(ContextCompat.getColor(context, R.color.daikin_75f));
                   } else if (CCUUiUtil.isAiroverseThemeEnabled(context)) {
                       btnMstpInitialize.setTextColor(ContextCompat.getColor(context, R.color.airoverse_primary));
                   } else {
                       btnMstpInitialize.setTextColor(ContextCompat.getColor(context, R.color.renatus_75F_accent));
                   }
               } else {
                   btnMstpInitialize.setEnabled(false);
                   btnMstpInitialize.setClickable(false);
                   btnMstpInitialize.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
               }
           }
    }

    private void saveBacnetConfiguration(int selectedOrdinal) {
        switch (selectedOrdinal) {
            case 0:
                if (validateBbmdData("")) {
                    displayCustomToastMessageOnSuccess("Configuration updated successfully.");
                    try {
                        DataBbmdObj dataBbmdObj = new DataBbmdObj();
                        for (int i = 0; i < bbmdInputViews.getChildCount(); i++) {
                            View childView = bbmdInputViews.getChildAt(i);
                            TextView etBbmdIp = childView.findViewById(R.id.etBbmdIp);
                            TextView etBbmdPort = childView.findViewById(R.id.etBbmdPort);
                            TextView etBbmdMask = childView.findViewById(R.id.etBbmdTime);

                            CcuLog.d(TAG_CCU_BACNET, "bbmd entry at->" + i + "<--ip-->" + etBbmdIp.getText().toString() + "<--port-->" +
                                    etBbmdPort.getText().toString() + "<--mask-->" + etBbmdMask.getText().toString());
                            dataBbmdObj.addItem(new DataBbmd(etBbmdIp.getText().toString(), Integer.parseInt(etBbmdPort.getText().toString()),
                                    Integer.parseInt(etBbmdMask.getText().toString())));
                        }

                        String jsonString = new Gson().toJson(dataBbmdObj);
                        CcuLog.d(TAG_CCU_BACNET, "bbmd output-->" + jsonString);
                        sharedPreferences.edit().putString(BACNET_DEVICE_TYPE, BACNET_DEVICE_TYPE_BBMD).apply();
                        sharedPreferences.edit().putString(BACNET_BBMD_CONFIGURATION, jsonString).apply();
                        sharedPreferences.edit().putString(BACNET_BBMD_CONFIGURATION_BACKUP, jsonString).apply(); // Backup BBMD configuration

                        sharedPreferences.edit().remove(BACNET_FD_CONFIGURATION).apply(); // Clearing FD configuration if any

                        Intent intent = new Intent(BROADCAST_BACNET_APP_CONFIGURATION_TYPE);
                        intent.putExtra("message", "BBMD");
                        intent.putExtra("data", jsonString);
                        context.sendBroadcast(intent);
                        spinnerBacnetConfigAdapter.setConfiguredIndex(BACnetConfigurationType.BBMD.ordinal());
                        updateBacnetConfigStateChanged(false);
                        performConfigFileBackup();
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case 1:
                if (validateFdData()) {
                    displayCustomToastMessageOnSuccess("Configuration updated successfully.");
                    try {
                        DataFdObj dataFdObj = new DataFdObj();

                        CcuLog.d(TAG_CCU_BACNET, "fd entry at->" + "<--ip-->" + etFDIP.getText().toString() + "<--port-->" +
                                etFDPort.getText().toString() + "<--time-->" + etFDPort.getText().toString());
                        dataFdObj.setDataFd(new DataFd(etFDIP.getText().toString(), Integer.parseInt(etFDPort.getText().toString()),
                                Integer.parseInt(etFDTime.getText().toString())));

                        String jsonString = new Gson().toJson(dataFdObj);
                        CcuLog.d(TAG_CCU_BACNET, "fd output-->" + jsonString);

                        sharedPreferences.edit().putString(BACNET_DEVICE_TYPE, BACNET_DEVICE_TYPE_FD).apply();
                        sharedPreferences.edit().putString(BACNET_FD_CONFIGURATION, jsonString).apply();
                        sharedPreferences.edit().putString(BACNET_FD_CONFIGURATION_BACKUP, jsonString).apply(); // Backup FD configuration

                        sharedPreferences.edit().remove(BACNET_BBMD_CONFIGURATION).apply(); // Clearing BBMD configuration if any

                        Intent intent = new Intent(BROADCAST_BACNET_APP_CONFIGURATION_TYPE);
                        intent.putExtra("message", "Foreign Device");
                        intent.putExtra("data", jsonString);
                        context.sendBroadcast(intent);

                        spinnerBacnetConfigAdapter.setConfiguredIndex(BACnetConfigurationType.FD.ordinal());
                        updateBacnetConfigStateChanged(false);
                        performConfigFileBackup();
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case 2:
                sharedPreferences.edit().putString(BACNET_DEVICE_TYPE, BACNET_DEVICE_TYPE_NORMAL).apply();
                sharedPreferences.edit().remove(BACNET_FD_CONFIGURATION).apply(); // Clearing FD configuration if any
                sharedPreferences.edit().remove(BACNET_BBMD_CONFIGURATION).apply(); // Clearing BBMD configuration if any
                sendBroadCast(context, BROADCAST_BACNET_APP_CONFIGURATION_TYPE, "Normal");
                spinnerBacnetConfigAdapter.setConfiguredIndex(BACnetConfigurationType.NORMAL.ordinal());
                displayCustomToastMessageOnSuccess("Configuration updated successfully.");
                break;
        }
    }

    private void updateBbmdListItems() {
        if (bbmdInputViews.getChildCount() > 0) {
            bbmdInputViews.removeViews(1, bbmdInputViews.getChildCount() - 1);
        }
        String configuration = sharedPreferences.getString(BACNET_BBMD_CONFIGURATION, null);
        if(configuration != null){
            CcuLog.d(TAG_CCU_BACNET, "bbmd configuration found");
            DataBbmdObj dataBbmdObj = new Gson().fromJson(configuration, DataBbmdObj.class);
            ArrayList<DataBbmd> listOdDataBbmd = dataBbmdObj.getListOfDataBbmd();
            CcuLog.d(TAG_CCU_BACNET, "bbmd lis size "+listOdDataBbmd.size());
            createBbmdViewAndFillData(listOdDataBbmd);
        }
        getIpAddress();
    }

    private void updateFdView() {
        String configuration = sharedPreferences.getString(BACNET_FD_CONFIGURATION, null);
        if(configuration != null){
            DataFdObj dataFdObj = new Gson().fromJson(configuration, DataFdObj.class);
            if(dataFdObj != null) {
                fillFdView(dataFdObj.getDataFd());
            }
        } else {
            etFDIP.setText("192.168.1.1"); //If FD configuration is not available, set default values
            etFDPort.setText("47808");
            etFDTime.setText("0");
        }
        updateBacnetConfigStateChanged(false);
    }

    private boolean validateFdData() {

            if (etFDIP.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidIPAddress(etFDIP.getText().toString().trim()))) {
                etFDIP.setError(getString(R.string.error_ip_address));
                return false;
            }
            if (etFDPort.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etFDPort.getText().toString()), 47808, 47823, 1))) {
                etFDPort.setError(getString(R.string.txt_error_port));
                return false;
            }
            if (etFDTime.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etFDTime.getText().toString()), 0, 65536, 1))) {
                etFDTime.setError(getString(R.string.txt_valid_number));
                return false;
            }
            etFDIP.setError(null);
            etFDPort.setError(null);
            etFDTime.setError(null);
        return true;
    }

    private boolean validateBbmdData(String ipAddressWhileAddingNew) {
        for (int i = 0; i < bbmdInputViews.getChildCount(); i++) {
            View childView = bbmdInputViews.getChildAt(i);
            TextView etBbmdIp = childView.findViewById(R.id.etBbmdIp);
            TextView etBbmdPort = childView.findViewById(R.id.etBbmdPort);
            TextView etBbmdMask = childView.findViewById(R.id.etBbmdTime);

            if (etBbmdIp.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidIPAddress(etBbmdIp.getText().toString().trim()))) {
                etBbmdIp.setError(getString(R.string.error_ip_address));
                return false;
            }
            if (!ipAddressWhileAddingNew.isEmpty() && etBbmdIp.getText().toString().equals(ipAddressWhileAddingNew)) {
                return false; // Same IP address is not allowed
            }
            if (etBbmdMask.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etBbmdMask.getText().toString()), 1, Integer.MAX_VALUE, 1))) {
                etBbmdMask.setError(getString(R.string.txt_valid_number));
                return false;
            }
            etBbmdIp.setError(null);
            etBbmdPort.setError(null);
            etBbmdMask.setError(null);
        }
        return true;
    }

    private void handleConfigurationType(String label) {
        if(label.equalsIgnoreCase(getString(R.string.label_bbmd))){
            bbmdInputContainer.setVisibility(View.VISIBLE);
            fdInputView.setVisibility(View.GONE);
            normalView.setVisibility(View.GONE);
            updateBbmdListItems();
            updateBbmdListView();
        }else if(label.equalsIgnoreCase(getString(R.string.label_foreign_device))){
            fdInputView.setVisibility(View.VISIBLE);
            bbmdInputContainer.setVisibility(View.GONE);
            normalView.setVisibility(View.GONE);
            updateFdView();
        }else{
            fdInputView.setVisibility(View.GONE);
            bbmdInputContainer.setVisibility(View.GONE);
            normalView.setVisibility(View.VISIBLE);
        }
    }

    private void setBACnetConfigurationValues() {
        try {
            tvIPDeviceObjectName.setText(deviceObject.getString(IP_DEVICE_OBJECT_NAME));
            toggleZoneToVirtualDeviceMapping.setChecked(config.getBoolean(ZONE_TO_VIRTUAL_DEVICE_MAPPING));
            description.setText(deviceObject.getString(DESCRIPTION).equals(NULL) || deviceObject.getString(DESCRIPTION).equals(EMPTY_STRING)  ? EMPTY_STRING : deviceObject.getString(DESCRIPTION).trim());
            location.setText((deviceObject.getString(LOCATION).equals(NULL)) || (deviceObject.getString(LOCATION).equals(EMPTY_STRING)) ? EMPTY_STRING : deviceObject.getString(LOCATION).trim());
            password.setText((deviceObject.getString(PASSWORD).equals(NULL)) || (deviceObject.getString(PASSWORD).equals(EMPTY_STRING)) ? EMPTY_STRING : deviceObject.getString(PASSWORD).trim());
            localNetworkNumber.setText(String.valueOf((networkObject.getString(LOCAL_NETWORK_NUMBER).equals(NULL)) || (networkObject.getString(LOCAL_NETWORK_NUMBER).equals(EMPTY_STRING)) ? EMPTY_STRING : networkObject.getInt(LOCAL_NETWORK_NUMBER)));
            virtualNetworkNumber.setText(String.valueOf((networkObject.getString(VIRTUAL_NETWORK_NUMBER).equals(NULL)) || (networkObject.getString(VIRTUAL_NETWORK_NUMBER).equals(EMPTY_STRING)) ? EMPTY_STRING : networkObject.getInt(VIRTUAL_NETWORK_NUMBER)));
            port.setText(String.valueOf(networkObject.getInt(PORT)));
            ipDeviceInstanceNumber.setText((deviceObject.getString(IP_DEVICE_INSTANCE_NUMBER).equals(NULL)) || (deviceObject.getString(IP_DEVICE_INSTANCE_NUMBER).equals(EMPTY_STRING)) ? EMPTY_STRING : String.valueOf(deviceObject.getInt(IP_DEVICE_INSTANCE_NUMBER)));
            apduTimeout.setText((deviceObject.getString(APDU_TIMEOUT).equals(NULL)) || (deviceObject.getString(APDU_TIMEOUT).equals(EMPTY_STRING)) ? EMPTY_STRING :  String.valueOf(deviceObject.getInt(APDU_TIMEOUT)));
            numberOfAPDURetries.setText((deviceObject.getString(NUMBER_OF_APDU_RETRIES).equals(NULL)) || (deviceObject.getString(NUMBER_OF_APDU_RETRIES).equals(EMPTY_STRING)) ? EMPTY_STRING :   String.valueOf(deviceObject.getInt(NUMBER_OF_APDU_RETRIES)));
            apduSegmentTimeout.setText((deviceObject.getString(APDU_SEGMENT_TIMEOUT).equals(NULL)) || (deviceObject.getString(APDU_SEGMENT_TIMEOUT).equals(EMPTY_STRING)) ? EMPTY_STRING : String.valueOf(deviceObject.getInt(APDU_SEGMENT_TIMEOUT)));
            etNotificationClassObjects.setText(String.valueOf((objectConf.getString(NUMBER_OF_NOTIFICATION_CLASS_OBJECTS).equals(NULL)) || (objectConf.getString(NUMBER_OF_NOTIFICATION_CLASS_OBJECTS).equals(EMPTY_STRING)) ? EMPTY_STRING : objectConf.getInt(NUMBER_OF_NOTIFICATION_CLASS_OBJECTS)));
            etTrendLogObjects.setText(String.valueOf((objectConf.getString(NUMBER_OF_TREND_LOG_OBJECTS).equals(NULL)) || (objectConf.getString(NUMBER_OF_TREND_LOG_OBJECTS).equals(EMPTY_STRING)) ? EMPTY_STRING : objectConf.getInt(NUMBER_OF_TREND_LOG_OBJECTS)));
            etScheduleObjects.setText(String.valueOf((objectConf.getString(NUMBER_OF_SCHEDULE_OBJECTS).equals(NULL)) || (objectConf.getString(NUMBER_OF_SCHEDULE_OBJECTS).equals(EMPTY_STRING)) ? EMPTY_STRING : objectConf.getInt(NUMBER_OF_SCHEDULE_OBJECTS)));
			etOffsetValues.setText(String.valueOf((objectConf.getString(NUMBER_OF_OFFSET_VALUES).equals(NULL)) || (objectConf.getString(NUMBER_OF_OFFSET_VALUES).equals(EMPTY_STRING)) ? EMPTY_STRING : objectConf.getInt(NUMBER_OF_OFFSET_VALUES)));
        }catch (JSONException e){
            CcuLog.d(TAG_CCU_BACNET, "Exception while populating");
            e.printStackTrace();
        }
    }

    private void doBACnetConfigurationValidation() {
        description.addTextChangedListener(new EditTextWatcher(description));
        location.addTextChangedListener(new EditTextWatcher(location));
        password.addTextChangedListener(new EditTextWatcher(password));
        localNetworkNumber.addTextChangedListener(new EditTextWatcher(localNetworkNumber));
        virtualNetworkNumber.addTextChangedListener(new EditTextWatcher(virtualNetworkNumber));
        port.addTextChangedListener(new EditTextWatcher(port));
        ipDeviceInstanceNumber.addTextChangedListener(new EditTextWatcher(ipDeviceInstanceNumber));
        apduTimeout.addTextChangedListener(new EditTextWatcher(apduTimeout));
        numberOfAPDURetries.addTextChangedListener(new EditTextWatcher(numberOfAPDURetries));
        apduSegmentTimeout.addTextChangedListener(new EditTextWatcher(apduSegmentTimeout));
        etNotificationClassObjects.addTextChangedListener(new EditTextWatcher(etNotificationClassObjects));
        etTrendLogObjects.addTextChangedListener(new EditTextWatcher(etTrendLogObjects));
        etScheduleObjects.addTextChangedListener(new EditTextWatcher(etScheduleObjects));
        etOffsetValues.addTextChangedListener(new EditTextWatcher(etOffsetValues));
        etFDIP.addTextChangedListener(new EditTextWatcher(etFDIP));
        etFDPort.addTextChangedListener(new EditTextWatcher(etFDPort));
        etFDTime.addTextChangedListener(new EditTextWatcher(etFDTime));
    }

    private void initializeBACnet() {

        if(UtilityApplication.isBACnetIntialized()){
            initializeBACnet.setVisibility(View.GONE);
            disableBACnet.setVisibility(View.VISIBLE);
            ipAddressSpinner.setEnabled(false);
            bacnetConfigureLayout.setVisibility(View.VISIBLE);
            toggleZoneToVirtualDeviceMapping.setEnabled(false);
            hideView(description, tvDescription);
            hideView(location, tvLocation);
            if(password.getText().toString().trim().isEmpty()){
                textInputLayout.setVisibility(View.GONE);
            }else{
                textInputLayout.setVisibility(View.GONE);
                tvPassword.setVisibility(View.VISIBLE);
                tvPassword.setText("******");
            }
            hideView(localNetworkNumber, tvLocalNetworkNumber);
            hideView(virtualNetworkNumber, tvVirtualNetworkAddress);
            hideView(port, tvPort);
            hideView(ipDeviceInstanceNumber, tvIPDeviceInstanceNumber);
            hideView(apduTimeout, tvAPDUTimeout);
            hideView(numberOfAPDURetries, tvNumberofAPDURetries);
            hideView(apduSegmentTimeout, tvApduSegmentTimeout);
            hideView(etNotificationClassObjects, tvNotificationClassObjects);
            hideView(etTrendLogObjects, tvTrendLogObjects);
            hideView(etScheduleObjects, tvScheduleObjects);
            hideView(etOffsetValues, tvOffsetValue);
        }else{
            disableBACnet.setVisibility(View.GONE);
            bacnetConfigureLayout.setVisibility(View.GONE);
            initializeBACnet.setVisibility(View.VISIBLE);
            toggleZoneToVirtualDeviceMapping.setEnabled(true);
            setBACnetConfigurationValues();
        }


        initializeBACnet.setOnClickListener(view -> executeTask());

        disableBACnet.setOnClickListener(v -> {
            toggleZoneToVirtualDeviceMapping.setEnabled(true);
            initializeBACnet.setVisibility(View.VISIBLE);
            disableBACnet.setVisibility(View.GONE);
            bacnetConfigureLayout.setVisibility(View.GONE);
            description.setVisibility(View.VISIBLE);
            tvDescription.setVisibility(View.GONE);
            location.setVisibility(View.VISIBLE);
            tvLocation.setVisibility(View.GONE);
            textInputLayout.setVisibility(View.VISIBLE);
            password.setVisibility(View.VISIBLE);
            tvPassword.setVisibility(View.GONE);
            ipAddressSpinner.setEnabled(true);
            localNetworkNumber.setVisibility(View.VISIBLE);
            tvLocalNetworkNumber.setVisibility(View.GONE);
            virtualNetworkNumber.setVisibility(View.VISIBLE);
            tvVirtualNetworkAddress.setVisibility(View.GONE);
            port.setVisibility(View.VISIBLE);
            tvPort.setVisibility(View.GONE);
            ipDeviceInstanceNumber.setVisibility(View.VISIBLE);
            tvIPDeviceInstanceNumber.setVisibility(View.GONE);
            apduTimeout.setVisibility(View.VISIBLE);
            tvAPDUTimeout.setVisibility(View.GONE);
            numberOfAPDURetries.setVisibility(View.VISIBLE);
            tvNumberofAPDURetries.setVisibility(View.GONE);
            apduSegmentTimeout.setVisibility(View.VISIBLE);
            tvApduSegmentTimeout.setVisibility(View.GONE);
            etNotificationClassObjects.setVisibility(View.VISIBLE);
            tvNotificationClassObjects.setVisibility(View.GONE);
            etTrendLogObjects.setVisibility(View.VISIBLE);
            tvTrendLogObjects.setVisibility(View.GONE);
            etScheduleObjects.setVisibility(View.VISIBLE);
            tvScheduleObjects.setVisibility(View.GONE);
            etOffsetValues.setVisibility(View.VISIBLE);
            tvOffsetValue.setVisibility(View.GONE);
            ipAddressSpinner.setEnabled(true);

            boolean isMstpInitialized = sharedPreferences.getBoolean(IS_BACNET_MSTP_INITIALIZED, false);
            if (!DashboardUtilKt.isDashboardConfig(context) && !isMstpInitialized) {
                stopRestServer();
            }
            sharedPreferences.edit().putBoolean(IS_BACNET_INITIALIZED, false).apply();
            sendBroadCast(context, BROADCAST_BACNET_APP_STOP, "Stop BACnet App");
            performConfigFileBackup();
        });

        toggleZoneToVirtualDeviceMapping.setOnCheckedChangeListener((buttonView, isChecked) -> {

            try {
                config.put(ZONE_TO_VIRTUAL_DEVICE_MAPPING, isChecked);
                sharedPreferences.edit().putString(BACNET_CONFIGURATION,config.toString()).apply();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });

        tvzvdmHint.setOnClickListener(v -> {
            if(!isZoneToVirtualDeviceErrorShowing)
            {
                zvdmHint.setVisibility(View.GONE);
                tvzvdmHint.setError("Enabled: Zone is represented as a virtual device and" +
                        " each endpoint is mapped to the respective " +
                        "BACnet Object.\n\n" +
                        "Disabled: System and Terminal profile points are exposed" +
                        " as a flat list.");
                isZoneToVirtualDeviceErrorShowing = true;
            }else{
                zvdmHint.setVisibility(View.VISIBLE);
                tvzvdmHint.setError(null);
                isZoneToVirtualDeviceErrorShowing = false;
            }

        });
    }

    private void hideView(EditText etView, TextView tvView) {
        etView.setVisibility(View.GONE);
        tvView.setText(etView.getText().toString().trim());
        tvView.setVisibility(View.VISIBLE);
    }

    private boolean validateEntries() {

        if(deviceIpAddress.equals(EMPTY_STRING) || (!CCUUiUtil.isValidIPAddress(deviceIpAddress.toString().trim()))) return false;

        if(localNetworkNumber.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(localNetworkNumber.getText().toString()), 1, 65534, 1)))  return false;

        if(virtualNetworkNumber.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(virtualNetworkNumber.getText().toString()), 1, 65534, 1)))  return false;

        if(Integer.parseInt(virtualNetworkNumber.getText().toString()) == Integer.parseInt(localNetworkNumber.getText().toString())) {
            virtualNetworkNumber.setError(context.getResources().getString(R.string.error_vnn_and_lnn_not_same));
            localNetworkNumber.setError(context.getResources().getString(R.string.error_vnn_and_lnn_not_same));
            return false;
        }else{
            virtualNetworkNumber.setError(null);
            localNetworkNumber.setError(null);
        }

        if(port.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(port.getText().toString()), 4069, 65535, 1))) return false;

        if(ipDeviceInstanceNumber.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(ipDeviceInstanceNumber.getText().toString()), 0, 4194302, 1)))  return false;

        if(apduTimeout.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(apduTimeout.getText().toString()), 6000, 65535, 1000))) return false;

        if(numberOfAPDURetries.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(numberOfAPDURetries.getText().toString()), 0, 255, 1))) return false;

        if(apduSegmentTimeout.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(apduSegmentTimeout.getText().toString()), 5000, 65535, 1000)))  return false;

        if(etNotificationClassObjects.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etNotificationClassObjects.getText().toString()), 0, 10, 1)))  return false;

        if(etTrendLogObjects.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etTrendLogObjects.getText().toString()), 0, 10, 1)))  return false;

        if(etOffsetValues.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etOffsetValues.getText().toString()), 1, 100, 1)))  return false;
        return !etScheduleObjects.getText().toString().equals(EMPTY_STRING) && (CCUUiUtil.isValidNumber(Integer.parseInt(etScheduleObjects.getText().toString()), 0, 10, 1));
    }

    public int readIntPref(String key, int defaultVal) {
        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return spDefaultPrefs.getInt(key, defaultVal);
    }
    
    public void writeIntPref(String key, int val) {
        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = spDefaultPrefs.edit();
        editor.putInt(key, val);
        editor.apply();
    }

    public void writeStringPref(String key, String val) {
        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = spDefaultPrefs.edit();
        editor.putString(key, val);
        editor.apply();
    }

    public String readStringPref(String key, String defaultVal) {
        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return spDefaultPrefs.getString(key, defaultVal);
    }
    

    private void setSpinnerBackground(){
        CCUUiUtil.setSpinnerDropDownColor(spinnerBaudRate,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spinnerParity,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spinnerDatabits,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spinnerBaudRate,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spinnerStopbits,getContext());
    }

    public void setIsBacConfigStateChangedAndUpdateView(boolean stateChanged) {
        RenatusLandingActivity.isBacnetConfigStateChanged = stateChanged;

        if (spinnerBacnetConfigAdapter.getConfiguredIndex() == BACnetConfigurationType.BBMD.ordinal()) {
            bacnetConfigSpinner.setSelection(BACnetConfigurationType.BBMD.ordinal());
            updateBbmdListItems();
        } else if (spinnerBacnetConfigAdapter.getConfiguredIndex() == BACnetConfigurationType.FD.ordinal()) {
            bacnetConfigSpinner.setSelection(BACnetConfigurationType.FD.ordinal());
            updateFdView();
        } else {
            bacnetConfigSpinner.setSelection(BACnetConfigurationType.NORMAL.ordinal());
        }
    }

    @Override
    public void updateMstpBacnetUi(Boolean isInitSuccess, String message, String errorCode) {
        CcuLog.d(TAG_CCU_BACNET_MSTP, "updateMstpBacnetUi-->"+isInitSuccess);
        requireActivity().runOnUiThread(() -> {
            if(isInitSuccess){
                displayCustomToastMessageOnSuccess("BACnet - MSTP Configuration initialized successfully");
                hideMstpConfigView();
            }else{
                enableMstpConfigView();
                Toast.makeText(requireContext(), "MSTP initialization Failed.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private class EditTextWatcher implements TextWatcher {

        private final View view;
        EditTextWatcher(View view){
            this.view = view;
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            switch (view.getId()) {

                case R.id.etDescription:
                    try {
                        CcuLog.i(TAG_CCU_BACNET, "Key: description, Value: "+description.getText().toString());
                        String description1 = description.getText().toString();
                        deviceObject.put(DESCRIPTION,description1);
                        sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    break;

                case R.id.etLocation:
                    try {
                        CcuLog.i(TAG_CCU_BACNET, "Key: location, Value: "+location.getText().toString());
                        String location1 = location.getText().toString().trim();
                        deviceObject.put(LOCATION, location1);
                        sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    break;

                case R.id.etPassword:
                    try {
                        CcuLog.i(TAG_CCU_BACNET, "Key: location, Value: "+password.getText().toString());
                        if (!CCUUiUtil.isAlphaNumeric(password.getText().toString())) {
                            password.setError(getString(R.string.error_password));
                        } else {
                            password.setError(null);
                            deviceObject.put(PASSWORD, password.getText().toString().trim());
                            sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    break;


                case R.id.etlocalNetworkAddress:
                    validateAndSetValue(localNetworkNumber, LOCAL_NETWORK_NUMBER, localNetworkNumber.getText().toString(), networkObject, 1,65534, 1, getString(R.string.error_local_network_number));
                    break;

                case R.id.etVirtualNetworkAddress:
                    validateAndSetValue(virtualNetworkNumber, VIRTUAL_NETWORK_NUMBER, virtualNetworkNumber.getText().toString(), networkObject, 1,65534, 1, getString(R.string.error_virtual_network_number));
                    break;

                case R.id.etPort:
                    validateAndSetValue(port, PORT, port.getText().toString(), networkObject, 47808,47823, 1, getString(R.string.txt_error_port));
                    break;

                case R.id.etIPDeviceInstanceNumber:
                    validateAndSetValue(ipDeviceInstanceNumber, IP_DEVICE_INSTANCE_NUMBER, ipDeviceInstanceNumber.getText().toString(), deviceObject, 0,4194302, 1, getString(R.string.error_ip_device_instance_number));
                    break;

                case R.id.etAPDU_Timeout:
                    validateAndSetValue(apduTimeout, APDU_TIMEOUT, apduTimeout.getText().toString(), deviceObject, 6000,65535, 1000, getString(R.string.error_apdu_timeout));
                    break;

                case R.id.etNumberofAPDURetries:
                    validateAndSetValue(numberOfAPDURetries, NUMBER_OF_APDU_RETRIES, numberOfAPDURetries.getText().toString(), deviceObject, 0,255,1, getString(R.string.error_apdu_retries));
                    break;

                case R.id.etAPDUSegmentTimeout:
                    validateAndSetValue(apduSegmentTimeout, APDU_SEGMENT_TIMEOUT, apduSegmentTimeout.getText().toString(), deviceObject, 5000,65535, 1000, getString(R.string.error_apdu_segment_timeout));
                    break;

                case R.id.etNotificationClassObjects:
                    validateAndSetValue(etNotificationClassObjects, NUMBER_OF_NOTIFICATION_CLASS_OBJECTS, etNotificationClassObjects.getText().toString(), objectConf, 0, 10, 1, getString(R.string.error_notification_class_objects));
                    break;

                case R.id.etTrendLogObjects:
                    validateAndSetValue(etTrendLogObjects, NUMBER_OF_TREND_LOG_OBJECTS, etTrendLogObjects.getText().toString(), objectConf, 0, 10, 1, getString(R.string.error_trend_log_objects));
                    break;

                case R.id.etScheduleObjects:
                    validateAndSetValue(etScheduleObjects, NUMBER_OF_SCHEDULE_OBJECTS, etScheduleObjects.getText().toString(), objectConf, 0, 10, 1, getString(R.string.error_schedule_objects));
                    break;
                case R.id.etOffsetValues:
                    validateAndSetValue(etOffsetValues, NUMBER_OF_OFFSET_VALUES, etOffsetValues.getText().toString(), objectConf, 1, 100, 1, getString(R.string.error_offset_values));
                    break;
                case R.id.etFdIp:
                    if (etFDIP.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidIPAddress(etFDIP.getText().toString().trim()))) {
                        etFDIP.setError(getString(R.string.error_ip_address));
                    }
                    else {
                        etFDIP.setError(null);
                    }
                    updateBacnetConfigStateChanged(true);
                    break;
                case R.id.etFdPort:
                    if (etFDPort.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etFDPort.getText().toString()), 47808, 47823, 1))) {
                        etFDPort.setError(getString(R.string.txt_error_port));
                    }
                    else {
                        etFDPort.setError(null);
                    }
                    updateBacnetConfigStateChanged(true);
                    break;
                case R.id.etFdTime:
                    if (etFDTime.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etFDTime.getText().toString()), 0, 65536, 1))) {
                        etFDTime.setError(getString(R.string.txt_valid_number));
                    }
                    else {
                        etFDTime.setError(null);
                    }
                    updateBacnetConfigStateChanged(true);
                    break;

                case R.id.etMstpSourceAddress:
                    validateAndUpdateMstpConfig(etMstpSourceAddress, PREF_MSTP_SOURCE_ADDRESS, etMstpSourceAddress.getText().toString(), 1, 127, 1, getString(R.string.err_txt_mstp_source_address));
                    break;
                case R.id.etMstpMaxMaster:
                    validateAndUpdateMstpConfig(etMstpMaxMaster, PREF_MSTP_MAX_MASTER, etMstpMaxMaster.getText().toString(), 1, 127, 1, getString(R.string.err_txt_mstp_max_master));
                    break;
                case R.id.etMstpMaxFrame:
                    validateAndUpdateMstpConfig(etMstpMaxFrame, PREF_MSTP_MAX_FRAME, etMstpMaxFrame.getText().toString(), 1, 255, 1, getString(R.string.err_txt_mstp_max_frame));
                    break;
                case R.id.etMstpDeviceId:
                    validateAndUpdateMstpConfig(etMstpDeviceId, PREF_MSTP_DEVICE_ID, etMstpDeviceId.getText().toString(), 1, 4194302, 1, getString(R.string.err_txt_mstp_device_id));
                    break;
            }
        }

        private void validateAndSetValue(EditText view, String key, String value, JSONObject jsonObject, int min, int max,  int multiple, String error) {
            try {
                if (!value.trim().isEmpty()) {
                    CcuLog.i(TAG_CCU_BACNET, "Key: "+key+", Value: "+value+", isValid: "+CCUUiUtil.isValidNumber(Integer.parseInt(value), min, max, multiple));
                    if (!CCUUiUtil.isValidNumber(Integer.parseInt(value.trim()),min,max, multiple)) {
                        view.setError(error);
                        view.setSelected(true);
                        initializeBACnet.setEnabled(false);
                        initializeBACnet.setClickable(false);
                        initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
                    } else {
                        view.setError(null);
                        view.setSelected(false);
                        jsonObject.put(key,Integer.parseInt(value));
                        sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                        if(validateEntries()){
                            initializeBACnet.setEnabled(true);
                            initializeBACnet.setClickable(true);
                            if (CCUUiUtil.isCarrierThemeEnabled(context)) {
                                initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.carrier_75f));
                            } else if (CCUUiUtil.isDaikinEnvironment(context)) {
                                initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.daikin_75f));
                            } else if (CCUUiUtil.isAiroverseThemeEnabled(context)) {
                                initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.airoverse_primary));
                            } else {
                                initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.renatus_75F_accent));
                            }
                        }else{
                            initializeBACnet.setEnabled(false);
                            initializeBACnet.setClickable(false);
                            initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
                        }
                    }
                }else{
                    view.setError(null);
                    jsonObject.put(key,"");
                    sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                    if(validateEntries()){
                        initializeBACnet.setEnabled(true);
                        initializeBACnet.setClickable(true);
                        if (CCUUiUtil.isCarrierThemeEnabled(context)) {
                            initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.carrier_75f));
                        } else if (CCUUiUtil.isDaikinEnvironment(context)) {
                            initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.daikin_75f));
                        } else if (CCUUiUtil.isAiroverseThemeEnabled(context)) {
                            initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.airoverse_primary));
                        } else {
                            initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.renatus_75F_accent));
                        }
                    }else{
                        initializeBACnet.setEnabled(false);
                        initializeBACnet.setClickable(false);
                        initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleClick(boolean isPortAvailable){

        if (!isPortAvailable && HttpServer.Companion.getInstance(context).isServerRunning()) {
            if(!DashboardUtilKt.isDashboardConfig(context)){
                CcuLog.d(TAG_CCU_BACNET,"5001 port is busy-->");
                Toast.makeText(context, "Port is busy try after some time", Toast.LENGTH_SHORT).show();
                HttpServer.Companion.getInstance(context).stopServer();
                return;
            }
        }
       else if(!isPortAvailable){
            CcuLog.d(TAG_CCU_BACNET,"5001 port is busy-->");
            Toast.makeText(context, "Port is busy try after some time", Toast.LENGTH_SHORT).show();
            HttpServer.Companion.getInstance(context).stopServer();
            return;
        }
        CcuLog.d(TAG_CCU_BACNET,"5001 port is free-->");
        if(validateEntries()) {
            toggleZoneToVirtualDeviceMapping.setEnabled(false);
            initializeBACnet.setVisibility(View.GONE);
            disableBACnet.setVisibility(View.VISIBLE);
            bacnetConfigureLayout.setVisibility(View.VISIBLE);
            hideView(description, tvDescription);
            hideView(location, tvLocation);
            if(password.getText().toString().trim().isEmpty()){
                textInputLayout.setVisibility(View.GONE);
            }else{
                textInputLayout.setVisibility(View.GONE);
                tvPassword.setVisibility(View.VISIBLE);
                tvPassword.setText("******");
            }
            ipAddressSpinner.setEnabled(false);
            hideView(localNetworkNumber, tvLocalNetworkNumber);
            hideView(virtualNetworkNumber, tvVirtualNetworkAddress);
            hideView(port, tvPort);
            hideView(ipDeviceInstanceNumber, tvIPDeviceInstanceNumber);
            hideView(apduTimeout, tvAPDUTimeout);
            hideView(numberOfAPDURetries, tvNumberofAPDURetries);
            hideView(apduSegmentTimeout, tvApduSegmentTimeout);
            hideView(etNotificationClassObjects, tvNotificationClassObjects);
            hideView(etTrendLogObjects, tvTrendLogObjects);
            hideView(etScheduleObjects, tvScheduleObjects);
            hideView(etOffsetValues, tvOffsetValue);
            if (!HttpServer.Companion.getInstance(context).isServerRunning()) {
                startRestServer();
            }
            sharedPreferences.edit().putBoolean(IS_BACNET_INITIALIZED, true).apply();
            BacnetUtilKt.launchBacApp(context, BROADCAST_BACNET_APP_START, "Start BACnet App", ipDeviceInstanceNumber.getText().toString(),false);
            performConfigFileBackup();
        }
    }

    public static boolean isPortAvailable(int port) {
        CcuLog.d(TAG_CCU_BACNET, "Checking port availability " + port);
        ServerSocket serverSocket = null;
        try {
            // Attempt to bind a server socket to the specified port.
            // If the port is available, it means the port is free.
            serverSocket = new ServerSocket(port);
            return true;
        } catch (IOException e) {
            // If an exception occurs during the binding attempt, it means the port is not available.
            CcuLog.d(TAG_CCU_BACNET, "Exception while checking port availability " + e);
            e.printStackTrace();
            return false;
        } finally {
            // Close the server socket to release the port.
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void executeTask() {
        executorService.submit(() -> {
            boolean isPortAvailable = isPortAvailable(5001);
            // Update the UI on the main thread
            requireActivity().runOnUiThread(() -> handleClick(isPortAvailable));
        });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Shutdown the ExecutorService when the Fragment is destroyed
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    private void getIpAddress() {
        ArrayList<String> ipAddresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                if (iface.getName().startsWith("wlan") || iface.getName().startsWith("eth")) {
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                            CcuLog.d(Tags.BACNET, "device interface and ip " + iface.getDisplayName() + " - " + addr.getHostAddress());
                            String networkType = iface.getName().startsWith("eth") ? "Ethernet" : "Wi-Fi";
                            String item = addr.getHostAddress() + " (" + networkType + ")";
                            if (!ipAddresses.contains(item)) { // Avoid duplicate items
                                ipAddresses.add(item);
                            }

                            for (InterfaceAddress interfaceAddress : iface.getInterfaceAddresses()) {
                                CcuLog.d(Tags.BACNET, "Address : " + interfaceAddress);
                                if (interfaceAddress.getAddress().equals(addr)) {
                                    subnetMask = interfaceAddress.getNetworkPrefixLength();
                                    CcuLog.d(Tags.BACNET, "Subnet Mask: " + subnetMask);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // If no addresses are found, add a default message
        if (ipAddresses.isEmpty()) {
            ipAddresses.add("No Network interfaces");
        }

        // Update the spinner with the dynamically fetched IP addresses
        ArrayAdapter<String> adapter = new CustomSelectionAdapter<>(this.requireContext(),R.layout.custom_spinner_view,ipAddresses,true);
        ipAddressSpinner.setAdapter(adapter);

        try {
            if (ipAddresses.size() > 1 ) { // We needs to Maintain the selection if we have 2 items
                String networkInterface = networkObject.get(NETWORK_INTERFACE).toString();
                if(!networkInterface.isEmpty()) {
                    for(String item : ipAddresses) {
                        if(item.contains(networkInterface) || (networkInterface.equals("Wifi") && item.contains("Wi-Fi"))) {
                            ipAddressSpinner.setSelection(ipAddresses.indexOf(item));
                            break;
                        }
                    }
                } else {
                    for(String item : ipAddresses) {
                        if(item.contains("Ethernet")) {
                            ipAddressSpinner.setSelection(ipAddresses.indexOf(item));
                            break;
                        }
                    }
                }
            }
        } catch (JSONException e) {
            CcuLog.e(TAG_CCU_BACNET, "Exception while fetching network object "+ e);
        }

        ipAddressSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedIp = ipAddresses.get(position);
                if(selectedIp.contains(" (")) {
                    deviceIpAddress = selectedIp.substring(0, selectedIp.indexOf(" ("));
                } else {
                    deviceIpAddress = "";
                }
                CcuLog.d(TAG_CCU_BACNET," item selected -- > "+deviceIpAddress);

                try {
                    if (selectedIp.contains("Wi-Fi")) {
                        networkObject.put(NETWORK_INTERFACE, "Wifi");
                    } else if (selectedIp.contains("Ethernet")) {
                        networkObject.put(NETWORK_INTERFACE, "Ethernet");
                    } else {
                        networkObject.put(NETWORK_INTERFACE, "");
                    }
                } catch (Exception e) {
                   CcuLog.e(TAG_CCU_BACNET,"Exception while updating network object "+e);
                }
                updateBbmdIPAddress();
                updateSelectedIpAddress();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        updateBbmdIPAddress();
    }

    private void updateBbmdIPAddress() {
        if ( !port.getText().toString().isBlank() && !port.getText().toString().isEmpty() ) {
            portNumber = Integer.parseInt(port.getText().toString());
        }
        String selectedDeviceType = sharedPreferences.getString(BACNET_DEVICE_TYPE, BACNET_DEVICE_TYPE_NORMAL);
        if (selectedDeviceType.equalsIgnoreCase(BACNET_DEVICE_TYPE_BBMD) || bacnetConfigSelectedPosition == BACnetConfigurationType.BBMD.ordinal()) {
            fillBbmdView(new DataBbmd(deviceIpAddress, portNumber, subnetMask),0);
        }
    }

    private CustomSpinnerDropDownAdapter getAdapterValue(ArrayList values) {
        return new CustomSpinnerDropDownAdapter(this.requireContext(), R.layout.spinner_dropdown_item, values);
    }


    private void addBbmdConfigurationRow(){
        CcuLog.d(TAG_CCU_BACNET, "add bbmd config");
        View bbmdView = LayoutInflater.from(getContext()).inflate(R.layout.lyt_bbmd_view, null);
        bbmdView.findViewById(R.id.tvBbmdRemove).setVisibility(View.VISIBLE);
        bbmdView.findViewById(R.id.tvBbmdRemove).setOnClickListener(view2 -> {
            View parent = bbmdView.findViewById(R.id.bbmdViewContainer);
            showAlertForRemoveBBMD(parent);
        });
        bbmdView.findViewById(R.id.tvBbmdEdit).setOnClickListener(view1 -> {
            showBBMDDialogView(bbmdView, true);
        });
        bbmdInputViews.addView(bbmdView);
    }

    private void createBbmdViewAndFillData(ArrayList<DataBbmd> listOdDataBbmd) {
        fillBbmdView(listOdDataBbmd.get(0), 0);
        for (int i = 1; i < listOdDataBbmd.size(); i++) {
            addBbmdConfigurationRow();
            DataBbmd item = listOdDataBbmd.get(i);
            fillBbmdView(item, i);
        }
    }

    private void fillBbmdView(DataBbmd item, int childPosition) {
        View childView = bbmdInputViews.getChildAt(childPosition);
        TextView etBbmdIp = childView.findViewById(R.id.etBbmdIp);
        TextView tvBbmdSelfIp = childView.findViewById(R.id.etBbmdIpSelf);
        TextView etBbmdPort = childView.findViewById(R.id.etBbmdPort);
        TextView etBbmdMask = childView.findViewById(R.id.etBbmdTime);
        ImageView ivBbmdRemove = childView.findViewById(R.id.tvBbmdRemove);
        ImageView ivBbmdEdit = childView.findViewById(R.id.tvBbmdEdit);
        CcuLog.d(TAG_CCU_BACNET, "getBbmdIp" + item.getBbmdIp() + "<--port-->" + item.getBbmdPort() + "<--mask-->" + item.getBbmdMask());
        etBbmdIp.setText(item.getBbmdIp());
        etBbmdPort.setText(String.valueOf(item.getBbmdPort()));
        etBbmdMask.setText(String.valueOf(item.getBbmdMask()));
        if(childPosition % 2 != 0){
            childView.setBackgroundColor(getResources().getColor(R.color.tuner_bg_grey));
        }

        if(childPosition == 0) { // Info for Own device IP address in BBMD table
            ivBbmdRemove.setEnabled(false);
            ivBbmdRemove.setColorFilter(ContextCompat.getColor(context, R.color.disable_element_color));
            ivBbmdEdit.setEnabled(false);
            ivBbmdEdit.setColorFilter(ContextCompat.getColor(context, R.color.disable_element_color));
            tvBbmdSelfIp.setVisibility(View.VISIBLE);
        }
    }

    private void fillFdView(DataFd item) {
        CcuLog.d(TAG_CCU_BACNET, "fdIp" + item.getBbmdIp() + "<--fdPort-->" + item.getBbmdPort() + "<--mask-->" + item.getBbmdMask());
        etFDIP.setText(item.getBbmdIp());
        etFDPort.setText(String.valueOf(item.getBbmdPort()));
        etFDTime.setText(String.valueOf(item.getBbmdMask()));
    }

    enum BACnetConfigurationType {
        BBMD, FD, NORMAL
    }

    private int getBacnetConfiguration (String selectedMode) {
        switch (selectedMode) {
            case BACNET_DEVICE_TYPE_BBMD:
                return BACnetConfigurationType.BBMD.ordinal();
            case BACNET_DEVICE_TYPE_FD:
                return BACnetConfigurationType.FD.ordinal();
            case BACNET_DEVICE_TYPE_NORMAL:
                return BACnetConfigurationType.NORMAL.ordinal();
            default:
        }
        return 0;
    }

    private void showBBMDDialogView(View bbmdView, boolean isUpdateBBMD) {
        // Create the dialog
        CcuLog.d(TAG_CCU_BACNET, "showBBMDDialogView called: isUpdateBBMD "+isUpdateBBMD);
        Dialog dialog = new Dialog(this.requireContext());
        dialog.setCancelable(true);

        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(this.requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_bbmd_view, null);
        dialog.setContentView(dialogView);

        // Initialize the input fields and buttons
        TextView title = dialogView.findViewById(R.id.title_text_bbmd_view);
        EditText ipAddressEditText = dialogView.findViewById(R.id.ip_address_edit_text);
        EditText portEditText = dialogView.findViewById(R.id.port_edit_text);
        EditText maskEditText = dialogView.findViewById(R.id.mask_edit_text);
        TextView cancelButton = dialogView.findViewById(R.id.cancel_button);
        TextView saveButton = dialogView.findViewById(R.id.save_button);

        TextView tvBbmdIp = bbmdView.findViewById(R.id.etBbmdIp);
        TextView tvBbmdPort = bbmdView.findViewById(R.id.etBbmdPort);
        TextView tvBbmdMask = bbmdView.findViewById(R.id.etBbmdTime);

        if(isUpdateBBMD){
            title.setText("Edit BBMD");
            ipAddressEditText.setText(tvBbmdIp.getText().toString());
            portEditText.setText(tvBbmdPort.getText().toString());
            maskEditText.setText(tvBbmdMask.getText().toString());
        }

        // Handle the Cancel button click
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss(); // Close the dialog
            }
        });

        // Handle the Save button click
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve values from the inputs
                String ipAddress = ipAddressEditText.getText().toString();
                String port = portEditText.getText().toString();
                String mask = maskEditText.getText().toString();

                // Validate input

                if (ipAddress.equals(EMPTY_STRING) || (!CCUUiUtil.isValidIPAddress(ipAddress.trim()))) {
                    ipAddressEditText.setError(getString(R.string.error_ip_address));
                    return;
                }

                if (port.equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(port), 47808, 47823, 1))) {
                    portEditText.setError("Port should be within 47808 and 47823");
                    return;
                }
                if (mask.equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(mask), 1, 32, 1))) {
                    maskEditText.setError("Mask should be within 1 and 32");
                    return ;
                }

                if (isUpdateBBMD) {
                    if(!validateBbmdData(ipAddress) && !ipAddress.equals(tvBbmdIp.getText().toString())) {
                        ipAddressEditText.setError("IP Address already exists");
                        return;
                    }
                } else {
                    if(!validateBbmdData(ipAddress)) {
                        ipAddressEditText.setError("IP Address already exists");
                        return;
                    }
                }

                ipAddressEditText.setError(null);
                portEditText.setError(null);
                maskEditText.setError(null);


                // Update the BBMD view with the new values
                if(isUpdateBBMD) {
                    DataBbmd dataBbmd = new DataBbmd(ipAddress, Integer.parseInt(port), Integer.parseInt(mask));
                    fillBbmdView(dataBbmd, bbmdInputViews.indexOfChild(bbmdView));
                    displayCustomToastMessageOnSuccess("BBMD updated successfully.");
                }
                else {
                    tvBbmdIp.setText(ipAddress);
                    tvBbmdPort.setText(port);
                    tvBbmdMask.setText(mask);
                    bbmdInputViews.addView(bbmdView);
                    updateBbmdListView();
                    displayCustomToastMessageOnSuccess("BBMD added successfully.");
                }
                updateBacnetConfigStateChanged(true);
                // Close the dialog after processing the input
                dialog.dismiss();
            }
        });

        // Show the dialog
        dialog.show();
    }

    private void updateBbmdListView() {
        for (int i = 0; i < bbmdInputViews.getChildCount(); i++) {
            View bbmdView = bbmdInputViews.getChildAt(i);
            if(i == 0) { // Info for Own device IP address in BBMD table
                ImageView ivBbmdRemove = bbmdView.findViewById(R.id.tvBbmdRemove);
                ImageView ivBbmdEdit = bbmdView.findViewById(R.id.tvBbmdEdit);
                TextView tvBbmdSelfIp = bbmdView.findViewById(R.id.etBbmdIpSelf);

                ivBbmdRemove.setEnabled(false);
                ivBbmdRemove.setColorFilter(ContextCompat.getColor(context, R.color.disable_element_color));
                ivBbmdEdit.setEnabled(false);
                ivBbmdEdit.setColorFilter(ContextCompat.getColor(context, R.color.disable_element_color));
                tvBbmdSelfIp.setVisibility(View.VISIBLE);
            }
            if(i % 2 != 0){
                bbmdView.setBackgroundColor(getResources().getColor(R.color.tuner_bg_grey));
            }
            else {
                bbmdView.setBackgroundColor(getResources().getColor(R.color.white));
            }
        }
    }
    private void displayCustomToastMessageOnSuccess(String message) {
        Toast toast = new Toast(Globals.getInstance().getApplicationContext());
        toast.setGravity(Gravity.BOTTOM, 50, 50);
        View toastSuccess = getLayoutInflater().inflate(R.layout.custom_toast_bacnet_configuration,
                rootView.findViewById(R.id.custom_toast_layout));
        toast.setView(toastSuccess);

        TextView textView = toast.getView().findViewById(R.id.custom_toast_message_detail);
        textView.setText(message);
        textView.setTextSize(21);

        TextView textViewTitle = toast.getView().findViewById(R.id.custom_toast_message_success);
        textViewTitle.setTypeface(textViewTitle.getTypeface(), Typeface.BOLD);
        textViewTitle.setTextSize(21);

        LinearLayout linearLayout = toast.getView().findViewById(R.id.custom_toast_layout);
        linearLayout.setPadding(20, 20, 20, 20);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }

    private void showAlertForRemoveBBMD(View parent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_schedule, null);
        TextView msg = dialogView.findViewById(R.id.tvMessage);
        TextView cancel = dialogView.findViewById(R.id.btnCancel);
        cancel.setText(getText(R.string.button_cancel));
        TextView proceed = dialogView.findViewById(R.id.btnProceed);
        proceed.setText(getText(R.string.button_delete));

        String ipAddress = ((TextView) parent.findViewById(R.id.etBbmdIp)).getText().toString();
        String message = "Are you sure you want to delete BBMD with <br>IP Address: <b>" + ipAddress + "?</b>";

        // Use Html to style the text
        msg.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
        msg.setTextSize(20);
        builder.setCancelable(false)
                .setView(dialogView);
        AlertDialog alert = builder.create();
        alert.show();

        cancel.setOnClickListener(view -> {alert.dismiss();});
        proceed.setOnClickListener(view -> {
            bbmdInputViews.removeView(parent);
            updateBbmdListView();
            updateBacnetConfigStateChanged(true);
            alert.dismiss();
        });
    }

    private void updateBacnetConfigStateChanged(boolean state) {
        RenatusLandingActivity.isBacnetConfigStateChanged = state;
    }

    private void showConfirmationForBacnetConfiguration(boolean isTabChanged, int position) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogView = getLayoutInflater().inflate(R.layout.bacnet_config_confirmation_alert, null);
        TextView msg = dialogView.findViewById(R.id.tvMessage);
        TextView lftBtn = dialogView.findViewById(R.id.btn_discard);
        TextView rgtBtn = dialogView.findViewById(R.id.btn_stay);

        String message = "You have unsaved <b>BACnet Config</b> changes. Are you sure you want to change the tab?";
        if (!isTabChanged) {
            message = "Are you sure you want to change configuration from <b>"+BACnetConfigurationType.values()[spinnerBacnetConfigAdapter.getConfiguredIndex()]+"</b> to <b>"+BACnetConfigurationType.values()[position]+"</b>? Any configuration saved as <b>"+BACnetConfigurationType.values()[spinnerBacnetConfigAdapter.getConfiguredIndex()]+"</b> would be lost.";
            lftBtn.setText(getText(R.string.button_cancel));
            rgtBtn.setText(getText(R.string.button_delete));
        }

        if(message.contains("FD")) {
            message = message.replace("FD", "Foreign Device");
        }
        if (message.contains("NORMAL")) {
            message = message.replace("NORMAL", "Normal");
        }

        msg.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
        builder.setView(dialogView)
                .setCancelable(false);

        AlertDialog alert = builder.create();
        alert.show();

        lftBtn.setOnClickListener(view -> {
            if (!isTabChanged) {
                bacnetConfigSpinner.setSelection(bacnetConfigSelectedPosition);// Stay on the same tab
            }
            else {
                RenatusLandingActivity.isBacnetConfigStateChanged = false;
                SettingsFragment.SettingFragmentHandler.sendEmptyMessage(position);
            }

            alert.dismiss();
        });
        rgtBtn.setOnClickListener(view -> {
            if (!isTabChanged) {
                saveBacnetConfiguration(bacnetConfigSelectedPosition);
            }
            alert.dismiss();
        });
    }

    public void tryToNavigateTab(int intent) {
        CcuLog.d(TAG_CCU_BACNET, "IsStateChanged: "+RenatusLandingActivity.isBacnetConfigStateChanged+" tryToNavigateTab: "+intent);
        if (RenatusLandingActivity.isBacnetConfigStateChanged) {
            showConfirmationForBacnetConfiguration(true, intent);
        }
        else {
            SettingsFragment.SettingFragmentHandler.sendEmptyMessage(intent);
        }
    }

    private void updateSelectedIpAddress() {
        try {
            if (!deviceIpAddress.trim().isEmpty()) {
                CcuLog.i(TAG_CCU_BACNET, "Key: ipAddress, Value: " + ipAddressSpinner.getSelectedItem().toString() + ", isValid: " + CCUUiUtil.isValidIPAddress(deviceIpAddress));
                if (!CCUUiUtil.isValidIPAddress(deviceIpAddress)) {
                    initializeBACnet.setEnabled(false);
                    initializeBACnet.setClickable(false);
                    initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
                } else {
                    networkObject.put(IP_ADDRESS, deviceIpAddress.trim());
                    sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                    if (validateEntries()) {
                        initializeBACnet.setEnabled(true);
                        initializeBACnet.setClickable(true);

                        TypedValue typedValue = new TypedValue();
                        requireActivity().getTheme().resolveAttribute(R.attr.orange_75f, typedValue, true);
                        int color = typedValue.data;
                        initializeBACnet.setTextColor(color);
                    } else {
                        initializeBACnet.setEnabled(false);
                        initializeBACnet.setClickable(false);
                        initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
                    }
                }
            } else {
                networkObject.put(IP_ADDRESS, "");
                sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                if (validateEntries()) {
                    initializeBACnet.setEnabled(true);
                    initializeBACnet.setClickable(true);

                    TypedValue typedValue = new TypedValue();
                    requireActivity().getTheme().resolveAttribute(R.attr.orange_75f, typedValue, true);
                    int color = typedValue.data;
                    initializeBACnet.setTextColor(color);
                } else {
                    initializeBACnet.setEnabled(false);
                    initializeBACnet.setClickable(false);
                    initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private ArrayList<String> getPortAddressMstpDevices(){
        ArrayList<String> usbSerialPorts = new ArrayList<>();

        File devDirectory = new File("/dev");
        File[] files = devDirectory.listFiles();

        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (fileName.startsWith("ttyUSB")) {
                    usbSerialPorts.add("/dev/" + fileName);
                }
            }
        }

        return usbSerialPorts;
    }

    private void runChmodUsbDevices() {
        CcuLog.e(TAG_CCU_BACNET_MSTP, "--runChmodUsbDevices--");
        try {
            // Start root shell
            Process process = Runtime.getRuntime().exec("su");

            String[] commands = {
                    "chmod 755 /dev/bus/usb\n",
                    "chmod 755 /dev/bus/usb/*\n",
                    "chmod 666 /dev/bus/usb/*/*\n",
                    "exit\n"
            };

            // Send commands to the shell
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                os.writeBytes(command);
            }
            os.flush();

            // Wait for the command to complete
            process.waitFor();

            // Log exit code
            CcuLog.d(TAG_CCU_BACNET_MSTP, "chmod executed with exit code: " + process.exitValue());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            CcuLog.e(TAG_CCU_BACNET_MSTP, "Error executing chmod", e);
        }
    }

    private void prepareMstpInit(){
        executorService.submit(() -> {
            RenatusApp.backgroundServiceInitiator.unbindServices();
            CcuLog.d(TAG_CCU_BACNET_MSTP, "--step 1--unbind modbus service--");

            Intent intent = new Intent(requireContext(), UsbModbusService.class);
            requireContext().stopService(intent);
            CcuLog.d(TAG_CCU_BACNET_MSTP, "--step 2--stop modbus service--");

            CcuLog.d(TAG_CCU_BACNET_MSTP, "--step 3--apply permissions--");

            UsbHelper.runChmodUsbDevices();
            UsbPortTrigger.triggerUsbSerialBinding(requireContext());
            UsbHelper.listUsbDevices(requireContext());
            UsbHelper.runAsRoot("ls /dev/tty*");

            CcuLog.d(TAG_CCU_BACNET_MSTP, "--step 4--update ui--");
            updateMstpUi();

            if(!isUSBSerialPortAvailable){
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Check USB connection, MSTP can not initialized.", Toast.LENGTH_LONG).show();
                });
                CcuLog.d(TAG_CCU_BACNET_MSTP, "--step 4--update ui Check USB connection, mstp can not initialized.--");
                return;
            }

            writeIntPref(PREF_MSTP_BAUD_RATE, Integer.parseInt(mstpSpinnerBaudRate.getSelectedItem().toString()));
            writeIntPref(PREF_MSTP_SOURCE_ADDRESS, Integer.parseInt(etMstpSourceAddress.getText().toString()));
            writeIntPref(PREF_MSTP_MAX_MASTER, Integer.parseInt(etMstpMaxMaster.getText().toString()));
            writeIntPref(PREF_MSTP_MAX_FRAME, Integer.parseInt(etMstpMaxFrame.getText().toString()));
            writeIntPref(PREF_MSTP_DEVICE_ID, Integer.parseInt(etMstpDeviceId.getText().toString()));

            if (!HttpServer.Companion.getInstance(context).isServerRunning()) {
                startRestServer();
            }

            DataMstpObj dataMstpObj = new DataMstpObj();
            dataMstpObj.setDataMstp(new DataMstp(
                    Integer.parseInt(mstpSpinnerBaudRate.getSelectedItem().toString()),
                    Integer.parseInt(etMstpSourceAddress.getText().toString()),
                    Integer.parseInt(etMstpMaxMaster.getText().toString()),
                    Integer.parseInt(etMstpMaxFrame.getText().toString()),
                    Integer.parseInt(etMstpDeviceId.getText().toString()),
                    portAddress
            ));

            String jsonString = new Gson().toJson(dataMstpObj);
            CcuLog.d(TAG_CCU_BACNET_MSTP, "--step 5--create mstp init json--"+jsonString);
            sharedPreferences.edit().putString(BACNET_MSTP_CONFIGURATION, jsonString).apply();
            sharedPreferences.edit().putBoolean(IS_BACNET_MSTP_INITIALIZED, true).apply();


            if (BacnetUtilKt.isAppRunning(BAC_APP_PACKAGE_NAME)) {
                Intent intentBacApp = new Intent("MSTP_CONFIGURATION");
                intentBacApp.putExtra("message", "MSTP");
                intentBacApp.putExtra("data", jsonString);
                context.sendBroadcast(intentBacApp);
            } else {
                BacnetUtilKt.launchBacApp(context, BROADCAST_BACNET_APP_START, "Start BACnet App", ipDeviceInstanceNumber.getText().toString(),true);
            }
            requireActivity().runOnUiThread(() -> {
                hideMstpConfigView();
            });

            CcuLog.d(TAG_CCU_BACNET_MSTP, "MSTP configuration initialized");
        });
    }
}
