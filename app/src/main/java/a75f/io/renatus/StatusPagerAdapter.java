package a75f.io.renatus;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import a75f.io.renatus.schedules.SchedulerFragment;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class StatusPagerAdapter extends FragmentStatePagerAdapter
{
	public StatusPagerAdapter(FragmentManager fm)
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
				fragment = DashboardFragment.newInstance();
				break;
			case 1:
				fragment = ZoneFragmentTemp.newInstance();
				break;
			case 2:
				//fragment = SystemFragment_bkp.newInstance();
				fragment = SystemFragment.newInstance();
				break;
			case 3:
				fragment = SchedulerFragment.newInstance();
				break;
			case 4:
				fragment = AlertsFragment.newInstance();
				break;
		}
		return fragment;
	}
	
	
	@Override
	public int getCount()
	{
		// Show 2 total pages.
		return 4;
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
				return "Dashboard";
			case 1:
				return "Zones";
			case 2:
				return "System";
			case 3:
				return "Building";
			case 4:
				return "Alerts";
		}
		return null;
	}
}
