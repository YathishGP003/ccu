package a75f.io.renatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import a75f.io.logger.CcuLog;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.helper.StringUtil;

import a75f.io.api.haystack.modbus.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.oao.OAOEquip;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.vav.VavIERtu;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.pubnub.UpdatePointHandler;
import a75f.io.logic.pubnub.ZoneDataInterface;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.modbusbox.EquipsManager;
import a75f.io.renatus.modbus.ZoneRecyclerModbusParamAdapter;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.HeartBeatUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.views.OaoArc;

import static a75f.io.logic.jobs.ScheduleProcessJob.ACTION_STATUS_CHANGE;


/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SystemFragment extends Fragment implements AdapterView.OnItemSelectedListener, ZoneDataInterface
{
	private static final String TAG = "SystemFragment";
	SeekBar  sbComfortValue;
	
	Spinner targetMaxInsideHumidity;
	Spinner targetMinInsideHumidity;
	ToggleButton tbCompHumidity;
	ToggleButton tbDemandResponse;
	ToggleButton tbSmartPrePurge;
	ToggleButton tbSmartPostPurge;
	ToggleButton tbEnhancedVentilation;
	LinearLayout purgeLayout,mainLayout;

	TextView energyMeterModelDetails;
	RecyclerView energyMeterParams;
	private TextView moduleStatusEmr;
	private TextView lastUpdatedEmr;

	RecyclerView btuMeterParams;
	TextView btuMeterModelDetails;
	private TextView moduleStatusBtu;
	private TextView lastUpdatedBtu;

	private TextView updatedTimeOao;

	
	int spinnerInit = 0;
	boolean minHumiditySpinnerReady = false;
	boolean maxHumiditySpinnerReady = false;

	View rootView;
	TextView ccuName;
	TextView profileTitle;
	//TODO uncomment for acctuall prod releasee, commenting it out for Automation test
	//SystemNumberPicker systemModePicker;
	NumberPicker systemModePicker;
	
	TextView occupancyStatus;
	TextView equipmentStatus;
	OaoArc oaoArc;
	
	boolean coolingAvailable = false;
	boolean heatingAvailable = false;
	
	ArrayList<String> modesAvailable = new ArrayList<>();
	ArrayAdapter<Double> humidityAdapter;
	private TextView IEGatewayOccupancyStatus;
	private TextView GUIDDetails;
	private LinearLayout IEGatewayDetail;
	Prefs prefs;
	public SystemFragment()
	{
	}
	
	
	public static SystemFragment newInstance()
	{
		return new SystemFragment();
	}

	public void refreshScreen(String id)
	{
		CcuLog.i("UI_PROFILING", "SystemFragment.refreshScreen");
		
		if(getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (!(L.ccu().systemProfile instanceof DefaultSystem)) {
						checkForOao();
						fetchPoints();
						if(rootView != null){
							configEnergyMeterDetails(rootView);
							configBTUMeterDetails(rootView);
						}
					}

				}
			});
		}
		CcuLog.i("UI_PROFILING", "SystemFragment.refreshScreen Done");
		
	}
	public void refreshDesiredTemp(String nodeAddress,String  coolDt, String heatDt){}
	public void refreshScreenbySchedule(String nodeAddress, String equipId, String zoneId){}
	public void updateTemperature(double currentTemp, short nodeAddress){}
	public void updateSensorValue(short nodeAddress){}
	public void refreshHeartBeatStatus(String id){}
	@Override
	public void onResume() {
		super.onResume();
		checkForOao();
		fetchPoints();
		profileTitle.setText(L.ccu().systemProfile.getProfileName());

		if(getUserVisibleHint()) {
            fetchPoints();
            if (prefs.getBoolean("REGISTRATION")) {
                UpdatePointHandler.setSystemDataInterface(this);
            }
        }
	}

	@Override
	public void onPause() {
		super.onPause();
		if (prefs.getBoolean("REGISTRATION")) {
			UpdatePointHandler.setSystemDataInterface(null);
		}
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if(isVisibleToUser) {
			UpdatePointHandler.setSystemDataInterface(this);
		} else {
			UpdatePointHandler.setSystemDataInterface(null);
		}
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.fragment_system_setting, container, false);
		return rootView;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		CcuLog.i("UI_PROFILING", "SystemFragment.onViewCreated");
		
		prefs = new Prefs(getActivity());
		ccuName = view.findViewById(R.id.ccuName);
		HashMap ccu = CCUHsApi.getInstance().read("device and ccu");
		ccuName.setText(ccu.get("dis").toString());
		profileTitle = view.findViewById(R.id.profileTitle);
		oaoArc = view.findViewById(R.id.oaoArc);
		purgeLayout = view.findViewById(R.id.purgelayout);
		systemModePicker = view.findViewById(R.id.systemModePicker);
		mainLayout = view.findViewById(R.id.main_layout);

		if (L.ccu().systemProfile != null) {
			coolingAvailable = L.ccu().systemProfile.isCoolingAvailable();
			heatingAvailable = L.ccu().systemProfile.isHeatingAvailable();
		}

		
		modesAvailable.add(SystemMode.OFF.displayName);
		if (coolingAvailable && heatingAvailable) {
			modesAvailable.add(SystemMode.AUTO.displayName);
		}
		if (coolingAvailable) {
			modesAvailable.add(SystemMode.COOLONLY.displayName);
		}
		if (heatingAvailable) {
			modesAvailable.add(SystemMode.HEATONLY.displayName);
		}


		systemModePicker.setMinValue(0);
		systemModePicker.setMaxValue(modesAvailable.size()-1);
		
		systemModePicker.setDisplayedValues(modesAvailable.toArray(new String[modesAvailable.size()]));
		systemModePicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

		//TODO we will comment out below two lines for prod release
		systemModePicker.setWrapSelectorWheel(false);

		
		
		systemModePicker.setOnScrollListener(new NumberPicker.OnScrollListener() {
			@Override
			public void onScrollStateChange(NumberPicker numberPicker, int scrollState) {
				if (scrollState == SCROLL_STATE_IDLE) {
					//Adding a dealy of 100ms as instant invocation of getVal() returns old value at times.
					new Handler().postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							if (numberPicker.getValue() != TunerUtil.readSystemUserIntentVal("conditioning and mode"))
							{
								setUserIntentBackground("conditioning and mode", SystemMode.getEnum(modesAvailable.get(numberPicker.getValue())).ordinal());
							}
						}
					}, 100);
				}
			}
		});
		//TODO Commented this out for Automation test, we will revoke for actual prod test
		/*systemModePicker.setOnScrollListener((view1, scrollState) -> {
			if (scrollState == SystemNumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
				//Adding a dealy of 100ms as instant invocation of getVal() returns old value at times.
				new Handler().postDelayed(() -> {
					if (systemModePicker.getValue() != TunerUtil.readSystemUserIntentVal("rtu and mode"))
					{
						setUserIntentBackground("rtu and mode", SystemMode.getEnum(modesAvailable.get(systemModePicker.getValue())).ordinal());
					}
				}, 100);
			}
		});*/
		
		occupancyStatus = view.findViewById(R.id.occupancyStatus);
		equipmentStatus = view.findViewById(R.id.equipmentStatus);
		IEGatewayOccupancyStatus = view.findViewById(R.id.IE_Gateway_Occupancy_Status);
		GUIDDetails = view.findViewById(R.id.GUID_Details);
		IEGatewayDetail = view.findViewById(R.id.ie_gateway_details);

		sbComfortValue = view.findViewById(R.id.systemComfortValue);
		
		targetMaxInsideHumidity = view.findViewById(R.id.targetMaxInsideHumidity);
		targetMinInsideHumidity = view.findViewById(R.id.targetMinInsideHumidity);
		CCUUiUtil.setSpinnerDropDownColor(targetMaxInsideHumidity,getContext());
		CCUUiUtil.setSpinnerDropDownColor(targetMinInsideHumidity,getContext());
		targetMinInsideHumidity.setOnTouchListener(new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			minHumiditySpinnerReady = true;
			return false;
		}
		});
		
		targetMaxInsideHumidity.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				maxHumiditySpinnerReady = true;
				return false;
			}
		});
		
		tbCompHumidity = view.findViewById(R.id.tbCompHumidity);
		tbDemandResponse = view.findViewById(R.id.tbDemandResponse);
		tbSmartPrePurge = view.findViewById(R.id.tbSmartPrePurge);
		tbSmartPostPurge = view.findViewById(R.id.tbSmartPostPurge);
		tbEnhancedVentilation = view.findViewById(R.id.tbEnhancedVentilation);
		updatedTimeOao = view.findViewById(R.id.last_updated_status_oao);

		tbCompHumidity.setEnabled(false);
		tbDemandResponse.setEnabled(false);

		energyMeterParams = view.findViewById(R.id.energyMeterParams);
		energyMeterModelDetails = view.findViewById(R.id.energyMeterModelDetails);
		moduleStatusEmr = view.findViewById(R.id.module_status_emr);
		lastUpdatedEmr = view.findViewById(R.id.last_updated_emr);
		configEnergyMeterDetails(view);

		/**
		 * init Modbus BTU meter  views
		 */
		btuMeterParams = view.findViewById(R.id.btuMeterParams);
		btuMeterModelDetails = view.findViewById(R.id.btuMeterModelDetails);
		moduleStatusBtu = view.findViewById(R.id.module_status_btu);
		lastUpdatedBtu = view.findViewById(R.id.last_updated_btu);
		configBTUMeterDetails(view);


		if (L.ccu().systemProfile instanceof DefaultSystem) {
			systemModePicker.setEnabled(false);
			sbComfortValue.setEnabled(false);

			ArrayList<Double> zoroToHundred = new ArrayList<>();
			for (double val = 0;  val <= 100.0; val++)
			{
				zoroToHundred.add(val);
			}
			humidityAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, zoroToHundred);
			humidityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			targetMinInsideHumidity.setAdapter(humidityAdapter);
			targetMinInsideHumidity.setSelection(0);
			targetMaxInsideHumidity.setAdapter(humidityAdapter);
			targetMaxInsideHumidity.setSelection(0);

			targetMaxInsideHumidity.setEnabled(false);
			targetMinInsideHumidity.setEnabled(false);
			tbCompHumidity.setEnabled(false);
			tbDemandResponse.setEnabled(false);
			tbSmartPrePurge.setEnabled(false);
			tbSmartPostPurge.setEnabled(false);
			tbEnhancedVentilation.setEnabled(false);
			purgeLayout.setVisibility(View.GONE);
			return;
		}
		
		sbComfortValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				sbComfortValue.setContentDescription(String.valueOf(seekBar.getProgress()));
				setUserIntentBackground("desired and ci",5 - seekBar.getProgress());
			}
		});


		double operatingMode = CCUHsApi.getInstance().readHisValByQuery("point and system and operating and mode");

		ArrayList<Double> zoroToHundred = new ArrayList<>();
		for (double val = 0;  val <= 100.0; val++)
		{
			zoroToHundred.add(val);
		}
		
		humidityAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, zoroToHundred);
		humidityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		
		targetMinInsideHumidity.setAdapter(humidityAdapter);
		targetMaxInsideHumidity.setAdapter(humidityAdapter);
		
		targetMinInsideHumidity.setOnItemSelectedListener(this);
		targetMaxInsideHumidity.setOnItemSelectedListener(this);
		
		tbCompHumidity.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b)
			{
				if (compoundButton.isPressed())
				{
					setUserIntentBackground("compensate and humidity", b ? 1 : 0);
				}
			}
		});

		tbDemandResponse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b)
			{
				if (compoundButton.isPressed())
				{
					setUserIntentBackground("demand and response", b ? 1 : 0);
				}
			}
		});
		tbSmartPrePurge.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b)
			{
				if (compoundButton.isPressed())
				{
					setUserIntentBackground("prePurge and enabled", b ? 1 : 0);
				}
			}
		});
		tbSmartPostPurge.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b)
			{
				if (compoundButton.isPressed())
				{
					setUserIntentBackground("postPurge and enabled", b ? 1 : 0);
				}
			}
		});
		tbEnhancedVentilation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b)
			{
				if (compoundButton.isPressed())
				{
					setUserIntentBackground("enhanced and ventilation and enabled", b ? 1 : 0);
				}
			}
		});
		getActivity().registerReceiver(occupancyReceiver, new IntentFilter(ACTION_STATUS_CHANGE));
		configWatermark();
		CcuLog.i("UI_PROFILING", "SystemFragment.onViewCreated Done");
		
	}

	private void checkForOao() {
		if (L.ccu().oaoProfile != null) {
			oaoArc.setVisibility(View.VISIBLE);
			purgeLayout.setVisibility(View.VISIBLE);
			tbSmartPrePurge.setChecked(TunerUtil.readSystemUserIntentVal("prePurge and enabled") > 0);
			tbSmartPostPurge.setChecked(TunerUtil.readSystemUserIntentVal("postPurge and enabled") > 0);
			tbEnhancedVentilation.setChecked(TunerUtil.readSystemUserIntentVal("enhanced and ventilation") > 0);
			ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and oao");

			if (equips != null && equips.size() > 0) {
				ArrayList<OAOEquip> equipList = new ArrayList<>();
				for (HashMap m : equips) {
					String nodeAddress = m.get("group").toString();
					equipList.add(new OAOEquip(ProfileType.OAO, Short.valueOf(nodeAddress)));
					updatedTimeOao.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
					oaoArc.updateStatus(HeartBeatUtil.isModuleAlive(nodeAddress));
				}

				double returnAirCO2 = equipList.get(0).getHisVal("return and air and co2 and sensor");
				double co2Threshold = equipList.get(0).getConfigNumVal("co2 and threshold");

				int angel = (int)co2Threshold / 20;
				if (angel < 0){
					angel = 0;
				} else if (angel > 2000){
					angel = 2000;
				}

				int progress = (int) returnAirCO2 / 20;
				if (progress < 0){
					progress = 0;
				} else if (progress > 2000){
					progress = 2000;
				}

				oaoArc.setProgress(progress);
				oaoArc.setData(angel,(int)returnAirCO2);
				oaoArc.setContentDescription(String.valueOf(returnAirCO2));
			}
		} else {
			RelativeLayout.LayoutParams layoutParams =(RelativeLayout.LayoutParams)systemModePicker.getLayoutParams();
			layoutParams.setMargins(0,300,0,0);
			systemModePicker.setLayoutParams(layoutParams);
			oaoArc.setVisibility(View.GONE);
			purgeLayout.setVisibility(View.GONE);
		}
	}

	public void fetchPoints()
	{
		if(getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					String colorHex = CCUUiUtil.getColorCode(getContext());
					String status = CCUHsApi.getInstance().readDefaultStrVal("system and status and message");
					//If the system status is not updated yet (within a minute of registering the device), generate a
					//default message.
					if (StringUtils.isEmpty(status)) {
						status = L.ccu().systemProfile.getStatusMessage();
					}

					if (L.ccu().systemProfile instanceof DefaultSystem) {
						equipmentStatus.setText(StringUtil.isBlank(status) ? "System is in gateway mode" : Html.fromHtml(status.replace("ON", "<font color='"+colorHex+"'>ON</font>")));
						occupancyStatus.setText("No Central equipment connected.");
						tbCompHumidity.setChecked(false);
						tbDemandResponse.setChecked(false);
						tbSmartPrePurge.setChecked(false);
						tbSmartPostPurge.setChecked(false);
						tbEnhancedVentilation.setChecked(false);
						sbComfortValue.setProgress(0);
						sbComfortValue.setContentDescription("0");
						targetMaxInsideHumidity.setSelection(humidityAdapter
								.getPosition(0.0), false);
						targetMinInsideHumidity.setSelection(humidityAdapter
								.getPosition(0.0), false);
					}else{
						systemModePicker.setValue((int) TunerUtil.readSystemUserIntentVal("conditioning and mode"));

						equipmentStatus.setText(StringUtil.isBlank(status)? Html.fromHtml("<font color='"+colorHex+"'>OFF</font>") : Html.fromHtml(status.replace("ON","<font color='"+colorHex+"'>ON</font>").replace("OFF","<font color='"+colorHex+"'>OFF</font>")));
						Log.i(TAG, "getSystemStatusString: Before system fragement");
						occupancyStatus.setText(ScheduleProcessJob.getSystemStatusString());
						tbCompHumidity.setChecked(TunerUtil.readSystemUserIntentVal("compensate and humidity") > 0);
						tbDemandResponse.setChecked(TunerUtil.readSystemUserIntentVal("demand and response") > 0);
						tbSmartPrePurge.setChecked(TunerUtil.readSystemUserIntentVal("prePurge and enabled") > 0);
						tbSmartPostPurge.setChecked(TunerUtil.readSystemUserIntentVal("postPurge and enabled") > 0);
						tbEnhancedVentilation.setChecked(TunerUtil.readSystemUserIntentVal("enhanced and ventilation and enabled") > 0);
						sbComfortValue.setProgress(5 - (int) TunerUtil.readSystemUserIntentVal("desired and ci"));
						sbComfortValue.setContentDescription(String.valueOf(5 - (int) TunerUtil.readSystemUserIntentVal("desired and ci")));

						targetMaxInsideHumidity.setSelection(humidityAdapter
								.getPosition(TunerUtil.readSystemUserIntentVal("target and max and inside and humidity")), false);
						targetMinInsideHumidity.setSelection(humidityAdapter
								.getPosition(TunerUtil.readSystemUserIntentVal("target and min and inside and humidity")), false);

						if(L.ccu().systemProfile instanceof VavIERtu){
							IEGatewayDetail.setVisibility(View.VISIBLE);
							IEGatewayOccupancyStatus.setText(getOccStatus());
							GUIDDetails.setText(CCUHsApi.getInstance().getSiteIdRef().toString());
						}
					}
				}
			});
		}
		
	}
	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
	                           long arg3)
	{
		double val = Double.parseDouble(arg0.getSelectedItem().toString());
		
		switch (arg0.getId())
		{
			case R.id.targetMaxInsideHumidity:
				if (maxHumiditySpinnerReady)
				{
					maxHumiditySpinnerReady = false;
					setUserIntentBackground("target and max and inside and humidity", val);
				}
				break;
			case R.id.targetMinInsideHumidity:
				if (minHumiditySpinnerReady)
				{
					minHumiditySpinnerReady = false;
					setUserIntentBackground("target and min and inside and humidity", val);
				}
				break;
		}
	}
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	}

	
	private void setUserIntentBackground(String query, double val) {
		
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground( final Void ... params ) {
				TunerUtil.writeSystemUserIntentVal(query, val);
				
				return null;
			}
			
			@Override
			protected void onPostExecute( final Void result ) {
				// continue what you are doing...
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void onDestroyView() {
		try {
			if (getActivity() != null){
				getActivity().unregisterReceiver(occupancyReceiver);
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		super.onDestroyView();
	}

	private final BroadcastReceiver occupancyReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null || intent.getAction() == null) {
				return;
			}
			if (getActivity() != null && isAdded()) {
				if (!(L.ccu().systemProfile instanceof DefaultSystem)) {
					fetchPoints();
				}
			}
		}
	};
	private void configEnergyMeterDetails(View view){
		List<EquipmentDevice> modbusDevices = getSystemLevelModBusDevices();;
		if(modbusDevices!=null&&modbusDevices.size()>0){
			EquipmentDevice  emDevice=null;
			for (int i = 0; i <modbusDevices.size() ; i++) {
				if(modbusDevices.get(i).getEquipType().equals("EMR")){
					emDevice = modbusDevices.get(i);
				}
			}

			if(emDevice==null)
				return;
			energyMeterParams.setVisibility(View.VISIBLE);
			energyMeterModelDetails.setVisibility(View.VISIBLE);
			moduleStatusEmr.setVisibility(View.VISIBLE);
			lastUpdatedEmr.setVisibility(View.VISIBLE);

			/**
			 * Assuming there is always only One Energy meter
			 */

			List<Parameter> parameterList = new ArrayList<>();
			if (Objects.nonNull(emDevice.getRegisters())) {
				for (Register registerTemp : emDevice.getRegisters()) {
					if (registerTemp.getParameters() != null) {
						for (Parameter p : registerTemp.getParameters()) {
							if (p.isDisplayInUI()) {
								p.setParameterDefinitionType(registerTemp.getParameterDefinitionType());
								parameterList.add(p);
							}
						}
					}
				}
			}
			String nodeAddress = String.valueOf(emDevice.getSlaveId());
			energyMeterModelDetails.setText(emDevice+ "("+emDevice.getEquipType() + nodeAddress + ")");
			GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2);
			energyMeterParams.setLayoutManager(gridLayoutManager);
			ZoneRecyclerModbusParamAdapter zoneRecyclerModbusParamAdapter = new ZoneRecyclerModbusParamAdapter(getContext(), emDevice.getEquipRef(), parameterList);
			energyMeterParams.setAdapter(zoneRecyclerModbusParamAdapter);
			TextView emrUpdatedTime = view.findViewById(R.id.last_updated_statusEM);
			emrUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
			TextView textViewModule = view.findViewById(R.id.module_status_emr);
			HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
		}

	}

	private List<EquipmentDevice> getSystemLevelModBusDevices(){
		return 	EquipsManager.getInstance().getAllMbEquips("SYSTEM");
	}

	private void configBTUMeterDetails(View view){
		List<EquipmentDevice> modbusDevices = getSystemLevelModBusDevices();
		if(modbusDevices!=null&&modbusDevices.size()>0){
			EquipmentDevice  btuDevice=null;
			for (int i = 0; i <modbusDevices.size() ; i++) {
				if(modbusDevices.get(i).getEquipType().equals("BTU")){
					btuDevice = modbusDevices.get(i);
				}
			}

			if(btuDevice==null)
				return;
			btuMeterParams.setVisibility(View.VISIBLE);
			btuMeterModelDetails.setVisibility(View.VISIBLE);
			moduleStatusBtu.setVisibility(View.VISIBLE);
			lastUpdatedBtu.setVisibility(View.VISIBLE);

			/**
			 * Assuming there is always only One BTU meter
			 */

			List<Parameter> parameterList = new ArrayList<>();
			if (Objects.nonNull(btuDevice.getRegisters())) {
				for (Register registerTemp : btuDevice.getRegisters()) {
					if (registerTemp.getParameters() != null) {
						for (Parameter p : registerTemp.getParameters()) {
							if (p.isDisplayInUI()) {
								p.setParameterDefinitionType(registerTemp.getParameterDefinitionType());
								parameterList.add(p);
							}
						}
					}
				}
			}
			String nodeAddress = String.valueOf(btuDevice.getSlaveId());
			btuMeterModelDetails.setText(btuDevice+ "("+btuDevice.getEquipType() + nodeAddress + ")");
			GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2);
			btuMeterParams.setLayoutManager(gridLayoutManager);
			ZoneRecyclerModbusParamAdapter zoneRecyclerModbusParamAdapter = new ZoneRecyclerModbusParamAdapter(getContext(), btuDevice.getEquipRef(), parameterList);
			btuMeterParams.setAdapter(zoneRecyclerModbusParamAdapter);
			TextView btuUpdatedTime = view.findViewById(R.id.last_updated_statusBTU);
			btuUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
			TextView textViewModule = view.findViewById(R.id.module_status_btu);
			HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
		}

	}

	private void configWatermark(){
		if(!BuildConfig.BUILD_TYPE.equals("daikin_prod")&&! CCUUiUtil.isDaikinThemeEnabled(getContext()))
			mainLayout.setBackgroundResource(R.drawable.bg_logoscreen);

	}
	private String getOccStatus(){
		HashMap point = CCUHsApi.getInstance().read("point and " +
				"system and ie and occStatus");
		if (!point.isEmpty()) {
			double occStatus = CCUHsApi.getInstance().readHisValById(point.get("id").toString());
			if (occStatus == 0) {
				return "Occupied";
			} else if (occStatus == 1) {
				return "Unoccupied";
			} else {
				return "Tenant Override";
			}
		}
		return "Unoccupied";
	}


}
