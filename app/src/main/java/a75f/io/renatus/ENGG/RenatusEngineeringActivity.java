package a75f.io.renatus.ENGG;

import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
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
		rowBradcast();
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
	/**
	 * Just to simulate the device
	 */
	private void rowBradcast(){

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				Log.i("MK_DEBUG", "run: ------Started Bradcast---------");
				Intent otaUpdateIntent = new Intent(Globals.IntentActions.PUBNUB_MESSAGE);
				otaUpdateIntent.putExtra("id", "157e84cd-bc7e-4c6e-ac5b-aae32d57aed3");
				otaUpdateIntent.putExtra("firmwareVersion", "HyperState_v1.0");
				otaUpdateIntent.putExtra("cmdLevel", "zone");
				Log.i("MK_DEBUG", "run: ------Sent bradcast message---------");

				sendBroadcast(otaUpdateIntent);

			}
		},15000);
	}
}
