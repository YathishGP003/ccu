package a75f.io.renatus.ENGG;

import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

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
		
	    initializeLogging();
	}
	
	public void initializeLogging() {
		// Wraps Android's native log framework.
		/*LogWrapper logWrapper = new LogWrapper();
		// Using CcuLog, front-end to the logging chain, emulates android.util.log method signatures.
		CcuLog.setLogNode(logWrapper);
		
		// Filter strips out everything except the message text.
		MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
		//logWrapper.setNext(msgFilter);
		
		// On screen logging via a fragment with a TextView.
		LogFragment logFragment = (LogFragment) getSupportFragmentManager()
				                                        .findFragmentById(R.id.log_fragment);
		msgFilter.setNext(logFragment.getLogView());
		
		//CcuLog.i(TAG, "Ready");*/
	}
}
