package a75f.io.renatus.BASE;

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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.bo.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.bo.serial.SnToCmOverAirSnRegularUpdateMessage_t;
import a75f.io.logic.SerialBLL;
import a75f.io.renatus.BLE.BLEHomeFragment;
import a75f.io.renatus.MainActivity;
import a75f.io.bo.serial.comm.SerialAction;
import a75f.io.bo.serial.comm.SerialEvent;
import a75f.io.usbserial.UsbService;


/**
 * Created by Yinten on 8/21/2017.
 *
 * This class will be the base serial activity that will connect to the serial BLL that will
 * control eventing around the application.
 *
 * Currently, there will be an annoyance with a popup asking for permissions.
 *
 * That is a TODO task that will need to be dealt with soon.
 *
 *
 */

public class BaseSerialAppCompatActivity extends AppCompatActivity
{
	
	
	
	private static final String            TAG          = BLEHomeFragment.class.getSimpleName();
	/*
   * Notifications from UsbService will be received here.
   */
	private final        BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
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
	}
	
	private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras)
	{
		if (!UsbService.SERVICE_CONNECTED)
		{
			Intent startService = new Intent(BaseSerialAppCompatActivity.this, service);
			if (extras != null && !extras.isEmpty())
			{
				Set<String> keys = extras.keySet();
				for (String key : keys)
				{
					String extra = extras.getString(key);
					startService.putExtra(key, extra);
				}
			}
			BaseSerialAppCompatActivity.this.startService(startService);
		}
		Intent bindingIntent = new Intent(BaseSerialAppCompatActivity.this, service);
		BaseSerialAppCompatActivity.this.bindService(bindingIntent, serviceConnection, Context
				                                                                          .BIND_AUTO_CREATE);
	}
	
	// Called in a separate thread
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onSerialEvent(SerialEvent event)
	{
		SerialBLL.handleSerialEvent(event);
		Log.i(TAG, "Event Type: " + event.getSerialAction().name());
		if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_PORT)
		{
			byte[] data = (byte[]) event.getBytes();
			Log.i(TAG, "Data return size: " + data.length);
			MessageType messageType = MessageType.values()[(event.getBytes()[0] & 0xff)];
			String pojoAsString = null;
			Log.i(TAG, "Message Type: " + messageType.name());
			if (messageType == MessageType.CM_REGULAR_UPDATE)
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
			else if (messageType == MessageType.CM_TO_CCU_OVER_USB_SN_REGULAR_UPDATE)
			{
				CmToCcuOverUsbSnRegularUpdateMessage_t smartNodeRegularUpdateMessage_t = new CmToCcuOverUsbSnRegularUpdateMessage_t();
				Log.i(TAG, "CmToCcuOverUsbSnRegularUpdateMessage_t size: " + smartNodeRegularUpdateMessage_t.size());
				Log.i(TAG, "Buffer size with smart node regular update message: " + data.length);
				Log.i(TAG, "Size of inner struct SnToCmOverAirSnRegularUpdateMessage_t: " + new SnToCmOverAirSnRegularUpdateMessage_t().size());
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
		BaseSerialAppCompatActivity.this.unregisterReceiver(mUsbReceiver);
		BaseSerialAppCompatActivity.this.unbindService(usbConnection);
	}
	
	
	private void setFilters()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
		filter.addAction(UsbService.ACTION_NO_USB);
		filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
		filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
		filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
		registerReceiver(mUsbReceiver, filter);
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
