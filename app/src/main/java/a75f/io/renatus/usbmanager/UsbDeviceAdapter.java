package a75f.io.renatus.usbmanager;

import static a75f.io.logic.L.TAG_CCU_BACNET_MSTP;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_MSTP_CONFIGURATION;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_MSTP_SERIAL_DEVICE;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_START;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_INITIALIZED;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_MSTP_INITIALIZED;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_BAUD_RATE;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_DEVICE_ID;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_MAX_FRAME;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_MAX_MASTER;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.PREF_MSTP_SOURCE_ADDRESS;
import static a75f.io.logic.util.bacnet.BacnetUtilKt.cancelScheduleJobToResubscribeBacnetMstpCOV;
import static a75f.io.renatus.UtilityApplication.startRestServer;
import static a75f.io.renatus.UtilityApplication.stopRestServer;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.BAUD_RATE;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.DATA_BITS;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.DEVICE_ID;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.MAX_FRAMES;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.MAX_MASTER;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.MSTP_STATUS_DISABLE;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.MSTP_STATUS_INITIALIZE;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.PARITY;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.PROTOCOL_BACNET_MSTP;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.PROTOCOL_BISKIT;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.PROTOCOL_MODBUS;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.PROTOCOL_NA;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.PROTOCOL_NOT_ASSIGNED;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.SOURCE_ADDRESS;
import static a75f.io.renatus.usbmanager.UsbManagerConstants.STOP_BITS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import a75f.io.domain.api.Ccu;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.util.bacnet.BacnetUtilKt;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.DataMstp;
import a75f.io.renatus.util.DataMstpObj;
import a75f.io.renatus.util.UsbHelper;
import a75f.io.restserver.server.HttpServer;
import a75f.io.usbserial.BacnetConfig;
import a75f.io.usbserial.ModbusConfig;
import a75f.io.usbserial.UsbDeviceItem;
import a75f.io.usbserial.UsbModbusService;
import a75f.io.usbserial.UsbPortTrigger;
import a75f.io.usbserial.UsbPrefHelper;
import a75f.io.usbserial.UsbSerialUtil;
import a75f.io.util.DashboardUtilKt;
import a75f.io.util.ExecutorTask;

public class UsbDeviceAdapter extends RecyclerView.Adapter<UsbDeviceAdapter.UsbViewHolder> {

    private List<UsbDeviceItem> devices;
    private final Context context;
    private int expandedPosition = -1;
    private UsbManagerViewModel viewModel;

    private List<String> cmDevices;

