package a75f.io.renatus;

import static a75f.io.device.bacnet.BacnetConfigConstants.BACNET_CONFIGURATION;
import static a75f.io.device.bacnet.BacnetConfigConstants.IS_BACNET_INITIALIZED;
import static a75f.io.renatus.UtilityApplication.context;
import static a75f.io.renatus.UtilityApplication.startRestServer;
import static a75f.io.renatus.UtilityApplication.stopRestServer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import a75f.io.logic.L;
import a75f.io.renatus.util.CCUUiUtil;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ModbusConfigFragment extends Fragment {
    
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

    @BindView(R.id.toggleZoneToVirtualDeviceMapping) ToggleButton toggleZoneToVirtualDeviceMapping;

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

    SharedPreferences sharedPreferences;
    JSONObject config;
    JSONObject networkObject;
    JSONObject deviceObject;
    JSONObject objectConf;

    boolean isZoneToVirtualDeviceErrorShowing = false;

    public ModbusConfigFragment() {
    
    }
    
    public static ModbusConfigFragment newInstance() {
        return new ModbusConfigFragment();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
                                                                                                  Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_modbusconfig, container, false);
        ButterKnife.bind(this, rootView);
        
        return rootView;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        spinnerBaudRate.setSelection(((ArrayAdapter<String>)spinnerBaudRate.getAdapter())
                                         .getPosition(String.valueOf(readIntPref(PREF_MB_BAUD_RATE, 9600))));
        spinnerBaudRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                writeIntPref(PREF_MB_BAUD_RATE, Integer.parseInt(spinnerBaudRate.getSelectedItem().toString()));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    
        spinnerParity.setSelection(readIntPref(PREF_MB_PARITY, 0),false);
        spinnerParity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                writeIntPref(PREF_MB_PARITY, position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    
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
            throw new RuntimeException(e);
        }
    }

    private void setBACnetConfigurationValues() {
        try {
            tvIPDeviceObjectName.setText(deviceObject.getString("ipDeviceObjectName"));
            toggleZoneToVirtualDeviceMapping.setChecked(config.getBoolean("zoneToVirtualDeviceMapping"));
            description.setText(deviceObject.getString("description").equals("null") || deviceObject.getString("description").equals("")  ? "" : deviceObject.getString("description").trim());
            location.setText((deviceObject.getString("location").equals("null")) || (deviceObject.getString("location").equals("")) ? "" : deviceObject.getString("location").trim());
            password.setText((deviceObject.getString("password").equals("null")) || (deviceObject.getString("password").equals("")) ? "" : deviceObject.getString("password").trim());
            ipAddress.setText(networkObject.getString("ipAddress"));
            localNetworkNumber.setText(String.valueOf((networkObject.getString("localNetNum").equals("null")) || (networkObject.getString("localNetNum").equals("")) ? "" : networkObject.getInt("localNetNum")));
            virtualNetworkNumber.setText(String.valueOf((networkObject.getString("virtualNetNum").equals("null")) || (networkObject.getString("virtualNetNum").equals("")) ? "" : networkObject.getInt("virtualNetNum")));
            port.setText(String.valueOf(networkObject.getInt("port")));
            ipDeviceInstanceNumber.setText((deviceObject.getString("ipDeviceInstanceNum").equals("null")) || (deviceObject.getString("ipDeviceInstanceNum").equals("")) ? "" : String.valueOf(deviceObject.getInt("ipDeviceInstanceNum")));
            apduTimeout.setText((deviceObject.getString("apduTimeout").equals("null")) || (deviceObject.getString("apduTimeout").equals("")) ? "" :  String.valueOf(deviceObject.getInt("apduTimeout")));
            numberOfAPDURetries.setText((deviceObject.getString("numOfApduRetries").equals("null")) || (deviceObject.getString("numOfApduRetries").equals("")) ? "" :   String.valueOf(deviceObject.getInt("numOfApduRetries")));
            apduSegmentTimeout.setText((deviceObject.getString("apduSegmentTimeout").equals("null")) || (deviceObject.getString("apduSegmentTimeout").equals("")) ? "" : String.valueOf(deviceObject.getInt("apduSegmentTimeout")));
            etNotificationClassObjects.setText(String.valueOf((objectConf.getString("noOfNotificationClassObjects").equals("null")) || (objectConf.getString("noOfNotificationClassObjects").equals("")) ? "" : objectConf.getInt("noOfNotificationClassObjects")));
            etTrendLogObjects.setText(String.valueOf((objectConf.getString("noOfTrendLogObjects").equals("null")) || (objectConf.getString("noOfTrendLogObjects").equals("")) ? "" : objectConf.getInt("noOfTrendLogObjects")));
            etScheduleObjects.setText(String.valueOf((objectConf.getString("noOfScheduleObjects").equals("null")) || (objectConf.getString("noOfScheduleObjects").equals("")) ? "" : objectConf.getInt("noOfScheduleObjects")));
        }catch (JSONException e){
            Log.d(L.TAG_CCU_BACNET, "Exception while populating");
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
        }else{
            disableBACnet.setVisibility(View.GONE);
            initializeBACnet.setVisibility(View.VISIBLE);
            toggleZoneToVirtualDeviceMapping.setEnabled(true);
            setBACnetConfigurationValues();
        }


        initializeBACnet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                    startRestServer();
                    sharedPreferences.edit().putBoolean(IS_BACNET_INITIALIZED, true).apply();
                }
            }
        });

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
                stopRestServer();
                sharedPreferences.edit().putBoolean(IS_BACNET_INITIALIZED, false).apply();
            }
        });

        toggleZoneToVirtualDeviceMapping.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                try {
                    config.put("zoneToVirtualDeviceMapping", isChecked);
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
                    tvzvdmHint.setError("Enabled: Zone is represented as a virtual device and" +
                            " each endpoint is mapped to the respective " +
                            "BACnet Object.\n\n" +
                            "Disabled: System and Terminal profile points are exposed" +
                            " as a flat list.");
                    isZoneToVirtualDeviceErrorShowing = true;
                }else{
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

        if(ipAddress.getText().toString().equals("") || (!CCUUiUtil.isValidIPAddress(ipAddress.getText().toString().trim()))) return false;

        if(localNetworkNumber.getText().toString().equals("") || (!CCUUiUtil.isValidNumber(Integer.parseInt(localNetworkNumber.getText().toString()), 1, 65535, 1)))  return false;

        if(virtualNetworkNumber.getText().toString().equals("") || (!CCUUiUtil.isValidNumber(Integer.parseInt(virtualNetworkNumber.getText().toString()), 1, 65535, 1)))  return false;

        if(Integer.parseInt(virtualNetworkNumber.getText().toString()) == Integer.parseInt(localNetworkNumber.getText().toString()))  return false;

        if(port.getText().toString().equals("") || (!CCUUiUtil.isValidNumber(Integer.parseInt(port.getText().toString()), 4069, 65535, 1))) return false;

        if(ipDeviceInstanceNumber.getText().toString().equals("") || (!CCUUiUtil.isValidNumber(Integer.parseInt(ipDeviceInstanceNumber.getText().toString()), 0, 4194302, 1)))  return false;

        if(apduTimeout.getText().toString().equals("") || (!CCUUiUtil.isValidNumber(Integer.parseInt(apduTimeout.getText().toString()), 6000, 65535, 1000))) return false;

        if(numberOfAPDURetries.getText().toString().equals("") || (!CCUUiUtil.isValidNumber(Integer.parseInt(numberOfAPDURetries.getText().toString()), 0, 255, 1))) return false;

        if(apduSegmentTimeout.getText().toString().equals("") || (!CCUUiUtil.isValidNumber(Integer.parseInt(apduSegmentTimeout.getText().toString()), 5000, 65535, 1000)))  return false;

        if(etNotificationClassObjects.getText().toString().equals("") || (!CCUUiUtil.isValidNumber(Integer.parseInt(etNotificationClassObjects.getText().toString()), 0, 10, 1)))  return false;

        if(etTrendLogObjects.getText().toString().equals("") || (!CCUUiUtil.isValidNumber(Integer.parseInt(etTrendLogObjects.getText().toString()), 0, 10, 1)))  return false;

        return !etScheduleObjects.getText().toString().equals("") && (CCUUiUtil.isValidNumber(Integer.parseInt(etScheduleObjects.getText().toString()), 0, 10, 1));
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
                        Log.i("BACNET_CONFIG", "Key: description, Value: "+description.getText().toString());
                        String description1 = description.getText().toString();
                        deviceObject.put("description",description1);
                        sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    break;

                case R.id.etLocation:
                    try {
                        Log.i("BACNET_CONFIG", "Key: location, Value: "+location.getText().toString());
                        String location1 = location.getText().toString().trim();
                        deviceObject.put("location", location1);
                        sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    break;

                case R.id.etPassword:
                    try {
                        Log.i("BACNET_CONFIG", "Key: location, Value: "+password.getText().toString());
                        if (!CCUUiUtil.isAlphaNumeric(password.getText().toString())) {
                            ipAddress.setError(getString(R.string.error_password));
                        } else {
                            ipAddress.setError(null);
                            deviceObject.put("password", password.getText().toString().trim());
                            sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    break;

                case R.id.etIP:
                    try {
                        if (ipAddress.getText().toString().trim().length() > 0) {
                            Log.i("BACNET_CONFIG", "Key: ipAddress, Value: "+ipAddress.getText().toString()+", isValid: "+CCUUiUtil.isValidIPAddress(ipAddress.getText().toString()));
                            if (!CCUUiUtil.isValidIPAddress(ipAddress.getText().toString())) {
                                ipAddress.setError(getString(R.string.error_ip_address));
                                initializeBACnet.setEnabled(false);
                                initializeBACnet.setClickable(false);
                                initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
                            } else {
                                ipAddress.setError(null);
                                networkObject.put("ipAddress",ipAddress.getText().toString().trim());
                                sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                                if(validateEntries()){
                                    initializeBACnet.setEnabled(true);
                                    initializeBACnet.setClickable(true);
                                    initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.ctaOrange));
                                }else{
                                    initializeBACnet.setEnabled(false);
                                    initializeBACnet.setClickable(false);
                                    initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.tuner_group));
                                }
                            }
                        }else{
                            ipAddress.setError(null);
                            networkObject.put("ipAddress","");
                            sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply();
                            if(validateEntries()){
                                initializeBACnet.setEnabled(true);
                                initializeBACnet.setClickable(true);
                                initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.ctaOrange));
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
                    validateAndSetValue(localNetworkNumber, "localNetNum", localNetworkNumber.getText().toString(), networkObject, 1,65535, 1, getString(R.string.error_local_network_number));
                    break;

                case R.id.etVirtualNetworkAddress:
                    validateAndSetValue(virtualNetworkNumber, "virtualNetNum", virtualNetworkNumber.getText().toString(), networkObject, 1,65535, 1, getString(R.string.error_virtual_network_number));
                    break;

                case R.id.etPort:
                    validateAndSetValue(port, "port", port.getText().toString(), networkObject, 4096,65535, 1, getString(R.string.txt_error_port));
                    break;

                case R.id.etIPDeviceInstanceNumber:
                    validateAndSetValue(ipDeviceInstanceNumber, "ipDeviceInstanceNum", ipDeviceInstanceNumber.getText().toString(), deviceObject, 0,4194302, 1, getString(R.string.error_ip_device_instance_number));
                    break;

                case R.id.etAPDU_Timeout:
                    validateAndSetValue(apduTimeout, "apduTimeout", apduTimeout.getText().toString(), deviceObject, 6000,65535, 1000, getString(R.string.error_apdu_timeout));
                    break;

                case R.id.etNumberofAPDURetries:
                    validateAndSetValue(numberOfAPDURetries, "numOfApduRetries", numberOfAPDURetries.getText().toString(), deviceObject, 0,255,1, getString(R.string.error_apdu_retries));
                    break;

                case R.id.etAPDUSegmentTimeout:
                    validateAndSetValue(apduSegmentTimeout, "apduSegmentTimeout", apduSegmentTimeout.getText().toString(), deviceObject, 5000,65535, 1000, getString(R.string.error_apdu_segment_timeout));
                    break;

                case R.id.etNotificationClassObjects:
                    validateAndSetValue(etNotificationClassObjects, "noOfNotificationClassObjects", etNotificationClassObjects.getText().toString(), objectConf, 0, 10, 1, getString(R.string.error_notification_class_objects));
                    break;

                case R.id.etTrendLogObjects:
                    validateAndSetValue(etTrendLogObjects, "noOfTrendLogObjects", etTrendLogObjects.getText().toString(), objectConf, 0, 10, 1, getString(R.string.error_trend_log_objects));
                    break;

                case R.id.etScheduleObjects:
                    validateAndSetValue(etScheduleObjects, "noOfScheduleObjects", etScheduleObjects.getText().toString(), objectConf, 0, 10, 1, getString(R.string.error_schedule_objects));
                    break;
            }
        }

        private void validateAndSetValue(EditText view, String key, String value, JSONObject jsonObject, int min, int max,  int multiple, String error) {
            try {
                if (value.trim().length() > 0) {
                    Log.i("BACNET_CONFIG", "Key: "+key+", Value: "+value+", isValid: "+CCUUiUtil.isValidNumber(Integer.parseInt(value), min, max, multiple));
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
                            initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.ctaOrange));
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
                        initializeBACnet.setTextColor(ContextCompat.getColor(context, R.color.ctaOrange));
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
}
