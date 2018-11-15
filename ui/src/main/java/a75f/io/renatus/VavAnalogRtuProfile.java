package a75f.io.renatus;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TableLayout;

import java.util.ArrayList;

import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.VavAnalogRtu;
import a75f.io.logic.tuners.SystemTunerUtil;
import a75f.io.logic.tuners.TunerConstants;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 11/6/18.
 */

public class VavAnalogRtuProfile extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener
{
    @BindView(R.id.ahu) TableLayout     ahu;
	
	@BindView(R.id.coolingSatMin) Spinner coolingSatMin;
	@BindView(R.id.coolingSatMax) Spinner coolingSatMax;
	
	@BindView(R.id.heatingSatMin) Spinner heatingSatMin;
	@BindView(R.id.heatingSatMax) Spinner heatingSatMax;
	
	@BindView(R.id.co2Min) Spinner co2Min;
	@BindView(R.id.co2Max) Spinner co2Max;
	@BindView(R.id.spMin) Spinner spMin;
	@BindView(R.id.spMax) Spinner spMax;
    
    @BindView(R.id.ahuAnalog1Min) Spinner analog1Min;
    @BindView(R.id.ahuAnalog1Max) Spinner analog1Max;
    @BindView(R.id.ahuAnalog2Min) Spinner analog2Min;
    @BindView(R.id.ahuAnalog2Max) Spinner analog2Max;
    @BindView(R.id.ahuAnalog3Min) Spinner analog3Min;
    @BindView(R.id.ahuAnalog3Max) Spinner analog3Max;
	@BindView(R.id.ahuAnalog4Min) Spinner analog4Min;
	@BindView(R.id.ahuAnalog4Max) Spinner analog4Max;
    
    @BindView(R.id.ahuAnalog1Cb) CheckBox ahuAnalog1Cb;
    @BindView(R.id.ahuAnalog2Cb) CheckBox ahuAnalog2Cb;
    @BindView(R.id.ahuAnalog3Cb) CheckBox ahuAnalog3Cb;
	@BindView(R.id.ahuAnalog4Cb) CheckBox ahuAnalog4Cb;
    
    @BindView(R.id.ahuAnalog1Test) Spinner ahuAnalog1Test;
    @BindView(R.id.ahuAnalog2Test) Spinner ahuAnalog2Test;
    @BindView(R.id.ahuAnalog3Test) Spinner ahuAnalog3Test;
	@BindView(R.id.ahuAnalog4Test) Spinner ahuAnalog4Test;
    
    
    public static VavAnalogRtuProfile newInstance()
    {
        return new VavAnalogRtuProfile();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_analogrtu, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
	    if (!(L.ccu().systemProfile instanceof VavAnalogRtu)) {
		    L.ccu().systemProfile = new VavAnalogRtu();
	    }
		setupTempLimitSelectors();
		setupAnalogLimitSelectors();
	    VavAnalogRtu p = (VavAnalogRtu) L.ccu().systemProfile;
	    ahuAnalog1Cb.setChecked(p.analog1Enabled);
	    ahuAnalog2Cb.setChecked(p.analog2Enabled);
	    ahuAnalog3Cb.setChecked(p.analog3Enabled);
	    ahuAnalog4Cb.setChecked(p.analog4Enabled);
	    
	    ahuAnalog1Cb.setOnCheckedChangeListener(this);
	    ahuAnalog2Cb.setOnCheckedChangeListener(this);
	    ahuAnalog3Cb.setOnCheckedChangeListener(this);
	    ahuAnalog4Cb.setOnCheckedChangeListener(this);
	}
	
