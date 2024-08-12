package a75f.io.renatus;

import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

import a75f.io.logger.CcuLog;
import a75f.io.renatus.tuners.TunerFragment;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SettingsPagerAdapter extends FragmentStatePagerAdapter {

    private List<Fragment> fragments;
    public SettingsPagerAdapter(FragmentManager fm) {
        super(fm);
        fragments = new ArrayList<>();
        fragments.add(FloorPlanFragment.newInstance());
        fragments.add(SystemConfigFragment.newInstance());
        fragments.add(SettingsFragment.newInstance());
        fragments.add(TunerFragment.newInstance());
    }

    public Fragment getItem(int position) {
        return fragments.get(position);
    }


    @Override
    public int getCount() {
        // Show 2 total pages.
        return 4;
    }


    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }


    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Floor Layout";
            case 1:
                return "System Config";
            case 2:
                return "Settings";
            case 3:
                return "Tuners";
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
        int btnTab = RenatusLandingActivity.btnTabs.getSelectedTabPosition();
        int tab = RenatusLandingActivity.mTabLayout.getSelectedTabPosition();
        if(btnTab == 1 && tab == 0) { // This will run when we switch from FloorPlanFragment to zone fragment
            FloorPlanFragment.getInstance().destroyActionBar();
        }
        //super.destroyItem(container, position, object);
    }
}
