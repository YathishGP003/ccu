package a75f.io.renatus.ENGG;

import com.google.android.material.tabs.TabLayout;

import androidx.core.content.res.ResourcesCompat;
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
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
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
		mTabLayout.post(() -> mTabLayout.setupWithViewPager(mViewPager, true));
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
		if(CCUUiUtil.isDaikinEnvironment(this))
			logo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.d3, null));
		else if (CCUUiUtil.isCarrierThemeEnabled(this))
			logo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ccu_carrier_logo, null));
		else if (CCUUiUtil.isAiroverseThemeEnabled(this))
			logo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.airoverse_brand_logo, null));
		else
			logo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_75f_logo, null));

	}
}
