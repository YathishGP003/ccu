package a75f.io.renatus;

import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import a75f.io.logger.CcuLog;
import a75f.io.renatus.schedules.ScheduleGroupFragment;
import a75f.io.renatus.ui.alerts.AlertsFragment;

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
		fragments.add(DashboardFragment.newInstance());
		fragments.add(ZoneFragmentNew.newInstance());
		fragments.add(SystemFragment.newInstance());
		fragments.add(new ScheduleGroupFragment());
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
		return 5;
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
				return "SCHEDULING";
			case 4:
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

	public void destroyAllItems(ViewGroup container) {
		List<Fragment> fragmentList = Collections.emptyList();
		try {
			Field field = FragmentStatePagerAdapter.class.getDeclaredField("mFragments");
			field.setAccessible(true);
			fragmentList = (ArrayList<Fragment>) field.get(this);
		} catch (Exception e) {
			CcuLog.e("UI_PROFILING", "Error accessing mFragments field in PagerAdapter", e);
		}
		if(fragmentList != null) {
			for (int i = 0; i < fragmentList.size(); i++) {
				CcuLog.i("UI_PROFILING", "StatusPagerAdapter.destroyAllItems: " + i + " " + fragmentList.get(i).toString());
				Fragment fragment = fragmentList.get(i);
				if (fragments.get(i) != null) {
					super.destroyItem(container, i, fragment);
				}
			}
			finishUpdate(container);
		}
	}
}
