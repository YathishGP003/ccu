package a75f.io.renatus.BLE;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
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
import a75f.io.bo.SmartNode;
import a75f.io.bo.ble.BLERoomName;
import a75f.io.bo.ble.GattAttributes;
import a75f.io.bo.ble.GattPin;
import a75f.io.bo.ble.StructShort;
import a75f.io.bo.serial.SerialConsts;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusApp;
import a75f.io.util.ByteArrayUtils;
import a75f.io.util.Globals;
import a75f.io.util.prefs.EncryptionPrefs;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by ryanmattison on 7/24/17.
 */

public class FragmentBLEDevicePin extends DialogFragment
{
	
	private static final String BUNDLE_KEY_BLUETOOTH_DEVICE = "bluetooth_device";
	private static final String TAG                         = FragmentBLEDevicePin.class.getSimpleName();
	@BindView(R.id.ble_dialog_enter_pin_textview)
	TextView bleDialogEnterPinTextview;
	@BindView(R.id.ble_dialog_enter_pin_edittext)
	EditText bleDialogEnterPinEdittext;
	@BindView(R.id.ble_dialog_done_button)
	Button   bleDialogDoneButton;
	private SmartNode mSmartNode;
	private boolean mPinEntered = false;
	private BluetoothDevice     mDevice;
	private BLEProvisionService mBLEProvisionService;
	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection()
	{
		
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service)
		{
			mBLEProvisionService = ((BLEProvisionService.LocalBinder) service).getService();
			if (!mBLEProvisionService.initialize())
			{
				Log.e(TAG, "Unable to initialize Bluetooth");
				getActivity().finish();
			}
			// Automatically connects to the device upon successful start-up initialization.
			Log.i(TAG, "Attempt to connect to mDevice.getAddress(): " + mDevice.getAddress());
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					mBLEProvisionService.connect(mDevice.getAddress());
				}
			}, 250);
		}
		
		@Override
		public void onServiceDisconnected(ComponentName componentName)
		{
			mBLEProvisionService.disconnect();
			mBLEProvisionService = null;
		}
	};
	private GattPin mGattPin;
	private byte[] mByteBufferZoneConfigInProgress = new byte[]{GattAttributes.ZONE_CONFIGURATION_IN_PROGRESS};
	private byte[] mBLERoomNameBuffer;
	private byte[] mBLEAddressBuffer;
	public static FragmentBLEDevicePin getInstance(BluetoothDevice device)
	{
		FragmentBLEDevicePin bleProvisionDialogFragment = new FragmentBLEDevicePin();
		Bundle b = new Bundle();
		b.putParcelable(BUNDLE_KEY_BLUETOOTH_DEVICE, device);
		bleProvisionDialogFragment.setArguments(b);
		return bleProvisionDialogFragment;
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.i(TAG, "onCreateView");
		if (getArguments() != null && getArguments().containsKey(BUNDLE_KEY_BLUETOOTH_DEVICE))
		{
			mDevice = getArguments().getParcelable(BUNDLE_KEY_BLUETOOTH_DEVICE);
		}
		else //No Bluetooth Device throw exception
		{
			error();
		}
		if (mDevice == null)
		{
			error();
		}
		mSmartNode = Globals.getInstance().getSmartNode();
		Log.i(TAG, "mAddress: " + (mDevice.getAddress() != null ? mDevice.getAddress() : "null"));
		View retVal = inflater.inflate(R.layout.fragment_ble_pin_dialog, container, false);
		ButterKnife.bind(this, retVal);
		Log.i(TAG, "Butterknife Bind");
		return retVal;
	}
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		Log.i(TAG, "On View Created");
		super.onViewCreated(view, savedInstanceState);
		Log.i(TAG, "Attempt to bind service");
		Intent gattServiceIntent = new Intent(this.getContext(), BLEProvisionService.class);
		getActivity().bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
		Log.i(TAG, "Service binded");
		BLERoomName mBLERoomName = new BLERoomName();
		StructShort mBLEAddress = new StructShort();
		mBLEAddress.smartNodeAddress.set(mSmartNode.getMeshAddress());
		mBLEAddressBuffer = mBLEAddress.getOrderedBuffer();
		mBLERoomName.roomName.set(mSmartNode.getName());
		mBLERoomNameBuffer = mBLERoomName.getOrderedBuffer();
	}
	private void error()
	{
		Log.e(TAG, "Please select a bluetooth device and package it before using this fragment.");
		dismissAllowingStateLoss();
	}
	@OnClick(R.id.ble_dialog_done_button)
	void done()
	{
		String editField = bleDialogEnterPinEdittext.getText().toString();
		//If they have entered the pin, the done button becomes a cancel button
		if (!mPinEntered)
		{
			if (editField.equalsIgnoreCase(String.valueOf(mGattPin.getPin())))
			{
				mPinEntered = true;
				//Pin has been entered, set button to cancel & and hide edittext.
				bleDialogDoneButton.setText("Cancel");
				bleDialogEnterPinTextview.setText("Processing");
				bleDialogEnterPinEdittext.setVisibility(View.GONE);
				writeZoneUpdateInProgress();
				//After the pin was successfully entered, write channel.
				Toast.makeText(this.getActivity(), "Pin Matches Proceed with BLE chararecteristic write", Toast.LENGTH_LONG).show();
			}
		}
		else
		{
			dismiss();
		}
		Log.i(TAG, "Done");
	}
	private void writeZoneUpdateInProgress()
	{
		//Zone Configuration Status
		boolean initiallySuccessful = mBLEProvisionService.writeCharacteristic(mBLEProvisionService.getSupportedGattAttribute(GattAttributes.ZONE_CONFIGURATION_STATUS), mByteBufferZoneConfigInProgress);
		Log.d(TAG, "Writing zone configuration status:  " + initiallySuccessful);
	}
	// Called in a separate thread
	@Subscribe(threadMode = ThreadMode.ASYNC)
	public void onBLEEvent(BLEProvisionService.BLEEvent event)
	{
		Log.i(TAG, "Event Type: " + event.getAction().name());
		if (event.getAction() == BLEAction.ACTION_GATT_SERVICES_DISCOVERED)
		{
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					mBLEProvisionService.readCharacteristic(GattAttributes.BLE_PIN);
				}
			}, 250);
		}
		else if (event.getAction() == BLEAction.ACTION_DATA_AVAILABLE && event.getBluetoothGattCharacteristic() != null)
		{
			if (event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.BLE_PIN))
			{
				Log.i(TAG, "BluetoothGatt Pin Service Read");
				mGattPin = GattPin.initialize(event.getBluetoothGattCharacteristic().getValue());
				Log.i(TAG, "mGattPin: " + mGattPin.getPin());
				FragmentBLEDevicePin.this.getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						setupPinFields();
					}
				});
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.ZONE_CONFIGURATION_STATUS))
			{
				if (!mByteBufferZoneConfigInProgress.equals(event.getBluetoothGattCharacteristic().getValue()))
				{
					Log.i(TAG, "Bluetooth configured success");
					((RenatusApp) getActivity().getApplication()).isProvisioned = true;
					Log.i(TAG, "Blue Status: " + event.getBluetoothGattCharacteristic().getValue().equals(new byte[]{GattAttributes.ZONE_CONFIGURATION_SUCCESS}));
					FragmentBLEDevicePin.this.getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							Toast.makeText(FragmentBLEDevicePin.this.getActivity(), "Pairing Success!", Toast.LENGTH_LONG).show();
							dismissAllowingStateLoss();
						}
					});
				}
			}
		}
		else if (event.getAction() == BLEAction.ACTION_DATA_WROTE && event.getBluetoothGattCharacteristic() != null)
		{
			if (event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.ZONE_CONFIGURATION_STATUS))
			{
				if (needsLinkKey())
				{
					mBLEProvisionService.writeCharacteristic(GattAttributes.BLE_LINK_KEY, EncryptionPrefs.getBLELinkKey());
				}
				else
				{
					mBLEProvisionService.writeCharacteristic(GattAttributes.ROOM_NAME, mBLERoomNameBuffer);
				}
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.BLE_LINK_KEY))
			{
				mBLEProvisionService.writeCharacteristic(GattAttributes.ROOM_NAME, mBLERoomNameBuffer);
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.ROOM_NAME))
			{
				mBLEProvisionService.writeCharacteristic(GattAttributes.LW_MESH_PAIRING_ADDRESS, mBLEAddressBuffer);
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.LW_MESH_PAIRING_ADDRESS))
			{
				mBLEProvisionService.writeCharacteristic(GattAttributes.LW_MESH_SECURITY_KEY, EncryptionPrefs.getEncryptionKey());
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.LW_MESH_SECURITY_KEY))
			{
				mBLEProvisionService.writeCharacteristic(GattAttributes.FIRMWARE_SIGNATURE_KEY, EncryptionPrefs.getFirmwareSignatureKey());
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.FIRMWARE_SIGNATURE_KEY))
			{
				byte[] crc = null;
				if (needsLinkKey())
				{
					crc = ByteArrayUtils.addBytes(EncryptionPrefs.getBLELinkKey(), mBLERoomNameBuffer, mBLEAddressBuffer, EncryptionPrefs.getEncryptionKey(), EncryptionPrefs.getFirmwareSignatureKey(), mByteBufferZoneConfigInProgress);
				}
				else
				{
					crc = ByteArrayUtils.addBytes(mBLERoomNameBuffer, mBLEAddressBuffer, EncryptionPrefs.getEncryptionKey(), EncryptionPrefs.getFirmwareSignatureKey(), mByteBufferZoneConfigInProgress);
				}
				mBLEProvisionService.writeCharacteristic(GattAttributes.CRC, ByteArrayUtils.bigToLittleEndian(ByteArrayUtils.computeCrc(crc)));
				Log.i(TAG, "Wrote to CRC gatt characteristic");
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString().equalsIgnoreCase(GattAttributes.CRC))
			{
				Log.i(TAG, "Successfully wrote to CRC...");
				Log.i(TAG, "Read status");
				mBLEProvisionService.readCharacteristic(GattAttributes.ZONE_CONFIGURATION_STATUS);
			}
		}
	}
	private void setupPinFields()
	{
		bleDialogEnterPinEdittext.setEnabled(true);
		bleDialogDoneButton.setEnabled(true);
		bleDialogEnterPinTextview.setText("Please enter pin: " + mGattPin.getPin());
		Log.i(TAG, "Set pin textview to: " + "Please enter pin: " + mGattPin.getPin());
	}
	private boolean needsLinkKey()
	{
		return !mDevice.getName().equalsIgnoreCase(SerialConsts.SMART_STAT_NAME);
	}
	@Override
	public void onStart()
	{
		Log.i(TAG, "OnStart");
		super.onStart();
		EventBus.getDefault().register(this);
		if (mBLEProvisionService != null)
		{
			final boolean result = mBLEProvisionService.connect(mDevice.getAddress());
			Log.d(TAG, "Connect request result=" + result);
		}
	}
	@Override
	public void onStop()
	{
		super.onStop();
		Log.i(TAG, "OnStop");
		EventBus.getDefault().unregister(this);
		getActivity().unbindService(mServiceConnection);
		mBLEProvisionService = null;
	}
}