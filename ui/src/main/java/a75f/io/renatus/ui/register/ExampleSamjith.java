package a75f.io.renatus.ui.register;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import a75f.io.bo.kinvey.BuildingAddresses;
import a75f.io.bo.kinvey.CCUZones;
import a75f.io.renatus.R;
import a75f.io.renatus.ui.register.adapters.LoginAdapter;
import a75f.io.renatus.views.wizard.ContextVariable;
import a75f.io.renatus.views.wizard.WizardStep;


public class ExampleSamjith extends WizardStep implements View.OnClickListener
{

	@ContextVariable
	private String firstLevelId;
	@ContextVariable
	private String secondLevelId;
	@ContextVariable
	private String secondLevelName;

	private LoginAdapter zonesAdapter;
	private LoginAdapter buildingsAdapter;
	private ProgressDialog dialog;
	private ArrayList<Serializable> zones;
	private HashMap<String, ArrayList<Serializable>> buildings = new HashMap<String, ArrayList<Serializable>>();
	private Spinner spinner;
	private Integer selectedCountryIndex = -1;

	// You must have an empty constructor for every step
	public ExampleSamjith()
	{
	}

	// Set your layout here
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.step_examplesamjith, container, false);
		// Set listener for 'Next' button click
		// Note that we are setting OnClickListener using getActivity()
		// because
		// the 'Next' button is actually part of the hosting activity's
		// layout
		// and not the step's layout
		Button nextButton = getActivity().findViewById(R.id.next_button);
		nextButton.setOnClickListener(this);
		nextButton.setEnabled(false);
		nextButton.setVisibility(Button.GONE);

		if (savedInstanceState != null)
		{
			zones = (ArrayList<Serializable>) savedInstanceState.getSerializable("zones");
			buildings = (HashMap<String, ArrayList<Serializable>>) savedInstanceState.getSerializable("buildings");
			selectedCountryIndex = savedInstanceState.getInt("selectedCountryIndex");

		}

		// Binding resources Array to ListAdapter
		spinner = v.findViewById(R.id.spinner);
		zonesAdapter = new LoginAdapter(getActivity());
		spinner.setAdapter(zonesAdapter);
		getZones();

		// Binding resources Array to ListAdapter
		buildingsAdapter = new LoginAdapter(getActivity());
		ListView locationLV = v.findViewById(R.id.listView);
		locationLV.setAdapter(buildingsAdapter);

		// listening to single list item on click
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				CCUZones nature = (CCUZones) parent.getItemAtPosition(position);
				if (nature != null)
				{
					firstLevelId = nature.getFloor_name();
				}
				//TODO: update kinvey with sync table, but don't push until finished.
				selectedCountryIndex = position;
				getBuildings();
			}

			public void onNothingSelected(AdapterView<?> parent)
			{
				Button nextButton = parent.getRootView().findViewById(R.id.next_button);
				nextButton.setEnabled(false);
			}
		});

		// listening to single list item on click
		locationLV.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				// selected item
				secondLevelId = ((BuildingAddresses) parent.getItemAtPosition(position)).getBuildingName();
				secondLevelName = ((BuildingAddresses) parent.getItemAtPosition(position)).getAddress();

				//Save building addresses to kinvey realm db.
				Button nextButton = parent.getRootView().findViewById(R.id.next_button);

				if (secondLevelId != null && secondLevelId.length() > 0)
				{
					nextButton.setEnabled(true);
					done();
				}
				else
					nextButton.setEnabled(false);
			}
		});

		return v;
	}

	@Override
	public void onSaveInstanceState(Bundle args)
	{
		super.onSaveInstanceState(args);
		args.putSerializable("buildings", buildings);
		args.putSerializable("zones", zones);
		args.putInt("selectedCountryIndex", selectedCountryIndex);
	}

	private void getBuildings()
	{

		ArrayList<Serializable> buildingAddresses = new ArrayList<>();
		BuildingAddresses build = new BuildingAddresses();
		build.setAddress("address");
		build.setBuildingName("name");
		buildingAddresses.add(build);
		buildingsAdapter.updateData(buildingAddresses);
	}

	private void getZones()
	{
		if (zonesAdapter != null)
		{
			zonesAdapter.updateData(zones);
			spinner.setSelection(selectedCountryIndex);
			return;
		}
		dialog = ProgressDialog.show(getActivity(), "", "Loading", true);
		CCUZones ccuZones = new CCUZones();
		ccuZones.setFloor_name("Floor");
		zones.add(ccuZones);
		zonesAdapter.updateData(zones);



	}

	@Override
	public void onClick(View view)
	{
		// And call done() to signal that the step is completed
		// successfully
		done();
	}
}
