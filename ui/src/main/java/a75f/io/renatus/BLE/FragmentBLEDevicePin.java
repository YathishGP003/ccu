package a75f.io.renatus.BLE;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import a75f.io.bluetooth.BLEAction;
import a75f.io.bluetooth.BLEProvisionService;
import a75f.io.bo.SmartNode;
import a75f.io.bo.ble.GattAttributes;
import a75f.io.bo.ble.GattPin;
import a75f.io.logic.SmartNodeBLL;
import a75f.io.renatus.R;
import a75f.io.util.ByteArrayUtils;
import a75f.io.util.prefs.EncryptionPrefs;
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


    private boolean mPinEntered = false;
    private BluetoothDevice mDevice;
    private BLEProvisionService mBLEProvisionService;
    private GattPin mGattPin;
    private SmartNode mSmartNode;
    private byte[] mByteBufferForSmartNodeAdderess;
    private byte[] mByteBufferZoneConfigInProgress;

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
            mSmartNode = SmartNodeBLL.generateSmartNodeFromJSON();
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

        //If they have entered the pin, the done button becomes a cancel button
        if (!mPinEntered) {
            if (editField.equalsIgnoreCase(String.valueOf(mGattPin.getPin()))) {
                mPinEntered = true;
                //Pin has been entered, set button to cancel & and hide edittext.
                bleDialogDoneButton.setText("Cancel");
                bleDialogEnterPinTextview.setText("Processing");
                bleDialogEnterPinEdittext.setVisibility(View.GONE);
                writeZoneUpdateInProgress();
                //After the pin was successfully entered, write channel.
                Toast.makeText(this.getActivity(), "Pin Matches Proceed with BLE chararecteristic write", Toast.LENGTH_LONG).show();
            }
        } else {
            dismiss();
        }

        Log.i(TAG, "Done");
    }

    private void writeZoneUpdateInProgress() {
        //Zone Configuration Status

        ByteBuffer byteBuffer = ByteBuffer.allocate(1).put(GattAttributes.ZONE_CONFIGURATION_IN_PROGRESS);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        mByteBufferZoneConfigInProgress = byteBuffer.array();
        boolean initiallySuccessful = mBLEProvisionService.writeCharacteristic(mBLEProvisionService.getSupportedGattAttribute(GattAttributes.ZONE_CONFIGURATION_STATUS), mByteBufferZoneConfigInProgress);
        Log.d(TAG, "Writing zone configuration status:  " + initiallySuccessful);
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
            mBLEProvisionService.readCharacteristic(GattAttributes.BLE_PIN);
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
            }
            else if(event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.ZONE_CONFIGURATION_STATUS))
            {
                if(mByteBufferZoneConfigInProgress != event.getBluetoothGattCharacteristic().getValue())
                {
                    Log.i(TAG, "Bluetooth configured success");
                    Toast.makeText(this.getActivity(), "Pairing Success!", Toast.LENGTH_LONG).show();
                    Log.i(TAG, "Blue Status: " + event.getBluetoothGattCharacteristic().getValue().equals(new byte[]{GattAttributes.ZONE_CONFIGURATION_SUCCESS}));

                    FragmentBLEDevicePin.this.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dismissAllowingStateLoss();
                        }
                    });

                }
            }

        } else if (event.getAction() == BLEAction.ACTION_DATA_WROTE && event.getBluetoothGattCharacteristic() != null) {
            if (event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.ZONE_CONFIGURATION_STATUS)) {
                Log.i(TAG, "Successfully wrote to zone configuration status!");
                Log.i(TAG, "Wiritng BLE Link Key");
                mBLEProvisionService.writeCharacteristic(GattAttributes.BLE_LINK_KEY, EncryptionPrefs.getBLELinkKey());
            }
            else if (event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.BLE_LINK_KEY)) {
                Log.i(TAG, "Successfully wrote to BLE LINK KEY!");
                Log.i(TAG, "Writing room name");
                mBLEProvisionService.writeCharacteristic(GattAttributes.ROOM_NAME, ByteArrayUtils.nullTerminateAndFillArrayToLengthFromString(mSmartNode.getName(), 25));
            }
            else if(event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.ROOM_NAME))
            {
                Log.i(TAG, "Successfully wrote room name!");
                Log.i(TAG, "Writing pairing address");
                ByteBuffer byteBuffer = ByteBuffer.allocate(2).putShort(mSmartNode.getMeshAddress());
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                mByteBufferForSmartNodeAdderess = byteBuffer.array();
                mBLEProvisionService.writeCharacteristic(GattAttributes.LW_MESH_PAIRING_ADDRESS, mByteBufferForSmartNodeAdderess);
            }
            else if(event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.LW_MESH_PAIRING_ADDRESS))
            {
                Log.i(TAG, "Successfully wrote mesh pairing address");
                Log.i(TAG, "Now writing mesh security key");
                mBLEProvisionService.writeCharacteristic(GattAttributes.LW_MESH_SECURITY_KEY, mSmartNode.getEncryptionKey());
            }
            else if(event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.LW_MESH_SECURITY_KEY))
            {
                Log.i(TAG, "Sucessfully wrote mesh security key");
                Log.i(TAG, "now writing firmware signature key");
                mBLEProvisionService.writeCharacteristic(GattAttributes.FIRMWARE_SIGNATURE_KEY, EncryptionPrefs.getFirmwareSignatureKey());
            }
            else if(event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.FIRMWARE_SIGNATURE_KEY))
            {
                Log.i(TAG, "Successfully wrote firmware signature key.");
                byte[] message = ByteArrayUtils.addBytes(EncryptionPrefs.getBLELinkKey(), ByteArrayUtils.nullTerminateAndFillArrayToLengthFromString(mSmartNode.getName(), 25), mByteBufferForSmartNodeAdderess,
                        mSmartNode.getEncryptionKey(), EncryptionPrefs.getFirmwareSignatureKey(), mByteBufferZoneConfigInProgress);
                mBLEProvisionService.writeCharacteristic(GattAttributes.CRC, ByteArrayUtils.bigToLittleEndian(ByteArrayUtils.computeCrc(message)));
                Log.i(TAG, "Wrote to CRC gatt characteristic");
            }
            else if(event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.CRC))
            {
                Log.i(TAG, "Successfully wrote to CRC...");
                Log.i(TAG, "Read status");
                mBLEProvisionService.readCharacteristic(GattAttributes.ZONE_CONFIGURATION_STATUS);
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