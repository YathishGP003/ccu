package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.HashMap;
import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbCmResetMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.logtasks.UploadLogs;
import a75f.io.logic.pubnub.RemoteCommandHandleInterface;
import a75f.io.logic.pubnub.RemoteCommandUpdateHandler;
import a75f.io.renatus.ENGG.AppInstaller;
import a75f.io.renatus.ENGG.RenatusEngineeringActivity;
import a75f.io.renatus.registration.CustomViewPager;
import a75f.io.renatus.schedules.SchedulerFragment;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.CCUUtils;
import a75f.io.renatus.util.CloudConnetionStatusThread;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.Receiver.ConnectionChangeReceiver;

import static a75f.io.logic.pubnub.RemoteCommandUpdateHandler.RESET_CM;
import static a75f.io.logic.pubnub.RemoteCommandUpdateHandler.RESTART_CCU;
import static a75f.io.logic.pubnub.RemoteCommandUpdateHandler.RESTART_MODULE;
import static a75f.io.logic.pubnub.RemoteCommandUpdateHandler.RESTART_TABLET;
import static a75f.io.logic.pubnub.RemoteCommandUpdateHandler.SAVE_CCU_LOGS;
import static a75f.io.logic.pubnub.RemoteCommandUpdateHandler.UPDATE_CCU;


public class RenatusLandingActivity extends AppCompatActivity implements RemoteCommandHandleInterface {

    private static final String TAG = RenatusLandingActivity.class.getSimpleName();
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

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
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
        CCUUiUtil.setThemeDetails(this);
        mConnectionChangeReceiver = new ConnectionChangeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        this.registerReceiver(mConnectionChangeReceiver, intentFilter);

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
                          if (isZonePassWordRequired()) {
                              showRequestPasswordAlert("Zone Settings Authentication", getString(R.string.ZONE_SETTINGS_PASSWORD_KEY), 0);
                          }

