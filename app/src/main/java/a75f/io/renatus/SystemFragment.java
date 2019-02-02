package a75f.io.renatus;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;

import java.util.ArrayList;

import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SystemFragment extends Fragment implements AdapterView.OnItemSelectedListener
{
	
	
	RadioButton systemOff;
	RadioButton systemAuto;
	RadioButton systemCool;
	RadioButton systemHeat;
	SeekBar  sbComfortValue;
	EditText stageStatusNow;
	
	Spinner targetMaxInsideHumidity;
	Spinner targetMinInsideHumidity;
	
	CheckBox cbHumidifier;
	CheckBox cbDehumidifier;
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
		systemOff = view.findViewById(R.id.systemOff);
		systemAuto = view.findViewById(R.id.systemAuto);
		systemCool = view.findViewById(R.id.systemManualCool);
		systemHeat = view.findViewById(R.id.systemManualHeat);
		sbComfortValue = view.findViewById(R.id.systemComfortValue);
		
		targetMaxInsideHumidity = view.findViewById(R.id.targetMaxInsideHumidity);
		targetMinInsideHumidity = view.findViewById(R.id.targetMinInsideHumidity);
		
		cbHumidifier = view.findViewById(R.id.cbHumidification);
		cbHumidifier.setVisibility(View.INVISIBLE);
		cbDehumidifier = view.findViewById(R.id.cbDehumidification);
		cbDehumidifier.setVisibility(View.INVISIBLE);
		
		if (L.ccu().systemProfile instanceof DefaultSystem) {
			systemOff.setEnabled(false);
			systemAuto.setEnabled(false);
			systemCool.setEnabled(false);
			systemHeat.setEnabled(false);
			sbComfortValue.setEnabled(false);
			targetMaxInsideHumidity.setEnabled(false);
			targetMinInsideHumidity.setEnabled(false);
			cbHumidifier.setEnabled(false);
			cbDehumidifier.setEnabled(false);
			return;
		}
		SystemMode systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("rtu and mode")];
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
		stageStatusNow.setText(VavSystemController.getInstance().getSystemState().name());
		
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
				                                     .getPosition(TunerUtil.readSystemUserIntentVal("target and max and inside and humidity")));
		targetMinInsideHumidity.setSelection(humidityAdapter
				                                     .getPosition(TunerUtil.readSystemUserIntentVal("target and min and inside and humidity")));
		
		targetMinInsideHumidity.setOnItemSelectedListener(this);
		targetMaxInsideHumidity.setOnItemSelectedListener(this);
		
		/*cbHumidifier.setChecked(TunerUtil.readSystemUserIntentVal("enable and humidifier") > 0);
		cbHumidifier.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b)
			{
				setUserIntentBackground("enable and humidifier", b == true ? 1 : 0);
			}
		});
		
		cbDehumidifier.setChecked(TunerUtil.readSystemUserIntentVal("enable and dehumidifier") > 0);
		cbDehumidifier.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b)
			{
				setUserIntentBackground("enable and dehumidifier", b == true ? 1 : 0);
			}
		});*/
	}
	
	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
	                           long arg3)
	{
		double val = Double.parseDouble(arg0.getSelectedItem().toString());
		switch (arg0.getId())
		{
			case R.id.targetMaxInsideHumidity:
				setUserIntentBackground("target and min and inside and humidity", val);
				break;
			case R.id.targetMinInsideHumidity:
				setUserIntentBackground("target and max and inside and humidity", val);
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
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
	}
	

}
