package a75f.io.renatus.ENGG;

import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import a75f.io.logger.CcuLog;
import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.R;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.CCUUtils;

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
		CCUUiUtil.setThemeDetails(this);
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
		configLogo();
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

	private void configLogo(){
		ImageView logo = findViewById(R.id.logo);
		if(BuildConfig.BUILD_TYPE.equals("daikin_prod")||CCUUiUtil.isDaikinThemeEnabled(this)){
			logo.setImageDrawable(getResources().getDrawable(R.drawable.d3));

		}else{
			logo.setImageDrawable(getResources().getDrawable(R.drawable.ic_75f_logo));

		}

	}
}
