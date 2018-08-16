package a75f.io.renatus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TableLayout;

import java.util.ArrayList;

import a75f.io.bo.building.system.AHU;
import a75f.io.bo.building.system.RtuIE;
import a75f.io.logic.L;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SettingsFragment extends Fragment
{
	
	@BindView(R.id.hvacEquipSelect) Spinner     spHvacEquipType;
	@BindView(R.id.ahu) TableLayout ahu;
	@BindView(R.id.rtuIE) TableLayout rtuIE;
	@BindView(R.id.ahuAnalog1Min) Spinner ahuAnalog1Min;
	@BindView(R.id.ahuAnalog1Max) Spinner ahuAnalog1Max;
	@BindView(R.id.ahuAnalog2Min) Spinner ahuAnalog2Min;
	@BindView(R.id.ahuAnalog2Max) Spinner ahuAnalog2Max;
	@BindView(R.id.ahuAnalog3Min) Spinner ahuAnalog3Min;
	@BindView(R.id.ahuAnalog3Max) Spinner ahuAnalog3Max;
	
	@BindView(R.id.ahuAnalog1Cb) CheckBox ahuAnalog1Cb;
	@BindView(R.id.ahuAnalog2Cb) CheckBox ahuAnalog2Cb;
	@BindView(R.id.ahuAnalog3Cb) CheckBox ahuAnalog3Cb;
	
	@BindView(R.id.ahuAnalog1Type) Spinner ahuAnalog1Type;
	@BindView(R.id.ahuAnalog2Type) Spinner ahuAnalog2Type;
	@BindView(R.id.ahuAnalog3Type) Spinner ahuAnalog3Type;
	
	@BindView(R.id.ahuAnalog1Test) Spinner ahuAnalog1Test;
	@BindView(R.id.ahuAnalog2Test) Spinner ahuAnalog2Test;
	@BindView(R.id.ahuAnalog3Test) Spinner ahuAnalog3Test;
	
	
	public SettingsFragment()
	{
	}
	
	
	public static SettingsFragment newInstance()
	{
		return new SettingsFragment();
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.systemprofile_fragment, container, false);
		ButterKnife.bind(this, rootView);
		return rootView;
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		ArrayAdapter<CharSequence> hvacEquipSelectorAdapter = ArrayAdapter.createFromResource(this.getActivity(),
				R.array.hvac_equip_select, R.layout.spinner_dropdown_item);
		hvacEquipSelectorAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		spHvacEquipType.setAdapter(hvacEquipSelectorAdapter);
		spHvacEquipType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
			{
				switch (i) {
					case 0:
						L.ccu().systemProfile = new AHU();
						ahu.setVisibility(View.VISIBLE);
						rtuIE.setVisibility(View.GONE);
						break;
					case 1:
						L.ccu().systemProfile = new RtuIE();
						ahu.setVisibility(View.GONE);
						rtuIE.setVisibility(View.VISIBLE);
						break;
					case 2:
					case 3:
						ahu.setVisibility(View.GONE);
						rtuIE.setVisibility(View.GONE);
						break;
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> adapterView)
			{
			}
		});
		
		ArrayList<Integer> analogArray = new ArrayList<>();
		for (int a = 0; a <= 10; a++)
		{
			analogArray.add(a);
		}
		ArrayAdapter<Integer> analogAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, analogArray);
		analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		
		ahuAnalog1Min.setAdapter(analogAdapter);
		ahuAnalog1Max.setAdapter(analogAdapter);
		ahuAnalog2Min.setAdapter(analogAdapter);
		ahuAnalog2Max.setAdapter(analogAdapter);
		ahuAnalog3Min.setAdapter(analogAdapter);
		ahuAnalog3Max.setAdapter(analogAdapter);
		
		String[] analogTypes = {"0-10V"};
		
		ArrayAdapter<String> analogTypeAdapter = new ArrayAdapter<> (getActivity(),R.layout.spinner_dropdown_item, analogTypes);
		analogTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog1Type.setAdapter(analogTypeAdapter);
		ahuAnalog2Type.setAdapter(analogTypeAdapter);
		ahuAnalog3Type.setAdapter(analogTypeAdapter);
		
		
		ArrayList<Double> satTestArray = new ArrayList<>();
		for (double sat = 50.0;  sat <= 150.0; sat++)
		{
			satTestArray.add(sat);
		}
		ArrayAdapter<Double> satTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, satTestArray);
		satTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog1Test.setAdapter(satTestAdapter);
		
		ArrayList<Double> spTestArray = new ArrayList<>();
		for (double sp = 0.5;  sp <= 2.0; sp=sp+0.1)
		{
			spTestArray.add(Math.round(sp * 100D) / 100D);
		}
		ArrayAdapter<Double> spTestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, spTestArray);
		spTestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog2Test.setAdapter(spTestAdapter);
		
		ArrayList<Double> co2TestArray = new ArrayList<>();
		for (double co2 = 800.0;  co2 <= 1000.0; co2++)
		{
			co2TestArray.add(co2);
		}
		ArrayAdapter<Double> co2TestAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, co2TestArray);
		co2TestAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		ahuAnalog3Test.setAdapter(co2TestAdapter);
		
		
	}
	
}
