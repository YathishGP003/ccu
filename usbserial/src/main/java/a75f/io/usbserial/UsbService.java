package a75f.io.usbserial;

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
import android.util.Log;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import static a75f.io.usbserial.UsbUtils.DEVICE_ID_FTDI;

/**
 * Created by rmatt isOn 7/30/2017.
 */
public class UsbService extends Service
{
	private static final String  TAG  = "CCU_SERIAL";

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
	public static final  String  ACTION_USB_PERMISSION_GRANTED     =
			"com.felhr.usbservice.USB_PERMISSION_GRANTED";
	public static final  String  ACTION_USB_PERMISSION_NOT_GRANTED =
			"com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
	public static final  String  ACTION_USB_DISCONNECTED           =
			"com.felhr.usbservice.USB_DISCONNECTED";
	public static final  String  ACTION_CDC_DRIVER_NOT_WORKING     =
			"com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
	public static final  String  ACTION_USB_DEVICE_NOT_WORKING     =
			"com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
	public static final  int     CTS_CHANGE                        = 1;
	public static final  int     DSR_CHANGE                        = 2;
	private static final String  ACTION_USB_PERMISSION             =
			"com.android.example.USB_PERMISSION";
	private static final int     BAUD_RATE                         = 38400;
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
	private boolean             serialPortConnected;
	
	private int reconnectCounter = 0;
	
