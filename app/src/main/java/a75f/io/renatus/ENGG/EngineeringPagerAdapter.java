package a75f.io.renatus.ENGG;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import a75f.io.renatus.ENGG.alertdefs.AlertDefsFragment;
import a75f.io.renatus.ENGG.logger.LogFragment;

/**
 * Created by samjithsadasivan isOn 8/17/17.
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
				fragment = DevSettings.newInstance();
				break;
			case 1:
				fragment = HaystackExplorer.newInstance();
				break;
			case 2:
				fragment = LogFragment.newInstance();
				break;
			case 3:
				fragment = new AlertDefsFragment();
				break;
			//	fragment = FieldTestFragment.newInstance();
			//	break;
			//case 3:
			//	fragment = SerialMessageFragment.newInstance();
			//	break;
			
			case 4:
				fragment = OTAUpdateTestFragment.newInstance();
				break;
			case 5:
				fragment = SerialMessageFragment.newInstance();
				break;
		}
		return fragment;
	}
	
	@Override
	public int getCount() {
		return 6;
	}
	
	@Override
	public CharSequence getPageTitle(int position) {
		switch (position) {
			case 0:
				return "Dev Settings";
			case 1:
				return "Haystack Explorer";
			case 2:
				return "CcuLog";
			case 3:
				return "Alert Defs";
			case 4:
				return "OTA Update";
			case 5:
				return "Modbus Test";
		}
		return null;
	}
	
	@Override
	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}
	
}
