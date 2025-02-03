package a75f.io.renatus;

import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_CONFIGURATION;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE_BBMD;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE_FD;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_DEVICE_TYPE_NORMAL;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_FD_AUTO_STATE;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_FD_CONFIGURATION;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_CONFIG_FILE_CREATED;
import static a75f.io.logic.util.bacnet.BacnetUtilKt.getUpdatedExistingBacnetConfigDeviceData;
import static a75f.io.logic.util.bacnet.BacnetUtilKt.populateBacnetConfigurationObject;
import static a75f.io.logic.bo.util.CCUUtils.isRecommendedVersionCheckIsNotFalse;
import static a75f.io.renatus.CcuRefReceiver.REQUEST_CCU_REF_ACTION;
import static a75f.io.renatus.Communication.isPortAvailable;
import static a75f.io.renatus.UtilityApplication.context;
import static a75f.io.renatus.bacnet.BacnetBackgroundTaskHandler.BACNET_FD_INTERVAL;
import static a75f.io.renatus.bacnet.BacnetBackgroundTaskHandler.BACNET_FD_IS_AUTO_ENABLED;
import static a75f.io.renatus.registration.UpdateCCUFragment.abortCCUDownloadProcess;
import static a75f.io.usbserial.UsbServiceActions.ACTION_USB_REQUIRES_TABLET_REBOOT;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import a75f.io.alerts.AlertManager;
import a75f.io.domain.api.Domain;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.interfaces.RemoteCommandHandleInterface;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.messaging.handler.RemoteCommandUpdateHandler;
import a75f.io.renatus.ENGG.RenatusEngineeringActivity;
import a75f.io.renatus.bacnet.BacnetBackgroundTaskHandler;
import a75f.io.renatus.bacnet.BacnetConfigChange;
import a75f.io.renatus.registration.CustomViewPager;
import a75f.io.renatus.schedules.ScheduleGroupFragment;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.CloudConnetionStatusThread;
import a75f.io.renatus.util.DataFdObj;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.Receiver.ConnectionChangeReceiver;
import a75f.io.renatus.util.TestSignalManager;
import a75f.io.renatus.util.remotecommand.RemoteCommandHandlerUtil;
import a75f.io.renatus.util.remotecommand.bundle.BundleInstallManager;
import a75f.io.usbserial.UsbServiceActions;
import kotlin.Pair;

public class RenatusLandingActivity extends AppCompatActivity implements RemoteCommandHandleInterface, BacnetConfigChange {

    private static final String TAG = "LandingActivityLog";
    private static CountDownTimer countDownTimer;
    private static final long DISCONNECT_TIMEOUT = 3000;
    private static final long INTERVAL = 1000;
    private long mStopTimeInFuture;
    private static final long SCREEN_SWITCH_TIMEOUT_MILLIS = 3600000;
    //TODO - refactor
    public boolean settingView = false;
    private TabItem pageSettingButton;
    private TabItem pageDashBoardButton;
    private ImageView logo_75f;
    private ImageView powerbylogo;
    private ImageView menuToggle;
    private ImageView floorMenu;
    static CloudConnetionStatusThread mCloudConnectionStatus = null;
    private BroadcastReceiver mConnectionChangeReceiver;
    private CcuRefReceiver mCcuRefReceiver = null;
    private ImageView powerByLogoForCarrier;

    private BacnetBackgroundTaskHandler bacnetBackgroundTaskHandler;

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    private SettingsPagerAdapter mSettingPagerAdapter;
    public static StatusPagerAdapter mStatusPagerAdapter;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    public static CustomViewPager mViewPager;
    public static TabLayout mTabLayout, btnTabs;
    private Prefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        executorService = Executors.newFixedThreadPool(1);
        CcuLog.i("UI_PROFILING","LandingActivity.onCreate");
        prefs = new Prefs(this);
        CCUUiUtil.setThemeDetails(this);
        mConnectionChangeReceiver = new ConnectionChangeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        this.registerReceiver(mConnectionChangeReceiver, intentFilter);

