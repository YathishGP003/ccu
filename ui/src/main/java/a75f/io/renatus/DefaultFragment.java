package a75f.io.renatus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ryanmattison on 7/24/17.
 */

public class DefaultFragment extends DialogFragment
{
	
	private static final String TAG        = DefaultFragment.class.getSimpleName();
	private static final String DIMISSABLE = "DIMISSABLE";
	List<Integer> ports = Arrays.asList(1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000);
	@BindView(R.id.fragment_main_sn_name_edittext)
	EditText     mSNNameEditText;
	@BindView(R.id.fragment_main_textview)
	TextView     mMainTextView;
	@BindView(R.id.fragment_main_port_spinner)
	Spinner      mPortSpinner;
	@BindView(R.id.button)
	Button       doneBtn;
	@BindView(R.id.fragment_main_ble_device_type)
	ToggleButton mBLEDeviceTypeButton;
	
	int mPortPosition = 0;
	private boolean mDismissable;
	
	
	public static DefaultFragment getInstance()
	{
		return DefaultFragment.getInstance(true);
	}
	
	
	public static DefaultFragment getInstance(boolean dismissable)
	{
		DefaultFragment defaultFragment = new DefaultFragment();
		Bundle b = new Bundle();
		b.putBoolean(DIMISSABLE, dismissable);
		defaultFragment.setArguments(b);
		return defaultFragment;
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View retVal = inflater.inflate(R.layout.fragment_main, container, false);
		ButterKnife.bind(this, retVal);
		mDismissable = getArguments().getBoolean(DIMISSABLE);
		return retVal;
	}
	
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
//		setupPortSpinner();
//		short meshAddress = Globals.getInstance().getSmartNode().getMeshAddress();
//		String roomName = Globals.getInstance().getSmartNode().getName();
//		int position = Arrays.binarySearch(ports.toArray(), (int) meshAddress);
//		mPortSpinner.setSelection(position);
//		mSNNameEditText.setText(roomName);
	}
	
	
	private void setupPortSpinner()
	{
		ArrayAdapter<Integer> dataAdapter = new ArrayAdapter<Integer>(this.getActivity(), android.R.layout.simple_spinner_item, ports);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mPortSpinner.setAdapter(dataAdapter);
		mPortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				mPortPosition = position;
			}
			
			
			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
			}
		});
	}
	
	
	@OnClick(R.id.button)
	public void done()
	{
		if (mSNNameEditText.getText() != null && !mSNNameEditText.getText().toString().equals(""))
		{
//			Globals.getInstance().getSmartNode().setName(mSNNameEditText.getText().toString());
//			Globals.getInstance().getSmartNode().setMeshAddress(ports.get(mPortPosition).shortValue());
			Toast.makeText(DefaultFragment.this.getActivity(), "Saved", Toast.LENGTH_SHORT).show();
			if (mBLEDeviceTypeButton.isChecked())
			{
				Log.i(TAG, "Is Checked: " + mBLEDeviceTypeButton.isChecked());
			}
			if (mDismissable)
			{
				dismiss();
			}
		}
		else
		{
			Toast.makeText(DefaultFragment.this.getActivity(), "Must enter a smart node name", Toast.LENGTH_SHORT).show();
		}
	}
}


