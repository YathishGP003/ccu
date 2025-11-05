package a75f.io.renatus;

import static a75f.io.alerts.AlertsConstantsKt.CCU_ANR;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONException;

import java.util.HashMap;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.SystemProperties;
import a75f.io.logic.ccu.restore.RestoreCCU;
import a75f.io.logic.logtasks.UploadLogs;
import a75f.io.logic.preconfig.PreconfigurationManager;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.renatus.ENGG.RenatusEngineeringActivity;
import a75f.io.renatus.anrwatchdog.ANRHandler;
import a75f.io.renatus.registration.CreateNewSite;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.registration.UpdateCCUFragment;
import a75f.io.renatus.safemode.SafeModeActivity;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.PreferenceConstants;
import a75f.io.renatus.util.Prefs;
import a75f.io.util.ExecutorTask;
import io.seventyfivef.haystack.tag.Tags;

public class SplashActivity extends AppCompatActivity implements Globals.OnCcuInitCompletedListener{

    public static final int CCU_PERMISSION_REQUEST_ID = 1;

    public static final String TAG = "CCU_INIT_SPLASH_UI";
    Prefs prefs;
    private Thread registrationThread;
    private ImageView splashLogo75f;
    private LinearLayout daikinSplash;
    private LinearLayout carrierSplash;
    private LinearLayout airoverseSplash;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CcuLog.i("UI_PROFILING", "SplashActivity.onCreate");

