package a75f.io.renatus.BLE;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import a75f.io.bluetooth.BLEAction;
import a75f.io.bluetooth.BLEProvisionService;
import a75f.io.bo.ble.GattAttributes;
import a75f.io.bo.ble.GattPin;
import a75f.io.renatus.R;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by ryanmattison on 7/24/17.
 */

public class FragmentBLEDevicePin extends DialogFragment {

    private static final String BUNDLE_KEY_BLUETOOTH_DEVICE = "bluetooth_device";

    @BindView(R.id.ble_dialog_enter_pin_textview)
    TextView bleDialogEnterPinTextview;
    @BindView(R.id.ble_dialog_enter_pin_edittext)
    EditText bleDialogEnterPinEdittext;
    @BindView(R.id.ble_dialog_done_button)
    Button bleDialogDoneButton;

    private BluetoothDevice mDevice;
    private BLEProvisionService mBLEProvisionService;
    private GattPin mGattPin;

    public static FragmentBLEDevicePin getInstance(BluetoothDevice device) {
        FragmentBLEDevicePin bleProvisionDialogFragment = new FragmentBLEDevicePin();
        Bundle b = new Bundle();
        b.putParcelable(BUNDLE_KEY_BLUETOOTH_DEVICE, device);
        bleProvisionDialogFragment.setArguments(b);
        return bleProvisionDialogFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        if (getArguments() != null && getArguments().containsKey(BUNDLE_KEY_BLUETOOTH_DEVICE)) {
            mDevice = getArguments().getParcelable(BUNDLE_KEY_BLUETOOTH_DEVICE);
        } else //No Bluetooth Device throw exception
        {
            error();
        }

        if (mDevice == null) {
            error();
        }

        Log.i(TAG, "mAddress: " + (mDevice.getAddress() != null ? mDevice.getAddress() : "null"));
        View retVal = inflater.inflate(R.layout.fragment_ble_pin_dialog, container, false);
        ButterKnife.bind(this, retVal);
        Log.i(TAG, "Butterknife Bind");
        return retVal;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "On View Created");
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "Attempt to bind service");
        Intent gattServiceIntent = new Intent(this.getContext(), BLEProvisionService.class);
        getActivity().bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.i(TAG, "Service binded");

    }

    private void error() {
        Log.e(TAG, "Please select a bluetooth device and package it before using this fragment.");
        dismissAllowingStateLoss();
    }

    @OnClick(R.id.ble_dialog_done_button)
    void done() {
        String editField = bleDialogEnterPinEdittext.getText().toString();
        if (editField.equalsIgnoreCase(String.valueOf(mGattPin.getPin()))) {
            Toast.makeText(this.getActivity(), "Pin Matches Proceed with BLE chararecteristic write", Toast.LENGTH_LONG).show();
        }

        Log.i(TAG, "Done");
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEProvisionService = ((BLEProvisionService.LocalBinder) service).getService();
            if (!mBLEProvisionService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                getActivity().finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            Log.i(TAG, "Attempt to connect to mDevice.getAddress(): " + mDevice.getAddress());
            mBLEProvisionService.connect(mDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEProvisionService.disconnect();
            mBLEProvisionService = null;
        }
    };

    // Called in a separate thread
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onBLEEvent(BLEProvisionService.BLEEvent event) {
        Log.i(TAG, "Event Type: " + event.getAction().name());
        if (event.getAction() == BLEAction.ACTION_GATT_SERVICES_DISCOVERED) {
                mBLEProvisionService.readCharacteristic(mBLEProvisionService.getSupportedGattAttribute(GattAttributes.BLE_PIN));
        } else if (event.getAction() == BLEAction.ACTION_DATA_AVAILABLE && event.getBluetoothGattCharacteristic() != null) {
            if (event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.BLE_PIN)) {
                Log.i(TAG, "BluetoothGatt Pin Service Read");
                mGattPin = GattPin.initialize(event.getBluetoothGattCharacteristic().getValue());
                Log.i(TAG, "mGattPin: " + mGattPin.getPin());
                FragmentBLEDevicePin.this.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupPinFields();
                    }
                });
                setupPinFields();
            }
        }

    }

    private void setupPinFields() {
        bleDialogEnterPinEdittext.setEnabled(true);
        bleDialogDoneButton.setEnabled(true);
        bleDialogEnterPinTextview.setText("Please enter pin: " + mGattPin.getPin());
        Log.i(TAG, "Set pin textview to: " + "Please enter pin: " + mGattPin.getPin());
    }


    @Override
    public void onStart() {
        Log.i(TAG, "OnStart");
        super.onStart();
        EventBus.getDefault().register(this);
        if (mBLEProvisionService != null) {
            final boolean result = mBLEProvisionService.connect(mDevice.getAddress());
            Log.d(TAG, "Connect request result=" + result);
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "OnStop");
        EventBus.getDefault().unregister(this);

        getActivity().unbindService(mServiceConnection);
        mBLEProvisionService = null;
    }

    private static final String TAG = FragmentBLEDevicePin.class.getSimpleName();

}