        // * Register the receiver that the RemoteApp uses to request CCU info
        mCcuRefReceiver = new CcuRefReceiver();
        IntentFilter raaIntentFilter = new IntentFilter();
        raaIntentFilter.addAction(REQUEST_CCU_REF_ACTION);
        registerReceiver(mCcuRefReceiver, raaIntentFilter);

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
            logo_75f = findViewById(R.id.logo_75f);
            powerbylogo = findViewById(R.id.powerbylogo);
            pageSettingButton = findViewById(R.id.pageSettingButton);
            pageDashBoardButton = findViewById(R.id.pageDashBoardButton);
            configLogo();

            btnTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    btnTabs.setEnabled(false);
                    try {
                        mViewPager.removeAllViews();
                        mViewPager.setAdapter(null);
                    } catch (IllegalStateException e) {
                      CcuLog.e(TAG,"IllegalStateException",e);
                    }
                    if (tab.getPosition() == 0){
                        if (isSetupPassWordRequired()) {
                            showRequestPasswordAlert("Setup Access Authentication",getString(R.string.USE_SETUP_PASSWORD_KEY), tab.getPosition());
                        }
                        tab.setIcon(R.drawable.ic_settings_orange);
                        mSettingPagerAdapter = new SettingsPagerAdapter(getSupportFragmentManager());
                        mViewPager.setAdapter(mSettingPagerAdapter);
                        mTabLayout.post(() -> mTabLayout.setupWithViewPager(mViewPager, true));
                        startCountDownTimer(INTERVAL);
                        setMarginStart(mTabLayout);
                        floorMenu.setVisibility(View.GONE);

                    } else if (tab.getPosition() == 1){
                        tab.setIcon(R.drawable.ic_dashboard_orange);
                        mStatusPagerAdapter = new StatusPagerAdapter(getSupportFragmentManager());
                        mViewPager.setAdapter(mStatusPagerAdapter);
                        mTabLayout.post(() -> mTabLayout.setupWithViewPager(mViewPager, true));
                        if (isZonePassWordRequired()) {
                            showRequestPasswordAlert("Zone Settings Authentication", getString(R.string.ZONE_SETTINGS_PASSWORD_KEY), 0);
                        }
                        floorMenu.setVisibility(View.VISIBLE);
                        menuToggle.setVisibility(View.GONE);
                    }
                    btnTabs.setEnabled(true);
                }

                private void setMarginStart(TabLayout mTabLayout) {
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                    );
                    mTabLayout.setLayoutParams(layoutParams);
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
                if (SettingsFragment.slidingPane.isOpen() || SystemConfigFragment.slidingSysPane.isOpen()) {
                    SettingsFragment.slidingPane.closePane();
                    SystemConfigFragment.slidingSysPane.closePane();
                } else {
                    SettingsFragment.slidingPane.openPane();
                    SystemConfigFragment.slidingSysPane.openPane();
                }
            });
            logo_75f.setOnLongClickListener(view -> {
                startActivity(new Intent(view.getContext(), RenatusEngineeringActivity.class));
                return true;
            });
            setViewPager();
            ScheduleManager.getInstance().updateSchedules();

            WeatherDataDownloadService.getWeatherData();

            floorMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Fragment currentFragment = mStatusPagerAdapter.getItem(mViewPager.getCurrentItem());
                    if (currentFragment != null && currentFragment instanceof ZoneFragmentNew) {
                        DrawerLayout mDrawerLayout = findViewById(R.id.drawer_layout);
                        LinearLayout drawer_screen = findViewById(R.id.drawer_screen);
                        try {
                            mDrawerLayout.openDrawer(drawer_screen);
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (mDrawerLayout != null && !mDrawerLayout.isShown()) {
                                mDrawerLayout.openDrawer(drawer_screen);
                            }
                        }
                    } else{
                        startCountDownTimer(INTERVAL);
                    }


                }
            });
        }
        CcuLog.i("UI_PROFILING","LandingActivity.onCreate Completed");

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbServiceActions.ACTION_USB_REQUIRES_TABLET_REBOOT);
        registerReceiver(mUsbEventReceiver, filter);
        CcuLog.e(L.TAG_CCU, "LifeCycleEvent LandingActivity Created");
        populateBACnetConfiguration();
        intializeBACnet();
        ccuLaunched();

        // If we just restarted after a bundled install, we need to perform
        // a few housekeeping tasks.  This will clear the existing bundle install
        // status and reboot the tablet if necessary.
        // This function runs in timer thread, it will run after 5mins
        BundleInstallManager.Companion.completeBundleInstallIfNecessary();

        // For Golden Release we need to update all side apps to recommended version
        // But we don't want to do this if CCU is replaced.
        // This function runs in timer thread, it will run after 1min
        if (!PreferenceUtil.isSideAppsUpdateFinished() && isRecommendedVersionCheckIsNotFalse()) {
            BundleInstallManager.Companion.initUpdatingSideAppsToRecommended();
        }

        checkBacnetDeviceType();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        CcuLog.i(TAG,"in user interaction");
        resetCountDownTimer();
    }

    private void resetCountDownTimer(){
        CcuLog.i(TAG,"resetCountDownTimer ");
        stopCountdownTimer();
        startCountDownTimer(DISCONNECT_TIMEOUT);
    }

    @SuppressLint("LogNotTimber")
    private void startCountDownTimer(long interval) {
        CcuLog.i(TAG,"startCountDownTimer ");
        mStopTimeInFuture = System.currentTimeMillis() + SCREEN_SWITCH_TIMEOUT_MILLIS;

        if (countDownTimer != null)
            countDownTimer.cancel();

        countDownTimer = new CountDownTimer(SCREEN_SWITCH_TIMEOUT_MILLIS, interval) {
            @Override
            public void onTick(long l) {
            }
            @Override
            public void onFinish() {
                CcuLog.i(TAG,"onFinish ");
                launchZoneFragment();
                stopCountdownTimer();
            }
        };
        countDownTimer.start();
    }

    private void launchZoneFragment() {
        CcuLog.i(TAG,"launch ZoneFragment");
        Globals.getInstance().setTestMode(false);
        TestSignalManager.INSTANCE.restoreAllPoints();
        Globals.getInstance().setTemporaryOverrideMode(false);
        if( btnTabs.getSelectedTabPosition() != 0) {
            updateStatusViewPagerAdapter();
        }
        btnTabs.getTabAt(1).select();
        mTabLayout.post(() -> mTabLayout.setupWithViewPager(mViewPager, true));
        // Better to add tag instead of class name, coz class name will be something like
        // class a75f.io.renatus.ZoneFragmentNew
        List<String> listOfFragmentTagToRemove = new ArrayList<>();
        listOfFragmentTagToRemove.add("ABOUT_FRAGMENT_TAG");
        removeFragment(listOfFragmentTagToRemove);
    }

    private void removeFragment(List<String> fragmentTagToRemove) {
        FragmentManager fm = getSupportFragmentManager();
        CcuLog.i(TAG,"Fragments To Remove: "+fragmentTagToRemove);
        try {
            if (!isFinishing() && !fm.isDestroyed()) {
                for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
                    fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                for (Fragment fragment : fm.getFragments()) {
                    if (fragmentTagToRemove.contains(fragment.getTag())) {
                        fm.beginTransaction().remove(fragment).commit();
                        CcuLog.i(TAG,"Fragment Removed: "+ fragment.getClass());
                    }
                }
            }
        } catch (IllegalStateException e){
            CcuLog.e(TAG,"IllegalStateException",e);
            e.printStackTrace();
        }
    }


    @SuppressLint("LogNotTimber")
    private  void stopCountdownTimer() {
        CcuLog.i(TAG,"stopCountdownTimer");
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        resetCountDownTimer();
        return super.onTouchEvent(event);
    }

    public void setViewPager() {
        menuToggle.setVisibility(View.GONE);
        floorMenu.setVisibility(View.GONE);
        mViewPager.setOffscreenPageLimit(1);

        mViewPager.setAdapter(mStatusPagerAdapter);
        btnTabs.getTabAt(1).select();

        mTabLayout.post(() -> mTabLayout.setupWithViewPager(mViewPager, true));


        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                if (i == 1 && mViewPager.getAdapter().instantiateItem(mViewPager, i)  instanceof SystemConfigFragment ) {
                    menuToggle.setVisibility(View.VISIBLE);
                    floorMenu.setVisibility(View.GONE);
                    startCountDownTimer(INTERVAL);
                } else if(i==2 && mViewPager.getAdapter().instantiateItem(mViewPager,i) instanceof SettingsFragment){
                    menuToggle.setVisibility(View.VISIBLE);
                    floorMenu.setVisibility(View.GONE);
                    startCountDownTimer(INTERVAL);
                } else if (i == 0 && mViewPager.getAdapter().instantiateItem(mViewPager, i) instanceof ZoneFragmentNew){
                    if (isZonePassWordRequired()) {
                        showRequestPasswordAlert("Zone Settings Authentication", getString(R.string.ZONE_SETTINGS_PASSWORD_KEY), i);
                    }
                    menuToggle.setVisibility(View.GONE);
                    floorMenu.setVisibility(View.VISIBLE);
                }  else if (i == 1 && mViewPager.getAdapter().instantiateItem(mViewPager, i) instanceof SystemFragment){
                    if (isSystemPassWordRequired()) {
                        showRequestPasswordAlert("System Settings Authentication", getString(R.string.SYSTEM_SETTINGS_PASSWORD_KEY), i);
                    }
                    menuToggle.setVisibility(View.GONE);
                    floorMenu.setVisibility(View.GONE);
                    startCountDownTimer(INTERVAL);
                    mViewPager.getAdapter().notifyDataSetChanged();
                }   else if (i == 2 && mViewPager.getAdapter().instantiateItem(mViewPager, i) instanceof ScheduleGroupFragment){
                    if (isBuildingPassWordRequired()) {
                        showRequestPasswordAlert("Building Settings Authentication", getString(R.string.BUILDING_SETTINGS_PASSWORD_KEY), i);
                    }
                    menuToggle.setVisibility(View.GONE);
                    floorMenu.setVisibility(View.GONE);
                    startCountDownTimer(INTERVAL);
                }else {
                    floorMenu.setVisibility(View.GONE);
                    menuToggle.setVisibility(View.GONE);
                    startCountDownTimer(INTERVAL);
                }
                Globals.getInstance().selectedTab = i;
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                LinearLayout tabLayout = (LinearLayout)((ViewGroup) mTabLayout.getChildAt(0)).getChildAt(tab.getPosition());
                TextView tabTextView = (TextView) tabLayout.getChildAt(1);

                tabTextView.setTextAppearance(tabLayout.getContext(), R.attr.RenatusTabTextSelected);
                if (btnTabs.getSelectedTabPosition() == 0 &&  mTabLayout.getSelectedTabPosition() != 0) { //This method wills when we switch from floor to other tab in settings
                    FloorPlanFragment.getInstance().destroyActionBar();
                }

            }

            @SuppressLint("ResourceType")
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                LinearLayout tabLayout = (LinearLayout)((ViewGroup) mTabLayout.getChildAt(0)).getChildAt(tab.getPosition());
                TextView tabTextView = (TextView) tabLayout.getChildAt(1);

                tabTextView.setTextAppearance(tabLayout.getContext(), R.attr.RenatusLandingTabTextStyle);
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
    public void onResume() {
        super.onResume();
        isCloudConnectionAlive();
        RemoteCommandUpdateHandler.setRemoteCommandInterface(this);
    }
    @Override
    public void onPause() {
        super.onPause();
        RemoteCommandUpdateHandler.setRemoteCommandInterface(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCloudConnectionStatus.stopThread();
        L.saveCCUState();
        ccuLaunched();
        AlertManager.getInstance().clearAlertsWhenAppClose();
        appRestarted();
        abortCCUDownloadProcess();
        try {
            if (mConnectionChangeReceiver != null) {
                this.unregisterReceiver(mConnectionChangeReceiver);
            }
            if (mUsbEventReceiver != null) {
                unregisterReceiver(mUsbEventReceiver);
            }
            if (mCcuRefReceiver != null) {
                this.unregisterReceiver(mCcuRefReceiver);
                mCcuRefReceiver = null;
            }
        } catch (Exception e) {
            // already unregistered
        }
        RenatusApp.backgroundServiceInitiator.unbindServices();
        if(bacnetBackgroundTaskHandler != null) {
            bacnetBackgroundTaskHandler.stopHandler();
        }
        CcuLog.e(L.TAG_CCU, "LifeCycleEvent LandingActivity Destroyed");
    }

    private void appRestarted() {
        AlertManager.getInstance().getRepo().setRestartAppToTrue();
        Domain.diagEquip.getAppRestart().writeHisVal(1.0);
    }

    private void ccuLaunched(){
        PreferenceUtil.setIsCcuLaunched(true);
        CCUUtils.setCCUReadyProperty("false");
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        }
    }
    public static synchronized boolean isCloudConnectionAlive() {
        if (mCloudConnectionStatus == null) {
            mCloudConnectionStatus = new CloudConnetionStatusThread();
            mCloudConnectionStatus.start();
        }
        return mCloudConnectionStatus.isCloudAlive();
    }
    public void showRequestPasswordAlert(String title, String key, int position) {

        final int[] passwordAttempt = {0};
        String password = getSavedPassword(key);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        SpannableString spannable = new SpannableString(title);
        spannable.setSpan(new ForegroundColorSpan(CCUUiUtil.getPrimaryThemeColor(RenatusLandingActivity.this)), 0, title.length(), 0);
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
                if (position == 0 && key.equals(getString(R.string.USE_SETUP_PASSWORD_KEY))) {
                    btnTabs.getTabAt(1).setIcon(R.drawable.ic_dashboard_orange);
                    btnTabs.getTabAt(1).select();
                    updateStatusViewPagerAdapter();
                    mTabLayout.post(() -> mTabLayout.setupWithViewPager(mViewPager, true));

                    menuToggle.setVisibility(View.GONE);
                    floorMenu.setVisibility(View.VISIBLE);
                    dialog.dismiss();
                    return;
                } else if (position == 0 && key.equals(getString(R.string.ZONE_SETTINGS_PASSWORD_KEY))) {
                    mViewPager.setCurrentItem(3);
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

    @Override
    public void updateRemoteCommands(String commands,String cmdLevel,String id) {
        CcuLog.d("RemoteCommand","LandingActivity.UpdateRemoteCommands="+commands+","+cmdLevel);
        RemoteCommandHandlerUtil.handleRemoteCommand(commands,cmdLevel,id);
    }

    @Override
    public void updateRemoteCommands(JsonObject msgObject) {
        CcuLog.d("RemoteCommand","LandingActivity.UpdateRemoteCommands="+msgObject.toString());
        RemoteCommandHandlerUtil.handleRemoteCommand(msgObject);
    }

    private void configLogo(){

        if(CCUUiUtil.isDaikinEnvironment(this)){
            logo_75f.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.d3, null));
            powerbylogo.setVisibility(View.VISIBLE);
        }else if (CCUUiUtil.isCarrierThemeEnabled(this)) {
            logo_75f.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ccu_carrier_logo, null));
            powerbylogo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.airoverse_75f_powered_by, null));
            powerbylogo.setVisibility(View.VISIBLE);
        }else if (CCUUiUtil.isAiroverseThemeEnabled(this)) {
            logo_75f.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.airoverse_brand_logo, null));
            powerbylogo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.airoverse_75f_powered_by, null));
            powerbylogo.setVisibility(View.VISIBLE);
        }else {
            logo_75f.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_75f_logo, null));
            powerbylogo.setVisibility(View.GONE);
        }

    }

    private final BroadcastReceiver mUsbEventReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_USB_REQUIRES_TABLET_REBOOT:
                    CcuLog.i("CCU_SERIAL"," SHOW REBOOT DIALOG");
                    CCUUiUtil.showRebootDialog(RenatusLandingActivity.this);
                    Toast.makeText(context, "USB device not connected", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    private void populateBACnetConfiguration() {
        String confString;
        if (!prefs.getBoolean(IS_BACNET_CONFIG_FILE_CREATED)) {
            confString = populateBacnetConfigurationObject().toString();
            prefs.setBoolean(IS_BACNET_CONFIG_FILE_CREATED, true);
            prefs.setString(BACNET_CONFIGURATION, confString);
        } else {
            Pair<String, Boolean> updatedConfig = getUpdatedExistingBacnetConfigDeviceData(prefs.getString(BACNET_CONFIGURATION));
            if(updatedConfig.component2().equals(true)) {
                prefs.setString(BACNET_CONFIGURATION, updatedConfig.component1().toString());
            }
        }
    }

    private void intializeBACnet() {
        if(UtilityApplication.isBACnetIntialized()) {
            executeTask();
        }
    }

    private ExecutorService executorService;
    private void executeTask() {
        executorService.submit(() -> {
            boolean isPortAvailable = isPortAvailable(5001);
            this.runOnUiThread(() -> {
                handleClick(isPortAvailable);
            });
        });
    }

    private void handleClick(boolean isPortAvailable){
        if(!isPortAvailable){
            Toast.makeText(context, "Port is busy try after some time", Toast.LENGTH_SHORT).show();
            return;
        }
        UtilityApplication.startRestServer();
    }
    private void updateStatusViewPagerAdapter() {
        mViewPager.removeAllViews();
        mViewPager.setAdapter(null);
        mStatusPagerAdapter = new StatusPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mStatusPagerAdapter);
    }

    @Override
    public void submitConfiguration(String configurationType, boolean isAutoEnabled, int timeInSeconds) {
        CcuLog.d(TAG, "submitConfiguration--->"+isAutoEnabled);
        if(bacnetBackgroundTaskHandler == null){
            bacnetBackgroundTaskHandler = new BacnetBackgroundTaskHandler();
        }
        bacnetBackgroundTaskHandler.sendBroadCastToBacApp();
        bacnetBackgroundTaskHandler.removeOldMessages(1);

        if(timeInSeconds > 0) {
            Message message = Message.obtain();
            message.what = 1;
            Bundle bundle = new Bundle();
            bundle.putInt(BACNET_FD_INTERVAL, timeInSeconds);
            bundle.putBoolean(BACNET_FD_IS_AUTO_ENABLED, isAutoEnabled);
            message.setData(bundle);
            CcuLog.d(TAG, "submitConfiguration--isAutoEnabled->"+isAutoEnabled + "<---time in secnds-->"+timeInSeconds);
            bacnetBackgroundTaskHandler.sendMessageDelayed(message, timeInSeconds * 1000L);
        }else{
            CcuLog.d(TAG, "submitConfiguration--isAutoEnabled->"+isAutoEnabled + "<---time in is 0 or less stop handler-->"+timeInSeconds);
            //bacnetBackgroundTaskHandler.stopHandler();
            bacnetBackgroundTaskHandler.removeCallBacks();
        }
    }

    private void checkBacnetDeviceType() {
        CcuLog.d(TAG, "---check if fd configuration is present---");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String bacnetDeviceType = sharedPreferences.getString(BACNET_DEVICE_TYPE, null);
        if(bacnetDeviceType != null){
            if(bacnetDeviceType.equalsIgnoreCase(BACNET_DEVICE_TYPE_FD)){
                CcuLog.d(TAG, "---bacnet configuration is ---"+BACNET_DEVICE_TYPE_FD);
                String config = sharedPreferences.getString(BACNET_FD_CONFIGURATION, null);
                DataFdObj dataFdObj = new Gson().fromJson(config, DataFdObj.class);
                boolean isAutoEnabled = sharedPreferences.getBoolean(BACNET_FD_AUTO_STATE, false);
                CcuLog.d(TAG, "---fd configuration isAutoEnabled---" + isAutoEnabled + "--config--" + config + "<--dataFdObj-->"+dataFdObj);
                if (config != null && dataFdObj != null) {
                    int time = dataFdObj.getDataFd().getBbmdMask();
                    if (time > 0) {
                        submitConfiguration("fd", isAutoEnabled, time);
                    }
                }
            }else if(bacnetDeviceType.equalsIgnoreCase(BACNET_DEVICE_TYPE_BBMD)){
                CcuLog.d(TAG, "---bacnet configuration is ---"+BACNET_DEVICE_TYPE_BBMD);
            }else{
                CcuLog.d(TAG, "---bacnet configuration is ---"+ BACNET_DEVICE_TYPE_NORMAL);
            }
        }
    }
}
