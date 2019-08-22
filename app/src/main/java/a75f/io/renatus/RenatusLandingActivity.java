package a75f.io.renatus;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.renatus.ENGG.RenatusEngineeringActivity;
import a75f.io.renatus.registartion.CustomViewPager;
import a75f.io.renatus.util.CCUUtils;

public class RenatusLandingActivity extends AppCompatActivity {

    private static final String TAG = RenatusLandingActivity.class.getSimpleName();
    //TODO - refactor
    public boolean settingView = false;
    ImageButton pageSwitchButton;
    ImageButton setupButton;
    ImageView menuToggle;
    ImageView floorMenu;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SettingsPagerAdapter mSettingPagerAdapter;
    private StatusPagerAdapter mStatusPagerAdapter;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private CustomViewPager mViewPager;
    private TabLayout mTabLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isFinishing()) {
            setContentView(R.layout.activity_renatus_landing);
            mSettingPagerAdapter = new SettingsPagerAdapter(getSupportFragmentManager());
            mStatusPagerAdapter = new StatusPagerAdapter(getSupportFragmentManager());

            floorMenu = findViewById(R.id.floorMenu);
            menuToggle = findViewById(R.id.menuToggle);
            mViewPager = findViewById(R.id.container);
            mTabLayout = findViewById(R.id.tabs);
            mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

            pageSwitchButton = findViewById(R.id.pageSwitchButton);
            pageSwitchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setViewPager();
                }
            });
            menuToggle.setOnClickListener(view -> {
                if (SettingsFragment.slidingPane.isOpen()) {
                    SettingsFragment.slidingPane.closePane();
                } else {
                    SettingsFragment.slidingPane.openPane();
                }
            });
            setupButton = (ImageButton) findViewById(R.id.logo);
            setupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
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
            setupButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    startActivity(new Intent(view.getContext(), RenatusEngineeringActivity.class));
                    return true;
                }
            });
            setViewPager();
            ScheduleProcessJob.updateSchedules();
            HashMap site = CCUHsApi.getInstance().read("site");
            String siteCountry = site.get("geoCountry").toString();
            String siteZipCode = site.get("geoPostalCode").toString();
            CCUUtils.getLocationInfo(siteCountry + " " + siteZipCode);

            floorMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Fragment currentFragment = mStatusPagerAdapter.getItem(mViewPager.getCurrentItem());
                    //if(currentFragment !=null && currentFragment instanceof ZoneFragmentTemp)
                    if (currentFragment != null && currentFragment instanceof ZoneFragmentNew) {
                        //if(zoneFragmentTemp instanceof ZoneFragmentTemp)
                        //Bitmap map = BlurEffect.takeScreenShot(RenatusLandingActivity.this);
                        //Bitmap fast = BlurEffect.fastblur(map, 10);
                        //final Drawable draw=new BitmapDrawable(getResources(),fast);


                        //ZoneFragmentTemp zoneFragmentTemp = (ZoneFragmentTemp)mStatusPagerAdapter.getItem(mViewPager.getCurrentItem());
                        ZoneFragmentNew zoneFragmentTemp = (ZoneFragmentNew) mStatusPagerAdapter.getItem(mViewPager.getCurrentItem());

                        DrawerLayout mDrawerLayout = findViewById(R.id.drawer_layout);
                        LinearLayout drawer_screen = findViewById(R.id.drawer_screen);
                        //zoneFragmentTemp.openFloor();
                        try {
                            //zoneFragmentTemp.mDrawerLayout.openDrawer(zoneFragmentTemp.drawer_screen);
                            mDrawerLayout.openDrawer(drawer_screen);
                            //mDrawerLayout.setBackgroundDrawable(draw);
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (mDrawerLayout != null && !mDrawerLayout.isShown()) {
                                mDrawerLayout.openDrawer(drawer_screen);
                                //mDrawerLayout.setBackgroundDrawable(draw);
                            }
                           /* if (zoneFragmentTemp.mDrawerLayout != null && !zoneFragmentTemp.mDrawerLayout.isShown()) {
                                zoneFragmentTemp.mDrawerLayout.openDrawer(zoneFragmentTemp.drawer_screen);
                            }*/
                        }
                        //ZoneFragmentTemp fragment = (ZoneFragmentTemp) getSupportFragmentManager().findFragmentById(R.id.container);
                        //fragment.openFloor();
                    }
                }
            });
        }
    }

    public void setViewPager() {
        menuToggle.setVisibility(View.GONE);
        floorMenu.setVisibility(View.GONE);
        mViewPager.setOffscreenPageLimit(1);
        if (settingView == true) {

            mViewPager.setAdapter(mStatusPagerAdapter);
            mTabLayout.post(new Runnable() {
                @Override
                public void run() {
                    mTabLayout.setupWithViewPager(mViewPager, true);
                }
            });
            settingView = false;
            pageSwitchButton.setImageResource(R.drawable.setting);
        } else {
            mViewPager.setAdapter(mSettingPagerAdapter);
            mTabLayout.post(new Runnable() {
                @Override
                public void run() {
                    mTabLayout.setupWithViewPager(mViewPager, true);
                }
            });
            settingView = true;
            pageSwitchButton.setImageResource(R.drawable.status);
        }


        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                if (settingView && i == 1) menuToggle.setVisibility(View.VISIBLE);
                else menuToggle.setVisibility(View.GONE);
                if (!settingView && i == 1) floorMenu.setVisibility(View.VISIBLE);
                else floorMenu.setVisibility(View.GONE);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_renatus_landing, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks isOn the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        L.saveCCUState();
    }

    @Override
    public void onBackPressed() {
        if (!(getSupportFragmentManager().getFragments().get(mViewPager.getCurrentItem()) instanceof SettingsFragment) && getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
           //no -op
        }
    }

    public void setPointVal(String id, double val) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {

                CCUHsApi hayStack = CCUHsApi.getInstance();
                Point p = new Point.Builder().setHashMap(hayStack.readMapById(id)).build();
                if (p.getMarkers().contains("writable")) {
                    CcuLog.d(L.TAG_CCU_UI, "Set Writbale Val " + p.getDisplayName() + ": " + val);
                    //CCUHsApi.getInstance().pointWrite(HRef.copy(id), TunerConstants.MANUAL_OVERRIDE_VAL_LEVEL, "manual", HNum.make(val) , HNum.make(2 * 60 * 60 * 1000, "ms"));
                    ScheduleProcessJob.handleDesiredTempUpdate(p, true, val);

                }

                if (p.getMarkers().contains("his")) {
                    CcuLog.d(L.TAG_CCU_UI, "Set His Val " + id + ": " + val);
                    hayStack.writeHisValById(id, val);
                }
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }
}