        setContentView(R.layout.splash);
        splashLogo75f = findViewById(R.id.splash_logo);
        daikinSplash = findViewById(R.id.daikin_splash);
        carrierSplash = findViewById(R.id.carrier_splash);
        airoverseSplash = findViewById(R.id.airoverse_splash);
        prefs = new Prefs(this);
        /*PreferenceManager.getDefaultSharedPreferences(this).edit().
                putBoolean(getString(R.string.prefs_theme_key),true).commit();*/
        CcuLog.i(TAG, "Splash activity");
        configSplashLogo();
        CcuLog.i(L.TAG_CCU_UI_PROFILING, "SplashActivity.onCreate Done");
        RenatusApp.backgroundServiceInitiator.initServices();
    }

    private void launchUI() {
        HashMap<Object, Object> site = CCUHsApi.getInstance().read("site");
        if (PreferenceUtil.getUpdateCCUStatus() || PreferenceUtil.isCCUInstalling()) {
            CcuLog.i(TAG, "Resume Update CCU");
            resumeUpdateCCU();
        } else if (prefs.getString("INSTALL_TYPE").equals("PRECONFIGCCU") &&
                !prefs.getString("preconfiguration_state").equalsIgnoreCase("Completed")) {
            CcuLog.i(TAG, "Preconfiguration in progress");
            PreconfigurationManager.INSTANCE.init(getApplicationContext());
            Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
            i.putExtra("viewpager_position", 7);
            startActivity(i);
            finish();
        } else if (site.isEmpty() || RestoreCCU.isReplaceCCUUnderProcess()) {
            CcuLog.i(TAG,"No Site Synced navigate to Register");
            Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
            startActivity(i);
            finish();
        }  else if(!site.isEmpty() && !prefs.getBoolean(PreferenceConstants.CCU_SETUP) &&
            prefs.getString("INSTALL_TYPE").equals("CREATENEW")) {
            if (prefs.getBoolean("siteRegistrationRetry")) {
                Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
                i.putExtra("viewpager_position", 3);
                startActivity(i);
                finish();
            } else {
                if (!CCUHsApi.getInstance().isCCURegistered()) {
                    CcuLog.d(TAG, "Site was successfully created but ccu device is not registered/created. " +
                            "Creating and Registering CCU now.");
                    ExecutorTask.executeAsync(
                            () -> CcuLog.i(TAG, "Create CCU & Diag Equip "),
                            () -> {
                                if (!CCUHsApi.getInstance().isCCURegistered()) {
                                    CcuLog.i(L.TAG_REGISTRATION, "CCU name from pref file "+prefs.getString("temp_ccu_name"));
                                    CreateNewSite.postSiteCreationSetup(
                                            false, site, prefs.getString("temp_ccu_name"),
                                            site.get(Tags.INSTALLER_EMAIL).toString(),
                                            site.get(Tags.FM_EMAIL).toString(),
                                            site.get("billingAdminEmail") != null ? site.get("billingAdminEmail").toString() : site.get(Tags.FM_EMAIL) != null ? site.get(Tags.FM_EMAIL).toString() : "");
                                }
                            },
                            () -> {
                                prefs.remove("temp_ccu_name");
                                prefs.setBoolean(PreferenceConstants.CCU_SETUP, true);
                                PreferenceUtil.setTempModeMigrationNotRequired();
                                L.saveCCUState();
                                CcuLog.i(TAG, "CCU Setup Complete ");
                            }
                    );
                }
                CcuLog.i(TAG, "CCU Setup is not completed");
                Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
                i.putExtra("viewpager_position", 21);
                startActivity(i);
                finish();
            }
        } else if(prefs.getBoolean(PreferenceConstants.CCU_SETUP) && !prefs.getBoolean(PreferenceConstants.PROFILE_SETUP)
                && !prefs.getBoolean("ADD_CCU")) {
            CcuLog.i(TAG,"No profile synced navigate to create profile");
            Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
            i.putExtra("viewpager_position", getViewPagerPosition());
            startActivity(i);
            finish();
        } else if(prefs.getBoolean(PreferenceConstants.PROFILE_SETUP) && !prefs.getBoolean(PreferenceConstants.REGISTRATION)) {
            CcuLog.i(TAG,"No floor is Created");
            System.out.println("No Floor is Created");
            Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
            i.putExtra("viewpager_position", getViewPagerPosition());
            startActivity(i);
            finish();
        } else if(prefs.getBoolean(PreferenceConstants.REGISTRATION)) {
            int recovery = SystemProperties.getInt("renatus_recovery",0);
            Intent i;
            if (ANRHandler.INSTANCE.isANRPendingTobeReported(this)) {
                ExecutorTask.executeBackground( () -> {
                    try {
                        CcuLog.e(TAG,"ANR Reported");
                        AlertManager.getInstance().generateAlert(CCU_ANR,
                                ANRHandler.INSTANCE.getAnrAlertMessage(CCUHsApi.getInstance()));
                        UploadLogs.instanceOf().saveCcuLogs();
                        ANRHandler.INSTANCE.updateANRReportingStatus(this, true);
                    } catch (Exception e) {
                        CcuLog.e(TAG,"Failed to save logs while in safe mode");
                    }
                });
            }
            if(Globals.getInstance().isSafeMode()){
                ExecutorTask.executeBackground( () -> {
                    try {
                        UploadLogs.instanceOf().saveCcuLogs();
                    } catch (Exception e) {
                        CcuLog.e(TAG,"Failed to save logs while in safe mode");
                    }
                });
                i = new Intent(SplashActivity.this, SafeModeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            }
            else if (recovery == 1) {
                i = new Intent(SplashActivity.this, RenatusEngineeringActivity.class);
            } else {
                i = new Intent(SplashActivity.this, RenatusLandingActivity.class);
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }else if(!site.isEmpty() && prefs.getBoolean(PreferenceConstants.CCU_SETUP)
                && prefs.getBoolean(PreferenceConstants.ADD_CCU)) {
            CcuLog.i(TAG,"ADD CCU is not completed");
            Intent i = new Intent(SplashActivity.this,
                    FreshRegistration.class);
            i.putExtra("viewpager_position", CCUHsApi.getInstance().isCCURegistered() ? 21 : 23);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }else if(prefs.getString("INSTALL_TYPE").equals("ADDCCU") && !prefs.getBoolean("ADD_CCU")){
            Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
            i.putExtra("viewpager_position", 6);
            startActivity(i);
            finish();
        }else if(prefs.getString("INSTALL_TYPE").equals("CREATENEW") && !prefs.getBoolean("CCU_SETUP")
                && !prefs.getBoolean("REGISTRATION")){
            Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
            i.putExtra("viewpager_position", 21);
            startActivity(i);
            finish();
        } else if(!site.isEmpty() && prefs.getString("INSTALL_TYPE").equals("ADDCCU")
                && !prefs.getBoolean(PreferenceConstants.ADD_CCU)) {
            Intent i = new Intent(SplashActivity.this,
                    FreshRegistration.class);
            i.putExtra("viewpager_position", 6);
            startActivity(i);
            finish();
        } else if(!site.isEmpty() && prefs.getString("INSTALL_TYPE").equals("ADDCCU")
                && !prefs.getBoolean(PreferenceConstants.ADD_CCU)) {
            Intent i = new Intent(SplashActivity.this,
                    FreshRegistration.class);
            i.putExtra("viewpager_position", 6);
            startActivity(i);
            finish();
        } else {
            CcuLog.i(TAG,"Default launching state");
            Intent i = new Intent(SplashActivity.this, RenatusLandingActivity.class);
            i.putExtra("viewpager_position", 0);
            startActivity(i);
            finish();
        }
    }

    private void resumeUpdateCCU(){
        if (prefs.getString("INSTALL_TYPE").equals("CREATENEW")) {
            Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
            i.putExtra("viewpager_position", 3);
            startActivity(i);
            finish();
        } else if (prefs.getString("INSTALL_TYPE").equals("REPLACECCU")){
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment previousFragment = getSupportFragmentManager().findFragmentByTag("popup");
            if (previousFragment != null) {
                ft.remove(previousFragment);
            }
            try {
                UpdateCCUFragment updateCCUFragment = new UpdateCCUFragment().resumeDownloadProcess(
                        PreferenceUtil.getUpdateCCUStatus(), PreferenceUtil.isCCUInstalling(), true);
                updateCCUFragment.show(ft, "popup");
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else if (prefs.getString("INSTALL_TYPE").equals("ADDCCU")){
            Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        PermissionHandler permissionHandler = new PermissionHandler();
        if (permissionHandler.hasAppPermissions(this)) {
            Globals.getInstance().registerOnCcuInitCompletedListener(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CCU_PERMISSION_REQUEST_ID) {
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, R.string.toast_msg_permission_denial, Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
        Globals.getInstance().registerOnCcuInitCompletedListener(this);
    }


    private int getViewPagerPosition() {
        String profileType = prefs.getString("PROFILE");
        switch (profileType) {
            case "DEFAULT":
                return 9;
            case "VAV_STAGED_RTU":
                return 10;
            case "VAV_FULLY_MODULATING":
                return 11;
            case "VAV_STAGED_RTU_VFD":
                return 12;
            case "VAV_HYBRID_RTU":
                return 13;
            case "DAB_STAGED_RTU":
                return 14;
            case "DAB_FULLY_MODULATING":
                return 15;
            case "DAB_STAGED_RTU_VFD":
                return 16;
            case "DAB_HYBRID_RTU":
                return 17;
            case "VAV_IE_RTU":
                return 18;
        }
        return 9;
    }


    private void configSplashLogo(){
        if(CCUUiUtil.isDaikinEnvironment(this))
            daikinSplash.setVisibility(View.VISIBLE);
        else if(CCUUiUtil.isCarrierThemeEnabled(this))
            carrierSplash.setVisibility(View.VISIBLE);
        else if(CCUUiUtil.isAiroverseThemeEnabled(this))
            airoverseSplash.setVisibility(View.VISIBLE);
        else
            splashLogo75f.setVisibility(View.VISIBLE);
    }
    @Override
    public void onInitCompleted() {
        CcuLog.i(L.TAG_CCU_INIT,"CCUInit Completed Callback");
        SplashActivity.this.runOnUiThread(() -> {
            launchUI();
            Globals.getInstance().unRegisterOnCcuInitCompletedListener(this);
        });
    }

}

