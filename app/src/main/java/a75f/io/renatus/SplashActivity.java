package a75f.io.renatus;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.util.Prefs;

public class SplashActivity extends Activity {
    public static final String TAG = SplashActivity.class.getSimpleName();
    Prefs prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        prefs = new Prefs(this);
        Log.i(TAG, "Splash");
        Log.i(TAG, "Waiting 5 seconds and navigating to the registered screen");
        
        Thread registrationThread = new Thread() {
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
                                    String installerEmail = prefs.getString("installerEmail");
                                    CCUHsApi.getInstance().registerCcu(installerEmail);
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

        try {
            PackageManager packageManager = getApplicationContext().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo("a75f.io.renatus", 0);
            String versionName = packageInfo.versionName;
            //Log.i("SplashActivity", "Starting " + BuildConfig.BUILD_TYPE + " app for " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("SplashActivity","Version information not found", e);
        }
        registrationThread.start();
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
}
