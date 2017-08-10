package a75f.io.renatus;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

import a75f.io.renatus.USB.USBHomeFragment;

/**
 * Created by samjithsadasivan on 8/7/17.
 */

public class SettingsPagerAdapter extends FragmentStatePagerAdapter
{
	public SettingsPagerAdapter(FragmentManager fm) {
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
		}
		return fragment;
	}
	
	@Override
	public int getCount() {
		// Show 2 total pages.
		return 2;
	}
	
	@Override
	public CharSequence getPageTitle(int position) {
		switch (position) {
			case 0:
				return "Floor Layout";
			case 1:
				return "Settings";
			
		}
		return null;
	}
	
	@Override
	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}

}
