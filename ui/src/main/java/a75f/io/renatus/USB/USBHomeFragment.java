package a75f.io.renatus.USB;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.Set;

import a75f.io.bo.SmartNode;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.renatus.BLE.BLEHomeFragment;
import a75f.io.renatus.MainActivity;
import a75f.io.renatus.R;
import a75f.io.usbserial.SerialAction;
import a75f.io.usbserial.SerialEvent;
import a75f.io.usbserial.UsbService;
import a75f.io.util.Globals;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ryanmattison on 7/27/17.
 */

public class USBHomeFragment extends DialogFragment
{
	
	private static final String TAG = BLEHomeFragment.class.getSimpleName();
	/*
   * Notifications from UsbService will be received here.
   */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			switch (intent.getAction())
			{
				case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
					Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
					break;
				case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
					Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
					break;
				case UsbService.ACTION_NO_USB: // NO USB CONNECTED
					Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
					break;
				case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
					Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
					break;
				case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
					Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
					break;
			}
		}
	};
	@BindView(R.id.fragment_usb_button)
	Button mUSBButton;
	
	@BindView(R.id.fragment_light_button)
	Button mLightButton;
	private UsbService usbService;
	private MyHandler  mHandler;
	private final ServiceConnection usbConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1)
		{
			usbService = ((UsbService.UsbBinder) arg1).getService();
			usbService.setHandler(mHandler);
		}
		
		
		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			usbService = null;
		}
	};
	
	
	public static USBHomeFragment getInstance()
	{
		return new USBHomeFragment();
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View retVal = inflater.inflate(R.layout.fragment_usb, container, false);
		ButterKnife.bind(this, retVal);
		return retVal;
	}


	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
	}


	@Override
	public void onResume()
	{
		super.onResume();
		setFilters();  // Start listening notifications from UsbService
		startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
	}
	
	
	@Override
	public void onPause()
	{
		super.onPause();
		getActivity().unregisterReceiver(mUsbReceiver);
		getActivity().unbindService(usbConnection);
	}
	
	
	private void setFilters()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
		filter.addAction(UsbService.ACTION_NO_USB);
		filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
		filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
		filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
		getActivity().registerReceiver(mUsbReceiver, filter);
	}
	
	
	private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras)
	{
		if (!UsbService.SERVICE_CONNECTED)
		{
			Intent startService = new Intent(getActivity(), service);
			if (extras != null && !extras.isEmpty())
			{
				Set<String> keys = extras.keySet();
				for (String key : keys)
				{
					String extra = extras.getString(key);
					startService.putExtra(key, extra);
				}
			}
			getActivity().startService(startService);
		}
		Intent bindingIntent = new Intent(this.getActivity(), service);
		getActivity().bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
	}
	
	
	@Override
	public void onStop()
	{
		super.onStop();
		EventBus.getDefault().unregister(this);
	}
	
	
	@Override
	public void onStart()
	{
		super.onStart();
		EventBus.getDefault().register(this);
		Dialog dialog = getDialog();
		if (dialog != null)
		{
			int width = ViewGroup.LayoutParams.MATCH_PARENT;
			int height = ViewGroup.LayoutParams.MATCH_PARENT;
			dialog.getWindow().setLayout(width, height);
		}
	}
	
	
	// Called in a separate thread
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onBLEEvent(SerialEvent event)
	{
		Log.i(TAG, "Event Type: " + event.getSerialAction().name());
		if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_PORT)
		{
			byte[] data = (byte[]) event.getBytes();
			Log.i(TAG, "Data return size: " + data.length);
		}
	}
	
	
	@OnClick(R.id.fragment_light_button)
	public void usbLight()
	{
		if (usbService != null)
		{
			usbService.setDebug(true);
			mLightButton.setText("Send ordered buffer!");
			SmartNode sn = Globals.getInstance().getSmartNode();
			CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
			seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
			//        try {
			//            seedMessage.encryptionKey.read(new ByteArrayInputStream(EncryptionPrefs.getEncryptionKey()));
			//        } catch (IOException e) {
			//            e.printStackTrace();
			//        }
			seedMessage.settings.roomName.set(Globals.getInstance().getSmartNode().getName());
			seedMessage.smartNodeAddress.set(Globals.getInstance().getSmartNode().getMeshAddress());
			//seedMessage.controls.time.day.set((short) 1);
			//seedMessage.controls.time.hours.set((short) 1);
			//seedMessage.controls.time.minutes.set((short) 1);
			//seedMessage.controls.digitalOut1.set(1);
			//seedMessage.settings.lightingControl.set(1);
			//seedMessage.settings.ledBitmap.digitalOut2.set(1);
			seedMessage.controls.digitalOut1.set((short) 1);
			usbService.write(seedMessage.getOrderedBuffer());
			//usbService.write(seedMessage.getOrderedBuffer());
		}
		else
		{
			Toast.makeText(USBHomeFragment.this.getContext(), "USB Service not connected", Toast.LENGTH_SHORT).show();
		}
	}
	
	
	@OnClick(R.id.fragment_usb_button)
	public void usbSubmit()
	{
		Log.i(TAG, "Done");
		usbService.setDebug(true);
		mUSBButton.setText("Send Array bytes");
		CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
		seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
		seedMessage.smartNodeAddress.set(2000);
		seedMessage.controls.time.day.set((short) 1);
		seedMessage.controls.time.hours.set((short) 1);
		seedMessage.controls.time.minutes.set((short) 1);
		seedMessage.controls.digitalOut1.set((short) 1);
		seedMessage.settings.ledBitmap.digitalOut1.set((short) 1);
		if (usbService != null)
		{ // if UsbService was correctly binded, Send data
			usbService.write(seedMessage.getByteBuffer().array());
			//usbService.write(seedMessage.getOrderedBuffer());
		}
	}
	
	/*
	 * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
	 */
	private static class MyHandler extends Handler
	{
		private final WeakReference<MainActivity> mActivity;
		
		
		public MyHandler(MainActivity activity)
		{
			mActivity = new WeakReference<>(activity);
		}
		
		
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case UsbService.CTS_CHANGE:
					Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
					break;
				case UsbService.DSR_CHANGE:
					Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
					break;
			}
		}
	}
}
