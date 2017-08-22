package a75f.io.renatus.BLE;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
import org.javolution.annotations.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import a75f.io.bluetooth.BLEAction;
import a75f.io.bluetooth.BLEProvisionService;
import a75f.io.bo.ble.BLERoomName;
import a75f.io.bo.ble.GattAttributes;
import a75f.io.bo.ble.GattPin;
import a75f.io.bo.ble.StructShort;
import a75f.io.bo.serial.SerialConsts;
import a75f.io.logic.SmartNodeBLL;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.R;
import a75f.io.renatus.ZONEPROFILE.LightingZoneProfileFragment;
import a75f.io.util.ByteArrayUtils;
import a75f.io.util.prefs.EncryptionPrefs;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.ARG_NAME;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.ARG_PAIRING_ADDR;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.FLOOR_NAME;
import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by ryanmattison on 7/24/17.
 */

public class FragmentBLEDevicePin extends BaseDialogFragment
{
	
	public static final String ID                                   = "pin_dialog";
	public static final int    RETRY_TIME_GATT_SERVICES_UNAVAILABLE = 500;
	
	private static final String BUNDLE_KEY_BLUETOOTH_DEVICE = "bluetooth_device";
	private static final String TAG                         =
			FragmentBLEDevicePin.class.getSimpleName();
	
