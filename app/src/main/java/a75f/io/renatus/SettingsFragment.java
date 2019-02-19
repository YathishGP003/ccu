package a75f.io.renatus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import a75f.io.logic.L;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SettingsFragment extends Fragment
{
	@BindView(R.id.hvacEquipSelect)
	Spinner spSystemProfile;
	
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
		View rootView = inflater.inflate(R.layout.fragment_profile_selector, container, false);
		ButterKnife.bind(this, rootView);
		return rootView;
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		ArrayAdapter<CharSequence> systemProfileSelectorAdapter = ArrayAdapter.createFromResource(this.getActivity(), R.array.system_profile_select, R.layout.spinner_dropdown_item);
		systemProfileSelectorAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		spSystemProfile.setAdapter(systemProfileSelectorAdapter);
		spSystemProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
			{
				switch (i)
				{
					case 0:
						getActivity().getSupportFragmentManager().beginTransaction()
						             .replace(R.id.profileContainer, new DefaultSystemProfile()).commit();
						break;
					
					case 1:
						getActivity().getSupportFragmentManager().beginTransaction()
						             .replace(R.id.profileContainer, new VavAnalogRtuProfile()).commit();
						break;
					
					case 2:
						getActivity().getSupportFragmentManager().beginTransaction()
						             .replace(R.id.profileContainer, new VavStagedRtuProfile()).commit();
						break;
					case 3:
						getActivity().getSupportFragmentManager().beginTransaction()
						             .replace(R.id.profileContainer, new VavStagedRtuWithVfdProfile()).commit();
						break;
					case 4:
						getActivity().getSupportFragmentManager().beginTransaction()
						             .replace(R.id.profileContainer, new VavHybridRtuProfile()).commit();
						break;
					
					/*case 2:
						getActivity().getSupportFragmentManager().beginTransaction()
						             .replace(R.id.profileContainer, new VavIERtuProfile()).commit();
						break;
					case 4:
						L.ccu().systemProfile = new VavBacnetRtu();
						
					case 5:
						getActivity().getSupportFragmentManager().beginTransaction()
						             .replace(R.id.profileContainer, new DABStagedProfile()).commit();
						break;*/
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> adapterView)
			{
			}
		});
		spSystemProfile.setSelection(L.ccu().systemProfile != null ?
				            systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0 );
	}
}
