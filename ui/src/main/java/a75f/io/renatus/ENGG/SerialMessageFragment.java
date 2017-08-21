package a75f.io.renatus.ENGG;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.javolution.io.Struct;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.bo.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbErrorReportMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.bo.serial.SnToCmOverAirSnRegularUpdateMessage_t;
import a75f.io.renatus.ENGG.logger.Log;
import a75f.io.renatus.MainActivity;
import a75f.io.renatus.R;

import a75f.io.usbserial.SerialAction;
import a75f.io.usbserial.SerialEvent;
import a75f.io.usbserial.UsbService;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by samjithsadasivan on 8/17/17.
 */

public class SerialMessageFragment extends Fragment
{
	List<String> messages = Arrays.asList("Select Message",
			"CcuToCmOverUsbDatabaseSeedSnMessage_t",
			"CcuToCmOverUsbSnSettingsMessage_t",
			"CcuToCmOverUsbSnControlsMessage_t",
			"CcuToCmOverUsbSnLightingScheduleMessage_t",
			"CcuToCmOverUsbDatabaseSeedSmartStatMessage_t",
			"CcuToCmOverUsbSmartStatSettingsMessage_t",
			"CcuToCmOverUsbSmartStatControlsMessage_t",
			"CcuToCmOverUsbCcuHeartbeatMessage_t",
			"CcuToCmOverUsbCmRelayActivationMessage_t");
	
	List<String> channels = Arrays.asList("1000", "2000", "3000", "4000", "5000", "6000", "7000", "8000", "9000");
	
	@BindView(R.id.msgSpinner)
	Spinner msgSpinner;

	@BindView(R.id.channelSpinner)
	Spinner channelSpinner;
	
	@BindView(R.id.msgSend)
	EditText msgSend;
	
	@BindView(R.id.msgRcvd)
	TextView msgRcvd;
	
	private int msgSelection;
	private int channelSelection = 0;
	
	private Class<?> msgClass = null;
	
	public static SerialMessageFragment newInstance(){
		return new SerialMessageFragment();
	}
	
	public SerialMessageFragment(){
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_serial_mesages, container, false);
		ButterKnife.bind(this, rootView);
		return rootView;
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initMessageSpinner();
		initChannelSpinner();
		
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
	}
	
	private void initMessageSpinner() {
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),
				                                                           android.R.layout.simple_spinner_dropdown_item, messages);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		msgSpinner.setAdapter(dataAdapter);
		msgSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l)
			{
				if (position == 0)
					msgSend.setText(null);
				else
					fillMessageView(position);
			}
			@Override
			public void onNothingSelected(AdapterView<?> adapterView)
			{
			}
		});
	}
	
	private void initChannelSpinner() {
		
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),
				                                                           android.R.layout.simple_spinner_item, channels);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		channelSpinner.setAdapter(dataAdapter);
		channelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				channelSelection = position;
				
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});
		
	}
	
	private void fillMessageView(int position) {
		try
		{
			msgClass =  Class.forName("a75f.io.bo.serial."+messages.get(position));
			Struct msg = (Struct) msgClass.newInstance();
			msgSend.setText(JsonSerializer.toJson(msg, true));
			
		} catch (Exception e) {
			Log.d("CCU" ,e.getMessage());
			
		}
		
	}
	
	@OnClick(R.id.msgSend)
	public void sendMessage() {
		
		try
		{
			Struct msg = (Struct) JsonSerializer.fromJson(msgSend.getText().toString(), msgClass);
			usbService.write(msg.getOrderedBuffer());
			
		} catch (Exception e) {
			Log.d("CCU" ,e.getMessage());
			
		}
		
	}
	
	/*
   * Notifications from UsbService will be received here.
   */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch (intent.getAction()) {
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
	private UsbService                      usbService;
	private MyHandler mHandler;
	private final ServiceConnection usbConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			usbService = ((UsbService.UsbBinder) arg1).getService();
			usbService.setHandler(mHandler);
		}
		
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			usbService = null;
		}
	};
	
	/*
	 * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
	 */
	private static class MyHandler extends Handler
	{
		private final WeakReference<MainActivity> mActivity;
		
		public MyHandler(MainActivity activity) {
			mActivity = new WeakReference<>(activity);
		}
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				
				case UsbService.CTS_CHANGE:
					Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
					break;
				case UsbService.DSR_CHANGE:
					Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
					break;
			}
		}
	}
	
	// Called in a separate thread
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onSerialEvent(SerialEvent event)
	{
		if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_PORT)
		{
			byte[] data = (byte[]) event.getBytes();
			
			MessageType messageType = MessageType.values()[(event.getBytes()[0] & 0xff)];
			String msgString = null;
			Struct msg = null;
			
			switch(messageType) {
				case CM_REGULAR_UPDATE:
					msg = new CmToCcuOverUsbCmRegularUpdateMessage_t();
					break;
				case CM_ERROR_REPORT:
					msg = new CmToCcuOverUsbErrorReportMessage_t();
					break;
				case CM_TO_CCU_OVER_USB_SN_REBOOT:
					// TODO - define struct
					break;
				case CM_TO_CCU_OVER_USB_SN_REGULAR_UPDATE:
					msg = new CmToCcuOverUsbSnRegularUpdateMessage_t();
					break;
				case CM_TO_CCU_OVER_USB_SMART_STAT_REGULAR_UPDATE:
					//TODO - define struct
					break;
			}
			msg.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
			try
			{
				msgString = JsonSerializer.toJson(msg, true);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			msgRcvd.setText(null);
			msgRcvd.setText(msgString);
		}
	}
	
}
