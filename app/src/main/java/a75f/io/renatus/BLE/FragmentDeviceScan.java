package a75f.io.renatus.BLE;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.device.serial.SerialConsts;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.R;
import butterknife.BindView;
import butterknife.ButterKnife;

import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.ARG_NAME;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.ARG_PAIRING_ADDR;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.FLOOR_NAME;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.NODE_TYPE;

/**
 * Created by ryanmattison isOn 7/24/17.
 */
public class FragmentDeviceScan extends BaseDialogFragment
{
    
    public static final  String ID                = "scan_dialog";
    static final         int    REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 60000;
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
    boolean             mScanning;

    private ScanCallback mScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if(getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    BluetoothDevice device = result.getDevice();
                    if (device != null && device.getName() != null &&
                            (device.getName().equalsIgnoreCase(SerialConsts.SMART_NODE_NAME) ||
                                    device.getName().equalsIgnoreCase(SerialConsts.SMART_STAT_NAME))) {
                        mLeDeviceListAdapter.addDevice(device);
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    };
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
    public void onResume()
    {
        super.onResume();
        // Ensures Bluetooth is enabled isOn the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        //Skip BLE because we can't emulate it.
        new Thread(() -> {
            if (mBluetoothAdapter != null)
            {
                if (!mBluetoothAdapter.isEnabled())
                {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                else
                {
                    // Initializes list view adapter.
                    mLeDeviceListAdapter = new LeDeviceListAdapter();
                    setListViewEmptyView();
                    mBLEDeviceListListView.setAdapter(mLeDeviceListAdapter);
                    mBLEDeviceListListView.setOnItemClickListener((adapterView, view, position, id) -> {
                        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                        finish(device);
                        scanLeDevice(false);
                    });
                    scanLeDevice(true);
                }
            }
        }).start();
    }
    
    
    private void setListViewEmptyView()
    {
        TextView emptyText = new TextView(getActivity());
        emptyText
                .setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        emptyText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        emptyText.setPadding(20, 20, 20, 20);
        ((ViewGroup) mBLEDeviceListListView.getParent()).addView(emptyText);
        emptyText.setText(R.string.ble_no_devices_found_warning);
        mBLEDeviceListListView.setEmptyView(emptyText);
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
            new Handler(Looper.getMainLooper()).postDelayed(() -> mBluetoothLeScanner.stopScan(mScanCallback),SCAN_PERIOD);

            mBluetoothLeScanner.startScan(mScanCallback);
        }
        else
        {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mScanCallback);
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
            return;
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
        
        
        public LeDeviceListAdapter()
        {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = FragmentDeviceScan.this.getActivity().getLayoutInflater();
        }
        
        
        public void addDevice(BluetoothDevice device)
        {
            if (!mLeDevices.contains(device))
            {
                mLeDevices.add(device);
            }
        }
        
        
        public BluetoothDevice getDevice(int position)
        {
            return mLeDevices.get(position);
        }
        
        
        public void clear()
        {
            mLeDevices.clear();
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
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
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
}


