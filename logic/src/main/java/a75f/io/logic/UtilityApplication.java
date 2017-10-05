package a75f.io.logic;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Set;

import a75f.io.bo.serial.comm.SerialEvent;
import a75f.io.usbserial.UsbService;

/**
 * Created by rmatt isOn 7/19/2017.
 */

public abstract class UtilityApplication extends Application
{
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			switch (intent.getAction())
			{
				case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
					Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
					// HeartBeatJob.scheduleJob();
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
	private final ServiceConnection usbConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1)
		{
			usbService = ((UsbService.UsbBinder) arg1).getService();
			LSerial.getInstance().setUSBService(usbService);
			//TODO: research what cts and dsr changes are.  For now no handler will be used, because I'm uncertain if the information is relevant.
			usbService.setHandler(null);
		}
		
		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			usbService = null;
		}
	};
	@Override
	public void onCreate()
	{
		super.onCreate();
		Globals.getInstance().setApplicationContext(this);
		setFilters();  // Start listening notifications from UsbService
		startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
		EventBus.getDefault().register(this);
		
		
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
	private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras)
	{
		if (!UsbService.SERVICE_CONNECTED)
		{
			Intent startService = new Intent(this, service);
			if (extras != null && !extras.isEmpty())
			{
				Set<String> keys = extras.keySet();
				for (String key : keys)
				{
					String extra = extras.getString(key);
					startService.putExtra(key, extra);
				}
			}
			this.startService(startService);
		}
		Intent bindingIntent = new Intent(this, service);
		bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
	}
	@Override
	public void onTerminate()
	{
		EventBus.getDefault().unregister(this);
		unregisterReceiver(mUsbReceiver);
		unbindService(usbConnection);
		super.onTerminate();
	}
	
	// Called in a separate thread
	@Subscribe(threadMode = ThreadMode.ASYNC)
	public void onSerialEvent(SerialEvent event)
	{
		LSerial.handleSerialEvent(event);
	}
	
	
}
