package a75f.io.renatus;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SettingsFragment extends Fragment
{
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
		View rootView = inflater.inflate(R.layout.fragment_renatus_landing, container, false);
		TextView textView = (TextView) rootView.findViewById(R.id.section_label);
		textView.setText("Settings screen comes here");
		return rootView;
	}
}