	/*
	 * Different notifications from OS will be received here (USB attached, detached, permission responses...)
	 * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
	 */
	private final BroadcastReceiver                  usbReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context arg0, Intent arg1)
		{
			Log.d("UsbService.java","OnReceive == "+arg1.getAction()+","+serialPortConnected);
			if (arg1.getAction().equals(ACTION_USB_PERMISSION))
			{
				Log.d("UsbService.java","OnReceive == "+arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED));
				boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
				if (granted) // User accepted our USB connection. Try to open the device as a serial port
				{
					Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
					arg0.sendBroadcast(intent);
					//connection = usbManager.openDevice(device);
					//new ConnectionThread().start();
				}
				else // User not accepted our USB connection. Send an Intent to the Main Activity
				{
					Log.d("UsbService.java","USB PERMISSION NOT GRANTED == "+arg1.getAction());
					Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
					arg0.sendBroadcast(intent);
				}
			}
			else if (arg1.getAction().equals(ACTION_USB_ATTACHED))
			{
				if (!serialPortConnected)
				{
					findSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port
				}
			}
			else if (arg1.getAction().equals(ACTION_USB_DETACHED))
			{

				// Usb device was disconnected. send an intent to the Main Activity
				Intent intent = new Intent(ACTION_USB_DISCONNECTED);
				arg0.sendBroadcast(intent);
				if (serialPortConnected)
				{
					serialPort.close();
				}
				serialPortConnected = false;
			}
		}
	};
	/*
	 *  Data received from serial port will be received here. Just populate onReceivedData with your code
	 *  In this particular example. byte stream is converted to String and send to UI thread to
	 *  be treated there.
	 */
	private       UsbSerialInterface.UsbReadCallback mCallback   =
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
							Log.d("SERIAL_DEBUG",
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


	private void parseBytes(byte[] inReg)
	{
		for (int nCount = 0; nCount < inReg.length; nCount++)
		{
			byte inData = inReg[nCount];
			switch (curState)
			{
				case PARSE_INIT:
					if (PARSE_DEBUG)
					{
						Log.i(TAG, "Parse INIT");
					}
					if (inData == ESC_BYTE)
					{
						curState = SerialState.ESC_BYTE_RCVD;
					}
					break;
				case ESC_BYTE_RCVD:
					if (PARSE_DEBUG)
					{
						Log.i(TAG, "ESC_BYTE_RCVD");
					}
					if (inData == SOF_BYTE)
					{
						curState = SerialState.SOF_BYTE_RCVD;
					}
					else
					{
						curState = SerialState.BAD_PACKET;
					}
					break;
				case SOF_BYTE_RCVD:
					if (PARSE_DEBUG)
					{
						Log.i(TAG, "SOF_BYTE_RCVD");
					}
					nDataLength = inData;
					Log.i(TAG, "Data Len: " + nDataLength);
					curState = SerialState.LEN_BYTE_RCVD;
					break;
				case LEN_BYTE_RCVD:
					if (PARSE_DEBUG)
					{
						Log.i(TAG, "LEN_BYTE_RCVD");
					}
					if (nCurIndex == nDataLength)
					{
						int nIncomingCRC = inData;
						if (nIncomingCRC == nCRC)
						{
							curState = SerialState.CRC_RCVD;
						}
					}
					else if (nCurIndex < nDataLength)
					{
						inDataBuffer[nCurIndex] = inData;
						nCRC ^= inData;
						nCurIndex++;
						if (inData == ESC_BYTE)
						{
							curState = SerialState.ESC_BYTE_IN_DATA_RCVD;
						}
					}
					else
					{
						curState = SerialState.BAD_PACKET;
					}
					break;
				case ESC_BYTE_IN_DATA_RCVD:
					if (PARSE_DEBUG)
					{
						Log.i(TAG, "END ESC_BYTE_IN_DATA_RCVD");
					}
					if (inData == ESC_BYTE)
					{
						curState = SerialState.LEN_BYTE_RCVD;
					}
					else
					{
						curState = SerialState.BAD_PACKET;
					}
					break;
				case CRC_RCVD:
					if (PARSE_DEBUG)
					{
						Log.i(TAG, "END CRC RCVD");
					}
					if (inData == ESC_BYTE)
					{
						curState = SerialState.ESC_BYTE_AS_END_OF_PACKET_RCVD;
					}
					else
					{
						curState = SerialState.BAD_PACKET;
					}
					break;
				case ESC_BYTE_AS_END_OF_PACKET_RCVD:
					if (PARSE_DEBUG)
					{
						Log.i(TAG, "case ESC_BYTE_AS_END_OF_PACKET_RCVD:");
					}
					if (inData == EOF_BYTE)
					{
						curState = SerialState.DATA_AVAILABLE;
					}
					else
					{
						curState = SerialState.BAD_PACKET;
					}
					break;
			}
			if (curState == SerialState.DATA_AVAILABLE)
			{
				if (PARSE_DEBUG)
				{
					Log.i(TAG, "DataAvailable sneding");
				}
				messageToClients(Arrays.copyOfRange(inDataBuffer, 0, nCurIndex));
				nCurIndex = 0;
				nCRC = 0;
				curState = SerialState.PARSE_INIT;
			}
			if (curState == SerialState.BAD_PACKET)
			{
				Log.d("SERIAL_RAW", "*******BAD PACKET RECEIVED*****");
				nCurIndex = 0;
				nCRC = 0;
				curState = SerialState.PARSE_INIT;
			}
		}
	}


	private void messageToClients(byte[] data)
	{
		SerialAction serialAction = SerialAction.MESSAGE_FROM_SERIAL_PORT;
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
		UsbService.SERVICE_CONNECTED = true;
		setFilter();
		usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		
		try {
			findSerialPortDevice();
		} catch (SecurityException e) {
			//Android throws SecurityException if the application process is not granted android.permission.MANAGE_USB
			Log.e(TAG, "USB Security Exception", e);
			Intent intent = new Intent(UsbServiceActions.ACTION_USB_PRIV_APP_PERMISSION_DENIED);
			UsbService.this.getApplicationContext().sendBroadcast(intent);
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
		UsbService.SERVICE_CONNECTED = false;
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


	private void findSerialPortDevice()
	{
		// This snippet will try to open the first encountered usb device connected, excluding usb root hubs
		HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
		Log.d(TAG,"findSerialPortDevce="+usbDevices.size());
		if (!usbDevices.isEmpty())
		{
			boolean keep = true;
			for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet())
			{
				device = entry.getValue();
				int deviceVID = device.getVendorId();
				int devicePID = device.getProductId();
				Log.i(TAG, "USB Device VID: " + deviceVID);
				Log.i(TAG, "USB Device PID: " + devicePID);
				if (deviceVID == 0x0403 || deviceVID == 1003 ||
						(deviceVID == DEVICE_ID_FTDI && UsbUtils.isBiskitMode(getApplicationContext())))
				{
					boolean success = grantRootPermissionToUSBDevice(device);
					connection = usbManager.openDevice(device);
					if (success)
					{
						new ConnectionThread().start();
						Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
						UsbService.this.getApplicationContext().sendBroadcast(intent);
						keep = true;
					}
					else
					{
						Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
						UsbService.this.getApplicationContext().sendBroadcast(intent);
						keep = false;
					}
					Log.d(TAG, "Opened Serial MODBUS device instance for "+deviceVID);
				}
				else
				{
					connection = null;
					device = null;
				}
				if (!keep)
				{
					break;
				}
			}
			if (!keep)
			{
				// There is no USB devices connected (but usb host were listed). Send an intent to MainActivity.
				Intent intent = new Intent(ACTION_NO_USB);
				sendBroadcast(intent);
			}
		}
		else
		{
			// There is no USB devices connected. Send an intent to MainActivity
			Intent intent = new Intent(ACTION_NO_USB);
			sendBroadcast(intent);
		}
	}
	
	/**
	 * Scans and initializes serial ports without sending broadcast messages.
	 */
	private void scanSerialPortSilentlyForCmDevice() {
		
		HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
		Log.d(TAG,"findSerialPortDevce="+usbDevices.size());
		if (!usbDevices.isEmpty()) {
			for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
				device = entry.getValue();
				int deviceVID = device.getVendorId();
				int devicePID = device.getProductId();
				Log.i(TAG, "USB Device VID: " + deviceVID);
				Log.i(TAG, "USB Device PID: " + devicePID);
				if (deviceVID == 0x0403 || deviceVID == 1003 ||
				        (deviceVID == DEVICE_ID_FTDI && UsbUtils.isBiskitMode(getApplicationContext()))) {
					boolean success = grantRootPermissionToUSBDevice(device);
					connection = usbManager.openDevice(device);
					if (success) {
						new ConnectionThread().start();
					}
					Log.d(TAG, "Opened Serial MODBUS device instance for " + deviceVID);
				} else {
					connection = null;
					device = null;
				}
			}
		}
	}

	private boolean grantRootPermissionToUSBDevice(UsbDevice device)
	{
		IBinder b = ServiceManager.getService(Context.USB_SERVICE);
		IUsbManager service = IUsbManager.Stub.asInterface(b);
		Log.i(TAG, "Try connecting!");
		// There is a device connected to our Android device. Try to open it as a Serial Port.
		try
		{
			service.grantDevicePermission(device, getApplicationInfo().uid);
			return true;
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		return false;
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
				PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
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
						Log.i(TAG, "Serial Port is not connected sleeping");
						if (reconnectCounter++ >= 30) {
							Log.i(TAG, "scanSerialPortSilentlyForCmDevice");
							scanSerialPortSilentlyForCmDevice();
							reconnectCounter = 0;
						}
						sleep(2000);
						continue;
					} else {
						Log.i(TAG, "Serial Port is connected.");
					}

					data = messageQueue.take();

					if (serialPort != null) {
						byte buffer[] = new byte[128];
						byte crc = 0;
						byte nOffset = 0;
						int len = data.length;
						if(len >= 128)
							buffer = new byte[160]; //For OTA Updates
						buffer[nOffset++] = (byte) (ESC_BYTE & 0xff);
						buffer[nOffset++] = (byte) (SOF_BYTE & 0xff);
						buffer[nOffset++] = (byte) (len & 0xff);
						for (int i = 0; i < len; i++) {
							buffer[i + nOffset] = data[i]; // add payload to the tx buffer
							crc ^= data[i];             // calculate the new crc
							if (data[i] == (byte) (ESC_BYTE &
									0xff)) // if the data is equal to ESC byte then add another instance of that
							{
								nOffset++;
								buffer[i + nOffset] = data[i];
							}
						}
						buffer[nOffset + len] = (byte) (crc & 0xff);
						nOffset++;
						buffer[nOffset + len] = (byte) (ESC_BYTE & 0xff);
						nOffset++;
						buffer[nOffset + len] = (byte) (EOF_BYTE & 0xff);
						nOffset++;
						serialPort.write(Arrays.copyOfRange(buffer,0,len + nOffset));

						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception exception) {
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
			Log.i(TAG, "Serial is disconnected, message discarded");
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
		public UsbService getService()
		{
			return UsbService.this;
		}
	}

	/*
	 * A simple thread to open a serial port.
	 * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
	 */
	private class ConnectionThread extends Thread
	{
		@Override
		public void run()
		{

			serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
			if (serialPort != null)
			{
				if (serialPort.open())
				{
					setDebug(false);
					serialPortConnected = true;
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
					try {
						sleep(2000); // sleep some. YMMV with different chips.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					// Everything went as expected. Send an intent to MainActivity
					Intent intent = new Intent(ACTION_USB_READY);
					context.sendBroadcast(intent);
				}
				else
				{
					// Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
					// Send an Intent to Main Activity
					if (serialPort instanceof CDCSerialDevice)
					{
						Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
						context.sendBroadcast(intent);
					}
					else
					{
						Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
						context.sendBroadcast(intent);
					}
				}
			}
			else
			{
				// No driver for given device, even generic CDC driver could not be loaded
				Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
				context.sendBroadcast(intent);
			}
		}
	}
}

