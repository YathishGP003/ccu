package a75f.io.renatus;

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
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import a75f.io.bo.SmartNode;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSmartStatMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnSettingsMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.bo.serial.SmartStatConditioningMode_t;
import a75f.io.bo.serial.SmartStatFanSpeed_t;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.usbserial.SerialAction;
import a75f.io.usbserial.SerialEvent;

import a75f.io.usbserial.UsbService;
import a75f.io.util.Globals;
import a75f.io.util.prefs.EncryptionPrefs;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static java.lang.Short.parseShort;

/**
 * Created by samjithsadasivan on 8/9/17.
 */

public class TestFragment extends BaseDialogFragment
{
	private static final String TAG = DialogFragment.class.getSimpleName();
	
	List<String> channels = Arrays.asList("Select Channel", "1000", "2000", "3000", "4000", "5000", "6000", "7000", "8000", "9000");
	
	List<String> profiles = Arrays.asList("Select Profile", "DAB", "LightingControl", "OAO", "SSE", "CustomControl");
	
	List<String> fanSpeed = Arrays.asList("Select Fan Speed", "Off", "Auto", "Low", "High");
	
	List<String> condMode = Arrays.asList("Select Conditioning Mode", "Off" ,"Auto", "Heating", "Cooling");
	
	
	public static TestFragment newInstance(){
		return new TestFragment();
	}
	
	public TestFragment(){
		
	}
	
	@BindView(R.id.roomName)
	EditText roomName;
	
	@BindView(R.id.channelSpinner)
	Spinner channelSpinner;
	
	@BindView(R.id.profileSpinner)
	Spinner profileSpinner;
	
	@BindView(R.id.fanspeedSpinner)
	Spinner fanspeedSpinner;
	
	@BindView(R.id.condModeSpinner)
	Spinner condModeSpinner;
	
	@BindView(R.id.maxTemp)
	EditText maxTemp;
	
	@BindView(R.id.minTemp)
	EditText minTemp;
	
	@BindView(R.id.showCentigrade)
	CheckBox showCentigrade;
	
	@BindView(R.id.occDetection)
	CheckBox occDetection;
	
	@BindView(R.id.setTemp)
	EditText setTemp;
	
	@BindViews({R.id.relay1, R.id.relay2, R.id.relay3, R.id.relay4, R.id.relay5, R.id.relay6})
	List<Switch> relayList;
	
	@BindView(R.id.sendSeed)
	Button sendSeedBtn;
	
	@BindView(R.id.sendSettings)
	Button sendSettings;
	
	@BindView(R.id.sendControl)
	Button sendControl;
	
