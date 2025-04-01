package a75f.io.renatus.ENGG;

import android.app.Dialog;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import a75f.io.device.MyStat;
import a75f.io.device.mesh.mystat.MyStatMsgSender;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.tuners.TunerUtil;
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

	List<String> profiles = Arrays.asList("Select Profile", "CPU", "HPU", "2PFCU", "4PFCU", "monitoring");

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

	@BindView(R.id.setTempCooling)
	EditText setTempCooling;
	
	@BindView(R.id.setTempHeating)
	EditText setTempHeating;

	@BindViews({R.id.relay1, R.id.relay2, R.id.relay3, R.id.relay4})
	List<Switch> relayList;
	
	@BindViews({R.id.analog1Out})
	List<Spinner> analogOutList;

	int channelSelection = 0;
	int profileSlection = 0;
	int fanspeedSelection = 0;
	int condModeSelection = 0;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		getDialog().setTitle("HyperStat Test");
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
		
		analogOutList.get(0).setAdapter(getAnalogOutAdapter());
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
			dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
		}
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
		CcuLog.i(TAG, "Event Type: " + event.getSerialAction().name());
		if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_PORT)
		{
			byte[] data = (byte[]) event.getBytes();
			CcuLog.i(TAG, "Data return size: " + data.length);
		}
	}

	private void initChannelSpinner() {

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this.getActivity(),
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

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this.getActivity(),
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
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this.getActivity(),
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
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this.getActivity(),
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

		MyStat.MyStatCcuDatabaseSeedMessage_t seed = MyStat.MyStatCcuDatabaseSeedMessage_t.newBuilder()
				.setEncryptionKey(
						ByteString.copyFrom(
								L.getEncryptionKey()))
				.setSerializedSettingsData(getMyStatSettingMessage().toByteString())
				.setSerializedControlsData(getMyStatControlMessage().toByteString())
				.build();

		CcuLog.i(L.TAG_CCU_SERIAL, "Send Test message " + seed.toString());
		MyStatMsgSender.INSTANCE.writeSeedMessage(seed, getChannelAddress(), true);
	}

	@OnClick(R.id.sendSettings)
	public void sendSettings() {

		CcuLog.i(L.TAG_CCU_SERIAL, "Send Test message " + getMyStatSettingMessage().toString());
		MyStatMsgSender.INSTANCE.writeSettingMessage(getMyStatSettingMessage(), getChannelAddress(), MessageType.MYSTAT_SETTINGS_MESSAGE,
		                                           false);
	}
	
	@OnClick(R.id.sendFota)
	public void sendFirmwareMetadata() {
		Toast.makeText(getContext(), "Not Supported !!", Toast.LENGTH_SHORT).show();
	}
	
	@OnClick(R.id.sendControl)
	public void sendControl() {
		
		CcuLog.i(L.TAG_CCU_SERIAL, "Send Test message " + getMyStatControlMessage().toString());
		MyStatMsgSender.INSTANCE.writeControlMessage(getMyStatControlMessage(), getChannelAddress(), MessageType.MYSTAT_CONTROLS_MESSAGE,
		                                           false);
	}

	private MyStat.MyStatControlsMessage_t getMyStatControlMessage() {
		return MyStat.MyStatControlsMessage_t.newBuilder()
				.setAnalogOut1(MyStat.MyStatAnalogOutputControl_t.newBuilder().setPercent(80))
				.setRelayBitmap(15)
				.setAnalogOut1(MyStat.MyStatAnalogOutputControl_t.newBuilder().setPercent(Integer.parseInt(analogOutList.get(0).getSelectedItem().toString())).build())
				.setSetTempCooling(10 * getSetTempCooling())
				.setSetTempHeating(10 * getSetTempHeating())
				.build();
	}

	private MyStat.MyStatSettingsMessage_t getMyStatSettingMessage() {
		return MyStat.MyStatSettingsMessage_t.newBuilder()
				.setRoomName(roomName.getText().toString())
				.setHeatingDeadBand(2)
				.setCoolingDeadBand(2)
				.setMinCoolingUserTemp((int) TunerUtil.readBuildingTunerValByQuery("cooling and user " +
						"and limit and min"))
				.setMaxCoolingUserTemp((int) TunerUtil.readBuildingTunerValByQuery("cooling and user " +
						"and limit and max"))
				.setMinHeatingUserTemp((int) TunerUtil.readBuildingTunerValByQuery("heating and user " +
						"and limit and min"))
				.setMaxHeatingUserTemp((int) TunerUtil.readBuildingTunerValByQuery("cooling and user " +
						"and limit and max"))
				.setTemperatureOffset(0)
				.build();
	}



	private int getSetTempCooling() {
		return setTempCooling.getText().toString().isEmpty() ? 74 :
			                            Integer.parseInt(setTempCooling.getText().toString());
	}
	
	private int getSetTempHeating() {
		return setTempHeating.getText().toString().isEmpty() ? 69 :
			                             Integer.parseInt(setTempHeating.getText().toString());
	}
	
	private int getChannelAddress() {
		return channelSpinner.getSelectedItemPosition() == 0 ? 7000 :
				                        Integer.parseInt(channelSpinner.getSelectedItem().toString());
	}
	
	private ArrayAdapter<Integer> getAnalogOutAdapter() {
		ArrayList<Integer> analogVal = new ArrayList<>();
		for (int pos = 0; pos <= 100; pos++) {
			analogVal.add(pos);
		}
		ArrayAdapter<Integer> analogValAdapter = new ArrayAdapter<>(getActivity(),
		                                                                 android.R.layout.simple_spinner_item, analogVal);
		analogValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		return analogValAdapter;
	}
}
