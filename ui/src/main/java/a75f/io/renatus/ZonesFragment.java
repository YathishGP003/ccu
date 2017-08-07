package a75f.io.renatus;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by samjithsadasivan on 8/7/17.
 */

public class ZonesFragment extends Fragment
{
	public static ZonesFragment newInstance(){
		return new ZonesFragment();
	}
	
	public ZonesFragment(){
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_renatus_landing, container, false);
		TextView textView = (TextView) rootView.findViewById(R.id.section_label);
		textView.setText("Zones screen comes here");
		return rootView;
	}
}
