package a75f.io.renatus.ENGG;

import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import a75f.io.logger.CcuLog;
import a75f.io.renatus.R;

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
	
	@Override
	public void onStart() {
		super.onStart();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		CcuLog.setLogNode(null);
	}
}
