package a75f.io.renatus.registration;

import static java.lang.Thread.sleep;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.projecthaystack.client.HClient;


import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.sync.CareTakerResponse;

import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.Zone;

import a75f.io.logger.CcuLog;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.ccu.restore.RestoreCCU;
import a75f.io.logic.preconfig.PreconfigurationManager;
import a75f.io.logic.preconfig.PreconfigurationState;
import a75f.io.logic.util.onLoadingCompleteListener;
import a75f.io.messaging.client.MessagingClient;
import a75f.io.renatus.DABHybridAhuProfile;
import a75f.io.renatus.DABStagedProfile;
import a75f.io.renatus.DefaultSystemProfile;
import a75f.io.renatus.FloorPlanFragment;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusLandingActivity;
import a75f.io.renatus.SystemFragment;
import a75f.io.renatus.UtilityApplication;
import a75f.io.renatus.VavHybridRtuProfile;
import a75f.io.renatus.VavIERtuProfile;
//import a75f.io.renatus.VavStagedRtuProfile;
import a75f.io.renatus.profiles.system.DabModulatingRtuFragment;
import a75f.io.renatus.profiles.system.DabStagedRtuFragment;
import a75f.io.renatus.profiles.system.DabStagedVfdRtuFragment;
import a75f.io.renatus.profiles.system.VavModulatingRtuFragment;
import a75f.io.renatus.profiles.system.VavStagedRtuFragment;
import a75f.io.renatus.profiles.system.VavStagedVfdRtuFragment;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.PreferenceConstants;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.views.CustomCCUSwitch;
import a75f.io.util.ExecutorTask;

public class FreshRegistration extends AppCompatActivity implements VerticalTabAdapter.OnItemClickListener, SwitchFragment {
    VerticalTabAdapter verticalTabAdapter;
    ListView listView_icons;
    ImageView imageView_logo;

    RelativeLayout rl_Header;
    TextView textView_title;
    ImageView imageView_Goback;
    Spinner spinnerSystemProile;
    ImageView imageRefresh;
    CustomCCUSwitch toggleWifi;
    Button buttonNext;
    Prefs prefs;
    WifiManager mainWifiObj;
    TextView ethernetStatus;
    EthernetNetworkChangeReceiver mEthernetNetworkReceiver;
    int selectedPosition = 0;

    int[] menu_icons = new int[]{
            R.drawable.ic_goarrow_svg,
            R.drawable.ic_wifi_svg,
            R.drawable.ic_settings_svg,
    };

    FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        CCUUiUtil.setThemeDetails(this);
        setContentView(R.layout.activity_freshregistration);
        container = findViewById(R.id.container);
        listView_icons = findViewById(R.id.listView_icons);
        imageView_logo = findViewById(R.id.imageLogo);
        buttonNext = findViewById(R.id.buttonNext);
        configLogo();
        prefs = new Prefs(FreshRegistration.this);

        rl_Header = findViewById(R.id.layoutHeader);
        textView_title = findViewById(R.id.textTitleFragment);
        imageView_Goback = findViewById(R.id.imageGoback);
        spinnerSystemProile = findViewById(R.id.spinnerSystemProfile);

        imageRefresh = findViewById(R.id.imageRefresh);
        toggleWifi = findViewById(R.id.toggleWifi);
        ethernetStatus = findViewById(R.id.ethernetConnectionStatus);
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mEthernetNetworkReceiver = new EthernetNetworkChangeReceiver();
        registerReceiver(mEthernetNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        showIcons(false);
        verticalTabAdapter = new VerticalTabAdapter(this, menu_icons, listView_icons, this, 0);
        listView_icons.setAdapter(verticalTabAdapter);
        rl_Header.setVisibility(View.GONE);

        int position = 0;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            position = extras.getInt("viewpager_position");
        }


        Animation animation = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(1200);

        imageView_Goback.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
            // TODO Auto-generated method stub
            if (currentFragment instanceof WifiFragment) {
                selectItem(1);
            }
            if (currentFragment instanceof CreateNewSite) {
                selectItem(2);
            }

