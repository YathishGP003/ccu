package a75f.io.renatus;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.ToggleButton;

import java.util.ArrayList;

import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbCmRelayActivationMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.registartion.FreshRegistration;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 11/6/18.
 */

public class DabAnalogRtuProfile extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener
{
    @BindView(R.id.ahu) TableLayout     ahu;
	
    @BindView(R.id.ahuAnalog1Min) Spinner analog1Min;
    @BindView(R.id.ahuAnalog1Max) Spinner analog1Max;
    @BindView(R.id.ahuAnalog2Min) Spinner analog2Min;
    @BindView(R.id.ahuAnalog2Max) Spinner analog2Max;
    @BindView(R.id.ahuAnalog3Min) Spinner analog3Min;
    @BindView(R.id.ahuAnalog3Max) Spinner analog3Max;
	
    @BindView(R.id.ahuAnalog1Cb)SwitchCompat       ahuAnalog1Cb;
    @BindView(R.id.ahuAnalog2Cb)SwitchCompat       ahuAnalog2Cb;
    @BindView(R.id.ahuAnalog3Cb)SwitchCompat       ahuAnalog3Cb;
	
	@BindView(R.id.relay3Cb) SwitchCompat relay3Cb;
	@BindView(R.id.relay7Cb) SwitchCompat relay7Cb;
	
	@BindView(R.id.relay7Spinner) Spinner relay7Spinner;
	
    @BindView(R.id.ahuAnalog1Test) Spinner ahuAnalog1Test;
    @BindView(R.id.ahuAnalog2Test) Spinner ahuAnalog2Test;
    @BindView(R.id.ahuAnalog3Test) Spinner ahuAnalog3Test;
	
	@BindView(R.id.relay3Test) ToggleButton relay3Test;
	@BindView(R.id.relay7Test) ToggleButton relay7Test;

	Prefs prefs;
	
	//TODO- TEMP
	/*@BindView(R.id.buttonNext)
	Button mNext;
	*/
	
	String PROFILE = "DAB_FULLY_MODULATING";
	boolean isFromReg = false;
	DabFullyModulatingRtu systemProfile = null;
    
    public static DabAnalogRtuProfile newInstance()
    {
        return new DabAnalogRtuProfile();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_dab_analogrtu, container, false);
        ButterKnife.bind(this, rootView);
		if(getArguments() != null) {
			isFromReg = getArguments().getBoolean("REGISTRATION_WIZARD");
		}
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
		prefs = new Prefs(getContext().getApplicationContext());

	    if (L.ccu().systemProfile instanceof DabFullyModulatingRtu) {
		    systemProfile = (DabFullyModulatingRtu) L.ccu().systemProfile;
		    ahuAnalog1Cb.setChecked(systemProfile.getConfigEnabled("analog1") > 0);
		    ahuAnalog2Cb.setChecked(systemProfile.getConfigEnabled("analog2") > 0);
		    ahuAnalog3Cb.setChecked(systemProfile.getConfigEnabled("analog3") > 0);
		    relay3Cb.setChecked(systemProfile.getConfigEnabled("relay3") > 0);
		    relay7Cb.setChecked(systemProfile.getConfigEnabled("relay7") > 0);
		    setupAnalogLimitSelectors();
	    } else {
		    
		    new AsyncTask<String, Void, Void>() {

			    @Override
			    protected void onPreExecute() {
					ProgressDialogUtils.showProgressDialog(getActivity(),"Loading System Profile");
				    super.onPreExecute();
			    }
			    
			    @Override
			    protected Void doInBackground( final String ... params ) {
			    	if (systemProfile != null) {
			    		systemProfile.deleteSystemEquip();
					    L.ccu().systemProfile = null; //Makes sure that System Algos dont run until new profile is ready.
				    }
				    systemProfile = new DabFullyModulatingRtu();
				    systemProfile.addSystemEquip();
				    L.ccu().systemProfile = systemProfile;
				    return null;
			    }
			    @Override
			    protected void onPostExecute( final Void result ) {
				    setupAnalogLimitSelectors();
					ProgressDialogUtils.hideProgressDialog();
			    }
		    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
	    }
		//setupTempLimitSelectors();
	    
	    ahuAnalog1Cb.setOnCheckedChangeListener(this);
	    ahuAnalog2Cb.setOnCheckedChangeListener(this);
	    ahuAnalog3Cb.setOnCheckedChangeListener(this);
	    relay3Cb.setOnCheckedChangeListener(this);
	    relay7Cb.setOnCheckedChangeListener(this);
	
	    //TODO-TEMP
		//isFromReg = getArguments().getBoolean("REGISTRATION_WIZARD");
	 
		/*if(isFromReg){
			mNext.setVisibility(View.VISIBLE);
		}
		else {
			mNext.setVisibility(View.GONE);
		}

		mNext.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// TODO Auto-generated method stub
				goTonext();
			}
		});*/
	}

