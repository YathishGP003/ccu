package com.felhr.usbserial;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import com.felhr.deviceids.CH34xIds;
import com.felhr.deviceids.CP210xIds;
import com.felhr.deviceids.FTDISioIds;
import com.felhr.deviceids.PL2303Ids;
import com.felhr.utils.UsbModbusUtils;
import com.x75f.modbus4j.base.ModbusUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class UsbSerialDevice implements UsbSerialInterface
{
	
	
	protected boolean isFiDI()
	{
		return false;
	}
	
	private static final String CLASS_ID = UsbSerialDevice.class.getSimpleName();
	
	private static boolean mr1Version;
	protected final UsbDevice device;
	protected final UsbDeviceConnection connection;

	protected static final int USB_TIMEOUT = 5000;



	protected SerialBuffer serialBuffer;
	protected ModbusWorkerThread modbusWorkerThread;
	protected ModbusWriteThread modbusWriteThread;
	protected WorkerThread workerThread;
	protected WriteThread writeThread;
	protected ReadThread readThread;
	private static HashMap<UsbDevice, UsbDeviceConnection> connectionList = new HashMap<>();
	private static HashMap<UsbDevice, SerialBuffer> serialBufferList = new HashMap<>();
	private HashMap<UsbDevice, UsbEndpoint> inEndpointList;
	private HashMap<UsbDevice, UsbEndpoint> outEndpointList;

	// Endpoints for synchronous read and write operations
	private UsbEndpoint inEndpoint;
	private UsbEndpoint outEndpoint;

	protected boolean asyncMode;

	// Get Android version if version < 4.3 It is not going to be asynchronous read operations
	static
	{
		if(android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
			mr1Version = true;
		else
			mr1Version = false;
	}

	public UsbSerialDevice(UsbDevice device, UsbDeviceConnection connection)
	{
		this.device = device;
		this.connection = connection;
		this.asyncMode = true;
		serialBuffer = new SerialBuffer(mr1Version);
		Log.d("Serial","UsbSerialDevice constructor");
	}

	public static UsbSerialDevice createUsbSerialDevice(UsbDevice device, UsbDeviceConnection connection)
	{
		return createUsbSerialDevice(device, connection, -1);
	}

	public static UsbSerialDevice createUsbSerialDevice(UsbDevice device, UsbDeviceConnection connection, int iface)
	{
		/*
		 * It checks given vid and pid and will return a custom driver or a CDC serial driver.
		 * When CDC is returned open() method is even more important, its response will inform about if it can be really
		 * opened as a serial device with a generic CDC serial driver
		 */
		int vid = device.getVendorId();
		int pid = device.getProductId();
		if(connectionList.isEmpty() || !connectionList.containsKey(device)) {
			connectionList.put(device, connection);
			serialBufferList.put(device, new SerialBuffer(true));
		}

		Log.d("Serial","UsbSerialDevice createNewSErialDevice "+vid);
		if(FTDISioIds.isDeviceSupported(vid, pid))
		{
			//return null;
			return new FTDISerialDevice(device, connection, iface);
		}
		else if(CP210xIds.isDeviceSupported(vid, pid))
			return new CP2102SerialDevice(device, connection, iface);
		else if(PL2303Ids.isDeviceSupported(vid, pid))
			return new PL2303SerialDevice(device, connection, iface);
		else if(CH34xIds.isDeviceSupported(vid, pid))
			return new CH34xSerialDevice(device, connection, iface);
		else if(isCdcDevice(device))
			return new CDCSerialDevice(device, connection, iface);
		else
			return null;
	}

	public static boolean isSupported(UsbDevice device)
	{
		int vid = device.getVendorId();
		int pid = device.getProductId();

		if(FTDISioIds.isDeviceSupported(vid, pid))
			return true;
		else if(CP210xIds.isDeviceSupported(vid, pid))
			return true;
		else if(PL2303Ids.isDeviceSupported(vid, pid))
			return true;
		else if(CH34xIds.isDeviceSupported(vid, pid))
			return true;
		else if(isCdcDevice(device))
			return true;
		else
			return false;
	}

	// Common Usb Serial Operations (I/O Asynchronous)
	@Override
	public abstract boolean open();
	
	@Override
	public void write(byte[] buffer)
	{

		Log.d("Serial","UsbSerialDevice write");
		//if(asyncMode)
		//	serialBuffer.putWriteBuffer(buffer);
		if(asyncMode){
			for (Map.Entry<UsbDevice, SerialBuffer> serialBufferEntry : serialBufferList.entrySet()) {
				if (serialBufferEntry.getKey().getVendorId() == 0x0403 || serialBufferEntry.getKey().getVendorId() == 0x1027 ||
						serialBufferEntry.getKey().getVendorId() == 1003) {
					serialBufferEntry.getValue().putWriteBuffer(buffer);
				}
			}
		}
	}
	@Override
	public void writeModbus(byte[] buffer)
	{
		//if(asyncMode)
		//	serialBuffer.putWriteBuffer(buffer);
		if(asyncMode){
			for (Map.Entry<UsbDevice, SerialBuffer> serialBufferEntry : serialBufferList.entrySet()) {
				if (serialBufferEntry.getKey().getVendorId() == 4292 || serialBufferEntry.getKey().getVendorId() == 1027) {
					Log.d("Serial","UsbSerialDevice writeModbus:"+Arrays.toString(buffer));
					serialBufferEntry.getValue().putWriteBuffer(buffer);
				}
			}
		}
	}
	@Override
	public int read(UsbReadCallback mCallback)
	{
		if(!asyncMode)
			return -1;

		if(mr1Version)
		{

			Log.d("Serial","UsbSerialDevice read");
			workerThread.setCallback(mCallback);
			for (Map.Entry<UsbDevice, SerialBuffer> serialBufferEntry : serialBufferList.entrySet()) {
				if (serialBufferEntry.getKey().getVendorId() == 0x0403 || serialBufferEntry.getKey().getVendorId() == 0x1027 ||
						serialBufferEntry.getKey().getVendorId() == 1003) {
					workerThread.getUsbRequest().queue(serialBufferEntry.getValue().getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
				}
			}
			//workerThread.getUsbRequest().queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
		}else
		{
			readThread.setCallback(mCallback);
			//readThread.start();
		}
		return 0;
	}

	@Override
	public int readModbus(UsbReadCallback mCallback)
	{
		if(!asyncMode)
			return -1;

		if(mr1Version)
		{

			modbusWorkerThread.setCallback(mCallback);

			for (Map.Entry<UsbDevice, SerialBuffer> serialBufferEntry : serialBufferList.entrySet()) {
				if ( serialBufferEntry.getKey().getVendorId() == 4292 || serialBufferEntry.getKey().getVendorId() == 1027) {
					modbusWorkerThread.getUsbRequest().queue(serialBufferEntry.getValue().getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
				}
			}
			//modbusWorkerThread.getUsbRequest().queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
		}else
		{
			readThread.setCallback(mCallback);
			//readThread.start();
		}
		return 0;
	}
	@Override
	public abstract void close();

	// Common Usb Serial Operations (I/O Synchronous)
	@Override
	public abstract boolean syncOpen();

	@Override
	public abstract void syncClose();

	@Override
	public int syncWrite(byte[] buffer, int timeout)
	{
		if(!asyncMode)
		{
			if(buffer == null)
				return 0;
			if(isFTDIDevice()) try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			return connection.bulkTransfer(outEndpoint, buffer, buffer.length, timeout);
		}else
		{
			return -1;
		}
	}

	@Override
	public int syncRead(byte[] buffer, int timeout)
	{
		if(asyncMode)
		{
			return -1;
		}

		if (buffer == null)
			return 0;

		return connection.bulkTransfer(inEndpoint, buffer, buffer.length, timeout);
	}

	// Serial port configuration
	@Override
	public abstract void setBaudRate(int baudRate);
	@Override
	public abstract void setDataBits(int dataBits);
	@Override
	public abstract void setStopBits(int stopBits);
	@Override
	public abstract void setParity(int parity);
	@Override
	public abstract void setFlowControl(int flowControl);

	//Debug options
	public void debug(boolean value)
	{
		if(serialBuffer != null)
			serialBuffer.debug(value);
	}

	public boolean isFTDIDevice()
	{
		return (this instanceof FTDISerialDevice);
	}

	public static boolean isCdcDevice(UsbDevice device)
	{
		int iIndex = device.getInterfaceCount();
		for(int i=0;i<=iIndex-1;i++)
		{
			UsbInterface iface = device.getInterface(i);
			if(iface.getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA)
				return true;
		}
		return false;
	}

	private static final int ESC_BYTE = 0xD9;
	private static final int SOF_BYTE = 0x00;
	private static final int EOF_BYTE = 0x03;
	private int mCurrentSnToUpdate = -1;


	private SerialState curState = SerialState.PARSE_INIT;
	private int nDataLength = 0;
	private int nCurIndex = 0;
	private int nCRC = 0;
	private byte inDataBuffer[] = new byte[1024];

	/*
	 * WorkerThread waits for request notifications from IN endpoint
	 */
	protected class WorkerThread extends Thread
	{
		private UsbSerialDevice usbSerialDevice;

		private UsbReadCallback callback;
		private UsbRequest requestIN;
		private AtomicBoolean working;

		public WorkerThread(UsbSerialDevice usbSerialDevice)
		{
			this.usbSerialDevice = usbSerialDevice;
			working = new AtomicBoolean(true);
		}
		
		@Override
		public void run()
		{
			while(working.get())
			{
				UsbRequest request = null;//connection.requestWait();
				byte[] inReg = null;//serialBuffer.getDataReceived();
				int nRet = 0;//inReg.length;
				for (Map.Entry<UsbDevice, UsbDeviceConnection> usbEntry : connectionList.entrySet()) {
					if (usbEntry.getKey().getVendorId() == 0x0403 || usbEntry.getKey().getVendorId() == 0x1027 ||
							usbEntry.getKey().getVendorId() == 1003) {
						request = usbEntry.getValue().requestWait();
					}
				}

				for (Map.Entry<UsbDevice, SerialBuffer> serialBufferEntry : serialBufferList.entrySet()) {
					if (serialBufferEntry.getKey().getVendorId() == 0x0403 || serialBufferEntry.getKey().getVendorId() == 0x1027 ||
							serialBufferEntry.getKey().getVendorId() == 1003) {
						inReg = serialBufferEntry.getValue().getDataReceived();
						nRet = inReg.length;
						serialBufferEntry.getValue().clearReadBuffer();
					}
				}
				if(request != null && request.getEndpoint().getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
						&& request.getEndpoint().getDirection() == UsbConstants.USB_DIR_IN)
				{
					//serialBuffer.clearReadBuffer();
					if(nRet > 0) {
						int nCount = 0;
						if (isFTDIDevice())
							nCount = 2;

						if(nRet > 2){
							String dp = "";
							for (int n = 0; n < inReg.length; n++)
								dp = dp + " " + String.valueOf((int) (inReg[n] & 0xff));
							Calendar curDate = GregorianCalendar.getInstance();
							Log.d("SERIAL_IN", "[" + (inReg.length) + "]-[" + curDate.get(Calendar.HOUR_OF_DAY) + ":" + curDate.get(Calendar.MINUTE) + "] :" + dp);
						}
						for (; nCount < nRet; nCount++) {
							byte inData =  inReg[nCount];
							int intData = inData & 0xff;
							switch (curState) {
								case PARSE_INIT:
                                    if (intData == ESC_BYTE)
										curState = SerialState.ESC_BYTE_RCVD;
									break;
								case ESC_BYTE_RCVD:
									if (intData == SOF_BYTE)
										curState = SerialState.SOF_BYTE_RCVD;
									else
										curState = SerialState.BAD_PACKET;

									break;
								case SOF_BYTE_RCVD:
									nDataLength = inData;
									curState = SerialState.LEN_BYTE_RCVD;
									break;
								case LEN_BYTE_RCVD:
									if (nCurIndex == nDataLength) {
										int nIncomingCRC = inData;
										if (nIncomingCRC == nCRC)
											curState = SerialState.CRC_RCVD;
										else {
											Log.d("SERIAL_RAW", "CRC Mismatch: Incoming: " + nIncomingCRC + "Calculated: " + nCRC);
											curState = SerialState.BAD_PACKET;
										}
									} else if (nCurIndex < nDataLength) {
										inDataBuffer[nCurIndex] = inData;
										nCRC ^= inData;
										nCurIndex++;
										if (intData == ESC_BYTE)
											curState = SerialState.ESC_BYTE_IN_DATA_RCVD;
									} else
										curState = SerialState.BAD_PACKET;
									break;
								case ESC_BYTE_IN_DATA_RCVD:
									if (intData == ESC_BYTE)
										curState = SerialState.LEN_BYTE_RCVD;
									else
										curState = SerialState.BAD_PACKET;
									break;
								case CRC_RCVD:
									if (intData == ESC_BYTE)
										curState = SerialState.ESC_BYTE_AS_END_OF_PACKET_RCVD;
									else
										curState = SerialState.BAD_PACKET;
									break;
								case ESC_BYTE_AS_END_OF_PACKET_RCVD:
									if (intData == EOF_BYTE)
										curState = SerialState.DATA_AVAILABLE;
									else
										curState = SerialState.BAD_PACKET;
									break;

							}
							/*if(curState == SerialState.PARSE_INIT) {
								switch (curModbusState) {
									case MB_PARSE_INIT:
										if (usbModbusUtils.validSlaveId(inData)){ //TODO need to check for available slave address configured based on modbus paired
											curModbusState = ModbusSerialState.MB_CHECK_FOR_SLAVE_ID;
											inDataBuffer[nCurIndex] = inData;
											nCRC = ModbusUtils.calculateCRC(inData & 0xff, 0xff,0xff);
											nCurIndex++;
										}else
											curState = SerialState.BAD_PACKET;
										break;
									case MB_CHECK_FOR_SLAVE_ID:
										int validFuncCode = usbModbusUtils.validateFunctionCode(inData);
										if(inData == validFuncCode){
											curModbusState = ModbusSerialState.MB_CHECK_FOR_FUNC_CODE;
											inDataBuffer[nCurIndex] = inData;
											nCRC = ModbusUtils.calculateCRC(inData & 0xff, (nCRC & 0xff),(nCRC >> 8 & 0xff));
											nCurIndex++;
										}else if(inData != SOF_BYTE){//functional error code from modbus
											curModbusState = ModbusSerialState.MB_FUNC_CODE_ERR;
											inDataBuffer[nCurIndex] = inData;
											nCRC = ModbusUtils.calculateCRC(inData & 0xff, (nCRC & 0xff),(nCRC >> 8 & 0xff));
											nCurIndex++;
										}
										break;
									case MB_CHECK_FOR_FUNC_CODE:
										if(nCurIndex == 2) {
											inDataBuffer[nCurIndex] = inData;
											nCRC = ModbusUtils.calculateCRC(inData & 0xff, (nCRC & 0xff),(nCRC >> 8 & 0xff));
											nCurIndex++;
											if(inData != 0) {
												nDataLength = inData + nCurIndex + 1;
												curModbusState = ModbusSerialState.MB_LEN_BYTE_RCVD;
											}else {
												curModbusState = ModbusSerialState.MB_WRITE_DATA_RCVD;
											}
										}
										break;
									case MB_FUNC_CODE_ERR:
										if(nCurIndex == 2) {
											curModbusState = ModbusSerialState.MB_LEN_BYTE_RCVD;
											inDataBuffer[nCurIndex] = inData;
											nCRC = ModbusUtils.calculateCRC(inData & 0xff, (nCRC & 0xff),(nCRC >> 8 & 0xff));
											nCurIndex++;
											nDataLength = nCurIndex + 1;
										}
										break;
									case MB_LEN_BYTE_RCVD:
										if (nCurIndex == nDataLength) {
											inDataBuffer[nCurIndex] = inData;
											nCRC = ModbusUtils.calculateCRC(inData & 0xff, (nCRC & 0xff),(nCRC >> 8 & 0xff));
											nCurIndex++;
											curModbusState = ModbusSerialState.MB_DATA_AVAILABLE;
										} else if (nCurIndex < nDataLength) {
											inDataBuffer[nCurIndex] = inData;
											nCRC = ModbusUtils.calculateCRC(inData & 0xff, (nCRC & 0xff),(nCRC >> 8 & 0xff));
											nCurIndex++;
										} else
											curState = SerialState.BAD_PACKET;
										break;
									case MB_WRITE_DATA_RCVD:
										if ((inData & 0xff) == ((nCRC >> 8) & 0xff)){
											inDataBuffer[nCurIndex] = inData;
											nCurIndex++;
											curModbusState = ModbusSerialState.MB_CRC_RCVD;
										}
										else {
											inDataBuffer[nCurIndex] = inData;
											nCurIndex++;
										}
										nCRC = ModbusUtils.calculateCRC(inData & 0xff, (nCRC & 0xff),(nCRC >> 8 & 0xff));
										break;
									case MB_CRC_RCVD:
										inDataBuffer[nCurIndex] = inData;
										nCurIndex++;
										curModbusState = ModbusSerialState.MB_DATA_AVAILABLE;
										break;
								}
								Log.d("SERIAL_RAW", "*******Modbus PACKET RECEIVED*****"+nCount+","+nCRC+","+nCurIndex+","+nDataLength+","+intData+","+curState.name()+","+curModbusState.name());
							}*/
							if (curState == SerialState.DATA_AVAILABLE ) {
								onReceivedData(inDataBuffer, nCurIndex);
								nCurIndex = 0;
								nCRC = 0;
								curState = SerialState.PARSE_INIT;
								//curModbusState = ModbusSerialState.MB_PARSE_INIT;
							}/*else if (curModbusState == ModbusSerialState.MB_DATA_AVAILABLE) {
								onReceivedData(inDataBuffer, nCurIndex,true);
								nCurIndex = 0;
								nCRC = 0;
								curState = SerialState.PARSE_INIT;
								curModbusState = ModbusSerialState.MB_PARSE_INIT;
							}*/

							if (curState == SerialState.BAD_PACKET) {
								Log.d("SERIAL_RAW", "*******BAD PACKET RECEIVED*****");
								nCurIndex = 0;
								nCRC = 0;
								curState = SerialState.PARSE_INIT;
								//curModbusState = ModbusSerialState.MB_PARSE_INIT;
							}
						}
					}
					// Queue a new request
					//requestIN.queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
					for (Map.Entry<UsbDevice, SerialBuffer> serialBufferEntry : serialBufferList.entrySet()) {
						if (serialBufferEntry.getKey().getVendorId() == 0x0403 || serialBufferEntry.getKey().getVendorId() == 0x1027 ||
								serialBufferEntry.getKey().getVendorId() == 1003) {
							requestIN.queue(serialBufferEntry.getValue().getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
						}
					}
				}
			}
		}

		public void setCallback(UsbReadCallback callback)
		{
			this.callback = callback;
		}

		public void setUsbRequest(UsbRequest request)
		{
			this.requestIN = request;
		}

		public UsbRequest getUsbRequest()
		{
			return requestIN;
		}

		private void onReceivedData(byte[] data, int length)
		{
			if(callback != null) {
				callback.onReceivedData(data, length);
			}
		}

		public void stopWorkingThread()
		{
			working.set(false);
		}
	}
	private ModbusSerialState curModbusState = ModbusSerialState.MB_PARSE_INIT;
	private int nMbDataLength = 0;
	private int nMbCurIndex = 0;
	private int nMbCRC = 0;
	private byte inMbDataBuffer[] = new byte[1024];


	/* ModbusWorkerThread waits for request notifications from IN endpoint*/

	protected class ModbusWorkerThread extends Thread
	{
		private UsbSerialDevice usbSerialDevice;

		private UsbReadCallback callback;
		private UsbRequest requestIN;
		private AtomicBoolean working;

		public ModbusWorkerThread(UsbSerialDevice usbSerialDevice)
		{
			this.usbSerialDevice = usbSerialDevice;
			working = new AtomicBoolean(true);
		}

		@Override
		public void run()
		{
			while(working.get())
			{
				UsbRequest request = null;//connection.requestWait();
				byte[] inReg = null;//serialBuffer.getDataReceived();
				int nRet = 0;//inReg.length;
				for (Map.Entry<UsbDevice, UsbDeviceConnection> usbEntry : connectionList.entrySet()) {
					if (usbEntry.getKey().getVendorId() == 0x0403 || usbEntry.getKey().getVendorId() == 1027 ||
							usbEntry.getKey().getVendorId() == 1003 || usbEntry.getKey().getVendorId() == 4292) {
						request = usbEntry.getValue().requestWait();
					}
				}

				for (Map.Entry<UsbDevice, SerialBuffer> serialBufferEntry : serialBufferList.entrySet()) {
					if (serialBufferEntry.getKey().getVendorId() == 0x0403 || serialBufferEntry.getKey().getVendorId() == 1027 ||
							serialBufferEntry.getKey().getVendorId() == 1003 || serialBufferEntry.getKey().getVendorId() == 4292) {
						inReg = serialBufferEntry.getValue().getDataReceived();
						nRet = inReg.length;
						serialBufferEntry.getValue().clearReadBuffer();
					}
				}
				if(inReg.length > 2)
					Log.d("SERIAL_IN_MB","ModbusWT Entry =="+request.toString()+","+ Arrays.toString(inReg)+","+inReg.length);
				if(request != null && request.getEndpoint().getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
						&& request.getEndpoint().getDirection() == UsbConstants.USB_DIR_IN)
				{
					//byte[] inReg = serialBuffer.getDataReceived();
					//int nRet = inReg.length;
					//serialBuffer.clearReadBuffer();
					if(nRet > 0) {
						int nCount = 0;
						if (isFTDIDevice())
							nCount = 2;

						if(nRet > 2){
							String dp = "";
							for (int n = 0; n < inReg.length; n++)
								dp = dp + " " + String.valueOf((int) (inReg[n] & 0xff));
							Calendar curDate = GregorianCalendar.getInstance();
							Log.d("SERIAL_IN_MB", "[" + (inReg.length) + "]-[" + curDate.get(Calendar.HOUR_OF_DAY) + ":" + curDate.get(Calendar.MINUTE) + "] :" + dp);
						}
						for (; nCount < nRet; nCount++) {
							byte inData =  inReg[nCount];
							int intData = inData & 0xff;
							switch (curModbusState) {
								case MB_PARSE_INIT:
									if (UsbModbusUtils.validSlaveId(inData)){ //TODO need to check for available slave address configured based on modbus paired
										curModbusState = ModbusSerialState.MB_CHECK_FOR_SLAVE_ID;
										inMbDataBuffer[nMbCurIndex] = inData;
										nMbCRC = ModbusUtils.calculateCRC(inData & 0xff, 0xff,0xff);
										nMbCurIndex++;
									}else
										curModbusState = ModbusSerialState.MB_BAD_PACKET;
									break;
								case MB_CHECK_FOR_SLAVE_ID:
									int validFuncCode = UsbModbusUtils.validateFunctionCode(inData);
									if(inData == validFuncCode){
										curModbusState = ModbusSerialState.MB_CHECK_FOR_FUNC_CODE;
										inMbDataBuffer[nMbCurIndex] = inData;
										nMbCRC = ModbusUtils.calculateCRC(inData & 0xff, (nMbCRC & 0xff),(nMbCRC >> 8 & 0xff));
										nMbCurIndex++;
									}else if(inData != SOF_BYTE){//functional error code from modbus
										curModbusState = ModbusSerialState.MB_FUNC_CODE_ERR;
										inMbDataBuffer[nMbCurIndex] = inData;
										nMbCRC = ModbusUtils.calculateCRC(inData & 0xff, (nMbCRC & 0xff),(nMbCRC >> 8 & 0xff));
										nMbCurIndex++;
									}
									break;
								case MB_CHECK_FOR_FUNC_CODE:
									if(nMbCurIndex == 2) {
										inMbDataBuffer[nMbCurIndex] = inData;
										nMbCRC = ModbusUtils.calculateCRC(inData & 0xff, (nMbCRC & 0xff),(nMbCRC >> 8 & 0xff));
										nMbCurIndex++;
										if(inData != 0) {
											nMbDataLength = inData + nMbCurIndex + 1;
											curModbusState = ModbusSerialState.MB_LEN_BYTE_RCVD;
										}else {
											curModbusState = ModbusSerialState.MB_WRITE_DATA_RCVD;
										}
									}
									break;
								case MB_FUNC_CODE_ERR:
									if(nMbCurIndex == 2) {
										curModbusState = ModbusSerialState.MB_LEN_BYTE_RCVD;
										inMbDataBuffer[nMbCurIndex] = inData;
										nMbCRC = ModbusUtils.calculateCRC(inData & 0xff, (nMbCRC & 0xff),(nMbCRC >> 8 & 0xff));
										nMbCurIndex++;
										nMbDataLength = nMbCurIndex + 1;
									}
									break;
								case MB_LEN_BYTE_RCVD:
									if (nMbCurIndex == nMbDataLength) {
										inMbDataBuffer[nMbCurIndex] = inData;
										nMbCRC = ModbusUtils.calculateCRC(inData & 0xff, (nMbCRC & 0xff),(nMbCRC >> 8 & 0xff));
										nMbCurIndex++;
										curModbusState = ModbusSerialState.MB_DATA_AVAILABLE;
									} else if (nMbCurIndex < nMbDataLength) {
										inMbDataBuffer[nMbCurIndex] = inData;
										nMbCRC = ModbusUtils.calculateCRC(inData & 0xff, (nMbCRC & 0xff),(nMbCRC >> 8 & 0xff));
										nMbCurIndex++;
									} else
										curModbusState = ModbusSerialState.MB_BAD_PACKET;
									break;
								case MB_WRITE_DATA_RCVD:
									if ((inData & 0xff) == ((nMbCRC >> 8) & 0xff)){
										inMbDataBuffer[nMbCurIndex] = inData;
										nMbCurIndex++;
										curModbusState = ModbusSerialState.MB_CRC_RCVD;
									}
									else {
										inMbDataBuffer[nMbCurIndex] = inData;
										nMbCurIndex++;
									}
									nMbCRC = ModbusUtils.calculateCRC(inData & 0xff, (nMbCRC & 0xff),(nMbCRC >> 8 & 0xff));
									break;
								case MB_CRC_RCVD:
									inMbDataBuffer[nMbCurIndex] = inData;
									nMbCurIndex++;
									curModbusState = ModbusSerialState.MB_DATA_AVAILABLE;
									break;
							}
							Log.d("SERIAL_RAW", "*******Modbus PACKET RECEIVED*****"+nCount+","+nMbCRC+","+nMbCurIndex+","+nMbDataLength+","+intData+","+curModbusState.name());
							if (curModbusState == ModbusSerialState.MB_DATA_AVAILABLE) {
								onReceivedData(inMbDataBuffer, nMbCurIndex);
								nMbCurIndex = 0;
								nMbCRC = 0;
								curModbusState = ModbusSerialState.MB_PARSE_INIT;
							}

							if (curModbusState == ModbusSerialState.MB_BAD_PACKET) {
								Log.d("SERIAL_RAW", "*******BAD PACKET RECEIVED*****");
								nMbCurIndex = 0;
								nMbCRC = 0;
								curModbusState = ModbusSerialState.MB_PARSE_INIT;
							}
						}
					}
					// Queue a new request
					//requestIN.queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);


					for (Map.Entry<UsbDevice, SerialBuffer> serialBufferEntry : serialBufferList.entrySet()) {
						if (serialBufferEntry.getKey().getVendorId() == 0x0403 || serialBufferEntry.getKey().getVendorId() == 0x1027 ||
								serialBufferEntry.getKey().getVendorId() == 1003 || serialBufferEntry.getKey().getVendorId() == 4292) {

							requestIN.queue(serialBufferEntry.getValue().getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
						}
					}
				}
			}
		}

		public void setCallback(UsbReadCallback callback)
		{
			this.callback = callback;
		}

		public void setUsbRequest(UsbRequest request)
		{
			this.requestIN = request;
		}

		public UsbRequest getUsbRequest()
		{
			return requestIN;
		}

		private void onReceivedData(byte[] data, int length)
		{
			if(callback != null) {
				callback.onReceivedData(data, length);
			}
		}

		public void stopWorkingThread()
		{
			working.set(false);
		}
	}
	protected class WriteThread extends Thread
	{
		private UsbEndpoint outEndpoint;
		private AtomicBoolean working;

		public WriteThread()
		{
			working = new AtomicBoolean(true);
		}

		@Override
		public void run()
		{
			while(working.get())
			{
				byte[] data = null;
				for (Map.Entry<UsbDevice, SerialBuffer> serialBufferEntry : serialBufferList.entrySet()) {
					if (serialBufferEntry.getKey().getVendorId() == 0x0403 || serialBufferEntry.getKey().getVendorId() == 0x1027 ||
							serialBufferEntry.getKey().getVendorId() == 1003 || serialBufferEntry.getKey().getVendorId() == 4292) {
						data = serialBufferEntry.getValue().getWriteBuffer();
					}
				}
				//byte[] data = serialBuffer.getWriteBuffer();
				for (Map.Entry<UsbDevice, UsbDeviceConnection> usbEntry : connectionList.entrySet()) {
					if (usbEntry.getKey().getVendorId() == 0x0403 || usbEntry.getKey().getVendorId() == 0x1027 ||
							usbEntry.getKey().getVendorId() == 1003 || usbEntry.getKey().getVendorId() == 4292) {
						usbEntry.getValue().bulkTransfer(outEndpoint,data, data.length, USB_TIMEOUT);
					}
				}
				//connection.bulkTransfer(outEndpoint, data, data.length, USB_TIMEOUT);
			}
		}

		public void setUsbEndpoint(UsbEndpoint outEndpoint)
		{
			this.outEndpoint = outEndpoint;
		}

		public void stopWriteThread()
		{
			working.set(false);
		}
	}
	protected class ModbusWriteThread extends Thread
	{
		private UsbEndpoint outEndpoint;
		private AtomicBoolean working;

		public ModbusWriteThread()
		{
			working = new AtomicBoolean(true);
		}

		@Override
		public void run()
		{
			while(working.get())
			{
				byte[] data = null;
				UsbDeviceConnection deviceConnection = null;
				for (Map.Entry<UsbDevice, UsbDeviceConnection> usbEntry : connectionList.entrySet()) {
					if ( usbEntry.getKey().getVendorId() == 4292 || usbEntry.getKey().getVendorId() == 1027) {
						deviceConnection = usbEntry.getValue();
					}
				}
				for (Map.Entry<UsbDevice, SerialBuffer> serialBufferEntry : serialBufferList.entrySet()) {
					if ( serialBufferEntry.getKey().getVendorId() == 4292 || serialBufferEntry.getKey().getVendorId() == 1027) {
						data = serialBufferEntry.getValue().getWriteBuffer();
						if(deviceConnection != null){
							deviceConnection.bulkTransfer(outEndpoint, data, data.length, USB_TIMEOUT);
						}

					}
				}
				//byte[] data = serialBuffer.getWriteBuffer();
			}
		}

		public void setUsbEndpoint(UsbEndpoint outEndpoint)
		{
			this.outEndpoint = outEndpoint;
		}

		public void stopWriteThread()
		{
			working.set(false);
		}
	}
	protected class ReadThread extends Thread
	{
		private UsbSerialDevice usbSerialDevice;

		private UsbReadCallback callback;
		private UsbEndpoint inEndpoint;
		private AtomicBoolean working;

		public ReadThread(UsbSerialDevice usbSerialDevice)
		{
			this.usbSerialDevice = usbSerialDevice;
			working = new AtomicBoolean(true);
		}

		public void setCallback(UsbReadCallback callback)
		{
			this.callback = callback;
		}

		@Override
		public void run()
		{
			byte[] dataReceived = null;

			while(working.get())
			{
				int numberBytes;
				if(inEndpoint != null)
					numberBytes = connection.bulkTransfer(inEndpoint, serialBuffer.getBufferCompatible(),
							SerialBuffer.DEFAULT_READ_BUFFER_SIZE, 0);
				else
					numberBytes = 0;

				if(numberBytes > 0)
				{
					dataReceived = serialBuffer.getDataReceivedCompatible(numberBytes);

					// FTDI devices reserve two first bytes of an IN endpoint with info about
					// modem and Line.
					if(isFTDIDevice())
					{
						((FTDISerialDevice) usbSerialDevice).ftdiUtilities.checkModemStatus(dataReceived);

						if(dataReceived.length > 2)
						{
							dataReceived = ((FTDISerialDevice) usbSerialDevice).ftdiUtilities.adaptArray(dataReceived);
							onReceivedData(null, 0);
						}
					}else
					{
						onReceivedData(null, numberBytes);
					}
				}
			}
		}

		public void setUsbEndpoint(UsbEndpoint inEndpoint)
		{
			this.inEndpoint = inEndpoint;
		}

		public void stopReadThread()
		{
			working.set(false);
		}

		private void onReceivedData(byte[] data, int length)
		{
			if(callback != null)
				callback.onReceivedData(data, length);
		}
	}

	protected void setSyncParams(UsbEndpoint inEndpoint, UsbEndpoint outEndpoint)
	{
		this.inEndpoint = inEndpoint;
		this.outEndpoint = outEndpoint;
	}
	public UsbEndpoint getInEndPoint()
	{
		for (Map.Entry<UsbDevice, UsbEndpoint> usbEntry : inEndpointList.entrySet()) {
			if (usbEntry.getKey().getVendorId() == 4292) {
				return usbEntry.getValue();
			} else if (usbEntry.getKey().getVendorId() == 0x0403 || usbEntry.getKey().getVendorId() == 0x1027 ||
					usbEntry.getKey().getVendorId() == 1003) {
				return usbEntry.getValue();
			}
		}
		return inEndpoint;
	}
	public UsbEndpoint getOutEndPoint()
	{

		for (Map.Entry<UsbDevice, UsbEndpoint> usbEntry : outEndpointList.entrySet()) {
			if (usbEntry.getKey().getVendorId() == 4292) {
				return usbEntry.getValue();
			} else if (usbEntry.getKey().getVendorId() == 0x0403 || usbEntry.getKey().getVendorId() == 0x1027 ||
					usbEntry.getKey().getVendorId() == 1003) {
				return usbEntry.getValue();
			}
		}
		return outEndpoint;
	}

	protected void setThreadsParams(UsbRequest request, UsbEndpoint endpoint)
	{
		if(mr1Version)
		{
			workerThread.setUsbRequest(request);
			writeThread.setUsbEndpoint(endpoint);
		}else
		{
			readThread.setUsbEndpoint(request.getEndpoint());
			writeThread.setUsbEndpoint(endpoint);
		}
	}
	protected void setMbThreadsParams(UsbRequest request, UsbEndpoint endpoint)
	{
		if(mr1Version)
		{
			modbusWorkerThread.setUsbRequest(request);
			modbusWriteThread.setUsbEndpoint(endpoint);
		}else
		{
			readThread.setUsbEndpoint(request.getEndpoint());
			writeThread.setUsbEndpoint(endpoint);
		}
	}
	/*
	 * Kill workingThread; This must be called when closing a device
	 */
	protected void killWorkingThread()
	{
		if(mr1Version && workerThread != null)
		{
			workerThread.stopWorkingThread();
			workerThread = null;
		}
		if(mr1Version && modbusWorkerThread != null)
		{
			modbusWorkerThread.stopWorkingThread();
			modbusWorkerThread = null;
		}
		else if(!mr1Version && readThread != null)
		{
			readThread.stopReadThread();
			readThread = null;
		}
	}

	/*
	 * Restart workingThread if it has been killed before
	 */
	protected void restartWorkingThread()
	{
		if(mr1Version && workerThread == null)
		{
			workerThread = new WorkerThread(this);
			workerThread.start();
			while(!workerThread.isAlive()){} // Busy waiting
		}
		else if(!mr1Version && readThread == null)
		{
			readThread = new ReadThread(this);
			readThread.start();
			while(!readThread.isAlive()){} // Busy waiting
		}
	}
	/*
	 * Restart workingThread if it has been killed before
	 */
	protected void restartMbWorkingThread()
	{
		if(mr1Version && modbusWorkerThread == null)
		{
			modbusWorkerThread = new ModbusWorkerThread(this);
			modbusWorkerThread.start();
			while(!modbusWorkerThread.isAlive()){} // Busy waiting
		}
		else if(!mr1Version && readThread == null)
		{
			readThread = new ReadThread(this);
			readThread.start();
			while(!readThread.isAlive()){} // Busy waiting
		}
	}
	protected void killWriteThread()
	{
		if(writeThread != null)
		{
			writeThread.stopWriteThread();
			writeThread = null;
			serialBuffer.resetWriteBuffer();
		}
		if(modbusWriteThread != null){
			modbusWriteThread.stopWriteThread();
			modbusWriteThread = null;
			for (Map.Entry<UsbDevice, SerialBuffer> usbEntry : serialBufferList.entrySet()) {
				if (usbEntry.getKey().getVendorId() == 4292 || usbEntry.getKey().getVendorId() == 1027) {
					usbEntry.getValue().resetWriteBuffer();
				}
			}
		}
	}

	protected void restartWriteThread()
	{
		if(writeThread == null)
		{
			writeThread = new WriteThread();
			writeThread.start();
			while(!writeThread.isAlive()){} // Busy waiting
		}
	}
	protected void restartMbWriteThread()
	{
		if(modbusWriteThread == null)
		{
			modbusWriteThread = new ModbusWriteThread();
			modbusWriteThread.start();
			while(!modbusWriteThread.isAlive()){} // Busy waiting
		}
	}
	private enum SerialState
	{
		PARSE_INIT, ESC_BYTE_RCVD, SOF_BYTE_RCVD, LEN_BYTE_RCVD, ESC_BYTE_IN_DATA_RCVD, CRC_RCVD,
		ESC_BYTE_AS_END_OF_PACKET_RCVD, BAD_PACKET, DATA_AVAILABLE
	}
	private enum ModbusSerialState
	{
		MB_PARSE_INIT, MB_CHECK_FOR_SLAVE_ID, MB_CHECK_FOR_FUNC_CODE , MB_FUNC_CODE_ERR,MB_LEN_BYTE_RCVD, MB_WRITE_DATA_RCVD, MB_CRC_RCVD, MB_BAD_PACKET,MB_DATA_AVAILABLE
	}
}