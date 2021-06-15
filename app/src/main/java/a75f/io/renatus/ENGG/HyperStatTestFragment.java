package a75f.io.renatus.ENGG;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.google.protobuf.ByteString;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import a75f.io.device.HyperStat;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.L;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.R;
import a75f.io.usbserial.SerialAction;
import a75f.io.usbserial.SerialEvent;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by samjithsadasivan isOn 8/9/17.
 */

public class HyperStatTestFragment extends BaseDialogFragment
{
	private static final String TAG = DialogFragment.class.getSimpleName();

	List<String> channels = Arrays.asList("Select Channel", "1000", "2000", "3000", "4000", "5000", "6000", "7000", "8000", "9000");

	List<String> profiles = Arrays.asList("Select Profile", "CPU", "HPU", "2PFCU", "4PFCU", "Sense");

	List<String> fanSpeed = Arrays.asList("Select Fan Speed", "Off", "Auto", "Low", "High");

	List<String> condMode = Arrays.asList("Select Conditioning Mode", "Off" ,"Auto", "Heating", "Cooling");


	public static HyperStatTestFragment newInstance(){
		return new HyperStatTestFragment();
	}

	public HyperStatTestFragment(){

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

	int channelSelection = 0;
	int profileSlection = 0;
	int fanspeedSelection = 0;
	int condModeSelection = 0;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		getDialog().setTitle("SmarStat Test");
		View retVal = inflater.inflate(R.layout.fragment_smartstattest, container, false);
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
	
	@Override
	public String getIdString()
	{
		return "HyperStatTestFragment";
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
				//TODO: fixme
				//Globals.getInstance().getSmartNode().setMeshAddress(Short.parseShort(channels.get(channelSelection)));
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
		
		HyperStat.HyperStatSettingsMessage_t settings = HyperStat.HyperStatSettingsMessage_t.newBuilder()
		                                                                                    .setRoomName("Protobuf Test Room")
		                                                                                    .setHeatingDeadBand(20)
		                                                                                    .setCoolingDeadBand(20)
																							.build();
		
		HyperStat.HyperStatControlsMessage_t controls = HyperStat.HyperStatControlsMessage_t.newBuilder()
		                                                                                    .setAnalogOut1(HyperStat.HyperStatAnalogOutputControl_t.newBuilder().setPercent(80))
		                                                                                    .setRelay1(true)
		                                                                                    .setRelay3(true)
		                                                                                    .setRelay5(false)
		                                                                                    .setSetTempCooling(740)
		                                                                                    .setSetTempHeating(690)
		                                                                                    .build();
		
		
		HyperStat.HyperStatCcuDatabaseSeedMessage_t seed = HyperStat.HyperStatCcuDatabaseSeedMessage_t.newBuilder()
		                                                                                              .setAddress(7000)
		                                                                                        .setEncryptionKey(
			                                                                                        ByteString.copyFrom(
			                                                                                        L.getEncryptionKey()))
		                                                                                        .setSerializedSettingsData(settings.toByteString())
		                                                                                        .setSerializedControlsData(controls.toByteString())
		                                                                                        .build();
		
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			seed.writeTo(os);
			sendMessageOverUsb((byte)MessageType.HYPERSTAT_CCU_DATABASE_SEED_MESSAGE.ordinal(), os);
			Log.d("CCU_SERIAL", "Send Proto Buf Message " + seed);
			
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	@OnClick(R.id.sendSettings)
	public void sendSettings() {
		HyperStat.HyperStatSettingsMessage_t settings = HyperStat.HyperStatSettingsMessage_t.newBuilder()
		                                                                                    .setRoomName("Protobuf Test Room")
		                                                                                    .setHeatingDeadBand(20)
		                                                                                    .setCoolingDeadBand(20)
		                                                                                    .build();
		
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			settings.writeTo(os);
			sendMessageOverUsb((byte)MessageType.HYPERSTAT_SETTINGS_MESSAGE.ordinal(), os);
			Log.d("CCU_SERIAL", "Send Proto Buf Message " + settings);
			
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@OnClick(R.id.sendFota)
	public void sendFirmwareMetadata() {
		Toast.makeText(getContext(), "Not Supported Yet!!", Toast.LENGTH_SHORT).show();
	}
	
	@OnClick(R.id.sendControl)
	public void sendControl() {
		
		HyperStat.HyperStatControlsMessage_t controls = HyperStat.HyperStatControlsMessage_t.newBuilder()
		                                                                                    .setAnalogOut1(HyperStat.HyperStatAnalogOutputControl_t.newBuilder().setPercent(80))
		                                                                                    .setRelay1(true)
		                                                                                    .setRelay3(true)
		                                                                                    .setRelay5(false)
		                                                                                    .setSetTempCooling(740)
		                                                                                    .setSetTempHeating(690)
		                                                                                    .build();
		
		try
		{
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			controls.writeTo(os);
			sendMessageOverUsb((byte)MessageType.HYPERSTAT_SETTINGS_MESSAGE.ordinal(), os);
			Log.d("CCU_SERIAL", "Send Proto Buf Message " + controls);
			
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void sendMessageOverUsb(byte msgType, ByteArrayOutputStream os) {
		
		byte[] tempBytes = os.toByteArray();
		byte[] msgBytes = new byte[tempBytes.length+1];
		
		//CM currently supports both legacy byte array and protobuf encoding. Message type is kept as raw byte at the start to help CM determine which type
		//of decoding to be used.
		msgBytes[0] = msgType;
		
		System.arraycopy(tempBytes, 0, msgBytes, 1, tempBytes.length);
		
		LSerial.getInstance().sendSerialBytesToCM(msgBytes);
		Log.d("CCU_SERIAL", Arrays.toString(msgBytes));
		
	}
	
}
