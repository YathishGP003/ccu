package a75f.io.renatus;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.SystemProperties;
import a75f.io.logic.ccu.restore.RestoreCCU;
import a75f.io.logic.logtasks.UploadLogs;
import a75f.io.renatus.ENGG.RenatusEngineeringActivity;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.safemode.SafeModeActivity;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.PreferenceConstants;
import a75f.io.renatus.util.Prefs;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class SplashActivity extends AppCompatActivity implements Globals.OnCcuInitCompletedListener{
    
    public static final int CCU_PERMISSION_REQUEST_ID = 1;
    
    public static final String TAG = SplashActivity.class.getSimpleName();
    Prefs prefs;
    private Thread registrationThread;
    private ImageView splashLogo75f;
    private LinearLayout daikinSplash;
    private LinearLayout carrierSplash;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CcuLog.i("UI_PROFILING", "SplashActivity.onCreate");
    
        setContentView(R.layout.splash);
        splashLogo75f = findViewById(R.id.splash_logo);
        daikinSplash = findViewById(R.id.daikin_splash);
        carrierSplash = findViewById(R.id.carrier_splash);
        prefs = new Prefs(this);
        /*PreferenceManager.getDefaultSharedPreferences(this).edit().
                putBoolean(getString(R.string.prefs_theme_key),true).commit();*/
        Log.i(TAG, "Splash activity");
        configSplashLogo();
        CcuLog.i("UI_PROFILING", "SplashActivity.onCreate Done");
    }

    private void launchUI() {
        HashMap<Object, Object> site = CCUHsApi.getInstance().read("site");
        if (site.size() == 0 || RestoreCCU.isReplaceCCUUnderProcess()) {
            Log.i(TAG,"No Site Synced navigate to Register");
            Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
            startActivity(i);
            finish();
        } else if(site.size() > 0 && !prefs.getBoolean(PreferenceConstants.CCU_SETUP)) {
            Log.i(TAG,"CCU Setup is not completed");
            Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
            i.putExtra("viewpager_position", 21);
            startActivity(i);
            finish();
        } else if(prefs.getBoolean(PreferenceConstants.CCU_SETUP) && !prefs.getBoolean(PreferenceConstants.PROFILE_SETUP)
                && !prefs.getBoolean("ADD_CCU")) {
            Log.i(TAG,"No profile synced navigate to create profile");
            Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
            i.putExtra("viewpager_position", getViewPagerPosition());
            startActivity(i);
            finish();
        } else if(prefs.getBoolean(PreferenceConstants.PROFILE_SETUP) && !prefs.getBoolean(PreferenceConstants.REGISTRATION)) {
            Log.i(TAG,"No floor is Created");
            System.out.println("No Floor is Created");
            Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
            i.putExtra("viewpager_position", getViewPagerPosition());
            startActivity(i);
            finish();
        } else if(prefs.getBoolean(PreferenceConstants.REGISTRATION)) {
            int recovery = SystemProperties.getInt("renatus_recovery",0);
            Intent i;
            if(Globals.getInstance().isSafeMode()){
                new Thread() {
                    @Override
                    public void run() {
                        UploadLogs.instanceOf().saveCcuLogs();
                    }
                }.start();


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
        }else if(site.size() > 0 && prefs.getBoolean(PreferenceConstants.CCU_SETUP)
                && prefs.getBoolean(PreferenceConstants.ADD_CCU)) {
            Log.i(TAG,"ADD CCU is not completed");
            Intent i = new Intent(SplashActivity.this,
                    FreshRegistration.class);
            i.putExtra("viewpager_position", 23);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }else if(prefs.getString("INSTALL_TYPE").equals("ADDCCU") && !prefs.getBoolean("ADD_CCU")){
            Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
            i.putExtra("viewpager_position", 6);
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

    // Yes, this essentially duplicates code in FreshRegistration{Activity}.  It's ok.  It's basically
    //  UI code in both places -- i.e. handling a network call -- and could deviate.
    private void registerCcuInBackground() {
        String installerEmail = prefs.getString("installerEmail");
        CCUHsApi.getInstance().registerCcuAsync(installerEmail)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> { },  // ignore success
                        error -> {
                            // A Toast rather than a dialog is necessary since the interface does not wait
                            // for the response here.  We should fix that when we rewrite Registration.
                            Context context = SplashActivity.this;
                            if (context != null) {
                                Toast.makeText(context, "Error registering CCU.  Please try again", Toast.LENGTH_LONG).show();
                            }
                            CcuLog.w("CCU_HS", "Unexpected error registering CCU.", error);
                        }
                );
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

