package a75f.io.renatus;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import a75f.io.renatus.ENGG.RenatusEngineeringActivity;

public class RenatusLandingActivity extends AppCompatActivity
{
	
	private static final String  TAG         = RenatusLandingActivity.class.getSimpleName();
	//TODO - refactor
	public               boolean settingView = false;
	ImageButton pageSwitchButton;
	ImageButton setupButton;
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link FragmentPagerAdapter} derivative, which will keep every
	 * loaded fragment in memory. If this becomes too memory intensive, it
	 * may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	private SettingsPagerAdapter mSettingPagerAdapter;
	private StatusPagerAdapter   mStatusPagerAdapter;
	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager            mViewPager;
	private TabLayout            mTabLayout;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_renatus_landing);
		mSettingPagerAdapter = new SettingsPagerAdapter(getSupportFragmentManager());
		mStatusPagerAdapter = new StatusPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.container);
		mTabLayout = (TabLayout) findViewById(R.id.tabs);
		mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
		pageSwitchButton = (ImageButton) findViewById(R.id.pageSwitchButton);
		pageSwitchButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				setViewPager();
			}
		});
		setupButton = (ImageButton) findViewById(R.id.logo);
		setupButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
/*				if (settingView == true && mTabLayout.getSelectedTabPosition() == 0)
				{
					DefaultFragment.getInstance().show(getSupportFragmentManager(), "setup");
				}*/
//				else if (settingView == true && mTabLayout.getSelectedTabPosition() == 1)
//				{
//					Intent i = new Intent(RenatusLandingActivity.this, MainActivity.class);
//					startActivity(i);
//				}
//				else if (settingView == false && mTabLayout.getSelectedTabPosition() == 1)
//				{
//					Intent i = new Intent(RenatusLandingActivity.this, MainActivity.class);
//					startActivity(i);
//				}
			}
		});
		setupButton.setOnLongClickListener(new View.OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View view)
			{
				startActivity(new Intent(view.getContext(), RenatusEngineeringActivity.class));
				return true;
			}
		});
		setViewPager();
//		FloorContainer.getInstance().loadData();
	}
	
	
	public void setViewPager()
	{
		if (settingView == true)
		{
			mViewPager.setAdapter(mStatusPagerAdapter);
			mTabLayout.post(new Runnable()
			{
				@Override
				public void run()
				{
					mTabLayout.setupWithViewPager(mViewPager, true);
				}
			});
			settingView = false;
			pageSwitchButton.setImageResource(R.drawable.setting);
		}
		else
		{
			mViewPager.setAdapter(mSettingPagerAdapter);
			mTabLayout.post(new Runnable()
			{
				@Override
				public void run()
				{
					mTabLayout.setupWithViewPager(mViewPager, true);
				}
			});
			settingView = true;
			pageSwitchButton.setImageResource(R.drawable.status);
		}
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_renatus_landing, menu);
		return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks isOn the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings)
		{
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	public void onPause()
	{
		super.onPause();
	}
}
