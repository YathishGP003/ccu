package a75f.io.renatus.registartion;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import a75f.io.alerts.AlertManager;
import a75f.io.alerts.AlertProcessor;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logic.Globals;
import a75f.io.renatus.DABFullyAHUProfile;
import a75f.io.renatus.DABHybridAhuProfile;
import a75f.io.renatus.DABStagedProfile;
import a75f.io.renatus.DABStagedRtuWithVfdProfile;
import a75f.io.renatus.DefaultSystemProfile;
import a75f.io.renatus.FloorPlanFragment;
import a75f.io.renatus.R;
import a75f.io.renatus.RenatusLandingActivity;
import a75f.io.renatus.SystemFragment;
import a75f.io.renatus.VavAnalogRtuProfile;
import a75f.io.renatus.VavHybridRtuProfile;
import a75f.io.renatus.VavIERtuProfile;
import a75f.io.renatus.VavStagedRtuProfile;
import a75f.io.renatus.VavStagedRtuWithVfdProfile;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;

public class FreshRegistration extends AppCompatActivity implements VerticalTabAdapter.OnItemClickListener, SwitchFragment {
    VerticalTabAdapter verticalTabAdapter;
    ListView listView_icons;
    ImageView imageView_logo;

    RelativeLayout rl_Header;
    TextView textView_title;
    ImageView imageView_Goback;
    Spinner spinnerSystemProile;
    ImageView imageRefresh;
    ToggleButton toggleWifi;
    Button buttonNext;
    Prefs prefs;
    WifiManager mainWifiObj;

    int[] menu_icons = new int[]{
            R.drawable.ic_goarrow_svg,
            R.drawable.ic_wifi_svg,
            R.drawable.ic_settings_svg,
            R.drawable.ic_options_svg,
            R.drawable.ic_security_svg,
            R.drawable.ic_ticket_alt_solid,
    };

    FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freshregistration);
        container = findViewById(R.id.container);
        listView_icons = findViewById(R.id.listView_icons);
        imageView_logo = findViewById(R.id.imageLogo);
        buttonNext = findViewById(R.id.buttonNext);

        prefs = new Prefs(FreshRegistration.this);

        rl_Header = findViewById(R.id.layoutHeader);
        textView_title = findViewById(R.id.textTitleFragment);
        imageView_Goback = findViewById(R.id.imageGoback);
        spinnerSystemProile = findViewById(R.id.spinnerSystemProfile);
        imageRefresh = (ImageView) findViewById(R.id.imageRefresh);
        toggleWifi = (ToggleButton) findViewById(R.id.toggleWifi);

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

        imageView_Goback.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);

                // TODO Auto-generated method stub
                if (currentFragment instanceof WifiFragment) {
                    selectItem(1);
                }
                if (currentFragment instanceof CreateNewSite) {
                    selectItem(2);
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
                    selectItem(1);
                }
                if (currentFragment instanceof ReplaceCCU) {
                    selectItem(1);
                }
                if (currentFragment instanceof DefaultSystemProfile) {
                    selectItem(5);
                }
                if (currentFragment instanceof VavStagedRtuProfile) {
                    selectItem(5);
                }
                if (currentFragment instanceof VavAnalogRtuProfile) {
                    selectItem(5);
                }
                if (currentFragment instanceof VavStagedRtuWithVfdProfile) {
                    selectItem(5);
                }
                if (currentFragment instanceof VavHybridRtuProfile) {
                    selectItem(5);
                }
                if (currentFragment instanceof DABStagedProfile) {
                    selectItem(5);
                }
                if (currentFragment instanceof DABFullyAHUProfile) {
                    selectItem(5);
                }
                if (currentFragment instanceof DABStagedRtuWithVfdProfile) {
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
            }
        });

        imageRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mainWifiObj.isWifiEnabled()) {
                    animation.setRepeatCount(0);
                    animation.setDuration(1200);
                    imageRefresh.startAnimation(animation);
                    mainWifiObj.startScan();
                }
            }
        });

        imageRefresh.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(v.getContext(), "Skip Wifi", Toast.LENGTH_LONG).show();
                Log.e("WIFI", "Skip Wifi");
                selectItem(3);
                return false;
            }
        });

        buttonNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
                if (currentFragment instanceof FloorPlanFragment) {
                    selectItem(20);
                }
                if (currentFragment instanceof SystemFragment) {
                    selectItem(21);
                }
                if (currentFragment instanceof WifiFragment) {
                    String INSTALL_TYPE = prefs.getString("INSTALL_TYPE");
                    ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (networkInfo.isConnected()) {
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
                    updateCCURegistrationInfo();
                }
            }
        });

        mainWifiObj = (WifiManager)

                getApplicationContext().

                        getSystemService(Context.WIFI_SERVICE);

        toggleWifi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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

        selectItem(position);
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

    public void setToggleWifi(boolean status) {
        toggleWifi.setChecked(status);
        if (status) {
            buttonNext.setVisibility(View.VISIBLE);
            imageRefresh.setImageResource(R.drawable.ic_refresh);
        } else {
            buttonNext.setVisibility(View.GONE);
            imageRefresh.setImageResource(R.drawable.ic_refresh_disable);
        }
    }

    public void Switch(int position) {
        selectItem(position);
    }

    @Override
    public void selectItem(int position) {
        Fragment fragment = null;
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (position == 0) {
            showIcons(false);
        } else {
            showIcons(true);
        }

        Log.i("Tab", "Position:" + position);

        if (position == 0) {
            fragment = new StartCCUFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
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
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
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
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
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
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
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
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
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
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
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
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
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
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
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
            paramsPager.topMargin = 305;
            paramsPager.leftMargin = 420;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 92;
            container.setLayoutParams(paramsPager);
        }
        if (position == 8) {

            fragment = new ReplaceCCU();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
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
        if (position == 9) {


            fragment = new DefaultSystemProfile();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
                    .commit();

            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);
            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
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

            fragment = new VavStagedRtuProfile();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
                    .commit();


            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);
            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
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


            fragment = new VavAnalogRtuProfile();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
                    .commit();

            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);

            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
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

            fragment = new VavStagedRtuWithVfdProfile();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
                    .commit();


            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
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
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
                    .commit();


            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
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
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
                    .commit();


            verticalTabAdapter.setCurrentSelected(5);
            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);
            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
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

            fragment = new DABFullyAHUProfile();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
                    .commit();

            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);
            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
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

            fragment = new DABStagedRtuWithVfdProfile();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
                    .commit();
            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);

            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
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
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
                    .commit();

            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);

            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
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
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
                    .commit();

            ConstraintLayout.LayoutParams headerparams = (ConstraintLayout.LayoutParams) rl_Header.getLayoutParams();
            headerparams.topMargin = 70;
            headerparams.leftMargin = 0;
            rl_Header.setLayoutParams(headerparams);

            verticalTabAdapter.setCurrentSelected(5);

            textView_title.setText(getText(R.string.title_systemprofile));
            textView_title.setVisibility(View.VISIBLE);
            spinnerSystemProile.setVisibility(View.VISIBLE);
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
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
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
        }
        if (position == 20) {


            fragment = new SystemFragment();

            Bundle data = new Bundle();
            data.putBoolean("REGISTRATION_WIZARD", true);
            fragment.setArguments(data);

            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
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
        }
        if (position == 21) {

            fragment = new CongratsFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK)
                    .commit();

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
            paramsPager.topMargin = 92;
            paramsPager.leftMargin = 116;
            paramsPager.bottomMargin = 24;
            paramsPager.rightMargin = 101;
            container.setLayoutParams(paramsPager);
        }
    }

    private void updateCCURegistrationInfo() {
        ProgressDialogUtils.showProgressDialog(this,"CCU Registering...");
        String installerEmail = prefs.getString("installerEmail");

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                ProgressDialogUtils.hideProgressDialog();
                if (pingCloudServer()){

                    CCUHsApi.getInstance().registerCcu(installerEmail);

                    AlertManager.getInstance().fetchAllPredefinedAlerts();
                    Intent i = new Intent(FreshRegistration.this, RenatusLandingActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();

                } else {
                    new AlertDialog.Builder(FreshRegistration.this)
                            .setCancelable(false)
                            .setMessage("No network connection, Registration is not complete and Facilisight cannot be accessed unless you connect to network.")
                            .setPositiveButton("Proceed", (dialog, id) -> {

                                CCUHsApi.getInstance().registerCcu(installerEmail);
                                AlertManager.getInstance().fetchAllPredefinedAlerts();
                                Intent i = new Intent(FreshRegistration.this, RenatusLandingActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                                finish();
                            })
                            .show();
                }
            }
        }, 1000);
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
}
