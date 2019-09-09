package a75f.io.renatus;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabItem;
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
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    TabItem pageSettingButton;
    TabItem pageDashBoardButton;
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
    private TabLayout mTabLayout, btnTabs;


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
            btnTabs = findViewById(R.id.btnTabs);
            mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
            btnTabs.setTabMode(TabLayout.MODE_SCROLLABLE);

            pageSettingButton = findViewById(R.id.pageSettingButton);
            pageDashBoardButton = findViewById(R.id.pageDashBoardButton);

            btnTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                      if (tab.getPosition() == 0){
                          tab.setIcon(R.drawable.ic_settings_orange);
                          mViewPager.setAdapter(mSettingPagerAdapter);
                          mTabLayout.post(() -> mTabLayout.setupWithViewPager(mViewPager, true));

                          menuToggle.setVisibility(View.GONE);
                          floorMenu.setVisibility(View.GONE);

                      } else if (tab.getPosition() == 1){
                          tab.setIcon(R.drawable.ic_dashboard_orange);
                          mViewPager.setAdapter(mStatusPagerAdapter);
                          mTabLayout.post(() -> mTabLayout.setupWithViewPager(mViewPager, true));

                          menuToggle.setVisibility(View.GONE);
                          floorMenu.setVisibility(View.GONE);
                      }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    if (tab.getPosition() == 0){
                        tab.setIcon(R.drawable.ic_account_white);
                    } else if(tab.getPosition() == 1) {
                        tab.setIcon(R.drawable.ic_tachometer);
                    }
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

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

        mViewPager.setAdapter(mSettingPagerAdapter);
        mTabLayout.post(() -> mTabLayout.setupWithViewPager(mViewPager, true));


        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                if (i == 1 && mViewPager.getAdapter().instantiateItem(mViewPager, i)  instanceof SettingsFragment ) {
                    menuToggle.setVisibility(View.VISIBLE);
                    floorMenu.setVisibility(View.GONE);
                } else if (i == 1 && mViewPager.getAdapter().instantiateItem(mViewPager, i) instanceof ZoneFragmentNew){
                    menuToggle.setVisibility(View.GONE);
                    floorMenu.setVisibility(View.VISIBLE);
                } else {
                    floorMenu.setVisibility(View.GONE);
                    menuToggle.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                LinearLayout tabLayout = (LinearLayout)((ViewGroup) mTabLayout.getChildAt(0)).getChildAt(tab.getPosition());
                TextView tabTextView = (TextView) tabLayout.getChildAt(1);

                tabTextView.setTextAppearance(tabLayout.getContext(), R.style.RenatusTabTextSelected);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                LinearLayout tabLayout = (LinearLayout)((ViewGroup) mTabLayout.getChildAt(0)).getChildAt(tab.getPosition());
                TextView tabTextView = (TextView) tabLayout.getChildAt(1);

                tabTextView.setTextAppearance(tabLayout.getContext(), R.style.RenatusLandingTabTextStyle);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

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
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        }
    }

}