                          menuToggle.setVisibility(View.GONE);
                          floorMenu.setVisibility(View.VISIBLE);
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
            logo_75f.setOnLongClickListener(view -> {
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
            floorMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Fragment currentFragment = mStatusPagerAdapter.getItem(mViewPager.getCurrentItem());

                    if (currentFragment != null && currentFragment instanceof ZoneFragmentNew) {

                        ZoneFragmentNew zoneFragmentTemp = (ZoneFragmentNew) mStatusPagerAdapter.getItem(mViewPager.getCurrentItem());

                        DrawerLayout mDrawerLayout = findViewById(R.id.drawer_layout);
                        LinearLayout drawer_screen = findViewById(R.id.drawer_screen);
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

        mViewPager.setAdapter(mStatusPagerAdapter);
        btnTabs.getTabAt(1).select();

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
                }   else if (i == 2 && mViewPager.getAdapter().instantiateItem(mViewPager, i) instanceof SchedulerFragment){
                    if (isBuildingPassWordRequired()) {
                        showRequestPasswordAlert("Building Settings Authentication", getString(R.string.BUILDING_SETTINGS_PASSWORD_KEY), i);
                    }
                    menuToggle.setVisibility(View.GONE);
                    floorMenu.setVisibility(View.GONE);
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
            @SuppressLint("ResourceType")
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                LinearLayout tabLayout = (LinearLayout)((ViewGroup) mTabLayout.getChildAt(0)).getChildAt(tab.getPosition());
                TextView tabTextView = (TextView) tabLayout.getChildAt(1);

                tabTextView.setTextAppearance(tabLayout.getContext(), R.attr.RenatusTabTextSelected);
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
        prefs.setBoolean("APP_START", true);
        mCloudConnectionStatus.stopThread();
        L.saveCCUState();
        AlertManager.getInstance().clearAlertsWhenAppClose();
        try {
            if (mConnectionChangeReceiver != null) {
                this.unregisterReceiver(mConnectionChangeReceiver);
            }
        } catch (Exception e) {
            // already unregistered
        }
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
                    mViewPager.setAdapter(mStatusPagerAdapter);
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
        CcuLog.d("RemoteCommand","PUBNUB RenatusLandingActivity="+commands+","+cmdLevel);
        if (!commands.isEmpty() && commands.equals(RESTART_CCU)) {
            RenatusApp.closeApp();
        } else if (!commands.isEmpty() && commands.equals(RESTART_TABLET)) {
            RenatusApp.rebootTablet();
        } else if (!commands.isEmpty() && commands.equals(SAVE_CCU_LOGS)) {
            new Thread() {
                @Override
                public void run() {
                    UploadLogs.instanceOf().saveCcuLogs();
                }
            }.start();
        } else if (!commands.isEmpty() && commands.equals(RESET_CM)) {
            CcuToCmOverUsbCmResetMessage_t msg = new CcuToCmOverUsbCmResetMessage_t();
            msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_CM_RESET);
            msg.reset.set((short)1);
            MeshUtil.sendStructToCM(msg);
            LSerial.getInstance().setResetSeedMessage(true);
        } else if (!commands.isEmpty() && commands.equals(UPDATE_CCU)) {
            String apkName = id;
            Log.d("CCU_DOWNLOAD", "got command to install update--"+ DownloadManager.EXTRA_DOWNLOAD_ID +","+apkName);
            RenatusApp.getAppContext().registerReceiver(new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                        Log.d("CCU_DOWNLOAD", String.format("Received download complete for %d from %d and %d", downloadId, AppInstaller.getHandle().getCCUAppDownloadId(), AppInstaller.getHandle().getDownloadedFileVersion(downloadId)));
                        if (downloadId == AppInstaller.getHandle().getCCUAppDownloadId()) {
                            if (AppInstaller.getHandle().getDownloadedFileVersion(downloadId) > 0)
                                AppInstaller.getHandle().install(null, false, true, true);
                        }else if(downloadId == AppInstaller.getHandle().getHomeAppDownloadId()){
                            int homeAppVersion = AppInstaller.getHandle().getDownloadedFileVersion(downloadId);
                            if(homeAppVersion >= 1) {
                                PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit().putInt("home_app_version", homeAppVersion).commit();
                                AppInstaller.getHandle().install(null, true, false, true);
                            }
                        }
                    }
                }

            }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            if(apkName.startsWith("75f") || apkName.startsWith("75F"))
                AppInstaller.getHandle().downloadHomeInstall(apkName);
            else if(apkName.startsWith("RENATUS_CCU") || apkName.startsWith("DAIKIN_CCU"))
                AppInstaller.getHandle().downloadCCUInstall(apkName);
        } else if (!commands.isEmpty() && commands.equals(RESTART_MODULE)) {

            //TODO Send commands to SmartNode
            switch (cmdLevel){
                case "system":
                    for (Floor floor : HSUtil.getFloors()) {
                        for (Zone zone : HSUtil.getZones(floor.getId())) {
                            for(Device d : HSUtil.getDevices(zone.getId())) {
                                if(d.getMarkers().contains("smartstat")) {
                                    CcuToCmOverUsbSmartStatControlsMessage_t ssControlsMessage_t = new CcuToCmOverUsbSmartStatControlsMessage_t();
                                    ssControlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SMART_STAT_CONTROLS);
                                    ssControlsMessage_t.address.set(Short.parseShort(d.getAddr()));
                                    ssControlsMessage_t.controls.reset.set((short) 1);
                                    MeshUtil.sendStructToNodes(ssControlsMessage_t);
                                }else if(d.getMarkers().contains("smartnode")){
                                    CcuToCmOverUsbSnControlsMessage_t snControlsMessage_t = new CcuToCmOverUsbSnControlsMessage_t();
                                    snControlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
                                    snControlsMessage_t.smartNodeAddress.set(Short.parseShort(d.getAddr()));
                                    snControlsMessage_t.controls.reset.set((short) 1);
                                    MeshUtil.sendStructToNodes(snControlsMessage_t);
                                }
                            }
                        }
                    }
                    break;
                case "zone":
                    for(Device d : HSUtil.getDevices("@"+id)) {
                        if(d.getMarkers().contains("smartstat")) {
                            CcuToCmOverUsbSmartStatControlsMessage_t ssControlsMessage_t = new CcuToCmOverUsbSmartStatControlsMessage_t();
                            ssControlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SMART_STAT_CONTROLS);
                            ssControlsMessage_t.address.set(Short.parseShort(d.getAddr()));
                            ssControlsMessage_t.controls.reset.set((short) 1);
                            MeshUtil.sendStructToNodes(ssControlsMessage_t);
                        }else if(d.getMarkers().contains("smartnode")) {
                            CcuToCmOverUsbSnControlsMessage_t snControlsMessage_t = new CcuToCmOverUsbSnControlsMessage_t();
                            snControlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
                            snControlsMessage_t.smartNodeAddress.set(Short.parseShort(d.getAddr()));
                            snControlsMessage_t.controls.reset.set((short) 1);
                            MeshUtil.sendStructToNodes(snControlsMessage_t);
                        }
                    }
                    break;
                case "module":
                    Equip equip = HSUtil.getEquipInfo("@"+id);
                    if(equip.getMarkers().contains("smartstat")) {
                        CcuToCmOverUsbSmartStatControlsMessage_t ssControlsMessage_t = new CcuToCmOverUsbSmartStatControlsMessage_t();
                        ssControlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SMART_STAT_CONTROLS);
                        ssControlsMessage_t.address.set(Short.parseShort(equip.getGroup()));
                        ssControlsMessage_t.controls.reset.set((short) 1);
                        MeshUtil.sendStructToNodes(ssControlsMessage_t);
                    }else if(equip.getMarkers().contains("smartnode")){
                        CcuToCmOverUsbSnControlsMessage_t snControlsMessage_t = new CcuToCmOverUsbSnControlsMessage_t();
                        snControlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
                        snControlsMessage_t.smartNodeAddress.set(Short.parseShort(equip.getGroup()));
                        snControlsMessage_t.controls.reset.set((short) 1);
                        MeshUtil.sendStructToNodes(snControlsMessage_t);
                    }
                    break;
            }
        }
    }

    private void configLogo(){

        if(BuildConfig.BUILD_TYPE.equals("daikin_prod")||CCUUiUtil.isDaikinThemeEnabled(this)){
            logo_75f.setImageDrawable(getResources().getDrawable(R.drawable.d3));
            powerbylogo.setVisibility(View.VISIBLE);
        }else{
            logo_75f.setImageDrawable(getResources().getDrawable(R.drawable.ic_75f_logo));
            powerbylogo.setVisibility(View.GONE);
        }

    }
}