	private void goTonext() {
		//Intent i = new Intent(mContext, RegisterGatherCCUDetails.class);
		//startActivity(i);
		prefs.setBoolean("PROFILE_SETUP",true);
		prefs.setString("PROFILE",PROFILE);
		((FreshRegistration)getActivity()).selectItem(19);
	}
	private void setupAnalogLimitSelectors() {
		ArrayList<Integer> analogArray = new ArrayList<>();
		for (int a = 0; a <= 10; a++)
		{
			analogArray.add(a);
		}
		ArrayAdapter<Integer> analogAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, analogArray);
		analogAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		analog1Min.setAdapter(analogAdapter);
		analog1Min.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("cooling and min")), false);
		analog1Max.setAdapter(analogAdapter);
		double analogVal = systemProfile.getConfigVal("cooling and max");
		analog1Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int)analogVal) : analogArray.size() -1 , false);
		
		analog2Min.setAdapter(analogAdapter);
		analog2Min.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("fan and min")), false);
		
		analog2Max.setAdapter(analogAdapter);
		analogVal = systemProfile.getConfigVal("fan and max");
		analog2Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int)analogVal) : analogArray.size() -1, false);
		
		analog3Min.setAdapter(analogAdapter);
		analog3Min.setSelection(analogAdapter.getPosition((int)systemProfile.getConfigVal("heating and min")), false);
		
		analog3Max.setAdapter(analogAdapter);
		analogVal = systemProfile.getConfigVal("heating and max");
		analog3Max.setSelection(analogVal != 0 ? analogAdapter.getPosition((int)analogVal) : analogArray.size() -1, false);
		
		ArrayList<String> humidifierOptions = new ArrayList<>();
		humidifierOptions.add("Humidifier");
		humidifierOptions.add("De-Humidifier");
		
		ArrayAdapter<String> humidityAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, humidifierOptions);
		humidityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		
		relay7Spinner.setAdapter(humidityAdapter);
		int selection = (int)systemProfile.getConfigVal("humidifier and type");
		relay7Spinner.setSelection(selection, false);
		
		
		ArrayList<Double> zoroToHundred = new ArrayList<>();
		for (double val = 0;  val <= 100.0; val++)
		{
			zoroToHundred.add(val);
		}
		ArrayAdapter<Double> coolingSatTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
		coolingSatTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog1Test.setAdapter(coolingSatTestAdapter);
		ahuAnalog1Test.setSelection(0,false);
		
		ArrayAdapter<Double> spTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
		spTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog2Test.setAdapter(spTestAdapter);
		ahuAnalog2Test.setSelection(0,false);
		
		ArrayAdapter<Double> heatingSatTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, zoroToHundred);
		heatingSatTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog3Test.setAdapter(heatingSatTestAdapter);
		ahuAnalog3Test.setSelection(0,false);
		
		
		analog1Min.setOnItemSelectedListener(this);
		analog1Max.setOnItemSelectedListener(this);
		analog2Min.setOnItemSelectedListener(this);
		analog2Max.setOnItemSelectedListener(this);
		analog3Min.setOnItemSelectedListener(this);
		analog3Max.setOnItemSelectedListener(this);
	
		ahuAnalog1Test.setOnItemSelectedListener(this);
		ahuAnalog2Test.setOnItemSelectedListener(this);
		ahuAnalog3Test.setOnItemSelectedListener(this);
		
		relay7Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
			{
				setConfigBackground("relay7 and humidifier and type", i);
			}
			@Override
			public void onNothingSelected(AdapterView<?> adapterView)
			{
			}
		});
		
		relay3Test.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b)
			{
				CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
				msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
				msg.relayBitmap.set((short)(b? 1 << 3: 0 ));
				MeshUtil.sendStructToCM(msg);
			}
		});
		relay7Test.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b)
			{
				CcuToCmOverUsbCmRelayActivationMessage_t msg = new CcuToCmOverUsbCmRelayActivationMessage_t();
				msg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
				msg.relayBitmap.set((short)(b ? 1<< 7 : 0));
				MeshUtil.sendStructToCM(msg);
			}
		});
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
			case R.id.relay3Cb:
				setSelectionBackground("relay3", isChecked);
				break;
			case R.id.relay7Cb:
				setSelectionBackground("relay7", isChecked);
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
				setConfigBackground("cooling and min", val);
				break;
			case R.id.ahuAnalog1Max:
				setConfigBackground("cooling and max", val);
				break;
			case R.id.ahuAnalog2Min:
				setConfigBackground("fan and min", val);
				break;
			case R.id.ahuAnalog2Max:
				setConfigBackground("fan and max", val);
				break;
			case R.id.ahuAnalog3Min:
				setConfigBackground("heating and min", val);
				break;
			case R.id.ahuAnalog3Max:
				setConfigBackground("heating and max", val);
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
		}
	}
	

	private void setConfigBackground(String tags, double val) {
		new AsyncTask<String, Void, Void>() {

			@Override
			protected Void doInBackground( final String ... params ) {
				systemProfile.setConfigVal(tags, val);
				return null;
			}
			
			@Override
			protected void onPostExecute( final Void result ) {
				// continue what you are doing...
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
	}
	
	private void setSelectionBackground(String analog, boolean selected) {
		new AsyncTask<String, Void, Void>() {
			@Override
			protected void onPreExecute() {
				ProgressDialogUtils.showProgressDialog(getActivity(),"Saving DAB System Configuration");
				super.onPreExecute();
			}
			@Override
			protected Void doInBackground( final String ... params ) {
				systemProfile.setConfigEnabled(analog, selected ? 1: 0);
				return null;
			}
			
			@Override
			protected void onPostExecute( final Void result ) {
				if (!selected) {
					updateSystemMode();
				}
				ProgressDialogUtils.hideProgressDialog();
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
	}
	
	private void setUserIntentBackground(String query, double val) {
		
		new AsyncTask<String, Void, Void>() {
			@Override
			protected Void doInBackground( final String ... params ) {
				TunerUtil.writeSystemUserIntentVal(query, val);
				return null;
			}
			
			@Override
			protected void onPostExecute( final Void result ) {
				// continue what you are doing...
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void updateSystemMode() {
		SystemMode systemMode = SystemMode.values()[(int)systemProfile.getUserIntentVal("rtu and mode")];
		if (systemMode == SystemMode.OFF) {
			return;
		}
		if ((systemMode == SystemMode.AUTO && (!systemProfile.isCoolingAvailable() || !systemProfile.isHeatingAvailable()))
		    || (systemMode == SystemMode.COOLONLY && !systemProfile.isCoolingAvailable())
		    || (systemMode == SystemMode.HEATONLY && !systemProfile.isHeatingAvailable()))
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle);//, AlertDialog.THEME_HOLO_DARK);
			String str = "Operational Mode changed from '" + systemMode.name() + "' to '" + SystemMode.OFF.name() + "' based on changed equipment selection.";
			str = str + "\nPlease select appropriate operational mode from System Settings.";
			builder.setCancelable(false)
			       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				       public void onClick(DialogInterface dialog, int id) {
					       dialog.cancel();
				       }
			       })
			       .setTitle("Operational Mode Changed")
			       .setMessage(str);
			
			AlertDialog dlg = builder.create();
			dlg.show();
			setUserIntentBackground("rtu and mode", SystemMode.OFF.ordinal());
		}
	}
	
	public void sendAnalog1OutTestSignal(double val) {
		double analogMin = systemProfile.getConfigVal("cooling and min");
		double analogMax = systemProfile.getConfigVal("cooling and max");
		
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
		double analogMin = systemProfile.getConfigVal("fan and min");
		double analogMax = systemProfile.getConfigVal("fan and max");
		
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
		double analogMin = systemProfile.getConfigVal("heating and min");
		double analogMax = systemProfile.getConfigVal("heating and max");
		
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
	
}
