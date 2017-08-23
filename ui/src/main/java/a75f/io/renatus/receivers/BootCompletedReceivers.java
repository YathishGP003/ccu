package a75f.io.renatus.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Yinten on 8/22/2017.
 */

public class BootCompletedReceivers extends BroadcastReceiver
{
	private static final String TAG = BootCompletedReceivers.class.getSimpleName();
	
	
	public void onReceive(Context context, Intent intent)
	{
		Log.i(TAG, "Boot Completed Intent Reciever");
		String action = intent.getAction();
		if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED))
		{
			try
			{
				removeUSBPermissionDialogs(context);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	
	private void removeUSBPermissionDialogs(Context context)
	{
		PackageManager pm = context.getPackageManager();
		ApplicationInfo ai = null;
		try
		{
			ai = pm.getApplicationInfo("a75f.io.renatus", 0);
		}
		catch (PackageManager.NameNotFoundException e)
		{
			e.printStackTrace();
		}
		if (ai != null)
		{
			Log.i(TAG, "Trying to grant permissions");
			UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
			IBinder b = ServiceManager.getService(Context.USB_SERVICE);
			IUsbManager service = IUsbManager.Stub.asInterface(b);
			HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
			Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
			while (deviceIterator.hasNext())
			{
				Log.i(TAG, "Granting permissions");
				UsbDevice device = deviceIterator.next();
				try
				{
					service.grantDevicePermission(device, ai.uid);
				}
				catch (RemoteException e)
				{
					e.printStackTrace();
				}
				try
				{
					service.setDevicePackage(device, "a75f.io.renatus", ai.uid);
				}
				catch (RemoteException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}