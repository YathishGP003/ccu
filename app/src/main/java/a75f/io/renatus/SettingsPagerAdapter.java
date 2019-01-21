package a75f.io.renatus;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import a75f.io.renatus.tuners.TunerFragment;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SettingsPagerAdapter extends FragmentStatePagerAdapter
{
	public SettingsPagerAdapter(FragmentManager fm)
	{
		super(fm);
	}
	
	
	@Override
	public Fragment getItem(int position)
	{
		// getItem is called to instantiate the fragment for the given page.
		// Return a PlaceholderFragment (defined as a static inner class below).
		Fragment fragment = null;
		switch (position)
		{
			case 0:
				fragment = FloorPlanFragment.newInstance();
				break;
			case 1:
				//fragment = USBHomeFragment.getInstance();
				fragment = SettingsFragment.newInstance();
				break;
			case 2:
				//fragment = USBHomeFragment.getInstance();
				fragment = TunerFragment.newInstance();
				break;
		}
		return fragment;
	}
	
	
	@Override
	public int getCount()
	{
		// Show 2 total pages.
		return 3;
	}
	
	
	@Override
	public int getItemPosition(Object object)
	{
		return POSITION_NONE;
	}
	
	
	@Override
	public CharSequence getPageTitle(int position)
	{
		switch (position)
		{
			case 0:
				return "Floor Layout";
			case 1:
				return "Settings";
			case 2:
				return "Tuners";
		}
		return null;
	}
}
