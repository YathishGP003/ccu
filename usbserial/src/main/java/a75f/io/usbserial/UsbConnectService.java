package a75f.io.usbserial;

import static com.felhr.usbserial.UsbSerialDevice.TAG_SERIAL_DEBUG;
import static a75f.io.usbserial.UsbModbusService.byteArrayToHex;
import static a75f.io.usbserial.UsbService.ACTION_USB_PERMISSION_GRANTED;
import static a75f.io.usbserial.UsbService.ACTION_USB_PERMISSION_NOT_GRANTED;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

/**
 * Usb service for managing connect module interface. This is a copy of existing CM Usb service, which
 * directly reads/writes to interface 0. This service instead would open and work with interface 2.
 *
 * Interface 2 on CM USB device (VID- 03EB, PID- 2404) expected to be the Connect module.
 * UsbInterface[mId=2,mAlternateSetting=0,mName=null,mClass=2,mSubclass=2,mProtocol=1,mEndpoints=[
 *
 */
public class UsbConnectService extends Service
{
	private static final String  TAG  = "CCU_USB_CONNECT";

	public static final byte ESC_BYTE = (byte) 0xD9;
	public static final byte SOF_BYTE = 0x00;
	public static final byte EOF_BYTE = 0x03;

	public static final  String  ACTION_USB_READY                  =
			"com.felhr.connectivityservices.USB_READY";
	public static final  String  ACTION_USB_ATTACHED               =
			"android.hardware.usb.action.USB_DEVICE_ATTACHED";
	public static final  String  ACTION_USB_DETACHED               =
			"android.hardware.usb.action.USB_DEVICE_DETACHED";
	public static final  String  ACTION_USB_NOT_SUPPORTED          =
			"com.felhr.usbservice.USB_NOT_SUPPORTED";
	public static final  String  ACTION_NO_USB                     = "com.felhr.usbservice.NO_USB";
	public static final  String  ACTION_USB_CONNECT_PERMISSION_GRANTED     =
			"com.felhr.usbconnectservice.USB_PERMISSION_GRANTED";
	public static final  String  ACTION_USB_CONNECT_PERMISSION_NOT_GRANTED =
			"com.felhr.usbconnectservice.USB_PERMISSION_NOT_GRANTED";
	public static final  String  ACTION_USB_CONNECT_DISCONNECTED           =
			"com.felhr.usbconnectservice.USB_DISCONNECTED";
	public static final  String  ACTION_CDC_DRIVER_NOT_WORKING     =
			"com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
	public static final  String  ACTION_USB_DEVICE_NOT_WORKING     =
			"com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
	public static final  int     CTS_CHANGE                        = 1;
	public static final  int     DSR_CHANGE                        = 2;
	private static final String  ACTION_USB_PERMISSION             =
			"com.android.example.USB_PERMISSION";
	private static final int     BAUD_RATE                         = 9600;
	// BaudRate. Change this value if you need

	private static final boolean PARSE_DEBUG                       = false;
	public static        boolean SERVICE_CONNECTED                 = false;
	SerialState curState = SerialState.PARSE_INIT;
	int         nCRC     = 0;
	int nCurIndex;
	//NOTE: reduced buffer size to 256
	byte[] inDataBuffer = new byte[256];
	int    nDataLength  = 0;
	private IBinder binder = new UsbBinder();
	private Context             context;
	private Handler             mHandler;
	private UsbManager          usbManager;
	private UsbDevice           device;
	private UsbDeviceConnection connection;
	private UsbSerialDevice     serialPort;
	private volatile boolean             serialPortConnected;
	
	private int   reconnectCounter = 0;
	private Timer usbPortScanTimer = new Timer();
	
