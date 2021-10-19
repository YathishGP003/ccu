package a75f.io.renatus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import a75f.io.logger.CcuLog;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.Prefs;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class SplashActivity extends AppCompatActivity {
    
    public static final int CCU_PERMISSION_REQUEST_ID = 1;
    
    public static final String TAG = SplashActivity.class.getSimpleName();
    Prefs prefs;
    private Thread registrationThread;
    private ImageView splashLogo75f;
    private LinearLayout daikinSplash;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CcuLog.i("UI_PROFILING", "SplashActivity.onCreate");
    
        setContentView(R.layout.splash);
        splashLogo75f = findViewById(R.id.splash_logo);
        daikinSplash = findViewById(R.id.daikin_splash);
        prefs = new Prefs(this);
        /*PreferenceManager.getDefaultSharedPreferences(this).edit().
                putBoolean(getString(R.string.prefs_theme_key),true).commit();*/
        Log.i(TAG, "Splash activity");
        configSplashLogo();
        registrationThread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(5000);
                    SplashActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            HashMap site = CCUHsApi.getInstance().read("site");

                            if (site.size() == 0) {
                                Log.i("SplashActivity","No Site Synced navigate to Register");
                                Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
                                startActivity(i);
                                finish();
                            } else if(site.size() > 0 && !prefs.getBoolean("CCU_SETUP")) {
                                Log.i("SplashActivity","CCU Setup is not completed");
                                Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
                                i.putExtra("viewpager_position", 4);
                                startActivity(i);
                                finish();
                            } else if(prefs.getBoolean("CCU_SETUP") && !prefs.getBoolean("PROFILE_SETUP")) {
                                Log.i("SplashActivity","No profile synced navigate to create profile");
                                Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
                                i.putExtra("viewpager_position", getViewPagerPosition());
                                startActivity(i);
                                finish();
                            } else if(prefs.getBoolean("PROFILE_SETUP") && !prefs.getBoolean("REGISTRATION")) {
                                Log.i("SplashActivity","No floor is Created");
                                System.out.println("No Floor is Created");
                                Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
                                i.putExtra("viewpager_position", getViewPagerPosition());
                                startActivity(i);
                                finish();
                            } else if(prefs.getBoolean("REGISTRATION")) {
                                if (!CCUHsApi.getInstance().isCCURegistered()){
                                    registerCcuInBackground();
                                    Log.i("SplashActivity","CCU is not yet registered");
                                }
                                Intent i = new Intent(SplashActivity.this, RenatusLandingActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                                finish();
                            }
                        }
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        CcuLog.i("UI_PROFILING", "SplashActivity.onCreate Done");
    
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    
        PermissionHandler permissionHandler = new PermissionHandler();
        if (permissionHandler.hasAppPermissions(this)) {
            registrationThread.start();
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
        registrationThread.start();
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
        if(BuildConfig.BUILD_TYPE.equals("daikin_prod")|| CCUUiUtil.isDaikinThemeEnabled(this))
            daikinSplash.setVisibility(View.VISIBLE);
        else
            splashLogo75f.setVisibility(View.VISIBLE);
    }
}

