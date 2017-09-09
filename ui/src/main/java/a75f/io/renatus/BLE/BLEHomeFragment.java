package a75f.io.renatus.BLE;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import a75f.io.bo.building.SmartNode;
import a75f.io.renatus.R;
import a75f.io.util.Globals;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ryanmattison isOn 7/24/17.
 */

public class BLEHomeFragment extends Fragment
{
	
	private static final String TAG = BLEHomeFragment.class.getSimpleName();
	@BindView(R.id.fragment_ble_button)
	Button mainTextView;
	
	
	public static BLEHomeFragment getInstance()
	{
		return new BLEHomeFragment();
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View retVal = inflater.inflate(R.layout.fragment_ble, container, false);
		ButterKnife.bind(this, retVal);
		return retVal;
	}
	
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		Log.i(TAG, "BLE VIEW INITIATED");
		mainTextView.setText("BLE");
	}
	
	
	@OnClick(R.id.fragment_ble_button)
	void bleSubmit()
	{
		Log.i(TAG, "Done");
		SmartNode smartNode = null;
		if (Globals.getInstance().getCCUApplication().smartNodes.size() == 0)
		{
			smartNode = new SmartNode();
			Globals.getInstance().getCCUApplication().smartNodes.add(smartNode);
		}
		else
		{
			smartNode = Globals.getInstance().getCCUApplication().smartNodes.get(0);
		}
		FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance((short) smartNode
				                                                                               .mAddress, smartNode.mRoomName, "Floor");
		showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
		Toast.makeText(this.getActivity(), "BLE Fragment Done", Toast.LENGTH_LONG).show();
	}
	
	
	private void showDialogFragment(DialogFragment dialogFragment, String id)
	{
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag(id);
		if (prev != null)
		{
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		// Create and show the dialog.
		dialogFragment.show(ft, id);
	}
}
