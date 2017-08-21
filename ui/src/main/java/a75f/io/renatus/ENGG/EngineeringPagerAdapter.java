package a75f.io.renatus.ENGG;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import a75f.io.renatus.ENGG.logger.LogFragment;


/**
 * Created by samjithsadasivan on 8/17/17.
 */

public class EngineeringPagerAdapter extends FragmentStatePagerAdapter
{
	public EngineeringPagerAdapter(FragmentManager fm) {
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
				fragment = SerialMessageFragment.newInstance();
				break;
			case 1:
				fragment = LogFragment.newInstance();
				break;
			case 2:
				fragment = FieldTestFragment.newInstance();
				break;
			case 3:
				fragment = BLETestFragment.newInstance();
				break;
		}
		return fragment;
	}
	
	@Override
	public int getCount() {
		// Show 2 total pages.
		return 4;
	}
	
	@Override
	public CharSequence getPageTitle(int position) {
		switch (position) {
			case 0:
				return "Serial Messages";
			case 1:
				return "Log View";
			case 2:
				return "Field Test";
			case 3:
				return "BLE Test";
		}
		return null;
	}
	
	@Override
	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}
	
}