            if (currentFragment instanceof InstallTypeFragment) {
                selectItem(0);
            }
            if (currentFragment instanceof InstallerOptions) {
               String installType = prefs.getString("INSTALL_TYPE");
                if (installType.equals("OFFLINE")) {
                    selectItem(1);
                } else {
                    selectItem(3);
                }
            }
            if (currentFragment instanceof Security) {
                selectItem(4);
            }
            if (currentFragment instanceof AddtoExisting) {
                selectItem(1);
            }
            if (currentFragment instanceof PreConfigCCU) {
                PreconfigurationManager.INSTANCE.transitionTo(PreconfigurationState.NotConfigured.INSTANCE);
                selectItem(1);
            }
            if (currentFragment instanceof ReplaceCCU) {
                selectItem(1);
            }
            if (currentFragment instanceof RegisterCCUToExistingSite) {
                selectItem(6);
            }
            if (currentFragment instanceof DefaultSystemProfile) {
                selectItem(5);
            }
            if (currentFragment instanceof VavStagedRtuFragment) {
                selectItem(5);
            }
            if (currentFragment instanceof VavModulatingRtuFragment) {
                selectItem(5);
            }
            if (currentFragment instanceof VavStagedVfdRtuFragment) {
                selectItem(5);
            }
            if (currentFragment instanceof VavHybridRtuProfile) {
                selectItem(5);
            }
            if (currentFragment instanceof DabStagedRtuFragment) {
                selectItem(5);
            }
            if (currentFragment instanceof DabModulatingRtuFragment) {
                selectItem(5);
            }
            if (currentFragment instanceof DabStagedVfdRtuFragment) {
                selectItem(5);
            }
            if (currentFragment instanceof DABHybridAhuProfile) {
                selectItem(5);
            }
            if (currentFragment instanceof VavIERtuProfile) {
                selectItem(5);
            }
            if (currentFragment instanceof FloorPlanFragment) {
                String profileType = prefs.getString("PROFILE");

                if (profileType.equals("DEFAULT")) {
                    spinnerSystemProile.setSelection(0);
                    selectItem(9);
                }
                if (profileType.equals("VAV_STAGED_RTU")) {
                    spinnerSystemProile.setSelection(1);
                    selectItem(10);
                }
                if (profileType.equals("VAV_FULLY_MODULATING")) {
                    spinnerSystemProile.setSelection(2);
                    selectItem(11);
                }
                if (profileType.equals("VAV_STAGED_RTU_VFD")) {
                    spinnerSystemProile.setSelection(3);
                    selectItem(12);
                }
                if (profileType.equals("VAV_HYBRID_RTU")) {
                    spinnerSystemProile.setSelection(4);
                    selectItem(13);
                }
                if (profileType.equals("DAB_STAGED_RTU")) {
                    spinnerSystemProile.setSelection(5);
                    selectItem(14);
                }
                if (profileType.equals("DAB_FULLY_MODULATING")) {
                    spinnerSystemProile.setSelection(6);
                    selectItem(15);
                }
                if (profileType.equals("DAB_STAGED_RTU_VFD")) {
                    spinnerSystemProile.setSelection(7);
                    selectItem(16);
                }
                if (profileType.equals("DAB_HYBRID_RTU")) {
                    spinnerSystemProile.setSelection(8);
                    selectItem(17);
                }
                if (profileType.equals("VAV_IE_RTU")) {
                    spinnerSystemProile.setSelection(8);
                    selectItem(18);
                }
            }

