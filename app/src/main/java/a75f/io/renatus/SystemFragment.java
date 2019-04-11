package a75f.io.renatus;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;


/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SystemFragment extends Fragment implements AdapterView.OnItemSelectedListener
{
	private static final String TAG = "SystemFragment";
	SeekBar  sbComfortValue;
	EditText stageStatusNow;
	Spinner mSysSpinnerSchedule;
	
	TextView systemScheduleStatus;
	
	RadioButton systemOff;
	RadioButton systemAuto;
	RadioButton systemCool;
	RadioButton systemHeat;
	
	Spinner targetMaxInsideHumidity;
	Spinner targetMinInsideHumidity;
	
	CheckBox cbCompHumidity;
	CheckBox cbDemandResponse;
	
	RadioGroup systemModeRg;
	public SystemFragment()
	{
	}
	
	
	public static SystemFragment newInstance()
	{
		return new SystemFragment();
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
		systemScheduleStatus = view.findViewById(R.id.systemSchedule);
		
		/*mSysSpinnerSchedule = view.findViewById(R.id.sysSpinnerSchedule);
		mSysSpinnerSchedule.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Toast.makeText(SystemFragment.this.getActivity(), "Schedule edit popup", Toast.LENGTH_SHORT).show();
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					showScheduleDialog();
				}


				return true;
			}
		});*/
		systemModeRg = view.findViewById(R.id.systemConditioningMode);
		systemOff = view.findViewById(R.id.systemOff);
		systemAuto = view.findViewById(R.id.systemAuto);
		systemCool = view.findViewById(R.id.systemManualCool);
		systemHeat = view.findViewById(R.id.systemManualHeat);

		sbComfortValue = view.findViewById(R.id.systemComfortValue);
		
		targetMaxInsideHumidity = view.findViewById(R.id.targetMaxInsideHumidity);
		targetMinInsideHumidity = view.findViewById(R.id.targetMinInsideHumidity);
		
		cbCompHumidity = view.findViewById(R.id.cbCompHumidity);
		cbDemandResponse = view.findViewById(R.id.cbDemandResponse);
		
		if (L.ccu().systemProfile instanceof DefaultSystem) {
			systemOff.setEnabled(false);
			systemAuto.setEnabled(false);
			systemCool.setEnabled(false);
			systemHeat.setEnabled(false);
			sbComfortValue.setEnabled(false);
			targetMaxInsideHumidity.setEnabled(false);
			targetMinInsideHumidity.setEnabled(false);
			cbCompHumidity.setEnabled(false);
			cbDemandResponse.setEnabled(false);
			return;
		}
		systemOff.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				setUserIntentBackground("rtu and mode", SystemMode.OFF.ordinal());
			}
		});
		systemAuto.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				setUserIntentBackground("rtu and mode", SystemMode.AUTO.ordinal());
			}
		});
		systemCool.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				setUserIntentBackground("rtu and mode", SystemMode.COOLONLY.ordinal());
			}
		});
		systemHeat.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				setUserIntentBackground("rtu and mode", SystemMode.HEATONLY.ordinal());
			}
		});
		
		systemModeRg.clearCheck();
		
		systemAuto.setEnabled(L.ccu().systemProfile.isCoolingAvailable() && L.ccu().systemProfile.isHeatingAvailable());
		systemCool.setEnabled(L.ccu().systemProfile.isCoolingAvailable());
		systemHeat.setEnabled(L.ccu().systemProfile.isHeatingAvailable());
		
		final Handler handler = new Handler();
		handler.post(new Runnable() {
	                    @Override
	                    public void run() {
		                    SystemMode systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("rtu and mode")];
		                    Log.d("CCU_UI"," Set system Mode "+systemMode);
		                    switch (systemMode) {
			                    case OFF:
				                    systemOff.setChecked(true);
				                    break;
			                    case AUTO:
				                    systemAuto.setChecked(true);
				                    break;
			                    case COOLONLY:
				                    systemCool.setChecked(true);
				                    break;
			                    case HEATONLY:
				                    systemHeat.setChecked(true);
				                    break;
			                    default:
				                    systemOff.setChecked(true);
		                    }
	                    }
                    });
		
		sbComfortValue.setProgress(5 - (int)TunerUtil.readSystemUserIntentVal("desired and ci"));
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
		
		stageStatusNow = view.findViewById(R.id.stageStatusNow);
		
		double operatingMode = CCUHsApi.getInstance().readHisValByQuery("point and system and operating and mode");
		
		Log.d("CCU_UI", "operatingMode :" + operatingMode);
		
		handler.post(new Runnable() {
			@Override
			public void run() {
				stageStatusNow.setText(SystemController.State.values()[(int)operatingMode].name());
			}
		});
		
		
		
		ArrayList<Double> zoroToHundred = new ArrayList<>();
		for (double val = 0;  val <= 100.0; val++)
		{
			zoroToHundred.add(val);
		}
		
		ArrayAdapter<Double> humidityAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
		humidityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		
		targetMinInsideHumidity.setAdapter(humidityAdapter);
		targetMaxInsideHumidity.setAdapter(humidityAdapter);
		
		targetMaxInsideHumidity.setSelection(humidityAdapter
				                                     .getPosition(TunerUtil.readSystemUserIntentVal("target and max and inside and humidity")), false);
		targetMinInsideHumidity.setSelection(humidityAdapter
				                                     .getPosition(TunerUtil.readSystemUserIntentVal("target and min and inside and humidity")), false);
		
		targetMinInsideHumidity.setOnItemSelectedListener(this);
		targetMaxInsideHumidity.setOnItemSelectedListener(this);
		
		cbCompHumidity.setChecked(TunerUtil.readSystemUserIntentVal("compensate and humidity") > 0);
		cbDemandResponse.setChecked(TunerUtil.readSystemUserIntentVal("demand and response") > 0);
		
		cbCompHumidity.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b)
			{
				setUserIntentBackground("compensate and humidity", b ? 1: 0);
			}
		});
		
		cbDemandResponse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b)
			{
				setUserIntentBackground("demand and response", b ? 1: 0);
			}
		});
		
		handler.post(new Runnable() {
			@Override
			public void run() {
				systemScheduleStatus.setText(ScheduleProcessJob.getSystemStatusString());
			}
		});
		
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
	                           long arg3)
	{
		double val = Double.parseDouble(arg0.getSelectedItem().toString());
		switch (arg0.getId())
		{
			case R.id.targetMaxInsideHumidity:
				setUserIntentBackground("target and max and inside and humidity", val);
				break;
			case R.id.targetMinInsideHumidity:
				setUserIntentBackground("target and min and inside and humidity", val);
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


}
