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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.UUID;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.Output;
import a75f.io.bo.building.OutputAnalogActuatorType;
import a75f.io.bo.building.SmartNode;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.bo.serial.SnToCmOverAirSnRegularUpdateMessage_t;
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
		mUSBButton.setText("Light Off");
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
	public void onSerialEvent(SerialEvent event)
	{
		Log.i(TAG, "Event Type: " + event.getSerialAction().name());
		if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_PORT)
		{
			byte[] data = (byte[]) event.getBytes();
			Log.i(TAG, "Data return size: " + data.length);
			
			MessageType messageType = MessageType.values()[(event.getBytes()[0] & 0xff)];
			String pojoAsString = null;
			Log.i(TAG, "Message Type: " + messageType.name());
			if(messageType == MessageType.CM_REGULAR_UPDATE)
			{
				
				CmToCcuOverUsbCmRegularUpdateMessage_t regularUpdateMessage_t = new CmToCcuOverUsbCmRegularUpdateMessage_t();
				regularUpdateMessage_t.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
				Log.i(TAG, "Regular Update Message: " + regularUpdateMessage_t.toString());
			
				try
				{
					pojoAsString = JsonSerializer.toJson(regularUpdateMessage_t, true);
					System.out.println("POJO as string:\n" + pojoAsString + "\n");
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			else if(messageType == MessageType.CM_TO_CCU_OVER_USB_SN_REGULAR_UPDATE)
			{
				CmToCcuOverUsbSnRegularUpdateMessage_t smartNodeRegularUpdateMessage_t = new CmToCcuOverUsbSnRegularUpdateMessage_t();
				Log.i(TAG, "CmToCcuOverUsbSnRegularUpdateMessage_t size: " + smartNodeRegularUpdateMessage_t.size());
				Log.i(TAG, "Buffer size with smart node regular update message: " + data.length);
				Log.i(TAG, "Size of inner struct SnToCmOverAirSnRegularUpdateMessage_t: " +  new SnToCmOverAirSnRegularUpdateMessage_t().size());
				smartNodeRegularUpdateMessage_t.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
				Log.i(TAG, "Smart Node Regular Update Message: " + smartNodeRegularUpdateMessage_t.toString());
				try
				{
					pojoAsString = JsonSerializer.toJson(smartNodeRegularUpdateMessage_t, true);
					System.out.println("POJO as string:\n" + pojoAsString + "\n");
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				
			}
			
		}
	}
	
	
	@OnClick(R.id.fragment_light_button)
	public void usbLight()
	{
		if (usbService != null)
		{
			CCUApplication ccuApplication = Globals.getInstance().getCCUApplication();
			usbService.setDebug(true);
			mLightButton.setText("Send ordered buffer!");
			if(ccuApplication.zones.size() == 0)
			{
				UUID analog15kUUID = UUID.randomUUID();
				SmartNode smartNode = ccuApplication.smartNodes.get(0);
				smartNode.analog1OutId = analog15kUUID;
				Zone zone5K = new Zone();
				zone5K.roomName = "5000 test zone";
				LightProfile lightProfile5K = new LightProfile();
				zone5K.zoneProfiles.add(lightProfile5K);
				ccuApplication.zones.add(zone5K);
				SmartNodeOutput smartNodeOutput5K = new SmartNodeOutput();
				smartNodeOutput5K.uniqueID = analog15kUUID;
				smartNodeOutput5K.outputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
				smartNodeOutput5K.output = Output.Analog;
				smartNodeOutput5K.name = "Dining Room";
				lightProfile5K.smartNodeOutputs.add(smartNodeOutput5K);
				
				
			}
			
			try
			{
				String ccuApplicationJSON = JsonSerializer.toJson(ccuApplication, true);
				System.out.println("CCU Application As String:\n" + ccuApplicationJSON + "\n");
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			SmartNode smartNode = ccuApplication.findSmartNodeByIOUUID(ccuApplication.zones.get(0).zoneProfiles.get(0).smartNodeOutputs.get(0).uniqueID);
			CcuToCmOverUsbDatabaseSeedSnMessage_t ccuToCmOverUsbDatabaseSeedSnMessage_t = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
			ccuToCmOverUsbDatabaseSeedSnMessage_t.smartNodeAddress.set(smartNode.address);
			ccuToCmOverUsbDatabaseSeedSnMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
			//ccuToCmOverUsbDatabaseSeedSnMessage_t.putEncrptionKey(Encryp);
			ZoneProfile zoneProfile = ccuApplication.zones.get(0).zoneProfiles.get(0);
			ccuToCmOverUsbDatabaseSeedSnMessage_t.controls.analogOut1.set((short) 100);
			
			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.profileBitmap.lightingControl.set((short) 1);
			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.ledBitmap.analogIn1.set((short)1);
			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.lightingIntensityForOccupantDetected.set((short) 100);
			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.minLightingControlOverrideTimeInMinutes.set((short)1);
			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.roomName.set(smartNode.roomName);
			try
			{
				String ccuToCmOverUsbDatabaseSeedSnMessageJSON = JsonSerializer.toJson(ccuToCmOverUsbDatabaseSeedSnMessage_t, true);
				System.out.println("CCU Application As String:\n" + ccuToCmOverUsbDatabaseSeedSnMessageJSON + "\n");
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			usbService.write(ccuToCmOverUsbDatabaseSeedSnMessage_t.getOrderedBuffer());
			//usbService.write(seedMessage.getOrderedBuffer());
		}
		else
		{
			Toast.makeText(USBHomeFragment.this.getContext(), "USB Service not connected", Toast.LENGTH_SHORT).show();
		}
	}

	boolean lightOn = false;
	
	@OnClick(R.id.fragment_usb_button)
	public void usbSubmit()
	{
		if (usbService != null)
		{
			CCUApplication ccuApplication = Globals.getInstance().getCCUApplication();
			usbService.setDebug(true);
			mLightButton.setText("Send ordered buffer!");
			if(ccuApplication.zones.size() == 0)
			{
				
				
				UUID analog15kUUID = UUID.randomUUID();
				SmartNode smartNode = ccuApplication.smartNodes.get(0);
				smartNode.analog1OutId = analog15kUUID;
				Zone zone5K = new Zone();
				zone5K.roomName = "5000 test zone";
				LightProfile lightProfile5K = new LightProfile();
				zone5K.zoneProfiles.add(lightProfile5K);
				ccuApplication.zones.add(zone5K);
				SmartNodeOutput smartNodeOutput5K = new SmartNodeOutput();
				smartNodeOutput5K.uniqueID = analog15kUUID;
				smartNodeOutput5K.outputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
				smartNodeOutput5K.output = Output.Analog;
				smartNodeOutput5K.name = "Dining Room";
				lightProfile5K.smartNodeOutputs.add(smartNodeOutput5K);
				
				
			}
			
			try
			{
				String ccuApplicationJSON = JsonSerializer.toJson(ccuApplication, true);
				System.out.println("CCU Application As String:\n" + ccuApplicationJSON + "\n");
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			SmartNode smartNode = ccuApplication.findSmartNodeByIOUUID(ccuApplication.zones.get(0).zoneProfiles.get(0).smartNodeOutputs.get(0).uniqueID);
			CcuToCmOverUsbSnControlsMessage_t controlsMessage_t = new CcuToCmOverUsbSnControlsMessage_t();
			controlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
			controlsMessage_t.smartNodeAddress.set(smartNode.address);
			lightOn = !lightOn;
			controlsMessage_t.controls.analogOut1.set(lightOn ? (short)100 : (short) 0);
			try
			{
				String controlsMessage_tJSON = JsonSerializer.toJson(controlsMessage_t, true);
				System.out.println("controlsMessage_tJSON As String:\n" + controlsMessage_tJSON + "\n");
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			usbService.write(controlsMessage_t.getOrderedBuffer());
			
			
//			CcuToCmOverUsbDatabaseSeedSnMessage_t ccuToCmOverUsbDatabaseSeedSnMessage_t = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
//			ccuToCmOverUsbDatabaseSeedSnMessage_t.smartNodeAddress.set(smartNode.address);
//			ccuToCmOverUsbDatabaseSeedSnMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
//			//ccuToCmOverUsbDatabaseSeedSnMessage_t.putEncrptionKey(Encryp);
//			ZoneProfile zoneProfile = ccuApplication.zones.get(0).zoneProfiles.get(0);
//			ccuToCmOverUsbDatabaseSeedSnMessage_t.controls.analogOut1.set((short) 0);
//
//			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.profileBitmap.lightingControl.set((short) 1);
//			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.ledBitmap.analogIn1.set((short)1);
//			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.lightingIntensityForOccupantDetected.set((short) 0);
//			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.minLightingControlOverrideTimeInMinutes.set((short) 1);
//			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.roomName.set(smartNode.roomName);
			
			//usbService.write(seedMessage.getOrderedBuffer());
		}
		else
		{
			Toast.makeText(USBHomeFragment.this.getContext(), "USB Service not connected", Toast.LENGTH_SHORT).show();
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
