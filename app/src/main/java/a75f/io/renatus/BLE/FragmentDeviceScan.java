package a75f.io.renatus.BLE;

import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.ARG_NAME;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.ARG_PAIRING_ADDR;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.FLOOR_NAME;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.NODE_TYPE;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

import a75f.io.device.serial.SerialConsts;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.AlternatePairingFragment;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.R;
import a75f.io.util.ExecutorTask;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ryanmattison isOn 7/24/17.
 */
public class FragmentDeviceScan extends BaseDialogFragment
{

    public static final  String ID                = "scan_dialog";
    static final         int    REQUEST_ENABLE_BT = 1;
    private static final String PROFILE_TYPE      = "profile_type";
    @BindView(R.id.ble_device_list_list_view)
    ListView mBLEDeviceListListView;
    String              mName;
    String              mFloorName;
    Short               mPairingAddress;
    ProfileType         mProfileType;
    LeDeviceListAdapter mLeDeviceListAdapter;
    BluetoothAdapter    mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    boolean mScanning;
    BluetoothStateReceiver bluetoothStateReceiver;
    @BindView(R.id.connect_manually)
    TextView connectManuallyInAvailableDevice;
    @BindView(R.id.connect_manually_no_device)
    TextView connectManuallyNoDevice;
    @BindView(R.id.pairManuallyText)
    TextView pairManuallyTextNoDevice;
    @BindView(R.id.pairManuallyText1)
    TextView pairManuallyTextInAvailableList;
    @BindView(R.id.available_devices_layout)
    LinearLayout availableDevicesLayout;
    @BindView(R.id.no_devices_layout)
    LinearLayout noDevicesLayout;

