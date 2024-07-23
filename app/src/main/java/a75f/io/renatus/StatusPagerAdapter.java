package a75f.io.renatus;

import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

import a75f.io.logger.CcuLog;
import a75f.io.renatus.schedules.SchedulerFragment;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class StatusPagerAdapter extends FragmentStatePagerAdapter
{
	private List<Fragment> fragments;
	public StatusPagerAdapter(FragmentManager fm)
	{
		super(fm);
		fragments = new ArrayList<>();
		fragments.add(ZoneFragmentNew.newInstance());
		fragments.add(SystemFragment.newInstance());
		fragments.add(SchedulerFragment.newInstance());
		fragments.add(AlertsFragment.newInstance());
	}

	public Fragment getItem(int position)
	{
		CcuLog.i("UI_PROFILING", "CreateItem: " + position + " " + fragments.get(position).toString());
		return fragments.get(position);
	}
	
	@Override
	public int getCount()
	{
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
				return "Zones";
			case 1:
				return "System";
			case 2:
				return "SCHEDULING";
			case 3:
				return "Alerts";
		}
		return null;
	}

	/**
	 * Intercepting the destroyItem method to avoid view deletion and creation during tab switch.
	 * Messing with android FW : but it seems like a decent quick fix that works till we upgrade to new ViewPager.
	 */
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		CcuLog.i("UI_PROFILING", "destroyItem: " + position + " " + object.toString());
		//super.destroyItem(container, position, object);
	}
}
