package a75f.io.renatus;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.util.Prefs;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SystemFragment extends Fragment implements AdapterView.OnItemSelectedListener, CCUHsApi.SystemDataInterface
{
	private static final String TAG = "SystemFragment";
	SeekBar  sbComfortValue;
	
	Spinner targetMaxInsideHumidity;
	Spinner targetMinInsideHumidity;
	
	//SwitchCompat tbCompHumidity;
	//SwitchCompat cbDemandResponse;
	ToggleButton tbCompHumidity;
	ToggleButton tbDemandResponse;


	
	int spinnerInit = 0;
	boolean minHumiditySpinnerReady = false;
	boolean maxHumiditySpinnerReady = false;
	
	
	TextView ccuName;
	TextView profileTitle;
	NumberPicker systemModePicker;
	
	TextView occupancyStatus;
	TextView equipmentStatus;
	
	boolean coolingAvailable = false;
	boolean heatingAvailable = false;
	
	ArrayList<String> modesAvailable = new ArrayList<>();
	ArrayAdapter<Double> humidityAdapter;
	Prefs prefs;
	public SystemFragment()
	{
	}
	
	
	public static SystemFragment newInstance()
	{
		return new SystemFragment();
	}

	public void refreshScreen()
	{
		if (!(L.ccu().systemProfile instanceof DefaultSystem))
		{
			fetchPoints();
		}
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		fetchPoints();
		if(prefs.getBoolean("REGISTRATION")) {
			CCUHsApi.setSystemDataInterface(this);
		}
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_system_setting, container, false);
		return rootView;
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		prefs = new Prefs(getActivity());
		ccuName = view.findViewById(R.id.ccuName);
		HashMap ccu = CCUHsApi.getInstance().read("device and ccu");
		ccuName.setText(ccu.get("dis").toString());
		profileTitle = view.findViewById(R.id.profileTitle);
		systemModePicker = view.findViewById(R.id.systemModePicker);
		coolingAvailable = L.ccu().systemProfile.isCoolingAvailable();
		heatingAvailable = L.ccu().systemProfile.isHeatingAvailable();
		
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
		systemModePicker.setWrapSelectorWheel(false);
		setDividerColor(systemModePicker);
		
		
		systemModePicker.setOnScrollListener(new NumberPicker.OnScrollListener() {
			@Override
			public void onScrollStateChange(NumberPicker numberPicker, int scrollState) {
				if (systemModePicker.getValue() != TunerUtil.readSystemUserIntentVal("rtu and mode")) {
					int newMode = systemModePicker.getValue();
					setUserIntentBackground("rtu and mode", SystemMode.getEnum(modesAvailable.get(newMode)).ordinal() );
				}
			}
		});
		
		occupancyStatus = view.findViewById(R.id.occupancyStatus);
		equipmentStatus = view.findViewById(R.id.equipmentStatus);
		
		sbComfortValue = view.findViewById(R.id.systemComfortValue);
		
		targetMaxInsideHumidity = view.findViewById(R.id.targetMaxInsideHumidity);
		targetMinInsideHumidity = view.findViewById(R.id.targetMinInsideHumidity);
		
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
				setUserIntentBackground("desired and ci",5 - seekBar.getProgress());
			}
		});
		
		profileTitle.setText(L.ccu().systemProfile.getProfileName());
		
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

		/*final Handler handler = new Handler();
		handler.post(new Runnable() {
			@Override
			public void run() {
				systemModePicker.setValue((int)TunerUtil.readSystemUserIntentVal("rtu and mode"));
				String status = L.ccu().systemProfile.getStatusMessage();
				equipmentStatus.setText(status.equals("") ? "OFF":status);
				occupancyStatus.setText(ScheduleProcessJob.getSystemStatusString());
				tbCompHumidity.setChecked(TunerUtil.readSystemUserIntentVal("compensate and humidity") > 0);
				tbDemandResponse.setChecked(TunerUtil.readSystemUserIntentVal("demand and response") > 0);
				sbComfortValue.setProgress(5 - (int)TunerUtil.readSystemUserIntentVal("desired and ci"));

				targetMaxInsideHumidity.setSelection(humidityAdapter
						                                     .getPosition(TunerUtil.readSystemUserIntentVal("target and max and inside and humidity")), false);
				targetMinInsideHumidity.setSelection(humidityAdapter
						                                     .getPosition(TunerUtil.readSystemUserIntentVal("target and min and inside and humidity")), false);
			}
		});*/

		fetchPoints();
		
	}

	public void fetchPoints()
	{
		if(getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					systemModePicker.setValue((int) TunerUtil.readSystemUserIntentVal("rtu and mode"));
					String status = L.ccu().systemProfile.getStatusMessage();
					equipmentStatus.setText(status.equals("") ? "OFF" : status);
					occupancyStatus.setText(ScheduleProcessJob.getSystemStatusString());
					tbCompHumidity.setChecked(TunerUtil.readSystemUserIntentVal("compensate and humidity") > 0);
					tbDemandResponse.setChecked(TunerUtil.readSystemUserIntentVal("demand and response") > 0);
					sbComfortValue.setProgress(5 - (int) TunerUtil.readSystemUserIntentVal("desired and ci"));

					targetMaxInsideHumidity.setSelection(humidityAdapter
							.getPosition(TunerUtil.readSystemUserIntentVal("target and max and inside and humidity")), false);
					targetMinInsideHumidity.setSelection(humidityAdapter
							.getPosition(TunerUtil.readSystemUserIntentVal("target and min and inside and humidity")), false);
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
	
	private void setDividerColor(NumberPicker picker) {
		Field[] numberPickerFields = NumberPicker.class.getDeclaredFields();
		for (Field field : numberPickerFields) {
			if (field.getName().equals("mSelectionDivider")) {
				field.setAccessible(true);
				try {
					field.set(picker, getResources().getDrawable(R.drawable.divider_np));
				} catch (IllegalArgumentException e) {
					Log.v("NP", "Illegal Argument Exception");
					e.printStackTrace();
				} catch (Resources.NotFoundException e) {
					Log.v("NP", "Resources NotFound");
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					Log.v("NP", "Illegal Access Exception");
					e.printStackTrace();
				}
				break;
			}
		}
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


}
