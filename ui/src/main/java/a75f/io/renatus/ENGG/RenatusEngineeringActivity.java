package a75f.io.renatus.ENGG;

import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import a75f.io.renatus.R;
import a75f.io.renatus.SettingsPagerAdapter;
import a75f.io.renatus.StatusPagerAdapter;

public class RenatusEngineeringActivity extends AppCompatActivity
{
	private EngineeringPagerAdapter mEnggPagerAdapter;
	
	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager mViewPager;
	
	private TabLayout mTabLayout;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_renatus_engineering);
		
		mEnggPagerAdapter = new EngineeringPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.container);
		
		mTabLayout = (TabLayout) findViewById(R.id.tabs);
		mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
		
		mViewPager.setAdapter(mEnggPagerAdapter);
		mTabLayout.post(new Runnable() {
			@Override
			public void run() {
				mTabLayout.setupWithViewPager(mViewPager, true);
			}
		});
	}
}