    public UsbDeviceAdapter(Context context, List<UsbDeviceItem> devices, List<String> cmConnectedDevices, UsbManagerViewModel viewModel) {
        this.context = context;
        this.devices = devices;
        this.cmDevices = cmConnectedDevices;
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public UsbViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.usb_device_row, parent, false);
        return new UsbViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull UsbViewHolder holder, @SuppressLint("RecyclerView") int position) {
        UsbDeviceItem device = devices.get(position);

        String serial = device.getSerial();
        String name = device.getName();

        if (name != null && !name.isEmpty()) {
            int lastSpace = name.lastIndexOf(' ');
            if (lastSpace != -1) {
                name = name.substring(0, lastSpace);
            }
        } else {
            name = "";
        }

        holder.tvSerial.setText(serial + (name.isEmpty() ? "" : " (" + name + ")"));
        holder.tvVendor.setText(device.getVendor() + " | " + device.getProductId());
        holder.tvPort.setText(device.getPort());

        if (device.getProtocol().equals("NA")) {
            holder.protocol_optionsNA.setVisibility(View.VISIBLE);
            holder.spinner.setVisibility(View.GONE);
        } else {
            ArrayAdapter<CharSequence> adapter = getProtocolArrayAdapter(device);
            holder.spinner.setAdapter(adapter);

            // Set spinner value
            int spinnerPosition = adapter.getPosition(device.getProtocol());
            if (spinnerPosition >= 0) {
                holder.spinner.setSelection(spinnerPosition);
            }

            holder.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                boolean firstCall = true;
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    if (firstCall) { firstCall = false; return; }
                    String selected = parent.getItemAtPosition(pos).toString();
                    //MSTP protocol changed
                    if (device.getProtocol().equals(PROTOCOL_BACNET_MSTP) && !selected.equals(PROTOCOL_BACNET_MSTP)) {
                        CcuLog.i(TAG_CCU_BACNET_MSTP, "UsbDevice protocol changed, disabling MSTP session : "+selected);
                        disableMstpSession();
                    } else if (selected.equals(PROTOCOL_BACNET_MSTP)) {
                        //Serial device changed, but an old active mstp config exists
                        String currentMstpDevice = PreferenceManager.getDefaultSharedPreferences(context).getString(BACNET_MSTP_SERIAL_DEVICE, "");
                        if (!device.getSerial().equals(currentMstpDevice)) {
                            disableMstpSession();
                            CcuLog.i(TAG_CCU_BACNET_MSTP, "Serial device changed, resetting Bacnet MSTP config "+currentMstpDevice+" -> "+device.getSerial());
                        }
                    }
                    device.setProtocol(selected);
                    //viewModel.updateDeviceProtocol(device.getSerial(), selected);

                    String updatedPort = getPortName(device);
                    device.setPort(updatedPort);
                    holder.tvPort.setText(updatedPort);

                    if (selected.equals(PROTOCOL_BACNET_MSTP) || selected.equals(PROTOCOL_MODBUS)) {
                        if (expandedPosition != position) {
                            int previousExpanded = expandedPosition;
                            expandedPosition = position;
                            if (previousExpanded != -1) {
                                notifyItemChanged(previousExpanded);
                            }
                            notifyItemChanged(position);
                        }
                    } else {
                        if (expandedPosition == position) {
                            expandedPosition = -1;
                            notifyItemChanged(position);
                        }
                    }

                    updateDetails((LinearLayout) holder.expandableLayout, selected, device); // Update expanded content
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        holder.tvPort.setText(getPortName(device));
        device.setPort(getPortName(device));
        boolean isExpanded = position == expandedPosition;
        holder.expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        if (isExpanded) {
            holder.downArrow.setVisibility(View.VISIBLE);
            holder.rightArrow.setVisibility(View.GONE);
        } else {
            holder.downArrow.setVisibility(View.GONE);
            holder.rightArrow.setVisibility(View.VISIBLE);
        }
        int bgColor = (position % 2 == 0)
                ? ContextCompat.getColor(context, android.R.color.white)
                : Color.parseColor("#FAFAFA");
        holder.layoutMainRow.setBackgroundColor(bgColor);

        holder.itemView.setOnClickListener(v -> {
            if (expandedPosition == position) {
                expandedPosition = -1;
            } else {
                expandedPosition = position;
            }
            notifyDataSetChanged();
        });

        // Set expanded details text on bind too
        updateDetails((LinearLayout) holder.expandableLayout, device.getProtocol(), device);
    }

    public void updateDevices(List<UsbDeviceItem> newDevices, List<String> cmConnectedDevices) {
        this.devices = newDevices;
        this.cmDevices = cmConnectedDevices;
        notifyDataSetChanged();
    }
    @NonNull
    private ArrayAdapter<CharSequence> getProtocolArrayAdapter(UsbDeviceItem device) {

        String[] entries;
        if(UsbSerialUtil.isBiskitModeEnabled(context)) {
            entries = context.getResources().getStringArray(R.array.usb_protocol_options_dev);
        } else {
            entries = context.getResources().getStringArray(R.array.usb_protocol_options);
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                context,
                R.layout.spinner_usb_protocol_item,
                entries
        ) {
            @Override
            public boolean isEnabled(int position) {
                String value = getItem(position).toString();
                return isProtocolAvailable(value);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                String value = getItem(position).toString();

                if (!isProtocolAvailable(value)) {
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }

            private boolean isProtocolAvailable(String protocol) {
                boolean cmConnected = devices.stream().anyMatch(d -> d.getSerial().equals("75FCM"));
                if ("BACnet MSTP".equals(protocol) &&
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || isProtocolTaken("BACnet MSTP", 1, device))) {
                    return false;
                } else if ("Modbus".equals(protocol) && isProtocolTaken("Modbus", 2, device)) {
                    return false;
                } else if ("Biskit".equals(protocol) && (cmConnected || isProtocolTaken("Biskit", 1, device))) {
                    return false;
                }
                return true;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }
    private boolean isProtocolTaken(String protocol, int maxAllowed, UsbDeviceItem currentDevice) {
        int count = 0;
        for (UsbDeviceItem d : devices) {
            if (!d.equals(currentDevice) && protocol.equals(d.getProtocol())) {
                count++;
            }
        }
        CcuLog.i("CCU_UI"," isProtocolTaken "+protocol+ ": count "+count+" maxAllowed "+maxAllowed);
        if (maxAllowed == 0 ) {
            return true;
        } else {
            return count >= maxAllowed;
        }
    }


    @Override
    public int getItemCount() {
        return devices.size();
    }

    private void updateDetails(LinearLayout parentLayout, String protocol, UsbDeviceItem device) {
        CcuLog.i(L.TAG_CCU_UI, "Update details for protocol "+protocol+" device "+device.getSerial());
        parentLayout.removeAllViews();
        switch (protocol) {
            case PROTOCOL_NA:
                addTitle(parentLayout, "CM Connected Devices", true);
                if (cmDevices.isEmpty()) {
                    addTitle(parentLayout, "No devices connected", false);
                } else {
                    addHorizontalStackedRow(context, parentLayout, "Connect Module", cmDevices);
                }
                break;
            case PROTOCOL_MODBUS:
                createModbusViews(parentLayout, device);
                break;
            case PROTOCOL_BACNET_MSTP:
                createBacnetMstpView(parentLayout, device);
                break;
            case PROTOCOL_BISKIT:
                break;
            default:
                addTitle(parentLayout, "Please select a protocol to proceed with configuration", false);
                break;
        }
    }

    private void createModbusViews(LinearLayout parentLayout, UsbDeviceItem device) {
        addTitle(parentLayout, "Modbus Configuration", true);
        ModbusConfig modbusConfig = device.getModbusConfig();
        if (modbusConfig != null) {
            addTextViewSpinnerRow(context, device, parentLayout, "Baud Rate", R.array.mb_config_baudrate_array,
                    Arrays.asList(context.getResources().getStringArray(R.array.mb_config_baudrate_array)).indexOf(String.valueOf(modbusConfig.getBaudRate())));
            addTextViewSpinnerRow(context, device, parentLayout, "Parity", R.array.mb_config_parity_array, modbusConfig.getParity());
            addTextViewSpinnerRow(context, device, parentLayout, "Data Bits", R.array.mb_config_databits_array,
                    Arrays.asList(context.getResources().getStringArray(R.array.mb_config_databits_array)).indexOf(String.valueOf(modbusConfig.getDataBits())));
            addTextViewSpinnerRow(context, device, parentLayout, "Stop Bits", R.array.mb_config_stopbits_array,
                    Arrays.asList(context.getResources().getStringArray(R.array.mb_config_stopbits_array)).indexOf(String.valueOf(modbusConfig.getStopBits())));
        } else {
            device.setModbusConfig(new ModbusConfig(device.getSerial(), 0,0,0,0));
            addTextViewSpinnerRow(context, device, parentLayout, "Baud Rate", R.array.mb_config_baudrate_array, 0);
            addTextViewSpinnerRow(context, device, parentLayout, "Parity", R.array.mb_config_parity_array, 0);
            addTextViewSpinnerRow(context, device, parentLayout, "Data Bits", R.array.mb_config_databits_array, 1);
            addTextViewSpinnerRow(context, device, parentLayout, "Stop Bits", R.array.mb_config_stopbits_array, 0);
        }
    }

    private void createBacnetMstpView(LinearLayout parentLayout, UsbDeviceItem device) {
        addTitle(parentLayout, "BACnet MSTP Configuration", true);
        BacnetConfig bacnetConfig = device.getBacnetConfig();
        EditText srcAddress, maxMaster, maxFrames, deviceId;
        Spinner baudRateSpinner;
        if (bacnetConfig != null) {
            baudRateSpinner = addTextViewSpinnerRow(context, parentLayout, "Baud Rate", R.array.mb_config_baudrate_array,
                    Arrays.asList(context.getResources().getStringArray(R.array.mb_config_baudrate_array)).indexOf(String.valueOf(bacnetConfig.getBaudRate())));
            baudRateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    device.getBacnetConfig().setBaudRate(Integer.parseInt(context.getResources().getStringArray(R.array.mb_config_baudrate_array)[pos]));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });;
            srcAddress = addTextViewEditTextRow(context, device, parentLayout, "Source Address", "In the range of 0 to 127", bacnetConfig.getSourceAddress());
            maxMaster = addTextViewEditTextRow(context, device, parentLayout, "Max Master", "In the range of 1 to 127", bacnetConfig.getMaxMaster());
            maxFrames = addTextViewEditTextRow(context, device, parentLayout, "Max Frames", "In the range of 1 to 255", bacnetConfig.getMaxFrames());
            deviceId = addTextViewEditTextRow(context, device, parentLayout, "Device Id", "In the range of 1 to 4194303", bacnetConfig.getDeviceId());
        } else {
            device.setBacnetConfig(new BacnetConfig(device.getSerial(), 0,1,127,1, 1));
            baudRateSpinner = addTextViewSpinnerRow(context, parentLayout, "Baud Rate", R.array.mb_config_baudrate_array, 0);
            baudRateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    device.getBacnetConfig().setBaudRate(Integer.parseInt(context.getResources().getStringArray(R.array.mb_config_baudrate_array)[pos]));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            srcAddress = addTextViewEditTextRow(context, device, parentLayout, "Source Address", "In the range of 0 to 127", 1);
            maxMaster = addTextViewEditTextRow(context, device, parentLayout, "Max Master", "In the range of 1 to 127", 127);
            maxFrames = addTextViewEditTextRow(context, device, parentLayout, "Max Frames", "In the range of 1 to 255", 1);
            deviceId = addTextViewEditTextRow(context, device, parentLayout, "Device Id", "In the range of 1 to 4194303", 1);
        }
        configureInitButton(parentLayout, device, baudRateSpinner, srcAddress, maxMaster, maxFrames, deviceId);
    }

    private void configureInitButton(LinearLayout parentLayout, UsbDeviceItem device, Spinner baudRateSpinner,
                                     EditText srcAddress, EditText maxMaster, EditText maxFrames, EditText deviceId) {
        boolean mstpInitStatus = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(IS_BACNET_MSTP_INITIALIZED, false);
        Button initButton = addButton(context, parentLayout, mstpInitStatus ? MSTP_STATUS_DISABLE: MSTP_STATUS_INITIALIZE);

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        initButton.setBackgroundResource(outValue.resourceId);
        enableMstpUI(baudRateSpinner, srcAddress, maxMaster, maxFrames, deviceId, !mstpInitStatus);

        initButton.setOnClickListener(view -> {
            if (initButton.getText().equals(MSTP_STATUS_INITIALIZE)) {
                CcuLog.i(L.TAG_CCU_UI, "Initialize MSTP");
                if (isMstpConfigValid(device)) {
                    initButton.setAlpha(0.5f);
                    initButton.setEnabled(false);

                    List<UsbDeviceItem> deviceConfigs = UsbPrefHelper.getUsbDeviceList(context);
                    UsbDeviceItem currentConfig = deviceConfigs.stream()
                                                    .filter(d -> d.getSerial().equals(device.getSerial()))
                                                    .findFirst().orElse(null);
                    if (currentConfig != null && currentConfig.getProtocol().equals(PROTOCOL_MODBUS)) {
                        Intent intent = new Intent(context, UsbModbusService.class);
                        boolean stopStatus = context.stopService(intent);
                        CcuLog.i(L.TAG_CCU_UI, "Protocol changed from Modbus to MSTP, stopping Modbus service: "+stopStatus);
                    }
                    prepareMstpInit(device);
                    initButton.setText(MSTP_STATUS_DISABLE);
                    enableMstpUI(baudRateSpinner, srcAddress, maxMaster, maxFrames, deviceId, false);
                    UsbPrefHelper.saveUsbDeviceList(context, devices);
                    initButton.postDelayed(() -> {
                        initButton.setEnabled(true);
                        initButton.setAlpha(1f);
                    }, 2000);
                } else {
                    Toast.makeText(context, "Invalid MSTP Configuration", Toast.LENGTH_LONG).show();
                }
            } else if (initButton.getText().equals(MSTP_STATUS_DISABLE)) {
                CcuLog.i(L.TAG_CCU_UI, "Disable MSTP");
                initButton.setAlpha(0.5f);
                initButton.setEnabled(false);
                disableMstpSession();
                initButton.setText(MSTP_STATUS_INITIALIZE);
                enableMstpUI(baudRateSpinner, srcAddress, maxMaster, maxFrames, deviceId, true);
                initButton.postDelayed(() -> {
                    initButton.setEnabled(true);
                    initButton.setAlpha(1f);
                }, 2000);
            }
        });
    }

    private void enableMstpUI(Spinner baudRateSpinner, EditText srcAddress, EditText maxMaster, EditText maxFrames,
                              EditText deviceId, boolean enabled) {
        baudRateSpinner.setEnabled(enabled);
        srcAddress.setEnabled(enabled);
        maxMaster.setEnabled(enabled);
        maxFrames.setEnabled(enabled);
        deviceId.setEnabled(enabled);
    }

    private void disableMstpSession() {
        ExecutorTask.executeBackground(() -> {
            RenatusApp.backgroundServiceInitiator.initServices();
            boolean isBacnetIpInitialized = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(IS_BACNET_INITIALIZED, false);
            if (!DashboardUtilKt.isDashboardConfig(context) && !isBacnetIpInitialized) {
                stopRestServer();
            }
            cancelScheduleJobToResubscribeBacnetMstpCOV("BACnet Mstp was disabled, cancelling COV subscription");
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit().putBoolean(IS_BACNET_MSTP_INITIALIZED, false).apply();
            Intent intent = new Intent("MSTP_STOP");
            context.sendBroadcast(intent);
        });
    }
    private void addTitle(LinearLayout parentLayout, String titleText, boolean boldText) {
        TextView titleView = new TextView(context);
        titleView.setText(titleText);
        titleView.setTextSize(20);
        if (boldText) {
            titleView.setTypeface(null, Typeface.BOLD);
        } else {
            titleView.setTypeface(null, Typeface.NORMAL);
        }
        titleView.setTextColor(Color.BLACK);
        titleView.setPadding(40, 6, 0, 6);

        Typeface latoBold = ResourcesCompat.getFont(context, R.font.lato_bold);
        titleView.setTypeface(latoBold);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleView.setLayoutParams(params);
        parentLayout.addView(titleView);
    }

    private void addTextViewSpinnerRow(Context context, UsbDeviceItem device, LinearLayout parentLayout, String labelText, int arrayResId, int selectedIndex) {
        addTextViewSpinnerRow(context, parentLayout, labelText, arrayResId, selectedIndex)
                .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                switch (labelText) {
                    case BAUD_RATE:
                        device.getModbusConfig().setBaudRate(Integer.parseInt(context.getResources().getStringArray(R.array.mb_config_baudrate_array)[pos]));
                        break;
                    case PARITY:
                        device.getModbusConfig().setParity(pos);
                        break;
                    case DATA_BITS:
                        CcuLog.i(L.TAG_CCU_UI, "Data bits selected pos "+pos+" val "+context.getResources().getStringArray(R.array.mb_config_databits_array)[pos]);
                        device.getModbusConfig().setDataBits(Integer.parseInt(context.getResources().getStringArray(R.array.mb_config_databits_array)[pos]));
                        break;
                    case STOP_BITS:
                        device.getModbusConfig().setStopBits(Integer.parseInt(context.getResources().getStringArray(R.array.mb_config_stopbits_array)[pos]));
                        break;
                }
                CcuLog.i(L.TAG_CCU_UI, "Save selection "+labelText+" Pos "+pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private Spinner addTextViewSpinnerRow(Context context, LinearLayout parentLayout, String labelText, int arrayResId, int selectedIndex) {
        // Create a horizontal LinearLayout
        LinearLayout rowLayout = new LinearLayout(context);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        rowLayout.setPadding(40, 4, 0, 4);

        int labelWidth = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 200, context.getResources().getDisplayMetrics());

        int spinnerWidth = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 120, context.getResources().getDisplayMetrics());


        // Create TextView
        TextView textView = new TextView(context);
        textView.setText(labelText);
        textView.setTextSize(19);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                labelWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        Typeface latoBold = ResourcesCompat.getFont(context, R.font.lato_bold);
        textView.setTypeface(latoBold);

        // Create Spinner
        Spinner spinner = new Spinner(context);

        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                spinnerWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        spinnerParams.setMarginStart(16); // Optional margin
        spinner.setLayoutParams(spinnerParams);


        // Set adapter from array resource
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, arrayResId, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        CcuLog.i(L.TAG_CCU_UI, "Spinner "+labelText+" selectedIndex "+selectedIndex+" count "+adapter.getCount());
        if (selectedIndex == -1 && labelText.equals(DATA_BITS)) selectedIndex = 1;
        if (selectedIndex >= 0 && selectedIndex < adapter.getCount()) {
            spinner.setSelection(selectedIndex);
        }

        // Add TextView and Spinner to row
        rowLayout.addView(textView);
        rowLayout.addView(spinner);

        // Add row to parent layout
        parentLayout.addView(rowLayout);
        return spinner;
    }

    private EditText addTextViewEditTextRow(Context context, UsbDeviceItem device, LinearLayout parentLayout,
                                            String title, String description, int currentVal) {
        EditText editText = addTextViewEditTextRow(context, parentLayout, title, description, currentVal);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Only if you need live updates
            }
            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().trim();
                if (!value.isEmpty()) {
                    int newValue = Integer.parseInt(value);
                    switch (title) {
                        case SOURCE_ADDRESS:
                            device.getBacnetConfig().setSourceAddress(newValue);
                            break;
                        case MAX_MASTER:
                            device.getBacnetConfig().setMaxMaster(newValue);
                            break;
                        case MAX_FRAMES:
                            device.getBacnetConfig().setMaxFrames(newValue);
                            break;
                        case DEVICE_ID:
                            device.getBacnetConfig().setDeviceId(newValue);
                            break;
                    }

                    CcuLog.i(L.TAG_CCU_UI, "Save config "+title+" Val "+newValue);
                }

            }
        });
        return editText;
    }

    private EditText addTextViewEditTextRow(Context context, LinearLayout parentLayout,
                                       String title, String description, int currentVal) {
        // Container for the row
        LinearLayout rowLayout = new LinearLayout(context);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setPadding(40, 6, 0, 6);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Left side: Text container
        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(200,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        textContainer.setLayoutParams(textParams);

        Typeface latoBold = ResourcesCompat.getFont(context, R.font.lato_bold);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextSize(19);
        tvTitle.setTypeface(latoBold);

        TextView tvDescription = new TextView(context);
        tvDescription.setText(description);
        tvDescription.setTextSize(16);
        tvDescription.setTextColor(Color.GRAY);
        tvDescription.setTypeface(latoBold);
        
        textContainer.addView(tvTitle);
        textContainer.addView(tvDescription);

        // Right side: EditText
        EditText editText = new EditText(context);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setSingleLine(true);
        editText.setText(String.valueOf(currentVal));
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                return true; // Consume Enter to stop focus jump
            }
            return false;
        });


        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(160,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        editParams.setMarginStart(16);
        editText.setLayoutParams(editParams);

        // Combine
        rowLayout.addView(textContainer);
        rowLayout.addView(editText);

        // Add to parent layout
        parentLayout.addView(rowLayout);
        return editText;
    }

    private void addHorizontalStackedRow(Context context, LinearLayout parentLayout,
                                         String title, List<String> subtexts) {
        LinearLayout rowLayout = new LinearLayout(context);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Add margin to the left (e.g. 24px)
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(24, 0, 0, 0); // left, top, right, bottom
        rowLayout.setLayoutParams(rowParams);

        for (int i = 0; i < subtexts.size(); i++) {
            // Container for title + subtext (vertical)
            LinearLayout textContainer = new LinearLayout(context);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            textContainer.setPadding(16, 8, 16, 8);

            TextView tvTitle = new TextView(context);
            tvTitle.setText(title);
            tvTitle.setTextSize(18);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);

            TextView tvSubtext = new TextView(context);
            tvSubtext.setText(subtexts.get(i));
            tvSubtext.setTextSize(16);
            tvSubtext.setTextColor(Color.GRAY);

            textContainer.addView(tvTitle);
            textContainer.addView(tvSubtext);

            rowLayout.addView(textContainer);

            // Add vertical line separator between cells
            if (i < subtexts.size() - 1) {
                View separator = new View(context);
                LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                        1, // line thickness
                        ViewGroup.LayoutParams.MATCH_PARENT // span full height of the row
                );
                sepParams.setMargins(8, 0, 8, 0);
                separator.setLayoutParams(sepParams);
                separator.setBackgroundColor(Color.GRAY);
                rowLayout.addView(separator);
            }
        }

        parentLayout.addView(rowLayout);
    }

    public Button addButton(Context context, LinearLayout parentLayout, String buttonText) {
        Button button = new Button(context);
        button.setText(buttonText);
        button.setAllCaps(false);

        // Transparent background
        button.setBackground(null);
        // Ensure it still has some padding so text is not clipped
        button.setPadding(20, 10, 20, 10);

        int endMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics());
        // Layout params
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        //params.setMarginStart(800);
        params.gravity = Gravity.END;
        params.setMarginEnd(endMargin);
        button.setLayoutParams(params);
        button.setTextSize(18);
        button.setTextColor(CCUUiUtil.getPrimaryColor());
        Typeface latoBold = ResourcesCompat.getFont(context, R.font.lato_bold);
        button.setTypeface(latoBold);

        // Add to parent
        parentLayout.addView(button);
        return button;
    }

    private boolean isMstpConfigValid(UsbDeviceItem device) {
        BacnetConfig mstpConfig = device.getBacnetConfig();
        if (!CCUUiUtil.isValidNumber(mstpConfig.getSourceAddress(), 1, 127, 1)) {
            return false;
        }

        if (!CCUUiUtil.isValidNumber(mstpConfig.getMaxMaster(), 1, 127, 1)) {
            return false;
        }
        if (!CCUUiUtil.isValidNumber(mstpConfig.getMaxFrames(), 1, 255, 1)) {
            return false;
        }
        return CCUUiUtil.isValidNumber(mstpConfig.getDeviceId(), 1, 4194302, 1);
    }

    private void prepareMstpInit(UsbDeviceItem device) {
        CcuLog.i(L.TAG_CCU_UI, "prepareMstpInit");
        ExecutorTask.executeBackground(() -> {
            //RenatusApp.backgroundServiceInitiator.unbindServices();
            //CcuLog.d(TAG_CCU_BACNET_MSTP, "--step 1--unbind modbus service--");

            //Intent intent = new Intent(context, UsbModbusService.class);
            //context.stopService(intent);
            //CcuLog.d(TAG_CCU_BACNET_MSTP, "--step 2--stop modbus service--");

            //CcuLog.d(TAG_CCU_BACNET_MSTP, "--step 3--apply permissions--");

            UsbHelper.runChmodUsbDevices();
            UsbPortTrigger.triggerUsbSerialBinding(context);
            UsbHelper.listUsbDevices(context);
            UsbHelper.runAsRoot("ls /dev/tty*");

            CcuLog.d(TAG_CCU_BACNET_MSTP, "--step 4--update ui--");
            /*updateMstpUi();

            if(!isUSBSerialPortAvailable){
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Check USB connection, MSTP can not initialized.", Toast.LENGTH_LONG).show();
                });
                CcuLog.d(TAG_CCU_BACNET_MSTP, "--step 4--update ui Check USB connection, mstp can not initialized.--");
                return;
            }*/

            BacnetConfig bacnetConfig = device.getBacnetConfig();
            writeIntPref(PREF_MSTP_BAUD_RATE, bacnetConfig.getBaudRate());
            writeIntPref(PREF_MSTP_SOURCE_ADDRESS, bacnetConfig.getSourceAddress());
            writeIntPref(PREF_MSTP_MAX_MASTER,bacnetConfig.getMaxMaster());
            writeIntPref(PREF_MSTP_MAX_FRAME, bacnetConfig.getMaxFrames());
            writeIntPref(PREF_MSTP_DEVICE_ID, bacnetConfig.getDeviceId());

            if (!HttpServer.Companion.getInstance(context).isServerRunning()) {
                startRestServer();
            }

            DataMstpObj dataMstpObj = new DataMstpObj();
            dataMstpObj.setDataMstp(new DataMstp(
                    bacnetConfig.getBaudRate(),
                    bacnetConfig.getSourceAddress(),
                    bacnetConfig.getMaxMaster(),
                    bacnetConfig.getMaxFrames(),
                    bacnetConfig.getDeviceId(),
                    device.getSerial()
            ));

            String jsonString = new Gson().toJson(dataMstpObj);
            CcuLog.d(TAG_CCU_BACNET_MSTP, "--step 5--create mstp init json--"+jsonString);
            PreferenceManager.getDefaultSharedPreferences(context)
                            .edit()
                            .putString(BACNET_MSTP_CONFIGURATION, jsonString).apply();
            PreferenceManager.getDefaultSharedPreferences(context)
                            .edit()
                            .putBoolean(IS_BACNET_MSTP_INITIALIZED, true).apply();
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString(BACNET_MSTP_SERIAL_DEVICE, device.getSerial()).apply();

            BacnetUtilKt.launchBacApp(context, BROADCAST_BACNET_APP_START, "Start BACnet App", "",true);
            CcuLog.d(TAG_CCU_BACNET_MSTP, "MSTP configuration initialized");
            //RenatusApp.backgroundServiceInitiator.initServices();
            //CcuLog.d(TAG_CCU_BACNET_MSTP, "--step 6--init services--");

        });
    }



    public void writeIntPref(String key, int val) {
        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = spDefaultPrefs.edit();
        editor.putInt(key, val);
        editor.apply();
    }
    static class UsbViewHolder extends RecyclerView.ViewHolder {
        TextView tvSerial, tvVendor, tvPort, protocol_optionsNA;
        Spinner spinner;
        View expandableLayout;
        LinearLayout layoutMainRow;
        ImageView rightArrow, downArrow;

        public UsbViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSerial = itemView.findViewById(R.id.tv_serial);
            tvVendor = itemView.findViewById(R.id.tv_vendor);
            tvPort = itemView.findViewById(R.id.tv_port);
            protocol_optionsNA = itemView.findViewById(R.id.protocol_optionsNA);
            spinner = itemView.findViewById(R.id.protocol_options);
            expandableLayout = itemView.findViewById(R.id.expandable_layout);
            layoutMainRow = itemView.findViewById(R.id.layout_main_row);
            rightArrow = itemView.findViewById(R.id.right_arrow);
            downArrow = itemView.findViewById(R.id.down_arrow);
        }
    }

    private String getPortName(UsbDeviceItem currentDevice) {
        if (Objects.equals(currentDevice.getProtocol(), PROTOCOL_NOT_ASSIGNED)
                    || Objects.equals(currentDevice.getProtocol(), PROTOCOL_NA)) {
            return PROTOCOL_NA;
        }
        if (!currentDevice.getPort().equals(PROTOCOL_NA)) {
            return currentDevice.getPort();
        }
        devices.forEach(d -> CcuLog.i(L.TAG_CCU_UI, d+"Existing device port "+d.getPort()+" proto "+d.getProtocol()));
        for (int i =1 ; i <= devices.size(); i++) {
            String portName = "COM "+i;
            boolean portTaken = devices.stream()
                    .anyMatch(device -> device.getPort().equals(portName));
            if (!portTaken) {
                return portName;
            }
        }
        return PROTOCOL_NA;
    }

}