            if (currentFragment instanceof SystemFragment) {
                selectItem(19);
            }
        });

        imageRefresh.setOnClickListener(v -> {
            // TODO Auto-generated method stub
            if (mainWifiObj.isWifiEnabled()) {
                animation.setRepeatCount(0);
                animation.setDuration(1200);
                imageRefresh.startAnimation(animation);
                mainWifiObj.startScan();
            }
        });

        imageRefresh.setOnLongClickListener(v -> {
            Toast.makeText(v.getContext(), "Skip Wifi", Toast.LENGTH_LONG).show();
            CcuLog.e(L.TAG_CCU_WIFI, "Skip Wifi");
            selectItem(3);
            return false;
        });

        buttonNext.setOnClickListener(v -> {
            // TODO Auto-generated method stub
            buttonNext.setEnabled(false);
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
            if (currentFragment instanceof FloorPlanFragment) {
                selectItem(20);
                buttonNext.setEnabled(true);
            }
            if (currentFragment instanceof SystemFragment) {
                selectItem(21);
                buttonNext.setEnabled(true);
            }
            if (currentFragment instanceof WifiFragment) {
                buttonNext.setEnabled(true);
                String INSTALL_TYPE = prefs.getString("INSTALL_TYPE");
                NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                NetworkInfo networkInfoForEthernet = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
                if (networkInfo.isConnected() || networkInfoForEthernet.isConnected()) {
                    switch (INSTALL_TYPE) {
                        case "CREATENEW":
                            selectItem(3);
                            break;
                        case "ADDCCU":
                            selectItem(6);
                            break;
                        case "PRECONFIGCCU":
                            selectItem(7);
                            break;
                        case "REPLACECCU":
                            selectItem(8);
                            break;
                        default:
                            Toast.makeText(FreshRegistration.this, "Please connect to internet to continue!", Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            }
            if (currentFragment instanceof CongratsFragment) {
                prefs.setBoolean("REGISTRATION", true);
                ExecutorTask.executeBackground(() -> Globals.getInstance().copyModels());
                if (prefs.getString("INSTALL_TYPE").equals("PRECONFIGCCU")) {
                    registerSite(currentFragment.getView());
                } else {
                    registerCCU();
                }
                buttonNext.setEnabled(true);
            }
        });

        mainWifiObj = (WifiManager)

                getApplicationContext().

                        getSystemService(Context.WIFI_SERVICE);

        toggleWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mainWifiObj = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (isChecked) {
                if (!mainWifiObj.isWifiEnabled()) {
                    animation.setDuration(2000);
                    imageRefresh.startAnimation(animation);
                    mainWifiObj = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    mainWifiObj.setWifiEnabled(true);
                    imageRefresh.setEnabled(true);
                    imageRefresh.setImageResource(R.drawable.ic_refresh);
                }
            } else {
                if (mainWifiObj.isWifiEnabled()) {
                    mainWifiObj = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    mainWifiObj.setWifiEnabled(false);
                    imageRefresh.setEnabled(false);
                    imageRefresh.setImageResource(R.drawable.ic_refresh_disable);
                }
            }
        });

        spinnerSystemProile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id) {
                if (position == 0) {
                    selectItem(9);
                }
                if (position == 1) {
                    selectItem(10);
                }
                if (position == 2) {
                    selectItem(11);
                }
                if (position == 3) {
                    selectItem(12);
                }
                if (position == 4) {
                    selectItem(13);
                }
                if (position == 5) {
                    selectItem(14);
                }
                if (position == 6) {
                    selectItem(15);
                }
                if (position == 7) {
                    selectItem(16);
                }
                if (position == 8) {
                    selectItem(17);
                }
                if (position == 9) {
                    selectItem(18);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }

        });
        CCUUiUtil.setSpinnerDropDownColor(spinnerSystemProile,FreshRegistration.this);
        selectItem(position);

        if(RestoreCCU.isReplaceCCUUnderProcess()){
            loadReplaceCCUFragment(getSupportFragmentManager());
        }
    }

    public void showIcons(boolean showIcons) {
        if (!showIcons) {
            listView_icons.setVisibility(View.INVISIBLE);
            imageView_logo.setVisibility(View.INVISIBLE);
            rl_Header.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
        } else {
            buttonNext.setVisibility(View.GONE);
            listView_icons.setVisibility(View.VISIBLE);
            imageView_logo.setVisibility(View.VISIBLE);
            rl_Header.setVisibility(View.VISIBLE);
        }
    }

    public void setToggleWifi(boolean status, boolean ethernetStatus) {
        toggleWifi.setChecked(status);
        if (status) {
            buttonNext.setVisibility(View.VISIBLE);
            imageRefresh.setImageResource(R.drawable.ic_refresh);
        } else {
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setImageResource(R.drawable.ic_refresh_disable);
        }
        if (ethernetStatus) {
            buttonNext.setVisibility(View.VISIBLE);
        }
    }

    public void Switch(int position) {
        selectItem(position);
    }

    @Override
    public void selectItem(int position) {
        Fragment fragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        selectedPosition = position;
        showIcons(position != 0);

        CcuLog.i(L.TAG_CCU_UI, "Tab Position:" + position);

        if (position == 0) {
            fragment = new StartCCUFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();

            textView_title.setVisibility(View.GONE);
            spinnerSystemProile.setVisibility(View.GONE);
            imageView_Goback.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            toggleWifi.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);

        }

        if (position == 1) {
            fragment = new InstallTypeFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();

            verticalTabAdapter.setCurrentSelected(0);

            textView_title.setVisibility(View.VISIBLE);
            textView_title.setText(getText(R.string.installtype));
            spinnerSystemProile.setVisibility(View.GONE);
            imageView_Goback.setVisibility(View.GONE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 178, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 136;
            paramsPager.leftMargin = 280;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 0;
            container.setLayoutParams(paramsPager);

        }
        if (position == 2) {

            fragment = new WifiFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();

            verticalTabAdapter.setCurrentSelected(1);
            textView_title.setText(getText(R.string.connectwifi));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.GONE);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.VISIBLE);
            buttonNext.setVisibility(View.VISIBLE);
            imageRefresh.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 333, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 130;
            paramsPager.leftMargin = 400;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 0;
            container.setLayoutParams(paramsPager);
        }
        if (position == 3) {

            fragment = new CreateNewSite();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();

            verticalTabAdapter.setCurrentSelected(2);

            textView_title.setText(getText(R.string.installcreatenew));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.GONE);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 312, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 142;
            paramsPager.leftMargin = 400;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 0;
            container.setLayoutParams(paramsPager);
        }
        if (position == 4) {
            fragment = new InstallerOptions();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();

            verticalTabAdapter.setCurrentSelected(3);

            textView_title.setText(getText(R.string.title_installeroption));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.GONE);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 603, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 126;
            paramsPager.leftMargin = 350;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 0;
            container.setLayoutParams(paramsPager);
        }
        if (position == 5) {

            fragment = new Security();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();


            verticalTabAdapter.setCurrentSelected(4);

            textView_title.setText(getText(R.string.title_security));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.GONE);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 776, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 106;
            paramsPager.leftMargin = 359;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 0;
            container.setLayoutParams(paramsPager);
        }
        if (position == 6) {

            fragment = new AddtoExisting();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();


            verticalTabAdapter.setCurrentSelected(2);

            textView_title.setText(getText(R.string.installaddccu));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.GONE);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 260, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 100;
            paramsPager.leftMargin = 420;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 0;
            container.setLayoutParams(paramsPager);
        }
        if (position == 7) {

            fragment = new PreConfigCCU();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();

            verticalTabAdapter.setCurrentSelected(2);

            textView_title.setText(getText(R.string.title_preconfig));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.GONE);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 260, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 150;
            paramsPager.leftMargin = 420;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 92;
            container.setLayoutParams(paramsPager);
        }
        if (position == 8) {
            loadReplaceCCUFragment(fragmentManager);
        }
        if (position == 9) {
            fragment = new DefaultSystemProfile();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();

            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);
            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
            spinnerSystemProile.setSelection(0);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 560, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 156;
            paramsPager.leftMargin = 250;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 82;
            container.setLayoutParams(paramsPager);
        }
        if (position == 10) {

            fragment = new VavStagedRtuFragment(onLoadingCompleteListener.INSTANCE);

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();


            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);
            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
            spinnerSystemProile.setSelection(1);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 560, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 146;
            paramsPager.leftMargin = 250;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 26;
            container.setLayoutParams(paramsPager);
        }
        if (position == 11) {


            fragment = new VavModulatingRtuFragment(onLoadingCompleteListener.INSTANCE);

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();

            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);

            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
            spinnerSystemProile.setSelection(2);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 560, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 146;
            paramsPager.leftMargin = 250;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 26;
            container.setLayoutParams(paramsPager);
        }
        if (position == 12) {
            fragment = new VavStagedVfdRtuFragment(onLoadingCompleteListener.INSTANCE);

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();


            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
            spinnerSystemProile.setSelection(3);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 560, 0);
            textView_title.setLayoutParams(params);
            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);
            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 146;
            paramsPager.leftMargin = 250;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 26;
            container.setLayoutParams(paramsPager);
        }
        if (position == 13) {

            fragment = new VavHybridRtuProfile();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();


            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
            spinnerSystemProile.setSelection(4);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 560, 0);
            textView_title.setLayoutParams(params);
            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);
            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 146;
            paramsPager.leftMargin = 250;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 26;
            container.setLayoutParams(paramsPager);
        }
        if (position == 14) {

            fragment = new DABStagedProfile();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();


            verticalTabAdapter.setCurrentSelected(5);
            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);
            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
            spinnerSystemProile.setSelection(5);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 560, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 146;
            paramsPager.leftMargin = 250;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 26;
            container.setLayoutParams(paramsPager);
        }
        if (position == 15) {

            fragment = new DabModulatingRtuFragment();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();

            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);
            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
            spinnerSystemProile.setSelection(6);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 560, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 146;
            paramsPager.leftMargin = 250;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 26;
            container.setLayoutParams(paramsPager);
        }
        if (position == 16) {

            fragment = new DabStagedVfdRtuFragment();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);

            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
            spinnerSystemProile.setSelection(7);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 560, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 146;
            paramsPager.leftMargin = 250;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 26;
            container.setLayoutParams(paramsPager);
        }
        if (position == 17) {

            fragment = new DABHybridAhuProfile();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();

            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);

            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
            spinnerSystemProile.setSelection(8);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 560, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 146;
            paramsPager.leftMargin = 250;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 26;
            container.setLayoutParams(paramsPager);
        }
        if (position == 18) {

            fragment = new VavIERtuProfile();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();

            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);

            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
            spinnerSystemProile.setSelection(9);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 560, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 146;
            paramsPager.leftMargin = 250;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 26;
            container.setLayoutParams(paramsPager);
        }
        if (position == 19) {

            fragment = new FloorPlanFragment();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();


            showIcons(false);
            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 26;
            headerparams.leftMargin = -40;
            rl_Header.setLayoutParams(headerparams);

            rl_Header.setVisibility(View.VISIBLE);
            buttonNext.setVisibility(View.VISIBLE);
            textView_title.setVisibility(View.GONE);
            spinnerSystemProile.setVisibility(View.GONE);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 560, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 10;
            paramsPager.leftMargin = 56;
            paramsPager.bottomMargin = 0;
            paramsPager.rightMargin = 0;
            container.setLayoutParams(paramsPager);
            L.saveCCUStateAsync();
        }
        if (position == 20) {


            fragment = new SystemFragment();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();


            showIcons(false);

            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 26;
            headerparams.leftMargin = -40;
            rl_Header.setLayoutParams(headerparams);

            rl_Header.setVisibility(View.VISIBLE);
            buttonNext.setVisibility(View.VISIBLE);
            textView_title.setVisibility(View.GONE);
            imageView_Goback.setVisibility(View.GONE);
            spinnerSystemProile.setVisibility(View.GONE);
            toggleWifi.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 560, 0);
            textView_title.setLayoutParams(params);

            TypedValue tv = new TypedValue();
            int actionBarHeight = 0;
            if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            }

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = actionBarHeight;
            paramsPager.leftMargin = 112;
            paramsPager.bottomMargin = 0;
            paramsPager.rightMargin = 0;
            container.setLayoutParams(paramsPager);
            L.saveCCUStateAsync();
        }
        if (position == 21) {
            CcuLog.i("CCU_PRECONFIGURATION", "CongratsFragment");
            fragment = new CongratsFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commitAllowingStateLoss();

            showIcons(false);
            rl_Header.setVisibility(View.VISIBLE);
            textView_title.setText(getText(R.string.title_congrats));
            buttonNext.setVisibility(View.VISIBLE);
            buttonNext.setText(getText(R.string.button_finish));
            buttonNext.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.GONE);
            imageView_Goback.setVisibility(View.GONE);
            toggleWifi.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 465, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 102;
            paramsPager.leftMargin = 116;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 101;
            container.setLayoutParams(paramsPager);
        }
        if (position == 22) {

            Intent intent = new Intent(FreshRegistration.this, RenatusLandingActivity.class);
            startActivity(intent);
        }
        if (position == 23) {

            fragment = new RegisterCCUToExistingSite();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commitAllowingStateLoss();

            verticalTabAdapter.setCurrentSelected(2);
            textView_title.setText(getText(R.string.add_new_ccu));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.GONE);
            imageView_Goback.setVisibility(View.VISIBLE);
            toggleWifi.setVisibility(View.GONE);
            imageRefresh.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(0, 0, 92, 0);
            textView_title.setLayoutParams(params);

            ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            paramsPager.topMargin = 130;
            paramsPager.leftMargin = 400;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 0;
            container.setLayoutParams(paramsPager);
        }
        if(position == 0) {
            ethernetStatus.setText("Internet connection is active via Ethernet.");
        }
        else {
            ethernetStatus.setText("Connected to the internet via Ethernet.");
        }
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfoForEthernet = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

        if (position >= 0 && position <= 2 && networkInfoForEthernet.isConnected()) {
            ethernetStatus.setVisibility(View.VISIBLE);
            if(!hasInternetConnection(this)) {
                ethernetStatus.setText("No internet access via Ethernet, please check your connection.");
            }
        } else {
            ethernetStatus.setVisibility(View.INVISIBLE);
        }
    }

    private void loadReplaceCCUFragment(FragmentManager fragmentManager) {
        Fragment fragment = new ReplaceCCU();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();


        verticalTabAdapter.setCurrentSelected(2);

        textView_title.setText(getText(R.string.installreplaceccu));
        textView_title.setVisibility(View.VISIBLE);
        spinnerSystemProile.setVisibility(View.GONE);
        imageView_Goback.setVisibility(View.VISIBLE);
        toggleWifi.setVisibility(View.GONE);
        buttonNext.setVisibility(View.GONE);
        imageRefresh.setVisibility(View.GONE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.setMargins(0, 0, 240, 0);
        textView_title.setLayoutParams(params);

        ConstraintLayout.LayoutParams paramsPager = (ConstraintLayout.LayoutParams) container.getLayoutParams();
        paramsPager.topMargin = 156;
        paramsPager.leftMargin = 420;
        paramsPager.bottomMargin = 24;
        paramsPager.rightMargin = 90;
        container.setLayoutParams(paramsPager);
    }

    private void registerSite(View currentView) {
        ProgressDialogUtils.showProgressDialog(this,"CCU Registering...");
        buttonNext.setEnabled(false);

        ExecutorTask.executeBackground(() -> {
            prefs.setBoolean(PreferenceConstants.CCU_SETUP, true);
            prefs.setBoolean(PreferenceConstants.PROFILE_SETUP, true);
        });
        //This is a hack to give bit more time to complete Site-registration before we start
        //CCU registration
        if (!CCUHsApi.getInstance().siteSynced()) {
            if (pingCloudServer()){
                ExecutorTask.executeBackground(() -> {
                    try {
                        CareTakerResponse siteResponse = CCUHsApi.getInstance().registerSite();

                        if (siteResponse == null) {
                            SnackbarUtil.showInfoMessage(currentView, "Error registering site. Please try again");
                            CcuLog.w(L.TAG_REGISTRATION, "Error registering site. NULL response.");
                            return;
                        }
                        CcuLog.i(L.TAG_REGISTRATION, "Site registration response: " + siteResponse);
                        if (siteResponse.getResponseCode() == 200) {
                            CcuLog.i(L.TAG_REGISTRATION, "Site registered successfully.");
                            Site siteObject = CCUHsApi.getInstance().getSite();
                            CCUHsApi.getInstance().importNamedSchedulebySite(new HClient(CCUHsApi.getInstance().getHSUrl(),
                                    HayStackConstants.USER, HayStackConstants.PASS), siteObject);

                            registerCcuInBackground();

                            //Delaying the schedule download a bit to make sure it is created.
                            sleep(1000);
                            updateDefaultSchedule(CCUHsApi.getInstance());
                            launchLandingActivity();

                        } else if (siteResponse.getResponseCode() >= 400) {
                            if (siteResponse.getErrorResponse() != null && siteResponse.getErrorResponse().getMessage() != null) {
                                CcuLog.w(L.TAG_REGISTRATION, "Error registering site." + siteResponse.getErrorResponse().getMessage());
                                SnackbarUtil.showConfirmationMessage(currentView,
                                        "Registration Failed !!",
                                        getRegistrationErrorMessage(siteResponse),
                                        this::launchLandingActivity);
                            } else {
                                SnackbarUtil.showConfirmationMessage(currentView,
                                        "Registration Failed !!",
                                        getRegistrationErrorMessage(siteResponse),
                                        this::launchLandingActivity);
                                CcuLog.w(L.TAG_REGISTRATION, "Error registering site." + siteResponse);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        CcuLog.w("CCU_HS", "Unexpected error registering site.", e);
                        SnackbarUtil.showConfirmationMessage(currentView,
                                "Registration Failed !!",
                                getRegistrationErrorMessage(null),
                                this::launchLandingActivity);
                    }

                    runOnUiThread(ProgressDialogUtils::hideProgressDialog);

                });
            } else {
                ProgressDialogUtils.hideProgressDialog();
                new AlertDialog.Builder(FreshRegistration.this)
                        .setCancelable(false)
                        .setMessage("No network connection, Registration is not complete and Facilisight cannot be accessed unless you connect to network.")
                        .setPositiveButton("Proceed", (dialog, id) -> {
                            registerCcuInBackground();
                           launchLandingActivity();
                        })
                        .show();
            }


        } else {
            CcuLog.i(L.TAG_REGISTRATION, "Site already registered.");
        }
    }

    private String getRegistrationErrorMessage(CareTakerResponse siteResponse) {
        if (siteResponse.getErrorResponse() != null && siteResponse.getErrorResponse().getMessage() != null) {
            return siteResponse.getErrorResponse().getMessage();
        } else {
            return "Error registering site. " +
                    "Update details from Settings screen and register again";
        }
    }

    private void launchLandingActivity() {
        Intent i = new Intent(FreshRegistration.this, RenatusLandingActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();

    }

    private void registerCCU() {
        ProgressDialogUtils.showProgressDialog(this,"CCU Registering...");
        buttonNext.setEnabled(false);

        CcuLog.i(L.TAG_REGISTRATION, "registerCCU");
        prefs.setBoolean(PreferenceConstants.CCU_SETUP, true);
        prefs.setBoolean(PreferenceConstants.PROFILE_SETUP, true);
        boolean isNetworkAvailable = pingCloudServer();
        if (isNetworkAvailable){
            CcuLog.i(L.TAG_REGISTRATION, "send registration request to server");
            registerCcuInBackground();
            ExecutorTask.executeBackground(() -> {
                try {
                    //Delaying the schedule download a bit to make sure it is created.
                    sleep(1000);
                    updateDefaultSchedule(CCUHsApi.getInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                    CcuLog.e(L.TAG_REGISTRATION, "Unexpected error updating default schedule. : "+e.getMessage());
                }
            });
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                ProgressDialogUtils.hideProgressDialog();
                launchLandingActivity();
            }, 2000);
        } else {
            CcuLog.e(L.TAG_REGISTRATION, "No network connection or server not reachable, Registration is not complete !!");
            new AlertDialog.Builder(FreshRegistration.this)
                    .setCancelable(false)
                    .setMessage("No network connection, Registration is not complete and Facilisight cannot be accessed unless you connect to network.")
                    .setPositiveButton("Proceed", (dialog, id) -> {
                        registerCcuInBackground();
                        launchLandingActivity();
                    })
                    .show();
        }
    }

    private void updateCCURegistrationInfo() {
        ProgressDialogUtils.showProgressDialog(this,"CCU Registering...");
        buttonNext.setEnabled(false);

        //This is a hack to give bit more time to complete Site-registration before we start
        //CCU registration
        long delay = CCUHsApi.getInstance().siteSynced() ? 1000 : 30000;
        CcuLog.i(L.TAG_REGISTRATION, "updateCCURegistrationInfo with delay "+delay);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            CcuLog.i(L.TAG_REGISTRATION, "updateCCURegistrationInfo");
            prefs.setBoolean(PreferenceConstants.CCU_SETUP, true);
            prefs.setBoolean(PreferenceConstants.PROFILE_SETUP, true);
            ProgressDialogUtils.hideProgressDialog();
            if (pingCloudServer()){

                registerCcuInBackground();

                ExecutorTask.executeBackground(() -> {
                    try {
                        //Delaying the schedule download a bit to make sure it is created.
                        sleep(1000);
                        updateDefaultSchedule(CCUHsApi.getInstance());
                    } catch (Exception e) {
                        e.printStackTrace();
                        CcuLog.e("CCU_HS", "Unexpected error updating default schedule. : "+e.getMessage());
                    }
                });
                launchLandingActivity();
            } else {
                new AlertDialog.Builder(FreshRegistration.this)
                        .setCancelable(false)
                        .setMessage("No network connection, Registration is not complete and Facilisight cannot be accessed unless you connect to network.")
                        .setPositiveButton("Proceed", (dialog, id) -> {
                            registerCcuInBackground();
                            launchLandingActivity();
                        })
                        .show();
            }
        }, delay);
    }

    private void registerCcuInBackground() {
        String installerEmail = prefs.getString("installerEmail");

        ExecutorTask.executeBackground( () ->  {
            try {
                CcuLog.i(L.TAG_REGISTRATION, "Registering CCU with email: " + installerEmail);
                CCUHsApi.getInstance().registerCcu(installerEmail);
                if (Globals.getInstance().isAckdMessagingEnabled()) {
                    MessagingClient.getInstance().init();
                }
                UtilityApplication.scheduleMessagingAckJob();
                CCUHsApi.getInstance().syncEntityWithPointWriteDelayed(15);
            } catch (Exception e) {
                CcuLog.w(L.TAG_REGISTRATION, "Unexpected error registering CCU.", e);
                runOnUiThread( () -> {
                    // A Toast rather than a dialog is necessary since the interface does not wait
                    // for the response here.  We should fix that when we rewrite Registration.
                    Context context = FreshRegistration.this;
                    if (context != null) {
                        Toast.makeText(context, "Error registering CCU.  Please try again", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private synchronized boolean pingCloudServer() {
        ConnectivityManager connMgr = (ConnectivityManager) Globals.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (netInfo != null && netInfo.isConnected() || (networkInfo != null && networkInfo.isConnected())) {
            //  Some sort of connection is open, check if server is reachable
            SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext());
            spDefaultPrefs.edit().putBoolean("75fNetworkAvailable", true).commit();
            return true;
        }
        else {
            SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext());
            spDefaultPrefs.edit().putBoolean("75fNetworkAvailable", false).commit();
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
         if(currentFragment instanceof  StartCCUFragment)
             super.onBackPressed();
        else
             imageView_Goback.callOnClick();
    }


    private void configLogo(){
        if(CCUUiUtil.isDaikinEnvironment(this))
            imageView_logo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_daikin_logo_colored, null));
        else if (CCUUiUtil.isCarrierThemeEnabled(this)) {
            imageView_logo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.carrier_logo_dark_blue, null));
        }else if (CCUUiUtil.isAiroverseThemeEnabled(this)) {
            imageView_logo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.airoverse_logo, null));
        }else {
            imageView_logo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_logo_svg, null));
            findViewById(R.id.main_layout).setBackgroundResource(R.drawable.bg_logoscreen);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mEthernetNetworkReceiver != null) {
            unregisterReceiver(mEthernetNetworkReceiver);
        }
    }
    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
                Network network = connectivityManager.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities networkCapabilities =
                            connectivityManager.getNetworkCapabilities(network);
                    return networkCapabilities != null &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                }
        }
        return false;
    }

    public class EthernetNetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfoForEthernet = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

            if (networkInfoForEthernet.isConnected()) {
                ethernetStatus.setVisibility(View.VISIBLE);
                if (hasInternetConnection(context)) {
                   if(selectedPosition == 0) {
                       ethernetStatus.setText("Internet connection is active via Ethernet.");
                   }
                   else {
                       ethernetStatus.setText("Connected to the internet via Ethernet.");
                   }
                } else {
                    ethernetStatus.setText("No internet access via Ethernet, please check your connection.");
                }
            } else {
                if( selectedPosition == 2 ) { // Dynamic update in Wifi Fragment
                    buttonNext.setVisibility(View.GONE);
                }
                ethernetStatus.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void updateDefaultSchedule(CCUHsApi hsApi) {
        Site siteObject = hsApi.getSite();
        hsApi.importNamedSchedulebySite(new HClient(CCUHsApi.getInstance().getHSUrl(),
                HayStackConstants.USER, HayStackConstants.PASS), siteObject);

        HashMap<Object, Object> defaultNamedSchedule =  hsApi.readEntity
                ("named and schedule and default and siteRef == "+siteObject.getId());

        if(!defaultNamedSchedule.isEmpty()) {
            List<HashMap<Object, Object>> zones = hsApi.readAllEntities("room");
            zones.forEach(zoneMap -> {
                Zone zone =  new Zone.Builder().setHashMap(zoneMap).build();
                zone.setScheduleRef(defaultNamedSchedule.get("id").toString());
                hsApi.updateZone(zone, zone.getId());
            });
        } else {
            CcuLog.e(L.TAG_CCU_UI, "No default schedule found !!!! ");
        }
    }
}