	GattPin mGattPin;
	byte[] mByteBufferZoneConfigInProgress =
			new byte[]{GattAttributes.ZONE_CONFIGURATION_IN_PROGRESS};
	byte[] mBLERoomNameBuffer;
	byte[] mBLEAddressBuffer;
	String mName;
	String mFloorName;
	short  mPairingAddress;
	boolean mPinEntered = false;
	BluetoothDevice     mDevice;
	BLEProvisionService mBLEProvisionService;
	final ServiceConnection mServiceConnection = new ServiceConnection()
	{
		
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service)
		{
			mBLEProvisionService = ((BLEProvisionService.LocalBinder) service).getService();
			if (!mBLEProvisionService.initialize())
			{
				Toast.makeText(FragmentBLEDevicePin.this
						               .getActivity(), R.string.unable_to_bluetooth, Toast.LENGTH_LONG)
				     .show();
				getActivity().finish();
			}
			else
			{
				mBLEProvisionService.connect(mDevice.getAddress());
			}
		}
		
		
		@Override
		public void onServiceDisconnected(ComponentName componentName)
		{
			mBLEProvisionService.disconnect();
			mBLEProvisionService.close();
			mBLEProvisionService = null;
		}
	};
	@BindView(R.id.ble_dialog_enter_pin_textview)
	TextView bleDialogEnterPinTextview;
	@BindView(R.id.ble_dialog_enter_pin_edittext)
	EditText bleDialogEnterPinEdittext;
	@BindView(R.id.ble_dialog_done_button)
	Button   bleDialogDoneButton;
	
	
	public static FragmentBLEDevicePin getInstance(short pairingAddress, String name,
	                                               String mFloorName, BluetoothDevice device)
	{
		FragmentBLEDevicePin bleProvisionDialogFragment = new FragmentBLEDevicePin();
		Bundle b = new Bundle();
		b.putParcelable(BUNDLE_KEY_BLUETOOTH_DEVICE, device);
		b.putShort(ARG_PAIRING_ADDR, pairingAddress);
		b.putString(ARG_NAME, name);
		b.putString(FragmentCommonBundleArgs.FLOOR_NAME, mFloorName);
		bleProvisionDialogFragment.setArguments(b);
		return bleProvisionDialogFragment;
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (getArguments() != null && getArguments().containsKey(BUNDLE_KEY_BLUETOOTH_DEVICE))
		{
			mDevice = getArguments().getParcelable(BUNDLE_KEY_BLUETOOTH_DEVICE);
			mName = getArguments().getString(ARG_NAME);
			mPairingAddress = getArguments().getShort(ARG_PAIRING_ADDR);
			mFloorName = getArguments().getString(FLOOR_NAME);
		}
		Intent gattServiceIntent = new Intent(getActivity(), BLEProvisionService.class);
		getActivity().bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}
	
	
	@Override
	public void onStart()
	{
		super.onStart();
		EventBus.getDefault().register(this);
	}
	
	
	@Override
	public void onStop()
	{
		super.onStop();
		EventBus.getDefault().unregister(this);
		mBLEProvisionService.disconnect();
		getActivity().unbindService(mServiceConnection);
		mBLEProvisionService = null;
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		View retVal = inflater.inflate(R.layout.fragment_ble_pin_dialog, container, false);
		ButterKnife.bind(this, retVal);
		showProgressDialog();
		return retVal;
	}
	
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		BLERoomName mBLERoomName = new BLERoomName();
		StructShort mBLEAddress = new StructShort();
		mBLEAddress.smartNodeAddress.set(mPairingAddress);
		mBLEAddressBuffer = mBLEAddress.getOrderedBuffer();
		mBLERoomName.roomName.set(mName);
		mBLERoomNameBuffer = mBLERoomName.getOrderedBuffer();
		bleDialogEnterPinEdittext.setVisibility(View.VISIBLE);
		setTitle("Pairing " + mBLERoomName.roomName.get());
	}
	
	
	@OnClick(R.id.ble_dialog_done_button)
	void done()
	{
		if (!mPinEntered)
		{
			if (bleDialogEnterPinEdittext.getText() == null ||
			    bleDialogEnterPinEdittext.getText().toString().trim().equals(""))
			{
				bleDialogEnterPinEdittext
						.setError("Pin required.  Please enter pin displayed on the 75F device you'd like to pair.");
			}
			else
			{
				String pinFeildText = bleDialogEnterPinEdittext.getText() != null &&
				                      !bleDialogEnterPinEdittext.getText().toString().trim()
				                                                .equals("")
						                      ? bleDialogEnterPinEdittext.getText().toString()
						                      : "0";
				int pinNumericValue = Integer.valueOf(pinFeildText);
				if (pinNumericValue == mGattPin.getPin())
				{
					mPinEntered = true;
					showProgressDialog();
					mBLEProvisionService.writeCharacteristic(mBLEProvisionService
							                                         .getSupportedGattAttribute(GattAttributes.ZONE_CONFIGURATION_STATUS), mByteBufferZoneConfigInProgress);
				}
				else
				{
					bleDialogEnterPinEdittext
							.setError("Pins do not match.  Please enter pin displayed on the 75F device you'd like to pair.");
				}
			}
		}
		else
		{
			dismiss();
		}
	}
	
	
	// Called in a separate thread
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onBLEEvent(BLEProvisionService.BLEEvent event)
	{
		Log.i(TAG, "Event Type: " + event.getAction().name());
		if (event.getAction() == BLEAction.ACTION_GATT_SERVICES_DISCOVERED)
		{
			if (!mBLEProvisionService.isCharacteristicReady(GattAttributes.BLE_PIN))
			{
				mBLEProvisionService.disconnect();
				new Timer().schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						mBLEProvisionService.connect(mDevice.getAddress());
					}
				}, RETRY_TIME_GATT_SERVICES_UNAVAILABLE);
			}
			else
			{
				mBLEProvisionService.readCharacteristic(GattAttributes.BLE_PIN);
			}
		}
		else if (event.getAction() == BLEAction.ACTION_DATA_AVAILABLE &&
		         event.getBluetoothGattCharacteristic() != null)
		{
			if (event.getBluetoothGattCharacteristic().getUuid().toString()
			         .equalsIgnoreCase(GattAttributes.BLE_PIN))
			{
				mGattPin = GattPin.initialize(event.getBluetoothGattCharacteristic().getValue());
				dismissProgressDialog();
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString()
			              .equalsIgnoreCase(GattAttributes.ZONE_CONFIGURATION_STATUS))
			{
				if (!mByteBufferZoneConfigInProgress
						     .equals(event.getBluetoothGattCharacteristic().getValue()))
				{
					pairingSuccess();
				}
			}
		}
		else if (event.getAction() == BLEAction.ACTION_DATA_WROTE &&
		         event.getBluetoothGattCharacteristic() != null)
		{
			if (event.getBluetoothGattCharacteristic().getUuid().toString()
			         .equalsIgnoreCase(GattAttributes.ZONE_CONFIGURATION_STATUS))
			{
				if (needsLinkKey())
				{
					mBLEProvisionService
							.writeCharacteristic(GattAttributes.BLE_LINK_KEY, EncryptionPrefs
									                                                  .getBLELinkKey());
				}
				else
				{
					mBLEProvisionService
							.writeCharacteristic(GattAttributes.ROOM_NAME, mBLERoomNameBuffer);
				}
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString()
			              .equalsIgnoreCase(GattAttributes.BLE_LINK_KEY))
			{
				mBLEProvisionService
						.writeCharacteristic(GattAttributes.ROOM_NAME, mBLERoomNameBuffer);
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString()
			              .equalsIgnoreCase(GattAttributes.ROOM_NAME))
			{
				mBLEProvisionService
						.writeCharacteristic(GattAttributes.LW_MESH_PAIRING_ADDRESS, mBLEAddressBuffer);
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString()
			              .equalsIgnoreCase(GattAttributes.LW_MESH_PAIRING_ADDRESS))
			{
				mBLEProvisionService
						.writeCharacteristic(GattAttributes.LW_MESH_SECURITY_KEY, EncryptionPrefs
								                                                          .getEncryptionKey());
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString()
			              .equalsIgnoreCase(GattAttributes.LW_MESH_SECURITY_KEY))
			{
				mBLEProvisionService
						.writeCharacteristic(GattAttributes.FIRMWARE_SIGNATURE_KEY, EncryptionPrefs
								                                                            .getFirmwareSignatureKey());
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString()
			              .equalsIgnoreCase(GattAttributes.FIRMWARE_SIGNATURE_KEY))
			{
				byte[] crc = null;
				if (needsLinkKey())
				{
					crc = ByteArrayUtils.addBytes(EncryptionPrefs
							                              .getBLELinkKey(), mBLERoomNameBuffer, mBLEAddressBuffer, EncryptionPrefs
									                                                                                       .getEncryptionKey(), EncryptionPrefs
											                                                                                                            .getFirmwareSignatureKey(), mByteBufferZoneConfigInProgress);
				}
				else
				{
					crc = ByteArrayUtils
							      .addBytes(mBLERoomNameBuffer, mBLEAddressBuffer, EncryptionPrefs
									                                                       .getEncryptionKey(), EncryptionPrefs
											                                                                            .getFirmwareSignatureKey(), mByteBufferZoneConfigInProgress);
				}
				mBLEProvisionService.writeCharacteristic(GattAttributes.CRC, ByteArrayUtils
						                                                             .bigToLittleEndian(ByteArrayUtils
								                                                                                .computeCrc(crc)));
			}
			else if (event.getBluetoothGattCharacteristic().getUuid().toString()
			              .equalsIgnoreCase(GattAttributes.CRC))
			{
				mBLEProvisionService.readCharacteristic(GattAttributes.ZONE_CONFIGURATION_STATUS);
			}
		}
	}
	
	
	private void pairingSuccess()
	{
		FragmentBLEDevicePin.this.getActivity().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				
				SmartNodeBLL.addSmartNodeAndSeed(mPairingAddress, mName);
				dismissProgressDialog();
				Toast.makeText(FragmentBLEDevicePin.this
						               .getActivity(), "Pairing Success!", Toast.LENGTH_LONG)
				     .show();
				removeDialogFragment(FragmentDeviceScan.ID);
				removeDialogFragment(FragmentBLEDevicePin.ID);
				showDialogFragment(LightingZoneProfileFragment
						                   .newInstance(mPairingAddress, mName, mFloorName), LightingZoneProfileFragment.ID);
			}
		});
	}
	
	
	private boolean needsLinkKey()
	{
		return !mDevice.getName().equalsIgnoreCase(SerialConsts.SMART_STAT_NAME);
	}
}
	