    private ScanCallback mScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if(getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    BluetoothDevice device = result.getDevice();
                    CcuLog.d(L.TAG_CCU_BLE,"onScanResult called "+device.getName()+" address "+device.getAddress());
                    if(device != null && device.getName() != null && !(isDeviceMatching(device.getName()))){
                        setListViewEmptyView();
                        return;
                    } else {
                        int deviceCount = mLeDeviceListAdapter.getCount();
                        int deviceCountLimitToShow = 4;
                        int maxSizeOfListView = 360;
                        if(deviceCount >= deviceCountLimitToShow) {
                            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                            float density = displayMetrics.density;
                            mBLEDeviceListListView.getLayoutParams().height = (int) (maxSizeOfListView * density);
                            mBLEDeviceListListView.requestLayout();
                        }
                        noDevicesLayout.setVisibility(View.GONE);
                        availableDevicesLayout.setVisibility(View.VISIBLE);
                       viewConnectManuallyOptionForAvailableDevice();
                    }
                    mLeDeviceListAdapter.addDevice(device);
                });
            }
        }
    };

    private boolean isDeviceMatching(String deviceName) {
        switch (deviceName){
            case SerialConsts.SMART_NODE_NAME :
                if(mNodeType == NodeType.SMART_NODE){
                    return true;
                }
                break;
            case SerialConsts.HYPERSTAT_NAME :
                if(mNodeType == NodeType.HYPER_STAT){
                    return true;
                }
                break;
            case SerialConsts.HYPERSTATSPLIT_NAME :
                if(mNodeType == NodeType.HYPERSTATSPLIT){
                    return true;
                }
                break;
            case SerialConsts.SMART_STAT_NAME :
                if(mNodeType == NodeType.SMART_STAT){
                    return true;
                }
                break;
            case SerialConsts.HELIONODE_NAME :
                if(mNodeType == NodeType.HELIO_NODE){
                    return true;
                }
                break;
       }
        return false;
    }

    private NodeType mNodeType;


    public static FragmentDeviceScan getInstance(short pairingAddress, String name,
                                                 String floorName, NodeType nodeType,
                                                 ProfileType profileType)
    {
        FragmentDeviceScan fds = new FragmentDeviceScan();
        Bundle args = new Bundle();
        args.putShort(ARG_PAIRING_ADDR, pairingAddress);
        args.putString(ARG_NAME, name);
        args.putString(FLOOR_NAME, floorName);
        args.putString(PROFILE_TYPE, profileType.name());
        args.putString(NODE_TYPE, nodeType.toString());
        fds.setArguments(args);
        return fds;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED)
        {
            getFragmentManager().popBackStack();
            return;
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK){

            if (mBluetoothLeScanner == null) {
                mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        mName = getArguments().getString(ARG_NAME);
        mPairingAddress = getArguments().getShort(ARG_PAIRING_ADDR);
        mFloorName = getArguments().getString(FLOOR_NAME);
        mProfileType = ProfileType.valueOf(getArguments().getString(PROFILE_TYPE));
        mNodeType = NodeType.valueOf(getArguments().getString(NODE_TYPE));
        View retVal = inflater.inflate(R.layout.fragment_device_scan, container, false);
        ButterKnife.bind(this, retVal);
        setTitle(getString(R.string.scan_dialog_title));
        return retVal;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        connectManuallyInAvailableDevice.setOnClickListener(connectManuallyListener);
        connectManuallyNoDevice.setOnClickListener(connectManuallyListener);
    }
    @Override
    public void onResume()
    {
        super.onResume();
        searchDevices();
    }

    private void searchDevices() {
        CcuLog.d(L.TAG_CCU_BLE, "ble searching...");
        bluetoothStateReceiver = new BluetoothStateReceiver();
        ExecutorTask.executeBackground(() -> {
            if (mBluetoothAdapter != null) {
                if (!mBluetoothAdapter.isEnabled()) {
                    // Ensures Bluetooth is enabled isOn the device.  If Bluetooth is not currently enabled,
                    // fire an intent to display a dialog asking the user to grant permission to enable it.
                    //Skip BLE because we can't emulate it.
                    CcuLog.d(L.TAG_CCU_BLE, "enabling bluetooth...");
                    mBluetoothAdapter.enable();
                    requireContext().registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

                } else {

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {

                        if (mBLEDeviceListListView.getCount() == 0 && getActivity() != null) {
                            CcuLog.d(L.TAG_CCU_BLE, "disabling bluetooth...");
                            mBluetoothAdapter.disable();
                            requireContext().registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
                        }
                    }, 10000);


                    try {
                        // Initializes list view adapter.
                        getActivity().runOnUiThread(() -> {
                            FragmentActivity fragmentActivity = FragmentDeviceScan.this.getActivity();
                            if (fragmentActivity == null) {
                                return;
                            }
                            mLeDeviceListAdapter = new LeDeviceListAdapter(fragmentActivity);
                            setListViewEmptyView();
                            mBLEDeviceListListView.setAdapter(mLeDeviceListAdapter);
                            mBLEDeviceListListView.setOnItemClickListener((adapterView, view, position, id) -> {
                                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                                CcuLog.d(L.TAG_CCU_BLE,"Clicked on the device "+device);
                                finish(device);
                                scanLeDevice(false);
                            });
                            scanLeDevice(true);
                        });
                    } catch (NullPointerException e) {
                        CcuLog.e(L.TAG_CCU_BLE, "Scanning has been interrupted. Fragment not attached to the activity.", e);
                        e.printStackTrace();
                    }

                }
            }
        });
    }
    View.OnClickListener connectManuallyListener = view -> {
        AlternatePairingFragment alternatePairingFragment = new AlternatePairingFragment(
                mNodeType, mPairingAddress, mName, mFloorName, mProfileType);
        showDialogFragment(alternatePairingFragment, AlternatePairingFragment.ID);
    };

    private void setListViewEmptyView() {
        if (isDeviceSupportsAlternatePairing() && mLeDeviceListAdapter.getCount() == 0) {
            noDevicesLayout.setVisibility(View.VISIBLE);
            availableDevicesLayout.setVisibility(View.GONE);
            viewConnectManuallyOption();
        } else {
            if(mBLEDeviceListListView.getEmptyView() == null){
                TextView emptyText = new TextView(getActivity());
                emptyText
                        .setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                emptyText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
                emptyText.setPadding(20, 20, 20, 20);
                ((ViewGroup) mBLEDeviceListListView.getParent()).addView(emptyText);
                emptyText.setText(R.string.ble_no_devices_found_warning);
                mBLEDeviceListListView.setEmptyView(emptyText);
            }
        }
    }

    private void viewConnectManuallyOption() {
        if (isDeviceSupportsAlternatePairing()) {
            pairManuallyTextNoDevice.setVisibility(View.VISIBLE);
            connectManuallyNoDevice.setVisibility(View.VISIBLE);
        }
    }

    private void viewConnectManuallyOptionForAvailableDevice() {
        if (isDeviceSupportsAlternatePairing()) {
            pairManuallyTextInAvailableList.setVisibility(View.VISIBLE);
            connectManuallyInAvailableDevice.setVisibility(View.VISIBLE);
        }
    }

    private boolean isDeviceSupportsAlternatePairing() {
        return mNodeType == NodeType.SMART_NODE || mNodeType.equals(NodeType.HELIO_NODE) ||
                mNodeType.equals(NodeType.HYPER_STAT);
    }


    private void finish(BluetoothDevice device)
    {
        DialogFragment newFragment = FragmentBLEDevicePin
                                             .getInstance(mPairingAddress, mName, mFloorName, mNodeType, mProfileType, device);
        showDialogFragment(newFragment, FragmentBLEDevicePin.ID);
    }
    
    
    private void scanLeDevice(final boolean enable)
    {
        if (enable)
        {
            mScanning = true;

            List<ScanFilter> filters = new ArrayList<>();
            ScanFilter smartNode = new ScanFilter.Builder()
                    .setDeviceName(SerialConsts.SMART_NODE_NAME)
                    .build();
            ScanFilter smartStat = new ScanFilter.Builder()
                    .setDeviceName(SerialConsts.SMART_STAT_NAME)
                    .build();
            ScanFilter hyperStat = new ScanFilter.Builder()
                    .setDeviceName(SerialConsts.HYPERSTAT_NAME)
                    .build();
            ScanFilter hyperStatSplit = new ScanFilter.Builder()
                    .setDeviceName(SerialConsts.HYPERSTATSPLIT_NAME)
                    .build();
            ScanFilter helioNode = new ScanFilter.Builder()
                    .setDeviceName(SerialConsts.HELIONODE_NAME)
                    .build();
            filters.add(smartNode);
            filters.add(smartStat);
            filters.add(hyperStat);
            filters.add(helioNode);
            filters.add(hyperStatSplit);

            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setReportDelay(0)
                    .build();

            if (mBluetoothLeScanner == null) {
                mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            }
            mBluetoothLeScanner.startScan(filters, scanSettings, mScanCallback);
            CcuLog.d(L.TAG_CCU_BLE,"Scan Started");
        }
        else
        {
            mScanning = false;
            if(mBluetoothLeScanner != null) {
                mBluetoothLeScanner.flushPendingScanResults(mScanCallback);
                mBluetoothLeScanner.stopScan(mScanCallback);
            }
            CcuLog.d(L.TAG_CCU_BLE,"Scan Stopped");
        }
    }
    
    
    @Override
    public String getIdString()
    {
        return ID;
    }
    
    
    @Override
    public void onPause()
    {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Use this check to determine whether BLE is supported isOn the device.  Then you can
        // selectively disable BLE-related features.
        if (!getActivity().getPackageManager()
                          .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this.getActivity(), "BLE not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        // Checks if Bluetooth is supported isOn the device.
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(this.getActivity(), "BLE not supported", Toast.LENGTH_SHORT).show();
        }
    }
    
    
    static class ViewHolder
    {
        TextView deviceName;
        TextView deviceAddress;
    }
    
    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter
    {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater             mInflator;
        
        
        public LeDeviceListAdapter(FragmentActivity fragmentActivity)
        {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = fragmentActivity.getLayoutInflater();
        }
        
        
        public void addDevice(BluetoothDevice device)
        {
            if (!mLeDevices.contains(device))
            {
                CcuLog.d(L.TAG_CCU_BLE,"New Device added to the list "+device);
                mLeDevices.add(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        }
        
        
        public BluetoothDevice getDevice(int position)
        {
            return mLeDevices.get(position);
        }


        @Override
        public int getCount()
        {
            return mLeDevices.size();
        }
        
        
        @Override
        public Object getItem(int i)
        {
            return mLeDevices.get(i);
        }
        
        
        @Override
        public long getItemId(int i)
        {
            return i;
        }
        
        
        @Override
        public View getView(int i, View view, ViewGroup viewGroup)
        {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null)
            {
                view = mInflator.inflate(R.layout.listitem_device_scan, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && !deviceName.isEmpty())
            {
                viewHolder.deviceName.setText(deviceName);
            }
            else
            {
                viewHolder.deviceName.setText("Unknown Device");
            }
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }

    public class BluetoothStateReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (isAdded()) {
                    final BluetoothManager bluetoothManager =
                            (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);

                    mBluetoothAdapter = bluetoothManager.getAdapter();
                    mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

                    if (state == BluetoothAdapter.STATE_ON) {
                        CcuLog.d(L.TAG_CCU_BLE, "bluetooth is enabled");
                        requireContext().unregisterReceiver(bluetoothStateReceiver);
                        searchDevices();
                    }
                    if (state == BluetoothAdapter.STATE_OFF) {
                        CcuLog.d(L.TAG_CCU_BLE, "bluetooth is disabled");
                        requireContext().unregisterReceiver(bluetoothStateReceiver);
                        searchDevices();
                    }
                } else {
                    CcuLog.d(L.TAG_CCU_BLE, "Fragment is not yet added");
                }
            }
        }
    }
}