	int channelSelection = 0;
	int profileSlection = 0;
	int fanspeedSelection = 0;
	int condModeSelection = 0;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		getDialog().setTitle("SmarStat Test");
		View retVal = inflater.inflate(R.layout.fragment_test, container, false);
		ButterKnife.bind(this, retVal);
		return retVal;
		
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initChannelSpinner();
		//channelSpinner.setVisibility(View.GONE);
		initProfileSpinner();
		initFanspeedSpinner();
		initCondModeSpinner();
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
		
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
	}
	
	
	@Override
	public void onStop()
	{
		super.onStop();
		EventBus.getDefault().unregister(this);
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
	private UsbService                usbService;
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
	
	@Override
	public void onResume() {
		super.onResume();
		setFilters();  // Start listening notifications from UsbService
		startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
	}
	
	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(mUsbReceiver);
		getActivity().unbindService(usbConnection);
	}
	
	private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
		if (!UsbService.SERVICE_CONNECTED) {
			Intent startService = new Intent(getActivity(), service);
			if (extras != null && !extras.isEmpty()) {
				Set<String> keys = extras.keySet();
				for (String key : keys) {
					String extra = extras.getString(key);
					startService.putExtra(key, extra);
				}
			}
			getActivity().startService(startService);
		}
		Intent bindingIntent = new Intent(this.getActivity(), service);
		getActivity().bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
	}
	
	private void setFilters() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
		filter.addAction(UsbService.ACTION_NO_USB);
		filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
		filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
		filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
		getActivity().registerReceiver(mUsbReceiver, filter);
	}
	
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
	public void onBLEEvent(SerialEvent event)
	{
		Log.i(TAG, "Event Type: " + event.getSerialAction().name());
		if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_PORT)
		{
			byte[] data = (byte[]) event.getBytes();
			Log.i(TAG, "Data return size: " + data.length);
		}
	}
	
	private void initChannelSpinner() {
		
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),
				                                                           android.R.layout.simple_spinner_item, channels);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		channelSpinner.setAdapter(dataAdapter);
		channelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				channelSelection = position > 0 ? position : 4;
				Globals.getInstance().getSmartNode().setMeshAddress(Short.parseShort(channels.get(channelSelection)));
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});
		
	}
	
	private void initProfileSpinner() {
		
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),
				                                                           android.R.layout.simple_spinner_item, profiles);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		profileSpinner.setAdapter(dataAdapter);
		profileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				profileSlection = position > 0 ? position -1 : 0;
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});
	}
	
	private void initFanspeedSpinner() {
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),
				                                                           android.R.layout.simple_spinner_item, fanSpeed);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		fanspeedSpinner.setAdapter(dataAdapter);
		fanspeedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				fanspeedSelection = position > 0 ? position -1 : 0;
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});
		
	}
	
	private void initCondModeSpinner() {
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),
				                                                           android.R.layout.simple_spinner_item, condMode);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		condModeSpinner.setAdapter(dataAdapter);
		condModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				condModeSelection = position > 0 ? position -1 : 0  ;
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});
		
	}
	
	
	@OnClick(R.id.sendSeed)
	public void sendSeed() {
		
		/*CcuToCmOverUsbDatabaseSeedSnMessage_t msg = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
		msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
		msg.smartNodeAddress.set(Globals.getInstance().getSmartNode().getMeshAddress());
		try
		{
			msg.putEncrptionKey(EncryptionPrefs.getEncryptionKey());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		msg.controls.digitalOut1.set((short)1);
		msg.controls.digitalOut2.set((short)1);*/
		
		
		short maxT ,minT, setT;
		try
		{
			maxT = parseShort(maxTemp.getText().toString());
			minT = parseShort(minTemp.getText().toString());
			setT = parseShort(setTemp.getText().toString());
		} catch (Exception e) {
			e.printStackTrace();
			maxT = (short) 80;
			minT = (short) 65;
			setT = (short) 75;
			Toast.makeText(TestFragment.this.getContext(), "Temperature not set , default values are used", Toast.LENGTH_SHORT).show();
		}
		
		SmartNode sn = Globals.getInstance().getSmartNode();
		CcuToCmOverUsbDatabaseSeedSmartStatMessage_t msg = new CcuToCmOverUsbDatabaseSeedSmartStatMessage_t();
		msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SMART_STAT);
		msg.address.set(sn.getMeshAddress());
		try
		{
			msg.putEncrptionKey(EncryptionPrefs.getEncryptionKey());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//settings
		msg.settings.roomName.set(roomName.getText().toString());
		msg.settings.profileBitmap.bitmap.set((short) (1 << profileSlection));
		msg.settings.maxUserTemp.set(maxT);
		msg.settings.minUserTemp.set(minT);
		msg.settings.showCentigrade.set(showCentigrade.isChecked() == true ? (short)1 : (short) 0);
		msg.settings.enableOccupancyDetection.set(occDetection.isChecked() == true ? (short)1 : (short)0);
		msg.settings.enabledRelaysBitmap.bitmap.set((short)0xFF); //Enable all relays
		
		//controls
		msg.controls.setTemperature.set(setT);
		msg.controls.fanSpeed.set(SmartStatFanSpeed_t.values()[fanspeedSelection]);
		msg.controls.conditioningMode.set(SmartStatConditioningMode_t.values()[condModeSelection]);
		
		msg.controls.relay1.set(((Switch)relayList.get(0)).isChecked() == true ? (short) 1: (short)0);
		msg.controls.relay2.set(((Switch)relayList.get(1)).isChecked() == true ? (short) 1: (short)0);
		msg.controls.relay3.set(((Switch)relayList.get(2)).isChecked() == true ? (short) 1: (short)0);
		msg.controls.relay4.set(((Switch)relayList.get(3)).isChecked() == true ? (short) 1: (short)0);
		msg.controls.relay5.set(((Switch)relayList.get(4)).isChecked() == true ? (short) 1: (short)0);
		msg.controls.relay6.set(((Switch)relayList.get(5)).isChecked() == true ? (short) 1: (short)0);
		
		if (usbService != null) {
			usbService.write(msg.getOrderedBuffer());
		}
		
	}
	
	@OnClick(R.id.sendSettings)
	public void sendSettings() {
		/*CcuToCmOverUsbSnSettingsMessage_t msg = new CcuToCmOverUsbSnSettingsMessage_t();
                msg.smartNodeAddress.set((short)4000);
                msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_SETTINGS);
                msg.settings.profileBitmap.bitmap.set((short) (1));*/


		SmartNode sn = Globals.getInstance().getSmartNode();
		CcuToCmOverUsbDatabaseSeedSmartStatMessage_t msg = new CcuToCmOverUsbDatabaseSeedSmartStatMessage_t();
		msg.messageType.set(MessageType.CM_TO_SMART_STAT_OVER_AIR_SMART_STAT_SETTINGS);
		msg.address.set(sn.getMeshAddress());
		
		msg.settings.roomName.set(roomName.getText().toString());
		msg.settings.profileBitmap.bitmap.set((short) (1 << profileSlection));
		msg.settings.maxUserTemp.set(parseShort(maxTemp.getText().toString()));
		msg.settings.minUserTemp.set(parseShort(minTemp.getText().toString()));
		msg.settings.showCentigrade.set(showCentigrade.isChecked() == true ? (short)1 : (short) 0);
		msg.settings.enableOccupancyDetection.set(occDetection.isChecked() == true ? (short)1 : (short)0);
		
		if (usbService != null) {
			usbService.write(msg.getOrderedBuffer());
		}
	}
	
	
	@OnClick(R.id.sendControl)
	public void sendControl() {
		
		
		SmartNode sn = Globals.getInstance().getSmartNode();
		CcuToCmOverUsbDatabaseSeedSmartStatMessage_t msg = new CcuToCmOverUsbDatabaseSeedSmartStatMessage_t();
		msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SMART_STAT_CONTROLS);
		msg.address.set(sn.getMeshAddress());
		short setT;
		try
		{
			setT = parseShort(setTemp.getText().toString());
		} catch (Exception e) {
			e.printStackTrace();
			setT = (short)75;
			Toast.makeText(TestFragment.this.getContext(), "Temperature not set , default values are used", Toast.LENGTH_SHORT).show();
		}
		
		msg.controls.setTemperature.set(setT);
		msg.controls.fanSpeed.set(SmartStatFanSpeed_t.values()[fanspeedSelection]);
		msg.controls.conditioningMode.set(SmartStatConditioningMode_t.values()[condModeSelection]);
		
		msg.controls.relay1.set(((Switch)relayList.get(0)).isChecked() == true ? (short) 1: (short)0);
		msg.controls.relay2.set(((Switch)relayList.get(1)).isChecked() == true ? (short) 1: (short)0);
		msg.controls.relay3.set(((Switch)relayList.get(2)).isChecked() == true ? (short) 1: (short)0);
		msg.controls.relay4.set(((Switch)relayList.get(3)).isChecked() == true ? (short) 1: (short)0);
		msg.controls.relay5.set(((Switch)relayList.get(4)).isChecked() == true ? (short) 1: (short)0);
		msg.controls.relay6.set(((Switch)relayList.get(5)).isChecked() == true ? (short) 1: (short)0);
		
		if (usbService != null) {
			usbService.write(msg.getOrderedBuffer());
		}
	}
}