	/*
	 * Different notifications from OS will be received here (USB attached, detached, permission responses...)
	 * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
	 */
	private final BroadcastReceiver                  usbReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context arg0, Intent arg1)
		{
			if (arg1.getAction().equals(ACTION_USB_PERMISSION)) {
				CcuLog.d(TAG,"OnReceive == "+arg1.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false));
				boolean granted = arg1.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
				if (granted) {
					// User accepted our USB connection. Try to open the device as a serial port
					Intent intent = new Intent(ACTION_USB_CONNECT_PERMISSION_GRANTED);
					arg0.sendBroadcast(intent);

				} else {
					// User not accepted our USB connection. Send an Intent to the Main Activity
				
					CcuLog.d(TAG,"USB PERMISSION NOT GRANTED == "+arg1.getAction());
					Intent intent = new Intent(ACTION_USB_CONNECT_PERMISSION_NOT_GRANTED);
					arg0.sendBroadcast(intent);
				}
			} else if (arg1.getAction().equals(ACTION_USB_ATTACHED)) {
				UsbDevice attachedDevice = arg1.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (!serialPortConnected && UsbSerialUtil.isCMDevice(attachedDevice, context)) {
					CcuLog.d(TAG,"CM Serial device connected "+attachedDevice.toString());
					scheduleUsbConnectedEvent(); // A USB device has been attached. Try to open it as a Serial port
				}
				ConnectSerialPort portSelection = ConnectSerialPort.values()[UsbSerialUtil.getPreferredConnectModuleSerialType(context)];
				if ((portSelection == ConnectSerialPort.CCU_PORT && UsbSerialUtil.isConnectDevice(attachedDevice, context))
						|| (portSelection == ConnectSerialPort.CM_VIRTUAL_PORT2 && UsbSerialUtil.isCMDevice(attachedDevice, context))) {
					int vendorId = attachedDevice.getVendorId();
					int productId = attachedDevice.getProductId();
					String manufacturerName = attachedDevice.getManufacturerName();
					String productName = attachedDevice.getProductName();
					String version = attachedDevice.getVersion();
					String serialNumber = attachedDevice.getSerialNumber();
					String logMessage = String.format(
							"Connect Module is Attached - VID: %d, PID: %d, Manufacturer: %s, Product Name: %s, Version: %s, Serial Number: %s",
							vendorId, productId, manufacturerName, productName, version, serialNumber
					);
					UsbUtil.writeUsbEvent(logMessage);
				}
			} else if (arg1.getAction().equals(ACTION_USB_DETACHED)) {
				UsbDevice detachedDevice = arg1.getParcelableExtra(UsbManager.EXTRA_DEVICE);

				ConnectSerialPort portSelection = ConnectSerialPort.values()[UsbSerialUtil.getPreferredConnectModuleSerialType(context)];
				if ((portSelection == ConnectSerialPort.CCU_PORT && UsbSerialUtil.isConnectDevice(detachedDevice, context))
					|| (portSelection == ConnectSerialPort.CM_VIRTUAL_PORT2 && UsbSerialUtil.isCMDevice(detachedDevice, context))) {
					CcuLog.d(TAG,"Connect Serial device disconnected "+detachedDevice.toString());
					usbPortScanTimer.cancel();
					if (serialPortConnected) {
						serialPort.close();
						serialPort = null;
					}
					serialPortConnected = false;
					// Usb device was disconnected. send an intent to the Main Activity
					Intent intent = new Intent(ACTION_USB_CONNECT_DISCONNECTED);
					arg0.sendBroadcast(intent);
					int vendorId = detachedDevice.getVendorId();
					int productId = detachedDevice.getProductId();
					String manufacturerName = detachedDevice.getManufacturerName();
					String productName = detachedDevice.getProductName();
					String version = detachedDevice.getVersion();
					String serialNumber = detachedDevice.getSerialNumber();
					String logMessage = String.format(
							"Connect Module is Detached - VID: %d, PID: %d, Manufacturer: %s, Product Name: %s, Version: %s, Serial Number: %s",
							vendorId, productId, manufacturerName, productName, version, serialNumber
					);
					UsbUtil.writeUsbEvent(logMessage);
				}
			}
			CcuLog.d(TAG,"UsbService: OnReceive == "+arg1.getAction()+","+serialPortConnected);
		}
	};
	
	/**
	 * Device scan is processed on a timer thread. Also a delayed scan ensures that there is only one scan
	 * performed for multiple events.
	 */
	private void scheduleUsbConnectedEvent() {
		usbPortScanTimer.cancel();
		usbPortScanTimer = new Timer();
		usbPortScanTimer.schedule(new TimerTask() {
			@Override public void run() {
				findSerialPortDevice();
			}
		}, 500);
	}
	
	/*
	 *  Data received from serial port will be received here. Just populate onReceivedData with your code
	 *  In this particular example. byte stream is converted to String and send to UI thread to
	 *  be treated there.
	 */
	private UsbSerialInterface.UsbReadCallback mCallback   =
			new UsbSerialInterface.UsbReadCallback()
			{
				@Override
				public void onReceivedData(byte[] data, int mLength) {
					if (data.length > 0)
					{
						int nMsg;
						try {
							nMsg = (data[0] & 0xff);
						} catch (ArrayIndexOutOfBoundsException e) {
							CcuLog.e(TAG_SERIAL_DEBUG,
									"Bad message type received: " + String.valueOf(data[0] & 0xff) +
											e.getMessage());
							return;
						}
						if (data.length < 3) {
							return; //We need minimum bytes atleast 3 with msg and fsv address causing crash for WRM Pairing
						}

						messageToClients(Arrays.copyOfRange(data, 0, mLength));
						//parseBytes(data);
					}
				}
			};
	/*
	 * State changes in the CTS line will be received here
	 */
	private       UsbSerialInterface.UsbCTSCallback  ctsCallback =
			new UsbSerialInterface.UsbCTSCallback()
			{
				@Override
				public void onCTSChanged(boolean state)
				{
					if (mHandler != null)
					{
						mHandler.obtainMessage(CTS_CHANGE).sendToTarget();
					}
				}
			};
	/*
	 * State changes in the DSR line will be received here
	 */
	private       UsbSerialInterface.UsbDSRCallback  dsrCallback =
			new UsbSerialInterface.UsbDSRCallback()
			{
				@Override
				public void onDSRChanged(boolean state)
				{
					if (mHandler != null)
					{
						mHandler.obtainMessage(DSR_CHANGE).sendToTarget();
					}
				}
			};


	private void messageToClients(byte[] data)
	{
		CcuLog.d(TAG, "messageToClients: " + Arrays.toString(data));
		SerialAction serialAction = SerialAction.MESSAGE_FROM_CM_CONNECT_PORT;
		SerialEvent serialEvent = new SerialEvent(serialAction, data);
		EventBus.getDefault().post(serialEvent);
	}


	/*
	 * onCreate will be executed when service is started. It configures an IntentFilter to listen for
	 * incoming Intents (USB ATTACHED, USB DETACHED...) and it tries to open a serial port.
	 */
	@Override
	public void onCreate()
	{
		this.context = this;
		serialPortConnected = false;
		UsbConnectService.SERVICE_CONNECTED = true;
		setFilter();
		usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		
		try {
			findSerialPortDevice();
		} catch (SecurityException e) {
			//Android throws SecurityException if the application process is not granted android.permission.MANAGE_USB
			CcuLog.e(TAG, "USB Security Exception", e);
			Intent intent = new Intent(UsbServiceActions.ACTION_USB_PRIV_APP_PERMISSION_DENIED);
			UsbConnectService.this.getApplicationContext().sendBroadcast(intent);
		}

		running.start();
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		return Service.START_NOT_STICKY;
	}


	@Override
	public void onDestroy()
	{
		super.onDestroy();
		UsbConnectService.SERVICE_CONNECTED = false;
	}


	/* MUST READ about services
	 * http://developer.android.com/guide/components/services.html
	 * http://developer.android.com/guide/components/bound-services.html
	 */
	@Override
	public IBinder onBind(Intent intent)
	{
		return binder;
	}


	private void setFilter()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(ACTION_USB_DETACHED);
		filter.addAction(ACTION_USB_ATTACHED);
		registerReceiver(usbReceiver, filter);
	}


	private void findSerialPortDevice() {
		// This snippet will try to open the first encountered usb device connected, excluding usb root hubs
		HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
		ConnectSerialPort portSelection = ConnectSerialPort.values()[UsbSerialUtil.getPreferredConnectModuleSerialType(context)];
		CcuLog.d(TAG,"findSerialPortDevice = "+usbDevices.size()+" portSelection `"+portSelection);
		if (!usbDevices.isEmpty()) {
			boolean keep = true;
			for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
				device = entry.getValue();
				if (portSelection == ConnectSerialPort.CCU_PORT) {
					CcuLog.d(TAG,"isConnectDevice = "+device);
					if (UsbSerialUtil.isConnectDevice(device, context)) {
						connection = usbManager.openDevice(device);
						handleUsbOpen(true);
						keep = true;
						CcuLog.d(TAG, "Opened Serial CCU-USB device instance " + device.getVendorId() );
					} else {
						connection = null;
						device = null;
					}
				} else if (portSelection == ConnectSerialPort.CM_VIRTUAL_PORT2) {
					if (UsbSerialUtil.isCMDevice(device, getApplicationContext())) {
						connection = usbManager.openDevice(device);
						handleUsbOpen(true);
						keep = true;
						CcuLog.d(TAG, "Opened Serial CM device instance for connect" + device.getVendorId() );
					} else {
						connection = null;
						device = null;
					}
				}
				sleep(100);
				if (!keep) {
					// There is no USB devices connected (but usb host were listed). Send an intent to MainActivity.
					Intent intent = new Intent(ACTION_NO_USB);
					sendBroadcast(intent);
				}
			}
		} else {
			// There is no USB devices connected. Send an intent to MainActivity
			Intent intent = new Intent(ACTION_NO_USB);
			sendBroadcast(intent);
		}
	}

	private void handleUsbOpen (boolean success) {
		if (success) {
			new ConnectionThread().start();
			Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
			context.getApplicationContext().sendBroadcast(intent);
		} else {
			Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
			context.getApplicationContext().sendBroadcast(intent);
		}
	}

	public ApplicationInfo getApplicationInfo()
	{
		PackageManager pm = getApplicationContext().getPackageManager();
		ApplicationInfo ai = null;
		try
		{
			ai = pm.getApplicationInfo("a75f.io.renatus", 0);
		}
		catch (PackageManager.NameNotFoundException e)
		{
			e.printStackTrace();
		}
		return ai;
	}


	/*
	 * Request user permission. The response will be received in the BroadcastReceiver
	 */
	private void requestUserPermission()
	{
		PendingIntent mPendingIntent =
				PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
		usbManager.requestPermission(device, mPendingIntent);
	}


	public void setDebug(boolean debug)
	{
		serialPort.debug(debug);
	}

	/************************************************************************************************************************/

	private final LinkedBlockingQueue<byte[]> messageQueue = new LinkedBlockingQueue<byte[]>();

	Thread running = new Thread() {
		@Override
		public void run() {
			super.run();
			byte[] data;

			while (true) {
				try {
					if (!serialPortConnected) {
						CcuLog.i(TAG, "Serial Port is not connected sleeping");
						if (reconnectCounter++ >= 30) {
							CcuLog.i(TAG, "findSerialPortDevice");
							findSerialPortDevice();
							reconnectCounter = 0;
						}
						
						sleep(2000);
						continue;
					}

					if (serialPort != null ) {
						data = messageQueue.poll(1, TimeUnit.SECONDS);
						if (data != null && data.length > 0) {
							CcuLog.i(TAG, "Write MB data : " + Arrays.toString(data) +" Hex : "+byteArrayToHex(data));
							serialPort.write(Arrays.copyOfRange(data, 0, data.length));
							Thread.sleep(50);
						}
					}
				} catch (Exception exception) {
					CcuLog.e(TAG, "Serial transaction failed: ", exception);
					exception.printStackTrace();
				}
			}

		}

	};

	/*
	 * This function will be called from MainActivity to write data through Serial Port
	 */
	public void write(byte[] data) {

		if(isConnected()) {
			messageQueue.add(data);
		}
		else
		{
			messageQueue.clear();
			CcuLog.i(TAG, "Serial is disconnected, message discarded");
		}
	}


	public void setHandler(Handler mHandler)
	{
		this.mHandler = mHandler;
	}

	public boolean isConnected() {
		return serialPortConnected;
	}


	private enum SerialState
	{
		PARSE_INIT, ESC_BYTE_RCVD, SOF_BYTE_RCVD, LEN_BYTE_RCVD, ESC_BYTE_IN_DATA_RCVD, CRC_RCVD,
		ESC_BYTE_AS_END_OF_PACKET_RCVD, BAD_PACKET, DATA_AVAILABLE
	}

	public class UsbBinder extends Binder
	{
		public UsbConnectService getService()
		{
			return UsbConnectService.this;
		}
	}

	/*
	 * A simple thread to open a serial port.
	 * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
	 */
	private class ConnectionThread extends Thread {
		@Override
		public void run() {
			try {
				configureSerialPort();
			} catch (Exception e) {
				//Unstable USB connections would result in configuration failures.
				CcuLog.e(TAG, "Connect: configureSerialPort Failed ", e);
				serialPortConnected = false;
			}
		}
	}
	
	private void configureSerialPort() {
		ConnectSerialPort portSelection = ConnectSerialPort.values()[UsbSerialUtil.getPreferredConnectModuleSerialType(context)];
        if (portSelection == ConnectSerialPort.NO_CONNECT_MODULE) {
            CcuLog.i(TAG, "Connect: configureSerialPort No Connect Module selected");
            return;
        }
		if (portSelection == ConnectSerialPort.CCU_PORT) {
			serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
		} else if (portSelection == ConnectSerialPort.CM_VIRTUAL_PORT2) {
			serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection, 3);
		}
		if (serialPort != null) {
			if (serialPort.open()) {
				setDebug(true);
				serialPortConnected = true;
				serialPort.setModbusDevice(true);
				UsbSerialWatchdog.getInstance().pet();
				serialPort.setBaudRate(BAUD_RATE);
				serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
				serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
				serialPort.setParity(UsbSerialInterface.PARITY_NONE);
				/**
				 * Current flow control Options:
				 * UsbSerialInterface.FLOW_CONTROL_OFF
				 * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
				 * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
				 */
				serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
				serialPort.read(mCallback);
				serialPort.getCTS(ctsCallback);
				serialPort.getDSR(dsrCallback);
				//
				// Some Arduinos would need some sleep because firmware wait some time to know whether a new sketch is going
				// to be uploaded or not
				sleep(2000); // sleep some. YMMV with different chips.
				// Everything went as expected. Send an intent to MainActivity
				Intent intent = new Intent(ACTION_USB_READY);
				context.sendBroadcast(intent);
				CcuLog.i(TAG, "Connect: configureSerialPort configured");
			} else {
				CcuLog.i(TAG, "Connect: configureSerialPort Open Failed!");
				// Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
				// Send an Intent to Main Activity
				Intent intent;
				if (serialPort instanceof CDCSerialDevice) {
					intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
				} else {
					intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
				}
				context.sendBroadcast(intent);
			}
		} else {
			CcuLog.e(TAG, "CM Connect: configureSerialPort Failed ");
			// No driver for given device, even generic CDC driver could not be loaded
			Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
			context.sendBroadcast(intent);
		}
	}

	public void sleep(long millis){
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

