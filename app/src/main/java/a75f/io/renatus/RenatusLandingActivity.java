package a75f.io.renatus;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabItem;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.renatus.ENGG.RenatusEngineeringActivity;
import a75f.io.renatus.registartion.CustomViewPager;
import a75f.io.renatus.schedules.SchedulerFragment;
import a75f.io.renatus.util.CCUUtils;
import a75f.io.renatus.util.Prefs;

public class RenatusLandingActivity extends AppCompatActivity {

    private static final String TAG = RenatusLandingActivity.class.getSimpleName();
    //TODO - refactor
    public boolean settingView = false;
    TabItem pageSettingButton;
    TabItem pageDashBoardButton;
    ImageView setupButton;
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
    private Prefs prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
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
            if (isSetupPassWordRequired()) {
                showRequestPasswordAlert("Setup Access Authentication",getString(R.string.USE_SETUP_PASSWORD_KEY), 0);
            }

            btnTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                      if (tab.getPosition() == 0){
                          if (isSetupPassWordRequired()) {
                              showRequestPasswordAlert("Setup Access Authentication",getString(R.string.USE_SETUP_PASSWORD_KEY), tab.getPosition());
                          }
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
            /*setupButton = findViewById(R.id.logo_daikin);
            setupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
*//*				if (settingView == true && mTabLayout.getSelectedTabPosition() == 0)
                {
					DefaultFragment.getInstance().show(getSupportFragmentManager(), "setup");
				}*//*
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
            });*/
            findViewById(R.id.logo_75f).setOnLongClickListener(view -> {
                startActivity(new Intent(view.getContext(), RenatusEngineeringActivity.class));
                return true;
            });
            setViewPager();
            ScheduleProcessJob.updateSchedules();
            HashMap site = CCUHsApi.getInstance().read("site");
            HashMap ccu = CCUHsApi.getInstance().read("device and ccu");
            String siteCountry = site.get("geoCountry").toString();
            String siteZipCode = site.get("geoPostalCode").toString();
            CCUUtils.getLocationInfo(siteCountry + " " + siteZipCode);
            /*try {
                JSONObject ccuRegInfo = new JSONObject();
                JSONObject locInfo = new JSONObject();
                locInfo.put("geoCity", site.get("geoCity").toString());
                locInfo.put("geoCountry", siteCountry);
                locInfo.put("geoState", site.get("geoState").toString());
                locInfo.put("geoAddr", site.get("geoAddr").toString());
                locInfo.put("geoPostalCode", siteZipCode);
                if(site.get("organization") != null)
				    locInfo.put("organization", site.get("organization").toString());
                if(site.size() > 0) {
                    ccuRegInfo.put("siteName", site.get("dis").toString());
                    String siteGUID = CCUHsApi.getInstance().getGUID(site.get("id").toString());
                    ccuRegInfo.put("siteId", siteGUID);
                    if (ccu.size() > 0) {
                        String ccuGUID = CCUHsApi.getInstance().getGUID(ccu.get("id").toString());
                        ccuRegInfo.put("deviceId", ccuGUID);
                        ccuRegInfo.put("deviceName", ccu.get("dis").toString());
                        ccuRegInfo.put("facilityManagerEmail", ccu.get("fmEmail").toString());
                        ccuRegInfo.put("installerEmail", ccu.get("fmEmail").toString());
                        ccuRegInfo.put("locationDetails", locInfo);
                        CcuLog.d("CCURegInfo", "createNewSite json Edit =" + ccuRegInfo.toString());

                        updateCCURegistrationInfo(ccuRegInfo.toString());
                    }
                }
            }catch (JSONException e){
                e.printStackTrace();
            }*/
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
                    if (isZonePassWordRequired()) {
                        showRequestPasswordAlert("Zone Settings Authentication", getString(R.string.ZONE_SETTINGS_PASSWORD_KEY), i);
                    }
                    menuToggle.setVisibility(View.GONE);
                    floorMenu.setVisibility(View.VISIBLE);
                }  else if (i == 2 && mViewPager.getAdapter().instantiateItem(mViewPager, i) instanceof SystemFragment){
                    if (isSystemPassWordRequired()) {
                        showRequestPasswordAlert("System Settings Authentication", getString(R.string.SYSTEM_SETTINGS_PASSWORD_KEY), i);
                    }
                    menuToggle.setVisibility(View.GONE);
                    floorMenu.setVisibility(View.VISIBLE);
                }   else if (i == 3 && mViewPager.getAdapter().instantiateItem(mViewPager, i) instanceof SchedulerFragment){
                    if (isBuildingPassWordRequired()) {
                        showRequestPasswordAlert("Building Settings Authentication", getString(R.string.BUILDING_SETTINGS_PASSWORD_KEY), i);
                    }
                    menuToggle.setVisibility(View.GONE);
                    floorMenu.setVisibility(View.VISIBLE);
                }else {
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

    public void showRequestPasswordAlert(String title, String key, int position) {

        final int[] passwordAttempt = {0};
        String password = getSavedPassword(key);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        SpannableString spannable = new SpannableString(title);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.accent)), 0, title.length(), 0);
        builder.setTitle(spannable);
        builder.setCancelable(false);

        EditText etPassword = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(20,20,20,0);
        etPassword.setLayoutParams(lp);
        etPassword.setHint("Enter Password");
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassword.setTextSize(20);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(etPassword);
        builder.setView(layout);

        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("CANCEL", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button posButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            posButton.setEnabled(false);
            etPassword.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int count) {
                    if (count>0){
                        posButton.setEnabled(true);
                    } else {
                        posButton.setEnabled(false);
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
            posButton.setTextColor(getResources().getColor(R.color.black));
            posButton.setOnClickListener(view -> {
                if (etPassword.getText().toString().equals(password)) {
                    dialogInterface.dismiss();
                    mViewPager.setCurrentItem(position);
                    passwordAttempt[0] = 0;
                    prefs.setInt("PASSWORD_ATTEMPT",passwordAttempt[0]);
                } else {
                    Toast.makeText(RenatusLandingActivity.this, "Incorrect Password!", Toast.LENGTH_LONG).show();
                    passwordAttempt[0]++;
                    prefs.setInt("PASSWORD_ATTEMPT",passwordAttempt[0]);
                    etPassword.setText("");
                }
            });

            Button negButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negButton.setOnClickListener(view -> {
                if (position == 0){
                    btnTabs.getTabAt(1).setIcon(R.drawable.ic_dashboard_orange);
                    btnTabs.getTabAt(1).select();
                    mViewPager.setAdapter(mStatusPagerAdapter);
                    mTabLayout.post(() -> mTabLayout.setupWithViewPager(mViewPager, true));

                    menuToggle.setVisibility(View.GONE);
                    floorMenu.setVisibility(View.GONE);
                    dialog.dismiss();
                    return;
                }
                mViewPager.setCurrentItem(0);
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private String getSavedPassword(String key) {
        String password;
        String tag = "";
        if (key.contains("zone")){
            tag = getString(R.string.ZONE_SETTINGS_PASSWORD_KEY);
        } else if (key.contains("building")){
            tag = getString(R.string.BUILDING_SETTINGS_PASSWORD_KEY);
        } else if (key.contains("system")){
            tag = getString(R.string.SYSTEM_SETTINGS_PASSWORD_KEY);
        } else if (key.contains("setup")){
            tag = getString(R.string.USE_SETUP_PASSWORD_KEY);
        }
        password = prefs.getString(tag);
        return password;
    }

    private boolean isZonePassWordRequired(){
        return (prefs.getBoolean(getString(R.string.SET_ZONE_PASSWORD))&& !prefs.getString(getString(R.string.ZONE_SETTINGS_PASSWORD_KEY)).isEmpty());
    }

    private boolean isSystemPassWordRequired(){
        return (prefs.getBoolean(getString(R.string.SET_SYSTEM_PASSWORD))&& !prefs.getString(getString(R.string.SYSTEM_SETTINGS_PASSWORD_KEY)).isEmpty());
    }

    private boolean isBuildingPassWordRequired(){
        return (prefs.getBoolean(getString(R.string.SET_BUILDING_PASSWORD))&& !prefs.getString(getString(R.string.BUILDING_SETTINGS_PASSWORD_KEY)).isEmpty());
    }

    private boolean isSetupPassWordRequired(){
        return (prefs.getBoolean(getString(R.string.SET_SETUP_PASSWORD))&& !prefs.getString(getString(R.string.USE_SETUP_PASSWORD_KEY)).isEmpty());
    }


    private void updateCCURegistrationInfo(final String ccuRegInfo) {
        AsyncTask<Void, Void, String> updateCCUReg = new AsyncTask<Void, Void, String>() {


            @Override
            protected String doInBackground(Void... voids) {

                Log.d("CCURegInfo","RenatusLA backgroundtask="+ccuRegInfo);
                return  HttpUtil.executeJSONPost(CCUHsApi.getInstance().getAuthenticationUrl()+"api/v1/device/register",ccuRegInfo);
            }

            @Override
            protected void onPostExecute(String result) {

                Log.d("CCURegInfo","RenatusLA Edit onPostExecute="+result);
                if((result != null) && (!result.equals(""))){

                    try {
                        JSONObject resString = new JSONObject(result);
                        if(resString.getBoolean("success")){

                            Toast.makeText(getApplicationContext(), "CCU Registered Successfully "+resString.getString("deviceId"), Toast.LENGTH_LONG).show();
                        }else
                            Toast.makeText(getApplicationContext(), "CCU Registration is not Successful "+resString.getString("deviceId"), Toast.LENGTH_LONG).show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else {
                    Toast.makeText(getApplicationContext(), "CCU Registration is not Successful", Toast.LENGTH_LONG).show();
                }
            }
        };

        updateCCUReg.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
