package a75f.io.renatus;

import android.app.ProgressDialog;
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

import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemConstants;
import a75f.io.logic.bo.building.system.vav.VavAnalogRtu;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 11/6/18.
 */

public class VavAnalogRtuProfile extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener
{
    @BindView(R.id.ahu) TableLayout     ahu;
	
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
	
	VavAnalogRtu systemProfile = null;
    
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
	    if (L.ccu().systemProfile instanceof VavAnalogRtu) {
		    systemProfile = (VavAnalogRtu) L.ccu().systemProfile;
		    ahuAnalog1Cb.setChecked(systemProfile.getConfigEnabled("analog1") > 0);
		    ahuAnalog2Cb.setChecked(systemProfile.getConfigEnabled("analog2") > 0);
		    ahuAnalog3Cb.setChecked(systemProfile.getConfigEnabled("analog3") > 0);
		    ahuAnalog4Cb.setChecked(systemProfile.getConfigEnabled("analog4") > 0);
		    setupAnalogLimitSelectors();
	    } else {
		    
		    new AsyncTask<Void, Void, Void>() {
			
			    ProgressDialog progressDlg = new ProgressDialog(getActivity());
			    @Override
			    protected void onPreExecute() {
				    progressDlg.setMessage("Loading System Profile");
				    progressDlg.show();
				    super.onPreExecute();
			    }
			    
			    @Override
			    protected Void doInBackground( final Void ... params ) {
			    	if (systemProfile != null) {
			    		systemProfile.deleteSystemEquip();
				    }
				    systemProfile = new VavAnalogRtu();
				    L.ccu().systemProfile = systemProfile;
				    systemProfile.initTRSystem();
				    return null;
			    }
			    @Override
			    protected void onPostExecute( final Void result ) {
				    setupAnalogLimitSelectors();
				    progressDlg.dismiss();
			    }
		    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
	    }
		//setupTempLimitSelectors();
	    
	    ahuAnalog1Cb.setOnCheckedChangeListener(this);
	    ahuAnalog2Cb.setOnCheckedChangeListener(this);
	    ahuAnalog3Cb.setOnCheckedChangeListener(this);
	    ahuAnalog4Cb.setOnCheckedChangeListener(this);
	}
	
	private void setupTempLimitSelectors() {
  
		ArrayList<Double> coolingSatArray = new ArrayList<>();
		for (double pos = SystemConstants.COOLING_SAT_CONFIG_MIN; pos <= SystemConstants.COOLING_SAT_CONFIG_MAX; pos++){
			coolingSatArray.add(pos);
		}
		
		ArrayList<Double> heatingSatArray = new ArrayList<>();
		for (double pos = SystemConstants.HEATING_SAT_CONFIG_MIN; pos <= SystemConstants.HEATING_SAT_CONFIG_MAX; pos++){
			heatingSatArray.add(pos);
		}
		
		ArrayList<Double> co2Array = new ArrayList<>();
		for (double pos = SystemConstants.CO2_CONFIG_MIN; pos <= SystemConstants.CO2_CONFIG_MAX; pos+=10){
			co2Array.add(pos);
		}
		ArrayList<Double> spArray = new ArrayList<>();
		for (double pos = SystemConstants.SP_CONFIG_MIN; pos <= SystemConstants.SP_CONFIG_MAX; pos +=0.1){
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
		
		
	}
	
	private void setupAnalogLimitSelectors() {
		ArrayList<Integer> analogArray = new ArrayList<>();
		for (int a = 0; a <= 10; a++)
		{
			analogArray.add(a);
		}
		ArrayAdapter<Integer> analogAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, analogArray);
		analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		
		double analogVal = 0;
		analog1Min.setAdapter(analogAdapter);
		analog1Min.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal(
													"cooling and sat and min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		analog1Max.setAdapter(analogAdapter);
		analogVal = systemProfile.getConfigVal("cooling and sat and max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);
		analog1Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int)analogVal) : analogArray.size() -1);
		
