package a75f.io.renatus.ENGG;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import a75f.io.renatus.R;
import a75f.io.renatus.ZonesFragment;

/**
 * Created by samjithsadasivan on 8/17/17.
 */

public class BLETestFragment extends Fragment
{
	public static BLETestFragment newInstance(){
		return new BLETestFragment();
	}
	
	public BLETestFragment(){
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_renatus_landing, container, false);
		TextView textView = (TextView) rootView.findViewById(R.id.section_label);
		textView.setText("BLE test screen comes here");
		return rootView;
	}
}