	private void setupTempLimitSelectors() {
  
		ArrayList<Double> coolingSatArray = new ArrayList<>();
		for (double pos = 55; pos <= 65; pos++){
			coolingSatArray.add(pos);
		}
		
		ArrayList<Double> heatingSatArray = new ArrayList<>();
		for (double pos = 75; pos <= 100; pos++){
			heatingSatArray.add(pos);
		}
		
		ArrayList<Double> co2Array = new ArrayList<>();
		for (double pos = 800; pos <= 1000; pos+=10){
			co2Array.add(pos);
		}
		ArrayList<Double> spArray = new ArrayList<>();
		for (double pos = 0.5; pos <= 1.5; pos +=0.1){
			spArray.add(Math.round(pos * 100D) / 100D);
		}
		
		ArrayAdapter<Double> coolingSatMinAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, coolingSatArray);
		coolingSatMinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		ArrayAdapter<Double> coolingSatMaxAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, coolingSatArray);
		coolingSatMaxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		ArrayAdapter<Double> heatingSatMinAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, heatingSatArray);
		coolingSatMinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		ArrayAdapter<Double> heatingSatMaxAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, heatingSatArray);
		coolingSatMaxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		
		ArrayAdapter<Double> co2MinAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, co2Array);
		co2MinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		ArrayAdapter<Double> co2MaxAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, co2Array);
		co2MaxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		ArrayAdapter<Double> spMinAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, spArray);
		spMinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		ArrayAdapter<Double> spMaxAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, spArray);
		spMaxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		
		coolingSatMin.setAdapter(coolingSatMinAdapter);
		coolingSatMin.setSelection(coolingSatMinAdapter.getPosition(SystemTunerUtil.getTuner(
											"coolingSat", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		coolingSatMax.setAdapter(coolingSatMaxAdapter);
		coolingSatMax.setSelection(coolingSatMaxAdapter.getPosition(SystemTunerUtil.getTuner(
											"coolingSat", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		heatingSatMin.setAdapter(heatingSatMinAdapter);
		heatingSatMin.setSelection(heatingSatMinAdapter.getPosition(SystemTunerUtil.getTuner(
											"heatingSat", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		heatingSatMax.setAdapter(heatingSatMaxAdapter);
		heatingSatMax.setSelection(heatingSatMaxAdapter.getPosition(SystemTunerUtil.getTuner(
											"heatingSat", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		co2Min.setAdapter(co2MinAdapter);
		co2Min.setSelection(co2MinAdapter.getPosition(SystemTunerUtil.getTuner(
				"co2Target", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		co2Max.setAdapter(co2MaxAdapter);
		co2Max.setSelection(co2MaxAdapter.getPosition(SystemTunerUtil.getTuner(
				"co2Target", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		spMin.setAdapter(spMinAdapter);
		spMin.setSelection(spMinAdapter.getPosition(SystemTunerUtil.getTuner(
				"spTarget", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		spMax.setAdapter(spMaxAdapter);
		spMax.setSelection(spMaxAdapter.getPosition(SystemTunerUtil.getTuner(
				"spTarget", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		coolingSatMin.setOnItemSelectedListener(this);
		coolingSatMax.setOnItemSelectedListener(this);
		heatingSatMin.setOnItemSelectedListener(this);
		heatingSatMax.setOnItemSelectedListener(this);
		co2Min.setOnItemSelectedListener(this);
		co2Max.setOnItemSelectedListener(this);
		spMin.setOnItemSelectedListener(this);
		spMax.setOnItemSelectedListener(this);
	}
	
	private void setupAnalogLimitSelectors() {
		ArrayList<Integer> analogArray = new ArrayList<>();
		for (int a = 0; a <= 10; a++)
		{
			analogArray.add(a);
		}
		ArrayAdapter<Integer> analogAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, analogArray);
		analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		
		analog1Min.setAdapter(analogAdapter);
		analog1Min.setSelection(analogAdapter.getPosition((int)SystemTunerUtil.getTuner(
													"analog1", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		analog1Max.setAdapter(analogAdapter);
		analog1Max.setSelection(analogAdapter.getPosition((int)SystemTunerUtil.getTuner(
													"analog1", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		analog2Min.setAdapter(analogAdapter);
		analog2Min.setSelection(analogAdapter.getPosition((int)SystemTunerUtil.getTuner(
													"analog2", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		analog2Max.setAdapter(analogAdapter);
		analog2Max.setSelection(analogAdapter.getPosition((int)SystemTunerUtil.getTuner(
													"analog2", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		analog3Min.setAdapter(analogAdapter);
		analog3Min.setSelection(analogAdapter.getPosition((int)SystemTunerUtil.getTuner(
													"analog3", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		analog3Max.setAdapter(analogAdapter);
		analog3Max.setSelection(analogAdapter.getPosition((int)SystemTunerUtil.getTuner(
													"analog3", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		analog4Min.setAdapter(analogAdapter);
		analog4Min.setSelection(analogAdapter.getPosition((int)SystemTunerUtil.getTuner(
				"analog4", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		analog4Max.setAdapter(analogAdapter);
		analog4Max.setSelection(analogAdapter.getPosition((int)SystemTunerUtil.getTuner(
				"analog4", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		
		/*String[] analogTypes = {"0-10V"};
		
		ArrayAdapter<String> analogTypeAdapter = new ArrayAdapter<> (getActivity(),R.layout.spinner_dropdown_item, analogTypes);
		analogTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog1Type.setAdapter(analogTypeAdapter);
		ahuAnalog2Type.setAdapter(analogTypeAdapter);
		ahuAnalog3Type.setAdapter(analogTypeAdapter);*/
		
		
		ArrayList<Double> coolingTestArray = new ArrayList<>();
		for (double sat = 55.0;  sat <= 65.0; sat++)
		{
			coolingTestArray.add(sat);
		}
		ArrayAdapter<Double> coolingSatTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, coolingTestArray);
		coolingSatTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog1Test.setAdapter(coolingSatTestAdapter);
		
		ArrayList<Double> heatingTestArray = new ArrayList<>();
		for (double sat = 75.0;  sat <= 100.0; sat++)
		{
			heatingTestArray.add(sat);
		}
		ArrayAdapter<Double> heatingSatTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, heatingTestArray);
		heatingSatTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog2Test.setAdapter(heatingSatTestAdapter);
		
		ArrayList<Double> co2TestArray = new ArrayList<>();
		for (double co2 = 800.0;  co2 <= 1000.0; co2++)
		{
			co2TestArray.add(co2);
		}
		ArrayAdapter<Double> co2TestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, co2TestArray);
		co2TestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog3Test.setAdapter(co2TestAdapter);
		
		ArrayList<Double> spTestArray = new ArrayList<>();
		for (double sp = 0.5;  sp <= 1.5; sp=sp+0.1)
		{
			spTestArray.add(Math.round(sp * 100D) / 100D);
		}
		ArrayAdapter<Double> spTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, spTestArray);
		spTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog4Test.setAdapter(spTestAdapter);
		
		
		analog1Min.setOnItemSelectedListener(this);
		analog1Max.setOnItemSelectedListener(this);
		analog2Min.setOnItemSelectedListener(this);
		analog2Max.setOnItemSelectedListener(this);
		analog3Min.setOnItemSelectedListener(this);
		analog3Max.setOnItemSelectedListener(this);
		analog4Min.setOnItemSelectedListener(this);
		analog4Max.setOnItemSelectedListener(this);
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		VavAnalogRtu p = (VavAnalogRtu) L.ccu().systemProfile;
		switch (buttonView.getId())
		{
			case R.id.ahuAnalog1Cb:
				p.analog1Enabled = ahuAnalog1Cb.isChecked();
				break;
			case R.id.ahuAnalog2Cb:
				p.analog2Enabled = ahuAnalog2Cb.isChecked();
				break;
			case R.id.ahuAnalog3Cb:
				p.analog3Enabled = ahuAnalog3Cb.isChecked();
				break;
			case R.id.ahuAnalog4Cb:
				p.analog4Enabled = ahuAnalog4Cb.isChecked();
				break;
		}
	}
	
	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
	                           long arg3)
	{
		double val = Double.parseDouble(arg0.getSelectedItem().toString());
		switch (arg0.getId())
		{
			case R.id.coolingSatMin:
				setTunerBackground("coolingSat", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.coolingSatMax:
				setTunerBackground("coolingSat", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.heatingSatMin:
				setTunerBackground("heatingSat", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.heatingSatMax:
				setTunerBackground("heatingSat", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.co2Min:
				setTunerBackground("co2Target", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.co2Max:
				setTunerBackground("co2Target", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.spMin:
				setTunerBackground("spTarget", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.spMax:
				setTunerBackground("spTarget", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog1Min:
				setTunerBackground("analog1", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog1Max:
				setTunerBackground("analog1", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog2Min:
				setTunerBackground("analog2", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog2Max:
				setTunerBackground("analog2", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog3Min:
				setTunerBackground("analog3", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, arg2);
				break;
			case R.id.ahuAnalog3Max:
				setTunerBackground("analog3", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog4Min:
				setTunerBackground("analog4", "min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog4Max:
				setTunerBackground("analog4", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
		}
	}
	
	private void setTunerBackground(String analog, String type, int level, double val) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground( final Void ... params ) {
				SystemTunerUtil.setTuner(analog, type, level, val);
				
				return null;
			}
			
			@Override
			protected void onPostExecute( final Void result ) {
				// continue what you are doing...
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
	
}