		analog2Min.setAdapter(analogAdapter);
		analog2Min.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal(
													"staticPressure and min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		analog2Max.setAdapter(analogAdapter);
		analogVal = systemProfile.getConfigVal("staticPressure and max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);
		analog2Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int)analogVal) : analogArray.size() -1);
		
		analog3Min.setAdapter(analogAdapter);
		analog3Min.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal(
													"heating and min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		analog3Max.setAdapter(analogAdapter);
		analogVal = systemProfile.getConfigVal("heating and max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);
		analog3Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int)analogVal) : analogArray.size() -1);
		
		analog4Min.setAdapter(analogAdapter);
		analog4Min.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal(
				"co2 and min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL)));
		
		analog4Max.setAdapter(analogAdapter);
		analogVal = systemProfile.getConfigVal("co2 and max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);
		analog4Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int)analogVal) : analogArray.size() -1);
		
		ArrayList<Double> zoroToHundred = new ArrayList<>();
		for (double val = 0;  val <= 100.0; val++)
		{
			zoroToHundred.add(val);
		}
		ArrayAdapter<Double> coolingSatTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
		coolingSatTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog1Test.setAdapter(coolingSatTestAdapter);
		
		ArrayAdapter<Double> spTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
		spTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog2Test.setAdapter(spTestAdapter);
		
		ArrayAdapter<Double> heatingSatTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
		heatingSatTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog3Test.setAdapter(heatingSatTestAdapter);
		
		ArrayAdapter<Double> co2TestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
		co2TestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog4Test.setAdapter(co2TestAdapter);
		
		analog1Min.setOnItemSelectedListener(this);
		analog1Max.setOnItemSelectedListener(this);
		analog2Min.setOnItemSelectedListener(this);
		analog2Max.setOnItemSelectedListener(this);
		analog3Min.setOnItemSelectedListener(this);
		analog3Max.setOnItemSelectedListener(this);
		analog4Min.setOnItemSelectedListener(this);
		analog4Max.setOnItemSelectedListener(this);
		ahuAnalog1Test.setOnItemSelectedListener(this);
		ahuAnalog2Test.setOnItemSelectedListener(this);
		ahuAnalog3Test.setOnItemSelectedListener(this);
		ahuAnalog4Test.setOnItemSelectedListener(this);
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		switch (buttonView.getId())
		{
			case R.id.ahuAnalog1Cb:
				setSelectionBackground("analog1", isChecked);
				break;
			case R.id.ahuAnalog2Cb:
				setSelectionBackground("analog2", isChecked);
				break;
			case R.id.ahuAnalog3Cb:
				setSelectionBackground("analog3", isChecked);
				break;
			case R.id.ahuAnalog4Cb:
				setSelectionBackground("analog4", isChecked);
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
			
			case R.id.ahuAnalog1Min:
				setTunerBackground("cooling and sat and min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog1Max:
				setTunerBackground("cooling and sat and max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog2Min:
				setTunerBackground("staticPressure and min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog2Max:
				setTunerBackground("staticPressure and max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog3Min:
				setTunerBackground("heating and min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog3Max:
				setTunerBackground("heating and max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog4Min:
				setTunerBackground("co2 and min", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog4Max:
				setTunerBackground("co2 and max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL, val);
				break;
			case R.id.ahuAnalog1Test:
				sendAnalog1OutTestSignal(val);
				break;
			case R.id.ahuAnalog2Test:
				sendAnalog2OutTestSignal(val);
				break;
			case R.id.ahuAnalog3Test:
				sendAnalog3OutTestSignal(val);
				break;
			case R.id.ahuAnalog4Test:
				sendAnalog4OutTestSignal(val);
				break;
		}
	}
	
	private void setTunerBackground(String tags, int level, double val) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground( final Void ... params ) {
				systemProfile.setConfigVal(tags, level, val);
				//SystemTunerUtil.setTuner(analog, type, level, val);
				
				return null;
			}
			
			@Override
			protected void onPostExecute( final Void result ) {
				// continue what you are doing...
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
	}
	
	private void setSelectionBackground(String analog, boolean selected) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground( final Void ... params ) {
				systemProfile.setConfigEnabled(analog, selected ? 1: 0);
				return null;
			}
			
			@Override
			protected void onPostExecute( final Void result ) {
				// continue what you are doing...
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
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
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void sendAnalog1OutTestSignal(double val) {
		double analogMin = systemProfile.getConfigVal("cooling and sat and min",TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);
		double analogMax = systemProfile.getConfigVal("cooling and sat and max",TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);
		
		CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
		msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
		
		short signal;
		if (analogMax > analogMin)
		{
			signal = (short) (10 * (analogMin + (analogMax - analogMin) * val/100));
		} else {
			signal = (short) (10 * (analogMin - (analogMin - analogMax) * val/100));
		}
		msg.analog0.set(signal);
		MeshUtil.sendStructToCM(msg);
	}
	
	public void sendAnalog2OutTestSignal(double val) {
		double analogMin = systemProfile.getConfigVal("staticPressure and min",TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);
		double analogMax = systemProfile.getConfigVal("staticPressure and max",TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);
		
		CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
		msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
		
		short signal;
		if (analogMax > analogMin)
		{
			signal = (short) (10 * (analogMin + (analogMax - analogMin) * val/100));
		} else {
			signal = (short) (10 * (analogMin - (analogMin - analogMax) * val/100));
		}
		msg.analog1.set(signal);
		MeshUtil.sendStructToCM(msg);
	}
	
	public void sendAnalog3OutTestSignal(double val) {
		double analogMin = systemProfile.getConfigVal("heating and min",TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);
		double analogMax = systemProfile.getConfigVal("heating and max",TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);
		
		CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
		msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
		
		short signal;
		if (analogMax > analogMin)
		{
			signal = (short) (10 * (analogMin + (analogMax - analogMin) * val/100));
		} else {
			signal = (short) (10 * (analogMin - (analogMin - analogMax) * val/100));
		}
		msg.analog2.set(signal);
		MeshUtil.sendStructToCM(msg);
	}
	
	public void sendAnalog4OutTestSignal(double val) {
		double analogMin = systemProfile.getConfigVal("co2 and min",TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);
		double analogMax = systemProfile.getConfigVal("co2 and max",TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);
		
		CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
		msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
		
		short signal;
		if (analogMax > analogMin)
		{
			signal = (short) (10 * (analogMin + (analogMax - analogMin) * val/100));
		} else {
			signal = (short) (10 * (analogMin - (analogMin - analogMax) * val/100));
		}
		msg.analog3.set(signal);
		MeshUtil.sendStructToCM(msg);
	}
}
