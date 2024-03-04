package a75f.io.renatus;

import static a75f.io.device.bacnet.BacnetConfigConstants.APDU_SEGMENT_TIMEOUT;
import static a75f.io.device.bacnet.BacnetConfigConstants.APDU_TIMEOUT;
import static a75f.io.device.bacnet.BacnetConfigConstants.BACNET_CONFIGURATION;
import static a75f.io.device.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_CONFIGURATION_TYPE;
import static a75f.io.device.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_START;
import static a75f.io.device.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_STOP;
import static a75f.io.device.bacnet.BacnetConfigConstants.DESCRIPTION;
import static a75f.io.device.bacnet.BacnetConfigConstants.EMPTY_STRING;
import static a75f.io.device.bacnet.BacnetConfigConstants.IP_ADDRESS;
import static a75f.io.device.bacnet.BacnetConfigConstants.IP_DEVICE_INSTANCE_NUMBER;
import static a75f.io.device.bacnet.BacnetConfigConstants.IP_DEVICE_OBJECT_NAME;
import static a75f.io.device.bacnet.BacnetConfigConstants.IS_BACNET_INITIALIZED;
import static a75f.io.device.bacnet.BacnetConfigConstants.LOCAL_NETWORK_NUMBER;
import static a75f.io.device.bacnet.BacnetConfigConstants.LOCATION;
import static a75f.io.device.bacnet.BacnetConfigConstants.NULL;
import static a75f.io.device.bacnet.BacnetConfigConstants.NUMBER_OF_APDU_RETRIES;
import static a75f.io.device.bacnet.BacnetConfigConstants.NUMBER_OF_NOTIFICATION_CLASS_OBJECTS;
import static a75f.io.device.bacnet.BacnetConfigConstants.NUMBER_OF_OFFSET_VALUES;
import static a75f.io.device.bacnet.BacnetConfigConstants.NUMBER_OF_SCHEDULE_OBJECTS;
import static a75f.io.device.bacnet.BacnetConfigConstants.NUMBER_OF_TREND_LOG_OBJECTS;
import static a75f.io.device.bacnet.BacnetConfigConstants.PASSWORD;
import static a75f.io.device.bacnet.BacnetConfigConstants.PORT;
import static a75f.io.device.bacnet.BacnetConfigConstants.VIRTUAL_NETWORK_NUMBER;
import static a75f.io.device.bacnet.BacnetConfigConstants.ZONE_TO_VIRTUAL_DEVICE_MAPPING;
import static a75f.io.device.bacnet.BacnetUtilKt.sendBroadCast;
import static a75f.io.logic.L.TAG_CCU_BACNET;
import static a75f.io.logic.service.FileBackupJobReceiver.performConfigFileBackup;
import static a75f.io.renatus.UtilityApplication.context;
import static a75f.io.renatus.UtilityApplication.startRestServer;
import static a75f.io.renatus.UtilityApplication.stopRestServer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import a75f.io.api.haystack.Tags;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.DataBbmd;
import a75f.io.renatus.util.DataBbmdObj;
import a75f.io.renatus.util.DataFd;
import a75f.io.renatus.util.DataFdObj;
import a75f.io.renatus.views.CustomCCUSwitch;
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter;
import a75f.io.renatus.util.DataFd;
import a75f.io.renatus.util.DataFdObj;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class Communication extends Fragment {
    
    private final String PREF_MB_BAUD_RATE = "mb_baudrate";
    private final String PREF_MB_PARITY = "mb_parity";
    private final String PREF_MB_DATA_BITS = "mb_databits";
    private final String PREF_MB_STOP_BITS = "mb_stopbits";
    
    @BindView(R.id.spinnerBaudRate) Spinner spinnerBaudRate;
    
    @BindView(R.id.spinnerParity) Spinner spinnerParity;
    
    @BindView(R.id.spinnerDatabits) Spinner spinnerDatabits;
    
    @BindView(R.id.spinnerStopBits) Spinner spinnerStopbits;
    
    @BindView(R.id.btnRestart) Button btnRestart;

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

    @BindView(R.id.etIP) EditText ipAddress;

    @BindView(R.id.tvIP1) TextView tvIPAddress;

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

    @BindView(R.id.rg_configuration_type)
    RadioGroup rgConfigurationType;

    @BindView(R.id.etOffsetValues) EditText etOffsetValues;

    @BindView(R.id.tvOffsetValue1) TextView tvOffsetValue;

    @BindView(R.id.fdInputView) View fdInputView;

    @BindView(R.id.bbmdInputContainer) View bbmdInputContainer;

    @BindView(R.id.tvFdAdd) View tvfDAdd;

    @BindView(R.id.tvFdSubmit) View tvFdSubmit;

    @BindView(R.id.fdInputViews) LinearLayout fdInputViews;

    @BindView(R.id.tvBbmdAdd) View tvBbmdAdd;

    @BindView(R.id.tvBbmdSubmit) View tvBbmdSubmit;

    @BindView(R.id.bbmdInputViews)
    LinearLayout bbmdInputViews;

    @BindView(R.id.iv_refresh_ip)
    ImageView ivRefreshView;

    SharedPreferences sharedPreferences;
    JSONObject config;
    JSONObject networkObject;
    JSONObject deviceObject;
    JSONObject objectConf;

    private ExecutorService executorService;

    boolean isZoneToVirtualDeviceErrorShowing = false;

    public Communication() {
    
    }
    
    public static Communication newInstance() {
        return new Communication();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
                                                                                                  Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_modbusconfig, container, false);
        ButterKnife.bind(this, rootView);
        executorService = Executors.newFixedThreadPool(1);
        return rootView;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

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
    
        btnRestart.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                CCUUiUtil.triggerRestart(getActivity());
            }
        });
        setSpinnerBackground();
        String confString = sharedPreferences.getString(BACNET_CONFIGURATION,null);

        try {
            config = new JSONObject(confString);
            networkObject = config.getJSONObject("network");
            deviceObject = config.getJSONObject("device");
            objectConf = config.getJSONObject("objectConf");
            setBACnetConfigurationValues();
            doBACnetConfigurationValidation();
            initializeBACnet();
        } catch (JSONException e) {
            Log.d(TAG_CCU_BACNET,"Config data: "+config+", Error message: "+e.getMessage());
            e.printStackTrace();
        }

        rgConfigurationType.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            RadioButton radioButton = view.findViewById(checkedId);
            String label = radioButton.getText().toString();
            Log.d(TAG_CCU_BACNET, "radioButton selected-->"+label);
            handleConfigurationType(label);
        });

        bbmdInputContainer.setVisibility(View.GONE);

        fdInputView.setVisibility(View.GONE);

        tvFdSubmit.setOnClickListener(view1 -> {
            if (validateFdData()) {
                Toast.makeText(context, "device configured as fd", Toast.LENGTH_SHORT).show();
                try {
                    DataFdObj dataFdObj = new DataFdObj();
                    for (int i = 0; i < fdInputViews.getChildCount(); i++) {
                        View childView = fdInputViews.getChildAt(i);
                        EditText etBbmdIp = childView.findViewById(R.id.etFdIp);
                        EditText etBbmdPort = childView.findViewById(R.id.etFdPort);
                        EditText etBbmdMask = childView.findViewById(R.id.etFdTime);

                        Log.d(TAG_CCU_BACNET, "fd entry at->" + i + "<--ip-->" + etBbmdIp.getText().toString() + "<--port-->" +
                                etBbmdPort.getText().toString() + "<--time-->" + etBbmdMask.getText().toString());
//                        dataFdObj.addItem(new DataFd(etBbmdIp.getText().toString(), Integer.parseInt(etBbmdPort.getText().toString()),
//                                Integer.parseInt(etBbmdMask.getText().toString())));
                        dataFdObj.setDataFd(new DataFd(etBbmdIp.getText().toString(), Integer.parseInt(etBbmdPort.getText().toString()),
                                Integer.parseInt(etBbmdMask.getText().toString())));
                    }

                    String jsonString = new Gson().toJson(dataFdObj);
                    Log.d(TAG_CCU_BACNET, "fd output-->" + jsonString);

                    RadioButton radioButton = view.findViewById(R.id.rb_foreign_device);
                    String label = radioButton.getText().toString();
                    Intent intent = new Intent(BROADCAST_BACNET_APP_CONFIGURATION_TYPE);
                    intent.putExtra("message", label);
                    intent.putExtra("data", jsonString);
                    context.sendBroadcast(intent);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });

        tvfDAdd.setOnClickListener(view1 -> {
            Log.d(TAG_CCU_BACNET, "add fd config");
            View fdView = LayoutInflater.from(getContext()).inflate(R.layout.lyt_fd_view, null);
            fdView.findViewById(R.id.tvfDRemove).setOnClickListener(view2 -> {
                View parent = fdView.findViewById(R.id.fdViewContainer);
                fdInputViews.removeView(parent);
            });
            fdInputViews.addView(fdView);
        });

        tvBbmdAdd.setOnClickListener(view1 -> {
            Log.d(TAG_CCU_BACNET, "add bbmd config");
            View bbmdView = LayoutInflater.from(getContext()).inflate(R.layout.lyt_bbmd_view, null);
            bbmdView.findViewById(R.id.tvBbmdRemove).setOnClickListener(view2 -> {
                View parent = bbmdView.findViewById(R.id.bbmdViewContainer);
                bbmdInputViews.removeView(parent);
            });
            bbmdInputViews.addView(bbmdView);
        });

        tvBbmdSubmit.setOnClickListener(view1 -> {
            if (validateBbmdData()) {
                Toast.makeText(context, "device configured as bbmd", Toast.LENGTH_SHORT).show();
                try {
                    DataBbmdObj dataBbmdObj = new DataBbmdObj();
                    for (int i = 0; i < bbmdInputViews.getChildCount(); i++) {
                        View childView = bbmdInputViews.getChildAt(i);
                        EditText etBbmdIp = childView.findViewById(R.id.etBbmdIp);
                        EditText etBbmdPort = childView.findViewById(R.id.etBbmdPort);
                        EditText etBbmdMask = childView.findViewById(R.id.etBbmdTime);

                        Log.d(TAG_CCU_BACNET, "bbmd entry at->" + i + "<--ip-->" + etBbmdIp.getText().toString() + "<--port-->" +
                                etBbmdPort.getText().toString() + "<--mask-->" + etBbmdMask.getText().toString());
                        dataBbmdObj.addItem(new DataBbmd(etBbmdIp.getText().toString(), Integer.parseInt(etBbmdPort.getText().toString()),
                                Integer.parseInt(etBbmdMask.getText().toString())));
                    }

                    String jsonString = new Gson().toJson(dataBbmdObj);
                    Log.d(TAG_CCU_BACNET, "bbmd output-->" + jsonString);

                    RadioButton radioButton = view.findViewById(R.id.rb_bbmd);
                    String label = radioButton.getText().toString();
                    Intent intent = new Intent(BROADCAST_BACNET_APP_CONFIGURATION_TYPE);
                    intent.putExtra("message", label);
                    intent.putExtra("data", jsonString);
                    context.sendBroadcast(intent);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });

        ivRefreshView.setOnClickListener(view12 -> {
            if(!UtilityApplication.isBACnetIntialized()){
                getIpAddress();
            }else{
                Toast.makeText(requireContext(), "Disable bacnet to fetch ip", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateFdData() {
        for (int i = 0; i < fdInputViews.getChildCount(); i++) {
            View childView = fdInputViews.getChildAt(i);
            EditText etBbmdIp = childView.findViewById(R.id.etFdIp);
            EditText etBbmdPort = childView.findViewById(R.id.etFdPort);
            EditText etBbmdMask = childView.findViewById(R.id.etFdTime);

            if (etBbmdIp.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidIPAddress(etBbmdIp.getText().toString().trim()))) {
                etBbmdIp.setError(getString(R.string.error_ip_address));
                return false;
            }
            if (etBbmdPort.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etBbmdPort.getText().toString()), 0, Integer.MAX_VALUE, 1))) {
                etBbmdPort.setError(getString(R.string.txt_valid_number));
                return false;
            }
//            if (etBbmdPort.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etBbmdPort.getText().toString()), 4069, 65535, 1))) {
//                etBbmdPort.setError(getString(R.string.txt_error_port));
//                return false;
//            }
            if (etBbmdMask.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etBbmdMask.getText().toString()), 0, Integer.MAX_VALUE, 1))) {
                etBbmdMask.setError(getString(R.string.txt_valid_number));
                return false;
            }
            etBbmdIp.setError(null);
            etBbmdPort.setError(null);
            etBbmdMask.setError(null);
        }
        return true;
    }

    private boolean validateBbmdData() {
        for (int i = 0; i < bbmdInputViews.getChildCount(); i++) {
            View childView = bbmdInputViews.getChildAt(i);
            EditText etBbmdIp = childView.findViewById(R.id.etBbmdIp);
            EditText etBbmdPort = childView.findViewById(R.id.etBbmdPort);
            EditText etBbmdMask = childView.findViewById(R.id.etBbmdTime);

            if (etBbmdIp.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidIPAddress(etBbmdIp.getText().toString().trim()))) {
                etBbmdIp.setError(getString(R.string.error_ip_address));
                return false;
            }
            /*if (etBbmdPort.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etBbmdPort.getText().toString()), 4069, 65535, 1))) {
                etBbmdPort.setError(getString(R.string.txt_error_port));
                return false;
            }*/
            if (etBbmdMask.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etBbmdMask.getText().toString()), 0, Integer.MAX_VALUE, 1))) {
                etBbmdMask.setError(getString(R.string.txt_valid_number));
                return false;
            }
            etBbmdIp.setError(null);
            etBbmdPort.setError(null);
            etBbmdMask.setError(null);
        }
        return true;
    }

    private void handleConfigurationType(String label){
        if(label.equalsIgnoreCase(getString(R.string.label_bbmd))){
            bbmdInputContainer.setVisibility(View.VISIBLE);
            fdInputView.setVisibility(View.GONE);
        }else if(label.equalsIgnoreCase(getString(R.string.label_foreign_device))){
            fdInputView.setVisibility(View.VISIBLE);
            bbmdInputContainer.setVisibility(View.GONE);
        }else{
            fdInputView.setVisibility(View.GONE);
            bbmdInputContainer.setVisibility(View.GONE);
            sendBroadCast(context, BROADCAST_BACNET_APP_CONFIGURATION_TYPE, label);
        }
    }

    private void setBACnetConfigurationValues() {
        try {
            tvIPDeviceObjectName.setText(deviceObject.getString(IP_DEVICE_OBJECT_NAME));
            toggleZoneToVirtualDeviceMapping.setChecked(config.getBoolean(ZONE_TO_VIRTUAL_DEVICE_MAPPING));
            description.setText(deviceObject.getString(DESCRIPTION).equals(NULL) || deviceObject.getString(DESCRIPTION).equals(EMPTY_STRING)  ? EMPTY_STRING : deviceObject.getString(DESCRIPTION).trim());
            location.setText((deviceObject.getString(LOCATION).equals(NULL)) || (deviceObject.getString(LOCATION).equals(EMPTY_STRING)) ? EMPTY_STRING : deviceObject.getString(LOCATION).trim());
            password.setText((deviceObject.getString(PASSWORD).equals(NULL)) || (deviceObject.getString(PASSWORD).equals(EMPTY_STRING)) ? EMPTY_STRING : deviceObject.getString(PASSWORD).trim());
            ipAddress.setText(networkObject.getString(IP_ADDRESS));
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
            Log.d(TAG_CCU_BACNET, "Exception while populating");
            e.printStackTrace();
        }
    }

    private void doBACnetConfigurationValidation() {
        description.addTextChangedListener(new EditTextWatcher(description));
        location.addTextChangedListener(new EditTextWatcher(location));
        password.addTextChangedListener(new EditTextWatcher(password));
        ipAddress.addTextChangedListener(new EditTextWatcher(ipAddress));
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
    }

    private void initializeBACnet() {

        if(UtilityApplication.isBACnetIntialized()){
            initializeBACnet.setVisibility(View.GONE);
            disableBACnet.setVisibility(View.VISIBLE);
            toggleZoneToVirtualDeviceMapping.setEnabled(false);
            hideView(description, tvDescription);
            hideView(location, tvLocation);
            if(password.getText().toString().trim().equals("")){
                textInputLayout.setVisibility(View.GONE);
            }else{
                textInputLayout.setVisibility(View.GONE);
                tvPassword.setVisibility(View.VISIBLE);
                tvPassword.setText("******");
            }
            hideView(ipAddress, tvIPAddress);
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
            initializeBACnet.setVisibility(View.VISIBLE);
            toggleZoneToVirtualDeviceMapping.setEnabled(true);
            setBACnetConfigurationValues();
        }


        initializeBACnet.setOnClickListener(view -> executeTask());

        disableBACnet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleZoneToVirtualDeviceMapping.setEnabled(true);
                initializeBACnet.setVisibility(View.VISIBLE);
                disableBACnet.setVisibility(View.GONE);
                description.setVisibility(View.VISIBLE);
                tvDescription.setVisibility(View.GONE);
                location.setVisibility(View.VISIBLE);
                tvLocation.setVisibility(View.GONE);
                textInputLayout.setVisibility(View.VISIBLE);
                password.setVisibility(View.VISIBLE);
                tvPassword.setVisibility(View.GONE);
                ipAddress.setVisibility(View.VISIBLE);
                tvIPAddress.setVisibility(View.GONE);
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
                stopRestServer();
                sharedPreferences.edit().putBoolean(IS_BACNET_INITIALIZED, false).apply();
                sendBroadCast(context, BROADCAST_BACNET_APP_STOP, "Stop BACnet App");
                performConfigFileBackup();
            }
        });

        toggleZoneToVirtualDeviceMapping.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                try {
                    config.put(ZONE_TO_VIRTUAL_DEVICE_MAPPING, isChecked);
                    sharedPreferences.edit().putString(BACNET_CONFIGURATION,config.toString()).apply();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        tvzvdmHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

            }
        });
    }

    private void hideView(EditText etView, TextView tvView) {
        etView.setVisibility(View.GONE);
        tvView.setText(etView.getText().toString().trim());
        tvView.setVisibility(View.VISIBLE);
    }

    private boolean validateEntries() {

        if(ipAddress.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidIPAddress(ipAddress.getText().toString().trim()))) return false;

        if(localNetworkNumber.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(localNetworkNumber.getText().toString()), 1, 65535, 1)))  return false;

        if(virtualNetworkNumber.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(virtualNetworkNumber.getText().toString()), 1, 65535, 1)))  return false;

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

        if(etOffsetValues.getText().toString().equals(EMPTY_STRING) || (!CCUUiUtil.isValidNumber(Integer.parseInt(etOffsetValues.getText().toString()), 0, 100, 1)))  return false;
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
        editor.commit();
    }
    

    private void setSpinnerBackground(){
        CCUUiUtil.setSpinnerDropDownColor(spinnerBaudRate,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spinnerParity,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spinnerDatabits,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spinnerBaudRate,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spinnerStopbits,getContext());
    }

    private class EditTextWatcher implements TextWatcher {

        private View view;
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
                        Log.i(TAG_CCU_BACNET, "Key: description, Value: "+description.getText().toString());
                        String description1 = description.getText().toString();
                        deviceObject.put(DESCRIPTION,description1);
                        sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    break;

                case R.id.etLocation:
                    try {
                        Log.i(TAG_CCU_BACNET, "Key: location, Value: "+location.getText().toString());
                        String location1 = location.getText().toString().trim();
                        deviceObject.put(LOCATION, location1);
                        sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    break;

                case R.id.etPassword:
                    try {
                        Log.i(TAG_CCU_BACNET, "Key: location, Value: "+password.getText().toString());
                        if (!CCUUiUtil.isAlphaNumeric(password.getText().toString())) {
                            ipAddress.setError(getString(R.string.error_password));
                        } else {
                            ipAddress.setError(null);
                            deviceObject.put(PASSWORD, password.getText().toString().trim());
                            sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    break;

                case R.id.etIP:
                    try {
                        if (ipAddress.getText().toString().trim().length() > 0) {
                            Log.i(TAG_CCU_BACNET, "Key: ipAddress, Value: "+ipAddress.getText().toString()+", isValid: "+CCUUiUtil.isValidIPAddress(ipAddress.getText().toString()));
                            if (!CCUUiUtil.isValidIPAddress(ipAddress.getText().toString())) {
                                ipAddress.setError(getString(R.string.error_ip_address));
                                initializeBACnet.setEnabled(false);
                                initializeBACnet.setClickable(false);
                                initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
                            } else {
                                ipAddress.setError(null);
                                networkObject.put(IP_ADDRESS, ipAddress.getText().toString().trim());
                                sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                                if(validateEntries()){
                                    initializeBACnet.setEnabled(true);
                                    initializeBACnet.setClickable(true);
                                    initializeBACnet.setTextColor(R.attr.orange_75f);
                                }else{
                                    initializeBACnet.setEnabled(false);
                                    initializeBACnet.setClickable(false);
                                    initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
                                }
                            }
                        }else{
                            ipAddress.setError(null);
                            networkObject.put(IP_ADDRESS,"");
                            sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                            if(validateEntries()){
                                initializeBACnet.setEnabled(true);
                                initializeBACnet.setClickable(true);
                                initializeBACnet.setTextColor(R.attr.orange_75f);
                            }else{
                                initializeBACnet.setEnabled(false);
                                initializeBACnet.setClickable(false);
                                initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
                            }
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    break;

                case R.id.etlocalNetworkAddress:
                    validateAndSetValue(localNetworkNumber, LOCAL_NETWORK_NUMBER, localNetworkNumber.getText().toString(), networkObject, 1,65535, 1, getString(R.string.error_local_network_number));
                    break;

                case R.id.etVirtualNetworkAddress:
                    validateAndSetValue(virtualNetworkNumber, VIRTUAL_NETWORK_NUMBER, virtualNetworkNumber.getText().toString(), networkObject, 1,65535, 1, getString(R.string.error_virtual_network_number));
                    break;

                case R.id.etPort:
                    validateAndSetValue(port, PORT, port.getText().toString(), networkObject, 4096,65535, 1, getString(R.string.txt_error_port));
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
            }
        }

        private void validateAndSetValue(EditText view, String key, String value, JSONObject jsonObject, int min, int max,  int multiple, String error) {
            try {
                if (value.trim().length() > 0) {
                    Log.i(TAG_CCU_BACNET, "Key: "+key+", Value: "+value+", isValid: "+CCUUiUtil.isValidNumber(Integer.parseInt(value), min, max, multiple));
                    if (!CCUUiUtil.isValidNumber(Integer.parseInt(value.trim()),min,max, multiple)) {
                        view.setError(error);
                        initializeBACnet.setEnabled(false);
                        initializeBACnet.setClickable(false);
                        initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
                    } else {
                        view.setError(null);
                        jsonObject.put(key,Integer.parseInt(value));
                        sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                        if(validateEntries()){
                            initializeBACnet.setEnabled(true);
                            initializeBACnet.setClickable(true);
                            if (CCUUiUtil.isCarrierThemeEnabled(context)) {
                                initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.carrier_75f));
                            } else if (CCUUiUtil.isDaikinEnvironment(context)) {
                                initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.daikin_75f));
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
        if(!isPortAvailable){
            Toast.makeText(context, "Port is busy try after some time", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG_CCU_BACNET,"5001 port is free-->");
        if(validateEntries()) {
            toggleZoneToVirtualDeviceMapping.setEnabled(false);
            initializeBACnet.setVisibility(View.GONE);
            disableBACnet.setVisibility(View.VISIBLE);
            hideView(description, tvDescription);
            hideView(location, tvLocation);
            if(password.getText().toString().trim().equals("")){
                textInputLayout.setVisibility(View.GONE);
            }else{
                textInputLayout.setVisibility(View.GONE);
                tvPassword.setVisibility(View.VISIBLE);
                tvPassword.setText("******");
            }
            hideView(ipAddress, tvIPAddress);
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
            startRestServer();
            sharedPreferences.edit().putBoolean(IS_BACNET_INITIALIZED, true).apply();
            sendBroadCast(context, BROADCAST_BACNET_APP_START, "Start BACnet App", ipDeviceInstanceNumber.getText().toString());
            performConfigFileBackup();
        }
    }

    public static boolean isPortAvailable(int port) {
        ServerSocket serverSocket = null;
        try {
            // Attempt to bind a server socket to the specified port.
            // If the port is available, it means the port is free.
            serverSocket = new ServerSocket(port);
            return true;
        } catch (IOException e) {
            // If an exception occurs during the binding attempt, it means the port is not available.
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
            requireActivity().runOnUiThread(() -> {
                handleClick(isPortAvailable);
            });
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
        String deviceIpAddress = "";
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
                            Log.d(Tags.BACNET, "device interface and ip" + iface.getDisplayName() + "-" + addr.getHostAddress());
                            deviceIpAddress = addr.getHostAddress();
                            if(iface.getName().startsWith("eth")){
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ipAddress.setText(deviceIpAddress);
    }
    private CustomSpinnerDropDownAdapter getAdapterValue(ArrayList values) {
        return new CustomSpinnerDropDownAdapter(this.requireContext(), R.layout.spinner_dropdown_item, values);
    }
}
